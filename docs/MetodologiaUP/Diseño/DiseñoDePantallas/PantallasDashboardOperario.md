# Diseño del panel de control del operario de producción

Este documento define el panel que el operario de producción ve en la pantalla de inicio (`/home`): qué muestra, qué atajos ofrece, qué permiso exige cada uno, cómo está armado y cuántos clics lleva cada objetivo del usuario. El panel es informativo y de atajos: no registra ni modifica nada por sí mismo; cada acción se hace en la pantalla de trabajo a la que el atajo lleva. El comportamiento verificable está en `../../../test/casos_de_prueba/services/dashboard` y en `../../../test/casos_de_prueba/controllers/dashboard`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

El panel responde a una pregunta: qué hay que hacer hoy en la planta. "Hoy" incluye lo que corresponde a la fecha de hoy, todo lo atrasado y todo lo que está en curso: nada se oculta porque su fecha ya pasó. Cada elemento del panel es un atajo directo a la pantalla de trabajo correspondiente.

## 1. Roles y permisos

| Acción | Permiso | Operario de producción | Gerente de producción | Demás roles |
| --- | --- | --- | --- | --- |
| Ver el panel del operario | Rol `OPERARIO_DE_PRODUCCION` | Sí | No (ve su propio panel, descrito en `PantallasDashboardGerente.md`) | No (cada rol ve su propio panel) |
| Abrir un lote y recorrer la lista de lotes ("Ver todos", contadores de lotes) | `LOTE_CONSULTAR` | Sí | No ve el panel | No |
| Ver el identificador de un equipo como enlace a su detalle | `EQUIPAMIENTO_CONSULTAR` | Sí | No ve el panel | No (texto plano) |
| Recorrer la lista de equipos ("Ver todos", contadores de equipos) | `LIMPIEZA_EQUIPAMIENTO_CONSULTAR` | Sí | No ve el panel | No |
| Recorrer la lista de barriles ("Ver todos", contadores de barriles) | Alguno de los permisos de consulta de operaciones de barril | Sí | No ve el panel | No |
| Registrar la limpieza de un equipo | `LIMPIEZA_EQUIPAMIENTO_REGISTRAR` | Sí | No | No |
| Registrar la limpieza de un barril | `LIMPIEZA_BARRIL_REGISTRAR` | Sí | No | No |
| Registrar el mantenimiento de un equipo | `MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR` | Sí | No | No |
| Registrar el mantenimiento de un barril | `MANTENIMIENTO_BARRIL_REGISTRAR` | Sí | No | No |

- El panel se muestra con `sec:authorize="hasRole('OPERARIO_DE_PRODUCCION')"` y el `HomeController` carga sus datos solo para ese rol: los demás roles no reciben el modelo del panel, de modo que ningún dato de planta llega a una vista que no lo muestra.
- Cada contador, enlace "Ver todos" y botón de fila lleva `sec:authorize="hasAuthority(...)"` con el permiso de la tabla. Es la misma condición que el `@PreAuthorize` del método de destino, que sigue siendo la fuente de verdad: un acceso no autorizado no devuelve un 403, el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción".
- El panel no tiene entrada propia en el menú: es el contenido de "Panel de Control" (ícono `bi-speedometer2`, clave activa `dashboard`), que todos los roles ven. El título de la pestaña del navegador no cambia.
- El gerente de producción no ve este panel porque no registra ni opera: su pantalla de inicio muestra el panel de solo lectura descrito en `shboardGerente.md`.

## 2. Inventario de pantallas y rutas

El panel no tiene rutas propias: es un fragmento de la pantalla de inicio. Sus atajos usan rutas de otras pantallas.

