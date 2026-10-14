package com.github.heikyudev.maestrocervecero.presentation.controller.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.CancelacionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.LoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFermentadorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMaceradorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMolinoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IOllaHervorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IConsumoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEnvasadoLoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceEtapaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EtapaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller del módulo de Gestión de Lotes: lista, registro, detalle por etapas, inicio, cancelación y finalización
 * de etapas, más los buscadores en modal que alimentan el formulario de registro y los filtros de la lista.
 * <p>
 * Cada método se autoriza por permiso ({@code hasAuthority}), no por rol: el gerente de producción registra y cancela
 * lotes, y el operario de producción los inicia y finaliza sus etapas; ambos los consultan. Las reglas del lote son
 * del service; este controller traduce los parámetros, arma la vista y vuelve a la pantalla (Post-Redirect-Get). Un
 * rechazo del service en una acción lo muestra el {@code ControllerAdvices} volviendo al origen con el motivo.
 * </p>
 * <p>
 * Los parámetros de la URL se interpretan con tolerancia: un valor inválido o un registro inexistente no rompe la
 * página, se ignora y se avisa (en un buscador en modal, el aviso va dentro del fragmento). Las pantallas de consumos y
 * de envasados tienen su propio Controller.
 * </p>
 */
@Controller
@RequestMapping("/lotes")
@RequiredArgsConstructor
public class LoteController {

    private static final String VISTA_LISTA = "lote/lote-lista";
    private static final String VISTA_FORMULARIO = "lote/lote-form";
    private static final String VISTA_DETALLE = "lote/lote-detalle";
    private static final String VISTA_BUSCADOR_PLANIFICACION = "lote/buscador-planificacion :: buscador";
    private static final String VISTA_BUSCADOR_EQUIPO = "lote/buscador-equipo :: buscador";
    private static final String VISTA_FILTRO_PLANIFICACION = "lote/filtro-planificacion :: buscador";
    private static final String VISTA_FILTRO_FERMENTADOR = "lote/filtro-fermentador :: buscador";
    private static final String ATRIBUTO_FORMULARIO = "loteForm";
    private static final String URL_LISTA = "/lotes";

    private final ILoteServicio loteServicio;
    private final IConsumoInsumoServicio consumoInsumoServicio;
    private final IEnvasadoLoteServicio envasadoLoteServicio;
    private final IPlanificacionProduccionServicio planificacionServicio;
    private final IMolinoServicio molinoServicio;
    private final IMaceradorServicio maceradorServicio;
    private final IOllaHervorServicio ollaHervorServicio;
    private final IFermentadorServicio fermentadorServicio;

    /**
     * Fila de un buscador de equipos: lo que ayuda a elegir (identificador, estado operativo y los valores de las
     * columnas propias de cada tipo de equipo, en el orden de las cabeceras).
     */
    @Value
    public static class FilaEquipo {
        Long id;
        String identificador;
        EstadoOperativo estadoOperativo;
        List<Double> valores;
    }

    /**
     * Lúpulo que se agrega en el hervido, con la cantidad total de la etapa y los tiempos de hervor que la receta le
     * asigna, del mayor al menor (vacío si no tiene ninguno, como el whirlpool).
     */
    @Value
    public static class LupuloDeHervido {
        String nombre;
        Double cantidad;
        UnidadDeMedida unidadDeMedida;
        List<Double> tiemposDeHervor;
    }

    // ==================== Lista ====================

