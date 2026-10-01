## Casos de prueba de `LevaduraController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `LevaduraControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `ILevaduraServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/insumo/`.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")`). Las levaduras son una de las tres pestañas de "Gestión de Insumos" (maltas, lúpulos y levaduras), cada una con su propia URL.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `insumo/levadura-form`, sin redirigir, para conservar lo tipeado.
- _"Alta contextual"_ es el alta abierta desde el selector de otro formulario: llegan los parámetros `retorno` (ruta interna del formulario de origen) y `campo` (campo que recibe el id creado). Al guardar redirige a `retorno` agregando `campo=<id>` y `desdeAlta=1`.
- La unidad de medida (gramos) la fija el sistema: el formulario no la pide, lo aclara una sola vez en la cabecera de la sección "Datos de la levadura" (no junto a ningún campo) y las listas la muestran.
- Las células viables por gramo se ingresan en **millones** (20000 equivale a 20.000.000.000) y el formulario lo aclara debajo del campo. El controller las convierte: el service recibe y valida células (entre 6.000.000.000 y 20.000.000.000), que es como las guarda la entidad y las usa el escalado de levadura. Al editar, el campo muestra el valor guardado en millones.
- Todos los campos de texto y numéricos tienen un placeholder con un ejemplo (ej. "Ej: Safale US-05", "Ej: 20000").

---

