package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.presentation.view.estadisticas.CampoFiltro;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteEquipamiento;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.aId;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.resolverUno;

/**
 * Pestaña "Equipamiento" de las estadísticas de producción, para los cuatro tipos de equipo. Criterios propios: el tipo de equipo (lista simple) y el equipo (selector con su buscador en modal).
 */
@Component
@RequiredArgsConstructor
public class ReporteEquipamiento implements ReporteDePestana {

    private final IEstadisticaProduccionServicio estadisticaProduccionServicio;
    private final IControlEquipamientoServicio controlEquipamientoServicio;

    @Override
    public PestanaEstadistica pestana() {
        return PestanaEstadistica.EQUIPAMIENTO;
    }

    @Override
    public Solicitud preparar(LocalDate desde, LocalDate hasta, Map<String, String> parametros) {
        List<String> avisos = new ArrayList<>();
        CamposDeReporte.Elegido<TipoEquipamiento> tipo = CamposDeReporte.tipoDeEquipo(parametros, avisos);
        EquipamientoResumenResponseDTO equipo = resolverUno(aId(parametros.get(CamposDeReporte.PARAMETRO_EQUIPO)), controlEquipamientoServicio::buscarResumen);
        String texto = equipo == null ? null : CamposDeReporte.textoDeEquipo(equipo.getIdentificadorInterno(), equipo.isDadoDeBaja());
        CampoFiltro campoEquipo = CampoFiltro.buscador(CamposDeReporte.PARAMETRO_EQUIPO, "Equipo", equipo == null ? null : String.valueOf(equipo.getId()), texto,
                CamposDeReporte.URL_BUSCADOR_EQUIPOS, "Seleccione equipo");
        CriteriosReporteEquipamiento criterios = new CriteriosReporteEquipamiento(desde, hasta, tipo.valor(), equipo == null ? null : equipo.getId(), texto);
        return new Solicitud(List.of(tipo.campo(), campoEquipo), avisos, pageable -> estadisticaProduccionServicio.generarReporteDeEquipamiento(criterios, pageable));
    }
}
