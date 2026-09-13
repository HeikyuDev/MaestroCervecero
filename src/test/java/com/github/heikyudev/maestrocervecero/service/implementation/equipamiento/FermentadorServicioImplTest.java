package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IFermentadorRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.FermentadorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.FermentadorResponseDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FermentadorServicioImplTest {

    @Mock
    private IFermentadorRepository fermentadorRepository;

    @Mock
    private IEquipamientoRepository equipamientoRepository;

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
        when(fermentadorRepository.filtrarFermentadores("Inexistente", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<FermentadorResponseDTO> resultado = fermentadorServicio.filtrarFermentadores("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(fermentadorRepository).filtrarFermentadores("Inexistente", null, pageable);
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

    // ==================== altaFermentador ====================

    @Test
    @DisplayName("altaFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es mayor a la total")
    void altaFermentador_debeRechazarCapacidadUtilMayorATotal() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 120.0);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad util no puede ser mayor a la capacidad total.");

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
                .hasMessage("La capacidad util no puede ser mayor a la capacidad total.");

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
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("ferm-01")).thenReturn(true);

        assertThatThrownBy(() -> fermentadorServicio.altaFermentador(fermentadorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("ferm-01");
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
        assertThat(entidadCapturada.getDescripcion()).isEqualTo("Fermentador de prueba");
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(entidadCapturada.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(entidadCapturada.getCapacidadUtil()).isEqualTo(80.0);
        // El alta siempre debe registrar al fermentador como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("FERM-02");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("FERM-02");
    }

    // ==================== modificarFermentador ====================

    @Test
    @DisplayName("modificarFermentador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es inválida")
    void modificarFermentador_debeRechazarCapacidadUtilInvalida() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 110.0);

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad util no puede ser mayor a la capacidad total.");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(fermentadorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("modificarFermentador lanza RecursoDuplicadoException y no busca ni persiste cuando el identificador está en uso por otro equipamiento")
    void modificarFermentador_debeRechazarIdentificadorEnUsoPorOtroEquipamiento() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-EXISTENTE", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FERM-EXISTENTE", 1L)).thenReturn(true);

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'FERM-EXISTENTE'");

        // La verificación de duplicados se ejecuta antes de localizar la entidad por ID
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("FERM-EXISTENTE", 1L);
        verify(fermentadorRepository, never()).findById(any());
        verify(fermentadorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarFermentador lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarFermentador_debeLanzarExcepcionSiNoExiste() {
        FermentadorFormDTO fermentadorFormDTO = fermentadorFormDTO("FERM-01", 100.0, 80.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FERM-01", 99L)).thenReturn(false);
        when(fermentadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fermentadorServicio.modificarFermentador(99L, fermentadorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fermentador con ID: 99");

        verify(fermentadorRepository).findById(99L);
        verify(fermentadorRepository, never()).save(any());
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
        assertThat(entidadCapturada.getDescripcion()).isEqualTo("Fermentador de prueba");
        assertThat(entidadCapturada.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(entidadCapturada.getCapacidadUtil()).isEqualTo(80.0);

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
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("ferm-01", 1L)).thenReturn(false);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(fermentadorRepository.save(fermentadorEntity)).thenReturn(fermentadorEntity);

        FermentadorResponseDTO resultado = fermentadorServicio.modificarFermentador(1L, fermentadorFormDTO);

        assertThat(resultado.getIdentificadorInterno()).isEqualTo("ferm-01");
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        verify(fermentadorRepository).save(fermentadorEntity);
    }

    // ==================== bajaFermentador ====================

    @Test
    @DisplayName("bajaFermentador marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe")
    void bajaFermentador_debeMarcarBajaYRetornarFermentadorExistente() {
        FermentadorEntity fermentadorEntity = crearFermentadorEntity(1L, "FERM-01", 100.0, 80.0);
        when(fermentadorRepository.findById(1L)).thenReturn(Optional.of(fermentadorEntity));
        when(fermentadorRepository.save(fermentadorEntity)).thenReturn(fermentadorEntity);

        FermentadorResponseDTO resultado = fermentadorServicio.bajaFermentador(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(fermentadorEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertFermentadorDTO(fermentadorEntity, resultado);
        verify(fermentadorRepository).findById(1L);
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

    // ==================== helpers ====================

    private static FermentadorEntity crearFermentadorEntity(Long id, String identificadorInterno, Double capacidadTotal,
                                                              Double capacidadUtil) {
        return FermentadorEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .descripcion("Fermentador de prueba")
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static FermentadorFormDTO fermentadorFormDTO(String identificadorInterno, Double capacidadTotal,
                                                           Double capacidadUtil) {
        return FermentadorFormDTO.builder()
                .identificadorInterno(identificadorInterno)
                .descripcion("Fermentador de prueba")
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .build();
    }

    private static void assertFermentadorDTO(FermentadorEntity entidad, FermentadorResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificadorInterno()).isEqualTo(entidad.getIdentificadorInterno());
        assertThat(dto.getDescripcion()).isEqualTo(entidad.getDescripcion());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getCapacidadTotal()).isEqualTo(entidad.getCapacidadTotal());
        assertThat(dto.getCapacidadUtil()).isEqualTo(entidad.getCapacidadUtil());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
