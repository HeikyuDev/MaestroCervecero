## Casos de prueba de `CierreSesionesServicioImpl`

**Tipo de prueba:** unitarias (clase `CierreSesionesServicioImplTest`), sin contexto de Spring. El `SessionRegistry` está simulado (mock); las sesiones son `SessionInformation` reales.

**Reglas:** cierra todas las sesiones activas del usuario indicado haciéndolas vencer (`expireNow`): su próximo pedido vuelve a la pantalla de inicio de sesión. Reconoce el principal como `UserDetails` (el que registra el inicio de sesión) o como texto. Las sesiones de los demás usuarios no se tocan.

---

### 1. `cerrarSesionesDe(String username)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CSS-01** | Cierra solo las sesiones del usuario | `username: "jperez"`; el registro tiene los principales `mlopez` y `jperez` (`UserDetails`), con dos sesiones de `jperez` | El principal coincide por nombre de usuario | Las dos sesiones de `jperez` quedan vencidas; no se consultan las sesiones de `mlopez`. |
| **CP-CSS-02** | Principal registrado como texto | `username: "jperez"`; el registro tiene el principal `"jperez"` (texto) con una sesión | El principal de texto coincide | La sesión queda vencida. |
| **CP-CSS-03** | Usuario sin sesiones | `username: "jperez"`; el registro solo tiene el principal `mlopez` | Ningún principal coincide | No consulta las sesiones de ningún principal. |
| **CP-CSS-04** | Nombre de usuario nulo | `username: null` | No hay usuario que buscar | No consulta el registro de sesiones. |
