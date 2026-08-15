package com.github.heikyudev.maestrocervecero.persistence.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato del script DDL {@code V2__add_tipo_insumo_unique_index.sql}.
 * <p>
 * El proyecto no usa herramienta de migraciones (Flyway/Liquibase); el schema lo genera
 * Hibernate con {@code ddl-auto=create-drop}. Este test protege las propiedades críticas
 * del script de ejecución manual que el diseño exige para entornos con datos existentes:
 * la columna discriminadora y el índice único parcial (case-insensitive, solo activos).
 */
class TipoInsumoMigrationScriptTest {

    private static final String SCRIPT_PATH = "db/migration/V2__add_tipo_insumo_unique_index.sql";

    @Test
    @DisplayName("El script V2 existe y agrega la columna discriminadora tipo_insumo")
    void scriptV2_debeExistirYAgregarColumnaTipoInsumo() throws IOException {
        String sql = cargarScriptNormalizado();

        assertThat(sql).contains("alter table insumo add column tipo_insumo varchar(31);");
    }

    @Test
    @DisplayName("El script V2 crea el índice único parcial por nombre y tipo solo para activos")
    void scriptV2_debeCrearIndiceUnicoParcialPorNombreYTipo() throws IOException {
        String sql = cargarScriptNormalizado();

        assertThat(sql).contains(
                "create unique index udx_insumo_nombre_tipo on insumo (lower(nombre), tipo_insumo)");
        assertThat(sql).contains("where deleted = false");
    }

    private String cargarScriptNormalizado() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(SCRIPT_PATH)) {
            assertThat(input)
                    .as("El script %s debe existir en el classpath", SCRIPT_PATH)
                    .isNotNull();
            String contenido = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return contenido.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        }
    }
}
