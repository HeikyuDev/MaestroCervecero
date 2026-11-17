Casos de prueba del Controller de la pantalla "Estadísticas" (`EstadisticasControllerTest`, con la pestaña "Lotes" de `ReporteLotes`) y de sus piezas de apoyo (`ApoyoReporteTest` y `FiltroPeriodoTest`).

### 1. Permisos y pestañas

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESTC-01** | Roles sin permisos de estadísticas | Administrador, Operario de Producción, Encargado de Depósito y Encargado de Distribución | GET `/estadisticas` y `/estadisticas/pdf` $\rightarrow$ **Rechazo** | Redirección con la alerta "No tiene permisos para realizar esta acción"; no se consulta el Service ni se genera el PDF. |
| **CP-ESTC-02** | Sin sesión | Sin usuario | GET pantalla y PDF $\rightarrow$ **Login** | Redirección al inicio de sesión. |
| **CP-ESTC-03** | Gerente de Compras sin pestañas de producción | Gerente de Compras | GET pantalla y PDF $\rightarrow$ **Sin reportes** | La pantalla dice "No hay reportes disponibles para su usuario" sin la pestaña Lotes; el PDF vuelve a `/estadisticas`. |
| **CP-ESTC-04** | Pestaña y período por defecto | Gerente de Producción sin parámetros | GET `/estadisticas` $\rightarrow$ **Lotes, mes en curso** | Vista `estadisticas/estadisticas`, pestaña Lotes activa con su ícono, título "Estadísticas - Maestro Cervecero", la pregunta de la pestaña y el período del primer día del mes a hoy enviado al Service sin receta ni estado. |
| **CP-ESTC-11** | Pestaña inexistente | `pestana=costos` | Pestaña activa $\rightarrow$ **Primera permitida** | Se muestra Lotes. |
| **CP-ESTC-24** | Tabla sin filas | Pestaña Lotes sin lotes en el período | Pantalla $\rightarrow$ **Sin totales** | Dice "Sin resultados para los criterios indicados" y no muestra la fila de totales. |
| **CP-ESTC-26** | Varias tablas en la pantalla | Pestaña Costos con dos recetas (dos tablas con título) | Pantalla $\rightarrow$ **Una tabla debajo de la otra** | Dos tablas, cada una con su título (la primera receta antes que la segunda), sin pie de paginación, y la fecha considerada "Lotes con envasado finalizado dentro del período". |
| **CP-ESTC-27** | Indicadores según su cantidad | Pestaña Costos (3 indicadores) y pestaña Lotes (4) | Pantalla $\rightarrow$ **Sin hueco en la fila** | Costos usa la grilla de 3 columnas con tarjetas amplias (`data-cantidad-indicadores="3"`) y Lotes la de 4 columnas. |
| **CP-ESTC-25** | Pestaña Tiempos deshabilitada | Gerente de Producción con `pestana=tiempos` (pantalla y PDF) y la propiedad `estadisticas.tiempos.habilitada` sin definir | Pestaña $
ightarrow$ **No se alcanza** | La barra de pestañas no incluye Tiempos, la pestaña activa y el PDF son los de Lotes y no se consulta el reporte de tiempos. |

### 2. Pantalla

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESTC-05** | Estructura del reporte | Período de noviembre y estado Finalizado, 3 lotes | HTML $\rightarrow$ **Estructura única** | Encabezado "Resultados del 01/11/2026 al 30/11/2026", 4 indicadores (con el tooltip de "Terminados en plazo"), un solo gráfico con los valores escritos, una tabla de 6 columnas con Acciones centrada y 3 botones "Abrir", fila de totales, paginación "3 lotes · Página 1 de 1", "Exportar PDF" con los mismos criterios, "Limpiar" a la pestaña sin criterios y el estado elegido seleccionado. Debajo del período se lee la fecha considerada ("Lotes con inicio estimado dentro del período"). |
| **CP-ESTC-06** | Sin LOTE_CONSULTAR | Usuario con solo `ESTADISTICA_PRODUCCION_CONSULTAR` | Columna Acciones $\rightarrow$ **Sin botón** | Las filas se muestran sin "Abrir". |
| **CP-ESTC-07** | Paginación | Página 9 de un período con 60 lotes | Página $\rightarrow$ **Última** | Se vuelve a pedir la página 3 y se ve "Página 3 de 3" con el enlace a la anterior que conserva pestaña y período. |
| **CP-ESTC-08** | Período rechazado | El Service rechaza el período de más de 366 días | Pantalla $\rightarrow$ **Alerta y sin reporte** | Alerta amarilla con el mensaje del Service, criterios conservados, la pregunta de la pestaña y la tarjeta que pide un período válido, sin indicadores ni "Exportar PDF". |
| **CP-ESTC-09** | Fechas inexistentes y vacías | Desde 30/02/2026; luego Desde y Hasta vacíos | Interpretación $\rightarrow$ **Error o Service** | "La fecha desde no es válida" sin llamar al Service; con los dos vacíos se envían nulos y se muestra "La fecha desde es obligatoria". |
| **CP-ESTC-10** | Criterios propios | Receta 7 dada de baja y estado en minúsculas; luego receta que el buscador no ofrece y estado inexistente | Criterios $\rightarrow$ **Filtran o se ignoran** | Primero filtran por la receta 7 ("Pale Ale (dada de baja)") y Cancelado, con el buscador de recetas de los filtros de lotes; después no filtran y se avisa el estado inválido. |

