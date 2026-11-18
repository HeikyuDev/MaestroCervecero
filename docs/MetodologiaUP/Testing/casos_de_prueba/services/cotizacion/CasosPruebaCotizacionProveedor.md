## Casos de prueba de `CotizacionProveedorServicioImpl`

**Tipo de prueba:** unitaria (clase `CotizacionProveedorServicioImplTest`), con los repositorios simulados. Es el service de la pantalla pública con la que un proveedor responde una solicitud de cotización desde el enlace del correo.

**Datos comunes:** solicitud 90 abierta que vence en 5 horas con tres insumos (Malta Pilsen 100 kg, Cascade 500 g y US-05 50 g); proveedor 1 activo cuyo catálogo vigente ofrece la malta en "Bolsa" de 25 kg (100) y "Balde" de 5 kg (101) y el lúpulo en "Paquete" de 1 kg (102), pero no la levadura; el enlace del proveedor vence con la solicitud.

### 1. Validación del enlace (`buscarCotizacion` y `registrarCotizacion`)

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CPV-01**|Token inválido|Token nulo, vacío, en blanco, de 129 caracteres o desconocido|Enlace existente $\rightarrow$ **No**|Lanza `EnlaceCotizacionInvalidoException` ("El enlace de la solicitud de cotización no es válido").|
|**CP-CPV-02**|Solicitud cancelada|Solicitud `CANCELADA`|Estado $\rightarrow$ **CANCELADA**|Lanza "La solicitud de cotización fue cancelada: ya no se reciben respuestas".|
|**CP-CPV-03**|Solicitud cerrada o vencida|Solicitud con planes generados; abierta con la fecha límite vencida; y abierta con el enlace vencido|Abierta y vigente $\rightarrow$ **No**|Lanza "La solicitud de cotización ya cerró: el plazo para responder venció" en los tres casos.|
|**CP-CPV-04**|Proveedor dado de baja|Proveedor `BAJA`|Proveedor activo $\rightarrow$ **No**|Lanza "El enlace ya no está habilitado para responder la solicitud de cotización".|

### 2. `buscarCotizacion(String token)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CPV-05**|Pantalla del proveedor _(Camino feliz)_|El proveedor ya respondió la malta: Bolsa de 25 kg a $ 32.500 con 4 unidades|Enlace vigente|Devuelve su razón social, la fecha límite y la de su respuesta; solo la malta y el lúpulo (no la levadura), la malta con sus presentaciones por nombre ("Balde (5 kg)", "Bolsa (25 kg)"), 100 kg solicitados y su respuesta actual; el lúpulo sin respuesta.|

### 3. `registrarCotizacion(String token, CotizacionProveedorFormDTO cotizacionProveedorFormDTO)`

Una fila sin presentación, precio ni cantidad no cotiza su insumo. Las reglas de una fila indican el campo (`ofertas[i].campo`) para que la pantalla lo marque.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CPV-06**|Filas inválidas|Datos nulos; 101 filas; una fila nula; fila sin insumo; fila de la levadura (no la ofrece) o de la línea 999 (no es de la solicitud); malta repetida|Filas válidas $\rightarrow$ **No**|Lanza "Los datos de la cotización son obligatorios", "La cotización no puede superar 100 insumos", "Las filas de la cotización son obligatorias", "El insumo de cada fila de la cotización es obligatorio", "Uno de los insumos de la cotización no forma parte de la solicitud o no figura en su catálogo" (ambos casos) y "La cotización no puede repetir el mismo insumo". No guarda.|
|**CP-CPV-07**|Fila a medio completar|Sin presentación; presentación del lúpulo para la malta (segunda fila); precio nulo, 0, sobre 1.000.000.000 o con 5 decimales; cantidad nula, 0 o sobre 1.000.000|Cada campo de la fila|Lanza, con el campo de la fila, "Seleccione la presentación de Malta Pilsen", "La presentación elegida para Malta Pilsen no figura en su catálogo" (`ofertas[1]`), "El precio unitario de Malta Pilsen es obligatorio / debe ser mayor a 0 / no puede superar 1.000.000.000 / no puede tener más de 4 decimales" y "La cantidad disponible de Malta Pilsen es obligatoria / debe ser mayor a 0 / no puede superar 1.000.000". No guarda.|
|**CP-CPV-08**|Respuesta registrada _(Camino feliz)_|Malta en Bolsa de 25 kg a $ 32.500,5 con 4 unidades; lúpulo vacío|Filas válidas|Queda una oferta de la malta con la presentación, el precio y la cantidad informados; la fecha de respuesta se registra. Antes de cargar la invitación se bloquea la solicitud (ID por el hash, bloqueo y recién entonces la invitación) y se guarda la invitación.|
|**CP-CPV-09**|Reemplazo de la respuesta anterior|Ofertas previas de malta (Bolsa) y lúpulo; nuevo envío con la malta en Balde a $ 7.000 con 20 unidades y el lúpulo vacío|Ofertas existentes|La oferta de la malta se actualiza en su lugar (mismo registro) y la del lúpulo se quita.|
|**CP-CPV-10**|La solicitud cerró al enviar|Solicitud `CERRADA` al momento del bloqueo|Vigente con la solicitud bloqueada $\rightarrow$ **No**|Lanza "La solicitud de cotización ya cerró: el plazo para responder venció" y no guarda.|
|**CP-CPV-11**|Token desconocido al enviar|Ningún enlace con ese token|ID de la solicitud $\rightarrow$ **Vacío**|Lanza "El enlace de la solicitud de cotización no es válido" sin bloquear ninguna solicitud.|
