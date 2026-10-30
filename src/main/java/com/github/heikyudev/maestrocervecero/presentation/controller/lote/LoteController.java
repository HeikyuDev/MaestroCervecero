package com.github.heikyudev.maestrocervecero.presentation.controller.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
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
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ICostoProduccionLoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IReservaInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoReservaInsumo;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEnvasadoLoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceEtapaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EtapaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AdicionLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.MensajeConLista;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.github.heikyudev.maestrocervecero.util.EtiquetaPlanificacion;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
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
    private static final String ATRIBUTO_FORMULARIO = "loteForm";
    private static final String VISTA_INFORMACION = "lote/lote-informacion";
    /** Pestañas del Detalle del lote, en orden; la primera es la que se abre por defecto. */
    private static final List<String> PESTANAS_INFORMACION = List.of("etapas", "consumos", "reservas", "envasados", "costos");
    private static final String URL_LISTA = "/lotes";
    private static final String URL_BUSCADOR_RECETAS = "/lotes/buscador/recetas";
    private static final String URL_FILTRO_RECETAS = "/lotes/filtros/recetas";

    private final ILoteServicio loteServicio;
    private final IConsumoInsumoServicio consumoInsumoServicio;
    private final IReservaInsumoServicio reservaInsumoServicio;
    private final ICostoProduccionLoteServicio costoProduccionLoteServicio;
    private final IEnvasadoLoteServicio envasadoLoteServicio;
    private final IPlanificacionProduccionServicio planificacionServicio;
    private final IMolinoServicio molinoServicio;
    private final IMaceradorServicio maceradorServicio;
    private final IOllaHervorServicio ollaHervorServicio;
    private final IFermentadorServicio fermentadorServicio;
    private final IRecetaServicio recetaServicio;

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
     * Una adición de lúpulo del hervido: su propia cantidad y el momento en que se agrega, ya redactado ("al inicio del
     * hervor", "cuando falten 15 min para terminar el hervor" o "en el whirlpool").
     */
    @Value
    public static class AdicionDeHervido {
        String nombre;
        Double cantidad;
        UnidadDeMedida unidadDeMedida;
        String momento;
    }

    /**
     * Malta molida en la etapa de Molienda del lote, para la guía de Maceración: lo que el operario realmente molió
     * (suma de los consumos registrados, no anulados, de cada malta) frente a lo requerido por la receta.
     * {@code conConsumos} es {@code false} cuando la Molienda no tiene consumos registrados: en ese caso las cantidades
     * por malta son las requeridas y la guía lo indica.
     */
    @Value
    public static class MaltaMolida {
        List<LineaDeMalta> lineas;
        Double total;
        Double requerido;
        boolean conConsumos;
    }

    /** La cantidad de una malta en la guía de Maceración, en la unidad del insumo. */
    @Value
    public static class LineaDeMalta {
        String nombre;
        Double cantidad;
        UnidadDeMedida unidadDeMedida;
    }

    // ==================== Lista ====================

    /**
     * Lista los lotes, de a 20 y con el último registrado arriba. Los criterios (planificación, receta, código, estado,
     * etapa actual) son opcionales y llegan como texto para poder ignorar un valor mal escrito en vez de fallar el pedido:
     * un valor inválido o una planificación o una receta inexistentes se ignoran con un aviso.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('LOTE_CONSULTAR')")
    public String listarLotes(@RequestParam(required = false) String idPlanificacion,
                              @RequestParam(required = false) String idReceta,
                              @RequestParam(required = false) String codigo,
                              @RequestParam(required = false) String estado,
                              @RequestParam(required = false) String etapaActual,
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

        Long idRecetaFiltro = aId(idReceta);
        String textoReceta = null;
        if (vacioANulo(idReceta) != null && idRecetaFiltro == null) {
            avisos.add("La receta indicada no es válida, se muestran todas");
        } else if (idRecetaFiltro != null) {
            RecetaResponseDTO receta = recetaElegida(idRecetaFiltro, recetaServicio::buscarActivaODadaDeBajaConPlanificacionesPorId);
            if (receta == null) {
                avisos.add("La receta indicada no existe, se muestran todas");
                idRecetaFiltro = null;
            } else {
                textoReceta = textoDeReceta(receta);
            }
        }

        EstadoLote estadoFiltro = aEstadoLote(estado);
        if (vacioANulo(estado) != null && estadoFiltro == null) {
            avisos.add("El estado indicado no es válido, se muestran todos");
        }

        TipoEtapa etapaFiltro = aTipoEtapa(etapaActual);
        if (vacioANulo(etapaActual) != null && etapaFiltro == null) {
            avisos.add("La etapa indicada no es válida, se muestran todas");
        }

        String codigoFiltro = vacioANulo(codigo);
        Page<LoteCronogramaResponseDTO> lotes = loteServicio.filtrarLotesParaListado(idPlanificacionFiltro, idRecetaFiltro,
                codigoFiltro, estadoFiltro, etapaFiltro, PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_MAS_RECIENTE_PRIMERO));

        model.addAttribute("lotes", lotes);
        model.addAttribute("estados", EstadoLote.values());
        model.addAttribute("filtroIdPlanificacion", idPlanificacionFiltro);
        model.addAttribute("filtroPlanificacionTexto", textoPlanificacion);
        model.addAttribute("filtroIdReceta", idRecetaFiltro);
        model.addAttribute("filtroRecetaTexto", textoReceta);
        model.addAttribute("filtroCodigo", codigoFiltro);
        model.addAttribute("filtroEstado", estadoFiltro);
        model.addAttribute("etapas", TipoEtapa.values());
        model.addAttribute("filtroEtapaActual", etapaFiltro);
        agregarAvisos(model, avisos);
        return VISTA_LISTA;
    }

    // ==================== Registro ====================

    /**
     * Muestra el formulario vacío para registrar un lote. Con {@code idPlanificacion} (el botón "Nuevo lote" del
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
            agregarAvisos(model, List.of("La planificación indicada no es válida"));
        } else if (idPlanificacionIndicada != null) {
            try {
                textoPlanificacion = textoDePlanificacion(planificacionServicio.buscarPorId(idPlanificacionIndicada));
                loteForm.setIdPlanificacionProduccion(idPlanificacionIndicada);
            } catch (RecursoNoEncontradoException e) {
                agregarAvisos(model, List.of("La planificación indicada no existe"));
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
                agregarMensaje(redirectAttributes, "Lote " + lote.getIdentificadorInterno() + " creado correctamente", TipoAlerta.SUCCESS);
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
                agregarAvisos(model, List.of("La etapa indicada no es válida, se muestra la etapa actual"));
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
        model.addAttribute("planificacionTexto", EtiquetaPlanificacion.de(lote.getIdPlanificacionProduccion(), receta != null ? receta.getNombre() : null));
        model.addAttribute("tipoEtapa", tipoEtapa);
        model.addAttribute("etapaMostrada", etapaMostrada);
        model.addAttribute("slugEtapa", slugDe(tipoEtapa));
        model.addAttribute("etapaOperable", operable);
        List<InsumoRequeridoResponseDTO> requeridos = cargarInsumosYRegistrosDeEtapa(model, id, tipoEtapa, etapaMostrada, operable);
        cargarGuiaDeEtapa(model, id, tipoEtapa, etapaMostrada, requeridos, receta, lote.getEtapas());

        // El aviso de envasados solo importa donde se puede cancelar un lote que ya empezó
        model.addAttribute("tieneEnvasados", lote.getEstado() == EstadoLote.EN_EJECUCION && envasadoLoteServicio.existenEnvasadosRegistrados(id));
        model.addAttribute("mensajeIniciar", "¿Iniciar el lote " + lote.getIdentificadorInterno() + "? Se reservarán los insumos y los equipos pasarán a En uso");
        model.addAttribute("tituloFinalizar", tipoEtapa == TipoEtapa.ENVASADO ? "Finalizar envasado" : "Finalizar etapa");
        model.addAttribute("mensajeFinalizar", mensajeDeFinalizacion(tipoEtapa));
        return VISTA_DETALLE;
    }

    /**
     * Muestra el "Detalle del lote": una pantalla de solo lectura con la información del lote siempre visible y, debajo,
     * una pestaña por tema (etapas, consumos, reservas, envasados y costos), distinta del panel de trabajo
     * ({@link #mostrarDetalle}). Es solo del GERENTE_DE_PRODUCCION (el operario trabaja en el panel y no ve costos). La
     * pestaña activa viaja en {@code ?pestana=} y solo se cargan los datos de esa pestaña (el costo solo se calcula al
     * abrir Costos); sin pestaña, o con una desconocida, se abre Etapas. Un lote inexistente lo resuelve el
     * {@code ControllerAdvices}.
     */
    @GetMapping("/{id}/detalle")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarInformacionDelLote(@PathVariable Long id, @RequestParam(required = false) String pestana, Model model) {
        LoteDetalleResponseDTO lote = loteServicio.buscarDetallePorId(id);
        VersionRecetaResponseDTO receta = lote.getVersionReceta() != null ? lote.getVersionReceta().getVersion() : null;
        String pestanaActiva = pestana != null && PESTANAS_INFORMACION.contains(pestana) ? pestana : PESTANAS_INFORMACION.get(0);

        model.addAttribute("lote", lote);
        model.addAttribute("receta", receta);
        model.addAttribute("planificacionTexto", EtiquetaPlanificacion.de(lote.getIdPlanificacionProduccion(), receta != null ? receta.getNombre() : null));
        model.addAttribute("etapaActual", lote.getEtapas().stream().filter(e -> e.getEstado() == EstadoEtapaLote.EN_CURSO).map(EtapaLoteResponseDTO::getEtapa).findFirst().orElse(null));
        model.addAttribute("pestana", pestanaActiva);
        switch (pestanaActiva) {
            case "consumos" -> model.addAttribute("consumos", consumoInsumoServicio.listarConsumosDelLote(id));
            case "reservas" -> model.addAttribute("reservas", reservaInsumoServicio.listarReservasDelLote(id));
            case "envasados" -> model.addAttribute("envasados", envasadoLoteServicio.listarEnvasadosDelLote(id));
            case "costos" -> model.addAttribute("costoProduccion", costoProduccionLoteServicio.calcularCostoProduccion(id));
            default -> { /* Etapas sale del propio lote */ }
        }
        return VISTA_INFORMACION;
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
     * <p>
     * La receta se elige con su propio buscador, que solo ofrece recetas activas: un {@code idReceta} que no es de una
     * receta activa se ignora y se busca sin filtrar por receta.
     * </p>
     */
    @GetMapping("/buscador/planificaciones")
    @PreAuthorize("hasAuthority('LOTE_REGISTRAR')")
    public String buscarPlanificaciones(@RequestParam(required = false) Long idReceta,
                                        @RequestParam(defaultValue = "0") int pagina,
                                        Model model) {
        cargarBuscadorDePlanificaciones(model, recetaElegida(idReceta, recetaServicio::buscarPorId), idReceta,
                EstadoSolicitud.PENDIENTE, pagina, "/lotes/buscador/planificaciones", URL_BUSCADOR_RECETAS,
                "Filtre y elija la planificación pendiente del lote.");
        return VISTA_BUSCADOR_PLANIFICACION;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de recetas del criterio "Receta" del buscador de planificaciones del
     * formulario de registro (criterio de nombre + resultados), de a 8 y en orden alfabético. Solo se ofrecen las
     * recetas activas. El orden lo define la consulta del repositorio, por eso el {@code Pageable} no lleva
     * ordenamiento.
     */
    @GetMapping("/buscador/recetas")
    @PreAuthorize("hasAuthority('LOTE_REGISTRAR')")
    public String buscarRecetas(@RequestParam(required = false) String nombre,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        cargarBuscadorDeRecetas(model, recetaServicio.filtrarRecetas(vacioANulo(nombre), paginaDeBuscador(pagina, Sort.unsorted())),
                nombre, URL_BUSCADOR_RECETAS, false, false);
        return VISTA_BUSCADOR_RECETA;
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
        return cargarBuscadorDeEquipo(model, filas, "molino", "molinos", "MOL-01", "/lotes/buscador/molinos",
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
        return cargarBuscadorDeEquipo(model, filas, "macerador", "maceradores", "MAC-01", "/lotes/buscador/maceradores",
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
        return cargarBuscadorDeEquipo(model, filas, "olla de hervor", "ollas de hervor", "OLL-01", "/lotes/buscador/ollas",
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
        return cargarBuscadorDeEquipo(model, filas, "fermentador", "fermentadores", "FER-01", "/lotes/buscador/fermentadores",
                List.of("Capacidad útil (L)"), identificador, estado, avisoDeEstadoOperativo(estadoOperativo, estado));
    }

    // ==================== Buscadores de los filtros de la lista ====================

    /**
     * Devuelve el contenido del modal de búsqueda de planificaciones del filtro de la lista (criterio de receta +
     * resultados), de a 8 y con la última registrada arriba. A diferencia del formulario de registro, ofrece las de
     * cualquier estado, porque filtra el historial.
     * <p>
     * La receta se elige con el buscador de recetas del filtro, que también ofrece las dadas de baja con
     * planificaciones: un {@code idReceta} que ese buscador no ofrece se ignora y se busca sin filtrar por receta.
     * </p>
     */
    @GetMapping("/filtros/planificaciones")
    @PreAuthorize("hasAuthority('LOTE_CONSULTAR')")
    public String filtrarPlanificaciones(@RequestParam(required = false) Long idReceta,
                                         @RequestParam(defaultValue = "0") int pagina,
                                         Model model) {
        cargarBuscadorDePlanificaciones(model, recetaElegida(idReceta, recetaServicio::buscarActivaODadaDeBajaConPlanificacionesPorId),
                idReceta, null, pagina, "/lotes/filtros/planificaciones", URL_FILTRO_RECETAS,
                "Filtre y elija la planificación de los lotes.");
        return VISTA_FILTRO_PLANIFICACION;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de recetas del criterio "Receta" del buscador de planificaciones del
     * filtro de la lista (nombre e "Incluir dados de baja" + resultados), de a 8 y en orden alfabético. Sin el último
     * criterio solo se ofrecen las recetas activas; con él se suman las dadas de baja que tienen alguna planificación,
     * para poder filtrar su historial.
     */
    @GetMapping("/filtros/recetas")
    @PreAuthorize("hasAuthority('LOTE_CONSULTAR')")
    public String filtrarRecetas(@RequestParam(required = false) String nombre,
                                 @RequestParam(defaultValue = "false") boolean incluirDadosDeBaja,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        String nombreBuscado = vacioANulo(nombre);
        Pageable pageable = paginaDeBuscador(pagina, Sort.unsorted());
        Page<RecetaResponseDTO> recetas = incluirDadosDeBaja
                ? recetaServicio.filtrarRecetasConDadasDeBajaConPlanificaciones(nombreBuscado, pageable)
                : recetaServicio.filtrarRecetas(nombreBuscado, pageable);
        cargarBuscadorDeRecetas(model, recetas, nombre, URL_FILTRO_RECETAS, true, incluirDadosDeBaja);
        return VISTA_BUSCADOR_RECETA;
    }

    // ==================== Apoyo: detalle ====================

    /**
     * Carga los insumos de la etapa (con su avance hacia el mínimo cuando se puede finalizar) y, en Envasado, los
     * envasados registrados. Los consumos tienen su propia pantalla. Las etapas que no consumen insumos no consultan
     * insumos.
     *
     * @return Los insumos requeridos de la etapa (vacío si no consume insumos).
     */
    private List<InsumoRequeridoResponseDTO> cargarInsumosYRegistrosDeEtapa(Model model, Long idLote, TipoEtapa tipoEtapa, EtapaLoteResponseDTO etapa, boolean operable) {
        Pageable todos = PageRequest.of(0, Integer.MAX_VALUE, ORDEN_MAS_RECIENTE_PRIMERO);
        List<InsumoRequeridoResponseDTO> requeridos = List.of();
        if (tipoEtapa.isPermiteRegistrarConsumo()) {
            requeridos = consumoInsumoServicio.filtrarInsumosRequeridos(etapa.getId());
        }
        if (tipoEtapa.isPermiteRegistrarEnvasado()) {
            model.addAttribute("envasados", envasadoLoteServicio.filtrarEnvasadosLote(etapa.getId(), null, null, todos).getContent());
        }
        // Las reservas de la etapa con su historial completo, también las consumidas y las liberadas, aunque la etapa o el
        // lote ya hayan terminado; las activas van primero y el resto conserva el orden del service (insumo y vencimiento)
        List<ReservaInsumoResponseDTO> reservas = tipoEtapa.isPermiteRegistrarConsumo()
                ? reservaInsumoServicio.listarReservasDeEtapa(etapa.getId()).stream()
                .sorted(Comparator.comparing((ReservaInsumoResponseDTO r) -> r.getEstado() == EstadoReservaInsumo.ACTIVA ? 0 : 1))
                .toList()
                : List.of();
        model.addAttribute("reservasEtapa", reservas);
        // El avance es lo que acompaña a "Finalizar etapa": solo se calcula donde ese botón puede aparecer
        AvanceEtapaResponseDTO avance = operable && !requeridos.isEmpty() ? loteServicio.calcularAvanceDeEtapa(idLote, tipoEtapa) : null;

        model.addAttribute("requeridos", requeridos);
        model.addAttribute("avance", avance);
        return requeridos;
    }

    /**
     * La malta de la Molienda del lote para la guía de Maceración: por cada malta, lo consumido (registrado, no anulado)
     * en la Molienda y el total molido. Si la Molienda no tiene consumos registrados, las cantidades son las requeridas
     * por la receta y {@code conConsumos} es {@code false}. Devuelve {@code null} si el lote no tiene etapa de Molienda
     * o sus requerimientos no se pueden calcular (la guía usa entonces la masa de malta del cálculo del agua).
     */
    private MaltaMolida maltaMolidaDelLote(List<EtapaLoteResponseDTO> etapas) {
        EtapaLoteResponseDTO molienda = etapas.stream().filter(e -> e.getEtapa() == TipoEtapa.MOLIENDA).findFirst().orElse(null);
        if (molienda == null) {
            return null;
        }
        List<InsumoRequeridoResponseDTO> maltas;
        try {
            maltas = consumoInsumoServicio.filtrarInsumosRequeridos(molienda.getId()).stream()
                    .filter(r -> r.getInsumo() instanceof MaltaResponseDTO)
                    .toList();
        } catch (ReglaNegocioException | RecursoNoEncontradoException e) {
            return null;
        }
        if (maltas.isEmpty()) {
            return null;
        }
        boolean conConsumos = maltas.stream().anyMatch(r -> r.getCantidadConsumida() != null && r.getCantidadConsumida() > 0);
        List<LineaDeMalta> lineas = maltas.stream()
                .filter(r -> !conConsumos || (r.getCantidadConsumida() != null && r.getCantidadConsumida() > 0))
                .map(r -> new LineaDeMalta(r.getInsumo().getNombre(),
                        Redondeo.redondear(conConsumos ? r.getCantidadConsumida() : r.getCantidadRequerida()), r.getUnidadDeMedida()))
                .sorted(Comparator.comparing(LineaDeMalta::getNombre))
                .toList();
        double total = Redondeo.redondear(lineas.stream().mapToDouble(LineaDeMalta::getCantidad).sum());
        double requerido = Redondeo.redondear(maltas.stream().mapToDouble(InsumoRequeridoResponseDTO::getCantidadRequerida).sum());
        return new MaltaMolida(lineas, total, requerido, conConsumos);
    }

    /**
     * Carga lo que la tarjeta guía de la etapa necesita además de los insumos: el agua del lote (Maceración y Hervido,
     * donde un cálculo imposible se informa en la propia tarjeta sin cerrar el detalle), los lúpulos con su tiempo de
     * hervor (Hervido) y el resumen del envasado (Envasado).
     */
    private void cargarGuiaDeEtapa(Model model, Long idLote, TipoEtapa tipoEtapa, EtapaLoteResponseDTO etapa,
                                   List<InsumoRequeridoResponseDTO> requeridos, VersionRecetaResponseDTO receta,
                                   List<EtapaLoteResponseDTO> etapas) {
        if (tipoEtapa == TipoEtapa.MACERACION) {
            model.addAttribute("maltaMolida", maltaMolidaDelLote(etapas));
        }
        if (tipoEtapa == TipoEtapa.MACERACION || tipoEtapa == TipoEtapa.HERVIDO) {
            try {
                model.addAttribute("agua", loteServicio.calcularAguaDelLote(idLote));
            } catch (ReglaNegocioException e) {
                model.addAttribute("aguaError", e.getMessage());
            }
        }
        if (tipoEtapa == TipoEtapa.HERVIDO) {
            model.addAttribute("adiciones", adicionesDeHervido(consumoInsumoServicio.filtrarAdicionesLupulo(etapa.getId()), receta));
        }
        if (tipoEtapa == TipoEtapa.FERMENTACION) {
            model.addAttribute("levaduras", requeridos.stream().filter(r -> r.getInsumo() instanceof LevaduraResponseDTO).toList());
        }
        if (tipoEtapa == TipoEtapa.FERMENTACION || tipoEtapa == TipoEtapa.MADURACION) {
            model.addAttribute("lupulosEnSeco", requeridos.stream().filter(r -> r.getInsumo() instanceof LupuloResponseDTO).toList());
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
     * Las adiciones de lúpulo del hervido, una línea por adición con su propia cantidad (no la suma del lúpulo): primero
     * las de hervor, de la que se agrega antes a la que se agrega después, y al final las del whirlpool. Un lúpulo que
     * hierve toda la duración del hervido se agrega "al inicio del hervor"; si no, "cuando falten N min para terminar el
     * hervor".
     */
    private static List<AdicionDeHervido> adicionesDeHervido(List<AdicionLupuloResponseDTO> adiciones, VersionRecetaResponseDTO receta) {
        Integer duracionHervido = receta != null ? receta.getDuracionHervido() : null;
        return adiciones.stream()
                .filter(adicion -> adicion.getUso() == UsoLupulo.HERVOR || adicion.getUso() == UsoLupulo.WHIRLPOOL)
                .sorted(Comparator.comparing((AdicionLupuloResponseDTO adicion) -> adicion.getUso() == UsoLupulo.WHIRLPOOL)
                        .thenComparing(Comparator.comparing(AdicionLupuloResponseDTO::getTiempoDeHervor).reversed())
                        .thenComparing(AdicionLupuloResponseDTO::getNombreInsumo))
                .map(adicion -> new AdicionDeHervido(adicion.getNombreInsumo(), adicion.getCantidad(), adicion.getUnidadDeMedida(),
                        momentoDeLaAdicion(adicion, duracionHervido)))
                .toList();
    }

    private static String momentoDeLaAdicion(AdicionLupuloResponseDTO adicion, Integer duracionHervido) {
        if (adicion.getUso() == UsoLupulo.WHIRLPOOL) {
            return "en el whirlpool";
        }
        double minutos = adicion.getTiempoDeHervor();
        if (duracionHervido != null && minutos >= duracionHervido) {
            return "al inicio del hervor";
        }
        String texto = minutos == Math.rint(minutos) ? String.valueOf((long) minutos) : String.valueOf(minutos).replace('.', ',');
        return "cuando falten " + texto + " min para terminar el hervor";
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
        return PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR, orden);
    }

    /**
     * Arma el modal de un buscador de planificaciones: las planificaciones del estado indicado ({@code null} para
     * cualquiera) de la receta elegida, de a 8 y con la última registrada arriba, y el selector de receta con la URL de
     * su propio buscador. Si la receta no se resolvió, se busca sin filtrar por receta y el selector queda en blanco.
     */
    private void cargarBuscadorDePlanificaciones(Model model, RecetaResponseDTO receta, Long idReceta, EstadoSolicitud estado, int pagina,
                                                 String urlBuscador, String urlBuscadorRecetas, String subtitulo) {
        Long idRecetaFiltro = receta != null ? idReceta : null;
        model.addAttribute("planificaciones", planificacionServicio.filtrarPlanificacionesProduccion(
                idRecetaFiltro, estado, null, paginaDeBuscador(pagina, ORDEN_MAS_RECIENTE_PRIMERO)));
        model.addAttribute("filtroIdReceta", idRecetaFiltro);
        model.addAttribute("filtroRecetaTexto", receta != null ? textoDeReceta(receta) : null);
        model.addAttribute("urlBuscador", urlBuscador);
        model.addAttribute("urlBuscadorRecetas", urlBuscadorRecetas);
        model.addAttribute("subtituloBuscador", subtitulo);
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
        return planificacion.getEtiqueta();
    }

    /** Muestra los avisos como una sola alerta amarilla, en forma de lista si son varios. */
    private static void agregarAvisos(Model model, List<String> avisos) {
        if (!avisos.isEmpty()) {
            model.addAttribute("mensaje", MensajeConLista.deAvisos(avisos));
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

    /** El tipo de etapa con ese nombre, o {@code null} si el texto está vacío o no corresponde a ninguno. */
    private static TipoEtapa aTipoEtapa(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return TipoEtapa.valueOf(limpio.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
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
