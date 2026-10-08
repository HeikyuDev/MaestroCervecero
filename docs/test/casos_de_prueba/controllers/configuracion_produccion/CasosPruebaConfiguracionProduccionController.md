## Casos de prueba de `ConfiguracionProduccionController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `ConfiguracionProduccionControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IConfiguracionProduccionServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/planificacion_produccion/CasosPruebaConfiguracionProduccion.md`. Para verificar la sidebar de los demás roles también se carga el `HomeController`.

**Rol requerido:** toda la clase exige `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")` a nivel de clase). Ningún otro rol accede.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- _"Vuelve al formulario"_ significa respuesta 200 con la vista `configuracion/configuracion-produccion`, sin redirigir, para conservar lo tipeado.
- La configuración es un registro único: no hay alta, baja ni listado. La pantalla carga los valores vigentes y los guarda completos.
- Los campos del formulario son: capacidad de lote estándar (litros), velocidad de envasado (litros por hora), porcentaje mínimo de consumo para avanzar de etapa (%) y los criterios de selección secuencial y concurrente. Todos son obligatorios, tienen texto de ayuda y los numéricos tienen un placeholder con un ejemplo.

---

### 1. `GET /configuracion-produccion` — `mostrarConfiguracion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CPC-01** | El GERENTE_DE_PRODUCCION ve la configuración vigente en los cinco campos, sin ceros sobrantes, con asterisco, placeholder y unidades | Rol `GERENTE_DE_PRODUCCION`. El service devuelve capacidad `300.0`, velocidad `100.5`, porcentaje `90.0` | El rol está autorizado | Responde 200 con la vista `configuracion/configuracion-produccion`. Formulario con `novalidate` y acción `/configuracion-produccion`; secciones "Tiempos y capacidades" y "Criterios de selección de equipos para planes de producción"; etiquetas con unidades; valores mostrados como `300`, `100.5` y `90` (nunca `300.0`); asterisco de obligatorio; placeholders "Ej: 300", "Ej: 100" y "Ej: 90"; botón "Guardar configuración"; sin resumen de errores. |
| **CP-CPC-02** | Cada campo explica para qué se usa con un texto de ayuda | Igual que el caso anterior | Texto de ayuda por campo $\rightarrow$ **Presente** | Se muestran los textos de la capacidad de lote, la velocidad de envasado, el porcentaje mínimo (con el rango "Entre 0 y 100.") y el de los criterios. |
| **CP-CPC-03** | Los dos selects muestran los criterios con nombres legibles y dejan elegido el vigente | El service devuelve el criterio secuencial `FERMENTADOR_LIBERACION_MAS_TEMPRANA` y el concurrente `MENOR_CANTIDAD_DE_LOTES` | Opciones y selección | Las opciones se muestran como "Fermentador de mayor capacidad", "Fermentador que se libera más temprano", "Menor cantidad de lotes" y "Equipos que se liberan más temprano" (nunca el nombre de la constante). Quedan seleccionados únicamente los dos criterios vigentes. |
| **CP-CPC-04** | Muestra la última modificación con fecha y usuario si la hay, y la omite si nunca se modificó | Primero una configuración con `lastModifiedDate` `01/10/2026 10:30` y `lastModifiedBy` `gerente.produccion`; luego una sin datos de auditoría de modificación | Datos de auditoría disponibles | En el primer caso muestra "Última modificación:" con la fecha y el usuario; en el segundo no muestra esa línea. |
| **CP-CPC-05** | Cualquier rol distinto del GERENTE_DE_PRODUCCION es rechazado al abrir la pantalla y no llega al service | Cada uno de los roles `ADMINISTRADOR`, `OPERARIO_DE_PRODUCCION`, `GERENTE_DE_COMPRAS`, `ENCARGADO_DE_DEPOSITO` y `GERENTE_COMERCIAL` | El rol no está autorizado | Redirige con alerta de tipo `danger` y el service no es invocado. |
| **CP-CPC-06** | Sin sesión iniciada se redirige al login | Petición sin usuario autenticado | Usuario autenticado $\rightarrow$ **No** | Redirige a `/login` y el service no es invocado. |
| **CP-CPC-07** | Si el service no encuentra la configuración, se redirige con una alerta y el mensaje del error | El service lanza `RecursoNoEncontradoException("No se encontró la configuración de producción")` | Configuración encontrada $\rightarrow$ **No** | Redirige con alerta `danger` y el mensaje de la excepción. |

