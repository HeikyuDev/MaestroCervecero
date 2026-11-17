# Diagrama de secuencia de diseño: registrar mantenimiento de equipamiento

El diagrama muestra cómo `OperacionEquipamientoController.registrarMantenimiento` y `MantenimientoEquipamientoServicioImpl.registrarMantenimientoEquipamiento` registran un mantenimiento y devuelven el equipo a disponibilidad operativa. Parte del formulario ya abierto, con un usuario autorizado mediante `MANTENIMIENTO_EQUIPAMIENTO_REGISTRAR`.

```mermaid
sequenceDiagram
    actor Usuario
    participant Vista as Interfaz web (Thymeleaf)
    participant C as OperacionEquipamientoController
    participant Panel as ControlEquipamientoController
    participant S as MantenimientoEquipamientoServicioImpl
    participant E as IEquipamientoRepository
    participant O as IOperacionCicloVidaEquipamientoRepository
    participant T as IEtapaLoteRepository
    participant M as IMantenimientoEquipamientoRepository
    participant A as ControllerAdvices

    Usuario->>Vista: Informar fecha, hora y observaciones del mantenimiento
    Vista->>C: POST /control-equipamiento/{idEquipamiento}/mantenimientos
    Note over C: Spring MVC enlaza y valida MantenimientoEquipamientoFormDTO<br/>El ID del equipo se toma de la ruta
    alt Errores de enlace o Bean Validation
        Note over C: Conservar formulario y BindingResult en atributos flash
        C-->>Vista: redirect:/control-equipamiento/{idEquipamiento}/mantenimientos/nuevo
        Vista->>C: GET del formulario de mantenimiento
        C-->>Vista: Vista control_equipamiento/control-form con datos y errores
        Vista-->>Usuario: Formulario para corregir
    else Formulario válido
        C->>S: registrarMantenimientoEquipamiento(form)
        Note over S: Validar fecha obligatoria y no futura<br/>Normalizar y validar observaciones obligatorias y longitud máxima
        alt Registro permitido (búsquedas y validaciones satisfactorias)
            S->>E: buscarPorIdParaOperacionCicloVida(idEquipamiento)
            E-->>S: EquipamientoEntity bajo bloqueo de escritura
            Note over S: Exigir equipo activo y estado operativo EN_MANTENIMIENTO
            S->>O: buscarFechaUltimaOperacionRegistrada(idEquipamiento)
            O-->>S: Fecha de última operación registrada, si existe
            S->>T: buscarFechaUltimaParticipacionRegistrada(idEquipamiento)
            T-->>S: Fin de última participación en un lote, si existe
            Note over S: Exigir mantenimiento estrictamente posterior a ambas fechas<br/>Cambiar equipo a DISPONIBLE
            S->>E: save(equipamientoEntity)
            Note over S: Crear MantenimientoEquipamientoEntity REGISTRADO<br/>con fecha, observaciones y equipo
            S->>M: save(mantenimientoEquipamientoEntity)
            M-->>S: MantenimientoEquipamientoEntity registrado
            S-->>C: MantenimientoEquipamientoResponseDTO
            Note over C: Mensaje flash: Mantenimiento registrado correctamente
            C-->>Vista: redirect:/control-equipamiento/{idEquipamiento}?pestana=mantenimientos
            Vista->>Panel: GET /control-equipamiento/{idEquipamiento}?pestana=mantenimientos
            Note over Panel: Recuperar resumen del equipo y operaciones de la pestaña
            Panel-->>Vista: Vista control_equipamiento/control-panel
            Vista-->>Usuario: Panel con mantenimiento registrado y equipo DISPONIBLE
        else Regla de negocio incumplida en cualquier paso
            S-->>C: ReglaNegocioException
            Note over S: Interrumpir el registro y revertir la transacción
            Note over C: registrar y rechazar asocian el error a fecha u observaciones<br/>si corresponde, o al resumen global<br/>Conservar formulario y errores en flash
            C-->>Vista: redirect:/control-equipamiento/{idEquipamiento}/mantenimientos/nuevo
            Vista->>C: GET del formulario de mantenimiento
            C-->>Vista: Vista control_equipamiento/control-form con datos y errores
            Vista-->>Usuario: Formulario para corregir
        else Equipamiento no encontrado
            S-->>C: RecursoNoEncontradoException
            Note over S: Interrumpir el registro y revertir la transacción
            Note over C,A: La excepción se propaga desde el controlador<br/>Spring MVC selecciona handleRecursoNoEncontrado
            A-->>Vista: Redirección segura con mensaje de error
            Vista-->>Usuario: Página de destino con el motivo del rechazo
        end
    end
```

