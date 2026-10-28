package com.github.heikyudev.maestrocervecero.presentation.controller.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularIngresoInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoDirectoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.IngresoInsumoPorCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.IIngresoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.ILoteInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.orden_compra.IOrdenCompraServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.IngresoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.DetalleCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesTexto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de los ingresos de insumo ("Ingresos de Insumo"). Todo el módulo es del ENCARGADO_DE_DEPOSITO y cada
 * método se autoriza por permiso ({@code hasAuthority}).
 * <p>
 * La pantalla principal lista los ingresos en dos pestañas, "Por compra" y "Directos", con el último ingreso arriba. Un
 * ingreso por compra recibe un ítem de una orden de compra pendiente: se elige la orden con su buscador (solo
 * pendientes) y luego el ítem de esa orden que todavía tiene cantidad pendiente. Un ingreso directo no tiene orden: se
 * elige el insumo. En los dos casos el lote se identifica con el código del proveedor y su vencimiento; el botón "Ver
 * lotes existentes" ofrece los lotes ya registrados del insumo para reutilizar su código y su vencimiento. El lote no se
 * crea aparte: lo crea el primer ingreso que lo usa.
 * </p>
 * <p>
 * La orden de compra elegida en el formulario por compra es un parámetro de la pantalla y no un campo del formulario: el
 * ítem ya determina la orden. Las reglas del ingreso son del service: un rechazo se muestra en el propio formulario, en
 * el campo que corresponde cuando el mensaje lo identifica y siempre en el resumen de errores. La anulación vuelve a la
 * pestaña del ingreso anulado (Post-Redirect-Get); sus rechazos los resuelve el {@code ControllerAdvices}.
 * </p>
 */
@Controller
@RequestMapping("/ingresos-insumo")
@RequiredArgsConstructor
public class IngresoInsumoController {

    private static final String VISTA_LISTA = "ingreso_insumo/ingreso-lista";
    private static final String VISTA_FORMULARIO_COMPRA = "ingreso_insumo/ingreso-compra-form";
    private static final String VISTA_FORMULARIO_DIRECTO = "ingreso_insumo/ingreso-directo-form";
    private static final String VISTA_BUSCADOR_ORDEN = "ingreso_insumo/buscador-orden-compra :: buscador";
    private static final String VISTA_BUSCADOR_ITEM = "ingreso_insumo/buscador-item-orden :: buscador";
    private static final String VISTA_BUSCADOR_INSUMO = "ingreso_insumo/buscador-insumo :: buscador";
    private static final String VISTA_BUSCADOR_LOTE = "ingreso_insumo/buscador-lote-existente :: buscador";
    private static final String ATRIBUTO_FORMULARIO = "ingresoForm";

    private static final String URL_LISTA = "/ingresos-insumo";
    private static final String URL_FORMULARIO_COMPRA = URL_LISTA + "/compra/nuevo";
    private static final String URL_BUSCADOR_ORDENES = URL_LISTA + "/compra/buscador/ordenes-compra";
    private static final String URL_BUSCADOR_ITEMS = URL_LISTA + "/compra/buscador/items";
    private static final String URL_BUSCADOR_INSUMOS = URL_LISTA + "/directo/buscador/insumos";
    private static final String URL_FILTRO_INSUMOS = URL_LISTA + "/filtros/insumos";
    private static final String URL_BUSCADOR_LOTES = URL_LISTA + "/buscador/lotes-insumo";
    /** Buscadores de las órdenes de compra que el encargado de depósito también puede usar (los ofrece el controller de órdenes). */
    private static final String URL_BUSCADOR_PROVEEDORES = "/ordenes-compra/buscador/proveedores";
    private static final String URL_FILTRO_PLANIFICACIONES = "/ordenes-compra/filtros/planificaciones";

    private static final String CONSULTAR = "hasAuthority('INGRESO_INSUMO_CONSULTAR')";
    private static final String REGISTRAR = "hasAuthority('INGRESO_INSUMO_REGISTRAR')";
    private static final String ANULAR = "hasAuthority('INGRESO_INSUMO_ANULAR')";

