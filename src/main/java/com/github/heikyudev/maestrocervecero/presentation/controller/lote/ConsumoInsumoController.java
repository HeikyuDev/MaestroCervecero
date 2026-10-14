package com.github.heikyudev.maestrocervecero.presentation.controller.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.ConsumoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.ILoteInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IConsumoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IReservaInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceEtapaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.AvanceInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EtapaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.InsumoRequeridoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de la pantalla de consumos de una etapa del lote: el resumen del avance por insumo, el consumo de los
 * insumos reservados, el consumo directo de cualquier lote de insumo de la etapa y la anulación de los consumos
 * registrados, más el buscador en modal de lotes de insumo.
 * <p>
 * La pantalla es de operación: cada método se autoriza por permiso ({@code hasAuthority}) y la usa el operario de
 * producción. El id de la etapa nunca viaja en un formulario: el Controller lo resuelve a partir del lote y del tipo
 * de etapa de la ruta, y verifica que el consumo a anular pertenezca a esa etapa. Las reglas del consumo son del
 * service; un rechazo lo muestra el {@code ControllerAdvices} volviendo al origen con el motivo. Después de registrar
 * o anular se vuelve a la misma pantalla (Post-Redirect-Get) y las advertencias del consumo viajan aparte para
 * mostrarse como alertas amarillas.
 * </p>
 */
@Controller
@RequestMapping("/lotes/{idLote}/etapas/{etapa}/consumos")
@RequiredArgsConstructor
public class ConsumoInsumoController {

    private static final String VISTA_PANTALLA = "lote/consumo-lista";
    private static final String VISTA_BUSCADOR = "lote/buscador-lote-insumo :: buscador";
    private static final String ATRIBUTO_FORMULARIO = "consumoForm";
    private static final String FORMULARIO_RESERVADO = "reservado";
    private static final String FORMULARIO_DIRECTO = "directo";
    private static final String MENSAJE_ETAPA_INVALIDA = "La etapa indicada no es válida";

    /** Los consumos se muestran con el último registrado arriba: el id es autogenerado y creciente. */
    private static final Sort ORDEN_MAS_RECIENTE_PRIMERO = Sort.by(Sort.Order.desc("id"));

    private final ILoteServicio loteServicio;
    private final IConsumoInsumoServicio consumoInsumoServicio;
    private final IReservaInsumoServicio reservaInsumoServicio;
    private final ILoteInsumoServicio loteInsumoServicio;

    /**
     * Los lotes de insumo reservados para un insumo requerido de la etapa: primero los que tienen saldo y después los
     * agotados.
     */
    @Value
    public static class ReservasDeInsumo {
        InsumoRequeridoResponseDTO requerido;
        List<ReservaInsumoResponseDTO> reservas;
    }

    /**
     * Una fila del resumen de la etapa: lo requerido, reservado y consumido de un insumo y su avance hacia el mínimo.
     */
    @Value
    public static class ResumenDeInsumo {
        InsumoRequeridoResponseDTO requerido;
        Double porcentaje;
        boolean cumple;
    }

    /** La etapa de la ruta ya resuelta a partir del lote. */
    private record ContextoDeEtapa(LoteDetalleResponseDTO lote, TipoEtapa tipo, EtapaLoteResponseDTO etapa) {

        /** Solo se registran y anulan consumos con el lote en ejecución y la etapa en curso. */
        boolean operable() {
            return lote.getEstado() == EstadoLote.EN_EJECUCION && etapa.getEstado() == EstadoEtapaLote.EN_CURSO;
        }
    }

    // ==================== Pantalla ====================

