package com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReporteEstadisticoResponseDTO;
import org.springframework.data.domain.Pageable;

/**
 * Estadísticas de producción. Son consultas de solo lectura: ningún método modifica datos. Cada reporte es un método
 * que valida el período con {@link PeriodoEstadistica#validar} y devuelve la estructura única
 * {@link ReporteEstadisticoResponseDTO}.
 */
public interface IEstadisticaProduccionServicio {

    /**
     * Genera el reporte "Lotes": cuántos lotes se planificaron en el período y cómo terminaron. El universo son los
     * lotes cuya fecha de inicio estimada cae en el período (ambos extremos inclusive), en cualquier estado.
     * <p>
     * Precondiciones: el período de los criterios tiene sus dos extremos, la fecha hasta no es anterior a la fecha
     * desde y el período no supera el rango máximo de las estadísticas.
     * </p>
     * <p>
     * Postcondiciones: devuelve los criterios aplicados (receta y estado, con "Todas" o "Todos" si no se indicaron);
     * cuatro indicadores: los lotes del período, los finalizados (con su porcentaje sobre el total), los litros
     * envasados (envasados registrados; los anulados no cuentan) y los terminados en plazo (porcentaje de los
     * finalizados cuya fecha de fin real no es posterior a la fecha de fin estimada, con un guion si no hay
     * finalizados); el gráfico de lotes por estado con los cuatro estados (cero donde no hay); y la página pedida de la
     * tabla (lote con su receta, estado, litros envasados, fin estimado, fin real y "Abrir"), del inicio estimado más
     * reciente al más antiguo y, ante igual fecha, del último registrado al primero, con la fila de totales de los
     * litros envasados del período. No modifica ningún dato.
     * </p>
     *
     * @param criterios Criterios del reporte.
     * @param pageable Página de la tabla; el orden lo define el reporte.
     * @return El reporte.
     * @throws ReglaNegocioException Si el período no es válido.
     */
    ReporteEstadisticoResponseDTO generarReporteDeLotes(CriteriosReporteLotes criterios, Pageable pageable);

    /**
     * Genera el reporte "Costos": cuánto costó producir cada litro en el período. El universo son los lotes cuya etapa de
     * Envasado está finalizada con fecha de finalización en el período y que tienen litros envasados registrados mayores
     * que cero.
     * <p>
     * Precondiciones: las mismas del período que en {@link #generarReporteDeLotes}.
     * </p>
     * <p>
     * Postcondiciones: el costo de un lote es el de sus consumos registrados (cantidad por costo unitario congelado al
     * consumir) más sus costos adicionales (congelados al finalizar el envasado); no incluye costos fijos. Devuelve el
     * criterio Receta; cuatro indicadores: costo total, litros envasados, costo por litro (costo total sobre litros) y
     * peso de los insumos (su porcentaje sobre el costo total; un guion si el costo total es cero); el gráfico del costo
     * por litro por receta (las ocho más caras, sin sumar el resto); y la página pedida de la tabla (lote con su receta,
     * fin de envasado, litros, costo de insumos, costo total y costo por litro), del fin de envasado más reciente al más
     * antiguo, con la fila de totales y el costo por litro ponderado. No modifica ningún dato.
     * </p>
     *
     * @param criterios Criterios del reporte.
     * @param pageable Página de la tabla.
     * @return El reporte.
     * @throws ReglaNegocioException Si el período no es válido.
     */
    ReporteEstadisticoResponseDTO generarReporteDeCostos(CriteriosReporteCostos criterios, Pageable pageable);

