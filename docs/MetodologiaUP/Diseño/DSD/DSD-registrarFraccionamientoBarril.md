# Diagrama de secuencia de diseño: registrar fraccionamiento de barril

El diagrama muestra cómo `OperacionBarrilController.registrarFraccionamiento` y `FraccionamientoBarrilServicioImpl.registrarFraccionamientoBarril` registran una extracción de cerveza y actualizan el contenido y estado operativo del barril. Parte del formulario ya abierto, con un usuario autorizado mediante `FRACCIONAMIENTO_BARRIL_REGISTRAR`.

```mermaid
sequenceDiagram
    actor Usuario
    participant Vista as Interfaz web (Thymeleaf)
    participant C as OperacionBarrilController
    participant Panel as ControlBarrilController
    participant S as FraccionamientoBarrilServicioImpl
    participant B as IBarrilRepository
    participant O as IOperacionCicloVidaBarrilRepository
    participant E as IEnvasadoLoteRepository
    participant F as IFraccionamientoBarrilRepository
    participant A as ControllerAdvices

    Usuario->>Vista: Informar cantidad a extraer, fecha, hora y observaciones
    Vista->>C: POST /control-barriles/{idBarril}/fraccionamientos
    Note over C: Spring MVC enlaza y valida FraccionamientoBarrilFormDTO<br/>El ID del barril se toma de la ruta
    alt Errores de enlace o Bean Validation del formulario
        Note over C: Conservar formulario y BindingResult en atributos flash
        C-->>Vista: redirect:/control-barriles/{idBarril}/fraccionamientos/nuevo
        Vista->>C: GET del formulario de fraccionamiento
        C-->>Vista: Vista control_barril/control-form con datos y errores
        Vista-->>Usuario: Formulario para corregir
    else Formulario válido
        C->>S: registrarFraccionamientoBarril(form)
        Note over S: Validar fecha obligatoria y no futura y observaciones<br/>Exigir cantidad finita, positiva y con hasta cuatro decimales
        alt Registro permitido (búsquedas y validaciones satisfactorias)
            S->>B: buscarPorIdParaCambiarEstadoOperativo(idBarril)
            B-->>S: BarrilEntity activo bajo bloqueo de escritura
            Note over S: Exigir estado CON_CERVEZA<br/>Calcular contenido restante redondeado y rechazar si es negativo
            S->>O: buscarFechaUltimaOperacionRegistrada(idBarril)
            O-->>S: Fecha de última operación registrada, si existe
            S->>E: buscarFechaUltimoEnvasadoRegistrado(idBarril)
            E-->>S: Fecha del último envasado registrado, si existe
            Note over S: Exigir fraccionamiento estrictamente posterior a ambas fechas
            S->>E: buscarFechaFinalizacionDelLoteDelUltimoEnvasadoRegistrado(idBarril)
            E-->>S: Fecha de finalización del lote del último envasado, si existe
            Note over S: Exigir fraccionamiento estrictamente posterior al cierre del lote
            alt Contenido restante igual a cero tras redondear
                Note over S: Estado operativo resultante EN_LIMPIEZA
            else Queda contenido en el barril
                Note over S: Estado operativo resultante CON_CERVEZA
            end
            Note over S: Aplicar contenido restante y estado resultante al barril
            S->>B: save(barrilEntity)
            Note over S: Crear FraccionamientoBarrilEntity REGISTRADO<br/>con cantidad extraída, fecha, observaciones y estadoOperativoResultante
            S->>F: save(fraccionamientoBarrilEntity)
            F-->>S: FraccionamientoBarrilEntity registrado
            S-->>C: FraccionamientoBarrilResponseDTO
            Note over C: Mensaje flash: Fraccionamiento registrado correctamente
            C-->>Vista: redirect:/control-barriles/{idBarril}?pestana=fraccionamientos
            Vista->>Panel: GET /control-barriles/{idBarril}?pestana=fraccionamientos
            Note over Panel: Recuperar resumen del barril y operaciones de la pestaña
            Panel-->>Vista: Vista control_barril/control-panel
            Vista-->>Usuario: Panel con fraccionamiento y contenido actualizado
        else Regla de negocio incumplida en cualquier paso
            S-->>C: ReglaNegocioException
            Note over S: Interrumpir el registro y revertir la transacción
            Note over C: registrar y rechazar asocian el error al campo admitido<br/>o al resumen global y conservan datos y errores en flash
            C-->>Vista: redirect:/control-barriles/{idBarril}/fraccionamientos/nuevo
            Vista->>C: GET del formulario de fraccionamiento
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
- **FraccionamientoBarrilServicioImpl e IBarrilRepository:** coordinan la transacción y recuperan el barril activo bajo bloqueo. Exigen `CON_CERVEZA`, descuentan la cantidad extraída y persisten contenido y estado resultantes. Es el único bloqueo de escritura de la operación y se toma antes de leer contenido e historial.
- **IOperacionCicloVidaBarrilRepository e IEnvasadoLoteRepository:** aportan las fechas de la última operación, del último envasado y de finalización de su lote. El fraccionamiento debe ser estrictamente posterior a esas actividades cuando existen.
- **IFraccionamientoBarrilRepository:** persiste la operación `REGISTRADO`, vinculada al barril. Conserva la cantidad fraccionada y el `estadoOperativoResultante` exacto para que una futura anulación pueda comprobarlo y restituir el contenido.
- **ControllerAdvices:** maneja los recursos inexistentes propagados por el controlador mediante `handleRecursoNoEncontrado` y una redirección segura con error.

La cantidad extraída debe estar informada, ser finita y positiva, y tener como máximo cuatro decimales. El servicio resta esa cantidad al contenido actual y redondea el resultado a la escala de cantidades. Un resultado negativo rechaza el registro. Un resultado cero deja el barril `EN_LIMPIEZA`, mientras que un remanente positivo lo mantiene `CON_CERVEZA`. El redondeo evita que residuos de punto flotante impidan reconocer un barril vacío.

La fecha y hora son obligatorias y no pueden ser futuras. Las observaciones también son obligatorias, se normalizan y tienen longitud máxima. Las validaciones compartidas de `MetodosCicloVida` y las modificaciones de las entidades se agrupan en notas. Registrar un fraccionamiento no crea un despacho ni ejecuta una limpieza.

El fragmento permitido presenta el recorrido satisfactorio. Cada rechazo interrumpe el primer paso que falle y revierte las escrituras del servicio. Las reglas de negocio se capturan en el controlador y se asocian al campo admitido indicado por la excepción o al resumen global. Los errores MVC y esos rechazos conservan datos y errores en flash y redirigen al formulario.

El controlador elimina del resultado de validación el error del ID del barril, porque ese identificador procede de la ruta. El GET del formulario consulta su resumen y, si el estado ya no permite fraccionar, vuelve al panel con un aviso. Esas consultas de presentación quedan encapsuladas.

Las entidades se representan como datos porque no coordinan colaboraciones significativas en este flujo. Se omiten getters, setters, construcción de entidades, conversiones a DTO, consultas internas del panel e infraestructura transversal de seguridad y auditoría. La anulación es un caso de uso independiente. Los errores técnicos quedan fuera del diagrama.

## Código de referencia

- [OperacionBarrilController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/control_barril/OperacionBarrilController.java): `registrarFraccionamiento`, `registrar`, `rechazar` y `mostrarFormulario`.
- [FraccionamientoBarrilServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/barril/FraccionamientoBarrilServicioImpl.java): `registrarFraccionamientoBarril`.
- [IBarrilRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/barril/IBarrilRepository.java), [IOperacionCicloVidaBarrilRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/barril/IOperacionCicloVidaBarrilRepository.java), [IEnvasadoLoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/IEnvasadoLoteRepository.java) e [IFraccionamientoBarrilRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/barril/IFraccionamientoBarrilRepository.java): bloqueo, fechas y persistencia.
- [MetodosCicloVida.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/util/method/MetodosCicloVida.java): validaciones compartidas de fecha, observaciones y barril.
- [ControlBarrilController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/control_barril/ControlBarrilController.java) y [ControllerAdvices.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/advice/ControllerAdvices.java): panel de resultado y manejo global de recursos no encontrados.
