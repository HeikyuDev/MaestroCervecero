# Estándares de interfaz

## Nombres de las entradas del menú

El prefijo de cada entrada del menú indica qué se hace en esa pantalla.

- **"Gestión de ...":** pantallas de alta, modificación y baja (ABM) de un maestro. Por ejemplo, Gestión de Insumos, Gestión de Recetas, Gestión de Barriles, Gestión de Equipamientos, Gestión de Proveedores, Gestión de Presentaciones, Gestión de Clientes, Gestión de Ubicaciones y Gestión de Usuarios.
- **"Control de ...":** pantallas donde se opera el día a día (registrar y anular operaciones que cambian el estado de un registro) y se consulta lo realizado. Por ejemplo, Control de Lotes, Control de Equipamientos y Control de Barriles.
- **Otras entradas** (órdenes de compra, planificaciones, ingresos, consumos): llevan el nombre propio de la operación que se realiza en ellas.

## Botón "Abrir"

Es la entrada desde la fila de una lista a la pantalla de trabajo o gestión de un registro. Hoy lo usa la lista de Control de Lotes; las listas de Control de Equipamientos y de Control de Barriles lo usan para llegar al panel del equipo o del barril.

- **Cuándo usarlo:** solo en la acción que abre la pantalla donde se trabaja con el registro. No se usa para editar (lápiz), dar de baja (papelera), anular, abrir un modal informativo ni consultar un detalle de solo lectura. Excepción: las pantallas de Stock de Insumos (lista de insumos y lotes de un insumo) lo usan para llegar al detalle de solo lectura del insumo y del lote de insumo.
- **Texto e ícono:** siempre "Abrir" con la flecha `bi-arrow-right`; es un botón visible con borde y fondo ámbar suave, no un ícono suelto ni un enlace de texto.
- **Sin tooltip:** el texto "Abrir" basta; no lleva `data-tooltip` ni `title`. Lleva un `aria-label` corto que identifica la fila, por ejemplo "Abrir LOT-R1-02".
- **Ubicación:** dentro de la celda `col-acciones` de la fila.
- **Fragmento:** `fragments/boton-abrir :: abrir(url, etiquetaAccesible)`. Lo protege `BotonAbrirTest`.

## Botón de registro en la fila

Es la acción de una fila que lleva al formulario de alta de una operación concreta sobre ese registro, por ejemplo la limpieza o el mantenimiento de un equipo o de un barril desde el panel del operario. "Abrir" entra a la pantalla donde se trabaja con el registro; el botón de registro entra directo al formulario de una operación, de modo que no se confunden.

- **Cuándo usarlo:** solo cuando la fila ofrece registrar una operación (limpieza, mantenimiento) y el destino es el formulario de alta de esa operación. Si el destino es el panel o el detalle del registro, va "Abrir".
- **Aspecto:** el mismo borde y fondo ámbar suaves de "Abrir" (`text-xs font-semibold`), con el ícono de la operación a la izquierda del texto y sin flecha. Los íconos son los de las pestañas del panel del equipo: `bi-stars` para la limpieza y `bi-wrench` para el mantenimiento.
- **Texto:** el nombre de la operación, "Registrar limpieza" o "Registrar mantenimiento".
- **Sin tooltip:** el texto basta; no lleva `data-tooltip` ni `title`. Lleva un `aria-label` que identifica la fila, por ejemplo "Registrar limpieza de FER-01".
- **Permiso:** se muestra solo con el permiso de registro de la operación (`LIMPIEZA_EQUIPAMIENTO_REGISTRAR`, `LIMPIEZA_BARRIL_REGISTRAR`, `MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR` o `MANTENIMIENTO_BARRIL_REGISTRAR`); sin él la celda queda vacía.
- **Ubicación:** dentro de la celda `col-acciones` de la fila, a la derecha.
- **Fragmento:** `fragments/boton-registrar :: registrar(url, texto, icono, etiquetaAccesible)`. Lo protege `BotonRegistrarTest`.

## Panel de control del operario

La pantalla de inicio del OPERARIO_DE_PRODUCCION muestra el panel de `home/dashboard-operario`. Se lee de arriba abajo en cuatro filas. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila.

- **Fila 1, contadores:** cuatro tarjetas (`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4`) con título en mayúsculas pequeñas, ícono y número grande. Cada contador lleva al listado filtrado correspondiente y sin el permiso de la lista queda como texto plano.
- **Fila 2:** "Lotes de hoy" a todo el ancho.
- **Fila 3:** "Limpiezas pendientes" a todo el ancho, con Equipos y debajo Barriles.
- **Fila 4, mantenimiento:** "Pendientes de mantenimiento" a todo el ancho, con Equipos y debajo Barriles, igual que "Limpiezas pendientes".
- **Bloques:** cada bloque es una tarjeta con su título, el contador del total real de sus registros y su enlace "Ver todos" en el encabezado; muestra hasta `LimitesTecnicos.PANEL_OPERARIO_FILAS_POR_BLOQUE` filas en la tabla compacta de las listas. Los lotes se abren con "Abrir" y las limpiezas y los mantenimientos se registran con el botón de registro.
- **Subgrupos apilados:** los subgrupos de un mismo bloque (Equipos y Barriles en las limpiezas y en el mantenimiento, En ejecución y A iniciar en los lotes) van uno debajo del otro, el de equipos primero, separados por una línea y con el mismo subencabezado y estilo de tabla; ningún par de subgrupos de un bloque va lado a lado.
- **Estado vacío:** un bloque sin filas muestra una sola línea de texto gris centrada ("No hay equipos en limpieza"), sin tabla, sin encabezado de columnas y sin tarjeta vacía.
- **Permisos:** cada atajo se protege con `sec:authorize` sobre el permiso de su destino, el mismo del `@PreAuthorize` del método al que lleva.
- Lo protegen `DashboardOperarioPlantillasTest` y `HomeControllerTest`.

