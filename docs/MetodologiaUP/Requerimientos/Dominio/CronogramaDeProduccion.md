# Cronograma de producción: cola de lotes de un fermentador

El cronograma se lleva por fermentador, el único equipo cuyo tiempo de uso se mide en días. Cada fermentador tiene una cola: el lote en ejecución (como máximo uno) y los lotes pendientes, ordenados por fecha de inicio estimada. Cada lote ocupa del inicio al fin estimados, y el siguiente comienza como mínimo dos días después (el día intermedio es la limpieza). Pueden quedar huecos.

## Prioridad de la planificación

Cada planificación de producción tiene una prioridad: `BAJA`, `MEDIA` o `ALTA`. Se define al registrarla y no cambia.

## Orden de los lotes nuevos

Al registrar un lote, su lugar en la cola pendiente del fermentador se decide comparando su planificación con las de los lotes pendientes:

1. Mayor prioridad primero.
2. A igual prioridad, menor margen primero. El margen es `fechaFinalizacionEstimada - fechaInicioEstimada` de la planificación, en días.
3. A igual margen, la planificación con la fecha de finalización estimada más temprana primero.
4. Si todo empata, el orden de registro: el lote nuevo queda después.

El lote nuevo se ubica inmediatamente después del último lote pendiente que le gana o empata, y por delante de los que le pierden. Esos lotes se empujan hacia atrás: conservan su duración y toda la cola pendiente se recalcula en la misma transacción, encadenada desde el lote en ejecución o desde hoy. Ningún lote comienza antes de hoy ni antes de la fecha de inicio estimada de su planificación.

## Reglas que no cambian

- El lote en ejecución nunca se mueve y es el piso de la cola: los lotes pendientes comienzan dos días después de su fin estimado (desde hoy más dos días si está atrasado), nunca antes.
- Subir y bajar un lote en la cola es manual y libre: no se controla contra la prioridad ni contra el margen.
- Con una cola reordenada a mano, el lote nuevo se ubica después del último lote que le gana o empata; nunca pasa por delante de uno que le gana o empata, aunque haya lotes que le pierden delante de él.
- Iniciar un lote no se valida contra las fechas de su planificación: un lote planificado para dentro de dos años puede iniciarse hoy. Su fecha estimada pasa a hoy, conserva su duración y los demás pendientes del fermentador se corren a continuación.
- Las colas de los demás fermentadores no intervienen.
