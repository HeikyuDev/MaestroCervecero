package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteCostos;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Pestaña "Costos" de las estadísticas de producción. Criterio propio: la receta (selector con el buscador de recetas de los filtros de lotes).
 */
@Component
@RequiredArgsConstructor
public class ReporteCostos implements ReporteDePestana {

    private final IEstadisticaProduccionServicio estadisticaProduccionServicio;
    private final IRecetaServicio recetaServicio;

    @Override
    public PestanaEstadistica pestana() {
        return PestanaEstadistica.COSTOS;
    }

    @Override
    public Solicitud preparar(LocalDate desde, LocalDate hasta, Map<String, String> parametros) {
        CamposDeReporte.Elegido<RecetaResponseDTO> receta = CamposDeReporte.receta(parametros, recetaServicio);
        CriteriosReporteCostos criterios = new CriteriosReporteCostos(desde, hasta, receta.id(RecetaResponseDTO::getId), receta.texto());
        return new Solicitud(List.of(receta.campo()), List.of(), pageable -> estadisticaProduccionServicio.generarReporteDeCostos(criterios, pageable));
    }
}
