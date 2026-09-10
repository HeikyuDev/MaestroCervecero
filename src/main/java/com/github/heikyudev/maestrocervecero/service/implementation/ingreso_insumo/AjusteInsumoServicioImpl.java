package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.AjusteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.MotivoAjusteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IAjusteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IMotivoAjusteRepository;
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

@Service
@RequiredArgsConstructor
public class AjusteInsumoServicioImpl implements IAjusteInsumoServicio {

    private final IAjusteInsumoRepository ajusteInsumoRepository;
    private final IMotivoAjusteRepository motivoAjusteRepository;
    private final ILoteInsumoRepository loteInsumoRepository;

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
     * disponible del lote de insumo; si es de tipo {@code EGRESO}, se resta.
     * </p>
     *
     * @param ajusteInsumoFormDTO Los datos del ajuste a registrar.
     * @return El ajuste de insumo registrado.
     * @throws RecursoNoEncontradoException Si el motivo de ajuste o el lote de insumo referenciados no existen.
     * @throws ReglaNegocioException Si la cantidad es nula o menor o igual a cero, o si un ajuste de tipo EGRESO descontaría más cantidad que la disponible en el lote de insumo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.AJUSTE_INSUMO)
    public AjusteInsumoResponseDTO registrarAjusteInsumo(AjusteInsumoFormDTO ajusteInsumoFormDTO) {
        // 1. Validar que el motivo de ajuste esté registrado en el sistema
        MotivoAjusteEntity motivoAjusteEntity = motivoAjusteRepository.findById(ajusteInsumoFormDTO.getIdMotivoAjuste())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el motivo de ajuste con ID: " + ajusteInsumoFormDTO.getIdMotivoAjuste()));

        // 2. Validar que el lote de insumo esté registrado en el sistema
        LoteInsumoEntity loteInsumoEntity = loteInsumoRepository.findById(ajusteInsumoFormDTO.getIdLoteInsumo())
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
     * @throws ReglaNegocioException Si el motivo de anulación no fue informado, si el ajuste no se encuentra en estado {@code REGISTRADO}, o si revertir un ajuste de tipo INGRESO dejaría cantidad disponible negativa en el lote de insumo.
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

        // 4. Revertir el ajuste en el lote (valida, al revertir un INGRESO, que no deje cantidad
        //    disponible negativa) y persistirlo
        LoteInsumoEntity loteInsumoEntity = revertirAjusteEnLote(ajusteInsumoEntity.getLoteInsumo(), ajusteInsumoEntity.getMotivoAjuste().getTipoAjuste(), ajusteInsumoEntity.getCantidad());
        ajusteInsumoEntity.setLoteInsumo(loteInsumoEntity);

        // 5. Aplicar la anulación sobre el ajuste y persistirlo
        ajusteInsumoEntity.setEstado(EstadoTransaccion.ANULADO);
        ajusteInsumoEntity.setFechaAnulacion(LocalDateTime.now());
        ajusteInsumoEntity.setMotivoAnulacion(anularAjusteInsumoFormDTO.getMotivoAnulacion());

        return MapperAjusteInsumo.toDTO(ajusteInsumoRepository.save(ajusteInsumoEntity));
    }

    /**
     * Aplica un ajuste de insumo al lote correspondiente y lo persiste: suma la cantidad si el
     * tipo de ajuste es {@code INGRESO}, o la resta (validando que no deje cantidad disponible
     * negativa) si es {@code EGRESO}.
     *
     * @param loteInsumoEntity Lote de insumo a actualizar.
     * @param tipoAjuste Tipo del motivo de ajuste aplicado.
     * @param cantidad Cantidad del ajuste.
     * @return El lote de insumo persistido.
     * @throws ReglaNegocioException Si el ajuste es de tipo EGRESO y la cantidad supera la cantidad disponible del lote de insumo.
     */
    private LoteInsumoEntity aplicarAjusteAlLote(LoteInsumoEntity loteInsumoEntity, TipoAjuste tipoAjuste, double cantidad) {
        // Un ajuste no tiene costo de compra propio: se aplica al PPP actual del lote,
        // lo que lo deja matemáticamente sin cambios (es una corrección de cantidad, no una compra)
        if (tipoAjuste == TipoAjuste.EGRESO) {
            if (cantidad > loteInsumoEntity.getCantidadDisponible()) {
                throw new ReglaNegocioException("La cantidad a descontar no puede superar la cantidad disponible del lote de insumo");
            }
            loteInsumoEntity.anularIngreso(cantidad, loteInsumoEntity.getCostoUnitarioPPP());
        } else {
            loteInsumoEntity.sumarIngreso(cantidad, loteInsumoEntity.getCostoUnitarioPPP());
        }
        return loteInsumoRepository.save(loteInsumoEntity);
    }

    /**
     * Revierte, en el lote correspondiente, el efecto de un ajuste de insumo anulado y lo
     * persiste: resta la cantidad (validando que no deje cantidad disponible negativa) si el
     * ajuste anulado era de tipo {@code INGRESO}, o la suma de vuelta si era de tipo
     * {@code EGRESO}.
     *
     * @param loteInsumoEntity Lote de insumo a actualizar.
     * @param tipoAjuste Tipo del motivo del ajuste anulado.
     * @param cantidad Cantidad del ajuste anulado.
     * @return El lote de insumo persistido.
     * @throws ReglaNegocioException Si el ajuste anulado era de tipo INGRESO y su cantidad supera la cantidad disponible del lote de insumo.
     */
    private LoteInsumoEntity revertirAjusteEnLote(LoteInsumoEntity loteInsumoEntity, TipoAjuste tipoAjuste, double cantidad) {
        if (tipoAjuste == TipoAjuste.INGRESO) {
            if (cantidad > loteInsumoEntity.getCantidadDisponible()) {
                throw new ReglaNegocioException("No se puede anular el ajuste: su cantidad supera la cantidad disponible del lote de insumo");
            }
            loteInsumoEntity.anularIngreso(cantidad, loteInsumoEntity.getCostoUnitarioPPP());
        } else {
            loteInsumoEntity.sumarIngreso(cantidad, loteInsumoEntity.getCostoUnitarioPPP());
        }
        return loteInsumoRepository.save(loteInsumoEntity);
    }
}
