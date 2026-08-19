### 1. `buscarTodos(Pageable pageable)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-BT-01** | Consulta con registros existentes | `pageable: PageRequest.of(0, 10)`, BD con 2 usuarios activos | `findAll(pageable)` contiene elementos | Retorna `Page<UsuarioResponseDTO>` con 2 elementos mapeados. |
| **CP-BT-02** | Consulta sin registros existentes | `pageable: PageRequest.of(0, 10)`, BD vacía | `findAll(pageable)` está vacío | Retorna `Page<UsuarioResponseDTO>` vacía (`getContent().isEmpty() == true`). |

---

### 2. `buscarPorId(Long id)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-BI-01** | Usuario encontrado | `id: 1L` (Existe en BD) | `findById(1L)` $\rightarrow$ **Presente** | Retorna `UsuarioResponseDTO` con los datos de la entidad. |
| **CP-BI-02** | Usuario inexistente | `id: 99L` (No existe en BD) | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "El usuario no existe". |

---

### 3. `altaUsuario(UsuarioFormDTO usuarioFormDTO)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-AU-01** | Username duplicado | `username: "juanperez"` (Ya existe en BD), `password: "123456"` | `existsByUsername` $\rightarrow$ **TRUE** | Lanza `RecursoDuplicadoException` con mensaje "El Username ya esta registrado". No encripta ni persiste. |
| **CP-AU-02** | Alta exitosa y encriptación de contraseña *(Camino feliz)* | `username: "carlos_cervecero"` (Único), `password: "ClaveSegura123"` | `existsByUsername` $\rightarrow$ **FALSE** | Encripta password con `passwordEncoder`, persiste entidad y retorna `UsuarioResponseDTO`. |

---

### 4. `modificarUsuario(Long id, UsuarioFormDTO usuarioFormDTO)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-MU-01** | Usuario no encontrado por ID | `id: 99L` (No existe en BD), DTO válido | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "El usuario no existe". No persiste. |
| **CP-MU-02** | Modificación de username en uso por otra cuenta | `id: 1L` (User actual: `"juan"`), `username: "pedro"` (Existe en BD) | `!equalsIgnoreCase` $\rightarrow$ **TRUE** && `existsByUsername` $\rightarrow$ **TRUE** | Lanza `RecursoDuplicadoException`. No actualiza ni persiste. |
| **CP-MU-03** | Modificación exitosa con nuevo username y nueva contraseña *(Camino Feliz)* | `id: 1L` (User actual: `"juan"`), `username: "juan_nuevo"` (Libre), `password: "NuevaClave123"` | `!equalsIgnoreCase` $\rightarrow$ **TRUE**, `password != null && !isBlank()` $\rightarrow$ **TRUE** | Encripta nueva contraseña, actualiza todos los datos, persiste y retorna DTO. |
| **CP-MU-04** | Conservar el propio username actual | `id: 1L` (User actual: `"juan"`), `username: "juan"` | `!equalsIgnoreCase` $\rightarrow$ **FALSE** | No consulta duplicados en BD, actualiza datos, persiste y retorna DTO. |
| **CP-MU-05** | Conservar el propio username con distinta capitalización *(Case-Insensitive)* | `id: 1L` (User actual: `"juan"`), `username: "JUAN"` | `!equalsIgnoreCase` $\rightarrow$ **FALSE** | No consulta duplicados en BD, actualiza datos, persiste y retorna DTO. |
| **CP-MU-06** | Modificación sin actualizar contraseña (password `null`) | `id: 1L`, `password: null`, resto válido | `password != null` $\rightarrow$ **FALSE** | Mantiene la contraseña existente en la entidad sin llamar a `passwordEncoder`, persiste y retorna DTO. |
| **CP-MU-07** | Modificación sin actualizar contraseña (password en blanco / vacío) | `id: 1L`, `password: "   "`, resto válido | `!password.isBlank()` $\rightarrow$ **FALSE** | Mantiene la contraseña existente en la entidad sin llamar a `passwordEncoder`, persiste y retorna DTO. |

---

### 5. `bajaUsuario(Long id)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-BU-01** | Baja de usuario inexistente | `id: 99L` (No existe en BD) | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el usuario con ID: 99". No llama a `delete()`. |
| **CP-BU-02** | Baja exitosa *(Soft Delete)* | `id: 1L` (Existe en BD) | `findById(1L)` $\rightarrow$ **Presente** | Invoca `delete(entity)` y retorna DTO del usuario dado de baja. |