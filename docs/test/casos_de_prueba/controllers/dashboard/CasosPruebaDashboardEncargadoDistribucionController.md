## Casos de prueba de `HomeController` (panel del encargado de distribución)

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `HomeControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`), el `ControllerAdvices` y las plantillas Thymeleaf reales `home.html` y `home/dashboard-encargado-distribucion.html`; los services de los paneles están simulados (`@MockitoBean`).

**Acceso:** `/` y `/home` requieren sesión iniciada. Los usuarios de prueba llevan el `ROLE_` de su rol y los permisos que le da el login. Los casos de los otros paneles están en los demás archivos `CasosPruebaDashboard*Controller.md`.

---

### 1. `GET /home` y `GET /` — `home(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-HCT-35** | Panel del encargado de distribución | Rol `ENCARGADO_DE_DISTRIBUCION`, `GET /home` | El encargado recibe su panel y no los de los otros roles | Responde 200 con la vista `home`; el modelo tiene `dashboardEncargadoDistribucion` y no los demás; el service se llama una vez y los otros cinco no se consultan. La pantalla muestra el panel (`data-dashboard-encargado-distribucion`, los contadores y los bloques de devoluciones, clientes y barriles listos) y no tiene tarjetas de resumen. |
| **CP-HCT-36** | Ruta raíz | Rol `ENCARGADO_DE_DISTRIBUCION`, `GET /` | La ruta raíz equivale a `/home` | Responde 200 con la vista `home` y el modelo tiene `dashboardEncargadoDistribucion`. |
| **CP-HCT-37** | Contadores | Rol `ENCARGADO_DE_DISTRIBUCION`. 8 barriles en poder de clientes, 3 devoluciones vencidas, 1 que vence hoy y 5 barriles listos | Los contadores muestran el valor del panel | Muestran 8, 3, 1 y 5. Los enlaces llevan a `/control-barriles?estadoOperativo=DESPACHADO` y a `/control-barriles?estadoOperativo=CON_CERVEZA`. |
| **CP-HCT-38** | Filas y atajos | Rol `ENCARGADO_DE_DISTRIBUCION`. Una devolución del barril BRL-01 del cliente "Bar Don Jorge" con 3 días de atraso, ese cliente con 2 barriles y un barril listo BRL-02 con cerveza "Pale Ale", lote "PALE_ALE-1" y 50 litros | Cada fila ofrece sus atajos | Se muestra "3 días"; "Registrar devolución" lleva a `/control-barriles/21/devoluciones/nueva` y "Abrir" a `/control-barriles/21`; el cliente se abre en `/clientes?nombre=Bar%20Don%20Jorge`; "Registrar despacho" lleva a `/control-barriles/31/despachos/nuevo`. Cada botón lleva su `aria-label`. No hay textos de estado vacío ni columna de situación. |
| **CP-HCT-39** | Panel en cero | Rol `ENCARGADO_DE_DISTRIBUCION`. Todos los totales en 0 | Estados vacíos | Los contadores muestran 0; se muestran los tres textos de estado vacío, sin tablas ni botones de registro ni "Abrir". |
| **CP-HCT-40** | Rechazo o fallo del service | Rol `ENCARGADO_DE_DISTRIBUCION`. El service rechaza con "No se encontró el recurso" y, en una segunda llamada, lanza un error inesperado | Ninguno redirige y no se expone el detalle de un error inesperado | Responde 200 con la vista `home`, sin `dashboardEncargadoDistribucion` y con la alerta `danger`: con el motivo en el rechazo y genérica ("No se pudo cargar el panel de control") en el fallo inesperado, sin el texto del error. |
