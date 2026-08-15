package com.github.heikyudev.maestrocervecero.persistence.entity.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato del enum {@link ConceptoAuditoria} para la gestión de insumos:
 * los conceptos MALTA, LUPULO y LEVADURA deben existir para que los servicios
 * puedan auditar CREAR/MODIFICAR/ELIMINAR por tipo de insumo, y los conceptos
 * preexistentes deben conservarse intactos.
 */
class ConceptoAuditoriaTest {

    @Test
    @DisplayName("ConceptoAuditoria resuelve los conceptos MALTA, LUPULO y LEVADURA")
    void conceptoAuditoria_debeResolverConceptosDeInsumos() {
        assertThat(ConceptoAuditoria.valueOf("MALTA").name()).isEqualTo("MALTA");
        assertThat(ConceptoAuditoria.valueOf("LUPULO").name()).isEqualTo("LUPULO");
        assertThat(ConceptoAuditoria.valueOf("LEVADURA").name()).isEqualTo("LEVADURA");
    }

    @Test
    @DisplayName("ConceptoAuditoria conserva los conceptos existentes y suma exactamente 3 nuevos")
    void conceptoAuditoria_debeConservarConceptosExistentes() {
        assertThat(ConceptoAuditoria.values())
                .extracting(ConceptoAuditoria::name)
                .contains("SESION", "INSUMO", "USUARIO", "MALTA", "LUPULO", "LEVADURA")
                .hasSize(31);
    }
}
