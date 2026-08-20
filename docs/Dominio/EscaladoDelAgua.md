## 1. Glosario Completo de Variables y Constantes

Para que el backend funcione, el sistema interactúa con datos de la Receta, datos del Equipamiento (ABMs) y Constantes Físicas universales.

### A. Datos de Entrada y Parámetros

- $V_{\text{obj}}$ (Volumen Objetivo del Lote): Cantidad en litros de cerveza terminada que se desea obtener en el fermentador. (Unidad: Litros).

- $M_{\text{grano}}$ (Masa de Grano/Malta): Kilos totales de malta requeridos para el lote, resultado de escalar la receta base al $V_{\text{obj}}$. (Unidad: Kilogramos).

- $R_{\text{emp}}$ (Relación de Empaste): Cantidad de litros de agua recomendados por cada kilo de grano para el macerado. Viene especificado en la receta. (Unidad: L/kg, ej: 3.0 L/kg).


### B. Parámetros de Equipamiento (Cargados en ABMs)

- $E_{\text{muerto}}$ (Espacio Muerto del Macerador): Volumen de agua que queda estancado debajo del falso fondo o tuberías del macerador y no entra en contacto con el grano. (Unidad: Litros).

- $P_{\text{trub}}$ (Pérdida por Trub / Fondo): Volumen de "barro" (lúpulos, proteínas y sedimentos) que queda atrapado en el fondo de la olla de hervor y no se puede trasvasar al fermentador. (Unidad: Litros).

- $\%_{\text{evap}}$ (Porcentaje de Evaporación): Tasa de evaporación de agua de la olla durante el hervor por hora. (Unidad: Porcentaje decimal, ej: 10% = 0.10).

- $C_{\text{util\_macerador}}$: Capacidad útil máxima del Macerador. (Unidad: Litros).

- $C_{\text{util\_olla}}$: Capacidad útil máxima de la Olla de Hervor. (Unidad: Litros).

- $C_{\text{util\_fermentador}}$: Capacidad útil máxima del Fermentador. (Unidad: Litros).


### C. Constantes Físicas del Sistema (Fijas en el Backend)

- $K_{\text{desplazamiento}} = 0.7 \text{ L/kg}$ (Constante de Desplazamiento Físico del Grano): Representa el volumen físico que ocupa 1 kg de malta seca molida cuando se sumerge en agua.

- $K_{\text{absorcion}} = 1.0 \text{ L/kg}$ (Constante de Retención / Absorción del Bagazo): Representa la cantidad de agua que el grano "chupa" como una esponja y retiene de forma permanente al finalizar la maceración.


## 2. El Paradigma: Flujo Físico vs. Algoritmo del Backend

Para entender el sistema, hay que diferenciar dos direcciones:



Plaintext

Flujo Físico en Planta (Hacia adelante):  
[ Maceración ] ──> [ Lavado (Sparge) ] ──> [ Hervor en Olla ] ──> [ Fermentador ]

Algoritmo del Backend (Hacia atrás / Reversa):  
[ Fermentador (Define V_obj) ] ──> [ Calcula Olla ] ──> [ Calcula Maceración ] ──> [ Calcula Lavado ]


- En la Planta: El operario empieza mezclando agua y malta en el macerador y va empujando el líquido hacia el fermentador.

- En el Software: Como el usuario ingresa como dato el Volumen Objetivo ($V_{\text{obj}}$) que quiere llenar en el Fermentador, el backend calcula hacia atrás las mermas que sufrirá el mosto en cada equipo para saber cuánta agua pedir al principio.


## 3. Desarrollo Matemático del Algoritmo (Paso a Paso)

### ETAPA 1: Fermentador y Olla de Hervor (Cálculo del Volumen Pre-Hervor)

El backend arranca determinando cuánto mosto dulce necesita juntar en la Olla de Hervor antes de prender el fuego.

#### 1.1. Volumen Post-Hervor Necesario

Es el volumen objetivo del fermentador más el mosto sucio (Trub) que quedará descartado en el fondo de la olla al apagar el fuego.



$$V_{\text{post-hervor}} = V_{\text{obj}} + P_{\text{trub}}$$

#### 1.2. Volumen Pre-Hervor Requerido

Como durante el hervor se evapora agua en forma de vapor, se necesita juntar un volumen mayor antes de hervir:



$$V_{\text{pre-hervor}} = \frac{V_{\text{post-hervor}}}{1 - \%_{\text{evap}}}$$

PUNTO DE CONTROL 1 (Validación Olla de Hervor):



$$V_{\text{pre-hervor}} \le C_{\text{util\_olla}}$$

Si $V_{\text{pre-hervor}}$ supera la capacidad útil de la olla, el sistema detiene el proceso e informa que la olla seleccionada quedará chica.

### ETAPA 2: Macerador (Cálculo del Agua de Maceración y Volumen Ocupado)

Ahora el backend calcula la cantidad de agua tibia que se necesita para arrancar el empaste en el Macerador.

#### 2.1. Agua Inicial de Maceración



$$V_{\text{agua\_mac}} = (M_{\text{grano}} \times R_{\text{emp}}) + E_{\text{muerto}}$$

- Explicación: Se multiplica la masa de grano por la relación de empaste de la receta y se le suma el Espacio Muerto porque esa agua queda retenida abajo del falso fondo y no moja directamente la malta.


#### 2.2. Volumen Total de la Mezcla en el Macerador (Agua + Malta)

Para asegurar que la "sopa" no rebalse el recipiente, se calcula el espacio real ocupado sumando el volumen físico que desplaza el grano ($K_{\text{desplazamiento}} = 0.7$):



