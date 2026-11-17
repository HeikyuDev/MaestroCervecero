# Diagrama de secuencia de diseño: finalizar hervido

El diagrama muestra cómo `LoteController.finalizarHervido` y `LoteServicioImpl.finalizarHervido` verifican el consumo mínimo del hervido, inician la fermentación, dejan la olla en limpieza y liberan las reservas sobrantes de la etapa. Parte del detalle del lote ya abierto, con un usuario autorizado mediante `LOTE_FINALIZAR_HERVIDO`.

```mermaid
sequenceDiagram
    actor Usuario
    participant Vista as Interfaz web (Thymeleaf)
    participant C as LoteController
    participant S as LoteServicioImpl
    participant L as ILoteRepository
    participant P as IConfiguracionProduccionRepository
    participant N as ConsumoInsumoServicioImpl
    participant O as IOllaHervorRepository
    participant R as IReservaInsumoRepository
    participant I as ILoteInsumoRepository
    participant A as ControllerAdvices

    Usuario->>Vista: Confirmar finalización de hervido
    Vista->>C: POST /lotes/{id}/etapas/hervido/finalizar
    C->>S: finalizarHervido(id)
    Note over S: Validar identificador informado
    alt Finalización permitida (búsquedas y validaciones satisfactorias)
        S->>L: buscarPorIdParaActualizar(id)
        L-->>S: LoteEntity bajo bloqueo de escritura
        Note over S: Exigir lote EN_EJECUCION y hervido EN_CURSO
        S->>P: findById(SINGLETON_ID)
        P-->>S: ConfiguracionProduccionEntity con porcentaje mínimo de consumo
        S->>N: filtrarInsumosRequeridos(idEtapaHervido)
        N-->>S: Insumos con cantidades requeridas y consumidas de la etapa
        Note over S: validarPorcentajeMinimoConsumido:<br/>cada insumo debe alcanzar el mínimo configurado
        Note over S: avanzarEtapa: hervido FINALIZADA y fermentación EN_CURSO<br/>Registrar fecha de fin y fecha de inicio respectivas
        S->>O: buscarPorIdParaCambiarEstadoOperativo(idOllaHervor)
        O-->>S: OllaHervorEntity activa bajo bloqueo de escritura
        Note over S: Olla de hervor EN_LIMPIEZA
        S->>O: save(ollaHervor)
        O-->>S: Olla de hervor actualizada
        S->>R: findByEtapaLoteId(idEtapaHervido)
        R-->>S: Reservas de la etapa de hervido
        loop Por cada reserva ACTIVA, ordenada por ID de lote de insumo
            S->>I: buscarPorIdParaLiberarReserva(idLoteInsumo)
            I-->>S: LoteInsumoEntity bajo bloqueo de escritura
            Note over S: Releer stock una vez por lote de insumo<br/>y liberar el saldo pendiente de la reserva
            S->>I: save(loteInsumo)
            I-->>S: Cantidad reservada actualizada
        end
        S->>R: saveAll(reservasPorLoteInsumo)
        R-->>S: Reservas liberadas conservando su historial
        S->>L: save(lote)
        L-->>S: Lote con sus etapas actualizadas
        S-->>C: LoteResponseDTO
        Note over C: Mensaje flash: Etapa de Hervido finalizada correctamente
        C-->>Vista: redirect:/lotes/{id}
        Vista->>C: GET /lotes/{id}
        Note over C: mostrarDetalle recupera el lote y la etapa en curso
        C-->>Vista: Vista lote/lote-detalle
        Vista-->>Usuario: Detalle con fermentación en curso y mensaje de éxito
    else Rechazo en el primer paso que falla
        S-->>C: ReglaNegocioException o RecursoNoEncontradoException
        Note over S: Interrumpir la finalización y revertir la transacción
        Note over C,A: La excepción se propaga desde el controlador<br/>Spring MVC selecciona el handler de ControllerAdvices
        Note over A: handleReglaNegocio o handleRecursoNoEncontrado<br/>prepara mensaje flash y resuelve un origen seguro
        A-->>Vista: Redirección al origen seguro o destino alternativo
        Vista-->>Usuario: Página de destino con el motivo del rechazo
    end
```

