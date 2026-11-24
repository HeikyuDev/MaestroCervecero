package com.github.heikyudev.maestrocervecero.util.mapper.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoEnvioInvitacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoOrdenCompraPropuesta;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoPlanCompra;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.InvitacionCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.OfertaCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.ItemOrdenCompraPropuestaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.LineaSolicitudCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.OrdenCompraPropuestaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.PlanCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.SolicitudCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.CoberturaInsumoPlanResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.InvitacionCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.ItemOrdenCompraPropuestaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.LineaSolicitudCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.OrdenCompraPropuestaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.PlanCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.ExplicacionSolicitudCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.SolicitudCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.EtiquetaPlanificacion;
import com.github.heikyudev.maestrocervecero.util.FormatoCantidad;
import com.github.heikyudev.maestrocervecero.util.FormatoImporte;
import com.github.heikyudev.maestrocervecero.util.method.CantidadesDetalleCompra;
import com.github.heikyudev.maestrocervecero.util.method.MetodosVersionado;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesTexto;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MapperSolicitudCotizacion tiene la responsabilidad de mapear una solicitud de cotización, con sus líneas,
 * invitaciones y su plan de órdenes de compra, a sus DTOs de respuesta. También arma los textos que comparten la pantalla
 * del gerente de compras y la del proveedor (tipo del insumo, nombre de la presentación con su contenido) y calcula la
 * cobertura de cada insumo del plan.
 */
public final class MapperSolicitudCotizacion {

    /** Saldo a partir del cual una cantidad sin cubrir se considera cubierta (la mitad de la última cifra de las cantidades). */
    private static final double SALDO_MINIMO = 0.00005;

    private MapperSolicitudCotizacion() {
    }

    /**
     * Mapea una solicitud para los listados: sus datos, los conteos y los avisos, sin líneas, invitaciones ni plan.
     *
     * @param entity Solicitud a convertir.
     * @return El DTO, o {@code null} si la entidad es nula.
     */
    public static SolicitudCotizacionResponseDTO toDTOResumen(SolicitudCotizacionEntity entity) {
        return entity == null ? null : armar(entity, false);
    }

    /**
     * Mapea una solicitud completa: sus datos, los conteos, los avisos, las líneas, las invitaciones y el plan.
     *
     * @param entity Solicitud a convertir.
     * @return El DTO, o {@code null} si la entidad es nula.
     */
    public static SolicitudCotizacionResponseDTO toDTO(SolicitudCotizacionEntity entity) {
        return entity == null ? null : armar(entity, true);
    }

    private static SolicitudCotizacionResponseDTO armar(SolicitudCotizacionEntity entity, boolean completo) {
        PlanCompraResponseDTO plan = entity.getPlan() == null ? null : toPlanDTO(entity.getPlan(), entity.getLineas());
        Set<LineaSolicitudCotizacionEntity> sinProveedor = lineasSinProveedor(entity);
        int respuestas = (int) entity.getInvitaciones().stream().filter(invitacion -> invitacion.getFechaRespuesta() != null).count();

        return SolicitudCotizacionResponseDTO.builder()
                .id(entity.getId())
                .estado(entity.getEstado())
                .idPlanificacion(entity.getPlanificacionProduccion().getId())
                .etiquetaPlanificacion(EtiquetaPlanificacion.de(entity.getPlanificacionProduccion().getId(),
                        entity.getPlanificacionProduccion().getVersionReceta().getNombre()))
                .fechaSolicitud(entity.getCreatedDate())
                .fechaLimiteRespuesta(entity.getFechaLimiteRespuesta())
                .fechaCierre(entity.getFechaCierre())
                .fechaGeneracionPlan(entity.getFechaGeneracionPlan())
                .fechaCancelacion(entity.getFechaCancelacion())
                .motivoCancelacion(entity.getMotivoCancelacion())
                .fechaConfirmacionEnvio(entity.getFechaConfirmacionEnvio())
                .explicacion(entity.getLineas().isEmpty() ? null : explicacion(entity))
                .usuarioCancelacion(entity.getEstado() == EstadoSolicitudCotizacion.CANCELADA ? entity.getLastModifiedBy() : null)
                .cantidadInsumos(entity.getLineas().size())
                .cantidadInvitaciones(entity.getInvitaciones().size())
                .cantidadRespuestas(respuestas)
                .fallaCierre(entity.isFallaCierre())
                .motivoFallaCierre(entity.getMotivoFallaCierre())
                .tiempoRestante(entity.getEstado() == EstadoSolicitudCotizacion.ABIERTA ? tiempoRestante(entity.getFechaLimiteRespuesta(), LocalDateTime.now()) : null)
                .planElegido(entity.getPlan() != null && entity.getPlan().getEstado() == EstadoPlanCompra.ELEGIDO)
                .avisos(armarAvisos(entity, respuestas, plan, sinProveedor))
                .lineas(completo ? entity.getLineas().stream().map(linea -> toLineaDTO(linea, sinProveedor.contains(linea))).toList() : null)
                .invitaciones(completo ? entity.getInvitaciones().stream().map(MapperSolicitudCotizacion::toInvitacionDTO).toList() : null)
                .plan(completo ? plan : null)
                .build();
    }