### 3. Reporte impreso

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESTC-12** | PDF con sus datos | Pestaña Lotes, noviembre de 2026 | GET `/estadisticas/pdf` $\rightarrow$ **PDF en línea** | `application/pdf` con `inline; filename="estadisticas-lotes_2026-11-01_2026-11-30.pdf"`, plantilla `estadisticas/pdf/reporte`, primera página con el tope técnico de filas el emisor "GERENTE_DE_PRODUCCION (Ana Pérez)" y la terminal de la petición. |
| **CP-ESTC-13** | PDF con criterios inválidos | Fecha inexistente; período invertido rechazado por el Service | GET PDF $\rightarrow$ **Vuelve a la pantalla** | Redirección a `/estadisticas` con los mismos parámetros; no se genera el PDF. |
| **CP-ESTC-21** | PDF que falla al generarse | El generador de PDF lanza una excepción | GET PDF $\rightarrow$ **Vuelve a la pantalla con alerta** | Redirección a `/estadisticas` con los mismos parámetros, alerta amarilla "No se pudo generar el reporte impreso" y el lugar de generación liberado. |
| **CP-ESTC-22** | Sin lugar para generar | Ya hay reportes en generación y no se libera un lugar a tiempo | GET PDF $\rightarrow$ **Vuelve a la pantalla con alerta** | Redirección con los mismos parámetros y la alerta amarilla "Hay otros reportes en generación, intente nuevamente en unos instantes"; no se genera nada ni se libera un lugar que no se tomó. |
| **CP-ESTC-23** | Con lugar para generar | Un lugar disponible | GET PDF $\rightarrow$ **PDF y lugar liberado** | Responde 200 con el PDF y el lugar queda disponible al terminar. |

### 3.1 Pestañas de producción y de compras (`EstadisticasPestanasControllerTest`)

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESTC-14** | Pestañas del gerente de producción | Gerente de Producción, con la pestaña Tiempos habilitada | Pestañas $\rightarrow$ **Cinco de producción** | Lotes, Costos, Tiempos, Insumos y Equipamiento con su ícono y ninguna de compras. |
| **CP-ESTC-15** | Pestañas del gerente de compras | Gerente de Compras | Pestañas $\rightarrow$ **Cuatro de compras** | Compras, Proveedores, Precios y Recepción, la primera activa por defecto y ninguna de producción; no se consulta el Service de producción. |
| **CP-ESTC-16** | Pestaña de otra gerencia | `pestana=costos` del gerente de compras; `pestana=precios` del de producción; PDF de una pestaña ajena | Pestaña $\rightarrow$ **La primera propia** | Cada gerente cae en la primera de las suyas, también en el PDF, y no se consulta el reporte ajeno. |
| **CP-ESTC-17** | Cada pestaña: pantalla y PDF | Las nueve pestañas, con Tiempos habilitada con el período de noviembre | Pantalla y PDF $\rightarrow$ **Estructura única** | Cada una muestra su pregunta, "Resultados del 01/11/2026 al 30/11/2026", "Exportar PDF", solo sus criterios propios (como máximo dos) y como máximo 4 indicadores; su PDF es `application/pdf` con el nombre `estadisticas-<clave>_2026-11-01_2026-11-30.pdf` y la plantilla única. |
| **CP-ESTC-18** | Criterios de producción | Con Tiempos habilitada: etapa en minúsculas y receta; tipo de insumo; tipo de equipo y equipo; etapa inexistente | Criterios $\rightarrow$ **Interpretados** | Llegan al Service como valores del enum y entidades con su texto; una etapa inexistente no filtra y se avisa "La etapa indicada no es válida, se muestran todas". |
| **CP-ESTC-19** | Criterios de compras | Proveedor y estado; tipo de insumo con un proveedor que ya no existe; tipo de insumo; tipo de ingreso | Criterios $\rightarrow$ **Interpretados** | Llegan al Service interpretados; un proveedor que ya no existe no filtra. |
| **CP-ESTC-20** | Buscador de equipos | Gerente de Compras | GET `/estadisticas/buscador/equipos` $\rightarrow$ **Rechazo** | Redirección con la alerta de permisos sin consultar equipos. |

