## Casos de prueba de `RetiroBarrilesServicioImpl`

**Tipo de prueba:** unitaria con Mockito (clase `RetiroBarrilesServicioImplTest`). Se simulan los repositorios de los recordatorios, de la configuración de distribución, de los despachos, de las solicitudes de búsqueda y de los barriles, y el notificador de correos. El cliente es "Bar Don Jorge" (ID 5) y el recordatorio tiene el ID 90.

Un enlace sin responder sirve hasta su fecha de expiración, con el cliente activo y sin un recordatorio posterior enviado o en envío desde hace menos de 15 minutos (un envío trabado no invalida el enlace). Un enlace respondido muestra lo registrado y no admite cambios. La fecha y hora del retiro es obligatoria, no puede ser anterior a la actual ni superar los días máximos de anticipación configurados.

### 1. Validación del enlace y `buscarRetiro(String token)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RBS-01**|Token con formato imposible|Token nulo, en blanco y de 129 caracteres, por separado|El token falta o es demasiado largo|Lanza `EnlaceRetiroInvalidoException` ("El enlace para coordinar el retiro de barriles no es válido") sin consultar la base.|
|**CP-RBS-02**|Token desconocido|Token sin recordatorio|`buscarIdPorTokenHash` $\rightarrow$ **Vacío**|Lanza `EnlaceRetiroInvalidoException` con el mismo mensaje.|
|**CP-RBS-03**|Enlace que no sirve|Recordatorio vencido; de un cliente en `BAJA`; reemplazado por uno posterior enviado, por separado|Cada motivo $\rightarrow$ **TRUE**|Lanza `EnlaceRetiroInvalidoException` con "El enlace para coordinar el retiro de barriles venció", "El enlace ya no está habilitado para coordinar el retiro de barriles" y "El enlace fue reemplazado por uno más reciente: use el enlace del último correo recibido", respectivamente.|
|**CP-RBS-04**|Barriles pendientes _(Camino feliz)_|Recordatorio vigente con 3 despachos: SN-1002 (devolución 15/11), SN-1001 (10/11) y uno ya devuelto; 7 días de anticipación|Solo dos siguen pendientes de retiro|`registrado: false`; barriles SN-1001 y SN-1002 en ese orden; `diasMaximosAnticipacionRetiro: 7` y fecha máxima en ahora + 7 días.|
|**CP-RBS-05**|Enlace respondido|Recordatorio respondido ayer, con una solicitud buscada de SN-1001 para dentro de 2 días con "Tocar timbre"|`fechaRespuesta != null`|`registrado: true` con la fecha de respuesta, la del retiro, las observaciones y el barril SN-1001 con su solicitud `BUSCADA`. No verifica si hay un recordatorio posterior.|

### 2. `registrarRetiro(String token, RetiroBarrilesFormDTO retiroBarrilesFormDTO)`

Orden: bloqueo del recordatorio, validación del enlace, recordatorio sin responder, datos, fecha, observaciones; luego el bloqueo de cada barril en orden de ID y la verificación de que su despacho siga pendiente de retiro.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RBS-06**|Enlace ya respondido|Recordatorio respondido hace una hora|`fechaRespuesta != null`|Lanza `ReglaNegocioException` ("El retiro ya fue coordinado desde este enlace y no se puede modificar"); no bloquea barriles, no registra ni avisa.|
|**CP-RBS-07**|Enlace reemplazado al confirmar|Recordatorio sin responder con uno posterior enviado|Se valida con el recordatorio bloqueado|Lanza `EnlaceRetiroInvalidoException`; no registra ni avisa.|
|**CP-RBS-08**|Datos inválidos|Datos nulos; fecha nula; fecha 5 minutos atrás; fecha en ahora + 7 días y 5 minutos; observaciones de 256 caracteres, por separado|Cada regla $\rightarrow$ **TRUE**|Lanza "Los datos del retiro son obligatorios", "La fecha y hora del retiro es obligatoria", "La fecha y hora del retiro no puede ser anterior a la fecha y hora actual", "La fecha y hora del retiro no puede superar 7 días desde la fecha y hora actual" (campo `fechaRetiro` en las tres) y "Las observaciones no pueden superar 255 caracteres" (campo `observaciones`). No bloquea barriles ni registra.|
|**CP-RBS-09**|Ningún barril pendiente|El único despacho ya no está pendiente de retiro|Ningún despacho cumple|Lanza `ReglaNegocioException` ("Ninguno de los barriles sigue pendiente de retiro: ya fueron devueltos o su retiro ya está coordinado"); no registra ni avisa.|
|**CP-RBS-10**|Registro del retiro _(Camino feliz)_|3 despachos (barriles 202, 203 y 201), dos pendientes; fecha en ahora + 5 días; observaciones "  Tocar   timbre  "; expiración en ahora + 3 días|Todas las validaciones $\rightarrow$ **FALSE**|Bloquea los barriles 201, 202 y 203 en ese orden; registra 2 solicitudes pendientes (SN-1001 y SN-1002) con la fecha, "Tocar timbre" y el recordatorio; el recordatorio queda respondido con la expiración extendida a la fecha del retiro; avisa a los encargados con el cliente, los dos barriles, la fecha y las observaciones.|
|**CP-RBS-11**|Expiración posterior y observaciones en blanco|Expiración en ahora + 6 días, fecha en ahora + 1 día, observaciones "   "|La expiración es posterior a la fecha del retiro|La expiración no cambia y las observaciones se guardan nulas.|
|**CP-RBS-12**|Recordatorio posterior trabado|Recordatorio vigente sin responder; ningún recordatorio posterior enviado ni en envío reciente|`existePosteriorEnviada` con el límite de los envíos trabados|El enlace sirve; la consulta recibe como límite el momento actual menos 15 minutos.|
|**CP-RBS-13**|Aviso solo al confirmar la transacción|Registro válido con una transacción activa que, después de guardar las solicitudes, se revierte; luego el mismo caso confirmado|El aviso se registra para después de la confirmación|Con la reversión no se envía ningún aviso; con la confirmación se avisa a los encargados con el cliente y el barril.|
