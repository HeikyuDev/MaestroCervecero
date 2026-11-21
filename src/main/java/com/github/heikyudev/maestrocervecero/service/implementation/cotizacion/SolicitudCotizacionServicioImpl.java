package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.ConfiguracionComprasEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoEnvioInvitacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoOrdenCompraPropuesta;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoPlanCompra;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.InvitacionCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.ItemOrdenCompraPropuestaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.LineaSolicitudCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.OfertaCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.OrdenCompraPropuestaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.PlanCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.SolicitudCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.IConfiguracionComprasRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.IInvitacionCotizacionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.IOrdenCompraPropuestaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.ISolicitudCotizacionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IIngresoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IDetalleCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IVersionProveedorRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.orden_compra.OrdenCompraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.EnvioInvitacionPreparado;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.ISolicitudCotizacionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEscaladoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.RequerimientoInsumo;
import com.github.heikyudev.maestrocervecero.service.interfaces.orden_compra.IOrdenCompraServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.ItemOrdenCompraPrearmadaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.OrdenCompraPrearmadaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.OrdenCompraPropuestaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.SolicitudCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.util.EtiquetaPlanificacion;
import com.github.heikyudev.maestrocervecero.util.mapper.cotizacion.MapperSolicitudCotizacion;
import com.github.heikyudev.maestrocervecero.util.method.CantidadesDetalleCompra;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementación de {@link ISolicitudCotizacionServicio}.
 * <p>
 * Los avisos a los gerentes de compras se envían recién cuando la transacción se confirma: si la solicitud o su plan
 * no llegan a guardarse, no se avisa nada. Las invitaciones a los proveedores nunca se envían con la solicitud
 * bloqueada: {@link #prepararEnvios(Long)} guarda el token de cada intento en una transacción corta, el correo se envía
 * fuera de toda transacción ({@code EnvioInvitacionesCotizacion}) y {@link #registrarResultadosEnvio(Long, Map)} guarda
 * el resultado en otra. El cálculo del plan lo hace
 * {@link GeneradorPlanesCompra}, que es puro.
 * </p>
 * <p>
 * Orden de bloqueo: la planificación de producción antes que la solicitud (lo respetan la anulación de la planificación
 * y la confirmación de una orden propuesta); el cierre y la respuesta de un proveedor solo bloquean la solicitud.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudCotizacionServicioImpl implements ISolicitudCotizacionServicio {

    /** Saldo a partir del cual un faltante se considera real (la mitad de la última cifra de las cantidades). */
    static final double SALDO_MINIMO = 0.00005;

    static final String MOTIVO_CANCELACION = "Se anuló la planificación de producción";
    static final String ASUNTO_SIN_PROVEEDOR = "Insumos sin proveedor en una solicitud de cotización";
    static final String ASUNTO_SIN_RESPUESTAS = "Solicitud de cotización cerrada sin ofertas";
    static final String ASUNTO_PLAN_LISTO = "Plan de órdenes de compra listo para revisar";
    static final String ASUNTO_PENDIENTE_CONFIRMACION = "Solicitud de cotización pendiente de confirmación";
    static final String ASUNTO_SIN_SOLICITUD = "No se generó la solicitud de cotización de una planificación";
    static final String ASUNTO_DATOS_INCOMPLETOS = "Insumos sin estimar en una solicitud de cotización";
    static final String ASUNTO_CORREO_NO_ENTREGADO = "Solicitud de cotización no entregada a proveedores";
    static final String ASUNTO_FALLA_CIERRE = "No se pudo cerrar una solicitud de cotización";
    static final String MOTIVO_FALLA_CIERRE = "Ocurrió un error inesperado al generar el plan";

    static final int MAXIMO_INTENTOS_ENVIO = LimitesTecnicos.INTENTOS_ENVIO_INVITACION_MAXIMO;
    static final int MAXIMO_INTENTOS_CIERRE = LimitesTecnicos.INTENTOS_CIERRE_SOLICITUD_MAXIMO;

    private static final Set<EstadoSolicitudCotizacion> ESTADOS_CANCELABLES =
            EnumSet.of(EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION, EstadoSolicitudCotizacion.ABIERTA, EstadoSolicitudCotizacion.CERRADA,
                    EstadoSolicitudCotizacion.PLAN_GENERADO);

    private final ISolicitudCotizacionRepository solicitudRepository;
    private final IInvitacionCotizacionRepository invitacionRepository;
    private final IOrdenCompraPropuestaRepository ordenPropuestaRepository;
    private final IConfiguracionComprasRepository configuracionComprasRepository;
    private final IPlanificacionProduccionRepository planificacionProduccionRepository;
    private final IMaceradorRepository maceradorRepository;
    private final ILoteRepository loteRepository;
    private final ILoteInsumoRepository loteInsumoRepository;
    private final IDetalleCompraRepository detalleCompraRepository;
    private final IIngresoInsumoRepository ingresoInsumoRepository;
    private final IVersionProveedorRepository versionProveedorRepository;
    private final IOrdenCompraRepository ordenCompraRepository;
    private final IEscaladoInsumoServicio escaladoInsumoServicio;
    private final IOrdenCompraServicio ordenCompraServicio;
    private final NotificadorCotizaciones notificador;

    // ==================== Generación de la solicitud ====================

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<SolicitudCotizacionResponseDTO> generarSolicitud(Long idPlanificacion) {
        // 1. Localizar la planificación: una que ya no está Pendiente, o que ya tiene su solicitud, no genera otra
        // Se bloquea la planificación (primer nivel del orden de bloqueo): una anulación simultánea espera a esta
        //    generación o la generación lee la planificación ya anulada y no crea nada
        PlanificacionProduccionEntity planificacion = planificacionProduccionRepository.buscarPorIdParaActualizar(idPlanificacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + idPlanificacion));
        if (planificacion.getEstado() != EstadoSolicitud.PENDIENTE
                || solicitudRepository.existsByPlanificacionProduccion_IdAndEstadoNot(idPlanificacion, EstadoSolicitudCotizacion.CANCELADA)) {
            return Optional.empty();
        }
        String etiqueta = EtiquetaPlanificacion.de(planificacion.getId(), planificacion.getVersionReceta().getNombre());

        // 2. Escalar la receta como un único lote virtual de la cantidad a producir, con los parámetros promedio de los
        //    equipos activos. Sin maceradores activos no se puede escalar la malta: no se genera nada y se avisa
        VersionRecetaEntity versionReceta = planificacion.getVersionReceta();
        Optional<MaceradorEntity> maceradorPromedio = versionReceta.getDetallesMalta().isEmpty() ? Optional.empty() : maceradorPromedio();
        if (!versionReceta.getDetallesMalta().isEmpty() && maceradorPromedio.isEmpty()) {
            ejecutarAlConfirmar(() -> notificador.avisarGerentesDeCompras(null, ASUNTO_SIN_SOLICITUD,
                    "No se generó la solicitud de cotización de la planificación " + etiqueta
                            + " porque no hay maceradores activos para estimar la malta de la receta", List.of()));
            return Optional.empty();
        }
        RequerimientosEstimados estimados = estimarRequerimientos(versionReceta, planificacion.getCantidadAProducir(), maceradorPromedio.orElse(null));

        // 3. Calcular el faltante de cada insumo: requerido - (stock vigente sin reservar - lo que van a consumir los lotes
        //    registrados sin iniciar, nunca menos de 0) - pendiente de recibir
        Map<Long, Double> requeridoPorInsumo = estimados.requeridoPorInsumo();
        Map<Long, Double> disponiblePorInsumo = requeridoPorInsumo.isEmpty() ? Map.of() : calcularDisponible(requeridoPorInsumo.keySet());
        Map<Long, Double> comprometidoPorInsumo = requeridoPorInsumo.isEmpty() ? Map.of()
                : calcularComprometidoEnLotesSinIniciar(requeridoPorInsumo.keySet(), estimados.problemas());
        Map<Long, Double> pendientePorInsumo = requeridoPorInsumo.isEmpty() ? Map.of() : calcularPendienteDeRecepcion(requeridoPorInsumo.keySet());
        SolicitudCotizacionEntity solicitud = SolicitudCotizacionEntity.builder()
                .planificacionProduccion(planificacion)
                .estado(EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION)
                .build();
        for (Map.Entry<Long, Double> requerido : requeridoPorInsumo.entrySet()) {
            double cantidadRequerida = Redondeo.redondear(requerido.getValue());
            double disponible = Redondeo.redondear(disponiblePorInsumo.getOrDefault(requerido.getKey(), 0.0));
            double pendiente = Redondeo.redondear(pendientePorInsumo.getOrDefault(requerido.getKey(), 0.0));
            double comprometido = Redondeo.redondear(comprometidoPorInsumo.getOrDefault(requerido.getKey(), 0.0));
            double libre = Math.max(0.0, Redondeo.redondear(disponible - comprometido));
            double faltante = Redondeo.redondear(cantidadRequerida - libre - pendiente);
            if (faltante > SALDO_MINIMO) {
                solicitud.getLineas().add(LineaSolicitudCotizacionEntity.builder()
                        .solicitud(solicitud)
                        .insumo(estimados.insumos().get(requerido.getKey()))
                        .cantidadRequerida(cantidadRequerida)
                        .cantidadDisponible(disponible)
                        .cantidadComprometidaLotes(comprometido)
                        .cantidadPendienteRecepcion(pendiente)
                        .cantidadFaltante(faltante)
                        .build());
            }
        }

        // 4. Sin faltantes no se solicita nada (los datos incompletos igual se avisan)
        if (solicitud.getLineas().isEmpty()) {
            avisarProblemas(null, etiqueta, estimados.problemas());
            return Optional.empty();
        }

        // 5. Registrar como candidato a cada proveedor activo que ofrece alguno de los insumos faltantes. Todavía no hay
        //    enlaces ni fecha límite: se generan al confirmar el envío
        LocalDateTime ahora = LocalDateTime.now();
        Set<Long> idsFaltantes = solicitud.getLineas().stream().map(linea -> linea.getInsumo().getId()).collect(Collectors.toSet());
        Set<Long> insumosConProveedor = new HashSet<>();
        for (VersionProveedorEntity version : versionProveedorRepository.buscarVigentesDeProveedoresActivosQueOfrecen(idsFaltantes)) {
            Set<Long> ofrecidos = version.getCatalogoProveedor().stream()
                    .map(item -> item.getInsumo().getId())
                    .filter(idsFaltantes::contains)
                    .collect(Collectors.toSet());
            if (ofrecidos.isEmpty()) {
                continue;
            }
            insumosConProveedor.addAll(ofrecidos);
            solicitud.getInvitaciones().add(InvitacionCotizacionEntity.builder()
                    .solicitud(solicitud)
                    .proveedor(version.getProveedor())
                    .estadoEnvio(EstadoEnvioInvitacion.PENDIENTE)
                    .intentosEnvio(0)
                    .build());
        }
        solicitud.getLineas().forEach(linea -> linea.setSinProveedor(!insumosConProveedor.contains(linea.getInsumo().getId())));

        // 6. Sin ningún proveedor candidato nadie puede responder: la solicitud queda cerrada de inmediato y se avisa
        if (solicitud.getInvitaciones().isEmpty()) {
            solicitud.setEstado(EstadoSolicitudCotizacion.CERRADA);
            solicitud.setFechaCierre(ahora);
        }

        // 7. Persistir y, al confirmarse la transacción, pedir a los gerentes de compras que confirmen el envío (o avisar
        //    que ningún proveedor ofrece los insumos) y avisar los datos incompletos
        SolicitudCotizacionEntity guardada = solicitudRepository.save(solicitud);
        List<String> sinProveedor = guardada.getLineas().stream()
                .filter(LineaSolicitudCotizacionEntity::isSinProveedor)
                .map(linea -> linea.getInsumo().getNombre())
                .toList();
        Long idSolicitud = guardada.getId();
        if (guardada.getEstado() == EstadoSolicitudCotizacion.CERRADA) {
            ejecutarAlConfirmar(() -> notificador.avisarGerentesDeCompras(idSolicitud, ASUNTO_SIN_PROVEEDOR,
                    "Ningún proveedor activo ofrece en su catálogo los siguientes insumos faltantes de la solicitud de cotización #" + idSolicitud,
                    sinProveedor));
        } else {
            String explicacion = MapperSolicitudCotizacion.explicacion(guardada);
            List<String> detalles = sinProveedor.isEmpty() ? List.of() : List.of("Ningún proveedor activo ofrece " + String.join(", ", sinProveedor));
            ejecutarAlConfirmar(() -> notificador.avisarGerentesDeCompras(idSolicitud, ASUNTO_PENDIENTE_CONFIRMACION, explicacion, detalles));
        }
        avisarProblemas(idSolicitud, etiqueta, estimados.problemas());
        return Optional.of(MapperSolicitudCotizacion.toDTO(guardada));
    }

    /** Insumos y cantidades requeridas de la planificación, con los grupos de insumos que no se pudieron calcular. */
    private record RequerimientosEstimados(Map<Long, InsumoEntity> insumos, Map<Long, Double> requeridoPorInsumo, List<String> problemas) {
    }

    /**
     * Escala la receta a un lote virtual del volumen indicado con las fórmulas de escalado de insumos, sin validar
     * capacidades de equipos. Cada grupo (malta, lúpulo, levadura) se calcula por separado: si sus datos están
     * incompletos (la fórmula falla o da un resultado que no es un número válido) se informa como problema y se sigue
     * con los demás.
     */
    private RequerimientosEstimados estimarRequerimientos(VersionRecetaEntity versionReceta, Double volumen, MaceradorEntity maceradorPromedio) {
        Map<Long, InsumoEntity> insumos = new LinkedHashMap<>();
        Map<Long, Double> requerido = new LinkedHashMap<>();
        List<String> problemas = new ArrayList<>();
        if (volumen == null || !Double.isFinite(volumen) || volumen <= 0) {
            problemas.add("La cantidad a producir de la planificación no es válida");
            return new RequerimientosEstimados(insumos, requerido, problemas);
        }
        if (!versionReceta.getDetallesMalta().isEmpty()) {
            sumarGrupo("la malta", () -> escaladoInsumoServicio.calcularRequerimientosMalta(versionReceta, volumen, maceradorPromedio), insumos, requerido, problemas);
        }
        if (!versionReceta.getDetallesLupulo().isEmpty()) {
            sumarGrupo("el lúpulo", () -> escaladoInsumoServicio.calcularRequerimientosLupulo(versionReceta, volumen), insumos, requerido, problemas);
        }
        if (!versionReceta.getDetallesLevadura().isEmpty()) {
            sumarGrupo("la levadura", () -> escaladoInsumoServicio.calcularRequerimientosLevadura(versionReceta, volumen), insumos, requerido, problemas);
        }
        return new RequerimientosEstimados(insumos, requerido, problemas);
    }

    private static void sumarGrupo(String grupo, java.util.function.Supplier<List<RequerimientoInsumo>> calculo, Map<Long, InsumoEntity> insumos,
                                   Map<Long, Double> requerido, List<String> problemas) {
        List<RequerimientoInsumo> requerimientos;
        try {
            requerimientos = calculo.get();
        } catch (RuntimeException e) {
            log.warn("No se pudo estimar {} de la receta: {}", grupo, e.getMessage());
            problemas.add("No se pudo calcular la cantidad requerida de " + grupo + ": revise los datos de la receta y de sus insumos");
            return;
        }
        if (requerimientos.stream().anyMatch(r -> r.insumo() == null || !Double.isFinite(r.cantidadRequerida()) || r.cantidadRequerida() < 0)) {
            problemas.add("No se pudo calcular la cantidad requerida de " + grupo + ": revise los datos de la receta y de sus insumos");
            return;
        }
        for (RequerimientoInsumo requerimiento : requerimientos) {
            insumos.putIfAbsent(requerimiento.insumo().getId(), requerimiento.insumo());
            requerido.merge(requerimiento.insumo().getId(), requerimiento.cantidadRequerida(), Double::sum);
        }
    }

    /** Avisa a los gerentes de compras los grupos de insumos que no se pudieron calcular, si hay alguno. */
    private void avisarProblemas(Long idSolicitud, String etiqueta, List<String> problemas) {
        if (problemas.isEmpty()) {
            return;
        }
        List<String> detalles = List.copyOf(problemas);
        String mensaje = idSolicitud == null
                ? "No se generó la solicitud de cotización de la planificación " + etiqueta + " porque faltan datos para estimar sus insumos"
                : "La solicitud de cotización #" + idSolicitud + " de la planificación " + etiqueta + " no incluye los insumos cuyos datos están incompletos";
        ejecutarAlConfirmar(() -> notificador.avisarGerentesDeCompras(idSolicitud, ASUNTO_DATOS_INCOMPLETOS, mensaje, detalles));
    }

    /**
     * Un macerador sin persistir con la eficiencia de maceración promedio de los maceradores activos (el único
     * parámetro de equipo que usan las fórmulas de escalado de insumos), o vacío si no hay ninguno.
     */
    private Optional<MaceradorEntity> maceradorPromedio() {
        List<MaceradorEntity> activos = maceradorRepository.buscarActivos().stream()
                .filter(macerador -> macerador.getEficienciaMaceracion() != null && macerador.getEficienciaMaceracion() > 0)
                .toList();
        if (activos.isEmpty()) {
            return Optional.empty();
        }
        double eficiencia = activos.stream().mapToDouble(MaceradorEntity::getEficienciaMaceracion).average().orElse(0);
        return Optional.of(MaceradorEntity.builder().eficienciaMaceracion(eficiencia).build());
    }

    // ==================== Confirmación del envío ====================

    @Override
    @Transactional
    public SolicitudCotizacionResponseDTO confirmarEnvio(Long id) {
        // 1. Bloquear la planificación y luego la solicitud (orden de bloqueo del proceso)
        Long idPlanificacion = solicitudRepository.buscarIdPlanificacion(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la solicitud de cotización con ID: " + id));
        PlanificacionProduccionEntity planificacion = planificacionProduccionRepository.buscarPorIdParaActualizar(idPlanificacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: " + idPlanificacion));
        SolicitudCotizacionEntity solicitud = solicitudRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la solicitud de cotización con ID: " + id));

        // 2. Validar que siga pendiente de confirmación y que la planificación siga pendiente
        if (solicitud.getEstado() != EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION) {
            throw new ReglaNegocioException("Solo se puede confirmar el envío de una solicitud de cotización pendiente de confirmación");
        }
        if (planificacion.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ReglaNegocioException("No se puede confirmar el envío porque la planificación de producción ya no está Pendiente");
        }

        // 3. Volver a verificar a los proveedores candidatos: se quitan los que se dieron de baja o ya no ofrecen ningún
        //    insumo faltante en su catálogo vigente, y se recalcula qué insumos quedan sin proveedor
        Set<Long> idsFaltantes = solicitud.getLineas().stream().map(linea -> linea.getInsumo().getId()).collect(Collectors.toSet());
        Set<Long> insumosConProveedor = new HashSet<>();
        solicitud.getInvitaciones().removeIf(invitacion -> {
            VersionProveedorEntity version = MapperSolicitudCotizacion.versionVigente(invitacion.getProveedor());
            if (invitacion.getProveedor().getEstado() != Estado.ACTIVO || version == null) {
                return true;
            }
            Set<Long> ofrecidos = version.getCatalogoProveedor().stream().map(item -> item.getInsumo().getId())
                    .filter(idsFaltantes::contains).collect(Collectors.toSet());
            insumosConProveedor.addAll(ofrecidos);
            return ofrecidos.isEmpty();
        });
        if (solicitud.getInvitaciones().isEmpty()) {
            throw new ReglaNegocioException("No se puede confirmar el envío porque ningún proveedor candidato sigue activo y ofrece los insumos faltantes: cancele la solicitud");
        }
        solicitud.getLineas().forEach(linea -> linea.setSinProveedor(!insumosConProveedor.contains(linea.getInsumo().getId())));

        // 4. Abrir la solicitud: el plazo de respuesta corre desde ahora y los enlaces vencen en la fecha límite
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaLimite = ahora.plusHours(buscarConfiguracion().getPlazoRespuestaHoras());
        solicitud.setEstado(EstadoSolicitudCotizacion.ABIERTA);
        solicitud.setFechaConfirmacionEnvio(ahora);
        solicitud.setFechaLimiteRespuesta(fechaLimite);
        solicitud.getInvitaciones().forEach(invitacion -> {
            invitacion.setFechaExpiracion(fechaLimite);
            invitacion.setEstadoEnvio(EstadoEnvioInvitacion.PENDIENTE);
        });
        return MapperSolicitudCotizacion.toDTO(solicitudRepository.save(solicitud));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudCotizacionResponseDTO> buscarPendientesDeConfirmacion() {
        return solicitudRepository.buscarPendientesDeConfirmacion().stream().map(MapperSolicitudCotizacion::toDTOResumen).toList();
    }

    // ==================== Envío de las invitaciones ====================

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<EnvioInvitacionPreparado> prepararEnvios(Long idSolicitud) {
        Optional<SolicitudCotizacionEntity> encontrada = solicitudRepository.buscarPorIdParaActualizar(idSolicitud);
        LocalDateTime ahora = LocalDateTime.now();
        if (encontrada.isEmpty() || encontrada.get().getEstado() != EstadoSolicitudCotizacion.ABIERTA
                || encontrada.get().getFechaLimiteRespuesta() == null || !encontrada.get().getFechaLimiteRespuesta().isAfter(ahora)) {
            return List.of();
        }
        SolicitudCotizacionEntity solicitud = encontrada.get();
        Map<Long, String> nombresFaltantes = solicitud.getLineas().stream()
                .collect(Collectors.toMap(linea -> linea.getInsumo().getId(), linea -> linea.getInsumo().getNombre(), (a, b) -> a, LinkedHashMap::new));
        List<EnvioInvitacionPreparado> preparados = new ArrayList<>();
        for (InvitacionCotizacionEntity invitacion : solicitud.getInvitaciones()) {
            if ((invitacion.getEstadoEnvio() != EstadoEnvioInvitacion.PENDIENTE && invitacion.getEstadoEnvio() != EstadoEnvioInvitacion.FALLIDA)
                    || invitacion.getIntentosEnvio() >= MAXIMO_INTENTOS_ENVIO) {
                continue;
            }
            // Un proveedor dado de baja o sin versión vigente no recibe el correo: queda omitido para siempre
            VersionProveedorEntity version = MapperSolicitudCotizacion.versionVigente(invitacion.getProveedor());
            if (invitacion.getProveedor().getEstado() != Estado.ACTIVO || version == null) {
                invitacion.setEstadoEnvio(EstadoEnvioInvitacion.OMITIDA);
                continue;
            }
            List<String> insumos = version.getCatalogoProveedor().stream()
                    .map(item -> item.getInsumo().getId())
                    .distinct()
                    .filter(nombresFaltantes::containsKey)
                    .map(nombresFaltantes::get)
                    .toList();
            // Cada intento usa un token nuevo (el del intento fallido nunca llegó) y se guarda antes de enviar: si el
            // resultado no se llega a registrar, el enlace que recibió el proveedor igual sirve
            String token = TokenCotizacion.generar();
            invitacion.setTokenHash(TokenCotizacion.calcularHash(token));
            invitacion.setIntentosEnvio(invitacion.getIntentosEnvio() + 1);
            invitacion.setFechaUltimoIntentoEnvio(ahora);
            invitacion.setEstadoEnvio(EstadoEnvioInvitacion.EN_ENVIO);
            preparados.add(new EnvioInvitacionPreparado(invitacion.getId(), version.getEmail(), version.getRazonSocial(), token,
                    solicitud.getFechaLimiteRespuesta(), insumos));
        }
        solicitudRepository.save(solicitud);
        return preparados;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarResultadosEnvio(Long idSolicitud, Map<Long, Boolean> resultados) {
        Optional<SolicitudCotizacionEntity> encontrada = solicitudRepository.buscarPorIdParaActualizar(idSolicitud);
        if (encontrada.isEmpty() || resultados == null || resultados.isEmpty()) {
            return;
        }
        SolicitudCotizacionEntity solicitud = encontrada.get();
        List<String> agotadas = new ArrayList<>();
        for (InvitacionCotizacionEntity invitacion : solicitud.getInvitaciones()) {
            Boolean enviada = resultados.get(invitacion.getId());
            if (enviada == null || invitacion.getEstadoEnvio() != EstadoEnvioInvitacion.EN_ENVIO) {
                continue;
            }
            invitacion.setEstadoEnvio(enviada ? EstadoEnvioInvitacion.ENVIADA : EstadoEnvioInvitacion.FALLIDA);
            if (!enviada && invitacion.getIntentosEnvio() >= MAXIMO_INTENTOS_ENVIO) {
                VersionProveedorEntity version = MapperSolicitudCotizacion.versionVigente(invitacion.getProveedor());
                agotadas.add(version == null ? "Proveedor #" + invitacion.getProveedor().getId() : version.getRazonSocial());
            }
        }
        solicitudRepository.save(solicitud);
        if (!agotadas.isEmpty()) {
            Long id = solicitud.getId();
            ejecutarAlConfirmar(() -> notificador.avisarGerentesDeCompras(id, ASUNTO_CORREO_NO_ENTREGADO,
                    "No se pudo entregar por correo la solicitud de cotización #" + id + " a los siguientes proveedores después de "
                            + MAXIMO_INTENTOS_ENVIO + " intentos: el correo no llegó, no es que no hayan respondido", agotadas));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> buscarIdsConEnviosPendientes() {
        return invitacionRepository.buscarIdsSolicitudesConEnviosPendientes(LocalDateTime.now(), MAXIMO_INTENTOS_ENVIO);
    }

    /** Stock vigente sin reservar de cada insumo (lo disponible de los lotes no vencidos), nunca negativo. */
    private Map<Long, Double> calcularDisponible(Collection<Long> idsInsumo) {
        Map<Long, Double> disponible = new HashMap<>();
        for (Object[] fila : loteInsumoRepository.sumarStockPorInsumo(idsInsumo, LocalDate.now())) {
            double valor = fila[1] == null ? 0.0 : ((Number) fila[1]).doubleValue();
            disponible.put((Long) fila[0], Math.max(0.0, valor));
        }
        return disponible;
    }

    /**
     * Lo que van a consumir, por insumo y en la unidad del insumo, los lotes registrados sin iniciar (de cualquier
     * planificación) que todavía no tienen reservas activas, calculado con la instantánea del escalado de cada lote (el
     * mismo cálculo con el que el lote reservará al iniciarse). Un lote cuyo requerimiento no se puede calcular se informa
     * como problema y no suma.
     */
    private Map<Long, Double> calcularComprometidoEnLotesSinIniciar(Collection<Long> idsInsumo, List<String> problemas) {
        Map<Long, Double> comprometido = new HashMap<>();
        for (LoteEntity lote : loteRepository.buscarPendientesSinReservasActivas()) {
            try {
                for (RequerimientoInsumo requerimiento : escaladoInsumoServicio.calcularRequerimientosTotales(lote)) {
                    Long idInsumo = requerimiento.insumo().getId();
                    if (idsInsumo.contains(idInsumo) && Double.isFinite(requerimiento.cantidadRequerida()) && requerimiento.cantidadRequerida() > 0) {
                        comprometido.merge(idInsumo, requerimiento.cantidadRequerida(), Double::sum);
                    }
                }
            } catch (RuntimeException e) {
                log.warn("No se pudo calcular lo que consumirá el lote {}: {}", lote.getId(), e.getMessage());
                problemas.add("No se pudo calcular lo que consumirá el lote " + lote.getIdentificadorInterno()
                        + " sin iniciar: no se descontó del stock disponible");
            }
        }
        return comprometido;
    }

    /** Lo que resta recibir de cada insumo en las órdenes de compra pendientes, en la unidad del insumo. */
    private Map<Long, Double> calcularPendienteDeRecepcion(Collection<Long> idsInsumo) {
        List<DetalleCompraEntity> detalles = detalleCompraRepository.buscarDeOrdenesPendientesPorInsumos(idsInsumo);
        if (detalles.isEmpty()) {
            return Map.of();
        }
        Map<Long, Double> recibidoPorDetalle = new HashMap<>();
        for (IngresoInsumoEntity ingreso : ingresoInsumoRepository.buscarRegistradosDeDetalles(detalles.stream().map(DetalleCompraEntity::getId).toList())) {
            recibidoPorDetalle.merge(ingreso.getDetalleCompra().getId(), ingreso.getCantidadRecibida(), Double::sum);
        }
        Map<Long, Double> pendiente = new HashMap<>();
        for (DetalleCompraEntity detalle : detalles) {
            double restante = CantidadesDetalleCompra.calcularCantidadPendiente(detalle, recibidoPorDetalle.getOrDefault(detalle.getId(), 0.0));
            pendiente.merge(detalle.getCatalogoProveedor().getInsumo().getId(), Math.max(0.0, restante), Double::sum);
        }
        return pendiente;
    }

    // ==================== Consultas ====================

    @Override
    @Transactional(readOnly = true)
    public Page<SolicitudCotizacionResponseDTO> filtrarSolicitudes(EstadoSolicitudCotizacion estado, Long idPlanificacion, Pageable pageable) {
        return solicitudRepository.filtrarSolicitudes(estado, idPlanificacion, pageable).map(MapperSolicitudCotizacion::toDTOResumen);
    }

    @Override
    @Transactional(readOnly = true)
    public SolicitudCotizacionResponseDTO buscarPorId(Long id) {
        return MapperSolicitudCotizacion.toDTO(solicitudRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la solicitud de cotización con ID: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudCotizacionResponseDTO> buscarPendientesDeDecision() {
        return solicitudRepository.buscarConPlanPendienteDeDecision().stream().map(MapperSolicitudCotizacion::toDTOResumen).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> buscarIdsVencidas() {
        return solicitudRepository.buscarIdsVencidas(EstadoSolicitudCotizacion.ABIERTA, LocalDateTime.now());
    }

    // ==================== Cierre y generación del plan ====================

    @Override
    @Transactional
    public void cerrarSolicitud(Long id) {
        // 1. Localizar la solicitud bloqueándola: una respuesta que se está registrando no se cruza con el cierre
        Optional<SolicitudCotizacionEntity> encontrada = solicitudRepository.buscarPorIdParaActualizar(id);
        LocalDateTime ahora = LocalDateTime.now();
        if (encontrada.isEmpty() || encontrada.get().getEstado() != EstadoSolicitudCotizacion.ABIERTA
                || encontrada.get().getFechaLimiteRespuesta().isAfter(ahora)) {
            return;
        }
        SolicitudCotizacionEntity solicitud = encontrada.get();

        // 2. Cerrar la solicitud
        solicitud.setEstado(EstadoSolicitudCotizacion.CERRADA);
        solicitud.setFechaCierre(ahora);

        // 3. Fijar las respuestas contra el catálogo vigente de cada proveedor (desde el cierre se muestran tal como quedan)
        fijarOfertasEnCatalogoVigente(solicitud);

        // 4. Reunir las ofertas de los proveedores activos (las de los dados de baja se descartan), sin las presentaciones
        //    que el proveedor indicó sin stock
        Map<Long, OfertaCotizacionEntity> ofertasPorId = new HashMap<>();
        List<GeneradorPlanesCompra.Oferta> ofertas = new ArrayList<>();
        for (InvitacionCotizacionEntity invitacion : solicitud.getInvitaciones()) {
            if (invitacion.getProveedor().getEstado() != Estado.ACTIVO) {
                continue;
            }
            for (OfertaCotizacionEntity oferta : invitacion.getOfertas()) {
                if (oferta.getCantidadDisponible() <= 0 || oferta.getPrecioUnitario() == null) {
                    continue;
                }
                ofertasPorId.put(oferta.getId(), oferta);
                ofertas.add(new GeneradorPlanesCompra.Oferta(oferta.getId(), oferta.getLineaSolicitud().getId(),
                        invitacion.getProveedor().getId(), oferta.getCatalogoProveedor().getId(),
                        MapperSolicitudCotizacion.contenidoPorPresentacion(oferta.getCatalogoProveedor()),
                        oferta.getPrecioUnitario(), oferta.getCantidadDisponible()));
            }
        }

        // 5. Generar el plan con la configuración de compras vigente
        ConfiguracionComprasEntity configuracion = buscarConfiguracion();
        List<GeneradorPlanesCompra.Demanda> demandas = solicitud.getLineas().stream()
                .map(linea -> new GeneradorPlanesCompra.Demanda(linea.getId(), linea.getCantidadFaltante()))
                .toList();
        Optional<GeneradorPlanesCompra.Plan> plan = GeneradorPlanesCompra.generar(demandas, ofertas,
                configuracion.getCriterioPlanCompra(), configuracion.getPesoPrecio());

        // 6. Sin ofertas útiles la solicitud queda cerrada sin plan y se avisa a los gerentes de compras
        Long idSolicitud = solicitud.getId();
        if (plan.isEmpty()) {
            solicitudRepository.save(solicitud);
            String mensaje = mensajeCierreSinOfertas(solicitud);
            List<String> insumos = solicitud.getLineas().stream().map(linea -> linea.getInsumo().getNombre()).toList();
            ejecutarAlConfirmar(() -> notificador.avisarGerentesDeCompras(idSolicitud, ASUNTO_SIN_RESPUESTAS, mensaje, insumos));
            return;
        }

        // 7. Guardar el plan (una orden propuesta por proveedor) y avisar que está listo
        Map<Long, LineaSolicitudCotizacionEntity> lineasPorId = solicitud.getLineas().stream()
                .collect(Collectors.toMap(LineaSolicitudCotizacionEntity::getId, linea -> linea));
        PlanCompraEntity planEntity = armarPlan(solicitud, plan.get(), ofertasPorId, lineasPorId);
        solicitud.setPlan(planEntity);
        solicitud.setEstado(EstadoSolicitudCotizacion.PLAN_GENERADO);
        solicitud.setFechaGeneracionPlan(ahora);
        solicitudRepository.save(solicitud);

        List<String> detalles = new ArrayList<>(MapperSolicitudCotizacion.toDTO(solicitud).getAvisos());
        detalles.add(0, "Órdenes de compra propuestas: " + planEntity.getOrdenes().size());
        String mensaje = "La solicitud de cotización #" + idSolicitud + " cerró y su plan de órdenes de compra está listo para revisar";
        ejecutarAlConfirmar(() -> notificador.avisarGerentesDeCompras(idSolicitud, ASUNTO_PLAN_LISTO, mensaje, detalles));
    }

    /**
     * El motivo por el que una solicitud cerró sin plan. Distingue los correos que no se pudieron entregar de los
     * proveedores que recibieron la solicitud y no respondieron.
     */
    static String mensajeCierreSinOfertas(SolicitudCotizacionEntity solicitud) {
        String prefijo = "La solicitud de cotización #" + solicitud.getId() + " cerró ";
        List<InvitacionCotizacionEntity> respondieron = solicitud.getInvitaciones().stream()
                .filter(invitacion -> invitacion.getFechaRespuesta() != null).toList();
        if (!respondieron.isEmpty()) {
            return respondieron.stream().noneMatch(MapperSolicitudCotizacion::ofreceConStock)
                    ? prefijo + "porque los proveedores que respondieron no ofrecieron ninguno de los insumos solicitados: no se generó el plan"
                    : prefijo + "sin ofertas de proveedores activos para los insumos solicitados: no se generó el plan";
        }
        long invitados = solicitud.getInvitaciones().stream().filter(invitacion -> invitacion.getEstadoEnvio() != EstadoEnvioInvitacion.OMITIDA).count();
        long noEntregadas = solicitud.getInvitaciones().stream().filter(invitacion -> invitacion.getEstadoEnvio() == EstadoEnvioInvitacion.FALLIDA).count();
        if (noEntregadas > 0 && noEntregadas == invitados) {
            return prefijo + "sin ofertas porque no se pudo entregar el correo a ningún proveedor invitado: no se generó el plan";
        }
        if (noEntregadas > 0) {
            return prefijo + "sin que ningún proveedor respondiera; además, no se pudo entregar el correo a " + noEntregadas
                    + (noEntregadas == 1 ? " proveedor" : " proveedores") + ": no se generó el plan";
        }
        return prefijo + "sin que ningún proveedor respondiera: no se generó el plan";
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallaCierre(Long id, String motivo) {
        Optional<SolicitudCotizacionEntity> encontrada = solicitudRepository.buscarPorIdParaActualizar(id);
        if (encontrada.isEmpty() || encontrada.get().getEstado() != EstadoSolicitudCotizacion.ABIERTA || encontrada.get().isFallaCierre()) {
            return;
        }
        // El detalle técnico solo va al log: la pantalla y el correo muestran un motivo genérico
        log.error("Falló el cierre programado de la solicitud de cotización {}: {}", id, motivo);
        SolicitudCotizacionEntity solicitud = encontrada.get();
        solicitud.setIntentosCierre(solicitud.getIntentosCierre() + 1);
        solicitud.setMotivoFallaCierre(MOTIVO_FALLA_CIERRE);
        if (solicitud.getIntentosCierre() >= MAXIMO_INTENTOS_CIERRE) {
            solicitud.setFallaCierre(true);
            ejecutarAlConfirmar(() -> notificador.avisarGerentesDeCompras(id, ASUNTO_FALLA_CIERRE,
                    "No se pudo cerrar automáticamente la solicitud de cotización #" + id + " después de " + MAXIMO_INTENTOS_CIERRE
                            + " intentos: el proceso programado dejó de reintentarlo", List.of()));
        }
        solicitudRepository.save(solicitud);
    }

    /** Convierte un plan del generador en su entidad: una orden propuesta por proveedor, con sus ítems. */
    private static PlanCompraEntity armarPlan(SolicitudCotizacionEntity solicitud, GeneradorPlanesCompra.Plan plan,
                                              Map<Long, OfertaCotizacionEntity> ofertasPorId,
                                              Map<Long, LineaSolicitudCotizacionEntity> lineasPorId) {
        PlanCompraEntity planEntity = PlanCompraEntity.builder()
                .solicitud(solicitud)
                .estado(EstadoPlanCompra.PROPUESTO)
                .build();
        Map<Long, OrdenCompraPropuestaEntity> ordenPorProveedor = new LinkedHashMap<>();
        for (GeneradorPlanesCompra.Asignacion asignacion : plan.asignaciones()) {
            OfertaCotizacionEntity oferta = ofertasPorId.get(asignacion.oferta().idOferta());
            ProveedorEntity proveedor = oferta.getInvitacion().getProveedor();
            OrdenCompraPropuestaEntity orden = ordenPorProveedor.computeIfAbsent(proveedor.getId(), idProveedor -> OrdenCompraPropuestaEntity.builder()
                    .plan(planEntity)
                    .proveedor(proveedor)
                    .estado(EstadoOrdenCompraPropuesta.PROPUESTA)
                    .build());
            orden.getItems().add(ItemOrdenCompraPropuestaEntity.builder()
                    .ordenPropuesta(orden)
                    .lineaSolicitud(lineasPorId.get(asignacion.oferta().idLinea()))
                    .catalogoProveedor(oferta.getCatalogoProveedor())
                    .cantidad(asignacion.unidades())
                    .precioUnitario(oferta.getPrecioUnitario())
                    .build());
        }
        planEntity.getOrdenes().addAll(ordenPorProveedor.values());
        return planEntity;
    }

    // ==================== Cancelación ====================

    @Override
    @Transactional
    public void cancelarPorAnulacionDePlanificacion(Long idPlanificacion) {
        LocalDateTime ahora = LocalDateTime.now();
        for (Long idSolicitud : solicitudRepository.buscarIdsPorPlanificacionYEstados(idPlanificacion, ESTADOS_CANCELABLES)) {
            // Se carga recién al bloquearla: un cierre que terminó mientras tanto ya dejó su plan guardado
            Optional<SolicitudCotizacionEntity> bloqueada = solicitudRepository.buscarPorIdParaActualizar(idSolicitud);
            if (bloqueada.isEmpty() || !ESTADOS_CANCELABLES.contains(bloqueada.get().getEstado())) {
                continue;
            }
            SolicitudCotizacionEntity solicitud = bloqueada.get();
            marcarCancelada(solicitud, MOTIVO_CANCELACION, ahora);
            PlanCompraEntity plan = solicitud.getPlan();
            if (plan != null) {
                // El plan elegido con órdenes ya confirmadas conserva su estado: esas órdenes de compra siguen vigentes
                boolean elegidoConConfirmadas = plan.getEstado() == EstadoPlanCompra.ELEGIDO && plan.getOrdenes().stream()
                        .anyMatch(orden -> orden.getEstado() == EstadoOrdenCompraPropuesta.CONFIRMADA);
                if (!elegidoConConfirmadas) {
                    plan.setEstado(EstadoPlanCompra.CANCELADO);
                }
                descartarPendientes(plan);
            }
            solicitudRepository.save(solicitud);
        }
    }

    @Override
    @Transactional
    public SolicitudCotizacionResponseDTO cancelarSolicitud(Long id, String motivo) {
        // 1. Validar el motivo
        String motivoCancelacion = NormalizacionTexto.nombrePropio(motivo);
        if (motivoCancelacion == null || motivoCancelacion.isEmpty()) {
            throw new ReglaNegocioException("El motivo de cancelación es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(motivoCancelacion, LimitesTecnicos.LONGITUD_MAXIMA_MOTIVO, "El motivo de cancelación");

        // 2. Bloquear la planificación y luego la solicitud (orden de bloqueo del proceso)
        Long idPlanificacion = solicitudRepository.buscarIdPlanificacion(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la solicitud de cotización con ID: " + id));
        planificacionProduccionRepository.buscarPorIdParaActualizar(idPlanificacion);
        SolicitudCotizacionEntity solicitud = solicitudRepository.buscarPorIdParaActualizar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la solicitud de cotización con ID: " + id));

        // 3. Validar que esté pendiente de confirmación, abierta o con el plan generado sin elegir
        if (solicitud.getEstado() != EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION && solicitud.getEstado() != EstadoSolicitudCotizacion.ABIERTA
                && solicitud.getEstado() != EstadoSolicitudCotizacion.PLAN_GENERADO) {
            throw new ReglaNegocioException("Solo se pueden cancelar solicitudes de cotización pendientes de confirmación, abiertas o con el plan generado");
        }
        PlanCompraEntity plan = solicitud.getPlan();
        if (plan != null && plan.getEstado() == EstadoPlanCompra.ELEGIDO) {
            throw new ReglaNegocioException("No se puede cancelar la solicitud de cotización porque su plan ya fue elegido");
        }

        // 4. Cancelarla: los enlaces vencen en el momento y el plan queda cancelado
        marcarCancelada(solicitud, motivoCancelacion, LocalDateTime.now());
        if (plan != null) {
            plan.setEstado(EstadoPlanCompra.CANCELADO);
            descartarPendientes(plan);
        }
        return MapperSolicitudCotizacion.toDTO(solicitudRepository.save(solicitud));
    }

    /**
     * Deja la solicitud cancelada con la fecha y el motivo, y vence en el momento los enlaces de los proveedores. Si
     * estaba abierta, antes fija las respuestas contra el catálogo vigente (ver {@link #fijarOfertasEnCatalogoVigente}).
     */
    private static void marcarCancelada(SolicitudCotizacionEntity solicitud, String motivo, LocalDateTime ahora) {
        if (solicitud.getEstado() == EstadoSolicitudCotizacion.ABIERTA) {
            fijarOfertasEnCatalogoVigente(solicitud);
        }
        solicitud.setEstado(EstadoSolicitudCotizacion.CANCELADA);
        solicitud.setFechaCancelacion(ahora);
        solicitud.setMotivoCancelacion(motivo);
        solicitud.getInvitaciones().forEach(invitacion -> {
            if (invitacion.getFechaExpiracion() == null || invitacion.getFechaExpiracion().isAfter(ahora)) {
                invitacion.setFechaExpiracion(ahora);
            }
        });
    }

    /**
     * Fija las respuestas de una solicitud que deja de estar abierta contra el catálogo vigente de cada proveedor: cada
     * oferta pasa a apuntar al ítem vigente de su presentación y se quitan las de presentaciones que salieron del
     * catálogo. Desde ese momento las ofertas se muestran tal como quedan guardadas, sin depender de modificaciones
     * posteriores del proveedor. Las respuestas de un proveedor dado de baja (sin versión vigente) no se tocan.
     */
    private static void fijarOfertasEnCatalogoVigente(SolicitudCotizacionEntity solicitud) {
        for (InvitacionCotizacionEntity invitacion : solicitud.getInvitaciones()) {
            if (!MapperSolicitudCotizacion.rigeCatalogoVigente(invitacion)) {
                continue;
            }
            List<MapperSolicitudCotizacion.OfertaVigente> vigentes = MapperSolicitudCotizacion.ofertasVigentes(invitacion);
            Set<OfertaCotizacionEntity> conservadas = Collections.newSetFromMap(new IdentityHashMap<>());
            for (MapperSolicitudCotizacion.OfertaVigente vigente : vigentes) {
                vigente.oferta().setCatalogoProveedor(vigente.itemVigente());
                conservadas.add(vigente.oferta());
            }
            invitacion.getOfertas().removeIf(oferta -> !conservadas.contains(oferta));
        }
    }

    /** Las órdenes propuestas sin confirmar del plan pasan a descartadas; las confirmadas no se tocan. */
    private static void descartarPendientes(PlanCompraEntity plan) {
        plan.getOrdenes().stream()
                .filter(orden -> orden.getEstado() == EstadoOrdenCompraPropuesta.PROPUESTA)
                .forEach(orden -> orden.setEstado(EstadoOrdenCompraPropuesta.DESCARTADA));
    }

    // ==================== Órdenes propuestas ====================

    @Override
    @Transactional(readOnly = true)
    public OrdenCompraPrearmadaResponseDTO prepararOrdenCompra(Long idOrdenPropuesta) {
        OrdenCompraPropuestaEntity orden = buscarOrdenPropuesta(idOrdenPropuesta);
        validarConfirmable(orden);

        VersionProveedorEntity version = MapperSolicitudCotizacion.versionVigente(orden.getProveedor());
        List<ItemOrdenCompraPrearmadaResponseDTO> items = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        for (ItemOrdenCompraPropuestaEntity item : orden.getItems()) {
            Optional<CatalogoProveedorEntity> vigente = MapperSolicitudCotizacion.itemVigente(version, item.getCatalogoProveedor());
            if (vigente.isPresent()) {
                items.add(ItemOrdenCompraPrearmadaResponseDTO.builder()
                        .idCatalogoProveedor(vigente.get().getId())
                        .cantidad(item.getCantidad())
                        .precioUnitario(item.getPrecioUnitario())
                        .build());
            } else {
                avisos.add("El proveedor ya no ofrece " + item.getCatalogoProveedor().getInsumo().getNombre() + " en "
                        + MapperSolicitudCotizacion.nombrePresentacion(item.getCatalogoProveedor().getPresentacionComercial())
                        + ": el ítem no se incluyó");
            }
        }
        PlanCompraEntity plan = orden.getPlan();
        return OrdenCompraPrearmadaResponseDTO.builder()
                .idOrdenPropuesta(orden.getId())
                .idSolicitud(plan.getSolicitud().getId())
                .idPlanificacion(plan.getSolicitud().getPlanificacionProduccion().getId())
                .idProveedor(orden.getProveedor().getId())
                .items(items)
                .avisos(avisos)
                .build();
    }

    @Override
    @Transactional
    /**
     * Ver {@link ISolicitudCotizacionServicio#confirmarOrdenPropuesta}. El formulario puede diferir de la propuesta en
     * cantidades, costos e ítems a propósito: el gerente de compras ajusta la orden con el proveedor antes de confirmarla,
     * y el alta de órdenes de compra vuelve a validar todo.
     */
    public OrdenCompraResponseDTO confirmarOrdenPropuesta(Long idOrdenPropuesta, OrdenCompraFormDTO ordenCompraFormDTO) {
        // 1. Validar que se hayan informado los datos de la orden
        if (ordenCompraFormDTO == null) {
            throw new ReglaNegocioException("Los datos de la orden de compra son obligatorios");
        }

        // 2. Bloquear la planificación y luego la solicitud (orden de bloqueo del proceso) y localizar la orden propuesta
        Long idPlanificacion = ordenPropuestaRepository.buscarIdPlanificacion(idOrdenPropuesta)
                .orElseThrow(() -> ordenPropuestaNoEncontrada(idOrdenPropuesta));
        planificacionProduccionRepository.buscarPorIdParaActualizar(idPlanificacion);
        bloquearSolicitudDe(idOrdenPropuesta);
        OrdenCompraPropuestaEntity orden = buscarOrdenPropuesta(idOrdenPropuesta);

        // 3. Validar los estados y que la orden sea del proveedor y de la planificación propuestos
        validarConfirmable(orden);
        if (!orden.getProveedor().getId().equals(ordenCompraFormDTO.getIdProveedor())) {
            throw new ReglaNegocioException("La orden de compra debe registrarse para el proveedor de la orden propuesta");
        }
        if (!idPlanificacion.equals(ordenCompraFormDTO.getIdPlanificacionProduccion())) {
            throw new ReglaNegocioException("La orden de compra debe registrarse para la planificación de producción de la solicitud de cotización");
        }

        // 4. Registrar la orden de compra con las reglas del alta (otro bean: su transacción se une a esta)
        OrdenCompraResponseDTO ordenCompra = ordenCompraServicio.registrarOrdenCompra(ordenCompraFormDTO);

        // 5. Vincularla a la orden propuesta, que queda confirmada
        orden.setOrdenCompra(ordenCompraRepository.getReferenceById(ordenCompra.getId()));
        orden.setEstado(EstadoOrdenCompraPropuesta.CONFIRMADA);

        // 6. La primera orden confirmada elige el plan: desde ese momento la solicitud no se puede cancelar
        PlanCompraEntity plan = orden.getPlan();
        if (plan.getEstado() == EstadoPlanCompra.PROPUESTO) {
            plan.setEstado(EstadoPlanCompra.ELEGIDO);
        }
        ordenPropuestaRepository.save(orden);
        return ordenCompra;
    }

    @Override
    @Transactional
    public OrdenCompraPropuestaResponseDTO descartarOrdenPropuesta(Long idSolicitud, Long idOrdenPropuesta) {
        // La orden propuesta debe ser de la solicitud indicada
        Long idSolicitudDeLaOrden = ordenPropuestaRepository.buscarIdSolicitud(idOrdenPropuesta)
                .orElseThrow(() -> ordenPropuestaNoEncontrada(idOrdenPropuesta));
        if (!idSolicitudDeLaOrden.equals(idSolicitud)) {
            throw new RecursoNoEncontradoException("No se encontró la orden propuesta con ID " + idOrdenPropuesta
                    + " en la solicitud de cotización #" + idSolicitud);
        }
        bloquearSolicitudDe(idOrdenPropuesta);
        OrdenCompraPropuestaEntity orden = buscarOrdenPropuesta(idOrdenPropuesta);
        if (orden.getEstado() != EstadoOrdenCompraPropuesta.PROPUESTA) {
            throw new ReglaNegocioException("Solo se pueden descartar órdenes propuestas pendientes de confirmar");
        }
        if (orden.getPlan().getEstado() != EstadoPlanCompra.ELEGIDO) {
            throw new ReglaNegocioException("Solo se pueden descartar órdenes propuestas del plan elegido");
        }
        orden.setEstado(EstadoOrdenCompraPropuesta.DESCARTADA);
        return MapperSolicitudCotizacion.toOrdenDTO(ordenPropuestaRepository.save(orden));
    }

    /**
     * Valida que una orden propuesta se pueda confirmar: está pendiente, su plan está propuesto o elegido, su solicitud
     * tiene el plan vigente y el proveedor sigue activo.
     */
    private static void validarConfirmable(OrdenCompraPropuestaEntity orden) {
        if (orden.getEstado() != EstadoOrdenCompraPropuesta.PROPUESTA) {
            throw new ReglaNegocioException("La orden propuesta ya fue " + orden.getEstado().getEtiqueta().toLowerCase());
        }
        PlanCompraEntity plan = orden.getPlan();
        if (plan.getEstado() != EstadoPlanCompra.PROPUESTO && plan.getEstado() != EstadoPlanCompra.ELEGIDO) {
            throw new ReglaNegocioException("No se puede confirmar la orden propuesta porque su plan está " + plan.getEstado().getEtiqueta().toLowerCase());
        }
        if (plan.getSolicitud().getEstado() != EstadoSolicitudCotizacion.PLAN_GENERADO) {
            throw new ReglaNegocioException("No se puede confirmar la orden propuesta porque la solicitud de cotización está "
                    + plan.getSolicitud().getEstado().getEtiqueta().toLowerCase());
        }
        if (orden.getProveedor().getEstado() != Estado.ACTIVO) {
            throw new ReglaNegocioException("No se puede confirmar la orden propuesta porque el proveedor fue dado de baja");
        }
    }

    private void bloquearSolicitudDe(Long idOrdenPropuesta) {
        Long idSolicitud = ordenPropuestaRepository.buscarIdSolicitud(idOrdenPropuesta)
                .orElseThrow(() -> ordenPropuestaNoEncontrada(idOrdenPropuesta));
        solicitudRepository.buscarPorIdParaActualizar(idSolicitud);
    }

    private OrdenCompraPropuestaEntity buscarOrdenPropuesta(Long idOrdenPropuesta) {
        return ordenPropuestaRepository.findById(idOrdenPropuesta).orElseThrow(() -> ordenPropuestaNoEncontrada(idOrdenPropuesta));
    }

    private static RecursoNoEncontradoException ordenPropuestaNoEncontrada(Long idOrdenPropuesta) {
        return new RecursoNoEncontradoException("No se encontró la orden propuesta con ID: " + idOrdenPropuesta);
    }

    private ConfiguracionComprasEntity buscarConfiguracion() {
        return configuracionComprasRepository.findById(ConfiguracionComprasEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("No se encontró la configuración de compras: la aplicación no inicializó correctamente"));
    }

    /** Con una transacción en curso, la acción espera a que se confirme; sin transacción, se ejecuta en el momento. */
    static void ejecutarAlConfirmar(Runnable accion) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        } else {
            accion.run();
        }
    }
}
