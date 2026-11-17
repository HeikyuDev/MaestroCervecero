# Diseño del panel de control del gerente de compras

Este documento define el panel que el gerente de compras ve en la pantalla de inicio (`/home`): qué muestra, qué atajos ofrece, qué permiso exige cada uno, cómo está armado y cuántos clics lleva cada objetivo del usuario. Es un panel de trabajo: no registra nada por sí mismo, pero la fila de cada planificación lleva con un clic al formulario de la orden de compra con esa planificación elegida, y las demás filas llevan a la consulta. El comportamiento verificable está en `../../../test/casos_de_prueba/services/dashboard` y en `../../../test/casos_de_prueba/controllers/dashboard`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

El panel responde a una pregunta: qué hay que comprar y cómo van las compras. Muestra las planificaciones que todavía no tienen orden de compra, las órdenes pendientes con el avance de su recepción y las órdenes finalizadas en los últimos 30 días.

## 1. El módulo de compras en el que se apoya

- Una orden de compra nace siempre de una planificación de producción Pendiente, del proveedor elegido y de ítems del catálogo de ese proveedor (insumo con su presentación comercial) cuyo insumo forma parte de la receta de la planificación. El costo unitario se define al registrar la orden.
- Una planificación puede tener varias órdenes de compra: el sistema no calcula cuánto de lo que necesita la planificación cubren sus órdenes. Por eso el panel solo puede afirmar que una planificación "no tiene orden de compra vigente" (ninguna orden Pendiente o Finalizada; las anuladas no cuentan), no que esté o no cubierta.
- Una orden Pendiente se finaliza sola cuando todos sus ítems quedan completamente recibidos por ingresos de insumo en estado Registrado (sin motivo de finalización), o manualmente con un motivo si tiene al menos un ingreso. Anular un ingreso reabre una orden finalizada automáticamente; una finalizada manualmente no se reabre. Una orden sin ingresos registrados solo puede anularse.
- Un ingreso por compra guarda la cantidad recibida en la unidad del insumo; la cantidad pedida de cada ítem son presentaciones comerciales. El avance de recepción convierte lo recibido a presentaciones con la misma conversión que usa el ingreso de insumos.

## 2. Roles y permisos

| Acción | Permiso | Gerente de compras | Demás roles |
| --- | --- | --- | --- |
| Ver el panel del gerente de compras | Rol `GERENTE_DE_COMPRAS` | Sí | No (cada rol ve su propio panel) |
| Registrar la orden de compra de una planificación | `ORDEN_COMPRA_REGISTRAR` | Sí | No ve el panel |
| Ver una planificación y recorrer la lista de planificaciones pendientes | `PLANIFICACION_PRODUCCION_CONSULTAR` | Sí | No ve el panel |
| Ver una orden de compra y recorrer sus listas (contadores, "Ver todas", ojo "Ver") | `ORDEN_COMPRA_CONSULTAR` | Sí | No ve el panel |

- El panel se muestra con `sec:authorize="hasRole('GERENTE_DE_COMPRAS')"` y el `HomeController` carga sus datos solo para ese rol.
- Cada contador, enlace "Ver todas" y botón de fila lleva `sec:authorize="hasAuthority(...)"` con el permiso de la tabla, el mismo del destino. Sin el permiso, el contador queda como texto plano.
- El panel no muestra stock de insumos: el stock corresponde al panel del encargado de depósito.

## 3. Inventario de pantallas y rutas

El panel no tiene rutas propias: es un fragmento de la pantalla de inicio. Sus atajos usan rutas de otras pantallas.

| N.º | Pantalla o destino | Ruta | Método | Vista | Permiso del destino |
| --- | --- | --- | --- | --- | --- |
| 1 | Pantalla de inicio con el panel del gerente de compras | `/home` (también `/`) | GET | `home` con el fragmento `home/dashboard-gerente-compras :: panel` | Usuario autenticado |
| 2 | Lista de planificaciones pendientes | `/planificaciones-produccion?estado=PENDIENTE` | GET | `planificacion_produccion/planificacion-lista` | `PLANIFICACION_PRODUCCION_CONSULTAR` |
| 3 | Estado actual de una planificación | `/planificaciones-produccion/{id}` | GET | `planificacion_produccion/planificacion-detalle` | `PLANIFICACION_PRODUCCION_CONSULTAR` |
| 4 | Formulario de la orden de compra con la planificación elegida | `/ordenes-compra/nuevo?idPlanificacion={id}` | GET | `orden_compra/orden-form` | `ORDEN_COMPRA_REGISTRAR` |
| 5 | Lista de órdenes de compra pendientes | `/ordenes-compra?estado=PENDIENTE` | GET | `orden_compra/orden-lista` | `ORDEN_COMPRA_CONSULTAR` |
| 6 | Lista de órdenes de compra finalizadas | `/ordenes-compra?estado=FINALIZADA` | GET | `orden_compra/orden-lista` | `ORDEN_COMPRA_CONSULTAR` |
| 7 | Detalle de una orden de compra | `/ordenes-compra/{id}` | GET | `orden_compra/orden-detalle` | `ORDEN_COMPRA_CONSULTAR` |

