## Casos de prueba de `OllaHervorController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `OllaHervorControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IOllaHervorServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/equipamiento/CasosPruebaOllaHervor.md`.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")`). Ollas de hervor es una de las cuatro pestañas de "Gestión de Equipamiento" (molinos, maceradores, ollas de hervor y fermentadores), cada una con su propia URL.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `equipamiento/olla-form`, sin redirigir, para conservar lo tipeado.
- _"Alta contextual"_ es el alta abierta desde el selector de otro formulario: llegan los parámetros `retorno` (ruta interna del formulario de origen) y `campo` (campo que recibe el id creado). Al guardar redirige a `retorno` agregando `campo=<id>` y `desdeAlta=1`.
- El estado operativo no se carga en el formulario: lo define el sistema (un equipo nuevo nace disponible) y lo cambian las operaciones de producción y de ciclo de vida.
- Todos los campos tienen un placeholder con un ejemplo.

---

### 1. `GET /equipamiento/ollas-hervor` — `listarOllasHervor(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-OHC-01** | El GERENTE_DE_PRODUCCION ve el listado con estado operativo, datos técnicos, las pestañas y las acciones por fila | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 olla de hervor (`id: 4`, `OLLA-01`, `EN_USO`, datos técnicos de ejemplo) | El rol está autorizado | Responde 200 con la vista `equipamiento/olla-lista`. Muestra el identificador, el estado como "En uso", los datos técnicos formateados, las pestañas de los cuatro tipos, el link de alta `/equipamiento/ollas-hervor/nuevo`, el de edición `/equipamiento/ollas-hervor/4/editar` y el botón de baja con `data-baja-url="/equipamiento/ollas-hervor/4/baja"`. El diálogo de confirmación de baja incluye el token CSRF. |
| **CP-OHC-02** | Sin filtros pide páginas de 20 ordenadas alfabéticamente por identificador, con los criterios en null | Rol `GERENTE_DE_PRODUCCION`, `GET /equipamiento/ollas-hervor` sin parámetros | Sin criterios. El equipamiento se lista alfabéticamente | Llama al service con `identificador: null`, `estadoOperativo: null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `identificadorInterno` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-OHC-03** | Los criterios llegan al service; un identificador en blanco y el select "Todos" se convierten en null | `identificador: " mol "`, `estadoOperativo: EN_USO`, `pagina: 1`; y por separado `identificador: "   "`, `estadoOperativo: ""` (opción "Todos") | Un texto en blanco o un select vacío equivale a no filtrar | Llama al service con `identificador: "mol"` y `EN_USO` en la página 1; con los valores vacíos lo llama con ambos criterios en `null`. |
| **CP-OHC-04** | Sin resultados muestra un mensaje claro en vez de una tabla vacía | Rol `GERENTE_DE_PRODUCCION`, el service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron ollas de hervor con los criterios indicados" en lugar de una tabla vacía. |
| **CP-OHC-05** | El OPERARIO_DE_PRODUCCION no accede a la gestión: el listado es rechazado y no llega al service | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /equipamiento/ollas-hervor/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-OHC-06** | El formulario de alta muestra los campos con asterisco, placeholders y la ayuda de cada regla | Rol `GERENTE_DE_PRODUCCION` | El rol está autorizado | Responde 200 con la vista `equipamiento/olla-form`. Muestra "Nueva olla de hervor", el asterisco rojo de obligatorio, `action="/equipamiento/ollas-hervor"`, el placeholder de cada campo de datos técnicos, ningún campo ni texto del identificador interno (el sistema lo asigna al guardar), el botón "Crear olla de hervor" y el link "Volver a ollas de hervor". No incluye el campo `retorno` ni el panel de errores. |
| **CP-OHC-07** | Abierto desde otro formulario (retorno y campo) los conserva y cancelar vuelve a ese formulario; un retorno externo se descarta | `retorno: "/lotes/nuevo"`, `campo: "idEquipamiento"`; y por separado `retorno: "//sitio-malicioso.com"` | Solo se aceptan rutas internas | Con una ruta interna incluye los campos ocultos `retorno` y `campo`, y el link de volver dice "Volver al formulario anterior" y apunta a `/lotes/nuevo?desdeAlta=1`. Con una ruta externa la descarta: no hay campo `retorno` ni aparece el sitio externo. |
| **CP-OHC-08** | El OPERARIO_DE_PRODUCCION no puede abrir el formulario de alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 3. `POST /equipamiento/ollas-hervor` — `altaOllaHervor(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-OHC-09** | Alta correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, datos técnicos válidos (sin identificador: lo genera el sistema), con CSRF | Formulario válido | Llama a `altaOllaHervor` con los datos ingresados. Redirige a `/equipamiento/ollas-hervor` con la alerta "Olla de hervor creada correctamente" de tipo `success`. |
| **CP-OHC-10** | Sin datos técnicos vuelve al formulario con todos los errores, sin llamar al service | sin datos técnicos | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear la olla de hervor" y un mensaje por cada campo obligatorio. No invoca al service. |
| **CP-OHC-11** | Un valor no numérico en un campo numérico muestra un mensaje en español, sin llamar al service | `capacidadTotal: "abc"` | El valor no se puede convertir a número | Vuelve al formulario con "Ingrese un número válido" (mensaje en español, no el texto técnico de Spring). No invoca al service. |
| **CP-OHC-12** | Un identificador interno enviado en el pedido de alta se ignora: lo genera el sistema | Rol `GERENTE_DE_PRODUCCION`, datos técnicos válidos y el parámetro `identificadorInterno: "HACK-99"`, con CSRF | El `FormDTO` no tiene `identificadorInterno` | Llama a `altaOllaHervor` igual que sin el parámetro y redirige al listado con la alerta de éxito. El valor enviado no llega al service. |
| **CP-OHC-13** | Una regla de negocio rechazada por el service se muestra como error general del formulario | El service lanza `ReglaNegocioException("Regla incumplida")` (ej. capacidad útil mayor a la total) | El service rechaza los datos | Vuelve al formulario y muestra el mensaje en el panel de errores, conservando lo tipeado. |
| **CP-OHC-14** | El alta contextual vuelve al formulario de origen con el equipamiento creado en el campo indicado | Datos válidos, `retorno: "/lotes/nuevo"`, `campo: "idEquipamiento"`. El service devuelve el equipo (`id: 9`) | El alta se abrió desde otro formulario | Redirige a `/lotes/nuevo?desdeAlta=1&idEquipamiento=9` con la alerta "Olla de hervor creada correctamente". |
| **CP-OHC-15** | Un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 4. `GET /equipamiento/ollas-hervor/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-OHC-16** | El formulario de edición muestra los datos actuales y apunta a la modificación | Rol `GERENTE_DE_PRODUCCION`, `id: 4`. El service devuelve `OLLA-01` con su descripción y datos técnicos | Olla de hervor existe | Responde 200 con la vista `equipamiento/olla-form`: título "Editar olla de hervor", sin el identificador interno, y los datos técnicos actuales; los números sin ceros sobrantes: `100`, `80`, `8` y `3`), `action="/equipamiento/ollas-hervor/4"` y el botón "Guardar cambios". |

---

### 5. `POST /equipamiento/ollas-hervor/{id}` — `modificarOllaHervor(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-OHC-17** | La modificación correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `id: 4`, datos válidos | Formulario válido | Llama a `modificarOllaHervor(4, ...)`. Redirige a `/equipamiento/ollas-hervor` con la alerta "Olla de hervor modificada correctamente" de tipo `success`. |
| **CP-OHC-18** | Un identificador interno enviado en el pedido de modificación se ignora: no se puede cambiar | Rol `GERENTE_DE_PRODUCCION`, `id: 4`, datos válidos y el parámetro `identificadorInterno: "HACK-99"` | El `FormDTO` no tiene `identificadorInterno` | Llama a `modificarOllaHervor(4, ...)` igual que sin el parámetro y redirige al listado con la alerta de éxito. |
| **CP-OHC-19** | Si el service rechaza la modificación por una regla de negocio, el motivo se muestra en el formulario | El service lanza `ReglaNegocioException("Los datos del equipo no son válidos")` | Regla de negocio incumplida | Vuelve al formulario de edición y muestra el motivo en el panel de errores y conservando lo tipeado. |
| **CP-OHC-20** | Un rol distinto de GERENTE_DE_PRODUCCION no puede modificar | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 6. `POST /equipamiento/ollas-hervor/{id}/baja` — `bajaOllaHervor(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-OHC-21** | La baja correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `id: 4` | Olla de hervor sin etapas pendientes y fuera de uso | Llama a `bajaOllaHervor(4)`. Redirige a `/equipamiento/ollas-hervor` con la alerta "Olla de hervor dada de baja correctamente" de tipo `success`. |
| **CP-OHC-22** | Si el service rechaza la baja (etapa pendiente o en uso) vuelve al origen con la alerta de advertencia | El service lanza `ReglaNegocioException("No se puede dar de baja la olla de hervor porque tiene comprometida una etapa en estado Pendiente de un lote")`, con cabecera `Referer: /equipamiento/ollas-hervor` | Tiene una etapa en estado Pendiente de un lote o está en uso | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-OHC-23** | Un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
