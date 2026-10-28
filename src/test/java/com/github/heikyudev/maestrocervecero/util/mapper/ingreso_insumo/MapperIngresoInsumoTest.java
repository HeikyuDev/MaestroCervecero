package com.github.heikyudev.maestrocervecero.util.mapper.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.IngresoInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoIngreso;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.IngresoInsumoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MapperIngresoInsumoTest {

    @Test
    @DisplayName("CP-MII-01: toDTO informa el usuario de la anulación de un ingreso anulado")
    void toDTO_debeInformarElUsuarioDeAnulacionDeUnIngresoAnulado() {
        // === PREPARACION DE DATOS ===
        IngresoInsumoEntity ingreso = ingreso(EstadoTransaccion.ANULADO, TipoIngreso.DIRECTO);
        ingreso.setFechaAnulacion(LocalDateTime.of(2027, 7, 2, 9, 30));
        ingreso.setMotivoAnulacion("Error de carga");
        ingreso.setLastModifiedBy("operario");

        // === EJECUCION ===
        IngresoInsumoResponseDTO resultado = MapperIngresoInsumo.toDTO(ingreso);

        // === ASSERTS ===
        assertThat(resultado.getFechaAnulacion()).isEqualTo(LocalDateTime.of(2027, 7, 2, 9, 30));
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("Error de carga");
        assertThat(resultado.getUsuarioAnulacion()).isEqualTo("operario");
    }

    @Test
    @DisplayName("CP-MII-02: toDTO no informa usuario de anulación en un ingreso registrado aunque tenga un último modificador")
    void toDTO_noDebeInformarUsuarioDeAnulacionEnUnIngresoRegistrado() {
        // === PREPARACION DE DATOS ===
        IngresoInsumoEntity ingreso = ingreso(EstadoTransaccion.REGISTRADO, TipoIngreso.DIRECTO);
        ingreso.setLastModifiedBy("operario");

        // === EJECUCION ===
        IngresoInsumoResponseDTO resultado = MapperIngresoInsumo.toDTO(ingreso);

        // === ASSERTS ===
        assertThat(resultado.getUsuarioAnulacion()).isNull();
    }

    @Test
    @DisplayName("CP-MII-03: toDTO informa el ID de la orden de compra de un ingreso por compra")
    void toDTO_debeInformarElIdDeLaOrdenDeCompraDeUnIngresoPorCompra() {
        // === PREPARACION DE DATOS ===
        IngresoInsumoEntity ingreso = ingreso(EstadoTransaccion.REGISTRADO, TipoIngreso.COMPRA);
        ingreso.setDetalleCompra(DetalleCompraEntity.builder().id(3L).ordenCompra(OrdenCompraEntity.builder().id(7L).build()).build());

        // === EJECUCION ===
        IngresoInsumoResponseDTO resultado = MapperIngresoInsumo.toDTO(ingreso);

        // === ASSERTS ===
        assertThat(resultado.getIdOrdenCompra()).isEqualTo(7L);
    }

    @Test
    @DisplayName("CP-MII-04: toDTO no informa ID de orden de compra en un ingreso directo")
    void toDTO_noDebeInformarIdDeOrdenDeCompraEnUnIngresoDirecto() {
        // === PREPARACION DE DATOS ===
        IngresoInsumoEntity ingreso = ingreso(EstadoTransaccion.REGISTRADO, TipoIngreso.DIRECTO);

        // === EJECUCION ===
        IngresoInsumoResponseDTO resultado = MapperIngresoInsumo.toDTO(ingreso);

        // === ASSERTS ===
        assertThat(resultado.getIdOrdenCompra()).isNull();
        assertThat(resultado.getDetalleCompra()).isNull();
    }

    @Test
    @DisplayName("CP-MII-05: toDTO informa a cuántas presentaciones equivale la cantidad recibida de un ingreso por compra: 50 kg de bolsas de 25 kg son 2 bolsas")
    void toDTO_debeInformarLasPresentacionesDeUnIngresoPorCompra() {
        // === PREPARACION DE DATOS ===
        IngresoInsumoEntity ingreso = ingreso(EstadoTransaccion.REGISTRADO, TipoIngreso.COMPRA);
        ingreso.setCantidadRecibida(50.0);
        ingreso.setDetalleCompra(detalleConPresentacion(25.0, UnidadDeMedida.KILOGRAMO));

        // === EJECUCION ===
        IngresoInsumoResponseDTO resultado = MapperIngresoInsumo.toDTO(ingreso);

        // === ASSERTS ===
        assertThat(resultado.getCantidadRecibida()).isEqualTo(50.0);
        assertThat(resultado.getCantidadPresentaciones()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("CP-MII-06: toDTO convierte la presentación a la unidad del insumo al informar las presentaciones: 50 kg de bolsas de 25000 g son 2 bolsas")
    void toDTO_debeConvertirLaUnidadDeLaPresentacion() {
        // === PREPARACION DE DATOS ===
        IngresoInsumoEntity ingreso = ingreso(EstadoTransaccion.REGISTRADO, TipoIngreso.COMPRA);
        ingreso.setCantidadRecibida(50.0);
        ingreso.setDetalleCompra(detalleConPresentacion(25000.0, UnidadDeMedida.GRAMO));

        // === EJECUCION ===
        IngresoInsumoResponseDTO resultado = MapperIngresoInsumo.toDTO(ingreso);

        // === ASSERTS ===
        assertThat(resultado.getCantidadPresentaciones()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("CP-MII-07: toDTO no informa presentaciones en un ingreso directo ni en uno por compra cuyo ítem no trae la presentación")
    void toDTO_noDebeInformarPresentacionesSinPresentacion() {
        // === PREPARACION DE DATOS ===
        IngresoInsumoEntity directo = ingreso(EstadoTransaccion.REGISTRADO, TipoIngreso.DIRECTO);
        IngresoInsumoEntity compraSinCatalogo = ingreso(EstadoTransaccion.REGISTRADO, TipoIngreso.COMPRA);
        compraSinCatalogo.setDetalleCompra(DetalleCompraEntity.builder().id(3L).ordenCompra(OrdenCompraEntity.builder().id(7L).build()).build());

        // === EJECUCION + ASSERTS ===
        assertThat(MapperIngresoInsumo.toDTO(directo).getCantidadPresentaciones()).isNull();
        assertThat(MapperIngresoInsumo.toDTO(compraSinCatalogo).getCantidadPresentaciones()).isNull();
    }

    private static DetalleCompraEntity detalleConPresentacion(double contenido, UnidadDeMedida unidad) {
        InsumoEntity insumo = MaltaEntity.builder().id(1L).nombre("Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
        PresentacionComercialEntity presentacion = PresentacionComercialEntity.builder().id(1L).nombre("Bolsa").cantidad(contenido).unidadDeMedida(unidad).build();
        return DetalleCompraEntity.builder().id(3L).cantidad(4).costoUnitario(new BigDecimal("12000"))
                .ordenCompra(OrdenCompraEntity.builder().id(7L).build())
                .catalogoProveedor(CatalogoProveedorEntity.builder().id(1L).insumo(insumo).presentacionComercial(presentacion).build())
                .build();
    }

    private static IngresoInsumoEntity ingreso(EstadoTransaccion estado, TipoIngreso tipoIngreso) {
        return IngresoInsumoEntity.builder()
                .id(1L)
                .cantidadRecibida(20.0)
                .tipoIngreso(tipoIngreso)
                .estado(estado)
                .build();
    }
}
