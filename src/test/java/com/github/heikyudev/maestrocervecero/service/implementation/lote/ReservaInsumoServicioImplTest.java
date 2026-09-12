package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaInsumoServicioImplTest {

    @Mock
    private IReservaInsumoRepository reservaInsumoRepository;

    @InjectMocks
    private ReservaInsumoServicioImpl reservaInsumoServicio;

    // ==================== buscarPorEtapaEInsumo ====================

    @Test
    @DisplayName("CP-BE-01: buscarPorEtapaEInsumo retorna las reservas de insumo de esa etapa para ese insumo, mapeadas a DTO")
    void buscarPorEtapaEInsumo_debeRetornarReservasMapeadas() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote();
        EtapaLoteEntity etapaLote = crearEtapaLote(1L, lote);
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteInsumoA = loteInsumoEntity(10L, malta, "LOTE-A");
        LoteInsumoEntity loteInsumoB = loteInsumoEntity(11L, malta, "LOTE-B");
        ReservaInsumoEntity reservaA = reservaInsumoEntity(100L, etapaLote, loteInsumoA, 8.0);
        ReservaInsumoEntity reservaB = reservaInsumoEntity(101L, etapaLote, loteInsumoB, 4.0);
        when(reservaInsumoRepository.findByEtapaLoteIdAndLoteInsumo_Insumo_Id(1L, 1L)).thenReturn(List.of(reservaA, reservaB));

        // === EJECUCION ===
        List<ReservaInsumoResponseDTO> resultado = reservaInsumoServicio.buscarPorEtapaEInsumo(1L, 1L);

        // === ASSERTS ===
        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(ReservaInsumoResponseDTO::getCantidadReservada).containsExactly(8.0, 4.0);
        verify(reservaInsumoRepository).findByEtapaLoteIdAndLoteInsumo_Insumo_Id(1L, 1L);
    }

    @Test
    @DisplayName("CP-BE-02: buscarPorEtapaEInsumo retorna una lista vacía cuando no hay reservas de ese insumo en esa etapa")
    void buscarPorEtapaEInsumo_debeRetornarListaVaciaSinReservas() {
        // === PREPARACION DE DATOS ===
        when(reservaInsumoRepository.findByEtapaLoteIdAndLoteInsumo_Insumo_Id(1L, 1L)).thenReturn(List.of());

        // === EJECUCION ===
        List<ReservaInsumoResponseDTO> resultado = reservaInsumoServicio.buscarPorEtapaEInsumo(1L, 1L);

        // === ASSERTS ===
        assertThat(resultado).isEmpty();
        verify(reservaInsumoRepository).findByEtapaLoteIdAndLoteInsumo_Insumo_Id(1L, 1L);
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
                .cantidadReservada(cantidadReservada)
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