| N.º | Pantalla o destino | Ruta | Método | Vista | Permiso del destino |
| --- | --- | --- | --- | --- | --- |
| 1 | Pantalla de inicio con el panel del operario | `/home` (también `/`) | GET | `home` con el fragmento `home/dashboard-operario :: panel` | Usuario autenticado |
| 2 | Lista de lotes en ejecución | `/lotes?estado=EN_EJECUCION` | GET | `lote/lote-lista` | `LOTE_CONSULTAR` |
| 3 | Lista de lotes pendientes | `/lotes?estado=PENDIENTE` | GET | `lote/lote-lista` | `LOTE_CONSULTAR` |
| 4 | Panel de trabajo de un lote en su etapa en curso | `/lotes/{id}?etapa=<etapa en curso en minúsculas>` | GET | `lote/lote-detalle` | `LOTE_CONSULTAR` |
| 5 | Panel de trabajo de un lote pendiente | `/lotes/{id}` | GET | `lote/lote-detalle` | `LOTE_CONSULTAR` |
| 6 | Lista de equipos en limpieza | `/control-equipamiento?estadoOperativo=EN_LIMPIEZA` | GET | `control_equipamiento/control-lista` | `LIMPIEZA_EQUIPAMIENTO_CONSULTAR` |
| 7 | Lista de barriles en limpieza | `/control-barriles?estadoOperativo=EN_LIMPIEZA` | GET | `control_barril/control-lista` | Alguno de los permisos de consulta de operaciones de barril |
| 8 | Lista de equipos en mantenimiento | `/control-equipamiento?estadoOperativo=EN_MANTENIMIENTO` | GET | `control_equipamiento/control-lista` | `LIMPIEZA_EQUIPAMIENTO_CONSULTAR` |
| 9 | Lista de barriles en mantenimiento | `/control-barriles?estadoOperativo=EN_MANTENIMIENTO` | GET | `control_barril/control-lista` | Alguno de los permisos de consulta de operaciones de barril |
| 10 | Formulario de limpieza de un equipo | `/control-equipamiento/{id}/limpiezas/nueva` | GET | `control_equipamiento/control-form` | `LIMPIEZA_EQUIPAMIENTO_REGISTRAR` |
| 11 | Formulario de limpieza de un barril | `/control-barriles/{id}/limpiezas/nueva` | GET | `control_barril/control-form` | `LIMPIEZA_BARRIL_REGISTRAR` |
| 12 | Formulario de mantenimiento de un equipo | `/control-equipamiento/{id}/mantenimientos/nuevo` | GET | `control_equipamiento/control-form` | `MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR` |
| 13 | Formulario de mantenimiento de un barril | `/control-barriles/{id}/mantenimientos/nuevo` | GET | `control_barril/control-form` | `MANTENIMIENTO_BARRIL_REGISTRAR` |

El parámetro `etapa` de la ruta 4 es el nombre de la etapa en minúsculas (`molienda`, `maceracion`, `hervido`, `fermentacion`, `maduracion`, `envasado`), el mismo que usan las pestañas de etapa del lote. Un lote en ejecución abre por defecto su etapa en curso, de modo que el parámetro y el valor por defecto llevan al mismo lugar.

Un formulario de registro cuyo equipo o barril ya no está en el estado que la operación exige (por ejemplo, otra persona registró la limpieza entre la carga del panel y el clic) vuelve al panel del equipo o del barril con la alerta `warning` del motivo, como lo hacen las demás entradas a esos formularios. Un ID inexistente responde como recurso no encontrado y el `ControllerAdvices` redirige con la alerta.

## 3. Reglas específicas