## Panel de control del gerente de producción

La pantalla de inicio del GERENTE_DE_PRODUCCION muestra el panel de `home/dashboard-gerente`. Es de solo lectura: no tiene botones de registro y cada elemento lleva a una pantalla de consulta. Se lee de arriba abajo en cuatro filas. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila.

- **Fila 1, contadores:** cuatro tarjetas (`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4`) con el mismo estilo que las del operario: lotes en ejecución, lotes atrasados (ícono rosa si hay atrasados; tarjeta sin enlace), fermentadores ocupados sobre el total ("3 / 5") y equipos y barriles fuera de servicio.
- **Fila 2, producción:** "Producción en curso" a todo el ancho, con un subgrupo por etapa.
- **Fila 3:** "Cola de fermentadores" a todo el ancho.
- **Fila 4, planificaciones:** "Planificaciones pendientes" a todo el ancho.
- **Filas de lotes:** la producción en curso muestra lote, fermentador, prioridad (insignia de las planificaciones), fin estimado y acciones. El atraso se informa en el contador y en la cola de fermentadores ("Atrasado 3 días" en rosa). Los lotes se abren con "Abrir" y las planificaciones con el ojo "Ver".
- **Columna Acciones:** con un solo botón por celda, la hoja de estilos lo centra, por lo que el encabezado "Acciones" y el contenedor del botón van centrados (`text-center` y `justify-center`), con el mismo `px-2 py-3`. Vale para los paneles del gerente y del operario.
- **Estado vacío, subgrupos apilados y permisos:** las mismas reglas del panel del operario: una sola línea de texto gris sin tabla, los subgrupos de un bloque uno debajo del otro y cada enlace protegido con `sec:authorize` sobre el permiso de su destino.
- Lo protegen `DashboardGerentePlantillasTest` y `HomeControllerTest`.

## Panel de control del encargado de depósito

La pantalla de inicio del ENCARGADO_DE_DEPOSITO muestra el panel de `home/dashboard-encargado-deposito`. Es un panel de trabajo: cada fila ofrece el atajo a la operación que corresponde y el acceso a la consulta. Se lee de arriba abajo en cuatro filas. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila.

- **Fila 1, contadores:** cuatro tarjetas (`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4`) con el mismo estilo que las de los otros paneles: órdenes pendientes, entregas vencidas (ícono rosa si hay vencidas; tarjeta sin enlace), lotes vencidos (tarjeta sin enlace) e insumos sin stock.
- **Fila 2:** "Órdenes de compra pendientes" a todo el ancho.
- **Fila 3:** "Insumos sin stock disponible" a todo el ancho.
- **Fila 4:** "Lotes de insumo vencidos" a todo el ancho.
- **Acciones de fila:** el ingreso de una orden y el ajuste de un lote se registran con el botón de registro ("Registrar ingreso" con `bi-box-arrow-in-down`, "Registrar ajuste" con `bi-sliders2`); la orden se consulta con el ojo "Ver" y el lote o el insumo con "Abrir". Cuando una celda tiene más de un botón se agrupan centrados.
- **Columna Acciones:** el encabezado "Acciones" y el contenedor de los botones van centrados (`text-center` y `justify-center`), con el mismo `px-2 py-3`.
- **Estado vacío, subgrupos apilados y permisos:** las mismas reglas de los otros paneles: una sola línea de texto gris sin tabla y cada enlace protegido con `sec:authorize` sobre el permiso de su destino.
- Lo protegen `DashboardEncargadoDepositoPlantillasTest` y `HomeControllerTest`.

## Panel de control del administrador

La pantalla de inicio del ADMINISTRADOR muestra el panel de `home/dashboard-administrador`. Es de solo lectura: no tiene botones de registro y cada elemento lleva a una pantalla de consulta. Se lee de arriba abajo en cuatro filas. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila.

- **Fila 1, contadores:** dos tarjetas (`grid grid-cols-1 sm:grid-cols-2 gap-4`) con el mismo estilo que las de los otros paneles: usuarios activos y eventos de hoy.
- **Fila 2:** "Usuarios activos por rol" a todo el ancho, con una ficha por rol (nombre y cantidad) que es un enlace a la lista de usuarios de ese rol con el permiso de la lista y una tarjeta plana sin él.
- **Fila 3:** "Usuarios" a todo el ancho.
- **Fila 4:** "Actividad reciente" a todo el ancho.
- **Acciones de fila:** los usuarios se abren con "Abrir" y las entradas de la bitácora con el ojo "Ver", solo las que tienen detalle.
- **Columna Acciones:** el encabezado "Acciones" y el contenedor de los botones van centrados (`text-center` y `justify-center`), con el mismo `px-2 py-3`.
- **Estado vacío, subgrupos apilados y permisos:** las mismas reglas de los otros paneles: una sola línea de texto gris sin tabla y cada enlace protegido con `sec:authorize` sobre el permiso de su destino.
- Lo protegen `DashboardAdministradorPlantillasTest` y `HomeControllerTest`.

