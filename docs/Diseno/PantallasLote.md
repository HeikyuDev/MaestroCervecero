# Diseño de las pantallas del módulo de Lotes

Este documento define las pantallas del Controller del módulo de Lotes (Control de Lotes): qué pantallas existen, sus rutas, qué rol puede hacer qué, cómo está armada cada una y cuántos clics lleva cada objetivo del usuario. El comportamiento verificable de cada endpoint está en `docs/test/casos_de_prueba/controllers/lote/`; las reglas de negocio, en `docs/test/casos_de_prueba/services/lote/` y en `docs/Dominio/Escalado/`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML y fragmentos HTML para los modales. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

## 1. Roles y permisos

| Acción | Permiso | Gerente de producción | Operario de producción |
| --- | --- | --- | --- |
| Ver la lista y el detalle de lotes | `LOTE_CONSULTAR` | Sí | Sí |
| Registrar un lote | `LOTE_REGISTRAR` | Sí | No |
| Cancelar un lote | `LOTE_ANULAR` | Sí | No |
| Iniciar un lote | `LOTE_INICIAR` | No | Sí |
| Finalizar una etapa (seis permisos, uno por etapa) | `LOTE_FINALIZAR_<ETAPA>` | No | Sí |
| Ver la pantalla de consumos de una etapa | `CONSUMO_INSUMO_CONSULTAR` | No | Sí |
| Registrar un consumo (reservado o directo) | `CONSUMO_INSUMO_REGISTRAR` | No | Sí |
| Anular un consumo | `CONSUMO_INSUMO_ANULAR` | No | Sí |
| Ver la pantalla de envasados | `ENVASADO_CONSULTAR` | No | Sí |
| Registrar un envasado | `ENVASADO_REGISTRAR` | No | Sí |
| Anular un envasado | `ENVASADO_ANULAR` | No | Sí |
| Ver el cronograma de producción | `LOTE_CONSULTAR` | Sí, con acciones | Sí, solo lectura |
| Subir o bajar un lote en la cola | `LOTE_REGISTRAR` | Sí | No |
| Registrar un lote desde una planificación | `LOTE_REGISTRAR` y `PLANIFICACION_PRODUCCION_CONSULTAR` | Sí | No |

- Cada método del Controller lleva `@PreAuthorize("hasAuthority('<PERMISO>')")` con el permiso de la tabla, y cada botón se oculta con `sec:authorize` por el mismo permiso. El servidor es quien rechaza: ocultar el botón es solo una ayuda visual.
- El gerente de producción ve en el detalle del lote todo lo que ocurrió (consumos de cada etapa, fechas de inicio y fin de cada etapa y envasados) sin botones de operación. Las pantallas de consumos y de envasados son de operación y las abre el operario.
- Un acceso no autorizado redirige con la alerta "No tiene permisos para realizar esta acción".

## 2. Inventario de pantallas y rutas

