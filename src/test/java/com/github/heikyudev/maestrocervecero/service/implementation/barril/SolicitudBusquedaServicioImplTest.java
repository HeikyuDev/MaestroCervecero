package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ISolicitudBusquedaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.SolicitudBusquedaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.SolicitudBusquedaResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudBusquedaServicioImplTest {

    @Mock
    private ISolicitudBusquedaRepository solicitudBusquedaRepository;
    @Mock
    private IDespachoBarrilRepository despachoBarrilRepository;
    @Mock
    private IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
    @Mock
    private IBarrilRepository barrilRepository;

    @InjectMocks
    private SolicitudBusquedaServicioImpl solicitudBusquedaServicio;

    // ==================== filtrarSolicitudesBusqueda ====================

    @Test
    @DisplayName("CP-FSB-01: filtrarSolicitudesBusqueda filtra por los 4 criterios informados")
    void filtrar_debeFiltrarPorLosCuatroCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 2, 28, 23, 59);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L);
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, LocalDateTime.of(2026, 2, 15, 10, 0), "Horario tarde", false, despacho);
        when(solicitudBusquedaRepository.filtrarSolicitudesBusqueda(1L, desde, hasta, false, pageable))
                .thenReturn(new PageImpl<>(List.of(solicitud), pageable, 1));

        // === EJECUCION ===
        Page<SolicitudBusquedaResponseDTO> resultado = solicitudBusquedaServicio.filtrarSolicitudesBusqueda(1L, desde, hasta, false, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("CP-FSB-02: Los 4 parámetros nulos no restringen la búsqueda, incluyendo mezcla de buscado")
    void filtrar_debePropagarParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L);
        SolicitudBusquedaEntity buscada = crearSolicitudEntity(1L, LocalDateTime.of(2026, 2, 15, 10, 0), "Obs A", true, despacho);
        SolicitudBusquedaEntity noBuscada = crearSolicitudEntity(2L, LocalDateTime.of(2026, 2, 16, 10, 0), "Obs B", false, despacho);
        when(solicitudBusquedaRepository.filtrarSolicitudesBusqueda(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(buscada, noBuscada), pageable, 2));

        // === EJECUCION ===
        Page<SolicitudBusquedaResponseDTO> resultado = solicitudBusquedaServicio.filtrarSolicitudesBusqueda(null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("CP-FSB-03: El usuario puede acotar explícitamente por buscado")
    void filtrar_debeAcotarPorBuscado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L);
        SolicitudBusquedaEntity buscada = crearSolicitudEntity(1L, LocalDateTime.of(2026, 2, 15, 10, 0), "Obs A", true, despacho);
        when(solicitudBusquedaRepository.filtrarSolicitudesBusqueda(null, null, null, true, pageable))
                .thenReturn(new PageImpl<>(List.of(buscada), pageable, 1));

        // === EJECUCION ===
        Page<SolicitudBusquedaResponseDTO> resultado = solicitudBusquedaServicio.filtrarSolicitudesBusqueda(null, null, null, true, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).isBuscado()).isTrue();
    }

    @Test
    @DisplayName("CP-FSB-04: filtrarSolicitudesBusqueda sin coincidencias")
    void filtrar_debeRetornarPaginaVacia() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.of(2030, 1, 1, 0, 0);
        when(solicitudBusquedaRepository.filtrarSolicitudesBusqueda(null, desde, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<SolicitudBusquedaResponseDTO> resultado = solicitudBusquedaServicio.filtrarSolicitudesBusqueda(null, desde, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
    }

    @Test
    @DisplayName("CP-FSB-05: Filtra por un despacho de barril específico")
    void filtrar_debeFiltrarPorDespachoEspecifico() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L);
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, LocalDateTime.of(2026, 2, 15, 10, 0), "Obs A", false, despacho);
        when(solicitudBusquedaRepository.filtrarSolicitudesBusqueda(1L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(solicitud), pageable, 1));

        // === EJECUCION ===
        Page<SolicitudBusquedaResponseDTO> resultado = solicitudBusquedaServicio.filtrarSolicitudesBusqueda(1L, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna la solicitud de búsqueda encontrada")
    void buscarPorId_debeRetornarSolicitudEncontrada() {
        // === PREPARACION DE DATOS ===
        DespachoBarrilEntity despacho = crearDespachoEntity(1L);
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, LocalDateTime.of(2026, 2, 15, 10, 0), "Horario tarde", false, despacho);
        when(solicitudBusquedaRepository.findById(1L)).thenReturn(Optional.of(solicitud));

        // === EJECUCION ===
        SolicitudBusquedaResponseDTO resultado = solicitudBusquedaServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getObservaciones()).isEqualTo("Horario tarde");
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // === PREPARACION DE DATOS ===
        when(solicitudBusquedaRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> solicitudBusquedaServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la solicitud de búsqueda con ID: 99");
    }

    // ==================== registrarSolicitudBusqueda ====================

    @Test
    @DisplayName("CP-RSB-01: registrarSolicitudBusqueda lanza ReglaNegocioException cuando la fecha de búsqueda es nula")
    void registrar_debeRechazarFechaBusquedaNula() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(1L, null, "Cliente prefiere horario de la tarde");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de búsqueda es obligatoria");

        verifyNoInteractions(despachoBarrilRepository, solicitudBusquedaRepository);
    }

    @Test
    @DisplayName("CP-RSB-02: registrarSolicitudBusqueda lanza ReglaNegocioException cuando la fecha de búsqueda es anterior a la fecha y hora actual")
    void registrar_debeRechazarFechaBusquedaAnteriorAHoy() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(1L, LocalDateTime.now().minusDays(1), "Cliente prefiere horario de la tarde");

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de búsqueda no puede ser anterior a la fecha y hora actual");

        verifyNoInteractions(despachoBarrilRepository, solicitudBusquedaRepository);
    }

    @Test
    @DisplayName("CP-RSB-03: registrarSolicitudBusqueda lanza RecursoNoEncontradoException cuando el despacho de barril no existe")
    void registrar_debeRechazarDespachoInexistente() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(99L, LocalDateTime.now().plusDays(1), "Cliente prefiere horario de la tarde");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el despacho de barril con ID: 99");

        verifyNoInteractions(barrilRepository);
        verify(despachoBarrilRepository, never()).findById(any());
        verify(solicitudBusquedaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RSB-04: registrarSolicitudBusqueda registra la solicitud correctamente (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaBusqueda = LocalDateTime.now().plusDays(1);
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(1L, fechaBusqueda, "Cliente prefiere horario de la tarde");
        DespachoBarrilEntity despacho = crearDespachoEntity(1L);
        stubDespachoYBarrilBloqueado(despacho);
        when(solicitudBusquedaRepository.save(any(SolicitudBusquedaEntity.class))).thenAnswer(invocation -> {
            SolicitudBusquedaEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        SolicitudBusquedaResponseDTO resultado = solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<SolicitudBusquedaEntity> captor = ArgumentCaptor.forClass(SolicitudBusquedaEntity.class);
        verify(solicitudBusquedaRepository).save(captor.capture());
        SolicitudBusquedaEntity solicitudGuardada = captor.getValue();
        assertThat(solicitudGuardada.getFechaBusqueda()).isEqualTo(fechaBusqueda);
        assertThat(solicitudGuardada.getObservaciones()).isEqualTo("Cliente prefiere horario de la tarde");
        assertThat(solicitudGuardada.isBuscado()).isFalse();
        assertThat(solicitudGuardada.getDespachoBarril()).isSameAs(despacho);

        assertThat(resultado.isBuscado()).isFalse();
        assertThat(resultado.getObservaciones()).isEqualTo("Cliente prefiere horario de la tarde");
    }

    @Test
    @DisplayName("CP-RSB-05: registrarSolicitudBusqueda registra la solicitud correctamente con observaciones nulas (camino feliz)")
    void registrar_debeAceptarObservacionesNulas() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaBusqueda = LocalDateTime.now().plusDays(1);
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(1L, fechaBusqueda, null);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L);
        stubDespachoYBarrilBloqueado(despacho);
        when(solicitudBusquedaRepository.save(any(SolicitudBusquedaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        SolicitudBusquedaResponseDTO resultado = solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getObservaciones()).isNull();
        assertThat(resultado.isBuscado()).isFalse();
    }

    @Test
    @DisplayName("CP-RSB-06: registrarSolicitudBusqueda lanza ReglaNegocioException y no consulta el despacho cuando las observaciones tienen 256 caracteres")
    void registrar_debeRechazarObservacionesDe256Caracteres() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(1L, LocalDateTime.now().plusDays(1), "a".repeat(256));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones no pueden superar 255 caracteres");

        verifyNoInteractions(despachoBarrilRepository, solicitudBusquedaRepository);
    }

    @Test
    @DisplayName("CP-RSB-07: registrarSolicitudBusqueda guarda las observaciones normalizadas (espacios extremos e internos)")
    void registrar_debeNormalizarLasObservaciones() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(1L, LocalDateTime.now().plusDays(1), "   Horario   de la tarde  ");
        stubDespachoYBarrilBloqueado(crearDespachoEntity(1L));
        when(solicitudBusquedaRepository.save(any(SolicitudBusquedaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        SolicitudBusquedaResponseDTO resultado = solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getObservaciones()).isEqualTo("Horario de la tarde");
    }

    @Test
    @DisplayName("CP-RSB-08: registrarSolicitudBusqueda guarda observaciones nulas cuando las observaciones están en blanco")
    void registrar_debeTratarObservacionesEnBlancoComoNulas() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(1L, LocalDateTime.now().plusDays(1), "    ");
        stubDespachoYBarrilBloqueado(crearDespachoEntity(1L));
        when(solicitudBusquedaRepository.save(any(SolicitudBusquedaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        SolicitudBusquedaResponseDTO resultado = solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getObservaciones()).isNull();
    }

    @Test
    @DisplayName("CP-RSB-09: registrarSolicitudBusqueda lanza ReglaNegocioException cuando el despacho no se encuentra en estado REGISTRADO")
    void registrar_debeRechazarDespachoAnulado() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(1L, LocalDateTime.now().plusDays(1), "Cliente prefiere horario de la tarde");
        DespachoBarrilEntity despacho = crearDespachoEntity(1L);
        despacho.setEstado(EstadoTransaccion.ANULADO);
        stubDespachoYBarrilBloqueado(despacho);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar una solicitud de búsqueda sobre un despacho en estado Registrado");

        verify(solicitudBusquedaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RSB-10: registrarSolicitudBusqueda lanza ReglaNegocioException cuando el barril ya no se encuentra DESPACHADO")
    void registrar_debeRechazarBarrilYaDevuelto() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(1L, LocalDateTime.now().plusDays(1), "Cliente prefiere horario de la tarde");
        DespachoBarrilEntity despacho = crearDespachoEntity(1L);
        despacho.getBarril().setEstadoOperativo(EstadoOperativoBarril.EN_LIMPIEZA);
        stubDespachoYBarrilBloqueado(despacho);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar una solicitud de búsqueda sobre un barril despachado");

        verify(solicitudBusquedaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RSB-11: registrarSolicitudBusqueda lanza RecursoNoEncontradoException con campo idBarril cuando el barril del despacho no existe o está dado de baja")
    void registrar_debeRechazarBarrilInexistente() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(1L, LocalDateTime.now().plusDays(1), "Cliente prefiere horario de la tarde");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(1L)).thenReturn(Optional.of(5L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(5L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 5")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verify(despachoBarrilRepository, never()).findById(any());
        verify(solicitudBusquedaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RSB-12: registrarSolicitudBusqueda bloquea el barril del despacho antes de leer el despacho y de validar los estados")
    void registrar_debeBloquearElBarrilAntesDeLeerElDespacho() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaFormDTO formDTO = solicitudBusquedaFormDTO(1L, LocalDateTime.now().plusDays(1), "Cliente prefiere horario de la tarde");
        DespachoBarrilEntity despacho = crearDespachoEntity(1L);
        stubDespachoYBarrilBloqueado(despacho);
        when(solicitudBusquedaRepository.save(any(SolicitudBusquedaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        solicitudBusquedaServicio.registrarSolicitudBusqueda(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(operacionCicloVidaBarrilRepository, barrilRepository, despachoBarrilRepository, solicitudBusquedaRepository);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdBarrilDeLaOperacion(1L);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        orden.verify(despachoBarrilRepository).findById(1L);
        orden.verify(solicitudBusquedaRepository).save(any(SolicitudBusquedaEntity.class));
    }

    // ==================== Helpers ====================

    private void stubDespachoYBarrilBloqueado(DespachoBarrilEntity despacho) {
        Long idBarril = despacho.getBarril().getId();
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(despacho.getId())).thenReturn(Optional.of(idBarril));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(idBarril)).thenReturn(Optional.of(despacho.getBarril()));
        when(despachoBarrilRepository.findById(despacho.getId())).thenReturn(Optional.of(despacho));
    }

    private static SolicitudBusquedaFormDTO solicitudBusquedaFormDTO(Long idDespachoBarril, LocalDateTime fechaBusqueda, String observaciones) {
        return SolicitudBusquedaFormDTO.builder()
                .idDespachoBarril(idDespachoBarril)
                .fechaBusqueda(fechaBusqueda)
                .observaciones(observaciones)
                .build();
    }

    private static SolicitudBusquedaEntity crearSolicitudEntity(Long id, LocalDateTime fechaBusqueda, String observaciones, boolean buscado, DespachoBarrilEntity despachoBarril) {
        return SolicitudBusquedaEntity.builder()
                .id(id)
                .fechaBusqueda(fechaBusqueda)
                .observaciones(observaciones)
                .buscado(buscado)
                .despachoBarril(despachoBarril)
                .build();
    }

    private static DespachoBarrilEntity crearDespachoEntity(Long id) {
        FabricanteBarrilEntity fabricante = FabricanteBarrilEntity.builder()
                .id(1L)
                .nombre("Fabricante Test SA")
                .estado(Estado.ACTIVO)
                .build();

        BarrilEntity barril = BarrilEntity.builder()
                .id(1L)
                .identificadorInterno("BAR-1")
                .capacidad(50.0)
                .contenidoActual(0.0)
                .estadoOperativo(EstadoOperativoBarril.DESPACHADO)
                .usosMaximosAntesMantenimiento(100)
                .estado(Estado.ACTIVO)
                .fabricante(fabricante)
                .build();

        ClienteEntity cliente = ClienteEntity.builder()
                .id(1L)
                .nombre("Cliente Test")
                .telefono("11-4455-6677")
                .email("cliente@test.com")
                .direccion("Calle Falsa 123")
                .estado(Estado.ACTIVO)
                .build();

        return DespachoBarrilEntity.builder()
                .id(id)
                .fecha(LocalDateTime.of(2026, 1, 10, 9, 0))
                .observaciones("Retiro programado")
                .fechaDevolucionEstimada(LocalDate.of(2026, 1, 20))
                .estado(EstadoTransaccion.REGISTRADO)
                .barril(barril)
                .cliente(cliente)
                .build();
    }
}
