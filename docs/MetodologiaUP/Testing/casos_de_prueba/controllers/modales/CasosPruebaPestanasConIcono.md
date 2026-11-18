## Casos de prueba de las pestañas con ícono

**Tipo de prueba:** guarda de consistencia (JUnit 5 plano, clase `PestanasConIconoTest`). No levanta Spring ni simula peticiones: lee del sistema de archivos las plantillas de `src/main/resources/templates`.

**Objetivo:** toda pestaña del sistema lleva un ícono que la acompaña, tanto las que usan el fragmento `fragments/pestanas :: pestana(...)` (Ingresos, Consumos, Insumos, Equipamiento, Barriles, Ubicaciones) como las pestañas de etapa del detalle del lote.

---

### 1. Uso del fragmento y pestañas de etapa

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PIC-01** | Todo uso del fragmento pasa un ícono | Todas las plantillas `.html` | Cada llamada a `fragments/pestanas :: pestana(` tiene un cuarto argumento (ícono) distinto de vacío y de `null` | El test pasa. Si alguna pestaña no lleva ícono, falla e indica la plantilla y los argumentos. |
| **CP-PIC-02** | El fragmento dibuja el ícono antes del texto | `fragments/pestanas.html` | El fragmento `pestana(url, activa, clave, icono, texto)` dibuja `${icono}` antes de `${texto}` | El test pasa. Si el ícono no se dibuja o va después del texto, falla. |
| **CP-PIC-03** | Las pestañas de etapa del lote llevan ícono | `lote/lote-detalle.html` | La navegación "Etapas del lote" marca el ícono (`data-icono-etapa`) y asigna un `bi-` a cada una de las seis etapas | El test pasa. Si falta el ícono o una etapa no tiene el suyo, falla. |
