### 1. `filtrarLotes(Long idReceta, String identificadorInterno, EstadoLote estado, TipoEtapa etapaActual, Double volumenObjetivo, Pageable pageable)`

`identificadorInterno` es el número autogenerado al registrar el lote (no lo tipea el usuario), por lo que se busca por coincidencia parcial, sin distinguir mayúsculas/minúsculas — igual que en el resto de los `filtrarX` del sistema. `idReceta`, `estado` y `volumenObjetivo` son coincidencia exacta. `etapaActual` filtra por la etapa que tenga `estado = EN_CURSO` en ese lote puntual (no por si el lote alguna vez tuvo una etapa de ese tipo, ya que todo lote tiene las 6). Todos los parámetros son opcionales, `null` = no filtra por ese criterio.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FL-01**|Filtra por los 5 criterios informados|`idReceta: 3L`, `identificadorInterno: "IPA"`, `estado: EN_EJECUCION`, `etapaActual: FERMENTACION`, `volumenObjetivo: 20.0`, `pageable: PageRequest.of(0, 10)`, BD con 2 lotes que cumplen todos los criterios|`filtrarLotes(3L, "IPA", EN_EJECUCION, FERMENTACION, 20.0, pageable)` contiene elementos|Retorna `Page<LoteResponseDTO>` con 2 elementos mapeados.|
|**CP-FL-02**|Los 5 parámetros nulos no restringen la búsqueda|`idReceta: null`, `identificadorInterno: null`, `estado: null`, `etapaActual: null`, `volumenObjetivo: null`, `pageable: PageRequest.of(0, 10)`|El service propaga los 5 parámetros nulos tal cual al repositorio|Retorna `Page<LoteResponseDTO>` con todos los lotes registrados (equivalente a no filtrar).|
|**CP-FL-03**|Consulta sin coincidencias|`identificadorInterno: "Inexistente"`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)`|`filtrarLotes("Inexistente", ...)` está vacío|Retorna `Page<LoteResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Lote encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `LoteResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Lote inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el lote con ID: 99".|

### 3. `registrarLote(LoteFormDTO loteFormDTO)`

Escenario base salvo indicación contraria: receta con 2 maltas (Pilsen 100kg 80% EP, Caramelo 20kg 74% EP — ejemplo de `EscaladoDeMalta.md`), OG 1.050, volumen objetivo 20L, macerador con eficiencia de maceración 75%, olla de hervor con pérdida por trub 2L y evaporación 3L/h, fermentador con capacidad útil 20L, molino con rendimiento 5 kg/h, configuración con velocidad de envasado 10 L/h.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RL-01**|Planificación de producción no encontrada|`idPlanificacionProduccion: 99L` (no existe)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. `verifyNoInteractions` sobre los 4 repos de equipamiento.|
|**CP-RL-02**|Planificación de producción no PENDIENTE|Planificación existente con `estado: FINALIZADA`|`estado != PENDIENTE` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`.|
|**CP-RL-03**|Molino no encontrado|`idMolino: 99L` (no existe)|`molinoRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta macerador/olla/fermentador.|
|**CP-RL-04**|Macerador no encontrado|`idMacerador: 99L` (no existe), molino OK|`maceradorRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta olla/fermentador.|
|**CP-RL-05**|Olla de hervor no encontrada|`idOllaHervor: 99L` (no existe), molino/macerador OK|`ollaHervorRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No consulta fermentador.|
|**CP-RL-06**|Fermentador no encontrado|`idFermentador: 99L` (no existe), resto OK|`fermentadorRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`.|
|**CP-RL-07**|Volumen objetivo nulo|`volumenObjetivo: null`|`volumenObjetivo == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`.|
|**CP-RL-08**|Volumen objetivo en cero _(Límite)_|`volumenObjetivo: 0.0`|`volumenObjetivo <= 0` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`.|
|**CP-RL-09**|Volumen objetivo supera la capacidad del fermentador|`volumenObjetivo: 25.0` (capacidad útil: 20.0)|`volumenObjetivo > capacidadUtil` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`.|
|**CP-RL-10**|Volumen objetivo igual a la capacidad del fermentador _(Límite válido)_|`volumenObjetivo: 20.0` (capacidad útil: 20.0)|`volumenObjetivo > capacidadUtil` $\rightarrow$ **FALSE**|No lanza por esta regla; el proceso continúa (no se verifica persistencia acá, ver CP-RL-14).|
|**CP-RL-11**|Volumen pre-hervor supera la capacidad de la olla de hervor|Olla con `capacidadUtil: 24.0` (pre-hervor calculado: 25.0)|`volumenPreHervor > capacidadUtil` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`.|
|**CP-RL-12**|Volumen de mezcla supera la capacidad del macerador|Macerador con `capacidadUtil: 20.0` (mezcla calculada: ≈21.26)|`volumenMezclaMacerador > capacidadUtil` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`.|
|**CP-RL-13**|Configuración de planificación de producción no encontrada|`configuracionPlanificacionProduccionRepository.findById(1L)` $\rightarrow$ vacío|Se necesita para `fechaFinalizacionEstimada`|Lanza `RecursoNoEncontradoException`. (El contador de la receta ya se incrementó en memoria en este punto; `@Transactional` revierte el cambio real en la base de datos.)|
|**CP-RL-14**|Alta exitosa — estructura _(Camino feliz)_|Escenario base completo, válido|Todas las validaciones $\rightarrow$ **FALSE**|Persiste el lote con `estado = PENDIENTE`, `identificadorInterno = "IPA Test-3"` (contador previo), incrementa `contadorLotes` de la receta a 4, y crea las 6 etapas en `PENDIENTE` con el equipamiento correcto (Molino→Molienda, Macerador→Maceración, Olla→Hervido, Fermentador→Fermentación/Maduración/Envasado).|
|**CP-RL-15**|Alta exitosa — fechas _(Camino feliz)_|Escenario base completo, fermentador sin lotes previos|`buscarUltimaFechaFinalizacionEstimadaPorFermentador` $\rightarrow$ **Optional.empty()**|`fechaInicioEstimada = hoy`; `fechaFinalizacionEstimada = hoy + 22 días` (1 día de brew day/envasado + 14 de fermentación + 7 de maduración).|
|**CP-RL-16**|Fecha de inicio encolada tras otro lote del mismo fermentador|`buscarUltimaFechaFinalizacionEstimadaPorFermentador` $\rightarrow$ `Optional.of(2026-10-01)`|Existe un lote previo planificado en ese fermentador|`fechaInicioEstimada = 2026-10-02`; `fechaFinalizacionEstimada = 2026-10-24`.|
|**CP-RL-17**|Delegación correcta de fermentador y estados vigentes al cronograma|Escenario base completo|`buscarUltimaFechaFinalizacionEstimadaPorFermentador(fermentador, estados)`|Se invoca exactamente con el fermentador seleccionado (mismo `id`) y `List.of(PENDIENTE, EN_EJECUCION)` — ni otro fermentador ni otros estados (FINALIZADO/CANCELADO) participan del cálculo.|
|**CP-RL-18**|Verificación numérica del escalado (malta y agua)|Escenario base completo (ejemplo de `EscaladoDeMalta.md`)|Cálculo de `masaMaltaEscalada`, `volumenPreHervor`, `volumenMezclaMacerador`|`masaMaltaEscalada ≈ 4.396 kg`, `volumenPreHervor = 25.0 L`, `volumenMezclaMacerador ≈ 21.264 L` — ninguno lanza excepción (todos ≤ su capacidad útil).|

### 4. `iniciarLote(Long id)`

Escenario base salvo indicación contraria: lote registrado en `PENDIENTE` con sus 6 etapas y equipamiento asignado, todos los equipos en `DISPONIBLE`.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-IL-01**|Lote no encontrado|`id: 99L` (no existe)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`.|
|**CP-IL-02**|Lote en estado EN_EJECUCION|Lote existente con `estado: EN_EJECUCION`|`estado != PENDIENTE` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`.|
|**CP-IL-03**|Lote en estado CANCELADO|Lote existente con `estado: CANCELADO`|`estado != PENDIENTE` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`.|
|**CP-IL-04**|Molino no encontrado al iniciar|`molinoRepository.buscarPorIdParaCambiarEstadoOperativo(id)` $\rightarrow$ **Optional.empty()**|Equipo dado de baja después de registrar el lote|Lanza `RecursoNoEncontradoException`.|
|**CP-IL-05**|Molino no disponible|Molino con `estadoOperativo: EN_USO`|`estadoOperativo != DISPONIBLE` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` mencionando "El molino".|
|**CP-IL-06**|Macerador no disponible|Macerador con `estadoOperativo: EN_LIMPIEZA`|`estadoOperativo != DISPONIBLE` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` mencionando "El macerador".|
|**CP-IL-07**|Olla de hervor no disponible|Olla con `estadoOperativo: EN_MANTENIMIENTO`|`estadoOperativo != DISPONIBLE` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` mencionando "La olla de hervor".|
|**CP-IL-08**|Fermentador no disponible|Fermentador con `estadoOperativo: EN_USO`|`estadoOperativo != DISPONIBLE` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` mencionando "El fermentador".|
|**CP-IL-09**|Stock insuficiente de malta|Requerimiento ≈3.70kg; stock disponible 2.0kg|`totalDisponible < cantidadRequerida` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No reserva ni persiste ningún `LoteInsumoEntity`/`ReservaInsumoEntity` (`verifyNoInteractions(reservaInsumoRepository)`, `loteInsumoRepository` nunca invoca `save`).|
|**CP-IL-10**|Stock insuficiente de lúpulo|Malta con stock suficiente; lúpulo requerido ≈14.4g, disponible 5.0g|`totalDisponible < cantidadRequerida` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin reservas ni persistencia.|
|**CP-IL-11**|Stock insuficiente de levadura|Malta y lúpulo con stock suficiente; levadura requerida 7.5g, disponible 2.0g|`totalDisponible < cantidadRequerida` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin reservas ni persistencia.|
|**CP-IL-12**|FEFO: se necesitan 2 lotes de insumo|Levadura requerida 7.5g; lote de insumo A (vence antes) disponible 5.0g, lote B (vence después) disponible 10.0g|Recorrido FEFO ordenado por vencimiento|Se reserva 5.0g del lote A (lo agota) y 2.5g del lote B. Se persisten 2 `ReservaInsumoEntity`.|
|**CP-IL-13**|FEFO: un solo lote de insumo alcanza|Levadura requerida 7.5g; único lote de insumo disponible 10.0g|Recorrido FEFO con un solo lote|Se reserva 7.5g de ese lote. Se persiste 1 sola `ReservaInsumoEntity`.|
|**CP-IL-14**|Fórmula de lúpulo HERVOR (Tinseth)|Receta sintética de 1 solo lúpulo HERVOR (AA 5.5%, Pellet, 60min), OG 1.050, IBU 35, volumen objetivo 20L (aísla la fórmula)|Cálculo de Tinseth paso a paso|Gramos reservados para ese lúpulo coinciden con el cálculo manual de Tinseth (factor de densidad, factor de tiempo, corrección Pellet 1.10).|
|**CP-IL-15**|Fórmula de lúpulo WHIRLPOOL/DRY_HOP (lineal)|1 lúpulo DRY_HOP, cantidad base 15g, volumen base 20L, volumen objetivo 100L (ejemplo de `EscaladoDelLupulo.md`)|`Gramos = CantidadBase × (Vobj/Vbase)`|Gramos reservados = 15 × (100/20) = 75.0g exactos.|
|**CP-IL-16**|Fórmula de levadura|1 levadura ALE, OG 1.050, volumen objetivo 20L, 20.000 millones de células/g (ejemplo Caso 1 de `EscaladoDeLevadura.md`)|Cálculo célula-a-gramo paso a paso|Gramos reservados = 9.375g exactos.|
|**CP-IL-17**|Mismo lúpulo en dos detalles de etapas distintas (WHIRLPOOL en Hervido + DRY_HOP en Maduración) NO se fusiona|1 lúpulo usado en 2 `DetalleLupuloEntity` (10g WHIRLPOOL → etapa Hervido, 5g DRY_HOP → etapa Maduración, volumen objetivo = volumen base)|`requerimientosPorInsumoYEtapa.merge(...)` clave = (idInsumo, idEtapa) — distintas etapas, no fusiona|Se generan 2 `ReservaInsumoEntity` separadas, cada una atada a su propia `EtapaLoteEntity` (Hervido 10g, Maduración 5g) — la cantidad total reservada sobre el lote de insumo físico sigue siendo 15.0g (stock compartido), pero repartida, no fusionada en una sola reserva.|
|**CP-IL-18**|Inicio exitoso completo _(Camino feliz)_|Escenario base completo, stock suficiente de los 3 insumos|Todas las validaciones $\rightarrow$ **FALSE**|`estado = EN_EJECUCION`, `fechaInicio` seteado; los 4 equipamientos persistidos con `estadoOperativo = EN_USO`; etapa Molienda en `EN_CURSO` con `fechaInicio` seteado; las otras 5 etapas siguen en `PENDIENTE`; se persisten 3 `ReservaInsumoEntity` (una por insumo).|
|**CP-IL-19**|Reporta todos los insumos con stock insuficiente en un solo error|Malta insuficiente, lúpulo suficiente, levadura insuficiente|`obtenerYValidarStockDisponible` evalúa TODOS los insumos antes de lanzar (no corta en el primero)|Lanza `ReglaNegocioException` con un único mensaje que menciona tanto la malta como la levadura faltantes.|