| N.º | Pantalla | Ruta | Método | Permiso | Vista |
| --- | --- | --- | --- | --- | --- |
| 1 | Lista de lotes | `/lotes` | GET | `LOTE_CONSULTAR` | `lote/lote-lista` |
| 2 | Registrar lote | `/lotes/nuevo` (`?idPlanificacion=`) | GET | `LOTE_REGISTRAR` | `lote/lote-form` |
| 2 | Registrar lote (envío) | `/lotes` | POST | `LOTE_REGISTRAR` | redirige a `/lotes/{id}` |
| 3 | Detalle del lote | `/lotes/{id}` (`?etapa=`) | GET | `LOTE_CONSULTAR` | `lote/lote-detalle` |
| 3 | Iniciar lote | `/lotes/{id}/iniciar` | POST | `LOTE_INICIAR` | redirige a `/lotes/{id}` |
| 3 | Cancelar lote | `/lotes/{id}/cancelar` | POST | `LOTE_ANULAR` | redirige a `/lotes/{id}` |
| 3 | Finalizar etapa (seis) | `/lotes/{id}/etapas/{etapa}/finalizar` | POST | `LOTE_FINALIZAR_<ETAPA>` | redirige a `/lotes/{id}` |
| 4 | Consumos de una etapa | `/lotes/{id}/etapas/{etapa}/consumos` | GET | `CONSUMO_INSUMO_CONSULTAR` | `lote/consumo-lista` |
| 4 | Registrar consumo reservado | `/lotes/{id}/etapas/{etapa}/consumos/reservado` | POST | `CONSUMO_INSUMO_REGISTRAR` | redirige a la misma pantalla |
| 4 | Registrar consumo directo | `/lotes/{id}/etapas/{etapa}/consumos/directo` | POST | `CONSUMO_INSUMO_REGISTRAR` | redirige a la misma pantalla |
| 4 | Anular consumo | `/lotes/{id}/etapas/{etapa}/consumos/{idConsumo}/anular` | POST | `CONSUMO_INSUMO_ANULAR` | redirige a la misma pantalla |
| 5 | Envasados del lote | `/lotes/{id}/envasados` | GET | `ENVASADO_CONSULTAR` | `lote/envasado-lista` |
| 5 | Registrar envasado | `/lotes/{id}/envasados` | POST | `ENVASADO_REGISTRAR` | redirige a la misma pantalla |
| 5 | Anular envasado | `/lotes/{id}/envasados/{idEnvasado}/anular` | POST | `ENVASADO_ANULAR` | redirige a la misma pantalla |
| M | Buscadores en modal (fragmentos) | `/lotes/buscador/{planificaciones, molinos, maceradores, ollas, fermentadores}`, `/lotes/{id}/etapas/{etapa}/consumos/buscador/lotes-insumo`, `/lotes/{id}/envasados/buscador/barriles` | GET | `LOTE_REGISTRAR`, `CONSUMO_INSUMO_REGISTRAR` y `ENVASADO_REGISTRAR` | `lote/buscador-*` |
| C | Cronograma de producción | `/cronograma-produccion` | GET | `LOTE_CONSULTAR` | `cronograma/cronograma` |

- `{etapa}` toma los valores `molienda`, `maceracion`, `hervido`, `fermentacion`, `maduracion` y `envasado`. La pantalla de consumos existe para las etapas que consumen insumos (Molienda, Hervido, Fermentación y Maduración); Maceración y Envasado no consumen insumos.
- La sidebar (sección Operación) muestra "Control de Lotes" (`/lotes`) para el gerente y el operario de producción, y "Cronograma de Producción" para ambos.

## 3. Reglas comunes de las pantallas

- **Post-Redirect-Get.** Toda acción exitosa redirige y muestra una alerta de éxito en el layout; recargar la página no repite la acción. Registrar un lote lleva al detalle del lote nuevo; iniciar, cancelar y finalizar vuelven al detalle (que abre la etapa en curso); registrar o anular un consumo o un envasado vuelve a la misma pantalla de consumos o de envasados.
- **Errores.** El mensaje de un rechazo del service se muestra como alerta (amarilla si es una regla de negocio, roja si el recurso no existe). Los errores de validación del formulario (`@Valid`) y los números que no se pueden convertir vuelven a mostrar la misma pantalla con un resumen de errores y lo que el usuario había cargado.
- **Advertencias.** Las advertencias de un consumo (lote de insumo vencido, total consumido mayor a lo requerido) no bloquean la operación: se muestran como alertas amarillas debajo de la alerta de éxito.
- **Formularios.** Son `novalidate`, el JavaScript no valida y el servidor es la fuente de verdad. Los campos obligatorios llevan asterisco rojo, todo campo lleva un placeholder de ejemplo ("Ej: 300") y las unidades van en la etiqueta o en la cabecera de la columna.
- **Selector de entidad.** Elegir una entidad relacionada (planificación, equipo, lote de insumo, barril) abre un modal con criterios de filtro y resultados paginados de a 8; nunca es un desplegable con todos los registros. Es solo para elegir: no hay alta al vuelo en estas pantallas.
- **Confirmaciones.** Iniciar un lote y finalizar una etapa se envían con un formulario POST y token CSRF; con JavaScript piden antes una confirmación en un diálogo, y sin JavaScript se envían directamente. Cancelar un lote y anular un consumo o un envasado piden un motivo obligatorio en un diálogo.
- **Pestañas.** Las seis pestañas del detalle son enlaces (`?etapa=<etapa>`), de modo que funcionan sin JavaScript y se pueden guardar en favoritos.
- **Orden de los listados.** Los registros transaccionales (lotes, consumos, envasados) muestran el último registrado arriba; los maestros (equipos, barriles) van en orden alfabético ascendente por identificador.
- **Estados.** Siempre con su nombre legible: lote (Pendiente, En ejecución, Finalizado, Cancelado) y etapa (Pendiente, En curso, Finalizada, Cancelada).

