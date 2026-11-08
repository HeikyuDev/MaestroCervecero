package com.github.heikyudev.maestrocervecero.presentation.controller.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.AnularEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.RegistrarEnvasadoLoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFabricanteBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEnvasadoLoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EnvasadoLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.EtapaLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ResumenEnvasadoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
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

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de las pantallas de envasados del lote: la pantalla de gestión (el indicador del envasado frente a la
 * capacidad útil del fermentador y la lista de los envasados registrados con su anulación), la pantalla de formulario de
 * un envasado nuevo y el buscador en modal de barriles.
 * <p>
 * Las pantallas son de operación: cada método se autoriza por permiso ({@code hasAuthority}) y las usa el operario de
 * producción. El id de la etapa nunca viaja en un formulario: el Controller lo resuelve a partir del lote de la ruta
 * (su etapa de Envasado) y verifica que el envasado a anular pertenezca a esa etapa. Las reglas del envasado son del
 * service; un rechazo lo muestra el {@code ControllerAdvices} volviendo al origen con el motivo. Después de registrar
 * o anular se vuelve a la lista (Post-Redirect-Get). Un formulario inválido vuelve a su pantalla de formulario por
 * Post-Redirect-Get, con los errores y lo escrito en los atributos flash: el navegador nunca queda en la URL de un POST.
 * </p>
 */
@Controller
@RequestMapping("/lotes/{idLote}/envasados")
@RequiredArgsConstructor
public class EnvasadoLoteController {

    private static final String VISTA_PANTALLA = "lote/envasado-lista";
    private static final String VISTA_FORMULARIO = "lote/envasado-form";
    private static final String MENSAJE_NO_OPERABLE = "Solo se pueden registrar envasados con el lote en ejecución y la etapa en curso";
    private static final String VISTA_BUSCADOR = "lote/buscador-barril :: buscador";
    private static final String ATRIBUTO_FORMULARIO = "envasadoForm";

    /**
     * Campos del formulario de registro a los que puede referirse un rechazo del service.
     */
    private static final Set<String> CAMPOS_DEL_FORMULARIO = Set.of("idBarril", "cantidad", "fechaEnvasado");

    /**
     * Los barriles son maestros: el buscador los ofrece en orden alfabético ascendente por identificador interno.
     */
    private static final Sort ORDEN_BARRILES = Sort.by(Sort.Order.asc("identificadorInterno"), Sort.Order.asc("id"));

    private final ILoteServicio loteServicio;
    private final IEnvasadoLoteServicio envasadoLoteServicio;
    private final IBarrilServicio barrilServicio;
    private final IFabricanteBarrilServicio fabricanteBarrilServicio;

    /** La etapa de Envasado de la ruta ya resuelta a partir del lote. */
    private record ContextoDeEnvasado(LoteDetalleResponseDTO lote, EtapaLoteResponseDTO etapa) {

        /** Solo se registran y anulan envasados con el lote en ejecución y la etapa en curso. */
        boolean operable() {
            return esOperable(lote.getEstado(), etapa.getEstado());
        }
    }

    // ==================== Pantalla ====================