## Panel de control del gerente de compras

La pantalla de inicio del GERENTE_DE_COMPRAS muestra el panel de `home/dashboard-gerente-compras`. Es un panel de trabajo: la planificación sin orden de compra ofrece el atajo para registrarla y el resto de las filas llevan a la consulta. Se lee de arriba abajo en cuatro filas. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila.

- **Fila 1, contadores:** cuatro tarjetas (`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4`) con el mismo estilo que las de los otros paneles: planificaciones sin orden (tarjeta sin enlace), órdenes pendientes, entregas vencidas (ícono rosa si hay vencidas; tarjeta sin enlace) y órdenes finalizadas, cuyo título dice la ventana de días ("Finalizadas, últimos 30 días").
- **Fila 2:** "Planificaciones por comprar" a todo el ancho.
- **Fila 3:** "Órdenes de compra pendientes" a todo el ancho, con la columna "Recibido" que muestra el avance de la recepción en porcentaje.
- **Fila 4:** "Órdenes de compra finalizadas" a todo el ancho, con la columna "Cierre" (Automático o Manual).
- **Acciones de fila:** la orden de una planificación se registra con el botón de registro ("Registrar orden" con `bi-cart-plus`); las planificaciones y las órdenes se consultan con el ojo "Ver".
- **Columna Acciones:** el encabezado "Acciones" y el contenedor de los botones van centrados (`text-center` y `justify-center`), con el mismo `px-2 py-3`.
- **Estado vacío, subgrupos apilados y permisos:** las mismas reglas de los otros paneles: una sola línea de texto gris sin tabla y cada enlace protegido con `sec:authorize` sobre el permiso de su destino.
- Lo protegen `DashboardGerenteComprasPlantillasTest` y `HomeControllerTest`.

## Panel de control del encargado de distribución

La pantalla de inicio del ENCARGADO_DE_DISTRIBUCION muestra el panel de `home/dashboard-encargado-distribucion`. Es un panel de trabajo: cada barril ofrece el atajo a la operación que corresponde y el acceso a la consulta. Con este panel los seis roles tienen el suyo y la pantalla de inicio no tiene tarjetas de resumen genéricas. Se lee de arriba abajo en cuatro filas. Cada bloque ocupa una fila entera a todo el ancho; no se colocan dos bloques en la misma fila.

- **Fila 1, contadores:** cuatro tarjetas (`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4`) con el mismo estilo que las de los otros paneles: barriles en poder de clientes, devoluciones vencidas (ícono rosa si hay vencidas; tarjeta sin enlace), devoluciones que vencen hoy (tarjeta sin enlace) y barriles listos para despachar.
- **Fila 2:** "Devoluciones de hoy y vencidas" a todo el ancho, con la columna "Días de atraso".
- **Fila 3:** "Clientes con barriles en poder" a todo el ancho, con un cliente por fila y la cantidad de sus barriles.
- **Fila 4:** "Barriles listos para despachar" a todo el ancho, con la receta y el lote de la cerveza.
- **Acciones de fila:** la devolución y el despacho de un barril se registran con el botón de registro ("Registrar devolución" con `bi-box-arrow-in-down-left`, "Registrar despacho" con `bi-box-arrow-up-right`); el barril y el cliente se abren con "Abrir". Cuando una celda tiene más de un botón se agrupan centrados.
- **Columna Acciones:** el encabezado "Acciones" y el contenedor de los botones van centrados (`text-center` y `justify-center`), con el mismo `px-2 py-3`.
- **Estado vacío, subgrupos apilados y permisos:** las mismas reglas de los otros paneles: una sola línea de texto gris sin tabla y cada enlace protegido sobre el permiso de su destino.
- Lo protegen `DashboardEncargadoDistribucionPlantillasTest` y `HomeControllerTest`.

## Ícono del ojo "Ver ..."

Es el estándar para abrir una vista de detalle de solo lectura: órdenes de compra, planificaciones, recetas y entradas de la bitácora de auditoría. Usa el ícono `bi-eye` con un texto "Ver ..." específico (por ejemplo, "Ver orden") y no se reemplaza por el botón "Abrir".

**Ojo y "Abrir" juntos.** Cuando un registro tiene una pantalla de detalle de solo lectura y además un panel de trabajo, el ojo "Ver" abre el detalle y "Abrir" el panel. Ejemplo, el lote: la lista de lotes ofrece al GERENTE_DE_PRODUCCION el ojo (`/lotes/{id}/detalle`, "Detalle del lote": información, etapas, costos, consumos, reservas y envasados) y a continuación "Abrir" (`/lotes/{id}`, panel de trabajo); el OPERARIO_DE_PRODUCCION ve solo "Abrir", porque el detalle y sus costos son del gerente. Ambos van centrados en la columna de acciones y el ojo lleva solo `aria-label` ("Ver LOT-R1-02"), sin tooltip. Las barras del Gantt y las colas del cronograma siguen llevando al panel de trabajo.

**Auditoría.** En la lista de la bitácora el ojo (`/auditoria/{id}`, solo el ADMINISTRADOR) aparece únicamente en las entradas que guardaron un estado anterior o posterior; las demás dejan la celda de Acciones vacía. Lleva solo `aria-label` ("Ver detalle de la entrada 41"), sin tooltip.

