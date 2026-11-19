package com.github.heikyudev.maestrocervecero.persistence.repository.distribucion;

import com.github.heikyudev.maestrocervecero.persistence.entity.distribucion.InvitacionRetiroBarrilesEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de los recordatorios de retiro de barriles que reciben los clientes ({@link InvitacionRetiroBarrilesEntity}).
 */
@Repository
public interface IInvitacionRetiroBarrilesRepository extends JpaRepository<InvitacionRetiroBarrilesEntity, Long> {

    /**
     * El ID del recordatorio cuyo token tiene el hash indicado, sin cargar entidades: quien responde bloquea el
     * recordatorio antes de cargarlo.
     *
     * @param tokenHash Hash SHA-256 del token, en hexadecimal.
     * @return El ID del recordatorio, o vacío si ningún enlace tiene ese token.
     */
    @Query("SELECT i.id FROM InvitacionRetiroBarrilesEntity i WHERE i.tokenHash = :tokenHash")
    Optional<Long> buscarIdPorTokenHash(@Param("tokenHash") String tokenHash);

    /**
     * Busca un recordatorio por su ID, con su cliente cargado, y bloquea su fila hasta el fin de la transacción
     * ({@code PESSIMISTIC_WRITE}): serializa las respuestas y los envíos sobre el mismo recordatorio.
     *
     * @param id El ID del recordatorio.
     * @return El recordatorio, o vacío si no existe.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InvitacionRetiroBarrilesEntity i JOIN FETCH i.cliente WHERE i.id = :id")
    Optional<InvitacionRetiroBarrilesEntity> buscarPorIdParaActualizar(@Param("id") Long id);

    /**
     * Busca el recordatorio de un cliente de un día determinado y bloquea su fila hasta el fin de la transacción.
     *
     * @param idCliente El ID del cliente.
     * @param fecha El día del recordatorio.
     * @return El recordatorio, o vacío si el cliente no tiene uno ese día.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InvitacionRetiroBarrilesEntity i WHERE i.cliente.id = :idCliente AND i.fechaInvitacion = :fecha")
    Optional<InvitacionRetiroBarrilesEntity> buscarDelClienteEnFechaParaActualizar(@Param("idCliente") Long idCliente,
                                                                                  @Param("fecha") LocalDate fecha);

    /**
     * Verifica si el cliente recibió un recordatorio posterior al indicado: uno con un ID mayor cuyo correo salió
     * (enviado) o puede estar saliendo (en envío desde el límite indicado). Un recordatorio posterior cuyo correo no salió,
     * o cuyo envío quedó trabado, no invalida el anterior.
     *
     * @param idCliente El ID del cliente.
     * @param idInvitacion El ID del recordatorio de referencia.
     * @param limiteEnEnvio Momento desde el cual un recordatorio en envío todavía no se considera trabado.
     * @return {@code true} si existe un recordatorio posterior.
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM InvitacionRetiroBarrilesEntity i "
            + "WHERE i.cliente.id = :idCliente AND i.id > :idInvitacion AND (i.estadoEnvio = 'ENVIADA' "
            + "OR (i.estadoEnvio = 'EN_ENVIO' AND i.fechaUltimoIntentoEnvio >= :limiteEnEnvio))")
    boolean existePosteriorEnviada(@Param("idCliente") Long idCliente, @Param("idInvitacion") Long idInvitacion,
                                   @Param("limiteEnEnvio") LocalDateTime limiteEnEnvio);

    /**
     * Los IDs de los clientes cuyo recordatorio del día indicado no se envió todavía, falló o quedó trabado en envío (su
     * último intento es anterior al límite indicado) y no agotó sus intentos, de menor a mayor.
     *
     * @param fecha El día de los recordatorios.
     * @param maximoIntentos Intentos de envío a partir de los cuales un recordatorio ya no se reintenta.
     * @param limiteEnEnvio Momento antes del cual un recordatorio que sigue en envío se considera trabado.
     * @return Los IDs de los clientes con envíos pendientes ese día.
     */
    @Query("SELECT i.cliente.id FROM InvitacionRetiroBarrilesEntity i WHERE i.fechaInvitacion = :fecha "
            + "AND (i.estadoEnvio IN ('PENDIENTE', 'FALLIDA') OR (i.estadoEnvio = 'EN_ENVIO' AND i.fechaUltimoIntentoEnvio < :limiteEnEnvio)) "
            + "AND i.intentosEnvio < :maximoIntentos ORDER BY i.cliente.id")
    List<Long> buscarIdsClientesConEnviosPendientes(@Param("fecha") LocalDate fecha, @Param("maximoIntentos") int maximoIntentos,
                                                    @Param("limiteEnEnvio") LocalDateTime limiteEnEnvio);
}
