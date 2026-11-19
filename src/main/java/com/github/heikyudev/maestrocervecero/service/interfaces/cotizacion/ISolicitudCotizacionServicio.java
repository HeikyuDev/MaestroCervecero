package com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.OrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.OrdenCompraPrearmadaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.OrdenCompraPropuestaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.SolicitudCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz que define los servicios del proceso "Generación automática de planes de órdenes de compra".
 * <p>
 * El proceso arranca al registrar una planificación de producción: si la receta escalada a la cantidad a producir
 * necesita más insumo del que hay disponible o en camino, se genera una solicitud de cotización consolidada pendiente de
 * confirmación y se avisa a los gerentes de compras. Cuando un gerente confirma el envío, se invita por correo a los
 * proveedores activos que ofrecen esos insumos. Al vencer el plazo de respuesta, un proceso programado
 * la cierra y genera su plan de órdenes de compra, y se avisa a los gerentes de compras. El gerente revisa el plan y
 * confirma cada orden propuesta en el alta de órdenes de compra: el proceso nunca registra órdenes de compra ni pagos
 * por sí mismo.
 * </p>
 */
public interface ISolicitudCotizacionServicio {

    /**
     * Genera la solicitud de cotización de una planificación de producción recién registrada.
     * <p>
     * La planificación se escala como un único lote virtual cuyo volumen objetivo es su cantidad a producir, con las
     * fórmulas de escalado de insumos y los parámetros promedio de los equipos activos de la planta. De esas fórmulas,
     * solo la malta depende de un equipo (la eficiencia de maceración del macerador); el lúpulo y la levadura dependen de
     * la receta y del volumen, y la evaporación y la pérdida por trub solo cambian el agua, que no se compra. No se
     * valida ninguna capacidad: el volumen virtual es una estimación del total, no un lote real. El faltante de cada
     * insumo es la cantidad requerida menos lo libre (el stock vigente sin reservar menos lo que van a consumir los lotes
     * registrados sin iniciar de cualquier planificación que todavía no reservaron, según la instantánea de su escalado; lo
     * libre nunca es negativo), menos lo que resta recibir de las órdenes de compra pendientes. Corre en una transacción propia: una falla no revierte la planificación ya registrada.
     * </p>
     * <p>
     * Postcondiciones: si la planificación ya no está pendiente o ya tiene una solicitud no cancelada, no se crea nada.
     * Si la receta tiene malta y no hay maceradores activos, no se crea nada y se avisa a los gerentes de compras. Si no
     * se puede calcular la cantidad de algún grupo de insumos (datos incompletos), se sigue con los demás y se avisa a
     * los gerentes. Sin faltantes no se crea nada. Con faltantes se crea una solicitud con una línea por insumo
     * faltante y un proveedor candidato (invitación sin enlace ni vencimiento) por cada proveedor activo que ofrece alguno
     * de esos insumos, en estado {@code PENDIENTE_DE_CONFIRMACION}, y se pide a los gerentes de compras que confirmen el
     * envío con la explicación de los insumos que el stock no cubre (los insumos sin proveedor quedan marcados). Si ningún
     * proveedor ofrece ningún insumo, la solicitud queda {@code CERRADA} de inmediato y se avisa a los gerentes.
     * </p>
     *
     * @param idPlanificacion ID de la planificación de producción registrada.
     * @return La solicitud generada, o vacío si no se generó ninguna.
     * @throws RecursoNoEncontradoException Si no existe la planificación de producción.
     */
    Optional<SolicitudCotizacionResponseDTO> generarSolicitud(Long idPlanificacion);

    /**
     * Confirma el envío de una solicitud pendiente de confirmación a sus proveedores candidatos.
     * <p>
     * Postcondiciones: los candidatos dados de baja o que ya no ofrecen ningún insumo faltante se quitan; la solicitud
     * queda {@code ABIERTA} con la fecha de confirmación y la fecha límite de respuesta igual a ahora más el plazo de la
     * configuración de compras (el plazo corre desde la confirmación); cada invitación vence en esa fecha y queda con
     * envío pendiente. Los correos los envía {@code EnvioInvitacionesCotizacion#enviarPendientes(Long)} cuando esta transacción se
     * confirmó (y los reintenta el proceso programado si fallan).
     * </p>
     *
     * @param id ID de la solicitud.
     * @return La solicitud abierta.
     * @throws RecursoNoEncontradoException Si la solicitud o su planificación no existen.
     * @throws ReglaNegocioException Si la solicitud no está pendiente de confirmación, si la planificación ya no está pendiente o si ningún proveedor candidato sigue activo y ofrece los insumos faltantes.
     */
    SolicitudCotizacionResponseDTO confirmarEnvio(Long id);

    /**
     * Las solicitudes que esperan que el gerente de compras confirme el envío. Es el aviso dentro del sistema de que hay
     * solicitudes por confirmar.
     *
     * @return Las solicitudes pendientes de confirmación, de la más nueva a la más vieja.
     */
    List<SolicitudCotizacionResponseDTO> buscarPendientesDeConfirmacion();

