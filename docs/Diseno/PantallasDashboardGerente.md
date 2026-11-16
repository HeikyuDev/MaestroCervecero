# Diseño del panel de control del gerente de producción

Este documento define el panel que el gerente de producción ve en la pantalla de inicio (`/home`): qué muestra, qué enlaces ofrece, qué permiso exige cada uno, cómo está armado y cuántos clics lleva cada objetivo del usuario. El panel es de solo lectura: no registra ni modifica nada; cada enlace lleva a la pantalla de consulta correspondiente. El comportamiento verificable está en `docs/test/casos_de_prueba/services/dashboard/` y en `docs/test/casos_de_prueba/controllers/dashboard/`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

El panel responde a una pregunta: cuál es el estado de la fábrica. Muestra qué se está produciendo y en qué etapa, qué va atrasado, cómo están las colas de los fermentadores, cuántos equipos y barriles están en cada estado, y qué planificaciones esperan. Los lotes a iniciar son responsabilidad del operario de producción y no figuran en este panel.

## 1. Roles y permisos

| Acción | Permiso | Gerente de producción | Operario de producción | Demás roles |
| --- | --- | --- | --- | --- |
| Ver el panel del gerente | Rol `GERENTE_DE_PRODUCCION` | Sí | No (ve su propio panel) | No (cada rol ve su propio panel) |
| Abrir un lote, recorrer la lista de lotes y el cronograma ("Ver todos", "Ver cronograma", contadores de lotes) | `LOTE_CONSULTAR` | Sí | No ve el panel | No |
| Ver el identificador de un equipo como enlace a su detalle | `EQUIPAMIENTO_CONSULTAR` | Sí | No ve el panel | No (texto plano) |
| Recorrer la lista de fermentadores (contador de fermentadores) | `LIMPIEZA_EQUIPAMIENTO_CONSULTAR` | Sí | No ve el panel | No |
| Ver una planificación y recorrer la lista de planificaciones | `PLANIFICACION_PRODUCCION_CONSULTAR` | Sí | No ve el panel | No |

- El panel se muestra con `sec:authorize="hasRole('GERENTE_DE_PRODUCCION')"` y el `HomeController` carga sus datos solo para ese rol: los demás roles no reciben el modelo del panel, de modo que ningún dato de planta llega a una vista que no lo muestra.
- Cada contador, enlace "Ver todos" y botón "Abrir" lleva `sec:authorize="hasAuthority(...)"` con el permiso de la tabla. Es la misma condición que el `@PreAuthorize` del método de destino, que sigue siendo la fuente de verdad: un acceso no autorizado no devuelve un 403, el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción". Sin el permiso, el contador queda como texto plano.
- El panel no tiene entrada propia en el menú: es el contenido de "Panel de Control" (ícono `bi-speedometer2`, clave activa `dashboard`), que todos los roles ven.
- El panel no tiene botones de registro: registrar limpiezas, mantenimientos o consumos es tarea del operario de producción.

## 2. Inventario de pantallas y rutas

El panel no tiene rutas propias: es un fragmento de la pantalla de inicio. Sus enlaces usan rutas de otras pantallas.

| N.º | Pantalla o destino | Ruta | Método | Vista | Permiso del destino |
| --- | --- | --- | --- | --- | --- |
| 1 | Pantalla de inicio con el panel del gerente | `/home` (también `/`) | GET | `home` con el fragmento `home/dashboard-gerente :: panel` | Usuario autenticado |
| 2 | Lista de lotes en ejecución | `/lotes?estado=EN_EJECUCION` | GET | `lote/lote-lista` | `LOTE_CONSULTAR` |
| 3 | Lista de lotes en ejecución en una etapa | `/lotes?estado=EN_EJECUCION&etapaActual=<etapa>` | GET | `lote/lote-lista` | `LOTE_CONSULTAR` |
| 5 | Panel de trabajo de un lote en su etapa en curso | `/lotes/{id}?etapa=<etapa en minúsculas>` | GET | `lote/lote-detalle` | `LOTE_CONSULTAR` |
| 6 | Panel de trabajo de un lote pendiente | `/lotes/{id}` | GET | `lote/lote-detalle` | `LOTE_CONSULTAR` |
| 7 | Cronograma de producción | `/cronograma-produccion` | GET | `cronograma/cronograma` | `LOTE_CONSULTAR` |
| 8 | Lista de fermentadores | `/control-equipamiento?tipo=FERMENTADOR` | GET | `control_equipamiento/control-lista` | `LIMPIEZA_EQUIPAMIENTO_CONSULTAR` |
| 9 | Lista de planificaciones pendientes | `/planificaciones-produccion?estado=PENDIENTE` | GET | `planificacion_produccion/planificacion-lista` | `PLANIFICACION_PRODUCCION_CONSULTAR` |
| 12 | Estado actual de una planificación | `/planificaciones-produccion/{id}` | GET | `planificacion_produccion/planificacion-detalle` | `PLANIFICACION_PRODUCCION_CONSULTAR` |

