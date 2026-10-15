package com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.presentation.controller.RetornoContextual;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.OllaHervorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IOllaHervorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OllaHervorResponseDTO;
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
 * Controller de la gestión de ollas de hervor, una de las pestañas de "Gestión de Equipamiento". El alta, la
 * modificación y la baja son del GERENTE_DE_PRODUCCION, que define el equipamiento productivo; el
 * estado operativo no se carga a mano: lo cambian las operaciones de producción y de ciclo de vida.
 * <p>
 * Si el alta se abre desde el selector de otro formulario ({@link RetornoContextual}), al guardar se
 * vuelve a ese formulario con la olla de hervor creada ya elegida.
 * </p>
 */
@Controller
@RequestMapping("/equipamiento/ollas-hervor")
@RequiredArgsConstructor
public class OllaHervorController {

    private static final String VISTA_LISTA = "equipamiento/olla-lista";
    private static final String VISTA_FORMULARIO = "equipamiento/olla-form";
    private static final String ATRIBUTO_FORMULARIO = "ollaHervorForm";
    private static final String URL_LISTA = "/equipamiento/ollas-hervor";

    private final IOllaHervorServicio ollaHervorServicio;

    /**
     * Lista las ollas de hervor activas, paginadas de a 20, en orden alfabético por identificador. Los
     * criterios (identificador y estado operativo) son opcionales.
     */
    @GetMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String listarOllasHervor(@RequestParam(required = false) String identificador,
                       @RequestParam(required = false) EstadoOperativo estadoOperativo,
                       @RequestParam(defaultValue = "0") int pagina,
                       Model model) {
        Page<OllaHervorResponseDTO> ollas = ollaHervorServicio.filtrarOllasHervor(
                vacioANulo(identificador), estadoOperativo, PageRequest.of(Paginacion.numeroDePagina(pagina), TAMANIO_PAGINA, ORDEN_POR_IDENTIFICADOR));

        model.addAttribute("ollas", ollas);
        model.addAttribute("estados", EstadoOperativo.values());
        model.addAttribute("filtroIdentificador", identificador);
        model.addAttribute("filtroEstado", estadoOperativo);
        return VISTA_LISTA;
    }

    /**
     * Muestra el formulario vacío para dar de alta una olla de hervor.
     */
    @GetMapping("/nuevo")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioAlta(@RequestParam(required = false) String retorno,
                                        @RequestParam(required = false) String campo,
                                        Model model) {
        model.addAttribute(ATRIBUTO_FORMULARIO, new OllaHervorFormDTO());
        cargarModeloFormulario(model, null, retorno, campo);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa el alta de la olla de hervor (Post-Redirect-Get). Si la validación del formulario o una regla del
     * service falla, se vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String altaOllaHervor(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) OllaHervorFormDTO ollaHervorForm,
                       BindingResult bindingResult,
                       @RequestParam(required = false) String retorno,
                       @RequestParam(required = false) String campo,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                OllaHervorResponseDTO creado = ollaHervorServicio.altaOllaHervor(ollaHervorForm);
                agregarMensaje(redirectAttributes, "Olla de hervor creada correctamente", TipoAlerta.SUCCESS);
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
     * Muestra el formulario de edición con los datos actuales de la olla de hervor.
     */
    @GetMapping("/{id}/editar")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String mostrarFormularioEdicion(@PathVariable Long id, Model model) {
        OllaHervorResponseDTO actual = ollaHervorServicio.buscarPorId(id);

        model.addAttribute(ATRIBUTO_FORMULARIO, OllaHervorFormDTO.builder()
                .identificadorInterno(actual.getIdentificadorInterno())
                .capacidadTotal(actual.getCapacidadTotal())
                .capacidadUtil(actual.getCapacidadUtil())
                .evaporacion(actual.getEvaporacion())
                .perdidaPorTrub(actual.getPerdidaPorTrub())
                .usosMaximosAntesMantenimiento(actual.getUsosMaximosAntesMantenimiento())
                .build());
        cargarModeloFormulario(model, id, null, null);
        return VISTA_FORMULARIO;
    }

    /**
     * Procesa la modificación de la olla de hervor (Post-Redirect-Get). El service la rechaza si tiene una etapa
     * pendiente de un lote o si está en uso; el motivo se muestra en el formulario.
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String modificarOllaHervor(@PathVariable Long id,
                            @Valid @ModelAttribute(ATRIBUTO_FORMULARIO) OllaHervorFormDTO ollaHervorForm,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                ollaHervorServicio.modificarOllaHervor(id, ollaHervorForm);
                agregarMensaje(redirectAttributes, "Olla de hervor modificada correctamente", TipoAlerta.SUCCESS);
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
     * Da de baja (lógica) a la olla de hervor. Si tiene una etapa en estado Pendiente de un lote o está en uso, el service
     * lo rechaza y el {@code ControllerAdvices} muestra el motivo.
     */
    @PostMapping("/{id}/baja")
    @PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
    public String bajaOllaHervor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        ollaHervorServicio.bajaOllaHervor(id);
        agregarMensaje(redirectAttributes, "Olla de hervor dada de baja correctamente", TipoAlerta.SUCCESS);
        return "redirect:" + URL_LISTA;
    }

    private void cargarModeloFormulario(Model model, Long ollaHervorId, String retorno, String campo) {
        String retornoValido = RetornoContextual.retornoValido(retorno);
        model.addAttribute("ollaHervorId", ollaHervorId);
        model.addAttribute("retorno", retornoValido);
        model.addAttribute("campo", RetornoContextual.campoValido(campo));
        model.addAttribute("urlCancelar", RetornoContextual.urlCancelar(retornoValido, URL_LISTA));
    }
}
