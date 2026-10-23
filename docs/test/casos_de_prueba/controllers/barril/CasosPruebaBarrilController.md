## Casos de prueba de `BarrilController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `BarrilControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; `IBarrilServicio` e `IFabricanteBarrilServicio` están simulados (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/barril/`.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")` en cada método), incluido el buscador de fabricantes. "Gestión de Barriles" tiene dos pestañas, cada una con su propia URL: "Barriles" (`/barriles`, este controller) y "Fabricantes de barril" (`/barriles/fabricantes`, `FabricanteBarrilController`, ver `CasosPruebaFabricanteBarrilController.md`).

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `barril/barril-form`, sin redirigir, para conservar lo tipeado.
- El estado operativo y el contenido actual no se cargan en el formulario: lo define el sistema (un barril nuevo nace `DISPONIBLE` y con contenido 0) y se muestran solo en el listado.
- El fabricante se elige con un selector (`data-selector`, fragmento `fragments/selector-entidad`): es un campo blanco con ícono de lista (`bi-list-ul`) y placeholder "Seleccione fabricante", que no se tipea y se hace clic en el propio campo (`data-selector-buscar`) para abrir el buscador `/barriles/buscador/fabricantes` en un modal. No ofrece el menú de tres puntitos (`data-selector-menu`): el alta de fabricantes no se abre desde el selector. En el filtro del listado el selector es limpiable (`data-selector-limpiar`).
- El buscador es un fragmento HTML que se muestra en un modal del selector.
- Los listados son de un módulo maestro: solo muestran registros activos, en orden alfabético ascendente (barriles por `identificador`, fabricantes por `nombre`, sin distinguir mayúsculas, con el `id` como desempate).
- Todos los campos tienen un placeholder con un ejemplo.

---

