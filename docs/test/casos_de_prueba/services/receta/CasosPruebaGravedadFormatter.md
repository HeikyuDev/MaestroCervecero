## Casos de prueba de `GravedadFormatter`

**Tipo de prueba:** unitaria (clase `GravedadFormatterTest`), sin Spring ni mocks. Verifica la conversión entre el texto de cuatro dígitos con el que se ingresa la OG y la FG de una receta y el valor decimal que maneja el sistema.

**Regla de conversión:** el texto debe tener exactamente cuatro dígitos y equivale a la densidad con tres decimales (`1050` es 1.050). Cualquier otro formato se rechaza con `ParseException`, que Spring MVC informa como error de conversión del campo. El formatter se registra en `RecetaController` para los campos `version.ogObjetivo` y `version.fgObjetivo`; los rangos permitidos los valida el service.

---

### `parse(String, Locale)` y `print(Double, Locale)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-GRF-01** | Texto de cuatro dígitos válido | `"1050"`, `"1010"`, `"1000"`, `"1120"`, `"1055"` | El texto coincide con `\d{4}` | Devuelve 1.05, 1.01, 1.0, 1.12 y 1.055. |
| **CP-GRF-02** | Texto que no tiene cuatro dígitos | `"105"`, `"10500"`, `"1,050"`, `"1.050"`, `"abcd"`, `"10a0"`, `" 1050"`, `"1050 "`, `"-105"`, `""` y `null` | El texto no coincide con `\d{4}` | Lanza `ParseException` en todos los casos. |
| **CP-GRF-03** | Mostrar una densidad | `1.05`, `1.01`, `1.0`, `1.055` | La densidad se expresa en milésimas | Devuelve `"1050"`, `"1010"`, `"1000"` y `"1055"`. |
| **CP-GRF-04** | Mostrar una densidad nula | `null` | No hay valor que mostrar | Devuelve una cadena vacía. |
| **CP-GRF-05** | Conversión inversa | Todos los textos de `"1000"` a `"1200"` | `print(parse(texto)) == texto` | Cada texto vuelve a ser el mismo luego de convertirlo a densidad y mostrarlo. |
