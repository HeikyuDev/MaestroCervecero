## Casos de prueba de `CalculosCerveza`

**Tipo de prueba:** unitaria (clase `CalculosCervezaTest`), sin Spring ni mocks. Verifica los cálculos estimados a partir de los objetivos de una receta, que son la única definición usada por el mapper de recetas, el detalle del lote y el formulario de receta.

**Reglas:** ABV (%) = (OG - FG) x 131,25, con la OG y la FG tal como están guardadas (por ejemplo `1.05` y `1.01` dan 5,25). Los litros de alcohol son `volumen x ABV / 100`. Ambos resultados llevan un único decimal con redondeo `HALF_UP`. Se calculan con `BigDecimal.valueOf`, para que no aparezcan errores de coma flotante como 5,249999. Si falta un dato (o la OG no supera a la FG, en el ABV) el resultado es `null`.

---

### `calcularAbv(Double, Double)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CC-01** | Redondeo de la mitad hacia arriba | OG `1.05`, FG `1.01` | (1,05 - 1,01) x 131,25 = 5,25 | `5.3`. |
| **CP-CC-02** | Sin errores de coma flotante | OG/FG `1.065`/`1.012`, `1.05`/`1.01` y `1.04`/`1.008` | En coma flotante `1.065 - 1.012` da 0,05299999999999994 | `7.0` (6,95625), `5.3` y `4.2`. |
| **CP-CC-03** | Un único decimal | OG/FG `1.048`/`1.010`, `1.046`/`1.010` y `1.05`/`1.0` | 4,9875, 4,725 y 6,5625 se redondean a un decimal | `5.0`, `4.7` y `6.6`. |
| **CP-CC-04** | Falta una densidad | (a) OG `null`; (b) FG `null`; (c) ambas `null` | Datos incompletos | Devuelve `null` en los tres casos. |
| **CP-CC-05** | OG igual o menor que la FG | OG `1.01`/FG `1.01`; OG `1.01`/FG `1.05` | Sin diferencia positiva no hay alcohol que mostrar | Devuelve `null`. |
| **CP-CC-06** | Valores grandes | OG/FG `1.12`/`1.0` y `2.0`/`1.0` | Se mantiene la precisión | `15.8` (15,75) y `131.3` (131,25). |

---
