## Casos de prueba de `OperacionEquipamientoController`

**Tipo de prueba:** capa web (`@WebMvcTest`): formularios de registro, registro y anulación de limpiezas, fallas y mantenimientos; services simulados. Clase `OperacionEquipamientoControllerTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-OPE-01** | Registrar una limpieza | El service recibe el equipamiento de la ruta, la fecha y las observaciones, y se vuelve a la pestaña con la alerta de éxito. |
| **CP-OPE-02** | Registrar una falla y un mantenimiento | Cada registro vuelve a la pestaña de su tipo. |
| **CP-OPE-03** | Formulario inválido | Vuelve al formulario de registro por redirect sin llamar al service, y el GET siguiente muestra los errores y lo escrito. |
| **CP-OPE-04** | Rechazo del service al registrar | Un rechazo sobre un campo se muestra en ese campo y uno general (estado del equipo) en el resumen, en el formulario. |
| **CP-OPE-05** | Anular una operación con motivo | El service recibe el ID y el motivo y se vuelve a la pestaña con la alerta de éxito. |
| **CP-OPE-06** | Rechazo del service al anular | Vuelve al origen con su mensaje en una alerta warning. |
| **CP-OPE-07** | Roles sin permisos de registrar y anular | Incluido el gerente de producción, son rechazados y no llegan a ningún service. |
| **CP-OPE-08** | Registrar sin token CSRF | Es rechazado y no llega al service. |
| **CP-OPE-09** | Anular una operación de otro equipamiento | Responde como inexistente (alerta de peligro) y no llama al service de anulación. |
| **CP-OPE-10** | Formulario de registro de una limpieza | Es una página con el título, el bloque de información del equipo, la fecha y hora y las observaciones en filas propias, Cancelar hacia la pestaña y "Registrar limpieza", y es novalidate. |
| **CP-OPE-11** | Formulario con un estado que no permite la operación | Vuelve a la pestaña con el aviso del service; el formulario del mantenimiento está en `/mantenimientos/nuevo`. |
| **CP-OPE-12** | Formularios para roles sin permiso de registrar | Incluido el gerente de producción, no acceden a los formularios y no se consulta el equipamiento. |
