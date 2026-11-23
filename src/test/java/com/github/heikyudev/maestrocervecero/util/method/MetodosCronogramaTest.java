package com.github.heikyudev.maestrocervecero.util.method;

import com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion;
import com.github.heikyudev.maestrocervecero.util.method.MetodosCronograma.CriterioDeCola;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion.ALTA;
import static com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion.BAJA;
import static com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion.MEDIA;
import static org.assertj.core.api.Assertions.assertThat;

class MetodosCronogramaTest {

    private static final LocalDate INICIO = LocalDate.of(2027, 7, 1);

    /** Criterio con fin 31/07/2027: igual para todos, así solo cuenta la prioridad. */
    private static CriterioDeCola p(PrioridadPlanificacion prioridad) {
        return f(prioridad, 30);
    }

    /** Criterio cuya planificación termina los días indicados después del 01/07/2027 ({@code null}: sin fecha de finalización). */
    private static CriterioDeCola f(PrioridadPlanificacion prioridad, Integer diasHastaElFin) {
        return new CriterioDeCola(prioridad, diasHastaElFin == null ? null : INICIO.plusDays(diasHastaElFin));
    }

    private static List<CriterioDeCola> cola(PrioridadPlanificacion... prioridades) {
        return Arrays.stream(prioridades).map(MetodosCronogramaTest::p).toList();
    }

