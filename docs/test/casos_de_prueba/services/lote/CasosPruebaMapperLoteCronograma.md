## Casos de prueba de `MapperLoteCronograma`

**Tipo de prueba:** unitaria (clase `MapperLoteCronogramaTest`), sin Spring ni mocks. Verifica la conversión de un `LoteEntity` en el `LoteCronogramaResponseDTO` que usa el cronograma de la planificación. El mapper solo lee las relaciones ya cargadas de la entidad (planificación, versión de receta y etapas con su equipamiento) y no ejecuta consultas. No calcula el atraso ni el corrimiento: `diasDeAtraso` y `diasDeDesplazamiento` salen en `0` y los completa `LoteServicioImpl` al consultar el cronograma (ver `CasosPruebaLote.md`, secciones 12 y 13).

**Identificador del lote:** `identificadorInterno` se copia tal como está guardado en el lote (`<NOMBRE>-R<idReceta>-<n>`, ej. `PILSEN_CLASICA-R4-3`, calculado al registrarlo). El mapper no lo recalcula a partir del nombre actual de la versión de receta, que se informa aparte en `nombreReceta`.

**Regla de conversión:** el fermentador es el equipamiento de la etapa `FERMENTACION`; el molino, el macerador y la olla de hervor son los equipamientos de las etapas `MOLIENDA`, `MACERACION` y `HERVIDO`. `etapaActual` es el tipo de la etapa con `estado = EN_CURSO`, o `null` si ninguna lo está. Si el lote no tiene la etapa de un tipo, los campos del equipamiento correspondiente quedan en `null`.

---

### `toDTO(LoteEntity)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MLC-01** | Lote nulo | `loteEntity: null` | `loteEntity == null` $\rightarrow$ **TRUE** | Retorna `null`. |
| **CP-MLC-02** | Datos generales del lote | Lote id 10, `identificadorInterno: "PILSEN_CLASICA-R4-3"`, `estado: PENDIENTE`, `volumenObjetivo: 300.0`, `fechaInicioEstimada: 2026-11-02`, `fechaFinalizacionEstimada: 2026-11-20`, planificación id 5 con versión de receta de nombre "Pilsen Clásica" | Los datos del lote y de su planificación se copian al DTO | El DTO tiene los mismos valores en `id`, `identificadorInterno`, `estado`, `volumenObjetivo`, `fechaInicioEstimada` y `fechaFinalizacionEstimada`; `idPlanificacionProduccion: 5` y `nombreReceta: "Pilsen Clásica"`. |
| **CP-MLC-03** | Equipos y fermentador de un lote PENDIENTE | Lote `PENDIENTE` con las 6 etapas (todas `PENDIENTE`): Molienda con molino "MOL-01", Maceración con macerador "MAC-02", Hervido con olla "OLL-03", y Fermentación, Maduración y Envasado con el fermentador id 40 "FER-04" | Cada equipamiento se resuelve por el tipo de su etapa | `identificadorInternoMolino: "MOL-01"`, `identificadorInternoMacerador: "MAC-02"`, `identificadorInternoOllaHervor: "OLL-03"`, `idFermentador: 40` e `identificadorInternoFermentador: "FER-04"`. |
| **CP-MLC-04** | Fechas reales nulas en un lote PENDIENTE | Lote `PENDIENTE` sin `fechaInicio`, `fechaFinalizacion` ni `fechaCancelacion` | Las fechas reales son nulas hasta que ocurren | `fechaInicio`, `fechaFinalizacion` y `fechaCancelacion` son `null`. |
| **CP-MLC-05** | Etapa actual: la etapa EN_CURSO | Lote `EN_EJECUCION` con Molienda y Maceración `FINALIZADA`, Hervido `EN_CURSO` y el resto `PENDIENTE`; `fechaInicio: 2026-11-02T08:00` | `estado == EN_CURSO` en la etapa de Hervido | `etapaActual: HERVIDO` y `fechaInicio: 2026-11-02T08:00`. |
| **CP-MLC-06** | Etapa actual nula sin ninguna etapa en curso | (a) Lote `PENDIENTE` con las 6 etapas `PENDIENTE`; (b) lote `FINALIZADO` con las 6 etapas `FINALIZADA` | Ninguna etapa tiene `estado = EN_CURSO` | En ambos casos `etapaActual: null`. En (b) `fechaInicio` y `fechaFinalizacion` se copian al DTO. |
| **CP-MLC-07** | Lote cancelado con fechas reales nulas | Lote `CANCELADO` con `fechaCancelacion: 2026-11-05T10:30`, `fechaInicio: null`, `fechaFinalizacion: null`, 6 etapas `PENDIENTE` | Un lote cancelado antes de iniciar no tiene fechas reales de inicio ni de fin | `estado: CANCELADO`, `fechaCancelacion: 2026-11-05T10:30`, `fechaInicio: null`, `fechaFinalizacion: null` y `etapaActual: null`. Mapea sin lanzar excepción. |
| **CP-MLC-08** | Lote sin etapas | Lote con la lista de etapas vacía | No existe etapa de ningún tipo $\rightarrow$ ningún equipamiento que resolver | `idFermentador`, `identificadorInternoFermentador`, `identificadorInternoMolino`, `identificadorInternoMacerador`, `identificadorInternoOllaHervor` y `etapaActual` son `null`; el resto de los campos se copia. No lanza excepción. |
| **CP-MLC-09** | Lote sin la etapa de algún tipo de equipo | Lote con solo las etapas Molienda (molino "MOL-01") y Fermentación (fermentador id 40 "FER-04") | Los tipos de etapa ausentes dejan sus campos en `null` | `identificadorInternoMolino: "MOL-01"`, `idFermentador: 40` e `identificadorInternoFermentador: "FER-04"`; `identificadorInternoMacerador` e `identificadorInternoOllaHervor` son `null`. No lanza excepción. |

### `toDTOList(List<LoteEntity>)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MLC-10** | Lista vacía | `lotes: []` | La lista de entrada no tiene elementos | Retorna una lista vacía. |
| **CP-MLC-11** | Lista con elementos conserva el orden | 3 lotes con ids 12, 10 y 11, en ese orden | Cada lote se mapea con `toDTO` y el orden se conserva | Retorna 3 `LoteCronogramaResponseDTO` con ids 12, 10 y 11, en ese orden. |
| **CP-MLC-12** | El mapper no calcula atraso ni corrimiento | Lote `EN_EJECUCION` con `fechaFinalizacionEstimada` anterior a hoy y lote `PENDIENTE` con `fechaInicioEstimada` anterior a hoy | El atraso depende de la cola completa del fermentador y no de un solo lote | En ambos DTO `diasDeAtraso: 0` y `diasDeDesplazamiento: 0`. |
| **CP-MLC-13** | Etapa de fermentación sin equipamiento | Lote con una etapa `FERMENTACION` cuyo equipamiento es `null` | El mapper no debe fallar por datos incompletos | No lanza excepción; el DTO conserva el id del lote y el fermentador queda en `null`. |
| **CP-MLC-14** | El identificador guardado no se recalcula con el nombre actual de la receta | Lote con `identificadorInterno: "PILSEN_CLASICA-R4-3"` cuya planificación apunta a una versión de receta que hoy se llama "Pilsen Reserva" | El código se calculó al registrar el lote y se conserva | `identificadorInterno: "PILSEN_CLASICA-R4-3"` y `nombreReceta: "Pilsen Reserva"`. |
