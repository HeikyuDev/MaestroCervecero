package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILevaduraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.ICatalogoProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IVersionRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.LevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
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
class LevaduraServicioImplTest {

    @Mock
    private ILevaduraRepository levaduraRepository;
    @Mock
    private IVersionRecetaRepository versionRecetaRepository;
    @Mock
    private ILoteInsumoRepository loteInsumoRepository;
    @Mock
    private IPlanificacionProduccionRepository planificacionProduccionRepository;
    @Mock
    private IOrdenCompraRepository ordenCompraRepository;
    @Mock
    private ICatalogoProveedorRepository catalogoProveedorRepository;

    @InjectMocks
    private LevaduraServicioImpl levaduraServicio;

    // ==================== filtrarLevaduras ====================

    @Test
    @DisplayName("CP-FLv-01: filtrarLevaduras retorna una página de levaduras correctamente mapeada a DTO cuando se filtra por nombre y tipo")
    void filtrarLevaduras_debeRetornarPaginaMapeadaFiltrandoPorNombreYTipo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        LevaduraEntity otraLevaduraEntity = crearLevaduraEntity(2L, "SafAle US-05", TipoLevadura.ALE, 1.2E10);
        when(levaduraRepository.filtrarLevaduras("SafAle", TipoLevadura.ALE, pageable))
                .thenReturn(new PageImpl<>(List.of(levaduraEntity, otraLevaduraEntity), pageable, 2));

        // === EJECUCION ===
        Page<LevaduraResponseDTO> resultado = levaduraServicio.filtrarLevaduras("SafAle", TipoLevadura.ALE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertLevaduraDTO(levaduraEntity, resultado.getContent().get(0));
        assertLevaduraDTO(otraLevaduraEntity, resultado.getContent().get(1));
        verify(levaduraRepository).filtrarLevaduras("SafAle", TipoLevadura.ALE, pageable);
    }

    @Test
    @DisplayName("CP-FLv-02: filtrarLevaduras propaga nombre y tipo nulos sin restringir esos criterios")
    void filtrarLevaduras_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        LevaduraEntity otraLevaduraEntity = crearLevaduraEntity(2L, "SafLager W-34/70", TipoLevadura.LAGER, 6.0E9);
        when(levaduraRepository.filtrarLevaduras(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(levaduraEntity, otraLevaduraEntity), pageable, 2));

        // === EJECUCION ===
        Page<LevaduraResponseDTO> resultado = levaduraServicio.filtrarLevaduras(null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(levaduraRepository).filtrarLevaduras(null, null, pageable);
    }

    @Test
    @DisplayName("CP-FLv-03: filtrarLevaduras retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarLevaduras_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(levaduraRepository.filtrarLevaduras("Inexistente", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<LevaduraResponseDTO> resultado = levaduraServicio.filtrarLevaduras("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(levaduraRepository).filtrarLevaduras("Inexistente", null, pageable);
    }

