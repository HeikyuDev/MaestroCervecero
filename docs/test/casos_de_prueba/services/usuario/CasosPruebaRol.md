Casos de prueba del enum `Rol` (no de un service: es el nombre legible que se muestra al usuario, sin dependencias externas).

### 1. `getEtiqueta()`

Cada rol trae su nombre legible. El nombre de la constante (`GERENTE_DE_PRODUCCION`) es el valor que se persiste y el que usa Spring Security; la etiqueta es solo para mostrar.

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-RO-01** | Etiqueta legible de cada rol | Los seis roles | `getEtiqueta()` $\rightarrow$ **Texto legible** | Devuelve "Administrador", "Gerente de Producción", "Operario de Producción", "Gerente de Compras", "Encargado de Depósito" y "Gerente Comercial", cada una para su rol. |
| **CP-RO-02** | Todos los roles tienen etiqueta | `Rol.values()` | Etiqueta definida $\rightarrow$ **Sí** | Son 6 roles y ninguna etiqueta está en blanco ni contiene guiones bajos. |


### 2. `getPermisos()` del control de equipamiento

Los permisos de limpieza, falla y mantenimiento de equipamiento (`*_EQUIPAMIENTO_REGISTRAR`, `*_EQUIPAMIENTO_ANULAR` y `*_EQUIPAMIENTO_CONSULTAR`) se reparten por rol: el Operario de Producción opera el ciclo de vida del equipamiento y el Gerente de Producción solo lo consulta.

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-RO-06** | El Operario de Producción solo consulta insumos, recetas, barriles y equipamiento | `Rol.OPERARIO_DE_PRODUCCION` | `getPermisos()` $\rightarrow$ **Contiene** `INSUMO_CONSULTAR`, `RECETA_CONSULTAR`, `BARRIL_CONSULTAR` y `EQUIPAMIENTO_CONSULTAR` y **no contiene** crear, modificar ni eliminar de esos módulos | Contiene los cuatro permisos de consulta y ninguno de escritura. |
| **CP-RO-07** | El Gerente de Producción conserva la gestión | `Rol.GERENTE_DE_PRODUCCION` | `getPermisos()` $\rightarrow$ **Contiene** crear, modificar, eliminar y consultar de insumos, recetas, barriles y equipamiento | Contiene los dieciséis permisos. |
| **CP-RO-03** | El Operario de Producción opera el ciclo de vida del equipamiento | `Rol.OPERARIO_DE_PRODUCCION` | `getPermisos()` $\rightarrow$ **Contiene** los permisos de registrar, anular y consultar de limpieza, falla y mantenimiento, y `EQUIPAMIENTO_CONSULTAR` | Contiene los diez permisos indicados. |
| **CP-RO-04** | El Gerente de Producción solo consulta el ciclo de vida del equipamiento | `Rol.GERENTE_DE_PRODUCCION` | `getPermisos()` $\rightarrow$ **Contiene** los permisos de consultar de limpieza, falla y mantenimiento y los `EQUIPAMIENTO_*`; no contiene registrar ni anular | Contiene `LIMPIEZA_EQUIPAMIENTO_CONSULTAR`, `FALLA_EQUIPAMIENTO_CONSULTAR`, `MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR` y los cuatro `EQUIPAMIENTO_*`. No contiene ningún permiso de registrar ni de anular limpieza, falla o mantenimiento de equipamiento. |
| **CP-RO-05** | Los demás roles no operan el ciclo de vida del equipamiento | `ADMINISTRADOR`, `GERENTE_DE_COMPRAS`, `ENCARGADO_DE_DEPOSITO` y `GERENTE_COMERCIAL` | `getPermisos()` $\rightarrow$ **No contiene** ningún permiso de limpieza, falla o mantenimiento de equipamiento | Ninguno de los cuatro roles contiene permisos de registrar, anular ni consultar limpieza, falla o mantenimiento de equipamiento. |
