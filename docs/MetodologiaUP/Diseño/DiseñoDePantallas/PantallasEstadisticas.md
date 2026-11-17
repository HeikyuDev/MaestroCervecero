# Diseño de la pantalla de estadísticas

Este documento define la pantalla "Estadísticas": quién la ve, cómo se organiza en pestañas y qué muestra cada pestaña, en pantalla y en su reporte impreso. La estructura común de todo reporte y la receta para agregar una pestaña están en `../../../Tecnico/EstandaresUI.md`, sección "Estadísticas y reportes impresos". El comportamiento verificable está en `../../../test/casos_de_prueba/services/estadisticas` y en `../../../test/casos_de_prueba/controllers/estadisticas`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve la página HTML y el PDF. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

## 1. Roles y permisos

| Acción | Permiso | Gerente de producción | Gerente de compras | Demás roles |
| --- | --- | --- | --- | --- |
| Entrar a "Estadísticas" | `ESTADISTICA_PRODUCCION_CONSULTAR` o `ESTADISTICA_COMPRAS_CONSULTAR` | Sí | Sí | No |
| Ver las pestañas de producción y su PDF (Lotes, Costos, Insumos y Equipamiento) | `ESTADISTICA_PRODUCCION_CONSULTAR` | Sí | No | No |
| Ver las pestañas de compras y su PDF (Compras, Proveedores y Precios) | `ESTADISTICA_COMPRAS_CONSULTAR` | No | Sí | No |
| "Abrir" un lote desde la tabla de Lotes | `LOTE_CONSULTAR` | Sí | No | No |
| "Ver" una orden de compra desde la tabla de Recepción (solo con la pestaña habilitada) | `ORDEN_COMPRA_CONSULTAR` | No | Sí | No |

- La ruta de la pantalla y la del PDF llevan `@PreAuthorize("hasAnyAuthority('ESTADISTICA_PRODUCCION_CONSULTAR', 'ESTADISTICA_COMPRAS_CONSULTAR')")`; dentro, cada pestaña se muestra solo con su propio permiso. Un acceso sin permiso no devuelve un 403: el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción".
- El menú tiene una sola entrada, "Estadísticas" (`bi-bar-chart`), en la sección "Reportes", debajo de Administración.
- Un usuario con un solo permiso ve solo las pestañas de su gerencia; los buscadores de los criterios propios exigen los permisos de las pantallas de donde se toman (recetas: `LOTE_CONSULTAR`; proveedores: gerente de compras o encargado de depósito; equipos: `ESTADISTICA_PRODUCCION_CONSULTAR`).

## 2. Ruta y pestañas

| Ruta | Respuesta |
| --- | --- |
| `GET /estadisticas?pestana=<clave>&desde=&hasta=&<criterios>&pagina=` | Pantalla `estadisticas/estadisticas` con la pestaña pedida. |
| `GET /estadisticas/pdf?pestana=<clave>&desde=&hasta=&<criterios>` | Reporte impreso de la pestaña, `application/pdf` en línea, nombre `estadisticas-<clave>_<desde>_<hasta>.pdf`. |