    @Test
    @DisplayName("CP-FLv-04: filtrarLevaduras normaliza el nombre buscado (espacios extremos e internos) antes de consultar")
    void filtrarLevaduras_debeNormalizarElNombreBuscado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(levaduraRepository.filtrarLevaduras("Safale US-05", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        levaduraServicio.filtrarLevaduras("   Safale   US-05  ", null, pageable);

        // === ASSERTS ===
        verify(levaduraRepository).filtrarLevaduras("Safale US-05", null, pageable);
    }

    @Test
    @DisplayName("CP-FLv-05: filtrarLevaduras trata un nombre formado solo por espacios como ausencia de filtro")
    void filtrarLevaduras_debeTratarNombreEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(levaduraRepository.filtrarLevaduras(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        levaduraServicio.filtrarLevaduras("    ", null, pageable);

        // === ASSERTS ===
        verify(levaduraRepository).filtrarLevaduras(null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("buscarPorId retorna el DTO de la levadura cuando el ID existe")
    void buscarPorId_debeRetornarLevaduraExistente() {
        // === PREPARACION DE DATOS ===
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));

        // === EJECUCION ===
        LevaduraResponseDTO resultado = levaduraServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertLevaduraDTO(levaduraEntity, resultado);
        verify(levaduraRepository).findById(1L);
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dada de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para levaduras dadas de baja
        when(levaduraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> levaduraServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la levadura con ID: 99");
        verify(levaduraRepository).findById(99L);
    }

    // ==================== altaLevadura ====================

    @Test
    @DisplayName("altaLevadura lanza ReglaNegocioException y no consulta el repositorio cuando la cantidad de células por gramo es nula")
    void altaLevadura_debeRechazarCantidadCelulasNula() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, null);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células viables por gramo es obligatoria");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("altaLevadura lanza ReglaNegocioException cuando la cantidad de células por gramo es igual a cero (valor límite)")
    void altaLevadura_debeRechazarCantidadCelulasCero() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, 0.0);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células viables por gramo debe estar entre 6.000 y 20.000 millones");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("altaLevadura lanza ReglaNegocioException cuando la cantidad de células por gramo es negativa")
    void altaLevadura_debeRechazarCantidadCelulasNegativa() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, -1.0);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células viables por gramo debe estar entre 6.000 y 20.000 millones");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("altaLevadura lanza ReglaNegocioException cuando la cantidad de células viables por gramo es menor a 6.000.000.000 (límite inferior)")
    void altaLevadura_debeRechazarCantidadCelulasMenorAlMinimo() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, 5_999_999_999.0);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células viables por gramo debe estar entre 6.000 y 20.000 millones");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("altaLevadura lanza ReglaNegocioException cuando la cantidad de células viables por gramo es mayor a 20.000.000.000 (límite superior)")
    void altaLevadura_debeRechazarCantidadCelulasMayorAlMaximo() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, 20_000_000_001.0);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células viables por gramo debe estar entre 6.000 y 20.000 millones");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("altaLevadura persiste con éxito cuando la cantidad de células viables por gramo está en el límite superior válido (20.000.000.000)")
    void altaLevadura_debePersistirConCantidadCelulasEnLimiteSuperiorValido() {
        // === PREPARACION DE DATOS ===
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("Kveik Voss", TipoLevadura.ALE, 20_000_000_000.0);
        when(levaduraRepository.existsByNombreIgnoreCase("Kveik Voss")).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> {
            LevaduraEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(5L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        LevaduraResponseDTO resultado = levaduraServicio.altaLevadura(levaduraFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<LevaduraEntity> captor = ArgumentCaptor.forClass(LevaduraEntity.class);
        verify(levaduraRepository).save(captor.capture());
        assertThat(captor.getValue().getCantidadCelulasPorGramo()).isEqualTo(20_000_000_000.0);
        assertThat(resultado.getCantidadCelulasPorGramo()).isEqualTo(20_000_000_000.0);
        assertThat(resultado.getNombre()).isEqualTo("Kveik Voss");
    }

    @Test
    @DisplayName("altaLevadura lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe")
    void altaLevadura_debeRechazarNombreDuplicado() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle US-05", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.existsByNombreIgnoreCase("SafAle US-05")).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una levadura con el nombre 'SafAle US-05'");

        verify(levaduraRepository).existsByNombreIgnoreCase("SafAle US-05");
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaLevadura lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case (case-insensitive)")
    void altaLevadura_debeRechazarNombreDuplicadoCaseInsensitive() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("safale us-05", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.existsByNombreIgnoreCase("safale us-05")).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una levadura con el nombre 'safale us-05'");

        verify(levaduraRepository).existsByNombreIgnoreCase("safale us-05");
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaLevadura persiste y retorna el DTO asignando fijamente GRAMO como unidad de medida (camino feliz)")
    void altaLevadura_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafLager W-34/70", TipoLevadura.LAGER, 1.0E10);
        when(levaduraRepository.existsByNombreIgnoreCase("SafLager W-34/70")).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> {
            LevaduraEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        LevaduraResponseDTO resultado = levaduraServicio.altaLevadura(levaduraFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<LevaduraEntity> captor = ArgumentCaptor.forClass(LevaduraEntity.class);
        verify(levaduraRepository).save(captor.capture());
        LevaduraEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("SafLager W-34/70");
        assertThat(entidadCapturada.getTipo()).isEqualTo(TipoLevadura.LAGER);
        assertThat(entidadCapturada.getCantidadCelulasPorGramo()).isEqualTo(1.0E10);
        // La unidad de medida es fija por regla de negocio y la asigna el service, no el FormDTO
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);
        // El alta siempre debe registrar a la levadura como ACTIVA, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("SafLager W-34/70");
        assertThat(resultado.getTipo()).isEqualTo(TipoLevadura.LAGER);
        assertThat(resultado.getCantidadCelulasPorGramo()).isEqualTo(1.0E10);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);
        verify(levaduraRepository).existsByNombreIgnoreCase("SafLager W-34/70");
    }

    @Test
    @DisplayName("altaLevadura persiste con éxito cuando la cantidad de células viables por gramo está en el límite inferior válido (6.000.000.000)")
    void altaLevadura_debePersistirConCantidadCelulasEnLimiteInferiorValido() {
        // === PREPARACION DE DATOS ===
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("Belle Saison", TipoLevadura.ALE, 6_000_000_000.0);
        when(levaduraRepository.existsByNombreIgnoreCase("Belle Saison")).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> {
            LevaduraEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(3L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        LevaduraResponseDTO resultado = levaduraServicio.altaLevadura(levaduraFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<LevaduraEntity> captor = ArgumentCaptor.forClass(LevaduraEntity.class);
        verify(levaduraRepository).save(captor.capture());
        LevaduraEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getCantidadCelulasPorGramo()).isEqualTo(6_000_000_000.0);
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);

        assertThat(resultado.getCantidadCelulasPorGramo()).isEqualTo(6_000_000_000.0);
        assertThat(resultado.getNombre()).isEqualTo("Belle Saison");
    }

    // ==================== modificarLevadura ====================

    @Test
    @DisplayName("modificarLevadura lanza ReglaNegocioException y no consulta el repositorio cuando la cantidad de células por gramo es inválida")
    void modificarLevadura_debeRechazarCantidadCelulasInvalida() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, 0.0);

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(1L, levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células viables por gramo debe estar entre 6.000 y 20.000 millones");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("modificarLevadura lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarLevadura_debeLanzarExcepcionSiNoExiste() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(99L, levaduraFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la levadura con ID: 99");

        verify(levaduraRepository).findById(99L);
        // Al no existir la entidad, no debe llegarse a validar la duplicidad de nombre
        verify(levaduraRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarLevadura lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otra levadura")
    void modificarLevadura_debeRechazarNombreEnUsoPorOtraLevadura() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle US-05", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("SafAle US-05", 1L)).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(1L, levaduraFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una levadura con el nombre 'SafAle US-05'");

        verify(levaduraRepository).findById(1L);
        verify(levaduraRepository).existsByNombreIgnoreCaseAndIdNot("SafAle US-05", 1L);
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarLevadura actualiza los datos, conserva GRAMO y persiste cuando el ID existe y el nombre está libre (camino feliz)")
    void modificarLevadura_debeActualizarLevaduraExistente() {
        // === PREPARACION DE DATOS ===
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04 Modificada", TipoLevadura.LAGER, 6.0E9);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("SafAle S-04 Modificada", 1L)).thenReturn(false);
        when(levaduraRepository.save(levaduraEntity)).thenReturn(levaduraEntity);

        // === EJECUCION ===
        LevaduraResponseDTO resultado = levaduraServicio.modificarLevadura(1L, levaduraFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<LevaduraEntity> captor = ArgumentCaptor.forClass(LevaduraEntity.class);
        verify(levaduraRepository).save(captor.capture());
        LevaduraEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("SafAle S-04 Modificada");
        assertThat(entidadCapturada.getTipo()).isEqualTo(TipoLevadura.LAGER);
        assertThat(entidadCapturada.getCantidadCelulasPorGramo()).isEqualTo(6.0E9);
        // La unidad de medida es fija por regla de negocio: no se toca durante la modificación
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);

        assertThat(resultado.getNombre()).isEqualTo("SafAle S-04 Modificada");
        assertThat(resultado.getTipo()).isEqualTo(TipoLevadura.LAGER);
        assertThat(resultado.getCantidadCelulasPorGramo()).isEqualTo(6.0E9);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);
        verify(levaduraRepository).findById(1L);
        verify(levaduraRepository).existsByNombreIgnoreCaseAndIdNot("SafAle S-04 Modificada", 1L);
    }

    @Test
    @DisplayName("modificarLevadura permite conservar el propio nombre actual al actualizar otros campos")
    void modificarLevadura_debePermitirConservarNombrePropio() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("safale s-04", TipoLevadura.ALE, 2.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("safale s-04", 1L)).thenReturn(false);
        when(levaduraRepository.save(levaduraEntity)).thenReturn(levaduraEntity);

        LevaduraResponseDTO resultado = levaduraServicio.modificarLevadura(1L, levaduraFormDTO);

        assertThat(resultado.getNombre()).isEqualTo("safale s-04");
        assertThat(resultado.getCantidadCelulasPorGramo()).isEqualTo(2.0E10);
        verify(levaduraRepository).save(levaduraEntity);
    }

    @Test
    @DisplayName("CP-ML-18: modificarLevadura permite cambiar solo el nombre de la levadura")
    void modificarLevadura_debePermitirCambiarSoloElNombre() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "Original", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("Nuevo Nombre", 1L)).thenReturn(false);
        when(levaduraRepository.save(levaduraEntity)).thenReturn(levaduraEntity);
        LevaduraFormDTO formDTO = levaduraFormDTO("Nuevo Nombre", TipoLevadura.ALE, 1.0E10);

        assertThatCode(() -> levaduraServicio.modificarLevadura(1L, formDTO)).doesNotThrowAnyException();

        assertThat(levaduraEntity.getNombre()).isEqualTo("Nuevo Nombre");
        verify(levaduraRepository).save(levaduraEntity);
    }

    @Test
    @DisplayName("CP-ML-19: modificarLevadura permite cambiar la cantidad de células por gramo aunque haya lotes que usan la levadura (el escalado de un lote iniciado queda guardado en su instantánea)")
    void modificarLevadura_debePermitirCambiarCelulasAunqueLotesUsenLaLevadura() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("SafAle S-04", 1L)).thenReturn(false);
        when(levaduraRepository.save(levaduraEntity)).thenReturn(levaduraEntity);
        LevaduraFormDTO formDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, 1.2E10);

        assertThatCode(() -> levaduraServicio.modificarLevadura(1L, formDTO)).doesNotThrowAnyException();

        assertThat(levaduraEntity.getCantidadCelulasPorGramo()).isEqualTo(1.2E10);
        verify(levaduraRepository).save(levaduraEntity);
    }

    // ==================== bajaLevadura ====================

    @Test
    @DisplayName("CP-BL-01: bajaLevadura lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaLevadura_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(levaduraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> levaduraServicio.bajaLevadura(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la levadura con ID: 99");

        verify(levaduraRepository).findById(99L);
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BL-02: bajaLevadura lanza ReglaNegocioException cuando la levadura forma parte de una receta activa")
    void bajaLevadura_debeRechazarSiFormaParteDeRecetaActiva() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(versionRecetaRepository.existsByDetalleLevaduraEnRecetaActiva(1L)).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.bajaLevadura(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la levadura porque forma parte de al menos una receta activa");

        verifyNoInteractions(planificacionProduccionRepository, ordenCompraRepository, catalogoProveedorRepository, loteInsumoRepository);
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BL-05: bajaLevadura lanza ReglaNegocioException y no persiste cuando la levadura forma parte de una planificación de producción en estado Pendiente")
    void bajaLevadura_debeRechazarSiFormaParteDePlanificacionPendiente() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(versionRecetaRepository.existsByDetalleLevaduraEnRecetaActiva(1L)).thenReturn(false);
        when(planificacionProduccionRepository.existsByDetalleLevaduraEnPlanificacionPendiente(1L)).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.bajaLevadura(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la levadura porque forma parte de una planificación de producción en estado Pendiente");

        verifyNoInteractions(ordenCompraRepository, catalogoProveedorRepository, loteInsumoRepository);
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BL-06: bajaLevadura lanza ReglaNegocioException y no persiste cuando la levadura se encuentra en una orden de compra en estado Pendiente")
    void bajaLevadura_debeRechazarSiSeEncuentraEnOrdenCompraPendiente() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(versionRecetaRepository.existsByDetalleLevaduraEnRecetaActiva(1L)).thenReturn(false);
        when(planificacionProduccionRepository.existsByDetalleLevaduraEnPlanificacionPendiente(1L)).thenReturn(false);
        when(ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(1L)).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.bajaLevadura(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la levadura porque se encuentra en al menos una orden de compra en estado Pendiente");

        verifyNoInteractions(catalogoProveedorRepository, loteInsumoRepository);
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BL-07: bajaLevadura lanza ReglaNegocioException y no persiste cuando la levadura se encuentra en el catálogo de un proveedor activo")
    void bajaLevadura_debeRechazarSiEstaEnCatalogoDeProveedorActivo() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(versionRecetaRepository.existsByDetalleLevaduraEnRecetaActiva(1L)).thenReturn(false);
        when(planificacionProduccionRepository.existsByDetalleLevaduraEnPlanificacionPendiente(1L)).thenReturn(false);
        when(ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(1L)).thenReturn(false);
        when(catalogoProveedorRepository.existsByInsumoId(1L)).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.bajaLevadura(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la levadura porque se encuentra asociada al catálogo de al menos un proveedor activo");

        verifyNoInteractions(loteInsumoRepository);
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BL-03: bajaLevadura lanza ReglaNegocioException cuando la levadura tiene stock")
    void bajaLevadura_debeRechazarSiTieneStock() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(versionRecetaRepository.existsByDetalleLevaduraEnRecetaActiva(1L)).thenReturn(false);
        when(planificacionProduccionRepository.existsByDetalleLevaduraEnPlanificacionPendiente(1L)).thenReturn(false);
        when(ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(1L)).thenReturn(false);
        when(catalogoProveedorRepository.existsByInsumoId(1L)).thenReturn(false);
        when(loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(1L, 0.0)).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.bajaLevadura(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la levadura porque tiene stock");

        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BL-04: bajaLevadura marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe y no tiene receta activa, planificación pendiente, orden de compra en estado Pendiente, catálogo de proveedor activo ni stock asociados")
    void bajaLevadura_debeMarcarBajaYRetornarLevaduraExistente() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(versionRecetaRepository.existsByDetalleLevaduraEnRecetaActiva(1L)).thenReturn(false);
        when(planificacionProduccionRepository.existsByDetalleLevaduraEnPlanificacionPendiente(1L)).thenReturn(false);
        when(ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(1L)).thenReturn(false);
        when(catalogoProveedorRepository.existsByInsumoId(1L)).thenReturn(false);
        when(loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(1L, 0.0)).thenReturn(false);
        when(levaduraRepository.save(levaduraEntity)).thenReturn(levaduraEntity);

        LevaduraResponseDTO resultado = levaduraServicio.bajaLevadura(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(levaduraEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertLevaduraDTO(levaduraEntity, resultado);
        verify(levaduraRepository).findById(1L);
        verify(levaduraRepository).save(levaduraEntity);
        verify(levaduraRepository, never()).delete(any());
    }

    // ==================== validaciones técnicas de Levadura (números finitos, techos y longitudes) ====================

    @Test
    @DisplayName("CP-AL-11: altaLevadura lanza ReglaNegocioException y no consulta el repositorio cuando la cantidad de células por gramo es NaN")
    void altaLevadura_debeRechazar_celulasNaN() {
        LevaduraFormDTO dto = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, Double.NaN);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células viables por gramo debe ser mayor a 0");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("CP-AL-12: altaLevadura lanza ReglaNegocioException y no consulta el repositorio cuando la cantidad de células por gramo es infinita positiva")
    void altaLevadura_debeRechazar_celulasInfinitas() {
        LevaduraFormDTO dto = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, Double.POSITIVE_INFINITY);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células viables por gramo debe estar entre 6.000 y 20.000 millones");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("CP-AL-13: altaLevadura lanza ReglaNegocioException y no consulta el repositorio cuando la cantidad de células por gramo es infinita negativa")
    void altaLevadura_debeRechazar_celulasInfinitasNegativas() {
        LevaduraFormDTO dto = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, Double.NEGATIVE_INFINITY);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células viables por gramo debe estar entre 6.000 y 20.000 millones");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("CP-AL-14: altaLevadura persiste con éxito cuando la cantidad de células por gramo es exactamente 6.000.000.000 (valor límite válido)")
    void altaLevadura_debeAceptar_celulasEnElMinimo() {
        LevaduraFormDTO dto = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, 6_000_000_000.0);
        when(levaduraRepository.existsByNombreIgnoreCase("SafAle S-04")).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> levaduraServicio.altaLevadura(dto)).doesNotThrowAnyException();

        verify(levaduraRepository).save(any(LevaduraEntity.class));
    }

    @Test
    @DisplayName("CP-AL-15: altaLevadura persiste con éxito cuando la cantidad de células por gramo es exactamente 20.000.000.000 (valor límite válido)")
    void altaLevadura_debeAceptar_celulasEnElMaximo() {
        LevaduraFormDTO dto = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, 20_000_000_000.0);
        when(levaduraRepository.existsByNombreIgnoreCase("SafAle S-04")).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> levaduraServicio.altaLevadura(dto)).doesNotThrowAnyException();

        verify(levaduraRepository).save(any(LevaduraEntity.class));
    }

    @Test
    @DisplayName("CP-AL-16: altaLevadura lanza ReglaNegocioException y no consulta el repositorio cuando el nombre tiene 101 caracteres")
    void altaLevadura_debeRechazar_nombreDe101Caracteres() {
        LevaduraFormDTO dto = levaduraFormDTO("A".repeat(101), TipoLevadura.ALE, 1.0E10);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre no puede superar 100 caracteres");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("CP-AL-17: altaLevadura persiste con éxito cuando el nombre tiene exactamente 100 caracteres (valor límite válido)")
    void altaLevadura_debeAceptar_nombreDe100Caracteres() {
        LevaduraFormDTO dto = levaduraFormDTO("A".repeat(100), TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.existsByNombreIgnoreCase("A".repeat(100))).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> levaduraServicio.altaLevadura(dto)).doesNotThrowAnyException();

        verify(levaduraRepository).save(any(LevaduraEntity.class));
    }

    @Test
    @DisplayName("CP-ML-06: modificarLevadura lanza ReglaNegocioException y no consulta la unicidad cuando la cantidad de células por gramo es NaN")
    void modificarLevadura_debeRechazar_celulasNaN() {
        LevaduraFormDTO dto = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, Double.NaN);

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células viables por gramo debe ser mayor a 0");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("CP-ML-07: modificarLevadura lanza ReglaNegocioException y no consulta la unicidad cuando el nombre tiene 101 caracteres")
    void modificarLevadura_debeRechazar_nombreDe101Caracteres() {
        LevaduraFormDTO dto = levaduraFormDTO("A".repeat(101), TipoLevadura.ALE, 1.0E10);

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre no puede superar 100 caracteres");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("CP-ML-08: modificarLevadura persiste con éxito cuando el nombre tiene exactamente 100 caracteres (valor límite válido)")
    void modificarLevadura_debeAceptar_nombreDe100Caracteres() {
        LevaduraFormDTO dto = levaduraFormDTO("A".repeat(100), TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10)));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("A".repeat(100), 1L)).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> levaduraServicio.modificarLevadura(1L, dto)).doesNotThrowAnyException();

        verify(levaduraRepository).save(any(LevaduraEntity.class));
    }

    // ==================== helpers ====================

    private static LevaduraEntity crearLevaduraEntity(Long id, String nombre, TipoLevadura tipo, Double cantidadCelulasPorGramo) {
        return LevaduraEntity.builder()
                .id(id)
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.GRAMO)
                .tipo(tipo)
                .cantidadCelulasPorGramo(cantidadCelulasPorGramo)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static LevaduraFormDTO levaduraFormDTO(String nombre, TipoLevadura tipo, Double cantidadCelulasPorGramo) {
        return LevaduraFormDTO.builder()
                .nombre(nombre)
                .tipo(tipo)
                .cantidadCelulasPorGramo(cantidadCelulasPorGramo)
                .build();
    }

    private static void assertLevaduraDTO(LevaduraEntity entidad, LevaduraResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getUnidadDeMedida()).isEqualTo(entidad.getUnidadDeMedida());
        assertThat(dto.getTipo()).isEqualTo(entidad.getTipo());
        assertThat(dto.getCantidadCelulasPorGramo()).isEqualTo(entidad.getCantidadCelulasPorGramo());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }

    // ==================== normalización y unicidad ====================

    @Test
    @DisplayName("CP-AL-18: altaLevadura guarda el nombre normalizado y consulta la unicidad con ese valor")
    void altaLevadura_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        LevaduraFormDTO formDTO = levaduraFormDTO("  Pilsen   Nacional  ", TipoLevadura.ALE, 10_000_000_000.0);
        when(levaduraRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        levaduraServicio.altaLevadura(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<LevaduraEntity> captor = ArgumentCaptor.forClass(LevaduraEntity.class);
        verify(levaduraRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(formDTO.getNombre()).isEqualTo("  Pilsen   Nacional  ");
        verify(levaduraRepository).existsByNombreIgnoreCase("Pilsen Nacional");
        verify(levaduraRepository).flush();
    }

    @Test
    @DisplayName("CP-AL-19: altaLevadura lanza ReglaNegocioException y no consulta el repositorio cuando el nombre está vacío")
    void altaLevadura_debeRechazarValorVacio() {
        LevaduraFormDTO formDTO = levaduraFormDTO("   ", TipoLevadura.ALE, 10_000_000_000.0);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("CP-AL-20: altaLevadura lanza RecursoDuplicadoException cuando el nombre normalizado ya existe")
    void altaLevadura_debeDetectarDuplicadoConElValorNormalizado() {
        LevaduraFormDTO formDTO = levaduraFormDTO("  pilsen ", TipoLevadura.ALE, 10_000_000_000.0);
        when(levaduraRepository.existsByNombreIgnoreCase("pilsen")).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una levadura con el nombre 'pilsen'");

        verify(levaduraRepository).existsByNombreIgnoreCase("pilsen");
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AL-21: altaLevadura traduce la violación del índice único udx_insumo_nombre al mensaje de duplicado")
    void altaLevadura_debeTraducirLaViolacionDelIndiceUnico() {
        LevaduraFormDTO formDTO = levaduraFormDTO("Pilsen Nacional", TipoLevadura.ALE, 10_000_000_000.0);
        when(levaduraRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_insumo_nombre"));

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una levadura con el nombre 'Pilsen Nacional'");
    }

    @Test
    @DisplayName("CP-AL-22: altaLevadura permite reutilizar el nombre de un registro dado de baja")
    void altaLevadura_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        LevaduraFormDTO formDTO = levaduraFormDTO("Pilsen Nacional", TipoLevadura.ALE, 10_000_000_000.0);
        // El único registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(levaduraRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> levaduraServicio.altaLevadura(formDTO)).doesNotThrowAnyException();

        verify(levaduraRepository).save(any(LevaduraEntity.class));
    }

    @Test
    @DisplayName("CP-AL-23: altaLevadura permite un nombre que difiere de otro solo en una tilde")
    void altaLevadura_debeTratarComoDistintosLosNombresConDistintaTilde() {
        LevaduraFormDTO formDTO = levaduraFormDTO("Pílsen", TipoLevadura.ALE, 10_000_000_000.0);
        when(levaduraRepository.existsByNombreIgnoreCase("Pílsen")).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> levaduraServicio.altaLevadura(formDTO)).doesNotThrowAnyException();

        verify(levaduraRepository).existsByNombreIgnoreCase("Pílsen");
        verify(levaduraRepository).save(any(LevaduraEntity.class));
    }

    @Test
    @DisplayName("CP-ML-09: modificarLevadura guarda el nombre normalizado y consulta la unicidad con ese valor")
    void modificarLevadura_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        LevaduraFormDTO formDTO = levaduraFormDTO("  Pilsen   Nacional  ", TipoLevadura.ALE, 10_000_000_000.0);
        LevaduraEntity existente = crearLevaduraEntity(1L, "Original", TipoLevadura.ALE, 10_000_000_000.0);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(levaduraRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        levaduraServicio.modificarLevadura(1L, formDTO);

        // === ASSERTS ===
        assertThat(existente.getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(formDTO.getNombre()).isEqualTo("  Pilsen   Nacional  ");
        verify(levaduraRepository).existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L);
        verify(levaduraRepository).save(existente);
        verify(levaduraRepository).flush();
    }

    @Test
    @DisplayName("CP-ML-10: modificarLevadura lanza ReglaNegocioException cuando el nombre está vacío")
    void modificarLevadura_debeRechazarValorVacio() {
        LevaduraFormDTO formDTO = levaduraFormDTO("   ", TipoLevadura.ALE, 10_000_000_000.0);

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("CP-ML-11: modificarLevadura lanza RecursoDuplicadoException cuando el nombre normalizado está en uso por otro registro")
    void modificarLevadura_debeDetectarDuplicadoConElValorNormalizado() {
        LevaduraFormDTO formDTO = levaduraFormDTO("  pilsen ", TipoLevadura.ALE, 10_000_000_000.0);
        LevaduraEntity existente = crearLevaduraEntity(1L, "Original", TipoLevadura.ALE, 10_000_000_000.0);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("pilsen", 1L)).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una levadura con el nombre 'pilsen'");

        verify(levaduraRepository).existsByNombreIgnoreCaseAndIdNot("pilsen", 1L);
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ML-12: modificarLevadura traduce la violación del índice único udx_insumo_nombre al mensaje de duplicado")
    void modificarLevadura_debeTraducirLaViolacionDelIndiceUnico() {
        LevaduraFormDTO formDTO = levaduraFormDTO("Pilsen Nacional", TipoLevadura.ALE, 10_000_000_000.0);
        LevaduraEntity existente = crearLevaduraEntity(1L, "Original", TipoLevadura.ALE, 10_000_000_000.0);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(levaduraRepository.save(existente)).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_insumo_nombre"));

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una levadura con el nombre 'Pilsen Nacional'");
    }

    @Test
    @DisplayName("CP-ML-13: modificarLevadura permite reutilizar el nombre de un registro dado de baja")
    void modificarLevadura_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        LevaduraFormDTO formDTO = levaduraFormDTO("Pilsen Nacional", TipoLevadura.ALE, 10_000_000_000.0);
        LevaduraEntity existente = crearLevaduraEntity(1L, "Original", TipoLevadura.ALE, 10_000_000_000.0);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(existente));
        // El único otro registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(levaduraRepository.save(existente)).thenReturn(existente);

        assertThatCode(() -> levaduraServicio.modificarLevadura(1L, formDTO)).doesNotThrowAnyException();

        assertThat(existente.getNombre()).isEqualTo("Pilsen Nacional");
        verify(levaduraRepository).save(existente);
    }
}
