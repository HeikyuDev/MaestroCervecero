# Diseño de las pantallas del módulo de Gestión de Clientes

Este documento define las pantallas del Controller del módulo de Gestión de Clientes: qué pantallas existen, sus rutas, qué rol puede hacer qué, cómo está armada cada una y cuántos clics lleva cada objetivo del usuario. El comportamiento verificable está en `docs/test/casos_de_prueba/controllers/cliente/CasosPruebaClienteController.md` y las reglas de negocio en `docs/test/casos_de_prueba/services/cliente/CasosPruebaCliente.md`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

## 1. Roles y permisos

| Acción | Permiso | Encargado de distribución | Demás roles |
| --- | --- | --- | --- |
| Ver la lista de clientes | `CLIENTE_CONSULTAR` | Sí | No |
| Dar de alta un cliente | `CLIENTE_CREAR` | Sí | No |
| Modificar un cliente | `CLIENTE_MODIFICAR` | Sí | No |
| Dar de baja un cliente | `CLIENTE_ELIMINAR` | Sí | No |
| Elegir país, provincia y localidad en los buscadores | rol (`hasAnyRole` de `ADMINISTRADOR`, `ENCARGADO_DE_DISTRIBUCION` y `GERENTE_DE_COMPRAS`) | Sí, solo el buscador | `ADMINISTRADOR` y `GERENTE_DE_COMPRAS` |

- El rol `ENCARGADO_DE_DISTRIBUCION` tiene los cuatro permisos de clientes y los de despacho y devolución de barriles; no tiene permisos de presentaciones comerciales.
- El módulo autoriza por rol: cada método lleva `@PreAuthorize("hasRole('ENCARGADO_DE_DISTRIBUCION')")`.
- Un acceso no autorizado no devuelve un 403: el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción". Sin sesión se redirige al login.
- La opción "Gestión de Clientes" está en la sección Administración del menú y solo la ve el encargado de distribución. La sección Administración se muestra al encargado de distribución con esa única opción. La opción queda resaltada en la lista, el alta y la edición.

## 2. Inventario de pantallas y rutas

| N.º | Pantalla | Ruta | Método | Vista |
| --- | --- | --- | --- | --- |
| 1 | Lista de clientes | `/clientes` (`?nombre=&direccion=&idLocalidad=&pagina=`) | GET | `cliente/cliente-lista` |
| 2 | Formulario de alta | `/clientes/nuevo` | GET | `cliente/cliente-form` |
| 3 | Crear un cliente | `/clientes` | POST | redirige a la lista (éxito) o vuelve al formulario (error) |
| 4 | Formulario de modificación | `/clientes/{id}/editar` | GET | `cliente/cliente-form` |
| 5 | Modificar un cliente | `/clientes/{id}` | POST | redirige a la lista (éxito) o vuelve al formulario (error) |
| 6 | Dar de baja un cliente | `/clientes/{id}/baja` | POST | redirige a la lista |

No hay pantalla de detalle: la tabla muestra todos los datos del cliente.

## 3. Descripción de cada pantalla

**Lista de clientes.** Título "Gestión de Clientes", subtítulo "Clientes en orden alfabético por nombre." y el botón "+ Nuevo cliente" a la derecha del encabezado. La tarjeta de filtros tiene Nombre (placeholder "Ej: Bar El Puerto"), Dirección (placeholder "Ej: Av. Corrientes 1234") y la ubicación con los tres buscadores enlazados País, Provincia y Localidad; "Limpiar" y "Buscar" van a la derecha de la tarjeta. La tabla es paginada de a 20 y lista los clientes activos en orden alfabético por nombre. Columnas: Nombre, Correo electrónico, Teléfono (un guion si no tiene), Ubicación (la localidad y, debajo, "Provincia, País"), Dirección y Acciones. Los textos largos se truncan y muestran el texto completo en un tooltip. La columna Acciones, fija a la derecha, tiene el lápiz (modificar) y la papelera (baja); la papelera abre el diálogo de confirmación "Dar de baja cliente" con la consecuencia "Si tiene un barril en su poder, el sistema no lo permitirá.". Sin resultados se muestra "No se encontraron clientes con los criterios indicados".

**Formulario.** Una sola página para el alta ("Nuevo cliente", botón "Crear cliente") y la modificación ("Editar cliente", botón "Guardar cambios"), con "Volver a clientes", el aviso de campos obligatorios y "Cancelar". El formulario no valida en el navegador (`novalidate`) y cada campo muestra el error del servidor debajo; un resumen superior lista todos los errores. Una fila por tema:

1. Nombre, obligatorio (placeholder "Ej: Bar El Puerto", hasta 100 caracteres).
2. Correo electrónico, obligatorio, único entre los clientes sin distinguir mayúsculas (placeholder "Ej: contacto@elpuerto.com", hasta 255 caracteres) y Teléfono, opcional, solo dígitos y hasta 15 (placeholder "Ej: 3764123456").
3. País, Provincia y Localidad, obligatorios, con los tres buscadores modales enlazados. Solo viaja el id de la localidad: país y provincia son ayuda de pantalla y se vuelven a cargar desde la localidad al editar y al volver con errores. Solo el administrador da de alta ubicaciones, por eso los selectores no ofrecen alta al vuelo.
4. Dirección, obligatoria, a todo el ancho debajo de la ubicación (placeholder "Ej: Av. Corrientes 1234", hasta 255 caracteres).

Alta contextual: el selector de cliente del despacho de un barril lleva a este formulario con `retorno` y `campo`; en ese caso el formulario dice "Volver al formulario anterior" y "Al guardar vuelve al formulario que estaba completando.", y tanto Cancelar como Crear cliente vuelven al despacho (el segundo con el cliente nuevo elegido).

Errores: el formato del correo, el correo duplicado y la localidad (faltante o inexistente) se marcan junto a su campo; las demás reglas del service (dato obligatorio vacío, teléfono con letras, longitud máxima) aparecen en el resumen. El formulario vuelve a mostrarse sin redirigir, con lo tipeado y la ubicación elegida.

**Baja.** Desde el diálogo de la lista. Si el cliente tiene un barril en su poder, el servicio rechaza la baja y se vuelve a la lista con la alerta "No se puede dar de baja el cliente porque tiene un barril en su poder". Un despacho cuyo barril ya fue devuelto no impide la baja.

## 4. Recorridos

| Objetivo | Rol | Recorrido | Clics |
| --- | --- | --- | --- |
| Dar de alta un cliente desde el despacho de un barril | Encargado de distribución | Despacho, menú de tres puntitos del selector de cliente, "Crear nuevo cliente", completar, Crear cliente (vuelve al despacho con el cliente elegido) | 3 más el formulario |
| Dar de alta un cliente | Encargado de distribución | Menú Gestión de Clientes, "+ Nuevo cliente", completar el formulario (la localidad con su buscador), Crear cliente | 3 más el formulario |
| Modificar un cliente | Encargado de distribución | Menú Gestión de Clientes, lápiz del cliente, cambiar los datos, Guardar cambios | 3 más el cambio |
| Dar de baja un cliente | Encargado de distribución | Menú Gestión de Clientes, papelera del cliente, confirmar | 3 |
| Buscar un cliente | Encargado de distribución | Menú Gestión de Clientes, completar los criterios, Buscar | 2 más los criterios |
