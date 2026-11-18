## Casos de prueba de `MapperSolicitudCotizacion`

**Tipo de prueba:** unitaria pura (clase `MapperSolicitudCotizacionTest`), sin repositorios ni mocks. Cubre los cálculos que comparten la pantalla del gerente de compras, la del proveedor y los avisos: la conversión de las presentaciones a la unidad del insumo, el precio por unidad del insumo, el tiempo restante y la explicación de la solicitud.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-MSC-01**|Contenido de la presentación|500 g para un insumo en kg; 1 kg para uno en g; 1 t para uno en kg; 25 kg para uno en kg|Conversión de unidades|0,5; 1.000; 1.000 y 25, respectivamente.|
|**CP-MSC-02**|Precio por unidad del insumo|500 g a $ 1.000 (insumo en kg); 1 kg a $ 9.000 (insumo en g); 3 kg a $ 10; presentación sin contenido|Precio / contenido convertido|$ 2.000/kg; $ 9/g; $ 3,3333/kg (4 decimales, `HALF_UP`); sin contenido no hay precio.|
|**CP-MSC-03**|Tiempo restante|Fecha límite a 2 d 5 h, 1 d 1 h, 3 d, 7 h 30 min, 20 min y en el momento|Días y horas|"2 días y 5 horas", "1 día y 1 hora", "3 días", "7 horas", "menos de 1 hora" y "Vencida".|
|**CP-MSC-04**|Explicación de la solicitud|Solicitud 12 de la planificación 45, pendiente de confirmación, con Malta Pilsen (falta 120 kg) y Lúpulo Cascade (falta 2,5 kg); luego abierta|Estado pendiente de confirmación|"Se generó la solicitud de cotización N° 12 para la planificación N° 45: el stock actual no cubre Malta Pilsen (faltan 120 kg) ni Lúpulo Cascade (faltan 2,5 kg). Confirme el envío a los proveedores o cancele la solicitud"; abierta, sin la frase final.|
