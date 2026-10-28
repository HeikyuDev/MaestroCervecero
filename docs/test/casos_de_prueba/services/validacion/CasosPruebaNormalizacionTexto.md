## Casos de prueba de `NormalizacionTexto`

**Tipo de prueba:** unitaria (clase `NormalizacionTextoTest`), sin Spring ni mocks. Verifica la normalización de los textos que ingresa el usuario, que se aplica antes de validar y de guardar.

**Regla:** son funciones puras y tolerantes a `null` (un valor nulo devuelve `null`). La normalización no valida: los caracteres no permitidos se conservan para que los rechace la validación correspondiente (`ValidacionesTexto`). Se considera espacio en blanco cualquier espacio ASCII, el espacio no separable (NBSP, `U+00A0`), los espacios de ancho fijo y los separadores Unicode. Los caracteres invisibles de ancho cero (`U+200B`, `U+200C`, `U+200D`, `U+2060`, `U+FEFF`) se eliminan. Las conversiones a mayúsculas y minúsculas usan `Locale.ROOT`.

---

### `nombrePropio(String)`

Nombres, razones sociales y demás textos libres. Conserva las mayúsculas, las tildes y la puntuación escritas.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-NT-01** | Espacios, tabulaciones y saltos de línea | `"  Pale Ale  "`, `"Pale    Ale"`, `"Pale  \t \n Ale"`, `"\tPale Ale\t"`, `"\r\nPale\r\nAle\r\n"`, `"Pale Ale"` | Se recortan los extremos y se colapsan los espacios internos | Devuelve `"Pale Ale"` en todos los casos. |
| **CP-NT-02** | Espacios no separables y separadores Unicode | NBSP en los extremos y repetido, `U+2003`, `U+202F`, `U+3000`, `U+2007` y `U+2009` entre las palabras | Cualquier espacio Unicode cuenta como espacio en blanco | Devuelve `"Pale Ale"` en todos los casos. |
| **CP-NT-03** | Caracteres de ancho cero | `U+200B` en los extremos y dentro de una palabra, `U+FEFF`, `U+2060`, `U+200D`, y un texto formado solo por `U+200B`, `U+200C` y `U+200D` | Los caracteres invisibles no se conservan | Devuelve `"Pale Ale"` para los textos con contenido y `""` para el que solo tiene caracteres invisibles. |
| **CP-NT-04** | Mayúsculas, tildes, ñ y puntuación | `"Cervecería  Ñandú S.A."`, `"  José   Pérez-Muñoz "`, `"IPA (Doble) #1"`, `"pale ale"` | Solo se corrigen los espacios | Conserva las mayúsculas, las tildes, la ñ y la puntuación escritas (`"Cervecería Ñandú S.A."`, `"José Pérez-Muñoz"`, `"IPA (Doble) #1"`, `"pale ale"`). |
| **CP-NT-05** | Forma Unicode NFC | `"Cafe"` con la `e` seguida del acento combinante `U+0301` (NFD); `"n"` seguida de `U+0303` | El texto se pasa a la forma compuesta | Devuelve `"Café"` y `"ñ"` con el carácter precompuesto (`U+00E9`, `U+00F1`). |
| **CP-NT-06** | Nulo, vacío y solo espacios | `null`, `""`, `"   \t "`, dos NBSP | No hay contenido que normalizar | Devuelve `null` para nulo y cadena vacía para los demás. |

### `codigo(String)`

Códigos e identificadores (identificación de lote del proveedor y texto buscado del identificador interno). Se guardan en mayúsculas.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-NT-07** | Mayúsculas con recorte y colapso de espacios | `"fer-01"`, `"  fer-01  "`, `"fer   01"`, NBSP + `"fer.01/a"` + NBSP, `"ñandú-1"`, `"Lote 2026/001"`, `"YA-EN-MAYUSCULAS"` | Se aplica `nombrePropio` y luego se pasa a mayúsculas; la puntuación cuenta | Devuelve `"FER-01"`, `"FER-01"`, `"FER 01"`, `"FER.01/A"`, `"ÑANDÚ-1"`, `"LOTE 2026/001"` y `"YA-EN-MAYUSCULAS"`. |
| **CP-NT-08** | Independencia del idioma del servidor | Idioma por defecto `tr-TR` y `"titan-i"` | La conversión usa `Locale.ROOT` (en turco, `i` pasaría a `İ`) | Devuelve `"TITAN-I"`. |
| **CP-NT-09** | Nulo, vacío y solo espacios | `null`, `""`, `"  \t "` | No hay contenido que normalizar | Devuelve `null` para nulo y cadena vacía para los demás. |

