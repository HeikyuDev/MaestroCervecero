## Casos de prueba de `ControlBarrilController`

**Tipo de prueba:** capa web (`@WebMvcTest`): lista de control, historial del barril, panel con las pestañas por rol y detalle de una operación; services simulados. Clase `ControlBarrilControllerTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-CBC-01** | Lista para el operario | Muestra las columnas del barril y el botón Abrir en cada fila, y no el ojo del historial. |
| **CP-CBC-02** | Lista para el gerente de producción | Muestra el ojo del historial y el botón Abrir. |
| **CP-CBC-03** | Lista para el encargado de distribución | Muestra el botón Abrir y no el ojo. |
| **CP-CBC-04** | Roles sin permisos de barril (encargado de depósito, gerente de compras, administrador) | La lista, el historial, el panel y el detalle redirigen con alerta de peligro y no se invoca el service. |
| **CP-CBC-05** | Criterios de la lista | El identificador sin espacios, el fabricante y el estado operativo se pasan al service, de a 20, y se conservan en el formulario. |
| **CP-CBC-06** | Estado inválido o fabricante inexistente | No filtran; el estado inválido se avisa con una alerta warning. |
| **CP-CBC-07** | Historial del gerente | Muestra el bloque de información, los filtros y una fila por actividad con su estado resultante, el ojo al lote o a la operación y el botón i de observaciones. |
| **CP-CBC-08** | Historial para el operario y el encargado | Redirige con alerta de peligro y no se invoca el service. |
| **CP-CBC-09** | Historial vacío | Sin actividad muestra "El barril no tiene operaciones registradas" y con filtros, "No se encontraron resultados". |
| **CP-CBC-10** | Filtros del historial | El tipo y las fechas se pasan al service (la fecha hasta incluye todo su día) y se conservan en el formulario. |
| **CP-CBC-11** | Tipo inválido o rango invertido | El tipo inválido se avisa y no filtra; el rango invertido muestra el aviso del service con la lista vacía. |
| **CP-CBC-12** | Pestañas del operario | Ve Limpiezas, Fallas, Fraccionamientos y Mantenimientos, y no Despachos ni Devoluciones. |
| **CP-CBC-13** | Pestañas del encargado | Ve solo Despachos y Devoluciones, con Despachos activa. |
| **CP-CBC-14** | Pestaña no permitida | El encargado que pide Limpiezas ve la primera de las suyas. |
| **CP-CBC-15** | Panel del gerente | Ve las seis pestañas en modo consulta, sin botón de alta, de anular ni diálogo de motivo. |
| **CP-CBC-16** | Panel del operario con el barril En limpieza | Muestra "+ Nueva limpieza" hacia su formulario y Anular solo en la operación anulable. |
| **CP-CBC-17** | Estado que no permite registrar | Explica el motivo con el texto del service y, con el barril En uso, avisa que lo ocupa un lote. |
| **CP-CBC-18** | Columna Acciones del panel | Las observaciones van en el diálogo del botón i y todos los botones de fila dentro de la columna Acciones. |
| **CP-CBC-19** | Paginación del panel | Se pagina de a 20 con el pie del sistema, que conserva la pestaña. |
| **CP-CBC-20** | Detalle de una limpieza | El operario y el gerente ven la información del barril y los datos por tema. |
| **CP-CBC-21** | Detalle de despacho, fraccionamiento y devolución | Muestra el cliente y la fecha estimada, la cantidad extraída y los litros descartados. |
| **CP-CBC-22** | Detalle por permiso | El operario no ve el detalle de un despacho y el encargado no ve el de una limpieza. |
| **CP-CBC-23** | Detalle de una operación anulada | Muestra la fecha y el motivo de la anulación, sin el usuario que anuló. |
| **CP-CBC-24** | Operación de otro barril o barril inexistente | Vuelve al origen con alerta de error, sin revelar datos. |
| **CP-CBC-25** | Dónde aparece Registrado por | Solo en el detalle de la operación; "Responsable" no aparece en ninguna pantalla. |
