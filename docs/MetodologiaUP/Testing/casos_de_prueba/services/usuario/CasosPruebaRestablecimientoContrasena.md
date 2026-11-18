## Casos de prueba de `RestablecimientoContrasenaServicioImpl`

**Tipo de prueba:** unitarias (clase `RestablecimientoContrasenaServicioImplTest`), sin base de datos. Los repositorios, el `PasswordEncoder`, el `ICorreoServicio`, el `IAuditLogService` y el `ICierreSesionesServicio` están simulados (mock). El servicio se construye con `app.url-base = http://localhost:7777`, 30 minutos de vigencia y 2 minutos entre solicitudes.

**Reglas:**
- El token del enlace son 32 bytes aleatorios en Base64URL sin relleno (43 caracteres). Solo se guarda su hash SHA-256 en hexadecimal.
- La solicitud no informa si el nombre de usuario existe: corre en un hilo aparte (`@Async`), y con un usuario inexistente no lanza excepciones, no guarda nada, no audita y no envía correo. La IP del pedido llega como parámetro, resuelta en el hilo de la request.
- Si al usuario se le envió un enlace hace menos de 2 minutos, la solicitud no hace nada.
- Un enlace nuevo hace vencer los enlaces sin usar anteriores del usuario. Un enlace sirve mientras no se usó, no venció y su usuario está activo.
- La contraseña nueva cumple las reglas de `MetodosPassword` y la confirmación coincide con ella; cada rechazo indica el campo del formulario.
- Validada la contraseña, el enlace se reclama con una única sentencia condicional (`reclamarParaUso`) antes de cambiar la contraseña: si otro pedido lo usó o venció en el medio, no se cambia nada y el motivo se decide leyendo de la base su fecha de uso.
- Confirmada la transacción, se cierran las sesiones abiertas del usuario.
- El correo y la bitácora se despachan al confirmarse la transacción (en las pruebas, sin transacción, en el momento).

---

### 1. `solicitarRestablecimiento(String username, String ipCliente)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-SRC-01** | Envía el enlace y guarda solo el hash | `username: "jperez"`, `ipCliente: "10.0.0.1"`, usuario activo id 1 con correo `juan@mail.com`, sin enlaces en los últimos 2 minutos | Existe el usuario y puede recibir otro enlace | Vence los enlaces vigentes del usuario; guarda un enlace del usuario sin usar, con vencimiento a los 30 minutos y como hash el SHA-256 del token del enlace (distinto del token). Envía a `juan@mail.com` el asunto "Restablecimiento de contraseña de Maestro Cervecero" con la plantilla `correo/restablecer-contrasena` y las variables `nombre = "Juan"`, `minutosVigencia = 30` y `enlace = "http://localhost:7777/restablecer-contrasena?token=<token de 43 caracteres Base64URL>"`. Consulta los enlaces enviados desde hace 2 minutos. |
| **CP-SRC-02** | Audita la solicitud | Mismo escenario que CP-SRC-01 | Se registró un enlace nuevo | Guarda en la bitácora una entrada con `username = "jperez"`, acción `SOLICITUD_RESTABLECIMIENTO_CONTRASENA`, concepto `SESION`, `entidadId = "1"`, fecha e IP `10.0.0.1` (la recibida). |
| **CP-SRC-03** | Normaliza el nombre de usuario | `username: "  JPerez "` | El nombre de usuario se normaliza como al guardarlo | Busca el usuario activo `"jperez"`. |
| **CP-SRC-04** | Nombre de usuario inexistente | `username: "inexistente"`, sin usuario activo con ese nombre | El usuario no existe | No lanza excepción; no usa el repositorio de enlaces, no envía correo y no audita. |
| **CP-SRC-05** | Nombre de usuario nulo o en blanco | `username: null` y `username: "   "` | No hay nombre de usuario que buscar | No consulta ningún repositorio, no envía correo y no audita. |
| **CP-SRC-06** | Enlace enviado hace menos de 2 minutos | `username: "jperez"`, el usuario tiene un enlace creado después de hace 2 minutos | Se limita la frecuencia de envío | No lanza excepción; no vence enlaces, no guarda uno nuevo, no envía correo y no audita. |
| **CP-SRC-07** | Cada solicitud genera un token distinto | Dos solicitudes seguidas de `"jperez"` sin enlaces recientes | El token es aleatorio | Los dos enlaces guardados tienen hashes distintos. |
| **CP-SRC-08** | Solicitud asíncrona | Método `solicitarRestablecimiento(String, String)` de la clase | El método tiene la anotación `@Async` | La anotación está presente: la request no espera el trabajo de la solicitud, sea cual sea el nombre de usuario. |

---

