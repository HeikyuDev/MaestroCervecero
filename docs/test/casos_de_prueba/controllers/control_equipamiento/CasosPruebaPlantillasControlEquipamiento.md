## Casos de prueba de `plantillas del control de equipamiento`

**Tipo de prueba:** guarda de plantillas (JUnit plano): filas por tema, Abrir y ojo, pestañas con ícono, enlace del identificador del equipo, menú, historial, observaciones en el diálogo y consistencia con las pantallas de referencia. Clase `ControlEquipamientoPlantillasTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-PEQ-01** | Bloque de información del equipo | Separa en filas la identidad, los parámetros del tipo y el mantenimiento. |
| **CP-PEQ-02** | Formulario de registro | Lleva la fecha en una fila y las observaciones en otra, con asterisco y ejemplo, y es novalidate. |
| **CP-PEQ-03** | Lista de control | Usa el fragmento del botón Abrir sin copiar su marcado; el ojo es solo del gerente y lleva aria-label sin tooltip. |
| **CP-PEQ-04** | Pestañas del panel | Usan el fragmento de pestañas con un ícono. |
| **CP-PEQ-05** | Identificador del equipo | Es un enlace ámbar al detalle solo con `EQUIPAMIENTO_CONSULTAR` y texto plano sin ese permiso. |
| **CP-PEQ-06** | Pantallas de lote y de planificación | Muestran el identificador del equipo con el fragmento de enlace. |
| **CP-PEQ-07** | Listas de Gestión de Equipamiento | Las cuatro tienen el ojo hacia el detalle con aria-label y sin tooltip. |
| **CP-PEQ-08** | Barra lateral | Tiene Control de Equipamiento en Operación para quien consulta limpiezas. |
| **CP-PEQ-10** | Accesos cruzados y personas | El historial, el panel, el detalle de la operación y el detalle del equipamiento no llevan Abrir ni el ojo en su encabezado ni "Responsable"; solo el detalle de la operación muestra "Registrado por". |
| **CP-PEQ-11** | Historial | Usa el patrón de las listas: filtros en una grilla con Limpiar y Buscar, tabla estándar con encabezado gris, badge de estado operativo y fragmento de paginación. |
| **CP-PEQ-12** | Observaciones en las tablas | Ninguna tabla las escribe en la fila: usan el botón i del fragmento compartido, cuyo diálogo recibe el título, la etiqueta y la fecha por atributo. |
| **CP-PEQ-13** | Consistencia con las pantallas de referencia | Repiten el encabezado, la tabla con Acciones fija, el botón "Nueva ..." en la tarjeta, el formulario con aviso de obligatorios, Cancelar y Registrar. |