### 1. `GET /barriles` — `listarBarriles(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BCT-01** | El GERENTE_DE_PRODUCCION ve el listado con fabricante, capacidad, contenido, estado operativo, las pestañas y las acciones por fila | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 barril (`id: 4`, `B-001`, capacidad 50, contenido actual 0, `DISPONIBLE`, 100 usos antes de mantenimiento, fabricante "Barrilitos SA") | El rol está autorizado | Responde 200 con la vista `barril/barril-lista`. Muestra el título "Gestión de Barriles", el identificador, el fabricante, la capacidad "50", el contenido actual "0", el estado como "Disponible", los usos antes de mantenimiento, las pestañas "Barriles" (activa) y "Fabricantes de barril" (`/barriles/fabricantes`), el link de alta `/barriles/nuevo` con el texto "Nuevo barril", el de edición `/barriles/4/editar` y el botón de baja con `data-baja-url="/barriles/4/baja"`. El diálogo de confirmación de baja incluye el token CSRF y la advertencia "Si no está Disponible, el sistema no lo permitirá." |
| **CP-BCT-02** | Sin filtros pide páginas de 20 ordenadas alfabéticamente por identificador, con los criterios en null | Rol `GERENTE_DE_PRODUCCION`, `GET /barriles` sin parámetros | Sin criterios. Barril es un módulo maestro | Llama a `filtrarBarriles` con `identificador`, `capacidad`, `estadoOperativo` e `idFabricante` en `null` y un `Pageable` de 20 elementos, página 0, ordenado de forma ascendente por `identificador` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-BCT-03** | El filtro por identificador llega recortado al service; un texto en blanco equivale a no filtrar | `identificador: " b-0 "`; y por separado `identificador: "   "` | Un texto en blanco equivale a no filtrar | Llama al service con `identificador: "b-0"`; con el texto en blanco lo llama con `identificador: null`. Los demás criterios van en `null`. |
| **CP-BCT-04** | El filtro por capacidad llega al service como valor exacto | `capacidad: 50` | Se filtra por capacidad | Llama al service con `capacidad: 50.0` y los demás criterios en `null`. |
| **CP-BCT-05** | El filtro por estado operativo ofrece las etiquetas del enum; la opción "Todos" no filtra | `estadoOperativo: CON_CERVEZA`; y por separado `estadoOperativo: ""` (opción "Todos") | Un select vacío equivale a no filtrar | El select ofrece "Todos", "Con cerveza", "Despachado", "Disponible", "En limpieza" y "En mantenimiento". Con `CON_CERVEZA` llama al service con ese estado (y esa opción queda seleccionada); con el valor vacío lo llama con `estadoOperativo: null`. |
| **CP-BCT-06** | El filtro por fabricante se elige con el selector y se muestra su nombre | `idFabricante: 2`. `IFabricanteBarrilServicio` devuelve "Barrilitos SA" | El fabricante del filtro existe | Llama al service con `idFabricante: 2`. El selector del filtro muestra `idFabricante=2` y el texto "Barrilitos SA", con `data-buscador-url="/barriles/buscador/fabricantes"`, el botón para quitar la selección y sin menú de tres puntitos. |
| **CP-BCT-07** | Fabricante del filtro inexistente | `idFabricante: 99`. `IFabricanteBarrilServicio` lanza `RecursoNoEncontradoException` | El fabricante del filtro ya no existe | El filtro de fabricante se ignora: llama al service con `idFabricante: null` y el selector queda sin selección ni texto (`data-selector-texto value=""`). |
| **CP-BCT-08** | Los cuatro criterios combinados llegan al service junto con la página pedida | `identificador: "b-0"`, `capacidad: 50`, `estadoOperativo: DISPONIBLE`, `idFabricante: 2`, `pagina: 1` | Todos los criterios se aplican a la vez | Llama al service con `identificador: "b-0"`, `capacidad: 50.0`, `DISPONIBLE`, `idFabricante: 2` y un `Pageable` de 20 elementos en la página 1. |
| **CP-BCT-09** | Los criterios quedan en el formulario y en los links de paginación | El service devuelve una página con anterior y siguiente; los cuatro criterios del caso anterior | Los criterios se conservan al paginar | El formulario muestra el identificador, la capacidad, el estado y el fabricante tal como se pidieron. Los links de página anterior y siguiente incluyen `pagina`, `identificador`, `capacidad`, `estadoOperativo` e `idFabricante`. El link "Limpiar" apunta a `/barriles` sin parámetros. |
| **CP-BCT-10** | Sin resultados muestra un mensaje claro en vez de una tabla vacía | Rol `GERENTE_DE_PRODUCCION`, el service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron barriles con los criterios indicados" en lugar de una tabla vacía. |
| **CP-BCT-11** | Un valor del filtro que no se puede convertir al tipo del parámetro no es un error del sistema | `capacidad: "abc"`; y por separado `estadoOperativo: "XYZ"` e `idFabricante: "abc"`, con `Referer: /barriles` | El valor no se puede convertir | En cada caso el `ControllerAdvices` redirige al origen (`/barriles`) con la alerta `warning` "El valor indicado no es válido". No invoca al service. |
| **CP-BCT-12** | El OPERARIO_DE_PRODUCCION no accede a la gestión: el listado es rechazado y no llega al service | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 2. `GET /barriles/buscador/fabricantes` — `buscarFabricantes(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BCT-13** | El buscador filtra por nombre y ofrece "Elegir" en cada fabricante | Rol `GERENTE_DE_PRODUCCION`, `nombre: " ton "`. El service devuelve "Tonelería del Sur" (`id: 2`) | El rol está autorizado para el buscador | Responde 200 con el fragmento `barril/buscador-fabricante :: buscador`. El campo de nombre conserva lo escrito y tiene el placeholder "Ej: Tonelería del Sur". Cada fabricante tiene un botón `data-elegir data-id="2" data-texto="Tonelería del Sur"`; no hay acciones de edición ni de baja. Llama a `filtrarFabricantesBarril` con `nombre: "ton"` y un `Pageable` de 8 elementos, página 0, ordenado de forma ascendente por `nombre` (sin distinguir mayúsculas) y por `id` como desempate. |
| **CP-BCT-14** | Sin criterio o con el nombre en blanco no filtra | `GET /barriles/buscador/fabricantes` sin parámetros; y por separado `nombre: "   "` | Un texto en blanco equivale a no filtrar | En ambos casos llama al service con `nombre: null`. |
| **CP-BCT-15** | Sin resultados muestra un mensaje claro dentro del modal | El service devuelve una página vacía | La página no tiene elementos | Responde 200 y muestra "No se encontraron fabricantes de barril con los criterios indicados" en lugar de la tabla. |
| **CP-BCT-16** | La paginación del buscador conserva el nombre buscado | `nombre: "ton"`, `pagina: 1`. El service devuelve una página con anterior y siguiente | Se pide la segunda página con criterio | Llama al service con `nombre: "ton"` en la página 1. Los links de página anterior y siguiente apuntan a `/barriles/buscador/fabricantes` con `pagina` y `nombre`. |
| **CP-BCT-17** | Un rol distinto de GERENTE_DE_PRODUCCION no accede al buscador | Rol `GERENTE_COMERCIAL` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-BCT-45** | Un fabricante con un nombre muy largo y sin espacios no desborda el buscador y conserva el botón "Elegir" | El service devuelve un fabricante cuyo `nombre` tiene 100 caracteres sin espacios | El texto largo no debe ensanchar la fila del modal | La tabla de resultados usa la clase `tabla-buscador` (el texto se parte en cualquier punto y el botón "Elegir" no se achica ni se parte). La celda del nombre no tiene `whitespace-nowrap` y muestra el nombre completo. La fila incluye el botón "Elegir" con el `data-id` y el `data-texto` completos. |

---

### 3. `GET /barriles/nuevo` — `mostrarFormularioAlta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BCT-18** | El formulario de alta muestra los campos con asterisco, placeholders y el selector de fabricante | Rol `GERENTE_DE_PRODUCCION` | El rol está autorizado | Responde 200 con la vista `barril/barril-form`. Muestra "Nuevo barril", el asterisco rojo de obligatorio en los cuatro campos, `action="/barriles"`, el formulario con `novalidate`, los campos "Identificador" (placeholder "Ej: B-001"), "Capacidad (L)" (placeholder "Ej: 50" y ayuda "Un número mayor a 0 y hasta 1.000."), "Usos antes de mantenimiento" (placeholder "Ej: 100" y ayuda "Un número entero mayor a 0 y hasta 100.000. Al llegar a esa cantidad de usos se requiere mantenimiento.") y "Fabricante" (selector con placeholder "Seleccione fabricante" y `data-buscador-url="/barriles/buscador/fabricantes"`, sin menú de tres puntitos), el botón "Crear barril" y el link "Volver a barriles" hacia `/barriles`. No incluye el panel de errores. |
| **CP-BCT-19** | El formulario de alta no ofrece estado operativo ni contenido actual | Rol `GERENTE_DE_PRODUCCION` | El sistema define el estado y el contenido iniciales | La página no tiene campos para el estado operativo ni para el contenido actual. |
| **CP-BCT-20** | El OPERARIO_DE_PRODUCCION no puede abrir el formulario de alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. |