El identificador de la planificación (`idPlanificacion`) es un parámetro aparte de la ruta: no forma parte del formulario. Si la planificación ya no está Pendiente, el formulario lo informa al registrar.

## 4. Reglas específicas

- **Planificaciones por comprar.** Entran las planificaciones Pendiente que no tienen ninguna orden de compra vigente. Una planificación con una orden vigente no figura aunque esa orden no cubra todos sus insumos. Se ordenan por fecha de inicio estimada ascendente (la que arranca antes arriba) y, ante igual fecha, por ID. El contador es una tarjeta sin enlace: la lista completa es este bloque.
- **Órdenes pendientes.** Entran todas las órdenes Pendiente, por fecha de entrega estimada ascendente (la más vencida arriba) y, ante igual fecha, por ID.
- **Recibido.** Es el avance de la recepción de la orden, de 0 a 100: las presentaciones recibidas por ingresos Registrado, sin pasar de lo pedido en cada ítem, sobre las presentaciones pedidas en todos los ítems, redondeado hacia abajo. Una orden sin ingresos muestra 0 %.
- **Entregas vencidas.** Son las órdenes Pendiente con fecha de entrega estimada anterior a hoy. Nunca superan a las órdenes pendientes. La tarjeta no es un enlace: las órdenes pendientes están en su bloque.
- **Órdenes finalizadas.** Entran las órdenes Finalizada cuya fecha de finalización es de los últimos 30 días contando desde el inicio del día de hace 30 días (`LimitesTecnicos.PANEL_GERENTE_COMPRAS_DIAS_ORDENES_FINALIZADAS`); el título del contador y del bloque dicen la ventana. Se ordenan por fecha de finalización descendente y, ante igual fecha, por ID descendente. El cierre es "Manual" si la orden guardó un motivo de finalización y "Automático" si se finalizó sola al recibirse todo. El enlace lleva a todas las órdenes finalizadas, sin la ventana.
- **Tamaño de cada bloque.** Hasta `LimitesTecnicos.PANEL_GERENTE_COMPRAS_FILAS_POR_BLOQUE` filas. El contador muestra el total real, no las filas visibles.
- **Alcance.** El panel no inventa un stock mínimo ni muestra órdenes anuladas.

## 5. Descripción de la pantalla

La pantalla se lee de arriba abajo en cuatro filas. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila.

- **Fila 1, contadores:** cuatro tarjetas (`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4`): "Planificaciones sin orden", "Órdenes pendientes", "Entregas vencidas" (ícono rosa si hay vencidas) y "Finalizadas, últimos 30 días". "Planificaciones sin orden" y "Entregas vencidas" son tarjetas sin enlace; las otras dos llevan a su lista y sin el permiso quedan como texto plano.
- **Fila 2:** "Planificaciones por comprar" a todo el ancho.
- **Fila 3:** "Órdenes de compra pendientes" a todo el ancho.
- **Fila 4:** "Órdenes de compra finalizadas, últimos 30 días" a todo el ancho.
- **Planificaciones por comprar.** Columnas: Planificación ("Plan #7 - Pale Ale"), Inicio estimado, Prioridad y Acciones. En Acciones, "Registrar orden" (botón de registro, ícono `bi-cart-plus`) lleva al formulario de la orden con la planificación elegida, y el ojo "Ver" abre su estado.
- **Órdenes pendientes.** Columnas: Orden ("Orden de compra #7"), Proveedor, Entrega estimada, Recibido ("40 %") y Acciones, con el ojo "Ver".
- **Órdenes finalizadas.** Columnas: Orden, Proveedor, Finalización (fecha y hora), Cierre (Automático o Manual) y Acciones, con el ojo "Ver".
- **Columna Acciones.** El encabezado "Acciones" y el contenedor de los botones van centrados (`text-center` y `justify-center`), con el mismo relleno `px-2 py-3`.
- **Estado vacío:** un sub-bloque sin filas muestra una sola línea de texto gris centrada ("No hay planificaciones pendientes sin orden de compra", "No hay órdenes de compra pendientes", "No hay órdenes de compra finalizadas en el período"), sin tabla, sin encabezado de columnas y sin tarjeta vacía.

## 6. Capas que sostienen el panel

Las capas siguen el orden de la metodología: DTOs, mapper, interfaz del service, validaciones y postcondiciones, implementación y casos de prueba.

**DTOs de respuesta** (paquete `service/response_dto/dashboard`, con Lombok `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`):

- `DashboardGerenteComprasResponseDTO`: `planificacionesSinOrden` (`BloqueDashboardResponseDTO<PlanificacionSinOrdenResponseDTO>`), `ordenesPendientes` (`BloqueDashboardResponseDTO<OrdenCompraEnCursoResponseDTO>`), `ordenesConEntregaVencida` (`long`), `ordenesFinalizadas` (`BloqueDashboardResponseDTO<OrdenCompraFinalizadaResponseDTO>`) y `diasDeOrdenesFinalizadas` (`int`).
- `PlanificacionSinOrdenResponseDTO`: `id`, `etiqueta`, `fechaInicioEstimada` y `prioridad`.
- `OrdenCompraEnCursoResponseDTO`: `id`, `proveedor`, `fechaEntregaEstimada` y `avanceRecepcion` (`int`, de 0 a 100).
- `OrdenCompraFinalizadaResponseDTO`: `id`, `proveedor`, `fechaFinalizacion` y `cierreManual` (`boolean`).

