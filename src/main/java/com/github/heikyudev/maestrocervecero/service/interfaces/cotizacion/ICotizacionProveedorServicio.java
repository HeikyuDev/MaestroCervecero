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
     * Obtiene lo que el proveedor ve al abrir el enlace: los insumos solicitados que ofrece, cada uno con las
     * presentaciones de su catálogo vigente, y su respuesta actual (ofertas por presentación y fecha estimada de entrega)
     * si ya respondió.
     *
     * @param token Token del enlace.
     * @return La cotización del proveedor.
     * @throws EnlaceCotizacionInvalidoException Si el token falta, es demasiado largo o no corresponde a ningún enlace; si la solicitud fue cancelada o ya no está abierta; si venció el plazo de respuesta; o si el proveedor ya no está activo.
     */
    CotizacionProveedorResponseDTO buscarCotizacion(String token);

    /**
     * Registra la respuesta del proveedor, reemplazando la anterior si ya había respondido.
     * <p>
     * Cada fila es un insumo solicitado en una presentación del catálogo vigente del proveedor: un mismo insumo puede
     * cotizarse en varias presentaciones a la vez. Una fila sin precio ni cantidad no responde esa presentación; una
     * fila con cantidad 0 y sin precio indica que no tiene stock; con cantidad mayor a 0 se exige el precio, con cantidad
     * 0 no se admite precio y con precio se exige la cantidad. Si ofrece alguna presentación con stock, la fecha estimada
     * de entrega es obligatoria.
     * Postcondiciones: el proveedor queda con una oferta por cada fila completa (las filas vacías quitan la oferta
     * anterior de esa presentación), con la fecha estimada de entrega indicada (nula si no ofrece ninguna presentación
     * con stock) y la fecha de respuesta de la invitación es la actual.
     * </p>
     *
     * @param token Token del enlace.
     * @param cotizacionProveedorFormDTO Las ofertas por insumo y presentación, y la fecha estimada de entrega.
     * @return La cotización del proveedor con la respuesta registrada.
     * @throws EnlaceCotizacionInvalidoException Por los mismos motivos que {@link #buscarCotizacion(String)}, verificados de nuevo con la solicitud bloqueada.
     * @throws ReglaNegocioException Si faltan los datos de la cotización; si supera el máximo de filas; si una fila es nula, no indica el insumo o la presentación, indica un insumo que no es de la solicitud o que el proveedor no ofrece, o una presentación que no es del catálogo vigente del proveedor para ese insumo; si repite la misma presentación de un insumo; si una fila indica precio sin cantidad, cantidad mayor a 0 sin precio o precio con cantidad 0; si el precio unitario no es mayor a 0, supera {@code LimitesTecnicos.COSTO_UNITARIO_COMPRA_MAXIMO} o tiene más de {@code LimitesTecnicos.ESCALA_COSTO_UNITARIO} decimales; si la cantidad disponible no es mayor a 0 o supera {@code LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA}; o si, ofreciendo alguna presentación con stock, la fecha estimada de entrega falta, es anterior a la fecha actual o supera {@code LimitesTecnicos.ORDEN_COMPRA_MAXIMO_DIAS_ENTREGA} días. Las reglas de una fila o de la fecha indican el campo afectado.
     */
    CotizacionProveedorResponseDTO registrarCotizacion(String token, CotizacionProveedorFormDTO cotizacionProveedorFormDTO);
}
