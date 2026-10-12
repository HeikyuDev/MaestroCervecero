### 1. `filtrarClientes(String nombre, String direccion, Long idLocalidad, Pageable pageable)`

`nombre` y `direccion` son coincidencia parcial, sin distinguir mayúsculas/minúsculas; `idLocalidad` es coincidencia exacta. Todos son opcionales, `null` = no filtra por ese criterio. El nombre y la dirección buscados se normalizan igual que los guardados (se recortan los espacios de los extremos y se colapsan los internos); un texto formado solo por espacios equivale a no filtrar.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FC-01**|Filtra por los 3 criterios informados|`nombre: "Juan"`, `direccion: "Falsa"`, `idLocalidad: 1L`, `pageable: PageRequest.of(0, 10)`, BD con 1 cliente activo que cumple los tres criterios|`filtrarClientes("Juan", "Falsa", 1L, pageable)` contiene elementos|Retorna `Page<ClienteResponseDTO>` con 1 elemento mapeado.|
|**CP-FC-02**|Los 3 parámetros nulos no restringen la búsqueda|`nombre: null`, `direccion: null`, `idLocalidad: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 clientes activos|El service propaga los 3 parámetros nulos tal cual al repositorio|Retorna `Page<ClienteResponseDTO>` con los 3 clientes activos (equivalente a no filtrar).|
|**CP-FC-03**|Consulta sin coincidencias|`nombre: "Inexistente"`, `direccion: null`, `idLocalidad: null`, `pageable: PageRequest.of(0, 10)`|`filtrarClientes("Inexistente", null, null, pageable)` está vacío|Retorna `Page<ClienteResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FC-04**|Normalización de los textos buscados|`nombre: "   Juan   Pérez  "`, `direccion: "  Calle   Falsa "`, `idLocalidad: 1L`|El Service normaliza cada texto antes de consultar|Consulta el repositorio con `"Juan Pérez"`, `"Calle Falsa"` y `1L`.|
|**CP-FC-05**|Textos de solo espacios equivalen a no filtrar|`nombre: "    "`, `direccion: "  "`, `idLocalidad: null`|Tras normalizar, los textos quedan vacíos|Consulta el repositorio con `nombre` y `direccion` en `null`.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-BI-01**|Cliente encontrado|`id: 1L` (Existe en BD)|`findById(1L)` $\rightarrow$ **Presente**|Retorna `ClienteResponseDTO` con los datos de la entidad.|
|**CP-BI-02**|Cliente inexistente|`id: 99L` (No existe en BD)|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el cliente con ID: 99".|

### 3. `altaCliente(ClienteFormDTO clienteFormDTO)`

