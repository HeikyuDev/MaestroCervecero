# Diseño de las pantallas del módulo de Stock de Insumos

Este documento define las pantallas del Controller del módulo de Stock de Insumos: qué pantallas existen, sus rutas, qué rol puede ver cada una, cómo está armada cada una y cuántos clics lleva cada objetivo del usuario. El módulo es informativo y de solo lectura: no registra ni modifica nada.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

## 1. Roles y permisos

| Acción | Permiso | Gerente de producción | Gerente de compras | Operario de producción | Encargado de depósito | Demás roles |
| --- | --- | --- | --- | --- | --- | --- |
| Ver la lista de stock, el detalle de un insumo y el detalle de un lote de insumo | `STOCK_INSUMO_CONSULTAR` | Sí | Sí | Sí | Sí | No |
| Abrir el panel de un lote de producción desde una reserva o un consumo | `LOTE_CONSULTAR` | Sí | No (ve el identificador como texto) | Sí | No (ve el identificador como texto) | No |

- Cada método del Controller lleva `@PreAuthorize("hasAuthority('STOCK_INSUMO_CONSULTAR')")`.
- Un acceso no autorizado no devuelve un 403: el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción".
- La opción "Stock de Insumos" (ícono `bi-stack`) está en la sección Operación del menú, debajo de "Ingresos de Insumo", y la ve quien tiene el permiso. El título de la pestaña del navegador es "Stock de insumos - Maestro Cervecero".

## 2. Inventario de pantallas y rutas

| N.º | Pantalla | Ruta | Método | Vista |
| --- | --- | --- | --- | --- |
| 1 | Lista de stock de insumos | `/stock-insumos` (`?nombre=&tipo=&situacionStock=&pagina=`) | GET | `stock_insumo/stock-lista` |
| 2 | Detalle del stock de un insumo | `/stock-insumos/{idInsumo}` (`?pagina=`) | GET | `stock_insumo/stock-detalle-insumo` |
| 3 | Detalle de un lote de insumo | `/stock-insumos/lotes/{idLoteInsumo}` (`?pestana=reservas|consumos&pagina=`) | GET | `stock_insumo/stock-detalle-lote` |

Un ID inexistente, o de un insumo dado de baja (o de un lote de un insumo dado de baja), responde como recurso no encontrado y el `ControllerAdvices` redirige con la alerta. Una página fuera de rango muestra la última.

## 3. Cantidades

Las cuatro cantidades se expresan en la unidad del insumo y se calculan a la fecha de hoy. Un lote está vencido si su fecha de vencimiento es anterior a hoy; el lote que vence hoy es vigente.

| Cantidad | Lote vigente | Lote vencido | Insumo |
| --- | --- | --- | --- |
| Disponible | cantidad actual menos cantidad reservada | 0 | suma de sus lotes |
| Reservada | cantidad reservada | 0 | suma de sus lotes |
| Vencida | 0 | cantidad actual (incluye lo que estaba reservado) | suma de sus lotes |
| Total | cantidad actual | cantidad actual | suma de sus lotes |

Siempre se cumple que Disponible + Reservada + Vencida = Total. Las cantidades se redondean a la escala de las cantidades del sistema. Ninguna pantalla muestra costos.

## 4. Descripción de cada pantalla

**Lista de stock de insumos.** Título "Stock de Insumos" y subtítulo "Cantidad disponible, reservada y vencida de cada insumo, en orden alfabético.". La tarjeta de filtros tiene Nombre (placeholder "Ej: Pilsen", coincidencia parcial sin distinguir mayúsculas), Tipo (Todos, Malta, Lúpulo, Levadura) y Situación del stock (Todas, Con stock disponible, Sin stock disponible, Con lotes vencidos); "Limpiar" y "Buscar" van a la derecha de la tarjeta. Las situaciones no son excluyentes: un insumo con un lote vigente con saldo y otro vencido con saldo cumple "Con stock disponible" y "Con lotes vencidos". "Sin stock disponible" significa disponible igual a 0, aunque haya cantidad reservada o vencida. La tabla lista los insumos activos de a 20 por página, en orden alfabético por nombre. Columnas: Nombre, Tipo, Disponible, Reservada, Vencida, Total y Acciones, con cada cantidad acompañada de la unidad del insumo, y el botón "Abrir" que lleva al detalle del insumo. Los encabezados de las cantidades tienen un tooltip con su significado. Sin resultados: "No se encontraron insumos con los criterios indicados".

