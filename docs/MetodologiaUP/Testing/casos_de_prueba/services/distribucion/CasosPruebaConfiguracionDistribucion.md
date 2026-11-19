## Casos de prueba de `ConfiguracionDistribucionServicioImpl`

**Tipo de prueba:** unitaria (clase `ConfiguracionDistribucionServicioImplTest`), con el repositorio simulado. La configuración de distribución es una entidad de fila única (`SINGLETON_ID = 1L`) que crea la migración `V35__coordinacion_automatica_retiro_barriles.sql` con la hora de envío 09:00 y 7 días máximos de anticipación del retiro.

### 1. `buscarConfiguracion()`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CDI-01**|Configuración encontrada _(Camino feliz)_|Fila única con hora 09:00 y 7 días|`findById(SINGLETON_ID)` $\rightarrow$ **Presente**|Retorna el DTO con el ID 1, la hora 09:00 y 7 días.|
|**CP-CDI-02**|Fila única inexistente|`findById(SINGLETON_ID)` vacío|`findById` $\rightarrow$ **Vacío**|Lanza `IllegalStateException` (la aplicación no inicializó correctamente).|

### 2. `actualizarConfiguracion(ConfiguracionDistribucionFormDTO configuracionDistribucionFormDTO)`

No admite actualización parcial. Orden de las validaciones: datos y parámetros obligatorios, días máximos de anticipación entre `LimitesTecnicos.DIAS_ANTICIPACION_RETIRO_MINIMO` (1) y `LimitesTecnicos.DIAS_ANTICIPACION_RETIRO_MAXIMO` (30). La hora se guarda sin segundos.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CDI-03**|Datos nulos|`configuracionDistribucionFormDTO: null`|Datos ausentes $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los datos de la configuración de distribución son obligatorios"). No ejecuta `save()`.|
|**CP-CDI-04**|Cada parámetro nulo|Por separado: hora y días en `null`, el resto válido|Parámetro nulo $\rightarrow$ **TRUE**|Lanza "La hora de envío del recordatorio es obligatoria" y "Los días máximos de anticipación del retiro son obligatorios", respectivamente. No ejecuta `save()`.|
|**CP-CDI-05**|Días fuera de rango _(Límite)_|`diasMaximosAnticipacionRetiro`: `0`, `-1` y `31`, por separado|Fuera de [1, 30] $\rightarrow$ **TRUE**|Lanza "Los días máximos de anticipación del retiro deben estar entre 1 y 30". No ejecuta `save()`.|
|**CP-CDI-06**|Límites válidos _(Camino feliz)_|(00:00, 1 día) y, por separado, (23:59, 30 días)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la configuración y el DTO devuelve los dos valores informados en cada caso.|
|**CP-CDI-07**|Hora con segundos|Hora 10:15:42, 7 días|La hora tiene segundos|Se guarda 10:15, sin segundos.|
