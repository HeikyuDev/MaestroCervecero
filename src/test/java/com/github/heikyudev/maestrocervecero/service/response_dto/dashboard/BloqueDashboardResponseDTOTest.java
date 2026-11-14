package com.github.heikyudev.maestrocervecero.service.response_dto.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueDashboardResponseDTOTest {

    @Test
    @DisplayName("CP-BDD-01: el bloque toma el total real de la página y mapea sus filas en el mismo orden")
    void de_tomaElTotalRealYMapeaLasFilas() {
        // Arrange
        PageImpl<Integer> pagina = new PageImpl<>(List.of(1, 2, 3), PageRequest.of(0, 3), 12);

        // Act
        BloqueDashboardResponseDTO<String> bloque = BloqueDashboardResponseDTO.de(pagina, numero -> "N" + numero);

        // Assert
        assertThat(bloque.getTotal()).isEqualTo(12);
        assertThat(bloque.getFilas()).containsExactly("N1", "N2", "N3");
    }

    @Test
    @DisplayName("CP-BDD-02: una página vacía da un bloque con total 0 y una lista de filas vacía, no nula")
    void de_paginaVaciaDaBloqueVacio() {
        // Arrange
        PageImpl<Integer> pagina = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);

        // Act
        BloqueDashboardResponseDTO<String> bloque = BloqueDashboardResponseDTO.de(pagina, String::valueOf);

        // Assert
        assertThat(bloque.getTotal()).isZero();
        assertThat(bloque.getFilas()).isNotNull().isEmpty();
    }
}
