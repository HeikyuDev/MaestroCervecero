# Diseño del panel de control del encargado de distribución

Este documento define el panel que el encargado de distribución ve en la pantalla de inicio (`/home`): qué muestra, qué atajos ofrece, qué permiso exige cada uno, cómo está armado y cuántos clics lleva cada objetivo del usuario. Es un panel de trabajo: no registra nada por sí mismo, pero la fila de cada barril lleva con un clic al formulario de la operación que corresponde (la devolución de un barril despachado o el despacho de un barril con cerveza) y a la pantalla de consulta. El comportamiento verificable está en `docs/test/casos_de_prueba/services/dashboard/` y en `docs/test/casos_de_prueba/controllers/dashboard/`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

El panel responde a una pregunta: qué hay que retirar de los clientes y qué se puede entregar. Muestra los barriles cuya devolución vence hoy o ya venció, los clientes que tienen barriles en su poder y los barriles con cerveza listos para despachar.

## 1. El módulo de distribución en el que se apoya

- Un despacho de barril guarda el barril, el cliente y la fecha de devolución estimada, que es obligatoria. La devolución no guarda una fecha esperada aparte: cierra el ciclo del barril.
- Un barril queda Con cerveza recién al finalizar el lote de su último envasado, y solo un barril Con cerveza se despacha. Por eso todos los barriles Con cerveza están listos para despachar y el panel no necesita volver a mirar el lote.
- Registrar un despacho deja al barril Despachado, es decir, en poder del cliente. Registrar su devolución lo saca de ese estado; anular el despacho o la devolución restituye el estado anterior.
- El despacho vigente de un barril Despachado es el último despacho registrado, sin ninguna operación registrada posterior. Los despachos anulados y los de barriles ya devueltos no cuentan.
- El sistema no define rutas de reparto ni puntaje de clientes: el panel no los muestra.

## 2. Roles y permisos

| Acción | Permiso | Encargado de distribución | Demás roles |
| --- | --- | --- | --- |
| Ver el panel del encargado de distribución | Rol `ENCARGADO_DE_DISTRIBUCION` | Sí | No (cada rol ve su propio panel) |
| Registrar la devolución de un barril | `DEVOLUCION_BARRIL_REGISTRAR` | Sí | No ve el panel |
| Registrar el despacho de un barril | `DESPACHO_BARRIL_REGISTRAR` | Sí | No ve el panel |
| Abrir un barril y recorrer la lista de barriles (contadores, "Ver todos", "Abrir") | Alguno de los permisos de consulta de operaciones de barril | Sí | No ve el panel |
| Abrir un cliente y recorrer la lista de clientes | `CLIENTE_CONSULTAR` | Sí | No ve el panel |

- El panel se muestra con `sec:authorize="hasRole('ENCARGADO_DE_DISTRIBUCION')"` y el `HomeController` carga sus datos solo para ese rol.
- Cada contador, enlace "Ver todos" y botón de fila lleva la condición de permiso de la tabla, la misma del destino. Sin el permiso, el contador queda como texto plano.

## 3. Inventario de pantallas y rutas

El panel no tiene rutas propias: es un fragmento de la pantalla de inicio. Sus atajos usan rutas de otras pantallas.

| N.º | Pantalla o destino | Ruta | Método | Vista | Permiso del destino |
| --- | --- | --- | --- | --- | --- |
| 1 | Pantalla de inicio con el panel del encargado de distribución | `/home` (también `/`) | GET | `home` con el fragmento `home/dashboard-encargado-distribucion :: panel` | Usuario autenticado |
| 2 | Lista de barriles Despachados | `/control-barriles?estadoOperativo=DESPACHADO` | GET | `control_barril/control-lista` | Alguno de los permisos de consulta de operaciones de barril |
| 3 | Lista de barriles Con cerveza | `/control-barriles?estadoOperativo=CON_CERVEZA` | GET | `control_barril/control-lista` | Alguno de los permisos de consulta de operaciones de barril |
| 4 | Panel de un barril | `/control-barriles/{id}` | GET | `control_barril/control-panel` | Alguno de los permisos de consulta de operaciones de barril |
| 5 | Formulario de devolución de un barril | `/control-barriles/{id}/devoluciones/nueva` | GET | `control_barril/control-form` | `DEVOLUCION_BARRIL_REGISTRAR` |
| 6 | Formulario de despacho de un barril | `/control-barriles/{id}/despachos/nuevo` | GET | `control_barril/control-form` | `DESPACHO_BARRIL_REGISTRAR` |
| 7 | Lista de clientes | `/clientes` | GET | `cliente/cliente-lista` | `CLIENTE_CONSULTAR` |
| 8 | Lista de clientes filtrada por nombre | `/clientes?nombre=<nombre>` | GET | `cliente/cliente-lista` | `CLIENTE_CONSULTAR` |