**Mapper** (`util/mapper/dashboard/MapperDashboardGerenteCompras`): de la entidad de la planificación (con su versión de receta cargada) y de la entidad de la orden (con su versión de proveedor cargada) a los tres DTO de fila; el avance llega ya calculado y el cierre manual se deriva del motivo de finalización.

**Interfaz del service** (`service/interfaces/dashboard/IDashboardGerenteComprasServicio`, solo lectura): `DashboardGerenteComprasResponseDTO obtenerDashboardGerenteCompras()`. Calcula todo con la fecha de hoy y el tope de `LimitesTecnicos.PANEL_GERENTE_COMPRAS_FILAS_POR_BLOQUE` filas por bloque. No recibe parámetros: el rol lo decide el Controller. Corre en una única transacción de lectura (`@Transactional(readOnly = true)`).

**Consultas:**

1. `IPlanificacionProduccionRepository.buscarPendientesSinOrdenDeCompraParaPanel(tope)`: página de las planificaciones Pendiente sin ninguna orden de compra que no esté Anulada (`NOT EXISTS`), con su versión de receta cargada, por fecha de inicio estimada y ID ascendentes, con su consulta de conteo.
2. `IOrdenCompraRepository.buscarPendientesParaPanel(tope)`: las órdenes Pendiente con su versión de proveedor cargada, por fecha de entrega estimada y ID ascendentes.
3. `IOrdenCompraRepository.contarPendientesConEntregaVencida(hoy)`: las órdenes Pendiente con entrega anterior a hoy.
4. `IOrdenCompraRepository.buscarFinalizadasDesdeParaPanel(desde, tope)`: las órdenes Finalizada con fecha de finalización desde el límite, con su versión de proveedor cargada, por fecha de finalización e ID descendentes.
5. `IDetalleCompraRepository.buscarConPresentacionDeOrdenes(ids)` e `IIngresoInsumoRepository.buscarRegistradosDeDetalles(ids)`: los ítems con su presentación comercial e insumo, y los ingresos Registrado de esos ítems, una consulta cada una para todas las órdenes visibles. Si no hay órdenes pendientes no se ejecutan.

No se agregó una migración: las consultas se apoyan en los índices existentes.

**Controller.** `HomeController` recibe `IDashboardGerenteComprasServicio` y, con la autoridad `ROLE_GERENTE_DE_COMPRAS`, agrega al modelo `dashboardGerenteCompras`. Si el service rechaza o falla, la pantalla de inicio se muestra sin el panel y con la alerta del motivo, sin redirigir. `OrdenCompraController` ya acepta `idPlanificacion` en `GET /ordenes-compra/nuevo`.

**Plantillas.**

- `home.html`: no tiene tarjetas de resumen. Incluye el panel de cada uno de los seis roles con `sec:authorize="hasRole('...')"` y su fragmento `panel`; el de este rol se incluye con `sec:authorize="hasRole('GERENTE_DE_COMPRAS')"`.
- `home/dashboard-gerente-compras.html`: fragmentos `panel(dashboard)`, `contadores(dashboard)`, `planificacionesSinOrden(dashboard)`, `ordenesPendientes(dashboard)` y `ordenesFinalizadas(dashboard)`, más la pieza compartida `encabezado`.
- Reutiliza `fragments/boton-registrar`, `planificacion_produccion/formato :: fecha, fechaHora y prioridadPlanificacion`.

**Pruebas y casos de prueba.** `../../../test/casos_de_prueba/services/dashboard/CasosPruebaDashboardGerenteCompras.md` y, en `../../../test/casos_de_prueba/controllers/dashboard`, `CasosPruebaMapperDashboardGerenteCompras.md`, `CasosPruebaDashboardGerenteComprasController.md` y `CasosPruebaPlantillasDashboardGerenteCompras.md`.

## 7. Recorridos

| Objetivo | Recorrido | Clics |
| --- | --- | --- |
| Ver cuántas planificaciones esperan una orden de compra | Pantalla de inicio | 0 |
| Registrar la orden de compra de una planificación | Pantalla de inicio, "Registrar orden" de la fila, completar, Registrar | 1 más el formulario |
| Ver cuánto se recibió de una orden pendiente | Pantalla de inicio, columna Recibido | 0 |
| Ver una orden pendiente | Pantalla de inicio, ojo "Ver" de la fila | 1 |
| Ver cuántas órdenes tienen la entrega vencida | Pantalla de inicio, tarjeta "Entregas vencidas" | 0 |
| Ver cómo se cerró una orden reciente | Pantalla de inicio, columna Cierre | 0 |
| Ver todas las órdenes finalizadas | Pantalla de inicio, "Ver todas" de Finalizadas | 1 |
