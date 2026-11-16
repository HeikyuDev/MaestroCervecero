# Diseño del panel de control del administrador

Este documento define el panel que el administrador ve en la pantalla de inicio (`/home`): qué muestra, qué enlaces ofrece, qué permiso exige cada uno, cómo está armado y cuántos clics lleva cada objetivo del usuario. El panel es de solo lectura: no registra ni modifica nada; cada enlace lleva a la pantalla de consulta correspondiente. El comportamiento verificable está en `docs/test/casos_de_prueba/services/dashboard/` y en `docs/test/casos_de_prueba/controllers/dashboard/`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

El panel responde a una pregunta: quién usa el sistema y qué pasó en él. Muestra los usuarios activos, su distribución por rol, los usuarios modificados recientemente y la actividad de la bitácora de auditoría.

## 1. Roles y permisos

| Acción | Permiso | Administrador | Demás roles |
| --- | --- | --- | --- |
| Ver el panel del administrador | Rol `ADMINISTRADOR` | Sí | No (cada rol ve su propio panel) |
| Recorrer los usuarios y abrir uno (contador de usuarios, fichas por rol, "Ver todos", "Abrir") | `USUARIO_CONSULTAR` | Sí | No ve el panel |
| Recorrer la bitácora de auditoría y ver el detalle de un evento (contador de eventos, "Ver todos", ojo "Ver") | Rol `ADMINISTRADOR` | Sí | No ve el panel |

- El panel se muestra con `sec:authorize="hasRole('ADMINISTRADOR')"` y el `HomeController` carga sus datos solo para ese rol: los demás roles no reciben el modelo del panel.
- Cada contador, ficha, enlace "Ver todos" y botón de fila lleva `sec:authorize` con el permiso de la tabla. Es la misma condición que el `@PreAuthorize` del método de destino, que sigue siendo la fuente de verdad. Sin el permiso, el contador o la ficha queda como texto plano.
- El panel no tiene entrada propia en el menú: es el contenido de "Panel de Control" (ícono `bi-speedometer2`, clave activa `dashboard`), que todos los roles ven.
- El panel no tiene botones de alta, modificación ni baja.

## 2. Inventario de pantallas y rutas

El panel no tiene rutas propias: es un fragmento de la pantalla de inicio. Sus enlaces usan rutas de otras pantallas.

| N.º | Pantalla o destino | Ruta | Método | Vista | Permiso del destino |
| --- | --- | --- | --- | --- | --- |
| 1 | Pantalla de inicio con el panel del administrador | `/home` (también `/`) | GET | `home` con el fragmento `home/dashboard-administrador :: panel` | Usuario autenticado |
| 2 | Lista de usuarios | `/usuarios` | GET | `usuario/usuario-lista` | `USUARIO_CONSULTAR` |
| 3 | Lista de usuarios de un rol | `/usuarios?rol=<rol>` | GET | `usuario/usuario-lista` | `USUARIO_CONSULTAR` |
| 4 | Lista de usuarios filtrada por nombre de usuario | `/usuarios?username=<usuario>` | GET | `usuario/usuario-lista` | `USUARIO_CONSULTAR` |
| 5 | Bitácora de auditoría | `/auditoria` | GET | `auditoria/auditoria-lista` | Rol `ADMINISTRADOR` |
| 6 | Bitácora de auditoría de hoy | `/auditoria?fechaDesde=<hoy>&fechaHasta=<hoy>` | GET | `auditoria/auditoria-lista` | Rol `ADMINISTRADOR` |
| 7 | Detalle de una entrada de la bitácora | `/auditoria/{id}` | GET | `auditoria/auditoria-detalle` | Rol `ADMINISTRADOR` |

## 3. Reglas específicas

