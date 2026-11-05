## Casos de prueba de `MapperEnvasadoLote`

**Tipo de prueba:** unitaria (clase `MapperEnvasadoLoteTest`), sin Spring ni mocks. Verifica cómo se informa la anulación de un envasado en el `EnvasadoLoteResponseDTO`.

**Regla de conversión:** `fechaEnvasado` se copia de la entidad y `fechaRegistro` es la fecha de creación de la auditoría. `fechaAnulacion` y `motivoAnulacion` se copian de la entidad. `usuarioAnulacion` es el usuario de la última modificación de la auditoría (`lastModifiedBy`) y solo se informa si el envasado está `ANULADO`.

---

### `toDTO(EnvasadoLoteEntity)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-MEL-01** | Envasado anulado | Envasado `ANULADO` con `fechaAnulacion: 02/07/2027 09:30`, `motivoAnulacion: "Barril equivocado"` y `lastModifiedBy: "operario"` | Se informa quién, cuándo y por qué se anuló | El DTO tiene la misma fecha y motivo y `usuarioAnulacion: "operario"`. |
| **CP-MEL-02** | Envasado registrado | Envasado `REGISTRADO` con `lastModifiedBy: "operario"` | El usuario de la última modificación no es el de una anulación | `usuarioAnulacion` y `fechaAnulacion` son `null`. |
| **CP-MEL-03** | Envasado nulo | `envasadoLoteEntity: null` | La entrada no existe | Retorna `null`. |
| **CP-MEL-04** | Fecha y hora del envasado | Envasado con `fechaEnvasado: 01/07/2027 09:50` y `createdDate: 01/07/2027 10:15` | La fecha del envasado es un dato propio, distinto de la fecha de registro | El DTO tiene `fechaEnvasado: 01/07/2027 09:50` y `fechaRegistro: 01/07/2027 10:15`. |