## 3. Reglas específicas

- **Solo lectura.** El panel no ofrece ninguna acción que registre o modifique datos.
- **Lotes en ejecución.** Entran todos los lotes en estado En ejecución.
- **Lotes atrasados.** Son los lotes En ejecución con fecha de finalización estimada anterior a hoy. Nunca superan a los lotes en ejecución. La tarjeta no es un enlace: los lotes atrasados se ven en la producción en curso y en la cola de fermentadores.
- **Atraso de un lote.** Es la cantidad de días entre la fecha de finalización estimada de un lote En ejecución y hoy, cuando esa fecha es anterior a hoy; vale 0 en cualquier otro caso. Es la misma regla del cronograma de producción (`MetodosCronograma.calcularDiasDeAtraso`). La producción en curso no muestra el atraso en sus filas: el contador "Lotes atrasados" lo informa y la cola de fermentadores muestra "Atrasado N días" en rosa junto al lote en ejecución que lo tiene.
- **Prioridad.** Cada lote muestra la prioridad de su planificación (Alta, Media o Baja) con la misma insignia de las planificaciones.
- **Fermentadores ocupados.** Es la cantidad de fermentadores activos En uso sobre el total de fermentadores activos, en cualquier estado operativo.
- **Fuera de servicio.** Es la suma de equipos y barriles activos En limpieza o En mantenimiento. La tarjeta no es un enlace.
- **Producción en curso.** Los lotes En ejecución se agrupan por etapa en curso, en el orden de las etapas (Molienda, Maceración, Hervido, Fermentación, Maduración, Envasado); una etapa sin lotes no se muestra. Dentro de cada etapa, los lotes van por fecha de finalización estimada ascendente (el más atrasado arriba) y, ante igual fecha, por ID.
- **Cola de fermentadores.** Cada fermentador activo muestra su lote En ejecución (si lo tiene, con su atraso), su próximo lote Pendiente (el de fecha de inicio estimada más temprana, como ordena el cronograma) y la cantidad de lotes Pendiente que esperan. Los fermentadores van en el orden natural por identificador de Control de Equipamientos. El enlace "Ver cronograma" lleva al diagrama completo.
- **Planificaciones pendientes.** Son las planificaciones de estado Pendiente.
- **Tamaño de cada bloque.** Hasta `LimitesTecnicos.PANEL_GERENTE_FILAS_POR_BLOQUE` filas. El contador muestra el total real, no las filas visibles, y "Ver todos" lleva a la lista filtrada completa.
- **Permisos.** Todo enlace respeta los permisos de la sección 1 y el servidor los vuelve a exigir en el destino.

## 4. Descripción de la pantalla

La pantalla se lee de arriba abajo en cuatro filas. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila.

