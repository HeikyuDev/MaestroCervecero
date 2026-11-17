# Diagrama de secuencia de diseño: registrar limpieza de equipamiento

El diagrama muestra cómo `OperacionEquipamientoController.registrarLimpieza` y `LimpiezaEquipamientoServicioImpl.registrarLimpiezaEquipamiento` registran una limpieza y deciden si el equipo queda disponible o requiere mantenimiento. Parte del formulario ya abierto, con un usuario autorizado mediante `LIMPIEZA_EQUIPAMIENTO_REGISTRAR`.

```mermaid
sequenceDiagram
    actor Usuario
    participant Vista as Interfaz web (Thymeleaf)
    participant C as OperacionEquipamientoController
    participant Panel as ControlEquipamientoController
    participant S as LimpiezaEquipamientoServicioImpl
    participant E as IEquipamientoRepository
    participant O as IOperacionCicloVidaEquipamientoRepository
    participant T as IEtapaLoteRepository
    participant M as IMantenimientoEquipamientoRepository
    participant L as ILimpiezaEquipamientoRepository
    participant A as ControllerAdvices

    Usuario->>Vista: Informar fecha, hora y observaciones y guardar
    Vista->>C: POST /control-equipamiento/{idEquipamiento}/limpiezas
    Note over C: Spring MVC enlaza y valida LimpiezaEquipamientoFormDTO<br/>El identificador del equipo se toma de la ruta
    alt Errores de enlace o Bean Validation
        Note over C: Conservar formulario y BindingResult en atributos flash
        C-->>Vista: redirect:/control-equipamiento/{idEquipamiento}/limpiezas/nueva
        Vista->>C: GET del formulario de limpieza
        C-->>Vista: Vista control_equipamiento/control-form con datos y errores
        Vista-->>Usuario: Formulario para corregir
    else Formulario válido
        C->>S: registrarLimpiezaEquipamiento(form)
        Note over S: Validar fecha obligatoria y no futura<br/>Normalizar y validar observaciones obligatorias y longitud máxima
        alt Registro permitido (búsquedas y validaciones satisfactorias)
            S->>E: buscarPorIdParaOperacionCicloVida(idEquipamiento)
            E-->>S: EquipamientoEntity bajo bloqueo de escritura
            Note over S: Exigir equipo activo y estado operativo EN_LIMPIEZA
            S->>O: buscarFechaUltimaOperacionRegistrada(idEquipamiento)
            O-->>S: Fecha de última operación, si existe
            S->>T: buscarFechaUltimaParticipacionRegistrada(idEquipamiento)
            T-->>S: Fin de última participación en un lote, si existe
            Note over S: Exigir fecha de limpieza estrictamente posterior<br/>a ambas actividades anteriores
            S->>M: buscarFechaUltimoMantenimientoRegistrado(idEquipamiento)
            M-->>S: Fecha del último mantenimiento, si existe
            S->>L: contarLimpiezasRegistradasDesde(idEquipamiento, fechaUltimoMantenimiento)
            L-->>S: Cantidad de limpiezas registradas del período
            alt Cantidad de limpiezas más la actual alcanza el máximo de usos
                Note over S: Estado operativo resultante EN_MANTENIMIENTO
            else Máximo de usos todavía no alcanzado
                Note over S: Estado operativo resultante DISPONIBLE
            end
            S->>E: save(equipamientoEntity)
            E-->>S: Equipo con estado operativo resultante
            Note over S: Crear LimpiezaEquipamientoEntity REGISTRADO<br/>con fecha, observaciones y estadoOperativoResultante
            S->>L: save(limpiezaEquipamientoEntity)
            L-->>S: LimpiezaEquipamientoEntity registrada
            S-->>C: LimpiezaEquipamientoResponseDTO
            Note over C: Mensaje flash: Limpieza registrada correctamente
            C-->>Vista: redirect:/control-equipamiento/{idEquipamiento}?pestana=limpiezas
            Vista->>Panel: GET /control-equipamiento/{idEquipamiento}?pestana=limpiezas
            Note over Panel: Recuperar resumen del equipo y operaciones de la pestaña
            Panel-->>Vista: Vista control_equipamiento/control-panel
            Vista-->>Usuario: Panel con limpieza registrada y estado actualizado
        else Regla de negocio incumplida en cualquier paso
            S-->>C: ReglaNegocioException
            Note over S: Interrumpir el registro y revertir la transacción
            Note over C: registrar y rechazar asocian el error a fecha u observaciones<br/>si corresponde, o al resumen global<br/>Conservar formulario y errores en flash
            C-->>Vista: redirect:/control-equipamiento/{idEquipamiento}/limpiezas/nueva
            Vista->>C: GET del formulario de limpieza
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

- **Interfaz web y OperacionEquipamientoController:** representan `control_equipamiento/control-form.html`, la petición HTTP y el retorno mediante Post-Redirect-Get. El ID del equipo procede de la ruta y se incorpora al formulario antes de delegar el registro. Los métodos comunes `registrar` y `rechazar` conservan los datos y errores en flash.
- **LimpiezaEquipamientoServicioImpl e IEquipamientoRepository:** coordinan la transacción y bloquean primero el equipo, antes de leer su historial o contar usos. El equipo debe existir, estar activo y encontrarse `EN_LIMPIEZA`.
- **IOperacionCicloVidaEquipamientoRepository e IEtapaLoteRepository:** aportan la última operación registrada y el fin del último uso del equipo en producción. La fecha de limpieza debe ser estrictamente posterior a ambas, además de no ser futura.
- **IMantenimientoEquipamientoRepository e ILimpiezaEquipamientoRepository:** aportan el último mantenimiento y la cantidad de limpiezas registradas desde entonces. Si nunca hubo mantenimiento, se cuentan todas. Se suma la limpieza actual y, al alcanzar o superar `usosMaximosAntesMantenimiento`, el equipo queda `EN_MANTENIMIENTO`. Si todavía no alcanza ese límite, queda `DISPONIBLE`.
- **ControllerAdvices:** maneja los recursos inexistentes que el controlador deja propagar, mediante `handleRecursoNoEncontrado` y una redirección segura con error.

La limpieza conserva el `estadoOperativoResultante` que dejó en el equipo. Este dato permite que una eventual anulación compruebe el resultado exacto de esa operación. Las entidades se representan como datos, ya que sus cambios relevantes quedan resumidos en notas.

El fragmento permitido presenta el recorrido satisfactorio. Cada rechazo interrumpe el primer paso que falle y revierte las escrituras del servicio. Las reglas de negocio se capturan en el controlador y se asocian al campo `fecha` u `observaciones` cuando la excepción lo indica, o al resumen global en otro caso. Tanto esos rechazos como los errores de validación MVC vuelven al formulario por redirección con los datos ingresados.

El GET del formulario consulta el resumen del equipo mediante `ControlEquipamientoServicioImpl.buscarResumen`. Si su estado ya no permite registrar una limpieza, vuelve al panel con un aviso en lugar de mostrar el formulario. El panel posterior al éxito lo atiende `ControlEquipamientoController`, cuya consulta se resume. Las validaciones compartidas de `MetodosCicloVidaEquipamiento` se agrupan en notas, sin una línea de vida auxiliar.

Se omiten getters, setters, construcción de entidades, conversiones a DTO y consultas de presentación. Este registro no incorpora un costo ni crea un mantenimiento automáticamente, solo puede dejar el equipo en `EN_MANTENIMIENTO`. La infraestructura transversal de seguridad y auditoría y los errores técnicos quedan fuera del diagrama.

## Código de referencia

- [OperacionEquipamientoController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/control_equipamiento/OperacionEquipamientoController.java): `registrarLimpieza`, `registrar`, `rechazar` y `mostrarFormulario`.
- [LimpiezaEquipamientoServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/equipamiento/LimpiezaEquipamientoServicioImpl.java): `registrarLimpiezaEquipamiento`.
- [MetodosCicloVidaEquipamiento.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/util/method/equipamiento/MetodosCicloVidaEquipamiento.java): reglas de fecha, observaciones, equipo activo y estado operativo.
- [IEquipamientoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/equipamiento/IEquipamientoRepository.java), [IOperacionCicloVidaEquipamientoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/equipamiento/IOperacionCicloVidaEquipamientoRepository.java), [IEtapaLoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/IEtapaLoteRepository.java), [IMantenimientoEquipamientoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/equipamiento/IMantenimientoEquipamientoRepository.java) e [ILimpiezaEquipamientoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/equipamiento/ILimpiezaEquipamientoRepository.java): bloqueo, historial, usos y persistencia.
- [ControlEquipamientoController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/control_equipamiento/ControlEquipamientoController.java) y [ControllerAdvices.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/advice/ControllerAdvices.java): panel de resultado y manejo global de recursos no encontrados.
