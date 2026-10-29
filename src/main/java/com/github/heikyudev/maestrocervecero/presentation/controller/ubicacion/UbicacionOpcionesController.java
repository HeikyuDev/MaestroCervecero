package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.ILocalidadServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IProvinciaServicio;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Opciones de los selectores en cascada País, Provincia y Localidad de los formularios. Devuelve JSON con las
 * provincias activas de un país y las localidades activas de una provincia, en orden alfabético. Lo usan el
 * ADMINISTRADOR y los gerentes comercial y de compras, los mismos roles que eligen una localidad en sus formularios
 * (el resto del módulo de ubicaciones es solo del ADMINISTRADOR). Los países los carga cada Controller con la pantalla.
 */
@Controller
@RequestMapping("/ubicaciones/opciones")
@RequiredArgsConstructor
public class UbicacionOpcionesController {

    private final IProvinciaServicio provinciaServicio;
    private final ILocalidadServicio localidadServicio;

    /**
     * Una opción de un desplegable: su id y el nombre que se muestra.
     */
    public record Opcion(Long id, String nombre) {
    }

    /**
     * Provincias activas del país, en orden alfabético.
     */
    @GetMapping("/provincias")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_COMERCIAL', 'GERENTE_DE_COMPRAS')")
    public List<Opcion> provincias(@RequestParam Long idPais) {
        return provinciaServicio.listarProvinciasActivasDePais(idPais).stream()
                .map(p -> new Opcion(p.getId(), p.getNombre())).toList();
    }

    /**
     * Localidades activas de la provincia, en orden alfabético.
     */
    @GetMapping("/localidades")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'GERENTE_COMERCIAL', 'GERENTE_DE_COMPRAS')")
    public List<Opcion> localidades(@RequestParam Long idProvincia) {
        return localidadServicio.listarLocalidadesActivasDeProvincia(idProvincia).stream()
                .map(l -> new Opcion(l.getId(), l.getNombre())).toList();
    }
}
