## **Escalado de la levadura**

### **Introducción**

Esta sección describe cómo el sistema calcula la cantidad de levadura necesaria para un lote, en función del volumen a producir, la densidad inicial objetivo (OG) y el tipo de fermentación. A diferencia de la malta y el lúpulo, acá lo que se calcula primero no es un peso, sino una **cantidad de células viables**, que recién al final se traduce a gramos según la concentración celular propia de cada levadura.

Cuando la receta define más de una levadura, el sistema aplica el mismo criterio ya usado en malta y lúpulo: preserva la proporción entre las levaduras definida en la receta, calcula un valor combinado ponderado de la mezcla, y reparte el total resultante manteniendo esa proporción.

### **A. Datos de entrada**

| Dato | Descripción | Origen |
| ----- | ----- | ----- |
| OG Objetivo | Densidad inicial que se desea alcanzar en el mosto. | Definida en la versión vigente de la receta. |
| Volumen Objetivo del Lote | Litros de cerveza que se desean obtener. | Ingresado por el usuario al iniciar el lote. |
| Detalle de Levaduras de la Receta | Listado de levaduras utilizadas, con cantidad (gramos, para el volumen base de la receta). | Definido en la versión vigente de la receta. |

### **B. Parámetros de insumo**

**Levadura**

| Dato | Descripción |
| ----- | ----- |
| Tipo (ALE / LAGER / HÍBRIDA) | Determina la Tasa de Inoculación fija asociada: ALE \= 0,75 — HÍBRIDA \= 1,00 — LAGER \= 1,50 (millones de células por mililitro por grado Plato). |
| Células viables por gramo | Cantidad de células viables que contiene cada gramo de esa levadura en particular. Valor configurable por el usuario; por defecto la interfaz sugiere 20.000 millones de células por gramo. |

### **C. Constante fija del sistema**

| Constante | Valor | Descripción |
| ----- | ----- | ----- |
| Divisor de conversión a grados Plato | 4 | Convierte los Puntos de Gravedad del OG objetivo a grados Plato. |

### **Desarrollo del cálculo, paso a paso**

**Paso 1 — Conversión del OG objetivo a grados Plato**

$Grados\ Plato\ =\ ((O{G}_{obj}\ -\ 1)\ x\ 1000)\ /\ 4$

**Paso 2 — Proporción de cada levadura en la receta**

Cuando la receta define más de una levadura, se calcula qué porcentaje del total representa cada una, según las cantidades base definidas en la receta:

$Proporcio{n}_{i}\ =\ Cantida{d}_{i\ base}\ /\ Cantidad\ total\ base$

**Paso 3 — Tasa de Inoculación promedio ponderada**

Como cada levadura puede tener un tipo distinto (y por lo tanto una Tasa de Inoculación distinta), se calcula un promedio ponderado según la proporción de cada una:

$Tasa\ promedio\ =\ Σ\ (Proporcio{n}_{i}x\ Tas{a}_{i})$

**Paso 4 — Cantidad total de células necesarias**

Con el volumen del lote (convertido a mililitros), los grados Plato y la Tasa de Inoculación promedio, se calcula el total de células necesarias, expresado en millones:

$Celulas\ totales\ millones\ =\ {V}_{obj\ \ en\ ml}\ x\ Grados\ Plato\ x\ Tasa\ Promedio$

**Paso 5 — Reparto entre las levaduras de la receta**

El total se reparte entre las levaduras manteniendo la misma proporción definida en la receta:

$Celula{s}_{i\ millones}=\ Celulas\ totales\ millones\ x\ Proporcio{n}_{i}$

**Paso 6 — Conversión a gramos**

Cada levadura se convierte a gramos utilizando su propio dato de células viables por gramo (ya que puede variar de una levadura a otra):

$Gramo{s}_{i}\ =\ (Celulas_i_millones\ x\ 1.000.000)\ /\ Celulas_por_gram{o}_{i}$

### **Ejemplo numérico ilustrativo  Caso 1: una sola levadura**

*Nota: los valores de este ejemplo son ilustrativos, con el único fin de mostrar el mecanismo de cálculo — no representan datos reales del sistema.*

**Datos de la receta:**

* OG Objetivo: 1.050  
* Volumen Objetivo del lote: 20 L (20.000 ml)  
* Levadura A: tipo ALE (tasa 0,75) — Células viables por gramo: 20.000 millones

**Cálculo:**

1. Grados Plato:   
   $((1.050\ -\ 1)\ x\ 1000)\ /\ 4\ =\ 50\ /\ 4\ =\ 12,5\ °P$

2. Proporción:   
   $Proporcio{n}_{A}\ =\ 1\ (100%)$

3. Tasa promedio: 0,75 (no hay mezcla, se usa la de la única levadura)

4. Células totales:  
   $20.000\ x\ 12,5\ x\ 0,75\ =\ 187.500\ millones\ de\ células$

5. Reparto:   
   $Celula{s}_{A}\ =\ 187.500\ millones\ (100%)$

6. Conversión a gramos:   
   $(187.500\ x\ 1.000.000)\ /\ (20.000\ x\ 1.000.000)\ =\ 187.500\ /\ 20.000\ ≈\ 9,38\ g$

### **Ejemplo numérico ilustrativo  Caso 2: mezcla de dos levaduras**

*Nota: este ejemplo es puramente ilustrativo del mecanismo matemático de reparto; no representa una combinación que necesariamente se use en la práctica.*

**Datos de la receta:**

* OG Objetivo: 1.050  
* Volumen Objetivo del lote: 20 L (20.000 ml)  
* Levadura A: tipo ALE (tasa 0,75), cantidad base 6 g — Células viables por gramo: 18.000 millones  
* Levadura B: tipo HÍBRIDA (tasa 1,00), cantidad base 4 g — Células viables por gramo: 20.000 millones

**Cálculo:**

1. Grados Plato:   
   $((1.050\ -\ 1)\ x\ 1000)\ /\ 4\ =\ 12,5\ °P$

2. Proporciones (sobre el total base, 6 \+ 4 \= 10 g):   
   $Proporcio{n}_{A}\ =\ 6\ /\ 10\ =\ 0,6\ (60%)\ $  
   $Proporcio{n}_{B}\ =\ 4\ /\ 10\ =\ 0,4\ (40%)$

3. Tasa promedio ponderada:   
   $(0,6\ x\ 0,75)\ +\ (0,4\ x\ 1,00)\ =\ 0,45\ +\ 0,40\ =\ 0,85$

4. Células totales:   
   $20.000\ x\ 12,5\ x\ 0,85\ =\ 212.500\ millones\ de\ células$

5. Reparto:   
   $Celula{s}_{A}\ =\ 212.500\ x\ 0,6\ =\ 127.500\ millones\ $  
   $Celula{s}_{A}\ =\ 212.500\ x\ 0,4\ =\ 85.000\ millones$

6. Conversión a gramos:   
   $Gramo{s}_{A}\ =\ (127.500\ x\ 1.000.000)\ /\ (18.000\ x\ 1.000.000)\ =\ 127.500\ /\ 18.000\ ≈\ 7,08\ g\ $  
   $Gramo{s}_{B}\ =\ (85.000\ x\ 1.000.000)\ /\ (20.000\ x\ 1.000.000)\ =\ 85.000\ /\ 20.000\ =\ 4,25\ g$  