- **Usuarios activos.** Son las cuentas en estado Activo. El sistema no registra sesiones abiertas ni el último acceso, por lo que el panel no informa quién está conectado. Una cuenta dada de baja no se cuenta ni se lista.
- **Usuarios activos por rol.** Se muestran siempre todos los roles, en el orden en que están declarados, aunque valgan 0. La suma de las fichas es el contador de usuarios activos. Cada ficha lleva a la lista de usuarios de ese rol.
- **Eventos de hoy.** Es la cantidad de entradas de la bitácora con fecha y hora de hoy, desde las 00:00 hasta el final del día. El número lleva a la bitácora filtrada por hoy.
- **Usuarios modificados recientemente.** Entran los usuarios activos, el modificado más recientemente primero y, ante igual fecha, el de mayor ID. Cada fila muestra el nombre de usuario, el nombre completo y el rol, y "Abrir" lleva a la lista de usuarios filtrada por ese nombre de usuario (no existe una pantalla de detalle de usuario).
- **Actividad reciente.** Entran todas las entradas de la bitácora, la más reciente primero y, ante igual fecha y hora, la de mayor ID. Cada fila muestra fecha y hora, usuario, acción y módulo. El ojo "Ver" aparece solo en las entradas que guardaron un estado anterior o posterior, como en la bitácora.
- **Tamaño de cada bloque.** Hasta `LimitesTecnicos.PANEL_ADMINISTRADOR_FILAS_POR_BLOQUE` filas. El contador muestra el total real, no las filas visibles, y "Ver todos" lleva a la lista completa.
- **Alcance.** El panel no muestra información de las ubicaciones (países, provincias y localidades).

## 4. Descripción de la pantalla

La pantalla se lee de arriba abajo en cuatro filas. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila.

- **Fila 1, contadores:** dos tarjetas (`grid grid-cols-1 sm:grid-cols-2 gap-4`) con título en mayúsculas pequeñas, ícono y número grande: "Usuarios activos" y "Eventos de hoy". Cada una lleva a su lista y sin el permiso queda como texto plano.
- **Fila 2, "Usuarios activos por rol":** una tarjeta a todo el ancho con una ficha por rol (nombre del rol y cantidad), en una grilla de hasta seis columnas.
- **Fila 3:** "Usuarios" a todo el ancho.
- **Fila 4:** "Actividad reciente" a todo el ancho.
- **Usuarios.** Columnas: Usuario (nombre de usuario y, debajo, nombre completo), Rol y Acciones, con "Abrir". El sub-bloque se llama "Modificados recientemente" y lleva su contador y "Ver todos".
- **Actividad reciente.** Columnas: Fecha y hora, Usuario, Acción, Módulo y Acciones, con el ojo "Ver". El sub-bloque se llama "Últimos eventos" y lleva su contador y "Ver todos". No se muestra la dirección IP ni el identificador del registro afectado.
- **Columna Acciones.** El encabezado "Acciones" y el contenedor de los botones van centrados (`text-center` y `justify-center`), con el mismo relleno `px-2 py-3`.
- **Estado vacío:** un sub-bloque sin filas muestra una sola línea de texto gris centrada ("No hay usuarios activos", "No hay eventos registrados en la bitácora"), sin tabla, sin encabezado de columnas y sin tarjeta vacía.

## 5. Capas que sostienen el panel

Las capas siguen el orden de la metodología: DTOs, mapper, interfaz del service, validaciones y postcondiciones, implementación y casos de prueba.

**DTOs de respuesta** (paquete `service/response_dto/dashboard`, con Lombok `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`):

- `DashboardAdministradorResponseDTO`: `usuariosActivos` (`long`), `usuariosPorRol` (`List<ConteoDashboardResponseDTO>`), `eventosDeHoy` (`long`), `usuariosRecientes` (`BloqueDashboardResponseDTO<UsuarioRecienteResponseDTO>`) y `actividadReciente` (`BloqueDashboardResponseDTO<EventoAuditoriaResponseDTO>`).
- `UsuarioRecienteResponseDTO`: `id`, `username`, `nombreCompleto` y `rol`.
- `EventoAuditoriaResponseDTO`: `id`, `fechaHora`, `username`, `accion`, `concepto` y `tieneDetalle`.
- `ConteoDashboardResponseDTO` y `BloqueDashboardResponseDTO<T>`: los mismos de los demás paneles.

