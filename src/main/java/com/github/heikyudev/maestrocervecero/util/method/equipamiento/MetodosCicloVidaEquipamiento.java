package com.github.heikyudev.maestrocervecero.util.method.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Validaciones compartidas por las tres operaciones del ciclo de vida de un equipamiento (limpieza, falla y
 * mantenimiento): fecha, orden respecto de la actividad anterior del equipamiento, localización del
 * equipamiento, estado operativo requerido y textos obligatorios.
 * <p>
 * Cada regla tiene una única redacción, usada por las tres operaciones. Los mensajes de los campos informan el campo del
 * formulario afectado.
 * </p>
 */
public final class MetodosCicloVidaEquipamiento {

    /** Nombre del campo del formulario con el equipamiento afectado. */
    public static final String CAMPO_EQUIPAMIENTO = "idEquipamiento";
    /** Nombre del campo del formulario con la fecha y hora de la operación. */
    public static final String CAMPO_FECHA = "fecha";
    /** Nombre del campo del formulario con las observaciones. */
    public static final String CAMPO_OBSERVACIONES = "observaciones";
    /** Nombre del campo del formulario con el motivo de anulación. */
    public static final String CAMPO_MOTIVO_ANULACION = "motivoAnulacion";

    private MetodosCicloVidaEquipamiento() {
    }

    /**
     * Normaliza y valida las observaciones de una operación: son obligatorias y no pueden superar
     * {@link LimitesTecnicos#LONGITUD_MAXIMA_MOTIVO} caracteres.
     *
     * @param observaciones Las observaciones ingresadas.
     * @return Las observaciones normalizadas.
     * @throws ReglaNegocioException Si no fueron informadas o superan el máximo.
     */
    public static String validarObservaciones(String observaciones) {
        String normalizadas = NormalizacionTexto.nombrePropio(observaciones);
        if (normalizadas == null || normalizadas.isEmpty()) {
            throw new ReglaNegocioException("Las observaciones son obligatorias", CAMPO_OBSERVACIONES);
        }
        ValidacionesNumericas.validarLongitudMaxima(normalizadas, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "El texto de las observaciones", CAMPO_OBSERVACIONES);
        return normalizadas;
    }

    /**
     * Normaliza y valida el motivo de una anulación: es obligatorio y no puede superar
     * {@link LimitesTecnicos#LONGITUD_MAXIMA_MOTIVO} caracteres.
     *
     * @param motivoAnulacion El motivo ingresado.
     * @return El motivo normalizado.
     * @throws ReglaNegocioException Si no fue informado o supera el máximo.
     */
    public static String validarMotivoAnulacion(String motivoAnulacion) {
        String normalizado = NormalizacionTexto.nombrePropio(motivoAnulacion);
        if (normalizado == null || normalizado.isEmpty()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio", CAMPO_MOTIVO_ANULACION);
        }
        ValidacionesNumericas.validarLongitudMaxima(normalizado, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "El motivo de anulación", CAMPO_MOTIVO_ANULACION);
        return normalizado;
    }

    /**
     * Valida la fecha y hora de una operación: es obligatoria y no puede ser posterior a la fecha y hora actual.
     *
     * @param fecha Fecha y hora en que se realizó la operación.
     * @throws ReglaNegocioException Si no fue informada o es futura.
     */
    public static void validarFecha(LocalDateTime fecha) {
        if (fecha == null) {
            throw new ReglaNegocioException("La fecha de la operación es obligatoria", CAMPO_FECHA);
        }
        if (fecha.isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha de la operación no puede ser posterior a la fecha y hora actual", CAMPO_FECHA);
        }
    }

    /**
     * Valida que la operación a registrar sea estrictamente posterior a la actividad anterior del equipamiento: la fecha
     * de su última operación registrada y el fin de su última participación en un lote. Dos actividades del mismo
     * equipamiento no pueden ocurrir en el mismo instante ni en orden inverso.
     *
     * @param fecha Fecha y hora de la operación a registrar.
     * @param actividadesAnteriores Fecha de la última operación registrada y fin de la última participación en un lote (cualquiera puede ser {@code null} si no existe).
     * @throws ReglaNegocioException Si alguna fecha dada es posterior o igual a {@code fecha}.
     */
    public static void validarFechaPosteriorALaActividadAnterior(LocalDateTime fecha, LocalDateTime... actividadesAnteriores) {
        for (LocalDateTime anterior : actividadesAnteriores) {
            if (anterior != null && !anterior.isBefore(fecha)) {
                throw new ReglaNegocioException("La fecha de la operación debe ser posterior a la última operación registrada o al último uso en un lote del equipamiento", CAMPO_FECHA);
            }
        }
    }

