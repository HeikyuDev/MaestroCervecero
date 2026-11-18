## Casos de prueba de `ControllerAdvices` (violaciones de integridad de datos, valores inválidos de la URL y origen igual a la página que falló)

**Tipo de prueba:** capa web (`MockMvc` en modo `standalone`, clase `ControllerAdvicesTest`) con un controller de prueba que lanza la excepción. No hay Spring Security, plantillas ni base de datos. Verifica el handler de red de seguridad para `DataIntegrityViolationException` y el de `MethodArgumentTypeMismatchException`; los demás handlers se cubren en los tests de cada controller.

**Regla:** el handler traduce la excepción en los atributos flash `mensaje` y `tipo` (el mismo contrato que consume el layout) y redirige al origen de la petición (cabecera `Referer`) solo si es del mismo sitio (esquema, servidor y puerto del pedido) y lo convierte en una ruta relativa con su consulta, sin fragmento; con un `Referer` ausente, mal formado, de otro sitio, con credenciales o de otro puerto, vuelve a la raíz del sitio. La redirección no expande las llaves de la ruta como variables de plantilla. Se aplica a una violación de integridad que ningún Service tradujo a una excepción de negocio (un índice único sin mensaje asociado o una clave foránea). La excepción deja registrada en el log la causa técnica, que no se muestra al usuario.

---

### Violación de integridad de datos

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CA-01** | Violación de integridad con origen conocido | El controller lanza `DataIntegrityViolationException`; la petición trae `Referer: http://localhost/clientes/nuevo` (mismo sitio) | Hay un handler específico, distinto del de errores inesperados | Redirige (3xx) a `/clientes/nuevo` con el mensaje "No se pudo guardar porque los datos entran en conflicto con otro registro. Revise la información e intente nuevamente" y tipo `danger`. |
| **CP-CA-02** | Violación de integridad sin `Referer` | El controller lanza `DataIntegrityViolationException`; la petición no trae `Referer` | No hay origen al que volver | Redirige (3xx) a `/` con el mismo mensaje. |
| **CP-CA-03** | Excepción cualquiera | El controller lanza `IllegalStateException` | El handler nuevo no captura excepciones ajenas | Redirige (3xx) con el mensaje "Ocurrió un error inesperado. Intente nuevamente". |

---

### Valor de la URL que no se puede convertir

Un valor que no se convierte al tipo del parámetro (`/lotes/abc` con un id numérico, `?pagina=xyz` con un entero) es un error del pedido, no del sistema: se registra como aviso (no como error) y no se muestra como "error inesperado".

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CA-04** | Valor inválido en una página | `GET /pagina/abc` con un parámetro numérico, con `Referer: http://localhost/lotes`; y por separado sin `Referer` | Una ruta de página vuelve al origen con un aviso | Redirige (3xx) a `/lotes` (sin `Referer`, a `/`) con la alerta `warning` "El valor indicado no es válido". |
| **CP-CA-05** | Valor inválido en un buscador en modal | `GET /lotes/buscador/molinos?pagina=xyz` y `GET /lotes/filtros/recetas?pagina=xyz`, con `Referer` | Un modal no puede recibir una página completa | Responde 400 (no redirige) con el texto "El valor indicado no es válido" (`text/plain`, UTF-8), que el modal muestra dentro de sí. |
| **CP-CA-06** | Rechazos en un buscador en modal | `GET` a rutas con `/buscador` en las que el controller lanza, por separado, `RecursoNoEncontradoException`, `ReglaNegocioException` y una excepción inesperada, con `Referer` de la pantalla de origen (el rechazo de permisos sigue redirigiendo al origen con su alerta) | Un modal no puede recibir una página completa: una redirección al origen la mostraría como "no se pudo cargar la búsqueda" sin dar el motivo | Responde 404, 422 y 500 respectivamente (no redirige) con el motivo como texto (`text/plain`, UTF-8): "No se encontró el lote con ID: 9", "La etapa no admite consumos" y "Ocurrió un error inesperado. Intente nuevamente". |
| **CP-CA-07** | `Referer` de otro sitio o inválido | El controller lanza `DataIntegrityViolationException`; la petición trae, por separado, `Referer` de `http://sitio-malicioso.example/robo`, de `https://localhost/clientes` (otro esquema), de `http://localhost:9999/clientes` (otro puerto), con credenciales (`http://usuario@localhost/clientes`), `//sitio-malicioso.example/robo`, `javascript:alert(1)` y `no es una url` | La cabecera la controla el cliente y no puede usarse para redirigir a otro sitio | En todos los casos redirige (3xx) a `/` con el mensaje de conflicto. |
| **CP-CA-08** | `Referer` del mismo sitio | La petición trae `Referer: http://localhost/lotes?pagina=2&estado=PENDIENTE#fin` y, por separado, `http://localhost:80/clientes` y la ruta relativa `/lotes?pagina=1` | El `Referer` del mismo sitio se convierte en ruta relativa | Redirige (3xx) a `/lotes?pagina=2&estado=PENDIENTE` (sin el fragmento) a `/clientes` y a `/lotes?pagina=1`. |
| **CP-CA-09** | Llaves en la ruta del `Referer` | La petición trae `Referer: http://localhost/lotes/%7Bid%7D?filtro=%7Bx%7D` y, por separado, una ruta con llaves sin codificar | Las llaves no se expanden como variables de plantilla | Redirige (3xx) a `/lotes/%7Bid%7D?filtro=%7Bx%7D` tal cual; la ruta con llaves sin codificar no es una URL válida y vuelve a la raíz sin lanzar error. |

