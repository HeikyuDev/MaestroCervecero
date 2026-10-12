package com.github.heikyudev.maestrocervecero.util.validacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizacionTextoTest {

    private static final String NBSP = " ";

    // ==================== nombrePropio ====================

    static Stream<Arguments> espaciosAscii() {
        return Stream.of(
                Arguments.of("  Pale Ale  ", "Pale Ale"),
                Arguments.of("Pale    Ale", "Pale Ale"),
                Arguments.of("Pale  \t \n Ale", "Pale Ale"),
                Arguments.of("\tPale Ale\t", "Pale Ale"),
                Arguments.of("\r\nPale\r\nAle\r\n", "Pale Ale"),
                Arguments.of("Pale Ale", "Pale Ale"));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("espaciosAscii")
    @DisplayName("CP-NT-01: nombrePropio recorta los extremos y colapsa los espacios, tabulaciones y saltos de línea internos")
    void nombrePropio_debeRecortarYColapsarEspacios(String entrada, String esperado) {
        assertThat(NormalizacionTexto.nombrePropio(entrada)).isEqualTo(esperado);
    }

    static Stream<Arguments> espaciosUnicode() {
        return Stream.of(
                Arguments.of(NBSP + "Pale" + NBSP + NBSP + "Ale" + NBSP, "Pale Ale"),
                Arguments.of("Pale Ale", "Pale Ale"),
                Arguments.of("Pale Ale", "Pale Ale"),
                Arguments.of("Pale　Ale", "Pale Ale"),
                Arguments.of(" Pale     Ale ", "Pale Ale"));
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("espaciosUnicode")
    @DisplayName("CP-NT-02: nombrePropio trata como espacio los espacios no separables y los separadores Unicode")
    void nombrePropio_debeColapsarEspaciosUnicode(String entrada, String esperado) {
        assertThat(NormalizacionTexto.nombrePropio(entrada)).isEqualTo(esperado);
    }

    static Stream<Arguments> anchoCero() {
        return Stream.of(
                Arguments.of("​Pale Ale​", "Pale Ale"),
                Arguments.of("Pa​le Ale", "Pale Ale"),
                Arguments.of("﻿Pale Ale", "Pale Ale"),
                Arguments.of("Pale⁠ Ale‍", "Pale Ale"),
                Arguments.of("​‌‍", ""));
    }

    @ParameterizedTest(name = "[{index}]")
    @MethodSource("anchoCero")
    @DisplayName("CP-NT-03: nombrePropio elimina los caracteres invisibles de ancho cero")
    void nombrePropio_debeEliminarCaracteresDeAnchoCero(String entrada, String esperado) {
        assertThat(NormalizacionTexto.nombrePropio(entrada)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("textosConMayusculasYTildes")
    @DisplayName("CP-NT-04: nombrePropio conserva las mayúsculas, las tildes, la ñ y la puntuación escritas")
    void nombrePropio_debeConservarMayusculasTildesYPuntuacion(String entrada, String esperado) {
        assertThat(NormalizacionTexto.nombrePropio(entrada)).isEqualTo(esperado);
    }

    static Stream<Arguments> textosConMayusculasYTildes() {
        return Stream.of(
                Arguments.of("Cervecería  Ñandú S.A.", "Cervecería Ñandú S.A."),
                Arguments.of("  José   Pérez-Muñoz ", "José Pérez-Muñoz"),
                Arguments.of("IPA (Doble) #1", "IPA (Doble) #1"),
                Arguments.of("pale ale", "pale ale"));
    }

    @Test
    @DisplayName("CP-NT-05: nombrePropio pasa el texto a forma Unicode NFC")
    void nombrePropio_debePasarANfc() {
        // "e" + acento combinante (NFD) debe quedar como la "é" precompuesta (NFC)
        assertThat(NormalizacionTexto.nombrePropio("Caf" + "é")).isEqualTo("Café");
        assertThat(NormalizacionTexto.nombrePropio("ñandu")).isEqualTo("ñandu");
    }

    @Test
    @DisplayName("CP-NT-06: nombrePropio devuelve null para null y cadena vacía para vacío o solo espacios")
    void nombrePropio_debeManejarNuloVacioYSoloEspacios() {
        assertThat(NormalizacionTexto.nombrePropio(null)).isNull();
        assertThat(NormalizacionTexto.nombrePropio("")).isEmpty();
        assertThat(NormalizacionTexto.nombrePropio("   \t ")).isEmpty();
        assertThat(NormalizacionTexto.nombrePropio(NBSP + NBSP)).isEmpty();
    }

    // ==================== codigo ====================

    static Stream<Arguments> codigos() {
        return Stream.of(
                Arguments.of("fer-01", "FER-01"),
                Arguments.of("  fer-01  ", "FER-01"),
                Arguments.of("fer   01", "FER 01"),
                Arguments.of(NBSP + "fer.01/a" + NBSP, "FER.01/A"),
                Arguments.of("ñandú-1", "ÑANDÚ-1"),
                Arguments.of("Lote 2026/001", "LOTE 2026/001"),
                Arguments.of("YA-EN-MAYUSCULAS", "YA-EN-MAYUSCULAS"));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("codigos")
    @DisplayName("CP-NT-07: codigo recorta, colapsa espacios, pasa a mayúsculas y conserva la puntuación")
    void codigo_debeNormalizarYPasarAMayusculas(String entrada, String esperado) {
        assertThat(NormalizacionTexto.codigo(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("CP-NT-08: codigo pasa a mayúsculas con Locale.ROOT, sin depender del idioma del servidor")
    void codigo_noDebeDependerDelIdiomaPorDefecto() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(NormalizacionTexto.codigo("titan-i")).isEqualTo("TITAN-I");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    @DisplayName("CP-NT-09: codigo devuelve null para null y cadena vacía para vacío o solo espacios")
    void codigo_debeManejarNuloYVacio() {
        assertThat(NormalizacionTexto.codigo(null)).isNull();
        assertThat(NormalizacionTexto.codigo("")).isEmpty();
        assertThat(NormalizacionTexto.codigo("  \t ")).isEmpty();
    }

    // ==================== correo ====================

    static Stream<Arguments> correos() {
        return Stream.of(
                Arguments.of("juan@mail.com", "juan@mail.com"),
                Arguments.of("  Juan.Perez@Mail.COM  ", "juan.perez@mail.com"),
                Arguments.of(NBSP + "JUAN@MAIL.COM" + NBSP, "juan@mail.com"),
                Arguments.of("\tAdmin@Cerveceria.com.ar\n", "admin@cerveceria.com.ar"));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("correos")
    @DisplayName("CP-NT-10: correo recorta los extremos y pasa a minúsculas")
    void correo_debeRecortarYPasarAMinusculas(String entrada, String esperado) {
        assertThat(NormalizacionTexto.correo(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("CP-NT-11: correo devuelve null para null y cadena vacía para vacío o solo espacios")
    void correo_debeManejarNuloYVacio() {
        assertThat(NormalizacionTexto.correo(null)).isNull();
        assertThat(NormalizacionTexto.correo("")).isEmpty();
        assertThat(NormalizacionTexto.correo("   ")).isEmpty();
    }

    // ==================== cuit ====================

    static Stream<Arguments> cuits() {
        return Stream.of(
                Arguments.of("20123456786", "20123456786"),
                Arguments.of("20-12345678-6", "20123456786"),
                Arguments.of("20 12345678 6", "20123456786"),
                Arguments.of("20.12345678.6", "20123456786"),
                Arguments.of("20/12345678/6", "20123456786"),
                Arguments.of("  20-12345678-6  ", "20123456786"),
                Arguments.of(NBSP + "20‐12345678–6" + NBSP, "20123456786"));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("cuits")
    @DisplayName("CP-NT-12: cuit elimina los separadores (espacios, guiones, puntos y barras) y deja solo los dígitos")
    void cuit_debeEliminarSeparadores(String entrada, String esperado) {
        assertThat(NormalizacionTexto.cuit(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("CP-NT-13: cuit conserva los caracteres que no son separadores para que la validación los rechace")
    void cuit_noDebeEliminarLetrasNiSimbolos() {
        assertThat(NormalizacionTexto.cuit("20-1234567a-6")).isEqualTo("201234567a6");
        assertThat(NormalizacionTexto.cuit("CUIT 20123456786")).isEqualTo("CUIT20123456786");
        assertThat(NormalizacionTexto.cuit("20123456786+")).isEqualTo("20123456786+");
    }

    @Test
    @DisplayName("CP-NT-14: cuit devuelve null para null y cadena vacía para vacío o solo separadores")
    void cuit_debeManejarNuloYVacio() {
        assertThat(NormalizacionTexto.cuit(null)).isNull();
        assertThat(NormalizacionTexto.cuit("")).isEmpty();
        assertThat(NormalizacionTexto.cuit(" - ")).isEmpty();
    }

    // ==================== telefono ====================

    @Test
    @DisplayName("CP-NT-15: telefono recorta los extremos y conserva el número")
    void telefono_debeRecortar() {
        assertThat(NormalizacionTexto.telefono("1144445555")).isEqualTo("1144445555");
        assertThat(NormalizacionTexto.telefono("  1144445555  ")).isEqualTo("1144445555");
        assertThat(NormalizacionTexto.telefono(NBSP + "1144445555" + NBSP)).isEqualTo("1144445555");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @NullSource
    @MethodSource("telefonosVacios")
    @DisplayName("CP-NT-16: telefono devuelve null para null, vacío o solo espacios (el teléfono es opcional)")
    void telefono_debeDevolverNullSiNoHayContenido(String entrada) {
        assertThat(NormalizacionTexto.telefono(entrada)).isNull();
    }

    static Stream<String> telefonosVacios() {
        return Stream.of("", " ", "   \t ", NBSP, "​");
    }

    @Test
    @DisplayName("CP-NT-17: telefono conserva los caracteres que no son dígitos para que la validación los rechace")
    void telefono_noDebeEliminarSimbolos() {
        assertThat(NormalizacionTexto.telefono("011-4444-5555")).isEqualTo("011-4444-5555");
        assertThat(NormalizacionTexto.telefono("+54 9 11 4444 5555")).isEqualTo("+54 9 11 4444 5555");
        assertThat(NormalizacionTexto.telefono("011   4444")).isEqualTo("011 4444");
    }

    // ==================== username ====================

    static Stream<Arguments> usernames() {
        return Stream.of(
                Arguments.of("juan.perez", "juan.perez"),
                Arguments.of("  Juan.Perez  ", "juan.perez"),
                Arguments.of("ADMIN", "admin"),
                Arguments.of("30123456", "30123456"),
                Arguments.of(NBSP + "Juan_Perez-2" + NBSP, "juan_perez-2"));
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @MethodSource("usernames")
    @DisplayName("CP-NT-18: username recorta los extremos y pasa a minúsculas")
    void username_debeRecortarYPasarAMinusculas(String entrada, String esperado) {
        assertThat(NormalizacionTexto.username(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("CP-NT-19: username conserva los espacios internos (colapsados) para que la validación los rechace")
    void username_noDebeEliminarEspaciosInternos() {
        assertThat(NormalizacionTexto.username("Juan   Perez")).isEqualTo("juan perez");
    }

    @Test
    @DisplayName("CP-NT-20: username devuelve null para null y cadena vacía para vacío o solo espacios")
    void username_debeManejarNuloYVacio() {
        assertThat(NormalizacionTexto.username(null)).isNull();
        assertThat(NormalizacionTexto.username("")).isEmpty();
        assertThat(NormalizacionTexto.username("   ")).isEmpty();
    }

    @Test
    @DisplayName("CP-NT-21: username pasa a minúsculas con Locale.ROOT, sin depender del idioma del servidor")
    void username_noDebeDependerDelIdiomaPorDefecto() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertThat(NormalizacionTexto.username("ISMAEL")).isEqualTo("ismael");
            assertThat(NormalizacionTexto.correo("INFO@MAIL.COM")).isEqualTo("info@mail.com");
        } finally {
            Locale.setDefault(original);
        }
    }
}
