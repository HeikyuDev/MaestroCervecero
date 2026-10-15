package com.github.heikyudev.maestrocervecero.integracion;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleMaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILevaduraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILupuloRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IMaltaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.ILoteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.IEscaladoInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.RequerimientoInsumo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica contra PostgreSQL que iniciar un lote guarda la instantánea del escalado (tablas {@code escalado_lote} y
 * {@code escalado_lote_insumo}, referenciada desde {@code lote.escalado_lote_id}) y que, una vez guardada, las
 * cantidades del lote no cambian aunque se editen el macerador y los insumos.
 */
class EscaladoLoteIntegracionTest extends IntegracionBaseTest {

    @Autowired
    private ILoteServicio loteServicio;
    @Autowired
    private IEscaladoInsumoServicio escaladoInsumoServicio;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private ILoteRepository loteRepository;
    @Autowired
    private IMaltaRepository maltaRepository;
    @Autowired
    private ILupuloRepository lupuloRepository;
    @Autowired
    private ILevaduraRepository levaduraRepository;
    @Autowired
    private IRecetaRepository recetaRepository;
    @Autowired
    private IPlanificacionProduccionRepository planificacionProduccionRepository;
    @Autowired
    private IMolinoRepository molinoRepository;
    @Autowired
    private IMaceradorRepository maceradorRepository;
    @Autowired
    private IOllaHervorRepository ollaHervorRepository;
    @Autowired
    private IFermentadorRepository fermentadorRepository;
    @Autowired
    private ILoteInsumoRepository loteInsumoRepository;

