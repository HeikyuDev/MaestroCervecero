package com.github.heikyudev.maestrocervecero.service.implementation.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.PaisEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IPaisRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IProvinciaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.PaisFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.PaisResponseDTO;
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
class PaisServicioImplTest {

    @Mock
    private IPaisRepository paisRepository;

    @Mock
    private IProvinciaRepository provinciaRepository;

    @InjectMocks
    private PaisServicioImpl paisServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de países correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        PaisEntity otroPaisEntity = crearPaisEntity(2L, "Brasil");
        PaisEntity tercerPaisEntity = crearPaisEntity(3L, "Uruguay");

        // Cuando paisRepository.findAll(pageable) sea llamado, retorna una página con los países activos
        // (el filtrado de soft-deleted es automático por @SoftDelete de Hibernate sobre la entidad)
        when(paisRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(paisEntity, otroPaisEntity, tercerPaisEntity), pageable, 3));

        // === EJECUCION ===
        Page<PaisResponseDTO> resultado = paisServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertPaisDTO(paisEntity, resultado.getContent().get(0));
        assertPaisDTO(otroPaisEntity, resultado.getContent().get(1));
        assertPaisDTO(tercerPaisEntity, resultado.getContent().get(2));
        verify(paisRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay países registrados")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(paisRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<PaisResponseDTO> resultado = paisServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(paisRepository).findAll(pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del país cuando el ID existe")
    void buscarPorId_debeRetornarPaisExistente() {
        // === PREPARACION DE DATOS ===
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));

        // === EJECUCION ===
        PaisResponseDTO resultado = paisServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertPaisDTO(paisEntity, resultado);
        verify(paisRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está soft-deleted")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // @SoftDelete hace que findById devuelva Optional.empty() también para registros eliminados lógicamente
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paisServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El país no existe");
        verify(paisRepository).findById(99L);
    }

    // ==================== altaPais ====================

    @Test
    @DisplayName("CP-AP-01: altaPais lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe")
    void altaPais_debeRechazarNombreDuplicado() {
        PaisFormDTO paisFormDTO = paisFormDTO("Argentina");
        when(paisRepository.existsByNombreIgnoreCase("Argentina")).thenReturn(true);

        assertThatThrownBy(() -> paisServicio.altaPais(paisFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un país con el nombre 'Argentina'");

        verify(paisRepository).existsByNombreIgnoreCase("Argentina");
        verify(paisRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-02: altaPais lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case (case-insensitive)")
    void altaPais_debeRechazarNombreDuplicadoCaseInsensitive() {
        PaisFormDTO paisFormDTO = paisFormDTO("argentina");
        when(paisRepository.existsByNombreIgnoreCase("argentina")).thenReturn(true);

        assertThatThrownBy(() -> paisServicio.altaPais(paisFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un país con el nombre 'argentina'");

        verify(paisRepository).existsByNombreIgnoreCase("argentina");
        verify(paisRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-03: altaPais persiste y retorna el DTO cuando el nombre es único (camino feliz)")
    void altaPais_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        PaisFormDTO paisFormDTO = paisFormDTO("Uruguay");
        when(paisRepository.existsByNombreIgnoreCase("Uruguay")).thenReturn(false);
        when(paisRepository.save(any(PaisEntity.class))).thenAnswer(invocation -> {
            PaisEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        PaisResponseDTO resultado = paisServicio.altaPais(paisFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<PaisEntity> captor = ArgumentCaptor.forClass(PaisEntity.class);
        verify(paisRepository).save(captor.capture());
        PaisEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Uruguay");

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Uruguay");
        verify(paisRepository).existsByNombreIgnoreCase("Uruguay");
    }

    // ==================== modificarPais ====================

    @Test
    @DisplayName("CP-MP-01: modificarPais lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarPais_debeLanzarExcepcionSiNoExiste() {
        PaisFormDTO paisFormDTO = paisFormDTO("Argentina");
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paisServicio.modificarPais(99L, paisFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El país no existe");

        verify(paisRepository).findById(99L);
        // Al no existir la entidad, no debe llegarse a validar la duplicidad de nombre
        verify(paisRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(paisRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-02: modificarPais lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otro país")
    void modificarPais_debeRechazarNombreEnUsoPorOtroPais() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        PaisFormDTO paisFormDTO = paisFormDTO("Brasil");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(paisRepository.existsByNombreIgnoreCaseAndIdNot("Brasil", 1L)).thenReturn(true);

        assertThatThrownBy(() -> paisServicio.modificarPais(1L, paisFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Brasil' ya está en uso por otro país");

        verify(paisRepository).findById(1L);
        verify(paisRepository).existsByNombreIgnoreCaseAndIdNot("Brasil", 1L);
        verify(paisRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-03: modificarPais permite conservar el propio nombre actual al actualizar")
    void modificarPais_debePermitirConservarNombrePropio() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        PaisFormDTO paisFormDTO = paisFormDTO("argentina");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(paisRepository.existsByNombreIgnoreCaseAndIdNot("argentina", 1L)).thenReturn(false);
        when(paisRepository.save(paisEntity)).thenReturn(paisEntity);

        PaisResponseDTO resultado = paisServicio.modificarPais(1L, paisFormDTO);

        assertThat(resultado.getNombre()).isEqualTo("argentina");
        verify(paisRepository).save(paisEntity);
    }

    @Test
    @DisplayName("CP-MP-04: modificarPais actualiza los datos y persiste cuando el ID existe y el nombre está libre (camino feliz)")
    void modificarPais_debeActualizarPaisExistente() {
        // === PREPARACION DE DATOS ===
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        PaisFormDTO paisFormDTO = paisFormDTO("Argentina Actualizado");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(paisRepository.existsByNombreIgnoreCaseAndIdNot("Argentina Actualizado", 1L)).thenReturn(false);
        when(paisRepository.save(paisEntity)).thenReturn(paisEntity);

        // === EJECUCION ===
        PaisResponseDTO resultado = paisServicio.modificarPais(1L, paisFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<PaisEntity> captor = ArgumentCaptor.forClass(PaisEntity.class);
        verify(paisRepository).save(captor.capture());
        PaisEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Argentina Actualizado");

        assertThat(resultado.getNombre()).isEqualTo("Argentina Actualizado");
        verify(paisRepository).findById(1L);
        verify(paisRepository).existsByNombreIgnoreCaseAndIdNot("Argentina Actualizado", 1L);
    }

    // ==================== bajaPais ====================

    @Test
    @DisplayName("CP-BP-01: bajaPais lanza RecursoNoEncontradoException y no consulta provincias ni elimina cuando el ID no existe")
    void bajaPais_debeLanzarExcepcionYNoEliminarSiNoExiste() {
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paisServicio.bajaPais(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el país con ID: 99");

        verify(paisRepository).findById(99L);
        verifyNoInteractions(provinciaRepository);
        verify(paisRepository, never()).delete(any());
    }

    @Test
    @DisplayName("CP-BP-02: bajaPais lanza ReglaNegocioException y no elimina cuando el país tiene provincias activas asociadas")
    void bajaPais_debeRechazarConProvinciasActivasAsociadas() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(provinciaRepository.existsByPaisId(1L)).thenReturn(true);

        assertThatThrownBy(() -> paisServicio.bajaPais(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el país porque tiene provincias activas asociadas");

        verify(provinciaRepository).existsByPaisId(1L);
        verify(paisRepository, never()).delete(any());
    }

    @Test
    @DisplayName("CP-BP-03: bajaPais elimina lógicamente (soft-delete) y retorna el DTO cuando no tiene provincias asociadas")
    void bajaPais_debeEliminarYRetornarPaisExistente() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(provinciaRepository.existsByPaisId(1L)).thenReturn(false);

        PaisResponseDTO resultado = paisServicio.bajaPais(1L);

        // El borrado físico a nivel repositorio es convertido a UPDATE por el @SoftDelete de Hibernate
        assertPaisDTO(paisEntity, resultado);
        verify(paisRepository).findById(1L);
        verify(provinciaRepository).existsByPaisId(1L);
        verify(paisRepository).delete(paisEntity);
    }

    // ==================== helpers ====================

    private static PaisEntity crearPaisEntity(Long id, String nombre) {
        return PaisEntity.builder()
                .id(id)
                .nombre(nombre)
                .build();
    }

    private static PaisFormDTO paisFormDTO(String nombre) {
        return PaisFormDTO.builder()
                .nombre(nombre)
                .build();
    }

    private static void assertPaisDTO(PaisEntity entidad, PaisResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
    }
}