    /**
     * Muestra la pantalla de envasados del lote: el indicador del envasado y los envasados de a 20 con el último
     * registrado arriba. El criterio opcional "estado" (Registrado o Anulado) filtra la lista; sin él, o con un valor
     * inválido (se avisa), se muestran en todos los estados. Con el lote fuera de ejecución o la etapa de Envasado fuera
     * de curso la pantalla es de solo lectura. Si el envasado no se puede resumir, vuelve al detalle del lote con el
     * motivo.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ENVASADO_CONSULTAR')")
    public String mostrarEnvasados(@PathVariable Long idLote,
                                   @RequestParam(required = false) String estado,
                                   @RequestParam(defaultValue = "0") int pagina,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        EstadoTransaccion filtroEstado = aEstadoTransaccion(estado);
        if (vacioANulo(estado) != null && filtroEstado == null) {
            model.addAttribute("mensaje", "El estado indicado no es válido, se muestran todos los envasados");
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }

        ContextoDeEnvasado contexto = resolverContexto(idLote);
        Long idEtapa = contexto.etapa().getId();
        ResumenEnvasadoResponseDTO resumen;
        try {
            resumen = envasadoLoteServicio.resumirEnvasado(idEtapa);
        } catch (ReglaNegocioException e) {
            agregarMensaje(redirectAttributes, e.getMessage(), TipoAlerta.WARNING);
            return "redirect:/lotes/" + idLote;
        }
        Page<EnvasadoLoteResponseDTO> envasados = envasadoLoteServicio.filtrarEnvasadosLote(idEtapa, null, filtroEstado,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_MAS_RECIENTE_PRIMERO));

        model.addAttribute("lote", contexto.lote());
        model.addAttribute("etapaMostrada", contexto.etapa());
        model.addAttribute("slugEtapa", slugDe(TipoEtapa.ENVASADO));
        model.addAttribute("operable", contexto.operable());
        model.addAttribute("resumen", resumen);
        model.addAttribute("porcentaje", porcentajeDe(resumen));
        model.addAttribute("envasados", envasados);
        model.addAttribute("estados", EstadoTransaccion.values());
        model.addAttribute("filtroEstado", filtroEstado);
        model.addAttribute("urlPantalla", urlDeLaPantalla(idLote));
        model.addAttribute("urlNuevo", urlDeLaPantalla(idLote) + "/nuevo");
        return VISTA_PANTALLA;
    }

    // ==================== Formulario ====================

    /**
     * Muestra la pantalla de formulario de un envasado nuevo. Con el lote fuera de ejecución o la etapa de Envasado
     * fuera de curso no se registra nada: vuelve a la lista con un aviso. Si llega de un formulario inválido, muestra
     * sus errores y lo que el usuario había cargado.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasAuthority('ENVASADO_REGISTRAR')")
    public String mostrarFormulario(@PathVariable Long idLote, Model model, RedirectAttributes redirectAttributes) {
        ContextoDeEnvasado contexto = resolverContexto(idLote);
        if (!contexto.operable()) {
            agregarMensaje(redirectAttributes, MENSAJE_NO_OPERABLE, TipoAlerta.WARNING);
            return volverALaPantalla(idLote);
        }

        if (model.containsAttribute(ATRIBUTO_FORMULARIO)) {
            BindingResult errores = (BindingResult) model.asMap().get(BindingResult.MODEL_KEY_PREFIX + ATRIBUTO_FORMULARIO);
            if (errores != null) {
                cargarLoEscrito(model, errores);
            }
        } else {
            model.addAttribute(ATRIBUTO_FORMULARIO, new RegistrarEnvasadoLoteFormDTO());
        }

        model.addAttribute("lote", contexto.lote());
        model.addAttribute("etapaMostrada", contexto.etapa());
        model.addAttribute("urlAccion", urlDeLaPantalla(idLote));
        model.addAttribute("urlCancelar", urlDeLaPantalla(idLote));
        model.addAttribute("urlBuscador", urlDeLaPantalla(idLote) + "/buscador/barriles");
        return VISTA_FORMULARIO;
    }

    // ==================== Registrar ====================

    /**
     * Registra un envasado en el barril elegido con el buscador y vuelve a la lista. Si el formulario es inválido, o
     * el service rechaza un campo del formulario (por ejemplo, la fecha y hora del envasado), vuelve al formulario con
     * el error en su campo, el barril elegido y lo escrito. Los demás rechazos del service los muestra el
     * {@code ControllerAdvices}.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ENVASADO_REGISTRAR')")
    public String registrarEnvasado(@PathVariable Long idLote,
                                    @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) RegistrarEnvasadoLoteFormDTO envasadoForm,
                                    BindingResult bindingResult,
                                    RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            // Post-Redirect-Get: el navegador no queda en la URL del POST
            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + ATRIBUTO_FORMULARIO, bindingResult);
            redirectAttributes.addFlashAttribute(ATRIBUTO_FORMULARIO, envasadoForm);
            return volverAlFormulario(idLote);
        }

        ContextoDeEnvasado contexto = resolverContexto(idLote);
        try {
            envasadoLoteServicio.registrarEnvasadoLote(contexto.etapa().getId(), envasadoForm);
        } catch (ReglaNegocioException e) {
            if (e.getCampo() == null || !CAMPOS_DEL_FORMULARIO.contains(e.getCampo())) {
                throw e;
            }
            bindingResult.rejectValue(e.getCampo(), "reglaNegocio", e.getMessage());
            redirectAttributes.addFlashAttribute(BindingResult.MODEL_KEY_PREFIX + ATRIBUTO_FORMULARIO, bindingResult);
            redirectAttributes.addFlashAttribute(ATRIBUTO_FORMULARIO, envasadoForm);
            return volverAlFormulario(idLote);
        }
        agregarMensaje(redirectAttributes, "Envasado creado correctamente", TipoAlerta.SUCCESS);
        return volverALaPantalla(idLote);
    }

    // ==================== Anular ====================

    /**
     * Anula un envasado de la etapa de Envasado y vuelve a la pantalla. Verifica que el envasado pertenezca a la etapa
     * (y, por ella, al lote) de la ruta. El motivo es obligatorio y lo valida el service: si falta, o si el envasado no
     * se puede anular, el {@code ControllerAdvices} vuelve al origen con el motivo del rechazo.
     */
    @PostMapping("/{idEnvasado}/anular")
    @PreAuthorize("hasAuthority('ENVASADO_ANULAR')")
    public String anularEnvasado(@PathVariable Long idLote,
                                 @PathVariable Long idEnvasado,
                                 @RequestParam(required = false) String motivo,
                                 RedirectAttributes redirectAttributes) {
        ContextoDeEnvasado contexto = resolverContexto(idLote);
        EnvasadoLoteResponseDTO envasado = envasadoLoteServicio.buscarPorId(idEnvasado);
        if (envasado.getEtapaLote() == null || !Objects.equals(envasado.getEtapaLote().getId(), contexto.etapa().getId())) {
            agregarMensaje(redirectAttributes, "El envasado no pertenece a este lote", TipoAlerta.WARNING);
            return volverALaPantalla(idLote);
        }

        envasadoLoteServicio.anularEnvasadoLote(idEnvasado, AnularEnvasadoLoteFormDTO.builder().motivoAnulacion(motivo).build());
        agregarMensaje(redirectAttributes, "Envasado anulado correctamente", TipoAlerta.SUCCESS);
        return volverALaPantalla(idLote);
    }

