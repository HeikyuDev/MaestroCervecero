# Diseño de las pantallas del módulo de Lotes

Este documento define las pantallas del Controller del módulo de Lotes (Gestión de Lotes): qué pantallas existen, sus rutas, qué rol puede hacer qué, cómo está armada cada una y cuántos clics lleva cada objetivo del usuario. El comportamiento verificable de cada endpoint está en `docs/test/casos_de_prueba/controllers/lote/`; las reglas de negocio, en `docs/test/casos_de_prueba/services/lote/` y en `docs/Dominio/Escalado/`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML y fragmentos HTML para los modales. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

## 1. Roles y permisos

| Acción | Permiso | Gerente de producción | Operario de producción |
| --- | --- | --- | --- |
| Ver la lista y el detalle de lotes | `LOTE_CONSULTAR` | Sí | Sí |
| Buscar planificaciones y fermentadores en los filtros de la lista | `LOTE_CONSULTAR` | Sí | Sí |
| Registrar un lote | `LOTE_REGISTRAR` | Sí | No |
| Elegir planificación y equipos en el formulario de registro | `LOTE_REGISTRAR` | Sí | No |
| Cancelar un lote | `LOTE_ANULAR` | Sí | No |
| Iniciar un lote | `LOTE_INICIAR` | No | Sí |
| Finalizar una etapa (seis permisos, uno por etapa) | `LOTE_FINALIZAR_<ETAPA>` | No | Sí |
| Ver la pantalla de consumos de una etapa y su botón "Gestionar consumos" en el detalle | `CONSUMO_INSUMO_CONSULTAR` | No | Sí (el encargado de depósito también la ve, en solo lectura) |
| Registrar un consumo (reservado o directo) | `CONSUMO_INSUMO_REGISTRAR` | No | Sí |
| Anular un consumo | `CONSUMO_INSUMO_ANULAR` | No | Sí |
| Ver la pantalla de envasados | `ENVASADO_CONSULTAR` | No | Sí |
| Registrar un envasado | `ENVASADO_REGISTRAR` | No | Sí |
| Anular un envasado | `ENVASADO_ANULAR` | No | Sí |
| Ver el cronograma de producción y buscar fermentadores en sus filtros | `LOTE_CONSULTAR` | Sí, con acciones | Sí, solo lectura |
| Subir o bajar un lote en la cola | `LOTE_REGISTRAR` | Sí | No |
| Destacar una planificación en el cronograma (filtro y su buscador) | `PLANIFICACION_PRODUCCION_CONSULTAR` | Sí | No |
| Registrar un lote desde una planificación | `LOTE_REGISTRAR` y `PLANIFICACION_PRODUCCION_CONSULTAR` | Sí | No |

- Cada método del Controller lleva `@PreAuthorize("hasAuthority('<PERMISO>')")` con el permiso de la tabla (`LOTE_CONSULTAR`, `LOTE_REGISTRAR`, `LOTE_ANULAR`, `LOTE_INICIAR`, `LOTE_FINALIZAR_<ETAPA>`, `CONSUMO_INSUMO_*` y `ENVASADO_*`), y cada botón se oculta con `sec:authorize` por el mismo permiso. El servidor es quien rechaza: ocultar el botón es solo una ayuda visual.
- El gerente de producción registra y cancela lotes y consulta la lista y el detalle, donde ve las fechas de inicio y fin de cada etapa, sus insumos y los envasados, sin botones de operación. Los consumos registrados se consultan en la pantalla de consumos, que exige `CONSUMO_INSUMO_CONSULTAR`: el gerente no tiene ese permiso y no ve el botón "Gestionar consumos" ni los consumos en ninguna pantalla. El operario de producción inicia lotes, finaliza las seis etapas, registra y anula consumos y envasados, consulta la lista y el detalle, y ve el cronograma en solo lectura. Las pantallas de consumos y de envasados son de operación: las abre el operario y el gerente no.
- El encargado de depósito tiene `CONSUMO_INSUMO_CONSULTAR` y puede abrir la pantalla de consumos de una etapa en solo lectura (resumen y consumos registrados de cada pestaña con su filtro por estado, sin los botones "Nuevo consumo" ni "Anular") escribiendo la dirección. No llega por el menú ni tiene el link "Volver al lote" (que exige `LOTE_CONSULTAR`). Es una decisión aprobada.
- Un acceso no autorizado no devuelve un 403: el `ControllerAdvices` traduce la excepción a un redirect con la alerta `danger` "No tiene permisos para realizar esta acción".

## 2. Inventario de pantallas y rutas

