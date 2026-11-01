# Estándares de interfaz

## Botón "Abrir"

Es la entrada desde la fila de una lista a la pantalla de trabajo o gestión de un registro. Hoy lo usa la lista de Gestión de Lotes; las listas de equipamiento y de barriles lo usarán para llegar a su pantalla de ciclo de vida.

- **Cuándo usarlo:** solo en la acción que abre la pantalla donde se trabaja con el registro. No se usa para editar (lápiz), dar de baja (papelera), anular, abrir un modal informativo ni consultar un detalle de solo lectura.
- **Texto e ícono:** siempre "Abrir" con la flecha `bi-arrow-right`; es un botón visible con borde y fondo ámbar suave, no un ícono suelto ni un enlace de texto.
- **Sin tooltip:** el texto "Abrir" basta; no lleva `data-tooltip` ni `title`. Lleva un `aria-label` corto que identifica la fila, por ejemplo "Abrir LOT-R1-02".
- **Ubicación:** dentro de la celda `col-acciones` de la fila.
- **Fragmento:** `fragments/boton-abrir :: abrir(url, etiquetaAccesible)`. Lo protege `BotonAbrirTest`.

## Ícono del ojo "Ver ..."

Es el estándar para abrir una vista de detalle de solo lectura: órdenes de compra, planificaciones y recetas. Usa el ícono `bi-eye` con un texto "Ver ..." específico (por ejemplo, "Ver orden") y no se reemplaza por el botón "Abrir".

**Ojo y "Abrir" juntos.** Cuando un registro tiene una pantalla de detalle de solo lectura y además un panel de trabajo, el ojo "Ver" abre el detalle y "Abrir" el panel. Ejemplo, el lote: la lista de lotes ofrece al GERENTE_DE_PRODUCCION el ojo (`/lotes/{id}/detalle`, "Detalle del lote": información, etapas, costos, consumos, reservas y envasados) y a continuación "Abrir" (`/lotes/{id}`, panel de trabajo); el OPERARIO_DE_PRODUCCION ve solo "Abrir", porque el detalle y sus costos son del gerente. Ambos van centrados en la columna de acciones y el ojo lleva solo `aria-label` ("Ver LOT-R1-02"), sin tooltip. Las barras del Gantt y las colas del cronograma siguen llevando al panel de trabajo.

**Equipamiento.** En la lista de Control de Equipamiento el ojo (solo el GERENTE_DE_PRODUCCION) abre el historial del equipo (`/control-equipamiento/{id}/historial`) y "Abrir" el panel operativo (`/control-equipamiento/{id}`), que el gerente ve en solo lectura. En las listas de Gestión de Equipamiento el ojo abre el detalle del equipamiento (`/equipamiento/{id}/detalle`).

**Identificador de equipo.** Cuando una pantalla muestra el identificador de un equipamiento (el fermentador del lote, el equipo de una etapa) lo hace con el fragmento `fragments/enlace-equipo :: enlace(id, identificador)`: un enlace ámbar al detalle del equipamiento para quien tiene `EQUIPAMIENTO_CONSULTAR` y texto plano para los demás roles.

## Detalle largo en un diálogo: el botón "i"

Un texto que puede ser largo (el motivo de una anulación o cancelación, las observaciones de una operación) nunca se escribe dentro de la fila de una tabla: se muestra con el botón `bi-info-circle` de `fragments/detalle-anulacion` (`boton` para los motivos y `observaciones` para las observaciones), que abre el diálogo pequeño compartido `fragments/dialogo-detalle-anulacion` con `/js/modales.js`. El botón entrega por atributo `data-detalle-*` el título, el texto, la etiqueta del texto ("Motivo" u "Observaciones"), la etiqueta de la fecha y la fecha y hora; sin ellos el diálogo usa los de una anulación. Lleva `aria-label` y `title` ("Ver detalle de la anulación" o "Ver observaciones"). En una pantalla de detalle, donde hay lugar, el texto se muestra completo en su propia fila con `break-words`. Lo protege `ControlEquipamientoPlantillasTest`.

