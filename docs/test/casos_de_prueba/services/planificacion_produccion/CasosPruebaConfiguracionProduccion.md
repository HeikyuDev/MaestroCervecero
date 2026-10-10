### 1. `buscarConfiguracion()`

Es una entidad singleton (`SINGLETON_ID = 1L`): la crea `ConfiguracionProduccionDataLoader` al arrancar la aplicación, así que siempre existe.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BC-01**|Configuración encontrada _(Camino feliz)_|BD con la fila singleton creada por el `DataLoader`|`findById(SINGLETON_ID)` $\rightarrow$ **Presente**|Retorna `ConfiguracionProduccionResponseDTO` con los datos de la entidad.|

### 2. `actualizarConfiguracion(ConfiguracionProduccionFormDTO configuracionProduccionFormDTO)`

No admite actualización parcial: el gerente de producción siempre reenvía el conjunto completo de parámetros.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AC-02**|Criterio de selección de plan secuencial nulo|`criterioSeleccionPlanSecuencial: null`, resto de los campos válidos|`criterioSeleccionPlanSecuencial == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Todos los parámetros de configuración son obligatorios"). No ejecuta `save()`.|
|**CP-AC-03**|Velocidad estándar de envasado en cero _(Límite)_|`velocidadEstandarEnvasado: 0.0`, resto de los campos válidos|`velocidadEstandarEnvasado <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La velocidad de envasado y la capacidad de lote estándar deben ser mayores a cero"). No ejecuta `save()`.|
|**CP-AC-04**|Capacidad de lote estándar negativa|`capacidadLoteEstandar: -5.0`, resto de los campos válidos|`capacidadLoteEstandar <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-AC-05**|Porcentaje mínimo de consumo negativo|`porcentajeMinimoConsumoParaAvanzarEtapa: -1.0`, resto de los campos válidos|`porcentajeMinimoConsumoParaAvanzarEtapa < 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El porcentaje mínimo de consumo para avanzar de etapa debe estar entre 0 y 100 inclusive"). No ejecuta `save()`.|
|**CP-AC-06**|Porcentaje mínimo de consumo mayor a 100|`porcentajeMinimoConsumoParaAvanzarEtapa: 101.0`, resto de los campos válidos|`porcentajeMinimoConsumoParaAvanzarEtapa > 100` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No ejecuta `save()`.|
|**CP-AC-07**|Porcentaje mínimo de consumo en 0 y en 100 _(Límites válidos, camino feliz)_|`porcentajeMinimoConsumoParaAvanzarEtapa: 0.0` y, por separado, `100.0`, resto de los campos válidos|Ambas validaciones $\rightarrow$ **FALSE**|Ninguno de los dos casos lanza excepción; la configuración se persiste con el valor informado.|
|**CP-AC-08**|Actualización exitosa _(Camino feliz)_|Todos los campos válidos: `velocidadEstandarEnvasado: 12.0`, `capacidadLoteEstandar: 25.0`, `porcentajeMinimoConsumoParaAvanzarEtapa: 90.0`, `criterioSeleccionPlanSecuencial: FERMENTADOR_LIBERACION_MAS_TEMPRANA`, `criterioSeleccionPlanConcurrente: MENOR_CANTIDAD_DE_LOTES`|Todas las validaciones $\rightarrow$ **FALSE**|La configuración se persiste con los 5 valores informados y retorna DTO actualizado.|
|**CP-AC-09**|Velocidad de envasado NaN o +Infinity|`velocidadEstandarEnvasado: Double.NaN` y, por separado, `Double.POSITIVE_INFINITY`, resto de los campos válidos|Valor no finito $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La velocidad de envasado debe ser un número mayor a 0"). No ejecuta `save()`.|
|**CP-AC-10**|Capacidad de lote estándar NaN o +Infinity|`capacidadLoteEstandar: Double.NaN` y, por separado, `Double.POSITIVE_INFINITY`, resto de los campos válidos|Valor no finito $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La capacidad de lote estándar debe ser un número mayor a 0"). No ejecuta `save()`.|
|**CP-AC-11**|Porcentaje mínimo de consumo NaN|`porcentajeMinimoConsumoParaAvanzarEtapa: Double.NaN`, resto de los campos válidos|Valor no finito $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("El porcentaje mínimo de consumo para avanzar de etapa debe ser un número mayor o igual a 0"). No ejecuta `save()`.|
|**CP-AC-12**|Velocidad y capacidad iguales a sus techos _(Límite válido)_|`velocidadEstandarEnvasado: 100000.0`, `capacidadLoteEstandar: 100000.0`, resto de los campos válidos|Ambos valores `> 100000` $\rightarrow$ **FALSE**|La configuración se persiste con los valores informados.|
|**CP-AC-13**|Velocidad de envasado sobre el techo|`velocidadEstandarEnvasado: 100000.01`, resto de los campos válidos|`velocidadEstandarEnvasado > 100000` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La velocidad de envasado no puede superar 100.000 litros por hora"). No ejecuta `save()`.|
|**CP-AC-14**|Capacidad de lote estándar sobre el techo|`capacidadLoteEstandar: 100000.01`, resto de los campos válidos|`capacidadLoteEstandar > 100000` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("La capacidad de lote estándar no puede superar 100.000 litros"). No ejecuta `save()`.|
