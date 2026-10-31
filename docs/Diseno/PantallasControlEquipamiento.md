# Diseño de las pantallas del módulo de Control de Equipamiento

Este documento define las pantallas del Controller del módulo de Control de Equipamiento: qué pantallas existen, sus rutas, qué rol puede hacer qué, cómo está armada cada una y cuántos clics lleva cada objetivo del usuario. El comportamiento verificable está en `docs/test/casos_de_prueba/controllers/control_equipamiento/`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

## 1. Roles y permisos

| Acción | Permiso | Gerente de producción | Operario de producción |
| --- | --- | --- | --- |
| Ver la lista de control y el panel de un equipo | `LIMPIEZA_EQUIPAMIENTO_CONSULTAR`, `FALLA_EQUIPAMIENTO_CONSULTAR`, `MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR` | Sí, solo lectura | Sí |
| Ver el cronograma de un equipo | rol `GERENTE_DE_PRODUCCION` | Sí | No |
| Ver el detalle de una operación | permiso de consulta de su tipo | Sí | Sí |
| Ver el detalle de un equipamiento | `EQUIPAMIENTO_CONSULTAR` | Sí | Sí |
| Registrar una limpieza, una falla o un mantenimiento | `LIMPIEZA_EQUIPAMIENTO_REGISTRAR`, `FALLA_EQUIPAMIENTO_REGISTRAR`, `MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR` | No | Sí |
| Anular la última operación del equipo | `LIMPIEZA_EQUIPAMIENTO_ANULAR`, `FALLA_EQUIPAMIENTO_ANULAR`, `MANTENIMIENTO_EQUIPAMIENTO_ANULAR` | No | Sí |

- Cada método lleva `@PreAuthorize` con su permiso y cada botón se oculta con `sec:authorize`.
- Un acceso no autorizado no devuelve un 403: el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción".
- La opción "Control de Equipamiento" de la sección Operación del menú la ven el gerente y el operario de producción.

## 2. Inventario de pantallas y rutas

| N.º | Pantalla | Ruta | Método | Vista |
| --- | --- | --- | --- | --- |
| 1 | Lista de control de equipamiento | `/control-equipamiento` | GET | `control_equipamiento/control-lista` |
| 2 | Cronograma del equipo | `/control-equipamiento/{id}/cronograma` (`?desde=&hasta=`) | GET | `control_equipamiento/control-cronograma` |
| 3 | Panel operativo | `/control-equipamiento/{id}` (`?pestana=limpiezas|fallas|mantenimientos`) | GET | `control_equipamiento/control-panel` |
| 4 | Registrar una operación | `/control-equipamiento/{id}/limpiezas`, `/fallas`, `/mantenimientos` | POST | redirige a la pestaña del panel |
| 5 | Anular una operación | `/control-equipamiento/{id}/{pestana}/{idOperacion}/anular` | POST | redirige a la pestaña del panel |
| 6 | Detalle de una operación | `/control-equipamiento/{id}/operaciones/{limpieza|falla|mantenimiento}/{idOperacion}` | GET | `control_equipamiento/operacion-detalle` |
| 7 | Detalle del equipamiento | `/equipamiento/{id}/detalle` | GET | `equipamiento/equipamiento-detalle` |

## 3. Descripción de cada pantalla

**Lista de control.** Todos los equipamientos activos en una tabla, con filtros Identificador (placeholder "Ej: FER-01"), Tipo y Estado operativo, botón Limpiar y paginación de 20, en orden natural por identificador. Columnas: Identificador (enlace al detalle del equipamiento), Tipo, Estado operativo, Usos ("3 de 10"), Última operación (tipo y fecha) y Acciones. El gerente ve el ojo (cronograma) y "Abrir"; el operario solo "Abrir". Los equipos dados de baja no aparecen.

**Bloque de información del equipo.** Fragmento `control_equipamiento/equipo-informacion` usado en el detalle del equipamiento, el cronograma, el panel y el detalle de una operación. Una fila por tema: identificador, tipo y estado operativo; los parámetros propios del tipo que tienen valor, con su unidad; usos desde el último mantenimiento, última limpieza y último mantenimiento (fecha y hora, "-" si no hay).