- **Fila 1, contadores:** cuatro tarjetas (`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4`) con título en mayúsculas pequeñas, ícono y número grande: "Lotes en ejecución", "Lotes atrasados" (ícono rosa si hay atrasados), "Fermentadores ocupados" (con el formato "3 / 5") y "Fuera de servicio". "Lotes atrasados" es una tarjeta sin enlace; "Lotes en ejecución" y "Fermentadores ocupados" lleva a la lista de fermentadores y sin el permiso queda como texto plano. "Fuera de servicio" es una tarjeta sin enlace.
- **Fila 2, producción:** "Producción en curso" a todo el ancho.
- **Fila 3:** "Cola de fermentadores" a todo el ancho.
- **Fila 4:** "Planificaciones pendientes" a todo el ancho.
- **Bloques:** cada bloque es una tarjeta con su título; los subgrupos con filas muestran el contador del total real y su enlace "Ver todos" en el encabezado, y hasta `LimitesTecnicos.PANEL_GERENTE_FILAS_POR_BLOQUE` filas en la tabla compacta de las listas. Los lotes se abren con "Abrir" y las planificaciones se consultan con el ojo "Ver".
- **Alineación de la columna Acciones:** cada celda de acciones tiene un solo botón, que la hoja de estilos centra; por eso el encabezado "Acciones" y el botón van centrados (`text-center` en el encabezado, `justify-center` en el contenedor del botón) con el mismo relleno `px-2 py-3`. Vale igual para el panel del operario.
- **Subgrupos apilados:** los subgrupos de un mismo bloque van uno debajo del otro, separados por una línea; ningún par de subgrupos de un bloque va lado a lado.
- **Estado vacío:** un subgrupo sin filas muestra una sola línea de texto gris centrada ("No hay lotes en ejecución", "No hay fermentadores registrados", "No hay planificaciones pendientes"), sin tabla, sin encabezado de columnas y sin tarjeta vacía.

## 5. Capas que sostienen el panel

Las capas siguen el orden de la metodología: DTOs, mapper, interfaz del service, validaciones y postcondiciones, implementación y casos de prueba.

**DTOs de respuesta** (paquete `service/response_dto/dashboard`, con Lombok `@Getter @Builder @NoArgsConstructor @AllArgsConstructor` como los demás):

- `DashboardGerenteResponseDTO`. Campos: `lotesEnEjecucion`, `lotesAtrasados`, `fermentadoresOcupados`, `fermentadoresTotal` y `fueraDeServicio` (todos `long`); `produccionEnCurso` (`List<EtapaEnCursoResponseDTO>`); `colaDeFermentadores` (`BloqueDashboardResponseDTO<ColaFermentadorResponseDTO>`); `planificacionesPendientes` (`BloqueDashboardResponseDTO<PlanificacionPendienteResponseDTO>`).
- `BloqueDashboardResponseDTO<T>`: `total` (`long`) y `filas` (`List<T>`), el mismo del panel del operario.
- `EtapaEnCursoResponseDTO`: `etapa` (`TipoEtapa`) y `lotes` (`BloqueDashboardResponseDTO<LoteEnProduccionResponseDTO>`).
- `LoteEnProduccionResponseDTO`: `id`, `identificadorInterno`, `nombreReceta`, `etapaActual` (nula en un lote Pendiente), `idFermentador`, `identificadorInternoFermentador`, `fechaInicioEstimada`, `fechaFinalizacionEstimada`, `diasDeAtraso` (`int`) y `prioridad` (`PrioridadPlanificacion`).
- `ColaFermentadorResponseDTO`: `id`, `identificadorInterno`, `estadoOperativo`, `loteEnEjecucion` y `proximoLote` (`LoteEnProduccionResponseDTO`, nulos si no hay) y `lotesPendientes` (`long`).
- `PlanificacionPendienteResponseDTO`: `id`, `etiqueta`, `prioridad`, `fechaInicioEstimada` y `fechaFinalizacionEstimada`.

**Mapper** (`util/mapper/dashboard/MapperDashboardGerente`): de un lote del listado a `LoteEnProduccionResponseDTO` (con el atraso ya calculado), de un fermentador de control con sus lotes a `ColaFermentadorResponseDTO` y de una planificación a `PlanificacionPendienteResponseDTO`.

**Interfaz del service** (`service/interfaces/dashboard/IDashboardGerenteServicio`, solo lectura): `DashboardGerenteResponseDTO obtenerDashboardGerente()`. Calcula todo con la fecha de hoy y el tope de `LimitesTecnicos.PANEL_GERENTE_FILAS_POR_BLOQUE` filas por bloque. No recibe parámetros: el rol lo decide el Controller. Corre en una única transacción de lectura (`@Transactional(readOnly = true)`).

**Servicios que compone:**

- `IControlEquipamientoServicio.filtrarEquipamientosParaControl`: los fermentadores ocupados y los equipos En limpieza y En mantenimiento usan una página de un registro y leen `getTotalElements()`; la cola usa la primera página de fermentadores.
- `IControlBarrilServicio.filtrarBarrilesParaControl`: los barriles En limpieza y En mantenimiento, con una página de un registro.
- `IPlanificacionProduccionServicio.filtrarPlanificacionesProduccion(null, PENDIENTE, null, tope)`: las planificaciones pendientes.

