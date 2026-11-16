package com.github.heikyudev.maestrocervecero.persistence.repository.estadisticas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija el universo de las consultas de producción que definen una regla de los reportes, sin base de datos: lo que se
 * verifica es el criterio que escribe cada consulta.
 */
class ConsultasEstadisticaProduccionTest {

    private static String consulta(String metodo) {
        Method method = Arrays.stream(IEstadisticaProduccionRepository.class.getMethods())
                .filter(m -> m.getName().equals(metodo))
                .findFirst().orElseThrow();
        return method.getAnnotation(Query.class).value();
    }

    @Test
    @DisplayName("CP-ESP-27: las etapas de Equipamiento incluyen las de un lote cancelado que ya habían iniciado (uso real hasta la cancelación) y excluyen las que nunca iniciaron")
    void equipamiento_incluyeEtapasCanceladasIniciadas() {
        String jpql = consulta("buscarEtapasDelPeriodo");

        assertThat(jpql).contains("et.estado IN ('FINALIZADA', 'CANCELADA')").contains("et.fechaFinalizacion >= :inicio")
                .contains("et.fechaInicio IS NOT NULL").contains("et.fechaInicio < :finExclusivo");
    }

    @Test
    @DisplayName("CP-ESP-28: las etapas de Tiempos son las FINALIZADAS cuya fecha de finalización cae en el período, aunque hayan empezado antes")
    void tiempos_universoPorFechaDeFinalizacion() {
        String jpql = consulta("buscarEtapasFinalizadas");

        assertThat(jpql).contains("et.estado = 'FINALIZADA'").contains("et.fechaFinalizacion >= :inicio")
                .contains("et.fechaFinalizacion < :finExclusivo").doesNotContain("et.fechaInicio >=").doesNotContain("CANCELADA");
    }
}
