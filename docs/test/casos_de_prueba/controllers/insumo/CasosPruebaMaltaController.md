## Casos de prueba de `MaltaController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `MaltaControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IMaltaServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/insumo/`.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")`). Las maltas son una de las tres pestañas de "Gestión de Insumos" (maltas, lúpulos y levaduras), cada una con su propia URL.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `insumo/malta-form`, sin redirigir, para conservar lo tipeado.
- _"Alta contextual"_ es el alta abierta desde el selector de otro formulario (por ejemplo, el de receta): llegan los parámetros `retorno` (ruta interna del formulario de origen) y `campo` (campo que recibe el id creado). Al guardar redirige a `retorno` agregando `campo=<id>` y `desdeAlta=1`.
- La unidad de medida (kilogramos) la fija el sistema: el formulario no la pide, lo aclara una sola vez en la cabecera de la sección "Datos de la malta" (no junto a ningún campo) y las listas la muestran.
- Todos los campos de texto y numéricos tienen un placeholder con un ejemplo (ej. "Ej: Pilsen", "Ej: 80").
- Las pestañas usan íconos propios del insumo (trigo para la malta, cono para el lúpulo, burbujas para la levadura), no emojis ni íconos genéricos.

---

### 1. `GET /insumos/maltas` — `listarMaltas(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MAC-01** | Listado para el gerente de producción | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 malta (`id: 4`, "Pilsen", `BASE`, potencial 80, `KILOGRAMO`) | El rol está autorizado | Responde 200 con la vista `insumo/malta-lista`. Muestra "Gestión de Insumos", nombre, tipo, "80 %" y la unidad "kg"; las pestañas hacia `/insumos/lupulos` y `/insumos/levaduras`; el link de alta `/insumos/maltas/nuevo`, el de edición `/insumos/maltas/4/editar` y el botón de baja con `data-baja-url="/insumos/maltas/4/baja"`. El diálogo de confirmación de baja incluye el token CSRF. Las pestañas llevan íconos SVG propios, el filtro de nombre tiene el placeholder "Ej: Pilsen" y la sidebar muestra la sección "Administración" para los maestros y la sección "Configuración" para los parámetros. |
| **CP-MAC-02** | Paginación y orden alfabético por defecto | Rol `GERENTE_DE_PRODUCCION`, sin parámetros | Sin criterios. Malta es un módulo maestro | Llama al service con `nombre: null`, `tipo: null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-MAC-03** | Criterios de búsqueda hacia el service | `nombre: " pil "`, `tipo: BASE`, `pagina: 1`; y por separado `nombre: "   "`, `tipo: ""` (opción "Todos") | Un texto en blanco o un select vacío equivale a no filtrar | Llama al service con `nombre: "pil"` y `tipo: BASE` en la página 1; con los valores vacíos lo llama con `nombre: null` y `tipo: null`. |
| **CP-MAC-04** | Listado sin resultados | El service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron maltas con los criterios indicados." en lugar de una tabla vacía. |
| **CP-MAC-05** | Rol no autorizado | Rol `ENCARGADO_DE_DEPOSITO` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /insumos/maltas/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MAC-06** | Formulario de alta | Rol `GERENTE_DE_PRODUCCION` | El rol está autorizado | Responde 200 con la vista `insumo/malta-form`. Muestra "Nueva malta", el asterisco rojo de obligatorio, `action="/insumos/maltas"`, las opciones de tipo (ej. `CARAMELO`), la aclaración de que la unidad la define el sistema (en la cabecera de la sección), los placeholders "Ej: Pilsen" y "Ej: 80", la ayuda "Un número entero mayor a 0 y hasta 90.", el botón "Crear malta" y el link "Volver a maltas". No incluye el campo `retorno` ni el panel de errores. |
| **CP-MAC-07** | Alta contextual: se conservan retorno y campo | `retorno: "/recetas/nueva"`, `campo: "idMalta"`; y por separado `retorno: "//sitio-malicioso.com"` | Solo se aceptan rutas internas | Con una ruta interna incluye los campos ocultos `retorno` y `campo`, y el link de volver dice "Volver al formulario anterior" y apunta a `/recetas/nueva?desdeAlta=1`. Con una ruta externa la descarta: no hay campo `retorno` ni aparece el sitio externo. |
| **CP-MAC-08** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 3. `POST /insumos/maltas` — `altaMalta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MAC-09** | Alta correcta | `nombre: "Pilsen"`, `tipo: BASE`, `potencialExtracto: 80`, con CSRF | Formulario válido | Llama a `altaMalta` con los tres datos. Redirige a `/insumos/maltas` con la alerta "Malta creada correctamente" de tipo `success`. |
| **CP-MAC-10** | Faltan los datos obligatorios | `nombre: "  "`, sin tipo ni potencial | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear la malta" y los mensajes "El nombre de la malta es obligatorio", "El tipo de malta es obligatorio" y "El potencial de extracto es obligatorio". No invoca al service. |
| **CP-MAC-11** | Valor no numérico en el potencial | `potencialExtracto: "abc"` | El valor no se puede convertir a número | Vuelve al formulario con "Ingresá un número entero válido" (mensaje en español, no el texto técnico de Spring). No invoca al service. |
| **CP-MAC-12** | Nombre duplicado | El service lanza `RecursoDuplicadoException("Ya existe una malta con el nombre 'Pilsen'")` | El service rechaza el nombre | Vuelve al formulario con el mensaje del service y lo tipeado conservado (nombre y potencial). |
| **CP-MAC-13** | Extracto potencial fuera de rango | `potencialExtracto: 95`. El service lanza `ReglaNegocioException("El extracto potencial debe ser mayor a 0% y menor o igual a 90%")` | El service rechaza el extracto potencial | Vuelve al formulario y muestra el mensaje junto al campo del potencial. |
| **CP-MAC-14** | Alta contextual correcta | Datos válidos, `retorno: "/recetas/nueva"`, `campo: "idMalta"`. El service devuelve la malta (`id: 9`) | El alta se abrió desde otro formulario | Redirige a `/recetas/nueva?desdeAlta=1&idMalta=9` con la alerta "Malta creada correctamente". |
| **CP-MAC-15** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 4. `GET /insumos/maltas/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MAC-16** | Formulario de edición con los datos actuales | `id: 4`. El service devuelve "Pilsen", `CARAMELO`, potencial 75 | La malta existe | Responde 200 con la vista `insumo/malta-form`: título "Editar malta", el nombre y el potencial actuales, la opción `CARAMELO` seleccionada, `action="/insumos/maltas/4"` y el botón "Guardar cambios". |

---

### 5. `POST /insumos/maltas/{id}` — `modificarMalta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MAC-17** | Modificación correcta | `id: 4`, `nombre: "Munich"`, `tipo: TOSTADA`, `potencialExtracto: 70` | Formulario válido | Llama a `modificarMalta(4, ...)`. Redirige a `/insumos/maltas` con la alerta "Malta modificada correctamente" de tipo `success`. |
| **CP-MAC-18** | Nombre duplicado | El service lanza `RecursoDuplicadoException` | El service rechaza el nombre | Vuelve al formulario de edición (`action="/insumos/maltas/4"`) con el panel "No se pudo modificar la malta" y el mensaje del service. |
| **CP-MAC-19** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 6. `POST /insumos/maltas/{id}/baja` — `bajaMalta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MAC-20** | Baja correcta | `id: 4` | La malta no forma parte de una receta activa ni tiene stock | Llama a `bajaMalta(4)`. Redirige a `/insumos/maltas` con la alerta "Malta dada de baja correctamente" de tipo `success`. |
| **CP-MAC-21** | Baja rechazada por el service | El service lanza `ReglaNegocioException("No se puede dar de baja la malta porque tiene stock")`, con cabecera `Referer: /insumos/maltas` | La malta tiene stock o forma parte de una receta | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-MAC-22** | Rol no autorizado | Rol `ENCARGADO_DE_DEPOSITO` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
