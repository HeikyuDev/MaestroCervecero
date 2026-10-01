## Casos de prueba de `ProvinciaController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `ProvinciaControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; `IProvinciaServicio` e `IPaisServicio` están simulados (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/ubicacion/CasosPruebaProvincia.md`.

**Roles requeridos:**
- Listado, alta, edición y baja: solo `ADMINISTRADOR`.
- Buscador (`/ubicaciones/provincias/buscador`): `ADMINISTRADOR`, `GERENTE_COMERCIAL` y `GERENTE_DE_COMPRAS`. Los gerentes no acceden al módulo; solo usan el buscador como criterio de filtro al seleccionar una localidad.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `ubicacion/provincia-form`, sin redirigir, para conservar lo tipeado.
- _"Alta contextual"_ es el alta abierta desde el selector de otro formulario: llegan los parámetros `retorno` (ruta interna del formulario de origen) y `campo` (campo que recibe el id creado). Al guardar redirige a `retorno` agregando `campo=<id>` y `desdeAlta=1`; si `retorno` ya trae parámetros (alta encadenada) se conservan.
- El país se elige con un selector (`data-selector`): se ve como un desplegable (flecha hacia abajo, placeholder "Seleccionar país", no se tipea) y se hace clic en el propio campo (`data-selector-buscar`) para abrir el buscador. El menú de tres puntitos (`data-selector-menu`) con la opción "Crear nuevo país" (`data-selector-alta`) solo se ofrece al `ADMINISTRADOR` y lleva a `/ubicaciones/paises/nuevo`. Al volver de crear el país, `idPais` llega en la URL y queda elegido.
- El buscador es un fragmento HTML que se muestra en un modal del selector.

---

### 1. `GET /ubicaciones/provincias` — `listarProvincias(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-01** | Listado para el administrador con acciones por fila | Rol `ADMINISTRADOR`. El service devuelve 1 provincia (`id: 5`, "Córdoba", país "Argentina") | El rol está autorizado | Responde 200 con la vista `ubicacion/provincia-lista`. Muestra provincia y país, el link de alta `/ubicaciones/provincias/nuevo`, el de edición `/ubicaciones/provincias/5/editar` y el botón de baja con `data-baja-url="/ubicaciones/provincias/5/baja"`. |
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
| **CP-PRC-09** | Formulario de alta con selector de país | Rol `ADMINISTRADOR` | El rol puede crear países | Responde 200 con la vista `ubicacion/provincia-form`. Muestra "Nueva provincia", el asterisco de obligatorio, `action="/ubicaciones/provincias"`, el link "Volver a provincias", el selector de país con `data-buscador-url="/ubicaciones/paises/buscador"` y el menú de tres puntitos con la opción "Crear nuevo país" (`data-alta-url="/ubicaciones/paises/nuevo"`). No incluye el campo `retorno`. |
| **CP-PRC-10** | Alta contextual: se conservan retorno y campo | Rol `ADMINISTRADOR`, `retorno: "/ubicaciones/localidades/nuevo"`, `campo: "idProvincia"` | El alta se abre desde el selector de otro formulario | Incluye los campos ocultos `retorno` y `campo`. El link de volver dice "Volver al formulario anterior" y apunta a `/ubicaciones/localidades/nuevo?desdeAlta=1`. |
| **CP-PRC-11** | Rol sin permiso de alta | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |
| **CP-PRC-26** | Vuelta de crear un país | `idPais: 7`, `desdeAlta: 1`. `IPaisServicio` devuelve "Chile" | El país recién creado llega en la URL | El selector queda con `idPais=7` y el texto "Chile". |

---

