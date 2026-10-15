package com.github.heikyudev.maestrocervecero.presentation.controller.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.ConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.ILoteInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IConsumoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IReservaInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceEtapaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EtapaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
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

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de las pantallas de consumos de una etapa del lote: el resumen del avance por insumo con las pestañas
 * "Reservados" y "Directos" (cada una lista los consumos de su tipo), las pantallas de formulario de un consumo
 * reservado y de un consumo directo, la anulación de los consumos registrados y los buscadores en modal de lotes de
 * insumo.
 * <p>
 * Las pantallas son de operación: cada método se autoriza por permiso ({@code hasAuthority}) y las usa el operario de
 * producción. El id de la etapa nunca viaja en un formulario: el Controller lo resuelve a partir del lote y del tipo
 * de etapa de la ruta, y verifica que el consumo a anular pertenezca a esa etapa. Las reglas del consumo son del
 * service; un rechazo lo muestra el {@code ControllerAdvices} volviendo al origen con el motivo. Después de registrar
 * se vuelve a la pestaña del tipo de consumo y después de anular a la pestaña del consumo anulado (Post-Redirect-Get);
 * las advertencias del consumo viajan aparte para mostrarse como alertas amarillas. Un formulario inválido vuelve a su
 * pantalla de formulario por Post-Redirect-Get, con los errores y lo escrito en los atributos flash: el navegador nunca
 * queda en la URL de un POST.
 * </p>
 */
@Controller
@RequestMapping("/lotes/{idLote}/etapas/{etapa}/consumos")
@RequiredArgsConstructor
public class ConsumoInsumoController {

    private static final String VISTA_PANTALLA = "lote/consumo-lista";
    private static final String VISTA_FORMULARIO = "lote/consumo-form";
    private static final String VISTA_BUSCADOR = "lote/buscador-lote-insumo :: buscador";
    private static final String ATRIBUTO_FORMULARIO = "consumoForm";
    private static final String MENSAJE_ETAPA_INVALIDA = "La etapa indicada no es válida";
    private static final String MENSAJE_NO_OPERABLE = "Solo se pueden registrar consumos con el lote en ejecución y la etapa en curso";
    private static final String MENSAJE_SIN_INSUMOS = "Esta etapa no consume insumos";
    private static final String MENSAJE_ELEGIR_INSUMO = "Elija primero un insumo de la etapa";
    private static final String VISTA_BUSCADOR_INSUMO = "lote/buscador-insumo-consumo :: buscador";

    private final ILoteServicio loteServicio;
    private final IConsumoInsumoServicio consumoInsumoServicio;
    private final IReservaInsumoServicio reservaInsumoServicio;
    private final ILoteInsumoServicio loteInsumoServicio;

    /**
     * Una fila del resumen de la etapa: lo requerido, reservado y consumido de un insumo y su avance hacia el mínimo.
     */
    @Value
    public static class ResumenDeInsumo {
        InsumoRequeridoResponseDTO requerido;
        Double porcentaje;
        boolean cumple;
    }

    /**
     * Una fila del buscador de lotes de insumo: el lote y la cantidad que se ofrece para consumir (el saldo reservado
     * en un consumo reservado, el disponible en un consumo directo).
     */
    @Value
    public static class FilaDeBuscador {
        LoteInsumoResponseDTO lote;
        Double cantidad;
    }

    /**
     * Una fila del buscador de insumos de un consumo: lo requerido, reservado y consumido del insumo en la etapa y su
     * tipo (Malta, Lúpulo o Levadura).
     */
    @Value
    public static class FilaDeInsumo {
        InsumoRequeridoResponseDTO requerido;
        String tipo;
    }

    /** La etapa de la ruta ya resuelta a partir del lote. */
    private record ContextoDeEtapa(LoteDetalleResponseDTO lote, TipoEtapa tipo, EtapaLoteResponseDTO etapa) {

        /** Solo se registran y anulan consumos con el lote en ejecución y la etapa en curso. */
        boolean operable() {
            return esOperable(lote.getEstado(), etapa.getEstado());
        }
    }

    // ==================== Pantalla ====================