| N.º | Pantalla | Ruta | Método | Permiso | Vista |
| --- | --- | --- | --- | --- | --- |
| 1 | Lista de lotes | `/lotes` | GET | `LOTE_CONSULTAR` | `lote/lote-lista` |
| 2 | Nuevo lote | `/lotes/nuevo` (`?idPlanificacion=`) | GET | `LOTE_REGISTRAR` | `lote/lote-form` |
| 2 | Crear lote (envío) | `/lotes` | POST | `LOTE_REGISTRAR` | redirige a `/lotes` (la lista) |
| 3 | Detalle del lote | `/lotes/{id}` (`?etapa=`) | GET | `LOTE_CONSULTAR` | `lote/lote-detalle` |
| 3 | Iniciar lote | `/lotes/{id}/iniciar` | POST | `LOTE_INICIAR` | redirige a `/lotes/{id}` |
| 3 | Cancelar lote | `/lotes/{id}/cancelar` | POST | `LOTE_ANULAR` | redirige a `/lotes/{id}` |
| 3 | Finalizar etapa (seis) | `/lotes/{id}/etapas/{etapa}/finalizar` | POST | `LOTE_FINALIZAR_<ETAPA>` | redirige a `/lotes/{id}` |
| 4 | Consumos de una etapa | `/lotes/{id}/etapas/{etapa}/consumos` (`?tipo=`, `?estado=`, `?pagina=`) | GET | `CONSUMO_INSUMO_CONSULTAR` | `lote/consumo-lista` |
| 4 | Nuevo consumo reservado | `/lotes/{id}/etapas/{etapa}/consumos/reservado/nuevo` | GET | `CONSUMO_INSUMO_REGISTRAR` | `lote/consumo-form` |
| 4 | Nuevo consumo directo | `/lotes/{id}/etapas/{etapa}/consumos/directo/nuevo` | GET | `CONSUMO_INSUMO_REGISTRAR` | `lote/consumo-form` |
| 4 | Crear consumo reservado (envío) | `/lotes/{id}/etapas/{etapa}/consumos/reservado` | POST | `CONSUMO_INSUMO_REGISTRAR` | redirige a la pestaña "Reservados" |
| 4 | Crear consumo directo (envío) | `/lotes/{id}/etapas/{etapa}/consumos/directo` | POST | `CONSUMO_INSUMO_REGISTRAR` | redirige a la pestaña "Directos" |
| 4 | Anular consumo | `/lotes/{id}/etapas/{etapa}/consumos/{idConsumo}/anular` | POST | `CONSUMO_INSUMO_ANULAR` | redirige a la pestaña del tipo del consumo |
| 5 | Envasados del lote | `/lotes/{id}/envasados` | GET | `ENVASADO_CONSULTAR` | `lote/envasado-lista` |
| 5 | Crear envasado (envío) | `/lotes/{id}/envasados` | POST | `ENVASADO_REGISTRAR` | redirige a la misma pantalla |
| 5 | Anular envasado | `/lotes/{id}/envasados/{idEnvasado}/anular` | POST | `ENVASADO_ANULAR` | redirige a la misma pantalla |
| M1 | Buscadores del formulario de registro (fragmentos) | `/lotes/buscador/{planificaciones, molinos, maceradores, ollas, fermentadores}` | GET | `LOTE_REGISTRAR` | `lote/buscador-*` |
| M2 | Buscadores de los filtros de la lista (fragmentos) | `/lotes/filtros/{planificaciones, fermentadores}` | GET | `LOTE_CONSULTAR` | `lote/filtro-*` |
| M3 | Buscadores de las pantallas de operación (fragmentos) | `/lotes/{id}/etapas/{etapa}/consumos/{reservado, directo}/buscador/insumos`, `/lotes/{id}/etapas/{etapa}/consumos/{reservado, directo}/buscador/lotes-insumo`, `/lotes/{id}/envasados/buscador/barriles` | GET | `CONSUMO_INSUMO_REGISTRAR` y `ENVASADO_REGISTRAR` | `lote/buscador-*` |
| C | Cronograma de producción (y su buscador de fermentadores) | `/cronograma-produccion`, `/cronograma-produccion/buscador/fermentadores` | GET | `LOTE_CONSULTAR` | `cronograma/cronograma` |
| C | Buscador de planificaciones del filtro "Planificación a destacar" (fragmento) | `/cronograma-produccion/buscador/planificaciones` | GET | `PLANIFICACION_PRODUCCION_CONSULTAR` | `cronograma/buscador-planificacion` |
| C | Subir o bajar un lote en la cola | `/cronograma-produccion/lotes/{id}/{subir, bajar}` | POST | `LOTE_REGISTRAR` | redirige al cronograma |

- `{etapa}` toma los valores `molienda`, `maceracion`, `hervido`, `fermentacion`, `maduracion` y `envasado`. La pantalla de consumos existe para las etapas que consumen insumos (Molienda, Hervido, Fermentación y Maduración); Maceración y Envasado no consumen insumos. Una etapa sin insumos según la receta (por ejemplo, Maduración sin lúpulo de maduración) tampoco tiene pantalla de consumos: ni la pantalla, ni los formularios, ni el registro, que se rechazan con "Esta etapa no consume insumos" y vuelven al detalle del lote.
- La sidebar (sección Operación) muestra "Gestión de Lotes" (`/lotes`, ícono `bi-collection`, de línea como los demás) para el gerente y el operario de producción, y "Cronograma de Producción" para ambos.

## 3. Reglas comunes de las pantallas

- **Post-Redirect-Get.** Toda acción exitosa redirige y muestra una alerta de éxito en el layout; recargar la página no repite la acción. Registrar un lote lleva a la lista de lotes (el lote queda Pendiente y lo inicia el operario); iniciar, cancelar y finalizar vuelven al detalle (que abre la etapa actual en ejecución); registrar o anular un consumo vuelve a la pestaña del tipo de consumo de la pantalla de consumos, y registrar o anular un envasado, a la pantalla de envasados, con la alerta de éxito.
- **Errores.** El mensaje de un rechazo del service se muestra como alerta (amarilla si es una regla de negocio, roja si el recurso no existe): el `ControllerAdvices` redirige al origen (`Referer`) con ese mensaje. Los errores de validación del formulario (`@Valid`) y los números que no se pueden convertir en las pantallas de consumos y de envasados también son un Post-Redirect-Get: redirigen a la pantalla del formulario (en los consumos, la de "Nuevo consumo") y viajan en los atributos flash, de modo que ella muestra un resumen de errores y lo que el usuario había cargado, y el navegador nunca queda en la URL de un `POST` (que no tiene pantalla propia y daría un 405 si un rechazo posterior volviera a ella). En los parámetros de la URL, un valor inválido se tolera: aviso amarillo y la pantalla se muestra sin ese filtro; en un buscador en modal, el aviso va dentro del propio fragmento. Un valor que no se puede convertir al tipo del parámetro (`/lotes/abc`, `?pagina=xyz`) redirige al origen con la alerta amarilla "El valor indicado no es válido" y, en un buscador en modal, responde 400 con ese texto, que el modal muestra dentro de sí. Un buscador en modal nunca redirige: cualquier rechazo (recurso inexistente, regla de negocio o error inesperado) responde con su estado HTTP (404, 422 o 500) y el motivo como texto, que el modal muestra dentro de sí (el rechazo de permisos redirige al origen con la alerta); una sesión que ya no está vigente (venció o se inició en otro lugar) se informa como "Su sesión finalizó: inicie sesión nuevamente".
- **Doble envío.** Los formularios de registrar un consumo o un envasado, el diálogo de motivo (anular y cancelar) y los de iniciar y finalizar una etapa llevan `data-un-solo-envio`: con JavaScript, al enviarlos se deshabilitan sus botones y un segundo envío se ignora; al volver a la página desde el historial del navegador se habilitan de nuevo. Sin JavaScript el formulario se envía igual.
- **Advertencias.** Las advertencias de un consumo (lote de insumo vencido, total consumido mayor a lo requerido) no bloquean la operación: se muestran como alertas amarillas en la parte superior de la pantalla, debajo de la alerta de éxito.
- **Formularios.** Son `novalidate`, el JavaScript no valida y el servidor es la fuente de verdad. Los campos obligatorios llevan asterisco rojo, todo campo lleva un placeholder de ejemplo ("Ej: 300") y las unidades van en la etiqueta o en la cabecera de la columna.
- **Selector de entidad.** Elegir una entidad relacionada (planificación, equipo, lote de insumo, barril) abre un modal con criterios de filtro y resultados paginados de a 8; nunca es un desplegable con todos los registros. Es solo para elegir: no hay alta al vuelo en estas pantallas. Un criterio que identifica una instancia es un campo de texto, nunca un desplegable, y elegir una instancia (insumo, lote de insumo, barril, equipo) siempre es con el buscador en modal; los desplegables se reservan para enums chicos (estado del lote, estado operativo).
- **Confirmaciones.** Iniciar un lote y finalizar una etapa se envían con un formulario POST y token CSRF; con JavaScript piden antes una confirmación en un diálogo ("¿Finalizar la etapa Molienda? El molino pasará a En limpieza", "¿Iniciar el lote PALE_ALE-R7-3? Se reservarán los insumos y los equipos pasarán a En uso"), y sin JavaScript se envían directamente. "Finalizar etapa" siempre está habilitado: el avance hacia el mínimo es informativo y el rechazo lo hace el service con su mensaje. Cancelar un lote y anular un consumo o un envasado piden un motivo obligatorio en un diálogo.
- **Pestañas.** Las seis pestañas del detalle (una por etapa) son enlaces (`?etapa=<etapa>`), de modo que funcionan sin JavaScript y se pueden guardar en favoritos. Se ve una etapa por vez, y se pueden ver todas, incluidas las anteriores a la actual.
- **Páginas de error.** Una dirección que no existe y un error inesperado muestran una página con el diseño del sistema (`error/404` y `error/500`, y `error` para los demás casos), con el link "Ir al inicio" (el único destino que todos los roles pueden abrir), sin detalles técnicos.
- **Orden de los listados.** Los registros transaccionales (lotes, consumos, envasados) muestran el último registrado arriba; los maestros (equipos, barriles) van en orden alfabético ascendente por identificador.
- **Estados.** Siempre con su nombre legible: lote (Pendiente, En ejecución, Finalizado, Cancelado) y etapa (Pendiente, En curso, Finalizada, Cancelada).

