## Casos de prueba de `ProvinciaController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `ProvinciaControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; `IProvinciaServicio` e `IPaisServicio` están simulados (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/ubicacion/CasosPruebaProvincia.md`.

**Roles requeridos:**
- Listado, alta, edición y baja: solo `ADMINISTRADOR`.
- Buscador (`/ubicaciones/provincias/buscador`): `ADMINISTRADOR`, `GERENTE_COMERCIAL` y `GERENTE_DE_COMPRAS`. Los gerentes no acceden al módulo; solo usan el buscador como criterio de filtro al seleccionar una localidad.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Responde 422 con el formulario"_ significa que el fragmento `ubicacion/provincia-form :: formulario` se devuelve con los errores para que el modal lo muestre sin perder lo tipeado.
- _"Alta al vuelo"_ es el alta abierta desde un selector (parámetro `modo=seleccion`): en vez de redirigir responde 201 con la provincia creada.
- El país se elige con un selector (`data-selector`); el botón "+" para crearlo (`data-selector-alta`) solo se ofrece al `ADMINISTRADOR`.
- Los formularios y el buscador son fragmentos HTML pensados para un modal.

---

### 1. `GET /ubicaciones/provincias` — `listarProvincias(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-01** | Listado para el administrador con acciones por fila | Rol `ADMINISTRADOR`. El service devuelve 1 provincia (`id: 5`, "Córdoba", país "Argentina") | El rol está autorizado | Responde 200 con la vista `ubicacion/provincia-lista`. Muestra provincia y país, el botón de alta con `data-modal-url="/ubicaciones/provincias/nuevo"`, el de edición con `data-modal-url="/ubicaciones/provincias/5/editar"` y el de baja con `data-baja-url="/ubicaciones/provincias/5/baja"`. |
| **CP-PRC-02** | Paginación y orden alfabético por defecto | Rol `ADMINISTRADOR`, sin parámetros | Sin criterios. Provincia es un módulo maestro | Llama al service con `nombre: null`, `idPais: null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-PRC-03** | Filtro por país | Rol `ADMINISTRADOR`, `nombre: "cor"`, `idPais: 3`. `IPaisServicio` devuelve "Argentina" | El país del filtro existe | Llama al service con `nombre: "cor"` e `idPais: 3`. El selector de país muestra `idPais=3` y el texto "Argentina". |
| **CP-PRC-04** | País del filtro inexistente | `idPais: 99`. `IPaisServicio` lanza `RecursoNoEncontradoException` | El país del filtro ya no existe | El filtro de país se ignora: llama al service con `idPais: null`. |
| **CP-PRC-05** | Rol sin acceso al módulo | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize("hasRole('ADMINISTRADOR')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /ubicaciones/provincias/buscador` — `buscarProvincias(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-06** | Buscador para el gerente comercial | Rol `GERENTE_COMERCIAL`, `nombre: "cor"`. El service devuelve "Córdoba" de "Argentina" (`id: 5`) | El rol está autorizado para el buscador | Responde 200 con el fragmento `ubicacion/buscador-provincia :: buscador`. Cada provincia tiene un botón `data-elegir data-id="5" data-texto="Córdoba (Argentina)"`; no hay acciones de baja. Llama al service con `nombre: "cor"` y páginas de 8 elementos. |
| **CP-PRC-07** | Buscador para el gerente de compras | Rol `GERENTE_DE_COMPRAS`, el service devuelve una página vacía | El rol está autorizado para el buscador | Responde 200 y muestra "No se encontraron provincias con los criterios indicados.". |
| **CP-PRC-08** | Rol sin relación con clientes ni proveedores | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 3. `GET /ubicaciones/provincias/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-09** | Formulario de alta con selector de país | Rol `ADMINISTRADOR` | El rol puede crear países | Responde 200 con el fragmento `ubicacion/provincia-form :: formulario`. Muestra "Nueva provincia", el asterisco de obligatorio, `action="/ubicaciones/provincias"`, el selector de país con `data-buscador-url="/ubicaciones/paises/buscador"` y el botón "+" con `data-alta-url="/ubicaciones/paises/nuevo?modo=seleccion"`. No incluye el campo `modo`. |
| **CP-PRC-10** | Formulario abierto desde un selector | Rol `ADMINISTRADOR`, parámetro `modo=seleccion` | El alta se abre desde un selector | Incluye el campo oculto `modo` con valor `seleccion`. |
| **CP-PRC-11** | Rol sin permiso de alta | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 4. `POST /ubicaciones/provincias` — `altaProvincia(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-12** | Alta correcta | Rol `ADMINISTRADOR`, `nombre: "Córdoba"`, `idPais: 3`, con CSRF | Formulario válido | Llama a `altaProvincia` con el nombre y el país indicados. Redirige a `/ubicaciones/provincias` con la alerta "Provincia creada correctamente" de tipo `success`. |
| **CP-PRC-13** | Faltan nombre y país | `nombre: "  "`, sin `idPais` | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Responde 422 con el formulario, el panel "No se pudo crear la provincia" y los mensajes "El nombre de la provincia es obligatorio" y "El país es obligatorio". No invoca al service. |
| **CP-PRC-14** | El selector conserva el país al volver con errores | `nombre: ""`, `idPais: 3`. `IPaisServicio` devuelve "Argentina" | El formulario se vuelve a mostrar | El selector conserva `idPais=3` y el texto "Argentina". |
| **CP-PRC-15** | Nombre duplicado | El service lanza `RecursoDuplicadoException("Ya existe una provincia con ese nombre")` | El service rechaza el nombre | Responde 422 con el formulario, el mensaje del service junto al campo y el nombre tipeado conservado. |
| **CP-PRC-16** | País elegido inexistente | `idPais: 99`. El service lanza `RecursoNoEncontradoException("No existe el país indicado")` | El país ya no existe | Responde 422 y muestra el mensaje junto al campo país. |
| **CP-PRC-17** | Alta al vuelo correcta | `nombre: "Córdoba"`, `idPais: 3`, `modo: "seleccion"`. El service devuelve la provincia (`id: 5`, país "Argentina") | El alta se abrió desde un selector | Responde 201 con el fragmento `ubicacion/resultado-alta :: resultado` que contiene `data-id="5"` y `data-texto="Córdoba (Argentina)"`. No redirige. |
| **CP-PRC-18** | Rol sin permiso de alta | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 5. `GET /ubicaciones/provincias/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-19** | Formulario de edición con los datos actuales | Rol `ADMINISTRADOR`, `id: 5`. El service devuelve "Córdoba" de "Argentina" (`id: 3`) | La provincia existe | Responde 200 con el fragmento `ubicacion/provincia-form :: formulario`: título "Editar provincia", el nombre actual, el selector con `idPais=3` y el texto "Argentina", `action="/ubicaciones/provincias/5"` y el botón "Guardar cambios". |

