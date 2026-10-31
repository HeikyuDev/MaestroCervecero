package com.github.heikyudev.maestrocervecero.util.method.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MetodosCicloVidaEquipamientoTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 1, 15, 10, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 1, 15, 12, 0);

    // ==================== validarObservaciones ====================

    @Test
    @DisplayName("CP-MCV-01: validarObservaciones devuelve las observaciones normalizadas")
    void validarObservaciones_debeNormalizar() {
        // === EJECUCION ===
        String resultado = MetodosCicloVidaEquipamiento.validarObservaciones("  Ruido   anormal  ");

        // === ASSERTS ===
        assertThat(resultado).isEqualTo("Ruido anormal");
    }

    @Test
    @DisplayName("CP-MCV-02: validarObservaciones rechaza observaciones nulas o en blanco")
    void validarObservaciones_debeRechazarNulasOEnBlanco() {
        // === EJECUCION Y ASSERTS ===
        for (String invalida : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarObservaciones(invalida))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("Las observaciones son obligatorias")
                    .extracting("campo").isEqualTo("observaciones");
        }
    }

    @Test
    @DisplayName("CP-MCV-03: validarObservaciones acepta 255 caracteres y rechaza 256")
    void validarObservaciones_debeRespetarElMaximoDe255() {
        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> MetodosCicloVidaEquipamiento.validarObservaciones("a".repeat(255))).doesNotThrowAnyException();
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarObservaciones("a".repeat(256)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El texto de las observaciones no puede superar 255 caracteres")
                .extracting("campo").isEqualTo("observaciones");
    }

    // ==================== validarMotivoAnulacion ====================

    @Test
    @DisplayName("CP-MCV-04: validarMotivoAnulacion rechaza un motivo nulo o en blanco")
    void validarMotivoAnulacion_debeRechazarNuloOEnBlanco() {
        // === EJECUCION Y ASSERTS ===
        for (String invalido : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarMotivoAnulacion(invalido))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El motivo de anulación es obligatorio")
                    .extracting("campo").isEqualTo("motivoAnulacion");
        }
    }

    @Test
    @DisplayName("CP-MCV-05: validarMotivoAnulacion acepta 255 caracteres, rechaza 256 y normaliza el texto")
    void validarMotivoAnulacion_debeRespetarElMaximoDe255() {
        // === EJECUCION Y ASSERTS ===
        assertThat(MetodosCicloVidaEquipamiento.validarMotivoAnulacion("  Carga   por error ")).isEqualTo("Carga por error");
        assertThatCode(() -> MetodosCicloVidaEquipamiento.validarMotivoAnulacion("a".repeat(255))).doesNotThrowAnyException();
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarMotivoAnulacion("a".repeat(256)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres")
                .extracting("campo").isEqualTo("motivoAnulacion");
    }

    // ==================== validarFecha ====================

    @Test
    @DisplayName("CP-MCV-06: validarFecha acepta una fecha pasada y la fecha y hora actual")
    void validarFecha_debeAceptarFechaValida() {
        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> MetodosCicloVidaEquipamiento.validarFecha(INICIO)).doesNotThrowAnyException();
        assertThatCode(() -> MetodosCicloVidaEquipamiento.validarFecha(LocalDateTime.now().minusSeconds(1))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-MCV-07: validarFecha rechaza una fecha nula")
    void validarFecha_debeRechazarFechaNula() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarFecha(null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación es obligatoria")
                .extracting("campo").isEqualTo("fecha");
    }

    @Test
    @DisplayName("CP-MCV-08: validarFecha rechaza una fecha posterior a la fecha y hora actual")
    void validarFecha_debeRechazarFechaFutura() {
        // === PREPARACION DE DATOS ===
        LocalDateTime futuro = LocalDateTime.now().plusMinutes(5);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarFecha(futuro))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación no puede ser posterior a la fecha y hora actual")
                .extracting("campo").isEqualTo("fecha");
    }

    // ==================== validarFechaPosteriorALaActividadAnterior ====================

    @Test
    @DisplayName("CP-MCV-11: validarFechaPosteriorALaActividadAnterior acepta una fecha posterior a todas las actividades anteriores o sin actividades")
    void validarFechaPosterior_debeAceptarFechaPosterior() {
        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> MetodosCicloVidaEquipamiento.validarFechaPosteriorALaActividadAnterior(INICIO, INICIO.minusSeconds(1), INICIO.minusDays(3))).doesNotThrowAnyException();
        assertThatCode(() -> MetodosCicloVidaEquipamiento.validarFechaPosteriorALaActividadAnterior(INICIO, null, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-MCV-12: validarFechaPosteriorALaActividadAnterior rechaza una fecha igual a la de una actividad anterior")
    void validarFechaPosterior_debeRechazarFechaIgual() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarFechaPosteriorALaActividadAnterior(INICIO, INICIO, null))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación debe ser posterior a la última operación registrada o al último uso en un lote del equipamiento")
                .extracting("campo").isEqualTo("fecha");
    }

    @Test
    @DisplayName("CP-MCV-13: validarFechaPosteriorALaActividadAnterior rechaza una fecha anterior a la de cualquiera de las actividades anteriores")
    void validarFechaPosterior_debeRechazarFechaAnteriorEnCualquieraDeLasActividades() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarFechaPosteriorALaActividadAnterior(INICIO, INICIO.minusDays(1), INICIO.plusHours(1)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de la operación debe ser posterior a la última operación registrada o al último uso en un lote del equipamiento");
    }

    // ==================== validarEsLaUltimaOperacion ====================

    @Test
    @DisplayName("CP-MCV-14: validarEsLaUltimaOperacion acepta la operación cuyo ID es el de la última, o cuando no hay operaciones")
    void validarEsLaUltimaOperacion_debeAceptarLaUltima() {
        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> MetodosCicloVidaEquipamiento.validarEsLaUltimaOperacion(5L, 5L)).doesNotThrowAnyException();
        assertThatCode(() -> MetodosCicloVidaEquipamiento.validarEsLaUltimaOperacion(5L, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-MCV-15: validarEsLaUltimaOperacion rechaza la operación cuando la última es otra (incluido un empate de fecha de fin desempatado por ID)")
    void validarEsLaUltimaOperacion_debeRechazarSiOtraTerminoDespues() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarEsLaUltimaOperacion(5L, 6L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del equipamiento; anule primero las operaciones posteriores");
    }

    // ==================== resolverEquipamientoActivo ====================

    @Test
    @DisplayName("CP-MCV-16: resolverEquipamientoActivo lanza RecursoNoEncontradoException cuando el equipamiento no existe")
    void resolverEquipamientoActivo_debeRechazarInexistente() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.resolverEquipamientoActivo(Optional.empty(), 99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el equipamiento con ID: 99")
                .extracting("campo").isEqualTo("idEquipamiento");
    }

    @Test
    @DisplayName("CP-MCV-17: resolverEquipamientoActivo lanza ReglaNegocioException con un mensaje propio cuando el equipamiento está dado de baja")
    void resolverEquipamientoActivo_debeRechazarDadoDeBaja() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = macerador(EstadoOperativo.DISPONIBLE);
        macerador.setEstado(Estado.BAJA);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.resolverEquipamientoActivo(Optional.of(macerador), 1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El equipamiento está dado de baja")
                .extracting("campo").isEqualTo("idEquipamiento");
    }

    @Test
    @DisplayName("CP-MCV-18: resolverEquipamientoActivo devuelve el equipamiento activo")
    void resolverEquipamientoActivo_debeDevolverElEquipamientoActivo() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity macerador = macerador(EstadoOperativo.DISPONIBLE);

        // === EJECUCION ===
        EquipamientoEntity resultado = MetodosCicloVidaEquipamiento.resolverEquipamientoActivo(Optional.of(macerador), 1L);

        // === ASSERTS ===
        assertThat(resultado).isSameAs(macerador);
    }

    // ==================== validarEstadoOperativoParaRegistrar ====================

    @Test
    @DisplayName("CP-MCV-19: validarEstadoOperativoParaRegistrar acepta el estado operativo que exige la operación")
    void validarEstado_debeAceptarElEstadoRequerido() {
        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> MetodosCicloVidaEquipamiento.validarEstadoOperativoParaRegistrar(EstadoOperativo.EN_LIMPIEZA, EstadoOperativo.EN_LIMPIEZA, "una limpieza")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-MCV-20: validarEstadoOperativoParaRegistrar rechaza toda operación manual sobre un equipamiento en uso")
    void validarEstado_debeRechazarEquipamientoEnUso() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarEstadoOperativoParaRegistrar(EstadoOperativo.EN_USO, EstadoOperativo.DISPONIBLE, "una falla"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede registrar una falla porque el equipamiento está en uso")
                .extracting("campo").isEqualTo("idEquipamiento");
    }

    @Test
    @DisplayName("CP-MCV-21: validarEstadoOperativoParaRegistrar orienta a registrar primero la limpieza cuando el equipamiento está en limpieza")
    void validarEstado_debeOrientarALaLimpieza() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarEstadoOperativoParaRegistrar(EstadoOperativo.EN_LIMPIEZA, EstadoOperativo.DISPONIBLE, "una falla"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Para registrar una falla, primero registre la limpieza del equipamiento");
    }

    @Test
    @DisplayName("CP-MCV-22: validarEstadoOperativoParaRegistrar orienta a registrar primero el mantenimiento cuando el equipamiento está en mantenimiento")
    void validarEstado_debeOrientarAlMantenimiento() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarEstadoOperativoParaRegistrar(EstadoOperativo.EN_MANTENIMIENTO, EstadoOperativo.EN_LIMPIEZA, "una limpieza"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Para registrar una limpieza, primero registre el mantenimiento del equipamiento");
    }

    @Test
    @DisplayName("CP-MCV-23: validarEstadoOperativoParaRegistrar rechaza una operación que no corresponde a un equipamiento disponible")
    void validarEstado_debeRechazarEquipamientoDisponible() {
        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> MetodosCicloVidaEquipamiento.validarEstadoOperativoParaRegistrar(EstadoOperativo.DISPONIBLE, EstadoOperativo.EN_MANTENIMIENTO, "un mantenimiento"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede registrar un mantenimiento porque el equipamiento está disponible");
    }

    private static MaceradorEntity macerador(EstadoOperativo estadoOperativo) {
        return MaceradorEntity.builder()
                .id(1L)
                .identificadorInterno("MAC-01")
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .estado(Estado.ACTIVO)
                .capacidadTotal(500.0)
                .capacidadUtil(450.0)
                .espacioMuerto(10.0)
                .eficienciaMaceracion(0.75)
                .build();
    }
}
