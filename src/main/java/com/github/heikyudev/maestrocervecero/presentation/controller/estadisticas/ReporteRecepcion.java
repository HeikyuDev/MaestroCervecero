package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.CriteriosReporteRecepcion;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaComprasServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Pestaña "Recepción" de las estadísticas de compras. Criterios propios: el proveedor (selector con buscador) y el tipo de ingreso (lista simple).
 * <p>
 * Solo se registra con {@code estadisticas.recepcion.habilitada=true} (en {@code application.properties} está en {@code false}): sin
 * este reporte la pantalla no muestra la pestaña y su dirección, incluido el PDF, cae en la primera pestaña del usuario.
 */
@Component
@ConditionalOnProperty(name = "estadisticas.recepcion.habilitada", havingValue = "true")
@RequiredArgsConstructor
public class ReporteRecepcion implements ReporteDePestana {

    private final IEstadisticaComprasServicio estadisticaComprasServicio;
    private final IProveedorServicio proveedorServicio;

    @Override
    public PestanaEstadistica pestana() {
        return PestanaEstadistica.RECEPCION;
    }

    @Override
    public Solicitud preparar(LocalDate desde, LocalDate hasta, Map<String, String> parametros) {
        List<String> avisos = new ArrayList<>();
        CamposDeReporte.Elegido<ProveedorResponseDTO> proveedor = CamposDeReporte.proveedor(parametros, proveedorServicio);
        CamposDeReporte.Elegido<TipoIngreso> tipo = CamposDeReporte.lista(TipoIngreso.class, "tipoIngreso", "Tipo de ingreso", "Todos",
                "El tipo de ingreso indicado no es válido, se muestran todos", parametros, t -> t == TipoIngreso.COMPRA ? "Compra" : "Directo", avisos);
        CriteriosReporteRecepcion criterios = new CriteriosReporteRecepcion(desde, hasta, proveedor.id(ProveedorResponseDTO::getId), proveedor.texto(), tipo.valor());
        return new Solicitud(List.of(proveedor.campo(), tipo.campo()), avisos, pageable -> estadisticaComprasServicio.generarReporteDeRecepcion(criterios, pageable));
    }
}
