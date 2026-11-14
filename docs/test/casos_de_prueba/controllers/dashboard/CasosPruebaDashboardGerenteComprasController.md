## Casos de prueba de `HomeController` (panel del gerente de compras)

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `HomeControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`), el `ControllerAdvices` y las plantillas Thymeleaf reales `home.html` y `home/dashboard-gerente-compras.html`; los services de los paneles están simulados (`@MockitoBean`).

**Acceso:** `/` y `/home` requieren sesión iniciada. Los usuarios de prueba llevan el `ROLE_` de su rol y los permisos que le da el login. Los casos de los otros paneles están en los demás archivos `CasosPruebaDashboard*Controller.md`.

---

### 1. `GET /home` y `GET /` — `home(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-HCT-29** | Panel del gerente de compras | Rol `GERENTE_DE_COMPRAS`, `GET /home` | El gerente de compras recibe su panel y no los de los otros roles | Responde 200 con la vista `home`; el modelo tiene `dashboardGerenteCompras` y no los demás; el service se llama una vez y los otros cinco no se consultan. La pantalla muestra el panel (`data-dashboard-gerente-compras`, los contadores y los bloques de planificaciones, órdenes pendientes y órdenes finalizadas). |
| **CP-HCT-30** | Ruta raíz | Rol `GERENTE_DE_COMPRAS`, `GET /` | La ruta raíz equivale a `/home` | Responde 200 con la vista `home` y el modelo tiene `dashboardGerenteCompras`. |
| **CP-HCT-31** | Contadores | Rol `GERENTE_DE_COMPRAS`. 6 planificaciones sin orden, 9 órdenes pendientes, 3 con entrega vencida y 14 finalizadas en 30 días | Los contadores muestran el valor del panel | Muestran 6, 9, 3 y 14, con el título "Finalizadas, últimos 30 días". Los enlaces llevan a `/planificaciones-produccion?estado=PENDIENTE`, `/ordenes-compra?estado=PENDIENTE` y `/ordenes-compra?estado=FINALIZADA`. |
| **CP-HCT-32** | Filas y atajos | Rol `GERENTE_DE_COMPRAS`. Una planificación "Plan #7 - Pale Ale", una orden pendiente con 40 % recibido y dos órdenes finalizadas, una de cierre automático y otra manual | Cada fila ofrece su atajo | "Registrar orden" lleva a `/ordenes-compra/nuevo?idPlanificacion=7` y el ojo a `/planificaciones-produccion/7`; la orden pendiente muestra "40 %" y su ojo lleva a `/ordenes-compra/7`; las finalizadas muestran "Automático" y "Manual". Cada botón lleva su `aria-label`. No hay textos de estado vacío ni columna de situación. |
| **CP-HCT-33** | Panel en cero | Rol `GERENTE_DE_COMPRAS`. Todos los totales en 0 | Estados vacíos | Los contadores muestran 0; se muestran los tres textos de estado vacío, sin tablas ni botones de registro. |
| **CP-HCT-34** | Rechazo o fallo del service | Rol `GERENTE_DE_COMPRAS`. El service rechaza con "No se encontró el recurso" y, en una segunda llamada, lanza un error inesperado | Ninguno redirige y no se expone el detalle de un error inesperado | Responde 200 con la vista `home`, sin `dashboardGerenteCompras` y con la alerta `danger`: con el motivo en el rechazo y genérica ("No se pudo cargar el panel de control") en el fallo inesperado, sin el texto del error. |