## 4. Pantalla 1: lista de lotes (`/lotes`)

```
Control de Lotes                                                  [ + Registrar lote ]*
Lotes registrados, con el último registrado arriba.

+- Criterios ---------------------------------------------------------------------------+
| Planificación        Fermentador          Código             Estado                   |
| [Seleccione plani..] [Seleccione ferm..]  [Ej: PALE_ALE-R7-3] [ Todos            v]   |
|                                                                [Limpiar] [ Buscar ]   |
+----------------------------------------------------------------------------------------+

+- Resultados --------------------------------------------------------------------------+
| Código            Planificación     Fermentador  Estado        Etapa actual  Estimado   Real          |
| PALE_ALE-R7-3     #7 - Pale Ale     F-01         En ejecución  Hervido       01/07/2027  01/07/2027 08:30  [Ver] |
|                                                                               05/07/2027  -                    |
| STOUT-R8-1        #8 - Stout        F-02         Pendiente     -             12/07/2027  -             [Ver] |
|                                                                               20/07/2027  -                    |
| 45 lotes · Página 1 de 3                                          [< Anterior] [Siguiente >]  |
+----------------------------------------------------------------------------------------+
* "Registrar lote" solo lo ve el gerente de producción.
```

- Criterios opcionales y combinables; "Planificación" y "Fermentador" son selectores de entidad (campo en blanco con ícono de lista que abre un buscador en modal, nunca un desplegable) con botón para quitar la selección. El buscador de planificaciones filtra por "Receta"; el de fermentadores, por "Identificador", "Estado operativo" (desplegable del enum) e "Incluir dados de baja" (casilla desmarcada por defecto, que suma los fermentadores dados de baja que tienen lotes para poder filtrar su historial); el elegido se muestra en el campo, con "(dado de baja)" si corresponde. Solo "Estado" es un desplegable, porque es un enum chico.
- Páginas de 20. Desde el breakpoint `xl` las cuatro fechas (inicio y fin, estimados y reales) pasan a columnas propias.
- Sin resultados: "No se encontraron lotes con los criterios indicados".

## 5. Pantalla 2: registrar lote (`/lotes/nuevo`)

Página aparte. Se llega desde "Registrar lote" de la lista o desde el estado actual de una planificación pendiente (con la planificación ya elegida).

```
< Volver a lotes
Registrar lote
Los campos marcados con * son obligatorios.

+- Resumen de errores (solo si hay) ------------------------------------------------------+
| No se pudo registrar el lote                                                              |
|  - El fermentador es obligatorio                                                          |
+-------------------------------------------------------------------------------------------+
PLANIFICACIÓN
  Planificación de producción *   [ Plan #7 - Pale Ale                         [=] ]
EQUIPAMIENTO
  Molino *            [ Seleccione molino            [=] ]   Macerador *   [ Seleccione macerador [=] ]
  Olla de hervor *    [ Seleccione olla de hervor    [=] ]   Fermentador * [ Seleccione fermentador [=] ]
PRODUCCIÓN
  Volumen objetivo (L) *   [ Ej: 300 ]
                                                              [Cancelar]  [ Registrar lote ]
```