**Comparación de estados (antes y después).** Una tabla que compara dos versiones de un registro lleva las columnas Campo, el estado anterior y el estado posterior, y resalta la fila completa según el cambio: `bg-amber-50` si el campo cambió, `bg-emerald-50` si solo existe en el posterior, `bg-rose-50` si solo existe en el anterior y sin resalte si quedó igual. En una fila cambiada el valor anterior va en `text-rose-700` y el posterior en `text-emerald-700`. Los valores sin dato se muestran con "-" y una referencia de colores precede a la tabla. Es el patrón del detalle de la bitácora (`auditoria/auditoria-detalle`); lo protege `AuditoriaPlantillasTest`.

**Diagramas de Gantt (colores de las barras).** El relleno de una barra de lote es la prioridad de su planificación, con los mismos colores que las insignias de prioridad: rojo (`#e11d48`) para Alta, ámbar (`#b45309`) para Media y gris (`#64748b`) para Baja, todos con texto blanco. El estado del lote nunca cambia el relleno: se expresa con el tratamiento de la barra y un ícono delante del texto. Pendiente es lisa con `bi-clock`; en ejecución lleva borde oscuro, rayas diagonales y `bi-play-fill`; finalizado se muestra apagado (menos saturado) con `bi-check2`; los cancelados no se dibujan. Una barra atenuada conserva su color con menor opacidad. Toda pantalla con un Gantt de lotes (cronograma de producción y estado actual de una planificación) muestra la leyenda "Prioridad: Alta, Media, Baja" sobre la leyenda de estados e informa la prioridad en el tooltip de cada barra. Lo implementa `planificacion_produccion/gantt` y lo protegen `CronogramaControllerTest` y los builders del Gantt.

**Equipamiento.** En la lista de Control de Equipamientos el ojo (solo el GERENTE_DE_PRODUCCION) abre el historial del equipo (`/control-equipamiento/{id}/historial`) y "Abrir" el panel operativo (`/control-equipamiento/{id}`), que el gerente ve en solo lectura. En las listas de Gestión de Equipamientos el ojo abre el detalle del equipamiento (`/equipamiento/{id}/detalle`).

**Barriles.** En la lista de Control de Barriles el ojo (solo el GERENTE_DE_PRODUCCION) abre el historial del barril (`/control-barriles/{id}/historial`) y "Abrir" el panel (`/control-barriles/{id}`), que el gerente ve en solo lectura y el operario de producción y el encargado de distribución con las pestañas de sus operaciones. El menú lo muestra como "Control de Barriles" en Operación, debajo de Control de Equipamientos, con el ícono `bi-clipboard-check`.

**Identificador de equipo.** Cuando una pantalla muestra el identificador de un equipamiento (el fermentador del lote, el equipo de una etapa) lo hace con el fragmento `fragments/enlace-equipo :: enlace(id, identificador)`: un enlace ámbar al detalle del equipamiento para quien tiene `EQUIPAMIENTO_CONSULTAR` y texto plano para los demás roles.

## Detalle largo en un diálogo: el botón "i"

Un texto que puede ser largo (el motivo de una anulación o cancelación, las observaciones de una operación) nunca se escribe dentro de la fila de una tabla: se muestra con el botón `bi-info-circle` de `fragments/detalle-anulacion` (`boton` para los motivos de anulación y cancelación, `finalizacion` para la finalización de una orden de compra y `observaciones` para las observaciones), que abre el diálogo pequeño compartido `fragments/dialogo-detalle-anulacion` con `/js/modales.js`. El botón entrega por atributo `data-detalle-*` el título, el texto, la etiqueta del texto ("Motivo" u "Observaciones"), la etiqueta de la fecha y la fecha y hora; sin ellos el diálogo usa los de una anulación. Lleva `aria-label` y `title` ("Ver detalle de la anulación" o "Ver observaciones"). En una pantalla de detalle, donde hay lugar, el texto se muestra completo en su propia fila con `break-words`. Lo protegen `ControlEquipamientoPlantillasTest` y `ControlBarrilPlantillasTest`.

**Todo botón de fila va en la columna Acciones.** El botón i (de anulación, cancelación u observaciones), el ojo "Ver", modificar, baja, "Anular" y "Abrir" van siempre en la celda de la columna Acciones (`col-acciones`), a la derecha de todo, nunca en otra celda junto al badge de estado o a un dato. El orden dentro de la celda es: i (observaciones, luego anulación), ojo, y al final anular, modificar o baja; los que no aplican no se renderizan y una fila sin ningún botón deja la celda vacía. La columna Acciones existe siempre (encabezado y celdas, sin `th:if` ni `sec:authorize` en la columna): solo el contenido de cada celda se condiciona al permiso y al estado, de modo que la i se ve también cuando la etapa no es operable o el usuario no puede anular. Las tablas de solo lectura con anulados (consumos y envasados del Detalle del lote) tienen su columna Acciones solo con la i. Excepción: en la cabecera de un detalle (planificación, orden de compra, lote cancelado) la i va junto al badge de estado, porque no es una fila. Lo verifica `BotonesDeFilaEnAccionesTest`.

**Botones de los filtros a la derecha.** En toda lista con tarjeta de filtros, "Limpiar" y "Buscar" van en un contenedor `flex items-center justify-end gap-3` que ocupa la fila completa de la grilla de filtros (`md:col-span-2` en una grilla de dos columnas, `lg:col-span-3` en una de tres, `col-span-full` en cualquier caso), de modo que quedan en el borde derecho de la tarjeta con cualquier cantidad de filtros. Excepción: la barra de filtro en línea de las pantallas de consumos y de envasados. Lo verifica `BotonesDeFiltrosALaDerechaTest`.

