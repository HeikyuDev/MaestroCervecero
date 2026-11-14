package com.github.heikyudev.maestrocervecero.util.mapper.dashboard;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.ConteoDashboardResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.EventoAuditoriaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.dashboard.UsuarioRecienteResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MapperDashboardAdministradorTest {

    @Test
    @DisplayName("CP-MDA-01: el usuario reciente lleva su ID, su username, su nombre y apellido unidos y su rol")
    void toUsuarioRecienteDTO_llevaLosDatosDelUsuario() {
        // Arrange
        UsuarioResponseDTO usuario = UsuarioResponseDTO.builder().id(3L).username("jperez").nombre("Juan").apellido("Pérez").rol(Rol.OPERARIO_DE_PRODUCCION).build();

        // Act
        UsuarioRecienteResponseDTO dto = MapperDashboardAdministrador.toUsuarioRecienteDTO(usuario);

        // Assert
        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getUsername()).isEqualTo("jperez");
        assertThat(dto.getNombreCompleto()).isEqualTo("Juan Pérez");
        assertThat(dto.getRol()).isEqualTo(Rol.OPERARIO_DE_PRODUCCION);
    }

    @Test
    @DisplayName("CP-MDA-02: el evento de auditoría lleva su ID, su fecha y hora, su usuario, su acción, su módulo y si tiene detalle")
    void toEventoAuditoriaDTO_llevaLosDatosDeLaEntrada() {
        // Arrange
        AuditLogResponseDTO entrada = AuditLogResponseDTO.builder().id(41L).username("admin").accion(AccionAuditoria.CREAR).conceptoAuditoria(ConceptoAuditoria.MALTA)
                .fechaHora(LocalDateTime.of(2027, 7, 1, 8, 30)).tieneDetalle(true).build();

        // Act
        EventoAuditoriaResponseDTO dto = MapperDashboardAdministrador.toEventoAuditoriaDTO(entrada);

        // Assert
        assertThat(dto.getId()).isEqualTo(41L);
        assertThat(dto.getFechaHora()).isEqualTo(LocalDateTime.of(2027, 7, 1, 8, 30));
        assertThat(dto.getUsername()).isEqualTo("admin");
        assertThat(dto.getAccion()).isEqualTo(AccionAuditoria.CREAR);
        assertThat(dto.getConcepto()).isEqualTo(ConceptoAuditoria.MALTA);
        assertThat(dto.isTieneDetalle()).isTrue();
    }

    @Test
    @DisplayName("CP-MDA-03: el conteo lleva el código, la etiqueta y el total recibidos")
    void toConteoDTO_llevaLosTresDatos() {
        // Act
        ConteoDashboardResponseDTO dto = MapperDashboardAdministrador.toConteoDTO("ADMINISTRADOR", "Administrador", 2);

        // Assert
        assertThat(dto.getCodigo()).isEqualTo("ADMINISTRADOR");
        assertThat(dto.getEtiqueta()).isEqualTo("Administrador");
        assertThat(dto.getTotal()).isEqualTo(2);
    }

    @Test
    @DisplayName("CP-MDA-04: un usuario o una entrada nulos se mapean a nulo")
    void toDTO_conEntradaNulaDevuelveNulo() {
        // Act and Assert
        assertThat(MapperDashboardAdministrador.toUsuarioRecienteDTO(null)).isNull();
        assertThat(MapperDashboardAdministrador.toEventoAuditoriaDTO(null)).isNull();
    }
}
