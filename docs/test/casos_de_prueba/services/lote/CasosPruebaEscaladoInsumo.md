### 1. `calcularRequerimientosMalta(VersionRecetaEntity versionReceta, double volumenObjetivo, MaceradorEntity macerador)`

Ver docs/Dominio/Escalado/EscaladoDeMalta.md. Esta cobertura es intencionalmente moderada: el cálculo ya se valida de punta a punta a través de `LoteServicioImplTest` (`iniciarLote`/`registrarLote`).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-EI-CM-01**|Reparte el total escalado entre 2 maltas manteniendo la proporción de la receta _(Camino feliz, ejemplo de EscaladoDeMalta.md)_|OG objetivo `1.050`, `volumenObjetivo: 20.0`, macerador con eficiencia `75%`, Pilsen `100kg` (extracto `80%`) y Caramelo `20kg` (extracto `74%`)|Fórmula de EscaladoDeMalta.md, Pasos 1 a 5|Retorna 2 requerimientos sin etapa asociada: total `4.3952kg` repartido en Pilsen `3.6627kg` y Caramelo `0.7325kg`.|

### 2. `calcularRequerimientosLupulo(VersionRecetaEntity versionReceta, double volumenObjetivo, LoteEntity lote)`

Ver docs/Dominio/Escalado/EscaladoDelLupulo.md.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-EI-CL-01**|Calcula HERVOR (Tinseth) y DRY_HOP (escalado lineal) en la misma llamada, resolviendo la etapa de cada detalle _(Camino feliz, ejemplo de EscaladoDelLupulo.md)_|OG `1.050`, IBU objetivo `35`, volumen base `20.0`, `volumenObjetivo: 100.0`; Cascade (AA 5.5%, Pellet, 60min, 30g base) y Centennial (AA 10%, Pellet, 15min, 10g base) en HERVOR/Hervido; Citra (15g base) en DRY_HOP/Maduración|Fórmula de Tinseth (Parte 1) para los de HERVOR, escalado lineal (Paso 7) para el de DRY_HOP|Retorna 3 requerimientos: total de HERVOR `257.09g`, repartido en Cascade `192.82g` y Centennial `64.27g`, ambos asociados a la etapa Hervido; Citra `= 75.0g` exactos, asociado a la etapa Maduración.|
|**CP-EI-CL-02**|El lote no tiene la etapa de uso indicada por un detalle|Detalle de lúpulo con `etapaDeUso: MADURACION`, lote sin etapas|`lote.getEtapas().stream().filter(...).findFirst()` $\rightarrow$ **vacío**|Lanza `RecursoNoEncontradoException` con mensaje "El lote no tiene una etapa de Maduración".|

### 3. `calcularRequerimientosLevadura(VersionRecetaEntity versionReceta, double volumenObjetivo)`

Ver docs/Dominio/Escalado/EscaladoDeLevadura.md.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-EI-CV-01**|Reparte el total escalado entre 2 levaduras manteniendo la proporción de la receta _(Camino feliz, ejemplo de EscaladoDeLevadura.md, Caso 2)_|OG `1.050`, `volumenObjetivo: 20.0`; Levadura A (ALE, 6g base, 18.000 millones cél/g) y Levadura B (HÍBRIDA, 4g base, 20.000 millones cél/g)|Fórmula de EscaladoDeLevadura.md, Pasos 1 a 6|Retorna 2 requerimientos sin etapa asociada: Levadura A ≈ `7.0833g`, Levadura B `= 4.25g` exactos.|

### 4. `asociarEtapa(List<RequerimientoInsumo> requerimientos, EtapaLoteEntity etapa)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-EI-AE-01**|Copia cada requerimiento con la etapa indicada, sin alterar la cantidad requerida|1 requerimiento sin etapa (`cantidadRequerida: 3.5`), etapa de Molienda|Copia con la misma cantidad y el mismo insumo, etapa reemplazada|Retorna 1 requerimiento con `etapa` igual a la etapa indicada, `cantidadRequerida = 3.5` (sin cambios) e `insumo` sin cambios.|
|**CP-EI-AE-02**|Conserva la cantidad requerida redondeada a 4 decimales, sin ruido de punto flotante|Requerimiento con `cantidadRequerida: 19.879134860050907`, etapa de Molienda|La cantidad se redondea al construir el requerimiento y se conserva al asociar la etapa|`cantidadRequerida = 19.8791` tanto en el requerimiento original como en la copia con etapa.|
|**CP-EI-AE-03**|Consumir exactamente la cantidad requerida mostrada alcanza el 100% y no supera lo requerido|Requerimiento con `cantidadRequerida: 430.8403177877895`|El valor mostrado coincide con el valor usado en las comparaciones|`cantidadRequerida = 430.8403`; el porcentaje de consumir esa cantidad es `100.0` y no supera lo requerido.|

