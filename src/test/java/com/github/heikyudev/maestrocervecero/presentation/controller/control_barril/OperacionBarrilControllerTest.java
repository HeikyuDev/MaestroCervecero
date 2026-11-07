package com.github.heikyudev.maestrocervecero.presentation.controller.control_barril;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.TipoOperacionBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionLimpiezaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DespachoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.FraccionamientoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.LimpiezaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IControlBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IDespachoBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IDevolucionBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFallaBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFraccionamientoBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.ILimpiezaBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.barril.IMantenimientoBarrilServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.cliente.IClienteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.OperacionBarrilResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.github.heikyudev.maestrocervecero.presentation.controller.control_barril.ControlBarrilControllerTest.como;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({OperacionBarrilController.class, ControlBarrilController.class})
@Import({SecurityConfig.class, ControllerAdvices.class})
class OperacionBarrilControllerTest {

    private static final String BASE = "/control-barriles/5";
    private static final List<String> RUTAS_OPERARIO = List.of("limpiezas", "fallas", "fraccionamientos", "mantenimientos");
    private static final List<String> RUTAS_ENCARGADO = List.of("despachos", "devoluciones");

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ILimpiezaBarrilServicio limpiezaServicio;
    @MockitoBean
    private IFallaBarrilServicio fallaServicio;
    @MockitoBean
    private IFraccionamientoBarrilServicio fraccionamientoServicio;
    @MockitoBean
    private IMantenimientoBarrilServicio mantenimientoServicio;
    @MockitoBean
    private IDespachoBarrilServicio despachoServicio;
    @MockitoBean
    private IDevolucionBarrilServicio devolucionServicio;
    @MockitoBean
    private IControlBarrilServicio controlServicio;
    @MockitoBean
    private IClienteServicio clienteServicio;
    @MockitoBean
    private com.github.heikyudev.maestrocervecero.service.interfaces.barril.IFabricanteBarrilServicio fabricanteServicio;
    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @BeforeEach
    void escenarioBase() {
        configurarBarril(EstadoOperativoBarril.EN_LIMPIEZA);
        for (TipoOperacionBarril tipo : TipoOperacionBarril.values()) {
            when(controlServicio.listarOperaciones(5L, tipo)).thenReturn(List.of(
                    OperacionBarrilResponseDTO.builder().id(7L).tipo(tipo).estado(EstadoTransaccion.REGISTRADO).anulable(true).build()));
        }
        when(clienteServicio.buscarPorId(3L)).thenReturn(ClienteResponseDTO.builder().id(3L).nombre("Bar El Puerto").build());
    }

    private void configurarBarril(EstadoOperativoBarril estado) {
        when(controlServicio.buscarResumen(5L)).thenReturn(BarrilResumenResponseDTO.builder().id(5L).identificadorInterno("BRL-01").numeroSerie("SN-001")
                .fabricante("Barrilitos SA").capacidad(50.0).contenidoActual(20.0).estadoOperativo(estado).usosMaximosAntesMantenimiento(10).usosDesdeUltimoMantenimiento(3L).build());
    }

    private MockHttpServletRequestBuilder registro(String ruta, Rol rol, String fecha, String obs) {
        return post(BASE + "/" + ruta).with(como(rol)).with(csrf()).param("fecha", fecha).param("observaciones", obs);
    }