    /**
     * Lista los lotes, de a 20 y con el último registrado arriba. Los criterios (planificación, fermentador, código y
     * estado) son opcionales y llegan como texto para poder ignorar un valor mal escrito en vez de fallar el pedido:
     * un valor inválido o una planificación o un fermentador inexistentes se ignoran con un aviso.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('LOTE_CONSULTAR')")
    public String listarLotes(@RequestParam(required = false) String idPlanificacion,
                              @RequestParam(required = false) String idFermentador,
                              @RequestParam(required = false) String codigo,
                              @RequestParam(required = false) String estado,
                              @RequestParam(defaultValue = "0") int pagina,
                              Model model) {
        List<String> avisos = new ArrayList<>();

        Long idPlanificacionFiltro = aId(idPlanificacion);
        String textoPlanificacion = null;
        if (vacioANulo(idPlanificacion) != null && idPlanificacionFiltro == null) {
            avisos.add("La planificación indicada no es válida, se muestran todas");
        } else if (idPlanificacionFiltro != null) {
            try {
                textoPlanificacion = textoDePlanificacion(planificacionServicio.buscarPorId(idPlanificacionFiltro));
            } catch (RecursoNoEncontradoException e) {
                avisos.add("La planificación indicada no existe, se muestran todas");
                idPlanificacionFiltro = null;
            }
        }

        Long idFermentadorFiltro = aId(idFermentador);
        String textoFermentador = null;
        if (vacioANulo(idFermentador) != null && idFermentadorFiltro == null) {
            avisos.add("El fermentador indicado no es válido, se muestran todos");
        } else if (idFermentadorFiltro != null) {
            try {
                textoFermentador = textoDeFermentador(fermentadorServicio.buscarActivoODadoDeBajaConLotesPorId(idFermentadorFiltro));
            } catch (RecursoNoEncontradoException e) {
                avisos.add("El fermentador indicado no existe, se muestran todos");
                idFermentadorFiltro = null;
            }
        }

        EstadoLote estadoFiltro = aEstadoLote(estado);
        if (vacioANulo(estado) != null && estadoFiltro == null) {
            avisos.add("El estado indicado no es válido, se muestran todos");
        }

        String codigoFiltro = vacioANulo(codigo);
        Page<LoteCronogramaResponseDTO> lotes = loteServicio.filtrarLotesParaListado(idPlanificacionFiltro, idFermentadorFiltro,
                codigoFiltro, estadoFiltro, PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA, ORDEN_MAS_RECIENTE_PRIMERO));

        model.addAttribute("lotes", lotes);
        model.addAttribute("estados", EstadoLote.values());
        model.addAttribute("filtroIdPlanificacion", idPlanificacionFiltro);
        model.addAttribute("filtroPlanificacionTexto", textoPlanificacion);
        model.addAttribute("filtroIdFermentador", idFermentadorFiltro);
        model.addAttribute("filtroFermentadorTexto", textoFermentador);
        model.addAttribute("filtroCodigo", codigoFiltro);
        model.addAttribute("filtroEstado", estadoFiltro);
        agregarAvisos(model, avisos, ". ");
        return VISTA_LISTA;
    }

    // ==================== Registro ====================

    /**
     * Muestra el formulario vacío para registrar un lote. Con {@code idPlanificacion} (el botón "Registrar lote" del
     * estado actual de una planificación) la planificación llega elegida; el estado de la planificación lo valida el
     * service al enviar.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority('LOTE_REGISTRAR')")
    public String mostrarFormularioAlta(@RequestParam(required = false) String idPlanificacion, Model model) {
        LoteFormDTO loteForm = new LoteFormDTO();
        String textoPlanificacion = null;

        Long idPlanificacionIndicada = aId(idPlanificacion);
        if (vacioANulo(idPlanificacion) != null && idPlanificacionIndicada == null) {
            agregarAvisos(model, List.of("La planificación indicada no es válida"), ". ");
        } else if (idPlanificacionIndicada != null) {
            try {
                textoPlanificacion = textoDePlanificacion(planificacionServicio.buscarPorId(idPlanificacionIndicada));
                loteForm.setIdPlanificacionProduccion(idPlanificacionIndicada);
            } catch (RecursoNoEncontradoException e) {
                agregarAvisos(model, List.of("La planificación indicada no existe"), ". ");
            }
        }

        model.addAttribute(ATRIBUTO_FORMULARIO, loteForm);
        model.addAttribute("planificacionTexto", textoPlanificacion);
        model.addAttribute("urlCancelar", URL_LISTA);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el registro de un lote (Post-Redirect-Get) y vuelve a la lista: el lote queda Pendiente y lo inicia el
     * operario, por eso quien lo registra no pasa por el detalle. Si la validación del formulario o una regla del
     * service falla, se vuelve a mostrar el formulario (sin redirigir) con lo que el usuario había cargado.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('LOTE_REGISTRAR')")
    public String registrarLote(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) LoteFormDTO loteForm,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                LoteResponseDTO lote = loteServicio.registrarLote(loteForm);
                agregarMensaje(redirectAttributes, "Lote " + lote.getIdentificadorInterno() + " registrado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoNoEncontradoException | ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, loteForm);
        return VISTA_FORMULARIO;
    }

    // ==================== Detalle ====================

    /**
     * Muestra el detalle del lote: la cabecera con sus acciones, las seis pestañas (una por etapa, con su estado) y el
     * panel de una sola etapa. Sin {@code etapa} se abre la que el service indica (la etapa en curso). Solo se
     * calculan los datos de la etapa mostrada. Un lote inexistente lo resuelve el {@code ControllerAdvices}.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LOTE_CONSULTAR')")
    public String mostrarDetalle(@PathVariable Long id,
                                 @RequestParam(required = false) String etapa,
                                 Model model) {
        LoteDetalleResponseDTO lote = loteServicio.buscarDetallePorId(id);

        TipoEtapa tipoEtapa = lote.getEtapaPorDefecto() != null ? lote.getEtapaPorDefecto() : TipoEtapa.MOLIENDA;
        if (vacioANulo(etapa) != null) {
            TipoEtapa indicada = aEtapa(etapa);
            if (indicada == null) {
                agregarAvisos(model, List.of("La etapa indicada no es válida, se muestra la etapa actual"), ". ");
            } else {
                tipoEtapa = indicada;
            }
        }
        final TipoEtapa tipoMostrado = tipoEtapa;
        EtapaLoteResponseDTO etapaMostrada = lote.getEtapas().stream()
                .filter(e -> e.getEtapa() == tipoMostrado)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa " + tipoMostrado.getEtiqueta() + " del lote con ID: " + id));
        VersionRecetaResponseDTO receta = lote.getVersionReceta() != null ? lote.getVersionReceta().getVersion() : null;
        boolean operable = esOperable(lote.getEstado(), etapaMostrada.getEstado());

        model.addAttribute("lote", lote);
        model.addAttribute("receta", receta);
        model.addAttribute("planificacionTexto", "Plan #" + lote.getIdPlanificacionProduccion()
                + (receta != null && receta.getNombre() != null ? " - " + receta.getNombre() : ""));
        model.addAttribute("tipoEtapa", tipoEtapa);
        model.addAttribute("etapaMostrada", etapaMostrada);
        model.addAttribute("slugEtapa", slugDe(tipoEtapa));
        model.addAttribute("etapaOperable", operable);
        List<InsumoRequeridoResponseDTO> requeridos = cargarInsumosYRegistrosDeEtapa(model, id, tipoEtapa, etapaMostrada, operable);
        cargarGuiaDeEtapa(model, id, tipoEtapa, etapaMostrada, requeridos, receta);

        // El aviso de envasados solo importa donde se puede cancelar un lote que ya empezó
        model.addAttribute("tieneEnvasados", lote.getEstado() == EstadoLote.EN_EJECUCION && envasadoLoteServicio.existenEnvasadosRegistrados(id));
        model.addAttribute("mensajeIniciar", "¿Iniciar el lote " + lote.getIdentificadorInterno() + "? Se reservarán los insumos y los equipos pasarán a En uso");
        model.addAttribute("tituloFinalizar", tipoEtapa == TipoEtapa.ENVASADO ? "Finalizar envasado" : "Finalizar etapa");
        model.addAttribute("mensajeFinalizar", mensajeDeFinalizacion(tipoEtapa));
        return VISTA_DETALLE;
    }

    // ==================== Iniciar y cancelar ====================

    /**
     * Inicia un lote pendiente y vuelve al detalle (Post-Redirect-Get), que abre la etapa en curso. Si el service lo
     * rechaza (stock insuficiente, equipamiento no disponible, estado), el {@code ControllerAdvices} vuelve al origen
     * con el motivo.
     */
    @PostMapping("/{id}/iniciar")
    @PreAuthorize("hasAuthority('LOTE_INICIAR')")
    public String iniciarLote(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        LoteResponseDTO lote = loteServicio.iniciarLote(id);
        agregarMensaje(redirectAttributes, "Lote " + lote.getIdentificadorInterno() + " iniciado correctamente", TipoAlerta.SUCCESS);
        return redirigirAlDetalle(id);
    }

