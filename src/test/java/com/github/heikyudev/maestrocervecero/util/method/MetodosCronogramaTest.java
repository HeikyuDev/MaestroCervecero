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

    /** Criterio con margen de 30 días y fin 31/07/2027: igual para todos, así solo cuenta la prioridad. */
    private static CriterioDeCola p(PrioridadPlanificacion prioridad) {
        return new CriterioDeCola(prioridad, INICIO, INICIO.plusDays(30));
    }

    /** Criterio con inicio 01/07/2027 y el margen indicado. */
    private static CriterioDeCola m(PrioridadPlanificacion prioridad, int margen) {
        return new CriterioDeCola(prioridad, INICIO, INICIO.plusDays(margen));
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
    @DisplayName("CP-MCR-08: a igual prioridad, el menor margen (fin - inicio de la planificación) va primero")
    void calcularPosicionEnCola_menorMargenVaPrimero() {
        // Cola MEDIA con márgenes 10 y 20: un lote con margen 15 va entre los dos; con 5, primero; con 25, último
        List<CriterioDeCola> cola = List.of(m(MEDIA, 10), m(MEDIA, 20));
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola, m(MEDIA, 15))).isEqualTo(1);
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola, m(MEDIA, 5))).isEqualTo(0);
        assertThat(MetodosCronograma.calcularPosicionEnCola(cola, m(MEDIA, 25))).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-MCR-09: a igual prioridad y margen, la fecha de finalización más temprana va primero")
    void calcularPosicionEnCola_mismoMargenGanaLaFinMasTemprana() {
        // Los dos con margen 20: fin 21/07/2027 (inicio 01/07) y fin 25/07/2027 (inicio 05/07)
        CriterioDeCola finTemprano = new CriterioDeCola(MEDIA, LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 21));
        CriterioDeCola finTardio = new CriterioDeCola(MEDIA, LocalDate.of(2027, 7, 5), LocalDate.of(2027, 7, 25));
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(finTardio), finTemprano)).isEqualTo(0);
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(finTemprano), finTardio)).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-MCR-10: con empate total el lote nuevo queda después de los existentes")
    void calcularPosicionEnCola_empateTotalVaDespues() {
        CriterioDeCola igual = new CriterioDeCola(ALTA, LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 21));
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(igual, igual), igual)).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-MCR-11: la prioridad estricta manda sobre el margen: un lote BAJA con margen mínimo va detrás de uno MEDIA con margen enorme")
    void calcularPosicionEnCola_laPrioridadManda() {
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(m(MEDIA, 400)), m(BAJA, 1))).isEqualTo(1);
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(m(BAJA, 1)), m(MEDIA, 400))).isEqualTo(0);
    }

    @Test
    @DisplayName("CP-MCR-12: en una cola reordenada a mano la posición es la siguiente al último lote que le gana o empata")
    void calcularPosicionEnCola_colaManualUsaElUltimoQueLeGanaOEmpata() {
        // Cola MEDIA con márgenes 10, 30, 10 y lote nuevo con margen 20: gana el último (10), así que va al final
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(m(MEDIA, 10), m(MEDIA, 30), m(MEDIA, 10)), m(MEDIA, 20))).isEqualTo(3);
        // Cola MEDIA con márgenes 30, 30, 10: solo el último le gana, el nuevo (20) va al final aunque los dos primeros le pierdan
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(m(MEDIA, 30), m(MEDIA, 30), m(MEDIA, 10)), m(MEDIA, 20))).isEqualTo(3);
        // Cola MEDIA con márgenes 30, 10, 30: el nuevo (20) va justo después del 10
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(m(MEDIA, 30), m(MEDIA, 10), m(MEDIA, 30)), m(MEDIA, 20))).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-MCR-13: el margen de una planificación sin alguna de sus fechas es el mayor posible y nunca le gana a una con fechas")
    void margenEnDias_sinFechasEsElMayor() {
        assertThat(new CriterioDeCola(MEDIA, null, INICIO).margenEnDias()).isEqualTo(Long.MAX_VALUE);
        assertThat(new CriterioDeCola(MEDIA, INICIO, INICIO.plusDays(12)).margenEnDias()).isEqualTo(12);
        assertThat(MetodosCronograma.calcularPosicionEnCola(List.of(new CriterioDeCola(MEDIA, null, null)), m(MEDIA, 10))).isEqualTo(0);
    }
}
