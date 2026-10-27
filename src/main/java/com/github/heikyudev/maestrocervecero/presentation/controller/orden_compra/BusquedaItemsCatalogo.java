package com.github.heikyudev.maestrocervecero.presentation.controller.orden_compra;

import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.VersionRecetaDePlanificacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.CatalogoProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.proveedor.VersionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lógica en memoria del buscador de ítems del formulario de orden de compra: de los ítems del catálogo vigente del
 * proveedor, deja los que usa la receta de la planificación y no están ya en la orden, los filtra por insumo y
 * presentación, los ordena por insumo y presentación y los pagina.
 */
final class BusquedaItemsCatalogo {

    private BusquedaItemsCatalogo() {
    }

    /**
     * La página de ítems que se pueden ofrecer.
     *
     * @param version Versión vigente del proveedor.
     * @param recetaDePlanificacion Versión de receta de la planificación elegida.
     * @param idsAgregados Ids de ítems del catálogo que la orden ya tiene (se excluyen).
     * @param insumo Texto buscado en el nombre del insumo, o {@code null}.
     * @param presentacion Texto buscado en el nombre de la presentación, o {@code null}.
     * @param pageable Página pedida.
     */
    static Page<CatalogoProveedorResponseDTO> buscar(VersionProveedorResponseDTO version,
                                                    VersionRecetaDePlanificacionResponseDTO recetaDePlanificacion,
                                                    Collection<Long> idsAgregados, String insumo, String presentacion,
                                                    Pageable pageable) {
        Set<Long> idsInsumosDeReceta = idsDeInsumos(recetaDePlanificacion);
        Set<Long> agregados = idsAgregados == null ? Set.of() : new HashSet<>(idsAgregados);
        String insumoBuscado = minuscula(insumo);
        String presentacionBuscada = minuscula(presentacion);

        List<CatalogoProveedorResponseDTO> items = (version.getCatalogoProveedor() == null ? List.<CatalogoProveedorResponseDTO>of() : version.getCatalogoProveedor())
                .stream()
                .filter(item -> item.getInsumo() != null && idsInsumosDeReceta.contains(item.getInsumo().getId()))
                .filter(item -> !agregados.contains(item.getId()))
                .filter(item -> contiene(item.getInsumo().getNombre(), insumoBuscado))
                .filter(item -> item.getPresentacionComercial() != null && contiene(item.getPresentacionComercial().getNombre(), presentacionBuscada))
                .sorted(Comparator.comparing((CatalogoProveedorResponseDTO item) -> item.getInsumo().getNombre(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(item -> item.getPresentacionComercial().getNombre(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CatalogoProveedorResponseDTO::getId))
                .toList();

        int desde = (int) Math.min(pageable.getOffset(), items.size());
        int hasta = Math.min(desde + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(desde, hasta), pageable, items.size());
    }

    /**
     * Los ids de los insumos (maltas, lúpulos y levaduras) de la versión de receta de la planificación.
     */
    private static Set<Long> idsDeInsumos(VersionRecetaDePlanificacionResponseDTO recetaDePlanificacion) {
        VersionRecetaResponseDTO version = recetaDePlanificacion.getVersion();
        Set<Long> ids = new HashSet<>();
        if (version.getDetallesMalta() != null) {
            version.getDetallesMalta().forEach(detalle -> ids.add(detalle.getMalta().getId()));
        }
        if (version.getDetallesLupulo() != null) {
            version.getDetallesLupulo().forEach(detalle -> ids.add(detalle.getLupulo().getId()));
        }
        if (version.getDetallesLevadura() != null) {
            version.getDetallesLevadura().forEach(detalle -> ids.add(detalle.getLevadura().getId()));
        }
        return ids;
    }

    private static String minuscula(String texto) {
        return texto == null ? null : texto.toLowerCase(Locale.ROOT);
    }

    /**
     * Si el nombre contiene el texto buscado, sin distinguir mayúsculas; sin texto buscado cualquier nombre coincide.
     */
    private static boolean contiene(String nombre, String buscadoEnMinuscula) {
        return buscadoEnMinuscula == null || (nombre != null && nombre.toLowerCase(Locale.ROOT).contains(buscadoEnMinuscula));
    }
}
