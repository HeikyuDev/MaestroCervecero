# Diseño de las pantallas del módulo de Ajustes de Insumo

Este documento define las pantallas del Controller de los ajustes de insumo: qué pantallas existen, sus rutas, qué rol puede hacer qué, cómo está armada cada una y cuántos clics lleva cada objetivo del usuario. El comportamiento verificable está en `../../Testing/casos_de_prueba/services/ingreso_insumo/CasosPruebaAjusteInsumo.md` y en `../../Testing/casos_de_prueba/controllers/ingreso_insumo`.

La aplicación es un monolito MVC con Thymeleaf: el servidor devuelve páginas HTML. Los textos están en español neutro, sin emojis y los mensajes no terminan en punto.

Un ajuste de insumo corrige la cantidad actual de un lote de insumo por una razón que no es un ingreso por compra ni un consumo (una merma, un recuento de inventario, un derrame). Su tipo lo define el motivo de ajuste elegido: un motivo de tipo Ingreso suma la cantidad al lote y uno de tipo Egreso la resta. Un ajuste registrado se puede anular; no se modifica ni se da de baja.

## 1. Roles y permisos

| Acción | Permiso | Encargado de depósito | Gerente de producción | Demás roles |
| --- | --- | --- | --- | --- |
| Ver la lista de ajustes y sus anulaciones | `AJUSTE_INSUMO_CONSULTAR` | Sí | Sí | No |
| Registrar un ajuste | `AJUSTE_INSUMO_REGISTRAR` | Sí | No | No |
| Anular un ajuste | `AJUSTE_INSUMO_ANULAR` | Sí | No | No |
| Crear un motivo desde el selector del formulario | `MOTIVO_AJUSTE_CREAR` | No | Sí | No |

- Cada método del Controller lleva `@PreAuthorize(hasAuthority(...))` con su permiso. Un acceso no autorizado no devuelve un 403: el `ControllerAdvices` redirige con la alerta `danger` "No tiene permisos para realizar esta acción".
- La opción "Ajustes de Insumo" (ícono `bi-sliders2`) está en la sección Operación del menú, debajo de "Stock de Insumos", con la clave activa `ajustes-insumo`, y la ve quien tiene `AJUSTE_INSUMO_CONSULTAR`. El título de la pestaña del navegador es "Ajustes de insumo - Maestro Cervecero".
- El gerente de producción solo consulta: no ve el botón "Nuevo ajuste", el botón "Anular" ni el botón "Ajustar" del detalle del lote de Stock de Insumos. Esos controles se ocultan con `sec:authorize="hasAuthority(...)"`, igual que el `@PreAuthorize` de cada método.
- El selector de motivo ofrece "Crear nuevo motivo" solo a quien tiene `MOTIVO_AJUSTE_CREAR`. Ese permiso es del gerente de producción, que no registra ajustes, de modo que el encargado de depósito no ve la opción y elige entre los motivos existentes.

## 2. Inventario de pantallas y rutas

| N.º | Pantalla | Ruta | Método | Vista |
| --- | --- | --- | --- | --- |
| 1 | Lista de ajustes de insumo | `/ajustes-insumo` (`?idLoteInsumo=&tipo=&estado=&fechaDesde=&fechaHasta=&pagina=`) | GET | `ingreso_insumo/ajuste-lista` |
| 2 | Formulario de alta | `/ajustes-insumo/nuevo` (`?idLoteInsumo=` opcional) | GET | `ingreso_insumo/ajuste-form` |
| 3 | Registrar un ajuste | `/ajustes-insumo` | POST | redirige a la lista (éxito) o vuelve al formulario con HTTP 200 (error) |
| 4 | Anular un ajuste | `/ajustes-insumo/{id}/anular` | POST | redirige a la lista |
| 5 | Buscador de insumo del formulario | `/ajustes-insumo/buscador/insumos` | GET | `ingreso_insumo/buscador-insumo :: buscador` |
| 6 | Buscador de lote del formulario, filtrado por el insumo elegido | `/ajustes-insumo/buscador/lotes` (`?idInsumo=`) | GET | `ingreso_insumo/buscador-lote-insumo :: buscador` |
| 7 | Buscador de motivo del formulario | `/ajustes-insumo/buscador/motivos` | GET | `ingreso_insumo/buscador-motivo-ajuste :: buscador` |
| 8 | Buscador de insumo del filtro de la lista | `/ajustes-insumo/filtros/insumos` | GET | `ingreso_insumo/buscador-insumo :: buscador` |
| 9 | Buscador de lote del filtro de la lista, filtrado por el insumo elegido | `/ajustes-insumo/filtros/lotes` (`?idInsumo=`) | GET | `ingreso_insumo/buscador-lote-insumo :: buscador` |

