package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.InsumoSinStockResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.LoteInsumoVencidoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.OrdenCompraPendienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockInsumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MapperDashboardEncargadoDepositoTest {

    @Test
    @DisplayName("CP-MDD-01: la orden pendiente lleva su ID, la razón social de su proveedor y su fecha de entrega estimada")
    void toOrdenCompraPendienteDTO_llevaLosDatosDeLaOrden() {
        // Arrange
        OrdenCompraEntity orden = OrdenCompraEntity.builder().id(7L).fechaEntregaEstimada(LocalDate.of(2027, 8, 1))
                .versionProveedor(VersionProveedorEntity.builder().razonSocial("Malterías del Sur S.A.").build()).build();

        // Act
        OrdenCompraPendienteResponseDTO dto = MapperDashboardEncargadoDeposito.toOrdenCompraPendienteDTO(orden);

        // Assert
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getProveedor()).isEqualTo("Malterías del Sur S.A.");
        assertThat(dto.getFechaEntregaEstimada()).isEqualTo(LocalDate.of(2027, 8, 1));
    }

    @Test
    @DisplayName("CP-MDD-02: el lote vencido lleva su ID, el nombre y la unidad de su insumo, su identificación, su vencimiento y su cantidad actual")
    void toLoteInsumoVencidoDTO_llevaLosDatosDelLote() {
        // Arrange
        LoteInsumoEntity lote = LoteInsumoEntity.builder().id(10L).identificacionLoteProveedor("L-125").fechaVencimiento(LocalDate.of(2026, 10, 1)).cantidadActual(7.5)
                .insumo(MaltaEntity.builder().nombre("Malta Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).build()).build();

        // Act
        LoteInsumoVencidoResponseDTO dto = MapperDashboardEncargadoDeposito.toLoteInsumoVencidoDTO(lote);

        // Assert
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getNombreInsumo()).isEqualTo("Malta Pilsen");
        assertThat(dto.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        assertThat(dto.getIdentificacionLoteProveedor()).isEqualTo("L-125");
        assertThat(dto.getFechaVencimiento()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(dto.getCantidadActual()).isEqualTo(7.5);
    }

    @Test
    @DisplayName("CP-MDD-03: el insumo sin stock lleva su ID, su nombre y su tipo")
    void toInsumoSinStockDTO_llevaLosDatosDelInsumo() {
        // Arrange
        StockInsumoResponseDTO insumo = StockInsumoResponseDTO.builder().id(4L).nombre("Lúpulo Cascade").tipo(TipoInsumo.LUPULO).build();

        // Act
        InsumoSinStockResponseDTO dto = MapperDashboardEncargadoDeposito.toInsumoSinStockDTO(insumo);

        // Assert
        assertThat(dto.getId()).isEqualTo(4L);
        assertThat(dto.getNombre()).isEqualTo("Lúpulo Cascade");
        assertThat(dto.getTipo()).isEqualTo(TipoInsumo.LUPULO);
    }

    @Test
    @DisplayName("CP-MDD-04: una orden, un lote o un insumo nulo se mapean a nulo")
    void toDTO_conEntradaNulaDevuelveNulo() {
        // Act and Assert
        assertThat(MapperDashboardEncargadoDeposito.toOrdenCompraPendienteDTO(null)).isNull();
        assertThat(MapperDashboardEncargadoDeposito.toLoteInsumoVencidoDTO(null)).isNull();
        assertThat(MapperDashboardEncargadoDeposito.toInsumoSinStockDTO(null)).isNull();
    }
}
