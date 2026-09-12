package com.github.heikyudev.maestrocervecero.persistence.repository.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
            + "AND (:nombre IS NULL OR UPPER(i.nombre) LIKE UPPER(CONCAT('%', :nombre, '%'))) "
            + "AND (:tipoClase IS NULL OR TYPE(i) = :tipoClase)",
            countQuery = "SELECT COUNT(i) FROM InsumoEntity i WHERE i.estado = 'ACTIVO' "
                    + "AND (:nombre IS NULL OR UPPER(i.nombre) LIKE UPPER(CONCAT('%', :nombre, '%'))) "
                    + "AND (:tipoClase IS NULL OR TYPE(i) = :tipoClase)")
    Page<InsumoEntity> filtrarInsumos(@Param("nombre") String nombre,
                                       @Param("tipoClase") Class<? extends InsumoEntity> tipoClase,
                                       Pageable pageable);
}
