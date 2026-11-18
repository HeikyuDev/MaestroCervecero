Casos de prueba de `MapperReservaInsumo` (mapper estático, sin Spring ni mocks).

### 1. `toDTO(ReservaInsumoEntity)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MRI-01**|Historial completo de la reserva|Reserva `LIBERADA` con `cantidadReservada: 10.0`, `cantidadConsumida: 6.0`, `cantidadLiberada: 1.0`, `cantidadReducidaPorAjuste: 3.0`, `cantidadPendiente: 0.0` sobre el lote de insumo "L-01"|Mapeo de la entidad|El DTO informa el id, las cinco cantidades, el estado `LIBERADA` y el lote de insumo con su identificación.|
|**CP-MRI-02**|Reserva nula|`null`|Entrada nula|Retorna `null`.|
