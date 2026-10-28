package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ISecuenciaIdentificadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFabricanteBarrilRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnicoTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BarrilServicioImplTest {

    private static final Integer USOS_MAXIMOS_ANTES_MANTENIMIENTO = 200;

    @Mock
    private IBarrilRepository barrilRepository;

    @Mock
    private IFabricanteBarrilRepository fabricanteBarrilRepository;

    @Mock
    private ISecuenciaIdentificadorRepository secuenciaIdentificadorRepository;

    @InjectMocks
    private BarrilServicioImpl barrilServicio;

    // ==================== filtrarBarriles ====================

    @Test
    @DisplayName("CP-FB-01: filtrarBarriles retorna una página de barriles correctamente mapeada a DTO cuando se filtra por identificador interno, número de serie, capacidad, estado operativo y fabricante")
    void filtrarBarriles_debeRetornarPaginaMapeadaFiltrandoPorLosCincoCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        BarrilEntity otroBarrilEntity = crearBarrilEntity(2L, "BAR-02", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        when(barrilRepository.filtrarBarriles("BAR", null, 50.0, EstadoOperativoBarril.DISPONIBLE, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(barrilEntity, otroBarrilEntity), pageable, 2));

        // === EJECUCION ===
        Page<BarrilResponseDTO> resultado = barrilServicio.filtrarBarriles("BAR", null, 50.0, EstadoOperativoBarril.DISPONIBLE, 1L, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertBarrilDTO(barrilEntity, resultado.getContent().get(0));
        assertBarrilDTO(otroBarrilEntity, resultado.getContent().get(1));
        verify(barrilRepository).filtrarBarriles("BAR", null, 50.0, EstadoOperativoBarril.DISPONIBLE, 1L, pageable);
    }

    @Test
    @DisplayName("CP-FB-04: filtrarBarriles filtra solo por fabricante propagando el resto de los criterios como nulos")
    void filtrarBarriles_debeFiltrarSoloPorFabricante() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(7L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        when(barrilRepository.filtrarBarriles(null, null, null, null, 7L, pageable))
                .thenReturn(new PageImpl<>(List.of(barrilEntity), pageable, 1));

        // === EJECUCION ===
        Page<BarrilResponseDTO> resultado = barrilServicio.filtrarBarriles(null, null, null, null, 7L, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertBarrilDTO(barrilEntity, resultado.getContent().get(0));
        verify(barrilRepository).filtrarBarriles(null, null, null, null, 7L, pageable);
    }

    @Test
    @DisplayName("CP-FB-02: filtrarBarriles propaga identificador, capacidad, estado operativo y fabricante nulos sin restringir esos criterios")
    void filtrarBarriles_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        BarrilEntity otroBarrilEntity = crearBarrilEntity(2L, "BAR-02", 100.0, EstadoOperativoBarril.EN_LIMPIEZA, fabricanteEntity);
        when(barrilRepository.filtrarBarriles(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(barrilEntity, otroBarrilEntity), pageable, 2));

        // === EJECUCION ===
        Page<BarrilResponseDTO> resultado = barrilServicio.filtrarBarriles(null, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(barrilRepository).filtrarBarriles(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FB-03: filtrarBarriles retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarBarriles_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(barrilRepository.filtrarBarriles("INEXISTENTE", null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<BarrilResponseDTO> resultado = barrilServicio.filtrarBarriles("Inexistente", null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(barrilRepository).filtrarBarriles("INEXISTENTE", null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FB-05: filtrarBarriles normaliza el identificador buscado (espacios y mayúsculas) antes de consultar")
    void filtrarBarriles_debeNormalizarElIdentificadorBuscado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(barrilRepository.filtrarBarriles("KEG 01", null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        barrilServicio.filtrarBarriles("  keg   01  ", null, null, null, null, pageable);

        // === ASSERTS ===
        verify(barrilRepository).filtrarBarriles("KEG 01", null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FB-06: filtrarBarriles trata el identificador formado solo por espacios como ausencia de filtro")
    void filtrarBarriles_debeTratarIdentificadorEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(barrilRepository.filtrarBarriles(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        barrilServicio.filtrarBarriles("    ", null, null, null, null, pageable);

        // === ASSERTS ===
        verify(barrilRepository).filtrarBarriles(null, null, null, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del barril, incluyendo el fabricante, cuando el ID existe")
    void buscarPorId_debeRetornarBarrilExistente() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barrilEntity));

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertBarrilDTO(barrilEntity, resultado);
        verify(barrilRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para barriles dados de baja
        when(barrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barrilServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");
        verify(barrilRepository).findById(99L);
    }

    // ==================== altaBarril ====================

    @Test
    @DisplayName("CP-AB-01: altaBarril lanza ReglaNegocioException y no consulta la BD cuando la capacidad es nula")
    void altaBarril_debeRechazarCapacidadNula() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", null, 1L);

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad es obligatoria");

        verifyNoInteractions(fabricanteBarrilRepository);
        verifyNoInteractions(barrilRepository);
    }

    @Test
    @DisplayName("CP-AB-02: altaBarril lanza ReglaNegocioException y no consulta la BD cuando la capacidad es negativa")
    void altaBarril_debeRechazarCapacidadNegativa() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", -1.0, 1L);

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad debe ser mayor a 0");

        verifyNoInteractions(fabricanteBarrilRepository);
        verifyNoInteractions(barrilRepository);
    }

    @Test
    @DisplayName("CP-AB-03: altaBarril lanza ReglaNegocioException cuando la capacidad es igual a cero (valor límite)")
    void altaBarril_debeRechazarCapacidadIgualACero() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 0.0, 1L);

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad debe ser mayor a 0");

        verifyNoInteractions(fabricanteBarrilRepository);
        verifyNoInteractions(barrilRepository);
    }

    @Test
    @DisplayName("CP-AB-04: altaBarril lanza RecursoNoEncontradoException y no persiste cuando el fabricante de barril no existe")
    void altaBarril_debeRechazarFabricanteInexistente() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 99L);
        when(fabricanteBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fabricante de barril con ID: 99");

        verify(barrilRepository, never()).existsByNumeroSerieIgnoreCaseAndFabricanteId(any(), any());
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AB-05: altaBarril lanza RecursoDuplicadoException y no persiste cuando el número de serie ya está registrado para el mismo fabricante")
    void altaBarril_debeRechazarNumeroSerieDuplicadoParaElMismoFabricante() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 1L);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("BAR-01", 1L)).thenReturn(true);

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un barril con el número de serie 'BAR-01' para el fabricante seleccionado");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AB-06: altaBarril permite el mismo número de serie cuando pertenece a un fabricante distinto (camino feliz)")
    void altaBarril_debePermitirNumeroSerieRepetidoParaOtroFabricante() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 2L);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(2L);
        when(fabricanteBarrilRepository.findById(2L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("BAR-01", 2L)).thenReturn(false);
        when(secuenciaIdentificadorRepository.siguienteNumeroBarril()).thenReturn(1L);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> {
            BarrilEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(5L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.altaBarril(barrilFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<BarrilEntity> captor = ArgumentCaptor.forClass(BarrilEntity.class);
        verify(barrilRepository).save(captor.capture());
        assertThat(captor.getValue().getFabricante()).isEqualTo(fabricanteEntity);
        assertThat(resultado.getNumeroSerie()).isEqualTo("BAR-01");
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("BRL-01");
        assertThat(resultado.getFabricante().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("CP-AB-07: altaBarril persiste con el identificador interno generado y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaBarril_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 1L);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("BAR-01", 1L)).thenReturn(false);
        when(secuenciaIdentificadorRepository.siguienteNumeroBarril()).thenReturn(1L);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> {
            BarrilEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.altaBarril(barrilFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<BarrilEntity> captor = ArgumentCaptor.forClass(BarrilEntity.class);
        verify(barrilRepository).save(captor.capture());
        BarrilEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("BRL-01");
        assertThat(entidadCapturada.getNumeroSerie()).isEqualTo("BAR-01");
        assertThat(entidadCapturada.getCapacidad()).isEqualTo(50.0);
        assertThat(entidadCapturada.getContenidoActual()).isEqualTo(0.0);
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(entidadCapturada.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        assertThat(entidadCapturada.getFabricante()).isEqualTo(fabricanteEntity);
        // El alta siempre debe registrar al barril como ACTIVO, DISPONIBLE y sin contenido, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("BRL-01");
        assertThat(resultado.getNumeroSerie()).isEqualTo("BAR-01");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(resultado.getContenidoActual()).isEqualTo(0.0);
        assertThat(resultado.getEstado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    @DisplayName("CP-AB-08: altaBarril lanza ReglaNegocioException y no consulta la BD cuando los usos máximos antes de mantenimiento son nulos")
    void altaBarril_debeRechazarUsosMaximosNulos() {
        BarrilFormDTO barrilFormDTO = BarrilFormDTO.builder()
                .numeroSerie("BAR-01").capacidad(50.0).usosMaximosAntesMantenimiento(null).idFabricanteBarril(1L).build();

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento son obligatorios");

        verifyNoInteractions(fabricanteBarrilRepository);
        verifyNoInteractions(barrilRepository);
    }

    @Test
    @DisplayName("CP-AB-09: altaBarril lanza ReglaNegocioException cuando los usos máximos antes de mantenimiento son iguales a cero (valor límite)")
    void altaBarril_debeRechazarUsosMaximosIgualACero() {
        BarrilFormDTO barrilFormDTO = BarrilFormDTO.builder()
                .numeroSerie("BAR-01").capacidad(50.0).usosMaximosAntesMantenimiento(0).idFabricanteBarril(1L).build();

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(fabricanteBarrilRepository);
        verifyNoInteractions(barrilRepository);
    }

    @Test
    @DisplayName("CP-AB-16: altaBarril lanza ReglaNegocioException y no consulta la BD cuando la capacidad supera el máximo técnico")
    void altaBarril_debeRechazarCapacidadMayorAlMaximo() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 1000.5, 1L);

        // === EJECUCION Y ASERTOS ===
        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad no puede superar 1.000 litros");

        verifyNoInteractions(fabricanteBarrilRepository);
        verifyNoInteractions(barrilRepository);
    }

    @Test
    @DisplayName("CP-AB-17: altaBarril persiste el barril cuando la capacidad es igual al máximo técnico (valor límite)")
    void altaBarril_debePermitirCapacidadIgualAlMaximo() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 1000.0, 1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("BAR-01", 1L)).thenReturn(false);
        when(secuenciaIdentificadorRepository.siguienteNumeroBarril()).thenReturn(1L);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.altaBarril(barrilFormDTO);

        // === ASERTOS ===
        assertThat(resultado.getCapacidad()).isEqualTo(1000.0);
    }

    @Test
    @DisplayName("CP-AB-18: altaBarril lanza ReglaNegocioException y no consulta la BD cuando los usos máximos antes de mantenimiento superan el máximo técnico")
    void altaBarril_debeRechazarUsosMaximosMayoresAlMaximo() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO barrilFormDTO = BarrilFormDTO.builder()
                .numeroSerie("BAR-01").capacidad(50.0).usosMaximosAntesMantenimiento(100_001).idFabricanteBarril(1L).build();

        // === EJECUCION Y ASERTOS ===
        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verifyNoInteractions(fabricanteBarrilRepository);
        verifyNoInteractions(barrilRepository);
    }

    // ==================== modificarBarril ====================

    @Test
    @DisplayName("CP-MB-01: modificarBarril lanza RecursoNoEncontradoException y no valida nada más cuando el ID no existe")
    void modificarBarril_debeLanzarExcepcionSiNoExiste() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 1L);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barrilServicio.modificarBarril(99L, barrilFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");

        verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(99L);
        verifyNoInteractions(fabricanteBarrilRepository);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-02: modificarBarril lanza ReglaNegocioException y no consulta el fabricante cuando la capacidad es nula")
    void modificarBarril_debeRechazarCapacidadNula() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", null, 1L);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad es obligatoria");

        verifyNoInteractions(fabricanteBarrilRepository);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-03: modificarBarril lanza ReglaNegocioException cuando la capacidad es igual a cero (valor límite)")
    void modificarBarril_debeRechazarCapacidadIgualACero() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 0.0, 1L);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad debe ser mayor a 0");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-04: modificarBarril lanza RecursoNoEncontradoException y no persiste cuando el fabricante indicado no existe")
    void modificarBarril_debeRechazarFabricanteInexistente() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 99L);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));
        when(fabricanteBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fabricante de barril con ID: 99");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-05: modificarBarril lanza RecursoDuplicadoException y no persiste cuando el número de serie está en uso por otro barril del mismo fabricante")
    void modificarBarril_debeRechazarNumeroSerieEnUsoPorOtroBarrilDelMismoFabricante() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-02", 50.0, 1L);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot("BAR-02", 1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un barril con el número de serie 'BAR-02' para el fabricante seleccionado");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-06: modificarBarril permite conservar el número de serie y el fabricante propios y persiste (camino feliz)")
    void modificarBarril_debePermitirConservarNumeroSerieYFabricantePropios() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 60.0, 1L);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot("BAR-01", 1L, 1L)).thenReturn(false);
        when(barrilRepository.save(barrilEntity)).thenReturn(barrilEntity);

        BarrilResponseDTO resultado = barrilServicio.modificarBarril(1L, barrilFormDTO);

        assertThat(resultado.getNumeroSerie()).isEqualTo("BAR-01");
        assertThat(resultado.getCapacidad()).isEqualTo(60.0);
        assertThat(resultado.getFabricante().getId()).isEqualTo(1L);
        verify(barrilRepository).save(barrilEntity);
    }

    @Test
    @DisplayName("CP-MB-07: modificarBarril reasigna el fabricante y persiste cuando la reasignación es válida (camino feliz)")
    void modificarBarril_debeReasignarFabricanteExitosamente() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilEntity fabricanteOriginal = crearFabricanteBarrilEntity(1L);
        FabricanteBarrilEntity fabricanteNuevo = crearFabricanteBarrilEntity(2L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteOriginal);
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 2L);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));
        when(fabricanteBarrilRepository.findById(2L)).thenReturn(Optional.of(fabricanteNuevo));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot("BAR-01", 2L, 1L)).thenReturn(false);
        when(barrilRepository.save(barrilEntity)).thenReturn(barrilEntity);

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.modificarBarril(1L, barrilFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<BarrilEntity> captor = ArgumentCaptor.forClass(BarrilEntity.class);
        verify(barrilRepository).save(captor.capture());
        assertThat(captor.getValue().getFabricante()).isEqualTo(fabricanteNuevo);
        // La reasignación de fabricante no debe tocar el estado operativo ni el contenido actual
        assertThat(captor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(resultado.getFabricante().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("CP-MB-08: modificarBarril lanza ReglaNegocioException y no persiste cuando la capacidad es menor al contenido actual del barril")
    void modificarBarril_debeRechazarCapacidadMenorAlContenidoActual() {
        BarrilEntity barrilEntity = BarrilEntity.builder()
                .id(1L).identificadorInterno("BRL-01").numeroSerie("BAR-01").capacidad(50.0).contenidoActual(30.0)
                .estadoOperativo(EstadoOperativoBarril.CON_CERVEZA).usosMaximosAntesMantenimiento(USOS_MAXIMOS_ANTES_MANTENIMIENTO)
                .estado(Estado.ACTIVO).fabricante(crearFabricanteBarrilEntity(1L)).build();
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 20.0, 1L);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad no puede ser menor al contenido actual del barril (30.0 L)");

        verifyNoInteractions(fabricanteBarrilRepository);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-09: modificarBarril lanza ReglaNegocioException y no consulta el fabricante cuando los usos máximos antes de mantenimiento son nulos")
    void modificarBarril_debeRechazarUsosMaximosNulos() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        BarrilFormDTO barrilFormDTO = BarrilFormDTO.builder()
                .numeroSerie("BAR-01").capacidad(50.0).usosMaximosAntesMantenimiento(null).idFabricanteBarril(1L).build();
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento son obligatorios");

        verifyNoInteractions(fabricanteBarrilRepository);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-15: modificarBarril lanza ReglaNegocioException y no consulta el fabricante cuando la capacidad supera el máximo técnico")
    void modificarBarril_debeRechazarCapacidadMayorAlMaximo() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 1000.5, 1L);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));

        // === EJECUCION Y ASERTOS ===
        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad no puede superar 1.000 litros");

        verifyNoInteractions(fabricanteBarrilRepository);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-16: modificarBarril persiste el barril cuando la capacidad es igual al máximo técnico (valor límite)")
    void modificarBarril_debePermitirCapacidadIgualAlMaximo() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilEntity fabricante = crearFabricanteBarrilEntity(1L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricante);
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 1000.0, 1L);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricante));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot("BAR-01", 1L, 1L)).thenReturn(false);
        when(barrilRepository.save(barrilEntity)).thenReturn(barrilEntity);

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.modificarBarril(1L, barrilFormDTO);

        // === ASERTOS ===
        assertThat(resultado.getCapacidad()).isEqualTo(1000.0);
        assertThat(barrilEntity.getCapacidad()).isEqualTo(1000.0);
    }

    @Test
    @DisplayName("CP-MB-17: modificarBarril lanza ReglaNegocioException y no consulta el fabricante cuando los usos máximos antes de mantenimiento superan el máximo técnico")
    void modificarBarril_debeRechazarUsosMaximosMayoresAlMaximo() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        BarrilFormDTO barrilFormDTO = BarrilFormDTO.builder()
                .numeroSerie("BAR-01").capacidad(50.0).usosMaximosAntesMantenimiento(100_001).idFabricanteBarril(1L).build();
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));

        // === EJECUCION Y ASERTOS ===
        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verifyNoInteractions(fabricanteBarrilRepository);
        verify(barrilRepository, never()).save(any());
    }

    // ==================== bajaBarril ====================

    @Test
    @DisplayName("CP-BB-01: bajaBarril lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaBarril_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barrilServicio.bajaBarril(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");

        verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(99L);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BB-02: bajaBarril lanza ReglaNegocioException y no persiste cuando el barril está en estado operativo CON_CERVEZA")
    void bajaBarril_debeRechazarEstadoOperativoConCerveza() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.CON_CERVEZA, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.bajaBarril(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el barril porque no se encuentra disponible");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BB-03: bajaBarril lanza ReglaNegocioException y no persiste cuando el barril está en estado operativo DESPACHADO")
    void bajaBarril_debeRechazarEstadoOperativoDespachado() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DESPACHADO, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.bajaBarril(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el barril porque no se encuentra disponible");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BB-05: bajaBarril lanza ReglaNegocioException y no persiste cuando el barril está en estado operativo EN_LIMPIEZA")
    void bajaBarril_debeRechazarEstadoOperativoEnLimpieza() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.EN_LIMPIEZA, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.bajaBarril(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el barril porque no se encuentra disponible");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BB-06: bajaBarril lanza ReglaNegocioException y no persiste cuando el barril está en estado operativo EN_MANTENIMIENTO")
    void bajaBarril_debeRechazarEstadoOperativoEnMantenimiento() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.EN_MANTENIMIENTO, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.bajaBarril(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el barril porque no se encuentra disponible");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BB-04: bajaBarril marca el estado como BAJA, persiste y retorna el DTO cuando el barril está en estado operativo DISPONIBLE")
    void bajaBarril_debeMarcarBajaYRetornarBarrilExistente() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barrilEntity));
        when(barrilRepository.save(barrilEntity)).thenReturn(barrilEntity);

        BarrilResponseDTO resultado = barrilServicio.bajaBarril(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(barrilEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertBarrilDTO(barrilEntity, resultado);
        verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        verify(barrilRepository).save(barrilEntity);
        verify(barrilRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static FabricanteBarrilEntity crearFabricanteBarrilEntity(Long id) {
        return FabricanteBarrilEntity.builder()
                .id(id)
                .nombre("Tonelería de prueba")
                .estado(Estado.ACTIVO)
                .build();
    }

    private static BarrilEntity crearBarrilEntity(Long id, String numeroSerie, Double capacidad, EstadoOperativoBarril estadoOperativo, FabricanteBarrilEntity fabricante) {
        return BarrilEntity.builder()
                .id(id)
                .identificadorInterno("BRL-0" + id)
                .numeroSerie(numeroSerie)
                .capacidad(capacidad)
                .contenidoActual(0.0)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(USOS_MAXIMOS_ANTES_MANTENIMIENTO)
                .estado(Estado.ACTIVO)
                .fabricante(fabricante)
                .build();
    }

    private static BarrilFormDTO barrilFormDTO(String numeroSerie, Double capacidad, Long idFabricanteBarril) {
        return BarrilFormDTO.builder()
                .numeroSerie(numeroSerie)
                .capacidad(capacidad)
                .usosMaximosAntesMantenimiento(USOS_MAXIMOS_ANTES_MANTENIMIENTO)
                .idFabricanteBarril(idFabricanteBarril)
                .build();
    }

    private static void assertBarrilDTO(BarrilEntity entidad, BarrilResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificadorInterno()).isEqualTo(entidad.getIdentificadorInterno());
        assertThat(dto.getNumeroSerie()).isEqualTo(entidad.getNumeroSerie());
        assertThat(dto.getCapacidad()).isEqualTo(entidad.getCapacidad());
        assertThat(dto.getContenidoActual()).isEqualTo(entidad.getContenidoActual());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getUsosMaximosAntesMantenimiento()).isEqualTo(entidad.getUsosMaximosAntesMantenimiento());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
        assertThat(dto.getFabricante().getId()).isEqualTo(entidad.getFabricante().getId());
    }

    // ==================== normalización y unicidad ====================

    @Test
    @DisplayName("CP-AB-10: altaBarril guarda el número de serie normalizado (sin espacios de más y conservando las mayúsculas) y consulta la unicidad con ese valor")
    void altaBarril_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO formDTO = barrilFormDTO("  bar   01 ", 50.0, 1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("bar 01", 1L)).thenReturn(false);
        when(secuenciaIdentificadorRepository.siguienteNumeroBarril()).thenReturn(1L);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        barrilServicio.altaBarril(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<BarrilEntity> captor = ArgumentCaptor.forClass(BarrilEntity.class);
        verify(barrilRepository).save(captor.capture());
        assertThat(captor.getValue().getNumeroSerie()).isEqualTo("bar 01");
        assertThat(formDTO.getNumeroSerie()).isEqualTo("  bar   01 ");
        verify(barrilRepository).existsByNumeroSerieIgnoreCaseAndFabricanteId("bar 01", 1L);
        verify(barrilRepository).flush();
    }

    @Test
    @DisplayName("CP-AB-11: altaBarril lanza ReglaNegocioException y no consulta el repositorio cuando el número de serie está vacío")
    void altaBarril_debeRechazarValorVacio() {
        BarrilFormDTO formDTO = barrilFormDTO("   ", 50.0, 1L);

        assertThatThrownBy(() -> barrilServicio.altaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El número de serie es obligatorio");

        verifyNoInteractions(barrilRepository);
    }

    @Test
    @DisplayName("CP-AB-12: altaBarril lanza RecursoDuplicadoException cuando el número de serie normalizado ya existe")
    void altaBarril_debeDetectarDuplicadoConElValorNormalizado() {
        BarrilFormDTO formDTO = barrilFormDTO("  bar-01 ", 50.0, 1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("bar-01", 1L)).thenReturn(true);

        assertThatThrownBy(() -> barrilServicio.altaBarril(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un barril con el número de serie 'bar-01' para el fabricante seleccionado");

        verify(barrilRepository).existsByNumeroSerieIgnoreCaseAndFabricanteId("bar-01", 1L);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AB-13: altaBarril traduce la violación del índice único udx_barril_numero_serie al mensaje de duplicado")
    void altaBarril_debeTraducirLaViolacionDelIndiceUnico() {
        BarrilFormDTO formDTO = barrilFormDTO("bar 01", 50.0, 1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("bar 01", 1L)).thenReturn(false);
        when(secuenciaIdentificadorRepository.siguienteNumeroBarril()).thenReturn(1L);
        when(barrilRepository.save(any(BarrilEntity.class))).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_barril_numero_serie"));

        assertThatThrownBy(() -> barrilServicio.altaBarril(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un barril con el número de serie 'bar 01' para el fabricante seleccionado");
    }

    @Test
    @DisplayName("CP-AB-14: altaBarril permite reutilizar el número de serie de un registro dado de baja")
    void altaBarril_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        BarrilFormDTO formDTO = barrilFormDTO("bar 01", 50.0, 1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        // El único registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("bar 01", 1L)).thenReturn(false);
        when(secuenciaIdentificadorRepository.siguienteNumeroBarril()).thenReturn(1L);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> barrilServicio.altaBarril(formDTO)).doesNotThrowAnyException();

        verify(barrilRepository).save(any(BarrilEntity.class));
    }

    @Test
    @DisplayName("CP-AB-15: altaBarril permite un número de serie que difiere de otro solo en la puntuación")
    void altaBarril_debeTratarComoDistintosLosNumerosDeSerieConDistintaPuntuacion() {
        BarrilFormDTO formDTO = barrilFormDTO("bar 01", 50.0, 1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("bar 01", 1L)).thenReturn(false);
        when(secuenciaIdentificadorRepository.siguienteNumeroBarril()).thenReturn(1L);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> barrilServicio.altaBarril(formDTO)).doesNotThrowAnyException();

        verify(barrilRepository).existsByNumeroSerieIgnoreCaseAndFabricanteId("bar 01", 1L);
        verify(barrilRepository).save(any(BarrilEntity.class));
    }

    @Test
    @DisplayName("CP-MB-10: modificarBarril guarda el número de serie normalizado conservando las mayúsculas y consulta la unicidad con ese valor")
    void modificarBarril_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO formDTO = barrilFormDTO("  bar   01 ", 50.0, 1L);
        BarrilEntity existente = crearBarrilEntity(1L, "Original", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(existente));
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot("bar 01", 1L, 1L)).thenReturn(false);
        when(barrilRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        barrilServicio.modificarBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(existente.getNumeroSerie()).isEqualTo("bar 01");
        assertThat(formDTO.getNumeroSerie()).isEqualTo("  bar   01 ");
        verify(barrilRepository).existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot("bar 01", 1L, 1L);
        verify(barrilRepository).save(existente);
        verify(barrilRepository).flush();
    }

    @Test
    @DisplayName("CP-MB-11: modificarBarril lanza ReglaNegocioException cuando el número de serie está vacío")
    void modificarBarril_debeRechazarValorVacio() {
        BarrilFormDTO formDTO = barrilFormDTO("   ", 50.0, 1L);
        BarrilEntity existente = crearBarrilEntity(1L, "Original", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El número de serie es obligatorio");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-12: modificarBarril lanza RecursoDuplicadoException cuando el número de serie normalizado está en uso por otro registro")
    void modificarBarril_debeDetectarDuplicadoConElValorNormalizado() {
        BarrilFormDTO formDTO = barrilFormDTO("  bar-01 ", 50.0, 1L);
        BarrilEntity existente = crearBarrilEntity(1L, "Original", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(existente));
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot("bar-01", 1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un barril con el número de serie 'bar-01' para el fabricante seleccionado");

        verify(barrilRepository).existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot("bar-01", 1L, 1L);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-13: modificarBarril traduce la violación del índice único udx_barril_numero_serie al mensaje de duplicado")
    void modificarBarril_debeTraducirLaViolacionDelIndiceUnico() {
        BarrilFormDTO formDTO = barrilFormDTO("bar 01", 50.0, 1L);
        BarrilEntity existente = crearBarrilEntity(1L, "Original", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(existente));
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot("bar 01", 1L, 1L)).thenReturn(false);
        when(barrilRepository.save(existente)).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_barril_numero_serie"));

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un barril con el número de serie 'bar 01' para el fabricante seleccionado");
    }

    @Test
    @DisplayName("CP-MB-14: modificarBarril permite reutilizar el número de serie de un registro dado de baja")
    void modificarBarril_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        BarrilFormDTO formDTO = barrilFormDTO("bar 01", 50.0, 1L);
        BarrilEntity existente = crearBarrilEntity(1L, "Original", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(existente));
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        // El único otro registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot("bar 01", 1L, 1L)).thenReturn(false);
        when(barrilRepository.save(existente)).thenReturn(existente);

        assertThatCode(() -> barrilServicio.modificarBarril(1L, formDTO)).doesNotThrowAnyException();

        assertThat(existente.getNumeroSerie()).isEqualTo("bar 01");
        verify(barrilRepository).save(existente);
    }

    // ==================== identificador interno generado y número de serie ====================

    @Test
    @DisplayName("CP-FB-07: filtrarBarriles normaliza el número de serie buscado (espacios de más) conservando las mayúsculas")
    void filtrarBarriles_debeNormalizarElNumeroDeSerieBuscadoSinCambiarMayusculas() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(barrilRepository.filtrarBarriles(null, "sn 01", null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        barrilServicio.filtrarBarriles(null, "  sn   01  ", null, null, null, pageable);

        // === ASSERTS ===
        verify(barrilRepository).filtrarBarriles(null, "sn 01", null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FB-08: filtrarBarriles trata el número de serie formado solo por espacios como ausencia de filtro")
    void filtrarBarriles_debeTratarNumeroDeSerieEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(barrilRepository.filtrarBarriles(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        barrilServicio.filtrarBarriles(null, "    ", null, null, null, pageable);

        // === ASSERTS ===
        verify(barrilRepository).filtrarBarriles(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-AB-19: altaBarril asigna el identificador interno generado a partir de la secuencia (BRL-07)")
    void altaBarril_debeAsignarElIdentificadorInternoGenerado() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO formDTO = barrilFormDTO("SN-2024-001", 50.0, 1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("SN-2024-001", 1L)).thenReturn(false);
        when(secuenciaIdentificadorRepository.siguienteNumeroBarril()).thenReturn(7L);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.altaBarril(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("BRL-07");
        assertThat(resultado.getNumeroSerie()).isEqualTo("SN-2024-001");
    }

    @Test
    @DisplayName("CP-AB-20: altaBarril conserva las mayúsculas y minúsculas del número de serie y solo recorta los espacios (sn-ab12 queda sn-ab12)")
    void altaBarril_debeConservarMayusculasDelNumeroDeSerie() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO formDTO = barrilFormDTO("  sn-ab12  ", 50.0, 1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("sn-ab12", 1L)).thenReturn(false);
        when(secuenciaIdentificadorRepository.siguienteNumeroBarril()).thenReturn(1L);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.altaBarril(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getNumeroSerie()).isEqualTo("sn-ab12");
    }

    @Test
    @DisplayName("CP-AB-21: altaBarril lanza ReglaNegocioException cuando el número de serie supera los 255 caracteres")
    void altaBarril_debeRechazarNumeroSerieMayorAlMaximo() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO formDTO = barrilFormDTO("S".repeat(256), 50.0, 1L);

        // === EJECUCION Y ASERTOS ===
        assertThatThrownBy(() -> barrilServicio.altaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El número de serie no puede superar 255 caracteres");

        verifyNoInteractions(secuenciaIdentificadorRepository);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AB-22: altaBarril persiste el barril cuando el número de serie tiene exactamente 255 caracteres (valor límite)")
    void altaBarril_debePermitirNumeroSerieIgualAlMaximo() {
        // === PREPARACION DE DATOS ===
        String numeroSerie = "S".repeat(255);
        BarrilFormDTO formDTO = barrilFormDTO(numeroSerie, 50.0, 1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId(numeroSerie, 1L)).thenReturn(false);
        when(secuenciaIdentificadorRepository.siguienteNumeroBarril()).thenReturn(1L);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.altaBarril(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getNumeroSerie()).hasSize(255);
    }

    @Test
    @DisplayName("CP-AB-23: altaBarril no pide un número de la secuencia cuando falla cualquier validación (número de serie vacío, capacidad, usos, fabricante inexistente o número de serie duplicado)")
    void altaBarril_noDebePedirNumeroDeLaSecuenciaSiFallaUnaValidacion() {
        // === PREPARACION DE DATOS ===
        when(fabricanteBarrilRepository.findById(99L)).thenReturn(Optional.empty());
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("DUP-01", 1L)).thenReturn(true);

        // === EJECUCION Y ASERTOS ===
        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO("   ", 50.0, 1L))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO("SN-01", null, 1L))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> barrilServicio.altaBarril(BarrilFormDTO.builder().numeroSerie("SN-01").capacidad(50.0)
                .usosMaximosAntesMantenimiento(0).idFabricanteBarril(1L).build())).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO("SN-01", 50.0, 99L))).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO("DUP-01", 50.0, 1L))).isInstanceOf(RecursoDuplicadoException.class);

        verifyNoInteractions(secuenciaIdentificadorRepository);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AB-24: altaBarril pide el número de la secuencia después de todas las validaciones y justo antes de guardar")
    void altaBarril_debePedirElNumeroDeLaSecuenciaComoUltimoPasoAntesDeGuardar() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO formDTO = barrilFormDTO("SN-01", 50.0, 1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(crearFabricanteBarrilEntity(1L)));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteId("SN-01", 1L)).thenReturn(false);
        when(secuenciaIdentificadorRepository.siguienteNumeroBarril()).thenReturn(1L);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        barrilServicio.altaBarril(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(fabricanteBarrilRepository, barrilRepository, secuenciaIdentificadorRepository);
        orden.verify(fabricanteBarrilRepository).findById(1L);
        orden.verify(barrilRepository).existsByNumeroSerieIgnoreCaseAndFabricanteId("SN-01", 1L);
        orden.verify(secuenciaIdentificadorRepository).siguienteNumeroBarril();
        orden.verify(barrilRepository).save(any(BarrilEntity.class));
    }

    @Test
    @DisplayName("CP-MB-18: modificarBarril no cambia el identificador interno ni pide un número de la secuencia")
    void modificarBarril_noDebeCambiarElIdentificadorInterno() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilEntity fabricante = crearFabricanteBarrilEntity(1L);
        BarrilEntity existente = crearBarrilEntity(1L, "Original", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricante);
        BarrilFormDTO formDTO = barrilFormDTO("Nuevo-SN", 50.0, 1L);
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(existente));
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricante));
        when(barrilRepository.existsByNumeroSerieIgnoreCaseAndFabricanteIdAndIdNot("Nuevo-SN", 1L, 1L)).thenReturn(false);
        when(barrilRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.modificarBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(existente.getIdentificadorInterno()).isEqualTo("BRL-01");
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("BRL-01");
        assertThat(resultado.getNumeroSerie()).isEqualTo("Nuevo-SN");
        verifyNoInteractions(secuenciaIdentificadorRepository);
    }
}
