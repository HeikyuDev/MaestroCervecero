package com.github.heikyudev.maestrocervecero.util.method;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Validaciones compartidas por las seis operaciones del ciclo de vida de un barril (limpieza, falla, mantenimiento,
 * fraccionamiento, despacho y devolución): textos obligatorios, orden temporal respecto de la actividad anterior del
 * barril, localización del barril y reglas para anular una operación.
 * <p>
 * La "actividad" de un barril son sus operaciones registradas y su último envasado registrado. Cada regla tiene una
 * única redacción, usada por las seis operaciones. Los mensajes de los campos informan el campo del formulario afectado.
 * </p>
 */
public class MetodosCicloVida {

    /** Formato con el que los mensajes muestran una fecha y hora. */
    private static final DateTimeFormatter FORMATO_FECHA_HORA_SEGUNDOS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Nombre del campo del formulario con el barril afectado. */
    public static final String CAMPO_BARRIL = "idBarril";
    /** Nombre del campo del formulario con la fecha y hora de la operación. */
    public static final String CAMPO_FECHA = "fecha";
    /** Nombre del campo del formulario con las observaciones. */
    public static final String CAMPO_OBSERVACIONES = "observaciones";
    /** Nombre del campo del formulario con el motivo de anulación. */
    public static final String CAMPO_MOTIVO_ANULACION = "motivoAnulacion";

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
        if (normalizadas.length() > LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO) {
            throw new ReglaNegocioException("Las observaciones no pueden superar " + LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO + " caracteres", CAMPO_OBSERVACIONES);
        }
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
     * Valida que la operación a anular sea la última operación registrada del barril. La "última" es la de mayor
     * fecha entre sus limpiezas, fallas, mantenimientos, fraccionamientos, despachos y devoluciones y, ante igual
     * fecha (posible en datos migrados), la de mayor ID.
     *
     * @param idEstaOperacion ID de la operación que se quiere anular.
     * @param idUltimaOperacion ID de la última operación registrada del barril (puede ser esta misma). No puede ser {@code null}: la operación a anular está registrada, así que el barril tiene al menos una.
     * @throws ReglaNegocioException Si la última operación registrada es otra.
     * @throws IllegalStateException Si no se encontró ninguna operación registrada (falla cerrada: es una inconsistencia de datos, no una regla de negocio).
     */
    public static void validarEsLaUltimaOperacion(Long idEstaOperacion, Long idUltimaOperacion) {
        if (idUltimaOperacion == null) {
            throw new IllegalStateException("No se encontró ninguna operación registrada para el barril, pero se intenta anular la operación con ID: " + idEstaOperacion);
        }
        if (!idUltimaOperacion.equals(idEstaOperacion)) {
            throw new ReglaNegocioException("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");
        }
    }

    /**
     * Indica si una operación puede anularse respecto del último envasado del barril: el envasado no se anula desde
     * este módulo, así que la operación debe ser estrictamente posterior a su fecha. Si el barril nunca fue
     * envasado, no hay restricción.
     *
     * @param fechaOperacion Fecha y hora de la operación.
     * @param fechaUltimoEnvasado Fecha del último envasado registrado del barril, o {@code null} si nunca fue envasado.
     * @return {@code true} si no hubo envasado o si la operación es estrictamente posterior a su fecha.
     */
    public static boolean esPosteriorAlUltimoEnvasado(LocalDateTime fechaOperacion, LocalDateTime fechaUltimoEnvasado) {
        return fechaUltimoEnvasado == null || fechaOperacion.isAfter(fechaUltimoEnvasado);
    }

    /**
     * Valida que la operación a anular sea estrictamente posterior al último envasado del barril.
     *
     * @param fechaOperacion Fecha y hora de la operación a anular.
     * @param fechaUltimoEnvasado Fecha del último envasado registrado del barril, o {@code null} si nunca fue envasado.
     * @throws ReglaNegocioException Si la operación no es posterior al último envasado.
     */
    public static void validarPosteriorAlUltimoEnvasado(LocalDateTime fechaOperacion, LocalDateTime fechaUltimoEnvasado) {
        if (!esPosteriorAlUltimoEnvasado(fechaOperacion, fechaUltimoEnvasado)) {
            throw new ReglaNegocioException("No se puede anular la operación porque el barril tuvo un envasado posterior");
        }
    }

