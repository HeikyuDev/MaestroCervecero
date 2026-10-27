package com.github.heikyudev.maestrocervecero.presentation.controller;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoEtapaLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Constantes y utilidades compartidas por los controllers de las pantallas de listado.
 */
public final class MetodosControlador {

    public static final int TAMANIO_PAGINA = 20;
    public static final int TAMANIO_PAGINA_BUSCADOR = 8;

    /**
     * Los módulos maestros (ubicaciones, insumos, etc.) se listan en orden alfabético por nombre (sin
     * distinguir mayúsculas), con el id como desempate para que el orden sea siempre estable.
     */
    public static final Sort ORDEN_ALFABETICO = Sort.by(
            Sort.Order.asc("nombre").ignoreCase(),
            Sort.Order.asc("id"));

    /**
     * El equipamiento no tiene nombre sino identificador interno: se lista en orden alfabético por
     * identificador (sin distinguir mayúsculas), con el id como desempate.
     */
    public static final Sort ORDEN_POR_IDENTIFICADOR = Sort.by(
            Sort.Order.asc("identificadorInterno").ignoreCase(),
            Sort.Order.asc("id"));

    /**
     * Los listados transaccionales se muestran con el último registro arriba: el id es autogenerado y creciente.
     */
    public static final Sort ORDEN_MAS_RECIENTE_PRIMERO = Sort.by(Sort.Order.desc("id"));

    /**
     * Fragmento del modal de búsqueda de recetas. Lo comparten el formulario de alta de planificaciones y los filtros
     * de consulta (listado de planificaciones, listado de lotes y cronograma de producción).
     */
    public static final String VISTA_BUSCADOR_RECETA = "planificacion_produccion/buscador-receta :: buscador";

    private MetodosControlador() {
    }

    /**
     * Un texto vacío o en blanco equivale a no filtrar: devuelve {@code null}; si no, el texto sin espacios sobrantes.
     */
    public static String vacioANulo(String texto) {
        return (texto == null || texto.isBlank()) ? null : texto.trim();
    }

    /**
     * El texto como id, o {@code null} si está vacío, no es un número o no es positivo.
     */
    public static Long aId(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            long valor = Long.parseLong(limpio);
            return valor > 0 ? valor : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * El estado operativo con ese nombre (sin distinguir mayúsculas), o {@code null} si el texto está vacío o no
     * corresponde a ninguno.
     */
    public static EstadoOperativo aEstadoOperativo(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        try {
            return EstadoOperativo.valueOf(limpio.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * El aviso para mostrar dentro de un buscador cuando el estado operativo pedido no corresponde a ninguno (se
     * ignora y se muestran todos), o {@code null} si no hay nada que avisar.
     */
    public static String avisoDeEstadoOperativo(String pedido, EstadoOperativo interpretado) {
        return vacioANulo(pedido) != null && interpretado == null ? "El estado operativo indicado no es válido, se muestran todos" : null;
    }

    /**
     * Si se puede operar una etapa de un lote (registrar y anular consumos o envasados, finalizar): el lote debe estar
     * en ejecución y la etapa en curso.
     */
    public static boolean esOperable(EstadoLote estadoLote, EstadoEtapaLote estadoEtapa) {
        return estadoLote == EstadoLote.EN_EJECUCION && estadoEtapa == EstadoEtapaLote.EN_CURSO;
    }

    /**
     * Un número válido se devuelve sin ceros sobrantes ("10" y no "10.0"); un texto inválido, tal cual se escribió.
     */
    public static String numeroEscrito(Double valido, Object escrito) {
        if (valido == null) {
            return escrito.toString();
        }
        return valido == valido.longValue() ? String.valueOf(valido.longValue()) : valido.toString();
    }

    /**
     * Agrega la alerta (mensaje + tipo) que el layout muestra tras un Post-Redirect-Get.
     */
    public static void agregarMensaje(RedirectAttributes redirectAttributes, String mensaje, TipoAlerta tipo) {
        redirectAttributes.addFlashAttribute("mensaje", mensaje);
        redirectAttributes.addFlashAttribute("tipo", tipo.getCodigo());
    }

    /**
     * El nombre de la etapa tal como viaja en la URL (por ejemplo, "maceracion").
     */
    public static String slugDe(TipoEtapa tipoEtapa) {
        return tipoEtapa.name().toLowerCase(Locale.ROOT);
    }

    /**
     * La etapa cuyo nombre en la URL coincide con el texto, o {@code null} si el texto está vacío o ninguna coincide.
     */
    public static TipoEtapa aEtapa(String texto) {
        String limpio = vacioANulo(texto);
        if (limpio == null) {
            return null;
        }
        return Arrays.stream(TipoEtapa.values())
                .filter(tipo -> slugDe(tipo).equals(limpio.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    /**
     * La receta elegida en un filtro, resuelta con la búsqueda indicada, o {@code null} si no se eligió ninguna o si
     * esa búsqueda no la encuentra (no existe, o es una receta que el buscador del filtro no ofrece).
     */
    public static RecetaResponseDTO recetaElegida(Long idReceta, Function<Long, RecetaResponseDTO> buscar) {
        if (idReceta == null) {
            return null;
        }
        try {
            return buscar.apply(idReceta);
        } catch (RecursoNoEncontradoException e) {
            return null;
        }
    }

    /**
     * El texto con el que un selector muestra la receta elegida: el nombre de su versión vigente, con "(dada de baja)"
     * si corresponde.
     */
    public static String textoDeReceta(RecetaResponseDTO receta) {
        String nombre = receta.getVersion() != null ? receta.getVersion().getNombre() : null;
        return receta.getEstado() == Estado.BAJA ? nombre + " (dada de baja)" : nombre;
    }

    /**
     * Carga el modal de búsqueda de recetas ({@link #VISTA_BUSCADOR_RECETA}): los resultados, el criterio de nombre
     * tal como se escribió, la URL del buscador (el formulario y la paginación vuelven a ella) y la casilla "Incluir
     * dados de baja", que solo muestran los buscadores de los filtros de consulta.
     */
    public static void cargarBuscadorDeRecetas(Model model, Page<RecetaResponseDTO> recetas, String nombre, String urlBuscador,
                                               boolean ofreceDadosDeBaja, boolean incluirDadosDeBaja) {
        model.addAttribute("recetas", recetas);
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("urlBuscadorRecetas", urlBuscador);
        model.addAttribute("ofreceIncluirDadosDeBaja", ofreceDadosDeBaja);
        model.addAttribute("filtroIncluirDadosDeBaja", ofreceDadosDeBaja && incluirDadosDeBaja);
    }

    /**
     * Busca una vez cada id (ignorando los nulos) y arma el mapa id -> datos. Un id que ya no existe no tiene entrada.
     */
    public static <T> Map<Long, T> resolver(Collection<Long> ids, Function<Long, T> buscar) {
        Map<Long, T> resultado = new HashMap<>();
        ids.stream().filter(Objects::nonNull).distinct().forEach(id -> {
            try {
                T encontrado = buscar.apply(id);
                if (encontrado != null) {
                    resultado.put(id, encontrado);
                }
            } catch (RecursoNoEncontradoException e) {
                // sin entrada: la vista indica que el dato ya no está disponible
            }
        });
        return resultado;
    }

    /**
     * El dato de un solo id, o {@code null} si el id es nulo o ya no existe.
     */
    public static <T> T resolverUno(Long id, Function<Long, T> buscar) {
        return id == null ? null : resolver(List.of(id), buscar).get(id);
    }
}
