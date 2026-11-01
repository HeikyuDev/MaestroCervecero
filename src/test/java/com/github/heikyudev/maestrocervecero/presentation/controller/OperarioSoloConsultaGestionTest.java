package com.github.heikyudev.maestrocervecero.presentation.controller;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.controller.barril.BarrilController;
import com.github.heikyudev.maestrocervecero.presentation.controller.barril.FabricanteBarrilController;
import com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento.FermentadorController;
import com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento.MaceradorController;
import com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento.MolinoController;
import com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento.OllaHervorController;
import com.github.heikyudev.maestrocervecero.presentation.controller.insumo.LevaduraController;
import com.github.heikyudev.maestrocervecero.presentation.controller.insumo.LupuloController;
import com.github.heikyudev.maestrocervecero.presentation.controller.insumo.MaltaController;
import com.github.heikyudev.maestrocervecero.presentation.controller.receta.RecetaController;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFabricanteBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IFermentadorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMaceradorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IMolinoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IOllaHervorServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILevaduraServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.ILupuloServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.insumo.IMaltaServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.heikyudev.maestrocervecero.presentation.controller.control_equipamiento.ControlEquipamientoControllerTest.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El operario de producción solo consulta las pantallas de Gestión de Equipamiento, Gestión de Insumos, Gestión de Barriles y
 * Gestión de Recetas: ve las listas con sus filtros, pero no ve ningún control de escritura (alta, modificar, baja) y no puede
 * ejecutarlos por URL ni por POST. El gerente de producción conserva todo. Los services son mocks que devuelven páginas vacías.
 */
@WebMvcTest(controllers = {MolinoController.class, MaceradorController.class, OllaHervorController.class, FermentadorController.class,
        MaltaController.class, LupuloController.class, LevaduraController.class, BarrilController.class, FabricanteBarrilController.class,
        RecetaController.class})
@Import({SecurityConfig.class, ControllerAdvices.class, OperarioSoloConsultaGestionTest.ServicesVacios.class})
class OperarioSoloConsultaGestionTest {

    private static final String[] LISTAS = {"/equipamiento/molinos", "/equipamiento/maceradores", "/equipamiento/ollas-hervor", "/equipamiento/fermentadores",
            "/insumos/maltas", "/insumos/lupulos", "/insumos/levaduras", "/barriles", "/barriles/fabricantes", "/recetas"};

    @TestConfiguration
    static class ServicesVacios {
        private static <T> T vacio(Class<T> tipo) {
            return Mockito.mock(tipo, invocacion -> {
                if (Page.class.isAssignableFrom(invocacion.getMethod().getReturnType())) {
                    Pageable pageable = java.util.Arrays.stream(invocacion.getArguments()).filter(Pageable.class::isInstance).map(Pageable.class::cast).findFirst().orElse(PageRequest.of(0, 20));
                    return new PageImpl<>(List.of(), pageable, 0);
                }
                return Mockito.RETURNS_DEFAULTS.answer(invocacion);
            });
        }

        @Bean IMolinoServicio molino() { return vacio(IMolinoServicio.class); }
        @Bean IMaceradorServicio macerador() { return vacio(IMaceradorServicio.class); }
        @Bean IOllaHervorServicio olla() { return vacio(IOllaHervorServicio.class); }
        @Bean IFermentadorServicio fermentador() { return vacio(IFermentadorServicio.class); }
        @Bean IMaltaServicio malta() { return vacio(IMaltaServicio.class); }
        @Bean ILupuloServicio lupulo() { return vacio(ILupuloServicio.class); }
        @Bean ILevaduraServicio levadura() { return vacio(ILevaduraServicio.class); }
        @Bean IBarrilServicio barril() { return vacio(IBarrilServicio.class); }
        @Bean IFabricanteBarrilServicio fabricante() { return vacio(IFabricanteBarrilServicio.class); }
        @Bean IRecetaServicio receta() { return vacio(IRecetaServicio.class); }
    }

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/equipamiento/molinos", "/equipamiento/maceradores", "/equipamiento/ollas-hervor", "/equipamiento/fermentadores",
            "/insumos/maltas", "/insumos/lupulos", "/insumos/levaduras", "/barriles", "/barriles/fabricantes", "/recetas"})
    @DisplayName("CP-OSC-01: el operario de producción abre la lista de cada módulo de gestión con sus filtros y sin ningún control de escritura (alta, modificar, baja)")
    void operarioVeLaListaSinEscritura(String lista) throws Exception {
        String html = mockMvc.perform(get(lista).with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("<form").doesNotContain("/nuevo\"").doesNotContain("/editar").doesNotContain("data-baja-url").doesNotContain("bi-plus-lg")
                .doesNotContain("bi-pencil-square").doesNotContain("bi-trash").doesNotContain("dialogo-baja");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/equipamiento/molinos", "/equipamiento/maceradores", "/equipamiento/ollas-hervor", "/equipamiento/fermentadores",
            "/insumos/maltas", "/insumos/lupulos", "/insumos/levaduras", "/barriles", "/barriles/fabricantes", "/recetas"})
    @DisplayName("CP-OSC-02: el gerente de producción conserva el botón de alta de cada lista")
    void gerenteConservaElAlta(String lista) throws Exception {
        String html = mockMvc.perform(get(lista).with(como(Rol.GERENTE_DE_PRODUCCION))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains(lista + "/nuevo\"").contains("bi-plus-lg");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/equipamiento/molinos", "/equipamiento/maceradores", "/equipamiento/ollas-hervor", "/equipamiento/fermentadores",
            "/insumos/maltas", "/insumos/lupulos", "/insumos/levaduras", "/barriles", "/barriles/fabricantes", "/recetas"})
    @DisplayName("CP-OSC-03: el operario no abre los formularios de alta ni de modificación y no puede dar de alta, modificar ni dar de baja por POST: es redirigido con la alerta de permisos")
    void operarioNoEscribe(String base) throws Exception {
        mockMvc.perform(get(base + "/nuevo").with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(get(base + "/1/editar").with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(base).with(csrf()).with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(base + "/1").with(csrf()).with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(base + "/1/baja").with(csrf()).with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "GERENTE_COMERCIAL", "ADMINISTRADOR"})
    @DisplayName("CP-OSC-04: los demás roles siguen sin acceso a las listas de gestión de equipamiento, insumos, barriles y recetas")
    void otrosRolesSinAcceso(Rol rol) throws Exception {
        for (String lista : LISTAS) {
            mockMvc.perform(get(lista).with(como(rol))).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        }
    }
}
