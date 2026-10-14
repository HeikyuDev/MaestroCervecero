## Casos de prueba de `ControllerAdvices` (violaciones de integridad de datos y valores inválidos de la URL)

**Tipo de prueba:** capa web (`MockMvc` en modo `standalone`, clase `ControllerAdvicesTest`) con un controller de prueba que lanza la excepción. No hay Spring Security, plantillas ni base de datos. Verifica el handler de red de seguridad para `DataIntegrityViolationException` y el de `MethodArgumentTypeMismatchException`; los demás handlers se cubren en los tests de cada controller.

**Regla:** el handler traduce la excepción en los atributos flash `mensaje` y `tipo` (el mismo contrato que consume el layout) y redirige al origen de la petición (cabecera `Referer`), con la raíz del sitio como alternativa. Se aplica a una violación de integridad que ningún Service tradujo a una excepción de negocio (un índice único sin mensaje asociado o una clave foránea). La excepción deja registrada en el log la causa técnica, que no se muestra al usuario.

---

### Violación de integridad de datos

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CA-01** | Violación de integridad con origen conocido | El controller lanza `DataIntegrityViolationException`; la petición trae `Referer: http://localhost/clientes/nuevo` | Hay un handler específico, distinto del de errores inesperados | Redirige (3xx) a `http://localhost/clientes/nuevo` con el mensaje "No se pudo guardar porque los datos entran en conflicto con otro registro. Revise la información e intente nuevamente" y tipo `danger`. |
| **CP-CA-02** | Violación de integridad sin `Referer` | El controller lanza `DataIntegrityViolationException`; la petición no trae `Referer` | No hay origen al que volver | Redirige (3xx) a `/` con el mismo mensaje. |
| **CP-CA-03** | Excepción cualquiera | El controller lanza `IllegalStateException` | El handler nuevo no captura excepciones ajenas | Redirige (3xx) con el mensaje "Ocurrió un error inesperado. Intente nuevamente". |

---

### Valor de la URL que no se puede convertir

Un valor que no se convierte al tipo del parámetro (`/lotes/abc` con un id numérico, `?pagina=xyz` con un entero) es un error del pedido, no del sistema: se registra como aviso (no como error) y no se muestra como "error inesperado".

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CA-04** | Valor inválido en una página | `GET /pagina/abc` con un parámetro numérico, con `Referer: http://localhost/lotes`; y por separado sin `Referer` | Una ruta de página vuelve al origen con un aviso | Redirige (3xx) a `http://localhost/lotes` (sin `Referer`, a `/`) con la alerta `warning` "El valor indicado no es válido". |
| **CP-CA-05** | Valor inválido en un buscador en modal | `GET /lotes/buscador/molinos?pagina=xyz` y `GET /lotes/filtros/fermentadores?pagina=xyz`, con `Referer` | Un modal no puede recibir una página completa | Responde 400 (no redirige) con el texto "El valor indicado no es válido" (`text/plain`, UTF-8), que el modal muestra dentro de sí. |
