Casos de prueba del enum `Rol` (no de un service: es el nombre legible que se muestra al usuario, sin dependencias externas).

### 1. `getEtiqueta()`

Cada rol trae su nombre legible. El nombre de la constante (`GERENTE_DE_PRODUCCION`) es el valor que se persiste y el que usa Spring Security; la etiqueta es solo para mostrar.

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-RO-01** | Etiqueta legible de cada rol | Los seis roles | `getEtiqueta()` $\rightarrow$ **Texto legible** | Devuelve "Administrador", "Gerente de Producción", "Operario de Producción", "Gerente de Compras", "Encargado de Depósito" y "Encargado de Distribución", cada una para su rol. |
| **CP-RO-02** | Todos los roles tienen etiqueta | `Rol.values()` | Etiqueta definida $\rightarrow$ **Sí** | Son 6 roles y ninguna etiqueta está en blanco ni contiene guiones bajos. |


### 2. `getPermisos()` del control de equipamiento

Los permisos de limpieza, falla y mantenimiento de equipamiento (`*_EQUIPAMIENTO_REGISTRAR`, `*_EQUIPAMIENTO_ANULAR` y `*_EQUIPAMIENTO_CONSULTAR`) se reparten por rol: el Operario de Producción opera el ciclo de vida del equipamiento y el Gerente de Producción solo lo consulta.

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-RO-06** | El Operario de Producción solo consulta insumos, recetas, barriles y equipamiento | `Rol.OPERARIO_DE_PRODUCCION` | `getPermisos()` $\rightarrow$ **Contiene** `INSUMO_CONSULTAR`, `RECETA_CONSULTAR`, `BARRIL_CONSULTAR` y `EQUIPAMIENTO_CONSULTAR` y **no contiene** crear, modificar ni eliminar de esos módulos | Contiene los cuatro permisos de consulta y ninguno de escritura. |
| **CP-RO-07** | El Gerente de Producción conserva la gestión | `Rol.GERENTE_DE_PRODUCCION` | `getPermisos()` $\rightarrow$ **Contiene** crear, modificar, eliminar y consultar de insumos, recetas, barriles y equipamiento | Contiene los dieciséis permisos. |
| **CP-RO-03** | El Operario de Producción opera el ciclo de vida del equipamiento | `Rol.OPERARIO_DE_PRODUCCION` | `getPermisos()` $\rightarrow$ **Contiene** los permisos de registrar, anular y consultar de limpieza, falla y mantenimiento, y `EQUIPAMIENTO_CONSULTAR` | Contiene los diez permisos indicados. |
| **CP-RO-04** | El Gerente de Producción solo consulta el ciclo de vida del equipamiento | `Rol.GERENTE_DE_PRODUCCION` | `getPermisos()` $\rightarrow$ **Contiene** los permisos de consultar de limpieza, falla y mantenimiento y los `EQUIPAMIENTO_*`; no contiene registrar ni anular | Contiene `LIMPIEZA_EQUIPAMIENTO_CONSULTAR`, `FALLA_EQUIPAMIENTO_CONSULTAR`, `MANTENIMIENTO_EQUIPAMIENTO_CONSULTAR` y los cuatro `EQUIPAMIENTO_*`. No contiene ningún permiso de registrar ni de anular limpieza, falla o mantenimiento de equipamiento. |
| **CP-RO-05** | Los demás roles no operan el ciclo de vida del equipamiento | `ADMINISTRADOR`, `GERENTE_DE_COMPRAS`, `ENCARGADO_DE_DEPOSITO` y `ENCARGADO_DE_DISTRIBUCION` | `getPermisos()` $\rightarrow$ **No contiene** ningún permiso de limpieza, falla o mantenimiento de equipamiento | Ninguno de los cuatro roles contiene permisos de registrar, anular ni consultar limpieza, falla o mantenimiento de equipamiento. |

### 3. `getPermisos()` del Encargado de Distribución