- **Hoy.** El panel muestra lo que corresponde a hoy, todo lo atrasado y todo lo que está en curso. Ningún elemento desaparece porque su fecha pasó.
- **Lotes en ejecución.** Entran todos los lotes en estado En ejecución. Se ordenan por fecha de finalización estimada ascendente (el más atrasado arriba) y, ante igual fecha, por ID.
- **Lotes a iniciar.** Entran los lotes en estado Pendiente cuya fecha de inicio estimada es hoy o anterior. Se ordenan por fecha de inicio estimada ascendente y, ante igual fecha, por ID. Iniciar un lote sigue las reglas de siempre (stock suficiente de cada insumo y equipos disponibles): si un lote listado no puede iniciarse todavía, por ejemplo porque su fermentador sigue ocupado por un lote atrasado, el panel de trabajo del lote muestra el motivo del rechazo.
- **Limpieza pendiente.** La limpieza solo se registra con el equipo o el barril en estado En limpieza. Por eso "pendiente de limpieza" significa "está en limpieza ahora": entran los equipos activos y los barriles activos en estado En limpieza. Un equipo o un barril En uso por un lote no entra.
- **Mantenimiento pendiente.** Un equipo o un barril pasa a En mantenimiento al registrar una falla o al registrar una limpieza que alcanza el máximo de usos antes del mantenimiento. El mantenimiento solo se registra con el equipo o el barril en ese estado. Entran los equipos activos y los barriles activos en estado En mantenimiento.
- **Causa del mantenimiento.** Se deriva de la última operación registrada del equipo o del barril (la de mayor fecha y, ante igual fecha, la de mayor ID; las anuladas no cuentan): una falla indica "Falla" y una limpieza que dejó el estado En mantenimiento (`estadoOperativoResultante`) indica "Preventivo". Si no hay operación que lo explique, la causa se muestra como "-". Anular un mantenimiento devuelve al equipo a En mantenimiento y la causa vuelve a ser la de la operación anterior.
- **Orden de equipos y barriles.** Orden natural por identificador, igual que en Control de Equipamientos y Control de Barriles.
- **Tamaño de cada bloque.** Hasta `LimitesTecnicos.PANEL_OPERARIO_FILAS_POR_BLOQUE` filas. El contador muestra el total real, no las filas visibles, y "Ver todos" lleva a la lista filtrada completa.
- **Permisos.** Todo atajo respeta los permisos del operario (sección 1) y el servidor los vuelve a exigir en el destino.

## 4. Descripción de cada pantalla

**Pantalla de inicio del operario.** Conserva el título "Panel de Control" y el subtítulo de bienvenida. Debajo, cuatro filas que se leen de arriba abajo: los contadores, los lotes de hoy, las limpiezas y el mantenimiento. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila. Cada bloque es una tarjeta (`bg-white border border-slate-200 rounded-xl shadow-sm`) con su título, su contador y su enlace "Ver todos" en el encabezado; la tabla del bloque usa el mismo estilo compacto de las listas (`px-2 py-3`, encabezado gris suave en mayúsculas).

**Fila 1: contadores.** Una banda horizontal de cuatro tarjetas (`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4`), con el título en mayúsculas pequeñas, el número grande y un ícono, como las tarjetas de los demás paneles:

1. **Lotes en ejecución** (`bi-play-circle`): toda la tarjeta es un enlace a `/lotes?estado=EN_EJECUCION`.
2. **Lotes a iniciar** (`bi-hourglass-split`): toda la tarjeta es un enlace a `/lotes?estado=PENDIENTE`. El número cuenta solo los lotes cuya fecha de inicio estimada es hoy o anterior.
3. **En limpieza** (`bi-stars`): el número es la suma de equipos y barriles. Debajo, dos enlaces en línea: "N equipos" (a `/control-equipamiento?estadoOperativo=EN_LIMPIEZA`) y "M barriles" (a `/control-barriles?estadoOperativo=EN_LIMPIEZA`).
4. **Pendientes de mantenimiento** (`bi-wrench`): el número es la suma de equipos y barriles. Debajo, "N equipos" (a `/control-equipamiento?estadoOperativo=EN_MANTENIMIENTO`) y "M barriles" (a `/control-barriles?estadoOperativo=EN_MANTENIMIENTO`).

Un contador en 0 se muestra igual, con el número 0. Los enlaces sin permiso se muestran como texto plano.

**Fila 2: "Lotes de hoy".** Un bloque a todo el ancho con dos sub-bloques, uno debajo del otro:

