# Diagrama de secuencia de diseño: iniciar lote

El diagrama muestra cómo `LoteController.iniciarLote` y `LoteServicioImpl.iniciarLote` coordinan el inicio de un lote pendiente: reservan sus insumos, ocupan los equipos, activan la molienda y ajustan el cronograma del fermentador. Parte del detalle del lote ya abierto, con un usuario autorizado mediante `LOTE_INICIAR`.

```mermaid
sequenceDiagram
    actor Usuario
    participant Vista as Interfaz web (Thymeleaf)
    participant C as LoteController
    participant S as LoteServicioImpl
    participant B as OrdenCanonicoDeBloqueo
    participant E as EscaladoInsumoServicioImpl
    participant I as ILoteInsumoRepository
    participant R as IReservaInsumoRepository
    participant L as ILoteRepository
    participant A as ControllerAdvices

    Usuario->>Vista: Confirmar inicio del lote
    Vista->>C: POST /lotes/{id}/iniciar
    C->>S: iniciarLote(id)
    Note over S: Validar que se haya informado el identificador
    alt Inicio permitido (búsquedas y validaciones satisfactorias)
        S->>B: bloquearLote(id, false)
        Note over B: Bloquear fermentador, cola pendiente y lote<br/>sin bloquear la planificación
        B-->>S: LoteEntity bajo bloqueo de escritura
        Note over S: Exigir lote PENDIENTE<br/>bloquearYValidarEquipamientoDisponible: bloquear los cuatro equipos<br/>y exigir estado operativo DISPONIBLE
        S->>E: calcularRequerimientosTotales(lote)
        E-->>S: Requerimientos de insumos por etapa
        S->>I: buscarNoVencidosPorInsumosIdsParaReservar(idsInsumo, hoy)
        I-->>S: Lotes de insumo candidatos bajo bloqueo de escritura
        Note over S: obtenerYValidarStockDisponible: sumar lo requerido por insumo<br/>y comprobar todos los faltantes antes de reservar
        loop Por cada aporte de stock necesario, en orden FEFO
            Note over S: reservarInsumosFEFO: reservar cantidad disponible<br/>y crear ReservaInsumoEntity asociada a la etapa
            S->>I: save(loteInsumo)
            I-->>S: Lote de insumo con cantidad reservada actualizada
        end
        S->>R: saveAll(reservas)
        R-->>S: Reservas de insumos guardadas
        Note over S: Lote EN_EJECUCION e inicio registrado<br/>Inicio estimado = hoy, conservando la duración planificada<br/>Cuatro equipos EN_USO, guardados en sus repositorios<br/>Molienda EN_CURSO con fecha y hora de inicio
        S->>L: save(lote)
        L-->>S: Lote iniciado con su etapa actualizada
        S->>L: flush()
        Note over S,L: Volcar el cambio antes de volver a consultar la cola pendiente
        S->>L: buscarPendientesDeFermentadorParaReordenar(idFermentador)
        L-->>S: Cola pendiente bloqueada, sin el lote iniciado
        S->>L: buscarLoteEnEjecucionDeFermentador(idFermentador)
        L-->>S: Lote en ejecución que determina la base del cronograma
        Note over S: reencadenarColaPendienteDeFermentador(idFermentador, false)<br/>Recalcular fechas conservando duraciones y orden de la cola<br/>y respetando planificación y margen de limpieza
        opt Cambiaron fechas de lotes pendientes
            S->>L: saveAll(lotesModificados)
            L-->>S: Cronograma pendiente actualizado
        end
        S-->>C: LoteResponseDTO
        Note over C: Mensaje flash con el identificador del lote iniciado
        C-->>Vista: redirect:/lotes/{id}
        Vista->>C: GET /lotes/{id}
        Note over C: mostrarDetalle recupera el lote y la etapa en curso
        C-->>Vista: Vista lote/lote-detalle
        Vista-->>Usuario: Detalle con molienda en curso y mensaje de éxito
    else Rechazo en el primer paso que falla
        S-->>C: ReglaNegocioException o RecursoNoEncontradoException
        Note over S: Se interrumpe el inicio y se revierte la transacción
        Note over C,A: La excepción se propaga desde el controlador<br/>Spring MVC selecciona el handler de ControllerAdvices
        Note over A: handleReglaNegocio o handleRecursoNoEncontrado<br/>prepara el mensaje flash y resuelve un origen seguro
        A-->>Vista: Redirección al origen del pedido o a la raíz
        Vista-->>Usuario: Página de destino con el motivo del rechazo
    end
```

