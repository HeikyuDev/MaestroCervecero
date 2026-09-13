package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
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
class LoteInsumoServicioImplTest {

    @Mock
    private ILoteInsumoRepository loteInsumoRepository;

    @InjectMocks
    private LoteInsumoServicioImpl loteInsumoServicio;

    // ==================== filtrarLotesInsumoDisponibles ====================

    @Test
    @DisplayName("CP-FLD-01: filtrarLotesInsumoDisponibles retorna los lotes de insumo con cantidad disponible, mapeados a DTO")
    void filtrarLotesInsumoDisponibles_debeRetornarLotesMapeados() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LoteInsumoEntity loteA = loteInsumoEntity(10L, malta, "LOTE-A", 10.0, 4.0);
        LoteInsumoEntity loteB = loteInsumoEntity(11L, malta, "LOTE-B", 5.0, 0.0);
        when(loteInsumoRepository.filtrarLotesInsumoDisponibles(1L)).thenReturn(List.of(loteA, loteB));

        // === EJECUCION ===
        List<LoteInsumoResponseDTO> resultado = loteInsumoServicio.filtrarLotesInsumoDisponibles(1L);

        // === ASSERTS ===
        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting(LoteInsumoResponseDTO::getIdentificacionLoteProveedor).containsExactly("LOTE-A", "LOTE-B");
        verify(loteInsumoRepository).filtrarLotesInsumoDisponibles(1L);
    }

    @Test
    @DisplayName("CP-FLD-02: filtrarLotesInsumoDisponibles retorna una lista vacía cuando el insumo no tiene lotes con cantidad disponible")
    void filtrarLotesInsumoDisponibles_debeRetornarListaVaciaSinLotesDisponibles() {
        // === PREPARACION DE DATOS ===
        when(loteInsumoRepository.filtrarLotesInsumoDisponibles(1L)).thenReturn(List.of());

        // === EJECUCION ===
        List<LoteInsumoResponseDTO> resultado = loteInsumoServicio.filtrarLotesInsumoDisponibles(1L);

        // === ASSERTS ===
        assertThat(resultado).isEmpty();
        verify(loteInsumoRepository).filtrarLotesInsumoDisponibles(1L);
    }

    // ==================== helpers de construcción ====================

    private static MaltaEntity maltaEntity(Long id) {
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").estado(Estado.ACTIVO).build();
    }

    private static LoteInsumoEntity loteInsumoEntity(Long id, MaltaEntity insumo, String identificacionLoteProveedor, double cantidadActual, double cantidadReservada) {
        return LoteInsumoEntity.builder()
                .id(id)
                .insumo(insumo)
                .identificacionLoteProveedor(identificacionLoteProveedor)
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .cantidadActual(cantidadActual)
                .cantidadReservada(cantidadReservada)
                .build();
    }
}
