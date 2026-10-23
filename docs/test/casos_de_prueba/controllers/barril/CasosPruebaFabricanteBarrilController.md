## Casos de prueba de `FabricanteBarrilController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `FabricanteBarrilControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; `IFabricanteBarrilServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/barril/CasosPruebaFabricanteBarril.md`.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")` en cada método). "Fabricantes de barril" es la segunda pestaña de "Gestión de Barriles" (barriles y fabricantes de barril), con su propia URL. El buscador de fabricantes que usa el selector del formulario de barriles y la sidebar se cubren en `CasosPruebaBarrilController.md`.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `barril/fabricante-form`, sin redirigir, para conservar lo tipeado.
- El listado es de un módulo maestro: solo muestra fabricantes activos, en orden alfabético ascendente por `nombre` (sin distinguir mayúsculas), con el `id` como desempate.
- Todos los campos tienen un placeholder con un ejemplo.

---

### 1. `GET /barriles/fabricantes` — `listarFabricantes(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-FBC-01** | El GERENTE_DE_PRODUCCION ve el listado con las pestañas y las acciones por fila | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 fabricante (`id: 2`, "Barrilitos SA") | El rol está autorizado | Responde 200 con la vista `barril/fabricante-lista`. Muestra el título "Gestión de Barriles", el nombre, las pestañas "Barriles" (`/barriles`) y "Fabricantes de barril" (activa), el link de alta `/barriles/fabricantes/nuevo` con el texto "Nuevo fabricante", el de edición `/barriles/fabricantes/2/editar` y el botón de baja con `data-baja-url="/barriles/fabricantes/2/baja"`. El diálogo de confirmación de baja incluye el token CSRF y la advertencia "Si tiene barriles activos asociados, el sistema no lo permitirá." En la sidebar, "Gestión de Barriles" aparece resaltada como opción activa. |
| **CP-FBC-02** | Sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con el criterio en null | Rol `GERENTE_DE_PRODUCCION`, `GET /barriles/fabricantes` sin parámetros | Sin criterios. Fabricante de barril es un módulo maestro | Llama a `filtrarFabricantesBarril` con `nombre: null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-FBC-03** | El criterio de nombre llega recortado al service; un texto en blanco equivale a no filtrar | `nombre: " ton "`, `pagina: 1`; y por separado `nombre: "   "` | Un texto en blanco equivale a no filtrar | Llama al service con `nombre: "ton"` en la página 1; con el texto en blanco lo llama con `nombre: null`. |
| **CP-FBC-04** | El criterio queda en el formulario y en los links de paginación | `nombre: "ton"`. El service devuelve una página con anterior y siguiente | El criterio se conserva al paginar | El campo "Nombre" muestra "ton" (placeholder "Ej: Tonelería del Sur"). Los links de página anterior y siguiente incluyen `pagina` y `nombre`. El link "Limpiar" apunta a `/barriles/fabricantes` sin parámetros. |
| **CP-FBC-05** | Sin resultados muestra un mensaje claro en vez de una tabla vacía | Rol `GERENTE_DE_PRODUCCION`, el service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron fabricantes de barril con los criterios indicados" en lugar de una tabla vacía. |
| **CP-FBC-06** | El OPERARIO_DE_PRODUCCION no accede a la gestión: el listado es rechazado y no llega al service | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /barriles/fabricantes/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-FBC-07** | El formulario de alta muestra el campo con asterisco y placeholder | Rol `GERENTE_DE_PRODUCCION` | El rol está autorizado | Responde 200 con la vista `barril/fabricante-form`. Muestra "Nuevo fabricante de barril", el asterisco rojo de obligatorio, `action="/barriles/fabricantes"`, el formulario con `novalidate`, el campo "Nombre" con el placeholder "Ej: Tonelería del Sur", el botón "Crear fabricante" y el link "Volver a fabricantes de barril" hacia `/barriles/fabricantes`. No incluye el panel de errores. |
| **CP-FBC-08** | El OPERARIO_DE_PRODUCCION no puede abrir el formulario de alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 3. `POST /barriles/fabricantes` — `altaFabricanteBarril(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-FBC-09** | Alta correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `nombre: "Barrilitos SA"`, con CSRF | Formulario válido | Llama a `altaFabricanteBarril` con el nombre ingresado. Redirige a `/barriles/fabricantes` con la alerta "Fabricante de barril creado correctamente" de tipo `success`. |
| **CP-FBC-10** | Sin nombre vuelve al formulario con el error, sin llamar al service | `nombre: "  "` | `@NotBlank` $\rightarrow$ **Falla** | Vuelve al formulario con el panel "No se pudo crear el fabricante de barril" y el mensaje "El nombre es obligatorio" junto al campo. No invoca al service. |
| **CP-FBC-11** | Un nombre duplicado vuelve al formulario con el mensaje del service junto al campo nombre | El service lanza `RecursoDuplicadoException("Ya existe un fabricante de barril con el nombre 'Barrilitos SA'")` | El service rechaza el nombre | Vuelve al formulario con el mensaje del service junto al campo "Nombre", marcado en rojo, y el nombre tipeado conservado. |
| **CP-FBC-12** | Una regla de negocio rechazada por el service se muestra como error general del formulario | El service lanza `ReglaNegocioException("Regla incumplida")` (ej. nombre más largo que el máximo permitido) | El service rechaza los datos | Vuelve al formulario y muestra el mensaje en el panel "No se pudo crear el fabricante de barril", conservando lo tipeado. |
| **CP-FBC-13** | Un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 4. `GET /barriles/fabricantes/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-FBC-14** | El formulario de edición muestra el nombre actual y apunta a la modificación | Rol `GERENTE_DE_PRODUCCION`, `id: 2`. El service devuelve "Barrilitos SA" | El fabricante existe | Responde 200 con la vista `barril/fabricante-form`: título "Editar fabricante de barril", el nombre actual, `action="/barriles/fabricantes/2"`, el botón "Guardar cambios" y el link "Volver a fabricantes de barril". |
| **CP-FBC-15** | Un fabricante inexistente redirige al origen con el motivo | `id: 99`. El service lanza `RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: 99")`, con `Referer: /barriles/fabricantes` | El `ControllerAdvices` maneja la excepción | Redirige al origen (`/barriles/fabricantes`) con la alerta "No se encontró el fabricante de barril con ID: 99" de tipo `danger`. No renderiza el formulario. |
| **CP-FBC-16** | Un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de edición | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 5. `POST /barriles/fabricantes/{id}` — `modificarFabricanteBarril(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-FBC-17** | La modificación correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `id: 2`, `nombre: "Tonelería del Sur"` | Formulario válido | Llama a `modificarFabricanteBarril(2, ...)`. Redirige a `/barriles/fabricantes` con la alerta "Fabricante de barril modificado correctamente" de tipo `success`. |
| **CP-FBC-18** | Modificar sin nombre vuelve al formulario de edición con el error, sin llamar al service | `id: 2`, `nombre: "  "` | `@NotBlank` $\rightarrow$ **Falla** | Vuelve al formulario de edición (`action="/barriles/fabricantes/2"`) con el panel "No se pudo modificar el fabricante de barril" y el mensaje "El nombre es obligatorio" junto al campo. No invoca al service. |
| **CP-FBC-19** | Modificar con un nombre duplicado vuelve al formulario de edición con el mensaje del service | El service lanza `RecursoDuplicadoException("El nombre 'Barrilitos SA' ya está en uso por otro fabricante de barril")` | El service rechaza el nombre | Vuelve al formulario de edición con el panel "No se pudo modificar el fabricante de barril" y el mensaje del service junto al campo "Nombre". |
| **CP-FBC-20** | Si el service rechaza la modificación por una regla de negocio, el motivo se muestra en el formulario | El service lanza `ReglaNegocioException("Regla incumplida")` | Regla de negocio incumplida | Vuelve al formulario de edición y muestra el motivo en el panel de errores, conservando lo tipeado. |
| **CP-FBC-21** | Un rol distinto de GERENTE_DE_PRODUCCION no puede modificar | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 6. `POST /barriles/fabricantes/{id}/baja` — `bajaFabricanteBarril(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-FBC-22** | La baja correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `id: 2` | Fabricante sin barriles activos asociados | Llama a `bajaFabricanteBarril(2)`. Redirige a `/barriles/fabricantes` con la alerta "Fabricante de barril dado de baja correctamente" de tipo `success`. |
| **CP-FBC-23** | Si el service rechaza la baja (barriles activos asociados) vuelve al origen con la alerta de advertencia | El service lanza `ReglaNegocioException("No se puede dar de baja el fabricante de barril porque tiene barriles activos asociados")`, con cabecera `Referer: /barriles/fabricantes` | El fabricante tiene barriles activos | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-FBC-24** | Un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
