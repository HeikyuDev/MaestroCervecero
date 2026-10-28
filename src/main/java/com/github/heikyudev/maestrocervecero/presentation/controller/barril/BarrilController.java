package com.github.heikyudev.maestrocervecero.presentation.controller.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFabricanteBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de la gestión de barriles, la primera pestaña de "Gestión de Barriles". Todo es del
 * GERENTE_DE_PRODUCCION, incluido el buscador de fabricantes que usan los selectores. El estado
 * operativo y el contenido actual no se cargan a mano: un barril nuevo nace DISPONIBLE y vacío, y
 * después lo cambian las operaciones del ciclo de vida.
 * <p>
 * El fabricante se elige con un selector (modal de búsqueda) sin alta al vuelo: los fabricantes se
 * gestionan en su propia pestaña.
 * </p>
 */
@Controller
@RequestMapping("/barriles")
@RequiredArgsConstructor
public class BarrilController {

    private static final String VISTA_LISTA = "barril/barril-lista";
    private static final String VISTA_BUSCADOR_FABRICANTE = "barril/buscador-fabricante :: buscador";
    private static final String VISTA_FORMULARIO = "barril/barril-form";
    private static final String ATRIBUTO_FORMULARIO = "barrilForm";
    private static final String URL_LISTA = "/barriles";

    /**
     * Los barriles se listan en orden alfabético por identificador interno (sin distinguir mayúsculas), con el id como desempate.
     */
    private static final Sort ORDEN_POR_IDENTIFICADOR_DE_BARRIL = Sort.by(
            Sort.Order.asc("identificadorInterno").ignoreCase(),
            Sort.Order.asc("id"));

    private final IBarrilServicio barrilServicio;
    private final IFabricanteBarrilServicio fabricanteBarrilServicio;

    /**
     * Lista los barriles activos, paginados de a 20, en orden alfabético por identificador interno. Los criterios
     * (identificador interno, número de serie, capacidad, estado operativo y fabricante) son opcionales.
     */
    @GetMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String listarBarriles(@RequestParam(required = false) String identificadorInterno,
                                 @RequestParam(required = false) String numeroSerie,
                                 @RequestParam(required = false) Double capacidad,
                                 @RequestParam(required = false) EstadoOperativoBarril estadoOperativo,
                                 @RequestParam(required = false) Long idFabricante,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        String fabricanteTexto = nombreFabricante(idFabricante);
        Long idFabricanteVigente = fabricanteTexto == null ? null : idFabricante;

        Page<BarrilResponseDTO> barriles = barrilServicio.filtrarBarriles(
                vacioANulo(identificadorInterno), vacioANulo(numeroSerie), capacidad, estadoOperativo, idFabricanteVigente,
                PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_POR_IDENTIFICADOR_DE_BARRIL));

        model.addAttribute("barriles", barriles);
        model.addAttribute("estados", EstadoOperativoBarril.values());
        model.addAttribute("filtroIdentificador", identificadorInterno);
        model.addAttribute("filtroNumeroSerie", numeroSerie);
        // El valor ya viene convertido a número: numeroEscrito solo le quita el ".0" (50.0 se muestra como 50)
        model.addAttribute("filtroCapacidad", capacidad == null ? null : numeroEscrito(capacidad, capacidad));
        model.addAttribute("filtroEstado", estadoOperativo);
        model.addAttribute("filtroIdFabricante", idFabricanteVigente);
        model.addAttribute("filtroFabricanteTexto", fabricanteTexto);
        return VISTA_LISTA;
    }

    /**
     * Devuelve el contenido del modal de búsqueda de fabricantes de barril (criterio de nombre + resultados),
     * que usan el selector del formulario y el filtro del listado.
     */
    @GetMapping("/buscador/fabricantes")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String buscarFabricantes(@RequestParam(required = false) String nombre,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        Page<FabricanteBarrilResponseDTO> fabricantes = fabricanteBarrilServicio.filtrarFabricantesBarril(
                vacioANulo(nombre), PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR, ORDEN_ALFABETICO));

        model.addAttribute("fabricantes", fabricantes);
        model.addAttribute("filtroNombre", nombre);
        return VISTA_BUSCADOR_FABRICANTE;
    }

    /**
     * Muestra el formulario vacío para dar de alta un barril.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioAlta(Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new BarrilFormDTO());
        cargarModeloFormulario(model, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta del barril (Post-Redirect-Get). Si la validación del formulario o una regla del
     * service falla, se vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String altaBarril(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) BarrilFormDTO barrilForm,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                barrilServicio.altaBarril(barrilForm);
                agregarMensaje(redirectAttributes, "Barril creado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("numeroSerie", "duplicado", e.getMessage());
            } catch (RecursoNoEncontradoException e) {
                bindingResult.rejectValue("idFabricanteBarril", "noEncontrado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, null, barrilForm.getIdFabricanteBarril());
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de edición con los datos actuales del barril.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        BarrilResponseDTO actual = barrilServicio.buscarPorId(id);
        Long idFabricante = actual.getFabricante().getId();

        model.addAttribute(ATRIBUTO_FORMULARIO, BarrilFormDTO.builder()
                .numeroSerie(actual.getNumeroSerie())
                .capacidad(actual.getCapacidad())
                .usosMaximosAntesMantenimiento(actual.getUsosMaximosAntesMantenimiento())
                .idFabricanteBarril(idFabricante)
                .build());
        model.addAttribute("identificadorInterno", actual.getIdentificadorInterno());
        cargarModeloFormulario(model, id, idFabricante);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación del barril (Post-Redirect-Get). El service la rechaza si los datos incumplen una
     * regla (por ejemplo, una capacidad menor al contenido actual); el motivo se muestra en el formulario.
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String modificarBarril(@PathVariable Long id,
                                  @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) BarrilFormDTO barrilForm,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                barrilServicio.modificarBarril(id, barrilForm);
                agregarMensaje(redirectAttributes, "Barril modificado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("numeroSerie", "duplicado", e.getMessage());
            } catch (RecursoNoEncontradoException e) {
                // Si el fabricante del formulario sigue existiendo, lo que falta es el barril: lo muestra el ControllerAdvices
                if (nombreFabricante(barrilForm.getIdFabricanteBarril()) != null) {
                    throw e;
                }
                bindingResult.rejectValue("idFabricanteBarril", "noEncontrado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        model.addAttribute("identificadorInterno", barrilServicio.buscarPorId(id).getIdentificadorInterno());
        cargarModeloFormulario(model, id, barrilForm.getIdFabricanteBarril());
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) al barril. Si no está disponible, el service lo rechaza y el
     * {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String bajaBarril(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        barrilServicio.bajaBarril(id);
        agregarMensaje(redirectAttributes, "Barril dado de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    private void cargarModeloFormulario(Model model, Long barrilId, Long idFabricante) {
        model.addAttribute("barrilId", barrilId);
        model.addAttribute("fabricanteSeleccionado", nombreFabricante(idFabricante));
        model.addAttribute("urlCancelar", URL_LISTA);
    }

    /**
     * Nombre del fabricante elegido en un selector, o {@code null} si no hay selección o ya no existe.
     */
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
}
