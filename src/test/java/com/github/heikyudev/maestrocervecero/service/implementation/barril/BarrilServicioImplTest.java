package com.github.heikyudev.maestrocervecero.service.implementation.barril;

import com.github.heikyudev.maestrocervecero.persistence.entity.barril.BarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.EstadoOperativoBarril;
import com.github.heikyudev.maestrocervecero.persistence.entity.barril.FabricanteBarrilEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IFabricanteBarrilRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.barril.BarrilFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.barril.BarrilResponseDTO;
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
class BarrilServicioImplTest {

    private static final Integer USOS_MAXIMOS_ANTES_MANTENIMIENTO = 200;

    @Mock
    private IBarrilRepository barrilRepository;

    @Mock
    private IFabricanteBarrilRepository fabricanteBarrilRepository;

    @InjectMocks
    private BarrilServicioImpl barrilServicio;

    // ==================== filtrarBarriles ====================

    @Test
    @DisplayName("CP-FB-01: filtrarBarriles retorna una página de barriles correctamente mapeada a DTO cuando se filtra por identificador, capacidad y estado operativo")
    void filtrarBarriles_debeRetornarPaginaMapeadaFiltrandoPorIdentificadorCapacidadYEstado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        BarrilEntity otroBarrilEntity = crearBarrilEntity(2L, "BAR-02", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        when(barrilRepository.filtrarBarriles("BAR", 50.0, EstadoOperativoBarril.DISPONIBLE, pageable))
                .thenReturn(new PageImpl<>(List.of(barrilEntity, otroBarrilEntity), pageable, 2));

        // === EJECUCION ===
        Page<BarrilResponseDTO> resultado = barrilServicio.filtrarBarriles("BAR", 50.0, EstadoOperativoBarril.DISPONIBLE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertBarrilDTO(barrilEntity, resultado.getContent().get(0));
        assertBarrilDTO(otroBarrilEntity, resultado.getContent().get(1));
        verify(barrilRepository).filtrarBarriles("BAR", 50.0, EstadoOperativoBarril.DISPONIBLE, pageable);
    }

    @Test
    @DisplayName("CP-FB-02: filtrarBarriles propaga identificador, capacidad y estado operativo nulos sin restringir esos criterios")
    void filtrarBarriles_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        BarrilEntity otroBarrilEntity = crearBarrilEntity(2L, "BAR-02", 100.0, EstadoOperativoBarril.EN_LIMPIEZA, fabricanteEntity);
        when(barrilRepository.filtrarBarriles(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(barrilEntity, otroBarrilEntity), pageable, 2));

        // === EJECUCION ===
        Page<BarrilResponseDTO> resultado = barrilServicio.filtrarBarriles(null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(barrilRepository).filtrarBarriles(null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FB-03: filtrarBarriles retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarBarriles_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(barrilRepository.filtrarBarriles("Inexistente", null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<BarrilResponseDTO> resultado = barrilServicio.filtrarBarriles("Inexistente", null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(barrilRepository).filtrarBarriles("Inexistente", null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del barril, incluyendo el fabricante, cuando el ID existe")
    void buscarPorId_debeRetornarBarrilExistente() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barrilEntity));

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertBarrilDTO(barrilEntity, resultado);
        verify(barrilRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para barriles dados de baja
        when(barrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barrilServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");
        verify(barrilRepository).findById(99L);
    }

    // ==================== altaBarril ====================

    @Test
    @DisplayName("CP-AB-01: altaBarril lanza ReglaNegocioException y no consulta la BD cuando la capacidad es nula")
    void altaBarril_debeRechazarCapacidadNula() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", null, 1L);

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad es obligatoria.");

        verifyNoInteractions(fabricanteBarrilRepository);
        verifyNoInteractions(barrilRepository);
    }

    @Test
    @DisplayName("CP-AB-02: altaBarril lanza ReglaNegocioException y no consulta la BD cuando la capacidad es negativa")
    void altaBarril_debeRechazarCapacidadNegativa() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", -1.0, 1L);

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad debe ser mayor a 0.");

        verifyNoInteractions(fabricanteBarrilRepository);
        verifyNoInteractions(barrilRepository);
    }

    @Test
    @DisplayName("CP-AB-03: altaBarril lanza ReglaNegocioException cuando la capacidad es igual a cero (valor límite)")
    void altaBarril_debeRechazarCapacidadIgualACero() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 0.0, 1L);

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad debe ser mayor a 0.");

        verifyNoInteractions(fabricanteBarrilRepository);
        verifyNoInteractions(barrilRepository);
    }

    @Test
    @DisplayName("CP-AB-04: altaBarril lanza RecursoNoEncontradoException y no persiste cuando el fabricante de barril no existe")
    void altaBarril_debeRechazarFabricanteInexistente() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 99L);
        when(fabricanteBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fabricante de barril con ID: 99");

        verify(barrilRepository, never()).existsByIdentificadorIgnoreCaseAndFabricanteId(any(), any());
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AB-05: altaBarril lanza RecursoDuplicadoException y no persiste cuando el identificador ya está registrado para el mismo fabricante activo")
    void altaBarril_debeRechazarIdentificadorDuplicadoParaElMismoFabricante() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 1L);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByIdentificadorIgnoreCaseAndFabricanteId("BAR-01", 1L)).thenReturn(true);

