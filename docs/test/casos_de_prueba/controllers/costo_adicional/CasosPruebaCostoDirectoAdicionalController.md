## Casos de prueba de `CostoDirectoAdicionalController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `CostoDirectoAdicionalControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas reales; el service está simulado.

**Alcance:** alta, modificación y baja de los costos directos adicionales, que se gestionan desde la sección "Costos directos adicionales" de la pantalla de Configuración de Producción (la lista está en `ConfiguracionProduccionController`, CP-CPC-16 a 18). No tiene lista propia y todas las operaciones vuelven a `/configuracion-produccion`.

**Permisos:** `COSTO_DIRECTO_ADICIONAL_CREAR` (formulario y alta), `COSTO_DIRECTO_ADICIONAL_MODIFICAR` (formulario y modificación) y `COSTO_DIRECTO_ADICIONAL_ELIMINAR` (baja). El `GERENTE_DE_PRODUCCION` tiene los cuatro permisos de costos directos adicionales.

**Efecto de los cambios:** modificar el costo por litro afecta solo a los lotes que se finalicen después; los lotes cerrados conservan el costo ya aplicado. La baja es lógica: el costo deja de aplicarse a los lotes nuevos y los lotes viejos conservan su detalle.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CDC-01** | Formulario de alta | Rol `GERENTE_DE_PRODUCCION`, `GET /configuracion-produccion/costos-directos-adicionales/nuevo` | El rol tiene el permiso de crear | Vista `configuracion/costo-directo-form`: "Nuevo costo directo adicional", `novalidate`, dos campos con asterisco rojo, placeholders "Ej: Gas butano, Dióxido de carbono" y "Ej: 12,50", el costo con el sufijo "$ por litro" (`fragments/campo-unidad`), botón "Crear costo directo adicional" y enlace "Volver a Configuración de Producción". |
| **CP-CDC-02** | Rol sin permiso no abre el alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con la alerta `danger`. |
| **CP-CDC-03** | Alta válida | Nombre "Gas butano", costo `12.5` | El service registra | Llama al service con esos datos y redirige a `/configuracion-produccion` con "Costo directo adicional creado correctamente" (`success`). |
| **CP-CDC-04** | Alta sin datos | Nombre y costo vacíos | Bean Validation $\rightarrow$ **Falla** | Vuelve al formulario con "No se pudo crear el costo directo adicional", "El nombre es obligatorio" y "El costo por litro es obligatorio". No llama al service. |
| **CP-CDC-05** | Nombre duplicado | El service lanza `RecursoDuplicadoException` | Unicidad entre activos | Vuelve al formulario con el mensaje en el campo nombre y conserva "Gas butano" y `12.5`. |
| **CP-CDC-06** | Regla sobre el costo | El service lanza `ReglaNegocioException("El costo por litro no puede superar 1.000.000", "costoPorLitro")` | La excepción indica el campo | El mensaje se muestra en el campo del costo, marcado en rojo. |
| **CP-CDC-07** | Costo no numérico | `costoPorLitro=abc` | El valor no se convierte | Vuelve al formulario con el panel de errores y no llama al service. |
| **CP-CDC-08** | Alta sin CSRF | Sin token | Seguridad | No responde 200 y no llama al service. |
| **CP-CDC-09** | Formulario de edición | `id: 5` ("Gas butano", `12.5000`) | El costo existe | "Editar costo directo adicional", `action=".../5"`, nombre y costo actuales (`12.5`, sin ceros sobrantes) y "Guardar cambios". |
| **CP-CDC-10** | Costo entero e inexistente | `id: 5` con `10.0000`; `id: 99` inexistente | Formato y recurso | El entero se muestra como `10` (sin notación científica). El inexistente redirige con la alerta `danger`. |
| **CP-CDC-11** | Modificación válida | `id: 5`, costo `15` | El service actualiza | Llama a `modificarCostoDirectoAdicional(5, ...)` y redirige a `/configuracion-produccion` con "Costo directo adicional modificado correctamente". |
| **CP-CDC-12** | Modificación con errores | Nombre duplicado; nombre vacío | Service y Bean Validation | Vuelve al formulario de edición (`action=".../5"`) con "No se pudo modificar el costo directo adicional" y el mensaje correspondiente. |
| **CP-CDC-13** | Baja | `id: 5` | El service da de baja | Llama a `bajaCostoDirectoAdicional(5)` y redirige a `/configuracion-produccion` con "Costo directo adicional dado de baja correctamente". |
| **CP-CDC-14** | Rol sin permisos | Rol `OPERARIO_DE_PRODUCCION` en el alta, la edición, la modificación y la baja | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con la alerta `danger` en las cuatro y no invoca al service. |
| **CP-CDC-15** | Permisos del gerente de producción | `Rol.GERENTE_DE_PRODUCCION` | Permisos asignados | Tiene crear, modificar, eliminar y consultar costos directos adicionales. |
