## Casos de prueba de MensajeConLista

**Tipo de prueba:** unitaria (JUnit 5 plano, clase `MensajeConListaTest`). No levanta Spring ni usa base de datos.

**Regla:** un mensaje que informa varios elementos a la vez (insumos con stock insuficiente, insumos que no alcanzan el porcentaje mínimo, avisos de una consulta) se arma como una línea de título seguida de un elemento por línea, separadas por un salto de línea. La alerta del layout lo muestra como título y lista con viñetas (ver `CasosPruebaAlertaMensaje.md`).

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MCL-01** | Un elemento | Título "No hay stock suficiente para iniciar el lote:" y un elemento "Malta Pilsen: faltan 12,5 kg (disponible 7,5 kg, necesario 20 kg)" | El título y el elemento van en líneas separadas | El mensaje es el título, un salto de línea y el elemento. |
| **CP-MCL-02** | Varios elementos | Título y tres elementos | Un elemento por línea, en el orden dado | El mensaje tiene cuatro líneas; `lineas` devuelve el título y los tres elementos en ese orden. |
| **CP-MCL-03** | Sin elementos | Lista vacía o nula | No hay nada que listar | El mensaje es solo el título. |
| **CP-MCL-04** | Avisos de una consulta | Un aviso; dos avisos | Un aviso solo no necesita título | Con un aviso, el mensaje es ese aviso tal cual. Con varios, el título "Se ajustó la consulta:" y un aviso por línea. |
| **CP-MCL-05** | Normalización de líneas | Mensaje con espacios, saltos de línea de Windows y líneas vacías; mensaje nulo | Las líneas vacías no se muestran | `lineas` devuelve las líneas sin espacios sobrantes ni vacías; con `null` devuelve una lista vacía. |