| Pestaña | Clave | Ícono | Permiso | Pregunta que responde |
| --- | --- | --- | --- | --- |
| Lotes | `lotes` | `bi-collection` | `ESTADISTICA_PRODUCCION_CONSULTAR` | Cuántos lotes se planificaron en el período y cómo terminaron |
| Costos | `costos` | `bi-cash-coin` | `ESTADISTICA_PRODUCCION_CONSULTAR` | Cuánto costó producir cada litro en el período |
| Tiempos (solo con la propiedad habilitada, ver abajo) | `tiempos` | `bi-hourglass-split` | `ESTADISTICA_PRODUCCION_CONSULTAR` | Cuánto duran las etapas frente a lo estimado |
| Insumos | `insumos` | `bi-basket` | `ESTADISTICA_PRODUCCION_CONSULTAR` | Qué insumos se consumieron y cuánto se gastó en ellos |
| Equipamiento | `equipamiento` | `bi-tools` | `ESTADISTICA_PRODUCCION_CONSULTAR` | Cuánto se usó cada equipo y cuántas intervenciones tuvo |
| Compras | `compras` | `bi-cart3` | `ESTADISTICA_COMPRAS_CONSULTAR` | Cuánto se compró y a qué proveedores |
| Proveedores | `proveedores` | `bi-truck` | `ESTADISTICA_COMPRAS_CONSULTAR` | Qué proveedores entregan lo pedido y en fecha |
| Precios | `precios` | `bi-graph-up-arrow` | `ESTADISTICA_COMPRAS_CONSULTAR` | Cómo variaron los precios de los insumos comprados |
| Recepción (solo con la propiedad habilitada, ver abajo) | `recepcion` | `bi-box-arrow-in-down` | `ESTADISTICA_COMPRAS_CONSULTAR` | Qué se recibió en el período y qué falta recibir |

Sin `pestana`, o con una que el usuario no puede ver o que no existe, se muestra la primera pestaña que el usuario puede ver. Cada pestaña es la misma URL con otro `?pestana=`.

La pestaña "Tiempos" se ofrece solo con `estadisticas.tiempos.habilitada=true` (en `application.properties` está en `false`): sin la propiedad no aparece entre las pestañas y su clave, en la pantalla y en el PDF, se trata como una clave inexistente. Las demás pestañas no dependen de ella.

La pestaña "Recepción" se ofrece solo con `estadisticas.recepcion.habilitada=true` (en `application.properties` está en `false`) y se comporta igual que Tiempos: sin la propiedad no aparece entre las pestañas y su clave, en la pantalla y en el PDF, se trata como una clave inexistente, por lo que el gerente de compras ve tres pestañas (Compras, Proveedores y Precios). El reporte, su Service y sus tests se conservan.

## 3. Pantalla

De arriba abajo: el título "Estadísticas", las pestañas (componente `fragments/pestanas`, cada una con su ícono) y, para la pestaña activa:

1. La línea con la pregunta que responde la pestaña.
2. Los filtros, con la grilla de las listas y cada campo a su ancho normal: "Desde" y "Hasta" (obligatorios, asterisco rojo; sin parámetros, del primer día del mes en curso a hoy), los criterios propios de la pestaña (como máximo dos). Debajo, en su propia fila y a la derecha, "Limpiar" (vuelve a la pestaña sin criterios) y "Filtrar", como en el resto de las listas.
3. El encabezado de resultados: "Resultados del dd/MM/yyyy al dd/MM/yyyy" a la izquierda, con una línea debajo que dice qué fecha decide qué registros entran en el período (por ejemplo, "Lotes con inicio estimado dentro del período"), y, a la derecha, el botón secundario "Exportar PDF" (abre el PDF en otra pestaña con los mismos criterios).
4. Hasta cuatro indicadores, todos en una fila que ocupa todo el ancho: la grilla toma tantas columnas como tarjetas (2, 3 o 4) y, con menos de cuatro, las tarjetas son más amplias y su valor más grande. Tarjetas por pestaña: Lotes 4, Costos 3, Tiempos 4, Insumos 4, Equipamiento 3, Compras 4, Proveedores 2, Precios 3 y Recepción 4 (esta última, solo con la pestaña habilitada).
5. Debajo, a todo el ancho, un gráfico de barras horizontales compacto, con el valor escrito a continuación de cada barra.
6. Una tabla paginada de hasta siete columnas, con fila de totales cuando corresponde. Costos muestra una tabla por receta, con título, una debajo de la otra y sin paginación.

Validación, solo en el servidor (el formulario lleva `novalidate`): el período debe tener sus dos extremos, la fecha hasta no puede ser anterior a la fecha desde y el período no puede superar los 366 días contando ambos extremos. Una fecha que no existe se informa con "La fecha desde no es válida" o "La fecha hasta no es válida". Un rechazo se muestra con la alerta amarilla del layout, se conservan los criterios y en lugar de los indicadores, el gráfico y la tabla se muestra "Indique un período válido y presione Filtrar para ver el reporte", sin el encabezado de resultados ni "Exportar PDF". Un valor de lista que no existe no filtra y se avisa; una entidad que el buscador no ofrece no filtra.

