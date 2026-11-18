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

        solicitud.setEstado(EstadoSolicitudCotizacion.PLANES_GENERADOS);
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
    @DisplayName("CP-CPV-05: el proveedor ve solo los insumos solicitados que ofrece, con sus presentaciones y su respuesta actual")
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
        assertThat(cotizacion.getLineas().get(0).getIdCatalogoProveedorOfertado()).isEqualTo(100L);
        assertThat(cotizacion.getLineas().get(0).getPrecioUnitarioOfertado()).isEqualByComparingTo("32500");
        assertThat(cotizacion.getLineas().get(0).getCantidadDisponibleOfertada()).isEqualTo(4);
        assertThat(cotizacion.getLineas().get(1).getIdCatalogoProveedorOfertado()).isNull();
    }

    // ==================== registrarCotizacion ====================

    @Test
    @DisplayName("CP-CPV-06: registrarCotizacion rechaza los datos nulos, el exceso de filas y las filas que no corresponden")
    void registrar_rechazaFilasInvalidas() {
        verificarRegla(null, "Los datos de la cotización son obligatorios", null);
        verificarRegla(CotizacionProveedorFormDTO.builder().ofertas(new ArrayList<>(Collections.nCopies(101, fila(501L, null, null, null)))).build(),
                "La cotización no puede superar 100 insumos", null);
        verificarRegla(CotizacionProveedorFormDTO.builder().ofertas(new ArrayList<>(Collections.singletonList(null))).build(),
                "Las filas de la cotización son obligatorias", null);
        verificarRegla(form(fila(null, 100L, "1", 1)), "El insumo de cada fila de la cotización es obligatorio", null);
        // La levadura es de la solicitud pero el proveedor no la ofrece; la línea 999 no es de la solicitud
        verificarRegla(form(fila(503L, 100L, "1", 1)), "Uno de los insumos de la cotización no forma parte de la solicitud o no figura en su catálogo", null);
        verificarRegla(form(fila(999L, 100L, "1", 1)), "Uno de los insumos de la cotización no forma parte de la solicitud o no figura en su catálogo", null);
        verificarRegla(form(fila(501L, 100L, "1", 1), fila(501L, null, null, null)), "La cotización no puede repetir el mismo insumo", null);
    }

    @Test
    @DisplayName("CP-CPV-07: una fila a medio completar exige presentación del catálogo, precio y cantidad válidos, indicando el campo")
    void registrar_validaCadaCampoDeLaFila() {
        verificarRegla(form(fila(501L, null, "100", 2)), "Seleccione la presentación de Malta Pilsen", "ofertas[0].idCatalogoProveedor");
        verificarRegla(form(fila(502L, null, null, null), fila(501L, 102L, "100", 2)), "La presentación elegida para Malta Pilsen no figura en su catálogo", "ofertas[1].idCatalogoProveedor");
        verificarRegla(form(fila(501L, 100L, null, 2)), "El precio unitario de Malta Pilsen es obligatorio", "ofertas[0].precioUnitario");
        verificarRegla(form(fila(501L, 100L, "0", 2)), "El precio unitario de Malta Pilsen debe ser mayor a 0", "ofertas[0].precioUnitario");
        verificarRegla(form(fila(501L, 100L, "1000000000.01", 2)), "El precio unitario de Malta Pilsen no puede superar 1.000.000.000", "ofertas[0].precioUnitario");
        verificarRegla(form(fila(501L, 100L, "10.12345", 2)), "El precio unitario de Malta Pilsen no puede tener más de 4 decimales", "ofertas[0].precioUnitario");
        verificarRegla(form(fila(501L, 100L, "10", null)), "La cantidad disponible de Malta Pilsen es obligatoria", "ofertas[0].cantidadDisponible");
        verificarRegla(form(fila(501L, 100L, "10", 0)), "La cantidad disponible de Malta Pilsen debe ser mayor a 0", "ofertas[0].cantidadDisponible");
        verificarRegla(form(fila(501L, 100L, "10", 1_000_001)), "La cantidad disponible de Malta Pilsen no puede superar 1.000.000", "ofertas[0].cantidadDisponible");
    }

    @Test
    @DisplayName("CP-CPV-08: registrarCotizacion guarda una oferta por fila completa, ignora las vacías y registra la fecha de respuesta, con la solicitud bloqueada")
    void registrar_guardaOfertas() {
        // === EJECUCION ===
        CotizacionProveedorResponseDTO resultado = servicio.registrarCotizacion(TOKEN,
                form(fila(501L, 100L, "32500.5", 4), fila(502L, null, null, null)));

        // === ASSERTS ===
        assertThat(invitacion.getOfertas()).singleElement().satisfies(oferta -> {
            assertThat(oferta.getLineaSolicitud()).isSameAs(lineaMalta);
            assertThat(oferta.getCatalogoProveedor()).isSameAs(maltaBolsa25);
            assertThat(oferta.getPrecioUnitario()).isEqualByComparingTo("32500.5");
            assertThat(oferta.getCantidadDisponible()).isEqualTo(4);
            assertThat(oferta.getInvitacion()).isSameAs(invitacion);
        });
        assertThat(invitacion.getFechaRespuesta()).isNotNull();
        assertThat(resultado.getLineas().get(0).getIdCatalogoProveedorOfertado()).isEqualTo(100L);
        InOrder orden = inOrder(invitacionRepository, solicitudRepository);
        orden.verify(invitacionRepository).buscarIdSolicitudPorTokenHash(HASH);
        orden.verify(solicitudRepository).buscarPorIdParaActualizar(90L);
        orden.verify(invitacionRepository).buscarPorTokenHash(HASH);
        orden.verify(invitacionRepository).save(invitacion);
    }

    @Test
    @DisplayName("CP-CPV-09: un nuevo envío actualiza en su lugar las ofertas que se vuelven a cotizar, quita las vaciadas y agrega las nuevas")
    void registrar_reemplazaRespuestaAnterior() {
        // === PREPARACION DE DATOS ===
        OfertaCotizacionEntity anteriorMalta = OfertaCotizacionEntity.builder().id(1L).invitacion(invitacion).lineaSolicitud(lineaMalta)
                .catalogoProveedor(maltaBolsa25).precioUnitario(new BigDecimal("30000")).cantidadDisponible(2).build();
        OfertaCotizacionEntity anteriorLupulo = OfertaCotizacionEntity.builder().id(2L).invitacion(invitacion).lineaSolicitud(lineaLupulo)
                .catalogoProveedor(lupuloPaquete).precioUnitario(new BigDecimal("9000")).cantidadDisponible(1).build();
        invitacion.getOfertas().addAll(List.of(anteriorMalta, anteriorLupulo));

        // === EJECUCION ===
        servicio.registrarCotizacion(TOKEN, form(fila(501L, 101L, "7000", 20), fila(502L, null, null, null)));

        // === ASSERTS ===
        assertThat(invitacion.getOfertas()).singleElement().isSameAs(anteriorMalta);
        assertThat(anteriorMalta.getId()).isEqualTo(1L);
        assertThat(anteriorMalta.getCatalogoProveedor()).isSameAs(maltaBolsa5);
        assertThat(anteriorMalta.getPrecioUnitario()).isEqualByComparingTo("7000");
        assertThat(anteriorMalta.getCantidadDisponible()).isEqualTo(20);
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
}
