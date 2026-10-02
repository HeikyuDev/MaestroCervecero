package com.github.heikyudev.maestrocervecero.util.mapper.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleLupuloFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.RecetaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.VersionRecetaFormDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.etapa_control.EtapaControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.parametro_control.ParametroControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleMaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleParametroControlResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.PlanMonitoreoEtapaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapperRecetaFormTest {

    private static RecetaResponseDTO receta() {
        VersionRecetaResponseDTO version = VersionRecetaResponseDTO.builder()
                .id(70L).nombre("Pale Ale").volumenBase(20.0).relacionDeEmpaste(3.0)
                .ogObjetivo(1.05).fgObjetivo(1.01).ibuObjetivo(35)
                .duracionMaceracion(60).duracionHervido(90).duracionFermentacion(10).duracionMaduracion(4)
                .detallesMalta(List.of(DetalleMaltaResponseDTO.builder().id(1L).cantidad(4.5)
                        .malta(MaltaResponseDTO.builder().id(11L).nombre("Pilsen").build()).build()))
                .detallesLupulo(List.of(
                        DetalleLupuloResponseDTO.builder().id(1L).cantidad(25.0).uso(UsoLupulo.HERVOR)
                                .etapaDeUso(TipoEtapa.HERVIDO).tiempoDeHervor(60.0)
                                .lupulo(LupuloResponseDTO.builder().id(21L).nombre("Cascade").build()).build(),
                        DetalleLupuloResponseDTO.builder().id(2L).cantidad(40.0).uso(UsoLupulo.DRY_HOP)
                                .etapaDeUso(TipoEtapa.FERMENTACION).tiempoDeHervor(0.0)
                                .lupulo(LupuloResponseDTO.builder().id(22L).nombre("Citra").build()).build()))
                .detallesLevadura(List.of(DetalleLevaduraResponseDTO.builder().id(1L).cantidad(11.5)
                        .levadura(LevaduraResponseDTO.builder().id(31L).nombre("US-05").build()).build()))
                .planesMonitoreo(List.of(PlanMonitoreoEtapaResponseDTO.builder().id(1L)
                        .etapaControl(EtapaControlResponseDTO.builder().id(41L).nombre("Control de densidad").build())
                        .detallesParametroControl(List.of(DetalleParametroControlResponseDTO.builder().id(1L)
                                .valorMinimo(1.01).valorIdeal(1.012).valorMaximo(1.015)
                                .parametroControl(ParametroControlResponseDTO.builder().id(51L).nombre("Densidad").build())
                                .build()))
                        .build()))
                .build();
        return RecetaResponseDTO.builder().id(7L).version(version).build();
    }

    @Test
    @DisplayName("CP-MRF-01: los datos generales y las duraciones de la versión vigente pasan al formulario")
    void toFormDTO_debeCopiarLosDatosGeneralesYLasDuraciones() {
        VersionRecetaFormDTO version = MapperRecetaForm.toFormDTO(receta()).getVersion();

        assertThat(version.getNombre()).isEqualTo("Pale Ale");
        assertThat(version.getVolumenBase()).isEqualTo(20.0);
        assertThat(version.getRelacionDeEmpaste()).isEqualTo(3.0);
        assertThat(version.getOgObjetivo()).isEqualTo(1.05);
        assertThat(version.getFgObjetivo()).isEqualTo(1.01);
        assertThat(version.getIbuObjetivo()).isEqualTo(35);
        assertThat(version.getDuracionMaceracion()).isEqualTo(60);
        assertThat(version.getDuracionHervido()).isEqualTo(90);
        assertThat(version.getDuracionFermentacion()).isEqualTo(10);
        assertThat(version.getDuracionMaduracion()).isEqualTo(4);
    }

    @Test
    @DisplayName("CP-MRF-02: cada detalle de malta, lúpulo y levadura queda como una fila con el id del insumo y su cantidad")
    void toFormDTO_debeConvertirLosDetallesEnFilasConElIdDelInsumo() {
        VersionRecetaFormDTO version = MapperRecetaForm.toFormDTO(receta()).getVersion();

        assertThat(version.getDetallesMalta()).hasSize(1);
        assertThat(version.getDetallesMalta().get(0).getIdMalta()).isEqualTo(11L);
        assertThat(version.getDetallesMalta().get(0).getCantidad()).isEqualTo(4.5);

        assertThat(version.getDetallesLupulo()).hasSize(2);
        DetalleLupuloFormDTO hervor = version.getDetallesLupulo().get(0);
        assertThat(hervor.getIdLupulo()).isEqualTo(21L);
        assertThat(hervor.getCantidad()).isEqualTo(25.0);
        assertThat(hervor.getUso()).isEqualTo(UsoLupulo.HERVOR);
        assertThat(hervor.getEtapaDeUso()).isEqualTo(TipoEtapa.HERVIDO);

        assertThat(version.getDetallesLevadura()).hasSize(1);
        assertThat(version.getDetallesLevadura().get(0).getIdLevadura()).isEqualTo(31L);
        assertThat(version.getDetallesLevadura().get(0).getCantidad()).isEqualTo(11.5);
    }

    @Test
    @DisplayName("CP-MRF-03: el tiempo de hervor solo se conserva en el uso Hervor; en los demás usos queda vacío")
    void toFormDTO_debeConservarElTiempoDeHervorSoloEnHervor() {
        VersionRecetaFormDTO version = MapperRecetaForm.toFormDTO(receta()).getVersion();

        assertThat(version.getDetallesLupulo().get(0).getTiempoDeHervor()).isEqualTo(60.0);
        assertThat(version.getDetallesLupulo().get(1).getUso()).isEqualTo(UsoLupulo.DRY_HOP);
        assertThat(version.getDetallesLupulo().get(1).getEtapaDeUso()).isEqualTo(TipoEtapa.FERMENTACION);
        assertThat(version.getDetallesLupulo().get(1).getTiempoDeHervor()).isNull();
    }

    @Test
    @DisplayName("CP-MRF-04: los planes de monitoreo conservan la etapa de control y el rango de cada parámetro")
    void toFormDTO_debeConservarLosPlanesDeMonitoreo() {
        VersionRecetaFormDTO version = MapperRecetaForm.toFormDTO(receta()).getVersion();

        assertThat(version.getPlanesMonitoreo()).hasSize(1);
        assertThat(version.getPlanesMonitoreo().get(0).getIdEtapaControl()).isEqualTo(41L);
        assertThat(version.getPlanesMonitoreo().get(0).getDetallesParametroControl()).hasSize(1);
        var parametro = version.getPlanesMonitoreo().get(0).getDetallesParametroControl().get(0);
        assertThat(parametro.getIdParametroControl()).isEqualTo(51L);
        assertThat(parametro.getValorMinimo()).isEqualTo(1.01);
        assertThat(parametro.getValorIdeal()).isEqualTo(1.012);
        assertThat(parametro.getValorMaximo()).isEqualTo(1.015);
    }

    @Test
    @DisplayName("CP-MRF-05: una receta nula devuelve null y una sin versión vigente devuelve un formulario vacío con listas inicializadas")
    void toFormDTO_debeManejarRecetaNulaOSinVersion() {
        assertThat(MapperRecetaForm.toFormDTO(null)).isNull();

        RecetaFormDTO vacio = MapperRecetaForm.toFormDTO(RecetaResponseDTO.builder().id(7L).build());
        assertThat(vacio.getVersion().getNombre()).isNull();
        assertThat(vacio.getVersion().getDetallesMalta()).isEmpty();
        assertThat(vacio.getVersion().getDetallesLupulo()).isEmpty();
        assertThat(vacio.getVersion().getDetallesLevadura()).isEmpty();
        assertThat(vacio.getVersion().getPlanesMonitoreo()).isEmpty();
    }
}
