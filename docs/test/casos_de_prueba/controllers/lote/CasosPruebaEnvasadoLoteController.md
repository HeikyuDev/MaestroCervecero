## Casos de prueba de `EnvasadoLoteController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `EnvasadoLoteControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; los services (`ILoteServicio`, `IEnvasadoLoteServicio`, `IBarrilServicio` y `IFermentadorServicio`) están simulados (mock) y no hay base de datos. Las reglas del envasado (barril disponible, capacidad del barril, capacidad útil del fermentador, anulación) se prueban aparte, en `services/lote/CasosPruebaEnvasadoLote.md`; el detalle del lote, que muestra los envasados en solo lectura y finaliza el Envasado, en `CasosPruebaLoteController.md`.

**Rutas y roles requeridos:** la pantalla cuelga de un lote: `/lotes/{idLote}/envasados`. Cada método lleva su `@PreAuthorize("hasAuthority('<PERMISO>')")`:

| Endpoint | Permiso | Roles que lo tienen |
| --- | --- | --- |
| `GET /lotes/{idLote}/envasados` | `ENVASADO_CONSULTAR` | `OPERARIO_DE_PRODUCCION` |
| `POST /lotes/{idLote}/envasados` | `ENVASADO_REGISTRAR` | `OPERARIO_DE_PRODUCCION` |
| `GET /lotes/{idLote}/envasados/buscador/barriles` | `ENVASADO_REGISTRAR` | `OPERARIO_DE_PRODUCCION` |
| `POST /lotes/{idLote}/envasados/{idEnvasado}/anular` | `ENVASADO_ANULAR` | `OPERARIO_DE_PRODUCCION` |

Cualquier otro rol (o un rol con el permiso de otra acción) es rechazado: redirige con alerta `danger` y no llega a ningún service. El `GERENTE_DE_PRODUCCION` ve los envasados en el detalle del lote y no abre esta pantalla de operación.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout. El rechazo por rol lo traduce el `ControllerAdvices` a un redirect con la alerta `danger` "No tiene permisos para realizar esta acción"; no es una respuesta 403. Un POST sin token CSRF es rechazado (3xx) y no llega al service.
- **Escenario base.** Lote `12` "PALE_ALE-R7-3" `EN_EJECUCION`, volumen objetivo 300 L, con la etapa de Envasado `EN_CURSO` (id de etapa 36) y el fermentador "F-01" (capacidad útil 330 L). Envasados de la etapa: `7` ("B-001", 50 L), `8` ("B-002", 50 L) y `9` ("B-003", 20 L), todos `REGISTRADO` (total envasado 120 L), y `10` ("B-004", 30 L, `ANULADO` con motivo "Barril equivocado"). Rol `OPERARIO_DE_PRODUCCION`. Cada caso cambia solo lo que dice.
- La etapa se resuelve a partir del lote consultado: el controller busca el lote, toma de él la etapa de Envasado y usa su id (`idEtapaLote`) en los services; ese id nunca viaja en un formulario.
- **Pantalla de envasados (`GET`).** Tiene el encabezado (“← Volver al lote”, código del lote y estado de la etapa), el indicador del envasado, el formulario de registro y la tabla de envasados. No es un modal: es una página aparte, y todas sus acciones vuelven a ella.
- **Indicador.** `data-indicador-envasado` dice "120 L de 330 L (capacidad útil del fermentador F-01)", con una barra de progreso (`role="progressbar"`, `aria-valuenow` igual al porcentaje, tope 100) y, debajo, "Volumen objetivo: 300 L" y "Quedan 210 L". El único tope del envasado es la capacidad útil del fermentador (total envasado registrado acumulado): se puede envasar más que el volumen objetivo, y superarlo es un hecho informativo, no un error.
- **Después de registrar o anular se queda en la misma pantalla.** Es un Post-Redirect-Get a `/lotes/{idLote}/envasados` con la alerta de éxito.
- **Errores.** Un error de validación (`@Valid`, o un número que no se puede convertir) vuelve a mostrar la misma pantalla (200, sin redirigir) con el resumen de errores y lo que el usuario había cargado (la cantidad y el barril elegido). Un rechazo del service (`ReglaNegocioException`, alerta `warning`) o un recurso inexistente (`RecursoNoEncontradoException`, alerta `danger`) vuelve al origen (header `Referer`, que es esta misma pantalla; sin él, `/`) mostrando el mensaje del service.
- **Pantalla de solo lectura.** Si el lote no está `EN_EJECUCION` o la etapa de Envasado no está `EN_CURSO`, la pantalla se muestra igual (se pueden consultar los envasados) con el aviso "La etapa de Envasado no está en curso, solo se pueden consultar los envasados" y sin el formulario de registro ni botones "Anular".
- **Tabla de envasados.** Columnas "Barril", "Cantidad (L)", "Estado" y "Acciones"; páginas de 20, el último registrado arriba (ordenamiento por `id` descendente). Los envasados anulados se muestran atenuados, con su motivo y sin botón "Anular". "Anular" abre el diálogo de motivo (`fragments/dialogo-motivo`, campo `motivo` obligatorio validado por el servidor) con `data-motivo-url` y `data-motivo-nombre`.
- **Formulario.** El barril se elige con el selector de entidad (modal; solo barriles Disponibles) y se escribe la cantidad en litros. Los campos llevan asterisco rojo y placeholder de ejemplo; el formulario es `novalidate` y el JavaScript no valida. La pantalla no tiene botón de finalizar: el Envasado se finaliza desde el detalle del lote.
- **Pertenencia.** Anular valida que el envasado pertenezca a la etapa de Envasado del lote de la ruta; si no, no llama al service de anulación.
- Formato: números sin ceros sobrantes (`120.0` se ve "120") y con coma decimal.

