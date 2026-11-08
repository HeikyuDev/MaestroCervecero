## Casos de prueba de la estructura de los buscadores modales

**Tipo de prueba:** guarda de plantillas (JUnit plano, clase `BuscadoresModalEstructuraTest`), sin Spring. Lee del sistema de archivos todas las plantillas `buscador-*.html`.

**Objetivo:** todos los buscadores modales comparten la misma estructura y ninguno se rompe con muchas filas ni con un ancho angosto. Los mensajes de falla nombran la plantilla infractora.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-BME-01** | Estructura común de los buscadores | Cada buscador tiene la tabla `tabla-buscador` dentro de un `div.contenedor-buscador` (desplazamiento horizontal controlado), el encabezado y la celda de acción con `col-acciones px-3 py-3` (columna fija a la derecha, siempre visible), el botón "Elegir" y ningún `overflow-x-hidden` ni `min-w-[`. Su formulario de filtros es una grilla `p-6 grid grid-cols-1 sm:grid-cols-2 gap-4 items-end` con "Buscar" en un contenedor `sm:col-span-2 flex items-center justify-end gap-3`. Se revisan al menos 23 buscadores. |