        assertThatThrownBy(() -> barrilServicio.altaBarril(barrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un barril activo con el identificador 'BAR-01' para el fabricante seleccionado");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AB-06: altaBarril permite el mismo identificador cuando pertenece a un fabricante distinto (camino feliz)")
    void altaBarril_debePermitirIdentificadorRepetidoParaOtroFabricante() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 2L);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(2L);
        when(fabricanteBarrilRepository.findById(2L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByIdentificadorIgnoreCaseAndFabricanteId("BAR-01", 2L)).thenReturn(false);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> {
            BarrilEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(5L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.altaBarril(barrilFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<BarrilEntity> captor = ArgumentCaptor.forClass(BarrilEntity.class);
        verify(barrilRepository).save(captor.capture());
        assertThat(captor.getValue().getFabricante()).isEqualTo(fabricanteEntity);
        assertThat(resultado.getIdentificador()).isEqualTo("BAR-01");
        assertThat(resultado.getFabricante().getId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("CP-AB-07: altaBarril persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaBarril_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 1L);
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByIdentificadorIgnoreCaseAndFabricanteId("BAR-01", 1L)).thenReturn(false);
        when(barrilRepository.save(any(BarrilEntity.class))).thenAnswer(invocation -> {
            BarrilEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.altaBarril(barrilFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<BarrilEntity> captor = ArgumentCaptor.forClass(BarrilEntity.class);
        verify(barrilRepository).save(captor.capture());
        BarrilEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getIdentificador()).isEqualTo("BAR-01");
        assertThat(entidadCapturada.getCapacidad()).isEqualTo(50.0);
        assertThat(entidadCapturada.getContenidoActual()).isEqualTo(0.0);
        assertThat(entidadCapturada.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(entidadCapturada.getUsosMaximosAntesMantenimiento()).isEqualTo(USOS_MAXIMOS_ANTES_MANTENIMIENTO);
        assertThat(entidadCapturada.getFabricante()).isEqualTo(fabricanteEntity);
        // El alta siempre debe registrar al barril como ACTIVO, DISPONIBLE y sin contenido, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getIdentificador()).isEqualTo("BAR-01");
        assertThat(resultado.getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(resultado.getContenidoActual()).isEqualTo(0.0);
        assertThat(resultado.getEstado()).isEqualTo(Estado.ACTIVO);
    }

    // ==================== modificarBarril ====================

    @Test
    @DisplayName("CP-MB-01: modificarBarril lanza RecursoNoEncontradoException y no valida nada más cuando el ID no existe")
    void modificarBarril_debeLanzarExcepcionSiNoExiste() {
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 1L);
        when(barrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barrilServicio.modificarBarril(99L, barrilFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");

        verify(barrilRepository).findById(99L);
        verifyNoInteractions(fabricanteBarrilRepository);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-02: modificarBarril lanza ReglaNegocioException y no consulta el fabricante cuando la capacidad es nula")
    void modificarBarril_debeRechazarCapacidadNula() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", null, 1L);
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad es obligatoria.");

        verifyNoInteractions(fabricanteBarrilRepository);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-03: modificarBarril lanza ReglaNegocioException cuando la capacidad es igual a cero (valor límite)")
    void modificarBarril_debeRechazarCapacidadIgualACero() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 0.0, 1L);
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La capacidad debe ser mayor a 0.");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-04: modificarBarril lanza RecursoNoEncontradoException y no persiste cuando el fabricante indicado no existe")
    void modificarBarril_debeRechazarFabricanteInexistente() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 99L);
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barrilEntity));
        when(fabricanteBarrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el fabricante de barril con ID: 99");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-05: modificarBarril lanza RecursoDuplicadoException y no persiste cuando el identificador está en uso por otro barril activo del mismo fabricante")
    void modificarBarril_debeRechazarIdentificadorEnUsoPorOtroBarrilDelMismoFabricante() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-02", 50.0, 1L);
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barrilEntity));
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByIdentificadorIgnoreCaseAndFabricanteIdAndIdNot("BAR-02", 1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> barrilServicio.modificarBarril(1L, barrilFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe otro barril activo con el identificador 'BAR-02' para el fabricante seleccionado");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MB-06: modificarBarril permite conservar el identificador y el fabricante propios y persiste (camino feliz)")
    void modificarBarril_debePermitirConservarIdentificadorYFabricantePropios() {
        FabricanteBarrilEntity fabricanteEntity = crearFabricanteBarrilEntity(1L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteEntity);
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 60.0, 1L);
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barrilEntity));
        when(fabricanteBarrilRepository.findById(1L)).thenReturn(Optional.of(fabricanteEntity));
        when(barrilRepository.existsByIdentificadorIgnoreCaseAndFabricanteIdAndIdNot("BAR-01", 1L, 1L)).thenReturn(false);
        when(barrilRepository.save(barrilEntity)).thenReturn(barrilEntity);

        BarrilResponseDTO resultado = barrilServicio.modificarBarril(1L, barrilFormDTO);

        assertThat(resultado.getIdentificador()).isEqualTo("BAR-01");
        assertThat(resultado.getCapacidad()).isEqualTo(60.0);
        assertThat(resultado.getFabricante().getId()).isEqualTo(1L);
        verify(barrilRepository).save(barrilEntity);
    }

    @Test
    @DisplayName("CP-MB-07: modificarBarril reasigna el fabricante y persiste cuando la reasignación es válida (camino feliz)")
    void modificarBarril_debeReasignarFabricanteExitosamente() {
        // === PREPARACION DE DATOS ===
        FabricanteBarrilEntity fabricanteOriginal = crearFabricanteBarrilEntity(1L);
        FabricanteBarrilEntity fabricanteNuevo = crearFabricanteBarrilEntity(2L);
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, fabricanteOriginal);
        BarrilFormDTO barrilFormDTO = barrilFormDTO("BAR-01", 50.0, 2L);
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barrilEntity));
        when(fabricanteBarrilRepository.findById(2L)).thenReturn(Optional.of(fabricanteNuevo));
        when(barrilRepository.existsByIdentificadorIgnoreCaseAndFabricanteIdAndIdNot("BAR-01", 2L, 1L)).thenReturn(false);
        when(barrilRepository.save(barrilEntity)).thenReturn(barrilEntity);

        // === EJECUCION ===
        BarrilResponseDTO resultado = barrilServicio.modificarBarril(1L, barrilFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<BarrilEntity> captor = ArgumentCaptor.forClass(BarrilEntity.class);
        verify(barrilRepository).save(captor.capture());
        assertThat(captor.getValue().getFabricante()).isEqualTo(fabricanteNuevo);
        // La reasignación de fabricante no debe tocar el estado operativo ni el contenido actual
        assertThat(captor.getValue().getEstadoOperativo()).isEqualTo(EstadoOperativoBarril.DISPONIBLE);
        assertThat(resultado.getFabricante().getId()).isEqualTo(2L);
    }

    // ==================== bajaBarril ====================

    @Test
    @DisplayName("CP-BB-01: bajaBarril lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaBarril_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(barrilRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> barrilServicio.bajaBarril(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el barril con ID: 99");

        verify(barrilRepository).findById(99L);
        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BB-02: bajaBarril lanza ReglaNegocioException y no persiste cuando el barril está en estado operativo CON_CERVEZA")
    void bajaBarril_debeRechazarEstadoOperativoConCerveza() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.CON_CERVEZA, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.bajaBarril(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el barril porque se encuentra en estado operativo CON_CERVEZA o DESPACHADO.");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BB-03: bajaBarril lanza ReglaNegocioException y no persiste cuando el barril está en estado operativo DESPACHADO")
    void bajaBarril_debeRechazarEstadoOperativoDespachado() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DESPACHADO, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barrilEntity));

        assertThatThrownBy(() -> barrilServicio.bajaBarril(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el barril porque se encuentra en estado operativo CON_CERVEZA o DESPACHADO.");

        verify(barrilRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BB-04: bajaBarril marca el estado como BAJA, persiste y retorna el DTO cuando el estado operativo lo permite")
    void bajaBarril_debeMarcarBajaYRetornarBarrilExistente() {
        BarrilEntity barrilEntity = crearBarrilEntity(1L, "BAR-01", 50.0, EstadoOperativoBarril.DISPONIBLE, crearFabricanteBarrilEntity(1L));
        when(barrilRepository.findById(1L)).thenReturn(Optional.of(barrilEntity));
        when(barrilRepository.save(barrilEntity)).thenReturn(barrilEntity);

        BarrilResponseDTO resultado = barrilServicio.bajaBarril(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(barrilEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertBarrilDTO(barrilEntity, resultado);
        verify(barrilRepository).findById(1L);
        verify(barrilRepository).save(barrilEntity);
        verify(barrilRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static FabricanteBarrilEntity crearFabricanteBarrilEntity(Long id) {
        return FabricanteBarrilEntity.builder()
                .id(id)
                .razonSocial("Tonelería de prueba")
                .nombreComercial("Tonelería")
                .cuit("30-11111111-1")
                .telefono("011-4444-5555")
                .email("contacto@tonelera.com")
                .estado(Estado.ACTIVO)
                .build();
    }

    private static BarrilEntity crearBarrilEntity(Long id, String identificador, Double capacidad, EstadoOperativoBarril estadoOperativo, FabricanteBarrilEntity fabricante) {
        return BarrilEntity.builder()
                .id(id)
                .identificador(identificador)
                .capacidad(capacidad)
                .contenidoActual(0.0)
                .estadoOperativo(estadoOperativo)
                .usosMaximosAntesMantenimiento(USOS_MAXIMOS_ANTES_MANTENIMIENTO)
                .estado(Estado.ACTIVO)
                .fabricante(fabricante)
                .build();
    }

    private static BarrilFormDTO barrilFormDTO(String identificador, Double capacidad, Long idFabricanteBarril) {
        return BarrilFormDTO.builder()
                .identificador(identificador)
                .capacidad(capacidad)
                .usosMaximosAntesMantenimiento(USOS_MAXIMOS_ANTES_MANTENIMIENTO)
                .idFabricanteBarril(idFabricanteBarril)
                .build();
    }

    private static void assertBarrilDTO(BarrilEntity entidad, BarrilResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getIdentificador()).isEqualTo(entidad.getIdentificador());
        assertThat(dto.getCapacidad()).isEqualTo(entidad.getCapacidad());
        assertThat(dto.getContenidoActual()).isEqualTo(entidad.getContenidoActual());
        assertThat(dto.getEstadoOperativo()).isEqualTo(entidad.getEstadoOperativo());
        assertThat(dto.getUsosMaximosAntesMantenimiento()).isEqualTo(entidad.getUsosMaximosAntesMantenimiento());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
        assertThat(dto.getFabricante().getId()).isEqualTo(entidad.getFabricante().getId());
    }
}
