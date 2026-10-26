## Casos de prueba del tooltip flotante

**Tipo de prueba:** guarda de consistencia (JUnit 5 plano, clase `TooltipFlotanteTest`). No levanta Spring ni simula peticiones: lee del sistema de archivos `static/js/tooltips.js`, el script compartido que crea la tarjeta `#tooltip-flotante` con el contenido de `data-tooltip` o `data-tooltip-contenido` en todas las pantallas.

**Objetivo:** ningún texto del tooltip sobresale de su caja, aunque sea una palabra larga sin espacios (por ejemplo, una razón social de 100 caracteres seguidos).

---

### 1. Estilos de la tarjeta del tooltip

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-TTF-01** | La tarjeta del tooltip tiene ancho máximo y parte las palabras sin espacios en cualquier punto | `static/js/tooltips.js` | El script crea la tarjeta con `id = 'tooltip-flotante'` y sus estilos en línea (`tarjeta.style.cssText`) incluyen `max-width:20rem` y `overflow-wrap:anywhere` | El test pasa. Si falta el ancho máximo o el corte de palabras, falla. |
