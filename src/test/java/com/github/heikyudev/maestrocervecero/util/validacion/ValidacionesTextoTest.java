package com.github.heikyudev.maestrocervecero.util.validacion;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidacionesTextoTest {

    private static final String MENSAJE_CUIT_LARGO = "El CUIT debe tener 11 dígitos";
    private static final String MENSAJE_TELEFONO_DIGITOS = "El teléfono solo puede contener dígitos";
    private static final String MENSAJE_USERNAME_FORMATO =
            "El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos, sin espacios";
    private static final String MENSAJE_CORREO_FORMATO = "El correo electrónico no tiene un formato válido";

    // ==================== validarCuit ====================

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
            "20123456786",  // 20-12345678-6
            "30546689979",  // 30-54668997-9 (YPF S.A.)
            "30703088534",  // 30-70308853-4 (MercadoLibre S.R.L.)
            "33693450239",  // 33-69345023-9 (AFIP)
            "30500010912",  // 30-50001091-2
            "27200000000"   // 27-20000000-0
    })
    @DisplayName("CP-VT-01: validarCuit acepta CUIT de 11 dígitos")
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
    @DisplayName("CP-VT-04: formatearCuit muestra un CUIT de 11 dígitos con el formato XX-XXXXXXXX-X")
    void formatearCuit_debeAgregarGuiones(String cuit, String esperado) {
        assertThat(ValidacionesTexto.formatearCuit(cuit)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("CP-VT-05: formatearCuit devuelve sin cambios un valor que no tiene 11 dígitos, y null para null")
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
    @DisplayName("CP-VT-06: validarTelefono acepta nulo, vacío y hasta 15 dígitos")
    void validarTelefono_debeAceptarValoresValidos(String telefono) {
        assertThatCode(() -> ValidacionesTexto.validarTelefono(telefono)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"011-4444-5555", "+5491144445555", "11 4444 5555", "(011)44445555", "11a4444", "1144.4455", "١٢٣٤٥"})
    @DisplayName("CP-VT-07: validarTelefono rechaza cualquier carácter que no sea un dígito")
    void validarTelefono_debeRechazarCaracteresNoNumericos(String telefono) {
        assertThatThrownBy(() -> ValidacionesTexto.validarTelefono(telefono))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_TELEFONO_DIGITOS);
    }

    @Test
    @DisplayName("CP-VT-08: validarTelefono rechaza más de 15 dígitos")
    void validarTelefono_debeRechazarMasDe15Digitos() {
        assertThatThrownBy(() -> ValidacionesTexto.validarTelefono("1234567890123456"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El teléfono no puede superar 15 dígitos");
    }

    // ==================== validarUsername ====================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"a", "juan.perez", "juan_perez", "juan-perez", "juan.perez-2_x", "30123456", "Admin", "ADMIN01"})
    @DisplayName("CP-VT-09: validarUsername acepta letras, números, puntos, guiones y guiones bajos (incluido un DNI)")
    void validarUsername_debeAceptarValoresValidos(String username) {
        assertThatCode(() -> ValidacionesTexto.validarUsername(username)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullSource
    @ValueSource(strings = {""})
    @DisplayName("CP-VT-10: validarUsername rechaza un nombre de usuario nulo o vacío")
    void validarUsername_debeRechazarNuloOVacio(String username) {
        assertThatThrownBy(() -> ValidacionesTexto.validarUsername(username))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre de usuario es obligatorio");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"juan perez", " juan", "juan\tperez", "juan@mail.com", "ñandú", "josé", "juan!", "juan/perez", "juan+perez", "٣٠١٢٣"})
    @DisplayName("CP-VT-11: validarUsername rechaza espacios, tildes, eñes y símbolos con un mensaje que explica qué se permite")
    void validarUsername_debeRechazarCaracteresInvalidos(String username) {
        assertThatThrownBy(() -> ValidacionesTexto.validarUsername(username))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_USERNAME_FORMATO);
    }

    // ==================== validarCorreo ====================

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"ventas@malteria.com.ar", "a.b+c@dominio.co", "contacto@malteriadelsur.com",
            "juan_perez-2@mail-sur.com.ar", "info%ventas@x1.io", "VENTAS@MALTERIA.COM"})
    @DisplayName("CP-VT-12: validarCorreo acepta una parte local, un único @ y un dominio con al menos un punto")
    void validarCorreo_debeAceptarCorreosValidos(String correo) {
        assertThatCode(() -> ValidacionesTexto.validarCorreo(correo)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "no-es-correo", "a@b", "a@@b.com", "a b@c.com", "@x.com", "x@.com", "x@y.",
            ".a@b.com", "a.@b.com", "a..b@c.com", "a@b..com", "a@-b.com", "a@b-.com", "a@b.c", "a@b.com.",
            "a@b,com", "ñandu@b.com", "a@b@c.com", "a@b.co m"})
    @DisplayName("CP-VT-13: validarCorreo rechaza un valor sin el formato de un correo electrónico con \"El correo electrónico no tiene un formato válido\"")
    void validarCorreo_debeRechazarFormatosInvalidos(String correo) {
        assertThatThrownBy(() -> ValidacionesTexto.validarCorreo(correo))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MENSAJE_CORREO_FORMATO);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"", "   ", "ventas@malteria.com.ar", "  Ventas@Malteria.COM.ar  ", "a.b+c@dominio.co"})
    @DisplayName("CP-VT-14: el patrón del formulario acepta un correo válido con espacios en los extremos o mayúsculas, y el valor vacío (lo informa la obligatoriedad)")
    void patronCorreoFormulario_debeAceptarLoQueAceptaElService(String correo) {
        assertThat(Pattern.matches(ValidacionesTexto.PATRON_CORREO_FORMULARIO, correo)).isTrue();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"no-es-correo", "a@b", "a@@b.com", "a b@c.com", "@x.com", "x@.com", "x@y.",
            "a..b@c.com", "a@b.c", "ñandu@b.com"})
    @DisplayName("CP-VT-15: el patrón del formulario rechaza los mismos formatos inválidos que validarCorreo")
    void patronCorreoFormulario_debeRechazarLoQueRechazaElService(String correo) {
        assertThat(Pattern.matches(ValidacionesTexto.PATRON_CORREO_FORMULARIO, correo)).isFalse();
    }
}