### 4. Apoyo (`ApoyoReporteTest`)

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESTA-01** | Volver a la pantalla | Parámetros con valores, vacíos y nulos | `volverALaPantalla` $\rightarrow$ **Redirección** | Redirige a `/estadisticas` con los parámetros no vacíos, en orden y codificados. |
| **CP-ESTA-02** | Respuesta del PDF | Contenido y período | `respuestaPdf` $\rightarrow$ **En línea** | `application/pdf` con el nombre `estadisticas-lotes_2026-11-01_2026-11-30.pdf`. |
| **CP-ESTA-03** | Emisor | Usuario existente y usuario inexistente | `emisor` $\rightarrow$ **Texto** | "ana (Ana Pérez)" y "nadie". |
| **CP-ESTA-04** | Mensajes y enum | Un error, solo avisos, nada; textos de enum | `mostrarMensajes` y `aEnum` $\rightarrow$ **Alerta y valor** | Alerta amarilla con el error o los avisos; `aEnum` no distingue mayúsculas y devuelve nulo si el texto está vacío o no existe. |

### 5. Período pedido (`FiltroPeriodoTest`)

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESTF-01** | Sin parámetros | Ni desde ni hasta | `resolver` $\rightarrow$ **Por defecto** | Del primer día del mes a hoy. |
| **CP-ESTF-02** | Fechas ISO | Desde y hasta válidas | `resolver` $\rightarrow$ **Indicado** | El período indicado sin errores. |
| **CP-ESTF-03** | Un extremo vacío | Uno indicado y el otro vacío o ausente | `resolver` $\rightarrow$ **Sin valor** | El extremo vacío queda nulo y sin error: lo rechaza el Service. |
| **CP-ESTF-04** | Fecha inexistente | Texto que no es una fecha | `resolver` $\rightarrow$ **Error** | "La fecha desde no es válida" o "La fecha hasta no es válida" y el extremo queda nulo. |

### 6. Terminal del reporte impreso (`TerminalClienteTest`)

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESTR-01** | Dirección remota | Petición desde 192.168.0.25 | Dirección $\rightarrow$ **Remota** | 192.168.0.25. |
| **CP-ESTR-02** | Encabezado enviado por el cliente | Petición desde 10.0.0.1 con `X-Forwarded-For` " 192.168.0.30 , 10.0.0.1" | Dirección $\rightarrow$ **Ignora el encabezado** | 10.0.0.1: se usa siempre la dirección de la conexión. |
| **CP-ESTR-03** | Loopback | 127.0.0.1, 0:0:0:0:0:0:0:1 y ::1 | Terminal $\rightarrow$ **El servidor** | "servidor-cerveceria (192.168.0.10)" sin resolver por DNS; si el servidor no se conoce, la IP. |
| **CP-ESTR-04** | Nombre de equipo | Nombre resuelto, nulo, vacío o igual a la IP; IP nula | Terminal $\rightarrow$ **Nombre (IP) o IP** | "pc-gerencia (192.168.0.25)"; si no hay nombre, solo la IP; sin IP, "-". |
| **CP-ESTR-05** | Solo direcciones IP | IPv4 e IPv6 válidas; nombres de equipo, octetos fuera de rango, zona de interfaz, texto cualquiera, vacío y nulo | `esDireccionLiteral` $\rightarrow$ **Solo IP literal** | Verdadero únicamente para las direcciones IP; un nombre de equipo nunca llega al DNS y su resolución devuelve nulo. |
| **CP-ESTR-06** | Resolución acotada | Una resolución que tarda 5 segundos con un máximo de 100 ms | Resolución $\rightarrow$ **Tiempo máximo** | Devuelve nulo dentro del tiempo, la terminal es solo la IP y la tarea se cancela (se interrumpe). |
| **CP-ESTR-07** | Caché de nombres | Una IP con nombre y otra sin nombre, consultadas dos veces; luego pasa el tiempo de vigencia | Caché $\rightarrow$ **Evita repetir** | Cada dirección se resuelve una sola vez (también la que no tiene nombre); vencida la vigencia, se vuelve a resolver. |
| **CP-ESTR-08** | Tamaño máximo de la caché | Tres direcciones en una caché de dos | Caché $\rightarrow$ **Descarta la más antigua** | La caché conserva dos direcciones y la primera se vuelve a resolver. |
