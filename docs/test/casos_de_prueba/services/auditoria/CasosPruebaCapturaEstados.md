## Casos de prueba de la captura de estados de la bitácora

**Tipo de prueba:** unitarias (clases `SerializadorEstadoEntidadTest`, `ComparadorEstadosAuditoriaTest`, `RegistroEstadosEntidadTest`, `CapturaEstadoInterceptorTest` y `AuditAspectTest`), sin base de datos. Cubren cómo se arma el JSON del estado anterior y del posterior de un registro auditado, cómo se acumulan durante la operación, cómo se asignan a la entrada de la bitácora y cómo se comparan para el detalle.

**Reglas:** el JSON es un objeto plano con los campos persistentes; las asociaciones van como el id de la entidad asociada (`<campo>Id`); no se recorren colecciones ni grafos; los campos secretos se omiten. Un alta solo tiene estado posterior, una eliminación física solo anterior, y la baja lógica es una modificación del estado (los dos estados).

### 1. `SerializadorEstadoEntidad`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-SE-01**|Tipos escalares|Valores `Integer`, `Long`, `Double`, `BigDecimal`, `Boolean`, `String`, un enumerado, `LocalDate` y `LocalDateTime`|Cada tipo se copia al mapa|Los números, el booleano y el texto se conservan; el enumerado va por su nombre; las fechas en ISO-8601 (`2026-11-13`, `2026-11-13T10:30:15`). El orden de las propiedades se conserva.|
|**CP-SE-02**|Valores nulos|`telefono: null`, `nombre: "Ana"`|Un valor sin dato|El campo `telefono` queda en el mapa con valor `null`.|
|**CP-SE-03**|Asociación|Una entidad asociada `fabricante` con id 3|Una asociación no se serializa completa|El mapa tiene `fabricanteId: 3` y ningún campo `fabricante`.|
|**CP-SE-04**|Proxy perezoso|Un proxy de Hibernate con identificador 7|No se debe inicializar ni recorrer|El mapa tiene `loteId: 7`; solo se consulta el identificador del proxy, sin inicializarlo.|
|**CP-SE-05**|Colecciones y no escalares|Propiedades de tipo colección (por su `Type` y por su valor), un mapa, un arreglo binario y un objeto cualquiera, más un `nombre`|Solo se copian escalares y ids|El mapa tiene solo `nombre`.|
|**CP-SE-06**|Campo anotado como excluido|`claveInterna` anotado con `@ExcluidoDeAuditoria` (también heredado de una superclase) y `nombre`|La anotación excluye el campo|El mapa tiene solo `nombre`.|
|**CP-SE-07**|Nombres sensibles|Campos `password`, `passwordTemporal`, `Contrasena`, `apiSecret`, `tokenRecuperacion`, `hashClave`, `claveAcceso`, `pinSeguridad`, `apiKey`, `credencialExterna`, `otpActual` y `nombre`|El nombre contiene `password`, `contrasena`, `contraseña`, `secret`, `token`, `hash`, `clave`, `pin`, `apikey`, `credencial` u `otp` (sin distinguir mayúsculas)|El mapa tiene solo `nombre`, aunque los campos no estén anotados.|
|**CP-SE-08**|Contraseña del usuario|`UsuarioEntity` con `username`, `password` (un hash BCrypt) y `nombre`|La contraseña nunca llega al JSON|`UsuarioEntity.password` lleva `@ExcluidoDeAuditoria`; el JSON no contiene ni el nombre del campo ni el hash.|
|**CP-SE-09**|Todas las entidades|Todas las clases `@Entity` del paquete de entidades|Ningún campo de nombre sensible queda sin la anotación|Todo campo cuyo nombre es sensible lleva `@ExcluidoDeAuditoria`.|
|**CP-SE-10**|Entradas nulas y copia|Nombres y valores `null`; y un arreglo de valores que se modifica después de serializar|El serializador tolera datos faltantes y no depende del arreglo de Hibernate|Con entradas nulas devuelve un mapa vacío; el mapa es una copia independiente del arreglo.|
|**CP-SE-11**|Tipos del JSON|Un estado con entero, decimal, booleano, texto, nulo y fecha y hora|`aJson` escribe tipos nativos de JSON|El entero y el decimal son números JSON, el booleano es booleano, el texto es texto, el nulo es `null` y la fecha es un texto ISO.|
|**CP-SE-12**|Estado nulo|`aJson(null)`|No hay estado|Devuelve `null`.|
|**CP-SE-13**|Estado no serializable|Un mapa que se contiene a sí mismo|La falla de Jackson no debe propagarse|Devuelve `null` y no lanza excepción.|
|**CP-SE-14**|Datos personales de contacto|`UsuarioEntity` (`correo`, `telefono`), `ClienteEntity` y `VersionProveedorEntity` (`email`, `telefono`, `direccion`); y una versión de proveedor con `cuit`, `razonSocial`, `email`, `telefono` y `direccion`|Los datos personales no se copian a la bitácora; los datos de negocio sí|Todos esos campos llevan `@ExcluidoDeAuditoria`; el mapa de la versión de proveedor tiene solo `cuit` y `razonSocial`.|

