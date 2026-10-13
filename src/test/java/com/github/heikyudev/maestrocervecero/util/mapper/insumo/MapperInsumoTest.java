package com.github.heikyudev.maestrocervecero.util.mapper.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class MapperInsumoTest {

    // ==================== toDTO ====================

    @Test
    @DisplayName("CP-MI-01: toDTO retorna null cuando el insumo es nulo")
    void toDTO_debeRetornarNullSiElInsumoEsNulo() {
        assertThat(MapperInsumo.toDTO(null)).isNull();
    }

    @Test
    @DisplayName("CP-MI-02: toDTO mapea una malta a MaltaResponseDTO")
    void toDTO_debeMapearUnaMalta() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = crearMalta(1L);

        // === EJECUCION ===
        InsumoResponseDTO resultado = MapperInsumo.toDTO(malta);

        // === ASSERTS ===
        assertThat(resultado).isInstanceOf(MaltaResponseDTO.class);
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Malta Pilsen");
    }

    @Test
    @DisplayName("CP-MI-03: toDTO mapea un lúpulo a LupuloResponseDTO")
    void toDTO_debeMapearUnLupulo() {
        // === PREPARACION DE DATOS ===
        LupuloEntity lupulo = LupuloEntity.builder().id(2L).nombre("Cascade").unidadDeMedida(UnidadDeMedida.GRAMO)
                .estado(Estado.ACTIVO).aa(5.5).formato(FormatoLupulo.PELLET).build();

        // === EJECUCION ===
        InsumoResponseDTO resultado = MapperInsumo.toDTO(lupulo);

        // === ASSERTS ===
        assertThat(resultado).isInstanceOf(LupuloResponseDTO.class);
        assertThat(resultado.getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("CP-MI-04: toDTO mapea una levadura a LevaduraResponseDTO")
    void toDTO_debeMapearUnaLevadura() {
        // === PREPARACION DE DATOS ===
        LevaduraEntity levadura = LevaduraEntity.builder().id(3L).nombre("Levadura Ale").unidadDeMedida(UnidadDeMedida.GRAMO)
                .estado(Estado.ACTIVO).tipo(TipoLevadura.ALE).cantidadCelulasPorGramo(10_000_000_000.0).build();

        // === EJECUCION ===
        InsumoResponseDTO resultado = MapperInsumo.toDTO(levadura);

        // === ASSERTS ===
        assertThat(resultado).isInstanceOf(LevaduraResponseDTO.class);
        assertThat(resultado.getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("CP-MI-05: toDTO desenvuelve el proxy de Hibernate de la clase abstracta InsumoEntity y mapea el insumo concreto")
    void toDTO_debeDesenvolverUnProxyDeHibernate() {
        // === PREPARACION DE DATOS ===
        MaltaEntity maltaReal = crearMalta(1L);
        InsumoEntity proxy = mock(InsumoEntity.class, withSettings().extraInterfaces(HibernateProxy.class));
        HibernateProxy proxyHibernate = (HibernateProxy) proxy;
        LazyInitializer inicializador = mock(LazyInitializer.class);
        when(proxyHibernate.asHibernateProxy()).thenReturn(proxyHibernate);
        when(proxyHibernate.getHibernateLazyInitializer()).thenReturn(inicializador);
        when(inicializador.getImplementation()).thenReturn(maltaReal);

        // === EJECUCION ===
        InsumoResponseDTO resultado = MapperInsumo.toDTO(proxy);

        // === ASSERTS ===
        assertThat(proxy).isNotInstanceOf(MaltaEntity.class);
        assertThat(resultado).isInstanceOf(MaltaResponseDTO.class);
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Malta Pilsen");
    }

    @Test
    @DisplayName("CP-MI-06: toDTO lanza IllegalStateException si el tipo concreto del insumo no está soportado")
    void toDTO_debeLanzarExcepcionSiElTipoNoEstaSoportado() {
        // === PREPARACION DE DATOS ===
        InsumoEntity insumoDeOtroTipo = new InsumoEntity() {
        };

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MapperInsumo.toDTO(insumoDeOtroTipo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Tipo de insumo no soportado");
    }

    // ==================== helpers de construcción ====================

    private static MaltaEntity crearMalta(Long id) {
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .estado(Estado.ACTIVO).potencialExtracto(80).build();
    }
}
