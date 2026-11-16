package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReportePrecios;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaComprasServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Pestaña "Precios" de las estadísticas de compras. Criterios propios: el tipo de insumo (lista simple) y el proveedor (selector con buscador).
 */
@Component
@RequiredArgsConstructor
public class ReportePrecios implements ReporteDePestana {

    private final IEstadisticaComprasServicio estadisticaComprasServicio;
    private final IProveedorServicio proveedorServicio;

    @Override
    public PestanaEstadistica pestana() {
        return PestanaEstadistica.PRECIOS;
    }

    @Override
    public Solicitud preparar(LocalDate desde, LocalDate hasta, Map<String, String> parametros) {
        List<String> avisos = new ArrayList<>();
        CamposDeReporte.Elegido<TipoInsumo> tipo = CamposDeReporte.tipoDeInsumo(parametros, avisos);
        CamposDeReporte.Elegido<ProveedorResponseDTO> proveedor = CamposDeReporte.proveedor(parametros, proveedorServicio);
        CriteriosReportePrecios criterios = new CriteriosReportePrecios(desde, hasta, tipo.valor(), proveedor.id(ProveedorResponseDTO::getId), proveedor.texto());
        return new Solicitud(List.of(tipo.campo(), proveedor.campo()), avisos, pageable -> estadisticaComprasServicio.generarReporteDePrecios(criterios, pageable));
    }
}
