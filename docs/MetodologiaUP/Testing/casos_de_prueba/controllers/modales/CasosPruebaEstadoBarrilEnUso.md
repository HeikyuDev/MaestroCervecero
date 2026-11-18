## Casos de prueba de los colores de las etiquetas de estado operativo del barril

**Tipo de prueba:** unitaria (clase `EstadoBarrilEnUsoTest`), lectura de las plantillas `barril/estado.html` y `equipamiento/estado.html`, sin Spring.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-EBU-01**|Color del estado En uso|Las plantillas de la etiqueta de estado del barril y del equipamiento|El barril En uso usa el mismo color que el equipamiento En uso|Ambas definen para `EN_USO` la misma pareja de clases (`bg-blue-50 text-blue-700`).|
|**CP-EBU-02**|Colores de los estados compartidos|Las mismas dos plantillas|Disponible, En uso, En limpieza y En mantenimiento usan el mismo color en barril y en equipamiento|Cada estado tiene la misma pareja de clases en las dos plantillas.|
|**CP-EBU-03**|Color propio de Con cerveza|La plantilla del barril|El estado Con cerveza tiene un color que ningún otro estado de barril ni de equipamiento usa|Su pareja de clases (`bg-cyan-50 text-cyan-700`) no coincide con la de ningún otro estado.|