El identificador del barril va en la ruta del formulario: no forma parte del formulario. Si el barril ya no está en el estado que la operación exige, el formulario vuelve al panel del barril con la alerta del motivo, como las demás entradas a esos formularios.

## 4. Reglas específicas

- **Barriles en poder de clientes.** Son los barriles activos en estado Despachado. El número lleva a la lista de barriles Despachados.
- **Devoluciones vencidas.** Son los despachos vigentes con fecha de devolución estimada anterior a hoy. La tarjeta no es un enlace: las devoluciones están en su bloque.
- **Devoluciones que vencen hoy.** Son los despachos vigentes con fecha de devolución estimada de hoy. La tarjeta no es un enlace: las devoluciones están en su bloque.
- **Devoluciones de hoy y vencidas.** Entran los despachos vigentes con fecha de devolución estimada de hoy o anterior, la más atrasada primero y, ante igual fecha, por ID de despacho. El total es la suma de las vencidas y las que vencen hoy. Cada fila muestra el cliente, el barril, la fecha de devolución y los días de atraso (un guion si vence hoy).
- **Clientes con barriles en poder.** Es una fila por cliente, con la cantidad de barriles que tiene en su poder y su próxima fecha de devolución estimada (la menor entre sus barriles). Se ordenan por esa fecha ascendente y, ante igual fecha, por nombre. "Abrir" lleva a la lista de clientes filtrada por el nombre, porque no existe una pantalla de detalle del cliente. El total es la cantidad de clientes distintos.
- **Barriles listos para despachar.** Son los barriles activos Con cerveza, por identificador interno, con la receta y el lote de la cerveza que contienen y su contenido en litros.
- **Tamaño de cada bloque.** Hasta `LimitesTecnicos.PANEL_ENCARGADO_DISTRIBUCION_FILAS_POR_BLOQUE` filas. El contador muestra el total real, no las filas visibles.
- **Alcance.** El panel no muestra rutas de reparto ni puntajes de clientes.

## 5. Descripción de la pantalla

La pantalla se lee de arriba abajo en cuatro filas. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila.

- **Fila 1, contadores:** cuatro tarjetas (`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4`): "En poder de clientes", "Devoluciones vencidas" (ícono rosa si hay vencidas), "Vencen hoy" y "Listos para despachar". "Devoluciones vencidas" y "Vencen hoy" son tarjetas sin enlace; "En poder de clientes" y "Listos para despachar" llevan a la lista de barriles que corresponde y sin el permiso quedan como texto plano.
- **Fila 2:** "Devoluciones de hoy y vencidas" a todo el ancho.
- **Fila 3:** "Clientes con barriles en poder" a todo el ancho.
- **Fila 4:** "Barriles listos para despachar" a todo el ancho.
- **Devoluciones.** Columnas: Cliente, Barril, Devolución, Días de atraso y Acciones. En Acciones, "Registrar devolución" (botón de registro, ícono `bi-box-arrow-in-down-left`) y "Abrir" hacia el panel del barril.
- **Clientes.** Columnas: Cliente, Barriles (alineada a la derecha), Próxima devolución y Acciones, con "Abrir".
- **Barriles listos.** Columnas: Barril, Cerveza (receta y, debajo, lote), Contenido (alineada a la derecha) y Acciones. En Acciones, "Registrar despacho" (botón de registro, ícono `bi-box-arrow-up-right`) y "Abrir".
- **Columna Acciones.** El encabezado "Acciones" y el contenedor de los botones van centrados (`text-center` y `justify-center`), con el mismo relleno `px-2 py-3`.
- **Estado vacío:** un sub-bloque sin filas muestra una sola línea de texto gris centrada ("No hay devoluciones vencidas ni que venzan hoy", "No hay clientes con barriles en su poder", "No hay barriles con cerveza listos para despachar"), sin tabla, sin encabezado de columnas y sin tarjeta vacía.

## 6. Capas que sostienen el panel

Las capas siguen el orden de la metodología: DTOs, mapper, interfaz del service, validaciones y postcondiciones, implementación y casos de prueba.

**DTOs de respuesta** (paquete `service/response_dto/dashboard`, con Lombok `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`):

- `DashboardEncargadoDistribucionResponseDTO`: `barrilesEnPoderDeClientes`, `devolucionesVencidas` y `devolucionesQueVencenHoy` (`long`); `devolucionesPendientes` (`BloqueDashboardResponseDTO<DevolucionPendienteResponseDTO>`); `clientesConBarriles` (`BloqueDashboardResponseDTO<ClienteConBarrilesResponseDTO>`) y `barrilesListos` (`BloqueDashboardResponseDTO<BarrilListoResponseDTO>`).
- `DevolucionPendienteResponseDTO`: `idBarril`, `identificadorInterno`, `nombreCliente`, `fechaDevolucionEstimada` y `diasDeAtraso` (`int`).
- `ClienteConBarrilesResponseDTO`: `idCliente`, `nombre`, `cantidadBarriles` (`long`) y `proximaFechaDevolucion`.
- `BarrilListoResponseDTO`: `id`, `identificadorInterno`, `nombreReceta`, `codigoLote` y `contenidoActual`.