---

### Origen igual a la página que falló

Si un `GET` falla y su `Referer` es la misma ruta y consulta pedida (por ejemplo, la pantalla de edición de un registro que otro usuario dio de baja mientras el formulario estaba abierto: el envío vuelve al formulario y el formulario vuelve a fallar), redirigir al origen repetiría el mismo rechazo sin fin (`ERR_TOO_MANY_REDIRECTS`). En ese caso se redirige a la ruta padre, la que resulta de quitar el sufijo `/{id}/editar` o `/{id}` (con un identificador numérico), con la misma alerta; si la ruta no termina en un identificador, a la raíz. Un `POST` no entra en esta regla: la redirección lo convierte en un `GET`, que es otra petición.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CA-10** | Pantalla de edición de un registro inexistente | `GET /proveedores/5/editar` y, por separado, `GET /equipamiento/fermentadores/3/editar`; el controller lanza `RecursoNoEncontradoException` y el `Referer` es la misma pantalla | El origen es la misma página que falló | Redirige (3xx) a `/proveedores` y a `/equipamiento/fermentadores` respectivamente, con el mensaje de la excepción y tipo `danger`. |
| **CP-CA-11** | Pantalla de detalle de un registro inexistente | `GET /recetas/7` con `Referer: /recetas/7`; el controller lanza `RecursoNoEncontradoException` | El origen es la misma página que falló | Redirige (3xx) a `/recetas` con el mensaje de la excepción y tipo `danger`. |
| **CP-CA-12** | Misma ruta sin identificador al final | `GET /tablero` con `Referer: http://localhost/tablero` (el controller lanza `RecursoNoEncontradoException`) y `GET /listado?pagina=abc` con `Referer: http://localhost/listado?pagina=abc` (valor que no se convierte) | No hay una ruta padre | Redirige (3xx) a `/` con la alerta de cada caso (`danger` y `warning`). |
| **CP-CA-13** | Origen distinto de la petición | `GET /proveedores/5/editar` con `Referer: http://localhost/proveedores?pagina=2`; `POST /proveedores/5` con `Referer: http://localhost/proveedores/5/editar`; `POST /recetas/7` con `Referer: http://localhost/recetas/7`; `GET /listado?pagina=abc` con `Referer: http://localhost/listado?pagina=2` | El origen no es la misma página pedida con `GET` | Redirige (3xx) al origen: `/proveedores?pagina=2`, `/proveedores/5/editar`, `/recetas/7` y `/listado?pagina=2`. |