Los datos de texto se normalizan antes de validar y de guardar: el correo electrónico se guarda en minúsculas y sin espacios en los extremos, y el nombre, la dirección y el teléfono sin espacios de más. La unicidad rige solo para el correo electrónico entre clientes activos (sin distinguir mayúsculas): el nombre y el teléfono no tienen unicidad. El teléfono es opcional; si se informa, debe tener solo dígitos y hasta 15.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AC-01**|Correo electrónico duplicado|`email: "juan@mail.com"` (Existe en BD), `telefono: "1122334455"` (Único), `idLocalidad: 1L`|`existsByEmailIgnoreCase("juan@mail.com")` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un cliente con el correo electrónico 'juan@mail.com'"). No consulta la localidad ni ejecuta `save()`.|
|**CP-AC-02**|El teléfono no tiene unicidad|`email: "nuevo@mail.com"` (Único), `telefono: "1122334455"` (Lo tiene otro cliente activo), `idLocalidad: 1L`|No existe consulta de unicidad del teléfono|Persiste con éxito el cliente con el mismo teléfono.|
|**CP-AC-03**|Correo electrónico duplicado detectado sobre el valor normalizado|`email: "  JUAN@mail.com "` (Existe `"juan@mail.com"`)|`existsByEmailIgnoreCase("juan@mail.com")` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` ("Ya existe un cliente con el correo electrónico 'juan@mail.com'"). No ejecuta `save()`.|
|**CP-AC-04**|Localidad inexistente|`email: "nuevo@mail.com"` (Único), `telefono: "1122334455"`, `idLocalidad: 99L` (No existe)|`existsByEmailIgnoreCase` $\rightarrow$ **FALSE**, `localidadRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-AC-05**|Precedencia de la validación de duplicado|`email: "juan@mail.com"` (Existe en BD), `telefono: "1122334455"`, `idLocalidad: 99L` (tampoco existe)|`existsByEmailIgnoreCase` se evalúa antes que la localidad|Lanza `RecursoDuplicadoException` (no `RecursoNoEncontradoException`). No consulta `localidadRepository`.|
|**CP-AC-06**|Alta exitosa _(Camino feliz)_|`nombre: "Juan Pérez"`, `telefono: "1122334455"`, `email: "juan@mail.com"` (Único), `direccion: "Calle Falsa 123"`, `idLocalidad: 1L` (Existe)|Todas las validaciones $\rightarrow$ **FALSE**|Persiste la entidad con `nombre`, `telefono`, `email`, `direccion`, `localidad` y `estado = ACTIVO` asignados, y retorna DTO.|
|**CP-AC-07**|Normalización de lo que se guarda|`nombre: "  Juan   Pérez "`, `telefono: " 1122334455 "`, `email: "  JUAN@Mail.COM "`, `direccion: "  Calle   Falsa   123 "`|El Service normaliza cada dato antes de validar, consultar la unicidad y guardar|Consulta `existsByEmailIgnoreCase("juan@mail.com")` y guarda `nombre: "Juan Pérez"`, `telefono: "1122334455"`, `email: "juan@mail.com"` y `direccion: "Calle Falsa 123"` (`save` y `flush`); el DTO no se modifica.|
|**CP-AC-08**|Datos obligatorios vacíos|`nombre`, `email` o `direccion` formados solo por espacios (un caso por dato)|El dato normalizado queda vacío|Lanza `ReglaNegocioException` ("El nombre es obligatorio", "El correo electrónico es obligatorio" o "La dirección es obligatoria", según el dato). No consulta la BD (`verifyNoInteractions`).|
|**CP-AC-09**|Textos de más de la longitud máxima _(Límite)_|Cuatro casos, cada uno con un dato excedido en 1: `nombre` (101), `email` (256), `direccion` (256) y `telefono` (16 dígitos)|Longitud `>` máximo: nombre 100, correo electrónico 255, dirección 255, teléfono 15|Lanza `ReglaNegocioException` ("El nombre no puede superar 100 caracteres", "El correo electrónico no puede superar 255 caracteres", "La dirección no puede superar 255 caracteres" o "El teléfono no puede superar 15 dígitos", según el dato). No consulta la BD.|
|**CP-AC-10**|Teléfono con caracteres que no son dígitos|`telefono: "11-2233-4455"`, `"+541122334455"`, `"11 2233 4455"`, `"abc123"`|`ValidacionesTexto.validarTelefono` $\rightarrow$ **Falla**|Lanza `ReglaNegocioException` ("El teléfono solo puede contener dígitos") en cada caso. No consulta la BD.|
|**CP-AC-11**|Teléfono opcional _(Camino feliz)_|`telefono: null`, `""` y `"   "`|El teléfono es opcional: vacío equivale a nulo|Persiste con `telefono = null` en los tres casos.|
|**CP-AC-12**|Violación del índice único al guardar|`email: "juan@mail.com"`; la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_cliente_email` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("Ya existe un cliente con el correo electrónico 'juan@mail.com'").|
|**CP-AC-13**|Reutilización del correo electrónico de un cliente dado de baja|`email: "juan@mail.com"` (solo un cliente con `estado = BAJA` tiene ese valor)|La consulta de unicidad considera solo clientes `ACTIVO` $\rightarrow$ **FALSE**|Persiste con éxito: el correo electrónico de un cliente dado de baja puede volver a usarse.|
|**CP-AC-14**|El nombre no tiene unicidad|Dos altas con `nombre: "Juan Pérez"` y `"juan pérez"`, con correo electrónico distinto|No existe consulta de unicidad del nombre|Ambas se persisten.|

### 4. `modificarCliente(Long id, ClienteFormDTO clienteFormDTO)`

Los datos de texto se normalizan y validan antes de consultar la base de datos, con las mismas reglas que el alta. La unicidad del correo electrónico excluye el propio ID.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MC-01**|Cliente no encontrado por ID|`id: 99L` (No existe en BD), DTO válido|`findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No valida duplicación ni localidad, ni ejecuta `save()`.|
|**CP-MC-02**|Correo electrónico en uso por otro cliente|`id: 1L` (Existe), `email: "otro@mail.com"` (Pertenece al `id: 2L`), `idLocalidad: 1L`|`existsByEmailIgnoreCaseAndIdNot("otro@mail.com", 1L)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` ("El correo electrónico 'otro@mail.com' ya está en uso por otro cliente"). No consulta la localidad ni ejecuta `save()`.|
|**CP-MC-03**|El teléfono no tiene unicidad|`id: 1L` (Existe), `telefono: "1199998888"` (Lo tiene otro cliente activo), `email` propio|No existe consulta de unicidad del teléfono|Persiste con éxito el cliente con el teléfono indicado.|
|**CP-MC-04**|Conservar el propio correo electrónico actual|`id: 1L`, `email: "JUAN@mail.com"` (Mismo cliente, distinto case), `idLocalidad: 1L`|`existsByEmailIgnoreCaseAndIdNot("juan@mail.com", 1L)` excluye el propio ID $\rightarrow$ **FALSE**|Permite actualizar, guarda el correo en minúsculas, persiste y retorna DTO.|
|**CP-MC-05**|Localidad inexistente|`id: 1L` (Existe), email libre, `idLocalidad: 99L` (No existe)|`existsByEmailIgnoreCaseAndIdNot` $\rightarrow$ **FALSE**, `localidadRepository.findById(99L)` $\rightarrow$ **Optional.empty()**|Lanza `RecursoNoEncontradoException`. No ejecuta `save()`.|
|**CP-MC-06**|Modificación exitosa _(Camino feliz)_|`id: 1L` (Existe), `nombre: "Juan Pérez Gómez"`, `telefono: "1133445566"`, `email: "juan.perez@mail.com"` (Libre), `direccion: "Av. Siempre Viva 742"`, `idLocalidad: 2L` (Existe)|Todas las validaciones $\rightarrow$ **FALSE**|Actualiza `nombre`, `telefono`, `email`, `direccion` y `localidad`, ejecuta `save()` y retorna DTO actualizado.|
|**CP-MC-07**|Normalización de lo que se guarda|`id: 1L` (Existe), `nombre: "  Juan   Pérez   Gómez "`, `telefono: " 1133445566 "`, `email: "  NUEVO@Mail.COM "`, `direccion: "  Av.   Siempre   Viva 742 "`|El Service normaliza cada dato antes de validar, consultar la unicidad y guardar|Consulta `existsByEmailIgnoreCaseAndIdNot("nuevo@mail.com", 1L)` y actualiza la entidad con `nombre: "Juan Pérez Gómez"`, `telefono: "1133445566"`, `email: "nuevo@mail.com"` y `direccion: "Av. Siempre Viva 742"` (`save` y `flush`); el DTO no se modifica.|
|**CP-MC-08**|Datos vacíos, demasiado largos o con teléfono no numérico|`nombre`, `email` o `direccion` de solo espacios; `email` de 256 caracteres; `telefono: "11-2233-4455"`|Las validaciones de obligatoriedad, longitud y formato $\rightarrow$ **Fallan**|Lanza `ReglaNegocioException` ("El nombre es obligatorio", "El correo electrónico es obligatorio", "La dirección es obligatoria", "El correo electrónico no puede superar 255 caracteres" o "El teléfono solo puede contener dígitos", según el caso). No consulta la BD (`verifyNoInteractions`).|
|**CP-MC-09**|Duplicado detectado sobre el valor normalizado|`id: 1L` (Existe), `email: "  OTRO@Mail.com "` (en uso por otro cliente activo)|`existsByEmailIgnoreCaseAndIdNot("otro@mail.com", 1L)` $\rightarrow$ **TRUE**|Lanza `RecursoDuplicadoException` ("El correo electrónico 'otro@mail.com' ya está en uso por otro cliente"). No modifica la entidad ni persiste.|
|**CP-MC-10**|Violación del índice único al guardar|`id: 1L` (Existe), `email: "otro@mail.com"`; la consulta previa devuelve FALSE y `save` lanza `DataIntegrityViolationException` por `udx_cliente_email` (pedido simultáneo)|La base de datos rechaza el duplicado que la consulta previa no vio|Lanza `RecursoDuplicadoException` con el mismo mensaje que la consulta previa ("El correo electrónico 'otro@mail.com' ya está en uso por otro cliente").|
|**CP-MC-11**|Reutilización del correo electrónico de un cliente dado de baja|`id: 1L` (Existe), `email: "baja@mail.com"` (solo otro cliente con `estado = BAJA` tiene ese valor)|La consulta de unicidad considera solo clientes `ACTIVO` $\rightarrow$ **FALSE**|Persiste con éxito: el correo electrónico de un cliente dado de baja puede volver a usarse.|
|**CP-MC-12**|Teléfono opcional|`id: 1L` (Existe), `telefono: "   "`|El teléfono vacío equivale a nulo|Persiste con `telefono = null`.|
|**CP-MC-13**|El nombre no tiene unicidad|`id: 1L` (Existe), `nombre: "María López"` (lo tiene otro cliente activo)|No existe consulta de unicidad del nombre|Persiste con éxito el cliente con el nombre indicado.|

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
