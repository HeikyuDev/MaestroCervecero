package com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanesProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionConLitrosAPlanificarResponseDTO;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz que define los servicios de la generación automática de planes de producción.
 * <p>
 * Un plan de producción es la propuesta de lotes (volumen, equipos y fechas estimadas) que cubre los litros que le
 * faltan planificar a una planificación de producción. Los planes se calculan al momento y nunca se persisten: lo
 * único que se registra son los lotes del plan que el gerente de producción confirma.
 * </p>
 * <h3>Reglas comunes de la generación</h3>
 * <ul>
 *     <li>Litros a planificar: la cantidad a producir menos los litros producidos (envasados registrados) y el volumen
 *     objetivo de los lotes pendientes y en ejecución de la planificación.</li>
 *     <li>Fermentadores candidatos: todos los activos, sin importar su estado operativo (igual que el registro de un
 *     lote). Su capacidad efectiva es el menor valor entre su capacidad útil y el mayor volumen entero de lote que
 *     admiten algún macerador activo y alguna olla de hervor activa, con las mismas fórmulas del registro. Los de
 *     capacidad efectiva 0 se descartan.</li>
 *     <li>Fechas de cada lote: las mismas fórmulas del registro ({@code EstimacionLote}); los lotes del propio plan
 *     también ocupan su fermentador para los lotes siguientes. La vista previa no contempla la prioridad de la
 *     planificación frente a los lotes pendientes de otras planificaciones, que el registro sí aplica.</li>
 *     <li>Volumen de cada lote: proporcional a la capacidad de su fermentador, en litros enteros por el método del resto
 *     (piso de la cuota y 1 L más en los lotes de mayor parte fraccionaria) con un total de {@code ceil(litros)}, sin
 *     superar la capacidad. Si ese excedente (menor a 1 L) supera el excedente máximo, el total entero es
 *     {@code floor(litros)} y el último lote lleva la fracción, de modo que el total es exacto. Así el plan secuencial
 *     siempre cumple las reglas del volumen total.</li>
 *     <li>Un plan que termina después de la fecha de finalización estimada de la planificación se ofrece igual, con un
 *     aviso que no impide confirmarlo.</li>
 *     <li>Equipos del lado caliente de cada lote, entre los activos compatibles con su volumen: el molino de mayor
 *     rendimiento, el macerador de mayor eficiencia y la olla de hervor de menor pérdida por trub (a igualdad, la de
 *     mayor evaporación). El último desempate es el identificador interno.</li>
 * </ul>
 */
public interface IPlanProduccionServicio {

    /**
     * Genera los dos planes de producción alternativos para una planificación: el secuencial y el concurrente, cada
     * uno con el criterio de selección de equipos de la configuración de producción.
     * <ul>
     *     <li>Secuencial: un único fermentador ({@code FERMENTADOR_MAYOR_CAPACIDAD}: el de mayor capacidad efectiva, a
     *     igualdad el que se libera antes; {@code FERMENTADOR_LIBERACION_MAS_TEMPRANA}: el que se libera antes, a
     *     igualdad el de mayor capacidad) con {@code ceil(litros / capacidad)} lotes.</li>
     *     <li>Concurrente (exige al menos dos fermentadores utilizables): {@code MENOR_CANTIDAD_DE_LOTES} prueba los k
     *     fermentadores de mayor capacidad (k = 2..n) en olas de un lote por fermentador y elige la menor cantidad de
     *     lotes, a igualdad la menor cantidad de olas y luego el fin de plan más temprano;
     *     {@code EQUIPOS_LIBERACION_MAS_TEMPRANA} asigna cada lote al fermentador que se libera antes (contando los
     *     lotes del plan ya asignados), a igualdad el de mayor capacidad, hasta cubrir los litros.</li>
     * </ul>
     * <p>
     * Nunca rechaza la generación por una regla de negocio: si un plan no se puede generar (la planificación no está
     * Pendiente, sus lotes ya cubren la cantidad a producir, faltan equipos activos, el plan supera el máximo de lotes o
     * su volumen total no cumple las reglas de la confirmación) se informa como no disponible, con el motivo. Un plan
     * disponible siempre cumple las reglas del volumen total que valida {@link #confirmarPlan}.
     * </p>
     *
     * @param idPlanificacion ID de la planificación de producción.
     * @return Los dos planes de la planificación.
     * @throws RecursoNoEncontradoException Si no existe una planificación de producción con el ID especificado.
     */
    PlanesProduccionResponseDTO generarPlanes(Long idPlanificacion);

