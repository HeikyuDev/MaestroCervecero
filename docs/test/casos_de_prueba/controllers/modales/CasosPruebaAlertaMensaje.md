## Casos de prueba del texto de las alertas

**Tipo de prueba:** renderizado de plantilla (JUnit 5 con el motor Thymeleaf, clase `AlertaMensajeTest`). No levanta Spring MVC: procesa el fragmento real `fragments/alerta-mensaje :: texto(...)`, que usan las cuatro alertas del layout (éxito, advertencia, error e información), tanto para los atributos flash como para los del modelo.

**Regla:** un mensaje de una sola línea se muestra como texto corrido. Un mensaje de varias líneas (ver `CasosPruebaMensajeConLista.md`) muestra la primera como título y las demás como una lista con viñetas. El texto siempre se escapa. La alerta completa dentro de una pantalla se verifica en `CP-LCT-07` y `CP-LCT-08`.

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-AMS-01** | Una sola línea | Mensaje "Lote iniciado correctamente" | Un mensaje sin saltos de línea no cambia | Se muestra el texto sin `<ul>` ni `data-alerta-con-lista`. |
| **CP-AMS-02** | Varias líneas | Título "No hay stock suficiente para iniciar el lote:" y dos insumos faltantes | La primera línea es el título y el resto, viñetas | El título está en el elemento `data-alerta-titulo` y cada insumo en su propio `<li>` (dos en total). |
| **CP-AMS-03** | Escape del texto | Segunda línea "<script>alert(1)</script>" | El texto no se interpreta como HTML | La salida no contiene `<script>` y sí `&lt;script&gt;`. |
