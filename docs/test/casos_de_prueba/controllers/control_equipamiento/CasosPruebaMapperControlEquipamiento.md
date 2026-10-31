## Casos de prueba de `MapperControlEquipamiento`

**Tipo de prueba:** unitaria sin Spring: campos agregados a los DTO de bloque y de operación. Clase `MapperControlEquipamientoTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-MCE-01** | el bloque de una etapa de lote lleva el ID del lote, además del de la ... | el bloque de una etapa de lote lleva el ID del lote, además del de la etapa |
| **CP-MCE-02** | el bloque de una operación lleva el responsable que la registró y ning... | el bloque de una operación lleva el responsable que la registró y ningún ID de lote |
| **CP-MCE-03** | la operación registrada lleva su fecha de registro y no tiene usuario ... | la operación registrada lleva su fecha de registro y no tiene usuario de anulación |
| **CP-MCE-04** | la operación anulada lleva como usuario de anulación a quien la modifi... | la operación anulada lleva como usuario de anulación a quien la modificó por última vez |
