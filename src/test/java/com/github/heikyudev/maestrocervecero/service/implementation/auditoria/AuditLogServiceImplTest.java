package com.github.heikyudev.maestrocervecero.service.implementation.auditoria;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.repository.auditoria.IAuditLogRepository;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogDetalleResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.AuditLogResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.CampoAuditoriaResponseDTO;
import com.github.heikyudev.maestrocervecero.service.response_dto.auditoria.EstadoCampoAuditoria;
import com.github.heikyudev.maestrocervecero.util.auditoria.ComparadorEstadosAuditoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private IAuditLogRepository auditLogRepository;

    // Real (no simulado): la comparación de los JSON es parte del comportamiento que se prueba
    @Spy
    private ComparadorEstadosAuditoria comparadorEstados = new ComparadorEstadosAuditoria(JsonMapper.builder().build());

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
                .hasMessage("La fecha hasta no puede ser anterior a la fecha desde");
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

    @Test
    @DisplayName("CP-FA-10: filtrarAuditLogs colapsa los espacios internos del username antes de consultar")
    void filtrarAuditLogs_debeColapsarEspaciosInternosDelUsername() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 20);
        when(auditLogRepository.filtrarAuditLogs(null, null, null, null, "juan perez", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        auditLogService.filtrarAuditLogs(null, null, null, null, "  juan   perez ", pageable);

        // === ASSERTS ===
        verify(auditLogRepository).filtrarAuditLogs(null, null, null, null, "juan perez", pageable);
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

    @Test
    @DisplayName("CP-GA-04: si falla al persistir una entrada con estados, se reintenta una vez sin ellos para conservar la entrada")
    void guardar_debeReintentarSinEstadosSiFallaConEstados() {
        // === PREPARACION DE DATOS ===
        AuditLogEntity entrada = crearEntrada(null, "admin", AccionAuditoria.MODIFICAR, ConceptoAuditoria.MALTA, "12",
                LocalDateTime.of(2026, 9, 15, 10, 30), "127.0.0.1");
        entrada.setEstadoAnterior("{\"id\":12}");
        entrada.setEstadoPosterior("{\"id\":12,\"nombre\":\"x\"}");
        when(auditLogRepository.save(any(AuditLogEntity.class)))
                .thenThrow(new RuntimeException("valor demasiado largo"))
                .thenReturn(entrada);

        // === EJECUCION ===
        assertThatCode(() -> auditLogService.guardar(entrada)).doesNotThrowAnyException();

        // === ASSERTS ===
        verify(auditLogRepository, times(2)).save(entrada);
        assertThat(entrada.getEstadoAnterior()).isNull();
        assertThat(entrada.getEstadoPosterior()).isNull();
    }

    @Test
    @DisplayName("CP-GA-05: si el reintento sin estados también falla, no se propaga la excepción ni se reintenta de nuevo")
    void guardar_noDebeReintentarMasDeUnaVez() {
        // === PREPARACION DE DATOS ===
        AuditLogEntity entrada = crearEntrada(null, "admin", AccionAuditoria.MODIFICAR, ConceptoAuditoria.MALTA, "12",
                LocalDateTime.of(2026, 9, 15, 10, 30), "127.0.0.1");
        entrada.setEstadoPosterior("{\"id\":12}");
        when(auditLogRepository.save(any(AuditLogEntity.class))).thenThrow(new RuntimeException("base de datos caída"));

        // === EJECUCION Y ASSERTS ===
        assertThatCode(() -> auditLogService.guardar(entrada)).doesNotThrowAnyException();
        verify(auditLogRepository, times(2)).save(entrada);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BA-01: buscarPorId de una modificación retorna la cabecera y compara campo a campo el estado anterior y el posterior")
    void buscarPorId_debeCompararLosEstadosDeUnaModificacion() {
        // === PREPARACION DE DATOS ===
        AuditLogEntity entrada = crearEntrada(7L, "admin", AccionAuditoria.MODIFICAR, ConceptoAuditoria.MALTA, "12",
                LocalDateTime.of(2026, 9, 15, 10, 30), "127.0.0.1");
        entrada.setEstadoAnterior("{\"id\":12,\"nombre\":\"Pilsen\",\"potencialExtracto\":80}");
        entrada.setEstadoPosterior("{\"id\":12,\"nombre\":\"Pilsen Premium\",\"potencialExtracto\":80}");
        when(auditLogRepository.findById(7L)).thenReturn(Optional.of(entrada));

        // === EJECUCION ===
        AuditLogDetalleResponseDTO detalle = auditLogService.buscarPorId(7L);

        // === ASSERTS ===
        assertThat(detalle.getId()).isEqualTo(7L);
        assertThat(detalle.getUsername()).isEqualTo("admin");
        assertThat(detalle.getAccion()).isEqualTo(AccionAuditoria.MODIFICAR);
        assertThat(detalle.getConceptoAuditoria()).isEqualTo(ConceptoAuditoria.MALTA);
        assertThat(detalle.getEntidadId()).isEqualTo("12");
        assertThat(detalle.getFechaHora()).isEqualTo(LocalDateTime.of(2026, 9, 15, 10, 30));
        assertThat(detalle.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(detalle.isTieneDetalle()).isTrue();
        assertThat(detalle.getCampos()).extracting(CampoAuditoriaResponseDTO::getCampo, CampoAuditoriaResponseDTO::getEstado)
                .containsExactly(
                        tuple("ID", EstadoCampoAuditoria.SIN_CAMBIOS),
                        tuple("Nombre", EstadoCampoAuditoria.MODIFICADO),
                        tuple("Potencial extracto", EstadoCampoAuditoria.SIN_CAMBIOS));
        assertThat(detalle.getCampos().get(1).getValorAnterior()).isEqualTo("Pilsen");
        assertThat(detalle.getCampos().get(1).getValorPosterior()).isEqualTo("Pilsen Premium");
    }

    @Test
    @DisplayName("CP-BA-02: buscarPorId de un alta (sin estado anterior) marca todos los campos como agregados")
    void buscarPorId_debeMarcarTodoComoAgregadoEnUnAlta() {
        // === PREPARACION DE DATOS ===
        AuditLogEntity entrada = crearEntrada(8L, "admin", AccionAuditoria.CREAR, ConceptoAuditoria.MALTA, "13",
                LocalDateTime.of(2026, 9, 15, 10, 30), null);
        entrada.setEstadoPosterior("{\"id\":13,\"nombre\":\"Cascade\"}");
        when(auditLogRepository.findById(8L)).thenReturn(Optional.of(entrada));

        // === EJECUCION ===
        AuditLogDetalleResponseDTO detalle = auditLogService.buscarPorId(8L);

        // === ASSERTS ===
        assertThat(detalle.isTieneDetalle()).isTrue();
        assertThat(detalle.getCampos()).extracting(CampoAuditoriaResponseDTO::getEstado).containsOnly(EstadoCampoAuditoria.AGREGADO);
        assertThat(detalle.getCampos()).extracting(CampoAuditoriaResponseDTO::getValorAnterior).containsOnlyNulls();
    }

    @Test
    @DisplayName("CP-BA-03: buscarPorId de una baja lógica marca solo el campo estado como modificado")
    void buscarPorId_debeMostrarLaBajaLogicaComoCambioDeEstado() {
        // === PREPARACION DE DATOS ===
        AuditLogEntity entrada = crearEntrada(9L, "admin", AccionAuditoria.ELIMINAR, ConceptoAuditoria.MALTA, "12",
                LocalDateTime.of(2026, 9, 15, 10, 30), null);
        entrada.setEstadoAnterior("{\"id\":12,\"estado\":\"ACTIVO\"}");
        entrada.setEstadoPosterior("{\"id\":12,\"estado\":\"BAJA\"}");
        when(auditLogRepository.findById(9L)).thenReturn(Optional.of(entrada));

        // === EJECUCION ===
        AuditLogDetalleResponseDTO detalle = auditLogService.buscarPorId(9L);

        // === ASSERTS ===
        assertThat(detalle.getCampos()).extracting(CampoAuditoriaResponseDTO::getEstado)
                .containsExactly(EstadoCampoAuditoria.SIN_CAMBIOS, EstadoCampoAuditoria.MODIFICADO);
        assertThat(detalle.getCampos().get(1).getValorAnterior()).isEqualTo("ACTIVO");
        assertThat(detalle.getCampos().get(1).getValorPosterior()).isEqualTo("BAJA");
    }

    @Test
    @DisplayName("CP-BA-04: buscarPorId detecta los campos agregados y eliminados entre los dos estados")
    void buscarPorId_debeDetectarCamposAgregadosYEliminados() {
        // === PREPARACION DE DATOS ===
        AuditLogEntity entrada = crearEntrada(10L, "admin", AccionAuditoria.MODIFICAR, ConceptoAuditoria.MALTA, "12",
                LocalDateTime.of(2026, 9, 15, 10, 30), null);
        entrada.setEstadoAnterior("{\"id\":12,\"viejo\":\"x\"}");
        entrada.setEstadoPosterior("{\"id\":12,\"nuevo\":\"y\"}");
        when(auditLogRepository.findById(10L)).thenReturn(Optional.of(entrada));

        // === EJECUCION ===
        AuditLogDetalleResponseDTO detalle = auditLogService.buscarPorId(10L);

        // === ASSERTS ===
        assertThat(detalle.getCampos()).extracting(CampoAuditoriaResponseDTO::getEstado)
                .containsExactly(EstadoCampoAuditoria.SIN_CAMBIOS, EstadoCampoAuditoria.ELIMINADO, EstadoCampoAuditoria.AGREGADO);
    }

    @Test
    @DisplayName("CP-BA-05: buscarPorId de una entrada sin estados guardados retorna el detalle sin campos y sin fallar")
    void buscarPorId_debeRetornarSinDetalleCuandoNoHayEstados() {
        // === PREPARACION DE DATOS ===
        AuditLogEntity entrada = crearEntrada(11L, "admin", AccionAuditoria.LOGIN_EXITOSO, ConceptoAuditoria.SESION, null,
                LocalDateTime.of(2026, 9, 15, 10, 30), "127.0.0.1");
        when(auditLogRepository.findById(11L)).thenReturn(Optional.of(entrada));

        // === EJECUCION ===
        AuditLogDetalleResponseDTO detalle = auditLogService.buscarPorId(11L);

        // === ASSERTS ===
        assertThat(detalle.isTieneDetalle()).isFalse();
        assertThat(detalle.getCampos()).isEmpty();
        assertThat(detalle.getUsername()).isEqualTo("admin");
    }

    @Test
    @DisplayName("CP-BA-06: buscarPorId con JSON inválido en ambos estados retorna el detalle sin campos y sin lanzar excepción")
    void buscarPorId_debeDegradarConJsonInvalido() {
        // === PREPARACION DE DATOS ===
        AuditLogEntity entrada = crearEntrada(12L, "admin", AccionAuditoria.MODIFICAR, ConceptoAuditoria.MALTA, "12",
                LocalDateTime.of(2026, 9, 15, 10, 30), null);
        entrada.setEstadoAnterior("{esto no es json");
        entrada.setEstadoPosterior("[1, 2");
        when(auditLogRepository.findById(12L)).thenReturn(Optional.of(entrada));

        // === EJECUCION ===
        AuditLogDetalleResponseDTO detalle = auditLogService.buscarPorId(12L);

        // === ASSERTS ===
        assertThat(detalle.isTieneDetalle()).isFalse();
        assertThat(detalle.getCampos()).isEmpty();
    }

    @Test
    @DisplayName("CP-BA-07: buscarPorId con un id inexistente lanza RecursoNoEncontradoException")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // === PREPARACION DE DATOS ===
        when(auditLogRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> auditLogService.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el registro de auditoría con ID: 99");
    }

    @Test
    @DisplayName("CP-FA-11: filtrarAuditLogs informa en cada entrada si guardó el estado anterior o el posterior, sin leer el JSON")
    void filtrarAuditLogs_debeInformarSiLaEntradaTieneDetalle() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 20);
        AuditLogEntity conPosterior = crearEntrada(1L, "admin", AccionAuditoria.CREAR, ConceptoAuditoria.MALTA, "1", LocalDateTime.of(2026, 9, 15, 10, 30), null);
        conPosterior.setEstadoPosterior("{\"id\":1}");
        AuditLogEntity conAnterior = crearEntrada(2L, "admin", AccionAuditoria.ELIMINAR, ConceptoAuditoria.MALTA, "2", LocalDateTime.of(2026, 9, 15, 10, 30), null);
        conAnterior.setEstadoAnterior("{\"id\":2}");
        AuditLogEntity enBlanco = crearEntrada(3L, "admin", AccionAuditoria.MODIFICAR, ConceptoAuditoria.MALTA, "3", LocalDateTime.of(2026, 9, 15, 10, 30), null);
        enBlanco.setEstadoAnterior("   ");
        AuditLogEntity sinEstados = crearEntrada(4L, "admin", AccionAuditoria.LOGOUT, ConceptoAuditoria.SESION, null, LocalDateTime.of(2026, 9, 15, 10, 30), null);
        when(auditLogRepository.filtrarAuditLogs(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(conPosterior, conAnterior, enBlanco, sinEstados), pageable, 4));

        // === EJECUCION ===
        Page<AuditLogResponseDTO> resultado = auditLogService.filtrarAuditLogs(null, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).extracting(AuditLogResponseDTO::isTieneDetalle).containsExactly(true, true, false, false);
        verifyNoInteractions(comparadorEstados);
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
