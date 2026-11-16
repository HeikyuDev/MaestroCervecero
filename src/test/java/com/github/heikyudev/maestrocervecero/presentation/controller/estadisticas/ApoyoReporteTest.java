package com.github.heikyudev.maestrocervecero.presentation.controller.estadisticas;

import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.ui.ExtendedModelMap;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApoyoReporteTest {

    @Test
    @DisplayName("CP-ESTA-01: volverALaPantalla redirige a la pantalla del reporte con los parámetros recibidos no vacíos, en su orden y codificados")
    void volverALaPantalla() {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("desde", "2026-11-01");
        parametros.put("hasta", " 2026-11-30 ");
        parametros.put("idReceta", "");
        parametros.put("estado", null);
        parametros.put("texto", "a b");

        assertThat(ApoyoReporte.volverALaPantalla("/estadisticas", parametros))
                .isEqualTo("redirect:/estadisticas?desde=2026-11-01&hasta=2026-11-30&texto=a%20b");
        assertThat(ApoyoReporte.volverALaPantalla("/estadisticas", Map.of())).isEqualTo("redirect:/estadisticas");
    }

    @Test
    @DisplayName("CP-ESTA-02: respuestaPdf devuelve el PDF en línea con el nombre reporte_desde_hasta.pdf")
    void respuestaPdf() {
        ResponseEntity<byte[]> respuesta = ApoyoReporte.respuestaPdf(new byte[]{1, 2}, "estadisticas-lotes", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(respuesta.getHeaders().getContentType()).hasToString("application/pdf");
        assertThat(respuesta.getHeaders().getFirst("Content-Disposition")).isEqualTo("inline; filename=\"estadisticas-lotes_2026-11-01_2026-11-30.pdf\"");
        assertThat(respuesta.getBody()).containsExactly(1, 2);
    }

    @Test
    @DisplayName("CP-ESTA-03: el emisor es el usuario con su nombre y apellido; si la cuenta no se encuentra, solo el usuario")
    void emisor() {
        IUsuarioServicio usuarios = mock(IUsuarioServicio.class);
        when(usuarios.buscarPorUsername("ana")).thenReturn(UsuarioResponseDTO.builder().nombre("Ana").apellido("Pérez").build());
        when(usuarios.buscarPorUsername("nadie")).thenThrow(new RecursoNoEncontradoException("No se encontró el usuario nadie"));

        assertThat(ApoyoReporte.emisor(new UsernamePasswordAuthenticationToken("ana", "x"), usuarios)).isEqualTo("ana (Ana Pérez)");
        assertThat(ApoyoReporte.emisor(new UsernamePasswordAuthenticationToken("nadie", "x"), usuarios)).isEqualTo("nadie");
    }

    @Test
    @DisplayName("CP-ESTA-04: mostrarMensajes muestra un error solo, varios en lista o, sin errores, los avisos; aEnum interpreta sin distinguir mayúsculas")
    void mensajesYEnum() {
        ExtendedModelMap model = new ExtendedModelMap();
        ApoyoReporte.mostrarMensajes(model, List.of("La fecha desde no es válida"), List.of("Aviso"));
        assertThat(model.get("mensaje")).isEqualTo("La fecha desde no es válida");
        assertThat(model.get("tipo")).isEqualTo("warning");

        ExtendedModelMap avisos = new ExtendedModelMap();
        ApoyoReporte.mostrarMensajes(avisos, List.of(), List.of("La etapa indicada no es válida, se muestran todas"));
        assertThat((String) avisos.get("mensaje")).contains("La etapa indicada no es válida, se muestran todas");

        ExtendedModelMap nada = new ExtendedModelMap();
        ApoyoReporte.mostrarMensajes(nada, List.of(), List.of());
        assertThat(nada.containsAttribute("mensaje")).isFalse();

        assertThat(ApoyoReporte.aEnum(TipoEtapa.class, " fermentacion ")).isEqualTo(TipoEtapa.FERMENTACION);
        assertThat(ApoyoReporte.aEnum(TipoEtapa.class, "otra")).isNull();
        assertThat(ApoyoReporte.aEnum(TipoEtapa.class, "")).isNull();
    }
}