    /**
     * Genera el reporte "Tiempos": cuánto duran las etapas frente a lo estimado. El universo son las etapas
     * {@code FINALIZADA} con fecha de finalización en el período.
     * <p>
     * Precondiciones: las mismas del período que en {@link #generarReporteDeLotes}.
     * </p>
     * <p>
     * Postcondiciones: la duración real es del inicio al fin de la etapa; la estimada es la de la receta (Maceración y
     * Hervido en minutos, Fermentación y Maduración en días), la de Molienda la malta consumida en la etapa sobre el
     * rendimiento del molino y la de Envasado la capacidad útil del fermentador sobre la velocidad estándar de envasado
     * (ambos datos de la instantánea de escalado del lote o, si no la tiene, los vigentes del equipo). Una etapa cuya
     * estimación no se puede calcular figura como "Sin estimar" y no cuenta en los porcentajes. Devuelve los criterios
     * Etapa y Receta; cuatro indicadores: etapas finalizadas, dentro de lo estimado (porcentaje con duración real no
     * mayor que la estimada), desvío promedio (porcentaje de exceso sobre lo estimado, con signo) y etapa con mayor
     * desvío promedio; el gráfico de la duración real como porcentaje de la estimada, en promedio por etapa; y la
     * página pedida de la tabla (lote con su receta, etapa, equipo, duración real, duración estimada y desvío), del fin
     * más reciente al más antiguo. No modifica ningún dato.
     * </p>
     *
     * @param criterios Criterios del reporte.
     * @param pageable Página de la tabla.
     * @return El reporte.
     * @throws ReglaNegocioException Si el período no es válido.
     */
    ReporteEstadisticoResponseDTO generarReporteDeTiempos(CriteriosReporteTiempos criterios, Pageable pageable);

    /**
     * Genera el reporte "Insumos": qué insumos se consumieron y cuánto valen. El universo son los consumos registrados
     * con fecha de alta en el período.
     * <p>
     * Precondiciones: las mismas del período que en {@link #generarReporteDeLotes}.
     * </p>
     * <p>
     * Postcondiciones: el valor de un consumo es su cantidad por el costo unitario congelado al consumir. Devuelve los
     * criterios Tipo de insumo y Receta; cuatro indicadores: valor consumido, insumos distintos, insumo de mayor valor y
     * consumo sin reserva (porcentaje del valor en consumos directos); el gráfico de los ocho insumos de mayor valor más
     * "Otros"; y la página pedida del ranking por valor (insumo, tipo, cantidad con su unidad, valor, porcentaje del total
     * y lotes), con la fila de totales. No modifica ningún dato.
     * </p>
     *
     * @param criterios Criterios del reporte.
     * @param pageable Página de la tabla.
     * @return El reporte.
     * @throws ReglaNegocioException Si el período no es válido.
     */
    ReporteEstadisticoResponseDTO generarReporteDeInsumos(CriteriosReporteInsumos criterios, Pageable pageable);

    /**
     * Genera el reporte "Equipamiento": cuánto se usó cada equipo y cuántas intervenciones tuvo, para los cuatro tipos
     * de equipo. El universo son los equipos activos y los dados de baja con actividad en el período.
     * <p>
     * Precondiciones: las mismas del período que en {@link #generarReporteDeLotes}.
     * </p>
     * <p>
     * Postcondiciones: las etapas atendidas son las iniciadas (en curso o finalizadas) que se superponen con el período;
     * las horas en uso son la suma de sus duraciones reales recortadas al período (una etapa en curso cuenta hasta
     * ahora); las limpiezas, fallas y mantenimientos son los registrados con fecha en el período. Devuelve los criterios
     * Tipo de equipo y Equipo; cuatro indicadores: equipos con actividad, etapas atendidas, limpiezas y fallas y
     * mantenimientos; el gráfico de horas en uso de los ocho equipos más usados; y la página pedida de la tabla (equipo
     * con su tipo, etapas atendidas, horas en uso, limpiezas, fallas y mantenimientos) por tipo e identificador, con la
     * fila de totales. No modifica ningún dato.
     * </p>
     *
     * @param criterios Criterios del reporte.
     * @param pageable Página de la tabla.
     * @return El reporte.
     * @throws ReglaNegocioException Si el período no es válido.
     */
    ReporteEstadisticoResponseDTO generarReporteDeEquipamiento(CriteriosReporteEquipamiento criterios, Pageable pageable);
}