### 3.1 Convenciones de textos y formato de la interfaz (todo el sistema)

Valen para todas las pantallas, no solo para las de lotes.

- **Alta.** El botón de la lista y el título de la página dicen "Nuevo X" o "Nueva X" (también lotes, planificaciones, consumos y envasados); el botón de envío dice "Crear X"; la alerta de éxito dice "X creado correctamente" o "X creada correctamente". La edición usa el título "Editar X", el botón "Guardar cambios" y la alerta "X modificado correctamente". La baja usa "Dar de baja X" y la alerta "X dado de baja correctamente".
- **Motivos.** Los diálogos que piden un motivo confirman con "Confirmar anulación", "Confirmar cancelación" o "Confirmar finalización"; el diálogo de baja confirma con "Dar de baja".
- **Puntuación.** Los textos descriptivos (subtítulos de página y de modal, ayudas de campo, descripciones de diálogos) terminan con punto. No llevan punto final las alertas, los mensajes de error y de validación, las etiquetas, los títulos, las cabeceras, los botones y los textos de estado vacío.
- **Tono.** Español neutro, con el imperativo de usted ("Seleccione", "Ingrese", "Vuelva"); sin voseo, sin emojis.
- **Estados vacíos.** Una lista con criterios de búsqueda dice "No se encontraron X con los criterios indicados"; una sección sin criterios dice "No hay X registrados".
- **Selectores.** El selector de una entidad usa el texto "Seleccione <entidad>" sin artículo; el de un valor de lista cerrada usa "Seleccione <valor>" ("Seleccione tipo"). La opción de los filtros que no restringe dice "Todos".
- **Volver.** "Volver a <plural>" sin artículo cuando el destino es una lista ("Volver a lotes"); "Volver al lote" cuando es un registro.
- **Planificación.** Siempre se nombra "Plan #N - Nombre" (por ejemplo "Plan #7 - Pale Ale"); lo arma un único componente (`EtiquetaPlanificacion`) que usan los controllers y las plantillas.
- **Números.** Coma decimal, miles con punto y sin ceros sobrantes ("100", "4,5"). Las unidades van entre paréntesis en la cabecera de la columna y en la etiqueta del campo ("Capacidad total (L)", "Rendimiento (kg/h)", "Eficiencia de maceración (%)") y la celda lleva solo el número; cuando la unidad cambia de una fila a otra (consumos de insumos) la unidad va en la celda. El porcentaje se escribe pegado al número ("90%"). La unidad de tiempo es "min" y la de volumen "L".
- **Fechas.** `dd/MM/yyyy` y `dd/MM/yyyy HH:mm`, y "-" cuando no hay valor. Los campos de fecha llevan un placeholder de ejemplo ("Ej: 01/07/2027").
- **Paginación.** Pie común de las listas: "N registro(s) · Página X de Y" con "Anterior" y "Siguiente" (íconos `bi-chevron-left` y `bi-chevron-right`); la etiqueta se escribe en singular cuando hay un solo registro ("1 consumo"). El cronograma pagina de a 5 fermentadores; las demás tablas, de a 10 (los lotes, de a 20).
- **Textos largos.** Un texto que escribe el usuario (nombre de receta, identificador, código) se acorta con puntos suspensivos dentro de un ancho máximo y se completa en un tooltip flotante; nunca ensancha la tabla.
- **Títulos.** Los títulos de pantalla y las opciones del menú van en mayúscula inicial en cada palabra significativa ("Panel de Control", "Gestión de Lotes"); las etiquetas y los datos, en oración ("Estado del sistema").
- **Obligatorios.** Todo formulario, incluidos los diálogos, lleva la nota "Los campos marcados con * son obligatorios." y el asterisco rojo.
- **Botones.** Buscar usa `bi-search` y va acompañado del enlace "Limpiar"; la acción de ver, editar y dar de baja en las listas usa solo ícono (`bi-eye`, `bi-pencil-square`, `bi-trash`, tamaño `text-lg` con `p-2`) con `title` y `aria-label`.
- **Mensajes.** Los motivos que acompañan a una indicación se separan con dos puntos ("... porque tiene envasados registrados: anule los envasados para poder cancelarlo"); un número fuera de rango se informa como "X debe ser mayor a 0"; el nombre obligatorio, como "El nombre es obligatorio".

