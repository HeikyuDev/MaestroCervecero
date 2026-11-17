package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IControlEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaComprasServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.PestanaEstadistica;
import com.github.heikyudev.maestrocervecero.service.interfaces.proveedor.IProveedorServicio;
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
 * La pestaña "Recepción" sin la propiedad {@code estadisticas.recepcion.habilitada}: su reporte no se registra, por lo que
 * no se muestra entre las pestañas y su dirección cae en la primera pestaña del usuario.
 */
@WebMvcTest(EstadisticasController.class)
@Import({SecurityConfig.class, ControllerAdvices.class, ReporteCompras.class, ReporteProveedores.class, ReportePrecios.class,
        ReporteRecepcion.class})
class EstadisticasRecepcionDeshabilitadaControllerTest {

    private static final String URL = "/estadisticas";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEstadisticaProduccionServicio estadisticaProduccionServicio;

    @MockitoBean
    private IEstadisticaComprasServicio estadisticaComprasServicio;

    @MockitoBean
    private IProveedorServicio proveedorServicio;

    @MockitoBean
    private IRecetaServicio recetaServicio;

    @MockitoBean
    private IControlEquipamientoServicio controlEquipamientoServicio;

    @MockitoBean
    private IUsuarioServicio usuarioServicio;

    @MockitoBean
    private IGeneradorPdfServicio generadorPdfServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @Test
    @DisplayName("CP-ESTC-28: con la pestaña Recepción deshabilitada el GERENTE_DE_COMPRAS ve tres pestañas (Compras, Proveedores y Precios) y la clave de Recepción, en la pantalla y en el PDF, cae en la primera sin consultar el reporte")
    void recepcionDeshabilitada_noSeAlcanza() throws Exception {
        when(estadisticaComprasServicio.generarReporteDeCompras(any(), any())).thenReturn(ReportesDePrueba.lotes(2, 2));
        when(generadorPdfServicio.generar(any(), anyMap())).thenReturn(new byte[]{1});

        String html = HtmlUtils.htmlUnescape(mockMvc.perform(get(URL).param("pestana", "recepcion").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pestanaActiva", PestanaEstadistica.COMPRAS))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        mockMvc.perform(get(URL + "/pdf").param("pestana", "recepcion").with(como(Rol.GERENTE_DE_COMPRAS)))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("estadisticas-compras_")));

        assertThat(html).contains("data-pestana-compras=\"true\"").contains("data-pestana-proveedores=\"true\"").contains("data-pestana-precios=\"true\"")
                .doesNotContain("data-pestana-recepcion=");
        verify(estadisticaComprasServicio, never()).generarReporteDeRecepcion(any(), any());
    }

    private static RequestPostProcessor como(Rol rol) {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        rol.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return user(rol.name()).authorities(autoridades);
    }
}