    /**
     * Valida que la fecha de una operación a registrar sea estrictamente posterior a la finalización del lote cuyo
     * envasado dejó la cerveza en el barril: hasta que el lote finaliza el barril está en uso, así que no puede
     * despacharse ni fraccionarse antes. Si el barril nunca fue envasado o el lote no tiene fecha de finalización,
     * no hay restricción.
     *
     * @param fechaOperacion Fecha y hora de la operación que se quiere registrar.
     * @param fechaFinalizacionLote Fecha y hora de finalización del lote del último envasado registrado del barril, o {@code null} si no existe.
     * @param campo Nombre del campo del formulario que contiene la fecha, o {@code null}.
     * @throws ReglaNegocioException Si la fecha no es posterior a la finalización del lote.
     */
    public static void validarPosteriorAFinalizacionDelLote(LocalDateTime fechaOperacion, LocalDateTime fechaFinalizacionLote, String campo) {
        if (fechaFinalizacionLote != null && !fechaOperacion.isAfter(fechaFinalizacionLote)) {
            throw new ReglaNegocioException("La fecha de la operación debe ser posterior a la finalización del lote que envasó este barril ("
                    + FORMATO_FECHA_HORA_SEGUNDOS.format(fechaFinalizacionLote) + ")", campo);
        }
    }

    /**
     * Indica si el barril está en uso por un lote. Un barril en uso no admite registrar ni anular operaciones, porque
     * podría quedar con un estado operativo inconsistente.
     *
     * @param estadoOperativo Estado operativo actual del barril.
     * @return {@code true} si el estado operativo es {@code EN_USO}.
     */
    public static boolean estaEnUso(EstadoOperativoBarril estadoOperativo) {
        return estadoOperativo == EstadoOperativoBarril.EN_USO;
    }

    /**
     * Valida que el barril de la operación a anular no esté en uso por un lote.
     *
     * @param estadoOperativo Estado operativo actual del barril.
     * @throws ReglaNegocioException Si el barril está en uso.
     */
    public static void validarNoEstaEnUso(EstadoOperativoBarril estadoOperativo) {
        if (estaEnUso(estadoOperativo)) {
            throw new ReglaNegocioException("No se puede anular la operación porque el barril está en uso por un lote");
        }
    }

    /**
     * Resuelve el barril sobre el que se opera a partir del resultado de su búsqueda con bloqueo. La búsqueda solo
     * encuentra barriles activos: uno dado de baja se informa como inexistente.
     *
     * @param barril El resultado de la búsqueda del barril por ID.
     * @param idBarril El ID buscado, usado en el mensaje.
     * @return El barril, activo.
     * @throws RecursoNoEncontradoException Si el barril no existe o está dado de baja.
     */
    public static BarrilEntity resolverBarril(Optional<BarrilEntity> barril, Long idBarril) {
        return barril.orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el barril con ID: " + idBarril, CAMPO_BARRIL));
    }

