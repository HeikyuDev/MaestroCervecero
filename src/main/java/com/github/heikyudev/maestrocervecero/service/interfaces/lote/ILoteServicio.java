package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.CancelacionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.LoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

/**
 * Interfaz que define los servicios relacionados con la gestión de lotes.
 */
public interface ILoteServicio {

    /**
     * Filtra los lotes registrados, opcionalmente por receta, identificador interno, estado,
     * etapa actualmente en curso y volumen objetivo.
     * <p>
     * {@code identificadorInterno} es el código autogenerado que el sistema le asigna al lote al
     * registrarse (nombre normalizado de la versión de receta + "-R" + id de la receta + "-" + número de lote de esa
     * receta, ej. "PILSEN_CLASICA-R7-3"), no un dato que el usuario
     * elija — la búsqueda es por coincidencia parcial, sin distinguir mayúsculas/minúsculas.
     * </p>
     *
     * @param idReceta El ID de la receta a filtrar, o {@code null} para no filtrar por ella.
     * @param identificadorInterno Texto a buscar dentro del identificador interno, o {@code null} para no filtrar por él.
     * @param estado El estado del lote a filtrar, o {@code null} para no filtrar por él.
     * @param etapaActual El tipo de etapa actualmente en curso a filtrar, o {@code null} para no filtrar por ella.
     * @param volumenObjetivo El volumen objetivo exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable La configuración de paginación.
     * @return Una página de lotes en formato DTO que cumplen los criterios indicados.
     */
    Page<LoteResponseDTO> filtrarLotes(Long idReceta, String identificadorInterno, EstadoLote estado, TipoEtapa etapaActual, Double volumenObjetivo, Pageable pageable);

    /**
     * Obtiene un lote por su ID.
     *
     * @param id El ID del lote.
     * @return El lote correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     */
    LoteResponseDTO buscarPorId(Long id);

    /**
     * Registra un nuevo lote, reservando su lugar en el cronograma de uso de equipamiento.
     * <p>
     * Registrar un lote no es lo mismo que iniciarlo: en este paso no se valida stock de insumos
     * ni disponibilidad de los equipos seleccionados. Al registrarse, el sistema genera
     * automáticamente el identificador interno del lote (nombre normalizado de la versión de receta de la
     * planificación + "-R" + id de la receta + "-" + número de lote de esa receta, ej. "PILSEN_CLASICA-R7-3") y crea sus 6 etapas en estado PENDIENTE, cada una asociada al equipamiento
     * correspondiente (Molino para Molienda, Macerador para Maceración, Olla de Hervor para
     * Hervido, y el mismo Fermentador para Fermentación, Maduración y Envasado).
     * </p>
     * <p>
     * La fecha de inicio estimada del lote es dos días después de la finalización estimada del último
     * lote pendiente o en ejecución de ese fermentador (el día intermedio es el de limpieza; si ese lote
     * está atrasado se cuenta desde hoy), u hoy si no tiene ninguno, pero nunca anterior a la fecha de
     * inicio estimada de la planificación de producción del lote ni a hoy; por eso pueden quedar huecos
     * en la cola del fermentador.
     * </p>
     *
     * @param loteFormDTO Los datos del lote a registrar.
     * @return El lote registrado.
     * @throws RecursoNoEncontradoException Si la planificación de producción o alguno de los equipamientos referenciados no existe.
     * @throws ReglaNegocioException Si la planificación de producción no está en estado PENDIENTE, si el volumen objetivo es
     *                               inválido, si algún equipo seleccionado no tiene capacidad suficiente, o si los lotes de la
     *                               planificación (litros ya producidos por sus lotes finalizados más el volumen objetivo de
     *                               sus lotes pendientes y en ejecución) ya cubren la cantidad a producir.
     */
    LoteResponseDTO registrarLote(LoteFormDTO loteFormDTO);

