## Casos de prueba del orden y la agrupación de campos

**Tipo de prueba:** guarda de consistencia (JUnit 5 plano, clase `OrdenDeCamposTest`). No levanta Spring ni simula peticiones: lee del sistema de archivos las plantillas de `src/main/resources/templates`.

**Objetivo:** el formulario y el detalle del proveedor agrupan los datos por sentido (identificación, contacto, ubicación) y la dirección nunca comparte fila con el correo (regla "Orden y agrupación de campos" de `docs/EstandaresUI.md`).

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-ODC-01** | Orden del detalle del proveedor | `proveedor/proveedor-detalle.html` | Posición de cada dato | Razón social, CUIT, teléfono, correo, país, provincia y localidad aparecen en ese orden y la dirección va después de la localidad, en una fila propia (`lg:col-span-3`); el correo ocupa dos columnas junto al teléfono (`lg:col-span-2`). |
| **CP-ODC-02** | Orden del formulario del proveedor | `proveedor/proveedor-form.html` | Posición de cada campo | Razón social, CUIT, teléfono, correo y los selectores de ubicación aparecen en ese orden y la dirección va después de la ubicación, en una fila propia (`md:col-span-3`); el correo ocupa dos columnas junto al teléfono (`md:col-span-2`). |
