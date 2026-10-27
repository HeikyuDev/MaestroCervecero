package com.github.heikyudev.maestrocervecero.util.validacion;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacionesNumericasTest {

    private static final String ETIQUETA = "La capacidad total";
    private static final Double[] NO_FINITOS = {null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};

    @Test
    @DisplayName("CP-VN-01: validarMayorACero rechaza nulo, NaN, infinitos, cero y negativos")
    void validarMayorACero_debeRechazarValoresInvalidos() {
        // Arrange
        Double[] invalidos = {null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -0.0, -1.5};

        // Act & Assert
        for (Double invalido : invalidos) {
            assertThatThrownBy(() -> ValidacionesNumericas.validarMayorACero(invalido, ETIQUETA))
                    .as("valor: %s", invalido)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La capacidad total debe ser mayor a 0");
        }
    }

    @Test
    @DisplayName("CP-VN-02: validarMayorACero acepta un valor positivo finito")
    void validarMayorACero_debeAceptarValorPositivo() {
        // Act & Assert
        assertThatCode(() -> ValidacionesNumericas.validarMayorACero(0.001, ETIQUETA)).doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarMayorACero(500.0, ETIQUETA)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-VN-03: validarMayorOIgualACero rechaza nulo, NaN, infinitos y negativos")
    void validarMayorOIgualACero_debeRechazarValoresInvalidos() {
        // Arrange
        Double[] invalidos = {null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.01, -10.0};

        // Act & Assert
        for (Double invalido : invalidos) {
            assertThatThrownBy(() -> ValidacionesNumericas.validarMayorOIgualACero(invalido, ETIQUETA))
                    .as("valor: %s", invalido)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La capacidad total debe ser mayor o igual a 0");
        }
    }

    @Test
    @DisplayName("CP-VN-04: validarMayorOIgualACero acepta cero y positivos")
    void validarMayorOIgualACero_debeAceptarCeroYPositivos() {
        // Act & Assert
        assertThatCode(() -> ValidacionesNumericas.validarMayorOIgualACero(0.0, ETIQUETA)).doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarMayorOIgualACero(12.5, ETIQUETA)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-VN-05: validarMaximo acepta un valor igual al máximo, menor al máximo o nulo")
    void validarMaximo_debeAceptarValoresHastaElMaximo() {
        // Act & Assert
        assertThatCode(() -> ValidacionesNumericas.validarMaximo(100_000.0, 100_000.0, ETIQUETA, "litros")).doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarMaximo(1.0, 100_000.0, ETIQUETA, "litros")).doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarMaximo(null, 100_000.0, ETIQUETA, "litros")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-VN-06: validarMaximo rechaza un valor apenas superior al máximo con el máximo formateado")
    void validarMaximo_debeRechazarValorSuperiorAlMaximo() {
        // Act & Assert
        assertThatThrownBy(() -> ValidacionesNumericas.validarMaximo(100_000.01, 100_000.0, ETIQUETA, "litros"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");
        assertThatThrownBy(() -> ValidacionesNumericas.validarMaximo(Double.POSITIVE_INFINITY, 100_000.0, ETIQUETA, "litros"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");
    }

    @Test
    @DisplayName("CP-VN-07: el máximo se formatea con separador de miles español y sin decimales sobrantes")
    void validarMaximo_debeFormatearElMaximoEnEspanol() {
        // Act & Assert
        assertThatThrownBy(() -> ValidacionesNumericas.validarMaximo(10_080.5, 10_080.0, "La duración", "minutos"))
                .hasMessage("La duración no puede superar 10.080 minutos");
        assertThatThrownBy(() -> ValidacionesNumericas.validarMaximo(1_000_001.0, 1_000_000.0, "La cantidad", "kg"))
                .hasMessage("La cantidad no puede superar 1.000.000 kg");
        assertThatThrownBy(() -> ValidacionesNumericas.validarMaximo(2.0, 1.5, "El valor", "unidades"))
                .hasMessage("El valor no puede superar 1,5 unidades");
        assertThatThrownBy(() -> ValidacionesNumericas.validarMaximo(21.0, 20.0, "La relación", "L/kg"))
                .hasMessage("La relación no puede superar 20 L/kg");
        assertThatThrownBy(() -> ValidacionesNumericas.validarMaximo(2_000.0, 1_000.0, "El valor", "u"))
                .hasMessage("El valor no puede superar 1.000 u");
    }

    @Test
    @DisplayName("CP-VN-08: validarMayorACeroConMaximo valida primero que sea positivo y luego el máximo")
    void validarMayorACeroConMaximo_debeValidarPositividadYMaximo() {
        // Act & Assert
        for (Double invalido : NO_FINITOS) {
            assertThatThrownBy(() -> ValidacionesNumericas.validarMayorACeroConMaximo(invalido, 100_000.0, ETIQUETA, "litros"))
                    .hasMessage("La capacidad total debe ser mayor a 0");
        }
        assertThatThrownBy(() -> ValidacionesNumericas.validarMayorACeroConMaximo(0.0, 100_000.0, ETIQUETA, "litros"))
                .hasMessage("La capacidad total debe ser mayor a 0");
        assertThatThrownBy(() -> ValidacionesNumericas.validarMayorACeroConMaximo(-1.0, 100_000.0, ETIQUETA, "litros"))
                .hasMessage("La capacidad total debe ser mayor a 0");
        assertThatThrownBy(() -> ValidacionesNumericas.validarMayorACeroConMaximo(100_001.0, 100_000.0, ETIQUETA, "litros"))
                .hasMessage("La capacidad total no puede superar 100.000 litros");
        assertThatCode(() -> ValidacionesNumericas.validarMayorACeroConMaximo(100_000.0, 100_000.0, ETIQUETA, "litros"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-VN-09: validarMayorOIgualACeroConMaximo acepta cero y el máximo, y rechaza negativos y excesos")
    void validarMayorOIgualACeroConMaximo_debeValidarNegatividadYMaximo() {
        // Act & Assert
        for (Double invalido : NO_FINITOS) {
            assertThatThrownBy(() -> ValidacionesNumericas.validarMayorOIgualACeroConMaximo(invalido, 100_000.0, ETIQUETA, "litros"))
                    .hasMessage("La capacidad total debe ser mayor o igual a 0");
        }
        assertThatThrownBy(() -> ValidacionesNumericas.validarMayorOIgualACeroConMaximo(-0.5, 100_000.0, ETIQUETA, "litros"))
                .hasMessage("La capacidad total debe ser mayor o igual a 0");
        assertThatThrownBy(() -> ValidacionesNumericas.validarMayorOIgualACeroConMaximo(100_000.5, 100_000.0, ETIQUETA, "litros"))
                .hasMessage("La capacidad total no puede superar 100.000 litros");
        assertThatCode(() -> ValidacionesNumericas.validarMayorOIgualACeroConMaximo(0.0, 100_000.0, ETIQUETA, "litros"))
                .doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarMayorOIgualACeroConMaximo(100_000.0, 100_000.0, ETIQUETA, "litros"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-VN-10: validarEnteroMayorACeroConMaximo rechaza nulo, cero y negativos")
    void validarEnteroMayorACeroConMaximo_debeRechazarNuloCeroYNegativos() {
        // Act & Assert
        for (Integer invalido : new Integer[]{null, 0, -1}) {
            assertThatThrownBy(() -> ValidacionesNumericas.validarEnteroMayorACeroConMaximo(invalido, 10_080, "La duración", "minutos"))
                    .as("valor: %s", invalido)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("La duración debe ser mayor a 0");
        }
    }

    @Test
    @DisplayName("CP-VN-11: validarEnteroMayorACeroConMaximo acepta el máximo y rechaza el valor siguiente")
    void validarEnteroMayorACeroConMaximo_debeValidarElMaximo() {
        // Act & Assert
        assertThatCode(() -> ValidacionesNumericas.validarEnteroMayorACeroConMaximo(1, 10_080, "La duración", "minutos"))
                .doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarEnteroMayorACeroConMaximo(10_080, 10_080, "La duración", "minutos"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ValidacionesNumericas.validarEnteroMayorACeroConMaximo(10_081, 10_080, "La duración", "minutos"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La duración no puede superar 10.080 minutos");
    }

    @Test
    @DisplayName("CP-VN-12: validarEnteroMayorOIgualACeroConMaximo rechaza nulo y negativos")
    void validarEnteroMayorOIgualACeroConMaximo_debeRechazarNuloYNegativos() {
        // Act & Assert
        for (Integer invalido : new Integer[]{null, -1}) {
            assertThatThrownBy(() -> ValidacionesNumericas.validarEnteroMayorOIgualACeroConMaximo(invalido, 500, "El amargor", "IBU"))
                    .as("valor: %s", invalido)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El amargor debe ser mayor o igual a 0");
        }
    }

    @Test
    @DisplayName("CP-VN-13: validarEnteroMayorOIgualACeroConMaximo acepta cero y el máximo, y rechaza el valor siguiente")
    void validarEnteroMayorOIgualACeroConMaximo_debeValidarElMaximo() {
        // Act & Assert
        assertThatCode(() -> ValidacionesNumericas.validarEnteroMayorOIgualACeroConMaximo(0, 500, "El amargor", "IBU"))
                .doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarEnteroMayorOIgualACeroConMaximo(500, 500, "El amargor", "IBU"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ValidacionesNumericas.validarEnteroMayorOIgualACeroConMaximo(501, 500, "El amargor", "IBU"))
                .hasMessage("El amargor no puede superar 500 IBU");
        assertThatThrownBy(() -> ValidacionesNumericas.validarEnteroMayorOIgualACeroConMaximo(100_001, 100_000, "Los usos", "usos"))
                .hasMessage("Los usos no puede superar 100.000 usos");
    }

    @Test
    @DisplayName("CP-VN-14: validarLongitudMaxima acepta nulo, vacío y la longitud exacta")
    void validarLongitudMaxima_debeAceptarHastaElMaximo() {
        // Arrange
        String exacto = "a".repeat(100);

        // Act & Assert
        assertThatCode(() -> ValidacionesNumericas.validarLongitudMaxima(null, 100, "El nombre")).doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarLongitudMaxima("", 100, "El nombre")).doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarLongitudMaxima(exacto, 100, "El nombre")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-VN-15: validarLongitudMaxima rechaza un texto con un carácter más que el máximo")
    void validarLongitudMaxima_debeRechazarTextoDemasiadoLargo() {
        // Arrange
        String excedido = "a".repeat(101);

        // Act & Assert
        assertThatThrownBy(() -> ValidacionesNumericas.validarLongitudMaxima(excedido, 100, "El nombre"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre no puede superar 100 caracteres");
    }

    @Test
    @DisplayName("CP-VN-16: validarMaximoDecimales acepta nulo, NaN, infinitos y valores con hasta el máximo de decimales (ignorando ceros finales)")
    void validarMaximoDecimales_debeAceptarHastaElMaximo() {
        // Act & Assert
        for (Double noEvaluado : NO_FINITOS) {
            assertThatCode(() -> ValidacionesNumericas.validarMaximoDecimales(noEvaluado, 1, "El IBU objetivo")).doesNotThrowAnyException();
        }
        assertThatCode(() -> ValidacionesNumericas.validarMaximoDecimales(35.0, 1, "El IBU objetivo")).doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarMaximoDecimales(35.5, 1, "El IBU objetivo")).doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarMaximoDecimales(35.50, 1, "El IBU objetivo")).doesNotThrowAnyException();
        assertThatCode(() -> ValidacionesNumericas.validarMaximoDecimales(0.1, 1, "El IBU objetivo")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-VN-17: validarMaximoDecimales rechaza un valor con más decimales que el máximo y usa singular o plural en el mensaje")
    void validarMaximoDecimales_debeRechazarDecimalesDeMas() {
        // Act & Assert
        assertThatThrownBy(() -> ValidacionesNumericas.validarMaximoDecimales(35.55, 1, "El IBU objetivo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El IBU objetivo no puede tener más de 1 decimal");
        assertThatThrownBy(() -> ValidacionesNumericas.validarMaximoDecimales(0.25, 1, "El IBU objetivo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El IBU objetivo no puede tener más de 1 decimal");
        assertThatThrownBy(() -> ValidacionesNumericas.validarMaximoDecimales(1.12345, 4, "El costo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El costo no puede tener más de 4 decimales");
    }
}
