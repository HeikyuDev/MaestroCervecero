package com.github.heikyudev.maestrocervecero.service.implementation.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EstadoOperativo;
import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.ISecuenciaIdentificadorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento.IMaceradorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.lote.IEtapaLoteRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.equipamiento.MaceradorFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.equipamiento.MaceradorResponseDTO;
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
class MaceradorServicioImplTest {

    private static final int USOS_MAXIMOS_ANTES_MANTENIMIENTO = 500;

    @Mock
    private IMaceradorRepository maceradorRepository;

    @Mock
    private ISecuenciaIdentificadorRepository secuenciaIdentificadorRepository;

    @Mock
    private IEtapaLoteRepository etapaLoteRepository;

    @InjectMocks
    private MaceradorServicioImpl maceradorServicio;

    @BeforeEach
    void secuenciaDelIdentificador() {
        // Valor por defecto de la secuencia para los casos de alta que no verifican el número generado
        lenient().when(secuenciaIdentificadorRepository.siguienteNumeroMacerador()).thenReturn(1L);
    }

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
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 120.0, 5.0, 75.0);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException cuando la capacidad útil es igual a la total (valor límite)")
    void altaMacerador_debeRechazarCapacidadUtilIgualATotal() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 100.0, 5.0, 75.0);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser menor a la capacidad total");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es nula")
    void altaMacerador_debeRechazarCapacidadTotalNula() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(null, 80.0, 5.0, 75.0);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total es obligatoria");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la eficiencia de maceración es nula")
    void altaMacerador_debeRechazarEficienciaNula() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, null);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración es obligatoria");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException cuando la eficiencia está por debajo del mínimo (límite inferior)")
    void altaMacerador_debeRechazarEficienciaPorDebajoDelMinimo() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 39.9);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador lanza ReglaNegocioException cuando la eficiencia está por encima del máximo (límite superior)")
    void altaMacerador_debeRechazarEficienciaPorEncimaDelMaximo() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 100.1);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("altaMacerador persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaMacerador_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroMacerador()).thenReturn(5L);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 75.0);
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
        assertThat(entidadCapturada.getIdentificadorInterno()).isEqualTo("MAC-05");
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(entidadCapturada.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(entidadCapturada.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(entidadCapturada.getEspacioMuerto()).isEqualTo(5.0);
        assertThat(entidadCapturada.getEficienciaMaceracion()).isEqualTo(75.0);
        assertThat(entidadCapturada.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        // El alta siempre debe registrar al macerador como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MAC-05");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(resultado.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
    }

    @Test
    @DisplayName("altaMacerador acepta 40.0 como borde válido inferior de la eficiencia de maceración")
    void altaMacerador_debeAceptarEficienciaEnLimiteInferior() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 40.0);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaceradorResponseDTO resultado = maceradorServicio.altaMacerador(maceradorFormDTO);

        assertThat(resultado.getEficienciaMaceracion()).isEqualTo(40.0);
        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("altaMacerador acepta 100.0 como borde válido superior de la eficiencia de maceración")
    void altaMacerador_debeAceptarEficienciaEnLimiteSuperior() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 100.0);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaceradorResponseDTO resultado = maceradorServicio.altaMacerador(maceradorFormDTO);

        assertThat(resultado.getEficienciaMaceracion()).isEqualTo(100.0);
        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-12: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son nulos")
    void altaMacerador_debeRechazarUsosMaximosAntesMantenimientoNulo() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 75.0, null);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento son obligatorios");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-13: altaMacerador lanza ReglaNegocioException cuando los usos máximos antes de mantenimiento son iguales a cero (valor límite)")
    void altaMacerador_debeRechazarUsosMaximosAntesMantenimientoIgualACero() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 75.0, 0);

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
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 120.0, 5.0, 75.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // La existencia del macerador se valida antes que las reglas de negocio sobre sus datos
        verify(maceradorRepository).findById(1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando la eficiencia es inválida")
    void modificarMacerador_debeRechazarEficienciaInvalida() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 30.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(maceradorRepository).findById(1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMacerador lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarMacerador_debeLanzarExcepcionSiNoExiste() {
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 75.0);
        when(maceradorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(99L, maceradorFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el macerador con ID: 99");

        verify(maceradorRepository).findById(99L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMacerador actualiza los datos y persiste cuando el ID existe, sin cambiar el identificador interno (camino feliz)")
    void modificarMacerador_debeActualizarMaceradorExistente() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-VIEJO", 90.0, 70.0, 4.0, 60.0);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 85.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(maceradorRepository.save(maceradorEntity)).thenReturn(maceradorEntity);

        // === EJECUCION ===
        MaceradorResponseDTO resultado = maceradorServicio.modificarMacerador(1L, maceradorFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MAC-VIEJO");
        assertThat(resultado.getCapacidadTotal()).isEqualTo(100.0);
        assertThat(resultado.getCapacidadUtil()).isEqualTo(80.0);
        assertThat(resultado.getEspacioMuerto()).isEqualTo(5.0);
        assertThat(resultado.getEficienciaMaceracion()).isEqualTo(85.0);
        assertThat(resultado.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        verify(maceradorRepository).findById(1L);
        verify(maceradorRepository).save(maceradorEntity);
        // El identificador interno lo asignó el sistema al dar de alta: modificar no lo cambia ni pide otro número
        verifyNoInteractions(secuenciaIdentificadorRepository);
    }

    @Test
    @DisplayName("CP-MM-06: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento son inválidos")
    void modificarMacerador_debeRechazarUsosMaximosAntesMantenimientoInvalido() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 75.0, 0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verify(maceradorRepository).findById(1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-07: modificarMacerador permite modificar macerador aunque tenga comprometida una etapa en estado Pendiente de un lote (su escalado queda guardado en la instantánea del lote)")
    void modificarMacerador_debePermitirModificarConLotePendiente() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 85.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(maceradorRepository.save(maceradorEntity)).thenReturn(maceradorEntity);

        org.assertj.core.api.Assertions.assertThatCode(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO)).doesNotThrowAnyException();

        verify(maceradorRepository).save(maceradorEntity);
        verify(etapaLoteRepository, never()).existsLotePendienteAsociado(any());
    }

    @Test
    @DisplayName("CP-MM-08: modificarMacerador permite modificar macerador aunque se encuentre en uso (su escalado queda guardado en la instantánea de cada lote)")
    void modificarMacerador_debePermitirModificarEnUso() {
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-01", 90.0, 70.0, 4.0, 60.0, EstadoOperativo.EN_USO);
        MaceradorFormDTO maceradorFormDTO = maceradorFormDTO(100.0, 80.0, 5.0, 85.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(maceradorRepository.save(maceradorEntity)).thenReturn(maceradorEntity);

        org.assertj.core.api.Assertions.assertThatCode(() -> maceradorServicio.modificarMacerador(1L, maceradorFormDTO)).doesNotThrowAnyException();

        verify(maceradorRepository).save(maceradorEntity);
        verify(etapaLoteRepository, never()).existsLotePendienteAsociado(any());
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
        MaceradorFormDTO dto = maceradorFormDTO(Double.NaN, 80.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-15: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es infinita positiva")
    void altaMacerador_debeRechazar_capacidadTotalInfinita() {
        MaceradorFormDTO dto = maceradorFormDTO(Double.POSITIVE_INFINITY, 80.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-16: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es igual a cero (valor límite)")
    void altaMacerador_debeRechazar_capacidadTotalCero() {
        MaceradorFormDTO dto = maceradorFormDTO(0.0, -1.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-17: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total es negativa")
    void altaMacerador_debeRechazar_capacidadTotalNegativa() {
        MaceradorFormDTO dto = maceradorFormDTO(-1.0, -2.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-18: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad total supera 100.000 litros")
    void altaMacerador_debeRechazar_capacidadTotalSuperaElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO(100000.01, 80.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-19: altaMacerador persiste con éxito cuando la capacidad total es exactamente 100.000 litros (valor límite válido)")
    void altaMacerador_debeAceptar_capacidadTotalEnElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO(100000.0, 99999.0, 10.0, 75.0, 500);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.altaMacerador(dto)).doesNotThrowAnyException();

        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-41: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es nula")
    void altaMacerador_debeRechazar_capacidadUtilNula() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, null, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil es obligatoria");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-20: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es NaN")
    void altaMacerador_debeRechazar_capacidadUtilNaN() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, Double.NaN, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser mayor a 0");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-21: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es igual a cero (valor límite)")
    void altaMacerador_debeRechazar_capacidadUtilCero() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 0.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser mayor a 0");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-22: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la capacidad útil es negativa")
    void altaMacerador_debeRechazar_capacidadUtilNegativa() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, -1.0, 10.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad útil debe ser mayor a 0");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-24: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento son negativos")
    void altaMacerador_debeRechazar_usosNegativos() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 10.0, 75.0, -1);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento deben ser mayores a 0");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-25: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando los usos máximos antes de mantenimiento superan 100.000")
    void altaMacerador_debeRechazar_usosSuperanElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 10.0, 75.0, 100001);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-26: altaMacerador persiste con éxito cuando los usos máximos antes de mantenimiento son exactamente 100.000 (valor límite válido)")
    void altaMacerador_debeAceptar_usosEnElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 10.0, 75.0, 100000);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.altaMacerador(dto)).doesNotThrowAnyException();

        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-29: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es nulo")
    void altaMacerador_debeRechazar_espacioMuertoNulo() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, null, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto es obligatorio");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-30: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es NaN")
    void altaMacerador_debeRechazar_espacioMuertoNaN() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, Double.NaN, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser mayor o igual a 0");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-31: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es infinito positivo")
    void altaMacerador_debeRechazar_espacioMuertoInfinito() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, Double.POSITIVE_INFINITY, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser menor a la capacidad útil");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-32: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es negativo")
    void altaMacerador_debeRechazar_espacioMuertoNegativo() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, -1.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser mayor o igual a 0");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-33: altaMacerador persiste con éxito cuando el espacio muerto es igual a cero (valor límite válido)")
    void altaMacerador_debeAceptar_espacioMuertoCero() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 0.0, 75.0, 500);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.altaMacerador(dto)).doesNotThrowAnyException();

        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-34: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es igual a la capacidad útil (valor límite)")
    void altaMacerador_debeRechazar_espacioMuertoIgualACapacidadUtil() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 80.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser menor a la capacidad útil");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-35: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto es mayor a la capacidad útil")
    void altaMacerador_debeRechazar_espacioMuertoMayorACapacidadUtil() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 90.0, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser menor a la capacidad útil");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-36: altaMacerador persiste con éxito cuando el espacio muerto es apenas menor a la capacidad útil (valor límite válido)")
    void altaMacerador_debeAceptar_espacioMuertoJustoDebajoDeCapacidadUtil() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 79.99, 75.0, 500);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maceradorServicio.altaMacerador(dto)).doesNotThrowAnyException();

        verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-37: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando el espacio muerto supera 100.000 litros, que es mayor a la capacidad útil")
    void altaMacerador_debeRechazar_espacioMuertoSuperaElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO(100000.0, 99999.0, 100000.01, 75.0, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser menor a la capacidad útil");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-38: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la eficiencia de maceración es NaN")
    void altaMacerador_debeRechazar_eficienciaNaN() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 10.0, Double.NaN, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-39: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la eficiencia de maceración es infinita positiva")
    void altaMacerador_debeRechazar_eficienciaInfinita() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 10.0, Double.POSITIVE_INFINITY, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-40: altaMacerador lanza ReglaNegocioException y no consulta el repositorio cuando la eficiencia de maceración es infinita negativa")
    void altaMacerador_debeRechazar_eficienciaInfinitaNegativa() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 10.0, Double.NEGATIVE_INFINITY, 500);

        assertThatThrownBy(() -> maceradorServicio.altaMacerador(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100");

        verifyNoInteractions(maceradorRepository);
    }

    @Test
    @DisplayName("CP-MM-09: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad total es NaN")
    void modificarMacerador_debeRechazar_capacidadTotalNaN() {
        MaceradorFormDTO dto = maceradorFormDTO(Double.NaN, 80.0, 10.0, 75.0, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total debe ser mayor a 0");

        verify(maceradorRepository).findById(1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-10: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando la capacidad total supera 100.000 litros")
    void modificarMacerador_debeRechazar_capacidadTotalSuperaElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO(100000.01, 80.0, 10.0, 75.0, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad total no puede superar 100.000 litros");

        verify(maceradorRepository).findById(1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-11: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando el espacio muerto es NaN")
    void modificarMacerador_debeRechazar_espacioMuertoNaN() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, Double.NaN, 75.0, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser mayor o igual a 0");

        verify(maceradorRepository).findById(1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-12: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando el espacio muerto es igual a la capacidad útil (valor límite)")
    void modificarMacerador_debeRechazar_espacioMuertoIgualACapacidadUtil() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 80.0, 75.0, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El espacio muerto debe ser menor a la capacidad útil");

        verify(maceradorRepository).findById(1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-13: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando la eficiencia de maceración es NaN")
    void modificarMacerador_debeRechazar_eficienciaNaN() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 10.0, Double.NaN, 500);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La eficiencia de maceración debe estar entre 40 y 100");

        verify(maceradorRepository).findById(1L);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-14: modificarMacerador lanza ReglaNegocioException y no consulta la unicidad cuando los usos máximos antes de mantenimiento superan 100.000")
    void modificarMacerador_debeRechazar_usosSuperanElMaximo() {
        MaceradorFormDTO dto = maceradorFormDTO(100.0, 80.0, 10.0, 75.0, 100001);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(crearMaceradorEntity(1L, "MAC-01", 100.0, 80.0, 10.0, 75.0)));

        assertThatThrownBy(() -> maceradorServicio.modificarMacerador(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Los usos máximos antes de mantenimiento no pueden superar 100.000 usos");

        verify(maceradorRepository).findById(1L);
        verify(maceradorRepository, never()).save(any());
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

    private static MaceradorFormDTO maceradorFormDTO(Double capacidadTotal,
                                                       Double capacidadUtil, Double espacioMuerto, Double eficienciaMaceracion) {
        return maceradorFormDTO(capacidadTotal, capacidadUtil, espacioMuerto, eficienciaMaceracion, USOS_MAXIMOS_ANTES_MANTENIMIENTO);
    }

    private static MaceradorFormDTO maceradorFormDTO(Double capacidadTotal, Double capacidadUtil,
                                                       Double espacioMuerto, Double eficienciaMaceracion, Integer usosMaximosAntesMantenimiento) {
        return MaceradorFormDTO.builder()
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

    // ==================== identificador interno generado ====================

    @Test
    @DisplayName("CP-48: altaMacerador asigna el identificador interno generado a partir de la secuencia (MAC-07)")
    void altaMacerador_debeAsignarElIdentificadorInternoGenerado() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroMacerador()).thenReturn(7L);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        var resultado = maceradorServicio.altaMacerador(maceradorFormDTO(100.0, 80.0, 5.0, 75.0));

        // === ASSERTS ===
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MAC-07");
    }

    @Test
    @DisplayName("CP-49: altaMacerador no pide un número de la secuencia cuando falla cualquier validación")
    void altaMacerador_noDebePedirNumeroDeLaSecuenciaSiFallaUnaValidacion() {
        // === EJECUCION Y ASERTOS ===
        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO(null, 80.0, 5.0, 75.0))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO(100.0, 120.0, 5.0, 75.0))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO(100.0, 80.0, 5.0, null))).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> maceradorServicio.altaMacerador(maceradorFormDTO(100.0, 80.0, 5.0, 75.0, 0))).isInstanceOf(ReglaNegocioException.class);

        verifyNoInteractions(secuenciaIdentificadorRepository);
        verify(maceradorRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-50: altaMacerador pide el número de la secuencia como último paso, justo antes de guardar")
    void altaMacerador_debePedirElNumeroDeLaSecuenciaAntesDeGuardar() {
        // === PREPARACION DE DATOS ===
        when(secuenciaIdentificadorRepository.siguienteNumeroMacerador()).thenReturn(1L);
        when(maceradorRepository.save(any(MaceradorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        maceradorServicio.altaMacerador(maceradorFormDTO(100.0, 80.0, 5.0, 75.0));

        // === ASSERTS ===
        InOrder orden = inOrder(secuenciaIdentificadorRepository, maceradorRepository);
        orden.verify(secuenciaIdentificadorRepository).siguienteNumeroMacerador();
        orden.verify(maceradorRepository).save(any(MaceradorEntity.class));
    }

    @Test
    @DisplayName("CP-MM-22: modificarMacerador no cambia el identificador interno asignado por el sistema ni pide otro número")
    void modificarMacerador_noDebeCambiarElIdentificadorInterno() {
        // === PREPARACION DE DATOS ===
        MaceradorEntity maceradorEntity = crearMaceradorEntity(1L, "MAC-03", 90.0, 70.0, 4.0, 60.0);
        when(maceradorRepository.findById(1L)).thenReturn(Optional.of(maceradorEntity));
        when(maceradorRepository.save(maceradorEntity)).thenReturn(maceradorEntity);

        // === EJECUCION ===
        var resultado = maceradorServicio.modificarMacerador(1L, maceradorFormDTO(100.0, 80.0, 5.0, 75.0));

        // === ASSERTS ===
        assertThat(maceradorEntity.getIdentificadorInterno()).isEqualTo("MAC-03");
        assertThat(resultado.getIdentificadorInterno()).isEqualTo("MAC-03");
        verifyNoInteractions(secuenciaIdentificadorRepository);
    }
}
