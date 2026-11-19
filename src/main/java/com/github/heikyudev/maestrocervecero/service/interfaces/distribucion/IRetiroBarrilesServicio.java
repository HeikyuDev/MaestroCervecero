package com.github.heikyudev.maestrocervecero.service.interfaces.distribucion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.distribucion.RetiroBarrilesFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceRetiroInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.distribucion.RetiroBarrilesResponseDTO;

/**
 * Interfaz que define los servicios de la pantalla pública con la que un cliente coordina el retiro de sus barriles con
 * la devolución vencida desde el enlace del recordatorio que recibió por correo, sin iniciar sesión.
 * <p>
 * Un enlace sin responder sirve hasta su fecha de expiración y mientras el cliente no reciba un recordatorio más nuevo;
 * el cliente tiene que seguir activo. Una vez confirmado el retiro, el enlace muestra lo registrado hasta su fecha de
 * expiración (como mínimo, hasta la fecha del retiro) y no admite cambios.
 * </p>
 */
public interface IRetiroBarrilesServicio {

    /**
     * Obtiene lo que el cliente ve al abrir el enlace: sus barriles del recordatorio que siguen pendientes de retiro
     * (despacho vigente y sin solicitud de búsqueda pendiente) con el plazo para proponerlo o, si ya respondió, el
     * retiro registrado.
     *
     * @param token Token del enlace.
     * @return El retiro del cliente.
     * @throws EnlaceRetiroInvalidoException Si el token falta, es demasiado largo o no corresponde a ningún enlace; si el enlace venció; si fue reemplazado por un recordatorio más nuevo sin haberse respondido; o si el cliente ya no está activo.
     */
    RetiroBarrilesResponseDTO buscarRetiro(String token);

    /**
     * Registra el retiro que propone el cliente.
     * <p>
     * Bloquea el recordatorio y, en orden de ID, cada barril del recordatorio (el mismo lock que usan las operaciones del
     * barril) antes de verificar que su despacho siga vigente y sin solicitud pendiente. Los barriles que ya no cumplen
     * (devueltos, despachos anulados o con un retiro ya coordinado) se omiten.
     * Postcondiciones: en una sola transacción se registra una solicitud de búsqueda pendiente por cada barril que sigue
     * pendiente, con la fecha y las observaciones indicadas y vinculada al recordatorio, y el recordatorio queda
     * respondido con la fecha actual (su enlace sirve, como mínimo, hasta la fecha del retiro). Recién confirmada la
     * transacción se avisa por correo a los encargados de distribución activos.
     * </p>
     *
     * @param token Token del enlace.
     * @param retiroBarrilesFormDTO La fecha y hora del retiro y las observaciones.
     * @return El retiro registrado.
     * @throws EnlaceRetiroInvalidoException Por los mismos motivos que {@link #buscarRetiro(String)}, verificados de nuevo con el recordatorio bloqueado.
     * @throws ReglaNegocioException Si el retiro ya fue registrado desde este enlace; si faltan los datos; si la fecha y hora del retiro falta, es anterior a la actual o supera los días máximos de anticipación configurados; si las observaciones superan {@code LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO} caracteres; o si ninguno de los barriles sigue pendiente de retiro. Las reglas de la fecha y de las observaciones indican el campo afectado.
     */
    RetiroBarrilesResponseDTO registrarRetiro(String token, RetiroBarrilesFormDTO retiroBarrilesFormDTO);
}