    @Test
    @DisplayName("CP-EIN-01: iniciarLote guarda la instantánea del escalado en la base y las cantidades del lote no cambian al editar el macerador y los insumos")
    void iniciarLote_debeGuardarLaInstantaneaYMantenerLasCantidades() {
        // === PREPARACION DE DATOS ===
        Long idLote = crearLotePendiente();

        // === EJECUCION ===
        loteServicio.iniciarLote(idLote);

        // === ASSERTS ===
        assertThat(jdbcTemplate.queryForObject("select count(*) from escalado_lote", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select escalado_lote_id from lote where id = ?", Long.class, idLote)).isNotNull();
        Map<String, Object> escalado = jdbcTemplate.queryForMap("select * from escalado_lote");
        assertThat(escalado.get("rendimiento_molienda")).isEqualTo(50.0);
        assertThat(escalado.get("eficiencia_maceracion")).isEqualTo(75.0);
        assertThat(escalado.get("espacio_muerto_macerador")).isEqualTo(5.0);
        assertThat(escalado.get("capacidad_util_macerador")).isEqualTo(100.0);
        assertThat(escalado.get("perdida_por_trub_olla")).isEqualTo(2.0);
        assertThat(escalado.get("evaporacion_olla")).isEqualTo(3.0);
        assertThat(escalado.get("capacidad_util_olla")).isEqualTo(200.0);
        assertThat(escalado.get("capacidad_util_fermentador")).isEqualTo(150.0);
        assertThat(jdbcTemplate.queryForObject("select count(*) from escalado_lote_insumo", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select potencial_extracto from escalado_lote_insumo where potencial_extracto is not null", Double.class)).isEqualTo(80.0);
        assertThat(jdbcTemplate.queryForObject("select alfa_acidos from escalado_lote_insumo where alfa_acidos is not null", Double.class)).isEqualTo(5.5);
        assertThat(jdbcTemplate.queryForObject("select formato from escalado_lote_insumo where formato is not null", String.class)).isEqualTo("PELLET");
        assertThat(jdbcTemplate.queryForObject("select tipo_levadura from escalado_lote_insumo where tipo_levadura is not null", String.class)).isEqualTo("ALE");

        // Las cantidades requeridas no cambian aunque se editen los datos vigentes del macerador y de los insumos
        List<Double> antes = cantidadesRequeridas(idLote);
        editarMaceradorEInsumos();
        assertThat(cantidadesRequeridas(idLote)).isEqualTo(antes);
        assertThat(jdbcTemplate.queryForObject("select eficiencia_maceracion from escalado_lote", Double.class)).isEqualTo(75.0);
    }

    private List<Double> cantidadesRequeridas(Long idLote) {
        return transactionTemplate.execute(estado -> {
            LoteEntity lote = loteRepository.findById(idLote).orElseThrow();
            return escaladoInsumoServicio.calcularRequerimientosTotales(lote).stream()
                    .map(RequerimientoInsumo::cantidadRequerida)
                    .toList();
        });
    }

    private void editarMaceradorEInsumos() {
        MaceradorEntity macerador = maceradorRepository.findAll().get(0);
        macerador.setEficienciaMaceracion(50.0);
        maceradorRepository.save(macerador);
        MaltaEntity malta = maltaRepository.findAll().get(0);
        malta.setPotencialExtracto(60);
        maltaRepository.save(malta);
        LupuloEntity lupulo = lupuloRepository.findAll().get(0);
        lupulo.setAa(12.0);
        lupulo.setFormato(FormatoLupulo.FLOR);
        lupuloRepository.save(lupulo);
        LevaduraEntity levadura = levaduraRepository.findAll().get(0);
        levadura.setTipo(TipoLevadura.LAGER);
        levadura.setCantidadCelulasPorGramo(5_000_000_000.0);
        levaduraRepository.save(levadura);
    }

    /** Crea una receta con una malta, un lúpulo y una levadura, sus cuatro equipos, el stock y un lote Pendiente. */
    private Long crearLotePendiente() {
        MaltaEntity malta = maltaRepository.save(MaltaEntity.builder().nombre("Pilsen").unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .estado(Estado.ACTIVO).tipo(TipoMalta.BASE).potencialExtracto(80).build());
        LupuloEntity lupulo = lupuloRepository.save(LupuloEntity.builder().nombre("Cascade").unidadDeMedida(UnidadDeMedida.GRAMO)
                .estado(Estado.ACTIVO).aa(5.5).formato(FormatoLupulo.PELLET).build());
        LevaduraEntity levadura = levaduraRepository.save(LevaduraEntity.builder().nombre("SafAle").unidadDeMedida(UnidadDeMedida.GRAMO)
                .estado(Estado.ACTIVO).tipo(TipoLevadura.ALE).cantidadCelulasPorGramo(10_000_000_000.0).build());
        for (var insumo : List.of(malta, lupulo, levadura)) {
            loteInsumoRepository.save(LoteInsumoEntity.builder().insumo(insumo).cantidadActual(100_000.0).cantidadReservada(0.0)
                    .identificacionLoteProveedor("PROV-" + insumo.getId()).fechaVencimiento(LocalDate.now().plusDays(90)).build());
        }

        RecetaEntity receta = RecetaEntity.builder().contadorLotes(1L).estado(Estado.ACTIVO).build();
        VersionRecetaEntity version = VersionRecetaEntity.builder().nombre("IPA Test").volumenBase(20.0).relacionDeEmpaste(3.0)
                .ogObjetivo(1.050).fgObjetivo(1.010).ibuObjetivo(35).duracionMaceracion(60).duracionHervido(60)
                .duracionFermentacion(14).duracionMaduracion(7).esUltimaVersion(true).padreActivo(true).receta(receta).build();
        version.getDetallesMalta().add(DetalleMaltaEntity.builder().cantidad(100.0).malta(malta).versionReceta(version).build());
        version.getDetallesLupulo().add(DetalleLupuloEntity.builder().cantidad(30.0).uso(UsoLupulo.HERVOR).etapaDeUso(TipoEtapa.HERVIDO)
                .tiempoDeHervor(60.0).lupulo(lupulo).versionReceta(version).build());
        version.getDetallesLevadura().add(DetalleLevaduraEntity.builder().cantidad(6.0).levadura(levadura).versionReceta(version).build());
        receta.getVersiones().add(version);
        recetaRepository.save(receta);

        PlanificacionProduccionEntity planificacion = planificacionProduccionRepository.save(PlanificacionProduccionEntity.builder()
                .fechaInicioEstimada(LocalDate.now()).fechaFinalizacionEstimada(LocalDate.now().plusDays(60)).cantidadAProducir(100.0)
                .estado(EstadoSolicitud.PENDIENTE).versionReceta(version).build());

        MolinoEntity molino = molinoRepository.save(MolinoEntity.builder().identificadorInterno("MOL-01").estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).estado(Estado.ACTIVO).rendimientoMolienda(50.0).build());
        MaceradorEntity macerador = maceradorRepository.save(MaceradorEntity.builder().identificadorInterno("MAC-01").estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).estado(Estado.ACTIVO).capacidadTotal(110.0).capacidadUtil(100.0).espacioMuerto(5.0).eficienciaMaceracion(75.0).build());
        OllaHervorEntity olla = ollaHervorRepository.save(OllaHervorEntity.builder().identificadorInterno("OLL-01").estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).estado(Estado.ACTIVO).capacidadTotal(210.0).capacidadUtil(200.0).evaporacion(3.0).perdidaPorTrub(2.0).build());
        FermentadorEntity fermentador = fermentadorRepository.save(FermentadorEntity.builder().identificadorInterno("FER-01").estadoOperativo(EstadoOperativo.DISPONIBLE)
                .usosMaximosAntesMantenimiento(100).estado(Estado.ACTIVO).capacidadTotal(160.0).capacidadUtil(150.0).build());

        LoteEntity lote = LoteEntity.builder().identificadorInterno("IPA_TEST-R1-1").volumenObjetivo(20.0).estado(EstadoLote.PENDIENTE)
                .fechaInicioEstimada(LocalDate.now()).fechaFinalizacionEstimada(LocalDate.now().plusDays(25)).planificacionProduccion(planificacion).build();
        List<EtapaLoteEntity> etapas = new ArrayList<>();
        etapas.add(etapa(lote, TipoEtapa.MOLIENDA, molino));
        etapas.add(etapa(lote, TipoEtapa.MACERACION, macerador));
        etapas.add(etapa(lote, TipoEtapa.HERVIDO, olla));
        etapas.add(etapa(lote, TipoEtapa.FERMENTACION, fermentador));
        etapas.add(etapa(lote, TipoEtapa.MADURACION, fermentador));
        etapas.add(etapa(lote, TipoEtapa.ENVASADO, fermentador));
        lote.setEtapas(etapas);
        return loteRepository.save(lote).getId();
    }

    private static EtapaLoteEntity etapa(LoteEntity lote, TipoEtapa tipo, com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity equipamiento) {
        return EtapaLoteEntity.builder().etapa(tipo).estado(EstadoEtapaLote.PENDIENTE).equipamiento(equipamiento).lote(lote).build();
    }
}
