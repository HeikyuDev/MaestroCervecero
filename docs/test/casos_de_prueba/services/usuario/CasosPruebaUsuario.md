### 1. `filtrarUsuarios(String nombre, String apellido, String correo, String username, Rol rol, Pageable pageable)`

`nombre`, `apellido`, `correo` y `username` son coincidencia parcial, sin distinguir mayúsculas/minúsculas; `rol` es coincidencia exacta. Todos son opcionales, `null` = no filtra por ese criterio.

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-FU-01** | Filtra por los 5 criterios informados | `nombre: "Juan"`, `apellido: "Apellido Prueba"`, `correo: "juan"`, `username: "jperez"`, `rol: OPERARIO_DE_PRODUCCION`, `pageable: PageRequest.of(0, 10)`, BD con 2 usuarios activos que cumplen los cinco criterios | `filtrarUsuarios("Juan", "Apellido Prueba", "juan", "jperez", OPERARIO_DE_PRODUCCION, pageable)` contiene elementos | Retorna `Page<UsuarioResponseDTO>` con 2 elementos mapeados (incluido el apellido). |
| **CP-FU-02** | Los 5 parámetros nulos no restringen la búsqueda | `nombre: null`, `apellido: null`, `correo: null`, `username: null`, `rol: null`, `pageable: PageRequest.of(0, 10)` | El service propaga los 5 parámetros nulos tal cual al repositorio | Retorna `Page<UsuarioResponseDTO>` con todos los usuarios activos (equivalente a no filtrar). |
| **CP-FU-03** | Consulta sin coincidencias | `nombre: "Inexistente"`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)` | `filtrarUsuarios("Inexistente", null, null, null, null, pageable)` está vacío | Retorna `Page<UsuarioResponseDTO>` vacía (`getContent().isEmpty() == true`). |

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
| **CP-AU-02** | Contraseña nula | `username: "carlos_cervecero"` (Único), `password: null` | `existsByUsername` $\rightarrow$ **FALSE**, `password == null` | Lanza `ReglaNegocioException` con mensaje "La contraseña es obligatoria". No encripta ni persiste. |
| **CP-AU-03** | Contraseña de solo espacios | `username: "carlos_cervecero"` (Único), `password: "                "` (16 espacios) | `existsByUsername` $\rightarrow$ **FALSE**, `validarPassword` $\rightarrow$ **Falla** | Lanza `ReglaNegocioException` con mensaje "La contraseña debe tener entre 8 y 72 caracteres y no puede contener espacios en blanco". No encripta ni persiste. |
| **CP-AU-04** | Alta exitosa y encriptación de contraseña *(Camino feliz)* | `username: "carlos_cervecero"` (Único), `password: "ClaveSegura123"` | `existsByUsername` $\rightarrow$ **FALSE** | Encripta password con `passwordEncoder`, persiste entidad (con `nombre` y `apellido`) con `estado = ACTIVO` y retorna `UsuarioResponseDTO`. |
| **CP-AU-05** | Contraseña vacía | `username: "carlos_cervecero"` (Único), `password: ""` | `password.isEmpty()` $\rightarrow$ **TRUE** | Lanza `ReglaNegocioException` con mensaje "La contraseña es obligatoria". No encripta ni persiste. |
| **CP-AU-06** | Contraseña de menos de 8 caracteres _(Límite)_ | `password: "abcdefg"` (7 caracteres) | `validarPassword` $\rightarrow$ **Falla** | Lanza `ReglaNegocioException` con el mensaje de contraseña inválida. No encripta ni persiste. |
| **CP-AU-07** | Contraseña con espacios en el medio, al inicio, al final o Unicode | `password: "clave segura1"`, `" ClaveSegura1"`, `"ClaveSegura1 "`, `"Clave Segura1"` (espacio de no separación) | `validarPassword` $\rightarrow$ **Falla** en los cuatro | Lanza `ReglaNegocioException` con el mensaje de contraseña inválida en cada caso. No encripta ni persiste. |
| **CP-AU-08** | Contraseñas en los límites válidos _(Camino feliz)_ | `password` de 8 caracteres y de 72 caracteres | `validarPassword` $\rightarrow$ **OK** | Encripta y persiste en ambos casos. |
| **CP-AU-09** | Contraseña demasiado larga (más de 72 caracteres o más de 72 bytes) | `password: "a" x 73`; y `"ñ" x 40` (40 caracteres pero 80 bytes en UTF-8) | `validarPassword` $\rightarrow$ **Falla** | Lanza `ReglaNegocioException`: mensaje de contraseña inválida en el primer caso y "demasiado larga" (límite de BCrypt de 72 bytes) en el segundo. No encripta ni persiste. |

---

### 4. `modificarUsuario(Long id, UsuarioFormDTO usuarioFormDTO)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-MU-01** | Usuario no encontrado por ID | `id: 99L` (No existe en BD), DTO válido | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "El usuario no existe". No persiste. |
| **CP-MU-02** | Modificación de username en uso por otra cuenta | `id: 1L` (User actual: `"juan"`), `username: "pedro"` (Existe en BD) | `!equalsIgnoreCase` $\rightarrow$ **TRUE** && `existsByUsername` $\rightarrow$ **TRUE** | Lanza `RecursoDuplicadoException`. No actualiza ni persiste. |
| **CP-MU-03** | Modificación exitosa con nuevo username y nueva contraseña *(Camino Feliz)* | `id: 1L` (User actual: `"juan"`), `username: "juan_nuevo"` (Libre), `password: "NuevaClave123"` | `!equalsIgnoreCase` $\rightarrow$ **TRUE**, `password != null && !isEmpty()` $\rightarrow$ **TRUE**, `validarPassword` $\rightarrow$ **OK** | Encripta nueva contraseña, actualiza todos los datos (incluido el apellido), persiste y retorna DTO. |
| **CP-MU-04** | Conservar el propio username actual | `id: 1L` (User actual: `"juan"`), `username: "juan"` | `!equalsIgnoreCase` $\rightarrow$ **FALSE** | No consulta duplicados en BD, actualiza datos, persiste y retorna DTO. |
| **CP-MU-05** | Conservar el propio username con distinta capitalización *(Case-Insensitive)* | `id: 1L` (User actual: `"juan"`), `username: "JUAN"` | `!equalsIgnoreCase` $\rightarrow$ **FALSE** | No consulta duplicados en BD, actualiza datos, persiste y retorna DTO. |
| **CP-MU-06** | Modificación sin actualizar contraseña (password `null`) | `id: 1L`, `password: null`, resto válido | `password != null` $\rightarrow$ **FALSE** | Mantiene la contraseña existente en la entidad sin llamar a `passwordEncoder`, persiste y retorna DTO. |
| **CP-MU-07** | Modificación sin actualizar contraseña (password vacío) | `id: 1L`, `password: ""`, resto válido | `password.isEmpty()` $\rightarrow$ **TRUE** | Mantiene la contraseña existente en la entidad sin llamar a `passwordEncoder`, persiste y retorna DTO. |
| **CP-MU-08** | Contraseña de solo espacios | `id: 1L`, `password: "                "` (16 espacios), `nombre` distinto al actual | `validarPassword` $\rightarrow$ **Falla** | Lanza `ReglaNegocioException` con el mensaje de contraseña inválida. La validación ocurre antes de tocar la entidad: no modifica ningún dato, no llama a `passwordEncoder` ni persiste. |
| **CP-MU-09** | Contraseña de menos de 8 caracteres o con espacios | `id: 1L`, `password: "abcdefg"` y `"clave con espacios"` | `validarPassword` $\rightarrow$ **Falla** en ambos | Lanza `ReglaNegocioException` con el mensaje de contraseña inválida. No modifica la entidad ni persiste. |

---

### 5. `bajaUsuario(Long id)`

| ID | Nombre del Caso | Datos de Entrada (Escenario) | Condición Evaluada | Resultado Esperado |
| --- | --- | --- | --- | --- |
| **CP-BU-01** | Baja de usuario inexistente | `id: 99L` (No existe en BD) | `findById(99L)` $\rightarrow$ **Optional.empty()** | Lanza `RecursoNoEncontradoException` con mensaje "No se encontró el usuario con ID: 99". No llama a `save()`. |
| **CP-BU-02** | Baja exitosa *(Baja lógica vía Estado)* | `id: 1L` (Existe en BD) | `findById(1L)` $\rightarrow$ **Presente** | Setea `estado = BAJA` en la entidad, invoca `save(entity)` y retorna DTO del usuario dado de baja. |