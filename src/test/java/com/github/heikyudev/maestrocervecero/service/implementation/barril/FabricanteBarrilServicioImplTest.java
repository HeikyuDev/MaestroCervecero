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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FabricanteBarrilServicioImplTest {

    private static final String NOMBRE_COMERCIAL_DEFAULT = "Fabricante de prueba";
    private static final String TELEFONO_DEFAULT = "011-4444-5555";
    private static final String EMAIL_DEFAULT = "contacto@fabricante.com";

    @Mock
    private IFabricanteBarrilRepository fabricanteBarrilRepository;

    @Mock
    private IBarrilRepository barrilRepository;

    @InjectMocks
    private FabricanteBarrilServicioImpl fabricanteBarrilServicio;

    // ==================== filtrarFabricantesBarril ====================

    @Test
    @DisplayName("CP-FFB-01: filtrarFabricantesBarril retorna una página de fabricantes correctamente mapeada a DTO cuando se filtra por razón social, nombre comercial y CUIT")
    void filtrarFabricantesBarril_debeRetornarPaginaMapeadaFiltrandoPorRazonSocialNombreComercialYCuit() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur S.A.", "30-11111111-1");
        FabricanteBarrilEntity otroFabricanteEntity = crearFabricanteBarrilEntity(2L, "Tonelería del Sur Hnos.", "30-22222222-2");
        when(fabricanteBarrilRepository.filtrarFabricantesBarril("Tonelería", "Sur", "30-1111", pageable))
                .thenReturn(new PageImpl<>(List.of(fabricanteEntity, otroFabricanteEntity), pageable, 2));

        // === EJECUCION ===
        Page<FabricanteBarrilResponseDTO> resultado = fabricanteBarrilServicio.filtrarFabricantesBarril("Tonelería", "Sur", "30-1111", pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertFabricanteBarrilDTO(fabricanteEntity, resultado.getContent().get(0));
        assertFabricanteBarrilDTO(otroFabricanteEntity, resultado.getContent().get(1));
        verify(fabricanteBarrilRepository).filtrarFabricantesBarril("Tonelería", "Sur", "30-1111", pageable);
    }

    @Test
    @DisplayName("CP-FFB-02: filtrarFabricantesBarril propaga razón social, nombre comercial y CUIT nulos sin restringir esos criterios")
    void filtrarFabricantesBarril_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur S.A.", "30-11111111-1");
        FabricanteBarrilEntity otroFabricanteEntity = crearFabricanteBarrilEntity(2L, "Tonelería del Norte S.A.", "30-22222222-2");
        when(fabricanteBarrilRepository.filtrarFabricantesBarril(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(fabricanteEntity, otroFabricanteEntity), pageable, 2));

        // === EJECUCION ===
        Page<FabricanteBarrilResponseDTO> resultado = fabricanteBarrilServicio.filtrarFabricantesBarril(null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(fabricanteBarrilRepository).filtrarFabricantesBarril(null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FFB-03: filtrarFabricantesBarril retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarFabricantesBarril_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(fabricanteBarrilRepository.filtrarFabricantesBarril("Inexistente", null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<FabricanteBarrilResponseDTO> resultado = fabricanteBarrilServicio.filtrarFabricantesBarril("Inexistente", null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(fabricanteBarrilRepository).filtrarFabricantesBarril("Inexistente", null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del fabricante de barril cuando el ID existe")
    void buscarPorId_debeRetornarFabricanteExistente() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur S.A.", "30-11111111-1");
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
    @DisplayName("CP-AFB-01: altaFabricanteBarril lanza RecursoDuplicadoException y no consulta el CUIT cuando la razón social ya está registrada por un fabricante activo")
    void altaFabricanteBarril_debeRechazarRazonSocialDuplicada() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Sur S.A.", "30-99999999-9");
        when(fabricanteBarrilRepository.existsByRazonSocialIgnoreCase("Tonelería del Sur S.A.")).thenReturn(true);

        assertThatThrownBy(() -> fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un fabricante de barril activo con la razón social 'Tonelería del Sur S.A.'");

        verify(fabricanteBarrilRepository, never()).existsByCuit(any());
        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-02: altaFabricanteBarril lanza RecursoDuplicadoException cuando el CUIT ya está registrado por un fabricante activo")
    void altaFabricanteBarril_debeRechazarCuitDuplicado() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Norte S.A.", "30-11111111-1");
        when(fabricanteBarrilRepository.existsByRazonSocialIgnoreCase("Tonelería del Norte S.A.")).thenReturn(false);
        when(fabricanteBarrilRepository.existsByCuit("30-11111111-1")).thenReturn(true);

        assertThatThrownBy(() -> fabricanteBarrilServicio.altaFabricanteBarril(fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un fabricante de barril activo con el CUIT '30-11111111-1'");

        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AFB-03: altaFabricanteBarril persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaFabricanteBarril_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Sur S.A.", "30-11111111-1");
        when(fabricanteBarrilRepository.existsByRazonSocialIgnoreCase("Tonelería del Sur S.A.")).thenReturn(false);
        when(fabricanteBarrilRepository.existsByCuit("30-11111111-1")).thenReturn(false);
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
        assertThat(entidadCapturada.getRazonSocial()).isEqualTo("Tonelería del Sur S.A.");
        assertThat(entidadCapturada.getNombreComercial()).isEqualTo(NOMBRE_COMERCIAL_DEFAULT);
        assertThat(entidadCapturada.getCuit()).isEqualTo("30-11111111-1");
        assertThat(entidadCapturada.getTelefono()).isEqualTo(TELEFONO_DEFAULT);
        assertThat(entidadCapturada.getEmail()).isEqualTo(EMAIL_DEFAULT);
        // El alta siempre debe registrar al fabricante como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getRazonSocial()).isEqualTo("Tonelería del Sur S.A.");
        assertThat(resultado.getCuit()).isEqualTo("30-11111111-1");
        assertThat(resultado.getEstado()).isEqualTo(Estado.ACTIVO);
    }

    // ==================== modificarFabricanteBarril ====================

    @Test
    @DisplayName("CP-MFB-01: modificarFabricanteBarril lanza RecursoNoEncontradoException y no valida duplicados ni persiste cuando el ID no existe")
    void modificarFabricanteBarril_debeLanzarExcepcionSiNoExiste() {
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Sur S.A.", "30-11111111-1");
        when(fabricanteBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fabricanteBarrilServicio.modificarFabricanteBarril(99L, fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fabricante de barril con ID: 99");

        verify(fabricanteBarrilRepository).findById(99L);
        verify(fabricanteBarrilRepository, never()).existsByRazonSocialIgnoreCaseAndIdNot(any(), any());
        verify(fabricanteBarrilRepository, never()).existsByCuitAndIdNot(any(), any());
        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MFB-02: modificarFabricanteBarril lanza RecursoDuplicadoException y no persiste cuando la razón social está en uso por otro fabricante activo")
    void modificarFabricanteBarril_debeRechazarRazonSocialEnUsoPorOtroFabricante() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur S.A.", "30-11111111-1");
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Norte S.A.", "30-11111111-1");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(fabricanteBarrilRepository.existsByRazonSocialIgnoreCaseAndIdNot("Tonelería del Norte S.A.", 1L)).thenReturn(true);

        assertThatThrownBy(() -> fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe otro fabricante de barril activo con la razón social 'Tonelería del Norte S.A.'");

        verify(fabricanteBarrilRepository, never()).existsByCuitAndIdNot(any(), any());
        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MFB-03: modificarFabricanteBarril lanza RecursoDuplicadoException y no persiste cuando el CUIT está en uso por otro fabricante activo")
    void modificarFabricanteBarril_debeRechazarCuitEnUsoPorOtroFabricante() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur S.A.", "30-11111111-1");
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Sur S.A.", "30-99999999-9");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(fabricanteBarrilRepository.existsByRazonSocialIgnoreCaseAndIdNot("Tonelería del Sur S.A.", 1L)).thenReturn(false);
        when(fabricanteBarrilRepository.existsByCuitAndIdNot("30-99999999-9", 1L)).thenReturn(true);

        assertThatThrownBy(() -> fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe otro fabricante de barril activo con el CUIT '30-99999999-9'");

        verify(fabricanteBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MFB-04: modificarFabricanteBarril permite conservar la razón social y el CUIT propios y persiste (camino feliz)")
    void modificarFabricanteBarril_debePermitirConservarRazonSocialYCuitPropios() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur S.A.", "30-11111111-1");
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = fabricanteBarrilFormDTO("Tonelería del Sur S.A.", "30-11111111-1");
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(fabricanteBarrilRepository.existsByRazonSocialIgnoreCaseAndIdNot("Tonelería del Sur S.A.", 1L)).thenReturn(false);
        when(fabricanteBarrilRepository.existsByCuitAndIdNot("30-11111111-1", 1L)).thenReturn(false);
        when(fabricanteBarrilRepository.save(fabricanteEntity)).thenReturn(fabricanteEntity);

        FabricanteBarrilResponseDTO resultado = fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO);

        assertThat(resultado.getRazonSocial()).isEqualTo("Tonelería del Sur S.A.");
        assertThat(resultado.getCuit()).isEqualTo("30-11111111-1");
        verify(fabricanteBarrilRepository).save(fabricanteEntity);
    }

    @Test
    @DisplayName("CP-MFB-05: modificarFabricanteBarril actualiza todos los campos y persiste cuando los nuevos datos son válidos y únicos (camino feliz)")
    void modificarFabricanteBarril_debeActualizarFabricanteExistente() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería Vieja S.A.", "30-11111111-1");
        FabricanteBarrilFormDTO fabricanteBarrilFormDTO = FabricanteBarrilFormDTO.builder()
                .razonSocial("Tonelería Nueva S.A.")
                .nombreComercial("Tonelería Nueva")
                .cuit("30-22222222-2")
                .telefono("011-5555-6666")
                .email("nuevo@fabricante.com")
                .build();
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(fabricanteBarrilRepository.existsByRazonSocialIgnoreCaseAndIdNot("Tonelería Nueva S.A.", 1L)).thenReturn(false);
        when(fabricanteBarrilRepository.existsByCuitAndIdNot("30-22222222-2", 1L)).thenReturn(false);
        when(fabricanteBarrilRepository.save(fabricanteEntity)).thenReturn(fabricanteEntity);

        // === EJECUCION ===
        FabricanteBarrilResponseDTO resultado = fabricanteBarrilServicio.modificarFabricanteBarril(1L, fabricanteBarrilFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<FabricanteBarrilEntity> captor = ArgumentCaptor.forClass(FabricanteBarrilEntity.class);
        verify(fabricanteBarrilRepository).save(captor.capture());
        FabricanteBarrilEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getRazonSocial()).isEqualTo("Tonelería Nueva S.A.");
        assertThat(entidadCapturada.getNombreComercial()).isEqualTo("Tonelería Nueva");
        assertThat(entidadCapturada.getCuit()).isEqualTo("30-22222222-2");
        assertThat(entidadCapturada.getTelefono()).isEqualTo("011-5555-6666");
        assertThat(entidadCapturada.getEmail()).isEqualTo("nuevo@fabricante.com");

        assertThat(resultado.getRazonSocial()).isEqualTo("Tonelería Nueva S.A.");
        assertThat(resultado.getCuit()).isEqualTo("30-22222222-2");
        verify(fabricanteBarrilRepository).findById(1L);
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
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur S.A.", "30-11111111-1");
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
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L, "Tonelería del Sur S.A.", "30-11111111-1");
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

    private static FabricanteBarrilEntity crearFabricanteBarrilEntity(Long id, String razonSocial, String cuit) {
        return FabricanteBarrilEntity.builder()
                .id(id)
                .razonSocial(razonSocial)
                .nombreComercial(NOMBRE_COMERCIAL_DEFAULT)
                .cuit(cuit)
                .telefono(TELEFONO_DEFAULT)
                .email(EMAIL_DEFAULT)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static FabricanteBarrilFormDTO fabricanteBarrilFormDTO(String razonSocial, String cuit) {
        return FabricanteBarrilFormDTO.builder()
                .razonSocial(razonSocial)
                .nombreComercial(NOMBRE_COMERCIAL_DEFAULT)
                .cuit(cuit)
                .telefono(TELEFONO_DEFAULT)
                .email(EMAIL_DEFAULT)
                .build();
    }

    private static void assertFabricanteBarrilDTO(FabricanteBarrilEntity entidad, FabricanteBarrilResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getRazonSocial()).isEqualTo(entidad.getRazonSocial());
        assertThat(dto.getNombreComercial()).isEqualTo(entidad.getNombreComercial());
        assertThat(dto.getCuit()).isEqualTo(entidad.getCuit());
        assertThat(dto.getTelefono()).isEqualTo(entidad.getTelefono());
        assertThat(dto.getEmail()).isEqualTo(entidad.getEmail());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
