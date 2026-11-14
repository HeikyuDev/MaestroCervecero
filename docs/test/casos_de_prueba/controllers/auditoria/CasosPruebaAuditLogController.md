## Casos de prueba de `AuditLogController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `AuditLogControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig` y `@PreAuthorize`), el `ControllerAdvices` y la plantilla Thymeleaf real; el `IAuditLogService` está simulado (mock) y no hay base de datos. Verifican la URL, el rol requerido, cómo se traducen los parámetros a criterios del service y lo que se muestra en pantalla. La lógica del filtro se prueba aparte, en `services/auditoria/CasosPruebaAuditLog.md`.

**Rol requerido:** `ADMINISTRADOR` (`@PreAuthorize("hasRole('ADMINISTRADOR')")`).

**Alcance:** la bitácora es de solo lectura. El controller tiene dos endpoints de consulta, la lista y el detalle de una entrada; no hay alta, modificación ni baja. Las guardas de las plantillas están en `CasosPruebaPlantillasAuditoria.md`.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- Las fechas del formulario se informan sin hora: "desde" equivale a las 00:00 de ese día y "hasta" al final de ese día (23:59:59.999).

---

### 1. `GET /auditoria` — `listarAuditoria(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-AC-01** | Listado para el administrador | Rol `ADMINISTRADOR`. El service devuelve 1 entrada (`admin`, `CREAR`, `ORDEN_COMPRA`, id "12", `2026-09-15T10:30:05`, IP `127.0.0.1`) | El rol está autorizado | Responde 200 con la vista `auditoria/auditoria-lista`. Muestra "Bitácora de Auditoría", el título de la pestaña "Bitácora de Auditoría - Maestro Cervecero" (el layout arma el título con la sección activa del menú), la fecha como `15/09/2026 10:30`, el usuario, el módulo como "Orden de compra", la acción como "Alta", el ID afectado y la IP. No muestra botón de alta ni acciones de baja. |
| **CP-AC-02** | Paginación y orden por defecto | Rol `ADMINISTRADOR`, `GET /auditoria` sin parámetros | Sin criterios. La bitácora es transaccional | Llama al service con los cinco criterios en `null` y un `Pageable` de 20 elementos, página 0, ordenado de forma descendente por `fechaHora` y luego por `id` (la entrada más reciente primero). |
| **CP-AC-03** | Criterios hacia el service | Rol `ADMINISTRADOR`, `fechaDesde: 2026-09-01`, `fechaHasta: 2026-09-30`, `username: "  adm  "`, `accion: CREAR`, `concepto: MALTA`, `pagina: 1` | Las fechas se completan con hora; el username se recorta | Llama al service con `desde: 2026-09-01T00:00`, `hasta: 2026-09-30T23:59:59.999`, `accion: CREAR`, `concepto: MALTA`, `username: "adm"` y página 1. |
| **CP-AC-04** | Valores vacíos equivalen a no filtrar | Rol `ADMINISTRADOR`, fechas vacías, `username: "   "`, `accion` y `concepto` vacíos (opciones "Todas" / "Todos") | Un valor vacío o en blanco no restringe | Llama al service con los cinco criterios en `null`. |
| **CP-AC-05** | Listado sin resultados | Rol `ADMINISTRADOR`, el service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron registros con los criterios indicados" en lugar de una tabla vacía. |
| **CP-AC-06** | Rango de fechas invertido | Rol `ADMINISTRADOR`, `fechaDesde: 2026-09-30`, `fechaHasta: 2026-09-01`, `username: "admin"`. El service lanza `ReglaNegocioException("La fecha hasta no puede ser anterior a la fecha desde")` | El service rechaza el rango | Responde 200 (no redirige) con el aviso del service y la lista vacía. Los filtros elegidos (las dos fechas y el usuario) se conservan en el formulario. |
| **CP-AC-07** | La paginación conserva los filtros | Rol `ADMINISTRADOR`, todos los criterios informados, `pagina: 1`, el service devuelve la página 1 de 3 | Los enlaces de página llevan los criterios | Los enlaces "Anterior" y "Siguiente" apuntan a `pagina=0` y `pagina=2` e incluyen `fechaDesde`, `fechaHasta`, `username`, `accion` y `concepto`. |
| **CP-AC-08** | Color de la acción y datos sin valor | Rol `ADMINISTRADOR`, entradas con `CREAR`, `MODIFICAR`, `ELIMINAR`, `FINALIZAR` y `LOGOUT`; una con ID e IP nulos | El color depende del tipo de acción | `CREAR` en verde, `MODIFICAR` en azul, `ELIMINAR` en rojo, `FINALIZAR` en ámbar y `LOGOUT` en gris. Cada acción se muestra con su etiqueta ("Alta", "Modificación", "Baja", "Finalización", "Cierre de sesión") y no con el nombre de la constante. El ID o la IP nulos se muestran como "-". |
| **CP-AC-09** | Opciones de los selects | Rol `ADMINISTRADOR`, `accion: LOGIN_EXITOSO`, `concepto: ORDEN_COMPRA` | Los selects salen de los enums `AccionAuditoria` y `ConceptoAuditoria` | Ofrecen todos los valores (ej. `ANULAR`, `SESION`, `USUARIO`) con su etiqueta ("Anulación", "Planificación de producción") y dejan marcados como seleccionados los valores elegidos. |
| **CP-AC-10** | Rol no autorizado | Rol `ENCARGADO_DE_DISTRIBUCION` | `@PreAuthorize("hasRole('ADMINISTRADOR')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-AC-11** | Visitante sin sesión | Sin autenticación | La URL exige estar autenticado | Redirige al login. No invoca al service. |
| **CP-AC-12** | Parámetros inválidos | `fechaDesde: "no-es-una-fecha"`; y por separado `accion: "INVENTADA"` | El parámetro no se puede convertir | Redirige con la alerta de tipo `warning` "El valor indicado no es válido" en ambos casos. No invoca al service. |
| **CP-AC-13** | Layout con recursos locales | Rol `ADMINISTRADOR`, `GET /auditoria` (el layout es el mismo de todas las pantallas autenticadas) | Los estilos y los íconos se sirven desde la aplicación | La página referencia `/css/tailwind.css` y `/vendor/bootstrap-icons/bootstrap-icons.min.css`, y no contiene `cdn.tailwindcss.com` ni `cdn.jsdelivr.net`. |
| **CP-AC-14** | Ojo Ver en la lista | Rol `ADMINISTRADOR`, el service devuelve 2 entradas: la 41 (`MODIFICAR`) con `tieneDetalle: true` y la 42 (`LOGOUT`) con `tieneDetalle: false` | El ojo depende de que la entrada tenga detalle de estados | La tabla tiene la columna "Acciones". La fila 41 tiene el ojo (`bi-eye`) hacia `/auditoria/41` con `aria-label` "Ver detalle de la entrada 41"; la fila 42 no tiene enlace al detalle. |

### 2. `GET /auditoria/{id}` — `detalleAuditoria(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-AC-15** | Detalle con comparación | Rol `ADMINISTRADOR`, `GET /auditoria/7`. El service devuelve una entrada de modificación con 4 campos: uno sin cambios, uno modificado, uno agregado y uno eliminado | El rol está autorizado | Responde 200 con la vista `auditoria/auditoria-detalle` y el título de pestaña "Bitácora de Auditoría - Maestro Cervecero". Muestra "Detalle de auditoría", "Volver a la bitácora", el usuario, la fecha `15/09/2026 10:30`, la IP, el módulo, y la tabla con las columnas "Campo", "Estado anterior" y "Estado posterior". Cada fila lleva `data-estado-campo` con su estado y el resalte de su color: ámbar el modificado, verde el agregado, rojo el eliminado y sin resalte el que no cambió. No muestra el aviso de que no hay detalle. |
| **CP-AC-16** | Detalle de un alta | Rol `ADMINISTRADOR`, entrada `CREAR` cuyo único campo (Nombre) tiene valor anterior `null` y valor posterior "Cascade" | Un alta no tiene estado anterior | La celda del estado anterior muestra "-" y la del posterior "Cascade". |
| **CP-AC-17** | Entrada sin estados | Rol `ADMINISTRADOR`, entrada `LOGIN_EXITOSO` con `tieneDetalle: false` y sin campos | No hay estados guardados | Responde 200 con el aviso gris "No hay detalle de estados para este registro" y sin la tabla de comparación. |
| **CP-AC-18** | Contenido escapado | Rol `ADMINISTRADOR`, usuario `<b>admin</b>` y valores `<script>alert(1)</script>` y `<img src=x onerror=alert(2)>` | Todo se renderiza con `th:text` | El HTML llega escapado (`&lt;script&gt;...`) y la página no contiene el marcado original. |
| **CP-AC-19** | Contenido del detalle | Rol `ADMINISTRADOR`, entrada con un campo modificado | El detalle muestra solo la cabecera y la tabla comparativa | La página tiene la sección de estados con la columna "Estado anterior" y no contiene `<details>` ni JSON. |
| **CP-AC-20** | ID inexistente | Rol `ADMINISTRADOR`, `GET /auditoria/99` con `Referer: /auditoria`. El service lanza `RecursoNoEncontradoException("No se encontró el registro de auditoría con ID: 99")` | El registro no existe | Redirige con la alerta `danger` y el mensaje del service. |
| **CP-AC-21** | ID no numérico | Rol `ADMINISTRADOR`, `GET /auditoria/abc` | El parámetro no se puede convertir | Redirige con alerta. No invoca al service. |
| **CP-AC-22** | Rol no autorizado | Rol `ENCARGADO_DE_DISTRIBUCION`, `GET /auditoria/7` | `@PreAuthorize("hasRole('ADMINISTRADOR')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-AC-23** | Visitante sin sesión | Sin autenticación, `GET /auditoria/7` | La URL exige estar autenticado | Redirige al login. No invoca al service. |
