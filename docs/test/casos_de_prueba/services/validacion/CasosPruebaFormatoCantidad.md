## Casos de prueba de `FormatoCantidad`

**Tipo de prueba:** unitaria (clase `FormatoCantidadTest`), sin Spring ni mocks. Verifica el formato único con el que las tablas y los mensajes muestran una cantidad o un número.

**Regla:** separador de miles `.`, separador decimal `,`, hasta cuatro decimales (la escala de las cantidades) y sin ceros sobrantes; lo que excede los cuatro decimales se redondea hacia arriba (`HALF_UP`). Un valor nulo se escribe como `-`. `ValidacionesNumericas.formatearNumero` y los fragmentos `numero` y `medida` de `receta/formato.html` usan este mismo formato.

---

### `formatear(Number)` y `conUnidad(Number, String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-FC-01** | Miles y coma decimal en enteros y decimales | `2000.0`, `1847.3685`, `11.655`, `62.0`, `4.5`, `1500000` y `1000.2500` | Separadores y ceros sobrantes | `2.000`, `1.847,3685`, `11,655`, `62`, `4,5`, `1.500.000` y `1.000,25`. |
| **CP-FC-02** | Hasta 4 decimales | `0.0001`, `0.12345` y `0.00004` | Se conservan hasta 4 decimales y el resto se redondea | `0,0001`, `0,1235` y `0`. |
| **CP-FC-03** | Valor nulo y unidad | `null`, `1847.3685` con `g` y `2000.0` con `g` | Sin valor y con la unidad agregada | `-`, `1.847,3685 g` y `2.000 g`. |
| **CP-FC-04** | Los mensajes coinciden con las tablas | `formatearNumero(11.655)`, `12.655` y `1250.5` | Mismos decimales que las tablas | `11,655`, `12,655` y `1.250,5`. |
| **CP-FC-05** | Plantillas sin formato manual | Todas las plantillas y `receta/formato.html` | Ninguna plantilla reemplaza el punto decimal por coma a mano y los fragmentos usan `FormatoCantidad` | Sin plantillas infractoras; `numero` y `medida` usan `#cantidades` (el dialecto `CantidadesDialect`, que expone `FormatoCantidad` en las plantillas). |
