package com.github.heikyudev.maestrocervecero.presentation.controller.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.AnulacionOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.DetalleCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.FinalizacionForzadaOrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.OrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.orden_compra.IOrdenCompraServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.VersionRecetaDePlanificacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.CatalogoProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesTexto;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de las órdenes de compra ("Órdenes de Compra").
 * <p>
 * Una orden de compra es un registro transaccional: se registra, se consulta y solo admite dos cierres manuales
 * (finalización forzada o anulación, ambos con un motivo); la finalización automática la resuelve el ingreso de
 * insumos. Ver el listado y el detalle es del GERENTE_DE_COMPRAS y del ENCARGADO_DE_DEPOSITO; registrar, anular y
 * finalizar son solo del GERENTE_DE_COMPRAS.
 * </p>
 * <p>
 * El formulario de alta es una única página: se elige la planificación de producción (solo pendientes) y el
 * proveedor con sus buscadores y, recién entonces, se agregan los ítems desde una ventana emergente que ofrece
 * únicamente los ítems del catálogo del proveedor cuyo insumo usa la receta de la planificación. Mientras la tabla
 * tiene ítems, la planificación y el proveedor no se pueden cambiar. Las filas agregadas viajan como campos indexados
 * del {@link OrdenCompraFormDTO}.
 * </p>
 */
@Controller
@RequestMapping("/ordenes-compra")
@RequiredArgsConstructor
public class OrdenCompraController {

    private static final String VISTA_LISTA = "orden_compra/orden-lista";
    private static final String VISTA_FORMULARIO = "orden_compra/orden-form";
    private static final String VISTA_DETALLE = "orden_compra/orden-detalle";
    /** Mismo modal que usa el módulo de lotes: el controller indica la URL del buscador, el subtítulo y el buscador de recetas. */
    private static final String VISTA_BUSCADOR_PLANIFICACION = "lote/buscador-planificacion :: buscador";
    private static final String VISTA_BUSCADOR_PROVEEDOR = "orden_compra/buscador-proveedor :: buscador";
    private static final String VISTA_BUSCADOR_ITEM = "orden_compra/buscador-item :: buscador";
    private static final String ATRIBUTO_FORMULARIO = "ordenCompraForm";
    private static final String URL_LISTA = "/ordenes-compra";
    private static final String URL_BUSCADOR_PLANIFICACIONES = "/ordenes-compra/buscador/planificaciones";
    private static final String URL_FILTRO_PLANIFICACIONES = "/ordenes-compra/filtros/planificaciones";
    private static final String URL_BUSCADOR_RECETAS = "/ordenes-compra/buscador/recetas";
    private static final String URL_FILTRO_RECETAS = "/ordenes-compra/filtros/recetas";

    private static final String SOLO_GERENTE = "hasRole('GERENTE_DE_COMPRAS')";
    private static final String GERENTE_O_ENCARGADO = "hasAnyRole('GERENTE_DE_COMPRAS','ENCARGADO_DE_DEPOSITO')";

    private final IOrdenCompraServicio ordenCompraServicio;
    private final IPlanificacionProduccionServicio planificacionServicio;
    private final IProveedorServicio proveedorServicio;
    private final IRecetaServicio recetaServicio;

    // ==================== Listado ====================

    /**
     * Lista las órdenes de compra, paginadas de a {@code TAMANIO_PAGINA} y con la última registrada arriba. Los criterios (planificación,
     * proveedor, estado y fecha de entrega estimada) son opcionales. Una planificación o un proveedor que ya no existe
     * no restringe el listado.
     */
    @GetMapping
    @PreAuthorize(GERENTE_O_ENCARGADO)
    public String listarOrdenesCompra(@RequestParam(required = false) Long idPlanificacion,
                                      @RequestParam(required = false) Long idProveedor,
                                      @RequestParam(required = false) EstadoSolicitud estado,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaEntregaEstimada,
                                      @RequestParam(defaultValue = "0") int pagina,
                                      Model model) {
        String planificacionTexto = etiquetaPlanificacion(idPlanificacion);
        Long idPlanificacionVigente = planificacionTexto == null ? null : idPlanificacion;
        ProveedorResponseDTO proveedor = proveedorElegido(idProveedor);
        Long idProveedorVigente = proveedor == null ? null : idProveedor;

        Page<OrdenCompraResponseDTO> ordenes = ordenCompraServicio.filtrarOrdenesCompra(
                idPlanificacionVigente, idProveedorVigente, estado, fechaEntregaEstimada,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_MAS_RECIENTE_PRIMERO));

