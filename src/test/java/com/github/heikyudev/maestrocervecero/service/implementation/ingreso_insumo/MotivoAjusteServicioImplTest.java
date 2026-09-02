package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.MotivoAjusteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IMotivoAjusteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.MotivoAjusteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.MotivoAjusteResponseDTO;
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
class MotivoAjusteServicioImplTest {

    @Mock
    private IMotivoAjusteRepository motivoAjusteRepository;

    @InjectMocks
    private MotivoAjusteServicioImpl motivoAjusteServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de motivos de ajuste correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MotivoAjusteEntity motivoEntity = crearMotivoAjusteEntity(1L, "Rotura de lote", TipoAjuste.EGRESO);
        MotivoAjusteEntity otroMotivoEntity = crearMotivoAjusteEntity(2L, "Corrección de conteo", TipoAjuste.INGRESO);
        MotivoAjusteEntity tercerMotivoEntity = crearMotivoAjusteEntity(3L, "Derrame", TipoAjuste.EGRESO);

        // Cuando motivoAjusteRepository.findAll(pageable) sea llamado, retorna una página con los motivos activos
        // (el filtrado por estado = ACTIVO ya está resuelto dentro de la consulta del repositorio)
        when(motivoAjusteRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(motivoEntity, otroMotivoEntity, tercerMotivoEntity), pageable, 3));

        // === EJECUCION ===
        Page<MotivoAjusteResponseDTO> resultado = motivoAjusteServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertMotivoAjusteDTO(motivoEntity, resultado.getContent().get(0));
        assertMotivoAjusteDTO(otroMotivoEntity, resultado.getContent().get(1));
        assertMotivoAjusteDTO(tercerMotivoEntity, resultado.getContent().get(2));
        verify(motivoAjusteRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay motivos de ajuste registrados")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(motivoAjusteRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<MotivoAjusteResponseDTO> resultado = motivoAjusteServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(motivoAjusteRepository).findAll(pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del motivo de ajuste cuando el ID existe")
    void buscarPorId_debeRetornarMotivoAjusteExistente() {
        // === PREPARACION DE DATOS ===
        MotivoAjusteEntity motivoEntity = crearMotivoAjusteEntity(1L, "Rotura de lote", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoEntity));

        // === EJECUCION ===
        MotivoAjusteResponseDTO resultado = motivoAjusteServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertMotivoAjusteDTO(motivoEntity, resultado);
        verify(motivoAjusteRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para motivos dados de baja
        when(motivoAjusteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> motivoAjusteServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el motivo de ajuste con ID: 99");
        verify(motivoAjusteRepository).findById(99L);
    }

    // ==================== altaMotivoAjuste ====================

    @Test
    @DisplayName("CP-AMA-01: altaMotivoAjuste lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe")
    void altaMotivoAjuste_debeRechazarNombreDuplicado() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Rotura de lote", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.existsByNombreIgnoreCase("Rotura de lote")).thenReturn(true);

        assertThatThrownBy(() -> motivoAjusteServicio.altaMotivoAjuste(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un motivo de ajuste con el nombre 'Rotura de lote'");

        verify(motivoAjusteRepository).existsByNombreIgnoreCase("Rotura de lote");
        verify(motivoAjusteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMA-02: altaMotivoAjuste lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case (case-insensitive)")
    void altaMotivoAjuste_debeRechazarNombreDuplicadoCaseInsensitive() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("rotura de lote", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.existsByNombreIgnoreCase("rotura de lote")).thenReturn(true);

        assertThatThrownBy(() -> motivoAjusteServicio.altaMotivoAjuste(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un motivo de ajuste con el nombre 'rotura de lote'");

        verify(motivoAjusteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMA-03: altaMotivoAjuste persiste y retorna el DTO con tipo INGRESO (camino feliz)")
    void altaMotivoAjuste_debePersistirConTipoIngreso() {
        // === PREPARACION DE DATOS ===
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Corrección de conteo", TipoAjuste.INGRESO);
        when(motivoAjusteRepository.existsByNombreIgnoreCase("Corrección de conteo")).thenReturn(false);
        when(motivoAjusteRepository.save(any(MotivoAjusteEntity.class))).thenAnswer(invocation -> {
            MotivoAjusteEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        MotivoAjusteResponseDTO resultado = motivoAjusteServicio.altaMotivoAjuste(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<MotivoAjusteEntity> captor = ArgumentCaptor.forClass(MotivoAjusteEntity.class);
        verify(motivoAjusteRepository).save(captor.capture());
        MotivoAjusteEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Corrección de conteo");
        assertThat(entidadCapturada.getTipoAjuste()).isEqualTo(TipoAjuste.INGRESO);
        // El alta siempre debe registrar al motivo de ajuste como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Corrección de conteo");
        assertThat(resultado.getTipoAjuste()).isEqualTo(TipoAjuste.INGRESO);
    }

    @Test
    @DisplayName("CP-AMA-04: altaMotivoAjuste persiste y retorna el DTO con tipo EGRESO")
    void altaMotivoAjuste_debePersistirConTipoEgreso() {
        // === PREPARACION DE DATOS ===
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Rotura de lote", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.existsByNombreIgnoreCase("Rotura de lote")).thenReturn(false);
        when(motivoAjusteRepository.save(any(MotivoAjusteEntity.class))).thenAnswer(invocation -> {
            MotivoAjusteEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(2L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        MotivoAjusteResponseDTO resultado = motivoAjusteServicio.altaMotivoAjuste(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getTipoAjuste()).isEqualTo(TipoAjuste.EGRESO);
        ArgumentCaptor<MotivoAjusteEntity> captor = ArgumentCaptor.forClass(MotivoAjusteEntity.class);
        verify(motivoAjusteRepository).save(captor.capture());
        assertThat(captor.getValue().getTipoAjuste()).isEqualTo(TipoAjuste.EGRESO);
        assertThat(captor.getValue().getEstado()).isEqualTo(Estado.ACTIVO);
    }

    // ==================== modificarMotivoAjuste ====================

    @Test
    @DisplayName("CP-MMA-01: modificarMotivoAjuste lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarMotivoAjuste_debeLanzarExcepcionSiNoExiste() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Rotura de lote", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> motivoAjusteServicio.modificarMotivoAjuste(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el motivo de ajuste con ID: 99");

        verify(motivoAjusteRepository).findById(99L);
        // Al no existir la entidad, no debe llegarse a validar la duplicidad de nombre
        verify(motivoAjusteRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(motivoAjusteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MMA-02: modificarMotivoAjuste lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otro motivo de ajuste")
    void modificarMotivoAjuste_debeRechazarNombreEnUsoPorOtroMotivoAjuste() {
        MotivoAjusteEntity motivoEntity = crearMotivoAjusteEntity(1L, "Corrección de conteo", TipoAjuste.INGRESO);
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Rotura de lote", TipoAjuste.INGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoEntity));
        when(motivoAjusteRepository.existsByNombreIgnoreCaseAndIdNot("Rotura de lote", 1L)).thenReturn(true);

        assertThatThrownBy(() -> motivoAjusteServicio.modificarMotivoAjuste(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Rotura de lote' ya está en uso por otro motivo de ajuste");

        verify(motivoAjusteRepository).existsByNombreIgnoreCaseAndIdNot("Rotura de lote", 1L);
        verify(motivoAjusteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MMA-03: modificarMotivoAjuste permite conservar el propio nombre actual al actualizar")
    void modificarMotivoAjuste_debePermitirConservarNombrePropio() {
        MotivoAjusteEntity motivoEntity = crearMotivoAjusteEntity(1L, "Corrección de conteo", TipoAjuste.INGRESO);
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("corrección de conteo", TipoAjuste.INGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoEntity));
        when(motivoAjusteRepository.existsByNombreIgnoreCaseAndIdNot("corrección de conteo", 1L)).thenReturn(false);
        when(motivoAjusteRepository.save(motivoEntity)).thenReturn(motivoEntity);

        MotivoAjusteResponseDTO resultado = motivoAjusteServicio.modificarMotivoAjuste(1L, formDTO);

        assertThat(resultado.getNombre()).isEqualTo("corrección de conteo");
        verify(motivoAjusteRepository).save(motivoEntity);
    }

    @Test
    @DisplayName("CP-MMA-04: modificarMotivoAjuste actualiza nombre y tipo de ajuste, y persiste (camino feliz)")
    void modificarMotivoAjuste_debeActualizarNombreYTipoAjuste() {
        // === PREPARACION DE DATOS ===
        MotivoAjusteEntity motivoEntity = crearMotivoAjusteEntity(1L, "Corrección de conteo", TipoAjuste.INGRESO);
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Corrección de conteo (egreso)", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoEntity));
        when(motivoAjusteRepository.existsByNombreIgnoreCaseAndIdNot("Corrección de conteo (egreso)", 1L)).thenReturn(false);
        when(motivoAjusteRepository.save(motivoEntity)).thenReturn(motivoEntity);

        // === EJECUCION ===
        MotivoAjusteResponseDTO resultado = motivoAjusteServicio.modificarMotivoAjuste(1L, formDTO);

        // === ASSERTS ===
        ArgumentCaptor<MotivoAjusteEntity> captor = ArgumentCaptor.forClass(MotivoAjusteEntity.class);
        verify(motivoAjusteRepository).save(captor.capture());
        MotivoAjusteEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Corrección de conteo (egreso)");
        assertThat(entidadCapturada.getTipoAjuste()).isEqualTo(TipoAjuste.EGRESO);

        assertThat(resultado.getNombre()).isEqualTo("Corrección de conteo (egreso)");
        assertThat(resultado.getTipoAjuste()).isEqualTo(TipoAjuste.EGRESO);
        verify(motivoAjusteRepository).findById(1L);
        verify(motivoAjusteRepository).existsByNombreIgnoreCaseAndIdNot("Corrección de conteo (egreso)", 1L);
    }

    // ==================== bajaMotivoAjuste ====================

    @Test
    @DisplayName("CP-BMA-01: bajaMotivoAjuste lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaMotivoAjuste_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(motivoAjusteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> motivoAjusteServicio.bajaMotivoAjuste(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el motivo de ajuste con ID: 99");

        verify(motivoAjusteRepository).findById(99L);
        verify(motivoAjusteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BMA-02: bajaMotivoAjuste marca el estado como BAJA, persiste y retorna el DTO")
    void bajaMotivoAjuste_debeMarcarBajaYRetornarMotivoAjusteExistente() {
        MotivoAjusteEntity motivoEntity = crearMotivoAjusteEntity(1L, "Rotura de lote", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoEntity));
        when(motivoAjusteRepository.save(motivoEntity)).thenReturn(motivoEntity);

        MotivoAjusteResponseDTO resultado = motivoAjusteServicio.bajaMotivoAjuste(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(motivoEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertMotivoAjusteDTO(motivoEntity, resultado);
        verify(motivoAjusteRepository).findById(1L);
        verify(motivoAjusteRepository).save(motivoEntity);
        verify(motivoAjusteRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static MotivoAjusteEntity crearMotivoAjusteEntity(Long id, String nombre, TipoAjuste tipoAjuste) {
        return MotivoAjusteEntity.builder()
                .id(id)
                .nombre(nombre)
                .tipoAjuste(tipoAjuste)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static MotivoAjusteFormDTO motivoAjusteFormDTO(String nombre, TipoAjuste tipoAjuste) {
        return MotivoAjusteFormDTO.builder()
                .nombre(nombre)
                .tipoAjuste(tipoAjuste)
                .build();
    }

    private static void assertMotivoAjusteDTO(MotivoAjusteEntity entidad, MotivoAjusteResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getTipoAjuste()).isEqualTo(entidad.getTipoAjuste());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