    /**
     * Muestra la pantalla de consumos de la etapa: el resumen de los insumos y la pestaña pedida ("reservado", la
     * predeterminada, o "directo") con los consumos de ese tipo de a 20 y el último registrado arriba. El criterio
     * opcional "estado" (Registrado o Anulado) filtra la lista; sin él, o con un valor inválido (se avisa), se muestran
     * en todos los estados. Una etapa que no admite consumos o inexistente vuelve al detalle del lote con un aviso. Con
     * el lote fuera de ejecución o la etapa fuera de curso la pantalla es de solo lectura.
     */
    @GetMapping({"", "/"})
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_CONSULTAR')")
    public String mostrarConsumos(@PathVariable Long idLote,
                                  @PathVariable String etapa,
                                  @RequestParam(required = false) String tipo,
                                  @RequestParam(required = false) String estado,
                                  @RequestParam(defaultValue = "0") int pagina,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        TipoEtapa tipoEtapa = aEtapa(etapa);
        String rechazo = motivoDeRechazo(tipoEtapa);
        if (rechazo != null) {
            return volverAlDetalle(idLote, rechazo, redirectAttributes);
        }

        EstadoTransaccion filtroEstado = aEstadoTransaccion(estado);
        if (vacioANulo(estado) != null && filtroEstado == null) {
            model.addAttribute("mensaje", "El estado indicado no es válido, se muestran todos los consumos");
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }

        ContextoDeEtapa contexto = resolverContexto(idLote, tipoEtapa);
        TipoConsumo tipoConsumo = aTipoConsumo(tipo);
        Long idEtapa = contexto.etapa().getId();
        List<InsumoRequeridoResponseDTO> requeridos = consumoInsumoServicio.filtrarInsumosRequeridos(idEtapa);
        if (requeridos.isEmpty()) {
            return volverAlDetalle(idLote, MENSAJE_SIN_INSUMOS, redirectAttributes);
        }
        AvanceEtapaResponseDTO avance = loteServicio.calcularAvanceDeEtapa(contexto.lote().getId(), tipoEtapa);
        Page<ConsumoInsumoResponseDTO> consumos = consumoInsumoServicio.filtrarConsumosInsumo(idEtapa, tipoConsumo, null, filtroEstado,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_MAS_RECIENTE_PRIMERO));

