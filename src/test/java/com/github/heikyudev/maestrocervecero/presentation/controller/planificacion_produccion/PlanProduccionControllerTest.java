package com.github.heikyudev.maestrocervecero.presentation.controller.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.PrioridadPlanificacion;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.ModalidadPlanProduccion;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.LotePlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanesProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PlanProduccionController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class PlanProduccionControllerTest {

    private static final String URL = "/planificaciones-produccion/7/planes";
    private static final LocalDate INICIO = LocalDate.of(2027, 7, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IPlanProduccionServicio planProduccionServicio;

    @MockitoBean
    private IPlanificacionProduccionServicio planificacionServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /planificaciones-produccion/{id}/planes ====================

    @Test
    @DisplayName("CP-PLC-01: el GERENTE_DE_PRODUCCION ve el resumen, las pestañas con ícono, los indicadores, la nota de fechas estimadas y la tabla de lotes con los equipos apilados y el botón de ícono \"Modificar lote N\"")
    void planes_debeMostrarLaPantallaCompleta() throws Exception {
        when(planProduccionServicio.generarPlanes(7L)).thenReturn(planes(planSecuencial(), planConcurrente()));
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());

        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/planes"))
                .andExpect(content().string(containsString("Planes de producción")))
                .andExpect(content().string(containsString("Plan #7 - Pale Ale")))
                .andExpect(content().string(containsString("Litros a planificar (L)")))
                .andExpect(content().string(containsString("data-pestana-secuencial")))
                .andExpect(content().string(containsString("data-pestana-concurrente")))
                .andExpect(content().string(containsString("bi-arrow-right-circle")))
                .andExpect(content().string(not(containsString("Criterio de selección de equipos"))))
                .andExpect(content().string(containsString("data-indicador=\"lotes\"")))
                .andExpect(content().string(containsString("Fin del plan")))
                .andExpect(content().string(not(containsString("Las fechas de los lotes son estimadas"))))
                .andExpect(content().string(containsString("name=\"lotes[0].volumenObjetivo\"")))
                .andExpect(content().string(containsString("value=\"500\"")))
                .andExpect(content().string(not(containsString("value=\"500.0\""))))
                .andExpect(content().string(containsString("name=\"lotes[1].idFermentador\" value=\"1\"")))
                .andExpect(content().string(containsString("name=\"modalidad\" value=\"SECUENCIAL\"")))
                .andExpect(content().string(containsString("Equipos</th>")))
                .andExpect(content().string(containsString("data-equipos-lote")))
                .andExpect(content().string(containsString("Olla de hervor:")))
                .andExpect(content().string(containsString("action=\"/planificaciones-produccion/7/planes/confirmar\"")))
                .andExpect(content().string(containsString("Confirmar plan")))
                .andExpect(content().string(containsString("data-url-recalcular=\"/planificaciones-produccion/7/planes/recalcular\"")))
                .andExpect(content().string(containsString("/lotes/buscador/fermentadores")))
                .andExpect(content().string(not(containsString("data-errores-plan"))))
                .andExpect(content().string(not(containsString("data-sin-planes"))))
                .andReturn().getResponse().getContentAsString();

        Matcher boton = Pattern.compile("(?s)<button[^>]*data-modificar-lote[^>]*>(.*?)</button>").matcher(html);
        assertThat(boton.find()).isTrue();
        assertThat(boton.group()).contains("title=\"Modificar lote 1\"").contains("aria-label=\"Modificar lote 1\"")
                .contains("p-2 rounded-lg text-slate-500 hover:bg-amber-50 hover:text-amber-600 transition-colors")
                .contains("<i class=\"bi bi-pencil-square text-lg\"></i>");
        assertThat(boton.group(1).replaceAll("<[^>]*>", "").trim()).isEmpty();
        assertThat(apertura(html, "SECUENCIAL")).doesNotContain("hidden");
        assertThat(apertura(html, "CONCURRENTE")).contains("hidden");
    }

    @Test
    @DisplayName("CP-PLC-02: con modalidad=CONCURRENTE se abre la pestaña concurrente; una modalidad inválida abre la secuencial")
    void planes_debeAbrirLaPestanaIndicada() throws Exception {
        when(planProduccionServicio.generarPlanes(7L)).thenReturn(planes(planSecuencial(), planConcurrente()));
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());

        String concurrente = mockMvc.perform(get(URL).param("modalidad", "CONCURRENTE").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String invalida = mockMvc.perform(get(URL).param("modalidad", "OTRA").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(apertura(concurrente, "CONCURRENTE")).doesNotContain("hidden");
        assertThat(apertura(concurrente, "SECUENCIAL")).contains("hidden");
        assertThat(apertura(invalida, "SECUENCIAL")).doesNotContain("hidden");
    }

    @Test
    @DisplayName("CP-PLC-03: con un solo plan disponible se muestra solo ese plan, sin pestañas ni sección de plan no disponible")
    void planes_unSoloPlanDisponibleSinPestanas() throws Exception {
        when(planProduccionServicio.generarPlanes(7L)).thenReturn(planes(planSecuencial(),
                noDisponible(ModalidadPlanProduccion.CONCURRENTE, "El plan concurrente necesita al menos dos fermentadores activos que puedan recibir un lote")));
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());

        String html = mockMvc.perform(get(URL).param("modalidad", "CONCURRENTE").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Plan secuencial")))
                .andExpect(content().string(not(containsString("data-pestana-"))))
                .andExpect(content().string(not(containsString("data-panel-plan=\"CONCURRENTE\""))))
                .andExpect(content().string(not(containsString("El plan no está disponible"))))
                .andExpect(content().string(not(containsString("El plan concurrente necesita"))))
                .andExpect(content().string(not(containsString("data-sin-planes"))))
                .andReturn().getResponse().getContentAsString();

        assertThat(apertura(html, "SECUENCIAL")).doesNotContain("hidden");
    }

    @Test
    @DisplayName("CP-PLC-13: si solo el plan concurrente está disponible se abre ese plan aunque se pida el secuencial")
    void planes_soloConcurrenteDisponible() throws Exception {
        when(planProduccionServicio.generarPlanes(7L)).thenReturn(planes(
                noDisponible(ModalidadPlanProduccion.SECUENCIAL, "El plan secuencial necesita 200 lotes y el máximo es de 100"), planConcurrente()));
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());

        String html = mockMvc.perform(get(URL).param("modalidad", "SECUENCIAL").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Plan concurrente")))
                .andExpect(content().string(not(containsString("data-panel-plan=\"SECUENCIAL\""))))
                .andExpect(content().string(not(containsString("data-pestana-"))))
                .andReturn().getResponse().getContentAsString();

        assertThat(apertura(html, "CONCURRENTE")).doesNotContain("hidden");
    }

    @Test
    @DisplayName("CP-PLC-14: sin planes disponibles se muestra un único aviso con los motivos, sin pestañas, paneles ni nota de fechas")
    void planes_ningunPlanDisponible() throws Exception {
        when(planProduccionServicio.generarPlanes(7L)).thenReturn(planes(
                noDisponible(ModalidadPlanProduccion.SECUENCIAL, "El plan secuencial necesita 200 lotes y el máximo es de 100"),
                noDisponible(ModalidadPlanProduccion.CONCURRENTE, "El plan concurrente necesita al menos dos fermentadores activos que puedan recibir un lote")));
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());

        mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-sin-planes")))
                .andExpect(content().string(containsString("No se pudo generar ningún plan de producción válido")))
                .andExpect(content().string(containsString("Secuencial: El plan secuencial necesita 200 lotes y el máximo es de 100")))
                .andExpect(content().string(containsString("Concurrente: El plan concurrente necesita al menos dos fermentadores activos")))
                .andExpect(content().string(not(containsString("data-panel-plan"))))
                .andExpect(content().string(not(containsString("data-pestana-"))))
                .andExpect(content().string(not(containsString("data-nota-fechas-estimadas"))));
    }

    @Test
    @DisplayName("CP-PLC-15: una planificación sin litros por planificar (no Pendiente o cubierta) vuelve a su estado actual con el motivo, sin generar planes")
    void planes_sinLitrosAPlanificarVuelveAlDetalle() throws Exception {
        when(planProduccionServicio.buscarMotivoSinLitrosAPlanificar(7L))
                .thenReturn(Optional.of("La planificación ya tiene lotes que cubren la cantidad a producir (1.000 L comprometidos de 1.000 L solicitados)"));

        mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion/7"))
                .andExpect(flash().attribute("tipo", "warning"))
                .andExpect(flash().attribute("mensaje", "La planificación ya tiene lotes que cubren la cantidad a producir (1.000 L comprometidos de 1.000 L solicitados)"));

        verify(planProduccionServicio, never()).generarPlanes(any());
    }

    @Test
    @DisplayName("CP-PLC-04: un rol sin LOTE_REGISTRAR (GERENTE_DE_COMPRAS) es rechazado y no llega al service")
    void planes_rolSinPermisoEsRechazado() throws Exception {
        mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL + "/confirmar").with(csrf()).with(como(Rol.GERENTE_DE_COMPRAS)).param("modalidad", "SECUENCIAL"))
                .andExpect(status().is3xxRedirection());

        verifyNoInteractions(planProduccionServicio);
    }

    // ==================== POST /planificaciones-produccion/{id}/planes/confirmar ====================

    @Test
    @DisplayName("CP-PLC-05: confirmar un plan válido entrega la modalidad y los lotes al service y redirige al estado actual con la cantidad de lotes registrados")
    void confirmar_debeRegistrarYRedirigir() throws Exception {
        when(planProduccionServicio.confirmarPlan(eq(7L), any())).thenReturn(List.of(LoteResponseDTO.builder().build(), LoteResponseDTO.builder().build()));

        mockMvc.perform(conLotes(post(URL + "/confirmar").with(csrf()).with(como(Rol.GERENTE_DE_PRODUCCION)), "CONCURRENTE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion/7"))
                .andExpect(flash().attribute("mensaje", "Plan de producción confirmado: se registraron 2 lotes"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<PlanProduccionFormDTO> captor = ArgumentCaptor.forClass(PlanProduccionFormDTO.class);
        verify(planProduccionServicio).confirmarPlan(eq(7L), captor.capture());
        assertThat(captor.getValue().getModalidad()).isEqualTo(ModalidadPlanProduccion.CONCURRENTE);
        assertThat(captor.getValue().getLotes()).hasSize(2);
        assertThat(captor.getValue().getLotes().get(1).getVolumenObjetivo()).isEqualTo(450.0);
        assertThat(captor.getValue().getLotes().get(1).getIdFermentador()).isEqualTo(2L);
        assertThat(captor.getValue().getLotes().get(0).getIdOllaHervor()).isEqualTo(12L);
    }

    @Test
    @DisplayName("CP-PLC-16: confirmar un plan de un solo lote informa \"se registró 1 lote\" en singular")
    void confirmar_unSoloLoteEnSingular() throws Exception {
        when(planProduccionServicio.confirmarPlan(eq(7L), any())).thenReturn(List.of(LoteResponseDTO.builder().build()));

        mockMvc.perform(conLotes(post(URL + "/confirmar").with(csrf()).with(como(Rol.GERENTE_DE_PRODUCCION)), "SECUENCIAL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "Plan de producción confirmado: se registró 1 lote"));
    }

    @Test
    @DisplayName("CP-PLC-06: un lote con un dato faltante o mal escrito redirige a los planes (Post-Redirect-Get) con los motivos y su número de lote, sin confirmar ni recalcular")
    void confirmar_erroresDelFormularioConNumeroDeLote() throws Exception {
        MvcResult resultado = mockMvc.perform(post(URL + "/confirmar").with(csrf()).with(como(Rol.GERENTE_DE_PRODUCCION))
                        .param("modalidad", "SECUENCIAL")
                        .param("lotes[0].volumenObjetivo", "500").param("lotes[0].idFermentador", "1")
                        .param("lotes[0].idMacerador", "11").param("lotes[0].idOllaHervor", "12")
                        .param("lotes[1].volumenObjetivo", "abc").param("lotes[1].idFermentador", "1").param("lotes[1].idMolino", "10")
                        .param("lotes[1].idMacerador", "11").param("lotes[1].idOllaHervor", "12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL + "?modalidad=SECUENCIAL"))
                .andExpect(flash().attributeCount(1))
                .andReturn();
        @SuppressWarnings("unchecked")
        List<String> errores = (List<String>) resultado.getFlashMap().get(PlanProduccionController.FLASH_ERRORES);
        assertThat(errores).contains("Lote 1 del plan: El molino es obligatorio", "Lote 2 del plan: Ingrese un número válido");

        // La pantalla de planes muestra los motivos (el GET que sigue a la redirección)
        when(planProduccionServicio.generarPlanes(7L)).thenReturn(planes(planSecuencial(), planConcurrente()));
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());
        mockMvc.perform(get(URL).param("modalidad", "SECUENCIAL").flashAttrs(resultado.getFlashMap()).with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo confirmar el plan")))
                .andExpect(content().string(containsString("Lote 2 del plan: Ingrese un número válido")));

        verify(planProduccionServicio, never()).confirmarPlan(any(), any());
        verify(planProduccionServicio, never()).recalcularPlan(any(), any());
    }

    @Test
    @DisplayName("CP-PLC-07: si el service rechaza la confirmación, redirige a los planes con el motivo y el plan enviado, que se muestra recalculado en su pestaña tal como el usuario lo modificó")
    void confirmar_rechazoDelServiceConservaElPlanModificado() throws Exception {
        when(planProduccionServicio.confirmarPlan(eq(7L), any()))
                .thenThrow(new ReglaNegocioException("Lote 2 del plan: El volumen objetivo (600 L) supera la capacidad útil del fermentador seleccionado (500 L)"));

        MvcResult resultado = mockMvc.perform(conLotes(post(URL + "/confirmar").with(csrf()).with(como(Rol.GERENTE_DE_PRODUCCION)), "CONCURRENTE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL + "?modalidad=CONCURRENTE"))
                .andExpect(flash().attribute(PlanProduccionController.FLASH_ERRORES,
                        List.of("Lote 2 del plan: El volumen objetivo (600 L) supera la capacidad útil del fermentador seleccionado (500 L)")))
                .andReturn();
        assertThat(resultado.getFlashMap().get(PlanProduccionController.FLASH_PLAN_ENVIADO)).isInstanceOf(PlanProduccionFormDTO.class);

        when(planProduccionServicio.generarPlanes(7L)).thenReturn(planes(planSecuencial(), planConcurrente()));
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion());
        when(planProduccionServicio.recalcularPlan(eq(7L), any())).thenReturn(plan(ModalidadPlanProduccion.CONCURRENTE,
                lote(1, 550.0, 1L, "FER-1"), lote(2, 600.5, 2L, "FER-EDITADO")));

        String html = mockMvc.perform(get(URL).param("modalidad", "CONCURRENTE").flashAttrs(resultado.getFlashMap()).with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo confirmar el plan")))
                .andExpect(content().string(containsString("Lote 2 del plan: El volumen objetivo (600 L) supera la capacidad útil del fermentador seleccionado (500 L)")))
                .andReturn().getResponse().getContentAsString();

        assertThat(apertura(html, "CONCURRENTE")).doesNotContain("hidden");
        assertThat(panel(html, "CONCURRENTE")).contains("FER-EDITADO").contains("value=\"600.5\"");
        assertThat(apertura(html, "SECUENCIAL")).contains("hidden");
    }

    @Test
    @DisplayName("CP-PLC-11: si la confirmación falla por un bloqueo de otra transacción, redirige a los planes con un mensaje legible y el plan enviado")
    void confirmar_bloqueoDeOtraTransaccion() throws Exception {
        when(planProduccionServicio.confirmarPlan(eq(7L), any())).thenThrow(new CannotAcquireLockException("could not obtain lock"));

        MvcResult resultado = mockMvc.perform(conLotes(post(URL + "/confirmar").with(csrf()).with(como(Rol.GERENTE_DE_PRODUCCION)), "SECUENCIAL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL + "?modalidad=SECUENCIAL"))
                .andExpect(flash().attribute(PlanProduccionController.FLASH_ERRORES,
                        List.of("Otro usuario está registrando lotes sobre los mismos fermentadores, intente nuevamente")))
                .andReturn();

        assertThat(resultado.getFlashMap().get(PlanProduccionController.FLASH_PLAN_ENVIADO)).isNotNull();
    }

    @Test
    @DisplayName("CP-PLC-12: si al volver a mostrar la pantalla tras un rechazo la planificación ya no existe, se vuelve al listado con todos los motivos")
    void planes_planificacionInexistenteTrasUnRechazo() throws Exception {
        when(planProduccionServicio.buscarMotivoSinLitrosAPlanificar(7L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: 7"));

        mockMvc.perform(get(URL).flashAttr(PlanProduccionController.FLASH_ERRORES, List.of("Lote 1 del plan: El molino es obligatorio"))
                        .with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion"))
                .andExpect(flash().attribute("tipo", "danger"))
                .andExpect(flash().attribute("mensaje", "Lote 1 del plan: El molino es obligatorio - No se encontró la planificación de producción con ID: 7"));
    }

    @Test
    @DisplayName("CP-PLC-17: confirmar una planificación sin litros por planificar vuelve a su estado actual con el motivo y no llega a confirmarse")
    void confirmar_sinLitrosAPlanificarVuelveAlDetalle() throws Exception {
        when(planProduccionServicio.buscarMotivoSinLitrosAPlanificar(7L))
                .thenReturn(Optional.of("La planificación de producción debe encontrarse en estado Pendiente para generar o confirmar un plan de producción"));

        mockMvc.perform(conLotes(post(URL + "/confirmar").with(csrf()).with(como(Rol.GERENTE_DE_PRODUCCION)), "SECUENCIAL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion/7"))
                .andExpect(flash().attribute("tipo", "warning"));

        verify(planProduccionServicio, never()).confirmarPlan(any(), any());
    }

    @Test
    @DisplayName("CP-PLC-08: confirmar sin token CSRF es rechazado y no llega al service")
    void confirmar_sinCsrfEsRechazado() throws Exception {
        mockMvc.perform(conLotes(post(URL + "/confirmar").with(como(Rol.GERENTE_DE_PRODUCCION)), "SECUENCIAL"))
                .andExpect(status().is3xxRedirection());

        verifyNoInteractions(planProduccionServicio);
    }

    // ==================== POST /planificaciones-produccion/{id}/planes/recalcular ====================

    @Test
    @DisplayName("CP-PLC-09: recalcular devuelve solo el fragmento del plan recalculado con los lotes del formulario")
    void recalcular_debeDevolverElFragmentoDelPlan() throws Exception {
        when(planProduccionServicio.recalcularPlan(eq(7L), any())).thenReturn(
                plan(ModalidadPlanProduccion.SECUENCIAL, lote(1, 450.0, 1L, "FER-1"), lote(2, 550.0, 1L, "FER-1")));

        mockMvc.perform(conLotes(post(URL + "/recalcular").with(csrf()).with(como(Rol.GERENTE_DE_PRODUCCION)), "SECUENCIAL"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-plan")))
                .andExpect(content().string(containsString("value=\"550\"")))
                .andExpect(content().string(containsString("Confirmar plan")))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("data-error-recalculo"))));

        ArgumentCaptor<PlanProduccionFormDTO> captor = ArgumentCaptor.forClass(PlanProduccionFormDTO.class);
        verify(planProduccionServicio).recalcularPlan(eq(7L), captor.capture());
        assertThat(captor.getValue().getLotes()).hasSize(2);
    }

    @Test
    @DisplayName("CP-PLC-10: si recalcular falla por el formulario o por el service, devuelve el fragmento de error con los motivos")
    void recalcular_debeDevolverElFragmentoDeError() throws Exception {
        when(planProduccionServicio.recalcularPlan(eq(7L), any()))
                .thenThrow(new ReglaNegocioException("Lote 1 del plan: No se encontró el fermentador con ID: 99"));

        mockMvc.perform(conLotes(post(URL + "/recalcular").with(csrf()).with(como(Rol.GERENTE_DE_PRODUCCION)), "SECUENCIAL"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-error-recalculo")))
                .andExpect(content().string(not(containsString("data-redirigir"))))
                .andExpect(content().string(containsString("Lote 1 del plan: No se encontró el fermentador con ID: 99")));

        mockMvc.perform(post(URL + "/recalcular").with(csrf()).with(como(Rol.GERENTE_DE_PRODUCCION))
                        .param("modalidad", "SECUENCIAL").param("lotes[0].idFermentador", "1").param("lotes[0].idMolino", "10")
                        .param("lotes[0].idMacerador", "11").param("lotes[0].idOllaHervor", "12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-error-recalculo")))
                .andExpect(content().string(containsString("Lote 1 del plan: El volumen objetivo es obligatorio")));
    }

    @Test
    @DisplayName("CP-PLC-18: recalcular una planificación sin litros por planificar devuelve el fragmento de error que indica volver a la pantalla de planes")
    void recalcular_sinLitrosAPlanificar() throws Exception {
        when(planProduccionServicio.buscarMotivoSinLitrosAPlanificar(7L))
                .thenReturn(Optional.of("La planificación ya tiene lotes que cubren la cantidad a producir (1.000 L comprometidos de 1.000 L solicitados)"));

        mockMvc.perform(conLotes(post(URL + "/recalcular").with(csrf()).with(como(Rol.GERENTE_DE_PRODUCCION)), "SECUENCIAL"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-error-recalculo")))
                .andExpect(content().string(containsString("data-redirigir=\"/planificaciones-produccion/7/planes\"")))
                .andExpect(content().string(containsString("La planificación ya tiene lotes que cubren la cantidad a producir")));

        verify(planProduccionServicio, never()).recalcularPlan(any(), any());
    }

    // ==================== Helpers ====================

    /** Etiqueta de apertura del panel de una modalidad (lleva hidden si el panel está oculto). */
    private static String apertura(String html, String modalidad) {
        Matcher matcher = Pattern.compile("<div data-panel-plan=\"" + modalidad + "\"[^>]*>").matcher(html);
        assertThat(matcher.find()).as("panel " + modalidad).isTrue();
        return matcher.group();
    }

    /** Contenido del panel de una modalidad: desde su apertura hasta el siguiente panel o el modal. */
    private static String panel(String html, String modalidad) {
        Matcher matcher = Pattern.compile("(?s)<div data-panel-plan=\"" + modalidad + "\"(.*?)(?=<div data-panel-plan=|<dialog id=\"dialogo-lote-plan\")").matcher(html);
        assertThat(matcher.find()).as("panel " + modalidad).isTrue();
        return matcher.group();
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder conLotes(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder pedido, String modalidad) {
        return pedido.param("modalidad", modalidad)
                .param("lotes[0].volumenObjetivo", "550").param("lotes[0].idFermentador", "1").param("lotes[0].idMolino", "10")
                .param("lotes[0].idMacerador", "11").param("lotes[0].idOllaHervor", "12")
                .param("lotes[1].volumenObjetivo", "450").param("lotes[1].idFermentador", "2").param("lotes[1].idMolino", "10")
                .param("lotes[1].idMacerador", "11").param("lotes[1].idOllaHervor", "12");
    }

    private static PlanesProduccionResponseDTO planes(PlanProduccionResponseDTO secuencial, PlanProduccionResponseDTO concurrente) {
        return PlanesProduccionResponseDTO.builder()
                .idPlanificacion(7L).litrosAPlanificar(1000.0).excedenteMaximoLitros(300.0)
                .secuencial(secuencial).concurrente(concurrente)
                .build();
    }

    private static PlanProduccionResponseDTO noDisponible(ModalidadPlanProduccion modalidad, String motivo) {
        return PlanProduccionResponseDTO.builder().modalidad(modalidad).disponible(false).motivoNoDisponible(motivo).build();
    }

    private static PlanProduccionResponseDTO planSecuencial() {
        return plan(ModalidadPlanProduccion.SECUENCIAL, lote(1, 500.0, 1L, "FER-1"), lote(2, 500.0, 1L, "FER-1"));
    }

    private static PlanProduccionResponseDTO planConcurrente() {
        return plan(ModalidadPlanProduccion.CONCURRENTE, lote(1, 550.0, 1L, "FER-1"), lote(2, 450.0, 2L, "FER-2"));
    }

    private static PlanProduccionResponseDTO plan(ModalidadPlanProduccion modalidad, LotePlanProduccionResponseDTO... lotes) {
        List<LotePlanProduccionResponseDTO> lista = new ArrayList<>(List.of(lotes));
        double total = lista.stream().mapToDouble(LotePlanProduccionResponseDTO::getVolumenObjetivo).sum();
        return PlanProduccionResponseDTO.builder()
                .modalidad(modalidad).disponible(true).lotes(lista)
                .litrosAPlanificar(1000.0).cantidadLotes(lista.size()).volumenTotal(total).excedente(Math.max(0, total - 1000.0))
                .fermentadoresUtilizados((int) lista.stream().map(LotePlanProduccionResponseDTO::getIdFermentador).distinct().count())
                .fechaFinalizacionPlan(INICIO.plusDays(46))
                .build();
    }

    private static LotePlanProduccionResponseDTO lote(int orden, double volumen, Long idFermentador, String fermentador) {
        return LotePlanProduccionResponseDTO.builder()
                .orden(orden).volumenObjetivo(volumen)
                .idFermentador(idFermentador).identificadorFermentador(fermentador).capacidadFermentador(500.0)
                .idMolino(10L).identificadorMolino("MOL-10")
                .idMacerador(11L).identificadorMacerador("MAC-11")
                .idOllaHervor(12L).identificadorOllaHervor("OLL-12")
                .fechaInicioEstimada(INICIO.plusDays((orden - 1) * 24L))
                .fechaFinalizacionEstimada(INICIO.plusDays((orden - 1) * 24L + 22))
                .build();
    }

    private static PlanificacionProduccionResponseDTO planificacion() {
        return PlanificacionProduccionResponseDTO.builder()
                .id(7L)
                .fechaInicioEstimada(INICIO)
                .fechaFinalizacionEstimada(LocalDate.of(2027, 9, 30))
                .cantidadAProducir(1000.0)
                .estado(EstadoSolicitud.PENDIENTE)
                .prioridad(PrioridadPlanificacion.ALTA)
                .versionReceta(VersionRecetaResponseDTO.builder().id(70L).nombre("Pale Ale").volumenBase(20.0).build())
                .build();
    }

    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }
}
