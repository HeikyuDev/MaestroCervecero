package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.reporte.IGeneradorPdfServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReportesDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La pestaña "Tiempos" sin la propiedad {@code estadisticas.tiempos.habilitada}: su reporte no se registra, por lo que no
 * aparece entre las pestañas y su clave, en la pantalla y en el PDF, se trata como una clave inexistente.
 */
@WebMvcTest(EstadisticasController.class)
@Import({SecurityConfig.class, ControllerAdvices.class, ReporteLotes.class, ReporteTiempos.class})
class EstadisticasTiemposDeshabilitadoControllerTest {

    private static final String URL = "/estadisticas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEstadisticaProduccionServicio estadisticaProduccionServicio;

    @MockitoBean
    private IRecetaServicio recetaServicio;

    @MockitoBean
    private IUsuarioServicio usuarioServicio;

    @MockitoBean
    private IGeneradorPdfServicio generadorPdfServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @Test
    @DisplayName("CP-ESTC-25: con la pestaña Tiempos deshabilitada no se muestra entre las pestañas y su clave, en la pantalla y en el PDF, cae en la primera pestaña del usuario sin consultar el reporte")
    void tiemposDeshabilitada_noSeAlcanza() throws Exception {
        when(estadisticaProduccionServicio.generarReporteDeLotes(any(), any())).thenReturn(ReportesDePrueba.lotes(2, 2));
        when(generadorPdfServicio.generar(any(), anyMap())).thenReturn(new byte[]{1});

        String html = HtmlUtils.htmlUnescape(mockMvc.perform(get(URL).param("pestana", "tiempos").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pestanaActiva", PestanaEstadistica.LOTES))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        mockMvc.perform(get(URL + "/pdf").param("pestana", "tiempos").with(como(Rol.GERENTE_DE_PRODUCCION)))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("estadisticas-lotes_")));

        assertThat(html).contains("data-pestana-lotes=\"true\"").doesNotContain("data-pestana-tiempos=");
        verify(estadisticaProduccionServicio, never()).generarReporteDeTiempos(any(), any());
    }

    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }
}