- Cada `[=]` abre el buscador en modal del campo: planificaciones pendientes (criterio "Receta"), molinos, maceradores, ollas de hervor y fermentadores (criterio "Identificador"). Los buscadores de equipos muestran el estado operativo y la capacidad útil de cada uno para ayudar a elegir.
- El registro no valida la disponibilidad de los equipos ni el stock (eso lo hace el inicio del lote); sí valida capacidades y la cobertura de la planificación, y esos mensajes se muestran en el resumen de errores.
- Éxito: el detalle del lote nuevo, con la alerta "Lote PALE_ALE-R7-3 registrado correctamente".

## 6. Pantalla 3: detalle del lote (`/lotes/{id}`)

### 6.1 Cabecera y acciones

```
< Volver a lotes
PALE_ALE-R7-3                                    [Pendiente | En ejecución | Finalizado | Cancelado]
                                              [ Iniciar lote ]o   [ Cancelar lote ]g

Planificación  Plan #7 (link solo para el gerente)    Receta  Pale Ale - versión 3 de 3
Fermentador    F-01                                   Volumen objetivo  300 L
Inicio estimado 01/07/2027   Fin estimado 05/07/2027  Inicio real 01/07/2027 08:30   Fin real -
Motivo de la cancelación (03/07/2027 10:15): Se detectó una contaminación en el mosto   (solo si fue cancelado)

o = solo el operario, con el lote Pendiente.     g = solo el gerente, con el lote Pendiente o En ejecución.
```

- "Cancelar lote" abre el diálogo de motivo. Con envasados registrados el botón se muestra desactivado, con el aviso "Anule los envasados registrados para poder cancelar el lote".
- Un lote Finalizado o Cancelado no tiene ningún botón de acción.

### 6.2 Pestañas

```
[ Molienda        ] [ Maceración      ] [ Hervido         ] [ Fermentación   ] [ Maduración     ] [ Envasado       ]
[ Finalizada      ] [ Finalizada      ] [ En curso  <     ] [ Pendiente      ] [ Pendiente      ] [ Pendiente      ]
```

- Cada pestaña muestra el nombre de la etapa y su estado; la actual (`aria-current`) se resalta. Sin el parámetro `etapa` se abre la etapa en curso.

### 6.3 Estructura común de una pestaña

Las seis pestañas tienen exactamente las mismas cinco secciones, en este orden. Lo que cambia es el contenido de la tarjeta guía y si la etapa consume insumos.

```
+- 1. FRANJA DE ESTADO ------------------------------------------------------------------+
| Estado: En curso     Equipo: OH-01     Inicio: 01/07/2027 11:30     Fin: -              |
+- 2. TARJETA GUÍA (según la etapa) -----------------------------------------------------+
| ...                                                                                      |
+- 3. INSUMOS DE LA ETAPA ---------------------------------------------------------------+
| Insumo     Requerido (g)   Reservado (g)   Consumido (g)   Avance                       |
| Cascade    25              10              15              15 de 25 g                   |
| Citra      15              15              0               0 de 15 g                    |
|   (o "Esta etapa no consume insumos")                                                    |
+- 4. CONSUMOS REGISTRADOS (solo lectura) -----------------------------------------------+
| Insumo   Lote de insumo   Tipo       Cantidad   Estado                                   |
| Cascade  L-123            Directo    15 g       Registrado                               |
| Cascade  L-124            Reservado  10 g       Anulado - Error de carga                 |
|   (o "No hay consumos registrados en esta etapa")                                        |
+- 5. ACCIONES --------------------------------------------------------------------------+
| [ Registrar consumo ]o     Avance para finalizar:  Cascade 60% de 80% requerido (!)      |
|                                                    Citra 0% de 80% requerido (!)         |
|                            [ Finalizar etapa ]o                                          |
+------------------------------------------------------------------------------------------+
o = solo el operario, con el lote En ejecución y esta etapa En curso.
```

