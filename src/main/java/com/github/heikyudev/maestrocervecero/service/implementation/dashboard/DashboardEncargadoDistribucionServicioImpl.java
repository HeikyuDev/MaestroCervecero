package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ISolicitudBusquedaRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IControlBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.dashboard.IDashboardEncargadoDistribucionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BloqueDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardEncargadoDistribucionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.dashboard.MapperDashboardEncargadoDistribucion;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Implementación de {@link IDashboardEncargadoDistribucionServicio}.
 * <p>
 * Es de solo lectura y corre en una única transacción de lectura. Los despachos vigentes y los retiros pendientes salen de
 * las consultas del panel (con el barril y el cliente cargados en la misma consulta) y los barriles Despachados y Con cerveza salen del servicio de
 * control de barriles, que ya devuelve las filas y el total en una sola llamada.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class DashboardEncargadoDistribucionServicioImpl implements IDashboardEncargadoDistribucionServicio {

    private static final Pageable TOPE_DE_FILAS = PageRequest.of(0, LimitesTecnicos.PANEL_ENCARGADO_DISTRIBUCION_FILAS_POR_BLOQUE);

    private final IDespachoBarrilRepository despachoBarrilRepository;
    private final IControlBarrilServicio controlBarrilServicio;
    private final ISolicitudBusquedaRepository solicitudBusquedaRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardEncargadoDistribucionResponseDTO obtenerDashboardEncargadoDistribucion() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();

        return DashboardEncargadoDistribucionResponseDTO.builder()
                .barrilesEnPoderDeClientes(controlBarrilServicio.filtrarBarrilesParaControl(null, null, EstadoOperativoBarril.DESPACHADO, LimitesTecnicos.PANEL_SOLO_EL_TOTAL).getTotalElements())
                .devolucionesVencidas(despachoBarrilRepository.contarVigentesConDevolucionAnteriorA(hoy))
                .devolucionesQueVencenHoy(despachoBarrilRepository.contarVigentesConDevolucionEn(hoy))
                .devolucionesPendientes(BloqueDashboardResponseDTO.de(despachoBarrilRepository.buscarVigentesConDevolucionHastaParaPanel(hoy, TOPE_DE_FILAS),
                        despacho -> MapperDashboardEncargadoDistribucion.toDevolucionPendienteDTO(despacho, hoy)))
                .retirosVencidos(solicitudBusquedaRepository.contarPendientesVencidas(ahora))
                .retirosPendientes(BloqueDashboardResponseDTO.de(solicitudBusquedaRepository.buscarPendientesParaPanel(TOPE_DE_FILAS),
                        solicitud -> MapperDashboardEncargadoDistribucion.toRetiroPendienteDTO(solicitud, ahora)))
                .clientesConBarriles(BloqueDashboardResponseDTO.de(despachoBarrilRepository.buscarClientesConBarrilesEnPoderParaPanel(TOPE_DE_FILAS),
                        MapperDashboardEncargadoDistribucion::toClienteConBarrilesDTO))
                .barrilesListos(BloqueDashboardResponseDTO.de(controlBarrilServicio.filtrarBarrilesParaControl(null, null, EstadoOperativoBarril.CON_CERVEZA, TOPE_DE_FILAS),
                        MapperDashboardEncargadoDistribucion::toBarrilListoDTO))
                .build();
    }
}