### 5. `cancelarLote(Long id, CancelacionLoteFormDTO cancelacionLoteFormDTO)`

Cancelar un lote NO revierte los consumos de insumo ya ejecutados en sus etapas — esos quedan firmes. Escenario base salvo indicación contraria: lote en `EN_EJECUCION` con sus 6 etapas y equipamiento asignado, motivo de cancelación informado.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CL-01**|Lote no encontrado|`id: 99L` (no existe)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`.|
|**CP-CL-02**|Motivo de cancelación nulo|`motivoCancelacion: null`|`motivoCancelacion == null` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. `verifyNoInteractions` sobre `loteRepository` y el resto de los repos — la validación es previa a cualquier consulta.|
|**CP-CL-03**|Motivo de cancelación vacío (solo espacios)|`motivoCancelacion: "   "`|`motivoCancelacion.isBlank()` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones.|
|**CP-CL-04**|Lote en estado FINALIZADO|Lote existente con `estado: FINALIZADO`|`estado != PENDIENTE \| EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No libera reservas ni toca equipamiento.|
|**CP-CL-05**|Lote ya CANCELADO|Lote existente con `estado: CANCELADO`|`estado != PENDIENTE \| EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. No libera reservas ni toca equipamiento.|
|**CP-CL-06**|Ninguna etapa EN_CURSO (lote PENDIENTE)|Las 6 etapas en `PENDIENTE`|Todas las validaciones $\rightarrow$ **FALSE**|Los 4 equipamientos (Molino, Macerador, Olla de Hervor, Fermentador) pasan a `DISPONIBLE`. El Fermentador se guarda una sola vez pese a estar asociado a 3 etapas.|
|**CP-CL-07**|Molienda EN_CURSO|Etapa Molienda `EN_CURSO`, resto `PENDIENTE`|Prioridad EN_CURSO &gt; PENDIENTE por equipamiento|El Molino pasa a `EN_LIMPIEZA`; Macerador, Olla de Hervor y Fermentador pasan a `DISPONIBLE`.|
|**CP-CL-08**|Fermentación EN_CURSO (equipamiento compartido)|Etapa Fermentación `EN_CURSO`; Maduración y Envasado (mismo Fermentador) `PENDIENTE`|El Fermentador se resuelve por equipamiento, no por etapa aislada|El Fermentador pasa a `EN_LIMPIEZA` — NO a `DISPONIBLE` — pese a que 2 de sus 3 etapas están `PENDIENTE`. Molino, Macerador y Olla de Hervor (sus propias etapas `PENDIENTE`) pasan a `DISPONIBLE`.|
|**CP-CL-09**|Liberación de reservas de insumo|2 `ReservaInsumoEntity` sobre 2 `LoteInsumoEntity` distintos (4.0 y 6.0 reservados)|`liberarReservasDelLote` recorre todas las reservas del lote|Cada `LoteInsumoEntity` queda con `cantidadReservada = 0.0`, se persiste, y las 2 `ReservaInsumoEntity` se eliminan (`reservaInsumoRepository.deleteAll(...)`).|
|**CP-CL-10**|Cancelación exitosa _(Camino feliz)_|Escenario base completo|Todas las validaciones $\rightarrow$ **FALSE**|`estado = CANCELADO`, `fechaCancelacion` seteada (justo ahora), `motivoCancelacion` guardado con el valor informado.|
|**CP-CL-11**|Etapa ya FINALIZADA no se toca|Etapa Maceración forzada a `FINALIZADA` (hoy no hay feature que la lleve ahí, pero el código la contempla)|`resolverEstadoOperativoAlCancelar(FINALIZADA)` $\rightarrow$ **null**|El Macerador no se consulta ni se persiste (`verifyNoInteractions(maceradorRepository)`) — mantiene el estado que ya tenía.|

### 6. `finalizarMolienda(Long id)`

No solicita ningún dato al usuario. Escenario base salvo indicación contraria: lote en `EN_EJECUCION` con sus 6 etapas y equipamiento asignado.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FM-01**|Lote no encontrado|`id: 99L` (no existe)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. `verifyNoInteractions(molinoRepository)`.|
|**CP-FM-02**|Lote no está EN_EJECUCION|Lote existente con `estado: PENDIENTE`|`estado != EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con el molino.|
|**CP-FM-03**|La etapa actual no es Molienda|Etapa Maceración `EN_CURSO` (no Molienda)|`etapaActual.getEtapa() != MOLIENDA` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con el molino.|
|**CP-FM-04**|Avance exitoso _(Camino feliz)_|Etapa Molienda `EN_CURSO`, resto `PENDIENTE`|Todas las validaciones $\rightarrow$ **FALSE**|Molienda pasa a `FINALIZADA` con `fechaFinalizacion` seteada; Maceración pasa a `EN_CURSO` con `fechaInicio` seteada; las otras 4 etapas siguen en `PENDIENTE`; el Molino se persiste con `estadoOperativo = EN_LIMPIEZA`; no se toca Macerador/Olla de Hervor/Fermentador.|