No hay pantalla de detalle: la tabla muestra todos los datos del ajuste y el detalle de una anulación se ve en un diálogo. Los buscadores 5 a 7 los ven quienes tienen `AJUSTE_INSUMO_REGISTRAR` y los buscadores 8 y 9 quienes tienen `AJUSTE_INSUMO_CONSULTAR`. El buscador de insumo reutiliza el fragmento `ingreso_insumo/buscador-insumo`, con la URL del buscador como atributo del modelo (`urlBuscador`), como lo hacen los ingresos de insumo.

Un `idLoteInsumo` inexistente en `/ajustes-insumo/nuevo` o en la lista no restringe ni preselecciona nada: el selector queda vacío. Una página fuera de rango muestra la última.

## 3. Reglas específicas

- **Tipo del ajuste.** Lo define el motivo elegido (Ingreso o Egreso); el formulario no tiene un campo de tipo.
- **Ingreso sobre un lote vencido.** Se rechaza: "El lote de insumo está vencido (venció el dd/MM/yyyy) y no admite ajustes de ingreso". Un lote vence si su fecha de vencimiento es anterior a hoy; el que vence hoy es vigente.
- **Egreso sobre un lote vencido.** Se admite, porque permite dar de baja la merma de un lote vencido.
- **Cantidad.** Mayor a 0, hasta 1.000.000 y hasta 4 decimales, expresada en la unidad del insumo del lote.
- **Egreso mayor a lo que hay.** No puede superar la cantidad actual del lote: "La cantidad a descontar no puede superar la cantidad actual del lote de insumo (<cantidad con unidad>)".
- **Egreso y reservas.** Si el egreso supera la cantidad disponible (la actual menos la reservada), reduce las reservas activas del lote en forma proporcional a su saldo pendiente, con el método del mayor resto a 4 decimales: lo quitado a las reservas suma exactamente el excedente (o todo el saldo pendiente, si este es menor) y la cantidad reservada del lote baja por esa misma suma. La anulación del ajuste revierte el efecto sobre la cantidad del lote y no restituye las reservas reducidas.
- **Observación.** Obligatoria, hasta 255 caracteres.
- **Anulación.** Solo un ajuste Registrado se puede anular, con un motivo obligatorio de hasta 255 caracteres. Anular un ajuste de tipo Ingreso se rechaza si dejaría la cantidad del lote por debajo de 0. El ajuste anulado permanece en la lista, con su motivo, quién lo anuló y cuándo.
- **Lote y motivo.** Se eligen entre los existentes; el lote debe ser de un insumo activo y el motivo debe estar activo.

## 4. Descripción de cada pantalla

**Lista de ajustes de insumo.** Título "Ajustes de Insumo" y subtítulo "Ajustes de insumo registrados, con el último ajuste arriba.". El botón ámbar "+ Nuevo ajuste" (`bi-plus-lg`) está en el encabezado de la tarjeta de la tabla, a la derecha, y existe solo para quien tiene `AJUSTE_INSUMO_REGISTRAR`. La tarjeta de filtros tiene, en una grilla de dos columnas con un tema por fila:

1. Lote de insumo (selector con buscador modal, limpiable, placeholder "Seleccione lote de insumo"): el modal pide primero el insumo y luego muestra sus lotes.
2. Tipo (Todos, Ingreso, Egreso) y Estado (Todos, Registrado, Anulado).
3. Fecha desde y Fecha hasta (campos de fecha, placeholders "Ej: 01/07/2027" y "Ej: 31/07/2027"), sobre la fecha de registro del ajuste, inclusive en ambos extremos y cualquiera de los dos puede omitirse; con el mismo orden y el mismo comportamiento que en el historial de Control de Equipamientos y de Control de Barriles.

"Limpiar" y "Buscar" van a la derecha de la tarjeta (`flex items-center justify-end gap-3` con `md:col-span-2`). Una fecha hasta anterior a la fecha desde se rechaza con la alerta de error "La fecha hasta no puede ser anterior a la fecha desde". Un estado o un tipo inválidos se ignoran con la alerta `warning` "El estado indicado no es válido, se muestran todos los ajustes" o "El tipo indicado no es válido, se muestran todos los ajustes".

