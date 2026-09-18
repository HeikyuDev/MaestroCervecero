package com.github.heikyudev.maestrocervecero.persistence.repository.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los usuarios ({@link UsuarioEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de usuarios dados de baja se
 * realiza explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}.
 * Los métodos heredados de {@link JpaRepository} ({@code findById}, {@code existsById},
 * {@code findAll}) se sobrescriben con esa misma condición para que todo el código existente
 * que ya los invoca (sin cambios) siga viendo únicamente usuarios activos.
 * </p>
 */
@Repository
public interface IUsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    /**
     * Busca un usuario activo por su ID.
     *
     * @param id El ID del usuario a buscar.
     * @return Un Optional que contiene el usuario si está activo, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT u FROM UsuarioEntity u WHERE u.id = :id AND u.estado = 'ACTIVO'")
    Optional<UsuarioEntity> findById(@Param("id") Long id);

    /**
     * Verifica si existe un usuario activo con el id dado.
     * @param id El id del usuario a buscar.
     * @return true si existe un usuario activo con el id dado, false en caso contrario.
     */
    @Override
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UsuarioEntity u WHERE u.id = :id AND u.estado = 'ACTIVO'")
    boolean existsById(@Param("id") Long id);

    /**
     * Obtiene una página de usuarios activos.
     * @param pageable La configuración de paginación.
     * @return Una página de usuarios activos.
     */
    @Override
    @Query(value = "SELECT u FROM UsuarioEntity u WHERE u.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(u) FROM UsuarioEntity u WHERE u.estado = 'ACTIVO'")
    Page<UsuarioEntity> findAll(Pageable pageable);

    /**
     * Busca un usuario activo por su nombre de usuario. Utilizado por {@code UserDetailServiceImpl}
     * en cada intento de login: un usuario dado de baja no se encuentra, por lo que no puede
     * autenticarse.
     * @param username El nombre de usuario a buscar.
     * @return Un Optional que contiene el usuario si está activo, o vacío si no se encuentra.
     */
    @Query("SELECT u FROM UsuarioEntity u WHERE u.username = :username AND u.estado = 'ACTIVO'")
    Optional<UsuarioEntity> findUserEntityByUsername(@Param("username") String username);

    /**
     * Verifica si existe un usuario activo con el nombre de usuario dado.
     * @param username El nombre de usuario a buscar.
     * @return true si existe un usuario activo con el nombre de usuario dado, false en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UsuarioEntity u WHERE UPPER(u.username) = UPPER(:username) AND u.estado = 'ACTIVO'")
    boolean existsByUsername(@Param("username") String username);

    /**
     * Filtra los usuarios activos, opcionalmente por nombre, correo electrónico, username y/o rol
     * (coincidencia parcial y sin distinguir mayúsculas/minúsculas para los tres primeros, exacta
     * para el rol). Un parámetro nulo no restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre, o {@code null} para no filtrar por él.
     * @param correo Texto a buscar dentro del correo electrónico, o {@code null} para no filtrar por él.
     * @param username Texto a buscar dentro del nombre de usuario, o {@code null} para no filtrar por él.
     * @param rol El rol exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de usuarios activos que cumplen los criterios indicados.
     */
    @Query(value = "SELECT u FROM UsuarioEntity u WHERE u.estado = 'ACTIVO' "
            + "AND (:nombre IS NULL OR UPPER(u.nombre) LIKE UPPER(CONCAT('%', :nombre, '%'))) "
            + "AND (:correo IS NULL OR UPPER(u.correo) LIKE UPPER(CONCAT('%', :correo, '%'))) "
            + "AND (:username IS NULL OR UPPER(u.username) LIKE UPPER(CONCAT('%', :username, '%'))) "
            + "AND (:rol IS NULL OR u.rol = :rol)",
            countQuery = "SELECT COUNT(u) FROM UsuarioEntity u WHERE u.estado = 'ACTIVO' "
                    + "AND (:nombre IS NULL OR UPPER(u.nombre) LIKE UPPER(CONCAT('%', :nombre, '%'))) "
                    + "AND (:correo IS NULL OR UPPER(u.correo) LIKE UPPER(CONCAT('%', :correo, '%'))) "
                    + "AND (:username IS NULL OR UPPER(u.username) LIKE UPPER(CONCAT('%', :username, '%'))) "
                    + "AND (:rol IS NULL OR u.rol = :rol)")
    Page<UsuarioEntity> filtrarUsuarios(@Param("nombre") String nombre,
                                         @Param("correo") String correo,
                                         @Param("username") String username,
                                         @Param("rol") Rol rol,
                                         Pageable pageable);
}
