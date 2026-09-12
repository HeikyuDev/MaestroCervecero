### 1. `filtrarInsumos(String nombre, TipoInsumo tipo, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FI-01**|Filtra por nombre y tipo informados, traduciendo el tipo a su clase concreta|`nombre: "Pilsen"`, `tipo: MALTA`, `pageable: PageRequest.of(0, 10)`, BD con 2 maltas activas que cumplen ambos criterios|El service traduce `TipoInsumo.MALTA` a `MaltaEntity.class` antes de llamar a `filtrarInsumos("Pilsen", MaltaEntity.class, pageable)`|Retorna `Page<InsumoResponseDTO>` con 2 elementos mapeados a su DTO concreto (`MaltaResponseDTO`).|
|**CP-FI-02**|Tipo nulo no restringe la búsqueda|`nombre: "Pilsen"`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|El service propaga `tipoClase: null` al repositorio|Retorna `Page<InsumoResponseDTO>` con todos los insumos activos que coinciden con el nombre, sin importar su tipo.|
|**CP-FI-03**|Nombre nulo no restringe la búsqueda|`nombre: null`, `tipo: MALTA`, `pageable: PageRequest.of(0, 10)`|El service propaga `nombre: null` tal cual al repositorio|Retorna `Page<InsumoResponseDTO>` con todas las maltas activas, sin filtrar por nombre.|
|**CP-FI-04**|Consulta sin coincidencias|`nombre: "Inexistente"`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|`filtrarInsumos("Inexistente", null, pageable)` está vacío|Retorna `Page<InsumoResponseDTO>` vacía (`getContent().isEmpty() == true`).|
