package com.github.heikyudev.maestrocervecero.service.aspect;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.service.aspect.RegistroEstadosEntidad.EstadosCapturados;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegistroEstadosEntidadTest {

    private final RegistroEstadosEntidad registro = new RegistroEstadosEntidad();

    @AfterEach
    void cerrar() {
        registro.finalizar();
    }

    private static Map<String, Object> estado(Object... paresClaveValor) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < paresClaveValor.length; i += 2) {
            mapa.put((String) paresClaveValor[i], paresClaveValor[i + 1]);
        }
        return mapa;
    }

    private static MaltaEntity malta(Long id) {
        MaltaEntity malta = new MaltaEntity();
        malta.setId(id);
        return malta;
    }

    @Test
    @DisplayName("CP-RE-01: sin captura abierta no registra nada y resolver devuelve vacío")
    void sinCapturaAbierta_noRegistraNada() {
        MaltaEntity malta = malta(5L);

        registro.registrarModificacion(malta, estado("nombre", "A"), estado("nombre", "B"));

        assertThat(registro.activo()).isFalse();
        assertThat(registro.resolver(ConceptoAuditoria.MALTA, "5")).isEmpty();
    }

    @Test
    @DisplayName("CP-RE-02: iniciar abre la captura solo la primera vez y finalizar la cierra y descarta lo acumulado")
    void iniciarYFinalizar() {
        assertThat(registro.iniciar(ConceptoAuditoria.MALTA)).isTrue();
        assertThat(registro.iniciar(ConceptoAuditoria.MALTA)).isFalse();
        registro.registrarAlta(malta(5L), estado("nombre", "A"));

        registro.finalizar();

        assertThat(registro.activo()).isFalse();
        assertThat(registro.iniciar(ConceptoAuditoria.MALTA)).isTrue();
        assertThat(registro.resolver(ConceptoAuditoria.MALTA, "5")).isEmpty();
    }

    @Test
    @DisplayName("CP-RE-03: una modificación devuelve el estado anterior y el posterior con el id como primer campo")
    void modificacion_devuelveAnteriorYPosterior() {
        registro.iniciar(ConceptoAuditoria.MALTA);
        registro.registrarModificacion(malta(5L), estado("nombre", "Pilsen", "potencial", 80), estado("nombre", "Pilsen 2", "potencial", 80));

        Optional<EstadosCapturados> estados = registro.resolver(ConceptoAuditoria.MALTA, "5");

        assertThat(estados).isPresent();
        assertThat(estados.get().anterior()).containsExactly(Map.entry("id", 5L), Map.entry("nombre", "Pilsen"), Map.entry("potencial", 80));
        assertThat(estados.get().posterior()).containsExactly(Map.entry("id", 5L), Map.entry("nombre", "Pilsen 2"), Map.entry("potencial", 80));
    }

    @Test
    @DisplayName("CP-RE-04: un alta solo tiene estado posterior y el id se toma de la entidad ya insertada")
    void alta_soloTienePosterior() {
        MaltaEntity nueva = malta(null);
        registro.iniciar(ConceptoAuditoria.MALTA);
        registro.registrarAlta(nueva, estado("nombre", "Cascade"));
        nueva.setId(9L);

        EstadosCapturados estados = registro.resolver(ConceptoAuditoria.MALTA, "9").orElseThrow();

        assertThat(estados.anterior()).isNull();
        assertThat(estados.posterior()).containsEntry("id", 9L).containsEntry("nombre", "Cascade");
    }

    @Test
    @DisplayName("CP-RE-05: una baja física solo tiene estado anterior")
    void baja_soloTieneAnterior() {
        registro.iniciar(ConceptoAuditoria.MALTA);
        registro.registrarBaja(malta(5L), estado("nombre", "Pilsen"));

        EstadosCapturados estados = registro.resolver(ConceptoAuditoria.MALTA, "5").orElseThrow();

        assertThat(estados.anterior()).containsEntry("nombre", "Pilsen");
        assertThat(estados.posterior()).isNull();
    }

    @Test
    @DisplayName("CP-RE-06: varios cambios de la misma entidad se resumen en el anterior del primero y el posterior del último")
    void variosCambios_seResumen() {
        MaltaEntity malta = malta(5L);
        registro.iniciar(ConceptoAuditoria.MALTA);
        registro.registrarModificacion(malta, estado("cantidad", 1), estado("cantidad", 2));
        registro.registrarModificacion(malta, estado("cantidad", 2), estado("cantidad", 3));

        EstadosCapturados estados = registro.resolver(ConceptoAuditoria.MALTA, "5").orElseThrow();

        assertThat(estados.anterior()).containsEntry("cantidad", 1);
        assertThat(estados.posterior()).containsEntry("cantidad", 3);
    }

    @Test
    @DisplayName("CP-RE-07: un alta seguida de una modificación conserva que no hubo estado anterior")
    void altaYModificacion_conservanSinAnterior() {
        MaltaEntity malta = malta(5L);
        registro.iniciar(ConceptoAuditoria.MALTA);
        registro.registrarAlta(malta, estado("cantidad", 1));
        registro.registrarModificacion(malta, estado("cantidad", 1), estado("cantidad", 4));

        EstadosCapturados estados = registro.resolver(ConceptoAuditoria.MALTA, "5").orElseThrow();

        assertThat(estados.anterior()).isNull();
        assertThat(estados.posterior()).containsEntry("cantidad", 4);
    }

    @Test
    @DisplayName("CP-RE-08: una baja después de modificar conserva el anterior del primer cambio y deja el posterior vacío")
    void modificacionYBaja() {
        MaltaEntity malta = malta(5L);
        registro.iniciar(ConceptoAuditoria.MALTA);
        registro.registrarModificacion(malta, estado("cantidad", 1), estado("cantidad", 2));
        registro.registrarBaja(malta, estado("cantidad", 2));

        EstadosCapturados estados = registro.resolver(ConceptoAuditoria.MALTA, "5").orElseThrow();

        assertThat(estados.anterior()).containsEntry("cantidad", 1);
        assertThat(estados.posterior()).isNull();
    }

    @Test
    @DisplayName("CP-RE-09: resolver elige la entidad del módulo auditado aunque otra entidad de otro tipo tenga el mismo id")
    void resolver_distingueEntidadesConElMismoId() {
        OllaHervorEntity olla = new OllaHervorEntity();
        olla.setId(5L);
        registro.iniciar(ConceptoAuditoria.MALTA);
        registro.iniciar(ConceptoAuditoria.OLLA_DE_HERVOR);
        registro.registrarModificacion(olla, estado("nombre", "olla"), estado("nombre", "olla 2"));
        registro.registrarModificacion(malta(5L), estado("nombre", "A"), estado("nombre", "B"));

        EstadosCapturados estados = registro.resolver(ConceptoAuditoria.MALTA, "5").orElseThrow();

        assertThat(estados.posterior()).containsEntry("nombre", "B");
        assertThat(registro.resolver(ConceptoAuditoria.LUPULO, "5")).isEmpty();
        assertThat(registro.resolver(ConceptoAuditoria.OLLA_DE_HERVOR, "5").orElseThrow().posterior()).containsEntry("nombre", "olla 2");
    }

    @Test
    @DisplayName("CP-RE-15: solo se retienen las entidades de los conceptos admitidos; un método anidado admite además el suyo")
    void soloRetieneEntidadesDeLosConceptosAdmitidos() {
        OllaHervorEntity olla = new OllaHervorEntity();
        olla.setId(5L);
        registro.iniciar(ConceptoAuditoria.MALTA);

        registro.registrarModificacion(olla, estado("nombre", "olla"), estado("nombre", "olla 2"));
        registro.registrarModificacion(malta(5L), estado("nombre", "A"), estado("nombre", "B"));
        assertThat(registro.iniciar(ConceptoAuditoria.OLLA_DE_HERVOR)).isFalse();

        assertThat(registro.resolver(ConceptoAuditoria.OLLA_DE_HERVOR, "5")).as("la olla se registró antes de admitir su concepto").isEmpty();
        assertThat(registro.resolver(ConceptoAuditoria.MALTA, "5")).isPresent();

        registro.registrarModificacion(olla, estado("nombre", "olla"), estado("nombre", "olla 3"));
        assertThat(registro.resolver(ConceptoAuditoria.OLLA_DE_HERVOR, "5").orElseThrow().posterior()).containsEntry("nombre", "olla 3");
    }

    @Test
    @DisplayName("CP-RE-10: resolver devuelve vacío si el id no coincide, es nulo o la operación no cambió el registro")
    void resolver_sinCoincidencia() {
        registro.iniciar(ConceptoAuditoria.MALTA);
        registro.registrarModificacion(malta(5L), estado("nombre", "A"), estado("nombre", "B"));

        assertThat(registro.resolver(ConceptoAuditoria.MALTA, "6")).isEmpty();
        assertThat(registro.resolver(ConceptoAuditoria.MALTA, null)).isEmpty();
        assertThat(registro.resolver(null, "5")).isEmpty();
        assertThat(registro.resolver(ConceptoAuditoria.SESION, "5")).isEmpty();
    }

    @Test
    @DisplayName("CP-RE-11: los registros ignoran entidades nulas")
    void ignoraEntidadesNulas() {
        registro.iniciar(ConceptoAuditoria.MALTA);

        registro.registrarAlta(null, estado("a", 1));
        registro.registrarModificacion(null, null, estado("a", 1));
        registro.registrarBaja(null, estado("a", 1));

        assertThat(registro.resolver(ConceptoAuditoria.MALTA, "1")).isEmpty();
    }

    @Test
    @DisplayName("CP-RE-12: la captura de un hilo no se ve desde otro hilo")
    void capturaPorHilo() throws InterruptedException {
        registro.iniciar(ConceptoAuditoria.MALTA);
        registro.registrarAlta(malta(5L), estado("nombre", "A"));
        boolean[] activoEnOtroHilo = {true};

        Thread otro = new Thread(() -> activoEnOtroHilo[0] = registro.activo());
        otro.start();
        otro.join();

        assertThat(activoEnOtroHilo[0]).isFalse();
    }

    @Test
    @DisplayName("CP-RE-13: coincide relaciona el concepto con la clase por su nombre, ignorando guiones bajos y la palabra DE")
    void coincide_porNombre() {
        assertThat(RegistroEstadosEntidad.coincide(ConceptoAuditoria.MALTA, MaltaEntity.class)).isTrue();
        assertThat(RegistroEstadosEntidad.coincide(ConceptoAuditoria.OLLA_DE_HERVOR, OllaHervorEntity.class)).isTrue();
        assertThat(RegistroEstadosEntidad.coincide(ConceptoAuditoria.LUPULO, MaltaEntity.class)).isFalse();
    }

    @Test
    @DisplayName("CP-RE-14: todo concepto que usa un método auditado corresponde a una única entidad persistente")
    void todoConceptoAuditadoTieneUnaEntidad() throws Exception {
        Set<ConceptoAuditoria> conceptos = EnumSet.noneOf(ConceptoAuditoria.class);
        var escanerServicios = new ClassPathScanningCandidateComponentProvider(false);
        escanerServicios.addIncludeFilter(new AnnotationTypeFilter(Service.class));
        for (var candidato : escanerServicios.findCandidateComponents("com.github.heikyudev.maestrocervecero.service.implementation")) {
            for (Method metodo : Class.forName(candidato.getBeanClassName()).getDeclaredMethods()) {
                AuditableAction anotacion = metodo.getAnnotation(AuditableAction.class);
                if (anotacion != null) {
                    conceptos.add(anotacion.conceptoAuditoria());
                }
            }
        }

        List<Class<?>> entidades = new ArrayList<>();
        var escanerEntidades = new ClassPathScanningCandidateComponentProvider(false);
        escanerEntidades.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        for (var candidato : escanerEntidades.findCandidateComponents("com.github.heikyudev.maestrocervecero.persistence.entity")) {
            entidades.add(Class.forName(candidato.getBeanClassName()));
        }

        assertThat(conceptos).hasSizeGreaterThan(30);
        for (ConceptoAuditoria concepto : conceptos) {
            assertThat(entidades.stream().filter(clase -> RegistroEstadosEntidad.coincide(concepto, clase)).toList())
                    .as("entidades del concepto " + concepto).hasSize(1);
        }
    }
}
