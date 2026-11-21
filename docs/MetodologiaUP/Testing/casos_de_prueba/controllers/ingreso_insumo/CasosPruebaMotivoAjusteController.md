## Casos de prueba de `MotivoAjusteController`

**Tipo de prueba:** capa web (`@WebMvcTest`): lista, alta, modificación y baja de motivos de ajuste en `/configuracion-produccion/motivos-ajuste`; service simulado. Clase `MotivoAjusteControllerTest`. Las consultas JPQL de los repositorios no se ejercitan aquí.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-CMA-01** | Lista | Muestra el título de Configuración de Producción, la pestaña Motivos de ajuste activa, las columnas, el tipo como Ingreso o Egreso, los botones de alta, edición y baja, los filtros con ejemplo y el diálogo de baja. |
| **CP-CMA-02** | Criterios de la lista | El nombre sin espacios y el tipo se pasan al service, de a 20 y ordenados por nombre sin distinguir mayúsculas, y se conservan en el formulario. |
| **CP-CMA-03** | Lista sin criterios | No se restringe por ninguno, un nombre en blanco equivale a no filtrar, una página negativa se acota a la primera y un tipo inexistente redirige con una alerta de advertencia. |
| **CP-CMA-04** | Lista vacía | Muestra "No se encontraron motivos de ajuste con los criterios indicados". |
| **CP-CMA-05** | Paginación | El pie informa el total y los enlaces conservan nombre y tipo. |
| **CP-CMA-06** | Roles sin permiso | Los demás roles redirigen con la alerta de permisos en todas las rutas (GET y POST) y no se invoca el service. |
| **CP-CMA-07** | Anotaciones de permiso | Cada método exige su permiso: consultar, crear, modificar o eliminar. |
| **CP-CMA-08** | Pestaña | Solo el gerente de producción tiene MOTIVO_AJUSTE_CONSULTAR, el permiso que guarda la pestaña. |
| **CP-CMA-09** | Formulario de alta | Nombre y Tipo de ajuste con asterisco, ejemplos, ayuda de qué hace cada tipo sobre el stock, selector habilitado y botón Crear. |
| **CP-CMA-10** | Alta válida | Llama al service con los datos y redirige a la lista con la alerta de éxito. |
| **CP-CMA-11** | Alta sin datos | Vuelve al formulario con los errores de nombre y de tipo en su campo y en el resumen, y no llama al service. |
| **CP-CMA-12** | Errores del service en el alta | Un nombre duplicado se marca en el campo nombre y una regla sobre el tipo en el campo tipoAjuste; se conserva lo escrito. |
| **CP-CMA-13** | Edición sin ajustes | Carga los datos y deja el selector del tipo habilitado. |
| **CP-CMA-14** | Edición con ajustes | El selector del tipo va deshabilitado, el tipo real viaja en un campo oculto y se explica el bloqueo con "No se puede cambiar porque el motivo ya tiene ajustes.". |
| **CP-CMA-15** | Modificación válida | Llama al service con el ID y redirige a la lista con la alerta de éxito. |
| **CP-CMA-16** | Cambio de tipo rechazado | Vuelve al formulario con el error en el campo tipoAjuste, el selector sigue deshabilitado y se conserva lo escrito. |
| **CP-CMA-17** | Duplicado e inexistente en la modificación | El nombre duplicado se marca en su campo y un motivo inexistente redirige con alerta de error. |
| **CP-CMA-18** | Baja válida | Llama al service y redirige a la lista con la alerta de éxito. |
| **CP-CMA-20** | Baja de un motivo inexistente | Redirige con alerta de error. |
