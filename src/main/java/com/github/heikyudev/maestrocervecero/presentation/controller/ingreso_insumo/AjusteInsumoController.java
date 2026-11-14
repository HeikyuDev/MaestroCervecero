package com.github.heikyudev.maestrocervecero.presentation.controller.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AjusteInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularAjusteInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.IAjusteInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.ILoteInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.IMotivoAjusteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.stock_insumo.IStockInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.AjusteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.MotivoAjusteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.MensajeConLista;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de los ajustes de insumo ("Ajustes de Insumo"). Es del ENCARGADO_DE_DEPOSITO, que los registra, los anula y
 * los consulta; el GERENTE_DE_PRODUCCION solo los consulta. Cada método se autoriza por permiso ({@code hasAuthority}).
 * <p>
 * La pantalla principal lista los ajustes con el último registrado arriba, filtrables por lote de insumo, tipo, estado y
 * rango de fecha de registro. Un ajuste corrige la cantidad actual de un lote de insumo y su tipo (Ingreso o Egreso) lo
 * define el motivo elegido: el formulario no tiene un campo de tipo. En el formulario el insumo es un campo de ayuda de
 * pantalla que solo sirve para ofrecer los lotes de ese insumo (todos, también los vencidos y los agotados): al registrar
 * viaja el lote, no el insumo. El lote se puede llegar ya elegido desde el detalle del lote en Stock de Insumos
 * ({@code idLoteInsumo}).
 * </p>
 * <p>
 * Las reglas del ajuste son del service: un rechazo se muestra en el propio formulario, en el campo que corresponde
 * cuando la excepción lo indica y siempre en el resumen de errores. La anulación vuelve a la lista (Post-Redirect-Get);
 * sus rechazos los resuelve el {@code ControllerAdvices}.
 * </p>
 */
@Controller
@RequestMapping("/ajustes-insumo")
@RequiredArgsConstructor
public class AjusteInsumoController {

    private static final String VISTA_LISTA = "ingreso_insumo/ajuste-lista";
    private static final String VISTA_FORMULARIO = "ingreso_insumo/ajuste-form";
    private static final String VISTA_BUSCADOR_INSUMO = "ingreso_insumo/buscador-insumo :: buscador";
    private static final String VISTA_BUSCADOR_LOTE = "ingreso_insumo/buscador-lote-insumo :: buscador";
    private static final String VISTA_BUSCADOR_MOTIVO = "ingreso_insumo/buscador-motivo-ajuste :: buscador";
    private static final String ATRIBUTO_FORMULARIO = "ajusteForm";

    private static final String URL_LISTA = "/ajustes-insumo";
    private static final String URL_BUSCADOR_INSUMOS = URL_LISTA + "/buscador/insumos";
    private static final String URL_BUSCADOR_LOTES = URL_LISTA + "/buscador/lotes";
    private static final String URL_BUSCADOR_MOTIVOS = URL_LISTA + "/buscador/motivos";
    private static final String URL_FILTRO_INSUMOS = URL_LISTA + "/filtros/insumos";
    private static final String URL_FILTRO_LOTES = URL_LISTA + "/filtros/lotes";

    private static final String CONSULTAR = "hasAuthority('AJUSTE_INSUMO_CONSULTAR')";
    private static final String REGISTRAR = "hasAuthority('AJUSTE_INSUMO_REGISTRAR')";
    private static final String ANULAR = "hasAuthority('AJUSTE_INSUMO_ANULAR')";

    private static final String MENSAJE_REGISTRADO = "Ajuste de insumo registrado correctamente";
    private static final String MENSAJE_ANULADO = "Ajuste de insumo anulado correctamente";
    private static final String MENSAJE_ELEGIR_INSUMO = "Elija primero el insumo";

    /** Los campos del formulario a los que se asocia el rechazo de una regla del service. */
    private static final Set<String> CAMPOS_FORMULARIO = Set.of("idLoteInsumo", "idMotivoAjuste", "cantidad", "observacion");

