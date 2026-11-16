Casos de prueba de las estadísticas de producción (`EstadisticaProduccionServicioImpl`), del período de las estadísticas (`PeriodoEstadistica`) y de la estructura única de los reportes (`ReporteEstadisticoResponseDTO` y sus piezas).

### 1. `generarReporteDeLotes` (`EstadisticaProduccionServicioImplTest`)

Reporte "Lotes": cuántos lotes se planificaron en el período y cómo terminaron. El universo son los lotes cuya fecha de inicio estimada cae en el período, en cualquier estado; la receta y el estado son criterios opcionales.

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESP-01** | Período inválido | Sin desde; sin hasta; hasta anterior a desde; 367 días | Validación del período $\rightarrow$ **Rechazo** | `ReglaNegocioException` con el mensaje del período en cada caso y ninguna consulta al repositorio. |
| **CP-ESP-02** | Extremos del período | Un solo día con receta 7 y estado Finalizado; 366 días sin criterios | Consultas $\rightarrow$ **Mismos criterios** | Se aceptan los dos períodos y las cuatro consultas reciben el período, la receta y el estado tal cual. |
| **CP-ESP-03** | Los cuatro indicadores | 8 lotes (3 finalizados), 1.380,555 L envasados, 3 finalizados de los que 2 terminaron no después del fin estimado | Indicadores $\rightarrow$ **Valores formateados** | "Lotes del período" 8, "Finalizados" 3 con "37,5 % de los lotes", "Litros envasados" 1.380,56 L y "Terminados en plazo" 66,67 % con "2 de 3 finalizados" y su definición en la ayuda. El reporte trae título, pregunta y período. |
| **CP-ESP-04** | Sin lotes | El período no tiene lotes ni envasados | Divisiones por cero $\rightarrow$ **No se calculan** | Indicadores 0, 0, 0 L y "-"; la aclaración de finalizados no se muestra y la de en plazo dice "Sin lotes finalizados". |
| **CP-ESP-05** | Sin finalizados con fechas | Lotes pendientes y un finalizado sin fecha estimada; luego un único finalizado fuera de plazo | Terminados en plazo $\rightarrow$ **Guion o singular** | Primero "-" con "Sin lotes finalizados"; luego "0 %" con "0 de 1 finalizado". |
| **CP-ESP-06** | Criterios aplicados | Sin criterios; con receta dada de baja y estado Cancelado | Criterios $\rightarrow$ **Receta y Estado** | "Receta: Todas" y "Estado: Todos"; con criterios, "Pale Ale (dada de baja)" y "Cancelado". |
| **CP-ESP-07** | Gráfico por estado | 9 lotes cancelados | Gráfico $\rightarrow$ **Cuatro estados** | "Lotes por estado" con Pendiente 0, En ejecución 0, Finalizado 0 y Cancelado 9, en ese orden. |
| **CP-ESP-08** | Tabla | Segunda página de un lote finalizado con 480,5 L | Columnas y fila $\rightarrow$ **Seis columnas** | Lote, Estado, Litros envasados, Fin estimado, Fin real y Acciones (ruta `/lotes/{id}`, permiso `LOTE_CONSULTAR`); la fila trae el código con la receta debajo, "Finalizado", "480,5", las fechas en dd/MM/yyyy y "Abrir" con el ID; la página conserva su número y el total. |
| **CP-ESP-09** | Lote sin envasados y totales | Un lote pendiente sin envasados registrados ni fin real | Fila y totales $\rightarrow$ **Cero y guion** | Litros "0", fin real "-" y la fila de totales "Totales del período" con los litros del período. |
| **CP-ESP-10** | Página sin filas | El período no tiene lotes | Consulta por lote $\rightarrow$ **No se ejecuta** | La tabla no tiene filas ni totales y no se consultan los litros por lote. |

