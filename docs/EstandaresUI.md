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

## Otros estándares

- Columna de acciones fija a la derecha: clase `col-acciones` (protegida por `ColumnaAccionesStickyTest`).
- Pestañas: fragmento `fragments/pestanas`. Toda pestaña lleva un ícono que la acompaña (Bootstrap Icons con `fragments/pestanas :: bi(...)`, o los íconos propios del módulo); pasar `null` como ícono no está permitido. Las pestañas de etapa del lote (`lote/lote-detalle`) también llevan ícono. La regla está protegida por `PestanasConIconoTest`.
- Campo numérico con la unidad como sufijo: fragmento `fragments/campo-unidad`.
- Selector de entidad con buscador modal: fragmento `fragments/selector-entidad`.
- Ubicación de un formulario (país, provincia y localidad): tres desplegables dependientes con el fragmento `fragments/ubicacion-cascada` y `/js/ubicacion-cascada.js`; solo viaja el id de la localidad. Las opciones salen de `/ubicaciones/opciones/...` (roles `ADMINISTRADOR`, `GERENTE_COMERCIAL` y `GERENTE_DE_COMPRAS`). En listas y detalles se muestran país, provincia y localidad.
- Alertas con lista de ítems: fragmento `fragments/alerta-mensaje` con `MensajeConLista`.