    private final IAjusteInsumoServicio ajusteInsumoServicio;
    private final ILoteInsumoServicio loteInsumoServicio;
    private final IMotivoAjusteServicio motivoAjusteServicio;
    private final IInsumoServicio insumoServicio;
    private final IStockInsumoServicio stockInsumoServicio;

    // ==================== Listado ====================

    /**
     * Lista los ajustes de a {@code TAMANIO_PAGINA} y con el último registrado arriba. Los criterios (lote de insumo,
     * tipo, estado y rango de fecha de registro, inclusive en ambos extremos) son opcionales: sin estado o sin tipo, o
     * con un valor inválido (se avisa), se muestran todos. Un lote de insumo que ya no existe no restringe el listado. Un
     * rango de fechas invertido muestra el aviso del service y una lista vacía, para no perder los criterios elegidos.
     */
    @GetMapping({"", "/"})
    @PreAuthorize(CONSULTAR)
    public String listarAjustes(@RequestParam(required = false) Long idLoteInsumo,
                                @RequestParam(required = false) String tipo,
                                @RequestParam(required = false) String estado,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        List<String> avisos = new ArrayList<>();
        EstadoTransaccion filtroEstado = aEstadoTransaccion(estado);
        if (vacioANulo(estado) != null && filtroEstado == null) {
            avisos.add("El estado indicado no es válido, se muestran todos los ajustes");
        }
        TipoAjuste filtroTipo = aTipoAjuste(tipo);
        if (vacioANulo(tipo) != null && filtroTipo == null) {
            avisos.add("El tipo indicado no es válido, se muestran todos los ajustes");
        }

        LoteInsumoResponseDTO lote = resolverUno(idLoteInsumo, loteInsumoServicio::buscarPorId);
        Pageable pageable = PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_MAS_RECIENTE_PRIMERO);
        Page<AjusteInsumoResponseDTO> ajustes;
        try {
            ajustes = ajusteInsumoServicio.filtrarAjustesInsumos(lote == null ? null : lote.getId(), filtroTipo, filtroEstado,
                    fechaDesde != null ? fechaDesde.atStartOfDay() : null,
                    fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null, pageable);
        } catch (ReglaNegocioException e) {
            ajustes = new PageImpl<>(List.of(), pageable, 0);
            avisos.add(e.getMessage());
        }

