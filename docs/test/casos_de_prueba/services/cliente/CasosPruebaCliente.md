### 1. `filtrarClientes(String nombre, String direccion, Long idLocalidad, Pageable pageable)`

`nombre` y `direccion` son coincidencia parcial, sin distinguir mayúsculas/minúsculas; `idLocalidad` es coincidencia exacta. Todos son opcionales, `null` = no filtra por ese criterio.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FC-01**|Filtra por los 3 criterios informados|`nombre: "Juan"`, `direccion: "Falsa"`, `idLocalidad: 1L`, `pageable: PageRequest.of(0, 10)`, BD con 1 cliente activo que cumple los tres criterios|`filtrarClientes("Juan", "Falsa", 1L, pageable)` contiene elementos|Retorna `Page<ClienteResponseDTO>` con 1 elemento mapeado.|
|**CP-FC-02**|Los 3 parámetros nulos no restringen la búsqueda|`nombre: null`, `direccion: null`, `idLocalidad: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 clientes activos|El service propaga los 3 parámetros nulos tal cual al repositorio|Retorna `Page<ClienteResponseDTO>` con los 3 clientes activos (equivalente a no filtrar).|
|**CP-FC-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `direccion: null`, `idLocalidad: null`, `pageable: PageRequest.of(0, 10)`|`filtrarClientes("Inexistente", null, null, pageable)` está vacío|Retorna `Page<ClienteResponseDTO>` vacía (`getContent().isEmpty() == true`).|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Cliente encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `ClienteResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Cliente inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el cliente con ID: 99".|

### 3. `altaCliente(ClienteFormDTO clienteFormDTO)`

