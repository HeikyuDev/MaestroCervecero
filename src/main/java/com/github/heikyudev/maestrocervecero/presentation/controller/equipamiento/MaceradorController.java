package com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.presentation.controller.RetornoContextual;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MaceradorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMaceradorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MaceradorResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
 * Controller de la gestión de maceradores, una de las pestañas de "Gestión de Equipamiento". El alta, la
 * modificación y la baja son del GERENTE_DE_PRODUCCION, que define el equipamiento productivo; el
 * estado operativo no se carga a mano: lo cambian las operaciones de producción y de ciclo de vida.
 * <p>
 * Si el alta se abre desde el selector de otro formulario ({@link RetornoContextual}), al guardar se
 * vuelve a ese formulario con el macerador creado ya elegido.
 * </p>
 */
@Controller
@RequestMapping("/equipamiento/maceradores")
@RequiredArgsConstructor
public class MaceradorController {

    private static final String VISTA_LISTA = "equipamiento/macerador-lista";
    private static final String VISTA_FORMULARIO = "equipamiento/macerador-form";
    private static final String ATRIBUTO_FORMULARIO = "maceradorForm";
    private static final String URL_LISTA = "/equipamiento/maceradores";

    private final IMaceradorServicio maceradorServicio;

    /**
     * Lista los maceradores activos, paginados de a 20, en orden alfabético por identificador. Los
     * criterios (identificador y estado operativo) son opcionales.
     */
    @GetMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String listarMaceradores(@RequestParam(required = false) String identificador,
                       @RequestParam(required = false) EstadoOperativo estadoOperativo,
                       @RequestParam(defaultValue = "0") int pagina,
                       Model model) {
        Page<MaceradorResponseDTO> maceradores = maceradorServicio.filtrarMaceradores(
                vacioANulo(identificador), estadoOperativo, PageRequest.of(Math.max(pagina, 0), TAMANIO_PAGINA, ORDEN_POR_IDENTIFICADOR));

        model.addAttribute("maceradores", maceradores);
        model.addAttribute("estados", EstadoOperativo.values());
        model.addAttribute("filtroIdentificador", identificador);
        model.addAttribute("filtroEstado", estadoOperativo);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta un macerador.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioAlta(@RequestParam(required = false) String retorno,
                                        @RequestParam(required = false) String campo,
                                        Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new MaceradorFormDTO());
        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta del macerador (Post-Redirect-Get). Si la validación del formulario o una regla del
     * service falla, se vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String altaMacerador(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) MaceradorFormDTO maceradorForm,
                       BindingResult bindingResult,
                       @RequestParam(required = false) String retorno,
                       @RequestParam(required = false) String campo,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                MaceradorResponseDTO creado = maceradorServicio.altaMacerador(maceradorForm);
                agregarMensaje(redirectAttributes, "Macerador creado correctamente", TipoAlerta.SUCCESS);
                String vuelta = RetornoContextual.urlDeVuelta(retorno, campo, creado.getId());
                return "redirect:" + (vuelta != null ? vuelta : URL_LISTA);
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("identificadorInterno", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Muestra el formulario de edición con los datos actuales del macerador.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        MaceradorResponseDTO actual = maceradorServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, MaceradorFormDTO.builder()
                .identificadorInterno(actual.getIdentificadorInterno())
                .descripcion(actual.getDescripcion())
                .capacidadTotal(actual.getCapacidadTotal())
                .capacidadUtil(actual.getCapacidadUtil())
                .espacioMuerto(actual.getEspacioMuerto())
                .eficienciaMaceracion(actual.getEficienciaMaceracion())
                .usosMaximosAntesMantenimiento(actual.getUsosMaximosAntesMantenimiento())
                .build());
        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación del macerador (Post-Redirect-Get). El service la rechaza si tiene una etapa
     * pendiente de un lote o si está en uso; el motivo se muestra en el formulario.
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String modificarMacerador(@PathVariable Long id,
                            @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) MaceradorFormDTO maceradorForm,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                maceradorServicio.modificarMacerador(id, maceradorForm);
                agregarMensaje(redirectAttributes, "Macerador modificado correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_LISTA;
            } catch (RecursoDuplicadoException e) {
                bindingResult.rejectValue("identificadorInterno", "duplicado", e.getMessage());
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Da de baja (lógica) al macerador. Si tiene una etapa pendiente de un lote o está en uso, el service
     * lo rechaza y el {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String bajaMacerador(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        maceradorServicio.bajaMacerador(id);
        agregarMensaje(redirectAttributes, "Macerador dado de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    private void cargarModeloFormulario(Model model, Long maceradorId, String retorno, String campo) {
        String retornoValido = RetornoContextual.retornoValido(retorno);
        model.addAttribute("maceradorId", maceradorId);
        model.addAttribute("retorno", retornoValido);
        model.addAttribute("campo", RetornoContextual.campoValido(campo));
        model.addAttribute("urlCancelar", RetornoContextual.urlCancelar(retornoValido, URL_LISTA));
    }
}
