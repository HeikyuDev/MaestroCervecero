## Casos de prueba de `LocalidadController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `LocalidadControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; `ILocalidadServicio`, `IProvinciaServicio` e `IPaisServicio` están simulados (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/ubicacion/CasosPruebaLocalidad.md`.

**Roles requeridos:**
- Listado, alta, edición y baja: solo `ADMINISTRADOR`.
- Buscador (`/ubicaciones/localidades/buscador`): `ADMINISTRADOR`, `GERENTE_COMERCIAL` (localidad de un cliente) y `GERENTE_DE_COMPRAS` (localidad de un proveedor). Los gerentes no acceden al módulo; solo usan el buscador.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `ubicacion/localidad-form`, sin redirigir, para conservar lo tipeado.
- _"Alta contextual"_ es el alta abierta desde el selector de otro formulario (por ejemplo, el de cliente): llegan los parámetros `retorno` (ruta interna del formulario de origen) y `campo` (campo que recibe el id creado). Al guardar redirige a `retorno` agregando `campo=<id>` y `desdeAlta=1`.
- La provincia se elige con un selector (`data-selector`): se hace clic en el propio campo (`data-selector-buscar`) para abrir el buscador. El menú de tres puntitos (`data-selector-menu`) con la opción "Crear nueva provincia" (`data-selector-alta`) solo se ofrece al `ADMINISTRADOR` y lleva a `/ubicaciones/provincias/nuevo`. Al volver de crear la provincia, `idProvincia` llega en la URL y queda elegida.
- El buscador es un fragmento HTML que se muestra en un modal del selector.

---

