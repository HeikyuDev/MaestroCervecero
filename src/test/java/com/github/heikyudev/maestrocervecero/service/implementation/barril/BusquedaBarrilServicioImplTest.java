package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BusquedaBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.SolicitudBusquedaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBusquedaBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.ISolicitudBusquedaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.AnulacionBusquedaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BusquedaBarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BusquedaBarrilResponseDTO;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusquedaBarrilServicioImplTest {

    @Mock
    private IBusquedaBarrilRepository busquedaBarrilRepository;
    @Mock
    private ISolicitudBusquedaRepository solicitudBusquedaRepository;

    @InjectMocks
    private BusquedaBarrilServicioImpl busquedaBarrilServicio;

    // ==================== filtrarBusquedasBarril ====================

    @Test
    @DisplayName("CP-FBB-01: filtrarBusquedasBarril filtra por los 2 criterios informados")
    void filtrar_debeFiltrarPorLosDosCriterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, false);
        BusquedaBarrilEntity busqueda = crearBusquedaEntity(1L, EstadoTransaccion.REGISTRADO, solicitud);
        when(busquedaBarrilRepository.filtrarBusquedasBarril(1L, EstadoTransaccion.REGISTRADO, pageable))
                .thenReturn(new PageImpl<>(List.of(busqueda), pageable, 1));

        // === EJECUCION ===
        Page<BusquedaBarrilResponseDTO> resultado = busquedaBarrilServicio.filtrarBusquedasBarril(1L, EstadoTransaccion.REGISTRADO, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("CP-FBB-02: Los 2 parámetros nulos no restringen la búsqueda, incluyendo mezcla de estados")
    void filtrar_debePropagarParametrosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, true);
        BusquedaBarrilEntity registrada = crearBusquedaEntity(1L, EstadoTransaccion.REGISTRADO, solicitud);
        BusquedaBarrilEntity anulada = crearBusquedaEntity(2L, EstadoTransaccion.ANULADO, solicitud);
        when(busquedaBarrilRepository.filtrarBusquedasBarril(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(registrada, anulada), pageable, 2));

        // === EJECUCION ===
        Page<BusquedaBarrilResponseDTO> resultado = busquedaBarrilServicio.filtrarBusquedasBarril(null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("CP-FBB-03: El usuario puede acotar explícitamente a un solo estado")
    void filtrar_debeAcotarPorEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, false);
        BusquedaBarrilEntity anulada = crearBusquedaEntity(2L, EstadoTransaccion.ANULADO, solicitud);
        when(busquedaBarrilRepository.filtrarBusquedasBarril(null, EstadoTransaccion.ANULADO, pageable))
                .thenReturn(new PageImpl<>(List.of(anulada), pageable, 1));

        // === EJECUCION ===
        Page<BusquedaBarrilResponseDTO> resultado = busquedaBarrilServicio.filtrarBusquedasBarril(null, EstadoTransaccion.ANULADO, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    @Test
    @DisplayName("CP-FBB-04: filtrarBusquedasBarril sin coincidencias")
    void filtrar_debeRetornarPaginaVacia() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(busquedaBarrilRepository.filtrarBusquedasBarril(99L, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<BusquedaBarrilResponseDTO> resultado = busquedaBarrilServicio.filtrarBusquedasBarril(99L, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
    }

    @Test
    @DisplayName("CP-FBB-05: Filtra por una solicitud de búsqueda específica")
    void filtrar_debeFiltrarPorSolicitudEspecifica() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, true);
        BusquedaBarrilEntity busqueda = crearBusquedaEntity(1L, EstadoTransaccion.REGISTRADO, solicitud);
        when(busquedaBarrilRepository.filtrarBusquedasBarril(1L, null, pageable))
                .thenReturn(new PageImpl<>(List.of(busqueda), pageable, 1));

        // === EJECUCION ===
        Page<BusquedaBarrilResponseDTO> resultado = busquedaBarrilServicio.filtrarBusquedasBarril(1L, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(1);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna la búsqueda de barril encontrada")
    void buscarPorId_debeRetornarBusquedaEncontrada() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, true);
        BusquedaBarrilEntity busqueda = crearBusquedaEntity(1L, EstadoTransaccion.REGISTRADO, solicitud);
        when(busquedaBarrilRepository.findById(1L)).thenReturn(Optional.of(busqueda));

        // === EJECUCION ===
        BusquedaBarrilResponseDTO resultado = busquedaBarrilServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // === PREPARACION DE DATOS ===
        when(busquedaBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> busquedaBarrilServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la búsqueda de barril con ID: 99");
    }

    // ==================== registrarBusquedaBarril ====================

    @Test
    @DisplayName("CP-RBB-01: registrarBusquedaBarril lanza RecursoNoEncontradoException cuando la solicitud de búsqueda no existe")
    void registrar_debeRechazarSolicitudInexistente() {
        // === PREPARACION DE DATOS ===
        BusquedaBarrilFormDTO formDTO = busquedaBarrilFormDTO(99L);
        when(solicitudBusquedaRepository.findById(99L)).thenReturn(Optional.empty());

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> busquedaBarrilServicio.registrarBusquedaBarril(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la solicitud de búsqueda con ID: 99");

        verify(busquedaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RBB-02: registrarBusquedaBarril lanza ReglaNegocioException cuando la solicitud ya fue buscada")
    void registrar_debeRechazarSolicitudYaBuscada() {
        // === PREPARACION DE DATOS ===
        BusquedaBarrilFormDTO formDTO = busquedaBarrilFormDTO(1L);
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, true);
        when(solicitudBusquedaRepository.findById(1L)).thenReturn(Optional.of(solicitud));

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> busquedaBarrilServicio.registrarBusquedaBarril(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se puede registrar una búsqueda sobre una solicitud de búsqueda que todavía no fue buscada");

        verify(solicitudBusquedaRepository, never()).save(any());
        verify(busquedaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-RBB-03: registrarBusquedaBarril registra la búsqueda y marca la solicitud como buscada (camino feliz)")
    void registrar_debeRegistrarCorrectamente() {
        // === PREPARACION DE DATOS ===
        BusquedaBarrilFormDTO formDTO = busquedaBarrilFormDTO(1L);
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, false);
        when(solicitudBusquedaRepository.findById(1L)).thenReturn(Optional.of(solicitud));
        when(busquedaBarrilRepository.save(any(BusquedaBarrilEntity.class))).thenAnswer(invocation -> {
            BusquedaBarrilEntity entidad = invocation.getArgument(0);
            entidad.setId(1L);
            return entidad;
        });

        // === EJECUCION ===
        BusquedaBarrilResponseDTO resultado = busquedaBarrilServicio.registrarBusquedaBarril(formDTO);

        // === ASSERTS ===
        assertThat(solicitud.isBuscado()).isTrue();
        verify(solicitudBusquedaRepository).save(solicitud);

        ArgumentCaptor<BusquedaBarrilEntity> captor = ArgumentCaptor.forClass(BusquedaBarrilEntity.class);
        verify(busquedaBarrilRepository).save(captor.capture());
        BusquedaBarrilEntity busquedaGuardada = captor.getValue();
        assertThat(busquedaGuardada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        assertThat(busquedaGuardada.getSolicitudBusqueda()).isSameAs(solicitud);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    // ==================== anularBusquedaBarril ====================

    @Test
    @DisplayName("CP-ABB-01: anularBusquedaBarril lanza ReglaNegocioException cuando el motivo de anulación es nulo")
    void anular_debeRechazarMotivoNulo() {
        AnulacionBusquedaBarrilFormDTO formDTO = anulacionFormDTO(null);

        assertThatThrownBy(() -> busquedaBarrilServicio.anularBusquedaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(busquedaBarrilRepository, solicitudBusquedaRepository);
    }

    @Test
    @DisplayName("CP-ABB-02: anularBusquedaBarril lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anular_debeRechazarMotivoEnBlanco() {
        AnulacionBusquedaBarrilFormDTO formDTO = anulacionFormDTO("   ");

        assertThatThrownBy(() -> busquedaBarrilServicio.anularBusquedaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(busquedaBarrilRepository, solicitudBusquedaRepository);
    }

    @Test
    @DisplayName("CP-ABB-03: anularBusquedaBarril lanza RecursoNoEncontradoException cuando la búsqueda no existe")
    void anular_debeLanzarExcepcionSiNoExiste() {
        AnulacionBusquedaBarrilFormDTO formDTO = anulacionFormDTO("Búsqueda cargada por error");
        when(busquedaBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> busquedaBarrilServicio.anularBusquedaBarril(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la búsqueda de barril con ID: 99");

        verify(busquedaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ABB-04: anularBusquedaBarril lanza ReglaNegocioException cuando la búsqueda no se encuentra en estado REGISTRADO")
    void anular_debeRechazarBusquedaNoRegistrada() {
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, false);
        BusquedaBarrilEntity busqueda = crearBusquedaEntity(1L, EstadoTransaccion.ANULADO, solicitud);
        AnulacionBusquedaBarrilFormDTO formDTO = anulacionFormDTO("Búsqueda cargada por error");
        when(busquedaBarrilRepository.findById(1L)).thenReturn(Optional.of(busqueda));

        assertThatThrownBy(() -> busquedaBarrilServicio.anularBusquedaBarril(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular búsquedas de barril en estado REGISTRADO");

        verifyNoInteractions(solicitudBusquedaRepository);
        verify(busquedaBarrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ABB-05: anularBusquedaBarril anula correctamente y restablece la solicitud a no buscada (camino feliz)")
    void anular_debeAnularCorrectamente() {
        // === PREPARACION DE DATOS ===
        SolicitudBusquedaEntity solicitud = crearSolicitudEntity(1L, true);
        BusquedaBarrilEntity busqueda = crearBusquedaEntity(1L, EstadoTransaccion.REGISTRADO, solicitud);
        AnulacionBusquedaBarrilFormDTO formDTO = anulacionFormDTO("Búsqueda cargada por error");
        when(busquedaBarrilRepository.findById(1L)).thenReturn(Optional.of(busqueda));
        when(busquedaBarrilRepository.save(any(BusquedaBarrilEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        BusquedaBarrilResponseDTO resultado = busquedaBarrilServicio.anularBusquedaBarril(1L, formDTO);

        // === ASSERTS ===
        assertThat(solicitud.isBuscado()).isFalse();
        verify(solicitudBusquedaRepository).save(solicitud);

        assertThat(busqueda.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(busqueda.getMotivoAnulacion()).isEqualTo("Búsqueda cargada por error");
        assertThat(busqueda.getFechaAnulacion()).isNotNull();

        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    // ==================== Helpers ====================

    private static BusquedaBarrilFormDTO busquedaBarrilFormDTO(Long idSolicitudBusqueda) {
        return BusquedaBarrilFormDTO.builder().idSolicitudBusqueda(idSolicitudBusqueda).build();
    }

    private static AnulacionBusquedaBarrilFormDTO anulacionFormDTO(String motivoAnulacion) {
        return AnulacionBusquedaBarrilFormDTO.builder().motivoAnulacion(motivoAnulacion).build();
    }

    private static SolicitudBusquedaEntity crearSolicitudEntity(Long id, boolean buscado) {
        return SolicitudBusquedaEntity.builder()
                .id(id)
                .fechaBusqueda(LocalDateTime.of(2026, 2, 15, 10, 0))
                .observaciones("Horario tarde")
                .buscado(buscado)
                .build();
    }

    private static BusquedaBarrilEntity crearBusquedaEntity(Long id, EstadoTransaccion estado, SolicitudBusquedaEntity solicitudBusqueda) {
        return BusquedaBarrilEntity.builder()
                .id(id)
                .estado(estado)
                .solicitudBusqueda(solicitudBusqueda)
                .build();
    }
}