    /**
     * Inicia un lote previamente registrado, dando paso a su ejecución real.
     * <p>
     * A diferencia de {@link #registrarLote(LoteFormDTO)}, este paso sí valida disponibilidad
     * real: escala todos los insumos de la receta al volumen objetivo del lote, verifica que
     * haya stock suficiente de cada uno y que el equipamiento de cada etapa (Molino, Macerador,
     * Olla de Hervor, Fermentador) esté disponible, y reserva los lotes de insumo necesarios
     * aplicando el criterio FEFO (First Expired, First Out).
     * </p>
     * <p>
     * Cualquier lote PENDIENTE puede iniciarse, sin importar su posición en la cola de su
     * fermentador. Al iniciarlo, sus fechas estimadas pasan a ser las reales: la fecha de inicio
     * estimada es hoy y la de finalización estimada es hoy más la duración con la que se planificó.
     * A continuación, las fechas estimadas de los lotes pendientes del fermentador se reencadenan
     * detrás de la finalización estimada del lote iniciado.
     * </p>
     *
     * @param id El ID del lote a iniciar.
     * @return El lote iniciado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado PENDIENTE, si no hay
     *                               stock suficiente de algún insumo, o si algún equipamiento
     *                               requerido no está disponible.
     */
    LoteResponseDTO iniciarLote(Long id);

    /**
     * Cancela un lote en curso.
     * <p>
     * A diferencia de una anulación, cancelar un lote NO revierte los consumos de insumo ya
     * ejecutados en sus etapas — esos consumos quedan firmes. Lo que sí hace:
     * </p>
     * <ul>
     *     <li>Libera automáticamente todas las reservas de insumo del lote todavía pendientes.</li>
     *     <li>Si el lote estaba "En ejecución", el equipamiento asociado a la etapa actualmente
     *     EN_CURSO pasa a estado "En Limpieza".</li>
     *     <li>Si el lote estaba "En ejecución", el equipamiento asociado a etapas todavía no
     *     ejecutadas pasa a estado "Disponible".</li>
     *     <li>Si el lote estaba "En ejecución", el resto del equipamiento (ya usado en etapas
     *     previas) mantiene su estado.</li>
     *     <li>Si el lote estaba "Pendiente", no se modifica el estado operativo de ningún equipamiento,
     *     porque todavía no ocupó ninguno.</li>
     *     <li>Las fechas estimadas de los lotes pendientes del fermentador del lote se reencadenan:
     *     si el lote estaba "En ejecución", la cola arranca como mínimo el día siguiente a hoy por
     *     la limpieza de sus equipos; si estaba "Pendiente", la cola se compacta desde hoy.</li>
     *     <li>Si la planificación de producción del lote ya había alcanzado su cantidad a producir y
     *     este era su último lote pendiente o en ejecución, la planificación se finaliza sola.</li>
     * </ul>
     *
     * @param id El ID del lote a cancelar.
     * @param cancelacionLoteFormDTO Los datos de la cancelación (motivo).
     * @return El lote cancelado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si el motivo de cancelación no fue informado, o si el lote no
     *                               se encuentra en estado PENDIENTE o EN_EJECUCION.
     */
    LoteResponseDTO cancelarLote(Long id, CancelacionLoteFormDTO cancelacionLoteFormDTO);

    /**
     * Finaliza la etapa de Molienda del lote y da paso a la Maceración.
     * <p>
     * No solicita ningún dato al usuario: el sistema valida y actúa a partir del ID del lote.
     * </p>
     * <ul>
     *     <li>La etapa de Molienda se desmarca como etapa actual (pasa a FINALIZADA) y se registra
     *     su fecha y hora de fin.</li>
     *     <li>La etapa de Maceración se marca como etapa actual (pasa a EN_CURSO) y se registra su
     *     fecha y hora de inicio.</li>
     *     <li>El molino utilizado pasa a estado "En Limpieza".</li>
     * </ul>
     *
     * @param id El ID del lote cuya Molienda se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, o si su
     *                               etapa actual (EN_CURSO) no es Molienda.
     */
    LoteResponseDTO finalizarMolienda(Long id);

