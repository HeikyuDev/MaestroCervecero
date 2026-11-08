## Casos de prueba de la validación de la capacidad en los FormDTO de equipamiento y de barril

**Tipo de prueba:** unitaria (clase `CapacidadFormDTOsValidacionTest`), sin Spring ni mocks: usa el `Validator` de Bean Validation sobre `FermentadorFormDTO`, `MaceradorFormDTO`, `OllaHervorFormDTO` y `BarrilFormDTO`.

**Regla:** la capacidad total del fermentador, del macerador y de la olla de hervor, y la capacidad del barril, son obligatorias solo en el alta. Su `@NotNull` pertenece al grupo `GrupoAlta` (que extiende `Default`), que el Controller de alta valida con `@Validated(GrupoAlta.class)`. La modificación valida con `@Valid` (grupo por defecto): el formulario de edición no envía la capacidad porque no se puede cambiar. El resto de los campos son obligatorios en ambos casos.

---

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-FCA-01** | `FermentadorFormDTO` exige la capacidad total solo en el alta | DTO sin `capacidadTotal` y DTO completo | Validación con el grupo por defecto y con `GrupoAlta` | Sin `capacidadTotal`: sin violaciones con el grupo por defecto y una violación "La capacidad total es obligatoria" con `GrupoAlta`. Completo: sin violaciones. |
| **CP-FCA-02** | `MaceradorFormDTO` exige la capacidad total solo en el alta | DTO sin `capacidadTotal` y DTO completo | Validación con el grupo por defecto y con `GrupoAlta` | Sin `capacidadTotal`: sin violaciones con el grupo por defecto y una violación "La capacidad total es obligatoria" con `GrupoAlta`. Completo: sin violaciones. |
| **CP-FCA-03** | `OllaHervorFormDTO` exige la capacidad total solo en el alta | DTO sin `capacidadTotal` y DTO completo | Validación con el grupo por defecto y con `GrupoAlta` | Sin `capacidadTotal`: sin violaciones con el grupo por defecto y una violación "La capacidad total es obligatoria" con `GrupoAlta`. Completo: sin violaciones. |
| **CP-FCA-04** | `BarrilFormDTO` exige la capacidad solo en el alta y el resto de los campos siempre | DTO sin `capacidad` y DTO vacío | Validación con el grupo por defecto y con `GrupoAlta` | Sin `capacidad`: sin violaciones con el grupo por defecto y una violación "La capacidad es obligatoria" con `GrupoAlta`. Vacío con el grupo por defecto: "El número de serie es obligatorio", "Los usos antes de mantenimiento son obligatorios" y "El fabricante es obligatorio". |
