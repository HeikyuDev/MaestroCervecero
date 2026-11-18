## Casos de prueba de `MapperReceta`

**Tipo de prueba:** unitaria (clase `MapperRecetaTest`), sin Spring ni mocks. Verifica el dato calculado `abv` de la conversión de `VersionRecetaEntity` en `VersionRecetaResponseDTO`.

**Regla:** el `abv` de la versión es el ABV estimado de `CalculosCerveza.calcularAbv` con la OG y la FG objetivo (un decimal); queda en `null` si no se puede calcular. El listado, el detalle y el lote usan este valor.

---

### `toDTO(VersionRecetaEntity)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MR-01** | ABV de la versión | Versión con OG `1.05` y FG `1.01` | (OG - FG) x 131,25 = 5,25 | `abv: 5.3`. |
| **CP-MR-02** | ABV no calculable | (a) versión con OG `1.05` y sin FG; (b) OG `1.01` y FG `1.05` | Falta una densidad o la OG no supera a la FG | `abv: null` en ambos casos. |
