package com.github.heikyudev.maestrocervecero.util.mapper.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapperRecetaTest {

    @Test
    @DisplayName("CP-MR-01: toDTO calcula el ABV de la versión a partir de su OG y su FG objetivo")
    void toDTO_debeCalcularElAbvDeLaVersion() {
        // Arrange
        VersionRecetaEntity version = VersionRecetaEntity.builder().id(70L).nombre("Pale Ale").ogObjetivo(1.05).fgObjetivo(1.01).build();

        // Act
        VersionRecetaResponseDTO dto = MapperReceta.toDTO(version);

        // Assert
        assertThat(dto.getAbv()).isEqualTo(5.3);
    }

    @Test
    @DisplayName("CP-MR-02: toDTO deja el ABV en null si falta una densidad o la OG no supera a la FG")
    void toDTO_debeDejarElAbvEnNullSiNoSePuedeCalcular() {
        // Arrange
        VersionRecetaEntity sinFg = VersionRecetaEntity.builder().ogObjetivo(1.05).build();
        VersionRecetaEntity ogMenor = VersionRecetaEntity.builder().ogObjetivo(1.01).fgObjetivo(1.05).build();

        // Act y Assert
        assertThat(MapperReceta.toDTO(sinFg).getAbv()).isNull();
        assertThat(MapperReceta.toDTO(ogMenor).getAbv()).isNull();
    }
}
