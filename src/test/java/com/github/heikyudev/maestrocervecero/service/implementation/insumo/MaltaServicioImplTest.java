package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo.ILoteInsumoRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IMaltaRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.orden_compra.IOrdenCompraRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.planificacion_produccion.IPlanificacionProduccionRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.ICatalogoProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.receta.IVersionRecetaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.MaltaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
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
class MaltaServicioImplTest {

    @Mock
    private IMaltaRepository maltaRepository;
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
    private MaltaServicioImpl maltaServicio;

    // ==================== filtrarMaltas ====================

    @Test
    @DisplayName("CP-FM-01: filtrarMaltas retorna una página de maltas correctamente mapeada a DTO cuando se filtra por nombre y tipo")
    void filtrarMaltas_debeRetornarPaginaMapeadaFiltrandoPorNombreYTipo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        MaltaEntity otraMaltaEntity = crearMaltaEntity(2L, "Pilsen Nacional", TipoMalta.BASE, 78);
        when(maltaRepository.filtrarMaltas("Pilsen", TipoMalta.BASE, pageable))
                .thenReturn(new PageImpl<>(List.of(maltaEntity, otraMaltaEntity), pageable, 2));

        // === EJECUCION ===
        Page<MaltaResponseDTO> resultado = maltaServicio.filtrarMaltas("Pilsen", TipoMalta.BASE, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertMaltaDTO(maltaEntity, resultado.getContent().get(0));
        assertMaltaDTO(otraMaltaEntity, resultado.getContent().get(1));
        verify(maltaRepository).filtrarMaltas("Pilsen", TipoMalta.BASE, pageable);
    }

    @Test
    @DisplayName("CP-FM-02: filtrarMaltas propaga nombre y tipo nulos sin restringir esos criterios")
    void filtrarMaltas_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        MaltaEntity otraMaltaEntity = crearMaltaEntity(2L, "Caramelo 60", TipoMalta.CARAMELO, 75);
        when(maltaRepository.filtrarMaltas(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(maltaEntity, otraMaltaEntity), pageable, 2));

        // === EJECUCION ===
        Page<MaltaResponseDTO> resultado = maltaServicio.filtrarMaltas(null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(maltaRepository).filtrarMaltas(null, null, pageable);
    }

    @Test
    @DisplayName("CP-FM-03: filtrarMaltas retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarMaltas_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(maltaRepository.filtrarMaltas("Inexistente", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<MaltaResponseDTO> resultado = maltaServicio.filtrarMaltas("Inexistente", null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(maltaRepository).filtrarMaltas("Inexistente", null, pageable);
    }

