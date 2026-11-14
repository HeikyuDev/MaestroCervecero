## Casos de prueba de `DashboardAdministradorServicioImpl`

**Tipo de prueba:** unitaria con Mockito (`@ExtendWith(MockitoExtension.class)`, clase `DashboardAdministradorServicioImplTest`). Se simulan `IUsuarioServicio` e `IAuditLogService`.

### 1. `obtenerDashboardAdministrador()`

Alimenta el panel del administrador. Es de solo lectura (`@Transactional(readOnly = true)`), no recibe parámetros y calcula todo con la fecha de hoy y un tope de 5 filas por bloque (`LimitesTecnicos.PANEL_ADMINISTRADOR_FILAS_POR_BLOQUE`). Devuelve `DashboardAdministradorResponseDTO` con:

- **Usuarios activos por rol:** una entrada por cada valor de `Rol`, en el orden declarado, con el total de `filtrarUsuarios` por rol (página de un registro); `usuariosActivos` es la suma.
- **Eventos de hoy:** el total de `filtrarAuditLogs` con el rango de hoy (de las 00:00 a `LocalTime.MAX`) y una página de un registro.
- **Usuarios recientes:** `filtrarUsuarios` sin criterios con 5 filas ordenadas por `lastModifiedDate` y `id` descendentes.
- **Actividad reciente:** `filtrarAuditLogs` sin criterios con 5 filas ordenadas por `fechaHora` e `id` descendentes.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-DAD-01** | Sistema vacío | Sin usuarios ni eventos | Todas las consultas devuelven páginas vacías | `usuariosActivos: 0`, un conteo por cada rol con `total: 0`, `eventosDeHoy: 0` y los dos bloques con `total: 0` y sin filas. |
| **CP-DAD-02** | Usuarios activos por rol | 2 administradores y 5 operarios de producción, el resto de los roles sin usuarios | Siempre se informan todos los roles, en orden | Los códigos son los de `Rol.values()` en su orden, el administrador tiene la etiqueta "Administrador" y `total: 2`, y `usuariosActivos: 7`. |
| **CP-DAD-03** | Eventos de hoy | 15 entradas de la bitácora de hoy | La consulta recibe el rango de hoy | `eventosDeHoy: 15`. |
| **CP-DAD-04** | Usuarios recientes | 8 usuarios activos, uno visible: "jperez", "Juan" "Pérez", operario de producción | Se piden 5 filas ordenadas por modificación y por ID descendentes | `total: 8`; la fila lleva el ID, el username, el nombre completo "Juan Pérez" y el rol. La página pedida tiene tamaño 5 y orden `lastModifiedDate: DESC, id: DESC`. |
| **CP-DAD-05** | Actividad reciente | 120 entradas de la bitácora, una visible con detalle | Se piden 5 filas | `total: 120`; la fila lleva el ID, la fecha y hora, el usuario, la acción, el módulo y `tieneDetalle: true`. |
