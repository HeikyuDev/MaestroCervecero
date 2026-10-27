## Casos de prueba de `ValidacionesNumericas`

**Tipo de prueba:** unitaria (clase `ValidacionesNumericasTest`), sin Spring ni mocks. Verifica las validaciones numéricas y de longitud de texto reutilizables por los services.

**Regla:** cada validación lanza `ReglaNegocioException` con un mensaje en español que empieza con la etiqueta que entrega quien llama (por ejemplo `"La capacidad total"`). Los números rechazados incluyen nulo, `NaN` e infinitos. El máximo se muestra con separador de miles español y sin decimales sobrantes (`100.000`, `1,5`). Un valor igual al máximo se acepta.

---

### Validaciones de números

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VN-01** | Mayor a cero con valor inválido | `validarMayorACero` con `null`, `NaN`, `+Infinity`, `-Infinity`, `0`, `-0` y `-1.5` | El valor no es un número finito mayor a 0 | Lanza `ReglaNegocioException`: "La capacidad total debe ser mayor a 0". |
| **CP-VN-02** | Mayor a cero con valor válido | `0.001` y `500.0` | El valor es finito y mayor a 0 | No lanza excepción. |
| **CP-VN-03** | Mayor o igual a cero con valor inválido | `validarMayorOIgualACero` con `null`, `NaN`, `+Infinity`, `-Infinity`, `-0.01` y `-10` | El valor no es un número finito mayor o igual a 0 | Lanza `ReglaNegocioException`: "La capacidad total debe ser mayor o igual a 0". |
| **CP-VN-04** | Mayor o igual a cero con valor válido | `0.0` y `12.5` | El valor es finito y no negativo | No lanza excepción. |
| **CP-VN-05** | Máximo respetado | `validarMaximo` con `100000.0`, `1.0` y `null` (máximo 100.000) | El valor no supera el máximo (el nulo no se evalúa) | No lanza excepción. |
| **CP-VN-06** | Máximo superado | `100000.01` y `+Infinity` (máximo 100.000) | El valor supera el máximo | Lanza `ReglaNegocioException`: "La capacidad total no puede superar 100.000 litros". |
| **CP-VN-07** | Formato del máximo en el mensaje | Máximos 10080, 1000000, 1.5, 20 y 1000 superados | Separador de miles `.`, decimal `,` y sin ceros sobrantes | Mensajes con "10.080 minutos", "1.000.000 kg", "1,5 unidades", "20 L/kg" y "1.000 u". |
| **CP-VN-08** | Mayor a cero con máximo | `validarMayorACeroConMaximo` con `null`, `NaN`, infinitos, `0`, `-1`, `100001` y `100000` (máximo 100.000) | Primero positividad y finitud, luego el máximo | Los inválidos lanzan el mensaje de "mayor a 0"; `100001` lanza el mensaje de máximo; `100000` no lanza. |
| **CP-VN-09** | Mayor o igual a cero con máximo | `validarMayorOIgualACeroConMaximo` con `null`, `NaN`, infinitos, `-0.5`, `100000.5`, `0` y `100000` (máximo 100.000) | Primero no negatividad y finitud, luego el máximo | Los inválidos lanzan el mensaje de "mayor o igual a 0"; `100000.5` lanza el mensaje de máximo; `0` y `100000` no lanzan. |

---

### Validaciones de enteros

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VN-10** | Entero mayor a cero inválido | `validarEnteroMayorACeroConMaximo` con `null`, `0` y `-1` | El valor es nulo, cero o negativo | Lanza `ReglaNegocioException`: "La duración debe ser mayor a 0". |
| **CP-VN-11** | Entero mayor a cero con máximo | `1`, `10080` y `10081` (máximo 10.080) | El valor no supera el máximo | `1` y `10080` no lanzan; `10081` lanza "La duración no puede superar 10.080 minutos". |
| **CP-VN-12** | Entero mayor o igual a cero inválido | `validarEnteroMayorOIgualACeroConMaximo` con `null` y `-1` | El valor es nulo o negativo | Lanza `ReglaNegocioException`: "El amargor debe ser mayor o igual a 0". |
| **CP-VN-13** | Entero mayor o igual a cero con máximo | `0`, `500` y `501` (máximo 500); `100001` (máximo 100.000) | El valor no supera el máximo | `0` y `500` no lanzan; `501` lanza "El amargor no puede superar 500 IBU"; `100001` lanza "Los usos no puede superar 100.000 usos". |

---

### Validaciones de longitud de texto

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VN-14** | Longitud dentro del máximo | `validarLongitudMaxima` con `null`, `""` y 100 caracteres (máximo 100) | La longitud no supera el máximo (el nulo se acepta) | No lanza excepción. |
| **CP-VN-15** | Longitud superior al máximo | 101 caracteres (máximo 100) | La longitud supera el máximo en un carácter | Lanza `ReglaNegocioException`: "El nombre no puede superar 100 caracteres". |
| **CP-VN-16** | Decimales dentro del máximo | `validarMaximoDecimales` con `null`, NaN, infinitos, 35.0, 35.5, 35.50 y 0.1 (máximo 1) | El valor no tiene más decimales que el máximo, ignorando ceros finales (nulo, NaN e infinitos no se evalúan) | No lanza excepción. |
| **CP-VN-17** | Decimales por encima del máximo | `validarMaximoDecimales` con 35.55 y 0.25 (máximo 1) y con 1.12345 (máximo 4) | El valor tiene más decimales que el máximo | Lanza `ReglaNegocioException`: "El IBU objetivo no puede tener más de 1 decimal" y "El costo no puede tener más de 4 decimales". |
