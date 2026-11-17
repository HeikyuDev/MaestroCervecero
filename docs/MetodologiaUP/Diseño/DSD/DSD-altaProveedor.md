# Diagrama de secuencia de diseño: dar de alta un proveedor

El diagrama muestra cómo `ProveedorController.altaProveedor` y `ProveedorServicioImpl.altaProveedor` registran un proveedor activo con su primera versión y catálogo. Parte del formulario ya abierto, con un usuario autorizado como `GERENTE_DE_COMPRAS`.

```mermaid
sequenceDiagram
    actor Usuario
    participant Vista as Interfaz web (Thymeleaf)
    participant C as ProveedorController
    participant S as ProveedorServicioImpl
    participant V as IVersionProveedorRepository
    participant L as ILocalidadRepository
    participant P as IPresentacionComercialRepository
    participant I as IInsumoRepository
    participant G as GuardadoUnico
    participant R as IProveedorRepository

    Usuario->>Vista: Completar datos del proveedor y catálogo y guardar
    Vista->>C: POST /proveedores
    Note over C: Spring MVC enlaza y valida ProveedorFormDTO
    alt Errores de enlace o Bean Validation
        C->>C: cargarModeloFormulario(model, null, proveedorForm)
        C-->>Vista: Vista proveedor/proveedor-form con datos y errores
        Vista-->>Usuario: Formulario para corregir
    else Formulario válido
        C->>S: altaProveedor(proveedorForm)
        Note over S: normalizarYValidar agrupa datos del proveedor y catálogo<br/>Validar obligatorios, formatos, longitudes y pares de catálogo no repetidos
        alt Alta permitida (validaciones y búsquedas satisfactorias)
            S->>V: existsByCuitAndEsUltimaVersionTrue(cuit)
            V-->>S: CUIT no registrado en otro proveedor activo
            S->>V: existsByEmailIgnoreCaseAndEsUltimaVersionTrue(email)
            V-->>S: Correo no registrado en otro proveedor activo
            Note over S: Crear ProveedorEntity ACTIVO<br/>construirVersion resuelve referencias y arma la versión inicial
            S->>L: findById(idLocalidad)
            L-->>S: LocalidadEntity activa
            loop Por cada ítem del catálogo
                S->>P: findById(idPresentacionComercial)
                P-->>S: PresentacionComercialEntity activa
                S->>I: findById(idInsumo)
                I-->>S: InsumoEntity activo
                Note over S: construirItemCatalogo vincula presentación, insumo y versión
            end
            Note over S: Versión inicial esUltimaVersion = true<br/>y catálogo vinculados al proveedor
            S->>G: guardar(proveedorRepository, proveedorEntity, excepcionesPorIndice)
            G->>R: save(proveedorEntity)
            R-->>G: Proveedor con versión y catálogo persistidos en cascada
            G->>R: flush()
            alt Guardado correcto
                R-->>G: Restricciones verificadas
                G-->>S: ProveedorEntity guardado
                S-->>C: ProveedorResponseDTO
                Note over C: Mensaje flash: Proveedor creado correctamente
                C-->>Vista: redirect:/proveedores
                Vista->>C: GET /proveedores
                Note over C: listarProveedores recupera el listado
                C-->>Vista: Vista proveedor/proveedor-lista
                Vista-->>Usuario: Listado actualizado y mensaje de éxito
            else Conflicto de unicidad al guardar
                R-->>G: DataIntegrityViolationException
                G-->>S: RecursoDuplicadoException o ReglaNegocioException según índice
                S-->>C: Excepción de negocio traducida
                Note over C: Duplicado en version.cuit o version.email<br/>Catálogo repetido como error global
                C->>C: cargarModeloFormulario(model, null, proveedorForm)
                C-->>Vista: Vista proveedor/proveedor-form con datos y errores
                Vista-->>Usuario: Formulario conservado para corregir
            end
        else Rechazo en el primer paso de validación o búsqueda que falla
            S-->>C: ReglaNegocioException, RecursoDuplicadoException o RecursoNoEncontradoException
            Note over C: Duplicado en el campo version.cuit o version.email<br/>Regla incumplida o referencia ausente como error global
            C->>C: cargarModeloFormulario(model, null, proveedorForm)
            C-->>Vista: Vista proveedor/proveedor-form con datos y errores
            Vista-->>Usuario: Formulario conservado para corregir
        end
    end
```