- **En ejecución.** Título "En ejecución", contador y "Ver todos" (a `/lotes?estado=EN_EJECUCION`). Hasta el tope de filas del bloque, con las columnas:
  - **Lote.** El código (`identificadorInterno`) y, debajo, el nombre de la receta.
  - **Etapa en curso.** El nombre de la etapa.
  - **Fermentador.** El identificador con `fragments/enlace-equipo :: enlace(id, identificador)`.
  - **Acciones.** El botón estándar "Abrir" (`fragments/boton-abrir :: abrir(url, etiquetaAccesible)`) a `/lotes/{id}?etapa=<etapa en curso>`, con `aria-label` "Abrir LOT-R1-02".
- **A iniciar.** Título "A iniciar", contador y "Ver todos" (a `/lotes?estado=PENDIENTE`). Hasta el tope de filas del bloque con el mismo estilo: Lote (código y receta), Fermentador, Inicio estimado (`planificacion_produccion/formato :: fecha`) y Acciones con "Abrir" a `/lotes/{id}`, donde el operario inicia el lote.

**Fila 3: "Limpiezas pendientes".** Un bloque a todo el ancho con dos sub-bloques separados, cada uno con su contador y su "Ver todos":

- **Equipos.** Hasta el tope de filas del bloque: identificador con `fragments/enlace-equipo`, tipo del equipo (Molino, Macerador, Olla de hervor, Fermentador) y la acción. "Ver todos" lleva a `/control-equipamiento?estadoOperativo=EN_LIMPIEZA`.
- **Barriles.** Hasta el tope de filas del bloque: identificador (texto) y fabricante, y la acción. "Ver todos" lleva a `/control-barriles?estadoOperativo=EN_LIMPIEZA`.

**Acción de fila de las limpiezas y los mantenimientos.** El botón "Abrir" es, según `../../../Tecnico/EstandaresUI.md`, la entrada a la pantalla donde se trabaja con el registro (el panel del lote, del equipo o del barril); el formulario de una operación es otra cosa: es la pantalla de alta de una operación concreta. Por eso estas filas no usan "Abrir": llevan un botón de registro propio, `fragments/boton-registrar :: registrar(url, texto, icono, etiquetaAccesible)`, con el mismo aspecto suave ámbar del botón "Abrir" (borde y fondo ámbar suave, `text-xs font-semibold`), el ícono de la operación a la izquierda (`bi-stars` para la limpieza, `bi-wrench` para el mantenimiento, los mismos de las pestañas del panel del equipo) y sin flecha, de modo que no se confunda con una entrada a un panel. Textos: "Registrar limpieza" y "Registrar mantenimiento". Sin tooltip; lleva `aria-label` con la fila ("Registrar limpieza de FER-01"). Va dentro de la celda `col-acciones` de la fila, a la derecha. Destinos: `/control-equipamiento/{id}/limpiezas/nueva`, `/control-barriles/{id}/limpiezas/nueva`, `/control-equipamiento/{id}/mantenimientos/nuevo` y `/control-barriles/{id}/mantenimientos/nuevo`. El botón se muestra solo con el permiso de registro correspondiente; si no, la celda queda vacía (la columna Acciones existe siempre).

**Fila 4: "Pendientes de mantenimiento".** Un bloque a todo el ancho con dos subgrupos apilados, Equipos y debajo Barriles, separados por una línea y con el mismo subencabezado y estilo de tabla que "Limpiezas pendientes", cada uno con su contador y su "Ver todos":

- **Equipos.** Hasta el tope de filas del bloque: identificador con `fragments/enlace-equipo`, tipo, causa y la acción "Registrar mantenimiento". "Ver todos" lleva a `/control-equipamiento?estadoOperativo=EN_MANTENIMIENTO`.
- **Barriles.** Hasta el tope de filas del bloque: identificador (texto), fabricante, causa y la acción "Registrar mantenimiento". "Ver todos" lleva a `/control-barriles?estadoOperativo=EN_MANTENIMIENTO`.

La causa se muestra como etiqueta: "Falla" (rosa) o "Preventivo" (celeste), o "-" cuando no se puede derivar (sección 3).

