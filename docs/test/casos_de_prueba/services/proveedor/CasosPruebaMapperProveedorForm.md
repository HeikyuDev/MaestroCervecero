## Casos de prueba de `MapperProveedorForm`

**Tipo de prueba:** unitaria (clase `MapperProveedorFormTest`), sin Spring ni mocks. Verifica la conversión de la versión vigente de un proveedor (`ProveedorResponseDTO`) en el `ProveedorFormDTO` con el que se precarga el formulario de modificación.

**Regla de conversión:** los datos de la versión vigente pasan a los campos del formulario; el CUIT se muestra con guiones (`XX-XXXXXXXX-X`), igual que en el listado; la localidad se referencia por su id; cada ítem del catálogo se convierte en una fila que referencia al insumo y a la presentación comercial por su id (el id del ítem no viaja, porque el catálogo se vuelve a crear en cada versión).

---

### `toFormDTO(ProveedorResponseDTO)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MPF-01** | Datos de la versión vigente | Proveedor "Malterías del Sur S.A." (nombre comercial "Malterías del Sur", CUIT `30712345678`, teléfono `3584123456`, correo `ventas@malteriasdelsur.com`, dirección "Av. Italia 1234", localidad id 9) | La versión vigente se copia al formulario | El formulario tiene los mismos valores, con el CUIT "30-71234567-8" y `idLocalidad: 9`. |
| **CP-MPF-02** | Ítems del catálogo como filas | Catálogo con la malta id 11 en la presentación id 5 y el lúpulo id 21 en la presentación id 6 | Cada ítem referencia al insumo y a la presentación por id | El catálogo del formulario tiene 2 filas, en el mismo orden: (11, 5) y (21, 6). |
| **CP-MPF-03** | Proveedor nulo o sin versión | (a) proveedor nulo; (b) proveedor sin versión vigente | Casos límite | (a) Devuelve `null`; (b) devuelve un formulario vacío con la versión inicializada y el catálogo vacío. |
| **CP-MPF-04** | Ítem sin insumo o sin presentación | Catálogo con un ítem sin insumo (presentación id 5) y otro sin presentación (malta id 11) | Un dato faltante del ítem no impide la conversión | El catálogo del formulario tiene 2 filas: (`null`, 5) y (11, `null`). |
| **CP-MPF-05** | Catálogo nulo | La versión vigente tiene el catálogo en `null` | Caso límite | El catálogo del formulario está vacío (no es `null`) y el resto de los datos se copia igual. |
| **CP-MPF-06** | CUIT ya formateado | La versión vigente tiene el CUIT `30-71234567-8` | El formato no se aplica dos veces | El CUIT del formulario es "30-71234567-8". |
