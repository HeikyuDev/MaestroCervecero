package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFabricanteBarrilRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FabricanteBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.FabricanteBarrilResponseDTO;
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
class FabricanteBarrilServicioImplTest {

    @Mock
    private IFabricanteBarrilRepository fabricanteBarrilRepository;

    @Mock
    private IBarrilRepository barrilRepository;

    @InjectMocks
    private FabricanteBarrilServicioImpl fabricanteBarrilServicio;

    // ==================== filtrarFabricantesBarril ====================

    @Test
    @DisplayName("CP-FFB-01: filtrarFabricantesBarril retorna una página de fabricantes correctamente mapeada a DTO cuando se filtra por nombre")
    void filtrarFabricantesBarril_debeRetornarPaginaMapeadaFiltrandoPorNombre() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur");
        FabricanteBarrilEntity otroFabricanteEntity = crearFabricanteBarrilEntity(2L, "Tonelería del Sur Hnos.");
        when(fabricanteBarrilRepository.filtrarFabricantesBarril("Tonelería", pageable))
                .thenReturn(new PageImpl<>(List.of(fabricanteEntity, otroFabricanteEntity), pageable, 2));

        // === EJECUCION ===
        Page<FabricanteBarrilResponseDTO> resultado = fabricanteBarrilServicio.filtrarFabricantesBarril("Tonelería", pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertFabricanteBarrilDTO(fabricanteEntity, resultado.getContent().get(0));
        assertFabricanteBarrilDTO(otroFabricanteEntity, resultado.getContent().get(1));
        verify(fabricanteBarrilRepository).filtrarFabricantesBarril("Tonelería", pageable);
    }

    @Test
    @DisplayName("CP-FFB-02: filtrarFabricantesBarril propaga el nombre nulo sin restringir la búsqueda")
    void filtrarFabricantesBarril_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur");
        FabricanteBarrilEntity otroFabricanteEntity = crearFabricanteBarrilEntity(2L, "Tonelería del Norte");
        when(fabricanteBarrilRepository.filtrarFabricantesBarril(null, pageable))
                .thenReturn(new PageImpl<>(List.of(fabricanteEntity, otroFabricanteEntity), pageable, 2));

        // === EJECUCION ===
        Page<FabricanteBarrilResponseDTO> resultado = fabricanteBarrilServicio.filtrarFabricantesBarril(null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(fabricanteBarrilRepository).filtrarFabricantesBarril(null, pageable);
    }