## Participantes y abstracción

- **Interfaz web, OperacionEquipamientoController y ControlEquipamientoController:** representan `control_equipamiento/control-form.html`, el envío HTTP y el retorno mediante Post-Redirect-Get al panel `control_equipamiento/control-panel.html`. El ID del equipo procede de la ruta y se incorpora al formulario antes de delegar el registro. El panel de resultado lo atiende un controlador distinto del que registra.
- **MantenimientoEquipamientoServicioImpl e IEquipamientoRepository:** coordinan la transacción, recuperan el equipo bajo bloqueo y exigen que esté activo y en `EN_MANTENIMIENTO`. Es el único bloqueo de escritura de la operación y se toma antes de consultar la actividad previa. El estado final es `DISPONIBLE`.
- **IOperacionCicloVidaEquipamientoRepository e IEtapaLoteRepository:** aportan la fecha de la última operación registrada y el fin de la última participación del equipo en un lote. El mantenimiento debe ser estrictamente posterior a ambas cuando existen.
- **IMantenimientoEquipamientoRepository:** persiste el mantenimiento `REGISTRADO`, asociado al equipo y con su fecha y observaciones.
- **ControllerAdvices:** maneja los recursos inexistentes propagados por el controlador mediante `handleRecursoNoEncontrado` y una redirección segura con error.

La fecha y hora son obligatorias y no pueden ser futuras. Las observaciones también son obligatorias, se normalizan y respetan una longitud máxima. Las reglas compartidas de `MetodosCicloVidaEquipamiento` y las modificaciones de las entidades se agrupan en notas.

El equipo ya debe estar `EN_MANTENIMIENTO`, tanto si llegó a ese estado por una falla como por alcanzar su máximo de usos. Este registro no inicia un mantenimiento voluntario sobre un equipo disponible, ni busca una falla concreta para actualizarla. Cambia la disponibilidad operativa y registra la operación.

El fragmento permitido presenta el recorrido satisfactorio. Cada rechazo interrumpe el primer paso que falle y revierte las escrituras del servicio. Las reglas de negocio se capturan en el controlador y se asocian a `fecha` u `observaciones` cuando la excepción lo indica, o al resumen global en otro caso. Los errores MVC y esos rechazos conservan los datos y errores en flash y redirigen al formulario.

El GET del formulario consulta el resumen del equipo y, si su estado ya no permite registrar el mantenimiento, vuelve al panel con un aviso. Las consultas de presentación y los métodos comunes del controlador `registrar`, `rechazar` y `mostrarFormulario` se encapsulan para mantener el nivel de diseño.

Las entidades se representan como datos porque no coordinan colaboraciones significativas en este flujo. Se omiten getters, setters, construcción de entidades, conversiones a DTO, consultas internas del panel e infraestructura transversal de seguridad y auditoría. No se registra un costo ni se modifica un lote de producción en esta operación. La anulación es un caso de uso independiente. Los errores técnicos quedan fuera del diagrama.

## Código de referencia

- [OperacionEquipamientoController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/control_equipamiento/OperacionEquipamientoController.java): `registrarMantenimiento`, `registrar`, `rechazar` y `mostrarFormulario`.
- [MantenimientoEquipamientoServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/equipamiento/MantenimientoEquipamientoServicioImpl.java): `registrarMantenimientoEquipamiento`.
- [MetodosCicloVidaEquipamiento.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/util/method/equipamiento/MetodosCicloVidaEquipamiento.java): reglas de fecha, observaciones, equipo activo y estado operativo.
- [IEquipamientoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/equipamiento/IEquipamientoRepository.java), [IOperacionCicloVidaEquipamientoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/equipamiento/IOperacionCicloVidaEquipamientoRepository.java), [IEtapaLoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/IEtapaLoteRepository.java) e [IMantenimientoEquipamientoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/equipamiento/IMantenimientoEquipamientoRepository.java): bloqueo, historial y persistencia.
- [ControlEquipamientoController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/control_equipamiento/ControlEquipamientoController.java) y [ControllerAdvices.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/advice/ControllerAdvices.java): panel de resultado y manejo global de recursos no encontrados.