        model.addAttribute("ordenes", ordenes);
        model.addAttribute("estados", EstadoSolicitud.values());
        model.addAttribute("filtroIdPlanificacion", idPlanificacionVigente);
        model.addAttribute("filtroPlanificacionTexto", planificacionTexto);
        model.addAttribute("filtroIdProveedor", idProveedorVigente);
        model.addAttribute("filtroProveedorTexto", proveedor == null ? null : textoDeProveedor(proveedor.getVersion()));
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("filtroFechaEntregaEstimada", fechaEntregaEstimada);
        return VISTA_LISTA;
    }

    // ==================== Alta ====================

    /**
     * Muestra el formulario vacío para registrar una orden de compra.
     */
    @GetMapping("/nuevo")
    @PreAuthorize(SOLO_GERENTE)
    public String mostrarFormularioAlta(Model model) {
        OrdenCompraFormDTO ordenCompraForm = new OrdenCompraFormDTO();
        model.addAttribute(ATRIBUTO_FORMULARIO, ordenCompraForm);
        cargarModeloFormulario(model, ordenCompraForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el registro de una orden de compra (Post-Redirect-Get). Si la validación del formulario o una regla del
     * service falla, se vuelve a renderizar el formulario (sin redirigir) con todo lo que el usuario había cargado,
     * incluidas las filas de ítems. Al registrarla se redirige a su detalle.
     */
    @PostMapping
    @PreAuthorize(SOLO_GERENTE)
    public String registrarOrdenCompra(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) OrdenCompraFormDTO ordenCompraForm,
                                       BindingResult bindingResult,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                OrdenCompraResponseDTO orden = ordenCompraServicio.registrarOrdenCompra(ordenCompraForm);
                agregarMensaje(redirectAttributes, "Orden de compra registrada correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA + "/" + orden.getId();
            } catch (RecursoNoEncontradoException | ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, ordenCompraForm);
        return VISTA_FORMULARIO;
    }

    // ==================== Detalle ====================

    /**
     * Muestra el detalle de la orden: sus datos, sus ítems con las cantidades recibida y pendiente, y su total. Si
     * no existe, el {@code ControllerAdvices} vuelve al origen con el motivo.
     */
    @GetMapping("/{id}")
    @PreAuthorize(GERENTE_O_ENCARGADO)
    public String mostrarDetalle(@PathVariable Long id, Model model) {
        model.addAttribute("orden", ordenCompraServicio.buscarPorId(id));
        return VISTA_DETALLE;
    }

    // ==================== Cierres manuales ====================

    /**
     * Anula una orden pendiente. El motivo es obligatorio: si falta o el service rechaza la anulación (por el estado
     * de la orden o por tener ingresos registrados), el {@code ControllerAdvices} vuelve al origen (el detalle de la
     * orden) con el motivo del rechazo.
     */
    @PostMapping("/{id}/anular")
    @PreAuthorize(SOLO_GERENTE)
    public String anularOrdenCompra(@PathVariable Long id,
                                    @RequestParam(required = false) String motivo,
                                    RedirectAttributes redirectAttributes) {
        ordenCompraServicio.anularOrdenCompra(id, AnulacionOrdenCompraFormDTO.builder().motivoAnulacion(motivo).build());
        agregarMensaje(redirectAttributes, "Orden de compra anulada correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA + "/" + id;
    }

    /**
     * Finaliza de forma forzada una orden pendiente. El motivo es obligatorio; las reglas las valida el service y el
     * {@code ControllerAdvices} muestra el motivo de un rechazo.
     */
    @PostMapping("/{id}/finalizar")
    @PreAuthorize(SOLO_GERENTE)
    public String finalizarOrdenCompra(@PathVariable Long id,
                                       @RequestParam(required = false) String motivo,
                                       RedirectAttributes redirectAttributes) {
        ordenCompraServicio.finalizarOrdenCompra(id, FinalizacionForzadaOrdenCompraFormDTO.builder().motivoFinalizacion(motivo).build());
        agregarMensaje(redirectAttributes, "Orden de compra finalizada correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA + "/" + id;
    }

    // ==================== Buscadores del formulario de alta ====================

    /**
     * Devuelve el contenido del modal de búsqueda de planificaciones del formulario de alta (criterio de receta +
     * resultados), de a {@code TAMANIO_PAGINA_BUSCADOR} y con la última registrada arriba. Solo se ofrecen las pendientes: es el único estado sobre
     * el que se registra una orden de compra. La receta se elige con su propio buscador, que solo ofrece recetas
     * activas: un {@code idReceta} que no es de una receta activa se ignora.
     */
    @GetMapping("/buscador/planificaciones")
    @PreAuthorize(SOLO_GERENTE)
    public String buscarPlanificaciones(@RequestParam(required = false) Long idReceta,
                                        @RequestParam(defaultValue = "0") int pagina,
                                        Model model) {
        cargarBuscadorDePlanificaciones(model, recetaElegida(idReceta, recetaServicio::buscarPorId), idReceta,
                EstadoSolicitud.PENDIENTE, pagina, URL_BUSCADOR_PLANIFICACIONES, URL_BUSCADOR_RECETAS,
                "Filtre y elija la planificación pendiente de la compra.");
        return VISTA_BUSCADOR_PLANIFICACION;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de recetas del criterio "Receta" del buscador de planificaciones del
     * formulario de alta (criterio de nombre + resultados), de a {@code TAMANIO_PAGINA_BUSCADOR} y en orden alfabético. Solo se ofrecen las recetas
     * activas. El orden lo define la consulta del repositorio, por eso el {@code Pageable} no lleva ordenamiento.
     */
    @GetMapping("/buscador/recetas")
    @PreAuthorize(SOLO_GERENTE)
    public String buscarRecetas(@RequestParam(required = false) String nombre,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        cargarBuscadorDeRecetas(model, recetaServicio.filtrarRecetas(vacioANulo(nombre), paginaDeBuscador(pagina)),
                nombre, URL_BUSCADOR_RECETAS, false, false);
        return VISTA_BUSCADOR_RECETA;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de proveedores (razón social y CUIT + resultados), de a {@code TAMANIO_PAGINA_BUSCADOR} y en orden
     * alfabético por razón social (lo define la consulta del repositorio). Lo usan el formulario de alta y el filtro
     * del listado, por eso lo ven los mismos roles que ven el listado. Solo se ofrecen proveedores activos.
     */
    @GetMapping("/buscador/proveedores")
    @PreAuthorize(GERENTE_O_ENCARGADO)
    public String buscarProveedores(@RequestParam(required = false) String razonSocial,
                                    @RequestParam(required = false) String cuit,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        Page<ProveedorResponseDTO> proveedores = proveedorServicio.filtrarProveedores(
                vacioANulo(razonSocial), null, vacioANulo(cuit), null, paginaDeBuscador(pagina));
        Map<Long, String> textosPorId = new HashMap<>();
        proveedores.forEach(proveedor -> textosPorId.put(proveedor.getId(), textoDeProveedor(proveedor.getVersion())));

        model.addAttribute("proveedores", proveedores);
        model.addAttribute("textosPorId", textosPorId);
        model.addAttribute("filtroRazonSocial", vacioANulo(razonSocial));
        model.addAttribute("filtroCuit", vacioANulo(cuit));
        return VISTA_BUSCADOR_PROVEEDOR;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de ítems del formulario de alta (insumo y presentación comercial +
     * resultados), de a {@code TAMANIO_PAGINA_BUSCADOR} y en orden alfabético por insumo. Solo ofrece los ítems de la versión vigente del catálogo
     * del proveedor elegido cuyo insumo forma parte de la versión de receta de la planificación elegida, y deja afuera
     * los que la orden ya tiene ({@code idsAgregados}).
     * <p>
     * Sin planificación o sin proveedor no hay nada que ofrecer: el modal muestra el motivo como texto. Si alguno no
     * existe, el {@code ControllerAdvices} responde con el motivo.
     * </p>
     */
    @GetMapping("/buscador/items")
    @PreAuthorize(SOLO_GERENTE)
    public String buscarItems(@RequestParam(required = false) Long idProveedor,
                              @RequestParam(required = false) Long idPlanificacion,
                              @RequestParam(required = false) String insumo,
                              @RequestParam(required = false) String presentacion,
                              @RequestParam(required = false) List<Long> idsAgregados,
                              @RequestParam(defaultValue = "0") int pagina,
                              Model model) {
        if (idProveedor == null || idPlanificacion == null) {
            throw new ReglaNegocioException("Elija primero la planificación de producción y el proveedor");
        }
        // Una orden tiene a lo sumo ORDEN_COMPRA_MAXIMO_ITEMS ítems: los ids sobrantes del pedido se ignoran
        List<Long> agregados = idsAgregados == null ? List.of()
                : idsAgregados.stream().filter(Objects::nonNull).limit(LimitesTecnicos.ORDEN_COMPRA_MAXIMO_ITEMS).toList();
        VersionProveedorResponseDTO version = proveedorServicio.buscarPorId(idProveedor).getVersion();

        model.addAttribute("items", BusquedaItemsCatalogo.buscar(version,
                planificacionServicio.buscarVersionRecetaDePlanificacion(idPlanificacion), agregados,
                vacioANulo(insumo), vacioANulo(presentacion), paginaDeBuscador(pagina)));
        model.addAttribute("filtroInsumo", vacioANulo(insumo));
        model.addAttribute("filtroPresentacion", vacioANulo(presentacion));
        model.addAttribute("idProveedor", idProveedor);
        model.addAttribute("idPlanificacion", idPlanificacion);
        model.addAttribute("idsAgregados", agregados.isEmpty() ? null
                : String.join(",", agregados.stream().map(String::valueOf).toList()));
        return VISTA_BUSCADOR_ITEM;
    }

    // ==================== Buscadores de los filtros del listado ====================

    /**
     * Devuelve el contenido del modal de búsqueda de planificaciones del filtro del listado (criterio de receta +
     * resultados), de a {@code TAMANIO_PAGINA_BUSCADOR} y con la última registrada arriba. A diferencia del formulario de alta, ofrece las de
     * cualquier estado, porque filtra el historial. La receta se elige con el buscador de recetas del filtro, que
     * también ofrece las dadas de baja con planificaciones: un {@code idReceta} que ese buscador no ofrece se ignora.
     */
    @GetMapping("/filtros/planificaciones")
    @PreAuthorize(GERENTE_O_ENCARGADO)
    public String filtrarPlanificaciones(@RequestParam(required = false) Long idReceta,
                                         @RequestParam(defaultValue = "0") int pagina,
                                         Model model) {
        cargarBuscadorDePlanificaciones(model, recetaElegida(idReceta, recetaServicio::buscarActivaODadaDeBajaConPlanificacionesPorId),
                idReceta, null, pagina, URL_FILTRO_PLANIFICACIONES, URL_FILTRO_RECETAS,
                "Filtre y elija la planificación de las órdenes de compra.");
        return VISTA_BUSCADOR_PLANIFICACION;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de recetas del criterio "Receta" del buscador de planificaciones del
     * filtro del listado (nombre e "Incluir dados de baja" + resultados), de a {@code TAMANIO_PAGINA_BUSCADOR} y en orden alfabético. Sin el último
     * criterio solo se ofrecen las recetas activas; con él se suman las dadas de baja que tienen alguna planificación.
     */
    @GetMapping("/filtros/recetas")
    @PreAuthorize(GERENTE_O_ENCARGADO)
    public String filtrarRecetas(@RequestParam(required = false) String nombre,
                                 @RequestParam(defaultValue = "false") boolean incluirDadosDeBaja,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        String nombreBuscado = vacioANulo(nombre);
        Pageable pageable = paginaDeBuscador(pagina);
        Page<RecetaResponseDTO> recetas = incluirDadosDeBaja
                ? recetaServicio.filtrarRecetasConDadasDeBajaConPlanificaciones(nombreBuscado, pageable)
                : recetaServicio.filtrarRecetas(nombreBuscado, pageable);
        cargarBuscadorDeRecetas(model, recetas, nombre, URL_FILTRO_RECETAS, true, incluirDadosDeBaja);
        return VISTA_BUSCADOR_RECETA;
    }

    // ==================== Modelo del formulario ====================

    /**
     * Carga lo que la vista del formulario necesita además del DTO: los textos de la planificación y del proveedor
     * elegidos, los datos de los ítems del catálogo referenciados por las filas (para mostrar sus nombres sin agregar
     * campos de solo lectura al DTO) y el total de las filas. Lo que ya no existe queda sin entrada y la vista lo indica.
     */
    private void cargarModeloFormulario(Model model, OrdenCompraFormDTO ordenCompraForm) {
        model.addAttribute("urlCancelar", URL_LISTA);
        model.addAttribute("planificacionTexto", etiquetaPlanificacion(ordenCompraForm.getIdPlanificacionProduccion()));

        ProveedorResponseDTO proveedor = proveedorElegido(ordenCompraForm.getIdProveedor());
        model.addAttribute("proveedorTexto", proveedor == null ? null : textoDeProveedor(proveedor.getVersion()));

        Map<Long, CatalogoProveedorResponseDTO> itemsPorId = new HashMap<>();
        if (proveedor != null && proveedor.getVersion().getCatalogoProveedor() != null) {
            proveedor.getVersion().getCatalogoProveedor().forEach(item -> itemsPorId.put(item.getId(), item));
        }
        model.addAttribute("itemsPorId", itemsPorId);
        model.addAttribute("total", totalDeFilas(ordenCompraForm));
    }

    /**
     * La suma de cantidad por costo unitario de las filas completas del formulario.
     */
    private static BigDecimal totalDeFilas(OrdenCompraFormDTO ordenCompraForm) {
        if (ordenCompraForm.getDetallesCompra() == null) {
            return BigDecimal.ZERO;
        }
        return ordenCompraForm.getDetallesCompra().stream()
                .filter(Objects::nonNull)
                .filter(OrdenCompraController::tieneCantidadYCostoValidos)
                .map((DetalleCompraFormDTO detalle) -> detalle.getCostoUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Una fila entra en el total solo si cumple las mismas reglas que el service: cantidad de 1 a
     * {@code CANTIDAD_COMPRA_MAXIMA} y costo unitario mayor a cero, de hasta {@code COSTO_UNITARIO_COMPRA_MAXIMO} y de hasta
     * {@code ESCALA_COSTO_UNITARIO} decimales. Las demás, que el service rechaza, se ignoran (el total solo se muestra).
     */
    private static boolean tieneCantidadYCostoValidos(DetalleCompraFormDTO detalle) {
        BigDecimal costo = detalle.getCostoUnitario();
        return detalle.getCantidad() != null && detalle.getCantidad() > 0 && detalle.getCantidad() <= LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA
                && costo != null && costo.signum() > 0
                && costo.compareTo(BigDecimal.valueOf(LimitesTecnicos.COSTO_UNITARIO_COMPRA_MAXIMO)) <= 0
                && costo.stripTrailingZeros().scale() <= LimitesTecnicos.ESCALA_COSTO_UNITARIO;
    }

    /**
     * Texto con el que un selector muestra la planificación elegida ("Plan #7 - Pale Ale"), o {@code null} si no hay
     * selección o ya no existe.
     */
    private String etiquetaPlanificacion(Long idPlanificacion) {
        PlanificacionProduccionResponseDTO planificacion = resolverUno(idPlanificacion, planificacionServicio::buscarPorId);
        return planificacion == null ? null : planificacion.getEtiqueta();
    }

    /**
     * El proveedor elegido, o {@code null} si no hay selección o ya no existe.
     */
    private ProveedorResponseDTO proveedorElegido(Long idProveedor) {
        return resolverUno(idProveedor, proveedorServicio::buscarPorId);
    }

    /**
     * Texto con el que un selector muestra el proveedor elegido: su razón social y, entre paréntesis, su CUIT con
     * guiones (la razón social no es única).
     */
    private static String textoDeProveedor(VersionProveedorResponseDTO version) {
        return version.getRazonSocial() + " (" + ValidacionesTexto.formatearCuit(version.getCuit()) + ")";
    }

    // ==================== Apoyo: buscadores ====================

    private static Pageable paginaDeBuscador(int pagina) {
        return PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR);
    }

    /**
     * Arma el modal de un buscador de planificaciones: las planificaciones del estado indicado ({@code null} para
     * cualquiera) de la receta elegida, de a {@code TAMANIO_PAGINA_BUSCADOR} y con la última registrada arriba, y el selector de receta con la URL de
     * su propio buscador. Si la receta no se resolvió, se busca sin filtrar por receta y el selector queda en blanco.
     */
    private void cargarBuscadorDePlanificaciones(Model model, RecetaResponseDTO receta, Long idReceta, EstadoSolicitud estado, int pagina,
                                                 String urlBuscador, String urlBuscadorRecetas, String subtitulo) {
        Long idRecetaFiltro = receta != null ? idReceta : null;
        model.addAttribute("planificaciones", planificacionServicio.filtrarPlanificacionesProduccion(
                idRecetaFiltro, estado, null,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR, ORDEN_MAS_RECIENTE_PRIMERO)));
        model.addAttribute("filtroIdReceta", idRecetaFiltro);
        model.addAttribute("filtroRecetaTexto", receta != null ? textoDeReceta(receta) : null);
        model.addAttribute("urlBuscador", urlBuscador);
        model.addAttribute("urlBuscadorRecetas", urlBuscadorRecetas);
        model.addAttribute("subtituloBuscador", subtitulo);
    }
}
