## Casos de prueba de `MaceradorController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `MaceradorControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IMaceradorServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/equipamiento/CasosPruebaMacerador.md`.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")`). Maceradores es una de las cuatro pestañas de "Gestión de Equipamiento" (molinos, maceradores, ollas de hervor y fermentadores), cada una con su propia URL.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `equipamiento/macerador-form`, sin redirigir, para conservar lo tipeado.
- _"Alta contextual"_ es el alta abierta desde el selector de otro formulario: llegan los parámetros `retorno` (ruta interna del formulario de origen) y `campo` (campo que recibe el id creado). Al guardar redirige a `retorno` agregando `campo=<id>` y `desdeAlta=1`.
- El estado operativo no se carga en el formulario: lo define el sistema (un equipo nuevo nace disponible) y lo cambian las operaciones de producción y de ciclo de vida.
- Todos los campos tienen un placeholder con un ejemplo.

---

### 1. `GET /equipamiento/maceradores` — `listarMaceradores(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MCC-01** | El GERENTE_DE_PRODUCCION ve el listado con estado operativo, datos técnicos, las pestañas y las acciones por fila | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 macerador (`id: 4`, `MAC-01`, `EN_USO`, datos técnicos de ejemplo) | El rol está autorizado | Responde 200 con la vista `equipamiento/macerador-lista`. Muestra el identificador, el estado como "En uso", los datos técnicos formateados, las pestañas de los cuatro tipos, el link de alta `/equipamiento/maceradores/nuevo`, el de edición `/equipamiento/maceradores/4/editar` y el botón de baja con `data-baja-url="/equipamiento/maceradores/4/baja"`. El diálogo de confirmación de baja incluye el token CSRF. |
| **CP-MCC-02** | Sin filtros pide páginas de 20 ordenadas alfabéticamente por identificador, con los criterios en null | Rol `GERENTE_DE_PRODUCCION`, `GET /equipamiento/maceradores` sin parámetros | Sin criterios. El equipamiento se lista alfabéticamente | Llama al service con `identificador: null`, `estadoOperativo: null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `identificadorInterno` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-MCC-03** | Los criterios llegan al service; un identificador en blanco y el select "Todos" se convierten en null | `identificador: " mol "`, `estadoOperativo: EN_USO`, `pagina: 1`; y por separado `identificador: "   "`, `estadoOperativo: ""` (opción "Todos") | Un texto en blanco o un select vacío equivale a no filtrar | Llama al service con `identificador: "mol"` y `EN_USO` en la página 1; con los valores vacíos lo llama con ambos criterios en `null`. |
| **CP-MCC-04** | Sin resultados muestra un mensaje claro en vez de una tabla vacía | Rol `GERENTE_DE_PRODUCCION`, el service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron maceradores con los criterios indicados." en lugar de una tabla vacía. |
| **CP-MCC-05** | El OPERARIO_DE_PRODUCCION no accede a la gestión: el listado es rechazado y no llega al service | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /equipamiento/maceradores/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MCC-06** | El formulario de alta muestra los campos con asterisco, placeholders y la ayuda de cada regla | Rol `GERENTE_DE_PRODUCCION` | El rol está autorizado | Responde 200 con la vista `equipamiento/macerador-form`. Muestra "Nuevo macerador", el asterisco rojo de obligatorio, `action="/equipamiento/maceradores"`, el placeholder de cada campo, el botón "Crear macerador" y el link "Volver a maceradores". No incluye el campo `retorno` ni el panel de errores. |
| **CP-MCC-07** | Abierto desde otro formulario (retorno y campo) los conserva y cancelar vuelve a ese formulario; un retorno externo se descarta | `retorno: "/lotes/nuevo"`, `campo: "idEquipamiento"`; y por separado `retorno: "//sitio-malicioso.com"` | Solo se aceptan rutas internas | Con una ruta interna incluye los campos ocultos `retorno` y `campo`, y el link de volver dice "Volver al formulario anterior" y apunta a `/lotes/nuevo?desdeAlta=1`. Con una ruta externa la descarta: no hay campo `retorno` ni aparece el sitio externo. |
| **CP-MCC-08** | El OPERARIO_DE_PRODUCCION no puede abrir el formulario de alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 3. `POST /equipamiento/maceradores` — `altaMacerador(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MCC-09** | Alta correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, identificador `MAC-01` y datos técnicos válidos, con CSRF | Formulario válido | Llama a `altaMacerador` con los datos ingresados. Redirige a `/equipamiento/maceradores` con la alerta "Macerador creado correctamente" de tipo `success`. |
| **CP-MCC-10** | Sin identificador ni datos técnicos vuelve al formulario con todos los errores, sin llamar al service | `identificadorInterno: "  "`, sin datos técnicos | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear el macerador" y un mensaje por cada campo obligatorio. No invoca al service. |
| **CP-MCC-11** | Un valor no numérico en un campo numérico muestra un mensaje en español, sin llamar al service | `capacidadTotal: "abc"` | El valor no se puede convertir a número | Vuelve al formulario con "Ingresá un número válido" (mensaje en español, no el texto técnico de Spring). No invoca al service. |
| **CP-MCC-12** | Un identificador duplicado vuelve al formulario con el mensaje del service y conserva lo tipeado | El service lanza `RecursoDuplicadoException("Ya existe un equipamiento con el identificador interno 'MAC-01'")` | El service rechaza el identificador | Vuelve al formulario con el mensaje del service junto al campo y el identificador tipeado conservado. |
| **CP-MCC-13** | Una regla de negocio rechazada por el service se muestra como error general del formulario | El service lanza `ReglaNegocioException("Regla incumplida")` (ej. capacidad útil mayor a la total) | El service rechaza los datos | Vuelve al formulario y muestra el mensaje en el panel de errores, conservando lo tipeado. |
| **CP-MCC-14** | El alta contextual vuelve al formulario de origen con el equipamiento creado en el campo indicado | Datos válidos, `retorno: "/lotes/nuevo"`, `campo: "idEquipamiento"`. El service devuelve el equipo (`id: 9`) | El alta se abrió desde otro formulario | Redirige a `/lotes/nuevo?desdeAlta=1&idEquipamiento=9` con la alerta "Macerador creado correctamente". |
| **CP-MCC-15** | Un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 4. `GET /equipamiento/maceradores/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MCC-16** | El formulario de edición muestra los datos actuales y apunta a la modificación | Rol `GERENTE_DE_PRODUCCION`, `id: 4`. El service devuelve `MAC-01` con su descripción y datos técnicos | Macerador existe | Responde 200 con la vista `equipamiento/macerador-form`: título "Editar macerador", los datos actuales (identificador, descripción y datos técnicos), `action="/equipamiento/maceradores/4"` y el botón "Guardar cambios". |

---

### 5. `POST /equipamiento/maceradores/{id}` — `modificarMacerador(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MCC-17** | La modificación correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `id: 4`, datos válidos | Formulario válido | Llama a `modificarMacerador(4, ...)`. Redirige a `/equipamiento/maceradores` con la alerta "Macerador modificado correctamente" de tipo `success`. |
| **CP-MCC-18** | Modificar con un identificador duplicado vuelve al formulario de edición con el mensaje del service | El service lanza `RecursoDuplicadoException` | El service rechaza el identificador | Vuelve al formulario de edición (`action="/equipamiento/maceradores/4"`) con el panel "No se pudo modificar el macerador" y el mensaje del service. |
| **CP-MCC-19** | Si el service rechaza la modificación porque el macerador está en uso, el motivo se muestra en el formulario | El service lanza `ReglaNegocioException("No se puede modificar el macerador porque se encuentra en uso")` | Macerador en uso o con una etapa pendiente | Vuelve al formulario de edición y muestra el motivo en el panel de errores, conservando lo tipeado. |
| **CP-MCC-20** | Un rol distinto de GERENTE_DE_PRODUCCION no puede modificar | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 6. `POST /equipamiento/maceradores/{id}/baja` — `bajaMacerador(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MCC-21** | La baja correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `id: 4` | Macerador sin etapas pendientes y fuera de uso | Llama a `bajaMacerador(4)`. Redirige a `/equipamiento/maceradores` con la alerta "Macerador dado de baja correctamente" de tipo `success`. |
| **CP-MCC-22** | Si el service rechaza la baja (etapa pendiente o en uso) vuelve al origen con la alerta de advertencia | El service lanza `ReglaNegocioException("No se puede dar de baja el macerador porque tiene comprometida una etapa pendiente de un lote")`, con cabecera `Referer: /equipamiento/maceradores` | Tiene una etapa pendiente de un lote o está en uso | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-MCC-23** | Un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