    /**
     * Valida que la fecha de una operación del ciclo de vida a registrar sea posterior a la de
     * todas las demás operaciones ya registradas sobre su barril, sin permitir tampoco que
     * coincida exactamente con ninguna. Evita cargar una operación con fecha retroactiva (o
     * ambigua, a la misma fecha y hora) a algo que ya pasó sobre ese barril: dos operaciones
     * distintas no pueden haber ocurrido en el mismo instante.
     * <p>
     * Se puede exigir "posterior o igual" sin riesgo de autorrechazo: la operación que se está
     * registrando todavía no existe en ninguna de las tablas consultadas, así que nunca se compara
     * contra sí misma.
     * </p>
     *
     * @param fechaNuevaOperacion Fecha de la operación que se quiere registrar.
     * @param nombreEntidad Nombre de la entidad asociada ("barril"), usado en el mensaje de error.
     * @param fechasOperacionesExistentes Fecha de la última actividad registrada de cada fuente consultada (cualquiera puede ser {@code null} si no existe).
     * @throws ReglaNegocioException Si alguna de las fechas dadas es posterior o igual a {@code fechaNuevaOperacion}.
     */
    public static void validarFechaPosteriorAUltimaOperacion(LocalDateTime fechaNuevaOperacion, String nombreEntidad, LocalDateTime... fechasOperacionesExistentes) {
        validarFechaPosteriorAUltimaOperacion(fechaNuevaOperacion, nombreEntidad, null, fechasOperacionesExistentes);
    }

    /**
     * Igual que {@link #validarFechaPosteriorAUltimaOperacion(LocalDateTime, String, LocalDateTime...)}, pero el
     * rechazo indica el campo del formulario al que se refiere la fecha, para que la capa web pueda marcarlo.
     *
     * @param fechaNuevaOperacion Fecha de la operación que se quiere registrar.
     * @param nombreEntidad Nombre de la entidad asociada ("barril"), usado en el mensaje de error.
     * @param campo Nombre del campo del formulario que contiene la fecha, o {@code null}.
     * @param fechasOperacionesExistentes Fecha de la última actividad registrada de cada fuente consultada (cualquiera puede ser {@code null} si no existe).
     * @throws ReglaNegocioException Si alguna de las fechas dadas es posterior o igual a {@code fechaNuevaOperacion}.
     */
    public static void validarFechaPosteriorAUltimaOperacion(LocalDateTime fechaNuevaOperacion, String nombreEntidad, String campo, LocalDateTime... fechasOperacionesExistentes) {
        if (hayFechaPosteriorOIgual(fechaNuevaOperacion, fechasOperacionesExistentes)) {
            throw new ReglaNegocioException("La fecha de la operación debe ser posterior a la última operación registrada sobre este " + nombreEntidad, campo);
        }
    }

    /**
     * Valida que la fecha de una operación del ciclo de vida a registrar no sea posterior a la
     * fecha y hora actual.
     * <p>
     * Una fecha futura envenenaría {@link #validarFechaPosteriorAUltimaOperacion}: quedaría como
     * "la última operación registrada" sobre ese barril, y bloquearía cualquier operación real
     * posterior hasta que, en el mundo real, se alcance esa fecha futura.
     * </p>
     *
     * @param fechaNuevaOperacion Fecha de la operación que se quiere registrar.
     * @throws ReglaNegocioException Si la fecha es posterior a la fecha y hora actual.
     */
    public static void validarFechaNoFutura(LocalDateTime fechaNuevaOperacion) {
        validarFechaNoFutura(fechaNuevaOperacion, null);
    }

    /**
     * Igual que {@link #validarFechaNoFutura(LocalDateTime)}, pero el rechazo indica el campo del formulario al que se
     * refiere la fecha, para que la capa web pueda marcarlo.
     *
     * @param fechaNuevaOperacion Fecha de la operación que se quiere registrar.
     * @param campo Nombre del campo del formulario que contiene la fecha, o {@code null}.
     * @throws ReglaNegocioException Si la fecha es posterior a la fecha y hora actual.
     */
    public static void validarFechaNoFutura(LocalDateTime fechaNuevaOperacion, String campo) {
        if (fechaNuevaOperacion.isAfter(LocalDateTime.now())) {
            throw new ReglaNegocioException("La fecha de la operación no puede ser posterior a la fecha y hora actual", campo);
        }
    }

    private static boolean hayFechaPosteriorOIgual(LocalDateTime fechaReferencia, LocalDateTime... fechas) {
        for (LocalDateTime fecha : fechas) {
            if (fecha != null && !fecha.isBefore(fechaReferencia)) {
                return true;
            }
        }
        return false;
    }
}
