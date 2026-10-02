package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EtapaLoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.LoteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ResumenEquipamientoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipamientoServicioImplTest {

    @Mock
    private IEquipamientoRepository equipamientoRepository;

    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

    @InjectMocks
    private EquipamientoServicioImpl equipamientoServicio;

    // ==================== filtrarEquipamientos ====================

    @Test
    @DisplayName("CP-FE-01: filtrarEquipamientos retorna una página de equipamientos correctamente mapeada a DTO cuando se filtra por los 3 criterios, traduciendo el tipo a su clase concreta")
    void filtrarEquipamientos_debeRetornarPaginaMapeadaFiltrandoPorLosTresCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EquipamientoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE);
        EquipamientoEntity otroMolinoEntity = crearMolinoEntity(2L, "MOL-02", EstadoOperativo.DISPONIBLE);
        when(equipamientoRepository.filtrarEquipamientos("MOL", MolinoEntity.class, EstadoOperativo.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(molinoEntity, otroMolinoEntity), pageable, 2));

        // === EJECUCION ===
        Page<EquipamientoResponseDTO> resultado = equipamientoServicio.filtrarEquipamientos("MOL", TipoEquipamiento.MOLINO, EstadoOperativo.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertEquipamientoDTO(molinoEntity, resultado.getContent().get(0));
        assertEquipamientoDTO(otroMolinoEntity, resultado.getContent().get(1));
        // Verifica que el service traduzca el enum a la Class concreta antes de pasarlo al repositorio
        verify(equipamientoRepository).filtrarEquipamientos("MOL", MolinoEntity.class, EstadoOperativo.DISPONIBLE, pageable);
    }

    @Test
    @DisplayName("CP-FE-02: filtrarEquipamientos propaga tipoClase nulo al repositorio cuando el tipo informado es nulo")
    void filtrarEquipamientos_debePropagarTipoClaseNuloCuandoTipoEsNulo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EquipamientoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE);
        when(equipamientoRepository.filtrarEquipamientos("MOL", null, EstadoOperativo.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(molinoEntity), pageable, 1));

        // === EJECUCION ===
        Page<EquipamientoResponseDTO> resultado = equipamientoServicio.filtrarEquipamientos("MOL", null, EstadoOperativo.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(equipamientoRepository).filtrarEquipamientos("MOL", null, EstadoOperativo.DISPONIBLE, pageable);
    }

    @Test
    @DisplayName("CP-FE-03: filtrarEquipamientos propaga identificadorInterno y estadoOperativo nulos sin restringir esos criterios")
    void filtrarEquipamientos_debePropagarIdentificadorYEstadoNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EquipamientoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE);
        EquipamientoEntity otroMolinoEntity = crearMolinoEntity(2L, "MOL-02", EstadoOperativo.EN_USO);
        when(equipamientoRepository.filtrarEquipamientos(null, MolinoEntity.class, null, pageable))
                .thenReturn(new PageImpl<>(List.of(molinoEntity, otroMolinoEntity), pageable, 2));

        // === EJECUCION ===
        Page<EquipamientoResponseDTO> resultado = equipamientoServicio.filtrarEquipamientos(null, TipoEquipamiento.MOLINO, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(equipamientoRepository).filtrarEquipamientos(null, MolinoEntity.class, null, pageable);
    }

    @Test
    @DisplayName("CP-FE-04: filtrarEquipamientos retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarEquipamientos_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(equipamientoRepository.filtrarEquipamientos("Inexistente", null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<EquipamientoResponseDTO> resultado = equipamientoServicio.filtrarEquipamientos("Inexistente", null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(equipamientoRepository).filtrarEquipamientos("Inexistente", null, null, pageable);
    }

    // ==================== obtenerResumen ====================

    @Test
    @DisplayName("CP-OR-01: obtenerResumen cuenta cada equipamiento en la combinación de su tipo concreto y su estado operativo")
    void obtenerResumen_debeContarPorTipoYEstado() {
        // === PREPARACION DE DATOS ===
        when(equipamientoRepository.listarActivos()).thenReturn(List.of(
                crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE),
                crearMolinoEntity(2L, "MOL-02", EstadoOperativo.DISPONIBLE),
                crearMolinoEntity(3L, "MOL-03", EstadoOperativo.EN_MANTENIMIENTO),
                crearFermentadorEntity(4L, "FER-01", EstadoOperativo.EN_USO),
                crearMaceradorEntity(5L, "MAC-01", EstadoOperativo.EN_LIMPIEZA)));

        // === EJECUCION ===
        Map<TipoEquipamiento, Map<EstadoOperativo, Long>> cantidades =
                equipamientoServicio.obtenerResumen().getCantidadesPorTipoYEstado();

        // === ASSERTS ===
        assertThat(cantidades.get(TipoEquipamiento.MOLINO).get(EstadoOperativo.DISPONIBLE)).isEqualTo(2L);
        assertThat(cantidades.get(TipoEquipamiento.MOLINO).get(EstadoOperativo.EN_MANTENIMIENTO)).isEqualTo(1L);
        assertThat(cantidades.get(TipoEquipamiento.FERMENTADOR).get(EstadoOperativo.EN_USO)).isEqualTo(1L);
        assertThat(cantidades.get(TipoEquipamiento.MACERADOR).get(EstadoOperativo.EN_LIMPIEZA)).isEqualTo(1L);
    }

    @Test
    @DisplayName("CP-OR-02: obtenerResumen no omite las combinaciones sin equipamientos: quedan en cero")
    void obtenerResumen_debeDejarEnCeroLasCombinacionesSinEquipamientos() {
        // === PREPARACION DE DATOS ===
        when(equipamientoRepository.listarActivos())
                .thenReturn(List.of(crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE)));

        // === EJECUCION ===
        ResumenEquipamientoResponseDTO resumen = equipamientoServicio.obtenerResumen();

        // === ASSERTS ===
        assertThat(sumaDeTodasLasCombinaciones(resumen)).isEqualTo(1L);
        assertCombinacionesCompletas(resumen);
        assertThat(resumen.getCantidadesPorTipoYEstado().get(TipoEquipamiento.MOLINO).get(EstadoOperativo.DISPONIBLE)).isEqualTo(1L);
        assertThat(resumen.getCantidadesPorTipoYEstado().get(TipoEquipamiento.OLLA_HERVOR).get(EstadoOperativo.EN_USO)).isZero();
    }

    @Test
    @DisplayName("CP-OR-03: obtenerResumen sin equipamientos retorna las 16 combinaciones en cero")
    void obtenerResumen_sinEquipamientosDebeRetornarTodoEnCero() {
        // === PREPARACION DE DATOS ===
        when(equipamientoRepository.listarActivos()).thenReturn(List.of());

        // === EJECUCION ===
        ResumenEquipamientoResponseDTO resumen = equipamientoServicio.obtenerResumen();

        // === ASSERTS ===
        assertCombinacionesCompletas(resumen);
        assertThat(sumaDeTodasLasCombinaciones(resumen)).isZero();
    }

    // ==================== filtrarResumenEquipamientos ====================

    @Test
    @DisplayName("CP-FR-01: filtrarResumenEquipamientos filtra por los 3 criterios traduciendo el tipo a su clase concreta y mapea la fila")
    void filtrarResumenEquipamientos_debeFiltrarPorLosTresCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EquipamientoEntity molino = crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE);
        when(equipamientoRepository.filtrarEquipamientos("MOL", MolinoEntity.class, EstadoOperativo.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(molino), pageable, 1));
        when(etapaLoteRepository.buscarEtapasEnCursoOPendientes(List.of(1L))).thenReturn(List.of());

        // === EJECUCION ===
        Page<EquipamientoResumenResponseDTO> resultado = equipamientoServicio
                .filtrarResumenEquipamientos("MOL", TipoEquipamiento.MOLINO, EstadoOperativo.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        EquipamientoResumenResponseDTO fila = resultado.getContent().get(0);
        assertThat(fila.getId()).isEqualTo(1L);
        assertThat(fila.getIdentificadorInterno()).isEqualTo("MOL-01");
        assertThat(fila.getTipo()).isEqualTo(TipoEquipamiento.MOLINO);
        assertThat(fila.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        verify(equipamientoRepository).filtrarEquipamientos("MOL", MolinoEntity.class, EstadoOperativo.DISPONIBLE, pageable);
    }

    @Test
    @DisplayName("CP-FR-02: filtrarResumenEquipamientos propaga los tres criterios nulos sin restringir la búsqueda")
    void filtrarResumenEquipamientos_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(equipamientoRepository.filtrarEquipamientos(null, null, null, pageable)).thenReturn(new PageImpl<>(
                List.of(crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE),
                        crearFermentadorEntity(2L, "FER-01", EstadoOperativo.EN_USO)), pageable, 2));
        when(etapaLoteRepository.buscarEtapasEnCursoOPendientes(List.of(1L, 2L))).thenReturn(List.of());

        // === EJECUCION ===
        Page<EquipamientoResumenResponseDTO> resultado =
                equipamientoServicio.filtrarResumenEquipamientos(null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).extracting(EquipamientoResumenResponseDTO::getTipo)
                .containsExactly(TipoEquipamiento.MOLINO, TipoEquipamiento.FERMENTADOR);
        verify(equipamientoRepository).filtrarEquipamientos(null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FR-03: un equipamiento en uso informa la etapa que tiene en curso y su lote")
    void filtrarResumenEquipamientos_debeInformarLaEtapaEnCurso() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FermentadorEntity fermentador = crearFermentadorEntity(4L, "FER-01", EstadoOperativo.EN_USO);
        when(equipamientoRepository.filtrarEquipamientos(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(fermentador), pageable, 1));
        when(etapaLoteRepository.buscarEtapasEnCursoOPendientes(List.of(4L))).thenReturn(List.of(
                crearEtapa(fermentador, 14L, "L-014", TipoEtapa.FERMENTACION, EstadoEtapaLote.EN_CURSO)));

        // === EJECUCION ===
        EquipamientoResumenResponseDTO fila = equipamientoServicio
                .filtrarResumenEquipamientos(null, null, null, pageable).getContent().get(0);

        // === ASSERTS ===
        assertThat(fila.getEtapaEnCurso()).isNotNull();
        assertThat(fila.getEtapaEnCurso().getIdLote()).isEqualTo(14L);
        assertThat(fila.getEtapaEnCurso().getIdentificadorInternoLote()).isEqualTo("L-014");
        assertThat(fila.getEtapaEnCurso().getEtapa()).isEqualTo(TipoEtapa.FERMENTACION);
        assertThat(fila.getEtapasPendientes()).isEmpty();
    }

    @Test
    @DisplayName("CP-FR-04: un equipamiento libre informa las etapas que tiene reservadas a futuro")
    void filtrarResumenEquipamientos_debeInformarLasEtapasPendientes() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MolinoEntity molino = crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE);
        when(equipamientoRepository.filtrarEquipamientos(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(molino), pageable, 1));
        when(etapaLoteRepository.buscarEtapasEnCursoOPendientes(List.of(1L))).thenReturn(List.of(
                crearEtapa(molino, 20L, "L-020", TipoEtapa.MOLIENDA, EstadoEtapaLote.PENDIENTE),
                crearEtapa(molino, 21L, "L-021", TipoEtapa.MOLIENDA, EstadoEtapaLote.PENDIENTE)));

        // === EJECUCION ===
        EquipamientoResumenResponseDTO fila = equipamientoServicio
                .filtrarResumenEquipamientos(null, null, null, pageable).getContent().get(0);

        // === ASSERTS ===
        assertThat(fila.getEtapaEnCurso()).isNull();
        assertThat(fila.getEtapasPendientes()).extracting("identificadorInternoLote")
                .containsExactly("L-020", "L-021");
        assertThat(fila.getEtapasPendientes()).extracting("etapa")
                .containsOnly(TipoEtapa.MOLIENDA);
    }

    @Test
    @DisplayName("CP-FR-05: un equipamiento sin participación no tiene etapa en curso ni pendientes")
    void filtrarResumenEquipamientos_equipamientoSinParticipacion() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(equipamientoRepository.filtrarEquipamientos(null, null, null, pageable)).thenReturn(
                new PageImpl<>(List.of(crearMaceradorEntity(5L, "MAC-01", EstadoOperativo.DISPONIBLE)), pageable, 1));
        when(etapaLoteRepository.buscarEtapasEnCursoOPendientes(List.of(5L))).thenReturn(List.of());

        // === EJECUCION ===
        EquipamientoResumenResponseDTO fila = equipamientoServicio
                .filtrarResumenEquipamientos(null, null, null, pageable).getContent().get(0);

        // === ASSERTS ===
        assertThat(fila.getEtapaEnCurso()).isNull();
        assertThat(fila.getEtapasPendientes()).isEmpty();
    }

    @Test
    @DisplayName("CP-FR-06: las etapas se reparten entre los equipamientos de la página con una sola consulta")
    void filtrarResumenEquipamientos_debeRepartirLasEtapasConUnaSolaConsulta() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MolinoEntity molino = crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE);
        FermentadorEntity fermentador = crearFermentadorEntity(4L, "FER-01", EstadoOperativo.EN_USO);
        when(equipamientoRepository.filtrarEquipamientos(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(molino, fermentador), pageable, 2));
        when(etapaLoteRepository.buscarEtapasEnCursoOPendientes(List.of(1L, 4L))).thenReturn(List.of(
                crearEtapa(molino, 20L, "L-020", TipoEtapa.MOLIENDA, EstadoEtapaLote.PENDIENTE),
                crearEtapa(fermentador, 14L, "L-014", TipoEtapa.FERMENTACION, EstadoEtapaLote.EN_CURSO)));

        // === EJECUCION ===
        List<EquipamientoResumenResponseDTO> filas = equipamientoServicio
                .filtrarResumenEquipamientos(null, null, null, pageable).getContent();

        // === ASSERTS ===
        assertThat(filas.get(0).getEtapaEnCurso()).isNull();
        assertThat(filas.get(0).getEtapasPendientes()).extracting("identificadorInternoLote").containsExactly("L-020");
        assertThat(filas.get(1).getEtapaEnCurso().getIdentificadorInternoLote()).isEqualTo("L-014");
        assertThat(filas.get(1).getEtapasPendientes()).isEmpty();
        verify(etapaLoteRepository, times(1)).buscarEtapasEnCursoOPendientes(anyCollection());
    }

    @Test
    @DisplayName("CP-FR-07: un mismo equipamiento con una etapa en curso y otras pendientes las separa según su estado")
    void filtrarResumenEquipamientos_debeSepararEnCursoYPendientes() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity macerador = crearMaceradorEntity(5L, "MAC-01", EstadoOperativo.EN_USO);
        when(equipamientoRepository.filtrarEquipamientos(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(macerador), pageable, 1));
        when(etapaLoteRepository.buscarEtapasEnCursoOPendientes(List.of(5L))).thenReturn(List.of(
                crearEtapa(macerador, 30L, "L-030", TipoEtapa.MACERACION, EstadoEtapaLote.EN_CURSO),
                crearEtapa(macerador, 31L, "L-031", TipoEtapa.MACERACION, EstadoEtapaLote.PENDIENTE)));

        // === EJECUCION ===
        EquipamientoResumenResponseDTO fila = equipamientoServicio
                .filtrarResumenEquipamientos(null, null, null, pageable).getContent().get(0);

        // === ASSERTS ===
        assertThat(fila.getEtapaEnCurso().getIdentificadorInternoLote()).isEqualTo("L-030");
        assertThat(fila.getEtapasPendientes()).extracting("identificadorInternoLote").containsExactly("L-031");
    }

    @Test
    @DisplayName("CP-FR-08: una consulta sin coincidencias retorna una página vacía y no consulta las etapas")
    void filtrarResumenEquipamientos_debeRetornarPaginaVaciaSinConsultarEtapas() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(equipamientoRepository.filtrarEquipamientos("Inexistente", null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<EquipamientoResumenResponseDTO> resultado =
                equipamientoServicio.filtrarResumenEquipamientos("Inexistente", null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verifyNoInteractions(etapaLoteRepository);
    }

    // ==================== helpers ====================

    private static FermentadorEntity crearFermentadorEntity(Long id, String identificadorInterno, EstadoOperativo estadoOperativo) {
        return FermentadorEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .estadoOperativo(estadoOperativo)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static MaceradorEntity crearMaceradorEntity(Long id, String identificadorInterno, EstadoOperativo estadoOperativo) {
        return MaceradorEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .estadoOperativo(estadoOperativo)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static EtapaLoteEntity crearEtapa(EquipamientoEntity equipamiento, Long idLote, String identificadorLote,
                                              TipoEtapa tipoEtapa, EstadoEtapaLote estadoEtapa) {
        return EtapaLoteEntity.builder()
                .equipamiento(equipamiento)
                .lote(LoteEntity.builder().id(idLote).identificadorInterno(identificadorLote).build())
                .etapa(tipoEtapa)
                .estado(estadoEtapa)
                .build();
    }

    private static long sumaDeTodasLasCombinaciones(ResumenEquipamientoResponseDTO resumen) {
        return resumen.getCantidadesPorTipoYEstado().values().stream()
                .flatMap(porEstado -> porEstado.values().stream())
                .mapToLong(Long::longValue)
                .sum();
    }

    private static void assertCombinacionesCompletas(ResumenEquipamientoResponseDTO resumen) {
        assertThat(resumen.getCantidadesPorTipoYEstado()).containsOnlyKeys(TipoEquipamiento.values());
        resumen.getCantidadesPorTipoYEstado().values()
                .forEach(porEstado -> assertThat(porEstado).containsOnlyKeys(EstadoOperativo.values()));
    }

    private static MolinoEntity crearMolinoEntity(Long id, String identificadorInterno, EstadoOperativo estadoOperativo) {
        return MolinoEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .descripcion("Molino de prueba")
                .estadoOperativo(estadoOperativo)
                .rendimientoMolienda(100.0)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static void assertEquipamientoDTO(EquipamientoEntity entidad, EquipamientoResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificadorInterno()).isEqualTo(entidad.getIdentificadorInterno());
        assertThat(dto.getDescripcion()).isEqualTo(entidad.getDescripcion());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
