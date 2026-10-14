package com.github.heikyudev.maestrocervecero.service.implementation.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.RequerimientoInsumo;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import com.github.heikyudev.maestrocervecero.util.validacion.Redondeo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class EscaladoInsumoServicioImplTest {

    private final EscaladoInsumoServicioImpl escaladoInsumoServicio = new EscaladoInsumoServicioImpl();

    // ==================== calcularRequerimientosMalta ====================

    @Test
    @DisplayName("CP-EI-CM-01: calcularRequerimientosMalta reparte el total escalado entre 2 maltas manteniendo la proporción de la receta (EscaladoDeMalta.md)")
    void calcularRequerimientosMalta_debeRepartirEntreDosMaltasSegunProporcion() {
        // === PREPARACION DE DATOS ===
        MaltaEntity pilsen = crearMalta(1L, "Pilsen", 80);
        MaltaEntity caramelo = crearMalta(2L, "Caramelo", 74);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050)
                .detallesMalta(List.of(detalleMalta(100.0, pilsen), detalleMalta(20.0, caramelo)))
                .build();
        MaceradorEntity macerador = crearMacerador(75.0);

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosMalta(versionReceta, 20.0, macerador);

        // === ASSERTS ===
        // PD=1000; EPpromedio=0.79; KgTotal=1000/(0.79*0.75*384)=4.3952kg; reparto 5/6 y 1/6: Pilsen 3.6627kg, Caramelo 0.7325kg
        assertThat(resultado).hasSize(2);
        RequerimientoInsumo requerimientoPilsen = resultado.stream().filter(r -> r.insumo().equals(pilsen)).findFirst().orElseThrow();
        RequerimientoInsumo requerimientoCaramelo = resultado.stream().filter(r -> r.insumo().equals(caramelo)).findFirst().orElseThrow();
        assertThat(requerimientoPilsen.cantidadRequerida()).isCloseTo(3.6627, within(0.0001));
        assertThat(requerimientoCaramelo.cantidadRequerida()).isCloseTo(0.7325, within(0.0001));
        assertThat(requerimientoPilsen.etapa()).isNull();
        assertThat(requerimientoCaramelo.etapa()).isNull();
    }

    // ==================== calcularRequerimientosLupulo ====================

    @Test
    @DisplayName("CP-EI-CL-01: calcularRequerimientosLupulo calcula HERVOR con Tinseth y DRY_HOP con escalado lineal en la misma llamada, resolviendo la etapa de cada detalle (EscaladoDelLupulo.md)")
    void calcularRequerimientosLupulo_debeCalcularHervorYDryHopMezclados() {
        // === PREPARACION DE DATOS ===
        LupuloEntity cascade = crearLupulo(1L, "Cascade", 5.5, FormatoLupulo.PELLET);
        LupuloEntity centennial = crearLupulo(2L, "Centennial", 10.0, FormatoLupulo.PELLET);
        LupuloEntity citra = crearLupulo(3L, "Citra", 12.0, FormatoLupulo.PELLET);
        DetalleLupuloEntity detalleCascade = detalleLupulo(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 60.0, cascade);
        DetalleLupuloEntity detalleCentennial = detalleLupulo(10.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 15.0, centennial);
        DetalleLupuloEntity detalleCitra = detalleLupulo(15.0, UsoLupulo.DRY_HOP, TipoEtapa.MADURACION, 0.0, citra);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050)
                .ibuObjetivo(35)
                .volumenBase(20.0)
                .detallesLupulo(List.of(detalleCascade, detalleCentennial, detalleCitra))
                .build();
        LoteEntity lote = crearLoteConEtapas();

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosLupulo(versionReceta, 100.0, lote);

        // === ASSERTS ===
        assertThat(resultado).hasSize(3);
        RequerimientoInsumo reqCascade = resultado.stream().filter(r -> r.insumo().equals(cascade)).findFirst().orElseThrow();
        RequerimientoInsumo reqCentennial = resultado.stream().filter(r -> r.insumo().equals(centennial)).findFirst().orElseThrow();
        RequerimientoInsumo reqCitra = resultado.stream().filter(r -> r.insumo().equals(citra)).findFirst().orElseThrow();

        // Gramos totales HERVOR = 257.09g, repartidos 75%/25% → A = 192.82g, B = 64.27g
        assertThat(reqCascade.cantidadRequerida()).isCloseTo(192.82, within(0.01));
        assertThat(reqCentennial.cantidadRequerida()).isCloseTo(64.27, within(0.01));
        assertThat(reqCascade.etapa().getEtapa()).isEqualTo(TipoEtapa.HERVIDO);
        assertThat(reqCentennial.etapa().getEtapa()).isEqualTo(TipoEtapa.HERVIDO);

        // Lúpulo C (DRY HOP, escalado lineal): 15 * (100/20) = 75g exactos
        assertThat(reqCitra.cantidadRequerida()).isCloseTo(75.0, within(0.001));
        assertThat(reqCitra.etapa().getEtapa()).isEqualTo(TipoEtapa.MADURACION);
    }

    @Test
    @DisplayName("CP-EI-CL-02: calcularRequerimientosLupulo lanza RecursoNoEncontradoException si el lote no tiene la etapa de uso indicada por un detalle")
    void calcularRequerimientosLupulo_debeLanzarExcepcionSiNoExisteLaEtapaDeUso() {
        // === PREPARACION DE DATOS ===
        LupuloEntity citra = crearLupulo(3L, "Citra", 12.0, FormatoLupulo.PELLET);
        DetalleLupuloEntity detalle = detalleLupulo(15.0, UsoLupulo.DRY_HOP, TipoEtapa.MADURACION, 0.0, citra);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050)
                .ibuObjetivo(35)
                .volumenBase(20.0)
                .detallesLupulo(List.of(detalle))
                .build();
        LoteEntity loteSinEtapas = LoteEntity.builder().id(1L).etapas(new ArrayList<>()).build();

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> escaladoInsumoServicio.calcularRequerimientosLupulo(versionReceta, 100.0, loteSinEtapas))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El lote no tiene una etapa de Maduración");
    }

    // ==================== calcularRequerimientosLevadura ====================

    @Test
    @DisplayName("CP-EI-CV-01: calcularRequerimientosLevadura reparte el total escalado entre 2 levaduras manteniendo la proporción de la receta (EscaladoDeLevadura.md, Caso 2)")
    void calcularRequerimientosLevadura_debeRepartirEntreDosLevadurasSegunProporcion() {
        // === PREPARACION DE DATOS ===
        LevaduraEntity levaduraA = crearLevadura(1L, "LevaduraA", TipoLevadura.ALE, 18_000.0e6);
        LevaduraEntity levaduraB = crearLevadura(2L, "LevaduraB", TipoLevadura.HIBRIDA, 20_000.0e6);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050)
                .detallesLevadura(List.of(detalleLevadura(6.0, levaduraA), detalleLevadura(4.0, levaduraB)))
                .build();

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosLevadura(versionReceta, 20.0);

        // === ASSERTS ===
        assertThat(resultado).hasSize(2);
        RequerimientoInsumo reqA = resultado.stream().filter(r -> r.insumo().equals(levaduraA)).findFirst().orElseThrow();
        RequerimientoInsumo reqB = resultado.stream().filter(r -> r.insumo().equals(levaduraB)).findFirst().orElseThrow();
        assertThat(reqA.cantidadRequerida()).isCloseTo(7.0833, within(0.001));
        assertThat(reqB.cantidadRequerida()).isCloseTo(4.25, within(0.001));
        assertThat(reqA.etapa()).isNull();
        assertThat(reqB.etapa()).isNull();
    }

    // ==================== asociarEtapa ====================

    @Test
    @DisplayName("CP-EI-AE-01: asociarEtapa devuelve una copia de cada requerimiento con la etapa indicada, sin alterar la cantidad requerida")
    void asociarEtapa_debeCopiarConLaEtapaIndicadaSinAlterarLaCantidad() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = crearMalta(1L, "Pilsen", 80);
        RequerimientoInsumo requerimientoSinEtapa = new RequerimientoInsumo(malta, null, 3.5);
        LoteEntity lote = crearLoteConEtapas();
        EtapaLoteEntity etapaMolienda = lote.getEtapas().stream().filter(e -> e.getEtapa() == TipoEtapa.MOLIENDA).findFirst().orElseThrow();

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.asociarEtapa(List.of(requerimientoSinEtapa), etapaMolienda);

        // === ASSERTS ===
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).etapa()).isEqualTo(etapaMolienda);
        assertThat(resultado.get(0).cantidadRequerida()).isEqualTo(3.5);
        assertThat(resultado.get(0).insumo()).isEqualTo(malta);
    }

    @Test
    @DisplayName("CP-EI-AE-02: asociarEtapa conserva la cantidad requerida redondeada a 4 decimales, sin ruido de punto flotante")
    void asociarEtapa_debeConservarLaCantidadRedondeadaA4Decimales() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = crearMalta(1L, "Pilsen", 80);
        RequerimientoInsumo requerimientoConRuido = new RequerimientoInsumo(malta, null, 19.879134860050907);
        EtapaLoteEntity etapaMolienda = crearLoteConEtapas().getEtapas().stream().filter(e -> e.getEtapa() == TipoEtapa.MOLIENDA).findFirst().orElseThrow();

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.asociarEtapa(List.of(requerimientoConRuido), etapaMolienda);

        // === ASSERTS ===
        assertThat(requerimientoConRuido.cantidadRequerida()).isEqualTo(19.8791);
        assertThat(resultado.get(0).cantidadRequerida()).isEqualTo(19.8791);
    }

    @Test
    @DisplayName("CP-EI-AE-03: consumir exactamente la cantidad requerida mostrada alcanza el 100% y no supera lo requerido")
    void asociarEtapa_consumirLoMostradoDebeAlcanzarElCienPorCiento() {
        // === PREPARACION DE DATOS ===
        MaltaEntity malta = crearMalta(1L, "Pilsen", 80);
        RequerimientoInsumo requerimiento = new RequerimientoInsumo(malta, null, 430.8403177877895);
        double cantidadMostrada = requerimiento.cantidadRequerida();

        // === ASSERTS ===
        assertThat(cantidadMostrada).isEqualTo(430.8403);
        assertThat(Redondeo.redondear(cantidadMostrada / requerimiento.cantidadRequerida() * 100.0)).isEqualTo(100.0);
        assertThat(Redondeo.redondear(cantidadMostrada)).isLessThanOrEqualTo(requerimiento.cantidadRequerida());
    }

    // ==================== calcularRequerimientosTotales ====================

    @Test
    @DisplayName("CP-EI-CT-01: calcularRequerimientosTotales NO fusiona el requerimiento cuando el mismo lúpulo aparece en dos detalles de etapas distintas (WHIRLPOOL en Hervido + DRY_HOP en Maduración): mantiene 2 requerimientos separados, cada uno atado a su propia etapa")
    void calcularRequerimientosTotales_noDebeFusionarElMismoInsumoEnEtapasDistintas() {
        // === PREPARACION DE DATOS ===
        LupuloEntity lupuloDoble = crearLupulo(23L, "LupuloDoble", 5.0, FormatoLupulo.FLOR);
        DetalleLupuloEntity whirlpool = detalleLupulo(10.0, UsoLupulo.WHIRLPOOL, TipoEtapa.HERVIDO, 0.0, lupuloDoble);
        DetalleLupuloEntity dryHop = detalleLupulo(5.0, UsoLupulo.DRY_HOP, TipoEtapa.MADURACION, 0.0, lupuloDoble);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050)
                .ibuObjetivo(35)
                .volumenBase(20.0)
                .detallesMalta(List.of())
                .detallesLupulo(List.of(whirlpool, dryHop))
                .detallesLevadura(List.of())
                .build();
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 20.0); // volumenObjetivo = volumenBase → ratio 1:1

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosTotales(lote);

        // === ASSERTS ===
        List<RequerimientoInsumo> requerimientosLupulo = resultado.stream().filter(r -> r.insumo().equals(lupuloDoble)).toList();
        assertThat(requerimientosLupulo).hasSize(2);

        RequerimientoInsumo requerimientoHervido = requerimientosLupulo.stream()
                .filter(r -> r.etapa().getEtapa() == TipoEtapa.HERVIDO)
                .findFirst().orElseThrow();
        RequerimientoInsumo requerimientoMaduracion = requerimientosLupulo.stream()
                .filter(r -> r.etapa().getEtapa() == TipoEtapa.MADURACION)
                .findFirst().orElseThrow();
        assertThat(requerimientoHervido.cantidadRequerida()).isCloseTo(10.0, within(0.001));
        assertThat(requerimientoMaduracion.cantidadRequerida()).isCloseTo(5.0, within(0.001));
    }

    @Test
    @DisplayName("CP-EI-CT-02: calcularRequerimientosTotales asocia la malta a la etapa de Molienda (calculada con el macerador de la etapa de Maceración), la levadura a Fermentación y el lúpulo a la etapa de su detalle")
    void calcularRequerimientosTotales_debeAsociarLaMaltaALaEtapaDeMolienda() {
        // === PREPARACION DE DATOS ===
        MaltaEntity pilsen = crearMalta(1L, "Pilsen", 80);
        LevaduraEntity levadura = crearLevadura(2L, "LevaduraA", TipoLevadura.ALE, 20_000.0e6);
        LupuloEntity cascade = crearLupulo(3L, "Cascade", 5.5, FormatoLupulo.PELLET);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050).ibuObjetivo(35).volumenBase(20.0)
                .detallesMalta(List.of(detalleMalta(100.0, pilsen)))
                .detallesLevadura(List.of(detalleLevadura(6.0, levadura)))
                .detallesLupulo(List.of(detalleLupulo(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 60.0, cascade)))
                .build();
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 20.0);

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosTotales(lote);

        // === ASSERTS ===
        assertThat(resultado).hasSize(3);
        RequerimientoInsumo reqMalta = resultado.stream().filter(r -> r.insumo().equals(pilsen)).findFirst().orElseThrow();
        RequerimientoInsumo reqLevadura = resultado.stream().filter(r -> r.insumo().equals(levadura)).findFirst().orElseThrow();
        RequerimientoInsumo reqLupulo = resultado.stream().filter(r -> r.insumo().equals(cascade)).findFirst().orElseThrow();
        assertThat(reqMalta.etapa().getEtapa()).isEqualTo(TipoEtapa.MOLIENDA);
        assertThat(reqMalta.cantidadRequerida()).isCloseTo(4.3403, within(0.0001));
        assertThat(reqLevadura.etapa().getEtapa()).isEqualTo(TipoEtapa.FERMENTACION);
        assertThat(reqLupulo.etapa().getEtapa()).isEqualTo(TipoEtapa.HERVIDO);
    }

    // ==================== calcularRequerimientosEtapa ====================

    @Test
    @DisplayName("CP-EI-CE-01: calcularRequerimientosEtapa en la etapa de Molienda solo calcula malta (con el macerador de la etapa de Maceración), aunque la receta también tenga lúpulo y levadura")
    void calcularRequerimientosEtapa_debeCalcularSoloMaltaEnMolienda() {
        // === PREPARACION DE DATOS ===
        MaltaEntity pilsen = crearMalta(1L, "Pilsen", 80);
        LevaduraEntity levadura = crearLevadura(2L, "LevaduraA", TipoLevadura.ALE, 20_000.0e6);
        LupuloEntity cascade = crearLupulo(3L, "Cascade", 5.5, FormatoLupulo.PELLET);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050).ibuObjetivo(35).volumenBase(20.0)
                .detallesMalta(List.of(detalleMalta(100.0, pilsen)))
                .detallesLevadura(List.of(detalleLevadura(6.0, levadura)))
                .detallesLupulo(List.of(detalleLupulo(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 60.0, cascade)))
                .build();
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 20.0);
        EtapaLoteEntity etapaMolienda = lote.obtenerEtapaPorTipo(TipoEtapa.MOLIENDA);

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosEtapa(etapaMolienda);

        // === ASSERTS ===
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).insumo()).isEqualTo(pilsen);
        assertThat(resultado.get(0).etapa()).isEqualTo(etapaMolienda);
        assertThat(resultado.get(0).cantidadRequerida()).isCloseTo(4.3403, within(0.0001));
    }

    @Test
    @DisplayName("CP-EI-CE-02: calcularRequerimientosEtapa en la etapa de Fermentación solo calcula levadura cuando ningún lúpulo de la receta apunta a esa etapa")
    void calcularRequerimientosEtapa_debeCalcularSoloLevaduraEnFermentacionSinDryHopAsociado() {
        // === PREPARACION DE DATOS ===
        MaltaEntity pilsen = crearMalta(1L, "Pilsen", 80);
        LevaduraEntity levadura = crearLevadura(2L, "LevaduraA", TipoLevadura.ALE, 20_000.0e6);
        LupuloEntity cascade = crearLupulo(3L, "Cascade", 5.5, FormatoLupulo.PELLET);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050).ibuObjetivo(35).volumenBase(20.0)
                .detallesMalta(List.of(detalleMalta(100.0, pilsen)))
                .detallesLevadura(List.of(detalleLevadura(6.0, levadura)))
                .detallesLupulo(List.of(detalleLupulo(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 60.0, cascade)))
                .build();
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 20.0);
        EtapaLoteEntity etapaFermentacion = lote.obtenerEtapaPorTipo(TipoEtapa.FERMENTACION);

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosEtapa(etapaFermentacion);

        // === ASSERTS ===
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).insumo()).isEqualTo(levadura);
        assertThat(resultado.get(0).etapa()).isEqualTo(etapaFermentacion);
    }

    @Test
    @DisplayName("CP-EI-CE-03: calcularRequerimientosEtapa en la etapa de Hervido solo calcula los lúpulos de HERVOR asociados a esa etapa, sin calcular malta ni levadura")
    void calcularRequerimientosEtapa_debeCalcularSoloLupuloEnHervido() {
        // === PREPARACION DE DATOS ===
        MaltaEntity pilsen = crearMalta(1L, "Pilsen", 80);
        LevaduraEntity levadura = crearLevadura(2L, "LevaduraA", TipoLevadura.ALE, 20_000.0e6);
        LupuloEntity cascade = crearLupulo(3L, "Cascade", 5.5, FormatoLupulo.PELLET);
        LupuloEntity centennial = crearLupulo(4L, "Centennial", 10.0, FormatoLupulo.PELLET);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050).ibuObjetivo(35).volumenBase(20.0)
                .detallesMalta(List.of(detalleMalta(100.0, pilsen)))
                .detallesLevadura(List.of(detalleLevadura(6.0, levadura)))
                .detallesLupulo(List.of(
                        detalleLupulo(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 60.0, cascade),
                        detalleLupulo(10.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 15.0, centennial)))
                .build();
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 100.0);
        EtapaLoteEntity etapaHervido = lote.obtenerEtapaPorTipo(TipoEtapa.HERVIDO);

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosEtapa(etapaHervido);

        // === ASSERTS === (mismos valores del ejemplo de EscaladoDelLupulo.md que CP-EI-CL-01)
        assertThat(resultado).hasSize(2);
        RequerimientoInsumo reqCascade = resultado.stream().filter(r -> r.insumo().equals(cascade)).findFirst().orElseThrow();
        RequerimientoInsumo reqCentennial = resultado.stream().filter(r -> r.insumo().equals(centennial)).findFirst().orElseThrow();
        assertThat(reqCascade.cantidadRequerida()).isCloseTo(192.82, within(0.01));
        assertThat(reqCentennial.cantidadRequerida()).isCloseTo(64.27, within(0.01));
        assertThat(reqCascade.etapa()).isEqualTo(etapaHervido);
        assertThat(reqCentennial.etapa()).isEqualTo(etapaHervido);
    }

    @Test
    @DisplayName("CP-EI-CE-04: calcularRequerimientosEtapa retorna una lista vacía cuando la etapa no requiere ningún insumo (ni Molienda ni Fermentación, y ningún lúpulo apunta a ella)")
    void calcularRequerimientosEtapa_debeRetornarListaVaciaSiNoHayInsumosParaEsaEtapa() {
        // === PREPARACION DE DATOS ===
        MaltaEntity pilsen = crearMalta(1L, "Pilsen", 80);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050).ibuObjetivo(35).volumenBase(20.0)
                .detallesMalta(List.of(detalleMalta(100.0, pilsen)))
                .detallesLevadura(List.of())
                .detallesLupulo(List.of())
                .build();
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 20.0);
        EtapaLoteEntity etapaEnvasado = lote.obtenerEtapaPorTipo(TipoEtapa.ENVASADO);

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosEtapa(etapaEnvasado);

        // === ASSERTS ===
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("CP-EI-CE-05: calcularRequerimientosEtapa fusiona (sumando la cantidad) dos detalles de lúpulo distintos del mismo insumo cuando ambos apuntan a la misma etapa")
    void calcularRequerimientosEtapa_debeFusionarDosDetallesDelMismoLupuloEnLaMismaEtapa() {
        // === PREPARACION DE DATOS ===
        LupuloEntity lupuloDoble = crearLupulo(5L, "LupuloDoble", 5.0, FormatoLupulo.FLOR);
        DetalleLupuloEntity whirlpool = detalleLupulo(10.0, UsoLupulo.WHIRLPOOL, TipoEtapa.MADURACION, 0.0, lupuloDoble);
        DetalleLupuloEntity dryHop = detalleLupulo(5.0, UsoLupulo.DRY_HOP, TipoEtapa.MADURACION, 0.0, lupuloDoble);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050).ibuObjetivo(35).volumenBase(20.0)
                .detallesMalta(List.of()).detallesLevadura(List.of())
                .detallesLupulo(List.of(whirlpool, dryHop))
                .build();
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 20.0); // volumenObjetivo = volumenBase → ratio 1:1
        EtapaLoteEntity etapaMaduracion = lote.obtenerEtapaPorTipo(TipoEtapa.MADURACION);

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosEtapa(etapaMaduracion);

        // === ASSERTS ===
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).insumo()).isEqualTo(lupuloDoble);
        assertThat(resultado.get(0).cantidadRequerida()).isCloseTo(15.0, within(0.001));
        assertThat(resultado.get(0).etapa()).isEqualTo(etapaMaduracion);
    }

    @Test
    @DisplayName("CP-EI-CE-06: calcularRequerimientosEtapa en la etapa de Molienda lanza ReglaNegocioException si la etapa de Maceración del lote no tiene un macerador asociado como equipamiento")
    void calcularRequerimientosEtapa_debeLanzarExcepcionSiElEquipamientoDeMaceracionNoEsUnMacerador() {
        // === PREPARACION DE DATOS ===
        MaltaEntity pilsen = crearMalta(1L, "Pilsen", 80);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050)
                .detallesMalta(List.of(detalleMalta(100.0, pilsen)))
                .build();
        RecetaEntity receta = RecetaEntity.builder().id(1L).build();
        versionReceta.setReceta(receta);
        PlanificacionProduccionEntity planificacion = PlanificacionProduccionEntity.builder().versionReceta(versionReceta).build();
        LoteEntity lote = LoteEntity.builder().id(1L).volumenObjetivo(20.0).planificacionProduccion(planificacion).build();
        // Equipamiento mal asociado deliberadamente: un Molino en vez de un Macerador
        MolinoEntity molino = MolinoEntity.builder().id(9L).identificadorInterno("Molino 1").estado(Estado.ACTIVO).build();
        EtapaLoteEntity etapaMolienda = EtapaLoteEntity.builder().id(1L).etapa(TipoEtapa.MOLIENDA).estado(EstadoEtapaLote.PENDIENTE).equipamiento(molino).lote(lote).build();
        EtapaLoteEntity etapaMaceracion = EtapaLoteEntity.builder().id(2L).etapa(TipoEtapa.MACERACION).estado(EstadoEtapaLote.PENDIENTE).equipamiento(molino).lote(lote).build();
        lote.setEtapas(List.of(etapaMolienda, etapaMaceracion));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> escaladoInsumoServicio.calcularRequerimientosEtapa(etapaMolienda))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa de Maceración del lote 1 no tiene un macerador asociado como equipamiento");
    }

    @Test
    @DisplayName("CP-EI-CE-07: calcularRequerimientosEtapa en la etapa de Molienda resuelve el macerador desenvolviendo el proxy de Hibernate del equipamiento de la etapa de Maceración")
    void calcularRequerimientosEtapa_debeResolverElMaceradorDesdeUnProxy() {
        // === PREPARACION DE DATOS ===
        MaltaEntity pilsen = crearMalta(1L, "Pilsen", 80);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050).ibuObjetivo(35).volumenBase(20.0)
                .detallesMalta(List.of(detalleMalta(100.0, pilsen)))
                .build();
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 20.0);
        EtapaLoteEntity etapaMolienda = lote.obtenerEtapaPorTipo(TipoEtapa.MOLIENDA);
        EtapaLoteEntity etapaMaceracion = lote.obtenerEtapaPorTipo(TipoEtapa.MACERACION);
        double cantidadConMaceradorReal = escaladoInsumoServicio.calcularRequerimientosEtapa(etapaMolienda).get(0).cantidadRequerida();
        EquipamientoEntity proxy = proxyDe(crearMacerador(75.0));
        etapaMaceracion.setEquipamiento(proxy);

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosEtapa(etapaMolienda);

        // === ASSERTS ===
        assertThat(proxy).isNotInstanceOf(MaceradorEntity.class);
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).insumo()).isEqualTo(pilsen);
        assertThat(resultado.get(0).cantidadRequerida()).isEqualTo(cantidadConMaceradorReal);
    }

    @Test
    @DisplayName("CP-EI-CE-08: calcularRequerimientosEtapa retorna una lista vacía en la etapa de Maceración, que no requiere insumos (la malta se requiere en Molienda), aunque la receta tenga malta")
    void calcularRequerimientosEtapa_debeRetornarListaVaciaEnMaceracion() {
        // === PREPARACION DE DATOS ===
        MaltaEntity pilsen = crearMalta(1L, "Pilsen", 80);
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .ogObjetivo(1.050).ibuObjetivo(35).volumenBase(20.0)
                .detallesMalta(List.of(detalleMalta(100.0, pilsen)))
                .detallesLevadura(List.of())
                .detallesLupulo(List.of())
                .build();
        LoteEntity lote = crearLoteConVersionReceta(versionReceta, 20.0);
        EtapaLoteEntity etapaMaceracion = lote.obtenerEtapaPorTipo(TipoEtapa.MACERACION);

        // === EJECUCION ===
        List<RequerimientoInsumo> resultado = escaladoInsumoServicio.calcularRequerimientosEtapa(etapaMaceracion);

        // === ASSERTS ===
        assertThat(resultado).isEmpty();
    }

    // ==================== helpers de construcción ====================

    /**
     * Simula el proxy perezoso de Hibernate que llega desde {@code EtapaLoteEntity.getEquipamiento()}: una instancia
     * de la clase abstracta {@code EquipamientoEntity} (no de {@code MaceradorEntity}) que delega en el real.
     */
    private static EquipamientoEntity proxyDe(MaceradorEntity real) {
        EquipamientoEntity proxy = mock(EquipamientoEntity.class, withSettings().extraInterfaces(HibernateProxy.class));
        HibernateProxy proxyHibernate = (HibernateProxy) proxy;
        LazyInitializer inicializador = mock(LazyInitializer.class);
        when(proxyHibernate.asHibernateProxy()).thenReturn(proxyHibernate);
        when(proxyHibernate.getHibernateLazyInitializer()).thenReturn(inicializador);
        when(inicializador.getImplementation()).thenReturn(real);
        return proxy;
    }

    private static MaltaEntity crearMalta(Long id, String nombre, int potencialExtracto) {
        return MaltaEntity.builder().id(id).nombre(nombre).unidadDeMedida(UnidadDeMedida.KILOGRAMO).estado(Estado.ACTIVO).potencialExtracto(potencialExtracto).build();
    }

    private static LupuloEntity crearLupulo(Long id, String nombre, double aa, FormatoLupulo formato) {
        return LupuloEntity.builder().id(id).nombre(nombre).unidadDeMedida(UnidadDeMedida.GRAMO).estado(Estado.ACTIVO).aa(aa).formato(formato).build();
    }

    private static LevaduraEntity crearLevadura(Long id, String nombre, TipoLevadura tipo, double celulasPorGramo) {
        return LevaduraEntity.builder().id(id).nombre(nombre).unidadDeMedida(UnidadDeMedida.GRAMO).estado(Estado.ACTIVO).tipo(tipo).cantidadCelulasPorGramo(celulasPorGramo).build();
    }

    private static DetalleMaltaEntity detalleMalta(double cantidad, MaltaEntity malta) {
        return DetalleMaltaEntity.builder().cantidad(cantidad).malta(malta).build();
    }

    private static DetalleLupuloEntity detalleLupulo(double cantidad, UsoLupulo uso, TipoEtapa etapaDeUso, double tiempoDeHervor, LupuloEntity lupulo) {
        return DetalleLupuloEntity.builder().cantidad(cantidad).uso(uso).etapaDeUso(etapaDeUso).tiempoDeHervor(tiempoDeHervor).lupulo(lupulo).build();
    }

    private static DetalleLevaduraEntity detalleLevadura(double cantidad, LevaduraEntity levadura) {
        return DetalleLevaduraEntity.builder().cantidad(cantidad).levadura(levadura).build();
    }

    private static MaceradorEntity crearMacerador(double eficienciaMaceracion) {
        return MaceradorEntity.builder()
                .id(1L)
                .identificadorInterno("Macerador 1")
                .estado(Estado.ACTIVO)
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100)
                .capacidadTotal(100.0)
                .capacidadUtil(90.0)
                .espacioMuerto(5.0)
                .eficienciaMaceracion(eficienciaMaceracion)
                .build();
    }

    /**
     * Lote mínimo con sus 6 etapas (sin equipamiento real asociado), suficiente para los tests que
     * solo necesitan resolver la etapa correspondiente a un tipo, sin ejercitar el escalado de
     * malta (que sí requiere un {@link MaceradorEntity} real navegable desde la etapa).
     */
    private static LoteEntity crearLoteConEtapas() {
        LoteEntity lote = LoteEntity.builder().id(1L).etapas(new ArrayList<>()).build();
        List<EtapaLoteEntity> etapas = new ArrayList<>();
        etapas.add(EtapaLoteEntity.builder().id(1L).etapa(TipoEtapa.MOLIENDA).estado(EstadoEtapaLote.PENDIENTE).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(2L).etapa(TipoEtapa.MACERACION).estado(EstadoEtapaLote.PENDIENTE).equipamiento(crearMacerador(75.0)).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(3L).etapa(TipoEtapa.HERVIDO).estado(EstadoEtapaLote.PENDIENTE).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(4L).etapa(TipoEtapa.FERMENTACION).estado(EstadoEtapaLote.PENDIENTE).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(5L).etapa(TipoEtapa.MADURACION).estado(EstadoEtapaLote.PENDIENTE).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(6L).etapa(TipoEtapa.ENVASADO).estado(EstadoEtapaLote.PENDIENTE).lote(lote).build());
        lote.setEtapas(etapas);
        return lote;
    }

    /**
     * Lote completo (con planificación de producción y receta) y sus 6 etapas, con la etapa de
     * Maceración asociada a un {@link MaceradorEntity} real — necesario para
     * {@code calcularRequerimientosTotales}, que resuelve el macerador navegando esa relación.
     */
    private static LoteEntity crearLoteConVersionReceta(VersionRecetaEntity versionReceta, double volumenObjetivo) {
        RecetaEntity receta = RecetaEntity.builder().id(1L).build();
        versionReceta.setReceta(receta);
        PlanificacionProduccionEntity planificacion = PlanificacionProduccionEntity.builder().versionReceta(versionReceta).build();
        LoteEntity lote = LoteEntity.builder().id(1L).volumenObjetivo(volumenObjetivo).estado(EstadoLote.PENDIENTE).planificacionProduccion(planificacion).build();
        List<EtapaLoteEntity> etapas = new ArrayList<>();
        etapas.add(EtapaLoteEntity.builder().id(1L).etapa(TipoEtapa.MOLIENDA).estado(EstadoEtapaLote.PENDIENTE).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(2L).etapa(TipoEtapa.MACERACION).estado(EstadoEtapaLote.PENDIENTE).equipamiento(crearMacerador(75.0)).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(3L).etapa(TipoEtapa.HERVIDO).estado(EstadoEtapaLote.PENDIENTE).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(4L).etapa(TipoEtapa.FERMENTACION).estado(EstadoEtapaLote.PENDIENTE).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(5L).etapa(TipoEtapa.MADURACION).estado(EstadoEtapaLote.PENDIENTE).lote(lote).build());
        etapas.add(EtapaLoteEntity.builder().id(6L).etapa(TipoEtapa.ENVASADO).estado(EstadoEtapaLote.PENDIENTE).lote(lote).build());
        lote.setEtapas(etapas);
        return lote;
    }
}
