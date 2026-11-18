# Diseño del panel de control del encargado de depósito

Este documento define el panel que el encargado de depósito ve en la pantalla de inicio (`/home`): qué muestra, qué atajos ofrece, qué permiso exige cada uno, cómo está armado y cuántos clics lleva cada objetivo del usuario. Es un panel de trabajo: no registra ni modifica nada por sí mismo, pero cada fila lleva con un clic a la pantalla donde se hace la operación que corresponde (registrar el ingreso de una orden de compra, registrar el ajuste de un lote de insumo) y a la pantalla de consulta. El comportamiento verificable está en `../../Testing/casos_de_prueba/services/dashboard` y en `../../Testing/casos_de_prueba/controllers/dashboard`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

El panel responde a una pregunta: qué hay que atender hoy en el depósito. Muestra las órdenes de compra que esperan su ingreso, los lotes de insumo vencidos que todavía conservan cantidad y los insumos que se quedaron sin stock disponible.

## 1. Roles y permisos

| Acción | Permiso | Encargado de depósito | Demás roles |
| --- | --- | --- | --- |
| Ver el panel del encargado de depósito | Rol `ENCARGADO_DE_DEPOSITO` | Sí | No (cada rol ve su propio panel) |
| Recorrer las órdenes de compra y ver una orden ("Ver todas", contadores de órdenes, ojo "Ver") | `ORDEN_COMPRA_CONSULTAR` | Sí | No ve el panel |
| Registrar el ingreso de una orden de compra | `INGRESO_INSUMO_REGISTRAR` | Sí | No ve el panel |
| Registrar el ajuste de un lote de insumo | `AJUSTE_INSUMO_REGISTRAR` | Sí | No ve el panel |
| Recorrer el stock de insumos y abrir un insumo o un lote ("Ver todos", contadores de stock, "Abrir") | `STOCK_INSUMO_CONSULTAR` | Sí | No ve el panel |

- El panel se muestra con `sec:authorize="hasRole('ENCARGADO_DE_DEPOSITO')"` y el `HomeController` carga sus datos solo para ese rol: los demás roles no reciben el modelo del panel, de modo que ningún dato de depósito llega a una vista que no lo muestra.
- Cada contador, enlace "Ver todos" y botón de fila lleva `sec:authorize="hasAuthority(...)"` con el permiso de la tabla. Es la misma condición que el `@PreAuthorize` del método de destino, que sigue siendo la fuente de verdad: un acceso no autorizado no devuelve un 403, el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción". Sin el permiso, el contador queda como texto plano.
- El panel no tiene entrada propia en el menú: es el contenido de "Panel de Control" (ícono `bi-speedometer2`, clave activa `dashboard`), que todos los roles ven.

## 2. Inventario de pantallas y rutas

El panel no tiene rutas propias: es un fragmento de la pantalla de inicio. Sus atajos usan rutas de otras pantallas.

