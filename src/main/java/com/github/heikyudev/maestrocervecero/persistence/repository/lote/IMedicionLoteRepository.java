package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.MedicionLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repositorio JPA de las mediciones de lote ({@link MedicionLoteEntity}).
 */
@Repository
public interface IMedicionLoteRepository extends JpaRepository<MedicionLoteEntity, Long> {

    /**
     * Indica si ya existe una medición, en el estado indicado, para el mismo detalle de parámetro
     * de control y la misma fecha y hora exactas.
     * <p>
     * Se usa para evitar duplicar una medición del mismo parámetro en el mismo instante. No impide
     * que se registren mediciones de parámetros distintos en ese mismo instante.
     * </p>
     *
     * @param idDetalleParametroControl El ID del detalle de parámetro de control.
     * @param fechaMedicion             La fecha y hora exacta de la medición.
     * @param estado                    El estado de medición a considerar (normalmente REGISTRADO).
     * @return {@code true} si ya existe una medición en esas condiciones.
     */
    boolean existsByDetalleParametroControl_IdAndFechaMedicionAndEstado(Long idDetalleParametroControl, LocalDateTime fechaMedicion, EstadoTransaccion estado);
}
