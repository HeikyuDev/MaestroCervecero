package com.github.heikyudev.maestrocervecero.persistence.repository.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EnvasadoLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

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

    /**
     * Busca la fecha del último envasado registrado de un barril determinado.
     * <p>
     * El envasado no tiene una fecha de negocio propia distinta a cuándo se registró (no admite
     * carga retroactiva), así que se usa {@code createdDate} como su fecha efectiva.
     * </p>
     *
     * @param idBarril El ID del barril cuyo último envasado se quiere buscar.
     * @return La fecha del último envasado registrado del barril, o {@link Optional#empty()} si nunca tuvo uno.
     */
    @Query("SELECT MAX(e.createdDate) FROM EnvasadoLoteEntity e WHERE e.barril.id = :idBarril AND e.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO")
    Optional<LocalDateTime> buscarFechaUltimoEnvasadoRegistrado(@Param("idBarril") Long idBarril);

    /**
     * Suma la cantidad envasada de todos los envasados {@code REGISTRADO} de una etapa de lote
     * puntual (la etapa de Envasado de un lote). Se usa al finalizar el envasado del lote, para
     * calcular sobre cuántos litros efectivamente envasados se aplican los costos directos
     * adicionales vigentes.
     *
     * @param idEtapaLote El ID de la etapa de lote (Envasado) cuyos envasados se quieren sumar.
     * @return La suma de {@code cantidadEnvasada} de sus envasados REGISTRADO, o {@code 0.0} si no tiene ninguno.
     */
    @Query("SELECT COALESCE(SUM(e.cantidadEnvasada), 0.0) FROM EnvasadoLoteEntity e WHERE e.etapaLote.id = :idEtapaLote AND e.estado = com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion.REGISTRADO")
    double sumarCantidadEnvasadaRegistrada(@Param("idEtapaLote") Long idEtapaLote);
}