    @Test
    @DisplayName("CP-FFB-03: filtrarFabricantesBarril retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarFabricantesBarril_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(fabricanteBarrilRepository.filtrarFabricantesBarril("Inexistente", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<FabricanteBarrilResponseDTO> resultado = fabricanteBarrilServicio.filtrarFabricantesBarril("Inexistente", pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(fabricanteBarrilRepository).filtrarFabricantesBarril("Inexistente", pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del fabricante de barril cuando el ID existe")
    void buscarPorId_debeRetornarFabricanteExistente() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));

        // === EJECUCION ===
        FabricanteBarrilResponseDTO resultado = fabricanteBarrilServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertFabricanteBarrilDTO(fabricanteEntity, resultado);
        verify(fabricanteBarrilRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para fabricantes dados de baja
        when(fabricanteBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fabricanteBarrilServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fabricante de barril con ID: 99");
        verify(fabricanteBarrilRepository).findById(99L);
    }

    // ==================== altaFabricanteBarril ====================

    @Test
    @DisplayName("CP-AFB-01: altaFabricanteBarril lanza RecursoDuplicadoException y no persiste cuando el nombre ya está registrado por un fabricante activo")
    void altaFabricanteBarril_debeRechazarNombreDuplicado() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Sur");
        when(fabricanteBarrilRepository.existsByNombreIgnoreCase("Tonelería del Sur")).thenReturn(true);

        assertThatThrownBy(() -> fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un fabricante de barril con el nombre 'Tonelería del Sur'");

        verify(fabricanteBarrilRepository).existsByNombreIgnoreCase("Tonelería del Sur");
        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-02: altaFabricanteBarril lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case (case-insensitive)")
    void altaFabricanteBarril_debeRechazarNombreDuplicadoCaseInsensitive() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("tonelería del sur");
        when(fabricanteBarrilRepository.existsByNombreIgnoreCase("tonelería del sur")).thenReturn(true);

        assertThatThrownBy(() -> fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un fabricante de barril con el nombre 'tonelería del sur'");

        verify(fabricanteBarrilRepository).existsByNombreIgnoreCase("tonelería del sur");
        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-03: altaFabricanteBarril persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaFabricanteBarril_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Sur");
        when(fabricanteBarrilRepository.existsByNombreIgnoreCase("Tonelería del Sur")).thenReturn(false);
        when(fabricanteBarrilRepository.save(any(FabricanteBarrilEntity.class))).thenAnswer(invocation -> {
            FabricanteBarrilEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        FabricanteBarrilResponseDTO resultado = fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<FabricanteBarrilEntity> captor = ArgumentCaptor.forClass(FabricanteBarrilEntity.class);
        verify(fabricanteBarrilRepository).save(captor.capture());
        FabricanteBarrilEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Tonelería del Sur");
        // El alta siempre debe registrar al fabricante como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Tonelería del Sur");
        assertThat(resultado.getEstado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    @DisplayName("CP-AFB-04: altaFabricanteBarril guarda el nombre normalizado y consulta la unicidad con ese valor")
    void altaFabricanteBarril_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("  Tonelería   del  Sur  ");
        when(fabricanteBarrilRepository.existsByNombreIgnoreCase("Tonelería del Sur")).thenReturn(false);
        when(fabricanteBarrilRepository.save(any(FabricanteBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<FabricanteBarrilEntity> captor = ArgumentCaptor.forClass(FabricanteBarrilEntity.class);
        verify(fabricanteBarrilRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Tonelería del Sur");
        assertThat(fabricanteBarrilFormDTO.getNombre()).isEqualTo("  Tonelería   del  Sur  ");
        verify(fabricanteBarrilRepository).existsByNombreIgnoreCase("Tonelería del Sur");
        verify(fabricanteBarrilRepository).flush();
    }

    @Test
    @DisplayName("CP-AFB-05: altaFabricanteBarril lanza ReglaNegocioException y no consulta el repositorio cuando el nombre está vacío")
    void altaFabricanteBarril_debeRechazarNombreVacio() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("   ");

        assertThatThrownBy(() -> fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(fabricanteBarrilRepository);
    }

    @Test
    @DisplayName("CP-AFB-06: altaFabricanteBarril lanza RecursoDuplicadoException cuando el nombre normalizado ya existe")
    void altaFabricanteBarril_debeDetectarDuplicadoConElValorNormalizado() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("  tonelería  del sur ");
        when(fabricanteBarrilRepository.existsByNombreIgnoreCase("tonelería del sur")).thenReturn(true);

        assertThatThrownBy(() -> fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un fabricante de barril con el nombre 'tonelería del sur'");

        verify(fabricanteBarrilRepository).existsByNombreIgnoreCase("tonelería del sur");
        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-07: altaFabricanteBarril traduce la violación del índice único udx_fabricante_barril_nombre al mensaje de duplicado")
    void altaFabricanteBarril_debeTraducirLaViolacionDelIndiceUnico() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Sur");
        when(fabricanteBarrilRepository.existsByNombreIgnoreCase("Tonelería del Sur")).thenReturn(false);
        when(fabricanteBarrilRepository.save(any(FabricanteBarrilEntity.class)))
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_fabricante_barril_nombre"));

        assertThatThrownBy(() -> fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un fabricante de barril con el nombre 'Tonelería del Sur'");
    }

    @Test
    @DisplayName("CP-AFB-08: altaFabricanteBarril permite reutilizar el nombre de un fabricante dado de baja")
    void altaFabricanteBarril_debePermitirReutilizarElNombreDeUnFabricanteDadoDeBaja() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Sur");
        // El único fabricante con ese nombre está dado de baja: la consulta solo considera fabricantes activos
        when(fabricanteBarrilRepository.existsByNombreIgnoreCase("Tonelería del Sur")).thenReturn(false);
        when(fabricanteBarrilRepository.save(any(FabricanteBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO)).doesNotThrowAnyException();

        verify(fabricanteBarrilRepository).save(any(FabricanteBarrilEntity.class));
    }

    @Test
    @DisplayName("CP-AFB-09: altaFabricanteBarril lanza ReglaNegocioException y no consulta el repositorio cuando el nombre supera los 100 caracteres")
    void altaFabricanteBarril_debeRechazarNombreQueSuperaElTope() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("a".repeat(101));

        assertThatThrownBy(() -> fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre no puede superar 100 caracteres");

        verifyNoInteractions(fabricanteBarrilRepository);
    }

    @Test
    @DisplayName("CP-AFB-10: altaFabricanteBarril persiste un nombre de exactamente 100 caracteres (límite)")
    void altaFabricanteBarril_debePermitirNombreEnElTope() {
        String nombre = "a".repeat(100);
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO(nombre);
        when(fabricanteBarrilRepository.existsByNombreIgnoreCase(nombre)).thenReturn(false);
        when(fabricanteBarrilRepository.save(any(FabricanteBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO)).doesNotThrowAnyException();

        verify(fabricanteBarrilRepository).save(any(FabricanteBarrilEntity.class));
    }

    // ==================== modificarFabricanteBarril ====================

    @Test
    @DisplayName("CP-MFB-01: modificarFabricanteBarril lanza RecursoNoEncontradoException y no valida duplicados ni persiste cuando el ID no existe")
    void modificarFabricanteBarril_debeLanzarExcepcionSiNoExiste() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Sur");
        when(fabricanteBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fabricanteBarrilServicio.modificarFabricanteBarril(99L, fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fabricante de barril con ID: 99");

        verify(fabricanteBarrilRepository).findById(99L);
        verify(fabricanteBarrilRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MFB-02: modificarFabricanteBarril lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otro fabricante activo")
    void modificarFabricanteBarril_debeRechazarNombreEnUsoPorOtroFabricante() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur");
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Norte");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(fabricanteBarrilRepository.existsByNombreIgnoreCaseAndIdNot("Tonelería del Norte", 1L)).thenReturn(true);

        assertThatThrownBy(() -> fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Tonelería del Norte' ya está en uso por otro fabricante de barril");

        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MFB-03: modificarFabricanteBarril lanza RecursoDuplicadoException cuando el nombre está en uso por otro fabricante activo con distinto case (case-insensitive)")
    void modificarFabricanteBarril_debeRechazarNombreEnUsoPorOtroFabricanteCaseInsensitive() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur");
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("tonelería del norte");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(fabricanteBarrilRepository.existsByNombreIgnoreCaseAndIdNot("tonelería del norte", 1L)).thenReturn(true);

        assertThatThrownBy(() -> fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'tonelería del norte' ya está en uso por otro fabricante de barril");

        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MFB-04: modificarFabricanteBarril permite conservar el nombre propio y persiste (camino feliz)")
    void modificarFabricanteBarril_debePermitirConservarNombrePropio() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur");
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Sur");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(fabricanteBarrilRepository.existsByNombreIgnoreCaseAndIdNot("Tonelería del Sur", 1L)).thenReturn(false);
        when(fabricanteBarrilRepository.save(fabricanteEntity)).thenReturn(fabricanteEntity);

        FabricanteBarrilResponseDTO resultado = fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO);

        assertThat(resultado.getNombre()).isEqualTo("Tonelería del Sur");
        verify(fabricanteBarrilRepository).save(fabricanteEntity);
    }

    @Test
    @DisplayName("CP-MFB-05: modificarFabricanteBarril actualiza el nombre y persiste cuando el nuevo nombre es válido y único (camino feliz)")
    void modificarFabricanteBarril_debeActualizarFabricanteExistente() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería Vieja");
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería Nueva");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(fabricanteBarrilRepository.existsByNombreIgnoreCaseAndIdNot("Tonelería Nueva", 1L)).thenReturn(false);
        when(fabricanteBarrilRepository.save(fabricanteEntity)).thenReturn(fabricanteEntity);

        // === EJECUCION ===
        FabricanteBarrilResponseDTO resultado = fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<FabricanteBarrilEntity> captor = ArgumentCaptor.forClass(FabricanteBarrilEntity.class);
        verify(fabricanteBarrilRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Tonelería Nueva");
        assertThat(captor.getValue().getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getNombre()).isEqualTo("Tonelería Nueva");
        verify(fabricanteBarrilRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-MFB-06: modificarFabricanteBarril guarda el nombre normalizado y consulta la unicidad con ese valor")
    void modificarFabricanteBarril_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("  Tonelería   Nueva  ");
        FabricanteBarrilEntity existente = crearFabricanteBarrilEntity(1L, "Tonelería Vieja");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(fabricanteBarrilRepository.existsByNombreIgnoreCaseAndIdNot("Tonelería Nueva", 1L)).thenReturn(false);
        when(fabricanteBarrilRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO);

        // === ASSERTS ===
        assertThat(existente.getNombre()).isEqualTo("Tonelería Nueva");
        assertThat(fabricanteBarrilFormDTO.getNombre()).isEqualTo("  Tonelería   Nueva  ");
        verify(fabricanteBarrilRepository).existsByNombreIgnoreCaseAndIdNot("Tonelería Nueva", 1L);
        verify(fabricanteBarrilRepository).save(existente);
        verify(fabricanteBarrilRepository).flush();
    }

    @Test
    @DisplayName("CP-MFB-07: modificarFabricanteBarril lanza ReglaNegocioException y no consulta el repositorio cuando el nombre está vacío")
    void modificarFabricanteBarril_debeRechazarNombreVacio() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("   ");

        assertThatThrownBy(() -> fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(fabricanteBarrilRepository);
    }

    @Test
    @DisplayName("CP-MFB-08: modificarFabricanteBarril lanza RecursoDuplicadoException cuando el nombre normalizado está en uso por otro fabricante")
    void modificarFabricanteBarril_debeDetectarDuplicadoConElValorNormalizado() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("  tonelería  del norte ");
        FabricanteBarrilEntity existente = crearFabricanteBarrilEntity(1L, "Tonelería del Sur");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(fabricanteBarrilRepository.existsByNombreIgnoreCaseAndIdNot("tonelería del norte", 1L)).thenReturn(true);

        assertThatThrownBy(() -> fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'tonelería del norte' ya está en uso por otro fabricante de barril");

        verify(fabricanteBarrilRepository).existsByNombreIgnoreCaseAndIdNot("tonelería del norte", 1L);
        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MFB-09: modificarFabricanteBarril traduce la violación del índice único udx_fabricante_barril_nombre al mensaje de duplicado")
    void modificarFabricanteBarril_debeTraducirLaViolacionDelIndiceUnico() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Norte");
        FabricanteBarrilEntity existente = crearFabricanteBarrilEntity(1L, "Tonelería del Sur");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(fabricanteBarrilRepository.existsByNombreIgnoreCaseAndIdNot("Tonelería del Norte", 1L)).thenReturn(false);
        when(fabricanteBarrilRepository.save(existente))
                .thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_fabricante_barril_nombre"));

        assertThatThrownBy(() -> fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Tonelería del Norte' ya está en uso por otro fabricante de barril");
    }

    @Test
    @DisplayName("CP-MFB-10: modificarFabricanteBarril permite reutilizar el nombre de un fabricante dado de baja")
    void modificarFabricanteBarril_debePermitirReutilizarElNombreDeUnFabricanteDadoDeBaja() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Norte");
        FabricanteBarrilEntity existente = crearFabricanteBarrilEntity(1L, "Tonelería del Sur");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(existente));
        // El único otro fabricante con ese nombre está dado de baja: la consulta solo considera fabricantes activos
        when(fabricanteBarrilRepository.existsByNombreIgnoreCaseAndIdNot("Tonelería del Norte", 1L)).thenReturn(false);
        when(fabricanteBarrilRepository.save(existente)).thenReturn(existente);

        assertThatCode(() -> fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO)).doesNotThrowAnyException();

        assertThat(existente.getNombre()).isEqualTo("Tonelería del Norte");
        verify(fabricanteBarrilRepository).save(existente);
    }

    @Test
    @DisplayName("CP-MFB-11: modificarFabricanteBarril lanza ReglaNegocioException y no consulta el repositorio cuando el nombre supera los 100 caracteres")
    void modificarFabricanteBarril_debeRechazarNombreQueSuperaElTope() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("a".repeat(101));

        assertThatThrownBy(() -> fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre no puede superar 100 caracteres");

        verifyNoInteractions(fabricanteBarrilRepository);
    }

    // ==================== bajaFabricanteBarril ====================

    @Test
    @DisplayName("CP-BFB-01: bajaFabricanteBarril lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaFabricanteBarril_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(fabricanteBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fabricanteBarrilServicio.bajaFabricanteBarril(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fabricante de barril con ID: 99");

        verify(fabricanteBarrilRepository).findById(99L);
        verify(barrilRepository, never()).existsByFabricanteId(any());
        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BFB-02: bajaFabricanteBarril lanza ReglaNegocioException y no persiste cuando tiene barriles activos asociados")
    void bajaFabricanteBarril_debeRechazarBarrilesActivosAsociados() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByFabricanteId(1L)).thenReturn(true);

        assertThatThrownBy(() -> fabricanteBarrilServicio.bajaFabricanteBarril(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el fabricante de barril porque tiene barriles activos asociados");

        verify(barrilRepository).existsByFabricanteId(1L);
        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BFB-03: bajaFabricanteBarril marca el estado como BAJA, persiste y retorna el DTO cuando no tiene barriles activos asociados")
    void bajaFabricanteBarril_debeMarcarBajaYRetornarFabricanteExistente() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByFabricanteId(1L)).thenReturn(false);
        when(fabricanteBarrilRepository.save(fabricanteEntity)).thenReturn(fabricanteEntity);

        FabricanteBarrilResponseDTO resultado = fabricanteBarrilServicio.bajaFabricanteBarril(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(fabricanteEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertFabricanteBarrilDTO(fabricanteEntity, resultado);
        verify(fabricanteBarrilRepository).findById(1L);
        verify(barrilRepository).existsByFabricanteId(1L);
        verify(fabricanteBarrilRepository).save(fabricanteEntity);
        verify(fabricanteBarrilRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static FabricanteBarrilEntity crearFabricanteBarrilEntity(Long id, String nombre) {
        return FabricanteBarrilEntity.builder()
                .id(id)
                .nombre(nombre)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static FabricanteBarrilFormDTO fabricanteBarrilFormDTO(String nombre) {
        return FabricanteBarrilFormDTO.builder()
                .nombre(nombre)
                .build();
    }

    private static void assertFabricanteBarrilDTO(FabricanteBarrilEntity entidad, FabricanteBarrilResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
