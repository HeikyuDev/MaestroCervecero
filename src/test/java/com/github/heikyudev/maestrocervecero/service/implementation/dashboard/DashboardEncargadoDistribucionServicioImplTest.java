package com.github.heikyudev.maestrocervecero.service.implementation.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IControlBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.ContenidoBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.DashboardEncargadoDistribucionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardEncargadoDistribucionServicioImplTest {

    private static final Pageable TOPE = PageRequest.of(0, 5);
    private static final Pageable SOLO_EL_TOTAL = PageRequest.of(0, 1);

    @Mock
    private IDespachoBarrilRepository despachoBarrilRepository;

    @Mock
    private IControlBarrilServicio controlBarrilServicio;

    @InjectMocks
    private DashboardEncargadoDistribucionServicioImpl dashboardServicio;

    @Test
    @DisplayName("CP-DDI-01: obtenerDashboardEncargadoDistribucion con el sistema vacío devuelve los contadores en cero y los tres bloques sin filas")
    void obtener_debeDevolverElPanelEnCeroConElSistemaVacio() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();

        // === EJECUCION ===
        DashboardEncargadoDistribucionResponseDTO resultado = dashboardServicio.obtenerDashboardEncargadoDistribucion();

        // === ASSERTS ===
        assertThat(resultado.getBarrilesEnPoderDeClientes()).isZero();
        assertThat(resultado.getDevolucionesVencidas()).isZero();
        assertThat(resultado.getDevolucionesQueVencenHoy()).isZero();
        assertThat(resultado.getDevolucionesPendientes().getTotal()).isZero();
        assertThat(resultado.getDevolucionesPendientes().getFilas()).isEmpty();
        assertThat(resultado.getClientesConBarriles().getTotal()).isZero();
        assertThat(resultado.getClientesConBarriles().getFilas()).isEmpty();
        assertThat(resultado.getBarrilesListos().getTotal()).isZero();
        assertThat(resultado.getBarrilesListos().getFilas()).isEmpty();
    }

    @Test
    @DisplayName("CP-DDI-02: los contadores informan los barriles Despachados, las devoluciones anteriores a hoy y las de hoy, con la fecha de hoy")
    void obtener_debeCalcularLosContadores() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(controlBarrilServicio.filtrarBarrilesParaControl(isNull(), isNull(), eq(EstadoOperativoBarril.DESPACHADO), eq(SOLO_EL_TOTAL)))
                .thenReturn(new PageImpl<>(List.of(), SOLO_EL_TOTAL, 8));
        when(despachoBarrilRepository.contarVigentesConDevolucionAnteriorA(hoy)).thenReturn(3L);
        when(despachoBarrilRepository.contarVigentesConDevolucionEn(hoy)).thenReturn(1L);

        // === EJECUCION ===
        DashboardEncargadoDistribucionResponseDTO resultado = dashboardServicio.obtenerDashboardEncargadoDistribucion();

        // === ASSERTS ===
        assertThat(resultado.getBarrilesEnPoderDeClientes()).isEqualTo(8);
        assertThat(resultado.getDevolucionesVencidas()).isEqualTo(3);
        assertThat(resultado.getDevolucionesQueVencenHoy()).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-DDI-03: las devoluciones pendientes se piden con la fecha de hoy y el tope de 5 filas, con el total real, el cliente, el barril, la fecha y los días de atraso (0 si vence hoy)")
    void obtener_debeInformarLasDevolucionesPendientes() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(despachoBarrilRepository.buscarVigentesConDevolucionHastaParaPanel(hoy, TOPE)).thenReturn(new PageImpl<>(List.of(
                despacho(1L, 21L, "BRL-01", "Bar Don Jorge", hoy.minusDays(3)), despacho(2L, 22L, "BRL-02", "Resto Sur", hoy)), TOPE, 9));

        // === EJECUCION ===
        DashboardEncargadoDistribucionResponseDTO resultado = dashboardServicio.obtenerDashboardEncargadoDistribucion();

        // === ASSERTS ===
        assertThat(resultado.getDevolucionesPendientes().getTotal()).isEqualTo(9);
        assertThat(resultado.getDevolucionesPendientes().getFilas()).hasSize(2);
        assertThat(resultado.getDevolucionesPendientes().getFilas().get(0).getIdBarril()).isEqualTo(21L);
        assertThat(resultado.getDevolucionesPendientes().getFilas().get(0).getIdentificadorInterno()).isEqualTo("BRL-01");
        assertThat(resultado.getDevolucionesPendientes().getFilas().get(0).getNombreCliente()).isEqualTo("Bar Don Jorge");
        assertThat(resultado.getDevolucionesPendientes().getFilas().get(0).getFechaDevolucionEstimada()).isEqualTo(hoy.minusDays(3));
        assertThat(resultado.getDevolucionesPendientes().getFilas().get(0).getDiasDeAtraso()).isEqualTo(3);
        assertThat(resultado.getDevolucionesPendientes().getFilas().get(1).getDiasDeAtraso()).isZero();
        verify(despachoBarrilRepository).buscarVigentesConDevolucionHastaParaPanel(hoy, TOPE);
    }

    @Test
    @DisplayName("CP-DDI-04: los clientes con barriles se informan una sola vez cada uno, con la cantidad de barriles y la próxima fecha de devolución, y el total de clientes")
    void obtener_debeInformarLosClientesConBarriles() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        LocalDate hoy = LocalDate.now();
        when(despachoBarrilRepository.buscarClientesConBarrilesEnPoderParaPanel(TOPE)).thenReturn(new PageImpl<>(List.of(
                new Object[]{5L, "Bar Don Jorge", 2L, hoy.minusDays(1)}, new Object[]{6L, "Resto Sur", 1L, hoy.plusDays(7)}), TOPE, 9));

        // === EJECUCION ===
        DashboardEncargadoDistribucionResponseDTO resultado = dashboardServicio.obtenerDashboardEncargadoDistribucion();

        // === ASSERTS ===
        assertThat(resultado.getClientesConBarriles().getTotal()).isEqualTo(9);
        assertThat(resultado.getClientesConBarriles().getFilas()).hasSize(2);
        assertThat(resultado.getClientesConBarriles().getFilas().get(0).getIdCliente()).isEqualTo(5L);
        assertThat(resultado.getClientesConBarriles().getFilas().get(0).getNombre()).isEqualTo("Bar Don Jorge");
        assertThat(resultado.getClientesConBarriles().getFilas().get(0).getCantidadBarriles()).isEqualTo(2);
        assertThat(resultado.getClientesConBarriles().getFilas().get(0).getProximaFechaDevolucion()).isEqualTo(hoy.minusDays(1));
        assertThat(resultado.getClientesConBarriles().getFilas().get(1).getNombre()).isEqualTo("Resto Sur");
    }

    @Test
    @DisplayName("CP-DDI-05: los barriles listos son los Con cerveza, pedidos con el tope de 5 filas, con la receta, el lote y el contenido, y el total real")
    void obtener_debeInformarLosBarrilesListosParaDespachar() {
        // === PREPARACION DE DATOS ===
        prepararSistemaVacio();
        BarrilControlResponseDTO conCerveza = BarrilControlResponseDTO.builder().id(31L).identificadorInterno("BRL-02").contenidoActual(50.0)
                .estadoOperativo(EstadoOperativoBarril.CON_CERVEZA).contenido(new ContenidoBarrilResponseDTO(12L, "PALE_ALE-1", "Pale Ale")).build();
        BarrilControlResponseDTO sinContenido = BarrilControlResponseDTO.builder().id(32L).identificadorInterno("BRL-03").contenidoActual(30.0)
                .estadoOperativo(EstadoOperativoBarril.CON_CERVEZA).build();
        when(controlBarrilServicio.filtrarBarrilesParaControl(isNull(), isNull(), eq(EstadoOperativoBarril.CON_CERVEZA), eq(TOPE)))
                .thenReturn(new PageImpl<>(List.of(conCerveza, sinContenido), TOPE, 7));

        // === EJECUCION ===
        DashboardEncargadoDistribucionResponseDTO resultado = dashboardServicio.obtenerDashboardEncargadoDistribucion();

        // === ASSERTS ===
        assertThat(resultado.getBarrilesListos().getTotal()).isEqualTo(7);
        assertThat(resultado.getBarrilesListos().getFilas().get(0).getId()).isEqualTo(31L);
        assertThat(resultado.getBarrilesListos().getFilas().get(0).getNombreReceta()).isEqualTo("Pale Ale");
        assertThat(resultado.getBarrilesListos().getFilas().get(0).getCodigoLote()).isEqualTo("PALE_ALE-1");
        assertThat(resultado.getBarrilesListos().getFilas().get(0).getContenidoActual()).isEqualTo(50.0);
        assertThat(resultado.getBarrilesListos().getFilas().get(1).getNombreReceta()).isNull();
        assertThat(resultado.getBarrilesListos().getFilas().get(1).getCodigoLote()).isNull();
    }

    // ==================== helpers ====================

    /** Deja todos los contadores y bloques en cero; cada test pisa solo lo que ejercita. */
    private void prepararSistemaVacio() {
        lenient().when(controlBarrilServicio.filtrarBarrilesParaControl(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty(TOPE));
        lenient().when(despachoBarrilRepository.contarVigentesConDevolucionAnteriorA(any(LocalDate.class))).thenReturn(0L);
        lenient().when(despachoBarrilRepository.contarVigentesConDevolucionEn(any(LocalDate.class))).thenReturn(0L);
        lenient().when(despachoBarrilRepository.buscarVigentesConDevolucionHastaParaPanel(any(LocalDate.class), any(Pageable.class))).thenReturn(Page.empty(TOPE));
        lenient().when(despachoBarrilRepository.buscarClientesConBarrilesEnPoderParaPanel(any(Pageable.class))).thenReturn(Page.empty(TOPE));
    }

    private static DespachoBarrilEntity despacho(Long id, Long idBarril, String identificador, String cliente, LocalDate devolucion) {
        return DespachoBarrilEntity.builder().id(id).fechaDevolucionEstimada(devolucion)
                .barril(BarrilEntity.builder().id(idBarril).identificadorInterno(identificador).build())
                .cliente(ClienteEntity.builder().nombre(cliente).build()).build();
    }
}
