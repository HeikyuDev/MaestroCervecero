## Casos de prueba de `Paginacion`

**Tipo de prueba:** unitaria (clase `PaginacionTest`), sin Spring ni mocks. Verifica que el número de página recibido por parámetro se acota antes de armar la consulta.

**Regla:** el desplazamiento de la consulta es `página × tamaño` en enteros de 32 bits; un número de página enorme lo desborda. El número de página se acota al rango `[0, Paginacion.PAGINA_MAXIMA]` (100000). Una página fuera de rango devuelve una página vacía.

---

### `numeroDePagina(int pagina)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PGN-01** | Página dentro del rango | `0`, `7` y `100000` | Número válido | Devuelve el mismo número. |
| **CP-PGN-02** | Página negativa | `-1` y `Integer.MIN_VALUE` | Número menor que cero | Devuelve `0`. |
| **CP-PGN-03** | Página desmedida | `107000000` y `Integer.MAX_VALUE` | Número mayor que el máximo | Devuelve `100000`; con un tamaño de página de 100, el desplazamiento resultante es menor que `Integer.MAX_VALUE`. |