---

### 1. `GET /lotes/{idLote}/envasados` — `mostrarEnvasados(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LEN-01** | Pantalla de envasados | Escenario base, `GET /lotes/12/envasados` | El rol está autorizado | Responde 200 con la vista `lote/envasado-lista`. Muestra el título "Envasados del lote PALE_ALE-R7-3", el link "Volver al lote" hacia `/lotes/12?etapa=envasado`, el estado de la etapa "En curso", el indicador, el formulario de registro y la tabla de envasados. No usa ningún diálogo modal para registrar. |
| **CP-LEN-02** | Indicador de capacidad útil | Escenario base | Se informa cuánto se envasó y cuánto se puede | `data-indicador-envasado` dice "120 L de 330 L (capacidad útil del fermentador F-01)", "Volumen objetivo: 300 L" y "Quedan 210 L"; la barra tiene `aria-valuenow="36"`. |
| **CP-LEN-03** | Sin envasados | Total envasado 0; el service devuelve una página vacía | Se muestra el estado inicial | El indicador dice "0 L de 330 L (capacidad útil del fermentador F-01)" y "Quedan 330 L" con la barra en 0; la tabla muestra "No hay envasados registrados". |
| **CP-LEN-04** | Superó el volumen objetivo | Total envasado 310 L (volumen objetivo 300 L, capacidad útil 330 L) | Superar el objetivo es informativo | El indicador dice "310 L de 330 L" y "Quedan 20 L", y se agrega el aviso "Superó el volumen objetivo por 10 L" en un estilo informativo (no de error). El formulario sigue visible. |
| **CP-LEN-05** | Capacidad útil alcanzada | Total envasado 330 L | La regla la sigue validando el service | El indicador dice "330 L de 330 L" y "Quedan 0 L" con el aviso "Se alcanzó la capacidad útil del fermentador"; el formulario sigue visible (el service rechazaría una cantidad nueva). |
| **CP-LEN-06** | Formulario de registro | Escenario base | El barril se elige con un buscador | Incluye un formulario `method="post"` hacia `/lotes/12/envasados` con CSRF y `novalidate`, el selector de entidad `idBarril` (`data-selector-buscar`, ícono `bi-list-ul`, `data-buscador-url="/lotes/12/envasados/buscador/barriles"`, placeholder "Seleccione barril", sin menú de alta y sin botón de quitar), el campo "Cantidad a envasar (L)" `cantidad` con el placeholder "Ej: 50", los asteriscos rojos y el botón "Registrar envasado". |
| **CP-LEN-07** | Tabla de envasados | Escenario base | Se lista lo registrado y lo anulado | Muestra, del más reciente al más antiguo, los envasados 10, 9, 8 y 7: el 10 atenuado con "B-004", "30", "Anulado", el motivo "Barril equivocado" y sin botón "Anular"; los otros con "B-003" "20", "B-002" "50" y "B-001" "50", "Registrado" y el botón "Anular". Pidió los envasados de la etapa 36 con ordenamiento `id` descendente, páginas de 20 y sin filtrar por barril ni por estado. |
| **CP-LEN-08** | Anular abre el diálogo de motivo | Envasado 9 `REGISTRADO` | La anulación pide un motivo | El botón "Anular" lleva `data-motivo-dialogo="dialogo-anular"`, `data-motivo-url` hacia `/lotes/12/envasados/9/anular` y `data-motivo-nombre="B-003"`; la página incluye el diálogo `dialogo-anular` ("Anular envasado") con el campo `motivo`, el placeholder "Ej: Se eligió el barril equivocado", el asterisco rojo y el token CSRF. |
| **CP-LEN-09** | Paginación de los envasados | 45 envasados; `pagina: 1` | Hay páginas anterior y siguiente | Muestra "Página 2 de 3" y los links `pagina=0` y `pagina=2` hacia `/lotes/12/envasados`. |
| **CP-LEN-10** | Alertas del Post-Redirect-Get | Atributos flash `mensaje` ("Envasado registrado correctamente") y `tipo` `success` | El layout muestra la alerta | La página incluye el mensaje con el estilo de éxito. |
| **CP-LEN-11** | Etapa de Envasado que no está en curso | Lote `EN_EJECUCION` con Hervido `EN_CURSO` y Envasado `PENDIENTE` | Solo se registra con el Envasado en curso | Responde 200 con el aviso "La etapa de Envasado no está en curso, solo se pueden consultar los envasados"; no hay formulario de registro, selector ni botones "Anular". El indicador y la tabla se ven. |
| **CP-LEN-12** | Lote cerrado | Lote `FINALIZADO` (y por separado `CANCELADO`) | Un lote cerrado se consulta sin operar | Responde 200 con el aviso de solo lectura y sin formulario ni botones "Anular". |
| **CP-LEN-13** | Lote inexistente | `GET /lotes/99/envasados`. El service lanza `RecursoNoEncontradoException("No se encontró el lote con ID: 99")`, con `Referer: /lotes` | El recurso no existe | Redirige al origen (`/lotes`) con la alerta `danger` y el mensaje del service. |
| **CP-LEN-14** | Datos pedidos a los services | Escenario base | La pantalla no pide más datos de los que muestra | Consulta el lote una vez, el resumen del envasado de la etapa 36 una vez, los envasados de la etapa una vez y no consulta barriles ni fermentadores uno por uno. |
| **CP-LEN-15** | Roles no autorizados | Roles `GERENTE_DE_PRODUCCION`, `ENCARGADO_DE_DEPOSITO`, `GERENTE_DE_COMPRAS`, `GERENTE_COMERCIAL` y `ADMINISTRADOR` | `@PreAuthorize("hasAuthority('ENVASADO_CONSULTAR')")` → **Rechaza** | Cada pedido redirige con alerta de tipo `danger`. No se invoca ningún service. |

