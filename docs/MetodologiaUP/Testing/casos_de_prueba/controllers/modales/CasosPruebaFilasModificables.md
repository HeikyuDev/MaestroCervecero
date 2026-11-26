## Casos de prueba de las filas modificables

**Tipo de prueba:** guarda de consistencia (JUnit 5 plano, clase `FilasModificablesPlantillasTest`). Lee las plantillas y el JS de `src/main/resources`.

**Objetivo:** en las tablas de ítems que se arman con una ventana emergente (maltas, lúpulos y levaduras de la receta, catálogo del proveedor e ítems de la orden de compra) cada fila se puede modificar además de quitar. "Modificar" vuelve a abrir la misma ventana con los datos de la fila; al confirmar, la fila se reemplaza en su lugar (la orden de compra recalcula el subtotal y el total) y al cancelar queda como estaba. Las filas que pinta el servidor (modificación, vuelta con errores u orden prearmada desde un plan de compra) también se pueden modificar.

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-FMP-01** | Botones de cada fila | `receta/receta-form-filas.html` (malta, lúpulo y levadura), `proveedor/proveedor-form-filas.html` y `orden_compra/orden-form-filas.html` | Columna Acciones de cada fragmento de fila | Cada celda `col-acciones` tiene "Modificar" (`data-modificar-fila`, ícono `bi-pencil-square`, texto ámbar) antes de "Quitar" (`data-quitar-fila`, ícono `bi-trash`, texto rojo). Cada fila lleva los textos de sus selectores: `data-etiqueta-insumo` en todas y además `data-etiqueta-presentacion` en el catálogo del proveedor. |
| **CP-FMP-02** | Modo modificar de las ventanas | `receta/receta-dialogos.html`, `proveedor/proveedor-dialogos.html` y `orden_compra/orden-dialogos.html` | Título y botón de cada ventana | Cada título (`data-titulo-dialogo`) indica el que lleva al modificar ("Modificar malta", "Modificar lúpulo", "Modificar levadura", "Modificar ítem del catálogo", "Modificar ítem de la orden") y cada botón de confirmar lleva `data-texto-modificar="Guardar cambios"`. |
| **CP-FMP-03** | Reemplazo de la fila en su lugar | `receta-form.js`, `proveedor-form.js` y `orden-compra-form.js` | Comportamiento de "Modificar" | Cada script reacciona a `data-modificar-fila` con `abrirModificar(fila)`, cambia el título y el botón con `prepararModo` y, al confirmar, reemplaza la fila (`replaceWith`) en lugar de agregar otra; los repetidos no se comparan con la fila modificada (la receta y el catálogo por su clave, la orden por el ítem, que además el buscador vuelve a ofrecer) y la fila escrita guarda los textos de sus selectores. La orden recalcula subtotal y total al reemplazar la fila. |
