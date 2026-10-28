package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MolinoEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ISecuenciaIdentificadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMolinoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MolinoFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MolinoResponseDTO;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnicoTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MolinoServicioImplTest {

    private static final int USOS_MAXIMOS_ANTES_MANTENIMIENTO = 500;

    @Mock
    private IMolinoRepository molinoRepository;

    @Mock
    private ISecuenciaIdentificadorRepository secuenciaIdentificadorRepository;

    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

    @InjectMocks
    private MolinoServicioImpl molinoServicio;

    @BeforeEach
    void secuenciaDelIdentificador() {
        // Valor por defecto de la secuencia para los casos de alta que no verifican el número generado
        lenient().when(secuenciaIdentificadorRepository.siguienteNumeroMolino()).thenReturn(1L);
    }

    // ==================== filtrarMolinos ====================

    @Test
    @DisplayName("CP-FMo-01: filtrarMolinos retorna una página de molinos correctamente mapeada a DTO cuando se filtra por identificador interno y estado operativo")
    void filtrarMolinos_debeRetornarPaginaMapeadaFiltrandoPorIdentificadorYEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0);
        MolinoEntity otroMolinoEntity = crearMolinoEntity(2L, "MOL-02", 120.0);
        when(molinoRepository.filtrarMolinos("MOL", EstadoOperativo.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(molinoEntity, otroMolinoEntity), pageable, 2));

        // === EJECUCION ===
        Page<MolinoResponseDTO> resultado = molinoServicio.filtrarMolinos("MOL", EstadoOperativo.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertMolinoDTO(molinoEntity, resultado.getContent().get(0));
        assertMolinoDTO(otroMolinoEntity, resultado.getContent().get(1));
        verify(molinoRepository).filtrarMolinos("MOL", EstadoOperativo.DISPONIBLE, pageable);
    }

    @Test
    @DisplayName("CP-FMo-02: filtrarMolinos propaga identificador interno y estado operativo nulos sin restringir esos criterios")
    void filtrarMolinos_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0);
        MolinoEntity otroMolinoEntity = crearMolinoEntity(2L, "MOL-02", 120.0);
        when(molinoRepository.filtrarMolinos(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(molinoEntity, otroMolinoEntity), pageable, 2));

        // === EJECUCION ===
        Page<MolinoResponseDTO> resultado = molinoServicio.filtrarMolinos(null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(molinoRepository).filtrarMolinos(null, null, pageable);
    }

    @Test
    @DisplayName("CP-FMo-03: filtrarMolinos retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarMolinos_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(molinoRepository.filtrarMolinos("INEXISTENTE", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<MolinoResponseDTO> resultado = molinoServicio.filtrarMolinos("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(molinoRepository).filtrarMolinos("INEXISTENTE", null, pageable);
    }

    @Test
    @DisplayName("CP-FMo-04: filtrarMolinos normaliza el identificador interno buscado (espacios y mayúsculas) antes de consultar")
    void filtrarMolinos_debeNormalizarElIdentificadorBuscado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(molinoRepository.filtrarMolinos("MOL 01", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        molinoServicio.filtrarMolinos("  mol   01  ", null, pageable);

        // === ASSERTS ===
        verify(molinoRepository).filtrarMolinos("MOL 01", null, pageable);
    }

    @Test
    @DisplayName("CP-FMo-05: filtrarMolinos trata el identificador interno formado solo por espacios como ausencia de filtro")
    void filtrarMolinos_debeTratarIdentificadorEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(molinoRepository.filtrarMolinos(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        molinoServicio.filtrarMolinos("    ", null, pageable);

        // === ASSERTS ===
        verify(molinoRepository).filtrarMolinos(null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del molino cuando el ID existe")
    void buscarPorId_debeRetornarMolinoExistente() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));

        // === EJECUCION ===
        MolinoResponseDTO resultado = molinoServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertMolinoDTO(molinoEntity, resultado);
        verify(molinoRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para molinos dados de baja
        when(molinoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> molinoServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el molino con ID: 99");
        verify(molinoRepository).findById(99L);
    }

    // ==================== altaMolino ====================

    @Test
    @DisplayName("CP-AM-01: altaMolino lanza ReglaNegocioException y no consulta el repositorio cuando el rendimiento de molienda es nulo")
    void altaMolino_debeRechazarRendimientoNulo() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO(null);

        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda es obligatorio");

        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-AM-02: altaMolino lanza ReglaNegocioException y no consulta el repositorio cuando el rendimiento de molienda es negativo")
    void altaMolino_debeRechazarRendimientoNegativo() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO(-1.0);

        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda debe ser mayor a 0");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-AM-03: altaMolino lanza ReglaNegocioException cuando el rendimiento de molienda es igual a cero (valor límite)")
    void altaMolino_debeRechazarRendimientoIgualACero() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO(0.0);

        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda debe ser mayor a 0");

        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-AM-06: altaMolino persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaMolino_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroMolino()).thenReturn(5L);
        MolinoFormDTO molinoFormDTO = molinoFormDTO(50.0);
        when(molinoRepository.save(any(MolinoEntity.class))).thenAnswer(invocation -> {
            MolinoEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        MolinoResponseDTO resultado = molinoServicio.altaMolino(molinoFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<MolinoEntity> captor = ArgumentCaptor.forClass(MolinoEntity.class);
        verify(molinoRepository).save(captor.capture());
        MolinoEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("MOL-05");
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(entidadCapturada.getRendimientoMolienda()).isEqualTo(50.0);
        assertThat(entidadCapturada.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        // El alta siempre debe registrar al molino como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MOL-05");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(resultado.getRendimientoMolienda()).isEqualTo(50.0);
        assertThat(resultado.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
    }

    @Test
    @DisplayName("CP-AM-07: altaMolino persiste con éxito cuando el rendimiento de molienda está en el límite inferior válido")
    void altaMolino_debePersistirConRendimientoEnLimiteInferiorValido() {
        // === PREPARACION DE DATOS ===
        MolinoFormDTO molinoFormDTO = molinoFormDTO(0.1);
        when(molinoRepository.save(any(MolinoEntity.class))).thenAnswer(invocation -> {
            MolinoEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(3L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        MolinoResponseDTO resultado = molinoServicio.altaMolino(molinoFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<MolinoEntity> captor = ArgumentCaptor.forClass(MolinoEntity.class);
        verify(molinoRepository).save(captor.capture());
        assertThat(captor.getValue().getRendimientoMolienda()).isEqualTo(0.1);
        assertThat(resultado.getRendimientoMolienda()).isEqualTo(0.1);
    }

    @Test
    @DisplayName("CP-AM-08: altaMolino lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son nulos")
    void altaMolino_debeRechazarUsosMaximosAntesMantenimientoNulo() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO(50.0, null);

        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento son obligatorios");

        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-AM-09: altaMolino lanza ReglaNegocioException cuando los usos máximos antes de mantenimiento son iguales a cero (valor límite)")
    void altaMolino_debeRechazarUsosMaximosAntesMantenimientoIgualACero() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO(50.0, 0);

        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(molinoRepository);
    }

    // ==================== modificarMolino ====================

    @Test
    @DisplayName("CP-MM-01: modificarMolino lanza ReglaNegocioException y no consulta la unicidad cuando el rendimiento de molienda es inválido")
    void modificarMolino_debeRechazarRendimientoInvalido() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 90.0);
        MolinoFormDTO molinoFormDTO = molinoFormDTO(0.0);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));

        assertThatThrownBy(() -> molinoServicio.modificarMolino(1L, molinoFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda debe ser mayor a 0");

        // La existencia del molino se valida antes que las reglas de negocio sobre sus datos
        verify(molinoRepository).findById(1L);
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-02: modificarMolino lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento son inválidos")
    void modificarMolino_debeRechazarUsosMaximosAntesMantenimientoInvalido() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 90.0);
        MolinoFormDTO molinoFormDTO = molinoFormDTO(50.0, 0);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));

        assertThatThrownBy(() -> molinoServicio.modificarMolino(1L, molinoFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verify(molinoRepository).findById(1L);
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-04: modificarMolino lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarMolino_debeLanzarExcepcionSiNoExiste() {
        MolinoFormDTO molinoFormDTO = molinoFormDTO(50.0);
        when(molinoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> molinoServicio.modificarMolino(99L, molinoFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el molino con ID: 99");

        verify(molinoRepository).findById(99L);
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-05: modificarMolino permite modificar molino aunque tenga comprometida una etapa en estado Pendiente de un lote (su escalado queda guardado en la instantánea del lote)")
    void modificarMolino_debePermitirModificarConLotePendiente() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 90.0);
        MolinoFormDTO molinoFormDTO = molinoFormDTO(100.0);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(molinoRepository.save(molinoEntity)).thenReturn(molinoEntity);

        org.assertj.core.api.Assertions.assertThatCode(() -> molinoServicio.modificarMolino(1L, molinoFormDTO)).doesNotThrowAnyException();

        verify(molinoRepository).save(molinoEntity);
        verify(etapaLoteRepository, never()).existsLotePendienteAsociado(any());
    }

    @Test
    @DisplayName("CP-MM-06: modificarMolino permite modificar molino aunque se encuentre en uso (su escalado queda guardado en la instantánea de cada lote)")
    void modificarMolino_debePermitirModificarEnUso() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 90.0, EstadoOperativo.EN_USO);
        MolinoFormDTO molinoFormDTO = molinoFormDTO(100.0);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(molinoRepository.save(molinoEntity)).thenReturn(molinoEntity);

        org.assertj.core.api.Assertions.assertThatCode(() -> molinoServicio.modificarMolino(1L, molinoFormDTO)).doesNotThrowAnyException();

        verify(molinoRepository).save(molinoEntity);
        verify(etapaLoteRepository, never()).existsLotePendienteAsociado(any());
    }

    @Test
    @DisplayName("CP-MM-07: modificarMolino actualiza los datos y persiste cuando el ID existe, sin cambiar el identificador interno (camino feliz)")
    void modificarMolino_debeActualizarMolinoExistente() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-VIEJO", 90.0);
        MolinoFormDTO molinoFormDTO = molinoFormDTO(100.0);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(molinoRepository.save(molinoEntity)).thenReturn(molinoEntity);

        // === EJECUCION ===
        MolinoResponseDTO resultado = molinoServicio.modificarMolino(1L, molinoFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<MolinoEntity> captor = ArgumentCaptor.forClass(MolinoEntity.class);
        verify(molinoRepository).save(captor.capture());
        MolinoEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("MOL-VIEJO");
        assertThat(entidadCapturada.getRendimientoMolienda()).isEqualTo(100.0);
        assertThat(entidadCapturada.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);

        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MOL-VIEJO");
        assertThat(resultado.getRendimientoMolienda()).isEqualTo(100.0);
        assertThat(resultado.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        verify(molinoRepository).findById(1L);
        // El identificador interno lo asignó el sistema al dar de alta: modificar no lo cambia ni pide otro número
        verifyNoInteractions(secuenciaIdentificadorRepository);
    }

    // ==================== bajaMolino ====================

    @Test
    @DisplayName("CP-BM-01: bajaMolino lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaMolino_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(molinoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> molinoServicio.bajaMolino(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el molino con ID: 99");

        verify(molinoRepository).findById(99L);
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-02: bajaMolino lanza ReglaNegocioException y no persiste cuando el molino tiene comprometida una etapa en estado Pendiente de un lote")
    void bajaMolino_debeRechazarAsociacionALotePendiente() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(true);

        assertThatThrownBy(() -> molinoServicio.bajaMolino(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el molino porque tiene comprometida una etapa en estado Pendiente de un lote");

        verify(etapaLoteRepository).existsLotePendienteAsociado(1L);
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-03: bajaMolino lanza ReglaNegocioException y no persiste cuando el molino se encuentra en uso")
    void bajaMolino_debeRechazarSiEstaEnUso() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0, EstadoOperativo.EN_USO);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> molinoServicio.bajaMolino(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el molino porque no se encuentra disponible");

        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-05: bajaMolino lanza ReglaNegocioException y no persiste cuando el molino se encuentra en limpieza")
    void bajaMolino_debeRechazarSiEstaEnLimpieza() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0, EstadoOperativo.EN_LIMPIEZA);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> molinoServicio.bajaMolino(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el molino porque no se encuentra disponible");

        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-06: bajaMolino lanza ReglaNegocioException y no persiste cuando el molino se encuentra en mantenimiento")
    void bajaMolino_debeRechazarSiEstaEnMantenimiento() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0, EstadoOperativo.EN_MANTENIMIENTO);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> molinoServicio.bajaMolino(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el molino porque no se encuentra disponible");

        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-07: bajaMolino informa primero la etapa en estado Pendiente de un lote cuando el molino además no está disponible")
    void bajaMolino_debePriorizarEtapaPendienteSobreNoDisponible() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0, EstadoOperativo.EN_USO);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(true);

        assertThatThrownBy(() -> molinoServicio.bajaMolino(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el molino porque tiene comprometida una etapa en estado Pendiente de un lote");

        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-04: bajaMolino marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe")
    void bajaMolino_debeMarcarBajaYRetornarMolinoExistente() {
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-01", 100.0);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);
        when(molinoRepository.save(molinoEntity)).thenReturn(molinoEntity);

        MolinoResponseDTO resultado = molinoServicio.bajaMolino(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(molinoEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertMolinoDTO(molinoEntity, resultado);
        verify(molinoRepository).findById(1L);
        verify(etapaLoteRepository).existsLotePendienteAsociado(1L);
        verify(molinoRepository).save(molinoEntity);
        verify(molinoRepository, never()).delete(any());
    }

    // ==================== validaciones técnicas de Molino (números finitos, techos y longitudes) ====================

    @Test
    @DisplayName("CP-AM-10: altaMolino lanza ReglaNegocioException y no consulta el repositorio cuando el rendimiento de molienda es NaN")
    void altaMolino_debeRechazar_rendimientoNaN() {
        MolinoFormDTO dto = molinoFormDTO(Double.NaN, 500);

        assertThatThrownBy(() -> molinoServicio.altaMolino(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda debe ser mayor a 0");

        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-AM-11: altaMolino lanza ReglaNegocioException y no consulta el repositorio cuando el rendimiento de molienda es infinito positivo")
    void altaMolino_debeRechazar_rendimientoInfinito() {
        MolinoFormDTO dto = molinoFormDTO(Double.POSITIVE_INFINITY, 500);

        assertThatThrownBy(() -> molinoServicio.altaMolino(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda debe ser mayor a 0");

        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-AM-12: altaMolino lanza ReglaNegocioException y no consulta el repositorio cuando el rendimiento de molienda es infinito negativo")
    void altaMolino_debeRechazar_rendimientoInfinitoNegativo() {
        MolinoFormDTO dto = molinoFormDTO(Double.NEGATIVE_INFINITY, 500);

        assertThatThrownBy(() -> molinoServicio.altaMolino(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda debe ser mayor a 0");

        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-AM-13: altaMolino lanza ReglaNegocioException y no consulta el repositorio cuando el rendimiento de molienda supera 10.000 kg/h")
    void altaMolino_debeRechazar_rendimientoSuperaElMaximo() {
        MolinoFormDTO dto = molinoFormDTO(10000.01, 500);

        assertThatThrownBy(() -> molinoServicio.altaMolino(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda no puede superar 10.000 kg/h");

        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-AM-14: altaMolino persiste con éxito cuando el rendimiento de molienda es exactamente 10.000 kg/h (valor límite válido)")
    void altaMolino_debeAceptar_rendimientoEnElMaximo() {
        MolinoFormDTO dto = molinoFormDTO(10000.0, 500);
        when(molinoRepository.save(any(MolinoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> molinoServicio.altaMolino(dto)).doesNotThrowAnyException();

        verify(molinoRepository).save(any(MolinoEntity.class));
    }

    @Test
    @DisplayName("CP-AM-15: altaMolino lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son negativos")
    void altaMolino_debeRechazar_usosNegativos() {
        MolinoFormDTO dto = molinoFormDTO(50.0, -1);

        assertThatThrownBy(() -> molinoServicio.altaMolino(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-AM-16: altaMolino lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento superan 100.000")
    void altaMolino_debeRechazar_usosSuperanElMaximo() {
        MolinoFormDTO dto = molinoFormDTO(50.0, 100001);

        assertThatThrownBy(() -> molinoServicio.altaMolino(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verifyNoInteractions(molinoRepository);
    }

    @Test
    @DisplayName("CP-AM-17: altaMolino persiste con éxito cuando los usos máximos antes de mantenimiento son exactamente 100.000 (valor límite válido)")
    void altaMolino_debeAceptar_usosEnElMaximo() {
        MolinoFormDTO dto = molinoFormDTO(50.0, 100000);
        when(molinoRepository.save(any(MolinoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> molinoServicio.altaMolino(dto)).doesNotThrowAnyException();

        verify(molinoRepository).save(any(MolinoEntity.class));
    }

    @Test
    @DisplayName("CP-MM-09: modificarMolino lanza ReglaNegocioException y no consulta la unicidad cuando el rendimiento de molienda es NaN")
    void modificarMolino_debeRechazar_rendimientoNaN() {
        MolinoFormDTO dto = molinoFormDTO(Double.NaN, 500);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(crearMolinoEntity(1L, "MOL-01", 90.0)));

        assertThatThrownBy(() -> molinoServicio.modificarMolino(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda debe ser mayor a 0");

        verify(molinoRepository).findById(1L);
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-10: modificarMolino lanza ReglaNegocioException y no consulta la unicidad cuando el rendimiento de molienda supera 10.000 kg/h")
    void modificarMolino_debeRechazar_rendimientoSuperaElMaximo() {
        MolinoFormDTO dto = molinoFormDTO(10000.01, 500);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(crearMolinoEntity(1L, "MOL-01", 90.0)));

        assertThatThrownBy(() -> molinoServicio.modificarMolino(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento de molienda no puede superar 10.000 kg/h");

        verify(molinoRepository).findById(1L);
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-11: modificarMolino lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento superan 100.000")
    void modificarMolino_debeRechazar_usosSuperanElMaximo() {
        MolinoFormDTO dto = molinoFormDTO(50.0, 100001);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(crearMolinoEntity(1L, "MOL-01", 90.0)));

        assertThatThrownBy(() -> molinoServicio.modificarMolino(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verify(molinoRepository).findById(1L);
        verify(molinoRepository, never()).save(any());
    }

    // ==================== helpers ====================

    private static MolinoEntity crearMolinoEntity(Long id, String identificadorInterno, Double rendimientoMolienda) {
        return crearMolinoEntity(id, identificadorInterno, rendimientoMolienda, EstadoOperativo.DISPONIBLE);
    }

    private static MolinoEntity crearMolinoEntity(Long id, String identificadorInterno, Double rendimientoMolienda, EstadoOperativo estadoOperativo) {
        return MolinoEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .estadoOperativo(estadoOperativo)
                .rendimientoMolienda(rendimientoMolienda)
                .usosMaximosAntesMantenimiento(USOS_MAXIMOS_ANTES_MANTENIMIENTO)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static MolinoFormDTO molinoFormDTO(Double rendimientoMolienda) {
        return molinoFormDTO(rendimientoMolienda, USOS_MAXIMOS_ANTES_MANTENIMIENTO);
    }

    private static MolinoFormDTO molinoFormDTO(Double rendimientoMolienda, Integer usosMaximosAntesMantenimiento) {
        return MolinoFormDTO.builder()
                .rendimientoMolienda(rendimientoMolienda)
                .usosMaximosAntesMantenimiento(usosMaximosAntesMantenimiento)
                .build();
    }

    private static void assertMolinoDTO(MolinoEntity entidad, MolinoResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificadorInterno()).isEqualTo(entidad.getIdentificadorInterno());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getRendimientoMolienda()).isEqualTo(entidad.getRendimientoMolienda());
        assertThat(dto.getUsosMaximosAntesMantenimiento()).isEqualTo(entidad.getUsosMaximosAntesMantenimiento());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }

    // ==================== normalización y unicidad ====================

    // ==================== identificador interno generado ====================

    @Test
    @DisplayName("CP-AM-26: altaMolino asigna el identificador interno generado a partir de la secuencia (MOL-07)")
    void altaMolino_debeAsignarElIdentificadorInternoGenerado() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroMolino()).thenReturn(7L);
        when(molinoRepository.save(any(MolinoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        var resultado = molinoServicio.altaMolino(molinoFormDTO(50.0));

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MOL-07");
    }

    @Test
    @DisplayName("CP-AM-27: altaMolino no pide un número de la secuencia cuando falla cualquier validación")
    void altaMolino_noDebePedirNumeroDeLaSecuenciaSiFallaUnaValidacion() {
        // === EJECUCION Y ASERTOS ===
        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO(null))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO(-1.0))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> molinoServicio.altaMolino(molinoFormDTO(150.0, 0))).isInstanceOf(ReglaNegocioException.class);

        verifyNoInteractions(secuenciaIdentificadorRepository);
        verify(molinoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AM-28: altaMolino pide el número de la secuencia como último paso, justo antes de guardar")
    void altaMolino_debePedirElNumeroDeLaSecuenciaAntesDeGuardar() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroMolino()).thenReturn(1L);
        when(molinoRepository.save(any(MolinoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        molinoServicio.altaMolino(molinoFormDTO(50.0));

        // === ASSERTS ===
        InOrder orden = inOrder(secuenciaIdentificadorRepository, molinoRepository);
        orden.verify(secuenciaIdentificadorRepository).siguienteNumeroMolino();
        orden.verify(molinoRepository).save(any(MolinoEntity.class));
    }

    @Test
    @DisplayName("CP-MM-19: modificarMolino no cambia el identificador interno asignado por el sistema ni pide otro número")
    void modificarMolino_noDebeCambiarElIdentificadorInterno() {
        // === PREPARACION DE DATOS ===
        MolinoEntity molinoEntity = crearMolinoEntity(1L, "MOL-03", 90.0);
        when(molinoRepository.findById(1L)).thenReturn(Optional.of(molinoEntity));
        when(molinoRepository.save(molinoEntity)).thenReturn(molinoEntity);

        // === EJECUCION ===
        var resultado = molinoServicio.modificarMolino(1L, molinoFormDTO(50.0));

        // === ASSERTS ===
        assertThat(molinoEntity.getIdentificadorInterno()).isEqualTo("MOL-03");
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MOL-03");
        verifyNoInteractions(secuenciaIdentificadorRepository);
    }
}