## Participantes y abstracción

- **Interfaz web y LoteController:** representan la confirmación desde `lote/lote-detalle.html`, la petición HTTP y el retorno mediante Post-Redirect-Get. `volverDeFinalizarEtapa` prepara el mensaje y redirige al detalle sin fijar pestaña; este abre la etapa en curso.
- **LoteServicioImpl e ILoteRepository:** coordinan la transacción y bloquean el lote para serializar la finalización con consumos, cancelaciones y solicitudes repetidas. El lote permanece `EN_EJECUCION`; cambian las etapas de hervido y fermentación, persistidas con el lote.
- **IConfiguracionProduccionRepository y ConsumoInsumoServicioImpl:** aportan el mínimo configurado y las cantidades requeridas y consumidas. La condición se evalúa por insumo, no sobre un porcentaje global. Un mínimo de cero no exige consumo; un rechazo informa todos los insumos insuficientes.
- **IOllaHervorRepository:** recupera la olla activa bajo bloqueo y la guarda en `EN_LIMPIEZA`. El fermentador conserva su estado operativo; este método inicia su etapa de fermentación sin volver a ocupar el equipo.
- **IReservaInsumoRepository e ILoteInsumoRepository:** liberan solo el saldo de las reservas activas de hervido y actualizan las cantidades reservadas del stock. Conservan el historial y las reservas de las otras etapas.
- **ControllerAdvices:** maneja los rechazos propagados por el controlador. Una regla incumplida genera una advertencia y un recurso ausente un error; resuelve una redirección segura a partir del origen del pedido y sus alternativas.

El fragmento permitido resume el recorrido satisfactorio. La alternativa representa una interrupción en el primer paso que falle: identificador no informado, lote o etapa incorrectos y consumo insuficiente generan `ReglaNegocioException`; los recursos no recuperables generan `RecursoNoEncontradoException`. No se ejecutan los pasos posteriores al fallo y la transacción revierte las escrituras previas.

`filtrarInsumosRequeridos` encapsula la recuperación de etapa, consumos registrados, saldos reservados y requerimientos calculados por `EscaladoInsumoServicioImpl`. Las transiciones del método real `avanzarEtapa` se agrupan en una nota. En la liberación, la relectura de stock mediante `EntityManager.refresh` también se resume: evita usar cantidades cargadas antes del bloqueo. Finalizar no registra consumo automáticamente; verifica el existente y libera el remanente reservado.

Se omiten getters, setters, conversiones a DTO, redondeos, consultas de presentación del detalle e infraestructura transversal de seguridad y auditoría. Las entidades se representan como datos; las etapas se actualizan con la cascada del lote. Este flujo no reencadena el cronograma del fermentador. Los errores técnicos quedan fuera del diagrama.

## Código de referencia

- [LoteController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/lote/LoteController.java): `finalizarHervido`, `volverDeFinalizarEtapa` y `mostrarDetalle`.
- [LoteServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/lote/LoteServicioImpl.java): `finalizarHervido`, validación del mínimo de consumo, avance y liberación de reservas.
- [ConsumoInsumoServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/lote/ConsumoInsumoServicioImpl.java): `filtrarInsumosRequeridos`.
- [ILoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/ILoteRepository.java), [IOllaHervorRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/equipamiento/IOllaHervorRepository.java), [IReservaInsumoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/IReservaInsumoRepository.java) e [ILoteInsumoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/ingreso_insumo/ILoteInsumoRepository.java): bloqueos y persistencia.
- [ControllerAdvices.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/advice/ControllerAdvices.java): manejo global de rechazos y redirección segura.
