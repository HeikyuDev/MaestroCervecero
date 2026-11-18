# Diseño de las pantallas del módulo de Motivos de Ajuste

Este documento define las pantallas del Controller de los motivos de ajuste: qué pantallas existen, sus rutas, qué rol puede hacer qué, cómo está armada cada una y cuántos clics lleva cada objetivo del usuario. El comportamiento verificable está en `../../Testing/casos_de_prueba/controllers/ingreso_insumo` y `../../Testing/casos_de_prueba/services/ingreso_insumo/CasosPruebaMotivoAjuste.md`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

Un motivo de ajuste es la razón que se elige al registrar un ajuste de insumo y define su efecto sobre el stock: un motivo de tipo Ingreso suma cantidad al lote de insumo y uno de tipo Egreso la resta.

## 1. Roles y permisos

| Acción | Permiso | Gerente de producción | Demás roles |
| --- | --- | --- | --- |
| Ver la lista de motivos de ajuste | `MOTIVO_AJUSTE_CONSULTAR` | Sí | No |
| Dar de alta un motivo | `MOTIVO_AJUSTE_CREAR` | Sí | No |
| Modificar un motivo | `MOTIVO_AJUSTE_MODIFICAR` | Sí | No |
| Dar de baja un motivo | `MOTIVO_AJUSTE_ELIMINAR` | Sí | No |

- Cada método del Controller lleva `@PreAuthorize` con su permiso. Un acceso no autorizado no devuelve un 403: el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción".
- La pantalla es la pestaña "Motivos de ajuste" de Configuración de Producción, junto a "Parámetros de producción" y "Costos directos adicionales", guardada por `MOTIVO_AJUSTE_CONSULTAR`. No tiene una entrada propia en el menú: se llega por la pestaña.

## 2. Inventario de pantallas y rutas

| N.º | Pantalla | Ruta | Método | Vista |
| --- | --- | --- | --- | --- |
| 1 | Lista de motivos de ajuste | `/configuracion-produccion/motivos-ajuste` (`?nombre=&tipo=&pagina=`) | GET | `configuracion/motivos-ajuste-lista` |
| 2 | Formulario de alta | `/configuracion-produccion/motivos-ajuste/nuevo` | GET | `configuracion/motivo-ajuste-form` |
| 3 | Crear un motivo | `/configuracion-produccion/motivos-ajuste` | POST | redirige a la lista (éxito) o vuelve al formulario (error) |
| 4 | Formulario de modificación | `/configuracion-produccion/motivos-ajuste/{id}/editar` | GET | `configuracion/motivo-ajuste-form` |
| 5 | Modificar un motivo | `/configuracion-produccion/motivos-ajuste/{id}` | POST | redirige a la lista (éxito) o vuelve al formulario (error) |
| 6 | Dar de baja un motivo | `/configuracion-produccion/motivos-ajuste/{id}/baja` | POST | redirige a la lista |

No hay pantalla de detalle: la tabla muestra todos los datos del motivo.

## 3. Descripción de cada pantalla

**Lista de motivos de ajuste.** Título "Configuración de Producción", la pestaña Motivos de ajuste activa y el botón "+ Nuevo motivo de ajuste" a la derecha del encabezado. La tarjeta de filtros tiene Nombre (placeholder "Ej: Merma por derrame", coincidencia parcial sin distinguir mayúsculas) y Tipo de ajuste (Todos, Ingreso, Egreso); "Limpiar" y "Buscar" van a la derecha. La tabla lista los motivos activos de a 20 por página, en orden alfabético por nombre, con las columnas Nombre, Tipo de ajuste (Ingreso o Egreso) y Acciones (lápiz y papelera). Sin resultados: "No se encontraron motivos de ajuste con los criterios indicados".

**Formulario.** Una sola página para el alta ("Nuevo motivo de ajuste", botón "Crear motivo de ajuste") y la modificación ("Editar motivo de ajuste", botón "Guardar cambios"), con "Volver a motivos de ajuste", el aviso de campos obligatorios y un campo por fila:

1. Nombre, obligatorio (placeholder "Ej: Merma por derrame, Recuento de inventario", único entre los motivos activos sin distinguir mayúsculas, hasta 100 caracteres).
2. Tipo de ajuste, obligatorio, con las opciones Ingreso y Egreso. La ayuda dice "Ingreso suma cantidad al lote de insumo; Egreso la resta.".

Errores: un campo vacío, un nombre duplicado o una regla del service se marcan junto a su campo y en el resumen del formulario, y se conserva lo escrito.

**Motivo con ajustes asociados.** Un motivo está usado cuando tiene al menos un ajuste de insumo asociado, registrado o anulado. En la edición de un motivo usado el selector del tipo se muestra deshabilitado, con el tipo guardado, y la ayuda dice "No se puede cambiar porque el motivo ya tiene ajustes."; el nombre sigue siendo editable. El tipo guardado viaja en un campo oculto y el service rechaza igualmente cualquier cambio de tipo con el error "No se puede cambiar el tipo de ajuste porque el motivo ya tiene ajustes de insumo asociados" en el campo del tipo.

**Baja.** Desde el diálogo de la lista. Si el motivo tiene ajustes de insumo asociados, registrados o anulados, el servicio la rechaza y se vuelve a la lista con la alerta de error "No se puede dar de baja el motivo de ajuste porque tiene ajustes de insumo asociados". Los motivos sin ajustes se dan de baja de forma lógica y su nombre puede reutilizarse.

## 4. Recorridos

| Objetivo | Rol | Recorrido | Clics |
| --- | --- | --- | --- |
| Dar de alta un motivo de ajuste | Gerente de producción | Menú Configuración de Producción, pestaña Motivos de ajuste, "+ Nuevo motivo de ajuste", completar, Crear motivo de ajuste | 4 más el formulario |
| Modificar un motivo | Gerente de producción | Pestaña Motivos de ajuste, lápiz del motivo, cambiar los datos, Guardar cambios | 3 más el cambio |
| Dar de baja un motivo | Gerente de producción | Pestaña Motivos de ajuste, papelera del motivo, confirmar | 3 |
| Buscar un motivo | Gerente de producción | Pestaña Motivos de ajuste, completar los criterios, Buscar | 2 más los criterios |
