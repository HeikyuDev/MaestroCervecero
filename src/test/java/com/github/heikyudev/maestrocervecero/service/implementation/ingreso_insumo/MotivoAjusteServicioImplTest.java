package com.github.heikyudev.maestrocervecero.service.implementation.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.MotivoAjusteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.TipoAjuste;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IAjusteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.IMotivoAjusteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ingreso_insumo.MotivoAjusteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ingreso_insumo.MotivoAjusteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnicoTest;
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
import static org.assertj.core.api.Assertions.assertThatCode;
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
    @Mock
    private IAjusteInsumoRepository ajusteInsumoRepository;

    @InjectMocks
    private MotivoAjusteServicioImpl motivoAjusteServicio;

    // ==================== filtrarMotivosAjuste ====================

    @Test
    @DisplayName("CP-FMA-01: filtrarMotivosAjuste filtra por nombre y tipo de ajuste informados")
    void filtrar_debeFiltrarPorNombreYTipoAjuste() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MotivoAjusteEntity motivo1 = crearMotivoAjusteEntity(1L, "Rotura de lote", TipoAjuste.EGRESO);
        MotivoAjusteEntity motivo2 = crearMotivoAjusteEntity(2L, "Rotura por caída", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.filtrarMotivosAjuste("Rotura", TipoAjuste.EGRESO, pageable))
                .thenReturn(new PageImpl<>(List.of(motivo1, motivo2), pageable, 2));

        // === EJECUCION ===
        Page<MotivoAjusteResponseDTO> resultado = motivoAjusteServicio.filtrarMotivosAjuste("Rotura", TipoAjuste.EGRESO, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(motivoAjusteRepository).filtrarMotivosAjuste("Rotura", TipoAjuste.EGRESO, pageable);
    }

    @Test
    @DisplayName("CP-FMA-02: filtrarMotivosAjuste con nombre y tipo de ajuste nulos no restringe la búsqueda")
    void filtrar_debePropagarNombreYTipoAjusteNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MotivoAjusteEntity motivo1 = crearMotivoAjusteEntity(1L, "Rotura de lote", TipoAjuste.EGRESO);
        MotivoAjusteEntity motivo2 = crearMotivoAjusteEntity(2L, "Corrección de conteo", TipoAjuste.INGRESO);
        when(motivoAjusteRepository.filtrarMotivosAjuste(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(motivo1, motivo2), pageable, 2));

        // === EJECUCION ===
        Page<MotivoAjusteResponseDTO> resultado = motivoAjusteServicio.filtrarMotivosAjuste(null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).hasSize(2);
        verify(motivoAjusteRepository).filtrarMotivosAjuste(null, null, pageable);
    }

    @Test
    @DisplayName("CP-FMA-03: filtrarMotivosAjuste retorna una página vacía cuando no hay coincidencias")
    void filtrar_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(motivoAjusteRepository.filtrarMotivosAjuste("Inexistente", null, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // === EJECUCION ===
        Page<MotivoAjusteResponseDTO> resultado = motivoAjusteServicio.filtrarMotivosAjuste("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(motivoAjusteRepository).filtrarMotivosAjuste("Inexistente", null, pageable);
    }

    @Test
    @DisplayName("CP-FMA-04: filtrarMotivosAjuste normaliza el nombre buscado (espacios extremos e internos) antes de consultar")
    void filtrarMotivosAjuste_debeNormalizarElNombreBuscado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(motivoAjusteRepository.filtrarMotivosAjuste("Rotura de envase", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        motivoAjusteServicio.filtrarMotivosAjuste("   Rotura   de   envase  ", null, pageable);

        // === ASSERTS ===
        verify(motivoAjusteRepository).filtrarMotivosAjuste("Rotura de envase", null, pageable);
    }

    @Test
    @DisplayName("CP-FMA-05: filtrarMotivosAjuste trata un nombre formado solo por espacios como ausencia de filtro")
    void filtrarMotivosAjuste_debeTratarNombreEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(motivoAjusteRepository.filtrarMotivosAjuste(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        motivoAjusteServicio.filtrarMotivosAjuste("    ", null, pageable);

        // === ASSERTS ===
        verify(motivoAjusteRepository).filtrarMotivosAjuste(null, null, pageable);
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

    @Test
    @DisplayName("CP-MMA-05: modificarMotivoAjuste lanza ReglaNegocioException y no persiste cuando el motivo tiene ajustes de insumo registrados")
    void modificarMotivoAjuste_debeRechazarSiTieneAjustesRegistrados() {
        // === PREPARACION DE DATOS ===
        MotivoAjusteEntity motivoEntity = crearMotivoAjusteEntity(1L, "Corrección de conteo", TipoAjuste.INGRESO);
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Corrección de conteo", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(motivoEntity));
        when(ajusteInsumoRepository.existsByMotivoAjusteIdAndEstadoRegistrado(1L)).thenReturn(true);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> motivoAjusteServicio.modificarMotivoAjuste(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede modificar un motivo de ajuste que tiene ajustes de insumo registrados");

        verify(motivoAjusteRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(motivoAjusteRepository, never()).save(any());
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

    // ==================== normalización y unicidad ====================

    @Test
    @DisplayName("CP-AMA-05: altaMotivoAjuste guarda el nombre normalizado y consulta la unicidad con ese valor")
    void altaMotivoAjuste_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("  Pilsen   Nacional  ", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(motivoAjusteRepository.save(any(MotivoAjusteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        motivoAjusteServicio.altaMotivoAjuste(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<MotivoAjusteEntity> captor = ArgumentCaptor.forClass(MotivoAjusteEntity.class);
        verify(motivoAjusteRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(formDTO.getNombre()).isEqualTo("  Pilsen   Nacional  ");
        verify(motivoAjusteRepository).existsByNombreIgnoreCase("Pilsen Nacional");
        verify(motivoAjusteRepository).flush();
    }

    @Test
    @DisplayName("CP-AMA-06: altaMotivoAjuste lanza ReglaNegocioException y no consulta el repositorio cuando el nombre está vacío")
    void altaMotivoAjuste_debeRechazarValorVacio() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("   ", TipoAjuste.EGRESO);

        assertThatThrownBy(() -> motivoAjusteServicio.altaMotivoAjuste(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(motivoAjusteRepository);
    }

    @Test
    @DisplayName("CP-AMA-07: altaMotivoAjuste lanza RecursoDuplicadoException cuando el nombre normalizado ya existe")
    void altaMotivoAjuste_debeDetectarDuplicadoConElValorNormalizado() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("  pilsen ", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.existsByNombreIgnoreCase("pilsen")).thenReturn(true);

        assertThatThrownBy(() -> motivoAjusteServicio.altaMotivoAjuste(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un motivo de ajuste con el nombre 'pilsen'");

        verify(motivoAjusteRepository).existsByNombreIgnoreCase("pilsen");
        verify(motivoAjusteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AMA-08: altaMotivoAjuste traduce la violación del índice único udx_motivo_ajuste_nombre al mensaje de duplicado")
    void altaMotivoAjuste_debeTraducirLaViolacionDelIndiceUnico() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Pilsen Nacional", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(motivoAjusteRepository.save(any(MotivoAjusteEntity.class))).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_motivo_ajuste_nombre"));

        assertThatThrownBy(() -> motivoAjusteServicio.altaMotivoAjuste(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un motivo de ajuste con el nombre 'Pilsen Nacional'");
    }

    @Test
    @DisplayName("CP-AMA-09: altaMotivoAjuste permite reutilizar el nombre de un registro dado de baja")
    void altaMotivoAjuste_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Pilsen Nacional", TipoAjuste.EGRESO);
        // El único registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(motivoAjusteRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(motivoAjusteRepository.save(any(MotivoAjusteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> motivoAjusteServicio.altaMotivoAjuste(formDTO)).doesNotThrowAnyException();

        verify(motivoAjusteRepository).save(any(MotivoAjusteEntity.class));
    }

    @Test
    @DisplayName("CP-AMA-10: altaMotivoAjuste permite un nombre que difiere de otro solo en una tilde")
    void altaMotivoAjuste_debeTratarComoDistintosLosNombresConDistintaTilde() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Pílsen", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.existsByNombreIgnoreCase("Pílsen")).thenReturn(false);
        when(motivoAjusteRepository.save(any(MotivoAjusteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> motivoAjusteServicio.altaMotivoAjuste(formDTO)).doesNotThrowAnyException();

        verify(motivoAjusteRepository).existsByNombreIgnoreCase("Pílsen");
        verify(motivoAjusteRepository).save(any(MotivoAjusteEntity.class));
    }

    @Test
    @DisplayName("CP-MMA-06: modificarMotivoAjuste guarda el nombre normalizado y consulta la unicidad con ese valor")
    void modificarMotivoAjuste_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("  Pilsen   Nacional  ", TipoAjuste.EGRESO);
        MotivoAjusteEntity existente = crearMotivoAjusteEntity(1L, "Original", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(motivoAjusteRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(motivoAjusteRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        motivoAjusteServicio.modificarMotivoAjuste(1L, formDTO);

        // === ASSERTS ===
        assertThat(existente.getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(formDTO.getNombre()).isEqualTo("  Pilsen   Nacional  ");
        verify(motivoAjusteRepository).existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L);
        verify(motivoAjusteRepository).save(existente);
        verify(motivoAjusteRepository).flush();
    }

    @Test
    @DisplayName("CP-MMA-07: modificarMotivoAjuste lanza ReglaNegocioException cuando el nombre está vacío")
    void modificarMotivoAjuste_debeRechazarValorVacio() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("   ", TipoAjuste.EGRESO);

        assertThatThrownBy(() -> motivoAjusteServicio.modificarMotivoAjuste(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(motivoAjusteRepository);
    }

    @Test
    @DisplayName("CP-MMA-08: modificarMotivoAjuste lanza RecursoDuplicadoException cuando el nombre normalizado está en uso por otro registro")
    void modificarMotivoAjuste_debeDetectarDuplicadoConElValorNormalizado() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("  pilsen ", TipoAjuste.EGRESO);
        MotivoAjusteEntity existente = crearMotivoAjusteEntity(1L, "Original", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(motivoAjusteRepository.existsByNombreIgnoreCaseAndIdNot("pilsen", 1L)).thenReturn(true);

        assertThatThrownBy(() -> motivoAjusteServicio.modificarMotivoAjuste(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'pilsen' ya está en uso por otro motivo de ajuste");

        verify(motivoAjusteRepository).existsByNombreIgnoreCaseAndIdNot("pilsen", 1L);
        verify(motivoAjusteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MMA-09: modificarMotivoAjuste traduce la violación del índice único udx_motivo_ajuste_nombre al mensaje de duplicado")
    void modificarMotivoAjuste_debeTraducirLaViolacionDelIndiceUnico() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Pilsen Nacional", TipoAjuste.EGRESO);
        MotivoAjusteEntity existente = crearMotivoAjusteEntity(1L, "Original", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(motivoAjusteRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(motivoAjusteRepository.save(existente)).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_motivo_ajuste_nombre"));

        assertThatThrownBy(() -> motivoAjusteServicio.modificarMotivoAjuste(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Pilsen Nacional' ya está en uso por otro motivo de ajuste");
    }

    @Test
    @DisplayName("CP-MMA-10: modificarMotivoAjuste permite reutilizar el nombre de un registro dado de baja")
    void modificarMotivoAjuste_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        MotivoAjusteFormDTO formDTO = motivoAjusteFormDTO("Pilsen Nacional", TipoAjuste.EGRESO);
        MotivoAjusteEntity existente = crearMotivoAjusteEntity(1L, "Original", TipoAjuste.EGRESO);
        when(motivoAjusteRepository.findById(1L)).thenReturn(Optional.of(existente));
        // El único otro registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(motivoAjusteRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(motivoAjusteRepository.save(existente)).thenReturn(existente);

        assertThatCode(() -> motivoAjusteServicio.modificarMotivoAjuste(1L, formDTO)).doesNotThrowAnyException();

        assertThat(existente.getNombre()).isEqualTo("Pilsen Nacional");
        verify(motivoAjusteRepository).save(existente);
    }
}