---

### 2. `POST /lotes/{idLote}/envasados` — `registrarEnvasado(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LEN-16** | Envasado correcto | `idBarril: 3`, `cantidad: 50`, con CSRF. El service devuelve el envasado | El service acepta el envasado | Llama a `registrarEnvasadoLote(36, ...)` con el DTO enlazado (barril 3, 50.0 L) y redirige a `/lotes/12/envasados` con la alerta de éxito "Envasado registrado correctamente". Se queda en la misma pantalla. |
| **CP-LEN-17** | Formulario vacío | Sin parámetros | Bean Validation: barril y cantidad son obligatorios | Responde 200 con la pantalla de envasados (sin redirigir), el resumen "No se pudo registrar el envasado" y los mensajes "El barril es obligatorio" y "La cantidad a envasar es obligatoria"; el indicador y la tabla se muestran completos. No invoca al service de registro. |
| **CP-LEN-18** | Cantidad no numérica | `idBarril: 3`, `cantidad: "abc"`. El service de barriles devuelve "B-001" para el id 3 | El valor no se puede convertir | Responde 200 con la pantalla y el error "Ingrese un número válido" en la cantidad; conserva "abc" y el barril elegido ("B-001" con `idBarril` oculto en 3). No invoca al service de registro. |
| **CP-LEN-19** | Falta el barril | `cantidad: 50`, sin `idBarril` | Bean Validation | Responde 200 con la pantalla, "El barril es obligatorio" debajo del selector y la cantidad "50" conservada. |
| **CP-LEN-20** | Cantidad no válida para el service | (a) `cantidad: 0`; (b) `-5`; (c) `NaN`; (d) `Infinity` | Bean Validation solo exige presencia; los valores inválidos los rechaza el service | En los cuatro casos llama al service con el valor recibido y, al lanzar `ReglaNegocioException("La cantidad a envasar debe ser mayor a 0")` (o el mensaje correspondiente), redirige al origen con la alerta `warning` y ese mensaje. |
| **CP-LEN-21** | Barril no disponible | El service lanza `ReglaNegocioException("El barril debe encontrarse en estado operativo Disponible para poder envasar en él")` | El service es la fuente de verdad | Redirige al origen con la alerta `warning` y ese mensaje. |
| **CP-LEN-22** | Cantidad mayor a la capacidad del barril | `cantidad: 80` en un barril de 50 L. El service lanza `ReglaNegocioException("La cantidad a envasar no puede superar la capacidad del barril seleccionado (50 L)")` | La capacidad la valida el service | Redirige al origen con la alerta `warning` y ese mensaje. |
| **CP-LEN-23** | Cantidad mayor a la capacidad útil del fermentador | Total envasado 320 L, `cantidad: 20`. El service lanza `ReglaNegocioException("La cantidad a envasar supera la capacidad útil del fermentador (quedan 10 L disponibles para envasar)")` | El único tope lo valida el service | Redirige al origen con la alerta `warning` y ese mensaje. |
| **CP-LEN-24** | Lote o etapa que no admiten el envasado | El service lanza `ReglaNegocioException("El lote debe encontrarse en estado En ejecución para poder registrar un envasado")`; y por separado `"La etapa debe estar en curso para poder registrar un envasado"` | El service es la fuente de verdad | Redirige al origen con la alerta `warning` y el mensaje del service. |
| **CP-LEN-25** | Barril inexistente | `idBarril: 99`. El service lanza `RecursoNoEncontradoException("No se encontró el barril con ID: 99")` | El recurso no existe | Redirige al origen con la alerta `danger` y el mensaje del service. |
| **CP-LEN-26** | Sin token CSRF | Datos válidos sin `_csrf` | Spring Security exige el token | La petición es rechazada (3xx) y no llega al service. |
| **CP-LEN-27** | Roles no autorizados | Roles `GERENTE_DE_PRODUCCION`, `ENCARGADO_DE_DEPOSITO`, `GERENTE_DE_COMPRAS`, `GERENTE_COMERCIAL` y `ADMINISTRADOR`, con datos válidos y CSRF | `@PreAuthorize("hasAuthority('ENVASADO_REGISTRAR')")` → **Rechaza** | Cada pedido redirige con alerta de tipo `danger`. El service no se invoca. |

