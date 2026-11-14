package com.github.heikyudev.maestrocervecero.util.auditoria;

import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.CampoAuditoriaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.EstadoCampoAuditoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComparadorEstadosAuditoriaTest {

    private final ComparadorEstadosAuditoria comparador = new ComparadorEstadosAuditoria(JsonMapper.builder().build());

    @Test
    @DisplayName("CP-CE-01: comparar marca como modificados los campos que cambiaron y como sin cambios los que quedaron igual, en el orden guardado")
    void comparar_debeMarcarModificadosYSinCambios() {
        var resultado = comparador.comparar(
                "{\"id\":5,\"nombre\":\"Pilsen\",\"potencialExtracto\":80}",
                "{\"id\":5,\"nombre\":\"Pilsen Premium\",\"potencialExtracto\":80}");

        assertThat(resultado.tieneDetalle()).isTrue();
        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getCampo)
                .containsExactly("ID", "Nombre", "Potencial extracto");
        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getEstado)
                .containsExactly(EstadoCampoAuditoria.SIN_CAMBIOS, EstadoCampoAuditoria.MODIFICADO, EstadoCampoAuditoria.SIN_CAMBIOS);
        CampoAuditoriaResponseDTO nombre = resultado.campos().get(1);
        assertThat(nombre.getValorAnterior()).isEqualTo("Pilsen");
        assertThat(nombre.getValorPosterior()).isEqualTo("Pilsen Premium");
    }

    @Test
    @DisplayName("CP-CE-02: comparar de un alta (sin estado anterior) marca todos los campos como agregados con el valor anterior vacío")
    void comparar_deUnAltaMarcaTodoComoAgregado() {
        var resultado = comparador.comparar(null, "{\"id\":9,\"nombre\":\"Cascade\"}");

        assertThat(resultado.tieneDetalle()).isTrue();
        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getEstado)
                .containsOnly(EstadoCampoAuditoria.AGREGADO);
        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getValorAnterior).containsOnlyNulls();
        assertThat(resultado.campos().get(1).getValorPosterior()).isEqualTo("Cascade");
    }

    @Test
    @DisplayName("CP-CE-03: comparar sin estado posterior marca todos los campos como eliminados")
    void comparar_sinPosteriorMarcaTodoComoEliminado() {
        var resultado = comparador.comparar("{\"id\":9,\"nombre\":\"Cascade\"}", null);

        assertThat(resultado.tieneDetalle()).isTrue();
        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getEstado)
                .containsOnly(EstadoCampoAuditoria.ELIMINADO);
        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getValorPosterior).containsOnlyNulls();
    }

    @Test
    @DisplayName("CP-CE-04: comparar detecta un campo que solo existe en el estado posterior (agregado) y uno que solo existe en el anterior (eliminado)")
    void comparar_debeDetectarCamposAgregadosYEliminados() {
        var resultado = comparador.comparar("{\"id\":1,\"viejo\":\"x\"}", "{\"id\":1,\"nuevo\":\"y\"}");

        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getCampo).containsExactly("ID", "Viejo", "Nuevo");
        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getEstado)
                .containsExactly(EstadoCampoAuditoria.SIN_CAMBIOS, EstadoCampoAuditoria.ELIMINADO, EstadoCampoAuditoria.AGREGADO);
    }

    @Test
    @DisplayName("CP-CE-05: comparar distingue un valor null de un campo ausente y trata null contra un valor como modificado")
    void comparar_debeTratarNullComoValorVacio() {
        var resultado = comparador.comparar("{\"telefono\":null,\"correo\":null}", "{\"telefono\":\"1155\",\"correo\":null}");

        assertThat(resultado.campos().get(0).getEstado()).isEqualTo(EstadoCampoAuditoria.MODIFICADO);
        assertThat(resultado.campos().get(0).getValorAnterior()).isNull();
        assertThat(resultado.campos().get(0).getValorPosterior()).isEqualTo("1155");
        assertThat(resultado.campos().get(1).getEstado()).isEqualTo(EstadoCampoAuditoria.SIN_CAMBIOS);
    }

    @Test
    @DisplayName("CP-CE-06: comparar muestra los booleanos como Sí / No y las fechas como dd/MM/yyyy y dd/MM/yyyy HH:mm:ss")
    void comparar_debeFormatearBooleanosYFechas() {
        var resultado = comparador.comparar(
                "{\"activo\":true,\"fecha\":\"2026-11-13\",\"fechaHora\":\"2026-11-13T10:30:15\"}",
                "{\"activo\":false,\"fecha\":\"2026-11-14\",\"fechaHora\":\"2026-11-13T10:30\"}");

        assertThat(resultado.campos().get(0).getValorAnterior()).isEqualTo("Sí");
        assertThat(resultado.campos().get(0).getValorPosterior()).isEqualTo("No");
        assertThat(resultado.campos().get(1).getValorAnterior()).isEqualTo("13/11/2026");
        assertThat(resultado.campos().get(1).getValorPosterior()).isEqualTo("14/11/2026");
        assertThat(resultado.campos().get(2).getValorAnterior()).isEqualTo("13/11/2026 10:30:15");
        assertThat(resultado.campos().get(2).getValorPosterior()).isEqualTo("13/11/2026 10:30:00");
    }

    @Test
    @DisplayName("CP-CE-07: comparar muestra los números con el formato de la aplicación, los identificadores sin separador de miles y un texto que parece fecha pero no lo es, sin cambios")
    void comparar_debeMostrarNumerosYTextosTalCual() {
        var resultado = comparador.comparar(
                "{\"cantidad\":12.5,\"codigo\":\"2026-13-45\",\"stock\":2000.0,\"id\":1234,\"fabricanteId\":5678}",
                "{\"cantidad\":13,\"codigo\":\"2026-13-45\",\"stock\":1847.3685,\"id\":1234,\"fabricanteId\":5678}");

        assertThat(resultado.campos().get(0).getValorAnterior()).isEqualTo("12,5");
        assertThat(resultado.campos().get(0).getValorPosterior()).isEqualTo("13");
        assertThat(resultado.campos().get(1).getValorAnterior()).isEqualTo("2026-13-45");
        assertThat(resultado.campos().get(1).getEstado()).isEqualTo(EstadoCampoAuditoria.SIN_CAMBIOS);
        assertThat(resultado.campos().get(2).getValorAnterior()).isEqualTo("2.000");
        assertThat(resultado.campos().get(2).getValorPosterior()).isEqualTo("1.847,3685");
        assertThat(resultado.campos().get(3).getValorAnterior()).isEqualTo("1234");
        assertThat(resultado.campos().get(4).getValorAnterior()).isEqualTo("5678");
    }

    @Test
    @DisplayName("CP-CE-12: comparar trata un campo con null en los dos estados como sin cambios y un campo que existe en un solo estado, aunque valga null, como agregado o eliminado")
    void comparar_debeDistinguirNullDeCampoAusente() {
        var resultado = comparador.comparar("{\"a\":null,\"b\":null}", "{\"a\":null,\"c\":null}");

        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getCampo).containsExactly("A", "B", "C");
        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getEstado)
                .containsExactly(EstadoCampoAuditoria.SIN_CAMBIOS, EstadoCampoAuditoria.ELIMINADO, EstadoCampoAuditoria.AGREGADO);
        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getValorAnterior).containsOnlyNulls();
        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getValorPosterior).containsOnlyNulls();
    }

    @Test
    @DisplayName("CP-CE-13: comparar considera iguales dos números de igual valor aunque sean de distinto tipo (1 y 1.0) y distintos si el valor difiere")
    void comparar_debeCompararNumerosPorValor() {
        var resultado = comparador.comparar("{\"a\":1,\"b\":2.50,\"c\":1}", "{\"a\":1.0,\"b\":2.5,\"c\":1.5}");

        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getEstado)
                .containsExactly(EstadoCampoAuditoria.SIN_CAMBIOS, EstadoCampoAuditoria.SIN_CAMBIOS, EstadoCampoAuditoria.MODIFICADO);
        assertThat(resultado.campos().get(0).getValorPosterior()).isEqualTo("1");
        assertThat(resultado.campos().get(2).getValorPosterior()).isEqualTo("1,5");
    }

    @Test
    @DisplayName("CP-CE-08: un JSON ausente, vacío, inválido o que no es un objeto se trata como un estado inexistente, sin lanzar excepción")
    void comparar_debeDegradarConJsonInvalido() {
        for (String invalido : new String[]{null, "", "   ", "{no es json", "[1,2,3]", "\"texto\"", "42"}) {
            var resultado = comparador.comparar(invalido, invalido);

            assertThat(resultado.tieneDetalle()).as("JSON: " + invalido).isFalse();
            assertThat(resultado.campos()).as("JSON: " + invalido).isEmpty();
        }
    }

    @Test
    @DisplayName("CP-CE-09: si solo uno de los dos JSON es legible, compara contra un estado inexistente")
    void comparar_conUnSoloJsonLegible() {
        var resultado = comparador.comparar("{rota", "{\"id\":2,\"nombre\":\"Ana\"}");

        assertThat(resultado.tieneDetalle()).isTrue();
        assertThat(resultado.campos()).extracting(CampoAuditoriaResponseDTO::getEstado).containsOnly(EstadoCampoAuditoria.AGREGADO);
    }

    @Test
    @DisplayName("CP-CE-10: la comparación expone solo si hay detalle y los campos comparados")
    void comparar_soloExponeDetalleYCampos() {
        var resultado = comparador.comparar("{\"id\":1}", "{\"id\":2}");

        assertThat(ComparadorEstadosAuditoria.Comparacion.class.getRecordComponents()).extracting(c -> c.getName())
                .containsExactly("tieneDetalle", "campos");
        assertThat(resultado.campos()).hasSize(1);
    }

    @Test
    @DisplayName("CP-CE-11: la etiqueta de un campo separa las palabras, traduce los campos de auditoría de fila y marca los ids de asociaciones")
    void etiqueta_debeSerLegible() {
        assertThat(ComparadorEstadosAuditoria.etiqueta("potencialExtracto")).isEqualTo("Potencial extracto");
        assertThat(ComparadorEstadosAuditoria.etiqueta("fabricanteId")).isEqualTo("Fabricante (ID)");
        assertThat(ComparadorEstadosAuditoria.etiqueta("idEtapaLote")).isEqualTo("Id etapa lote");
        assertThat(ComparadorEstadosAuditoria.etiqueta("id")).isEqualTo("ID");
        assertThat(ComparadorEstadosAuditoria.etiqueta("createdBy")).isEqualTo("Creado por");
        assertThat(ComparadorEstadosAuditoria.etiqueta("lastModifiedDate")).isEqualTo("Fecha de modificación");
        assertThat(ComparadorEstadosAuditoria.etiqueta("nombre")).isEqualTo("Nombre");
        assertThat(List.of(ComparadorEstadosAuditoria.etiqueta(" "))).containsExactly(" ");
    }
}
