## Casos de prueba de `GanttEquipamientoBuilder`

**Tipo de prueba:** unitaria sin Spring: geometría de la línea de tiempo de un equipo. Clase `GanttEquipamientoBuilderTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-GEQ-01** | sin bloques el diagrama es vacío, con un carril con el identificador del equipo, el eje del período y el título Equipo | sin bloques el diagrama es vacío, con un carril con el identificador del equipo, el eje del período y el título Equipo |
| **CP-GEQ-02** | un bloque de lote lleva código y etapa, color por estado de la etapa, enlace al detalle del lote y un tooltip con inicio, fin y estado | un bloque de lote lleva código y etapa, color por estado de la etapa, enlace al detalle del lote y un tooltip con inicio, fin y estado |
| **CP-GEQ-03** | una etapa en curso muestra En curso como fin provisorio en el tooltip | una etapa en curso muestra En curso como fin provisorio en el tooltip |
| **CP-GEQ-04** | cada operación tiene su color (limpieza, falla y mantenimiento), enlaza a su detalle y su tooltip lleva la fecha y hora y las observaciones | cada operación tiene su color (limpieza, falla y mantenimiento), enlaza a su detalle y su tooltip lleva la fecha y hora y las observaciones |
| **CP-GEQ-05** | toda barra tiene un ancho mínimo para leer su texto: 120 px las operaciones y 170 px los lotes, aunque duren minutos, y empieza en su posición real del eje | toda barra tiene un ancho mínimo para leer su texto: 120 px las operaciones y 170 px los lotes, aunque duren minutos, y empieza en su posición real del eje |
| **CP-GEQ-07** | las barras no llevan ícono ni marcador: los lotes se colorean por el estado de su etapa y las operaciones por su tipo | las barras no llevan ícono ni marcador: los lotes se colorean por el estado de su etapa y las operaciones por su tipo |
| **CP-GEQ-08** | las barras que se pisan por su ancho mínimo se reparten en subcarriles sin solaparse y la fila crece; sin superposición la fila conserva su alto estándar | las barras que se pisan por su ancho mínimo se reparten en subcarriles sin solaparse y la fila crece; sin superposición la fila conserva su alto estándar |
| **CP-GEQ-06** | una etapa que sale del período se recorta en el borde y se marca como continuación; un bloque fuera del período se omite | una etapa que sale del período se recorta en el borde y se marca como continuación; un bloque fuera del período se omite |
| **CP-GEQ-09** | la leyenda lista solo los tipos de bloque presentes, con el color de cada uno | la leyenda lista solo los tipos de bloque presentes, con el color de cada uno |