    @Test
    @DisplayName("CP-MCR-01: calcularPosicionEnCola ubica un lote de prioridad ALTA delante de todos los pendientes de menor prioridad")
    void calcularPosicionEnCola_altaVaDelanteDeLosDeMenorPrioridad() {
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola(BAJA, BAJA), p(ALTA))).isEqualTo(0);
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola(ALTA, MEDIA, BAJA), p(ALTA))).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-MCR-02: calcularPosicionEnCola ubica un lote de igual prioridad y desempate completo detrás de los que ya estaban (el primero que llega va primero)")
    void calcularPosicionEnCola_igualPrioridadVaAlFinal() {
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola(MEDIA, MEDIA), p(MEDIA))).isEqualTo(2);
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola(ALTA, MEDIA, MEDIA), p(MEDIA))).isEqualTo(3);
    }

    @Test
    @DisplayName("CP-MCR-03: calcularPosicionEnCola ubica un lote de prioridad MEDIA entre los de ALTA y los de BAJA")
    void calcularPosicionEnCola_mediaVaEntreAltaYBaja() {
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola(ALTA, ALTA, BAJA, BAJA), p(MEDIA))).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-MCR-04: calcularPosicionEnCola ubica un lote de prioridad BAJA al final aunque la cola tenga lotes de BAJA")
    void calcularPosicionEnCola_bajaVaAlFinal() {
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola(ALTA, MEDIA, BAJA), p(BAJA))).isEqualTo(3);
    }

    @Test
    @DisplayName("CP-MCR-05: calcularPosicionEnCola en una cola vacía devuelve 0")
    void calcularPosicionEnCola_colaVaciaDevuelveCero() {
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(), p(ALTA))).isEqualTo(0);
    }

    @Test
    @DisplayName("CP-MCR-06: calcularPosicionEnCola nunca pasa delante de un lote que le gana o empata en una cola reordenada a mano")
    void calcularPosicionEnCola_colaDesordenadaNoPasaPorEncimaDeIgualOMayor() {
        // BAJA, MEDIA, BAJA: un lote MEDIA queda detrás del MEDIA existente, aunque haya una BAJA delante
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola(BAJA, MEDIA, BAJA), p(MEDIA))).isEqualTo(2);
        // MEDIA, BAJA, ALTA: un lote MEDIA queda detrás de la ALTA
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola(MEDIA, BAJA, ALTA), p(MEDIA))).isEqualTo(3);
    }

    @Test
    @DisplayName("CP-MCR-07: calcularPosicionEnCola trata una prioridad nula como MEDIA")
    void calcularPosicionEnCola_prioridadNulaSeTrataComoMedia() {
        List<CriterioDeCola> conNulo = Arrays.asList(p(null), p(BAJA));
        assertThat(MetodosCronograma.calcularPosicionEnCola(conNulo, p(null))).isEqualTo(1);
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola(MEDIA, BAJA), p(null))).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-MCR-08: a igual prioridad, la planificación con la fecha de finalización estimada más temprana va primero")
    void calcularPosicionEnCola_finMasTempranoVaPrimero() {
        // Cola MEDIA que termina el 11/07 y el 21/07: un lote que termina el 16/07 va entre los dos; el 06/07, primero; el 26/07, último
        List<CriterioDeCola> cola = List.of(f(MEDIA, 10), f(MEDIA, 20));
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola, f(MEDIA, 15))).isEqualTo(1);
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola, f(MEDIA, 5))).isEqualTo(0);
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola, f(MEDIA, 25))).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-MCR-09: la duración de la planificación no interviene: la #8 (15/10 al 31/01) queda delante de la #10 (07/12 al 11/03), registrada después")
    void calcularPosicionEnCola_laDuracionDeLaPlanificacionNoInterviene() {
        // #8: del 15/10/2026 al 31/01/2027 (108 días); #10: del 07/12/2026 al 11/03/2027 (94 días). Solo cuenta el fin
        CriterioDeCola planificacion8 = new CriterioDeCola(MEDIA, LocalDate.of(2027, 1, 31));
        CriterioDeCola planificacion10 = new CriterioDeCola(MEDIA, LocalDate.of(2027, 3, 11));
        // La #10 dura menos (94 días contra 108) pero termina después: va detrás de los lotes de la #8
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(planificacion8, planificacion8), planificacion10)).isEqualTo(2);
        // Y un lote nuevo de la #8 pasa por delante de los de la #10
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(planificacion10), planificacion8)).isEqualTo(0);
    }

    @Test
    @DisplayName("CP-MCR-10: con empate total el lote nuevo queda después de los existentes")
    void calcularPosicionEnCola_empateTotalVaDespues() {
        CriterioDeCola igual = f(ALTA, 20);
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(igual, igual), igual)).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-MCR-11: la prioridad estricta manda sobre la fecha de finalización: un lote BAJA que termina antes va detrás de uno MEDIA que termina mucho después")
    void calcularPosicionEnCola_laPrioridadManda() {
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(f(MEDIA, 400)), f(BAJA, 1))).isEqualTo(1);
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(f(BAJA, 1)), f(MEDIA, 400))).isEqualTo(0);
    }

    @Test
    @DisplayName("CP-MCR-12: en una cola reordenada a mano la posición es la siguiente al último lote que le gana o empata")
    void calcularPosicionEnCola_colaManualUsaElUltimoQueLeGanaOEmpata() {
        // Cola MEDIA que termina en +10, +30, +10 y lote nuevo en +20: le gana el último, así que va al final
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(f(MEDIA, 10), f(MEDIA, 30), f(MEDIA, 10)), f(MEDIA, 20))).isEqualTo(3);
        // Cola +30, +30, +10: solo el último le gana, el nuevo va al final aunque los dos primeros le pierdan
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(f(MEDIA, 30), f(MEDIA, 30), f(MEDIA, 10)), f(MEDIA, 20))).isEqualTo(3);
        // Cola +30, +10, +30: el nuevo va justo después del +10
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(f(MEDIA, 30), f(MEDIA, 10), f(MEDIA, 30)), f(MEDIA, 20))).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-MCR-13: a igual prioridad, una planificación sin fecha de finalización estimada va detrás de las que la tienen")
    void calcularPosicionEnCola_sinFechaDeFinalizacionVaUltima() {
        CriterioDeCola sinFin = f(MEDIA, null);
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(sinFin), f(MEDIA, 10))).isEqualTo(0);
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(f(MEDIA, 10)), sinFin)).isEqualTo(1);
        // Dos sin fecha empatan por completo: el que llegó primero va primero
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(sinFin), sinFin)).isEqualTo(1);
    }
}