### 2. `ComparadorEstadosAuditoria.comparar(estadoAnterior, estadoPosterior)`

Devuelve `tieneDetalle`, los campos comparados (en el orden del estado anterior y, después, los que solo existen en el posterior) El valor se muestra ya formateado: `null` si no hay dato, "Sí" y "No" para los booleanos, `dd/MM/yyyy` y `dd/MM/yyyy HH:mm:ss` para las fechas y el formato de cantidades de la aplicación para los números (separador de miles `.`, decimal `,`, hasta cuatro decimales y sin ceros sobrantes; los identificadores, `id` y los campos terminados en `Id`, se muestran sin separador de miles).

**Regla de comparación:** un campo con valor `null` en los dos estados no cambió; un campo que existe en un solo estado, aunque su valor sea `null`, es agregado o eliminado. Dos números son iguales si tienen el mismo valor, aunque sean de distinto tipo (`1` y `1.0`).

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CE-01**|Modificado y sin cambios|anterior `{"id":5,"nombre":"Pilsen","potencialExtracto":80}`, posterior con `"Pilsen Premium"`|Campos presentes en los dos estados|"ID" y "Potencial extracto" `SIN_CAMBIOS`; "Nombre" `MODIFICADO` de "Pilsen" a "Pilsen Premium", en el orden guardado.|
|**CP-CE-02**|Alta|anterior `null`, posterior `{"id":9,"nombre":"Cascade"}`|No hay estado anterior|`tieneDetalle: true` y todos los campos `AGREGADO` con valor anterior nulo.|
|**CP-CE-03**|Sin estado posterior|anterior `{"id":9,"nombre":"Cascade"}`, posterior `null`|No hay estado posterior|Todos los campos `ELIMINADO` con valor posterior nulo.|
|**CP-CE-04**|Campo agregado y campo eliminado|anterior `{"id":1,"viejo":"x"}`, posterior `{"id":1,"nuevo":"y"}`|Un campo existe solo en un estado|`SIN_CAMBIOS`, `ELIMINADO` ("Viejo") y `AGREGADO` ("Nuevo"), en ese orden.|
|**CP-CE-05**|Null contra un valor|anterior `{"telefono":null,"correo":null}`, posterior `{"telefono":"1155","correo":null}`|Un `null` no es lo mismo que un valor|"Telefono" `MODIFICADO` (anterior nulo, posterior "1155"); "Correo" `SIN_CAMBIOS`.|
|**CP-CE-06**|Booleanos y fechas|`activo`, `fecha` y `fechaHora` con distintos valores|Formato de presentación|"Sí" y "No"; `13/11/2026` y `14/11/2026`; `13/11/2026 10:30:15` y `13/11/2026 10:30:00`.|
|**CP-CE-07**|Números y textos parecidos a una fecha|`cantidad` 12.5 y 13; `stock` 2000.0 y 1847.3685; `id` 1234 y `fabricanteId` 5678; `codigo` `"2026-13-45"` igual en los dos|Los números usan el formato de la aplicación y un texto que no es una fecha válida se muestra tal cual|"12,5" y "13"; "2.000" y "1.847,3685"; los identificadores "1234" y "5678" sin separador de miles; el código se conserva y queda `SIN_CAMBIOS`.|
|**CP-CE-08**|JSON ausente o inválido|`null`, vacío, en blanco, `{no es json`, `[1,2,3]`, `"texto"` y `42` en los dos estados|Un JSON que no es un objeto legible es un estado inexistente|`tieneDetalle: false` y sin campos. No lanza excepción.|
|**CP-CE-09**|Un solo JSON legible|anterior `{rota`, posterior `{"id":2,"nombre":"Ana"}`|Solo uno de los estados se puede leer|`tieneDetalle: true` y todos los campos `AGREGADO`.|
|**CP-CE-10**|Contenido de la comparación|anterior `{"id":1}`, posterior `{"id":2}`|Solo se devuelven los datos que muestra la tabla|La comparación expone únicamente `tieneDetalle` y `campos`; hay 1 campo.|
|**CP-CE-11**|Etiquetas de campo|`potencialExtracto`, `fabricanteId`, `idEtapaLote`, `id`, `createdBy`, `lastModifiedDate`, `nombre` y un nombre en blanco|El nombre se muestra legible|"Potencial extracto", "Fabricante (ID)", "Id etapa lote", "ID", "Creado por", "Fecha de modificación" y "Nombre"; el nombre en blanco no cambia.|
|**CP-CE-12**|Null contra campo ausente|anterior `{"a":null,"b":null}`, posterior `{"a":null,"c":null}`|Un campo con `null` en los dos estados no cambió; uno que existe en un solo estado es agregado o eliminado aunque valga `null`|"A" `SIN_CAMBIOS`, "B" `ELIMINADO` y "C" `AGREGADO`, todos con valores nulos.|
|**CP-CE-13**|Números de igual valor|anterior `{"a":1,"b":2.50,"c":1}`, posterior `{"a":1.0,"b":2.5,"c":1.5}`|Los números se comparan por valor, sin importar el tipo del nodo|"A" y "B" `SIN_CAMBIOS`; "C" `MODIFICADO` con "1,5".|

