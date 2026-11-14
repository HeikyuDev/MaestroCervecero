package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FallaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.LimpiezaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.OperacionCicloVidaBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FallaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.LimpiezaEquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OperacionCicloVidaEquipamiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.BarrilPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.CausaMantenimiento;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EquipoPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteDelDiaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;

/**
 * Mapea los DTO del listado de lotes y de las pantallas de control, y las últimas operaciones del ciclo de vida, a los
 * DTO de respuesta del panel del operario. No ejecuta consultas: los valores que requieren un service (atraso, falta
 * de consumo) o una consulta (la última operación) llegan ya resueltos.
 */
public class MapperDashboardOperario {

    private MapperDashboardOperario() {
    }

    /**
     * Mapea un lote del listado a {@link LoteDelDiaResponseDTO}.
     *
     * @param lote Lote del listado a convertir.
     * @param diasDeAtraso Días de atraso ya calculados (0 si el lote no está En ejecución o no pasó su fecha estimada).
     * @param faltaConsumo {@code true} si la etapa en curso no alcanza el consumo mínimo (siempre {@code false} en un lote Pendiente).
     * @return El DTO, o {@code null} si el lote es nulo.
     */
    public static LoteDelDiaResponseDTO toLoteDelDiaDTO(LoteCronogramaResponseDTO lote, int diasDeAtraso, boolean faltaConsumo) {
        if (lote == null) {
            return null;
        }
        return LoteDelDiaResponseDTO.builder()
                .id(lote.getId())
                .identificadorInterno(lote.getIdentificadorInterno())
                .nombreReceta(lote.getNombreReceta())
                .etapaActual(lote.getEtapaActual())
                .idFermentador(lote.getIdFermentador())
                .identificadorInternoFermentador(lote.getIdentificadorInternoFermentador())
                .fechaInicioEstimada(lote.getFechaInicioEstimada())
                .fechaFinalizacionEstimada(lote.getFechaFinalizacionEstimada())
                .diasDeAtraso(diasDeAtraso)
                .faltaConsumo(faltaConsumo)
                .build();
    }

    /**
     * Mapea un equipo de la pantalla de control a {@link EquipoPendienteResponseDTO}.
     *
     * @param equipo Equipo a convertir.
     * @param causaMantenimiento Causa del mantenimiento, o {@code null} en los bloques de limpieza o si no se puede derivar.
     * @return El DTO, o {@code null} si el equipo es nulo.
     */
    public static EquipoPendienteResponseDTO toEquipoPendienteDTO(EquipamientoControlResponseDTO equipo, CausaMantenimiento causaMantenimiento) {
        if (equipo == null) {
            return null;
        }
        return EquipoPendienteResponseDTO.builder()
                .id(equipo.getId())
                .identificadorInterno(equipo.getIdentificadorInterno())
                .tipo(equipo.getTipo())
                .causaMantenimiento(causaMantenimiento)
                .build();
    }

    /**
     * Mapea un barril de la pantalla de control a {@link BarrilPendienteResponseDTO}.
     *
     * @param barril Barril a convertir.
     * @param causaMantenimiento Causa del mantenimiento, o {@code null} en los bloques de limpieza o si no se puede derivar.
     * @return El DTO, o {@code null} si el barril es nulo.
     */
    public static BarrilPendienteResponseDTO toBarrilPendienteDTO(BarrilControlResponseDTO barril, CausaMantenimiento causaMantenimiento) {
        if (barril == null) {
            return null;
        }
        return BarrilPendienteResponseDTO.builder()
                .id(barril.getId())
                .identificadorInterno(barril.getIdentificadorInterno())
                .fabricante(barril.getFabricante())
                .causaMantenimiento(causaMantenimiento)
                .build();
    }

    /**
     * Deriva la causa del mantenimiento de un equipo a partir de su última operación registrada: una falla da
     * {@link CausaMantenimiento#FALLA}; una limpieza que dejó al equipo En mantenimiento da
     * {@link CausaMantenimiento#PREVENTIVO}; cualquier otra operación, o ninguna, da {@code null}.
     *
     * @param ultimaOperacion Última operación registrada del equipo, o {@code null} si no tiene.
     * @return La causa, o {@code null} si no se puede derivar.
     */
    public static CausaMantenimiento toCausaMantenimientoDeEquipo(OperacionCicloVidaEquipamiento ultimaOperacion) {
        return derivarCausa(ultimaOperacion instanceof FallaEquipamientoEntity,
                ultimaOperacion instanceof LimpiezaEquipamientoEntity limpieza
                        && limpieza.getEstadoOperativoResultante() == EstadoOperativo.EN_MANTENIMIENTO);
    }

    /**
     * Deriva la causa del mantenimiento de un barril a partir de su última operación registrada, con la misma regla
     * que {@link #toCausaMantenimientoDeEquipo}.
     *
     * @param ultimaOperacion Última operación registrada del barril, o {@code null} si no tiene.
     * @return La causa, o {@code null} si no se puede derivar.
     */
    public static CausaMantenimiento toCausaMantenimientoDeBarril(OperacionCicloVidaBarril ultimaOperacion) {
        return derivarCausa(ultimaOperacion instanceof FallaBarrilEntity,
                ultimaOperacion instanceof LimpiezaBarrilEntity limpieza
                        && limpieza.getEstadoOperativoResultante() == EstadoOperativoBarril.EN_MANTENIMIENTO);
    }

    /**
     * Regla común de la causa del mantenimiento: una falla da {@link CausaMantenimiento#FALLA}, una limpieza que dejó el
     * activo En mantenimiento da {@link CausaMantenimiento#PREVENTIVO} y cualquier otra operación da {@code null}.
     */
    private static CausaMantenimiento derivarCausa(boolean esFalla, boolean esLimpiezaQueDejoEnMantenimiento) {
        if (esFalla) {
            return CausaMantenimiento.FALLA;
        }
        return esLimpiezaQueDejoEnMantenimiento ? CausaMantenimiento.PREVENTIVO : null;
    }
}
