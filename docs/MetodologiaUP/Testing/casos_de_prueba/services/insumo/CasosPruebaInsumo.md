### 1. `filtrarInsumos(String nombre, TipoInsumo tipo, Pageable pageable)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FI-01**|Filtra por nombre y tipo informados, traduciendo el tipo a su clase concreta|`nombre: "Pilsen"`, `tipo: MALTA`, `pageable: PageRequest.of(0, 10)`, BD con 2 maltas activas que cumplen ambos criterios|El service traduce `TipoInsumo.MALTA` a `MaltaEntity.class` antes de llamar a `filtrarInsumos("Pilsen", MaltaEntity.class, pageable)`|Retorna `Page<InsumoResponseDTO>` con 2 elementos mapeados a su DTO concreto (`MaltaResponseDTO`).|
|**CP-FI-02**|Tipo nulo no restringe la búsqueda|`nombre: "Pilsen"`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|El service propaga `tipoClase: null` al repositorio|Retorna `Page<InsumoResponseDTO>` con todos los insumos activos que coinciden con el nombre, sin importar su tipo.|
|**CP-FI-03**|Nombre nulo no restringe la búsqueda|`nombre: null`, `tipo: MALTA`, `pageable: PageRequest.of(0, 10)`|El service propaga `nombre: null` tal cual al repositorio|Retorna `Page<InsumoResponseDTO>` con todas las maltas activas, sin filtrar por nombre.|
|**CP-FI-04**|Consulta sin coincidencias|`nombre: "Inexistente"`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|`filtrarInsumos("Inexistente", null, pageable)` está vacío|Retorna `Page<InsumoResponseDTO>` vacía (`getContent().isEmpty() == true`).|
|**CP-FI-05**|El nombre buscado se normaliza|`nombre: "   Pilsen   Nacional  "`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|El texto buscado recibe el mismo tratamiento que el nombre guardado: sin espacios en los extremos y con un solo espacio entre palabras|Consulta el repositorio con `nombre: "Pilsen Nacional"`.|
|**CP-FI-06**|Un nombre formado solo por espacios no filtra|`nombre: "    "`, `tipo: null`, `pageable: PageRequest.of(0, 10)`|Tras normalizar queda vacío y se interpreta como ausencia de filtro|Consulta el repositorio con `nombre: null`.|

### 2. `buscarPorId(Long id)`

|**ID**|**Nombre del Caso**|**Datos de Entrada (Escenario)**|**Condición Evaluada**|**Resultado Esperado**|
|---|---|---|---|---|
|**CP-FI-07**|Devuelve el insumo activo en su DTO concreto|`id: 1`, BD con la malta activa "Pilsen" (`id: 1`)|`findById(1)` encuentra el insumo, cualquiera sea su tipo|Retorna un `MaltaResponseDTO` con el id, el nombre, la unidad de medida y el estado de la malta.|
|**CP-FI-08**|Insumo inexistente o dado de baja|`id: 99`, sin insumo activo con ese id|`findById(99)` está vacío|Lanza `RecursoNoEncontradoException` con el mensaje "No se encontró el insumo con ID: 99".|
