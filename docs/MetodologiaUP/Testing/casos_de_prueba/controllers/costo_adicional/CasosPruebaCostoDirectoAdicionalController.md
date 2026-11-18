## Casos de prueba de `CostoDirectoAdicionalController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `CostoDirectoAdicionalControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas reales; el service está simulado.

**Alcance:** la lista (pestaña "Costos directos adicionales" de Configuración de Producción, `/configuracion-produccion/costos-directos-adicionales`) y el alta, la modificación y la baja de los costos directos adicionales, que vuelven a esa pestaña.

**Permisos:** `COSTO_DIRECTO_ADICIONAL_CONSULTAR` (lista), `COSTO_DIRECTO_ADICIONAL_CREAR` (formulario y alta), `COSTO_DIRECTO_ADICIONAL_MODIFICAR` (formulario y modificación) y `COSTO_DIRECTO_ADICIONAL_ELIMINAR` (baja). El `GERENTE_DE_PRODUCCION` tiene los cuatro permisos de costos directos adicionales.

**Efecto de los cambios:** modificar el costo por litro afecta solo a los lotes que se finalicen después; los lotes cerrados conservan el costo ya aplicado. La baja es lógica: el costo deja de aplicarse a los lotes nuevos y los lotes viejos conservan su detalle.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CDC-01** | Formulario de alta | Rol `GERENTE_DE_PRODUCCION`, `GET /configuracion-produccion/costos-directos-adicionales/nuevo` | El rol tiene el permiso de crear | Vista `configuracion/costo-directo-form`: "Nuevo costo directo adicional", `novalidate`, dos campos con asterisco rojo, placeholders "Ej: Gas butano, Dióxido de carbono" y "Ej: 12,50", el costo con el sufijo "$ por litro" (`fragments/campo-unidad`), botón "Crear costo directo adicional" y enlace "Volver a costos directos adicionales". |
| **CP-CDC-02** | Rol sin permiso no abre el alta | Rol `OPERARIO_DE_PRODUCCION` | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con la alerta `danger`. |
| **CP-CDC-03** | Alta válida | Nombre "Gas butano", costo `12.5` | El service registra | Llama al service con esos datos y redirige a `/configuracion-produccion/costos-directos-adicionales` con "Costo directo adicional creado correctamente" (`success`). |
| **CP-CDC-04** | Alta sin datos | Nombre y costo vacíos | Bean Validation $\rightarrow$ **Falla** | Vuelve al formulario con "No se pudo crear el costo directo adicional", "El nombre es obligatorio" y "El costo por litro es obligatorio". No llama al service. |
| **CP-CDC-05** | Nombre duplicado | El service lanza `RecursoDuplicadoException` | Unicidad entre activos | Vuelve al formulario con el mensaje en el campo nombre y conserva "Gas butano" y `12.5`. |
| **CP-CDC-06** | Regla sobre el costo | El service lanza `ReglaNegocioException("El costo por litro no puede superar 1.000.000", "costoPorLitro")` | La excepción indica el campo | El mensaje se muestra en el campo del costo, marcado en rojo. |
| **CP-CDC-07** | Costo no numérico | `costoPorLitro=abc` | El valor no se convierte | Vuelve al formulario con el panel de errores y no llama al service. |
| **CP-CDC-08** | Alta sin CSRF | Sin token | Seguridad | No responde 200 y no llama al service. |
| **CP-CDC-09** | Formulario de edición | `id: 5` ("Gas butano", `12.5000`) | El costo existe | "Editar costo directo adicional", `action=".../5"`, nombre y costo actuales (`12.5`, sin ceros sobrantes) y "Guardar cambios". |
| **CP-CDC-10** | Costo entero e inexistente | `id: 5` con `10.0000`; `id: 99` inexistente | Formato y recurso | El entero se muestra como `10` (sin notación científica). El inexistente redirige con la alerta `danger`. |
| **CP-CDC-11** | Modificación válida | `id: 5`, costo `15` | El service actualiza | Llama a `modificarCostoDirectoAdicional(5, ...)` y redirige a `/configuracion-produccion/costos-directos-adicionales` con "Costo directo adicional modificado correctamente". |
| **CP-CDC-12** | Modificación con errores | Nombre duplicado; nombre vacío | Service y Bean Validation | Vuelve al formulario de edición (`action=".../5"`) con "No se pudo modificar el costo directo adicional" y el mensaje correspondiente. |
| **CP-CDC-13** | Baja | `id: 5` | El service da de baja | Llama a `bajaCostoDirectoAdicional(5)` y redirige a `/configuracion-produccion/costos-directos-adicionales` con "Costo directo adicional dado de baja correctamente". |
| **CP-CDC-14** | Rol sin permisos | Rol `OPERARIO_DE_PRODUCCION` en el alta, la edición, la modificación y la baja | `@PreAuthorize` $\rightarrow$ **Rechaza** | Redirige con la alerta `danger` en las cuatro y no invoca al service. |
| **CP-CDC-15** | Permisos del gerente de producción | `Rol.GERENTE_DE_PRODUCCION` | Permisos asignados | Tiene crear, modificar, eliminar y consultar costos directos adicionales. |
| **CP-CDC-16** | Lista de costos | Rol `GERENTE_DE_PRODUCCION`; el service devuelve "Gas butano" (`12.5000`) y "Dióxido de carbono" (`1234.5`) | La pestaña lista los costos activos | Vista `configuracion/costos-directos-lista` con título de pestaña "Configuración de producción - Maestro Cervecero", las pestañas de Configuración (la de costos activa) y el botón "Nuevo costo directo adicional" en el encabezado, a la derecha del título. Tabla con "Nombre", "Costo por litro" y "Acciones" (`col-acciones`), importes con el formato de la aplicación ("12,50" y "1.234,50"), editar y baja por fila (`data-baja-url`) y el diálogo de baja con CSRF. Pide páginas de 20 ordenadas por nombre (sin distinguir mayúsculas) e id. |
| **CP-CDC-17** | Lista vacía y permisos | Sin costos; usuario con solo `COSTO_DIRECTO_ADICIONAL_CONSULTAR`; rol `OPERARIO_DE_PRODUCCION` | Permisos por acción | Sin costos dice "No hay costos directos adicionales registrados". Con solo consultar no se muestran "Nuevo costo directo adicional", editar ni baja ni el diálogo. El operario es rechazado con la alerta `danger`. |
| **CP-CDC-18** | Paginación | `pagina=1` | Página pedida | Pide la página 1 de los costos. |