La unicidad de correo electrónico y teléfono se valida por separado (una consulta y un mensaje de excepción por campo), no con una única consulta combinada — así el usuario sabe exactamente cuál de los dos campos colisionó.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AC-01**|Correo electrónico duplicado|`email: "juan@mail.com"` (Existe en BD), `telefono: "1122334455"` (Único), `idLocalidad: 1L`|`existsByEmailIgnoreCase("juan@mail.com")` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un cliente registrado con el correo electrónico 'juan@mail.com'"). No consulta `existsByTelefono` ni la localidad, ni ejecuta `save()`.|
|**CP-AC-02**|Teléfono duplicado|`email: "nuevo@mail.com"` (Único), `telefono: "1122334455"` (Existe en BD), `idLocalidad: 1L`|`existsByEmailIgnoreCase` $\rightarrow$ **FALSE**, `existsByTelefono("1122334455")` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un cliente registrado con el teléfono '1122334455'"). No consulta la localidad ni ejecuta `save()`.|
|**CP-AC-03**|Correo electrónico duplicado Case-Insensitive|`email: "JUAN@mail.com"` (Existe `"juan@mail.com"`), `telefono: "1122334455"` (Único)|`existsByEmailIgnoreCase("JUAN@mail.com")` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AC-04**|Localidad inexistente|`email: "nuevo@mail.com"` (Único), `telefono: "1122334455"` (Único), `idLocalidad: 99L` (No existe)|Ambos `existsBy...` $\rightarrow$ **FALSE**, `localidadRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-AC-05**|Precedencia de la validación de duplicado|`email: "juan@mail.com"` (Existe en BD), `telefono: "1122334455"` (Único), `idLocalidad: 99L` (tampoco existe)|`existsByEmailIgnoreCase` se evalúa antes que la localidad|Lanza `RecursoDuplicadoException` (no `RecursoNoEncontradoException`). No consulta `localidadRepository`.|
|**CP-AC-06**|Alta exitosa _(Camino feliz)_|`nombre: "Juan Pérez"`, `telefono: "1122334455"` (Único), `email: "juan@mail.com"` (Único), `direccion: "Calle Falsa 123"`, `idLocalidad: 1L` (Existe)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `nombre`, `telefono`, `email`, `direccion`, `localidad` y `estado = ACTIVO` asignados, y retorna DTO.|

### 4. `modificarCliente(Long id, ClienteFormDTO clienteFormDTO)`

Misma separación por campo que en el alta, cada una excluyendo el propio ID.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MC-01**|Cliente no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida duplicación ni localidad, ni ejecuta `save()`.|
|**CP-MC-02**|Correo electrónico en uso por otro cliente|`id: 1L` (Existe), `email: "otro@mail.com"` (Pertenece al `id: 2L`), `idLocalidad: 1L`|`existsByEmailIgnoreCaseAndIdNot("otro@mail.com", 1L)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe otro cliente registrado con el correo electrónico 'otro@mail.com'"). No consulta `existsByTelefonoAndIdNot` ni la localidad, ni ejecuta `save()`.|
|**CP-MC-03**|Teléfono en uso por otro cliente|`id: 1L` (Existe), `telefono: "1199998888"` (Pertenece a otro cliente), `email` propio|`existsByEmailIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**, `existsByTelefonoAndIdNot("1199998888", 1L)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe otro cliente registrado con el teléfono '1199998888'"). No consulta la localidad ni ejecuta `save()`.|
|**CP-MC-04**|Conservar el propio correo electrónico y teléfono actual|`id: 1L`, `email: "JUAN@mail.com"` (Mismo cliente, distinto case), `telefono: "1122334455"` (Propio), `idLocalidad: 1L`|Ambos `existsBy...AndIdNot` $\rightarrow$ **FALSE**|Permite actualizar, persiste y retorna DTO.|
|**CP-MC-05**|Localidad inexistente|`id: 1L` (Existe), email y teléfono libres, `idLocalidad: 99L` (No existe)|Ambos `existsBy...AndIdNot` $\rightarrow$ **FALSE**, `localidadRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-MC-06**|Modificación exitosa _(Camino feliz)_|`id: 1L` (Existe), `nombre: "Juan Pérez Gómez"`, `telefono: "1133445566"` (Libre), `email: "juan.perez@mail.com"` (Libre), `direccion: "Av. Siempre Viva 742"`, `idLocalidad: 2L` (Existe)|Todas las validaciones $\rightarrow$ **FALSE**|Actualiza `nombre`, `telefono`, `email`, `direccion` y `localidad`, ejecuta `save()` y retorna DTO actualizado.|

### 5. `bajaCliente(Long id)`

La baja se bloquea solo mientras el cliente tiene un barril actualmente en su poder: un despacho `REGISTRADO` cuyo barril sigue en estado operativo `DESPACHADO` y que es el despacho registrado más reciente de ese barril. Un despacho histórico cuyo barril ya fue devuelto, o devuelto y despachado luego a otro cliente, no bloquea la baja.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BC-01**|Baja de cliente inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No llama a `save()`.|
|**CP-BC-02**|El cliente tiene un barril en su poder|`id: 1L` (Existe), `despachoBarrilRepository.clienteTieneBarrilEnSuPoder(1L)` $\rightarrow$ **TRUE**|El cliente tiene un barril actualmente en su poder|Lanza `ReglaNegocioException` ("No se puede dar de baja el cliente porque tiene un despacho de barril registrado a su nombre"). No ejecuta `save()`.|
|**CP-BC-03**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD), sin barriles en su poder|`findById(1L)` $\rightarrow$ **Presente**, `clienteTieneBarrilEnSuPoder(1L)` $\rightarrow$ **FALSE**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` (nunca `delete()`) y retorna DTO del cliente dado de baja.|
|**CP-BC-04**|Cliente con despachos históricos cuyos barriles ya no están en su poder _(Regresión)_|`id: 1L` (Existe), con despachos registrados de barriles ya devueltos|`clienteTieneBarrilEnSuPoder(1L)` $\rightarrow$ **FALSE**|Permite la baja: setea `estado = BAJA`, invoca `save(entity)` y retorna el DTO.|
|**CP-BC-05**|Orden de chequeos: el cliente inexistente no consulta los barriles en su poder|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No interactúa con `despachoBarrilRepository` ni ejecuta `save()`.|

#### Casos manuales contra la base de datos: `IDespachoBarrilRepository.clienteTieneBarrilEnSuPoder`

Los tests de servicio usan mocks, por lo que la consulta JPQL se verifica manualmente contra PostgreSQL.

|**ID**|**Escenario en BD**|**Resultado Esperado**|
|---|---|---|
|**CP-BC-M01**|El cliente A tiene un despacho `REGISTRADO` y el barril está en `DESPACHADO`|`TRUE`|
|**CP-BC-M02**|El cliente A tuvo un despacho `REGISTRADO` y el barril fue devuelto (barril en `EN_LIMPIEZA`)|`FALSE`|
|**CP-BC-M03**|El despacho del cliente A fue anulado (despacho `ANULADO`, barril en `CON_CERVEZA`)|`FALSE`|
|**CP-BC-M04**|El barril fue devuelto por A y despachado luego a B (barril en `DESPACHADO`, ambos despachos `REGISTRADO`)|A: `FALSE`; B: `TRUE`|
|**CP-BC-M05**|La devolución de A fue anulada (despacho `REGISTRADO`, barril de vuelta en `DESPACHADO`)|`TRUE`|
|**CP-BC-M06**|El cliente no tiene ningún despacho|`FALSE`|
