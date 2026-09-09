## **Escalado del lúpulo**

### **Introducción**

Esta sección describe cómo el sistema calcula la cantidad de lúpulo necesaria para una receta, según el volumen del lote a producir. A diferencia de la malta, el lúpulo tiene dos comportamientos distintos según su uso: los lúpulos utilizados durante el hervor aportan amargor (IBU) y se calculan con la fórmula de Tinseth; los lúpulos utilizados en whirlpool o dry hop tienen fines exclusivamente aromáticos, no aportan amargor de forma significativa, y se escalan de manera simple, en proporción directa al volumen del lote.

### **A. Datos de entrada**

| Dato | Descripción | Origen |
| ----- | ----- | ----- |
| IBU Objetivo | Amargor que se desea alcanzar en la receta. | Definido en la versión vigente de la receta. |
| OG Objetivo | Densidad inicial objetivo (afecta la utilización del lúpulo). | Definido en la versión vigente de la receta. |
| Volumen Objetivo del Lote | Litros de cerveza que se desean obtener. | Ingresado por el usuario al iniciar el lote. |
| Volumen Base de la Receta | Litros para los que fue pensada originalmente la receta. | Definido en la versión vigente de la receta. |
| Detalle de Lúpulos de la Receta | Listado de lúpulos utilizados, con cantidad (gramos, para el volumen base), uso, tiempo de hervor (si aplica) y AA% del lúpulo. | Definido en la versión vigente de la receta. |

### **B. Parámetros de insumo**

**Lúpulo**

| Dato | Descripción |
| ----- | ----- |
| AA% (Alfa Ácidos) | Porcentaje de ácidos alfa de ese lúpulo en particular. |
| Formato (Pellet / Flor) | Determina un factor de corrección sobre la utilización: Pellet incrementa la utilización un 10%; Flor no aplica corrección. |

### **C. Constantes fijas del sistema (fórmula de Tinseth)**

| Constante | Valor | Descripción |
| ----- | ----- | ----- |
| Constante de Factor de Densidad | 1.65 | Constante base del Factor de Densidad. |
| Base del Factor de Densidad | 0.000125 | Base elevada a (OG objetivo − 1\) en el cálculo del Factor de Densidad. |
| Constante de decaimiento del Factor de Tiempo | 0.04 | Exponente que regula cómo crece la utilización con el tiempo de hervor. |
| Divisor del Factor de Tiempo | 4.15 | Valor de ajuste del Factor de Tiempo, según el modelo de Tinseth. |

### **Desarrollo del cálculo, paso a paso**

### **Parte 1 — Lúpulos de uso HERVOR (aportan amargor)**

**Paso 1 — Factor de Densidad (común a todos los lúpulos de HERVOR del lote)**

Este factor depende únicamente del OG objetivo de la receta, por lo que es el mismo para todos los lúpulos de HERVOR de un mismo lote:

$FactorDensidad\ =\ 1.65\ ×\ 0.000125\ ^\ (O{G}_{obj}\ -\ 1)$

**Paso 2 — Factor de Tiempo y Utilización individual de cada lúpulo**

Cada lúpulo de HERVOR tiene su propio tiempo de hervor, por lo tanto su propio Factor de Tiempo:

$FactorTiemp{o}_{i}\ =\ (1\ -\ e^(-0.04\ ×\ {t}_{i}))\ /\ 4.15$

Con este factor, más el Factor de Densidad del Paso 1 y la corrección según el formato del lúpulo (Pellet o Flor), se obtiene la **Utilización individual** de cada uno:

$Utilizacio{n}_{i}\ =\ FactorDensidad\ ×\ FactorTiemp{o}_{i}\ ×\ FactorCorreccio{n}_{format{o}_{i}}$

**Paso 3 — Proporción de cada lúpulo de HERVOR en la receta**

Igual que con la malta, se calcula qué porcentaje del total de lúpulo de HERVOR representa cada uno, según las cantidades base de la receta:

$Proporcio{n}_{i}\ =\ Cantida{d}_{i\ base}\ /\ Cantidad\ total\ base\ hervor$

**Paso 4 — Constante ponderada de la mezcla**

Como cada lúpulo aporta una cantidad distinta de IBU por gramo (según su AA% y su propia Utilización), se calcula una constante que resume el aporte conjunto de la mezcla, ponderada por la proporción de cada lúpulo:

