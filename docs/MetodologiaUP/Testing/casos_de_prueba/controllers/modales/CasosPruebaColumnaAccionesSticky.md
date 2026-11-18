## Casos de prueba de la columna de acciones fija

**Tipo de prueba:** guarda de consistencia (JUnit 5 plano, clase `ColumnaAccionesStickyTest`). No levanta Spring ni simula peticiones: lee del sistema de archivos todas las plantillas `.html` de `templates` (en cualquier subcarpeta), salvo los buscadores modales (`buscador*.html` y `filtro-*.html`, cuya tabla `.tabla-buscador` tiene su propio estilo), y el archivo `frontend/tailwind/input.css`.

**Objetivo:** en todas las tablas de listado del sistema la columna de acciones (editar, dar de baja, ver, anular, mover, quitar) queda fija contra el borde derecho de la tarjeta mientras el resto de la tabla se desplaza en horizontal, con el mismo aspecto en todas las pantallas. Una tabla nueva con columna de acciones sin la clase compartida hace fallar el test.

**Criterio de detección de la columna de acciones:**

- **Encabezado:** el último `<th>` del `<thead>` de una tabla tiene el texto `Acciones` o `Mover`, o está vacío (columna de "Quitar" de los formularios con detalle).
- **Celda:** la última `<td>` de una fila, sin `colspan`, contiene un `<button>` o un enlace `<a>` con `aria-label`. Incluye las filas definidas como fragmentos (`receta-form-filas.html`, `proveedor-form-filas.html`).

---

### 1. Plantillas

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CAS-01** | El encabezado de toda columna de acciones lleva la clase compartida | Todas las plantillas con `<table>` fuera de los buscadores modales | Cada encabezado de acciones detectado tiene `col-acciones` en su atributo `class`, y se detectan al menos 20 | El test pasa. Si algún encabezado de acciones no tiene la clase, falla e indica la plantilla y la etiqueta. |
| **CP-CAS-02** | Toda celda de acciones de una fila lleva la clase compartida | Todas las filas `<tr>` de las plantillas fuera de los buscadores modales | Cada última celda con un botón o un enlace con `aria-label` tiene `col-acciones` en su atributo `class`, y se detectan al menos 20 | El test pasa. Si alguna celda de acciones no tiene la clase, falla e indica la plantilla y la etiqueta. |
| **CP-CAS-03** | Toda tabla con columna de acciones se desplaza dentro de su contenedor | Las tablas que contienen `col-acciones` | El último `<div>` abierto antes de la `<table>` tiene la clase `overflow-x-auto` | El test pasa. Si una tabla con columna de acciones no está dentro de un contenedor desplazable, falla e indica la plantilla. |

### 2. Hoja de estilos

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CAS-04** | La hoja de estilos define la columna fija con el fondo de su fila | `frontend/tailwind/input.css` | La regla `.col-acciones` incluye `position: sticky`, `right: 0` y `background-color: inherit`, y el archivo define el fondo por defecto `:where(tbody > tr:has(> .col-acciones))` | El test pasa. Si falta alguna de esas reglas, falla. |
| **CP-CAS-05** | La hoja de estilos centra la acción única de la columna | `frontend/tailwind/input.css` | Existe la regla `td.col-acciones > .flex:has(> :only-child)` con `justify-content: center`, de modo que una celda con una sola acción (ojo, Abrir, Finalizar o Anular) la muestra centrada y con dos o más acciones el grupo queda a la derecha | El test pasa. Si falta la regla, falla. |
