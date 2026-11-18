package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Fórmulas puras (sin acceso a datos) con las que se estima un lote antes de registrarlo: los volúmenes del lado
 * caliente que se comparan contra la capacidad útil de la olla de hervor y del macerador, y las fechas de inicio y de
 * finalización estimadas en el cronograma de su fermentador.
 * <p>
 * Son la única fuente de estos cálculos: las usan el registro de un lote y la generación automática de planes de
 * producción, de modo que un plan propone exactamente las fechas y los límites de capacidad que el registro aplica.
 * </p>
 */
public final class EstimacionLote {

    /**
     * Separación, en días, entre dos lotes consecutivos de un fermentador: el día fin + 1 es el de limpieza y el
     * siguiente lote arranca como mínimo el día fin + 2.
     */
    public static final int DIAS_MARGEN_LIMPIEZA = 2;

    /** Estados de lote que todavía ocupan un lugar en el cronograma de un fermentador. */
    public static final List<EstadoLote> ESTADOS_QUE_OCUPAN_FERMENTADOR = List.of(EstadoLote.PENDIENTE, EstadoLote.EN_EJECUCION);

    private static final int HORAS_POR_DIA = 24;
    private static final double MINUTOS_POR_HORA = 60.0;

    private EstimacionLote() {
    }

    /**
     * Calcula el volumen pre-hervor de un lote (Paso 2 del escalado de agua), redondeado a la escala de las cantidades:
     * es el valor que se compara contra la capacidad útil de la olla de hervor.
     */
    public static double calcularVolumenPreHervor(double volumenObjetivo, OllaHervorEntity ollaHervor, VersionRecetaEntity versionReceta) {
        return Redondeo.redondear(EscaladoAgua.calcularVolumenPreHervor(volumenObjetivo, ollaHervor, versionReceta));
    }

    /**
     * Calcula el volumen de la mezcla de agua y malta en el macerador (Paso 3 del escalado de agua), redondeado a la
     * escala de las cantidades: es el valor que se compara contra la capacidad útil del macerador.
     */
    public static double calcularVolumenMezclaMacerador(double masaMaltaEscalada, MaceradorEntity macerador, VersionRecetaEntity versionReceta) {
        return Redondeo.redondear(EscaladoAgua.calcularVolumenMezclaMacerador(masaMaltaEscalada, macerador, versionReceta));
    }

    /**
     * Indica si la olla de hervor admite el volumen pre-hervor de un lote del volumen indicado.
     */
    public static boolean ollaAdmiteVolumen(double volumenObjetivo, OllaHervorEntity ollaHervor, VersionRecetaEntity versionReceta) {
        return !superaCapacidad(calcularVolumenPreHervor(volumenObjetivo, ollaHervor, versionReceta), ollaHervor.getCapacidadUtil());
    }

    /**
     * Indica si el macerador admite la mezcla de agua y malta de un lote con la masa de malta indicada.
     */
    public static boolean maceradorAdmiteMasaMalta(double masaMaltaEscalada, MaceradorEntity macerador, VersionRecetaEntity versionReceta) {
        return !superaCapacidad(calcularVolumenMezclaMacerador(masaMaltaEscalada, macerador, versionReceta), macerador.getCapacidadUtil());
    }

    /**
     * Indica si un volumen ya redondeado a la escala de las cantidades supera la capacidad útil de un equipo (también
     * redondeada). Es la comparación de todos los límites de capacidad del registro de un lote.
     */
    public static boolean superaCapacidad(double volumenRedondeado, double capacidadUtil) {
        return volumenRedondeado > Redondeo.redondear(capacidadUtil);
    }

    /**
     * Primer día en que un fermentador puede arrancar otro lote después del lote que termina en {@code fin}: dos días
     * después (el intermedio es el de limpieza) o, si ese lote está atrasado, dos días después de hoy.
     */
    public static LocalDate calcularInicioTrasLote(LocalDate fin, LocalDate hoy) {
        return (fin.isBefore(hoy) ? hoy : fin).plusDays(DIAS_MARGEN_LIMPIEZA);
    }

    /**
     * Calcula la fecha de inicio estimada de un lote encolado detrás del último lote vivo de su fermentador: dos días
     * después de la fecha de finalización estimada de ese lote (si está atrasado se cuenta desde hoy), u hoy si el
     * fermentador no tiene lotes vivos, pero nunca antes de la fecha de inicio estimada de la planificación ni de hoy.
     *
     * @param finUltimoLoteVivo Fecha de finalización estimada del último lote vivo del fermentador, o {@code null} si no tiene.
     * @param inicioPlanificacion Fecha de inicio estimada de la planificación de producción del lote.
     * @param hoy La fecha actual.
     * @return {@code max(max(fin del último lote, hoy) + 2 días, inicio de la planificación, hoy)}, o
     * {@code max(inicio de la planificación, hoy)} si no hay lotes vivos.
     */
    public static LocalDate calcularFechaInicioEstimada(LocalDate finUltimoLoteVivo, LocalDate inicioPlanificacion, LocalDate hoy) {
        LocalDate fechaPorCola = finUltimoLoteVivo == null ? hoy : calcularInicioTrasLote(finUltimoLoteVivo, hoy);
        return Collections.max(List.of(fechaPorCola, inicioPlanificacion, hoy));
    }

    /**
     * Calcula la fecha de finalización estimada de un lote sumando, sobre su fecha de inicio estimada, la duración
     * total del proceso: el día de brewing (molienda + maceración + hervido) más el envasado, redondeados hacia arriba
     * a días completos, más la fermentación y la maduración de la receta.
     *
     * @param fechaInicioEstimada Fecha de inicio estimada del lote.
     * @param volumenObjetivo Volumen objetivo del lote, en litros.
     * @param rendimientoMolienda Rendimiento del molino, en kg/h.
     * @param masaMaltaEscalada Masa total de malta escalada del lote, en kg.
     * @param versionReceta Versión de receta del lote.
     * @param velocidadEstandarEnvasado Velocidad estándar de envasado de la configuración de producción, en L/h.
     * @return La fecha de finalización estimada.
     */
    public static LocalDate calcularFechaFinalizacionEstimada(LocalDate fechaInicioEstimada, double volumenObjetivo, double rendimientoMolienda,
                                                              double masaMaltaEscalada, VersionRecetaEntity versionReceta, double velocidadEstandarEnvasado) {
        double horasMolienda = masaMaltaEscalada / rendimientoMolienda;
        double horasMaceracionYHervido = (versionReceta.getDuracionMaceracion() + versionReceta.getDuracionHervido()) / MINUTOS_POR_HORA;
        double horasEnvasado = volumenObjetivo / velocidadEstandarEnvasado;

        long diasBrewDayYEnvasado = (long) Math.ceil((horasMolienda + horasMaceracionYHervido + horasEnvasado) / HORAS_POR_DIA);
        long diasTotales = diasBrewDayYEnvasado + versionReceta.getDuracionFermentacion() + versionReceta.getDuracionMaduracion();

        return fechaInicioEstimada.plusDays(diasTotales);
    }
}