---

### 3. `POST /lotes/{idLote}/envasados/{idEnvasado}/anular` — `anularEnvasado(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LEN-28** | Anulación correcta | Envasado `9` de la etapa 36, `motivo: "Se eligió el barril equivocado"`, con CSRF | El service acepta la anulación | Llama a `anularEnvasadoLote(9, ...)` con el motivo (`AnularEnvasadoLoteFormDTO`) y redirige a `/lotes/12/envasados` con la alerta de éxito "Envasado anulado correctamente". Se queda en la misma pantalla. |
| **CP-LEN-29** | Sin motivo | Sin el parámetro `motivo`. El service lanza `ReglaNegocioException("El motivo de anulación es obligatorio")` | El controller no valida: el service es la fuente de verdad | Llama al service con el motivo `null` y redirige al origen con la alerta `warning` y ese mensaje. |
| **CP-LEN-30** | El barril ya no está intacto | El service lanza `ReglaNegocioException("No se puede anular el envasado porque el barril asociado ya no mantiene su contenido intacto")` | El service es la fuente de verdad | Redirige al origen con la alerta `warning` y ese mensaje. |
| **CP-LEN-31** | Envasado ya anulado | El service lanza `ReglaNegocioException("Solo se pueden anular envasados en estado Registrado")` | El service es la fuente de verdad | Redirige al origen con la alerta `warning` y ese mensaje. |
| **CP-LEN-32** | Lote que no está en ejecución | El service lanza `ReglaNegocioException("El lote debe encontrarse en estado En ejecución para poder anular un envasado")` | El service es la fuente de verdad | Redirige al origen con la alerta `warning` y ese mensaje. |
| **CP-LEN-33** | Envasado inexistente | `idEnvasado: 99`. El service lanza `RecursoNoEncontradoException("No se encontró el envasado de lote con ID: 99")` | El recurso no existe | Redirige al origen con la alerta `danger` y el mensaje del service. |
| **CP-LEN-34** | El envasado es de otro lote | `idEnvasado: 15` cuyo `etapaLote.id` es 66 (otro lote), en la ruta del lote 12 | La ruta y el envasado deben coincidir | Redirige a `/lotes/12/envasados` con la alerta `warning` "El envasado no pertenece a este lote". No invoca `anularEnvasadoLote`. |
| **CP-LEN-35** | Sin token CSRF | `POST .../envasados/9/anular` sin `_csrf` | Spring Security exige el token | La petición es rechazada (3xx) y no llega al service. |
| **CP-LEN-36** | Roles no autorizados | Roles `GERENTE_DE_PRODUCCION`, `ENCARGADO_DE_DEPOSITO`, `GERENTE_DE_COMPRAS`, `GERENTE_COMERCIAL` y `ADMINISTRADOR`, con CSRF | `@PreAuthorize("hasAuthority('ENVASADO_ANULAR')")` → **Rechaza** | Cada pedido redirige con alerta de tipo `danger`. El service no se invoca. |

