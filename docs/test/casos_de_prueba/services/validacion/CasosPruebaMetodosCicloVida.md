## Casos de prueba de `MetodosCicloVida` (variantes con campo)

**Tipo de prueba:** unitaria (clase `MetodosCicloVidaTest`), sin Spring ni mocks. Verifica que el rechazo de una fecha indique, cuando se informa, el campo del formulario al que se refiere (`ReglaNegocioException.getCampo()`), para que la pantalla lo marque. Sin campo, el rechazo no lo indica.

### `validarFechaPosteriorAUltimaOperacion(fecha, nombreEntidad, [campo,] fechasExistentes...)` y `validarFechaNoFutura(fecha, [campo])`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MCV-01**|Fecha igual o anterior a una existente, sin campo|`fecha: 15/01/2026 10:00`; una fecha existente igual y otra posterior|Hay una fecha posterior o igual|Lanza `ReglaNegocioException` ("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril") con `campo = null`.|
|**CP-MCV-02**|Fecha igual o anterior a una existente, con campo|`fecha: 15/01/2026 10:00`, `campo: "fechaEnvasado"`; fechas existentes anterior e igual|Hay una fecha igual|Lanza `ReglaNegocioException` con `campo = "fechaEnvasado"`. Con una fecha existente un minuto anterior y otra nula no lanza nada.|
|**CP-MCV-03**|Fecha futura|`fecha`: mañana, con y sin `campo: "fechaEnvasado"`|`fecha > ahora`|Lanza `ReglaNegocioException` ("La fecha de esta operación no puede ser posterior a la fecha y hora actual") con `campo = "fechaEnvasado"` o `null` según corresponda. Una fecha pasada no lanza nada.|
