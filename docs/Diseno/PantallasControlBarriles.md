# Diseño de las pantallas del módulo de Control de Barriles

Este documento define las pantallas del Controller del módulo de Control de Barriles: qué pantallas existen, sus rutas, qué rol puede hacer qué, cómo está armada cada una y cuántos clics lleva cada objetivo del usuario. El comportamiento verificable está en `docs/test/casos_de_prueba/controllers/control_barril/`. El módulo sigue el diseño de Control de Equipamientos (`PantallasControlEquipamiento.md`) con seis tipos de operación en lugar de tres.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

## 1. Roles y permisos

| Acción | Permiso | Gerente de producción | Operario de producción | Encargado de distribución |
| --- | --- | --- | --- | --- |
| Ver la lista de control | alguno de los seis `*_BARRIL_CONSULTAR` de operaciones | Sí, solo lectura | Sí | Sí |
| Ver el panel de un barril | ídem | Sí, las seis pestañas | Limpiezas, Fallas, Fraccionamientos y Mantenimientos | Despachos y Devoluciones |
| Ver el historial de un barril | rol `GERENTE_DE_PRODUCCION` | Sí | No | No |
| Ver el detalle de una operación | permiso de consulta de su tipo | Sí | Las cuatro suyas | Despachos y devoluciones |
| Ver el formulario y registrar | `LIMPIEZA_BARRIL_REGISTRAR`, `FALLA_BARRIL_REGISTRAR`, `FRACCIONAMIENTO_BARRIL_REGISTRAR`, `MANTENIMIENTO_BARRIL_REGISTRAR`, `DESPACHO_BARRIL_REGISTRAR`, `DEVOLUCION_BARRIL_REGISTRAR` | No | Las cuatro suyas | Despacho y devolución |
| Anular la última operación del barril | los `*_BARRIL_ANULAR` de cada tipo | No | Las cuatro suyas | Despacho y devolución |
| Buscar fabricantes (filtro de la lista) | `GET /barriles/buscador/fabricantes`, por rol | Sí | Sí | Sí |
| Buscar clientes (selector del despacho) | `GET /clientes/buscador`, rol `ENCARGADO_DE_DISTRIBUCION` | No | No | Sí |

- El gerente comercial ya no existe (es el encargado de distribución); el gerente de compras, el encargado de depósito y el administrador no acceden a ninguna pantalla del módulo.
- Cada método lleva `@PreAuthorize` con su permiso y cada botón se oculta según el permiso.
- Un acceso no autorizado no devuelve un 403: el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción".
- La opción "Control de Barriles" (ícono `bi-clipboard-check`) está en la sección Operación del menú, debajo de "Control de Equipamientos". La ven quienes tienen alguno de los permisos de consulta de operaciones de barril (operario, gerente de producción y encargado de distribución, que por eso ve la sección Operación). "Gestión de Barriles" (Administración) no cambia.

## 2. Inventario de pantallas y rutas

| N.º | Pantalla | Ruta | Método | Vista |
| --- | --- | --- | --- | --- |
| 1 | Lista de control de barriles | `/control-barriles` (`?identificador=&idFabricante=&estadoOperativo=&pagina=`) | GET | `control_barril/control-lista` |
| 2 | Historial del barril | `/control-barriles/{id}/historial` (`?tipo=&fechaDesde=&fechaHasta=&pagina=`) | GET | `control_barril/control-historial` |
| 3 | Panel del barril | `/control-barriles/{id}` (`?pestana=limpiezas|fallas|fraccionamientos|mantenimientos|despachos|devoluciones`) | GET | `control_barril/control-panel` |
| 4 | Formulario de registro | `/control-barriles/{id}/limpiezas/nueva`, `/fallas/nueva`, `/fraccionamientos/nuevo`, `/mantenimientos/nuevo`, `/despachos/nuevo`, `/devoluciones/nueva` | GET | `control_barril/control-form` |
| 5 | Registrar una operación | `/control-barriles/{id}/limpiezas`, `/fallas`, `/fraccionamientos`, `/mantenimientos`, `/despachos`, `/devoluciones` | POST | redirige a la pestaña (éxito) o al formulario (error) |
| 6 | Anular una operación | `/control-barriles/{id}/{pestana}/{idOperacion}/anular` | POST | redirige a la pestaña del panel |
| 7 | Detalle de una operación | `/control-barriles/{id}/operaciones/{limpieza|falla|fraccionamiento|mantenimiento|despacho|devolucion}/{idOperacion}` | GET | `control_barril/operacion-detalle` |
| 8 | Buscador de clientes (modal) | `/clientes/buscador` | GET | `cliente/buscador-cliente :: buscador` |

## 3. Descripción de cada pantalla

**Lista de control.** Todos los barriles activos en una tabla de 20 por página, en orden natural por identificador. Filtros en una fila: Identificador (placeholder "Ej: BRL-01"), Fabricante (selector de entidad con buscador modal, igual al de Gestión de Barriles) y Estado operativo (Todos, Disponible, En uso, Con cerveza, Despachado, En limpieza, En mantenimiento). "Limpiar" y "Buscar" van a la derecha de la tarjeta. Columnas: Identificador, Fabricante, Capacidad (L), Contenido (L), Estado operativo (badge), Usos ("3 de 10": limpiezas registradas desde el último mantenimiento sobre el máximo) y Acciones. El gerente ve el ojo (historial) y "Abrir"; el operario y el encargado solo "Abrir". Un estado inválido o un fabricante que ya no existe no filtran (el estado inválido se avisa).

