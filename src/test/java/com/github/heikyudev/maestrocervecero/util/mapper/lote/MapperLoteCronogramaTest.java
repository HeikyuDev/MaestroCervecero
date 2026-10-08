package com.github.heikyudev.maestrocervecero.util.mapper.lote;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.PlanificacionProduccionEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MapperLoteCronogramaTest {

    // ==================== toDTO ====================

    @Test
    @DisplayName("CP-MLC-01: toDTO retorna null cuando el lote es nulo")
    void toDTO_debeRetornarNullConLoteNulo() {
        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = MapperLoteCronograma.toDTO(null);

        // === ASSERTS ===
        assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("CP-MLC-02: toDTO copia los datos generales del lote y de su planificación")
    void toDTO_debeCopiarLosDatosGeneralesDelLote() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(10L, EstadoLote.PENDIENTE, seisEtapasConEstado(EstadoEtapaLote.PENDIENTE));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = MapperLoteCronograma.toDTO(lote);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("PILSEN_CLASICA-3");
        assertThat(resultado.getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(resultado.getVolumenObjetivo()).isEqualTo(300.0);
        assertThat(resultado.getFechaInicioEstimada()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(resultado.getFechaFinalizacionEstimada()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(resultado.getIdPlanificacionProduccion()).isEqualTo(5L);
        assertThat(resultado.getNombreReceta()).isEqualTo("Pilsen Clásica");
    }

    @Test
    @DisplayName("CP-MLC-03: toDTO resuelve cada equipamiento y el fermentador por el tipo de su etapa")
    void toDTO_debeResolverLosEquiposPorElTipoDeEtapa() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(10L, EstadoLote.PENDIENTE, seisEtapasConEstado(EstadoEtapaLote.PENDIENTE));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = MapperLoteCronograma.toDTO(lote);

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInternoMolino()).isEqualTo("MOL-01");
        assertThat(resultado.getIdentificadorInternoMacerador()).isEqualTo("MAC-02");
        assertThat(resultado.getIdentificadorInternoOllaHervor()).isEqualTo("OLL-03");
        assertThat(resultado.getIdFermentador()).isEqualTo(40L);
        assertThat(resultado.getIdentificadorInternoFermentador()).isEqualTo("FER-04");
    }

    @Test
    @DisplayName("CP-MLC-04: toDTO deja nulas las fechas reales de un lote PENDIENTE")
    void toDTO_debeDejarNulasLasFechasRealesDeUnLotePendiente() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(10L, EstadoLote.PENDIENTE, seisEtapasConEstado(EstadoEtapaLote.PENDIENTE));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = MapperLoteCronograma.toDTO(lote);

        // === ASSERTS ===
        assertThat(resultado.getFechaInicio()).isNull();
        assertThat(resultado.getFechaFinalizacion()).isNull();
        assertThat(resultado.getFechaCancelacion()).isNull();
    }

    @Test
    @DisplayName("CP-MLC-05: toDTO informa como etapa actual la etapa EN_CURSO")
    void toDTO_debeInformarLaEtapaEnCursoComoEtapaActual() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(10L, EstadoLote.EN_EJECUCION, seisEtapas(
                EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.FINALIZADA, EstadoEtapaLote.EN_CURSO,
                EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE, EstadoEtapaLote.PENDIENTE));
        lote.setFechaInicio(LocalDateTime.of(2026, 11, 2, 8, 0));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = MapperLoteCronograma.toDTO(lote);

        // === ASSERTS ===
        assertThat(resultado.getEtapaActual()).isEqualTo(TipoEtapa.HERVIDO);
        assertThat(resultado.getFechaInicio()).isEqualTo(LocalDateTime.of(2026, 11, 2, 8, 0));
    }

    @Test
    @DisplayName("CP-MLC-06: toDTO deja la etapa actual en null cuando ninguna etapa está EN_CURSO")
    void toDTO_debeDejarEtapaActualNulaSinEtapaEnCurso() {
        // === PREPARACION DE DATOS ===
        LoteEntity lotePendiente = crearLote(10L, EstadoLote.PENDIENTE, seisEtapasConEstado(EstadoEtapaLote.PENDIENTE));
        LoteEntity loteFinalizado = crearLote(11L, EstadoLote.FINALIZADO, seisEtapasConEstado(EstadoEtapaLote.FINALIZADA));
        loteFinalizado.setFechaInicio(LocalDateTime.of(2026, 11, 2, 8, 0));
        loteFinalizado.setFechaFinalizacion(LocalDateTime.of(2026, 11, 20, 17, 30));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultadoPendiente = MapperLoteCronograma.toDTO(lotePendiente);
        LoteCronogramaResponseDTO resultadoFinalizado = MapperLoteCronograma.toDTO(loteFinalizado);

        // === ASSERTS ===
        assertThat(resultadoPendiente.getEtapaActual()).isNull();
        assertThat(resultadoFinalizado.getEtapaActual()).isNull();
        assertThat(resultadoFinalizado.getFechaInicio()).isEqualTo(LocalDateTime.of(2026, 11, 2, 8, 0));
        assertThat(resultadoFinalizado.getFechaFinalizacion()).isEqualTo(LocalDateTime.of(2026, 11, 20, 17, 30));
    }

    @Test
    @DisplayName("CP-MLC-07: toDTO mapea un lote CANCELADO antes de iniciar con sus fechas reales nulas")
    void toDTO_debeMapearLoteCanceladoConFechasRealesNulas() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(10L, EstadoLote.CANCELADO, seisEtapasConEstado(EstadoEtapaLote.PENDIENTE));
        lote.setFechaCancelacion(LocalDateTime.of(2026, 11, 5, 10, 30));

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = MapperLoteCronograma.toDTO(lote);

        // === ASSERTS ===
        assertThat(resultado.getEstado()).isEqualTo(EstadoLote.CANCELADO);
        assertThat(resultado.getFechaCancelacion()).isEqualTo(LocalDateTime.of(2026, 11, 5, 10, 30));
        assertThat(resultado.getFechaInicio()).isNull();
        assertThat(resultado.getFechaFinalizacion()).isNull();
        assertThat(resultado.getEtapaActual()).isNull();
    }

    @Test
    @DisplayName("CP-MLC-08: toDTO mapea un lote sin etapas dejando en null los equipos y la etapa actual")
    void toDTO_debeMapearLoteSinEtapas() {
        // === PREPARACION DE DATOS ===
        LoteEntity lote = crearLote(10L, EstadoLote.PENDIENTE, new ArrayList<>());

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = MapperLoteCronograma.toDTO(lote);

        // === ASSERTS ===
        assertThat(resultado.getIdFermentador()).isNull();
        assertThat(resultado.getIdentificadorInternoFermentador()).isNull();
        assertThat(resultado.getIdentificadorInternoMolino()).isNull();
        assertThat(resultado.getIdentificadorInternoMacerador()).isNull();
        assertThat(resultado.getIdentificadorInternoOllaHervor()).isNull();
        assertThat(resultado.getEtapaActual()).isNull();
        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("PILSEN_CLASICA-3");
        assertThat(resultado.getEstado()).isEqualTo(EstadoLote.PENDIENTE);
        assertThat(resultado.getVolumenObjetivo()).isEqualTo(300.0);
        assertThat(resultado.getFechaInicioEstimada()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(resultado.getFechaFinalizacionEstimada()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(resultado.getIdPlanificacionProduccion()).isEqualTo(5L);
        assertThat(resultado.getNombreReceta()).isEqualTo("Pilsen Clásica");
    }

    @Test
    @DisplayName("CP-MLC-09: toDTO deja en null los equipos de los tipos de etapa ausentes")
    void toDTO_debeDejarNulosLosEquiposDeLosTiposDeEtapaAusentes() {
        // === PREPARACION DE DATOS ===
        List<EtapaLoteEntity> etapas = new ArrayList<>();
        etapas.add(etapa(TipoEtapa.MOLIENDA, EstadoEtapaLote.PENDIENTE, molino()));
        etapas.add(etapa(TipoEtapa.FERMENTACION, EstadoEtapaLote.PENDIENTE, fermentador()));
        LoteEntity lote = crearLote(10L, EstadoLote.PENDIENTE, etapas);

        // === EJECUCION ===
        LoteCronogramaResponseDTO resultado = MapperLoteCronograma.toDTO(lote);

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInternoMolino()).isEqualTo("MOL-01");
        assertThat(resultado.getIdFermentador()).isEqualTo(40L);
        assertThat(resultado.getIdentificadorInternoFermentador()).isEqualTo("FER-04");
        assertThat(resultado.getIdentificadorInternoMacerador()).isNull();
        assertThat(resultado.getIdentificadorInternoOllaHervor()).isNull();
    }

    // ==================== toDTOList ====================

    @Test
    @DisplayName("CP-MLC-10: toDTOList retorna una lista vacía cuando la lista de entrada está vacía")
    void toDTOList_debeRetornarListaVaciaConListaVacia() {
        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = MapperLoteCronograma.toDTOList(List.of());

        // === ASSERTS ===
        assertThat(resultado).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("CP-MLC-11: toDTOList mapea cada lote y conserva el orden de la lista")
    void toDTOList_debeConservarElOrdenDeLaLista() {
        // === PREPARACION DE DATOS ===
        List<LoteEntity> lotes = List.of(
                crearLote(12L, EstadoLote.PENDIENTE, seisEtapasConEstado(EstadoEtapaLote.PENDIENTE)),
                crearLote(10L, EstadoLote.PENDIENTE, seisEtapasConEstado(EstadoEtapaLote.PENDIENTE)),
                crearLote(11L, EstadoLote.PENDIENTE, seisEtapasConEstado(EstadoEtapaLote.PENDIENTE)));

        // === EJECUCION ===
        List<LoteCronogramaResponseDTO> resultado = MapperLoteCronograma.toDTOList(lotes);

        // === ASSERTS ===
        assertThat(resultado).extracting(LoteCronogramaResponseDTO::getId).containsExactly(12L, 10L, 11L);
    }

    // ==================== helpers ====================

    private static LoteEntity crearLote(Long id, EstadoLote estado, List<EtapaLoteEntity> etapas) {
        VersionRecetaEntity versionReceta = VersionRecetaEntity.builder()
                .id(1L)
                .nombre("Pilsen Clásica")
                .build();
        PlanificacionProduccionEntity planificacion = PlanificacionProduccionEntity.builder()
                .id(5L)
                .versionReceta(versionReceta)
                .build();
        LoteEntity lote = LoteEntity.builder()
                .id(id)
                .identificadorInterno("PILSEN_CLASICA-3")
                .estado(estado)
                .volumenObjetivo(300.0)
                .fechaInicioEstimada(LocalDate.of(2026, 11, 2))
                .fechaFinalizacionEstimada(LocalDate.of(2026, 11, 20))
                .planificacionProduccion(planificacion)
                .build();
        lote.setEtapas(etapas);
        return lote;
    }

    private static List<EtapaLoteEntity> seisEtapasConEstado(EstadoEtapaLote estado) {
        return seisEtapas(estado, estado, estado, estado, estado, estado);
    }

    private static List<EtapaLoteEntity> seisEtapas(EstadoEtapaLote molienda, EstadoEtapaLote maceracion, EstadoEtapaLote hervido,
                                                    EstadoEtapaLote fermentacion, EstadoEtapaLote maduracion, EstadoEtapaLote envasado) {
        FermentadorEntity fermentador = fermentador();
        List<EtapaLoteEntity> etapas = new ArrayList<>();
        etapas.add(etapa(TipoEtapa.MOLIENDA, molienda, molino()));
        etapas.add(etapa(TipoEtapa.MACERACION, maceracion, macerador()));
        etapas.add(etapa(TipoEtapa.HERVIDO, hervido, ollaHervor()));
        etapas.add(etapa(TipoEtapa.FERMENTACION, fermentacion, fermentador));
        etapas.add(etapa(TipoEtapa.MADURACION, maduracion, fermentador));
        etapas.add(etapa(TipoEtapa.ENVASADO, envasado, fermentador));
        return etapas;
    }

    private static EtapaLoteEntity etapa(TipoEtapa tipo, EstadoEtapaLote estado, EquipamientoEntity equipamiento) {
        return EtapaLoteEntity.builder().etapa(tipo).estado(estado).equipamiento(equipamiento).build();
    }

    private static MolinoEntity molino() {
        return MolinoEntity.builder().id(10L).identificadorInterno("MOL-01").build();
    }

    private static MaceradorEntity macerador() {
        return MaceradorEntity.builder().id(20L).identificadorInterno("MAC-02").build();
    }

    private static OllaHervorEntity ollaHervor() {
        return OllaHervorEntity.builder().id(30L).identificadorInterno("OLL-03").build();
    }

    private static FermentadorEntity fermentador() {
        return FermentadorEntity.builder().id(40L).identificadorInterno("FER-04").build();
    }
}
