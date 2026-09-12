### 1. `buscarPorEtapaEInsumo(Long idEtapaLote, Long idInsumo)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BE-01**|Consulta con reservas existentes|`idEtapaLote: 1L`, `idInsumo: 1L`, 2 reservas de esa etapa para ese insumo (`8.0` y `4.0`)|`findByEtapaLoteIdAndLoteInsumo_Insumo_Id(1L, 1L)` contiene elementos|Retorna `List<ReservaInsumoResponseDTO>` con los 2 elementos mapeados, en el mismo orden.|
|**CP-BE-02**|Consulta sin reservas para ese insumo en esa etapa|`idEtapaLote: 1L`, `idInsumo: 1L`, sin reservas|`findByEtapaLoteIdAndLoteInsumo_Insumo_Id(1L, 1L)` está vacío|Retorna una lista vacía (no lanza excepción).|
