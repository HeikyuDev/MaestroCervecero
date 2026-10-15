## Casos de prueba de integración contra PostgreSQL: instantánea del escalado del lote

**Tipo de prueba:** integración (clase `EscaladoLoteIntegracionTest`, etiqueta `integracion`). Levanta el contexto completo de Spring con el perfil `test` contra la base `maestrocervecero_test`, con el esquema creado por Flyway y validado por Hibernate. Cómo prepararla y ejecutarla: `docs/BaseDeDatos.md`, sección "Tests de integración".

**Regla:** antes de cada test `IntegracionBaseTest` vacía las tablas de negocio (incluidas `escalado_lote` y `escalado_lote_insumo`) y elimina los usuarios distintos de `admin`.

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-EIN-01** | Iniciar un lote guarda la instantánea del escalado y sus cantidades no cambian al editar equipos e insumos | Lote `PENDIENTE` de 20 L con una malta (extracto `80`), un lúpulo (`5.5`, `PELLET`) y una levadura (`ALE`), stock suficiente y sus cuatro equipos (molino `50`, macerador de eficiencia `75`, espacio muerto `5` y capacidad útil `100`, olla de trub `2`, evaporación `3` y capacidad útil `200`, fermentador de capacidad útil `150`); `iniciarLote`, y después se editan la eficiencia del macerador y los datos de la malta, el lúpulo y la levadura | Se guarda una fila de `escalado_lote` referenciada desde `lote.escalado_lote_id` y una fila de `escalado_lote_insumo` por insumo; las cantidades requeridas calculadas después de la edición son iguales a las anteriores | `escalado_lote` tiene los valores de los cuatro equipos; `escalado_lote_insumo` tiene 3 filas con el extracto, los ácidos alfa y el formato, y el tipo de levadura; `eficiencia_maceracion` sigue en `75.0` y las cantidades de malta, lúpulo y levadura no cambian. |
