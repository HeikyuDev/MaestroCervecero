## Casos de prueba de `EstadoOperativoBarril`

**Tipo de prueba:** unitaria (clase `EstadoOperativoBarrilTest`), sin Spring ni mocks.

**Estados:** `CON_CERVEZA`, `DESPACHADO`, `DISPONIBLE`, `EN_LIMPIEZA`, `EN_MANTENIMIENTO` y `EN_USO`. `EN_USO` indica que el barril está siendo llenado por un lote en ejecución y no admite operaciones hasta que el lote se finalice.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-EOB-01**|Etiqueta del estado En uso|`EstadoOperativoBarril.EN_USO`|La etiqueta es la misma que la del estado `EN_USO` de los equipamientos|`getEtiqueta()` devuelve "En uso", igual a `EstadoOperativo.EN_USO.getEtiqueta()`.|
|**CP-EOB-02**|Estados y etiquetas|Todos los valores del enum|Cada estado tiene etiqueta legible|Son exactamente seis estados y ninguna etiqueta está vacía ni contiene guiones bajos.|
