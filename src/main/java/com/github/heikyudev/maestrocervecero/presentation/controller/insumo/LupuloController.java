package com.github.heikyudev.maestrocervecero.presentation.controller.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.presentation.controller.RetornoContextual;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.LupuloFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILupuloServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.github.heikyudev.maestrocervecero.util.Paginacion;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.*;

/**
 * Controller de la gestión de lúpulos, una de las tres pestañas de "Gestión de Insumos". Todo el
 * módulo es del GERENTE_DE_PRODUCCION, que define los insumos que intervienen en el sistema.
 * <p>
 * Si el alta se abre desde el selector de otro formulario ({@link RetornoContextual}), al guardar se
 * vuelve a ese formulario con el lúpulo creado ya elegido.
 * </p>
 */
@Controller
@RequestMapping("/insumos/lupulos")
@RequiredArgsConstructor
public class LupuloController {

    private static final String VISTA_LISTA = "insumo/lupulo-lista";
    private static final String VISTA_FORMULARIO = "insumo/lupulo-form";
    private static final String ATRIBUTO_FORMULARIO = "lupuloForm";
    private static final String URL_LISTA = "/insumos/lupulos";

    private final ILupuloServicio lupuloServicio;

    /**
     * Lista los lúpulos activos, paginados de a 20, en orden alfabético. Los criterios son opcionales.
     */
    @GetMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String listarLupulos(@RequestParam(required = false) String nombre,
                                @RequestParam(required = false) FormatoLupulo formato,
                                @RequestParam(defaultValue = "0") int pagina,
                                Model model) {
        Page<LupuloResponseDTO> lupulos = lupuloServicio.filtrarLupulos(
                vacioANulo(nombre), formato, PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_ALFABETICO));

        model.addAttribute("lupulos", lupulos);
        model.addAttribute("formatos", FormatoLupulo.values());
        model.addAttribute("filtroNombre", nombre);
        model.addAttribute("filtroFormato", formato);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta un lúpulo.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioAlta(@RequestParam(required = false) String retorno,
                                        @RequestParam(required = false) String campo,
                                        Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new LupuloFormDTO());
        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de un lúpulo (Post-Redirect-Get). Si la validación del formulario o una regla
     * del service falla, se vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String altaLupulo(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) LupuloFormDTO lupuloForm,
                             BindingResult bindingResult,
                             @RequestParam(required = false) String retorno,
                             @RequestParam(required = false) String campo,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                LupuloResponseDTO lupulo = lupuloServicio.altaLupulo(lupuloForm);
                agregarMensaje(redirectAttributes, "Lúpulo creado correctamente", TipoAlerta.SUCCESS);
                String vuelta = RetornoContextual.urlDeVuelta(retorno, campo, lupulo.getId());
                return "redirect:" + (vuelta != null ? vuelta : URL_LISTA);
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.rejectValue("aa", "reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de edición con los datos actuales del lúpulo.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        LupuloResponseDTO lupulo = lupuloServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, LupuloFormDTO.builder()
                .nombre(lupulo.getNombre())
                .formato(lupulo.getFormato())
                .aa(lupulo.getAa())
                .build());
        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de un lúpulo (Post-Redirect-Get).
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String modificarLupulo(@PathVariable Long id,
                                  @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) LupuloFormDTO lupuloForm,
                                  BindingResult bindingResult,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                lupuloServicio.modificarLupulo(id, lupuloForm);
                agregarMensaje(redirectAttributes, "Lúpulo modificado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("nombre", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.rejectValue("aa", "reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) a un lúpulo. Si forma parte de una receta activa o tiene stock el service
     * lo rechaza y el {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String bajaLupulo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        lupuloServicio.bajaLupulo(id);
        agregarMensaje(redirectAttributes, "Lúpulo dado de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    private void cargarModeloFormulario(Model model, Long lupuloId, String retorno, String campo) {
        String retornoValido = RetornoContextual.retornoValido(retorno);
        model.addAttribute("lupuloId", lupuloId);
        model.addAttribute("formatos", FormatoLupulo.values());
        model.addAttribute("retorno", retornoValido);
        model.addAttribute("campo", RetornoContextual.campoValido(campo));
        model.addAttribute("urlCancelar", RetornoContextual.urlCancelar(retornoValido, URL_LISTA));
    }
}
