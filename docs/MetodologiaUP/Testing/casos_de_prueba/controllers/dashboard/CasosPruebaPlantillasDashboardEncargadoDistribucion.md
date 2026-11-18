## Casos de prueba de las plantillas del panel del encargado de distribución

**Tipo de prueba:** guardas de plantilla (lectura de los archivos HTML). Clase `DashboardEncargadoDistribucionPlantillasTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-PDE-01** | Integración en la pantalla de inicio | `home.html` incluye `home/dashboard-encargado-distribucion :: panel(${dashboardEncargadoDistribucion})` dentro de un bloque `hasRole('ENCARGADO_DE_DISTRIBUCION')`, tiene un panel por cada uno de los seis roles y no tiene tarjetas de resumen ni condiciones `hasAnyRole`. |
| **CP-PDE-02** | Fragmentos y filas | `home/dashboard-encargado-distribucion.html` define `panel`, `contadores`, `devolucionesPendientes`, `clientesConBarriles` y `barrilesListos`; el panel los arma en ese orden, uno debajo de otro: los 3 bloques de datos son hijos directos del panel, cada uno en su propia fila, sin grilla ni envoltorio de varias columnas y sin `col-span`; los contadores van en su grilla de tarjetas. Hay ocho elementos `data-contador` (cuatro tarjetas, con su variante sin permiso). |
| **CP-PDE-03** | Enlaces | Los contadores y los encabezados llevan a `/control-barriles` filtrado por `DESPACHADO` y por `CON_CERVEZA`, a `/clientes` y a `/clientes?nombre=<nombre>`, y las filas al panel de cada barril. Los cuatro contadores tienen su título y su número sale del DTO. "Devoluciones vencidas" y "Vencen hoy" son tarjetas sin enlace. |
| **CP-PDE-04** | Permisos de los atajos | Los enlaces de barriles se protegen con la condición de consulta de barril, los de clientes con `CLIENTE_CONSULTAR`, "Registrar devolución" con `DEVOLUCION_BARRIL_REGISTRAR` y "Registrar despacho" con `DESPACHO_BARRIL_REGISTRAR`; sin el permiso, los contadores quedan como texto plano. |
| **CP-PDE-05** | Atajos de registro y de consulta | La devolución y el despacho de un barril usan el botón de registro con el barril en la ruta; "Abrir" lleva a la consulta; todo va en la celda de la columna Acciones. Las columnas Días de atraso y Barriles existen y no hay formularios, botones ni columna de situación. |
| **CP-PDE-06** | Alineación de la columna Acciones | En las tres tablas, el encabezado `col-acciones` lleva `text-center` y `px-2 py-3`, la celda lleva `px-2 py-3` y el contenedor de los botones lleva `justify-center`, sin `text-right` ni `justify-end`. |
| **CP-PDE-07** | Estados vacíos | Cada sub-bloque sin filas muestra una sola línea de texto gris: "No hay devoluciones vencidas ni que venzan hoy", "No hay clientes con barriles en su poder" y "No hay barriles con cerveza listos para despachar". |
| **CP-PDE-08** | Sin emojis ni punto final | El panel no tiene emojis y ningún mensaje termina en punto. |
