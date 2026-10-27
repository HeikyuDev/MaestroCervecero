## Casos de prueba de `FormatoImporte`

**Tipo de prueba:** unitaria (clase `FormatoImporteTest`), sin Spring ni mocks. Verifica el formato con el que las pantallas muestran un importe en pesos.

**Regla:** separador de miles `.`, separador decimal `,`, al menos dos decimales y hasta cuatro (los que admite el costo unitario); lo que excede los cuatro decimales se redondea hacia arriba (`HALF_UP`). Un importe nulo se escribe como `-`.

---

### `formatear(BigDecimal)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-FI-01** | Miles y dos decimales como mínimo | `1250.5`, `1000000` y `0` | Separadores y decimales mínimos | `1.250,50`, `1.000.000,00` y `0,00`. |
| **CP-FI-02** | De 2 a 4 decimales | `0.1234`, `12.30`, `0.123` y `0.12345` | Se conservan hasta 4 decimales y el resto se redondea | `0,1234`, `12,30`, `0,123` y `0,1235`. |
| **CP-FI-03** | Importe nulo | `null` | Sin valor | Devuelve `-`. |
