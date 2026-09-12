package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IInsumoRepository;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
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
