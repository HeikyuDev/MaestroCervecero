package com.github.heikyudev.maestrocervecero.presentation.controller.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.entity.lote.EstadoLote;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.AnulacionPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.FinalizacionForzadaPlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.PlanificacionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.lote.ILoteServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IPlanificacionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.lote.LoteCronogramaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.PlanificacionProduccionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.SugerenciaFechaFinalizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.VersionRecetaDePlanificacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLevaduraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleLupuloResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.DetalleMaltaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.VersionRecetaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

@WebMvcTest(PlanificacionProduccionController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class PlanificacionProduccionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IPlanificacionProduccionServicio planificacionServicio;

    @MockitoBean
    private IRecetaServicio recetaServicio;

    @MockitoBean
    private ILoteServicio loteServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /planificaciones-produccion ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-01: el GERENTE_DE_PRODUCCION ve el listado con las columnas, los datos formateados, el estado legible y las acciones de una planificación pendiente")
    void listado_debeMostrarLasPlanificacionesParaGerenteDeProduccion() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/planificaciones-produccion"))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/planificacion-lista"))
                .andExpect(content().string(containsString("Planificación de Producción")))
                .andExpect(content().string(containsString("Receta</th>")))
                .andExpect(content().string(containsString("Cantidad a producir (L)")))
                .andExpect(content().string(containsString("Inicio estimado")))
                .andExpect(content().string(containsString("Fin estimado")))
                .andExpect(content().string(containsString("Estado</th>")))
                .andExpect(content().string(not(containsString("Lotes</th>"))))
                .andExpect(content().string(containsString("Pale Ale")))
                .andExpect(content().string(containsString(">600<")))
                .andExpect(content().string(not(containsString("600.0"))))
                .andExpect(content().string(containsString("01/07/2027")))
                .andExpect(content().string(containsString("25/07/2027")))
                .andExpect(content().string(matchesPattern("(?s).*bg-blue-50 text-blue-700[^>]*>\\s*Pendiente\\s*<.*")))
                .andExpect(content().string(not(containsString(">PENDIENTE<"))))
                .andExpect(content().string(containsString("href=\"/planificaciones-produccion/nueva\"")))
                .andExpect(content().string(containsString("Nueva planificación")))
                .andExpect(content().string(containsString("href=\"/planificaciones-produccion/7\"")))
                .andExpect(content().string(containsString("title=\"Ver estado actual\"")))
                .andExpect(content().string(containsString("bi-eye")))
                .andExpect(content().string(containsString("data-motivo-dialogo=\"dialogo-finalizar\"")))
                .andExpect(content().string(containsString("data-motivo-url=\"/planificaciones-produccion/7/finalizar\"")))
                .andExpect(content().string(containsString("bi-check2-circle")))
                .andExpect(content().string(containsString("data-motivo-dialogo=\"dialogo-anular\"")))
                .andExpect(content().string(containsString("data-motivo-url=\"/planificaciones-produccion/7/anular\"")))
                .andExpect(content().string(containsString("data-motivo-nombre=\"Pale Ale\"")))
                .andExpect(content().string(containsString("bi-x-circle")))
                // La tabla se desplaza horizontalmente dentro de su tarjeta y las celdas no se parten
                .andExpect(content().string(containsString("overflow-x-auto")))
                .andExpect(content().string(containsString("whitespace-nowrap")))
                // Debajo de xl, Inicio y Fin comparten una celda y las columnas completas se muestran desde xl
                .andExpect(content().string(containsString("title=\"Inicio estimado y fin estimado\"")))
                .andExpect(content().string(containsString("hidden xl:table-cell")))
                .andExpect(content().string(containsString("aria-label=\"Finalizar planificación\"")))
                .andExpect(content().string(containsString("aria-label=\"Anular planificación\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-02: Finalizar y Anular aparecen solo en las filas Pendiente; las Finalizada y Anulada solo tienen \"Ver estado actual\"")
    void listado_losBotonesDeCierreSoloAparecenEnPendientes() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE),
                        planificacion(8L, "Stout", EstadoSolicitud.FINALIZADA),
                        planificacion(9L, "Lager", EstadoSolicitud.ANULADA)), PageRequest.of(0, 20), 3));

        mockMvc.perform(get("/planificaciones-produccion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/planificaciones-produccion/8\"")))
                .andExpect(content().string(containsString("href=\"/planificaciones-produccion/9\"")))
                .andExpect(content().string(containsString("data-motivo-url=\"/planificaciones-produccion/7/finalizar\"")))
                .andExpect(content().string(not(containsString("/planificaciones-produccion/8/finalizar"))))
                .andExpect(content().string(not(containsString("/planificaciones-produccion/8/anular"))))
                .andExpect(content().string(not(containsString("/planificaciones-produccion/9/finalizar"))))
                .andExpect(content().string(not(containsString("/planificaciones-produccion/9/anular"))))
                .andExpect(content().string(containsString(">Finalizada<")))
                .andExpect(content().string(containsString(">Anulada<")))
                .andExpect(content().string(not(containsString(">FINALIZADA<"))))
                .andExpect(content().string(not(containsString(">ANULADA<"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-03: sin filtros pide páginas de 20, página 0, con la última registrada primero (id descendente) y los tres criterios en null")
    void listado_debePedirPaginasDe20ConLaUltimaRegistradaPrimero() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/planificaciones-produccion")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(planificacionServicio).filtrarPlanificacionesProduccion(isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getSort()).isEqualTo(Sort.by(Sort.Order.desc("id")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-04: los filtros de receta, estado y fecha de inicio llegan al service, y el listado muestra la receta elegida y la fecha")
    void listado_debePasarLosFiltrosAlService() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));
        when(recetaServicio.buscarPorId(3L)).thenReturn(receta(3L, "Pale Ale"));

        mockMvc.perform(get("/planificaciones-produccion")
                        .param("idReceta", "3").param("estado", "PENDIENTE")
                        .param("fechaInicio", "2027-07-01").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Pale Ale\"")))
                .andExpect(content().string(containsString("name=\"idReceta\" value=\"3\"")))
                .andExpect(content().string(containsString("value=\"2027-07-01\"")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"PENDIENTE\" selected=\"selected\">Pendiente</option>.*")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(planificacionServicio).filtrarPlanificacionesProduccion(
                eq(3L), eq(EstadoSolicitud.PENDIENTE), eq(LocalDate.of(2027, 7, 1)), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-05: los criterios vacíos equivalen a no filtrar y el filtro de estado ofrece los nombres legibles")
    void listado_loVacioNoFiltraYElEstadoSeMuestraLegible() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/planificaciones-produccion")
                        .param("idReceta", "").param("estado", "").param("fechaInicio", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<option value=\"\">Todos</option>")))
                .andExpect(content().string(containsString(">Pendiente</option>")))
                .andExpect(content().string(containsString(">Finalizada</option>")))
                .andExpect(content().string(containsString(">Anulada</option>")))
                .andExpect(content().string(not(containsString(">PENDIENTE</option>"))));

        verify(planificacionServicio).filtrarPlanificacionesProduccion(isNull(), isNull(), isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-06: el filtro de receta es un campo en blanco con ícono de lista que abre el buscador (no un combo) y todos los filtros tienen placeholder")
    void listado_elFiltroDeRecetaUsaElSelectorDeEntidad() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/planificaciones-produccion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-buscador-url=\"/planificaciones-produccion/buscador/recetas\"")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione receta\"")))
                .andExpect(content().string(containsString("bi bi-list-ul")))
                .andExpect(content().string(containsString("data-selector-limpiar")))
                .andExpect(content().string(not(containsString("data-selector-alta"))))
                .andExpect(content().string(not(containsString("data-selector-menu"))))
                .andExpect(content().string(containsString("Fecha de inicio estimada")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 01/07/2027\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-07: los enlaces de paginación conservan los filtros")
    void listado_debeConservarLosFiltrosEnLaPaginacion() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE)), PageRequest.of(1, 20), 45));

        mockMvc.perform(get("/planificaciones-produccion")
                        .param("idReceta", "3").param("estado", "PENDIENTE").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Página <span>2</span> de <span>3</span>")))
                .andExpect(content().string(containsString("pagina=0")))
                .andExpect(content().string(containsString("pagina=2")))
                .andExpect(content().string(containsString("idReceta=3")))
                .andExpect(content().string(containsString("estado=PENDIENTE")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-08: sin resultados muestra un mensaje claro en vez de una tabla vacía")
    void listado_debeMostrarMensajeCuandoNoHayResultados() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/planificaciones-produccion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron planificaciones de producción con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-09: el listado incluye los diálogos de motivo (finalizar y anular) con campo obligatorio, placeholder, CSRF y el campo motivo")
    void listado_debeIncluirLosDialogosDeMotivo() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/planificaciones-produccion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"dialogo-finalizar\"")))
                .andExpect(content().string(containsString("id=\"dialogo-anular\"")))
                .andExpect(content().string(containsString("Finalizar planificación")))
                .andExpect(content().string(containsString("Anular planificación")))
                .andExpect(content().string(containsString("name=\"motivo\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Se completó la producción prevista con menos lotes\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Se canceló el pedido del cliente\"")))
                .andExpect(content().string(containsString("<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>")))
                .andExpect(content().string(containsString("data-cerrar-dialogo")))
                .andExpect(content().string(matchesPattern("(?s).*<dialog id=\"dialogo-anular\".*name=\"_csrf\".*</dialog>.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PPC-10: el GERENTE_DE_COMPRAS ve el listado sin ningún botón de acción: ni Nueva planificación, ni Finalizar, ni Anular, ni diálogos")
    void listado_elGerenteDeComprasSoloConsulta() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/planificaciones-produccion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pale Ale")))
                .andExpect(content().string(containsString("href=\"/planificaciones-produccion/7\"")))
                .andExpect(content().string(not(containsString("Nueva planificación"))))
                .andExpect(content().string(not(containsString("/planificaciones-produccion/nueva"))))
                .andExpect(content().string(not(containsString("data-motivo-url"))))
                .andExpect(content().string(not(containsString("dialogo-finalizar"))))
                .andExpect(content().string(not(containsString("dialogo-anular"))));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-PPC-11: un rol que no es gerente (OPERARIO_DE_PRODUCCION) es rechazado en el listado y no llega al service")
    void listado_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-PPC-12: el ADMINISTRADOR también es rechazado en el listado y no llega al service")
    void listado_debeRechazarAlAdministrador() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_GERENTE_DE_PRODUCCION", "LOTE_CONSULTAR"})
    @DisplayName("CP-PPC-13: en la sidebar \"Planificación de Producción\" está en la sección Operación, junto a Gestión de Lotes")
    void sidebar_debeUbicarLaPlanificacionEnOperacion() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/planificaciones-produccion"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("bi-calendar-check")))
                .andExpect(content().string(matchesPattern("(?s).*Operación.*Gestión de Lotes.*Planificación de Producción.*Administración.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PPC-14: el GERENTE_DE_COMPRAS ve la opción en la sidebar, dentro de Operación y junto a Órdenes de Compra")
    void sidebar_elGerenteDeComprasVeLaOpcion() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/planificaciones-produccion"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*Operación.*href=\"/planificaciones-produccion\".*Órdenes de Compra.*")))
                .andExpect(content().string(not(containsString("Gestión de Lotes"))));
    }

    // ==================== GET /planificaciones-produccion/nueva ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-15: el formulario de alta es una página con la sección de datos, unidades en las etiquetas, placeholders, asterisco rojo, novalidate y token CSRF")
    void formularioAlta_debeMostrarLaSeccionYLosCamposVacios() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion/nueva"))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/planificacion-form"))
                .andExpect(content().string(containsString("Nueva planificación")))
                .andExpect(content().string(containsString("action=\"/planificaciones-produccion\"")))
                .andExpect(content().string(containsString("novalidate")))
                .andExpect(content().string(containsString("Datos de la planificación")))
                .andExpect(content().string(containsString("Cantidad a producir (L)")))
                .andExpect(content().string(containsString("Fecha de inicio estimada")))
                .andExpect(content().string(containsString("Fecha de finalización estimada")))
                .andExpect(content().string(containsString("placeholder=\"Seleccione receta\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 600\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 01/07/2027\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 25/07/2027\"")))
                .andExpect(content().string(containsString("type=\"date\"")))
                .andExpect(content().string(containsString("min=\"" + LocalDate.now() + "\"")))
                .andExpect(content().string(containsString("<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>")))
                .andExpect(content().string(containsString("Registrar planificación")))
                .andExpect(content().string(containsString("href=\"/planificaciones-produccion\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-16: la receta se elige con el selector (campo en blanco con ícono de lista que abre el buscador), sin menú de alta al vuelo")
    void formularioAlta_laRecetaSeEligeConElBuscador() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion/nueva"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-buscador-url=\"/planificaciones-produccion/buscador/recetas\"")))
                .andExpect(content().string(containsString("name=\"idReceta\"")))
                .andExpect(content().string(containsString("data-selector-buscar")))
                .andExpect(content().string(containsString("bi bi-list-ul")))
                .andExpect(content().string(not(containsString("data-selector-alta"))))
                .andExpect(content().string(not(containsString("data-selector-menu"))))
                .andExpect(content().string(not(containsString("data-selector-limpiar"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-17: junto a la fecha de finalización hay un botón \"Sugerir fecha\" y un área para la ayuda; el botón no corre solo (sin disparadores automáticos) y el script es un archivo propio")
    void formularioAlta_elBotonSugerirFechaNoCorreSolo() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion/nueva"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-sugerir-fecha")))
                .andExpect(content().string(containsString("Sugerir fecha")))
                .andExpect(content().string(containsString("bi-magic")))
                .andExpect(content().string(containsString("data-sugerencia-url=\"/planificaciones-produccion/sugerencia-fecha\"")))
                .andExpect(content().string(containsString("data-sugerencia-destino")))
                .andExpect(content().string(matchesPattern("(?s).*id=\"fechaFinalizacionEstimada\".*data-sugerir-fecha.*data-sugerencia-destino.*")))
                .andExpect(content().string(containsString("whitespace-nowrap")))
                .andExpect(content().string(containsString("<button type=\"button\" data-sugerir-fecha")))
                .andExpect(content().string(not(containsString("data-auto"))))
                .andExpect(content().string(not(containsString("onload"))))
                .andExpect(content().string(not(containsString("onchange"))))
                .andExpect(content().string(containsString("/js/planificacion-form.js")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PPC-18: el GERENTE_DE_COMPRAS no puede abrir el formulario de alta")
    void formularioAlta_debeRechazarAlGerenteDeCompras() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion/nueva"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /planificaciones-produccion ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-19: un alta válida enlaza los datos (fechas ISO), delega en el service y redirige al listado con alerta de éxito")
    void alta_debeRedirigirAlListadoConAlerta() throws Exception {
        mockMvc.perform(post("/planificaciones-produccion").with(csrf())
                        .param("idReceta", "3").param("cantidadAProducir", "600")
                        .param("fechaInicioEstimada", "2027-07-01").param("fechaFinalizacionEstimada", "2027-07-25"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion"))
                .andExpect(flash().attribute("mensaje", "Planificación de producción registrada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<PlanificacionProduccionFormDTO> captor = ArgumentCaptor.forClass(PlanificacionProduccionFormDTO.class);
        verify(planificacionServicio).registrarPlanificacionProduccion(captor.capture());
        assertThat(captor.getValue().getIdReceta()).isEqualTo(3L);
        assertThat(captor.getValue().getCantidadAProducir()).isEqualTo(600.0);
        assertThat(captor.getValue().getFechaInicioEstimada()).isEqualTo(LocalDate.of(2027, 7, 1));
        assertThat(captor.getValue().getFechaFinalizacionEstimada()).isEqualTo(LocalDate.of(2027, 7, 25));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-20: un formulario vacío vuelve a mostrarse (sin redirigir) con un error por campo obligatorio y sin llamar al service")
    void alta_conCamposVaciosMuestraLosErrores() throws Exception {
        mockMvc.perform(post("/planificaciones-produccion").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/planificacion-form"))
                .andExpect(content().string(containsString("No se pudo registrar la planificación")))
                .andExpect(content().string(containsString("La receta es obligatoria")))
                .andExpect(content().string(containsString("La cantidad a producir es obligatoria")))
                .andExpect(content().string(containsString("La fecha de inicio estimada es obligatoria")))
                .andExpect(content().string(containsString("La fecha de finalización estimada es obligatoria")));

        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-21: una cantidad cero o negativa se rechaza con Bean Validation, conserva lo cargado (incluida la receta elegida) y no llama al service")
    void alta_conCantidadInvalidaConservaLoCargado() throws Exception {
        when(recetaServicio.buscarPorId(3L)).thenReturn(receta(3L, "Pale Ale"));

        mockMvc.perform(post("/planificaciones-produccion").with(csrf())
                        .param("idReceta", "3").param("cantidadAProducir", "-5")
                        .param("fechaInicioEstimada", "2027-07-01").param("fechaFinalizacionEstimada", "2027-07-25"))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/planificacion-form"))
                .andExpect(content().string(containsString("La cantidad a producir debe ser mayor a 0")))
                .andExpect(content().string(containsString("value=\"Pale Ale\"")))
                .andExpect(content().string(containsString("name=\"idReceta\" value=\"3\"")))
                .andExpect(content().string(containsString("value=\"-5\"")))
                .andExpect(content().string(containsString("value=\"2027-07-01\"")))
                .andExpect(content().string(containsString("value=\"2027-07-25\"")));

        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-22: un valor que no es número ni fecha es un error del campo (mensaje claro) y no llama al service")
    void alta_conTiposInvalidosMuestraErroresDeCampo() throws Exception {
        mockMvc.perform(post("/planificaciones-produccion").with(csrf())
                        .param("idReceta", "3").param("cantidadAProducir", "abc")
                        .param("fechaInicioEstimada", "no-es-fecha").param("fechaFinalizacionEstimada", "2027-07-25"))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/planificacion-form"))
                .andExpect(content().string(containsString("Ingrese un número válido")))
                .andExpect(content().string(containsString("El valor ingresado no es una fecha válida")));

        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-23: si el service rechaza la regla (fin anterior al inicio) se muestra el mensaje como error global y se conserva todo lo cargado")
    void alta_conReglaDeNegocioRechazadaConservaLoCargado() throws Exception {
        when(recetaServicio.buscarPorId(3L)).thenReturn(receta(3L, "Pale Ale"));
        when(planificacionServicio.registrarPlanificacionProduccion(any()))
                .thenThrow(new ReglaNegocioException("La fecha de finalización estimada no puede ser anterior a la fecha de inicio estimada"));

        mockMvc.perform(post("/planificaciones-produccion").with(csrf())
                        .param("idReceta", "3").param("cantidadAProducir", "600")
                        .param("fechaInicioEstimada", "2027-07-25").param("fechaFinalizacionEstimada", "2027-07-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/planificacion-form"))
                .andExpect(content().string(containsString("No se pudo registrar la planificación")))
                .andExpect(content().string(containsString("La fecha de finalización estimada no puede ser anterior a la fecha de inicio estimada")))
                .andExpect(content().string(containsString("value=\"Pale Ale\"")))
                .andExpect(content().string(containsString("value=\"600\"")))
                .andExpect(content().string(containsString("value=\"2027-07-25\"")))
                .andExpect(content().string(containsString("value=\"2027-07-01\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-24: si la receta ya no existe o no tiene versión activa, el mensaje del service se muestra en el formulario sin perder lo cargado")
    void alta_conRecetaInexistenteMuestraElMensaje() throws Exception {
        when(recetaServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la receta con ID: 99"));
        when(planificacionServicio.registrarPlanificacionProduccion(any()))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la receta con ID: 99"));

        mockMvc.perform(post("/planificaciones-produccion").with(csrf())
                        .param("idReceta", "99").param("cantidadAProducir", "600")
                        .param("fechaInicioEstimada", "2027-07-01").param("fechaFinalizacionEstimada", "2027-07-25"))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/planificacion-form"))
                .andExpect(content().string(containsString("No se encontró la receta con ID: 99")))
                .andExpect(content().string(containsString("value=\"600\"")))
                .andExpect(content().string(containsString("value=\"2027-07-01\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PPC-25: el GERENTE_DE_COMPRAS no puede registrar planificaciones y el service no se invoca")
    void alta_debeRechazarAlGerenteDeCompras() throws Exception {
        mockMvc.perform(post("/planificaciones-produccion").with(csrf())
                        .param("idReceta", "3").param("cantidadAProducir", "600")
                        .param("fechaInicioEstimada", "2027-07-01").param("fechaFinalizacionEstimada", "2027-07-25"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-PPC-26: el OPERARIO_DE_PRODUCCION no puede registrar planificaciones")
    void alta_debeRechazarAlOperario() throws Exception {
        mockMvc.perform(post("/planificaciones-produccion").with(csrf())
                        .param("idReceta", "3").param("cantidadAProducir", "600")
                        .param("fechaInicioEstimada", "2027-07-01").param("fechaFinalizacionEstimada", "2027-07-25"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(planificacionServicio);
    }

    // ==================== GET /planificaciones-produccion/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-27: el estado actual muestra la cabecera, los contadores por estado, el cronograma (carriles y barras, sin el lote cancelado) y la tabla de lotes (con él)")
    void detalle_debeMostrarCabeceraContadoresGanttYLotes() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01"),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.EN_EJECUCION, "F-01"),
                lote(3L, "PALE_ALE-R7-3", EstadoLote.PENDIENTE, "F-02"),
                lote(4L, "PALE_ALE-R7-4", EstadoLote.CANCELADO, "F-02")));

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/planificacion-detalle"))
                .andExpect(content().string(containsString("Estado actual de la planificación")))
                .andExpect(content().string(containsString("Pale Ale")))
                .andExpect(content().string(containsString("Cantidad a producir (L)")))
                .andExpect(content().string(containsString("01/07/2027")))
                .andExpect(content().string(containsString("25/07/2027")))
                // Contadores
                .andExpect(content().string(containsString("Lotes totales")))
                .andExpect(content().string(containsString("Pendientes")))
                .andExpect(content().string(matchesPattern("(?s).*data-contador=\"totales\"[^>]*>4<.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-contador=\"pendientes\"[^>]*>1<.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-contador=\"en-ejecucion\"[^>]*>1<.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-contador=\"finalizados\"[^>]*>1<.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-contador=\"cancelados\"[^>]*>1<.*")))
                // Gantt: dos carriles (fermentadores) y una barra por lote con los colores del estado
                .andExpect(content().string(containsString("data-gantt")))
                .andExpect(content().string(containsString("Cronograma de lotes")))
                .andExpect(content().string(containsString("Fermentador")))
                .andExpect(content().string(containsString("title=\"F-02\"")))
                .andExpect(content().string(containsString("gantt-color--finalizado")))
                .andExpect(content().string(containsString("gantt-color--en-ejecucion")))
                .andExpect(content().string(containsString("gantt-color--pendiente")))
                .andExpect(content().string(not(containsString("gantt-color--cancelado"))))
                .andExpect(content().string(containsString("PALE_ALE-R7-3 - Pendiente")))
                .andExpect(content().string(not(containsString("gantt-progreso"))))
                .andExpect(content().string(containsString("overflow-x-auto")))
                // Tabla de lotes
                .andExpect(content().string(containsString("Inicio real")))
                .andExpect(content().string(containsString("Fin real")))
                .andExpect(content().string(containsString("PALE_ALE-R7-1")))
                .andExpect(content().string(containsString(">PALE_ALE-R7-4<")))
                .andExpect(content().string(not(containsString("No hay lotes registrados para esta planificación"))))
                .andExpect(content().string(not(containsString(">EN_EJECUCION<"))))
                .andExpect(content().string(containsString(">En ejecución<")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-28: sin lotes el cronograma y la tabla muestran el mensaje \"No hay lotes registrados para esta planificación\" y los contadores quedan en cero")
    void detalle_sinLotesMuestraElMensajeVacio() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(matchesMensajeVacioDosVeces())
                .andExpect(content().string(matchesPattern("(?s).*data-contador=\"totales\"[^>]*>0<.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-contador=\"cancelados\"[^>]*>0<.*")))
                .andExpect(content().string(not(containsString("data-gantt"))))
                .andExpect(content().string(not(containsString("gantt-barra--"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-29: una planificación Pendiente muestra Finalizar y Anular en la cabecera, con sus diálogos, y \"Volver a planificaciones\"")
    void detalle_pendienteMuestraLosBotonesDeCierre() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Volver a planificaciones")))
                .andExpect(content().string(containsString("href=\"/planificaciones-produccion\"")))
                .andExpect(content().string(containsString("data-motivo-url=\"/planificaciones-produccion/7/finalizar\"")))
                .andExpect(content().string(containsString("data-motivo-url=\"/planificaciones-produccion/7/anular\"")))
                .andExpect(content().string(containsString("id=\"dialogo-finalizar\"")))
                .andExpect(content().string(containsString("id=\"dialogo-anular\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-30: una planificación Finalizada muestra el motivo de la finalización con su fecha y no ofrece botones de cierre")
    void detalle_finalizadaMuestraElMotivoSinBotones() throws Exception {
        PlanificacionProduccionResponseDTO finalizada = planificacionBuilder(7L, "Pale Ale", EstadoSolicitud.FINALIZADA)
                .motivoFinalizacion("Se alcanzó el volumen necesario")
                .fechaFinalizacion(LocalDateTime.of(2027, 7, 20, 14, 30)).build();
        when(planificacionServicio.buscarPorId(7L)).thenReturn(finalizada);
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Finalizada<")))
                .andExpect(content().string(containsString("Motivo de la finalización (20/07/2027 14:30)")))
                .andExpect(content().string(containsString("Se alcanzó el volumen necesario")))
                .andExpect(content().string(not(containsString("data-motivo-url"))))
                .andExpect(content().string(not(containsString("dialogo-anular"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-31: una planificación Anulada muestra el motivo de la anulación con su fecha y no ofrece botones de cierre")
    void detalle_anuladaMuestraElMotivoSinBotones() throws Exception {
        PlanificacionProduccionResponseDTO anulada = planificacionBuilder(7L, "Pale Ale", EstadoSolicitud.ANULADA)
                .motivoAnulacion("Se canceló el pedido del cliente")
                .fechaAnulacion(LocalDateTime.of(2027, 6, 15, 9, 5)).build();
        when(planificacionServicio.buscarPorId(7L)).thenReturn(anulada);
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">Anulada<")))
                .andExpect(content().string(containsString("Motivo de la anulación (15/06/2027 09:05)")))
                .andExpect(content().string(containsString("Se canceló el pedido del cliente")))
                .andExpect(content().string(not(containsString("data-motivo-url"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PPC-32: el GERENTE_DE_COMPRAS ve el estado actual completo (cronograma y lotes) sin ningún botón de acción")
    void detalle_elGerenteDeComprasSoloConsulta() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01")));

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Estado actual de la planificación")))
                .andExpect(content().string(containsString("data-gantt")))
                .andExpect(content().string(containsString("PALE_ALE-R7-1")))
                .andExpect(content().string(containsString("Volver a planificaciones")))
                .andExpect(content().string(not(containsString("data-motivo-url"))))
                .andExpect(content().string(not(containsString("dialogo-finalizar"))))
                .andExpect(content().string(not(containsString("dialogo-anular"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-33: una planificación inexistente vuelve al origen con la alerta de error del service")
    void detalle_debeMostrarElErrorSiNoExiste() throws Exception {
        when(planificacionServicio.buscarPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: 99"));

        mockMvc.perform(get("/planificaciones-produccion/99").header("Referer", "/planificaciones-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion"))
                .andExpect(flash().attribute("mensaje", "No se encontró la planificación de producción con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));

        verify(loteServicio, never()).listarLotesDePlanificacion(any());
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-PPC-34: un rol que no es gerente no puede ver el estado actual y no llega al service")
    void detalle_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(planificacionServicio, loteServicio);
    }

    // ==================== POST /planificaciones-produccion/{id}/anular ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-35: anular correctamente delega en el service con el motivo y redirige al estado actual con alerta de éxito")
    void anular_debeRedirigirConAlerta() throws Exception {
        mockMvc.perform(post("/planificaciones-produccion/7/anular").with(csrf()).param("motivo", "Se canceló el pedido"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion/7"))
                .andExpect(flash().attribute("mensaje", "Planificación de producción anulada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<AnulacionPlanificacionProduccionFormDTO> captor = ArgumentCaptor.forClass(AnulacionPlanificacionProduccionFormDTO.class);
        verify(planificacionServicio).anularPlanificacionProduccion(eq(7L), captor.capture());
        assertThat(captor.getValue().getMotivoAnulacion()).isEqualTo("Se canceló el pedido");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-36: si el service rechaza la anulación (lotes asociados) vuelve al origen con la alerta de advertencia y el mensaje del service")
    void anular_debeMostrarElMotivoDelRechazo() throws Exception {
        doThrow(new ReglaNegocioException("No se puede anular la planificación de producción porque tiene al menos un lote en estado Pendiente o En ejecución asociado"))
                .when(planificacionServicio).anularPlanificacionProduccion(eq(7L), any());

        mockMvc.perform(post("/planificaciones-produccion/7/anular").with(csrf()).param("motivo", "x")
                        .header("Referer", "/planificaciones-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion"))
                .andExpect(flash().attribute("mensaje", "No se puede anular la planificación de producción porque tiene al menos un lote en estado Pendiente o En ejecución asociado"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-37: sin motivo la anulación llega al service con el motivo vacío y su rechazo (\"El motivo de anulación es obligatorio\") se muestra en la alerta")
    void anular_sinMotivoMuestraElMensajeDelService() throws Exception {
        doThrow(new ReglaNegocioException("El motivo de anulación es obligatorio"))
                .when(planificacionServicio).anularPlanificacionProduccion(eq(7L), any());

        mockMvc.perform(post("/planificaciones-produccion/7/anular").with(csrf()).param("motivo", "")
                        .header("Referer", "/planificaciones-produccion/7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion/7"))
                .andExpect(flash().attribute("mensaje", "El motivo de anulación es obligatorio"))
                .andExpect(flash().attribute("tipo", "warning"));

        ArgumentCaptor<AnulacionPlanificacionProduccionFormDTO> captor = ArgumentCaptor.forClass(AnulacionPlanificacionProduccionFormDTO.class);
        verify(planificacionServicio).anularPlanificacionProduccion(eq(7L), captor.capture());
        assertThat(captor.getValue().getMotivoAnulacion()).isEmpty();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-38: anular una planificación inexistente vuelve al origen con la alerta de error del service")
    void anular_debeMostrarElErrorSiNoExiste() throws Exception {
        doThrow(new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: 99"))
                .when(planificacionServicio).anularPlanificacionProduccion(eq(99L), any());

        mockMvc.perform(post("/planificaciones-produccion/99/anular").with(csrf()).param("motivo", "x")
                        .header("Referer", "/planificaciones-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se encontró la planificación de producción con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PPC-39: el GERENTE_DE_COMPRAS no puede anular y el service no se invoca")
    void anular_debeRechazarAlGerenteDeCompras() throws Exception {
        mockMvc.perform(post("/planificaciones-produccion/7/anular").with(csrf()).param("motivo", "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(planificacionServicio);
    }

    // ==================== POST /planificaciones-produccion/{id}/finalizar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-40: finalizar correctamente delega en el service con el motivo y redirige al estado actual con alerta de éxito")
    void finalizar_debeRedirigirConAlerta() throws Exception {
        mockMvc.perform(post("/planificaciones-produccion/7/finalizar").with(csrf()).param("motivo", "Se alcanzó el volumen"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion/7"))
                .andExpect(flash().attribute("mensaje", "Planificación de producción finalizada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<FinalizacionForzadaPlanificacionProduccionFormDTO> captor =
                ArgumentCaptor.forClass(FinalizacionForzadaPlanificacionProduccionFormDTO.class);
        verify(planificacionServicio).finalizarPlanificacionProduccion(eq(7L), captor.capture());
        assertThat(captor.getValue().getMotivoFinalizacion()).isEqualTo("Se alcanzó el volumen");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-41: si el service rechaza la finalización (sin lotes finalizados) vuelve al origen con la alerta de advertencia y el mensaje del service")
    void finalizar_debeMostrarElMotivoDelRechazo() throws Exception {
        doThrow(new ReglaNegocioException("No se puede finalizar la planificación de producción porque no tiene ningún lote en estado Finalizado asociado; corresponde anularla en su lugar"))
                .when(planificacionServicio).finalizarPlanificacionProduccion(eq(7L), any());

        mockMvc.perform(post("/planificaciones-produccion/7/finalizar").with(csrf()).param("motivo", "x")
                        .header("Referer", "/planificaciones-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion"))
                .andExpect(flash().attribute("mensaje", "No se puede finalizar la planificación de producción porque no tiene ningún lote en estado Finalizado asociado; corresponde anularla en su lugar"))
                .andExpect(flash().attribute("tipo", "warning"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-42: sin el parámetro motivo la finalización llega al service con el motivo nulo y su rechazo se muestra en la alerta")
    void finalizar_sinMotivoMuestraElMensajeDelService() throws Exception {
        doThrow(new ReglaNegocioException("El motivo de finalización es obligatorio"))
                .when(planificacionServicio).finalizarPlanificacionProduccion(eq(7L), any());

        mockMvc.perform(post("/planificaciones-produccion/7/finalizar").with(csrf())
                        .header("Referer", "/planificaciones-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "El motivo de finalización es obligatorio"))
                .andExpect(flash().attribute("tipo", "warning"));

        ArgumentCaptor<FinalizacionForzadaPlanificacionProduccionFormDTO> captor =
                ArgumentCaptor.forClass(FinalizacionForzadaPlanificacionProduccionFormDTO.class);
        verify(planificacionServicio).finalizarPlanificacionProduccion(eq(7L), captor.capture());
        assertThat(captor.getValue().getMotivoFinalizacion()).isNull();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-43: finalizar una planificación inexistente vuelve al origen con la alerta de error del service")
    void finalizar_debeMostrarElErrorSiNoExiste() throws Exception {
        doThrow(new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: 99"))
                .when(planificacionServicio).finalizarPlanificacionProduccion(eq(99L), any());

        mockMvc.perform(post("/planificaciones-produccion/99/finalizar").with(csrf()).param("motivo", "x")
                        .header("Referer", "/planificaciones-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se encontró la planificación de producción con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PPC-44: el GERENTE_DE_COMPRAS no puede finalizar y el service no se invoca")
    void finalizar_debeRechazarAlGerenteDeCompras() throws Exception {
        mockMvc.perform(post("/planificaciones-produccion/7/finalizar").with(csrf()).param("motivo", "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-45: un POST sin token CSRF es rechazado (redirige) y no llega al service")
    void cierres_sinCsrfSonRechazados() throws Exception {
        mockMvc.perform(post("/planificaciones-produccion/7/anular").param("motivo", "x"))
                .andExpect(status().is3xxRedirection());

        verifyNoInteractions(planificacionServicio);
    }

    // ==================== GET /planificaciones-produccion/sugerencia-fecha ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-46: la sugerencia es un fragmento HTML (sin layout) con el texto de la simulación, el fin sugerido formateado y la fecha ISO en un atributo data")
    void sugerencia_devuelveElFragmentoConLaFecha() throws Exception {
        when(planificacionServicio.sugerirFechaFinalizacion(eq(3L), eq(4200.0), eq(LocalDate.of(2027, 7, 1))))
                .thenReturn(sugerencia(14, 300.0, 18, LocalDate.of(2027, 7, 25)));

        mockMvc.perform(get("/planificaciones-produccion/sugerencia-fecha")
                        .param("idReceta", "3").param("cantidadAProducir", "4200").param("fechaInicioEstimada", "2027-07-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/sugerencia-fecha :: sugerencia"))
                .andExpect(content().string(containsString("data-fecha-sugerida=\"2027-07-25\"")))
                .andExpect(content().string(containsString("data-sugerencia")))
                .andExpect(content().string(matchesPattern("(?s).*14 lotes de 300 L, de 18 días cada uno, con 1 día de limpieza entre lotes\\. Fin sugerido: <strong[^>]*>25/07/2027</strong>\\..*")))
                .andExpect(content().string(not(containsString("data-sugerencia-error"))))
                .andExpect(content().string(not(containsString("<html"))))
                .andExpect(content().string(not(containsString("Maestro Cervecero"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-47: con un solo lote y un solo día el texto va en singular")
    void sugerencia_usaElSingularCuandoCorresponde() throws Exception {
        when(planificacionServicio.sugerirFechaFinalizacion(any(), any(), any()))
                .thenReturn(sugerencia(1, 300.5, 1, LocalDate.of(2027, 7, 2)));

        mockMvc.perform(get("/planificaciones-produccion/sugerencia-fecha")
                        .param("idReceta", "3").param("cantidadAProducir", "300,5").param("fechaInicioEstimada", "2027-07-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1 lote de 300,5 L, de 1 día cada uno")));

        verify(planificacionServicio).sugerirFechaFinalizacion(3L, 300.5, LocalDate.of(2027, 7, 1));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-48: si el service rechaza la regla (ReglaNegocioException) devuelve el mismo fragmento en estilo de error con el mensaje y sin fecha")
    void sugerencia_conReglaRechazadaMuestraElError() throws Exception {
        when(planificacionServicio.sugerirFechaFinalizacion(any(), any(), any()))
                .thenThrow(new ReglaNegocioException("La cantidad a producir debe ser mayor a 0"));

        mockMvc.perform(get("/planificaciones-produccion/sugerencia-fecha")
                        .param("idReceta", "3").param("cantidadAProducir", "0").param("fechaInicioEstimada", "2027-07-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-sugerencia-error")))
                .andExpect(content().string(containsString("text-rose-600")))
                .andExpect(content().string(containsString("La cantidad a producir debe ser mayor a 0")))
                .andExpect(content().string(not(containsString("data-fecha-sugerida"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-49: si la receta no existe o no tiene versión activa (RecursoNoEncontradoException) devuelve el fragmento de error con el mensaje, sin fecha y sin error del servidor")
    void sugerencia_conRecursoNoEncontradoMuestraElError() throws Exception {
        when(planificacionServicio.sugerirFechaFinalizacion(any(), any(), any()))
                .thenThrow(new RecursoNoEncontradoException("No se encontró una versión activa para la receta con ID: 3"));

        mockMvc.perform(get("/planificaciones-produccion/sugerencia-fecha")
                        .param("idReceta", "3").param("cantidadAProducir", "600").param("fechaInicioEstimada", "2027-07-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-sugerencia-error")))
                .andExpect(content().string(containsString("No se encontró una versión activa para la receta con ID: 3")))
                .andExpect(content().string(not(containsString("data-fecha-sugerida"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-50: sin receta elegida avisa que hay que elegirla, sin llamar al service")
    void sugerencia_sinRecetaPideElegirla() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion/sugerencia-fecha")
                        .param("idReceta", "").param("cantidadAProducir", "600").param("fechaInicioEstimada", "2027-07-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-sugerencia-error")))
                .andExpect(content().string(containsString("Seleccione una receta para sugerir la fecha de finalización")))
                .andExpect(content().string(not(containsString("data-fecha-sugerida"))));

        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-51: los datos vacíos llegan como null al service, que los rechaza con su mensaje (nunca un error 500)")
    void sugerencia_conDatosVaciosDejaQueElServiceLosRechace() throws Exception {
        when(planificacionServicio.sugerirFechaFinalizacion(eq(3L), isNull(), isNull()))
                .thenThrow(new ReglaNegocioException("La cantidad a producir debe ser mayor a 0"));

        mockMvc.perform(get("/planificaciones-produccion/sugerencia-fecha").param("idReceta", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La cantidad a producir debe ser mayor a 0")));

        verify(planificacionServicio).sugerirFechaFinalizacion(3L, null, null);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-52: una cantidad o una fecha mal escrita se informa en el fragmento de error y no llama al service")
    void sugerencia_conDatosInvalidosMuestraElError() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion/sugerencia-fecha")
                        .param("idReceta", "3").param("cantidadAProducir", "abc").param("fechaInicioEstimada", "2027-07-01"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingrese una cantidad a producir válida")));
        mockMvc.perform(get("/planificaciones-produccion/sugerencia-fecha")
                        .param("idReceta", "3").param("cantidadAProducir", "600").param("fechaInicioEstimada", "31/12/2027"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingrese una fecha de inicio estimada válida")));

        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PPC-53: el GERENTE_DE_COMPRAS no puede pedir sugerencias y el service no se invoca")
    void sugerencia_debeRechazarAlGerenteDeCompras() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion/sugerencia-fecha").param("idReceta", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(planificacionServicio);
    }

    // ==================== GET /planificaciones-produccion/buscador/recetas ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-54: el buscador devuelve un fragmento (sin layout) con el criterio de nombre, el placeholder, las recetas y el botón Elegir con su id y nombre")
    void buscador_debeDevolverElFragmentoConLasRecetas() throws Exception {
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(receta(3L, "Pale Ale"), receta(4L, "Stout")), PageRequest.of(0, 8), 2));

        mockMvc.perform(get("/planificaciones-produccion/buscador/recetas"))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/buscador-receta :: buscador"))
                .andExpect(content().string(containsString("Buscar receta")))
                .andExpect(content().string(containsString("placeholder=\"Ej: Pale Ale\"")))
                .andExpect(content().string(containsString("action=\"/planificaciones-produccion/buscador/recetas\"")))
                .andExpect(content().string(containsString("data-modal-buscar")))
                .andExpect(content().string(containsString("data-id=\"3\"")))
                .andExpect(content().string(containsString("data-texto=\"Pale Ale\"")))
                .andExpect(content().string(containsString("data-id=\"4\"")))
                .andExpect(content().string(containsString("Elegir")))
                .andExpect(content().string(not(containsString("<html"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-55: el buscador pide páginas de 8 sin ordenamiento (el alfabético lo define la consulta del repositorio) y el nombre llega sin espacios; en blanco equivale a null")
    void buscador_debePedirPaginasDe8() throws Exception {
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 8), 0));

        mockMvc.perform(get("/planificaciones-produccion/buscador/recetas").param("nombre", " pale ").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/planificaciones-produccion/buscador/recetas").param("nombre", "  "))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(recetaServicio).filtrarRecetas(eq("pale"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(8);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
        verify(recetaServicio).filtrarRecetas(isNull(), any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-56: la paginación del buscador conserva el nombre, se pide dentro del modal y sin resultados muestra un mensaje claro")
    void buscador_debeConservarElCriterioYMostrarElVacio() throws Exception {
        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(receta(3L, "Pale Ale")), PageRequest.of(1, 8), 20));

        mockMvc.perform(get("/planificaciones-produccion/buscador/recetas").param("nombre", "pale").param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Página <span>2</span> de <span>3</span>")))
                .andExpect(content().string(containsString("data-modal-get=\"true\"")))
                .andExpect(content().string(containsString("pagina=0")))
                .andExpect(content().string(containsString("pagina=2")))
                .andExpect(content().string(containsString("nombre=pale")));

        when(recetaServicio.filtrarRecetas(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 8), 0));

        mockMvc.perform(get("/planificaciones-produccion/buscador/recetas").param("nombre", "zzz"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron recetas con los criterios indicados")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PPC-57: el GERENTE_DE_COMPRAS no puede usar el buscador de recetas y no llega al service")
    void buscador_debeRechazarAlGerenteDeCompras() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion/buscador/recetas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(recetaServicio);
    }

    // ==================== Litros producidos ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-58: el listado muestra la columna \"Producido (L)\" con los litros producidos formateados (2.350) y 0 si no hay envasados")
    void listado_debeMostrarLaColumnaProducidoConElFormatoDeNumeros() throws Exception {
        when(planificacionServicio.filtrarPlanificacionesProduccion(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(
                        planificacionBuilder(7L, "Pale Ale", EstadoSolicitud.PENDIENTE).litrosProducidos(2350.0).build(),
                        planificacionBuilder(8L, "Stout", EstadoSolicitud.PENDIENTE).litrosProducidos(250.5).build(),
                        planificacion(9L, "Lager", EstadoSolicitud.PENDIENTE)), PageRequest.of(0, 20), 3));

        mockMvc.perform(get("/planificaciones-produccion"))
                .andExpect(status().isOk())
                // La columna va justo después de "Cantidad (L)" (título completo: "Cantidad a producir (L)")
                .andExpect(content().string(matchesPattern("(?s).*>Cantidad \\(L\\)</th>\\s*<th[^>]*>Producido \\(L\\)</th>.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-litros-producidos[^>]*>2\\.350<.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-litros-producidos[^>]*>250,5<.*")))
                .andExpect(content().string(matchesPattern("(?s).*data-litros-producidos[^>]*>0<.*")))
                .andExpect(content().string(not(containsString("2350.0"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-59: el estado actual muestra el avance parcial: \"Producido: 250 L de 600 L (42%) · Faltan 350 L\" y la barra al 42%")
    void detalle_debeMostrarElAvanceParcial() throws Exception {
        when(planificacionServicio.buscarPorId(7L))
                .thenReturn(planificacionBuilder(7L, "Pale Ale", EstadoSolicitud.PENDIENTE).litrosProducidos(250.0).build());
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*Producido: <strong[^>]*>250 L</strong>\\s+de 600 L\\s+\\(42%\\).*")))
                .andExpect(content().string(matchesPattern("(?s).*data-faltan[^>]*>&middot; Faltan 350 L<.*")))
                .andExpect(content().string(not(containsString("Superó lo solicitado"))))
                .andExpect(content().string(containsString("role=\"progressbar\"")))
                .andExpect(content().string(containsString("aria-valuenow=\"42\"")))
                .andExpect(content().string(matchesPattern("(?s).*data-barra-progreso[^>]*style=\"width: 42%\".*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-60: el estado actual sin envasados muestra \"Producido: 0 L de 600 L (0%) · Faltan 600 L\" y la barra vacía")
    void detalle_debeMostrarElAvanceEnCeroSinEnvasados() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*Producido: <strong[^>]*>0 L</strong>\\s+de 600 L\\s+\\(0%\\).*")))
                .andExpect(content().string(matchesPattern("(?s).*data-faltan[^>]*>&middot; Faltan 600 L<.*")))
                .andExpect(content().string(containsString("aria-valuenow=\"0\"")))
                .andExpect(content().string(matchesPattern("(?s).*data-barra-progreso[^>]*style=\"width: 0%\".*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-61: el estado actual que superó lo solicitado muestra \"Superó lo solicitado por 1.750 L\" en lugar de \"Faltan\" y la barra tope al 100%")
    void detalle_debeMostrarLoQueSuperoLoSolicitado() throws Exception {
        when(planificacionServicio.buscarPorId(7L))
                .thenReturn(planificacionBuilder(7L, "Pale Ale", EstadoSolicitud.PENDIENTE).litrosProducidos(2350.0).build());
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*Producido: <strong[^>]*>2\\.350 L</strong>\\s+de 600 L\\s+\\(392%\\).*")))
                .andExpect(content().string(matchesPattern("(?s).*data-superado[^>]*>&middot; Superó lo solicitado por 1\\.750 L<.*")))
                .andExpect(content().string(not(containsString("Faltan"))))
                .andExpect(content().string(containsString("aria-valuenow=\"100\"")))
                .andExpect(content().string(matchesPattern("(?s).*data-barra-progreso[^>]*style=\"width: 100%\".*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-62: la leyenda explica los tres colores y las marcas (Pendiente, En ejecución, Finalizado, Limpieza, Hoy e Inicio y fin estimados de la planificación)")
    void detalle_laLeyendaExplicaLosTresColoresYLasMarcas() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01")));

        String html = mockMvc.perform(get("/planificaciones-produccion/7")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Matcher m = Pattern.compile("(?s)aria-label=\"Leyenda del diagrama\">(.*?)</ul>").matcher(html);
        assertThat(m.find()).isTrue();
        String leyenda = m.group(1);
        List<String> ordenadas = List.of("Pendiente", "En ejecuci", "Finalizado", "Limpieza", "Hoy", "Inicio y fin estimados de la planificaci");
        for (int i = 1; i < ordenadas.size(); i++) {
            assertThat(leyenda.indexOf(ordenadas.get(i - 1))).isGreaterThanOrEqualTo(0).isLessThan(leyenda.indexOf(ordenadas.get(i)));
        }
        assertThat(leyenda).doesNotContain("Cancelado").doesNotContain("Atraso").doesNotContain("Desplazado").doesNotContain("transcurrido");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-63: los lotes cancelados figuran en la tabla pero no en el diagrama; si todos son cancelados el diagrama lo dice")
    void detalle_losCanceladosFiguranEnLaTablaPeroNoEnElDiagrama() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));

        // (a) un pendiente y un cancelado
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01"),
                lote(2L, "PALE_ALE-R7-2", EstadoLote.CANCELADO, "F-02")));
        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("title=\"F-01\"")))
                .andExpect(content().string(not(containsString("title=\"F-02\""))))
                .andExpect(content().string(not(containsString("gantt-color--cancelado"))))
                .andExpect(content().string(containsString(">PALE_ALE-R7-1<")))
                .andExpect(content().string(containsString(">PALE_ALE-R7-2<")))
                .andExpect(content().string(containsString(">Cancelado<")))
                .andExpect(content().string(matchesPattern("(?s).*data-contador=\"cancelados\"[^>]*>1<.*")));

        // (b) un único lote cancelado
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(lote(2L, "PALE_ALE-R7-2", EstadoLote.CANCELADO, "F-02")));
        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data-gantt"))))
                .andExpect(content().string(not(containsString("gantt-barra--"))))
                .andExpect(content().string(containsString("No hay lotes para mostrar en el diagrama (los lotes cancelados no se dibujan)")))
                .andExpect(content().string(not(containsString("Todavía no hay lotes registrados"))))
                .andExpect(content().string(containsString(">PALE_ALE-R7-2<")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-64: el código completo del lote se muestra en la barra, su tooltip y la tabla de lotes")
    void detalle_muestraElCodigoCompletoDelLote() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(lote(1L, "PILSEN_CLASICA-R4-3", EstadoLote.PENDIENTE, "F-01")));

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(">PILSEN_CLASICA-R4-3 - Pendiente<")))
                .andExpect(content().string(containsString("title=\"PILSEN_CLASICA-R4-3 - Pendiente")))
                .andExpect(content().string(containsString(">PILSEN_CLASICA-R4-3<")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-65: la limpieza se dibuja en el diagrama solo donde queda un día libre futuro entre lotes vivos")
    void detalle_dibujaLaLimpiezaSoloDondeQuedaUnDiaLibre() throws Exception {
        LocalDate hoy = LocalDate.now();
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));

        // (a) con un día libre entre los lotes
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(
                loteEn(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", hoy.plusDays(3), hoy.plusDays(7)),
                loteEn(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", hoy.plusDays(9), hoy.plusDays(12))));
        String conLimpieza = mockMvc.perform(get("/planificaciones-produccion/7")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // (b) con lotes consecutivos
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(
                loteEn(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", hoy.plusDays(3), hoy.plusDays(7)),
                loteEn(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", hoy.plusDays(8), hoy.plusDays(12))));
        String sinLimpieza = mockMvc.perform(get("/planificaciones-produccion/7")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(conLimpieza.split("data-gantt-limpieza", -1).length - conLimpieza.split("data-gantt-limpieza-icono", -1).length).isEqualTo(1);
        assertThat(conLimpieza).contains("Limpieza de equipamientos");
        assertThat(sinLimpieza).doesNotContain("data-gantt-limpieza");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-66: el atraso del lote en ejecución se informa en su barra con la etiqueta \"Atrasado 3 días\", sin color ni tramo aparte")
    void detalle_elAtrasoSeInformaEnLaBarra() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(
                lote(1L, "PALE_ALE-R7-1", EstadoLote.EN_EJECUCION, "F-01").toBuilder().diasDeAtraso(3).build()));

        String html = mockMvc.perform(get("/planificaciones-produccion/7")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("gantt-color--en-ejecucion").contains("data-gantt-atraso-etiqueta").contains("Atrasado 3 d");
        assertThat(Pattern.compile("data-gantt-atraso(?!-etiqueta)").matcher(html).find()).isFalse();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-67: no se dibuja la limpieza anterior a hoy ni la que sigue a un lote finalizado; las barras se dibujan igual")
    void detalle_noDibujaLimpiezasEnElPasadoNiTrasFinalizados() throws Exception {
        LocalDate hoy = LocalDate.now();
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));

        // (a) un día libre anterior a hoy entre dos pendientes
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(
                loteEn(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", hoy.minusDays(10), hoy.minusDays(6)),
                loteEn(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", hoy.minusDays(4), hoy.minusDays(1))));
        String pasado = mockMvc.perform(get("/planificaciones-produccion/7")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // (b) un finalizado seguido de un pendiente
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(
                loteEn(3L, "PALE_ALE-R7-3", EstadoLote.FINALIZADO, "F-02", hoy.minusDays(10), hoy.minusDays(6)).toBuilder()
                        .fechaInicio(hoy.minusDays(10).atTime(8, 0)).fechaFinalizacion(hoy.minusDays(6).atTime(18, 0)).build(),
                loteEn(4L, "PALE_ALE-R7-4", EstadoLote.PENDIENTE, "F-02", hoy.plusDays(3), hoy.plusDays(6))));
        String trasFinalizado = mockMvc.perform(get("/planificaciones-produccion/7")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(pasado).doesNotContain("data-gantt-limpieza");
        assertThat(pasado.split("role=\"img\"", -1).length - 1).isEqualTo(2);
        assertThat(trasFinalizado).doesNotContain("data-gantt-limpieza");
        assertThat(trasFinalizado.split("role=\"img\"", -1).length - 1).isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-68: el bloque de limpieza es celeste (gantt-color--limpieza), distinto del azul de lo planificado, y se reduce a solo color si es muy angosto")
    void detalle_elBloqueDeLimpiezaEsCelesteYSeReduceASoloColor() throws Exception {
        String html = detalleConUnaLimpieza();

        String bloque = bloqueDeLimpieza(html);

        assertThat(bloque).contains("gantt-color--limpieza").doesNotContain("gantt-color--pendiente");
        assertThat(html).contains(".gantt-color--limpieza {").contains("container-type: inline-size")
                .contains("@container (max-width: 15px)");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-69: el bloque de limpieza lleva un icono de destellos decorativo (data-gantt-limpieza-icono, bi-stars, aria-hidden)")
    void detalle_elBloqueDeLimpiezaLlevaUnIconoDeDestellos() throws Exception {
        String html = detalleConUnaLimpieza();

        String bloque = bloqueDeLimpieza(html);

        assertThat(bloque.split("data-gantt-limpieza-icono", -1).length - 1).isEqualTo(1);
        assertThat(bloque).containsPattern("<i[^>]*class=\"[^\"]*bi bi-stars[^\"]*\"[^>]*data-gantt-limpieza-icono[^>]*aria-hidden=\"true\"")
                .contains("role=\"img\"").contains("Limpieza de equipamientos");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-70: la leyenda muestra la limpieza con la misma muestra celeste y el icono de destellos, aunque no haya limpiezas dibujadas")
    void detalle_laLeyendaMuestraLaLimpiezaConElIconoDeDestellos() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01")));

        String html = mockMvc.perform(get("/planificaciones-produccion/7")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Matcher m = Pattern.compile("(?s)aria-label=\"Leyenda del diagrama\">(.*?)</ul>").matcher(html);
        assertThat(m.find()).isTrue();
        assertThat(m.group(1)).containsPattern("gantt-color--limpieza\"><i[^>]*bi bi-stars[^>]*data-gantt-leyenda-limpieza-icono[^>]*aria-hidden=\"true\"")
                .contains("Limpieza de equipamientos");
    }

    private String detalleConUnaLimpieza() throws Exception {
        LocalDate hoy = LocalDate.now();
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(
                loteEn(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01", hoy.plusDays(3), hoy.plusDays(7)),
                loteEn(2L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01", hoy.plusDays(9), hoy.plusDays(12))));
        return mockMvc.perform(get("/planificaciones-produccion/7")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String bloqueDeLimpieza(String html) {
        Matcher m = Pattern.compile("(?s)<div[^>]*data-gantt-limpieza(?![-\\w]).*?</div>").matcher(html);
        assertThat(m.find()).isTrue();
        return m.group();
    }

    // ==================== Botón "Ver receta" en el estado actual ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-71: el estado actual de una planificación pendiente incluye el link \"Ver receta\" junto a \"Volver a planificaciones\"")
    void detalle_debeMostrarElBotonVerReceta() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/planificaciones-produccion/7/receta\"")))
                .andExpect(content().string(containsString("Ver receta")))
                .andExpect(content().string(matchesPattern("(?s).*Volver a planificaciones.*?<a [^>]*href=\"/planificaciones-produccion/7/receta\"[^>]*whitespace-nowrap[^>]*>\\s*<i class=\"bi bi-journal-text\"></i> Ver receta.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-72: el link \"Ver receta\" aparece también en una planificación finalizada y en una anulada, aunque no tengan botones de cierre")
    void detalle_elBotonVerRecetaAparecaEnCualquierEstado() throws Exception {
        for (EstadoSolicitud estado : List.of(EstadoSolicitud.FINALIZADA, EstadoSolicitud.ANULADA)) {
            when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", estado));
            when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

            mockMvc.perform(get("/planificaciones-produccion/7"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("href=\"/planificaciones-produccion/7/receta\"")))
                    .andExpect(content().string(containsString("Ver receta")))
                    .andExpect(content().string(not(containsString("/planificaciones-produccion/7/finalizar"))))
                    .andExpect(content().string(not(containsString("/planificaciones-produccion/7/anular"))));
        }
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PPC-73: el GERENTE_DE_COMPRAS no ve el link \"Ver receta\" en el estado actual")
    void detalle_elGerenteDeComprasNoVeElBotonVerReceta() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        mockMvc.perform(get("/planificaciones-produccion/7"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Ver receta"))))
                .andExpect(content().string(not(containsString("/planificaciones-produccion/7/receta"))));
    }

    // ==================== GET /planificaciones-produccion/{id}/receta ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-74: la receta de la planificación muestra el nombre, \"Versión 3 de 3\" y la etiqueta \"Versión vigente\", sin el aviso de versión histórica")
    void recetaDePlanificacion_debeMostrarLaVersionVigente() throws Exception {
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacionBase());

        mockMvc.perform(get("/planificaciones-produccion/7/receta"))
                .andExpect(status().isOk())
                .andExpect(view().name("planificacion_produccion/planificacion-receta"))
                .andExpect(content().string(containsString("Pale Ale")))
                .andExpect(content().string(containsString("Versión 3 de 3")))
                .andExpect(content().string(containsString("Versión vigente")))
                .andExpect(content().string(not(containsString("Versión histórica"))))
                .andExpect(content().string(not(containsString("Esta planificación usa una versión anterior"))));

        verify(planificacionServicio).buscarVersionRecetaDePlanificacion(7L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-75: la receta de la planificación muestra los datos generales y las duraciones con números sin ceros sobrantes")
    void recetaDePlanificacion_debeMostrarLosDatosGeneralesYLasDuraciones() throws Exception {
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacionBase());

        mockMvc.perform(get("/planificaciones-produccion/7/receta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Datos generales")))
                .andExpect(content().string(containsString(">R3<")))
                .andExpect(content().string(containsString("Volumen base (L)")))
                .andExpect(content().string(containsString("Relación de empaste (L/kg)")))
                .andExpect(content().string(containsString("OG objetivo")))
                .andExpect(content().string(containsString("FG objetivo")))
                .andExpect(content().string(containsString("IBU objetivo")))
                .andExpect(content().string(containsString(">20<")))
                .andExpect(content().string(containsString(">3<")))
                .andExpect(content().string(containsString(">1,050<")))
                .andExpect(content().string(containsString(">1,010<")))
                .andExpect(content().string(containsString(">35<")))
                .andExpect(content().string(not(containsString("20.0"))))
                .andExpect(content().string(containsString("Duraciones estimadas")))
                .andExpect(content().string(matchesPattern("(?s).*Maceración \\(minutos\\).*?>\\s*60\\s*<.*Hervido \\(minutos\\).*?>\\s*90\\s*<.*"
                        + "Fermentación \\(días\\).*?>\\s*10\\s*<.*Maduración \\(días\\).*?>\\s*4\\s*<.*")))
                .andExpect(content().string(not(containsString("Duración total"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-76: una versión histórica muestra \"Versión 1 de 3\", la etiqueta \"Versión histórica\" y el aviso, con los datos de esa versión")
    void recetaDePlanificacion_debeAvisarQueLaVersionEsHistorica() throws Exception {
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(
                recetaDePlanificacion(versionDeRecetaBuilder("Pale Ale Clásica", false).build(), 1, 3, Estado.ACTIVO));

        mockMvc.perform(get("/planificaciones-produccion/7/receta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pale Ale Clásica")))
                .andExpect(content().string(containsString("Versión 1 de 3")))
                .andExpect(content().string(containsString("Versión histórica")))
                .andExpect(content().string(containsString("Esta planificación usa una versión anterior de la receta; los datos mostrados son los de esa versión y no los de la versión vigente")))
                .andExpect(content().string(not(containsString("Versión vigente"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-77: una receta dada de baja se sigue viendo, con la etiqueta \"Receta dada de baja\" y su aviso")
    void recetaDePlanificacion_debeMostrarUnaRecetaDadaDeBaja() throws Exception {
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(
                recetaDePlanificacion(versionDeRecetaBuilder("Pale Ale", true).build(), 3, 3, Estado.BAJA));

        mockMvc.perform(get("/planificaciones-produccion/7/receta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Receta dada de baja")))
                .andExpect(content().string(containsString("La receta fue dada de baja; se muestra la versión que usa la planificación")))
                .andExpect(content().string(containsString("Datos generales")))
                .andExpect(content().string(containsString("Duraciones estimadas")))
                .andExpect(content().string(containsString("Maltas (cantidad en kg)")))
                .andExpect(content().string(containsString("Lúpulos (cantidad en g, tiempo de hervor en minutos)")))
                .andExpect(content().string(containsString("Levaduras (cantidad en g)")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-78: la receta de la planificación lista todas las maltas con nombre, tipo legible y cantidad, en el orden recibido")
    void recetaDePlanificacion_debeListarTodasLasMaltas() throws Exception {
        VersionRecetaResponseDTO version = versionDeRecetaBuilder("Pale Ale", true)
                .detallesMalta(List.of(
                        DetalleMaltaResponseDTO.builder().id(1L).cantidad(4.5)
                                .malta(MaltaResponseDTO.builder().id(1L).nombre("Pilsen").tipo(TipoMalta.BASE).build()).build(),
                        DetalleMaltaResponseDTO.builder().id(2L).cantidad(0.5)
                                .malta(MaltaResponseDTO.builder().id(2L).nombre("Caramelo 60").tipo(TipoMalta.CARAMELO).build()).build()))
                .build();
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacion(version, 3, 3, Estado.ACTIVO));

        mockMvc.perform(get("/planificaciones-produccion/7/receta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Maltas (cantidad en kg)")))
                .andExpect(content().string(matchesPattern("(?s).*Pilsen.*?>\\s*Base\\s*<.*?>\\s*4,5\\s*<.*Caramelo 60.*?>\\s*Caramelo\\s*<.*?>\\s*0,5\\s*<.*")))
                .andExpect(content().string(not(containsString("La receta no tiene maltas"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-79: los lúpulos muestran formato, alfa, uso y etapa legibles; el tiempo de hervor solo aparece en el uso Hervor y el orden es el recibido")
    void recetaDePlanificacion_debeMostrarElTiempoDeHervorSoloParaHervor() throws Exception {
        VersionRecetaResponseDTO version = versionDeRecetaBuilder("Pale Ale", true)
                .detallesLupulo(List.of(
                        DetalleLupuloResponseDTO.builder().id(1L).cantidad(25.0).uso(UsoLupulo.HERVOR)
                                .etapaDeUso(TipoEtapa.HERVIDO).tiempoDeHervor(60.0)
                                .lupulo(LupuloResponseDTO.builder().id(1L).nombre("Cascade").formato(FormatoLupulo.PELLET).aa(5.5).build()).build(),
                        DetalleLupuloResponseDTO.builder().id(2L).cantidad(40.0).uso(UsoLupulo.DRY_HOP)
                                .etapaDeUso(TipoEtapa.FERMENTACION)
                                .lupulo(LupuloResponseDTO.builder().id(2L).nombre("Citra").formato(FormatoLupulo.FLOR).aa(12.0).build()).build(),
                        DetalleLupuloResponseDTO.builder().id(3L).cantidad(30.0).uso(UsoLupulo.WHIRLPOOL)
                                .etapaDeUso(TipoEtapa.HERVIDO)
                                .lupulo(LupuloResponseDTO.builder().id(3L).nombre("Mosaic").formato(FormatoLupulo.PELLET).aa(11.5).build()).build()))
                .build();
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacion(version, 3, 3, Estado.ACTIVO));

        mockMvc.perform(get("/planificaciones-produccion/7/receta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lúpulos (cantidad en g, tiempo de hervor en minutos)")))
                .andExpect(content().string(containsString("Tiempo de hervor")))
                .andExpect(content().string(matchesPattern(
                        "(?s).*Cascade.*?>\\s*Pellet\\s*<.*?>\\s*5,5\\s*<.*?>\\s*Hervor\\s*<.*?>\\s*Hervido\\s*<.*?>\\s*60\\s*<.*?>\\s*25\\s*<.*"
                                + "Citra.*?>\\s*Flor\\s*<.*?>\\s*12\\s*<.*?>\\s*Dry hop\\s*<.*?>\\s*Fermentación\\s*<.*?>\\s*-\\s*<.*?>\\s*40\\s*<.*"
                                + "Mosaic.*?>\\s*Pellet\\s*<.*?>\\s*11,5\\s*<.*?>\\s*Whirlpool\\s*<.*?>\\s*Hervido\\s*<.*?>\\s*-\\s*<.*?>\\s*30\\s*<.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-80: la receta de la planificación lista todas las levaduras con nombre, tipo legible y cantidad, en el orden recibido")
    void recetaDePlanificacion_debeListarTodasLasLevaduras() throws Exception {
        VersionRecetaResponseDTO version = versionDeRecetaBuilder("Pale Ale", true)
                .detallesLevadura(List.of(
                        DetalleLevaduraResponseDTO.builder().id(1L).cantidad(11.5)
                                .levadura(LevaduraResponseDTO.builder().id(1L).nombre("US-05").tipo(TipoLevadura.ALE).build()).build(),
                        DetalleLevaduraResponseDTO.builder().id(2L).cantidad(12.0)
                                .levadura(LevaduraResponseDTO.builder().id(2L).nombre("W-34/70").tipo(TipoLevadura.LAGER).build()).build()))
                .build();
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacion(version, 3, 3, Estado.ACTIVO));

        mockMvc.perform(get("/planificaciones-produccion/7/receta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Levaduras (cantidad en g)")))
                .andExpect(content().string(matchesPattern("(?s).*US-05.*?>\\s*Ale\\s*<.*?>\\s*11,5\\s*<.*W-34/70.*?>\\s*Lager\\s*<.*?>\\s*12\\s*<.*")))
                .andExpect(content().string(not(containsString("La receta no tiene levaduras"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-81: las secciones sin detalles informan que la receta no tiene maltas, lúpulos ni levaduras")
    void recetaDePlanificacion_debeInformarLasSeccionesSinDetalles() throws Exception {
        VersionRecetaResponseDTO version = versionDeRecetaBuilder("Pale Ale", true)
                .detallesMalta(List.of()).detallesLupulo(List.of()).detallesLevadura(List.of()).build();
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacion(version, 3, 3, Estado.ACTIVO));

        mockMvc.perform(get("/planificaciones-produccion/7/receta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La receta no tiene maltas")))
                .andExpect(content().string(containsString("La receta no tiene lúpulos")))
                .andExpect(content().string(containsString("La receta no tiene levaduras")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-82: la pantalla es de solo lectura: sin Editar, sin links a la receta, sin baja y sin formularios en su contenido")
    void recetaDePlanificacion_esDeSoloLectura() throws Exception {
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacionBase());

        String html = mockMvc.perform(get("/planificaciones-produccion/7/receta")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // El header del layout tiene su propio formulario de cierre de sesión: se revisa el contenido de la página
        Matcher contenido = Pattern.compile("(?s)<main.*?</main>").matcher(html);
        assertThat(contenido.find()).isTrue();
        assertThat(contenido.group())
                .doesNotContain("Editar")
                .doesNotContain("/recetas/3/editar")
                .doesNotContain("/recetas/3")
                .doesNotContain("data-baja-url")
                .doesNotContain("dialogo-baja")
                .doesNotContain("<form")
                .doesNotContain("<input")
                .doesNotContain("_csrf");
        assertThat(html).doesNotContain("/recetas/3/editar").doesNotContain("data-baja-url");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-83: \"Volver a la planificación\" lleva al estado actual de la misma planificación y no al listado")
    void recetaDePlanificacion_debeVolverALaPlanificacion() throws Exception {
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacionBase());

        mockMvc.perform(get("/planificaciones-produccion/7/receta"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("(?s).*<a [^>]*href=\"/planificaciones-produccion/7\"[^>]*>\\s*<i class=\"bi bi-arrow-left\"></i> Volver a la planificación.*")))
                .andExpect(content().string(not(containsString("Volver a planificaciones"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-84: la receta se ve igual sea cual sea el estado de la planificación: el controller no consulta la planificación")
    void recetaDePlanificacion_seVeEnCualquierEstadoDeLaPlanificacion() throws Exception {
        for (long id : List.of(7L, 8L, 9L)) {
            when(planificacionServicio.buscarVersionRecetaDePlanificacion(id)).thenReturn(recetaDePlanificacion(id));

            mockMvc.perform(get("/planificaciones-produccion/" + id + "/receta"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("planificacion_produccion/planificacion-receta"))
                    .andExpect(content().string(containsString("Pale Ale")))
                    .andExpect(content().string(containsString("Versión 3 de 3")));
        }

        verify(planificacionServicio, never()).buscarPorId(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-85: una planificación inexistente vuelve al origen con la alerta de error del service y no renderiza la vista")
    void recetaDePlanificacion_debeMostrarElErrorSiLaPlanificacionNoExiste() throws Exception {
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(99L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la planificación de producción con ID: 99"));

        mockMvc.perform(get("/planificaciones-produccion/99/receta").header("Referer", "/planificaciones-produccion"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/planificaciones-produccion"))
                .andExpect(flash().attribute("mensaje", "No se encontró la planificación de producción con ID: 99"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-PPC-86: el GERENTE_DE_COMPRAS no puede ver la receta de la planificación y no llega al service")
    void recetaDePlanificacion_debeRechazarAlGerenteDeCompras() throws Exception {
        mockMvc.perform(get("/planificaciones-produccion/7/receta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @DisplayName("CP-PPC-87: el OPERARIO_DE_PRODUCCION y el ADMINISTRADOR no pueden ver la receta de la planificación y no llegan al service")
    void recetaDePlanificacion_debeRechazarOtrosRoles() throws Exception {
        for (String rol : List.of("OPERARIO_DE_PRODUCCION", "ADMINISTRADOR")) {
            mockMvc.perform(get("/planificaciones-produccion/7/receta").with(user("x").roles(rol)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"));
        }

        verifyNoInteractions(planificacionServicio);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-88: la pantalla hace un único llamado a buscarVersionRecetaDePlanificacion y no usa ningún otro service")
    void recetaDePlanificacion_debeHacerUnSoloLlamadoAlService() throws Exception {
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacionBase());

        mockMvc.perform(get("/planificaciones-produccion/7/receta")).andExpect(status().isOk());

        verify(planificacionServicio, times(1)).buscarVersionRecetaDePlanificacion(7L);
        verifyNoMoreInteractions(planificacionServicio);
        verifyNoInteractions(recetaServicio, loteServicio);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-PPC-89: en la sidebar \"Planificación de Producción\" queda resaltada como opción activa")
    void recetaDePlanificacion_laSidebarResaltaLaPlanificacion() throws Exception {
        when(planificacionServicio.buscarVersionRecetaDePlanificacion(7L)).thenReturn(recetaDePlanificacionBase());

        mockMvc.perform(get("/planificaciones-produccion/7/receta"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern(
                        "(?s).*<a href=\"/planificaciones-produccion\"[^>]*class=\"[^\"]*bg-amber-500/10 text-amber-500[^\"]*\"[^>]*>\\s*<i class=\"bi text-lg bi-calendar-check\"></i>\\s*<span>Planificación de Producción</span>.*")));
    }

    // ==================== Enlaces con el módulo de lotes ====================

    @Test
    @DisplayName("CP-PPC-90: una planificación Pendiente muestra a quien tiene LOTE_REGISTRAR el link \"Registrar lote\" hacia el formulario con la planificación elegida, junto a Ver receta, Finalizar y Anular")
    void detalle_pendienteMuestraRegistrarLote() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        String html = mockMvc.perform(get("/planificaciones-produccion/7").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/lotes/nuevo?idPlanificacion=7\"")))
                .andExpect(content().string(containsString("Registrar lote")))
                .andReturn().getResponse().getContentAsString();

        Matcher enlace = Pattern.compile("(?s)<a[^>]*href=\"/lotes/nuevo\\?idPlanificacion=7\"[^>]*>(.*?)</a>").matcher(html);
        assertThat(enlace.find()).isTrue();
        assertThat(enlace.group()).contains("bi-plus-lg").contains("whitespace-nowrap");
        assertThat(html).contains("Ver receta").contains("data-motivo-url=\"/planificaciones-produccion/7/finalizar\"")
                .contains("data-motivo-url=\"/planificaciones-produccion/7/anular\"");
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = EstadoSolicitud.class, names = {"FINALIZADA", "ANULADA"})
    @DisplayName("CP-PPC-91: una planificación Finalizada o Anulada no muestra el link \"Registrar lote\"")
    void detalle_cerradaNoMuestraRegistrarLote(EstadoSolicitud estado) throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", estado));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        mockMvc.perform(get("/planificaciones-produccion/7").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Registrar lote"))))
                .andExpect(content().string(not(containsString("/lotes/nuevo"))));
    }

    @Test
    @DisplayName("CP-PPC-92: el GERENTE_DE_COMPRAS (sin LOTE_REGISTRAR) ve el estado actual completo sin el link \"Registrar lote\"")
    void detalle_soloElQueRegistraLotesVeRegistrarLote() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(lote(1L, "PALE_ALE-R7-1", EstadoLote.PENDIENTE, "F-01")));

        mockMvc.perform(get("/planificaciones-produccion/7").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Estado actual de la planificación")))
                .andExpect(content().string(containsString("PALE_ALE-R7-1")))
                .andExpect(content().string(not(containsString("Registrar lote"))))
                .andExpect(content().string(not(containsString("/lotes/nuevo"))));
    }

    @Test
    @DisplayName("CP-PPC-93: la tabla de lotes lleva por fila el link \"Ver\" al detalle del lote para quien tiene LOTE_CONSULTAR")
    void detalle_laTablaDeLotesEnlazaAlDetalle() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(
                lote(11L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01"),
                lote(12L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01")));

        String html = mockMvc.perform(get("/planificaciones-produccion/7").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> enlaces = new ArrayList<>();
        Matcher m = Pattern.compile("(?s)<a[^>]*aria-label=\"Ver lote\"[^>]*>(.*?)</a>").matcher(html);
        while (m.find()) {
            enlaces.add(m.group());
        }
        assertThat(enlaces).hasSize(2);
        assertThat(enlaces.get(0)).contains("href=\"/lotes/11\"").contains("title=\"Ver lote\"").contains("bi-eye").contains("Ver");
        assertThat(enlaces.get(1)).contains("href=\"/lotes/12\"");
    }

    @Test
    @DisplayName("CP-PPC-94: sin LOTE_CONSULTAR la tabla de lotes se ve completa pero sin links \"Ver\" ni columna de acciones")
    void detalle_sinLoteConsultarNoHayLinksVer() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of(
                lote(11L, "PALE_ALE-R7-1", EstadoLote.FINALIZADO, "F-01"),
                lote(12L, "PALE_ALE-R7-2", EstadoLote.PENDIENTE, "F-01")));

        mockMvc.perform(get("/planificaciones-produccion/7").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PALE_ALE-R7-1")))
                .andExpect(content().string(containsString("PALE_ALE-R7-2")))
                .andExpect(content().string(not(containsString("aria-label=\"Ver lote\""))))
                .andExpect(content().string(not(containsString("/lotes/"))))
                .andExpect(content().string(not(containsString("Acciones</th>"))));
    }

    @Test
    @DisplayName("CP-PPC-95: una planificación pendiente sin lotes muestra el mensaje de vacío, ningún link \"Ver\" y conserva el link \"Registrar lote\"; la fila vacía abarca todas las columnas de la tabla")
    void detalle_sinLotesConservaRegistrarLote() throws Exception {
        when(planificacionServicio.buscarPorId(7L)).thenReturn(planificacion(7L, "Pale Ale", EstadoSolicitud.PENDIENTE));
        when(loteServicio.listarLotesDePlanificacion(7L)).thenReturn(List.of());

        mockMvc.perform(get("/planificaciones-produccion/7").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(matchesMensajeVacioDosVeces())
                .andExpect(content().string(not(containsString("aria-label=\"Ver lote\""))))
                .andExpect(content().string(containsString("href=\"/lotes/nuevo?idPlanificacion=7\"")))
                .andExpect(content().string(containsString("colspan=\"10\"")));

        // Sin LOTE_CONSULTAR la tabla no tiene la columna Acciones
        mockMvc.perform(get("/planificaciones-produccion/7").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("colspan=\"9\"")));
    }

    // ==================== Datos de prueba ====================

    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }

    private static org.springframework.test.web.servlet.ResultMatcher matchesMensajeVacioDosVeces() {
        return result -> {
            String html = result.getResponse().getContentAsString();
            String mensaje = "No hay lotes registrados para esta planificación";
            assertThat(html.split(mensaje, -1).length - 1).isEqualTo(2);
        };
    }

    private static PlanificacionProduccionResponseDTO planificacion(Long id, String nombreReceta, EstadoSolicitud estado) {
        return planificacionBuilder(id, nombreReceta, estado).build();
    }

    private static PlanificacionProduccionResponseDTO.PlanificacionProduccionResponseDTOBuilder planificacionBuilder(
            Long id, String nombreReceta, EstadoSolicitud estado) {
        return PlanificacionProduccionResponseDTO.builder()
                .id(id)
                .fechaInicioEstimada(LocalDate.of(2027, 7, 1))
                .fechaFinalizacionEstimada(LocalDate.of(2027, 7, 25))
                .cantidadAProducir(600.0)
                .estado(estado)
                .versionReceta(VersionRecetaResponseDTO.builder().id(70L).nombre(nombreReceta).volumenBase(20.0).build());
    }

    private static RecetaResponseDTO receta(Long id, String nombre) {
        return RecetaResponseDTO.builder().id(id)
                .version(VersionRecetaResponseDTO.builder().id(70L).nombre(nombre).volumenBase(20.0).build())
                .build();
    }

    private static VersionRecetaDePlanificacionResponseDTO recetaDePlanificacionBase() {
        return recetaDePlanificacion(7L);
    }

    private static VersionRecetaDePlanificacionResponseDTO recetaDePlanificacion(Long idPlanificacion) {
        return recetaDePlanificacion(idPlanificacion, versionDeRecetaBuilder("Pale Ale", true).build(), 3, 3, Estado.ACTIVO);
    }

    private static VersionRecetaDePlanificacionResponseDTO recetaDePlanificacion(VersionRecetaResponseDTO version, int numeroVersion,
                                                                                int cantidadVersiones, Estado estadoReceta) {
        return recetaDePlanificacion(7L, version, numeroVersion, cantidadVersiones, estadoReceta);
    }

    private static VersionRecetaDePlanificacionResponseDTO recetaDePlanificacion(Long idPlanificacion, VersionRecetaResponseDTO version,
                                                                                int numeroVersion, int cantidadVersiones, Estado estadoReceta) {
        return VersionRecetaDePlanificacionResponseDTO.builder()
                .idPlanificacion(idPlanificacion)
                .idReceta(3L)
                .estadoReceta(estadoReceta)
                .numeroVersion(numeroVersion)
                .cantidadVersiones(cantidadVersiones)
                .version(version)
                .build();
    }

    /**
     * Versión del escenario base de la receta de una planificación: "Pale Ale" con una malta, un lúpulo en Hervor y una levadura.
     */
    private static VersionRecetaResponseDTO.VersionRecetaResponseDTOBuilder versionDeRecetaBuilder(String nombre, boolean esUltimaVersion) {
        return VersionRecetaResponseDTO.builder()
                .id(70L)
                .nombre(nombre)
                .volumenBase(20.0)
                .relacionDeEmpaste(3.0)
                .ogObjetivo(1.05)
                .fgObjetivo(1.01)
                .ibuObjetivo(35)
                .duracionMaceracion(60)
                .duracionHervido(90)
                .duracionFermentacion(10)
                .duracionMaduracion(4)
                .esUltimaVersion(esUltimaVersion)
                .detallesMalta(List.of(DetalleMaltaResponseDTO.builder().id(1L).cantidad(4.5)
                        .malta(MaltaResponseDTO.builder().id(1L).nombre("Pilsen").tipo(TipoMalta.BASE).build()).build()))
                .detallesLupulo(List.of(DetalleLupuloResponseDTO.builder().id(1L).cantidad(25.0).uso(UsoLupulo.HERVOR)
                        .etapaDeUso(TipoEtapa.HERVIDO).tiempoDeHervor(60.0)
                        .lupulo(LupuloResponseDTO.builder().id(1L).nombre("Cascade").formato(FormatoLupulo.PELLET).aa(5.5).build()).build()))
                .detallesLevadura(List.of(DetalleLevaduraResponseDTO.builder().id(1L).cantidad(11.5)
                        .levadura(LevaduraResponseDTO.builder().id(1L).nombre("US-05").tipo(TipoLevadura.ALE).build()).build()));
    }

    private static LoteCronogramaResponseDTO loteEn(Long id, String identificador, EstadoLote estado, String fermentador,
                                                    LocalDate inicioEstimado, LocalDate finEstimado) {
        return lote(id, identificador, estado, fermentador).toBuilder()
                .fechaInicioEstimada(inicioEstimado)
                .fechaFinalizacionEstimada(finEstimado)
                .build();
    }

    private static LoteCronogramaResponseDTO lote(Long id, String identificador, EstadoLote estado, String fermentador) {
        LocalDate inicioEstimado = LocalDate.of(2027, 7, 1).plusDays((id - 1) * 5);
        return LoteCronogramaResponseDTO.builder()
                .id(id)
                .identificadorInterno(identificador)
                .estado(estado)
                .volumenObjetivo(300.0)
                .fechaInicioEstimada(inicioEstimado)
                .fechaFinalizacionEstimada(inicioEstimado.plusDays(4))
                .fechaInicio(estado == EstadoLote.PENDIENTE ? null : inicioEstimado.atTime(8, 0))
                .fechaFinalizacion(estado == EstadoLote.FINALIZADO ? inicioEstimado.plusDays(4).atTime(18, 0) : null)
                .fechaCancelacion(estado == EstadoLote.CANCELADO ? inicioEstimado.plusDays(2).atTime(10, 0) : null)
                .idPlanificacionProduccion(7L)
                .identificadorInternoFermentador(fermentador)
                .build();
    }

    private static SugerenciaFechaFinalizacionResponseDTO sugerencia(int lotes, double capacidad, long duracion, LocalDate fin) {
        return SugerenciaFechaFinalizacionResponseDTO.builder()
                .fechaInicioEstimada(LocalDate.of(2027, 7, 1))
                .fechaFinalizacionSugerida(fin)
                .cantidadLotes(lotes)
                .capacidadLoteEstandar(capacidad)
                .duracionLoteDias(duracion)
                .build();
    }
}