### 2. `validarEnlace(String token)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VEC-01** | Enlace vigente | Token cuyo hash SHA-256 corresponde a un enlace sin usar, que vence en 10 minutos, de un usuario activo | El enlace sirve | No lanza excepción. El enlace se busca por el hash del token. |
| **CP-VEC-02** | Token nulo, en blanco o demasiado largo | `token: null`, `"  "` y un texto de 129 caracteres | No hay token válido que buscar | Lanza `EnlaceRestablecimientoInvalidoException` con "El enlace para restablecer la contraseña no es válido" sin consultar el repositorio. |
| **CP-VEC-03** | Token desconocido | Ningún enlace tiene el hash del token | El enlace no existe | Lanza `EnlaceRestablecimientoInvalidoException` con "El enlace para restablecer la contraseña no es válido". |
| **CP-VEC-04** | Enlace ya usado | Enlace con fecha de uso | El enlace ya se usó | Lanza `EnlaceRestablecimientoInvalidoException` con "El enlace para restablecer la contraseña ya fue utilizado". |
| **CP-VEC-05** | Enlace vencido o reemplazado | Enlace sin usar que venció hace 1 segundo | El enlace venció | Lanza `EnlaceRestablecimientoInvalidoException` con "El enlace para restablecer la contraseña venció o fue reemplazado por uno más nuevo". |
| **CP-VEC-06** | Usuario dado de baja | Enlace vigente de un usuario en estado `BAJA` | El usuario ya no está activo | Lanza `EnlaceRestablecimientoInvalidoException` con "El enlace para restablecer la contraseña no es válido". |

---

### 3. `restablecerContrasena(String token, RestablecimientoContrasenaFormDTO formDTO)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-RCC-01** | Restablece la contraseña | Enlace vigente id 10 del usuario activo id 1 `"jperez"`; `password` y `confirmacionPassword`: `"NuevaClave123"`; el reclamo del enlace afecta 1 fila | El enlace sirve, la contraseña cumple las reglas y el reclamo gana | En este orden: reclama el enlace 10 con la fecha actual, codifica la contraseña, guarda el usuario con el hash nuevo y vence los demás enlaces vigentes del usuario. No vuelve a guardar el enlace. La bitácora registra `username = "jperez"`, acción `RESTABLECIMIENTO_CONTRASENA`, concepto `SESION`, `entidadId = "1"`, y se cierran las sesiones de `"jperez"`. |
| **CP-RCC-02** | Enlace que no sirve | Enlace ya usado; contraseña válida | El enlace se valida antes que la contraseña | Lanza `EnlaceRestablecimientoInvalidoException` con "El enlace para restablecer la contraseña ya fue utilizado"; no reclama el enlace, no codifica, no guarda, no audita y no cierra sesiones. |
| **CP-RCC-03** | Contraseña vacía | Enlace vigente; `password` y `confirmacionPassword`: `""` | La contraseña es obligatoria | Lanza `ReglaNegocioException` con "La contraseña es obligatoria" y campo `password`; no reclama el enlace, no codifica ni guarda. |
| **CP-RCC-04** | Contraseña fuera de las reglas | Enlace vigente; `password` y confirmación `"corta"` y `"con espacio1"` | La contraseña no cumple `MetodosPassword` | Lanza `ReglaNegocioException` con `MetodosPassword.MENSAJE_INVALIDA` y campo `password`; no reclama el enlace, no codifica ni guarda. |
| **CP-RCC-05** | Contraseña de más de 72 bytes | Enlace vigente; `password` y confirmación de 40 caracteres `"ñ"` (80 bytes) | Supera el límite de BCrypt | Lanza `ReglaNegocioException` con un mensaje que empieza por "La contraseña es demasiado larga" y campo `password`; no reclama el enlace, no codifica ni guarda. |
| **CP-RCC-06** | Confirmación distinta | Enlace vigente; `password: "NuevaClave123"`, `confirmacionPassword: "OtraClave123"` | La confirmación no coincide | Lanza `ReglaNegocioException` con "La confirmación no coincide con la contraseña" y campo `confirmacionPassword`; no reclama el enlace, no codifica, no guarda y no audita. |
| **CP-RCC-07** | Otro pedido usó el enlace en el medio | Enlace vigente al validarlo; el reclamo afecta 0 filas y la base informa fecha de uso | Dos pedidos simultáneos con el mismo enlace: este perdió | Lanza `EnlaceRestablecimientoInvalidoException` con "El enlace para restablecer la contraseña ya fue utilizado"; no codifica, no guarda, no vence enlaces, no audita y no cierra sesiones. |
| **CP-RCC-08** | El enlace venció o fue reemplazado en el medio | Enlace vigente al validarlo; el reclamo afecta 0 filas y la base no informa fecha de uso | El enlace dejó de servir antes del reclamo | Lanza `EnlaceRestablecimientoInvalidoException` con "El enlace para restablecer la contraseña venció o fue reemplazado por uno más nuevo"; no codifica, no guarda, no audita y no cierra sesiones. |
