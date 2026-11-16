package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteTiempos;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Pestaña "Tiempos" de las estadísticas de producción. Criterios propios: la etapa (lista simple) y la receta (selector con buscador).
 */
@Component
@RequiredArgsConstructor
public class ReporteTiempos implements ReporteDePestana {

    private final IEstadisticaProduccionServicio estadisticaProduccionServicio;
    private final IRecetaServicio recetaServicio;

    @Override
    public PestanaEstadistica pestana() {
        return PestanaEstadistica.TIEMPOS;
    }

    @Override
    public Solicitud preparar(LocalDate desde, LocalDate hasta, Map<String, String> parametros) {
        List<String> avisos = new ArrayList<>();
        CamposDeReporte.Elegido<TipoEtapa> etapa = CamposDeReporte.etapa(parametros, avisos);
        CamposDeReporte.Elegido<RecetaResponseDTO> receta = CamposDeReporte.receta(parametros, recetaServicio);
        CriteriosReporteTiempos criterios = new CriteriosReporteTiempos(desde, hasta, etapa.valor(), receta.id(RecetaResponseDTO::getId), receta.texto());
        return new Solicitud(List.of(etapa.campo(), receta.campo()), avisos, pageable -> estadisticaProduccionServicio.generarReporteDeTiempos(criterios, pageable));
    }
}
