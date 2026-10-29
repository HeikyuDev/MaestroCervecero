package com.github.heikyudev.maestrocervecero.presentation.controller.ubicacion;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.ILocalidadServicio;
import com.github.heikyudev.maestrocervecero.service.interfaces.ubicacion.IProvinciaServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.ProvinciaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UbicacionOpcionesController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class UbicacionOpcionesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IProvinciaServicio provinciaServicio;

    @MockitoBean
    private ILocalidadServicio localidadServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-UOC-01: las provincias de un país se devuelven como JSON con id y nombre, en el orden del service")
    void provincias_debeDevolverLasProvinciasDelPais() throws Exception {
        // Arrange
        when(provinciaServicio.listarProvinciasActivasDePais(3L)).thenReturn(List.of(
                ProvinciaResponseDTO.builder().id(2L).nombre("Córdoba").build(),
                ProvinciaResponseDTO.builder().id(4L).nombre("Misiones").build()));

        // Act + Assert
        mockMvc.perform(get("/ubicaciones/opciones/provincias").param("idPais", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Córdoba"))
                .andExpect(jsonPath("$[1].id").value(4))
                .andExpect(jsonPath("$[1].nombre").value("Misiones"));
        verify(provinciaServicio).listarProvinciasActivasDePais(3L);
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-UOC-02: las localidades de una provincia se devuelven como JSON con id y nombre, en el orden del service")
    void localidades_debeDevolverLasLocalidadesDeLaProvincia() throws Exception {
        // Arrange
        when(localidadServicio.listarLocalidadesActivasDeProvincia(2L)).thenReturn(List.of(
                LocalidadResponseDTO.builder().id(9L).nombre("Río Cuarto").build()));

        // Act + Assert
        mockMvc.perform(get("/ubicaciones/opciones/localidades").param("idProvincia", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].nombre").value("Río Cuarto"));
        verify(localidadServicio).listarLocalidadesActivasDeProvincia(2L);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("CP-UOC-03: el ADMINISTRADOR también accede a las opciones; un país sin provincias devuelve una lista vacía")
    void provincias_debePermitirAlAdministradorYDevolverListaVacia() throws Exception {
        // Arrange
        when(provinciaServicio.listarProvinciasActivasDePais(7L)).thenReturn(List.of());

        // Act + Assert
        mockMvc.perform(get("/ubicaciones/opciones/provincias").param("idPais", "7"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-UOC-04: un rol que no elige ubicaciones no accede a las opciones y no se invoca ningún service")
    void opciones_debeRechazarAUnRolSinPermiso() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/ubicaciones/opciones/provincias").param("idPais", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));
        mockMvc.perform(get("/ubicaciones/opciones/localidades").param("idProvincia", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(provinciaServicio, never()).listarProvinciasActivasDePais(any());
        verify(localidadServicio, never()).listarLocalidadesActivasDeProvincia(any());
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_COMPRAS")
    @DisplayName("CP-UOC-05: sin el parámetro obligatorio la petición se rechaza con la alerta del ControllerAdvices y no se invoca el service")
    void opciones_debeRechazarLaPeticionSinParametro() throws Exception {
        // Act + Assert
        mockMvc.perform(get("/ubicaciones/opciones/provincias"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(provinciaServicio, never()).listarProvinciasActivasDePais(any());
    }
}
