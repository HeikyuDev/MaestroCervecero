## Casos de prueba de los buscadores modales

**Tipo de prueba:** guarda de consistencia (JUnit 5 plano, clase `BuscadoresModalTest`). No levanta Spring ni simula peticiones: lee del sistema de archivos las plantillas `buscador*.html` y `filtro-*.html` (en cualquier subcarpeta de `templates`) que contienen una `<table`, el archivo `static/js/modales.js` y `frontend/tailwind/input.css`.

**Objetivo:** todos los buscadores modales de la aplicación se ven idénticos y no se rompen con textos largos: el texto se parte en cualquier punto y el botón "Elegir" siempre queda a la vista. Los mensajes de falla nombran la plantilla infractora.

---

### 1. Consistencia de plantillas y estilos de los buscadores modales

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BMO-01** | Toda tabla de un buscador modal usa la clase `tabla-buscador` | Todas las plantillas `buscador*.html` y `filtro-*.html` que contienen `<table` | Cada etiqueta `<table` declara `class="tabla-buscador"` | El test pasa. Si alguna tabla usa otra clase, falla indicando la plantilla. |
| **CP-BMO-02** | Todo botón `data-elegir` tiene `shrink-0` | Las mismas plantillas | Cada botón con el atributo `data-elegir` incluye la clase `shrink-0`, y la plantilla tiene al menos uno | El test pasa. Si algún botón no la tiene o falta el botón, falla indicando la plantilla. |
| **CP-BMO-03** | El cascarón del modal oculta el desborde horizontal | `static/js/modales.js` | La constante `CLASES_MODAL` contiene `overflow-x-hidden` | El test pasa. Si la clase no está, falla. |
| **CP-BMO-04** | La hoja de estilos define las clases del buscador | `frontend/tailwind/input.css` | El archivo define `.tabla-buscador` y `.tabla-buscador [data-elegir]` | El test pasa. Si falta alguna de las dos definiciones, falla. |
