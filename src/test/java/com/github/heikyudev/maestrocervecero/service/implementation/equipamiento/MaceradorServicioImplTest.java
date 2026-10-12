package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MaceradorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MaceradorResponseDTO;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaceradorServicioImplTest {

    private static final int USOS_MAXIMOS_ANTES_MANTENIMIENTO = 500;

    @Mock
    private IMaceradorRepository maceradorRepository;

    @Mock
    private IEquipamientoRepository equipamientoRepository;

    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

    @InjectMocks
    private MaceradorServicioImpl maceradorServicio;

    // ==================== filtrarMaceradores ====================

    @Test
    @DisplayName("CP-FMa-01: filtrarMaceradores retorna una página de maceradores correctamente mapeada a DTO cuando se filtra por identificador interno y estado operativo")
    void filtrarMaceradores_debeRetornarPaginaMapeadaFiltrandoPorIdentificadorYEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.filtrarMaceradores("MAC", EstadoOperativo.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(maceradorEntity), pageable, 1));

        // === EJECUCION ===
        Page<MaceradorResponseDTO> resultado = maceradorServicio.filtrarMaceradores("MAC", EstadoOperativo.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertMaceradorDTO(maceradorEntity, resultado.getContent().get(0));
        verify(maceradorRepository).filtrarMaceradores("MAC", EstadoOperativo.DISPONIBLE, pageable);
    }

    @Test
    @DisplayName("CP-FMa-02: filtrarMaceradores propaga identificador interno y estado operativo nulos sin restringir esos criterios")
    void filtrarMaceradores_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.filtrarMaceradores(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(maceradorEntity), pageable, 1));

        // === EJECUCION ===
        Page<MaceradorResponseDTO> resultado = maceradorServicio.filtrarMaceradores(null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        verify(maceradorRepository).filtrarMaceradores(null, null, pageable);
    }

    @Test
    @DisplayName("CP-FMa-03: filtrarMaceradores retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarMaceradores_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(maceradorRepository.filtrarMaceradores("INEXISTENTE", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<MaceradorResponseDTO> resultado = maceradorServicio.filtrarMaceradores("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(maceradorRepository).filtrarMaceradores("INEXISTENTE", null, pageable);
    }

    @Test
    @DisplayName("CP-FMa-04: filtrarMaceradores normaliza el identificador interno buscado (espacios y mayúsculas) antes de consultar")
    void filtrarMaceradores_debeNormalizarElIdentificadorBuscado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(maceradorRepository.filtrarMaceradores("MAC 01", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        maceradorServicio.filtrarMaceradores("  mac   01  ", null, pageable);

        // === ASSERTS ===
        verify(maceradorRepository).filtrarMaceradores("MAC 01", null, pageable);
    }

    @Test
    @DisplayName("CP-FMa-05: filtrarMaceradores trata el identificador interno formado solo por espacios como ausencia de filtro")
    void filtrarMaceradores_debeTratarIdentificadorEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(maceradorRepository.filtrarMaceradores(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        maceradorServicio.filtrarMaceradores("    ", null, pageable);

        // === ASSERTS ===
        verify(maceradorRepository).filtrarMaceradores(null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("buscarPorId retorna el DTO del macerador cuando el ID existe")
    void buscarPorId_debeRetornarMaceradorExistente() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));

        // === EJECUCION ===
        MaceradorResponseDTO resultado = maceradorServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertMaceradorDTO(maceradorEntity, resultado);
        verify(maceradorRepository).findById(1L);
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para maceradores dados de baja
        when(maceradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maceradorServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el macerador con ID: 99");
        verify(maceradorRepository).findById(99L);
    }

    // ==================== altaMacerador ====================

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es mayor a la total")
    void altaMacerador_debeRechazarCapacidadUtilMayorATotal() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 120.0, 5.0, 75.0);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException cuando la capacidad útil es igual a la total (valor límite)")
    void altaMacerador_debeRechazarCapacidadUtilIgualATotal() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 100.0, 5.0, 75.0);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es nula")
    void altaMacerador_debeRechazarCapacidadTotalNula() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", null, 80.0, 5.0, 75.0);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total es obligatoria");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la eficiencia de maceración es nula")
    void altaMacerador_debeRechazarEficienciaNula() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, null);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración es obligatoria");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException cuando la eficiencia está por debajo del mínimo (límite inferior)")
    void altaMacerador_debeRechazarEficienciaPorDebajoDelMinimo() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 39.9);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException cuando la eficiencia está por encima del máximo (límite superior)")
    void altaMacerador_debeRechazarEficienciaPorEncimaDelMaximo() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 100.1);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza RecursoDuplicadoException y no persiste cuando el identificador interno ya existe")
    void altaMacerador_debeRechazarIdentificadorDuplicado() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 75.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MAC-01")).thenReturn(true);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'MAC-01'");

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("MAC-01");
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaMacerador lanza RecursoDuplicadoException cuando el identificador ya existe con distinto case (case-insensitive)")
    void altaMacerador_debeRechazarIdentificadorDuplicadoCaseInsensitive() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("mac-01", 100.0, 80.0, 5.0, 75.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MAC-01")).thenReturn(true);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("MAC-01");
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaMacerador persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaMacerador_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-02", 100.0, 80.0, 5.0, 75.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MAC-02")).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> {
            MaceradorEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        MaceradorResponseDTO resultado = maceradorServicio.altaMacerador(maceradorFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<MaceradorEntity> captor = ArgumentCaptor.forClass(MaceradorEntity.class);
        verify(maceradorRepository).save(captor.capture());
        MaceradorEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("MAC-02");
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(entidadCapturada.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(entidadCapturada.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(entidadCapturada.getEspacioMuerto()).isEqualTo(5.0);
        assertThat(entidadCapturada.getEficienciaMaceracion()).isEqualTo(75.0);
        assertThat(entidadCapturada.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        // El alta siempre debe registrar al macerador como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MAC-02");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(resultado.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("MAC-02");
    }

    @Test
    @DisplayName("altaMacerador acepta 40.0 como borde válido inferior de la eficiencia de maceración")
    void altaMacerador_debeAceptarEficienciaEnLimiteInferior() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-03", 100.0, 80.0, 5.0, 40.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MAC-03")).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaceradorResponseDTO resultado = maceradorServicio.altaMacerador(maceradorFormDTO);

        assertThat(resultado.getEficienciaMaceracion()).isEqualTo(40.0);
        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("altaMacerador acepta 100.0 como borde válido superior de la eficiencia de maceración")
    void altaMacerador_debeAceptarEficienciaEnLimiteSuperior() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-04", 100.0, 80.0, 5.0, 100.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MAC-04")).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaceradorResponseDTO resultado = maceradorServicio.altaMacerador(maceradorFormDTO);

        assertThat(resultado.getEficienciaMaceracion()).isEqualTo(100.0);
        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-12: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son nulos")
    void altaMacerador_debeRechazarUsosMaximosAntesMantenimientoNulo() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 75.0, null);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento son obligatorios");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-13: altaMacerador lanza ReglaNegocioException cuando los usos máximos antes de mantenimiento son iguales a cero (valor límite)")
    void altaMacerador_debeRechazarUsosMaximosAntesMantenimientoIgualACero() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 75.0, 0);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(maceradorRepository);
    }

    // ==================== modificarMacerador ====================

    @Test
    @DisplayName("modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad útil es inválida")
    void modificarMacerador_debeRechazarCapacidadUtilInvalida() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 120.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // La existencia del macerador se valida antes que las reglas de negocio sobre sus datos
        verify(maceradorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando la eficiencia es inválida")
    void modificarMacerador_debeRechazarEficienciaInvalida() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 30.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(maceradorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMacerador lanza RecursoDuplicadoException y no persiste cuando el identificador está en uso por otro Equipamiento")
    void modificarMacerador_debeRechazarIdentificadorEnUsoPorOtroEquipamiento() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-EXISTENTE", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-EXISTENTE", 1L)).thenReturn(true);

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El identificador interno \'MAC-EXISTENTE\' ya está en uso por otro equipamiento");

        verify(maceradorRepository).findById(1L);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-EXISTENTE", 1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMacerador lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarMacerador_debeLanzarExcepcionSiNoExiste() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(99L, maceradorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el macerador con ID: 99");

        verify(maceradorRepository).findById(99L);
        verify(maceradorRepository, never()).save(any());
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("modificarMacerador actualiza los datos y persiste cuando el ID existe y el identificador está libre (camino feliz)")
    void modificarMacerador_debeActualizarMaceradorExistente() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-VIEJO", 90.0, 70.0, 4.0, 60.0);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-NUEVO", 100.0, 80.0, 5.0, 85.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-NUEVO", 1L)).thenReturn(false);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);
        when(maceradorRepository.save(maceradorEntity)).thenReturn(maceradorEntity);

        // === EJECUCION ===
        MaceradorResponseDTO resultado = maceradorServicio.modificarMacerador(1L, maceradorFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MAC-NUEVO");
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(resultado.getEspacioMuerto()).isEqualTo(5.0);
        assertThat(resultado.getEficienciaMaceracion()).isEqualTo(85.0);
        assertThat(resultado.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        verify(maceradorRepository).findById(1L);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-NUEVO", 1L);
        verify(etapaLoteRepository).existsLotePendienteAsociado(1L);
        verify(maceradorRepository).save(maceradorEntity);
    }

    @Test
    @DisplayName("modificarMacerador permite conservar el propio identificador actual al actualizar otros campos")
    void modificarMacerador_debePermitirConservarIdentificadorPropio() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0);
        // Mismo identificador (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("mac-01", 100.0, 80.0, 5.0, 85.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-01", 1L)).thenReturn(false);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);
        when(maceradorRepository.save(maceradorEntity)).thenReturn(maceradorEntity);

        MaceradorResponseDTO resultado = maceradorServicio.modificarMacerador(1L, maceradorFormDTO);

        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MAC-01");
        assertThat(resultado.getEficienciaMaceracion()).isEqualTo(85.0);
        verify(maceradorRepository).save(maceradorEntity);
    }

    @Test
    @DisplayName("CP-MM-06: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento son inválidos")
    void modificarMacerador_debeRechazarUsosMaximosAntesMantenimientoInvalido() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 75.0, 0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verify(maceradorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-07: modificarMacerador lanza ReglaNegocioException y no persiste cuando el macerador tiene comprometida una etapa en estado Pendiente de un lote")
    void modificarMacerador_debeRechazarAsociacionALotePendiente() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 85.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-01", 1L)).thenReturn(false);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(true);

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede modificar el macerador porque tiene comprometida una etapa en estado Pendiente de un lote");

        verify(etapaLoteRepository).existsLotePendienteAsociado(1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-08: modificarMacerador lanza ReglaNegocioException y no persiste cuando el macerador se encuentra en uso")
    void modificarMacerador_debeRechazarSiEstaEnUso() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0, EstadoOperativo.EN_USO);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 85.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-01", 1L)).thenReturn(false);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede modificar el macerador porque se encuentra en uso");

        verify(maceradorRepository, never()).save(any());
    }

    // ==================== bajaMacerador ====================

    @Test
    @DisplayName("bajaMacerador marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe")
    void bajaMacerador_debeMarcarBajaYRetornarMaceradorExistente() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);
        when(maceradorRepository.save(maceradorEntity)).thenReturn(maceradorEntity);

        MaceradorResponseDTO resultado = maceradorServicio.bajaMacerador(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(maceradorEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertMaceradorDTO(maceradorEntity, resultado);
        verify(maceradorRepository).findById(1L);
        verify(etapaLoteRepository).existsLotePendienteAsociado(1L);
        verify(maceradorRepository).save(maceradorEntity);
        verify(maceradorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("bajaMacerador lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaMacerador_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(maceradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maceradorServicio.bajaMacerador(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el macerador con ID: 99");

        verify(maceradorRepository).findById(99L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-03: bajaMacerador lanza ReglaNegocioException y no persiste cuando el macerador tiene comprometida una etapa en estado Pendiente de un lote")
    void bajaMacerador_debeRechazarAsociacionALotePendiente() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(true);

        assertThatThrownBy(() -> maceradorServicio.bajaMacerador(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el macerador porque tiene comprometida una etapa en estado Pendiente de un lote");

        verify(etapaLoteRepository).existsLotePendienteAsociado(1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-04: bajaMacerador lanza ReglaNegocioException y no persiste cuando el macerador se encuentra en uso")
    void bajaMacerador_debeRechazarSiEstaEnUso() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 5.0, 75.0, EstadoOperativo.EN_USO);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> maceradorServicio.bajaMacerador(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el macerador porque no se encuentra disponible");

        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-05: bajaMacerador lanza ReglaNegocioException y no persiste cuando el macerador se encuentra en limpieza")
    void bajaMacerador_debeRechazarSiEstaEnLimpieza() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 5.0, 75.0, EstadoOperativo.EN_LIMPIEZA);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> maceradorServicio.bajaMacerador(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el macerador porque no se encuentra disponible");

        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-06: bajaMacerador lanza ReglaNegocioException y no persiste cuando el macerador se encuentra en mantenimiento")
    void bajaMacerador_debeRechazarSiEstaEnMantenimiento() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 5.0, 75.0, EstadoOperativo.EN_MANTENIMIENTO);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> maceradorServicio.bajaMacerador(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el macerador porque no se encuentra disponible");

        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-07: bajaMacerador informa primero la etapa en estado Pendiente de un lote cuando el macerador además no está disponible")
    void bajaMacerador_debePriorizarEtapaPendienteSobreNoDisponible() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 5.0, 75.0, EstadoOperativo.EN_USO);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(true);

        assertThatThrownBy(() -> maceradorServicio.bajaMacerador(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el macerador porque tiene comprometida una etapa en estado Pendiente de un lote");

        verify(maceradorRepository, never()).save(any());
    }

    // ==================== validaciones técnicas de Macerador (números finitos, techos y longitudes) ====================

    @Test
    @DisplayName("CP-14: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es NaN")
    void altaMacerador_debeRechazar_capacidadTotalNaN() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", Double.NaN, 80.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser un número mayor a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-15: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es infinita positiva")
    void altaMacerador_debeRechazar_capacidadTotalInfinita() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", Double.POSITIVE_INFINITY, 80.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser un número mayor a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-16: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es igual a cero (valor límite)")
    void altaMacerador_debeRechazar_capacidadTotalCero() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 0.0, -1.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser un número mayor a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-17: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es negativa")
    void altaMacerador_debeRechazar_capacidadTotalNegativa() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", -1.0, -2.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser un número mayor a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-18: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total supera 100.000 litros")
    void altaMacerador_debeRechazar_capacidadTotalSuperaElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100000.01, 80.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-19: altaMacerador persiste con éxito cuando la capacidad total es exactamente 100.000 litros (valor límite válido)")
    void altaMacerador_debeAceptar_capacidadTotalEnElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100000.0, 99999.0, 10.0, 75.0, 500);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MAC-01")).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.altaMacerador(dto)).doesNotThrowAnyException();

        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-41: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es nula")
    void altaMacerador_debeRechazar_capacidadUtilNula() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, null, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil es obligatoria");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-20: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es NaN")
    void altaMacerador_debeRechazar_capacidadUtilNaN() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, Double.NaN, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser un número mayor a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-21: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es igual a cero (valor límite)")
    void altaMacerador_debeRechazar_capacidadUtilCero() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 0.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser un número mayor a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-22: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es negativa")
    void altaMacerador_debeRechazar_capacidadUtilNegativa() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, -1.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser un número mayor a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-24: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son negativos")
    void altaMacerador_debeRechazar_usosNegativos() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 10.0, 75.0, -1);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-25: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento superan 100.000")
    void altaMacerador_debeRechazar_usosSuperanElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 10.0, 75.0, 100001);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-26: altaMacerador persiste con éxito cuando los usos máximos antes de mantenimiento son exactamente 100.000 (valor límite válido)")
    void altaMacerador_debeAceptar_usosEnElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 10.0, 75.0, 100000);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MAC-01")).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.altaMacerador(dto)).doesNotThrowAnyException();

        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-27: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el identificador interno tiene 101 caracteres")
    void altaMacerador_debeRechazar_identificadorDe101Caracteres() {
        MaceradorFormDTO dto = maceradorFormDTO("A".repeat(101), 100.0, 80.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El identificador interno no puede superar 100 caracteres");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-28: altaMacerador persiste con éxito cuando el identificador interno tiene exactamente 100 caracteres (valor límite válido)")
    void altaMacerador_debeAceptar_identificadorDe100Caracteres() {
        MaceradorFormDTO dto = maceradorFormDTO("A".repeat(100), 100.0, 80.0, 10.0, 75.0, 500);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("A".repeat(100))).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.altaMacerador(dto)).doesNotThrowAnyException();

        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-29: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es nulo")
    void altaMacerador_debeRechazar_espacioMuertoNulo() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, null, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto es obligatorio");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-30: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es NaN")
    void altaMacerador_debeRechazar_espacioMuertoNaN() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, Double.NaN, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser un número mayor o igual a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-31: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es infinito positivo")
    void altaMacerador_debeRechazar_espacioMuertoInfinito() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, Double.POSITIVE_INFINITY, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser un número mayor o igual a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-32: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es negativo")
    void altaMacerador_debeRechazar_espacioMuertoNegativo() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, -1.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser un número mayor o igual a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-33: altaMacerador persiste con éxito cuando el espacio muerto es igual a cero (valor límite válido)")
    void altaMacerador_debeAceptar_espacioMuertoCero() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 0.0, 75.0, 500);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MAC-01")).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.altaMacerador(dto)).doesNotThrowAnyException();

        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-34: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es igual a la capacidad útil (valor límite)")
    void altaMacerador_debeRechazar_espacioMuertoIgualACapacidadUtil() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 80.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser menor a la capacidad útil");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-35: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es mayor a la capacidad útil")
    void altaMacerador_debeRechazar_espacioMuertoMayorACapacidadUtil() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 90.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser menor a la capacidad útil");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-36: altaMacerador persiste con éxito cuando el espacio muerto es apenas menor a la capacidad útil (valor límite válido)")
    void altaMacerador_debeAceptar_espacioMuertoJustoDebajoDeCapacidadUtil() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 79.99, 75.0, 500);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("MAC-01")).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.altaMacerador(dto)).doesNotThrowAnyException();

        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-37: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto supera 100.000 litros")
    void altaMacerador_debeRechazar_espacioMuertoSuperaElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100000.0, 99999.0, 100000.01, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto no puede superar 100.000 litros");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-38: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la eficiencia de maceración es NaN")
    void altaMacerador_debeRechazar_eficienciaNaN() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 10.0, Double.NaN, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe ser un número mayor a 0");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-39: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la eficiencia de maceración es infinita positiva")
    void altaMacerador_debeRechazar_eficienciaInfinita() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 10.0, Double.POSITIVE_INFINITY, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-40: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la eficiencia de maceración es infinita negativa")
    void altaMacerador_debeRechazar_eficienciaInfinitaNegativa() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 10.0, Double.NEGATIVE_INFINITY, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100");

        verifyNoInteractions(maceradorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("CP-MM-09: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad total es NaN")
    void modificarMacerador_debeRechazar_capacidadTotalNaN() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", Double.NaN, 80.0, 10.0, 75.0, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser un número mayor a 0");

        verify(maceradorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-10: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad total supera 100.000 litros")
    void modificarMacerador_debeRechazar_capacidadTotalSuperaElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100000.01, 80.0, 10.0, 75.0, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");

        verify(maceradorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-11: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando el espacio muerto es NaN")
    void modificarMacerador_debeRechazar_espacioMuertoNaN() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, Double.NaN, 75.0, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser un número mayor o igual a 0");

        verify(maceradorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-12: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando el espacio muerto es igual a la capacidad útil (valor límite)")
    void modificarMacerador_debeRechazar_espacioMuertoIgualACapacidadUtil() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 80.0, 75.0, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser menor a la capacidad útil");

        verify(maceradorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-13: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando la eficiencia de maceración es NaN")
    void modificarMacerador_debeRechazar_eficienciaNaN() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 10.0, Double.NaN, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe ser un número mayor a 0");

        verify(maceradorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-14: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento superan 100.000")
    void modificarMacerador_debeRechazar_usosSuperanElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO("MAC-01", 100.0, 80.0, 10.0, 75.0, 100001);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verify(maceradorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-15: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando el identificador interno tiene 101 caracteres")
    void modificarMacerador_debeRechazar_identificadorDe101Caracteres() {
        MaceradorFormDTO dto = maceradorFormDTO("A".repeat(101), 100.0, 80.0, 10.0, 75.0, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El identificador interno no puede superar 100 caracteres");

        verify(maceradorRepository).findById(1L);
        verifyNoInteractions(equipamientoRepository);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-16: modificarMacerador persiste con éxito cuando el identificador interno tiene exactamente 100 caracteres (valor límite válido)")
    void modificarMacerador_debeAceptar_identificadorDe100Caracteres() {
        MaceradorFormDTO dto = maceradorFormDTO("A".repeat(100), 100.0, 80.0, 10.0, 75.0, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("A".repeat(100), 1L)).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.modificarMacerador(1L, dto)).doesNotThrowAnyException();

        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    // ==================== helpers ====================

    private static MaceradorEntity crearMaceradorEntity(Long id, String identificadorInterno, Double capacidadTotal,
                                                          Double capacidadUtil, Double espacioMuerto, Double eficienciaMaceracion) {
        return crearMaceradorEntity(id, identificadorInterno, capacidadTotal, capacidadUtil, espacioMuerto, eficienciaMaceracion, EstadoOperativo.DISPONIBLE);
    }

    private static MaceradorEntity crearMaceradorEntity(Long id, String identificadorInterno, Double capacidadTotal,
                                                          Double capacidadUtil, Double espacioMuerto, Double eficienciaMaceracion,
                                                          EstadoOperativo estadoOperativo) {
        return MaceradorEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .estadoOperativo(estadoOperativo)
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .espacioMuerto(espacioMuerto)
                .eficienciaMaceracion(eficienciaMaceracion)
                .usosMaximosAntesMantenimiento(USOS_MAXIMOS_ANTES_MANTENIMIENTO)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static MaceradorFormDTO maceradorFormDTO(String identificadorInterno, Double capacidadTotal,
                                                       Double capacidadUtil, Double espacioMuerto, Double eficienciaMaceracion) {
        return maceradorFormDTO(identificadorInterno, capacidadTotal, capacidadUtil, espacioMuerto, eficienciaMaceracion, USOS_MAXIMOS_ANTES_MANTENIMIENTO);
    }

    private static MaceradorFormDTO maceradorFormDTO(String identificadorInterno, Double capacidadTotal, Double capacidadUtil,
                                                       Double espacioMuerto, Double eficienciaMaceracion, Integer usosMaximosAntesMantenimiento) {
        return MaceradorFormDTO.builder()
                .identificadorInterno(identificadorInterno)
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .espacioMuerto(espacioMuerto)
                .eficienciaMaceracion(eficienciaMaceracion)
                .usosMaximosAntesMantenimiento(usosMaximosAntesMantenimiento)
                .build();
    }

    private static void assertMaceradorDTO(MaceradorEntity entidad, MaceradorResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificadorInterno()).isEqualTo(entidad.getIdentificadorInterno());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getCapacidadTotal()).isEqualTo(entidad.getCapacidadTotal());
        assertThat(dto.getCapacidadUtil()).isEqualTo(entidad.getCapacidadUtil());
        assertThat(dto.getEspacioMuerto()).isEqualTo(entidad.getEspacioMuerto());
        assertThat(dto.getEficienciaMaceracion()).isEqualTo(entidad.getEficienciaMaceracion());
        assertThat(dto.getUsosMaximosAntesMantenimiento()).isEqualTo(entidad.getUsosMaximosAntesMantenimiento());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }

    // ==================== normalización y unicidad ====================

    @Test
    @DisplayName("CP-42: altaMacerador guarda el identificador interno normalizado y consulta la unicidad con ese valor")
    void altaMacerador_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        MaceradorFormDTO formDTO = maceradorFormDTO("  fer   01 ", 100.0, 80.0, 5.0, 75.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER 01")).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        maceradorServicio.altaMacerador(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<MaceradorEntity> captor = ArgumentCaptor.forClass(MaceradorEntity.class);
        verify(maceradorRepository).save(captor.capture());
        assertThat(captor.getValue().getIdentificadorInterno()).isEqualTo("FER 01");
        assertThat(formDTO.getIdentificadorInterno()).isEqualTo("  fer   01 ");
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("FER 01");
        verify(maceradorRepository).flush();
    }

    @Test
    @DisplayName("CP-43: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el identificador interno está vacío")
    void altaMacerador_debeRechazarValorVacio() {
        MaceradorFormDTO formDTO = maceradorFormDTO("   ", 100.0, 80.0, 5.0, 75.0);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El identificador interno es obligatorio");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-44: altaMacerador lanza RecursoDuplicadoException cuando el identificador interno normalizado ya existe")
    void altaMacerador_debeDetectarDuplicadoConElValorNormalizado() {
        MaceradorFormDTO formDTO = maceradorFormDTO("  fer-01 ", 100.0, 80.0, 5.0, 75.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER-01")).thenReturn(true);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'FER-01'");

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("FER-01");
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-45: altaMacerador traduce la violación del índice único udx_equipamiento_identificador_interno al mensaje de duplicado")
    void altaMacerador_debeTraducirLaViolacionDelIndiceUnico() {
        MaceradorFormDTO formDTO = maceradorFormDTO("FER 01", 100.0, 80.0, 5.0, 75.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER 01")).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_equipamiento_identificador_interno"));

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'FER 01'");
    }

    @Test
    @DisplayName("CP-46: altaMacerador permite reutilizar el identificador interno de un registro dado de baja")
    void altaMacerador_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        MaceradorFormDTO formDTO = maceradorFormDTO("FER 01", 100.0, 80.0, 5.0, 75.0);
        // El único registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER 01")).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.altaMacerador(formDTO)).doesNotThrowAnyException();

        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-47: altaMacerador permite un identificador que difiere de otro solo en la puntuación")
    void altaMacerador_debeTratarComoDistintosLosIdentificadoresConDistintaPuntuacion() {
        MaceradorFormDTO formDTO = maceradorFormDTO("fer 01", 100.0, 80.0, 5.0, 75.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("FER 01")).thenReturn(false);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.altaMacerador(formDTO)).doesNotThrowAnyException();

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("FER 01");
        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-MM-17: modificarMacerador guarda el identificador interno normalizado y consulta la unicidad con ese valor")
    void modificarMacerador_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        MaceradorFormDTO formDTO = maceradorFormDTO("  fer   01 ", 100.0, 80.0, 5.0, 75.0);
        MaceradorEntity existente = crearMaceradorEntity(1L, "Original", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FER 01", 1L)).thenReturn(false);
        when(maceradorRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        maceradorServicio.modificarMacerador(1L, formDTO);

        // === ASSERTS ===
        assertThat(existente.getIdentificadorInterno()).isEqualTo("FER 01");
        assertThat(formDTO.getIdentificadorInterno()).isEqualTo("  fer   01 ");
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("FER 01", 1L);
        verify(maceradorRepository).save(existente);
        verify(maceradorRepository).flush();
    }

    @Test
    @DisplayName("CP-MM-18: modificarMacerador lanza ReglaNegocioException cuando el identificador interno está vacío")
    void modificarMacerador_debeRechazarValorVacio() {
        MaceradorFormDTO formDTO = maceradorFormDTO("   ", 100.0, 80.0, 5.0, 75.0);
        MaceradorEntity existente = crearMaceradorEntity(1L, "Original", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El identificador interno es obligatorio");

        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-19: modificarMacerador lanza RecursoDuplicadoException cuando el identificador interno normalizado está en uso por otro registro")
    void modificarMacerador_debeDetectarDuplicadoConElValorNormalizado() {
        MaceradorFormDTO formDTO = maceradorFormDTO("  fer-01 ", 100.0, 80.0, 5.0, 75.0);
        MaceradorEntity existente = crearMaceradorEntity(1L, "Original", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FER-01", 1L)).thenReturn(true);

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El identificador interno 'FER-01' ya está en uso por otro equipamiento");

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("FER-01", 1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-20: modificarMacerador traduce la violación del índice único udx_equipamiento_identificador_interno al mensaje de duplicado")
    void modificarMacerador_debeTraducirLaViolacionDelIndiceUnico() {
        MaceradorFormDTO formDTO = maceradorFormDTO("FER 01", 100.0, 80.0, 5.0, 75.0);
        MaceradorEntity existente = crearMaceradorEntity(1L, "Original", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FER 01", 1L)).thenReturn(false);
        when(maceradorRepository.save(existente)).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_equipamiento_identificador_interno"));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El identificador interno 'FER 01' ya está en uso por otro equipamiento");
    }

    @Test
    @DisplayName("CP-MM-21: modificarMacerador permite reutilizar el identificador interno de un registro dado de baja")
    void modificarMacerador_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        MaceradorFormDTO formDTO = maceradorFormDTO("FER 01", 100.0, 80.0, 5.0, 75.0);
        MaceradorEntity existente = crearMaceradorEntity(1L, "Original", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(existente));
        // El único otro registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("FER 01", 1L)).thenReturn(false);
        when(maceradorRepository.save(existente)).thenReturn(existente);

        assertThatCode(() -> maceradorServicio.modificarMacerador(1L, formDTO)).doesNotThrowAnyException();

        assertThat(existente.getIdentificadorInterno()).isEqualTo("FER 01");
        verify(maceradorRepository).save(existente);
    }
}
