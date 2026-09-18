package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EnvasadoLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repositorio JPA de los envasados de lote ({@link EnvasadoLoteEntity}).
 */
@Repository
public interface IEnvasadoLoteRepository extends JpaRepository<EnvasadoLoteEntity, Long> {

    /**
     * Filtra los envasados de lote de una etapa de lote puntual, opcionalmente por barril
     * utilizado y por estado transaccional.
     *
     * @param idEtapaLote El ID de la etapa de lote (obligatorio).
     * @param idBarril El ID del barril utilizado a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de envasados de lote que cumplen los criterios indicados.
     */
    @Query(value = "SELECT e FROM EnvasadoLoteEntity e WHERE e.etapaLote.id = :idEtapaLote "
            + "AND (:idBarril IS NULL OR e.barril.id = :idBarril) "
            + "AND (:estado IS NULL OR e.estado = :estado)",
            countQuery = "SELECT COUNT(e) FROM EnvasadoLoteEntity e WHERE e.etapaLote.id = :idEtapaLote "
                    + "AND (:idBarril IS NULL OR e.barril.id = :idBarril) "
                    + "AND (:estado IS NULL OR e.estado = :estado)")
    Page<EnvasadoLoteEntity> filtrarEnvasadosLote(@Param("idEtapaLote") Long idEtapaLote,
                                                   @Param("idBarril") Long idBarril,
                                                   @Param("estado") EstadoTransaccion estado,
                                                   Pageable pageable);
}
