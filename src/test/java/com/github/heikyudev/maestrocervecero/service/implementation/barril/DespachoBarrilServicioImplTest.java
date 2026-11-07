package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.DespachoBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IOperacionCicloVidaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
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
class DespachoBarrilServicioImplTest {

    @Mock
    private IDespachoBarrilRepository despachoBarrilRepository;
    @Mock
    private IEnvasadoLoteRepository envasadoLoteRepository;
    @Mock
    private IClienteRepository clienteRepository;
    @Mock
    private IOperacionCicloVidaBarrilRepository operacionCicloVidaBarrilRepository;
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
    @DisplayName("CP-RDB-01: registrarDespachoBarril lanza ReglaNegocioException con campo fecha cuando la fecha de despacho es nula")
    void registrar_debeRechazarFechaDespachoNula() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, null, LocalDate.of(2026, 2, 1), "Retiro programado");

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de despacho es obligatoria")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(clienteRepository, barrilRepository, despachoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RDB-02: registrarDespachoBarril lanza ReglaNegocioException con campo fechaDevolucionEstimada cuando la fecha estimada de devolución es nula")
    void registrar_debeRechazarFechaEstimadaNula() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), null, "Retiro programado");

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha estimada de devolución es obligatoria")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fechaDevolucionEstimada"));

        verifyNoInteractions(clienteRepository, barrilRepository, despachoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RDB-03: registrarDespachoBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones son nulas")
    void registrar_debeRechazarObservacionesNulas() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), null);

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(clienteRepository, barrilRepository, despachoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RDB-04: registrarDespachoBarril lanza ReglaNegocioException cuando las observaciones están en blanco")
    void registrar_debeRechazarObservacionesEnBlanco() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "   ");

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones son obligatorias");

        verifyNoInteractions(clienteRepository, barrilRepository, despachoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RDB-05: registrarDespachoBarril lanza ReglaNegocioException con campo observaciones cuando las observaciones tienen 256 caracteres")
    void registrar_debeRechazarObservacionesDe256Caracteres() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "a".repeat(256));

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Las observaciones no pueden superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("observaciones"));

        verifyNoInteractions(clienteRepository, barrilRepository, despachoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RDB-06: registrarDespachoBarril lanza RecursoNoEncontradoException con campo idCliente cuando el cliente no existe o no está activo")
    void registrar_debeRechazarClienteInexistente() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 99L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el cliente con ID: 99")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idCliente"));

        verifyNoInteractions(barrilRepository);
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-07: registrarDespachoBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril no existe")
    void registrar_debeRechazarBarrilInexistente() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(99L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(crearClienteEntity(1L)));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-08: registrarDespachoBarril lanza ReglaNegocioException con campo idBarril cuando el barril no está en estado operativo CON_CERVEZA")
    void registrar_debeRechazarBarrilNoConCerveza() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.DISPONIBLE);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(crearClienteEntity(1L)));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar un despacho sobre un barril en estado operativo Con cerveza")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-09: registrarDespachoBarril lanza ReglaNegocioException con campo fechaDevolucionEstimada cuando la fecha estimada de devolución es anterior a la fecha de despacho")
    void registrar_debeRechazarFechaEstimadaAnteriorAlDespacho() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 1, 19), "Retiro programado");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(crearClienteEntity(1L)));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha estimada de devolución debe ser posterior o igual a la fecha de despacho")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fechaDevolucionEstimada"));

        verifyNoInteractions(operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-10: registrarDespachoBarril lanza ReglaNegocioException con campo fecha cuando existe un envasado posterior del mismo barril")
    void registrar_debeRechazarSiExisteEnvasadoPosterior() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(crearClienteEntity(1L)));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 22, 9, 0)));

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-11: registrarDespachoBarril lanza ReglaNegocioException con campo fecha cuando existe una operación registrada posterior de cualquier tipo")
    void registrar_debeRechazarSiExisteOperacionPosterior() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(crearClienteEntity(1L)));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 21, 9, 0)));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-12: registrarDespachoBarril lanza ReglaNegocioException cuando la fecha coincide exactamente con la del último envasado")
    void registrar_debeRechazarFechaIgualALaDelUltimoEnvasado() {
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 20, 9, 0);
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, fecha, LocalDate.of(2026, 2, 1), "Retiro programado");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(crearClienteEntity(1L)));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.of(fecha));

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación debe ser posterior a la última operación registrada sobre este barril");

        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RDB-13: registrarDespachoBarril lanza ReglaNegocioException con campo fecha cuando la fecha de despacho es posterior a la fecha y hora actual")
    void registrar_debeRechazarFechaDespachoFutura() {
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.now().plusDays(1), LocalDate.now().plusDays(10), "Retiro programado");

        assertThatThrownBy(() -> despachoBarrilServicio.registrarDespachoBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La fecha de esta operación no puede ser posterior a la fecha y hora actual")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("fecha"));

        verifyNoInteractions(clienteRepository, barrilRepository, despachoBarrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-RDB-14: registrarDespachoBarril registra el despacho y pasa el barril a DESPACHADO (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fechaDespacho = LocalDateTime.of(2026, 1, 20, 9, 0);
        LocalDate fechaDevolucionEstimada = LocalDate.of(2026, 2, 1);
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, fechaDespacho, fechaDevolucionEstimada, "Retiro programado en depósito del cliente");
        ClienteEntity cliente = crearClienteEntity(1L);
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(operacionCicloVidaBarrilRepository.buscarFechaUltimaOperacionRegistrada(1L)).thenReturn(Optional.empty());
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(1L)).thenReturn(Optional.empty());
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
    @DisplayName("CP-RDB-15: registrarDespachoBarril bloquea el barril antes de leer la última operación y el último envasado")
    void registrar_debeBloquearElBarrilAntesDeLeerLaActividad() {
        // === PREPARACION DE DATOS ===
        DespachoBarrilFormDTO formDTO = despachoBarrilFormDTO(1L, 1L, LocalDateTime.of(2026, 1, 20, 9, 0), LocalDate.of(2026, 2, 1), "Retiro programado");
        BarrilEntity barril = crearBarrilEntity(1L, EstadoOperativoBarril.CON_CERVEZA);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(crearClienteEntity(1L)));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(1L)).thenReturn(Optional.of(barril));
        when(despachoBarrilRepository.save(any(DespachoBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        despachoBarrilServicio.registrarDespachoBarril(formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarFechaUltimaOperacionRegistrada(1L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(1L);
    }

    // ==================== anularDespachoBarril ====================

    @Test
    @DisplayName("CP-ADB-01: anularDespachoBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(despachoBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-ADB-02: anularDespachoBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(despachoBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-ADB-03: anularDespachoBarril lanza ReglaNegocioException con campo motivoAnulacion cuando el motivo de anulación tiene 256 caracteres")
    void anular_debeRechazarMotivoDe256Caracteres() {
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("a".repeat(256));

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación no puede superar 255 caracteres")
                .satisfies(e -> assertThat(((ReglaNegocioException) e).getCampo()).isEqualTo("motivoAnulacion"));

        verifyNoInteractions(despachoBarrilRepository, barrilRepository, operacionCicloVidaBarrilRepository, envasadoLoteRepository);
    }

    @Test
    @DisplayName("CP-ADB-04: anularDespachoBarril lanza RecursoNoEncontradoException sin bloquear ningún barril cuando el despacho no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el despacho de barril con ID: 99");

        verifyNoInteractions(barrilRepository, envasadoLoteRepository);
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-05: anularDespachoBarril lanza RecursoNoEncontradoException con campo idBarril cuando el barril asociado no existe")
    void anular_debeRechazarBarrilAsociadoInexistente() {
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(1L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 2")
                .satisfies(e -> assertThat(((RecursoNoEncontradoException) e).getCampo()).isEqualTo("idBarril"));

        verifyNoInteractions(envasadoLoteRepository);
        verify(despachoBarrilRepository, never()).findById(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-06: anularDespachoBarril lanza RecursoNoEncontradoException cuando el ID corresponde a una operación de otro tipo")
    void anular_debeRechazarIdDeOperacionDeOtroTipo() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(7L)).thenReturn(Optional.of(2L));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(2L)).thenReturn(Optional.of(barril));
        when(despachoBarrilRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(7L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el despacho de barril con ID: 7");

        // El ID 7 es de otra operación del barril (por ejemplo una falla): no se lee la última operación ni el envasado
        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-07: anularDespachoBarril lanza ReglaNegocioException cuando el despacho no se encuentra en estado REGISTRADO")
    void anular_debeRechazarDespachoNoRegistrado() {
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.ANULADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, crearClienteEntity(1L));
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        stubBarrilBloqueadoYDespacho(barril, despacho);

        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular despachos de barril en estado Registrado");

        verifyNoInteractions(envasadoLoteRepository);
        verify(operacionCicloVidaBarrilRepository, never()).buscarIdUltimaOperacionRegistrada(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-08: anularDespachoBarril lanza ReglaNegocioException cuando la última operación registrada del barril es otra")
    void anular_debeRechazarSiLaUltimaOperacionEsOtra() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, crearClienteEntity(1L));
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        stubBarrilBloqueadoYDespacho(barril, despacho);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(5L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-09: anularDespachoBarril lanza ReglaNegocioException ante un empate de fecha cuando la última es la de mayor ID")
    void anular_debeRechazarEnEmpateDeFechaSiLaUltimaEsLaDeMayorId() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        DespachoBarrilEntity despacho = crearDespachoEntity(7L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, crearClienteEntity(1L));
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        stubBarrilBloqueadoYDespacho(barril, despacho);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(8L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(7L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular la última operación registrada del barril; anule primero las operaciones posteriores");

        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-10: anularDespachoBarril lanza ReglaNegocioException sin modificar nada cuando el barril tuvo un envasado posterior al despacho")
    void anular_debeRechazarSiElBarrilTuvoUnEnvasadoPosterior() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, crearClienteEntity(1L));
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        stubBarrilBloqueadoYDespacho(barril, despacho);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 25, 9, 0)));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DESPACHADO);
        assertThat(despacho.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-11: anularDespachoBarril lanza ReglaNegocioException cuando la fecha del último envasado coincide con la del despacho")
    void anular_debeRechazarSiElEnvasadoCoincideConLaFechaDelDespacho() {
        // === PREPARACION DE DATOS ===
        LocalDateTime fecha = LocalDateTime.of(2026, 1, 20, 9, 0);
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, fecha, "Retiro programado", LocalDate.of(2026, 2, 1), barril, crearClienteEntity(1L));
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        stubBarrilBloqueadoYDespacho(barril, despacho);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(fecha));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril tuvo un envasado posterior");

        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-12: anularDespachoBarril lanza ReglaNegocioException por el uso, sin consultar el último envasado, cuando el barril está en uso por un lote")
    void anular_debeRechazarSiElBarrilEstaEnUso() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.EN_USO);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, crearClienteEntity(1L));
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        stubBarrilBloqueadoYDespacho(barril, despacho);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular la operación porque el barril está en uso por un lote");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-13: anularDespachoBarril lanza ReglaNegocioException cuando el barril asociado no está en estado operativo DESPACHADO")
    void anular_debeRechazarBarrilNoDespachado() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.CON_CERVEZA);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, crearClienteEntity(1L));
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        stubBarrilBloqueadoYDespacho(barril, despacho);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede anular un despacho cuyo barril asociado se encuentre en estado operativo Despachado");

        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ADB-14: anularDespachoBarril anula el despacho y restablece el barril a CON_CERVEZA (camino feliz)")
    void anular_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, crearClienteEntity(1L));
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        stubBarrilBloqueadoYDespacho(barril, despacho);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
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
    @DisplayName("CP-ADB-15: anularDespachoBarril anula el despacho cuando es posterior al último envasado del barril (camino feliz)")
    void anular_debeAnularCuandoEsPosteriorAlUltimoEnvasado() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, crearClienteEntity(1L));
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        stubBarrilBloqueadoYDespacho(barril, despacho);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.of(1L));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(2L)).thenReturn(Optional.of(LocalDateTime.of(2026, 1, 15, 9, 0)));
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(despachoBarrilRepository.save(despacho)).thenReturn(despacho);

        // === EJECUCION ===
        DespachoBarrilResponseDTO resultado = despachoBarrilServicio.anularDespachoBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(barril.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.CON_CERVEZA);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    @Test
    @DisplayName("CP-ADB-16: anularDespachoBarril valida en orden: bloqueo del barril, lectura del despacho, última operación, último envasado")
    void anular_debeValidarEnOrden() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, crearClienteEntity(1L));
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        stubBarrilBloqueadoYDespacho(barril, despacho);
        stubSinOperacionesPosterioresNiEnvasados(1L, 2L);
        when(barrilRepository.save(barril)).thenReturn(barril);
        when(despachoBarrilRepository.save(despacho)).thenReturn(despacho);

        // === EJECUCION ===
        despachoBarrilServicio.anularDespachoBarril(1L, formDTO);

        // === ASSERTS ===
        InOrder orden = inOrder(operacionCicloVidaBarrilRepository, barrilRepository, despachoBarrilRepository, envasadoLoteRepository);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdBarrilDeLaOperacion(1L);
        orden.verify(barrilRepository).buscarPorIdParaCambiarEstadoOperativo(2L);
        orden.verify(despachoBarrilRepository).findById(1L);
        orden.verify(operacionCicloVidaBarrilRepository).buscarIdUltimaOperacionRegistrada(2L);
        orden.verify(envasadoLoteRepository).buscarFechaUltimoEnvasadoRegistrado(2L);
        orden.verify(barrilRepository).save(barril);
    }

    @Test
    @DisplayName("CP-ADB-17: anularDespachoBarril lanza IllegalStateException sin modificar nada cuando no se encuentra ninguna operación registrada del barril")
    void anular_debeFallarCerradoSiNoHayUltimaOperacion() {
        // === PREPARACION DE DATOS ===
        BarrilEntity barril = crearBarrilEntity(2L, EstadoOperativoBarril.DESPACHADO);
        DespachoBarrilEntity despacho = crearDespachoEntity(1L, EstadoTransaccion.REGISTRADO, LocalDateTime.of(2026, 1, 20, 9, 0), "Retiro programado", LocalDate.of(2026, 2, 1), barril, crearClienteEntity(1L));
        AnulacionDespachoBarrilFormDTO formDTO = anulacionFormDTO("Despacho cargado por error");
        stubBarrilBloqueadoYDespacho(barril, despacho);
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(2L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> despachoBarrilServicio.anularDespachoBarril(1L, formDTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se encontró ninguna operación registrada para el barril, pero se intenta anular la operación con ID: 1");

        verifyNoInteractions(envasadoLoteRepository);
        verify(barrilRepository, never()).save(any());
        verify(despachoBarrilRepository, never()).save(any());
    }

    // ==================== helpers de construcción ====================

    private void stubBarrilBloqueadoYDespacho(BarrilEntity barril, DespachoBarrilEntity despacho) {
        when(operacionCicloVidaBarrilRepository.buscarIdBarrilDeLaOperacion(despacho.getId())).thenReturn(Optional.of(barril.getId()));
        when(barrilRepository.buscarPorIdParaCambiarEstadoOperativo(barril.getId())).thenReturn(Optional.of(barril));
        when(despachoBarrilRepository.findById(despacho.getId())).thenReturn(Optional.of(despacho));
    }

    private void stubSinOperacionesPosterioresNiEnvasados(Long idDespacho, Long idBarril) {
        when(operacionCicloVidaBarrilRepository.buscarIdUltimaOperacionRegistrada(idBarril)).thenReturn(Optional.of(idDespacho));
        when(envasadoLoteRepository.buscarFechaUltimoEnvasadoRegistrado(idBarril)).thenReturn(Optional.empty());
    }

    private static DespachoBarrilFormDTO despachoBarrilFormDTO(Long idBarril, Long idCliente, LocalDateTime fecha, LocalDate fechaDevolucionEstimada, String observaciones) {
        return DespachoBarrilFormDTO.builder()
                .idBarril(idBarril)
                .idCliente(idCliente)
                .fecha(fecha)
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
                .nombre("Fabricante Test SA")
                .estado(Estado.ACTIVO)
                .build();

        return BarrilEntity.builder()
                .id(id)
                .identificadorInterno("BAR-" + id)
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
