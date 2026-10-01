## Casos de prueba de `LocalidadController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `LocalidadControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; `ILocalidadServicio`, `IProvinciaServicio` e `IPaisServicio` están simulados (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/ubicacion/CasosPruebaLocalidad.md`.

**Roles requeridos:**
- Listado, alta, edición y baja: solo `ADMINISTRADOR`.
- Buscador (`/ubicaciones/localidades/buscador`): `ADMINISTRADOR`, `GERENTE_COMERCIAL` (localidad de un cliente) y `GERENTE_DE_COMPRAS` (localidad de un proveedor). Los gerentes no acceden al módulo; solo usan el buscador.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Responde 422 con el formulario"_ significa que el fragmento `ubicacion/localidad-form :: formulario` se devuelve con los errores para que el modal lo muestre sin perder lo tipeado.
- _"Alta al vuelo"_ es el alta abierta desde un selector (parámetro `modo=seleccion`): en vez de redirigir responde 201 con la localidad creada.
- La provincia se elige con un selector (`data-selector`); el botón "+" para crearla (`data-selector-alta`) solo se ofrece al `ADMINISTRADOR`.
- Los formularios y el buscador son fragmentos HTML pensados para un modal.

---

### 1. `GET /ubicaciones/localidades` — `listarLocalidades(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-01** | Listado para el administrador con acciones por fila | Rol `ADMINISTRADOR`. El service devuelve 1 localidad (`id: 8`, "Río Cuarto", CP "5800", provincia "Córdoba", país "Argentina") | El rol está autorizado | Responde 200 con la vista `ubicacion/localidad-lista`. Muestra nombre, código postal, provincia y país, el botón de alta con `data-modal-url="/ubicaciones/localidades/nuevo"`, el de edición con `data-modal-url="/ubicaciones/localidades/8/editar"` y el de baja con `data-baja-url="/ubicaciones/localidades/8/baja"`. |
| **CP-LOC-02** | Paginación y orden alfabético por defecto | Rol `ADMINISTRADOR`, sin parámetros | Sin criterios. Localidad es un módulo maestro | Llama al service con los cuatro criterios en `null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-LOC-03** | Los cuatro criterios de filtro | Rol `ADMINISTRADOR`, `nombre: "rio"`, `codigoPostal: "5800"`, `idProvincia: 5`, `idPais: 3`. Los servicios de provincia y país devuelven "Córdoba" y "Argentina" | Provincia y país del filtro existen | Llama al service con los cuatro criterios. Los selectores muestran `idProvincia=5` con "Córdoba (Argentina)" e `idPais=3` con "Argentina". |
| **CP-LOC-04** | Provincia o país del filtro inexistentes | `idProvincia: 99`, `idPais: 98`. Ambos servicios lanzan `RecursoNoEncontradoException` | Provincia y país ya no existen | Esos filtros se ignoran: llama al service con `idProvincia: null` e `idPais: null`. |
| **CP-LOC-05** | Rol sin acceso al módulo | Rol `GERENTE_COMERCIAL` | `@PreAuthorize("hasRole('ADMINISTRADOR')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /ubicaciones/localidades/buscador` — `buscarLocalidades(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-06** | Buscador para el gerente comercial | Rol `GERENTE_COMERCIAL`, `nombre: "rio"`. El service devuelve "Río Cuarto" de "Córdoba" (`id: 8`) | El rol está autorizado para el buscador | Responde 200 con el fragmento `ubicacion/buscador-localidad :: buscador`. Cada localidad tiene un botón `data-elegir data-id="8" data-texto="Río Cuarto (Córdoba)"`. Los filtros de provincia y país usan los buscadores `/ubicaciones/provincias/buscador` y `/ubicaciones/paises/buscador`, sin botón "+" ni acciones de baja. Llama al service con `nombre: "rio"` y páginas de 8 elementos. |
| **CP-LOC-07** | Buscador para el gerente de compras | Rol `GERENTE_DE_COMPRAS`, el service devuelve una página vacía | El rol está autorizado para el buscador | Responde 200 y muestra "No se encontraron localidades con los criterios indicados.". |
| **CP-LOC-08** | Rol sin relación con clientes ni proveedores | Rol `ENCARGADO_DE_DEPOSITO` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 3. `GET /ubicaciones/localidades/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-09** | Formulario de alta con selector de provincia | Rol `ADMINISTRADOR` | El rol puede crear provincias | Responde 200 con el fragmento `ubicacion/localidad-form :: formulario`. Muestra "Nueva localidad", el asterisco de obligatorio, `action="/ubicaciones/localidades"`, el selector de provincia con `data-buscador-url="/ubicaciones/provincias/buscador"` y el botón "+" con `data-alta-url="/ubicaciones/provincias/nuevo?modo=seleccion"`. No incluye el campo `modo`. |
| **CP-LOC-10** | Formulario abierto desde un selector | Rol `ADMINISTRADOR`, parámetro `modo=seleccion` | El alta se abre desde un selector | Incluye el campo oculto `modo` con valor `seleccion`. |
| **CP-LOC-11** | Rol sin permiso de alta | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 4. `POST /ubicaciones/localidades` — `altaLocalidad(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-12** | Alta correcta | Rol `ADMINISTRADOR`, `nombre: "Río Cuarto"`, `codigoPostal: "5800"`, `idProvincia: 5`, con CSRF | Formulario válido | Llama a `altaLocalidad` con los tres datos. Redirige a `/ubicaciones/localidades` con la alerta "Localidad creada correctamente" de tipo `success`. |
| **CP-LOC-13** | Faltan nombre, código postal y provincia | `nombre: " "`, sin código postal ni `idProvincia` | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Responde 422 con el formulario, el panel "No se pudo crear la localidad" y los mensajes "El nombre de la localidad es obligatorio", "El código postal es obligatorio" y "La provincia es obligatoria". No invoca al service. |
| **CP-LOC-14** | El selector conserva la provincia al volver con errores | `nombre: ""`, `codigoPostal: "5800"`, `idProvincia: 5`. `IProvinciaServicio` devuelve "Córdoba" de "Argentina" | El formulario se vuelve a mostrar | El selector conserva `idProvincia=5` y el texto "Córdoba (Argentina)"; el código postal tipeado se conserva. |
| **CP-LOC-15** | Nombre duplicado en la provincia | El service lanza `RecursoDuplicadoException("Ya existe una localidad con ese nombre en la provincia")` | El service rechaza el nombre | Responde 422 con el formulario, el mensaje del service junto al campo y el nombre tipeado conservado. |
| **CP-LOC-16** | Provincia elegida inexistente | `idProvincia: 99`. El service lanza `RecursoNoEncontradoException("No existe la provincia indicada")` | La provincia ya no existe | Responde 422 y muestra el mensaje junto al campo provincia. |
| **CP-LOC-17** | Alta al vuelo correcta | Datos válidos con `modo: "seleccion"`. El service devuelve la localidad (`id: 8`, "Río Cuarto", provincia "Córdoba") | El alta se abrió desde un selector | Responde 201 con el fragmento `ubicacion/resultado-alta :: resultado` que contiene `data-id="8"` y `data-texto="Río Cuarto (Córdoba)"`. No redirige. |
| **CP-LOC-18** | Rol sin permiso de alta | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 5. `GET /ubicaciones/localidades/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-19** | Formulario de edición con los datos actuales | Rol `ADMINISTRADOR`, `id: 8`. El service devuelve "Río Cuarto", CP "5800", provincia "Córdoba" (`id: 5`) | La localidad existe | Responde 200 con el fragmento `ubicacion/localidad-form :: formulario`: título "Editar localidad", nombre y código postal actuales, el selector con `idProvincia=5` y el texto "Córdoba (Argentina)", `action="/ubicaciones/localidades/8"` y el botón "Guardar cambios". |

---

### 6. `POST /ubicaciones/localidades/{id}` — `modificarLocalidad(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-20** | Modificación correcta | Rol `ADMINISTRADOR`, `id: 8`, `nombre: "Villa María"`, `codigoPostal: "5900"`, `idProvincia: 5` | Formulario válido | Llama a `modificarLocalidad(8, ...)`. Redirige a `/ubicaciones/localidades` con la alerta "Localidad modificada correctamente" de tipo `success`. |
| **CP-LOC-21** | Nombre duplicado en la provincia | El service lanza `RecursoDuplicadoException` | El service rechaza el nombre | Responde 422 con el formulario de edición (`action="/ubicaciones/localidades/8"`), el panel "No se pudo modificar la localidad" y el mensaje del service. |
| **CP-LOC-22** | Rol sin permiso de modificación | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 7. `POST /ubicaciones/localidades/{id}/baja` — `bajaLocalidad(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LOC-23** | Baja correcta | Rol `ADMINISTRADOR`, `id: 8` | La localidad no está asociada a clientes ni proveedores activos | Llama a `bajaLocalidad(8)`. Redirige a `/ubicaciones/localidades` con la alerta "Localidad dada de baja correctamente" de tipo `success`. |
| **CP-LOC-24** | Baja rechazada por el service | El service lanza `ReglaNegocioException("La localidad está asociada a un cliente activo")`, con cabecera `Referer: /ubicaciones/localidades` | La localidad está asociada a un cliente o proveedor activo | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-LOC-25** | Rol sin permiso de baja | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