| N.º | Pantalla o destino | Ruta | Método | Vista | Permiso del destino |
| --- | --- | --- | --- | --- | --- |
| 1 | Pantalla de inicio con el panel del encargado de depósito | `/home` (también `/`) | GET | `home` con el fragmento `home/dashboard-encargado-deposito :: panel` | Usuario autenticado |
| 2 | Lista de órdenes de compra pendientes | `/ordenes-compra?estado=PENDIENTE` | GET | `orden_compra/orden-lista` | `ORDEN_COMPRA_CONSULTAR` |
| 3 | Detalle de una orden de compra | `/ordenes-compra/{id}` | GET | `orden_compra/orden-detalle` | `ORDEN_COMPRA_CONSULTAR` |
| 4 | Formulario de ingreso por compra con la orden elegida | `/ingresos-insumo/compra/nuevo?idOrdenCompra={id}` | GET | `ingreso_insumo/ingreso-compra-form` | `INGRESO_INSUMO_REGISTRAR` |
| 5 | Formulario de ajuste con el lote elegido | `/ajustes-insumo/nuevo?idLoteInsumo={id}` | GET | `ingreso_insumo/ajuste-form` | `AJUSTE_INSUMO_REGISTRAR` |
| 6 | Stock de insumos con lotes vencidos | `/stock-insumos?situacionStock=CON_VENCIDOS` | GET | `stock_insumo/stock-lista` | `STOCK_INSUMO_CONSULTAR` |
| 7 | Stock de insumos sin stock disponible | `/stock-insumos?situacionStock=SIN_STOCK_DISPONIBLE` | GET | `stock_insumo/stock-lista` | `STOCK_INSUMO_CONSULTAR` |
| 8 | Detalle del stock de un insumo | `/stock-insumos/{id}` | GET | `stock_insumo/stock-detalle-insumo` | `STOCK_INSUMO_CONSULTAR` |
| 9 | Detalle de un lote de insumo | `/stock-insumos/lotes/{id}` | GET | `stock_insumo/stock-detalle-lote` | `STOCK_INSUMO_CONSULTAR` |

El identificador de la orden (`idOrdenCompra`) y el del lote (`idLoteInsumo`) son parámetros aparte de la ruta: no forman parte del formulario. El formulario de ingreso por compra muestra la orden elegida solo si existe y está Pendiente; si no, el selector de orden queda vacío. El formulario de ajuste deja vacío el selector de lote si el lote no existe.

## 3. Reglas específicas

- **Órdenes pendientes.** Entran todas las órdenes de compra en estado Pendiente, por fecha de entrega estimada ascendente (la más vencida arriba) y, ante igual fecha, por ID.
- **Entregas vencidas.** Son las órdenes Pendiente con fecha de entrega estimada anterior a hoy. Nunca superan a las órdenes pendientes. La tarjeta no es un enlace: las órdenes pendientes están en su bloque.
- **Lotes vencidos.** Entran los lotes de insumo de insumos activos con fecha de vencimiento anterior a hoy (un lote que vence hoy todavía es válido) y cantidad actual mayor a 0,00005 (el mismo umbral del filtro "Con lotes vencidos" del stock). Se ordenan por fecha de vencimiento ascendente (el que venció antes arriba) y, ante igual fecha, por ID. El contador cuenta lotes y es una tarjeta sin enlace; el encabezado del bloque lleva al stock filtrado por "Con lotes vencidos", que lista insumos.
- **Insumos sin stock.** Entran los insumos activos cuya cantidad disponible es 0, con el criterio de "Sin stock disponible" de la pantalla de stock: puede haber cantidad reservada o vencida. El sistema no define un stock mínimo, por lo que el panel no informa insumos bajo mínimo.
- **Tamaño de cada bloque.** Hasta `LimitesTecnicos.PANEL_ENCARGADO_DEPOSITO_FILAS_POR_BLOQUE` filas. El contador muestra el total real, no las filas visibles, y "Ver todos" lleva a la lista completa.
- **Permisos.** Todo atajo respeta los permisos de la sección 1 y el servidor los vuelve a exigir en el destino.

## 4. Descripción de la pantalla

La pantalla se lee de arriba abajo en cuatro filas. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila.

