## Casos de prueba de `ConfiguracionProduccionController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `ConfiguracionProduccionControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IConfiguracionProduccionServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/planificacion_produccion/CasosPruebaConfiguracionProduccion.md`. Para verificar la sidebar de los demás roles también se carga el `HomeController`.

**Rol requerido:** toda la clase exige `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")` a nivel de clase). Ningún otro rol accede.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `configuracion/configuracion-produccion`, sin redirigir, para conservar lo tipeado.
- La configuración es un registro único: no hay alta, baja ni listado. La pantalla carga los valores vigentes y los guarda completos.
- Los campos del formulario son: velocidad de envasado (litros por hora), porcentaje mínimo de consumo para avanzar de etapa (%) y excedente máximo de un plan (litros). Todos son obligatorios y tienen texto de ayuda y un placeholder con un ejemplo.

---

### 1. `GET /configuracion-produccion` — `mostrarConfiguracion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CPC-01** | El GERENTE_DE_PRODUCCION ve la configuración vigente en sus tres campos, sin ceros sobrantes, con asterisco, placeholder y unidades | Rol `GERENTE_DE_PRODUCCION`. El service devuelve velocidad `100.5`, porcentaje `90.0` y excedente máximo `300.0` | El rol está autorizado | Responde 200 con la vista `configuracion/configuracion-produccion`. Formulario con `novalidate` y acción `/configuracion-produccion`; secciones "Tiempos de producción" y "Planes de producción"; etiquetas con unidades; valores mostrados como `100.5`, `90` y `300` (nunca `300.0`); sin campo de capacidad de lote; asterisco de obligatorio; placeholders "Ej: 300", "Ej: 100" y "Ej: 90"; botón "Guardar configuración"; sin resumen de errores. |
| **CP-CPC-02** | Cada campo explica para qué se usa con un texto de ayuda | Igual que el caso anterior | Texto de ayuda por campo $\rightarrow$ **Presente** | Se muestran los textos de la velocidad de envasado, el porcentaje mínimo (con el rango "Un número entre 0 y 100.") y el del excedente máximo de un plan. |
| **CP-CPC-03** | La sección Planes de producción tiene un único parámetro, el excedente máximo | Igual que el caso anterior | Parámetros de la sección | La sección "Planes de producción" muestra el campo "Excedente máximo de un plan" con asterisco. La pantalla no tiene selects ni criterios de selección de equipos: el criterio de cada modalidad de plan es fijo. |
| **CP-CPC-05** | Cualquier rol distinto del GERENTE_DE_PRODUCCION es rechazado al abrir la pantalla y no llega al service | Cada uno de los roles `ADMINISTRADOR`, `OPERARIO_DE_PRODUCCION`, `GERENTE_DE_COMPRAS`, `ENCARGADO_DE_DEPOSITO` y `ENCARGADO_DE_DISTRIBUCION` | El rol no está autorizado | Redirige con alerta de tipo `danger` y el service no es invocado. |
| **CP-CPC-06** | Sin sesión iniciada se redirige al login | Petición sin usuario autenticado | Usuario autenticado $\rightarrow$ **No** | Redirige a `/login` y el service no es invocado. |
| **CP-CPC-07** | Si el service no encuentra la configuración, se redirige con una alerta y el mensaje del error | El service lanza `RecursoNoEncontradoException("No se encontró la configuración de producción")` | Configuración encontrada $\rightarrow$ **No** | Redirige con alerta `danger` y el mensaje de la excepción. |

---

