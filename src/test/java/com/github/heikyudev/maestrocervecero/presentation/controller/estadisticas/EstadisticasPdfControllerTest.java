package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.service.interfaces.estadisticas.IEstadisticaProduccionServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.receta.IRecetaServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.reporte.IGeneradorPdfServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.estadisticas.ReportesDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resiliencia del reporte impreso de {@link EstadisticasController}: falla al generar el documento y reportes en
 * generación simultánea. Se prueba el Controller solo, sin seguridad ni plantillas.
 */
class EstadisticasPdfControllerTest {

    private static final String URL_PDF = "/estadisticas/pdf";
    private static final String DESTINO = "/estadisticas?pestana=lotes&desde=2026-11-01&hasta=2026-11-30";

    private IEstadisticaProduccionServicio estadisticaProduccionServicio;
    private IGeneradorPdfServicio generadorPdfServicio;
    private Semaphore cupo;
    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        estadisticaProduccionServicio = mock(IEstadisticaProduccionServicio.class);
        generadorPdfServicio = mock(IGeneradorPdfServicio.class);
        cupo = new Semaphore(1);
        ReporteLotes reporte = new ReporteLotes(estadisticaProduccionServicio, mock(IRecetaServicio.class));
        EstadisticasController controller = new EstadisticasController(List.of(reporte), mock(IUsuarioServicio.class), generadorPdfServicio, cupo, 50);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(estadisticaProduccionServicio.generarReporteDeLotes(any(), any())).thenReturn(ReportesDePrueba.lotes(3, 3));
    }

    private static UsernamePasswordAuthenticationToken gerente() {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        Rol.GERENTE_DE_PRODUCCION.getPermisos().forEach(permiso -> autoridades.add(new SimpleGrantedAuthority(permiso.name())));
        return new UsernamePasswordAuthenticationToken("gerente", "n/a", autoridades);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder pedido() {
        return get(URL_PDF).param("pestana", "lotes").param("desde", "2026-11-01").param("hasta", "2026-11-30").principal(gerente());
    }

    @Test
    @DisplayName("CP-ESTC-21: si el documento no se puede generar, vuelve a la pantalla con los mismos parámetros y la alerta 'No se pudo generar el reporte impreso', y libera el lugar")
    void pdf_falloAlGenerar() throws Exception {
        when(generadorPdfServicio.generar(eq(EstadisticasController.PLANTILLA_PDF), anyMap())).thenThrow(new IllegalStateException("fallo de render"));

        mockMvc.perform(pedido())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DESTINO))
                .andExpect(flash().attribute("mensaje", "No se pudo generar el reporte impreso"))
                .andExpect(flash().attribute("tipo", "warning"));

        assertThat(cupo.availablePermits()).as("el lugar de generación se libera aunque falle").isEqualTo(1);
    }

    @Test
    @DisplayName("CP-ESTC-22: si hay otros reportes en generación y no se libera un lugar a tiempo, vuelve a la pantalla con la alerta de reintentar y no genera nada")
    void pdf_sinLugarParaGenerar() throws Exception {
        cupo.acquire();

        mockMvc.perform(pedido())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DESTINO))
                .andExpect(flash().attribute("mensaje", "Hay otros reportes en generación, intente nuevamente en unos instantes"))
                .andExpect(flash().attribute("tipo", "warning"));

        verifyNoInteractions(generadorPdfServicio);
        assertThat(cupo.availablePermits()).as("no se libera un lugar que no se tomó").isZero();
    }

    @Test
    @DisplayName("CP-ESTC-23: con lugar disponible el PDF se genera y el lugar se libera al terminar")
    void pdf_conLugar() throws Exception {
        when(generadorPdfServicio.generar(eq(EstadisticasController.PLANTILLA_PDF), anyMap())).thenReturn("%PDF-1.7".getBytes());

        mockMvc.perform(pedido()).andExpect(status().isOk());

        verify(generadorPdfServicio).generar(eq(EstadisticasController.PLANTILLA_PDF), anyMap());
        assertThat(cupo.availablePermits()).isEqualTo(1);
    }
}