---

### 4. `GET /lotes/{idLote}/envasados/buscador/barriles` — `buscarBarriles(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LEN-37** | Fragmento del buscador | El service devuelve los barriles "B-001" (id 3, 50 L, fabricante "Barrilitos SA", Disponible) y "B-002" (id 4, 30 L, Disponible) | El buscador devuelve un fragmento, no una página | Responde 200 con la vista `lote/buscador-barril :: buscador`, sin `<html`. Muestra "Buscar barril", el criterio "Identificador" con el placeholder "Ej: B-001" (formulario `data-modal-buscar` hacia `/lotes/12/envasados/buscador/barriles`), las columnas "Identificador", "Capacidad (L)", "Fabricante" y "Estado" y por fila el botón "Elegir" con `data-id` (3, 4), `data-texto="B-001"` y `data-capacidad` (50, 30). |
| **CP-LEN-38** | Solo barriles disponibles | Sin parámetros | Un barril no disponible no puede envasarse | Llama al service con el identificador `null`, capacidad `null`, estado operativo `DISPONIBLE`, fabricante `null` y un `Pageable` de 8 elementos con ordenamiento por `identificador` ascendente (los maestros van en orden alfabético). |
| **CP-LEN-39** | Criterio de identificador | `identificador: " b-001 "`; y por separado `identificador: "  "` | Un criterio en blanco equivale a no filtrar | Llama al service con `b-001` (recortado) y, en el otro caso, con `null`. El campo conserva lo escrito. |
| **CP-LEN-40** | Paginación de a 8 | 10 barriles disponibles; páginas 0 y 1 | Páginas de 8 | La primera página tiene 8 filas, "Página 1 de 2" y los links `data-modal-get="true"` con `pagina=1` e `identificador`; la segunda trae las 2 restantes y el link `pagina=0`. |
| **CP-LEN-41** | Sin resultados | El service devuelve una página vacía; y por separado `pagina: -3` | Mensaje claro; una página negativa equivale a la primera | Muestra "No se encontraron barriles disponibles con los criterios indicados"; con la página negativa responde 200 con la primera página. |
| **CP-LEN-42** | Lote inexistente | `GET /lotes/99/envasados/buscador/barriles`. El service lanza `RecursoNoEncontradoException` | Un modal no puede redirigir a otra página | Responde 200 con el fragmento y el mensaje del service, sin criterios ni filas. |
| **CP-LEN-43** | Roles no autorizados | Roles `GERENTE_DE_PRODUCCION`, `ENCARGADO_DE_DEPOSITO`, `GERENTE_DE_COMPRAS`, `GERENTE_COMERCIAL` y `ADMINISTRADOR` | `@PreAuthorize("hasAuthority('ENVASADO_REGISTRAR')")` → **Rechaza** | Cada pedido redirige con alerta de tipo `danger`. No se invoca ningún service. |