    /**
     * Muestra la pantalla de consumos de la etapa, con los consumos registrados de a 20 y el último registrado arriba.
     * Una etapa que no admite consumos o inexistente vuelve al detalle del lote con un aviso. Con el lote fuera de
     * ejecución o la etapa fuera de curso la pantalla es de solo lectura.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_CONSULTAR')")
    public String mostrarConsumos(@PathVariable Long idLote,
                                  @PathVariable String etapa,
                                  @RequestParam(defaultValue = "0") int pagina,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        TipoEtapa tipo = aEtapa(etapa);
        String rechazo = motivoDeRechazo(tipo);
        if (rechazo != null) {
            return volverAlDetalle(idLote, rechazo, redirectAttributes);
        }

        model.addAttribute(ATRIBUTO_FORMULARIO, new ConsumoInsumoFormDTO());
        cargarPantalla(model, resolverContexto(idLote, tipo), pagina, null, null);
        return VISTA_PANTALLA;
    }

    // ==================== Registrar ====================

    /**
     * Registra el consumo de un lote de insumo reservado para la etapa (cada fila de reserva es su propio formulario) y
     * vuelve a la pantalla. Si el formulario es inválido, se vuelve a mostrar la pantalla (sin redirigir) con lo que el
     * usuario había cargado.
     */
    @PostMapping("/reservado")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_REGISTRAR')")
    public String registrarConsumoReservado(@PathVariable Long idLote,
                                            @PathVariable String etapa,
                                            @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ConsumoInsumoFormDTO consumoForm,
                                            BindingResult bindingResult,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {
        return registrar(idLote, etapa, consumoForm, bindingResult, model, redirectAttributes, FORMULARIO_RESERVADO);
    }

    /**
     * Registra el consumo directo de un lote de insumo elegido con el buscador y vuelve a la pantalla. Si el
     * formulario es inválido, se vuelve a mostrar la pantalla (sin redirigir) con el lote elegido y la cantidad
     * escrita.
     */
    @PostMapping("/directo")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_REGISTRAR')")
    public String registrarConsumoDirecto(@PathVariable Long idLote,
                                          @PathVariable String etapa,
                                          @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ConsumoInsumoFormDTO consumoForm,
                                          BindingResult bindingResult,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        return registrar(idLote, etapa, consumoForm, bindingResult, model, redirectAttributes, FORMULARIO_DIRECTO);
    }

    private String registrar(Long idLote, String etapa, ConsumoInsumoFormDTO consumoForm, BindingResult bindingResult,
                             Model model, RedirectAttributes redirectAttributes, String formulario) {
        TipoEtapa tipo = aEtapa(etapa);
        String rechazo = motivoDeRechazo(tipo);
        if (rechazo != null) {
            return volverAlDetalle(idLote, rechazo, redirectAttributes);
        }

        ContextoDeEtapa contexto = resolverContexto(idLote, tipo);
        if (bindingResult.hasErrors()) {
            cargarPantalla(model, contexto, 0, bindingResult, formulario);
            return VISTA_PANTALLA;
        }

        ConsumoInsumoResponseDTO consumo = FORMULARIO_RESERVADO.equals(formulario)
                ? consumoInsumoServicio.registrarConsumoInsumoReservado(contexto.etapa().getId(), consumoForm)
                : consumoInsumoServicio.registrarConsumoInsumoDirecto(contexto.etapa().getId(), consumoForm);

        agregarMensaje(redirectAttributes, "Consumo registrado correctamente", TipoAlerta.SUCCESS);
        if (consumo.getAdvertencias() != null && !consumo.getAdvertencias().isEmpty()) {
            redirectAttributes.addFlashAttribute("advertencias", consumo.getAdvertencias());
        }
        return volverALaPantalla(idLote, tipo);
    }

    // ==================== Anular ====================

    /**
     * Anula un consumo de la etapa y vuelve a la pantalla. Verifica que el consumo pertenezca a la etapa (y, por ella,
     * al lote) de la ruta. El motivo es obligatorio y lo valida el service: si falta, o si el consumo no se puede
     * anular, el {@code ControllerAdvices} vuelve al origen con el motivo del rechazo.
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
            return volverALaPantalla(idLote, tipo);
        }

        consumoInsumoServicio.anularConsumoInsumo(idConsumo, AnularConsumoInsumoFormDTO.builder().motivoAnulacion(motivo).build());
        agregarMensaje(redirectAttributes, "Consumo anulado correctamente", TipoAlerta.SUCCESS);
        return volverALaPantalla(idLote, tipo);
    }

    // ==================== Buscador ====================

    /**
     * Devuelve el contenido del modal de búsqueda de lotes de insumo del consumo directo (criterios de insumo e
     * identificación del lote + resultados), de a 8. Solo se ofrecen lotes de los insumos que la etapa requiere, con
     * saldo disponible; el orden (vigentes por vencimiento, luego vencidos) lo da el service y no se reordena. Un modal
     * no puede redirigir: una etapa que no admite consumos se informa dentro del propio fragmento.
     */
    @GetMapping("/buscador/lotes-insumo")
    @PreAuthorize("hasAuthority('CONSUMO_INSUMO_REGISTRAR')")
    public String buscarLotesInsumo(@PathVariable Long idLote,
                                    @PathVariable String etapa,
                                    @RequestParam(required = false) String insumo,
                                    @RequestParam(required = false) String identificacion,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        TipoEtapa tipo = aEtapa(etapa);
        String rechazo = motivoDeRechazo(tipo);
        if (rechazo != null) {
            model.addAttribute("mensajeBuscador", rechazo);
            return VISTA_BUSCADOR;
        }

        ContextoDeEtapa contexto = resolverContexto(idLote, tipo);
        List<Long> idsInsumo = consumoInsumoServicio.filtrarInsumosRequeridos(contexto.etapa().getId()).stream()
                .map(requerido -> requerido.getInsumo().getId())
                .toList();
        Page<LoteInsumoResponseDTO> lotesInsumo = loteInsumoServicio.filtrarLotesInsumoDisponibles(
                idsInsumo, vacioANulo(insumo), vacioANulo(identificacion), PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA_BUSCADOR));

        model.addAttribute("lotesInsumo", lotesInsumo);
        model.addAttribute("filtroInsumo", insumo);
        model.addAttribute("filtroIdentificacion", identificacion);
        model.addAttribute("urlBuscador", urlDeLaPantalla(idLote, tipo) + "/buscador/lotes-insumo");
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
     * Carga todo lo que la pantalla muestra: el avance, los insumos requeridos con sus reservas (solo si se puede
     * operar) y los consumos de la etapa. Con errores de un formulario, además lo que el usuario había cargado.
     *
     * @param bindingResult El resultado del enlace del formulario que falló, o {@code null} si no hubo envío.
     * @param formulario    {@code "reservado"} o {@code "directo"}: qué formulario falló, o {@code null}.
     */
    private void cargarPantalla(Model model, ContextoDeEtapa contexto, int pagina, BindingResult bindingResult, String formulario) {
        Long idEtapa = contexto.etapa().getId();
        boolean operable = contexto.operable();

        List<InsumoRequeridoResponseDTO> requeridos = consumoInsumoServicio.filtrarInsumosRequeridos(idEtapa);
        AvanceEtapaResponseDTO avance = loteServicio.calcularAvanceDeEtapa(contexto.lote().getId(), contexto.tipo());
        Page<ConsumoInsumoResponseDTO> consumos = consumoInsumoServicio.filtrarConsumosInsumo(idEtapa, null, null, null,
                PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA, ORDEN_MAS_RECIENTE_PRIMERO));

        model.addAttribute("lote", contexto.lote());
        model.addAttribute("tipoEtapa", contexto.tipo());
        model.addAttribute("etapaMostrada", contexto.etapa());
        model.addAttribute("slugEtapa", slugDe(contexto.tipo()));
        model.addAttribute("operable", operable);
        model.addAttribute("avance", avance);
        model.addAttribute("resumen", resumir(requeridos, avance));
        model.addAttribute("reservas", operable ? reservasDe(idEtapa, requeridos) : List.of());
        model.addAttribute("consumos", consumos);
        model.addAttribute("urlPantalla", urlDeLaPantalla(contexto.lote().getId(), contexto.tipo()));
        model.addAttribute("urlBuscador", urlDeLaPantalla(contexto.lote().getId(), contexto.tipo()) + "/buscador/lotes-insumo");

        if (bindingResult != null) {
            cargarLoEscrito(model, bindingResult, formulario);
        }
    }

