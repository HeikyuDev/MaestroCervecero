## Casos de prueba de las plantillas de motivos de ajuste

**Tipo de prueba:** guardas de plantilla (lectura de los archivos HTML). Clase `MotivoAjustePlantillasTest`.

| **ID** | **Nombre del Caso** | **Resultado Esperado** |
| --- | --- | --- |
| **CP-PMA-01** | Pestaña | `configuracion/tabs.html` tiene la pestaña Motivos de ajuste guardada por `MOTIVO_AJUSTE_CONSULTAR`, con ícono del fragmento de pestañas, junto a Costos directos adicionales. |
| **CP-PMA-02** | Lista | Usa el diálogo de baja compartido, los filtros con ejemplo y Limpiar y Buscar a la derecha, y la columna Acciones. |
| **CP-PMA-03** | Formulario | Es `novalidate` con envío único, marca los obligatorios con asterisco rojo, tiene ejemplo en el nombre y el selector del tipo se deshabilita solo con el bloqueo, con el tipo real en un campo oculto. |
