# Mejoras Futuras

Este documento registra funcionalidades identificadas durante el desarrollo que **no se van a implementar todavía**, pero que aportan valor real al sistema y quedan documentadas para retomarlas más adelante.

---

## 1. Re-reserva automática por FEFO tras una merma que afecta cantidad reservada

### Contexto

Al registrar un `AjusteInsumo` de tipo `EGRESO` (o al anular uno de tipo `INGRESO`) que supera la `cantidadDisponible` de un `LoteInsumoEntity`, el sistema actual (`AjusteInsumoServicioImpl.descontarDelLote`) reduce **proporcionalmente** la `cantidadReservada` de todas las `ReservaInsumoEntity` activas sobre ese lote de insumo. Esto evita que el usuario tenga que cancelar un lote de producción cuando en la realidad operativa lo que ocurrió fue la rotura/pérdida de una bolsa física de insumo.

El problema que deja abierto ese comportamiento: la(s) etapa(s) de producción que tenían esa cantidad reservada quedan con **menos insumo reservado del que en verdad necesitan** para completar su receta. Hoy el sistema no hace nada para compensar esa merma — el faltante queda silenciosamente reflejado como una reserva más chica.

### Propuesta

En lugar de solo reducir la reserva afectada, el sistema debería intentar **re-reservar automáticamente la cantidad faltante usando el mismo algoritmo FEFO** (First Expired, First Out) que ya se usa en `iniciarLote`, buscando otro(s) lote(s) de insumo disponibles para cubrir el déficit.

Ejemplo: se pierden 100 Kg de un lote físico de Malta A que tenía reservados 60 Kg para la etapa de Maceración del Lote de producción #12. Hoy, esa reserva simplemente queda reducida (o en 0, si el excedente la consume entera). Con la mejora, el sistema debería buscar automáticamente otro lote de Malta A en stock (el siguiente más próximo a vencer) y generar una nueva `ReservaInsumoEntity` para cubrir esos 60 Kg faltantes de esa misma etapa.

### Por qué no es trivial

1. **Cruce de módulos**: hoy `AjusteInsumoServicioImpl` (paquete `ingreso_insumo`) no depende en absoluto de la lógica de `Lote`/`ReservaInsumo`. La lógica FEFO de reserva vive hoy embebida dentro de `LoteServicioImpl` (`obtenerYValidarStockDisponible`, `reservarInsumosFEFO`). Para reutilizarla desde Ajuste, hay que extraerla a un componente compartido (p. ej. un `IReservaInsumoServicio` o similar) del que dependan ambos servicios, en lugar de que uno le pegue directamente a la implementación del otro.

2. **Qué pasa si no alcanza el stock**: si no queda otro lote de ese insumo en stock (o no alcanza para cubrir todo el déficit), no se puede bloquear el registro del ajuste — el operario igual necesita dejar constancia de la merma real. El comportamiento actual (reducir la reserva proporcionalmente) pasaría a ser el **fallback**: se re-reserva todo lo que se pueda vía FEFO, y lo que no se logre cubrir cae en la reducción de reserva existente.

3. **Un solo ajuste puede afectar múltiples etapas/lotes de producción a la vez**: un mismo `LoteInsumoEntity` puede tener reservas activas de varias `EtapaLoteEntity` distintas (de distintos lotes de producción en curso). Una sola merma podría necesitar disparar **N re-reservas FEFO** en la misma transacción — mismo patrón que ya maneja `iniciarLote` para múltiples requerimientos, pero ahora encadenado dentro del flujo de un ajuste.

4. **Cobertura de tests**: hace falta cubrir como mínimo tres escenarios — cobertura total (hay stock suficiente en otro lote para cubrir todo el faltante), cobertura parcial (se cubre una parte y el resto cae en la reducción de reserva actual) y cobertura nula (no hay stock alternativo, se preserva el comportamiento de hoy).

### Cuándo retomarla

Después de cerrar "Registrar Consumo de Insumo", ya que esa funcionalidad también trabaja de lleno sobre `ReservaInsumoEntity` y va a dejar más clara la forma correcta de manipular reservas antes de meterle una re-reserva automática encima.
