package com.github.heikyudev.maestrocervecero.util.validacion;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacionesTextoTest {

    private static final String MENSAJE_CUIT_LARGO = "El CUIT debe tener 11 dígitos";
    private static final String MENSAJE_CUIT_INVALIDO = "El CUIT no es válido";
    private static final String MENSAJE_TELEFONO_DIGITOS = "El teléfono solo puede contener dígitos";
    private static final String MENSAJE_USERNAME_FORMATO =
            "El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos, sin espacios";

    // ==================== validarCuit ====================

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "20123456786",  // 20-12345678-6
            "30546689979",  // 30-54668997-9 (YPF S.A.)
            "30703088534",  // 30-70308853-4 (MercadoLibre S.R.L.)
            "33693450239",  // 33-69345023-9 (AFIP)
            "30500010912",  // 30-50001091-2
            "27200000000"   // 27-20000000-0 (verificador 0)
    })
    @DisplayName("CP-VT-01: validarCuit acepta CUIT reales con prefijo y dígito verificador correctos")
    void validarCuit_debeAceptarCuitsValidos(String cuit) {
        assertThatCode(() -> ValidacionesTexto.validarCuit(cuit)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "2012345678", "201234567866", "123", "20 12345678 6", "20-12345678-6"})
    @DisplayName("CP-VT-02: validarCuit rechaza un valor nulo o de largo distinto de 11 con \"El CUIT debe tener 11 dígitos\"")
    void validarCuit_debeRechazarLargoIncorrecto(String cuit) {
        assertThatThrownBy(() -> ValidacionesTexto.validarCuit(cuit))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_CUIT_LARGO);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"2012345678a", "2O123456786", "abcdefghijk", "20123456 86", "٢٠١٢٣٤٥٦٧٨٦"})
    @DisplayName("CP-VT-03: validarCuit rechaza un valor de 11 caracteres que no son todos dígitos ASCII")
    void validarCuit_debeRechazarCaracteresNoNumericos(String cuit) {
        assertThatThrownBy(() -> ValidacionesTexto.validarCuit(cuit))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_CUIT_LARGO);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"20123456787", "20123456780", "30546689970", "33693450238", "27200000001"})
    @DisplayName("CP-VT-04: validarCuit rechaza un dígito verificador incorrecto con \"El CUIT no es válido\"")
    void validarCuit_debeRechazarDigitoVerificadorIncorrecto(String cuit) {
        assertThatThrownBy(() -> ValidacionesTexto.validarCuit(cuit))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_CUIT_INVALIDO);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"10123456780", "21123456782", "25123456788", "40123456787", "00123456785"})
    @DisplayName("CP-VT-05: validarCuit rechaza un prefijo que no es 20, 23, 24, 27, 30, 33 ni 34 aunque el verificador sea correcto")
    void validarCuit_debeRechazarPrefijoInvalido(String cuit) {
        assertThatThrownBy(() -> ValidacionesTexto.validarCuit(cuit))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_CUIT_INVALIDO);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"30689999910", "30689999911", "30689999915", "30689999919"})
    @DisplayName("CP-VT-06: validarCuit rechaza un número cuyo módulo 11 no admite dígito verificador, sea cual sea el último dígito")
    void validarCuit_debeRechazarNumeroSinDigitoVerificadorPosible(String cuit) {
        assertThatThrownBy(() -> ValidacionesTexto.validarCuit(cuit))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_CUIT_INVALIDO);
    }

    // ==================== formatearCuit ====================

    static Stream<Arguments> cuitsAFormatear() {
        return Stream.of(
                Arguments.of("20123456786", "20-12345678-6"),
                Arguments.of("20-12345678-6", "20-12345678-6"),
                Arguments.of("20 12345678 6", "20-12345678-6"),
                Arguments.of("30546689979", "30-54668997-9"));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("cuitsAFormatear")
    @DisplayName("CP-VT-07: formatearCuit muestra un CUIT de 11 dígitos con el formato XX-XXXXXXXX-X")
    void formatearCuit_debeAgregarGuiones(String cuit, String esperado) {
        assertThat(ValidacionesTexto.formatearCuit(cuit)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("CP-VT-08: formatearCuit devuelve sin cambios un valor que no tiene 11 dígitos, y null para null")
    void formatearCuit_debeDevolverSinCambiosUnValorIncompleto() {
        assertThat(ValidacionesTexto.formatearCuit(null)).isNull();
        assertThat(ValidacionesTexto.formatearCuit("")).isEmpty();
        assertThat(ValidacionesTexto.formatearCuit("123")).isEqualTo("123");
        assertThat(ValidacionesTexto.formatearCuit("2012345678a")).isEqualTo("2012345678a");
    }

    // ==================== validarTelefono ====================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "1", "1144445555", "011444455556", "123456789012345"})
    @DisplayName("CP-VT-09: validarTelefono acepta nulo, vacío y hasta 15 dígitos")
    void validarTelefono_debeAceptarValoresValidos(String telefono) {
        assertThatCode(() -> ValidacionesTexto.validarTelefono(telefono)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"011-4444-5555", "+5491144445555", "11 4444 5555", "(011)44445555", "11a4444", "1144.4455", "١٢٣٤٥"})
    @DisplayName("CP-VT-10: validarTelefono rechaza cualquier carácter que no sea un dígito")
    void validarTelefono_debeRechazarCaracteresNoNumericos(String telefono) {
        assertThatThrownBy(() -> ValidacionesTexto.validarTelefono(telefono))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_TELEFONO_DIGITOS);
    }

    @Test
    @DisplayName("CP-VT-11: validarTelefono rechaza más de 15 dígitos")
    void validarTelefono_debeRechazarMasDe15Digitos() {
        assertThatThrownBy(() -> ValidacionesTexto.validarTelefono("1234567890123456"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El teléfono no puede superar 15 dígitos");
    }

    // ==================== validarUsername ====================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"a", "juan.perez", "juan_perez", "juan-perez", "juan.perez-2_x", "30123456", "Admin", "ADMIN01"})
    @DisplayName("CP-VT-12: validarUsername acepta letras, números, puntos, guiones y guiones bajos (incluido un DNI)")
    void validarUsername_debeAceptarValoresValidos(String username) {
        assertThatCode(() -> ValidacionesTexto.validarUsername(username)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullSource
    @ValueSource(strings = {""})
    @DisplayName("CP-VT-13: validarUsername rechaza un nombre de usuario nulo o vacío")
    void validarUsername_debeRechazarNuloOVacio(String username) {
        assertThatThrownBy(() -> ValidacionesTexto.validarUsername(username))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre de usuario es obligatorio");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"juan perez", " juan", "juan\tperez", "juan@mail.com", "ñandú", "josé", "juan!", "juan/perez", "juan+perez", "٣٠١٢٣"})
    @DisplayName("CP-VT-14: validarUsername rechaza espacios, tildes, eñes y símbolos con un mensaje que explica qué se permite")
    void validarUsername_debeRechazarCaracteresInvalidos(String username) {
        assertThatThrownBy(() -> ValidacionesTexto.validarUsername(username))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_USERNAME_FORMATO);
    }
}