### 7. `finalizarMaceracion(Long id)`

No solicita ningún dato al usuario. Escenario base salvo indicación contraria: lote en `EN_EJECUCION` con sus 6 etapas y equipamiento asignado, etapa actual Maceración `EN_CURSO`.

Valida, además de lo habitual, que el consumo de CADA insumo requerido de la etapa alcance el porcentaje mínimo configurado en `ConfiguracionProduccionEntity.porcentajeMinimoConsumoParaAvanzarEtapa` (Sección 2 de esa configuración): reutiliza `ConsumoInsumoServicio.filtrarInsumosRequeridos(idEtapaMaceracion)` (mismos datos que ve el operario en la pantalla de gestión de consumos) en vez de recalcular esa lógica acá. La comparación es `cantidadConsumida >= cantidadRequerida * (porcentajeMinimo / 100)`, insumo por insumo — no en total agregado, para que un consumo de más de un insumo no tape el olvido de otro.

Al finalizar, además de avanzar la etapa y pasar el macerador a `EN_LIMPIEZA`, libera las reservas de insumo que hayan quedado sin consumir de la etapa de Maceración puntual (`IReservaInsumoRepository.findByEtapaLoteId`, no las de todo el lote).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FMC-01**|Lote no encontrado|`id: 99L` (no existe)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. `verifyNoInteractions(maceradorRepository, consumoInsumoServicio)`.|
|**CP-FMC-02**|Lote no está EN_EJECUCION|Lote existente con `estado: PENDIENTE`|`estado != EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con el macerador ni con `consumoInsumoServicio`.|
|**CP-FMC-03**|La etapa actual no es Maceración|Etapa Molienda `EN_CURSO` (no Maceración)|`etapaActual.getEtapa() != MACERACION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con el macerador ni con `consumoInsumoServicio`.|
|**CP-FMC-04**|No existe la configuración de producción|`configuracionProduccionRepository.findById(SINGLETON_ID)` $\rightarrow$ **Optional.empty()**|La configuración no existe|Lanza `RecursoNoEncontradoException`. Sin interacciones con el macerador ni con `consumoInsumoServicio`.|
|**CP-FMC-05**|Algún insumo requerido no alcanza el porcentaje mínimo|`porcentajeMinimoConsumoParaAvanzarEtapa: 80.0`; Malta Pilsen con `cantidadRequerida: 10.0`, `cantidadConsumida: 7.0` (70%)|`7.0 < 10.0 * 0.80` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` mencionando "Malta Pilsen". Sin interacciones con el macerador ni con las reservas. No se llama a `loteRepository.save()`.|
|**CP-FMC-06**|Avance exitoso _(Camino feliz)_|`porcentajeMinimoConsumoParaAvanzarEtapa: 80.0`; Malta Pilsen con `cantidadRequerida: 10.0`, `cantidadConsumida: 8.0` (exactamente 80%, límite); 1 reserva de `2.0` sobre un lote de insumo con `cantidadReservada: 2.0`|`8.0 >= 10.0 * 0.80` $\rightarrow$ **TRUE** (alcanza); resto de validaciones $\rightarrow$ **FALSE**|Maceración pasa a `FINALIZADA` con `fechaFinalizacion` seteada; Hervido pasa a `EN_CURSO` con `fechaInicio` seteada; las otras 4 etapas siguen en `PENDIENTE`; el Macerador se persiste con `estadoOperativo = EN_LIMPIEZA`; la reserva se libera (el lote de insumo queda con `cantidadReservada = 0.0` y se persiste, y la reserva se elimina); no se toca Molino/Olla de Hervor/Fermentador.|

### 8. `finalizarHervido(Long id)`

No solicita ningún dato al usuario. Escenario base salvo indicación contraria: lote en `EN_EJECUCION` con sus 6 etapas y equipamiento asignado, etapa actual Hervido `EN_CURSO`. Mismo patrón que `finalizarMaceracion` (sección 7), aplicado a la transición Hervido → Fermentación: valida el porcentaje mínimo de consumo vía `ConsumoInsumoServicio.filtrarInsumosRequeridos`, pasa la olla de hervor a `EN_LIMPIEZA`, y libera las reservas de insumo que hayan quedado sin consumir de la etapa de Hervido puntual.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FH-01**|Lote no encontrado|`id: 99L` (no existe)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. `verifyNoInteractions(ollaHervorRepository, consumoInsumoServicio)`.|
|**CP-FH-02**|Lote no está EN_EJECUCION|Lote existente con `estado: PENDIENTE`|`estado != EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con la olla de hervor ni con `consumoInsumoServicio`.|
|**CP-FH-03**|La etapa actual no es Hervido|Etapa Maceración `EN_CURSO` (no Hervido)|`etapaActual.getEtapa() != HERVIDO` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con la olla de hervor ni con `consumoInsumoServicio`.|
|**CP-FH-04**|No existe la configuración de producción|`configuracionProduccionRepository.findById(SINGLETON_ID)` $\rightarrow$ **Optional.empty()**|La configuración no existe|Lanza `RecursoNoEncontradoException`. Sin interacciones con la olla de hervor ni con `consumoInsumoServicio`.|
|**CP-FH-05**|Algún insumo requerido no alcanza el porcentaje mínimo|`porcentajeMinimoConsumoParaAvanzarEtapa: 80.0`; Lúpulo Cascade con `cantidadRequerida: 10.0`, `cantidadConsumida: 7.0` (70%)|`7.0 < 10.0 * 0.80` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` mencionando "Cascade". Sin interacciones con la olla de hervor ni con las reservas. No se llama a `loteRepository.save()`.|
|**CP-FH-06**|Avance exitoso _(Camino feliz)_|`porcentajeMinimoConsumoParaAvanzarEtapa: 80.0`; Lúpulo Cascade con `cantidadRequerida: 10.0`, `cantidadConsumida: 8.0` (exactamente 80%, límite); 1 reserva de `2.0` sobre un lote de insumo con `cantidadReservada: 2.0`|`8.0 >= 10.0 * 0.80` $\rightarrow$ **TRUE** (alcanza); resto de validaciones $\rightarrow$ **FALSE**|Hervido pasa a `FINALIZADA` con `fechaFinalizacion` seteada; Fermentación pasa a `EN_CURSO` con `fechaInicio` seteada; las otras 4 etapas siguen en `PENDIENTE`; la Olla de Hervor se persiste con `estadoOperativo = EN_LIMPIEZA`; la reserva se libera (el lote de insumo queda con `cantidadReservada = 0.0` y se persiste, y la reserva se elimina); no se toca Molino/Macerador/Fermentador.|

### 9. `finalizarFermentacion(Long id)`

No solicita ningún dato al usuario. Escenario base salvo indicación contraria: lote en `EN_EJECUCION` con sus 6 etapas y equipamiento asignado, etapa actual Fermentación `EN_CURSO`. Mismo patrón que `finalizarMaceracion`/`finalizarHervido` (secciones 7 y 8), aplicado a la transición Fermentación → Maduración, con una diferencia clave: **no cambia el estado operativo de ningún equipamiento** — el fermentador es compartido por Fermentación, Maduración y Envasado, así que sigue `EN_USO` sin interrupción hasta que termine la última de esas tres etapas. Sí valida el porcentaje mínimo de consumo y libera las reservas de insumo que hayan quedado sin consumir de la etapa de Fermentación puntual, igual que las anteriores.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FF-01**|Lote no encontrado|`id: 99L` (no existe)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. `verifyNoInteractions(consumoInsumoServicio)`.|
|**CP-FF-02**|Lote no está EN_EJECUCION|Lote existente con `estado: PENDIENTE`|`estado != EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con `consumoInsumoServicio`.|
|**CP-FF-03**|La etapa actual no es Fermentación|Etapa Hervido `EN_CURSO` (no Fermentación)|`etapaActual.getEtapa() != FERMENTACION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con `consumoInsumoServicio`.|
|**CP-FF-04**|No existe la configuración de producción|`configuracionProduccionRepository.findById(SINGLETON_ID)` $\rightarrow$ **Optional.empty()**|La configuración no existe|Lanza `RecursoNoEncontradoException`. Sin interacciones con `consumoInsumoServicio`.|
|**CP-FF-05**|Algún insumo requerido no alcanza el porcentaje mínimo|`porcentajeMinimoConsumoParaAvanzarEtapa: 80.0`; Levadura Ale con `cantidadRequerida: 10.0`, `cantidadConsumida: 7.0` (70%)|`7.0 < 10.0 * 0.80` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` mencionando "Levadura Ale". Sin interacciones con las reservas. No se llama a `loteRepository.save()`.|
|**CP-FF-06**|Avance exitoso _(Camino feliz)_|`porcentajeMinimoConsumoParaAvanzarEtapa: 80.0`; Levadura Ale con `cantidadRequerida: 10.0`, `cantidadConsumida: 8.0` (exactamente 80%, límite); 1 reserva de `2.0` sobre un lote de insumo con `cantidadReservada: 2.0`|`8.0 >= 10.0 * 0.80` $\rightarrow$ **TRUE** (alcanza); resto de validaciones $\rightarrow$ **FALSE**|Fermentación pasa a `FINALIZADA` con `fechaFinalizacion` seteada; Maduración pasa a `EN_CURSO` con `fechaInicio` seteada; las otras 4 etapas siguen en `PENDIENTE`; la reserva se libera (el lote de insumo queda con `cantidadReservada = 0.0` y se persiste, y la reserva se elimina); **ningún equipamiento cambia de estado** (`verifyNoInteractions` sobre Molino, Macerador, Olla de Hervor y Fermentador).|