    /**
     * Las situaciones que el gerente de compras debe conocer: insumos que ningún proveedor ofrece (ver
     * {@link #lineasSinProveedor}), solicitud cerrada sin respuestas o sin ofertas útiles, y plan que no cubre todo el
     * faltante.
     */
    private static List<String> armarAvisos(SolicitudCotizacionEntity entity, int respuestas, PlanCompraResponseDTO plan,
                                            Set<LineaSolicitudCotizacionEntity> lineasSinProveedor) {
        List<String> avisos = new ArrayList<>();
        List<String> sinProveedor = entity.getLineas().stream()
                .filter(lineasSinProveedor::contains)
                .map(linea -> linea.getInsumo().getNombre())
                .toList();
        if (!sinProveedor.isEmpty()) {
            avisos.add("Ningún proveedor activo ofrece " + String.join(", ", sinProveedor));
        }
        List<String> agotadas = entity.getInvitaciones().stream()
                .filter(invitacion -> invitacion.getEstadoEnvio() == EstadoEnvioInvitacion.FALLIDA
                        && invitacion.getIntentosEnvio() >= LimitesTecnicos.INTENTOS_ENVIO_INVITACION_MAXIMO)
                .map(invitacion -> {
                    VersionProveedorEntity version = versionVigente(invitacion.getProveedor());
                    return version == null ? "Proveedor #" + invitacion.getProveedor().getId() : version.getRazonSocial();
                })
                .toList();
        if (!agotadas.isEmpty()) {
            avisos.add("No se pudo entregar por correo la solicitud a " + String.join(", ", agotadas));
        }
        if (entity.getEstado() == EstadoSolicitudCotizacion.CERRADA) {
            List<InvitacionCotizacionEntity> respondieron = entity.getInvitaciones().stream().filter(invitacion -> invitacion.getFechaRespuesta() != null).toList();
            long invitados = entity.getInvitaciones().stream().filter(invitacion -> invitacion.getEstadoEnvio() != EstadoEnvioInvitacion.OMITIDA).count();
            long noEntregadas = entity.getInvitaciones().stream().filter(invitacion -> invitacion.getEstadoEnvio() == EstadoEnvioInvitacion.FALLIDA).count();
            if (!respondieron.isEmpty()) {
                avisos.add(respondieron.stream().noneMatch(MapperSolicitudCotizacion::ofreceConStock)
                        ? "Los proveedores que respondieron no ofrecieron ninguno de los insumos solicitados"
                        : "Ninguna respuesta de un proveedor activo ofreció los insumos solicitados");
            } else if (noEntregadas > 0 && noEntregadas == invitados) {
                avisos.add("No se pudo entregar el correo a ningún proveedor invitado");
            } else if (noEntregadas > 0) {
                avisos.add("Ningún proveedor respondió antes del cierre y no se pudo entregar el correo a " + noEntregadas
                        + (noEntregadas == 1 ? " proveedor" : " proveedores"));
            } else {
                avisos.add("Ningún proveedor respondió antes del cierre");
            }
        }
        if (entity.isFallaCierre()) {
            avisos.add("No se pudo cerrar la solicitud automáticamente: " + entity.getMotivoFallaCierre());
        }
        if (entity.getEstado() == EstadoSolicitudCotizacion.PLAN_GENERADO && plan != null && !plan.isCubreTodo()) {
            avisos.add("El plan no cubre todo el faltante: revise la cantidad sin cubrir de cada insumo");
        }
        return avisos;
    }

