package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de las etapas de lote ({@link EtapaLoteEntity}).
 * <p>
 * Hasta ahora, las etapas de un lote solo se accedían indirectamente a través de
 * {@link com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity#getEtapas()}.
 * Este repositorio existe para poder resolver una etapa puntual por su propio ID (por ejemplo, al
 * registrar una medición, donde el formulario indica directamente sobre qué etapa se mide).
 * </p>
 */
@Repository
public interface IEtapaLoteRepository extends JpaRepository<EtapaLoteEntity, Long> {
}
