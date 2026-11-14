package com.github.heikyudev.maestrocervecero.util.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ExcluidoDeAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import jakarta.persistence.Entity;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.Type;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SerializadorEstadoEntidadTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final SerializadorEstadoEntidad serializador = new SerializadorEstadoEntidad(jsonMapper);

    @Entity
    static class FabricanteFalso {
        Long id = 3L;

        public Long getId() {
            return id;
        }
    }

    static class EntidadConSecretos {
        @ExcluidoDeAuditoria
        String claveInterna;
        String nombre;
    }

    enum Color {ROJO}

    // ==================== aMapa ====================

    @Test
    @DisplayName("CP-SE-01: aMapa copia los escalares como número, booleano y texto, los enumerados por su nombre y las fechas en ISO-8601")
    void aMapa_debeCopiarLosTiposEscalares() {
        String[] nombres = {"entero", "largo", "decimal", "preciso", "activo", "nombre", "color", "fecha", "fechaHora"};
        Object[] valores = {5, 10L, 2.5, new BigDecimal("12.3400"), true, "Pilsen", Color.ROJO,
                LocalDate.of(2026, 11, 13), LocalDateTime.of(2026, 11, 13, 10, 30, 15)};

        Map<String, Object> estado = serializador.aMapa(EntidadConSecretos.class, nombres, valores, null);

        assertThat(estado).containsEntry("entero", 5).containsEntry("largo", 10L).containsEntry("decimal", 2.5)
                .containsEntry("preciso", new BigDecimal("12.3400")).containsEntry("activo", true)
                .containsEntry("nombre", "Pilsen").containsEntry("color", "ROJO")
                .containsEntry("fecha", "2026-11-13").containsEntry("fechaHora", "2026-11-13T10:30:15");
        assertThat(estado.keySet()).containsExactlyElementsOf(List.of(nombres));
    }

    @Test
    @DisplayName("CP-SE-02: aMapa conserva los valores nulos")
    void aMapa_debeConservarLosNulos() {
        Map<String, Object> estado = serializador.aMapa(EntidadConSecretos.class, new String[]{"telefono", "nombre"},
                new Object[]{null, "Ana"}, null);

        assertThat(estado).containsEntry("telefono", null).containsEntry("nombre", "Ana");
    }

    @Test
    @DisplayName("CP-SE-03: aMapa reemplaza una asociación por el id de la entidad asociada bajo el nombre del campo seguido de Id")
    void aMapa_debeReemplazarLasAsociacionesPorSuId() {
        Map<String, Object> estado = serializador.aMapa(EntidadConSecretos.class, new String[]{"fabricante"},
                new Object[]{new FabricanteFalso()}, null);

        assertThat(estado).containsExactly(Map.entry("fabricanteId", 3L));
    }

    @Test
    @DisplayName("CP-SE-04: aMapa lee el id de un proxy perezoso sin inicializarlo")
    void aMapa_noDebeInicializarUnProxyPerezoso() {
        HibernateProxy proxy = mock(HibernateProxy.class);
        LazyInitializer inicializador = mock(LazyInitializer.class);
        when(proxy.getHibernateLazyInitializer()).thenReturn(inicializador);
        when(inicializador.getIdentifier()).thenReturn(7L);

        Map<String, Object> estado = serializador.aMapa(EntidadConSecretos.class, new String[]{"lote"},
                new Object[]{proxy}, null);

        assertThat(estado).containsExactly(Map.entry("loteId", 7L));
        verify(inicializador).getIdentifier();
        verify(inicializador, never()).getImplementation();
        verify(inicializador, never()).initialize();
        verifyNoMoreInteractions(inicializador);
    }

    @Test
    @DisplayName("CP-SE-05: aMapa omite las colecciones (por su tipo o por su valor), los mapas, los binarios y los objetos que no son escalares")
    void aMapa_debeOmitirLoQueNoEsEscalar() {
        Type tipoColeccion = mock(Type.class);
        when(tipoColeccion.isCollectionType()).thenReturn(true);
        Type tipoComun = mock(Type.class);
        when(tipoComun.isCollectionType()).thenReturn(false);

        Map<String, Object> estado = serializador.aMapa(EntidadConSecretos.class,
                new String[]{"detalles", "etiquetas", "mapa", "archivo", "objeto", "nombre"},
                new Object[]{List.of(1, 2), List.of("a"), Map.of("a", 1), new byte[]{1, 2}, new Object(), "Ana"},
                new Type[]{tipoColeccion, tipoComun, tipoComun, tipoComun, tipoComun, tipoComun});

        assertThat(estado).containsOnlyKeys("nombre");
    }

    @Test
    @DisplayName("CP-SE-06: aMapa omite los campos anotados con ExcluidoDeAuditoria, también los de una superclase")
    void aMapa_debeOmitirLosCamposAnotados() {
        Map<String, Object> estado = serializador.aMapa(EntidadConSecretos.class, new String[]{"claveInterna", "nombre"},
                new Object[]{"s3cr3t", "Ana"}, null);

        assertThat(estado).containsOnlyKeys("nombre");

        class Hija extends EntidadConSecretos {
        }
        assertThat(serializador.aMapa(Hija.class, new String[]{"claveInterna", "nombre"},
                new Object[]{"s3cr3t", "Ana"}, null)).containsOnlyKeys("nombre");
    }

    @Test
    @DisplayName("CP-SE-07: aMapa omite todo campo cuyo nombre contenga password, contrasena, secret, token, hash, clave, pin, apikey, credencial u otp, aunque no tenga la anotación")
    void aMapa_debeOmitirLosNombresSensibles() {
        Map<String, Object> estado = serializador.aMapa(EntidadConSecretos.class,
                new String[]{"password", "passwordTemporal", "Contrasena", "apiSecret", "tokenRecuperacion", "hashClave", "nombre",
                        "claveAcceso", "pinSeguridad", "apiKey", "credencialExterna", "otpActual"},
                new Object[]{"a", "b", "c", "d", "e", "f", "Ana", "g", "h", "i", "j", "k"}, null);

        assertThat(estado).containsOnlyKeys("nombre");
        assertThat(SerializadorEstadoEntidad.FRAGMENTOS_SENSIBLES)
                .contains("password", "token", "hash", "secret", "clave", "pin", "apikey", "credencial", "otp");
    }

    @Test
    @DisplayName("CP-SE-14: los datos de contacto personales del usuario, del cliente y de la versión del proveedor nunca llegan al JSON")
    void aMapa_nuncaDebeIncluirDatosPersonalesDeContacto() throws Exception {
        for (Class<?> clase : new Class<?>[]{UsuarioEntity.class, com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity.class,
                com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity.class}) {
            for (String campo : new String[]{"telefono", clase == UsuarioEntity.class ? "correo" : "email", "direccion"}) {
                if (clase == UsuarioEntity.class && campo.equals("direccion")) {
                    continue;
                }
                assertThat(clase.getDeclaredField(campo).isAnnotationPresent(ExcluidoDeAuditoria.class))
                        .as(clase.getSimpleName() + "." + campo).isTrue();
            }
        }
        // Los datos de negocio se conservan
        Map<String, Object> estado = serializador.aMapa(
                com.github.heikyudev.maestrocervecero.persistence.entity.proveedor.VersionProveedorEntity.class,
                new String[]{"cuit", "razonSocial", "email", "telefono", "direccion"},
                new Object[]{"20-1", "Malta SA", "a@b.com", "123", "Calle 1"}, null);
        assertThat(estado).containsOnlyKeys("cuit", "razonSocial");
    }

    @Test
    @DisplayName("CP-SE-08: el campo password del usuario nunca llega al JSON, ni su valor ni su nombre")
    void aMapa_nuncaDebeIncluirLaContrasenaDelUsuario() throws Exception {
        assertThat(UsuarioEntity.class.getDeclaredField("password").isAnnotationPresent(ExcluidoDeAuditoria.class)).isTrue();

        String json = serializador.aJson(serializador.aMapa(UsuarioEntity.class,
                new String[]{"username", "password", "nombre"}, new Object[]{"ana", "$2a$10$hashbcrypt", "Ana"}, null));

        assertThat(json).contains("ana").doesNotContain("password").doesNotContain("$2a$10$hashbcrypt");
    }

    @Test
    @DisplayName("CP-SE-09: ningún campo de ninguna entidad tiene un nombre sensible sin estar anotado como excluido")
    void ningunaEntidadExponeUnCampoSensibleSinAnotar() throws Exception {
        var escaner = new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new org.springframework.core.type.filter.AnnotationTypeFilter(Entity.class));
        for (var candidato : escaner.findCandidateComponents("com.github.heikyudev.maestrocervecero.persistence.entity")) {
            Class<?> clase = Class.forName(candidato.getBeanClassName());
            for (Class<?> actual = clase; actual != null && actual != Object.class; actual = actual.getSuperclass()) {
                for (Field campo : actual.getDeclaredFields()) {
                    if (SerializadorEstadoEntidad.esNombreSensible(campo.getName())) {
                        assertThat(campo.isAnnotationPresent(ExcluidoDeAuditoria.class))
                                .as(clase.getSimpleName() + "." + campo.getName()).isTrue();
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("CP-SE-10: aMapa con nombres o valores nulos devuelve un mapa vacío y devuelve una copia independiente del arreglo de Hibernate")
    void aMapa_debeTolerarEntradasNulasYCopiarLosValores() {
        assertThat(serializador.aMapa(EntidadConSecretos.class, null, null, null)).isEmpty();

        Object[] valores = {"Ana"};
        Map<String, Object> estado = serializador.aMapa(EntidadConSecretos.class, new String[]{"nombre"}, valores, null);
        valores[0] = "Otra";

        assertThat(estado).containsEntry("nombre", "Ana");
    }

    // ==================== aJson ====================

    @Test
    @DisplayName("CP-SE-11: aJson escribe números como números, textos como textos y nulos como null, con fechas en ISO")
    void aJson_debeEscribirTiposNativosDeJson() {
        Map<String, Object> estado = serializador.aMapa(EntidadConSecretos.class,
                new String[]{"cantidad", "costo", "activo", "nombre", "telefono", "fecha"},
                new Object[]{4, new BigDecimal("12.50"), false, "Pilsen", null, LocalDateTime.of(2026, 11, 13, 10, 30)}, null);

        JsonNode json = jsonMapper.readTree(serializador.aJson(estado));

        assertThat(json.get("cantidad").isIntegralNumber()).isTrue();
        assertThat(json.get("costo").isNumber()).isTrue();
        assertThat(json.get("costo").decimalValue()).isEqualByComparingTo("12.50");
        assertThat(json.get("activo").isBoolean()).isTrue();
        assertThat(json.get("nombre").stringValue()).isEqualTo("Pilsen");
        assertThat(json.get("telefono").isNull()).isTrue();
        assertThat(json.get("fecha").stringValue()).isEqualTo("2026-11-13T10:30");
    }

    @Test
    @DisplayName("CP-SE-12: aJson de un estado nulo devuelve null")
    void aJson_conEstadoNuloDevuelveNull() {
        assertThat(serializador.aJson(null)).isNull();
    }

    @Test
    @DisplayName("CP-SE-13: aJson nunca lanza una excepción si el estado no se puede serializar")
    void aJson_noDebeLanzarSiNoSePuedeSerializar() {
        Map<String, Object> ciclico = new java.util.HashMap<>();
        ciclico.put("yo", ciclico);

        assertThat(serializador.aJson(ciclico)).isNull();
    }
}
