package com.github.heikyudev.maestrocervecero.persistence.repository.costo_adicional;

import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.DetalleCostoDirectoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA de los costos directos adicionales aplicados a un lote ({@link DetalleCostoDirectoEntity}).
 */
@Repository
public interface IDetalleCostoDirectoRepository extends JpaRepository<DetalleCostoDirectoEntity, Long> {

    /**
     * Lista, en una sola consulta, los costos directos adicionales aplicados a un lote con su costo directo adicional cargado
     * (también si después se dio de baja), por nombre sin distinguir mayúsculas.
     *
     * @param idLote El ID del lote.
     * @return Los detalles de costo directo del lote.
     */
    @Query("SELECT d FROM DetalleCostoDirectoEntity d JOIN FETCH d.costoDirectoAdicional c WHERE d.lote.id = :idLote ORDER BY UPPER(c.nombre), d.id")
    List<DetalleCostoDirectoEntity> listarDelLote(@Param("idLote") Long idLote);
}
