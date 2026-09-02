package com.github.heikyudev.maestrocervecero.persistence.repository.cliente;

import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los clientes ({@link ClienteEntity}).
 * <p>
 * La entidad no utiliza {@code @SoftDelete}: el filtrado de clientes dados de baja se realiza
 * explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}. Los métodos
 * heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se sobrescriben con esa
 * misma condición para que todo el código existente que ya los invoca (sin cambios) siga viendo
 * únicamente clientes activos.
 * </p>
 */
@Repository
public interface IClienteRepository extends JpaRepository<ClienteEntity, Long> {

    /**
     * Busca un cliente activo por su ID.
     *
     * @param id El ID del cliente a buscar.
     * @return Un Optional que contiene el cliente si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT c FROM ClienteEntity c WHERE c.id = :id AND c.estado = 'ACTIVO'")
    Optional<ClienteEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de clientes activos.
     *
     * @param pageable La configuración de paginación.
     * @return Una página de clientes activos.
     */
    @Override
    @Query(value = "SELECT c FROM ClienteEntity c WHERE c.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(c) FROM ClienteEntity c WHERE c.estado = 'ACTIVO'")
    Page<ClienteEntity> findAll(Pageable pageable);

    /**
     * Verifica si existe un cliente activo con el correo electrónico dado (ignorando mayúsculas
     * y minúsculas) o el teléfono dado.
     *
     * @param email El correo electrónico a buscar.
     * @param telefono El teléfono a buscar.
     * @return {@code true} si ya existe un cliente activo con ese correo electrónico o ese teléfono, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ClienteEntity c WHERE (UPPER(c.email) = UPPER(:email) OR c.telefono = :telefono) AND c.estado = 'ACTIVO'")
    boolean existsByEmailIgnoreCaseOrTelefono(@Param("email") String email, @Param("telefono") String telefono);

    /**
     * Verifica si existe un cliente activo con el correo electrónico dado (ignorando mayúsculas
     * y minúsculas) o el teléfono dado, excluyendo de la búsqueda al cliente con el ID indicado.
     * <p>
     * Se utiliza en la modificación para permitir conservar el propio correo electrónico o
     * teléfono actual sin que la validación de unicidad falle contra el mismo registro.
     * </p>
     *
     * @param email El correo electrónico a buscar.
     * @param telefono El teléfono a buscar.
     * @param id El ID del cliente a excluir de la verificación.
     * @return {@code true} si otro cliente activo ya posee ese correo electrónico o ese teléfono, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ClienteEntity c WHERE (UPPER(c.email) = UPPER(:email) OR c.telefono = :telefono) AND c.id <> :id AND c.estado = 'ACTIVO'")
    boolean existsByEmailIgnoreCaseOrTelefonoAndIdNot(@Param("email") String email, @Param("telefono") String telefono, @Param("id") Long id);

    /**
     * Verifica si existe algún cliente activo asociado a la localidad indicada.
     * <p>
     * Se utiliza para impedir la baja de una localidad que todavía tiene clientes activos
     * dependientes de ella.
     * </p>
     *
     * @param idLocalidad El ID de la localidad a verificar.
     * @return {@code true} si existe al menos un cliente activo asociado a esa localidad, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM ClienteEntity c WHERE c.localidad.id = :idLocalidad AND c.estado = 'ACTIVO'")
    boolean existsByLocalidadId(@Param("idLocalidad") Long idLocalidad);
}