    /**
     * Finaliza la etapa de Maceración del lote y da paso al Hervido.
     * <p>
     * No solicita ningún dato al usuario: el sistema valida y actúa a partir del ID del lote.
     * </p>
     * <ul>
     *     <li>Valida que el consumo de cada insumo requerido de la etapa alcance el porcentaje
     *     mínimo de consumo configurado por el gerente de producción — evita avanzar de etapa por
     *     error sin haber registrado consumos, o habiéndolos registrado de forma incompleta.</li>
     *     <li>La etapa de Maceración se desmarca como etapa actual (pasa a FINALIZADA) y se
     *     registra su fecha y hora de fin.</li>
     *     <li>La etapa de Hervido se marca como etapa actual (pasa a EN_CURSO) y se registra su
     *     fecha y hora de inicio.</li>
     *     <li>El macerador utilizado pasa a estado "En Limpieza".</li>
     *     <li>Se liberan las reservas de insumo que hayan quedado sin consumir de la etapa de
     *     Maceración.</li>
     * </ul>
     *
     * @param id El ID del lote cuya Maceración se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si su etapa
     *                               actual (EN_CURSO) no es Maceración, o si algún insumo
     *                               requerido de la etapa no alcanzó el porcentaje mínimo de
     *                               consumo configurado.
     */
    LoteResponseDTO finalizarMaceracion(Long id);

    /**
     * Finaliza la etapa de Hervido del lote y da paso a la Fermentación.
     * <p>
     * No solicita ningún dato al usuario: el sistema valida y actúa a partir del ID del lote.
     * </p>
     * <ul>
     *     <li>Valida que el consumo de cada insumo requerido de la etapa alcance el porcentaje
     *     mínimo de consumo configurado por el gerente de producción — evita avanzar de etapa por
     *     error sin haber registrado consumos, o habiéndolos registrado de forma incompleta.</li>
     *     <li>La etapa de Hervido se desmarca como etapa actual (pasa a FINALIZADA) y se registra
     *     su fecha y hora de fin.</li>
     *     <li>La etapa de Fermentación se marca como etapa actual (pasa a EN_CURSO) y se registra
     *     su fecha y hora de inicio.</li>
     *     <li>La olla de hervor utilizada pasa a estado "En Limpieza".</li>
     *     <li>Se liberan las reservas de insumo que hayan quedado sin consumir de la etapa de
     *     Hervido.</li>
     * </ul>
     *
     * @param id El ID del lote cuyo Hervido se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si su etapa
     *                               actual (EN_CURSO) no es Hervido, o si algún insumo requerido
     *                               de la etapa no alcanzó el porcentaje mínimo de consumo
     *                               configurado.
     */
    LoteResponseDTO finalizarHervido(Long id);

    /**
     * Finaliza la etapa de Fermentación del lote y da paso a la Maduración.
     * <p>
     * No solicita ningún dato al usuario: el sistema valida y actúa a partir del ID del lote.
     * A diferencia de las demás transiciones de etapa, no cambia el estado operativo de ningún
     * equipamiento: el fermentador es compartido por Fermentación, Maduración y Envasado, así que
     * sigue "En Uso" sin interrupción hasta que termine la última de esas tres etapas.
     * </p>
     * <ul>
     *     <li>Valida que el consumo de cada insumo requerido de la etapa alcance el porcentaje
     *     mínimo de consumo configurado por el gerente de producción — evita avanzar de etapa por
     *     error sin haber registrado consumos, o habiéndolos registrado de forma incompleta.</li>
     *     <li>La etapa de Fermentación se desmarca como etapa actual (pasa a FINALIZADA) y se
     *     registra su fecha y hora de fin.</li>
     *     <li>La etapa de Maduración se marca como etapa actual (pasa a EN_CURSO) y se registra su
     *     fecha y hora de inicio.</li>
     *     <li>Se liberan las reservas de insumo que hayan quedado sin consumir de la etapa de
     *     Fermentación.</li>
     * </ul>
     *
     * @param id El ID del lote cuya Fermentación se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si su etapa
     *                               actual (EN_CURSO) no es Fermentación, o si algún insumo
     *                               requerido de la etapa no alcanzó el porcentaje mínimo de
     *                               consumo configurado.
     */
    LoteResponseDTO finalizarFermentacion(Long id);