**Estados vacíos.** Un sub-bloque sin filas no muestra la tabla ni su encabezado de columnas: muestra una sola línea de texto gris centrada, sin tarjeta vacía dentro de la tarjeta. Los textos son:

- "No hay lotes en ejecución"
- "No hay lotes a iniciar"
- "No hay equipos en limpieza"
- "No hay barriles en limpieza"
- "No hay equipos pendientes de mantenimiento"
- "No hay barriles pendientes de mantenimiento"

Cuando todo el panel está en 0, los cuatro contadores muestran 0 y cada sub-bloque muestra su texto.

**Accesibilidad y pantallas chicas.** Los contadores, los enlaces y los botones son enfocables y llevan `aria-label` cuando su texto no identifica la fila. En pantallas chicas la grilla de contadores pasa a una sola columna; las tablas se desplazan horizontalmente dentro de su tarjeta (`overflow-x-auto`) con la columna Acciones fija a la derecha (`col-acciones`).

**Fuera del alcance.** Quedan en el backlog, sin pantalla ni indicador en este panel:

- Insumos próximos a vencer.
- Stock bajo de insumos.
- Mantenimiento preventivo por vencer (equipos y barriles cercanos al máximo de usos que todavía no están En mantenimiento).
- Actualización automática del panel: el panel muestra el estado al cargar la pantalla.
- Paneles de los demás roles.

## 5. Capas que sostienen el panel

Las capas siguen el orden de la metodología: DTOs, mapper, interfaz del service, validaciones y postcondiciones, implementación y casos de prueba.

**DTOs de respuesta** (paquete `service/response_dto/dashboard`, con Lombok `@Getter @Builder @NoArgsConstructor @AllArgsConstructor` como los demás):

- `DashboardOperarioResponseDTO`. Campos:
  - `lotesEnEjecucion` (`BloqueDashboardResponseDTO<LoteDelDiaResponseDTO>`)
  - `lotesAIniciar` (`BloqueDashboardResponseDTO<LoteDelDiaResponseDTO>`)
  - `equiposEnLimpieza` (`BloqueDashboardResponseDTO<EquipoPendienteResponseDTO>`)
  - `barrilesEnLimpieza` (`BloqueDashboardResponseDTO<BarrilPendienteResponseDTO>`)
  - `equiposEnMantenimiento` (`BloqueDashboardResponseDTO<EquipoPendienteResponseDTO>`)
  - `barrilesEnMantenimiento` (`BloqueDashboardResponseDTO<BarrilPendienteResponseDTO>`)
  - Los contadores de la fila 1 salen de los `total` de los bloques: no hay campos de conteo aparte, y "En limpieza" y "Pendientes de mantenimiento" suman los totales de equipos y barriles en la vista.
- `BloqueDashboardResponseDTO<T>`. Campos: `total` (`long`, total real de registros del bloque) y `filas` (`List<T>`, hasta `LimitesTecnicos.PANEL_OPERARIO_FILAS_POR_BLOQUE`).
- `LoteDelDiaResponseDTO`. Campos:
  - `id` (`Long`)
  - `identificadorInterno` (`String`)
  - `nombreReceta` (`String`)
  - `etapaActual` (`TipoEtapa`, nula en un lote Pendiente)
  - `idFermentador` (`Long`)
  - `identificadorInternoFermentador` (`String`)
  - `fechaInicioEstimada` (`LocalDate`)
  - `fechaFinalizacionEstimada` (`LocalDate`)
- `EquipoPendienteResponseDTO`. Campos: `id`, `identificadorInterno`, `tipo` (`TipoEquipamiento`) y `causaMantenimiento` (`CausaMantenimiento`, nula en los bloques de limpieza y cuando no se puede derivar).
- `BarrilPendienteResponseDTO`. Campos: `id`, `identificadorInterno`, `fabricante` (`String`) y `causaMantenimiento` (`CausaMantenimiento`, con la misma regla).
- `CausaMantenimiento` (enum del mismo paquete, con `etiqueta`): `FALLA` ("Falla") y `PREVENTIVO` ("Preventivo").

