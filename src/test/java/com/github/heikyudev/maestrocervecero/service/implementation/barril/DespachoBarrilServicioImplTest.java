package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDevolucionBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFallaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFraccionamientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ILimpiezaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IMantenimientoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cliente.IClienteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEnvasadoLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionDespachoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.DespachoBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.DespachoBarrilResponseDTO;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DespachoBarrilServicioImplTest {

    @Mock
    private IDespachoBarrilRepository despachoBarrilRepository;
    @Mock
    private IFallaBarrilRepository fallaBarrilRepository;
    @Mock
    private IMantenimientoBarrilRepository mantenimientoBarrilRepository;
    @Mock
    private ILimpiezaBarrilRepository limpiezaBarrilRepository;
    @Mock
    private IDevolucionBarrilRepository devolucionBarrilRepository;
    @Mock
    private IFraccionamientoBarrilRepository fraccionamientoBarrilRepository;
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;
    @Mock
    private IClienteRepository clienteRepository;
    @Mock
    private IBarrilRepository barrilRepository;

    @InjectMocks
    private DespachoBarrilServicioImpl despachoBarrilServicio;

    // ==================== filtrarDespachosBarril ====================

    @Test
    @DisplayName("CP-FDB-01: filtrarDespachosBarril filtra por los 5 criterios informados")
    void filtrar_debeFiltrarPorLosCincoCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 31, 23, 59);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        when(despachoBarrilRepository.filtrarDespachosBarril(EstadoTransaccion.REGISTRADO, 1L, 1L, desde, hasta, pageable))
                .thenReturn(new PageImpl<>(List.of(despacho)));

        // === EJECUCION ===
        Page<DespachoBarrilResponseDTO> resultado = despachoBarrilServicio.filtrarDespachosBarril(EstadoTransaccion.REGISTRADO, 1L, 1L, desde, hasta, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        verify(despachoBarrilRepository).filtrarDespachosBarril(EstadoTransaccion.REGISTRADO, 1L, 1L, desde, hasta, pageable);
    }

    @Test
    @DisplayName("CP-FDB-02: filtrarDespachosBarril con los 5 parámetros nulos no restringe la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarCincoParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity registrado = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        DespachoBarrilEntity anulado = crearDespachoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Despacho cargado por error", LocalDate.of(2026, 2, 2), barril, cliente);
        when(despachoBarrilRepository.filtrarDespachosBarril(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrado, anulado)));

        // === EJECUCION ===
        Page<DespachoBarrilResponseDTO> resultado = despachoBarrilServicio.filtrarDespachosBarril(null, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(despachoBarrilRepository).filtrarDespachosBarril(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FDB-03: filtrarDespachosBarril permite acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity anulado = crearDespachoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 21, 9, 0), "Despacho cargado por error", LocalDate.of(2026, 2, 2), barril, cliente);
        when(despachoBarrilRepository.filtrarDespachosBarril(EstadoTransaccion.ANULADO, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(anulado)));

        // === EJECUCION ===
        Page<DespachoBarrilResponseDTO> resultado = despachoBarrilServicio.filtrarDespachosBarril(EstadoTransaccion.ANULADO, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(despachoBarrilRepository).filtrarDespachosBarril(EstadoTransaccion.ANULADO, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FDB-04: filtrarDespachosBarril retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(despachoBarrilRepository.filtrarDespachosBarril(null, 99L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<DespachoBarrilResponseDTO> resultado = despachoBarrilServicio.filtrarDespachosBarril(null, 99L, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(despachoBarrilRepository).filtrarDespachosBarril(null, 99L, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FDB-05: filtrarDespachosBarril filtra por un barril específico")
    void filtrar_debeFiltrarPorBarrilEspecifico() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity despacho1 = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        DespachoBarrilEntity despacho2 = crearDespachoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 1, 9, 0), "Despacho cargado por error", LocalDate.of(2026, 2, 10), barril, cliente);
        when(despachoBarrilRepository.filtrarDespachosBarril(null, 1L, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(despacho1, despacho2)));

        // === EJECUCION ===
        Page<DespachoBarrilResponseDTO> resultado = despachoBarrilServicio.filtrarDespachosBarril(null, 1L, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(despachoBarrilRepository).filtrarDespachosBarril(null, 1L, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FDB-06: filtrarDespachosBarril filtra por un cliente específico")
    void filtrar_debeFiltrarPorClienteEspecifico() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity despacho1 = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        DespachoBarrilEntity despacho2 = crearDespachoEntity(2L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 2, 1, 9, 0), "Despacho cargado por error", LocalDate.of(2026, 2, 10), barril, cliente);
        when(despachoBarrilRepository.filtrarDespachosBarril(null, null, 1L, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(despacho1, despacho2)));

        // === EJECUCION ===
        Page<DespachoBarrilResponseDTO> resultado = despachoBarrilServicio.filtrarDespachosBarril(null, null, 1L, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(despachoBarrilRepository).filtrarDespachosBarril(null, null, 1L, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del despacho de barril cuando el ID existe")
    void buscarPorId_debeRetornarDespachoExistente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DESPACHADO);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        when(despachoBarrilRepository.findById(1L)).thenReturn(Optional.of(despacho));

        // === EJECUCION ===
        DespachoBarrilResponseDTO resultado = despachoBarrilServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(despachoBarrilRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(despachoBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoBarrilServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el despacho de barril con ID: 99");

        verify(despachoBarrilRepository).findById(99L);
    }

    // ==================== registrarDespachoBarril ====================

    @Test
    @DisplayName("CP-RDB-01: registrarDespachoBarril lanza ReglaNegocioException cuando la fecha de despacho es nula")
    void registrar_debeRechazarFechaDespachoNula() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, null, LocalDate.of(2026, 2, 1), "Retiro programado");

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de despacho es obligatoria");

        verifyNoInteractions(clienteRepository, barrilRepository, despachoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RDB-02: registrarDespachoBarril lanza ReglaNegocioException cuando la fecha estimada de devolución es nula")
    void registrar_debeRechazarFechaDevolucionEstimadaNula() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), null, "Retiro programado");

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha estimada de devolución es obligatoria");

        verifyNoInteractions(clienteRepository, barrilRepository, despachoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RDB-03: registrarDespachoBarril lanza ReglaNegocioException cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), null);

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(clienteRepository, barrilRepository, despachoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RDB-04: registrarDespachoBarril lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "   ");

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(clienteRepository, barrilRepository, despachoBarrilRepository);
    }

    @Test
    @DisplayName("CP-RDB-05: registrarDespachoBarril lanza RecursoNoEncontradoException cuando el cliente no existe o no está activo")
    void registrar_debeRechazarClienteInexistente() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 99L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el cliente con ID: 99");

        verifyNoInteractions(barrilRepository);
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-06: registrarDespachoBarril lanza RecursoNoEncontradoException cuando el barril no existe")
    void registrar_debeRechazarBarrilInexistente() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(99L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        ClienteEntity cliente = crearClienteEntity(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");

        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-07: registrarDespachoBarril lanza ReglaNegocioException cuando el barril no está en estado operativo CON_CERVEZA")
    void registrar_debeRechazarBarrilNoConCerveza() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        ClienteEntity cliente = crearClienteEntity(1L);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar un despacho sobre un barril en estado operativo CON_CERVEZA");

        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-08: registrarDespachoBarril lanza ReglaNegocioException cuando la fecha estimada de devolución es anterior a la fecha de despacho")
    void registrar_debeRechazarFechaDevolucionEstimadaAnterior() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 1, 19), "Retiro programado");
        ClienteEntity cliente = crearClienteEntity(1L);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha estimada de devolución debe ser posterior o igual a la fecha de despacho");

        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-09: registrarDespachoBarril lanza ReglaNegocioException cuando existe un despacho posterior del mismo barril")
    void registrar_debeRechazarSiExisteDespachoPosterior() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        ClienteEntity cliente = crearClienteEntity(1L);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-10: registrarDespachoBarril lanza ReglaNegocioException cuando existe una limpieza posterior del mismo barril")
    void registrar_debeRechazarSiExisteLimpiezaPosterior() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        ClienteEntity cliente = crearClienteEntity(1L);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-11: registrarDespachoBarril lanza ReglaNegocioException cuando existe un envasado posterior del mismo barril")
    void registrar_debeRechazarSiExisteEnvasadoPosterior() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        ClienteEntity cliente = crearClienteEntity(1L);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-12: registrarDespachoBarril registra el despacho y pasa el barril a DESPACHADO (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaDespacho = LocalDateTime.of(2026, 1, 20, 9, 0);
        LocalDate fechaDevolucionEstimada = LocalDate.of(2026, 2, 1);
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, fechaDespacho, fechaDevolucionEstimada, "Retiro programado en depósito del cliente");
        ClienteEntity cliente = crearClienteEntity(1L);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(1L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(1L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(1L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(1L)).thenReturn(Optional.empty());
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(despachoBarrilRepository.save(any(DespachoBarrilEntity.class))).thenAnswer(invocation -> {
            DespachoBarrilEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        DespachoBarrilResponseDTO resultado = despachoBarrilServicio.registrarDespachoBarril(formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DESPACHADO);
        verify(barrilRepository).save(barril);

        ArgumentCaptor<DespachoBarrilEntity> captor = ArgumentCaptor.forClass(DespachoBarrilEntity.class);
        verify(despachoBarrilRepository).save(captor.capture());
        DespachoBarrilEntity despachoGuardado = captor.getValue();
        assertThat(despachoGuardado.getFecha()).isEqualTo(fechaDespacho);
        assertThat(despachoGuardado.getObservaciones()).isEqualTo("Retiro programado en depósito del cliente");
        assertThat(despachoGuardado.getFechaDevolucionEstimada()).isEqualTo(fechaDevolucionEstimada);
        assertThat(despachoGuardado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(despachoGuardado.getBarril()).isSameAs(barril);
        assertThat(despachoGuardado.getCliente()).isSameAs(cliente);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RDB-13: registrarDespachoBarril lanza ReglaNegocioException cuando existe un fraccionamiento posterior del mismo barril")
    void registrar_debeRechazarSiExisteFraccionamientoPosterior() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        ClienteEntity cliente = crearClienteEntity(1L);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(1L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(1L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    // ==================== anularDespachoBarril ====================

    @Test
    @DisplayName("CP-ADB-01: anularDespachoBarril lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(despachoBarrilRepository, fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, devolucionBarrilRepository, fraccionamientoBarrilRepository, barrilRepository);
    }

    @Test
    @DisplayName("CP-ADB-02: anularDespachoBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(despachoBarrilRepository, fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, devolucionBarrilRepository, fraccionamientoBarrilRepository, barrilRepository);
    }

    @Test
    @DisplayName("CP-ADB-03: anularDespachoBarril lanza RecursoNoEncontradoException cuando el despacho no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        when(despachoBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el despacho de barril con ID: 99");

        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-04: anularDespachoBarril lanza ReglaNegocioException cuando el despacho no se encuentra en estado REGISTRADO")
    void anular_debeRechazarDespachoNoRegistrado() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        when(despachoBarrilRepository.findById(1L)).thenReturn(Optional.of(despacho));

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular despachos de barril en estado REGISTRADO");

        verifyNoInteractions(fallaBarrilRepository, mantenimientoBarrilRepository, limpiezaBarrilRepository, devolucionBarrilRepository, fraccionamientoBarrilRepository, barrilRepository);
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-05: anularDespachoBarril lanza ReglaNegocioException cuando existe un despacho posterior del mismo tipo sobre el barril")
    void anular_debeRechazarSiExisteDespachoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        when(despachoBarrilRepository.findById(1L)).thenReturn(Optional.of(despacho));
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-06: anularDespachoBarril lanza ReglaNegocioException cuando existe una operación posterior de otro tipo sobre el barril")
    void anular_debeRechazarSiExisteOperacionPosteriorDeOtroTipo() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        when(despachoBarrilRepository.findById(1L)).thenReturn(Optional.of(despacho));
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-07: anularDespachoBarril lanza RecursoNoEncontradoException cuando el barril asociado no existe")
    void anular_debeRechazarBarrilAsociadoInexistente() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        when(despachoBarrilRepository.findById(1L)).thenReturn(Optional.of(despacho));
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 2");

        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-08: anularDespachoBarril lanza ReglaNegocioException cuando el barril asociado no está en estado operativo DESPACHADO")
    void anular_debeRechazarBarrilNoDespachado() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        when(despachoBarrilRepository.findById(1L)).thenReturn(Optional.of(despacho));
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular un despacho cuyo barril asociado se encuentre en estado operativo DESPACHADO");

        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-09: anularDespachoBarril anula el despacho y restablece el barril a CON_CERVEZA (camino feliz)")
    void anular_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        when(despachoBarrilRepository.findById(1L)).thenReturn(Optional.of(despacho));
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.empty());
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(despachoBarrilRepository.save(despacho)).thenReturn(despacho);

        // === EJECUCION ===
        DespachoBarrilResponseDTO resultado = despachoBarrilServicio.anularDespachoBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
        assertThat(despacho.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(despacho.getFechaAnulacion()).isNotNull();
        assertThat(despacho.getMotivoAnulacion()).isEqualTo("Despacho cargado por error");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        verify(barrilRepository).save(barril);
        verify(despachoBarrilRepository).save(despacho);
    }

    @Test
    @DisplayName("CP-ADB-10: anularDespachoBarril lanza ReglaNegocioException cuando existe un fraccionamiento posterior sobre el barril")
    void anular_debeRechazarSiExisteFraccionamientoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        ClienteEntity cliente = crearClienteEntity(1L);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, cliente);
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        when(despachoBarrilRepository.findById(1L)).thenReturn(Optional.of(despacho));
        when(despachoBarrilRepository.buscarFechaUltimoDespachoRegistrado(2L)).thenReturn(Optional.empty());
        when(fallaBarrilRepository.buscarFechaUltimaFallaRegistrada(2L)).thenReturn(Optional.empty());
        when(mantenimientoBarrilRepository.buscarFechaUltimoMantenimientoRegistrado(2L)).thenReturn(Optional.empty());
        when(limpiezaBarrilRepository.buscarFechaUltimaLimpiezaRegistrada(2L)).thenReturn(Optional.empty());
        when(devolucionBarrilRepository.buscarFechaUltimaDevolucionRegistrada(2L)).thenReturn(Optional.empty());
        when(fraccionamientoBarrilRepository.buscarFechaUltimoFraccionamientoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la operación más reciente registrada sobre este barril");

        verifyNoInteractions(barrilRepository);
        verify(despachoBarrilRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private static DespachoBarrilFormDTO despachoBarrilFormDTO(Long idBarril, Long idCliente, LocalDateTime fechaDespacho, LocalDate fechaDevolucionEstimada, String observaciones) {
        return DespachoBarrilFormDTO.builder()
                .idBarril(idBarril)
                .idCliente(idCliente)
                .fechaDespacho(fechaDespacho)
                .fechaDevolucionEstimada(fechaDevolucionEstimada)
                .observaciones(observaciones)
                .build();
    }

    private static AnulacionDespachoBarrilFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionDespachoBarrilFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static BarrilEntity crearBarrilEntity(Long id, EstadoOperativoBarril estadoOperativo) {
        FabricanteBarrilEntity fabricante = FabricanteBarrilEntity.builder()
                .id(1L)
                .razonSocial("Fabricante Test SA")
                .nombreComercial("Fabricante Test")
                .cuit("30-12345678-9")
                .telefono("11-2233-4455")
                .email("contacto@fabricante.com")
                .estado(Estado.ACTIVO)
                .build();

        return BarrilEntity.builder()
                .id(id)
                .identificador("BAR-" + id)
                .capacidad(50.0)
                .contenidoActual(50.0)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(100)
                .estado(Estado.ACTIVO)
                .fabricante(fabricante)
                .build();
    }

    private static ClienteEntity crearClienteEntity(Long id) {
        return ClienteEntity.builder()
                .id(id)
                .nombre("Cliente Test")
                .telefono("11-4455-6677")
                .email("cliente@test.com")
                .direccion("Calle Falsa 123")
                .estado(Estado.ACTIVO)
                .build();
    }

    private static DespachoBarrilEntity crearDespachoEntity(Long id, EstadoTransaccion estado, LocalDateTime fechaDespacho, String observaciones, LocalDate fechaDevolucionEstimada, BarrilEntity barril, ClienteEntity cliente) {
        return DespachoBarrilEntity.builder()
                .id(id)
                .fecha(fechaDespacho)
                .observaciones(observaciones)
                .fechaDevolucionEstimada(fechaDevolucionEstimada)
                .estado(estado)
                .barril(barril)
                .cliente(cliente)
                .build();
    }
}
