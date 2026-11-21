package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.AjusteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.MotivoAjusteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
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
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AjusteInsumoServicioImpl implements IAjusteInsumoServicio {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final long UNIDADES_POR_CANTIDAD = Math.round(Math.pow(10, LimitesTecnicos.ESCALA_CANTIDADES));

    private final IAjusteInsumoRepository ajusteInsumoRepository;
    private final IMotivoAjusteRepository motivoAjusteRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IReservaInsumoRepository reservaInsumoRepository;

    /**
     * Filtra los ajustes de insumo, opcionalmente por lote de insumo, tipo de ajuste, estado
     * transaccional y/o rango de fecha de registro.
     * <p>
     * A diferencia del resto de los módulos, {@code estado} no asume {@code REGISTRADO} por
     * defecto: incluye tanto los ajustes en estado {@code REGISTRADO} como los {@code ANULADO}
     * cuando no se lo informa, ya que la anulación es un cierre excepcional del registro
     * histórico, no una baja lógica que deba ocultarlo de las búsquedas.
     * </p>
     * <p>
     * Valida que el rango de fechas, si viene completo, no esté invertido; en ese caso no consulta
     * el repositorio.
     * </p>
     *
     * @param idLoteInsumo El ID del lote de insumo a filtrar, o {@code null} para no filtrar por él.
     * @param tipoAjuste El tipo de ajuste (el del motivo del ajuste) a filtrar, o {@code null} para no filtrar por él.
     * @param estado El estado transaccional a filtrar, o {@code null} para no filtrar por él.
     * @param fechaDesde Límite inferior (inclusive) del rango de fecha de registro, o {@code null} para no acotarlo.
     * @param fechaHasta Límite superior (inclusive) del rango de fecha de registro, o {@code null} para no acotarlo.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return Una página de ajustes de insumo en formato DTO que cumplen los criterios indicados.
     * @throws ReglaNegocioException Si {@code fechaDesde} es posterior a {@code fechaHasta}.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AjusteInsumoResponseDTO> filtrarAjustesInsumos(Long idLoteInsumo, TipoAjuste tipoAjuste, EstadoTransaccion estado, LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new ReglaNegocioException("La fecha hasta no puede ser anterior a la fecha desde");
        }

        return ajusteInsumoRepository.filtrarAjustesInsumo(idLoteInsumo, tipoAjuste, estado, fechaDesde, fechaHasta, pageable)
                .map(MapperAjusteInsumo::toDTO);
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
     * @throws ReglaNegocioException Si los datos, el lote de insumo o el motivo de ajuste no fueron informados, si el motivo de ajuste está dado de baja (campo {@code idMotivoAjuste}), si la cantidad es nula, NaN, infinita, menor o igual a cero, supera el máximo o tiene más decimales que la escala, si la observación no fue informada, si un ajuste de tipo INGRESO se aplica sobre un lote de insumo vencido, o si un ajuste de tipo EGRESO descontaría más cantidad que la cantidad actual del lote de insumo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.AJUSTE_INSUMO)
    public AjusteInsumoResponseDTO registrarAjusteInsumo(AjusteInsumoFormDTO ajusteInsumoFormDTO) {
        // 1. Validar que se hayan informado los datos, el lote de insumo y el motivo de ajuste
        if (ajusteInsumoFormDTO == null) {
            throw new ReglaNegocioException("Los datos del ajuste de insumo son obligatorios");
        }
        if (ajusteInsumoFormDTO.getIdLoteInsumo() == null) {
            throw new ReglaNegocioException("El lote de insumo es obligatorio", "idLoteInsumo");
        }
        if (ajusteInsumoFormDTO.getIdMotivoAjuste() == null) {
            throw new ReglaNegocioException("El motivo de ajuste es obligatorio", "idMotivoAjuste");
        }

        // 2. Validar la cantidad (informada, finita, mayor a 0, dentro del máximo y de la escala) y la observación
        double cantidad = validarCantidad(ajusteInsumoFormDTO.getCantidad());
        String observacion = NormalizacionTexto.nombrePropio(ajusteInsumoFormDTO.getObservacion());
        if (observacion == null || observacion.isEmpty()) {
            throw new ReglaNegocioException("La observación es obligatoria", "observacion");
        }
        ValidacionesNumericas.validarLongitudMaxima(observacion, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "La observación", "observacion");

        // 3. Validar que el motivo de ajuste esté registrado en el sistema y activo: uno dado de baja se informa como tal
        Long idMotivoAjuste = ajusteInsumoFormDTO.getIdMotivoAjuste();
        MotivoAjusteEntity motivoAjusteEntity = motivoAjusteRepository.findById(idMotivoAjuste)
                .orElseThrow(() -> motivoAjusteRepository.existsByIdAndEstado(idMotivoAjuste, Estado.BAJA)
                        ? new ReglaNegocioException("El motivo de ajuste está dado de baja", "idMotivoAjuste")
                        : new RecursoNoEncontradoException("No se encontró el motivo de ajuste con ID: " + idMotivoAjuste));

        // 4. Validar que el lote de insumo esté registrado en el sistema (bloqueado para escritura,
        //    ya que este ajuste va a mutar su stock)
        LoteInsumoEntity loteInsumoEntity = loteInsumoRepository.buscarPorIdParaAjustar(ajusteInsumoFormDTO.getIdLoteInsumo())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + ajusteInsumoFormDTO.getIdLoteInsumo()));

        // 5. Validar que un ajuste de tipo INGRESO no se aplique sobre un lote de insumo vencido
        //    (un EGRESO sí se permite: es la merma típica de un lote vencido)
        if (motivoAjusteEntity.getTipoAjuste() == TipoAjuste.INGRESO
                && loteInsumoEntity.getFechaVencimiento().isBefore(LocalDate.now())) {
            throw new ReglaNegocioException("El lote de insumo está vencido (venció el "
                    + loteInsumoEntity.getFechaVencimiento().format(FORMATO_FECHA) + ") y no admite ajustes de ingreso", "idLoteInsumo");
        }

        // 6. Aplicar el ajuste al lote (valida, para un EGRESO, que no deje cantidad disponible
        //    negativa) y persistirlo
        loteInsumoEntity = aplicarAjusteAlLote(loteInsumoEntity, motivoAjusteEntity.getTipoAjuste(), cantidad);

        // 7. Construir y persistir el ajuste de insumo, y retornar el DTO de respuesta correspondiente
        AjusteInsumoEntity ajusteInsumoEntity = AjusteInsumoEntity.builder()
                .cantidad(cantidad)
                .observacion(observacion)
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
     * @throws ReglaNegocioException Si el ID o los datos de la anulación no fueron informados, si el motivo de anulación no fue informado, si el ajuste no se encuentra en estado {@code REGISTRADO}, o si revertir un ajuste de tipo INGRESO descontaría más cantidad que la cantidad actual del lote de insumo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ANULAR, conceptoAuditoria = ConceptoAuditoria.AJUSTE_INSUMO)
    public AjusteInsumoResponseDTO anularAjusteInsumo(Long id, AnularAjusteInsumoFormDTO anularAjusteInsumoFormDTO) {
        // 1. Validar que se hayan informado el ajuste y el motivo de anulación
        if (id == null) {
            throw new ReglaNegocioException("El ajuste de insumo es obligatorio");
        }
        if (anularAjusteInsumoFormDTO == null) {
            throw new ReglaNegocioException("Los datos de la anulación del ajuste de insumo son obligatorios");
        }
        String motivoAnulacion = NormalizacionTexto.nombrePropio(anularAjusteInsumoFormDTO.getMotivoAnulacion());
        if (motivoAnulacion == null || motivoAnulacion.isEmpty()) {
            throw new ReglaNegocioException("El motivo de anulación es obligatorio", "motivoAnulacion");
        }
        ValidacionesNumericas.validarLongitudMaxima(motivoAnulacion, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "El motivo de anulación", "motivoAnulacion");

        // 2. Localizar el ajuste de insumo bloqueando su fila antes de validar su estado: una segunda anulación del
        //    mismo ajuste espera a la primera y luego lo encuentra ANULADO. Si no existe, se dispara RecursoNoEncontradoException
        AjusteInsumoEntity ajusteInsumoEntity = ajusteInsumoRepository.buscarPorIdParaAnular(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ajuste de insumo con ID: " + id));

        // 3. Validar que el ajuste se encuentre en estado REGISTRADO
        if (ajusteInsumoEntity.getEstado() != EstadoTransaccion.REGISTRADO) {
            throw new ReglaNegocioException("Solo se pueden anular ajustes de insumo en estado Registrado");
        }

        // 4. Recuperar el lote de insumo bloqueado para escritura (no alcanza con navegar la
        //    relación perezosa del ajuste: hay que releerlo con lock, ya que esta anulación va a
        //    mutar su stock) y revertir el ajuste en él (valida, al revertir un INGRESO, que no
        //    deje cantidad disponible negativa), persistiéndolo. El tipo de ajuste se lee del
        //    motivo asociado con seguridad: MotivoAjusteServicioImpl no permite modificar el tipo
        //    de un motivo que ya tenga algún ajuste REGISTRADO, así que nunca puede haber cambiado
        //    entre el registro de este ajuste y su anulación.
        LoteInsumoEntity loteInsumoEntity = loteInsumoRepository.buscarPorIdParaAjustar(ajusteInsumoEntity.getLoteInsumo().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el lote de insumo con ID: " + ajusteInsumoEntity.getLoteInsumo().getId()));
        loteInsumoEntity = revertirAjusteEnLote(loteInsumoEntity, ajusteInsumoEntity.getMotivoAjuste().getTipoAjuste(), ajusteInsumoEntity.getCantidad());
        ajusteInsumoEntity.setLoteInsumo(loteInsumoEntity);

        // 5. Aplicar la anulación sobre el ajuste y persistirlo
        ajusteInsumoEntity.setEstado(EstadoTransaccion.ANULADO);
        ajusteInsumoEntity.setFechaAnulacion(LocalDateTime.now());
        ajusteInsumoEntity.setMotivoAnulacion(motivoAnulacion);

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
            throw new ReglaNegocioException("La cantidad a descontar no puede superar la cantidad actual del lote de insumo ("
                    + describirCantidad(loteInsumoEntity.getCantidadActual(), loteInsumoEntity) + ")");
        }

        double excedenteSobreReservado = Math.max(0, cantidad - loteInsumoEntity.getCantidadDisponible());

        loteInsumoEntity.anularIngreso(cantidad, loteInsumoEntity.getCostoUnitarioPPP());

        if (excedenteSobreReservado > 0) {
            reducirReservasProporcionalmente(loteInsumoEntity, excedenteSobreReservado);
        }

        return loteInsumoRepository.save(loteInsumoEntity);
    }

    /**
     * Reduce, proporcionalmente a su saldo pendiente, las reservas de insumo ACTIVAS sobre el lote de insumo
     * indicado (las consumidas y las liberadas no participan): la cantidad quitada se suma a la
     * {@code cantidadReducidaPorAjuste} de cada reserva (no a la liberada) y se resta de su saldo pendiente; si una
     * reserva se agota sin haberse consumido por completo queda Liberada. Esa misma reducción total se refleja en el
     * contador agregado del propio {@link LoteInsumoEntity}. La anulación de un ajuste no restaura las reservas.
     * <p>
     * Se bloquean las reservas para escritura antes de tocarlas, para evitar que una reserva nueva
     * o un consumo concurrente las modifique al mismo tiempo.
     * </p>
     *
     * <p>
     * El reparto trabaja en unidades enteras de la última cifra de las cantidades y usa el método del mayor resto: cada
     * reserva recibe su parte entera y las unidades que sobran se asignan a las reservas con mayor resto (a igual resto,
     * la de mayor saldo pendiente), sin superar nunca el saldo de ninguna. Así la suma de lo quitado a las reservas es
     * exactamente el excedente (o todo el saldo pendiente, si este es menor) y el contador del lote baja por esa misma
     * suma.
     * </p>
     *
     * @param loteInsumoEntity Lote de insumo sobre el que se reducen las reservas.
     * @param excedente Cantidad total a repartir y descontar entre las reservas activas.
     */
    private void reducirReservasProporcionalmente(LoteInsumoEntity loteInsumoEntity, double excedente) {
        List<ReservaInsumoEntity> reservas = reservaInsumoRepository.buscarPorLoteInsumoIdParaReducir(loteInsumoEntity.getId());

        long[] pendientes = new long[reservas.size()];
        BigInteger totalPendiente = BigInteger.ZERO;
        for (int i = 0; i < pendientes.length; i++) {
            pendientes[i] = aUnidades(reservas.get(i).getCantidadPendiente());
            totalPendiente = totalPendiente.add(BigInteger.valueOf(pendientes[i]));
        }

        BigInteger aReducir = BigInteger.valueOf(aUnidades(excedente)).min(totalPendiente);
        long[] reducciones = repartirPorMayorResto(pendientes, totalPendiente, aReducir);

        long totalReducido = 0;
        for (int i = 0; i < reducciones.length; i++) {
            if (reducciones[i] > 0) {
                reservas.get(i).reducirPorAjuste(deUnidades(reducciones[i]));
                totalReducido += reducciones[i];
            }
        }
        reservaInsumoRepository.saveAll(reservas);

        // El contador del lote baja por lo realmente quitado a las reservas, nunca por el excedente en bruto
        loteInsumoEntity.liberarReserva(Math.min(deUnidades(totalReducido), loteInsumoEntity.getCantidadReservada()));
        // Lo reservado nunca supera lo que físicamente queda en el lote
        double reservadoSobrante = Redondeo.redondear(loteInsumoEntity.getCantidadReservada() - loteInsumoEntity.getCantidadActual());
        if (reservadoSobrante > 0) {
            loteInsumoEntity.liberarReserva(reservadoSobrante);
        }
    }

    /**
     * Reparte {@code aRepartir} unidades enteras en proporción a {@code pendientes} con el método del mayor resto,
     * sin asignar a ninguna posición más que su propio pendiente.
     *
     * @param pendientes Saldo pendiente de cada reserva, en unidades enteras.
     * @param totalPendiente Suma de {@code pendientes}.
     * @param aRepartir Unidades a repartir (no superiores a {@code totalPendiente}).
     * @return Las unidades asignadas a cada posición; su suma es exactamente {@code aRepartir}.
     */
    private static long[] repartirPorMayorResto(long[] pendientes, BigInteger totalPendiente, BigInteger aRepartir) {
        long[] partes = new long[pendientes.length];
        if (totalPendiente.signum() == 0 || aRepartir.signum() == 0) {
            return partes;
        }

        BigInteger[] restos = new BigInteger[pendientes.length];
        BigInteger asignado = BigInteger.ZERO;
        for (int i = 0; i < pendientes.length; i++) {
            BigInteger[] divisionYResto = aRepartir.multiply(BigInteger.valueOf(pendientes[i])).divideAndRemainder(totalPendiente);
            partes[i] = divisionYResto[0].longValueExact();
            restos[i] = divisionYResto[1];
            asignado = asignado.add(divisionYResto[0]);
        }

        // Las unidades que sobran son menos que la cantidad de reservas: una más a cada una de las de mayor resto
        List<Integer> orden = new ArrayList<>();
        for (int i = 0; i < pendientes.length; i++) {
            orden.add(i);
        }
        orden.sort(Comparator.<Integer, BigInteger>comparing(i -> restos[i]).reversed()
                .thenComparing(Comparator.<Integer>comparingLong(i -> pendientes[i]).reversed()));
        long sobrantes = aRepartir.subtract(asignado).longValueExact();
        for (int k = 0; k < sobrantes; k++) {
            partes[orden.get(k)]++;
        }
        return partes;
    }

    private static long aUnidades(double cantidad) {
        return Math.round(Redondeo.redondear(cantidad) * UNIDADES_POR_CANTIDAD);
    }

    private static double deUnidades(long unidades) {
        return Redondeo.redondear((double) unidades / UNIDADES_POR_CANTIDAD);
    }

    /**
     * Valida la cantidad de un ajuste, expresada en la unidad de medida del insumo, y la devuelve redondeada a la
     * escala de las cantidades ({@link LimitesTecnicos#ESCALA_CANTIDADES} decimales): informada, un número finito
     * mayor a 0, que no supere {@link LimitesTecnicos#CANTIDAD_INGRESO_MAXIMA} y con no más de
     * {@link LimitesTecnicos#ESCALA_CANTIDADES} decimales.
     *
     * @param cantidad Cantidad a validar.
     * @return La cantidad redondeada.
     * @throws ReglaNegocioException Si la cantidad es nula, NaN, infinita, menor o igual a cero, supera el máximo o tiene más decimales que la escala.
     */
    private static double validarCantidad(Double cantidad) {
        if (cantidad == null) {
            throw new ReglaNegocioException("La cantidad es obligatoria", "cantidad");
        }
        if (!Double.isFinite(cantidad) || cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor a 0", "cantidad");
        }
        if (cantidad > LimitesTecnicos.CANTIDAD_INGRESO_MAXIMA) {
            throw new ReglaNegocioException("La cantidad no puede superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.CANTIDAD_INGRESO_MAXIMA), "cantidad");
        }
        ValidacionesNumericas.validarMaximoDecimales(cantidad, LimitesTecnicos.ESCALA_CANTIDADES, "La cantidad", "cantidad");
        return Redondeo.redondear(cantidad);
    }

    /**
     * Describe una cantidad de un insumo con la unidad de medida del insumo del lote de insumo (por ejemplo
     * {@code "12,35 kg"}).
     */
    private static String describirCantidad(double cantidad, LoteInsumoEntity loteInsumo) {
        return ValidacionesNumericas.formatearNumero(cantidad) + " " + loteInsumo.getInsumo().getUnidadDeMedida().getSimbolo();
    }
}