    // ==================== Buscador ====================

    /**
     * Devuelve el contenido del modal de búsqueda de barriles (criterios de identificador interno, número de serie,
     * capacidad y fabricante + resultados), de a 8. Solo se ofrecen barriles Disponibles, en orden alfabético por
     * identificador interno. Un fabricante que ya no existe no restringe la búsqueda. Un modal no puede redirigir: un lote
     * inexistente se informa dentro del propio fragmento.
     */
    @GetMapping("/buscador/barriles")
    @PreAuthorize("hasAuthority('ENVASADO_REGISTRAR')")
    public String buscarBarriles(@PathVariable Long idLote,
                                 @RequestParam(required = false) String identificadorInterno,
                                 @RequestParam(required = false) String numeroSerie,
                                 @RequestParam(required = false) Double capacidad,
                                 @RequestParam(required = false) Long idFabricante,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        try {
            loteServicio.buscarDetallePorId(idLote);
        } catch (RecursoNoEncontradoException e) {
            model.addAttribute("mensajeBuscador", e.getMessage());
            return VISTA_BUSCADOR;
        }

        String fabricanteTexto = nombreFabricante(idFabricante);
        Long idFabricanteVigente = fabricanteTexto == null ? null : idFabricante;

        Page<BarrilResponseDTO> barriles = barrilServicio.filtrarBarriles(vacioANulo(identificadorInterno), vacioANulo(numeroSerie), capacidad,
                EstadoOperativoBarril.DISPONIBLE, idFabricanteVigente,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR, ORDEN_BARRILES));

