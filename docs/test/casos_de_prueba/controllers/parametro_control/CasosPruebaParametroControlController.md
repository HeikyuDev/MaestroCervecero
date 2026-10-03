## Casos de prueba de `ParametroControlController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `ParametroControlControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IParametroControlServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/parametro_control/`.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")`). Los parámetros de control son una de las tres pestañas de "Gestión de Recetas" (recetas, etapas de control y parámetros de control), cada una con su propia URL.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `receta/parametro-control-form`, sin redirigir, para conservar lo tipeado.
- _"Alta contextual"_ es el alta abierta desde el selector de otro formulario (por ejemplo, el plan de monitoreo de una receta): llegan los parámetros `retorno` (ruta interna del formulario de origen) y `campo` (campo que recibe el id creado). Al guardar redirige a `retorno` agregando `campo=<id>` y `desdeAlta=1`.
- La unidad de medida (`unidadDeMedida`, ej. "°C", "pH", "°P", "g/L") es un campo obligatorio de hasta 20 caracteres. Los valores mínimo y máximo se expresan en esa unidad, y el formulario lo aclara una sola vez, en la cabecera de la sección "Datos del parámetro de control". El listado la muestra en la columna "Unidad".
- La descripción no se muestra en las tablas (un texto muy largo las desarmaría): la fila que la tiene muestra el botón "Ver descripción" (ícono `bi-info-circle`, con `title` y `aria-label`), que abre un diálogo con el nombre como título y el texto completo. El nombre y el texto viajan en los atributos `data-descripcion-titulo` y `data-descripcion-texto`, escapados por Thymeleaf. El diálogo se cierra con el botón "Cerrar", con Esc o con un clic en el fondo. La fila sin descripción no muestra el botón.
- Los valores mínimo y máximo se muestran sin ceros sobrantes y con coma decimal (62.0 se muestra "62" y 68.5 se muestra "68,5").
- Todos los campos de texto y numéricos tienen un placeholder con un ejemplo (ej. "Ej: Temperatura", "Ej: °C", "Ej: 62").
- Las pestañas usan íconos de Bootstrap Icons (`bi-journal-bookmark`, `bi-list-check` y `bi-speedometer2`).

---