## 4. Pestaña "Lotes"

**Universo.** Los lotes cuya fecha de inicio estimada cae en el período (ambos extremos inclusive), en cualquier estado. Fecha considerada, escrita debajo del período: "Lotes con inicio estimado dentro del período".

**Criterios propios.**

| Criterio | Parámetro | Control | Sin valor |
| --- | --- | --- | --- |
| Receta | `idReceta` | Selector con el buscador en modal de los filtros de lotes (`/lotes/filtros/recetas`): recetas activas y dadas de baja con planificaciones; incluye todas las versiones de la receta | Todas |
| Estado | `estado` | Lista simple con los cuatro estados del lote | Todos; un valor inexistente avisa "El estado indicado no es válido, se muestran todos" |

**Indicadores.**

| Indicador | Valor | Aclaración |
| --- | --- | --- |
| Lotes del período | Cantidad de lotes del universo | "Por fecha de inicio estimada" |
| Finalizados | Cantidad de lotes finalizados | Su porcentaje sobre los lotes del período ("37,5 % de los lotes"); sin lotes, sin aclaración |
| Litros envasados | Suma de los envasados registrados (los anulados no cuentan), en L | "Envasados registrados" |
| Terminados en plazo | Porcentaje de los finalizados cuya fecha de fin real no es posterior a la de fin estimado; "-" si no hay finalizados | "N de M finalizados" o "Sin lotes finalizados". Tooltip con la definición |

**Gráfico.** "Lotes por estado": una barra por estado (Pendiente, En ejecución, Finalizado, Cancelado), con cero donde no hay lotes y la cantidad escrita en cada barra.

**Tabla.** Del inicio estimado más reciente al más antiguo y, ante igual fecha, del último registrado al primero; de a 20 filas.

| Columna | Contenido |
| --- | --- |
| Lote | Código del lote y, debajo en letra chica, el nombre de la versión de receta |
| Estado | Estado del lote |
| Litros envasados | Suma de sus envasados registrados; 0 si no tiene |
| Fin estimado | Fecha de finalización estimada |
| Fin real | Fecha de finalización real, o "-" |
| Acciones | Botón "Abrir" hacia el lote, con `LOTE_CONSULTAR` (encabezado y celda centrados) |

La fila de totales ("Totales del período") lleva los litros envasados de todo el período, no solo de la página. Sin lotes, la tabla dice "Sin resultados para los criterios indicados".

## 5. Reglas comunes de las pestañas

- **Costos congelados.** El costo de un consumo es su cantidad por el costo unitario promedio que se congeló al consumir; el de un costo adicional, el que se congeló al finalizar el envasado. No hay costos fijos. Un cambio de precios posterior no modifica los reportes de lo ya consumido.
- **Anulados.** Los consumos, envasados, ingresos y operaciones anulados no cuentan; las órdenes de compra anuladas no cuentan en montos, proveedores ni precios (solo en "Órdenes emitidas" y "Órdenes anuladas" de Compras, que las muestra).
- **Gráficos.** Las ocho categorías más grandes; el resto se suma en "Otros" cuando el valor es sumable (montos, valores) y se omite cuando no lo es (costo por litro, porcentajes, variaciones).
- **Sin datos.** Un porcentaje o promedio sin elementos sobre los que calcularse se muestra con un guion; un total o una cantidad sin elementos, con cero. Una tabla sin filas dice "Sin resultados para los criterios indicados" y no muestra la fila de totales, en la pantalla ni en el reporte impreso.
- **Rendimiento.** Cada reporte se calcula con consultas agrupadas o proyecciones (sin cargar entidades ni consultar fila por fila); las tablas de los reportes que no se paginan en la base se arman en memoria con el universo acotado del período y se paginan de a 20.

