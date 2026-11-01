## Casos de prueba de `BotonesDeFiltrosALaDerechaTest`

**Tipo de prueba:** guarda de plantillas (JUnit plano), sin Spring.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-BFD-01** | Botones Limpiar y Buscar de cada lista con filtros | El contenedor está alineado a la derecha (`justify-end`) y ocupa la fila completa de la grilla de filtros (`col-span-N` con las N columnas del tamaño de pantalla, o `col-span-full`), de modo que quedan en el borde derecho con cualquier cantidad de filtros. Excepciones: la lista de lotes y la barra de filtro en línea de consumos. |
