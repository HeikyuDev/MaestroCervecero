## Casos de prueba de `ControlEquipamientoController`

**Tipo de prueba:** capa web (`@WebMvcTest`): lista de control, historial del equipo, panel con la tabla de cada pestaña y detalle de una operación; services simulados. Clase `ControlEquipamientoControllerTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-CCE-01** | Lista de control para el operario | Muestra el botón Abrir en cada fila, no muestra el ojo del historial y la tabla no tiene columna de última operación. |
| **CP-CCE-02** | Lista de control para el gerente de producción | Muestra el ojo del historial y el botón Abrir en cada fila. |
| **CP-CCE-03** | Roles sin permiso de consulta | La lista, el historial y el panel redirigen con alerta de peligro y no se invoca el service. |
| **CP-CCE-04** | Criterios de la lista | El identificador sin espacios, el tipo y el estado operativo se pasan al service. |
| **CP-CCE-05** | Tipo o estado operativo inválidos | No filtran y se avisa con una alerta warning. |
| **CP-CCE-06** | Historial del equipo para el gerente | Muestra la información del equipo, los filtros y una fila por actividad con su estado resultante, el botón de acciones hacia el lote o la operación y el botón i de observaciones. |
| **CP-CCE-07** | Historial del equipo para el operario | Redirige con alerta de peligro y no se invoca el service. |
| **CP-CCE-08** | Historial vacío | Sin actividad muestra "El equipamiento no tiene operaciones registradas" y con filtros sin resultados, "No se encontraron resultados". |
| **CP-CCE-09** | Filtros del historial | El tipo y las fechas se pasan al service (la fecha hasta incluye todo su día) y se conservan en el formulario y en los enlaces de paginación. |
| **CP-CCE-10** | Tipo inválido o rango de fechas invertido en el historial | El tipo inválido se avisa y no filtra; el rango invertido muestra el aviso del service con la lista vacía. |
| **CP-CCE-11** | Panel de limpiezas del operario con el equipo EN_LIMPIEZA | Muestra la tabla, el botón "Nueva limpieza" hacia su formulario, ningún formulario incrustado y el botón Anular solo en la operación anulable. |
| **CP-CCE-12** | Panel con un estado que no permite registrar | No muestra el botón de alta y explica el motivo con el texto del service. |
| **CP-CCE-13** | Panel para el gerente de producción | Modo consulta: sin botón de alta, sin formulario y sin botones de anular. |
| **CP-CCE-14** | Pestaña inexistente o pedida | Una pestaña inexistente muestra la primera; la pedida se marca como activa. |
| **CP-CCE-15** | Detalle de una operación | Ambos roles ven la información del equipo y los datos de la operación por tema. |
| **CP-CCE-16** | Detalle de una operación anulada | Muestra la fecha y el motivo de la anulación, sin el usuario que anuló. |
| **CP-CCE-17** | Operación o equipamiento inexistente | Vuelve al origen con alerta de error. |
| **CP-CCE-18** | Dónde aparece Registrado por | Aparece solo en el detalle de la operación, no en el panel ni en el historial, y "Responsable" no aparece en ninguna pantalla. |
| **CP-CCE-20** | Observaciones en la tabla del panel | No se escriben en la fila: el botón i las abre en el diálogo compartido, con el título del tipo y la fecha de la operación. |
| **CP-CCE-21** | Columna Acciones del panel | Lleva siempre el ojo y el botón i, y Anular solo en la operación anulable y solo para el operario; los títulos de las tablas concuerdan en género. |
| **CP-CCE-22** | Paginación de la tabla del panel | Se pagina de a 20 con el pie de paginación del sistema, que conserva la pestaña. |
