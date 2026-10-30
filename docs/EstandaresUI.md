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

