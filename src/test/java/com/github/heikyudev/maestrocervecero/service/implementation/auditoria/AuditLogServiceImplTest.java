package com.github.heikyudev.maestrocervecero.service.implementation.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.repository.auditoria.IAuditLogRepository;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private IAuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    // ==================== filtrarAuditLogs ====================

    @Test
    @DisplayName("CP-FA-01: filtrarAuditLogs retorna una página correctamente mapeada a DTO cuando se filtra por fechas, acción, concepto y username")
    void filtrarAuditLogs_debeRetornarPaginaMapeadaFiltrandoPorLosCincoCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime desde = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 9, 30, 23, 59, 59);
        AuditLogEntity entrada = crearEntrada(1L, "admin", AccionAuditoria.CREAR, ConceptoAuditoria.MALTA, "12",
                LocalDateTime.of(2026, 9, 15, 10, 30), "127.0.0.1");
        AuditLogEntity otraEntrada = crearEntrada(2L, "admin2", AccionAuditoria.CREAR, ConceptoAuditoria.MALTA, null,
                LocalDateTime.of(2026, 9, 16, 11, 0), null);
        when(auditLogRepository.filtrarAuditLogs(desde, hasta, AccionAuditoria.CREAR, ConceptoAuditoria.MALTA, "adm", pageable))
                .thenReturn(new PageImpl<>(List.of(entrada, otraEntrada), pageable, 2));

        // === EJECUCION ===
        Page<AuditLogResponseDTO> resultado = auditLogService.filtrarAuditLogs(
                desde, hasta, AccionAuditoria.CREAR, ConceptoAuditoria.MALTA, "adm", pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertAuditLogDTO(entrada, resultado.getContent().get(0));
        assertAuditLogDTO(otraEntrada, resultado.getContent().get(1));
        verify(auditLogRepository).filtrarAuditLogs(desde, hasta, AccionAuditoria.CREAR, ConceptoAuditoria.MALTA, "adm", pageable);
    }

    @Test
    @DisplayName("CP-FA-02: filtrarAuditLogs propaga todos los criterios nulos sin restringir la búsqueda")
    void filtrarAuditLogs_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 20);
        AuditLogEntity entrada = crearEntrada(1L, "admin", AccionAuditoria.LOGIN_EXITOSO, ConceptoAuditoria.SESION, null,
                LocalDateTime.of(2026, 9, 15, 10, 30), "127.0.0.1");
        when(auditLogRepository.filtrarAuditLogs(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(entrada), pageable, 1));

        // === EJECUCION ===
        Page<AuditLogResponseDTO> resultado = auditLogService.filtrarAuditLogs(null, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(auditLogRepository).filtrarAuditLogs(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FA-03: filtrarAuditLogs retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarAuditLogs_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.filtrarAuditLogs(null, null, null, null, "inexistente", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<AuditLogResponseDTO> resultado = auditLogService.filtrarAuditLogs(null, null, null, null, "inexistente", pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(auditLogRepository).filtrarAuditLogs(null, null, null, null, "inexistente", pageable);
    }

    @Test
    @DisplayName("CP-FA-04: filtrarAuditLogs rechaza un rango de fechas invertido sin consultar el repositorio")
    void filtrarAuditLogs_debeRechazarRangoInvertido() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime desde = LocalDateTime.of(2026, 9, 30, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 9, 1, 0, 0);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> auditLogService.filtrarAuditLogs(desde, hasta, null, null, null, pageable))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha desde no puede ser posterior a la fecha hasta.");
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("CP-FA-05: filtrarAuditLogs acepta un rango con solo la fecha desde y la propaga tal cual")
    void filtrarAuditLogs_debeAceptarSoloFechaDesde() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime desde = LocalDateTime.of(2026, 9, 1, 0, 0);
        when(auditLogRepository.filtrarAuditLogs(desde, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<AuditLogResponseDTO> resultado = auditLogService.filtrarAuditLogs(desde, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(auditLogRepository).filtrarAuditLogs(desde, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FA-06: filtrarAuditLogs acepta un rango con solo la fecha hasta y la propaga tal cual")
    void filtrarAuditLogs_debeAceptarSoloFechaHasta() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime hasta = LocalDateTime.of(2026, 9, 30, 23, 59, 59);
        when(auditLogRepository.filtrarAuditLogs(null, hasta, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<AuditLogResponseDTO> resultado = auditLogService.filtrarAuditLogs(null, hasta, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(auditLogRepository).filtrarAuditLogs(null, hasta, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FA-07: filtrarAuditLogs acepta una fecha desde igual a la fecha hasta (límites inclusivos)")
    void filtrarAuditLogs_debeAceptarFechasIguales() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 20);
        LocalDateTime instante = LocalDateTime.of(2026, 9, 15, 10, 30);
        when(auditLogRepository.filtrarAuditLogs(instante, instante, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> auditLogService.filtrarAuditLogs(instante, instante, null, null, null, pageable))
                .doesNotThrowAnyException();
        verify(auditLogRepository).filtrarAuditLogs(instante, instante, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FA-08: filtrarAuditLogs trata un username en blanco como \"sin filtro\" y consulta con null")
    void filtrarAuditLogs_debeNormalizarUsernameEnBlanco() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.filtrarAuditLogs(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        auditLogService.filtrarAuditLogs(null, null, null, null, "   ", pageable);

        // === ASSERTS ===
        verify(auditLogRepository).filtrarAuditLogs(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FA-09: filtrarAuditLogs recorta los espacios sobrantes del username antes de consultar")
    void filtrarAuditLogs_debeRecortarElUsername() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.filtrarAuditLogs(null, null, null, null, "admin", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        auditLogService.filtrarAuditLogs(null, null, null, null, "  admin  ", pageable);

        // === ASSERTS ===
        verify(auditLogRepository).filtrarAuditLogs(null, null, null, null, "admin", pageable);
    }

    // ==================== guardar ====================

    @Test
    @DisplayName("CP-GA-01: guardar persiste la entrada conservando la fecha y hora que ya trae")
    void guardar_debeConservarLaFechaHoraInformada() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaHora = LocalDateTime.of(2026, 9, 15, 10, 30);
        AuditLogEntity entrada = crearEntrada(null, "admin", AccionAuditoria.CREAR, ConceptoAuditoria.MALTA, "12", fechaHora, "127.0.0.1");

        // === EJECUCION ===
        auditLogService.guardar(entrada);

        // === ASSERTS ===
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(entrada);
        assertThat(captor.getValue().getFechaHora()).isEqualTo(fechaHora);
    }

    @Test
    @DisplayName("CP-GA-02: guardar completa la fecha y hora actual cuando la entrada no la trae")
    void guardar_debeCompletarLaFechaHoraSiFalta() {
        // === PREPARACION DE DATOS ===
        AuditLogEntity entrada = crearEntrada(null, "admin", AccionAuditoria.LOGOUT, ConceptoAuditoria.SESION, null, null, null);
        LocalDateTime antes = LocalDateTime.now();

        // === EJECUCION ===
        auditLogService.guardar(entrada);

        // === ASSERTS ===
        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getFechaHora()).isNotNull().isBetween(antes, LocalDateTime.now());
    }

    @Test
    @DisplayName("CP-GA-03: guardar no propaga una falla del repositorio para no afectar a la operación de negocio")
    void guardar_noDebePropagarFallasDelRepositorio() {
        // === PREPARACION DE DATOS ===
        AuditLogEntity entrada = crearEntrada(null, "admin", AccionAuditoria.CREAR, ConceptoAuditoria.MALTA, "12",
                LocalDateTime.of(2026, 9, 15, 10, 30), "127.0.0.1");
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenThrow(new RuntimeException("base de datos caída"));

        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> auditLogService.guardar(entrada)).doesNotThrowAnyException();
        verify(auditLogRepository).save(entrada);
    }

    // ==================== helpers ====================

    private static AuditLogEntity crearEntrada(Long id, String username, AccionAuditoria accion, ConceptoAuditoria concepto,
                                               String entidadId, LocalDateTime fechaHora, String ipAddress) {
        return AuditLogEntity.builder()
                .id(id)
                .username(username)
                .accion(accion)
                .conceptoAuditoria(concepto)
                .entidadId(entidadId)
                .fechaHora(fechaHora)
                .ipAddress(ipAddress)
                .build();
    }

    private static void assertAuditLogDTO(AuditLogEntity esperado, AuditLogResponseDTO real) {
        assertThat(real.getId()).isEqualTo(esperado.getId());
        assertThat(real.getUsername()).isEqualTo(esperado.getUsername());
        assertThat(real.getAccion()).isEqualTo(esperado.getAccion());
        assertThat(real.getConceptoAuditoria()).isEqualTo(esperado.getConceptoAuditoria());
        assertThat(real.getEntidadId()).isEqualTo(esperado.getEntidadId());
        assertThat(real.getFechaHora()).isEqualTo(esperado.getFechaHora());
        assertThat(real.getIpAddress()).isEqualTo(esperado.getIpAddress());
    }
}