    /**
     * Las líneas de la solicitud cuyo insumo ningún proveedor ofrece. Mientras la solicitud está pendiente de
     * confirmación se calcula en el momento con la misma regla que la confirmación del envío
     * ({@link #insumosCubiertosPorInvitados}): un insumo queda sin proveedor si ningún proveedor invitado que sigue activo
     * lo ofrece en su catálogo vigente. En los demás estados es la marca guardada en cada línea. Es de solo lectura: no
     * modifica la marca guardada.
     */
    public static Set<LineaSolicitudCotizacionEntity> lineasSinProveedor(SolicitudCotizacionEntity solicitud) {
        Set<LineaSolicitudCotizacionEntity> sinProveedor = Collections.newSetFromMap(new IdentityHashMap<>());
        if (solicitud.getEstado() == EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION) {
            Set<Long> cubiertos = insumosCubiertosPorInvitados(solicitud);
            solicitud.getLineas().stream().filter(linea -> !cubiertos.contains(linea.getInsumo().getId())).forEach(sinProveedor::add);
        } else {
            solicitud.getLineas().stream().filter(LineaSolicitudCotizacionEntity::isSinProveedor).forEach(sinProveedor::add);
        }
        return sinProveedor;
    }

    /**
     * Los insumos faltantes de la solicitud que ofrece al menos un proveedor invitado (ver
     * {@link #insumosOfrecidosPorInvitado}).
     */
    public static Set<Long> insumosCubiertosPorInvitados(SolicitudCotizacionEntity solicitud) {
        Set<Long> idsFaltantes = idsInsumosFaltantes(solicitud);
        return solicitud.getInvitaciones().stream()
                .flatMap(invitacion -> insumosOfrecidosPorInvitado(invitacion, idsFaltantes).stream())
                .collect(Collectors.toSet());
    }

    /**
     * Los insumos faltantes que un proveedor invitado ofrece en el catálogo de su versión vigente; ninguno si el
     * proveedor está dado de baja o no tiene versión vigente.
     *
     * @param invitacion La invitación del proveedor.
     * @param idsFaltantes IDs de los insumos faltantes de la solicitud.
     */
    public static Set<Long> insumosOfrecidosPorInvitado(InvitacionCotizacionEntity invitacion, Set<Long> idsFaltantes) {
        VersionProveedorEntity version = versionVigente(invitacion.getProveedor());
        if (invitacion.getProveedor().getEstado() != Estado.ACTIVO || version == null) {
            return Set.of();
        }
        return version.getCatalogoProveedor().stream()
                .map(item -> item.getInsumo().getId())
                .filter(idsFaltantes::contains)
                .collect(Collectors.toSet());
    }

    /** IDs de los insumos faltantes de la solicitud (uno por línea). */
    public static Set<Long> idsInsumosFaltantes(SolicitudCotizacionEntity solicitud) {
        return solicitud.getLineas().stream().map(linea -> linea.getInsumo().getId()).collect(Collectors.toSet());
    }

