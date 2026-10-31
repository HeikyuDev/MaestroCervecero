## Casos de prueba de `OperacionEquipamientoController`

**Tipo de prueba:** capa web (`@WebMvcTest`): registro y anulación de limpiezas, fallas y mantenimientos desde el panel; services simulados. Clase `OperacionEquipamientoControllerTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-OPE-01** | el operario registra una limpieza: el service recibe el equipamiento de la ruta, las fechas y las observaciones, y vuelve a la pestaña con la alerta de éxito | el operario registra una limpieza: el service recibe el equipamiento de la ruta, las fechas y las observaciones, y vuelve a la pestaña con la alerta de éxito |
| **CP-OPE-02** | el operario registra una falla y un mantenimiento y vuelve a la pestaña de cada uno | el operario registra una falla y un mantenimiento y vuelve a la pestaña de cada uno |
| **CP-OPE-03** | un formulario inválido vuelve a la pestaña por redirect sin llamar al service, y el GET siguiente muestra los errores y lo escrito | un formulario inválido vuelve a la pestaña por redirect sin llamar al service, y el GET siguiente muestra los errores y lo escrito |
| **CP-OPE-04** | un rechazo del service sobre un campo se muestra en ese campo, y uno general (estado del equipo) en el resumen | un rechazo del service sobre un campo se muestra en ese campo, y uno general (estado del equipo) en el resumen |
| **CP-OPE-05** | el operario anula una operación con motivo: el service recibe el ID y el motivo y vuelve a la pestaña con la alerta de éxito | el operario anula una operación con motivo: el service recibe el ID y el motivo y vuelve a la pestaña con la alerta de éxito |
| **CP-OPE-06** | si el service rechaza la anulación vuelve al origen con su mensaje en una alerta warning | si el service rechaza la anulación vuelve al origen con su mensaje en una alerta warning |
| **CP-OPE-09** | anular una operación que no es del equipamiento de la ruta responde como inexistente (alerta danger) y no llama al service de anulación | anular una operación que no es del equipamiento de la ruta responde como inexistente (alerta danger) y no llama al service de anulación |
| **CP-OPE-07** | los roles sin permisos de registrar y anular (incluido el gerente de producción) son rechazados y no llegan a ningún service | los roles sin permisos de registrar y anular (incluido el gerente de producción) son rechazados y no llegan a ningún service |
| **CP-OPE-08** | registrar sin token CSRF es rechazado y no llega al service | registrar sin token CSRF es rechazado y no llega al service |
