## Casos de prueba de `CierreSesionesServicioImpl`

**Tipo de prueba:** unitarias (clase `CierreSesionesServicioImplTest`), sin contexto de Spring. El `SessionRegistry` y `SesionesCerradasPorRestablecimiento` están simulados (mock); las sesiones son `SessionInformation` reales.

**Reglas:** cierra todas las sesiones activas del usuario indicado haciéndolas vencer (`expireNow`) y marca cada una en `SesionesCerradasPorRestablecimiento`: su próximo pedido vuelve a la pantalla de inicio de sesión con el aviso "Su contraseña fue restablecida, inicie sesión nuevamente". Reconoce el principal como `UserDetails` (el que registra el inicio de sesión) o como texto. Las sesiones de los demás usuarios no se tocan.

---

### 1. `cerrarSesionesDe(String username)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CSS-01** | Cierra solo las sesiones del usuario | `username: "jperez"`; el registro tiene los principales `mlopez` y `jperez` (`UserDetails`), con dos sesiones de `jperez` | El principal coincide por nombre de usuario | Las dos sesiones de `jperez` quedan vencidas y marcadas (`s1` y `s2`); no se consultan las sesiones de `mlopez`. |
| **CP-CSS-02** | Principal registrado como texto | `username: "jperez"`; el registro tiene el principal `"jperez"` (texto) con una sesión | El principal de texto coincide | La sesión queda vencida y marcada (`s1`). |
| **CP-CSS-03** | Usuario sin sesiones | `username: "jperez"`; el registro solo tiene el principal `mlopez` | Ningún principal coincide | No consulta las sesiones de ningún principal ni marca ninguna sesión. |
| **CP-CSS-04** | Nombre de usuario nulo | `username: null` | No hay usuario que buscar | No consulta el registro de sesiones ni marca ninguna sesión. |
