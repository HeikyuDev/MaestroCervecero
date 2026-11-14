# Estimación de tiempo de una etapa del lote

La guía de cada etapa del lote informa cuánto se espera que dure la etapa y, con la etapa en curso, cuándo se prevé que termine. El cálculo es de solo lectura (`ILoteServicio.estimarEtapa`) y sus fórmulas puras viven en `MetodosEstimacionEtapa`.

## Duración estimada

Toda duración se expresa en minutos enteros.

| Etapa | Fórmula | Datos |
|---|---|---|
| Molienda | $t = \lceil \dfrac{M}{R} \times 60 \rceil$ | $M$: malta planificada del lote en kg (suma de los requerimientos de malta escalados al volumen objetivo, ver `EscaladoDeMalta.md`). $R$: rendimiento de molienda del molino en kg/h; el de la instantánea del escalado del lote y, si no tiene, el vigente del molino de la etapa. |
| Maceración | $t = d_{mac}$ | $d_{mac}$: minutos de maceración de la versión de receta. |
| Hervido | $t = d_{herv}$ | $d_{herv}$: minutos de hervido de la versión de receta. |
| Fermentación | $t = d_{ferm} \times 1440$ | $d_{ferm}$: días de fermentación de la versión de receta. |
| Maduración | $t = d_{mad} \times 1440$ | $d_{mad}$: días de maduración de la versión de receta. |
| Envasado | $t = \lceil \dfrac{C}{v} \times 60 \rceil$ | $C$: capacidad útil del fermentador del lote en litros (el máximo que admite el envasado); la de la instantánea del escalado del lote y, si no tiene, la vigente del fermentador de la etapa de Envasado. $v$: velocidad estándar de envasado de la configuración de producción en L/h. |

Las duraciones por rendimiento (Molienda y Envasado) se redondean hacia arriba al minuto, con un mínimo de 1 minuto, y toleran el error de coma flotante (1,1 kg a 0,1 kg/h son 660 minutos exactos). Un dato faltante, no positivo o no finito deja la duración sin estimar; nunca se inventa un valor.

## Reglas

- La duración del envasado del cronograma usa el volumen objetivo del lote, mientras que la guía de la etapa usa la capacidad útil del fermentador.
- Un lote sin versión de receta no tiene duración estimada en las etapas que toman su dato de la receta; la aclaración informa que falta la versión de receta.

## Ejemplo

Un lote con 4,3952 kg de malta, un molino de 5 kg/h, un fermentador de 20 L de capacidad útil y una velocidad de envasado de 10 L/h:

- Molienda: $4{,}3952 / 5 \times 60 = 52{,}7424$ minutos, que son 53 minutos.
- Envasado: $20 / 10 \times 60 = 120$ minutos, que son "2 h".

## Fin previsto y duración real

- Fin previsto = inicio real de la etapa + duración estimada. Solo se informa con la etapa En curso y duración estimada.
- Duración real = fin real − inicio real, en minutos completos. Solo se informa con la etapa Finalizada y fechas coherentes.
- Con la etapa Pendiente o Cancelada solo hay duración estimada.

## Redacción

Los días completos se redactan en días ("1 día", "10 días"); el resto en minutos o en horas y minutos ("45 min", "1 h", "2 h 30 min").

## Alcance

La estimación es una guía para el operario: no cambia las fechas estimadas del lote ni el cronograma, que siguen calculándose con la duración total del proceso al registrar el lote.