## Participantes y abstracción

- **Interfaz web y ProveedorController:** representan `proveedor/proveedor-form.html`, la petición HTTP y el listado `proveedor/proveedor-lista.html`. El éxito utiliza Post-Redirect-Get. Los errores vuelven a renderizar el formulario en la misma petición, conservando los datos y filas del catálogo.
- **ProveedorServicioImpl e IVersionProveedorRepository:** coordinan el alta transaccional y comprueban la unicidad del CUIT normalizado y del correo, sin distinguir mayúsculas en este último, entre versiones vigentes de proveedores activos. La razón social no es única y el teléfono es opcional.
- **ILocalidadRepository, IPresentacionComercialRepository e IInsumoRepository:** resuelven las referencias activas de la versión y de cada ítem. El catálogo debe tener al menos un ítem, respetar el máximo técnico y no repetir la combinación de insumo y presentación comercial.
- **GuardadoUnico e IProveedorRepository:** guardan el proveedor, la versión inicial y su catálogo mediante cascada. `save` y `flush` se conservan visibles para explicar la traducción de conflictos concurrentes de unicidad dentro del servicio. El helper traduce los índices de CUIT y correo a duplicados y el de ítem repetido a una regla de negocio.

El proveedor funciona como contenedor estable: sus datos comerciales, contacto, localidad y catálogo pertenecen a `VersionProveedorEntity`, marcada inicialmente como vigente. Las entidades se muestran como datos, sin líneas de vida propias porque no coordinan las colaboraciones del alta.

La alternativa permitida presenta el recorrido satisfactorio hasta el guardado. Un dato inválido, un duplicado o una referencia inexistente interrumpen el primer paso que falle, sin ejecutar los siguientes. Un conflicto concurrente puede ocurrir aunque las consultas previas de unicidad hayan pasado. La transacción revierte las escrituras si falla el alta. No se agrega recuperación para errores técnicos que el controlador no captura.

`rechazarDuplicado` asocia el error a `version.cuit` o `version.email` cuando la excepción indica el campo, y lo registra como error global si no lo indica. Las reglas de negocio y las referencias ausentes se muestran en el resumen global. `cargarModeloFormulario` encapsula las consultas de presentación que recuperan la ubicación y los nombres de los insumos y presentaciones comerciales seleccionados.

La normalización de CUIT, correo y textos, las validaciones internas, la construcción de entidades y la conversión a DTO se resumen. Se omiten getters, setters, buscadores, apertura inicial del formulario e infraestructura transversal de seguridad y auditoría. El diagrama cubre el alta y no la creación de versiones posteriores al modificar un proveedor.

## Código de referencia

- [ProveedorController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/proveedor/ProveedorController.java): `altaProveedor`, `rechazarDuplicado`, `cargarModeloFormulario` y listado.
- [ProveedorServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/proveedor/ProveedorServicioImpl.java): `altaProveedor`, `normalizarYValidar`, `construirVersion` y `construirItemCatalogo`.
- [IVersionProveedorRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/proveedor/IVersionProveedorRepository.java) e [IProveedorRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/proveedor/IProveedorRepository.java): unicidad y persistencia.
- [ILocalidadRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/ubicacion/ILocalidadRepository.java), [IPresentacionComercialRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/proveedor/IPresentacionComercialRepository.java) e [IInsumoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/insumo/IInsumoRepository.java): referencias de localidad y catálogo.
- [GuardadoUnico.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/util/persistencia/GuardadoUnico.java): persistencia y traducción de índices únicos.
