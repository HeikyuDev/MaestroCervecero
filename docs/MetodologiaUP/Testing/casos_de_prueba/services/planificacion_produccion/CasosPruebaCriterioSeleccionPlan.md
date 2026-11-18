Casos de prueba de los enums `CriterioSeleccionPlanSecuencial` y `CriterioSeleccionPlanConcurrente` (no son un service: es el nombre legible que se muestra al usuario, sin dependencias externas).

### 1. `getEtiqueta()`

Cada criterio trae su nombre legible. El nombre de la constante (`FERMENTADOR_MAYOR_CAPACIDAD`) es el valor que se persiste; la etiqueta es solo para mostrar.

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-CE-01** | Etiqueta legible de cada criterio | Los dos criterios secuenciales y los dos concurrentes | `getEtiqueta()` $\rightarrow$ **Texto legible** | Devuelve "Fermentador de mayor capacidad", "Fermentador que se libera más temprano", "Menor cantidad de lotes" y "Equipos que se liberan más temprano", cada una para su criterio. |
| **CP-CE-02** | Todos los criterios tienen etiqueta | `values()` de ambos enums | Etiqueta definida $\rightarrow$ **Sí** | Hay 2 criterios de cada tipo y ninguna etiqueta está en blanco ni contiene guiones bajos. |
