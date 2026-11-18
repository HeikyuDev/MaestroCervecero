## Casos de prueba de `HomeController` (panel del administrador)

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `HomeControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`), el `ControllerAdvices` y las plantillas Thymeleaf reales `home.html` y `home/dashboard-administrador.html`; los services de los paneles están simulados (`@MockitoBean`).

**Acceso:** `/` y `/home` requieren sesión iniciada. Los usuarios de prueba llevan el `ROLE_` de su rol y los permisos que le da el login. Los casos de los otros paneles están en los demás archivos `CasosPruebaDashboard*Controller.md`.

---

### 1. `GET /home` y `GET /` — `home(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-HCT-23** | Panel del administrador | Rol `ADMINISTRADOR`, `GET /home` | El administrador recibe su panel y no los de los otros roles | Responde 200 con la vista `home`; el modelo tiene `dashboardAdministrador` y no los demás; el service se llama una vez y los otros tres no se consultan. La pantalla muestra el panel (`data-dashboard-administrador`, los contadores y los bloques de usuarios por rol, usuarios y actividad reciente). |
| **CP-HCT-24** | Ruta raíz | Rol `ADMINISTRADOR`, `GET /` | La ruta raíz equivale a `/home` | Responde 200 con la vista `home` y el modelo tiene `dashboardAdministrador`. |
| **CP-HCT-25** | Contadores y fichas por rol | Rol `ADMINISTRADOR`. 8 usuarios activos, 15 eventos de hoy y un conteo por cada rol | Los contadores muestran el valor del panel | Muestran 8 y 15. Hay una ficha por rol con su etiqueta; los enlaces llevan a `/usuarios`, a `/usuarios?rol=<rol>` y a `/auditoria?fechaDesde=<hoy>&fechaHasta=<hoy>`. |
| **CP-HCT-26** | Filas y atajos | Rol `ADMINISTRADOR`. Un usuario "jperez" (Juan Pérez, operario de producción) y dos eventos, uno con detalle y otro sin él | Cada fila ofrece su atajo | "Abrir" lleva a `/usuarios?username=jperez` con su `aria-label`; el evento con detalle muestra el ojo "Ver detalle de la entrada 41" hacia `/auditoria/41` y el otro no tiene enlace. No muestra textos de estado vacío. |
| **CP-HCT-27** | Sin eventos | Rol `ADMINISTRADOR`. Todos los totales en 0 | Estados vacíos | Los contadores muestran 0; se muestran "No hay usuarios activos" y "No hay eventos registrados en la bitácora", sin tablas ni botones "Abrir". |
| **CP-HCT-28** | Rechazo o fallo del service | Rol `ADMINISTRADOR`. El service rechaza con "No se encontró el recurso" y, en una segunda llamada, lanza un error inesperado | Ninguno redirige y no se expone el detalle de un error inesperado | Responde 200 con la vista `home`, sin `dashboardAdministrador` y con la alerta `danger`: con el motivo en el rechazo y genérica ("No se pudo cargar el panel de control") en el fallo inesperado, sin el texto del error. |
