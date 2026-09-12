package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ConsumoInsumoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los consumos de insumo ({@link ConsumoInsumoEntity}).
 */
@Repository
public interface IConsumoInsumoRepository extends JpaRepository<ConsumoInsumoEntity, Long> {
}
