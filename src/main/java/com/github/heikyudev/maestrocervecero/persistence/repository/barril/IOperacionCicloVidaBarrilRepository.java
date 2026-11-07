package com.github.heikyudev.maestrocervecero.persistence.repository.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.OperacionCicloVidaBarril;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA polimórfico, de solo lectura, de las operaciones del ciclo de vida de un barril
 * ({@link OperacionCicloVidaBarril}): consulta a la vez limpiezas, fallas, mantenimientos, fraccionamientos,
 * despachos y devoluciones con una única consulta, sin recorrer una tabla por tipo de operación.
 * <p>
 * Todas las consultas con "registrada" consideran solamente las operaciones en estado {@code REGISTRADO}: una
 * operación anulada no cuenta como "última" ni como operación vigente.
 * </p>
 */
@Repository
public interface IOperacionCicloVidaBarrilRepository extends JpaRepository<OperacionCicloVidaBarril, Long> {

    /**
     * Busca la fecha de la última operación registrada (de cualquiera de los seis tipos) de un barril.
     * <p>
     * Es la referencia con la que se valida que una operación nueva ocurra después de la última.
     * </p>
     *
     * @param idBarril El ID del barril.
     * @return La fecha más reciente entre sus operaciones registradas, o vacío si no tiene ninguna.
     */
    @Query("SELECT MAX(o.fecha) FROM OperacionCicloVidaBarril o WHERE o.barril.id = :idBarril AND o.estado = 'REGISTRADO'")
    Optional<LocalDateTime> buscarFechaUltimaOperacionRegistrada(@Param("idBarril") Long idBarril);

    /**
     * Verifica si un barril tiene alguna operación registrada (de cualquiera de los seis tipos). Las anuladas no cuentan.
     *
     * @param idBarril El ID del barril.
     * @return {@code true} si tiene al menos una operación en estado {@code REGISTRADO}.
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM OperacionCicloVidaBarril o "
            + "WHERE o.barril.id = :idBarril AND o.estado = 'REGISTRADO'")
    boolean existsOperacionRegistrada(@Param("idBarril") Long idBarril);

    /**
     * Resuelve, sin cargar la operación, el ID del barril al que pertenece. Permite bloquear el barril antes de
     * leer la operación.
     *
     * @param idOperacion El ID de la operación.
     * @return El ID del barril, o vacío si la operación no existe.
     */
    @Query("SELECT o.barril.id FROM OperacionCicloVidaBarril o WHERE o.id = :idOperacion")
    Optional<Long> buscarIdBarrilDeLaOperacion(@Param("idOperacion") Long idOperacion);

    /**
     * Busca los IDs de las operaciones registradas de un barril, de la última a la más antigua. La "última" es la de
     * mayor fecha y, ante igual fecha, la de mayor ID: es la única regla de desempate usada para anular.
     *
     * @param idBarril El ID del barril.
     * @param pageable Paginación; se pide la primera página de tamaño 1 para obtener solo la última.
     * @return Los IDs de las operaciones registradas, ordenados de la última a la más antigua.
     */
    @Query("SELECT o.id FROM OperacionCicloVidaBarril o WHERE o.barril.id = :idBarril AND o.estado = 'REGISTRADO' "
            + "ORDER BY o.fecha DESC, o.id DESC")
    List<Long> buscarIdsOperacionesRegistradasDeLaUltimaALaPrimera(@Param("idBarril") Long idBarril, Pageable pageable);

    /**
     * Busca el ID de la última operación registrada de un barril: la de mayor fecha y, ante igual fecha, la de mayor ID.
     *
     * @param idBarril El ID del barril.
     * @return El ID de la última operación registrada, o vacío si no tiene ninguna.
     */
    default Optional<Long> buscarIdUltimaOperacionRegistrada(Long idBarril) {
        return buscarIdsOperacionesRegistradasDeLaUltimaALaPrimera(idBarril, PageRequest.of(0, 1)).stream().findFirst();
    }
}
