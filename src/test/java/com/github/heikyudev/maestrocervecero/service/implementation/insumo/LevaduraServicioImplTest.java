package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILevaduraRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.LevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LevaduraResponseDTO;
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
class LevaduraServicioImplTest {

    @Mock
    private ILevaduraRepository levaduraRepository;

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
                .hasMessage("La levadura no existe");
        verify(levaduraRepository).findById(99L);
    }

    // ==================== altaLevadura ====================

    @Test
    @DisplayName("altaLevadura lanza ReglaNegocioException y no consulta el repositorio cuando la cantidad de células por gramo es nula")
    void altaLevadura_debeRechazarCantidadCelulasNula() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, null);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células por gramo debe ser mayor a 0");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("altaLevadura lanza ReglaNegocioException cuando la cantidad de células por gramo es igual a cero (valor límite)")
    void altaLevadura_debeRechazarCantidadCelulasCero() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, 0.0);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células por gramo debe ser mayor a 0");

        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("altaLevadura lanza ReglaNegocioException cuando la cantidad de células por gramo es negativa")
    void altaLevadura_debeRechazarCantidadCelulasNegativa() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, -1.0);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células por gramo debe ser mayor a 0");

        verifyNoInteractions(levaduraRepository);
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
    @DisplayName("altaLevadura persiste con éxito cuando la cantidad de células por gramo está en el límite inferior válido")
    void altaLevadura_debePersistirConCantidadCelulasEnLimiteInferiorValido() {
        // === PREPARACION DE DATOS ===
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("Belle Saison", TipoLevadura.ALE, 0.1);
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
        assertThat(entidadCapturada.getCantidadCelulasPorGramo()).isEqualTo(0.1);
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);

        assertThat(resultado.getCantidadCelulasPorGramo()).isEqualTo(0.1);
        assertThat(resultado.getNombre()).isEqualTo("Belle Saison");
    }

    // ==================== modificarLevadura ====================

    @Test
    @DisplayName("modificarLevadura lanza ReglaNegocioException y no consulta el repositorio cuando la cantidad de células por gramo es inválida")
    void modificarLevadura_debeRechazarCantidadCelulasInvalida() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle S-04", TipoLevadura.ALE, 0.0);

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(1L, levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La cantidad de células por gramo debe ser mayor a 0");

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
                .hasMessage("La levadura no existe");

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
                .hasMessage("El nombre 'SafAle US-05' ya está en uso por otra levadura");

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

    // ==================== bajaLevadura ====================

    @Test
    @DisplayName("bajaLevadura lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaLevadura_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(levaduraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> levaduraServicio.bajaLevadura(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la levadura con ID: 99");

        verify(levaduraRepository).findById(99L);
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("bajaLevadura marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe")
    void bajaLevadura_debeMarcarBajaYRetornarLevaduraExistente() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle S-04", TipoLevadura.ALE, 1.0E10);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(levaduraRepository.save(levaduraEntity)).thenReturn(levaduraEntity);

        LevaduraResponseDTO resultado = levaduraServicio.bajaLevadura(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(levaduraEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertLevaduraDTO(levaduraEntity, resultado);
        verify(levaduraRepository).findById(1L);
        verify(levaduraRepository).save(levaduraEntity);
        verify(levaduraRepository, never()).delete(any());
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
}
