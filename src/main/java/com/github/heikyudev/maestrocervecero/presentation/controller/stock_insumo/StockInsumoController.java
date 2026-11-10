package com.github.heikyudev.maestrocervecero.presentation.controller.stock_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.persistence.enums.SituacionStockInsumo;
import com.github.heikyudev.maestrocervecero.service.interfaces.stock_insumo.IStockInsumoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ConsumoInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.ReservaInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.stock_insumo.StockLoteInsumoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.TAMANIO_PAGINA;
import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.vacioANulo;

/**
 * Controller de la consulta del stock de insumos ("Stock de Insumos"). Es de solo lectura y todo el módulo se autoriza
 * con el permiso {@code STOCK_INSUMO_CONSULTAR}.
 * <p>
 * Tiene tres pantallas: la lista de insumos con sus cantidades (disponible, reservada, vencida y total), el detalle de
 * un insumo con todos sus lotes en orden FEFO y el detalle de un lote de insumo con sus reservas y sus consumos, en
 * dos pestañas. El orden de cada tabla lo define el service; este controller solo arma la página pedida. Un ID que no
 * existe o de un insumo dado de baja lo resuelve el {@code ControllerAdvices}.
 * </p>
 */
@Controller
@RequestMapping("/stock-insumos")
@RequiredArgsConstructor
public class StockInsumoController {

    private static final String VISTA_LISTA = "stock_insumo/stock-lista";
    private static final String VISTA_DETALLE_INSUMO = "stock_insumo/stock-detalle-insumo";
    private static final String VISTA_DETALLE_LOTE = "stock_insumo/stock-detalle-lote";

    private static final String PESTANA_RESERVAS = "reservas";
    private static final String PESTANA_CONSUMOS = "consumos";

    private static final String CONSULTAR = "hasAuthority('STOCK_INSUMO_CONSULTAR')";

    private final IStockInsumoServicio stockInsumoServicio;

    /**
     * Lista los insumos activos con sus cantidades, paginados de a 20, en orden alfabético. Los criterios son
     * opcionales.
     */
    @GetMapping
    @PreAuthorize(CONSULTAR)
    public String listarInsumos(@RequestParam(required = false) String nombre,
                                @RequestParam(required = false) TipoInsumo tipo,
                                @RequestParam(required = false) SituacionStockInsumo situacionStock,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        Page<StockInsumoResponseDTO> insumos = stockInsumoServicio.filtrarInsumos(vacioANulo(nombre), tipo, situacionStock, pageable(pagina));

        model.addAttribute("insumos", insumos);
        model.addAttribute("tipos", TipoInsumo.values());
        model.addAttribute("situaciones", SituacionStockInsumo.values());
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroTipo", tipo);
        model.addAttribute("filtroSituacionStock", situacionStock);
        return VISTA_LISTA;
    }

    /**
     * Muestra el resumen de stock de un insumo y la lista paginada de todos sus lotes, en orden FEFO.
     */
    @GetMapping("/{idInsumo}")
    @PreAuthorize(CONSULTAR)
    public String detalleInsumo(@PathVariable Long idInsumo,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        StockInsumoResponseDTO insumo = stockInsumoServicio.obtenerStockInsumo(idInsumo);
        Page<StockLoteInsumoResponseDTO> lotes = stockInsumoServicio.filtrarLotesDeInsumo(idInsumo, pageable(pagina));

        model.addAttribute("insumo", insumo);
        model.addAttribute("lotes", lotes);
        return VISTA_DETALLE_INSUMO;
    }

    /**
     * Muestra la cabecera de un lote de insumo y, en dos pestañas, sus reservas y sus consumos, el último registro
     * arriba. Una pestaña desconocida equivale a "reservas".
     */
    @GetMapping("/lotes/{idLoteInsumo}")
    @PreAuthorize(CONSULTAR)
    public String detalleLoteInsumo(@PathVariable Long idLoteInsumo,
                                    @RequestParam(required = false) String pestana,
                                    @RequestParam(defaultValue = "0") int pagina,
                                    Model model) {
        StockLoteInsumoDetalleResponseDTO lote = stockInsumoServicio.obtenerDetalleLoteInsumo(idLoteInsumo);
        boolean consumos = PESTANA_CONSUMOS.equals(pestana);

        if (consumos) {
            Page<ConsumoInsumoResponseDTO> paginaConsumos = stockInsumoServicio.filtrarConsumosDeLoteInsumo(idLoteInsumo, pageable(pagina));
            model.addAttribute("consumos", paginaConsumos);
        } else {
            Page<ReservaInsumoResponseDTO> paginaReservas = stockInsumoServicio.filtrarReservasDeLoteInsumo(idLoteInsumo, pageable(pagina));
            model.addAttribute("reservas", paginaReservas);
        }

        model.addAttribute("lote", lote);
        model.addAttribute("pestana", consumos ? PESTANA_CONSUMOS : PESTANA_RESERVAS);
        return VISTA_DETALLE_LOTE;
    }

    private static PageRequest pageable(int pagina) {
        return PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA);
    }
}
