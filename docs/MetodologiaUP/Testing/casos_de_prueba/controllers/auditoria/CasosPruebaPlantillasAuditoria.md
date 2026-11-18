## Casos de prueba de las plantillas de la bitácora de auditoría

**Tipo de prueba:** guardas de plantilla (lectura de los archivos HTML). Clase `AuditoriaPlantillasTest`. Además de estas guardas, las plantillas quedan cubiertas por las guardas generales que recorren todas las plantillas (`BotonesDeFiltrosALaDerechaTest`, `ColumnaAccionesStickyTest`, `BotonesDeFilaEnAccionesTest`, `PestanasConIconoTest`, `BotonAbrirTest` y otras). El comportamiento renderizado está en `CasosPruebaAuditLogController.md`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-PAU-01** | Ojo Ver en la lista | La tabla tiene la columna "Acciones" (`col-acciones`) y su celda contiene el ojo `bi-eye`, condicionado a `tieneDetalle`, que apunta a `/auditoria/{id}`, con `aria-label` "Ver detalle de la entrada <id>" y sin tooltip. La tabla está dentro de un contenedor `overflow-x-auto` y la fila vacía ocupa las 7 columnas. |
| **CP-PAU-02** | Título de la pestaña | Las dos pantallas usan la clave `auditoria` del layout. |
| **CP-PAU-03** | Contenido escapado | Ninguna plantilla usa `th:utext` ni el escape deshabilitado `[(...)]`. |
| **CP-PAU-04** | Comparación | El detalle tiene las columnas Campo, Estado anterior y Estado posterior, en ese orden, y el resalte ámbar (modificado), verde (agregado) y rojo (eliminado) con el atributo `data-estado-campo`, dentro de un contenedor `overflow-x-auto`. |
| **CP-PAU-05** | Valores vacíos y aviso | Los valores sin dato muestran "-"; sin estados se muestra en gris "No hay detalle de estados para este registro"; no hay ninguna sección `<details>`. |
| **CP-PAU-06** | Orden de la cabecera | La cabecera del detalle muestra Usuario, Fecha y hora e IP en una fila, y después Módulo, Acción e ID afectado en otra; los dos temas no comparten fila. |