## Participantes y abstracción

- **Interfaz web y LoteController:** representan la confirmación de inicio desde `lote/lote-detalle.html`, la petición HTTP y la redirección al detalle. No existe un formulario de alta que deba reconstruirse en este flujo.
- **LoteServicioImpl y OrdenCanonicoDeBloqueo:** coordinan la transacción y el bloqueo pesimista en orden canónico. El componente de bloqueo encapsula sus consultas reales a `IFermentadorRepository` e `ILoteRepository`; el parámetro `false` indica que esta operación no bloquea la planificación.
- **EscaladoInsumoServicioImpl:** calcula los requerimientos usando la receta de la planificación, el volumen y la instantánea de escalado del lote. Asocia malta a molienda, levadura a fermentación y lúpulo a la etapa configurada.
- **ILoteInsumoRepository e IReservaInsumoRepository:** bloquean stock no vencido y persisten las cantidades reservadas y las reservas por etapa. FEFO prioriza el vencimiento más próximo. Se comprueba el stock total requerido por cada insumo antes de crear reservas; si falta stock, el mensaje reúne todos los faltantes.
- **ILoteRepository:** guarda el inicio y permite reencadenar el cronograma. El `flush` se mantiene visible porque garantiza que la consulta de pendientes ya no incluya el lote que acaba de iniciarse.
- **ControllerAdvices:** maneja los rechazos propagados; `LoteController` no los captura. Una regla de negocio produce una alerta de advertencia y un recurso inexistente una alerta de error. La vuelta utiliza el `Referer` solo si es seguro y del mismo sitio; en otro caso vuelve a `/`.

El fragmento de inicio permitido resume el recorrido satisfactorio; la alternativa de rechazo representa una interrupción en cualquiera de sus pasos, incluso al validar las fechas del cronograma. No se ejecutan los pasos posteriores al fallo y las escrituras previas se revierten. Los errores técnicos y la infraestructura transversal de seguridad y auditoría quedan fuera del diagrama.

Para mantener el nivel de diseño, se encapsulan las consultas `buscarPorIdParaCambiarEstadoOperativo` y los `save` de `IMolinoRepository`, `IMaceradorRepository`, `IOllaHervorRepository` e `IFermentadorRepository`. Los cuatro equipos se ocupan al iniciar el lote; solo la molienda pasa a `EN_CURSO`. Las reservas comprometen stock para las etapas: el consumo efectivo se registra posteriormente.

El reencadenado conserva el orden y la duración de los lotes pendientes, respeta el inicio de sus planificaciones y sitúa la cola después del lote en ejecución con el margen de limpieza. Se omiten las fórmulas de fechas, getters, setters, conversiones a DTO y consultas de presentación del detalle. Las entidades se representan como datos porque sus cambios relevantes quedan resumidos en las notas.

## Código de referencia

- [LoteController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/lote/LoteController.java): `iniciarLote`, `redirigirAlDetalle` y `mostrarDetalle`.
- [LoteServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/lote/LoteServicioImpl.java): `iniciarLote`, validación de equipos y stock, reservas FEFO y reencadenado.
- [OrdenCanonicoDeBloqueo.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/lote/OrdenCanonicoDeBloqueo.java): `bloquearLote`.
- [EscaladoInsumoServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/lote/EscaladoInsumoServicioImpl.java): `calcularRequerimientosTotales`.
- [ILoteInsumoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/ingreso_insumo/ILoteInsumoRepository.java), [IReservaInsumoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/IReservaInsumoRepository.java) e [ILoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/ILoteRepository.java): stock, reservas y cronograma.
- [ControllerAdvices.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/advice/ControllerAdvices.java): handlers de negocio y redirección segura con mensaje.
