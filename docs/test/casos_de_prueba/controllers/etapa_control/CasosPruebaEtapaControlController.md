## Casos de prueba de `EtapaControlController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `EtapaControlControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IEtapaControlServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/etapa_control/`.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")`). Las etapas de control son una de las tres pestañas de "Gestión de Recetas" (recetas, etapas de control y parámetros de control), cada una con su propia URL.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `receta/etapa-control-form`, sin redirigir, para conservar lo tipeado.
- _"Alta contextual"_ es el alta abierta desde el selector de otro formulario (por ejemplo, el plan de monitoreo de una receta): llegan los parámetros `retorno` (ruta interna del formulario de origen) y `campo` (campo que recibe el id creado). Al guardar redirige a `retorno` agregando `campo=<id>` y `desdeAlta=1`.
- Las etapas que se pueden controlar son cuatro: `MACERACION`, `HERVIDO`, `FERMENTACION` y `MADURACION`. La pantalla las muestra con su nombre legible (Maceración, Hervido, Fermentación, Maduración).
- Todos los campos de texto tienen un placeholder con un ejemplo (ej. "Ej: Control de densidad").
- Las pestañas usan íconos de Bootstrap Icons (`bi-journal-bookmark`, `bi-list-check` y `bi-speedometer2`).

---

