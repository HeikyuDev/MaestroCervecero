package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteLotes;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pestaña "Lotes" de las estadísticas de producción. Criterios propios: la receta (selector con el buscador de recetas
 * de los filtros de lotes, activas o dadas de baja con planificaciones) y el estado del lote (lista simple).
 */
@Component
@RequiredArgsConstructor
public class ReporteLotes implements ReporteDePestana {

    private final IEstadisticaProduccionServicio estadisticaProduccionServicio;
    private final IRecetaServicio recetaServicio;

    @Override
    public PestanaEstadistica pestana() {
        return PestanaEstadistica.LOTES;
    }

    @Override
    public Solicitud preparar(LocalDate desde, LocalDate hasta, Map<String, String> parametros) {
        List<String> avisos = new ArrayList<>();
        CamposDeReporte.Elegido<RecetaResponseDTO> receta = CamposDeReporte.receta(parametros, recetaServicio);
        CamposDeReporte.Elegido<EstadoLote> estado = CamposDeReporte.estado(EstadoLote.class, parametros, EstadoLote::getEtiqueta, avisos);
        CriteriosReporteLotes criterios = new CriteriosReporteLotes(desde, hasta, receta.id(RecetaResponseDTO::getId), receta.texto(), estado.valor());
        return new Solicitud(List.of(receta.campo(), estado.campo()), avisos, pageable -> estadisticaProduccionServicio.generarReporteDeLotes(criterios, pageable));
    }
}