    /**
     * Cancela un lote pendiente o en ejecución. El motivo es obligatorio y lo valida el service: si falta, o si el lote
     * tiene envasados registrados, el {@code ControllerAdvices} vuelve al origen con el motivo del rechazo.
     */
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('LOTE_ANULAR')")
    public String cancelarLote(@PathVariable Long id,
                               @RequestParam(required = false) String motivo,
                               RedirectAttributes redirectAttributes) {
        LoteResponseDTO lote = loteServicio.cancelarLote(id, CancelacionLoteFormDTO.builder().motivoCancelacion(motivo).build());
        agregarMensaje(redirectAttributes, "Lote " + lote.getIdentificadorInterno() + " cancelado correctamente", TipoAlerta.SUCCESS);
        return redirigirAlDetalle(id);
    }

    // ==================== Finalizar etapas ====================

    /** Finaliza la etapa de Molienda y vuelve al detalle, que abre la etapa en curso. */
    @PostMapping("/{id}/etapas/molienda/finalizar")
    @PreAuthorize("hasAuthority('LOTE_FINALIZAR_MOLIENDA')")
    public String finalizarMolienda(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        loteServicio.finalizarMolienda(id);
        return volverDeFinalizarEtapa(id, TipoEtapa.MOLIENDA, redirectAttributes);
    }

