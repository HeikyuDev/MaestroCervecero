## Casos de prueba de las plantillas del panel del administrador

**Tipo de prueba:** guardas de plantilla (lectura de los archivos HTML). Clase `DashboardAdministradorPlantillasTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-PDA-01** | Integración en la pantalla de inicio | `home.html` incluye `home/dashboard-administrador :: panel(${dashboardAdministrador})` dentro de un bloque `hasRole('ADMINISTRADOR')`, tiene un panel por cada uno de los seis roles y no tiene tarjetas de resumen ni condiciones `hasAnyRole`. |
| **CP-PDA-02** | Fragmentos y filas | `home/dashboard-administrador.html` define `panel`, `contadores`, `usuariosPorRol`, `usuariosRecientes` y `actividadReciente`; el panel los arma en ese orden, con la grilla de tres columnas de la fila 3. Hay cuatro elementos `data-contador` (dos tarjetas, con su variante sin permiso). |
| **CP-PDA-03** | Enlaces | Los contadores, las fichas por rol, las filas y los encabezados llevan a `/usuarios`, `/usuarios?rol=<rol>`, `/usuarios?username=<usuario>`, `/auditoria` (también filtrada por hoy) y `/auditoria/{id}`. Los dos contadores tienen su título y su número sale del DTO. |
| **CP-PDA-04** | Permisos de los enlaces | Los enlaces de usuarios se protegen con `USUARIO_CONSULTAR` y los de la bitácora con el rol `ADMINISTRADOR`; sin el permiso, los contadores y las fichas quedan como texto plano. Los usuarios se abren con el fragmento "Abrir". |
| **CP-PDA-05** | Solo lectura | El panel no usa el botón de registro, no enlaza a formularios de alta, de modificación ni de baja y no tiene formularios ni botones. El ojo "Ver" solo se muestra con las entradas que tienen detalle. |
| **CP-PDA-06** | Sin ubicaciones | El panel no menciona las ubicaciones, ni países, provincias o localidades, ni lleva columnas de situación. |
| **CP-PDA-07** | Alineación de la columna Acciones | En las dos tablas, el encabezado `col-acciones` lleva `text-center` y `px-2 py-3`, la celda lleva `px-2 py-3` y el contenedor de los botones lleva `justify-center`, sin `text-right` ni `justify-end`. |
| **CP-PDA-08** | Estados vacíos | Cada sub-bloque sin filas muestra una sola línea de texto gris: "No hay usuarios activos" y "No hay eventos registrados en la bitácora". |
| **CP-PDA-09** | Sin emojis ni punto final | El panel no tiene emojis y ningún mensaje termina en punto. |