**Consultas** (en `ILoteRepository`):

1. `buscarIdsDeLotesEnEjecucionParaPanel`: el total de lotes en ejecución (página de un registro).
2. `contarLotesEnEjecucionAtrasados(hoy)`: cantidad de lotes En ejecución con fecha de finalización estimada anterior a hoy.
3. `buscarIdsDeLotesEnEjecucionDeEtapaParaPanel(etapa, tope)`: página de IDs de los lotes En ejecución cuya etapa en curso es la indicada, por fecha de finalización estimada ascendente y, ante igual fecha, por ID, con su consulta de conteo. El service la ejecuta una vez por tipo de etapa y omite las etapas sin lotes.
4. `buscarParaListadoPorIds`: carga con sus relaciones los lotes de cada página, en el orden de los IDs. Si la carga no devuelve alguno de los IDs de la página, ese lote no se muestra y el service deja una advertencia en el log.
5. `buscarVivosDeFermentadores`: los lotes vivos de los fermentadores visibles, en una sola consulta, ordenados por fecha de inicio estimada y, ante igual fecha, por ID.

Las consultas 1 y 2 usan el índice de `V20__indices_consultas_panel_y_ciclo_de_vida.sql` (estado y fecha de finalización estimada); la 3 filtra por el estado del lote y la etapa en curso. No hace falta una migración nueva.

**Controller.** `HomeController` recibe `IDashboardOperarioServicio` e `IDashboardGerenteServicio`. Con la autoridad `ROLE_OPERARIO_DE_PRODUCCION` agrega al modelo `dashboardOperario`; con `ROLE_GERENTE_DE_PRODUCCION` agrega `dashboardGerente` con `obtenerDashboardGerente()`. Para los demás roles no consulta nada. Si el service rechaza o falla, la pantalla de inicio se muestra sin el panel y con la alerta del motivo, sin redirigir: el `ControllerAdvices` redirige a la raíz, que es esta misma pantalla, y el fallo se repetiría en bucle.

**Plantillas.**

- `home.html`: no tiene tarjetas de resumen. Incluye el panel de cada uno de los seis roles con `sec:authorize="hasRole('...')"` y su fragmento `panel`; el de este rol se incluye con `sec:authorize="hasRole('GERENTE_DE_PRODUCCION')"`.
- `home/dashboard-gerente.html`: fragmentos `panel(dashboard)`, `contadores(dashboard)`, `produccionEnCurso(dashboard)`, `colaDeFermentadores(dashboard)` y `planificacionesPendientes(dashboard)`, más la pieza compartida `encabezado`.
- Reutiliza `fragments/boton-abrir`, `fragments/enlace-equipo`, `lote/formato :: duracion`, `planificacion_produccion/formato :: fecha` y `prioridadPlanificacion`, y los mismos estilos de tarjeta y de tabla de las listas.

**Pruebas y casos de prueba.** `docs/test/casos_de_prueba/services/dashboard/CasosPruebaDashboardGerente.md`, `docs/test/casos_de_prueba/controllers/dashboard/CasosPruebaMapperDashboardGerente.md`, `CasosPruebaDashboardGerenteController.md` y `CasosPruebaPlantillasDashboardGerente.md` documentan los casos con los mismos IDs de los tests.

## 6. Recorridos

| Objetivo | Recorrido | Clics |
| --- | --- | --- |
| Ver cuántos lotes van atrasados | Pantalla de inicio | 0 |
| Ver qué lotes están en cada etapa | Pantalla de inicio, "Producción en curso" | 0 |
| Entrar a un lote atrasado | Pantalla de inicio, "Abrir" del lote | 1 |
| Ver cuántos fermentadores están ocupados | Pantalla de inicio | 0 |
| Ver qué viene después en un fermentador | Pantalla de inicio, "Cola de fermentadores" | 0 |
| Ver el cronograma completo | Pantalla de inicio, "Ver cronograma" | 1 |
| Ver las planificaciones pendientes | Pantalla de inicio, "Ver todas" de Planificaciones pendientes | 1 |
