package com.github.heikyudev.maestrocervecero.persistence.repository.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleParametroControlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los detalles de parámetro de control ({@link DetalleParametroControlEntity}).
 */
@Repository
public interface IDetalleParametroControlRepository extends JpaRepository<DetalleParametroControlEntity, Long> {
}
