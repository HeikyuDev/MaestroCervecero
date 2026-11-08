## Casos de prueba de `OperacionBarrilController`

**Tipo de prueba:** capa web (`@WebMvcTest`): formularios de registro, registro y anulación de las seis operaciones de barril; services simulados. Clase `OperacionBarrilControllerTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-OBC-01** | El operario registra una limpieza | El service recibe el barril de la ruta, la fecha y las observaciones; vuelve a la pestaña con la alerta de éxito. |
| **CP-OBC-02** | El operario registra falla, fraccionamiento y mantenimiento | Vuelve a la pestaña de cada uno; el fraccionamiento lleva la cantidad extraída. |
| **CP-OBC-03** | El encargado registra despacho y devolución | El despacho lleva cliente y fecha estimada de devolución; vuelve a la pestaña de cada uno. |
| **CP-OBC-04** | Formulario inválido | Redirige al formulario sin llamar al service; el GET siguiente muestra los errores y lo escrito. |
| **CP-OBC-05** | Rechazo del service | Un rechazo sobre un campo (por ejemplo la cantidad) se muestra en ese campo; uno general, en el resumen. |
| **CP-OBC-06** | Despacho rechazado | Vuelve al formulario con el cliente elegido, la fecha estimada y el error. |
| **CP-OBC-07** | Anulación de las seis operaciones | El service recibe el ID y el motivo; vuelve a la pestaña con la alerta de éxito. |
| **CP-OBC-08** | Anulación rechazada por el service | Vuelve al origen con su mensaje en una alerta warning. |
| **CP-OBC-09** | Anular una operación de otro barril | Responde como inexistente (alerta danger) y no llama a ningún service de anulación. |
| **CP-OBC-10** | Roles sin permisos de registrar y anular (gerente de producción, depósito, compras, administrador) | Rechazados en las seis operaciones, en el formulario, el POST y la anulación. |
| **CP-OBC-11** | Cruce operario y encargado | El operario no registra ni anula despachos ni devoluciones y el encargado no registra ni anula las otras cuatro. |
| **CP-OBC-12** | Formulario de limpieza | Título, bloque de información, fecha y observaciones en filas propias, Cancelar hacia la pestaña, novalidate. |
| **CP-OBC-13** | Formulario de fraccionamiento y de devolución | El fraccionamiento pide la cantidad con sufijo L antes de la fecha; la devolución avisa que se descarta el contenido. |
| **CP-OBC-14** | Formulario de despacho | Selector de cliente con su buscador y la opción "Crear nuevo cliente" en el menú de tres puntitos (alta contextual hacia `/clientes/nuevo`), y la fecha estimada en la fila de las fechas. |
| **CP-OBC-15** | Estado no permitido por URL | Redirige a la pestaña con el aviso del service; con el barril En uso, avisa que lo ocupa un lote. |
| **CP-OBC-16** | Sin token CSRF | Es rechazado y no llega al service. |
| **CP-OBC-17** | `idBarril` en el pedido | Un POST con `idBarril=999` en la ruta del barril 5 | El `@InitBinder` no enlaza el campo; el controller descarta su error de validación y fija el barril de la ruta | El service recibe el barril 5 en las seis operaciones; el pedido no puede inyectar otro barril. |
| **CP-OBC-18** | Volver de crear un cliente | `GET /despachos/nuevo?idCliente=3&desdeAlta=1` trae elegido al cliente creado (campo oculto `idCliente` y su nombre en el selector). |