**Mapper** (`util/mapper/dashboard/MapperDashboardOperario`):

- De un lote del listado a `LoteDelDiaResponseDTO`.
- De `EquipamientoControlResponseDTO` y de `BarrilControlResponseDTO` a `EquipoPendienteResponseDTO` y `BarrilPendienteResponseDTO`, recibiendo la causa.
- De la última operación registrada de un equipo o de un barril a `CausaMantenimiento`: una falla da `FALLA`; una limpieza cuyo `estadoOperativoResultante` es En mantenimiento da `PREVENTIVO`; cualquier otra operación, o ninguna, da nulo.

**Interfaz del service** (`service/interfaces/dashboard/IDashboardOperarioServicio`, solo lectura):

- `DashboardOperarioResponseDTO obtenerDashboardOperario()`. Calcula todo con la fecha de hoy y el tope de `LimitesTecnicos.PANEL_OPERARIO_FILAS_POR_BLOQUE` filas por bloque. No recibe parámetros: el rol lo decide el Controller. Es de solo lectura y corre en una única transacción de lectura (`@Transactional(readOnly = true)`).

**Servicios que compone** (verificados en sus interfaces):

- `IControlEquipamientoServicio.filtrarEquipamientosParaControl(null, null, estadoOperativo, tope)` con `EN_LIMPIEZA` y con `EN_MANTENIMIENTO`, donde `tope` es la primera página de `LimitesTecnicos.PANEL_OPERARIO_FILAS_POR_BLOQUE` filas: devuelve las filas (orden natural por identificador) y el total (`getTotalElements`) en una sola llamada.
- `IControlBarrilServicio.filtrarBarrilesParaControl(null, null, estadoOperativo, tope)` con `EN_LIMPIEZA` y con `EN_MANTENIMIENTO`: lo mismo para los barriles.

**Consultas** (en los repositorios):

1. `ILoteRepository.buscarIdsDeLotesEnEjecucionParaPanel`: página de IDs de los lotes En ejecución ordenados por fecha de finalización estimada ascendente y, ante igual fecha, por ID, con su consulta de conteo. El total sale de la misma página. La fecha es obligatoria en el lote, por lo que el orden no trata nulos. `filtrarIdsDeLotesParaListado` ordena por ID descendente y por eso no sirve para este orden.
2. `ILoteRepository.buscarIdsDeLotesAIniciarParaPanel`: página de IDs de los lotes Pendiente con fecha de inicio estimada menor o igual a la fecha recibida (hoy), ordenados por fecha de inicio estimada ascendente y, ante igual fecha, por ID, con su consulta de conteo.
3. Los lotes de cada página se cargan con `buscarParaListadoPorIds` (planificación, versión de receta y etapas con su equipamiento en una sola consulta) y se ordenan según el orden de los IDs. Si la carga no devuelve alguno de los IDs de la página, ese lote no se muestra y el service deja una advertencia en el log.
4. `IOperacionCicloVidaEquipamientoRepository` y `IOperacionCicloVidaBarrilRepository`: última operación registrada de cada equipo (o barril) de un conjunto de IDs, en una sola consulta por repositorio. La "última" sigue la regla única del módulo (mayor fecha y, ante igual fecha, mayor ID; solo estado Registrado). El service la invoca una vez con los IDs de las filas visibles de mantenimiento y la causa se deriva en el mapper. Así la causa no se resuelve fila por fila.

Los índices de estas consultas los crea la migración `V20__indices_consultas_panel_y_ciclo_de_vida.sql`. Los contadores no usan consultas de conteo aparte: salen de los `total` de las páginas.



**Controller.** `HomeController` recibe `IDashboardOperarioServicio` y, solo si el usuario autenticado tiene la autoridad `ROLE_OPERARIO_DE_PRODUCCION`, agrega al modelo `dashboardOperario` con `obtenerDashboardOperario()`. Para los demás roles no consulta nada. Los ID de los atajos salen del DTO; el Controller no recibe parámetros nuevos.

