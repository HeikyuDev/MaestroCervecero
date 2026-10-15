package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FermentadorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnicoTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FermentadorServicioImplTest {

    private static final int USOS_MAXIMOS_ANTES_MANTENIMIENTO = 500;

    @Mock
    private IFermentadorRepository fermentadorRepository;

    @Mock
    private IEquipamientoRepository equipamientoRepository;

    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

    @InjectMocks
    private FermentadorServicioImpl fermentadorServicio;

    // ==================== filtrarFermentadores ====================

    @Test
    @DisplayName("CP-FF-01: filtrarFermentadores retorna una página de fermentadores correctamente mapeada a DTO cuando se filtra por identificador interno y estado operativo")
    void filtrarFermentadores_debeRetornarPaginaMapeadaFiltrandoPorIdentificadorYEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 100.0, 80.0);
        FermentadorEntity otraFermentadorEntity = crearFermentadorEntity(2L, "FERM-02", 120.0, 90.0);
        when(fermentadorRepository.filtrarFermentadores("FERM", EstadoOperativo.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(fermentadorEntity, otraFermentadorEntity), pageable, 2));

        // === EJECUCION ===
        Page<FermentadorResponseDTO> resultado = fermentadorServicio.filtrarFermentadores("FERM", EstadoOperativo.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertFermentadorDTO(fermentadorEntity, resultado.getContent().get(0));
        assertFermentadorDTO(otraFermentadorEntity, resultado.getContent().get(1));
        verify(fermentadorRepository).filtrarFermentadores("FERM", EstadoOperativo.DISPONIBLE, pageable);
    }

    @Test
    @DisplayName("CP-FF-02: filtrarFermentadores propaga identificador interno y estado operativo nulos sin restringir esos criterios")
    void filtrarFermentadores_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 100.0, 80.0);
        FermentadorEntity otraFermentadorEntity = crearFermentadorEntity(2L, "FERM-02", 120.0, 90.0);
        when(fermentadorRepository.filtrarFermentadores(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(fermentadorEntity, otraFermentadorEntity), pageable, 2));

        // === EJECUCION ===
        Page<FermentadorResponseDTO> resultado = fermentadorServicio.filtrarFermentadores(null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(fermentadorRepository).filtrarFermentadores(null, null, pageable);
    }

    @Test
    @DisplayName("CP-FF-03: filtrarFermentadores retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarFermentadores_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(fermentadorRepository.filtrarFermentadores("INEXISTENTE", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<FermentadorResponseDTO> resultado = fermentadorServicio.filtrarFermentadores("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(fermentadorRepository).filtrarFermentadores("INEXISTENTE", null, pageable);
    }

    @Test
    @DisplayName("CP-FF-04: filtrarFermentadores normaliza el identificador interno buscado (espacios y mayúsculas) antes de consultar")
    void filtrarFermentadores_debeNormalizarElIdentificadorBuscado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(fermentadorRepository.filtrarFermentadores("FERM 01", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        fermentadorServicio.filtrarFermentadores("  ferm   01  ", null, pageable);

        // === ASSERTS ===
        verify(fermentadorRepository).filtrarFermentadores("FERM 01", null, pageable);
    }

    @Test
    @DisplayName("CP-FF-05: filtrarFermentadores trata el identificador interno formado solo por espacios como ausencia de filtro")
    void filtrarFermentadores_debeTratarIdentificadorEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(fermentadorRepository.filtrarFermentadores(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        fermentadorServicio.filtrarFermentadores("    ", null, pageable);

        // === ASSERTS ===
        verify(fermentadorRepository).filtrarFermentadores(null, null, pageable);
    }

    // ==================== filtrarFermentadoresConDadosDeBajaConLotes ====================

    @Test
    @DisplayName("CP-FD-01: filtrarFermentadoresConDadosDeBajaConLotes retorna una página de fermentadores (activos y dados de baja con lotes) mapeada a DTO cuando se filtra por identificador interno y estado operativo")
    void filtrarFermentadoresConDadosDeBajaConLotes_debeRetornarPaginaMapeadaFiltrandoPorIdentificadorYEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 8);
        FermentadorEntity activo = crearFermentadorEntity(1L, "F-01", 100.0, 80.0);
        FermentadorEntity dadoDeBaja = crearFermentadorEntity(2L, "F-02", 120.0, 90.0);
        dadoDeBaja.setEstado(Estado.BAJA);
        when(fermentadorRepository.filtrarFermentadoresConDadosDeBajaConLotes("F-0", EstadoOperativo.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(activo, dadoDeBaja), pageable, 2));

        // === EJECUCION ===
        Page<FermentadorResponseDTO> resultado = fermentadorServicio.filtrarFermentadoresConDadosDeBajaConLotes("F-0", EstadoOperativo.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        assertFermentadorDTO(activo, resultado.getContent().get(0));
        assertFermentadorDTO(dadoDeBaja, resultado.getContent().get(1));
        assertThat(resultado.getContent()).extracting(FermentadorResponseDTO::getEstado).containsExactly(Estado.ACTIVO, Estado.BAJA);
        verify(fermentadorRepository).filtrarFermentadoresConDadosDeBajaConLotes("F-0", EstadoOperativo.DISPONIBLE, pageable);
    }

    @Test
    @DisplayName("CP-FD-02: filtrarFermentadoresConDadosDeBajaConLotes propaga identificador interno y estado operativo nulos sin restringir esos criterios")
    void filtrarFermentadoresConDadosDeBajaConLotes_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 8);
        when(fermentadorRepository.filtrarFermentadoresConDadosDeBajaConLotes(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<FermentadorResponseDTO> resultado = fermentadorServicio.filtrarFermentadoresConDadosDeBajaConLotes(null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(fermentadorRepository).filtrarFermentadoresConDadosDeBajaConLotes(null, null, pageable);
    }

    @Test
    @DisplayName("CP-FD-03: filtrarFermentadoresConDadosDeBajaConLotes normaliza el identificador interno buscado (espacios y mayúsculas) antes de consultar")
    void filtrarFermentadoresConDadosDeBajaConLotes_debeNormalizarElIdentificadorBuscado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 8);
        when(fermentadorRepository.filtrarFermentadoresConDadosDeBajaConLotes("FERM 01", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        fermentadorServicio.filtrarFermentadoresConDadosDeBajaConLotes("  ferm   01  ", null, pageable);

        // === ASSERTS ===
        verify(fermentadorRepository).filtrarFermentadoresConDadosDeBajaConLotes("FERM 01", null, pageable);
    }

    @Test
    @DisplayName("CP-FD-04: filtrarFermentadoresConDadosDeBajaConLotes trata el identificador interno formado solo por espacios como ausencia de filtro")
    void filtrarFermentadoresConDadosDeBajaConLotes_debeTratarIdentificadorEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 8);
        when(fermentadorRepository.filtrarFermentadoresConDadosDeBajaConLotes(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        fermentadorServicio.filtrarFermentadoresConDadosDeBajaConLotes("    ", null, pageable);

        // === ASSERTS ===
        verify(fermentadorRepository).filtrarFermentadoresConDadosDeBajaConLotes(null, null, pageable);
    }

    // ==================== listarDadosDeBajaConLotes ====================

    @Test
    @DisplayName("CP-LB-01: listarDadosDeBajaConLotes mapea a DTO los fermentadores dados de baja con lotes que devuelve el repositorio, en su orden")
    void listarDadosDeBajaConLotes_debeMapearLosFermentadoresDelRepositorio() {
        // === PREPARACION DE DATOS ===
        FermentadorEntity primero = crearFermentadorEntity(2L, "F-02", 100.0, 80.0);
        primero.setEstado(Estado.BAJA);
        FermentadorEntity segundo = crearFermentadorEntity(5L, "F-05", 120.0, 90.0);
        segundo.setEstado(Estado.BAJA);
        when(fermentadorRepository.buscarDadosDeBajaConLotes()).thenReturn(List.of(primero, segundo));

        // === EJECUCION ===
        List<FermentadorResponseDTO> resultado = fermentadorServicio.listarDadosDeBajaConLotes();

        // === ASSERTS ===
        assertThat(resultado).hasSize(2);
        assertFermentadorDTO(primero, resultado.get(0));
        assertFermentadorDTO(segundo, resultado.get(1));
        assertThat(resultado).extracting(FermentadorResponseDTO::getEstado).containsOnly(Estado.BAJA);
    }

    @Test
    @DisplayName("CP-LB-02: listarDadosDeBajaConLotes retorna una lista vacía cuando no hay fermentadores dados de baja con lotes")
    void listarDadosDeBajaConLotes_debeRetornarListaVaciaSinFermentadores() {
        // === PREPARACION DE DATOS ===
        when(fermentadorRepository.buscarDadosDeBajaConLotes()).thenReturn(List.of());

        // === EJECUCION ===
        List<FermentadorResponseDTO> resultado = fermentadorServicio.listarDadosDeBajaConLotes();

        // === ASSERTS ===
        assertThat(resultado).isNotNull().isEmpty();
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("buscarPorId retorna el DTO del fermentador cuando el ID existe")
    void buscarPorId_debeRetornarFermentadorExistente() {
        // === PREPARACION DE DATOS ===
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 100.0, 80.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));

        // === EJECUCION ===
        FermentadorResponseDTO resultado = fermentadorServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertFermentadorDTO(fermentadorEntity, resultado);
        verify(fermentadorRepository).findById(1L);
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para fermentadores dados de baja
        when(fermentadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fermentadorServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fermentador con ID: 99");
        verify(fermentadorRepository).findById(99L);
    }

    // ==================== buscarActivoODadoDeBajaConLotesPorId ====================

    @Test
    @DisplayName("CP-BB-01: buscarActivoODadoDeBajaConLotesPorId retorna el DTO de un fermentador dado de baja con lotes sin tocar findById")
    void buscarActivoODadoDeBajaConLotesPorId_debeRetornarFermentadorDadoDeBaja() {
        // === PREPARACION DE DATOS ===
        FermentadorEntity dadoDeBaja = crearFermentadorEntity(3L, "FERM-99", 100.0, 80.0);
        dadoDeBaja.setEstado(Estado.BAJA);
        when(fermentadorRepository.buscarActivoODadoDeBajaConLotesPorId(3L)).thenReturn(Optional.of(dadoDeBaja));

        // === EJECUCION ===
        FermentadorResponseDTO resultado = fermentadorServicio.buscarActivoODadoDeBajaConLotesPorId(3L);

        // === VERIFICACION ===
        assertFermentadorDTO(dadoDeBaja, resultado);
        assertThat(resultado.getEstado()).isEqualTo(Estado.BAJA);
        verify(fermentadorRepository).buscarActivoODadoDeBajaConLotesPorId(3L);
        verify(fermentadorRepository, never()).findById(any());
    }

    @Test
    @DisplayName("CP-BB-02: buscarActivoODadoDeBajaConLotesPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja sin lotes")
    void buscarActivoODadoDeBajaConLotesPorId_debeLanzarExcepcionSiNoExiste() {
        when(fermentadorRepository.buscarActivoODadoDeBajaConLotesPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fermentadorServicio.buscarActivoODadoDeBajaConLotesPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fermentador con ID: 99");
        verify(fermentadorRepository).buscarActivoODadoDeBajaConLotesPorId(99L);
    }

    // ==================== altaFermentador ====================

    @Test
    @DisplayName("altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es mayor a la total")
    void altaFermentador_debeRechazarCapacidadUtilMayorATotal() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 120.0);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("altaFermentador lanza ReglaNegocioException cuando la capacidad útil es igual a la total (valor límite)")
    void altaFermentador_debeRechazarCapacidadUtilIgualATotal() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 100.0);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("altaFermentador lanza RecursoDuplicadoException y no persiste cuando el identificador interno ya existe")
    void altaFermentador_debeRechazarIdentificadorDuplicado() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FERM-01")).thenReturn(true);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'FERM-01'");

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("FERM-01");
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaFermentador lanza RecursoDuplicadoException cuando el identificador ya existe con distinto case (case-insensitive)")
    void altaFermentador_debeRechazarIdentificadorDuplicadoCaseInsensitive() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("ferm-01", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FERM-01")).thenReturn(true);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("FERM-01");
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaFermentador persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaFermentador_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-02", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FERM-02")).thenReturn(false);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> {
            FermentadorEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        FermentadorResponseDTO resultado = fermentadorServicio.altaFermentador(fermentadorFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<FermentadorEntity> captor = ArgumentCaptor.forClass(FermentadorEntity.class);
        verify(fermentadorRepository).save(captor.capture());
        FermentadorEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("FERM-02");
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(entidadCapturada.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(entidadCapturada.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(entidadCapturada.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        // El alta siempre debe registrar al fermentador como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("FERM-02");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(resultado.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("FERM-02");
    }

    @Test
    @DisplayName("CP-AF-06: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son nulos")
    void altaFermentador_debeRechazarUsosMaximosAntesMantenimientoNulo() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 80.0, null);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento son obligatorios");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-07: altaFermentador lanza ReglaNegocioException cuando los usos máximos antes de mantenimiento son iguales a cero (valor límite)")
    void altaFermentador_debeRechazarUsosMaximosAntesMantenimientoIgualACero() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 80.0, 0);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    // ==================== modificarFermentador ====================

    @Test
    @DisplayName("modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad útil es inválida")
    void modificarFermentador_debeRechazarCapacidadUtilInvalida() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 90.0, 70.0);
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 110.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        // La existencia del fermentador se valida antes que las reglas de negocio sobre sus datos
        verify(fermentadorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarFermentador lanza RecursoDuplicadoException y no persiste cuando el identificador está en uso por otro equipamiento")
    void modificarFermentador_debeRechazarIdentificadorEnUsoPorOtroEquipamiento() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 90.0, 70.0);
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-EXISTENTE", 100.0, 80.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FERM-EXISTENTE", 1L)).thenReturn(true);

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El identificador interno \'FERM-EXISTENTE\' ya está en uso por otro equipamiento");

        verify(fermentadorRepository).findById(1L);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("FERM-EXISTENTE", 1L);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarFermentador lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarFermentador_debeLanzarExcepcionSiNoExiste() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 80.0);
        when(fermentadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(99L, fermentadorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fermentador con ID: 99");

        verify(fermentadorRepository).findById(99L);
        verify(fermentadorRepository, never()).save(any());
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("modificarFermentador actualiza los datos y persiste cuando el ID existe y el identificador está libre (camino feliz)")
    void modificarFermentador_debeActualizarFermentadorExistente() {
        // === PREPARACION DE DATOS ===
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-VIEJO", 90.0, 70.0);
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-NUEVO", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FERM-NUEVO", 1L)).thenReturn(false);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(fermentadorRepository.save(fermentadorEntity)).thenReturn(fermentadorEntity);

        // === EJECUCION ===
        FermentadorResponseDTO resultado = fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<FermentadorEntity> captor = ArgumentCaptor.forClass(FermentadorEntity.class);
        verify(fermentadorRepository).save(captor.capture());
        FermentadorEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("FERM-NUEVO");
        assertThat(entidadCapturada.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(entidadCapturada.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(entidadCapturada.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);

        assertThat(resultado.getIdentificadorInterno()).isEqualTo("FERM-NUEVO");
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        verify(fermentadorRepository).findById(1L);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("FERM-NUEVO", 1L);
    }

    @Test
    @DisplayName("modificarFermentador permite conservar el propio identificador actual al actualizar otros campos")
    void modificarFermentador_debePermitirConservarIdentificadorPropio() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 90.0, 70.0);
        // Mismo identificador (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("ferm-01", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FERM-01", 1L)).thenReturn(false);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(fermentadorRepository.save(fermentadorEntity)).thenReturn(fermentadorEntity);

        FermentadorResponseDTO resultado = fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO);

        assertThat(resultado.getIdentificadorInterno()).isEqualTo("FERM-01");
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        verify(fermentadorRepository).save(fermentadorEntity);
    }

    @Test
    @DisplayName("CP-MF-06: modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento son inválidos")
    void modificarFermentador_debeRechazarUsosMaximosAntesMantenimientoInvalido() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 90.0, 70.0);
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 80.0, 0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verify(fermentadorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-07: modificarFermentador permite modificar fermentador aunque tenga comprometida una etapa en estado Pendiente de un lote (su escalado queda guardado en la instantánea del lote)")
    void modificarFermentador_debePermitirModificarConLotePendiente() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 90.0, 70.0);
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FERM-01", 1L)).thenReturn(false);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(fermentadorRepository.save(fermentadorEntity)).thenReturn(fermentadorEntity);

        org.assertj.core.api.Assertions.assertThatCode(() -> fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO)).doesNotThrowAnyException();

        verify(fermentadorRepository).save(fermentadorEntity);
        verify(etapaLoteRepository, never()).existsLotePendienteAsociado(any());
    }

    @Test
    @DisplayName("CP-MF-08: modificarFermentador permite modificar fermentador aunque se encuentre en uso (su escalado queda guardado en la instantánea de cada lote)")
    void modificarFermentador_debePermitirModificarEnUso() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 90.0, 70.0, EstadoOperativo.EN_USO);
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FERM-01", 1L)).thenReturn(false);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(fermentadorRepository.save(fermentadorEntity)).thenReturn(fermentadorEntity);

        org.assertj.core.api.Assertions.assertThatCode(() -> fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO)).doesNotThrowAnyException();

        verify(fermentadorRepository).save(fermentadorEntity);
        verify(etapaLoteRepository, never()).existsLotePendienteAsociado(any());
    }

    // ==================== bajaFermentador ====================

    @Test
    @DisplayName("bajaFermentador marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe")
    void bajaFermentador_debeMarcarBajaYRetornarFermentadorExistente() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 100.0, 80.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);
        when(fermentadorRepository.save(fermentadorEntity)).thenReturn(fermentadorEntity);

        FermentadorResponseDTO resultado = fermentadorServicio.bajaFermentador(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(fermentadorEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertFermentadorDTO(fermentadorEntity, resultado);
        verify(fermentadorRepository).findById(1L);
        verify(etapaLoteRepository).existsLotePendienteAsociado(1L);
        verify(fermentadorRepository).save(fermentadorEntity);
        verify(fermentadorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("bajaFermentador lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaFermentador_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(fermentadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fermentadorServicio.bajaFermentador(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fermentador con ID: 99");

        verify(fermentadorRepository).findById(99L);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BF-03: bajaFermentador lanza ReglaNegocioException y no persiste cuando el fermentador tiene comprometida una etapa en estado Pendiente de un lote")
    void bajaFermentador_debeRechazarAsociacionALotePendiente() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 100.0, 80.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(true);

        assertThatThrownBy(() -> fermentadorServicio.bajaFermentador(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el fermentador porque tiene comprometida una etapa en estado Pendiente de un lote");

        verify(etapaLoteRepository).existsLotePendienteAsociado(1L);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BF-04: bajaFermentador lanza ReglaNegocioException y no persiste cuando el fermentador se encuentra en uso")
    void bajaFermentador_debeRechazarSiEstaEnUso() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 100.0, 80.0, EstadoOperativo.EN_USO);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> fermentadorServicio.bajaFermentador(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el fermentador porque no se encuentra disponible");

        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BF-05: bajaFermentador lanza ReglaNegocioException y no persiste cuando el fermentador se encuentra en limpieza")
    void bajaFermentador_debeRechazarSiEstaEnLimpieza() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 100.0, 80.0, EstadoOperativo.EN_LIMPIEZA);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> fermentadorServicio.bajaFermentador(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el fermentador porque no se encuentra disponible");

        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BF-06: bajaFermentador lanza ReglaNegocioException y no persiste cuando el fermentador se encuentra en mantenimiento")
    void bajaFermentador_debeRechazarSiEstaEnMantenimiento() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 100.0, 80.0, EstadoOperativo.EN_MANTENIMIENTO);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> fermentadorServicio.bajaFermentador(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el fermentador porque no se encuentra disponible");

        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BF-07: bajaFermentador informa primero la etapa en estado Pendiente de un lote cuando el fermentador además no está disponible")
    void bajaFermentador_debePriorizarEtapaPendienteSobreNoDisponible() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 100.0, 80.0, EstadoOperativo.EN_USO);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(true);

        assertThatThrownBy(() -> fermentadorServicio.bajaFermentador(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el fermentador porque tiene comprometida una etapa en estado Pendiente de un lote");

        verify(fermentadorRepository, never()).save(any());
    }

    // ==================== validaciones técnicas de Fermentador (números finitos, techos y longitudes) ====================

    @Test
    @DisplayName("CP-AF-08: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es NaN")
    void altaFermentador_debeRechazar_capacidadTotalNaN() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", Double.NaN, 80.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser un número mayor a 0");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-09: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es infinita positiva")
    void altaFermentador_debeRechazar_capacidadTotalInfinita() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", Double.POSITIVE_INFINITY, 80.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser un número mayor a 0");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-10: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es igual a cero (valor límite)")
    void altaFermentador_debeRechazar_capacidadTotalCero() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 0.0, -1.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser un número mayor a 0");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-11: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es negativa")
    void altaFermentador_debeRechazar_capacidadTotalNegativa() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", -1.0, -2.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser un número mayor a 0");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-12: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total supera 100.000 litros")
    void altaFermentador_debeRechazar_capacidadTotalSuperaElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100000.01, 80.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-13: altaFermentador persiste con éxito cuando la capacidad total es exactamente 100.000 litros (valor límite válido)")
    void altaFermentador_debeAceptar_capacidadTotalEnElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100000.0, 99999.0, 500);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER-01")).thenReturn(false);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> fermentadorServicio.altaFermentador(dto)).doesNotThrowAnyException();

        verify(fermentadorRepository).save(any(FermentadorEntity.class));
    }

    @Test
    @DisplayName("CP-AF-23: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es nula")
    void altaFermentador_debeRechazar_capacidadUtilNula() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100.0, null, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil es obligatoria");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-14: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es NaN")
    void altaFermentador_debeRechazar_capacidadUtilNaN() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100.0, Double.NaN, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser un número mayor a 0");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-15: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es igual a cero (valor límite)")
    void altaFermentador_debeRechazar_capacidadUtilCero() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100.0, 0.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser un número mayor a 0");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-16: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es negativa")
    void altaFermentador_debeRechazar_capacidadUtilNegativa() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100.0, -1.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser un número mayor a 0");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-18: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son negativos")
    void altaFermentador_debeRechazar_usosNegativos() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100.0, 80.0, -1);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-19: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento superan 100.000")
    void altaFermentador_debeRechazar_usosSuperanElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100.0, 80.0, 100001);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-20: altaFermentador persiste con éxito cuando los usos máximos antes de mantenimiento son exactamente 100.000 (valor límite válido)")
    void altaFermentador_debeAceptar_usosEnElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100.0, 80.0, 100000);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER-01")).thenReturn(false);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> fermentadorServicio.altaFermentador(dto)).doesNotThrowAnyException();

        verify(fermentadorRepository).save(any(FermentadorEntity.class));
    }

    @Test
    @DisplayName("CP-AF-21: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando el identificador interno tiene 101 caracteres")
    void altaFermentador_debeRechazar_identificadorDe101Caracteres() {
        FermentadorFormDTO dto = fermentadorFormDTO("A".repeat(101), 100.0, 80.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El identificador interno no puede superar 100 caracteres");

        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AF-22: altaFermentador persiste con éxito cuando el identificador interno tiene exactamente 100 caracteres (valor límite válido)")
    void altaFermentador_debeAceptar_identificadorDe100Caracteres() {
        FermentadorFormDTO dto = fermentadorFormDTO("A".repeat(100), 100.0, 80.0, 500);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("A".repeat(100))).thenReturn(false);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> fermentadorServicio.altaFermentador(dto)).doesNotThrowAnyException();

        verify(fermentadorRepository).save(any(FermentadorEntity.class));
    }

    @Test
    @DisplayName("CP-MF-09: modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad total es NaN")
    void modificarFermentador_debeRechazar_capacidadTotalNaN() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", Double.NaN, 80.0, 500);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(crearFermentadorEntity(1L, "FER-01", 100.0, 80.0)));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser un número mayor a 0");

        verify(fermentadorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-10: modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad total supera 100.000 litros")
    void modificarFermentador_debeRechazar_capacidadTotalSuperaElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100000.01, 80.0, 500);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(crearFermentadorEntity(1L, "FER-01", 100.0, 80.0)));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");

        verify(fermentadorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-11: modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad útil es NaN")
    void modificarFermentador_debeRechazar_capacidadUtilNaN() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100.0, Double.NaN, 500);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(crearFermentadorEntity(1L, "FER-01", 100.0, 80.0)));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser un número mayor a 0");

        verify(fermentadorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-12: modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento superan 100.000")
    void modificarFermentador_debeRechazar_usosSuperanElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO("FER-01", 100.0, 80.0, 100001);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(crearFermentadorEntity(1L, "FER-01", 100.0, 80.0)));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verify(fermentadorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-13: modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando el identificador interno tiene 101 caracteres")
    void modificarFermentador_debeRechazar_identificadorDe101Caracteres() {
        FermentadorFormDTO dto = fermentadorFormDTO("A".repeat(101), 100.0, 80.0, 500);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(crearFermentadorEntity(1L, "FER-01", 100.0, 80.0)));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El identificador interno no puede superar 100 caracteres");

        verify(fermentadorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-14: modificarFermentador persiste con éxito cuando el identificador interno tiene exactamente 100 caracteres (valor límite válido)")
    void modificarFermentador_debeAceptar_identificadorDe100Caracteres() {
        FermentadorFormDTO dto = fermentadorFormDTO("A".repeat(100), 100.0, 80.0, 500);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(crearFermentadorEntity(1L, "FER-01", 100.0, 80.0)));
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("A".repeat(100), 1L)).thenReturn(false);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> fermentadorServicio.modificarFermentador(1L, dto)).doesNotThrowAnyException();

        verify(fermentadorRepository).save(any(FermentadorEntity.class));
    }

    // ==================== helpers ====================

    private static FermentadorEntity crearFermentadorEntity(Long id, String identificadorInterno, Double capacidadTotal,
                                                              Double capacidadUtil) {
        return crearFermentadorEntity(id, identificadorInterno, capacidadTotal, capacidadUtil, EstadoOperativo.DISPONIBLE);
    }

    private static FermentadorEntity crearFermentadorEntity(Long id, String identificadorInterno, Double capacidadTotal,
                                                              Double capacidadUtil, EstadoOperativo estadoOperativo) {
        return FermentadorEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .estadoOperativo(estadoOperativo)
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .usosMaximosAntesMantenimiento(USOS_MAXIMOS_ANTES_MANTENIMIENTO)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static FermentadorFormDTO fermentadorFormDTO(String identificadorInterno, Double capacidadTotal,
                                                           Double capacidadUtil) {
        return fermentadorFormDTO(identificadorInterno, capacidadTotal, capacidadUtil, USOS_MAXIMOS_ANTES_MANTENIMIENTO);
    }

    private static FermentadorFormDTO fermentadorFormDTO(String identificadorInterno, Double capacidadTotal,
                                                           Double capacidadUtil, Integer usosMaximosAntesMantenimiento) {
        return FermentadorFormDTO.builder()
                .identificadorInterno(identificadorInterno)
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .usosMaximosAntesMantenimiento(usosMaximosAntesMantenimiento)
                .build();
    }

    private static void assertFermentadorDTO(FermentadorEntity entidad, FermentadorResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificadorInterno()).isEqualTo(entidad.getIdentificadorInterno());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getCapacidadTotal()).isEqualTo(entidad.getCapacidadTotal());
        assertThat(dto.getCapacidadUtil()).isEqualTo(entidad.getCapacidadUtil());
        assertThat(dto.getUsosMaximosAntesMantenimiento()).isEqualTo(entidad.getUsosMaximosAntesMantenimiento());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }

    // ==================== normalización y unicidad ====================

    @Test
    @DisplayName("CP-AF-24: altaFermentador guarda el identificador interno normalizado y consulta la unicidad con ese valor")
    void altaFermentador_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        FermentadorFormDTO formDTO = fermentadorFormDTO("  fer   01 ", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER 01")).thenReturn(false);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        fermentadorServicio.altaFermentador(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<FermentadorEntity> captor = ArgumentCaptor.forClass(FermentadorEntity.class);
        verify(fermentadorRepository).save(captor.capture());
        assertThat(captor.getValue().getIdentificadorInterno()).isEqualTo("FER 01");
        assertThat(formDTO.getIdentificadorInterno()).isEqualTo("  fer   01 ");
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("FER 01");
        verify(fermentadorRepository).flush();
    }

    @Test
    @DisplayName("CP-AF-25: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando el identificador interno está vacío")
    void altaFermentador_debeRechazarValorVacio() {
        FermentadorFormDTO formDTO = fermentadorFormDTO("   ", 100.0, 80.0);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El identificador interno es obligatorio");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-26: altaFermentador lanza RecursoDuplicadoException cuando el identificador interno normalizado ya existe")
    void altaFermentador_debeDetectarDuplicadoConElValorNormalizado() {
        FermentadorFormDTO formDTO = fermentadorFormDTO("  fer-01 ", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER-01")).thenReturn(true);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'FER-01'");

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("FER-01");
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AF-27: altaFermentador traduce la violación del índice único udx_equipamiento_identificador_interno al mensaje de duplicado")
    void altaFermentador_debeTraducirLaViolacionDelIndiceUnico() {
        FermentadorFormDTO formDTO = fermentadorFormDTO("FER 01", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER 01")).thenReturn(false);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_equipamiento_identificador_interno"));

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'FER 01'");
    }

    @Test
    @DisplayName("CP-AF-28: altaFermentador permite reutilizar el identificador interno de un registro dado de baja")
    void altaFermentador_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        FermentadorFormDTO formDTO = fermentadorFormDTO("FER 01", 100.0, 80.0);
        // El único registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER 01")).thenReturn(false);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> fermentadorServicio.altaFermentador(formDTO)).doesNotThrowAnyException();

        verify(fermentadorRepository).save(any(FermentadorEntity.class));
    }

    @Test
    @DisplayName("CP-AF-29: altaFermentador permite un identificador que difiere de otro solo en la puntuación")
    void altaFermentador_debeTratarComoDistintosLosIdentificadoresConDistintaPuntuacion() {
        FermentadorFormDTO formDTO = fermentadorFormDTO("fer 01", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER 01")).thenReturn(false);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> fermentadorServicio.altaFermentador(formDTO)).doesNotThrowAnyException();

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("FER 01");
        verify(fermentadorRepository).save(any(FermentadorEntity.class));
    }

    @Test
    @DisplayName("CP-MF-15: modificarFermentador guarda el identificador interno normalizado y consulta la unicidad con ese valor")
    void modificarFermentador_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        FermentadorFormDTO formDTO = fermentadorFormDTO("  fer   01 ", 100.0, 80.0);
        FermentadorEntity existente = crearFermentadorEntity(1L, "Original", 100.0, 80.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FER 01", 1L)).thenReturn(false);
        when(fermentadorRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        fermentadorServicio.modificarFermentador(1L, formDTO);

        // === ASSERTS ===
        assertThat(existente.getIdentificadorInterno()).isEqualTo("FER 01");
        assertThat(formDTO.getIdentificadorInterno()).isEqualTo("  fer   01 ");
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("FER 01", 1L);
        verify(fermentadorRepository).save(existente);
        verify(fermentadorRepository).flush();
    }

    @Test
    @DisplayName("CP-MF-16: modificarFermentador lanza ReglaNegocioException cuando el identificador interno está vacío")
    void modificarFermentador_debeRechazarValorVacio() {
        FermentadorFormDTO formDTO = fermentadorFormDTO("   ", 100.0, 80.0);
        FermentadorEntity existente = crearFermentadorEntity(1L, "Original", 100.0, 80.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El identificador interno es obligatorio");

        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-17: modificarFermentador lanza RecursoDuplicadoException cuando el identificador interno normalizado está en uso por otro registro")
    void modificarFermentador_debeDetectarDuplicadoConElValorNormalizado() {
        FermentadorFormDTO formDTO = fermentadorFormDTO("  fer-01 ", 100.0, 80.0);
        FermentadorEntity existente = crearFermentadorEntity(1L, "Original", 100.0, 80.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FER-01", 1L)).thenReturn(true);

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El identificador interno 'FER-01' ya está en uso por otro equipamiento");

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("FER-01", 1L);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-18: modificarFermentador traduce la violación del índice único udx_equipamiento_identificador_interno al mensaje de duplicado")
    void modificarFermentador_debeTraducirLaViolacionDelIndiceUnico() {
        FermentadorFormDTO formDTO = fermentadorFormDTO("FER 01", 100.0, 80.0);
        FermentadorEntity existente = crearFermentadorEntity(1L, "Original", 100.0, 80.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FER 01", 1L)).thenReturn(false);
        when(fermentadorRepository.save(existente)).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_equipamiento_identificador_interno"));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El identificador interno 'FER 01' ya está en uso por otro equipamiento");
    }

    @Test
    @DisplayName("CP-MF-19: modificarFermentador permite reutilizar el identificador interno de un registro dado de baja")
    void modificarFermentador_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        FermentadorFormDTO formDTO = fermentadorFormDTO("FER 01", 100.0, 80.0);
        FermentadorEntity existente = crearFermentadorEntity(1L, "Original", 100.0, 80.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(existente));
        // El único otro registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FER 01", 1L)).thenReturn(false);
        when(fermentadorRepository.save(existente)).thenReturn(existente);

        assertThatCode(() -> fermentadorServicio.modificarFermentador(1L, formDTO)).doesNotThrowAnyException();

        assertThat(existente.getIdentificadorInterno()).isEqualTo("FER 01");
        verify(fermentadorRepository).save(existente);
    }
}
