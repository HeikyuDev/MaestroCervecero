## Casos de prueba de `CotizacionProveedorServicioImpl`

**Tipo de prueba:** unitaria (clase `CotizacionProveedorServicioImplTest`), con los repositorios simulados. Es el service de la pantalla pública con la que un proveedor responde una solicitud de cotización desde el enlace del correo.

**Datos comunes:** solicitud 90 abierta que vence en 5 horas con tres insumos (Malta Pilsen 100 kg, Cascade 500 g y US-05 50 g); proveedor 1 activo cuyo catálogo vigente ofrece la malta en "Bolsa" de 25 kg (100) y "Balde" de 5 kg (101) y el lúpulo en "Paquete" de 1 kg (102), pero no la levadura; el enlace del proveedor vence con la solicitud.

### 1. Validación del enlace (`buscarCotizacion` y `registrarCotizacion`)

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CPV-01**|Token inválido|Token nulo, vacío, en blanco, de 129 caracteres o desconocido|Enlace existente $\rightarrow$ **No**|Lanza `EnlaceCotizacionInvalidoException` ("El enlace de la solicitud de cotización no es válido").|
|**CP-CPV-02**|Solicitud cancelada|Solicitud `CANCELADA`|Estado $\rightarrow$ **CANCELADA**|Lanza "La solicitud de cotización fue cancelada: ya no se reciben respuestas".|
|**CP-CPV-03**|Solicitud cerrada o vencida|Solicitud con el plan generado; abierta con la fecha límite vencida; y abierta con el enlace vencido|Abierta y vigente $\rightarrow$ **No**|Lanza "La solicitud de cotización ya cerró: el plazo para responder venció" en los tres casos.|
|**CP-CPV-04**|Proveedor dado de baja|Proveedor `BAJA`|Proveedor activo $\rightarrow$ **No**|Lanza "El enlace ya no está habilitado para responder la solicitud de cotización".|

### 2. `buscarCotizacion(String token)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CPV-05**|Pantalla del proveedor _(Camino feliz)_|El proveedor ya respondió la malta: Bolsa de 25 kg a $ 32.500 con 4 unidades|Enlace vigente|Devuelve su razón social, la fecha límite y la de su respuesta; solo la malta y el lúpulo (no la levadura); la malta con una fila por presentación ("Balde (5 kg)", "Bolsa (25 kg)"), 100 kg solicitados, la respuesta actual en la Bolsa y el Balde sin cotizar; el lúpulo sin respuesta.|

### 3. `registrarCotizacion(String token, CotizacionProveedorFormDTO cotizacionProveedorFormDTO)`

Cada fila es un insumo solicitado en una presentación del catálogo vigente del proveedor; un insumo puede cotizarse en varias presentaciones a la vez. Una fila sin precio ni cantidad no responde esa presentación; cantidad 0 sin precio indica que no tiene stock. Las reglas de una fila indican el campo (`ofertas[i].campo`), para que la pantalla lo marque.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CPV-06**|Filas inválidas|Datos nulos; 101 filas; una fila nula; fila sin insumo; fila sin presentación; fila de la levadura (no la ofrece) o de la línea 999 (no es de la solicitud); la presentación del lúpulo (102) en la fila de la malta; un ítem de catálogo 777 que no es de su catálogo vigente|Filas válidas $\rightarrow$ **No**|Lanza "Los datos de la cotización son obligatorios", "La cotización no puede superar 100 presentaciones", "Las filas de la cotización son obligatorias", "El insumo de cada fila de la cotización es obligatorio", "La presentación de cada fila de la cotización es obligatoria", "Uno de los insumos de la cotización no forma parte de la solicitud o no figura en su catálogo" (ambos casos) y "Una de las presentaciones de Malta Pilsen no figura en su catálogo" (ambos casos). No guarda.|
|**CP-CPV-07**|Fila con precio y cantidad|Precio 0, sobre 1.000.000.000 o con 5 decimales con cantidad 2; cantidad -1 sin precio; cantidad sobre 1.000.000; todo en la Bolsa de 25 kg de la malta|Cada campo de la fila|Lanza, con el campo de la fila, "El precio unitario de Malta Pilsen en Bolsa (25 kg) debe ser mayor a 0" / "no puede superar 1.000.000.000" / "no puede tener más de 4 decimales" y "La cantidad disponible de Malta Pilsen en Bolsa (25 kg) no puede ser negativa" / "no puede superar 1.000.000". No guarda.|
|**CP-CPV-08**|Respuesta registrada _(Camino feliz)_|Malta en Balde vacía, malta en Bolsa de 25 kg a $ 32.500,5 con 4 unidades y lúpulo vacío|Filas válidas|Queda una oferta de la malta en la Bolsa con el precio y la cantidad informados; se registra la fecha de respuesta y la respuesta devuelta refleja la oferta. Antes de cargar la invitación se bloquea la solicitud (ID por el hash, bloqueo y recién entonces la invitación) y se guarda la invitación.|
|**CP-CPV-09**|Reemplazo de la respuesta anterior|Ofertas previas de malta (Bolsa a $ 30.000, 2 unidades) y lúpulo; nuevo envío con la malta en Bolsa a $ 31.000 con 3 unidades y en Balde a $ 7.000 con 20 unidades, y el lúpulo vacío|Ofertas existentes|La oferta de la malta en Bolsa se actualiza en su lugar (mismo registro), se agrega una oferta nueva de la malta en Balde y la del lúpulo se quita.|
|**CP-CPV-10**|La solicitud cerró al enviar|Solicitud `CERRADA` al momento del bloqueo|Vigente con la solicitud bloqueada $\rightarrow$ **No**|Lanza "La solicitud de cotización ya cerró: el plazo para responder venció" y no guarda.|
|**CP-CPV-11**|Token desconocido al enviar|Ningún enlace con ese token|ID de la solicitud $\rightarrow$ **Vacío**|Lanza "El enlace de la solicitud de cotización no es válido" sin bloquear ninguna solicitud.|
|**CP-CPV-12**|Varias presentaciones del mismo insumo|Malta en Balde de 5 kg a $ 6.000 con 3 unidades y en Bolsa de 25 kg a $ 28.000 con 2 unidades|Un insumo, varias presentaciones|Quedan dos ofertas de la malta, una por presentación (Bolsa con 2 unidades y Balde con 3).|
|**CP-CPV-13**|Presentación repetida|Dos filas de la malta en la misma Bolsa de 25 kg (la segunda sin stock)|Presentación única por insumo $\rightarrow$ **No**|Lanza "La cotización no puede repetir la misma presentación de un insumo". No guarda.|
|**CP-CPV-15**|Respuesta sin ofertas|Respuesta previa con una oferta; nuevo envío con todas las filas vacías|Cotiza alguna presentación $\rightarrow$ **No**|Quita las ofertas anteriores y registra la fecha de respuesta.|

