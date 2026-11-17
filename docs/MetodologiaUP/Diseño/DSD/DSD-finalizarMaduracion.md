# Diagrama de secuencia de diseño: finalizar maduración

El diagrama muestra cómo `LoteController.finalizarMaduracion` y `LoteServicioImpl.finalizarMaduracion` verifican el consumo mínimo de la etapa, inician el envasado y liberan las reservas sobrantes de maduración. Parte del detalle del lote ya abierto, con un usuario autorizado mediante `LOTE_FINALIZAR_MADURACION`.

```mermaid
sequenceDiagram
    actor Usuario
    participant Vista as Interfaz web (Thymeleaf)
    participant C as LoteController
    participant S as LoteServicioImpl
    participant L as ILoteRepository
    participant P as IConfiguracionProduccionRepository
    participant N as ConsumoInsumoServicioImpl
    participant R as IReservaInsumoRepository
    participant I as ILoteInsumoRepository
    participant A as ControllerAdvices

    Usuario->>Vista: Confirmar finalización de maduración
    Vista->>C: POST /lotes/{id}/etapas/maduracion/finalizar
    C->>S: finalizarMaduracion(id)
    Note over S: Validar identificador informado
    alt Finalización permitida (búsquedas y validaciones satisfactorias)
        S->>L: buscarPorIdParaActualizar(id)
        L-->>S: LoteEntity bajo bloqueo de escritura
        Note over S: Exigir lote EN_EJECUCION y maduración EN_CURSO
        S->>P: findById(SINGLETON_ID)
        P-->>S: ConfiguracionProduccionEntity con porcentaje mínimo de consumo
        S->>N: filtrarInsumosRequeridos(idEtapaMaduracion)
        N-->>S: Insumos con cantidades requeridas y consumidas de la etapa
        Note over S: validarPorcentajeMinimoConsumido:<br/>cada insumo requerido debe alcanzar el mínimo configurado
        Note over S: avanzarEtapa: maduración FINALIZADA y envasado EN_CURSO<br/>Registrar fecha de fin y fecha de inicio respectivas<br/>El fermentador compartido continúa EN_USO
        S->>R: findByEtapaLoteId(idEtapaMaduracion)
        R-->>S: Reservas de la etapa de maduración
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
        Note over C: Mensaje flash: Etapa de Maduración finalizada correctamente
        C-->>Vista: redirect:/lotes/{id}
        Vista->>C: GET /lotes/{id}
        Note over C: mostrarDetalle recupera el lote y la etapa en curso
        C-->>Vista: Vista lote/lote-detalle
        Vista-->>Usuario: Detalle con envasado en curso y mensaje de éxito
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
- **LoteServicioImpl e ILoteRepository:** coordinan la transacción y bloquean el lote para serializar la finalización con consumos, cancelaciones y solicitudes repetidas. El lote permanece `EN_EJECUCION`; maduración pasa a `FINALIZADA` y envasado a `EN_CURSO`, con sus fechas respectivas.
- **IConfiguracionProduccionRepository y ConsumoInsumoServicioImpl:** aportan el mínimo configurado y las cantidades requeridas y consumidas. La maduración puede requerir insumos, por ejemplo lúpulo de Dry Hop, por lo que se valida el mínimo por insumo. Si no hay requerimientos, no hay consumos que exigir; un mínimo de cero también permite avanzar sin consumo. El rechazo reúne todos los insumos insuficientes.
- **IReservaInsumoRepository e ILoteInsumoRepository:** liberan solo los saldos de reservas activas de maduración y actualizan las cantidades reservadas del stock, conservando el historial y las reservas de otras etapas.
- **ControllerAdvices:** maneja los rechazos propagados por el controlador. Una regla incumplida genera una advertencia y un recurso ausente un error; resuelve una redirección segura a partir del origen del pedido y sus alternativas.

El fragmento permitido resume el recorrido satisfactorio. La alternativa representa una interrupción en el primer paso que falle: identificador no informado, lote o etapa incorrectos y consumo insuficiente generan `ReglaNegocioException`; los recursos no recuperables generan `RecursoNoEncontradoException`. No se ejecutan los pasos posteriores al fallo y la transacción revierte las escrituras previas.

El fermentador es compartido por fermentación, maduración y envasado: continúa `EN_USO`, sin consultas ni escrituras a un repositorio de equipos en esta transición. El método real `avanzarEtapa` se resume en una nota. Las entidades se representan como datos y las etapas se actualizan mediante la cascada del lote.

`filtrarInsumosRequeridos` encapsula la recuperación de etapa, consumos registrados, saldos reservados y requerimientos calculados por `EscaladoInsumoServicioImpl`. La liberación resume la relectura de stock mediante `EntityManager.refresh`, que evita usar cantidades cargadas antes del bloqueo. Finalizar verifica el consumo existente y libera el remanente reservado; no registra consumo ni envasados automáticamente.

Se omiten getters, setters, conversiones a DTO, redondeos, consultas de presentación del detalle e infraestructura transversal de seguridad y auditoría. Este flujo no reencadena el cronograma del fermentador. Los errores técnicos quedan fuera del diagrama.

## Código de referencia

- [LoteController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/lote/LoteController.java): `finalizarMaduracion`, `volverDeFinalizarEtapa` y `mostrarDetalle`.
- [LoteServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/lote/LoteServicioImpl.java): `finalizarMaduracion`, validación del mínimo, avance y liberación de reservas.
- [ConsumoInsumoServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/lote/ConsumoInsumoServicioImpl.java): `filtrarInsumosRequeridos`.
- [ILoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/ILoteRepository.java), [IReservaInsumoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/IReservaInsumoRepository.java) e [ILoteInsumoRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/ingreso_insumo/ILoteInsumoRepository.java): bloqueo y persistencia.
- [ControllerAdvices.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/advice/ControllerAdvices.java): manejo global de rechazos y redirección segura.
