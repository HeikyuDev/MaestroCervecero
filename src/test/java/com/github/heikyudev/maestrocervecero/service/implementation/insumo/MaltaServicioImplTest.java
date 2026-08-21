package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.IMaltaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.MaltaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.MaltaResponseDTO;
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
class MaltaServicioImplTest {

    @Mock
    private IMaltaRepository maltaRepository;

    @InjectMocks
    private MaltaServicioImpl maltaServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("buscarTodos retorna una página de maltas correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        MaltaEntity otraMaltaEntity = crearMaltaEntity(2L, "Caramelo 60", TipoMalta.CARAMELO, 75);

        // Cuando maltaRepository.findAll(pageable) sea llamado, retorna una página con las maltas activas
        // (el filtrado por estado = ACTIVO ya está resuelto dentro de la consulta del repositorio)
        when(maltaRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(maltaEntity, otraMaltaEntity), pageable, 2));

        // === EJECUCION ===
        Page<MaltaResponseDTO> resultado = maltaServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertMaltaDTO(maltaEntity, resultado.getContent().get(0));
        assertMaltaDTO(otraMaltaEntity, resultado.getContent().get(1));
        verify(maltaRepository).findAll(pageable);
    }

    @Test
    @DisplayName("buscarTodos retorna una página vacía cuando no hay maltas registradas")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(maltaRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<MaltaResponseDTO> resultado = maltaServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(maltaRepository).findAll(pageable);
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
                .hasMessage("La malta no existe");
        verify(maltaRepository).findById(99L);
    }

    // ==================== altaMalta ====================

    @Test
    @DisplayName("altaMalta lanza ReglaNegocioException y no consulta el repositorio cuando el rendimiento es nulo")
    void altaMalta_debeRechazarRendimientoNulo() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, null);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento debe estar entre 0 y 100 inclusive");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("altaMalta lanza ReglaNegocioException cuando el rendimiento es negativo (límite inferior)")
    void altaMalta_debeRechazarRendimientoNegativo() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, -1);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento debe estar entre 0 y 100 inclusive");

        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("altaMalta lanza ReglaNegocioException cuando el rendimiento es mayor a 100 (límite superior)")
    void altaMalta_debeRechazarRendimientoMayorA100() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, 101);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento debe estar entre 0 y 100 inclusive");

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
        assertThat(entidadCapturada.getRendimiento()).isEqualTo(80);
        // La unidad de medida es fija por regla de negocio y la asigna el service, no el FormDTO
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        // El alta siempre debe registrar a la malta como ACTIVA, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Chocolate");
        assertThat(resultado.getTipo()).isEqualTo(TipoMalta.BASE);
        assertThat(resultado.getRendimiento()).isEqualTo(80);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        verify(maltaRepository).existsByNombreIgnoreCase("Chocolate");
    }

    @Test
    @DisplayName("altaMalta persiste con éxito cuando el rendimiento está en el límite inferior entero válido")
    void altaMalta_debePersistirConRendimientoEnLimiteInferiorValido() {
        // === PREPARACION DE DATOS ===
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Malta Roasted", TipoMalta.TOSTADA, 0);
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
        assertThat(entidadCapturada.getRendimiento()).isEqualTo(0);
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);

        assertThat(resultado.getRendimiento()).isEqualTo(0);
        assertThat(resultado.getNombre()).isEqualTo("Malta Roasted");
    }

    @Test
    @DisplayName("altaMalta persiste con éxito cuando el rendimiento está en el límite superior entero válido")
    void altaMalta_debePersistirConRendimientoEnLimiteSuperiorValido() {
        // === PREPARACION DE DATOS ===
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Malta Pale Ale", TipoMalta.BASE, 100);
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
        assertThat(entidadCapturada.getRendimiento()).isEqualTo(100);
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);

        assertThat(resultado.getRendimiento()).isEqualTo(100);
        assertThat(resultado.getNombre()).isEqualTo("Malta Pale Ale");
    }

    // ==================== modificarMalta ====================

    @Test
    @DisplayName("modificarMalta lanza ReglaNegocioException y no consulta el repositorio cuando el rendimiento es inválido")
    void modificarMalta_debeRechazarRendimientoInvalido() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pilsen", TipoMalta.BASE, -5);

        assertThatThrownBy(() -> maltaServicio.modificarMalta(1L, maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El rendimiento debe estar entre 0 y 100 inclusive");

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
                .hasMessage("La malta no existe");

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
        assertThat(entidadCapturada.getRendimiento()).isEqualTo(82);
        // La unidad de medida es fija por regla de negocio: no se toca durante la modificación
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);

        assertThat(resultado.getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(resultado.getTipo()).isEqualTo(TipoMalta.BASE);
        assertThat(resultado.getRendimiento()).isEqualTo(82);
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
        assertThat(resultado.getRendimiento()).isEqualTo(85);
        verify(maltaRepository).save(maltaEntity);
    }

    // ==================== bajaMalta ====================

    @Test
    @DisplayName("bajaMalta lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaMalta_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(maltaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maltaServicio.bajaMalta(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la malta con ID: 99");

        verify(maltaRepository).findById(99L);
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("bajaMalta marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe")
    void bajaMalta_debeMarcarBajaYRetornarMaltaExistente() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pilsen", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(maltaRepository.save(maltaEntity)).thenReturn(maltaEntity);

        MaltaResponseDTO resultado = maltaServicio.bajaMalta(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(maltaEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertMaltaDTO(maltaEntity, resultado);
        verify(maltaRepository).findById(1L);
        verify(maltaRepository).save(maltaEntity);
        verify(maltaRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static MaltaEntity crearMaltaEntity(Long id, String nombre, TipoMalta tipo, Integer rendimiento) {
        return MaltaEntity.builder()
                .id(id)
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .tipo(tipo)
                .rendimiento(rendimiento)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static MaltaFormDTO maltaFormDTO(String nombre, TipoMalta tipo, Integer rendimiento) {
        return MaltaFormDTO.builder()
                .nombre(nombre)
                .tipo(tipo)
                .rendimiento(rendimiento)
                .build();
    }

    private static void assertMaltaDTO(MaltaEntity entidad, MaltaResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getUnidadDeMedida()).isEqualTo(entidad.getUnidadDeMedida());
        assertThat(dto.getTipo()).isEqualTo(entidad.getTipo());
        assertThat(dto.getRendimiento()).isEqualTo(entidad.getRendimiento());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
