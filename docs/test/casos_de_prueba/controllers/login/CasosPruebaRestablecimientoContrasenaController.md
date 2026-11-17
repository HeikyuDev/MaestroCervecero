## Casos de prueba de `RestablecimientoContrasenaController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `RestablecimientoContrasenaControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`), el `ControllerAdvices` y las plantillas Thymeleaf reales `login/recuperar-contrasena.html`, `login/restablecer-contrasena.html` y `login/enlace-invalido.html`; el `IRestablecimientoContrasenaServicio` y el `UserDetailServiceImpl` están simulados (mock) y no hay base de datos.

**Acceso:** `/recuperar-contrasena` y `/restablecer-contrasena` son públicos; no requieren autenticación. Los `POST` exigen el token CSRF.

**Reglas:**
- Pedir el enlace responde siempre el mismo aviso, exista o no el nombre de usuario y aunque el service falle: "Si el nombre de usuario existe, se envió un correo con las instrucciones para restablecer la contraseña".
- El token del enlace llega como parámetro `token`, aparte del formulario.
- Un enlace que no sirve muestra el motivo y ofrece pedir un enlace nuevo.

---

### 1. `GET /recuperar-contrasena` — `mostrarFormularioSolicitud(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-RCT-01** | Formulario para pedir el enlace | `GET /recuperar-contrasena` sin sesión | La pantalla es pública | Responde 200 con la vista `login/recuperar-contrasena` y el formulario `solicitudForm`. Muestra "Nombre de usuario" con el asterisco rojo de obligatorio, el placeholder "Ej: admin", el formulario con `novalidate` y el enlace a `/login`. |

---

### 2. `POST /recuperar-contrasena` — `solicitarRestablecimiento(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-RCT-02** | Pedido del enlace | `username: "jperez"`, con CSRF | El formulario es válido | Llama a `solicitarRestablecimiento("jperez")` y redirige a `/login` con `mensaje` = aviso neutral y `tipo = info`. |
| **CP-RCT-03** | Falla del service | `username: "jperez"`, con CSRF; el service lanza `IllegalStateException` | La falla no cambia la respuesta | Redirige a `/login` con el mismo aviso neutral y `tipo = info`. |
| **CP-RCT-04** | Nombre de usuario vacío | `username: "  "`, con CSRF | Falla `@NotBlank` | Responde 200 con la vista `login/recuperar-contrasena`, error en el campo `username` y el texto "El nombre de usuario es obligatorio". No llama al service. |
| **CP-RCT-05** | Sin token CSRF | `username: "jperez"`, sin CSRF y sin sesión | Spring Security rechaza el pedido | Redirige a `/login?timeout`. No llama al service. |

---

### 3. `GET /restablecer-contrasena` — `mostrarFormularioRestablecimiento(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-RCT-06** | Enlace que sirve | `token` vigente | `validarEnlace(token)` no lanza excepción | Responde 200 con la vista `login/restablecer-contrasena`, el formulario `restablecimientoForm` y el atributo `token`. La pantalla tiene el campo oculto `token`, el formulario con `novalidate`, `<meta name="referrer" content="no-referrer">`, los botones del ojo de `password` y `confirmacionPassword` (`aria-label="Mostrar contraseña"`), el script `/js/mostrar-contrasena.js` y las reglas de `MetodosPassword`. |
| **CP-RCT-07** | Enlace que no sirve | `token`; `validarEnlace` lanza `EnlaceRestablecimientoInvalidoException("El enlace para restablecer la contraseña ya fue utilizado")` | El enlace no sirve | Responde 200 con la vista `login/enlace-invalido`, `mensaje` = motivo y `tipo = warning`. La pantalla muestra el motivo y "Solicitar un enlace nuevo" con enlace a `/recuperar-contrasena`, y no contiene el token. |

---

### 4. `POST /restablecer-contrasena` — `restablecerContrasena(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-RCT-08** | Contraseña restablecida | `token`, `password` y `confirmacionPassword`: `"NuevaClave123"`, con CSRF | El formulario es válido y el service no rechaza | Llama a `restablecerContrasena(token, formulario)` con la contraseña y la confirmación recibidas, y redirige a `/login` con `mensaje = "La contraseña se restableció correctamente: inicie sesión con la contraseña nueva"` y `tipo = success`. |
| **CP-RCT-09** | Campos vacíos | `token`, `password: ""`, `confirmacionPassword: ""`, con CSRF | Fallan los `@NotBlank` | Responde 200 con la vista `login/restablecer-contrasena`, errores en `password` y `confirmacionPassword`, el atributo `token` y los textos "La contraseña es obligatoria" y "La confirmación de la contraseña es obligatoria". No llama al service. |
| **CP-RCT-10** | Contraseña fuera de las reglas | `token`, `password` y confirmación `"corta"`, con CSRF | Falla el `@Pattern` de `MetodosPassword` | Responde 200 con error en `password` y el texto de `MetodosPassword.MENSAJE_INVALIDA`; la contraseña no se vuelve a mostrar en la pantalla. No llama al service. |
| **CP-RCT-11** | Regla rechazada por el service | `password: "NuevaClave123"`, `confirmacionPassword: "OtraClave123"`; el service lanza `ReglaNegocioException("La confirmación no coincide con la contraseña", "confirmacionPassword")` | El rechazo indica el campo | Responde 200 con la vista `login/restablecer-contrasena`, error `reglaNegocio` en `confirmacionPassword` y el texto del rechazo. |
| **CP-RCT-12** | Enlace que dejó de servir | Formulario válido; el service lanza `EnlaceRestablecimientoInvalidoException("El enlace para restablecer la contraseña venció o fue reemplazado por uno más nuevo")` | El enlace venció entre la apertura y el envío | Responde 200 con la vista `login/enlace-invalido`, el motivo y el enlace a `/recuperar-contrasena`. |
| **CP-RCT-13** | Sin token CSRF | Formulario válido, sin CSRF y sin sesión | Spring Security rechaza el pedido | Redirige a `/login?timeout`. No llama al service. |