---

### 2. `POST /configuracion-produccion` — `actualizarConfiguracion(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CPC-08** | Guarda con los valores enviados, redirige a la pantalla y avisa que se actualizó | Capacidad `250`, velocidad `80.5`, porcentaje `75`, criterio secuencial `FERMENTADOR_MAYOR_CAPACIDAD` y concurrente `EQUIPOS_LIBERACION_MAS_TEMPRANA` | Datos válidos | Redirige a `/configuracion-produccion` con alerta `success` "Configuración de producción actualizada correctamente". El service recibe los cinco valores exactamente como se enviaron. |
| **CP-CPC-09** | Si el service rechaza por una regla, vuelve al formulario con el mensaje en el resumen de errores y lo tipeado conservado | El service lanza `ReglaNegocioException` por un porcentaje fuera de 0 a 100 (se envió `150`) | Regla de negocio cumplida $\rightarrow$ **No** | Vuelve al formulario (200) con "No se pudo guardar la configuración" y el mensaje de la regla en el resumen. Los campos conservan lo tipeado (`250`, `80`, `150`) y el criterio concurrente elegido sigue seleccionado. |
| **CP-CPC-10** | Un número mal escrito muestra "Ingresá un número válido" en el campo y no llega al service | Capacidad de lote `abc`; el resto válido | Valor convertible a número $\rightarrow$ **No** | Vuelve al formulario con el resumen de errores, el mensaje "Ingresá un número válido", el campo marcado en rojo y los demás valores conservados. El service no es invocado. |
| **CP-CPC-11** | Los campos vacíos se informan como obligatorios, uno por uno, y no llegan al service | Los cinco campos enviados vacíos | Campos informados $\rightarrow$ **No** | Vuelve al formulario con un mensaje de obligatorio por cada campo (capacidad, velocidad, porcentaje y los dos criterios). El service no es invocado. |
| **CP-CPC-12** | Cualquier rol distinto del GERENTE_DE_PRODUCCION es rechazado al guardar y no llega al service | Cada uno de los roles `ADMINISTRADOR`, `OPERARIO_DE_PRODUCCION`, `GERENTE_DE_COMPRAS`, `ENCARGADO_DE_DEPOSITO` y `GERENTE_COMERCIAL`, con datos válidos y token CSRF | El rol no está autorizado | Redirige con alerta de tipo `danger` y el service no es invocado. |
| **CP-CPC-13** | Guardar sin token CSRF es rechazado (redirige) y no llega al service | Rol `GERENTE_DE_PRODUCCION`, datos válidos, sin token CSRF | Token CSRF presente $\rightarrow$ **No** | Responde con una redirección y el service no es invocado. |

---

### 3. Sidebar

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CPC-14** | La sidebar del GERENTE_DE_PRODUCCION muestra la sección "Configuración" con "Configuración de Producción" (bi-sliders), activa en esta pantalla | Rol `GERENTE_DE_PRODUCCION` en `/configuracion-produccion` | El rol está autorizado | Aparece el enlace `/configuracion-produccion` con el ícono `bi-sliders`, resaltado como opción activa, en una sección "Configuración" ubicada debajo de "Administración". |
| **CP-CPC-15** | La sidebar de los demás roles no muestra ni la sección "Configuración" ni el enlace a la pantalla | Cada uno de los roles `ADMINISTRADOR`, `OPERARIO_DE_PRODUCCION`, `GERENTE_DE_COMPRAS`, `ENCARGADO_DE_DEPOSITO` y `GERENTE_COMERCIAL` en `/home` | El rol no está autorizado | No aparece el título de la sección "Configuración", ni el enlace `/configuracion-produccion`, ni el ícono `bi-sliders`. |