### 1. `GET /ubicaciones/localidades` — `listarLocalidades(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-01** | Listado para el administrador con acciones por fila | Rol `ADMINISTRADOR`. El service devuelve 1 localidad (`id: 8`, "Río Cuarto", CP "5800", provincia "Córdoba", país "Argentina") | El rol está autorizado | Responde 200 con la vista `ubicacion/localidad-lista`. Muestra nombre, código postal, provincia y país, el link de alta `/ubicaciones/localidades/nuevo`, el de edición `/ubicaciones/localidades/8/editar` y el botón de baja con `data-baja-url="/ubicaciones/localidades/8/baja"`. |
| **CP-LOC-02** | Paginación y orden alfabético por defecto | Rol `ADMINISTRADOR`, sin parámetros | Sin criterios. Localidad es un módulo maestro | Llama al service con los cuatro criterios en `null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-LOC-03** | Los cuatro criterios de filtro | Rol `ADMINISTRADOR`, `nombre: "rio"`, `codigoPostal: "5800"`, `idProvincia: 5`, `idPais: 3`. Los servicios de provincia y país devuelven "Córdoba" y "Argentina" | Provincia y país del filtro existen | Llama al service con los cuatro criterios. Los selectores muestran `idProvincia=5` con "Córdoba (Argentina)" e `idPais=3` con "Argentina". |
| **CP-LOC-04** | Provincia o país del filtro inexistentes | `idProvincia: 99`, `idPais: 98`. Ambos servicios lanzan `RecursoNoEncontradoException` | Provincia y país ya no existen | Esos filtros se ignoran: llama al service con `idProvincia: null` e `idPais: null`. |
| **CP-LOC-05** | Rol sin acceso al módulo | Rol `GERENTE_COMERCIAL` | `@PreAuthorize("hasRole('ADMINISTRADOR')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /ubicaciones/localidades/buscador` — `buscarLocalidades(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-06** | Buscador para el gerente comercial | Rol `GERENTE_COMERCIAL`, `nombre: "rio"`. El service devuelve "Río Cuarto" de "Córdoba" (`id: 8`) | El rol está autorizado para el buscador | Responde 200 con el fragmento `ubicacion/buscador-localidad :: buscador`. Cada localidad tiene un botón `data-elegir data-id="8" data-texto="Río Cuarto (Córdoba)"`. Los filtros de provincia y país usan los buscadores `/ubicaciones/provincias/buscador` y `/ubicaciones/paises/buscador`, sin menú de creación ni acciones de baja. Llama al service con `nombre: "rio"` y páginas de 8 elementos. |
| **CP-LOC-07** | Buscador para el gerente de compras | Rol `GERENTE_DE_COMPRAS`, el service devuelve una página vacía | El rol está autorizado para el buscador | Responde 200 y muestra "No se encontraron localidades con los criterios indicados.". |
| **CP-LOC-08** | Rol sin relación con clientes ni proveedores | Rol `ENCARGADO_DE_DEPOSITO` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 3. `GET /ubicaciones/localidades/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-09** | Formulario de alta con selector de provincia | Rol `ADMINISTRADOR` | El rol puede crear provincias | Responde 200 con la vista `ubicacion/localidad-form`. Muestra "Nueva localidad", el asterisco de obligatorio, `action="/ubicaciones/localidades"`, el link "Volver a localidades", el selector de provincia con `data-buscador-url="/ubicaciones/provincias/buscador"` y el menú de tres puntitos con la opción "Crear nueva provincia" (`data-alta-url="/ubicaciones/provincias/nuevo"`). No incluye el campo `retorno`. |
| **CP-LOC-10** | Alta contextual: se conservan retorno y campo | Rol `ADMINISTRADOR`, `retorno: "/clientes/nuevo"`, `campo: "idLocalidad"` | El alta se abre desde el selector de otro formulario | Incluye los campos ocultos `retorno` y `campo`. El link de volver dice "Volver al formulario anterior" y apunta a `/clientes/nuevo?desdeAlta=1`. |
| **CP-LOC-11** | Rol sin permiso de alta | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |
| **CP-LOC-26** | Vuelta de crear una provincia | `idProvincia: 5`, `desdeAlta: 1`. `IProvinciaServicio` devuelve "Córdoba" de "Argentina" | La provincia recién creada llega en la URL | El selector queda con `idProvincia=5` y el texto "Córdoba (Argentina)". |

---

### 4. `POST /ubicaciones/localidades` — `altaLocalidad(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-12** | Alta correcta | Rol `ADMINISTRADOR`, `nombre: "Río Cuarto"`, `codigoPostal: "5800"`, `idProvincia: 5`, con CSRF | Formulario válido | Llama a `altaLocalidad` con los tres datos. Redirige a `/ubicaciones/localidades` con la alerta "Localidad creada correctamente" de tipo `success`. |
| **CP-LOC-13** | Faltan nombre, código postal y provincia | `nombre: " "`, sin código postal ni `idProvincia` | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear la localidad" y los mensajes "El nombre de la localidad es obligatorio", "El código postal es obligatorio" y "La provincia es obligatoria". No invoca al service. |
| **CP-LOC-14** | El selector conserva la provincia al volver con errores | `nombre: ""`, `codigoPostal: "5800"`, `idProvincia: 5`. `IProvinciaServicio` devuelve "Córdoba" de "Argentina" | El formulario se vuelve a mostrar | El selector conserva `idProvincia=5` y el texto "Córdoba (Argentina)"; el código postal tipeado se conserva. |
| **CP-LOC-15** | Nombre duplicado en la provincia | El service lanza `RecursoDuplicadoException("Ya existe una localidad con ese nombre en la provincia")` | El service rechaza el nombre | Vuelve al formulario con el mensaje del service junto al campo y el nombre tipeado conservado. |
| **CP-LOC-16** | Provincia elegida inexistente | `idProvincia: 99`. El service lanza `RecursoNoEncontradoException("No existe la provincia indicada")` | La provincia ya no existe | Vuelve al formulario y muestra el mensaje junto al campo provincia. |
| **CP-LOC-17** | Alta contextual correcta | Datos válidos, `retorno: "/clientes/nuevo"`, `campo: "idLocalidad"`. El service devuelve la localidad (`id: 8`) | El alta se abrió desde otro formulario | Redirige a `/clientes/nuevo?desdeAlta=1&idLocalidad=8` con la alerta "Localidad creada correctamente". |
| **CP-LOC-18** | Rol sin permiso de alta | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 5. `GET /ubicaciones/localidades/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-19** | Formulario de edición con los datos actuales | Rol `ADMINISTRADOR`, `id: 8`. El service devuelve "Río Cuarto", CP "5800", provincia "Córdoba" (`id: 5`) | La localidad existe | Responde 200 con la vista `ubicacion/localidad-form`: título "Editar localidad", nombre y código postal actuales, el selector con `idProvincia=5` y el texto "Córdoba (Argentina)", `action="/ubicaciones/localidades/8"` y el botón "Guardar cambios". |
| **CP-LOC-27** | Vuelta de crear una provincia al editar | `id: 8`, `idProvincia: 9`. `IProvinciaServicio` devuelve "Santa Fe" | La provincia recién creada llega en la URL | El selector muestra `idProvincia=9` y "Santa Fe (Argentina)" en lugar de la provincia actual. |

---

### 6. `POST /ubicaciones/localidades/{id}` — `modificarLocalidad(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-20** | Modificación correcta | Rol `ADMINISTRADOR`, `id: 8`, `nombre: "Villa María"`, `codigoPostal: "5900"`, `idProvincia: 5` | Formulario válido | Llama a `modificarLocalidad(8, ...)`. Redirige a `/ubicaciones/localidades` con la alerta "Localidad modificada correctamente" de tipo `success`. |
| **CP-LOC-21** | Nombre duplicado en la provincia | El service lanza `RecursoDuplicadoException` | El service rechaza el nombre | Vuelve al formulario de edición (`action="/ubicaciones/localidades/8"`) con el panel "No se pudo modificar la localidad" y el mensaje del service. |
| **CP-LOC-22** | Rol sin permiso de modificación | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 7. `POST /ubicaciones/localidades/{id}/baja` — `bajaLocalidad(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-23** | Baja correcta | Rol `ADMINISTRADOR`, `id: 8` | La localidad no está asociada a clientes ni proveedores activos | Llama a `bajaLocalidad(8)`. Redirige a `/ubicaciones/localidades` con la alerta "Localidad dada de baja correctamente" de tipo `success`. |
| **CP-LOC-24** | Baja rechazada por el service | El service lanza `ReglaNegocioException("La localidad está asociada a un cliente activo")`, con cabecera `Referer: /ubicaciones/localidades` | La localidad está asociada a un cliente o proveedor activo | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-LOC-25** | Rol sin permiso de baja | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