        model.addAttribute("ajustes", ajustes);
        model.addAttribute("estados", EstadoTransaccion.values());
        model.addAttribute("tipos", TipoAjuste.values());
        model.addAttribute("urlPantalla", URL_LISTA);
        model.addAttribute("urlNuevo", URL_LISTA + "/nuevo");
        model.addAttribute("urlFiltroInsumos", URL_FILTRO_INSUMOS);
        model.addAttribute("filtroIdLoteInsumo", lote == null ? null : lote.getId());
        model.addAttribute("filtroLoteTexto", lote == null ? null : textoDeLoteConInsumo(lote));
        model.addAttribute("filtroTipo", filtroTipo);
        model.addAttribute("filtroEstado", filtroEstado);
        model.addAttribute("filtroFechaDesde", fechaDesde);
        model.addAttribute("filtroFechaHasta", fechaHasta);
        if (!avisos.isEmpty()) {
            model.addAttribute("mensaje", MensajeConLista.deAvisos(avisos));
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }
        return VISTA_LISTA;
    }

    // ==================== Alta ====================

    /**
     * Muestra el formulario vacío del ajuste. Si llega un lote de insumo ({@code idLoteInsumo}, desde el detalle del lote
     * en Stock de Insumos) el lote y su insumo llegan elegidos, y si llega un motivo ({@code idMotivoAjuste}, al volver del
     * alta de un motivo desde el selector) el motivo llega elegido. Un lote o un motivo que ya no existe deja su selector
     * vacío.
     */
    @GetMapping("/nuevo")
    @PreAuthorize(REGISTRAR)
    public String mostrarFormulario(@RequestParam(required = false) Long idLoteInsumo,
                                    @RequestParam(required = false) Long idMotivoAjuste,
                                    Model model) {
        AjusteInsumoFormDTO ajusteForm = new AjusteInsumoFormDTO();
        ajusteForm.setIdLoteInsumo(resolverUno(idLoteInsumo, loteInsumoServicio::buscarPorId) == null ? null : idLoteInsumo);
        ajusteForm.setIdMotivoAjuste(resolverUno(idMotivoAjuste, motivoAjusteServicio::buscarPorId) == null ? null : idMotivoAjuste);
        model.addAttribute(ATRIBUTO_FORMULARIO, ajusteForm);
        cargarModeloFormulario(model, ajusteForm, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Registra un ajuste (Post-Redirect-Get) y vuelve a la lista. Si la validación del formulario o una regla del service
     * falla, vuelve a mostrar el formulario (sin redirigir) con lo que el usuario había cargado.
     *
     * @param idInsumo El insumo que el usuario eligió: solo se usa para volver a mostrarlo si el registro falla.
     */
    @PostMapping({"", "/"})
    @PreAuthorize(REGISTRAR)
    public String registrarAjuste(@RequestParam(required = false) Long idInsumo,
                                  @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) AjusteInsumoFormDTO ajusteForm,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                ajusteInsumoServicio.registrarAjusteInsumo(ajusteForm);
                agregarMensaje(redirectAttributes, MENSAJE_REGISTRADO, TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoNoEncontradoException e) {
                rechazarRegla(bindingResult, e.getMessage(), e.getCampo());
            } catch (ReglaNegocioException e) {
                rechazarRegla(bindingResult, e.getMessage(), e.getCampo());
            }
        }

        Object escrito = bindingResult.getFieldValue("cantidad");
        cargarModeloFormulario(model, ajusteForm, idInsumo, escrito == null ? null : numeroEscrito(ajusteForm.getCantidad(), escrito));
        return VISTA_FORMULARIO;
    }

    // ==================== Anular ====================

    /**
     * Anula un ajuste y vuelve a la lista. El motivo es obligatorio y lo valida el service: si falta, si supera el máximo
     * o si el ajuste no se puede anular, el {@code ControllerAdvices} vuelve al origen con el motivo del rechazo.
     */
    @PostMapping("/{id}/anular")
    @PreAuthorize(ANULAR)
    public String anularAjuste(@PathVariable Long id,
                               @RequestParam(required = false) String motivo,
                               RedirectAttributes redirectAttributes) {
        ajusteInsumoServicio.anularAjusteInsumo(id, AnularAjusteInsumoFormDTO.builder().motivoAnulacion(motivo).build());
        agregarMensaje(redirectAttributes, MENSAJE_ANULADO, TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    // ==================== Buscadores del formulario ====================

    /**
     * Devuelve el contenido del modal de búsqueda de insumos del formulario (nombre y tipo + resultados), de a
     * {@code TAMANIO_PAGINA_BUSCADOR} y en orden alfabético. Se ofrecen los insumos activos de cualquier tipo.
     */
    @GetMapping("/buscador/insumos")
    @PreAuthorize(REGISTRAR)
    public String buscarInsumos(@RequestParam(required = false) String nombre,
                                @RequestParam(required = false) TipoInsumo tipo,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        cargarBuscadorDeInsumos(model, nombre, tipo, pagina, URL_BUSCADOR_INSUMOS);
        return VISTA_BUSCADOR_INSUMO;
    }

    /**
     * Devuelve el contenido del modal de lotes del insumo elegido en el formulario: todos sus lotes, también los
     * vencidos y los agotados, por fecha de vencimiento, con la fecha de vencimiento y la cantidad actual. Un modal no
     * puede redirigir: un insumo sin elegir se informa dentro del propio fragmento; un insumo que no existe lo resuelve el
     * {@code ControllerAdvices} con el motivo como texto.
     */
    @GetMapping("/buscador/lotes")
    @PreAuthorize(REGISTRAR)
    public String buscarLotes(@RequestParam(required = false) Long idInsumo,
                              @RequestParam(defaultValue = "0") int pagina,
                              Model model) {
        cargarBuscadorDeLotes(model, idInsumo, pagina, URL_BUSCADOR_LOTES, null);
        return VISTA_BUSCADOR_LOTE;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de motivos de ajuste (nombre y tipo + resultados), de a
     * {@code TAMANIO_PAGINA_BUSCADOR} y en orden alfabético. Se ofrecen los motivos activos.
     */
    @GetMapping("/buscador/motivos")
    @PreAuthorize(REGISTRAR)
    public String buscarMotivos(@RequestParam(required = false) String nombre,
                                @RequestParam(required = false) TipoAjuste tipo,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        Page<MotivoAjusteResponseDTO> motivos = motivoAjusteServicio.filtrarMotivosAjuste(vacioANulo(nombre), tipo,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));

        model.addAttribute("motivos", motivos);
        model.addAttribute("tipos", TipoAjuste.values());
        model.addAttribute("filtroNombre", vacioANulo(nombre));
        model.addAttribute("filtroTipo", tipo);
        model.addAttribute("urlBuscador", URL_BUSCADOR_MOTIVOS);
        return VISTA_BUSCADOR_MOTIVO;
    }

    // ==================== Buscadores del filtro del listado ====================

    /**
     * Devuelve el contenido del primer paso del modal del filtro por lote: la búsqueda de insumos (nombre y tipo +
     * resultados), de a {@code TAMANIO_PAGINA_BUSCADOR} y en orden alfabético. Elegir un insumo lleva, dentro del mismo
     * modal, a sus lotes. Lo ven quienes pueden consultar los ajustes.
     */
    @GetMapping("/filtros/insumos")
    @PreAuthorize(CONSULTAR)
    public String filtrarInsumos(@RequestParam(required = false) String nombre,
                                 @RequestParam(required = false) TipoInsumo tipo,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        cargarBuscadorDeInsumos(model, nombre, tipo, pagina, URL_FILTRO_INSUMOS);
        model.addAttribute("urlLotesDelInsumo", URL_FILTRO_LOTES);
        return VISTA_BUSCADOR_INSUMO;
    }

    /**
     * Devuelve el contenido del segundo paso del modal del filtro por lote: todos los lotes del insumo elegido, también
     * los vencidos y los agotados, con un enlace para volver a elegir otro insumo. Lo ven quienes pueden consultar los
     * ajustes.
     */
    @GetMapping("/filtros/lotes")
    @PreAuthorize(CONSULTAR)
    public String filtrarLotes(@RequestParam(required = false) Long idInsumo,
                               @RequestParam(defaultValue = "0") int pagina,
                               Model model) {
        cargarBuscadorDeLotes(model, idInsumo, pagina, URL_FILTRO_LOTES, URL_FILTRO_INSUMOS);
        return VISTA_BUSCADOR_LOTE;
    }

    // ==================== Modelo del formulario ====================

    /**
     * Carga lo que la vista del formulario necesita además del DTO: el lote elegido con su insumo, el motivo elegido, la
     * cantidad tal como se escribió y las URL de los buscadores. Si el lote no está elegido, el insumo es el que el usuario
     * eligió (solo de ayuda). Lo que ya no existe deja el selector vacío.
     */
    private void cargarModeloFormulario(Model model, AjusteInsumoFormDTO ajusteForm, Long idInsumo, String cantidadEscrita) {
        model.addAttribute("urlAccion", URL_LISTA);
        model.addAttribute("urlCancelar", URL_LISTA);
        model.addAttribute("urlBuscadorInsumos", URL_BUSCADOR_INSUMOS);
        model.addAttribute("urlBuscadorLotes", URL_BUSCADOR_LOTES);
        model.addAttribute("urlBuscadorMotivos", URL_BUSCADOR_MOTIVOS);
        model.addAttribute("cantidadEscrita", cantidadEscrita);

        LoteInsumoResponseDTO lote = resolverUno(ajusteForm.getIdLoteInsumo(), loteInsumoServicio::buscarPorId);
        InsumoResponseDTO insumo = lote != null ? lote.getInsumo() : resolverUno(idInsumo, insumoServicio::buscarPorId);
        model.addAttribute("loteElegido", lote);
        model.addAttribute("insumoElegido", insumo);
        model.addAttribute("motivoElegido", resolverUno(ajusteForm.getIdMotivoAjuste(), motivoAjusteServicio::buscarPorId));
    }

    // ==================== Apoyo ====================

    private void cargarBuscadorDeInsumos(Model model, String nombre, TipoInsumo tipo, int pagina, String urlBuscador) {
        Page<InsumoResponseDTO> insumos = insumoServicio.filtrarInsumos(vacioANulo(nombre), tipo,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));
        Map<Long, String> tiposPorId = new HashMap<>();
        insumos.forEach(insumo -> tiposPorId.put(insumo.getId(), etiquetaTipo(insumo)));

        model.addAttribute("insumos", insumos);
        model.addAttribute("tiposPorId", tiposPorId);
        model.addAttribute("tipos", tiposDeInsumo());
        model.addAttribute("filtroNombre", vacioANulo(nombre));
        model.addAttribute("filtroTipo", tipo);
        model.addAttribute("urlBuscador", urlBuscador);
    }

    /**
     * Carga el modal de lotes de un insumo. {@code urlInsumos} es la URL del paso anterior (el filtro de la lista, que
     * elige primero el insumo): si viene, el modal ofrece volver a ella y el texto del lote elegido incluye el insumo.
     */
    private void cargarBuscadorDeLotes(Model model, Long idInsumo, int pagina, String urlBuscador, String urlInsumos) {
        if (idInsumo == null) {
            model.addAttribute("mensajeBuscador", MENSAJE_ELEGIR_INSUMO);
            model.addAttribute("urlInsumos", urlInsumos);
            return;
        }
        InsumoResponseDTO insumo = insumoServicio.buscarPorId(idInsumo);
        Page<StockLoteInsumoResponseDTO> lotes = stockInsumoServicio.filtrarLotesDeInsumo(idInsumo,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR));

        model.addAttribute("lotes", lotes);
        model.addAttribute("idInsumo", idInsumo);
        model.addAttribute("nombreInsumo", insumo.getNombre());
        model.addAttribute("unidad", insumo.getUnidadDeMedida().getSimbolo());
        model.addAttribute("urlBuscador", urlBuscador);
        model.addAttribute("urlInsumos", urlInsumos);
    }

    /** Texto con el que el filtro muestra el lote elegido: el insumo y la identificación del lote del proveedor. */
    private static String textoDeLoteConInsumo(LoteInsumoResponseDTO lote) {
        return lote.getInsumo().getNombre() + " - " + lote.getIdentificacionLoteProveedor();
    }

    /** El tipo con ese nombre, o {@code null} si el texto está vacío o no corresponde a ninguno (se muestran todos). */
    private static TipoAjuste aTipoAjuste(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return TipoAjuste.valueOf(limpio.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Asocia el rechazo del service al campo que indica la excepción, si el formulario lo tiene, y siempre lo deja en el
     * resumen de errores. Un rechazo que no indica un campo del formulario queda como error general.
     */
    private static void rechazarRegla(BindingResult bindingResult, String mensaje, String campo) {
        if (campo != null && CAMPOS_FORMULARIO.contains(campo)) {
            bindingResult.rejectValue(campo, "reglaNegocio", mensaje);
        } else {
            bindingResult.reject("reglaNegocio", mensaje);
        }
    }
}