### 3. `RegistroEstadosEntidad`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-RE-01**|Sin captura abierta|Una modificación registrada sin llamar a `iniciar`|No hay un método auditado en curso|No registra nada: `activo` es `false` y `resolver` devuelve vacío.|
|**CP-RE-02**|Abrir y cerrar|`iniciar` dos veces y `finalizar`|Solo el primero abre la captura|La primera llamada devuelve `true` y la segunda `false`; `finalizar` cierra y descarta lo acumulado.|
|**CP-RE-03**|Modificación|Entidad `MaltaEntity` 5 con estado anterior y posterior|Se resuelve por concepto `MALTA` e id "5"|Devuelve los dos estados con el `id` como primer campo.|
|**CP-RE-04**|Alta|Entidad sin id al registrarse, que recibe el id 9 después de insertarse|El id se toma de la entidad al resolver|Estado anterior `null` y posterior con `id: 9`.|
|**CP-RE-05**|Eliminación física|Entidad con estado anterior|Solo existe el estado anterior|Posterior `null`.|
|**CP-RE-06**|Varios cambios de la misma entidad|Dos modificaciones: 1 a 2 y 2 a 3|Se resumen en un solo par|Anterior con 1 (el del primer cambio) y posterior con 3 (el del último).|
|**CP-RE-07**|Alta y luego modificación|Alta con 1 y modificación de 1 a 4|Una entidad creada en la operación no tiene estado anterior|Anterior `null` y posterior con 4.|
|**CP-RE-08**|Modificación y luego eliminación|Modificación de 1 a 2 y eliminación|El posterior desaparece|Anterior con 1 y posterior `null`.|
|**CP-RE-09**|Mismo id en otro tipo|Una olla de hervor y una malta, ambas con id 5, con los conceptos `MALTA` y `OLLA_DE_HERVOR` admitidos|Se distingue por el concepto|`MALTA` devuelve el estado de la malta, `OLLA_DE_HERVOR` el de la olla y `LUPULO` devuelve vacío.|
|**CP-RE-10**|Sin coincidencia|Id distinto, id nulo, concepto nulo y concepto `SESION`|No hay un cambio que atribuir|`resolver` devuelve vacío.|
|**CP-RE-11**|Entidades nulas|Registros con entidad `null`|Se ignoran|No registra nada.|
|**CP-RE-12**|Aislamiento por hilo|La captura se abre en un hilo y se consulta desde otro|El estado es propio de cada hilo|`activo` es `false` en el otro hilo.|
|**CP-RE-13**|Correspondencia concepto y entidad|`MALTA` con `MaltaEntity`, `OLLA_DE_HERVOR` con `OllaHervorEntity`, `LUPULO` con `MaltaEntity`|El nombre de la clase sin `Entity` es igual al concepto sin guiones bajos ni la palabra `DE`|`true`, `true` y `false`.|
|**CP-RE-14**|Todos los conceptos auditados|Todos los conceptos usados por algún método `@AuditableAction` de los services|Cada concepto debe corresponder a una entidad|Cada concepto coincide con exactamente una clase `@Entity`.|
|**CP-RE-15**|Solo conceptos admitidos|Captura abierta con `MALTA`; se registra una olla de hervor y una malta; luego un método anidado admite `OLLA_DE_HERVOR`|Solo se retienen las entidades de los conceptos admitidos, para acotar la memoria|La olla registrada antes de admitir su concepto no se retiene; la malta sí; después de admitirlo, los cambios de la olla se retienen. Iniciar con una captura abierta devuelve `false`.|

