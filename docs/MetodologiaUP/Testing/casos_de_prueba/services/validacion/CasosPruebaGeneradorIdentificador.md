## Casos de prueba de `GeneradorIdentificador`

**Tipo de prueba:** unitaria (clase `GeneradorIdentificadorTest`), sin Spring ni mocks. Verifica el único lugar donde se arma el identificador interno que genera el sistema para los equipos (molino, macerador, olla de hervor y fermentador) y los barriles.

**Regla:** el identificador es el prefijo del tipo (`MOL`, `MAC`, `OLL`, `FER`, `BRL`), un guion y el valor de la secuencia con dos dígitos como mínimo, completado con ceros a la izquierda. Un número de más de dos dígitos no se trunca.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-GI-01** | Relleno con ceros hasta dos dígitos | Prefijos de los cinco tipos con los números 1, 9, 10, 99 y 5 | Un número de un dígito se completa con un cero | Devuelve `MOL-01`, `MAC-09`, `OLL-10`, `FER-99` y `BRL-05`. |
| **CP-GI-02** | Números de tres o más dígitos | `MOL` con 99 y 100; `BRL` con 12345 | El formato no trunca el número | Devuelve `MOL-99`, `MOL-100` y `BRL-12345`. |
| **CP-GI-03** | Número no positivo | `MOL` con 0 y con -3 | Las secuencias solo entregan números mayores a 0 | Lanza `IllegalArgumentException` con el mensaje `El número del identificador debe ser mayor a 0`. |
| **CP-GI-04** | Prefijo vacío | `null` y `"  "` con el número 1 | El prefijo es obligatorio | Lanza `IllegalArgumentException` con el mensaje `El prefijo del identificador es obligatorio`. |