La tabla lista los ajustes de a 20 por página, con el último registrado arriba (por ID descendente). Las filas anuladas se muestran atenuadas. Columnas:

- **Ajuste.** Dos líneas: la fecha y hora de registro y, debajo, el badge del estado (`lote/formato :: estadoTransaccion`).
- **Insumo y lote.** El nombre del insumo y, debajo, "Lote <identificación del proveedor> · vence <fecha>", con tooltip.
- **Motivo y tipo.** El nombre del motivo y, debajo, el tipo (Ingreso o Egreso).
- **Cantidad.** La cantidad con la unidad del insumo (`receta/formato :: medida`), con el signo "+" para un Ingreso y "-" para un Egreso.
- **Observación.** El texto completo, con `break-words` para que un texto largo se parta en varias líneas dentro de la celda.
- **Acciones.** El botón "i" (`fragments/detalle-anulacion :: boton`) en los ajustes anulados, que abre el diálogo compartido con el motivo, quién anuló y cuándo; y el botón rosa "Anular" en los registrados, solo para quien tiene `AJUSTE_INSUMO_ANULAR`, que abre el diálogo `fragments/dialogo-motivo` con el título "Anular ajuste de insumo", el resumen "Se anula el ajuste, se revierte su efecto sobre la cantidad del lote y queda registrado el motivo.", el ejemplo "Se cargó la cantidad equivocada" y el botón "Confirmar anulación". La columna Acciones existe siempre (`col-acciones`) y una fila sin botones deja la celda vacía.

La paginación conserva todos los criterios. Sin resultados: "No se encontraron ajustes de insumo con los criterios indicados".

**Formulario de alta.** Una página aparte con "Volver a ajustes de insumo", el título "Nuevo ajuste de insumo", el aviso de campos obligatorios y el formulario con `novalidate data-un-solo-envio` y el resumen de errores `fragments/form-errors :: resumen('No se pudo registrar el ajuste')`. Dos secciones, con un tema por fila:

- **Lote de insumo.**
  1. Insumo, obligatorio (selector de entidad, placeholder "Seleccione insumo"). Es un campo de ayuda de pantalla: solo viaja el ID del lote. Al elegir otro insumo se limpia el lote elegido.
  2. Lote de insumo, obligatorio (selector de entidad, placeholder "Seleccione lote de insumo"). Su buscador se abre filtrado por el insumo elegido y no se puede abrir sin insumo. Al elegir un lote se muestran debajo su fecha de vencimiento y su cantidad actual con la unidad del insumo; si llega `idLoteInsumo` por la URL, el lote y su insumo se preseleccionan.
- **Ajuste.**
  1. Motivo de ajuste, obligatorio (selector de entidad, placeholder "Seleccione motivo de ajuste"), con el tipo del motivo elegido (Ingreso o Egreso) debajo. Lleva un tooltip con el texto "Un egreso que supera la cantidad disponible del lote reduce en forma proporcional las reservas activas, y anular el ajuste no las restituye". La opción "Crear nuevo motivo" del menú del selector (`/configuracion-produccion/motivos-ajuste/nuevo`) se muestra solo con `MOTIVO_AJUSTE_CREAR`.
  2. Cantidad, obligatoria (`fragments/campo-unidad :: numero`, placeholder "Ej: 2,5"), con la unidad del insumo como sufijo (queda "-" mientras no hay insumo).
  3. Observación, obligatoria, en su propia fila a todo el ancho (área de texto, placeholder "Ej: Derrame durante el trasvase"). La ayuda dice "Hasta 255 caracteres.".

Los campos obligatorios llevan el asterisco rojo. El formulario no valida en el navegador: el service es la fuente de verdad. Al pie, "Cancelar" (vuelve a la lista) y el botón ámbar "Registrar ajuste".

Errores: un campo vacío o una regla del service se marcan junto a su campo (`idLoteInsumo`, `idMotivoAjuste`, `cantidad`, `observacion`) y en el resumen del formulario; un rechazo sin campo queda como error general. El formulario se vuelve a mostrar con HTTP 200 y se conservan el insumo, el lote, el motivo, la cantidad y la observación. Al registrar se vuelve a la lista con la alerta de éxito "Ajuste de insumo registrado correctamente".

