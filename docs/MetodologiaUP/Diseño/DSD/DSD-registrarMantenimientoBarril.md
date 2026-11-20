# Diagrama de secuencia de diseño: registrar mantenimiento de barril

El diagrama muestra cómo `OperacionBarrilController.registrarMantenimiento` y `MantenimientoBarrilServicioImpl.registrarMantenimientoBarril` registran un mantenimiento y devuelven el barril a disponibilidad operativa. Parte del formulario ya abierto, con un usuario autorizado mediante `MANTENIMIENTO_BARRIL_REGISTRAR`.

```mermaid
sequenceDiagram
    actor Usuario
    participant Vista as Interfaz web (Thymeleaf)
    participant C as OperacionBarrilController
    participant Panel as ControlBarrilController
    participant S as MantenimientoBarrilServicioImpl
    participant B as IBarrilRepository
    participant M as IMantenimientoBarrilRepository
    participant A as ControllerAdvices

    Usuario->>Vista: Informar observaciones del mantenimiento
    Vista->>C: POST /control-barriles/{idBarril}/mantenimientos
    Note over C: Spring MVC enlaza y valida MantenimientoBarrilFormDTO<br/>El ID del barril se toma de la ruta
    alt Errores de enlace o Bean Validation del formulario
        Note over C: Conservar formulario y BindingResult en atributos flash
        C-->>Vista: redirect:/control-barriles/{idBarril}/mantenimientos/nuevo
        Vista->>C: GET del formulario de mantenimiento
        C-->>Vista: Vista control_barril/control-form con datos y errores
        Vista-->>Usuario: Formulario para corregir
    else Formulario válido
        C->>S: registrarMantenimientoBarril(form)
        Note over S: Tomar la fecha y hora actual como fecha de la operación<br/>Normalizar y validar observaciones obligatorias y longitud máxima
        alt Registro permitido (búsquedas y validaciones satisfactorias)
            S->>B: buscarPorIdParaCambiarEstadoOperativo(idBarril)
            B-->>S: BarrilEntity activo bajo bloqueo de escritura
            Note over S: Exigir estado operativo EN_MANTENIMIENTO
            Note over S: Cambiar el barril a DISPONIBLE
            S->>B: save(barrilEntity)
            Note over S: Crear MantenimientoBarrilEntity REGISTRADO<br/>con la fecha y hora actual, observaciones y barril
            S->>M: save(mantenimientoBarrilEntity)
            M-->>S: MantenimientoBarrilEntity registrado
            S-->>C: MantenimientoBarrilResponseDTO
            Note over C: Mensaje flash: Mantenimiento registrado correctamente
            C-->>Vista: redirect:/control-barriles/{idBarril}?pestana=mantenimientos
            Vista->>Panel: GET /control-barriles/{idBarril}?pestana=mantenimientos
            Note over Panel: Recuperar resumen del barril y operaciones de la pestaña
            Panel-->>Vista: Vista control_barril/control-panel
            Vista-->>Usuario: Panel con mantenimiento registrado y barril DISPONIBLE
        else Regla de negocio incumplida en cualquier paso
            S-->>C: ReglaNegocioException
            Note over S: Interrumpir el registro y revertir la transacción
            Note over C: registrar y rechazar asocian el error al campo admitido<br/>o al resumen global y conservan datos y errores en flash
            C-->>Vista: redirect:/control-barriles/{idBarril}/mantenimientos/nuevo
            Vista->>C: GET del formulario de mantenimiento
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

- **Interfaz web, OperacionBarrilController y ControlBarrilController:** representan `control_barril/control-form.html`, el envío HTTP y el retorno mediante Post-Redirect-Get al panel `control_barril/control-panel.html`. El barril procede de la ruta. El panel de resultado lo atiende un controlador distinto del que registra.
- **MantenimientoBarrilServicioImpl e IBarrilRepository:** coordinan la transacción y recuperan el barril activo bajo bloqueo. Exigen `EN_MANTENIMIENTO` y guardan la transición a `DISPONIBLE`. Es el único bloqueo de escritura de la operación.
- **IMantenimientoBarrilRepository:** persiste el mantenimiento `REGISTRADO`, asociado al barril y con la fecha y hora actual y sus observaciones.
- **ControllerAdvices:** maneja los recursos inexistentes propagados por el controlador mediante `handleRecursoNoEncontrado` y una redirección segura con error.

La fecha y hora de la operación es la del momento del registro: la asigna el servicio, no se ingresa en el formulario. Las observaciones son obligatorias, se normalizan y tienen longitud máxima. Las validaciones compartidas de `MetodosCicloVida` y los cambios de las entidades se agrupan en notas. La precondición `EN_MANTENIMIENTO` expresa el ciclo operativo del barril, sin una consulta a una falla individual en este registro.

El fragmento permitido presenta el recorrido satisfactorio. Cada rechazo interrumpe el primer paso que falle y revierte las escrituras del servicio. Las reglas de negocio se capturan en el controlador y se asocian al campo admitido indicado por la excepción o al resumen global. Los errores MVC y esos rechazos conservan los datos y errores en flash y redirigen al formulario.

El controlador elimina del resultado de validación el error del ID del barril, porque ese identificador procede de la ruta. El GET del formulario consulta su resumen y, si el estado ya no permite registrar un mantenimiento, vuelve al panel con un aviso. Esas consultas de presentación quedan encapsuladas.

Las entidades se representan como datos porque no coordinan colaboraciones significativas en este flujo. Se omiten getters, setters, construcción de entidades, conversiones a DTO, consultas internas del panel e infraestructura transversal de seguridad y auditoría. Este registro cambia la disponibilidad operativa sin modificar el contenido del barril ni registrar un costo de mantenimiento. La anulación es un caso de uso independiente. Los errores técnicos quedan fuera del diagrama.

## Código de referencia

- [OperacionBarrilController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/control_barril/OperacionBarrilController.java): `registrarMantenimiento`, `registrar`, `rechazar` y `mostrarFormulario`.
- [MantenimientoBarrilServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/barril/MantenimientoBarrilServicioImpl.java): `registrarMantenimientoBarril`.
- [IBarrilRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/barril/IBarrilRepository.java) e [IMantenimientoBarrilRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/barril/IMantenimientoBarrilRepository.java): bloqueo y persistencia.
- [MetodosCicloVida.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/util/method/MetodosCicloVida.java): validaciones compartidas de observaciones y barril.
- [ControlBarrilController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/control_barril/ControlBarrilController.java) y [ControllerAdvices.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/advice/ControllerAdvices.java): panel de resultado y manejo global de recursos no encontrados.