### 1. `GET /insumos/levaduras` — `listarLevaduras(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LEC-01** | Listado para el gerente de producción | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 levadura (`id: 3`, "US-05", `ALE`, 6.000.000.000 células por gramo, `GRAMO`) | El rol está autorizado | Responde 200 con la vista `insumo/levadura-lista`. Muestra "Gestión de Insumos", nombre, tipo, "6.000.000.000" y la unidad "g"; las pestañas hacia `/insumos/maltas` y `/insumos/lupulos`; el link de alta `/insumos/levaduras/nuevo`, el de edición `/insumos/levaduras/3/editar` y el botón de baja con `data-baja-url="/insumos/levaduras/3/baja"`. El diálogo de confirmación de baja incluye el token CSRF. |
| **CP-LEC-02** | Paginación y orden alfabético por defecto | Rol `GERENTE_DE_PRODUCCION`, sin parámetros | Sin criterios. Levadura es un módulo maestro | Llama al service con `nombre: null`, `tipo: null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-LEC-03** | Criterios de búsqueda hacia el service | `nombre: " us "`, `tipo: LAGER`, `pagina: 1`; y por separado `nombre: "   "`, `tipo: ""` (opción "Todos") | Un texto en blanco o un select vacío equivale a no filtrar | Llama al service con `nombre: "us"` y `tipo: LAGER` en la página 1; con los valores vacíos lo llama con `nombre: null` y `tipo: null`. |
| **CP-LEC-04** | Listado sin resultados | El service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron levaduras con los criterios indicados." en lugar de una tabla vacía. |
| **CP-LEC-05** | Rol no autorizado | Rol `ENCARGADO_DE_DEPOSITO` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /insumos/levaduras/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LEC-06** | Formulario de alta | Rol `GERENTE_DE_PRODUCCION` | El rol está autorizado | Responde 200 con la vista `insumo/levadura-form`. Muestra "Nueva levadura", el asterisco rojo de obligatorio, `action="/insumos/levaduras"`, las opciones de tipo (ej. `HIBRIDA`), la aclaración de que la unidad la define el sistema (en la cabecera de la sección), el campo "Células viables por gramo (en millones)" con el placeholder "Ej: 20000" y la ayuda "20000 equivale a 20.000.000.000 células por gramo", el placeholder "Ej: Safale US-05" en el nombre, el botón "Crear levadura" y el link "Volver a levaduras". No incluye el campo `retorno` ni el panel de errores. |
| **CP-LEC-07** | Alta contextual: se conservan retorno y campo | `retorno: "/recetas/nueva"`, `campo: "idLevadura"` | El alta se abre desde el selector de otro formulario | Incluye los campos ocultos `retorno` y `campo`. El link de volver dice "Volver al formulario anterior" y apunta a `/recetas/nueva?desdeAlta=1`. |
| **CP-LEC-08** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 3. `POST /insumos/levaduras` — `altaLevadura(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LEC-09** | Alta correcta | `nombre: "US-05"`, `tipo: ALE`, `celulasEnMillones: 20000`, con CSRF | Formulario válido | Llama a `altaLevadura` con los tres datos; las 20000 millones llegan al service como 20.000.000.000 células. Redirige a `/insumos/levaduras` con la alerta "Levadura creada correctamente" de tipo `success`. |
| **CP-LEC-10** | Faltan los datos obligatorios | `nombre: "  "`, sin tipo ni células por gramo | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear la levadura" y los mensajes "El nombre de la levadura es obligatorio", "El tipo de levadura es obligatorio" y "La cantidad de células viables por gramo es obligatoria". No invoca al service. |
| **CP-LEC-11** | Nombre duplicado | El service lanza `RecursoDuplicadoException("Ya existe una levadura con el nombre 'US-05'")` | El service rechaza el nombre | Vuelve al formulario con el mensaje del service y el nombre tipeado conservado. |
| **CP-LEC-12** | Células por gramo fuera de rango | `celulasEnMillones: 5000`. El service lanza `ReglaNegocioException("La cantidad de células viables por gramo debe ser mayor o igual a 6.000.000.000 y menor o igual a 20.000.000.000")` | El service rechaza el valor | Vuelve al formulario, muestra el mensaje junto al campo de células y conserva el valor tipeado (`5000`). |
| **CP-LEC-13** | Alta contextual correcta | Datos válidos, `retorno: "/recetas/nueva"`, `campo: "idLevadura"`. El service devuelve la levadura (`id: 9`) | El alta se abrió desde otro formulario | Redirige a `/recetas/nueva?desdeAlta=1&idLevadura=9` con la alerta "Levadura creada correctamente". |
| **CP-LEC-14** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-LEC-22** | Valor no numérico en las células | `celulasEnMillones: "abc"` | El valor no se puede convertir a número | Vuelve al formulario con "Ingresá un número entero válido" (mensaje en español). No invoca al service. |

---

### 4. `GET /insumos/levaduras/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LEC-15** | Formulario de edición con los datos actuales | `id: 3`. El service devuelve "US-05", `LAGER`, 6.000.000.000 células por gramo | La levadura existe | Responde 200 con la vista `insumo/levadura-form`: título "Editar levadura", el nombre actual, las células en millones (`6000`), la opción `LAGER` seleccionada, `action="/insumos/levaduras/3"` y el botón "Guardar cambios". |

---

### 5. `POST /insumos/levaduras/{id}` — `modificarLevadura(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LEC-16** | Modificación correcta | `id: 3`, `nombre: "S-23"`, `tipo: LAGER`, `cantidadCelulasPorGramo: 5000000000` | Formulario válido | Llama a `modificarLevadura(3, ...)`. Redirige a `/insumos/levaduras` con la alerta "Levadura modificada correctamente" de tipo `success`. |
| **CP-LEC-17** | Nombre duplicado | El service lanza `RecursoDuplicadoException` | El service rechaza el nombre | Vuelve al formulario de edición (`action="/insumos/levaduras/3"`) con el panel "No se pudo modificar la levadura" y el mensaje del service. |
| **CP-LEC-18** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 6. `POST /insumos/levaduras/{id}/baja` — `bajaLevadura(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LEC-19** | Baja correcta | `id: 3` | La levadura no forma parte de una receta activa ni tiene stock | Llama a `bajaLevadura(3)`. Redirige a `/insumos/levaduras` con la alerta "Levadura dada de baja correctamente" de tipo `success`. |
| **CP-LEC-20** | Baja rechazada por el service | El service lanza `ReglaNegocioException("No se puede dar de baja la levadura porque tiene stock")`, con cabecera `Referer: /insumos/levaduras` | La levadura tiene stock o forma parte de una receta | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-LEC-21** | Rol no autorizado | Rol `ENCARGADO_DE_DEPOSITO` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
