## Casos de prueba de `plantillas del control de equipamiento`

**Tipo de prueba:** guarda de plantillas (JUnit plano): filas por tema, Abrir y ojo, pestañas con ícono, enlace del identificador del equipo y menú. Clase `ControlEquipamientoPlantillasTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-PEQ-01** | el bloque de información del equipo separa en filas la identidad, los parámetros del tipo y el mantenimiento | el bloque de información del equipo separa en filas la identidad, los parámetros del tipo y el mantenimiento |
| **CP-PEQ-02** | el formulario de registro lleva la fecha en una fila y las observaciones en otra, con asterisco y ejemplo, y es novalidate | el formulario de registro lleva la fecha en una fila y las observaciones en otra, con asterisco y ejemplo, y es novalidate |
| **CP-PEQ-03** | la lista de control usa el fragmento del botón Abrir sin copiar su marcado, y el ojo es solo del gerente y lleva aria-label sin tooltip | la lista de control usa el fragmento del botón Abrir sin copiar su marcado, y el ojo es solo del gerente y lleva aria-label sin tooltip |
| **CP-PEQ-04** | las pestañas del panel usan el fragmento de pestañas con un ícono | las pestañas del panel usan el fragmento de pestañas con un ícono |
| **CP-PEQ-05** | el identificador del equipo es un enlace ámbar al detalle solo con EQUIPAMIENTO_CONSULTAR y texto plano sin ese permiso | el identificador del equipo es un enlace ámbar al detalle solo con EQUIPAMIENTO_CONSULTAR y texto plano sin ese permiso |
| **CP-PEQ-06** | las pantallas de lote y de planificación muestran el identificador del equipo con el fragmento de enlace | las pantallas de lote y de planificación muestran el identificador del equipo con el fragmento de enlace |
| **CP-PEQ-07** | las cuatro listas de gestión de equipamiento tienen el ojo hacia el detalle con aria-label y sin tooltip | las cuatro listas de gestión de equipamiento tienen el ojo hacia el detalle con aria-label y sin tooltip |
| **CP-PEQ-08** | la barra lateral tiene Control de Equipamiento en Operación para quien consulta limpiezas | la barra lateral tiene Control de Equipamiento en Operación para quien consulta limpiezas |
| **CP-PEQ-09** | el diagrama del equipo usa barras rectangulares sin marcadores ni íconos, con leyenda de cuadrados de color, y los diagramas de lotes conservan sus clases y el ícono de la limpieza | el diagrama del equipo usa barras rectangulares sin marcadores ni íconos, con leyenda de cuadrados de color, y los diagramas de lotes conservan sus clases y el ícono de la limpieza |
| **CP-PEQ-10** | el cronograma, el panel, el detalle de la operación y el detalle del equipamiento no llevan Abrir ni el ojo en su encabezado, no muestran Responsable y solo el detalle de la operación muestra Registrado por | el cronograma, el panel, el detalle de la operación y el detalle del equipamiento no llevan Abrir ni el ojo en su encabezado, no muestran Responsable y solo el detalle de la operación muestra Registrado por |
