# Diagrama de secuencia de diseño: finalizar envasado

El diagrama muestra cómo `LoteController.finalizarEnvasado` y `LoteServicioImpl.finalizarEnvasado` cierran el proceso productivo: finalizan el lote, actualizan fermentador y barriles, registran costos históricos y revisan el cronograma y la planificación. Parte del detalle del lote ya abierto, con un usuario autorizado mediante `LOTE_FINALIZAR_ENVASADO`.

```mermaid
sequenceDiagram
    actor Usuario
    participant Vista as Interfaz web (Thymeleaf)
    participant C as LoteController
    participant S as LoteServicioImpl
    participant B as OrdenCanonicoDeBloqueo
    participant E as IEnvasadoLoteRepository
    participant F as IFermentadorRepository
    participant R as IBarrilRepository
    participant D as ICostoDirectoAdicionalRepository
    participant L as ILoteRepository
    participant P as IPlanificacionProduccionRepository
    participant A as ControllerAdvices

    Usuario->>Vista: Confirmar finalización de envasado
    Vista->>C: POST /lotes/{id}/etapas/envasado/finalizar
    C->>S: finalizarEnvasado(id)
    Note over S: Validar identificador informado
    alt Finalización permitida (búsquedas y validaciones satisfactorias)
        S->>B: bloquearLote(id, true)
        Note over B: Bloquear planificación, fermentador, cola pendiente y lote<br/>en orden canónico
        B-->>S: LoteEntity bajo bloqueo de escritura
        Note over S: Exigir lote EN_EJECUCION y envasado EN_CURSO
        S->>E: sumarCantidadEnvasadaRegistrada(idEtapaEnvasado)
        E-->>S: Total de litros envasados registrados
        Note over S: Exigir total envasado mayor a 0
        S->>F: buscarPorIdParaCambiarEstadoOperativo(idFermentador)
        F-->>S: FermentadorEntity activo bajo bloqueo de escritura
        Note over S: Fermentador EN_LIMPIEZA
        S->>F: save(fermentador)
        S->>R: buscarBarrilesEnvasadosDelLoteParaCambiarEstadoOperativo(id)
        R-->>S: Barriles activos con envasados registrados, bloqueados por ID
        Note over S: Los barriles EN_USO pasan a CON_CERVEZA
        S->>R: saveAll(barrilesEnvasados)
        Note over S: Envasado FINALIZADA y lote FINALIZADO<br/>Registrar fechas de finalización
        S->>D: findAllActivos()
        D-->>S: Costos directos adicionales activos
        Note over S: registrarCostosDirectosDelLote:<br/>congelar costo por litro y calcular subtotal sobre litros realmente envasados
        S->>L: save(lote)
        L-->>S: Lote finalizado con etapas y detalles de costos persistidos
        S->>L: flush()
        Note over S,L: Volcar el cierre antes de consultar cronograma y producción
        Note over S: reencadenarColaPendienteDeFermentador(idFermentador, true)<br/>Recalcular y guardar fechas modificadas mediante ILoteRepository<br/>respetando duraciones, planificaciones y limpieza
        S->>P: buscarPorIdParaActualizar(idPlanificacionProduccion)
        P-->>S: PlanificacionProduccionEntity bajo bloqueo de escritura
        opt Planificación PENDIENTE
            S->>E: sumarLitrosProducidosDePlanificacion(idPlanificacionProduccion)
            E-->>S: Litros producidos por sus lotes finalizados
            opt Litros producidos alcanzan la cantidad a producir
                S->>L: existsByPlanificacionProduccion_IdAndEstadoIn(idPlanificacionProduccion, estadosVivos)
                L-->>S: Existencia de lotes pendientes o en ejecución
                opt No quedan lotes vivos
                    Note over S: Planificación FINALIZADA con fecha de finalización
                    S->>P: save(planificacionProduccion)
                end
            end
        end
        S-->>C: LoteResponseDTO
        Note over C: Mensaje flash con el identificador del lote finalizado
        C-->>Vista: redirect:/lotes/{id}
        Vista->>C: GET /lotes/{id}
        Note over C: mostrarDetalle recupera el lote y su etapa final
        C-->>Vista: Vista lote/lote-detalle
        Vista-->>Usuario: Detalle del lote finalizado y mensaje de éxito
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

- **Interfaz web y LoteController:** representan la confirmación desde `lote/lote-detalle.html`, la petición HTTP y el retorno mediante Post-Redirect-Get al detalle con un mensaje de éxito.
- **LoteServicioImpl y OrdenCanonicoDeBloqueo:** coordinan la transacción y los bloqueos. El parámetro `true` incluye la planificación antes de bloquear fermentador, cola y lote, porque esta operación puede finalizarla.
- **IEnvasadoLoteRepository:** aporta los litros efectivamente envasados del lote y los litros producidos de la planificación. Se exige producción positiva, no alcanzar el volumen objetivo del lote.
- **IFermentadorRepository e IBarrilRepository:** dejan el fermentador en `EN_LIMPIEZA` y los barriles utilizados que están `EN_USO` en `CON_CERVEZA`. Los barriles se recuperan bajo bloqueo por ID ascendente; no se inventa una liberación general de todos los equipos.
- **ICostoDirectoAdicionalRepository e ILoteRepository:** aportan los costos activos y persisten el cierre. Cada `DetalleCostoDirectoEntity` conserva el costo por litro aplicado y su subtotal sobre el total realmente envasado; los detalles y las etapas se guardan con el lote mediante cascada.
- **IPlanificacionProduccionRepository:** participa en `finalizarPlanificacionSiCorresponde`. La planificación solo pasa a `FINALIZADA` si sigue `PENDIENTE`, alcanzó su cantidad a producir y no tiene lotes pendientes ni en ejecución. Si alguna condición no se cumple, el lote igualmente queda finalizado.
- **ControllerAdvices:** maneja los rechazos propagados por el controlador. Una regla incumplida genera una advertencia y un recurso ausente un error; resuelve una redirección segura a partir del origen del pedido y sus alternativas.

El fragmento permitido resume el recorrido satisfactorio. La alternativa representa una interrupción en el primer paso que falle: identificador no informado, lote o etapa incorrectos, ausencia de litros envasados o fechas inválidas al reencadenar generan `ReglaNegocioException`; los recursos no recuperables generan `RecursoNoEncontradoException`. Los pasos posteriores no se ejecutan y la transacción revierte las escrituras previas, incluso si ya hubo `flush`.

El reencadenado se encapsula en una nota para evitar expandir sus consultas internas a `ILoteRepository`. Conserva orden y duraciones de los lotes pendientes, respeta sus planificaciones y deja como mínimo el día siguiente al actual para el próximo inicio por limpieza. El `flush` previo hace que las consultas posteriores ya observen el lote finalizado dentro de la transacción.

Envasado es la última etapa: no se utiliza `avanzarEtapa`, no se valida porcentaje mínimo de consumo y no se liberan reservas de insumos. Este método cierra la etapa usando envasados previamente registrados; no registra nuevas cargas en barriles. Se omiten getters, setters, conversiones a DTO, cálculos aritméticos, consultas de presentación e infraestructura transversal de seguridad y auditoría. Las entidades se representan como datos y los errores técnicos quedan fuera del diagrama.

## Código de referencia

- [LoteController.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/controller/lote/LoteController.java): `finalizarEnvasado`, `redirigirAlDetalle` y `mostrarDetalle`.
- [LoteServicioImpl.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/lote/LoteServicioImpl.java): `finalizarEnvasado`, registro de costos, reencadenado y finalización condicional de la planificación.
- [OrdenCanonicoDeBloqueo.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/service/implementation/lote/OrdenCanonicoDeBloqueo.java): `bloquearLote`.
- [IEnvasadoLoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/IEnvasadoLoteRepository.java), [IFermentadorRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/equipamiento/IFermentadorRepository.java) e [IBarrilRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/barril/IBarrilRepository.java): litros envasados y equipos/barriles.
- [ICostoDirectoAdicionalRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/costo_adicional/ICostoDirectoAdicionalRepository.java), [ILoteRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/lote/ILoteRepository.java) e [IPlanificacionProduccionRepository.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/persistence/repository/planificacion_produccion/IPlanificacionProduccionRepository.java): costos, persistencia y cierre de la planificación.
- [ControllerAdvices.java](../../../../src/main/java/com/github/heikyudev/maestrocervecero/presentation/advice/ControllerAdvices.java): manejo global de rechazos y redirección segura.
