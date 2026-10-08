## Casos de prueba de `PlanificacionProduccionController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `PlanificacionProduccionControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; los services (`IPlanificacionProduccionServicio`, `IRecetaServicio`, `ILoteServicio`) están simulados (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/planificacion_produccion/`; la geometría del diagrama de Gantt, en `CasosPruebaGanttPlanificacionBuilder.md`.

**Roles requeridos:** el listado (`GET /planificaciones-produccion`) y el estado actual (`GET /planificaciones-produccion/{id}`) los ven el `GERENTE_DE_PRODUCCION` y el `GERENTE_DE_COMPRAS` (`@PreAuthorize` a nivel de clase). Todo lo demás (alta, anulación, finalización, sugerencia de fecha y buscador de recetas) es solo del `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")` en cada método). Cualquier otro rol es rechazado.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout. Un rol no autorizado y un POST sin token CSRF redirigen con alerta `danger` y no llegan al service.
- Una planificación es un registro transaccional: no se modifica ni se da de baja. Solo admite el registro y dos cierres excepcionales (anulación y finalización forzada), ambos con un motivo.
- El listado se pide con páginas de 20 y la última planificación registrada arriba: el controller arma el `Pageable` con ordenamiento por `id` descendente (el id es autogenerado y creciente, así que equivale al orden de registro).
- Los estados se muestran con su nombre legible (`etiqueta` del enum): Pendiente, Finalizada y Anulada para la planificación; Pendiente, En ejecución, Finalizado y Cancelado para los lotes. Nunca la constante del enum.
- Las fechas se muestran como `dd/MM/yyyy` (las reales con hora, `dd/MM/yyyy HH:mm`) y viajan en el formulario en formato ISO (campos `type="date"`). Los litros se muestran sin ceros sobrantes (`600.0` se ve "600").
- Los botones Finalizar y Anular solo aparecen en las planificaciones Pendiente y solo para el `GERENTE_DE_PRODUCCION`. Abren un diálogo pequeño (`fragments/dialogo-motivo`, mismo mecanismo que el de baja) con un campo "Motivo" obligatorio, que envía un POST con el token CSRF. El servidor es el único que valida: un motivo vacío lo rechaza el service y el mensaje se muestra en la alerta de la página de origen.
- Un cierre correcto redirige al estado actual de la planificación (`/planificaciones-produccion/{id}`) con alerta de éxito. Un rechazo del service (`ReglaNegocioException`, alerta `warning`) o una planificación inexistente (`RecursoNoEncontradoException`, alerta `danger`) vuelve al origen (header `Referer`) mostrando el mensaje del service.
- El formulario de alta es una única página (`novalidate`, sin validación en JavaScript) con la sección "Datos de la planificación". La receta se elige con el selector de entidad (campo en blanco con ícono de lista que abre el buscador; sin menú de alta al vuelo). Las unidades van en las etiquetas, los campos tienen un placeholder de ejemplo y los obligatorios llevan asterisco rojo.
- Un error de validación (`@Valid`) o una regla del service vuelven a mostrar el formulario (sin redirigir) con lo que el usuario había cargado, incluida la receta elegida; los mensajes aparecen en el resumen de errores y, los de campo, debajo del campo.
- El botón "Sugerir fecha" está junto a la fecha de fin y nunca corre solo: el HTML no tiene disparadores automáticos (`onload`, `onchange`, `data-auto`). Al apretarlo, `planificacion-form.js` pide `GET /planificaciones-produccion/sugerencia-fecha` y muestra el fragmento devuelto; completa la fecha de fin solo si está vacía (o si la puso antes ese botón) y, si el usuario ya escribió una, ofrece "Usar esta fecha". Ese comportamiento del navegador no se prueba aquí.
- El estado actual muestra la cabecera, los contadores de lotes, el diagrama de Gantt y la tabla de lotes. Sin lotes, el diagrama y la tabla muestran "Todavía no hay lotes registrados para esta planificación".
- La sidebar muestra "Planificación de Producción" (ícono `bi-calendar-check`) en la sección Operación para el `GERENTE_DE_PRODUCCION` y el `GERENTE_DE_COMPRAS`.