    /**
     * Mapea una línea de la solicitud.
     *
     * @param linea La línea.
     * @param sinProveedor Si ningún proveedor ofrece su insumo (ver {@link #lineasSinProveedor}).
     */
    public static LineaSolicitudCotizacionResponseDTO toLineaDTO(LineaSolicitudCotizacionEntity linea, boolean sinProveedor) {
        InsumoEntity insumo = linea.getInsumo();
        return LineaSolicitudCotizacionResponseDTO.builder()
                .id(linea.getId())
                .idInsumo(insumo.getId())
                .nombreInsumo(insumo.getNombre())
                .tipoInsumo(tipoDeInsumo(insumo))
                .unidad(insumo.getUnidadDeMedida().getSimbolo())
                .cantidadRequerida(linea.getCantidadRequerida())
                .cantidadDisponible(linea.getCantidadDisponible())
                .cantidadComprometidaLotes(linea.getCantidadComprometidaLotes())
                .cantidadPendienteRecepcion(linea.getCantidadPendienteRecepcion())
                .cantidadFaltante(linea.getCantidadFaltante())
                .sinProveedor(sinProveedor)
                .build();
    }

    /**
     * Mapea una invitación con el estado de su respuesta. Las ofertas que se cuentan y se listan son las de
     * {@link #ofertasDeLaRespuesta}.
     */
    public static InvitacionCotizacionResponseDTO toInvitacionDTO(InvitacionCotizacionEntity invitacion) {
        ProveedorEntity proveedor = invitacion.getProveedor();
        VersionProveedorEntity version = versionVigente(proveedor);
        List<OfertaVigente> ofertas = ofertasDeLaRespuesta(invitacion);
        return InvitacionCotizacionResponseDTO.builder()
                .id(invitacion.getId())
                .idProveedor(proveedor.getId())
                .razonSocial(version == null ? null : version.getRazonSocial())
                .cuit(version == null ? null : ValidacionesTexto.formatearCuit(version.getCuit()))
                .proveedorActivo(proveedor.getEstado() == Estado.ACTIVO)
                .fechaRespuesta(invitacion.getFechaRespuesta())
                .cantidadOfertas((int) ofertas.stream().map(OfertaVigente::oferta).filter(MapperSolicitudCotizacion::tieneStock).count())
                .estadoParticipacion(estadoParticipacion(invitacion))
                .insumosCotizados(ofertas.stream()
                        .sorted(Comparator.comparing((OfertaVigente vigente) -> vigente.oferta().getLineaSolicitud().getId())
                                .thenComparing(vigente -> vigente.itemVigente().getId()))
                        .map(vigente -> descripcionOferta(vigente.oferta()))
                        .toList())
                .estadoEnvio(invitacion.getEstadoEnvio())
                .intentosEnvio(invitacion.getIntentosEnvio() == null ? 0 : invitacion.getIntentosEnvio())
                .build();
    }

    /** Lo que falta hacer con una solicitud pendiente de confirmación. */
    public static final String ACCION_PENDIENTE_CONFIRMACION = "Confirme el envío a los proveedores o cancele la solicitud";

    /**
     * Por qué se generó la solicitud y qué falta hacer, en partes para mostrarla como una lista:
     * <ul>
     *     <li>introducción: "Se generó la solicitud de cotización N° 12 para la planificación N° 45. El stock actual no cubre:";</li>
     *     <li>un renglón por cada línea, en su orden: "Malta Pilsen: faltan 120 kg";</li>
     *     <li>acción: {@link #ACCION_PENDIENTE_CONFIRMACION}, solo mientras la solicitud está pendiente de confirmación.</li>
     * </ul>
     *
     * @param solicitud La solicitud, con su ID y sus líneas.
     * @return La explicación.
     */
    public static ExplicacionSolicitudCotizacionResponseDTO explicacion(SolicitudCotizacionEntity solicitud) {
        return ExplicacionSolicitudCotizacionResponseDTO.builder()
                .introduccion("Se generó la solicitud de cotización N° " + solicitud.getId() + " para la planificación N° "
                        + solicitud.getPlanificacionProduccion().getId() + ". El stock actual no cubre:")
                .insumosNoCubiertos(solicitud.getLineas().stream()
                        .map(linea -> tipoDeInsumo(linea.getInsumo()) + " " + linea.getInsumo().getNombre() + ": faltan "
                                + FormatoCantidad.conUnidad(linea.getCantidadFaltante(), linea.getInsumo().getUnidadDeMedida().getSimbolo()))
                        .toList())
                .accion(solicitud.getEstado() == EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION ? ACCION_PENDIENTE_CONFIRMACION : null)
                .build();
    }

