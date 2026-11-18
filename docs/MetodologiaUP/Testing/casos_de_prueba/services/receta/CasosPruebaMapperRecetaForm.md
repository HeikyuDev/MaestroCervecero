## Casos de prueba de `MapperRecetaForm`

**Tipo de prueba:** unitaria (clase `MapperRecetaFormTest`), sin Spring ni mocks. Verifica la conversión de la versión vigente de una receta (`RecetaResponseDTO`) en el `RecetaFormDTO` con el que se precarga el formulario de modificación.

**Regla de conversión:** cada detalle de malta, lúpulo y levadura se convierte en una fila que referencia al insumo por su id; el tiempo de hervor solo se conserva en el uso `HERVOR` (para los demás usos la entidad lo guarda en 0 y el formulario lo deja vacío).

---

### `toFormDTO(RecetaResponseDTO)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MRF-01** | Datos generales y duraciones | Receta "Pale Ale": volumen base 20, relación de empaste 3, OG 1.05, FG 1.01, IBU 35.5, maceración 60 min, hervido 90 min, fermentación 10 días, maduración 4 días | La versión vigente se copia al formulario | El formulario tiene los mismos valores en cada campo. |
| **CP-MRF-02** | Detalles como filas | Una malta (id 11, 4.5 kg), dos lúpulos (Cascade id 21, 25 g, `HERVOR`, `HERVIDO`; Citra id 22) y una levadura (id 31, 11.5 g) | Cada detalle referencia al insumo por id | Cada lista del formulario tiene una fila por detalle, con el id del insumo, la cantidad y, en los lúpulos, el uso y la etapa de uso. |
| **CP-MRF-03** | Tiempo de hervor solo en Hervor | Cascade `HERVOR` con 60 min; Citra `DRY_HOP` (etapa `FERMENTACION`) con tiempo 0 en la entidad | El tiempo solo aplica al uso Hervor | Cascade conserva 60; Citra queda con el tiempo de hervor vacío (`null`) y conserva su etapa de uso. |
| **CP-MRF-05** | Receta nula o sin versión | (a) receta nula; (b) receta sin versión vigente | Casos límite | (a) Devuelve `null`; (b) devuelve un formulario vacío con las tres listas (maltas, lúpulos y levaduras) inicializadas y vacías. |
