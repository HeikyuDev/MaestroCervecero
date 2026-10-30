package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.CostoDirectoAdicionalEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.costo_adicional.DetalleCostoDirectoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.costo_adicional.IDetalleCostoDirectoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IConsumoInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.CostoProduccionLoteResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostoProduccionLoteServicioImplTest {

    @Mock
    private ILoteRepository loteRepository;
    @Mock
    private IEtapaLoteRepository etapaLoteRepository;
    @Mock
    private IConsumoInsumoRepository consumoInsumoRepository;
    @Mock
    private IDetalleCostoDirectoRepository detalleCostoDirectoRepository;
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;

    @InjectMocks
    private CostoProduccionLoteServicioImpl servicio;

    @Test
    @DisplayName("CP-CPL-01: calcularCostoProduccion lanza RecursoNoEncontradoException cuando el lote no existe y no consulta nada más")
    void calcularCostoProduccion_debeRechazarUnLoteInexistente() {
        // Arrange
        when(loteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> servicio.calcularCostoProduccion(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote con ID: 99");
        verifyNoInteractions(consumoInsumoRepository, detalleCostoDirectoRepository, etapaLoteRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-CPL-02: sin consumos ni costos adicionales el costo es cero, no hay nada para mostrar y no hay total")
    void calcularCostoProduccion_sinConsumos() {
        // Arrange
        prepararLote(EstadoLote.EN_EJECUCION, EstadoEtapaLote.PENDIENTE);

        // Act
        CostoProduccionLoteResponseDTO costo = servicio.calcularCostoProduccion(1L);

        // Assert
        assertThat(costo.getInsumos()).isEmpty();
        assertThat(costo.getAdicionales()).isEmpty();
        assertThat(costo.getTotalInsumos()).isEqualByComparingTo("0");
        assertThat(costo.isHayDatos()).isFalse();
        assertThat(costo.getCostoTotal()).isNull();
        assertThat(costo.getCostoPorLitro()).isNull();
    }

    @Test
    @DisplayName("CP-CPL-03: un lote en curso con consumos informa el costo acumulado de insumos (por insumo, con su costo promedio por unidad) y no el total ni el costo por litro")
    void calcularCostoProduccion_loteEnCurso() {
        // Arrange
        prepararLote(EstadoLote.EN_EJECUCION, EstadoEtapaLote.PENDIENTE);
        // Pilsen: 18 kg reservados a 5 y 2 kg directos a 5,5 = 101; Cascade: 100 g a 0,12
        when(consumoInsumoRepository.resumirCostoDeInsumosDelLote(1L)).thenReturn(List.of(
                fila(1L, "Cascade", UnidadDeMedida.GRAMO, 100.0, new BigDecimal("12.0000")),
                fila(2L, "Pilsen", UnidadDeMedida.KILOGRAMO, 20.0, new BigDecimal("101.0000"))));

        // Act
        CostoProduccionLoteResponseDTO costo = servicio.calcularCostoProduccion(1L);

        // Assert
        assertThat(costo.getInsumos()).extracting("nombre").containsExactly("Cascade", "Pilsen");
        assertThat(costo.getInsumos().get(1).getCantidadConsumida()).isEqualTo(20.0);
        assertThat(costo.getInsumos().get(1).getCostoPromedioPorUnidad()).isEqualByComparingTo("5.05");
        assertThat(costo.getInsumos().get(1).getSubtotal()).isEqualByComparingTo("101");
        assertThat(costo.getInsumos().get(0).getCostoPromedioPorUnidad()).isEqualByComparingTo("0.12");
        assertThat(costo.getTotalInsumos()).isEqualByComparingTo("113");
        assertThat(costo.isHayDatos()).isTrue();
        assertThat(costo.isEnvasadoFinalizado()).isFalse();
        assertThat(costo.getCostoTotal()).isNull();
        assertThat(costo.getCostoPorLitro()).isNull();
    }

    @Test
    @DisplayName("CP-CPL-04: con el envasado finalizado suma los costos adicionales y calcula el costo total y el costo por litro envasado")
    void calcularCostoProduccion_conEnvasadoFinalizado() {
        // Arrange
        prepararLote(EstadoLote.FINALIZADO, EstadoEtapaLote.FINALIZADA);
        when(consumoInsumoRepository.resumirCostoDeInsumosDelLote(1L)).thenReturn(List.<Object[]>of(
                fila(2L, "Pilsen", UnidadDeMedida.KILOGRAMO, 20.0, new BigDecimal("101.0000"))));
        when(detalleCostoDirectoRepository.listarDelLote(1L)).thenReturn(List.of(
                detalle("Dióxido de carbono", "2.5000", "100.0000"), detalle("Gas butano", "15.0000", "600.0000")));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(6L)).thenReturn(40.0);

        // Act
        CostoProduccionLoteResponseDTO costo = servicio.calcularCostoProduccion(1L);

        // Assert
        assertThat(costo.getAdicionales()).extracting("nombre").containsExactly("Dióxido de carbono", "Gas butano");
        assertThat(costo.getAdicionales().get(1).getCostoPorLitroAplicado()).isEqualByComparingTo("15");
        assertThat(costo.getTotalAdicionales()).isEqualByComparingTo("700");
        assertThat(costo.isEnvasadoFinalizado()).isTrue();
        assertThat(costo.getLitrosEnvasados()).isEqualTo(40.0);
        assertThat(costo.getCostoTotal()).isEqualByComparingTo("801");
        assertThat(costo.getCostoPorLitro()).isEqualByComparingTo("20.025");
    }

    @Test
    @DisplayName("CP-CPL-05: con el envasado finalizado y cero litros envasados no hay costo total ni costo por litro, pero se informan los insumos y los adicionales")
    void calcularCostoProduccion_conCeroLitros() {
        // Arrange
        prepararLote(EstadoLote.FINALIZADO, EstadoEtapaLote.FINALIZADA);
        when(consumoInsumoRepository.resumirCostoDeInsumosDelLote(1L)).thenReturn(List.<Object[]>of(
                fila(2L, "Pilsen", UnidadDeMedida.KILOGRAMO, 20.0, new BigDecimal("101.0000"))));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(6L)).thenReturn(0.0);

        // Act
        CostoProduccionLoteResponseDTO costo = servicio.calcularCostoProduccion(1L);

        // Assert
        assertThat(costo.getTotalInsumos()).isEqualByComparingTo("101");
        assertThat(costo.getLitrosEnvasados()).isEqualTo(0.0);
        assertThat(costo.getCostoTotal()).isNull();
        assertThat(costo.getCostoPorLitro()).isNull();
    }

    @Test
    @DisplayName("CP-CPL-06: el costo por litro se redondea a 4 decimales con HALF_UP y el costo promedio por unidad también")
    void calcularCostoProduccion_redondeaAHalfUp() {
        // Arrange
        prepararLote(EstadoLote.FINALIZADO, EstadoEtapaLote.FINALIZADA);
        when(consumoInsumoRepository.resumirCostoDeInsumosDelLote(1L)).thenReturn(List.<Object[]>of(
                fila(2L, "Pilsen", UnidadDeMedida.KILOGRAMO, 3.0, new BigDecimal("10.0000"))));
        when(envasadoLoteRepository.sumarCantidadEnvasadaRegistrada(6L)).thenReturn(3.0);

        // Act
        CostoProduccionLoteResponseDTO costo = servicio.calcularCostoProduccion(1L);

        // Assert
        assertThat(costo.getInsumos().get(0).getCostoPromedioPorUnidad()).isEqualByComparingTo("3.3333");
        assertThat(costo.getCostoPorLitro()).isEqualByComparingTo("3.3333");
        assertThat(costo.getCostoPorLitro().scale()).isEqualTo(4);
    }

    @Test
    @DisplayName("CP-CPL-07: un lote cancelado informa el costo de lo que alcanzó a consumir y no calcula total ni costo por litro")
    void calcularCostoProduccion_loteCancelado() {
        // Arrange
        prepararLote(EstadoLote.CANCELADO, EstadoEtapaLote.CANCELADA);
        when(consumoInsumoRepository.resumirCostoDeInsumosDelLote(1L)).thenReturn(List.<Object[]>of(
                fila(2L, "Pilsen", UnidadDeMedida.KILOGRAMO, 4.0, new BigDecimal("20.0000"))));

        // Act
        CostoProduccionLoteResponseDTO costo = servicio.calcularCostoProduccion(1L);

        // Assert
        assertThat(costo.getEstadoLote()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(costo.getTotalInsumos()).isEqualByComparingTo("20");
        assertThat(costo.isEnvasadoFinalizado()).isFalse();
        assertThat(costo.getCostoTotal()).isNull();
        assertThat(costo.getLitrosEnvasados()).isNull();
    }

    @Test
    @DisplayName("CP-CPL-08: una cantidad consumida en cero no divide por cero: el costo promedio por unidad es cero")
    void calcularCostoProduccion_cantidadEnCero() {
        // Arrange
        prepararLote(EstadoLote.EN_EJECUCION, EstadoEtapaLote.PENDIENTE);
        when(consumoInsumoRepository.resumirCostoDeInsumosDelLote(1L)).thenReturn(List.<Object[]>of(
                fila(2L, "Pilsen", UnidadDeMedida.KILOGRAMO, 0.0, new BigDecimal("0"))));

        // Act
        CostoProduccionLoteResponseDTO costo = servicio.calcularCostoProduccion(1L);

        // Assert
        assertThat(costo.getInsumos().get(0).getCostoPromedioPorUnidad()).isEqualByComparingTo("0");
    }

    // ---------- Apoyo ----------

    private void prepararLote(EstadoLote estadoLote, EstadoEtapaLote estadoEnvasado) {
        LoteEntity lote = LoteEntity.builder().id(1L).estado(estadoLote).build();
        EtapaLoteEntity envasado = EtapaLoteEntity.builder().id(6L).etapa(TipoEtapa.ENVASADO).estado(estadoEnvasado).lote(lote).build();
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote));
        lenient().when(etapaLoteRepository.buscarEtapaDeEnvasadoDelLote(1L)).thenReturn(Optional.of(envasado));
        lenient().when(consumoInsumoRepository.resumirCostoDeInsumosDelLote(1L)).thenReturn(List.of());
        lenient().when(detalleCostoDirectoRepository.listarDelLote(1L)).thenReturn(List.of());
    }

    private static Object[] fila(Long id, String nombre, UnidadDeMedida unidad, double cantidad, BigDecimal costo) {
        return new Object[]{id, nombre, unidad, cantidad, costo};
    }

    private static DetalleCostoDirectoEntity detalle(String nombre, String costoPorLitro, String subtotal) {
        return DetalleCostoDirectoEntity.builder()
                .costoPorLitroAplicado(new BigDecimal(costoPorLitro)).subtotal(new BigDecimal(subtotal))
                .costoDirectoAdicional(CostoDirectoAdicionalEntity.builder().nombre(nombre).build())
                .build();
    }
}