**Acceso desde Stock de Insumos.** En la cabecera del detalle de un lote de insumo (`stock_insumo/stock-detalle-lote`), a la derecha del título, el botón ámbar "Ajustar" (`bi-sliders2`) lleva a `/ajustes-insumo/nuevo?idLoteInsumo=<id>` y se muestra solo a quien tiene `AJUSTE_INSUMO_REGISTRAR`. El formulario llega con el insumo y el lote elegidos; "Cancelar" vuelve a la lista de ajustes.

**Anulación.** Desde el diálogo de la lista. Al confirmar se vuelve a la lista con la alerta de éxito "Ajuste de insumo anulado correctamente". Los rechazos (motivo vacío o de más de 255 caracteres, ajuste ya anulado, ingreso que dejaría el lote en negativo) los resuelve el `ControllerAdvices`, que vuelve al origen con el motivo del rechazo.

## 5. Capas que sostienen las pantallas

- **Permisos.** `Permiso`, `Rol` y `RolTest` definen `AJUSTE_INSUMO_REGISTRAR`, `AJUSTE_INSUMO_ANULAR` y `AJUSTE_INSUMO_CONSULTAR`. El Encargado de depósito tiene los tres y el Gerente de producción solo `AJUSTE_INSUMO_CONSULTAR`.
- **`AjusteInsumoResponseDTO` y `MapperAjusteInsumo`.** El DTO lleva `id`, `cantidad`, `observacion`, `motivoAjuste`, `loteInsumo`, `estado`, `fechaAnulacion` y `motivoAnulacion`, y para la lista también:
  - `usuarioAnulacion` (String): quien anuló el ajuste, igual que en `IngresoInsumoResponseDTO`; el mapper lo carga con `getLastModifiedBy()` solo si el estado es `ANULADO`, como `MapperIngresoInsumo`.
  - `fechaRegistro` (LocalDateTime): la fecha y hora de registro del ajuste, que el mapper toma de `getCreatedDate()` de la auditoría. Es la misma fecha sobre la que el service aplica el rango de fechas.
- **`IAjusteInsumoServicio.filtrarAjustesInsumos`.** Recibe el rango como `LocalDateTime`; el Controller convierte la fecha desde a las 00:00 y la fecha hasta al final del día. El orden de la lista (ID descendente) lo define el `Pageable` que arma el Controller.
- **Buscador de motivos.** El Controller de ajustes lista los motivos activos con nombre y tipo (Todos, Ingreso, Egreso), en orden alfabético, con el service de motivos de ajuste, y los lotes de un insumo con su vencimiento y su cantidad actual con el service de lotes de insumo, que filtra por insumo.
- **Alta de motivo desde el selector.** La pantalla de alta de motivos (`/configuracion-produccion/motivos-ajuste/nuevo`) vuelve al formulario del ajuste con lo escrito y el motivo creado elegido, como lo hacen las demás altas ofrecidas desde un selector de entidad.
- **Detalle del lote de Stock de Insumos.** `stock-detalle-lote.html` muestra el botón "Ajustar" descrito arriba, con `sec:authorize="hasAuthority('AJUSTE_INSUMO_REGISTRAR')"`.

## 6. Recorridos

| Objetivo | Rol | Recorrido | Clics |
| --- | --- | --- | --- |
| Registrar un ajuste de un lote que ya se está mirando | Encargado de depósito | Detalle del lote en Stock de Insumos, "Ajustar", elegir el motivo, completar cantidad y observación, Registrar ajuste | 3 más el formulario |
| Registrar un ajuste desde cero | Encargado de depósito | Menú Ajustes de Insumo, "+ Nuevo ajuste", elegir insumo, elegir lote, elegir motivo, completar, Registrar ajuste | 6 más el formulario |
| Anular un ajuste | Encargado de depósito | Menú Ajustes de Insumo, "Anular" del ajuste, completar el motivo, Confirmar anulación | 3 más el motivo |
| Ver los ajustes de un lote | Encargado de depósito, gerente de producción | Menú Ajustes de Insumo, elegir el insumo y el lote en el filtro, Buscar | 4 más los criterios |
| Ver los egresos de un período | Encargado de depósito, gerente de producción | Menú Ajustes de Insumo, Tipo "Egreso", fechas desde y hasta, Buscar | 2 más los criterios |
| Ver por qué se anuló un ajuste | Encargado de depósito, gerente de producción | Menú Ajustes de Insumo, estado "Anulado", Buscar, botón "i" del ajuste | 3 más el criterio |
