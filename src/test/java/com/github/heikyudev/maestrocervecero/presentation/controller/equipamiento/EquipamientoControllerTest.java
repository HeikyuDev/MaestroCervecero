package com.github.heikyudev.maestrocervecero.presentation.controller.equipamiento;

import com.github.heikyudev.maestrocervecero.configuration.security.SecurityConfig;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.TipoEquipamiento;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.presentation.advice.ControllerAdvices;
import com.github.heikyudev.maestrocervecero.service.implementation.usuario.UserDetailServiceImpl;
import com.github.heikyudev.maestrocervecero.service.interfaces.equipamiento.IEquipamientoServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.EquipamientoResumenResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ParticipacionLoteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.ResumenEquipamientoResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(EquipamientoController.class)
@Import({SecurityConfig.class, ControllerAdvices.class})
class EquipamientoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IEquipamientoServicio equipamientoServicio;

    @MockitoBean
    private UserDetailServiceImpl userDetailService;

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-REC-01: el GERENTE_DE_PRODUCCION ve las tarjetas con los totales por tipo y estado, la tabla de equipos y las cinco pestañas")
    void resumen_debeMostrarTotalesTablaYPestanasParaElGerente() throws Exception {
        when(equipamientoServicio.obtenerResumen()).thenReturn(resumen());
        when(equipamientoServicio.filtrarResumenEquipamientos(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                fila(1L, "FER-02", TipoEquipamiento.FERMENTADOR, EstadoOperativo.EN_USO,
                        new ParticipacionLoteResponseDTO(14L, "L-014", TipoEtapa.FERMENTACION), List.of()),
                fila(2L, "MOL-01", TipoEquipamiento.MOLINO, EstadoOperativo.DISPONIBLE, null,
                        List.of(new ParticipacionLoteResponseDTO(20L, "L-020", TipoEtapa.MOLIENDA)))),
                PageRequest.of(0, 20), 2));

        mockMvc.perform(get("/equipamiento"))
                .andExpect(status().isOk())
                .andExpect(view().name("equipamiento/resumen"))
                .andExpect(content().string(containsString("Gestión de Equipamiento")))
                .andExpect(content().string(containsString("Molinos")))
                .andExpect(content().string(containsString("Maceradores")))
                .andExpect(content().string(containsString("Ollas de hervor")))
                .andExpect(content().string(containsString("Fermentadores")))
                .andExpect(content().string(containsString("FER-02")))
                .andExpect(content().string(containsString("EN USO")))
                .andExpect(content().string(containsString("href=\"/equipamiento/molinos\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/maceradores\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/ollas-hervor\"")))
                .andExpect(content().string(containsString("href=\"/equipamiento/fermentadores\"")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-REC-02: las tarjetas muestran la cantidad de cada tipo en cada estado operativo")
    void resumen_debeMostrarLasCantidadesPorTipoYEstado() throws Exception {
        when(equipamientoServicio.obtenerResumen()).thenReturn(resumen());
        when(equipamientoServicio.filtrarResumenEquipamientos(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        String html = mockMvc.perform(get("/equipamiento")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Cada tarjeta lista los 4 estados con su cantidad: el molino tiene 7 disponibles, 2 en mantenimiento
        assertThat(html).contains("DISPONIBLE", "EN USO", "EN LIMPIEZA", "EN MANTENIMIENTO");
        assertThat(html).containsPattern("(?s)Molinos.*?DISPONIBLE.*?>7<.*?EN MANTENIMIENTO.*?>2<");
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-REC-03: un equipo en uso muestra el lote y la etapa en curso; uno reservado, los lotes pendientes; los demás, un guion")
    void resumen_debeMostrarLoteEnCursoYReservas() throws Exception {
        when(equipamientoServicio.obtenerResumen()).thenReturn(resumen());
        when(equipamientoServicio.filtrarResumenEquipamientos(any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                fila(1L, "FER-02", TipoEquipamiento.FERMENTADOR, EstadoOperativo.EN_USO,
                        new ParticipacionLoteResponseDTO(14L, "L-014", TipoEtapa.FERMENTACION), List.of()),
                fila(2L, "MOL-01", TipoEquipamiento.MOLINO, EstadoOperativo.DISPONIBLE, null,
                        List.of(new ParticipacionLoteResponseDTO(20L, "L-020", TipoEtapa.MOLIENDA),
                                new ParticipacionLoteResponseDTO(21L, "L-021", TipoEtapa.MOLIENDA))),
                fila(3L, "MAC-01", TipoEquipamiento.MACERADOR, EstadoOperativo.EN_MANTENIMIENTO, null, List.of())),
                PageRequest.of(0, 20), 3));

        mockMvc.perform(get("/equipamiento"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("L-014")))
                .andExpect(content().string(containsString("Fermentación")))
                .andExpect(content().string(containsString("L-020")))
                .andExpect(content().string(containsString("L-021")))
                .andExpect(content().string(containsString("Molienda")))
                .andExpect(content().string(containsString("EN MANTENIMIENTO")))
                .andExpect(content().string(containsString("—")));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-REC-04: sin filtros pide páginas de 20 ordenadas alfabéticamente por identificador, con los criterios en null")
    void resumen_debePedirPaginasDe20OrdenadasPorIdentificador() throws Exception {
        when(equipamientoServicio.obtenerResumen()).thenReturn(resumen());
        when(equipamientoServicio.filtrarResumenEquipamientos(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/equipamiento")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(equipamientoServicio).filtrarResumenEquipamientos(isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().stream().map(Sort.Order::getProperty).toList())
                .containsExactly("identificadorInterno", "id");
        assertThat(captor.getValue().getSort().getOrderFor("identificadorInterno").isIgnoreCase()).isTrue();
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-REC-05: los criterios llegan al service; un identificador en blanco y los selects \"Todos\" se convierten en null")
    void resumen_debePasarLosFiltrosAlService() throws Exception {
        when(equipamientoServicio.obtenerResumen()).thenReturn(resumen());
        when(equipamientoServicio.filtrarResumenEquipamientos(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 20), 0));

        mockMvc.perform(get("/equipamiento").param("identificador", " fer ").param("tipo", "FERMENTADOR")
                        .param("estadoOperativo", "DISPONIBLE").param("pagina", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/equipamiento").param("identificador", "  ").param("tipo", "").param("estadoOperativo", ""))
                .andExpect(status().isOk());

        verify(equipamientoServicio).filtrarResumenEquipamientos(eq("fer"), eq(TipoEquipamiento.FERMENTADOR),
                eq(EstadoOperativo.DISPONIBLE), any(Pageable.class));
        verify(equipamientoServicio).filtrarResumenEquipamientos(isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "GERENTE_DE_PRODUCCION")
    @DisplayName("CP-REC-06: sin resultados muestra un mensaje claro en vez de una tabla vacía, y los totales se siguen mostrando")
    void resumen_debeMostrarMensajeCuandoNoHayResultados() throws Exception {
        when(equipamientoServicio.obtenerResumen()).thenReturn(resumen());
        when(equipamientoServicio.filtrarResumenEquipamientos(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get("/equipamiento").param("identificador", "zzz"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se encontraron equipos con los criterios indicados.")))
                .andExpect(content().string(containsString("Molinos")));
    }

    @Test
    @WithMockUser(roles = "OPERARIO_DE_PRODUCCION")
    @DisplayName("CP-REC-07: el OPERARIO_DE_PRODUCCION ve el resumen pero no las pestañas de gestión")
    void resumen_elOperarioNoVeLasPestanasDeGestion() throws Exception {
        when(equipamientoServicio.obtenerResumen()).thenReturn(resumen());
        when(equipamientoServicio.filtrarResumenEquipamientos(any(), any(), any(), any())).thenReturn(new PageImpl<>(
                List.of(fila(1L, "FER-02", TipoEquipamiento.FERMENTADOR, EstadoOperativo.DISPONIBLE, null, List.of())),
                PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/equipamiento"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("FER-02")))
                .andExpect(content().string(containsString("href=\"/equipamiento\"")))
                .andExpect(content().string(not(containsString("href=\"/equipamiento/molinos\""))))
                .andExpect(content().string(not(containsString("href=\"/equipamiento/fermentadores\""))))
                .andExpect(content().string(not(containsString("data-baja-url"))));
    }

    @Test
    @WithMockUser(roles = "GERENTE_COMERCIAL")
    @DisplayName("CP-REC-08: un rol sin relación con la producción no puede ver el resumen (no llega al service)")
    void resumen_debeRechazarRolNoAutorizado() throws Exception {
        mockMvc.perform(get("/equipamiento"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("tipo", "danger"));

        verify(equipamientoServicio, never()).obtenerResumen();
        verify(equipamientoServicio, never()).filtrarResumenEquipamientos(any(), any(), any(), any());
    }

    @Test
    @DisplayName("CP-REC-09: un visitante sin sesión es enviado al login")
    void resumen_debeRedirigirAlLoginSiNoHaySesion() throws Exception {
        mockMvc.perform(get("/equipamiento"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));

        verify(equipamientoServicio, never()).obtenerResumen();
    }

    private static ResumenEquipamientoResponseDTO resumen() {
        Map<TipoEquipamiento, Map<EstadoOperativo, Long>> cantidades = new EnumMap<>(TipoEquipamiento.class);
        for (TipoEquipamiento tipo : TipoEquipamiento.values()) {
            Map<EstadoOperativo, Long> porEstado = new EnumMap<>(EstadoOperativo.class);
            for (EstadoOperativo estado : EstadoOperativo.values()) {
                porEstado.put(estado, 0L);
            }
            cantidades.put(tipo, porEstado);
        }
        cantidades.get(TipoEquipamiento.MOLINO).put(EstadoOperativo.DISPONIBLE, 7L);
        cantidades.get(TipoEquipamiento.MOLINO).put(EstadoOperativo.EN_MANTENIMIENTO, 2L);
        return ResumenEquipamientoResponseDTO.builder().cantidadesPorTipoYEstado(cantidades).build();
    }

    private static EquipamientoResumenResponseDTO fila(Long id, String identificador, TipoEquipamiento tipo, EstadoOperativo estado,
                                                       ParticipacionLoteResponseDTO enCurso, List<ParticipacionLoteResponseDTO> pendientes) {
        return EquipamientoResumenResponseDTO.builder().id(id).identificadorInterno(identificador).tipo(tipo)
                .estadoOperativo(estado).etapaEnCurso(enCurso).etapasPendientes(pendientes).build();
    }
}
