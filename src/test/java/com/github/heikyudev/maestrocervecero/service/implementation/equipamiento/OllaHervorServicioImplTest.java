package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IEquipamientoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.OllaHervorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OllaHervorResponseDTO;
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
class OllaHervorServicioImplTest {

    @Mock
    private IOllaHervorRepository ollaHervorRepository;

    @Mock
    private IEquipamientoRepository equipamientoRepository;

    @InjectMocks
    private OllaHervorServicioImpl ollaHervorServicio;

    // ==================== filtrarOllasHervor ====================

    @Test
    @DisplayName("CP-FOH-01: filtrarOllasHervor retorna una página de ollas de hervor correctamente mapeada a DTO cuando se filtra por identificador interno y estado operativo")
    void filtrarOllasHervor_debeRetornarPaginaMapeadaFiltrandoPorIdentificadorYEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 100.0, 80.0, 10.0, 3.0);
        OllaHervorEntity otraOllaHervorEntity = crearOllaHervorEntity(2L, "OLLA-02", 120.0, 90.0, 12.0, 4.0);
        when(ollaHervorRepository.filtrarOllasHervor("OLLA", EstadoOperativo.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(ollaHervorEntity, otraOllaHervorEntity), pageable, 2));

        // === EJECUCION ===
        Page<OllaHervorResponseDTO> resultado = ollaHervorServicio.filtrarOllasHervor("OLLA", EstadoOperativo.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertOllaHervorDTO(ollaHervorEntity, resultado.getContent().get(0));
        assertOllaHervorDTO(otraOllaHervorEntity, resultado.getContent().get(1));
        verify(ollaHervorRepository).filtrarOllasHervor("OLLA", EstadoOperativo.DISPONIBLE, pageable);
    }

    @Test
    @DisplayName("CP-FOH-02: filtrarOllasHervor propaga identificador interno y estado operativo nulos sin restringir esos criterios")
    void filtrarOllasHervor_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 100.0, 80.0, 10.0, 3.0);
        OllaHervorEntity otraOllaHervorEntity = crearOllaHervorEntity(2L, "OLLA-02", 120.0, 90.0, 12.0, 4.0);
        when(ollaHervorRepository.filtrarOllasHervor(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(ollaHervorEntity, otraOllaHervorEntity), pageable, 2));

        // === EJECUCION ===
        Page<OllaHervorResponseDTO> resultado = ollaHervorServicio.filtrarOllasHervor(null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(ollaHervorRepository).filtrarOllasHervor(null, null, pageable);
    }

    @Test
    @DisplayName("CP-FOH-03: filtrarOllasHervor retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarOllasHervor_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ollaHervorRepository.filtrarOllasHervor("Inexistente", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<OllaHervorResponseDTO> resultado = ollaHervorServicio.filtrarOllasHervor("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(ollaHervorRepository).filtrarOllasHervor("Inexistente", null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("buscarPorId retorna el DTO de la olla de hervor cuando el ID existe")
    void buscarPorId_debeRetornarOllaHervorExistente() {
        // === PREPARACION DE DATOS ===
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 100.0, 80.0, 10.0, 3.0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));

        // === EJECUCION ===
        OllaHervorResponseDTO resultado = ollaHervorServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertOllaHervorDTO(ollaHervorEntity, resultado);
        verify(ollaHervorRepository).findById(1L);
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para ollas de hervor dadas de baja
        when(ollaHervorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ollaHervorServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la Olla de Hervor con ID:99");
        verify(ollaHervorRepository).findById(99L);
    }

    // ==================== altaOllaHervor ====================

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es mayor a la total")
    void altaOllaHervor_debeRechazarCapacidadUtilMayorATotal() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-01", 100.0, 120.0, 10.0, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad util no puede ser mayor a la capacidad total.");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(ollaHervorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException cuando la capacidad útil es igual a la total (valor límite)")
    void altaOllaHervor_debeRechazarCapacidadUtilIgualATotal() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-01", 100.0, 100.0, 10.0, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad util no puede ser mayor a la capacidad total.");

        verifyNoInteractions(ollaHervorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException cuando el porcentaje de evaporación es negativo (límite inferior)")
    void altaOllaHervor_debeRechazarEvaporacionNegativa() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-01", 100.0, 80.0, -0.1, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El porcentaje de evaporación debe estar entre 0 y 100.");

        verifyNoInteractions(ollaHervorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException cuando el porcentaje de evaporación supera el 100% (límite superior)")
    void altaOllaHervor_debeRechazarEvaporacionMayorA100() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-01", 100.0, 80.0, 100.1, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El porcentaje de evaporación debe estar entre 0 y 100.");

        verifyNoInteractions(ollaHervorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la pérdida por trub es negativa")
    void altaOllaHervor_debeRechazarPerdidaPorTrubNegativa() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-01", 100.0, 80.0, 10.0, -0.1);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La pérdida por trub no puede ser negativa.");

        verifyNoInteractions(ollaHervorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza RecursoDuplicadoException y no persiste cuando el identificador interno ya existe")
    void altaOllaHervor_debeRechazarIdentificadorDuplicado() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-01", 100.0, 80.0, 10.0, 3.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("OLLA-01")).thenReturn(true);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'OLLA-01'");

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("OLLA-01");
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaOllaHervor lanza RecursoDuplicadoException cuando el identificador ya existe con distinto case (case-insensitive)")
    void altaOllaHervor_debeRechazarIdentificadorDuplicadoCaseInsensitive() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("olla-01", 100.0, 80.0, 10.0, 3.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("olla-01")).thenReturn(true);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("olla-01");
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaOllaHervor persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaOllaHervor_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-02", 100.0, 80.0, 10.0, 3.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("OLLA-02")).thenReturn(false);
        when(ollaHervorRepository.save(any(OllaHervorEntity.class))).thenAnswer(invocation -> {
            OllaHervorEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        OllaHervorResponseDTO resultado = ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<OllaHervorEntity> captor = ArgumentCaptor.forClass(OllaHervorEntity.class);
        verify(ollaHervorRepository).save(captor.capture());
        OllaHervorEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("OLLA-02");
        assertThat(entidadCapturada.getDescripcion()).isEqualTo("Olla de hervor de prueba");
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(entidadCapturada.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(entidadCapturada.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(entidadCapturada.getEvaporacion()).isEqualTo(10.0);
        assertThat(entidadCapturada.getPerdidaPorTrub()).isEqualTo(3.0);
        // El alta siempre debe registrar a la olla de hervor como ACTIVA, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("OLLA-02");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCase("OLLA-02");
    }

    @Test
    @DisplayName("altaOllaHervor acepta 0.0 como borde válido inferior del porcentaje de evaporación")
    void altaOllaHervor_debeAceptarEvaporacionEnLimiteInferior() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-03", 100.0, 80.0, 0.0, 3.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("OLLA-03")).thenReturn(false);
        when(ollaHervorRepository.save(any(OllaHervorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OllaHervorResponseDTO resultado = ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO);

        assertThat(resultado.getEvaporacion()).isEqualTo(0.0);
        verify(ollaHervorRepository).save(any(OllaHervorEntity.class));
    }

    @Test
    @DisplayName("altaOllaHervor acepta 100.0 como borde válido superior del porcentaje de evaporación")
    void altaOllaHervor_debeAceptarEvaporacionEnLimiteSuperior() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-04", 100.0, 80.0, 100.0, 3.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("OLLA-04")).thenReturn(false);
        when(ollaHervorRepository.save(any(OllaHervorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OllaHervorResponseDTO resultado = ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO);

        assertThat(resultado.getEvaporacion()).isEqualTo(100.0);
        verify(ollaHervorRepository).save(any(OllaHervorEntity.class));
    }

    @Test
    @DisplayName("altaOllaHervor acepta 0.0 como borde válido inferior de la pérdida por trub")
    void altaOllaHervor_debeAceptarPerdidaPorTrubEnLimiteInferior() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-05", 100.0, 80.0, 10.0, 0.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCase("OLLA-05")).thenReturn(false);
        when(ollaHervorRepository.save(any(OllaHervorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OllaHervorResponseDTO resultado = ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO);

        assertThat(resultado.getPerdidaPorTrub()).isEqualTo(0.0);
        verify(ollaHervorRepository).save(any(OllaHervorEntity.class));
    }

    // ==================== modificarOllaHervor ====================

    @Test
    @DisplayName("modificarOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es inválida")
    void modificarOllaHervor_debeRechazarCapacidadUtilInvalida() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-01", 100.0, 110.0, 10.0, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(ollaHervorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("modificarOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando el porcentaje de evaporación es inválido")
    void modificarOllaHervor_debeRechazarEvaporacionInvalida() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-01", 100.0, 80.0, 105.0, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verifyNoInteractions(ollaHervorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("modificarOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la pérdida por trub es negativa")
    void modificarOllaHervor_debeRechazarPerdidaPorTrubInvalida() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-01", 100.0, 80.0, 10.0, -1.0);

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verifyNoInteractions(ollaHervorRepository);
        verifyNoInteractions(equipamientoRepository);
    }

    @Test
    @DisplayName("modificarOllaHervor lanza RecursoDuplicadoException y no busca ni persiste cuando el identificador está en uso por otro Equipamiento")
    void modificarOllaHervor_debeRechazarIdentificadorEnUsoPorOtroEquipamiento() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-EXISTENTE", 100.0, 80.0, 10.0, 3.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("OLLA-EXISTENTE", 1L)).thenReturn(true);

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un equipamiento con el identificador interno 'OLLA-EXISTENTE'");

        // La verificación de duplicados se ejecuta antes de localizar la entidad por ID
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("OLLA-EXISTENTE", 1L);
        verify(ollaHervorRepository, never()).findById(any());
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarOllaHervor lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarOllaHervor_debeLanzarExcepcionSiNoExiste() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-01", 100.0, 80.0, 10.0, 3.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("OLLA-01", 99L)).thenReturn(false);
        when(ollaHervorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(99L, ollaHervorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la Olla de Hervor con ID:99");

        verify(ollaHervorRepository).findById(99L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarOllaHervor actualiza los datos y persiste cuando el ID existe y el identificador está libre (camino feliz)")
    void modificarOllaHervor_debeActualizarOllaHervorExistente() {
        // === PREPARACION DE DATOS ===
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-VIEJA", 90.0, 70.0, 8.0, 2.0);
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("OLLA-NUEVA", 100.0, 80.0, 10.0, 3.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("OLLA-NUEVA", 1L)).thenReturn(false);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(ollaHervorRepository.save(ollaHervorEntity)).thenReturn(ollaHervorEntity);

        // === EJECUCION ===
        OllaHervorResponseDTO resultado = ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("OLLA-NUEVA");
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(resultado.getEvaporacion()).isEqualTo(10.0);
        assertThat(resultado.getPerdidaPorTrub()).isEqualTo(3.0);
        verify(ollaHervorRepository).findById(1L);
        verify(equipamientoRepository).existsByIdentificadorInternoIgnoreCaseAndIdNot("OLLA-NUEVA", 1L);
        verify(ollaHervorRepository).save(ollaHervorEntity);
    }

    @Test
    @DisplayName("modificarOllaHervor permite conservar el propio identificador actual al actualizar otros campos")
    void modificarOllaHervor_debePermitirConservarIdentificadorPropio() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 90.0, 70.0, 8.0, 2.0);
        // Mismo identificador (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO("olla-01", 100.0, 80.0, 10.0, 3.0);
        when(equipamientoRepository.existsByIdentificadorInternoIgnoreCaseAndIdNot("olla-01", 1L)).thenReturn(false);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(ollaHervorRepository.save(ollaHervorEntity)).thenReturn(ollaHervorEntity);

        OllaHervorResponseDTO resultado = ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO);

        assertThat(resultado.getIdentificadorInterno()).isEqualTo("olla-01");
        assertThat(resultado.getPerdidaPorTrub()).isEqualTo(3.0);
        verify(ollaHervorRepository).save(ollaHervorEntity);
    }

    // ==================== bajaOllaHervor ====================

    @Test
    @DisplayName("bajaOllaHervor marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe")
    void bajaOllaHervor_debeMarcarBajaYRetornarOllaHervorExistente() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 100.0, 80.0, 10.0, 3.0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(ollaHervorRepository.save(ollaHervorEntity)).thenReturn(ollaHervorEntity);

        OllaHervorResponseDTO resultado = ollaHervorServicio.bajaOllaHervor(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(ollaHervorEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertOllaHervorDTO(ollaHervorEntity, resultado);
        verify(ollaHervorRepository).findById(1L);
        verify(ollaHervorRepository).save(ollaHervorEntity);
        verify(ollaHervorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("bajaOllaHervor lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaOllaHervor_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(ollaHervorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ollaHervorServicio.bajaOllaHervor(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la Olla de Hervor con ID:99");

        verify(ollaHervorRepository).findById(99L);
        verify(ollaHervorRepository, never()).save(any());
    }

    // ==================== helpers ====================

    private static OllaHervorEntity crearOllaHervorEntity(Long id, String identificadorInterno, Double capacidadTotal,
                                                            Double capacidadUtil, Double evaporacion, Double perdidaPorTrub) {
        return OllaHervorEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .descripcion("Olla de hervor de prueba")
                .estadoOperativo(EstadoOperativo.DISPONIBLE)
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .evaporacion(evaporacion)
                .perdidaPorTrub(perdidaPorTrub)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static OllaHervorFormDTO ollaHervorFormDTO(String identificadorInterno, Double capacidadTotal,
                                                       Double capacidadUtil, Double evaporacion, Double perdidaPorTrub) {
        return OllaHervorFormDTO.builder()
                .identificadorInterno(identificadorInterno)
                .descripcion("Olla de hervor de prueba")
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .evaporacion(evaporacion)
                .perdidaPorTrub(perdidaPorTrub)
                .build();
    }

    private static void assertOllaHervorDTO(OllaHervorEntity entidad, OllaHervorResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificadorInterno()).isEqualTo(entidad.getIdentificadorInterno());
        assertThat(dto.getDescripcion()).isEqualTo(entidad.getDescripcion());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getCapacidadTotal()).isEqualTo(entidad.getCapacidadTotal());
        assertThat(dto.getCapacidadUtil()).isEqualTo(entidad.getCapacidadUtil());
        assertThat(dto.getEvaporacion()).isEqualTo(entidad.getEvaporacion());
        assertThat(dto.getPerdidaPorTrub()).isEqualTo(entidad.getPerdidaPorTrub());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
