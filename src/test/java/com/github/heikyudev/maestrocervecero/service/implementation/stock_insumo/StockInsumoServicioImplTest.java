package com.github.heikyudev.maestrocervecero.service.implementation.stock_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ConsumoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoReservaInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.ReservaInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.TipoConsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.SituacionStockInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IConsumoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IReservaInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockInsumoServicioImplTest {

    @Mock
    private IInsumoRepository insumoRepository;

    @Mock
    private ILoteInsumoRepository loteInsumoRepository;

    @Mock
    private IReservaInsumoRepository reservaInsumoRepository;

    @Mock
    private IConsumoInsumoRepository consumoInsumoRepository;

    @InjectMocks
    private StockInsumoServicioImpl servicio;

    // ==================== filtrarInsumos ====================

    @Test
    @DisplayName("CP-SSI-01: filtrarInsumos normaliza el nombre, escapa los comodines de LIKE, traduce el tipo a su clase y pasa la fecha de hoy")
    void filtrarInsumos_pasaLosCriterios() {
        when(insumoRepository.filtrarInsumosParaStock(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio.filtrarInsumos("   Pil_sen   100% ", TipoInsumo.MALTA, null, PageRequest.of(0, 20));

        ArgumentCaptor<LocalDate> hoy = ArgumentCaptor.forClass(LocalDate.class);
        verify(insumoRepository).filtrarInsumosParaStock(eq("Pil!_sen 100!%"), eq(MaltaEntity.class), eq(false), eq(false), eq(false), hoy.capture(), any());
        assertThat(hoy.getValue()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("CP-SSI-02: filtrarInsumos sin criterios (nombre nulo o en blanco, tipo y situación nulos) no restringe por ninguno")
    void filtrarInsumos_sinCriterios() {
        when(insumoRepository.filtrarInsumosParaStock(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio.filtrarInsumos(null, null, null, PageRequest.of(0, 20));
        servicio.filtrarInsumos("    ", null, null, PageRequest.of(0, 20));

        verify(insumoRepository, org.mockito.Mockito.times(2))
                .filtrarInsumosParaStock(isNull(), isNull(), eq(false), eq(false), eq(false), any(), any());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "CON_STOCK_DISPONIBLE, true, false, false",
            "SIN_STOCK_DISPONIBLE, false, true, false",
            "CON_VENCIDOS, false, false, true"
    })
    @DisplayName("CP-SSI-03: filtrarInsumos traduce cada situación de stock a su bandera y las demás quedan apagadas")
    void filtrarInsumos_situacion(SituacionStockInsumo situacion, boolean conDisponible, boolean sinDisponible, boolean conVencidos) {
        when(insumoRepository.filtrarInsumosParaStock(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        servicio.filtrarInsumos(null, null, situacion, PageRequest.of(0, 20));

        verify(insumoRepository).filtrarInsumosParaStock(isNull(), isNull(), eq(conDisponible), eq(sinDisponible), eq(conVencidos), any(), any());
    }

    @Test
    @DisplayName("CP-SSI-04: filtrarInsumos suma el stock de los insumos de la página con una sola consulta y a los insumos sin lotes les da todo en 0")
    void filtrarInsumos_cantidades() {
        MaltaEntity conLotes = malta(1L, "Malta Pilsen");
        LupuloEntity sinLotes = lupulo(2L, "Cascade");
        when(insumoRepository.filtrarInsumosParaStock(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(conLotes, sinLotes), PageRequest.of(0, 20), 2));
        List<Object[]> sumas = new java.util.ArrayList<>();
        sumas.add(new Object[]{1L, 6.0, 4.0, 7.0, 17.0});
        when(loteInsumoRepository.sumarStockPorInsumo(eq(List.of(1L, 2L)), any())).thenReturn(sumas);

        Page<StockInsumoResponseDTO> resultado = servicio.filtrarInsumos(null, null, null, PageRequest.of(0, 20));

        assertThat(resultado.getContent()).hasSize(2);
        StockInsumoResponseDTO primero = resultado.getContent().get(0);
        assertThat(primero.getNombre()).isEqualTo("Malta Pilsen");
        assertThat(primero.getTipo()).isEqualTo(TipoInsumo.MALTA);
        assertThat(primero.getCantidades().getDisponible()).isEqualTo(6.0);
        assertThat(primero.getCantidades().getReservada()).isEqualTo(4.0);
        assertThat(primero.getCantidades().getVencida()).isEqualTo(7.0);
        assertThat(primero.getCantidades().getTotal()).isEqualTo(17.0);
        StockInsumoResponseDTO segundo = resultado.getContent().get(1);
        assertThat(segundo.getTipo()).isEqualTo(TipoInsumo.LUPULO);
        assertThat(segundo.getCantidades().getDisponible()).isEqualTo(0.0);
        assertThat(segundo.getCantidades().getReservada()).isEqualTo(0.0);
        assertThat(segundo.getCantidades().getVencida()).isEqualTo(0.0);
        assertThat(segundo.getCantidades().getTotal()).isEqualTo(0.0);
        verify(loteInsumoRepository).sumarStockPorInsumo(eq(List.of(1L, 2L)), eq(LocalDate.now()));
    }

    @Test
    @DisplayName("CP-SSI-05: filtrarInsumos con una página vacía no consulta las sumas de stock")
    void filtrarInsumos_paginaVacia() {
        when(insumoRepository.filtrarInsumosParaStock(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        Page<StockInsumoResponseDTO> resultado = servicio.filtrarInsumos("Inexistente", null, null, PageRequest.of(0, 20));

        assertThat(resultado.getContent()).isEmpty();
        verifyNoInteractions(loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-SSI-06: filtrarInsumos ignora el orden del Pageable y pasa la misma página y tamaño")
    void filtrarInsumos_ignoraElOrden() {
        when(insumoRepository.filtrarInsumosParaStock(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 8), 0));

        servicio.filtrarInsumos(null, null, null, PageRequest.of(1, 8, Sort.by(Sort.Direction.DESC, "id")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(insumoRepository).filtrarInsumosParaStock(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName("CP-SSI-07: filtrarInsumos con una página más allá de la última repite la consulta con la última página")
    void filtrarInsumos_paginaFueraDeRango() {
        MaltaEntity malta = malta(1L, "Malta Pilsen");
        when(insumoRepository.filtrarInsumosParaStock(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), argThat(p -> p != null && p.getPageNumber() == 5)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(5, 20), 30));
        when(insumoRepository.filtrarInsumosParaStock(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), argThat(p -> p != null && p.getPageNumber() == 1)))
                .thenReturn(new PageImpl<>(List.of(malta), PageRequest.of(1, 20), 30));
        when(loteInsumoRepository.sumarStockPorInsumo(any(), any())).thenReturn(List.of());

        Page<StockInsumoResponseDTO> resultado = servicio.filtrarInsumos(null, null, null, PageRequest.of(5, 20));

        assertThat(resultado.getNumber()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
    }

    // ==================== obtenerStockInsumo ====================

    @Test
    @DisplayName("CP-SSI-08: obtenerStockInsumo devuelve el insumo con sus cantidades sumadas")
    void obtenerStockInsumo_ok() {
        MaltaEntity malta = malta(1L, "Malta Pilsen");
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(malta));
        List<Object[]> sumas = new java.util.ArrayList<>();
        sumas.add(new Object[]{1L, 6.0, 4.0, 0.0, 10.0});
        when(loteInsumoRepository.sumarStockPorInsumo(eq(List.of(1L)), eq(LocalDate.now()))).thenReturn(sumas);

        StockInsumoResponseDTO resultado = servicio.obtenerStockInsumo(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        assertThat(resultado.getCantidades().getDisponible()).isEqualTo(6.0);
        assertThat(resultado.getCantidades().getTotal()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("CP-SSI-09: obtenerStockInsumo de un insumo sin lotes devuelve todas las cantidades en 0")
    void obtenerStockInsumo_sinLotes() {
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(malta(1L, "Malta Pilsen")));
        when(loteInsumoRepository.sumarStockPorInsumo(any(), any())).thenReturn(List.of());

        StockInsumoResponseDTO resultado = servicio.obtenerStockInsumo(1L);

        assertThat(resultado.getCantidades().getDisponible()).isEqualTo(0.0);
        assertThat(resultado.getCantidades().getTotal()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-SSI-10: obtenerStockInsumo de un insumo inexistente o dado de baja lanza RecursoNoEncontradoException")
    void obtenerStockInsumo_noEncontrado() {
        when(insumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerStockInsumo(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el insumo con ID: 99");
        verifyNoInteractions(loteInsumoRepository);
    }

    // ==================== filtrarLotesDeInsumo ====================

    @Test
    @DisplayName("CP-SSI-11: filtrarLotesDeInsumo conserva el orden de la consulta, calcula las cantidades de cada lote y pasa un Pageable sin orden")
    void filtrarLotesDeInsumo_ok() {
        MaltaEntity malta = malta(1L, "Malta Pilsen");
        LoteInsumoEntity vencido = lote(10L, malta, "L-VIEJO", 7.0, 2.0, LocalDate.now().minusDays(1));
        LoteInsumoEntity vigente = lote(11L, malta, "L-NUEVO", 10.0, 4.0, LocalDate.now());
        LoteInsumoEntity sinStock = lote(12L, malta, "L-VACIO", 0.0, 0.0, LocalDate.now().plusDays(30));
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(malta));
        when(loteInsumoRepository.filtrarLotesDeInsumo(eq(1L), any())).thenReturn(new PageImpl<>(List.of(vencido, vigente, sinStock)));

        Page<StockLoteInsumoResponseDTO> resultado = servicio.filtrarLotesDeInsumo(1L, PageRequest.of(2, 8, Sort.by("id")));

        assertThat(resultado.getContent()).extracting(StockLoteInsumoResponseDTO::getIdentificacionLoteProveedor).containsExactly("L-VIEJO", "L-NUEVO", "L-VACIO");
        assertThat(resultado.getContent().get(0).getVencido()).isTrue();
        assertThat(resultado.getContent().get(0).getCantidades().getVencida()).isEqualTo(7.0);
        assertThat(resultado.getContent().get(1).getVencido()).isFalse();
        assertThat(resultado.getContent().get(1).getCantidades().getDisponible()).isEqualTo(6.0);
        assertThat(resultado.getContent().get(2).getCantidades().getTotal()).isEqualTo(0.0);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(loteInsumoRepository, org.mockito.Mockito.atLeastOnce()).filtrarLotesDeInsumo(eq(1L), captor.capture());
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName("CP-SSI-12: filtrarLotesDeInsumo de un insumo inexistente o dado de baja lanza RecursoNoEncontradoException y no consulta los lotes")
    void filtrarLotesDeInsumo_noEncontrado() {
        when(insumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.filtrarLotesDeInsumo(99L, PageRequest.of(0, 20)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el insumo con ID: 99");
        verifyNoInteractions(loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-SSI-13: filtrarLotesDeInsumo con una página más allá de la última repite la consulta con la última página")
    void filtrarLotesDeInsumo_paginaFueraDeRango() {
        MaltaEntity malta = malta(1L, "Malta Pilsen");
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(malta));
        when(loteInsumoRepository.filtrarLotesDeInsumo(eq(1L), argThat(p -> p != null && p.getPageNumber() == 4)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(4, 20), 25));
        when(loteInsumoRepository.filtrarLotesDeInsumo(eq(1L), argThat(p -> p != null && p.getPageNumber() == 1)))
                .thenReturn(new PageImpl<>(List.of(lote(10L, malta, "L-1", 1.0, 0.0, LocalDate.now())), PageRequest.of(1, 20), 25));

        Page<StockLoteInsumoResponseDTO> resultado = servicio.filtrarLotesDeInsumo(1L, PageRequest.of(4, 20));

        assertThat(resultado.getNumber()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
    }

    // ==================== obtenerDetalleLoteInsumo ====================

    @Test
    @DisplayName("CP-SSI-14: obtenerDetalleLoteInsumo devuelve la cabecera del lote con su insumo y sus cantidades")
    void obtenerDetalleLoteInsumo_ok() {
        MaltaEntity malta = malta(1L, "Malta Pilsen");
        when(loteInsumoRepository.buscarConInsumoActivoPorId(10L)).thenReturn(Optional.of(lote(10L, malta, "L-1", 10.0, 4.0, LocalDate.now().plusDays(3))));

        StockLoteInsumoDetalleResponseDTO resultado = servicio.obtenerDetalleLoteInsumo(10L);

        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getIdInsumo()).isEqualTo(1L);
        assertThat(resultado.getNombreInsumo()).isEqualTo("Malta Pilsen");
        assertThat(resultado.getCantidades().getDisponible()).isEqualTo(6.0);
        assertThat(resultado.getCantidades().getReservada()).isEqualTo(4.0);
        assertThat(resultado.getVencido()).isFalse();
    }

    @Test
    @DisplayName("CP-SSI-15: obtenerDetalleLoteInsumo de un lote inexistente o de un insumo dado de baja lanza RecursoNoEncontradoException")
    void obtenerDetalleLoteInsumo_noEncontrado() {
        when(loteInsumoRepository.buscarConInsumoActivoPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerDetalleLoteInsumo(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote de insumo con ID: 99");
    }

    // ==================== filtrarReservasDeLoteInsumo ====================

    @Test
    @DisplayName("CP-SSI-16: filtrarReservasDeLoteInsumo devuelve las reservas del lote en el orden de la consulta, mapeadas a DTO")
    void filtrarReservasDeLoteInsumo_ok() {
        MaltaEntity malta = malta(1L, "Malta Pilsen");
        LoteInsumoEntity lote = lote(10L, malta, "L-1", 10.0, 4.0, LocalDate.now().plusDays(3));
        when(loteInsumoRepository.buscarConInsumoActivoPorId(10L)).thenReturn(Optional.of(lote));
        ReservaInsumoEntity segunda = reserva(21L, lote, 3.0);
        ReservaInsumoEntity primera = reserva(20L, lote, 1.0);
        when(reservaInsumoRepository.filtrarPorLoteInsumo(eq(10L), any())).thenReturn(new PageImpl<>(List.of(segunda, primera)));

        Page<ReservaInsumoResponseDTO> resultado = servicio.filtrarReservasDeLoteInsumo(10L, PageRequest.of(0, 20, Sort.by("id")));

        assertThat(resultado.getContent()).extracting(ReservaInsumoResponseDTO::getId).containsExactly(21L, 20L);
        assertThat(resultado.getContent().get(0).getCantidadReservada()).isEqualTo(3.0);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoReservaInsumo.ACTIVA);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reservaInsumoRepository).filtrarPorLoteInsumo(eq(10L), captor.capture());
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName("CP-SSI-17: filtrarReservasDeLoteInsumo de un lote inexistente o de un insumo dado de baja lanza RecursoNoEncontradoException y no consulta las reservas")
    void filtrarReservasDeLoteInsumo_noEncontrado() {
        when(loteInsumoRepository.buscarConInsumoActivoPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.filtrarReservasDeLoteInsumo(99L, PageRequest.of(0, 20)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote de insumo con ID: 99");
        verify(reservaInsumoRepository, never()).filtrarPorLoteInsumo(any(), any());
    }

    // ==================== filtrarConsumosDeLoteInsumo ====================

    @Test
    @DisplayName("CP-SSI-18: filtrarConsumosDeLoteInsumo devuelve los consumos del lote en el orden de la consulta, mapeados a DTO")
    void filtrarConsumosDeLoteInsumo_ok() {
        MaltaEntity malta = malta(1L, "Malta Pilsen");
        LoteInsumoEntity lote = lote(10L, malta, "L-1", 10.0, 4.0, LocalDate.now().plusDays(3));
        when(loteInsumoRepository.buscarConInsumoActivoPorId(10L)).thenReturn(Optional.of(lote));
        ConsumoInsumoEntity ultimo = consumo(31L, lote, 2.0, TipoConsumo.DIRECTO, EstadoTransaccion.REGISTRADO);
        ConsumoInsumoEntity anterior = consumo(30L, lote, 1.0, TipoConsumo.RESERVADO, EstadoTransaccion.ANULADO);
        when(consumoInsumoRepository.filtrarPorLoteInsumo(eq(10L), any())).thenReturn(new PageImpl<>(List.of(ultimo, anterior)));

        Page<ConsumoInsumoResponseDTO> resultado = servicio.filtrarConsumosDeLoteInsumo(10L, PageRequest.of(0, 20, Sort.by("id")));

        assertThat(resultado.getContent()).extracting(ConsumoInsumoResponseDTO::getId).containsExactly(31L, 30L);
        assertThat(resultado.getContent().get(0).getTipoConsumo()).isEqualTo(TipoConsumo.DIRECTO);
        assertThat(resultado.getContent().get(1).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(consumoInsumoRepository).filtrarPorLoteInsumo(eq(10L), captor.capture());
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName("CP-SSI-19: filtrarConsumosDeLoteInsumo de un lote inexistente o de un insumo dado de baja lanza RecursoNoEncontradoException y no consulta los consumos")
    void filtrarConsumosDeLoteInsumo_noEncontrado() {
        when(loteInsumoRepository.buscarConInsumoActivoPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.filtrarConsumosDeLoteInsumo(99L, PageRequest.of(0, 20)))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote de insumo con ID: 99");
        verify(consumoInsumoRepository, never()).filtrarPorLoteInsumo(any(), any());
    }

    @Test
    @DisplayName("CP-SSI-20: filtrarReservasDeLoteInsumo y filtrarConsumosDeLoteInsumo con una página más allá de la última repiten la consulta con la última página")
    void reservasYConsumos_paginaFueraDeRango() {
        MaltaEntity malta = malta(1L, "Malta Pilsen");
        LoteInsumoEntity lote = lote(10L, malta, "L-1", 10.0, 4.0, LocalDate.now().plusDays(3));
        when(loteInsumoRepository.buscarConInsumoActivoPorId(10L)).thenReturn(Optional.of(lote));
        when(reservaInsumoRepository.filtrarPorLoteInsumo(eq(10L), argThat(p -> p != null && p.getPageNumber() == 9)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(9, 20), 21));
        when(reservaInsumoRepository.filtrarPorLoteInsumo(eq(10L), argThat(p -> p != null && p.getPageNumber() == 1)))
                .thenReturn(new PageImpl<>(List.of(reserva(20L, lote, 1.0)), PageRequest.of(1, 20), 21));
        when(consumoInsumoRepository.filtrarPorLoteInsumo(eq(10L), argThat(p -> p != null && p.getPageNumber() == 9)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(9, 20), 21));
        when(consumoInsumoRepository.filtrarPorLoteInsumo(eq(10L), argThat(p -> p != null && p.getPageNumber() == 1)))
                .thenReturn(new PageImpl<>(List.of(consumo(30L, lote, 1.0, TipoConsumo.DIRECTO, EstadoTransaccion.REGISTRADO)), PageRequest.of(1, 20), 21));

        assertThat(servicio.filtrarReservasDeLoteInsumo(10L, PageRequest.of(9, 20)).getNumber()).isEqualTo(1);
        assertThat(servicio.filtrarConsumosDeLoteInsumo(10L, PageRequest.of(9, 20)).getNumber()).isEqualTo(1);
    }

    // ==================== apoyo ====================

    private static boolean anyBoolean() {
        return org.mockito.ArgumentMatchers.anyBoolean();
    }

    private static MaltaEntity malta(Long id, String nombre) {
        return MaltaEntity.builder().id(id).nombre(nombre).unidadDeMedida(UnidadDeMedida.KILOGRAMO).estado(Estado.ACTIVO).build();
    }

    private static LupuloEntity lupulo(Long id, String nombre) {
        return LupuloEntity.builder().id(id).nombre(nombre).unidadDeMedida(UnidadDeMedida.GRAMO).estado(Estado.ACTIVO).build();
    }

    private static LoteInsumoEntity lote(Long id, InsumoEntity insumo, String identificacion, double actual, double reservada, LocalDate vencimiento) {
        return LoteInsumoEntity.builder().id(id).insumo(insumo).identificacionLoteProveedor(identificacion)
                .cantidadActual(actual).cantidadReservada(reservada).fechaVencimiento(vencimiento).build();
    }

    private static ReservaInsumoEntity reserva(Long id, LoteInsumoEntity lote, double cantidad) {
        return ReservaInsumoEntity.builder().id(id).loteInsumo(lote).cantidadReservada(cantidad).cantidadPendiente(cantidad).build();
    }

    private static ConsumoInsumoEntity consumo(Long id, LoteInsumoEntity lote, double cantidad, TipoConsumo tipo, EstadoTransaccion estado) {
        return ConsumoInsumoEntity.builder().id(id).loteInsumo(lote).cantidadConsumida(cantidad).tipoConsumo(tipo).estado(estado)
                .costoUnitarioPPP(java.math.BigDecimal.ONE).build();
    }
}