### 5. `calcularRequerimientosTotales(LoteEntity lote)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-EI-CT-01**|NO fusiona el requerimiento cuando el mismo lúpulo aparece en dos detalles de etapas distintas (WHIRLPOOL en Hervido + DRY_HOP en Maduración): mantiene 2 requerimientos separados, cada uno atado a su propia etapa|Lote sin maltas ni levaduras en la receta, un único lúpulo con un detalle WHIRLPOOL (10g base, etapa Hervido) y un detalle DRY_HOP (5g base, etapa Maduración), `volumenObjetivo = volumenBase` (ratio 1:1)|El merge por clave `insumo-etapa` mantiene ambos requerimientos separados porque sus etapas difieren|Retorna 2 requerimientos para ese lúpulo: `10.0g` asociado a la etapa Hervido y `5.0g` asociado a la etapa Maduración.|
|**CP-EI-CT-02**|La malta se asocia a la etapa de Molienda (calculada con el macerador de la etapa de Maceración), la levadura a Fermentación y el lúpulo a la etapa de su detalle|Receta con 1 malta (Pilsen `100kg`, extracto `80%`), 1 levadura y 1 lúpulo de HERVOR (etapa Hervido); OG `1.050`, `volumenObjetivo: 20.0`, macerador con eficiencia `75%`|La malta se asocia a la etapa Molienda; el macerador se resuelve desde la etapa Maceración|Retorna 3 requerimientos: la malta con `4.3403kg` asociada a la etapa Molienda, la levadura asociada a Fermentación y el lúpulo asociado a Hervido.|

### 6. `calcularRequerimientosEtapa(EtapaLoteEntity etapaLote)`

El equipamiento de una etapa es una relación perezosa: puede llegar como proxy de Hibernate de la clase abstracta `EquipamientoEntity`, que no es instancia de `MaceradorEntity`. Por eso el macerador se resuelve desenvolviendo el proxy (`Hibernate.unproxy`) antes de verificar su tipo (CP-EI-CE-06 y CP-EI-CE-07). Los tests simulan el proxy con un mock de `EquipamientoEntity` que implementa `HibernateProxy`.

