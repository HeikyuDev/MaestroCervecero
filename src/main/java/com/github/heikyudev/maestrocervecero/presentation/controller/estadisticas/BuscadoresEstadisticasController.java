package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.presentation.controller.lote.LoteController.FilaEquipo;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.ORDEN_POR_IDENTIFICADOR;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.TAMANIO_PAGINA_BUSCADOR;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.aEstadoOperativo;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.avisoDeEstadoOperativo;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.vacioANulo;

/**
 * Buscadores en modal de los criterios propios de las pestañas de estadísticas. El de equipos (de cualquier tipo) lo usa
 * la pestaña "Equipamiento"; las recetas y los proveedores se eligen con los buscadores de los filtros de lotes y de
 * órdenes de compra, que ya existen.
 */
@Controller
@RequestMapping("/estadisticas/buscador")
@RequiredArgsConstructor
public class BuscadoresEstadisticasController {

    static final String VISTA_BUSCADOR_EQUIPO = "lote/buscador-equipo :: buscador";

    private final IControlEquipamientoServicio controlEquipamientoServicio;

    /**
     * Devuelve el contenido del modal de búsqueda de equipamientos activos de cualquier tipo (criterios de identificador
     * y estado operativo + resultados con "Elegir"), de a {@code TAMANIO_PAGINA_BUSCADOR} y en orden por identificador. Un
     * estado operativo inválido no filtra y se avisa dentro del modal.
     */
    @GetMapping("/equipos")
    @PreAuthorize("hasAuthority('ESTADISTICA_PRODUCCION_CONSULTAR')")
    public String buscarEquipos(@RequestParam(required = false) String identificador,
                                @RequestParam(required = false) String estadoOperativo,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        EstadoOperativo estado = aEstadoOperativo(estadoOperativo);
        Page<FilaEquipo> filas = controlEquipamientoServicio.filtrarEquipamientosParaControl(vacioANulo(identificador), null, estado,
                        PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA_BUSCADOR, ORDEN_POR_IDENTIFICADOR))
                .map(e -> new FilaEquipo(e.getId(), e.getIdentificadorInterno(), e.getEstadoOperativo(), List.of()));

        model.addAttribute("filas", filas);
        model.addAttribute("nombreEquipo", "equipamiento");
        model.addAttribute("nombreEquipoPlural", "equipamientos");
        model.addAttribute("ejemploIdentificador", "FER-01");
        model.addAttribute("urlBuscador", CamposDeReporte.URL_BUSCADOR_EQUIPOS);
        model.addAttribute("columnas", List.of());
        model.addAttribute("estados", EstadoOperativo.values());
        model.addAttribute("filtroIdentificador", identificador);
        model.addAttribute("filtroEstado", estado);
        model.addAttribute("avisoBuscador", avisoDeEstadoOperativo(estadoOperativo, estado));
        return VISTA_BUSCADOR_EQUIPO;
    }
}