    /** Finaliza la etapa de Maceración y vuelve al detalle, que abre la etapa en curso. */
    @PostMapping("/{id}/etapas/maceracion/finalizar")
    @PreAuthorize("hasAuthority('LOTE_FINALIZAR_MACERACION')")
    public String finalizarMaceracion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        loteServicio.finalizarMaceracion(id);
        return volverDeFinalizarEtapa(id, TipoEtapa.MACERACION, redirectAttributes);
    }

    /** Finaliza la etapa de Hervido y vuelve al detalle, que abre la etapa en curso. */
    @PostMapping("/{id}/etapas/hervido/finalizar")
    @PreAuthorize("hasAuthority('LOTE_FINALIZAR_HERVIDO')")
    public String finalizarHervido(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        loteServicio.finalizarHervido(id);
        return volverDeFinalizarEtapa(id, TipoEtapa.HERVIDO, redirectAttributes);
    }

    /** Finaliza la etapa de Fermentación y vuelve al detalle, que abre la etapa en curso. */
    @PostMapping("/{id}/etapas/fermentacion/finalizar")
    @PreAuthorize("hasAuthority('LOTE_FINALIZAR_FERMENTACION')")
    public String finalizarFermentacion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        loteServicio.finalizarFermentacion(id);
        return volverDeFinalizarEtapa(id, TipoEtapa.FERMENTACION, redirectAttributes);
    }

    /** Finaliza la etapa de Maduración y vuelve al detalle, que abre la etapa en curso. */
    @PostMapping("/{id}/etapas/maduracion/finalizar")
    @PreAuthorize("hasAuthority('LOTE_FINALIZAR_MADURACION')")
    public String finalizarMaduracion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        loteServicio.finalizarMaduracion(id);
        return volverDeFinalizarEtapa(id, TipoEtapa.MADURACION, redirectAttributes);
    }

    /** Finaliza el envasado, con lo que el lote queda Finalizado, y vuelve al detalle. */
    @PostMapping("/{id}/etapas/envasado/finalizar")
    @PreAuthorize("hasAuthority('LOTE_FINALIZAR_ENVASADO')")
    public String finalizarEnvasado(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        LoteResponseDTO lote = loteServicio.finalizarEnvasado(id);
        agregarMensaje(redirectAttributes, "Lote " + lote.getIdentificadorInterno() + " finalizado correctamente", TipoAlerta.SUCCESS);
        return redirigirAlDetalle(id);
    }

    // ==================== Buscadores del formulario de registro ====================

    /**
     * Devuelve el contenido del modal de búsqueda de planificaciones del formulario de registro (criterio de receta +
     * resultados), de a 8 y con la última registrada arriba. Solo se ofrecen las pendientes: es el único estado sobre
     * el que se registra un lote.
     */
    @GetMapping("/buscador/planificaciones")
    @PreAuthorize("hasAuthority('LOTE_REGISTRAR')")
    public String buscarPlanificaciones(@RequestParam(required = false) String receta,
                                        @RequestParam(defaultValue = "0") int pagina,
                                        Model model) {
        model.addAttribute("planificaciones", planificacionServicio.filtrarPlanificacionesPorNombreDeReceta(
                vacioANulo(receta), EstadoSolicitud.PENDIENTE,
                paginaDeBuscador(pagina, ORDEN_MAS_RECIENTE_PRIMERO)));
        model.addAttribute("filtroReceta", receta);
        model.addAttribute("urlBuscador", "/lotes/buscador/planificaciones");
        model.addAttribute("subtituloBuscador", "Filtre y elija la planificación pendiente del lote");
        return VISTA_BUSCADOR_PLANIFICACION;
    }

    /** Devuelve el contenido del modal de búsqueda de molinos activos, en orden alfabético por identificador. */
    @GetMapping("/buscador/molinos")
    @PreAuthorize("hasAuthority('LOTE_REGISTRAR')")
    public String buscarMolinos(@RequestParam(required = false) String identificador,
                                @RequestParam(required = false) String estadoOperativo,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        EstadoOperativo estado = aEstadoOperativo(estadoOperativo);
        Page<FilaEquipo> filas = molinoServicio
                .filtrarMolinos(vacioANulo(identificador), estado, paginaDeBuscador(pagina, ORDEN_POR_IDENTIFICADOR))
                .map(m -> new FilaEquipo(m.getId(), m.getIdentificadorInterno(), m.getEstadoOperativo(), List.of(m.getRendimientoMolienda())));
        return cargarBuscadorDeEquipo(model, filas, "molino", "molinos", "M-01", "/lotes/buscador/molinos",
                List.of("Rendimiento (kg/h)"), identificador, estado, avisoDeEstadoOperativo(estadoOperativo, estado));
    }

    /** Devuelve el contenido del modal de búsqueda de maceradores activos, en orden alfabético por identificador. */
    @GetMapping("/buscador/maceradores")
    @PreAuthorize("hasAuthority('LOTE_REGISTRAR')")
    public String buscarMaceradores(@RequestParam(required = false) String identificador,
                                    @RequestParam(required = false) String estadoOperativo,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        EstadoOperativo estado = aEstadoOperativo(estadoOperativo);
        Page<FilaEquipo> filas = maceradorServicio
                .filtrarMaceradores(vacioANulo(identificador), estado, paginaDeBuscador(pagina, ORDEN_POR_IDENTIFICADOR))
                .map(m -> new FilaEquipo(m.getId(), m.getIdentificadorInterno(), m.getEstadoOperativo(), List.of(m.getCapacidadUtil(), m.getEspacioMuerto())));
        return cargarBuscadorDeEquipo(model, filas, "macerador", "maceradores", "MA-01", "/lotes/buscador/maceradores",
                List.of("Capacidad útil (L)", "Espacio muerto (L)"), identificador, estado, avisoDeEstadoOperativo(estadoOperativo, estado));
    }

    /** Devuelve el contenido del modal de búsqueda de ollas de hervor activas, en orden alfabético por identificador. */
    @GetMapping("/buscador/ollas")
    @PreAuthorize("hasAuthority('LOTE_REGISTRAR')")
    public String buscarOllasHervor(@RequestParam(required = false) String identificador,
                                    @RequestParam(required = false) String estadoOperativo,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        EstadoOperativo estado = aEstadoOperativo(estadoOperativo);
        Page<FilaEquipo> filas = ollaHervorServicio
                .filtrarOllasHervor(vacioANulo(identificador), estado, paginaDeBuscador(pagina, ORDEN_POR_IDENTIFICADOR))
                .map(o -> new FilaEquipo(o.getId(), o.getIdentificadorInterno(), o.getEstadoOperativo(), List.of(o.getCapacidadUtil())));
        return cargarBuscadorDeEquipo(model, filas, "olla de hervor", "ollas de hervor", "OH-01", "/lotes/buscador/ollas",
                List.of("Capacidad útil (L)"), identificador, estado, avisoDeEstadoOperativo(estadoOperativo, estado));
    }

    /** Devuelve el contenido del modal de búsqueda de fermentadores activos, en orden alfabético por identificador. */
    @GetMapping("/buscador/fermentadores")
    @PreAuthorize("hasAuthority('LOTE_REGISTRAR')")
    public String buscarFermentadores(@RequestParam(required = false) String identificador,
                                      @RequestParam(required = false) String estadoOperativo,
                                      @RequestParam(defaultValue = "0") int pagina,
                                      Model model) {
        EstadoOperativo estado = aEstadoOperativo(estadoOperativo);
        Page<FilaEquipo> filas = fermentadorServicio
                .filtrarFermentadores(vacioANulo(identificador), estado, paginaDeBuscador(pagina, ORDEN_POR_IDENTIFICADOR))
                .map(f -> new FilaEquipo(f.getId(), f.getIdentificadorInterno(), f.getEstadoOperativo(), List.of(f.getCapacidadUtil())));
        return cargarBuscadorDeEquipo(model, filas, "fermentador", "fermentadores", "F-01", "/lotes/buscador/fermentadores",
                List.of("Capacidad útil (L)"), identificador, estado, avisoDeEstadoOperativo(estadoOperativo, estado));
    }

    // ==================== Buscadores de los filtros de la lista ====================

    /**
     * Devuelve el contenido del modal de búsqueda de planificaciones del filtro de la lista (criterio de receta +
     * resultados), de a 8 y con la última registrada arriba. A diferencia del formulario de registro, ofrece las de
     * cualquier estado, porque filtra el historial.
     */
    @GetMapping("/filtros/planificaciones")
    @PreAuthorize("hasAuthority('LOTE_CONSULTAR')")
    public String filtrarPlanificaciones(@RequestParam(required = false) String receta,
                                         @RequestParam(defaultValue = "0") int pagina,
                                         Model model) {
        model.addAttribute("planificaciones", planificacionServicio.filtrarPlanificacionesPorNombreDeReceta(
                vacioANulo(receta), null, paginaDeBuscador(pagina, ORDEN_MAS_RECIENTE_PRIMERO)));
        model.addAttribute("filtroReceta", receta);
        model.addAttribute("urlBuscador", "/lotes/filtros/planificaciones");
        model.addAttribute("subtituloBuscador", "Filtre y elija la planificación de los lotes");
        return VISTA_FILTRO_PLANIFICACION;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de fermentadores del filtro de la lista (identificador, estado
     * operativo e "Incluir dados de baja" + resultados), de a 8 y en orden alfabético por identificador. Sin el último
     * criterio solo se ofrecen los activos; con él se suman los dados de baja que tuvieron lotes, para poder filtrar
     * su historial.
     */
    @GetMapping("/filtros/fermentadores")
    @PreAuthorize("hasAuthority('LOTE_CONSULTAR')")
    public String filtrarFermentadores(@RequestParam(required = false) String identificador,
                                       @RequestParam(required = false) String estadoOperativo,
                                       @RequestParam(defaultValue = "false") boolean incluirDadosDeBaja,
                                       @RequestParam(defaultValue = "0") int pagina,
                                       Model model) {
        EstadoOperativo estado = aEstadoOperativo(estadoOperativo);
        String identificadorBuscado = vacioANulo(identificador);
        Pageable pageable = paginaDeBuscador(pagina, ORDEN_POR_IDENTIFICADOR);
        Page<FermentadorResponseDTO> fermentadores = incluirDadosDeBaja
                ? fermentadorServicio.filtrarFermentadoresConDadosDeBajaConLotes(identificadorBuscado, estado, pageable)
                : fermentadorServicio.filtrarFermentadores(identificadorBuscado, estado, pageable);

        model.addAttribute("fermentadores", fermentadores);
        model.addAttribute("estados", EstadoOperativo.values());
        model.addAttribute("filtroIdentificador", identificador);
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("avisoBuscador", avisoDeEstadoOperativo(estadoOperativo, estado));
        model.addAttribute("filtroIncluirDadosDeBaja", incluirDadosDeBaja);
        return VISTA_FILTRO_FERMENTADOR;
    }

    // ==================== Apoyo: detalle ====================

    /**
     * Carga los insumos de la etapa (con su avance hacia el mínimo cuando se puede finalizar) y sus registros de solo
     * lectura: los consumos o, en Envasado, los envasados. Las etapas que no consumen insumos no consultan nada.
     *
     * @return Los insumos requeridos de la etapa (vacío si no consume insumos).
     */
    private List<InsumoRequeridoResponseDTO> cargarInsumosYRegistrosDeEtapa(Model model, Long idLote, TipoEtapa tipoEtapa, EtapaLoteResponseDTO etapa, boolean operable) {
        Pageable todos = PageRequest.of(0, Integer.MAX_VALUE, ORDEN_MAS_RECIENTE_PRIMERO);
        List<InsumoRequeridoResponseDTO> requeridos = List.of();
        if (tipoEtapa.isPermiteRegistrarConsumo()) {
            requeridos = consumoInsumoServicio.filtrarInsumosRequeridos(etapa.getId());
            model.addAttribute("consumos", consumoInsumoServicio.filtrarConsumosInsumo(etapa.getId(), null, null, null, todos).getContent());
        }
        if (tipoEtapa.isPermiteRegistrarEnvasado()) {
            model.addAttribute("envasados", envasadoLoteServicio.filtrarEnvasadosLote(etapa.getId(), null, null, todos).getContent());
        }
        // El avance es lo que acompaña a "Finalizar etapa": solo se calcula donde ese botón puede aparecer
        AvanceEtapaResponseDTO avance = operable && !requeridos.isEmpty() ? loteServicio.calcularAvanceDeEtapa(idLote, tipoEtapa) : null;

        model.addAttribute("requeridos", requeridos);
        model.addAttribute("avance", avance);
        return requeridos;
    }

    /**
     * Carga lo que la tarjeta guía de la etapa necesita además de los insumos: el agua del lote (Maceración y Hervido,
     * donde un cálculo imposible se informa en la propia tarjeta sin cerrar el detalle), los lúpulos con su tiempo de
     * hervor (Hervido) y el resumen del envasado (Envasado).
     */
    private void cargarGuiaDeEtapa(Model model, Long idLote, TipoEtapa tipoEtapa, EtapaLoteResponseDTO etapa,
                                   List<InsumoRequeridoResponseDTO> requeridos, VersionRecetaResponseDTO receta) {
        if (tipoEtapa == TipoEtapa.MACERACION || tipoEtapa == TipoEtapa.HERVIDO) {
            try {
                model.addAttribute("agua", loteServicio.calcularAguaDelLote(idLote));
            } catch (ReglaNegocioException e) {
                model.addAttribute("aguaError", e.getMessage());
            }
        }
        if (tipoEtapa == TipoEtapa.HERVIDO) {
            model.addAttribute("lupulos", lupulosDeHervido(requeridos, receta));
        }
        if (tipoEtapa == TipoEtapa.ENVASADO) {
            try {
                model.addAttribute("resumenEnvasado", envasadoLoteServicio.resumirEnvasado(etapa.getId()));
            } catch (ReglaNegocioException e) {
                model.addAttribute("resumenEnvasadoError", e.getMessage());
            }
        }
    }

    /**
     * Los lúpulos que se agregan en el hervido son los insumos requeridos de la etapa, con su cantidad total (el service
     * suma la de todos los detalles del mismo insumo en la etapa). Sus tiempos de hervor salen de la receta, cruzando
     * por el id del insumo: si la receta repite el lúpulo con distintos tiempos, se listan todos; el que no tiene
     * ninguno, como el whirlpool, queda sin tiempo.
     */
    private static List<LupuloDeHervido> lupulosDeHervido(List<InsumoRequeridoResponseDTO> requeridos, VersionRecetaResponseDTO receta) {
        return requeridos.stream()
                .map(requerido -> new LupuloDeHervido(requerido.getInsumo().getNombre(), requerido.getCantidadRequerida(),
                        requerido.getUnidadDeMedida(), tiemposDeHervor(receta, requerido.getInsumo().getId())))
                .toList();
    }

    /** Los tiempos de hervor de la receta para ese insumo en el hervido, sin repetir y del mayor al menor. */
    private static List<Double> tiemposDeHervor(VersionRecetaResponseDTO receta, Long idInsumo) {
        if (receta == null || receta.getDetallesLupulo() == null) {
            return List.of();
        }
        return receta.getDetallesLupulo().stream()
                .filter(detalle -> detalle.getEtapaDeUso() == TipoEtapa.HERVIDO && detalle.getTiempoDeHervor() != null
                        && detalle.getTiempoDeHervor() > 0 && detalle.getLupulo() != null && Objects.equals(detalle.getLupulo().getId(), idInsumo))
                .map(DetalleLupuloResponseDTO::getTiempoDeHervor)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /**
     * Lo que el diálogo de confirmación anuncia al finalizar la etapa: qué pasa con el equipo que se libera.
     */
    private static String mensajeDeFinalizacion(TipoEtapa tipoEtapa) {
        return switch (tipoEtapa) {
            case MOLIENDA -> "¿Finalizar la etapa Molienda? El molino pasará a En limpieza";
            case MACERACION -> "¿Finalizar la etapa Maceración? El macerador pasará a En limpieza";
            case HERVIDO -> "¿Finalizar la etapa Hervido? La olla de hervor pasará a En limpieza";
            case FERMENTACION -> "¿Finalizar la etapa Fermentación? El fermentador sigue En uso hasta finalizar el envasado";
            case MADURACION -> "¿Finalizar la etapa Maduración? El fermentador sigue En uso hasta finalizar el envasado";
            case ENVASADO -> "¿Finalizar el envasado? El lote quedará Finalizado y el fermentador pasará a En limpieza";
        };
    }

    // ==================== Apoyo: formulario ====================

    /**
     * Carga lo que la vista del formulario necesita además del DTO: el texto de cada entidad elegida (para mostrarlo
     * en su selector sin agregar campos de solo lectura al DTO) y la URL de cancelación. Una entidad que ya no existe
     * deja su selector en blanco.
     */
    private void cargarModeloFormulario(Model model, LoteFormDTO loteForm) {
        model.addAttribute("planificacionTexto", textoDePlanificacion(loteForm.getIdPlanificacionProduccion()));
        model.addAttribute("molinoTexto", resolverTexto(loteForm.getIdMolino(), id -> molinoServicio.buscarPorId(id).getIdentificadorInterno()));
        model.addAttribute("maceradorTexto", resolverTexto(loteForm.getIdMacerador(), id -> maceradorServicio.buscarPorId(id).getIdentificadorInterno()));
        model.addAttribute("ollaTexto", resolverTexto(loteForm.getIdOllaHervor(), id -> ollaHervorServicio.buscarPorId(id).getIdentificadorInterno()));
        model.addAttribute("fermentadorTexto", resolverTexto(loteForm.getIdFermentador(), id -> fermentadorServicio.buscarPorId(id).getIdentificadorInterno()));
        model.addAttribute("urlCancelar", URL_LISTA);
    }

    private String textoDePlanificacion(Long idPlanificacion) {
        return resolverTexto(idPlanificacion, id -> textoDePlanificacion(planificacionServicio.buscarPorId(id)));
    }

    /**
     * El texto de la entidad elegida, o {@code null} si no se eligió ninguna o ya no existe.
     */
    private static String resolverTexto(Long id, Function<Long, String> consulta) {
        if (id == null) {
            return null;
        }
        try {
            return consulta.apply(id);
        } catch (RecursoNoEncontradoException e) {
            return null;
        }
    }

    // ==================== Apoyo: buscadores ====================

    private static Pageable paginaDeBuscador(int pagina, Sort orden) {
        return PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA_BUSCADOR, orden);
    }

    /**
     * Arma el modal de un buscador de equipos del formulario de registro. Los cuatro equipos comparten la misma vista:
     * cambian el nombre, el ejemplo del criterio, la URL y las columnas propias de cada uno.
     */
    private String cargarBuscadorDeEquipo(Model model, Page<FilaEquipo> filas, String nombre, String nombrePlural, String ejemplo,
                                          String urlBuscador, List<String> columnas, String identificador, EstadoOperativo estadoOperativo,
                                          String aviso) {
        model.addAttribute("filas", filas);
        model.addAttribute("nombreEquipo", nombre);
        model.addAttribute("nombreEquipoPlural", nombrePlural);
        model.addAttribute("ejemploIdentificador", ejemplo);
        model.addAttribute("urlBuscador", urlBuscador);
        model.addAttribute("columnas", columnas);
        model.addAttribute("estados", EstadoOperativo.values());
        model.addAttribute("filtroIdentificador", identificador);
        model.addAttribute("filtroEstado", estadoOperativo);
        model.addAttribute("avisoBuscador", aviso);
        return VISTA_BUSCADOR_EQUIPO;
    }

    // ==================== Apoyo: general ====================

    private static String textoDePlanificacion(PlanificacionProduccionResponseDTO planificacion) {
        String receta = planificacion.getVersionReceta() != null ? planificacion.getVersionReceta().getNombre() : null;
        return "Plan #" + planificacion.getId() + (receta != null ? " - " + receta : "");
    }

    private static String textoDeFermentador(FermentadorResponseDTO fermentador) {
        return fermentador.getEstado() == Estado.BAJA
                ? fermentador.getIdentificadorInterno() + " (dado de baja)"
                : fermentador.getIdentificadorInterno();
    }

    /** Muestra los avisos como una sola alerta amarilla, unidos con el separador indicado. */
    private static void agregarAvisos(Model model, List<String> avisos, String separador) {
        if (!avisos.isEmpty()) {
            model.addAttribute("mensaje", String.join(separador, avisos));
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }
    }

    private static String volverDeFinalizarEtapa(Long id, TipoEtapa tipoEtapa, RedirectAttributes redirectAttributes) {
        agregarMensaje(redirectAttributes, "Etapa de " + tipoEtapa.getEtiqueta() + " finalizada correctamente", TipoAlerta.SUCCESS);
        return redirigirAlDetalle(id);
    }

    /** Vuelve al detalle sin fijar la pestaña: el detalle abre la etapa en curso. */
    private static String redirigirAlDetalle(Long id) {
        return "redirect:" + URL_LISTA + "/" + id;
    }

    /** El estado del lote con ese nombre, o {@code null} si el texto está vacío o no corresponde a ninguno. */
    private static EstadoLote aEstadoLote(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return EstadoLote.valueOf(limpio.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
