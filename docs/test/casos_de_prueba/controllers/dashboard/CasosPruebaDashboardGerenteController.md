## Casos de prueba de `HomeController` (panel del gerente)

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `HomeControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`), el `ControllerAdvices` y las plantillas Thymeleaf reales `home.html` y `home/dashboard-gerente.html`; los services de los paneles están simulados (`@MockitoBean`).

**Acceso:** `/` y `/home` requieren sesión iniciada. Los usuarios de prueba llevan el `ROLE_` de su rol y los permisos que le da el login. Los casos del panel del operario están en `CasosPruebaDashboardOperarioController.md`.

---

### 1. `GET /home` y `GET /` — `home(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-HCT-10** | Panel del gerente | Rol `GERENTE_DE_PRODUCCION`, `GET /home` | El gerente recibe su panel y no el del operario | Responde 200 con la vista `home`; el modelo tiene `dashboardGerente` y no `dashboardOperario`; `obtenerDashboardGerente()` se llama una vez y el service del operario no se consulta. La pantalla muestra el panel (`data-dashboard-gerente`, los contadores y los bloques de producción, cola, equipamiento y planificaciones pendientes). |
| **CP-HCT-11** | Ruta raíz | Rol `GERENTE_DE_PRODUCCION`, `GET /` | La ruta raíz equivale a `/home` | Responde 200 con la vista `home` y el modelo tiene `dashboardGerente`. |
| **CP-HCT-12** | Contadores | Rol `GERENTE_DE_PRODUCCION`. 7 lotes en ejecución, 2 atrasados, 3 fermentadores ocupados de 5 y 9 equipos y barriles fuera de servicio | Los contadores muestran el valor del panel | Muestran 7, 2, "3 / 5" y 9. Los enlaces llevan a `/lotes?estado=EN_EJECUCION` y a `/control-equipamiento?tipo=FERMENTADOR`. |
| **CP-HCT-13** | Bloques y enlaces | Rol `GERENTE_DE_PRODUCCION`. Un lote en Fermentación atrasado 3 días con prioridad Alta, un fermentador con lote en ejecución, próximo lote y 2 pendientes, conteos de equipos y barriles, una planificación pendiente | Cada bloque muestra sus filas y sus enlaces | La etapa Fermentación lleva a `/lotes?estado=EN_EJECUCION&etapaActual=FERMENTACION`; "Abrir" lleva a `/lotes/12?etapa=fermentacion`; se muestran "Atrasado 3 días" (en la cola), la prioridad "Alta", la cola con "2 lotes pendientes" y el enlace al cronograma. Las fichas llevan a las listas filtradas de equipos y barriles; la planificación se consulta con el ojo "Ver Plan #7 - Pale Ale" y "Ver todas" lleva a las planificaciones pendientes. No muestra textos de estado vacío ni botones de registro. |
| **CP-HCT-14** | Panel en cero | Rol `GERENTE_DE_PRODUCCION`. Todos los totales en 0 | Estados vacíos | Los contadores muestran 0 y "0 / 0"; se muestran los textos "No hay lotes en ejecución", "No hay fermentadores registrados", "No hay planificaciones pendientes", sin tablas ni botones "Abrir". |
| **CP-HCT-15** | Rechazo del service | Rol `GERENTE_DE_PRODUCCION`. El service rechaza con "No se encontró la configuración de producción" | El rechazo no redirige | Responde 200 con la vista `home`, sin `dashboardGerente` en el modelo, con la alerta `danger` "No se pudo cargar el panel de control: No se encontró la configuración de producción", sin el panel ni las tarjetas de resumen. |
| **CP-HCT-16** | Fallo inesperado | Rol `GERENTE_DE_PRODUCCION`. El service lanza un error inesperado | No se expone el detalle del error | Responde 200 sin `dashboardGerente`, con la alerta `danger` "No se pudo cargar el panel de control" y sin el texto del error. |
