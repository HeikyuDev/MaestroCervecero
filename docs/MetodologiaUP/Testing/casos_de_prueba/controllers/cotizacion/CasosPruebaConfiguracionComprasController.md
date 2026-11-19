## Casos de prueba de `ConfiguracionComprasController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `ConfiguracionComprasControllerTest`). Se ejecutan sobre Spring MVC, Spring Security (`SecurityConfig`, con CSRF y `@PreAuthorize`), el `ControllerAdvices` y las plantillas Thymeleaf reales; el `IConfiguracionComprasServicio` está simulado y no hay base de datos. La lógica de negocio se prueba en `services/cotizacion/CasosPruebaConfiguracionCompras.md`.

**Rol requerido:** `GERENTE_DE_COMPRAS` en toda la clase. La pantalla está en la sección "Configuración" del menú.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CCC-01** | Pantalla con la configuración vigente | Rol `GERENTE_DE_COMPRAS`; plazo 48, `PONDERADO`, peso 50 | Rol autorizado | Responde 200 con `configuracion/configuracion-compras`: formulario `novalidate`, secciones "Solicitudes de cotización" y "Plan de órdenes de compra", asterisco en los tres campos, valores 48 y 50 (nunca 50.0), placeholders, el criterio elegido entre los tres (sin opción vacía "Seleccione criterio"), las ayudas de rango y el enlace del menú; sin resumen de errores. |
| **CP-CCC-02** | Guardado correcto | 24 h, `MENOR_PRECIO`, peso 70,5 | Formulario válido | Delega los tres valores en el service y redirige a `/configuracion-compras` con "Configuración de compras actualizada correctamente" (success). |
| **CP-CCC-03** | Campos vacíos | Los tres campos vacíos | Bean Validation | Vuelve al formulario (200) con "No se pudo guardar la configuración" y el mensaje obligatorio de cada campo; no llama al service. |
| **CP-CCC-04** | Topes y rangos | 721 h, peso 100,5; y 0 h, peso -1 | Bean Validation | Muestra "no puede superar 720 horas", "debe ser mayor a 0" y "El peso del precio debe estar entre 0 y 100"; no llama al service. |
| **CP-CCC-05** | Regla rechazada por el service | El service lanza "El peso del precio debe estar entre 0 y 100" | Regla rechazada | Vuelve al formulario con el motivo y conserva lo tipeado. |
| **CP-CCC-06** | Otros roles | `GERENTE_DE_PRODUCCION`, `ENCARGADO_DE_DEPOSITO`, `ADMINISTRADOR`, `OPERARIO_DE_PRODUCCION`, `ENCARGADO_DE_DISTRIBUCION` | Rol no autorizado | `GET` y `POST` redirigen (acceso denegado) y nunca se actualiza la configuración. |
