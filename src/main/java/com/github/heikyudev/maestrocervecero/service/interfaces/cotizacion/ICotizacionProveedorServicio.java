package com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.CotizacionProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceCotizacionInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.CotizacionProveedorResponseDTO;

/**
 * Interfaz que define los servicios de la pantalla pública con la que un proveedor responde una solicitud de
 * cotización desde el enlace que recibió por correo, sin iniciar sesión.
 * <p>
 * Un enlace sirve mientras la solicitud está abierta, no venció su plazo de respuesta y el proveedor sigue activo. El
 * proveedor solo ve los insumos solicitados que ofrece en su catálogo vigente y puede editar su respuesta hasta el
 * cierre.
 * </p>
 */
public interface ICotizacionProveedorServicio {

    /**
     * Obtiene lo que el proveedor ve al abrir el enlace: los insumos solicitados que ofrece, con las presentaciones de
     * su catálogo vigente y su respuesta actual si ya respondió.
     *
     * @param token Token del enlace.
     * @return La cotización del proveedor.
     * @throws EnlaceCotizacionInvalidoException Si el token falta, es demasiado largo o no corresponde a ningún enlace; si la solicitud fue cancelada o ya no está abierta; si venció el plazo de respuesta; o si el proveedor ya no está activo.
     */
    CotizacionProveedorResponseDTO buscarCotizacion(String token);

    /**
     * Registra la respuesta del proveedor, reemplazando la anterior si ya había respondido.
     * <p>
     * Una fila sin presentación, precio ni cantidad no cotiza su insumo; si se completa alguno de los tres, se exigen
     * los tres. Postcondiciones: el proveedor queda con una oferta por cada fila completa (las filas vacías quitan la
     * oferta anterior de ese insumo) y la fecha de respuesta de la invitación es la actual.
     * </p>
     *
     * @param token Token del enlace.
     * @param cotizacionProveedorFormDTO Las ofertas por insumo.
     * @return La cotización del proveedor con la respuesta registrada.
     * @throws EnlaceCotizacionInvalidoException Por los mismos motivos que {@link #buscarCotizacion(String)}, verificados de nuevo con la solicitud bloqueada.
     * @throws ReglaNegocioException Si faltan los datos de la cotización; si supera el máximo de filas; si una fila es nula, no indica el insumo, indica un insumo que no es de la solicitud o que el proveedor no ofrece, o repite un insumo; si una fila incompleta no indica la presentación, el precio o la cantidad; si la presentación no es del catálogo vigente del proveedor para ese insumo; si el precio unitario no es mayor a 0, supera {@code LimitesTecnicos.COSTO_UNITARIO_COMPRA_MAXIMO} o tiene más de {@code LimitesTecnicos.ESCALA_COSTO_UNITARIO} decimales; o si la cantidad disponible no es mayor a 0 o supera {@code LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA}. Las reglas de una fila indican el campo afectado.
     */
    CotizacionProveedorResponseDTO registrarCotizacion(String token, CotizacionProveedorFormDTO cotizacionProveedorFormDTO);
}
