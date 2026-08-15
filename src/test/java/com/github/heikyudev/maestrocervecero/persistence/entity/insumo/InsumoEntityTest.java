package com.github.heikyudev.maestrocervecero.persistence.entity.insumo;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import org.hibernate.annotations.SoftDelete;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el contrato estructural de {@link InsumoEntity} exigido por el diseño
 * de gestión de insumos: la columna discriminadora {@code tipo_insumo} que habilita
 * el índice único parcial (LOWER(nombre), tipo_insumo) solo para registros activos,
 * sin romper la herencia JOINED ni el soft-delete.
 */
class InsumoEntityTest {

    @Test
    @DisplayName("InsumoEntity declara @DiscriminatorColumn 'tipo_insumo' para identificar el tipo sin JOIN")
    void insumoEntity_debeDeclararDiscriminatorColumnTipoInsumo() {
        DiscriminatorColumn discriminatorColumn =
                InsumoEntity.class.getAnnotation(DiscriminatorColumn.class);

        assertThat(discriminatorColumn)
                .as("InsumoEntity debe declarar @DiscriminatorColumn")
                .isNotNull();
        assertThat(discriminatorColumn.name()).isEqualTo("tipo_insumo");
    }

    @Test
    @DisplayName("InsumoEntity conserva la herencia JOINED y el soft-delete junto al discriminador")
    void insumoEntity_debeConservarHerenciaJoinedYSoftDelete() {
        Inheritance inheritance = InsumoEntity.class.getAnnotation(Inheritance.class);
        SoftDelete softDelete = InsumoEntity.class.getAnnotation(SoftDelete.class);

        assertThat(inheritance).isNotNull();
        assertThat(inheritance.strategy()).isEqualTo(InheritanceType.JOINED);
        assertThat(softDelete)
                .as("InsumoEntity debe conservar @SoftDelete (el índice parcial filtra por deleted = false)")
                .isNotNull();
    }
}
