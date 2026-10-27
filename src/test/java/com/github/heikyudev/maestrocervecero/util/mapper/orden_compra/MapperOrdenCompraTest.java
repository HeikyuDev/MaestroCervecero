package com.github.heikyudev.maestrocervecero.util.mapper.orden_compra;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.DetalleCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.orden_compra.OrdenCompraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.DetalleCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.orden_compra.OrdenCompraResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MapperOrdenCompraTest {

    @Test
    @DisplayName("CP-MOC-01: toDTO informa el subtotal de cada ítem y el total de la orden, sin las cantidades recibida y pendiente")
    void toDTO_debeInformarSubtotalesYTotalSinCantidades() {
        // === PREPARACION DE DATOS ===
        OrdenCompraEntity orden = ordenCompra();
        agregarDetalle(orden, 1L, 3, new BigDecimal("12.3456"));
        agregarDetalle(orden, 2L, 2, new BigDecimal("0.5000"));

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = MapperOrdenCompra.toDTO(orden);

        // === ASSERTS ===
        assertThat(resultado.getDetallesCompra()).extracting(DetalleCompraResponseDTO::getSubtotal)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("37.0368"), new BigDecimal("1.0000"));
        assertThat(resultado.getTotal()).isEqualByComparingTo("38.0368");
        assertThat(resultado.getDetallesCompra()).allSatisfy(detalle -> {
            assertThat(detalle.getCantidadRecibida()).isNull();
            assertThat(detalle.getCantidadPendiente()).isNull();
        });
    }

    @Test
    @DisplayName("CP-MOC-02: toDTO informa total cero cuando la orden no tiene ítems")
    void toDTO_debeInformarTotalCeroSinItems() {
        // === PREPARACION DE DATOS ===
        OrdenCompraEntity orden = ordenCompra();

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = MapperOrdenCompra.toDTO(orden);

        // === ASSERTS ===
        assertThat(resultado.getDetallesCompra()).isEmpty();
        assertThat(resultado.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("CP-MOC-03: toDTO conserva los 4 decimales del costo unitario en el subtotal y el total, sin redondear")
    void toDTO_debeConservarLosDecimalesDelCostoUnitario() {
        // === PREPARACION DE DATOS ===
        OrdenCompraEntity orden = ordenCompra();
        agregarDetalle(orden, 1L, 3, new BigDecimal("0.0001"));

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = MapperOrdenCompra.toDTO(orden);

        // === ASSERTS ===
        assertThat(resultado.getDetallesCompra().get(0).getSubtotal()).isEqualTo(new BigDecimal("0.0003"));
        assertThat(resultado.getTotal()).isEqualTo(new BigDecimal("0.0003"));
    }

    @Test
    @DisplayName("CP-MOC-04: toDTOConCantidades informa la cantidad recibida y la pendiente de cada ítem redondeadas a 4 decimales")
    void toDTOConCantidades_debeInformarRecibidaYPendienteRedondeadas() {
        // === PREPARACION DE DATOS ===
        OrdenCompraEntity orden = ordenCompra();
        agregarDetalle(orden, 1L, 2, new BigDecimal("10.0000"));

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = MapperOrdenCompra.toDTOConCantidades(orden, detalle -> 0.33333);

        // === ASSERTS ===
        DetalleCompraResponseDTO detalle = resultado.getDetallesCompra().get(0);
        assertThat(detalle.getCantidadRecibida()).isEqualTo(0.3333);
        assertThat(detalle.getCantidadPendiente()).isEqualTo(1.6667);
        assertThat(detalle.getSubtotal()).isEqualByComparingTo("20.0000");
        assertThat(resultado.getTotal()).isEqualByComparingTo("20.0000");
    }

    @Test
    @DisplayName("CP-MOC-05: toDTOConCantidades convierte la presentación comercial a la unidad del insumo al calcular la cantidad pendiente")
    void toDTOConCantidades_debeConvertirLaPresentacionALaUnidadDelInsumo() {
        // === PREPARACION DE DATOS ===
        OrdenCompraEntity orden = ordenCompra();
        agregarDetalle(orden, 1L, 5, new BigDecimal("10.0000"), 25000.0, UnidadDeMedida.GRAMO);

        // === EJECUCION ===
        OrdenCompraResponseDTO resultado = MapperOrdenCompra.toDTOConCantidades(orden, detalle -> 50.5);

        // === ASSERTS ===
        DetalleCompraResponseDTO detalle = resultado.getDetallesCompra().get(0);
        assertThat(detalle.getCantidadRecibida()).isEqualTo(50.5);
        assertThat(detalle.getCantidadPendiente()).isEqualTo(74.5);
    }

    @Test
    @DisplayName("CP-MOC-06: toDTO y toDTOConCantidades retornan null cuando la orden es nula")
    void toDTO_debeRetornarNullConOrdenNula() {
        // === EJECUCION + ASSERTS ===
        assertThat(MapperOrdenCompra.toDTO(null)).isNull();
        assertThat(MapperOrdenCompra.toDTOConCantidades(null, detalle -> 0.0)).isNull();
    }

    @Test
    @DisplayName("CP-MOC-07: toDTO informa el usuario de anulación o de finalización solo en el estado que corresponde")
    void toDTO_debeInformarElUsuarioDeCierreSegunElEstado() {
        // === PREPARACION DE DATOS ===
        OrdenCompraEntity anulada = ordenCompra();
        anulada.setEstado(EstadoSolicitud.ANULADA);
        anulada.setLastModifiedBy("gerente.compras");
        OrdenCompraEntity finalizada = ordenCompra();
        finalizada.setEstado(EstadoSolicitud.FINALIZADA);
        finalizada.setLastModifiedBy("gerente.compras");
        OrdenCompraEntity pendiente = ordenCompra();
        pendiente.setLastModifiedBy("gerente.compras");

        // === EJECUCION ===
        OrdenCompraResponseDTO dtoAnulada = MapperOrdenCompra.toDTO(anulada);
        OrdenCompraResponseDTO dtoFinalizada = MapperOrdenCompra.toDTO(finalizada);
        OrdenCompraResponseDTO dtoPendiente = MapperOrdenCompra.toDTO(pendiente);

        // === ASSERTS ===
        assertThat(dtoAnulada.getUsuarioAnulacion()).isEqualTo("gerente.compras");
        assertThat(dtoAnulada.getUsuarioFinalizacion()).isNull();
        assertThat(dtoFinalizada.getUsuarioFinalizacion()).isEqualTo("gerente.compras");
        assertThat(dtoFinalizada.getUsuarioAnulacion()).isNull();
        assertThat(dtoPendiente.getUsuarioAnulacion()).isNull();
        assertThat(dtoPendiente.getUsuarioFinalizacion()).isNull();
    }

    private static OrdenCompraEntity ordenCompra() {
        return OrdenCompraEntity.builder().id(1L).estado(EstadoSolicitud.PENDIENTE).build();
    }

    private static void agregarDetalle(OrdenCompraEntity orden, Long id, int cantidad, BigDecimal costoUnitario) {
        agregarDetalle(orden, id, cantidad, costoUnitario, 1.0, UnidadDeMedida.KILOGRAMO);
    }

    /**
     * Agrega un ítem de un insumo que se mide en KILOGRAMO, con una presentación comercial del contenido indicado.
     */
    private static void agregarDetalle(OrdenCompraEntity orden, Long id, int cantidad, BigDecimal costoUnitario,
                                       double cantidadPresentacion, UnidadDeMedida unidadPresentacion) {
        MaltaEntity insumo = MaltaEntity.builder().id(1L).nombre("Malta Pilsen").estado(Estado.ACTIVO).unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
        PresentacionComercialEntity presentacion = PresentacionComercialEntity.builder()
                .id(1L).nombre("Presentación de prueba").cantidad(cantidadPresentacion).unidadDeMedida(unidadPresentacion).estado(Estado.ACTIVO).build();
        CatalogoProveedorEntity catalogo = CatalogoProveedorEntity.builder().id(id).insumo(insumo).presentacionComercial(presentacion).build();
        orden.getDetallesCompra().add(DetalleCompraEntity.builder()
                .id(id).cantidad(cantidad).costoUnitario(costoUnitario).ordenCompra(orden).catalogoProveedor(catalogo).build());
    }
}
