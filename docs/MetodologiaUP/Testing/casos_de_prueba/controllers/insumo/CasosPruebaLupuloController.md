## Casos de prueba de `LupuloController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `LupuloControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `ILupuloServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/insumo/`.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")`). Los lúpulos son una de las tres pestañas de "Gestión de Insumos" (maltas, lúpulos y levaduras), cada una con su propia URL.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `insumo/lupulo-form`, sin redirigir, para conservar lo tipeado.
- _"Alta contextual"_ es el alta abierta desde el selector de otro formulario: llegan los parámetros `retorno` (ruta interna del formulario de origen) y `campo` (campo que recibe el id creado). Al guardar redirige a `retorno` agregando `campo=<id>` y `desdeAlta=1`.
- La unidad de medida (gramos) la fija el sistema: el formulario no la pide, lo aclara una sola vez en la cabecera de la sección "Datos del lúpulo" (no junto a ningún campo) y las listas la muestran.
- Todos los campos de texto y numéricos tienen un placeholder con un ejemplo (ej. "Ej: Cascade", "Ej: 5,5").

---

### 1. `GET /insumos/lupulos` — `listarLupulos(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LUC-01** | Listado para el gerente de producción | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 lúpulo (`id: 6`, "Cascade", `PELLET`, AA 5.5, `GRAMO`) | El rol está autorizado | Responde 200 con la vista `insumo/lupulo-lista`. Muestra "Gestión de Insumos", nombre, formato, la columna "Ácidos alfa (%)" con "5,5" y la unidad "g"; las pestañas hacia `/insumos/maltas` y `/insumos/levaduras`; el link de alta `/insumos/lupulos/nuevo`, el de edición `/insumos/lupulos/6/editar` y el botón de baja con `data-baja-url="/insumos/lupulos/6/baja"`. El diálogo de confirmación de baja incluye el token CSRF. |
| **CP-LUC-02** | Paginación y orden alfabético por defecto | Rol `GERENTE_DE_PRODUCCION`, sin parámetros | Sin criterios. Lúpulo es un módulo maestro | Llama al service con `nombre: null`, `formato: null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-LUC-03** | Criterios de búsqueda hacia el service | `nombre: " cas "`, `formato: FLOR`, `pagina: 1`; y por separado `nombre: "   "`, `formato: ""` (opción "Todos") | Un texto en blanco o un select vacío equivale a no filtrar | Llama al service con `nombre: "cas"` y `formato: FLOR` en la página 1; con los valores vacíos lo llama con `nombre: null` y `formato: null`. |
| **CP-LUC-04** | Listado sin resultados | El service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron lúpulos con los criterios indicados" en lugar de una tabla vacía. |
| **CP-LUC-05** | Rol no autorizado | Rol `ENCARGADO_DE_DEPOSITO` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /insumos/lupulos/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LUC-06** | Formulario de alta | Rol `GERENTE_DE_PRODUCCION` | El rol está autorizado | Responde 200 con la vista `insumo/lupulo-form`. Muestra "Nuevo lúpulo", el asterisco rojo de obligatorio, `action="/insumos/lupulos"`, las opciones de formato (ej. `PELLET`), la aclaración de que la unidad la define el sistema (en la cabecera de la sección), los placeholders "Ej: Cascade" y "Ej: 5,5", la ayuda "Un número mayor a 0 y hasta 25", el botón "Crear lúpulo" y el link "Volver a lúpulos". No incluye el campo `retorno` ni el panel de errores. |
| **CP-LUC-07** | Alta contextual: se conservan retorno y campo | `retorno: "/recetas/nueva"`, `campo: "idLupulo"` | El alta se abre desde el selector de otro formulario | Incluye los campos ocultos `retorno` y `campo`. El link de volver dice "Volver al formulario anterior" y apunta a `/recetas/nueva?desdeAlta=1`. |
| **CP-LUC-08** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 3. `POST /insumos/lupulos` — `altaLupulo(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LUC-09** | Alta correcta | `nombre: "Cascade"`, `formato: PELLET`, `aa: 5.5`, con CSRF | Formulario válido | Llama a `altaLupulo` con los tres datos. Redirige a `/insumos/lupulos` con la alerta "Lúpulo creado correctamente" de tipo `success`. |
| **CP-LUC-10** | Faltan los datos obligatorios | `nombre: "  "`, sin formato ni alfa ácidos | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear el lúpulo" y los mensajes "El nombre es obligatorio", "El formato es obligatorio" y "Los ácidos alfa son obligatorios". No invoca al service. |
| **CP-LUC-11** | Nombre duplicado | El service lanza `RecursoDuplicadoException("Ya existe un lúpulo con el nombre 'Cascade'")` | El service rechaza el nombre | Vuelve al formulario con el mensaje del service y lo tipeado conservado (nombre y alfa ácidos). |
| **CP-LUC-12** | Ácidos alfa fuera de rango | `aa: 233333`. El service lanza `ReglaNegocioException("Los ácidos alfa deben ser mayores a 0% y menores o iguales a 25%")` | El service rechaza el valor | Vuelve al formulario, muestra el mensaje junto al campo de ácidos alfa y conserva el valor tipeado sin ceros sobrantes (`233333`). |
| **CP-LUC-13** | Alta contextual correcta | Datos válidos, `retorno: "/recetas/nueva"`, `campo: "idLupulo"`. El service devuelve el lúpulo (`id: 9`) | El alta se abrió desde otro formulario | Redirige a `/recetas/nueva?desdeAlta=1&idLupulo=9` con la alerta "Lúpulo creado correctamente". |
| **CP-LUC-14** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 4. `GET /insumos/lupulos/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LUC-15** | Formulario de edición con los datos actuales | `id: 6`. El service devuelve "Cascade", `FLOR`, AA 5.5 | El lúpulo existe | Responde 200 con la vista `insumo/lupulo-form`: título "Editar lúpulo", el nombre y los alfa ácidos actuales, la opción `FLOR` seleccionada, `action="/insumos/lupulos/6"` y el botón "Guardar cambios". |

---

### 5. `POST /insumos/lupulos/{id}` — `modificarLupulo(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LUC-16** | Modificación correcta | `id: 6`, `nombre: "Citra"`, `formato: FLOR`, `aa: 12` | Formulario válido | Llama a `modificarLupulo(6, ...)`. Redirige a `/insumos/lupulos` con la alerta "Lúpulo modificado correctamente" de tipo `success`. |
| **CP-LUC-17** | Nombre duplicado | El service lanza `RecursoDuplicadoException` | El service rechaza el nombre | Vuelve al formulario de edición (`action="/insumos/lupulos/6"`) con el panel "No se pudo modificar el lúpulo" y el mensaje del service. |
| **CP-LUC-18** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 6. `POST /insumos/lupulos/{id}/baja` — `bajaLupulo(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LUC-19** | Baja correcta | `id: 6` | El lúpulo no forma parte de una receta activa ni tiene stock | Llama a `bajaLupulo(6)`. Redirige a `/insumos/lupulos` con la alerta "Lúpulo dado de baja correctamente" de tipo `success`. |
| **CP-LUC-20** | Baja rechazada por el service | El service lanza `ReglaNegocioException("No se puede dar de baja el lúpulo porque tiene stock")`, con cabecera `Referer: /insumos/lupulos` | El lúpulo tiene stock o forma parte de una receta | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-LUC-21** | Rol no autorizado | Rol `ENCARGADO_DE_DEPOSITO` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
