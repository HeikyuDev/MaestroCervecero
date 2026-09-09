## **Escalado del Agua**

### **Introducción**

Esta sección describe la lógica que el sistema utiliza para calcular automáticamente la cantidad de agua necesaria para producir un lote de cerveza. El objetivo es que, a partir de unos pocos datos ingresados por el usuario y de la configuración técnica de los equipos, el sistema determine con precisión cuánta agua debe prepararse en cada etapa del proceso, evitando tanto el desperdicio de insumos como el riesgo de que el líquido no entre en los recipientes disponibles.

La lógica parte de una particularidad del proceso: en la planta, el agua se va agregando "hacia adelante" (primero se macera, después se lava el grano, después se hierve, y recién al final se llega al volumen deseado en el fermentador). El sistema, en cambio, calcula "hacia atrás": el usuario indica cuánta cerveza quiere obtener al final, y el sistema deduce, paso por paso, cuánta agua hay que preparar desde el principio para llegar exactamente a ese resultado, descontando las pérdidas propias de cada etapa.

### **A. Datos de entrada**

| Dato | Descripción | Origen |
| :---- | :---- | :---- |
| Volumen Objetivo del Lote | Cantidad de litros de cerveza que se desea obtener en el fermentador. | Ingresado por el usuario al iniciar el lote. |
| Masa de Malta Escalada | Kilos totales de malta que se van a utilizar, ya calculados en función del volumen objetivo del lote. | Resultado del  escalado de malta. |
| Relación de Empaste | Litros de agua recomendados por cada kilo de malta utilizada. | Definida en la versión vigente de la receta. |
| Duración estimada del hervor | Tiempo que va a durar el hervido del mosto, expresado en minutos. | Definida en la versión vigente de la receta. |

### **B. Parámetros de equipamiento**

**Macerador**

| Dato | Descripción |
| :---- | :---- |
| Capacidad útil | Volumen máximo que el macerador puede contener sin riesgo de desborde. |
| Espacio muerto | Volumen de agua que queda retenido debajo del falso fondo del macerador, sin entrar en contacto directo con la malta. |

**Olla de Hervor**

| Dato | Descripción |
| :---- | :---- |
| Capacidad útil | Volumen máximo que la olla puede contener sin riesgo de desborde. |
| Evaporación | Cantidad de litros que la olla evapora por cada hora de hervor. |
| Pérdida por trub | Volumen de sedimentos (restos de lúpulo y proteínas) que queda en el fondo de la olla al finalizar el hervor y no puede trasvasarse. |

**Fermentador**

| Dato | Descripción |
| :---- | :---- |
| Capacidad útil | Volumen máximo que el fermentador puede contener. Define el límite superior del volumen objetivo del lote. |

### **C. Constantes fijas del sistema**

| Constante | Valor | Descripción |
| :---- | :---- | :---- |
| Desplazamiento del grano | 0.7 L/kg | Volumen físico que ocupa un kilo de malta molida al sumergirse en agua. |
| Absorción del grano | 1.0 L/kg | Cantidad de agua que un kilo de malta retiene de forma permanente durante la maceración. |

### **Desarrollo del cálculo, paso a paso**

**Paso 1 — Verificación del fermentador** Antes de cualquier cálculo, el sistema verifica que el volumen objetivo del lote no supere la capacidad útil del fermentador seleccionado. Si el volumen solicitado es mayor, el sistema no permite continuar: no tiene sentido calcular nada más si el fermentador elegido no puede contener el lote.

**Paso 2 — Volumen a hervir** El sistema calcula cuánto líquido debe reunirse en la olla antes de empezar a hervir. Parte del volumen que se quiere obtener al final y le suma dos pérdidas que van a ocurrir durante el hervido:

* El volumen que se va a evaporar, calculado multiplicando la evaporación por hora de la olla por la duración del hervor definida en la receta (convertida de minutos a horas).  
* El volumen que va a quedar atrapado como sedimento (trub) en el fondo de la olla.

Con este resultado, el sistema valida que la olla de hervor elegida tenga **capacidad suficiente para contener ese volumen**. Si no la tiene, se le informa al usuario que la olla seleccionada quedará chica para ese lote.

**Paso 3 — Agua para la maceración** El sistema calcula cuánta agua hace falta al principio, en el macerador. Esto surge de combinar la masa de malta a utilizar con la relación de empaste definida en la receta, sumando además el agua que va a quedar retenida en el espacio muerto del macerador.

A este resultado se le suma el volumen físico que ocupa la malta al mojarse (usando la constante de desplazamiento del grano), para obtener el volumen total que realmente va a ocupar la mezcla de agua y malta dentro del macerador. El sistema valida que el macerador elegido tenga capacidad suficiente para esa mezcla; si no la tiene, informa que el macerador seleccionado desbordará.

**Paso 4 — Agua de lavado (sparge)** Una parte del agua que se cargó en el macerador queda retenida en el grano y no se puede aprovechar (se calcula multiplicando la masa de malta por la constante de absorción del grano). El sistema descuenta esa pérdida, junto con el agua ya retenida en el espacio muerto, para saber cuánto líquido útil sale realmente del macerador hacia la olla.