- "Registrar consumo" lleva a la pantalla 4. "Finalizar etapa" es un formulario POST (con confirmación cuando hay JavaScript).
- El avance muestra, por insumo, el porcentaje consumido frente al mínimo exigido ("75% de 80% requerido"). Con mínimo 0 dice "Sin consumo mínimo requerido". El botón no se desactiva por el avance: si no se alcanza el mínimo, el service rechaza y su mensaje se muestra en una alerta.
- En una pestaña de una etapa que todavía no está en curso, o ya terminó, las acciones no aparecen y las secciones 3 y 4 se ven igual.

### 6.4 Tarjeta guía de cada etapa

**Molienda**

```
Moler la malta de la receta en el molino M-01
  Pilsen        20 de 40 kg
  Caramelo 60   0 de 5 kg
La malta se consume en esta etapa. Se pueden moler varias maltas a la vez.
```

**Maceración** (no consume insumos)

```
Malta molida a agregar: 45 kg
Agua de macerado: 139 L
   3 L/kg x 45 kg + 4 L de espacio muerto = 139 L
   Incluye el espacio muerto del macerador
Agua de lavado: 60 L            (o: "No se requiere agua de lavado")
Agua total: 199 L
Duración estimada: 60 minutos            Macerador: MA-01
[alertas amarillas con las advertencias del cálculo, si las hay]
```

**Hervido**

```
Volumen pre-hervor: 345 L          Duración estimada: 90 minutos          Olla de hervor: OH-01
Lúpulos                      Cantidad (g)   Tiempo de hervor (min)
Cascade                      25             60
Citra                        15             -
```

**Fermentación**

```
Levadura: US-05   0 de 11,5 g
OG objetivo: 1,050        FG objetivo: 1,010
Duración estimada: 10 días        Fermentador: F-01
```

**Maduración**

```
Duración estimada: 4 días        Fermentador: F-01
Insumos de la etapa: Citra 0 de 40 g   (dry hop)
```

**Envasado** (no consume insumos; las acciones son de envasado)

```
Volumen objetivo: 300 L
Envasado hasta ahora: 120 L
Máximo según la capacidad útil del fermentador F-01: 330 L (quedan 210 L)
[Superó el volumen objetivo por 10 L]    (aviso informativo, solo si corresponde)

Acciones:  [ Registrar envasado ]o    [ Finalizar envasado ]o
```

La sección 4 de esta pestaña lista los envasados registrados (barril, cantidad, estado y motivo si fue anulado).

## 7. Pantalla 4: consumos de una etapa (`/lotes/{id}/etapas/{etapa}/consumos`)

Página aparte (no es un modal), solo para etapas que consumen insumos. Todas sus acciones vuelven a ella.

```
< Volver al lote
Consumos de Hervido                                           Lote PALE_ALE-R7-3 - Etapa En curso

[ Consumo registrado correctamente ]                                                  (verde)
[ Este lote de insumo se encuentra vencido (venció el 01/01/2020) ]                    (amarillo)

RESUMEN                       Porcentaje mínimo para avanzar de etapa: 80%
  Cascade   15 de 25 g   reservado 10 g   60% de 80% requerido
  Citra     0 de 15 g    reservado 15 g   0% de 80% requerido

INSUMOS RESERVADOS
  Cascade
   Lote de insumo   Vencimiento   Saldo reservado (g)   Cantidad (g)     
   L-123            12/03/2028    10                    [ Ej: 10 ]  [ Registrar ]
   L-124            -             0 (Agotada)           (atenuada, sin campo)
  Citra
   C-900 [Vencido]  01/01/2020    15                    [ Ej: 10 ]  [ Registrar ]
     ! Este lote de insumo se encuentra vencido

CONSUMO DIRECTO
  Lote de insumo *  [ Seleccione lote de insumo      [=] ]     Cantidad (g o kg, según el insumo) * [ Ej: 10 ]
                                                                       [ Registrar consumo directo ]

CONSUMOS REGISTRADOS
  Insumo   Lote de insumo   Tipo       Cantidad   Estado                      Acciones
  Cascade  L-123            Directo    15 g       Registrado                  [ Anular ]
  Cascade  L-124            Reservado  10 g       Anulado - Error de carga
  45 consumos · Página 1 de 3
```

