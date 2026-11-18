## Casos de prueba de `MapperDevolucionBarril`

**Tipo de prueba:** unitaria (clase `MapperDevolucionBarrilTest`), sin Spring ni mocks. Verifica la conversión de `DevolucionBarrilEntity` a `DevolucionBarrilResponseDTO`.

**Regla de conversión:** `litrosDescartados` se copia de la entidad (los litros que tenía el barril al volver y que se descartaron al registrar la devolución; 0 si no tenía).

### `toDTO(DevolucionBarrilEntity)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MDVB-01**|Devolución con litros descartados|Devolución con `litrosDescartados: 35.5`, fecha `05/02/2026 10:00` y barril `2L`|Se informan los litros descartados junto con los demás datos|El DTO tiene `litrosDescartados: 35.5`, la fecha de devolución y el barril.|
|**CP-MDVB-02**|Devolución sin litros descartados informados|Devolución construida sin `litrosDescartados`|El valor por defecto es 0|El DTO tiene `litrosDescartados: 0.0`.|
|**CP-MDVB-03**|Devolución nula|`devolucionBarrilEntity: null`|La entrada no existe|Retorna `null`.|
