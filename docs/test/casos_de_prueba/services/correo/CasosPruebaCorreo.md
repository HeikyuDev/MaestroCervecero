## Casos de prueba de `CorreoServicioImpl`

**Tipo de prueba:** unitarias (clase `CorreoServicioImplTest`), sin servidor de correo ni contexto de Spring. El `JavaMailSender` está simulado (mock); el motor de plantillas está simulado salvo en CP-CO-05, que procesa la plantilla real.

**Reglas:** el servicio arma un correo HTML a partir de una plantilla Thymeleaf de `templates` y sus variables, con el remitente del sistema (`app.correo.remitente` y `app.correo.nombre-remitente`), y lo envía en un hilo aparte (`@Async`). Ninguna falla (de la plantilla, de la conexión o de las credenciales) se propaga a quien lo invoca: se registra en el log.

---

### 1. `enviarCorreoHtml(String destinatario, String asunto, String plantilla, Map<String, Object> variables)`

| **ID** | **Nombre del Caso** | **Datos de Entrada (Escenario)** | **Condición Evaluada** | **Resultado Esperado** |
| --- | --- | --- | --- | --- |
| **CP-CO-01** | Envía la plantilla completada como HTML | `destinatario: "juan@mail.com"`, `asunto: "Asunto de prueba"`, `plantilla: "correo/restablecer-contrasena"`, `variables: {nombre: "Juan"}`; el motor devuelve `"<p>Hola Juan</p>"` | El motor recibe las variables y el correo se arma con los datos indicados | El motor procesa la plantilla con la variable `nombre = "Juan"`. Se envía un único correo con remitente `no-responder@maestrocervecero.com` (nombre "Maestro Cervecero"), destinatario `juan@mail.com`, el asunto indicado, tipo `text/html` y el cuerpo devuelto por el motor. |
| **CP-CO-02** | Falla del servidor de correo | El `JavaMailSender` lanza `MailSendException("Autenticación rechazada")` al enviar | La falla se atrapa dentro del servicio | No lanza ninguna excepción. |
| **CP-CO-03** | Falla de la plantilla | El motor lanza `TemplateInputException("Plantilla inexistente")` | La falla se atrapa antes de armar el correo | No lanza ninguna excepción y no envía ningún correo. |
| **CP-CO-04** | Envío asíncrono | Método `enviarCorreoHtml` de la clase | El método tiene la anotación `@Async` | La anotación está presente: quien envía no espera al servidor SMTP. |
| **CP-CO-05** | Plantilla real del restablecimiento de contraseña | Motor Thymeleaf real con las plantillas de `templates`; `variables: {nombre: "Juan", enlace: "http://localhost:7777/restablecer-contrasena?token=abc_DEF-123", minutosVigencia: 30}` | La plantilla se procesa sin request HTTP | El cuerpo enviado contiene "Hola Juan", un `href` con el enlace exacto, "30 minutos" y la marca "MAESTRO" "CERVECERO". |