### 1. `GET /recetas/etapas-control` — `listarEtapasControl(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-ECC-01** | Listado para el gerente de producción | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 etapa de control (`id: 4`, "Control de densidad", descripción "Densidad del mosto", `FERMENTACION`) | El rol está autorizado | Responde 200 con la vista `receta/etapa-control-lista`. Muestra "Gestión de Recetas", el nombre, la descripción y la etapa como "Fermentación" (no el valor del enum); el placeholder "Ej: Control de densidad" en el filtro; el link de alta `/recetas/etapas-control/nuevo`, el de edición `/recetas/etapas-control/4/editar` y el botón de baja con `data-baja-url="/recetas/etapas-control/4/baja"`. La tabla está envuelta en un contenedor `overflow-x-auto` y el diálogo de confirmación de baja incluye el token CSRF. |
| **CP-ECC-02** | Pestañas del módulo | Rol `GERENTE_DE_PRODUCCION`, listado vacío | El módulo tiene tres pestañas | Muestra los links a `/recetas`, `/recetas/etapas-control` y `/recetas/parametros-control` con los textos "Etapas de control" y "Parámetros de control" y los íconos `bi-journal-bookmark`, `bi-list-check` y `bi-speedometer2`. La pestaña de etapas de control aparece marcada como activa. |
| **CP-ECC-03** | Paginación y orden alfabético por defecto | Rol `GERENTE_DE_PRODUCCION`, sin parámetros | Sin criterios. Etapa de control es un módulo maestro | Llama al service con `nombre: null`, `etapa: null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-ECC-04** | Criterios de búsqueda hacia el service | `nombre: " dens "`, `etapa: HERVIDO`, `pagina: 1`; y por separado `nombre: "   "`, `etapa: ""` (opción "Todas") | Un texto en blanco o un select vacío equivale a no filtrar | Llama al service con `nombre: "dens"` y `etapa: HERVIDO` en la página 1; con los valores vacíos lo llama con `nombre: null` y `etapa: null`. |
| **CP-ECC-05** | El filtro de etapa solo ofrece las etapas controlables | Rol `GERENTE_DE_PRODUCCION`, `etapa: HERVIDO` | Solo se pueden controlar cuatro etapas | El select ofrece `MACERACION`, `HERVIDO` (seleccionada), `FERMENTACION` y `MADURACION`, con los textos "Maceración", "Hervido" y "Maduración"; no ofrece `MOLIENDA` ni `ENVASADO`. |
| **CP-ECC-06** | La paginación conserva los criterios | `nombre: "dens"`, `etapa: HERVIDO`, `pagina: 1`. El service devuelve la página 2 de 3 (45 etapas de control) | Hay páginas anterior y siguiente | Muestra "Página 2 de 3" y los links de anterior (`pagina=0`) y siguiente (`pagina=2`) con `nombre=dens` y `etapa=HERVIDO`. |
| **CP-ECC-07** | Listado sin resultados | El service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron etapas de control con los criterios indicados." en lugar de una tabla vacía. |
| **CP-ECC-08** | Rol no autorizado | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /recetas/etapas-control/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-ECC-09** | Formulario de alta | Rol `GERENTE_DE_PRODUCCION` | El rol está autorizado | Responde 200 con la vista `receta/etapa-control-form`. Muestra "Nueva etapa de control", el asterisco rojo de obligatorio, `action="/recetas/etapas-control"`, las cuatro etapas controlables (`MACERACION`, `HERVIDO`, `FERMENTACION`, `MADURACION`; no `MOLIENDA` ni `ENVASADO`) con su nombre legible, los placeholders "Ej: Control de densidad" y "Ej: Medición de la densidad del mosto al finalizar la maceración", el botón "Crear etapa de control" (sin salto de línea) y el link "Volver a etapas de control". No incluye el campo `retorno` ni el panel de errores. |
| **CP-ECC-10** | Alta contextual: se conservan retorno y campo | `retorno: "/recetas/nueva"`, `campo: "idEtapaControl"`; y por separado `retorno: "//sitio-malicioso.com"` | Solo se aceptan rutas internas | Con una ruta interna incluye los campos ocultos `retorno` y `campo`, y el link de volver dice "Volver al formulario anterior" y apunta a `/recetas/nueva?desdeAlta=1`. Con una ruta externa la descarta: no hay campo `retorno` ni aparece el sitio externo. |
| **CP-ECC-11** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 3. `POST /recetas/etapas-control` — `altaEtapaControl(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-ECC-12** | Alta correcta | `nombre: "Control de densidad"`, `descripcion: "Densidad del mosto"`, `etapaAControlar: MACERACION`, con CSRF | Formulario válido | Llama a `altaEtapaControl` con los tres datos. Redirige a `/recetas/etapas-control` con la alerta "Etapa de control creada correctamente" de tipo `success`. |
| **CP-ECC-13** | Alta sin descripción | `nombre: "Control de densidad"`, `etapaAControlar: MACERACION`, sin descripción | La descripción es opcional | Llama a `altaEtapaControl` y redirige a `/recetas/etapas-control`. |
| **CP-ECC-14** | Faltan los datos obligatorios | `nombre: "  "`, sin etapa a controlar | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear la etapa de control" y los mensajes "El nombre de la etapa de control es obligatorio" y "La etapa a controlar es obligatoria". No invoca al service. |
| **CP-ECC-15** | Descripción demasiado larga | `descripcion` de 256 caracteres | `@Size(max = 255)` $\rightarrow$ **Falla** | Vuelve al formulario con "La descripción no puede superar los 255 caracteres". No invoca al service. |
| **CP-ECC-16** | Nombre duplicado para la misma etapa | El service lanza `RecursoDuplicadoException("Ya existe una etapa de control con el nombre 'Control de densidad' para la etapa MACERACION")` | El service rechaza el nombre | Vuelve al formulario con el mensaje del service y lo tipeado conservado (nombre y etapa seleccionada). |
| **CP-ECC-17** | Etapa no controlable | `etapaAControlar: MOLIENDA`. El service lanza `ReglaNegocioException("La etapa a controlar debe ser MACERACION, FERMENTACION, HERVIDO o MADURACION")` | El service rechaza la etapa | Vuelve al formulario con el panel "No se pudo crear la etapa de control" y el mensaje del service junto al campo de la etapa. |
| **CP-ECC-18** | Alta contextual correcta | Datos válidos, `retorno: "/recetas/nueva"`, `campo: "idEtapaControl"`. El service devuelve la etapa de control (`id: 9`) | El alta se abrió desde otro formulario | Redirige a `/recetas/nueva?desdeAlta=1&idEtapaControl=9` con la alerta "Etapa de control creada correctamente". |
| **CP-ECC-19** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 4. `GET /recetas/etapas-control/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-ECC-20** | Formulario de edición con los datos actuales | `id: 4`. El service devuelve "Control de densidad", descripción "Densidad del mosto", `FERMENTACION` | La etapa de control existe | Responde 200 con la vista `receta/etapa-control-form`: título "Editar etapa de control", el nombre y la descripción actuales, la opción `FERMENTACION` seleccionada, `action="/recetas/etapas-control/4"` y el botón "Guardar cambios". |
| **CP-ECC-21** | Rol no autorizado | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 5. `POST /recetas/etapas-control/{id}` — `modificarEtapaControl(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-ECC-22** | Modificación correcta | `id: 4`, `nombre: "Control de gravedad"`, `etapaAControlar: HERVIDO` | Formulario válido | Llama a `modificarEtapaControl(4, ...)` con la etapa `HERVIDO`. Redirige a `/recetas/etapas-control` con la alerta "Etapa de control modificada correctamente" de tipo `success`. |
| **CP-ECC-23** | Datos inválidos | `id: 4`, `nombre: ""`, sin etapa | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario de edición (`action="/recetas/etapas-control/4"`) con el panel "No se pudo modificar la etapa de control" y los mensajes de los dos campos. No invoca al service. |
| **CP-ECC-24** | Nombre duplicado | El service lanza `RecursoDuplicadoException` | El service rechaza el nombre | Vuelve al formulario de edición con el panel "No se pudo modificar la etapa de control" y el mensaje del service. |
| **CP-ECC-25** | Etapa no controlable | `etapaAControlar: ENVASADO`. El service lanza `ReglaNegocioException` | El service rechaza la etapa | Vuelve al formulario de edición y muestra el mensaje del service. |
| **CP-ECC-26** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 6. `POST /recetas/etapas-control/{id}/baja` — `bajaEtapaControl(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-ECC-27** | Baja correcta | `id: 4` | La etapa de control no está asociada al plan de monitoreo de una receta activa | Llama a `bajaEtapaControl(4)`. Redirige a `/recetas/etapas-control` con la alerta "Etapa de control dada de baja correctamente" de tipo `success`. |
| **CP-ECC-28** | Baja rechazada por el service | El service lanza `ReglaNegocioException("No se puede dar de baja la etapa de control porque está asociada al plan de monitoreo de una receta activa")`, con cabecera `Referer: /recetas/etapas-control` | La etapa de control está asociada al plan de monitoreo de una receta activa | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-ECC-29** | Rol no autorizado | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
