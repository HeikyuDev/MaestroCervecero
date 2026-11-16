Casos de prueba de la presentación de las estadísticas: la geometría del gráfico (`GraficoBarrasTest`) y las guardas de las plantillas (`EstadisticasPlantillasTest`).

### 1. Gráfico de barras (`GraficoBarrasTest`)

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESTG-01** | Barras proporcionales | Valores 4, 2 y 1 | Barras $\rightarrow$ **Orden y largo** | Una barra por categoría en el orden recibido, con su largo proporcional al valor sobre el valor más alto. |
| **CP-ESTG-02** | Ocupa el ancho disponible | Valores 10, 3 y 7 | Largo $\rightarrow$ **Máximo 75 %** | La barra más larga ocupa el 75 % del ancho y ninguna lo supera, para dejar lugar al valor escrito. |
| **CP-ESTG-03** | Cero y mínimo | Una categoría en cero y otra muy chica | Largo $\rightarrow$ **Cero o mínimo** | La de cero no dibuja barra; la chica conserva el largo mínimo visible. |
| **CP-ESTG-04** | Negativos | Un valor negativo | Largo y texto $\rightarrow$ **Cero** | Se dibuja como cero. |
| **CP-ESTG-05** | Gráfico vacío | Sin categorías, todas en cero o sin datos | `isVacio()` $\rightarrow$ **Verdadero** | Sin barras. |
| **CP-ESTG-06** | Valores y descripción | 1.380,5 y 2 con unidad L | Textos $\rightarrow$ **Formato de la aplicación** | "1.380,5 L"; la descripción enumera cada categoría con su valor y las barras usan el ámbar de acento. |
| **CP-ESTG-07** | Etiqueta larga | Etiqueta de más de 16 caracteres | Etiqueta corta $\rightarrow$ **Abreviada** | Se abrevia con puntos suspensivos y conserva el texto completo; "En ejecución" no se abrevia. |
| **CP-ESTG-08** | Gráfico de importes | Valores con símbolo de pesos y unidad L | Textos $\rightarrow$ **Importe / unidad** | "$ 1.250,50 / L"; sin unidad, "$ 5,00". |
| **CP-ESTG-09** | Gráfico de variaciones | Variaciones +20, -10 y 0 | Barras $\rightarrow$ **Con signo y color** | "+20 %", "-10 %" y "0 %"; la negativa en azul, el largo por valor absoluto y la de cero sin barra; todo en cero queda vacío. |

### 2. Plantillas (`EstadisticasPlantillasTest`)

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-ESTT-01** | Una sola entrada en el menú | `fragments/sidebar.html` y `layout/base.html` | Menú $\rightarrow$ **Estadísticas** | Una entrada `/estadisticas` con `bi-bar-chart`, visible con cualquiera de los dos permisos, en la sección Reportes debajo de Administración, sin rutas por reporte; el título de la pestaña del navegador es "Estadísticas". |
| **CP-ESTT-02** | Plantillas genéricas | Carpeta `templates/estadisticas` | Archivos $\rightarrow$ **Cuatro** | Solo `estadisticas.html`, `fragments/reporte.html`, `fragments/grafico.html` y `pdf/reporte.html`. |
| **CP-ESTT-03** | Pestañas | Pantalla y `PestanaEstadistica` | Pestañas $\rightarrow$ **Componente estándar** | Usa `fragments/pestanas` con el ícono de cada pestaña y la línea de la pregunta; cada pestaña tiene clave, ícono `bi-`, pregunta sin punto final y un permiso de estadísticas. |
| **CP-ESTT-04** | Barra de filtros | Piezas de la pantalla | Formulario $\rightarrow$ **Grilla de las listas** | GET con `novalidate`, sin `required`, la grilla de los filtros de las listas, Desde y Hasta con asterisco rojo y placeholder, los criterios propios (como máximo dos) y, en su propia fila a la derecha, Limpiar y Filtrar, sin "Exportar PDF". |
| **CP-ESTT-05** | Estructura del reporte | Piezas de la pantalla y topes | Contenido $\rightarrow$ **Topes** | Hasta 4 indicadores en una fila con el tooltip del proyecto, el único gráfico debajo a todo el ancho, una tabla de hasta 6 columnas con Acciones centrada y protegida por permiso, totales, estado vacío y paginación estándar. El encabezado de una columna puede llevar un tooltip con su definición (en el reporte impreso, una nota debajo de los indicadores de la tabla). |
| **CP-ESTT-06** | Gráfico accesible | `fragments/grafico.html` | Gráfico $\rightarrow$ **HTML del servidor** | Una fila por barra con la etiqueta, la barra con su largo en porcentaje del ancho y el valor escrito a continuación; `role="img"` con los valores en `aria-label`, "Sin datos para graficar" y sin JavaScript ni SVG. |
| **CP-ESTT-07** | Reporte impreso | `pdf/reporte.html` | Diseño $\rightarrow$ **A4 vertical** | A4 vertical, gráfico de 170 mm, encabezado y pie repetidos, datos de emisión con la terminal, "Página X de Y", encabezado de tabla repetido, filas sin partir, sin la columna de acciones, aviso de tope, leyenda sin resultados y sin expresiones web. |
| **CP-ESTT-08** | Textos | Las cuatro plantillas | Textos $\rightarrow$ **Convenciones** | Sin emojis, sin voseo y sin punto final en los textos de estado. |
| **CP-ESTT-09** | Encabezado de resultados | Piezas y pantalla | Encabezado $\rightarrow$ **Período y PDF** | Debajo de los filtros y solo con reporte: "Resultados del dd/MM/yyyy al dd/MM/yyyy" a la izquierda y el botón secundario "Exportar PDF" (con borde, sin relleno ámbar) a la derecha; siguen indicadores, gráfico y tabla. |
| **CP-ESTT-10** | Botón de acciones | Piezas de la pantalla | Columna de acciones $\rightarrow$ **Abrir o Ver** | "Abrir" o el ojo "Ver" según la columna, protegido por su permiso y sin botón si la celda no tiene ID. |
| **CP-ESTT-11** | Pestañas definidas | `PestanaEstadistica` | Pestañas $\rightarrow$ **Nueve** | Claves, íconos y preguntas distintos; las cinco primeras con el permiso de producción y las cuatro últimas con el de compras. |