**Mapper** (`util/mapper/dashboard/MapperDashboardEncargadoDistribucion`): de un despacho (con su barril y su cliente cargados) a `DevolucionPendienteResponseDTO`, con los días de atraso calculados con la misma regla del cronograma de producción; de una fila agrupada por cliente a `ClienteConBarrilesResponseDTO`; y de un barril de la pantalla de control a `BarrilListoResponseDTO`.

**Interfaz del service** (`service/interfaces/dashboard/IDashboardEncargadoDistribucionServicio`, solo lectura): `DashboardEncargadoDistribucionResponseDTO obtenerDashboardEncargadoDistribucion()`. Calcula todo con la fecha de hoy y el tope de `LimitesTecnicos.PANEL_ENCARGADO_DISTRIBUCION_FILAS_POR_BLOQUE` filas por bloque. No recibe parámetros: el rol lo decide el Controller. Corre en una única transacción de lectura (`@Transactional(readOnly = true)`).

**Servicios y consultas:**

- `IControlBarrilServicio.filtrarBarrilesParaControl(null, null, estado, página)` con `DESPACHADO` (página de un registro, para el contador) y con `CON_CERVEZA` (primera página, para los barriles listos y su total).
- `IDespachoBarrilRepository.buscarVigentesConDevolucionHastaParaPanel(hoy, tope)`: página de los despachos vigentes con devolución estimada hasta hoy, con el barril y el cliente cargados, por fecha de devolución estimada e ID ascendentes, con su consulta de conteo.
- `IDespachoBarrilRepository.contarVigentesConDevolucionAnteriorA(hoy)` y `contarVigentesConDevolucionEn(hoy)`: las devoluciones vencidas y las que vencen hoy.
- `IDespachoBarrilRepository.buscarClientesConBarrilesEnPoderParaPanel(tope)`: una fila por cliente con la cantidad de despachos vigentes y la menor fecha de devolución estimada, agrupada por cliente, con su conteo de clientes distintos.

La condición de despacho vigente es común a las cuatro consultas: despacho registrado de un barril activo en estado Despachado sin ninguna operación registrada posterior. No se agregó una migración: las consultas se apoyan en los índices existentes de las operaciones de barril.

**Controller.** `HomeController` recibe `IDashboardEncargadoDistribucionServicio` y, con la autoridad `ROLE_ENCARGADO_DE_DISTRIBUCION`, agrega al modelo `dashboardEncargadoDistribucion`. Si el service rechaza o falla, la pantalla de inicio se muestra sin el panel y con la alerta del motivo, sin redirigir.

**Plantillas.**

- `home.html`: no tiene tarjetas de resumen. Incluye el panel de cada uno de los seis roles con `sec:authorize="hasRole('...')"` y su fragmento `panel`; el de este rol se incluye con `sec:authorize="hasRole('ENCARGADO_DE_DISTRIBUCION')"`.
- `home/dashboard-encargado-distribucion.html`: fragmentos `panel(dashboard)`, `contadores(dashboard)`, `devolucionesPendientes(dashboard)`, `clientesConBarriles(dashboard)` y `barrilesListos(dashboard)`, más la pieza compartida `encabezado`.
- Reutiliza `fragments/boton-registrar`, `fragments/boton-abrir`, `planificacion_produccion/formato :: fecha`, `lote/formato :: duracion` y `receta/formato :: medida`.

**Pruebas y casos de prueba.** `docs/test/casos_de_prueba/services/dashboard/CasosPruebaDashboardEncargadoDistribucion.md` y, en `docs/test/casos_de_prueba/controllers/dashboard/`, `CasosPruebaMapperDashboardEncargadoDistribucion.md`, `CasosPruebaDashboardEncargadoDistribucionController.md` y `CasosPruebaPlantillasDashboardEncargadoDistribucion.md`.

## 7. Recorridos

| Objetivo | Recorrido | Clics |
| --- | --- | --- |
| Ver cuántos barriles tienen los clientes y cuántas devoluciones vencieron | Pantalla de inicio | 0 |
| Registrar la devolución de un barril vencido | Pantalla de inicio, "Registrar devolución" de la fila, completar, Registrar | 1 más el formulario |
| Ver qué clientes tienen barriles y cuándo vence el próximo | Pantalla de inicio, "Clientes con barriles en poder" | 0 |
| Encontrar a un cliente para coordinar el retiro | Pantalla de inicio, "Abrir" del cliente | 1 |
| Registrar el despacho de un barril con cerveza | Pantalla de inicio, "Registrar despacho" de la fila, completar, Registrar | 1 más el formulario |
| Ver todos los barriles en poder de clientes | Pantalla de inicio, "Ver todos" de Barriles a retirar | 1 |
