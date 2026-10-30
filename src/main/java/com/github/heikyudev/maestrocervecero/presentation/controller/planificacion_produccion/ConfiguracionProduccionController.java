package com.github.heikyudev.maestrocervecero.presentation.controller.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanConcurrente;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanSecuencial;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.ConfiguracionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IConfiguracionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.ConfiguracionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.util.TipoAlerta;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.github.heikyudev.maestrocervecero.presentation.controller.MetodosControlador.agregarMensaje;

/**
 * Controller de la pantalla "Configuración de Producción": una única página con los parámetros
 * generales de producción vigentes, editables por el GERENTE_DE_PRODUCCION. No es un CRUD: la
 * configuración es un registro único que siempre existe, así que solo se consulta y se actualiza
 * por completo.
 * <p>
 * Es la pestaña "Parámetros de producción" de Configuración de Producción; la pestaña "Costos directos adicionales" la atiende
 * {@code CostoDirectoAdicionalController}.
 * </p>
 */
@Controller
@RequestMapping("/configuracion-produccion")
@PreAuthorize("hasRole('GERENTE_DE_PRODUCCION')")
@RequiredArgsConstructor
public class ConfiguracionProduccionController {

    private static final String VISTA_FORMULARIO = "configuracion/configuracion-produccion";
    private static final String ATRIBUTO_FORMULARIO = "configuracionForm";
    private static final String URL_CONFIGURACION = "/configuracion-produccion";

    private final IConfiguracionProduccionServicio configuracionProduccionServicio;

    /**
     * Muestra la configuración vigente en un formulario editable.
     */
    @GetMapping
    public String mostrarConfiguracion(Model model) {
        ConfiguracionProduccionResponseDTO actual = configuracionProduccionServicio.buscarConfiguracion();

        model.addAttribute(ATRIBUTO_FORMULARIO, ConfiguracionProduccionFormDTO.builder()
                .velocidadEstandarEnvasado(actual.getVelocidadEstandarEnvasado())
                .capacidadLoteEstandar(actual.getCapacidadLoteEstandar())
                .porcentajeMinimoConsumoParaAvanzarEtapa(actual.getPorcentajeMinimoConsumoParaAvanzarEtapa())
                .criterioSeleccionPlanSecuencial(actual.getCriterioSeleccionPlanSecuencial())
                .criterioSeleccionPlanConcurrente(actual.getCriterioSeleccionPlanConcurrente())
                .build());
        cargarModeloFormulario(model, actual);
        return VISTA_FORMULARIO;
    }

    /**
     * Guarda la configuración (Post-Redirect-Get). Si la validación del formulario o una regla del
     * service falla, se vuelve a renderizar el formulario (sin redirigir) para no perder lo tipeado.
     */
    @PostMapping
    public String actualizarConfiguracion(@Valid @ModelAttribute(ATRIBUTO_FORMULARIO) ConfiguracionProduccionFormDTO configuracionForm,
                                          BindingResult bindingResult,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        if (!bindingResult.hasErrors()) {
            try {
                configuracionProduccionServicio.actualizarConfiguracion(configuracionForm);
                agregarMensaje(redirectAttributes, "Configuración de producción actualizada correctamente", TipoAlerta.SUCCESS);
                return "redirect:" + URL_CONFIGURACION;
            } catch (ReglaNegocioException e) {
                bindingResult.reject("reglaNegocio", e.getMessage());
            }
        }

        cargarModeloFormulario(model, configuracionProduccionServicio.buscarConfiguracion());
        return VISTA_FORMULARIO;
    }

    private void cargarModeloFormulario(Model model, ConfiguracionProduccionResponseDTO vigente) {
        model.addAttribute("vigente", vigente);
        model.addAttribute("criteriosSecuenciales", CriterioSeleccionPlanSecuencial.values());
        model.addAttribute("criteriosConcurrentes", CriterioSeleccionPlanConcurrente.values());
    }
}
