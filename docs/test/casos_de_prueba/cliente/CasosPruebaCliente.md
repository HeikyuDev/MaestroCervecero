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

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AC-01**|Correo electrónico duplicado|`email: "juan@mail.com"` (Existe en BD), `telefono: "1122334455"` (Único), `idLocalidad: 1L`|`existsByEmailIgnoreCaseOrTelefono` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No consulta la localidad ni ejecuta `save()`.|
|**CP-AC-02**|Teléfono duplicado|`email: "nuevo@mail.com"` (Único), `telefono: "1122334455"` (Existe en BD), `idLocalidad: 1L`|`existsByEmailIgnoreCaseOrTelefono` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No consulta la localidad ni ejecuta `save()`.|
|**CP-AC-03**|Correo electrónico duplicado Case-Insensitive|`email: "JUAN@mail.com"` (Existe `"juan@mail.com"`), `telefono: "1122334455"` (Único)|`existsByEmailIgnoreCaseOrTelefono` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No ejecuta `save()`.|
|**CP-AC-04**|Localidad inexistente|`email: "nuevo@mail.com"` (Único), `telefono: "1122334455"` (Único), `idLocalidad: 99L` (No existe)|`existsByEmailIgnoreCaseOrTelefono` $\rightarrow$ **FALSE**, `localidadRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-AC-05**|Precedencia de la validación de duplicado|`email: "juan@mail.com"` (Existe en BD), `telefono: "1122334455"` (Único), `idLocalidad: 99L` (tampoco existe)|`existsByEmailIgnoreCaseOrTelefono` se evalúa antes que `localidadRepository.findById`|Lanza `RecursoDuplicadoException` (no `RecursoNoEncontradoException`). No consulta `localidadRepository`.|
|**CP-AC-06**|Alta exitosa _(Camino feliz)_|`nombre: "Juan Pérez"`, `telefono: "1122334455"` (Único), `email: "juan@mail.com"` (Único), `direccion: "Calle Falsa 123"`, `idLocalidad: 1L` (Existe)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `nombre`, `telefono`, `email`, `direccion`, `localidad` y `estado = ACTIVO` asignados, y retorna DTO.|

### 4. `modificarCliente(Long id, ClienteFormDTO clienteFormDTO)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MC-01**|Cliente no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida duplicación ni localidad, ni ejecuta `save()` (`verifyNoInteractions` sobre `clienteRepository` salvo `findById`, y sin interacción con `localidadRepository`).|
|**CP-MC-02**|Correo electrónico o teléfono en uso por otro cliente|`id: 1L` (Existe), `email: "otro@mail.com"` (Pertenece al `id: 2L`), `telefono: "1122334455"`, `idLocalidad: 1L`|`existsByEmailIgnoreCaseOrTelefonoAndIdNot` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException`. No consulta la localidad ni ejecuta `save()`.|
|**CP-MC-03**|Conservar el propio correo electrónico y teléfono actual|`id: 1L`, `email: "JUAN@mail.com"` (Mismo cliente, distinto case), `telefono: "1122334455"` (Propio), `idLocalidad: 1L`|`existsByEmailIgnoreCaseOrTelefonoAndIdNot` $\rightarrow$ **FALSE**|Permite actualizar, persiste y retorna DTO.|
|**CP-MC-04**|Localidad inexistente|`id: 1L` (Existe), email y teléfono libres, `idLocalidad: 99L` (No existe)|`existsByEmailIgnoreCaseOrTelefonoAndIdNot` $\rightarrow$ **FALSE**, `localidadRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-MC-05**|Modificación exitosa _(Camino feliz)_|`id: 1L` (Existe), `nombre: "Juan Pérez Gómez"`, `telefono: "1133445566"` (Libre), `email: "juan.perez@mail.com"` (Libre), `direccion: "Av. Siempre Viva 742"`, `idLocalidad: 2L` (Existe)|Todas las validaciones $\rightarrow$ **FALSE**|Actualiza `nombre`, `telefono`, `email`, `direccion` y `localidad`, ejecuta `save()` y retorna DTO actualizado.|

### 5. `bajaCliente(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BC-01**|Baja de cliente inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No llama a `save()`.|
|**CP-BC-02**|Baja exitosa _(Baja lógica vía Estado)_|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Setea `estado = BAJA` en la entidad, invoca `save(entity)` (nunca `delete()`) y retorna DTO del cliente dado de baja.|

> **Nota:** la regla de negocio "no debe poseer barriles en estado Despachado a su nombre" queda pendiente de cobertura: el servicio la marca con un `TODO` porque el módulo de Barriles/Despacho todavía no está implementado.