### `correo(String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-NT-10** | Recorte y minúsculas | `"juan@mail.com"`, `"  Juan.Perez@Mail.COM  "`, NBSP + `"JUAN@MAIL.COM"` + NBSP, `"\tAdmin@Cerveceria.com.ar\n"` | Se recortan los extremos y se pasa a minúsculas | Devuelve `"juan@mail.com"`, `"juan.perez@mail.com"`, `"juan@mail.com"` y `"admin@cerveceria.com.ar"`. |
| **CP-NT-11** | Nulo, vacío y solo espacios | `null`, `""`, `"   "` | No hay contenido que normalizar | Devuelve `null` para nulo y cadena vacía para los demás. |

### `cuit(String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-NT-12** | Separadores del CUIT | `"20123456786"`, `"20-12345678-6"`, `"20 12345678 6"`, `"20.12345678.6"`, `"20/12345678/6"`, `"  20-12345678-6  "`, y el mismo número con guiones Unicode (`U+2010`, `U+2013`) y NBSP | Se eliminan los espacios, guiones, puntos y barras | Devuelve `"20123456786"` en todos los casos. |
| **CP-NT-13** | Caracteres que no son separadores | `"20-1234567a-6"`, `"CUIT 20123456786"`, `"20123456786+"` | Las letras y los símbolos se conservan para que `validarCuit` rechace el valor y no lo acepte en silencio | Devuelve `"201234567a6"`, `"CUIT20123456786"` y `"20123456786+"`. |
| **CP-NT-14** | Nulo, vacío y solo separadores | `null`, `""`, `" - "` | No queda contenido | Devuelve `null` para nulo y cadena vacía para los demás. |

### `telefono(String)`

El teléfono es un dato opcional.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-NT-15** | Recorte | `"1144445555"`, `"  1144445555  "`, NBSP + `"1144445555"` + NBSP | Se recortan los extremos | Devuelve `"1144445555"` en todos los casos. |
| **CP-NT-16** | Sin contenido | `null`, `""`, `" "`, `"   \t "`, NBSP, `U+200B` | Un teléfono vacío equivale a no informado | Devuelve `null` en todos los casos. |
| **CP-NT-17** | Caracteres que no son dígitos | `"011-4444-5555"`, `"+54 9 11 4444 5555"`, `"011   4444"` | Los símbolos y espacios internos se conservan para que `validarTelefono` los rechace; solo se colapsan los espacios repetidos | Devuelve `"011-4444-5555"`, `"+54 9 11 4444 5555"` y `"011 4444"`. |

### `username(String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-NT-18** | Recorte y minúsculas | `"juan.perez"`, `"  Juan.Perez  "`, `"ADMIN"`, `"30123456"` (un DNI), NBSP + `"Juan_Perez-2"` + NBSP | Se recortan los extremos y se pasa a minúsculas | Devuelve `"juan.perez"`, `"juan.perez"`, `"admin"`, `"30123456"` y `"juan_perez-2"`. |
| **CP-NT-19** | Espacios internos | `"Juan   Perez"` | Los espacios internos se conservan (colapsados) para que `validarUsername` los rechace | Devuelve `"juan perez"`. |
| **CP-NT-20** | Nulo, vacío y solo espacios | `null`, `""`, `"   "` | No hay contenido que normalizar | Devuelve `null` para nulo y cadena vacía para los demás. |
| **CP-NT-21** | Independencia del idioma del servidor | Idioma por defecto `tr-TR`, `username("ISMAEL")` y `correo("INFO@MAIL.COM")` | La conversión usa `Locale.ROOT` (en turco, `I` pasaría a `ı`) | Devuelve `"ismael"` y `"info@mail.com"`. |
