## Casos de prueba del proceso de los recordatorios de retiro

**Tipo de prueba:** unitaria con Mockito (clase `ProcesoRecordatoriosRetiroTest`, con una clase anidada por pieza): `EnvioRecordatoriosRetiro` (envío de un recordatorio en tres pasos), `EnvioRecordatoriosRetiroProgramado` (proceso programado de cada minuto) y `NotificadorRetiroBarriles` (correos). Se simulan el service de los recordatorios, el notificador, el servicio de correo y el repositorio de usuarios.

### 1. Envío de un recordatorio

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-PRR-01**|Envío aceptado _(Camino feliz)_|Recordatorio 90 preparado para el cliente 5; el servidor acepta el correo|Orden de los pasos|Prepara, envía el correo con el correo, el nombre, el token, la expiración y los barriles, y registra el resultado aceptado, en ese orden.|
|**CP-PRR-02**|Envío fallido|El servidor rechaza el correo y, en un segundo envío, el notificador lanza una excepción|El correo no salió|En ambos casos registra el resultado como fallido.|
|**CP-PRR-03**|Nada que enviar|`prepararRecordatorio` vacío|No corresponde enviar|No envía ningún correo ni registra resultado.|

### 2. Proceso programado

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-PRR-04**|Antes de la hora de envío|`alcanzoLaHoraDeEnvio` falso|No es la hora|No busca clientes ni envía nada.|
|**CP-PRR-05**|Primera corrida del día|Desde la hora; cliente 3 con el envío pendiente; el día sin ejecución; clientes 5 y 8 a invitar, sin fallas|El día no se envió|Envía al 3, al 5 y al 8, en ese orden, y registra la ejecución del día.|
|**CP-PRR-09**|Un cliente falla|El día sin ejecución; clientes 5 y 8 a invitar; el envío del cliente 5 lanza una excepción|Hubo una falla en el recorrido|Envía igual al 8, pero no registra la ejecución: el día queda abierto y la próxima corrida retoma a los clientes sin recordatorio.|
|**CP-PRR-10**|Cruce de la medianoche|Reloj fijo en el 19/11/2026 23:59:59.999; cliente 5 a invitar|La corrida toma el momento una sola vez|La hora de envío se evalúa con ese momento y todas las consultas, el envío y la ejecución usan el 19/11, nunca el 20/11.|
|**CP-PRR-06**|Día ya enviado|Desde la hora; cliente 3 con el envío pendiente; el día con ejecución|El día ya se envió|Solo reintenta al 3; no busca clientes a invitar ni registra la ejecución.|

### 3. Correos

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-PRR-07**|Recordatorio al cliente|Cliente "Bar Don Jorge", token, expiración 26/11/2026 09:00, barril "N.º de serie SN-1001 (BRL-01)"|El servidor acepta el correo|Usa `correo/recordatorio-retiro-barriles` con el asunto "Coordinación del retiro de barriles de Maestro Cervecero", el enlace `<url-base>/retiro-barriles/<token>`, la expiración "26/11/2026 09:00" y los barriles; devuelve verdadero.|
|**CP-PRR-08**|Aviso a los encargados de distribución|Dos encargados de distribución activos; retiro del 25/11/2026 10:30 con "Tocar timbre"|`findByRolAndEstado(ENCARGADO_DE_DISTRIBUCION, ACTIVO)`|Envía `correo/aviso-solicitud-retiro` con el asunto "Retiro de barriles coordinado por un cliente" a cada uno, con su nombre, el cliente, la fecha "25/11/2026 10:30", las observaciones y el enlace `<url-base>/solicitudes-retiro`.|