        model.addAttribute("lote", contexto.lote());
        model.addAttribute("tipoEtapa", tipoEtapa);
        model.addAttribute("etapaMostrada", contexto.etapa());
        model.addAttribute("slugEtapa", slugDe(tipoEtapa));
        model.addAttribute("operable", contexto.operable());
        model.addAttribute("avance", avance);
        model.addAttribute("resumen", resumir(requeridos, avance));
        model.addAttribute("consumos", consumos);
        model.addAttribute("estados", EstadoTransaccion.values());
        model.addAttribute("filtroEstado", filtroEstado);
        model.addAttribute("pestana", slugDeConsumo(tipoConsumo));
        model.addAttribute("urlPantalla", urlDeLaPantalla(idLote, tipoEtapa));
        model.addAttribute("urlNuevo", urlDeLaPantalla(idLote, tipoEtapa) + "/" + slugDeConsumo(tipoConsumo) + "/nuevo");
        return VISTA_PANTALLA;
    }

    // ==================== Formularios ====================

    /** Muestra el formulario del consumo de un lote de insumo reservado para la etapa. */
    @GetMapping("/reservado/nuevo")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_REGISTRAR')")
    public String mostrarFormularioReservado(@PathVariable Long idLote, @PathVariable String etapa, Model model,
                                             RedirectAttributes redirectAttributes) {
        return mostrarFormulario(idLote, etapa, TipoConsumo.RESERVADO, model, redirectAttributes);
    }

    /** Muestra el formulario del consumo directo de un lote de insumo de la etapa. */
    @GetMapping("/directo/nuevo")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_REGISTRAR')")
    public String mostrarFormularioDirecto(@PathVariable Long idLote, @PathVariable String etapa, Model model,
                                           RedirectAttributes redirectAttributes) {
        return mostrarFormulario(idLote, etapa, TipoConsumo.DIRECTO, model, redirectAttributes);
    }

    /**
     * Prepara la pantalla de formulario de un tipo de consumo. Con el lote fuera de ejecución o la etapa fuera de curso
     * no se registra nada: vuelve a la pestaña con un aviso. Si llega de un formulario inválido, muestra sus errores y
     * lo que el usuario había cargado.
     */
    private String mostrarFormulario(Long idLote, String etapa, TipoConsumo tipoConsumo, Model model, RedirectAttributes redirectAttributes) {
        TipoEtapa tipo = aEtapa(etapa);
        String rechazo = motivoDeRechazo(tipo);
        if (rechazo != null) {
            return volverAlDetalle(idLote, rechazo, redirectAttributes);
        }

        ContextoDeEtapa contexto = resolverContexto(idLote, tipo);
        List<InsumoRequeridoResponseDTO> requeridos = consumoInsumoServicio.filtrarInsumosRequeridos(contexto.etapa().getId());
        if (requeridos.isEmpty()) {
            return volverAlDetalle(idLote, MENSAJE_SIN_INSUMOS, redirectAttributes);
        }
        if (!contexto.operable()) {
            agregarMensaje(redirectAttributes, MENSAJE_NO_OPERABLE, TipoAlerta.WARNING);
            return volverALaPestana(idLote, tipo, tipoConsumo);
        }

        if (model.containsAttribute(ATRIBUTO_FORMULARIO)) {
            BindingResult errores = (BindingResult) model.asMap().get(BindingResult.MODEL_KEY_PREFIX + ATRIBUTO_FORMULARIO);
            if (errores != null) {
                cargarLoEscrito(model, errores, requeridos);
            }
        } else {
            model.addAttribute(ATRIBUTO_FORMULARIO, new ConsumoInsumoFormDTO());
        }

        String urlPantalla = urlDeLaPantalla(idLote, tipo);
        model.addAttribute("lote", contexto.lote());
        model.addAttribute("tipoEtapa", tipo);
        model.addAttribute("etapaMostrada", contexto.etapa());
        model.addAttribute("slugEtapa", slugDe(tipo));
        model.addAttribute("pestana", slugDeConsumo(tipoConsumo));
        model.addAttribute("reservado", tipoConsumo == TipoConsumo.RESERVADO);
        model.addAttribute("urlAccion", urlPantalla + "/" + slugDeConsumo(tipoConsumo));
        model.addAttribute("urlCancelar", urlPantalla + "?tipo=" + slugDeConsumo(tipoConsumo));
        model.addAttribute("urlBuscadorInsumos", urlPantalla + "/" + slugDeConsumo(tipoConsumo) + "/buscador/insumos");
        model.addAttribute("urlBuscador", urlPantalla + "/" + slugDeConsumo(tipoConsumo) + "/buscador/lotes-insumo");
        return VISTA_FORMULARIO;
    }

    // ==================== Registrar ====================

    /**
     * Registra el consumo de un lote de insumo reservado para la etapa y vuelve a la pestaña "Reservados". Si el
     * formulario es inválido, vuelve al formulario con los errores y lo que el usuario había cargado.
     */
    @PostMapping("/reservado")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_REGISTRAR')")
    public String registrarConsumoReservado(@PathVariable Long idLote,
                                            @PathVariable String etapa,
                                            @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ConsumoInsumoFormDTO consumoForm,
                                            BindingResult bindingResult,
                                            RedirectAttributes redirectAttributes) {
        return registrar(idLote, etapa, consumoForm, bindingResult, redirectAttributes, TipoConsumo.RESERVADO);
    }

    /**
     * Registra el consumo directo de un lote de insumo elegido con el buscador y vuelve a la pestaña "Directos". Si el
     * formulario es inválido, vuelve al formulario con los errores, el lote elegido y la cantidad escrita.
     */
    @PostMapping("/directo")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_REGISTRAR')")
    public String registrarConsumoDirecto(@PathVariable Long idLote,
                                          @PathVariable String etapa,
                                          @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ConsumoInsumoFormDTO consumoForm,
                                          BindingResult bindingResult,
                                          RedirectAttributes redirectAttributes) {
        return registrar(idLote, etapa, consumoForm, bindingResult, redirectAttributes, TipoConsumo.DIRECTO);
    }

    private String registrar(Long idLote, String etapa, ConsumoInsumoFormDTO consumoForm, BindingResult bindingResult,
                             RedirectAttributes redirectAttributes, TipoConsumo tipoConsumo) {
        TipoEtapa tipo = aEtapa(etapa);
        String rechazo = motivoDeRechazo(tipo);
        if (rechazo != null) {
            return volverAlDetalle(idLote, rechazo, redirectAttributes);
        }

        ContextoDeEtapa contexto = resolverContexto(idLote, tipo);
        if (consumoInsumoServicio.filtrarInsumosRequeridos(contexto.etapa().getId()).isEmpty()) {
            return volverAlDetalle(idLote, MENSAJE_SIN_INSUMOS, redirectAttributes);
        }

        if (bindingResult.hasErrors()) {
            // Post-Redirect-Get: el navegador no queda en la URL del POST, que no tiene pantalla propia
            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + ATRIBUTO_FORMULARIO, bindingResult);
            redirectAttributes.addFlashAttribute(ATRIBUTO_FORMULARIO, consumoForm);
            return "redirect:" + urlDeLaPantalla(idLote, tipo) + "/" + slugDeConsumo(tipoConsumo) + "/nuevo";
        }

        ConsumoInsumoResponseDTO consumo = tipoConsumo == TipoConsumo.RESERVADO
                ? consumoInsumoServicio.registrarConsumoInsumoReservado(contexto.etapa().getId(), consumoForm)
                : consumoInsumoServicio.registrarConsumoInsumoDirecto(contexto.etapa().getId(), consumoForm);

        agregarMensaje(redirectAttributes, "Consumo registrado correctamente", TipoAlerta.SUCCESS);
        if (consumo.getAdvertencias() != null && !consumo.getAdvertencias().isEmpty()) {
            redirectAttributes.addFlashAttribute("advertencias", consumo.getAdvertencias());
        }
        return volverALaPestana(idLote, tipo, tipoConsumo);
    }

    // ==================== Anular ====================

    /**
     * Anula un consumo de la etapa y vuelve a la pestaña de su tipo. Verifica que el consumo pertenezca a la etapa (y,
     * por ella, al lote) de la ruta. El motivo es obligatorio y lo valida el service: si falta, o si el consumo no se
     * puede anular, el {@code ControllerAdvices} vuelve al origen con el motivo del rechazo.
     */
    @PostMapping("/{idConsumo}/anular")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_ANULAR')")
    public String anularConsumo(@PathVariable Long idLote,
                                @PathVariable String etapa,
                                @PathVariable Long idConsumo,
                                @RequestParam(required = false) String motivo,
                                RedirectAttributes redirectAttributes) {
        TipoEtapa tipo = aEtapa(etapa);
        String rechazo = motivoDeRechazo(tipo);
        if (rechazo != null) {
            return volverAlDetalle(idLote, rechazo, redirectAttributes);
        }

        ContextoDeEtapa contexto = resolverContexto(idLote, tipo);
        ConsumoInsumoResponseDTO consumo = consumoInsumoServicio.buscarPorId(idConsumo);
        if (consumo.getEtapaLote() == null || !Objects.equals(consumo.getEtapaLote().getId(), contexto.etapa().getId())) {
            agregarMensaje(redirectAttributes, "El consumo no pertenece a esta etapa del lote", TipoAlerta.WARNING);
            return "redirect:" + urlDeLaPantalla(idLote, tipo);
        }

        consumoInsumoServicio.anularConsumoInsumo(idConsumo, AnularConsumoInsumoFormDTO.builder().motivoAnulacion(motivo).build());
        agregarMensaje(redirectAttributes, "Consumo anulado correctamente", TipoAlerta.SUCCESS);
        return volverALaPestana(idLote, tipo, consumo.getTipoConsumo());
    }

    // ==================== Buscadores ====================

    /**
     * Devuelve el contenido del modal de búsqueda de insumos del consumo reservado, de los insumos de la etapa que
     * tienen saldo reservado. El criterio opcional "nombre" filtra por el nombre del insumo. Un modal no puede
     * redirigir: una etapa que no admite consumos o un lote inexistente se informan dentro del propio fragmento.
     */
    @GetMapping("/reservado/buscador/insumos")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_REGISTRAR')")
    public String buscarInsumosReservados(@PathVariable Long idLote,
                                          @PathVariable String etapa,
                                          @RequestParam(required = false) String nombre,
                                          Model model) {
        return buscarInsumos(idLote, etapa, TipoConsumo.RESERVADO, nombre, model);
    }

    /**
     * Devuelve el contenido del modal de búsqueda de insumos del consumo directo, de los insumos que la etapa requiere.
     * El criterio opcional "nombre" filtra por el nombre del insumo.
     */
    @GetMapping("/directo/buscador/insumos")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_REGISTRAR')")
    public String buscarInsumosDirectos(@PathVariable Long idLote,
                                        @PathVariable String etapa,
                                        @RequestParam(required = false) String nombre,
                                        Model model) {
        return buscarInsumos(idLote, etapa, TipoConsumo.DIRECTO, nombre, model);
    }

    private String buscarInsumos(Long idLote, String etapa, TipoConsumo tipoConsumo, String nombre, Model model) {
        TipoEtapa tipo = aEtapa(etapa);
        String rechazo = motivoDeRechazo(tipo);
        if (rechazo != null) {
            model.addAttribute("mensajeBuscador", rechazo);
            return VISTA_BUSCADOR_INSUMO;
        }

        List<InsumoRequeridoResponseDTO> ofrecidos;
        try {
            ofrecidos = insumosOfrecidos(resolverContexto(idLote, tipo), tipoConsumo);
        } catch (RecursoNoEncontradoException e) {
            model.addAttribute("mensajeBuscador", e.getMessage());
            return VISTA_BUSCADOR_INSUMO;
        }

        String criterio = vacioANulo(nombre);
        List<FilaDeInsumo> filas = ofrecidos.stream()
                .filter(requerido -> criterio == null
                        || requerido.getInsumo().getNombre().toLowerCase(Locale.ROOT).contains(criterio.toLowerCase(Locale.ROOT)))
                .map(requerido -> new FilaDeInsumo(requerido, tipoDeInsumo(requerido.getInsumo())))
                .toList();

        model.addAttribute("filas", filas);
        model.addAttribute("reservado", tipoConsumo == TipoConsumo.RESERVADO);
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("urlBuscador", urlDeLaPantalla(idLote, tipo) + "/" + slugDeConsumo(tipoConsumo) + "/buscador/insumos");
        return VISTA_BUSCADOR_INSUMO;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de lotes de insumo del consumo reservado (criterio de identificación
     * del lote + resultados), de a 8. Solo se ofrecen los lotes del insumo elegido ({@code idInsumo}) con saldo
     * reservado para la etapa; el orden (FEFO: vigentes por vencimiento, luego vencidos) lo da el service y no se
     * reordena. Un modal no puede redirigir: una etapa que no admite consumos, un lote inexistente o un insumo que no
     * se eligió (o que la etapa no ofrece) se informan dentro del propio fragmento.
     */
    @GetMapping("/reservado/buscador/lotes-insumo")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_REGISTRAR')")
    public String buscarLotesInsumoReservados(@PathVariable Long idLote,
                                              @PathVariable String etapa,
                                              @RequestParam(required = false) Long idInsumo,
                                              @RequestParam(required = false) String identificacion,
                                              @RequestParam(defaultValue = "0") int pagina,
                                              Model model) {
        return buscarLotesInsumo(idLote, etapa, TipoConsumo.RESERVADO, idInsumo, identificacion, pagina, model);
    }

    /**
     * Devuelve el contenido del modal de búsqueda de lotes de insumo del consumo directo, de a 8. Se ofrecen todos los
     * lotes con saldo disponible del insumo elegido, en orden FEFO con los vencidos al final.
     */
    @GetMapping("/directo/buscador/lotes-insumo")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_REGISTRAR')")
    public String buscarLotesInsumoDirectos(@PathVariable Long idLote,
                                            @PathVariable String etapa,
                                            @RequestParam(required = false) Long idInsumo,
                                            @RequestParam(required = false) String identificacion,
                                            @RequestParam(defaultValue = "0") int pagina,
                                            Model model) {
        return buscarLotesInsumo(idLote, etapa, TipoConsumo.DIRECTO, idInsumo, identificacion, pagina, model);
    }

    private String buscarLotesInsumo(Long idLote, String etapa, TipoConsumo tipoConsumo, Long idInsumo, String identificacion,
                                     int pagina, Model model) {
        TipoEtapa tipo = aEtapa(etapa);
        String rechazo = motivoDeRechazo(tipo);
        if (rechazo != null) {
            model.addAttribute("mensajeBuscador", rechazo);
            return VISTA_BUSCADOR;
        }

        ContextoDeEtapa contexto;
        List<InsumoRequeridoResponseDTO> ofrecidos;
        try {
            contexto = resolverContexto(idLote, tipo);
            ofrecidos = insumosOfrecidos(contexto, tipoConsumo);
        } catch (RecursoNoEncontradoException e) {
            model.addAttribute("mensajeBuscador", e.getMessage());
            return VISTA_BUSCADOR;
        }
        if (idInsumo == null || ofrecidos.stream().noneMatch(requerido -> idInsumo.equals(requerido.getInsumo().getId()))) {
            model.addAttribute("mensajeBuscador", MENSAJE_ELEGIR_INSUMO);
            return VISTA_BUSCADOR;
        }

        PageRequest paginaPedida = PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR);
        Page<FilaDeBuscador> filas;
        if (tipoConsumo == TipoConsumo.RESERVADO) {
            filas = reservaInsumoServicio.filtrarReservasConSaldo(contexto.etapa().getId(), idInsumo, vacioANulo(identificacion), paginaPedida)
                    .map(reserva -> new FilaDeBuscador(reserva.getLoteInsumo(), reserva.getCantidadReservada()));
        } else {
            filas = loteInsumoServicio.filtrarLotesInsumoDisponibles(List.of(idInsumo), null, vacioANulo(identificacion), paginaPedida)
                    .map(lote -> new FilaDeBuscador(lote, lote.getCantidadDisponible()));
        }

        model.addAttribute("filas", filas);
        model.addAttribute("tituloCantidad", tipoConsumo == TipoConsumo.RESERVADO ? "Reservado" : "Disponible");
        model.addAttribute("idInsumo", idInsumo);
        model.addAttribute("filtroIdentificacion", identificacion);
        model.addAttribute("urlBuscador", urlDeLaPantalla(idLote, tipo) + "/" + slugDeConsumo(tipoConsumo) + "/buscador/lotes-insumo");
        return VISTA_BUSCADOR;
    }

    // ==================== Apoyo ====================

    /** El aviso por el que la etapa de la ruta no tiene pantalla de consumos, o {@code null} si la tiene. */
    private static String motivoDeRechazo(TipoEtapa tipo) {
        if (tipo == null) {
            return MENSAJE_ETAPA_INVALIDA;
        }
        return tipo.isPermiteRegistrarConsumo() ? null : "La etapa de " + tipo.getEtiqueta() + " no admite consumos de insumo";
    }

    /**
     * Los insumos de la etapa que ofrece el consumo: los que tienen saldo reservado en el consumo reservado y todos los
     * que la etapa requiere en el consumo directo.
     */
    private List<InsumoRequeridoResponseDTO> insumosOfrecidos(ContextoDeEtapa contexto, TipoConsumo tipoConsumo) {
        List<InsumoRequeridoResponseDTO> requeridos = consumoInsumoServicio.filtrarInsumosRequeridos(contexto.etapa().getId());
        if (tipoConsumo == TipoConsumo.DIRECTO) {
            return requeridos;
        }
        return requeridos.stream()
                .filter(requerido -> requerido.getCantidadReservada() != null && requerido.getCantidadReservada() > 0)
                .toList();
    }

    /** El tipo de insumo para mostrar: Malta, Lúpulo o Levadura. */
    private static String tipoDeInsumo(InsumoResponseDTO insumo) {
        if (insumo instanceof MaltaResponseDTO) {
            return "Malta";
        }
        return insumo instanceof LupuloResponseDTO ? "Lúpulo" : "Levadura";
    }

    /** Busca el lote (si no existe lo resuelve el {@code ControllerAdvices}) y toma de él la etapa del tipo pedido. */
    private ContextoDeEtapa resolverContexto(Long idLote, TipoEtapa tipo) {
        LoteDetalleResponseDTO lote = loteServicio.buscarDetallePorId(idLote);
        EtapaLoteResponseDTO etapa = lote.getEtapas().stream()
                .filter(e -> e.getEtapa() == tipo)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la etapa " + tipo.getEtiqueta() + " del lote con ID: " + idLote));
        return new ContextoDeEtapa(lote, tipo, etapa);
    }

    /**
     * Lo que el usuario había cargado en el formulario que falló: la cantidad tal como la escribió (aunque no fuera un
     * número), el insumo elegido (si la etapa lo ofrece) y el lote de insumo elegido. Un lote de insumo que ya no existe deja el selector vacío.
     */
    private void cargarLoEscrito(Model model, BindingResult bindingResult, List<InsumoRequeridoResponseDTO> requeridos) {
        ConsumoInsumoFormDTO consumoForm = (ConsumoInsumoFormDTO) bindingResult.getTarget();
        Object escrito = bindingResult.getFieldValue("cantidadConsumida");
        model.addAttribute("cantidadEscrita", escrito == null ? null : numeroEscrito(consumoForm.getCantidadConsumida(), escrito));

        if (consumoForm.getIdInsumo() != null) {
            requeridos.stream()
                    .filter(requerido -> consumoForm.getIdInsumo().equals(requerido.getInsumo().getId()))
                    .findFirst()
                    .ifPresent(requerido -> {
                        model.addAttribute("idInsumoElegido", requerido.getInsumo().getId());
                        model.addAttribute("insumoElegidoTexto", requerido.getInsumo().getNombre());
                        model.addAttribute("unidadInsumoElegido", requerido.getUnidadDeMedida().getSimbolo());
                    });
        }

        if (consumoForm.getIdLoteInsumo() != null) {
            model.addAttribute("idLoteElegido", consumoForm.getIdLoteInsumo());
            try {
                LoteInsumoResponseDTO loteElegido = loteInsumoServicio.buscarPorId(consumoForm.getIdLoteInsumo());
                model.addAttribute("loteElegido", loteElegido);
                model.addAttribute("loteElegidoTexto", loteElegido.getIdentificacionLoteProveedor());
            } catch (RecursoNoEncontradoException e) {
                model.addAttribute("loteElegido", null);
            }
        }
    }

    private static List<ResumenDeInsumo> resumir(List<InsumoRequeridoResponseDTO> requeridos, AvanceEtapaResponseDTO avance) {
        return requeridos.stream().map(requerido -> {
            AvanceInsumoResponseDTO avanceDelInsumo = avance.getInsumos().stream()
                    .filter(a -> Objects.equals(a.getNombreInsumo(), requerido.getInsumo().getNombre()))
                    .findFirst()
                    .orElse(null);
            return avanceDelInsumo != null
                    ? new ResumenDeInsumo(requerido, avanceDelInsumo.getPorcentaje(), avanceDelInsumo.isCumple())
                    : new ResumenDeInsumo(requerido, requerido.getPorcentajeConsumido(), true);
        }).toList();
    }

    /** El estado con ese nombre, o {@code null} si el texto está vacío o no corresponde a ninguno (se muestran todos). */
    private static EstadoTransaccion aEstadoTransaccion(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return EstadoTransaccion.valueOf(limpio.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** La pestaña pedida: "directo" es el consumo directo; cualquier otro valor, o ninguno, la pestaña "Reservados". */
    private static TipoConsumo aTipoConsumo(String texto) {
        return "directo".equalsIgnoreCase(vacioANulo(texto)) ? TipoConsumo.DIRECTO : TipoConsumo.RESERVADO;
    }

    private static String slugDeConsumo(TipoConsumo tipoConsumo) {
        return tipoConsumo == TipoConsumo.DIRECTO ? "directo" : "reservado";
    }

    private static String urlDeLaPantalla(Long idLote, TipoEtapa tipo) {
        return "/lotes/" + idLote + "/etapas/" + slugDe(tipo) + "/consumos";
    }

    /** Vuelve a la pestaña del tipo de consumo de la misma etapa (Post-Redirect-Get). */
    private static String volverALaPestana(Long idLote, TipoEtapa tipo, TipoConsumo tipoConsumo) {
        return "redirect:" + urlDeLaPantalla(idLote, tipo) + "?tipo=" + slugDeConsumo(tipoConsumo);
    }

    /** La etapa no tiene pantalla de consumos: vuelve al detalle del lote con el aviso. */
    private static String volverAlDetalle(Long idLote, String aviso, RedirectAttributes redirectAttributes) {
        agregarMensaje(redirectAttributes, aviso, TipoAlerta.WARNING);
        return "redirect:/lotes/" + idLote;
    }
}
