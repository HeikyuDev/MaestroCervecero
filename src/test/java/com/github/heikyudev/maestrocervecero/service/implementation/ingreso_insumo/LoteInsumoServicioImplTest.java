package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.LoteInsumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    // ==================== filtrarLotesInsumoDisponibles (paginado, con criterios) ====================

    @Test
    @DisplayName("CP-FLD-03: filtrarLotesInsumoDisponibles paginado conserva el orden que da la consulta y marca como vencido solo el lote con vencimiento anterior a hoy")
    void filtrarLotesInsumoDisponiblesPaginado_debeConservarElOrdenYMarcarLosVencidos() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LocalDate hoy = LocalDate.now();
        LoteInsumoEntity vigente = loteInsumoEntity(10L, malta, "LOTE-A", 10.0, 4.0);
        vigente.setFechaVencimiento(hoy.plusDays(5));
        LoteInsumoEntity venceHoy = loteInsumoEntity(11L, malta, "LOTE-B", 5.0, 0.0);
        venceHoy.setFechaVencimiento(hoy);
        LoteInsumoEntity vencido = loteInsumoEntity(12L, malta, "LOTE-C", 2.0, 0.0);
        vencido.setFechaVencimiento(hoy.minusDays(1));
        Pageable pageable = PageRequest.of(0, 8);
        when(loteInsumoRepository.filtrarLotesInsumoDisponiblesParaConsumo(List.of(1L), null, null, hoy, pageable))
                .thenReturn(new PageImpl<>(List.of(vigente, venceHoy, vencido), pageable, 3));

        // === EJECUCION ===
        Page<LoteInsumoResponseDTO> resultado = loteInsumoServicio.filtrarLotesInsumoDisponibles(List.of(1L), null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getContent()).extracting(LoteInsumoResponseDTO::getIdentificacionLoteProveedor).containsExactly("LOTE-A", "LOTE-B", "LOTE-C");
        assertThat(resultado.getContent()).extracting(LoteInsumoResponseDTO::isVencido).containsExactly(false, false, true);
        assertThat(resultado.getContent().get(0).getFechaVencimiento()).isEqualTo(hoy.plusDays(5));
        assertThat(resultado.getContent().get(0).getCantidadDisponible()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("CP-FLD-04: filtrarLotesInsumoDisponibles paginado sin insumos de la etapa (nulo o vacío) devuelve una página vacía sin consultar")
    void filtrarLotesInsumoDisponiblesPaginado_debeRetornarPaginaVaciaSinInsumos() {
        // === EJECUCION ===
        Page<LoteInsumoResponseDTO> conNulo = loteInsumoServicio.filtrarLotesInsumoDisponibles(null, null, null, PageRequest.of(0, 8));
        Page<LoteInsumoResponseDTO> conVacio = loteInsumoServicio.filtrarLotesInsumoDisponibles(List.of(), "malta", null, PageRequest.of(0, 8));

        // === ASSERTS ===
        assertThat(conNulo.getContent()).isEmpty();
        assertThat(conVacio.getContent()).isEmpty();
        verifyNoInteractions(loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-FLD-05: filtrarLotesInsumoDisponibles paginado normaliza el nombre (nombrePropio) y la identificación (código en mayúsculas) y trata los textos en blanco como no filtrar")
    void filtrarLotesInsumoDisponiblesPaginado_debeNormalizarLosTextosBuscados() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 8);
        when(loteInsumoRepository.filtrarLotesInsumoDisponiblesParaConsumo(eq(List.of(1L)), any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        loteInsumoServicio.filtrarLotesInsumoDisponibles(List.of(1L), "   Malta    Pilsen ", "  lote-a  ", pageable);
        loteInsumoServicio.filtrarLotesInsumoDisponibles(List.of(1L), "    ", "   ", pageable);

        // === ASSERTS ===
        verify(loteInsumoRepository).filtrarLotesInsumoDisponiblesParaConsumo(eq(List.of(1L)), eq("Malta Pilsen"), eq("LOTE-A"), any(), eq(pageable));
        verify(loteInsumoRepository).filtrarLotesInsumoDisponiblesParaConsumo(eq(List.of(1L)), isNull(), isNull(), any(), eq(pageable));
    }

    @Test
    @DisplayName("CP-FLD-06: filtrarLotesInsumoDisponibles paginado ignora el orden del Pageable (el FEFO lo define la consulta), conserva página y tamaño y pasa la fecha de hoy")
    void filtrarLotesInsumoDisponiblesPaginado_debeIgnorarElOrdenDelPageableYPasarHoy() {
        // === PREPARACION DE DATOS ===
        Pageable pageableConOrden = PageRequest.of(1, 8, Sort.by(Sort.Direction.DESC, "id"));
        when(loteInsumoRepository.filtrarLotesInsumoDisponiblesParaConsumo(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 8), 0));

        // === EJECUCION ===
        loteInsumoServicio.filtrarLotesInsumoDisponibles(List.of(1L, 2L), null, null, pageableConOrden);

        // === ASSERTS ===
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(loteInsumoRepository).filtrarLotesInsumoDisponiblesParaConsumo(eq(List.of(1L, 2L)), isNull(), isNull(), eq(LocalDate.now()), captor.capture());
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
    }

    @Test
    @DisplayName("CP-FLD-07: filtrarLotesInsumoDisponibles paginado devuelve una página vacía cuando ningún lote cumple los criterios")
    void filtrarLotesInsumoDisponiblesPaginado_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 8);
        when(loteInsumoRepository.filtrarLotesInsumoDisponiblesParaConsumo(List.of(1L), "Inexistente", null, LocalDate.now(), pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<LoteInsumoResponseDTO> resultado = loteInsumoServicio.filtrarLotesInsumoDisponibles(List.of(1L), "Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        assertThat(resultado.getTotalElements()).isZero();
    }

    // ==================== filtrarLotesInsumoPorInsumo ====================

    @Test
    @DisplayName("CP-FLI-01: filtrarLotesInsumoPorInsumo conserva el orden de la consulta, incluye los lotes agotados y marca como vencido solo el lote con vencimiento anterior a hoy")
    void filtrarLotesInsumoPorInsumo_debeIncluirAgotadosYMarcarLosVencidos() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = maltaEntity(1L);
        LocalDate hoy = LocalDate.now();
        LoteInsumoEntity vencido = loteInsumoEntity(12L, malta, "LOTE-C", 2.0, 0.0);
        vencido.setFechaVencimiento(hoy.minusDays(1));
        LoteInsumoEntity agotado = loteInsumoEntity(10L, malta, "LOTE-A", 0.0, 0.0);
        agotado.setFechaVencimiento(hoy.plusDays(5));
        LoteInsumoEntity vigente = loteInsumoEntity(11L, malta, "LOTE-B", 5.0, 1.0);
        vigente.setFechaVencimiento(hoy.plusDays(9));
        Pageable pageable = PageRequest.of(0, 8);
        when(loteInsumoRepository.filtrarLotesInsumoPorInsumo(1L, null, pageable))
                .thenReturn(new PageImpl<>(List.of(vencido, agotado, vigente), pageable, 3));

        // === EJECUCION ===
        Page<LoteInsumoResponseDTO> resultado = loteInsumoServicio.filtrarLotesInsumoPorInsumo(1L, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertThat(resultado.getContent()).extracting(LoteInsumoResponseDTO::getIdentificacionLoteProveedor).containsExactly("LOTE-C", "LOTE-A", "LOTE-B");
        assertThat(resultado.getContent()).extracting(LoteInsumoResponseDTO::isVencido).containsExactly(true, false, false);
        assertThat(resultado.getContent().get(1).getCantidadActual()).isZero();
    }

    @Test
    @DisplayName("CP-FLI-02: filtrarLotesInsumoPorInsumo sin insumo devuelve una página vacía sin consultar")
    void filtrarLotesInsumoPorInsumo_debeRetornarPaginaVaciaSinInsumo() {
        // === EJECUCION ===
        Page<LoteInsumoResponseDTO> resultado = loteInsumoServicio.filtrarLotesInsumoPorInsumo(null, "lote", PageRequest.of(0, 8));

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verifyNoInteractions(loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-FLI-03: filtrarLotesInsumoPorInsumo normaliza la identificación (código en mayúsculas) y trata un texto en blanco como no filtrar")
    void filtrarLotesInsumoPorInsumo_debeNormalizarLaIdentificacion() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 8);
        when(loteInsumoRepository.filtrarLotesInsumoPorInsumo(eq(1L), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        loteInsumoServicio.filtrarLotesInsumoPorInsumo(1L, "  lote-a  ", pageable);
        loteInsumoServicio.filtrarLotesInsumoPorInsumo(1L, "   ", pageable);

        // === ASSERTS ===
        verify(loteInsumoRepository).filtrarLotesInsumoPorInsumo(1L, "LOTE-A", pageable);
        verify(loteInsumoRepository).filtrarLotesInsumoPorInsumo(eq(1L), isNull(), eq(pageable));
    }

    @Test
    @DisplayName("CP-FLI-04: filtrarLotesInsumoPorInsumo ignora el orden del Pageable (lo define la consulta) y conserva página y tamaño")
    void filtrarLotesInsumoPorInsumo_debeIgnorarElOrdenDelPageable() {
        // === PREPARACION DE DATOS ===
        Pageable pageableConOrden = PageRequest.of(1, 8, Sort.by(Sort.Direction.DESC, "id"));
        when(loteInsumoRepository.filtrarLotesInsumoPorInsumo(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 8), 0));

        // === EJECUCION ===
        loteInsumoServicio.filtrarLotesInsumoPorInsumo(1L, null, pageableConOrden);

        // === ASSERTS ===
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(loteInsumoRepository).filtrarLotesInsumoPorInsumo(eq(1L), isNull(), captor.capture());
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BLI-01: buscarPorId retorna el lote de insumo mapeado a DTO, con su vencimiento y su cantidad disponible")
    void buscarPorId_debeRetornarElLoteDeInsumo() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(10L, maltaEntity(1L), "LOTE-A", 10.0, 4.0);
        lote.setFechaVencimiento(LocalDate.now().minusDays(3));
        when(loteInsumoRepository.findById(10L)).thenReturn(Optional.of(lote));

        // === EJECUCION ===
        LoteInsumoResponseDTO resultado = loteInsumoServicio.buscarPorId(10L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getIdentificacionLoteProveedor()).isEqualTo("LOTE-A");
        assertThat(resultado.getCantidadDisponible()).isEqualTo(6.0);
        assertThat(resultado.isVencido()).isTrue();
    }

    @Test
    @DisplayName("CP-BLI-02: buscarPorId lanza RecursoNoEncontradoException si el lote de insumo no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // === PREPARACION DE DATOS ===
        when(loteInsumoRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> loteInsumoServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote de insumo con ID: 99");
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
