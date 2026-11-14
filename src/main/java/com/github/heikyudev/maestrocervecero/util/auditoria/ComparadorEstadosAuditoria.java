package com.github.heikyudev.maestrocervecero.util.auditoria;

import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.CampoAuditoriaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.EstadoCampoAuditoria;
import com.github.heikyudev.maestrocervecero.util.FormatoCantidad;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Lee los JSON de los estados anterior y posterior de una entrada de la bitácora y los compara campo a campo.
 * <p>
 * Un JSON ausente, vacío, inválido o que no sea un objeto se trata como un estado inexistente: nunca lanza una
 * excepción, de modo que una entrada con datos dañados se muestra sin detalle en lugar de fallar.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComparadorEstadosAuditoria {

    /**
     * Resultado de la comparación.
     *
     * @param tieneDetalle {@code true} si al menos uno de los dos estados es un JSON legible.
     * @param campos Todos los campos de los dos estados, con su resultado de comparación; vacía si no hay detalle.
     */
    public record Comparacion(boolean tieneDetalle, List<CampoAuditoriaResponseDTO> campos) {
    }

    private static final Map<String, String> ETIQUETAS_FIJAS = Map.of(
            "id", "ID",
            "createdBy", "Creado por",
            "createdDate", "Fecha de creación",
            "lastModifiedBy", "Modificado por",
            "lastModifiedDate", "Fecha de modificación");

    private static final Pattern FECHA = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern FECHA_HORA = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}.*");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JsonMapper jsonMapper;

    /**
     * Compara dos estados guardados como JSON.
     *
     * @param estadoAnterior JSON del estado anterior, o {@code null}.
     * @param estadoPosterior JSON del estado posterior, o {@code null}.
     * @return La comparación; sin detalle si ninguno de los dos JSON es un objeto legible.
     */
    public Comparacion comparar(String estadoAnterior, String estadoPosterior) {
        JsonNode anterior = leer(estadoAnterior);
        JsonNode posterior = leer(estadoPosterior);
        if (anterior == null && posterior == null) {
            return new Comparacion(false, List.of());
        }

        // Los campos salen en el orden del estado anterior y, después, los que solo existen en el posterior
        Set<String> claves = new LinkedHashSet<>();
        if (anterior != null) {
            anterior.properties().forEach(campo -> claves.add(campo.getKey()));
        }
        if (posterior != null) {
            posterior.properties().forEach(campo -> claves.add(campo.getKey()));
        }

        List<CampoAuditoriaResponseDTO> campos = new ArrayList<>();
        for (String clave : claves) {
            JsonNode valorAnterior = anterior != null ? anterior.get(clave) : null;
            JsonNode valorPosterior = posterior != null ? posterior.get(clave) : null;
            campos.add(CampoAuditoriaResponseDTO.builder()
                    .campo(etiqueta(clave))
                    .valorAnterior(aTexto(clave, valorAnterior))
                    .valorPosterior(aTexto(clave, valorPosterior))
                    .estado(estadoDe(valorAnterior, valorPosterior))
                    .build());
        }
        return new Comparacion(true, campos);
    }

    private JsonNode leer(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode nodo = jsonMapper.readTree(json);
            return nodo != null && nodo.isObject() ? nodo : null;
        } catch (JacksonException ex) {
            log.warn("El estado guardado en la bitácora no es un JSON válido ({})", ex.getClass().getSimpleName());
            return null;
        }
    }

    // Un campo con valor null en los dos estados no cambió; un campo que existe en un solo estado (aunque su valor sea
    // null) es AGREGADO o ELIMINADO. Aquí null de Java es "el campo no existe" y NullNode es "el campo vale null".
    private static EstadoCampoAuditoria estadoDe(JsonNode anterior, JsonNode posterior) {
        if (anterior == null && posterior == null) {
            return EstadoCampoAuditoria.SIN_CAMBIOS;
        }
        if (anterior == null) {
            return EstadoCampoAuditoria.AGREGADO;
        }
        if (posterior == null) {
            return EstadoCampoAuditoria.ELIMINADO;
        }
        return sonIguales(anterior, posterior) ? EstadoCampoAuditoria.SIN_CAMBIOS : EstadoCampoAuditoria.MODIFICADO;
    }

    // Dos números son iguales por su valor, sin importar el tipo del nodo (1 y 1.0 son el mismo valor)
    private static boolean sonIguales(JsonNode anterior, JsonNode posterior) {
        if (anterior.isNumber() && posterior.isNumber()) {
            return anterior.decimalValue().compareTo(posterior.decimalValue()) == 0;
        }
        return anterior.equals(posterior);
    }

    // Texto para mostrar: null si el campo no existe o no tiene valor; fechas como dd/MM/yyyy; booleanos como Sí / No;
    // números con el formato de la aplicación (los identificadores se muestran sin separador de miles)
    private static String aTexto(String clave, JsonNode nodo) {
        if (nodo == null || nodo.isNull() || nodo.isMissingNode()) {
            return null;
        }
        if (nodo.isBoolean()) {
            return nodo.booleanValue() ? "Sí" : "No";
        }
        if (nodo.isString()) {
            return formatearFecha(nodo.stringValue());
        }
        if (nodo.isContainer()) {
            return nodo.toString();
        }
        if (nodo.isNumber() && !esIdentificador(clave)) {
            return FormatoCantidad.formatear(nodo.decimalValue());
        }
        return nodo.asString();
    }

    private static boolean esIdentificador(String clave) {
        return clave.equals("id") || (clave.length() > 2 && clave.endsWith("Id"));
    }

    private static String formatearFecha(String texto) {
        try {
            if (FECHA_HORA.matcher(texto).matches()) {
                return LocalDateTime.parse(texto).format(FORMATO_FECHA_HORA);
            }
            if (FECHA.matcher(texto).matches()) {
                return LocalDate.parse(texto).format(FORMATO_FECHA);
            }
        } catch (DateTimeParseException ex) {
            return texto;
        }
        return texto;
    }

    /**
     * Nombre legible de un campo: los de auditoría de fila tienen su nombre propio y el resto se separa en palabras
     * (por ejemplo {@code potencialExtracto} es "Potencial extracto" y {@code fabricanteId} es "Fabricante (ID)").
     */
    static String etiqueta(String clave) {
        String fija = ETIQUETAS_FIJAS.get(clave);
        if (fija != null) {
            return fija;
        }
        if (clave.isBlank()) {
            return clave;
        }
        boolean esId = clave.length() > 2 && clave.endsWith("Id");
        String base = esId ? clave.substring(0, clave.length() - 2) : clave;
        StringBuilder palabras = new StringBuilder();
        for (char c : base.toCharArray()) {
            if (Character.isUpperCase(c) && !palabras.isEmpty()) {
                palabras.append(' ');
            }
            palabras.append(Character.toLowerCase(c));
        }
        String texto = palabras.toString();
        texto = texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
        return esId ? texto + " (ID)" : texto;
    }
}
