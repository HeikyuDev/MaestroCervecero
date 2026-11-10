## Casos de prueba de las plantillas del stock de insumos

**Tipo de prueba:** guardas de plantilla (lectura de los archivos HTML). Clase `StockInsumoPlantillasTest`. Además de estas guardas, las plantillas quedan cubiertas por las guardas generales que recorren todas las plantillas (`BotonesDeFiltrosALaDerechaTest`, `ColumnaAccionesStickyTest`, `PestanasConIconoTest`, `BotonAbrirTest` y otras).

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-PSI-01** | Barra lateral | Stock de Insumos está en la sección Operación, protegido por `STOCK_INSUMO_CONSULTAR`, con ícono propio y su clave activa; la sección Operación la ven los cuatro roles con el permiso. |
| **CP-PSI-02** | Título de la pestaña | El layout conoce la clave `stock-insumos` y las tres pantallas la usan. |
| **CP-PSI-03** | Botón Abrir | La lista y el detalle del insumo usan el fragmento del botón Abrir sin copiar su marcado, dentro de la columna Acciones. |
| **CP-PSI-04** | Columnas y filtros | Cada tabla lleva sus columnas; la lista tiene los filtros con ejemplo, Limpiar y Buscar y es `novalidate`. |
| **CP-PSI-05** | Detalle del lote | Usa pestañas con ícono y protege el enlace al lote de producción con `LOTE_CONSULTAR`. |
| **CP-PSI-06** | Sin Vencido ni costos | Ninguna pantalla del stock lleva un distintivo Vencido ni muestra costos. |
