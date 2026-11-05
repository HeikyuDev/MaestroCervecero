# Diseño de las pantallas del módulo de Control de Equipamiento

Este documento define las pantallas del Controller del módulo de Control de Equipamiento: qué pantallas existen, sus rutas, qué rol puede hacer qué, cómo está armada cada una y cuántos clics lleva cada objetivo del usuario. El comportamiento verificable está en `docs/test/casos_de_prueba/controllers/control_equipamiento/`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

## 1. Roles y permisos

| Acción | Permiso | Gerente de producción | Operario de producción |
| --- | --- | --- | --- |
| Ver la lista de control y el panel de un equipo | `LIMPIEZA_EQUIPAMIENTO_CONSULTAR`, `FALLA_EQUIPAMIENTO_CONSULTAR`, `MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR` | Sí, solo lectura | Sí |
| Ver el historial de un equipo | rol `GERENTE_DE_PRODUCCION` | Sí | No |
| Ver el detalle de una operación | permiso de consulta de su tipo | Sí | Sí |
| Ver el detalle de un equipamiento | `EQUIPAMIENTO_CONSULTAR` | Sí | Sí |
| Ver el formulario y registrar una limpieza, una falla o un mantenimiento | `LIMPIEZA_EQUIPAMIENTO_REGISTRAR`, `FALLA_EQUIPAMIENTO_REGISTRAR`, `MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR` | No | Sí |
| Anular la última operación del equipo | `LIMPIEZA_EQUIPAMIENTO_ANULAR`, `FALLA_EQUIPAMIENTO_ANULAR`, `MANTENIMIENTO_EQUIPAMIENTO_ANULAR` | No | Sí |
| Consultar las listas y detalles de Gestión de Equipamiento, de Insumos, de Barriles y de Recetas | `EQUIPAMIENTO_CONSULTAR`, `INSUMO_CONSULTAR`, `BARRIL_CONSULTAR`, `RECETA_CONSULTAR` (los controllers de gestión autorizan por rol: `hasAnyRole` de ambos) | Sí | Sí, solo consulta |
| Alta, modificación y baja en esos módulos | `*_CREAR`, `*_MODIFICAR`, `*_ELIMINAR` (`hasRole('GERENTE_DE_PRODUCCION')`) | Sí | No |

- Cada método lleva `@PreAuthorize` con su permiso y cada botón se oculta con `sec:authorize`.
- Un acceso no autorizado no devuelve un 403: el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción".
- La opción "Control de Equipamiento" de la sección Operación del menú la ven el gerente y el operario de producción.

## 2. Inventario de pantallas y rutas

| N.º | Pantalla | Ruta | Método | Vista |
| --- | --- | --- | --- | --- |
| 1 | Lista de control de equipamiento | `/control-equipamiento` | GET | `control_equipamiento/control-lista` |
| 2 | Historial del equipo | `/control-equipamiento/{id}/historial` (`?tipo=&fechaDesde=&fechaHasta=&pagina=`) | GET | `control_equipamiento/control-historial` |
| 3 | Panel operativo | `/control-equipamiento/{id}` (`?pestana=limpiezas|fallas|mantenimientos`) | GET | `control_equipamiento/control-panel` |
| 4 | Formulario de registro | `/control-equipamiento/{id}/limpiezas/nueva`, `/fallas/nueva`, `/mantenimientos/nuevo` | GET | `control_equipamiento/control-form` |
| 5 | Registrar una operación | `/control-equipamiento/{id}/limpiezas`, `/fallas`, `/mantenimientos` | POST | redirige a la pestaña del panel (éxito) o al formulario (error) |
| 6 | Anular una operación | `/control-equipamiento/{id}/{pestana}/{idOperacion}/anular` | POST | redirige a la pestaña del panel |
| 7 | Detalle de una operación | `/control-equipamiento/{id}/operaciones/{limpieza|falla|mantenimiento}/{idOperacion}` | GET | `control_equipamiento/operacion-detalle` |
| 8 | Detalle del equipamiento | `/equipamiento/{id}/detalle` | GET | `equipamiento/equipamiento-detalle` |

