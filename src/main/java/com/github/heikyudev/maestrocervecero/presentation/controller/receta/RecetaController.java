package com.github.heikyudev.maestrocervecero.presentation.controller.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleLevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleLupuloFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleMaltaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleParametroControlFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.PlanMonitoreoEtapaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.RecetaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.etapa_control.IEtapaControlServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILevaduraServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILupuloServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IMaltaServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.parametro_control.IParametroControlServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.etapa_control.EtapaControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.parametro_control.ParametroControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import com.github.heikyudev.maestrocervecero.util.formatter.GravedadFormatter;
import com.github.heikyudev.maestrocervecero.util.mapper.receta.MapperRecetaForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de la gestión de recetas, la primera de las pestañas de "Gestión de Recetas" (junto
 * con las etapas de control y los parámetros de control). Todo el módulo es del
 * GERENTE_DE_PRODUCCION.
 * <p>
 * Una receta se muestra siempre con los datos de su versión vigente: el listado y el detalle no
 * exponen el historial de versiones.
 * </p>
 * <p>
 * El formulario de alta y de modificación es una única página. Las maltas, los lúpulos y las
 * levaduras se agregan desde una ventana emergente que elige el insumo con un buscador (modal, igual
 * que el selector de localidades); las filas agregadas viajan como campos indexados del
 * {@link RecetaFormDTO}. Modificar una receta crea una nueva versión.
 * </p>
 * <p>
 * El plan de monitoreo (opcional) se arma igual: cada plan elige una etapa de control con un buscador
 * y se le agregan sus parámetros de control (también elegidos con un buscador) con el mínimo, el ideal
 * y el máximo. Los planes viajan como campos indexados de dos niveles
 * ({@code version.planesMonitoreo[i].detallesParametroControl[j].valorMinimo}) y modificar un plan es
 * quitarlo y agregarlo de nuevo.
 * </p>
 */
@Controller
@RequestMapping("/recetas")
@RequiredArgsConstructor
public class RecetaController {

    private static final String VISTA_LISTA = "receta/receta-lista";
    private static final String VISTA_DETALLE = "receta/receta-detalle";
    private static final String VISTA_FORMULARIO = "receta/receta-form";
    private static final String VISTA_BUSCADOR_MALTA = "receta/buscador-malta :: buscador";
    private static final String VISTA_BUSCADOR_LUPULO = "receta/buscador-lupulo :: buscador";
    private static final String VISTA_BUSCADOR_LEVADURA = "receta/buscador-levadura :: buscador";
    private static final String VISTA_BUSCADOR_ETAPA_CONTROL = "receta/buscador-etapa-control :: buscador";
    private static final String VISTA_BUSCADOR_PARAMETRO_CONTROL = "receta/buscador-parametro-control :: buscador";
    private static final String ATRIBUTO_FORMULARIO = "recetaForm";
    private static final String URL_LISTA = "/recetas";
    private static final List<TipoEtapa> ETAPAS_CONTROLABLES = List.of(
            TipoEtapa.MACERACION, TipoEtapa.HERVIDO, TipoEtapa.FERMENTACION, TipoEtapa.MADURACION);

    private final IRecetaServicio recetaServicio;
    private final IMaltaServicio maltaServicio;
    private final ILupuloServicio lupuloServicio;
    private final ILevaduraServicio levaduraServicio;
    private final IEtapaControlServicio etapaControlServicio;
    private final IParametroControlServicio parametroControlServicio;

    /**
     * La OG y la FG se ingresan con cuatro dígitos ({@code 1050} equivale a 1,050): el
     * {@link GravedadFormatter} las convierte al enlazar el formulario y las vuelve a mostrar así.
     */
    @InitBinder
    public void configurarFormateoDeDensidades(WebDataBinder binder) {
        GravedadFormatter formatter = new GravedadFormatter();
        binder.addCustomFormatter(formatter, "version.ogObjetivo", "version.fgObjetivo");
    }

    /**
     * Lista las recetas activas, paginadas de a 20. El criterio (nombre de la versión vigente) es
     * opcional. El orden alfabético por nombre lo define la consulta del repositorio, porque el nombre
     * vive en la versión y no en la receta: por eso el {@code Pageable} no lleva ordenamiento.
     */
    @GetMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String listarRecetas(@RequestParam(required = false) String nombre,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        Page<RecetaResponseDTO> recetas = recetaServicio.filtrarRecetas(
                vacioANulo(nombre), PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA));

