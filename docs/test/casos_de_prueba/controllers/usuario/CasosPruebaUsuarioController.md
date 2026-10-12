## Casos de prueba de `UsuarioController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `UsuarioControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IUsuarioServicio` está simulado (mock) y no hay base de datos. Verifican la URL, el rol requerido, la validación del formulario, el patrón Post-Redirect-Get y lo que se muestra en pantalla. La lógica de negocio se prueba aparte, en los casos de prueba del servicio (`services/usuario/CasosPruebaUsuario.md`).

**Rol requerido:** todos los métodos del controller exigen `ADMINISTRADOR` (`@PreAuthorize("hasRole('ADMINISTRADOR')")`).

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `usuario/usuario-form`, sin redirigir, para conservar lo tipeado.
- _"Panel resumen"_ es el panel de errores arriba del formulario (`fragments/form-errors`).

---

### 1. `GET /usuarios` — `listarUsuarios(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-UC-01** | Listado para el administrador con acciones por fila | Rol `ADMINISTRADOR`. El service devuelve 1 usuario (`id: 7`, `carlos`, "Carlos" "Gomez", `carlos@mail.com`, `GERENTE_DE_COMPRAS`) | El rol está autorizado | Responde 200 con la vista `usuario/usuario-lista`. Muestra correo, nombre, apellido y el rol como "Gerente de Compras", el link `/usuarios/nuevo`, el link de editar `/usuarios/7/editar` y el botón de baja con `data-baja-url="/usuarios/7/baja"`. El diálogo de confirmación de baja incluye el token CSRF. |
| **CP-UC-02** | Paginación y orden alfabético por defecto | Rol `ADMINISTRADOR`, `GET /usuarios` sin parámetros | Sin criterios de búsqueda. Usuario es un módulo maestro | Llama al service con los 5 criterios en `null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `apellido`, luego `nombre` (ambos sin distinguir mayúsculas) y por último `id` como desempate. |
| **CP-UC-03** | Criterios de búsqueda hacia el service | Rol `ADMINISTRADOR`, parámetros `username: "carl"`, `nombre: "   "`, `apellido: "gom"`, `correo: "mail"`, `rol: GERENTE_DE_COMPRAS`, `pagina: 1` | Nombre y apellido se filtran por separado; un texto en blanco equivale a no filtrar | Llama al service con `nombre: null`, `apellido: "gom"`, `correo: "mail"`, `username: "carl"`, el rol indicado y página 1. |
| **CP-UC-04** | Listado sin resultados | Rol `ADMINISTRADOR`, el service devuelve una página vacía | La página no tiene elementos | Responde 200 y muestra "No se encontraron usuarios con los criterios indicados" en lugar de una tabla vacía. |
| **CP-UC-05** | Baja no ofrecida sobre la cuenta propia | Usuario autenticado `admin` con rol `ADMINISTRADOR`; el listado incluye al usuario `admin` (`id: 1`) | El `username` de la fila coincide con el del usuario autenticado | Muestra el basurero deshabilitado con "No se puede dar de baja el usuario porque es la cuenta con la sesión iniciada" y no genera el botón `data-baja-url="/usuarios/1/baja"`. |
| **CP-UC-06** | Rol no autorizado | Rol `GERENTE_DE_PRODUCCION` | `@PreAuthorize("hasRole('ADMINISTRADOR')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /usuarios/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-UC-07** | Formulario de alta | Rol `ADMINISTRADOR` | El rol está autorizado | Responde 200 con la vista `usuario/usuario-form` y los atributos `usuarioForm` y `roles`. Muestra el título "Nuevo usuario", los campos "Nombre/s" y "Apellido/s" (no existe "Nombre completo"), el asterisco rojo en los campos obligatorios, el botón "Crear usuario" y los roles disponibles (ej. "Gerente de Producción"). No muestra el panel resumen de errores. |
| **CP-UC-08** | Rol no autorizado | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con la alerta "No tiene permisos para realizar esta acción" de tipo `danger` (no un "error inesperado"). |
| **CP-UC-31** | Reglas de la contraseña como ayuda | Rol `ADMINISTRADOR` | El formulario informa la regla vigente | Muestra "La contraseña debe tener entre 8 y 72 caracteres y no puede contener espacios en blanco" bajo el campo contraseña. |
| **CP-UC-32** | Rol con nombre legible en listado y filtro | Rol `ADMINISTRADOR`. El service devuelve 1 usuario con rol `GERENTE_DE_PRODUCCION` | La vista muestra la etiqueta del rol, no el nombre de la constante | Muestra "Gerente de Producción" en la fila y en las opciones del filtro; el `value` de la opción sigue siendo `GERENTE_DE_PRODUCCION`. No aparece "GERENTE DE PRODUCCION". |
| **CP-UC-38** | Ayudas de los campos | Rol `ADMINISTRADOR` | El formulario informa cómo se guardan y qué admiten el nombre de usuario, el teléfono y el correo electrónico | Muestra "Se guarda en minúsculas. Puede usar letras, números, punto, guion y guion bajo" bajo el nombre de usuario, "Solo dígitos" bajo el teléfono (que ya no lleva asterisco de obligatorio) y "Se guarda en minúsculas y no puede repetirse en otro usuario" bajo el correo electrónico. |
| **CP-UC-33** | Rol con nombre legible en el encabezado | Autenticado con rol `ADMINISTRADOR` | El encabezado traduce la authority `ROLE_ADMINISTRADOR` a su etiqueta | Bajo el nombre de usuario muestra "Administrador". |

---

### 3. `POST /usuarios` — `altaUsuario(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-UC-09** | Alta con datos válidos _(Camino feliz)_ | Rol `ADMINISTRADOR`, todos los campos válidos, con token CSRF | `@Valid` $\rightarrow$ **Sin errores**, el service no lanza excepción | Invoca `altaUsuario` y redirige a `/usuarios` (Post-Redirect-Get) con la alerta "Usuario creado correctamente" de tipo `success`. |
| **CP-UC-10** | Datos inválidos | Rol `ADMINISTRADOR`, `username` vacío, `nombre: "Carlos"`, `correo: "no-es-un-correo"`, sin `apellido` ni `rol` | `@Valid` $\rightarrow$ **Errores** en `username`, `apellido`, `correo` y `rol` | Vuelve al formulario. El panel resumen "No se pudo crear el usuario" lista los cuatro mensajes ("El nombre de usuario es obligatorio", "El apellido es obligatorio", "El correo electrónico no tiene un formato válido", "El rol es obligatorio") y cada campo marca su error. No invoca al service. |
| **CP-UC-11** | Username duplicado | Rol `ADMINISTRADOR`, datos válidos; el service lanza `RecursoDuplicadoException("Ya existe un usuario con el nombre de usuario 'carlos'")` | El service rechaza el alta | Vuelve al formulario con el error asociado al campo `username`, el mensaje visible y los datos tipeados conservados (ej. el apellido). |
| **CP-UC-12** | Regla de negocio del service | Rol `ADMINISTRADOR`, datos válidos; el service lanza `ReglaNegocioException("La contraseña es obligatoria")` | El service rechaza el alta | Vuelve al formulario y el panel resumen "No se pudo crear el usuario" muestra "La contraseña es obligatoria". |
| **CP-UC-13** | Rol no autorizado | Rol `GERENTE_DE_COMPRAS`, datos válidos | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-UC-14** | Sin sesión | Sin autenticación, con token CSRF | `anyRequest().authenticated()` $\rightarrow$ **Rechaza** | Redirige a `/login`. No invoca al service. |
| **CP-UC-35** | Correo electrónico duplicado | Rol `ADMINISTRADOR`, datos válidos; el service lanza `RecursoDuplicadoException("Ya existe un usuario con el correo electrónico 'carlos@mail.com'")` | El service rechaza el alta | Vuelve al formulario con el error asociado al campo `correo` y el mensaje visible. |
| **CP-UC-37** | Alta sin teléfono | Rol `ADMINISTRADOR`, datos válidos con `telefono` vacío | El teléfono es opcional: `@Valid` $\rightarrow$ **Sin errores** | Invoca `altaUsuario` y redirige a `/usuarios`. |
| **CP-UC-27** | Contraseña de solo espacios | Rol `ADMINISTRADOR`, datos válidos con `password` de 16 espacios | `@Pattern` de la contraseña $\rightarrow$ **Falla** | Vuelve al formulario con el error en el campo `password`; el panel resumen "No se pudo crear el usuario" muestra "no puede contener espacios en blanco". No invoca al service. |
| **CP-UC-28** | Contraseña corta o con espacios en el medio | Rol `ADMINISTRADOR`, datos válidos con `password: "abc123"` y, por separado, `"clave segura1"` | `@Pattern` de la contraseña $\rightarrow$ **Falla** en ambos | En cada caso vuelve al formulario con el error en el campo `password`. No invoca al service. |

---

### 4. `GET /usuarios/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-UC-15** | Formulario de edición con los datos actuales | Autenticado como `admin` (`ADMINISTRADOR`); `buscarPorId(7)` devuelve el usuario `carlos` | El usuario editado es otra cuenta | Responde 200 con la vista `usuario/usuario-form`, `usuarioId = 7` y `esCuentaPropia = false`. Muestra "Editar usuario" y "Guardar cambios", los campos precargados (`carlos`, `Gomez`), el marcador "Dejala vacía para conservar la actual" en la contraseña, `action="/usuarios/7"` y el selector de rol. |
| **CP-UC-16** | Edición de la cuenta propia | Autenticado como `admin`; `buscarPorId(1)` devuelve el usuario `admin` | El usuario editado coincide con el autenticado | `esCuentaPropia = true`. Muestra "No se puede cambiar el propio rol" y no ofrece el selector de rol. |
| **CP-UC-17** | Usuario inexistente | Rol `ADMINISTRADOR`; `buscarPorId(99)` lanza `RecursoNoEncontradoException("No se encontró el usuario con ID: 99")` | El `ControllerAdvices` traduce la excepción | Redirige con la alerta "No se encontró el usuario con ID: 99" de tipo `danger`. |

---

### 5. `POST /usuarios/{id}` — `modificarUsuario(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-UC-18** | Modificación con datos válidos _(Camino feliz)_ | Autenticado como `admin`, `id: 7` (otra cuenta), datos válidos | `@Valid` $\rightarrow$ **Sin errores** | Invoca `modificarUsuario(7, ...)` y redirige a `/usuarios` con la alerta "Usuario modificado correctamente" de tipo `success`. |
| **CP-UC-19** | Datos inválidos | Autenticado como `admin`, `id: 7`, `nombre` vacío | `@Valid` $\rightarrow$ **Error** en `nombre` | Vuelve al formulario en modo edición (`usuarioId = 7`). El panel resumen "No se pudo modificar el usuario" muestra "El nombre es obligatorio". No invoca al service. |
| **CP-UC-20** | Username en uso por otra cuenta | Autenticado como `admin`, `id: 7`, `username: "pedro"`; el service lanza `RecursoDuplicadoException("El nombre de usuario 'pedro' ya está en uso por otro usuario")` | El service rechaza la modificación | Vuelve al formulario con el error asociado al campo `username` y el mensaje visible. |
| **CP-UC-21** | Cambio del propio rol | Autenticado como `admin` sobre su propia cuenta (`id: 1`, rol actual `ADMINISTRADOR`), envía `rol: GERENTE_DE_COMPRAS` | Cuenta propia y rol distinto del actual | Vuelve al formulario con el error en el campo `rol` ("No se puede cambiar el propio rol"). No invoca al service. |
| **CP-UC-22** | Cambio del propio username | Autenticado como `admin` sobre su propia cuenta, envía `username: "admin2"` | Cuenta propia y username distinto del actual | Invoca `modificarUsuario`, cierra la sesión y redirige a `/login` con la alerta "Cambió su nombre de usuario: inicie sesión nuevamente" de tipo `info`. |
| **CP-UC-23** | Rol no autorizado | Rol `GERENTE_DE_COMPRAS`, datos válidos | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-UC-36** | Correo electrónico en uso por otra cuenta | Autenticado como `admin`, `id: 7`; el service lanza `RecursoDuplicadoException("El correo electrónico 'carlos@mail.com' ya está en uso por otro usuario")` | El service rechaza la modificación | Vuelve al formulario con el error asociado al campo `correo` y el mensaje visible. |
| **CP-UC-29** | Contraseña de solo espacios | Autenticado como `admin`, `id: 7`, `password` de 16 espacios | `@Pattern` de la contraseña $\rightarrow$ **Falla** | Vuelve al formulario en modo edición con el error en el campo `password` y el panel "No se pudo modificar el usuario". No invoca al service. |
| **CP-UC-30** | Contraseña vacía _(conserva la actual)_ | Autenticado como `admin`, `id: 7`, `password: ""`, resto válido | La contraseña vacía es válida en la modificación | Invoca `modificarUsuario(7, ...)` y redirige a `/usuarios`. |

---

### 6. `POST /usuarios/{id}/baja` — `bajaUsuario(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-UC-24** | Baja de otra cuenta _(Camino feliz)_ | Autenticado como `admin`; `buscarPorId(7)` devuelve el usuario `carlos` | El usuario dado de baja no es la cuenta propia | Invoca `bajaUsuario(7)` y redirige a `/usuarios` con la alerta "Usuario dado de baja correctamente" de tipo `success`. |
| **CP-UC-25** | Baja de la cuenta propia | Autenticado como `admin`; `buscarPorId(1)` devuelve el usuario `admin` | El usuario coincide con el autenticado | Redirige con la alerta "No se puede dar de baja el usuario porque es la cuenta con la sesión iniciada" de tipo `warning`. No invoca a `bajaUsuario`. |
| **CP-UC-26** | Rol no autorizado | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca a `bajaUsuario`. |
| **CP-UC-34** | El service rechaza la baja del único administrador activo | Autenticado como `admin`; `buscarPorId(7)` devuelve un usuario `carlos` con rol `ADMINISTRADOR`; `bajaUsuario(7)` lanza `ReglaNegocioException` | El service informa que es el único administrador activo | Redirige a la pantalla de origen con la alerta "No se puede dar de baja el usuario porque es el único administrador activo" de tipo `warning`. |
