package com.github.heikyudev.maestrocervecero.persistence.repository.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repositorio JPA base de los insumos ({@link InsumoEntity}).
 * <p>
 * Permite buscar cualquier insumo (Malta, Lúpulo o Levadura) por su ID sin conocer de
 * antemano su tipo concreto: Hibernate resuelve la subclase real gracias a la estrategia
 * {@code InheritanceType.JOINED} y a la columna discriminadora {@code tipo_insumo}. Se utiliza
 * en módulos que referencian un insumo de forma polimórfica, como el catálogo de proveedores.
 * </p>
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de insumos dados de baja se
 * realiza explícitamente mediante la condición {@code estado = 'ACTIVO'}.
 * </p>
 */
@Repository
public interface IInsumoRepository extends JpaRepository<InsumoEntity, Long> {

    /**
     * Busca un insumo activo por su ID, cualquiera sea su tipo concreto.
     *
     * @param id El ID del insumo a buscar.
     * @return Un Optional que contiene el insumo si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT i FROM InsumoEntity i WHERE i.id = :id AND i.estado = 'ACTIVO'")
    Optional<InsumoEntity> findById(@Param("id") Long id);

    /**
     * Busca un insumo por su ID y bloquea su fila hasta el fin de la transacción ({@code PESSIMISTIC_WRITE}).
     * <p>
     * Se usa al registrar un ingreso de insumo: serializa los ingresos del mismo insumo, de modo que dos ingresos
     * simultáneos de la misma identificación de lote de proveedor no decidan ambos que el lote no existe y lo creen
     * a la vez.
     * </p>
     *
     * @param id El ID del insumo a buscar.
     * @return Un Optional que contiene el insumo, o vacío si no existe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InsumoEntity i WHERE i.id = :id")
    Optional<InsumoEntity> buscarPorIdParaIngresar(@Param("id") Long id);

    /**
     * Busca, entre TODOS los tipos concretos de insumo a la vez, una página de insumos activos
     * filtrados opcionalmente por nombre (coincidencia parcial, sin distinguir
     * mayúsculas/minúsculas) y/o tipo concreto (Malta/Lúpulo/Levadura). Un parámetro nulo no
     * restringe por ese criterio.
     * <p>
     * Se usa en contextos donde no importa el tipo concreto del insumo (por ejemplo, el catálogo
     * de un proveedor). Para listar únicamente los insumos de un tipo determinado con todos sus
     * datos técnicos propios, usar el método {@code filtrarX} del repositorio específico de ese
     * tipo en su lugar.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre del insumo, o {@code null} para no filtrar por él.
     * @param tipoClase Clase concreta de insumo a filtrar (ver {@link com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo#getEntityClass()}), o {@code null} para no filtrar por tipo.
     * @param pageable La configuración de paginación.
     * @return Una página de insumos activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT i FROM InsumoEntity i WHERE i.estado = 'ACTIVO' "
            + "AND (:nombre IS NULL OR UPPER(i.nombre) LIKE UPPER(CONCAT('%', CAST(:nombre AS string), '%'))) "
            + "AND (:tipoClase IS NULL OR TYPE(i) = :tipoClase)",
            countQuery = "SELECT COUNT(i) FROM InsumoEntity i WHERE i.estado = 'ACTIVO' "
                    + "AND (:nombre IS NULL OR UPPER(i.nombre) LIKE UPPER(CONCAT('%', CAST(:nombre AS string), '%'))) "
                    + "AND (:tipoClase IS NULL OR TYPE(i) = :tipoClase)")
    Page<InsumoEntity> filtrarInsumos(@Param("nombre") String nombre,
                                       @Param("tipoClase") Class<? extends InsumoEntity> tipoClase,
                                       Pageable pageable);

    /**
     * Filtra, paginados, los insumos activos para el listado de stock, ordenados alfabéticamente por nombre (sin
     * distinguir mayúsculas) y, ante igual nombre, por ID.
     * <p>
     * La situación de stock se resuelve con subconsultas {@code EXISTS} sobre los lotes, sin agrupar, de modo que el
     * conteo y la paginación sean los de una consulta simple. Como la cantidad reservada nunca supera a la actual,
     * la suma de lo disponible de los lotes vigentes es mayor a 0 si y solo si algún lote vigente tiene saldo
     * disponible; lo mismo vale para la cantidad vencida. Un lote vence cuando su fecha es anterior a hoy.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre con los comodines de {@code LIKE} ya escapados con {@code '!'}, o {@code null} para no filtrar.
     * @param tipoClase Clase concreta del tipo de insumo, o {@code null} para no filtrar por tipo.
     * @param exigirConDisponible Si es {@code true}, solo los insumos con cantidad disponible mayor a 0.
     * @param exigirSinDisponible Si es {@code true}, solo los insumos sin cantidad disponible.
     * @param exigirConVencidos Si es {@code true}, solo los insumos con cantidad vencida mayor a 0.
     * @param hoy Fecha de referencia para decidir qué lotes están vencidos.
     * @param pageable La configuración de paginación (sin orden: lo define la consulta).
     * @return Una página de insumos activos.
     */
    @Query(value = "SELECT i FROM InsumoEntity i WHERE i.estado = 'ACTIVO' "
            + "AND (:nombre IS NULL OR UPPER(i.nombre) LIKE UPPER(CONCAT('%', CAST(:nombre AS string), '%')) ESCAPE '!') "
            + "AND (:tipoClase IS NULL OR TYPE(i) = :tipoClase) "
            + "AND (:exigirConDisponible = FALSE OR EXISTS (SELECT 1 FROM LoteInsumoEntity li WHERE li.insumo = i "
            + "AND li.fechaVencimiento >= :hoy AND (li.cantidadActual - li.cantidadReservada) > 0.00005)) "
            + "AND (:exigirSinDisponible = FALSE OR NOT EXISTS (SELECT 1 FROM LoteInsumoEntity li WHERE li.insumo = i "
            + "AND li.fechaVencimiento >= :hoy AND (li.cantidadActual - li.cantidadReservada) > 0.00005)) "
            + "AND (:exigirConVencidos = FALSE OR EXISTS (SELECT 1 FROM LoteInsumoEntity li WHERE li.insumo = i "
            + "AND li.fechaVencimiento < :hoy AND li.cantidadActual > 0.00005)) "
            + "ORDER BY UPPER(i.nombre) ASC, i.id ASC",
            countQuery = "SELECT COUNT(i) FROM InsumoEntity i WHERE i.estado = 'ACTIVO' "
                    + "AND (:nombre IS NULL OR UPPER(i.nombre) LIKE UPPER(CONCAT('%', CAST(:nombre AS string), '%')) ESCAPE '!') "
                    + "AND (:tipoClase IS NULL OR TYPE(i) = :tipoClase) "
                    + "AND (:exigirConDisponible = FALSE OR EXISTS (SELECT 1 FROM LoteInsumoEntity li WHERE li.insumo = i "
                    + "AND li.fechaVencimiento >= :hoy AND (li.cantidadActual - li.cantidadReservada) > 0.00005)) "
                    + "AND (:exigirSinDisponible = FALSE OR NOT EXISTS (SELECT 1 FROM LoteInsumoEntity li WHERE li.insumo = i "
                    + "AND li.fechaVencimiento >= :hoy AND (li.cantidadActual - li.cantidadReservada) > 0.00005)) "
                    + "AND (:exigirConVencidos = FALSE OR EXISTS (SELECT 1 FROM LoteInsumoEntity li WHERE li.insumo = i "
                    + "AND li.fechaVencimiento < :hoy AND li.cantidadActual > 0.00005))")
    Page<InsumoEntity> filtrarInsumosParaStock(@Param("nombre") String nombre,
                                               @Param("tipoClase") Class<? extends InsumoEntity> tipoClase,
                                               @Param("exigirConDisponible") boolean exigirConDisponible,
                                               @Param("exigirSinDisponible") boolean exigirSinDisponible,
                                               @Param("exigirConVencidos") boolean exigirConVencidos,
                                               @Param("hoy") LocalDate hoy,
                                               Pageable pageable);
}