    /**
     * Finaliza la etapa de Maduración del lote y da paso al Envasado.
     * <p>
     * No solicita ningún dato al usuario: el sistema valida y actúa a partir del ID del lote.
     * Igual que {@code finalizarFermentacion}, no cambia el estado operativo de ningún
     * equipamiento: el fermentador es compartido por Fermentación, Maduración y Envasado, así que
     * sigue "En Uso" sin interrupción hasta que termine el Envasado.
     * </p>
     * <ul>
     *     <li>Valida que el consumo de cada insumo requerido de la etapa alcance el porcentaje
     *     mínimo de consumo configurado por el gerente de producción (infrecuente que Maduración
     *     requiera insumos, pero puede haberlos, por ejemplo un lúpulo de Dry Hop).</li>
     *     <li>La etapa de Maduración se desmarca como etapa actual (pasa a FINALIZADA) y se
     *     registra su fecha y hora de fin.</li>
     *     <li>La etapa de Envasado se marca como etapa actual (pasa a EN_CURSO) y se registra su
     *     fecha y hora de inicio.</li>
     *     <li>Se liberan las reservas de insumo que hayan quedado sin consumir de la etapa de
     *     Maduración.</li>
     * </ul>
     *
     * @param id El ID del lote cuya Maduración se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra la configuración de producción.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si su etapa
     *                               actual (EN_CURSO) no es Maduración, o si algún insumo
     *                               requerido de la etapa no alcanzó el porcentaje mínimo de
     *                               consumo configurado.
     */
    LoteResponseDTO finalizarMaduracion(Long id);

    /**
     * Finaliza la etapa de Envasado del lote, dando por concluido el proceso productivo completo.
     * <p>
     * No solicita ningún dato al usuario: el sistema valida y actúa a partir del ID del lote. A
     * diferencia de las demás transiciones de etapa, Envasado es la última: no hay una etapa
     * siguiente a la que avanzar, y en ella no se registran consumos de insumo (solo envasados,
     * el traspaso de cerveza del fermentador a los barriles), por lo que no aplica la validación
     * de porcentaje mínimo de consumo.
     * </p>
     * <ul>
     *     <li>La etapa de Envasado se desmarca como etapa actual (pasa a FINALIZADA) y se registra
     *     su fecha y hora de fin.</li>
     *     <li>El lote pasa a estado "Finalizado".</li>
     *     <li>El fermentador utilizado (compartido por Fermentación, Maduración y Envasado) pasa a
     *     estado "En Limpieza".</li>
     *     <li>Las fechas estimadas de los lotes pendientes del fermentador se reencadenan, con
     *     inicio mínimo el día siguiente a hoy por la limpieza de los equipos.</li>
     *     <li>Si con este lote los litros producidos de la planificación de producción (envasados
     *     registrados de sus lotes finalizados) alcanzan la cantidad a producir y la planificación no
     *     tiene más lotes pendientes ni en ejecución, la planificación se finaliza sola.</li>
     * </ul>
     * <p>
     * Un lote sin litros envasados registrados no puede finalizarse: si no se va a producir, se cancela.
     * </p>
     *
     * @param id El ID del lote cuyo Envasado se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra el fermentador asociado.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, si su
     *                               etapa actual (EN_CURSO) no es Envasado, o si no tiene litros
     *                               envasados registrados.
     */
    LoteResponseDTO finalizarEnvasado(Long id);