---

### 1. `GET /planificaciones-produccion` — `listarPlanificaciones(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PPC-01** | Listado para el gerente de producción | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 planificación (`id: 7`, "Pale Ale", 600.0 L, del 01/07/2027 al 25/07/2027, `PENDIENTE`) | El rol está autorizado | Responde 200 con la vista `planificacion_produccion/planificacion-lista`. Muestra "Planificación de Producción", las columnas "Receta", "Litros a producir (L)", "Inicio estimado", "Fin estimado" y "Estado" (sin columna de lotes), los valores "Pale Ale", "600" (no `600.0`), "01/07/2027" y "25/07/2027", el estado "Pendiente" en azul (no la constante), el botón "Nueva planificación" (`/planificaciones-produccion/nueva`), y por fila el link "Ver estado actual" (`/planificaciones-produccion/7`, ícono `bi-eye`) y los botones Finalizar (`bi-check2-circle`) y Anular (`bi-x-circle`) con `data-motivo-url` y `data-motivo-nombre="Pale Ale"`. La tabla está en un contenedor `overflow-x-auto` y las celdas llevan `whitespace-nowrap`. |
| **CP-PPC-02** | Botones solo en pendientes | El service devuelve 3 planificaciones: 7 `PENDIENTE`, 8 `FINALIZADA`, 9 `ANULADA` | Finalizar y Anular solo proceden sobre una planificación pendiente | La 7 tiene los botones de cierre; la 8 y la 9 solo el link de estado actual y sus etiquetas "Finalizada" y "Anulada". No aparecen URLs `/8/finalizar`, `/8/anular`, `/9/finalizar` ni `/9/anular`. |
| **CP-PPC-03** | Paginación y orden por defecto | Rol `GERENTE_DE_PRODUCCION`, sin parámetros | Sin criterios | Llama al service con receta, estado y fecha en `null` y un `Pageable` de 20 elementos, página 0 y ordenamiento `id` descendente (la última registrada primero). |
| **CP-PPC-04** | Filtros hacia el service | `idReceta: 3`, `estado: PENDIENTE`, `fechaInicio: 2027-07-01`, `pagina: 1`. El service de recetas devuelve "Pale Ale" para el id 3 | Los tres criterios son opcionales y se combinan | Llama al service con `3`, `PENDIENTE`, `2027-07-01` y la página 1. El listado muestra la receta elegida ("Pale Ale") en el selector con `idReceta` oculto en 3, la fecha en el campo y "Pendiente" seleccionado en el estado. |
| **CP-PPC-05** | Criterios vacíos y estados legibles | `idReceta`, `estado` y `fechaInicio` enviados vacíos | Un criterio vacío equivale a no filtrar | Llama al service con los tres en `null`. El filtro de estado ofrece "Todos", "Pendiente", "Finalizada" y "Anulada" (no las constantes). |
| **CP-PPC-06** | Filtro de receta con selector | Rol `GERENTE_DE_PRODUCCION` | Elegir una entidad relacionada es un buscador, no un combo | El filtro es un campo en blanco con ícono `bi-list-ul`, `data-buscador-url="/planificaciones-produccion/buscador/recetas"`, placeholder "Seleccionar receta" y botón para quitar la selección; sin menú de alta. El filtro de fecha lleva el placeholder "Ej: 01/07/2027". |
| **CP-PPC-07** | La paginación conserva los filtros | `idReceta: 3`, `estado: PENDIENTE`, `pagina: 1`. El service devuelve la página 2 de 3 (45 planificaciones) | Hay páginas anterior y siguiente | Muestra "Página 2 de 3" y los links de anterior (`pagina=0`) y siguiente (`pagina=2`) con `idReceta=3` y `estado=PENDIENTE`. |
| **CP-PPC-08** | Listado sin resultados | El service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron planificaciones de producción con los criterios indicados." en lugar de una tabla vacía. |
| **CP-PPC-09** | Diálogos de motivo | Rol `GERENTE_DE_PRODUCCION`, 1 planificación pendiente | Finalizar y Anular piden un motivo | Incluye los diálogos `dialogo-finalizar` y `dialogo-anular` ("Finalizar planificación", "Anular planificación"), con el campo `motivo`, el placeholder "Ej: Se canceló el pedido del cliente", el asterisco rojo, los botones de cancelar (`data-cerrar-dialogo`) y el token CSRF (`_csrf`) en el formulario. |
| **CP-PPC-10** | Gerente de compras solo consulta | Rol `GERENTE_DE_COMPRAS`, 1 planificación pendiente | El gerente de compras no gestiona planificaciones | Responde 200 con el listado y el link de estado actual, pero sin "Nueva planificación", sin `data-motivo-url` y sin los diálogos de finalizar ni anular. |
| **CP-PPC-11** | Rol no autorizado | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-PPC-12** | Administrador rechazado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-PPC-13** | Sidebar del gerente de producción | Rol `GERENTE_DE_PRODUCCION` | La planificación es operación diaria | "Planificación de Producción" (`bi-calendar-check`) aparece en la sección Operación, después de "Control de Lotes" y antes de Administración. |
| **CP-PPC-14** | Sidebar del gerente de compras | Rol `GERENTE_DE_COMPRAS` | El gerente de compras la consulta | La opción (`/planificaciones-produccion`) aparece en Operación, antes de "Órdenes de Compra", y no aparece "Control de Lotes". |

