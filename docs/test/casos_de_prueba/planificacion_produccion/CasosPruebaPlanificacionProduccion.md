### 1. `filtrarPlanificacionesProduccion(Long idReceta, EstadoSolicitud estado, LocalDate fechaInicio, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FPP-01**|Filtra por los 3 criterios informados|`idReceta: 1L`, `estado: PENDIENTE`, `fechaInicio: 2026-03-01`, `pageable: PageRequest.of(0, 10)`, BD con 1 planificación que cumple los 3 criterios|`filtrarPlanificacionesProduccion(1L, PENDIENTE, 2026-03-01, pageable)` contiene elementos|Retorna `Page<PlanificacionProduccionResponseDTO>` con 1 elemento mapeado.|
|**CP-FPP-02**|Los 3 criterios nulos no restringen la búsqueda|`idReceta: null`, `estado: null`, `fechaInicio: null`, `pageable: PageRequest.of(0, 10)`|El service propaga los 3 parámetros nulos tal cual al repositorio|Retorna `Page<PlanificacionProduccionResponseDTO>` con todas las planificaciones (equivalente a no filtrar).|
|**CP-FPP-03**|Consulta sin coincidencias|`idReceta: 99L`, `estado: null`, `fechaInicio: null`, `pageable: PageRequest.of(0, 10)`|`filtrarPlanificacionesProduccion(99L, null, null, pageable)` está vacío|Retorna `Page<PlanificacionProduccionResponseDTO>` vacía (`getContent().isEmpty() == true`).|
