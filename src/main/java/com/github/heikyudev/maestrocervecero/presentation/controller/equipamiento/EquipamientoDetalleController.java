package com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

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

    private static final Map<TipoEquipamiento, String> RUTA_LISTA = Map.of(
            TipoEquipamiento.MOLINO, "/equipamiento/molinos",
            TipoEquipamiento.MACERADOR, "/equipamiento/maceradores",
            TipoEquipamiento.OLLA_HERVOR, "/equipamiento/ollas-hervor",
            TipoEquipamiento.FERMENTADOR, "/equipamiento/fermentadores");
    private static final Map<TipoEquipamiento, String> TEXTO_LISTA = Map.of(
            TipoEquipamiento.MOLINO, "molinos",
            TipoEquipamiento.MACERADOR, "maceradores",
            TipoEquipamiento.OLLA_HERVOR, "ollas de hervor",
            TipoEquipamiento.FERMENTADOR, "fermentadores");

    private final IControlEquipamientoServicio controlEquipamientoServicio;

    /**
     * Muestra el detalle del equipamiento. Si no existe o está dado de baja, el {@code ControllerAdvices} vuelve al
     * origen con el motivo. El detalle pertenece a la Gestión de Equipamientos: resalta su opción del menú y "Volver"
     * lleva a la pestaña de la lista del tipo del equipo, para el gerente y para el operario de producción (que consulta).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('EQUIPAMIENTO_CONSULTAR')")
    public String mostrarDetalle(@PathVariable Long id, Model model) {
        EquipamientoResumenResponseDTO equipo = controlEquipamientoServicio.buscarResumen(id);
        model.addAttribute("equipo", equipo);
        model.addAttribute("urlVolver", RUTA_LISTA.get(equipo.getTipo()));
        model.addAttribute("textoVolver", "Volver a " + TEXTO_LISTA.get(equipo.getTipo()));
        return VISTA_DETALLE;
    }
}
