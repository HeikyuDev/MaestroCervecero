package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.AjusteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.MotivoAjusteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoTransaccion;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IAjusteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IMotivoAjusteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AjusteInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.AnularAjusteInsumoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.AjusteInsumoResponseDTO;
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
class AjusteInsumoServicioImplTest {

    @Mock
    private IAjusteInsumoRepository ajusteInsumoRepository;

    @Mock
    private IMotivoAjusteRepository motivoAjusteRepository;

    @Mock
    private ILoteInsumoRepository loteInsumoRepository;

    @InjectMocks
    private AjusteInsumoServicioImpl ajusteInsumoServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de ajustes de insumo correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 5.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), lote);
        AjusteInsumoEntity otroAjusteEntity = ajusteInsumoEntity(2L, EstadoTransaccion.REGISTRADO, 3.0, motivoAjusteEntity(2L, TipoAjuste.EGRESO), lote);
        AjusteInsumoEntity tercerAjusteEntity = ajusteInsumoEntity(3L, EstadoTransaccion.ANULADO, 2.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), lote);

        when(ajusteInsumoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(ajusteEntity, otroAjusteEntity, tercerAjusteEntity), pageable, 3));

        // === EJECUCION ===
        Page<AjusteInsumoResponseDTO> resultado = ajusteInsumoServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        verify(ajusteInsumoRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay ajustes de insumo registrados")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ajusteInsumoRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<AjusteInsumoResponseDTO> resultado = ajusteInsumoServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(ajusteInsumoRepository).findAll(pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del ajuste de insumo cuando el ID existe")
    void buscarPorId_debeRetornarAjusteInsumoExistente() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity lote = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 5.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), lote);
        when(ajusteInsumoRepository.findById(1L)).thenReturn(Optional.of(ajusteEntity));

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getCantidad()).isEqualTo(5.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(ajusteInsumoRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        when(ajusteInsumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ajusteInsumoServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el ajuste de insumo con ID: 99");
        verify(ajusteInsumoRepository).findById(99L);
    }

    // ==================== registrarAjusteInsumo ====================

    @Test
    @DisplayName("CP-RA-01: registrarAjusteInsumo lanza RecursoNoEncontradoException y no persiste cuando el motivo de ajuste no existe")
    void registrarAjusteInsumo_debeRechazarMotivoAjusteInexistente() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(99L, 1L, 10.0, "Observación");
        when(motivoAjusteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el motivo de ajuste con ID: 99");

        verifyNoInteractions(loteInsumoRepository);
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-02: registrarAjusteInsumo lanza RecursoNoEncontradoException y no persiste cuando el lote de insumo no existe")
    void registrarAjusteInsumo_debeRechazarLoteInsumoInexistente() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 99L, 10.0, "Observación");
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.INGRESO)));
        when(loteInsumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lote de insumo con ID: 99");

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-03: registrarAjusteInsumo lanza ReglaNegocioException cuando la cantidad es nula")
    void registrarAjusteInsumo_debeRechazarCantidadNula() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, null, "Observación");
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.INGRESO)));
        when(loteInsumoRepository.findById(1L)).thenReturn(Optional.of(loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0)));

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a cero");

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-04: registrarAjusteInsumo lanza ReglaNegocioException cuando la cantidad es cero (límite)")
    void registrarAjusteInsumo_debeRechazarCantidadEnCero() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 0.0, "Observación");
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.INGRESO)));
        when(loteInsumoRepository.findById(1L)).thenReturn(Optional.of(loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0)));

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a cero");

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-05: registrarAjusteInsumo lanza ReglaNegocioException cuando la cantidad es negativa")
    void registrarAjusteInsumo_debeRechazarCantidadNegativa() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, -5.0, "Observación");
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.INGRESO)));
        when(loteInsumoRepository.findById(1L)).thenReturn(Optional.of(loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0)));

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad debe ser mayor a cero");

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-06: registrarAjusteInsumo lanza ReglaNegocioException con motivo EGRESO cuando la cantidad supera la disponible del lote")
    void registrarAjusteInsumo_debeRechazarEgresoQueSuperaCantidadDisponible() {
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 15.0, "Rotura de lote");
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.findById(1L)).thenReturn(Optional.of(loteInsumoEntity(1L, insumoEntity(1L), 10.0, 0.0)));

        assertThatThrownBy(() -> ajusteInsumoServicio.registrarAjusteInsumo(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad a descontar no puede superar la cantidad disponible del lote de insumo");

        verify(loteInsumoRepository, never()).save(any());
        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-RA-07: registrarAjusteInsumo permite un EGRESO con cantidad igual a la disponible (límite, no debe quedar negativo)")
    void registrarAjusteInsumo_debePermitirEgresoConCantidadIgualALaDisponible() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 10.0, "Rotura de lote");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 10.0, 0.0);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.findById(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> {
            AjusteInsumoEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(0.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
        verify(loteInsumoRepository).save(loteEntity);
    }

    @Test
    @DisplayName("CP-RA-08: registrarAjusteInsumo con motivo INGRESO no valida contra la cantidad disponible del lote")
    void registrarAjusteInsumo_debePermitirIngresoSinValidarDisponible() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 100.0, "Ingreso grande");
        // disponible = 5.0 - 3.0 = 2.0, pero la cantidad del ajuste (100.0) no debe compararse contra eso
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 5.0, 3.0);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.INGRESO)));
        when(loteInsumoRepository.findById(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(105.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RA-09: registrarAjusteInsumo persiste y retorna el DTO con motivo INGRESO (camino feliz)")
    void registrarAjusteInsumo_debePersistirConMotivoIngreso() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 5.0, "Corrección de conteo físico");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        MotivoAjusteEntity motivoEntity = motivoAjusteEntity(1L, TipoAjuste.INGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoEntity));
        when(loteInsumoRepository.findById(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> {
            AjusteInsumoEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(25.0);

        ArgumentCaptor<AjusteInsumoEntity> captor = ArgumentCaptor.forClass(AjusteInsumoEntity.class);
        verify(ajusteInsumoRepository).save(captor.capture());
        AjusteInsumoEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getCantidad()).isEqualTo(5.0);
        assertThat(entidadCapturada.getObservacion()).isEqualTo("Corrección de conteo físico");
        assertThat(entidadCapturada.getMotivoAjuste()).isEqualTo(motivoEntity);
        assertThat(entidadCapturada.getLoteInsumo()).isEqualTo(loteEntity);
        assertThat(entidadCapturada.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    @Test
    @DisplayName("CP-RA-10: registrarAjusteInsumo persiste y retorna el DTO con motivo EGRESO (camino feliz)")
    void registrarAjusteInsumo_debePersistirConMotivoEgreso() {
        // === PREPARACION DE DATOS ===
        AjusteInsumoFormDTO formDTO = ajusteInsumoFormDTO(1L, 1L, 5.0, "Rotura de lote");
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoAjusteEntity(1L, TipoAjuste.EGRESO)));
        when(loteInsumoRepository.findById(1L)).thenReturn(Optional.of(loteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(any(AjusteInsumoEntity.class))).thenAnswer(invocation -> {
            AjusteInsumoEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(2L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.registrarAjusteInsumo(formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(15.0);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.REGISTRADO);
    }

    // ==================== anularAjusteInsumo ====================

    @Test
    @DisplayName("CP-AA-01: anularAjusteInsumo lanza ReglaNegocioException y no consulta el repositorio cuando el motivo de anulación es nulo")
    void anularAjusteInsumo_debeRechazarMotivoAnulacionNulo() {
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO(null);

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(ajusteInsumoRepository);
        verifyNoInteractions(loteInsumoRepository);
    }

    @Test
    @DisplayName("CP-AA-02: anularAjusteInsumo lanza ReglaNegocioException cuando el motivo de anulación está en blanco")
    void anularAjusteInsumo_debeRechazarMotivoAnulacionEnBlanco() {
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("   ");

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El motivo de anulación es obligatorio");

        verifyNoInteractions(ajusteInsumoRepository);
    }

    @Test
    @DisplayName("CP-AA-03: anularAjusteInsumo lanza RecursoNoEncontradoException y no persiste cuando el ajuste no existe")
    void anularAjusteInsumo_debeLanzarExcepcionSiNoExiste() {
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ajusteInsumoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el ajuste de insumo con ID: 99");

        verify(ajusteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AA-04: anularAjusteInsumo lanza ReglaNegocioException cuando el ajuste ya está anulado")
    void anularAjusteInsumo_debeRechazarAjusteYaAnulado() {
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 20.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.ANULADO, 5.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), loteEntity);
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ajusteInsumoRepository.findById(1L)).thenReturn(Optional.of(ajusteEntity));

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Solo se pueden anular ajustes de insumo en estado REGISTRADO");

        verify(ajusteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AA-05: anularAjusteInsumo lanza ReglaNegocioException al revertir un INGRESO cuya cantidad supera la disponible del lote")
    void anularAjusteInsumo_debeRechazarReversionDeIngresoQueSuperaCantidadDisponible() {
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 15.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 20.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), loteEntity);
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ajusteInsumoRepository.findById(1L)).thenReturn(Optional.of(ajusteEntity));

        assertThatThrownBy(() -> ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede anular el ajuste: su cantidad supera la cantidad disponible del lote de insumo");

        verify(loteInsumoRepository, never()).save(any());
        verify(ajusteInsumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AA-06: anularAjusteInsumo revierte exitosamente un ajuste INGRESO")
    void anularAjusteInsumo_debeRevertirAjusteIngresoExitosamente() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 15.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 10.0, motivoAjusteEntity(1L, TipoAjuste.INGRESO), loteEntity);
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ajusteInsumoRepository.findById(1L)).thenReturn(Optional.of(ajusteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(ajusteEntity)).thenReturn(ajusteEntity);

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(5.0);
        assertThat(ajusteEntity.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(ajusteEntity.getFechaAnulacion()).isNotNull();
        assertThat(ajusteEntity.getMotivoAnulacion()).isEqualTo("Error de carga");
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getMotivoAnulacion()).isEqualTo("Error de carga");
        verify(loteInsumoRepository).save(loteEntity);
    }

    @Test
    @DisplayName("CP-AA-07: anularAjusteInsumo revierte exitosamente un ajuste EGRESO (nunca queda negativo)")
    void anularAjusteInsumo_debeRevertirAjusteEgresoExitosamente() {
        // === PREPARACION DE DATOS ===
        LoteInsumoEntity loteEntity = loteInsumoEntity(1L, insumoEntity(1L), 5.0, 0.0);
        AjusteInsumoEntity ajusteEntity = ajusteInsumoEntity(1L, EstadoTransaccion.REGISTRADO, 10.0, motivoAjusteEntity(1L, TipoAjuste.EGRESO), loteEntity);
        AnularAjusteInsumoFormDTO formDTO = anularFormDTO("Error de carga");
        when(ajusteInsumoRepository.findById(1L)).thenReturn(Optional.of(ajusteEntity));
        when(loteInsumoRepository.save(loteEntity)).thenReturn(loteEntity);
        when(ajusteInsumoRepository.save(ajusteEntity)).thenReturn(ajusteEntity);

        // === EJECUCION ===
        AjusteInsumoResponseDTO resultado = ajusteInsumoServicio.anularAjusteInsumo(1L, formDTO);

        // === ASSERTS ===
        assertThat(loteEntity.getCantidadActual()).isEqualTo(15.0);
        assertThat(ajusteEntity.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTransaccion.ANULADO);
    }

    // ==================== helpers de construcción ====================

    private static AjusteInsumoFormDTO ajusteInsumoFormDTO(Long idMotivoAjuste, Long idLoteInsumo, Double cantidad, String observacion) {
        return AjusteInsumoFormDTO.builder()
                .idMotivoAjuste(idMotivoAjuste)
                .idLoteInsumo(idLoteInsumo)
                .cantidad(cantidad)
                .observacion(observacion)
                .build();
    }

    private static AnularAjusteInsumoFormDTO anularFormDTO(String motivoAnulacion) {
        return AnularAjusteInsumoFormDTO.builder()
                .motivoAnulacion(motivoAnulacion)
                .build();
    }

    private static InsumoEntity insumoEntity(Long id) {
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").estado(Estado.ACTIVO).build();
    }

    private static MotivoAjusteEntity motivoAjusteEntity(Long id, TipoAjuste tipoAjuste) {
        return MotivoAjusteEntity.builder()
                .id(id)
                .nombre(tipoAjuste == TipoAjuste.INGRESO ? "Corrección de conteo" : "Rotura de lote")
                .tipoAjuste(tipoAjuste)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static LoteInsumoEntity loteInsumoEntity(Long id, InsumoEntity insumo, double cantidadActual, double cantidadReservada) {
        return LoteInsumoEntity.builder()
                .id(id)
                .insumo(insumo)
                .identificacionLoteProveedor("LOTE-" + id)
                .fechaVencimiento(java.time.LocalDate.now().plusMonths(6))
                .cantidadActual(cantidadActual)
                .cantidadReservada(cantidadReservada)
                .build();
    }

    private static AjusteInsumoEntity ajusteInsumoEntity(Long id, EstadoTransaccion estado, Double cantidad, MotivoAjusteEntity motivoAjuste, LoteInsumoEntity loteInsumo) {
        return AjusteInsumoEntity.builder()
                .id(id)
                .cantidad(cantidad)
                .observacion("Observación de prueba")
                .motivoAjuste(motivoAjuste)
                .loteInsumo(loteInsumo)
                .estado(estado)
                .build();
    }
}
