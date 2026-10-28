package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.OllaHervorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ISecuenciaIdentificadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IOllaHervorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.OllaHervorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.OllaHervorResponseDTO;
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
class OllaHervorServicioImplTest {

    private static final int USOS_MAXIMOS_ANTES_MANTENIMIENTO = 500;

    @Mock
    private IOllaHervorRepository ollaHervorRepository;

    @Mock
    private ISecuenciaIdentificadorRepository secuenciaIdentificadorRepository;

    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

    @InjectMocks
    private OllaHervorServicioImpl ollaHervorServicio;

    @BeforeEach
    void secuenciaDelIdentificador() {
        // Valor por defecto de la secuencia para los casos de alta que no verifican el número generado
        lenient().when(secuenciaIdentificadorRepository.siguienteNumeroOllaHervor()).thenReturn(1L);
    }

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
        when(ollaHervorRepository.filtrarOllasHervor("INEXISTENTE", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<OllaHervorResponseDTO> resultado = ollaHervorServicio.filtrarOllasHervor("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(ollaHervorRepository).filtrarOllasHervor("INEXISTENTE", null, pageable);
    }

    @Test
    @DisplayName("CP-FOH-04: filtrarOllasHervor normaliza el identificador interno buscado (espacios y mayúsculas) antes de consultar")
    void filtrarOllasHervor_debeNormalizarElIdentificadorBuscado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ollaHervorRepository.filtrarOllasHervor("OLLA 01", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        ollaHervorServicio.filtrarOllasHervor("  olla   01  ", null, pageable);

        // === ASSERTS ===
        verify(ollaHervorRepository).filtrarOllasHervor("OLLA 01", null, pageable);
    }

    @Test
    @DisplayName("CP-FOH-05: filtrarOllasHervor trata el identificador interno formado solo por espacios como ausencia de filtro")
    void filtrarOllasHervor_debeTratarIdentificadorEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(ollaHervorRepository.filtrarOllasHervor(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        ollaHervorServicio.filtrarOllasHervor("    ", null, pageable);

        // === ASSERTS ===
        verify(ollaHervorRepository).filtrarOllasHervor(null, null, pageable);
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
                .hasMessage("No se encontró la olla de hervor con ID: 99");
        verify(ollaHervorRepository).findById(99L);
    }

    // ==================== altaOllaHervor ====================

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es mayor a la total")
    void altaOllaHervor_debeRechazarCapacidadUtilMayorATotal() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 120.0, 10.0, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException cuando la capacidad útil es igual a la total (valor límite)")
    void altaOllaHervor_debeRechazarCapacidadUtilIgualATotal() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 100.0, 10.0, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la evaporación es nula")
    void altaOllaHervor_debeRechazarEvaporacionNula() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, null, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La evaporación es obligatoria");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la pérdida por trub es nula")
    void altaOllaHervor_debeRechazarPerdidaPorTrubNula() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, null);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La pérdida por trub es obligatoria");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException cuando la evaporación es negativa")
    void altaOllaHervor_debeRechazarEvaporacionNegativa() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, -0.1, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La evaporación debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException cuando la evaporación supera la capacidad útil (límite superior)")
    void altaOllaHervor_debeRechazarEvaporacionMayorALaCapacidadUtil() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 80.1, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La evaporación no puede superar la capacidad útil de la olla");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException cuando la evaporación es igual a cero (límite inferior)")
    void altaOllaHervor_debeRechazarEvaporacionCero() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 0.0, 3.0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La evaporación debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException cuando la pérdida por trub es igual a cero (límite inferior)")
    void altaOllaHervor_debeRechazarPerdidaPorTrubCero() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, 0.0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La pérdida por trub debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException cuando la pérdida por trub supera la capacidad útil (límite superior)")
    void altaOllaHervor_debeRechazarPerdidaPorTrubMayorALaCapacidadUtil() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, 80.1);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La pérdida por trub no puede superar la capacidad útil de la olla");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la pérdida por trub es negativa")
    void altaOllaHervor_debeRechazarPerdidaPorTrubNegativa() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, -0.1);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La pérdida por trub debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("altaOllaHervor persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaOllaHervor_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroOllaHervor()).thenReturn(5L);
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0);
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
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("OLL-05");
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(entidadCapturada.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(entidadCapturada.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(entidadCapturada.getEvaporacion()).isEqualTo(10.0);
        assertThat(entidadCapturada.getPerdidaPorTrub()).isEqualTo(3.0);
        assertThat(entidadCapturada.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        // El alta siempre debe registrar a la olla de hervor como ACTIVA, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("OLL-05");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(resultado.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
    }

    @Test
    @DisplayName("altaOllaHervor acepta 0.1 como borde válido inferior de la evaporación")
    void altaOllaHervor_debeAceptarEvaporacionEnLimiteInferior() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 0.1, 3.0);
        when(ollaHervorRepository.save(any(OllaHervorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OllaHervorResponseDTO resultado = ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO);

        assertThat(resultado.getEvaporacion()).isEqualTo(0.1);
        verify(ollaHervorRepository).save(any(OllaHervorEntity.class));
    }

    @Test
    @DisplayName("altaOllaHervor acepta una evaporación igual a la capacidad útil como borde válido superior")
    void altaOllaHervor_debeAceptarEvaporacionEnLimiteSuperior() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 80.0, 3.0);
        when(ollaHervorRepository.save(any(OllaHervorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OllaHervorResponseDTO resultado = ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO);

        assertThat(resultado.getEvaporacion()).isEqualTo(80.0);
        verify(ollaHervorRepository).save(any(OllaHervorEntity.class));
    }

    @Test
    @DisplayName("altaOllaHervor acepta 0.1 como borde válido inferior de la pérdida por trub")
    void altaOllaHervor_debeAceptarPerdidaPorTrubEnLimiteInferior() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, 0.1);
        when(ollaHervorRepository.save(any(OllaHervorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OllaHervorResponseDTO resultado = ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO);

        assertThat(resultado.getPerdidaPorTrub()).isEqualTo(0.1);
        verify(ollaHervorRepository).save(any(OllaHervorEntity.class));
    }

    @Test
    @DisplayName("CP-AO-14: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son nulos")
    void altaOllaHervor_debeRechazarUsosMaximosAntesMantenimientoNulo() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0, null);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento son obligatorios");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-15: altaOllaHervor lanza ReglaNegocioException cuando los usos máximos antes de mantenimiento son iguales a cero (valor límite)")
    void altaOllaHervor_debeRechazarUsosMaximosAntesMantenimientoIgualACero() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0, 0);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    // ==================== modificarOllaHervor ====================

    @Test
    @DisplayName("modificarOllaHervor lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad útil es inválida")
    void modificarOllaHervor_debeRechazarCapacidadUtilInvalida() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 90.0, 70.0, 8.0, 2.0);
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 110.0, 10.0, 3.0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // La existencia de la olla de hervor se valida antes que las reglas de negocio sobre sus datos
        verify(ollaHervorRepository).findById(1L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarOllaHervor lanza ReglaNegocioException y no consulta la unicidad cuando la evaporación es inválida")
    void modificarOllaHervor_debeRechazarEvaporacionInvalida() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 90.0, 70.0, 8.0, 2.0);
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 105.0, 3.0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(ollaHervorRepository).findById(1L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarOllaHervor lanza ReglaNegocioException y no consulta la unicidad cuando la pérdida por trub es negativa")
    void modificarOllaHervor_debeRechazarPerdidaPorTrubInvalida() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 90.0, 70.0, 8.0, 2.0);
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, -1.0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(ollaHervorRepository).findById(1L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarOllaHervor lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarOllaHervor_debeLanzarExcepcionSiNoExiste() {
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0);
        when(ollaHervorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(99L, ollaHervorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la olla de hervor con ID: 99");

        verify(ollaHervorRepository).findById(99L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarOllaHervor actualiza los datos y persiste cuando el ID existe, sin cambiar el identificador interno (camino feliz)")
    void modificarOllaHervor_debeActualizarOllaHervorExistente() {
        // === PREPARACION DE DATOS ===
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-VIEJA", 90.0, 70.0, 8.0, 2.0);
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(ollaHervorRepository.save(ollaHervorEntity)).thenReturn(ollaHervorEntity);

        // === EJECUCION ===
        OllaHervorResponseDTO resultado = ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("OLLA-VIEJA");
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(resultado.getEvaporacion()).isEqualTo(10.0);
        assertThat(resultado.getPerdidaPorTrub()).isEqualTo(3.0);
        assertThat(resultado.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        verify(ollaHervorRepository).findById(1L);
        verify(ollaHervorRepository).save(ollaHervorEntity);
        // El identificador interno lo asignó el sistema al dar de alta: modificar no lo cambia ni pide otro número
        verifyNoInteractions(secuenciaIdentificadorRepository);
    }

    @Test
    @DisplayName("CP-MO-08: modificarOllaHervor lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento son inválidos")
    void modificarOllaHervor_debeRechazarUsosMaximosAntesMantenimientoInvalido() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 90.0, 70.0, 8.0, 2.0);
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0, 0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verify(ollaHervorRepository).findById(1L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MO-09: modificarOllaHervor permite modificar olla de hervor aunque tenga comprometida una etapa en estado Pendiente de un lote (su escalado queda guardado en la instantánea del lote)")
    void modificarOllaHervor_debePermitirModificarConLotePendiente() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 90.0, 70.0, 8.0, 2.0);
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(ollaHervorRepository.save(ollaHervorEntity)).thenReturn(ollaHervorEntity);

        org.assertj.core.api.Assertions.assertThatCode(() -> ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO)).doesNotThrowAnyException();

        verify(ollaHervorRepository).save(ollaHervorEntity);
        verify(etapaLoteRepository, never()).existsLotePendienteAsociado(any());
    }

    @Test
    @DisplayName("CP-MO-10: modificarOllaHervor permite modificar olla de hervor aunque se encuentre en uso (su escalado queda guardado en la instantánea de cada lote)")
    void modificarOllaHervor_debePermitirModificarEnUso() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 90.0, 70.0, 8.0, 2.0, EstadoOperativo.EN_USO);
        OllaHervorFormDTO ollaHervorFormDTO = ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(ollaHervorRepository.save(ollaHervorEntity)).thenReturn(ollaHervorEntity);

        org.assertj.core.api.Assertions.assertThatCode(() -> ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO)).doesNotThrowAnyException();

        verify(ollaHervorRepository).save(ollaHervorEntity);
        verify(etapaLoteRepository, never()).existsLotePendienteAsociado(any());
    }

    // ==================== bajaOllaHervor ====================

    @Test
    @DisplayName("bajaOllaHervor marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe")
    void bajaOllaHervor_debeMarcarBajaYRetornarOllaHervorExistente() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 100.0, 80.0, 10.0, 3.0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);
        when(ollaHervorRepository.save(ollaHervorEntity)).thenReturn(ollaHervorEntity);

        OllaHervorResponseDTO resultado = ollaHervorServicio.bajaOllaHervor(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(ollaHervorEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertOllaHervorDTO(ollaHervorEntity, resultado);
        verify(ollaHervorRepository).findById(1L);
        verify(etapaLoteRepository).existsLotePendienteAsociado(1L);
        verify(ollaHervorRepository).save(ollaHervorEntity);
        verify(ollaHervorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("bajaOllaHervor lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaOllaHervor_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(ollaHervorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ollaHervorServicio.bajaOllaHervor(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la olla de hervor con ID: 99");

        verify(ollaHervorRepository).findById(99L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BO-03: bajaOllaHervor lanza ReglaNegocioException y no persiste cuando la olla de hervor tiene comprometida una etapa en estado Pendiente de un lote")
    void bajaOllaHervor_debeRechazarAsociacionALotePendiente() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 100.0, 80.0, 10.0, 3.0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(true);

        assertThatThrownBy(() -> ollaHervorServicio.bajaOllaHervor(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la olla de hervor porque tiene comprometida una etapa en estado Pendiente de un lote");

        verify(etapaLoteRepository).existsLotePendienteAsociado(1L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BO-04: bajaOllaHervor lanza ReglaNegocioException y no persiste cuando la olla de hervor se encuentra en uso")
    void bajaOllaHervor_debeRechazarSiEstaEnUso() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 100.0, 80.0, 10.0, 3.0, EstadoOperativo.EN_USO);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> ollaHervorServicio.bajaOllaHervor(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la olla de hervor porque no se encuentra disponible");

        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BO-05: bajaOllaHervor lanza ReglaNegocioException y no persiste cuando la olla de hervor se encuentra en limpieza")
    void bajaOllaHervor_debeRechazarSiEstaEnLimpieza() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 100.0, 80.0, 10.0, 3.0, EstadoOperativo.EN_LIMPIEZA);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> ollaHervorServicio.bajaOllaHervor(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la olla de hervor porque no se encuentra disponible");

        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BO-06: bajaOllaHervor lanza ReglaNegocioException y no persiste cuando la olla de hervor se encuentra en mantenimiento")
    void bajaOllaHervor_debeRechazarSiEstaEnMantenimiento() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 100.0, 80.0, 10.0, 3.0, EstadoOperativo.EN_MANTENIMIENTO);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(false);

        assertThatThrownBy(() -> ollaHervorServicio.bajaOllaHervor(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la olla de hervor porque no se encuentra disponible");

        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BO-07: bajaOllaHervor informa primero la etapa en estado Pendiente de un lote cuando el olla de hervor además no está disponible")
    void bajaOllaHervor_debePriorizarEtapaPendienteSobreNoDisponible() {
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLLA-01", 100.0, 80.0, 10.0, 3.0, EstadoOperativo.EN_USO);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(etapaLoteRepository.existsLotePendienteAsociado(1L)).thenReturn(true);

        assertThatThrownBy(() -> ollaHervorServicio.bajaOllaHervor(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la olla de hervor porque tiene comprometida una etapa en estado Pendiente de un lote");

        verify(ollaHervorRepository, never()).save(any());
    }

    // ==================== validaciones técnicas de OllaHervor (números finitos, techos y longitudes) ====================

    @Test
    @DisplayName("CP-AO-19: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es NaN")
    void altaOllaHervor_debeRechazar_capacidadTotalNaN() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(Double.NaN, 80.0, 5.0, 2.0, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-20: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es infinita positiva")
    void altaOllaHervor_debeRechazar_capacidadTotalInfinita() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(Double.POSITIVE_INFINITY, 80.0, 5.0, 2.0, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-21: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es igual a cero (valor límite)")
    void altaOllaHervor_debeRechazar_capacidadTotalCero() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(0.0, -1.0, 5.0, 2.0, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-22: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es negativa")
    void altaOllaHervor_debeRechazar_capacidadTotalNegativa() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(-1.0, -2.0, 5.0, 2.0, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-23: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total supera 100.000 litros")
    void altaOllaHervor_debeRechazar_capacidadTotalSuperaElMaximo() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100000.01, 80.0, 5.0, 2.0, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-24: altaOllaHervor persiste con éxito cuando la capacidad total es exactamente 100.000 litros (valor límite válido)")
    void altaOllaHervor_debeAceptar_capacidadTotalEnElMaximo() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100000.0, 99999.0, 5.0, 2.0, 500);
        when(ollaHervorRepository.save(any(OllaHervorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> ollaHervorServicio.altaOllaHervor(dto)).doesNotThrowAnyException();

        verify(ollaHervorRepository).save(any(OllaHervorEntity.class));
    }

    @Test
    @DisplayName("CP-AO-38: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es nula")
    void altaOllaHervor_debeRechazar_capacidadUtilNula() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, null, 5.0, 2.0, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil es obligatoria");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-25: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es NaN")
    void altaOllaHervor_debeRechazar_capacidadUtilNaN() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, Double.NaN, 5.0, 2.0, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-26: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es igual a cero (valor límite)")
    void altaOllaHervor_debeRechazar_capacidadUtilCero() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, 0.0, 5.0, 2.0, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-27: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es negativa")
    void altaOllaHervor_debeRechazar_capacidadUtilNegativa() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, -1.0, 5.0, 2.0, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-29: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son negativos")
    void altaOllaHervor_debeRechazar_usosNegativos() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, 80.0, 5.0, 2.0, -1);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-30: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento superan 100.000")
    void altaOllaHervor_debeRechazar_usosSuperanElMaximo() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, 80.0, 5.0, 2.0, 100001);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-31: altaOllaHervor persiste con éxito cuando los usos máximos antes de mantenimiento son exactamente 100.000 (valor límite válido)")
    void altaOllaHervor_debeAceptar_usosEnElMaximo() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, 80.0, 5.0, 2.0, 100000);
        when(ollaHervorRepository.save(any(OllaHervorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> ollaHervorServicio.altaOllaHervor(dto)).doesNotThrowAnyException();

        verify(ollaHervorRepository).save(any(OllaHervorEntity.class));
    }

    @Test
    @DisplayName("CP-AO-34: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la evaporación es NaN")
    void altaOllaHervor_debeRechazar_evaporacionNaN() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, 80.0, Double.NaN, 2.0, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La evaporación debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-35: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la evaporación es infinita positiva")
    void altaOllaHervor_debeRechazar_evaporacionInfinita() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, 80.0, Double.POSITIVE_INFINITY, 2.0, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La evaporación no puede superar la capacidad útil de la olla");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-36: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la pérdida por trub es NaN")
    void altaOllaHervor_debeRechazar_perdidaPorTrubNaN() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, 80.0, 5.0, Double.NaN, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La pérdida por trub debe ser mayor a 0");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-AO-37: altaOllaHervor lanza ReglaNegocioException y no consulta el repositorio cuando la pérdida por trub es infinita positiva")
    void altaOllaHervor_debeRechazar_perdidaPorTrubInfinita() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, 80.0, 5.0, Double.POSITIVE_INFINITY, 500);

        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La pérdida por trub no puede superar la capacidad útil de la olla");

        verifyNoInteractions(ollaHervorRepository);
    }

    @Test
    @DisplayName("CP-MO-11: modificarOllaHervor lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad total es NaN")
    void modificarOllaHervor_debeRechazar_capacidadTotalNaN() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(Double.NaN, 80.0, 5.0, 2.0, 500);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(crearOllaHervorEntity(1L, "OLL-01", 100.0, 80.0, 5.0, 2.0)));

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verify(ollaHervorRepository).findById(1L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MO-12: modificarOllaHervor lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad total supera 100.000 litros")
    void modificarOllaHervor_debeRechazar_capacidadTotalSuperaElMaximo() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100000.01, 80.0, 5.0, 2.0, 500);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(crearOllaHervorEntity(1L, "OLL-01", 100.0, 80.0, 5.0, 2.0)));

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");

        verify(ollaHervorRepository).findById(1L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MO-13: modificarOllaHervor lanza ReglaNegocioException y no consulta la unicidad cuando la evaporación es NaN")
    void modificarOllaHervor_debeRechazar_evaporacionNaN() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, 80.0, Double.NaN, 2.0, 500);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(crearOllaHervorEntity(1L, "OLL-01", 100.0, 80.0, 5.0, 2.0)));

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La evaporación debe ser mayor a 0");

        verify(ollaHervorRepository).findById(1L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MO-14: modificarOllaHervor lanza ReglaNegocioException y no consulta la unicidad cuando la pérdida por trub es NaN")
    void modificarOllaHervor_debeRechazar_perdidaPorTrubNaN() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, 80.0, 5.0, Double.NaN, 500);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(crearOllaHervorEntity(1L, "OLL-01", 100.0, 80.0, 5.0, 2.0)));

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La pérdida por trub debe ser mayor a 0");

        verify(ollaHervorRepository).findById(1L);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MO-15: modificarOllaHervor lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento superan 100.000")
    void modificarOllaHervor_debeRechazar_usosSuperanElMaximo() {
        OllaHervorFormDTO dto = ollaHervorFormDTO(100.0, 80.0, 5.0, 2.0, 100001);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(crearOllaHervorEntity(1L, "OLL-01", 100.0, 80.0, 5.0, 2.0)));

        assertThatThrownBy(() -> ollaHervorServicio.modificarOllaHervor(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verify(ollaHervorRepository).findById(1L);
        verify(ollaHervorRepository, never()).save(any());
    }

    // ==================== helpers ====================

    private static OllaHervorEntity crearOllaHervorEntity(Long id, String identificadorInterno, Double capacidadTotal,
                                                            Double capacidadUtil, Double evaporacion, Double perdidaPorTrub) {
        return crearOllaHervorEntity(id, identificadorInterno, capacidadTotal, capacidadUtil, evaporacion, perdidaPorTrub, EstadoOperativo.DISPONIBLE);
    }

    private static OllaHervorEntity crearOllaHervorEntity(Long id, String identificadorInterno, Double capacidadTotal,
                                                            Double capacidadUtil, Double evaporacion, Double perdidaPorTrub,
                                                            EstadoOperativo estadoOperativo) {
        return OllaHervorEntity.builder()
                .id(id)
                .identificadorInterno(identificadorInterno)
                .estadoOperativo(estadoOperativo)
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .evaporacion(evaporacion)
                .perdidaPorTrub(perdidaPorTrub)
                .usosMaximosAntesMantenimiento(USOS_MAXIMOS_ANTES_MANTENIMIENTO)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static OllaHervorFormDTO ollaHervorFormDTO(Double capacidadTotal,
                                                       Double capacidadUtil, Double evaporacion, Double perdidaPorTrub) {
        return ollaHervorFormDTO(capacidadTotal, capacidadUtil, evaporacion, perdidaPorTrub, USOS_MAXIMOS_ANTES_MANTENIMIENTO);
    }

    private static OllaHervorFormDTO ollaHervorFormDTO(Double capacidadTotal, Double capacidadUtil,
                                                         Double evaporacion, Double perdidaPorTrub, Integer usosMaximosAntesMantenimiento) {
        return OllaHervorFormDTO.builder()
                .capacidadTotal(capacidadTotal)
                .capacidadUtil(capacidadUtil)
                .evaporacion(evaporacion)
                .perdidaPorTrub(perdidaPorTrub)
                .usosMaximosAntesMantenimiento(usosMaximosAntesMantenimiento)
                .build();
    }

    private static void assertOllaHervorDTO(OllaHervorEntity entidad, OllaHervorResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificadorInterno()).isEqualTo(entidad.getIdentificadorInterno());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getCapacidadTotal()).isEqualTo(entidad.getCapacidadTotal());
        assertThat(dto.getCapacidadUtil()).isEqualTo(entidad.getCapacidadUtil());
        assertThat(dto.getEvaporacion()).isEqualTo(entidad.getEvaporacion());
        assertThat(dto.getPerdidaPorTrub()).isEqualTo(entidad.getPerdidaPorTrub());
        assertThat(dto.getUsosMaximosAntesMantenimiento()).isEqualTo(entidad.getUsosMaximosAntesMantenimiento());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }

    // ==================== normalización y unicidad ====================

    // ==================== identificador interno generado ====================

    @Test
    @DisplayName("CP-AO-45: altaOllaHervor asigna el identificador interno generado a partir de la secuencia (OLL-07)")
    void altaOllaHervor_debeAsignarElIdentificadorInternoGenerado() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroOllaHervor()).thenReturn(7L);
        when(ollaHervorRepository.save(any(OllaHervorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        var resultado = ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0));

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("OLL-07");
    }

    @Test
    @DisplayName("CP-AO-46: altaOllaHervor no pide un número de la secuencia cuando falla cualquier validación")
    void altaOllaHervor_noDebePedirNumeroDeLaSecuenciaSiFallaUnaValidacion() {
        // === EJECUCION Y ASERTOS ===
        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO(null, 80.0, 10.0, 3.0))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO(100.0, 120.0, 10.0, 3.0))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO(100.0, 80.0, null, 3.0))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0, 0))).isInstanceOf(ReglaNegocioException.class);

        verifyNoInteractions(secuenciaIdentificadorRepository);
        verify(ollaHervorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AO-47: altaOllaHervor pide el número de la secuencia como último paso, justo antes de guardar")
    void altaOllaHervor_debePedirElNumeroDeLaSecuenciaAntesDeGuardar() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroOllaHervor()).thenReturn(1L);
        when(ollaHervorRepository.save(any(OllaHervorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        ollaHervorServicio.altaOllaHervor(ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0));

        // === ASSERTS ===
        InOrder orden = inOrder(secuenciaIdentificadorRepository, ollaHervorRepository);
        orden.verify(secuenciaIdentificadorRepository).siguienteNumeroOllaHervor();
        orden.verify(ollaHervorRepository).save(any(OllaHervorEntity.class));
    }

    @Test
    @DisplayName("CP-MO-23: modificarOllaHervor no cambia el identificador interno asignado por el sistema ni pide otro número")
    void modificarOllaHervor_noDebeCambiarElIdentificadorInterno() {
        // === PREPARACION DE DATOS ===
        OllaHervorEntity ollaHervorEntity = crearOllaHervorEntity(1L, "OLL-03", 90.0, 70.0, 8.0, 2.0);
        when(ollaHervorRepository.findById(1L)).thenReturn(Optional.of(ollaHervorEntity));
        when(ollaHervorRepository.save(ollaHervorEntity)).thenReturn(ollaHervorEntity);

        // === EJECUCION ===
        var resultado = ollaHervorServicio.modificarOllaHervor(1L, ollaHervorFormDTO(100.0, 80.0, 10.0, 3.0));

        // === ASSERTS ===
        assertThat(ollaHervorEntity.getIdentificadorInterno()).isEqualTo("OLL-03");
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("OLL-03");
        verifyNoInteractions(secuenciaIdentificadorRepository);
    }
}