### 4. Combinaciones de precio y cantidad de una fila

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CPV-16**|Sin precio ni cantidad|Bolsa vacía y Balde a $ 6.000 con 3 unidades|Fila sin responder|La Bolsa se ignora: queda solo la oferta del Balde.|
|**CP-CPV-17**|Sin precio con cantidad 0|Solo la Bolsa con cantidad 0|Sin stock|Queda una oferta de la Bolsa con cantidad 0 y sin precio.|
|**CP-CPV-18**|Precio con cantidad 0|Balde vacío y Bolsa a $ 25.000 con cantidad 0 (segunda fila); y Bolsa con cantidad 0 y precio 0, -5, 1.000.000.000,01 o con 5 decimales|Precio con cantidad 0 $
ightarrow$ **No**|Lanza siempre "No indique precio unitario si la cantidad disponible es 0" en el precio de la fila (`ofertas[1]` y `ofertas[0]`), antes que las reglas del valor del precio. No guarda.|
|**CP-CPV-19**|Sin precio con cantidad mayor a 0|Bolsa sin precio con 2 unidades|Precio informado $\rightarrow$ **No**|Lanza "El precio unitario es obligatorio si indica una cantidad disponible" en `ofertas[0].precioUnitario`. No guarda.|
|**CP-CPV-20**|Precio sin cantidad|Bolsa a $ 25.000 sin cantidad|Cantidad informada $\rightarrow$ **No**|Lanza "La cantidad disponible es obligatoria si indica un precio unitario" en `ofertas[0].cantidadDisponible`. No guarda.|
|**CP-CPV-21**|Precio con cantidad mayor a 0|Bolsa sin stock y Balde a $ 6.000 con 3 unidades|Oferta con stock|Quedan la oferta sin stock de la Bolsa y la del Balde con su precio.|
|**CP-CPV-22**|Fila vacía con datos viejos|Filas sin precio ni cantidad con la línea 999, con la presentación 777 y repitiendo la Bolsa, más la Bolsa a $ 28.000 con 2 unidades|Fila sin responder|Las filas vacías se ignoran antes de validar su insumo y su presentación: queda solo la oferta de la Bolsa.|

### 5. Catálogo modificado después de responder

Al modificar el proveedor se crea una versión con ítems de catálogo nuevos. Una oferta se identifica por su insumo y su presentación comercial: se resuelve siempre contra el ítem del catálogo vigente. En estos casos la versión vigente pasa a ser la 11, con la malta en "Bolsa" de 25 kg (200) y el lúpulo en "Paquete" de 1 kg (201); el "Balde" de 5 kg de la malta sale del catálogo.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-CPV-23**|Respuesta visible con el catálogo modificado|Respuesta registrada con la versión 10: malta en Bolsa a $ 32.500 con 4 unidades, malta en Balde a $ 7.000 con 20 unidades y lúpulo en Paquete a $ 9.000 con 1 unidad; luego se modifica el catálogo|Presentación en el catálogo vigente|La malta solo muestra la Bolsa, con el ítem vigente 200 y la respuesta anterior ($ 32.500, 4 unidades); el Balde no se muestra. El lúpulo muestra el ítem 201 con su respuesta anterior.|
|**CP-CPV-24**|Nuevo envío con el catálogo modificado|Las mismas ofertas anteriores; nuevo envío con la malta en Bolsa (200) a $ 31.000 con 3 unidades y el lúpulo (201) a $ 9.500 con 2 unidades|Oferta registrada con una versión anterior|Las ofertas de la Bolsa y del lúpulo se actualizan en su lugar (mismos registros) y pasan a apuntar a los ítems vigentes 200 y 201; la oferta del Balde se quita.|
|**CP-CPV-25**|Ítem de una versión anterior|Envío con la malta en el ítem 100 de la versión 10|Ítem del catálogo vigente $\rightarrow$ **No**|Lanza "Una de las presentaciones de Malta Pilsen no figura en su catálogo". No guarda.|
