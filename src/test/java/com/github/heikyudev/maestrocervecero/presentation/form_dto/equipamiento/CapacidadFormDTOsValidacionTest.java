package com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento;

import com.github.heikyudev.maestrocervecero.presentation.form_dto.GrupoAlta;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BarrilFormDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La capacidad total (o la capacidad del barril) se exige solo en el alta, con el grupo {@link GrupoAlta}: en la modificación
 * no se envía porque no se puede cambiar.
 */
class CapacidadFormDTOsValidacionTest {

    private static ValidatorFactory fabrica;
    private static Validator validator;

    @BeforeAll
    static void crearValidator() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validator = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarFabrica() {
        fabrica.close();
    }

    @Test
    @DisplayName("CP-FCA-01: FermentadorFormDTO exige la capacidad total solo en el alta")
    void fermentadorFormDTO_debeExigirLaCapacidadTotalSoloEnElAlta() {
        // Arrange
        FermentadorFormDTO sinTotal = FermentadorFormDTO.builder().capacidadUtil(80.0).usosMaximosAntesMantenimiento(50).build();
        FermentadorFormDTO conTotal = FermentadorFormDTO.builder().capacidadTotal(100.0).capacidadUtil(80.0).usosMaximosAntesMantenimiento(50).build();

        // Act + Assert
        assertThat(validator.validate(sinTotal)).isEmpty();
        assertThat(mensajes(validator.validate(sinTotal, GrupoAlta.class))).containsExactly("La capacidad total es obligatoria");
        assertThat(validator.validate(conTotal, GrupoAlta.class)).isEmpty();
    }

    @Test
    @DisplayName("CP-FCA-02: MaceradorFormDTO exige la capacidad total solo en el alta")
    void maceradorFormDTO_debeExigirLaCapacidadTotalSoloEnElAlta() {
        // Arrange
        MaceradorFormDTO sinTotal = MaceradorFormDTO.builder().capacidadUtil(80.0).espacioMuerto(2.0).eficienciaMaceracion(75.0).usosMaximosAntesMantenimiento(50).build();
        MaceradorFormDTO conTotal = MaceradorFormDTO.builder().capacidadTotal(100.0).capacidadUtil(80.0).espacioMuerto(2.0).eficienciaMaceracion(75.0).usosMaximosAntesMantenimiento(50).build();

        // Act + Assert
        assertThat(validator.validate(sinTotal)).isEmpty();
        assertThat(mensajes(validator.validate(sinTotal, GrupoAlta.class))).containsExactly("La capacidad total es obligatoria");
        assertThat(validator.validate(conTotal, GrupoAlta.class)).isEmpty();
    }

    @Test
    @DisplayName("CP-FCA-03: OllaHervorFormDTO exige la capacidad total solo en el alta")
    void ollaHervorFormDTO_debeExigirLaCapacidadTotalSoloEnElAlta() {
        // Arrange
        OllaHervorFormDTO sinTotal = OllaHervorFormDTO.builder().capacidadUtil(80.0).evaporacion(8.0).perdidaPorTrub(3.0).usosMaximosAntesMantenimiento(50).build();
        OllaHervorFormDTO conTotal = OllaHervorFormDTO.builder().capacidadTotal(100.0).capacidadUtil(80.0).evaporacion(8.0).perdidaPorTrub(3.0).usosMaximosAntesMantenimiento(50).build();

        // Act + Assert
        assertThat(validator.validate(sinTotal)).isEmpty();
        assertThat(mensajes(validator.validate(sinTotal, GrupoAlta.class))).containsExactly("La capacidad total es obligatoria");
        assertThat(validator.validate(conTotal, GrupoAlta.class)).isEmpty();
    }

    @Test
    @DisplayName("CP-FCA-04: BarrilFormDTO exige la capacidad solo en el alta y el resto de los campos siempre")
    void barrilFormDTO_debeExigirLaCapacidadSoloEnElAlta() {
        // Arrange
        BarrilFormDTO sinCapacidad = BarrilFormDTO.builder().numeroSerie("SN-1").usosMaximosAntesMantenimiento(100).idFabricanteBarril(1L).build();
        BarrilFormDTO vacio = new BarrilFormDTO();

        // Act + Assert
        assertThat(validator.validate(sinCapacidad)).isEmpty();
        assertThat(mensajes(validator.validate(sinCapacidad, GrupoAlta.class))).containsExactly("La capacidad es obligatoria");
        assertThat(mensajes(validator.validate(vacio))).containsExactlyInAnyOrder(
                "El número de serie es obligatorio",
                "Los usos máximos antes de mantenimiento son obligatorios",
                "El fabricante de barril es obligatorio");
    }

    private static List<String> mensajes(Set<? extends ConstraintViolation<?>> violaciones) {
        return violaciones.stream().map(ConstraintViolation::getMessage).toList();
    }
}
