## Casos de prueba de `plantillas del control de equipamiento`

**Tipo de prueba:** guarda de plantillas (JUnit plano): filas por tema, Abrir y ojo, pestañas con ícono, enlace del identificador del equipo, menú, historial, observaciones en el diálogo y consistencia con las pantallas de referencia. Clase `ControlEquipamientoPlantillasTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-PEQ-01** | Bloque de información del equipo | Separa en filas la identidad, los parámetros del tipo y el mantenimiento. |
| **CP-PEQ-02** | Formulario de registro | Lleva solo las observaciones, con asterisco y ejemplo, sin campo de fecha, y es novalidate. |
| **CP-PEQ-03** | Lista de control | Usa el fragmento del botón Abrir sin copiar su marcado; el ojo es solo del gerente y lleva aria-label sin tooltip. |
| **CP-PEQ-04** | Pestañas del panel | Usan el fragmento de pestañas con un ícono. |
| **CP-PEQ-05** | Identificador del equipo | Es un enlace ámbar al detalle solo con `EQUIPAMIENTO_CONSULTAR` y texto plano sin ese permiso. |
| **CP-PEQ-06** | Pantallas de lote y de planificación | Muestran el identificador del equipo con el fragmento de enlace. |
| **CP-PEQ-07** | Listas de Gestión de Equipamientos | Las cuatro tienen el ojo hacia el detalle con aria-label y sin tooltip. |
| **CP-PEQ-08** | Barra lateral | Tiene Control de Equipamientos en Operación para quien consulta limpiezas. |
| **CP-PEQ-10** | Accesos cruzados y personas | El historial, el panel, el detalle de la operación y el detalle del equipamiento no llevan Abrir ni el ojo en su encabezado ni "Responsable"; solo el detalle de la operación muestra "Registrado por". |
| **CP-PEQ-11** | Historial | Usa el patrón de las listas: filtros en una grilla con Limpiar y Buscar, tabla estándar con encabezado gris, badge de estado operativo y fragmento de paginación. |
| **CP-PEQ-12** | El botón i es solo el de la anulación | Ninguna tabla (panel e historial) muestra las observaciones ni incluye `detalle-anulacion :: observaciones(`; en el panel la celda Acciones tiene un único fragmento de `detalle-anulacion`, `botonSinUsuario`, solo para la operación anulada, junto al ojo; el historial no tiene botón i; el detalle de la operación muestra las observaciones con `break-words`; el fragmento compartido no ofrece un botón de observaciones. |
| **CP-PEQ-13** | Consistencia con las pantallas de referencia | Repiten el encabezado, la tabla con Acciones fija, el botón "Nueva ..." en la tarjeta, el formulario con aviso de obligatorios, Cancelar y Registrar. |
| **CP-PEQ-14** | Columnas de la lista de control | Tiene cinco columnas: Identificador, Tipo, Estado operativo, Usos y Acciones; la fila vacía abarca las cinco. |