## 4. Pantalla 1: lista de lotes (`/lotes`)

```
Gestión de Lotes                                                  [ + Nuevo lote ]*
Lotes registrados, con el último registrado arriba

+- Criterios ---------------------------------------------------------------------------+
| Planificación        Fermentador          Código             Estado                   |
| [Seleccione plani..] [Seleccione ferm..]  [Ej: PALE_ALE-R7-3] [ Todos            v]   |
|                                                                [Limpiar] [ Buscar ]   |
+----------------------------------------------------------------------------------------+

+- Resultados --------------------------------------------------------------------------------------------------+
| Código         Planificación         Fermentador  Estado y etapa  Inicio          Fin             Acciones   |
| PALE_ALE-R7-3  Plan #7 - Pale Ale    F-01         En ejecución    Est. 01/07/2027  Est. 05/07/2027  [engranaje] |
|                                                    Hervido                                                       |
|                                                                    Real 01/07/2027  Real -                    |
| STOUT-R8-1     Plan #8 - Stout       F-02         Pendiente       Est. 12/07/2027  Est. 20/07/2027  [engranaje] |
|                                                                    Real -           Real -                    |
| 45 lotes · Página 1 de 3                                                          [< Anterior] [Siguiente >]  |
+---------------------------------------------------------------------------------------------------------------+
* "Nuevo lote" solo lo ve el gerente de producción.
```

- Abrir un lote se hace con el botón de ícono `bi-gear` con tooltip "Gestionar lote" (visible, con borde y fondo ámbar suave), que cierra cada fila en la última columna, "Acciones". La cabecera y la celda de esa columna se centran y el botón queda dentro de la columna. La fila no es clickeable y el código es texto.

- Criterios opcionales y combinables; "Planificación" y "Fermentador" son selectores de entidad (campo en blanco con ícono de lista que abre un buscador en modal, nunca un desplegable) con botón para quitar la selección. Los buscadores requieren `LOTE_CONSULTAR`: los usan el gerente y el operario. El buscador de planificaciones filtra por "Receta" y ofrece planificaciones de cualquier estado; el de fermentadores, por "Identificador", "Estado operativo" (desplegable del enum) e "Incluir dados de baja" (casilla desmarcada por defecto, que suma los fermentadores dados de baja que tienen lotes para poder filtrar su historial); el elegido se muestra en el campo, con "(dado de baja)" si corresponde. Solo "Estado" es un desplegable, porque es un enum chico. Un parámetro inválido o inexistente en la URL se ignora con un aviso amarillo y la lista se muestra sin ese filtro.
- Páginas de 20. Las cuatro fechas van en dos columnas, "Inicio" y "Fin"; cada celda tiene dos líneas, "Est." (estimado, con tooltip) y "Real", con la etiqueta en gris chico; la fecha real que todavía no existe se muestra como "-". La tabla cabe en un ancho de 1280 px sin desplazamiento horizontal. La planificación se muestra como "Plan #N - Nombre". Los textos largos (código, planificación, fermentador) se acortan con puntos suspensivos dentro de un ancho máximo y su texto completo se ve en un tooltip flotante al apuntarlos o enfocarlos.
- Sin resultados: "No se encontraron lotes con los criterios indicados".

## 5. Pantalla 2: registrar lote (`/lotes/nuevo`)

Página aparte. La abre el gerente de producción desde "Nuevo lote" de la lista o desde el estado actual de una planificación Pendiente (con la planificación ya cargada).

```
< Volver a lotes
Nuevo lote
Los campos marcados con * son obligatorios.

+- Resumen de errores (solo si hay) ------------------------------------------------------+
| No se pudo crear el lote                                                              |
|  - El fermentador es obligatorio                                                          |
+-------------------------------------------------------------------------------------------+
PLANIFICACIÓN
  Planificación de producción *   [ Plan #7 - Pale Ale                         [=] ]
EQUIPAMIENTO
  Molino *            [ Seleccione molino            [=] ]   Macerador *   [ Seleccione macerador [=] ]
  Olla de hervor *    [ Seleccione olla de hervor    [=] ]   Fermentador * [ Seleccione fermentador [=] ]
PRODUCCIÓN
  Volumen objetivo (L) *   [ Ej: 300 ]
                                                              [Cancelar]  [ Crear lote ]
```

- Cada `[=]` abre el buscador en modal del campo: planificaciones pendientes (criterio "Receta"), molinos, maceradores, ollas de hervor y fermentadores (criterio "Identificador"). Los buscadores de equipos muestran todos los equipos activos con su estado operativo y su capacidad útil para ayudar a elegir.
- El registro no valida la disponibilidad de los equipos ni el stock (eso lo hace el inicio del lote); sí valida capacidades y la cobertura de la planificación, y esos mensajes se muestran en el resumen de errores.
- Éxito: la lista de lotes, con la alerta "Lote PALE_ALE-R7-3 creado correctamente". El lote queda Pendiente y lo inicia el operario, por eso el gerente no pasa por el detalle.

## 6. Pantalla 3: detalle del lote (`/lotes/{id}`)

### 6.1 Cabecera y acciones

```
< Volver a lotes
PALE_ALE-R7-3                                    [Pendiente | En ejecución | Finalizado | Cancelado]
                                              [ Iniciar lote ]o   [ Cancelar lote ]g

Planificación  Plan #7 (link solo para el gerente)    Receta  Pale Ale - versión 3 de 3
Fermentador    F-01                                   Volumen objetivo  300 L
Inicio estimado 01/07/2027   Fin estimado 05/07/2027   Inicio real 01/07/2027 08:30   Fin real -

o = solo el operario, con el lote Pendiente.     g = solo el gerente, con el lote Pendiente o En ejecución.
```