### 4. `POST /ubicaciones/provincias` — `altaProvincia(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-12** | Alta correcta | Rol `ADMINISTRADOR`, `nombre: "Córdoba"`, `idPais: 3`, con CSRF | Formulario válido | Llama a `altaProvincia` con el nombre y el país indicados. Redirige a `/ubicaciones/provincias` con la alerta "Provincia creada correctamente" de tipo `success`. |
| **CP-PRC-13** | Faltan nombre y país | `nombre: "  "`, sin `idPais` | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear la provincia" y los mensajes "El nombre de la provincia es obligatorio" y "El país es obligatorio". No invoca al service. |
| **CP-PRC-14** | El selector conserva el país al volver con errores | `nombre: ""`, `idPais: 3`. `IPaisServicio` devuelve "Argentina" | El formulario se vuelve a mostrar | El selector conserva `idPais=3` y el texto "Argentina". |
| **CP-PRC-15** | Nombre duplicado | El service lanza `RecursoDuplicadoException("Ya existe una provincia con ese nombre")` | El service rechaza el nombre | Vuelve al formulario con el mensaje del service junto al campo y el nombre tipeado conservado. |
| **CP-PRC-16** | País elegido inexistente | `idPais: 99`. El service lanza `RecursoNoEncontradoException("No existe el país indicado")` | El país ya no existe | Vuelve al formulario y muestra el mensaje junto al campo país. |
| **CP-PRC-17** | Alta contextual correcta | `nombre: "Córdoba"`, `idPais: 3`, `retorno: "/ubicaciones/localidades/nuevo"`, `campo: "idProvincia"`. El service devuelve la provincia (`id: 5`) | El alta se abrió desde otro formulario | Redirige a `/ubicaciones/localidades/nuevo?desdeAlta=1&idProvincia=5` con la alerta "Provincia creada correctamente". |
| **CP-PRC-27** | Alta contextual encadenada | `retorno: "/ubicaciones/localidades/nuevo?retorno=%2Fx&campo=idLocalidad"`, `campo: "idProvincia"` | El formulario de origen tiene a su vez un retorno | Redirige a `/ubicaciones/localidades/nuevo?retorno=%2Fx&campo=idLocalidad&desdeAlta=1&idProvincia=5`: el retorno original se conserva. |
| **CP-PRC-18** | Rol sin permiso de alta | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 5. `GET /ubicaciones/provincias/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-19** | Formulario de edición con los datos actuales | Rol `ADMINISTRADOR`, `id: 5`. El service devuelve "Córdoba" de "Argentina" (`id: 3`) | La provincia existe | Responde 200 con la vista `ubicacion/provincia-form`: título "Editar provincia", el nombre actual, el selector con `idPais=3` y el texto "Argentina", `action="/ubicaciones/provincias/5"` y el botón "Guardar cambios". |
| **CP-PRC-28** | Vuelta de crear un país al editar | `id: 5`, `idPais: 7`. `IPaisServicio` devuelve "Chile" | El país recién creado llega en la URL | El selector muestra `idPais=7` y "Chile" en lugar del país actual. |

---

### 6. `POST /ubicaciones/provincias/{id}` — `modificarProvincia(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-20** | Modificación correcta | Rol `ADMINISTRADOR`, `id: 5`, `nombre: "Santa Fe"`, `idPais: 3` | Formulario válido | Llama a `modificarProvincia(5, ...)`. Redirige a `/ubicaciones/provincias` con la alerta "Provincia modificada correctamente" de tipo `success`. |
| **CP-PRC-21** | Nombre duplicado | El service lanza `RecursoDuplicadoException` | El service rechaza el nombre | Vuelve al formulario de edición (`action="/ubicaciones/provincias/5"`) con el panel "No se pudo modificar la provincia" y el mensaje del service. |
| **CP-PRC-22** | Rol sin permiso de modificación | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 7. `POST /ubicaciones/provincias/{id}/baja` — `bajaProvincia(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PRC-23** | Baja correcta | Rol `ADMINISTRADOR`, `id: 5` | La provincia no tiene localidades activas | Llama a `bajaProvincia(5)`. Redirige a `/ubicaciones/provincias` con la alerta "Provincia dada de baja correctamente" de tipo `success`. |
| **CP-PRC-24** | Baja rechazada por el service | El service lanza `ReglaNegocioException("La provincia tiene localidades asociadas")`, con cabecera `Referer: /ubicaciones/provincias` | La provincia tiene localidades activas | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-PRC-25** | Rol sin permiso de baja | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