    /**
     * Prepara, en una transacción corta, los correos de las invitaciones de una solicitud abierta y vigente cuyo correo
     * no se envió todavía o falló, hasta {@code LimitesTecnicos.INTENTOS_ENVIO_INVITACION_MAXIMO} intentos por invitación.
     * No envía nada: el correo se envía después, fuera de toda transacción.
     * <p>
     * Postcondiciones: cada invitación preparada tiene un token nuevo guardado (como hash), suma un intento y queda
     * {@code EN_ENVIO}; las de proveedores dados de baja o sin versión vigente quedan {@code OMITIDA} (estado final). Si
     * la solicitud no existe, no está abierta o venció, no cambia nada.
     * </p>
     *
     * @param idSolicitud ID de la solicitud.
     * @return Los correos listos para enviar, con su token.
     */
    List<EnvioInvitacionPreparado> prepararEnvios(Long idSolicitud);

    /**
     * Registra, en otra transacción corta, el resultado de los correos enviados.
     * <p>
     * Postcondiciones: cada invitación en envío queda {@code ENVIADA} o {@code FALLIDA}; las que fallaron en su último
     * intento permitido se avisan a los gerentes de compras.
     * </p>
     *
     * @param idSolicitud ID de la solicitud.
     * @param resultados Si el servidor de correo aceptó el correo, por ID de invitación.
     */
    void registrarResultadosEnvio(Long idSolicitud, java.util.Map<Long, Boolean> resultados);

    /**
     * Los IDs de las solicitudes abiertas y vigentes con invitaciones cuyo correo falta enviar o reintentar.
     *
     * @return Los IDs, de la más antigua a la más nueva.
     */
    List<Long> buscarIdsConEnviosPendientes();

    /**
     * Filtra las solicitudes de cotización, opcionalmente por estado y por planificación de producción. Primero van las
     * pendientes de confirmación, después las abiertas, de la que vence antes a la que vence después, y luego las demás,
     * de la más nueva a la más vieja.
     *
     * @param estado Estado a filtrar, o {@code null} para no filtrar por él.
     * @param idPlanificacion ID de la planificación a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable Paginación y orden.
     * @return Una página de solicitudes, sin líneas, invitaciones ni plan.
     */
    Page<SolicitudCotizacionResponseDTO> filtrarSolicitudes(EstadoSolicitudCotizacion estado, Long idPlanificacion, Pageable pageable);

    /**
     * Obtiene una solicitud de cotización completa: líneas, invitaciones con su respuesta, plan y avisos.
     *
     * @param id ID de la solicitud.
     * @return La solicitud.
     * @throws RecursoNoEncontradoException Si no existe una solicitud con ese ID.
     */
    SolicitudCotizacionResponseDTO buscarPorId(Long id);

    /**
     * Las solicitudes que esperan una decisión del gerente de compras: tienen el plan generado y todavía no elegido. Es
     * el aviso dentro del sistema de que hay planes listos para revisar.
     *
     * @return Las solicitudes pendientes de decisión, de la más nueva a la más vieja.
     */
    List<SolicitudCotizacionResponseDTO> buscarPendientesDeDecision();

    /**
     * Los IDs de las solicitudes abiertas cuya fecha límite de respuesta ya llegó y cuyo cierre no quedó marcado como
     * fallido, de la más antigua a la más nueva.
     *
     * @return Los IDs de las solicitudes que el cierre programado debe cerrar.
     */
    List<Long> buscarIdsVencidas();

    /**
     * Cierra una solicitud abierta vencida y genera su plan de órdenes de compra.
     * <p>
     * Solo cuentan las ofertas de los proveedores activos; las de proveedores dados de baja se descartan. Se genera un
     * único plan, el mejor según el criterio y el peso del precio de la configuración de compras. Si las ofertas no
     * cubren el faltante, el plan se genera igual con lo disponible.
     * </p>
     * <p>
     * Postcondiciones: si la solicitud no existe, no está abierta o todavía no venció, no cambia nada. Si no hay ofertas
     * útiles queda {@code CERRADA} sin plan y se avisa a los gerentes de compras; si las hay, queda
     * {@code PLAN_GENERADO} con su plan en estado {@code PROPUESTO} y se avisa a los gerentes con el enlace a la
     * solicitud.
     * </p>
     *
     * @param id ID de la solicitud.
     */
    void cerrarSolicitud(Long id);

    /**
     * Registra que el cierre programado de una solicitud falló.
     * <p>
     * Postcondiciones: suma un intento de cierre fallido y guarda un motivo genérico (el detalle técnico solo va al log). Al llegar a {@code LimitesTecnicos.INTENTOS_CIERRE_SOLICITUD_MAXIMO},
     * la solicitud queda marcada con la falla, el proceso programado deja de reintentarla y se avisa a los gerentes de
     * compras. Una solicitud que no existe o ya no está abierta no cambia.
     * </p>
     *
     * @param id ID de la solicitud.
     * @param motivo Detalle técnico de la falla, solo para el log.
     */
    void registrarFallaCierre(Long id, String motivo);

