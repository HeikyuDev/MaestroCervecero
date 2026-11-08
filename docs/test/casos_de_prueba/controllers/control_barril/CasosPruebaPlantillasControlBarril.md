## Casos de prueba de las plantillas del control de barriles

**Tipo de prueba:** guardas de plantilla (lectura de los archivos HTML). Clase `ControlBarrilPlantillasTest`. Además de estas guardas, las plantillas nuevas quedan cubiertas por las guardas generales que recorren todas las plantillas: `BotonesDeFiltrosALaDerechaTest`, `BotonesDeFilaEnAccionesTest`, `ColumnaAccionesStickyTest`, `PestanasConIconoTest`, `BuscadoresModalTest` y `OrdenDeCamposTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-PCB-01** | Bloque de información | Separa en filas la identidad, las medidas con el estado y el mantenimiento. |
| **CP-PCB-02** | Formulario por tema | Cantidad, cliente (con el menú de alta contextual de cliente), fechas y observaciones cada uno en su fila, con asterisco y ejemplo; novalidate; la ayuda de la devolución usa el estilo de las ayudas. |
| **CP-PCB-03** | Lista con Abrir y ojo | Usa el fragmento del botón Abrir, el ojo es solo del gerente con aria-label sin tooltip y ningún dato de la fila es un enlace. |
| **CP-PCB-04** | Lista de siete columnas | Identificador, Fabricante, Capacidad, Contenido, Estado, Usos y Acciones; Limpiar y Buscar a la derecha; fabricante como selector de entidad. |
| **CP-PCB-05** | Pestañas con ícono | Usan el fragmento de pestañas con el ícono de cada tipo. |
| **CP-PCB-06** | Barra lateral | Control de Barriles en Operación, con ícono propio, para quien consulta operaciones de barril; la sección Operación la ve el encargado de distribución. |
| **CP-PCB-07** | Historial | Patrón de las listas: filtros con Limpiar y Buscar a la derecha, tabla estándar, badge del barril y paginación. |
| **CP-PCB-08** | Sin accesos cruzados ni personas | Ninguna pantalla lleva Abrir, el ojo del historial, Responsable ni el usuario que anuló; solo el detalle muestra Registrado por. |
| **CP-PCB-09** | Consistencia con las pantallas de referencia | Encabezados, tarjeta con botón + Nueva, aviso de solo consulta, formulario con Cancelar y Registrar. |
| **CP-PCB-10** | Observaciones solo en el diálogo | El botón i del fragmento compartido, dentro de la celda Acciones. |
| **CP-PCB-11** | Orden de los botones de fila | i, i de la anulación, ojo y anular al final. |
| **CP-PCB-12** | Buscador de clientes | Tabla de buscador, botón Elegir sin alta al vuelo y paginación del modal. |
