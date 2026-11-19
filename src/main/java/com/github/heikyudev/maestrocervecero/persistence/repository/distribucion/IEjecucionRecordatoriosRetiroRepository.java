package com.github.heikyudev.maestrocervecero.persistence.repository.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.EjecucionRecordatoriosRetiroEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * Repositorio de los días en los que ya se completó el envío diario de los recordatorios de retiro de barriles
 * ({@link EjecucionRecordatoriosRetiroEntity}).
 */
@Repository
public interface IEjecucionRecordatoriosRetiroRepository extends JpaRepository<EjecucionRecordatoriosRetiroEntity, LocalDate> {
}
