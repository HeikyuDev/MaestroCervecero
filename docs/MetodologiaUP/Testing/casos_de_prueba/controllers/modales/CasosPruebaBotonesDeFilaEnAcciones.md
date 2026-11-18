## Casos de prueba de `BotonesDeFilaEnAccionesTest`

**Tipo de prueba:** guarda de plantillas (JUnit plano, sin Spring).

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-BFA-01** | Botones de fila fuera de la columna Acciones | En ninguna plantilla (salvo los buscadores modales) hay una celda sin `col-acciones` que contenga el botón i de anulación u observaciones, el ojo, modificar, baja, anular o Abrir. |
| **CP-BFA-02** | Columna Acciones incondicional | En las tablas de ingresos, consumos y envasados el encabezado Acciones y su celda no llevan `th:if` ni `sec:authorize`: solo el contenido se condiciona al permiso y al estado operable. |
| **CP-BFA-03** | Tablas de solo lectura con anulados | Los consumos y envasados del Detalle del lote y los del panel del lote tienen su columna Acciones con el botón i; el panel de control de equipamiento lleva ahí las observaciones y el detalle de la anulación. |