---

### 6. `POST /ubicaciones/provincias/{id}` — `modificarProvincia(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-20** | Modificación correcta | Rol `ADMINISTRADOR`, `id: 5`, `nombre: "Santa Fe"`, `idPais: 3` | Formulario válido | Llama a `modificarProvincia(5, ...)`. Redirige a `/ubicaciones/provincias` con la alerta "Provincia modificada correctamente" de tipo `success`. |
| **CP-PRC-21** | Nombre duplicado | El service lanza `RecursoDuplicadoException` | El service rechaza el nombre | Responde 422 con el formulario de edición (`action="/ubicaciones/provincias/5"`), el panel "No se pudo modificar la provincia" y el mensaje del service. |
| **CP-PRC-22** | Rol sin permiso de modificación | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 7. `POST /ubicaciones/provincias/{id}/baja` — `bajaProvincia(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-23** | Baja correcta | Rol `ADMINISTRADOR`, `id: 5` | La provincia no tiene localidades activas | Llama a `bajaProvincia(5)`. Redirige a `/ubicaciones/provincias` con la alerta "Provincia dada de baja correctamente" de tipo `success`. |
| **CP-PRC-24** | Baja rechazada por el service | El service lanza `ReglaNegocioException("La provincia tiene localidades asociadas")`, con cabecera `Referer: /ubicaciones/provincias` | La provincia tiene localidades activas | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-PRC-25** | Rol sin permiso de baja | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