**Operario de producción: solo consulta de la administración.** El OPERARIO_DE_PRODUCCION ve en la sección Administración del menú Gestión de Insumos, Gestión de Recetas, Gestión de Barriles y Gestión de Equipamientos (mismas opciones, mismo orden y mismo resaltado que el gerente de producción), con sus listas, filtros, pestañas y detalles (el ojo de equipamiento y de recetas). Nunca ve ni ejecuta altas, modificaciones ni bajas: los botones "Nuevo ...", el lápiz y el tacho se ocultan con `sec:authorize="hasRole('GERENTE_DE_PRODUCCION')"`, igual que el `@PreAuthorize` de esos métodos, que sigue exclusivo del gerente. Si la columna Acciones queda sin ningún botón (maltas, lúpulos, levaduras, barriles y fabricantes, cuyas tablas ya muestran todos los datos), se oculta entera para el operario; si le queda el ojo, el ojo va centrado. No se crean pantallas de detalle donde la tabla ya muestra todos los datos.

**Pestaña con tabla y alta en un formulario aparte.** Cada pestaña de un panel (consumos del lote, operaciones del equipamiento) es la tabla paginada de su tipo, con el botón ámbar de alta (`bi-plus-lg`, "Nuevo consumo reservado", "Nueva limpieza") en el encabezado de la tarjeta; el botón solo existe para quien puede registrar y solo si el estado lo permite, y si no un aviso gris explica el motivo. El botón abre una página de formulario con "Cancelar" y la acción ("Registrar limpieza"); al registrar se vuelve a la pestaña con la alerta de éxito y un error vuelve al formulario con lo escrito. La columna Acciones de estas tablas va a la derecha y la de las listas centrada.

## Otros estándares

- Columna de acciones fija a la derecha: clase `col-acciones` (protegida por `ColumnaAccionesStickyTest`).
- Pestañas: fragmento `fragments/pestanas`. Toda pestaña lleva un ícono que la acompaña (Bootstrap Icons con `fragments/pestanas :: bi(...)`, o los íconos propios del módulo); pasar `null` como ícono no está permitido. Las pestañas de etapa del lote (`lote/lote-detalle`) también llevan ícono. La regla está protegida por `PestanasConIconoTest`.
- Campo numérico con la unidad como sufijo: fragmento `fragments/campo-unidad`. La unidad (L, kg, kg/h, %, $, g, IBU, min, días, unidades) se muestra siempre como sufijo dentro del campo y nunca en la etiqueta ("Capacidad total", no "Capacidad total (L)"). Variantes: `fijo` (ligado con `th:field`) y `medida` (valor y nombre propios) para una unidad fija, `dialogo` para los campos de los diálogos y `numero` cuando la unidad depende del insumo elegido (sin insumo el sufijo muestra "-", o "$" en el precio unitario). Los encabezados de columna de las listas pueden conservar la unidad.
- Ayudas de un campo: la ayuda estática es texto de ayuda (color `text-slate-400`, punto final) y no un mensaje de error. Todo campo único lleva "Único entre los X, sin distinguir mayúsculas." y todo campo con tope de longitud lleva "Hasta N caracteres.". Todo diálogo de motivo indica "El motivo admite hasta 255 caracteres.".
- Todo formulario de alta o modificación y el diálogo de baja llevan `novalidate data-un-solo-envio`. El autofocus va en el primer campo editable que no es un selector.
- Selector de entidad con buscador modal: fragmento `fragments/selector-entidad`.
- Estructura común de todo buscador modal (`buscador-*.html`): formulario de filtros en una grilla de dos columnas (`p-6 grid grid-cols-1 sm:grid-cols-2 gap-4 items-end`) con "Buscar" en un contenedor `sm:col-span-2 flex items-center justify-end gap-3` a la derecha; la tabla `tabla-buscador` dentro de un `contenedor-buscador` (desplazamiento horizontal controlado, nunca recortada por el modal); y la acción de la fila, un botón "Elegir", en la columna `col-acciones` (fija a la derecha y siempre visible) con el mismo relleno `px-3 py-3` en encabezados y celdas. El modal (`/js/modales.js`) tiene ancho máximo `max-w-3xl`. Protegida por `BuscadoresModalEstructuraTest`.
- Criterios de un buscador: cada buscador filtra por los mismos criterios que la pantalla de Gestión de su entidad. Un criterio cuyo valor fija el contexto (por ejemplo, el estado Pendiente de una planificación o Disponible de un barril, o el insumo ya elegido) no se muestra. Los links de paginación conservan todos los criterios.
- Ubicación de un formulario (país, provincia y localidad): tres selectores de entidad enlazados con el fragmento `fragments/ubicacion-enlazada` y `/js/ubicacion-enlazada.js`, cada uno con su buscador en modal (`/ubicaciones/paises|provincias|localidades/buscador`, roles `ADMINISTRADOR`, `ENCARGADO_DE_DISTRIBUCION` y `GERENTE_DE_COMPRAS`). Elegir un país limpia la provincia y la localidad que no son de él; elegir una provincia completa el país y limpia la localidad que no es suya; elegir una localidad completa provincia y país. Los buscadores de provincia y de localidad se abren filtrados por lo ya elegido (el filtro se puede quitar o cambiar) y cada botón "Elegir" expone los datos del padre (`data-id-pais`, `data-texto-pais`, `data-id-provincia`, `data-texto-provincia`). Solo viaja el id de la localidad; país y provincia son ayuda de pantalla. En listas y detalles se muestran país, provincia y localidad.
- Alertas con lista de ítems: fragmento `fragments/alerta-mensaje` con `MensajeConLista`.
- Acciones secundarias o de trazabilidad de una tarjeta: menú de tres puntos con el fragmento `fragments/menu-opciones` y `/js/menu-opciones.js`. El botón (`bi-three-dots-vertical`) solo lleva `aria-label`, sin tooltip ni `title`; se cierra con clic afuera o Escape y se maneja con teclado. Las opciones que muestran información abren un `<dialog data-dialogo-informativo>` (de solo lectura) con `opcionDialogo`. La acción principal de la tarjeta va como botón ámbar a la izquierda del menú.
- Acceso a una pantalla de gestión desde el detalle (consumos, envasados): botón sólido ámbar "Gestionar ..." en la cabecera de la tarjeta, a la derecha del título, con ícono (`bi-list-check`, `bi-box-seam`). La pantalla de gestión muestra el resumen y la lista de lo registrado, con el botón ámbar de alta ("Nuevo consumo reservado", "Nuevo envasado") que lleva a una pantalla de formulario aparte; el registro nunca vive en la lista.