    /**
     * Indica por qué una planificación no admite planes de producción: no está en estado Pendiente, o lo producido y
     * sus lotes pendientes y en ejecución ya cubren la cantidad a producir. No calcula ningún plan.
     *
     * @param idPlanificacion ID de la planificación de producción.
     * @return El motivo, o vacío si la planificación todavía tiene litros por planificar.
     * @throws RecursoNoEncontradoException Si no existe una planificación de producción con el ID especificado.
     */
    Optional<String> buscarMotivoSinLitrosAPlanificar(Long idPlanificacion);

    /**
     * Obtiene las planificaciones que esperan un plan de producción: las que están en estado Pendiente y todavía tienen
     * litros a planificar mayores a 0, con la misma regla de la generación (la cantidad a producir menos los litros
     * producidos y el volumen objetivo de sus lotes pendientes y en ejecución). Es el aviso que recuerda al gerente de
     * producción las planificaciones registradas sin un plan confirmado. No calcula ningún plan.
     * <p>
     * Orden: por fecha de inicio estimada ascendente (la más urgente primero) y, a igualdad, por ID ascendente.
     * </p>
     *
     * @return Las planificaciones con litros sin planificar y sus litros a planificar, o una lista vacía si no hay
     *         ninguna.
     */
    List<PlanificacionConLitrosAPlanificarResponseDTO> buscarPlanificacionesConLitrosAPlanificar();

    /**
     * Recalcula un plan de producción con los lotes que indica el gerente de producción (los propuestos o los que
     * modificó), en el orden dado: resuelve sus equipos, sus fechas estimadas y los totales, y agrega como avisos lo
     * que la confirmación rechazaría (un volumen que supera la capacidad de un equipo, un total que no alcanza o que
     * supera el excedente máximo, o un lote de más). No registra nada.
     *
     * @param idPlanificacion ID de la planificación de producción.
     * @param planProduccionFormDTO La modalidad y los lotes del plan.
     * @return El plan recalculado, disponible.
     * @throws ReglaNegocioException Si no se informó la modalidad, si el plan no tiene lotes o supera el máximo de lotes,
     *                               o si a un lote le falta el volumen objetivo (o no es mayor a 0) o algún equipo.
     * @throws RecursoNoEncontradoException Si no existe la planificación o alguno de los equipos activos indicados.
     */
    PlanProduccionResponseDTO recalcularPlan(Long idPlanificacion, PlanProduccionFormDTO planProduccionFormDTO);

    /**
     * Confirma un plan de producción: registra todos sus lotes en una única transacción. Cada lote se registra con
     * {@link ILoteServicio#registrarLote}, con todas sus validaciones; si alguno falla no se registra ninguno. Antes de
     * registrar se bloquean los fermentadores distintos del plan por ID ascendente, para que dos confirmaciones
     * simultáneas no se esperen mutuamente; después los lotes se registran en el orden del plan, que es el orden de sus
     * identificadores internos.
     * <p>
     * Antes de registrar valida el plan completo: el volumen total debe cubrir los litros a planificar sin superarlos
     * en más del excedente máximo configurado, y ningún lote puede sobrar (sin el lote de menor volumen, los demás no
     * deben cubrir los litros a planificar). Esta última regla garantiza que, en cualquier orden, ningún lote encuentre
     * la planificación ya cubierta por los anteriores al registrarse.
     * </p>
     *
     * @param idPlanificacion ID de la planificación de producción.
     * @param planProduccionFormDTO La modalidad y los lotes del plan elegido.
     * @return Los lotes registrados, en el orden del plan.
     * @throws ReglaNegocioException Si no se informó la modalidad, si el plan no tiene lotes o supera el máximo de lotes,
     *                               si a un lote le falta el volumen objetivo (o no es mayor a 0) o algún equipo, si la
     *                               planificación no está Pendiente o sus lotes ya cubren la cantidad a producir, si el
     *                               volumen total no alcanza los litros a planificar o los supera en más del excedente
     *                               máximo, si sobra un lote, o si el registro de algún lote lo rechaza (el mensaje
     *                               indica el número de lote).
     * @throws RecursoNoEncontradoException Si no existe la planificación o alguno de los equipos indicados.
     */
    List<LoteResponseDTO> confirmarPlan(Long idPlanificacion, PlanProduccionFormDTO planProduccionFormDTO);
}
