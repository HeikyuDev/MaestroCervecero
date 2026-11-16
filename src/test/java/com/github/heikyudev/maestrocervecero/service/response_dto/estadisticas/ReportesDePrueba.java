package com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reportes de estadísticas armados para las pruebas de la pantalla y del reporte impreso.
 */
public final class ReportesDePrueba {

    private ReportesDePrueba() {
    }

    /**
     * Un reporte "Lotes" con {@code cantidad} filas en la página de un total de {@code total} lotes en el período, los
     * cuatro indicadores, el gráfico por estado y la fila de totales.
     */
    public static ReporteEstadisticoResponseDTO lotes(int cantidad, long total) {
        return lotesEnPagina(cantidad, PageRequest.of(0, Math.max(1, cantidad)), total);
    }

    /** Igual que {@link #lotes(int, long)} con las filas en la página indicada. */
    public static ReporteEstadisticoResponseDTO lotesEnPagina(int cantidad, Pageable pagina, long total) {
        List<FilaReporteResponseDTO> filas = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            String codigo = String.format("LOT-R7-%02d", i);
            filas.add(new FilaReporteResponseDTO(List.of(
                    CeldaReporteResponseDTO.texto(codigo, "Ñandú Añejo"),
                    CeldaReporteResponseDTO.texto("Finalizado"),
                    CeldaReporteResponseDTO.numero(480.5),
                    CeldaReporteResponseDTO.fecha(LocalDate.of(2026, 11, 20)),
                    CeldaReporteResponseDTO.fecha(LocalDateTime.of(2026, 11, 22, 17, 0)),
                    CeldaReporteResponseDTO.enlace((long) i, "Abrir " + codigo))));
        }
        boolean hayLotes = cantidad > 0;
        TablaReporteResponseDTO tabla = new TablaReporteResponseDTO(List.of(
                ColumnaReporteResponseDTO.de("Lote", TipoColumna.TEXTO),
                ColumnaReporteResponseDTO.de("Estado", TipoColumna.TEXTO),
                ColumnaReporteResponseDTO.de("Litros envasados", TipoColumna.NUMERO),
                ColumnaReporteResponseDTO.de("Fin estimado", TipoColumna.FECHA),
                ColumnaReporteResponseDTO.de("Fin real", TipoColumna.FECHA),
                ColumnaReporteResponseDTO.enlace("Acciones", "/lotes/{id}", "LOTE_CONSULTAR")),
                new PageImpl<>(filas, pagina, total),
                new FilaReporteResponseDTO(List.of(CeldaReporteResponseDTO.texto("Totales del período"), CeldaReporteResponseDTO.vacia(),
                        CeldaReporteResponseDTO.numero(480.5 * cantidad), CeldaReporteResponseDTO.vacia(), CeldaReporteResponseDTO.vacia(),
                        CeldaReporteResponseDTO.vacia())),
                "lote|lotes");
        return ReporteEstadisticoResponseDTO.builder()
                .titulo("Lotes")
                .descripcion("Cuántos lotes se planificaron en el período y cómo terminaron")
                .desde(LocalDate.of(2026, 11, 1))
                .hasta(LocalDate.of(2026, 11, 30))
                .criterios(List.of(CriterioAplicadoResponseDTO.de("Receta", null, "Todas"), CriterioAplicadoResponseDTO.de("Estado", "Finalizado", "Todos")))
                .indicadores(List.of(
                        new IndicadorResponseDTO("lotes", "Lotes del período", String.valueOf(total), "Por fecha de inicio estimada", null),
                        new IndicadorResponseDTO("finalizados", "Finalizados", String.valueOf(total), null, null),
                        new IndicadorResponseDTO("litros-envasados", "Litros envasados", "1.441,5 L", "Envasados registrados", null),
                        new IndicadorResponseDTO("en-plazo", "Terminados en plazo", hayLotes ? "66,67 %" : "-",
                                hayLotes ? "2 de 3 finalizados" : "Sin lotes finalizados",
                                "Porcentaje de los lotes finalizados cuya fecha de fin real no es posterior a la fecha de fin estimada")))
                .grafico(new GraficoReporteResponseDTO("Lotes por estado", List.of(
                        new DatoGraficoResponseDTO("Pendiente", 0), new DatoGraficoResponseDTO("En ejecución", 0),
                        new DatoGraficoResponseDTO("Finalizado", cantidad), new DatoGraficoResponseDTO("Cancelado", 0)), null))
                .tabla(tabla)
                .build();
    }
}
