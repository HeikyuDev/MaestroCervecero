## Casos de prueba de `StockInsumoController`

**Tipo de prueba:** capa web (`@WebMvcTest`): lista de stock de insumos, detalle de un insumo y detalle de un lote de insumo con sus pestañas; service simulado. Clase `StockInsumoControllerTest`. Las consultas JPQL que usa el service no se ejercitan aquí (ver `services/stock_insumo/CasosPruebaStockInsumo.md`).

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-CSI-01** | Lista de stock | Muestra las columnas Nombre, Tipo, Unidad, Disponible, Reservada, Vencida, Total y Acciones, la unidad del insumo, el botón Abrir hacia el detalle, los filtros con ejemplo, Limpiar y Buscar, y ningún distintivo Vencido. |
| **CP-CSI-02** | Criterios de la lista | El nombre sin espacios, el tipo y la situación se pasan al service, de a 20, y se conservan en el formulario. |
| **CP-CSI-03** | Lista sin criterios | No se restringe por ninguno; un nombre en blanco equivale a no filtrar. |
| **CP-CSI-04** | Parámetros inválidos | Una página negativa se acota a la primera; un tipo o una situación inexistentes, o una página que no es número, redirigen con una alerta de advertencia sin consultar el service. |
| **CP-CSI-05** | Lista vacía | Muestra "No se encontraron insumos con los criterios indicados". |
| **CP-CSI-06** | Paginación de la lista | El pie informa el total y la página, y los enlaces conservan nombre, tipo y situación. |
| **CP-CSI-07** | Roles sin permiso | El encargado de distribución y el administrador no abren ninguna de las tres pantallas: redirigen con la alerta "No tiene permisos para realizar esta acción" y no se invoca el service. |
| **CP-CSI-08** | Roles con permiso | El gerente de producción, el gerente de compras, el operario de producción y el encargado de depósito abren las tres pantallas. |
| **CP-CSI-09** | Anotación de permiso | Cada método público del Controller lleva `@PreAuthorize("hasAuthority('STOCK_INSUMO_CONSULTAR')")`. |
| **CP-CSI-10** | Detalle del insumo | Muestra el volver a la lista, el resumen con las cuatro cantidades y su unidad, y la tabla de lotes con Lote del proveedor, Vence, las cuatro cantidades y el botón Abrir hacia el lote, sin distintivo Vencido. |
| **CP-CSI-11** | Orden de los lotes | Se muestran en el orden que entrega el service (FEFO), también el vencido. |
| **CP-CSI-12** | Página del detalle del insumo | Se pide con tamaño 20 y una página negativa se acota a la primera. |
| **CP-CSI-13** | Insumo inexistente o ID inválido | Un insumo inexistente o dado de baja redirige con alerta de peligro; un ID que no es número redirige con alerta. |
| **CP-CSI-14** | Detalle del lote: reservas | Muestra la cabecera con las cuatro cantidades, el volver al insumo y la tabla de reservas con Lote de producción, Etapa, Reservada, Consumida, Liberada, Reducida por ajuste, Pendiente y Estado; no consulta los consumos. |
| **CP-CSI-15** | Detalle del lote: consumos | Con `pestana=consumos` muestra Lote de producción, Etapa, Cantidad, Tipo, Fecha, Estado y el botón i del detalle de la anulación; no consulta las reservas. |
| **CP-CSI-16** | Pestaña desconocida | Equivale a Reservas. |
| **CP-CSI-17** | Enlace al lote de producción | El gerente de producción y el operario de producción ven un enlace a `/lotes/{id}` en reservas y consumos; el gerente de compras y el encargado de depósito ven solo el identificador como texto. |
| **CP-CSI-18** | Paginación de las pestañas | Se pide con tamaño 20 y el pie conserva la pestaña. |
| **CP-CSI-19** | Lote de insumo inexistente | Un lote inexistente o de un insumo dado de baja redirige con alerta de peligro. |
| **CP-CSI-20** | Menú | Las pantallas muestran la opción Stock de Insumos a los cuatro roles con el permiso y el título de la pestaña del navegador. |
