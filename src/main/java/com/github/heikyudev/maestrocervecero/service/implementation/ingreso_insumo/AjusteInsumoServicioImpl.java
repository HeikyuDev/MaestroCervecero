package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.AjusteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.MotivoAjusteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IAjusteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IMotivoAjusteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AjusteInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularAjusteInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.ingreso_insumo.IAjusteInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.AjusteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo.MapperAjusteInsumo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AjusteInsumoServicioImpl implements IAjusteInsumoServicio {

    private final IAjusteInsumoRepository ajusteInsumoRepository;
    private final IMotivoAjusteRepository motivoAjusteRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IReservaInsumoRepository reservaInsumoRepository;

    /**
     * Recupera una página de ajustes de insumo registrados en el sistema.
     * <p>
     * A diferencia del resto de los módulos, incluye tanto los ajustes en estado
     * {@code REGISTRADO} como los {@code ANULADO}: la anulación es un cierre excepcional del
     * registro histórico, no una baja lógica que deba ocultarlo de las búsquedas.
     * </p>
     *
     * @param pageable Configuración de paginación y ordenamiento.
     * @return Una página de ajustes de insumo en formato DTO.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AjusteInsumoResponseDTO> buscarTodos(Pageable pageable) {
        return ajusteInsumoRepository.findAll(pageable).map(MapperAjusteInsumo::toDTO);
    }

    /**
     * Busca y retorna un ajuste de insumo específico mediante su identificador único.
     *
     * @param id El ID del ajuste de insumo.
     * @return El ajuste de insumo correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún ajuste de insumo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public AjusteInsumoResponseDTO buscarPorId(Long id) {
        return MapperAjusteInsumo.toDTO(ajusteInsumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ajuste de insumo con ID: " + id)));
    }

    /**
     * Registra un nuevo ajuste de insumo.
     * <p>
     * Si el motivo de ajuste es de tipo {@code INGRESO}, la cantidad se suma a la cantidad
     * disponible del lote de insumo; si es de tipo {@code EGRESO}, se resta, priorizando la
     * cantidad disponible pero pudiendo comer también de lo ya reservado (repartido
     * proporcionalmente entre las reservas activas) si la cantidad a descontar la excede — una
     * merma o pérdida física no le pregunta al sistema si esa cantidad ya estaba reservada.
     * </p>
     *
     * @param ajusteInsumoFormDTO Los datos del ajuste a registrar.
     * @return El ajuste de insumo registrado.
     * @throws RecursoNoEncontradoException Si el motivo de ajuste o el lote de insumo referenciados no existen.
     * @throws ReglaNegocioException Si la cantidad es nula o menor o igual a cero, o si un ajuste de tipo EGRESO descontaría más cantidad que la cantidad actual del lote de insumo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.AJUSTE_INSUMO)
    public AjusteInsumoResponseDTO registrarAjusteInsumo(AjusteInsumoFormDTO ajusteInsumoFormDTO) {
        // 1. Validar que el motivo de ajuste esté registrado en el sistema
        MotivoAjusteEntity motivoAjusteEntity = motivoAjusteRepository.findById(ajusteInsumoFormDTO.getIdMotivoAjuste())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el motivo de ajuste con ID: " + ajusteInsumoFormDTO.getIdMotivoAjuste()));

        // 2. Validar que el lote de insumo esté registrado en el sistema (bloqueado para escritura,
        //    ya que este ajuste va a mutar su stock)
        LoteInsumoEntity loteInsumoEntity = loteInsumoRepository.buscarPorIdParaAjustar(ajusteInsumoFormDTO.getIdLoteInsumo())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + ajusteInsumoFormDTO.getIdLoteInsumo()));

        // 3. Validar que la cantidad sea mayor a cero
        Double cantidad = ajusteInsumoFormDTO.getCantidad();
        if (cantidad == null || cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor a cero");
        }

        // 4. Aplicar el ajuste al lote (valida, para un EGRESO, que no deje cantidad disponible
        //    negativa) y persistirlo
        loteInsumoEntity = aplicarAjusteAlLote(loteInsumoEntity, motivoAjusteEntity.getTipoAjuste(), cantidad);

        // 5. Construir y persistir el ajuste de insumo, y retornar el DTO de respuesta correspondiente
        AjusteInsumoEntity ajusteInsumoEntity = AjusteInsumoEntity.builder()
                .cantidad(cantidad)
                .observacion(ajusteInsumoFormDTO.getObservacion())
                .motivoAjuste(motivoAjusteEntity)
                .loteInsumo(loteInsumoEntity)
                .estado(EstadoTransaccion.REGISTRADO)
                .build();

        return MapperAjusteInsumo.toDTO(ajusteInsumoRepository.save(ajusteInsumoEntity));
    }

    /**
     * Anula un ajuste de insumo existente en el sistema.
     * <p>
     * No existe la baja lógica para este registro: un ajuste solo puede pasar de
     * {@code REGISTRADO} a {@code ANULADO}, nunca eliminarse.
     * </p>
     *
     * @param id El ID del ajuste de insumo a anular.
     * @param anularAjusteInsumoFormDTO Los datos de la anulación (motivo).
     * @return El ajuste de insumo anulado.
     * @throws RecursoNoEncontradoException Si el ajuste de insumo con el ID especificado no existe.
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el ajuste no se encuentra en estado {@code REGISTRADO}, o si revertir un ajuste de tipo INGRESO descontaría más cantidad que la cantidad actual del lote de insumo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.AJUSTE_INSUMO)
    public AjusteInsumoResponseDTO anularAjusteInsumo(Long id, AnularAjusteInsumoFormDTO anularAjusteInsumoFormDTO) {
        // 1. Validar que se haya informado el motivo de anulación
        if (anularAjusteInsumoFormDTO.getMotivoAnulacion() == null || anularAjusteInsumoFormDTO.getMotivoAnulacion().isBlank()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio");
        }

        // 2. Localizar el ajuste de insumo. Si no existe, se dispara RecursoNoEncontradoException
        AjusteInsumoEntity ajusteInsumoEntity = ajusteInsumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ajuste de insumo con ID: " + id));

        // 3. Validar que el ajuste se encuentre en estado REGISTRADO
        if (ajusteInsumoEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular ajustes de insumo en estado REGISTRADO");
        }

        // 4. Recuperar el lote de insumo bloqueado para escritura (no alcanza con navegar la
        //    relación perezosa del ajuste: hay que releerlo con lock, ya que esta anulación va a
        //    mutar su stock) y revertir el ajuste en él (valida, al revertir un INGRESO, que no
        //    deje cantidad disponible negativa), persistiéndolo
        LoteInsumoEntity loteInsumoEntity = loteInsumoRepository.buscarPorIdParaAjustar(ajusteInsumoEntity.getLoteInsumo().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + ajusteInsumoEntity.getLoteInsumo().getId()));
        loteInsumoEntity = revertirAjusteEnLote(loteInsumoEntity, ajusteInsumoEntity.getMotivoAjuste().getTipoAjuste(), ajusteInsumoEntity.getCantidad());
        ajusteInsumoEntity.setLoteInsumo(loteInsumoEntity);

        // 5. Aplicar la anulación sobre el ajuste y persistirlo
        ajusteInsumoEntity.setEstado(EstadoTransaccion.ANULADO);
        ajusteInsumoEntity.setFechaAnulacion(LocalDateTime.now());
        ajusteInsumoEntity.setMotivoAnulacion(anularAjusteInsumoFormDTO.getMotivoAnulacion());

        return MapperAjusteInsumo.toDTO(ajusteInsumoRepository.save(ajusteInsumoEntity));
    }

    /**
     * Aplica un ajuste de insumo al lote correspondiente y lo persiste: suma la cantidad si el
     * tipo de ajuste es {@code INGRESO}, o la descuenta (priorizando la cantidad disponible y,
     * si no alcanza, repartiendo el excedente proporcionalmente entre las reservas activas sobre
     * ese lote de insumo — ver {@link #descontarDelLote(LoteInsumoEntity, double)}) si es
     * {@code EGRESO}.
     *
     * @param loteInsumoEntity Lote de insumo a actualizar.
     * @param tipoAjuste Tipo del motivo de ajuste aplicado.
     * @param cantidad Cantidad del ajuste.
     * @return El lote de insumo persistido.
     * @throws ReglaNegocioException Si el ajuste es de tipo EGRESO y la cantidad supera la cantidad actual del lote de insumo.
     */
    private LoteInsumoEntity aplicarAjusteAlLote(LoteInsumoEntity loteInsumoEntity, TipoAjuste tipoAjuste, double cantidad) {
        // Un ajuste no tiene costo de compra propio: se aplica al PPP actual del lote,
        // lo que lo deja matemáticamente sin cambios (es una corrección de cantidad, no una compra)
        if (tipoAjuste == TipoAjuste.EGRESO) {
            return descontarDelLote(loteInsumoEntity, cantidad);
        }
        loteInsumoEntity.sumarIngreso(cantidad, loteInsumoEntity.getCostoUnitarioPPP());
        return loteInsumoRepository.save(loteInsumoEntity);
    }

    /**
     * Revierte, en el lote correspondiente, el efecto de un ajuste de insumo anulado y lo
     * persiste: descuenta la cantidad (priorizando la cantidad disponible y, si no alcanza,
     * repartiendo el excedente proporcionalmente entre las reservas activas — mismo mecanismo que
     * aplicar un EGRESO nuevo) si el ajuste anulado era de tipo {@code INGRESO}, o la suma de
     * vuelta si era de tipo {@code EGRESO}.
     *
     * @param loteInsumoEntity Lote de insumo a actualizar.
     * @param tipoAjuste Tipo del motivo del ajuste anulado.
     * @param cantidad Cantidad del ajuste anulado.
     * @return El lote de insumo persistido.
     * @throws ReglaNegocioException Si el ajuste anulado era de tipo INGRESO y su cantidad supera la cantidad actual del lote de insumo.
     */
    private LoteInsumoEntity revertirAjusteEnLote(LoteInsumoEntity loteInsumoEntity, TipoAjuste tipoAjuste, double cantidad) {
        if (tipoAjuste == TipoAjuste.INGRESO) {
            return descontarDelLote(loteInsumoEntity, cantidad);
        }
        loteInsumoEntity.sumarIngreso(cantidad, loteInsumoEntity.getCostoUnitarioPPP());
        return loteInsumoRepository.save(loteInsumoEntity);
    }

    /**
     * Descuenta una cantidad del lote de insumo, reflejando la realidad operativa de una merma o
     * pérdida física: prioriza siempre la cantidad disponible (no reservada), pero si la cantidad a
     * descontar la excede, el excedente se reparte PROPORCIONALMENTE entre todas las reservas de
     * insumo activas sobre ese mismo lote de insumo (sin importar a qué lote de producción
     * pertenezca cada una) — nunca se rechaza el descuento solo porque ya había algo reservado, ya
     * que en la operación real el insumo se pierde igual, esté reservado o no.
     * <p>
     * El único límite real es la cantidad actual del lote de insumo: no se puede perder más
     * cantidad de la que físicamente existe.
     * </p>
     *
     * @param loteInsumoEntity Lote de insumo del que se descuenta.
     * @param cantidad Cantidad a descontar.
     * @return El lote de insumo persistido.
     * @throws ReglaNegocioException Si la cantidad a descontar supera la cantidad actual del lote de insumo.
     */
    private LoteInsumoEntity descontarDelLote(LoteInsumoEntity loteInsumoEntity, double cantidad) {
        if (cantidad > loteInsumoEntity.getCantidadActual()) {
            throw new ReglaNegocioException("La cantidad a descontar no puede superar la cantidad actual del lote de insumo");
        }

        double excedenteSobreReservado = Math.max(0, cantidad - loteInsumoEntity.getCantidadDisponible());

        loteInsumoEntity.anularIngreso(cantidad, loteInsumoEntity.getCostoUnitarioPPP());

        if (excedenteSobreReservado > 0) {
            reducirReservasProporcionalmente(loteInsumoEntity, excedenteSobreReservado);
        }

        return loteInsumoRepository.save(loteInsumoEntity);
    }

    /**
     * Reduce, proporcionalmente a lo que cada una ya tenía reservado, la {@code cantidadReservada}
     * de todas las reservas de insumo activas sobre el lote de insumo indicado, y refleja esa
     * misma reducción total en el contador agregado del propio {@link LoteInsumoEntity}.
     * <p>
     * Se bloquean las reservas para escritura antes de tocarlas, para evitar que una reserva nueva
     * o un consumo concurrente las modifique al mismo tiempo.
     * </p>
     *
     * @param loteInsumoEntity Lote de insumo sobre el que se reducen las reservas.
     * @param excedente Cantidad total a repartir y descontar entre las reservas activas.
     */
    private void reducirReservasProporcionalmente(LoteInsumoEntity loteInsumoEntity, double excedente) {
        List<ReservaInsumoEntity> reservas = reservaInsumoRepository.buscarPorLoteInsumoIdParaReducir(loteInsumoEntity.getId());
        double totalReservado = reservas.stream().mapToDouble(ReservaInsumoEntity::getCantidadReservada).sum();

        for (ReservaInsumoEntity reserva : reservas) {
            double proporcion = reserva.getCantidadReservada() / totalReservado;
            reserva.setCantidadReservada(reserva.getCantidadReservada() - (excedente * proporcion));
        }
        reservaInsumoRepository.saveAll(reservas);

        loteInsumoEntity.setCantidadReservada(loteInsumoEntity.getCantidadReservada() - excedente);
    }
}