---

### 2. `GET /planificaciones-produccion/nueva` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PPC-15** | Formulario de alta | Rol `GERENTE_DE_PRODUCCION` | El rol está autorizado | Responde 200 con la vista `planificacion_produccion/planificacion-form`. Muestra "Nueva planificación", `action="/planificaciones-produccion"`, `novalidate`, la sección "Datos de la planificación", las etiquetas "Cantidad a producir (litros)", "Fecha de inicio estimada" y "Fecha de fin estimada", los placeholders "Seleccionar receta", "Ej: 600", "Ej: 01/07/2027" y "Ej: 25/07/2027", campos `type="date"`, el asterisco rojo de obligatorio, el botón "Registrar planificación", el link de cancelar y el token CSRF. |
| **CP-PPC-16** | La receta se elige con el buscador | Rol `GERENTE_DE_PRODUCCION` | Elegir una entidad relacionada es un modal con filtros | El campo `idReceta` es el selector de entidad (`data-selector-buscar`, ícono `bi-list-ul`, `data-buscador-url="/planificaciones-produccion/buscador/recetas"`), sin menú de tres puntitos, sin alta al vuelo y sin botón de quitar. |
| **CP-PPC-17** | Botón "Sugerir fecha" que no corre solo | Rol `GERENTE_DE_PRODUCCION` | La sugerencia la pide el usuario | Junto a la fecha de fin hay un `<button type="button" data-sugerir-fecha>` con el ícono `bi-magic`, el texto "Sugerir fecha", `whitespace-nowrap`, `data-sugerencia-url="/planificaciones-produccion/sugerencia-fecha"` y, debajo, el área `data-sugerencia-destino`. El HTML no contiene `data-auto`, `onload` ni `onchange`; carga `/js/planificacion-form.js`. |
| **CP-PPC-18** | Rol no autorizado | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 3. `POST /planificaciones-produccion` — `registrarPlanificacion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PPC-19** | Alta válida | `idReceta: 3`, `cantidadAProducir: 600`, `fechaInicioEstimada: 2027-07-01`, `fechaFinalizacionEstimada: 2027-07-25`, con token CSRF | El formulario es válido | Llama al service con el DTO enlazado (receta 3, 600.0 L, fechas ISO convertidas a `LocalDate`) y redirige a `/planificaciones-produccion` con la alerta de éxito "Planificación de producción registrada correctamente" (Post-Redirect-Get). |
| **CP-PPC-20** | Formulario vacío | Sin parámetros | Bean Validation: todos los campos son obligatorios | Responde 200 con el formulario (sin redirigir), el resumen "No se pudo registrar la planificación" y los mensajes "La receta es obligatoria", "La cantidad a producir es obligatoria", "La fecha de inicio estimada es obligatoria" y "La fecha de finalización estimada es obligatoria". No invoca al service. |
| **CP-PPC-21** | Cantidad negativa conserva lo cargado | `idReceta: 3` (se resuelve "Pale Ale"), `cantidadAProducir: -5`, fechas válidas | `@Positive` $\rightarrow$ **Rechaza** | Vuelve a mostrar el formulario con "La cantidad a producir debe ser mayor a 0", conservando la receta elegida ("Pale Ale" e `idReceta` 3), "-5" y las dos fechas. No invoca al service. |
| **CP-PPC-22** | Valores con formato inválido | `cantidadAProducir: "abc"`, `fechaInicioEstimada: "no-es-fecha"` | El valor no se puede convertir | Vuelve a mostrar el formulario con el error de campo "Ingresá un número válido" (y el de la fecha). No invoca al service. |
| **CP-PPC-23** | Regla de negocio rechazada | Datos válidos con el fin (2027-07-01) anterior al inicio (2027-07-25). El service lanza `ReglaNegocioException("La fecha de finalización estimada no puede ser anterior a la fecha de inicio estimada")` | El service es la fuente de verdad | Vuelve a mostrar el formulario (sin redirigir) con el mensaje en el resumen de errores y conserva la receta, la cantidad y las dos fechas. |
| **CP-PPC-24** | Receta inexistente | `idReceta: 99`. El service lanza `RecursoNoEncontradoException("No se encontró la receta con ID: 99")` | La receta ya no existe (o no tiene versión activa) | Vuelve a mostrar el formulario con el mensaje del service y conserva la cantidad y las fechas. |
| **CP-PPC-25** | Gerente de compras rechazado | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta `danger`. No invoca al service. |
| **CP-PPC-26** | Operario rechazado | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta `danger`. No invoca al service. |

---

### 4. `GET /planificaciones-produccion/{id}` — `mostrarDetalle(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PPC-27** | Estado actual con lotes | `id: 7` (pendiente, "Pale Ale", 600 L). El service de lotes devuelve 4 lotes: finalizado y en ejecución en "F-01", pendiente y cancelado en "F-02" | La planificación existe | Responde 200 con la vista `planificacion_produccion/planificacion-detalle`. Muestra "Estado actual de la planificación", los datos de la cabecera, los contadores (Lotes totales 4, Planificados (Pendiente) 1, En ejecución 1, Finalizados 1, Cancelados 1), el cronograma (`data-gantt`, "Cronograma de lotes", columna "Fermentador", una barra por lote con los colores `gantt-color--finalizado`, `--en-ejecucion`, `--pendiente` y `--cancelado`, el tramo de progreso `gantt-progreso` y la etiqueta "PALE_ALE-3 - Pendiente") y la tabla de lotes (columnas "Inicio real" y "Fin real", estados legibles como "En ejecución"). No muestra el mensaje de sin lotes. |
| **CP-PPC-28** | Sin lotes | `id: 7`. El service de lotes devuelve una lista vacía | No hay lotes que mostrar | El cronograma y la tabla muestran, cada uno, "Todavía no hay lotes registrados para esta planificación" (dos veces en la página); los cinco contadores quedan en 0; no se dibuja el diagrama (sin `data-gantt` ni barras). |
| **CP-PPC-29** | Botones de cierre en una pendiente | Rol `GERENTE_DE_PRODUCCION`, planificación `PENDIENTE` | Finalizar y Anular proceden solo sobre una pendiente | La cabecera incluye "Volver al listado" (`/planificaciones-produccion`), los botones Finalizar y Anular (`/planificaciones-produccion/7/finalizar` y `/anular`) y los diálogos `dialogo-finalizar` y `dialogo-anular`. |
| **CP-PPC-30** | Planificación finalizada | Planificación `FINALIZADA` con `motivoFinalizacion: "Se alcanzó el volumen necesario"` y `fechaFinalizacion: 2027-07-20 14:30` | Los cierres excepcionales guardan su motivo | Muestra la etiqueta "Finalizada" y "Motivo de la finalización (20/07/2027 14:30)" con el motivo. No hay botones de cierre ni diálogos. |
| **CP-PPC-31** | Planificación anulada | Planificación `ANULADA` con `motivoAnulacion: "Se canceló el pedido del cliente"` y `fechaAnulacion: 2027-06-15 09:05` | Los cierres excepcionales guardan su motivo | Muestra la etiqueta "Anulada" y "Motivo de la anulación (15/06/2027 09:05)" con el motivo. No hay botones de cierre. |
| **CP-PPC-32** | Gerente de compras solo consulta | Rol `GERENTE_DE_COMPRAS`, planificación pendiente con 1 lote | El gerente de compras no gestiona planificaciones | Ve el estado actual completo (cronograma, tabla y "Volver al listado") sin botones de acción ni diálogos. |
| **CP-PPC-33** | Planificación inexistente | `id: 99`. El service lanza `RecursoNoEncontradoException("No se encontró la planificación de producción con ID: 99")` | El recurso no existe | Redirige al origen (`Referer`) con la alerta `danger` y el mensaje del service. No consulta los lotes. |
| **CP-PPC-34** | Rol no autorizado | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta `danger`. No invoca a ningún service. |

---

### 5. `POST /planificaciones-produccion/{id}/anular` — `anularPlanificacion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PPC-35** | Anulación correcta | `id: 7`, `motivo: "Se canceló el pedido"`, con token CSRF | El service acepta la anulación | Llama al service con el id 7 y el motivo, y redirige a `/planificaciones-produccion/7` con la alerta de éxito "Planificación de producción anulada correctamente". |
| **CP-PPC-36** | Rechazo por reglas del service | El service lanza `ReglaNegocioException("No se puede anular la planificación de producción porque tiene al menos un lote en estado Pendiente o En ejecución asociado")` | El `ControllerAdvices` traduce la regla | Redirige al origen con la alerta `warning` y el mensaje del service. |
| **CP-PPC-37** | Sin motivo | `motivo: ""`. El service lanza `ReglaNegocioException("El motivo de anulación es obligatorio")` | El controller no valida: el service es la fuente de verdad | Llama al service con el motivo vacío y redirige al origen con la alerta `warning` "El motivo de anulación es obligatorio". |
| **CP-PPC-38** | Planificación inexistente | `id: 99`. El service lanza `RecursoNoEncontradoException` | El recurso no existe | Redirige al origen con la alerta `danger` y el mensaje del service. |
| **CP-PPC-39** | Gerente de compras rechazado | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta `danger`. No invoca al service. |

---

### 6. `POST /planificaciones-produccion/{id}/finalizar` — `finalizarPlanificacion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PPC-40** | Finalización correcta | `id: 7`, `motivo: "Se alcanzó el volumen"`, con token CSRF | El service acepta la finalización | Llama al service con el id 7 y el motivo, y redirige a `/planificaciones-produccion/7` con la alerta de éxito "Planificación de producción finalizada correctamente". |
| **CP-PPC-41** | Rechazo por reglas del service | El service lanza `ReglaNegocioException("No se puede finalizar la planificación de producción porque no tiene ningún lote en estado Finalizado asociado; corresponde anularla en su lugar")` | El `ControllerAdvices` traduce la regla | Redirige al origen con la alerta `warning` y el mensaje del service. |
| **CP-PPC-42** | Sin motivo | Sin el parámetro `motivo`. El service lanza `ReglaNegocioException("El motivo de finalización es obligatorio")` | El controller no valida: el service es la fuente de verdad | Llama al service con el motivo `null` y redirige al origen con la alerta `warning` "El motivo de finalización es obligatorio". |
| **CP-PPC-43** | Planificación inexistente | `id: 99`. El service lanza `RecursoNoEncontradoException` | El recurso no existe | Redirige al origen con la alerta `danger` y el mensaje del service. |
| **CP-PPC-44** | Gerente de compras rechazado | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta `danger`. No invoca al service. |
| **CP-PPC-45** | Sin token CSRF | Rol `GERENTE_DE_PRODUCCION`, `POST .../7/anular` sin token CSRF | Spring Security exige el token | La petición es rechazada (redirige con alerta) y no llega al service. |

---

### 7. `GET /planificaciones-produccion/sugerencia-fecha` — `sugerirFechaFinalizacion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PPC-46** | Sugerencia correcta | `idReceta: 3`, `cantidadAProducir: 4200`, `fechaInicioEstimada: 2027-07-01`. El service devuelve 14 lotes de 300 L, de 18 días, con fin sugerido el 2027-07-25 | El service acepta la simulación | Responde 200 con el fragmento `planificacion_produccion/sugerencia-fecha :: sugerencia` (HTML, sin layout ni `<html>`), con `data-fecha-sugerida="2027-07-25"` y el texto "14 lotes de 300 L, de 18 días cada uno, con 1 día de limpieza entre lotes. Fin sugerido: 25/07/2027." |
| **CP-PPC-47** | Texto en singular | Sugerencia de 1 lote de 300,5 L y 1 día; `cantidadAProducir: "300,5"` | Concordancia de lote(s) y día(s) | El texto dice "1 lote de 300,5 L, de 1 día cada uno". La cantidad con coma decimal llega al service como `300.5`. |
| **CP-PPC-48** | Regla rechazada | El service lanza `ReglaNegocioException("La cantidad a producir debe ser mayor a 0")` | Un rechazo no es un error del servidor | Responde 200 con el mismo fragmento en estilo de error (`data-sugerencia-error`, `text-rose-600`) con el mensaje del service y sin `data-fecha-sugerida`. |
| **CP-PPC-49** | Receta sin versión activa | El service lanza `RecursoNoEncontradoException("No se encontró una versión activa para la receta con ID: 3")` | Un rechazo no es un error del servidor | Responde 200 con el fragmento de error y el mensaje del service, sin fecha. |
| **CP-PPC-50** | Sin receta elegida | `idReceta` vacío | Sin receta no hay simulación posible | Responde 200 con el fragmento de error "Elegí una receta para sugerir la fecha de finalización", sin fecha. No invoca al service. |
| **CP-PPC-51** | Datos vacíos | Solo `idReceta: 3` | El service es la fuente de verdad de las reglas | Llama al service con cantidad y fecha en `null`; el fragmento de error muestra su mensaje ("La cantidad a producir debe ser mayor a 0"). |
| **CP-PPC-52** | Datos mal escritos | `cantidadAProducir: "abc"`; y por separado `fechaInicioEstimada: "31/12/2027"` | Los datos llegan como texto para explicar el error | Responde 200 con el fragmento de error "Ingresá una cantidad a producir válida" y "Ingresá una fecha de inicio estimada válida". No invoca al service. |
| **CP-PPC-53** | Rol no autorizado | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta `danger`. No invoca al service. |

---

### 8. `GET /planificaciones-produccion/buscador/recetas` — `buscarRecetas(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PPC-54** | Fragmento del buscador | El service devuelve 2 recetas ("Pale Ale" id 3 y "Stout" id 4) | El rol está autorizado | Responde 200 con el fragmento `planificacion_produccion/buscador-receta :: buscador` (sin layout): título "Buscar receta", criterio de nombre con el placeholder "Ej: Pale Ale", el formulario `data-modal-buscar` hacia `/planificaciones-produccion/buscador/recetas` y una fila por receta con el botón "Elegir" (`data-id` y `data-texto`). |
| **CP-PPC-55** | Páginas de 8 y criterio | `nombre: " pale "`, `pagina: 1`; y por separado `nombre: "  "` | Un nombre en blanco equivale a no filtrar | Llama al service con `nombre: "pale"` y un `Pageable` de 8 elementos, página 1 y sin ordenamiento (el alfabético lo define la consulta del repositorio de recetas); con el valor en blanco lo llama con `null`. Solo trae recetas activas (lo garantiza el service). |
| **CP-PPC-56** | Paginación dentro del modal | `nombre: "pale"`, `pagina: 1`. El service devuelve la página 2 de 3 (20 recetas); y por separado una página vacía | Hay páginas anterior y siguiente; sin resultados | Muestra "Página 2 de 3" y los links `pagina=0` y `pagina=2` con `nombre=pale` marcados con `data-modal-get="true"`; sin resultados muestra "No se encontraron recetas con los criterios indicados." |
| **CP-PPC-57** | Rol no autorizado | Rol `GERENTE_DE_COMPRAS` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta `danger`. No invoca al service. |