    @Test
    @DisplayName("CP-OBC-01: el operario registra una limpieza: el service recibe el barril de la ruta, la fecha y las observaciones, y vuelve a la pestaña con la alerta de éxito")
    void registrarLimpieza() throws Exception {
        mockMvc.perform(registro("limpiezas", Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T10:00", "Limpieza con soda"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE + "?pestana=limpiezas"))
                .andExpect(flash().attribute("mensaje", "Limpieza registrada correctamente")).andExpect(flash().attribute("tipo", "success"));

        ArgumentCaptor<LimpiezaBarrilFormDTO> form = ArgumentCaptor.forClass(LimpiezaBarrilFormDTO.class);
        verify(limpiezaServicio).registrarLimpiezaBarril(form.capture());
        assertThat(form.getValue().getIdBarril()).isEqualTo(5L);
        assertThat(form.getValue().getFecha()).isEqualTo(LocalDateTime.of(2026, 10, 20, 10, 0));
        assertThat(form.getValue().getObservaciones()).isEqualTo("Limpieza con soda");
    }

    @Test
    @DisplayName("CP-OBC-02: el operario registra una falla, un fraccionamiento (con la cantidad) y un mantenimiento, y vuelve a la pestaña de cada uno")
    void registrarOtrasDelOperario() throws Exception {
        mockMvc.perform(registro("fallas", Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T10:00", "Pérdida en la válvula"))
                .andExpect(redirectedUrl(BASE + "?pestana=fallas")).andExpect(flash().attribute("mensaje", "Falla registrada correctamente"));
        mockMvc.perform(registro("fraccionamientos", Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T10:00", "Muestra").param("cantidadExtraida", "5.5"))
                .andExpect(redirectedUrl(BASE + "?pestana=fraccionamientos")).andExpect(flash().attribute("mensaje", "Fraccionamiento registrado correctamente"));
        mockMvc.perform(registro("mantenimientos", Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T10:00", "Cambio de juntas"))
                .andExpect(redirectedUrl(BASE + "?pestana=mantenimientos")).andExpect(flash().attribute("mensaje", "Mantenimiento registrado correctamente"));

        ArgumentCaptor<FraccionamientoBarrilFormDTO> form = ArgumentCaptor.forClass(FraccionamientoBarrilFormDTO.class);
        verify(fraccionamientoServicio).registrarFraccionamientoBarril(form.capture());
        assertThat(form.getValue().getIdBarril()).isEqualTo(5L);
        assertThat(form.getValue().getCantidadExtraida()).isEqualTo(5.5);
        verify(fallaServicio).registrarFallaBarril(any());
        verify(mantenimientoServicio).registrarMantenimientoBarril(any());
    }

    @Test
    @DisplayName("CP-OBC-03: el encargado registra un despacho (cliente y fecha estimada) y una devolución, y vuelve a la pestaña de cada uno")
    void registrarDelEncargado() throws Exception {
        mockMvc.perform(registro("despachos", Rol.ENCARGADO_DE_DISTRIBUCION, "2026-10-20T10:00", "Entrega").param("idCliente", "3").param("fechaDevolucionEstimada", "2026-11-30"))
                .andExpect(redirectedUrl(BASE + "?pestana=despachos")).andExpect(flash().attribute("mensaje", "Despacho registrado correctamente"));
        mockMvc.perform(registro("devoluciones", Rol.ENCARGADO_DE_DISTRIBUCION, "2026-10-21T10:00", "Devuelto"))
                .andExpect(redirectedUrl(BASE + "?pestana=devoluciones")).andExpect(flash().attribute("mensaje", "Devolución registrada correctamente"));

        ArgumentCaptor<DespachoBarrilFormDTO> form = ArgumentCaptor.forClass(DespachoBarrilFormDTO.class);
        verify(despachoServicio).registrarDespachoBarril(form.capture());
        assertThat(form.getValue().getIdBarril()).isEqualTo(5L);
        assertThat(form.getValue().getIdCliente()).isEqualTo(3L);
        assertThat(form.getValue().getFechaDevolucionEstimada()).isEqualTo(LocalDate.of(2026, 11, 30));
        verify(devolucionServicio).registrarDevolucionBarril(any());
    }

    @Test
    @DisplayName("CP-OBC-04: un formulario inválido vuelve al formulario por redirect sin llamar al service, y el GET siguiente muestra los errores y lo escrito")
    void registrarInvalido() throws Exception {
        MvcResult resultado = mockMvc.perform(registro("limpiezas", Rol.OPERARIO_DE_PRODUCCION, "", "  "))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE + "/limpiezas/nueva")).andReturn();
        verify(limpiezaServicio, never()).registrarLimpiezaBarril(any());

        String html = mockMvc.perform(get(BASE + "/limpiezas/nueva").with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(resultado.getFlashMap()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("No se pudo registrar la limpieza").contains("La fecha de limpieza es obligatoria").contains("Las observaciones son obligatorias");
    }

    @Test
    @DisplayName("CP-OBC-05: un rechazo del service sobre un campo se muestra en ese campo, y uno general (estado del barril) en el resumen")
    void registrarRechazadoPorElService() throws Exception {
        when(fraccionamientoServicio.registrarFraccionamientoBarril(any()))
                .thenThrow(new ReglaNegocioException("La cantidad a extraer no puede superar el contenido actual del barril", "cantidadExtraida"));
        MvcResult campo = mockMvc.perform(registro("fraccionamientos", Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T12:00", "Muestra").param("cantidadExtraida", "99"))
                .andExpect(redirectedUrl(BASE + "/fraccionamientos/nuevo")).andReturn();
        when(limpiezaServicio.registrarLimpiezaBarril(any()))
                .thenThrow(new ReglaNegocioException("Solo se puede registrar una limpieza sobre un barril en estado operativo En limpieza", "idBarril"));
        MvcResult general = mockMvc.perform(registro("limpiezas", Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T10:00", "Limpieza"))
                .andExpect(redirectedUrl(BASE + "/limpiezas/nueva")).andReturn();

        configurarBarril(EstadoOperativoBarril.CON_CERVEZA);
        String htmlCampo = mockMvc.perform(get(BASE + "/fraccionamientos/nuevo").with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(campo.getFlashMap()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        configurarBarril(EstadoOperativoBarril.EN_LIMPIEZA);
        String htmlGeneral = mockMvc.perform(get(BASE + "/limpiezas/nueva").with(como(Rol.OPERARIO_DE_PRODUCCION)).flashAttrs(general.getFlashMap()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(htmlCampo).contains("La cantidad a extraer no puede superar el contenido actual del barril").contains("value=\"99\"");
        assertThat(htmlGeneral).contains("Solo se puede registrar una limpieza sobre un barril en estado operativo En limpieza");
    }

    @Test
    @DisplayName("CP-OBC-06: el rechazo de un despacho vuelve al formulario con el cliente elegido y lo escrito")
    void despachoRechazadoConservaElCliente() throws Exception {
        configurarBarril(EstadoOperativoBarril.CON_CERVEZA);
        when(despachoServicio.registrarDespachoBarril(any()))
                .thenThrow(new ReglaNegocioException("La fecha estimada de devolución debe ser posterior o igual a la fecha de despacho", "fechaDevolucionEstimada"));
        MvcResult resultado = mockMvc.perform(registro("despachos", Rol.ENCARGADO_DE_DISTRIBUCION, "2026-10-20T10:00", "Entrega").param("idCliente", "3").param("fechaDevolucionEstimada", "2026-10-01"))
                .andExpect(redirectedUrl(BASE + "/despachos/nuevo")).andReturn();

        String html = mockMvc.perform(get(BASE + "/despachos/nuevo").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).flashAttrs(resultado.getFlashMap()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("La fecha estimada de devolución debe ser posterior o igual a la fecha de despacho").contains("value=\"Bar El Puerto\"")
                .contains("value=\"3\"").contains("value=\"2026-10-01\"");
    }

    @Test
    @DisplayName("CP-OBC-07: cada rol anula sus operaciones con motivo: el service recibe el ID y el motivo y vuelve a la pestaña con la alerta de éxito")
    void anular() throws Exception {
        String[][] casos = {{"limpiezas", "Limpieza anulada correctamente"}, {"fallas", "Falla anulada correctamente"}, {"fraccionamientos", "Fraccionamiento anulado correctamente"},
                {"mantenimientos", "Mantenimiento anulado correctamente"}};
        for (String[] caso : casos) {
            mockMvc.perform(post(BASE + "/" + caso[0] + "/7/anular").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).param("motivo", "Error de carga"))
                    .andExpect(redirectedUrl(BASE + "?pestana=" + caso[0])).andExpect(flash().attribute("mensaje", caso[1]));
        }
        mockMvc.perform(post(BASE + "/despachos/7/anular").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).with(csrf()).param("motivo", "Error de carga"))
                .andExpect(redirectedUrl(BASE + "?pestana=despachos")).andExpect(flash().attribute("mensaje", "Despacho anulado correctamente"));
        mockMvc.perform(post(BASE + "/devoluciones/7/anular").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).with(csrf()).param("motivo", "Error de carga"))
                .andExpect(redirectedUrl(BASE + "?pestana=devoluciones")).andExpect(flash().attribute("mensaje", "Devolución anulada correctamente"));

        ArgumentCaptor<AnulacionLimpiezaBarrilFormDTO> form = ArgumentCaptor.forClass(AnulacionLimpiezaBarrilFormDTO.class);
        verify(limpiezaServicio).anularLimpiezaBarril(eq(7L), form.capture());
        assertThat(form.getValue().getMotivoAnulacion()).isEqualTo("Error de carga");
        verify(fallaServicio).anularFallaBarril(eq(7L), any());
        verify(fraccionamientoServicio).anularFraccionamientoBarril(eq(7L), any());
        verify(mantenimientoServicio).anularMantenimientoBarril(eq(7L), any());
        verify(despachoServicio).anularDespachoBarril(eq(7L), any());
        verify(devolucionServicio).anularDevolucionBarril(eq(7L), any());
    }

    @Test
    @DisplayName("CP-OBC-08: si el service rechaza la anulación vuelve al origen con su mensaje en una alerta warning")
    void anularRechazada() throws Exception {
        when(limpiezaServicio.anularLimpiezaBarril(any(), any()))
                .thenThrow(new ReglaNegocioException("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores"));

        mockMvc.perform(post(BASE + "/limpiezas/7/anular").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).header("Referer", BASE).param("motivo", "x"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "warning"))
                .andExpect(flash().attribute("mensaje", "Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores"));
    }

    @Test
    @DisplayName("CP-OBC-09: anular una operación que no es del barril de la ruta responde como inexistente (alerta danger) y no llama a ningún service de anulación")
    void anularOperacionDeOtroBarril() throws Exception {
        for (String ruta : RUTAS_OPERARIO) {
            mockMvc.perform(post(BASE + "/" + ruta + "/99/anular").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).header("Referer", BASE).param("motivo", "Error de carga"))
                    .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        }
        for (String ruta : RUTAS_ENCARGADO) {
            mockMvc.perform(post(BASE + "/" + ruta + "/99/anular").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).with(csrf()).header("Referer", BASE).param("motivo", "Error de carga"))
                    .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        }
        verifyNoInteractions(limpiezaServicio, fallaServicio, fraccionamientoServicio, mantenimientoServicio, despachoServicio, devolucionServicio);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Rol.class, names = {"GERENTE_DE_PRODUCCION", "ENCARGADO_DE_DEPOSITO", "GERENTE_DE_COMPRAS", "ADMINISTRADOR"})
    @DisplayName("CP-OBC-10: los roles sin permisos de registrar y anular (incluido el gerente de producción) son rechazados en las seis operaciones")
    void rolesNoAutorizados(Rol rol) throws Exception {
        for (String ruta : List.of("limpiezas", "fallas", "fraccionamientos", "mantenimientos", "despachos", "devoluciones")) {
            mockMvc.perform(registro(ruta, rol, "2026-10-20T10:00", "x")).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
            mockMvc.perform(post(BASE + "/" + ruta + "/7/anular").with(como(rol)).with(csrf()).param("motivo", "x")).andExpect(status().is3xxRedirection())
                    .andExpect(flash().attribute("tipo", "danger"));
        }
        for (String ruta : List.of("/limpiezas/nueva", "/fallas/nueva", "/fraccionamientos/nuevo", "/mantenimientos/nuevo", "/despachos/nuevo", "/devoluciones/nueva")) {
            mockMvc.perform(get(BASE + ruta).with(como(rol))).andExpect(status().is3xxRedirection()).andExpect(flash().attribute("tipo", "danger"));
        }
        verifyNoInteractions(limpiezaServicio, fallaServicio, fraccionamientoServicio, mantenimientoServicio, despachoServicio, devolucionServicio, controlServicio);
    }

    @Test
    @DisplayName("CP-OBC-11: el operario no registra ni anula despachos ni devoluciones, y el encargado no registra ni anula las otras cuatro operaciones")
    void cruceEntreOperarioYEncargado() throws Exception {
        for (String ruta : RUTAS_ENCARGADO) {
            mockMvc.perform(registro(ruta, Rol.OPERARIO_DE_PRODUCCION, "2026-10-20T10:00", "x")).andExpect(flash().attribute("tipo", "danger"));
            mockMvc.perform(post(BASE + "/" + ruta + "/7/anular").with(como(Rol.OPERARIO_DE_PRODUCCION)).with(csrf()).param("motivo", "x")).andExpect(flash().attribute("tipo", "danger"));
            mockMvc.perform(get(BASE + "/" + ruta + (ruta.equals("despachos") ? "/nuevo" : "/nueva")).with(como(Rol.OPERARIO_DE_PRODUCCION))).andExpect(flash().attribute("tipo", "danger"));
        }
        for (String ruta : RUTAS_OPERARIO) {
            mockMvc.perform(registro(ruta, Rol.ENCARGADO_DE_DISTRIBUCION, "2026-10-20T10:00", "x")).andExpect(flash().attribute("tipo", "danger"));
            mockMvc.perform(post(BASE + "/" + ruta + "/7/anular").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)).with(csrf()).param("motivo", "x")).andExpect(flash().attribute("tipo", "danger"));
        }
        verifyNoInteractions(limpiezaServicio, fallaServicio, fraccionamientoServicio, mantenimientoServicio, despachoServicio, devolucionServicio);
    }

    @Test
    @DisplayName("CP-OBC-12: el formulario de limpieza es una página con el título, la información del barril, la fecha y las observaciones en filas propias, Cancelar hacia la pestaña y Registrar limpieza, y es novalidate")
    void formularioDeRegistro() throws Exception {
        String html = mockMvc.perform(get(BASE + "/limpiezas/nueva").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("Registrar limpieza").contains("data-informacion-barril").contains("novalidate").contains("name=\"fecha\"").contains("name=\"observaciones\"")
                .contains("type=\"datetime-local\"").contains("Fecha y hora de la operación").contains("Ej: Limpieza con soda cáustica y enjuague")
                .contains("href=\"/control-barriles/5?pestana=limpiezas\"").contains(">Cancelar<").contains("action=\"/control-barriles/5/limpiezas\"")
                .doesNotContain("cantidadExtraida").doesNotContain("idCliente");
    }

    @Test
    @DisplayName("CP-OBC-13: el formulario de fraccionamiento pide la cantidad extraída en litros antes de la fecha, y el de devolución avisa que se descarta el contenido")
    void formularioFraccionamientoYDevolucion() throws Exception {
        configurarBarril(EstadoOperativoBarril.CON_CERVEZA);
        String fraccionamiento = mockMvc.perform(get(BASE + "/fraccionamientos/nuevo").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        configurarBarril(EstadoOperativoBarril.DESPACHADO);
        String devolucion = mockMvc.perform(get(BASE + "/devoluciones/nueva").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(fraccionamiento).contains("Registrar fraccionamiento").contains("Cantidad extraída").contains("name=\"cantidadExtraida\"").contains("Ej: 5").contains("data-unidad-sufijo");
        assertThat(fraccionamiento.indexOf("Cantidad extraída")).isLessThan(fraccionamiento.indexOf("Fecha y hora de la operación"));
        assertThat(devolucion).contains("Registrar devolución").contains("Al registrar la devolución se descarta el contenido del barril").doesNotContain("cantidadExtraida");
    }

    @Test
    @DisplayName("CP-OBC-14: el formulario de despacho lleva el selector de cliente con su buscador, sin alta al vuelo, y la fecha estimada de devolución en la fila de las fechas")
    void formularioDespacho() throws Exception {
        configurarBarril(EstadoOperativoBarril.CON_CERVEZA);
        String html = mockMvc.perform(get(BASE + "/despachos/nuevo").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(html).contains("Registrar despacho").contains("name=\"idCliente\"").contains("data-buscador-url=\"/clientes/buscador\"").contains("Seleccione cliente")
                .contains("name=\"fechaDevolucionEstimada\"").contains("Fecha estimada de devolución").doesNotContain("data-selector-menu");
    }

    @Test
    @DisplayName("CP-OBC-15: si el estado del barril no permite la operación, el formulario vuelve a la pestaña con el aviso del service, y con el barril EN_USO avisa que lo ocupa un lote")
    void formularioConEstadoNoPermitido() throws Exception {
        mockMvc.perform(get(BASE + "/fallas/nueva").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl(BASE + "?pestana=fallas")).andExpect(flash().attribute("tipo", "warning"))
                .andExpect(flash().attribute("mensaje", "Solo se puede registrar una falla sobre un barril en estado operativo Disponible"));
        mockMvc.perform(get(BASE + "/despachos/nuevo").with(como(Rol.ENCARGADO_DE_DISTRIBUCION)))
                .andExpect(redirectedUrl(BASE + "?pestana=despachos")).andExpect(flash().attribute("mensaje", "Solo se puede registrar un despacho sobre un barril en estado operativo Con cerveza"));
        configurarBarril(EstadoOperativoBarril.EN_USO);
        mockMvc.perform(get(BASE + "/limpiezas/nueva").with(como(Rol.OPERARIO_DE_PRODUCCION)))
                .andExpect(flash().attribute("mensaje", "No se puede registrar una limpieza porque el barril está en uso por un lote"));
    }

    @Test
    @DisplayName("CP-OBC-16: registrar sin token CSRF es rechazado y no llega al service")
    void sinCsrf() throws Exception {
        mockMvc.perform(post(BASE + "/limpiezas").with(como(Rol.OPERARIO_DE_PRODUCCION)).param("fecha", "2026-10-20T10:00")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(limpiezaServicio);
    }
}