**Bloque de información del barril.** Fragmento `control_barril/barril-informacion` usado en el panel, los formularios, el historial y el detalle de una operación. Una fila por tema: identificador, número de serie y fabricante; capacidad (L), contenido (L) y estado operativo; usos desde el último mantenimiento, última limpieza y último mantenimiento (fecha y hora, "-" si no hay).

**Panel del barril.** Botón Volver, título "Control de <identificador>" con el badge del estado, el bloque de información y, en pestañas con ícono, una tabla por tipo de operación; cada rol ve solo las pestañas cuyo permiso de consulta tiene. La tarjeta de cada pestaña lleva el título ("Limpiezas registradas") y, a la derecha, el botón ámbar "+ Nueva limpieza", "+ Nueva falla", "+ Nuevo fraccionamiento", "+ Nuevo mantenimiento", "+ Nuevo despacho" o "+ Nueva devolución". El botón solo existe con el permiso de registrar y solo si el estado del barril lo permite: limpieza con el barril En limpieza, falla con Disponible, mantenimiento con En mantenimiento, fraccionamiento y despacho con Con cerveza, devolución con Despachado. Si no lo permite, un aviso gris sobre la tabla repite el texto del servicio ("Solo se puede registrar una falla sobre un barril en estado operativo Disponible") y, con el barril En uso, avisa que lo ocupa un lote. Columnas: Fecha y hora | Estado | Acciones (i de observaciones, i del detalle de la anulación en las anuladas, ojo y, con el permiso de anular y solo en la operación `anulable`, "Anular" con el diálogo del motivo). El gerente ve "Solo puede consultar las operaciones del barril".

**Regla de `anulable`.** Una operación es anulable si está Registrada, es la última operación registrada del barril (mayor fecha y, a igualdad, mayor ID, de cualquier tipo), el barril no está En uso y es posterior al último envasado del barril. Es la misma regla de los servicios de operaciones (`MetodosCicloVida`); el servicio sigue siendo la fuente de verdad al anular.

**Formulario de registro.** Página titulada "Registrar <operación>", con Volver, el aviso de campos obligatorios, el bloque de información y los campos por tema, uno por fila: fraccionamiento, "Cantidad extraída" con el sufijo L (hasta 4 decimales); despacho, "Cliente" (selector con buscador modal, sin alta al vuelo); luego "Fecha y hora de la operación" (y, en el despacho, "Fecha estimada de devolución" en la misma fila) y "Observaciones". La devolución muestra la ayuda "Al registrar la devolución se descarta el contenido del barril". Los errores del servicio se muestran en el resumen y en su campo; Registrar vuelve a la pestaña con la alerta de éxito y un rechazo vuelve al formulario con lo escrito. Si el estado del barril no permite la operación y alguien entra por la URL, redirige a la pestaña con el aviso del servicio.

**Historial del barril (solo gerente de producción).** Bloque de información, filtros (Tipo: Todos, Envasado, Limpieza, Falla, Fraccionamiento, Mantenimiento, Despacho, Devolución; Fecha desde; Fecha hasta, que incluye todo su día) y una tabla paginada, de la actividad más reciente a la más antigua y sin anuladas. Columnas: Fecha y hora | Operación (texto plano: "Lote · LOT-R1-01 · Envasado" o el nombre del tipo) | Estado resultante | Acciones (i de observaciones y ojo al detalle de la operación; en los envasados, el ojo al Detalle del lote). Estado resultante: limpieza y fraccionamiento, el que registraron; falla, En mantenimiento; mantenimiento, Disponible; despacho, Despachado; devolución, En limpieza; envasado, En uso si el lote está en ejecución y Con cerveza si está finalizado. La fecha del envasado es su fecha de envasado. Se leen dos consultas (envasados con su lote y operaciones) unidas en memoria, sin una consulta por fila.

**Detalle de una operación.** Bloque de información y datos por tema: tipo y estado; fecha y hora; datos propios (cantidad extraída; cliente y fecha estimada de devolución; litros descartados); observaciones completas; "Registrado por" y fecha de registro; y, si está anulada, la fecha y el motivo (nunca quién anuló). Una operación que no es del barril de la ruta responde como inexistente, también al anular.

## 4. Recorridos

| Objetivo | Rol | Recorrido | Clics |
| --- | --- | --- | --- |
| Registrar un fraccionamiento | Operario | Menú Control de Barriles, "Abrir" del barril, pestaña Fraccionamientos, "+ Nuevo fraccionamiento", completar, Registrar | 4 más el formulario |
| Registrar una limpieza, falla o mantenimiento | Operario | Igual, en la pestaña del tipo | 4 a 5 más el formulario |
| Despachar un barril | Encargado | Menú, "Abrir", "+ Nuevo despacho", elegir cliente, completar, Registrar | 5 más el formulario |
| Registrar la devolución | Encargado | Menú, "Abrir", pestaña Devoluciones, "+ Nueva devolución" | 5 más el formulario |
| Anular la última operación | Operario o encargado | "Abrir", pestaña, "Anular", motivo, confirmar | 4 más el motivo |
| Ver la actividad de un barril | Gerente | Menú, ojo del barril | 2 |

## 5. Reglas comunes

- Registrar y anular son POST con Post-Redirect-Get a la misma pestaña. El id del barril viaja siempre en la ruta (`idBarril`), nunca en un campo del formulario.
- Los formularios son `novalidate`: las reglas las valida el servidor.
- Ningún dato de una fila es un enlace de texto; todos los botones de fila van en la columna Acciones, a la derecha, en el orden i, ojo y anular.
- Las observaciones nunca se escriben en la fila: el botón i las abre en el diálogo pequeño compartido.
- Las pantallas no muestran personas, salvo "Registrado por" en el detalle de la operación; nunca figura "Responsable" ni quién anuló.