- Cada reserva es su propio formulario (lote de insumo oculto y cantidad), de modo que "Registrar" consume exactamente de ese lote reservado.
- Las reservas con saldo van primero; las agotadas se muestran atenuadas. Un lote vencido se marca con "Vencido" y una advertencia, pero puede consumirse.
- En el consumo directo, "Seleccione lote de insumo" abre el buscador de lotes de insumo, que ofrece solo lotes de los insumos de la etapa con cantidad disponible, marca los vencidos y permite filtrar por insumo y por identificación del lote.
- "Anular" abre el diálogo de motivo.
- Si el lote no está En ejecución o la etapa no está En curso, se muestra el aviso "La etapa de Hervido no está en curso, solo se pueden consultar los consumos" y desaparecen los formularios y los botones "Anular".

## 8. Pantalla 5: envasados del lote (`/lotes/{id}/envasados`)

Página aparte. Todas sus acciones vuelven a ella.

```
< Volver al lote
Envasados del lote PALE_ALE-R7-3                                       Etapa En curso

[ Envasado registrado correctamente ]                                                 (verde)

120 L de 330 L (capacidad útil del fermentador F-01)
[#######-------------]    Volumen objetivo: 300 L    Quedan 210 L

REGISTRAR ENVASADO
  Barril *  [ Seleccione barril          [=] ]     Cantidad a envasar (L) * [ Ej: 50 ]
                                                                [ Registrar envasado ]

ENVASADOS REGISTRADOS
  Barril   Cantidad (L)   Estado                         Acciones
  B-003    20             Registrado                     [ Anular ]
  B-004    30             Anulado - Barril equivocado
  B-002    50             Registrado                     [ Anular ]
  B-001    50             Registrado                     [ Anular ]
```

- El único tope del envasado es la capacidad útil del fermentador: se puede envasar más que el volumen objetivo, y superarlo solo muestra un aviso informativo.
- "Seleccione barril" abre el buscador de barriles Disponibles (criterio "Identificador").
- La pantalla no finaliza el envasado: eso se hace en el detalle del lote ("Finalizar envasado").
- Sin el Envasado en curso, la pantalla es de solo lectura (aviso, sin formulario ni "Anular").

## 9. Cronograma de producción para el operario (`/cronograma-produccion`)

El operario ve el mismo diagrama, los mismos filtros y las mismas colas que el gerente de producción, en solo lectura:

```
Cola pendiente de F-01
  En ejecución (no se puede mover)   PALE_ALE-R7-1     Fin estimado 05/07/2027
  Pos.  Lote            Receta     Plan   Volumen (L)   Inicio       Fin
  1     PALE_ALE-R7-2   Pale Ale   #7     300           07/07/2027   11/07/2027
  2     PALE_ALE-R7-3   Pale Ale   #7     300           13/07/2027   17/07/2027
   (el gerente de producción ve además, por fila, los botones [ Subir ] y [ Bajar ])
```

- Sin botones "Subir" ni "Bajar", sin formularios POST y sin columna de acciones.
- El selector "Planificación a destacar" se ofrece solo a quien puede consultar planificaciones (el gerente de producción).

## 10. Estado de planificación: enlaces al módulo de lotes

- En el estado actual de una planificación Pendiente, el gerente de producción ve el botón "Registrar lote", que abre el formulario con la planificación ya elegida.
- En la tabla de lotes de la planificación, cada fila tiene el link "Ver" al detalle del lote para quien puede consultar lotes.

## 11. Qué botón se ve en cada situación

