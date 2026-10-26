## Casos de prueba de `ValidacionesTexto`

**Tipo de prueba:** unitaria (clase `ValidacionesTextoTest`), sin Spring ni mocks. Verifica las validaciones de formato del CUIT, el teléfono, el nombre de usuario y el correo electrónico, que se aplican sobre el valor ya normalizado con `NormalizacionTexto`.

**Regla:** cada validación lanza `ReglaNegocioException` con un mensaje en español, sin punto final.

- **CUIT:** 11 dígitos ASCII; no se verifica el prefijo ni el dígito verificador.
- **Teléfono:** opcional; si se informa, solo dígitos y hasta 15 (`LimitesTecnicos.LONGITUD_MAXIMA_TELEFONO`).
- **Nombre de usuario:** obligatorio; letras ASCII (sin tildes ni eñes), números, `.`, `_` y `-`, sin espacios.
- **Correo electrónico:** se valida después de su obligatoriedad y su longitud máxima (255). Parte local de letras ASCII, dígitos y `. _ % + -`, sin puntos al inicio, al final ni consecutivos; un único `@`; dominio de etiquetas de letras, dígitos y guiones (sin guion en los extremos) separadas por puntos, terminado en una etiqueta de al menos dos letras; sin espacios. No cubre todo el RFC 5322.

---

### `validarCuit(String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VT-01** | CUIT válidos | `20123456786`, `30546689979` (YPF S.A.), `30703088534` (MercadoLibre S.R.L.), `33693450239` (AFIP), `30500010912`, `27200000000` | Tiene 11 dígitos ASCII | No lanza excepción. |
| **CP-VT-02** | Largo distinto de 11 | `null`, `""`, `2012345678` (10), `201234567866` (12), `123`, `20 12345678 6`, `20-12345678-6` | La cantidad de caracteres no es 11 | Lanza `ReglaNegocioException`: "El CUIT debe tener 11 dígitos". |
| **CP-VT-03** | 11 caracteres que no son todos dígitos ASCII | `2012345678a`, `2O123456786` (letra O), `abcdefghijk`, `20123456 86`, once dígitos arábigos orientales | Algún carácter no es un dígito `0-9` | Lanza `ReglaNegocioException`: "El CUIT debe tener 11 dígitos". |

### `formatearCuit(String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VT-04** | CUIT de 11 dígitos | `20123456786`, `20-12345678-6`, `20 12345678 6`, `30546689979` | Sin separadores hay 11 dígitos | Devuelve `20-12345678-6`, `20-12345678-6`, `20-12345678-6` y `30-54668997-9`. |
| **CP-VT-05** | Valor que no tiene 11 dígitos | `null`, `""`, `123`, `2012345678a` | No se puede dar formato | Devuelve el valor sin cambios (`null` para nulo). |

### `validarTelefono(String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VT-06** | Teléfono válido u omitido | `null`, `""`, `1`, `1144445555`, `011444455556` (12 dígitos), `123456789012345` (15 dígitos) | Es opcional y, si se informa, tiene solo dígitos y hasta 15 | No lanza excepción. |
| **CP-VT-07** | Caracteres que no son dígitos | `011-4444-5555`, `+5491144445555`, `11 4444 5555`, `(011)44445555`, `11a4444`, `1144.4455`, cinco dígitos arábigos orientales | Algún carácter no es un dígito `0-9` | Lanza `ReglaNegocioException`: "El teléfono solo puede contener dígitos". |
| **CP-VT-08** | Más de 15 dígitos | `1234567890123456` (16 dígitos) | La cantidad de dígitos supera el máximo | Lanza `ReglaNegocioException`: "El teléfono no puede superar 15 dígitos". |

### `validarUsername(String)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VT-09** | Nombre de usuario válido | `a`, `juan.perez`, `juan_perez`, `juan-perez`, `juan.perez-2_x`, `30123456` (un DNI), `Admin`, `ADMIN01` | Solo letras ASCII, números, `.`, `_` y `-` | No lanza excepción. |
| **CP-VT-10** | Nulo o vacío | `null`, `""` | El nombre de usuario es obligatorio | Lanza `ReglaNegocioException`: "El nombre de usuario es obligatorio". |
| **CP-VT-11** | Caracteres no permitidos | `juan perez`, ` juan`, `juan\tperez`, `juan@mail.com`, `ñandú`, `josé`, `juan!`, `juan/perez`, `juan+perez`, dígitos arábigos orientales | Contiene espacios, tildes, eñes, símbolos u otros caracteres fuera del conjunto permitido | Lanza `ReglaNegocioException`: "El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos, sin espacios". |

### `validarCorreo(String)` y `PATRON_CORREO_FORMULARIO`

`PATRON_CORREO_FORMULARIO` es el patrón de `@Pattern` de los FormDTO de Proveedor, Cliente y Usuario, con el mismo mensaje que `validarCorreo` (`MENSAJE_CORREO_INVALIDO`): el formulario y el Service rechazan los mismos valores con el mismo texto.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-VT-12** | Correo válido | `ventas@malteria.com.ar`, `a.b+c@dominio.co`, `contacto@malteriadelsur.com`, `juan_perez-2@mail-sur.com.ar`, `info%ventas@x1.io`, `VENTAS@MALTERIA.COM` | Parte local, un único `@` y un dominio con al menos un punto que termina en una etiqueta de al menos dos letras | No lanza excepción. |
| **CP-VT-13** | Formato inválido | `null`, `""`, `no-es-correo`, `a@b`, `a@@b.com`, `a b@c.com`, `@x.com`, `x@.com`, `x@y.`, `.a@b.com`, `a.@b.com`, `a..b@c.com`, `a@b..com`, `a@-b.com`, `a@b-.com`, `a@b.c`, `a@b.com.`, `a@b,com`, `ñandu@b.com`, `a@b@c.com`, `a@b.co m` | Falta la parte local, el `@` o el punto del dominio; hay más de un `@`, espacios, puntos al inicio, al final o consecutivos, guiones en los extremos de una etiqueta, una terminación de una sola letra o caracteres no permitidos | Lanza `ReglaNegocioException`: "El correo electrónico no tiene un formato válido". |
| **CP-VT-14** | El patrón del formulario acepta lo que acepta el Service | `""`, `"   "`, `ventas@malteria.com.ar`, `"  Ventas@Malteria.COM.ar  "`, `a.b+c@dominio.co` | El valor llega sin normalizar: se admiten espacios en los extremos y mayúsculas; el vacío lo informa `@NotBlank` | El patrón coincide. |
| **CP-VT-15** | El patrón del formulario rechaza lo que rechaza el Service | `no-es-correo`, `a@b`, `a@@b.com`, `a b@c.com`, `@x.com`, `x@.com`, `x@y.`, `a..b@c.com`, `a@b.c`, `ñandu@b.com` | Mismo formato que `validarCorreo` | El patrón no coincide. |