- La cabecera muestra siempre las cuatro fechas, en cualquier estado del lote: "Inicio estimado" y "Fin estimado" (las vigentes en el cronograma) e "Inicio real" y "Fin real", que se muestran con "-" mientras no existen. En un lote Cancelado, "Fin real" es la fecha de cancelación.
- "Iniciar lote" pide confirmación. "Cancelar lote" abre el diálogo de motivo. En un lote Cancelado, junto a la etiqueta de estado hay un botón de información (`bi-info-circle`, "Ver detalle de la cancelación") que abre el diálogo "Detalle de la cancelación" con el motivo, quién canceló y la fecha y hora (dd/MM/yyyy HH:mm); el motivo nunca se muestra en línea en la cabecera. Con envasados registrados el botón se muestra desactivado, con el aviso "Anule los envasados registrados para poder cancelar el lote".
- Un lote Finalizado o Cancelado no tiene ningún botón de acción.

### 6.2 Pestañas

```
[ Molienda        ] [ Maceración      ] [ Hervido         ] [ Fermentación   ] [ Maduración     ] [ Envasado       ]
[ Finalizada      ] [ Finalizada      ] [ En curso  <     ] [ Pendiente      ] [ Pendiente      ] [ Pendiente      ]
```

- Cada pestaña muestra el nombre de la etapa y su estado; la que se está viendo (`aria-current`) se resalta. Se pueden ver todas las etapas, incluidas las anteriores a la actual.
- Sin el parámetro `etapa` se abre la etapa actual en ejecución. Con el lote Pendiente se abre Molienda; con el lote Finalizado, Envasado; con el lote Cancelado, la etapa en que se canceló (la que quedó Cancelada o, si ninguna lo está, la última etapa iniciada; Molienda si se canceló sin empezar).

### 6.3 Estructura común de una pestaña

Las seis pestañas tienen las mismas secciones, en este orden. Lo que cambia es el contenido de la tarjeta guía y si la etapa consume insumos; Envasado suma la sección de envasados registrados.

```
+- 1. FRANJA DE ESTADO ------------------------------------------------------------------+
| Hervido   Estado: En curso   Equipo: OH-01   Inicio: 01/07/2027 11:30   Fin: -           |
|                                                              [ Finalizar etapa ]o        |
+- 2. TARJETA GUÍA (según la etapa) -----------------------------------------------------+
| ...                                                                                      |
+- 3. INSUMOS DE LA ETAPA ---------------------------------------------------------------+
| Insumo     Requerido (g)   Reservado (g)   Consumido (g)   Avance                       |
| Cascade    25              10              15              15 de 25 g                   |
| Citra      15              15              0               0 de 15 g                    |
| Avance para finalizar:  Cascade 60% de 80% requerido (!)                                 |
|                         Citra 0% de 80% requerido (!)                                   |
| Registre y consulte los consumos de esta etapa          [ Gestionar consumos ]c         |
+------------------------------------------------------------------------------------------+
o = solo el operario, con el lote En ejecución y esta etapa En curso.
c = quien tiene CONSUMO_INSUMO_CONSULTAR (el operario), en las etapas que consumen insumos y con cualquier estado.
```

- La franja de estado lleva el nombre de la etapa y, a la derecha, "Finalizar etapa" (en Envasado, "Finalizar envasado"), la acción principal: es un formulario POST con confirmación cuando hay JavaScript. No hay una tarjeta de acciones aparte.
- El pie de la sección de insumos tiene, a la izquierda, el texto de ayuda "Registre y consulte los consumos de esta etapa" y, a la derecha, el botón sólido ámbar "Gestionar consumos" (ícono `bi-list-check`), que lleva a la pantalla 4. Los consumos registrados no se listan en la pestaña: se consultan en esa pantalla.
- El avance, bajo la tabla, muestra por insumo el porcentaje consumido frente al mínimo exigido ("75% de 80% requerido"). Con mínimo 0 dice "Sin consumo mínimo requerido". Solo aparece con la etapa en curso. El botón siempre está habilitado: el avance es informativo y, si no se alcanza el mínimo, el service rechaza y su mensaje se muestra en una alerta.
- La tarjeta de insumos solo existe en las etapas que consumen insumos: en una etapa sin insumos (Maceración, Envasado, o una etapa que la receta no usa) no se muestra la tarjeta ni el botón "Gestionar consumos".
- En Envasado, una sección lista los envasados registrados (barril, cantidad y estado; los anulados llevan el botón "Ver detalle de la anulación") y en su cabecera lleva "Nuevo envasado" para el operario con el envasado en curso (lleva a la pantalla 5).
- Lo consumido frente a lo requerido se muestra solo en la tabla de insumos; la guía no lo repite.

### 6.4 Tarjeta guía de cada etapa

Cada dato de la guía es una etiqueta gris chica con su valor destacado debajo, igual que la cabecera del lote. Las cantidades que se piden a los insumos salen del cálculo del escalado.

**Molienda**

```
Molino            Malta a moler
M-01              Pilsen: 40 kg
                  Caramelo 60: 5 kg
```

**Maceración** (no consume insumos)

```
Malta molida a agregar   Agua de macerado (i)   Agua de lavado   Agua total
Pilsen: 16,7131 kg       139 L                  60 L             199 L
Caramelo 60: 2,0892 kg
Total: 18,8023 kg
Requerida por la receta: 20,8914 kg
Duración estimada        Macerador
60 min                   MA-01
[alertas amarillas con las advertencias del cálculo, si las hay]
```

- "Malta molida a agregar" muestra lo que el operario realmente molió: por cada malta, la suma de los consumos registrados (no anulados) de ese lote en la etapa de Molienda, más el total cuando hay más de una malta y, en gris, la cantidad requerida por la receta para comparar. Una malta sin consumos registrados no aparece en la lista.
- Si la Molienda todavía no tiene consumos registrados, la tarjeta muestra "Malta a agregar (requerida por la receta)" con la cantidad requerida y una nota que explica que todavía no hay malta molida registrada.
- El agua de macerado, el agua de lavado y el agua total se calculan con la malta molida (la misma masa que muestra la tarjeta). Sin consumos de malta en la Molienda se usa la masa requerida por la receta y el tooltip dice "Relación de empaste × malta requerida por la receta + espacio muerto del macerador"; con malta molida dice "Relación de empaste × malta molida + espacio muerto del macerador" y muestra los kilos molidos. El volumen pre-hervor no depende de la malta. Si la mezcla de agua y malta molida supera la capacidad útil del macerador, la tarjeta muestra una advertencia amarilla que no bloquea nada. Al registrar un consumo de malta en la Molienda, si el total molido hace que esa mezcla supere la capacidad útil del macerador, el consumo se rechaza con el mensaje "La malta molida (X kg en total) requiere una mezcla de Y L que supera la capacidad útil del macerador (Z L)".

