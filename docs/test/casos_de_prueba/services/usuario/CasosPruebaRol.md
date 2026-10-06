Casos de prueba del enum `Rol` (no de un service: es el nombre legible que se muestra al usuario, sin dependencias externas).

### 1. `getEtiqueta()`

Cada rol trae su nombre legible. El nombre de la constante (`GERENTE_DE_PRODUCCION`) es el valor que se persiste y el que usa Spring Security; la etiqueta es solo para mostrar.

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-RO-01** | Etiqueta legible de cada rol | Los seis roles | `getEtiqueta()` $\rightarrow$ **Texto legible** | Devuelve "Administrador", "Gerente de Producción", "Operario de Producción", "Gerente de Compras", "Encargado de Depósito" y "Gerente Comercial", cada una para su rol. |
| **CP-RO-02** | Todos los roles tienen etiqueta | `Rol.values()` | Etiqueta definida $\rightarrow$ **Sí** | Son 6 roles y ninguna etiqueta está en blanco ni contiene guiones bajos. |
