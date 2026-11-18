## Casos de prueba de `CotizacionProveedorController`

**Tipo de prueba:** capa web (`@WebMvcTest`, clase `CotizacionProveedorControllerTest`), con Spring Security (incluida la cadena pública de `/cotizacion/**`), el `ControllerAdvices` y las plantillas reales; el `ICotizacionProveedorServicio` está simulado. La lógica se prueba en `services/cotizacion/CasosPruebaCotizacionProveedor.md`.

**Acceso:** pública, sin iniciar sesión; el token del enlace viaja en la dirección (`/cotizacion/{token}`) y el formulario conserva la protección CSRF. Es una pantalla aparte, sin el menú de la aplicación.

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CPC-01** | Pantalla pública con la respuesta actual | Usuario anónimo; cotización de MALTERIA SUR que vence el 20/11/2026 09:00, respondida el 19/11/2026 10:15; malta (Bolsa de 25 kg, $ 32.500, 4 unidades) y lúpulo | Enlace vigente | Responde 200 con `cotizacion/cotizacion-proveedor` sin menú: fechas, formulario `novalidate` con token CSRF hacia `/cotizacion/{token}`, una fila por insumo con la cantidad solicitada, la presentación elegida, "No cotizo este insumo", el precio y la cantidad cargados, placeholders, "Enviar cotización" y la política `no-referrer`. |
| **CP-CPC-02** | Sin respuesta previa | Cotización sin respuesta | Respuesta previa $\rightarrow$ **No** | Muestra "Todavía no respondió" y las filas vacías. |
| **CP-CPC-03** | Enlace que ya no sirve | El service lanza "La solicitud de cotización ya cerró: el plazo para responder venció" | Enlace vigente $\rightarrow$ **No** | Responde 200 con `cotizacion/enlace-invalido`, el título "El enlace no está disponible" y el motivo. |
| **CP-CPC-04** | Envío correcto | Fila de la malta completa y fila del lúpulo vacía | Formulario válido | Delega las dos filas en el service y redirige a `/cotizacion/{token}` con "Cotización enviada correctamente: puede modificarla hasta el 20/11/2026 09:00". |
| **CP-CPC-05** | Regla de una fila | El service lanza "La cantidad disponible de Malta Pilsen es obligatoria" con el campo `ofertas[0].cantidadDisponible` | Regla con campo | Vuelve al formulario con "No se pudo enviar la cotización", el mensaje, lo cargado y el campo marcado. |
| **CP-CPC-06** | Valores fuera de rango | Precio 0 y cantidad 1.000.001 | Bean Validation | Muestra "El precio unitario debe ser mayor a 0" y "La cantidad disponible no puede superar 1.000.000" sin llamar al registro. |
| **CP-CPC-07** | El enlace dejó de servir al enviar | El registro lanza "La solicitud de cotización fue cancelada: ya no se reciben respuestas" | Enlace vigente $\rightarrow$ **No** | Muestra la pantalla del enlace no disponible con el motivo. |
| **CP-CPC-08** | Envío sin token CSRF | `POST` sin token CSRF | CSRF válido $\rightarrow$ **No** | Responde 403 y no llama al service. |
| **CP-CPC-09** | Cabeceras de la pantalla pública | `GET /cotizacion/{token}` vigente | Cadena pública | Responde con `Referrer-Policy: no-referrer` y sin crear la sesión de la aplicación (no hay cookie `JSESSIONID`). |