    /**
     * Valida que la operación a anular sea la última operación registrada del equipamiento. La "última" es la de mayor
     * fecha entre sus limpiezas, fallas y mantenimientos y, ante igual fecha (posible en datos migrados),
     * la de mayor ID.
     *
     * @param idEstaOperacion ID de la operación que se quiere anular.
     * @param idUltimaOperacion ID de la última operación registrada del equipamiento (puede ser esta misma), o {@code null} si no hay ninguna.
     * @throws ReglaNegocioException Si la última operación registrada es otra.
     */
    public static void validarEsLaUltimaOperacion(Long idEstaOperacion, Long idUltimaOperacion) {
        if (idUltimaOperacion != null && !idUltimaOperacion.equals(idEstaOperacion)) {
            throw new ReglaNegocioException("Solo se puede anular la última operación registrada del equipamiento; anule primero las operaciones posteriores");
        }
    }

    /**
     * Indica si una operación puede anularse respecto de la última participación del equipamiento en un lote: la
     * participación en un lote no se anula desde este módulo, así que la operación debe ser estrictamente posterior
     * al fin de esa participación. Si el equipamiento nunca participó en un lote, no hay restricción.
     *
     * @param fechaOperacion Fecha y hora de la operación.
     * @param fechaFinUltimaParticipacion Fin de la última participación del equipamiento en un lote, o {@code null} si nunca participó en uno.
     * @return {@code true} si no hubo participación o si la operación es estrictamente posterior a su fin.
     */
    public static boolean esPosteriorAlUltimoLote(LocalDateTime fechaOperacion, LocalDateTime fechaFinUltimaParticipacion) {
        return fechaFinUltimaParticipacion == null || fechaOperacion.isAfter(fechaFinUltimaParticipacion);
    }

    /**
     * Valida que la operación a anular sea estrictamente posterior a la última participación del equipamiento en un lote.
     *
     * @param fechaOperacion Fecha y hora de la operación a anular.
     * @param fechaFinUltimaParticipacion Fin de la última participación del equipamiento en un lote, o {@code null} si nunca participó en uno.
     * @throws ReglaNegocioException Si la operación no es posterior al fin de la última participación en un lote.
     */
    public static void validarPosteriorAlUltimoLote(LocalDateTime fechaOperacion, LocalDateTime fechaFinUltimaParticipacion) {
        if (!esPosteriorAlUltimoLote(fechaOperacion, fechaFinUltimaParticipacion)) {
            throw new ReglaNegocioException("No se puede anular la operación porque el equipamiento participó en un lote posterior");
        }
    }

    /**
     * Resuelve el equipamiento sobre el que se opera a partir del resultado de su búsqueda con bloqueo, distinguiendo
     * un equipamiento inexistente de uno dado de baja.
     *
     * @param equipamiento El resultado de la búsqueda del equipamiento por ID.
     * @param idEquipamiento El ID buscado, usado en el mensaje.
     * @return El equipamiento, activo.
     * @throws RecursoNoEncontradoException Si el equipamiento no existe.
     * @throws ReglaNegocioException Si el equipamiento está dado de baja.
     */
    public static EquipamientoEntity resolverEquipamientoActivo(Optional<EquipamientoEntity> equipamiento, Long idEquipamiento) {
        EquipamientoEntity entidad = equipamiento
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el equipamiento con ID: " + idEquipamiento, CAMPO_EQUIPAMIENTO));
        if (entidad.getEstado() == Estado.BAJA) {
            throw new ReglaNegocioException("El equipamiento está dado de baja", CAMPO_EQUIPAMIENTO);
        }
        return entidad;
    }

    /**
     * Valida que el equipamiento esté en el estado operativo que exige la operación a registrar y, si no lo está,
     * orienta sobre qué hacer primero. Un equipamiento en uso no admite operaciones manuales.
     *
     * @param estadoActual Estado operativo actual del equipamiento.
     * @param estadoRequerido Estado operativo que exige la operación.
     * @param operacion Nombre de la operación con su artículo, para el mensaje ("una limpieza", "una falla", "un mantenimiento").
     * @throws ReglaNegocioException Si el estado actual no es el requerido.
     */
    public static void validarEstadoOperativoParaRegistrar(EstadoOperativo estadoActual, EstadoOperativo estadoRequerido, String operacion) {
        if (estadoActual == estadoRequerido) {
            return;
        }
        String mensaje = switch (estadoActual) {
            case EN_USO -> "No se puede registrar " + operacion + " porque el equipamiento está en uso";
            case EN_LIMPIEZA -> "Para registrar " + operacion + ", primero registre la limpieza del equipamiento";
            case EN_MANTENIMIENTO -> "Para registrar " + operacion + ", primero registre el mantenimiento del equipamiento";
            case DISPONIBLE -> "No se puede registrar " + operacion + " porque el equipamiento está disponible";
        };
        throw new ReglaNegocioException(mensaje, CAMPO_EQUIPAMIENTO);
    }
}
