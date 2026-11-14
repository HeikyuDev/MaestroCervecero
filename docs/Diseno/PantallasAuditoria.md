# Diseño de las pantallas del módulo de Auditoría

Este documento define las pantallas del Controller de la bitácora de auditoría: qué pantallas existen, sus rutas, qué rol puede ver cada una, cómo está armada cada una y cuántos clics lleva cada objetivo del usuario. El módulo es informativo y de solo lectura: no registra ni modifica nada. Las entradas de la bitácora las generan el sistema al confirmarse cada operación de negocio y al iniciar o cerrar sesión.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

## 1. Roles y permisos

| Acción | Rol | Administrador | Demás roles |
| --- | --- | --- | --- |
| Ver la lista de la bitácora | `ADMINISTRADOR` | Sí | No |
| Ver el detalle de una entrada con sus estados anterior y posterior | `ADMINISTRADOR` | Sí | No |

- Cada método del Controller lleva `@PreAuthorize("hasRole('ADMINISTRADOR')")`.
- Un acceso no autorizado no devuelve un 403: el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción".
- La opción "Bitácora de Auditoría" (ícono `bi-shield-check`) está en el menú y la ve el administrador. El título de la pestaña del navegador de las dos pantallas es "Bitácora de Auditoría - Maestro Cervecero".

## 2. Inventario de pantallas y rutas

| N.º | Pantalla | Ruta | Método | Vista |
| --- | --- | --- | --- | --- |
| 1 | Lista de la bitácora | `/auditoria` (`?fechaDesde=&fechaHasta=&username=&accion=&concepto=&pagina=`) | GET | `auditoria/auditoria-lista` |
| 2 | Detalle de una entrada | `/auditoria/{id}` | GET | `auditoria/auditoria-detalle` |

Un ID inexistente responde como recurso no encontrado y el `ControllerAdvices` redirige con la alerta "No se encontró el registro de auditoría con ID: <id>". Un ID que no es numérico se rechaza con la alerta `warning` "El valor indicado no es válido".

## 3. Estados anterior y posterior

Cada entrada de negocio guarda, en JSON, el estado del registro afectado antes y después de la operación (ver `BaseDeDatos.md`, sección "Detalle de estados de la bitácora"):

| Operación | Estado anterior | Estado posterior |
| --- | --- | --- |
| Alta | Sin estado | Registro creado |
| Modificación, finalización, anulación y baja lógica | Registro antes del cambio | Registro después del cambio |
| Eliminación física | Registro eliminado | Sin estado |

Las entradas de inicio y cierre de sesión, y las operaciones que no modificaron el registro, no tienen estados. El JSON nunca incluye contraseñas ni otros datos secretos, ni los datos personales de contacto: el correo y el teléfono del usuario, y el teléfono, el correo y la dirección de clientes y proveedores. Esos campos no aparecen en la comparación del detalle, porque la bitácora la ven todos los administradores y no debe convertirse en una copia de datos personales. Los datos de negocio (CUIT, razón social, nombres) sí se muestran.

## 4. Descripción de cada pantalla

**Lista de la bitácora.** Título "Bitácora de Auditoría" y subtítulo "Registro de las operaciones realizadas en el sistema, de la más reciente a la más antigua.". La tarjeta de filtros tiene Fecha desde, Fecha hasta, Usuario (placeholder "Ej: admin", coincidencia parcial), Acción y Módulo; "Limpiar" y "Buscar" van a la derecha de la tarjeta. La tabla lista las entradas de a 20 por página, la más reciente primero. Columnas: Fecha y hora, Usuario, Módulo, Acción (con un color según su tipo), ID afectado, IP y Acciones. Los datos sin valor se muestran con "-". La columna Acciones lleva el ojo "Ver" (`bi-eye`, con `aria-label` "Ver detalle de la entrada <id>", sin tooltip) que abre el detalle; solo aparece en las entradas que guardaron algún estado, de modo que las de inicio y cierre de sesión tienen la celda vacía. Sin resultados: "No se encontraron registros con los criterios indicados".

**Detalle de una entrada.** Botón "Volver a la bitácora", título "Detalle de auditoría" y la acción como badge con el color de la lista. Tiene dos secciones:

- **Datos de la entrada.** Una fila con quién y cuándo (Usuario, Fecha y hora, IP) y otra con qué se hizo y sobre qué (Módulo, Acción, ID afectado).
- **Estado anterior y posterior.** Una referencia de colores y la tabla Campo, Estado anterior, Estado posterior con todos los campos del registro, en el orden en que se guardaron. Cada fila se resalta según su cambio: ámbar si el campo cambió, verde si solo existe en el estado posterior (todos los campos de un alta), rojo si solo existe en el anterior y sin resalte si quedó igual. En una fila cambiada el valor anterior va en rojo y el posterior en verde. Los valores sin dato (y el estado que no existe) se muestran con "-". Los nombres de campo se muestran legibles (por ejemplo "Potencial extracto"; las asociaciones llevan "(ID)", como "Fabricante (ID)"), los booleanos como "Sí" y "No", las fechas como `dd/MM/yyyy` o `dd/MM/yyyy HH:mm:ss` y los números con el formato de cantidades de la aplicación (separador de miles `.`, decimal `,`, sin ceros sobrantes; los identificadores van sin separador de miles). Un campo con `null` en los dos estados figura sin cambios; un campo que existe en un solo estado, aunque valga `null`, figura como agregado o eliminado; dos números de igual valor (`1` y `1.0`) no se consideran un cambio.

Si la entrada no tiene estados, o los estados guardados no se pueden leer, la sección muestra en gris "No hay detalle de estados para este registro" en lugar de la tabla; la pantalla nunca falla por un estado dañado. Todo el contenido se escapa: un valor con HTML se muestra como texto.

## 5. Recorridos

| Objetivo | Rol | Recorrido | Clics |
| --- | --- | --- | --- |
| Ver qué se hizo en el sistema | Administrador | Menú Bitácora de Auditoría | 1 |
| Ver qué cambió en una operación | Administrador | Menú Bitácora de Auditoría, ojo "Ver" de la entrada | 2 |
| Ver qué modificó un usuario | Administrador | Menú Bitácora de Auditoría, completar el usuario, Buscar, ojo "Ver" | 3 más el criterio |
