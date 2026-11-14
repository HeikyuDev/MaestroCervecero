## Casos de prueba de `HomeController` (panel del encargado de depósito)

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `HomeControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`), el `ControllerAdvices` y las plantillas Thymeleaf reales `home.html` y `home/dashboard-encargado-deposito.html`; los services de los paneles están simulados (`@MockitoBean`).

**Acceso:** `/` y `/home` requieren sesión iniciada. Los usuarios de prueba llevan el `ROLE_` de su rol y los permisos que le da el login. Los casos de los otros paneles están en `CasosPruebaDashboardOperarioController.md` y `CasosPruebaDashboardGerenteController.md`.

---

### 1. `GET /home` y `GET /` — `home(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-HCT-17** | Panel del encargado de depósito | Rol `ENCARGADO_DE_DEPOSITO`, `GET /home` | El encargado recibe su panel y no los de los otros roles | Responde 200 con la vista `home`; el modelo tiene `dashboardEncargadoDeposito` y no `dashboardOperario` ni `dashboardGerente`; el service se llama una vez y los otros dos no se consultan. La pantalla muestra el panel (`data-dashboard-encargado-deposito`, los contadores y los bloques de órdenes, insumos sin stock y lotes vencidos). |
| **CP-HCT-18** | Ruta raíz | Rol `ENCARGADO_DE_DEPOSITO`, `GET /` | La ruta raíz equivale a `/home` | Responde 200 con la vista `home` y el modelo tiene `dashboardEncargadoDeposito`. |
| **CP-HCT-19** | Contadores | Rol `ENCARGADO_DE_DEPOSITO`. 9 órdenes pendientes, 4 con entrega vencida, 12 lotes vencidos y 6 insumos sin stock | Los contadores muestran el valor del panel | Muestran 9, 4, 12 y 6. Los enlaces llevan a `/ordenes-compra?estado=PENDIENTE` y a `/stock-insumos` con `situacionStock=CON_VENCIDOS` y `SIN_STOCK_DISPONIBLE`. |
| **CP-HCT-20** | Filas y atajos | Rol `ENCARGADO_DE_DEPOSITO`. Una orden de la que se muestra "Orden de compra #7" y su proveedor, un lote vencido "L-125" de "Malta Pilsen" con 7,5 kg y un insumo sin stock | Cada fila ofrece sus atajos | "Registrar ingreso" lleva a `/ingresos-insumo/compra/nuevo?idOrdenCompra=7` y el ojo a `/ordenes-compra/7`; "Registrar ajuste" lleva a `/ajustes-insumo/nuevo?idLoteInsumo=10` y "Abrir" a `/stock-insumos/lotes/10`; el insumo se abre en `/stock-insumos/4`. Cada botón lleva su `aria-label`. No hay textos de estado vacío ni columna de situación. |
| **CP-HCT-21** | Panel en cero | Rol `ENCARGADO_DE_DEPOSITO`. Todos los totales en 0 | Estados vacíos | Los contadores muestran 0; se muestran los tres textos de estado vacío, sin tablas ni botones "Abrir" ni de registro. |
| **CP-HCT-22** | Rechazo o fallo del service | Rol `ENCARGADO_DE_DEPOSITO`. El service rechaza con "No se encontró la configuración de producción" y, en una segunda llamada, lanza un error inesperado | Ninguno redirige y no se expone el detalle de un error inesperado | Responde 200 con la vista `home`, sin `dashboardEncargadoDeposito` en el modelo y con la alerta `danger`: con el motivo en el rechazo ("No se pudo cargar el panel de control: No se encontró la configuración de producción") y genérica ("No se pudo cargar el panel de control") en el fallo inesperado, sin el texto del error. |