**Mapper** (`util/mapper/dashboard/MapperDashboardAdministrador`): de un usuario a `UsuarioRecienteResponseDTO` (nombre y apellido unidos), de una entrada de la bitácora a `EventoAuditoriaResponseDTO` y el armado de un conteo por rol.

**Interfaz del service** (`service/interfaces/dashboard/IDashboardAdministradorServicio`, solo lectura): `DashboardAdministradorResponseDTO obtenerDashboardAdministrador()`. Calcula todo con la fecha de hoy y el tope de `LimitesTecnicos.PANEL_ADMINISTRADOR_FILAS_POR_BLOQUE` filas por bloque. No recibe parámetros: el rol lo decide el Controller. Corre en una única transacción de lectura (`@Transactional(readOnly = true)`).

**Servicios que compone** (no se agregaron consultas):

- `IUsuarioServicio.filtrarUsuarios(null, null, null, null, rol, página de un registro)` por cada rol, para los conteos; y `filtrarUsuarios` sin criterios con 5 filas ordenadas por `lastModifiedDate` descendente y `id` descendente, para los usuarios recientes. El listado solo devuelve usuarios activos.
- `IAuditLogService.filtrarAuditLogs`: con el rango de hoy y una página de un registro para los eventos de hoy, y sin criterios con 5 filas ordenadas por `fechaHora` descendente e `id` descendente para la actividad reciente.

**Controller.** `HomeController` recibe `IDashboardAdministradorServicio` y, con la autoridad `ROLE_ADMINISTRADOR`, agrega al modelo `dashboardAdministrador`. Si el service rechaza o falla, la pantalla de inicio se muestra sin el panel y con la alerta del motivo, sin redirigir.

**Plantillas.**

- `home.html`: no tiene tarjetas de resumen. Incluye el panel de cada uno de los seis roles con `sec:authorize="hasRole('...')"` y su fragmento `panel`; el de este rol se incluye con `sec:authorize="hasRole('ADMINISTRADOR')"`.
- `home/dashboard-administrador.html`: fragmentos `panel(dashboard)`, `contadores(dashboard)`, `usuariosPorRol(dashboard)`, `usuariosRecientes(dashboard)` y `actividadReciente(dashboard)`, más las piezas compartidas `encabezado` y `contenidoFicha`.
- Reutiliza `fragments/boton-abrir`, `planificacion_produccion/formato :: fechaHora`.

**Pruebas y casos de prueba.** `docs/test/casos_de_prueba/services/dashboard/CasosPruebaDashboardAdministrador.md` y, en `docs/test/casos_de_prueba/controllers/dashboard/`, `CasosPruebaMapperDashboardAdministrador.md`, `CasosPruebaDashboardAdministradorController.md` y `CasosPruebaPlantillasDashboardAdministrador.md`.

## 6. Recorridos

| Objetivo | Recorrido | Clics |
| --- | --- | --- |
| Ver cuántos usuarios activos hay y de qué rol | Pantalla de inicio | 0 |
| Ver los usuarios de un rol | Pantalla de inicio, ficha del rol | 1 |
| Ver qué usuarios se modificaron hace poco | Pantalla de inicio, "Usuarios" | 0 |
| Entrar a un usuario | Pantalla de inicio, "Abrir" de la fila | 1 |
| Ver cuántos eventos hubo hoy | Pantalla de inicio | 0 |
| Ver los eventos de hoy | Pantalla de inicio, tarjeta "Eventos de hoy" | 1 |
| Ver el detalle de un evento reciente | Pantalla de inicio, ojo "Ver" de la fila | 1 |
