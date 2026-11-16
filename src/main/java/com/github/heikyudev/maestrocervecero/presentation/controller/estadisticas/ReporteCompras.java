package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteCompras;
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
 * Pestaña "Compras" de las estadísticas de compras. Criterios propios: el proveedor (selector con buscador) y el estado de la orden (lista simple).
 */
@Component
@RequiredArgsConstructor
public class ReporteCompras implements ReporteDePestana {

    private final IEstadisticaComprasServicio estadisticaComprasServicio;
    private final IProveedorServicio proveedorServicio;

    @Override
    public PestanaEstadistica pestana() {
        return PestanaEstadistica.COMPRAS;
    }

    @Override
    public Solicitud preparar(LocalDate desde, LocalDate hasta, Map<String, String> parametros) {
        List<String> avisos = new ArrayList<>();
        CamposDeReporte.Elegido<ProveedorResponseDTO> proveedor = CamposDeReporte.proveedor(parametros, proveedorServicio);
        CamposDeReporte.Elegido<EstadoSolicitud> estado = CamposDeReporte.estado(EstadoSolicitud.class, parametros, EstadoSolicitud::getEtiqueta, avisos);
        CriteriosReporteCompras criterios = new CriteriosReporteCompras(desde, hasta, proveedor.id(ProveedorResponseDTO::getId), proveedor.texto(), estado.valor());
        return new Solicitud(List.of(proveedor.campo(), estado.campo()), avisos, pageable -> estadisticaComprasServicio.generarReporteDeCompras(criterios, pageable));
    }
}
