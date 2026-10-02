## Casos de prueba de `RecetaController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `RecetaControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IRecetaServicio` está simulado (mock) y no hay base de datos. La lógica de negocio se prueba aparte, en `services/receta/`.

**Rol requerido:** todos los métodos exigen `GERENTE_DE_PRODUCCION` (`@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")`). Las recetas son la primera de las tres pestañas de "Gestión de Recetas" (recetas, etapas de control y parámetros de control), cada una con su propia URL. El controller expone el listado, el detalle y la baja.

**Convenciones de los resultados:**
- _"Redirige con alerta"_ significa una respuesta 3xx con los atributos flash `mensaje` y `tipo` que muestra el layout.
- Una receta se muestra siempre con los datos de su versión vigente; no se expone el historial de versiones.
- El nombre de la receta es el de su versión vigente. El orden alfabético (sin distinguir mayúsculas, con el id como desempate) lo define la consulta `IRecetaRepository.filtrarRecetas`, porque el nombre no es una propiedad de la receta; por eso el `Pageable` que arma el controller no lleva ordenamiento.
- Los números se muestran sin ceros sobrantes y con coma decimal (`20.0` se ve "20" y `68.5` se ve "68,5").
- La duración total estimada es un texto legible calculado en `VersionRecetaResponseDTO` (por ejemplo "14 días 2 h 30 min"); la plantilla solo lo muestra.
- Los enums se muestran con su nombre legible: etapas (Maceración, Hervido, Fermentación, Maduración), uso del lúpulo (Hervor, Whirlpool, Dry hop), tipo de malta, formato del lúpulo y tipo de levadura.
- La sidebar muestra "Gestión de Recetas" dentro de la sección Administración, junto a "Gestión de Insumos" y "Gestión de Equipamiento".

---

### 1. `GET /recetas` — `listarRecetas(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-RCC-01** | Listado para el gerente de producción | Rol `GERENTE_DE_PRODUCCION`. El service devuelve 1 receta (`id: 7`, "Pale Ale", volumen base 20, OG 1.05, FG 1.01, IBU 35, maceración 60 min, hervido 90 min, fermentación 10 días, maduración 4 días) | El rol está autorizado | Responde 200 con la vista `receta/receta-lista`. Muestra "Gestión de Recetas", el nombre, las columnas "Volumen base (L)", "OG objetivo", "FG objetivo", "IBU objetivo" y "Duración total estimada" con los valores "20", "1,050", "35" y "14 días 2 h 30 min" (no `20.0`); el placeholder "Ej: Pale Ale" en el filtro; el botón "Nueva receta" con link a `/recetas/nuevo`; por fila el link de detalle `/recetas/7` (ícono `bi-eye`), el de modificación `/recetas/7/editar` y el botón de baja con `data-baja-url="/recetas/7/baja"` y `data-baja-nombre="Pale Ale"`. La tabla está envuelta en un contenedor `overflow-x-auto` y los botones llevan `whitespace-nowrap`. |
| **CP-RCC-02** | Pestañas del módulo | Rol `GERENTE_DE_PRODUCCION`, listado vacío | El módulo tiene tres pestañas | Muestra los links a `/recetas`, `/recetas/etapas-control` y `/recetas/parametros-control` con los textos "Etapas de control" y "Parámetros de control". La pestaña de recetas aparece marcada como activa. |
| **CP-RCC-03** | Paginación por defecto | Rol `GERENTE_DE_PRODUCCION`, sin parámetros | Sin criterios | Llama al service con `nombre: null` y un `Pageable` de 20 elementos, página 0 y sin ordenamiento. |
| **CP-RCC-04** | Orden alfabético definido por la consulta | Anotación `@Query` de `IRecetaRepository.filtrarRecetas` | El nombre vive en la versión vigente y no en la receta | La consulta termina en `ORDER BY UPPER(v.nombre) ASC, r.id ASC` (alfabético por nombre de la versión vigente, sin distinguir mayúsculas, con el id como desempate) y la consulta de conteo no lleva `ORDER BY`. |
| **CP-RCC-05** | Criterio de búsqueda hacia el service | `nombre: " pale "`, `pagina: 1`; y por separado `nombre: "   "` | Un texto en blanco equivale a no filtrar | Llama al service con `nombre: "pale"` en la página 1; con el valor en blanco lo llama con `nombre: null`. |
| **CP-RCC-06** | La paginación conserva el criterio | `nombre: "pale"`, `pagina: 1`. El service devuelve la página 2 de 3 (45 recetas) | Hay páginas anterior y siguiente | Muestra "Página 2 de 3" y los links de anterior (`pagina=0`) y siguiente (`pagina=2`) con `nombre=pale`. |
| **CP-RCC-07** | Listado sin resultados | El service devuelve una página vacía | La página no tiene elementos | Muestra "No se encontraron recetas con los criterios indicados." en lugar de una tabla vacía. |
| **CP-RCC-08** | Diálogo de baja con CSRF | Rol `GERENTE_DE_PRODUCCION`, 1 receta | La baja envía un POST | Incluye el diálogo "Dar de baja receta" con el aviso "Si tiene una planificación de producción pendiente, el sistema no lo permitirá." y su formulario lleva el token CSRF (`_csrf`). |
| **CP-RCC-09** | Rol no autorizado | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
| **CP-RCC-10** | Ubicación en la sidebar | Rol `GERENTE_DE_PRODUCCION` | Las recetas se administran, no son operación diaria | "Gestión de Recetas" aparece dentro de la sección Administración (después de "Gestión de Insumos" y antes de "Gestión de Equipamiento") y no entre los botones de Operación; "Control de Lotes" sigue en Operación. |

