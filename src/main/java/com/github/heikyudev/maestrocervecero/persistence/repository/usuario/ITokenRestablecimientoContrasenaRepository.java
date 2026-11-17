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
}