## 6. Pestaña "Costos"

**Universo.** Los lotes cuya etapa de Envasado está finalizada con fecha de finalización en el período y que tienen litros envasados registrados mayores que cero. Fecha considerada, escrita debajo del período: "Lotes con envasado finalizado dentro del período".

**Criterio propio.** Receta (selector con buscador; incluye todas sus versiones).

| Indicador | Valor | Aclaración |
| --- | --- | --- |
| Costo total | Suma del costo de insumos y de costos adicionales de los lotes | "Insumos y costos adicionales" |
| Litros envasados | Suma de los envasados registrados de esos lotes | Cantidad de lotes |
| Peso de los insumos | Porcentaje del costo total que son insumos; "-" si el costo total es cero. Tooltip | "Del costo total" |

**Gráfico.** Costo por litro por receta (las ocho más caras, sin "Otras": una razón no se suma). El costo por litro de una receta es la suma del costo total de sus lotes sobre la suma de sus litros.

**Tablas.** Una tabla por receta con lotes en el período, una debajo de la otra y sin paginación, en el mismo orden del gráfico (de mayor a menor costo por litro y, ante un empate, por nombre) y con el mismo nombre de receta como título, para seguir cada barra hasta sus lotes. Cada tabla lista solo los lotes de su receta, del fin de envasado más reciente al más antiguo: Lote, Fin de envasado, Litros, Costo de insumos, Costo adicional, Costo total y Costo por litro (el costo total es la suma del costo de insumos y el costo adicional). Totales de la receta en el período: litros, costo de insumos, costo adicional, costo total y costo por litro ponderado. Sin lotes en el período, una sola tabla vacía sin título.

## 7. Pestaña "Tiempos"

Se ofrece solo con `estadisticas.tiempos.habilitada=true`.

**Universo.** Las etapas finalizadas cuya fecha de finalización cae en el período (ambos extremos inclusive), aunque hayan empezado antes. Las etapas canceladas no cuentan.

**Criterios propios.** Etapa (lista) y Receta (selector).

**Duración estimada.** Maceración y Hervido, la de la receta en minutos; Fermentación y Maduración, la de la receta en días; Molienda, la malta consumida en la etapa sobre el rendimiento del molino; Envasado, la capacidad útil del fermentador sobre la velocidad estándar de envasado. Molienda y Envasado usan el dato de la instantánea de escalado del lote o, si no la tiene, el vigente del equipo. Si no se puede calcular, la etapa figura como "Sin estimar" y no cuenta en los porcentajes.

| Indicador | Valor |
| --- | --- |
| Etapas finalizadas | Cantidad (aclaración: cuántas sin estimar) |
| Dentro de lo estimado | Porcentaje de las etapas con estimación cuya duración real no superó la estimada; "-" sin estimadas. Tooltip |
| Desvío promedio | Promedio de (real - estimada) sobre la estimada, con signo; "-" sin estimadas. Tooltip |
| Etapa con mayor desvío | La etapa con mayor desvío promedio, con su valor en la aclaración; ante un empate, la primera en el orden del proceso (Molienda, Maceración, Hervido, Fermentación, Maduración y Envasado); "-" sin estimadas |

**Redondeo.** "Dentro de lo estimado", el desvío promedio y la etapa con mayor desvío se calculan con los desvíos de cada etapa sin redondear; el redondeo a dos decimales es solo para mostrarlos.

**Gráfico.** Duración real como porcentaje de la estimada, promedio por etapa (100 % es lo estimado).

**Tabla** (fin más reciente primero): Lote (con la receta debajo), Etapa, Equipo, Duración real, Duración estimada y Desvío.

## 8. Pestaña "Insumos"

**Universo.** Los consumos registrados (los anulados no cuentan) cuya fecha de alta cae en el período, ambos extremos inclusive; es la fecha en que se registró el consumo, no la de su lote de producción. Fecha considerada, escrita debajo del período: "Consumos registrados dentro del período".

