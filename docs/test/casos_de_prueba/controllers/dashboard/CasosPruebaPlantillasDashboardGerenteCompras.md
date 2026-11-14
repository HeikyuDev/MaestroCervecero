## Casos de prueba de las plantillas del panel del gerente de compras

**Tipo de prueba:** guardas de plantilla (lectura de los archivos HTML). Clase `DashboardGerenteComprasPlantillasTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-PDC-01** | Integración en la pantalla de inicio | `home.html` incluye `home/dashboard-gerente-compras :: panel(${dashboardGerenteCompras})` dentro de un bloque `hasRole('GERENTE_DE_COMPRAS')`, tiene un panel por cada uno de los seis roles y no tiene tarjetas de resumen ni condiciones `hasAnyRole`. |
| **CP-PDC-02** | Fragmentos y filas | `home/dashboard-gerente-compras.html` define `panel`, `contadores`, `planificacionesSinOrden`, `ordenesPendientes` y `ordenesFinalizadas`; el panel los arma en ese orden, con la grilla de tres columnas de la fila 2 y la de cuatro tarjetas de los contadores. Hay ocho elementos `data-contador` (cuatro tarjetas, con su variante sin permiso). |
| **CP-PDC-03** | Enlaces y ventana de días | Los contadores y los encabezados llevan a `/planificaciones-produccion?estado=PENDIENTE`, `/ordenes-compra?estado=PENDIENTE` y `/ordenes-compra?estado=FINALIZADA`. El contador y el título de las finalizadas dicen los días de la ventana. "Planificaciones sin orden" y "Entregas vencidas" son tarjetas sin enlace. |
| **CP-PDC-04** | Permisos de los atajos | Cada enlace se protege con el permiso de su destino (`PLANIFICACION_PRODUCCION_CONSULTAR`, `ORDEN_COMPRA_CONSULTAR`, `ORDEN_COMPRA_REGISTRAR`) y sin él los contadores quedan como texto plano. |
| **CP-PDC-05** | Atajos de registro y de consulta | La orden de una planificación se registra con el botón de registro con la planificación como parámetro aparte (`idPlanificacion`); el ojo "Ver" lleva a la consulta; todo va en la celda de la columna Acciones. Las columnas Recibido y Cierre existen y no hay formularios, botones ni columna de situación. |
| **CP-PDC-06** | Alineación de la columna Acciones | En las tres tablas, el encabezado `col-acciones` lleva `text-center` y `px-2 py-3`, la celda lleva `px-2 py-3` y el contenedor de los botones lleva `justify-center`, sin `text-right` ni `justify-end`. |
| **CP-PDC-07** | Estados vacíos | Cada sub-bloque sin filas muestra una sola línea de texto gris: "No hay planificaciones pendientes sin orden de compra", "No hay órdenes de compra pendientes" y "No hay órdenes de compra finalizadas en el período". |
| **CP-PDC-08** | Sin emojis, sin stock y sin punto final | El panel no tiene emojis, no enlaza al stock de insumos ni usa su permiso y ningún mensaje termina en punto. |
| **CP-PDC-09** | Encabezados de los bloques | Cada bloque es una tarjeta con título; los sub-bloques llevan el total real y el enlace "Ver todas" protegido por el permiso de su lista. |
