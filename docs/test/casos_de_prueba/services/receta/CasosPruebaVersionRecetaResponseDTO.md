## Casos de prueba de `VersionRecetaResponseDTO`

**Tipo de prueba:** unitaria (clase `VersionRecetaResponseDTOTest`), sin Spring ni mocks. Verifica la duración total estimada, un valor derivado de solo lectura que calcula el DTO (`getDuracionTotalEstimada()`) y que las pantallas de recetas solo muestran.

**Regla de cálculo:** suma las duraciones en minutos (maceración + hervido), las expresa en horas y minutos, y le agrega la suma de las duraciones en días (fermentación + maduración). Una duración nula cuenta como cero y las unidades en cero se omiten. Si no hay ninguna duración devuelve "0 min".

---

### `getDuracionTotalEstimada()`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VRR-01** | Combina días, horas y minutos | Maceración 60 min, hervido 90 min, fermentación 10 días, maduración 4 días | Hay duraciones en minutos y en días | Devuelve "14 días 2 h 30 min". |
| **CP-VRR-02** | Omite las unidades en cero | (a) maceración 45 min, el resto en 0; (b) maceración 60 y hervido 60, el resto en 0; (c) fermentación 7 días, el resto en 0 | Solo una unidad tiene valor | Devuelve "45 min", "2 h" y "7 días" respectivamente. |
| **CP-VRR-03** | Un día en singular | Hervido 30 min, fermentación 1 día | El total de días es 1 | Devuelve "1 día 30 min". |
| **CP-VRR-04** | Duraciones nulas | (a) maceración y fermentación nulas, hervido 90 min, maduración 3 días; (b) las cuatro duraciones nulas | Una duración nula cuenta como cero | (a) Devuelve "3 días 1 h 30 min"; (b) devuelve "0 min". |