**Plantillas.**

- `home.html`: no tiene tarjetas de resumen. Incluye el panel de cada uno de los seis roles con `sec:authorize="hasRole('...')"` y su fragmento `panel`; el de este rol se incluye con `sec:authorize="hasRole('OPERARIO_DE_PRODUCCION')"`.
- `home/dashboard-operario.html`: fragmentos `panel(dashboard)`, `contadores(dashboard)`, `lotesDeHoy(dashboard)`, `limpiezasPendientes(dashboard)` y `pendientesDeMantenimiento(dashboard)`, un bloque por fila.
- `fragments/boton-registrar.html`: fragmento `registrar(url, texto, icono, etiquetaAccesible)` descrito en la sección 4.
- Reutiliza `fragments/boton-abrir`, `fragments/enlace-equipo`, `fragments/tooltip` (`data-tooltip`), `lote/formato :: duracion`, `planificacion_produccion/formato :: fecha` y los mismos estilos de tarjeta y de tabla de las listas. `fragments/pestanas` no se usa: el panel no tiene pestañas.

**Estándares.** `../../../Tecnico/EstandaresUI.md` suma un apartado "Botón de registro en la fila" con el contrato de `fragments/boton-registrar` (cuándo se usa frente a "Abrir", texto, ícono, `aria-label`, ubicación en `col-acciones`) y su prueba de plantilla.

**Pruebas y casos de prueba.** `../../../test/casos_de_prueba/services/dashboard` y `../../../test/casos_de_prueba/controllers/dashboard` documentan los casos con los mismos IDs de los tests: `DashboardOperarioServicioImplTest` (orden y tope de 5, totales, causa de mantenimiento, bloques vacíos), `HomeControllerTest` (el modelo del panel solo para el operario) y una prueba de plantilla del panel (permisos de cada atajo, estados vacíos, botón "Abrir" frente a "Registrar").

## 6. Recorridos

| Objetivo | Rol | Recorrido | Clics |
| --- | --- | --- | --- |
| Ver qué lotes están en ejecución | Operario de producción | Pantalla de inicio | 0 |
| Entrar al lote que hay que atender ahora | Operario de producción | Pantalla de inicio, "Abrir" del lote en ejecución | 1 |
| Iniciar un lote que corresponde a hoy | Operario de producción | Pantalla de inicio, "Abrir" del lote en "A iniciar", "Iniciar lote", confirmar | 3 |
| Registrar el consumo que falta en la etapa en curso | Operario de producción | Pantalla de inicio, "Abrir" del lote, "Gestionar consumos", "Nuevo consumo reservado" | 3 más el formulario |
| Registrar la limpieza de un equipo | Operario de producción | Pantalla de inicio, "Registrar limpieza" de la fila, completar, Registrar limpieza | 1 más el formulario |
| Registrar la limpieza de un barril | Operario de producción | Pantalla de inicio, "Registrar limpieza" de la fila, completar, Registrar limpieza | 1 más el formulario |
| Registrar un mantenimiento pendiente | Operario de producción | Pantalla de inicio, "Registrar mantenimiento" de la fila, completar, Registrar mantenimiento | 1 más el formulario |
| Ver todos los equipos que esperan limpieza | Operario de producción | Pantalla de inicio, "Ver todos" de Equipos en Limpiezas pendientes | 1 |
| Ver todos los lotes en ejecución | Operario de producción | Pantalla de inicio, tarjeta "Lotes en ejecución" | 1 |

## 7. Alcance del contador y del enlace de "Lotes a iniciar"

El número cuenta los lotes Pendiente con fecha de inicio estimada de hoy o anterior, y el enlace lleva a `/lotes?estado=PENDIENTE&inicioEstimadoHasta=<hoy>`: la lista de lotes tiene el criterio "Inicio estimado hasta", de modo que el número y la lista coinciden.
