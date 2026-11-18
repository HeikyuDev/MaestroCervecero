package com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.InvitacionCotizacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de las invitaciones de proveedores a responder una solicitud de cotización
 * ({@link InvitacionCotizacionEntity}).
 */
@Repository
public interface IInvitacionCotizacionRepository extends JpaRepository<InvitacionCotizacionEntity, Long> {

    /**
     * Busca la invitación cuyo token tiene el hash indicado, con su solicitud y su proveedor cargados.
     *
     * @param tokenHash Hash SHA-256 del token, en hexadecimal.
     * @return La invitación, o vacío si ningún enlace tiene ese token.
     */
    @Query("SELECT i FROM InvitacionCotizacionEntity i JOIN FETCH i.solicitud JOIN FETCH i.proveedor WHERE i.tokenHash = :tokenHash")
    Optional<InvitacionCotizacionEntity> buscarPorTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * El ID de la solicitud de la invitación cuyo token tiene el hash indicado, sin cargar entidades: quien responde
     * bloquea la solicitud antes de cargarla.
     *
     * @param tokenHash Hash SHA-256 del token, en hexadecimal.
     * @return El ID de la solicitud, o vacío si ningún enlace tiene ese token.
     */
    @Query("SELECT i.solicitud.id FROM InvitacionCotizacionEntity i WHERE i.tokenHash = :tokenHash")
    Optional<Long> buscarIdSolicitudPorTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Los IDs de las solicitudes abiertas y vigentes con alguna invitación cuyo correo no se envió todavía o falló y que
     * no agotó sus intentos (las que están en envío, enviadas u omitidas no cuentan), de la más antigua a la más nueva.
     *
     * @param ahora Momento de referencia: la solicitud no debe haber vencido.
     * @param maximoIntentos Intentos de envío a partir de los cuales una invitación ya no se reintenta.
     * @return Los IDs de las solicitudes con envíos pendientes.
     */
    @Query("SELECT DISTINCT i.solicitud.id FROM InvitacionCotizacionEntity i WHERE i.solicitud.estado = 'ABIERTA' "
            + "AND i.solicitud.fechaLimiteRespuesta > :ahora AND i.estadoEnvio IN ('PENDIENTE', 'FALLIDA') AND i.intentosEnvio < :maximoIntentos "
            + "ORDER BY i.solicitud.id")
    java.util.List<Long> buscarIdsSolicitudesConEnviosPendientes(@Param("ahora") java.time.LocalDateTime ahora,
                                                                 @Param("maximoIntentos") int maximoIntentos);
}
