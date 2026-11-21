package com.github.heikyudev.maestrocervecero.service.implementation.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.InvitacionCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.LineaSolicitudCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.OfertaCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.SolicitudCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.IInvitacionCotizacionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cotizacion.ISolicitudCotizacionRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.CotizacionProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.OfertaCotizacionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceCotizacionInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.CotizacionProveedorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CotizacionProveedorServicioImplTest {

    private static final String TOKEN = "kV2s9fJ1x0QmB7r3TzYwLp4cHn8aUe6dGqXo5iSjNkA";
    private static final String HASH = TokenCotizacion.calcularHash(TOKEN);

    @Mock private IInvitacionCotizacionRepository invitacionRepository;
    @Mock private ISolicitudCotizacionRepository solicitudRepository;

    @InjectMocks
    private CotizacionProveedorServicioImpl servicio;

    private SolicitudCotizacionEntity solicitud;
    private ProveedorEntity proveedor;
    private InvitacionCotizacionEntity invitacion;
    private LineaSolicitudCotizacionEntity lineaMalta;
    private LineaSolicitudCotizacionEntity lineaLupulo;
    private LineaSolicitudCotizacionEntity lineaLevadura;
    private CatalogoProveedorEntity maltaBolsa25;
    private CatalogoProveedorEntity maltaBolsa5;
    private CatalogoProveedorEntity lupuloPaquete;

    @BeforeEach
    void preparar() {
        MaltaEntity malta = MaltaEntity.builder().id(1L).nombre("Malta Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
        LupuloEntity lupulo = LupuloEntity.builder().id(2L).nombre("Cascade").unidadDeMedida(UnidadDeMedida.GRAMO).build();
        LevaduraEntity levadura = LevaduraEntity.builder().id(3L).nombre("US-05").unidadDeMedida(UnidadDeMedida.GRAMO).build();
        solicitud = SolicitudCotizacionEntity.builder().id(90L).estado(EstadoSolicitudCotizacion.ABIERTA)
                .fechaLimiteRespuesta(LocalDateTime.now().plusHours(5))
                .planificacionProduccion(PlanificacionProduccionEntity.builder().id(7L).versionReceta(VersionRecetaEntity.builder().nombre("Pale Ale").build()).build())
                .build();
        lineaMalta = linea(501L, malta, 100.0);
        lineaLupulo = linea(502L, lupulo, 500.0);
        lineaLevadura = linea(503L, levadura, 50.0);
        solicitud.getLineas().addAll(List.of(lineaMalta, lineaLupulo, lineaLevadura));

        proveedor = ProveedorEntity.builder().id(1L).estado(Estado.ACTIVO).build();
        VersionProveedorEntity version = VersionProveedorEntity.builder().id(10L).razonSocial("MALTERIA SUR").nombreComercial("Maltería Sur")
                .cuit("30123456789").esUltimaVersion(true).proveedor(proveedor).build();
        maltaBolsa25 = catalogo(100L, version, malta, presentacion(30L, "Bolsa", 25.0, UnidadDeMedida.KILOGRAMO));
        maltaBolsa5 = catalogo(101L, version, malta, presentacion(31L, "Balde", 5.0, UnidadDeMedida.KILOGRAMO));
        lupuloPaquete = catalogo(102L, version, lupulo, presentacion(32L, "Paquete", 1.0, UnidadDeMedida.KILOGRAMO));
        version.getCatalogoProveedor().addAll(List.of(maltaBolsa25, maltaBolsa5, lupuloPaquete));
        proveedor.getVersiones().add(version);

        invitacion = InvitacionCotizacionEntity.builder().id(600L).solicitud(solicitud).proveedor(proveedor).tokenHash(HASH)
                .fechaExpiracion(solicitud.getFechaLimiteRespuesta()).build();
        solicitud.getInvitaciones().add(invitacion);

        lenient().when(invitacionRepository.buscarPorTokenHash(HASH)).thenReturn(Optional.of(invitacion));
        lenient().when(invitacionRepository.buscarIdSolicitudPorTokenHash(HASH)).thenReturn(Optional.of(90L));
        lenient().when(solicitudRepository.buscarPorIdParaActualizar(90L)).thenReturn(Optional.of(solicitud));
        lenient().when(invitacionRepository.save(any(InvitacionCotizacionEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private LineaSolicitudCotizacionEntity linea(long id, com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity insumo, double faltante) {
        return LineaSolicitudCotizacionEntity.builder().id(id).solicitud(solicitud).insumo(insumo).cantidadRequerida(faltante)
                .cantidadDisponible(0.0).cantidadPendienteRecepcion(0.0).cantidadFaltante(faltante).build();
    }

    private static PresentacionComercialEntity presentacion(long id, String nombre, double cantidad, UnidadDeMedida unidad) {
        return PresentacionComercialEntity.builder().id(id).nombre(nombre).cantidad(cantidad).unidadDeMedida(unidad).estado(Estado.ACTIVO).build();
    }

    private static CatalogoProveedorEntity catalogo(long id, VersionProveedorEntity version, com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity insumo, PresentacionComercialEntity presentacion) {
        return CatalogoProveedorEntity.builder().id(id).version(version).insumo(insumo).presentacionComercial(presentacion).build();
    }

    private static OfertaCotizacionFormDTO fila(Long idLinea, Long idCatalogo, String precio, Integer cantidad) {
        return OfertaCotizacionFormDTO.builder().idLinea(idLinea).idCatalogoProveedor(idCatalogo)
                .precioUnitario(precio == null ? null : new BigDecimal(precio)).cantidadDisponible(cantidad).build();
    }

    private static CotizacionProveedorFormDTO form(OfertaCotizacionFormDTO... filas) {
        return CotizacionProveedorFormDTO.builder().ofertas(new ArrayList<>(Arrays.asList(filas))).build();
    }

    private void verificarRegla(CotizacionProveedorFormDTO formDTO, String mensaje, String campo) {
        assertThatThrownBy(() -> servicio.registrarCotizacion(TOKEN, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(mensaje)
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo(campo));
        verify(invitacionRepository, never()).save(any());
    }

    // ==================== Validación del enlace ====================

    @Test
    @DisplayName("CP-CPV-01: un token nulo, vacío, demasiado largo o desconocido no es un enlace válido")
    void enlace_tokenInvalido() {
        when(invitacionRepository.buscarPorTokenHash(TokenCotizacion.calcularHash("desconocido"))).thenReturn(Optional.empty());

        for (String token : Arrays.asList(null, "", "   ", "x".repeat(129), "desconocido")) {
            assertThatThrownBy(() -> servicio.buscarCotizacion(token))
                    .isInstanceOf(EnlaceCotizacionInvalidoException.class)
                    .hasMessage("El enlace de la solicitud de cotización no es válido");
        }
    }

    @Test
    @DisplayName("CP-CPV-02: el enlace de una solicitud cancelada no sirve")
    void enlace_solicitudCancelada() {
        solicitud.setEstado(EstadoSolicitudCotizacion.CANCELADA);

        assertThatThrownBy(() -> servicio.buscarCotizacion(TOKEN))
                .isInstanceOf(EnlaceCotizacionInvalidoException.class)
                .hasMessage("La solicitud de cotización fue cancelada: ya no se reciben respuestas");
    }

    @Test
    @DisplayName("CP-CPV-03: el enlace no sirve si la solicitud ya cerró, si venció su plazo o si venció el enlace")
    void enlace_solicitudCerradaOVencida() {
        String mensaje = "La solicitud de cotización ya cerró: el plazo para responder venció";

        solicitud.setEstado(EstadoSolicitudCotizacion.PLAN_GENERADO);
        assertThatThrownBy(() -> servicio.buscarCotizacion(TOKEN)).isInstanceOf(EnlaceCotizacionInvalidoException.class).hasMessage(mensaje);

        solicitud.setEstado(EstadoSolicitudCotizacion.ABIERTA);
        solicitud.setFechaLimiteRespuesta(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> servicio.buscarCotizacion(TOKEN)).isInstanceOf(EnlaceCotizacionInvalidoException.class).hasMessage(mensaje);

        solicitud.setFechaLimiteRespuesta(LocalDateTime.now().plusHours(5));
        invitacion.setFechaExpiracion(LocalDateTime.now().minusSeconds(1));
        assertThatThrownBy(() -> servicio.buscarCotizacion(TOKEN)).isInstanceOf(EnlaceCotizacionInvalidoException.class).hasMessage(mensaje);
    }

    @Test
    @DisplayName("CP-CPV-04: el enlace de un proveedor dado de baja no sirve")
    void enlace_proveedorDadoDeBaja() {
        proveedor.setEstado(Estado.BAJA);

        assertThatThrownBy(() -> servicio.buscarCotizacion(TOKEN))
                .isInstanceOf(EnlaceCotizacionInvalidoException.class)
                .hasMessage("El enlace ya no está habilitado para responder la solicitud de cotización");
    }

    // ==================== buscarCotizacion ====================

    @Test
    @DisplayName("CP-CPV-05: el proveedor ve solo los insumos solicitados que ofrece, con una fila por presentación con su respuesta actual")
    void buscar_muestraSoloLoQueOfrece() {
        // === PREPARACION DE DATOS ===
        invitacion.setFechaRespuesta(LocalDateTime.now().minusHours(1));
        invitacion.getOfertas().add(OfertaCotizacionEntity.builder().id(1L).invitacion(invitacion).lineaSolicitud(lineaMalta)
                .catalogoProveedor(maltaBolsa25).precioUnitario(new BigDecimal("32500")).cantidadDisponible(4).build());

        // === EJECUCION ===
        CotizacionProveedorResponseDTO cotizacion = servicio.buscarCotizacion(TOKEN);

        // === ASSERTS ===
        assertThat(cotizacion.getRazonSocial()).isEqualTo("MALTERIA SUR");
        assertThat(cotizacion.getFechaLimiteRespuesta()).isEqualTo(solicitud.getFechaLimiteRespuesta());
        assertThat(cotizacion.getFechaRespuesta()).isEqualTo(invitacion.getFechaRespuesta());
        // La levadura no figura en su catálogo: no se le muestra
        assertThat(cotizacion.getLineas()).extracting(l -> l.getNombreInsumo()).containsExactly("Malta Pilsen", "Cascade");
        assertThat(cotizacion.getLineas().get(0).getPresentaciones()).extracting(p -> p.getNombre())
                .containsExactly("Balde (5 kg)", "Bolsa (25 kg)");
        assertThat(cotizacion.getLineas().get(0).getCantidadSolicitada()).isEqualTo(100.0);
        assertThat(cotizacion.getLineas().get(0).getUnidad()).isEqualTo("kg");
        // La respuesta actual queda en su presentación; el Balde no se cotizó
        assertThat(cotizacion.getLineas().get(0).getPresentaciones().get(1).getIdCatalogoProveedor()).isEqualTo(100L);
        assertThat(cotizacion.getLineas().get(0).getPresentaciones().get(1).getPrecioUnitarioOfertado()).isEqualByComparingTo("32500");
        assertThat(cotizacion.getLineas().get(0).getPresentaciones().get(1).getCantidadDisponibleOfertada()).isEqualTo(4);
        assertThat(cotizacion.getLineas().get(0).getPresentaciones().get(0).getPrecioUnitarioOfertado()).isNull();
        assertThat(cotizacion.getLineas().get(1).getPresentaciones()).singleElement()
                .satisfies(p -> assertThat(p.getCantidadDisponibleOfertada()).isNull());
    }

    // ==================== registrarCotizacion ====================

    @Test
    @DisplayName("CP-CPV-06: registrarCotizacion rechaza los datos nulos, el exceso de filas y las filas que no corresponden")
    void registrar_rechazaFilasInvalidas() {
        verificarRegla(null, "Los datos de la cotización son obligatorios", null);
        verificarRegla(CotizacionProveedorFormDTO.builder().ofertas(new ArrayList<>(Collections.nCopies(101, fila(501L, 100L, null, null)))).build(),
                "La cotización no puede superar 100 presentaciones", null);
        verificarRegla(CotizacionProveedorFormDTO.builder().ofertas(new ArrayList<>(Collections.singletonList(null))).build(),
                "Las filas de la cotización son obligatorias", null);
        verificarRegla(form(fila(null, 100L, "1", 1)), "El insumo de cada fila de la cotización es obligatorio", null);
        verificarRegla(form(fila(501L, null, "1", 1)), "La presentación de cada fila de la cotización es obligatoria", null);
        // La levadura es de la solicitud pero el proveedor no la ofrece; la línea 999 no es de la solicitud
        verificarRegla(form(fila(503L, 100L, "1", 1)), "Uno de los insumos de la cotización no forma parte de la solicitud o no figura en su catálogo", null);
        verificarRegla(form(fila(999L, 100L, "1", 1)), "Uno de los insumos de la cotización no forma parte de la solicitud o no figura en su catálogo", null);
        // La presentación del lúpulo no es de la malta, y la 777 no es de su catálogo vigente
        verificarRegla(form(fila(501L, 102L, "1", 1)), "Una de las presentaciones de Malta Pilsen no figura en su catálogo", null);
        verificarRegla(form(fila(501L, 777L, "1", 1)), "Una de las presentaciones de Malta Pilsen no figura en su catálogo", null);
    }

    @Test
    @DisplayName("CP-CPV-07: una fila con precio y cantidad exige valores válidos y la cantidad no puede ser negativa, indicando el campo y la presentación")
    void registrar_validaCadaCampoDeLaFila() {
        String malta = "Malta Pilsen en Bolsa (25 kg)";
        verificarRegla(form(fila(501L, 100L, "0", 2)), "El precio unitario de " + malta + " debe ser mayor a 0", "ofertas[0].precioUnitario");
        verificarRegla(form(fila(501L, 100L, "1000000000.01", 2)), "El precio unitario de " + malta + " no puede superar 1.000.000.000", "ofertas[0].precioUnitario");
        verificarRegla(form(fila(501L, 100L, "10.12345", 2)), "El precio unitario de " + malta + " no puede tener más de 4 decimales", "ofertas[0].precioUnitario");
        verificarRegla(form(fila(501L, 100L, null, -1)), "La cantidad disponible de " + malta + " no puede ser negativa", "ofertas[0].cantidadDisponible");
        verificarRegla(form(fila(501L, 100L, "10", 1_000_001)), "La cantidad disponible de " + malta + " no puede superar 1.000.000", "ofertas[0].cantidadDisponible");
    }

    @Test
    @DisplayName("CP-CPV-08: registrarCotizacion guarda una oferta por fila completa, ignora las vacías y registra la fecha de respuesta, con la solicitud bloqueada")
    void registrar_guardaOfertas() {
        // === EJECUCION ===
        CotizacionProveedorResponseDTO resultado = servicio.registrarCotizacion(TOKEN,
                form(fila(501L, 101L, null, null), fila(501L, 100L, "32500.5", 4), fila(502L, 102L, null, null)));

        // === ASSERTS ===
        assertThat(invitacion.getOfertas()).singleElement().satisfies(oferta -> {
            assertThat(oferta.getLineaSolicitud()).isSameAs(lineaMalta);
            assertThat(oferta.getCatalogoProveedor()).isSameAs(maltaBolsa25);
            assertThat(oferta.getPrecioUnitario()).isEqualByComparingTo("32500.5");
            assertThat(oferta.getCantidadDisponible()).isEqualTo(4);
            assertThat(oferta.getInvitacion()).isSameAs(invitacion);
        });
        assertThat(invitacion.getFechaRespuesta()).isNotNull();
        assertThat(resultado.getLineas().get(0).getPresentaciones().get(1).getCantidadDisponibleOfertada()).isEqualTo(4);
        InOrder orden = inOrder(invitacionRepository, solicitudRepository);
        orden.verify(invitacionRepository).buscarIdSolicitudPorTokenHash(HASH);
        orden.verify(solicitudRepository).buscarPorIdParaActualizar(90L);
        orden.verify(invitacionRepository).buscarPorTokenHash(HASH);
        orden.verify(invitacionRepository).save(invitacion);
    }

    @Test
    @DisplayName("CP-CPV-09: un nuevo envío actualiza en su lugar las ofertas de las presentaciones que se vuelven a cotizar, quita las vaciadas y agrega las nuevas")
    void registrar_reemplazaRespuestaAnterior() {
        // === PREPARACION DE DATOS ===
        OfertaCotizacionEntity anteriorMalta = OfertaCotizacionEntity.builder().id(1L).invitacion(invitacion).lineaSolicitud(lineaMalta)
                .catalogoProveedor(maltaBolsa25).precioUnitario(new BigDecimal("30000")).cantidadDisponible(2).build();
        OfertaCotizacionEntity anteriorLupulo = OfertaCotizacionEntity.builder().id(2L).invitacion(invitacion).lineaSolicitud(lineaLupulo)
                .catalogoProveedor(lupuloPaquete).precioUnitario(new BigDecimal("9000")).cantidadDisponible(1).build();
        invitacion.getOfertas().addAll(List.of(anteriorMalta, anteriorLupulo));

        // === EJECUCION ===
        servicio.registrarCotizacion(TOKEN, form(fila(501L, 100L, "31000", 3), fila(501L, 101L, "7000", 20), fila(502L, 102L, null, null)));

        // === ASSERTS ===
        assertThat(invitacion.getOfertas()).hasSize(2);
        assertThat(invitacion.getOfertas().get(0)).isSameAs(anteriorMalta);
        assertThat(anteriorMalta.getCatalogoProveedor()).isSameAs(maltaBolsa25);
        assertThat(anteriorMalta.getPrecioUnitario()).isEqualByComparingTo("31000");
        assertThat(anteriorMalta.getCantidadDisponible()).isEqualTo(3);
        assertThat(invitacion.getOfertas().get(1)).satisfies(nueva -> {
            assertThat(nueva.getId()).isNull();
            assertThat(nueva.getCatalogoProveedor()).isSameAs(maltaBolsa5);
            assertThat(nueva.getPrecioUnitario()).isEqualByComparingTo("7000");
            assertThat(nueva.getCantidadDisponible()).isEqualTo(20);
        });
    }

    @Test
    @DisplayName("CP-CPV-10: registrarCotizacion vuelve a validar el enlace con la solicitud bloqueada y no guarda si ya cerró")
    void registrar_revalidaConLaSolicitudBloqueada() {
        // === PREPARACION DE DATOS ===
        solicitud.setEstado(EstadoSolicitudCotizacion.CERRADA);

        // === EJECUCION Y VERIFICACION ===
        assertThatThrownBy(() -> servicio.registrarCotizacion(TOKEN, form(fila(501L, 100L, "10", 1))))
                .isInstanceOf(EnlaceCotizacionInvalidoException.class)
                .hasMessage("La solicitud de cotización ya cerró: el plazo para responder venció");
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-CPV-11: registrarCotizacion rechaza un token desconocido sin bloquear ninguna solicitud")
    void registrar_tokenDesconocido() {
        when(invitacionRepository.buscarIdSolicitudPorTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.registrarCotizacion("otro", form()))
                .isInstanceOf(EnlaceCotizacionInvalidoException.class)
                .hasMessage("El enlace de la solicitud de cotización no es válido");
        verify(solicitudRepository, never()).buscarPorIdParaActualizar(any());
    }

    @Test
    @DisplayName("CP-CPV-12: un insumo se puede cotizar en varias presentaciones a la vez, una oferta por presentación")
    void registrar_variasPresentacionesDelMismoInsumo() {
        // === EJECUCION ===
        servicio.registrarCotizacion(TOKEN, form(fila(501L, 101L, "6000", 3), fila(501L, 100L, "28000", 2)));

        // === ASSERTS ===
        assertThat(invitacion.getOfertas()).extracting(oferta -> oferta.getCatalogoProveedor().getId(), OfertaCotizacionEntity::getCantidadDisponible)
                .containsExactly(tuple(100L, 2), tuple(101L, 3));
        assertThat(invitacion.getOfertas()).allSatisfy(oferta -> assertThat(oferta.getLineaSolicitud()).isSameAs(lineaMalta));
    }

    @Test
    @DisplayName("CP-CPV-13: la cotización no puede repetir la misma presentación de un insumo")
    void registrar_presentacionRepetida() {
        verificarRegla(form(fila(501L, 100L, "28000", 2), fila(501L, 100L, null, 0)),
                "La cotización no puede repetir la misma presentación de un insumo", null);
    }

    @Test
    @DisplayName("CP-CPV-15: una respuesta sin ofertas quita las ofertas anteriores y registra la fecha de respuesta")
    void registrar_sinOfertasQuitaLasAnteriores() {
        // === PREPARACION DE DATOS ===
        invitacion.getOfertas().add(OfertaCotizacionEntity.builder().id(1L).invitacion(invitacion).lineaSolicitud(lineaMalta)
                .catalogoProveedor(maltaBolsa25).precioUnitario(new BigDecimal("30000")).cantidadDisponible(2).build());

        // === EJECUCION ===
        servicio.registrarCotizacion(TOKEN, form(fila(501L, 100L, null, null), fila(501L, 101L, null, null)));

        // === ASSERTS ===
        assertThat(invitacion.getOfertas()).isEmpty();
        assertThat(invitacion.getFechaRespuesta()).isNotNull();
    }

    // ==================== Combinaciones de precio y cantidad de una fila ====================

    @Test
    @DisplayName("CP-CPV-16: una fila sin precio ni cantidad no responde esa presentación y se ignora")
    void fila_sinPrecioNiCantidad_seIgnora() {
        servicio.registrarCotizacion(TOKEN, form(fila(501L, 100L, null, null), fila(501L, 101L, "6000", 3)));

        assertThat(invitacion.getOfertas()).singleElement().satisfies(oferta -> assertThat(oferta.getCatalogoProveedor()).isSameAs(maltaBolsa5));
    }

    @Test
    @DisplayName("CP-CPV-17: una fila sin precio con cantidad 0 se guarda como sin stock")
    void fila_sinPrecioConCantidadCero_esSinStock() {
        servicio.registrarCotizacion(TOKEN, form(fila(501L, 100L, null, 0)));

        assertThat(invitacion.getOfertas()).singleElement().satisfies(oferta -> {
            assertThat(oferta.getCatalogoProveedor()).isSameAs(maltaBolsa25);
            assertThat(oferta.getCantidadDisponible()).isZero();
            assertThat(oferta.getPrecioUnitario()).isNull();
        });
    }

    @Test
    @DisplayName("CP-CPV-18: una fila con precio (cualquiera, incluso 0, negativo o fuera de rango) y cantidad 0 se rechaza en el precio de esa fila")
    void fila_conPrecioYCantidadCero_seRechaza() {
        verificarRegla(form(fila(501L, 101L, null, null), fila(501L, 100L, "25000", 0)),
                "No indique precio unitario si la cantidad disponible es 0", "ofertas[1].precioUnitario");
        // También con un precio 0, negativo o fuera de rango: el motivo es siempre el de la combinación
        for (String precio : List.of("0", "-5", "1000000000.01", "10.12345")) {
            verificarRegla(form(fila(501L, 100L, precio, 0)), "No indique precio unitario si la cantidad disponible es 0", "ofertas[0].precioUnitario");
        }
    }

    @Test
    @DisplayName("CP-CPV-19: una fila sin precio con cantidad mayor a 0 se rechaza en el precio de esa fila")
    void fila_sinPrecioConCantidad_seRechaza() {
        verificarRegla(form(fila(501L, 100L, null, 2)),
                "El precio unitario es obligatorio si indica una cantidad disponible", "ofertas[0].precioUnitario");
    }

    @Test
    @DisplayName("CP-CPV-20: una fila con precio y sin cantidad se rechaza en la cantidad de esa fila")
    void fila_conPrecioSinCantidad_seRechaza() {
        verificarRegla(form(fila(501L, 100L, "25000", null)),
                "La cantidad disponible es obligatoria si indica un precio unitario", "ofertas[0].cantidadDisponible");
    }

    @Test
    @DisplayName("CP-CPV-21: una fila con precio y cantidad mayor a 0 es una oferta, también junto a una sin stock")
    void fila_conPrecioYCantidad_esOferta() {
        servicio.registrarCotizacion(TOKEN, form(fila(501L, 100L, null, 0), fila(501L, 101L, "6000", 3)));

        assertThat(invitacion.getOfertas()).extracting(oferta -> oferta.getCatalogoProveedor().getId(), OfertaCotizacionEntity::getCantidadDisponible)
                .containsExactly(tuple(100L, 0), tuple(101L, 3));
        assertThat(invitacion.getOfertas().get(1).getPrecioUnitario()).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("CP-CPV-22: una fila sin precio ni cantidad se ignora aunque traiga un insumo o una presentación que ya no corresponden")
    void fila_vaciaConDatosViejos_noBloqueaLaRespuesta() {
        // Filas vacías con la línea 999 (no es de la solicitud), la presentación 777 (no es de su catálogo) y una repetida
        servicio.registrarCotizacion(TOKEN, form(fila(999L, 100L, null, null), fila(501L, 777L, null, null),
                fila(501L, 100L, "28000", 2), fila(501L, 100L, null, null)));

        assertThat(invitacion.getOfertas()).singleElement().satisfies(oferta -> {
            assertThat(oferta.getCatalogoProveedor()).isSameAs(maltaBolsa25);
            assertThat(oferta.getCantidadDisponible()).isEqualTo(2);
        });
    }

    // ==================== Catálogo modificado después de responder ====================

    /**
     * El gerente de compras modifica el proveedor: la versión 10 deja de ser la vigente y la 11 ofrece la malta en Bolsa
     * de 25 kg (200) y el lúpulo en Paquete (201), con ítems nuevos; el Balde de 5 kg de la malta sale del catálogo.
     */
    private VersionProveedorEntity modificarCatalogo() {
        VersionProveedorEntity anterior = proveedor.getVersiones().get(0);
        anterior.setEsUltimaVersion(false);
        VersionProveedorEntity vigente = VersionProveedorEntity.builder().id(11L).razonSocial("MALTERIA SUR").nombreComercial("Maltería Sur")
                .cuit("30123456789").esUltimaVersion(true).proveedor(proveedor).build();
        vigente.getCatalogoProveedor().add(catalogo(200L, vigente, maltaBolsa25.getInsumo(), maltaBolsa25.getPresentacionComercial()));
        vigente.getCatalogoProveedor().add(catalogo(201L, vigente, lupuloPaquete.getInsumo(), lupuloPaquete.getPresentacionComercial()));
        proveedor.getVersiones().add(vigente);
        return vigente;
    }

    private OfertaCotizacionEntity ofertaAnterior(long id, LineaSolicitudCotizacionEntity linea, CatalogoProveedorEntity item, String precio, int cantidad) {
        OfertaCotizacionEntity oferta = OfertaCotizacionEntity.builder().id(id).invitacion(invitacion).lineaSolicitud(linea)
                .catalogoProveedor(item).precioUnitario(new BigDecimal(precio)).cantidadDisponible(cantidad).build();
        invitacion.getOfertas().add(oferta);
        return oferta;
    }

    @Test
    @DisplayName("CP-CPV-23: con el catálogo modificado después de responder, el proveedor ve su respuesta en cada presentación que sigue en el catálogo vigente y no ve las que salieron")
    void buscar_catalogoModificado_muestraRespuestaPorPresentacion() {
        // === PREPARACION DE DATOS ===
        invitacion.setFechaRespuesta(LocalDateTime.now().minusHours(1));
        ofertaAnterior(1L, lineaMalta, maltaBolsa25, "32500", 4);
        ofertaAnterior(2L, lineaMalta, maltaBolsa5, "7000", 20);
        ofertaAnterior(3L, lineaLupulo, lupuloPaquete, "9000", 1);
        modificarCatalogo();

        // === EJECUCION ===
        CotizacionProveedorResponseDTO cotizacion = servicio.buscarCotizacion(TOKEN);

        // === ASSERTS ===
        // La malta solo figura en Bolsa (el Balde salió del catálogo), con el ítem vigente y la respuesta anterior
        assertThat(cotizacion.getLineas().get(0).getPresentaciones()).singleElement().satisfies(p -> {
            assertThat(p.getIdCatalogoProveedor()).isEqualTo(200L);
            assertThat(p.getNombre()).isEqualTo("Bolsa (25 kg)");
            assertThat(p.getPrecioUnitarioOfertado()).isEqualByComparingTo("32500");
            assertThat(p.getCantidadDisponibleOfertada()).isEqualTo(4);
        });
        assertThat(cotizacion.getLineas().get(1).getPresentaciones()).singleElement().satisfies(p -> {
            assertThat(p.getIdCatalogoProveedor()).isEqualTo(201L);
            assertThat(p.getPrecioUnitarioOfertado()).isEqualByComparingTo("9000");
            assertThat(p.getCantidadDisponibleOfertada()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("CP-CPV-24: con el catálogo modificado, un nuevo envío actualiza en su lugar la oferta de la misma presentación apuntándola al ítem vigente y quita las de presentaciones que salieron")
    void registrar_catalogoModificado_actualizaPorPresentacion() {
        // === PREPARACION DE DATOS ===
        OfertaCotizacionEntity anteriorBolsa = ofertaAnterior(1L, lineaMalta, maltaBolsa25, "30000", 2);
        ofertaAnterior(2L, lineaMalta, maltaBolsa5, "7000", 20);
        OfertaCotizacionEntity anteriorLupulo = ofertaAnterior(3L, lineaLupulo, lupuloPaquete, "9000", 1);
        VersionProveedorEntity vigente = modificarCatalogo();

        // === EJECUCION ===
        servicio.registrarCotizacion(TOKEN, form(fila(501L, 200L, "31000", 3), fila(502L, 201L, "9500", 2)));

        // === ASSERTS ===
        // Mismos registros, apuntando a los ítems vigentes; la oferta del Balde se quita
        assertThat(invitacion.getOfertas()).containsExactly(anteriorBolsa, anteriorLupulo);
        assertThat(anteriorBolsa.getCatalogoProveedor()).isSameAs(vigente.getCatalogoProveedor().get(0));
        assertThat(anteriorBolsa.getPrecioUnitario()).isEqualByComparingTo("31000");
        assertThat(anteriorBolsa.getCantidadDisponible()).isEqualTo(3);
        assertThat(anteriorLupulo.getCatalogoProveedor()).isSameAs(vigente.getCatalogoProveedor().get(1));
        assertThat(anteriorLupulo.getPrecioUnitario()).isEqualByComparingTo("9500");
    }

    @Test
    @DisplayName("CP-CPV-25: con el catálogo modificado, un ítem de la versión anterior ya no es una presentación válida")
    void registrar_catalogoModificado_rechazaItemDeVersionAnterior() {
        modificarCatalogo();

        verificarRegla(form(fila(501L, 100L, "31000", 3)), "Una de las presentaciones de Malta Pilsen no figura en su catálogo", null);
    }
}
