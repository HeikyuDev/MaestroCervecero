## Casos de prueba de la ubicación enlazada

**Tipo de prueba:** guarda de consistencia (JUnit 5 plano, clase `UbicacionEnlazadaTest`). No levanta Spring ni simula peticiones: lee del sistema de archivos las plantillas de `src/main/resources/templates`.

**Objetivo:** país, provincia y localidad de un formulario son tres selectores de entidad enlazados (fragmento `fragments/ubicacion-enlazada` y `/js/ubicacion-enlazada.js`), cada uno con su buscador en modal, y los botones "Elegir" de los buscadores exponen los datos del padre para el autocompletado.

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-UEN-01** | El fragmento arma tres selectores de entidad | `fragments/ubicacion-enlazada.html` | Contenido del fragmento | Tiene el bloque `data-ubicacion-enlazada` con los contenedores `pais`, `provincia` y `localidad`, los tres buscadores de ubicaciones, exactamente tres usos de `fragments/selector-entidad :: selector(` y ningún `<select`. |
| **CP-UEN-02** | El proveedor usa el fragmento | `proveedor/proveedor-form.html` y `proveedor/proveedor-lista.html` | Uso del fragmento | Ambas invocan `fragments/ubicacion-enlazada :: ubicacion(`, cargan `/js/ubicacion-enlazada.js` y no tienen desplegables de ubicación. |
| **CP-UEN-03** | Los buscadores exponen los datos del padre | `ubicacion/buscador-pais.html`, `buscador-provincia.html` y `buscador-localidad.html` | Atributos del botón "Elegir" | País: `data-nombre`. Provincia: `data-nombre`, `data-id-pais` y `data-texto-pais`. Localidad: `data-nombre`, `data-id-provincia`, `data-texto-provincia`, `data-id-pais` y `data-texto-pais`. |
