package com.github.heikyudev.maestrocervecero.presentation.controller.receta;

import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de la gestión de recetas, la primera de las pestañas de "Gestión de Recetas" (junto
 * con las etapas de control y los parámetros de control). Todo el módulo es del
 * GERENTE_DE_PRODUCCION.
 * <p>
 * Una receta se muestra siempre con los datos de su versión vigente: el listado y el detalle no
 * exponen el historial de versiones.
 * </p>
 */
@Controller
@RequestMapping("/recetas")
@RequiredArgsConstructor
public class RecetaController {

    private static final String VISTA_LISTA = "receta/receta-lista";
    private static final String VISTA_DETALLE = "receta/receta-detalle";
    private static final String URL_LISTA = "/recetas";

    private final IRecetaServicio recetaServicio;

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
     * Muestra el detalle completo de una receta (los datos de su versión vigente).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarDetalle(@PathVariable Long id, Model model) {
        model.addAttribute("receta", recetaServicio.buscarPorId(id));
        return VISTA_DETALLE;
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
}
