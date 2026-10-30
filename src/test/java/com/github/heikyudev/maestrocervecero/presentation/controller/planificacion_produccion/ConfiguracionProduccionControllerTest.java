package com.github.heikyudev.maestrocervecero.presentation.controller.planificacion_produccion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanConcurrente;
import com.github.heikyudev.maestrocervecero.persistence.entity.planificacion_produccion.CriterioSeleccionPlanSecuencial;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.controller.login.HomeController;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.planificacion_produccion.ConfiguracionProduccionFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.costo_adicional.ICostoDirectoAdicionalServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.planificacion_produccion.IConfiguracionProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.costo_adicional.CostoDirectoAdicionalResponseDTO;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.github.heikyudev.maestrocervecero.service.response_dto.planificacion_produccion.ConfiguracionProduccionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

@WebMvcTest({ConfiguracionProduccionController.class, HomeController.class})
@Import({SecurityConfig.class, ControllerAdvices.class})
class ConfiguracionProduccionControllerTest {

    private static final String URL = "/configuracion-produccion";
    private static final String OBLIGATORIO = "<span class=\"text-rose-600\" title=\"Obligatorio\">*</span>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IConfiguracionProduccionServicio configuracionServicio;

    @MockitoBean
    private ICostoDirectoAdicionalServicio costoDirectoAdicionalServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== GET /configuracion-produccion ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CPC-01: el GERENTE_DE_PRODUCCION ve la configuración vigente en los cinco campos, sin ceros sobrantes, con asterisco, placeholder y unidades")
    void caso_cp_cpc_01() throws Exception {
        // === PREPARACION DE DATOS ===
        when(configuracionServicio.buscarConfiguracion()).thenReturn(configuracion());

        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracion/configuracion-produccion"))
                .andExpect(content().string(containsString("Configuración de Producción")))
                .andExpect(content().string(containsString("action=\"/configuracion-produccion\"")))
                .andExpect(content().string(containsString("novalidate")))
                .andExpect(content().string(containsString("Tiempos y capacidades")))
                .andExpect(content().string(containsString("Criterios de selección de equipos para planes de producción")))
                .andExpect(content().string(containsString("Capacidad de lote estándar (L)")))
                .andExpect(content().string(containsString("Velocidad de envasado (L/h)")))
                .andExpect(content().string(containsString("Porcentaje mínimo de consumo (%)")))
                .andExpect(content().string(containsString("name=\"capacidadLoteEstandar\" value=\"300\"")))
                .andExpect(content().string(containsString("name=\"velocidadEstandarEnvasado\" value=\"100.5\"")))
                .andExpect(content().string(containsString("name=\"porcentajeMinimoConsumoParaAvanzarEtapa\" value=\"90\"")))
                .andExpect(content().string(not(containsString("value=\"300.0\""))))
                .andExpect(content().string(containsString(OBLIGATORIO)))
                .andExpect(content().string(containsString("placeholder=\"Ej: 300\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 100\"")))
                .andExpect(content().string(containsString("placeholder=\"Ej: 90\"")))
                .andExpect(content().string(containsString("Guardar configuración")))
                .andExpect(content().string(not(containsString("No se pudo guardar"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CPC-02: cada campo explica para qué se usa con un texto de ayuda")
    void caso_cp_cpc_02() throws Exception {
        // === PREPARACION DE DATOS ===
        when(configuracionServicio.buscarConfiguracion()).thenReturn(configuracion());

        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Volumen por lote que usa la simulación al sugerir la fecha de finalización de una planificación.")))
                .andExpect(content().string(containsString("Se usa para estimar la duración del envasado al calcular las fechas de cada lote.")))
                .andExpect(content().string(containsString("Porcentaje de la cantidad requerida que debe estar consumido en cada insumo de la etapa")))
                .andExpect(content().string(containsString("Un número entre 0 y 100.")))
                .andExpect(content().string(containsString("Criterio con el que se elegirán los equipos al generar planes secuenciales.")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CPC-03: los dos selects muestran los criterios con nombres legibles y dejan elegido el vigente")
    void caso_cp_cpc_03() throws Exception {
        // === PREPARACION DE DATOS ===
        when(configuracionServicio.buscarConfiguracion()).thenReturn(configuracion());

        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Fermentador de mayor capacidad")))
                .andExpect(content().string(containsString("Fermentador que se libera más temprano")))
                .andExpect(content().string(containsString("Menor cantidad de lotes")))
                .andExpect(content().string(containsString("Equipos que se liberan más temprano")))
                .andExpect(content().string(not(containsString(">FERMENTADOR_MAYOR_CAPACIDAD<"))))
                .andExpect(content().string(not(containsString(">MENOR_CANTIDAD_DE_LOTES<"))))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"FERMENTADOR_LIBERACION_MAS_TEMPRANA\" selected=\"selected\">.*")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"MENOR_CANTIDAD_DE_LOTES\" selected=\"selected\">.*")))
                .andExpect(content().string(not(containsString("<option value=\"FERMENTADOR_MAYOR_CAPACIDAD\" selected"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL"})
    @DisplayName("CP-CPC-05: cualquier rol distinto del GERENTE_DE_PRODUCCION es rechazado al abrir la pantalla y no llega al service")
    void caso_cp_cpc_05(String rol) throws Exception {
        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(get(URL).with(user("usuario").roles(rol)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(configuracionServicio, never()).buscarConfiguracion();
    }

    @Test
    @DisplayName("CP-CPC-06: sin sesión iniciada se redirige al login")
    void caso_cp_cpc_06() throws Exception {
        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(configuracionServicio, never()).buscarConfiguracion();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CPC-07: si el service no encuentra la configuración, se redirige con una alerta y el mensaje del error")
    void caso_cp_cpc_07() throws Exception {
        // === PREPARACION DE DATOS ===
        when(configuracionServicio.buscarConfiguracion())
                .thenThrow(new RecursoNoEncontradoException("No se encontró la configuración de producción"));

        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se encontró la configuración de producción"))
                .andExpect(flash().attribute("tipo", "danger"));
    }

    // ==================== POST /configuracion-produccion ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CPC-08: guarda con los valores enviados, redirige a la pantalla y avisa que se actualizó")
    void caso_cp_cpc_08() throws Exception {
        // === PREPARACION DE DATOS ===
        when(configuracionServicio.actualizarConfiguracion(any())).thenReturn(configuracion());

        // === EJECUCION ===
        mockMvc.perform(post(URL).with(csrf())
                        .param("capacidadLoteEstandar", "250")
                        .param("velocidadEstandarEnvasado", "80.5")
                        .param("porcentajeMinimoConsumoParaAvanzarEtapa", "75")
                        .param("criterioSeleccionPlanSecuencial", "FERMENTADOR_MAYOR_CAPACIDAD")
                        .param("criterioSeleccionPlanConcurrente", "EQUIPOS_LIBERACION_MAS_TEMPRANA"))
                // === VERIFICACION ===
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Configuración de producción actualizada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<ConfiguracionProduccionFormDTO> captor = ArgumentCaptor.forClass(ConfiguracionProduccionFormDTO.class);
        verify(configuracionServicio).actualizarConfiguracion(captor.capture());
        assertThat(captor.getValue().getCapacidadLoteEstandar()).isEqualTo(250.0);
        assertThat(captor.getValue().getVelocidadEstandarEnvasado()).isEqualTo(80.5);
        assertThat(captor.getValue().getPorcentajeMinimoConsumoParaAvanzarEtapa()).isEqualTo(75.0);
        assertThat(captor.getValue().getCriterioSeleccionPlanSecuencial()).isEqualTo(CriterioSeleccionPlanSecuencial.FERMENTADOR_MAYOR_CAPACIDAD);
        assertThat(captor.getValue().getCriterioSeleccionPlanConcurrente()).isEqualTo(CriterioSeleccionPlanConcurrente.EQUIPOS_LIBERACION_MAS_TEMPRANA);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CPC-09: si el service rechaza por una regla, vuelve al formulario con el mensaje en el resumen de errores y lo tipeado conservado")
    void caso_cp_cpc_09() throws Exception {
        // === PREPARACION DE DATOS ===
        when(configuracionServicio.buscarConfiguracion()).thenReturn(configuracion());
        doThrow(new ReglaNegocioException("El porcentaje mínimo de consumo debe estar entre 0 y 100"))
                .when(configuracionServicio).actualizarConfiguracion(any());

        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(post(URL).with(csrf())
                        .param("capacidadLoteEstandar", "250")
                        .param("velocidadEstandarEnvasado", "80")
                        .param("porcentajeMinimoConsumoParaAvanzarEtapa", "150")
                        .param("criterioSeleccionPlanSecuencial", "FERMENTADOR_MAYOR_CAPACIDAD")
                        .param("criterioSeleccionPlanConcurrente", "EQUIPOS_LIBERACION_MAS_TEMPRANA"))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracion/configuracion-produccion"))
                .andExpect(content().string(containsString("No se pudo guardar la configuración")))
                .andExpect(content().string(containsString("debe estar entre 0 y 100")))
                .andExpect(content().string(containsString("name=\"capacidadLoteEstandar\" value=\"250\"")))
                .andExpect(content().string(containsString("name=\"velocidadEstandarEnvasado\" value=\"80\"")))
                .andExpect(content().string(containsString("name=\"porcentajeMinimoConsumoParaAvanzarEtapa\" value=\"150\"")))
                .andExpect(content().string(matchesPattern("(?s).*<option value=\"EQUIPOS_LIBERACION_MAS_TEMPRANA\" selected=\"selected\">.*")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CPC-10: un número mal escrito muestra \"Ingrese un número válido\" en el campo y no llega al service")
    void caso_cp_cpc_10() throws Exception {
        // === PREPARACION DE DATOS ===
        when(configuracionServicio.buscarConfiguracion()).thenReturn(configuracion());

        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(post(URL).with(csrf())
                        .param("capacidadLoteEstandar", "abc")
                        .param("velocidadEstandarEnvasado", "80")
                        .param("porcentajeMinimoConsumoParaAvanzarEtapa", "75")
                        .param("criterioSeleccionPlanSecuencial", "FERMENTADOR_MAYOR_CAPACIDAD")
                        .param("criterioSeleccionPlanConcurrente", "MENOR_CANTIDAD_DE_LOTES"))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracion/configuracion-produccion"))
                .andExpect(content().string(containsString("No se pudo guardar la configuración")))
                .andExpect(content().string(containsString("Ingres")))
                .andExpect(content().string(containsString("mero v")))
                .andExpect(content().string(containsString("border-rose-400")))
                .andExpect(content().string(containsString("name=\"velocidadEstandarEnvasado\" value=\"80\"")));

        verify(configuracionServicio, never()).actualizarConfiguracion(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CPC-11: los campos vacíos se informan como obligatorios, uno por uno, y no llegan al service")
    void caso_cp_cpc_11() throws Exception {
        // === PREPARACION DE DATOS ===
        when(configuracionServicio.buscarConfiguracion()).thenReturn(configuracion());

        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(post(URL).with(csrf())
                        .param("capacidadLoteEstandar", "")
                        .param("velocidadEstandarEnvasado", "")
                        .param("porcentajeMinimoConsumoParaAvanzarEtapa", "")
                        .param("criterioSeleccionPlanSecuencial", "")
                        .param("criterioSeleccionPlanConcurrente", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La capacidad de lote est")))
                .andExpect(content().string(containsString("La velocidad de envasado es obligatoria")))
                .andExpect(content().string(containsString("El porcentaje m")))
                .andExpect(content().string(containsString("El criterio de selecci")))
                .andExpect(content().string(containsString("es obligatori")));

        verify(configuracionServicio, never()).actualizarConfiguracion(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL"})
    @DisplayName("CP-CPC-12: cualquier rol distinto del GERENTE_DE_PRODUCCION es rechazado al guardar y no llega al service")
    void caso_cp_cpc_12(String rol) throws Exception {
        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(post(URL).with(csrf()).with(user("usuario").roles(rol))
                        .param("capacidadLoteEstandar", "250")
                        .param("velocidadEstandarEnvasado", "80")
                        .param("porcentajeMinimoConsumoParaAvanzarEtapa", "75")
                        .param("criterioSeleccionPlanSecuencial", "FERMENTADOR_MAYOR_CAPACIDAD")
                        .param("criterioSeleccionPlanConcurrente", "MENOR_CANTIDAD_DE_LOTES"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(configuracionServicio, never()).actualizarConfiguracion(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CPC-13: guardar sin token CSRF es rechazado (redirige) y no llega al service")
    void caso_cp_cpc_13() throws Exception {
        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(post(URL)
                        .param("capacidadLoteEstandar", "250")
                        .param("velocidadEstandarEnvasado", "80")
                        .param("porcentajeMinimoConsumoParaAvanzarEtapa", "75")
                        .param("criterioSeleccionPlanSecuencial", "FERMENTADOR_MAYOR_CAPACIDAD")
                        .param("criterioSeleccionPlanConcurrente", "MENOR_CANTIDAD_DE_LOTES"))
                .andExpect(status().is3xxRedirection());

        verify(configuracionServicio, never()).actualizarConfiguracion(any());
    }

    // ==================== Sidebar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-CPC-14: la sidebar del GERENTE_DE_PRODUCCION muestra la sección \"Configuración\" con \"Configuración de Producción\" (bi-sliders), activa en esta pantalla")
    void caso_cp_cpc_14() throws Exception {
        // === PREPARACION DE DATOS ===
        when(configuracionServicio.buscarConfiguracion()).thenReturn(configuracion());

        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/configuracion-produccion\"")))
                .andExpect(content().string(containsString("bi-sliders")))
                .andExpect(content().string(containsString("border-amber-500")))
                .andExpect(content().string(matchesPattern("(?s).*Administración.*>Configuración<.*Configuración de Producción.*")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRADOR", "OPERARIO_DE_PRODUCCION", "GERENTE_DE_COMPRAS", "ENCARGADO_DE_DEPOSITO", "GERENTE_COMERCIAL"})
    @DisplayName("CP-CPC-15: la sidebar de los demás roles no muestra ni la sección \"Configuración\" ni el enlace a la pantalla")
    void caso_cp_cpc_15(String rol) throws Exception {
        // === EJECUCION Y VERIFICACION ===
        mockMvc.perform(get("/home").with(user("usuario").roles(rol)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/configuracion-produccion\""))))
                .andExpect(content().string(not(containsString("bi-sliders"))))
                .andExpect(content().string(not(containsString(">Configuración<"))));
    }

    // ==================== Datos de prueba ====================

    private static ConfiguracionProduccionResponseDTO configuracion() {
        return ConfiguracionProduccionResponseDTO.builder()
                .id(1L)
                .capacidadLoteEstandar(300.0)
                .velocidadEstandarEnvasado(100.5)
                .porcentajeMinimoConsumoParaAvanzarEtapa(90.0)
                .criterioSeleccionPlanSecuencial(CriterioSeleccionPlanSecuencial.FERMENTADOR_LIBERACION_MAS_TEMPRANA)
                .criterioSeleccionPlanConcurrente(CriterioSeleccionPlanConcurrente.MENOR_CANTIDAD_DE_LOTES)
                .build();
    }

    // ==================== Sección "Costos directos adicionales" ====================

    @Test
    @DisplayName("CP-CPC-16: el GERENTE_DE_PRODUCCION ve la sección Costos directos adicionales con el botón de alta, los costos en orden alfabético con el importe formateado y las acciones de editar y dar de baja")
    void seccionDeCostosDirectos_debeMostrarLaLista() throws Exception {
        // Arrange
        when(configuracionServicio.buscarConfiguracion()).thenReturn(configuracion());
        when(costoDirectoAdicionalServicio.filtrarCostosDirectosAdicionales(any(), any())).thenReturn(new PageImpl<>(List.of(
                costo(5L, "Gas butano", "12.5000"), costo(6L, "Dióxido de carbono", "1234.5")), PageRequest.of(0, 20), 2));

        // Act
        String html = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("data-costos-directos").contains("Costos directos adicionales").contains("href=\"/configuracion-produccion/costos-directos-adicionales/nuevo\"")
                .contains("Nuevo costo directo adicional").contains(">12,50<").contains(">1.234,50<").contains("Costo por litro ($ por litro)")
                .contains("href=\"/configuracion-produccion/costos-directos-adicionales/5/editar\"")
                .contains("data-baja-url=\"/configuracion-produccion/costos-directos-adicionales/6/baja\"").contains("data-baja-nombre=\"Gas butano\"")
                .contains("class=\"col-acciones px-4 py-3 text-right\">Acciones");
        assertThat(html).containsPattern("(?s)<dialog id=\"dialogo-baja\".*name=\"_csrf\".*</dialog>");
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(costoDirectoAdicionalServicio).filtrarCostosDirectosAdicionales(org.mockito.ArgumentMatchers.isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().stream().map(org.springframework.data.domain.Sort.Order::getProperty).toList()).containsExactly("nombre", "id");
    }

    @Test
    @DisplayName("CP-CPC-17: sin costos la sección lo informa, y sin los permisos de costos la sección no se muestra")
    void seccionDeCostosDirectos_sinCostosOSinPermisos() throws Exception {
        // Arrange
        when(configuracionServicio.buscarConfiguracion()).thenReturn(configuracion());
        when(costoDirectoAdicionalServicio.filtrarCostosDirectosAdicionales(any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // Act
        String vacia = mockMvc.perform(get(URL).with(como(Rol.GERENTE_DE_PRODUCCION))).andReturn().getResponse().getContentAsString();
        String sinPermisos = mockMvc.perform(get(URL).with(user("gp").authorities(new SimpleGrantedAuthority("ROLE_GERENTE_DE_PRODUCCION"))))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(vacia).contains("No hay costos directos adicionales registrados");
        assertThat(sinPermisos).doesNotContain("data-costos-directos").doesNotContain("Nuevo costo directo adicional").doesNotContain("dialogo-baja");
    }

    @Test
    @DisplayName("CP-CPC-18: la lista de costos pide la página indicada y, con la configuración rechazada, la pantalla vuelve con la sección cargada")
    void seccionDeCostosDirectos_debePedirLaPaginaYVolverCargadaConErrores() throws Exception {
        // Arrange
        when(configuracionServicio.buscarConfiguracion()).thenReturn(configuracion());
        when(costoDirectoAdicionalServicio.filtrarCostosDirectosAdicionales(any(), any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 25));

        // Act
        mockMvc.perform(get(URL).param("pagina", "1").with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk());
        mockMvc.perform(post(URL).with(como(Rol.GERENTE_DE_PRODUCCION)).with(csrf()).param("capacidadLoteEstandar", ""))
                .andExpect(status().isOk()).andExpect(content().string(containsString("data-costos-directos")));

        // Assert
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(costoDirectoAdicionalServicio, org.mockito.Mockito.atLeastOnce()).filtrarCostosDirectosAdicionales(any(), captor.capture());
        assertThat(captor.getAllValues().get(0).getPageNumber()).isEqualTo(1);
    }

    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }

    private static CostoDirectoAdicionalResponseDTO costo(Long id, String nombre, String importe) {
        return CostoDirectoAdicionalResponseDTO.builder().id(id).nombre(nombre).costoPorLitro(new BigDecimal(importe)).build();
    }
}
