package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsumoServicioImplTest {

    @Mock
    private IInsumoRepository insumoRepository;

    @InjectMocks
    private InsumoServicioImpl insumoServicio;

    // ==================== filtrarInsumos ====================

    @Test
    @DisplayName("CP-FI-01: filtrarInsumos retorna una página de insumos correctamente mapeada a DTO cuando se filtra por nombre y tipo, traduciendo el tipo a su clase concreta")
    void filtrarInsumos_debeRetornarPaginaMapeadaFiltrandoPorNombreYTipo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        InsumoEntity maltaEntity = crearMaltaEntity(1L, "Pilsen");
        InsumoEntity otraMaltaEntity = crearMaltaEntity(2L, "Pilsen Nacional");
        when(insumoRepository.filtrarInsumos("Pilsen", MaltaEntity.class, pageable))
                .thenReturn(new PageImpl<>(List.of(maltaEntity, otraMaltaEntity), pageable, 2));

        // === EJECUCION ===
        Page<InsumoResponseDTO> resultado = insumoServicio.filtrarInsumos("Pilsen", TipoInsumo.MALTA, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertInsumoDTO(maltaEntity, resultado.getContent().get(0));
        assertInsumoDTO(otraMaltaEntity, resultado.getContent().get(1));
        // Verifica que el service traduzca el enum a la Class concreta antes de pasarlo al repositorio
        verify(insumoRepository).filtrarInsumos("Pilsen", MaltaEntity.class, pageable);
    }

    @Test
    @DisplayName("CP-FI-02: filtrarInsumos propaga tipoClase nulo al repositorio cuando el tipo informado es nulo")
    void filtrarInsumos_debePropagarTipoClaseNuloCuandoTipoEsNulo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        InsumoEntity maltaEntity = crearMaltaEntity(1L, "Pilsen");
        when(insumoRepository.filtrarInsumos("Pilsen", null, pageable))
                .thenReturn(new PageImpl<>(List.of(maltaEntity), pageable, 1));

        // === EJECUCION ===
        Page<InsumoResponseDTO> resultado = insumoServicio.filtrarInsumos("Pilsen", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(insumoRepository).filtrarInsumos("Pilsen", null, pageable);
    }

    @Test
    @DisplayName("CP-FI-03: filtrarInsumos propaga nombre nulo sin restringir ese criterio")
    void filtrarInsumos_debePropagarNombreNulo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        InsumoEntity maltaEntity = crearMaltaEntity(1L, "Pilsen");
        InsumoEntity otraMaltaEntity = crearMaltaEntity(2L, "Caramelo 60");
        when(insumoRepository.filtrarInsumos(null, MaltaEntity.class, pageable))
                .thenReturn(new PageImpl<>(List.of(maltaEntity, otraMaltaEntity), pageable, 2));

        // === EJECUCION ===
        Page<InsumoResponseDTO> resultado = insumoServicio.filtrarInsumos(null, TipoInsumo.MALTA, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(insumoRepository).filtrarInsumos(null, MaltaEntity.class, pageable);
    }

    @Test
    @DisplayName("CP-FI-04: filtrarInsumos retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarInsumos_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(insumoRepository.filtrarInsumos("Inexistente", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<InsumoResponseDTO> resultado = insumoServicio.filtrarInsumos("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(insumoRepository).filtrarInsumos("Inexistente", null, pageable);
    }

    @Test
    @DisplayName("CP-FI-05: filtrarInsumos normaliza el nombre buscado (espacios extremos e internos) antes de consultar")
    void filtrarInsumos_debeNormalizarElNombreBuscado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(insumoRepository.filtrarInsumos("Pilsen Nacional", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        insumoServicio.filtrarInsumos("   Pilsen   Nacional  ", null, pageable);

        // === ASSERTS ===
        verify(insumoRepository).filtrarInsumos("Pilsen Nacional", null, pageable);
    }

    @Test
    @DisplayName("CP-FI-06: filtrarInsumos trata un nombre formado solo por espacios como ausencia de filtro")
    void filtrarInsumos_debeTratarNombreEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(insumoRepository.filtrarInsumos(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        insumoServicio.filtrarInsumos("    ", null, pageable);

        // === ASSERTS ===
        verify(insumoRepository).filtrarInsumos(null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-FI-07: buscarPorId retorna el insumo activo en su DTO concreto, cualquiera sea su tipo")
    void buscarPorId_debeRetornarElInsumoEnSuDTOConcreto() {
        // === PREPARACION DE DATOS ===
        InsumoEntity maltaEntity = crearMaltaEntity(1L, "Pilsen");
        when(insumoRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));

        // === EJECUCION ===
        InsumoResponseDTO resultado = insumoServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado).isInstanceOf(MaltaResponseDTO.class);
        assertInsumoDTO(maltaEntity, resultado);
    }

    @Test
    @DisplayName("CP-FI-08: buscarPorId lanza RecursoNoEncontradoException cuando no existe un insumo activo con ese ID")
    void buscarPorId_debeLanzarExcepcionCuandoNoExiste() {
        // === PREPARACION DE DATOS ===
        when(insumoRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> insumoServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el insumo con ID: 99");
    }

    // ==================== helpers ====================

    private static MaltaEntity crearMaltaEntity(Long id, String nombre) {
        return MaltaEntity.builder()
                .id(id)
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .tipo(TipoMalta.BASE)
                .potencialExtracto(80)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static void assertInsumoDTO(InsumoEntity entidad, InsumoResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getUnidadDeMedida()).isEqualTo(entidad.getUnidadDeMedida());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