**Detalle del stock de un insumo.** Botón "Volver a stock de insumos", título "Stock de <nombre>" con el tipo del insumo. Un panel de resumen con una fila de identidad (nombre, tipo y unidad) y una fila con las cuatro cantidades (Disponible, Reservada, Vencida y Total), cada una con su unidad. Debajo, la tabla "Lotes del insumo" con todos sus lotes, también los que no tienen stock y los vencidos, paginada de a 20 y en orden de vencimiento ascendente (el que vence primero arriba) y, ante igual fecha, por ID. Columnas: Lote del proveedor, Vence, Disponible, Reservada, Vencida, Total y Acciones, con el botón "Abrir" que lleva al detalle del lote y, para quien tiene el permiso `AJUSTE_INSUMO_REGISTRAR`, el botón "Registrar ajuste" que lleva al formulario de ajuste con ese lote elegido (`/ajustes-insumo/nuevo?idLoteInsumo=<id>`). No hay marcas de vencido: el lote vencido se reconoce por su fecha y por su cantidad Vencida. Sin lotes: "El insumo todavía no tiene lotes".

**Detalle de un lote de insumo.** Botón "Volver a <nombre del insumo>" (lleva al detalle del insumo) y título "Lote <identificación del proveedor>". Un panel con una fila de identidad (insumo, lote del proveedor y fecha de vencimiento) y una fila con las cuatro cantidades del lote, con su unidad. Debajo, dos pestañas con ícono, cada una con su tabla paginada de a 20 y el último registro arriba:

- **Reservas** (por defecto). Columnas: Lote de producción, Etapa, Reservada, Consumida, Liberada, Reducida por ajuste, Pendiente y Estado (Activa, Consumida o Liberada). Se listan las reservas en cualquier estado. Sin reservas: "El lote de insumo no tiene reservas".
- **Consumos.** Columnas: Lote de producción, Etapa, Cantidad, Tipo (Reservado o Directo), Fecha, Estado (Registrado o Anulado) y Acciones, con el botón "i" del detalle de la anulación en los consumos anulados. Se listan los consumos registrados y anulados. Sin consumos: "El lote de insumo no tiene consumos".

El lote de producción de cada fila es un enlace al panel del lote (`/lotes/{id}`) para quien tiene `LOTE_CONSULTAR`; para los demás es texto. Una pestaña desconocida equivale a Reservas.

## 5. Recorridos

| Objetivo | Rol | Recorrido | Clics |
| --- | --- | --- | --- |
| Ver cuánto hay disponible de un insumo | Cualquiera de los cuatro roles | Menú Stock de Insumos, completar el nombre, Buscar | 2 más el criterio |
| Ver qué insumos tienen lotes vencidos | Cualquiera de los cuatro roles | Menú Stock de Insumos, Situación del stock "Con lotes vencidos", Buscar | 2 más el criterio |
| Ver los lotes de un insumo y sus vencimientos | Cualquiera de los cuatro roles | Menú Stock de Insumos, "Abrir" del insumo | 2 |
| Ver para qué lotes de producción está reservado un lote de insumo | Cualquiera de los cuatro roles | Menú Stock de Insumos, "Abrir" del insumo, "Abrir" del lote | 3 |
| Ver dónde se consumió un lote de insumo | Cualquiera de los cuatro roles | Menú Stock de Insumos, "Abrir" del insumo, "Abrir" del lote, pestaña Consumos | 4 |
| Ir al lote de producción de una reserva o un consumo | Gerente de producción, operario de producción | Detalle del lote de insumo, enlace del lote de producción | 1 más el recorrido anterior |
