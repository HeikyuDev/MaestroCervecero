package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.view.estadisticas.CampoFiltro;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.ProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesTexto;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.aId;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.recetaElegida;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.resolverUno;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.textoDeReceta;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.vacioANulo;

/**
 * Criterios de filtro que comparten las pestañas de estadísticas: cada uno interpreta su parámetro (un valor inexistente
 * o una entidad que el buscador no ofrece no filtra) y arma su {@link CampoFiltro}. Los avisos de un valor inexistente se
 * agregan a la lista recibida.
 */
final class CamposDeReporte {

    static final String PARAMETRO_RECETA = "idReceta";
    static final String PARAMETRO_PROVEEDOR = "idProveedor";
    static final String PARAMETRO_EQUIPO = "idEquipo";
    static final String URL_BUSCADOR_RECETAS = "/lotes/filtros/recetas";
    static final String URL_BUSCADOR_PROVEEDORES = "/ordenes-compra/buscador/proveedores";
    static final String URL_BUSCADOR_EQUIPOS = "/estadisticas/buscador/equipos";
    static final String PARAMETRO_ESTADO = "estado";
    static final String AVISO_ESTADO_INVALIDO = "El estado indicado no es válido, se muestran todos";

    private CamposDeReporte() {
    }

    /** Un criterio ya interpretado: el campo de la barra de filtros, el valor y el texto con que se muestra. */
    record Elegido<T>(CampoFiltro campo, T valor, String texto) {

        /** El ID del valor elegido con la función indicada, o {@code null} si no se eligió ninguno. */
        Long id(Function<T, Long> idDe) {
            return valor == null ? null : idDe.apply(valor);
        }
    }

    static Elegido<RecetaResponseDTO> receta(Map<String, String> parametros, IRecetaServicio recetaServicio) {
        RecetaResponseDTO receta = recetaElegida(aId(parametros.get(PARAMETRO_RECETA)), recetaServicio::buscarActivaODadaDeBajaConPlanificacionesPorId);
        String texto = receta == null ? null : textoDeReceta(receta);
        return new Elegido<>(CampoFiltro.buscador(PARAMETRO_RECETA, "Receta", receta == null ? null : String.valueOf(receta.getId()), texto,
                URL_BUSCADOR_RECETAS, "Seleccione receta"), receta, texto);
    }

    static Elegido<ProveedorResponseDTO> proveedor(Map<String, String> parametros, IProveedorServicio proveedorServicio) {
        ProveedorResponseDTO proveedor = resolverUno(aId(parametros.get(PARAMETRO_PROVEEDOR)), proveedorServicio::buscarPorId);
        String texto = proveedor == null ? null
                : proveedor.getVersion().getRazonSocial() + " (" + ValidacionesTexto.formatearCuit(proveedor.getVersion().getCuit()) + ")";
        return new Elegido<>(CampoFiltro.buscador(PARAMETRO_PROVEEDOR, "Proveedor", proveedor == null ? null : String.valueOf(proveedor.getId()), texto,
                URL_BUSCADOR_PROVEEDORES, "Seleccione proveedor"), proveedor, texto);
    }

    /** El valor de un enum pedido en un parámetro, avisando si no existe, y su lista simple. */
    static <E extends Enum<E>> Elegido<E> lista(Class<E> tipo, String parametro, String etiqueta, String textoTodos, String avisoInvalido,
                                                 Map<String, String> parametros, Function<E, String> etiquetaDe, List<String> avisos) {
        String pedido = parametros.get(parametro);
        E valor = ApoyoReporte.aEnum(tipo, pedido);
        if (vacioANulo(pedido) != null && valor == null) {
            avisos.add(avisoInvalido);
        }
        List<CampoFiltro.Opcion> opciones = Arrays.stream(tipo.getEnumConstants()).map(e -> new CampoFiltro.Opcion(e.name(), etiquetaDe.apply(e))).toList();
        return new Elegido<>(CampoFiltro.lista(parametro, etiqueta, valor == null ? null : valor.name(), opciones, textoTodos), valor,
                valor == null ? null : etiquetaDe.apply(valor));
    }

    /** El estado pedido (de un lote o de una orden de compra), avisando si no existe, y su lista simple. */
    static <E extends Enum<E>> Elegido<E> estado(Class<E> tipo, Map<String, String> parametros, Function<E, String> etiquetaDe, List<String> avisos) {
        return lista(tipo, PARAMETRO_ESTADO, "Estado", "Todos", AVISO_ESTADO_INVALIDO, parametros, etiquetaDe, avisos);
    }

    static Elegido<TipoInsumo> tipoDeInsumo(Map<String, String> parametros, List<String> avisos) {
        return lista(TipoInsumo.class, "tipoInsumo", "Tipo de insumo", "Todos", "El tipo de insumo indicado no es válido, se muestran todos",
                parametros, TipoInsumo::getEtiqueta, avisos);
    }

    static Elegido<TipoEtapa> etapa(Map<String, String> parametros, List<String> avisos) {
        return lista(TipoEtapa.class, "etapa", "Etapa", "Todas", "La etapa indicada no es válida, se muestran todas", parametros, TipoEtapa::getEtiqueta, avisos);
    }

    static Elegido<TipoEquipamiento> tipoDeEquipo(Map<String, String> parametros, List<String> avisos) {
        return lista(TipoEquipamiento.class, "tipo", "Tipo de equipo", "Todos", "El tipo de equipo indicado no es válido, se muestran todos",
                parametros, TipoEquipamiento::getEtiqueta, avisos);
    }

    /**
     * Texto con el que se muestra un equipo en el filtro de equipo: su identificador y, si está dado de baja, la marca
     * "(dado de baja)", igual que en la tabla del reporte de equipamiento.
     */
    static String textoDeEquipo(String identificadorInterno, boolean dadoDeBaja) {
        return dadoDeBaja ? identificadorInterno + " (dado de baja)" : identificadorInterno;
    }
}
