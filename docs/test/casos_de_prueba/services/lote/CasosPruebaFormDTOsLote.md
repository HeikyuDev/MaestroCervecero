## Casos de prueba de la validación de los FormDTO de Lote

**Tipo de prueba:** unitaria (clase `FormDTOsLoteValidacionTest`), sin Spring ni mocks: usa el `Validator` de Bean Validation sobre cada FormDTO. Verifica que las anotaciones informen los campos obligatorios y la longitud de los motivos con los mismos textos que los services de Lote, Envasado y Consumo, de modo que el Controller muestre el mismo mensaje con cualquiera de las dos capas.

**Reglas:** los identificadores y las cantidades son obligatorios (`@NotNull`); los motivos son obligatorios (`@NotBlank`) y no superan 255 caracteres (`@Size`, `LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO`). No se anota que las cantidades sean mayores a 0: esa regla, con su redondeo y su tope máximo, la resuelve el Service. Los FormDTO exponen setters para que el binding del formulario funcione.

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-FDL-01** | `LoteFormDTO` sin datos | `new LoteFormDTO()` | Todos los campos son nulos | 6 violaciones: "El volumen objetivo del lote es obligatorio", "La planificación de producción es obligatoria", "El fermentador es obligatorio", "El molino es obligatorio", "El macerador es obligatorio" y "La olla de hervor es obligatoria". |
| **CP-FDL-02** | `LoteFormDTO` completo | Volumen objetivo `20.0` e ids de planificación, fermentador, molino, macerador y olla de hervor cargados con los setters | Todos los campos informados | Sin violaciones. |
| **CP-FDL-03** | `ConsumoInsumoFormDTO` | Sin datos; y con `idLoteInsumo: 1L`, `cantidadConsumida: 2.5` | Campos nulos / informados | Sin datos: "El lote de insumo es obligatorio" y "La cantidad consumida es obligatoria". Con datos: sin violaciones. |
| **CP-FDL-04** | `RegistrarEnvasadoLoteFormDTO` | Sin datos; y con `idBarril: 1L`, `cantidad: 30.0` | Campos nulos / informados | Sin datos: "El barril es obligatorio" y "La cantidad a envasar es obligatoria". Con datos: sin violaciones. |
| **CP-FDL-05** | `CancelacionLoteFormDTO` | `motivoCancelacion` nulo, `"   "`, de 256 caracteres y de 255 caracteres | `@NotBlank` y `@Size(max = 255)` | Nulo y en blanco: "El motivo de cancelación es obligatorio". 256 caracteres: "El motivo de cancelación no puede superar 255 caracteres". 255 caracteres: sin violaciones. |
| **CP-FDL-06** | `AnularConsumoInsumoFormDTO` y `AnularEnvasadoLoteFormDTO` | `motivoAnulacion` nulo o en blanco, de 256 caracteres y un motivo válido | `@NotBlank` y `@Size(max = 255)` | Nulo y en blanco: "El motivo de anulación es obligatorio". 256 caracteres: "El motivo de anulación no puede superar 255 caracteres". Motivo válido: sin violaciones. |
