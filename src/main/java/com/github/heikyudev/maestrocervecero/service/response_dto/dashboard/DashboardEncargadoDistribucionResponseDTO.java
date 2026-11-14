package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con el panel del encargado de distribución: los barriles en poder de clientes, las devoluciones que
 * vencen hoy o ya vencieron, los clientes con barriles en su poder y los barriles listos para despachar. Los contadores de
 * la pantalla salen de los contadores propios y de los {@code total} de los bloques.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardEncargadoDistribucionResponseDTO {

    /** Cantidad de barriles activos Despachados, es decir, en poder de clientes. */
    private long barrilesEnPoderDeClientes;

    /** Cantidad de barriles despachados cuya fecha de devolución estimada es anterior a hoy. */
    private long devolucionesVencidas;

    /** Cantidad de barriles despachados cuya fecha de devolución estimada es hoy. */
    private long devolucionesQueVencenHoy;

    /**
     * Barriles despachados con devolución estimada para hoy o anterior, el más atrasado primero. Su total es la suma de
     * {@link #devolucionesVencidas} y {@link #devolucionesQueVencenHoy}.
     */
    private BloqueDashboardResponseDTO<DevolucionPendienteResponseDTO> devolucionesPendientes;

    /** Clientes con barriles en su poder, uno por cliente, el de la devolución más próxima primero. */
    private BloqueDashboardResponseDTO<ClienteConBarrilesResponseDTO> clientesConBarriles;

    /** Barriles Con cerveza, listos para despachar, por identificador interno. */
    private BloqueDashboardResponseDTO<BarrilListoResponseDTO> barrilesListos;
}