---

### 5. Transversales

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-LEN-44** | Registrar y anular son Post-Redirect-Get a la misma pantalla | Registro y anulación exitosos | Recargar la página no reenvía la acción | Ambos responden 3xx con `Location` igual a `/lotes/12/envasados` y la alerta de éxito en los atributos flash; ninguno responde 200. |
| **CP-LEN-45** | Rechazo sin `Referer` | `POST /lotes/12/envasados` sin header `Referer`; el service lanza `ReglaNegocioException` | No hay origen al que volver | Redirige a `/` con la alerta `warning` y el mensaje del service. |
| **CP-LEN-46** | Sin JavaScript | La pantalla del escenario base | Las acciones no dependen de scripts | El registro es un formulario `method="post"` con `action` y `_csrf`; el formulario es `novalidate` y no hay atributos `required`, `min` ni `pattern`. Solo el selector del barril (modal) y el diálogo de motivo de "Anular" usan JavaScript (`/js/modales.js`); ese comportamiento del navegador no se prueba aquí. |
| **CP-LEN-47** | Ayudas visuales de la pantalla | El escenario base | Marcas obligatorias y ejemplos | El selector y la cantidad llevan asterisco rojo y placeholder de ejemplo, la unidad (L) va en la etiqueta y en la cabecera de la columna, y la pantalla no contiene emojis ni mensajes con punto final. |
| **CP-LEN-48** | Matriz de permisos | Cada endpoint de este documento, con un usuario que tiene solo el permiso correspondiente y con otro que tiene todos los demás permisos de envasado | Cada método exige únicamente su permiso | El pedido procede solo con el permiso del endpoint; con cualquier otro redirige con alerta `danger` y no invoca al service. |
