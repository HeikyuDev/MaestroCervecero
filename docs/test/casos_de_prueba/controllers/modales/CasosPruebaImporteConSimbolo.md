## Casos de prueba de los importes con símbolo

**Tipo de prueba:** guarda de consistencia (JUnit 5 plano, clase `ImporteConSimboloTest`). Lee las plantillas y el JS de `src/main/resources`.

**Objetivo:** todo importe que se muestra lleva el símbolo "$" delante (regla "Importes" de `docs/EstandaresUI.md`).

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-IMS-01** | Sin importes sin símbolo | Todas las plantillas | Uso de `FormatoImporte` | Ninguna plantilla llama a `formatear`; las pantallas usan `conSimbolo`. |
| **CP-IMS-02** | Encabezados sin la unidad repetida | Costos del lote, orden de compra, ingresos, costos directos y `orden-compra-form.js` | Etiquetas y cálculo en pantalla | Ningún encabezado contiene "($)" ni "($ por litro)" y el cálculo de la orden escribe los importes como `$ 1.250,50`. |
