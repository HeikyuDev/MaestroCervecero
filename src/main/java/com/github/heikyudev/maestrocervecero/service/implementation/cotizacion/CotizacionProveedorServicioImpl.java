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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementación de {@link ICotizacionProveedorServicio}.
 * <p>
 * La respuesta bloquea la solicitud antes de volver a validar el enlace: no se cruza con el cierre programado, de modo
 * que una respuesta nunca se registra en una solicitud que ya cerró. Las ofertas se actualizan en su lugar (no se borran
 * y se vuelven a crear) para respetar la unicidad de una oferta por insumo y proveedor.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CotizacionProveedorServicioImpl implements ICotizacionProveedorServicio {

    static final String MENSAJE_ENLACE_NO_VALIDO = "El enlace de la solicitud de cotización no es válido";
    static final String MENSAJE_SOLICITUD_CANCELADA = "La solicitud de cotización fue cancelada: ya no se reciben respuestas";
    static final String MENSAJE_SOLICITUD_CERRADA = "La solicitud de cotización ya cerró: el plazo para responder venció";
    static final String MENSAJE_PROVEEDOR_INACTIVO = "El enlace ya no está habilitado para responder la solicitud de cotización";

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
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.ORDEN_COMPRA_MAXIMO_ITEMS) + " insumos");
        }

        // 3. Validar cada fila contra los insumos que el proveedor ofrece y su catálogo vigente
        VersionProveedorEntity version = MapperSolicitudCotizacion.versionVigente(invitacion.getProveedor());
        Map<Long, LineaSolicitudCotizacionEntity> lineasOfrecidas = lineasQueOfrece(solicitud, version).stream()
                .collect(Collectors.toMap(LineaSolicitudCotizacionEntity::getId, Function.identity()));
        Map<Long, OfertaCotizacionEntity> nuevas = new HashMap<>();
        Set<Long> lineasVistas = new HashSet<>();
        for (int i = 0; i < filas.size(); i++) {
            OfertaCotizacionFormDTO fila = filas.get(i);
            if (fila == null) {
                throw new ReglaNegocioException("Las filas de la cotización son obligatorias");
            }
            if (fila.getIdLinea() == null) {
                throw new ReglaNegocioException("El insumo de cada fila de la cotización es obligatorio");
            }
            LineaSolicitudCotizacionEntity linea = lineasOfrecidas.get(fila.getIdLinea());
            if (linea == null) {
                throw new ReglaNegocioException("Uno de los insumos de la cotización no forma parte de la solicitud o no figura en su catálogo");
            }
            if (!lineasVistas.add(linea.getId())) {
                throw new ReglaNegocioException("La cotización no puede repetir el mismo insumo");
            }
            if (fila.getIdCatalogoProveedor() == null && fila.getPrecioUnitario() == null && fila.getCantidadDisponible() == null) {
                continue;
            }
            CatalogoProveedorEntity catalogo = validarFila(i, fila, linea, version);
            nuevas.put(linea.getId(), OfertaCotizacionEntity.builder()
                    .invitacion(invitacion)
                    .lineaSolicitud(linea)
                    .catalogoProveedor(catalogo)
                    .precioUnitario(fila.getPrecioUnitario())
                    .cantidadDisponible(fila.getCantidadDisponible())
                    .build());
        }

        // 4. Reemplazar las ofertas: se actualizan las de los insumos que se vuelven a cotizar, se quitan las demás y se
        //    agregan las nuevas
        invitacion.getOfertas().removeIf(oferta -> !nuevas.containsKey(oferta.getLineaSolicitud().getId()));
        for (OfertaCotizacionEntity existente : invitacion.getOfertas()) {
            OfertaCotizacionEntity nueva = nuevas.remove(existente.getLineaSolicitud().getId());
            existente.setCatalogoProveedor(nueva.getCatalogoProveedor());
            existente.setPrecioUnitario(nueva.getPrecioUnitario());
            existente.setCantidadDisponible(nueva.getCantidadDisponible());
        }
        nuevas.values().stream()
                .sorted(Comparator.comparing(oferta -> oferta.getLineaSolicitud().getId()))
                .forEach(invitacion.getOfertas()::add);
        invitacion.setFechaRespuesta(ahora);

        return armarCotizacion(invitacionRepository.save(invitacion));
    }

    /**
     * Valida una fila completa (o a medio completar): la presentación, el precio unitario y la cantidad disponible. Cada
     * regla indica el campo de la fila que falló.
     *
     * @return El ítem del catálogo vigente elegido.
     */
    private static CatalogoProveedorEntity validarFila(int indice, OfertaCotizacionFormDTO fila, LineaSolicitudCotizacionEntity linea,
                                                       VersionProveedorEntity version) {
        String insumo = linea.getInsumo().getNombre();
        String prefijo = "ofertas[" + indice + "].";

        if (fila.getIdCatalogoProveedor() == null) {
            throw new ReglaNegocioException("Seleccione la presentación de " + insumo, prefijo + "idCatalogoProveedor");
        }
        CatalogoProveedorEntity catalogo = version.getCatalogoProveedor().stream()
                .filter(item -> item.getId().equals(fila.getIdCatalogoProveedor()) && item.getInsumo().getId().equals(linea.getInsumo().getId()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException("La presentación elegida para " + insumo + " no figura en su catálogo",
                        prefijo + "idCatalogoProveedor"));

        BigDecimal precio = fila.getPrecioUnitario();
        if (precio == null) {
            throw new ReglaNegocioException("El precio unitario de " + insumo + " es obligatorio", prefijo + "precioUnitario");
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

        Integer cantidad = fila.getCantidadDisponible();
        if (cantidad == null) {
            throw new ReglaNegocioException("La cantidad disponible de " + insumo + " es obligatoria", prefijo + "cantidadDisponible");
        }
        if (cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad disponible de " + insumo + " debe ser mayor a 0", prefijo + "cantidadDisponible");
        }
        if (cantidad > LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA) {
            throw new ReglaNegocioException("La cantidad disponible de " + insumo + " no puede superar "
                    + ValidacionesNumericas.formatearNumero(LimitesTecnicos.CANTIDAD_COMPRA_MAXIMA), prefijo + "cantidadDisponible");
        }
        return catalogo;
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

    /** Arma lo que ve el proveedor: los insumos que ofrece, con sus presentaciones y su respuesta actual. */
    private static CotizacionProveedorResponseDTO armarCotizacion(InvitacionCotizacionEntity invitacion) {
        SolicitudCotizacionEntity solicitud = invitacion.getSolicitud();
        VersionProveedorEntity version = MapperSolicitudCotizacion.versionVigente(invitacion.getProveedor());
        Map<Long, OfertaCotizacionEntity> ofertaPorLinea = invitacion.getOfertas().stream()
                .collect(Collectors.toMap(oferta -> oferta.getLineaSolicitud().getId(), Function.identity(), (a, b) -> a));

        List<LineaCotizacionProveedorResponseDTO> lineas = new ArrayList<>();
        for (LineaSolicitudCotizacionEntity linea : lineasQueOfrece(solicitud, version)) {
            List<OpcionPresentacionResponseDTO> presentaciones = version.getCatalogoProveedor().stream()
                    .filter(item -> item.getInsumo().getId().equals(linea.getInsumo().getId()))
                    .sorted(Comparator.comparing((CatalogoProveedorEntity item) -> item.getPresentacionComercial().getNombre(), String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(CatalogoProveedorEntity::getId))
                    .map(item -> OpcionPresentacionResponseDTO.builder()
                            .idCatalogoProveedor(item.getId())
                            .nombre(MapperSolicitudCotizacion.nombrePresentacion(item.getPresentacionComercial()))
                            .build())
                    .toList();
            OfertaCotizacionEntity oferta = ofertaPorLinea.get(linea.getId());
            lineas.add(LineaCotizacionProveedorResponseDTO.builder()
                    .idLinea(linea.getId())
                    .nombreInsumo(linea.getInsumo().getNombre())
                    .tipoInsumo(MapperSolicitudCotizacion.tipoDeInsumo(linea.getInsumo()))
                    .unidad(linea.getInsumo().getUnidadDeMedida().getSimbolo())
                    .cantidadSolicitada(linea.getCantidadFaltante())
                    .presentaciones(presentaciones)
                    .idCatalogoProveedorOfertado(oferta == null ? null : oferta.getCatalogoProveedor().getId())
                    .precioUnitarioOfertado(oferta == null ? null : oferta.getPrecioUnitario())
                    .cantidadDisponibleOfertada(oferta == null ? null : oferta.getCantidadDisponible())
                    .build());
        }
        return CotizacionProveedorResponseDTO.builder()
                .idSolicitud(solicitud.getId())
                .razonSocial(version == null ? null : version.getRazonSocial())
                .nombreComercial(version == null ? null : version.getNombreComercial())
                .fechaLimiteRespuesta(solicitud.getFechaLimiteRespuesta())
                .fechaRespuesta(invitacion.getFechaRespuesta())
                .lineas(lineas)
                .build();
    }
}