- Sin agua de lavado, "Agua de lavado" dice "No se requiere".
- (i) es un botón de información (`bi-info-circle`, con `aria-label`) junto a "Agua de macerado". Muestra un tooltip flotante (`/js/tooltips.js`, tarjeta oscura con flecha, que no modifica el diseño de la página) con el cálculo y los números reales ("3 L/kg × 45 kg + 4 L de espacio muerto = 139 L") y qué es el espacio muerto: el agua que queda retenida en el fondo del macerador y no se recupera, por eso se suma. Se abre al pasar el mouse, al enfocar el botón con el teclado y con un clic o toque, y se cierra al sacar el mouse, al perder el foco, con Escape y con un clic fuera.

**Hervido**

```
Volumen pre-hervor   Duración estimada   Olla de hervor
345 L                90 min             OH-01
Lúpulos a agregar
  Saaz: 215 g, agregar al inicio del hervor
  Saaz: 25 g, agregar cuando falten 15 min para terminar el hervor
  Cascade: 125 g, agregar en el whirlpool
```

Los lúpulos son líneas de texto, sin tabla, una por adición. Cada línea lleva la cantidad propia de esa adición (un lúpulo usado dos veces aparece dos veces, sin sumar; la suma de sus adiciones es lo requerido en la tabla de insumos). Un lúpulo que hierve toda la duración del hervido se agrega "al inicio del hervor"; si no, "cuando falten N min para terminar el hervor", donde N es el tiempo de hervor de la receta. Las de whirlpool dicen "agregar en el whirlpool" y van al final. Se ordenan de la que se agrega antes a la que se agrega después.

**Fermentación**

```
Levadura a utilizar   OG objetivo   FG objetivo   Duración estimada   Fermentador
US-05: 11,5 g         1,050         1,010         10 días             F-01
Lúpulos a agregar en seco (si la receta los usa en esta etapa)
  Citra: 40 g
```

**Maduración**

```
Duración estimada   Fermentador   Lúpulos a agregar en seco
4 días              F-01          Citra: 40 g
```

**Envasado** (no consume insumos)

```
Volumen objetivo   Envasado hasta ahora   Máximo según la capacidad útil del fermentador F-01
300 L              120 L                  330 L  quedan 210 L
[El total envasado supera el volumen objetivo del lote (300 L)]    (aviso informativo, solo si corresponde)
```

## 7. Pantalla 4: consumos de una etapa (`/lotes/{id}/etapas/{etapa}/consumos`)

Página aparte (no es un modal), solo para etapas que consumen insumos. Arriba, el resumen de los insumos de la etapa; abajo, dos pestañas, "Reservados" y "Directos", que son enlaces (`?tipo=reservado`, la predeterminada, o `?tipo=directo`) y funcionan sin JavaScript. Cada pestaña lista los consumos de su tipo y lleva el botón que abre su pantalla de formulario. Las acciones vuelven a la pestaña del tipo de consumo.

```
< Volver al lote
Consumos de Hervido  ·  Lote PALE_ALE-R7-3  ·  Etapa En curso

[ Consumo creado correctamente ]                                                  (verde)
[ Este lote de insumo se encuentra vencido (venció el 01/01/2020) ]                    (amarillo, arriba)
[ El total consumido de Citra en esta etapa (20 g) supera la cantidad requerida (15 g) ] (amarillo, arriba)

INSUMOS DE LA ETAPA           Avance para finalizar: 80% como mínimo de cada insumo
  Insumo    Requerido   Reservado   Consumido   Avance
  Cascade   25 g        10 g        15 g        60%
  Citra     15 g        15 g        0 g         0%

[ Reservados ] [ Directos ]
  Estado [ Todos v ] [ Filtrar ]                                  [ + Nuevo consumo reservado ]o
  Insumo   Lote de insumo   Cantidad   Fecha de registro   Estado                         Acciones
  Cascade  L-123            15 g       01/07/2027 09:10    Registrado                     [ Anular ]o
  Cascade  L-124 [Vencido]   10 g       01/07/2027 09:05    Anulado (i)
  45 consumos · Página 1 de 3
o = solo con el lote En ejecución y la etapa En curso.
```

- El resumen lista los insumos de la etapa con lo requerido, lo reservado (el saldo que todavía no se consumió), lo consumido y el avance, y a la derecha de su cabecera "Avance para finalizar" con el porcentaje mínimo exigido ("Sin consumo mínimo requerido" cuando es 0).
- Cada pestaña muestra solo los consumos de su tipo, de a 20 y con el último registrado arriba. El tipo, el estado y la página viajan en la URL y se conservan al cambiar de página; después de registrar o anular se vuelve a la pestaña del consumo.
- El filtro "Estado" es un desplegable del enum chico (Todos, Registrado, Anulado) y, por defecto, muestra todos los estados: la pantalla es el historial de la etapa y por eso incluye lo anulado. Un valor inválido se ignora con un aviso amarillo y se muestran todos. Sin consumos dice "No hay consumos reservados registrados en esta etapa", "No hay consumos directos registrados en esta etapa" o, con un estado elegido, "No hay consumos en el estado indicado".
- Un consumo anulado se muestra atenuado, con un botón de información (i, "Ver detalle de la anulación") junto al estado y sin botón "Anular". El botón abre el diálogo "Detalle de la anulación" con el motivo, "Anulado por" (el usuario de la última modificación de su auditoría) y la fecha y hora; el motivo nunca va en la fila. Si el lote de insumo ya estaba vencido el día en que se registró el consumo, la fila muestra la marca "Vencido" junto a la identificación del lote, con el tooltip "Venció el dd/MM/yyyy"; vale para las dos pestañas. "Anular" abre el diálogo de motivo. El Controller verifica que el consumo a anular pertenezca a la etapa y al lote de la ruta.
- "Nuevo consumo reservado" y "Nuevo consumo directo" (`CONSUMO_INSUMO_REGISTRAR`) llevan a la pantalla de formulario del tipo. Si el lote no está En ejecución o la etapa no está En curso, se muestra el aviso "La etapa de Hervido no está en curso, solo se pueden consultar los consumos" y desaparecen esos botones y los botones "Anular"; el filtro por estado sigue disponible.
- La pantalla se abre desde el botón "Gestionar consumos" de la sección de insumos del detalle del lote.