- **Fila 1, contadores:** cuatro tarjetas (`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4`) con título en mayúsculas pequeñas, ícono y número grande: "Órdenes pendientes", "Entregas vencidas" (ícono rosa si hay vencidas), "Lotes vencidos" e "Insumos sin stock". "Entregas vencidas" y "Lotes vencidos" son tarjetas sin enlace; las otras dos llevan a su lista y sin el permiso quedan como texto plano.
- **Fila 2:** "Órdenes de compra pendientes" a todo el ancho.
- **Fila 3:** "Insumos sin stock disponible" a todo el ancho.
- **Fila 4:** "Lotes de insumo vencidos" a todo el ancho.
- **Órdenes pendientes.** Columnas: Orden ("Orden de compra #7"), Proveedor, Entrega estimada y Acciones. En Acciones, "Registrar ingreso" (botón de registro, ícono `bi-box-arrow-in-down`) lleva al formulario de ingreso por compra con la orden elegida, y el ojo "Ver" abre el detalle de la orden.
- **Lotes vencidos.** Columnas: Insumo, Lote del proveedor, Vencimiento, Cantidad actual (con la unidad del insumo, alineada a la derecha) y Acciones. En Acciones, "Registrar ajuste" (botón de registro, ícono `bi-sliders2`) lleva al formulario de ajuste con el lote elegido, y "Abrir" lleva al detalle del lote.
- **Insumos sin stock.** Columnas: Insumo, Tipo y Acciones, con "Abrir" hacia el detalle del insumo.
- **Columna Acciones.** El encabezado "Acciones" y el contenedor de los botones van centrados (`text-center` y `justify-center`), con el mismo relleno `px-2 py-3`.
- **Bloques:** cada bloque es una tarjeta con su título, el contador del total real y su enlace "Ver todos" (o "Ver todas") en el encabezado, y hasta 5 filas en la tabla compacta de las listas.
- **Estado vacío:** un sub-bloque sin filas muestra una sola línea de texto gris centrada ("No hay órdenes de compra pendientes", "No hay insumos sin stock disponible", "No hay lotes de insumo vencidos con cantidad"), sin tabla, sin encabezado de columnas y sin tarjeta vacía.

## 5. Capas que sostienen el panel

Las capas siguen el orden de la metodología: DTOs, mapper, interfaz del service, validaciones y postcondiciones, implementación y casos de prueba.

**DTOs de respuesta** (paquete `service/response_dto/dashboard`, con Lombok `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`):

- `DashboardEncargadoDepositoResponseDTO`: `ordenesPendientes` (`BloqueDashboardResponseDTO<OrdenCompraPendienteResponseDTO>`), `ordenesConEntregaVencida` (`long`), `lotesVencidos` (`BloqueDashboardResponseDTO<LoteInsumoVencidoResponseDTO>`) e `insumosSinStock` (`BloqueDashboardResponseDTO<InsumoSinStockResponseDTO>`). Los contadores salen de los `total` de los bloques y de `ordenesConEntregaVencida`.
- `OrdenCompraPendienteResponseDTO`: `id`, `proveedor` (razón social) y `fechaEntregaEstimada`.
- `LoteInsumoVencidoResponseDTO`: `id`, `nombreInsumo`, `unidadDeMedida`, `identificacionLoteProveedor`, `fechaVencimiento` y `cantidadActual`.
- `InsumoSinStockResponseDTO`: `id`, `nombre` y `tipo`.

**Mapper** (`util/mapper/dashboard/MapperDashboardEncargadoDeposito`): de la entidad de la orden (con su versión de proveedor cargada), de la entidad del lote (con su insumo cargado) y del DTO de stock del insumo a los tres DTO de fila.

**Interfaz del service** (`service/interfaces/dashboard/IDashboardEncargadoDepositoServicio`, solo lectura): `DashboardEncargadoDepositoResponseDTO obtenerDashboardEncargadoDeposito()`. Calcula todo con la fecha de hoy y el tope de `LimitesTecnicos.PANEL_ENCARGADO_DEPOSITO_FILAS_POR_BLOQUE` filas por bloque. No recibe parámetros: el rol lo decide el Controller. Corre en una única transacción de lectura (`@Transactional(readOnly = true)`).

**Consultas:**