**Criterios propios.** Tipo de insumo (lista) y Receta (selector; la del lote del consumo).

| Indicador | Valor |
| --- | --- |
| Valor consumido | Suma, de cada consumo registrado, de la cantidad consumida por el costo promedio ponderado (PPP) que tenía el lote de insumo al consumirlo y que quedó congelado en el consumo. Sin tooltip; aclaración "Al costo de cada consumo" |
| Insumos distintos | Cantidad de insumos consumidos |
| Insumo de mayor valor | Su nombre, con su valor en la aclaración; "-" sin consumos |
| Consumo sin reserva | Porcentaje del valor que se registró como consumo directo (sin reserva previa); "-" sin valor. Tooltip |

**Gráfico.** Los ocho insumos de mayor valor y "Otros".

**Tabla** (ranking por valor, de mayor a menor): Insumo, Tipo, Cantidad (con su unidad), Valor, % del total y Lotes de producción. "Valor" es el mismo valor consumido del indicador, por insumo. "Lotes de producción" es la cantidad de lotes de producción distintos en los que se consumió el insumo en el período (no es la cantidad de lotes de insumo) y su encabezado lleva el tooltip "Cantidad de lotes de producción en los que se consumió el insumo en el período". Totales: valor y 100 %.

## 9. Pestaña "Equipamiento"

**Universo.** Todos los equipos activos de los cuatro tipos (molinos, maceradores, ollas de hervor y fermentadores) y los dados de baja que tuvieron actividad en el período.

**Criterios propios.** Tipo de equipo (lista) y Equipo (selector con su buscador en modal).

**Fecha considerada**, escrita debajo del período: "Etapas superpuestas con el período e intervenciones registradas dentro de él".

**Actividad.** Las etapas atendidas son las que usaron realmente el equipo y se superponen con el período: las en curso, las finalizadas y las canceladas que ya habían iniciado (el lote se canceló con la etapa en marcha); una etapa cancelada que nunca inició no cuenta. Las horas en uso son su duración real recortada al período: una etapa en curso cuenta hasta ahora, sin pasar del fin del período, y una cancelada cuenta hasta el momento de la cancelación, que el lote guarda como su fecha de finalización (por ejemplo, 30 h de uso y 1 etapa para una etapa iniciada 30 horas antes de que se cancelara el lote); las limpiezas, fallas y mantenimientos son los registrados con fecha en el período (cada uno tiene una sola fecha).

| Indicador | Valor |
| --- | --- |
| Limpiezas | Cantidad registrada | "Registradas" |
| Fallas | Cantidad registrada | "Registradas" |
| Mantenimientos | Cantidad registrada | "Registrados" |

**Gráfico.** Horas en uso por equipo (los ocho más usados, sin "Otros").

**Tabla** (por tipo e identificador, paginada): Equipo (con el tipo debajo; "(dado de baja)" si corresponde), Etapas atendidas, Horas en uso (con su unidad, por ejemplo "0,37 h"), Limpiezas, Fallas y Mantenimientos. Totales de todas las columnas numéricas, con las horas también con su unidad.

## 10. Pestaña "Compras"

**Universo.** Las órdenes de compra cuya fecha de emisión (fecha de alta) cae en el período, en cualquier estado. Fecha considerada, escrita debajo del período: "Órdenes de compra emitidas dentro del período". El monto de una orden es la suma de cantidad por costo unitario de sus ítems.

**Criterios propios.** Proveedor (selector) y Estado (lista).

| Indicador | Valor |
| --- | --- |
| Monto comprado | Suma de los montos de las órdenes no anuladas |
| Órdenes emitidas | Cantidad de órdenes del universo, anuladas incluidas |
| Monto promedio por orden | Monto comprado sobre las órdenes no anuladas; "-" sin órdenes |
| Órdenes anuladas | Porcentaje de las emitidas (aclaración: "N de M"); "-" sin órdenes |

**Gráfico.** Monto por proveedor (los ocho mayores y "Otros"), sin anuladas.