    private static final String PESTANA_COMPRA = "compra";
    private static final String PESTANA_DIRECTO = "directo";
    private static final String MENSAJE_REGISTRADO = "Ingreso de insumo registrado correctamente";
    private static final String MENSAJE_ITEM_FUERA_DE_LA_ORDEN = "El ítem elegido no pertenece a la orden de compra seleccionada";
    private static final String MENSAJE_ELEGIR_ORDEN = "Elija primero la orden de compra";
    private static final String MENSAJE_ELEGIR_INSUMO = "Elija primero el insumo";

    /** El último ingreso arriba: por fecha de ingreso y, ante igual fecha, el registrado más tarde. */
    private static final Sort ORDEN_INGRESOS = Sort.by(Sort.Order.desc("fechaIngreso"), Sort.Order.desc("id"));

    /** Los campos del formulario por compra a los que se asocia el rechazo de una regla del service. */
    private static final Set<String> CAMPOS_COMPRA = Set.of("identificacionLoteProveedor", "fechaIngreso", "cantidadRecibida",
            "fechaVencimiento", "idDetalleCompra");
    /** Los campos del formulario directo a los que se asocia el rechazo de una regla del service. */
    private static final Set<String> CAMPOS_DIRECTO = Set.of("identificacionLoteProveedor", "fechaIngreso", "cantidadRecibida",
            "fechaVencimiento", "costoUnitario", "idInsumo");

    private final IIngresoInsumoServicio ingresoInsumoServicio;
    private final IOrdenCompraServicio ordenCompraServicio;
    private final IInsumoServicio insumoServicio;
    private final ILoteInsumoServicio loteInsumoServicio;
    private final IProveedorServicio proveedorServicio;
    private final IPlanificacionProduccionServicio planificacionServicio;

    /**
     * Una fila del buscador de ítems de la orden: el ítem con sus cantidades en la unidad del insumo y los datos que el
     * formulario muestra del ítem elegido.
     */
    @Value
    public static class FilaDeItem {
        DetalleCompraResponseDTO detalle;
        /** Cantidad total pedida, en la unidad del insumo. */
        Double pedido;
        /** Cantidad de insumo que trae una presentación comercial, en la unidad del insumo, como número sin notación científica. */
        String factor;
        /** La presentación comercial con su contenido, por ejemplo "Bolsa (25 kg)". */
        String presentacion;
        String unidad;
        String texto;
    }

    // ==================== Listado ====================

    /**
     * Lista los ingresos de la pestaña pedida ("compra", la predeterminada, o "directo") de a {@code TAMANIO_PAGINA} y
     * con el último ingreso arriba. Los criterios (insumo, identificación del lote del proveedor, estado y fecha de
     * ingreso) son opcionales: sin estado, o con un valor inválido (se avisa), se muestran todos. Un insumo que ya no
     * existe no restringe el listado.
     */
    @GetMapping({"", "/"})
    @PreAuthorize(CONSULTAR)
    public String listarIngresos(@RequestParam(required = false) String tipo,
                                 @RequestParam(required = false) Long idInsumo,
                                 @RequestParam(required = false) String identificacion,
                                 @RequestParam(required = false) String estado,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaIngreso,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        EstadoTransaccion filtroEstado = aEstadoTransaccion(estado);
        if (vacioANulo(estado) != null && filtroEstado == null) {
            model.addAttribute("mensaje", "El estado indicado no es válido, se muestran todos los ingresos");
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }

        InsumoResponseDTO insumo = resolverUno(idInsumo, insumoServicio::buscarPorId);
        String pestana = pestanaDe(tipo);
        TipoIngreso tipoIngreso = PESTANA_DIRECTO.equals(pestana) ? TipoIngreso.DIRECTO : TipoIngreso.COMPRA;

        Page<IngresoInsumoResponseDTO> ingresos = ingresoInsumoServicio.filtrarIngresoInsumo(
                insumo == null ? null : insumo.getId(), vacioANulo(identificacion), filtroEstado, tipoIngreso, fechaIngreso,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_INGRESOS));

