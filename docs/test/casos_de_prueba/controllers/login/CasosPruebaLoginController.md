## Casos de prueba de `LoginController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `LoginControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`), el `ControllerAdvices` y la plantilla Thymeleaf real `login.html`; el `UserDetailServiceImpl` está simulado (mock) y no hay base de datos. Verifican cómo se traducen a avisos los parámetros de query que agrega Spring Security al redirigir a `/login`.

**Acceso:** `/login` es público; no requiere autenticación.

**Parámetros de query:**
- `error`: credenciales incorrectas.
- `logout`: cierre de sesión correcto.
- `timeout`: la cookie de sesión ya no es válida (`invalidSessionUrl` de `SecurityConfig`).
- `expired`: la sesión fue desplazada por un inicio de sesión posterior del mismo usuario (`maximumSessions(1)` con `expiredUrl` de `SecurityConfig`).
- `mensaje` y `tipo`: aviso explícito que muestra la pantalla tal cual.

---

### 1. `GET /login` — `loginPage(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LGC-01** | Pantalla sin avisos | `GET /login` sin parámetros | La pantalla es pública y no hay nada que avisar | Responde 200 con la vista `login`. Muestra "Iniciar sesión" y "Nombre de usuario"; no muestra ningún aviso ("Su sesión...", "Sesión cerrada correctamente", "Usuario o contraseña incorrectos"). |
| **CP-LGC-02** | Credenciales incorrectas | Parámetro `error` | El inicio de sesión falló | Muestra "Usuario o contraseña incorrectos". |
| **CP-LGC-03** | Sesión cerrada | Parámetro `logout` | El usuario cerró sesión | Muestra "Sesión cerrada correctamente". |
| **CP-LGC-04** | Sesión vencida por inactividad | Parámetro `timeout` | La cookie de sesión ya no es válida | El modelo tiene `mensaje = "Su sesión venció por inactividad: inicie sesión nuevamente"` y `tipo = warning`; la pantalla muestra el aviso con el ícono de advertencia. |
| **CP-LGC-05** | Sesión cerrada por otro inicio de sesión | Parámetro `expired` | La sesión fue desplazada por un inicio de sesión posterior | El modelo tiene `mensaje = "Su sesión se cerró porque se inició en otro dispositivo"` y `tipo = warning`; la pantalla muestra el aviso. |
| **CP-LGC-06** | Parámetros de texto ignorados | Parámetros `mensaje = "Comuniquese al 0800-FALSO"` y `tipo = danger` | La pantalla pública no refleja texto recibido por la URL | El modelo no tiene `mensaje` ni `tipo`; la pantalla no muestra "Comuniquese al 0800-FALSO". |
| **CP-LGC-07** | Aviso flash de otra pantalla | Parámetro `timeout` y atributos flash `mensaje = "Cambió su nombre de usuario: inicie sesión nuevamente"` y `tipo = info` | Un aviso enviado por otra pantalla prevalece sobre los avisos de sesión | La pantalla muestra "Cambió su nombre de usuario: inicie sesión nuevamente" y no muestra "Su sesión venció por inactividad". |
