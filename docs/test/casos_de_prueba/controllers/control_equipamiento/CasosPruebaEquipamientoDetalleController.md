## Casos de prueba de `EquipamientoDetalleController`

**Tipo de prueba:** capa web (`@WebMvcTest`): detalle de solo lectura de un equipamiento. Clase `EquipamientoDetalleControllerTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-EQD-01** | el gerente y el operario ven el detalle con identidad, solo los parámetros con valor y las fechas de mantenimiento | el gerente y el operario ven el detalle con identidad, solo los parámetros con valor y las fechas de mantenimiento |
| **CP-EQD-02** | los demás roles no abren el detalle del equipamiento | los demás roles no abren el detalle del equipamiento |
| **CP-EQD-03** | un equipamiento inexistente o dado de baja vuelve al origen con alerta de error | un equipamiento inexistente o dado de baja vuelve al origen con alerta de error |
