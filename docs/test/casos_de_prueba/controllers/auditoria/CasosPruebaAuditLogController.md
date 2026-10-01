## Casos de prueba de `AuditLogController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `AuditLogControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig` y `@PreAuthorize`), el `ControllerAdvices` y la plantilla Thymeleaf real; el `IAuditLogService` está simulado (mock) y no hay base de datos. Verifican la URL, el rol requerido, cómo se traducen los parámetros a criterios del service y lo que se muestra en pantalla. La lógica del filtro se prueba aparte, en `services/auditoria/CasosPruebaAuditLog.md`.

**Rol requerido:** `ADMINISTRADOR` (`@PreAuthorize("hasRole('ADMINISTRADOR')")`).

**Alcance:** la bitácora es de solo lectura. El controller tiene un único endpoint de consulta; no hay alta, modificación ni baja.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- Las fechas del formulario se informan sin hora: "desde" equivale a las 00:00 de ese día y "hasta" al final de ese día (23:59:59.999).

---

### 1. `GET /auditoria` — `listarAuditoria(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-AC-01** | Listado para el administrador | Rol `ADMINISTRADOR`. El service devuelve 1 entrada (`admin`, `CREAR`, `ORDEN_COMPRA`, id "12", `2026-09-15T10:30:05`, IP `127.0.0.1`) | El rol está autorizado | Responde 200 con la vista `auditoria/auditoria-lista`. Muestra "Bitácora de Auditoría", la fecha como `15/09/2026 10:30:05`, el usuario, el módulo como "ORDEN COMPRA", la acción, el ID afectado y la IP. No muestra botón de alta ni acciones de baja. |
| **CP-AC-02** | Paginación y orden por defecto | Rol `ADMINISTRADOR`, `GET /auditoria` sin parámetros | Sin criterios. La bitácora es transaccional | Llama al service con los cinco criterios en `null` y un `Pageable` de 20 elementos, página 0, ordenado de forma descendente por `fechaHora` y luego por `id` (la entrada más reciente primero). |
| **CP-AC-03** | Criterios hacia el service | Rol `ADMINISTRADOR`, `fechaDesde: 2026-09-01`, `fechaHasta: 2026-09-30`, `username: "  adm  "`, `accion: CREAR`, `concepto: MALTA`, `pagina: 1` | Las fechas se completan con hora; el username se recorta | Llama al service con `desde: 2026-09-01T00:00`, `hasta: 2026-09-30T23:59:59.999`, `accion: CREAR`, `concepto: MALTA`, `username: "adm"` y página 1. |
| **CP-AC-04** | Valores vacíos equivalen a no filtrar | Rol `ADMINISTRADOR`, fechas vacías, `username: "   "`, `accion` y `concepto` vacíos (opciones "Todas" / "Todos") | Un valor vacío o en blanco no restringe | Llama al service con los cinco criterios en `null`. |
| **CP-AC-05** | Listado sin resultados | Rol `ADMINISTRADOR`, el service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron registros con los criterios indicados." en lugar de una tabla vacía. |
| **CP-AC-06** | Rango de fechas invertido | Rol `ADMINISTRADOR`, `fechaDesde: 2026-09-30`, `fechaHasta: 2026-09-01`, `username: "admin"`. El service lanza `ReglaNegocioException("La fecha desde no puede ser posterior a la fecha hasta.")` | El service rechaza el rango | Responde 200 (no redirige) con el aviso del service y la lista vacía. Los filtros elegidos (las dos fechas y el usuario) se conservan en el formulario. |
| **CP-AC-07** | La paginación conserva los filtros | Rol `ADMINISTRADOR`, todos los criterios informados, `pagina: 1`, el service devuelve la página 1 de 3 | Los enlaces de página llevan los criterios | Los enlaces "Anterior" y "Siguiente" apuntan a `pagina=0` y `pagina=2` e incluyen `fechaDesde`, `fechaHasta`, `username`, `accion` y `concepto`. |
| **CP-AC-08** | Color de la acción y datos sin valor | Rol `ADMINISTRADOR`, entradas con `CREAR`, `MODIFICAR`, `ELIMINAR`, `FINALIZAR` y `LOGOUT`; una con ID e IP nulos | El color depende del tipo de acción | `CREAR` en verde, `MODIFICAR` en azul, `ELIMINAR` en rojo, `FINALIZAR` en ámbar y `LOGOUT` en gris. El ID o la IP nulos se muestran como "—". |
| **CP-AC-09** | Opciones de los selects | Rol `ADMINISTRADOR`, `accion: LOGIN_EXITOSO`, `concepto: ORDEN_COMPRA` | Los selects salen de los enums `AccionAuditoria` y `ConceptoAuditoria` | Ofrecen todos los valores (ej. `ANULAR`, `SESION`, `USUARIO`) y dejan marcados como seleccionados los valores elegidos. |
| **CP-AC-10** | Rol no autorizado | Rol `GERENTE_COMERCIAL` | `@PreAuthorize("hasRole('ADMINISTRADOR')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-AC-11** | Visitante sin sesión | Sin autenticación | La URL exige estar autenticado | Redirige al login. No invoca al service. |
| **CP-AC-12** | Parámetros inválidos | `fechaDesde: "no-es-una-fecha"`; y por separado `accion: "INVENTADA"` | El parámetro no se puede convertir | Redirige con alerta de tipo `danger` en ambos casos. No invoca al service. |
