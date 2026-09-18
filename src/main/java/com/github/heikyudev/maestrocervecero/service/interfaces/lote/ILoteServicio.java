package com.github.heikyudev.maestrocervecero.service.interfaces.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.CancelacionLoteFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.lote.LoteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios relacionados con la gestión de lotes.
 */
public interface ILoteServicio {

    /**
     * Filtra los lotes registrados, opcionalmente por receta, identificador interno, estado,
     * etapa actualmente en curso y volumen objetivo.
     * <p>
     * {@code identificadorInterno} es el número autogenerado que el sistema le asigna al lote al
     * registrarse (nombre de la receta + número de lote de esa receta), no un dato que el usuario
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
     * automáticamente el identificador interno del lote (nombre de la receta + número de lote de
     * esa receta) y crea sus 6 etapas en estado PENDIENTE, cada una asociada al equipamiento
     * correspondiente (Molino para Molienda, Macerador para Maceración, Olla de Hervor para
     * Hervido, y el mismo Fermentador para Fermentación, Maduración y Envasado).
     * </p>
     *
     * @param loteFormDTO Los datos del lote a registrar.
     * @return El lote registrado.
     * @throws RecursoNoEncontradoException Si la planificación de producción o alguno de los equipamientos referenciados no existe.
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
     *     <li>El equipamiento asociado a la etapa actualmente EN_CURSO pasa a estado
     *     "En Limpieza".</li>
     *     <li>El equipamiento asociado a etapas todavía no ejecutadas pasa a estado "Disponible".</li>
     *     <li>El resto del equipamiento (ya usado en etapas previas) mantiene su estado.</li>
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
     * </ul>
     *
     * @param id El ID del lote cuyo Envasado se quiere finalizar.
     * @return El lote actualizado.
     * @throws RecursoNoEncontradoException Si no existe un lote con el ID especificado, o si no
     *                                      se encuentra el fermentador asociado.
     * @throws ReglaNegocioException Si el lote no se encuentra en estado EN_EJECUCION, o si su
     *                               etapa actual (EN_CURSO) no es Envasado.
     */
    LoteResponseDTO finalizarEnvasado(Long id);
}
