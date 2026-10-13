package com.github.heikyudev.maestrocervecero.util.validacion;

import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BloqueoPorLotesVivosTest {

    private static final String CAMPOS = "el extracto potencial de la malta";

    @Test
    @DisplayName("CP-BLV-01: validar no lanza excepción cuando no hay lotes (lista vacía o nula)")
    void validar_debeAceptarSinLotes() {
        assertThatCode(() -> BloqueoPorLotesVivos.validar(List.of(), CAMPOS, "la")).doesNotThrowAnyException();
        assertThatCode(() -> BloqueoPorLotesVivos.validar(null, CAMPOS, "la")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-BLV-02: validar usa el singular cuando hay un solo lote")
    void validar_debeUsarSingularConUnLote() {
        assertThatThrownBy(() -> BloqueoPorLotesVivos.validar(List.of("PILSEN_CLASICA-R7-3"), CAMPOS, "la"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede modificar el extracto potencial de la malta porque la usa el lote PILSEN_CLASICA-R7-3");
    }

    @Test
    @DisplayName("CP-BLV-03: validar une dos lotes con ' y '")
    void validar_debeUnirDosLotes() {
        assertThatThrownBy(() -> BloqueoPorLotesVivos.validar(List.of("A-R1-1", "A-R1-2"), CAMPOS, "lo"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede modificar el extracto potencial de la malta porque lo usan los lotes A-R1-1 y A-R1-2");
    }

    @Test
    @DisplayName("CP-BLV-04: validar separa con ', ' y une el último con ' y ' cuando hay tres lotes")
    void validar_debeUnirTresLotes() {
        assertThatThrownBy(() -> BloqueoPorLotesVivos.validar(List.of("A-R1-1", "A-R1-2", "A-R1-3"), CAMPOS, "la"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede modificar el extracto potencial de la malta porque la usan los lotes A-R1-1, A-R1-2 y A-R1-3");
    }

    @Test
    @DisplayName("CP-BLV-05: validar lista los 5 lotes completos cuando hay exactamente 5 (límite)")
    void validar_debeListarCincoLotes() {
        assertThatThrownBy(() -> BloqueoPorLotesVivos.validar(List.of("L1", "L2", "L3", "L4", "L5"), CAMPOS, "la"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede modificar el extracto potencial de la malta porque la usan los lotes L1, L2, L3, L4 y L5");
    }

    @Test
    @DisplayName("CP-BLV-06: validar limita a los primeros 5 lotes y agrega 'y 1 más' cuando hay 6 (límite)")
    void validar_debeResumirUnLoteSobrante() {
        assertThatThrownBy(() -> BloqueoPorLotesVivos.validar(List.of("L1", "L2", "L3", "L4", "L5", "L6"), CAMPOS, "la"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede modificar el extracto potencial de la malta porque la usan los lotes L1, L2, L3, L4, L5 y 1 más");
    }

    @Test
    @DisplayName("CP-BLV-07: validar limita a los primeros 5 lotes y agrega 'y N más' cuando hay más de 6")
    void validar_debeResumirLotesSobrantes() {
        assertThatThrownBy(() -> BloqueoPorLotesVivos.validar(
                List.of("L1", "L2", "L3", "L4", "L5", "L6", "L7", "L8"), CAMPOS, "la"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede modificar el extracto potencial de la malta porque la usan los lotes L1, L2, L3, L4, L5 y 3 más");
    }
}
