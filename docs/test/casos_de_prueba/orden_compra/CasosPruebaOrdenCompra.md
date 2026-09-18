### 1. `filtrarOrdenesCompra(Long idPlanificacionProduccion, Long idProveedor, EstadoSolicitud estado, LocalDate fechaEntregaEstimada, Pageable pageable)`

`idProveedor` no vive directamente en `OrdenCompraEntity`: se resuelve a través de la relación `versionProveedor.proveedor`, y considera cualquier versión (histórica o activa) del proveedor. Todos los criterios son coincidencia exacta y opcionales, `null` = no filtra por ese criterio.

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FOC-01**|Filtra por los 4 criterios informados|`idPlanificacionProduccion: 1L`, `idProveedor: 2L`, `estado: PENDIENTE`, `fechaEntregaEstimada: 2026-01-15`, `pageable: PageRequest.of(0, 10)`, BD con 1 orden que cumple los cuatro criterios|`filtrarOrdenesCompra(1L, 2L, PENDIENTE, 2026-01-15, pageable)` contiene elementos|Retorna `Page<OrdenCompraResponseDTO>` con 1 elemento mapeado.|
|**CP-FOC-02**|Los 4 parámetros nulos no restringen la búsqueda|`idPlanificacionProduccion: null`, `idProveedor: null`, `estado: null`, `fechaEntregaEstimada: null`, `pageable: PageRequest.of(0, 10)`, BD con 3 órdenes en distintos estados|El service propaga los 4 parámetros nulos tal cual al repositorio|Retorna `Page<OrdenCompraResponseDTO>` con las 3 órdenes (equivalente a no filtrar).|
|**CP-FOC-03**|Consulta sin coincidencias|`idPlanificacionProduccion: 99L`, resto de los parámetros nulos, `pageable: PageRequest.of(0, 10)`|`filtrarOrdenesCompra(99L, null, null, null, pageable)` está vacío|Retorna `Page<OrdenCompraResponseDTO>` vacía (`getContent().isEmpty() == true`).|
