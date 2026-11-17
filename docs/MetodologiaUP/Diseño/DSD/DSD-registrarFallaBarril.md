# Diagrama de secuencia de diseño: registrar falla de barril

El diagrama muestra cómo `OperacionBarrilController.registrarFalla` y `FallaBarrilServicioImpl.registrarFallaBarril` registran una falla y dejan el barril en mantenimiento. Parte del formulario ya abierto, con un usuario autorizado mediante `FALLA_BARRIL_REGISTRAR`.

```mermaid
sequenceDiagram
    actor Usuario
    participant Vista as Interfaz web (Thymeleaf)
    participant C as OperacionBarrilController
    participant Panel as ControlBarrilController
    participant S as FallaBarrilServicioImpl
    participant B as IBarrilRepository
    participant O as IOperacionCicloVidaBarrilRepository
    participant E as IEnvasadoLoteRepository
    participant F as IFallaBarrilRepository
    participant A as ControllerAdvices

    Usuario->>Vista: Informar fecha, hora y observaciones de la falla
    Vista->>C: POST /control-barriles/{idBarril}/fallas
    Note over C: Spring MVC enlaza y valida FallaBarrilFormDTO<br/>El ID del barril se toma de la ruta
    alt Errores de enlace o Bean Validation del formulario
        Note over C: Conservar formulario y BindingResult en atributos flash
        C-->>Vista: redirect:/control-barriles/{idBarril}/fallas/nueva
        Vista->>C: GET del formulario de falla
        C-->>Vista: Vista control_barril/control-form con datos y errores
        Vista-->>Usuario: Formulario para corregir
    else Formulario válido
        C->>S: registrarFallaBarril(form)
        Note over S: Validar fecha y hora obligatorias y no futuras<br/>Normalizar y validar observaciones obligatorias y longitud máxima
        alt Registro permitido (búsquedas y validaciones satisfactorias)
            S->>B: buscarPorIdParaCambiarEstadoOperativo(idBarril)
            B-->>S: BarrilEntity activo bajo bloqueo de escritura
            Note over S: Exigir estado operativo DISPONIBLE
            S->>O: buscarFechaUltimaOperacionRegistrada(idBarril)
            O-->>S: Fecha de última operación registrada, si existe
            S->>E: buscarFechaUltimoEnvasadoRegistrado(idBarril)
            E-->>S: Fecha del último envasado registrado, si existe
            Note over S: Exigir falla estrictamente posterior a ambas fechas<br/>Cambiar barril a EN_MANTENIMIENTO
            S->>B: save(barrilEntity)
            Note over S: Crear FallaBarrilEntity REGISTRADO<br/>con fecha, observaciones y barril
            S->>F: save(fallaBarrilEntity)
            F-->>S: FallaBarrilEntity registrada
            S-->>C: FallaBarrilResponseDTO
            Note over C: Mensaje flash: Falla registrada correctamente
            C-->>Vista: redirect:/control-barriles/{idBarril}?pestana=fallas
            Vista->>Panel: GET /control-barriles/{idBarril}?pestana=fallas
            Note over Panel: Recuperar resumen del barril y operaciones de la pestaña
            Panel-->>Vista: Vista control_barril/control-panel
            Vista-->>Usuario: Panel con falla registrada y barril EN_MANTENIMIENTO
        else Regla de negocio incumplida en cualquier paso
            S-->>C: ReglaNegocioException
            Note over S: Interrumpir el registro y revertir la transacción
            Note over C: registrar y rechazar asocian el error al campo admitido<br/>o al resumen global y conservan datos y errores en flash
            C-->>Vista: redirect:/control-barriles/{idBarril}/fallas/nueva
            Vista->>C: GET del formulario de falla
            C-->>Vista: Vista control_barril/control-form con datos y errores
            Vista-->>Usuario: Formulario para corregir
        else Barril no encontrado
            S-->>C: RecursoNoEncontradoException
            Note over S: Interrumpir el registro y revertir la transacción
            Note over C,A: La excepción se propaga desde el controlador<br/>Spring MVC selecciona handleRecursoNoEncontrado
            A-->>Vista: Redirección segura con mensaje de error
            Vista-->>Usuario: Página de destino con el motivo del rechazo
        end
    end
```

