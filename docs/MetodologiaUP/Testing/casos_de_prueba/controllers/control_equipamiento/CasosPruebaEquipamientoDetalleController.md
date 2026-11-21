## Casos de prueba de `EquipamientoDetalleController`

**Tipo de prueba:** capa web (`@WebMvcTest`): detalle de solo lectura de un equipamiento. Clase `EquipamientoDetalleControllerTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-EQD-01** | Detalle del equipamiento para el gerente y el operario | Muestra la identidad, solo los parámetros con valor y las fechas de mantenimiento. |
| **CP-EQD-02** | Roles sin permiso de consulta del equipamiento | No abren el detalle. |
| **CP-EQD-03** | Equipamiento inexistente | Vuelve al origen con alerta de error. |
| **CP-EQD-04** | Detalle para el gerente de producción | Pertenece a la Gestión de Equipamientos: resalta su opción del menú, no la de Control, y Volver lleva a la lista del tipo del equipo. |
| **CP-EQD-05** | Detalle para el operario | Igual que para el gerente: resalta Gestión de Equipamientos y Volver lleva a la lista del tipo del equipo, porque el operario consulta esa gestión. |
| **CP-EQD-06** | Detalle de un equipamiento dado de baja | Se abre (las pantallas de lote enlazan a él) y muestra la marca "Dado de baja" en la cabecera y en la información. |
| **CP-EQD-07** | Detalle de un equipamiento activo | No muestra la marca "Dado de baja". |