---

### 4. `POST /barriles` — `altaBarril(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BCT-21** | Alta correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `identificador: "B-001"`, `capacidad: 50`, `usosMaximosAntesMantenimiento: 100`, `idFabricanteBarril: 2`, con CSRF | Formulario válido | Llama a `altaBarril` con los datos ingresados. Redirige a `/barriles` con la alerta "Barril creado correctamente" de tipo `success`. |
| **CP-BCT-22** | Sin ningún dato vuelve al formulario con todos los errores, sin llamar al service | `identificador: "  "`, sin capacidad, sin usos y sin fabricante | `@NotBlank` y `@NotNull` $\rightarrow$ **Fallan** | Vuelve al formulario con el panel "No se pudo crear el barril" y los mensajes "El identificador es obligatorio", "La capacidad es obligatoria", "Los usos máximos antes de mantenimiento son obligatorios" y "El fabricante de barril es obligatorio", cada uno junto a su campo. No invoca al service. |
| **CP-BCT-23** | Una capacidad no numérica muestra un mensaje en español, sin llamar al service | `capacidad: "abc"` | El valor no se puede convertir a número | Vuelve al formulario con "Ingrese un número válido" (mensaje en español, no el texto técnico de Spring) junto a la capacidad. No invoca al service. |
| **CP-BCT-24** | Unos usos no enteros muestran un mensaje en español, sin llamar al service | `usosMaximosAntesMantenimiento: "abc"` | El valor no se puede convertir a número entero | Vuelve al formulario con "Ingrese un número entero válido" junto al campo. No invoca al service. |
| **CP-BCT-25** | Al volver con errores el selector conserva el fabricante elegido | `identificador: ""`, `idFabricanteBarril: 2`. `IFabricanteBarrilServicio` devuelve "Barrilitos SA" | El formulario se vuelve a mostrar | El selector conserva `idFabricanteBarril=2` y el texto "Barrilitos SA", junto con los demás valores tipeados (la capacidad sin ceros sobrantes: `50`). |
| **CP-BCT-26** | Un identificador duplicado vuelve al formulario con el mensaje del service junto al campo identificador | El service lanza `RecursoDuplicadoException("Ya existe un barril con el identificador 'B-001' para el fabricante seleccionado")` | El service rechaza el identificador | Vuelve al formulario con el mensaje del service junto al campo "Identificador", marcado en rojo, y con lo tipeado conservado (incluido el fabricante elegido). |
| **CP-BCT-27** | Una regla de negocio rechazada por el service se muestra como error general del formulario | El service lanza `ReglaNegocioException("Regla incumplida")` (ej. capacidad mayor al máximo permitido) | El service rechaza los datos | Vuelve al formulario y muestra el mensaje en el panel "No se pudo crear el barril", conservando lo tipeado. |
| **CP-BCT-28** | Un fabricante inexistente se informa junto al campo fabricante | `idFabricanteBarril: 99`. El service lanza `RecursoNoEncontradoException("No se encontró el fabricante de barril con ID: 99")` y `IFabricanteBarrilServicio` también | El fabricante ya no existe | Vuelve al formulario y muestra el mensaje del service junto al campo "Fabricante". El selector queda sin texto y el resto de lo tipeado se conserva. |
| **CP-BCT-29** | Un rol distinto de GERENTE_DE_PRODUCCION no puede dar de alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-BCT-46** | Al volver con errores la capacidad decimal se muestra con punto y un texto inválido deja el campo vacío con su error | Primero `identificador: ""` y `capacidad: "4.5"`; después `capacidad: "abc"` | El identificador es obligatorio; la capacidad no es un número | En el primer caso el campo capacidad conserva `4.5`. En el segundo queda vacío y resaltado como error. No invoca al service. |

