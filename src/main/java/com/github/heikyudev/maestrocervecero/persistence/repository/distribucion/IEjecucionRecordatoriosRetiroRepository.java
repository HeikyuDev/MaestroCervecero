package com.github.heikyudev.maestrocervecero.persistence.repository.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.EjecucionRecordatoriosRetiroEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Repositorio de los días en los que ya se completó el envío diario de los recordatorios de retiro de barriles
 * ({@link EjecucionRecordatoriosRetiroEntity}).
 */
@Repository
public interface IEjecucionRecordatoriosRetiroRepository extends JpaRepository<EjecucionRecordatoriosRetiroEntity, LocalDate> {

    /**
     * Registra el día como enviado si todavía no lo está, en una sola sentencia ({@code ON CONFLICT DO NOTHING}): dos
     * procesos que lo registran a la vez no fallan por la clave repetida.
     *
     * @param fecha El día.
     * @param fechaFin Momento en que terminó el envío de ese día.
     * @return 1 si se registró el día; 0 si ya estaba registrado.
     */
    @Modifying
    @Query(value = "insert into ejecucion_recordatorios_retiro (fecha, fecha_fin) values (:fecha, :fechaFin) on conflict (fecha) do nothing",
            nativeQuery = true)
    int registrarSiNoExiste(@Param("fecha") LocalDate fecha, @Param("fechaFin") LocalDateTime fechaFin);
}
