package com.github.heikyudev.maestrocervecero.presentation.form_dto.lote;

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

class FormDTOsLoteValidacionTest {

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

    // ==================== LoteFormDTO ====================

    @Test
    @DisplayName("CP-FDL-01: LoteFormDTO sin datos informa los 6 campos obligatorios con los mismos textos que el Service")
    void loteFormDTO_debeInformarLosCamposObligatorios() {
        assertThat(mensajes(new LoteFormDTO())).containsExactlyInAnyOrder(
                "El volumen objetivo del lote es obligatorio",
                "La planificación de producción es obligatoria",
                "El fermentador es obligatorio",
                "El molino es obligatorio",
                "El macerador es obligatorio",
                "La olla de hervor es obligatoria");
    }

    @Test
    @DisplayName("CP-FDL-02: LoteFormDTO con todos los datos informados, cargados por sus setters, no tiene violaciones")
    void loteFormDTO_conTodosLosDatosNoDebeTenerViolaciones() {
        // === PREPARACION DE DATOS ===
        LoteFormDTO formDTO = new LoteFormDTO();
        formDTO.setVolumenObjetivo(20.0);
        formDTO.setIdPlanificacionProduccion(1L);
        formDTO.setIdFermentador(4L);
        formDTO.setIdMolino(1L);
        formDTO.setIdMacerador(2L);
        formDTO.setIdOllaHervor(3L);

        // === EJECUCION Y ASSERTS ===
        assertThat(validator.validate(formDTO)).isEmpty();
    }

    // ==================== ConsumoInsumoFormDTO / RegistrarEnvasadoLoteFormDTO ====================

    @Test
    @DisplayName("CP-FDL-03: ConsumoInsumoFormDTO sin datos informa el lote de insumo y la cantidad consumida como obligatorios")
    void consumoInsumoFormDTO_debeInformarLosCamposObligatorios() {
        assertThat(mensajes(new ConsumoInsumoFormDTO())).containsExactlyInAnyOrder(
                "El lote de insumo es obligatorio",
                "La cantidad consumida es obligatoria");
        assertThat(validator.validate(ConsumoInsumoFormDTO.builder().idLoteInsumo(1L).cantidadConsumida(2.5).build())).isEmpty();
    }

    @Test
    @DisplayName("CP-FDL-04: RegistrarEnvasadoLoteFormDTO sin datos informa el barril y la cantidad a envasar como obligatorios")
    void registrarEnvasadoLoteFormDTO_debeInformarLosCamposObligatorios() {
        assertThat(mensajes(new RegistrarEnvasadoLoteFormDTO())).containsExactlyInAnyOrder(
                "El barril es obligatorio",
                "La cantidad a envasar es obligatoria");
        assertThat(validator.validate(RegistrarEnvasadoLoteFormDTO.builder().idBarril(1L).cantidad(30.0).build())).isEmpty();
    }

    // ==================== motivos ====================

    @Test
    @DisplayName("CP-FDL-05: CancelacionLoteFormDTO exige el motivo de cancelación y limita su longitud a 255 caracteres")
    void cancelacionLoteFormDTO_debeValidarElMotivo() {
        assertThat(mensajes(CancelacionLoteFormDTO.builder().motivoCancelacion(null).build()))
                .containsExactly("El motivo de cancelación es obligatorio");
        assertThat(mensajes(CancelacionLoteFormDTO.builder().motivoCancelacion("   ").build()))
                .containsExactly("El motivo de cancelación es obligatorio");
        assertThat(mensajes(CancelacionLoteFormDTO.builder().motivoCancelacion("a".repeat(256)).build()))
                .containsExactly("El motivo de cancelación no puede superar 255 caracteres");
        assertThat(validator.validate(CancelacionLoteFormDTO.builder().motivoCancelacion("a".repeat(255)).build())).isEmpty();
    }

    @Test
    @DisplayName("CP-FDL-06: AnularConsumoInsumoFormDTO y AnularEnvasadoLoteFormDTO exigen el motivo de anulación y limitan su longitud a 255 caracteres")
    void formDTOsDeAnulacion_debenValidarElMotivo() {
        assertThat(mensajes(AnularConsumoInsumoFormDTO.builder().motivoAnulacion(null).build()))
                .containsExactly("El motivo de anulación es obligatorio");
        assertThat(mensajes(AnularConsumoInsumoFormDTO.builder().motivoAnulacion("a".repeat(256)).build()))
                .containsExactly("El motivo de anulación no puede superar 255 caracteres");
        assertThat(validator.validate(AnularConsumoInsumoFormDTO.builder().motivoAnulacion("Error de tipeo").build())).isEmpty();

        assertThat(mensajes(AnularEnvasadoLoteFormDTO.builder().motivoAnulacion("   ").build()))
                .containsExactly("El motivo de anulación es obligatorio");
        assertThat(mensajes(AnularEnvasadoLoteFormDTO.builder().motivoAnulacion("a".repeat(256)).build()))
                .containsExactly("El motivo de anulación no puede superar 255 caracteres");
        assertThat(validator.validate(AnularEnvasadoLoteFormDTO.builder().motivoAnulacion("Error de carga").build())).isEmpty();
    }

    // ==================== helpers ====================

    private static List<String> mensajes(Object formDTO) {
        Set<ConstraintViolation<Object>> violaciones = validator.validate(formDTO);
        return violaciones.stream().map(ConstraintViolation::getMessage).toList();
    }
}
