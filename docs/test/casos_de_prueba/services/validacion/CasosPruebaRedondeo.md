## Casos de prueba de `Redondeo`

**Tipo de prueba:** unitaria (clase `RedondeoTest`), sin Spring ni mocks. Verifica el redondeo de las cantidades de stock, reservas, consumos y envasados.

**Regla:** toda cantidad se redondea a `LimitesTecnicos.ESCALA_CANTIDADES` (4) decimales con `RoundingMode.HALF_UP`, tanto al escribirla como al compararla. Los números de punto flotante no representan con exactitud la mayoría de los decimales (`0.1 + 0.2` da `0.30000000000000004`); el redondeo evita que ese residuo haga fallar comparaciones entre cantidades. Un valor no finito (`NaN` o infinito) se devuelve sin cambios, para que lo rechace la validación numérica que corresponda.

---

### `redondear(double cantidad)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-RDN-01** | Suma decimal con residuo | `0.1 + 0.2` (`0.30000000000000004`) | El residuo está más allá del cuarto decimal | Retorna `0.3`. |
| **CP-RDN-02** | Cuatro decimales con redondeo hacia arriba en la mitad | `1.00005`, `2.00004` y `12.345678` | Cuarto decimal y modo `HALF_UP` | Retorna `1.0001`, `2.0` y `12.3457`. `ESCALA_CANTIDADES` vale 4. |
| **CP-RDN-03** | Valores negativos | `-1.00005` y `-0.30000000000000004` | La misma regla aplica al valor absoluto | Retorna `-1.0001` y `-0.3`. |
| **CP-RDN-04** | Valor que ya tiene 4 decimales o menos | `89.9`, `100.1`, `0.0` y `1000000.0` | El valor ya es exacto | Retorna el mismo valor. |
| **CP-RDN-05** | Residuo mínimo | `1e-16` y `0.3 - 0.30000000000000004` | El residuo es menor a medio décimo de milésimo | Retorna `0.0`. |
| **CP-RDN-06** | Valores no finitos | `NaN`, `+Infinity` y `-Infinity` | `!Double.isFinite(cantidad)` | Retorna el mismo valor, sin lanzar excepción. |
| **CP-RDN-07** | Acumulado decimal contra su tope | `10.2 + 89.9` (`100.10000000000001`) contra `100.1` | Sin redondeo el acumulado parece superar el tope | `redondear(10.2 + 89.9)` es igual a `redondear(100.1)`. |