    /**
     * La situación del proveedor en la solicitud: dado de baja, correo fallido, respondió, invitado (mientras la
     * solicitud está abierta) o sin respuesta.
     */
    static String estadoParticipacion(InvitacionCotizacionEntity invitacion) {
        if (invitacion.getProveedor().getEstado() != Estado.ACTIVO || invitacion.getEstadoEnvio() == EstadoEnvioInvitacion.OMITIDA) {
            return "Dado de baja";
        }
        if (invitacion.getSolicitud() != null && invitacion.getSolicitud().getEstado() == EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION) {
            return "Sin enviar";
        }
        if (invitacion.getFechaRespuesta() != null) {
            return "Respondió";
        }
        if (invitacion.getEstadoEnvio() == EstadoEnvioInvitacion.FALLIDA) {
            return "Correo fallido";
        }
        return invitacion.getSolicitud() != null && invitacion.getSolicitud().getEstado() == EstadoSolicitudCotizacion.ABIERTA
                ? "Invitado" : "Sin respuesta";
    }

    /**
     * El tiempo que queda hasta la fecha límite, en días y horas (ej. "2 días y 5 horas", "3 horas", "menos de 1
     * hora"), o "Vencida" si ya pasó.
     *
     * @param fechaLimite Fecha límite de respuesta.
     * @param ahora Momento de referencia.
     * @return El texto del tiempo restante.
     */
    public static String tiempoRestante(LocalDateTime fechaLimite, LocalDateTime ahora) {
        if (fechaLimite == null || !fechaLimite.isAfter(ahora)) {
            return "Vencida";
        }
        long horasTotales = java.time.Duration.between(ahora, fechaLimite).toHours();
        long dias = horasTotales / 24;
        long horas = horasTotales % 24;
        String textoDias = dias == 1 ? "1 día" : dias + " días";
        String textoHoras = horas == 1 ? "1 hora" : horas + " horas";
        if (dias > 0 && horas > 0) {
            return textoDias + " y " + textoHoras;
        }
        if (dias > 0) {
            return textoDias;
        }
        return horas > 0 ? textoHoras : "menos de 1 hora";
    }

    /**
     * Mapea un plan con sus órdenes propuestas, su costo total y la cobertura de cada línea de la solicitud.
     *
     * @param plan Plan a convertir.
     * @param lineas Líneas de la solicitud del plan.
     * @return El DTO del plan.
     */
    public static PlanCompraResponseDTO toPlanDTO(PlanCompraEntity plan, List<LineaSolicitudCotizacionEntity> lineas) {
        List<OrdenCompraPropuestaResponseDTO> ordenes = plan.getOrdenes().stream().map(MapperSolicitudCotizacion::toOrdenDTO).toList();
        List<CoberturaInsumoPlanResponseDTO> coberturas = calcularCoberturas(plan, lineas);
        return PlanCompraResponseDTO.builder()
                .id(plan.getId())
                .estado(plan.getEstado())
                .costoTotal(ordenes.stream().map(OrdenCompraPropuestaResponseDTO::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add))
                .cubreTodo(coberturas.stream().allMatch(cobertura -> cobertura.getCantidadSinCubrir() <= SALDO_MINIMO))
                .ordenes(ordenes)
                .coberturas(coberturas)
                .build();
    }

