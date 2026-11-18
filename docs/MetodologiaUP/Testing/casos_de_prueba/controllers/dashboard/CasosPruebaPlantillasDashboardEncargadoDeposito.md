## Casos de prueba de las plantillas del panel del encargado de depósito

**Tipo de prueba:** guardas de plantilla (lectura de los archivos HTML). Clase `DashboardEncargadoDepositoPlantillasTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-PDD-01** | Integración en la pantalla de inicio | `home.html` incluye `home/dashboard-encargado-deposito :: panel(${dashboardEncargadoDeposito})` dentro de un bloque `hasRole('ENCARGADO_DE_DEPOSITO')`, tiene un panel por cada uno de los seis roles y no tiene tarjetas de resumen ni condiciones `hasAnyRole`. |
| **CP-PDD-02** | Fragmentos y filas | `home/dashboard-encargado-deposito.html` define `panel`, `contadores`, `ordenesPendientes`, `insumosSinStock` y `lotesVencidos`; el panel los arma en ese orden, uno debajo de otro: los 3 bloques de datos son hijos directos del panel, cada uno en su propia fila, sin grilla ni envoltorio de varias columnas y sin `col-span`; los contadores van en su grilla de tarjetas. Hay ocho elementos `data-contador` (cuatro tarjetas, con su variante sin permiso). |
| **CP-PDD-03** | Enlaces a las listas filtradas | Los contadores y los encabezados llevan a `/ordenes-compra?estado=PENDIENTE` y a `/stock-insumos` con `situacionStock` `CON_VENCIDOS` y `SIN_STOCK_DISPONIBLE`. Los cuatro contadores tienen su título y su número sale del DTO. "Entregas vencidas" y "Lotes vencidos" son tarjetas sin enlace. |
| **CP-PDD-04** | Permisos de los atajos | Cada enlace se protege con el permiso de su destino (`ORDEN_COMPRA_CONSULTAR`, `STOCK_INSUMO_CONSULTAR`, `INGRESO_INSUMO_REGISTRAR`, `AJUSTE_INSUMO_REGISTRAR`) y sin él los contadores quedan como texto plano. |
| **CP-PDD-05** | Atajos de registro y de consulta | El ingreso de una orden y el ajuste de un lote usan el botón de registro; el ojo "Ver" y "Abrir" llevan a la consulta; todo va en la celda de la columna Acciones. No hay formularios, botones ni columna de situación. |
| **CP-PDD-06** | Alineación de la columna Acciones | En las tres tablas, el encabezado `col-acciones` lleva `text-center` y `px-2 py-3`, la celda lleva `px-2 py-3` y el contenedor de los botones lleva `justify-center`, sin `text-right` ni `justify-end`. |
| **CP-PDD-07** | Estados vacíos | Cada sub-bloque sin filas muestra una sola línea de texto gris: "No hay órdenes de compra pendientes", "No hay insumos sin stock disponible" y "No hay lotes de insumo vencidos con cantidad". |
| **CP-PDD-08** | Sin emojis ni punto final | El panel no tiene emojis y ningún mensaje termina en punto. |
| **CP-PDD-09** | Encabezados de los bloques | Cada bloque es una tarjeta con título; los sub-bloques llevan el total real y el enlace "Ver todos" o "Ver todas" protegido por el permiso de su lista. |