    /**
     * Lo que el usuario había cargado en el formulario que falló: la cantidad tal como la escribió (aunque no fuera un
     * número) y, en el consumo directo, el lote de insumo elegido. Una reserva que ya no existe deja el selector vacío.
     */
    private void cargarLoEscrito(Model model, BindingResult bindingResult, String formulario) {
        ConsumoInsumoFormDTO consumoForm = (ConsumoInsumoFormDTO) bindingResult.getTarget();
        Object escrito = bindingResult.getFieldValue("cantidadConsumida");
        model.addAttribute("formularioConError", formulario);
        model.addAttribute("cantidadEscrita", escrito == null ? null : numeroEscrito(consumoForm.getCantidadConsumida(), escrito));

        if (FORMULARIO_DIRECTO.equals(formulario) && consumoForm.getIdLoteInsumo() != null) {
            model.addAttribute("idLoteElegido", consumoForm.getIdLoteInsumo());
            try {
                LoteInsumoResponseDTO loteElegido = loteInsumoServicio.buscarPorId(consumoForm.getIdLoteInsumo());
                model.addAttribute("loteElegido", loteElegido);
                model.addAttribute("loteElegidoTexto", loteElegido.getInsumo().getNombre() + " - " + loteElegido.getIdentificacionLoteProveedor());
            } catch (RecursoNoEncontradoException e) {
                model.addAttribute("loteElegido", null);
            }
        }
    }