    /**
     * La cobertura de cada línea de la solicitud con los ítems del plan: lo cubierto es la suma, en la unidad del insumo,
     * de las unidades propuestas por el contenido de su presentación. Las órdenes descartadas no cubren nada.
     */
    private static List<CoberturaInsumoPlanResponseDTO> calcularCoberturas(PlanCompraEntity plan, List<LineaSolicitudCotizacionEntity> lineas) {
        Map<Long, Double> cubiertaPorLinea = new LinkedHashMap<>();
        for (OrdenCompraPropuestaEntity orden : plan.getOrdenes()) {
            if (orden.getEstado() == EstadoOrdenCompraPropuesta.DESCARTADA) {
                continue;
            }
            for (ItemOrdenCompraPropuestaEntity item : orden.getItems()) {
                cubiertaPorLinea.merge(item.getLineaSolicitud().getId(), cantidadEnUnidadDelInsumo(item.getCatalogoProveedor(), item.getCantidad()), Double::sum);
            }
        }
        return lineas.stream().map(linea -> {
            double cubierta = Redondeo.redondear(cubiertaPorLinea.getOrDefault(linea.getId(), 0.0));
            return CoberturaInsumoPlanResponseDTO.builder()
                    .idInsumo(linea.getInsumo().getId())
                    .nombreInsumo(linea.getInsumo().getNombre())
                    .unidad(linea.getInsumo().getUnidadDeMedida().getSimbolo())
                    .cantidadFaltante(linea.getCantidadFaltante())
                    .cantidadCubierta(cubierta)
                    .cantidadSinCubrir(Math.max(0.0, Redondeo.redondear(linea.getCantidadFaltante() - cubierta)))
                    .build();
        }).toList();
    }

    /**
     * Mapea una orden propuesta con sus ítems y su total.
     */
    public static OrdenCompraPropuestaResponseDTO toOrdenDTO(OrdenCompraPropuestaEntity orden) {
        List<ItemOrdenCompraPropuestaResponseDTO> items = orden.getItems().stream().map(MapperSolicitudCotizacion::toItemDTO).toList();
        ProveedorEntity proveedor = orden.getProveedor();
        VersionProveedorEntity version = versionVigente(proveedor);
        return OrdenCompraPropuestaResponseDTO.builder()
                .id(orden.getId())
                .idProveedor(proveedor.getId())
                .razonSocial(version == null ? null : version.getRazonSocial())
                .cuit(version == null ? null : ValidacionesTexto.formatearCuit(version.getCuit()))
                .proveedorActivo(proveedor.getEstado() == Estado.ACTIVO)
                .estado(orden.getEstado())
                .idOrdenCompra(orden.getOrdenCompra() == null ? null : orden.getOrdenCompra().getId())
                .total(items.stream().map(ItemOrdenCompraPropuestaResponseDTO::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add))
                .items(items)
                .build();
    }

    /**
     * Si el proveedor ofreció con stock (cantidad mayor a 0) alguna de las ofertas de {@link #ofertasDeLaRespuesta}. Una
     * respuesta solo con presentaciones sin stock o que salieron del catálogo antes del cierre cuenta como respuesta sin
     * ofertas.
     */
    public static boolean ofreceConStock(InvitacionCotizacionEntity invitacion) {
        return ofertasDeLaRespuesta(invitacion).stream().map(OfertaVigente::oferta).anyMatch(MapperSolicitudCotizacion::tieneStock);
    }

    /** Si la oferta indica una cantidad disponible mayor a 0. */
    private static boolean tieneStock(OfertaCotizacionEntity oferta) {
        return oferta.getCantidadDisponible() != null && oferta.getCantidadDisponible() > 0;
    }

    /**
     * Una oferta para mostrar: insumo, unidades, presentación y precio unitario, por ejemplo
     * {@code Malta Pilsen: 2 x Bolsa (25 kg) a $ 32.500,00}, o {@code Malta Pilsen: Sin stock en Bolsa (25 kg)} si el
     * proveedor indicó cantidad 0.
     */
    static String descripcionOferta(OfertaCotizacionEntity oferta) {
        if (oferta.getCantidadDisponible() == null || oferta.getCantidadDisponible() == 0) {
            return oferta.getLineaSolicitud().getInsumo().getNombre() + ": Sin stock en "
                    + nombrePresentacion(oferta.getCatalogoProveedor().getPresentacionComercial());
        }
        return oferta.getLineaSolicitud().getInsumo().getNombre() + ": " + oferta.getCantidadDisponible() + " x "
                + nombrePresentacion(oferta.getCatalogoProveedor().getPresentacionComercial()) + " a "
                + FormatoImporte.conSimbolo(oferta.getPrecioUnitario());
    }

