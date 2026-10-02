package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResponseDTO;
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
class EquipamientoServicioImplTest {

    @Mock
    private IEquipamientoRepository equipamientoRepository;

    @InjectMocks
    private EquipamientoServicioImpl equipamientoServicio;

    // ==================== filtrarEquipamientos ====================

    @Test
    @DisplayName("CP-FE-01: filtrarEquipamientos retorna una página de equipamientos correctamente mapeada a DTO cuando se filtra por los 3 criterios, traduciendo el tipo a su clase concreta")
    void filtrarEquipamientos_debeRetornarPaginaMapeadaFiltrandoPorLosTresCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EquipamientoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE);
        EquipamientoEntity otroMolinoEntity = crearMolinoEntity(2L, "MOL-02", EstadoOperativo.DISPONIBLE);
        when(equipamientoRepository.filtrarEquipamientos("MOL", MolinoEntity.class, EstadoOperativo.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(molinoEntity, otroMolinoEntity), pageable, 2));

        // === EJECUCION ===
        Page<EquipamientoResponseDTO> resultado = equipamientoServicio.filtrarEquipamientos("MOL", TipoEquipamiento.MOLINO, EstadoOperativo.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertEquipamientoDTO(molinoEntity, resultado.getContent().get(0));
        assertEquipamientoDTO(otroMolinoEntity, resultado.getContent().get(1));
        // Verifica que el service traduzca el enum a la Class concreta antes de pasarlo al repositorio
        verify(equipamientoRepository).filtrarEquipamientos("MOL", MolinoEntity.class, EstadoOperativo.DISPONIBLE, pageable);
    }

    @Test
    @DisplayName("CP-FE-02: filtrarEquipamientos propaga tipoClase nulo al repositorio cuando el tipo informado es nulo")
    void filtrarEquipamientos_debePropagarTipoClaseNuloCuandoTipoEsNulo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EquipamientoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE);
        when(equipamientoRepository.filtrarEquipamientos("MOL", null, EstadoOperativo.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(molinoEntity), pageable, 1));

        // === EJECUCION ===
        Page<EquipamientoResponseDTO> resultado = equipamientoServicio.filtrarEquipamientos("MOL", null, EstadoOperativo.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(equipamientoRepository).filtrarEquipamientos("MOL", null, EstadoOperativo.DISPONIBLE, pageable);
    }

    @Test
    @DisplayName("CP-FE-03: filtrarEquipamientos propaga identificadorInterno y estadoOperativo nulos sin restringir esos criterios")
    void filtrarEquipamientos_debePropagarIdentificadorYEstadoNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EquipamientoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", EstadoOperativo.DISPONIBLE);
        EquipamientoEntity otroMolinoEntity = crearMolinoEntity(2L, "MOL-02", EstadoOperativo.EN_USO);
        when(equipamientoRepository.filtrarEquipamientos(null, MolinoEntity.class, null, pageable))
                .thenReturn(new PageImpl<>(List.of(molinoEntity, otroMolinoEntity), pageable, 2));

        // === EJECUCION ===
        Page<EquipamientoResponseDTO> resultado = equipamientoServicio.filtrarEquipamientos(null, TipoEquipamiento.MOLINO, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(equipamientoRepository).filtrarEquipamientos(null, MolinoEntity.class, null, pageable);
    }

    @Test
    @DisplayName("CP-FE-04: filtrarEquipamientos retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarEquipamientos_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(equipamientoRepository.filtrarEquipamientos("Inexistente", null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<EquipamientoResponseDTO> resultado = equipamientoServicio.filtrarEquipamientos("Inexistente", null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(equipamientoRepository).filtrarEquipamientos("Inexistente", null, null, pageable);
    }

    // ==================== helpers ====================

    private static MolinoEntity crearMolinoEntity(Long id, String identificadorInterno, EstadoOperativo estadoOperativo) {
        return MolinoEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .descripcion("Molino de prueba")
                .estadoOperativo(estadoOperativo)
                .rendimientoMolienda(100.0)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static void assertEquipamientoDTO(EquipamientoEntity entidad, EquipamientoResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificadorInterno()).isEqualTo(entidad.getIdentificadorInterno());
        assertThat(dto.getDescripcion()).isEqualTo(entidad.getDescripcion());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
