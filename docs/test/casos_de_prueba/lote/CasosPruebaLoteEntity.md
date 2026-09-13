Casos de prueba de los métodos de dominio de `LoteEntity` (no de un service — es lógica propia de la entidad, sin dependencias externas).

### 1. `obtenerEtapaPorTipo(TipoEtapa tipo)`

Antes vivía (por error de ubicación) en `EscaladoInsumoServicioImpl`: no es responsabilidad del escalado de insumos resolver una etapa del lote por tipo, es una consulta propia del lote sobre sus propias etapas. Se movió acá como método de dominio de `LoteEntity`; `EscaladoInsumoServicioImpl` y `LoteServicioImpl` ahora llaman a `lote.obtenerEtapaPorTipo(tipo)` directamente.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-LE-01**|Etapa encontrada|Lote con sus 6 etapas, `tipo: HERVIDO`|`etapas.stream().filter(...).findFirst()` $\rightarrow$ **Presente**|Retorna la `EtapaLoteEntity` cuyo `etapa` es `HERVIDO`.|
|**CP-LE-02**|Etapa no encontrada|Lote sin etapas, `tipo: MACERACION`|`etapas.stream().filter(...).findFirst()` $\rightarrow$ **vacío**|Lanza `RecursoNoEncontradoException` con mensaje "El lote no tiene una etapa de MACERACION".|
