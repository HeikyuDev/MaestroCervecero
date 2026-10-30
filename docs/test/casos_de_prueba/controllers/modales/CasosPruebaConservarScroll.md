## Casos de prueba del scroll conservado

**Tipo de prueba:** guarda de consistencia (JUnit 5 plano, clase `ConservarScrollTest`). Lee del sistema de archivos el layout, el sidebar, las pestañas y el script `conservar-scroll.js`.

**Objetivo:** el sidebar no vuelve al inicio al navegar y, al cambiar de pestaña, la página conserva su posición (regla "Scroll conservado" de `docs/EstandaresUI.md`).

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CSC-01** | Script incluido en el layout | `layout/base.html` y `fragments/sidebar.html` | Ubicación del script | `conservar-scroll.js` se incluye después de `</main>` y antes de `</body>`, sin `defer`, y el sidebar conserva `sidebar-scroll` y `overflow-y-auto`. |
| **CP-CSC-02** | Claves y selectores | `static/js/conservar-scroll.js` | Qué guarda y cuándo restaura | Usa las claves `scroll-sidebar` y `scroll-pestana`, el selector del aside y los de las pestañas (`nav.border-b[aria-label] a` y `nav[data-pestanas] a`), guarda también en `pagehide`, restaura la pestaña solo si la ruta coincide con la guardada y envuelve cada acceso a `sessionStorage` en `try/catch`. |
