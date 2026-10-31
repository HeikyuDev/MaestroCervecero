## Casos de prueba de `GanttEquipamientoBuilder`

**Tipo de prueba:** unitaria sin Spring: geometría de la línea de tiempo de un equipo. Clase `GanttEquipamientoBuilderTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-GEQ-01** | sin bloques el diagrama es vacío, con un carril con el identificador d... | sin bloques el diagrama es vacío, con un carril con el identificador del equipo, el eje del período y el título Equipo |
| **CP-GEQ-02** | un bloque de lote lleva código y etapa, color de lote, enlace al detal... | un bloque de lote lleva código y etapa, color de lote, enlace al detalle del lote y un tooltip con inicio, fin y estado |
| **CP-GEQ-03** | una etapa en curso muestra En curso como fin provisorio en el tooltip | una etapa en curso muestra En curso como fin provisorio en el tooltip |
| **CP-GEQ-04** | cada operación tiene su color, enlaza a su detalle y su tooltip lleva ... | cada operación tiene su color, enlaza a su detalle y su tooltip lleva observaciones y responsable |
| **CP-GEQ-05** | un bloque de pocos minutos ocupa un ancho mínimo para poder verse y pr... | un bloque de pocos minutos ocupa un ancho mínimo para poder verse y presionarse |
| **CP-GEQ-06** | un bloque que sale del período se recorta en el borde y se marca como ... | un bloque que sale del período se recorta en el borde y se marca como continuación; uno fuera del período se omite |
