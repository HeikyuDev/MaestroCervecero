## **Escalado de Malta**

### **Introducción**

Esta sección describe la lógica que el sistema utiliza para calcular automáticamente la cantidad de cada malta necesaria para alcanzar la densidad inicial (OG) objetivo de una receta, para el volumen de lote que se desee producir. A diferencia del escalado de agua —que depende de las dimensiones físicas de los equipos—, el escalado de malta depende de tres factores: 

1. Cuánta densidad se quiere lograr   
2. Qué tan eficiente es el macerador utilizado para extraer los azúcares del grano  
3. Qué tan "rica" en azúcares es cada malta en particular.

### **Aclaración de terminología:** 

Antes de presentar la fórmula, es importante distinguir dos datos del sistema que, si no se diferencian con claridad, pueden confundirse por tratar temas cercanos:

* **Extracto Potencial** (definido en el maestro de Malta): es una propiedad de cada malta en particular. Representa, en términos simples, qué porcentaje del peso de esa malta son azúcares aprovechables en condiciones ideales. Por ejemplo, una malta con 80% de Extracto Potencial significa que, en el mejor de los casos, de cada kilo de esa malta se podrían obtener 0,8 kilos de azúcares.

* **Rendimiento de Maceración** (definido en el equipamiento Macerador): es una propiedad del proceso, ligada al equipo utilizado. Representa qué porcentaje de esos azúcares potenciales se logran extraer realmente durante la maceración, dado el diseño y la eficiencia de ese macerador específico. Ningún proceso real logra extraer el 100% del extracto potencial de la malta; siempre queda una porción sin aprovechar.

Estos dos valores actúan en conjunto: el Extracto Potencial dice "cuánta azúcar tiene la malta en teoría", y el Rendimiento de Maceración dice "cuánto de esa azúcar se logra extraer realmente con este equipo". Ambos son necesarios para calcular con precisión cuánta malta hay que utilizar.

### **A. Datos de entrada**

| Dato | Descripción | Origen |
| :---- | :---- | :---- |
| OG Objetivo | Densidad inicial que se desea alcanzar en el mosto. | Definida en la versión vigente de la receta. |
| Volumen Objetivo del Lote | Cantidad de litros de cerveza que se desea obtener. | Ingresado por el usuario al iniciar el lote. |
| Detalle de Maltas de la Receta | Listado de maltas utilizadas en la receta, junto con la cantidad (en kg) definida para el volumen base de la misma. | Definido en la versión vigente de la receta. |

### **B. Parámetros de equipamiento**

**Macerador**

| Dato | Descripción |
| ----- | ----- |
| Rendimiento de Maceración | Porcentaje de los azúcares potenciales de la malta que este macerador logra extraer realmente durante el proceso. |

### **C. Parámetros de insumo**

**Malta**

| Dato | Descripción |
| ----- | ----- |
| Extracto Potencial | Porcentaje del peso de esta malta en particular que corresponde a azúcares aprovechables, en condiciones ideales. |

### **D. Constante fija del sistema**

| Constante | Valor | Descripción |
| ----- | ----- | ----- |
| Referencia de azúcar pura | 384 puntos/kg/L | Puntos de densidad que aportaría un kilo de azúcar pura disuelto en un litro de agua. Es el valor de referencia contra el cual se mide el Extracto Potencial de cualquier malta (100% de extracto equivale a esta constante). |

### **Desarrollo del cálculo, paso a paso**

**Paso 1 — Puntos de Densidad objetivo**

El sistema traduce el OG objetivo de la receta a una magnitud que se pueda combinar directamente con el volumen del lote:

$P{D}_{obj}\ =\ (O{G}_{obj}\ -\ 1)\ \times \ 1000\ \times \ {V}_{obj}$

**Paso 2 — Proporción de cada malta en la receta**

Cuando la receta define más de una malta, el sistema calcula qué porcentaje del total de grano representa cada una, en base a las cantidades definidas para el volumen base de la receta: 

$Proporcio{n}_{i}\ =\ \frac{Cantida{d}_{{i\ base}_{}}}{Cantida{d}_{total\ base}}$

**Paso 3 — Extracto Potencial promedio de la mezcla**

Como cada malta aporta una cantidad distinta de azúcar por kilo, el sistema calcula un promedio del Extracto Potencial de todas las maltas de la receta, ponderado según la proporción de cada una:

$E{P}_{promedio\ }=\ \sum\limits_{}^{}(Proporcion\ \times \ E{P}_{i})$

**Paso 4 — Cantidad total de malta necesaria**

Con los Puntos de Densidad objetivo, el Extracto Potencial promedio de la mezcla y el Rendimiento de Maceración del equipo seleccionado, el sistema calcula cuántos kilos de malta, en total, hacen falta para el lote:

$K{G}_{malta\ total}=\ \frac{P{D}_{obj}}{E{P}_{promedio}\times Ren{d}_{mac}\times 384}$

**Paso 5 — Reparto entre las maltas de la receta**

Finalmente, el total calculado se reparte entre las distintas maltas manteniendo exactamente la misma proporción definida originalmente en la receta:

$K{g}_{malta\ i\ }=\ K{g}_{malta\ total}\ \times \ Proporcio{n}_{i}$

De esta forma, el sistema garantiza que la relación entre las distintas maltas (por ejemplo, Pilsen y Caramelo) se mantenga siempre igual a la definida por el usuario en la receta, sin importar el volumen del lote ni la eficiencia del macerador utilizado — lo único que varía es la cantidad total, nunca la proporción entre maltas.

---

### **Ejemplo numérico ilustrativo**

*Nota: los valores de este ejemplo son ilustrativos, con el único fin de mostrar el mecanismo de cálculo — no representan datos reales del sistema.*

**Datos de la receta:**

* OG Objetivo: 1.050  
* Malta Pilsen: 100 kg (base) — Extracto Potencial: 80%  
* Malta Caramelo: 20 kg (base) — Extracto Potencial: 74%

**Datos del lote:**

* Volumen Objetivo: 20 L

**Datos del macerador seleccionado:**

* Rendimiento de Maceración: 75%

**Cálculo:**

1. Puntos de Densidad objetivo:  
   1. $$$(1.050\ -\ 1)\ ×\ 1000\ ×\ 20\ =\ 1000\ PD$  
2. Proporciones:  
   1. $Pilsen\ =\ 100\ /\ 120\ =\ 0.833\ (83.3%)$  
   2. $Caramelo\ =\ 20\ /\ 120\ =\ 0.167\ (16.7%)$  
3. Extracto Potencial promedio:  
   1. $(0.833\ ×\ 80%)\ +\ (0.167\ ×\ 74%)\ =\ 66.64%\ +\ 12.36%\ =\ 79.0%$  
4. Malta total:  
   1. $\ 1000\ /\ (0.79\ ×\ 0.75\ ×\ 384)\ =\ 1000\ /\ 227.52\ ≈\ 4.40\ kg$  
5. Reparto:  
   1. $Kg_Pilsen\ =\ 4.40\ ×\ 0.833\ ≈\ 3.66\ kg$  
   2. $\ Kg_Caramelo\ =\ 4.40\ ×\ 0.167\ ≈\ 0.74\ kg$