$$V_{\text{mezcla\_mac}} = V_{\text{agua\_mac}} + (M_{\text{grano}} \times 0.7)$$

PUNTO DE CONTROL 2 (Validación Macerador):



$$V_{\text{mezcla\_mac}} \le C_{\text{util\_macerador}}$$

Si el volumen de la mezcla supera la capacidad útil del macerador, el sistema arroja error de desborde.

### ETAPA 3: Extracción, Pérdida por Bagazo y Agua de Lavado (Sparge)

Una vez terminada la maceración, el mosto se trasvasa a la Olla de Hervor.

#### 3.1. Agua Retenida en Bagazo (Pérdida)

El grano húmedo retiene $1.0 \text{ L}$ de agua por cada kilo de malta ($K_{\text{absorcion}} = 1.0$):



$$P_{\text{bagazo}} = M_{\text{grano}} \times 1.0$$

#### 3.2. Mosto Extraído Efectivo de Maceración

Es el líquido útil que sale del macerador hacia la olla de hervor en el primer flujo:



$$V_{\text{mosto\_extraido}} = V_{\text{agua\_mac}} - E_{\text{muerto}} - P_{\text{bagazo}}$$

#### 3.3. Agua de Lavado Requerida (Sparge)

Como $V_{\text{mosto\_extraido}}$ es menor que el $V_{\text{pre-hervor}}$ que la olla necesita, el sistema calcula cuánta agua limpia caliente hay que rociar sobre el grano para "enjuagarlo" y completar la olla:



$$V_{\text{agua\_lavado}} = V_{\text{pre-hervor}} - V_{\text{mosto\_extraido}}$$

### ETAPA 4: Balance Final de Agua

El backend calcula el total de agua que la planta debe preparar (filtrar y calentar) para todo el proceso:



$$V_{\text{agua\_total}} = V_{\text{agua\_mac}} + V_{\text{agua\_lavado}}$$

## 4. Matriz Resumen de Validaciones del Sistema

El caso de uso Registrar Lote ejecuta estas 3 validaciones en orden estricto antes de guardar el registro:

|   |   |   |
|---|---|---|
|Equipo|Condición de Validación|Mensaje de Error en Pantalla|
|Fermentador|$V_{\text{obj}} \le C_{\text{util\_fermentador}}$|"El volumen objetivo excede la capacidad útil del fermentador."|
|Olla de Hervor|$V_{\text{pre-hervor}} \le C_{\text{util\_olla}}$|"El volumen pre-hervor ($V_{\text{pre-hervor}}\text{L}$) supera la capacidad útil de la olla."|
|Macerador|$V_{\text{mezcla\_mac}} \le C_{\text{util\_macerador}}$|"La mezcla de agua y grano ($V_{\text{mezcla\_mac}}\text{L}$) desbordará el macerador."|

## 5. Ejemplo Práctico Numérico Desarrollado

Imaginemos una corrida real en el sistema con estos parámetros:

### Parámetros de Entrada:

- Volumen Objetivo ($V_{\text{obj}}$): $500 \text{ L}$

- Masa de Grano ($M_{\text{grano}}$): $100 \text{ kg}$

- Relación de Empaste ($R_{\text{emp}}$): $3.0 \text{ L/kg}$

- Espacio Muerto ($E_{\text{muerto}}$): $10 \text{ L}$

- Pérdida por Trub ($P_{\text{trub}}$): $10 \text{ L}$

- Evaporación ($\%_{\text{evap}}$): $10\%$ ($0.10$)


### Ejecución del Algoritmo del Backend:

1. Cálculo en Olla de Hervor:  
   $$V_{\text{post-hervor}} = 500 + 10 = 510 \text{ L}$$  
   $$V_{\text{pre-hervor}} = \frac{510}{1 - 0.10} = \frac{510}{0.90} = \mathbf{566.66 \text{ Litros}}$$  
   (Se valida que la Olla de Hervor tenga al menos $566.66 \text{ L}$ de capacidad útil).

2. Cálculo en Macerador:  
   $$V_{\text{agua\_mac}} = (100 \times 3.0) + 10 = \mathbf{310 \text{ Litros}}$$  
   $$V_{\text{mezcla\_mac}} = 310 + (100 \times 0.7) = 310 + 70 = \mathbf{380 \text{ Litros}}$$  
   (Se valida que el Macerador tenga al menos $380 \text{ L}$ de capacidad útil).

3. Cálculo de Pérdidas y Lavado (Sparge):  
   $$P_{\text{bagazo}} = 100 \times 1.0 = 100 \text{ L}$$  
   $$V_{\text{mosto\_extraido}} = 310 - 10 - 100 = 200 \text{ L}$$  
   $$V_{\text{agua\_lavado}} = 566.66 - 200 = \mathbf{366.66 \text{ Litros}}$$

4. Resultado de Agua Total requerida:  
   $$V_{\text{agua\_total}} = 310 + 366.66 = \mathbf{676.66 \text{ Litros}}$$


### Resumen Visual de la Operación

Plaintext

====================================================================  
RESUMEN DE PLANIFICACIÓN DE AGUA PARA EL LOTE  
====================================================================  
• Agua para Maceración:    310.00 Litros  
• Agua para Lavado:        366.66 Litros
--------------------------------------------------------------------  
  TOTAL AGUA A PREPARAR:   676.66 Litros
--------------------------------------------------------------------  
• Espacio Requerido en Macerador:   380.00 Litros (Agua + Malta)  
• Volumen a Hervir en Olla:         566.66 Litros  
• Volumen Final en Fermentador:     500.00 Litros  
====================================================================


**