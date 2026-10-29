package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interfaz que define los servicios de consulta relacionados con las reservas de insumo
 * generadas al iniciar un lote.
 */
public interface IReservaInsumoServicio {

    /**
     * Busca las reservas de insumo de una etapa de lote para un insumo determinado, para
     * mostrarle al operario qué lotes de insumo físico tiene reservados y con qué cantidad,
     * antes de registrar un consumo.
     * <p>
     * Devuelve TODAS las reservas de ese insumo en esa etapa, incluidas las que ya no tienen saldo
     * (consumidas o liberadas): mostrar solo las que todavía tienen saldo, u ordenarlas primero, es una
     * decisión de presentación de la vista, no un filtro de este método.
     * </p>
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio; no lo tipea el usuario, lo
     *                    determina el contexto de la pantalla, igual que en
     *                    {@code filtrarInsumosRequeridos}).
     * @param idInsumo El ID del insumo, elegido por el usuario entre los devueltos por
     *                 {@code filtrarInsumosRequeridos}.
     * @return Las reservas correspondientes, en formato DTO.
     */
    List<ReservaInsumoResponseDTO> filtrarLotesInsumoReservados(Long idEtapaLote, Long idInsumo);

    /**
     * Filtra las reservas de una etapa de lote que todavía tienen saldo reservado, para elegir el lote de insumo de un
     * consumo reservado, en orden FEFO (vigentes por vencimiento, luego sin fecha de vencimiento y al final los
     * vencidos, que se marcan). Las reservas agotadas no se ofrecen.
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio; lo resuelve el Controller).
     * @param idInsumo El ID del insumo elegido, o {@code null} para no restringir.
     * @param identificacion Texto a buscar dentro de la identificación del lote de insumo, o {@code null}/vacío para no restringir.
     * @param pageable La configuración de paginación; el orden lo define el service.
     * @return Una página de reservas en formato DTO.
     */
    Page<ReservaInsumoResponseDTO> filtrarReservasConSaldo(Long idEtapaLote, Long idInsumo, String identificacion, Pageable pageable);

    /**
     * Lista todas las reservas de una etapa de lote con su historial completo (cantidad reservada original, consumida,
     * liberada, reducida por ajuste, pendiente y estado), también las consumidas y las liberadas y las de etapas ya
     * cerradas o lotes finalizados o cancelados. Es de solo lectura y no filtra por estado: ordenar las activas primero es
     * decisión de la vista.
     *
     * @param idEtapaLote El ID de la etapa de lote.
     * @return Las reservas de la etapa en formato DTO, por insumo alfabético y luego por vencimiento del lote de insumo.
     */
    List<ReservaInsumoResponseDTO> listarReservasDeEtapa(Long idEtapaLote);
}