    /**
     * Lista los lotes reales de una planificación de producción, en una versión liviana pensada
     * para el detalle de la planificación y el cronograma.
     * <p>
     * Incluye los lotes en cualquier estado (pendientes, en ejecución, finalizados y cancelados),
     * ordenados por fecha de inicio estimada ascendente y, ante igual fecha, por ID ascendente.
     * </p>
     *
     * @param idPlanificacionProduccion El ID de la planificación de producción cuyos lotes se quieren listar.
     * @return Los lotes de la planificación en formato DTO de cronograma, o una lista vacía si todavía no tiene lotes.
     * @throws RecursoNoEncontradoException Si no existe una planificación de producción con el ID especificado.
     */
    List<LoteCronogramaResponseDTO> listarLotesDePlanificacion(Long idPlanificacionProduccion);

    /**
     * Lista los lotes de todas las planificaciones de producción para el cronograma global de un período, que se
     * organiza por fermentador: en el carril de cada uno conviven,
     * intercalados, los lotes de todas las planificaciones que lo usan.
     * <p>
     * Reglas:
     * </p>
     * <ul>
     *     <li>El período es obligatorio y cerrado: {@code desde} y {@code hasta} son inclusivos, {@code hasta} no puede
     *     ser anterior a {@code desde} y el período no puede abarcar más de 1.826 días contando ambos extremos (una
     *     planificación de 5 años, con 1.825 días entre sus fechas, entra completa).</li>
     *     <li>Siempre incluye todos los lotes EN_EJECUCION y PENDIENTE de los fermentadores pedidos, sin mirar el
     *     período: el atraso y el corrimiento dependen de la cola completa de cada fermentador. Qué barras caen dentro del
     *     período lo resuelve el diagrama.</li>
     *     <li>Los lotes FINALIZADO se incluyen solo si su vida real (del inicio a la finalización) se solapa con el período.</li>
     *     <li>Los lotes CANCELADO nunca se incluyen: el cronograma no los dibuja.</li>
     *     <li>El fermentador de un lote se toma sin mirar su estado: los lotes de un fermentador dado de baja se
     *     incluyen igual, para que un período histórico muestre los fermentadores que ya no están activos.</li>
     *     <li>El orden es por identificador interno del fermentador, luego por fecha de inicio
     *     estimada y, ante igual fecha, por ID. El orden de la cola de un fermentador es, entonces,
     *     el de la fecha de inicio estimada de sus lotes (no existe un campo de orden aparte). Como
     *     ningún lote arranca antes de la fecha de inicio estimada de su planificación ni antes de
     *     hoy, la cola puede tener huecos entre lotes.</li>
     *     <li>No incluye operaciones de equipamiento (limpieza, mantenimiento, falla).</li>
     *     <li>Los carriles de fermentadores sin lotes no se devuelven: completarlos es tarea del
     *     Controller.</li>
     *     <li>Se resuelve con una sola consulta de lotes, sin consultas por fermentador ni por lote.</li>
     * </ul>
     *
     * @param idFermentador El ID del fermentador cuyo carril se quiere listar (activo o dado de baja), o {@code null} para listar todos.
     * @param desde El primer día del período (inclusive).
     * @param hasta El último día del período (inclusive).
     * @return Los lotes en formato DTO de cronograma, o una lista vacía si no hay ninguno.
     * @throws ReglaNegocioException Si {@code desde} o {@code hasta} son nulos, si {@code hasta} es anterior a {@code desde} o si el período supera los 1.826 días contando ambos extremos.
     * @throws RecursoNoEncontradoException Si se informa un {@code idFermentador} que no existe.
     */
    List<LoteCronogramaResponseDTO> listarLotesParaCronograma(Long idFermentador, LocalDate desde, LocalDate hasta);