**Pestaña con tabla y alta en un formulario aparte.** Cada pestaña de un panel (consumos del lote, operaciones del equipamiento) es la tabla paginada de su tipo, con el botón ámbar de alta (`bi-plus-lg`, "Nuevo consumo reservado", "Nueva limpieza") en el encabezado de la tarjeta; el botón solo existe para quien puede registrar y solo si el estado lo permite, y si no un aviso gris explica el motivo. El botón abre una página de formulario con "Cancelar" y la acción ("Registrar limpieza"); al registrar se vuelve a la pestaña con la alerta de éxito y un error vuelve al formulario con lo escrito. La columna Acciones de estas tablas va a la derecha y la de las listas centrada.

## Otros estándares

- Columna de acciones fija a la derecha: clase `col-acciones` (protegida por `ColumnaAccionesStickyTest`).
- Pestañas: fragmento `fragments/pestanas`. Toda pestaña lleva un ícono que la acompaña (Bootstrap Icons con `fragments/pestanas :: bi(...)`, o los íconos propios del módulo); pasar `null` como ícono no está permitido. Las pestañas de etapa del lote (`lote/lote-detalle`) también llevan ícono. La regla está protegida por `PestanasConIconoTest`.
- Campo numérico con la unidad como sufijo: fragmento `fragments/campo-unidad`.
- Selector de entidad con buscador modal: fragmento `fragments/selector-entidad`.
- Ubicación de un formulario (país, provincia y localidad): tres selectores de entidad enlazados con el fragmento `fragments/ubicacion-enlazada` y `/js/ubicacion-enlazada.js`, cada uno con su buscador en modal (`/ubicaciones/paises|provincias|localidades/buscador`, roles `ADMINISTRADOR`, `GERENTE_COMERCIAL` y `GERENTE_DE_COMPRAS`). Elegir un país limpia la provincia y la localidad que no son de él; elegir una provincia completa el país y limpia la localidad que no es suya; elegir una localidad completa provincia y país. Los buscadores de provincia y de localidad se abren filtrados por lo ya elegido (el filtro se puede quitar o cambiar) y cada botón "Elegir" expone los datos del padre (`data-id-pais`, `data-texto-pais`, `data-id-provincia`, `data-texto-provincia`). Solo viaja el id de la localidad; país y provincia son ayuda de pantalla. En listas y detalles se muestran país, provincia y localidad.
- Alertas con lista de ítems: fragmento `fragments/alerta-mensaje` con `MensajeConLista`.
- Acciones secundarias o de trazabilidad de una tarjeta: menú de tres puntos con el fragmento `fragments/menu-opciones` y `/js/menu-opciones.js`. El botón (`bi-three-dots-vertical`) solo lleva `aria-label`, sin tooltip ni `title`; se cierra con clic afuera o Escape y se maneja con teclado. Las opciones que muestran información abren un `<dialog data-dialogo-informativo>` (de solo lectura) con `opcionDialogo`. La acción principal de la tarjeta va como botón ámbar a la izquierda del menú.
- Acceso a una pantalla de gestión desde el detalle (consumos, envasados): botón sólido ámbar "Gestionar ..." en la cabecera de la tarjeta, a la derecha del título, con ícono (`bi-list-check`, `bi-box-seam`); el registro vive en la pantalla de gestión.

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

## Scroll conservado

- `static/js/conservar-scroll.js` (incluido al final del body de `layout/base.html`) hace que el sidebar no vuelva al inicio al navegar: guarda su `scrollTop` en `sessionStorage` (`scroll-sidebar`) y lo restaura al cargar.
- Al cambiar de pestaña (`fragments/pestanas` y las pestañas de etapa del lote) se conserva también el scroll del contenedor `<main>` (`scroll-pestana`, solo si la ruta cargada es la del enlace pulsado). El resto de las navegaciones abre arriba.
- Todo acceso a `sessionStorage` va en `try/catch`. Protegido por `ConservarScrollTest`.

## Importes

- Todo importe en pesos que se muestra lleva el símbolo delante con un espacio y el formato numérico de `FormatoImporte` (`FormatoImporte.conSimbolo`, o `conSimbolo` en el cálculo de `orden-compra-form.js`): `$ 23.460,00`. Un importe por unidad se escribe `$ 5,05 / kg` o `$ 15,00 / L`.
- Los encabezados y etiquetas de valores mostrados no repiten la unidad ("Subtotal", no "Subtotal ($)"). En los campos de formulario el sufijo `campo-unidad` ("$", "$ por litro") se mantiene porque es una unidad de entrada.
- Protegido por `ImporteConSimboloTest`.

