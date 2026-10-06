package com.github.heikyudev.maestrocervecero.service.implementation.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.DetalleLupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.UsoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.receta.VersionRecetaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.EstadoSolicitud;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILevaduraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILupuloRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IMaltaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IRecetaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IVersionRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleLevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleLupuloFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.DetalleMaltaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.RecetaFormDTO;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.receta.VersionRecetaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.receta.RecetaResponseDTO;
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
class RecetaServicioImplTest {

    private static final Long MALTA_ID = 1L;
    private static final Long LUPULO_ID = 1L;
    private static final Long LEVADURA_ID = 1L;

    @Mock
    private IRecetaRepository recetaRepository;
    @Mock
    private IVersionRecetaRepository versionRecetaRepository;
    @Mock
    private IMaltaRepository maltaRepository;
    @Mock
    private ILupuloRepository lupuloRepository;
    @Mock
    private ILevaduraRepository levaduraRepository;
    @Mock
    private IPlanificacionProduccionRepository planificacionProduccionRepository;

    @InjectMocks
    private RecetaServicioImpl recetaServicio;

    // ==================== filtrarRecetas ====================

    @Test
    @DisplayName("CP-FR-01: filtrarRecetas retorna una página de recetas correctamente mapeada a DTO cuando se filtra por nombre")
    void filtrarRecetas_debeRetornarPaginaMapeadaFiltrandoPorNombre() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        RecetaEntity receta1 = crearRecetaEntityConVersionActiva(1L, "IPA Clásica");
        RecetaEntity receta2 = crearRecetaEntityConVersionActiva(2L, "IPA Doble");
        when(recetaRepository.filtrarRecetas("IPA", pageable))
                .thenReturn(new PageImpl<>(List.of(receta1, receta2), pageable, 2));

