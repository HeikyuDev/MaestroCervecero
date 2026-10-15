package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Bloquea para escritura un lote y todo lo que lo precede en el orden canónico de bloqueo del módulo de lotes.
 * <p>
 * El orden global es: planificación de producción, fermentador (exclusión mutua de su cola), cola de lotes pendientes
 * del fermentador, lote, equipos del lote (molino, macerador, olla de hervor, fermentador) y lotes de insumo por ID
 * ascendente. Todas las operaciones que bloquean varias filas respetan este orden (se pueden omitir niveles, nunca volver
 * a uno anterior), de modo que dos transacciones concurrentes no se esperen mutuamente.
 * </p>
 * <p>
 * Los IDs se resuelven con consultas escalares, sin cargar entidades: cada fila se lee recién cuando ya está bloqueada,
 * por lo que nunca se opera con datos anteriores al bloqueo. Debe invocarse dentro de una transacción.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class OrdenCanonicoDeBloqueo {

    private final ILoteRepository loteRepository;
    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final IFermentadorRepository fermentadorRepository;

    /**
     * Bloquea un lote y lo que lo precede: su planificación (opcional), su fermentador, la cola de lotes pendientes de ese
     * fermentador y, por último, el lote.
     *
     * @param idLote El ID del lote.
     * @param bloquearPlanificacion {@code true} si la operación también modifica la planificación del lote.
     * @return El lote bloqueado, con su estado más reciente.
     * @throws RecursoNoEncontradoException Si no existe el lote, su planificación o su fermentador.
     */
    public LoteEntity bloquearLote(Long idLote, boolean bloquearPlanificacion) {
        Supplier<RecursoNoEncontradoException> noEncontrado = () -> new RecursoNoEncontradoException("No se encontró el lote con ID: " + idLote);
        Long idFermentador = loteRepository.buscarIdFermentadorDelLote(idLote).orElseThrow(noEncontrado);

        if (bloquearPlanificacion) {
            Long idPlanificacion = loteRepository.buscarIdPlanificacionDelLote(idLote).orElseThrow(noEncontrado);
            planificacionProduccionRepository.buscarPorIdParaActualizar(idPlanificacion)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + idPlanificacion));
        }

        fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(idFermentador)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el fermentador con ID: " + idFermentador));
        loteRepository.buscarPendientesDeFermentadorParaReordenar(idFermentador);

        return loteRepository.buscarPorIdParaActualizar(idLote).orElseThrow(noEncontrado);
    }
}
