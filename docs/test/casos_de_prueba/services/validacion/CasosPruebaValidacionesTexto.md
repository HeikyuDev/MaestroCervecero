## Casos de prueba de `ValidacionesTexto`

**Tipo de prueba:** unitaria (clase `ValidacionesTextoTest`), sin Spring ni mocks. Verifica las validaciones de formato del CUIT, el teléfono y el nombre de usuario, que se aplican sobre el valor ya normalizado con `NormalizacionTexto`.

**Regla:** cada validación lanza `ReglaNegocioException` con un mensaje en español, sin punto final.

- **CUIT:** 11 dígitos ASCII, prefijo de tipo de persona 20, 23, 24, 27, 30, 33 o 34 y dígito verificador de módulo 11 (factores 5, 4, 3, 2, 7, 6, 5, 4, 3, 2 sobre los 10 primeros dígitos; resto 0 equivale a verificador 0; resto 1 no admite verificador).
- **Teléfono:** opcional; si se informa, solo dígitos y hasta 15 (`LimitesTecnicos.LONGITUD_MAXIMA_TELEFONO`).
- **Nombre de usuario:** obligatorio; letras ASCII (sin tildes ni eñes), números, `.`, `_` y `-`, sin espacios.

---

### `validarCuit(String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VT-01** | CUIT válidos | `20123456786`, `30546689979` (YPF S.A.), `30703088534` (MercadoLibre S.R.L.), `33693450239` (AFIP), `30500010912`, `27200000000` (verificador 0) | Prefijo válido, 11 dígitos y verificador correcto | No lanza excepción. |
| **CP-VT-02** | Largo distinto de 11 | `null`, `""`, `2012345678` (10), `201234567866` (12), `123`, `20 12345678 6`, `20-12345678-6` | La cantidad de caracteres no es 11 | Lanza `ReglaNegocioException`: "El CUIT debe tener 11 dígitos". |
| **CP-VT-03** | 11 caracteres que no son todos dígitos ASCII | `2012345678a`, `2O123456786` (letra O), `abcdefghijk`, `20123456 86`, once dígitos arábigos orientales | Algún carácter no es un dígito `0-9` | Lanza `ReglaNegocioException`: "El CUIT debe tener 11 dígitos". |
| **CP-VT-04** | Dígito verificador incorrecto | `20123456787`, `20123456780`, `30546689970`, `33693450238`, `27200000001` | El último dígito no coincide con el calculado | Lanza `ReglaNegocioException`: "El CUIT no es válido". |
| **CP-VT-05** | Prefijo inválido con verificador correcto | `10123456780`, `21123456782`, `25123456788`, `40123456787`, `00123456785` | El prefijo no es 20, 23, 24, 27, 30, 33 ni 34 | Lanza `ReglaNegocioException`: "El CUIT no es válido". |
| **CP-VT-06** | Número sin verificador posible | `30689999910`, `30689999911`, `30689999915`, `30689999919` | El módulo 11 de los 10 primeros dígitos da 10, que no es un dígito | Lanza `ReglaNegocioException`: "El CUIT no es válido" sea cual sea el último dígito. |

### `formatearCuit(String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VT-07** | CUIT de 11 dígitos | `20123456786`, `20-12345678-6`, `20 12345678 6`, `30546689979` | Sin separadores hay 11 dígitos | Devuelve `20-12345678-6`, `20-12345678-6`, `20-12345678-6` y `30-54668997-9`. |
| **CP-VT-08** | Valor que no tiene 11 dígitos | `null`, `""`, `123`, `2012345678a` | No se puede dar formato | Devuelve el valor sin cambios (`null` para nulo). |

### `validarTelefono(String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VT-09** | Teléfono válido u omitido | `null`, `""`, `1`, `1144445555`, `011444455556` (12 dígitos), `123456789012345` (15 dígitos) | Es opcional y, si se informa, tiene solo dígitos y hasta 15 | No lanza excepción. |
| **CP-VT-10** | Caracteres que no son dígitos | `011-4444-5555`, `+5491144445555`, `11 4444 5555`, `(011)44445555`, `11a4444`, `1144.4455`, cinco dígitos arábigos orientales | Algún carácter no es un dígito `0-9` | Lanza `ReglaNegocioException`: "El teléfono solo puede contener dígitos". |
| **CP-VT-11** | Más de 15 dígitos | `1234567890123456` (16 dígitos) | La cantidad de dígitos supera el máximo | Lanza `ReglaNegocioException`: "El teléfono no puede superar 15 dígitos". |

### `validarUsername(String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VT-12** | Nombre de usuario válido | `a`, `juan.perez`, `juan_perez`, `juan-perez`, `juan.perez-2_x`, `30123456` (un DNI), `Admin`, `ADMIN01` | Solo letras ASCII, números, `.`, `_` y `-` | No lanza excepción. |
| **CP-VT-13** | Nulo o vacío | `null`, `""` | El nombre de usuario es obligatorio | Lanza `ReglaNegocioException`: "El nombre de usuario es obligatorio". |
| **CP-VT-14** | Caracteres no permitidos | `juan perez`, ` juan`, `juan\tperez`, `juan@mail.com`, `ñandú`, `josé`, `juan!`, `juan/perez`, `juan+perez`, dígitos arábigos orientales | Contiene espacios, tildes, eñes, símbolos u otros caracteres fuera del conjunto permitido | Lanza `ReglaNegocioException`: "El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos, sin espacios". |
