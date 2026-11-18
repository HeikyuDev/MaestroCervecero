## Casos de prueba del botón de registro en la fila

**Tipo de prueba:** guarda de consistencia (JUnit 5 plano, clase `BotonRegistrarTest`). No levanta Spring ni simula peticiones: lee del sistema de archivos las plantillas de `src/main/resources/templates`.

**Objetivo:** la acción de una fila que lleva al formulario de alta de una operación (limpieza, mantenimiento) usa el mismo botón (fragmento `fragments/boton-registrar`), con el aspecto suave ámbar de "Abrir", el ícono de la operación a la izquierda, sin flecha y sin tooltip.

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BRG-01** | El fragmento define el botón de registro | `fragments/boton-registrar.html` y `fragments/boton-abrir.html` | El fragmento `registrar(url, texto, icono, etiquetaAccesible)` es un enlace con `data-boton-registrar`, `th:href` con la URL, `aria-label` con la etiqueta accesible y las mismas clases de "Abrir"; el ícono de la operación va antes del texto, sin `bi-arrow-right`, `data-tooltip` ni `title` | El test pasa. |
| **CP-BRG-02** | El panel del operario usa el fragmento | `home/dashboard-operario.html` | Las cuatro filas de limpieza y mantenimiento invocan el fragmento con su texto, su ícono y su destino, y no copian el atributo `data-boton-registrar` | El test pasa. Si una fila copia el marcado o cambia el ícono o el destino, falla. |