## 3. Descripción de cada pantalla

**Lista de control.** Todos los equipamientos activos en una tabla, con filtros Identificador (placeholder "Ej: FER-01"), Tipo y Estado operativo, botón Limpiar y paginación de 20, en orden natural por identificador. Columnas: Identificador (enlace al detalle del equipamiento), Tipo, Estado operativo, Usos ("3 de 10") y Acciones. El gerente ve el ojo (historial) y "Abrir"; el operario solo "Abrir". Los botones Limpiar y Buscar van a la derecha de la tarjeta de filtros. Los equipos dados de baja no aparecen.

**Bloque de información del equipo.** Fragmento `control_equipamiento/equipo-informacion` usado en el detalle del equipamiento, el historial, el panel y el detalle de una operación. Una fila por tema: identificador, tipo y estado operativo; los parámetros propios del tipo que tienen valor, con su unidad; usos desde el último mantenimiento, última limpieza y último mantenimiento (fecha y hora, "-" si no hay).

**Historial del equipo.** Título "Historial del equipamiento <identificador>" con el badge del estado operativo, el botón Volver, el bloque de información del equipo y, debajo, los filtros y una tabla paginada. Los filtros siguen el patrón de las otras listas, en una grilla con "Limpiar" y "Buscar": Tipo (Todos, Lote, Limpieza, Falla, Mantenimiento) y Fecha desde y Fecha hasta (con placeholder de ejemplo; la fecha hasta incluye todo su día). La tabla muestra, de la actividad más reciente a la más antigua y de a 20 filas, las etapas de lote en las que se usó el equipo y las limpiezas, fallas y mantenimientos registrados (las operaciones anuladas no aparecen). Columnas: Fecha y hora (la de la operación, o "inicio - fin" de la etapa, con "En curso" si todavía no terminó) | Operación (texto plano: "Limpieza", "Falla", "Mantenimiento" o "Lote · LOT-R1-01 · Molienda") | Estado resultante | Acciones. Ningún texto de la fila es un enlace: en Acciones van el botón i, que abre las observaciones en el diálogo compartido (el texto no se escribe en la fila), y el ojo, que abre el detalle de la operación o el Detalle del lote. El estado resultante es el estado operativo en que la actividad dejó al equipo, con el mismo badge de la lista de control: una limpieza deja el que registró (Disponible o En mantenimiento), una falla En mantenimiento y un mantenimiento Disponible; una etapa de lote en curso deja En uso, al finalizar Molienda, Maceración, Hervido o Envasado deja En limpieza, al cancelarse una etapa iniciada deja En limpieza, y Fermentación y Maduración finalizadas no cambian el estado ("-"), porque el fermentador es compartido por las tres últimas etapas y solo pasa a En limpieza al finalizar el Envasado. Sin actividad: "El equipamiento no tiene operaciones registradas"; con filtros sin resultados: "No se encontraron resultados". Un tipo inválido se avisa y no filtra; un rango de fechas invertido muestra el aviso del servicio y la lista vacía.

**Panel operativo.** Botón Volver, título "Control de <identificador>" con el badge del estado, el bloque de información y tres pestañas con ícono: Limpiezas, Fallas y Mantenimientos. Cada pestaña es la tabla paginada (20 por página) de todas las operaciones de ese tipo, anuladas incluidas y la más reciente arriba, en una tarjeta cuyo encabezado lleva el título ("Limpiezas registradas") y, a la derecha, el botón ámbar "+ Nueva limpieza", "+ Nueva falla" o "+ Nuevo mantenimiento". El botón solo existe para el operario y solo si el estado del equipo lo permite (limpieza con el equipo En limpieza, falla con el equipo Disponible, mantenimiento con el equipo En mantenimiento); si no lo permite, un aviso gris sobre la tabla repite el texto del servicio. Columnas: Fecha y hora | Estado (solo el badge) | Acciones, a la derecha: el botón i de las observaciones, el botón i del detalle de la anulación (en las anuladas), el ojo del detalle de la operación y, solo para el operario y solo en la última operación registrada del equipo y mientras el equipo no esté En uso por un lote, "Anular", que abre el diálogo del motivo. El gerente ve el aviso "Solo puede consultar las operaciones del equipamiento", sin botón de alta ni de anular.