### 7.1 Formulario de un consumo (`.../consumos/reservado/nuevo` y `.../consumos/directo/nuevo`)

```
< Volver a los consumos
Nuevo consumo reservado  ·  Lote PALE_ALE-R7-3  ·  Etapa Hervido
Los campos marcados con * son obligatorios.

+- DATOS DEL CONSUMO RESERVADO -------------------------------------------------------+
|  Insumo *            Lote de insumo *                 Cantidad (g) *                |
|  [ Seleccione insumo [=] ]  [ Seleccione lote de insumo [=] ]  [ Ej: 10 ]           |
|                      Vencimiento: 12/03/2028 · Reservado: 10 g                      |
+--------------------------------------------------------------------------------------+
                                         [ Cancelar ] [ Crear consumo reservado ]
```

- Es un formulario como los demás del sistema: una tarjeta con su cabecera de sección, campos de ancho normal en una grilla responsive (una, dos o tres columnas según el ancho), asterisco rojo en los obligatorios, placeholder de ejemplo en cada campo, el aviso "Los campos marcados con * son obligatorios." y, al pie, "Cancelar" y el botón principal.

- El insumo y el lote de insumo se eligen con el buscador en modal (campo de solo lectura, id oculto y selector de entidad). El insumo es el primer campo y el lote queda deshabilitado ("Elija primero el insumo") hasta elegirlo; al cambiar el insumo se quita el lote elegido. Al elegir el insumo, la unidad de la cantidad ("Cantidad (g)") se completa con la del insumo; al elegir el lote, la pantalla muestra su vencimiento (con la marca "Vencido" si corresponde) y la cantidad ofrecida.
- El buscador de insumos ofrece solo los insumos de la etapa en curso del lote, con su tipo (Malta, Lúpulo o Levadura) y lo requerido, reservado y consumido, y un criterio "Nombre": en el consumo reservado, los insumos con saldo reservado; en el directo, todos los que la etapa requiere. El servidor valida que el insumo pertenezca a la etapa y que el lote sea de ese insumo.
- El buscador de lotes del consumo reservado ofrece solo los lotes del insumo elegido que tienen una reserva con saldo para la etapa en curso del lote (la cantidad que se muestra es el saldo reservado). El del consumo directo ofrece todos los lotes del insumo elegido con cantidad disponible (la cantidad que se muestra es la disponible). Ambos tienen como único criterio la "Identificación del lote" (el insumo ya está elegido) y se paginan de a 8. El orden es FEFO: primero los lotes vigentes por fecha de vencimiento ascendente (el que vence antes, primero), después los lotes sin fecha de vencimiento y al final los vencidos, también por fecha, marcados con "Vencido".
- Errores: un error de validación (`@Valid`) redirige al formulario, que muestra el resumen de errores, lo escrito y el insumo y el lote elegidos; un rechazo del service redirige al origen con su mensaje. Al registrar con éxito se vuelve a la pestaña del tipo con la alerta de éxito y, si las hay, las advertencias (lote vencido, total consumido mayor a lo requerido). Con el lote fuera de ejecución o la etapa fuera de curso el formulario no se abre: se vuelve a la pestaña con el aviso "Solo se pueden registrar consumos con el lote en ejecución y la etapa en curso".

## 8. Pantalla 5: envasados del lote (`/lotes/{id}/envasados`)

Página aparte. Todas sus acciones vuelven a ella.

```
< Volver al lote
Envasados  ·  Lote PALE_ALE-R7-3  ·  Etapa En curso

[ Envasado creado correctamente ]                                                 (verde)

120 L de 330 L (capacidad útil del fermentador F-01)
[#######-------------]    Volumen objetivo: 300 L    Quedan 210 L

REGISTRAR ENVASADO
  Barril *  [ Seleccione barril          [=] ]     Cantidad a envasar (L) * [ Ej: 50 ]
                                                                [ Crear envasado ]

ENVASADOS REGISTRADOS
  Barril   Cantidad (L)   Fecha de registro   Estado                         Acciones
  B-003    20             01/07/2027 10:30    Registrado                     [ Anular ]
  B-004    30             01/07/2027 10:20    Anulado - Barril equivocado
  B-002    50             01/07/2027 10:10    Registrado                     [ Anular ]
  B-001    50             01/07/2027 10:00    Registrado                     [ Anular ]
```

- El único tope del envasado es la capacidad útil del fermentador: se puede envasar más que el volumen objetivo, y superarlo solo muestra un aviso informativo.
- "Seleccione barril" abre el buscador de barriles Disponibles (criterio "Identificador").
- La pantalla no finaliza el envasado: eso se hace en el detalle del lote ("Finalizar envasado").
- Errores: un error de validación (`@Valid`) redirige a esta misma pantalla, que muestra los errores y lo escrito; un rechazo del service redirige al origen con su mensaje. El Controller verifica que el envasado a anular pertenezca al lote de la ruta.
- Sin el Envasado en curso, la pantalla es de solo lectura (aviso, sin formulario ni "Anular").

## 9. Cronograma de producción para el operario (`/cronograma-produccion`)

El operario ve el mismo diagrama, los mismos filtros y las mismas colas que el gerente de producción, en solo lectura:

```
Cola pendiente de F-01
  #     Lote            Receta     Planificación   Volumen (L)   Inicio       Fin
  1     PALE_ALE-R7-2   Pale Ale   #7              300           07/07/2027   11/07/2027
  2     PALE_ALE-R7-3   Pale Ale   #7              300           13/07/2027   17/07/2027
   (el gerente de producción ve además, por fila, los botones [ Subir ] y [ Bajar ])
```

- Sin botones "Subir" ni "Bajar", sin formularios POST y sin columna de acciones. El código de cada lote de la cola es un enlace "Ver lote" al detalle (`/lotes/{id}`) para quien tiene `LOTE_CONSULTAR`. Cada barra del diagrama también es un enlace al detalle del lote (con su tooltip, su color y su posición): quien consulta lotes la abre con un clic. La cola lista solo los pendientes; el lote en ejecución se ve en el diagrama y no se repite en la cola. El bloque de limpieza del diagrama lleva un icono de destellos (`bi-stars`) que se oculta si el bloque es muy angosto.
- El filtro "Planificación a destacar" se ofrece solo a quien puede consultar planificaciones (el gerente de producción): el operario no lo ve y el parámetro `idPlanificacion` se ignora. El filtro de fermentador (con su buscador, `LOTE_CONSULTAR`) y el período los tienen ambos roles. Subir y bajar (`POST`) son del gerente (`LOTE_REGISTRAR`).

