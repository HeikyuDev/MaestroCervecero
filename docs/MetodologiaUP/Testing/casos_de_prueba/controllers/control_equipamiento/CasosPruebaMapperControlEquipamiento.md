## Casos de prueba de `MapperControlEquipamiento`

**Tipo de prueba:** unitaria del mapper de las filas del historial y de las operaciones. Clase `MapperControlEquipamientoTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-MCE-01** | Fila de una etapa de lote | Lleva el ID del lote, el código, la etapa, sus fechas y el estado que deja en el equipamiento. |
| **CP-MCE-02** | Fila de una limpieza, una falla o un mantenimiento | La limpieza lleva su estado resultante real; la falla y el mantenimiento, el que cada una deja en el equipamiento. |
| **CP-MCE-03** | Operación registrada | Lleva su fecha de registro y el usuario que la registró. |
| **CP-MCE-04** | Operación anulada | Lleva su estado, su fecha y su motivo de anulación y no es anulable. |