        model.addAttribute("recetas", recetas);
        model.addAttribute("filtroNombre", nombre);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta una receta.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioAlta(Model model) {
        RecetaFormDTO recetaForm = new RecetaFormDTO();
        model.addAttribute(ATRIBUTO_FORMULARIO, recetaForm);
        cargarModeloFormulario(model, null, recetaForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de una receta (Post-Redirect-Get). Si la validación del formulario o una regla
     * del service falla, se vuelve a renderizar el formulario (sin redirigir) con todo lo que el
     * usuario había cargado, incluidas las filas de las tablas.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String altaReceta(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) RecetaFormDTO recetaForm,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                recetaServicio.altaReceta(recetaForm);
                agregarMensaje(redirectAttributes, "Receta creada correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("version.nombre", "duplicado", e.getMessage());
            } catch (RecursoNoEncontradoException | ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, null, recetaForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el detalle completo de una receta (los datos de su versión vigente).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarDetalle(@PathVariable Long id, Model model) {
        model.addAttribute("receta", recetaServicio.buscarPorId(id));
        return VISTA_DETALLE;
    }

    /**
     * Muestra el formulario de modificación con los datos de la versión vigente de la receta. Si la
     * receta no existe, el {@code ControllerAdvices} vuelve al origen con el motivo.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        RecetaFormDTO recetaForm = MapperRecetaForm.toFormDTO(recetaServicio.buscarPorId(id));
        model.addAttribute(ATRIBUTO_FORMULARIO, recetaForm);
        cargarModeloFormulario(model, id, recetaForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de una receta (Post-Redirect-Get): el service registra una nueva versión.
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String modificarReceta(@PathVariable Long id,
                                  @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) RecetaFormDTO recetaForm,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                recetaServicio.modificarReceta(id, recetaForm);
                agregarMensaje(redirectAttributes, "Receta modificada correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("version.nombre", "duplicado", e.getMessage());
            } catch (RecursoNoEncontradoException e) {
                // Si falta la propia receta esta consulta lanza la excepción y el ControllerAdvices vuelve
                // al origen; si no, lo que falta es un insumo y se informa en el formulario
                recetaServicio.buscarPorId(id);
                bindingResult.reject("noEncontrado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, id, recetaForm);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) a una receta. Si tiene una planificación de producción pendiente el
     * service lo rechaza y el {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String bajaReceta(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        recetaServicio.bajaReceta(id);
        agregarMensaje(redirectAttributes, "Receta dada de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    // ==================== Buscadores de insumos del formulario ====================

    /**
     * Devuelve el contenido del modal de búsqueda de maltas activas (criterios de filtro + resultados),
     * en orden alfabético.
     */
    @GetMapping("/buscador/maltas")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String buscarMaltas(@RequestParam(required = false) String nombre,
                               @RequestParam(required = false) TipoMalta tipo,
                               @RequestParam(defaultValue = "0") int pagina,
                               Model model) {
        Page<MaltaResponseDTO> maltas = maltaServicio.filtrarMaltas(vacioANulo(nombre), tipo,
                PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));

        model.addAttribute("maltas", maltas);
        model.addAttribute("tipos", TipoMalta.values());
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroTipo", tipo);
        return VISTA_BUSCADOR_MALTA;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de lúpulos activos (criterios de filtro + resultados),
     * en orden alfabético.
     */
    @GetMapping("/buscador/lupulos")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String buscarLupulos(@RequestParam(required = false) String nombre,
                                @RequestParam(required = false) FormatoLupulo formato,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        Page<LupuloResponseDTO> lupulos = lupuloServicio.filtrarLupulos(vacioANulo(nombre), formato,
                PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));

        model.addAttribute("lupulos", lupulos);
        model.addAttribute("formatos", FormatoLupulo.values());
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroFormato", formato);
        return VISTA_BUSCADOR_LUPULO;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de levaduras activas (criterios de filtro + resultados),
     * en orden alfabético.
     */
    @GetMapping("/buscador/levaduras")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String buscarLevaduras(@RequestParam(required = false) String nombre,
                                  @RequestParam(required = false) TipoLevadura tipo,
                                  @RequestParam(defaultValue = "0") int pagina,
                                  Model model) {
        Page<LevaduraResponseDTO> levaduras = levaduraServicio.filtrarLevaduras(vacioANulo(nombre), tipo,
                PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));

        model.addAttribute("levaduras", levaduras);
        model.addAttribute("tipos", TipoLevadura.values());
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroTipo", tipo);
        return VISTA_BUSCADOR_LEVADURA;
    }

    // ==================== Buscadores del plan de monitoreo ====================

    /**
     * Devuelve el contenido del modal de búsqueda de etapas de control activas (criterios de filtro +
     * resultados), en orden alfabético. La etapa a controlar se elige entre las cuatro controlables.
     */
    @GetMapping("/buscador/etapas-control")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String buscarEtapasControl(@RequestParam(required = false) String nombre,
                                      @RequestParam(required = false) TipoEtapa etapa,
                                      @RequestParam(defaultValue = "0") int pagina,
                                      Model model) {
        Page<EtapaControlResponseDTO> etapasControl = etapaControlServicio.filtrarEtapasControl(vacioANulo(nombre), etapa,
                PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));

        model.addAttribute("etapasControl", etapasControl);
        model.addAttribute("etapas", ETAPAS_CONTROLABLES);
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroEtapa", etapa);
        return VISTA_BUSCADOR_ETAPA_CONTROL;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de parámetros de control activos (criterio de filtro +
     * resultados con su unidad y su rango admitido), en orden alfabético.
     */
    @GetMapping("/buscador/parametros-control")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String buscarParametrosControl(@RequestParam(required = false) String nombre,
                                          @RequestParam(defaultValue = "0") int pagina,
                                          Model model) {
        Page<ParametroControlResponseDTO> parametrosControl = parametroControlServicio.filtrarParametrosControl(vacioANulo(nombre),
                PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));

        model.addAttribute("parametrosControl", parametrosControl);
        model.addAttribute("filtroNombre", nombre);
        return VISTA_BUSCADOR_PARAMETRO_CONTROL;
    }

    // ==================== Modelo del formulario ====================

    /**
     * Carga lo que la vista necesita además del formulario: el id de la receta (null en el alta) y los
     * datos de los insumos, etapas y parámetros referenciados por las filas, para mostrar sus nombres
     * sin agregar campos de solo lectura al DTO. Lo que ya no existe queda sin entrada en el mapa y la
     * vista lo indica.
     */
    private void cargarModeloFormulario(Model model, Long recetaId, RecetaFormDTO recetaForm) {
        model.addAttribute("recetaId", recetaId);
        model.addAttribute("urlCancelar", URL_LISTA);

        List<DetalleMaltaFormDTO> maltas = recetaForm.getVersion().getDetallesMalta();
        List<DetalleLupuloFormDTO> lupulos = recetaForm.getVersion().getDetallesLupulo();
        List<DetalleLevaduraFormDTO> levaduras = recetaForm.getVersion().getDetallesLevadura();
        List<PlanMonitoreoEtapaFormDTO> planes = recetaForm.getVersion().getPlanesMonitoreo();

        model.addAttribute("maltasPorId", resolver(
                maltas.stream().map(DetalleMaltaFormDTO::getIdMalta).toList(), maltaServicio::buscarPorId));
        model.addAttribute("lupulosPorId", resolver(
                lupulos.stream().map(DetalleLupuloFormDTO::getIdLupulo).toList(), lupuloServicio::buscarPorId));
        model.addAttribute("levadurasPorId", resolver(
                levaduras.stream().map(DetalleLevaduraFormDTO::getIdLevadura).toList(), levaduraServicio::buscarPorId));
        model.addAttribute("etapasControlPorId", resolver(
                planes.stream().map(PlanMonitoreoEtapaFormDTO::getIdEtapaControl).toList(), etapaControlServicio::buscarPorId));
        model.addAttribute("parametrosControlPorId", resolver(
                planes.stream()
                        .flatMap(plan -> plan.getDetallesParametroControl().stream())
                        .map(DetalleParametroControlFormDTO::getIdParametroControl).toList(),
                parametroControlServicio::buscarPorId));
    }

    /**
     * Busca una vez cada id (ignorando los nulos) y arma el mapa id -> datos. Un id que ya no existe
     * no tiene entrada.
     */
    private static <T> Map<Long, T> resolver(Collection<Long> ids, Function<Long, T> buscar) {
        Map<Long, T> resultado = new HashMap<>();
        Stream<Long> distintos = ids.stream().filter(Objects::nonNull).distinct();
        distintos.forEach(id -> {
            try {
                T encontrado = buscar.apply(id);
                if (encontrado != null) {
                    resultado.put(id, encontrado);
                }
            } catch (RecursoNoEncontradoException e) {
                // sin entrada: la vista indica que el dato ya no está disponible
            }
        });
        return resultado;
    }
}
