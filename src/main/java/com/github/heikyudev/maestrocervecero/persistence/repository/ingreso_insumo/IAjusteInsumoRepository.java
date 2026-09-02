package com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.AjusteInsumoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los ajustes de insumo ({@link AjusteInsumoEntity}).
 * <p>
 * No filtra por estado en {@code findAll}/{@code findById}: a diferencia de la baja lógica del
 * resto de los módulos, un ajuste anulado sigue siendo un registro histórico consultable, no un
 * registro "eliminado".
 * </p>
 */
@Repository
public interface IAjusteInsumoRepository extends JpaRepository<AjusteInsumoEntity, Long> {
}