1. `IOrdenCompraRepository.buscarPendientesParaPanel(tope)`: página de las órdenes Pendiente con su versión de proveedor cargada (`JOIN FETCH`), por fecha de entrega estimada ascendente y, ante igual fecha, por ID, con su consulta de conteo.
2. `IOrdenCompraRepository.contarPendientesConEntregaVencida(hoy)`: órdenes Pendiente con fecha de entrega estimada anterior a hoy.
3. `ILoteInsumoRepository.buscarVencidosConCantidadParaPanel(hoy, tope)`: página de los lotes de insumos activos con vencimiento anterior a hoy y cantidad actual mayor a 0, con el insumo cargado, por vencimiento ascendente y, ante igual fecha, por ID, con su consulta de conteo.
4. `IStockInsumoServicio.filtrarInsumos(null, null, SIN_STOCK_DISPONIBLE, tope)`: los insumos sin stock con su total.

No se agregó una migración: las consultas se apoyan en los índices existentes de las tablas de órdenes y de lotes de insumo.

**Controller.** `HomeController` recibe `IDashboardEncargadoDepositoServicio` y, con la autoridad `ROLE_ENCARGADO_DE_DEPOSITO`, agrega al modelo `dashboardEncargadoDeposito`. Si el service rechaza o falla, la pantalla de inicio se muestra sin el panel y con la alerta del motivo, sin redirigir. `IngresoInsumoController` acepta el parámetro opcional `idOrdenCompra` en `GET /ingresos-insumo/compra/nuevo` para mostrar la orden elegida; `AjusteInsumoController` ya aceptaba `idLoteInsumo` en `GET /ajustes-insumo/nuevo`.

**Plantillas.**

- `home.html`: no tiene tarjetas de resumen. Incluye el panel de cada uno de los seis roles con `sec:authorize="hasRole('...')"` y su fragmento `panel`; el de este rol se incluye con `sec:authorize="hasRole('ENCARGADO_DE_DEPOSITO')"`.
- `home/dashboard-encargado-deposito.html`: fragmentos `panel(dashboard)`, `contadores(dashboard)`, `ordenesPendientes(dashboard)`, `insumosSinStock(dashboard)` y `lotesVencidos(dashboard)`, más la pieza compartida `encabezado`.
- Reutiliza `fragments/boton-registrar`, `fragments/boton-abrir`, `planificacion_produccion/formato :: fecha`, `receta/formato :: medida` y `stock_insumo/formato :: tipoInsumo`.

**Pantalla de stock.** En el detalle del stock de un insumo, cada lote ofrece "Registrar ajuste" (botón de registro con ícono `bi-sliders2`) junto a "Abrir", solo para quien tiene `AJUSTE_INSUMO_REGISTRAR`; lleva al formulario de ajuste con el lote elegido. El gerente de producción, que solo consulta el stock, no lo ve.

**Pruebas y casos de prueba.** `../../Testing/casos_de_prueba/services/dashboard/CasosPruebaDashboardEncargadoDeposito.md` y, en `../../Testing/casos_de_prueba/controllers/dashboard`, `CasosPruebaMapperDashboardEncargadoDeposito.md`, `CasosPruebaDashboardEncargadoDepositoController.md` y `CasosPruebaPlantillasDashboardEncargadoDeposito.md`.

## 6. Recorridos

| Objetivo | Recorrido | Clics |
| --- | --- | --- |
| Ver cuántas órdenes esperan ingreso y cuántas tienen la entrega vencida | Pantalla de inicio | 0 |
| Registrar el ingreso de una orden de compra | Pantalla de inicio, "Registrar ingreso" de la fila, completar, Registrar ingreso | 1 más el formulario |
| Ver el detalle de una orden pendiente | Pantalla de inicio, ojo "Ver" de la fila | 1 |
| Ajustar un lote de insumo vencido | Pantalla de inicio, "Registrar ajuste" de la fila, completar, Registrar ajuste | 1 más el formulario |
| Ajustar un lote desde el stock de un insumo | Stock de Insumos, "Abrir" del insumo, "Registrar ajuste" del lote, completar | 2 más el formulario |
| Ver los insumos sin stock disponible | Pantalla de inicio, "Ver todos" de Sin stock | 1 |
| Ver todas las órdenes pendientes | Pantalla de inicio, "Ver todas" de Pendientes de ingreso | 1 |