### 4. `CapturaEstadoInterceptor`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CI-01**|Actualización sin contraseña|`onFlushDirty` de un `UsuarioEntity` con estado actual y previo, cada uno con `password`|Los dos estados se capturan sin secretos|Devuelve `false` (no modifica la entidad); el anterior tiene "Ana" y el posterior "Ana María"; ningún estado contiene `password` ni los hashes.|
|**CP-CI-02**|Estado previo desconocido|`onFlushDirty` con `previousState` nulo|Hibernate no conoce el estado previo|Anterior `null` y posterior con los valores actuales.|
|**CP-CI-03**|Altas|`onPersist` y `onSave`|Un alta tiene solo posterior|Anterior `null` y posterior con los valores; el id se lee al resolver.|
|**CP-CI-04**|Eliminaciones|`onRemove` y `onDelete`|Una eliminación tiene solo anterior|Posterior `null` y anterior con los valores.|
|**CP-CI-05**|Sin captura abierta|Los callbacks sin un método auditado en curso|No se hace trabajo innecesario|El serializador no se invoca.|
|**CP-CI-06**|La bitácora no se audita|`onSave` de un `AuditLogEntity`|La bitácora no se captura a sí misma|No registra nada.|
|**CP-CI-07**|Falla al capturar|El serializador lanza una excepción|Una falla no debe romper el guardado|Devuelve `false`, no propaga la excepción y no registra el cambio. El log solo informa la entidad y el tipo de la excepción, nunca su mensaje ni su traza (pueden citar valores del estado).|
|**CP-CI-08**|Registro del interceptor|`AuditConfig.capturaEstadoCustomizer`|El interceptor se registra para todo Hibernate|Agrega la propiedad `hibernate.session_factory.interceptor` con el interceptor.|

### 5. `AuditAspect`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-AA-01**|Modificación|El método registra una modificación de la malta 5 y devuelve un resultado con `getId() = 5`|Los estados salen de la captura|La entrada guardada tiene acción `MODIFICAR`, concepto `MALTA`, `entidadId: "5"`, el estado anterior y el posterior en JSON con el `id` primero.|
|**CP-AA-02**|Alta|El método registra un alta|Un alta no tiene anterior|`estadoAnterior` nulo y `estadoPosterior` con el JSON.|
|**CP-AA-03**|Baja lógica|El método registra el cambio de `estado` de `ACTIVO` a `BAJA`|La baja lógica es una modificación|Los dos estados están en la entrada.|
|**CP-AA-04**|Sin cambios|El método no cambia ninguna entidad|No hay estados que guardar|La entrada se guarda igual, con los dos estados nulos, y el resultado del método se devuelve.|
|**CP-AA-05**|Otro registro|El método cambia la malta 99 y devuelve el id 5|Solo se atribuye el registro auditado|Los dos estados quedan nulos.|
|**CP-AA-06**|El método falla|El método lanza una excepción|Lo que no se completó no se audita|La excepción se propaga, no se guarda nada y la captura queda cerrada.|
|**CP-AA-07**|Ciclo de la captura|Se consulta `activo` durante el método y después|La captura vive durante la operación|Está abierta durante el método y cerrada al terminar.|
|**CP-AA-08**|Método anidado|La captura ya estaba abierta por un método externo|Solo el que abrió la captura la cierra|Después del método anidado la captura sigue abierta.|
|**CP-AA-09**|Transacción externa que se confirma|Hay una transacción externa activa; el flush ocurre después del método auditado|El registro se difiere al commit|No se guarda antes del commit; la captura sigue abierta; al confirmarse se guarda la entrada con los estados del flush y la captura se cierra al terminar la transacción.|
|**CP-AA-10**|Transacción externa que se revierte|Hay una transacción externa que se revierte|Lo revertido no se audita|No se guarda nada y la captura se cierra.|
|**CP-AA-11**|Falla al completar los estados|La captura lanza una excepción al resolver|La bitácora no depende del detalle|La entrada se guarda igual con los dos estados nulos.|
|**CP-AA-12**|Falla al despachar la entrada|`guardar` lanza `TaskRejectedException` (cola asíncrona llena)|La operación ya se confirmó|El método auditado devuelve su resultado sin error y la captura se cierra.|
|**CP-AA-13**|Falla al despachar después del commit|Transacción externa; `guardar` lanza `TaskRejectedException` en `afterCommit`|Una falla de la bitácora no afecta a la operación confirmada|`afterCommit` no lanza excepción y la captura se cierra al terminar la transacción.|
|**CP-AA-14**|Falla al registrar la sincronización|`registerSynchronization` lanza una excepción|Sin sincronización nadie cerraría la captura|La operación devuelve su resultado, la captura queda cerrada y no se guarda la entrada.|
