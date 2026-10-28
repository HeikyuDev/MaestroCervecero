## Casos de prueba del botón estándar "Abrir"

**Tipo de prueba:** guarda de consistencia (JUnit 5 plano, clase `BotonAbrirTest`). No levanta Spring ni simula peticiones: lee del sistema de archivos las plantillas de `src/main/resources/templates`.

**Objetivo:** la entrada a la pantalla de trabajo de un registro (hoy, la lista de lotes) usa el mismo botón (fragmento `fragments/boton-abrir`), con el mismo aspecto, el texto "Abrir", la flecha y un `aria-label` corto, sin tooltip.

---

### 1. Fragmento y lista que lo usa

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BAB-01** | El fragmento define el botón estándar | `fragments/boton-abrir.html` | El fragmento `abrir(url, etiquetaAccesible)` es un enlace con `data-boton-abrir`, `th:href` con la URL, `aria-label` con la etiqueta accesible, `whitespace-nowrap`, anillo de foco y estado hover ámbar; sin atributo `data-tooltip` ni `title`; su texto es "Abrir" con el ícono `bi-arrow-right` | El test pasa. Si falta alguno de esos elementos, falla. |
| **CP-BAB-02** | La lista de lotes usa el fragmento | `lote/lote-lista.html` | La plantilla invoca `fragments/boton-abrir :: abrir(` y no copia el atributo `data-boton-abrir` | El test pasa. Si la lista no usa el fragmento o copia su marcado, falla. |
| **CP-BAB-03** | La lista de lotes no conserva el engranaje | `lote/lote-lista.html` | La plantilla no contiene `bi-gear` ni el texto "Gestionar lote" | El test pasa. Si la lista conserva el ícono o el texto anterior, falla. |