---

### 2. `GET /recetas/{id}` — `mostrarDetalle(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-RCC-11** | Detalle completo de la versión vigente | `id: 7`. El service devuelve "Pale Ale" con una malta (Pilsen, Base, 4.5 kg), una levadura (US-05, Ale, 11.5 g) y un plan de monitoreo "Control de densidad" (etapa a controlar Fermentación) con el parámetro "Densidad" (unidad SG, mínimo 1.01, ideal 1.012, máximo 1.015) | La receta existe | Responde 200 con la vista `receta/receta-detalle`. Muestra las secciones "Datos generales" (volumen base (L), relación de empaste (L/kg), OG, FG e IBU objetivo), "Duraciones estimadas" (maceración y hervido en minutos, fermentación y maduración en días, y la duración total "14 días 2 h 30 min"), "Maltas (cantidad en kg)", "Levaduras (cantidad en g)" y "Planes de monitoreo" con la etapa de control, la etapa a controlar y la tabla de parámetros con unidad, mínimo, ideal y máximo sin ceros sobrantes. Incluye "Volver al listado" y "Modificar" (`/recetas/7/editar`). |
| **CP-RCC-12** | Lúpulos: tiempo de hervor solo para Hervor | La receta tiene tres lúpulos: Cascade (Pellet, alfa 5.5, uso `HERVOR`, etapa `HERVIDO`, 60 min, 25 g), Citra (Flor, alfa 12, uso `DRY_HOP`, etapa `FERMENTACION`, 40 g) y Mosaic (uso `WHIRLPOOL`, etapa `HERVIDO`, 30 g) | El tiempo de hervor solo tiene sentido en el uso Hervor | La sección "Lúpulos (cantidad en g, tiempo de hervor en minutos)" muestra formato, alfa, uso ("Hervor", "Dry hop", "Whirlpool") y etapa de uso legibles. Cascade muestra 60 en "Tiempo de hervor (min)"; Citra y Mosaic muestran un guion en esa columna. |
| **CP-RCC-13** | Receta sin plan de monitoreo | La versión no tiene planes de monitoreo | El plan es opcional | Muestra la sección "Planes de monitoreo" con el texto "La receta no tiene plan de monitoreo." y ninguna tabla de parámetros. |
| **CP-RCC-14** | Receta inexistente | `id: 99`. El service lanza `RecursoNoEncontradoException("No se encontró la receta con ID: 99")`, con `Referer: /recetas` | El `ControllerAdvices` maneja la excepción | Redirige a `/recetas` con la alerta "No se encontró la receta con ID: 99" de tipo `danger`. |
| **CP-RCC-15** | Rol no autorizado | Rol `ADMINISTRADOR` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |

---

### 3. `POST /recetas/{id}/baja` — `bajaReceta(...)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-RCC-16** | Baja correcta | `id: 7`, con CSRF | La receta puede darse de baja | Llama a `bajaReceta(7)`. Redirige a `/recetas` con la alerta "Receta dada de baja correctamente" de tipo `success`. |
| **CP-RCC-17** | Baja rechazada por planificación pendiente | El service lanza `ReglaNegocioException("No se puede dar de baja la receta porque tiene una planificación de producción en estado PENDIENTE asociada")`, con `Referer: /recetas` | La regla de negocio bloquea la baja | Redirige al origen (`/recetas`) con el motivo del service como alerta de tipo `warning`. |
| **CP-RCC-18** | Baja de una receta inexistente | `id: 99`. El service lanza `RecursoNoEncontradoException("No se encontró la receta con ID: 99")`, con `Referer: /recetas` | El `ControllerAdvices` maneja la excepción | Redirige a `/recetas` con la alerta "No se encontró la receta con ID: 99" de tipo `danger`. |
| **CP-RCC-19** | Rol no autorizado | Rol `OPERARIO_DE_PRODUCCION`, con CSRF | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con alerta de tipo `danger`. No invoca al service. |