        model.addAttribute("barriles", barriles);
        model.addAttribute("filtroIdentificador", identificadorInterno);
        model.addAttribute("filtroNumeroSerie", numeroSerie);
        // El valor ya viene convertido a número: numeroEscrito solo le quita el ".0" (50.0 se muestra como 50)
        model.addAttribute("filtroCapacidad", capacidad == null ? null : numeroEscrito(capacidad, capacidad));
        model.addAttribute("filtroIdFabricante", idFabricanteVigente);
        model.addAttribute("filtroFabricanteTexto", fabricanteTexto);
        model.addAttribute("urlBuscador", urlDeLaPantalla(idLote) + "/buscador/barriles");
        return VISTA_BUSCADOR;
    }

    // ==================== Apoyo ====================

    /** Busca el lote (si no existe lo resuelve el {@code ControllerAdvices}) y toma de él su etapa de Envasado. */
    private ContextoDeEnvasado resolverContexto(Long idLote) {
        LoteDetalleResponseDTO lote = loteServicio.buscarDetallePorId(idLote);
        EtapaLoteResponseDTO etapa = lote.getEtapas().stream()
                .filter(e -> e.getEtapa() == TipoEtapa.ENVASADO)
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la etapa " + TipoEtapa.ENVASADO.getEtiqueta() + " del lote con ID: " + idLote));
        return new ContextoDeEnvasado(lote, etapa);
    }

    /** El nombre del fabricante elegido en el buscador, o {@code null} si no hay selección o ya no existe. */
    private String nombreFabricante(Long idFabricante) {
        if (idFabricante == null) {
            return null;
        }
        try {
            return fabricanteBarrilServicio.buscarPorId(idFabricante).getNombre();
        } catch (RecursoNoEncontradoException e) {
            return null;
        }
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

    /**
     * Lo que el usuario había cargado en el formulario que falló: la cantidad tal como la escribió (aunque no fuera un
     * número) y el barril elegido. Un barril que ya no existe deja el selector vacío.
     */
    private void cargarLoEscrito(Model model, BindingResult bindingResult) {
        RegistrarEnvasadoLoteFormDTO envasadoForm = (RegistrarEnvasadoLoteFormDTO) bindingResult.getTarget();
        Object escrito = bindingResult.getFieldValue("cantidad");
        model.addAttribute("formularioConError", true);
        model.addAttribute("cantidadEscrita", escrito == null ? null : numeroEscrito(envasadoForm.getCantidad(), escrito));

        if (envasadoForm.getIdBarril() != null) {
            model.addAttribute("idBarrilElegido", envasadoForm.getIdBarril());
            try {
                BarrilResponseDTO barrilElegido = barrilServicio.buscarPorId(envasadoForm.getIdBarril());
                model.addAttribute("barrilElegido", barrilElegido);
                model.addAttribute("barrilElegidoTexto", barrilElegido.getIdentificadorInterno());
            } catch (RecursoNoEncontradoException e) {
                model.addAttribute("barrilElegido", null);
            }
        }
    }

    /** El avance del envasado hacia la capacidad útil del fermentador, de 0 a 100 y redondeado hacia abajo. */
    private static int porcentajeDe(ResumenEnvasadoResponseDTO resumen) {
        if (resumen.getCapacidadUtilFermentador() == null || resumen.getCapacidadUtilFermentador() <= 0 || resumen.getTotalEnvasado() == null) {
            return 0;
        }
        double porcentaje = resumen.getTotalEnvasado() * 100 / resumen.getCapacidadUtilFermentador();
        return (int) Math.max(0, Math.min(100, Math.floor(porcentaje)));
    }

    private static String urlDeLaPantalla(Long idLote) {
        return "/lotes/" + idLote + "/envasados";
    }

    /** Vuelve a la lista de envasados del lote (Post-Redirect-Get). */
    private static String volverALaPantalla(Long idLote) {
        return "redirect:" + urlDeLaPantalla(idLote);
    }

    /** Vuelve al formulario de un envasado nuevo con los errores y lo escrito (Post-Redirect-Get). */
    private static String volverAlFormulario(Long idLote) {
        return "redirect:" + urlDeLaPantalla(idLote) + "/nuevo";
    }
}
