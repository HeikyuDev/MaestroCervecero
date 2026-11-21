package com.github.heikyudev.maestrocervecero.util.mapper.cotizacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.InvitacionCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.OfertaCotizacionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.CatalogoProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.PresentacionComercialEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.ProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapperSolicitudCotizacionTest {

    private static CatalogoProveedorEntity item(UnidadDeMedida unidadInsumo, double cantidadPresentacion, UnidadDeMedida unidadPresentacion) {
        return CatalogoProveedorEntity.builder()
                .insumo(unidadInsumo == UnidadDeMedida.GRAMO
                        ? LupuloEntity.builder().id(2L).nombre("Cascade").unidadDeMedida(unidadInsumo).build()
                        : MaltaEntity.builder().id(1L).nombre("Pilsen").unidadDeMedida(unidadInsumo).build())
                .presentacionComercial(PresentacionComercialEntity.builder().nombre("Bolsa").cantidad(cantidadPresentacion).unidadDeMedida(unidadPresentacion).build())
                .build();
    }

    @Test
    @DisplayName("CP-MSC-01: el contenido de la presentación se convierte a la unidad del insumo (g a kg, kg a g, t a kg)")
    void contenidoPorPresentacion_convierteUnidades() {
        assertThat(MapperSolicitudCotizacion.contenidoPorPresentacion(item(UnidadDeMedida.KILOGRAMO, 500.0, UnidadDeMedida.GRAMO))).isEqualTo(0.5);
        assertThat(MapperSolicitudCotizacion.contenidoPorPresentacion(item(UnidadDeMedida.GRAMO, 1.0, UnidadDeMedida.KILOGRAMO))).isEqualTo(1000.0);
        assertThat(MapperSolicitudCotizacion.contenidoPorPresentacion(item(UnidadDeMedida.KILOGRAMO, 1.0, UnidadDeMedida.TONELADA))).isEqualTo(1000.0);
        assertThat(MapperSolicitudCotizacion.contenidoPorPresentacion(item(UnidadDeMedida.KILOGRAMO, 25.0, UnidadDeMedida.KILOGRAMO))).isEqualTo(25.0);
    }

    @Test
    @DisplayName("CP-MSC-02: el precio por unidad del insumo divide el precio de la presentación por su contenido convertido, con 4 decimales")
    void precioPorUnidadDelInsumo_convierteYRedondea() {
        // 500 g a $ 1.000 para un insumo en kg: $ 2.000/kg
        assertThat(MapperSolicitudCotizacion.precioPorUnidadDelInsumo(item(UnidadDeMedida.KILOGRAMO, 500.0, UnidadDeMedida.GRAMO), new BigDecimal("1000"))).isEqualByComparingTo("2000");
        // 1 kg a $ 9.000 para un insumo en g: $ 9/g
        assertThat(MapperSolicitudCotizacion.precioPorUnidadDelInsumo(item(UnidadDeMedida.GRAMO, 1.0, UnidadDeMedida.KILOGRAMO), new BigDecimal("9000"))).isEqualByComparingTo("9");
        // 3 kg a $ 10: 3,3333 por kg (HALF_UP a 4 decimales)
        assertThat(MapperSolicitudCotizacion.precioPorUnidadDelInsumo(item(UnidadDeMedida.KILOGRAMO, 3.0, UnidadDeMedida.KILOGRAMO), new BigDecimal("10"))).isEqualByComparingTo("3.3333");
        // Presentación sin contenido: no hay precio por unidad
        assertThat(MapperSolicitudCotizacion.precioPorUnidadDelInsumo(item(UnidadDeMedida.KILOGRAMO, 0.0, UnidadDeMedida.KILOGRAMO), BigDecimal.TEN)).isNull();
    }

    @Test
    @DisplayName("CP-MSC-03: el tiempo restante se informa en días y horas, y como vencida al llegar a la fecha límite")
    void tiempoRestante_enDiasYHoras() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 18, 10, 0);
        assertThat(MapperSolicitudCotizacion.tiempoRestante(ahora.plusDays(2).plusHours(5), ahora)).isEqualTo("2 días y 5 horas");
        assertThat(MapperSolicitudCotizacion.tiempoRestante(ahora.plusDays(1).plusHours(1), ahora)).isEqualTo("1 día y 1 hora");
        assertThat(MapperSolicitudCotizacion.tiempoRestante(ahora.plusDays(3), ahora)).isEqualTo("3 días");
        assertThat(MapperSolicitudCotizacion.tiempoRestante(ahora.plusHours(7).plusMinutes(30), ahora)).isEqualTo("7 horas");
        assertThat(MapperSolicitudCotizacion.tiempoRestante(ahora.plusMinutes(20), ahora)).isEqualTo("menos de 1 hora");
        assertThat(MapperSolicitudCotizacion.tiempoRestante(ahora, ahora)).isEqualTo("Vencida");
    }

    @Test
    @DisplayName("CP-MSC-04: la explicación indica los insumos que el stock no cubre y, mientras está pendiente de confirmación, qué falta hacer")
    void explicacion_insumosYAccion() {
        com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.SolicitudCotizacionEntity solicitud =
                com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.SolicitudCotizacionEntity.builder().id(12L)
                        .estado(com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION)
                        .planificacionProduccion(com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity.builder().id(45L).build())
                        .build();
        solicitud.getLineas().add(com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.LineaSolicitudCotizacionEntity.builder()
                .insumo(MaltaEntity.builder().id(1L).nombre("Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).build()).cantidadFaltante(120.0).build());
        solicitud.getLineas().add(com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.LineaSolicitudCotizacionEntity.builder()
                .insumo(LupuloEntity.builder().id(2L).nombre("Cascade").unidadDeMedida(UnidadDeMedida.KILOGRAMO).build()).cantidadFaltante(2.5).build());

        assertThat(MapperSolicitudCotizacion.explicacion(solicitud)).isEqualTo("Se generó la solicitud de cotización N° 12 para la planificación N° 45: "
                + "el stock actual no cubre Malta Pilsen (faltan 120 kg) ni Lúpulo Cascade (faltan 2,5 kg). Confirme el envío a los proveedores o cancele la solicitud");

        solicitud.setEstado(com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion.ABIERTA);
        assertThat(MapperSolicitudCotizacion.explicacion(solicitud)).doesNotContain("Confirme").endsWith("(faltan 2,5 kg)");
    }

    @Test
    @DisplayName("CP-MSC-05: las ofertas vigentes de una invitación son las de presentaciones que siguen en el catálogo vigente, cada una con su ítem vigente (por insumo y presentación)")
    void ofertasVigentes_resuelvenPorInsumoYPresentacion() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = MaltaEntity.builder().id(1L).nombre("Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
        LupuloEntity lupulo = LupuloEntity.builder().id(2L).nombre("Cascade").unidadDeMedida(UnidadDeMedida.GRAMO).build();
        PresentacionComercialEntity bolsa = PresentacionComercialEntity.builder().id(30L).nombre("Bolsa").cantidad(25.0).unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
        PresentacionComercialEntity balde = PresentacionComercialEntity.builder().id(31L).nombre("Balde").cantidad(5.0).unidadDeMedida(UnidadDeMedida.KILOGRAMO).build();
        ProveedorEntity proveedor = ProveedorEntity.builder().id(1L).build();
        VersionProveedorEntity anterior = VersionProveedorEntity.builder().id(10L).esUltimaVersion(false).proveedor(proveedor).build();
        VersionProveedorEntity vigente = VersionProveedorEntity.builder().id(11L).esUltimaVersion(true).proveedor(proveedor).build();
        CatalogoProveedorEntity maltaBolsaAnterior = CatalogoProveedorEntity.builder().id(100L).version(anterior).insumo(malta).presentacionComercial(bolsa).build();
        CatalogoProveedorEntity maltaBaldeAnterior = CatalogoProveedorEntity.builder().id(101L).version(anterior).insumo(malta).presentacionComercial(balde).build();
        CatalogoProveedorEntity maltaBolsaVigente = CatalogoProveedorEntity.builder().id(200L).version(vigente).insumo(malta).presentacionComercial(bolsa).build();
        // El lúpulo vigente viene en Bolsa: una oferta de malta en Bolsa no se resuelve con un ítem de otro insumo
        CatalogoProveedorEntity lupuloBolsaVigente = CatalogoProveedorEntity.builder().id(201L).version(vigente).insumo(lupulo).presentacionComercial(bolsa).build();
        anterior.getCatalogoProveedor().addAll(List.of(maltaBolsaAnterior, maltaBaldeAnterior));
        vigente.getCatalogoProveedor().addAll(List.of(lupuloBolsaVigente, maltaBolsaVigente));
        proveedor.getVersiones().addAll(List.of(anterior, vigente));
        InvitacionCotizacionEntity invitacion = InvitacionCotizacionEntity.builder().id(600L).proveedor(proveedor).build();
        OfertaCotizacionEntity ofertaBolsa = OfertaCotizacionEntity.builder().id(1L).invitacion(invitacion).catalogoProveedor(maltaBolsaAnterior).cantidadDisponible(2).build();
        OfertaCotizacionEntity ofertaBalde = OfertaCotizacionEntity.builder().id(2L).invitacion(invitacion).catalogoProveedor(maltaBaldeAnterior).cantidadDisponible(3).build();
        invitacion.getOfertas().addAll(List.of(ofertaBolsa, ofertaBalde));

        // === EJECUCION ===
        List<MapperSolicitudCotizacion.OfertaVigente> vigentes = MapperSolicitudCotizacion.ofertasVigentes(invitacion);

        // === ASSERTS ===
        assertThat(vigentes).singleElement().satisfies(ofertaVigente -> {
            assertThat(ofertaVigente.oferta()).isSameAs(ofertaBolsa);
            assertThat(ofertaVigente.itemVigente()).isSameAs(maltaBolsaVigente);
        });
        // Sin versión vigente no queda ninguna oferta
        vigente.setEsUltimaVersion(false);
        assertThat(MapperSolicitudCotizacion.ofertasVigentes(invitacion)).isEmpty();
    }
}
