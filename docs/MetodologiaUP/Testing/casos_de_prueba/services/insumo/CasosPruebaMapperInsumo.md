## Casos de prueba de `MapperInsumo`

**Tipo de prueba:** unitaria (clase `MapperInsumoTest`). Verifica la conversión polimórfica de un `InsumoEntity` en el `InsumoResponseDTO` concreto de su tipo (malta, lúpulo o levadura).

**Regla de conversión:** el mapper elige el mapper de cada tipo según el tipo real del insumo. La relación perezosa hacia `InsumoEntity` (por ejemplo, el insumo de un lote de insumo o de un catálogo de proveedor) llega como proxy de Hibernate de la clase abstracta, que no es instancia de ninguna subclase concreta; por eso el insumo se desenvuelve (`Hibernate.unproxy`) antes de verificar su tipo. El test simula el proxy con un mock de `InsumoEntity` que implementa `HibernateProxy` y delega en una malta real.

---

### `toDTO(InsumoEntity)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MI-01** | Insumo nulo | `null` | La entidad de entrada es nula | Retorna `null`. |
| **CP-MI-02** | Malta | `MaltaEntity` con id `1L` y nombre "Malta Pilsen" | El tipo real es malta | Retorna un `MaltaResponseDTO` con el mismo id y nombre. |
| **CP-MI-03** | Lúpulo | `LupuloEntity` con id `2L` | El tipo real es lúpulo | Retorna un `LupuloResponseDTO` con el mismo id. |
| **CP-MI-04** | Levadura | `LevaduraEntity` con id `3L` | El tipo real es levadura | Retorna un `LevaduraResponseDTO` con el mismo id. |
| **CP-MI-05** | Proxy de Hibernate de `InsumoEntity` | Proxy (no es instancia de `MaltaEntity`) que delega en la malta de id `1L` | `Hibernate.unproxy(insumo) instanceof MaltaEntity` $\rightarrow$ **TRUE** | Retorna un `MaltaResponseDTO` con id `1L` y nombre "Malta Pilsen". |
| **CP-MI-06** | Tipo de insumo no soportado | Subclase anónima de `InsumoEntity` que no es malta, lúpulo ni levadura | Ningún tipo soportado coincide | Lanza `IllegalStateException` ("Tipo de insumo no soportado: ..."). |
