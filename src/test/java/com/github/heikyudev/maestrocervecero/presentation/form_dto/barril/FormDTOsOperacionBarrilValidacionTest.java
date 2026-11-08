package com.github.heikyudev.maestrocervecero.presentation.form_dto.barril;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FormDTOsOperacionBarrilValidacionTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 1, 20, 9, 0);

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

    // ==================== limpieza, falla, mantenimiento y devolución ====================

    @Test
    @DisplayName("CP-FDB-01: LimpiezaBarrilFormDTO sin datos informa el barril, la fecha y las observaciones como obligatorios")
    void limpiezaBarrilFormDTO_debeInformarLosCamposObligatorios() {
        assertThat(mensajes(new LimpiezaBarrilFormDTO())).containsExactlyInAnyOrder(
                "El barril es obligatorio",
                "La fecha de limpieza es obligatoria",
                "Las observaciones son obligatorias");
        assertThat(validator.validate(LimpiezaBarrilFormDTO.builder().idBarril(1L).fecha(FECHA).observaciones("Limpieza CIP").build())).isEmpty();
    }

    @Test
    @DisplayName("CP-FDB-02: FallaBarrilFormDTO sin datos informa el barril, la fecha y las observaciones como obligatorios")
    void fallaBarrilFormDTO_debeInformarLosCamposObligatorios() {
        assertThat(mensajes(new FallaBarrilFormDTO())).containsExactlyInAnyOrder(
                "El barril es obligatorio",
                "La fecha de falla es obligatoria",
                "Las observaciones son obligatorias");
        assertThat(validator.validate(FallaBarrilFormDTO.builder().idBarril(1L).fecha(FECHA).observaciones("Pérdida en la válvula").build())).isEmpty();
    }

    @Test
    @DisplayName("CP-FDB-03: MantenimientoBarrilFormDTO sin datos informa el barril, la fecha y las observaciones como obligatorios")
    void mantenimientoBarrilFormDTO_debeInformarLosCamposObligatorios() {
        assertThat(mensajes(new MantenimientoBarrilFormDTO())).containsExactlyInAnyOrder(
                "El barril es obligatorio",
                "La fecha de mantenimiento es obligatoria",
                "Las observaciones son obligatorias");
        assertThat(validator.validate(MantenimientoBarrilFormDTO.builder().idBarril(1L).fecha(FECHA).observaciones("Cambio de válvula").build())).isEmpty();
    }

    @Test
    @DisplayName("CP-FDB-04: DevolucionBarrilFormDTO sin datos informa el barril, la fecha y las observaciones como obligatorios")
    void devolucionBarrilFormDTO_debeInformarLosCamposObligatorios() {
        assertThat(mensajes(new DevolucionBarrilFormDTO())).containsExactlyInAnyOrder(
                "El barril es obligatorio",
                "La fecha de devolución es obligatoria",
                "Las observaciones son obligatorias");
        assertThat(validator.validate(DevolucionBarrilFormDTO.builder().idBarril(1L).fecha(FECHA).observaciones("Devuelto vacío").build())).isEmpty();
    }

    // ==================== fraccionamiento y despacho ====================

    @Test
    @DisplayName("CP-FDB-05: FraccionamientoBarrilFormDTO sin datos informa el barril, la fecha, la cantidad a extraer y las observaciones como obligatorios")
    void fraccionamientoBarrilFormDTO_debeInformarLosCamposObligatorios() {
        assertThat(mensajes(new FraccionamientoBarrilFormDTO())).containsExactlyInAnyOrder(
                "El barril es obligatorio",
                "La fecha de fraccionamiento es obligatoria",
                "La cantidad a extraer es obligatoria",
                "Las observaciones son obligatorias");
        assertThat(validator.validate(FraccionamientoBarrilFormDTO.builder().idBarril(1L).fecha(FECHA).cantidadExtraida(5.5).observaciones("Merma").build())).isEmpty();
    }

    @Test
    @DisplayName("CP-FDB-06: FraccionamientoBarrilFormDTO rechaza una cantidad a extraer igual a cero o negativa")
    void fraccionamientoBarrilFormDTO_debeRechazarCantidadNoPositiva() {
        assertThat(mensajes(FraccionamientoBarrilFormDTO.builder().idBarril(1L).fecha(FECHA).cantidadExtraida(0.0).observaciones("Merma").build()))
                .containsExactly("La cantidad a extraer debe ser mayor a 0");
        assertThat(mensajes(FraccionamientoBarrilFormDTO.builder().idBarril(1L).fecha(FECHA).cantidadExtraida(-1.0).observaciones("Merma").build()))
                .containsExactly("La cantidad a extraer debe ser mayor a 0");
    }

    @Test
    @DisplayName("CP-FDB-07: DespachoBarrilFormDTO sin datos informa el barril, el cliente, la fecha, la fecha estimada de devolución y las observaciones como obligatorios")
    void despachoBarrilFormDTO_debeInformarLosCamposObligatorios() {
        assertThat(mensajes(new DespachoBarrilFormDTO())).containsExactlyInAnyOrder(
                "El barril es obligatorio",
                "El cliente es obligatorio",
                "La fecha de despacho es obligatoria",
                "La fecha estimada de devolución es obligatoria",
                "Las observaciones son obligatorias");
        assertThat(validator.validate(DespachoBarrilFormDTO.builder().idBarril(1L).idCliente(2L).fecha(FECHA)
                .fechaDevolucionEstimada(LocalDate.of(2026, 2, 20)).observaciones("Entrega en el local").build())).isEmpty();
    }

    // ==================== observaciones y motivos ====================

    @Test
    @DisplayName("CP-FDB-08: los FormDTO de registro exigen observaciones no vacías y limitan su longitud a 255 caracteres")
    void formDTOsDeRegistro_debenValidarLasObservaciones() {
        assertThat(mensajes(LimpiezaBarrilFormDTO.builder().idBarril(1L).fecha(FECHA).observaciones("   ").build()))
                .containsExactly("Las observaciones son obligatorias");
        assertThat(mensajes(FallaBarrilFormDTO.builder().idBarril(1L).fecha(FECHA).observaciones("a".repeat(256)).build()))
                .containsExactly("Las observaciones no pueden superar 255 caracteres");
        assertThat(mensajes(DespachoBarrilFormDTO.builder().idBarril(1L).idCliente(2L).fecha(FECHA)
                .fechaDevolucionEstimada(LocalDate.of(2026, 2, 20)).observaciones("a".repeat(256)).build()))
                .containsExactly("Las observaciones no pueden superar 255 caracteres");
        assertThat(validator.validate(MantenimientoBarrilFormDTO.builder().idBarril(1L).fecha(FECHA).observaciones("a".repeat(255)).build())).isEmpty();
    }

    @Test
    @DisplayName("CP-FDB-09: los FormDTO de anulación exigen el motivo de anulación y limitan su longitud a 255 caracteres")
    void formDTOsDeAnulacion_debenValidarElMotivo() {
        assertThat(mensajes(new AnulacionLimpiezaBarrilFormDTO())).containsExactly("El motivo de anulación es obligatorio");
        assertThat(mensajes(AnulacionFallaBarrilFormDTO.builder().motivoAnulacion("   ").build())).containsExactly("El motivo de anulación es obligatorio");
        assertThat(mensajes(AnulacionMantenimientoBarrilFormDTO.builder().motivoAnulacion("a".repeat(256)).build()))
                .containsExactly("El motivo de anulación no puede superar 255 caracteres");
        assertThat(mensajes(AnulacionFraccionamientoBarrilFormDTO.builder().motivoAnulacion("a".repeat(256)).build()))
                .containsExactly("El motivo de anulación no puede superar 255 caracteres");
        assertThat(mensajes(AnulacionDespachoBarrilFormDTO.builder().motivoAnulacion(null).build())).containsExactly("El motivo de anulación es obligatorio");
        assertThat(mensajes(AnulacionDevolucionBarrilFormDTO.builder().motivoAnulacion(null).build())).containsExactly("El motivo de anulación es obligatorio");
        assertThat(validator.validate(AnulacionLimpiezaBarrilFormDTO.builder().motivoAnulacion("Cargada por error").build())).isEmpty();
        assertThat(validator.validate(AnulacionDevolucionBarrilFormDTO.builder().motivoAnulacion("a".repeat(255)).build())).isEmpty();
    }

    @Test
    @DisplayName("CP-FDB-10: los FormDTO de operación exponen setters para el binding del formulario")
    void formDTOs_debenExponerSetters() {
        // === PREPARACION DE DATOS ===
        DespachoBarrilFormDTO formDTO = new DespachoBarrilFormDTO();
        formDTO.setIdBarril(1L);
        formDTO.setIdCliente(2L);
        formDTO.setFecha(FECHA);
        formDTO.setFechaDevolucionEstimada(LocalDate.of(2026, 2, 20));
        formDTO.setObservaciones("Entrega en el local");
        FraccionamientoBarrilFormDTO fraccionamiento = new FraccionamientoBarrilFormDTO();
        fraccionamiento.setIdBarril(1L);
        fraccionamiento.setFecha(FECHA);
        fraccionamiento.setCantidadExtraida(2.0);
        fraccionamiento.setObservaciones("Merma");
        AnulacionLimpiezaBarrilFormDTO anulacion = new AnulacionLimpiezaBarrilFormDTO();
        anulacion.setMotivoAnulacion("Cargada por error");

        // === EJECUCION Y ASSERTS ===
        assertThat(validator.validate(formDTO)).isEmpty();
        assertThat(validator.validate(fraccionamiento)).isEmpty();
        assertThat(validator.validate(anulacion)).isEmpty();
        assertThat(formDTO.getFecha()).isEqualTo(FECHA);
    }

    // ==================== helpers ====================

    private static List<String> mensajes(Object formDTO) {
        Set<ConstraintViolation<Object>> violaciones = validator.validate(formDTO);
        return violaciones.stream().map(ConstraintViolation::getMessage).toList();
    }
}