## Orden y agrupación de campos

En todos los formularios y los detalles de solo lectura los datos se agrupan por sentido y se leen de izquierda a derecha y de arriba abajo, con los campos relacionados en la misma fila lógica:

- Un tema por fila: cada fila (una grilla) trata un solo tema y se lee de izquierda a derecha. Si un tema no completa la fila, la fila queda incompleta (o el campo ocupa más columnas); nunca se rellena con datos de otro tema. Orden de los temas: identidad, relación u origen, medidas con su unidad, mantenimiento, fechas (estimadas y luego reales) y estado u observaciones.
- Nunca se mezclan en una misma fila datos de temas distintos (por ejemplo, dirección junto al correo): identidad con identidad, contacto con contacto, ubicación con ubicación, cantidades con sus unidades y fechas con fechas.
- Orden habitual: identificación, contacto, ubicación (país, provincia y localidad en una fila y la dirección debajo, a todo el ancho).
- Un dato largo (dirección, observaciones) ocupa su propia fila con `col-span` a todo el ancho.
- El formulario y el detalle del mismo módulo usan el mismo orden.
- Los títulos de bloque se usan solo donde ayudan; se conservan el asterisco rojo, los placeholders, los `id` y los `name` de los campos.
- El ABV vive solo en la receta; ninguna pantalla de lote muestra ABV ni alcohol estimado. Los datos de la receta (OG, FG, IBU) se ven en la guía de Fermentación y en el detalle de la receta, no en el Detalle del lote, que enlaza con la receta.
- Totales: los totales van en una tabla de resumen (Concepto e Importe, con el total en negrita) o en una fila de total con estilo propio (fondo blanco, borde superior marcado y negrita); nunca con el estilo del encabezado de columnas (fondo gris suave, mayúsculas y texto gris). Las tablas de detalle de costos no llevan fila de total y cada una va en su propia tarjeta con título, separadas verticalmente.
- En una tabla el encabezado y la celda de cada columna usan el mismo relleno horizontal, para que queden en el mismo borde izquierdo.
- Proveedor es la referencia y el resto de las pantallas siguen la misma regla (protegidas por `OrdenDeCamposTest`).
- Dato de solo lectura dentro de un formulario de edición: un dato que se fija en el alta y no se puede cambiar (la capacidad total de un fermentador, un macerador o una olla de hervor y la capacidad de un barril) se muestra en la edición como texto, no como input: la etiqueta sin unidad ("Capacidad total"), el valor con la unidad (`receta/formato :: medida(valor, 'L')`, por ejemplo "100 L") y la ayuda "No se puede modificar.", en la misma posición de la misma fila que ocupa el input en el alta. El bloque lleva `data-capacidad-solo-lectura`, sin `id` ni `name` de input, y el valor sale del dato guardado, nunca del pedido. El alta mantiene el input editable, con el asterisco rojo y la unidad en el sufijo.

## Scroll conservado

- `static/js/conservar-scroll.js` (incluido al final del body de `layout/base.html`) hace que el sidebar no vuelva al inicio al navegar: guarda su `scrollTop` en `sessionStorage` (`scroll-sidebar`) y lo restaura al cargar.
- Al cambiar de pestaña (`fragments/pestanas` y las pestañas de etapa del lote) se conserva también el scroll del contenedor `<main>` (`scroll-pestana`, solo si la ruta cargada es la del enlace pulsado). El resto de las navegaciones abre arriba.
- Todo acceso a `sessionStorage` va en `try/catch`. Protegido por `ConservarScrollTest`.

## Importes

- Todo importe en pesos que se muestra lleva el símbolo delante con un espacio y el formato numérico de `FormatoImporte` (`FormatoImporte.conSimbolo`, o `conSimbolo` en el cálculo de `orden-compra-form.js`): `$ 23.460,00`. Un importe por unidad se escribe `$ 5,05 / kg` o `$ 15,00 / L`.
- Los encabezados y etiquetas de valores mostrados no repiten la unidad ("Subtotal", no "Subtotal ($)"). En los campos de formulario la unidad va en el sufijo `campo-unidad` ("$", "$ por litro", "$ / kg").
- Protegido por `ImporteConSimboloTest`.


## Estadísticas y reportes impresos