        // === EJECUCION ===
        Page<RecetaResponseDTO> resultado = recetaServicio.filtrarRecetas("IPA", pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent().get(0).getVersion().getNombre()).isEqualTo("IPA Clásica");
        assertThat(resultado.getContent().get(1).getVersion().getNombre()).isEqualTo("IPA Doble");
        verify(recetaRepository).filtrarRecetas("IPA", pageable);
    }

    @Test
    @DisplayName("CP-FR-02: filtrarRecetas propaga nombre nulo sin restringir ese criterio")
    void filtrarRecetas_debePropagarNombreNulo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        RecetaEntity receta1 = crearRecetaEntityConVersionActiva(1L, "IPA Clásica");
        RecetaEntity receta2 = crearRecetaEntityConVersionActiva(2L, "Stout Imperial");
        when(recetaRepository.filtrarRecetas(null, pageable))
                .thenReturn(new PageImpl<>(List.of(receta1, receta2), pageable, 2));

        // === EJECUCION ===
        Page<RecetaResponseDTO> resultado = recetaServicio.filtrarRecetas(null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(recetaRepository).filtrarRecetas(null, pageable);
    }

    @Test
    @DisplayName("CP-FR-03: filtrarRecetas retorna una página vacía cuando ninguna receta cumple el criterio")
    void filtrarRecetas_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(recetaRepository.filtrarRecetas("Inexistente", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<RecetaResponseDTO> resultado = recetaServicio.filtrarRecetas("Inexistente", pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(recetaRepository).filtrarRecetas("Inexistente", pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la receta cuando el ID existe")
    void buscarPorId_debeRetornarRecetaExistente() {
        // === PREPARACION DE DATOS ===
        RecetaEntity recetaEntity = crearRecetaEntityConVersionActiva(1L, "IPA Clásica");
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaEntity));

        // === EJECUCION ===
        RecetaResponseDTO resultado = recetaServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getVersion().getNombre()).isEqualTo("IPA Clásica");
        assertThat(resultado.getEstado()).isEqualTo(Estado.ACTIVO);
        verify(recetaRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dada de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para recetas dadas de baja
        when(recetaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recetaServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la receta con ID: 99");
        verify(recetaRepository).findById(99L);
    }

    // ==================== altaReceta ====================

    @Test
    @DisplayName("CP-AR-01: altaReceta lanza ReglaNegocioException y no consulta la BD cuando el volumen base es cero (límite)")
    void altaReceta_debeRechazarVolumenBaseCero() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().volumenBase(0.0).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El volumen base debe ser mayor a 0");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-02: altaReceta lanza ReglaNegocioException cuando el volumen base es nulo")
    void altaReceta_debeRechazarVolumenBaseNulo() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().volumenBase(null).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El volumen base debe ser mayor a 0");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-03: altaReceta lanza ReglaNegocioException cuando la relación de empaste es cero (límite)")
    void altaReceta_debeRechazarRelacionDeEmpasteCero() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().relacionDeEmpaste(0.0).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La relación de empaste debe ser mayor a 0");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-04: altaReceta lanza ReglaNegocioException cuando la OG objetivo es igual a la FG objetivo (límite)")
    void altaReceta_debeRechazarOgIgualAFg() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().ogObjetivo(1.030).fgObjetivo(1.030).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad original objetivo (OG) debe ser mayor a la densidad final objetivo (FG)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-05: altaReceta lanza ReglaNegocioException cuando la OG objetivo es menor a la FG objetivo")
    void altaReceta_debeRechazarOgMenorAFg() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().ogObjetivo(1.020).fgObjetivo(1.030).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad original objetivo (OG) debe ser mayor a la densidad final objetivo (FG)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-06: altaReceta lanza ReglaNegocioException cuando el IBU objetivo es negativo (límite inferior)")
    void altaReceta_debeRechazarIbuObjetivoNegativo() {
        // Nota: el campo ibuObjetivo es Integer (no admite -0.1 como en el caso de prueba documentado); se usa -1 como límite negativo equivalente.
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().ibuObjetivo(-1).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El IBU objetivo no puede ser negativo");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-07: altaReceta persiste y retorna DTO cuando el IBU objetivo es cero (límite válido)")
    void altaReceta_debePersistirConIbuObjetivoCero() {
        // === PREPARACION DE DATOS ===
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder().ibuObjetivo(0).build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        RecetaResponseDTO resultado = recetaServicio.altaReceta(recetaFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getVersion().getIbuObjetivo()).isZero();
        verify(recetaRepository).save(any(RecetaEntity.class));
    }

    @Test
    @DisplayName("CP-AR-08: altaReceta lanza ReglaNegocioException cuando la duración de maceración es cero (límite)")
    void altaReceta_debeRechazarDuracionMaceracionCero() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().duracionMaceracion(0).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La duración estimada de la maceración debe ser mayor a 0");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-09: altaReceta lanza ReglaNegocioException cuando la duración de hervido es cero (límite)")
    void altaReceta_debeRechazarDuracionHervidoCero() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().duracionHervido(0).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La duración estimada del hervido debe ser mayor a 0");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-10: altaReceta lanza ReglaNegocioException cuando la duración de fermentación es cero (límite)")
    void altaReceta_debeRechazarDuracionFermentacionCero() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().duracionFermentacion(0).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La duración estimada de la fermentación debe ser mayor a 0");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-11: altaReceta lanza ReglaNegocioException cuando la duración de maduración es cero (límite)")
    void altaReceta_debeRechazarDuracionMaduracionCero() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().duracionMaduracion(0).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La duración estimada de la maduración debe ser mayor a 0");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-12: altaReceta lanza ReglaNegocioException y no consulta maltaRepository cuando no hay maltas")
    void altaReceta_debeRechazarSinMaltas() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().detallesMalta(List.of()).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta debe tener al menos una malta");

        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("CP-AR-13: altaReceta lanza ReglaNegocioException cuando la cantidad de una malta es cero (límite)")
    void altaReceta_debeRechazarCantidadMaltaCero() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesMalta(List.of(detalleMaltaFormDTO(0.0, MALTA_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de cada malta debe ser mayor a 0");

        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("CP-AR-14: altaReceta lanza ReglaNegocioException y no consulta lupuloRepository cuando no hay lúpulos")
    void altaReceta_debeRechazarSinLupulos() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().detallesLupulo(List.of()).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta debe tener al menos un lúpulo");

        verifyNoInteractions(lupuloRepository);
    }

    @Test
    @DisplayName("CP-AR-15: altaReceta lanza ReglaNegocioException cuando la cantidad de un lúpulo es cero (límite)")
    void altaReceta_debeRechazarCantidadLupuloCero() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLupulo(List.of(detalleLupuloFormDTO(0.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de cada lúpulo debe ser mayor a 0");

        verifyNoInteractions(lupuloRepository);
    }

    @Test
    @DisplayName("CP-AR-16: altaReceta lanza ReglaNegocioException cuando ningún lúpulo tiene uso HERVOR")
    void altaReceta_debeRechazarSinLupuloHervor() {
        // Nota: UsoLupulo solo define HERVOR, WHIRLPOOL y DRY_HOP (no AROMA/DRY_HOPPING como en el caso documentado); se usan los valores equivalentes del enum.
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLupulo(List.of(
                        detalleLupuloFormDTO(20.0, UsoLupulo.WHIRLPOOL, TipoEtapa.HERVIDO, null, LUPULO_ID),
                        detalleLupuloFormDTO(15.0, UsoLupulo.DRY_HOP, TipoEtapa.FERMENTACION, null, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta debe tener al menos un lúpulo con uso Hervor");

        verifyNoInteractions(lupuloRepository);
    }

    @Test
    @DisplayName("CP-AR-17: altaReceta lanza ReglaNegocioException cuando el tiempo de hervor es cero (límite inferior)")
    void altaReceta_debeRechazarTiempoDeHervorCero() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .duracionHervido(60)
                .detallesLupulo(List.of(detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 0.0, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El tiempo de hervor de cada lúpulo con uso Hervor debe ser mayor a 0 y menor a la duración estimada del hervido");

        verifyNoInteractions(lupuloRepository);
    }

    @Test
    @DisplayName("CP-AR-18: altaReceta lanza ReglaNegocioException cuando el tiempo de hervor iguala la duración del hervido (límite superior)")
    void altaReceta_debeRechazarTiempoDeHervorIgualADuracion() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .duracionHervido(60)
                .detallesLupulo(List.of(detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 60.0, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El tiempo de hervor de cada lúpulo con uso Hervor debe ser mayor a 0 y menor a la duración estimada del hervido");

        verifyNoInteractions(lupuloRepository);
    }

    @Test
    @DisplayName("CP-AR-19: altaReceta persiste cuando el tiempo de hervor está en el límite superior válido")
    void altaReceta_debePersistirConTiempoDeHervorEnLimiteValido() {
        // === PREPARACION DE DATOS ===
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder()
                .duracionHervido(60)
                .detallesLupulo(List.of(detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 59.0, LUPULO_ID)))
                .build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        RecetaResponseDTO resultado = recetaServicio.altaReceta(recetaFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getVersion().getDetallesLupulo().get(0).getTiempoDeHervor()).isEqualTo(59.0);
        verify(recetaRepository).save(any(RecetaEntity.class));
    }

    @Test
    @DisplayName("CP-AR-20: altaReceta normaliza a cero el tiempo de hervor de un lúpulo que no es HERVOR")
    void altaReceta_debeNormalizarTiempoDeHervorEnLupuloNoHervor() {
        // === PREPARACION DE DATOS ===
        DetalleLupuloFormDTO lupuloHervor = detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID);
        DetalleLupuloFormDTO lupuloNoHervor = detalleLupuloFormDTO(15.0, UsoLupulo.WHIRLPOOL, TipoEtapa.HERVIDO, 30.0, LUPULO_ID);
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder()
                .detallesLupulo(List.of(lupuloHervor, lupuloNoHervor))
                .build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        recetaServicio.altaReceta(recetaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<RecetaEntity> captor = ArgumentCaptor.forClass(RecetaEntity.class);
        verify(recetaRepository).save(captor.capture());
        List<DetalleLupuloEntity> detallesGuardados = captor.getValue().getVersiones().get(0).getDetallesLupulo();
        assertThat(detallesGuardados).hasSize(2);
        assertThat(detallesGuardados.get(1).getTiempoDeHervor()).isZero();
    }

    @Test
    @DisplayName("CP-AR-21: altaReceta lanza ReglaNegocioException y no consulta levaduraRepository cuando no hay levaduras")
    void altaReceta_debeRechazarSinLevaduras() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().detallesLevadura(List.of()).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta debe tener al menos una levadura");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("CP-AR-22: altaReceta lanza ReglaNegocioException cuando la cantidad de una levadura es cero (límite)")
    void altaReceta_debeRechazarCantidadLevaduraCero() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLevadura(List.of(detalleLevaduraFormDTO(0.0, LEVADURA_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de cada levadura debe ser mayor a 0");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("CP-AR-23: altaReceta lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe")
    void altaReceta_debeRechazarNombreDuplicado() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().nombre("IPA Clásica").build());
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrue("IPA Clásica")).thenReturn(true);

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una receta con el nombre 'IPA Clásica'");

        verify(versionRecetaRepository).existsByNombreIgnoreCaseAndEsUltimaVersionTrue("IPA Clásica");
        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AR-24: altaReceta lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case (case-insensitive)")
    void altaReceta_debeRechazarNombreDuplicadoCaseInsensitive() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().nombre("ipa clásica").build());
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrue("ipa clásica")).thenReturn(true);

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una receta con el nombre 'ipa clásica'");

        verify(versionRecetaRepository).existsByNombreIgnoreCaseAndEsUltimaVersionTrue("ipa clásica");
        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AR-25: altaReceta lanza RecursoNoEncontradoException y no persiste cuando la malta referenciada no existe")
    void altaReceta_debeRechazarMaltaInexistente() {
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder()
                .detallesMalta(List.of(detalleMaltaFormDTO(2.0, 99L)))
                .build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrue("IPA Clásica")).thenReturn(false);
        when(maltaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la malta con ID: 99");

        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AR-26: altaReceta lanza RecursoNoEncontradoException y no persiste cuando el lúpulo referenciado no existe")
    void altaReceta_debeRechazarLupuloInexistente() {
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder()
                .detallesLupulo(List.of(detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, 99L)))
                .build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrue("IPA Clásica")).thenReturn(false);
        when(maltaRepository.findById(MALTA_ID)).thenReturn(Optional.of(maltaEntity(MALTA_ID)));
        when(lupuloRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lúpulo con ID: 99");

        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AR-27: altaReceta lanza RecursoNoEncontradoException y no persiste cuando la levadura referenciada no existe")
    void altaReceta_debeRechazarLevaduraInexistente() {
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder()
                .detallesLevadura(List.of(detalleLevaduraFormDTO(11.0, 99L)))
                .build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrue("IPA Clásica")).thenReturn(false);
        when(maltaRepository.findById(MALTA_ID)).thenReturn(Optional.of(maltaEntity(MALTA_ID)));
        when(lupuloRepository.findById(LUPULO_ID)).thenReturn(Optional.of(lupuloEntity(LUPULO_ID)));
        when(levaduraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la levadura con ID: 99");

        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AR-36: altaReceta persiste la receta completa con maltas, lúpulos y levadura (camino feliz)")
    void altaReceta_debePersistirRecetaCompleta() {
        // === PREPARACION DE DATOS ===
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder()
                .nombre("IPA Nueva")
                .detallesMalta(List.of(detalleMaltaFormDTO(2.0, 1L), detalleMaltaFormDTO(0.5, 2L)))
                .detallesLupulo(List.of(
                        detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, 1L),
                        detalleLupuloFormDTO(10.0, UsoLupulo.WHIRLPOOL, TipoEtapa.HERVIDO, null, 2L),
                        detalleLupuloFormDTO(15.0, UsoLupulo.DRY_HOP, TipoEtapa.MADURACION, null, 3L)))
                .build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        RecetaResponseDTO resultado = recetaServicio.altaReceta(recetaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<RecetaEntity> captor = ArgumentCaptor.forClass(RecetaEntity.class);
        verify(recetaRepository).save(captor.capture());
        VersionRecetaEntity versionGuardada = captor.getValue().getVersiones().get(0);
        assertThat(versionGuardada.isEsUltimaVersion()).isTrue();
        assertThat(versionGuardada.getDetallesMalta()).hasSize(2);
        assertThat(versionGuardada.getDetallesLupulo()).hasSize(3);
        assertThat(versionGuardada.getDetallesLevadura()).hasSize(1);
        assertThat(resultado.getVersion().isEsUltimaVersion()).isTrue();
        // El alta siempre debe registrar a la receta como ACTIVA, sin importar lo que traiga el FormDTO
        assertThat(captor.getValue().getEstado()).isEqualTo(Estado.ACTIVO);
        assertThat(resultado.getEstado()).isEqualTo(Estado.ACTIVO);
    }

    @Test
    @DisplayName("CP-AR-37: altaReceta lanza ReglaNegocioException cuando un lúpulo HERVOR tiene una etapa de uso distinta de HERVIDO")
    void altaReceta_debeRechazarLupuloHervorConEtapaDistintaDeHervido() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLupulo(List.of(detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.FERMENTACION, 30.0, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa de uso de un lúpulo con uso Hervor o Whirlpool debe ser Hervido");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-38: altaReceta lanza ReglaNegocioException cuando un lúpulo WHIRLPOOL tiene una etapa de uso distinta de HERVIDO")
    void altaReceta_debeRechazarLupuloWhirlpoolConEtapaDistintaDeHervido() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLupulo(List.of(
                        detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID),
                        detalleLupuloFormDTO(10.0, UsoLupulo.WHIRLPOOL, TipoEtapa.MADURACION, null, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa de uso de un lúpulo con uso Hervor o Whirlpool debe ser Hervido");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-39: altaReceta lanza ReglaNegocioException cuando un lúpulo DRY_HOP tiene la etapa de uso HERVIDO")
    void altaReceta_debeRechazarLupuloDryHopConEtapaHervido() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLupulo(List.of(
                        detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID),
                        detalleLupuloFormDTO(15.0, UsoLupulo.DRY_HOP, TipoEtapa.HERVIDO, null, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa de uso de un lúpulo con uso Dry hop debe ser Fermentación o Maduración");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-40: altaReceta lanza ReglaNegocioException cuando la etapa de uso de un lúpulo es nula")
    void altaReceta_debeRechazarLupuloConEtapaDeUsoNula() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLupulo(List.of(detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, null, 30.0, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa de uso de un lúpulo con uso Hervor o Whirlpool debe ser Hervido");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-41: altaReceta lanza ReglaNegocioException cuando la misma malta se repite")
    void altaReceta_debeRechazarMaltaRepetida() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesMalta(List.of(detalleMaltaFormDTO(2.0, MALTA_ID), detalleMaltaFormDTO(1.0, MALTA_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta no puede tener la misma malta más de una vez");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-42: altaReceta lanza ReglaNegocioException cuando la misma levadura se repite")
    void altaReceta_debeRechazarLevaduraRepetida() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLevadura(List.of(detalleLevaduraFormDTO(11.0, LEVADURA_ID), detalleLevaduraFormDTO(5.0, LEVADURA_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta no puede tener la misma levadura más de una vez");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-43: altaReceta lanza ReglaNegocioException cuando se repite el mismo lúpulo con el mismo uso y tiempo de hervor")
    void altaReceta_debeRechazarLupuloRepetidoConMismoUsoYTiempo() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLupulo(List.of(
                        detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID),
                        detalleLupuloFormDTO(20.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta no puede repetir un lúpulo con el mismo uso y el mismo tiempo de hervor");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-44: altaReceta lanza ReglaNegocioException cuando se repite el mismo lúpulo WHIRLPOOL (el tiempo de hervor se normaliza a cero)")
    void altaReceta_debeRechazarLupuloRepetidoNoHervorComparandoTiempoNormalizado() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLupulo(List.of(
                        detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID),
                        detalleLupuloFormDTO(10.0, UsoLupulo.WHIRLPOOL, TipoEtapa.HERVIDO, null, LUPULO_ID),
                        detalleLupuloFormDTO(5.0, UsoLupulo.WHIRLPOOL, TipoEtapa.HERVIDO, 15.0, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta no puede repetir un lúpulo con el mismo uso y el mismo tiempo de hervor");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-45: altaReceta persiste cuando el mismo lúpulo se repite con uso HERVOR y distinto tiempo de hervor")
    void altaReceta_debePersistirMismoLupuloHervorConDistintoTiempo() {
        // === PREPARACION DE DATOS ===
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder()
                .detallesLupulo(List.of(
                        detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID),
                        detalleLupuloFormDTO(20.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 15.0, LUPULO_ID)))
                .build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        recetaServicio.altaReceta(recetaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<RecetaEntity> captor = ArgumentCaptor.forClass(RecetaEntity.class);
        verify(recetaRepository).save(captor.capture());
        assertThat(captor.getValue().getVersiones().get(0).getDetallesLupulo()).hasSize(2);
    }

    @Test
    @DisplayName("CP-AR-46: altaReceta persiste cuando el mismo lúpulo se repite con distinto uso (HERVOR y DRY_HOP)")
    void altaReceta_debePersistirMismoLupuloConDistintoUso() {
        // === PREPARACION DE DATOS ===
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder()
                .detallesLupulo(List.of(
                        detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID),
                        detalleLupuloFormDTO(15.0, UsoLupulo.DRY_HOP, TipoEtapa.FERMENTACION, null, LUPULO_ID)))
                .build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        recetaServicio.altaReceta(recetaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<RecetaEntity> captor = ArgumentCaptor.forClass(RecetaEntity.class);
        verify(recetaRepository).save(captor.capture());
        assertThat(captor.getValue().getVersiones().get(0).getDetallesLupulo()).hasSize(2);
    }

    @Test
    @DisplayName("CP-AR-51: altaReceta lanza ReglaNegocioException y no consulta la BD cuando la OG objetivo es menor a 1,020 (límite inferior inválido)")
    void altaReceta_debeRechazarOgBajoRango() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().ogObjetivo(1.019).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad inicial (OG) debe estar entre 1,020 y 1,120 y tener como máximo tres decimales (por ejemplo 1,050)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-52: altaReceta lanza ReglaNegocioException y no consulta la BD cuando la OG objetivo es mayor a 1,120 (límite superior inválido)")
    void altaReceta_debeRechazarOgSobreRango() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().ogObjetivo(1.121).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad inicial (OG) debe estar entre 1,020 y 1,120 y tener como máximo tres decimales (por ejemplo 1,050)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-53: altaReceta lanza ReglaNegocioException y no consulta la BD cuando la OG objetivo tiene cuatro decimales")
    void altaReceta_debeRechazarOgConCuatroDecimales() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().ogObjetivo(1.0505).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad inicial (OG) debe estar entre 1,020 y 1,120 y tener como máximo tres decimales (por ejemplo 1,050)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-54: altaReceta lanza ReglaNegocioException y no consulta la BD cuando la OG objetivo es nula")
    void altaReceta_debeRechazarOgNula() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().ogObjetivo(null).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad inicial (OG) debe estar entre 1,020 y 1,120 y tener como máximo tres decimales (por ejemplo 1,050)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-55: altaReceta lanza ReglaNegocioException y no consulta la BD cuando la FG objetivo es menor a 1,000 (límite inferior inválido)")
    void altaReceta_debeRechazarFgBajoRango() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().fgObjetivo(0.999).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad final (FG) debe estar entre 1,000 y 1,040 y tener como máximo tres decimales (por ejemplo 1,010)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-56: altaReceta lanza ReglaNegocioException y no consulta la BD cuando la FG objetivo es mayor a 1,040 (límite superior inválido)")
    void altaReceta_debeRechazarFgSobreRango() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().fgObjetivo(1.041).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad final (FG) debe estar entre 1,000 y 1,040 y tener como máximo tres decimales (por ejemplo 1,010)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-57: altaReceta lanza ReglaNegocioException y no consulta la BD cuando la FG objetivo tiene cuatro decimales")
    void altaReceta_debeRechazarFgConCuatroDecimales() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().fgObjetivo(1.0105).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad final (FG) debe estar entre 1,000 y 1,040 y tener como máximo tres decimales (por ejemplo 1,010)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-58: altaReceta lanza ReglaNegocioException y no consulta la BD cuando la FG objetivo es nula")
    void altaReceta_debeRechazarFgNula() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().fgObjetivo(null).build());

        assertThatThrownBy(() -> recetaServicio.altaReceta(recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad final (FG) debe estar entre 1,000 y 1,040 y tener como máximo tres decimales (por ejemplo 1,010)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-AR-59: altaReceta persiste y retorna DTO cuando la OG y la FG están en sus límites inferiores válidos (1,020 y 1,000)")
    void altaReceta_debePersistirConOgYFgEnLimiteInferior() {
        // === PREPARACION DE DATOS ===
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder().ogObjetivo(1.020).fgObjetivo(1.000).build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        RecetaResponseDTO resultado = recetaServicio.altaReceta(recetaFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getVersion().getOgObjetivo()).isEqualTo(1.020);
        assertThat(resultado.getVersion().getFgObjetivo()).isEqualTo(1.000);
        verify(recetaRepository).save(any(RecetaEntity.class));
    }

    @Test
    @DisplayName("CP-AR-60: altaReceta persiste y retorna DTO cuando la OG y la FG están en sus límites superiores válidos (1,120 y 1,040)")
    void altaReceta_debePersistirConOgYFgEnLimiteSuperior() {
        // === PREPARACION DE DATOS ===
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder().ogObjetivo(1.120).fgObjetivo(1.040).build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        RecetaResponseDTO resultado = recetaServicio.altaReceta(recetaFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getVersion().getOgObjetivo()).isEqualTo(1.120);
        assertThat(resultado.getVersion().getFgObjetivo()).isEqualTo(1.040);
        verify(recetaRepository).save(any(RecetaEntity.class));
    }

    @Test
    @DisplayName("CP-AR-61: altaReceta persiste y retorna DTO cuando la OG y la FG tienen dos y tres decimales (1,05 y 1,015)")
    void altaReceta_debePersistirConOgYFgConHastaTresDecimales() {
        // === PREPARACION DE DATOS ===
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder().ogObjetivo(1.05).fgObjetivo(1.015).build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        mockearAltaExitosa(versionFormDTO);

        // === EJECUCION ===
        RecetaResponseDTO resultado = recetaServicio.altaReceta(recetaFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getVersion().getOgObjetivo()).isEqualTo(1.05);
        assertThat(resultado.getVersion().getFgObjetivo()).isEqualTo(1.015);
        verify(recetaRepository).save(any(RecetaEntity.class));
    }

    // ==================== modificarReceta ====================

    @Test
    @DisplayName("CP-MR-01: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando los datos generales son inválidos")
    void modificarReceta_debeRechazarDatosGeneralesInvalidos() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().volumenBase(0.0).build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El volumen base debe ser mayor a 0");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-02: modificarReceta lanza ReglaNegocioException y no busca por ID ni persiste cuando faltan detalles de malta")
    void modificarReceta_debeRechazarSinDetallesMalta() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().detallesMalta(List.of()).build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta debe tener al menos una malta");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-03: modificarReceta lanza ReglaNegocioException y no busca por ID ni persiste cuando ningún lúpulo tiene uso HERVOR")
    void modificarReceta_debeRechazarSinLupuloHervor() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLupulo(List.of(
                        detalleLupuloFormDTO(20.0, UsoLupulo.WHIRLPOOL, TipoEtapa.HERVIDO, null, LUPULO_ID),
                        detalleLupuloFormDTO(15.0, UsoLupulo.DRY_HOP, TipoEtapa.FERMENTACION, null, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta debe tener al menos un lúpulo con uso Hervor");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-04: modificarReceta lanza ReglaNegocioException y no busca por ID ni persiste cuando faltan detalles de levadura")
    void modificarReceta_debeRechazarSinDetallesLevadura() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().detallesLevadura(List.of()).build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta debe tener al menos una levadura");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-06: modificarReceta lanza RecursoDuplicadoException y no busca por ID ni persiste cuando el nombre está en uso por otra receta")
    void modificarReceta_debeRechazarNombreEnUsoPorOtraReceta() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().nombre("RECETA-EXISTENTE").build());
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrueAndRecetaIdNot("RECETA-EXISTENTE", 1L)).thenReturn(true);

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una receta con el nombre 'RECETA-EXISTENTE'");

        verify(versionRecetaRepository).existsByNombreIgnoreCaseAndEsUltimaVersionTrueAndRecetaIdNot("RECETA-EXISTENTE", 1L);
        verify(recetaRepository, never()).findById(any());
        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MR-07: modificarReceta permite conservar el nombre propio actual y persiste la nueva versión (camino feliz)")
    void modificarReceta_debePermitirConservarNombrePropio() {
        // === PREPARACION DE DATOS ===
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder().nombre("IPA Clásica").build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        RecetaEntity recetaEntityExistente = crearRecetaEntityConVersiones(1L, crearVersionRecetaEntity(10L, "IPA Clásica", true));
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrueAndRecetaIdNot("IPA Clásica", 1L)).thenReturn(false);
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaEntityExistente));
        mockearInsumos(versionFormDTO);
        when(recetaRepository.save(recetaEntityExistente)).thenReturn(recetaEntityExistente);

        // === EJECUCION ===
        RecetaResponseDTO resultado = recetaServicio.modificarReceta(1L, recetaFormDTO);

        // === ASSERTS ===
        assertThat(resultado).isNotNull();
        assertThat(resultado.getVersion().getNombre()).isEqualTo("IPA Clásica");
        verify(recetaRepository).save(recetaEntityExistente);
    }

    @Test
    @DisplayName("CP-MR-08: modificarReceta lanza RecursoNoEncontradoException y no persiste cuando la receta no existe")
    void modificarReceta_debeLanzarExcepcionSiNoExiste() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().build());
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrueAndRecetaIdNot("IPA Clásica", 99L)).thenReturn(false);
        when(recetaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(99L, recetaFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la receta con ID: 99");

        verify(recetaRepository).findById(99L);
        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MR-09: modificarReceta lanza RecursoNoEncontradoException y no persiste cuando un insumo referenciado no existe")
    void modificarReceta_debeRechazarInsumoInexistente() {
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder()
                .detallesMalta(List.of(detalleMaltaFormDTO(2.0, 99L)))
                .build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        RecetaEntity recetaEntityExistente = crearRecetaEntityConVersiones(1L, crearVersionRecetaEntity(10L, "IPA Clásica", true));
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrueAndRecetaIdNot("IPA Clásica", 1L)).thenReturn(false);
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaEntityExistente));
        when(maltaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la malta con ID: 99");

        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MR-10: modificarReceta desactiva la versión previa, agrega la nueva y persiste (camino feliz)")
    void modificarReceta_debeVersionarExitosamente() {
        // === PREPARACION DE DATOS ===
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder().build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        VersionRecetaEntity versionActivaPrevia = crearVersionRecetaEntity(10L, "IPA Clásica", true);
        RecetaEntity recetaEntityExistente = crearRecetaEntityConVersiones(1L, versionActivaPrevia);
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrueAndRecetaIdNot("IPA Clásica", 1L)).thenReturn(false);
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaEntityExistente));
        mockearInsumos(versionFormDTO);
        when(recetaRepository.save(recetaEntityExistente)).thenReturn(recetaEntityExistente);

        // === EJECUCION ===
        RecetaResponseDTO resultado = recetaServicio.modificarReceta(1L, recetaFormDTO);

        // === ASSERTS ===
        assertThat(resultado).isNotNull();
        assertThat(versionActivaPrevia.isEsUltimaVersion()).isFalse();
        assertThat(recetaEntityExistente.getVersiones()).hasSize(2);
        long cantidadActivas = recetaEntityExistente.getVersiones().stream().filter(VersionRecetaEntity::isEsUltimaVersion).count();
        assertThat(cantidadActivas).isEqualTo(1);
        verify(recetaRepository).save(recetaEntityExistente);
    }

    @Test
    @DisplayName("CP-MR-11: modificarReceta versiona correctamente sobre un historial existente de múltiples versiones")
    void modificarReceta_debeVersionarSobreHistorialExistente() {
        // === PREPARACION DE DATOS ===
        VersionRecetaFormDTO versionFormDTO = versionValidaBuilder().build();
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionFormDTO);
        VersionRecetaEntity version1 = crearVersionRecetaEntity(10L, "IPA v1", false);
        VersionRecetaEntity version2 = crearVersionRecetaEntity(11L, "IPA v2", false);
        VersionRecetaEntity version3 = crearVersionRecetaEntity(12L, "IPA Clásica", true);
        RecetaEntity recetaEntityExistente = crearRecetaEntityConVersiones(1L, version1, version2, version3);
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrueAndRecetaIdNot("IPA Clásica", 1L)).thenReturn(false);
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaEntityExistente));
        mockearInsumos(versionFormDTO);
        when(recetaRepository.save(recetaEntityExistente)).thenReturn(recetaEntityExistente);

        // === EJECUCION ===
        recetaServicio.modificarReceta(1L, recetaFormDTO);

        // === ASSERTS ===
        assertThat(recetaEntityExistente.getVersiones()).hasSize(4);
        long cantidadActivas = recetaEntityExistente.getVersiones().stream().filter(VersionRecetaEntity::isEsUltimaVersion).count();
        assertThat(cantidadActivas).isEqualTo(1);
    }

    @Test
    @DisplayName("CP-MR-12: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando la etapa de uso de un lúpulo no corresponde a su uso")
    void modificarReceta_debeRechazarEtapaDeUsoIncoherenteConElUso() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLupulo(List.of(
                        detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID),
                        detalleLupuloFormDTO(15.0, UsoLupulo.DRY_HOP, TipoEtapa.HERVIDO, null, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa de uso de un lúpulo con uso Dry hop debe ser Fermentación o Maduración");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-13: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando la misma malta se repite")
    void modificarReceta_debeRechazarMaltaRepetida() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesMalta(List.of(detalleMaltaFormDTO(2.0, MALTA_ID), detalleMaltaFormDTO(1.0, MALTA_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta no puede tener la misma malta más de una vez");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-14: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando la misma levadura se repite")
    void modificarReceta_debeRechazarLevaduraRepetida() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLevadura(List.of(detalleLevaduraFormDTO(11.0, LEVADURA_ID), detalleLevaduraFormDTO(5.0, LEVADURA_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta no puede tener la misma levadura más de una vez");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-15: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando se repite el mismo lúpulo con el mismo uso y tiempo de hervor")
    void modificarReceta_debeRechazarLupuloRepetidoConMismoUsoYTiempo() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder()
                .detallesLupulo(List.of(
                        detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID),
                        detalleLupuloFormDTO(20.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID)))
                .build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La receta no puede repetir un lúpulo con el mismo uso y el mismo tiempo de hervor");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-19: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando la OG objetivo es menor a 1,020 (límite inferior inválido)")
    void modificarReceta_debeRechazarOgBajoRango() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().ogObjetivo(1.019).build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad inicial (OG) debe estar entre 1,020 y 1,120 y tener como máximo tres decimales (por ejemplo 1,050)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-20: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando la OG objetivo es mayor a 1,120 (límite superior inválido)")
    void modificarReceta_debeRechazarOgSobreRango() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().ogObjetivo(1.121).build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad inicial (OG) debe estar entre 1,020 y 1,120 y tener como máximo tres decimales (por ejemplo 1,050)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-21: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando la OG objetivo tiene cuatro decimales")
    void modificarReceta_debeRechazarOgConCuatroDecimales() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().ogObjetivo(1.0505).build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad inicial (OG) debe estar entre 1,020 y 1,120 y tener como máximo tres decimales (por ejemplo 1,050)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-22: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando la OG objetivo es nula")
    void modificarReceta_debeRechazarOgNula() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().ogObjetivo(null).build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad inicial (OG) debe estar entre 1,020 y 1,120 y tener como máximo tres decimales (por ejemplo 1,050)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-23: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando la FG objetivo es menor a 1,000 (límite inferior inválido)")
    void modificarReceta_debeRechazarFgBajoRango() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().fgObjetivo(0.999).build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad final (FG) debe estar entre 1,000 y 1,040 y tener como máximo tres decimales (por ejemplo 1,010)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-24: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando la FG objetivo es mayor a 1,040 (límite superior inválido)")
    void modificarReceta_debeRechazarFgSobreRango() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().fgObjetivo(1.041).build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad final (FG) debe estar entre 1,000 y 1,040 y tener como máximo tres decimales (por ejemplo 1,010)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-25: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando la FG objetivo tiene cuatro decimales")
    void modificarReceta_debeRechazarFgConCuatroDecimales() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().fgObjetivo(1.0105).build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad final (FG) debe estar entre 1,000 y 1,040 y tener como máximo tres decimales (por ejemplo 1,010)");

        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("CP-MR-26: modificarReceta lanza ReglaNegocioException y no consulta la BD cuando la FG objetivo es nula")
    void modificarReceta_debeRechazarFgNula() {
        RecetaFormDTO recetaFormDTO = recetaFormDTO(versionValidaBuilder().fgObjetivo(null).build());

        assertThatThrownBy(() -> recetaServicio.modificarReceta(1L, recetaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La densidad final (FG) debe estar entre 1,000 y 1,040 y tener como máximo tres decimales (por ejemplo 1,010)");

        verifyNoRepositoryInteractions();
    }

    // ==================== bajaReceta ====================

    @Test
    @DisplayName("CP-BR-01: bajaReceta lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaReceta_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(recetaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recetaServicio.bajaReceta(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la receta con ID: 99");

        verify(recetaRepository).findById(99L);
        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BR-02: bajaReceta marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe")
    void bajaReceta_debeMarcarBajaYRetornarRecetaExistente() {
        // === PREPARACION DE DATOS ===
        RecetaEntity recetaEntity = crearRecetaEntityConVersionActiva(1L, "IPA Clásica");
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaEntity));
        when(planificacionProduccionRepository.existsByVersionReceta_Receta_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(false);
        when(recetaRepository.save(recetaEntity)).thenReturn(recetaEntity);

        // === EJECUCION ===
        RecetaResponseDTO resultado = recetaServicio.bajaReceta(1L);

        // === ASSERTS ===
        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(recetaEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertThat(resultado.getId()).isEqualTo(1L);
        verify(recetaRepository).findById(1L);
        verify(planificacionProduccionRepository).existsByVersionReceta_Receta_IdAndEstado(1L, EstadoSolicitud.PENDIENTE);
        verify(recetaRepository).save(recetaEntity);
        verify(recetaRepository, never()).delete(any());
    }

    @Test
    @DisplayName("CP-BR-03: bajaReceta lanza ReglaNegocioException y no persiste cuando hay una planificación de producción PENDIENTE asociada")
    void bajaReceta_debeRechazarConOrdenDeProduccionPendienteAsociada() {
        // === PREPARACION DE DATOS ===
        RecetaEntity recetaEntity = crearRecetaEntityConVersionActiva(1L, "IPA Clásica");
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaEntity));
        when(planificacionProduccionRepository.existsByVersionReceta_Receta_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(true);

        // === EJECUCION Y ASSERTS ===
        assertThatThrownBy(() -> recetaServicio.bajaReceta(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la receta porque tiene una planificación de producción en estado Pendiente asociada");

        verify(recetaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BR-04: bajaReceta permite la baja cuando existen planificaciones de producción asociadas pero ninguna en estado PENDIENTE")
    void bajaReceta_debePermitirBajaConOrdenesDeProduccionEnOtroEstado() {
        // === PREPARACION DE DATOS ===
        RecetaEntity recetaEntity = crearRecetaEntityConVersionActiva(1L, "IPA Clásica");
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaEntity));
        when(planificacionProduccionRepository.existsByVersionReceta_Receta_IdAndEstado(1L, EstadoSolicitud.PENDIENTE)).thenReturn(false);
        when(recetaRepository.save(recetaEntity)).thenReturn(recetaEntity);

        // === EJECUCION ===
        RecetaResponseDTO resultado = recetaServicio.bajaReceta(1L);

        // === ASSERTS ===
        assertThat(resultado.getId()).isEqualTo(1L);
        verify(recetaRepository).save(recetaEntity);
    }

    // ==================== helpers de mockeo ====================

    private void verifyNoRepositoryInteractions() {
        verifyNoInteractions(recetaRepository, versionRecetaRepository, maltaRepository, lupuloRepository,
                levaduraRepository, planificacionProduccionRepository);
    }

    private void mockearInsumos(VersionRecetaFormDTO versionFormDTO) {
        versionFormDTO.getDetallesMalta().forEach(detalle ->
                when(maltaRepository.findById(detalle.getIdMalta())).thenReturn(Optional.of(maltaEntity(detalle.getIdMalta()))));
        versionFormDTO.getDetallesLupulo().forEach(detalle ->
                when(lupuloRepository.findById(detalle.getIdLupulo())).thenReturn(Optional.of(lupuloEntity(detalle.getIdLupulo()))));
        versionFormDTO.getDetallesLevadura().forEach(detalle ->
                when(levaduraRepository.findById(detalle.getIdLevadura())).thenReturn(Optional.of(levaduraEntity(detalle.getIdLevadura()))));
    }

    private void mockearAltaExitosa(VersionRecetaFormDTO versionFormDTO) {
        when(versionRecetaRepository.existsByNombreIgnoreCaseAndEsUltimaVersionTrue(versionFormDTO.getNombre())).thenReturn(false);
        mockearInsumos(versionFormDTO);
        when(recetaRepository.save(any(RecetaEntity.class))).thenAnswer(invocation -> {
            RecetaEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });
    }

    // ==================== helpers de construcción ====================

    private static VersionRecetaFormDTO.VersionRecetaFormDTOBuilder versionValidaBuilder() {
        return VersionRecetaFormDTO.builder()
                .nombre("IPA Clásica")
                .volumenBase(20.0)
                .relacionDeEmpaste(3.0)
                .ogObjetivo(1.050)
                .fgObjetivo(1.010)
                .ibuObjetivo(40)
                .duracionMaceracion(60)
                .duracionHervido(60)
                .duracionFermentacion(14)
                .duracionMaduracion(7)
                .detallesMalta(List.of(detalleMaltaFormDTO(2.0, MALTA_ID)))
                .detallesLupulo(List.of(detalleLupuloFormDTO(30.0, UsoLupulo.HERVOR, TipoEtapa.HERVIDO, 30.0, LUPULO_ID)))
                .detallesLevadura(List.of(detalleLevaduraFormDTO(11.0, LEVADURA_ID)));
    }

    private static RecetaFormDTO recetaFormDTO(VersionRecetaFormDTO version) {
        return RecetaFormDTO.builder().version(version).build();
    }

    private static DetalleMaltaFormDTO detalleMaltaFormDTO(Double cantidad, Long idMalta) {
        return DetalleMaltaFormDTO.builder().cantidad(cantidad).idMalta(idMalta).build();
    }

    private static DetalleLupuloFormDTO detalleLupuloFormDTO(Double cantidad, UsoLupulo uso, TipoEtapa etapaDeUso, Double tiempoDeHervor, Long idLupulo) {
        return DetalleLupuloFormDTO.builder()
                .cantidad(cantidad)
                .uso(uso)
                .etapaDeUso(etapaDeUso)
                .tiempoDeHervor(tiempoDeHervor)
                .idLupulo(idLupulo)
                .build();
    }

    private static DetalleLevaduraFormDTO detalleLevaduraFormDTO(Double cantidad, Long idLevadura) {
        return DetalleLevaduraFormDTO.builder().cantidad(cantidad).idLevadura(idLevadura).build();
    }

    private static MaltaEntity maltaEntity(Long id) {
        return MaltaEntity.builder().id(id).nombre("Malta Pilsen").build();
    }

    private static LupuloEntity lupuloEntity(Long id) {
        return LupuloEntity.builder().id(id).nombre("Cascade").build();
    }

    private static LevaduraEntity levaduraEntity(Long id) {
        return LevaduraEntity.builder().id(id).nombre("US-05").build();
    }

    private static VersionRecetaEntity crearVersionRecetaEntity(Long id, String nombre, boolean esUltimaVersion) {
        return VersionRecetaEntity.builder()
                .id(id)
                .nombre(nombre)
                .volumenBase(20.0)
                .relacionDeEmpaste(3.0)
                .ogObjetivo(1.050)
                .fgObjetivo(1.010)
                .ibuObjetivo(40)
                .duracionMaceracion(60)
                .duracionHervido(60)
                .duracionFermentacion(14)
                .duracionMaduracion(7)
                .esUltimaVersion(esUltimaVersion)
                .build();
    }

    private static RecetaEntity crearRecetaEntityConVersiones(Long id, VersionRecetaEntity... versiones) {
        RecetaEntity recetaEntity = RecetaEntity.builder().id(id).contadorLotes(1L).estado(Estado.ACTIVO).build();
        for (VersionRecetaEntity version : versiones) {
            version.setReceta(recetaEntity);
            recetaEntity.getVersiones().add(version);
        }
        return recetaEntity;
    }

    private static RecetaEntity crearRecetaEntityConVersionActiva(Long id, String nombre) {
        return crearRecetaEntityConVersiones(id, crearVersionRecetaEntity(id, nombre, true));
    }

}