### 2. `POST /configuracion-produccion` — `actualizarConfiguracion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CPC-08** | Guarda con los valores enviados, redirige a la pantalla y avisa que se actualizó | Velocidad `80.5`, porcentaje `75`, excedente máximo `300` | Datos válidos | Redirige a `/configuracion-produccion` con alerta `success` "Configuración de producción actualizada correctamente". El service recibe los tres valores exactamente como se enviaron. |
| **CP-CPC-09** | Si el service rechaza por una regla, vuelve al formulario con el mensaje en el resumen de errores y lo tipeado conservado | El service lanza `ReglaNegocioException` por un porcentaje fuera de 0 a 100 (se envió `150`) | Regla de negocio cumplida $\rightarrow$ **No** | Vuelve al formulario (200) con "No se pudo guardar la configuración" y el mensaje de la regla en el resumen. Los campos conservan lo tipeado (`80` y `150`). |
| **CP-CPC-10** | Un número mal escrito muestra "Ingrese un número válido" en el campo y no llega al service | Velocidad de envasado `abc`; el resto válido | Valor convertible a número $\rightarrow$ **No** | Vuelve al formulario con el resumen de errores, el mensaje "Ingrese un número válido", el campo marcado en rojo y los demás valores conservados. El service no es invocado. |
| **CP-CPC-11** | Los campos vacíos se informan como obligatorios, uno por uno, y no llegan al service | Los tres campos enviados vacíos | Campos informados $\rightarrow$ **No** | Vuelve al formulario con un mensaje de obligatorio por cada campo (velocidad, porcentaje y excedente máximo). El service no es invocado. |
| **CP-CPC-19** | La sección Planes de producción muestra el excedente máximo vigente | Rol `GERENTE_DE_PRODUCCION`. El service devuelve excedente máximo `300.0` | El rol está autorizado | El campo "Excedente máximo de un plan" lleva asterisco, valor `300` (nunca `300.0`), unidad "L", placeholder "Ej: 300" y el texto de ayuda "Litros que la suma de los lotes de un plan puede superar a la cantidad a producir al confirmarlo". |
| **CP-CPC-20** | Un excedente máximo negativo se rechaza en el campo | Datos válidos y excedente máximo `-5` | `@PositiveOrZero` $\rightarrow$ **No cumple** | Vuelve al formulario con "No se pudo guardar la configuración" y "El excedente máximo de un plan debe ser mayor o igual a 0", conservando `-5` en el campo. El service no es invocado. |
| **CP-CPC-12** | Cualquier rol distinto del GERENTE_DE_PRODUCCION es rechazado al guardar y no llega al service | Cada uno de los roles `ADMINISTRADOR`, `OPERARIO_DE_PRODUCCION`, `GERENTE_DE_COMPRAS`, `ENCARGADO_DE_DEPOSITO` y `ENCARGADO_DE_DISTRIBUCION`, con datos válidos y token CSRF | El rol no está autorizado | Redirige con alerta de tipo `danger` y el service no es invocado. |
| **CP-CPC-13** | Guardar sin token CSRF es rechazado (redirige) y no llega al service | Rol `GERENTE_DE_PRODUCCION`, datos válidos, sin token CSRF | Token CSRF presente $\rightarrow$ **No** | Responde con una redirección y el service no es invocado. |

---

### 3. Sidebar

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CPC-14** | La sidebar del GERENTE_DE_PRODUCCION muestra la sección "Configuración" con "Configuración de Producción" (bi-sliders), activa en esta pantalla | Rol `GERENTE_DE_PRODUCCION` en `/configuracion-produccion` | El rol está autorizado | Aparece el enlace `/configuracion-produccion` con el ícono `bi-sliders`, resaltado como opción activa, en una sección "Configuración" ubicada debajo de "Administración". |
| **CP-CPC-15** | La sidebar de los demás roles no muestra ni la sección "Configuración" ni el enlace a la pantalla | Cada uno de los roles `ADMINISTRADOR`, `OPERARIO_DE_PRODUCCION` y `ENCARGADO_DE_DEPOSITO` en `/home` | El rol no está autorizado | No aparece el título de la sección "Configuración", ni el enlace `/configuracion-produccion`, ni el ícono `bi-sliders`. |
| **CP-CPC-21** | La sidebar del GERENTE_DE_COMPRAS muestra la sección "Configuración" solo con "Configuración de Compras" | Rol `GERENTE_DE_COMPRAS` en `/home` | El rol no está autorizado a esta pantalla, pero tiene su propia configuración | Aparece la sección "Configuración" con el enlace `/configuracion-compras`; no aparece el enlace `/configuracion-produccion` ni el texto "Configuración de Producción". |
| **CP-CPC-22** | La sidebar del ENCARGADO_DE_DISTRIBUCION muestra la sección "Configuración" solo con "Configuración de Distribución" | Rol `ENCARGADO_DE_DISTRIBUCION` en `/home` | El rol no está autorizado a esta pantalla, pero tiene su propia configuración | Aparece la sección "Configuración" con el enlace `/configuracion-distribucion`; no aparece el enlace `/configuracion-produccion` ni el texto "Configuración de Producción". |
| **CP-CPC-16** | Pestañas de Configuración de Producción | Rol `GERENTE_DE_PRODUCCION` con sus permisos, `GET /configuracion-produccion` | La pantalla tiene dos pestañas con ícono y ruta propia | `nav` con `aria-label="Configuración de Producción"`: "Parámetros de producción" (`bi-sliders`, `/configuracion-produccion`, activa con `aria-current="page"`) y "Costos directos adicionales" (`bi-cash-coin`, `/configuracion-produccion/costos-directos-adicionales`). La pestaña de parámetros trae el formulario completo con su único botón "Guardar configuración" y no incluye la lista de costos ni "Nuevo costo directo adicional". |
| **CP-CPC-17** | Visibilidad de la pestaña de costos | Rol `GERENTE_DE_PRODUCCION` sin el permiso `COSTO_DIRECTO_ADICIONAL_CONSULTAR` | La pestaña de costos exige ese permiso | Solo se muestra la pestaña de parámetros; el formulario sigue siendo un único guardado (un solo botón "Guardar configuración"). |
| **CP-CPC-18** | Pestañas al volver con errores | Un envío del formulario con un campo vacío | Reenvío con errores | Vuelve con el formulario y ambas pestañas, la de parámetros activa. |
