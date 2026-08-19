package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MolinoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MolinoResponseDTO;
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
class MolinoServicioImplTest {

    @Mock
    private IMolinoRepository molinoRepository;

    @Mock
    private IEquipamientoRepository equipamientoRepository;

    @InjectMocks
    private MolinoServicioImpl molinoServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de molinos correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0);
        MolinoEntity otroMolinoEntity = crearMolinoEntity(2L, "MOL-02", 120.0);

        // Cuando molinoRepository.findAll(pageable) sea llamado, retorna una página con los molinos activos
        // (el filtrado de soft-deleted es automático por @SoftDelete de Hibernate sobre la entidad)
        when(molinoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(molinoEntity, otroMolinoEntity), pageable, 2));

        // === EJECUCION ===
        Page<MolinoResponseDTO> resultado = molinoServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertMolinoDTO(molinoEntity, resultado.getContent().get(0));
        assertMolinoDTO(otroMolinoEntity, resultado.getContent().get(1));
        verify(molinoRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay molinos registrados")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(molinoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<MolinoResponseDTO> resultado = molinoServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(molinoRepository).findAll(pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del molino cuando el ID existe")
    void buscarPorId_debeRetornarMolinoExistente() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));

        // === EJECUCION ===
        MolinoResponseDTO resultado = molinoServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertMolinoDTO(molinoEntity, resultado);
        verify(molinoRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está soft-deleted")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // @SoftDelete hace que findById devuelva Optional.empty() también para registros eliminados lógicamente
        when(molinoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> molinoServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el Molino con ID:99");
        verify(molinoRepository).findById(99L);
    }

    // ==================== altaMolino ====================

    @Test
    @DisplayName("CP-AM-01: altaMolino lanza ReglaNegocioException y no consulta el repositorio cuando el rendimiento de molienda es negativo")
    void altaMolino_debeRechazarRendimientoNegativo() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO("MOL-01", -1.0);

        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda debe ser mayor a 0.");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(molinoRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AM-02: altaMolino lanza ReglaNegocioException cuando el rendimiento de molienda es igual a cero (valor límite)")
    void altaMolino_debeRechazarRendimientoIgualACero() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO("MOL-01", 0.0);

        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda debe ser mayor a 0.");

        verifyNoInteractions(molinoRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-AM-03: altaMolino lanza RecursoDuplicadoException y no persiste cuando el identificador interno ya existe")
    void altaMolino_debeRechazarIdentificadorDuplicado() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO("MOL-01", 50.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MOL-01")).thenReturn(true);

        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'MOL-01'");

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("MOL-01");
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AM-04: altaMolino lanza RecursoDuplicadoException cuando el identificador ya existe con distinto case (case-insensitive)")
    void altaMolino_debeRechazarIdentificadorDuplicadoCaseInsensitive() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO("mol-01", 50.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("mol-01")).thenReturn(true);

        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("mol-01");
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AM-05: altaMolino persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaMolino_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        MolinoFormDTO molinoFormDTO = molinoFormDTO("MOL-02", 50.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MOL-02")).thenReturn(false);
        when(molinoRepository.save(any(MolinoEntity.class))).thenAnswer(invocation -> {
            MolinoEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        MolinoResponseDTO resultado = molinoServicio.altaMolino(molinoFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<MolinoEntity> captor = ArgumentCaptor.forClass(MolinoEntity.class);
        verify(molinoRepository).save(captor.capture());
        MolinoEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("MOL-02");
        assertThat(entidadCapturada.getDescripcion()).isEqualTo("Molino de prueba");
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(entidadCapturada.getRendimientoMolienda()).isEqualTo(50.0);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MOL-02");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(resultado.getRendimientoMolienda()).isEqualTo(50.0);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("MOL-02");
    }

    @Test
    @DisplayName("CP-AM-06: altaMolino persiste con éxito cuando el rendimiento de molienda está en el límite inferior válido")
    void altaMolino_debePersistirConRendimientoEnLimiteInferiorValido() {
        // === PREPARACION DE DATOS ===
        MolinoFormDTO molinoFormDTO = molinoFormDTO("MOL-03", 0.1);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MOL-03")).thenReturn(false);
        when(molinoRepository.save(any(MolinoEntity.class))).thenAnswer(invocation -> {
            MolinoEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(3L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        MolinoResponseDTO resultado = molinoServicio.altaMolino(molinoFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<MolinoEntity> captor = ArgumentCaptor.forClass(MolinoEntity.class);
        verify(molinoRepository).save(captor.capture());
        assertThat(captor.getValue().getRendimientoMolienda()).isEqualTo(0.1);
        assertThat(resultado.getRendimientoMolienda()).isEqualTo(0.1);
    }

    // ==================== modificarMolino ====================

    @Test
    @DisplayName("CP-MM-01: modificarMolino lanza ReglaNegocioException y no consulta el repositorio cuando el rendimiento de molienda es inválido")
    void modificarMolino_debeRechazarRendimientoInvalido() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO("MOL-01", 0.0);

        assertThatThrownBy(() -> molinoServicio.modificarMolino(1L, molinoFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda debe ser mayor a 0.");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(molinoRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-MM-02: modificarMolino lanza RecursoDuplicadoException y no busca ni persiste cuando el identificador está en uso por otro equipamiento")
    void modificarMolino_debeRechazarIdentificadorEnUsoPorOtroEquipamiento() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO("MOL-EXISTENTE", 50.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("MOL-EXISTENTE", 1L)).thenReturn(true);

        assertThatThrownBy(() -> molinoServicio.modificarMolino(1L, molinoFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'MOL-EXISTENTE'");

        // La verificación de duplicados se ejecuta antes de localizar la entidad por ID
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("MOL-EXISTENTE", 1L);
        verify(molinoRepository, never()).findById(any());
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-03: modificarMolino lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarMolino_debeLanzarExcepcionSiNoExiste() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO("MOL-01", 50.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("MOL-01", 99L)).thenReturn(false);
        when(molinoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> molinoServicio.modificarMolino(99L, molinoFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el Molino con ID:99");

        verify(molinoRepository).findById(99L);
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-04: modificarMolino actualiza los datos y persiste cuando el ID existe y el identificador está libre (camino feliz)")
    void modificarMolino_debeActualizarMolinoExistente() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-VIEJO", 90.0);
        MolinoFormDTO molinoFormDTO = molinoFormDTO("MOL-NUEVO", 100.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("MOL-NUEVO", 1L)).thenReturn(false);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(molinoRepository.save(molinoEntity)).thenReturn(molinoEntity);

        // === EJECUCION ===
        MolinoResponseDTO resultado = molinoServicio.modificarMolino(1L, molinoFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<MolinoEntity> captor = ArgumentCaptor.forClass(MolinoEntity.class);
        verify(molinoRepository).save(captor.capture());
        MolinoEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("MOL-NUEVO");
        assertThat(entidadCapturada.getDescripcion()).isEqualTo("Molino de prueba");
        assertThat(entidadCapturada.getRendimientoMolienda()).isEqualTo(100.0);

        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MOL-NUEVO");
        assertThat(resultado.getRendimientoMolienda()).isEqualTo(100.0);
        verify(molinoRepository).findById(1L);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("MOL-NUEVO", 1L);
    }

    @Test
    @DisplayName("CP-MM-05: modificarMolino permite conservar el propio identificador actual al actualizar otros campos")
    void modificarMolino_debePermitirConservarIdentificadorPropio() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 90.0);
        // Mismo identificador (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        MolinoFormDTO molinoFormDTO = molinoFormDTO("mol-01", 100.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("mol-01", 1L)).thenReturn(false);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(molinoRepository.save(molinoEntity)).thenReturn(molinoEntity);

        MolinoResponseDTO resultado = molinoServicio.modificarMolino(1L, molinoFormDTO);

        assertThat(resultado.getIdentificadorInterno()).isEqualTo("mol-01");
        assertThat(resultado.getRendimientoMolienda()).isEqualTo(100.0);
        verify(molinoRepository).save(molinoEntity);
    }

    // ==================== bajaMolino ====================

    @Test
    @DisplayName("CP-BM-02: bajaMolino elimina lógicamente (soft-delete) y retorna el DTO cuando el ID existe")
    void bajaMolino_debeEliminarYRetornarMolinoExistente() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));

        MolinoResponseDTO resultado = molinoServicio.bajaMolino(1L);

        // El borrado físico a nivel repositorio es convertido a UPDATE por el @SoftDelete de Hibernate
        assertMolinoDTO(molinoEntity, resultado);
        verify(molinoRepository).findById(1L);
        verify(molinoRepository).delete(molinoEntity);
    }

    @Test
    @DisplayName("CP-BM-01: bajaMolino lanza RecursoNoEncontradoException y no elimina cuando el ID no existe")
    void bajaMolino_debeLanzarExcepcionYNoEliminarSiNoExiste() {
        when(molinoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> molinoServicio.bajaMolino(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el Molino con ID:99");

        verify(molinoRepository).findById(99L);
        verify(molinoRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static MolinoEntity crearMolinoEntity(Long id, String identificadorInterno, Double rendimientoMolienda) {
        return MolinoEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .descripcion("Molino de prueba")
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .rendimientoMolienda(rendimientoMolienda)
                .build();
    }

    private static MolinoFormDTO molinoFormDTO(String identificadorInterno, Double rendimientoMolienda) {
        return MolinoFormDTO.builder()
                .identificadorInterno(identificadorInterno)
                .descripcion("Molino de prueba")
                .rendimientoMolienda(rendimientoMolienda)
                .build();
    }

    private static void assertMolinoDTO(MolinoEntity entidad, MolinoResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificadorInterno()).isEqualTo(entidad.getIdentificadorInterno());
        assertThat(dto.getDescripcion()).isEqualTo(entidad.getDescripcion());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getRendimientoMolienda()).isEqualTo(entidad.getRendimientoMolienda());
    }
}