    /**
     * Mapea un ítem de una orden propuesta, con su equivalente y su precio en la unidad del insumo.
     */
    public static ItemOrdenCompraPropuestaResponseDTO toItemDTO(ItemOrdenCompraPropuestaEntity item) {
        CatalogoProveedorEntity catalogo = item.getCatalogoProveedor();
        return ItemOrdenCompraPropuestaResponseDTO.builder()
                .id(item.getId())
                .idCatalogoProveedor(catalogo.getId())
                .nombreInsumo(catalogo.getInsumo().getNombre())
                .nombrePresentacion(nombrePresentacion(catalogo.getPresentacionComercial()))
                .cantidad(item.getCantidad())
                .precioUnitario(item.getPrecioUnitario())
                .subtotal(item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad())))
                .cantidadEnUnidadDelInsumo(cantidadEnUnidadDelInsumo(catalogo, item.getCantidad()))
                .unidad(catalogo.getInsumo().getUnidadDeMedida().getSimbolo())
                .precioPorUnidadDelInsumo(precioPorUnidadDelInsumo(catalogo, item.getPrecioUnitario()))
                .build();
    }

    /**
     * El contenido de una unidad del ítem del catálogo, en la unidad de medida de su insumo.
     *
     * @param catalogo Ítem del catálogo (insumo y presentación).
     * @return Cantidad de insumo por unidad de presentación.
     */
    public static double contenidoPorPresentacion(CatalogoProveedorEntity catalogo) {
        PresentacionComercialEntity presentacion = catalogo.getPresentacionComercial();
        return CantidadesDetalleCompra.calcularCantidadPorPresentacion(presentacion.getCantidad(), presentacion.getUnidadDeMedida(),
                catalogo.getInsumo().getUnidadDeMedida());
    }

    /**
     * Las unidades del ítem del catálogo convertidas a la unidad de medida del insumo, redondeadas a la escala de las
     * cantidades.
     */
    public static double cantidadEnUnidadDelInsumo(CatalogoProveedorEntity catalogo, int unidades) {
        return Redondeo.redondear(unidades * contenidoPorPresentacion(catalogo));
    }

    /**
     * El precio de una unidad de la presentación dividido por su contenido en la unidad del insumo, con 4 decimales.
     */
    public static BigDecimal precioPorUnidadDelInsumo(CatalogoProveedorEntity catalogo, BigDecimal precioUnitario) {
        double contenido = contenidoPorPresentacion(catalogo);
        if (!(contenido > 0)) {
            return null;
        }
        return precioUnitario.divide(BigDecimal.valueOf(contenido), 4, RoundingMode.HALF_UP);
    }

    /**
     * El nombre de la presentación con su contenido, por ejemplo "Bolsa (25 kg)".
     */
    public static String nombrePresentacion(PresentacionComercialEntity presentacion) {
        return presentacion.getNombre() + " (" + FormatoCantidad.conUnidad(presentacion.getCantidad(), presentacion.getUnidadDeMedida().getSimbolo()) + ")";
    }

    /**
     * El tipo del insumo para mostrar: Malta, Lúpulo o Levadura.
     */
    public static String tipoDeInsumo(InsumoEntity insumo) {
        Object real = Hibernate.unproxy(insumo);
        if (real instanceof MaltaEntity) {
            return "Malta";
        }
        return real instanceof LupuloEntity ? "Lúpulo" : "Levadura";
    }

    /**
     * La versión vigente del proveedor, o {@code null} si no tiene una.
     */
    public static VersionProveedorEntity versionVigente(ProveedorEntity proveedor) {
        return MetodosVersionado.buscarVersionActiva(proveedor.getVersiones()).orElse(null);
    }

    /**
     * Una oferta de la respuesta de un proveedor junto con el ítem de su catálogo vigente que tiene el mismo insumo y la
     * misma presentación comercial que el ítem al que apunta la oferta.
     *
     * @param oferta La oferta registrada.
     * @param itemVigente El ítem equivalente del catálogo vigente del proveedor.
     */
    public record OfertaVigente(OfertaCotizacionEntity oferta, CatalogoProveedorEntity itemVigente) {
    }

    /**
     * Las ofertas de la invitación cuya presentación sigue en el catálogo vigente del proveedor, cada una con su ítem
     * vigente, en el orden de la invitación. Una oferta se identifica por su insumo y su presentación comercial, no por
     * el ítem del catálogo: al modificar el proveedor se crea una versión con ítems nuevos, y la respuesta sigue valiendo
     * para cada presentación que el catálogo vigente conserva. Las ofertas de presentaciones que salieron del catálogo
     * no se devuelven.
     *
     * @param invitacion La invitación con sus ofertas y su proveedor.
     * @return Las ofertas vigentes; vacía si el proveedor no tiene una versión vigente.
     */
    public static List<OfertaVigente> ofertasVigentes(InvitacionCotizacionEntity invitacion) {
        VersionProveedorEntity version = versionVigente(invitacion.getProveedor());
        List<OfertaVigente> vigentes = new ArrayList<>();
        for (OfertaCotizacionEntity oferta : invitacion.getOfertas()) {
            itemVigente(version, oferta.getCatalogoProveedor()).ifPresent(item -> vigentes.add(new OfertaVigente(oferta, item)));
        }
        return vigentes;
    }

    /**
     * Las ofertas de la respuesta que se cuentan y se muestran al gerente de compras. Mientras la solicitud está abierta
     * y el proveedor sigue activo con una versión vigente, rige su catálogo vigente ({@link #ofertasVigentes}). En los
     * demás casos las ofertas se muestran tal como quedaron guardadas: al cerrar, cada oferta se apuntó al ítem del
     * catálogo vigente y se quitaron las de presentaciones que habían salido del catálogo, así una modificación posterior
     * del proveedor no cambia lo que se respondió; las de un proveedor dado de baja se conservan sin cambios.
     *
     * @param invitacion La invitación con su solicitud, sus ofertas y su proveedor.
     * @return Las ofertas de la respuesta, cada una con el ítem de catálogo con el que se muestra.
     */
    static List<OfertaVigente> ofertasDeLaRespuesta(InvitacionCotizacionEntity invitacion) {
        if (invitacion.getSolicitud() != null && invitacion.getSolicitud().getEstado() == EstadoSolicitudCotizacion.ABIERTA
                && rigeCatalogoVigente(invitacion)) {
            return ofertasVigentes(invitacion);
        }
        return invitacion.getOfertas().stream().map(oferta -> new OfertaVigente(oferta, oferta.getCatalogoProveedor())).toList();
    }

    /**
     * Si la respuesta del proveedor se resuelve contra su catálogo vigente: el proveedor está activo y tiene una versión
     * vigente. Un proveedor dado de baja no tiene versión vigente; sus ofertas se conservan tal como quedaron guardadas
     * (no entran en el plan porque el proveedor no está activo).
     *
     * @param invitacion La invitación con su proveedor.
     * @return {@code true} si rige el catálogo vigente del proveedor.
     */
    public static boolean rigeCatalogoVigente(InvitacionCotizacionEntity invitacion) {
        return invitacion.getProveedor().getEstado() == Estado.ACTIVO && versionVigente(invitacion.getProveedor()) != null;
    }

    /**
     * El ítem del catálogo de la versión indicada con el mismo insumo y la misma presentación comercial que el ítem dado.
     *
     * @param version Versión del proveedor (normalmente la vigente); puede ser nula.
     * @param item Ítem de catálogo de cualquier versión del proveedor.
     * @return El ítem equivalente, o vacío si la versión es nula o ya no ofrece esa presentación del insumo.
     */
    public static Optional<CatalogoProveedorEntity> itemVigente(VersionProveedorEntity version, CatalogoProveedorEntity item) {
        if (version == null) {
            return Optional.empty();
        }
        return version.getCatalogoProveedor().stream()
                .filter(candidato -> candidato.getInsumo().getId().equals(item.getInsumo().getId())
                        && candidato.getPresentacionComercial().getId().equals(item.getPresentacionComercial().getId()))
                .findFirst();
    }
}
