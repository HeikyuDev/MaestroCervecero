package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ISecuenciaIdentificadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FermentadorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnicoTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
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
    private ISecuenciaIdentificadorRepository secuenciaIdentificadorRepository;

    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

    @InjectMocks
    private FermentadorServicioImpl fermentadorServicio;

    @BeforeEach
    void secuenciaDelIdentificador() {
        // Valor por defecto de la secuencia para los casos de alta que no verifican el número generado
        lenient().when(secuenciaIdentificadorRepository.siguienteNumeroFermentador()).thenReturn(1L);
    }

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
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO(100.0, 120.0);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("altaFermentador lanza ReglaNegocioException cuando la capacidad útil es igual a la total (valor límite)")
    void altaFermentador_debeRechazarCapacidadUtilIgualATotal() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO(100.0, 100.0);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("altaFermentador persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaFermentador_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroFermentador()).thenReturn(5L);
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO(100.0, 80.0);
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
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("FER-05");
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(entidadCapturada.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(entidadCapturada.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(entidadCapturada.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        // El alta siempre debe registrar al fermentador como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("FER-05");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(resultado.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
    }

    @Test
    @DisplayName("CP-AF-06: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son nulos")
    void altaFermentador_debeRechazarUsosMaximosAntesMantenimientoNulo() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO(100.0, 80.0, null);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento son obligatorios");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-07: altaFermentador lanza ReglaNegocioException cuando los usos máximos antes de mantenimiento son iguales a cero (valor límite)")
    void altaFermentador_debeRechazarUsosMaximosAntesMantenimientoIgualACero() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO(100.0, 80.0, 0);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(fermentadorRepository);
    }

    // ==================== modificarFermentador ====================

    @Test
    @DisplayName("modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad útil es inválida")
    void modificarFermentador_debeRechazarCapacidadUtilInvalida() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 90.0, 70.0);
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO(100.0, 110.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        // La existencia del fermentador se valida antes que las reglas de negocio sobre sus datos
        verify(fermentadorRepository).findById(1L);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarFermentador lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarFermentador_debeLanzarExcepcionSiNoExiste() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO(100.0, 80.0);
        when(fermentadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(99L, fermentadorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fermentador con ID: 99");

        verify(fermentadorRepository).findById(99L);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarFermentador actualiza los datos y persiste cuando el ID existe, sin cambiar el identificador interno (camino feliz)")
    void modificarFermentador_debeActualizarFermentadorExistente() {
        // === PREPARACION DE DATOS ===
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-VIEJO", 90.0, 70.0);
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO(100.0, 80.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(fermentadorRepository.save(fermentadorEntity)).thenReturn(fermentadorEntity);

        // === EJECUCION ===
        FermentadorResponseDTO resultado = fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<FermentadorEntity> captor = ArgumentCaptor.forClass(FermentadorEntity.class);
        verify(fermentadorRepository).save(captor.capture());
        FermentadorEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("FERM-VIEJO");
        assertThat(entidadCapturada.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(entidadCapturada.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(entidadCapturada.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);

        assertThat(resultado.getIdentificadorInterno()).isEqualTo("FERM-VIEJO");
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        verify(fermentadorRepository).findById(1L);
        // El identificador interno lo asignó el sistema al dar de alta: modificar no lo cambia ni pide otro número
        verifyNoInteractions(secuenciaIdentificadorRepository);
    }

    @Test
    @DisplayName("CP-MF-06: modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento son inválidos")
    void modificarFermentador_debeRechazarUsosMaximosAntesMantenimientoInvalido() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 90.0, 70.0);
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO(100.0, 80.0, 0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verify(fermentadorRepository).findById(1L);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-07: modificarFermentador permite modificar fermentador aunque tenga comprometida una etapa en estado Pendiente de un lote (su escalado queda guardado en la instantánea del lote)")
    void modificarFermentador_debePermitirModificarConLotePendiente() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 90.0, 70.0);
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO(100.0, 80.0);
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
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO(100.0, 80.0);
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
        FermentadorFormDTO dto = fermentadorFormDTO(Double.NaN, 80.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-09: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es infinita positiva")
    void altaFermentador_debeRechazar_capacidadTotalInfinita() {
        FermentadorFormDTO dto = fermentadorFormDTO(Double.POSITIVE_INFINITY, 80.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-10: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es igual a cero (valor límite)")
    void altaFermentador_debeRechazar_capacidadTotalCero() {
        FermentadorFormDTO dto = fermentadorFormDTO(0.0, -1.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-11: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es negativa")
    void altaFermentador_debeRechazar_capacidadTotalNegativa() {
        FermentadorFormDTO dto = fermentadorFormDTO(-1.0, -2.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-12: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total supera 100.000 litros")
    void altaFermentador_debeRechazar_capacidadTotalSuperaElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO(100000.01, 80.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-13: altaFermentador persiste con éxito cuando la capacidad total es exactamente 100.000 litros (valor límite válido)")
    void altaFermentador_debeAceptar_capacidadTotalEnElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO(100000.0, 99999.0, 500);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> fermentadorServicio.altaFermentador(dto)).doesNotThrowAnyException();

        verify(fermentadorRepository).save(any(FermentadorEntity.class));
    }

    @Test
    @DisplayName("CP-AF-23: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es nula")
    void altaFermentador_debeRechazar_capacidadUtilNula() {
        FermentadorFormDTO dto = fermentadorFormDTO(100.0, null, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil es obligatoria");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-14: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es NaN")
    void altaFermentador_debeRechazar_capacidadUtilNaN() {
        FermentadorFormDTO dto = fermentadorFormDTO(100.0, Double.NaN, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser mayor a 0");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-15: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es igual a cero (valor límite)")
    void altaFermentador_debeRechazar_capacidadUtilCero() {
        FermentadorFormDTO dto = fermentadorFormDTO(100.0, 0.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser mayor a 0");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-16: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es negativa")
    void altaFermentador_debeRechazar_capacidadUtilNegativa() {
        FermentadorFormDTO dto = fermentadorFormDTO(100.0, -1.0, 500);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser mayor a 0");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-18: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son negativos")
    void altaFermentador_debeRechazar_usosNegativos() {
        FermentadorFormDTO dto = fermentadorFormDTO(100.0, 80.0, -1);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-19: altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento superan 100.000")
    void altaFermentador_debeRechazar_usosSuperanElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO(100.0, 80.0, 100001);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verifyNoInteractions(fermentadorRepository);
    }

    @Test
    @DisplayName("CP-AF-20: altaFermentador persiste con éxito cuando los usos máximos antes de mantenimiento son exactamente 100.000 (valor límite válido)")
    void altaFermentador_debeAceptar_usosEnElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO(100.0, 80.0, 100000);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> fermentadorServicio.altaFermentador(dto)).doesNotThrowAnyException();

        verify(fermentadorRepository).save(any(FermentadorEntity.class));
    }

    @Test
    @DisplayName("CP-MF-09: modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad total es NaN")
    void modificarFermentador_debeRechazar_capacidadTotalNaN() {
        FermentadorFormDTO dto = fermentadorFormDTO(Double.NaN, 80.0, 500);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(crearFermentadorEntity(1L, "FER-01", 100.0, 80.0)));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verify(fermentadorRepository).findById(1L);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-10: modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad total supera 100.000 litros")
    void modificarFermentador_debeRechazar_capacidadTotalSuperaElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO(100000.01, 80.0, 500);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(crearFermentadorEntity(1L, "FER-01", 100.0, 80.0)));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");

        verify(fermentadorRepository).findById(1L);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-11: modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad útil es NaN")
    void modificarFermentador_debeRechazar_capacidadUtilNaN() {
        FermentadorFormDTO dto = fermentadorFormDTO(100.0, Double.NaN, 500);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(crearFermentadorEntity(1L, "FER-01", 100.0, 80.0)));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser mayor a 0");

        verify(fermentadorRepository).findById(1L);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MF-12: modificarFermentador lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento superan 100.000")
    void modificarFermentador_debeRechazar_usosSuperanElMaximo() {
        FermentadorFormDTO dto = fermentadorFormDTO(100.0, 80.0, 100001);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(crearFermentadorEntity(1L, "FER-01", 100.0, 80.0)));

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verify(fermentadorRepository).findById(1L);
        verify(fermentadorRepository, never()).save(any());
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

    private static FermentadorFormDTO fermentadorFormDTO(Double capacidadTotal,
                                                           Double capacidadUtil) {
        return fermentadorFormDTO(capacidadTotal, capacidadUtil, USOS_MAXIMOS_ANTES_MANTENIMIENTO);
    }

    private static FermentadorFormDTO fermentadorFormDTO(Double capacidadTotal,
                                                           Double capacidadUtil, Integer usosMaximosAntesMantenimiento) {
        return FermentadorFormDTO.builder()
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

    // ==================== identificador interno generado ====================

    @Test
    @DisplayName("CP-AF-30: altaFermentador asigna el identificador interno generado a partir de la secuencia (FER-07)")
    void altaFermentador_debeAsignarElIdentificadorInternoGenerado() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroFermentador()).thenReturn(7L);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        var resultado = fermentadorServicio.altaFermentador(fermentadorFormDTO(100.0, 80.0));

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("FER-07");
    }

    @Test
    @DisplayName("CP-AF-31: altaFermentador no pide un número de la secuencia cuando falla cualquier validación")
    void altaFermentador_noDebePedirNumeroDeLaSecuenciaSiFallaUnaValidacion() {
        // === EJECUCION Y ASERTOS ===
        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO(null, 80.0))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO(100.0, 120.0))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO(100.0, 80.0, 0))).isInstanceOf(ReglaNegocioException.class);

        verifyNoInteractions(secuenciaIdentificadorRepository);
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AF-32: altaFermentador pide el número de la secuencia como último paso, justo antes de guardar")
    void altaFermentador_debePedirElNumeroDeLaSecuenciaAntesDeGuardar() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroFermentador()).thenReturn(1L);
        when(fermentadorRepository.save(any(FermentadorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        fermentadorServicio.altaFermentador(fermentadorFormDTO(100.0, 80.0));

        // === ASSERTS ===
        InOrder orden = inOrder(secuenciaIdentificadorRepository, fermentadorRepository);
        orden.verify(secuenciaIdentificadorRepository).siguienteNumeroFermentador();
        orden.verify(fermentadorRepository).save(any(FermentadorEntity.class));
    }

    @Test
    @DisplayName("CP-MF-20: modificarFermentador no cambia el identificador interno asignado por el sistema ni pide otro número")
    void modificarFermentador_noDebeCambiarElIdentificadorInterno() {
        // === PREPARACION DE DATOS ===
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FER-03", 90.0, 70.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(fermentadorRepository.save(fermentadorEntity)).thenReturn(fermentadorEntity);

        // === EJECUCION ===
        var resultado = fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO(100.0, 80.0));

        // === ASSERTS ===
        assertThat(fermentadorEntity.getIdentificadorInterno()).isEqualTo("FER-03");
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("FER-03");
        verifyNoInteractions(secuenciaIdentificadorRepository);
    }
}