### 1.1 Reportes "Costos", "Tiempos", "Insumos" y "Equipamiento" (`EstadisticaProduccionReportesServicioImplTest`)

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESP-11** | Período inválido en los cuatro reportes | Sin desde, sin hasta, invertido y de 367 días | Validación $\rightarrow$ **Rechazo** | `ReglaNegocioException` con el mensaje del período y ninguna consulta al repositorio. |
| **CP-ESP-12** | Costos: indicadores y tabla | Dos lotes envasados (100 L y 50 L) con costos de insumos de 150 y 50 y un costo adicional de 50 | Costos $\rightarrow$ **Suma por lote** | Costo total $ 250, 150 L, costo por litro $ 1,6667 y peso de los insumos 80 %; la tabla del fin de envasado más reciente al más antiguo con 6 columnas y la fila de totales con el costo por litro ponderado. |
| **CP-ESP-13** | Costos sin lotes | Ningún lote en el período | Divisiones por cero $\rightarrow$ **No se calculan** | Costo $ 0, 0 L y guiones en el costo por litro y en el peso de los insumos; sin totales ni gráfico. |
| **CP-ESP-14** | Costos: gráfico | Diez recetas | Gráfico $\rightarrow$ **Ocho, sin "Otras"** | Las ocho recetas de mayor costo por litro, en orden, sin categoría que sume el resto. |
| **CP-ESP-15** | Tiempos: estimaciones e indicadores | Etapas de Fermentación (12 días sobre 10), Maceración (45 min sobre 60), Hervido sin duración en la receta, Molienda (100 kg a 50 kg/h, 150 min) y Envasado (200 L a 100 L/h, 120 min) | Tiempos $\rightarrow$ **Real contra estimada** | 5 etapas (1 sin estimar), 50 % dentro de lo estimado (2 de 4), desvío promedio +5 % y Molienda como etapa de mayor desvío; el gráfico en promedio por etapa y la tabla con "Sin estimar" y el desvío en "-". |
| **CP-ESP-16** | Tiempos sin etapas estimables | Una Molienda sin malta consumida ni escalado, con el rendimiento vigente del molino | Porcentajes $\rightarrow$ **Guion** | 1 etapa y guiones; "Sin etapas estimadas" y gráfico vacío. |
| **CP-ESP-17** | Tiempos sin etapas | Ninguna etapa finalizada | Reporte $\rightarrow$ **Ceros y guiones** | 0 etapas y guiones, sin filas. |
| **CP-ESP-18** | Insumos: ranking | Pilsen (malta, $ 600) y Cascade (lúpulo, $ 400 de los que $ 100 son consumo directo) | Insumos $\rightarrow$ **Ranking por valor** | Valor $ 1.000, 2 insumos, mayor valor Pilsen y 10 % sin reserva; Pilsen primero con "Malta", "120 kg", 60 % y 2 lotes de producción; totales con 100 %. La pregunta es "Qué insumos se consumieron y cuánto se gastó en ellos", la columna se llama "Lotes de producción" y sus tooltips (y el del indicador "Valor consumido") explican el valor (cantidad por PPP registrado en cada consumo) y los lotes de producción en que se consumió el insumo. |
| **CP-ESP-19** | Insumos: vacío y tipo elegido | Tipo Malta y receta 7 sin consumos | Insumos $\rightarrow$ **Ceros y guiones** | Consulta por la clase de malta; $ 0, 0 y guiones, sin totales. |
| **CP-ESP-20** | Insumos: gráfico | Diez insumos | Gráfico $\rightarrow$ **Ocho y "Otros"** | Los ocho de mayor valor y "Otros" con la suma de los dos restantes. |
| **CP-ESP-21** | Equipamiento: los cuatro tipos | Un molino, un macerador, una olla, dos fermentadores y dos molinos dados de baja (uno con actividad); etapas que empiezan antes del período, en curso y de un equipo dado de baja | Actividad $\rightarrow$ **Recortada al período** | Equipos de los cuatro tipos por tipo e identificador; el dado de baja sin actividad no figura y el otro dice "(dado de baja)"; horas en uso recortadas (12 h, 38 h hasta el fin del período y 6 h); limpiezas, fallas y mantenimientos; indicadores, gráfico de horas por equipo y totales. |
| **CP-ESP-22** | Equipamiento: filtros y vacío | Tipo Fermentador y equipo 4 sin equipos | Reporte $\rightarrow$ **Ceros** | Consulta por la clase y el equipo; ceros, sin totales ni gráfico y criterios "Fermentador" y "FER-01". |
| **CP-ESP-23** | Tiempos: etapa que empezó antes del período | Una Maceración iniciada el 31/10 a las 22:00 que terminó el 1/11 a las 02:00 (4 h sobre 2 h estimadas) | Universo $\rightarrow$ **Finalizadas con fin en el período** | La etapa figura con "4 h", "2 h" y "+100 %": el universo son las etapas finalizadas cuya fecha de finalización cae en el período, aunque hayan empezado antes. |
| **CP-ESP-24** | Tiempos: promedio sin redondear antes | Dos Fermentaciones de 14 días (20.160 min): una de 20.162 min (+0,00992 %, se muestra +0,01 %) y otra de 20.160 min (0 %) | Indicadores $\rightarrow$ **Mismos valores sin redondear** | 2 etapas, "50 %" dentro de lo estimado, desvío promedio "0 %" (no "+0,01 %") y "0 % en promedio" en la etapa de mayor desvío; la celda de la primera etapa sí muestra "+0,01 %". |
| **CP-ESP-25** | Tiempos: empate en la etapa de mayor desvío | Un Hervido y una Maceración, ambos con +50 % | Etapa con mayor desvío $\rightarrow$ **Primera del proceso** | "Maceración" con "+50 % en promedio": ante un empate gana la primera en el orden del proceso. |
| **CP-ESP-26** | Equipamiento: lote cancelado con la etapa en marcha | Una etapa de un fermentador iniciada el 1/1 a las 06:00 y finalizada por la cancelación del lote el 2/1 a las 12:00 | Uso real $\rightarrow$ **Hasta la cancelación** | La fila del equipo muestra 1 etapa y 30 horas en uso, y el indicador "Etapas atendidas" dice 1 con "30 h en uso". |

