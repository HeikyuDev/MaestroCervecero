## Casos de prueba de `MapperStockInsumo`

**Tipo de prueba:** unitaria (clase `MapperStockInsumoTest`), sin Spring ni mocks. Verifica el cálculo de las cantidades de stock (disponible, reservada, vencida y total) de un lote de insumo y de un insumo, y el mapeo a los DTO de stock.

**Reglas:** un lote está vencido si su fecha de vencimiento es estrictamente anterior a hoy (el que vence hoy es vigente). Lote vigente: disponible es la cantidad actual menos la reservada, reservada es la reservada, vencida es 0 y total es la actual. Lote vencido: disponible y reservada son 0, vencida es la cantidad actual (incluye lo que estaba reservado) y total es la actual. Un insumo suma, cantidad por cantidad, las de sus lotes. Siempre se cumple `disponible + reservada + vencida = total` y las cantidades se redondean con `Redondeo`. El parámetro `hoy` se recibe, de modo que el cálculo es determinista.

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MSI-01** | Lote vigente | `cantidadActual: 10`, `cantidadReservada: 4`, vence dentro de 30 días | `fechaVencimiento` no es anterior a hoy | `disponible: 6`, `reservada: 4`, `vencida: 0`, `total: 10`. |
| **CP-MSI-02** | Lote que vence hoy | `10` y `4`, `fechaVencimiento` igual a hoy | Vence hoy: es vigente | `estaVencido` es `false`; `disponible: 6`, `reservada: 4`, `vencida: 0`. |
| **CP-MSI-03** | Lote que venció ayer | `10` y `4`, `fechaVencimiento` de ayer | Vencido: todo su saldo es vencido | `estaVencido` es `true`; `disponible: 0`, `reservada: 0`, `vencida: 10`, `total: 10`. |
| **CP-MSI-04** | Invariante | Lote vigente, vencido, sin stock y totalmente reservado | `disponible + reservada + vencida = total` | Se cumple en los cuatro casos. |
| **CP-MSI-05** | Redondeo | `cantidadActual: 0.3`, `cantidadReservada: 0.1 + 0.2` | Un residuo de punto flotante no cuenta como stock | `disponible: 0`, `reservada: 0.3`, `total: 0.3`. |
| **CP-MSI-06** | Cantidades nulas | `cantidadActual` y `cantidadReservada` nulas | Nulo equivale a 0 | Las cuatro cantidades son 0. |
| **CP-MSI-07** | Desde la entidad | `LoteInsumoEntity` con `8`, `3` y vencimiento futuro | Usa los campos del lote | `disponible: 5`, `reservada: 3`, `total: 8`. |
| **CP-MSI-08** | Suma de varios lotes | Lote vigente (10, 4), lote que vence hoy (2,5, 0) y lote vencido (7, 1) | Suma cantidad por cantidad | `disponible: 8,5`, `reservada: 4`, `vencida: 7`, `total: 19,5`; se cumple el invariante. |
| **CP-MSI-09** | Suma sin lotes | Colección vacía y colección nula | No hay lotes que sumar | Las cuatro cantidades son 0 en ambos casos. |
| **CP-MSI-10** | Suma con residuo | `0.1` y `0.2` | La suma se redondea | `disponible: 0.3`, `total: 0.3`. |
| **CP-MSI-11** | `desdeSumas` redondea | `0.30000000000000004`, `1.00004`, `2.00006`, `3` | Se redondea a la escala de las cantidades | `0.3`, `1`, `2.0001`, `3`. |
| **CP-MSI-12** | `toDTO` de un insumo | Malta `Malta Pilsen` en `KILOGRAMO` y unas cantidades | Mapea los datos del insumo | `id`, `nombre`, `tipo: MALTA`, `unidadDeMedida` y las mismas cantidades recibidas. |
| **CP-MSI-13** | Entidades nulas | Insumo nulo, lote nulo y lote nulo para el detalle | Entrada nula | Los tres métodos devuelven `null`. |
| **CP-MSI-14** | `toDTO` de un lote | Lote vencido ayer (10, 4) y lote que vence hoy (10, 4) | Mapea identificación, vencimiento, `vencido` y cantidades | El vencido tiene `vencido: true` y `vencida: 10`; el que vence hoy `vencido: false` y `disponible: 6`. |
| **CP-MSI-15** | `toDetalleDTO` | Lote `L-1` de la malta con ID 1, `10` y `4` | Mapea el lote junto con su insumo | Informa `idInsumo`, `nombreInsumo`, `unidadDeMedida`, identificación, vencimiento, `vencido: false` y `disponible: 6`. |
| **CP-MSI-16** | Tipo del insumo | Malta, lúpulo y levadura | Se resuelve por la clase concreta | `MALTA`, `LUPULO` y `LEVADURA`. |
| **CP-MSI-17** | Clase desconocida | Una subclase de `InsumoEntity` que no es ninguna de las tres | El tipo no se puede resolver | Lanza `IllegalArgumentException`. |