$K\ =\ Σ\ (\ Proporcio{n}_{i}\ ×\ (A{A}_{i}\ /\ 100)\ ×\ Utilizacio{n}_{i}\ )$

**Paso 5 — Cantidad total de lúpulo de HERVOR necesaria**

Con el IBU objetivo de la receta, el volumen del lote y la constante K calculada, se obtiene el total de gramos de lúpulo de HERVOR necesarios:

$Gramos\ total\ hervor\ =\ \frac{(IB{U}_{obj}\ ×\ {V}_{obj})}{(1000\ ×\ K)}$

**Paso 6 — Reparto entre los lúpulos de HERVOR**

El total se reparte manteniendo la misma proporción definida en la receta:

$Gramos\ =\ Gramos\ total\ hervor\ ×\ Proporcio{n}_{i}$

### **Parte 2 — Lúpulos de uso WHIRLPOOL y DRY HOP (aportan aroma)**

**Paso 7 — Escalado lineal**

Para estos lúpulos, al no aportar amargor de forma significativa, el sistema no aplica Tinseth: simplemente escala la cantidad original de la receta en la misma proporción en que cambia el volumen del lote respecto al volumen base:

$Gramo{s}_{i}\ =\ Cantida{d}_{i\ base}\ ×\ ({V}_{obj}\ /\ {V}_{base})$

### **Ejemplo numérico ilustrativo**

*Nota: los valores de este ejemplo son ilustrativos, con el único fin de mostrar el mecanismo de cálculo — no representan datos reales del sistema.*

**Datos de la receta:**

* OG Objetivo: 1.050  
* IBU Objetivo: 35  
* Volumen Base de la receta: 20 L  
* Volumen Objetivo del lote: 100 L

**Lúpulos de HERVOR:**

* Lúpulo A (Cascade): AA 5.5%, Pellet, tiempo de hervor 60 min, cantidad base 30 g  
* Lúpulo B (Centennial): AA 10%, Pellet, tiempo de hervor 15 min, cantidad base 10 g

**Lúpulo de DRY HOP:**

* Lúpulo C (Citra): cantidad base 15 g

**Cálculo:**

1. Factor de Densidad (común):   
   $1.65\ ×\ 0.000125\ ^\ 0.050\ ≈\ 1.65\ ×\ 0.6382\ ≈\ 1.053$

2. Factor de Tiempo y Utilización Lúpulo A (60 min):   
   $FactorTiemp{o}_{A}\ =\ (1\ -\ e^(-0.04×60))\ /\ 4.15\ ≈\ (1\ -\ 0.0907)\ /\ 4.15\ ≈\ 0.219\ $  
   $Utilizacio{n}_{A}\ =\ 1.053\ ×\ 0.219\ ×\ 1.10\ ≈\ 0.254$

3. Factor de Tiempo y Utilización — Lúpulo B (15 min):   
   $FactorTiemp{o}_{B}\ =\ (1\ -\ e^(-0.04×15))\ /\ 4.15\ ≈\ (1\ -\ 0.5488)\ /\ 4.15\ ≈\ 0.109$$Utilizacio{n}_{B}\ =\ 1.053\ ×\ 0.109\ ×\ 1.10\ ≈\ 0.126$

4. Proporciones (sobre el total de HERVOR, 30 \+ 10 \= 40 g):   
   $Proporcion_A\ =\ 30\ /\ 40\ =\ 0.75\ (75%)\ $  
   $Proporcion_B\ =\ 10\ /\ 40\ =\ 0.25\ (25%)$

5. Constante ponderada K:   
   $K\ =\ (0.75\ ×\ 0.055\ ×\ 0.254)\ +\ (0.25\ ×\ 0.10\ ×\ 0.126)\ ≈\ 0.01048\ +\ 0.00315\ ≈\ 0.01363$

6. Gramos totales de HERVOR:   
   $(35\ ×\ 100)\ /\ (1000\ ×\ 0.01363)\ ≈\ 3500\ /\ 13.63\ ≈\ 256.8\ g$

7. Reparto:   
   $Gramo{s}_{A}\ =\ 256.8\ ×\ 0.75\ ≈\ 192.6\ g\ $  
   $Gramo{s}_{B}\ =\ 256.8\ ×\ 0.25\ ≈\ 64.2\ g$

8. Lúpulo C (DRY HOP, escalado lineal):   
   $15\ ×\ (100\ /\ 20)\ =\ 15\ ×\ 5\ =\ 75\ g$