**Formulario de registro.** Página titulada "Registrar limpieza", "Registrar falla" o "Registrar mantenimiento", con Volver a la pestaña, el aviso de campos obligatorios, el bloque de información del equipo y el formulario por tema: "Fecha y hora de la operación" (asterisco rojo) en una fila y "Observaciones" (asterisco rojo, con un ejemplo como placeholder) en otra; Cancelar vuelve a la pestaña y el botón de acción registra. Los errores del servicio se muestran en el resumen y en el campo. Si el estado del equipo no permite la operación y alguien entra por la URL, redirige a la pestaña con el aviso del servicio; el gerente y los demás roles son redirigidos con la alerta "No tiene permisos para realizar esta acción".

**Detalle de una operación.** Bloque de información y los datos de la operación por tema: tipo y estado; fecha de la operación; observaciones; "Registrado por" y fecha de registro; y, si está anulada, cuándo y el motivo.

**Detalle del equipamiento.** Pertenece a la Gestión de Equipamiento. Bloque de información y el botón Volver. Se llega desde el ojo de las listas de Gestión de Equipamiento y desde el identificador del equipo, que en las pantallas de lote y de planificación es un enlace ámbar para quien tiene `EQUIPAMIENTO_CONSULTAR` (el gerente y el operario de producción). Resalta la opción "Gestión de Equipamiento" del menú y "Volver a molinos / maceradores / ollas de hervor / fermentadores" lleva a la lista del tipo del equipo, para ambos roles. El operario solo consulta esas listas: no ve los botones de alta, modificar ni baja.

## 4. Recorridos

| Objetivo | Rol | Recorrido | Clics |
| --- | --- | --- | --- |
| Registrar la limpieza de un equipo | Operario | Menú Control de Equipamiento, "Abrir" del equipo, "+ Nueva limpieza", completar el formulario, Registrar limpieza | 4 más el formulario |
| Registrar una falla | Operario | Igual, pestaña Fallas, "+ Nueva falla" (el equipo debe estar Disponible) | 5 más el formulario |
| Anular la última operación | Operario | "Abrir", pestaña de la operación, "Anular", motivo, confirmar | 4 más el motivo |
| Ver la actividad de un equipo | Gerente | Menú Control de Equipamiento, ojo del equipo | 2 |
| Ver una operación del historial | Gerente | Ojo de la fila del historial | 1 más el recorrido anterior |
| Ver un equipo desde un lote | Gerente u operario | Detalle o panel del lote, identificador del equipo | 1 |

## 5. Reglas comunes

- Registrar y anular son POST con Post-Redirect-Get a la misma pestaña, con la alerta de éxito. Un formulario inválido, o un registro que el servicio rechaza, vuelve al formulario de registro con los errores y lo escrito; el rechazo de una anulación vuelve al origen con la alerta.
- El identificador del equipo viaja siempre en la ruta, nunca en el formulario.
- Los formularios son `novalidate`: las reglas las valida el servidor.
- El historial, el panel, el detalle de una operación y el detalle del equipamiento no llevan accesos cruzados ("Abrir" u ojo) en su encabezado: se llega a cada uno desde la lista de control (ojo y "Abrir" en la columna de acciones) y desde los botones de la columna Acciones de las tablas. Solo llevan el botón Volver. Ningún dato de una fila de tabla es un enlace de texto.
- Las pantallas no muestran personas, salvo "Registrado por" (el usuario que registró la operación) en el detalle de la operación; nunca figura como "Responsable", y no aparece en la tabla del panel ni en los tooltips. Nunca se muestra quién anuló: la anulación muestra solo el motivo y la fecha (`fragments/detalle-anulacion :: botonSinUsuario`).
