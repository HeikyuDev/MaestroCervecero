package com.github.heikyudev.maestrocervecero.persistence.repository.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.TokenRestablecimientoContrasenaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio JPA de los enlaces para restablecer la contraseña ({@link TokenRestablecimientoContrasenaEntity}).
 */
@Repository
public interface ITokenRestablecimientoContrasenaRepository extends JpaRepository<TokenRestablecimientoContrasenaEntity, Long> {

    /**
     * Busca un enlace por el hash de su token, junto con su usuario.
     *
     * @param tokenHash Hash SHA-256 del token, en hexadecimal.
     * @return Un Optional con el enlace, o vacío si ningún enlace tiene ese hash.
     */
    @Query("SELECT t FROM TokenRestablecimientoContrasenaEntity t JOIN FETCH t.usuario WHERE t.tokenHash = :tokenHash")
    Optional<TokenRestablecimientoContrasenaEntity> buscarPorTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Verifica si se le envió a un usuario algún enlace después del momento indicado.
     *
     * @param idUsuario El ID del usuario.
     * @param desde Momento desde el cual se buscan enlaces enviados (excluido).
     * @return {@code true} si el usuario tiene al menos un enlace creado después de {@code desde}, {@code false} en caso contrario.
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TokenRestablecimientoContrasenaEntity t "
            + "WHERE t.usuario.id = :idUsuario AND t.createdDate > :desde")
    boolean existeEnviadoDespuesDe(@Param("idUsuario") Long idUsuario, @Param("desde") LocalDateTime desde);

    /**
     * Hace vencer, en el momento indicado, los enlaces del usuario que todavía sirven (sin usar y sin vencer).
     *
     * @param idUsuario El ID del usuario.
     * @param ahora Momento actual: los enlaces vigentes pasan a vencer en él.
     * @return Cantidad de enlaces que dejaron de servir.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE TokenRestablecimientoContrasenaEntity t SET t.fechaExpiracion = :ahora "
            + "WHERE t.usuario.id = :idUsuario AND t.fechaUso IS NULL AND t.fechaExpiracion > :ahora")
    int vencerVigentesDelUsuario(@Param("idUsuario") Long idUsuario, @Param("ahora") LocalDateTime ahora);

    /**
     * Marca el enlace como usado en el momento indicado, solo si todavía sirve (sin usar y sin vencer). Es una única
     * sentencia: de dos pedidos simultáneos con el mismo enlace, solo uno lo reclama.
     *
     * @param id El ID del enlace.
     * @param ahora Momento actual: queda como fecha de uso.
     * @return 1 si el enlace se reclamó, 0 si ya estaba usado o vencido.
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE TokenRestablecimientoContrasenaEntity t SET t.fechaUso = :ahora "
            + "WHERE t.id = :id AND t.fechaUso IS NULL AND t.fechaExpiracion > :ahora")
    int reclamarParaUso(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);

    /**
     * Lee de la base la fecha de uso de un enlace, sin pasar por las entidades ya cargadas. Distingue, tras un reclamo
     * fallido, si el enlace se usó o venció.
     *
     * @param id El ID del enlace.
     * @return Un Optional con la fecha de uso, o vacío si el enlace no se usó.
     */
    @Query("SELECT t.fechaUso FROM TokenRestablecimientoContrasenaEntity t WHERE t.id = :id")
    Optional<LocalDateTime> buscarFechaUso(@Param("id") Long id);
}
