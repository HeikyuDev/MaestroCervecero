package com.github.heikyudev.maestrocervecero.presentation.controller.cotizacion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoOrdenCompraPropuesta;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoPlanCompra;
import com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoSolicitudCotizacion;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.cotizacion.EnvioInvitacionesCotizacion;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.cotizacion.ISolicitudCotizacionServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.CoberturaInsumoPlanResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.InvitacionCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.ItemOrdenCompraPropuestaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.LineaSolicitudCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.OrdenCompraPropuestaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.PlanCompraResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.ExplicacionSolicitudCotizacionResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cotizacion.SolicitudCotizacionResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(SolicitudCotizacionController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class SolicitudCotizacionControllerTest {

    private static final String URL = "/solicitudes-cotizacion";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ISolicitudCotizacionServicio solicitudCotizacionServicio;

    @MockitoBean
    private EnvioInvitacionesCotizacion envioInvitaciones;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    // ==================== Datos de prueba ====================

    private static SolicitudCotizacionResponseDTO resumen(Long id, EstadoSolicitudCotizacion estado, List<String> avisos) {
        return SolicitudCotizacionResponseDTO.builder().id(id).estado(estado).idPlanificacion(7L).etiquetaPlanificacion("Plan #7 - Pale Ale")
                .fechaSolicitud(LocalDateTime.of(2026, 11, 18, 9, 0)).fechaLimiteRespuesta(LocalDateTime.of(2026, 11, 20, 9, 0))
                .cantidadInsumos(2).cantidadInvitaciones(3).cantidadRespuestas(1).avisos(avisos)
                .tiempoRestante(estado == EstadoSolicitudCotizacion.ABIERTA ? "1 día y 3 horas" : null).build();
    }

    private static OrdenCompraPropuestaResponseDTO orden(Long id, EstadoOrdenCompraPropuesta estado, Long idOrdenCompra) {
        return OrdenCompraPropuestaResponseDTO.builder().id(id).idProveedor(1L).razonSocial("MALTERIA SUR").cuit("30-12345678-9")
                .proveedorActivo(true).estado(estado).idOrdenCompra(idOrdenCompra).total(new BigDecimal("120000"))
                .fechaEntregaEstimada(java.time.LocalDate.of(2026, 12, 5))
                .items(List.of(ItemOrdenCompraPropuestaResponseDTO.builder().id(1L).idCatalogoProveedor(100L).nombreInsumo("Malta Pilsen")
                        .nombrePresentacion("Bolsa (25 kg)").cantidad(4).precioUnitario(new BigDecimal("30000")).subtotal(new BigDecimal("120000"))
                        .cantidadEnUnidadDelInsumo(100.0).unidad("kg").precioPorUnidadDelInsumo(new BigDecimal("1200")).build()))
                .build();
    }

    private static PlanCompraResponseDTO plan(EstadoPlanCompra estado, OrdenCompraPropuestaResponseDTO... ordenes) {
        return PlanCompraResponseDTO.builder().id(701L).estado(estado).costoTotal(new BigDecimal("120000"))
                .cubreTodo(false).ordenes(List.of(ordenes))
                .coberturas(List.of(CoberturaInsumoPlanResponseDTO.builder().idInsumo(1L).nombreInsumo("Malta Pilsen").unidad("kg")
                        .cantidadFaltante(150.0).cantidadCubierta(100.0).cantidadSinCubrir(50.0).build()))
                .build();
    }

    private static SolicitudCotizacionResponseDTO detalle(EstadoSolicitudCotizacion estado, PlanCompraResponseDTO plan) {
        return SolicitudCotizacionResponseDTO.builder().id(90L).estado(estado).idPlanificacion(7L).etiquetaPlanificacion("Plan #7 - Pale Ale")
                .fechaSolicitud(LocalDateTime.of(2026, 11, 18, 9, 0)).fechaLimiteRespuesta(LocalDateTime.of(2026, 11, 20, 9, 0))
                .tiempoRestante(estado == EstadoSolicitudCotizacion.ABIERTA ? "1 día y 3 horas" : null)
                .fechaCancelacion(estado == EstadoSolicitudCotizacion.CANCELADA ? LocalDateTime.of(2026, 11, 19, 8, 0) : null)
                .motivoCancelacion(estado == EstadoSolicitudCotizacion.CANCELADA ? "Se consiguió el insumo" : null)
                .usuarioCancelacion(estado == EstadoSolicitudCotizacion.CANCELADA ? "compras1" : null)
                .avisos(List.of("El plan no cubre todo el faltante: revise la cantidad sin cubrir de cada insumo"))
                .lineas(List.of(LineaSolicitudCotizacionResponseDTO.builder().id(501L).idInsumo(1L).nombreInsumo("Malta Pilsen").tipoInsumo("Malta")
                        .unidad("kg").cantidadRequerida(200.0).cantidadDisponible(30.0).cantidadPendienteRecepcion(20.0).cantidadFaltante(150.0).build()))
                .invitaciones(List.of(
                        InvitacionCotizacionResponseDTO.builder().id(1L).idProveedor(1L).razonSocial("MALTERIA SUR").cuit("30-12345678-9")
                                .proveedorActivo(true).fechaRespuesta(LocalDateTime.of(2026, 11, 19, 10, 15)).cantidadOfertas(1)
                                .estadoEnvio(com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoEnvioInvitacion.ENVIADA).intentosEnvio(1)
                                .estadoParticipacion("Respondió").insumosCotizados(List.of("Malta Pilsen: 4 x Bolsa (25 kg) a $ 30.000,00", "Malta Pilsen: 3 x Bolsa (10 kg) a $ 13.000,00"))
                                .fechaEntregaEstimada(java.time.LocalDate.of(2026, 12, 5)).build(),
                        InvitacionCotizacionResponseDTO.builder().id(2L).idProveedor(2L).razonSocial("LUPULOS DEL VALLE").cuit("30-87654321-0")
                                .proveedorActivo(false).cantidadOfertas(0)
                                .estadoEnvio(com.github.heikyudev.maestrocervecero.persistence.entity.cotizacion.EstadoEnvioInvitacion.FALLIDA).intentosEnvio(3)
                                .estadoParticipacion("Dado de baja").insumosCotizados(List.of()).build()))
                .plan(plan)
                .build();
    }

    // ==================== GET /solicitudes-cotizacion ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-01: el listado muestra las solicitudes con su tiempo restante, el regreso a órdenes de compra y el menú de órdenes de compra resaltado; el orden lo define la consulta")
    void caso_cp_scc_01() throws Exception {
        // Arrange
        when(solicitudCotizacionServicio.filtrarSolicitudes(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resumen(91L, EstadoSolicitudCotizacion.ABIERTA, List.of()),
                        resumen(90L, EstadoSolicitudCotizacion.PLAN_GENERADO, List.of("Ningún proveedor activo ofrece Cascade"))), PageRequest.of(0, 20), 2));

        // Act
        String html = mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("cotizacion/solicitudes-lista"))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html)
                .contains("Solicitudes de cotización").contains("Volver a órdenes de compra").contains("href=\"/ordenes-compra\"")
                .doesNotContain("data-aviso-planes-pendientes")
                .contains("Tiempo restante").contains("1 día y 3 horas")
                .contains("href=\"/solicitudes-cotizacion/90\"").contains("data-boton-abrir").contains("aria-label=\"Abrir solicitud 90\"").doesNotContain("bi-eye")
                .contains("Plan generado").contains("1 de 3").contains("18/11/2026 09:00").contains("20/11/2026 09:00")
                .contains("data-tooltip=\"Ningún proveedor activo ofrece Cascade\"")
                .containsPattern("href=\"/ordenes-compra\"[^>]*bg-amber-500/10");
        assertThat(html.indexOf("href=\"/solicitudes-cotizacion/91\"")).isLessThan(html.indexOf("href=\"/solicitudes-cotizacion/90\""));
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(solicitudCotizacionServicio).filtrarSolicitudes(isNull(), isNull(), pagina.capture());
        assertThat(pagina.getValue().getSort().isUnsorted()).isTrue();
        assertThat(pagina.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-02: el filtro de estado se aplica sin distinguir mayúsculas y un estado inexistente se ignora; sin solicitudes se muestra el estado vacío sin aviso")
    void caso_cp_scc_02() throws Exception {
        // Arrange
        when(solicitudCotizacionServicio.filtrarSolicitudes(any(), isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        // Act
        String html = mockMvc.perform(get(URL).param("estado", "abierta")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mockMvc.perform(get(URL).param("estado", "INEXISTENTE")).andExpect(status().isOk());

        // Assert
        verify(solicitudCotizacionServicio).filtrarSolicitudes(eq(EstadoSolicitudCotizacion.ABIERTA), isNull(), any(Pageable.class));
        verify(solicitudCotizacionServicio).filtrarSolicitudes(isNull(), isNull(), any(Pageable.class));
        assertThat(html)
                .contains("No se encontraron solicitudes de cotización con los criterios indicados")
                .contains("<option value=\"ABIERTA\" selected=\"selected\">Abierta</option>")
                .doesNotContain("data-aviso-planes-pendientes");
    }

    // ==================== GET /solicitudes-cotizacion/{id} ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-03: con el plan generado y sin pestaña pedida, el detalle abre en la pestaña del plan: el encabezado sin cancelar, los avisos y las tres pestañas arriba, el plan con la fecha de entrega del proveedor y confirmar y descartar en cada orden pendiente, y sin el contenido de las otras pestañas")
    void caso_cp_scc_03() throws Exception {
        // Arrange
        when(solicitudCotizacionServicio.buscarPorId(90L)).thenReturn(detalle(EstadoSolicitudCotizacion.PLAN_GENERADO,
                plan(EstadoPlanCompra.PROPUESTO, orden(801L, EstadoOrdenCompraPropuesta.PROPUESTA, null))));

        // Act
        String html = mockMvc.perform(get(URL + "/90"))
                .andExpect(status().isOk())
                .andExpect(view().name("cotizacion/solicitud-detalle"))
                .andExpect(model().attribute("pestanaActiva", PestanaSolicitudCotizacion.PLAN))
                .andExpect(model().attribute("pestanas", List.of(PestanaSolicitudCotizacion.SOLICITUD,
                        PestanaSolicitudCotizacion.PROVEEDORES, PestanaSolicitudCotizacion.PLAN)))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html)
                .contains("Solicitud de cotización #90")
                .contains("El plan no cubre todo el faltante: revise la cantidad sin cubrir de cada insumo")
                .doesNotContain("data-boton-cancelar")
                .contains("data-pestanas-solicitud")
                .contains("href=\"/solicitudes-cotizacion/90?pestana=solicitud\"").contains("href=\"/solicitudes-cotizacion/90?pestana=proveedores\"")
                .contains("href=\"/solicitudes-cotizacion/90?pestana=plan\"")
                .containsPattern("data-pestana-plan=\"true\"[^>]*aria-current=\"page\"").contains("Plan de compra")
                .contains("data-plan>").contains("Plan de órdenes de compra").contains("Resumen del plan").contains("Propuesto")
                .contains("Confirme o descarte cada orden propuesta. Cuando no quedan órdenes pendientes, la solicitud queda finalizada")
                .doesNotContain("elige el plan")
                .doesNotContain("?plan=").doesNotContain("Plan 1").doesNotContain("Planes de órdenes de compra")
                .contains("Cobertura de los insumos").contains("50 kg")
                .contains("Bolsa (25 kg)").contains("$ 1.200,00 / kg").contains("$ 120.000,00")
                .containsPattern("data-fecha-entrega-propuesta>\\s*Fecha de entrega estimada indicada por el proveedor: <strong[^>]*>05/12/2026</strong>")
                .contains("href=\"/ordenes-compra/nuevo?idOrdenPropuesta=801\"")
                .contains("action=\"/solicitudes-cotizacion/90/ordenes-propuestas/801/descartar\"")
                .doesNotContain("Datos de la solicitud").doesNotContain("Insumos solicitados").doesNotContain("Proveedores participantes");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-04: en el plan propuesto con órdenes ya resueltas, la pendiente ofrece confirmar y descartar, la confirmada enlaza a su orden de compra, la descartada no ofrece acciones y la solicitud no se puede cancelar")
    void caso_cp_scc_04() throws Exception {
        // Arrange
        when(solicitudCotizacionServicio.buscarPorId(90L)).thenReturn(detalle(EstadoSolicitudCotizacion.PLAN_GENERADO,
                plan(EstadoPlanCompra.PROPUESTO, orden(801L, EstadoOrdenCompraPropuesta.DESCARTADA, null),
                        orden(802L, EstadoOrdenCompraPropuesta.CONFIRMADA, 12L), orden(803L, EstadoOrdenCompraPropuesta.PROPUESTA, null))));

        // Act
        String html = mockMvc.perform(get(URL + "/90")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html)
                .contains("Propuesto").contains("Confirmada").doesNotContain("Elegido")
                .contains("href=\"/ordenes-compra/12\"").contains("Ver orden de compra #12")
                .contains("href=\"/ordenes-compra/nuevo?idOrdenPropuesta=803\"")
                .contains("action=\"/solicitudes-cotizacion/90/ordenes-propuestas/803/descartar\"")
                .contains("data-confirmar-titulo=\"Descartar orden propuesta\"")
                .doesNotContain("idOrdenPropuesta=801").doesNotContain("idOrdenPropuesta=802")
                .doesNotContain("ordenes-propuestas/801/descartar").doesNotContain("ordenes-propuestas/802/descartar")
                .doesNotContain("data-boton-cancelar");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-05: una solicitud cancelada abre en su plan cancelado sin acciones y, en la pestaña de la solicitud, muestra el detalle de su cancelación en un diálogo; una abierta abre en la solicitud con el tiempo restante y ofrece cancelar, sin pestaña ni sección del plan")
    void caso_cp_scc_05() throws Exception {
        // Arrange
        when(solicitudCotizacionServicio.buscarPorId(90L)).thenReturn(detalle(EstadoSolicitudCotizacion.CANCELADA,
                plan(EstadoPlanCompra.CANCELADO, orden(801L, EstadoOrdenCompraPropuesta.DESCARTADA, null))));
        when(solicitudCotizacionServicio.buscarPorId(91L)).thenReturn(detalle(EstadoSolicitudCotizacion.ABIERTA, null));

        // Act
        String cancelada = mockMvc.perform(get(URL + "/90")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String canceladaSolicitud = mockMvc.perform(get(URL + "/90").param("pestana", "solicitud")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String abierta = mockMvc.perform(get(URL + "/91")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(cancelada).contains("data-plan>").contains("Cancelado").doesNotContain("idOrdenPropuesta=").doesNotContain("/descartar")
                .doesNotContain("data-boton-cancelar");
        assertThat(canceladaSolicitud).doesNotContain("data-plan>").doesNotContain("data-boton-cancelar")
                .contains("data-detalle-motivo=\"Se consiguió el insumo\"").contains("data-detalle-usuario=\"compras1\"")
                .contains("Detalle de la cancelación").doesNotContain(">Se consiguió el insumo<");
        assertThat(abierta).doesNotContain("data-plan>").doesNotContain("Cobertura de los insumos").doesNotContain("data-pestana-plan")
                .contains("data-tiempo-restante").contains("1 día y 3 horas").contains("data-boton-cancelar");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-06: una solicitud inexistente vuelve al origen con el motivo")
    void caso_cp_scc_06() throws Exception {
        when(solicitudCotizacionServicio.buscarPorId(99L)).thenThrow(new RecursoNoEncontradoException("No se encontró la solicitud de cotización con ID: 99"));

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se encontró la solicitud de cotización con ID: 99"));
    }

    // ==================== POST /solicitudes-cotizacion/{id}/ordenes-propuestas/{idOrdenPropuesta}/descartar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-07: descartar una orden propuesta delega en el service y vuelve a la pestaña del plan de la solicitud con alerta de éxito")
    void caso_cp_scc_07() throws Exception {
        mockMvc.perform(post(URL + "/90/ordenes-propuestas/803/descartar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitudes-cotizacion/90?pestana=plan"))
                .andExpect(flash().attribute("mensaje", "Orden propuesta descartada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(solicitudCotizacionServicio).descartarOrdenPropuesta(90L, 803L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-08: si el service rechaza el descarte, vuelve al origen con el motivo")
    void caso_cp_scc_08() throws Exception {
        when(solicitudCotizacionServicio.descartarOrdenPropuesta(90L, 803L)).thenThrow(new ReglaNegocioException("No se puede descartar la orden propuesta porque su plan está finalizado"));

        mockMvc.perform(post(URL + "/90/ordenes-propuestas/803/descartar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se puede descartar la orden propuesta porque su plan está finalizado"));
    }

    // ==================== Seguridad ====================

    @ParameterizedTest
    @ValueSource(strings = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "ADMINISTRADOR", "OPERARIO_DE_PRODUCCION", "ENCARGADO_DE_DISTRIBUCION"})
    @DisplayName("CP-SCC-09: ningún otro rol accede a las solicitudes de cotización, ni las cancela ni descarta órdenes propuestas")
    void caso_cp_scc_09(String rol) throws Exception {
        for (String ruta : List.of(URL, URL + "/90")) {
            mockMvc.perform(get(ruta).with(user("usuario").roles(rol)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"));
        }
        mockMvc.perform(post(URL + "/90/ordenes-propuestas/803/descartar").with(user("usuario").roles(rol)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL + "/90/cancelar").with(user("usuario").roles(rol)).with(csrf()).param("motivo", "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(post(URL + "/90/confirmar-envio").with(user("usuario").roles(rol)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verifyNoInteractions(solicitudCotizacionServicio, envioInvitaciones);
    }

    @Test
    @DisplayName("CP-SCC-10: un usuario sin sesión es enviado al inicio de sesión")
    void caso_cp_scc_10() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        verify(solicitudCotizacionServicio, never()).buscarPorId(any());
    }

    // ==================== POST /solicitudes-cotizacion/{id}/cancelar ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-11: cancelar delega el motivo en el service y vuelve al detalle con alerta de éxito")
    void caso_cp_scc_11() throws Exception {
        mockMvc.perform(post(URL + "/90/cancelar").with(csrf()).param("motivo", "Se consiguió el insumo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitudes-cotizacion/90"))
                .andExpect(flash().attribute("mensaje", "Solicitud de cotización cancelada correctamente"))
                .andExpect(flash().attribute("tipo", "success"));

        verify(solicitudCotizacionServicio).cancelarSolicitud(90L, "Se consiguió el insumo");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-12: si el service rechaza la cancelación (sin motivo o con el plan generado), vuelve al origen con el motivo")
    void caso_cp_scc_12() throws Exception {
        when(solicitudCotizacionServicio.cancelarSolicitud(90L, null)).thenThrow(new ReglaNegocioException("El motivo de cancelación es obligatorio"));

        mockMvc.perform(post(URL + "/90/cancelar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "El motivo de cancelación es obligatorio"));
    }

    // ==================== Confirmación del envío ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-13: una solicitud pendiente de confirmación muestra la explicación y ofrece confirmar el envío (con diálogo) y cancelar")
    void caso_cp_scc_13() throws Exception {
        // Arrange
        SolicitudCotizacionResponseDTO pendiente = SolicitudCotizacionResponseDTO.builder().id(12L).estado(EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION)
                .idPlanificacion(45L).etiquetaPlanificacion("Plan #45 - Pale Ale").avisos(List.of())
                .explicacion(ExplicacionSolicitudCotizacionResponseDTO.builder()
                        .introduccion("Se generó la solicitud de cotización N° 12 para la planificación N° 45. El stock actual no cubre:")
                        .insumosNoCubiertos(List.of("Malta Pilsen: faltan 120 kg", "Lúpulo Cascade: faltan 2,5 kg"))
                        .accion("Confirme el envío a los proveedores o cancele la solicitud").build())
                .lineas(List.of()).invitaciones(List.of()).build();
        when(solicitudCotizacionServicio.buscarPorId(12L)).thenReturn(pendiente);

        // Act
        String html = mockMvc.perform(get(URL + "/12")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html)
                .contains("data-explicacion-solicitud").contains("El stock actual no cubre:")
                .containsPattern("<ul[^>]*data-insumos-no-cubiertos[^>]*>\\s*<li[^>]*>Malta Pilsen: faltan 120 kg</li>\\s*<li[^>]*>Lúpulo Cascade: faltan 2,5 kg</li>")
                .contains("Confirme el envío a los proveedores o cancele la solicitud")
                .contains("Pendiente de confirmación")
                .contains("action=\"/solicitudes-cotizacion/12/confirmar-envio\"").contains("data-confirmar-titulo=\"Confirmar envío\"")
                .contains("data-boton-cancelar").doesNotContain("data-tiempo-restante");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-14: confirmar el envío abre la solicitud, envía las invitaciones después de confirmarla y vuelve al detalle con alerta de éxito")
    void caso_cp_scc_14() throws Exception {
        mockMvc.perform(post(URL + "/12/confirmar-envio").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/solicitudes-cotizacion/12"))
                .andExpect(flash().attribute("mensaje", "Envío a los proveedores confirmado correctamente"));

        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(solicitudCotizacionServicio, envioInvitaciones);
        orden.verify(solicitudCotizacionServicio).confirmarEnvio(12L);
        orden.verify(envioInvitaciones).enviarPendientes(12L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-15: si el service rechaza la confirmación, vuelve al origen con el motivo y no envía nada")
    void caso_cp_scc_15() throws Exception {
        when(solicitudCotizacionServicio.confirmarEnvio(12L)).thenThrow(new ReglaNegocioException("No se puede confirmar el envío porque la planificación de producción ya no está Pendiente"));

        mockMvc.perform(post(URL + "/12/confirmar-envio").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se puede confirmar el envío porque la planificación de producción ya no está Pendiente"));

        verify(envioInvitaciones, never()).enviarPendientes(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-16: descartar pasa al service la solicitud de la URL; una orden propuesta de otra solicitud vuelve con el motivo")
    void caso_cp_scc_16() throws Exception {
        when(solicitudCotizacionServicio.descartarOrdenPropuesta(91L, 803L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la orden propuesta con ID 803 en la solicitud de cotización #91"));

        mockMvc.perform(post(URL + "/91/ordenes-propuestas/803/descartar").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "No se encontró la orden propuesta con ID 803 en la solicitud de cotización #91"))
                .andExpect(flash().attribute("tipo", "danger"));

        verify(solicitudCotizacionServicio).descartarOrdenPropuesta(91L, 803L);
    }

    // ==================== Pestañas del detalle ====================

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-17: la pestaña de la solicitud muestra los datos con el enlace a la planificación y los insumos solicitados, sin los proveedores ni el plan")
    void caso_cp_scc_17() throws Exception {
        // Arrange
        when(solicitudCotizacionServicio.buscarPorId(90L)).thenReturn(detalle(EstadoSolicitudCotizacion.PLAN_GENERADO,
                plan(EstadoPlanCompra.PROPUESTO, orden(801L, EstadoOrdenCompraPropuesta.PROPUESTA, null))));

        // Act
        String html = mockMvc.perform(get(URL + "/90").param("pestana", "SOLICITUD"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pestanaActiva", PestanaSolicitudCotizacion.SOLICITUD))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html)
                .containsPattern("data-pestana-solicitud=\"true\"[^>]*aria-current=\"page\"")
                .contains("Datos de la solicitud").contains("Plan #7 - Pale Ale").contains("href=\"/planificaciones-produccion/7\"")
                .contains("Insumos solicitados").contains("Malta Pilsen").contains("200 kg").contains("30 kg").contains("20 kg").contains("150 kg")
                .contains("El plan no cubre todo el faltante: revise la cantidad sin cubrir de cada insumo")
                .doesNotContain("Proveedores participantes").doesNotContain("data-plan>").doesNotContain("Cobertura de los insumos");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-18: la pestaña de proveedores muestra la situación de cada proveedor con su fecha estimada de entrega y cada presentación que cotizó; sin invitaciones muestra que no hay proveedores participantes")
    void caso_cp_scc_18() throws Exception {
        // Arrange
        when(solicitudCotizacionServicio.buscarPorId(90L)).thenReturn(detalle(EstadoSolicitudCotizacion.PLAN_GENERADO,
                plan(EstadoPlanCompra.PROPUESTO, orden(801L, EstadoOrdenCompraPropuesta.PROPUESTA, null))));
        SolicitudCotizacionResponseDTO sinInvitaciones = SolicitudCotizacionResponseDTO.builder().id(12L).estado(EstadoSolicitudCotizacion.PENDIENTE_DE_CONFIRMACION)
                .idPlanificacion(45L).etiquetaPlanificacion("Plan #45 - Pale Ale").avisos(List.of()).lineas(List.of()).invitaciones(List.of()).build();
        when(solicitudCotizacionServicio.buscarPorId(12L)).thenReturn(sinInvitaciones);

        // Act
        String html = mockMvc.perform(get(URL + "/90").param("pestana", "proveedores"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pestanaActiva", PestanaSolicitudCotizacion.PROVEEDORES))
                .andReturn().getResponse().getContentAsString();
        String vacio = mockMvc.perform(get(URL + "/12").param("pestana", "proveedores"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pestanaActiva", PestanaSolicitudCotizacion.PROVEEDORES))
                .andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html)
                .containsPattern("data-pestana-proveedores=\"true\"[^>]*aria-current=\"page\"")
                .contains("Proveedores participantes").contains("MALTERIA SUR").contains("19/11/2026 10:15")
                .contains("Respondió").contains("Dado de baja").contains("No entregada (3 intentos)")
                .containsPattern("data-insumos-cotizados>\\s*<p[^>]*>Malta Pilsen: 4 x Bolsa \\(25 kg\\) a \\$ 30\\.000,00</p>\\s*<p[^>]*>Malta Pilsen: 3 x Bolsa \\(10 kg\\) a \\$ 13\\.000,00</p>")
                .contains("Entrega estimada").containsPattern("data-fecha-entrega>\\s*05/12/2026")
                .doesNotContain("Insumos solicitados").doesNotContain("data-plan>").doesNotContain("data-sin-proveedores");
        assertThat(vacio).contains("Proveedores participantes").contains("data-sin-proveedores").contains("No hay proveedores participantes")
                .doesNotContain("data-estado-participacion");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-19: sin plan el detalle abre en la solicitud y no ofrece la pestaña del plan aunque se la pida; con plan, una pestaña inexistente abre en el plan")
    void caso_cp_scc_19() throws Exception {
        // Arrange
        when(solicitudCotizacionServicio.buscarPorId(91L)).thenReturn(detalle(EstadoSolicitudCotizacion.ABIERTA, null));
        when(solicitudCotizacionServicio.buscarPorId(90L)).thenReturn(detalle(EstadoSolicitudCotizacion.PLAN_GENERADO,
                plan(EstadoPlanCompra.PROPUESTO, orden(801L, EstadoOrdenCompraPropuesta.PROPUESTA, null))));
        List<PestanaSolicitudCotizacion> sinPlan = List.of(PestanaSolicitudCotizacion.SOLICITUD, PestanaSolicitudCotizacion.PROVEEDORES);

        // Act
        String porDefecto = mockMvc.perform(get(URL + "/91"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pestanaActiva", PestanaSolicitudCotizacion.SOLICITUD))
                .andExpect(model().attribute("pestanas", sinPlan))
                .andReturn().getResponse().getContentAsString();
        String planPedido = mockMvc.perform(get(URL + "/91").param("pestana", "plan"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pestanaActiva", PestanaSolicitudCotizacion.SOLICITUD))
                .andExpect(model().attribute("pestanas", sinPlan))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get(URL + "/90").param("pestana", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pestanaActiva", PestanaSolicitudCotizacion.PLAN));

        // Assert
        for (String html : List.of(porDefecto, planPedido)) {
            assertThat(html).contains("Datos de la solicitud").contains("Insumos solicitados")
                    .contains("href=\"/solicitudes-cotizacion/90?pestana=proveedores\"")
                    .doesNotContain("data-pestana-plan").doesNotContain("pestana=plan").doesNotContain("data-plan>");
        }
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-SCC-20: una solicitud finalizada abre en su plan finalizado sin acciones sobre las órdenes ni cancelar; el listado ofrece filtrar por Finalizada")
    void caso_cp_scc_20() throws Exception {
        // Arrange
        when(solicitudCotizacionServicio.buscarPorId(90L)).thenReturn(detalle(EstadoSolicitudCotizacion.FINALIZADA,
                plan(EstadoPlanCompra.FINALIZADO, orden(801L, EstadoOrdenCompraPropuesta.CONFIRMADA, 12L),
                        orden(802L, EstadoOrdenCompraPropuesta.DESCARTADA, null))));
        when(solicitudCotizacionServicio.filtrarSolicitudes(any(), isNull(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        // Act
        String html = mockMvc.perform(get(URL + "/90"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pestanaActiva", PestanaSolicitudCotizacion.PLAN))
                .andReturn().getResponse().getContentAsString();
        String lista = mockMvc.perform(get(URL).param("estado", "finalizada")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Assert
        assertThat(html).contains("data-plan>").contains("Finalizado").contains("href=\"/ordenes-compra/12\"")
                .doesNotContain("idOrdenPropuesta=").doesNotContain("/descartar").doesNotContain("data-boton-cancelar");
        assertThat(lista).contains("<option value=\"FINALIZADA\" selected=\"selected\">Finalizada</option>");
        verify(solicitudCotizacionServicio).filtrarSolicitudes(eq(EstadoSolicitudCotizacion.FINALIZADA), isNull(), any(Pageable.class));
    }
}
