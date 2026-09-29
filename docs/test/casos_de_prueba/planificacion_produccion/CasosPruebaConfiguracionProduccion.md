### 1. `buscarConfiguracion()`

Es una entidad singleton (`SINGLETON_ID = 1L`): la crea `ConfiguracionProduccionDataLoader` al arrancar la aplicación, así que siempre existe.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BC-01**|Configuración encontrada _(Camino feliz)_|BD con la fila singleton creada por el `DataLoader`|`findById(SINGLETON_ID)` $\rightarrow$ **Presente**|Retorna `ConfiguracionProduccionResponseDTO` con los datos de la entidad.|

### 2. `actualizarConfiguracion(ConfiguracionProduccionFormDTO configuracionProduccionFormDTO)`

No admite actualización parcial: el gerente de producción siempre reenvía el conjunto completo de parámetros.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AC-01**|Velocidad estándar de molienda nula|`velocidadEstandarMolienda: null`, resto de los campos válidos|`velocidadEstandarMolienda == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Todos los parámetros de configuración son obligatorios"). No ejecuta `save()`.|
|**CP-AC-02**|Criterio de selección de plan secuencial nulo|`criterioSeleccionPlanSecuencial: null`, resto de los campos válidos|`criterioSeleccionPlanSecuencial == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Todos los parámetros de configuración son obligatorios"). No ejecuta `save()`.|
|**CP-AC-03**|Velocidad estándar de envasado en cero _(Límite)_|`velocidadEstandarEnvasado: 0.0`, resto de los campos válidos|`velocidadEstandarEnvasado <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Las velocidades y la capacidad de lote estándar deben ser mayores a cero"). No ejecuta `save()`.|
|**CP-AC-04**|Capacidad de lote estándar negativa|`capacidadLoteEstandar: -5.0`, resto de los campos válidos|`capacidadLoteEstandar <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-AC-05**|Porcentaje mínimo de consumo negativo|`porcentajeMinimoConsumoParaAvanzarEtapa: -1.0`, resto de los campos válidos|`porcentajeMinimoConsumoParaAvanzarEtapa < 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El porcentaje mínimo de consumo para avanzar de etapa debe estar entre 0 y 100"). No ejecuta `save()`.|
|**CP-AC-06**|Porcentaje mínimo de consumo mayor a 100|`porcentajeMinimoConsumoParaAvanzarEtapa: 101.0`, resto de los campos válidos|`porcentajeMinimoConsumoParaAvanzarEtapa > 100` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-AC-07**|Porcentaje mínimo de consumo en 0 y en 100 _(Límites válidos, camino feliz)_|`porcentajeMinimoConsumoParaAvanzarEtapa: 0.0` y, por separado, `100.0`, resto de los campos válidos|Ambas validaciones $\rightarrow$ **FALSE**|Ninguno de los dos casos lanza excepción; la configuración se persiste con el valor informado.|
|**CP-AC-08**|Actualización exitosa _(Camino feliz)_|Todos los campos válidos: `velocidadEstandarMolienda: 6.0`, `velocidadEstandarEnvasado: 12.0`, `capacidadLoteEstandar: 25.0`, `porcentajeMinimoConsumoParaAvanzarEtapa: 90.0`, `criterioSeleccionPlanSecuencial: FERMENTADOR_LIBERACION_MAS_TEMPRANA`, `criterioSeleccionPlanConcurrente: MENOR_CANTIDAD_DE_LOTES`|Todas las validaciones $\rightarrow$ **FALSE**|La configuración se persiste con los 6 valores informados y retorna DTO actualizado.|