---

### 5. `GET /barriles/{id}/editar` — `mostrarFormularioEdicion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BCT-30** | El formulario de edición muestra los datos actuales, el fabricante elegido y apunta a la modificación | Rol `GERENTE_DE_PRODUCCION`, `id: 4`. El service devuelve `B-001`, capacidad 50, 100 usos y fabricante "Barrilitos SA" (`id: 2`) | El barril existe | Responde 200 con la vista `barril/barril-form`: título "Editar barril", los datos actuales (identificador, capacidad `50` sin ceros sobrantes y usos), el selector con `idFabricanteBarril=2` y el texto "Barrilitos SA", `action="/barriles/4"`, el botón "Guardar cambios" y el link "Volver a barriles". |
| **CP-BCT-31** | Un barril inexistente redirige al origen con el motivo | `id: 99`. El service lanza `RecursoNoEncontradoException("No se encontró el barril con ID: 99")`, con `Referer: /barriles` | El `ControllerAdvices` maneja la excepción | Redirige al origen (`/barriles`) con la alerta "No se encontró el barril con ID: 99" de tipo `danger`. No renderiza el formulario. |
| **CP-BCT-32** | Un rol distinto de GERENTE_DE_PRODUCCION no puede abrir el formulario de edición | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 6. `POST /barriles/{id}` — `modificarBarril(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BCT-33** | La modificación correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `id: 4`, datos válidos | Formulario válido | Llama a `modificarBarril(4, ...)`. Redirige a `/barriles` con la alerta "Barril modificado correctamente" de tipo `success`. |
| **CP-BCT-34** | Modificar sin datos obligatorios vuelve al formulario de edición con los errores, sin llamar al service | `id: 4`, `identificador: "  "`, `idFabricanteBarril: 2` | `@NotBlank` $\rightarrow$ **Falla** | Vuelve al formulario de edición (`action="/barriles/4"`) con el panel "No se pudo modificar el barril" y el mensaje "El identificador es obligatorio" junto al campo; el selector conserva el fabricante. No invoca al service. |
| **CP-BCT-35** | Modificar con un identificador duplicado vuelve al formulario de edición con el mensaje del service | El service lanza `RecursoDuplicadoException("Ya existe un barril con el identificador 'B-002' para el fabricante seleccionado")` | El service rechaza el identificador | Vuelve al formulario de edición con el panel "No se pudo modificar el barril" y el mensaje del service junto al campo "Identificador". |
| **CP-BCT-36** | Si el service rechaza la modificación por una regla de negocio, el motivo se muestra en el formulario | El service lanza `ReglaNegocioException("La capacidad no puede ser menor al contenido actual del barril (30.0 L)")` | Regla de negocio incumplida | Vuelve al formulario de edición y muestra el motivo en el panel de errores, conservando lo tipeado. |
| **CP-BCT-37** | Un rol distinto de GERENTE_DE_PRODUCCION no puede modificar | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-BCT-44** | Modificar un barril inexistente (con el fabricante vigente) lo maneja el `ControllerAdvices`, no el campo fabricante | `id: 99`, `idFabricanteBarril: 2`. El service lanza `RecursoNoEncontradoException("No se encontró el barril con ID: 99")` e `IFabricanteBarrilServicio` devuelve "Barrilitos SA", con `Referer: /barriles` | El fabricante del formulario existe, por lo que el recurso faltante es el barril | Redirige al origen (`/barriles`) con la alerta "No se encontró el barril con ID: 99" de tipo `danger`, igual que CP-BCT-31. No renderiza el formulario ni muestra el mensaje junto al campo "Fabricante". |

---

### 7. `POST /barriles/{id}/baja` — `bajaBarril(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BCT-38** | La baja correcta delega en el service y redirige al listado con alerta de éxito | Rol `GERENTE_DE_PRODUCCION`, `id: 4` | Barril disponible | Llama a `bajaBarril(4)`. Redirige a `/barriles` con la alerta "Barril dado de baja correctamente" de tipo `success`. |
| **CP-BCT-39** | Si el service rechaza la baja (barril no disponible) vuelve al origen con la alerta de advertencia | El service lanza `ReglaNegocioException("No se puede dar de baja el barril porque no se encuentra disponible")`, con cabecera `Referer: /barriles` | El barril está con cerveza, despachado, en limpieza o en mantenimiento | Redirige al origen (`Referer`) con el mensaje del service y alerta de tipo `warning`. |
| **CP-BCT-40** | Un rol distinto de GERENTE_DE_PRODUCCION no puede dar de baja | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 8. Sidebar

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-BCT-41** | La sidebar del GERENTE_DE_PRODUCCION ofrece "Gestión de Barriles" en Administración, justo encima de "Gestión de Equipamiento" | Rol `GERENTE_DE_PRODUCCION` en `/barriles` | El rol está autorizado | Aparece el enlace `/barriles` con el texto "Gestión de Barriles" y el ícono `bi-boxes`, dentro de la sección "Administración", inmediatamente antes de "Gestión de Equipamiento" y resaltado como opción activa. |
| **CP-BCT-42** | La opción permanece resaltada en el formulario de alta y en el de edición | Rol `GERENTE_DE_PRODUCCION` en `/barriles/nuevo` y en `/barriles/4/editar` | La opción activa de las pantallas del módulo es "barriles" | En ambas páginas "Gestión de Barriles" aparece resaltada como opción activa. |
| **CP-BCT-43** | La sidebar de los demás roles no muestra "Gestión de Barriles" | Cada uno de los roles `ADMINISTRADOR`, `OPERARIO_DE_PRODUCCION`, `GERENTE_DE_COMPRAS`, `ENCARGADO_DE_DEPOSITO` y `GERENTE_COMERCIAL` en `/home` | El rol no está autorizado | No aparece el enlace `/barriles` ni el texto "Gestión de Barriles". |