### 1.2 Consultas del universo (`ConsultasEstadisticaProduccionTest`)

Fijan, sin base de datos, el criterio que escribe cada consulta de producción que define una regla de los reportes.

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESP-27** | Equipamiento: etapas con uso real | Consulta de las etapas del período | Estados $\rightarrow$ **Finalizadas y canceladas iniciadas** | Incluye las etapas en curso y las `FINALIZADA` o `CANCELADA` con fecha de finalización desde el inicio del período, exige fecha de inicio y que empiece antes del fin del período. |
| **CP-ESP-28** | Tiempos: etapas finalizadas | Consulta de las etapas finalizadas | Período $\rightarrow$ **Por fecha de finalización** | Solo `FINALIZADA`, con la fecha de finalización dentro del período y sin exigir que la fecha de inicio esté en él. |

### 2. `PeriodoEstadistica` (`PeriodoEstadisticaTest`)

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESTP-01** | Período válido | Dos extremos en orden; un solo día | `validar` $\rightarrow$ **Aceptado** | Devuelve el período con los mismos extremos. |
| **CP-ESTP-02** | Sin fecha desde | Desde nula | `validar` $\rightarrow$ **Rechazo** | "La fecha desde es obligatoria". |
| **CP-ESTP-03** | Sin fecha hasta | Hasta nula | `validar` $\rightarrow$ **Rechazo** | "La fecha hasta es obligatoria". |
| **CP-ESTP-04** | Período invertido | Hasta anterior a desde | `validar` $\rightarrow$ **Rechazo** | "La fecha hasta no puede ser anterior a la fecha desde". |
| **CP-ESTP-05** | 366 días | Un año bisiesto completo | `validar` $\rightarrow$ **Aceptado** | El período se acepta. |
| **CP-ESTP-06** | 367 días | Un día más que el máximo | `validar` $\rightarrow$ **Rechazo** | "El período no puede superar los 366 días". |
| **CP-ESTP-07** | Período por defecto | Una fecha actual | `porDefecto` $\rightarrow$ **Mes en curso** | Del primer día del mes a hoy. |

### 3. Estructura única del reporte (`ReporteEstadisticoResponseDTOTest`)

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ERG-01** | Tope de indicadores | Reportes con 4 y con 5 indicadores | Construcción $\rightarrow$ **Máximo 4** | Con 4 se arma; con 5, `IllegalArgumentException` "Un reporte admite como máximo 4 indicadores". |
| **CP-ERG-02** | Tope de columnas y celdas | Tabla de 7 columnas; fila o totales con menos celdas que columnas | Construcción $\rightarrow$ **Rechazo** | "La tabla de un reporte admite como máximo 6 columnas" y "Cada fila de la tabla debe tener una celda por columna". |
| **CP-ERG-03** | Totales solo con filas | Tabla con filas y tabla vacía, ambas con totales | `getTotales()` $\rightarrow$ **Según filas** | Con filas devuelve la fila de totales; sin filas, ninguna. |
| **CP-ERG-04** | Columnas imprimibles | Tabla con Lote, Litros y Acciones | Reporte impreso $\rightarrow$ **Sin acciones** | Se imprimen Lote y Litros con sus celdas en orden; cada tipo tiene su alineación (texto y fecha a la izquierda, número, porcentaje e importe a la derecha, acciones al centro). |
| **CP-ERG-05** | Tabla truncada | Página de 2 filas con total 2 y con total 5 | `isTruncada()` $\rightarrow$ **Según total** | Falso con total 2, verdadero con total 5; el plural de las filas es "lotes". |
| **CP-ERG-06** | Formato de celdas | Número, porcentaje, importe, fechas, texto vacío, texto con detalle y enlace | Fábricas de celda $\rightarrow$ **Formato de la aplicación** | "1.380,5", "66,67 %", "$ 1.250,50", "02/11/2026", "22/11/2026", "-" sin dato; el detalle y el ID del enlace se conservan. |
| **CP-ERG-07** | Criterio sin valor | Receta nula, estado en blanco, estado indicado | `CriterioAplicadoResponseDTO.de` $\rightarrow$ **Sin filtro** | "Todas", "Todos" y "Finalizado". |