**Tabla** (la más reciente primero): Orden (N.º), Fecha de emisión, Proveedor, Estado, Ítems y Monto. Total sin anuladas.

## 11. Pestaña "Proveedores"

**Universo.** Los ítems de las órdenes no anuladas emitidas en el período, agrupados por orden y por proveedor. Con un tipo de insumo, solo cuentan esos ítems. Fecha considerada, escrita debajo del período: "Órdenes de compra no anuladas emitidas dentro del período". Los porcentajes de entrega (las dos tarjetas, la columna "A tiempo" y el gráfico) consideran solo órdenes completas: las pendientes, las recibidas en parte y las cerradas a mano sin todo lo pedido recibido no se cuentan.

**Criterios propios.** Proveedor (selector) y Tipo de insumo (lista).

**Definiciones.**

- Una orden está **completa** si de cada ítem se recibió al menos lo pedido (los ingresos registrados, convertidos a presentaciones) y tiene al menos un ingreso.
- Una orden llegó **a tiempo** si está completa y la fecha de su último ingreso no es posterior a su fecha de entrega estimada.
- El **plazo de entrega** de una orden completa son los días desde su fecha de emisión hasta la fecha de su último ingreso.
- Una orden llegó **fuera de plazo** si está completa y no llegó a tiempo: la fecha de su último ingreso es posterior a su fecha de entrega estimada (o no tiene fecha estimada). Las entregas a tiempo y fuera de plazo suman las órdenes completas.
- Lo **recibido sobre lo pedido** (columna "Recibido" de la tabla) se mide en presentaciones, sin pasar de lo pedido en cada ítem, igual que el avance del panel del gerente de compras.

| Indicador | Valor |
| --- | --- |
| Entregas a tiempo | Porcentaje de las órdenes completas que llegaron a tiempo; "-" sin órdenes completas. Tooltip |
| Entregas fuera de plazo | Porcentaje de las órdenes completas que no llegaron a tiempo; "-" sin órdenes completas (aclaración "N de M órdenes completas" o "Sin órdenes completas"). Tooltip |

Son dos tarjetas, por lo que la grilla de indicadores usa dos columnas con tarjetas más amplias.

**Gráfico.** Porcentaje de entregas a tiempo por proveedor (los ocho con más órdenes completas; sin "Otros").

**Tabla** (ranking por monto): Proveedor, Órdenes, Monto, A tiempo (%), Plazo promedio (días) y Recibido (%); el plazo promedio y lo recibido se muestran solo por proveedor. Totales del período.

## 12. Pestaña "Precios"

**Universo.** Los ítems de las órdenes no anuladas emitidas en el período. Fecha considerada, escrita debajo del período: "Ítems de órdenes de compra no anuladas emitidas dentro del período". El precio se compara por unidad de medida del insumo: el costo de la presentación sobre la cantidad de insumo que trae (la misma conversión de las órdenes de compra).

**Criterios propios.** Tipo de insumo (lista) y Proveedor (selector).

**Variación.** Último precio contra el primero del período, por fecha de emisión de la orden; "-" si el insumo tuvo una sola compra. El precio promedio se pondera por la cantidad comprada. El precio por unidad se calcula con 10 decimales y el mínimo, el máximo, el promedio y la variación salen de esos valores sin redondear: el redondeo (4 decimales en los importes, 2 en los porcentajes) es solo para mostrarlos. Por ejemplo, un insumo en gramos comprado en bolsas de 25 kg a $ 12 y luego a $ 13 varía +8,33 %.

| Indicador | Valor |
| --- | --- |
| Insumos comprados | Cantidad de insumos del universo |
| Insumos con suba | Cantidad con variación positiva (aclaración: de cuántos con más de una compra). Tooltip |
| Insumos con baja | Cantidad con variación negativa (aclaración: de cuántos con más de una compra). Tooltip |

**Gráfico.** Variación porcentual por insumo (los ocho de mayor variación absoluta). Las subas usan el ámbar y las bajas el azul, con el signo escrito en cada barra.

