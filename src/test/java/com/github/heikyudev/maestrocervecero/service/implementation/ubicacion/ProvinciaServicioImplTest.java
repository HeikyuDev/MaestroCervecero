package com.github.heikyudev.maestrocervecero.service.implementation.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.PaisEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.ProvinciaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IPaisRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IProvinciaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.ProvinciaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;
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
class ProvinciaServicioImplTest {

    @Mock
    private IProvinciaRepository provinciaRepository;

    @Mock
    private IPaisRepository paisRepository;

    @Mock
    private ILocalidadRepository localidadRepository;

    @InjectMocks
    private ProvinciaServicioImpl provinciaServicio;

    // ==================== filtrarProvincias ====================

    @Test
    @DisplayName("CP-FPr-01: filtrarProvincias retorna una página de provincias correctamente mapeada a DTO cuando se filtra por nombre e idPais")
    void filtrarProvincias_debeRetornarPaginaMapeadaFiltrandoPorNombreEIdPais() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaEntity provinciaEntity = crearProvinciaEntity(1L, "Misiones", paisEntity);
        when(provinciaRepository.filtrarProvincias("Mis", 1L, pageable)).thenReturn(new PageImpl<>(List.of(provinciaEntity), pageable, 1));

        // === EJECUCION ===
        Page<ProvinciaResponseDTO> resultado = provinciaServicio.filtrarProvincias("Mis", 1L, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertProvinciaDTO(provinciaEntity, resultado.getContent().get(0));
        verify(provinciaRepository).filtrarProvincias("Mis", 1L, pageable);
    }

    @Test
    @DisplayName("CP-FPr-02: filtrarProvincias propaga nombre e idPais nulos sin restringir esos criterios")
    void filtrarProvincias_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaEntity provinciaEntity = crearProvinciaEntity(1L, "Misiones", paisEntity);
        ProvinciaEntity otraProvinciaEntity = crearProvinciaEntity(2L, "Corrientes", paisEntity);
        ProvinciaEntity terceraProvinciaEntity = crearProvinciaEntity(3L, "Formosa", paisEntity);
        when(provinciaRepository.filtrarProvincias(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(provinciaEntity, otraProvinciaEntity, terceraProvinciaEntity), pageable, 3));

        // === EJECUCION ===
        Page<ProvinciaResponseDTO> resultado = provinciaServicio.filtrarProvincias(null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        verify(provinciaRepository).filtrarProvincias(null, null, pageable);
    }

