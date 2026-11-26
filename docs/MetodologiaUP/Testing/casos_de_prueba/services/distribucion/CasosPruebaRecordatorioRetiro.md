## Casos de prueba de `RecordatorioRetiroServicioImpl`

**Tipo de prueba:** unitaria con Mockito (clase `RecordatorioRetiroServicioImplTest`). Se simulan los repositorios de la configuración de distribución, de las ejecuciones del envío diario, de los recordatorios, de los despachos y de los clientes. El día de referencia es el 19/11/2026.

Un cliente recibe a lo sumo un recordatorio por día (unicidad de cliente y día, ver V35). Lista los despachos vigentes con la devolución vencida y sin solicitud de búsqueda pendiente. Cada intento de envío genera un token nuevo, del que solo se guarda el hash SHA-256; se reintenta hasta `LimitesTecnicos.INTENTOS_ENVIO_INVITACION_MAXIMO` (3) intentos. Un recordatorio que sigue en envío después de `LimitesTecnicos.ENVIO_RECORDATORIO_RETIRO_TRABADO_MINUTOS` (15) minutos se considera trabado y se retoma como uno fallido.

### 1. Hora de envío y ejecución del día

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RRS-01**|Hora de envío alcanzada _(Límite)_|Hora configurada 09:00; momentos 08:59:59, 09:00 y 23:30|`ahora.toLocalTime() >= hora`|Falso a las 08:59:59 y verdadero a las 09:00 y a las 23:30.|
|**CP-RRS-02**|Ejecución del día registrada|El 19/11 tiene ejecución registrada y el 20/11 no|`existsById(fecha)`|Verdadero para el 19/11 y falso para el 20/11.|
|**CP-RRS-03**|Clientes a invitar y envíos pendientes|Clientes 5 y 8 a invitar; cliente 8 con el envío del día pendiente|Se consultan con el día, el máximo de 3 intentos y el límite de los envíos trabados|Devuelve `[5, 8]` y `[8]`, respectivamente; el límite es el momento actual menos 15 minutos.|

### 2. `prepararRecordatorio(Long idCliente, LocalDate fecha)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RRS-04**|Cliente inexistente o dado de baja|Cliente 5 inexistente; cliente 6 en `BAJA`|El cliente no existe o no está `ACTIVO`|No prepara nada y no consulta recordatorios ni despachos.|
|**CP-RRS-05**|Recordatorio nuevo _(Camino feliz)_|Cliente 5 activo sin recordatorio del día, con 2 despachos vencidos (SN-1001 BRL-01 y SN-1002 BRL-02), 7 días de anticipación|No existe recordatorio del día y hay vencidos|Guarda un recordatorio del cliente para el día con los 2 despachos, `EN_ENVIO`, 1 intento, expiración en ahora + 7 días y el hash del token devuelto (43 caracteres). Devuelve el correo y el nombre del cliente y los barriles "N.º de serie SN-1001 (BRL-01)" y "N.º de serie SN-1002 (BRL-02)".|
|**CP-RRS-06**|Sin barriles que recordar|Cliente 5 sin recordatorio del día ni despachos vencidos sin solicitud pendiente|No hay vencidos|No crea ningún recordatorio.|
|**CP-RRS-07**|Reintento de un recordatorio fallido|Recordatorio del día `FALLIDA` con 1 intento y un hash anterior; 1 despacho vencido|El recordatorio es reintentable|Queda `EN_ENVIO` con 2 intentos, un hash nuevo (el del token devuelto) y el despacho actual.|
|**CP-RRS-08**|Recordatorio del día no reintentable|Recordatorio del día `ENVIADA`; `EN_ENVIO` con el último intento hace 14 minutos; `FALLIDA` con 3 intentos; `OMITIDA`; `EN_ENVIO` trabado (30 minutos) con 3 intentos, por separado|No es `PENDIENTE`, `FALLIDA` ni `EN_ENVIO` trabado, o agotó los intentos|No prepara nada, no consulta los despachos ni guarda el recordatorio.|
|**CP-RRS-13**|Recuperación de un envío trabado|Recordatorio del día `EN_ENVIO` con 1 intento, el último hace 20 minutos y un hash anterior; 1 despacho vencido|El envío quedó trabado (más de 15 minutos)|Queda `EN_ENVIO` con 2 intentos, la fecha del último intento actual y un hash nuevo (el del token devuelto).|
|**CP-RRS-09**|Fallido sin barriles que recordar|Recordatorio del día `FALLIDA` con 1 intento; sin despachos vencidos|No hay vencidos|El recordatorio queda `OMITIDA` y se persiste; no prepara nada.|

### 3. `registrarResultadoEnvio(Long idInvitacion, boolean enviado)` y `registrarEjecucion(LocalDate fecha)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RRS-10**|Resultado de un envío en curso|Recordatorios 90 y 91 `EN_ENVIO`; resultados aceptado y fallido|El recordatorio está en envío|El 90 queda `ENVIADA` y el 91 `FALLIDA`; ambos se persisten.|
|**CP-RRS-11**|Recordatorio que ya no está en envío o inexistente|Recordatorio 90 `OMITIDA`; recordatorio 99 inexistente|No está en envío o no existe|No cambia ni persiste nada.|
|**CP-RRS-12**|Ejecución del día una sola vez|Se registra dos veces el 19/11|Inserción que no hace nada si el día ya existe|Cada registro llama a `registrarSiNoExiste` con el 19/11 y la fecha de fin (una sola sentencia `ON CONFLICT DO NOTHING`, que no falla si otro proceso ya lo registró); nunca usa `existsById` ni `save`.|