El Encargado de Distribución gestiona clientes, consulta los barriles y registra los despachos y las devoluciones de barriles. No tiene permisos de presentaciones comerciales (son del Gerente de Compras).

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-RO-08** | El Encargado de Distribución tiene los permisos de clientes, despachos y devoluciones de barril, la consulta de barriles y ningún otro | `Rol.ENCARGADO_DE_DISTRIBUCION` | `getPermisos()` $
ightarrow$ **Contiene exactamente** `CLIENTE_CREAR`, `CLIENTE_MODIFICAR`, `CLIENTE_ELIMINAR`, `CLIENTE_CONSULTAR`, `DESPACHO_BARRIL_REGISTRAR`, `DESPACHO_BARRIL_ANULAR`, `DESPACHO_BARRIL_CONSULTAR`, `DEVOLUCION_BARRIL_REGISTRAR`, `DEVOLUCION_BARRIL_ANULAR` y `DEVOLUCION_BARRIL_CONSULTAR` | El conjunto tiene esos once permisos y ningún otro. |

### 4. `getPermisos()` del control de barriles

El Operario de Producción registra, anula y consulta las limpiezas, las fallas, los fraccionamientos y los mantenimientos de barril (`LIMPIEZA_BARRIL_*`, `FALLA_BARRIL_*`, `FRACCIONAMIENTO_BARRIL_*` y `MANTENIMIENTO_BARRIL_*`) y consulta los barriles; no tiene permisos de despacho ni de devolución. El Gerente de Producción conserva la gestión de barriles (`BARRIL_*`) y solo consulta las seis operaciones (limpieza, falla, mantenimiento, fraccionamiento, despacho y devolución). El Encargado de Distribución consulta los barriles (`BARRIL_CONSULTAR`) además de sus permisos de clientes, despachos y devoluciones. Para las pantallas de Control de Barriles (`/control-barriles`) cada método se autoriza con los permisos `*_BARRIL_CONSULTAR`, `*_BARRIL_REGISTRAR` y `*_BARRIL_ANULAR` de su operación: el operario accede a limpieza, falla, fraccionamiento y mantenimiento, el encargado de distribución a despacho y devolución, el gerente de producción solo consulta las seis y es el único que ve el historial del barril; los demás roles no acceden (ver `docs/Diseno/PantallasControlBarriles.md`).

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-RO-09** | El Gerente de Producción tiene el conjunto exacto de permisos | `Rol.GERENTE_DE_PRODUCCION` | `getPermisos()` $\rightarrow$ **Contiene exactamente** los permisos de insumos, equipamiento, consulta de limpieza, falla y mantenimiento de equipamiento, recetas, planificación de producción, lotes, `BARRIL_CREAR`, `BARRIL_MODIFICAR`, `BARRIL_ELIMINAR`, `BARRIL_CONSULTAR`, los seis `*_BARRIL_CONSULTAR` de las operaciones, costos directos adicionales, costos fijos, motivos de ajuste y ajustes de stock | El conjunto tiene esos permisos y ningún otro: del ciclo de vida del barril solo consulta, no registra ni anula. |
| **CP-RO-10** | El Operario de Producción tiene el conjunto exacto de permisos | `Rol.OPERARIO_DE_PRODUCCION` | `getPermisos()` $\rightarrow$ **Contiene exactamente** consulta de insumos, recetas, equipamiento y barriles, permisos de lotes, consumos y envasados, registrar, anular y consultar limpieza, falla y mantenimiento de equipamiento, y registrar, anular y consultar limpieza, falla, fraccionamiento y mantenimiento de barril | El conjunto tiene esos permisos y ningún otro. |
| **CP-RO-11** | Producción no registra ni anula despachos ni devoluciones | `Rol.OPERARIO_DE_PRODUCCION` y `Rol.GERENTE_DE_PRODUCCION` | `getPermisos()` $\rightarrow$ **No contiene** `DESPACHO_BARRIL_*` ni `DEVOLUCION_BARRIL_*` de registrar o anular (el Operario tampoco los de consultar) | Ninguno de los dos roles contiene permisos de registrar ni de anular despachos y devoluciones de barril; el Operario no contiene ninguno de los seis. |
| **CP-RO-12** | Los demás roles no operan limpiezas, fallas, mantenimientos ni fraccionamientos de barril | `ADMINISTRADOR`, `GERENTE_DE_COMPRAS`, `ENCARGADO_DE_DEPOSITO` y `ENCARGADO_DE_DISTRIBUCION` | `getPermisos()` $\rightarrow$ **No contiene** ningún permiso de limpieza, falla, mantenimiento o fraccionamiento de barril | Ninguno de los cuatro roles contiene permisos de registrar, anular ni consultar esas cuatro operaciones. |