## 10. Estado de planificación: enlaces al módulo de lotes

- En el estado actual de una planificación Pendiente, solo el gerente de producción (`LOTE_REGISTRAR`) ve el botón "Nuevo lote", que abre el formulario con la planificación ya elegida. Una planificación Finalizada o Anulada no lo muestra.
- En la tabla de lotes de la planificación, cada fila tiene el link "Ver" al detalle del lote para quien puede consultar lotes.

## 11. Qué botón se ve en cada situación

| Botón | Permiso | Lote | Etapa |
| --- | --- | --- | --- |
| Nuevo lote (lista) | `LOTE_REGISTRAR` | - | - |
| Nuevo lote (estado de la planificación) | `LOTE_REGISTRAR` | - (planificación Pendiente) | - |
| Iniciar lote (con confirmación) | `LOTE_INICIAR` | Pendiente | - |
| Cancelar lote | `LOTE_ANULAR` | Pendiente o En ejecución (desactivado con envasados registrados) | - |
| Gestionar consumos (pie de los insumos) | `CONSUMO_INSUMO_CONSULTAR` | Cualquiera | Cualquiera, de una etapa que consume insumos |
| Nuevo consumo / Anular consumo (pantalla 4) | `CONSUMO_INSUMO_REGISTRAR` / `CONSUMO_INSUMO_ANULAR` | En ejecución | En curso |
| Nuevo envasado (cabecera de los envasados) | `ENVASADO_REGISTRAR` | En ejecución | Envasado en curso |
| Crear / Anular envasado (pantalla 5) | `ENVASADO_REGISTRAR` / `ENVASADO_ANULAR` | En ejecución | Envasado en curso |
| Finalizar etapa (seis, con confirmación; siempre habilitado) | `LOTE_FINALIZAR_<ETAPA>` | En ejecución | Esa etapa en curso |
| Subir / Bajar (cronograma) | `LOTE_REGISTRAR` | Pendiente | - |

## 12. Flujos de clics

Un clic es un clic de mouse o un toque. Escribir en un campo no se cuenta como clic y se indica aparte. Se parte de cualquier pantalla con la sidebar visible. El detalle se abre solo en la etapa actual en ejecución, por lo que operar sobre ella no requiere elegir pestaña.

| Objetivo | Rol | Pasos | Clics |
| --- | --- | --- | --- |
| Ver el cronograma | Gerente / operario (solo lectura) | "Cronograma de Producción" | 1 |
| Ver los lotes en ejecución | Ambos | "Gestión de Lotes"; elegir estado "En ejecución" (1 clic para abrir la lista y 1 para elegir) y "Buscar" | 4 |
| Abrir un lote | Ambos | "Gestión de Lotes"; "Gestionar lote" en la fila | 2 |
| Filtrar los lotes de una planificación | Ambos | "Gestión de Lotes"; abrir el selector de planificación; "Elegir"; "Buscar" | 4 |
| Registrar un lote desde la lista | Gerente | "Gestión de Lotes"; "Nuevo lote"; planificación (abrir el selector y "Elegir"); los cuatro equipos (abrir y "Elegir", 2 clics cada uno); escribir el volumen; "Registrar lote" (vuelve a la lista con el mensaje de éxito) | 13 y 1 escritura |
| Registrar un lote desde una planificación | Gerente | "Planificación de Producción"; "Ver estado actual"; "Nuevo lote" (la planificación ya está elegida); los cuatro equipos (8 clics); escribir el volumen; "Registrar lote" (vuelve a la lista con el mensaje de éxito) | 12 y 1 escritura |
| Iniciar un lote | Operario | lista ("Gestión de Lotes") -> abrir el lote -> "Iniciar lote" -> confirmar | 4 |
| Cancelar un lote | Gerente | "Gestión de Lotes"; abrir el lote; "Cancelar lote"; escribir el motivo; confirmar | 4 y 1 escritura |
| Registrar un consumo reservado (primero del día) | Operario | "Gestión de Lotes"; abrir el lote (el detalle abre en la etapa actual); "Gestionar consumos"; "Nuevo consumo reservado"; elegir el insumo y el lote de insumo con los buscadores; escribir la cantidad; "Crear consumo reservado" | 9 y 1 escritura |
| Registrar otro consumo reservado (ya en la pestaña) | Operario | "Nuevo consumo reservado"; elegir el insumo; elegir el lote de insumo; escribir la cantidad; "Crear consumo reservado" | 6 y 1 escritura |
| Registrar un consumo directo | Operario | "Gestión de Lotes"; abrir el lote; "Gestionar consumos"; pestaña "Directos"; "Nuevo consumo directo"; elegir el insumo ("Elegir"); abrir el selector del lote de insumo; "Elegir"; escribir la cantidad; "Crear consumo directo" | 10 y 1 escritura |
| Anular un consumo | Operario | "Gestión de Lotes"; abrir el lote; "Gestionar consumos"; "Anular" en la fila; escribir el motivo; confirmar | 5 y 1 escritura |
| Finalizar la etapa en curso | Operario | "Gestión de Lotes"; abrir el lote; "Finalizar etapa"; confirmar (vuelve al detalle, que abre la etapa siguiente) | 4 |
| Registrar el primer envasado | Operario | "Gestión de Lotes"; abrir el lote; "Nuevo envasado"; abrir el selector del barril; "Elegir"; escribir la cantidad; "Crear envasado" | 6 y 1 escritura |
| Registrar otro envasado (ya en la pantalla) | Operario | abrir el selector del barril; "Elegir"; escribir la cantidad; "Crear envasado" | 3 y 1 escritura |
| Finalizar el envasado | Operario | "Volver al lote" (o "Gestión de Lotes" y abrir el lote); "Finalizar envasado"; confirmar | 3 (4 desde la sidebar) |
| Revisar qué se consumió en una etapa pasada | Operario | abrir el lote; clic en la pestaña de la etapa; "Gestionar consumos" | 4 |
| Revisar los envasados de un lote | Gerente | "Gestión de Lotes"; abrir el lote; pestaña "Envasado" | 3 |
