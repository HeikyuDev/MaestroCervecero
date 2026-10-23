## Casos de prueba de `PresentacionComercialController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `PresentacionComercialControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; `IPresentacionComercialServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/proveedor/CasosPruebaPresentacionComercial.md`. La prueba también levanta `HomeController` para verificar la sidebar desde el panel de control.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_COMPRAS` (`@PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")` en cada método). "Gestión de Presentaciones" es una sola pantalla (sin pestañas) de la sección Administración de la sidebar.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `presentacion_comercial/presentacion-form`, sin redirigir, para conservar lo tipeado.
- El listado es de un módulo maestro: solo muestra presentaciones activas, en orden alfabético ascendente por `nombre` (sin distinguir mayúsculas), con el `id` como desempate.
- La unidad de medida se muestra con su nombre legible y su símbolo: "Gramo (g)", "Kilogramo (kg)" y "Tonelada (t)".
- Las cantidades del listado se muestran sin ceros sobrantes y con coma decimal (25.0 se muestra "25" y 2.5 "2,5"); en los campos numéricos del formulario se muestran sin ceros sobrantes y con punto decimal (25.0 se muestra "25" y 2.5 "2.5").
- Todos los campos tienen un placeholder con un ejemplo.

---

### 1. `GET /presentaciones-comerciales` — `listarPresentaciones(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-01** | El GERENTE_DE_COMPRAS ve el listado con nombre, cantidad, unidad y las acciones por fila | Rol `GERENTE_DE_COMPRAS`. El service devuelve 1 presentación (`id: 3`, "Bolsa de 25 Kg", `25.0`, `KILOGRAMO`) | El rol está autorizado | Responde 200 con la vista `presentacion_comercial/presentacion-lista`. Muestra el título "Gestión de Presentaciones", el título de pestaña "Presentaciones comerciales - Maestro Cervecero", las columnas "Cantidad" y "Unidad", el nombre, la cantidad "25", la unidad "Kilogramo (kg)", el link de alta `/presentaciones-comerciales/nuevo` con el texto "Nueva presentación", el de edición `/presentaciones-comerciales/3/editar` y el botón de baja con `data-baja-url="/presentaciones-comerciales/3/baja"`. El diálogo de confirmación de baja incluye el token CSRF y la advertencia "Si está en el catálogo de un proveedor activo o en una orden de compra pendiente, el sistema no lo permitirá." En la sidebar, "Gestión de Presentaciones" aparece resaltada como opción activa. |
| **CP-PCC-02** | Sin filtros pide páginas de 20 ordenadas alfabéticamente por nombre, con los criterios en null | Rol `GERENTE_DE_COMPRAS`, `GET /presentaciones-comerciales` sin parámetros | Sin criterios. Presentación comercial es un módulo maestro | Llama a `filtrarPresentacionesComerciales` con `nombre`, `cantidad` y `unidadDeMedida` en `null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-PCC-03** | El criterio de nombre llega recortado al service; un texto en blanco equivale a no filtrar | `nombre: " bolsa "`; y por separado `nombre: "   "` | Un texto en blanco equivale a no filtrar | Llama al service con `nombre: "bolsa"`; con el texto en blanco lo llama con `nombre: null`. |
| **CP-PCC-04** | Los filtros por cantidad y por unidad de medida llegan al service; la unidad ofrece etiquetas legibles y "Todas" no filtra | `cantidad: "25"`, `unidadDeMedida: "KILOGRAMO"`; y por separado ambos vacíos | Criterios opcionales | El selector de unidad ofrece "Todas", "Gramo (g)", "Kilogramo (kg)" y "Tonelada (t)", con "KILOGRAMO" seleccionado. Llama al service con `cantidad: 25.0` y `unidadDeMedida: KILOGRAMO`; con los valores vacíos los pasa en `null`. |
| **CP-PCC-05** | Los criterios quedan en el formulario y en los links de paginación | `nombre: "bolsa"`, `cantidad: "25"`, `unidadDeMedida: "KILOGRAMO"`, `pagina: 1`. El service devuelve una página con anterior y siguiente | Los criterios se conservan al paginar | El campo "Nombre" muestra "bolsa" (placeholder "Ej: Bolsa de 25 Kg") y el campo "Cantidad" muestra "25" (placeholder "Ej: 25"). Los links de página anterior y siguiente incluyen `pagina`, `nombre`, `cantidad` y `unidadDeMedida`. El link "Limpiar" apunta a `/presentaciones-comerciales` sin parámetros. Llama al service con la página 1. |
| **CP-PCC-06** | Sin resultados muestra un mensaje claro en vez de una tabla vacía | Rol `GERENTE_DE_COMPRAS`, el service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron presentaciones comerciales con los criterios indicados" en lugar de una tabla vacía. |
| **CP-PCC-07** | Un valor del filtro que no se puede convertir al tipo del parámetro no es un error del sistema | `cantidad: "abc"`; y por separado `unidadDeMedida: "LITRO"`; con cabecera `Referer: /presentaciones-comerciales` | El `ControllerAdvices` maneja el valor inválido | Redirige al origen con la alerta "El valor indicado no es válido" de tipo `warning`. No invoca al service. |
| **CP-PCC-08** | El listado muestra las cantidades enteras sin ".0" y conserva los decimales | El service devuelve "Bolsa de 25 Kg" (`25.0`, `KILOGRAMO`) y "Sobre de 2,5 g" (`2.5`, `GRAMO`) | Formato de números del listado | Muestra "25" (nunca "25.0") y "2,5", y la unidad "Gramo (g)". |
| **CP-PCC-09** | Un rol distinto de GERENTE_DE_COMPRAS no accede al listado y no llega al service | Roles `GERENTE_DE_PRODUCCION`, `OPERARIO_DE_PRODUCCION`, `ADMINISTRADOR`, `ENCARGADO_DE_DEPOSITO` y `GERENTE_COMERCIAL` (un caso por rol) | `@PreAuthorize("hasRole('GERENTE_DE_COMPRAS')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-PCC-10** | Un usuario sin sesión es enviado al login en cada operación y no llega al service | Sin usuario autenticado: listado, formulario de alta, formulario de edición, alta, modificación y baja | Spring Security exige autenticación | Cada pedido redirige a `/login`. No invoca al service. |

---

### 2. `GET /presentaciones-comerciales/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-11** | El formulario de alta muestra los campos con asterisco, placeholders, ayudas con el límite y el selector de unidad | Rol `GERENTE_DE_COMPRAS` | El rol está autorizado | Responde 200 con la vista `presentacion_comercial/presentacion-form`. Muestra "Nueva presentación comercial", `action="/presentaciones-comerciales"`, el formulario con `novalidate` y el asterisco rojo de obligatorio en los tres campos: "Nombre" (placeholder "Ej: Bolsa de 25 Kg", ayuda "Único entre las presentaciones comerciales, sin distinguir mayúsculas. Hasta 100 caracteres."), "Cantidad" (placeholder "Ej: 25", ayuda "Un número mayor a 0. Su equivalente no puede superar los 10.000 kilogramos.") y "Unidad de medida" (opción "Seleccione unidad" y las tres unidades legibles). Muestra el botón "Crear presentación" y el link "Volver a presentaciones comerciales" hacia `/presentaciones-comerciales`. No incluye la nota de bloqueo de la edición ni el panel de errores. |
| **CP-PCC-12** | Un rol distinto de GERENTE_DE_COMPRAS no puede abrir el formulario de alta | Rol `GERENTE_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 3. `POST /presentaciones-comerciales` — `altaPresentacion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-13** | Alta correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_COMPRAS`, `nombre: "Bolsa de 25 Kg"`, `cantidad: "25"`, `unidadDeMedida: "KILOGRAMO"`, con CSRF | Formulario válido | Llama a `altaPresentacionComercial` con los datos ingresados (`25.0`, `KILOGRAMO`). Redirige a `/presentaciones-comerciales` con la alerta "Presentación comercial creada correctamente" de tipo `success`. |
| **CP-PCC-14** | Sin ningún dato vuelve al formulario con todos los errores, sin llamar al service | `nombre: "  "`, `cantidad: ""`, `unidadDeMedida: ""` | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear la presentación comercial" y los mensajes "El nombre es obligatorio", "La cantidad es obligatoria" y "La unidad de medida es obligatoria"; el selector de unidad queda marcado en rojo. No invoca al service. |
| **CP-PCC-15** | Una cantidad no numérica o una unidad inexistente muestran un mensaje en español, sin llamar al service | `cantidad: "abc"`; y por separado `unidadDeMedida: "LITRO"` | Error de conversión del dato | Con la cantidad no numérica muestra "Ingrese un número válido" y el campo vacío marcado en rojo; con la unidad inexistente muestra "El valor ingresado no es válido". Nunca muestra el mensaje técnico de conversión. No invoca al service. |
| **CP-PCC-16** | Un nombre duplicado vuelve al formulario con el mensaje del service junto al campo nombre | El service lanza `RecursoDuplicadoException("Ya existe una presentación comercial con el nombre 'Bolsa de 25 Kg'")` | El service rechaza el nombre | Vuelve al formulario con el mensaje del service junto al campo "Nombre", marcado en rojo, conservando el nombre y la unidad elegida. |
| **CP-PCC-17** | Una regla de negocio rechazada por el service se muestra como error general del formulario | `cantidad: "20000"`. El service lanza `ReglaNegocioException("La cantidad no puede superar el equivalente a 10.000 kilogramos")` | El service rechaza los datos | Vuelve al formulario y muestra el mensaje en el panel "No se pudo crear la presentación comercial", conservando lo tipeado. |
| **CP-PCC-18** | Un rol distinto de GERENTE_DE_COMPRAS no puede dar de alta | Rol `GERENTE_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 4. `GET /presentaciones-comerciales/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-19** | El formulario de edición muestra los datos actuales, la nota de bloqueo y apunta a la modificación | Rol `GERENTE_DE_COMPRAS`, `id: 3`. El service devuelve "Bolsa de 25 Kg", `25.0`, `KILOGRAMO` | La presentación existe | Responde 200 con la vista `presentacion_comercial/presentacion-form`: título "Editar presentación comercial", el nombre actual, la cantidad "25" (nunca "25.0"), "KILOGRAMO" seleccionado, `action="/presentaciones-comerciales/3"`, la nota "La cantidad y la unidad de medida no se pueden cambiar si la presentación está en el catálogo de un proveedor activo o en una orden de compra pendiente.", el botón "Guardar cambios" y el link "Volver a presentaciones comerciales". |
| **CP-PCC-20** | El formulario de edición conserva los decimales de la cantidad | `id: 4`. El service devuelve "Sobre de 2,5 g", `2.5`, `GRAMO` | Cantidad con decimales | El campo "Cantidad" muestra "2.5" y "GRAMO" queda seleccionado. |
| **CP-PCC-21** | Una presentación inexistente redirige al origen con el motivo | `id: 99`. El service lanza `RecursoNoEncontradoException("No se encontró la presentación comercial con ID: 99")`, con `Referer: /presentaciones-comerciales` | El `ControllerAdvices` maneja la excepción | Redirige al origen (`/presentaciones-comerciales`) con la alerta "No se encontró la presentación comercial con ID: 99" de tipo `danger`. No renderiza el formulario. |
| **CP-PCC-22** | Un rol distinto de GERENTE_DE_COMPRAS no puede abrir el formulario de edición | Rol `GERENTE_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 5. `POST /presentaciones-comerciales/{id}` — `modificarPresentacion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-23** | La modificación correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_COMPRAS`, `id: 3`, datos válidos | Formulario válido | Llama a `modificarPresentacionComercial(3, ...)`. Redirige a `/presentaciones-comerciales` con la alerta "Presentación comercial modificada correctamente" de tipo `success`. |
| **CP-PCC-24** | Modificar sin datos obligatorios vuelve al formulario de edición con los errores, sin llamar al service | `id: 3`, `nombre: "  "`, sin cantidad ni unidad | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario de edición (`action="/presentaciones-comerciales/3"`, con la nota de bloqueo) con el panel "No se pudo modificar la presentación comercial" y los tres mensajes de obligatoriedad. No invoca al service. |
| **CP-PCC-25** | Modificar con un nombre duplicado vuelve al formulario de edición con el mensaje del service | El service lanza `RecursoDuplicadoException("El nombre 'Bolsa de 25 Kg' ya está en uso por otra presentación comercial")` | El service rechaza el nombre | Vuelve al formulario de edición con el panel "No se pudo modificar la presentación comercial" y el mensaje del service junto al campo "Nombre", marcado en rojo. |
| **CP-PCC-26** | Si el service rechaza el cambio de cantidad o unidad de una presentación en uso, el motivo se muestra en el formulario | `id: 3`, `cantidad: "50"`. El service lanza `ReglaNegocioException("No se puede modificar la cantidad ni la unidad de medida de una presentación comercial que está en un catálogo de proveedor activo o en una orden de compra pendiente")` | La presentación está en uso | Vuelve al formulario de edición (`action="/presentaciones-comerciales/3"`) con el motivo en el panel "No se pudo modificar la presentación comercial", conservando el nombre y la cantidad tipeada ("50"). |
| **CP-PCC-27** | Modificar una presentación inexistente lo maneja el ControllerAdvices | `id: 99`. El service lanza `RecursoNoEncontradoException("No se encontró la presentación comercial con ID: 99")`, con `Referer: /presentaciones-comerciales` | La presentación no existe | Redirige al origen con el mensaje del service y alerta de tipo `danger`. |
| **CP-PCC-28** | Un rol distinto de GERENTE_DE_COMPRAS no puede modificar | Rol `GERENTE_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 6. `POST /presentaciones-comerciales/{id}/baja` — `bajaPresentacion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-29** | La baja correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_COMPRAS`, `id: 3` | Presentación sin uso | Llama a `bajaPresentacionComercial(3)`. Redirige a `/presentaciones-comerciales` con la alerta "Presentación comercial dada de baja correctamente" de tipo `success`. |
| **CP-PCC-30** | Si el service rechaza la baja (presentación en uso) vuelve al origen con la alerta de advertencia | El service lanza `ReglaNegocioException("No se puede dar de baja la presentación comercial porque se encuentra asociada al catálogo de al menos un proveedor activo")`, con cabecera `Referer: /presentaciones-comerciales` | La presentación está en uso | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-PCC-31** | Un rol distinto de GERENTE_DE_COMPRAS no puede dar de baja | Rol `GERENTE_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 7. Sidebar

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-PCC-32** | La sidebar del GERENTE_DE_COMPRAS ofrece "Gestión de Presentaciones" en la sección Administración, debajo de Operación | Rol `GERENTE_DE_COMPRAS`, `GET /home` | La sección Administración incluye al GERENTE_DE_COMPRAS | La sidebar muestra Operación (con "Órdenes de Compra") y luego Administración con el enlace `/presentaciones-comerciales`, icono `bi-box` y texto "Gestión de Presentaciones", sin resaltar en el panel de control. No muestra "Gestión de Insumos", "Gestión de Equipamiento" ni "Gestión de Ubicaciones". |
| **CP-PCC-33** | La opción permanece resaltada en el formulario de alta y en el de edición | Rol `GERENTE_DE_COMPRAS`, `GET /presentaciones-comerciales/nuevo` y `GET /presentaciones-comerciales/3/editar` | Opción activa `presentaciones-comerciales` | En ambas pantallas "Gestión de Presentaciones" aparece resaltada. |
| **CP-PCC-34** | La sidebar de los demás roles no muestra "Gestión de Presentaciones" | Roles `GERENTE_DE_PRODUCCION`, `ADMINISTRADOR`, `OPERARIO_DE_PRODUCCION`, `ENCARGADO_DE_DEPOSITO` y `GERENTE_COMERCIAL` (un caso por rol), `GET /home` | La opción es solo del GERENTE_DE_COMPRAS | La sidebar no incluye el enlace `/presentaciones-comerciales` ni el texto "Gestión de Presentaciones". |
| **CP-PCC-35** | La sección Administración sigue apareciendo solo para el GERENTE_DE_PRODUCCION y el ADMINISTRADOR entre los demás roles | Roles `GERENTE_DE_PRODUCCION` y `ADMINISTRADOR`; y por separado `OPERARIO_DE_PRODUCCION`, `ENCARGADO_DE_DEPOSITO` y `GERENTE_COMERCIAL`, `GET /home` | Visibilidad del título de la sección | El GERENTE_DE_PRODUCCION y el ADMINISTRADOR ven "Administración"; el OPERARIO_DE_PRODUCCION, el ENCARGADO_DE_DEPOSITO y el GERENTE_COMERCIAL no la ven. |