## Participantes y abstracción

- **Interfaz web, OperacionBarrilController y ControlBarrilController:** representan `control_barril/control-form.html`, el envío HTTP y el retorno mediante Post-Redirect-Get al panel `control_barril/control-panel.html`. El ID del barril procede de la ruta. El panel de resultado lo atiende un controlador distinto del que registra.
- **FallaBarrilServicioImpl e IBarrilRepository:** coordinan la transacción y recuperan el barril activo bajo bloqueo. Exigen `DISPONIBLE` y guardan la transición a `EN_MANTENIMIENTO`. Es el único bloqueo de escritura de la operación y se toma antes de consultar el historial.
- **IOperacionCicloVidaBarrilRepository e IEnvasadoLoteRepository:** aportan las fechas de la última operación y del último envasado registrado. La falla debe ser estrictamente posterior a ambas cuando existen.
- **IFallaBarrilRepository:** persiste la falla `REGISTRADO`, asociada al barril y con su fecha y observaciones.
- **ControllerAdvices:** maneja los recursos inexistentes propagados por el controlador mediante `handleRecursoNoEncontrado` y una redirección segura con error.

La fecha y hora son obligatorias y no pueden ser futuras. Las observaciones también son obligatorias, se normalizan y respetan una longitud máxima. Las validaciones compartidas de `MetodosCicloVida` y los cambios de las entidades se agrupan en notas.

La precondición `DISPONIBLE` impide registrar esta falla sobre un barril en uso, con cerveza, despachado, en limpieza o ya en mantenimiento. El caso de uso cambia su disponibilidad operativa sin modificar su contenido, crear un mantenimiento o alterar un lote de producción automáticamente.

El fragmento permitido presenta el recorrido satisfactorio. Cada rechazo interrumpe el primer paso que falle y revierte las escrituras del servicio. Las reglas de negocio se capturan en el controlador y se asocian al campo admitido indicado por la excepción o al resumen global. Los errores MVC y esos rechazos conservan datos y errores en flash y redirigen al formulario.

El controlador elimina del resultado de validación el error del ID del barril, porque ese identificador procede de la ruta. El GET del formulario consulta su resumen y, si el estado ya no permite registrar una falla, vuelve al panel con un aviso. Esas consultas de presentación quedan encapsuladas.

Las entidades se representan como datos porque no coordinan colaboraciones significativas en este flujo. Se omiten getters, setters, construcción de entidades, conversiones a DTO, consultas internas del panel e infraestructura transversal de seguridad y auditoría. No se registra un costo en esta operación. La anulación es un caso de uso independiente. Los errores técnicos quedan fuera del diagrama.

## Código de referencia

- [OperacionBarrilController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/control_barril/OperacionBarrilController.java): `registrarFalla`, `registrar`, `rechazar` y `mostrarFormulario`.
- [FallaBarrilServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/barril/FallaBarrilServicioImpl.java): `registrarFallaBarril`.
- [IBarrilRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/barril/IBarrilRepository.java), [IOperacionCicloVidaBarrilRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/barril/IOperacionCicloVidaBarrilRepository.java), [IEnvasadoLoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/IEnvasadoLoteRepository.java) e [IFallaBarrilRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/barril/IFallaBarrilRepository.java): bloqueo, fechas y persistencia.
- [MetodosCicloVida.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/util/method/MetodosCicloVida.java): validaciones compartidas de fecha, observaciones y barril.
- [ControlBarrilController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/control_barril/ControlBarrilController.java) y [ControllerAdvices.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/advice/ControllerAdvices.java): panel de resultado y manejo global de recursos no encontrados.