    /**
     * Cancela las solicitudes de una planificación de producción que se anula.
     * <p>
     * Postcondiciones: cada solicitud no cancelada de la planificación queda {@code CANCELADA} con la fecha y el
     * motivo y sus enlaces dejan de servir. Un plan elegido que ya tiene órdenes confirmadas sigue {@code ELEGIDO}; en
     * cualquier otro caso el plan queda {@code CANCELADO}. Las órdenes propuestas sin confirmar quedan {@code DESCARTADA} y las órdenes de compra ya confirmadas no se modifican.
     * </p>
     *
     * @param idPlanificacion ID de la planificación que se anula.
     */
    void cancelarPorAnulacionDePlanificacion(Long idPlanificacion);

    /**
     * Cancela a mano una solicitud de cotización que el gerente de compras ya no necesita.
     * <p>
     * Procede sobre una solicitud pendiente de confirmación, {@code ABIERTA} o con el plan generado que todavía no fue
     * elegido.
     * Postcondiciones: queda {@code CANCELADA} con la fecha y el motivo (el usuario queda en la auditoría de la última
     * modificación); los enlaces de los proveedores dejan de servir en el momento; su plan queda {@code CANCELADO} y
     * sus órdenes propuestas sin confirmar, {@code DESCARTADA}.
     * </p>
     *
     * @param id ID de la solicitud.
     * @param motivo Motivo de la cancelación.
     * @return La solicitud cancelada.
     * @throws RecursoNoEncontradoException Si la solicitud no existe.
     * @throws ReglaNegocioException Si el motivo falta o supera {@code LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO} caracteres, si la solicitud no está pendiente de confirmación, abierta ni con el plan generado, o si su plan ya fue elegido.
     */
    SolicitudCotizacionResponseDTO cancelarSolicitud(Long id, String motivo);

    /**
     * Arma los datos con los que se prearma el alta de una orden de compra a partir de una orden propuesta: la
     * planificación de la solicitud, el proveedor y los ítems con sus cantidades y precios, traducidos al catálogo
     * vigente del proveedor.
     *
     * @param idOrdenPropuesta ID de la orden propuesta.
     * @return Los datos de la orden prearmada, con un aviso por cada ítem que el proveedor ya no ofrece.
     * @throws RecursoNoEncontradoException Si no existe la orden propuesta.
     * @throws ReglaNegocioException Si la orden propuesta no está pendiente de confirmar, si su plan no está propuesto ni elegido, si su solicitud no tiene el plan vigente o si el proveedor fue dado de baja.
     */
    OrdenCompraPrearmadaResponseDTO prepararOrdenCompra(Long idOrdenPropuesta);

    /**
     * Registra la orden de compra definitiva de una orden propuesta, con los datos que completó el gerente de compras
     * en el alta de órdenes de compra. Es intencional que pueda modificar cantidades, costos e ítems respecto de la
     * propuesta (la propuesta es una sugerencia y la orden se negocia con el proveedor): solo se exige que sea del
     * proveedor propuesto y de la planificación de la solicitud, y que cumpla las reglas del alta.
     * <p>
     * Postcondiciones: la orden de compra queda registrada (con las reglas del alta de órdenes de compra) y vinculada a
     * la orden propuesta, que pasa a {@code CONFIRMADA}. Si era la primera orden confirmada del plan, el plan pasa a
     * {@code ELEGIDO} y la solicitud ya no se puede cancelar.
     * </p>
     *
     * @param idOrdenPropuesta ID de la orden propuesta.
     * @param ordenCompraFormDTO Los datos de la orden de compra.
     * @return La orden de compra registrada.
     * @throws RecursoNoEncontradoException Si no existe la orden propuesta, o si el alta de la orden de compra no encuentra alguno de sus datos.
     * @throws ReglaNegocioException Si faltan los datos de la orden; si la orden propuesta no está pendiente de confirmar, su plan no está propuesto ni elegido o su solicitud no tiene el plan vigente; si la orden es para otro proveedor u otra planificación; o si el alta de la orden de compra la rechaza.
     */
    OrdenCompraResponseDTO confirmarOrdenPropuesta(Long idOrdenPropuesta, OrdenCompraFormDTO ordenCompraFormDTO);

    /**
     * Descarta una orden propuesta del plan elegido que el gerente de compras decide no registrar.
     * <p>
     * Postcondiciones: la orden propuesta queda {@code DESCARTADA}; su plan sigue elegido.
     * </p>
     *
     * @param idSolicitud ID de la solicitud a la que debe pertenecer la orden propuesta.
     * @param idOrdenPropuesta ID de la orden propuesta.
     * @return La orden propuesta descartada.
     * @throws RecursoNoEncontradoException Si no existe la orden propuesta o no pertenece a la solicitud indicada.
     * @throws ReglaNegocioException Si la orden propuesta no está pendiente de confirmar o si su plan no es el elegido.
     */
    OrdenCompraPropuestaResponseDTO descartarOrdenPropuesta(Long idSolicitud, Long idOrdenPropuesta);
}