| Botón | Permiso | Lote | Etapa |
| --- | --- | --- | --- |
| Registrar lote (lista, planificación) | `LOTE_REGISTRAR` | - | - |
| Iniciar lote | `LOTE_INICIAR` | Pendiente | - |
| Cancelar lote | `LOTE_ANULAR` | Pendiente o En ejecución (desactivado con envasados registrados) | - |
| Registrar consumo | `CONSUMO_INSUMO_REGISTRAR` | En ejecución | En curso, de una etapa que consume insumos |
| Registrar / Anular consumo (pantalla 4) | `CONSUMO_INSUMO_REGISTRAR` / `CONSUMO_INSUMO_ANULAR` | En ejecución | En curso |
| Registrar envasado | `ENVASADO_REGISTRAR` | En ejecución | Envasado en curso |
| Registrar / Anular envasado (pantalla 5) | `ENVASADO_REGISTRAR` / `ENVASADO_ANULAR` | En ejecución | Envasado en curso |
| Finalizar etapa (seis) | `LOTE_FINALIZAR_<ETAPA>` | En ejecución | Esa etapa en curso |
| Subir / Bajar (cronograma) | `LOTE_REGISTRAR` | Pendiente | - |

## 12. Flujos de clics

Un clic es un clic de mouse o un toque. Escribir en un campo no se cuenta como clic y se indica aparte. Se parte de cualquier pantalla con la sidebar visible. Las pestañas se abren solas en la etapa en curso, por lo que operar sobre la etapa actual no requiere elegir pestaña.

| Objetivo | Rol | Pasos | Clics |
| --- | --- | --- | --- |
| Ver el cronograma | Gerente / operario | "Cronograma de Producción" | 1 |
| Ver los lotes en ejecución | Ambos | "Control de Lotes"; elegir estado "En ejecución" (1 clic para abrir la lista y 1 para elegir) y "Buscar" | 4 |
| Abrir un lote | Ambos | "Control de Lotes"; "Ver" en la fila | 2 |
| Registrar un lote desde la lista | Gerente | "Control de Lotes"; "Registrar lote"; planificación (abrir el selector y "Elegir"); los cuatro equipos (abrir y "Elegir", 2 clics cada uno); escribir el volumen; "Registrar lote" | 13 y 1 escritura |
| Registrar un lote desde una planificación | Gerente | "Planificación de Producción"; "Ver estado actual"; "Registrar lote" (la planificación ya está elegida); los cuatro equipos (8 clics); escribir el volumen; "Registrar lote" | 12 y 1 escritura |
| Iniciar un lote | Operario | "Control de Lotes"; "Ver"; "Iniciar lote"; confirmar | 4 |
| Cancelar un lote | Gerente | "Control de Lotes"; "Ver"; "Cancelar lote"; escribir el motivo; confirmar | 4 y 1 escritura |
| Registrar un consumo reservado (primero del día) | Operario | "Control de Lotes"; "Ver"; "Registrar consumo"; escribir la cantidad; "Registrar" | 4 y 1 escritura |
| Registrar otro consumo reservado (ya en la pantalla) | Operario | escribir la cantidad; "Registrar" | 1 y 1 escritura |
| Registrar un consumo directo | Operario | "Control de Lotes"; "Ver"; "Registrar consumo"; abrir el selector del lote de insumo; "Elegir"; escribir la cantidad; "Registrar consumo directo" | 6 y 1 escritura |
| Anular un consumo | Operario | "Control de Lotes"; "Ver"; "Registrar consumo"; "Anular" en la fila; escribir el motivo; confirmar | 5 y 1 escritura |
| Finalizar la etapa en curso | Operario | "Control de Lotes"; "Ver"; "Finalizar etapa"; confirmar | 4 |
| Registrar el primer envasado | Operario | "Control de Lotes"; "Ver"; "Registrar envasado"; abrir el selector del barril; "Elegir"; escribir la cantidad; "Registrar envasado" | 6 y 1 escritura |
| Registrar otro envasado (ya en la pantalla) | Operario | abrir el selector del barril; "Elegir"; escribir la cantidad; "Registrar envasado" | 3 y 1 escritura |
| Finalizar el envasado | Operario | "Volver al lote" (o "Control de Lotes" y "Ver"); "Finalizar envasado"; confirmar | 3 (4 desde la sidebar) |
| Revisar qué se consumió en una etapa pasada | Gerente | "Control de Lotes"; "Ver"; clic en la pestaña de la etapa | 3 |
| Revisar los envasados de un lote | Gerente | "Control de Lotes"; "Ver"; pestaña "Envasado" | 3 |