Todas las estadísticas viven en una sola pantalla, "Estadísticas" (`/estadisticas`), con una pestaña por reporte. Todo reporte tiene la misma estructura, la muestra la misma plantilla en pantalla y la misma plantilla en PDF: agregar un reporte no agrega plantillas.

**Menú y permisos.** Una sola entrada "Estadísticas" (`bi-bar-chart`, clave activa `estadisticas`) en la sección "Reportes", debajo de Administración, visible con `ESTADISTICA_PRODUCCION_CONSULTAR` o `ESTADISTICA_COMPRAS_CONSULTAR`. Cada pestaña declara su permiso y solo la ve quien lo tiene (las pestañas Tiempos y Recepción, además, solo con `estadisticas.tiempos.habilitada=true` y `estadisticas.recepcion.habilitada=true`: su `ReporteDePestana` lleva `@ConditionalOnProperty` y sin él la clave cae en la primera pestaña del usuario); sin pestaña indicada se muestra la primera que el usuario puede ver. La pantalla y el PDF llevan el mismo `@PreAuthorize` con los dos permisos.

**Estructura única** (`ReporteEstadisticoResponseDTO`, en `service/response_dto/estadisticas`):

- `titulo` y `descripcion`: la pregunta que responde el reporte, en palabras simples y sin punto final (por ejemplo, "Cuántos lotes se planificaron en el período y cómo terminaron").
- `criterioDeFecha`: qué fecha decide qué registros entran en el período, en una línea sin punto final (por ejemplo, "Lotes con inicio estimado dentro del período"); se muestra debajo del período, en la pantalla y en el PDF. Todo reporte la declara.
- `desde`, `hasta` y `criterios` (`CriterioAplicadoResponseDTO`: etiqueta y valor, con "Todos" o "Todas" si no se indicó).
- `indicadores` (`IndicadorResponseDTO`, como máximo 4): un nombre corto, el valor ya formateado ("-" si no se puede calcular), una aclaración opcional de una línea y una definición opcional que se muestra con el tooltip del proyecto.
- `grafico` (`GraficoReporteResponseDTO`): un único gráfico de barras horizontales simples, con su título, una barra por categoría (`DatoGraficoResponseDTO`), la unidad y la forma de escribir el valor: cantidad con su unidad, importe (`moneda`, "$ 15,20 / L") o variación con signo (`variacion`, "+12 %"; sus barras negativas usan el azul del sistema y el largo es proporcional al valor absoluto). Un gráfico de valores que se suman muestra las ocho categorías mayores y "Otros"; uno de razones o porcentajes, solo las ocho mayores.
- `tabla` o `tablas` (`TablaReporteResponseDTO`, como máximo 7 columnas cada una; un reporte lleva una tabla o una lista de tablas con título, una debajo de la otra y sin paginación, como Costos, con una por receta; `getTabla()` es la primera): las columnas (`ColumnaReporteResponseDTO` con su `TipoColumna`: `TEXTO`, `NUMERO`, `PORCENTAJE`, `MONEDA`, `FECHA` o `ENLACE`, que define la alineación), una página de filas (`FilaReporteResponseDTO` con una `CeldaReporteResponseDTO` por columna, ya formateada; una celda puede llevar un texto secundario debajo) y la fila de totales del período completo. La columna `ENLACE` es "Acciones", centrada, con el botón "Abrir" (`ColumnaReporteResponseDTO.enlace`, hacia una pantalla de trabajo, por ejemplo `/lotes/{id}`) o el ojo "Ver" (`ColumnaReporteResponseDTO.ver`, hacia un detalle de solo lectura, por ejemplo `/ordenes-compra/{id}`), protegido por el permiso de la columna; una celda sin ID no muestra el botón. No se imprime.

Los topes se validan al construir la estructura: un reporte con más de 4 indicadores o una tabla con más de 7 columnas no se arma.

**Pantalla** (`estadisticas/estadisticas.html` con las piezas de `estadisticas/fragments/reporte.html`): pestañas (`fragments/pestanas`, con ícono), la línea de la pregunta y, en este orden:

1. Filtros: formulario GET con `novalidate` y la misma grilla que los filtros de las listas (`grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 items-end`), con cada campo a su ancho normal de columna: "Desde" y "Hasta" obligatorios, con asterisco rojo y placeholder (sin parámetros, del primer día del mes en curso a hoy), y hasta dos criterios propios (`CampoFiltro`: lista simple con "Todos"/"Todas", o selector de entidad con su buscador en modal). "Limpiar" y "Filtrar" van en su propia fila debajo de los campos, a la derecha (`col-span-full flex items-center justify-end gap-3`), igual que en el resto de las listas.
2. Encabezado de resultados (`resultados`), solo si los criterios son válidos: "Resultados del dd/MM/yyyy al dd/MM/yyyy" (el período aplicado) a la izquierda, con la fecha considerada debajo en letra chica gris, y, en la misma línea a la derecha, el botón secundario "Exportar PDF" (con borde, sin relleno, `bi-file-earmark-pdf`), que abre el PDF en otra pestaña con los mismos criterios. No va arriba a la derecha junto al título: ese lugar es de las altas ("+ Nuevo").
3. Los indicadores, todos en una fila que ocupa todo el ancho (hasta cuatro tarjetas, como en los paneles de control y en el PDF): la grilla toma tantas columnas como tarjetas (`sm:grid-cols-2 lg:grid-cols-4` con cuatro, `md:grid-cols-3` con tres, `sm:grid-cols-2` con dos) para no dejar un hueco y, con menos de cuatro, las tarjetas llevan más relleno (`p-7`) y el valor es más grande (`text-4xl`). Las clases se escriben completas en la plantilla para que Tailwind las detecte.
4. Debajo, a todo el ancho, el gráfico (`fragments/grafico.html`): barras horizontales dibujadas en el servidor con HTML y CSS a partir de `GraficoBarras`, una fila por categoría con la etiqueta, la barra (su largo es un porcentaje del ancho de la tarjeta; la más larga, el 75 %) y el valor escrito a continuación, al tamaño del texto de las tablas y sin espacio vacío. Lleva `role="img"` con todos los valores en `aria-label`. Sin valores dice "Sin datos para graficar".
5. La tabla, paginada con `fragments/paginacion` (de a 20), con la fila de totales con estilo propio y, sin filas, "Sin resultados para los criterios indicados". Si el reporte tiene varias tablas, cada una va en su propia tarjeta con su título (`data-titulo-tabla`), una debajo de la otra, sin paginación.

