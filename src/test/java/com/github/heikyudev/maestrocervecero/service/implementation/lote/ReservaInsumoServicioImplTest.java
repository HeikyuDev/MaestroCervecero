package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoReservaInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaInsumoServicioImplTest {

    @Mock
    private IReservaInsumoRepository reservaInsumoRepository;

    @InjectMocks
    private ReservaInsumoServicioImpl reservaInsumoServicio;

    // ==================== filtrarLotesInsumoReservados ====================

    @Test
    @DisplayName("CP-FLR-01: filtrarLotesInsumoReservados retorna las reservas de insumo de esa etapa para ese insumo, mapeadas a DTO")
    void filtrarLotesInsumoReservados_debeRetornarReservasMapeadas() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote();
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, lote);
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumoA = loteInsumoEntity(10L, malta, "LOTE-A");
        LoteInsumoEntity loteInsumoB = loteInsumoEntity(11L, malta, "LOTE-B");
        ReservaInsumoEntity reservaA = reservaInsumoEntity(100L, etapaLote, loteInsumoA, 8.0);
        ReservaInsumoEntity reservaB = reservaInsumoEntity(101L, etapaLote, loteInsumoB, 4.0);
        when(reservaInsumoRepository.filtrarLotesInsumoReservados(1L, 1L)).thenReturn(List.of(reservaA, reservaB));

        // === EJECUCION ===
        List<ReservaInsumoResponseDTO> resultado = reservaInsumoServicio.filtrarLotesInsumoReservados(1L, 1L);

        // === ASSERTS ===
        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(ReservaInsumoResponseDTO::getCantidadReservada).containsExactly(8.0, 4.0);
        verify(reservaInsumoRepository).filtrarLotesInsumoReservados(1L, 1L);
    }

    @Test
    @DisplayName("CP-FLR-02: filtrarLotesInsumoReservados retorna una lista vacía cuando no hay reservas de ese insumo en esa etapa")
    void filtrarLotesInsumoReservados_debeRetornarListaVaciaSinReservas() {
        // === PREPARACION DE DATOS ===
        when(reservaInsumoRepository.filtrarLotesInsumoReservados(1L, 1L)).thenReturn(List.of());

        // === EJECUCION ===
        List<ReservaInsumoResponseDTO> resultado = reservaInsumoServicio.filtrarLotesInsumoReservados(1L, 1L);

        // === ASSERTS ===
        assertThat(resultado).isEmpty();
        verify(reservaInsumoRepository).filtrarLotesInsumoReservados(1L, 1L);
    }

    @Test
    @DisplayName("CP-FLR-03: filtrarLotesInsumoReservados con el insumo nulo propaga el nulo (todas las reservas de la etapa) y marca como vencido el lote de insumo vencido")
    void filtrarLotesInsumoReservados_debePropagarInsumoNuloYMarcarVencidos() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, crearLote());
        LoteInsumoEntity vencido = loteInsumoEntity(10L, maltaEntity(1L), "LOTE-A");
        vencido.setFechaVencimiento(LocalDate.now().minusDays(1));
        LoteInsumoEntity vigente = loteInsumoEntity(11L, maltaEntity(2L), "LOTE-B");
        when(reservaInsumoRepository.filtrarLotesInsumoReservados(1L, null)).thenReturn(List.of(
                reservaInsumoEntity(100L, etapaLote, vencido, 8.0),
                reservaInsumoEntity(101L, etapaLote, vigente, 0.0)));

        // === EJECUCION ===
        List<ReservaInsumoResponseDTO> resultado = reservaInsumoServicio.filtrarLotesInsumoReservados(1L, null);

        // === ASSERTS ===
        assertThat(resultado).extracting(ReservaInsumoResponseDTO::getCantidadReservada).containsExactly(8.0, 0.0);
        assertThat(resultado.get(0).getLoteInsumo().isVencido()).isTrue();
        assertThat(resultado.get(1).getLoteInsumo().isVencido()).isFalse();
        verify(reservaInsumoRepository).filtrarLotesInsumoReservados(1L, null);
    }

    // ==================== filtrarReservasConSaldo ====================

    @Test
    @DisplayName("CP-FRS-01: filtrarReservasConSaldo restringe por el insumo, normaliza la identificación, ignora el orden del pageable y retorna las reservas mapeadas con el vencido marcado")
    void filtrarReservasConSaldo_debeRestringirPorInsumoYMapear() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, crearLote());
        LoteInsumoEntity vencido = loteInsumoEntity(10L, maltaEntity(1L), "LOTE-A");
        vencido.setFechaVencimiento(LocalDate.now().minusDays(1));
        when(reservaInsumoRepository.filtrarReservasConSaldo(eq(1L), eq(7L), eq("l-125"), any(LocalDate.class), any()))
                .thenReturn(new PageImpl<>(List.of(reservaInsumoEntity(100L, etapaLote, vencido, 8.0))));

        // === EJECUCION ===
        Page<ReservaInsumoResponseDTO> resultado = reservaInsumoServicio.filtrarReservasConSaldo(
                1L, 7L, " l-125 ", PageRequest.of(2, 8, Sort.by("id").descending()));

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getCantidadReservada()).isEqualTo(8.0);
        assertThat(resultado.getContent().get(0).getLoteInsumo().isVencido()).isTrue();
        verify(reservaInsumoRepository).filtrarReservasConSaldo(eq(1L), eq(7L), eq("l-125"), any(LocalDate.class), eq(PageRequest.of(2, 8)));
    }

    @Test
    @DisplayName("CP-FRS-02: filtrarReservasConSaldo con criterios vacíos o nulos los propaga como nulos (no restringen)")
    void filtrarReservasConSaldo_debePropagarCriteriosVaciosComoNulos() {
        // === PREPARACION DE DATOS ===
        when(reservaInsumoRepository.filtrarReservasConSaldo(eq(1L), eq(null), eq(null), any(LocalDate.class), any()))
                .thenReturn(Page.empty());

        // === EJECUCION ===
        Page<ReservaInsumoResponseDTO> resultado = reservaInsumoServicio.filtrarReservasConSaldo(1L, null, "   ", PageRequest.of(0, 8));

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(reservaInsumoRepository).filtrarReservasConSaldo(eq(1L), eq(null), eq(null), any(LocalDate.class), eq(PageRequest.of(0, 8)));
    }

    // ==================== listarReservasDeEtapa ====================

    @Test
    @DisplayName("CP-LRE-01: listarReservasDeEtapa retorna todas las reservas de la etapa con su historial, también las consumidas y las liberadas")
    void listarReservasDeEtapa_debeRetornarTodasLasReservasConSuHistorial() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, crearLote());
        LoteInsumoEntity loteInsumo = loteInsumoEntity(10L, maltaEntity(1L), "LOTE-A");
        ReservaInsumoEntity activa = reservaInsumoEntity(100L, etapaLote, loteInsumo, 8.0);
        ReservaInsumoEntity consumida = reservaInsumoEntity(101L, etapaLote, loteInsumo, 5.0);
        consumida.consumir(5.0);
        ReservaInsumoEntity liberada = reservaInsumoEntity(102L, etapaLote, loteInsumo, 4.0);
        liberada.liberar();
        when(reservaInsumoRepository.listarPorEtapa(1L)).thenReturn(List.of(activa, consumida, liberada));

        // === EJECUCION ===
        List<ReservaInsumoResponseDTO> resultado = reservaInsumoServicio.listarReservasDeEtapa(1L);

        // === ASSERTS ===
        assertThat(resultado).extracting(ReservaInsumoResponseDTO::getEstado)
                .containsExactly(EstadoReservaInsumo.ACTIVA, EstadoReservaInsumo.CONSUMIDA, EstadoReservaInsumo.LIBERADA);
        assertThat(resultado).extracting(ReservaInsumoResponseDTO::getCantidadReservada).containsExactly(8.0, 5.0, 4.0);
        assertThat(resultado).extracting(ReservaInsumoResponseDTO::getCantidadConsumida).containsExactly(0.0, 5.0, 0.0);
        assertThat(resultado).extracting(ReservaInsumoResponseDTO::getCantidadLiberada).containsExactly(0.0, 0.0, 4.0);
        assertThat(resultado).extracting(ReservaInsumoResponseDTO::getCantidadPendiente).containsExactly(8.0, 0.0, 0.0);
        verify(reservaInsumoRepository).listarPorEtapa(1L);
    }

    @Test
    @DisplayName("CP-LRE-02: listarReservasDeEtapa retorna una lista vacía cuando la etapa no tiene reservas")
    void listarReservasDeEtapa_debeRetornarListaVaciaSinReservas() {
        // === PREPARACION DE DATOS ===
        when(reservaInsumoRepository.listarPorEtapa(1L)).thenReturn(List.of());

        // === EJECUCION ===
        List<ReservaInsumoResponseDTO> resultado = reservaInsumoServicio.listarReservasDeEtapa(1L);

        // === ASSERTS ===
        assertThat(resultado).isEmpty();
    }

    // ==================== listarReservasDelLote ====================

    @Test
    @DisplayName("CP-LRL-01: listarReservasDelLote retorna todas las reservas del lote (activas, consumidas y liberadas) con su historial, mapeadas a DTO")
    void listarReservasDelLote_debeRetornarTodasLasReservasDelLote() {
        // === PREPARACION DE DATOS ===
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, crearLote());
        LoteInsumoEntity loteInsumo = loteInsumoEntity(10L, maltaEntity(1L), "LOTE-A");
        ReservaInsumoEntity activa = reservaInsumoEntity(100L, etapaLote, loteInsumo, 8.0);
        ReservaInsumoEntity liberada = reservaInsumoEntity(101L, etapaLote, loteInsumo, 4.0);
        liberada.liberar();
        when(reservaInsumoRepository.listarPorLote(7L)).thenReturn(List.of(activa, liberada));

        // === EJECUCION ===
        List<ReservaInsumoResponseDTO> resultado = reservaInsumoServicio.listarReservasDelLote(7L);

        // === ASSERTS ===
        assertThat(resultado).extracting(ReservaInsumoResponseDTO::getEstado).containsExactly(EstadoReservaInsumo.ACTIVA, EstadoReservaInsumo.LIBERADA);
        assertThat(resultado).extracting(ReservaInsumoResponseDTO::getCantidadLiberada).containsExactly(0.0, 4.0);
        verify(reservaInsumoRepository).listarPorLote(7L);
    }

    @Test
    @DisplayName("CP-LRL-02: listarReservasDelLote retorna una lista vacía cuando el lote no tiene reservas")
    void listarReservasDelLote_debeRetornarListaVaciaSinReservas() {
        // === PREPARACION DE DATOS ===
        when(reservaInsumoRepository.listarPorLote(7L)).thenReturn(List.of());

        // === EJECUCION + ASSERTS ===
        assertThat(reservaInsumoServicio.listarReservasDelLote(7L)).isEmpty();
    }

    // ==================== helpers de construcción ====================

    private static MaltaEntity maltaEntity(Long id) {
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").estado(Estado.ACTIVO).build();
    }

    private static LoteInsumoEntity loteInsumoEntity(Long id, MaltaEntity insumo, String identificacionLoteProveedor) {
        return LoteInsumoEntity.builder()
                .id(id)
                .insumo(insumo)
                .identificacionLoteProveedor(identificacionLoteProveedor)
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .cantidadActual(20.0)
                .cantidadReservada(0.0)
                .build();
    }

    private static ReservaInsumoEntity reservaInsumoEntity(Long id, EtapaLoteEntity etapaLote, LoteInsumoEntity loteInsumo, double cantidadReservada) {
        return ReservaInsumoEntity.builder()
                .id(id)
                .etapaLote(etapaLote)
                .loteInsumo(loteInsumo)
                .cantidadReservada(cantidadReservada).cantidadPendiente(cantidadReservada)
                .build();
    }

    private static LoteEntity crearLote() {
        return LoteEntity.builder()
                .id(1L)
                .identificadorInterno("IPA Test-1")
                .volumenObjetivo(20.0)
                .estado(EstadoLote.EN_EJECUCION)
                .fechaInicioEstimada(LocalDate.now())
                .fechaFinalizacionEstimada(LocalDate.now().plusDays(20))
                .build();
    }

    private static EtapaLoteEntity crearEtapaLote(Long id, LoteEntity lote) {
        return EtapaLoteEntity.builder()
                .id(id)
                .etapa(TipoEtapa.MACERACION)
                .estado(EstadoEtapaLote.EN_CURSO)
                .lote(lote)
                .build();
    }
}
