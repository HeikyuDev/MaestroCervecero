package com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento;

import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller del detalle de un equipamiento, de solo lectura: su información (datos, parámetros propios de su tipo, usos
 * y últimas fechas). Lo ven el gerente de producción y el operario de producción, que son quienes tienen el permiso de
 * consulta de equipamiento; desde la lista de lotes y las pantallas de lote se llega con el identificador del equipo.
 */
@Controller
@RequestMapping("/equipamiento/{id}/detalle")
@RequiredArgsConstructor
public class EquipamientoDetalleController {

    private static final String VISTA_DETALLE = "equipamiento/equipamiento-detalle";

    private final IControlEquipamientoServicio controlEquipamientoServicio;

    /**
     * Muestra el detalle del equipamiento. Si no existe o está dado de baja, el {@code ControllerAdvices} vuelve al
     * origen con el motivo.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('EQUIPAMIENTO_CONSULTAR')")
    public String mostrarDetalle(@PathVariable Long id, Model model) {
        model.addAttribute("equipo", controlEquipamientoServicio.buscarResumen(id));
        return VISTA_DETALLE;
    }
}