### 1. `GET /recetas/parametros-control` — `listarParametrosControl(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-01** | Listado para el gerente de producción | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 parámetro (`id: 4`, "Temperatura de maceración", unidad "°C", descripción "Temperatura del macerado", mínimo 62.0, máximo 68.5) | El rol está autorizado | Responde 200 con la vista `receta/parametro-control-lista`. Muestra "Gestión de Recetas", el nombre, la columna "Unidad" con "°C", el mínimo como "62" y el máximo como "68,5" (nunca "62.0"); no hay columna de descripción ni su texto dentro de una celda de la tabla; el placeholder "Ej: Temperatura" en el filtro; el link de alta `/recetas/parametros-control/nuevo`, el de edición `/recetas/parametros-control/4/editar` y el botón de baja con `data-baja-url="/recetas/parametros-control/4/baja"`. La tabla está envuelta en un contenedor `overflow-x-auto` y el diálogo de confirmación de baja incluye el token CSRF. |
| **CP-PCC-02** | Pestañas del módulo | Rol `GERENTE_DE_PRODUCCION`, listado vacío | El módulo tiene tres pestañas | Muestra los links a `/recetas`, `/recetas/etapas-control` y `/recetas/parametros-control` con los textos "Etapas de control" y "Parámetros de control" y los íconos `bi-journal-bookmark`, `bi-list-check` y `bi-speedometer2`. La pestaña de parámetros de control aparece marcada como activa. |
| **CP-PCC-03** | Paginación y orden alfabético por defecto | Rol `GERENTE_DE_PRODUCCION`, sin parámetros | Sin criterios. Parámetro de control es un módulo maestro | Llama al service con `nombre: null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-PCC-04** | Criterio de búsqueda hacia el service | `nombre: " temp "`, `pagina: 1`; y por separado `nombre: "   "` | Un texto en blanco equivale a no filtrar | Llama al service con `nombre: "temp"` en la página 1; con el valor en blanco lo llama con `nombre: null`. |
| **CP-PCC-05** | La paginación conserva el criterio | `nombre: "temp"`, `pagina: 1`. El service devuelve la página 2 de 3 (45 parámetros) | Hay páginas anterior y siguiente | Muestra "Página 2 de 3" y los links de anterior (`pagina=0`) y siguiente (`pagina=2`) con `nombre=temp`. |
| **CP-PCC-06** | Listado sin resultados | El service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron parámetros de control con los criterios indicados." en lugar de una tabla vacía. |
| **CP-PCC-07** | Rol no autorizado | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-PCC-33** | Botón "Ver descripción" en la fila con descripción | El service devuelve 1 parámetro (`id: 4`, "Temperatura", descripción `Línea 1 <b>negrita</b>`) | La fila tiene descripción | Muestra el botón con `title="Ver descripción"`, `aria-label="Ver descripción"` y el ícono `bi-info-circle`, con `data-descripcion-titulo="Temperatura"` y `data-descripcion-texto="Línea 1 &lt;b&gt;negrita&lt;/b&gt;"` (HTML escapado, nunca `<b>negrita</b>` sin escapar). Incluye una sola vez el diálogo `dialogo-descripcion` con el texto en `whitespace-pre-wrap break-words` y el botón `data-cerrar-descripcion`. |
| **CP-PCC-34** | Sin botón en la fila sin descripción | El service devuelve 2 parámetros: uno con descripción `null` y otro con descripción `"   "` | La fila no tiene descripción | Muestra los links de edición de ambas filas y no muestra ningún botón "Ver descripción" (ni atributo `data-descripcion-texto`). |

---

### 2. `GET /recetas/parametros-control/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-08** | Formulario de alta | Rol `GERENTE_DE_PRODUCCION` | El rol está autorizado | Responde 200 con la vista `receta/parametro-control-form`. Muestra "Nuevo parámetro de control", el asterisco rojo de obligatorio, `action="/recetas/parametros-control"`, el campo "Unidad de medida" (`name="unidadDeMedida"`), los placeholders "Ej: Temperatura", "Ej: °C", "Ej: 62", "Ej: 68" y "Ej: Temperatura del mosto en el punto de control", la aclaración "Los valores mínimo y máximo se expresan en la unidad de medida indicada." en la cabecera de la sección, el botón "Crear parámetro de control" (sin salto de línea) y el link "Volver a parámetros de control". No incluye el campo `retorno` ni el panel de errores. |
| **CP-PCC-09** | Alta contextual: se conservan retorno y campo | `retorno: "/recetas/nueva"`, `campo: "idParametroControl"`; y por separado `retorno: "//sitio-malicioso.com"` | Solo se aceptan rutas internas | Con una ruta interna incluye los campos ocultos `retorno` y `campo`, y el link de volver dice "Volver al formulario anterior" y apunta a `/recetas/nueva?desdeAlta=1`. Con una ruta externa la descarta: no hay campo `retorno` ni aparece el sitio externo. |
| **CP-PCC-10** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 3. `POST /recetas/parametros-control` — `altaParametroControl(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-11** | Alta correcta | `nombre: "Temperatura (°C)"`, `descripcion: "Temperatura del macerado"`, `unidadDeMedida: "°C"`, `valorMinimo: 62`, `valorMaximo: 68.5`, con CSRF | Formulario válido | Llama a `altaParametroControl` con los cinco datos. Redirige a `/recetas/parametros-control` con la alerta "Parámetro de control creado correctamente" de tipo `success`. |
| **CP-PCC-12** | Alta sin descripción | `nombre: "Temperatura (°C)"`, `valorMinimo: 62`, `valorMaximo: 68`, sin descripción | La descripción es opcional | Llama a `altaParametroControl` y redirige a `/recetas/parametros-control`. |
| **CP-PCC-13** | Faltan los datos obligatorios | `nombre: "  "`, sin unidad de medida, sin valor mínimo ni máximo | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear el parámetro de control" y los mensajes "El nombre del parámetro de control es obligatorio", "La unidad de medida es obligatoria", "El valor mínimo es obligatorio" y "El valor máximo es obligatorio". No invoca al service. |
| **CP-PCC-14** | Valor no numérico | `valorMinimo: "abc"` | El valor no se puede convertir a número | Vuelve al formulario con "Ingresá un número válido" (mensaje en español, no el texto técnico de Spring). No invoca al service. |
| **CP-PCC-15** | Descripción demasiado larga | `descripcion` de 256 caracteres | `@Size(max = 255)` $\rightarrow$ **Falla** | Vuelve al formulario con "La descripción no puede superar los 255 caracteres". No invoca al service. |
| **CP-PCC-16** | Nombre duplicado | El service lanza `RecursoDuplicadoException("Ya existe un parámetro de control con el nombre 'Temperatura (°C)'")` | El service rechaza el nombre | Vuelve al formulario con el mensaje del service y lo tipeado conservado (nombre y valores). |
| **CP-PCC-17** | Rango inválido | `valorMinimo: 70`, `valorMaximo: 60`. El service lanza `ReglaNegocioException("El valor mínimo no puede ser mayor al valor máximo")` | El service rechaza el rango | Vuelve al formulario con el panel "No se pudo crear el parámetro de control" y el mensaje del service; el valor tipeado se conserva. |
| **CP-PCC-18** | Alta contextual correcta | Datos válidos, `retorno: "/recetas/nueva"`, `campo: "idParametroControl"`. El service devuelve el parámetro (`id: 9`) | El alta se abrió desde otro formulario | Redirige a `/recetas/nueva?desdeAlta=1&idParametroControl=9` con la alerta "Parámetro de control creado correctamente". |
| **CP-PCC-19** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-PCC-30** | Unidad de medida en blanco | `nombre: "Temperatura"`, `unidadDeMedida: "   "`, `valorMinimo: 62`, `valorMaximo: 68` | `@NotBlank` $\rightarrow$ **Falla** | Vuelve al formulario con "La unidad de medida es obligatoria" y el nombre tipeado conservado. No invoca al service. |
| **CP-PCC-31** | Unidad de medida demasiado larga | `unidadDeMedida` de 21 caracteres | `@Size(max = 20)` $\rightarrow$ **Falla** | Vuelve al formulario con "La unidad de medida no puede superar los 20 caracteres". No invoca al service. |

---

### 4. `GET /recetas/parametros-control/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-20** | Formulario de edición con los datos actuales | `id: 4`. El service devuelve "Temperatura", unidad "°C", descripción "Temperatura del macerado", mínimo 62.0, máximo 68.5 | El parámetro de control existe | Responde 200 con la vista `receta/parametro-control-form`: título "Editar parámetro de control", el nombre, la unidad de medida, la descripción y los dos valores actuales, `action="/recetas/parametros-control/4"` y el botón "Guardar cambios". |
| **CP-PCC-21** | Rol no autorizado | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 5. `POST /recetas/parametros-control/{id}` — `modificarParametroControl(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-22** | Modificación correcta | `id: 4`, `nombre: "pH"`, `unidadDeMedida: "°C"`, `valorMinimo: 5.2`, `valorMaximo: 5.6` | Formulario válido | Llama a `modificarParametroControl(4, ...)` con los dos valores. Redirige a `/recetas/parametros-control` con la alerta "Parámetro de control modificado correctamente" de tipo `success`. |
| **CP-PCC-23** | Datos inválidos | `id: 4`, `nombre: ""`, sin valores | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario de edición (`action="/recetas/parametros-control/4"`) con el panel "No se pudo modificar el parámetro de control" y los mensajes de los campos. No invoca al service. |
| **CP-PCC-24** | Nombre duplicado | El service lanza `RecursoDuplicadoException` | El service rechaza el nombre | Vuelve al formulario de edición con el mensaje del service. |
| **CP-PCC-25** | Mínimo no positivo | `valorMinimo: 0`. El service lanza `ReglaNegocioException("El valor mínimo debe ser mayor a 0")` | El service rechaza el rango | Vuelve al formulario de edición y muestra el mensaje del service. |
| **CP-PCC-26** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-PCC-32** | Unidad de medida en blanco | `id: 4`, `nombre: "pH"`, `unidadDeMedida: ""`, `valorMinimo: 5.2`, `valorMaximo: 5.6` | `@NotBlank` $\rightarrow$ **Falla** | Vuelve al formulario de edición (`action="/recetas/parametros-control/4"`) con el panel "No se pudo modificar el parámetro de control" y el mensaje "La unidad de medida es obligatoria". No invoca al service. |

---

### 6. `POST /recetas/parametros-control/{id}/baja` — `bajaParametroControl(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-27** | Baja correcta | `id: 4` | El parámetro de control no está asociado al plan de monitoreo de una receta activa | Llama a `bajaParametroControl(4)`. Redirige a `/recetas/parametros-control` con la alerta "Parámetro de control dado de baja correctamente" de tipo `success`. |
| **CP-PCC-28** | Baja rechazada por el service | El service lanza `ReglaNegocioException("No se puede dar de baja el parámetro de control porque está asociado al plan de monitoreo de una receta activa")`, con cabecera `Referer: /recetas/parametros-control` | El parámetro de control está asociado al plan de monitoreo de una receta activa | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-PCC-29** | Rol no autorizado | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
