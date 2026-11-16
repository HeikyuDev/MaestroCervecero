package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteProveedores;
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
 * Pestaña "Proveedores" de las estadísticas de compras. Criterios propios: el proveedor (selector con buscador) y el tipo de insumo (lista simple).
 */
@Component
@RequiredArgsConstructor
public class ReporteProveedores implements ReporteDePestana {

    private final IEstadisticaComprasServicio estadisticaComprasServicio;
    private final IProveedorServicio proveedorServicio;

    @Override
    public PestanaEstadistica pestana() {
        return PestanaEstadistica.PROVEEDORES;
    }

    @Override
    public Solicitud preparar(LocalDate desde, LocalDate hasta, Map<String, String> parametros) {
        List<String> avisos = new ArrayList<>();
        CamposDeReporte.Elegido<ProveedorResponseDTO> proveedor = CamposDeReporte.proveedor(parametros, proveedorServicio);
        CamposDeReporte.Elegido<TipoInsumo> tipo = CamposDeReporte.tipoDeInsumo(parametros, avisos);
        CriteriosReporteProveedores criterios = new CriteriosReporteProveedores(desde, hasta, proveedor.id(ProveedorResponseDTO::getId), proveedor.texto(), tipo.valor());
        return new Solicitud(List.of(proveedor.campo(), tipo.campo()), avisos, pageable -> estadisticaComprasServicio.generarReporteDeProveedores(criterios, pageable));
    }
}