**Validación.** Solo en el servidor. El Service valida el período con `PeriodoEstadistica.validar`: los dos extremos obligatorios, la fecha hasta no anterior a la desde y hasta `LimitesTecnicos.RANGO_MAXIMO_ESTADISTICAS_DIAS` (366) días contando ambos extremos. Un rechazo se muestra con la alerta amarilla del layout, se conservan los criterios y en lugar del reporte se pide un período válido. Una fecha que no existe se informa con "La fecha desde no es válida" o "La fecha hasta no es válida". Un valor de lista inexistente no filtra y se avisa; una entidad que el buscador no ofrece no filtra. Si el PDF no se puede generar por los criterios, su ruta vuelve a la pantalla con los mismos parámetros, que muestra el motivo. Si el documento falla al armarse, o ya hay `LimitesTecnicos.REPORTE_IMPRESO_SIMULTANEOS_MAXIMO` (3) reportes en generación y no se libera un lugar en `LimitesTecnicos.REPORTE_IMPRESO_ESPERA_MAXIMA_MS` (2 segundos), la ruta vuelve a la pantalla con la alerta amarilla "No se pudo generar el reporte impreso" o "Hay otros reportes en generación, intente nuevamente en unos instantes"; la falla se registra en el log con la pestaña y el período, sin datos personales.

**Reporte impreso** (`/estadisticas/pdf`, plantilla única `estadisticas/pdf/reporte.html`, generada con `IGeneradorPdfServicio`):

- Siempre en hoja A4 vertical, con la fuente Liberation Sans incrustada.
- Encabezado en cada página: "Maestro Cervecero", el título del reporte y una línea.
- Datos de emisión en la primera página: "Emitido por" (usuario y, entre paréntesis, nombre y apellido), "Fecha de emisión" (dd/MM/yyyy HH:mm), "Terminal" (el equipo desde el que se pidió el reporte: la dirección es la de la conexión que pidió el reporte, sin tener en cuenta los encabezados que escribe el cliente, como `X-Forwarded-For`; una dirección de loopback se reemplaza por el nombre y la dirección del servidor; el nombre del equipo se resuelve por DNS inverso solo para direcciones IP, con un tiempo máximo de 300 ms y en un grupo reducido de hilos propio, el resultado (también cuando no hay nombre) se conserva 10 minutos por dirección y se muestra `nombre-de-equipo (IP)`, o solo la IP si no se resuelve), "Período", "Fecha considerada" y "Criterios de filtro" con todos los criterios.
- Cuerpo: la pregunta, los mismos indicadores (con sus definiciones escritas debajo), el mismo gráfico (170 mm de ancho) y las tablas completas (cada una con su título si el reporte tiene varias) sin paginar y sin la columna de acciones, hasta `LimitesTecnicos.REPORTE_IMPRESO_MAXIMO_FILAS` filas (si el período tiene más, se avisa antes de la tabla). El encabezado de la tabla se repite en cada página, ninguna fila se parte y los totales van al final.
- Pie en cada página: la fecha de emisión, "Maestro Cervecero" y "Página X de Y".
- Respuesta `application/pdf` en línea con el nombre `estadisticas-<pestaña>_<desde>_<hasta>.pdf`.

La plantilla del PDF es XHTML bien formado y no usa enlaces `@{/...}` ni atributos de seguridad: se procesa fuera de la petición web.

**Agregar un reporte.**

1. Un método en el Service de estadísticas de su área que valida el período con `PeriodoEstadistica.validar` y devuelve un `ReporteEstadisticoResponseDTO` para la página pedida de la tabla, con sus consultas agregadas en el repositorio de estadísticas (sin consultas por fila).
2. Una constante en `PestanaEstadistica` (en `service/interfaces/estadisticas`) con su clave, texto, ícono, pregunta y permiso. La pregunta se escribe solo ahí: el Service la toma de esa constante para el reporte y la pantalla la muestra de la misma constante.
3. Un componente `ReporteDePestana` (como `ReporteLotes`) que declara sus criterios propios (`CampoFiltro`, como máximo dos), los interpreta y llama al método del Service. `CamposDeReporte` ya resuelve los criterios que se repiten: receta, proveedor, tipo de insumo, etapa y tipo de equipo. Un criterio nuevo con buscador en modal agrega su ruta de buscador, protegida con el permiso de la pestaña.

La pantalla, el PDF, los permisos, el período, la paginación y el menú no cambian.
