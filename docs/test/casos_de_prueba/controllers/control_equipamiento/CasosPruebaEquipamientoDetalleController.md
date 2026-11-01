## Casos de prueba de `EquipamientoDetalleController`

**Tipo de prueba:** capa web (`@WebMvcTest`): detalle de solo lectura de un equipamiento. Clase `EquipamientoDetalleControllerTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-EQD-01** | Detalle del equipamiento para el gerente y el operario | Muestra la identidad, solo los parámetros con valor y las fechas de mantenimiento. |
| **CP-EQD-02** | Roles sin permiso de consulta del equipamiento | No abren el detalle. |
| **CP-EQD-03** | Equipamiento inexistente o dado de baja | Vuelve al origen con alerta de error. |