    @Test
    @DisplayName("CP-FPr-03: filtrarProvincias retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarProvincias_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(provinciaRepository.filtrarProvincias("Inexistente", null, pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<ProvinciaResponseDTO> resultado = provinciaServicio.filtrarProvincias("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(provinciaRepository).filtrarProvincias("Inexistente", null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la provincia cuando el ID existe")
    void buscarPorId_debeRetornarProvinciaExistente() {
        // === PREPARACION DE DATOS ===
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaEntity provinciaEntity = crearProvinciaEntity(1L, "Misiones", paisEntity);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));

        // === EJECUCION ===
        ProvinciaResponseDTO resultado = provinciaServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertProvinciaDTO(provinciaEntity, resultado);
        verify(provinciaRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dada de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para provincias dadas de baja
        when(provinciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provinciaServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La provincia no existe");
        verify(provinciaRepository).findById(99L);
    }

    // ==================== altaProvincia ====================

    @Test
    @DisplayName("CP-APR-01: altaProvincia lanza RecursoNoEncontradoException y no valida duplicación ni persiste cuando el país no existe")
    void altaProvincia_debeRechazarPaisInexistente() {
        ProvinciaFormDTO provinciaFormDTO = provinciaFormDTO("Misiones", 99L);
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provinciaServicio.altaProvincia(provinciaFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El país no existe");

        verify(paisRepository).findById(99L);
        verify(provinciaRepository, never()).existsByNombreIgnoreCaseAndPaisId(any(), any());
        verify(provinciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-APR-02: altaProvincia lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe en el mismo país")
    void altaProvincia_debeRechazarNombreDuplicadoEnMismoPais() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaFormDTO provinciaFormDTO = provinciaFormDTO("Misiones", 1L);
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(provinciaRepository.existsByNombreIgnoreCaseAndPaisId("Misiones", 1L)).thenReturn(true);

        assertThatThrownBy(() -> provinciaServicio.altaProvincia(provinciaFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una provincia con el nombre 'Misiones' para el país seleccionado");

        verify(provinciaRepository).existsByNombreIgnoreCaseAndPaisId("Misiones", 1L);
        verify(provinciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-APR-03: altaProvincia lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case en el mismo país (case-insensitive)")
    void altaProvincia_debeRechazarNombreDuplicadoCaseInsensitive() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaFormDTO provinciaFormDTO = provinciaFormDTO("misiones", 1L);
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(provinciaRepository.existsByNombreIgnoreCaseAndPaisId("misiones", 1L)).thenReturn(true);

        assertThatThrownBy(() -> provinciaServicio.altaProvincia(provinciaFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una provincia con el nombre 'misiones' para el país seleccionado");

        verify(provinciaRepository).existsByNombreIgnoreCaseAndPaisId("misiones", 1L);
        verify(provinciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-APR-04: altaProvincia permite el mismo nombre en distinto país")
    void altaProvincia_debePermitirMismoNombreEnDistintoPais() {
        // === PREPARACION DE DATOS ===
        PaisEntity paisEntity = crearPaisEntity(2L, "Brasil");
        ProvinciaFormDTO provinciaFormDTO = provinciaFormDTO("Misiones", 2L);
        when(paisRepository.findById(2L)).thenReturn(Optional.of(paisEntity));
        when(provinciaRepository.existsByNombreIgnoreCaseAndPaisId("Misiones", 2L)).thenReturn(false);
        when(provinciaRepository.save(any(ProvinciaEntity.class))).thenAnswer(invocation -> {
            ProvinciaEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(5L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        ProvinciaResponseDTO resultado = provinciaServicio.altaProvincia(provinciaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<ProvinciaEntity> captor = ArgumentCaptor.forClass(ProvinciaEntity.class);
        verify(provinciaRepository).save(captor.capture());
        ProvinciaEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Misiones");
        assertThat(entidadCapturada.getPais()).isEqualTo(paisEntity);

        assertThat(resultado.getPais().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("CP-APR-05: altaProvincia persiste y retorna el DTO cuando el nombre es único en el país (camino feliz)")
    void altaProvincia_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaFormDTO provinciaFormDTO = provinciaFormDTO("Corrientes", 1L);
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(provinciaRepository.existsByNombreIgnoreCaseAndPaisId("Corrientes", 1L)).thenReturn(false);
        when(provinciaRepository.save(any(ProvinciaEntity.class))).thenAnswer(invocation -> {
            ProvinciaEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        ProvinciaResponseDTO resultado = provinciaServicio.altaProvincia(provinciaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<ProvinciaEntity> captor = ArgumentCaptor.forClass(ProvinciaEntity.class);
        verify(provinciaRepository).save(captor.capture());
        ProvinciaEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Corrientes");
        assertThat(entidadCapturada.getPais()).isEqualTo(paisEntity);
        // El alta siempre debe registrar a la provincia como ACTIVA, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Corrientes");
        assertThat(resultado.getPais().getId()).isEqualTo(1L);
    }

    // ==================== modificarProvincia ====================

    @Test
    @DisplayName("CP-MPR-01: modificarProvincia lanza RecursoNoEncontradoException y no consulta país ni persiste cuando el ID no existe")
    void modificarProvincia_debeLanzarExcepcionSiNoExiste() {
        ProvinciaFormDTO provinciaFormDTO = provinciaFormDTO("Misiones", 1L);
        when(provinciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provinciaServicio.modificarProvincia(99L, provinciaFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La provincia no existe");

        verify(provinciaRepository).findById(99L);
        verifyNoInteractions(paisRepository);
        verify(provinciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MPR-02: modificarProvincia lanza RecursoNoEncontradoException y no valida duplicación ni persiste cuando el país no existe")
    void modificarProvincia_debeRechazarPaisInexistente() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaEntity provinciaEntity = crearProvinciaEntity(1L, "Misiones", paisEntity);
        ProvinciaFormDTO provinciaFormDTO = provinciaFormDTO("Misiones", 99L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provinciaServicio.modificarProvincia(1L, provinciaFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El país no existe");

        verify(paisRepository).findById(99L);
        verify(provinciaRepository, never()).existsByNombreIgnoreCaseAndPaisIdAndIdNot(any(), any(), any());
        verify(provinciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MPR-03: modificarProvincia lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otra provincia del mismo país")
    void modificarProvincia_debeRechazarNombreEnUsoPorOtraProvinciaDelMismoPais() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaEntity provinciaEntity = crearProvinciaEntity(1L, "Misiones", paisEntity);
        ProvinciaFormDTO provinciaFormDTO = provinciaFormDTO("Corrientes", 1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(provinciaRepository.existsByNombreIgnoreCaseAndPaisIdAndIdNot("Corrientes", 1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> provinciaServicio.modificarProvincia(1L, provinciaFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Corrientes' ya está en uso por otra provincia del país seleccionado");

        verify(provinciaRepository).existsByNombreIgnoreCaseAndPaisIdAndIdNot("Corrientes", 1L, 1L);
        verify(provinciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MPR-04: modificarProvincia permite conservar el propio nombre actual al actualizar")
    void modificarProvincia_debePermitirConservarNombrePropio() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaEntity provinciaEntity = crearProvinciaEntity(1L, "Misiones", paisEntity);
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        ProvinciaFormDTO provinciaFormDTO = provinciaFormDTO("misiones", 1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(provinciaRepository.existsByNombreIgnoreCaseAndPaisIdAndIdNot("misiones", 1L, 1L)).thenReturn(false);
        when(provinciaRepository.save(provinciaEntity)).thenReturn(provinciaEntity);

        ProvinciaResponseDTO resultado = provinciaServicio.modificarProvincia(1L, provinciaFormDTO);

        assertThat(resultado.getNombre()).isEqualTo("misiones");
        verify(provinciaRepository).save(provinciaEntity);
    }

    @Test
    @DisplayName("CP-MPR-05: modificarProvincia permite reasignar la provincia a otro país")
    void modificarProvincia_debePermitirReasignacionDePais() {
        // === PREPARACION DE DATOS ===
        PaisEntity paisOrigen = crearPaisEntity(1L, "Argentina");
        PaisEntity paisDestino = crearPaisEntity(2L, "Brasil");
        ProvinciaEntity provinciaEntity = crearProvinciaEntity(1L, "Misiones", paisOrigen);
        ProvinciaFormDTO provinciaFormDTO = provinciaFormDTO("Misiones", 2L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(paisRepository.findById(2L)).thenReturn(Optional.of(paisDestino));
        when(provinciaRepository.existsByNombreIgnoreCaseAndPaisIdAndIdNot("Misiones", 2L, 1L)).thenReturn(false);
        when(provinciaRepository.save(provinciaEntity)).thenReturn(provinciaEntity);

        // === EJECUCION ===
        ProvinciaResponseDTO resultado = provinciaServicio.modificarProvincia(1L, provinciaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<ProvinciaEntity> captor = ArgumentCaptor.forClass(ProvinciaEntity.class);
        verify(provinciaRepository).save(captor.capture());
        assertThat(captor.getValue().getPais()).isEqualTo(paisDestino);
        assertThat(resultado.getPais().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("CP-MPR-06: modificarProvincia actualiza los datos y persiste cuando el ID existe y el nombre está libre (camino feliz)")
    void modificarProvincia_debeActualizarProvinciaExistente() {
        // === PREPARACION DE DATOS ===
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaEntity provinciaEntity = crearProvinciaEntity(1L, "Misiones", paisEntity);
        ProvinciaFormDTO provinciaFormDTO = provinciaFormDTO("Misiones Actualizada", 1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(provinciaRepository.existsByNombreIgnoreCaseAndPaisIdAndIdNot("Misiones Actualizada", 1L, 1L)).thenReturn(false);
        when(provinciaRepository.save(provinciaEntity)).thenReturn(provinciaEntity);

        // === EJECUCION ===
        ProvinciaResponseDTO resultado = provinciaServicio.modificarProvincia(1L, provinciaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<ProvinciaEntity> captor = ArgumentCaptor.forClass(ProvinciaEntity.class);
        verify(provinciaRepository).save(captor.capture());
        ProvinciaEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Misiones Actualizada");
        assertThat(entidadCapturada.getPais()).isEqualTo(paisEntity);

        assertThat(resultado.getNombre()).isEqualTo("Misiones Actualizada");
        verify(provinciaRepository).findById(1L);
        verify(paisRepository).findById(1L);
        verify(provinciaRepository).existsByNombreIgnoreCaseAndPaisIdAndIdNot("Misiones Actualizada", 1L, 1L);
    }

    // ==================== bajaProvincia ====================

    @Test
    @DisplayName("CP-BPR-01: bajaProvincia lanza RecursoNoEncontradoException y no consulta localidades ni persiste cuando el ID no existe")
    void bajaProvincia_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(provinciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provinciaServicio.bajaProvincia(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la provincia con ID: 99");

        verify(provinciaRepository).findById(99L);
        verifyNoInteractions(localidadRepository);
        verify(provinciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BPR-02: bajaProvincia lanza ReglaNegocioException y no persiste cuando la provincia tiene localidades activas asociadas")
    void bajaProvincia_debeRechazarConLocalidadesActivasAsociadas() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaEntity provinciaEntity = crearProvinciaEntity(1L, "Misiones", paisEntity);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(localidadRepository.existsByProvinciaId(1L)).thenReturn(true);

        assertThatThrownBy(() -> provinciaServicio.bajaProvincia(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la provincia porque tiene localidades activas asociadas");

        verify(localidadRepository).existsByProvinciaId(1L);
        verify(provinciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BPR-03: bajaProvincia marca el estado como BAJA, persiste y retorna el DTO cuando no tiene localidades asociadas")
    void bajaProvincia_debeMarcarBajaYRetornarProvinciaExistente() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        ProvinciaEntity provinciaEntity = crearProvinciaEntity(1L, "Misiones", paisEntity);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(localidadRepository.existsByProvinciaId(1L)).thenReturn(false);
        when(provinciaRepository.save(provinciaEntity)).thenReturn(provinciaEntity);

        ProvinciaResponseDTO resultado = provinciaServicio.bajaProvincia(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(provinciaEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertProvinciaDTO(provinciaEntity, resultado);
        verify(provinciaRepository).findById(1L);
        verify(localidadRepository).existsByProvinciaId(1L);
        verify(provinciaRepository).save(provinciaEntity);
        verify(provinciaRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static PaisEntity crearPaisEntity(Long id, String nombre) {
        return PaisEntity.builder()
                .id(id)
                .nombre(nombre)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static ProvinciaEntity crearProvinciaEntity(Long id, String nombre, PaisEntity pais) {
        return ProvinciaEntity.builder()
                .id(id)
                .nombre(nombre)
                .pais(pais)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static ProvinciaFormDTO provinciaFormDTO(String nombre, Long idPais) {
        return ProvinciaFormDTO.builder()
                .nombre(nombre)
                .idPais(idPais)
                .build();
    }

    private static void assertProvinciaDTO(ProvinciaEntity entidad, ProvinciaResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getPais().getId()).isEqualTo(entidad.getPais().getId());
        assertThat(dto.getPais().getNombre()).isEqualTo(entidad.getPais().getNombre());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
