## Casos de prueba del destino de una sesión vencida

**Tipo de prueba:** unitarias (clases `SesionesCerradasPorRestablecimientoTest` y `EstrategiaSesionVencidaTest`), sin contexto de Spring.

**Reglas:** `CierreSesionesServicioImpl` marca en `SesionesCerradasPorRestablecimiento` cada sesión que cierra por un restablecimiento de contraseña. Cuando una sesión vencida vuelve a usarse, `EstrategiaSesionVencida` consume su marca: con marca va a `/login?restablecida`; sin marca (desplazada por un inicio de sesión posterior del mismo usuario) va a `/login?expired`. Cada marca se reconoce una sola vez y dura lo mismo que una sesión inactiva (`server.servlet.session.timeout`, 30 minutos); marcar otra sesión descarta las marcas vencidas.

---

### 1. `SesionesCerradasPorRestablecimiento`

Retención de 30 minutos y reloj fijo o controlado por la prueba.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-SCR-01** | Marca consumida una sola vez | `marcar("s1")`; dos veces `consumir("s1")` | La marca se quita al reconocerla | La primera consulta devuelve `true` y la segunda `false`. |
| **CP-SCR-02** | Sesión no marcada o ID nulo | `marcar("s1")`; `consumir("s2")` y `consumir(null)` | No hay marca para esas sesiones | Ambas consultas devuelven `false`. |
| **CP-SCR-03** | Marca vencida | `marcar("s1")`; pasan 30 minutos y 1 segundo; `consumir("s1")` | La marca superó el tiempo de inactividad | Devuelve `false`. |
| **CP-SCR-04** | Limpieza de marcas vencidas | `marcar("vieja")`; 20 minutos después `marcar("reciente")`; 11 minutos después `marcar("nueva")` | Marcar descarta las marcas de más de 30 minutos | `consumir("vieja")` devuelve `false`; `consumir("reciente")` y `consumir("nueva")` devuelven `true`. |

---

### 2. `EstrategiaSesionVencida.onExpiredSessionDetected(SessionInformationExpiredEvent event)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-ESV-01** | Sesión cerrada por restablecimiento | Sesión vencida `s1`, marcada | La sesión tiene marca | Redirige a `/login?restablecida`. |
| **CP-ESV-02** | Sesión desplazada por otro inicio de sesión | Sesión vencida `s1`, sin marca (solo está marcada `otra`) | La sesión no tiene marca | Redirige a `/login?expired`, el mismo destino de siempre para este caso. |