    /**
     * Sube un lote una posición en la cola de lotes PENDIENTE de su fermentador (lo adelanta), y
     * reencadena las fechas de toda la cola.
     * <p>
     * La cola está formada por los lotes PENDIENTE del fermentador del lote, de cualquier
     * planificación, ordenados por fecha de inicio estimada y, ante igual fecha, por ID. Tras el
     * intercambio se reencadenan las fechas de todos los lotes PENDIENTE de ese fermentador en el
     * nuevo orden:
     * </p>
     * <ul>
     *     <li>Cada lote conserva su propia duración (fecha de finalización estimada menos fecha de
     *     inicio estimada, en días, tal como están guardadas).</li>
     *     <li>El primer lote pendiente arranca a partir de dos días después de la fecha de finalización
     *     estimada del lote EN_EJECUCION de ese fermentador (o de hoy, si está atrasado), o de hoy si no hay
     *     ninguno (misma regla que usa {@link #registrarLote(LoteFormDTO)}).</li>
     *     <li>Cada lote pendiente siguiente arranca a partir de dos días después de que termina el
     *     anterior (el día intermedio es la limpieza del fermentador).</li>
     *     <li>Ningún lote arranca antes de la fecha de inicio estimada de su planificación de
     *     producción ni antes de hoy: la fecha de inicio de cada lote es el máximo entre esas tres
     *     fechas. Por eso pueden quedar huecos en la cola.</li>
     *     <li>Los lotes EN_EJECUCION, FINALIZADO y CANCELADO nunca se mueven.</li>
     * </ul>
     * <p>
     * Ejemplo: con la cola A (en ejecución), B, C, D, subir D deja A, B, D, C: D arranca donde
     * arrancaba C, y C se corre hacia la derecha detrás de D, cada uno con su duración. Si D
     * pertenece a una planificación que recién inicia dentro de 10 días, D conserva como mínimo
     * esa fecha de inicio aunque el lote anterior termine antes, y queda un hueco en la cola.
     * </p>
     * <p>
     * La operación es transaccional y bloquea los lotes pendientes del fermentador para evitar
     * reordenamientos concurrentes. Se audita como una modificación del lote. El mismo
     * reencadenado se aplica cuando un lote del fermentador se inicia, finaliza o cancela; si el lote
     * que finaliza o se cancela estaba "En ejecución", ningún lote pendiente arranca antes del día
     * siguiente a hoy.
     * </p>
     *
     * @param idLote El ID del lote a subir.
     * @return El lote movido, con sus nuevas fechas estimadas.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado PENDIENTE ("Solo se pueden mover lotes en estado Pendiente"),
     *                               o si es el primer lote pendiente de la cola de su fermentador.
     */
    LoteCronogramaResponseDTO subirLoteEnCola(Long idLote);

    /**
     * Baja un lote una posición en la cola de lotes PENDIENTE de su fermentador (lo retrasa), y
     * reencadena las fechas de toda la cola.
     * <p>
     * Es la operación inversa de {@link #subirLoteEnCola(Long)} y comparte con ella las mismas
     * reglas de cola, reencadenado de fechas (duración propia de cada lote, primer pendiente
     * detrás del lote EN_EJECUCION u hoy, dos días de separación entre lotes, y piso en la fecha de
     * inicio estimada de la planificación de cada lote y en hoy), bloqueo y auditoría.
     * </p>
     * <p>
     * Ejemplo: con la cola A (en ejecución), B, C, D, bajar B deja A, C, B, D: C arranca donde
     * arrancaba B, y B se corre hacia la derecha detrás de C, cada uno con su duración. Si C
     * pertenece a una planificación que recién inicia dentro de 10 días, C no arranca antes de esa
     * fecha (queda un hueco) y B se encadena detrás de C.
     * </p>
     *
     * @param idLote El ID del lote a bajar.
     * @return El lote movido, con sus nuevas fechas estimadas.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado PENDIENTE ("Solo se pueden mover lotes en estado Pendiente"),
     *                               o si es el último lote pendiente de la cola de su fermentador.
     */
    LoteCronogramaResponseDTO bajarLoteEnCola(Long idLote);
}
