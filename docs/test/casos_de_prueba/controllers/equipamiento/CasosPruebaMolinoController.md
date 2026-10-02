## Casos de prueba de `MolinoController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `MolinoControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IMolinoServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/equipamiento/CasosPruebaMolino.md`.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")`). Molinos es una de las cinco pestañas de "Gestión de Equipamiento" (Resumen, molinos, maceradores, ollas de hervor y fermentadores), cada una con su propia URL. El operario de producción solo ve el Resumen.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `equipamiento/molino-form`, sin redirigir, para conservar lo tipeado.
- _"Alta contextual"_ es el alta abierta desde el selector de otro formulario: llegan los parámetros `retorno` (ruta interna del formulario de origen) y `campo` (campo que recibe el id creado). Al guardar redirige a `retorno` agregando `campo=<id>` y `desdeAlta=1`.
- El estado operativo no se carga en el formulario: lo define el sistema (un equipo nuevo nace disponible) y lo cambian las operaciones de producción y de ciclo de vida.
- Todos los campos tienen un placeholder con un ejemplo.

---

### 1. `GET /equipamiento/molinos` — `listarMolinos(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MOC-01** | El GERENTE_DE_PRODUCCION ve el listado con estado operativo, datos técnicos, las pestañas y las acciones por fila | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 molino (`id: 4`, `MOL-01`, `EN_USO`, datos técnicos de ejemplo) | El rol está autorizado | Responde 200 con la vista `equipamiento/molino-lista`. Muestra el identificador, el estado como "EN USO", los datos técnicos formateados, las pestañas (Resumen y los otros tres tipos), el link de alta `/equipamiento/molinos/nuevo`, el de edición `/equipamiento/molinos/4/editar` y el botón de baja con `data-baja-url="/equipamiento/molinos/4/baja"`. El diálogo de confirmación de baja incluye el token CSRF. |
| **CP-MOC-02** | Sin filtros pide páginas de 20 ordenadas alfabéticamente por identificador, con los criterios en null | Rol `GERENTE_DE_PRODUCCION`, `GET /equipamiento/molinos` sin parámetros | Sin criterios. El equipamiento se lista alfabéticamente | Llama al service con `identificador: null`, `estadoOperativo: null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `identificadorInterno` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-MOC-03** | Los criterios llegan al service; un identificador en blanco y el select "Todos" se convierten en null | `identificador: " mol "`, `estadoOperativo: EN_USO`, `pagina: 1`; y por separado `identificador: "   "`, `estadoOperativo: ""` (opción "Todos") | Un texto en blanco o un select vacío equivale a no filtrar | Llama al service con `identificador: "mol"` y `EN_USO` en la página 1; con los valores vacíos lo llama con ambos criterios en `null`. |
| **CP-MOC-04** | Sin resultados muestra un mensaje claro en vez de una tabla vacía | Rol `GERENTE_DE_PRODUCCION`, el service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron molinos con los criterios indicados." en lugar de una tabla vacía. |
| **CP-MOC-05** | El OPERARIO_DE_PRODUCCION no accede a la gestión: el listado es rechazado y no llega al service | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. El operario solo ve el Resumen. |

---

### 2. `GET /equipamiento/molinos/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MOC-06** | El formulario de alta muestra los campos con asterisco, placeholders y la ayuda de cada regla | Rol `GERENTE_DE_PRODUCCION` | El rol está autorizado | Responde 200 con la vista `equipamiento/molino-form`. Muestra "Nuevo molino", el asterisco rojo de obligatorio, `action="/equipamiento/molinos"`, el placeholder de cada campo, el botón "Crear molino" y el link "Volver a molinos". No incluye el campo `retorno` ni el panel de errores. |
| **CP-MOC-07** | Abierto desde otro formulario (retorno y campo) los conserva y cancelar vuelve a ese formulario; un retorno externo se descarta | `retorno: "/lotes/nuevo"`, `campo: "idEquipamiento"`; y por separado `retorno: "//sitio-malicioso.com"` | Solo se aceptan rutas internas | Con una ruta interna incluye los campos ocultos `retorno` y `campo`, y el link de volver dice "Volver al formulario anterior" y apunta a `/lotes/nuevo?desdeAlta=1`. Con una ruta externa la descarta: no hay campo `retorno` ni aparece el sitio externo. |
| **CP-MOC-08** | El OPERARIO_DE_PRODUCCION no puede abrir el formulario de alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 3. `POST /equipamiento/molinos` — `altaMolino(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MOC-09** | Alta correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, identificador `MOL-01` y datos técnicos válidos, con CSRF | Formulario válido | Llama a `altaMolino` con los datos ingresados. Redirige a `/equipamiento/molinos` con la alerta "Molino creado correctamente" de tipo `success`. |
| **CP-MOC-10** | Sin identificador ni datos técnicos vuelve al formulario con todos los errores, sin llamar al service | `identificadorInterno: "  "`, sin datos técnicos | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear el molino" y un mensaje por cada campo obligatorio. No invoca al service. |
| **CP-MOC-11** | Un valor no numérico en un campo numérico muestra un mensaje en español, sin llamar al service | `rendimientoMolienda: "abc"` | El valor no se puede convertir a número | Vuelve al formulario con "Ingresá un número válido" (mensaje en español, no el texto técnico de Spring). No invoca al service. |
| **CP-MOC-12** | Un identificador duplicado vuelve al formulario con el mensaje del service y conserva lo tipeado | El service lanza `RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno 'MOL-01'")` | El service rechaza el identificador | Vuelve al formulario con el mensaje del service junto al campo y el identificador tipeado conservado. |
| **CP-MOC-13** | Una regla de negocio rechazada por el service se muestra como error general del formulario | El service lanza `ReglaNegocioException("Regla incumplida")` (ej. capacidad útil mayor a la total) | El service rechaza los datos | Vuelve al formulario y muestra el mensaje en el panel de errores, conservando lo tipeado. |
| **CP-MOC-14** | El alta contextual vuelve al formulario de origen con el equipamiento creado en el campo indicado | Datos válidos, `retorno: "/lotes/nuevo"`, `campo: "idEquipamiento"`. El service devuelve el equipo (`id: 9`) | El alta se abrió desde otro formulario | Redirige a `/lotes/nuevo?desdeAlta=1&idEquipamiento=9` con la alerta "Molino creado correctamente". |
| **CP-MOC-15** | Un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 4. `GET /equipamiento/molinos/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MOC-16** | El formulario de edición muestra los datos actuales y apunta a la modificación | Rol `GERENTE_DE_PRODUCCION`, `id: 4`. El service devuelve `MOL-01` con su descripción y datos técnicos | Molino existe | Responde 200 con la vista `equipamiento/molino-form`: título "Editar molino", los datos actuales (identificador, descripción y datos técnicos), `action="/equipamiento/molinos/4"` y el botón "Guardar cambios". |

---

### 5. `POST /equipamiento/molinos/{id}` — `modificarMolino(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MOC-17** | La modificación correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `id: 4`, datos válidos | Formulario válido | Llama a `modificarMolino(4, ...)`. Redirige a `/equipamiento/molinos` con la alerta "Molino modificado correctamente" de tipo `success`. |
| **CP-MOC-18** | Modificar con un identificador duplicado vuelve al formulario de edición con el mensaje del service | El service lanza `RecursoDuplicadoException` | El service rechaza el identificador | Vuelve al formulario de edición (`action="/equipamiento/molinos/4"`) con el panel "No se pudo modificar el molino" y el mensaje del service. |
| **CP-MOC-19** | Si el service rechaza la modificación porque el molino está en uso, el motivo se muestra en el formulario | El service lanza `ReglaNegocioException("No se puede modificar el molino porque se encuentra en uso.")` | Molino en uso o con una etapa pendiente | Vuelve al formulario de edición y muestra el motivo en el panel de errores, conservando lo tipeado. |
| **CP-MOC-20** | Un rol distinto de GERENTE_DE_PRODUCCION no puede modificar | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 6. `POST /equipamiento/molinos/{id}/baja` — `bajaMolino(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MOC-21** | La baja correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `id: 4` | Molino sin etapas pendientes y fuera de uso | Llama a `bajaMolino(4)`. Redirige a `/equipamiento/molinos` con la alerta "Molino dado de baja correctamente" de tipo `success`. |
| **CP-MOC-22** | Si el service rechaza la baja (etapa pendiente o en uso) vuelve al origen con la alerta de advertencia | El service lanza `ReglaNegocioException("No se puede dar de baja el molino porque tiene comprometida una etapa pendiente de un lote.")`, con cabecera `Referer: /equipamiento/molinos` | Tiene una etapa pendiente de un lote o está en uso | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-MOC-23** | Un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
