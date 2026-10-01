## Casos de prueba de `PaisController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `PaisControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IPaisServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/ubicacion/CasosPruebaPais.md`.

**Roles requeridos:**
- Listado, alta, edición y baja: solo `ADMINISTRADOR`.
- Buscador (`/ubicaciones/paises/buscador`): `ADMINISTRADOR`, `GERENTE_COMERCIAL` y `GERENTE_DE_COMPRAS`. Los gerentes no acceden al módulo; solo usan el buscador como criterio de filtro al seleccionar una localidad.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `ubicacion/pais-form`, sin redirigir, para conservar lo tipeado.
- _"Panel resumen"_ es el panel de errores arriba del formulario (`fragments/form-errors`).
- _"Alta contextual"_ es el alta abierta desde el selector de otro formulario: llegan los parámetros `retorno` (ruta interna del formulario de origen) y `campo` (campo que recibe el id creado). Al guardar redirige a `retorno` agregando `campo=<id>` y `desdeAlta=1`. Un `retorno` que no sea una ruta interna del sitio se descarta.
- El buscador es un fragmento HTML que se muestra en un modal del selector.

---

### 1. `GET /ubicaciones/paises` — `listarPaises(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PAC-01** | Listado para el administrador con acciones por fila | Rol `ADMINISTRADOR`. El service devuelve 1 país (`id: 3`, "Argentina") | El rol está autorizado | Responde 200 con la vista `ubicacion/pais-lista`. Muestra "Argentina", las pestañas hacia `/ubicaciones/provincias` y `/ubicaciones/localidades`, el link de alta `/ubicaciones/paises/nuevo`, el de edición `/ubicaciones/paises/3/editar` y el botón de baja con `data-baja-url="/ubicaciones/paises/3/baja"`. El diálogo de confirmación de baja incluye el token CSRF. |
| **CP-PAC-02** | Paginación y orden alfabético por defecto | Rol `ADMINISTRADOR`, `GET /ubicaciones/paises` sin parámetros | Sin criterios. País es un módulo maestro | Llama al service con `nombre: null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-PAC-03** | Criterio de nombre hacia el service | Rol `ADMINISTRADOR`, `nombre: " arg "` con `pagina: 1`, y luego `nombre: "   "` | Un texto en blanco equivale a no filtrar | Llama al service con `nombre: "arg"` (sin espacios) y página 1; con el texto en blanco lo llama con `nombre: null`. |
| **CP-PAC-04** | Listado sin resultados | Rol `ADMINISTRADOR`, el service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron países con los criterios indicados." en lugar de una tabla vacía. |
| **CP-PAC-05** | Rol sin acceso al módulo | Rol `GERENTE_COMERCIAL` | `@PreAuthorize("hasRole('ADMINISTRADOR')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /ubicaciones/paises/buscador` — `buscarPaises(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PAC-06** | Buscador para el gerente comercial | Rol `GERENTE_COMERCIAL`, `nombre: "arg"`. El service devuelve 1 país (`id: 3`, "Argentina") | El rol está autorizado para el buscador | Responde 200 con el fragmento `ubicacion/buscador-pais :: buscador`. Cada país tiene un botón `data-elegir data-id="3" data-texto="Argentina"`; no hay acciones de baja. Llama al service con `nombre: "arg"` y páginas de 8 elementos. |
| **CP-PAC-07** | Buscador para el gerente de compras | Rol `GERENTE_DE_COMPRAS`, el service devuelve una página vacía | El rol está autorizado para el buscador | Responde 200 y muestra "No se encontraron países con los criterios indicados.". |
| **CP-PAC-08** | Rol sin relación con clientes ni proveedores | Rol `GERENTE_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 3. `GET /ubicaciones/paises/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PAC-09** | Formulario de alta | Rol `ADMINISTRADOR` | El rol está autorizado | Responde 200 con la vista `ubicacion/pais-form`. Muestra "Nuevo país", el asterisco rojo de obligatorio, `action="/ubicaciones/paises"`, el botón "Crear país" y el link "Volver a países". No incluye los campos `retorno` ni el panel de errores. |
| **CP-PAC-10** | Alta contextual: se conservan retorno y campo | Rol `ADMINISTRADOR`, `retorno: "/ubicaciones/provincias/nuevo"`, `campo: "idPais"` | El alta se abre desde el selector de otro formulario | Incluye los campos ocultos `retorno` y `campo`. El link de volver y "Cancelar" dicen "Volver al formulario anterior" y apuntan a `/ubicaciones/provincias/nuevo?desdeAlta=1`. |
| **CP-PAC-11** | Rol sin permiso de alta | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |
| **CP-PAC-28** | Retorno fuera del sitio | `retorno: "//sitio-malicioso.com/x"` | Solo se aceptan rutas internas | Se descarta: no hay campo `retorno`, el link de volver es "Volver a países" y no aparece el sitio externo. |

---

### 4. `POST /ubicaciones/paises` — `altaPais(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PAC-12** | Alta correcta | Rol `ADMINISTRADOR`, `nombre: "Chile"`, con CSRF | Formulario válido | Llama a `altaPais` con `nombre: "Chile"`. Redirige a `/ubicaciones/paises` con la alerta "País creado correctamente" de tipo `success`. |
| **CP-PAC-13** | Nombre en blanco | `nombre: "   "` | `@NotBlank` $\rightarrow$ **Falla** | Vuelve al formulario con el panel "No se pudo crear el país" y el mensaje "El nombre del país es obligatorio". No invoca al service. |
| **CP-PAC-14** | Nombre duplicado | El service lanza `RecursoDuplicadoException("Ya existe un país con ese nombre")` | El service rechaza el nombre | Vuelve al formulario con el mensaje del service junto al campo y el nombre tipeado conservado. |
| **CP-PAC-15** | Regla de negocio rechazada | El service lanza `ReglaNegocioException("Regla incumplida")` | El service rechaza la operación | Vuelve al formulario y muestra el mensaje como error general. |
| **CP-PAC-16** | Alta contextual correcta | `nombre: "Chile"`, `retorno: "/ubicaciones/provincias/nuevo"`, `campo: "idPais"`. El service devuelve el país (`id: 9`) | El alta se abrió desde otro formulario | Redirige a `/ubicaciones/provincias/nuevo?desdeAlta=1&idPais=9` con la alerta "País creado correctamente". |
| **CP-PAC-17** | Alta contextual con errores | `nombre: ""`, con `retorno` y `campo` | `@NotBlank` $\rightarrow$ **Falla** | Vuelve al formulario conservando los campos ocultos `retorno` y `campo`. No invoca al service. |
| **CP-PAC-18** | Rol sin permiso de alta | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-PAC-19** | Sin token CSRF | Rol `ADMINISTRADOR`, sin CSRF | Spring Security exige CSRF | La petición es rechazada (redirección) y no invoca al service. |
| **CP-PAC-29** | Alta con retorno fuera del sitio | `nombre: "Chile"`, `retorno: "https://sitio-malicioso.com"` | Solo se aceptan rutas internas | El retorno se ignora: redirige a `/ubicaciones/paises`. |

---

### 5. `GET /ubicaciones/paises/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PAC-20** | Formulario de edición con los datos actuales | Rol `ADMINISTRADOR`, `id: 3`. El service devuelve "Argentina" | El país existe | Responde 200 con la vista `ubicacion/pais-form`: título "Editar país", `value="Argentina"`, `action="/ubicaciones/paises/3"` y el botón "Guardar cambios". |

---

### 6. `POST /ubicaciones/paises/{id}` — `modificarPais(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PAC-21** | Modificación correcta | Rol `ADMINISTRADOR`, `id: 3`, `nombre: "Chile"` | Formulario válido | Llama a `modificarPais(3, ...)`. Redirige a `/ubicaciones/paises` con la alerta "País modificado correctamente" de tipo `success`. |
| **CP-PAC-22** | Nombre duplicado | El service lanza `RecursoDuplicadoException` | El service rechaza el nombre | Vuelve al formulario de edición (`action="/ubicaciones/paises/3"`) con el panel "No se pudo modificar el país" y el mensaje del service. |
| **CP-PAC-23** | Nombre en blanco | `nombre: ""` | `@NotBlank` $\rightarrow$ **Falla** | Vuelve al formulario con "El nombre del país es obligatorio". No invoca al service. |
| **CP-PAC-24** | Rol sin permiso de modificación | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 7. `POST /ubicaciones/paises/{id}/baja` — `bajaPais(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PAC-25** | Baja correcta | Rol `ADMINISTRADOR`, `id: 3` | El país no tiene provincias activas | Llama a `bajaPais(3)`. Redirige a `/ubicaciones/paises` con la alerta "País dado de baja correctamente" de tipo `success`. |
| **CP-PAC-26** | Baja rechazada por el service | El service lanza `ReglaNegocioException("El país tiene provincias asociadas")`, con cabecera `Referer: /ubicaciones/paises` | El país tiene provincias activas | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-PAC-27** | Rol sin permiso de baja | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