        model.addAttribute("ingresos", ingresos);
        model.addAttribute("estados", EstadoTransaccion.values());
        model.addAttribute("pestana", pestana);
        model.addAttribute("urlPantalla", URL_LISTA);
        model.addAttribute("urlNuevo", PESTANA_DIRECTO.equals(pestana) ? URL_LISTA + "/directo/nuevo" : URL_FORMULARIO_COMPRA);
        model.addAttribute("urlFiltroInsumos", URL_FILTRO_INSUMOS);
        model.addAttribute("filtroIdInsumo", insumo == null ? null : insumo.getId());
        model.addAttribute("filtroInsumoTexto", insumo == null ? null : insumo.getNombre());
        model.addAttribute("filtroIdentificacion", vacioANulo(identificacion));
        model.addAttribute("filtroEstado", filtroEstado);
        model.addAttribute("filtroFechaIngreso", fechaIngreso);
        return VISTA_LISTA;
    }

    // ==================== Alta por compra ====================

    /**
     * Muestra el formulario vacío del ingreso por compra, con la fecha de ingreso de hoy. Con {@code idOrdenCompra}
     * llega con la orden ya elegida (por ejemplo, para cargar otro ítem de la misma orden); una orden que ya no existe o
     * que ya no está pendiente no se elige y se avisa.
     */
    @GetMapping("/compra/nuevo")
    @PreAuthorize(REGISTRAR)
    public String mostrarFormularioCompra(@RequestParam(required = false) Long idOrdenCompra, Model model) {
        IngresoInsumoPorCompraFormDTO ingresoForm = IngresoInsumoPorCompraFormDTO.builder().fechaIngreso(LocalDate.now()).build();
        OrdenCompraResponseDTO orden = resolverUno(idOrdenCompra, ordenCompraServicio::buscarPorId);
        if (idOrdenCompra != null && (orden == null || orden.getEstado() != EstadoSolicitud.PENDIENTE)) {
            orden = null;
            model.addAttribute("mensaje", "La orden de compra indicada no está pendiente, elija otra");
            model.addAttribute("tipo", TipoAlerta.WARNING.getCodigo());
        }
        model.addAttribute(ATRIBUTO_FORMULARIO, ingresoForm);
        cargarModeloCompra(model, ingresoForm, orden);
        return VISTA_FORMULARIO_COMPRA;
    }

    /**
     * Registra el ingreso de un ítem de una orden de compra (Post-Redirect-Get). Si la validación del formulario o una
     * regla del service falla, vuelve a mostrar el formulario (sin redirigir) con lo que el usuario había cargado. Al
     * registrar vuelve a la pestaña "Por compra" o, con {@code cargarOtro}, al formulario con la misma orden elegida
     * mientras siga pendiente (si el ingreso la completó, vuelve a la pestaña con un aviso).
     *
     * @param idOrdenCompra La orden que el usuario eligió: solo se usa para volver a mostrarla si el registro falla.
     * @param cargarOtro Presente cuando se pidió registrar y cargar otro ítem de la misma orden.
     */
    @PostMapping("/compra")
    @PreAuthorize(REGISTRAR)
    public String registrarIngresoPorCompra(@RequestParam(required = false) Long idOrdenCompra,
                                            @RequestParam(required = false) String cargarOtro,
                                            @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) IngresoInsumoPorCompraFormDTO ingresoForm,
                                            BindingResult bindingResult,
                                            Model model,
                                            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors() && !itemPerteneceALaOrden(ingresoForm.getIdDetalleCompra(), idOrdenCompra)) {
            bindingResult.rejectValue("idDetalleCompra", "itemFueraDeLaOrden", MENSAJE_ITEM_FUERA_DE_LA_ORDEN);
        }
        if (!bindingResult.hasErrors()) {
            try {
                IngresoInsumoResponseDTO ingreso = ingresoInsumoServicio.registrarIngresoInsumoPorCompra(ingresoForm);
                Long idOrden = ingreso != null && ingreso.getIdOrdenCompra() != null ? ingreso.getIdOrdenCompra() : idOrdenCompra;
                return volverTrasRegistrarPorCompra(idOrden, cargarOtro != null, redirectAttributes);
            } catch (RecursoDuplicadoException e) {
                rechazarDuplicado(bindingResult, e, CAMPOS_COMPRA);
            } catch (RecursoNoEncontradoException e) {
                rechazarRegla(bindingResult, e.getMessage(), e.getCampo(), CAMPOS_COMPRA);
            } catch (ReglaNegocioException e) {
                rechazarRegla(bindingResult, e.getMessage(), e.getCampo(), CAMPOS_COMPRA);
            }
        }

        cargarModeloCompra(model, ingresoForm, resolverUno(idOrdenCompra, ordenCompraServicio::buscarPorId));
        return VISTA_FORMULARIO_COMPRA;
    }

    // ==================== Alta directa ====================

    /** Muestra el formulario vacío del ingreso directo, con la fecha de ingreso de hoy. */
    @GetMapping("/directo/nuevo")
    @PreAuthorize(REGISTRAR)
    public String mostrarFormularioDirecto(Model model) {
        IngresoInsumoDirectoFormDTO ingresoForm = IngresoInsumoDirectoFormDTO.builder().fechaIngreso(LocalDate.now()).build();
        model.addAttribute(ATRIBUTO_FORMULARIO, ingresoForm);
        cargarModeloDirecto(model, ingresoForm);
        return VISTA_FORMULARIO_DIRECTO;
    }

    /**
     * Registra un ingreso directo (Post-Redirect-Get) y vuelve a la pestaña "Directos". Si la validación del formulario
     * o una regla del service falla, vuelve a mostrar el formulario (sin redirigir) con lo que el usuario había cargado.
     */
    @PostMapping("/directo")
    @PreAuthorize(REGISTRAR)
    public String registrarIngresoDirecto(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) IngresoInsumoDirectoFormDTO ingresoForm,
                                          BindingResult bindingResult,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                ingresoInsumoServicio.registrarIngresoInsumoDirecto(ingresoForm);
                agregarMensaje(redirectAttributes, MENSAJE_REGISTRADO, TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA + "?tipo=" + PESTANA_DIRECTO;
            } catch (RecursoDuplicadoException e) {
                rechazarDuplicado(bindingResult, e, CAMPOS_DIRECTO);
            } catch (RecursoNoEncontradoException e) {
                rechazarRegla(bindingResult, e.getMessage(), e.getCampo(), CAMPOS_DIRECTO);
            } catch (ReglaNegocioException e) {
                rechazarRegla(bindingResult, e.getMessage(), e.getCampo(), CAMPOS_DIRECTO);
            }
        }

        cargarModeloDirecto(model, ingresoForm);
        return VISTA_FORMULARIO_DIRECTO;
    }

    // ==================== Anular ====================

    /**
     * Anula un ingreso y vuelve a la pestaña de su tipo. El motivo es obligatorio y lo valida el service: si falta, si
     * supera el máximo o si el ingreso no se puede anular, el {@code ControllerAdvices} vuelve al origen con el motivo del
     * rechazo.
     */
    @PostMapping("/{id}/anular")
    @PreAuthorize(ANULAR)
    public String anularIngreso(@PathVariable Long id,
                                @RequestParam(required = false) String motivo,
                                RedirectAttributes redirectAttributes) {
        IngresoInsumoResponseDTO anulado = ingresoInsumoServicio.anularIngresoInsumo(id,
                AnularIngresoInsumoFormDTO.builder().motivoAnulacion(motivo).build());
        agregarMensaje(redirectAttributes, "Ingreso de insumo anulado correctamente", TipoAlerta.SUCCESS);
        boolean directo = anulado != null && anulado.getTipoIngreso() == TipoIngreso.DIRECTO;
        return "redirect:" + URL_LISTA + "?tipo=" + (directo ? PESTANA_DIRECTO : PESTANA_COMPRA);
    }

    // ==================== Buscadores de los formularios ====================

    /**
     * Devuelve el contenido del modal de búsqueda de órdenes de compra del ingreso por compra (criterios de proveedor,
     * planificación y fecha de entrega estimada + resultados), de a {@code TAMANIO_PAGINA_BUSCADOR} y con la última
     * registrada arriba. Solo se ofrecen las pendientes: es el único estado sobre el que se registran ingresos. Un
     * proveedor o una planificación que ya no existe no restringe la búsqueda.
     */
    @GetMapping("/compra/buscador/ordenes-compra")
    @PreAuthorize(REGISTRAR)
    public String buscarOrdenesCompra(@RequestParam(required = false) Long idProveedor,
                                      @RequestParam(required = false) Long idPlanificacion,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntregaEstimada,
                                      @RequestParam(defaultValue = "0") int pagina,
                                      Model model) {
        ProveedorResponseDTO proveedor = resolverUno(idProveedor, proveedorServicio::buscarPorId);
        PlanificacionProduccionResponseDTO planificacion = resolverUno(idPlanificacion, planificacionServicio::buscarPorId);

        Page<OrdenCompraResponseDTO> ordenes = ordenCompraServicio.filtrarOrdenesCompra(
                planificacion == null ? null : planificacion.getId(), proveedor == null ? null : proveedor.getId(),
                EstadoSolicitud.PENDIENTE, fechaEntregaEstimada,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR, ORDEN_MAS_RECIENTE_PRIMERO));

        Map<Long, String> textosPorId = new HashMap<>();
        ordenes.forEach(orden -> textosPorId.put(orden.getId(), textoDeOrden(orden)));

        model.addAttribute("ordenes", ordenes);
        model.addAttribute("textosPorId", textosPorId);
        model.addAttribute("urlBuscador", URL_BUSCADOR_ORDENES);
        model.addAttribute("urlBuscadorProveedores", URL_BUSCADOR_PROVEEDORES);
        model.addAttribute("urlBuscadorPlanificaciones", URL_FILTRO_PLANIFICACIONES);
        model.addAttribute("filtroIdProveedor", proveedor == null ? null : proveedor.getId());
        model.addAttribute("filtroProveedorTexto", proveedor == null ? null : textoDeProveedor(proveedor.getVersion()));
        model.addAttribute("filtroIdPlanificacion", planificacion == null ? null : planificacion.getId());
        model.addAttribute("filtroPlanificacionTexto", planificacion == null ? null : planificacion.getEtiqueta());
        model.addAttribute("filtroFechaEntregaEstimada", fechaEntregaEstimada);
        return VISTA_BUSCADOR_ORDEN;
    }

    /**
     * Devuelve el contenido del modal de ítems de la orden elegida: los ítems que todavía tienen cantidad pendiente, en
     * orden alfabético por insumo, con lo pedido, lo recibido y lo pendiente en la unidad del insumo. Un modal no puede
     * redirigir: una orden sin elegir o que ya no está pendiente se informan dentro del propio fragmento; una orden que no
     * existe la resuelve el {@code ControllerAdvices} con el motivo como texto.
     */
    @GetMapping("/compra/buscador/items")
    @PreAuthorize(REGISTRAR)
    public String buscarItemsDeLaOrden(@RequestParam(required = false) Long idOrdenCompra, Model model) {
        if (idOrdenCompra == null) {
            model.addAttribute("mensajeBuscador", MENSAJE_ELEGIR_ORDEN);
            return VISTA_BUSCADOR_ITEM;
        }
        OrdenCompraResponseDTO orden = ordenCompraServicio.buscarPorId(idOrdenCompra);
        if (orden.getEstado() != EstadoSolicitud.PENDIENTE) {
            model.addAttribute("mensajeBuscador", "La orden de compra ya no está pendiente");
            return VISTA_BUSCADOR_ITEM;
        }
        model.addAttribute("items", itemsPendientes(orden));
        model.addAttribute("textoOrden", textoDeOrden(orden));
        return VISTA_BUSCADOR_ITEM;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de insumos del ingreso directo (nombre y tipo + resultados), de a
     * {@code TAMANIO_PAGINA_BUSCADOR} y en orden alfabético. Se ofrecen los insumos activos de cualquier tipo.
     */
    @GetMapping("/directo/buscador/insumos")
    @PreAuthorize(REGISTRAR)
    public String buscarInsumos(@RequestParam(required = false) String nombre,
                                @RequestParam(required = false) TipoInsumo tipo,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        cargarBuscadorDeInsumos(model, nombre, tipo, pagina, URL_BUSCADOR_INSUMOS);
        return VISTA_BUSCADOR_INSUMO;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de lotes ya registrados de un insumo (identificación + resultados), de
     * a {@code TAMANIO_PAGINA_BUSCADOR}, por vencimiento ascendente y con los lotes agotados incluidos. Sin insumo
     * elegido, el motivo se informa dentro del propio fragmento.
     */
    @GetMapping("/buscador/lotes-insumo")
    @PreAuthorize(REGISTRAR)
    public String buscarLotesExistentes(@RequestParam(required = false) Long idInsumo,
                                        @RequestParam(required = false) String identificacion,
                                        @RequestParam(defaultValue = "0") int pagina,
                                        Model model) {
        if (idInsumo == null) {
            model.addAttribute("mensajeBuscador", MENSAJE_ELEGIR_INSUMO);
            return VISTA_BUSCADOR_LOTE;
        }
        model.addAttribute("lotes", loteInsumoServicio.filtrarLotesInsumoPorInsumo(idInsumo, vacioANulo(identificacion), paginaDeBuscador(pagina)));
        model.addAttribute("urlBuscador", URL_BUSCADOR_LOTES);
        model.addAttribute("idInsumo", idInsumo);
        model.addAttribute("filtroIdentificacion", vacioANulo(identificacion));
        return VISTA_BUSCADOR_LOTE;
    }

    // ==================== Buscador del filtro del listado ====================

    /**
     * Devuelve el contenido del modal de búsqueda de insumos del filtro del listado (nombre y tipo + resultados), de a
     * {@code TAMANIO_PAGINA_BUSCADOR} y en orden alfabético. Lo ven quienes pueden consultar los ingresos.
     */
    @GetMapping("/filtros/insumos")
    @PreAuthorize(CONSULTAR)
    public String filtrarInsumos(@RequestParam(required = false) String nombre,
                                 @RequestParam(required = false) TipoInsumo tipo,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        cargarBuscadorDeInsumos(model, nombre, tipo, pagina, URL_FILTRO_INSUMOS);
        return VISTA_BUSCADOR_INSUMO;
    }

    // ==================== Modelo de los formularios ====================

    /**
     * Carga lo que la vista del formulario por compra necesita además del DTO: la orden elegida, el ítem elegido de esa
     * orden (con su insumo, su unidad y lo que le queda pendiente) y las URL de los buscadores. Lo que ya no existe deja el
     * selector vacío.
     */
    private void cargarModeloCompra(Model model, IngresoInsumoPorCompraFormDTO ingresoForm, OrdenCompraResponseDTO orden) {
        model.addAttribute("urlAccion", URL_LISTA + "/compra");
        model.addAttribute("urlCancelar", URL_LISTA + "?tipo=" + PESTANA_COMPRA);
        model.addAttribute("urlBuscadorOrdenes", URL_BUSCADOR_ORDENES);
        model.addAttribute("urlBuscadorItems", URL_BUSCADOR_ITEMS);
        model.addAttribute("urlBuscadorLotes", URL_BUSCADOR_LOTES);
        model.addAttribute("idOrdenElegida", orden == null ? null : orden.getId());
        model.addAttribute("ordenElegidaTexto", orden == null ? null : textoDeOrden(orden));

        FilaDeItem item = orden == null || ingresoForm.getIdDetalleCompra() == null ? null
                : itemsPendientes(orden).stream()
                .filter(fila -> ingresoForm.getIdDetalleCompra().equals(fila.getDetalle().getId()))
                .findFirst().orElse(null);
        model.addAttribute("itemElegido", item);
        model.addAttribute("idInsumoElegido", item == null ? null : item.getDetalle().getCatalogoProveedor().getInsumo().getId());
    }

    /** Carga lo que la vista del formulario directo necesita además del DTO: el insumo elegido y las URL de los buscadores. */
    private void cargarModeloDirecto(Model model, IngresoInsumoDirectoFormDTO ingresoForm) {
        model.addAttribute("urlAccion", URL_LISTA + "/directo");
        model.addAttribute("urlCancelar", URL_LISTA + "?tipo=" + PESTANA_DIRECTO);
        model.addAttribute("urlBuscadorInsumos", URL_BUSCADOR_INSUMOS);
        model.addAttribute("urlBuscadorLotes", URL_BUSCADOR_LOTES);
        InsumoResponseDTO insumo = resolverUno(ingresoForm.getIdInsumo(), insumoServicio::buscarPorId);
        model.addAttribute("insumoElegido", insumo);
        model.addAttribute("tipoInsumoElegido", insumo == null ? null : etiquetaTipo(insumo));
    }

    /**
     * Después de registrar por compra: con {@code cargarOtro} vuelve al formulario con la orden elegida si sigue
     * pendiente; si el ingreso la completó (o ya no se puede consultar) vuelve a la pestaña con un aviso.
     */
    private String volverTrasRegistrarPorCompra(Long idOrdenCompra, boolean cargarOtro, RedirectAttributes redirectAttributes) {
        if (cargarOtro && idOrdenCompra != null) {
            OrdenCompraResponseDTO orden = resolverUno(idOrdenCompra, ordenCompraServicio::buscarPorId);
            if (orden != null && orden.getEstado() == EstadoSolicitud.PENDIENTE) {
                agregarMensaje(redirectAttributes, MENSAJE_REGISTRADO, TipoAlerta.SUCCESS);
                return "redirect:" + URL_FORMULARIO_COMPRA + "?idOrdenCompra=" + idOrdenCompra;
            }
            agregarMensaje(redirectAttributes, MENSAJE_REGISTRADO + ". La orden de compra N.º " + idOrdenCompra
                    + " quedó completa y ya no tiene ítems pendientes", TipoAlerta.INFO);
            return "redirect:" + URL_LISTA + "?tipo=" + PESTANA_COMPRA;
        }
        agregarMensaje(redirectAttributes, MENSAJE_REGISTRADO, TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA + "?tipo=" + PESTANA_COMPRA;
    }

    // ==================== Apoyo ====================

    /**
     * Si el ítem es de la orden elegida. Sin orden o sin ítem no hay nada que comparar, y una orden que ya no existe la
     * resuelve el service con el ítem.
     */
    private boolean itemPerteneceALaOrden(Long idDetalleCompra, Long idOrdenCompra) {
        if (idDetalleCompra == null || idOrdenCompra == null) {
            return true;
        }
        OrdenCompraResponseDTO orden = resolverUno(idOrdenCompra, ordenCompraServicio::buscarPorId);
        if (orden == null) {
            return true;
        }
        return orden.getDetallesCompra() != null
                && orden.getDetallesCompra().stream().anyMatch(detalle -> idDetalleCompra.equals(detalle.getId()));
    }

    private static Pageable paginaDeBuscador(int pagina) {
        return PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR);
    }

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
     * Los ítems de la orden con cantidad pendiente mayor a cero, en orden alfabético por insumo y presentación, con sus
     * cantidades en la unidad del insumo.
     */
    private static List<FilaDeItem> itemsPendientes(OrdenCompraResponseDTO orden) {
        if (orden.getDetallesCompra() == null) {
            return List.of();
        }
        return orden.getDetallesCompra().stream()
                .filter(detalle -> detalle.getCantidadPendiente() != null && detalle.getCantidadPendiente() > 0)
                .filter(detalle -> detalle.getId() != null && detalle.getCantidad() != null && detalle.getCatalogoProveedor() != null
                        && detalle.getCatalogoProveedor().getInsumo() != null && detalle.getCatalogoProveedor().getPresentacionComercial() != null)
                .map(IngresoInsumoController::aFilaDeItem)
                .sorted(Comparator.comparing((FilaDeItem fila) -> fila.getDetalle().getCatalogoProveedor().getInsumo().getNombre(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(fila -> fila.getDetalle().getCatalogoProveedor().getPresentacionComercial().getNombre(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(fila -> fila.getDetalle().getId()))
                .toList();
    }

    private static FilaDeItem aFilaDeItem(DetalleCompraResponseDTO detalle) {
        InsumoResponseDTO insumo = detalle.getCatalogoProveedor().getInsumo();
        var presentacion = detalle.getCatalogoProveedor().getPresentacionComercial();
        UnidadDeMedida unidad = insumo.getUnidadDeMedida();
        double factor = unidad.desdeGramos(presentacion.getUnidadDeMedida().aGramos(presentacion.getCantidad()));
        String contenido = textoDeNumero(presentacion.getCantidad()) + " " + presentacion.getUnidadDeMedida().getSimbolo();
        return new FilaDeItem(detalle, Redondeo.redondear(factor * detalle.getCantidad()),
                BigDecimal.valueOf(factor).setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(),
                presentacion.getNombre() + " (" + contenido + ")", unidad.getSimbolo(),
                insumo.getNombre() + " - " + presentacion.getNombre());
    }

    /** El número sin ceros sobrantes ("25" y no "25.0"). */
    private static String textoDeNumero(Double valor) {
        return BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString();
    }

    /** Texto con el que un selector muestra la orden elegida: su número y la razón social del proveedor. */
    private static String textoDeOrden(OrdenCompraResponseDTO orden) {
        return "N.º " + orden.getId() + (orden.getProveedor() == null ? "" : " - " + orden.getProveedor().getRazonSocial());
    }

    /** Texto con el que un selector muestra el proveedor: su razón social y, entre paréntesis, su CUIT con guiones. */
    private static String textoDeProveedor(VersionProveedorResponseDTO version) {
        return version.getRazonSocial() + " (" + ValidacionesTexto.formatearCuit(version.getCuit()) + ")";
    }

    /** El tipo del insumo para mostrar: Malta, Lúpulo o Levadura. */
    private static String etiquetaTipo(InsumoResponseDTO insumo) {
        if (insumo instanceof MaltaResponseDTO) {
            return "Malta";
        }
        return insumo instanceof LupuloResponseDTO ? "Lúpulo" : "Levadura";
    }

    private static Map<TipoInsumo, String> tiposDeInsumo() {
        Map<TipoInsumo, String> tipos = new LinkedHashMap<>();
        tipos.put(TipoInsumo.MALTA, "Malta");
        tipos.put(TipoInsumo.LUPULO, "Lúpulo");
        tipos.put(TipoInsumo.LEVADURA, "Levadura");
        return tipos;
    }

    /** La pestaña pedida: "directo" es la de los ingresos directos; cualquier otro valor, o ninguno, la de "Por compra". */
    private static String pestanaDe(String texto) {
        return PESTANA_DIRECTO.equalsIgnoreCase(vacioANulo(texto)) ? PESTANA_DIRECTO : PESTANA_COMPRA;
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
     * Asocia el rechazo del service al campo que indica la excepción, si el formulario lo tiene, y siempre lo deja en el
     * resumen de errores. Un rechazo que no indica un campo del formulario queda como error general.
     */
    private static void rechazarRegla(BindingResult bindingResult, String mensaje, String campo, Set<String> camposDelFormulario) {
        if (campo != null && camposDelFormulario.contains(campo)) {
            bindingResult.rejectValue(campo, "reglaNegocio", mensaje);
        } else {
            bindingResult.reject("reglaNegocio", mensaje);
        }
    }

    /** Marca el duplicado en el campo que indica el service; si no indica uno del formulario, es un error general. */
    private static void rechazarDuplicado(BindingResult bindingResult, RecursoDuplicadoException e, Set<String> camposDelFormulario) {
        if (e.getCampo() != null && camposDelFormulario.contains(e.getCampo())) {
            bindingResult.rejectValue(e.getCampo(), "duplicado", e.getMessage());
        } else {
            bindingResult.reject("duplicado", e.getMessage());
        }
    }
}
