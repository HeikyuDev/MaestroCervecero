## Casos de prueba de `ConfiguracionComprasServicioImpl`

**Tipo de prueba:** unitaria (clase `ConfiguracionComprasServicioImplTest`), con el repositorio simulado. La configuración de compras es una entidad de fila única (`SINGLETON_ID = 1L`) que crea la migración `V30__generacion_automatica_planes_compra.sql` con plazo de 48 horas, 3 planes, criterio ponderado y peso del precio 50.

### 1. `buscarConfiguracion()`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CCO-01**|Configuración encontrada _(Camino feliz)_|Fila única con plazo 48, 3 planes, `PONDERADO`, peso 50|`findById(SINGLETON_ID)` $\rightarrow$ **Presente**|Retorna el DTO con los cuatro valores y el ID 1.|
|**CP-CCO-02**|Fila única inexistente|`findById(SINGLETON_ID)` vacío|`findById` $\rightarrow$ **Vacío**|Lanza `IllegalStateException` (la aplicación no inicializó correctamente).|

### 2. `actualizarConfiguracion(ConfiguracionComprasFormDTO configuracionComprasFormDTO)`

No admite actualización parcial. Orden de las validaciones: datos y parámetros obligatorios, plazo y cantidad de planes (enteros mayores a 0 con tope), peso del precio entre 0 y 100.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CCO-03**|Datos nulos|`configuracionComprasFormDTO: null`|Datos ausentes $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` ("Los datos de la configuración de compras son obligatorios"). No ejecuta `save()`.|
|**CP-CCO-04**|Cada parámetro nulo|Por separado: plazo, cantidad de planes, criterio y peso en `null`, el resto válido|Parámetro nulo $\rightarrow$ **TRUE**|Lanza "El plazo de respuesta es obligatorio", "La cantidad de planes es obligatoria", "El criterio de los planes es obligatorio" y "El peso del precio es obligatorio", respectivamente. No ejecuta `save()`.|
|**CP-CCO-05**|Plazo de respuesta que no es mayor a 0 _(Límite)_|`plazoRespuestaHoras: 0` y, por separado, `-1`|`plazo <= 0` $\rightarrow$ **TRUE**|Lanza "El plazo de respuesta debe ser mayor a 0". No ejecuta `save()`.|
|**CP-CCO-06**|Plazo de respuesta sobre el tope|`plazoRespuestaHoras: 721`|`plazo > 720` $\rightarrow$ **TRUE**|Lanza "El plazo de respuesta no puede superar 720 horas". No ejecuta `save()`.|
|**CP-CCO-07**|Cantidad de planes que no es mayor a 0 _(Límite)_|`cantidadPlanes: 0` y, por separado, `-2`|`planes <= 0` $\rightarrow$ **TRUE**|Lanza "La cantidad de planes debe ser mayor a 0". No ejecuta `save()`.|
|**CP-CCO-08**|Cantidad de planes sobre el tope|`cantidadPlanes: 11`|`planes > 10` $\rightarrow$ **TRUE**|Lanza "La cantidad de planes no puede superar 10 planes". No ejecuta `save()`.|
|**CP-CCO-09**|Peso del precio fuera de rango o que no es un número|`pesoPrecio`: `-0.01`, `100.01`, `NaN` e `Infinity`, por separado|Fuera de [0, 100] o no finito $\rightarrow$ **TRUE**|Lanza "El peso del precio debe estar entre 0 y 100". No ejecuta `save()`.|
|**CP-CCO-10**|Límites válidos _(Camino feliz)_|(1 h, 1 plan, `MENOR_PRECIO`, peso 0) y, por separado, (720 h, 10 planes, `MAYOR_CANTIDAD`, peso 100)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la configuración y el DTO devuelve los cuatro valores informados en cada caso.|
