# Estadísticas del sistema

Este documento describe las estadísticas que ofrece el sistema para apoyar las decisiones de la gerencia. Para cada una se indica la pregunta que responde, de qué módulos salen los datos, por qué importa y una explicación en lenguaje sencillo.

## Cómo funcionan todas las estadísticas

- Se encuentran en la opción **Estadísticas** del menú lateral, dentro de la sección **Reportes**. Cada estadística es una pestaña.
- Cada rol ve únicamente las pestañas que le corresponden:
  - **Gerente de Producción:** Lotes, Costos, Insumos y Equipamiento (la pestaña Tiempos se ofrece solo con la propiedad `estadisticas.tiempos.habilitada=true`).
  - **Gerente de Compras:** Compras, Proveedores, Precios y Recepción.
- Todas se consultan para un período **Desde – Hasta** (obligatorio, hasta 366 días) y admiten hasta dos criterios de filtro adicionales.
- Todas muestran lo mismo, en el mismo orden: hasta cuatro indicadores, un gráfico y una tabla de detalle (Costos muestra una tabla por receta, una debajo de la otra).
- Debajo del período, cada una aclara con qué fecha decide qué registros entran: por ejemplo, los lotes por su inicio estimado, los costos por el fin del envasado o las órdenes de compra por su fecha de emisión.
- Todas se pueden exportar a **PDF en hoja A4 vertical**, con encabezado y pie en cada página, número de página, quién lo emitió, fecha de emisión, terminal de emisión, período y todos los criterios de filtro aplicados.
- Los valores monetarios se calculan con los precios registrados en el momento de cada operación, por lo que un informe de un período pasado siempre da el mismo resultado.

---

# Estadísticas del Gerente de Producción

## 1. Lotes

**Pregunta que resuelve:** ¿Cuántos lotes se planificaron en el período y cómo terminaron?

**Módulos de origen:** Lotes (estado y fechas de cada lote, envasados registrados) y Planificación de Producción (a través de ella se conoce la receta de cada lote).

**Importancia:** Es la medida básica del cumplimiento de la producción. Permite saber si la fábrica produce lo que planifica, cuántos lotes se pierden por cancelación y si los lotes terminan dentro del plazo previsto.

**Fecha considerada:** lotes con inicio estimado dentro del período.

**Explicación sencilla:** Muestra la "foto" de la producción del período: cuántos lotes se programaron, cuántos ya se terminaron, cuántos litros de cerveza se envasaron realmente y qué porcentaje de los lotes terminados lo hizo a tiempo. La tabla permite ver lote por lote cómo quedó cada uno.

## 2. Costos

**Pregunta que resuelve:** ¿Cuánto costó producir cada litro de cerveza en el período?

**Módulos de origen:** Lotes (litros envasados y cierre del envasado), Consumo de Insumos (valor de lo consumido en cada lote) y Costos Directos Adicionales (costos por litro aplicados al cerrar el envasado).

**Importancia:** Es la base para decidir a qué precio conviene vender y qué recetas son más convenientes de producir. También permite detectar cuándo el costo de una receta sube sin que nadie lo haya notado.

**Fecha considerada:** lotes con envasado finalizado dentro del período.

**Explicación sencilla:** Suma todo lo que se gastó en materia prima y en otros costos de cada lote terminado y lo divide por los litros que se obtuvieron. Así se sabe cuánto cuesta cada litro, receta por receta, y qué parte de ese costo corresponde a los insumos. Hay una tabla por receta, en el mismo orden del gráfico, con los lotes de esa receta y sus totales.

## 3. Tiempos

Se ofrece solo con la propiedad `estadisticas.tiempos.habilitada=true`.

**Pregunta que resuelve:** ¿Cuánto duran las etapas frente a lo estimado?

**Módulos de origen:** Lotes (fechas reales de inicio y fin de cada etapa), Recetas (duraciones previstas de maceración, hervido, fermentación y maduración), Configuración de Producción (velocidad estándar de envasado) y Equipamiento (rendimiento del molino).

**Importancia:** Los desvíos de tiempo se acumulan y retrasan lotes posteriores y el uso de los equipos. Identificar qué etapa se demora más indica dónde mejorar la planificación o revisar los procesos.

**Explicación sencilla:** Compara cuánto tiempo se calculó que llevaría cada etapa con cuánto llevó realmente. Muestra qué porcentaje de las etapas se cumplió dentro de lo estimado y cuál es la etapa que más se desvía.

## 4. Insumos

**Pregunta que resuelve:** ¿Qué insumos se consumieron y cuánto se gastó en ellos?

**Módulos de origen:** Consumo de Insumos (reservados y directos), Insumos (tipo y unidad de medida) y Recetas (a qué receta corresponde cada consumo).

**Importancia:** Muestra en qué insumos se concentra el gasto de producción. Ayuda a decidir qué materias primas conviene negociar, comprar en mayor volumen o vigilar de cerca.

