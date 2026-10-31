## Casos de prueba de `MapperControlEquipamiento`

**Tipo de prueba:** unitaria sin Spring: campos agregados a los DTO de bloque y de operación. Clase `MapperControlEquipamientoTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-MCE-01** | el bloque de una etapa de lote lleva el ID del lote, además del de la etapa | el bloque de una etapa de lote lleva el ID del lote, además del de la etapa |
| **CP-MCE-02** | el bloque de una operación no lleva ningún ID de lote | el bloque de una operación no lleva ningún ID de lote |
| **CP-MCE-03** | la operación lleva su fecha de registro y el usuario que la registró | la operación lleva su fecha de registro y el usuario que la registró |
| **CP-MCE-04** | la operación anulada lleva su estado, su fecha y su motivo de anulación y no es anulable | la operación anulada lleva su estado, su fecha y su motivo de anulación y no es anulable |