Como ese líquido extraído casi nunca alcanza el volumen que la olla necesita (calculado en el Paso 2), el sistema calcula la diferencia: esa diferencia es el agua de lavado, que se agrega rociando agua caliente adicional sobre el grano para completar el volumen necesario.

**Paso 5 — Agua total a preparar** Finalmente, el sistema suma el agua utilizada en la maceración (Paso 3\) más el agua de lavado (Paso 4), obteniendo el total de agua que la planta debe preparar y calentar para ejecutar el lote completo.

## 

## 

### **Matriz de validaciones**

| Equipo | Se valida que... | Mensaje si falla |
| :---- | :---- | :---- |
| Fermentador | El volumen objetivo del lote no supere la capacidad útil del fermentador. | Volumen físico que ocupa un kilo de malta molida al sumergirse en agua. |
| Olla de Hervor | El volumen a hervir (Paso 2\) no supere la capacidad útil de la olla | "El volumen pre-hervor supera la capacidad útil de la olla." |
| Macerador | El volumen de la mezcla de agua y malta (Paso 3\) no supere la capacidad útil del macerador | "La mezcla de agua y grano desbordará el macerador." |

### **Glosario de variables** 

| Símbolo | Significado | Unidad |
| :---- | :---- | :---- |
| ${V}_{obj}$ | Volumen Objetivo del Lote | Litros |
| ${M}_{malta}$ | Masa de Malta Escalada | Kilogramos |
| ${R}_{emp}$ | Relación de Empaste (de la receta) | L/kg |
| ${t}_{hervor}$ | Duración estimada del hervor (de la receta) | Minutos |
| ${E}_{muerto}$ | Espacio Muerto (del macerador) | Litros |
| ${E}_{evap}$ | Evaporación por hora (de la olla de hervor) | L/hora |
| ${P}_{trub}$ | Pérdida por Trub (de la olla de hervor) | Litros |
| ${C}_{uti{l}_{}mac}$ | Capacidad útil del Macerador | Litros |
| ${C}_{uti{l}_{}olla}$ | Capacidad útil de la Olla de Hervor | Litros |
| ${C}_{uti{l}_{}ferm}$ | Capacidad útil del Fermentador | Litros |
| ${K}_{desp}$ | Constante de Desplazamiento del Grano | 0.7 L/kg (fija) |
| ${K}_{abs}$ | Constante de Absorción del Grano | 1.0 L/kg (fija) |

## 

### **Desarrollo del cálculo, paso a paso**

**Paso 1 — Verificación del fermentador** Antes de cualquier cálculo, el sistema verifica que el volumen objetivo del lote no supere la capacidad útil del fermentador seleccionado.

${V}_{obj}\leq {C}_{uti{l}_{}ferm}$

**Paso 2 — Volumen a hervir** El sistema calcula cuánto líquido debe reunirse en la olla antes de empezar a hervir: parte del volumen final deseado y le suma lo que se va a perder por evaporación durante el hervido y lo que va a quedar atrapado como sedimento (trub) en el fondo de la olla.

${V}_{pre-hervor}={V}_{obj}+{P}_{trub}+({E}_{evap}\times {(t}_{hervor})/60)$

Validación:

${V}_{pre-hervor}\leq {C}_{uti{l}_{}olla}$

**Paso 3 — Agua para la maceración** El agua necesaria para arrancar la maceración surge de combinar la masa de malta con la relación de empaste de la receta, sumando el agua retenida en el espacio muerto del macerador:

${V}_{agu{a}_{}mac}=({M}_{malta}\times {R}_{emp})+{E}_{muerto}$

A esto se le suma el volumen físico que desplaza la malta al mojarse, para obtener el volumen real que va a ocupar la mezcla dentro del macerador:

${V}_{mezcl\ mac}={V}_{agu{a}_{}mac}+({M}_{malta}\times {K}_{desp})$

Validación:

${V}_{mezcl{a}_{}mac}\leq {C}_{uti{l}_{}mac}$

**Paso 4 — Agua de lavado (sparge)** Parte del agua cargada en el macerador queda retenida en el grano y no se recupera:

${P}_{bagazo}={M}_{malta}\times {K}_{abs}$

Con esto, se calcula el mosto que efectivamente sale del macerador hacia la olla:

${V}_{most{o}_{}extraído}={V}_{agu{a}_{}mac}-{E}_{muerto}-{P}_{bagazo}$

Y la diferencia entre lo que la olla necesita (Paso 2\) y lo que efectivamente llegó, es el agua de lavado a agregar:

${V}_{agu{a}_{}lavado}={V}_{pre-hervor}-{V}_{most{o}_{}extraido}$

**Paso 5 — Agua total a preparar**

${V}_{agu{a}_{}total}={V}_{agu{a}_{}mac}+{V}_{agu{a}_{}lavado}$