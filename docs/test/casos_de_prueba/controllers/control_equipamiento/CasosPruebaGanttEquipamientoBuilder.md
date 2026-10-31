## Casos de prueba de `GanttEquipamientoBuilder`

**Tipo de prueba:** unitaria sin Spring: geometría de la línea de tiempo de un equipo. Clase `GanttEquipamientoBuilderTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-GEQ-01** | sin bloques el diagrama es vacío, con un carril con el identificador del equipo, el eje de una semana a 240 px por día y el título Equipo | sin bloques el diagrama es vacío, con un carril con el identificador del equipo, el eje de una semana a 240 px por día y el título Equipo |
| **CP-GEQ-02** | el eje rotula cada día con su día de la semana y su fecha y cada 6 horas con la hora, y la marca de hoy está en el momento actual | el eje rotula cada día con su día de la semana y su fecha y cada 6 horas con la hora, y la marca de hoy está en el momento actual |
| **CP-GEQ-03** | el ancho de una etapa de lote es proporcional a su duración real, con el color de su estado, el texto dentro cuando entra completo y el enlace al lote | el ancho de una etapa de lote es proporcional a su duración real, con el color de su estado, el texto dentro cuando entra completo y el enlace al lote |
| **CP-GEQ-04** | una etapa corta, de pocas horas, se dibuja proporcional y sin texto, y se identifica por su color y su tooltip | una etapa corta, de pocas horas, se dibuja proporcional y sin texto, y se identifica por su color y su tooltip |
| **CP-GEQ-05** | una etapa en curso muestra En curso como fin en el tooltip y una cancelada se pinta de gris | una etapa en curso muestra En curso como fin en el tooltip y una cancelada se pinta de gris |
| **CP-GEQ-06** | una operación es un rectángulo angosto de ancho fijo en su instante, sin texto, con su color sólido, su enlace y su tooltip con fecha y hora exactas | una operación es un rectángulo angosto de ancho fijo en su instante, sin texto, con su color sólido, su enlace y su tooltip con fecha y hora exactas |
| **CP-GEQ-07** | todo es una sola fila: sin subcarriles, sin íconos ni marcadores, y cada barra conserva el alto estándar | todo es una sola fila: sin subcarriles, sin íconos ni marcadores, y cada barra conserva el alto estándar |
| **CP-GEQ-08** | una etapa que sale de la semana se recorta en el borde y se marca como continuación; un bloque fuera de la semana se omite | una etapa que sale de la semana se recorta en el borde y se marca como continuación; un bloque fuera de la semana se omite |
| **CP-GEQ-09** | la leyenda lista solo los tipos de bloque presentes, con el color de cada uno | la leyenda lista solo los tipos de bloque presentes, con el color de cada uno |
