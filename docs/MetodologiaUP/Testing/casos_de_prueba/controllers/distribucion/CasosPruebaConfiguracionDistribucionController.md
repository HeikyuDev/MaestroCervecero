## Casos de prueba de `ConfiguracionDistribucionController`

**Tipo de prueba:** unitaria de la capa web (`@WebMvcTest`, clase `ConfiguracionDistribucionControllerTest`) con `SecurityConfig` y `ControllerAdvices`, y el service simulado. La pantalla `/configuracion-distribucion` es del `ENCARGADO_DE_DISTRIBUCION` (`hasRole`).

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CDC-01** | Pantalla con la configuración vigente | Rol `ENCARGADO_DE_DISTRIBUCION` con sus permisos; configuración 09:00 y 7 días | `GET /configuracion-distribucion` | Vista `configuracion/configuracion-distribucion` con el título "Configuración de distribución - Maestro Cervecero", el formulario `novalidate`, la sección "Retiro de barriles", los dos campos con asterisco rojo, la hora en un campo `time` con "09:00", los días con "7" y la unidad "días", los placeholders "Ej: 09:00" y "Ej: 7", la ayuda "Un número entero entre 1 y 30" y la sección "Configuración" de la sidebar con el enlace a la pantalla. |
| **CP-CDC-02** | Guardado _(Camino feliz)_ | Hora 10:30 y 14 días | `POST` válido | Delega en el service la hora 10:30 y 14 días y redirige a `/configuracion-distribucion` con "Configuración de distribución actualizada correctamente" (`success`). |
| **CP-CDC-03** | Datos vacíos | Hora y días vacíos | Bean Validation | Vuelve al formulario con "No se pudo guardar la configuración", "La hora de envío del recordatorio es obligatoria" y "Los días máximos de anticipación del retiro son obligatorios", sin llamar al service. |
| **CP-CDC-04** | Días fuera de rango | Días 0 y 31, por separado | `@Min(1)` y `@Max(30)` | Vuelve al formulario con "Los días máximos de anticipación del retiro deben estar entre 1 y 30", sin llamar al service. |
| **CP-CDC-05** | Regla rechazada por el service | El service lanza `ReglaNegocioException` | Error global | Vuelve al formulario con el motivo y lo tipeado (08:15 y 5). |
| **CP-CDC-06** | Otros roles | `GERENTE_DE_PRODUCCION`, `GERENTE_DE_COMPRAS`, `ENCARGADO_DE_DEPOSITO`, `ADMINISTRADOR` y `OPERARIO_DE_PRODUCCION` | `GET` y `POST` | Se redirige con la alerta de permisos y no se actualiza nada. |