**Cronograma del equipo.** Bloque de información, período con Anterior, Hoy y Siguiente (por defecto los últimos 30 días hasta hoy; los botones desplazan el período su misma longitud) y la línea de tiempo de una fila, que es el diagrama de Gantt de lotes con el eje en días. Todos los bloques son barras rectangulares con su texto, sin íconos: una por cada etapa de lote en la que se usó el equipo (por ejemplo "LOT-R1-01 · Molienda", con el color de su estado: en curso, finalizada o cancelada; enlace al detalle del lote) y una por cada limpieza, falla y mantenimiento registrado (enlace a su detalle; las anuladas no aparecen), cada tipo con su color sobrio. Cada barra empieza en su posición real del eje y tiene un ancho mínimo para leer su texto completo (170 px los lotes y 120 px las operaciones), aunque dure minutos; las barras que por eso se pisan se reparten en filas apiladas dentro del mismo carril. La leyenda son cuadrados de color con su texto. El tooltip muestra inicio y fin exactos (fecha y hora) y los datos del bloque (lote, etapa y estado; o tipo y observaciones); una etapa en curso muestra "En curso" como fin. Sin actividad: "El equipamiento no tiene actividad en el período".

**Panel operativo.** Bloque de información y tres pestañas con ícono: Limpiezas, Fallas y Mantenimientos. Cada pestaña tiene, para el operario, el formulario de registro cuando el estado del equipo lo permite (limpieza con el equipo En limpieza, falla con el equipo Disponible, mantenimiento con el equipo En mantenimiento): "Fecha y hora de la operación" en una fila y "Observaciones" en otra; si el estado no lo permite, un aviso con el mismo texto que da el servicio. Debajo, la tabla de todas las operaciones de ese tipo (anuladas incluidas), la de fecha más reciente arriba, con Fecha (enlace al detalle), Observaciones y Estado (con el detalle de la anulación). La columna Acciones existe solo para quien puede anular y el botón "Anular" aparece únicamente en la última operación registrada del equipo; abre el diálogo del motivo. El gerente ve el aviso "Solo puede consultar las operaciones del equipamiento", sin formulario ni botones de anular.

**Detalle de una operación.** Bloque de información y los datos de la operación por tema: tipo y estado; fecha de la operación; observaciones; "Registrado por" y fecha de registro; y, si está anulada, cuándo y el motivo.

**Detalle del equipamiento.** Bloque de información y el botón Volver. Se llega desde el ojo de las listas de Gestión de Equipamiento y desde el identificador del equipo, que en las pantallas de lote y de planificación es un enlace ámbar para quien tiene `EQUIPAMIENTO_CONSULTAR`.

## 4. Recorridos

| Objetivo | Rol | Recorrido | Clics |
| --- | --- | --- | --- |
| Registrar la limpieza de un equipo | Operario | Menú Control de Equipamiento, "Abrir" del equipo, completar el formulario, Registrar limpieza | 3 más el formulario |
| Registrar una falla | Operario | Igual, pestaña Fallas (el equipo debe estar Disponible) | 4 más el formulario |
| Anular la última operación | Operario | "Abrir", pestaña de la operación, "Anular", motivo, confirmar | 4 más el motivo |
| Ver la actividad de un equipo | Gerente | Menú Control de Equipamiento, ojo del equipo | 2 |
| Ver un equipo desde un lote | Gerente u operario | Detalle o panel del lote, identificador del equipo | 1 |

## 5. Reglas comunes

- Registrar y anular son POST con Post-Redirect-Get a la misma pestaña, con la alerta de éxito. Un formulario inválido, o un registro que el servicio rechaza, vuelve a la pestaña con los errores y lo escrito; el rechazo de una anulación vuelve al origen con la alerta.
- El identificador del equipo viaja siempre en la ruta, nunca en el formulario.
- Los formularios son `novalidate`: las reglas las valida el servidor.
- El cronograma, el panel, el detalle de una operación y el detalle del equipamiento no llevan accesos cruzados ("Abrir" u ojo) en su encabezado: se llega a cada uno desde la lista de control (ojo y "Abrir" en la columna de acciones) y desde los enlaces del contenido (identificador del equipo, bloques del cronograma, fecha de inicio de una operación). Solo llevan el botón Volver.
- Las pantallas no muestran personas, salvo "Registrado por" (el usuario que registró la operación) en el detalle de la operación; nunca figura como "Responsable", y no aparece en la tabla del panel ni en los tooltips. Nunca se muestra quién anuló: la anulación muestra solo el motivo y la fecha (`fragments/detalle-anulacion :: botonSinUsuario`).