    /** Un número válido se devuelve sin ceros sobrantes ("10" y no "10.0"); un texto inválido, tal cual se escribió. */
    private static String numeroEscrito(Double valido, Object escrito) {
        if (valido == null) {
            return escrito.toString();
        }
        return valido == valido.longValue() ? String.valueOf(valido.longValue()) : valido.toString();
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

    /** Las reservas de cada insumo requerido, con las que tienen saldo primero y las agotadas al final. */
    private List<ReservasDeInsumo> reservasDe(Long idEtapa, List<InsumoRequeridoResponseDTO> requeridos) {
        return requeridos.stream()
                .map(requerido -> new ReservasDeInsumo(requerido, reservaInsumoServicio
                        .filtrarLotesInsumoReservados(idEtapa, requerido.getInsumo().getId()).stream()
                        .sorted(Comparator.comparing(reserva -> !tieneSaldo(reserva)))
                        .toList()))
                .toList();
    }

    private static boolean tieneSaldo(ReservaInsumoResponseDTO reserva) {
        return reserva.getCantidadReservada() != null && reserva.getCantidadReservada() > 0;
    }

    private static String urlDeLaPantalla(Long idLote, TipoEtapa tipo) {
        return "/lotes/" + idLote + "/etapas/" + slugDe(tipo) + "/consumos";
    }

    /** Vuelve a la pantalla de consumos de la misma etapa (Post-Redirect-Get). */
    private static String volverALaPantalla(Long idLote, TipoEtapa tipo) {
        return "redirect:" + urlDeLaPantalla(idLote, tipo);
    }

    /** La etapa no tiene pantalla de consumos: vuelve al detalle del lote con el aviso. */
    private static String volverAlDetalle(Long idLote, String aviso, RedirectAttributes redirectAttributes) {
        agregarMensaje(redirectAttributes, aviso, TipoAlerta.WARNING);
        return "redirect:/lotes/" + idLote;
    }
}
