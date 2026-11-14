## Casos de prueba de `MapperDashboardAdministrador`

**Tipo de prueba:** unitaria del mapper de los DTO del panel del administrador. Clase `MapperDashboardAdministradorTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-MDA-01** | Usuario reciente | Lleva su ID, su username, su nombre y apellido unidos y su rol. |
| **CP-MDA-02** | Evento de auditoría | Lleva su ID, su fecha y hora, su usuario, su acción, su módulo y si tiene detalle. |
| **CP-MDA-03** | Conteo | Lleva el código, la etiqueta y el total recibidos. |
| **CP-MDA-04** | Entrada nula | El usuario y la entrada nulos se mapean a nulo. |
