package com.github.heikyudev.maestrocervecero.util.mapper.stock_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.CantidadesStockResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapperStockInsumoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 14);

    // ==================== calcularCantidades ====================

    @Test
    @DisplayName("CP-MSI-01: un lote vigente informa disponible como actual menos reservada, su reservada, vencida en 0 y total igual a la actual")
    void calcularCantidades_loteVigente() {
        CantidadesStockResponseDTO c = MapperStockInsumo.calcularCantidades(10.0, 4.0, HOY.plusDays(30), HOY);

        assertThat(c.getDisponible()).isEqualTo(6.0);
        assertThat(c.getReservada()).isEqualTo(4.0);
        assertThat(c.getVencida()).isEqualTo(0.0);
        assertThat(c.getTotal()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("CP-MSI-02: un lote que vence hoy es vigente")
    void calcularCantidades_venceHoyEsVigente() {
        CantidadesStockResponseDTO c = MapperStockInsumo.calcularCantidades(10.0, 4.0, HOY, HOY);

        assertThat(MapperStockInsumo.estaVencido(HOY, HOY)).isFalse();
        assertThat(c.getDisponible()).isEqualTo(6.0);
        assertThat(c.getReservada()).isEqualTo(4.0);
        assertThat(c.getVencida()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-MSI-03: un lote que venció ayer está vencido: toda su cantidad, también la reservada, se informa como vencida")
    void calcularCantidades_venceAyerEstaVencido() {
        CantidadesStockResponseDTO c = MapperStockInsumo.calcularCantidades(10.0, 4.0, HOY.minusDays(1), HOY);

        assertThat(MapperStockInsumo.estaVencido(HOY.minusDays(1), HOY)).isTrue();
        assertThat(c.getDisponible()).isEqualTo(0.0);
        assertThat(c.getReservada()).isEqualTo(0.0);
        assertThat(c.getVencida()).isEqualTo(10.0);
        assertThat(c.getTotal()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("CP-MSI-04: se cumple disponible + reservada + vencida = total en un lote vigente, uno vencido y uno sin stock")
    void calcularCantidades_invariante() {
        List<CantidadesStockResponseDTO> casos = List.of(
                MapperStockInsumo.calcularCantidades(10.0, 4.0, HOY.plusDays(1), HOY),
                MapperStockInsumo.calcularCantidades(10.0, 4.0, HOY.minusDays(1), HOY),
                MapperStockInsumo.calcularCantidades(0.0, 0.0, HOY.minusDays(1), HOY),
                MapperStockInsumo.calcularCantidades(2.5, 2.5, HOY, HOY));

        for (CantidadesStockResponseDTO c : casos) {
            assertThat(c.getDisponible() + c.getReservada() + c.getVencida()).isEqualTo(c.getTotal());
        }
    }

    @Test
    @DisplayName("CP-MSI-05: las cantidades se redondean y un residuo de punto flotante no cuenta como stock")
    void calcularCantidades_redondeo() {
        CantidadesStockResponseDTO c = MapperStockInsumo.calcularCantidades(0.3, 0.1 + 0.2, HOY.plusDays(1), HOY);

        assertThat(c.getDisponible()).isEqualTo(0.0);
        assertThat(c.getReservada()).isEqualTo(0.3);
        assertThat(c.getTotal()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("CP-MSI-06: una cantidad nula equivale a 0")
    void calcularCantidades_nulos() {
        CantidadesStockResponseDTO c = MapperStockInsumo.calcularCantidades(null, null, HOY.plusDays(1), HOY);

        assertThat(c.getDisponible()).isEqualTo(0.0);
        assertThat(c.getReservada()).isEqualTo(0.0);
        assertThat(c.getVencida()).isEqualTo(0.0);
        assertThat(c.getTotal()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("CP-MSI-07: calcularCantidades desde la entidad usa la cantidad actual, la reservada y el vencimiento del lote")
    void calcularCantidades_desdeEntidad() {
        LoteInsumoEntity lote = lote(1L, maltaEntity(), "L-1", 8.0, 3.0, HOY.plusDays(2));

        CantidadesStockResponseDTO c = MapperStockInsumo.calcularCantidades(lote, HOY);

        assertThat(c.getDisponible()).isEqualTo(5.0);
        assertThat(c.getReservada()).isEqualTo(3.0);
        assertThat(c.getTotal()).isEqualTo(8.0);
    }

    // ==================== sumar y desdeSumas ====================

    @Test
    @DisplayName("CP-MSI-08: sumar suma cantidad por cantidad los lotes de un insumo y mantiene el invariante")
    void sumar_variosLotes() {
        CantidadesStockResponseDTO vigente = MapperStockInsumo.calcularCantidades(10.0, 4.0, HOY.plusDays(5), HOY);
        CantidadesStockResponseDTO otroVigente = MapperStockInsumo.calcularCantidades(2.5, 0.0, HOY, HOY);
        CantidadesStockResponseDTO vencido = MapperStockInsumo.calcularCantidades(7.0, 1.0, HOY.minusDays(3), HOY);

        CantidadesStockResponseDTO suma = MapperStockInsumo.sumar(List.of(vigente, otroVigente, vencido));

        assertThat(suma.getDisponible()).isEqualTo(8.5);
        assertThat(suma.getReservada()).isEqualTo(4.0);
        assertThat(suma.getVencida()).isEqualTo(7.0);
        assertThat(suma.getTotal()).isEqualTo(19.5);
        assertThat(suma.getDisponible() + suma.getReservada() + suma.getVencida()).isEqualTo(suma.getTotal());
    }

    @Test
    @DisplayName("CP-MSI-09: sumar sin lotes (vacío o nulo) da todas las cantidades en 0")
    void sumar_sinLotes() {
        for (CantidadesStockResponseDTO suma : List.of(MapperStockInsumo.sumar(List.of()), MapperStockInsumo.sumar(null))) {
            assertThat(suma.getDisponible()).isEqualTo(0.0);
            assertThat(suma.getReservada()).isEqualTo(0.0);
            assertThat(suma.getVencida()).isEqualTo(0.0);
            assertThat(suma.getTotal()).isEqualTo(0.0);
        }
    }

    @Test
    @DisplayName("CP-MSI-10: sumar redondea el residuo de punto flotante de la suma")
    void sumar_redondeo() {
        CantidadesStockResponseDTO a = MapperStockInsumo.desdeSumas(0.1, 0.0, 0.0, 0.1);
        CantidadesStockResponseDTO b = MapperStockInsumo.desdeSumas(0.2, 0.0, 0.0, 0.2);

        CantidadesStockResponseDTO suma = MapperStockInsumo.sumar(List.of(a, b));

        assertThat(suma.getDisponible()).isEqualTo(0.3);
        assertThat(suma.getTotal()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("CP-MSI-11: desdeSumas redondea las sumas que calculó la base de datos")
    void desdeSumas_redondea() {
        CantidadesStockResponseDTO c = MapperStockInsumo.desdeSumas(0.30000000000000004, 1.00004, 2.00006, 3.0);

        assertThat(c.getDisponible()).isEqualTo(0.3);
        assertThat(c.getReservada()).isEqualTo(1.0);
        assertThat(c.getVencida()).isEqualTo(2.0001);
        assertThat(c.getTotal()).isEqualTo(3.0);
    }

    // ==================== toDTO ====================

    @Test
    @DisplayName("CP-MSI-12: toDTO de un insumo informa id, nombre, tipo, unidad y las cantidades recibidas")
    void toDTO_insumo() {
        CantidadesStockResponseDTO cantidades = MapperStockInsumo.desdeSumas(1.0, 2.0, 3.0, 6.0);

        StockInsumoResponseDTO dto = MapperStockInsumo.toDTO(maltaEntity(), cantidades);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getNombre()).isEqualTo("Malta Pilsen");
        assertThat(dto.getTipo()).isEqualTo(TipoInsumo.MALTA);
        assertThat(dto.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        assertThat(dto.getCantidades()).isSameAs(cantidades);
    }

    @Test
    @DisplayName("CP-MSI-13: toDTO de un insumo, toDTO de un lote y toDetalleDTO devuelven null si la entidad es nula")
    void toDTO_nulos() {
        assertThat(MapperStockInsumo.toDTO((InsumoEntity) null, null)).isNull();
        assertThat(MapperStockInsumo.toDTO((LoteInsumoEntity) null, HOY)).isNull();
        assertThat(MapperStockInsumo.toDetalleDTO(null, HOY)).isNull();
    }

    @Test
    @DisplayName("CP-MSI-14: toDTO de un lote informa identificación, vencimiento, si está vencido y sus cantidades")
    void toDTO_lote() {
        LoteInsumoEntity vencido = lote(5L, maltaEntity(), "L-VIEJO", 10.0, 4.0, HOY.minusDays(1));
        LoteInsumoEntity vigente = lote(6L, maltaEntity(), "L-NUEVO", 10.0, 4.0, HOY);

        StockLoteInsumoResponseDTO dtoVencido = MapperStockInsumo.toDTO(vencido, HOY);
        StockLoteInsumoResponseDTO dtoVigente = MapperStockInsumo.toDTO(vigente, HOY);

        assertThat(dtoVencido.getId()).isEqualTo(5L);
        assertThat(dtoVencido.getIdentificacionLoteProveedor()).isEqualTo("L-VIEJO");
        assertThat(dtoVencido.getFechaVencimiento()).isEqualTo(HOY.minusDays(1));
        assertThat(dtoVencido.getVencido()).isTrue();
        assertThat(dtoVencido.getCantidades().getVencida()).isEqualTo(10.0);
        assertThat(dtoVigente.getVencido()).isFalse();
        assertThat(dtoVigente.getCantidades().getDisponible()).isEqualTo(6.0);
    }

    @Test
    @DisplayName("CP-MSI-15: toDetalleDTO informa el insumo del lote junto con sus datos y cantidades")
    void toDetalleDTO() {
        LoteInsumoEntity lote = lote(5L, maltaEntity(), "L-1", 10.0, 4.0, HOY.plusDays(9));

        StockLoteInsumoDetalleResponseDTO dto = MapperStockInsumo.toDetalleDTO(lote, HOY);

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getIdInsumo()).isEqualTo(1L);
        assertThat(dto.getNombreInsumo()).isEqualTo("Malta Pilsen");
        assertThat(dto.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        assertThat(dto.getIdentificacionLoteProveedor()).isEqualTo("L-1");
        assertThat(dto.getFechaVencimiento()).isEqualTo(HOY.plusDays(9));
        assertThat(dto.getVencido()).isFalse();
        assertThat(dto.getCantidades().getDisponible()).isEqualTo(6.0);
    }

    // ==================== tipoDe ====================

    @Test
    @DisplayName("CP-MSI-16: tipoDe resuelve Malta, Lúpulo y Levadura por la clase concreta del insumo")
    void tipoDe() {
        assertThat(MapperStockInsumo.tipoDe(maltaEntity())).isEqualTo(TipoInsumo.MALTA);
        assertThat(MapperStockInsumo.tipoDe(LupuloEntity.builder().id(2L).nombre("Cascade").estado(Estado.ACTIVO).build())).isEqualTo(TipoInsumo.LUPULO);
        assertThat(MapperStockInsumo.tipoDe(LevaduraEntity.builder().id(3L).nombre("US-05").estado(Estado.ACTIVO).build())).isEqualTo(TipoInsumo.LEVADURA);
    }

    @Test
    @DisplayName("CP-MSI-17: tipoDe rechaza una clase de insumo que no es Malta, Lúpulo ni Levadura")
    void tipoDe_claseDesconocida() {
        assertThatThrownBy(() -> MapperStockInsumo.tipoDe(new InsumoEntity() { }))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static MaltaEntity maltaEntity() {
        return MaltaEntity.builder().id(1L).nombre("Malta Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).estado(Estado.ACTIVO).build();
    }

    private static LoteInsumoEntity lote(Long id, MaltaEntity insumo, String identificacion, double actual, double reservada, LocalDate vencimiento) {
        return LoteInsumoEntity.builder().id(id).insumo(insumo).identificacionLoteProveedor(identificacion)
                .cantidadActual(actual).cantidadReservada(reservada).fechaVencimiento(vencimiento).build();
    }
}