Variante acotada de `calcularRequerimientosTotales`, pensada para consultas de una única etapa puntual (la usa `ConsumoInsumoServicio.filtrarInsumosRequeridos`): evita calcular las categorías de insumo que no aplican a la etapa pedida en vez de calcular las 6 etapas completas y descartar el resto. La malta se requiere en la etapa de Molienda y se calcula con el macerador de la etapa de Maceración del lote (su eficiencia determina los kilos necesarios); solo se calcula si la etapa es Molienda. La Maceración no requiere insumos. La levadura, solo si la etapa es Fermentación; el lúpulo se calcula (agrupando, como siempre, todos los HERVOR de la receta para la fórmula de Tinseth) únicamente si algún detalle de la receta apunta a esta etapa puntual, y de ahí se descarta lo que no sea de ella.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-EI-CE-01**|En la etapa de Molienda solo calcula malta, aunque la receta también tenga lúpulo y levadura|Receta con 1 malta (Pilsen `100kg`, extracto `80%`), 1 levadura y 1 lúpulo de HERVOR (etapa Hervido); OG `1.050`, `volumenObjetivo: 20.0`, macerador con eficiencia `75%`; etapa consultada: Molienda|`tipoEtapa == MOLIENDA` $\rightarrow$ **TRUE**; no se evalúa levadura ni lúpulo (ninguno apunta a Molienda)|Retorna 1 requerimiento: la malta, `4.3403kg`, asociada a la etapa de Molienda.|
|**CP-EI-CE-02**|En la etapa de Fermentación solo calcula levadura, cuando ningún lúpulo de la receta apunta a esa etapa|Misma receta que CP-EI-CE-01 (el único lúpulo apunta a Hervido, no a Fermentación); etapa consultada: Fermentación|`tipoEtapa == FERMENTACION` $\rightarrow$ **TRUE**; `detallesLupulo.stream().anyMatch(etapaDeUso == FERMENTACION)` $\rightarrow$ **FALSE**|Retorna 1 requerimiento: la levadura, asociada a la etapa de Fermentación.|
|**CP-EI-CE-03**|En la etapa de Hervido solo calcula los lúpulos de HERVOR asociados a esa etapa, sin calcular malta ni levadura|Receta con 1 malta, 1 levadura y 2 lúpulos de HERVOR (Cascade y Centennial, mismos datos del ejemplo de EscaladoDelLupulo.md); etapa consultada: Hervido|`tipoEtapa != MOLIENDA` y `!= FERMENTACION` $\rightarrow$ no calcula malta ni levadura; `anyMatch(etapaDeUso == HERVIDO)` $\rightarrow$ **TRUE**|Retorna 2 requerimientos: Cascade `192.82g` y Centennial `64.27g`, ambos asociados a la etapa Hervido.|
|**CP-EI-CE-04**|Retorna una lista vacía cuando la etapa no requiere ningún insumo (ni Molienda ni Fermentación, y ningún lúpulo apunta a ella)|Receta con 1 malta y ningún lúpulo ni levadura; etapa consultada: Envasado|Las 3 condiciones (malta, levadura, lúpulo) $\rightarrow$ **FALSE**|Retorna una lista vacía, sin lanzar excepción.|
|**CP-EI-CE-05**|Fusiona (sumando la cantidad) dos detalles de lúpulo distintos del mismo insumo cuando ambos apuntan a la misma etapa|Un único lúpulo con un detalle WHIRLPOOL (10g base) y un detalle DRY_HOP (5g base), ambos con etapa de uso Maduración, `volumenObjetivo = volumenBase` (ratio 1:1); etapa consultada: Maduración|El merge por clave `insumo-etapa` fusiona ambos detalles porque comparten la misma etapa|Retorna 1 requerimiento para ese lúpulo: `15.0g`, asociado a la etapa Maduración.|
|**CP-EI-CE-06**|Lanza `ReglaNegocioException` si la etapa de Maceración del lote no tiene un macerador asociado como equipamiento|Etapa consultada: Molienda; la etapa de Maceración del lote tiene como equipamiento un `MolinoEntity` (dato corrupto, no debería ocurrir bajo el invariante del dominio)|`Hibernate.unproxy(etapaMaceracion.getEquipamiento()) instanceof MaceradorEntity` $\rightarrow$ **FALSE**|Lanza `ReglaNegocioException` ("La etapa de Maceración del lote 1 no tiene un macerador asociado como equipamiento").|
|**CP-EI-CE-07**|El macerador se resuelve desde un proxy de Hibernate|Etapa consultada: Molienda; la etapa de Maceración del lote tiene como equipamiento un proxy de `EquipamientoEntity` (no es instancia de `MaceradorEntity`) que delega en un macerador con eficiencia `75%`; receta con la malta Pilsen `100kg`, OG `1.050`, `volumenObjetivo: 20.0`|`Hibernate.unproxy(etapaMaceracion.getEquipamiento()) instanceof MaceradorEntity` $\rightarrow$ **TRUE**|Retorna 1 requerimiento con la malta Pilsen y la misma cantidad requerida que con el macerador real (sin lanzar excepción).|
|**CP-EI-CE-08**|La etapa de Maceración no requiere insumos, aunque la receta tenga malta|Receta con 1 malta (Pilsen `100kg`) y sin lúpulos ni levaduras; etapa consultada: Maceración|`tipoEtapa == MOLIENDA` $\rightarrow$ **FALSE**; las demás condiciones $\rightarrow$ **FALSE**|Retorna una lista vacía, sin lanzar excepción.|
