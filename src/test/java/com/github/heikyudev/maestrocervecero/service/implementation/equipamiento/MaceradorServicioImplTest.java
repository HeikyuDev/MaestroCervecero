package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MaceradorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MaceradorResponseDTO;
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
class MaceradorServicioImplTest {

    @Mock
    private IMaceradorRepository maceradorRepository;

    @Mock
    private IEquipamientoRepository equipamientoRepository;

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
        when(maceradorRepository.filtrarMaceradores("Inexistente", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<MaceradorResponseDTO> resultado = maceradorServicio.filtrarMaceradores("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(maceradorRepository).filtrarMaceradores("Inexistente", null, pageable);
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
                .hasMessage("No se encontró el Macerador con ID:99");
        verify(maceradorRepository).findById(99L);
    }

    // ==================== altaMacerador ====================

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es mayor a la total")
    void altaMacerador_debeRechazarCapacidadUtilMayorATotal() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 120.0, 5.0, 75.0);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad util no puede ser mayor a la capacidad total.");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException cuando la capacidad útil es igual a la total (valor límite)")
    void altaMacerador_debeRechazarCapacidadUtilIgualATotal() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 100.0, 5.0, 75.0);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad util no puede ser mayor a la capacidad total.");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException cuando la eficiencia está por debajo del mínimo (límite inferior)")
    void altaMacerador_debeRechazarEficienciaPorDebajoDelMinimo() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 39.9);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100 inclusive.");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException cuando la eficiencia está por encima del máximo (límite superior)")
    void altaMacerador_debeRechazarEficienciaPorEncimaDelMaximo() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 100.1);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100 inclusive.");

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
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("mac-01")).thenReturn(true);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("mac-01");
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
        assertThat(entidadCapturada.getDescripcion()).isEqualTo("Macerador de prueba");
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(entidadCapturada.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(entidadCapturada.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(entidadCapturada.getEspacioMuerto()).isEqualTo(5.0);
        assertThat(entidadCapturada.getEficienciaMaceracion()).isEqualTo(75.0);
        // El alta siempre debe registrar al macerador como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MAC-02");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
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

    // ==================== modificarMacerador ====================

    @Test
    @DisplayName("modificarMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es inválida")
    void modificarMacerador_debeRechazarCapacidadUtilInvalida() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 120.0, 5.0, 75.0);

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("modificarMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la eficiencia es inválida")
    void modificarMacerador_debeRechazarEficienciaInvalida() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 30.0);

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("modificarMacerador lanza RecursoDuplicadoException y no busca ni persiste cuando el identificador está en uso por otro Equipamiento")
    void modificarMacerador_debeRechazarIdentificadorEnUsoPorOtroEquipamiento() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-EXISTENTE", 100.0, 80.0, 5.0, 75.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-EXISTENTE", 1L)).thenReturn(true);

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'MAC-EXISTENTE'");

        // La verificación de duplicados se ejecuta antes de localizar la entidad por ID
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-EXISTENTE", 1L);
        verify(maceradorRepository, never()).findById(any());
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMacerador lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarMacerador_debeLanzarExcepcionSiNoExiste() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-01", 100.0, 80.0, 5.0, 75.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-01", 99L)).thenReturn(false);
        when(maceradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(99L, maceradorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el Macerador con ID:99");

        verify(maceradorRepository).findById(99L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMacerador actualiza los datos y persiste cuando el ID existe y el identificador está libre (camino feliz)")
    void modificarMacerador_debeActualizarMaceradorExistente() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-VIEJO", 90.0, 70.0, 4.0, 60.0);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("MAC-NUEVO", 100.0, 80.0, 5.0, 85.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-NUEVO", 1L)).thenReturn(false);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(maceradorRepository.save(maceradorEntity)).thenReturn(maceradorEntity);

        // === EJECUCION ===
        MaceradorResponseDTO resultado = maceradorServicio.modificarMacerador(1L, maceradorFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MAC-NUEVO");
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(resultado.getEspacioMuerto()).isEqualTo(5.0);
        assertThat(resultado.getEficienciaMaceracion()).isEqualTo(85.0);
        verify(maceradorRepository).findById(1L);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("MAC-NUEVO", 1L);
        verify(maceradorRepository).save(maceradorEntity);
    }

    @Test
    @DisplayName("modificarMacerador permite conservar el propio identificador actual al actualizar otros campos")
    void modificarMacerador_debePermitirConservarIdentificadorPropio() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0);
        // Mismo identificador (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO("mac-01", 100.0, 80.0, 5.0, 85.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("mac-01", 1L)).thenReturn(false);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(maceradorRepository.save(maceradorEntity)).thenReturn(maceradorEntity);

        MaceradorResponseDTO resultado = maceradorServicio.modificarMacerador(1L, maceradorFormDTO);

        assertThat(resultado.getIdentificadorInterno()).isEqualTo("mac-01");
        assertThat(resultado.getEficienciaMaceracion()).isEqualTo(85.0);
        verify(maceradorRepository).save(maceradorEntity);
    }

    // ==================== bajaMacerador ====================

    @Test
    @DisplayName("bajaMacerador marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe")
    void bajaMacerador_debeMarcarBajaYRetornarMaceradorExistente() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(maceradorRepository.save(maceradorEntity)).thenReturn(maceradorEntity);

        MaceradorResponseDTO resultado = maceradorServicio.bajaMacerador(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(maceradorEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertMaceradorDTO(maceradorEntity, resultado);
        verify(maceradorRepository).findById(1L);
        verify(maceradorRepository).save(maceradorEntity);
        verify(maceradorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("bajaMacerador lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaMacerador_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(maceradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maceradorServicio.bajaMacerador(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el Macerador con ID:99");

        verify(maceradorRepository).findById(99L);
        verify(maceradorRepository, never()).save(any());
    }

    // ==================== helpers ====================

    private static MaceradorEntity crearMaceradorEntity(Long id, String identificadorInterno, Double capacidadTotal,
                                                          Double capacidadUtil, Double espacioMuerto, Double eficienciaMaceracion) {
        return MaceradorEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .descripcion("Macerador de prueba")
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .espacioMuerto(espacioMuerto)
                .eficienciaMaceracion(eficienciaMaceracion)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static MaceradorFormDTO maceradorFormDTO(String identificadorInterno, Double capacidadTotal,
                                                       Double capacidadUtil, Double espacioMuerto, Double eficienciaMaceracion) {
        return MaceradorFormDTO.builder()
                .identificadorInterno(identificadorInterno)
                .descripcion("Macerador de prueba")
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .espacioMuerto(espacioMuerto)
                .eficienciaMaceracion(eficienciaMaceracion)
                .build();
    }

    private static void assertMaceradorDTO(MaceradorEntity entidad, MaceradorResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificadorInterno()).isEqualTo(entidad.getIdentificadorInterno());
        assertThat(dto.getDescripcion()).isEqualTo(entidad.getDescripcion());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getCapacidadTotal()).isEqualTo(entidad.getCapacidadTotal());
        assertThat(dto.getCapacidadUtil()).isEqualTo(entidad.getCapacidadUtil());
        assertThat(dto.getEspacioMuerto()).isEqualTo(entidad.getEspacioMuerto());
        assertThat(dto.getEficienciaMaceracion()).isEqualTo(entidad.getEficienciaMaceracion());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