### 10. `finalizarMaduracion(Long id)`

No solicita ningún dato al usuario. Escenario base salvo indicación contraria: lote en `EN_EJECUCION` con sus 6 etapas y equipamiento asignado, etapa actual Maduración `EN_CURSO`. Idéntica a `finalizarFermentacion` (sección 9), aplicada a la transición Maduración → Envasado: tampoco cambia el estado operativo de ningún equipamiento (el fermentador sigue `EN_USO` hasta terminar el Envasado). La validación del porcentaje mínimo de consumo también aplica acá — es infrecuente que Maduración requiera insumos, pero puede haberlos (por ejemplo, un lúpulo de Dry Hop que se tira en esta etapa).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FMD-01**|Lote no encontrado|`id: 99L` (no existe)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. `verifyNoInteractions(consumoInsumoServicio)`.|
|**CP-FMD-02**|Lote no está EN_EJECUCION|Lote existente con `estado: PENDIENTE`|`estado != EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con `consumoInsumoServicio`.|
|**CP-FMD-03**|La etapa actual no es Maduración|Etapa Fermentación `EN_CURSO` (no Maduración)|`etapaActual.getEtapa() != MADURACION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con `consumoInsumoServicio`.|
|**CP-FMD-04**|No existe la configuración de producción|`configuracionProduccionRepository.findById(SINGLETON_ID)` $\rightarrow$ **Optional.empty()**|La configuración no existe|Lanza `RecursoNoEncontradoException`. Sin interacciones con `consumoInsumoServicio`.|
|**CP-FMD-05**|Algún insumo requerido (ej. un lúpulo de Dry Hop) no alcanza el porcentaje mínimo|`porcentajeMinimoConsumoParaAvanzarEtapa: 80.0`; Lúpulo "Citra Dry Hop" con `cantidadRequerida: 10.0`, `cantidadConsumida: 7.0` (70%)|`7.0 < 10.0 * 0.80` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException` mencionando "Citra Dry Hop". Sin interacciones con las reservas. No se llama a `loteRepository.save()`.|
|**CP-FMD-06**|Avance exitoso _(Camino feliz)_|`porcentajeMinimoConsumoParaAvanzarEtapa: 80.0`; Lúpulo "Citra Dry Hop" con `cantidadRequerida: 10.0`, `cantidadConsumida: 8.0` (exactamente 80%, límite); 1 reserva de `2.0` sobre un lote de insumo con `cantidadReservada: 2.0`|`8.0 >= 10.0 * 0.80` $\rightarrow$ **TRUE** (alcanza); resto de validaciones $\rightarrow$ **FALSE**|Maduración pasa a `FINALIZADA` con `fechaFinalizacion` seteada; Envasado pasa a `EN_CURSO` con `fechaInicio` seteada; las otras 4 etapas siguen en `PENDIENTE`; la reserva se libera (el lote de insumo queda con `cantidadReservada = 0.0` y se persiste, y la reserva se elimina); **ningún equipamiento cambia de estado** (`verifyNoInteractions` sobre Molino, Macerador, Olla de Hervor y Fermentador).|

### 11. `finalizarEnvasado(Long id)`

No solicita ningún dato al usuario. Escenario base salvo indicación contraria: lote en `EN_EJECUCION` con sus 6 etapas y equipamiento asignado, etapa actual Envasado `EN_CURSO`. A diferencia de las 4 transiciones anteriores, Envasado es la **última** etapa: no hay una etapa siguiente a la que avanzar, no se valida porcentaje mínimo de consumo ni se liberan reservas (en Envasado no se registran consumos de insumo, solo envasados — el traspaso de cerveza del fermentador a los barriles, gestionado por `EnvasadoLoteServicio`, no por `LoteServicioImpl`). En su lugar, esta operación cierra el lote completo.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FE-01**|Lote no encontrado|`id: 99L` (no existe)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. `verifyNoInteractions(fermentadorRepository)`.|
|**CP-FE-02**|Lote no está EN_EJECUCION|Lote existente con `estado: PENDIENTE`|`estado != EN_EJECUCION` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con el fermentador.|
|**CP-FE-03**|La etapa actual no es Envasado|Etapa Maduración `EN_CURSO` (no Envasado)|`etapaActual.getEtapa() != ENVASADO` $\rightarrow$ **TRUE**|Lanza `ReglaNegocioException`. Sin interacciones con el fermentador.|
|**CP-FE-04**|Fermentador no encontrado|Etapa Envasado `EN_CURSO`, `fermentadorRepository.buscarPorIdParaCambiarEstadoOperativo(id)` $\rightarrow$ **Optional.empty()**|Equipo dado de baja después de registrar el lote|Lanza `RecursoNoEncontradoException`. No se llama a `loteRepository.save()`.|
|**CP-FE-05**|Finalización exitosa _(Camino feliz)_|Etapa Envasado `EN_CURSO`, resto `FINALIZADA`, fermentador `EN_USO`|Todas las validaciones $\rightarrow$ **FALSE**|Envasado pasa a `FINALIZADA` con `fechaFinalizacion` seteada (sin ninguna etapa que pase a `EN_CURSO`); el Fermentador se persiste con `estadoOperativo = EN_LIMPIEZA`; el lote pasa a `estado = FINALIZADO` con su propia `fechaFinalizacion` seteada (justo ahora); no se consulta `consumoInsumoServicio` ni `reservaInsumoRepository`.|
