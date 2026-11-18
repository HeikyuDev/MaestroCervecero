package com.github.heikyudev.maestrocervecero.presentation.controller.cotizacion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cotizacion.CotizacionProveedorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.EnlaceCotizacionInvalidoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.ICotizacionProveedorServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.CotizacionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.LineaCotizacionProveedorResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.OpcionPresentacionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CotizacionProveedorController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class CotizacionProveedorControllerTest {

    private static final String TOKEN = "kV2s9fJ1x0QmB7r3TzYwLp4cHn8aUe6dGqXo5iSjNkA";
    private static final String URL = "/cotizacion/" + TOKEN;
    private static final String VISTA = "cotizacion/cotizacion-proveedor";
    private static final String VISTA_INVALIDO = "cotizacion/enlace-invalido";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICotizacionProveedorServicio cotizacionProveedorServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    private static final LocalDate ENTREGA = LocalDate.of(2026, 12, 5);

    /**
     * Cotización de MALTERIA SUR: la malta en Bolsa (25 kg) y Balde (5 kg) y el lúpulo en Paquete (1 kg). Con
     * {@code precio} la Bolsa ya está cotizada con esa cantidad y la fecha estimada de entrega {@link #ENTREGA}.
     */
    private static CotizacionProveedorResponseDTO cotizacion(LocalDateTime fechaRespuesta, String precio, Integer cantidad) {
        return CotizacionProveedorResponseDTO.builder().idSolicitud(90L).razonSocial("MALTERIA SUR").nombreComercial("Maltería Sur")
                .fechaLimiteRespuesta(LocalDateTime.of(2026, 11, 20, 9, 0)).fechaRespuesta(fechaRespuesta)
                .fechaEntregaEstimada(precio == null ? null : ENTREGA)
                .lineas(List.of(
                        LineaCotizacionProveedorResponseDTO.builder().idLinea(501L).nombreInsumo("Malta Pilsen").tipoInsumo("Malta").unidad("kg")
                                .cantidadSolicitada(150.0)
                                .presentaciones(List.of(OpcionPresentacionResponseDTO.builder().idCatalogoProveedor(100L).nombre("Bolsa (25 kg)")
                                                .precioUnitarioOfertado(precio == null ? null : new BigDecimal(precio)).cantidadDisponibleOfertada(cantidad).build(),
                                        OpcionPresentacionResponseDTO.builder().idCatalogoProveedor(101L).nombre("Balde (5 kg)").build()))
                                .build(),
                        LineaCotizacionProveedorResponseDTO.builder().idLinea(502L).nombreInsumo("Cascade").tipoInsumo("Lúpulo").unidad("g")
                                .cantidadSolicitada(500.0)
                                .presentaciones(List.of(OpcionPresentacionResponseDTO.builder().idCatalogoProveedor(102L).nombre("Paquete (1 kg)").build()))
                                .build()))
                .build();
    }

    @Test
    @DisplayName("CP-CPC-01: la pantalla es pública, sin el menú de la aplicación, y muestra por insumo una fila por presentación con la respuesta actual y la fecha estimada de entrega")
    void caso_cp_cpc_01() throws Exception {
        // Arrange
        when(cotizacionProveedorServicio.buscarCotizacion(TOKEN)).thenReturn(cotizacion(LocalDateTime.of(2026, 11, 19, 10, 15), "32500", 4));

        // Act
        String html = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html)
                .contains("Solicitud de cotización").contains("MALTERIA SUR")
                .contains("20/11/2026 09:00").contains("19/11/2026 10:15")
                .contains("action=\"/cotizacion/" + TOKEN + "\"").contains("novalidate")
                .contains("name=\"_csrf\"")
                .contains("Malta Pilsen").contains("Cantidad solicitada:").contains("150 kg").contains("500 g")
                .contains("name=\"ofertas[0].idLinea\" value=\"501\"").contains("name=\"ofertas[0].idCatalogoProveedor\" value=\"100\"")
                .contains("name=\"ofertas[1].idLinea\" value=\"501\"").contains("name=\"ofertas[1].idCatalogoProveedor\" value=\"101\"")
                .contains("name=\"ofertas[2].idLinea\" value=\"502\"").contains("name=\"ofertas[2].idCatalogoProveedor\" value=\"102\"")
                .contains("Bolsa (25 kg)").contains("Balde (5 kg)").contains("Paquete (1 kg)")
                .doesNotContain("<select")
                .contains("Fecha estimada de entrega").contains("name=\"fechaEntregaEstimada\" value=\"2026-12-05\"").contains("placeholder=\"Ej: 15/07/2027\"")
                .containsPattern("Fecha estimada de entrega <span class=\"text-rose-600\"[^>]*>\\*</span>")
                .contains("name=\"ofertas[0].precioUnitario\" value=\"32500\"")
                .contains("name=\"ofertas[1].precioUnitario\" value=\"\"")
                .contains("name=\"ofertas[0].cantidadDisponible\" value=\"4\"")
                .contains("placeholder=\"Ej: 32500\"").contains("placeholder=\"Ej: 10 (0 si no tiene stock)\"")
                .contains("Enviar cotización")
                .contains("<meta name=\"referrer\" content=\"no-referrer\">")
                .doesNotContain("Cerrar sesión").doesNotContain("href=\"/home\"");
    }

    @Test
    @DisplayName("CP-CPC-02: sin respuesta previa se aclara que todavía no respondió y las filas llegan vacías")
    void caso_cp_cpc_02() throws Exception {
        when(cotizacionProveedorServicio.buscarCotizacion(TOKEN)).thenReturn(cotizacion(null, null, null));

        String html = mockMvc.perform(get(URL)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Todavía no respondió")
                .contains("name=\"ofertas[0].precioUnitario\" value=\"\"")
                .contains("name=\"ofertas[2].cantidadDisponible\" value=\"\"")
                .contains("name=\"fechaEntregaEstimada\" value=\"\"");
    }

    @Test
    @DisplayName("CP-CPC-03: un enlace que ya no sirve muestra la pantalla que explica el motivo")
    void caso_cp_cpc_03() throws Exception {
        when(cotizacionProveedorServicio.buscarCotizacion(TOKEN))
                .thenThrow(new EnlaceCotizacionInvalidoException("La solicitud de cotización ya cerró: el plazo para responder venció"));

        String html = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_INVALIDO))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("El enlace no está disponible").contains("La solicitud de cotización ya cerró: el plazo para responder venció");
    }

    @Test
    @DisplayName("CP-CPC-04: enviar la cotización delega las filas en el service y vuelve al formulario con el aviso de que puede modificarla hasta el cierre")
    void caso_cp_cpc_04() throws Exception {
        // Arrange
        when(cotizacionProveedorServicio.registrarCotizacion(eq(TOKEN), any())).thenReturn(cotizacion(LocalDateTime.now(), "32500", 4));

        // Act + Assert
        mockMvc.perform(post(URL).with(csrf())
                        .param("fechaEntregaEstimada", "2026-12-05")
                        .param("ofertas[0].idLinea", "501").param("ofertas[0].idCatalogoProveedor", "100")
                        .param("ofertas[0].precioUnitario", "32500.50").param("ofertas[0].cantidadDisponible", "4")
                        .param("ofertas[1].idLinea", "501").param("ofertas[1].idCatalogoProveedor", "101")
                        .param("ofertas[1].precioUnitario", "6000").param("ofertas[1].cantidadDisponible", "3")
                        .param("ofertas[2].idLinea", "502").param("ofertas[2].idCatalogoProveedor", "102")
                        .param("ofertas[2].precioUnitario", "").param("ofertas[2].cantidadDisponible", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("mensaje", "Cotización enviada correctamente: puede modificarla hasta el 20/11/2026 09:00"))
                .andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<CotizacionProveedorFormDTO> captor = ArgumentCaptor.forClass(CotizacionProveedorFormDTO.class);
        verify(cotizacionProveedorServicio).registrarCotizacion(eq(TOKEN), captor.capture());
        assertThat(captor.getValue().getFechaEntregaEstimada()).isEqualTo(ENTREGA);
        assertThat(captor.getValue().getOfertas()).hasSize(3);
        assertThat(captor.getValue().getOfertas().get(0).getPrecioUnitario()).isEqualByComparingTo("32500.50");
        assertThat(captor.getValue().getOfertas().get(0).getCantidadDisponible()).isEqualTo(4);
        assertThat(captor.getValue().getOfertas().get(1).getIdCatalogoProveedor()).isEqualTo(101L);
        assertThat(captor.getValue().getOfertas().get(1).getCantidadDisponible()).isEqualTo(3);
        assertThat(captor.getValue().getOfertas().get(2).getPrecioUnitario()).isNull();
    }

    @Test
    @DisplayName("CP-CPC-05: una regla de una fila vuelve al formulario con el error junto al campo y lo que se había cargado")
    void caso_cp_cpc_05() throws Exception {
        // Arrange
        when(cotizacionProveedorServicio.registrarCotizacion(eq(TOKEN), any()))
                .thenThrow(new ReglaNegocioException("La cantidad disponible de Malta Pilsen en Bolsa (25 kg) es obligatoria", "ofertas[0].cantidadDisponible"));
        when(cotizacionProveedorServicio.buscarCotizacion(TOKEN)).thenReturn(cotizacion(null, null, null));

        // Act
        String html = mockMvc.perform(post(URL).with(csrf())
                        .param("ofertas[0].idLinea", "501").param("ofertas[0].idCatalogoProveedor", "100").param("ofertas[0].precioUnitario", "100")
                        .param("ofertas[1].idLinea", "501").param("ofertas[1].idCatalogoProveedor", "101")
                        .param("ofertas[2].idLinea", "502").param("ofertas[2].idCatalogoProveedor", "102"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html)
                .contains("No se pudo enviar la cotización")
                .contains("La cantidad disponible de Malta Pilsen en Bolsa (25 kg) es obligatoria")
                .contains("name=\"ofertas[0].precioUnitario\" value=\"100\"")
                .contains("name=\"ofertas[0].idCatalogoProveedor\" value=\"100\"")
                .containsPattern("id=\"cantidad-0\"[^>]*border-rose-400");
    }

    @Test
    @DisplayName("CP-CPC-06: una cantidad fuera de rango se rechaza en el formulario sin llamar al registro; el precio no lo valida el formulario")
    void caso_cp_cpc_06() throws Exception {
        when(cotizacionProveedorServicio.buscarCotizacion(TOKEN)).thenReturn(cotizacion(null, null, null));

        String html = mockMvc.perform(post(URL).with(csrf())
                        .param("ofertas[0].idLinea", "501").param("ofertas[0].idCatalogoProveedor", "100")
                        .param("ofertas[0].precioUnitario", "0").param("ofertas[0].cantidadDisponible", "1000001")
                        .param("ofertas[1].idLinea", "501").param("ofertas[1].idCatalogoProveedor", "101")
                        .param("ofertas[2].idLinea", "502").param("ofertas[2].idCatalogoProveedor", "102"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // El precio lo valida el service: el formulario solo rechaza la cantidad
        assertThat(html).contains("La cantidad disponible no puede superar 1.000.000").doesNotContain("El precio unitario debe ser mayor a 0");
        verify(cotizacionProveedorServicio, never()).registrarCotizacion(any(), any());
    }

    @Test
    @DisplayName("CP-CPC-07: si el enlace dejó de servir al enviar, se muestra la pantalla que explica el motivo")
    void caso_cp_cpc_07() throws Exception {
        when(cotizacionProveedorServicio.registrarCotizacion(eq(TOKEN), any()))
                .thenThrow(new EnlaceCotizacionInvalidoException("La solicitud de cotización fue cancelada: ya no se reciben respuestas"));

        String html = mockMvc.perform(post(URL).with(csrf()).param("ofertas[0].idLinea", "501").param("ofertas[0].idCatalogoProveedor", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA_INVALIDO))
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("La solicitud de cotización fue cancelada: ya no se reciben respuestas");
    }

    @Test
    @DisplayName("CP-CPC-09: la pantalla responde con Referrer-Policy no-referrer, sin crear la sesión de la aplicación")
    void caso_cp_cpc_09() throws Exception {
        when(cotizacionProveedorServicio.buscarCotizacion(TOKEN)).thenReturn(cotizacion(null, null, null));

        org.springframework.mock.web.MockHttpServletResponse respuesta = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Referrer-Policy", "no-referrer"))
                .andReturn().getResponse();

        assertThat(respuesta.getCookie("JSESSIONID")).isNull();
    }

    @Test
    @DisplayName("CP-CPC-08: el envío sin token CSRF se rechaza aunque la pantalla sea pública")
    void caso_cp_cpc_08() throws Exception {
        mockMvc.perform(post(URL).param("ofertas[0].idLinea", "501")).andExpect(status().isForbidden());

        verify(cotizacionProveedorServicio, never()).registrarCotizacion(any(), any());
    }

    @Test
    @DisplayName("CP-CPC-10: una regla de la fecha estimada de entrega vuelve al formulario con el error junto a la fecha")
    void caso_cp_cpc_10() throws Exception {
        // Arrange
        when(cotizacionProveedorServicio.registrarCotizacion(eq(TOKEN), any()))
                .thenThrow(new ReglaNegocioException("La fecha estimada de entrega es obligatoria", "fechaEntregaEstimada"));
        when(cotizacionProveedorServicio.buscarCotizacion(TOKEN)).thenReturn(cotizacion(null, null, null));

        // Act
        String html = mockMvc.perform(post(URL).with(csrf())
                        .param("ofertas[0].idLinea", "501").param("ofertas[0].idCatalogoProveedor", "100")
                        .param("ofertas[0].precioUnitario", "100").param("ofertas[0].cantidadDisponible", "2")
                        .param("ofertas[1].idLinea", "501").param("ofertas[1].idCatalogoProveedor", "101")
                        .param("ofertas[2].idLinea", "502").param("ofertas[2].idCatalogoProveedor", "102"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("La fecha estimada de entrega es obligatoria")
                .containsPattern("id=\"fechaEntregaEstimada\"[^>]*border-rose-400")
                .contains("name=\"ofertas[0].cantidadDisponible\" value=\"2\"");
    }

    @Test
    @DisplayName("CP-CPC-11: un precio con cantidad 0 vuelve al formulario con el error en el precio de esa fila y conserva lo cargado")
    void caso_cp_cpc_11() throws Exception {
        // Arrange
        when(cotizacionProveedorServicio.registrarCotizacion(eq(TOKEN), any()))
                .thenThrow(new ReglaNegocioException("No indique precio unitario si la cantidad disponible es 0", "ofertas[1].precioUnitario"));
        when(cotizacionProveedorServicio.buscarCotizacion(TOKEN)).thenReturn(cotizacion(null, null, null));

        // Act
        String html = mockMvc.perform(post(URL).with(csrf()).param("fechaEntregaEstimada", "2026-12-05")
                        .param("ofertas[0].idLinea", "501").param("ofertas[0].idCatalogoProveedor", "100")
                        .param("ofertas[0].precioUnitario", "32500").param("ofertas[0].cantidadDisponible", "2")
                        .param("ofertas[1].idLinea", "501").param("ofertas[1].idCatalogoProveedor", "101")
                        .param("ofertas[1].precioUnitario", "6000").param("ofertas[1].cantidadDisponible", "0")
                        .param("ofertas[2].idLinea", "502").param("ofertas[2].idCatalogoProveedor", "102"))
                .andExpect(status().isOk())
                .andExpect(view().name(VISTA))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("No indique precio unitario si la cantidad disponible es 0")
                .containsPattern("id=\"precio-1\"[^>]*border-rose-400")
                .contains("name=\"ofertas[1].precioUnitario\" value=\"6000\"")
                .contains("name=\"ofertas[1].cantidadDisponible\" value=\"0\"")
                .contains("name=\"ofertas[0].precioUnitario\" value=\"32500\"")
                .contains("name=\"fechaEntregaEstimada\" value=\"2026-12-05\"");
    }

    @Test
    @DisplayName("CP-CPC-12: una cantidad negativa se rechaza en el formulario sin llamar al registro")
    void caso_cp_cpc_12() throws Exception {
        when(cotizacionProveedorServicio.buscarCotizacion(TOKEN)).thenReturn(cotizacion(null, null, null));

        String html = mockMvc.perform(post(URL).with(csrf())
                        .param("ofertas[0].idLinea", "501").param("ofertas[0].idCatalogoProveedor", "100").param("ofertas[0].cantidadDisponible", "-1")
                        .param("ofertas[1].idLinea", "501").param("ofertas[1].idCatalogoProveedor", "101")
                        .param("ofertas[2].idLinea", "502").param("ofertas[2].idCatalogoProveedor", "102"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("La cantidad disponible no puede ser negativa");
        verify(cotizacionProveedorServicio, never()).registrarCotizacion(any(), any());
    }

    @Test
    @DisplayName("CP-CPC-13: un precio 0 o negativo con cantidad 0 llega al service, que informa solo el motivo de la combinación")
    void caso_cp_cpc_13() throws Exception {
        // Arrange
        when(cotizacionProveedorServicio.registrarCotizacion(eq(TOKEN), any()))
                .thenThrow(new ReglaNegocioException("No indique precio unitario si la cantidad disponible es 0", "ofertas[0].precioUnitario"));
        when(cotizacionProveedorServicio.buscarCotizacion(TOKEN)).thenReturn(cotizacion(null, null, null));

        for (String precio : List.of("0", "-5")) {
            // Act
            String html = mockMvc.perform(post(URL).with(csrf())
                            .param("ofertas[0].idLinea", "501").param("ofertas[0].idCatalogoProveedor", "100")
                            .param("ofertas[0].precioUnitario", precio).param("ofertas[0].cantidadDisponible", "0")
                            .param("ofertas[1].idLinea", "501").param("ofertas[1].idCatalogoProveedor", "101")
                            .param("ofertas[2].idLinea", "502").param("ofertas[2].idCatalogoProveedor", "102"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Assert
            assertThat(html).contains("No indique precio unitario si la cantidad disponible es 0")
                    .doesNotContain("El precio unitario debe ser mayor a 0")
                    .contains("name=\"ofertas[0].precioUnitario\" value=\"" + precio + "\"");
        }
        verify(cotizacionProveedorServicio, org.mockito.Mockito.times(2)).registrarCotizacion(eq(TOKEN), any());
    }
}
