package com.github.heikyudev.maestrocervecero.service.implementation.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.PaisEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IPaisRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IProvinciaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.PaisFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.PaisResponseDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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

    // ==================== filtrarPaises ====================

    @Test
    @DisplayName("CP-FP-01: filtrarPaises retorna una página de países correctamente mapeada a DTO cuando se filtra por nombre")
    void filtrarPaises_debeRetornarPaginaMapeadaFiltrandoPorNombre() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        when(paisRepository.filtrarPaises("Arg", pageable)).thenReturn(new PageImpl<>(List.of(paisEntity), pageable, 1));

        // === EJECUCION ===
        Page<PaisResponseDTO> resultado = paisServicio.filtrarPaises("Arg", pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertPaisDTO(paisEntity, resultado.getContent().get(0));
        verify(paisRepository).filtrarPaises("Arg", pageable);
    }

    @Test
    @DisplayName("CP-FP-02: filtrarPaises propaga el nombre nulo sin restringir la búsqueda")
    void filtrarPaises_debePropagarNombreNulo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        PaisEntity otroPaisEntity = crearPaisEntity(2L, "Brasil");
        PaisEntity tercerPaisEntity = crearPaisEntity(3L, "Uruguay");
        when(paisRepository.filtrarPaises(null, pageable)).thenReturn(new PageImpl<>(List.of(paisEntity, otroPaisEntity, tercerPaisEntity), pageable, 3));

        // === EJECUCION ===
        Page<PaisResponseDTO> resultado = paisServicio.filtrarPaises(null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        verify(paisRepository).filtrarPaises(null, pageable);
    }

    @Test
    @DisplayName("CP-FP-03: filtrarPaises retorna una página vacía cuando ningún registro cumple el criterio")
    void filtrarPaises_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(paisRepository.filtrarPaises("Inexistente", pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<PaisResponseDTO> resultado = paisServicio.filtrarPaises("Inexistente", pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(paisRepository).filtrarPaises("Inexistente", pageable);
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
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para países dados de baja
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paisServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el país con ID: 99");
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
        // El alta siempre debe registrar al país como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Uruguay");
        verify(paisRepository).existsByNombreIgnoreCase("Uruguay");
    }

    @Test
    @DisplayName("CP-AP-04: altaPais lanza ReglaNegocioException y no consulta la BD cuando el nombre supera los 100 caracteres")
    void altaPais_debeRechazarNombreDemasiadoLargo() {
        PaisFormDTO paisFormDTO = paisFormDTO("a".repeat(101));

        assertThatThrownBy(() -> paisServicio.altaPais(paisFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre no puede superar 100 caracteres");

        verifyNoInteractions(paisRepository);
    }

    // ==================== modificarPais ====================

    @Test
    @DisplayName("CP-MP-01: modificarPais lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarPais_debeLanzarExcepcionSiNoExiste() {
        PaisFormDTO paisFormDTO = paisFormDTO("Argentina");
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paisServicio.modificarPais(99L, paisFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el país con ID: 99");

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

    @Test
    @DisplayName("CP-MP-05: modificarPais lanza ReglaNegocioException y no consulta la BD cuando el nombre supera los 100 caracteres")
    void modificarPais_debeRechazarNombreDemasiadoLargo() {
        PaisFormDTO paisFormDTO = paisFormDTO("a".repeat(101));

        assertThatThrownBy(() -> paisServicio.modificarPais(1L, paisFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre no puede superar 100 caracteres");

        verifyNoInteractions(paisRepository);
    }

    // ==================== bajaPais ====================

    @Test
    @DisplayName("CP-BP-01: bajaPais lanza RecursoNoEncontradoException y no consulta provincias ni persiste cuando el ID no existe")
    void bajaPais_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(paisRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paisServicio.bajaPais(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el país con ID: 99");

        verify(paisRepository).findById(99L);
        verifyNoInteractions(provinciaRepository);
        verify(paisRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BP-02: bajaPais lanza ReglaNegocioException y no persiste cuando el país tiene provincias activas asociadas")
    void bajaPais_debeRechazarConProvinciasActivasAsociadas() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(provinciaRepository.existsByPaisId(1L)).thenReturn(true);

        assertThatThrownBy(() -> paisServicio.bajaPais(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el país porque tiene provincias activas asociadas");

        verify(provinciaRepository).existsByPaisId(1L);
        verify(paisRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BP-03: bajaPais marca el estado como BAJA, persiste y retorna el DTO cuando no tiene provincias asociadas")
    void bajaPais_debeMarcarBajaYRetornarPaisExistente() {
        PaisEntity paisEntity = crearPaisEntity(1L, "Argentina");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(paisEntity));
        when(provinciaRepository.existsByPaisId(1L)).thenReturn(false);
        when(paisRepository.save(paisEntity)).thenReturn(paisEntity);

        PaisResponseDTO resultado = paisServicio.bajaPais(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(paisEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertPaisDTO(paisEntity, resultado);
        verify(paisRepository).findById(1L);
        verify(provinciaRepository).existsByPaisId(1L);
        verify(paisRepository).save(paisEntity);
        verify(paisRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static PaisEntity crearPaisEntity(Long id, String nombre) {
        return PaisEntity.builder()
                .id(id)
                .nombre(nombre)
                .estado(Estado.ACTIVO)
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
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }

    // ==================== normalización y unicidad ====================

    @Test
    @DisplayName("CP-AP-05: altaPais guarda el nombre normalizado y consulta la unicidad con ese valor")
    void altaPais_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        PaisFormDTO formDTO = paisFormDTO("  Pilsen   Nacional  ");
        when(paisRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(paisRepository.save(any(PaisEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        paisServicio.altaPais(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<PaisEntity> captor = ArgumentCaptor.forClass(PaisEntity.class);
        verify(paisRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(formDTO.getNombre()).isEqualTo("  Pilsen   Nacional  ");
        verify(paisRepository).existsByNombreIgnoreCase("Pilsen Nacional");
        verify(paisRepository).flush();
    }

    @Test
    @DisplayName("CP-AP-06: altaPais lanza ReglaNegocioException y no consulta el repositorio cuando el nombre está vacío")
    void altaPais_debeRechazarValorVacio() {
        PaisFormDTO formDTO = paisFormDTO("   ");

        assertThatThrownBy(() -> paisServicio.altaPais(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(paisRepository);
    }

    @Test
    @DisplayName("CP-AP-07: altaPais lanza RecursoDuplicadoException cuando el nombre normalizado ya existe")
    void altaPais_debeDetectarDuplicadoConElValorNormalizado() {
        PaisFormDTO formDTO = paisFormDTO("  pilsen ");
        when(paisRepository.existsByNombreIgnoreCase("pilsen")).thenReturn(true);

        assertThatThrownBy(() -> paisServicio.altaPais(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un país con el nombre 'pilsen'");

        verify(paisRepository).existsByNombreIgnoreCase("pilsen");
        verify(paisRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-08: altaPais traduce la violación del índice único udx_pais_nombre al mensaje de duplicado")
    void altaPais_debeTraducirLaViolacionDelIndiceUnico() {
        PaisFormDTO formDTO = paisFormDTO("Pilsen Nacional");
        when(paisRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(paisRepository.save(any(PaisEntity.class))).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_pais_nombre"));

        assertThatThrownBy(() -> paisServicio.altaPais(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un país con el nombre 'Pilsen Nacional'");
    }

    @Test
    @DisplayName("CP-AP-09: altaPais permite reutilizar el nombre de un registro dado de baja")
    void altaPais_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        PaisFormDTO formDTO = paisFormDTO("Pilsen Nacional");
        // El único registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(paisRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(paisRepository.save(any(PaisEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> paisServicio.altaPais(formDTO)).doesNotThrowAnyException();

        verify(paisRepository).save(any(PaisEntity.class));
    }

    @Test
    @DisplayName("CP-AP-10: altaPais permite un nombre que difiere de otro solo en una tilde")
    void altaPais_debeTratarComoDistintosLosNombresConDistintaTilde() {
        PaisFormDTO formDTO = paisFormDTO("Pílsen");
        when(paisRepository.existsByNombreIgnoreCase("Pílsen")).thenReturn(false);
        when(paisRepository.save(any(PaisEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> paisServicio.altaPais(formDTO)).doesNotThrowAnyException();

        verify(paisRepository).existsByNombreIgnoreCase("Pílsen");
        verify(paisRepository).save(any(PaisEntity.class));
    }

    @Test
    @DisplayName("CP-MP-06: modificarPais guarda el nombre normalizado y consulta la unicidad con ese valor")
    void modificarPais_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        PaisFormDTO formDTO = paisFormDTO("  Pilsen   Nacional  ");
        PaisEntity existente = crearPaisEntity(1L, "Original");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(paisRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(paisRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        paisServicio.modificarPais(1L, formDTO);

        // === ASSERTS ===
        assertThat(existente.getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(formDTO.getNombre()).isEqualTo("  Pilsen   Nacional  ");
        verify(paisRepository).existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L);
        verify(paisRepository).save(existente);
        verify(paisRepository).flush();
    }

    @Test
    @DisplayName("CP-MP-07: modificarPais lanza ReglaNegocioException cuando el nombre está vacío")
    void modificarPais_debeRechazarValorVacio() {
        PaisFormDTO formDTO = paisFormDTO("   ");

        assertThatThrownBy(() -> paisServicio.modificarPais(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(paisRepository);
    }

    @Test
    @DisplayName("CP-MP-08: modificarPais lanza RecursoDuplicadoException cuando el nombre normalizado está en uso por otro registro")
    void modificarPais_debeDetectarDuplicadoConElValorNormalizado() {
        PaisFormDTO formDTO = paisFormDTO("  pilsen ");
        PaisEntity existente = crearPaisEntity(1L, "Original");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(paisRepository.existsByNombreIgnoreCaseAndIdNot("pilsen", 1L)).thenReturn(true);

        assertThatThrownBy(() -> paisServicio.modificarPais(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'pilsen' ya está en uso por otro país");

        verify(paisRepository).existsByNombreIgnoreCaseAndIdNot("pilsen", 1L);
        verify(paisRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-09: modificarPais traduce la violación del índice único udx_pais_nombre al mensaje de duplicado")
    void modificarPais_debeTraducirLaViolacionDelIndiceUnico() {
        PaisFormDTO formDTO = paisFormDTO("Pilsen Nacional");
        PaisEntity existente = crearPaisEntity(1L, "Original");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(paisRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(paisRepository.save(existente)).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_pais_nombre"));

        assertThatThrownBy(() -> paisServicio.modificarPais(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Pilsen Nacional' ya está en uso por otro país");
    }

    @Test
    @DisplayName("CP-MP-10: modificarPais permite reutilizar el nombre de un registro dado de baja")
    void modificarPais_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        PaisFormDTO formDTO = paisFormDTO("Pilsen Nacional");
        PaisEntity existente = crearPaisEntity(1L, "Original");
        when(paisRepository.findById(1L)).thenReturn(Optional.of(existente));
        // El único otro registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(paisRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(paisRepository.save(existente)).thenReturn(existente);

        assertThatCode(() -> paisServicio.modificarPais(1L, formDTO)).doesNotThrowAnyException();

        assertThat(existente.getNombre()).isEqualTo("Pilsen Nacional");
        verify(paisRepository).save(existente);
    }
}