    @Test
    @DisplayName("CP-FM-04: filtrarMaltas normaliza el nombre buscado (espacios extremos e internos) antes de consultar")
    void filtrarMaltas_debeNormalizarElNombreBuscado() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(maltaRepository.filtrarMaltas("Pilsen Nacional", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        maltaServicio.filtrarMaltas("   Pilsen   Nacional  ", null, pageable);

        // === ASSERTS ===
        verify(maltaRepository).filtrarMaltas("Pilsen Nacional", null, pageable);
    }

    @Test
    @DisplayName("CP-FM-05: filtrarMaltas trata un nombre formado solo por espacios como ausencia de filtro")
    void filtrarMaltas_debeTratarNombreEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(maltaRepository.filtrarMaltas(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        maltaServicio.filtrarMaltas("    ", null, pageable);

        // === ASSERTS ===
        verify(maltaRepository).filtrarMaltas(null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("buscarPorId retorna el DTO de la malta cuando el ID existe")
    void buscarPorId_debeRetornarMaltaExistente() {
        // === PREPARACION DE DATOS ===
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));

        // === EJECUCION ===
        MaltaResponseDTO resultado = maltaServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertMaltaDTO(maltaEntity, resultado);
        verify(maltaRepository).findById(1L);
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dada de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para maltas dadas de baja
        when(maltaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maltaServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la malta con ID: 99");
        verify(maltaRepository).findById(99L);
    }

    // ==================== altaMalta ====================

    @Test
    @DisplayName("altaMalta lanza ReglaNegocioException y no consulta el repositorio cuando el extracto potencial es nulo")
    void altaMalta_debeRechazarPotencialExtractoNulo() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, null);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El extracto potencial es obligatorio");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("altaMalta lanza ReglaNegocioException cuando el extracto potencial es negativo")
    void altaMalta_debeRechazarPotencialExtractoNegativo() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, -1);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El extracto potencial debe ser mayor a 0");

        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("altaMalta lanza ReglaNegocioException cuando el extracto potencial es mayor a 90 (límite superior)")
    void altaMalta_debeRechazarPotencialExtractoMayorA90() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, 91);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El extracto potencial debe ser mayor a 0% y menor o igual a 90%");

        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("altaMalta lanza ReglaNegocioException cuando el extracto potencial es igual a cero (límite inferior)")
    void altaMalta_debeRechazarPotencialExtractoCero() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, 0);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El extracto potencial debe ser mayor a 0");

        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("altaMalta lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe")
    void altaMalta_debeRechazarNombreDuplicado() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Caramelo 60", TipoMalta.CARAMELO, 80);
        when(maltaRepository.existsByNombreIgnoreCase("Caramelo 60")).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una malta con el nombre 'Caramelo 60'");

        verify(maltaRepository).existsByNombreIgnoreCase("Caramelo 60");
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaMalta lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case (case-insensitive)")
    void altaMalta_debeRechazarNombreDuplicadoCaseInsensitive() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("caramelo 60", TipoMalta.CARAMELO, 80);
        when(maltaRepository.existsByNombreIgnoreCase("caramelo 60")).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una malta con el nombre 'caramelo 60'");

        verify(maltaRepository).existsByNombreIgnoreCase("caramelo 60");
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaMalta persiste y retorna el DTO asignando fijamente KILOGRAMO como unidad de medida (camino feliz)")
    void altaMalta_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Chocolate", TipoMalta.BASE, 80);
        when(maltaRepository.existsByNombreIgnoreCase("Chocolate")).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenAnswer(invocation -> {
            MaltaEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        MaltaResponseDTO resultado = maltaServicio.altaMalta(maltaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<MaltaEntity> captor = ArgumentCaptor.forClass(MaltaEntity.class);
        verify(maltaRepository).save(captor.capture());
        MaltaEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Chocolate");
        assertThat(entidadCapturada.getTipo()).isEqualTo(TipoMalta.BASE);
        assertThat(entidadCapturada.getPotencialExtracto()).isEqualTo(80);
        // La unidad de medida es fija por regla de negocio y la asigna el service, no el FormDTO
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        // El alta siempre debe registrar a la malta como ACTIVA, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Chocolate");
        assertThat(resultado.getTipo()).isEqualTo(TipoMalta.BASE);
        assertThat(resultado.getPotencialExtracto()).isEqualTo(80);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        verify(maltaRepository).existsByNombreIgnoreCase("Chocolate");
    }

    @Test
    @DisplayName("altaMalta persiste con éxito cuando el extracto potencial está en el límite inferior entero válido (1)")
    void altaMalta_debePersistirConPotencialExtractoEnLimiteInferiorValido() {
        // === PREPARACION DE DATOS ===
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Malta Roasted", TipoMalta.TOSTADA, 1);
        when(maltaRepository.existsByNombreIgnoreCase("Malta Roasted")).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenAnswer(invocation -> {
            MaltaEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(3L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        MaltaResponseDTO resultado = maltaServicio.altaMalta(maltaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<MaltaEntity> captor = ArgumentCaptor.forClass(MaltaEntity.class);
        verify(maltaRepository).save(captor.capture());
        MaltaEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getPotencialExtracto()).isEqualTo(1);
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);

        assertThat(resultado.getPotencialExtracto()).isEqualTo(1);
        assertThat(resultado.getNombre()).isEqualTo("Malta Roasted");
    }

    @Test
    @DisplayName("altaMalta persiste con éxito cuando el extracto potencial está en el límite superior válido (90)")
    void altaMalta_debePersistirConPotencialExtractoEnLimiteSuperiorValido() {
        // === PREPARACION DE DATOS ===
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Malta Pale Ale", TipoMalta.BASE, 90);
        when(maltaRepository.existsByNombreIgnoreCase("Malta Pale Ale")).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenAnswer(invocation -> {
            MaltaEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(4L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        MaltaResponseDTO resultado = maltaServicio.altaMalta(maltaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<MaltaEntity> captor = ArgumentCaptor.forClass(MaltaEntity.class);
        verify(maltaRepository).save(captor.capture());
        MaltaEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getPotencialExtracto()).isEqualTo(90);
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);

        assertThat(resultado.getPotencialExtracto()).isEqualTo(90);
        assertThat(resultado.getNombre()).isEqualTo("Malta Pale Ale");
    }

    // ==================== modificarMalta ====================

    @Test
    @DisplayName("modificarMalta lanza ReglaNegocioException y no consulta el repositorio cuando el extracto potencial es inválido")
    void modificarMalta_debeRechazarPotencialExtractoInvalido() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, -5);

        assertThatThrownBy(() -> maltaServicio.modificarMalta(1L, maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El extracto potencial debe ser mayor a 0");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("modificarMalta lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarMalta_debeLanzarExcepcionSiNoExiste() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, 80);
        when(maltaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maltaServicio.modificarMalta(99L, maltaFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la malta con ID: 99");

        verify(maltaRepository).findById(99L);
        // Al no existir la entidad, no debe llegarse a validar la duplicidad de nombre
        verify(maltaRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMalta lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otra malta")
    void modificarMalta_debeRechazarNombreEnUsoPorOtraMalta() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Chocolate", TipoMalta.BASE, 80);
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen", 1L)).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.modificarMalta(1L, maltaFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Pilsen' ya está en uso por otra malta");

        verify(maltaRepository).findById(1L);
        verify(maltaRepository).existsByNombreIgnoreCaseAndIdNot("Pilsen", 1L);
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMalta actualiza los datos, conserva KILOGRAMO y persiste cuando el ID existe y el nombre está libre (camino feliz)")
    void modificarMalta_debeActualizarMaltaExistente() {
        // === PREPARACION DE DATOS ===
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen Nacional", TipoMalta.BASE, 82);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(maltaRepository.save(maltaEntity)).thenReturn(maltaEntity);

        // === EJECUCION ===
        MaltaResponseDTO resultado = maltaServicio.modificarMalta(1L, maltaFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<MaltaEntity> captor = ArgumentCaptor.forClass(MaltaEntity.class);
        verify(maltaRepository).save(captor.capture());
        MaltaEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(entidadCapturada.getTipo()).isEqualTo(TipoMalta.BASE);
        assertThat(entidadCapturada.getPotencialExtracto()).isEqualTo(82);
        // La unidad de medida es fija por regla de negocio: no se toca durante la modificación
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);

        assertThat(resultado.getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(resultado.getTipo()).isEqualTo(TipoMalta.BASE);
        assertThat(resultado.getPotencialExtracto()).isEqualTo(82);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        verify(maltaRepository).findById(1L);
        verify(maltaRepository).existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L);
    }

    @Test
    @DisplayName("modificarMalta permite conservar el propio nombre actual al actualizar otros campos")
    void modificarMalta_debePermitirConservarNombrePropio() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        MaltaFormDTO maltaFormDTO = maltaFormDTO("pilsen", TipoMalta.BASE, 85);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("pilsen", 1L)).thenReturn(false);
        when(maltaRepository.save(maltaEntity)).thenReturn(maltaEntity);

        MaltaResponseDTO resultado = maltaServicio.modificarMalta(1L, maltaFormDTO);

        assertThat(resultado.getNombre()).isEqualTo("pilsen");
        assertThat(resultado.getPotencialExtracto()).isEqualTo(85);
        verify(maltaRepository).save(maltaEntity);
    }

    @Test
    @DisplayName("CP-MM-17: modificarMalta permite cambiar solo el nombre de la malta")
    void modificarMalta_debePermitirCambiarSoloElNombre() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Original", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("Nuevo Nombre", 1L)).thenReturn(false);
        when(maltaRepository.save(maltaEntity)).thenReturn(maltaEntity);
        MaltaFormDTO formDTO = maltaFormDTO("Nuevo Nombre", TipoMalta.BASE, 80);

        assertThatCode(() -> maltaServicio.modificarMalta(1L, formDTO)).doesNotThrowAnyException();

        assertThat(maltaEntity.getNombre()).isEqualTo("Nuevo Nombre");
        verify(maltaRepository).save(maltaEntity);
    }

    @Test
    @DisplayName("CP-MM-18: modificarMalta permite cambiar el extracto potencial aunque haya lotes que usan la malta (el escalado de un lote iniciado queda guardado en su instantánea)")
    void modificarMalta_debePermitirCambiarPotencialAunqueLotesUsenLaMalta() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen", 1L)).thenReturn(false);
        when(maltaRepository.save(maltaEntity)).thenReturn(maltaEntity);
        MaltaFormDTO formDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, 82);

        assertThatCode(() -> maltaServicio.modificarMalta(1L, formDTO)).doesNotThrowAnyException();

        assertThat(maltaEntity.getPotencialExtracto()).isEqualTo(82);
        verify(maltaRepository).save(maltaEntity);
    }

    // ==================== bajaMalta ====================

    @Test
    @DisplayName("CP-BM-01: bajaMalta lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaMalta_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(maltaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maltaServicio.bajaMalta(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la malta con ID: 99");

        verify(maltaRepository).findById(99L);
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-02: bajaMalta lanza ReglaNegocioException cuando la malta forma parte de una receta activa")
    void bajaMalta_debeRechazarSiFormaParteDeRecetaActiva() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(versionRecetaRepository.existsByDetalleMaltaEnRecetaActiva(1L)).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.bajaMalta(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la malta porque forma parte de al menos una receta activa");

        verifyNoInteractions(planificacionProduccionRepository, ordenCompraRepository, catalogoProveedorRepository, loteInsumoRepository);
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-05: bajaMalta lanza ReglaNegocioException y no persiste cuando la malta forma parte de una planificación de producción en estado Pendiente")
    void bajaMalta_debeRechazarSiFormaParteDePlanificacionPendiente() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(versionRecetaRepository.existsByDetalleMaltaEnRecetaActiva(1L)).thenReturn(false);
        when(planificacionProduccionRepository.existsByDetalleMaltaEnPlanificacionPendiente(1L)).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.bajaMalta(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la malta porque forma parte de una planificación de producción en estado Pendiente");

        verifyNoInteractions(ordenCompraRepository, catalogoProveedorRepository, loteInsumoRepository);
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-06: bajaMalta lanza ReglaNegocioException y no persiste cuando la malta se encuentra en una orden de compra en estado Pendiente")
    void bajaMalta_debeRechazarSiSeEncuentraEnOrdenCompraPendiente() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(versionRecetaRepository.existsByDetalleMaltaEnRecetaActiva(1L)).thenReturn(false);
        when(planificacionProduccionRepository.existsByDetalleMaltaEnPlanificacionPendiente(1L)).thenReturn(false);
        when(ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(1L)).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.bajaMalta(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la malta porque se encuentra en al menos una orden de compra en estado Pendiente");

        verifyNoInteractions(catalogoProveedorRepository, loteInsumoRepository);
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-07: bajaMalta lanza ReglaNegocioException y no persiste cuando la malta se encuentra en el catálogo de un proveedor activo")
    void bajaMalta_debeRechazarSiEstaEnCatalogoDeProveedorActivo() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(versionRecetaRepository.existsByDetalleMaltaEnRecetaActiva(1L)).thenReturn(false);
        when(planificacionProduccionRepository.existsByDetalleMaltaEnPlanificacionPendiente(1L)).thenReturn(false);
        when(ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(1L)).thenReturn(false);
        when(catalogoProveedorRepository.existsByInsumoId(1L)).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.bajaMalta(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la malta porque se encuentra asociada al catálogo de al menos un proveedor activo");

        verifyNoInteractions(loteInsumoRepository);
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-03: bajaMalta lanza ReglaNegocioException cuando la malta tiene stock")
    void bajaMalta_debeRechazarSiTieneStock() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(versionRecetaRepository.existsByDetalleMaltaEnRecetaActiva(1L)).thenReturn(false);
        when(planificacionProduccionRepository.existsByDetalleMaltaEnPlanificacionPendiente(1L)).thenReturn(false);
        when(ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(1L)).thenReturn(false);
        when(catalogoProveedorRepository.existsByInsumoId(1L)).thenReturn(false);
        when(loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(1L, 0.0)).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.bajaMalta(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la malta porque tiene stock");

        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BM-04: bajaMalta marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe y no tiene receta activa, planificación pendiente, orden de compra en estado Pendiente, catálogo de proveedor activo ni stock asociados")
    void bajaMalta_debeMarcarBajaYRetornarMaltaExistente() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(versionRecetaRepository.existsByDetalleMaltaEnRecetaActiva(1L)).thenReturn(false);
        when(planificacionProduccionRepository.existsByDetalleMaltaEnPlanificacionPendiente(1L)).thenReturn(false);
        when(ordenCompraRepository.existsByInsumoEnOrdenCompraPendiente(1L)).thenReturn(false);
        when(catalogoProveedorRepository.existsByInsumoId(1L)).thenReturn(false);
        when(loteInsumoRepository.existsByInsumo_IdAndCantidadActualGreaterThan(1L, 0.0)).thenReturn(false);
        when(maltaRepository.save(maltaEntity)).thenReturn(maltaEntity);

        MaltaResponseDTO resultado = maltaServicio.bajaMalta(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(maltaEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertMaltaDTO(maltaEntity, resultado);
        verify(maltaRepository).findById(1L);
        verify(maltaRepository).save(maltaEntity);
        verify(maltaRepository, never()).delete(any());
    }

    // ==================== validaciones técnicas de Malta (números finitos, techos y longitudes) ====================

    @Test
    @DisplayName("CP-AM-10: altaMalta lanza ReglaNegocioException y no consulta el repositorio cuando el nombre tiene 101 caracteres")
    void altaMalta_debeRechazar_nombreDe101Caracteres() {
        MaltaFormDTO dto = maltaFormDTO("A".repeat(101), TipoMalta.BASE, 80);

        assertThatThrownBy(() -> maltaServicio.altaMalta(dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre no puede superar 100 caracteres");

        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("CP-AM-11: altaMalta persiste con éxito cuando el nombre tiene exactamente 100 caracteres (valor límite válido)")
    void altaMalta_debeAceptar_nombreDe100Caracteres() {
        MaltaFormDTO dto = maltaFormDTO("A".repeat(100), TipoMalta.BASE, 80);
        when(maltaRepository.existsByNombreIgnoreCase("A".repeat(100))).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maltaServicio.altaMalta(dto)).doesNotThrowAnyException();

        verify(maltaRepository).save(any(MaltaEntity.class));
    }

    @Test
    @DisplayName("CP-MM-06: modificarMalta lanza ReglaNegocioException y no consulta la unicidad cuando el nombre tiene 101 caracteres")
    void modificarMalta_debeRechazar_nombreDe101Caracteres() {
        MaltaFormDTO dto = maltaFormDTO("A".repeat(101), TipoMalta.BASE, 80);

        assertThatThrownBy(() -> maltaServicio.modificarMalta(1L, dto))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre no puede superar 100 caracteres");

        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("CP-MM-07: modificarMalta persiste con éxito cuando el nombre tiene exactamente 100 caracteres (valor límite válido)")
    void modificarMalta_debeAceptar_nombreDe100Caracteres() {
        MaltaFormDTO dto = maltaFormDTO("A".repeat(100), TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80)));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("A".repeat(100), 1L)).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maltaServicio.modificarMalta(1L, dto)).doesNotThrowAnyException();

        verify(maltaRepository).save(any(MaltaEntity.class));
    }

    // ==================== helpers ====================

    private static MaltaEntity crearMaltaEntity(Long id, String nombre, TipoMalta tipo, Integer rendimiento) {
        return MaltaEntity.builder()
                .id(id)
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .tipo(tipo)
                .potencialExtracto(rendimiento)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static MaltaFormDTO maltaFormDTO(String nombre, TipoMalta tipo, Integer potencialExtracto) {
        return MaltaFormDTO.builder()
                .nombre(nombre)
                .tipo(tipo)
                .potencialExtracto(potencialExtracto)
                .build();
    }

    private static void assertMaltaDTO(MaltaEntity entidad, MaltaResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getUnidadDeMedida()).isEqualTo(entidad.getUnidadDeMedida());
        assertThat(dto.getTipo()).isEqualTo(entidad.getTipo());
        assertThat(dto.getPotencialExtracto()).isEqualTo(entidad.getPotencialExtracto());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }

    // ==================== normalización y unicidad ====================

    @Test
    @DisplayName("CP-AM-12: altaMalta guarda el nombre normalizado y consulta la unicidad con ese valor")
    void altaMalta_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        MaltaFormDTO formDTO = maltaFormDTO("  Pilsen   Nacional  ", TipoMalta.BASE, 80);
        when(maltaRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        maltaServicio.altaMalta(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<MaltaEntity> captor = ArgumentCaptor.forClass(MaltaEntity.class);
        verify(maltaRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(formDTO.getNombre()).isEqualTo("  Pilsen   Nacional  ");
        verify(maltaRepository).existsByNombreIgnoreCase("Pilsen Nacional");
        verify(maltaRepository).flush();
    }

    @Test
    @DisplayName("CP-AM-13: altaMalta lanza ReglaNegocioException y no consulta el repositorio cuando el nombre está vacío")
    void altaMalta_debeRechazarValorVacio() {
        MaltaFormDTO formDTO = maltaFormDTO("   ", TipoMalta.BASE, 80);

        assertThatThrownBy(() -> maltaServicio.altaMalta(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("CP-AM-14: altaMalta lanza RecursoDuplicadoException cuando el nombre normalizado ya existe")
    void altaMalta_debeDetectarDuplicadoConElValorNormalizado() {
        MaltaFormDTO formDTO = maltaFormDTO("  pilsen ", TipoMalta.BASE, 80);
        when(maltaRepository.existsByNombreIgnoreCase("pilsen")).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.altaMalta(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una malta con el nombre 'pilsen'");

        verify(maltaRepository).existsByNombreIgnoreCase("pilsen");
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AM-15: altaMalta traduce la violación del índice único udx_insumo_nombre al mensaje de duplicado")
    void altaMalta_debeTraducirLaViolacionDelIndiceUnico() {
        MaltaFormDTO formDTO = maltaFormDTO("Pilsen Nacional", TipoMalta.BASE, 80);
        when(maltaRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_insumo_nombre"));

        assertThatThrownBy(() -> maltaServicio.altaMalta(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una malta con el nombre 'Pilsen Nacional'");
    }

    @Test
    @DisplayName("CP-AM-16: altaMalta permite reutilizar el nombre de un registro dado de baja")
    void altaMalta_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        MaltaFormDTO formDTO = maltaFormDTO("Pilsen Nacional", TipoMalta.BASE, 80);
        // El único registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(maltaRepository.existsByNombreIgnoreCase("Pilsen Nacional")).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maltaServicio.altaMalta(formDTO)).doesNotThrowAnyException();

        verify(maltaRepository).save(any(MaltaEntity.class));
    }

    @Test
    @DisplayName("CP-AM-17: altaMalta permite un nombre que difiere de otro solo en una tilde")
    void altaMalta_debeTratarComoDistintosLosNombresConDistintaTilde() {
        MaltaFormDTO formDTO = maltaFormDTO("Pílsen", TipoMalta.BASE, 80);
        when(maltaRepository.existsByNombreIgnoreCase("Pílsen")).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> maltaServicio.altaMalta(formDTO)).doesNotThrowAnyException();

        verify(maltaRepository).existsByNombreIgnoreCase("Pílsen");
        verify(maltaRepository).save(any(MaltaEntity.class));
    }

    @Test
    @DisplayName("CP-MM-08: modificarMalta guarda el nombre normalizado y consulta la unicidad con ese valor")
    void modificarMalta_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        MaltaFormDTO formDTO = maltaFormDTO("  Pilsen   Nacional  ", TipoMalta.BASE, 80);
        MaltaEntity existente = crearMaltaEntity(1L, "Original", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(maltaRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        maltaServicio.modificarMalta(1L, formDTO);

        // === ASSERTS ===
        assertThat(existente.getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(formDTO.getNombre()).isEqualTo("  Pilsen   Nacional  ");
        verify(maltaRepository).existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L);
        verify(maltaRepository).save(existente);
        verify(maltaRepository).flush();
    }

    @Test
    @DisplayName("CP-MM-09: modificarMalta lanza ReglaNegocioException cuando el nombre está vacío")
    void modificarMalta_debeRechazarValorVacio() {
        MaltaFormDTO formDTO = maltaFormDTO("   ", TipoMalta.BASE, 80);

        assertThatThrownBy(() -> maltaServicio.modificarMalta(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("CP-MM-10: modificarMalta lanza RecursoDuplicadoException cuando el nombre normalizado está en uso por otro registro")
    void modificarMalta_debeDetectarDuplicadoConElValorNormalizado() {
        MaltaFormDTO formDTO = maltaFormDTO("  pilsen ", TipoMalta.BASE, 80);
        MaltaEntity existente = crearMaltaEntity(1L, "Original", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("pilsen", 1L)).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.modificarMalta(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'pilsen' ya está en uso por otra malta");

        verify(maltaRepository).existsByNombreIgnoreCaseAndIdNot("pilsen", 1L);
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MM-11: modificarMalta traduce la violación del índice único udx_insumo_nombre al mensaje de duplicado")
    void modificarMalta_debeTraducirLaViolacionDelIndiceUnico() {
        MaltaFormDTO formDTO = maltaFormDTO("Pilsen Nacional", TipoMalta.BASE, 80);
        MaltaEntity existente = crearMaltaEntity(1L, "Original", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(maltaRepository.save(existente)).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_insumo_nombre"));

        assertThatThrownBy(() -> maltaServicio.modificarMalta(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Pilsen Nacional' ya está en uso por otra malta");
    }

    @Test
    @DisplayName("CP-MM-12: modificarMalta permite reutilizar el nombre de un registro dado de baja")
    void modificarMalta_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        MaltaFormDTO formDTO = maltaFormDTO("Pilsen Nacional", TipoMalta.BASE, 80);
        MaltaEntity existente = crearMaltaEntity(1L, "Original", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(existente));
        // El único otro registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("Pilsen Nacional", 1L)).thenReturn(false);
        when(maltaRepository.save(existente)).thenReturn(existente);

        assertThatCode(() -> maltaServicio.modificarMalta(1L, formDTO)).doesNotThrowAnyException();

        assertThat(existente.getNombre()).isEqualTo("Pilsen Nacional");
        verify(maltaRepository).save(existente);
    }
}