**Explicación sencilla:** Es un ranking de lo que se usó en la cervecería: de qué insumos se gastó más dinero, qué cantidad se consumió de cada uno y qué parte del total representa. El dinero gastado en un insumo es la cantidad consumida multiplicada por su costo promedio ponderado (PPP), que es el costo que tenía ese insumo en el momento de consumirlo: si el insumo se compró en varios ingresos a distintos precios, el PPP es el promedio de esos precios según las cantidades. Ese costo queda registrado en cada consumo, por lo que no cambia aunque el precio varíe después. "Lotes de producción" indica en cuántos lotes de cerveza se consumió el insumo en el período. Se cuentan los consumos registrados dentro del período (fecha considerada: consumos registrados dentro del período). También indica cuánto de lo consumido se retiró sin haberse reservado antes.

## 5. Equipamiento

**Pregunta que resuelve:** ¿Cuánto se usó cada equipo y cuántas intervenciones tuvo?

**Módulos de origen:** Equipamiento (molinos, maceradores, ollas de hervor y fermentadores, con sus limpiezas, fallas y mantenimientos) y Lotes (etapas en las que se utilizó cada equipo).

**Importancia:** Permite ver si los equipos están sobrecargados o ociosos, y qué equipos concentran fallas y mantenimientos. Sirve para planificar mantenimientos, decidir inversiones y balancear el uso de la planta.

**Explicación sencilla:** Cuenta las horas que trabajó cada equipo en el período y cuántas veces se limpió, falló o recibió mantenimiento. Muestra tarjetas separadas de limpiezas, fallas y mantenimientos, y una sola tabla por equipo. Incluye todos los tipos de equipo, no solo los fermentadores. Fecha considerada: etapas superpuestas con el período e intervenciones registradas dentro de él. Si un lote se canceló con una etapa en marcha, esa etapa cuenta como uso del equipo hasta el momento de la cancelación.

---

# Estadísticas del Gerente de Compras

## 6. Compras

**Pregunta que resuelve:** ¿Cuánto se compró y a qué proveedores?

**Módulos de origen:** Órdenes de Compra (fecha de emisión, estado y detalle de cada orden) y Proveedores.

**Importancia:** Da control sobre el gasto en compras y la dependencia de cada proveedor. El porcentaje de órdenes anuladas señala problemas de planificación o de comunicación con los proveedores.

**Explicación sencilla:** Resume cuánto dinero se pidió en el período, cuántas órdenes se emitieron, cuál fue el monto promedio de cada una y cuántas se anularon. Muestra además cuánto se le compró a cada proveedor. Fecha considerada: órdenes de compra emitidas dentro del período.

## 7. Proveedores

**Pregunta que resuelve:** ¿Qué proveedores entregan lo pedido y en fecha?

**Módulos de origen:** Órdenes de Compra (fecha de entrega estimada y cierre), Ingresos de Insumo (fecha y cantidad realmente recibida) y Proveedores.

**Importancia:** Una entrega tardía o incompleta puede frenar la producción. Esta estadística respalda la elección del proveedor y la negociación de plazos.

**Explicación sencilla:** Califica a cada proveedor: qué porcentaje de sus entregas llegó a tiempo, cuántos días tarda en promedio y qué parte de lo pedido entregó realmente. Cuanto mejores esos números, más confiable es el proveedor. Fecha considerada: órdenes de compra no anuladas emitidas dentro del período.

## 8. Precios

**Pregunta que resuelve:** ¿Cómo variaron los precios de los insumos comprados?

**Módulos de origen:** Órdenes de Compra (precio y cantidad de cada ítem), Presentaciones Comerciales (contenido de cada presentación, para comparar en una misma unidad) e Insumos.

**Importancia:** Anticipa el impacto de los aumentos en el costo de producción y ayuda a elegir cuándo y a quién comprar. Es la contraparte de la estadística de Costos.

**Explicación sencilla:** Compara el precio por unidad (por ejemplo, por kilo) de cada insumo al principio y al final del período, para saber cuáles subieron o bajaron y cuánto. Como cada proveedor vende en presentaciones distintas, todos los precios se llevan a la misma unidad para que sean comparables. Fecha considerada: ítems de órdenes de compra no anuladas emitidas dentro del período.

## 9. Recepción

**Pregunta que resuelve:** ¿Qué se recibió en el período y qué falta recibir?

**Módulos de origen:** Ingresos de Insumo (compras y ingresos directos) y Órdenes de Compra (órdenes pendientes y fechas de entrega).

**Importancia:** Permite saber qué mercadería está pendiente y cuáles órdenes están atrasadas, para reclamar a tiempo y evitar faltantes en producción.

**Explicación sencilla:** Muestra qué mercadería llegó al depósito y cuánto valía, y cuántas órdenes todavía esperan mercadería, cuántas de ellas ya superaron la fecha de entrega prometida. Fecha considerada de lo recibido: ingresos con fecha de ingreso dentro del período.

---

# Datos que el sistema no registra

Estas medidas no están disponibles porque el sistema no guarda la información necesaria:

- Ventas, precios de venta y márgenes de ganancia.
- Costos fijos (alquileres, sueldos, servicios) y su reparto entre lotes.
- Densidades y graduación alcohólica medidas en cada lote; solo se conocen los valores objetivo de la receta.
- Duración de las fallas y de los mantenimientos de los equipos; solo se registra la fecha en que ocurrieron.
- Stock mínimo o punto de reposición de los insumos.
