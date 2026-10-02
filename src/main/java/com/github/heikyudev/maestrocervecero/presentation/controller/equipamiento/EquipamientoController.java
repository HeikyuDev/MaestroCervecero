package com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller del resumen de equipamiento: la pestaña inicial de "Gestión de Equipamiento". Muestra
 * cuántos equipos hay de cada tipo en cada estado operativo y una tabla con todos los equipos, el lote
 * en el que se usan ahora y los lotes que los tienen reservados.
 * <p>
 * Es de solo lectura y la ven el GERENTE_DE_PRODUCCION y el OPERARIO_DE_PRODUCCION. El alta, la
 * modificación y la baja de cada tipo están en sus propios controllers y son solo del gerente.
 * </p>
 */
@Controller
@RequestMapping("/equipamiento")
@RequiredArgsConstructor
public class EquipamientoController {

    private static final String VISTA_RESUMEN = "equipamiento/resumen";

    private final IEquipamientoServicio equipamientoServicio;

    /**
     * Muestra el resumen: los totales por tipo y estado, y la tabla de equipos paginada de a 20, en
     * orden alfabético por identificador. Los criterios (identificador, tipo y estado operativo) son
     * opcionales y filtran solo la tabla; los totales siempre cuentan todo el equipamiento activo.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('GERENTE_DE_PRODUCCION', 'OPERARIO_DE_PRODUCCION')")
    public String mostrarResumen(@RequestParam(required = false) String identificador,
                                 @RequestParam(required = false) TipoEquipamiento tipo,
                                 @RequestParam(required = false) EstadoOperativo estadoOperativo,
                                 @RequestParam(defaultValue = "0") int pagina,
                                 Model model) {
        Page<EquipamientoResumenResponseDTO> equipos = equipamientoServicio.filtrarResumenEquipamientos(
                vacioANulo(identificador), tipo, estadoOperativo,
                PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA, ORDEN_POR_IDENTIFICADOR));

        model.addAttribute("resumen", equipamientoServicio.obtenerResumen().getCantidadesPorTipoYEstado());
        model.addAttribute("equipos", equipos);
        model.addAttribute("tipos", TipoEquipamiento.values());
        model.addAttribute("estados", EstadoOperativo.values());
        model.addAttribute("filtroIdentificador", identificador);
        model.addAttribute("filtroTipo", tipo);
        model.addAttribute("filtroEstado", estadoOperativo);
        return VISTA_RESUMEN;
    }
}