**Tabla** (orden alfabético): Insumo (con "Por <unidad>" debajo), Precio mínimo, Precio máximo, Precio promedio, Primer → último precio y Variación.

## 13. Pestaña "Recepción"

**Universo.** Los ingresos registrados con fecha de ingreso en el período, por compra o directos. Fecha considerada, escrita debajo del período: "Ingresos con fecha de ingreso dentro del período". El valor de un ingreso es su cantidad recibida por su costo unitario. Los dos indicadores de lo pendiente son de hoy y no dependen del período.

**Criterios propios.** Proveedor (selector; con un proveedor no se incluyen los ingresos directos) y Tipo de ingreso (lista).

| Indicador | Valor |
| --- | --- |
| Ingresos registrados | Cantidad del período |
| Valor recibido | Suma del valor de los ingresos |
| Órdenes con saldo pendiente | Órdenes en estado pendiente hoy (las del proveedor elegido). Tooltip |
| Órdenes con entrega vencida | Las pendientes con fecha de entrega estimada anterior a hoy. Tooltip |

**Gráfico.** Valor recibido por proveedor (los ocho mayores y "Otros"); los ingresos directos figuran como "Sin orden de compra".

**Tabla** (el ingreso más reciente primero): Fecha, Insumo, Cantidad (con su unidad), Valor, Tipo y Orden (el ojo "Ver" hacia la orden, solo en los ingresos por compra y con `ORDEN_COMPRA_CONSULTAR`). Total del valor.

## 14. Reporte impreso

El mismo contenido que la pantalla, en hoja A4 vertical, con la plantilla única `estadisticas/pdf/reporte`:

- Encabezado en cada página: "Maestro Cervecero", "Estadísticas: <pestaña>" y una línea.
- Datos de emisión en la primera página: "Emitido por" (usuario y, entre paréntesis, nombre y apellido), "Fecha de emisión" (dd/MM/yyyy HH:mm), "Terminal" (el equipo desde el que se pidió el reporte: la dirección es la de la conexión que pidió el reporte, sin tener en cuenta los encabezados que escribe el cliente, como `X-Forwarded-For`; una dirección de loopback se reemplaza por el nombre y la dirección del servidor; el nombre del equipo se resuelve por DNS inverso solo para direcciones IP, con un tiempo máximo de 300 ms y en un grupo reducido de hilos propio, el resultado (también cuando no hay nombre) se conserva 10 minutos por dirección y se muestra `nombre-de-equipo (IP)`, o solo la IP si no se resuelve), "Período", "Fecha considerada" (la misma línea de la pantalla) y "Criterios de filtro" con cada criterio de la pestaña ("Todas" o "Todos" si no se indicó).
- Cuerpo: la pregunta, los mismos indicadores (con las definiciones de los que tienen tooltip escritas debajo), el mismo gráfico y las tablas completas (cada una con su título si el reporte tiene varias), sin paginar y sin la columna Acciones: el encabezado de la tabla se repite en cada página, ninguna fila se parte y la fila de totales va al final. Si el período tiene más filas que el tope técnico (`LimitesTecnicos.REPORTE_IMPRESO_MAXIMO_FILAS`, 2.000), se avisa antes de la tabla.
- Pie en cada página: la fecha de emisión, "Maestro Cervecero" y "Página X de Y".

Si los criterios no son válidos, la ruta del PDF vuelve a la pantalla con los mismos parámetros, que muestra el motivo. Si el documento falla al armarse, o ya hay 3 reportes en generación (`LimitesTecnicos.REPORTE_IMPRESO_SIMULTANEOS_MAXIMO`) y no se libera un lugar en 2 segundos (`LimitesTecnicos.REPORTE_IMPRESO_ESPERA_MAXIMA_MS`), vuelve a la pantalla con la alerta amarilla "No se pudo generar el reporte impreso" o "Hay otros reportes en generación, intente nuevamente en unos instantes". La falla se registra en el log con la pestaña y el período, sin datos personales.
