package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.InvitacionCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.LineaSolicitudCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.OfertaCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.SolicitudCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.IInvitacionCotizacionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.ISolicitudCotizacionRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.CotizacionProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.OfertaCotizacionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceCotizacionInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.ICotizacionProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.CotizacionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.LineaCotizacionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.OpcionPresentacionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.cotizacion.MapperSolicitudCotizacion;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementación de {@link ICotizacionProveedorServicio}.
 * <p>
 * La respuesta bloquea la solicitud antes de volver a validar el enlace: no se cruza con el cierre programado, de modo
 * que una respuesta nunca se registra en una solicitud que ya cerró. Un proveedor puede ofrecer un mismo insumo en varias
 * presentaciones de su catálogo: cada presentación con precio y cantidad es una oferta, y una presentación con cantidad
 * 0 y sin precio es una oferta sin stock, que los planes no usan. Las ofertas se actualizan en su
 * lugar (no se borran y se vuelven a crear) para respetar la unicidad de una oferta por invitación, insumo y presentación.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CotizacionProveedorServicioImpl implements ICotizacionProveedorServicio {

    static final String MENSAJE_ENLACE_NO_VALIDO = "El enlace de la solicitud de cotización no es válido";
    static final String MENSAJE_SOLICITUD_CANCELADA = "La solicitud de cotización fue cancelada: ya no se reciben respuestas";
    static final String MENSAJE_SOLICITUD_CERRADA = "La solicitud de cotización ya cerró: el plazo para responder venció";
    static final String MENSAJE_PRECIO_SIN_STOCK = "No indique precio unitario si la cantidad disponible es 0";
    static final String MENSAJE_PRECIO_OBLIGATORIO = "El precio unitario es obligatorio si indica una cantidad disponible";
    static final String MENSAJE_CANTIDAD_OBLIGATORIA = "La cantidad disponible es obligatoria si indica un precio unitario";
    static final String MENSAJE_PROVEEDOR_INACTIVO = "El enlace ya no está habilitado para responder la solicitud de cotización";

    private static final String CAMPO_FECHA_ENTREGA = "fechaEntregaEstimada";

    private final IInvitacionCotizacionRepository invitacionRepository;
    private final ISolicitudCotizacionRepository solicitudRepository;

    @Override
    @Transactional(readOnly = true)
    public CotizacionProveedorResponseDTO buscarCotizacion(String token) {
        InvitacionCotizacionEntity invitacion = buscarInvitacion(token);
        validarVigente(invitacion, invitacion.getSolicitud(), LocalDateTime.now());
        return armarCotizacion(invitacion);
    }

    @Override
    @Transactional
    public CotizacionProveedorResponseDTO registrarCotizacion(String token, CotizacionProveedorFormDTO cotizacionProveedorFormDTO) {
        // 1. Bloquear la solicitud del enlace antes de cargarla (así se lee su estado vigente) y validar que el enlace sirva
        String tokenHash = hashDeTokenValido(token);
        Long idSolicitud = invitacionRepository.buscarIdSolicitudPorTokenHash(tokenHash)
                .orElseThrow(() -> new EnlaceCotizacionInvalidoException(MENSAJE_ENLACE_NO_VALIDO));
        SolicitudCotizacionEntity solicitud = solicitudRepository.buscarPorIdParaActualizar(idSolicitud)
                .orElseThrow(() -> new EnlaceCotizacionInvalidoException(MENSAJE_ENLACE_NO_VALIDO));
        InvitacionCotizacionEntity invitacion = invitacionRepository.buscarPorTokenHash(tokenHash)
                .orElseThrow(() -> new EnlaceCotizacionInvalidoException(MENSAJE_ENLACE_NO_VALIDO));
        LocalDateTime ahora = LocalDateTime.now();
        validarVigente(invitacion, solicitud, ahora);

        // 2. Validar los datos y la cantidad de filas
        if (cotizacionProveedorFormDTO == null) {
            throw new ReglaNegocioException("Los datos de la cotización son obligatorios");
        }
        List<OfertaCotizacionFormDTO> filas = cotizacionProveedorFormDTO.getOfertas() == null ? List.of() : cotizacionProveedorFormDTO.getOfertas();
        if (filas.size() > LimitesTecnicos.ORDEN_COMPRA_MAXIMO_ITEMS) {
            throw new ReglaNegocioException("La cotización no puede superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.ORDEN_COMPRA_MAXIMO_ITEMS) + " presentaciones");
        }

        // 3. Validar cada fila (un insumo solicitado en una presentación) contra los insumos que el proveedor ofrece y su
        //    catálogo vigente: el ítem del catálogo debe ser de ese proveedor, de su versión vigente y del mismo insumo
        VersionProveedorEntity version = MapperSolicitudCotizacion.versionVigente(invitacion.getProveedor());
        Map<Long, LineaSolicitudCotizacionEntity> lineasOfrecidas = lineasQueOfrece(solicitud, version).stream()
                .collect(Collectors.toMap(LineaSolicitudCotizacionEntity::getId, Function.identity()));
        Map<ClaveOferta, OfertaCotizacionEntity> nuevas = new LinkedHashMap<>();
        Set<ClaveOferta> filasVistas = new HashSet<>();
        for (int i = 0; i < filas.size(); i++) {
            OfertaCotizacionFormDTO fila = filas.get(i);
            if (fila == null) {
                throw new ReglaNegocioException("Las filas de la cotización son obligatorias");
            }
            // Una fila sin precio ni cantidad no responde esa presentación: se ignora antes de mirar su insumo y su
            // presentación, para que un dato viejo en una fila que el proveedor no tocó no bloquee la respuesta
            if (fila.getPrecioUnitario() == null && fila.getCantidadDisponible() == null) {
                continue;
            }
            if (fila.getIdLinea() == null) {
                throw new ReglaNegocioException("El insumo de cada fila de la cotización es obligatorio");
            }
            if (fila.getIdCatalogoProveedor() == null) {
                throw new ReglaNegocioException("La presentación de cada fila de la cotización es obligatoria");
            }
            LineaSolicitudCotizacionEntity linea = lineasOfrecidas.get(fila.getIdLinea());
            if (linea == null) {
                throw new ReglaNegocioException("Uno de los insumos de la cotización no forma parte de la solicitud o no figura en su catálogo");
            }
            CatalogoProveedorEntity catalogo = version.getCatalogoProveedor().stream()
                    .filter(item -> item.getId().equals(fila.getIdCatalogoProveedor()) && item.getInsumo().getId().equals(linea.getInsumo().getId()))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("Una de las presentaciones de " + linea.getInsumo().getNombre() + " no figura en su catálogo"));
            ClaveOferta clave = new ClaveOferta(linea.getId(), catalogo.getId());
            if (!filasVistas.add(clave)) {
                throw new ReglaNegocioException("La cotización no puede repetir la misma presentación de un insumo");
            }
            boolean sinStock = validarFila(i, fila, linea, catalogo);
            nuevas.put(clave, OfertaCotizacionEntity.builder()
                    .invitacion(invitacion)
                    .lineaSolicitud(linea)
                    .catalogoProveedor(catalogo)
                    // Sin stock no lleva precio (la validación ya rechazó un precio con cantidad 0)
                    .precioUnitario(sinStock ? null : fila.getPrecioUnitario())
                    .cantidadDisponible(fila.getCantidadDisponible())
                    .build());
        }

        // 4. Validar la fecha estimada de entrega: obligatoria si ofrece alguna presentación con stock (cantidad mayor a 0),
        //    no anterior a la fecha actual y a no más del máximo de una orden de compra (la usa la orden que se le propone)
        boolean ofreceConStock = nuevas.values().stream().anyMatch(oferta -> oferta.getCantidadDisponible() > 0);
        LocalDate fechaEntregaEstimada = ofreceConStock ? cotizacionProveedorFormDTO.getFechaEntregaEstimada() : null;
        if (ofreceConStock) {
            validarFechaEntregaEstimada(fechaEntregaEstimada);
        }

        // 5. Reemplazar las ofertas: se actualizan las de las presentaciones que se vuelven a cotizar, se quitan las demás
        //    y se agregan las nuevas
        invitacion.getOfertas().removeIf(oferta -> !nuevas.containsKey(ClaveOferta.de(oferta)));
        for (OfertaCotizacionEntity existente : invitacion.getOfertas()) {
            OfertaCotizacionEntity nueva = nuevas.remove(ClaveOferta.de(existente));
            existente.setPrecioUnitario(nueva.getPrecioUnitario());
            existente.setCantidadDisponible(nueva.getCantidadDisponible());
        }
        nuevas.values().stream()
                .sorted(Comparator.comparing((OfertaCotizacionEntity oferta) -> oferta.getLineaSolicitud().getId())
                        .thenComparing(oferta -> oferta.getCatalogoProveedor().getId()))
                .forEach(invitacion.getOfertas()::add);
        invitacion.setFechaRespuesta(ahora);
        invitacion.setFechaEntregaEstimada(fechaEntregaEstimada);

        return armarCotizacion(invitacionRepository.save(invitacion));
    }

    /**
     * Valida la fecha estimada de entrega con las mismas reglas que el alta de órdenes de compra: informada, no anterior
     * a la fecha actual y a no más de {@code LimitesTecnicos.ORDEN_COMPRA_MAXIMO_DIAS_ENTREGA} días.
     */
    private static void validarFechaEntregaEstimada(LocalDate fecha) {
        if (fecha == null) {
            throw new ReglaNegocioException("La fecha estimada de entrega es obligatoria", CAMPO_FECHA_ENTREGA);
        }
        LocalDate hoy = LocalDate.now();
        if (fecha.isBefore(hoy)) {
            throw new ReglaNegocioException("La fecha estimada de entrega no puede ser anterior a la fecha actual", CAMPO_FECHA_ENTREGA);
        }
        if (fecha.isAfter(hoy.plusDays(LimitesTecnicos.ORDEN_COMPRA_MAXIMO_DIAS_ENTREGA))) {
            throw new ReglaNegocioException("La fecha estimada de entrega no puede superar un año desde la fecha actual", CAMPO_FECHA_ENTREGA);
        }
    }

    /** Identidad de una oferta dentro de la respuesta de un proveedor: el insumo solicitado y la presentación. */
    private record ClaveOferta(Long idLinea, Long idCatalogoProveedor) {

        static ClaveOferta de(OfertaCotizacionEntity oferta) {
            return new ClaveOferta(oferta.getLineaSolicitud().getId(), oferta.getCatalogoProveedor().getId());
        }
    }

    /**
     * Valida el precio unitario y la cantidad disponible de una fila completa (o a medio completar): cantidad 0 sin
     * precio indica que el proveedor no tiene stock de esa presentación; cantidad 0 con precio, cantidad mayor a 0 sin
     * precio y precio sin cantidad se rechazan; precio y cantidad mayor a 0 son una oferta. Cada regla indica el campo
     * de la fila que falló.
     *
     * @return {@code true} si la fila indica que no tiene stock (cantidad 0).
     */
    private static boolean validarFila(int indice, OfertaCotizacionFormDTO fila, LineaSolicitudCotizacionEntity linea,
                                       CatalogoProveedorEntity catalogo) {
        String insumo = linea.getInsumo().getNombre() + " en " + MapperSolicitudCotizacion.nombrePresentacion(catalogo.getPresentacionComercial());
        String prefijo = "ofertas[" + indice + "].";

        Integer cantidad = fila.getCantidadDisponible();
        BigDecimal precio = fila.getPrecioUnitario();
        if (cantidad != null && cantidad < 0) {
            throw new ReglaNegocioException("La cantidad disponible de " + insumo + " no puede ser negativa", prefijo + "cantidadDisponible");
        }
        // Cantidad 0 sin precio: el proveedor indica que no tiene stock de la presentación
        if (cantidad != null && cantidad == 0) {
            if (precio != null) {
                throw new ReglaNegocioException(MENSAJE_PRECIO_SIN_STOCK, prefijo + "precioUnitario");
            }
            return true;
        }
        if (precio == null) {
            throw new ReglaNegocioException(MENSAJE_PRECIO_OBLIGATORIO, prefijo + "precioUnitario");
        }
        if (cantidad == null) {
            throw new ReglaNegocioException(MENSAJE_CANTIDAD_OBLIGATORIA, prefijo + "cantidadDisponible");
        }
        if (precio.signum() <= 0) {
            throw new ReglaNegocioException("El precio unitario de " + insumo + " debe ser mayor a 0", prefijo + "precioUnitario");
        }
        if (precio.compareTo(BigDecimal.valueOf(LimitesTecnicos.COSTO_UNITARIO_COMPRA_MAXIMO)) > 0) {
            throw new ReglaNegocioException("El precio unitario de " + insumo + " no puede superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.COSTO_UNITARIO_COMPRA_MAXIMO), prefijo + "precioUnitario");
        }
        if (precio.stripTrailingZeros().scale() > LimitesTecnicos.ESCALA_COSTO_UNITARIO) {
            throw new ReglaNegocioException("El precio unitario de " + insumo + " no puede tener más de "
                    + LimitesTecnicos.ESCALA_COSTO_UNITARIO + " decimales", prefijo + "precioUnitario");
        }

        if (cantidad > LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA) {
            throw new ReglaNegocioException("La cantidad disponible de " + insumo + " no puede superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA), prefijo + "cantidadDisponible");
        }
        return false;
    }

    /**
     * Busca la invitación del token.
     *
     * @throws EnlaceCotizacionInvalidoException Si el token falta, es demasiado largo o no corresponde a ningún enlace.
     */
    private InvitacionCotizacionEntity buscarInvitacion(String token) {
        return invitacionRepository.buscarPorTokenHash(hashDeTokenValido(token))
                .orElseThrow(() -> new EnlaceCotizacionInvalidoException(MENSAJE_ENLACE_NO_VALIDO));
    }

    /**
     * El hash del token, si tiene un formato posible.
     *
     * @throws EnlaceCotizacionInvalidoException Si el token falta o es demasiado largo.
     */
    private static String hashDeTokenValido(String token) {
        if (token == null || token.isBlank() || token.length() > TokenCotizacion.LONGITUD_MAXIMA_TOKEN) {
            throw new EnlaceCotizacionInvalidoException(MENSAJE_ENLACE_NO_VALIDO);
        }
        return TokenCotizacion.calcularHash(token);
    }

    /**
     * Verifica que el enlace sirva en el momento indicado: la solicitud está abierta, no venció su plazo ni el del
     * enlace, y el proveedor sigue activo.
     */
    private static void validarVigente(InvitacionCotizacionEntity invitacion, SolicitudCotizacionEntity solicitud, LocalDateTime ahora) {
        if (solicitud.getEstado() == EstadoSolicitudCotizacion.CANCELADA) {
            throw new EnlaceCotizacionInvalidoException(MENSAJE_SOLICITUD_CANCELADA);
        }
        if (solicitud.getEstado() != EstadoSolicitudCotizacion.ABIERTA
                || solicitud.getFechaLimiteRespuesta() == null || !solicitud.getFechaLimiteRespuesta().isAfter(ahora)
                || invitacion.getFechaExpiracion() == null || !invitacion.getFechaExpiracion().isAfter(ahora)) {
            throw new EnlaceCotizacionInvalidoException(MENSAJE_SOLICITUD_CERRADA);
        }
        if (invitacion.getProveedor().getEstado() != Estado.ACTIVO) {
            throw new EnlaceCotizacionInvalidoException(MENSAJE_PROVEEDOR_INACTIVO);
        }
    }

    /** Las líneas de la solicitud cuyo insumo el proveedor ofrece en su catálogo vigente. */
    private static List<LineaSolicitudCotizacionEntity> lineasQueOfrece(SolicitudCotizacionEntity solicitud, VersionProveedorEntity version) {
        if (version == null) {
            return List.of();
        }
        Set<Long> insumosDelCatalogo = version.getCatalogoProveedor().stream().map(item -> item.getInsumo().getId()).collect(Collectors.toSet());
        return solicitud.getLineas().stream().filter(linea -> insumosDelCatalogo.contains(linea.getInsumo().getId())).toList();
    }

    /**
     * Arma lo que ve el proveedor: los insumos que ofrece, cada uno con las presentaciones de su catálogo vigente y, en
     * cada presentación, su respuesta actual.
     */
    private static CotizacionProveedorResponseDTO armarCotizacion(InvitacionCotizacionEntity invitacion) {
        SolicitudCotizacionEntity solicitud = invitacion.getSolicitud();
        VersionProveedorEntity version = MapperSolicitudCotizacion.versionVigente(invitacion.getProveedor());
        Map<ClaveOferta, OfertaCotizacionEntity> ofertas = invitacion.getOfertas().stream()
                .collect(Collectors.toMap(ClaveOferta::de, Function.identity(), (primera, segunda) -> primera));

        List<LineaCotizacionProveedorResponseDTO> lineas = new ArrayList<>();
        for (LineaSolicitudCotizacionEntity linea : lineasQueOfrece(solicitud, version)) {
            List<OpcionPresentacionResponseDTO> presentaciones = version.getCatalogoProveedor().stream()
                    .filter(item -> item.getInsumo().getId().equals(linea.getInsumo().getId()))
                    .sorted(Comparator.comparing((CatalogoProveedorEntity item) -> item.getPresentacionComercial().getNombre(), String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(CatalogoProveedorEntity::getId))
                    .map(item -> {
                        OfertaCotizacionEntity oferta = ofertas.get(new ClaveOferta(linea.getId(), item.getId()));
                        return OpcionPresentacionResponseDTO.builder()
                                .idCatalogoProveedor(item.getId())
                                .nombre(MapperSolicitudCotizacion.nombrePresentacion(item.getPresentacionComercial()))
                                .precioUnitarioOfertado(oferta == null ? null : oferta.getPrecioUnitario())
                                .cantidadDisponibleOfertada(oferta == null ? null : oferta.getCantidadDisponible())
                                .build();
                    })
                    .toList();
            lineas.add(LineaCotizacionProveedorResponseDTO.builder()
                    .idLinea(linea.getId())
                    .nombreInsumo(linea.getInsumo().getNombre())
                    .tipoInsumo(MapperSolicitudCotizacion.tipoDeInsumo(linea.getInsumo()))
                    .unidad(linea.getInsumo().getUnidadDeMedida().getSimbolo())
                    .cantidadSolicitada(linea.getCantidadFaltante())
                    .presentaciones(presentaciones)
                    .build());
        }
        return CotizacionProveedorResponseDTO.builder()
                .idSolicitud(solicitud.getId())
                .razonSocial(version == null ? null : version.getRazonSocial())
                .nombreComercial(version == null ? null : version.getNombreComercial())
                .fechaLimiteRespuesta(solicitud.getFechaLimiteRespuesta())
                .fechaRespuesta(invitacion.getFechaRespuesta())
                .fechaEntregaEstimada(invitacion.getFechaEntregaEstimada())
                .lineas(lineas)
                .build();
    }
}
