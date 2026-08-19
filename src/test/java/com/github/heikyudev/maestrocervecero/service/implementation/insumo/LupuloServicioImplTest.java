package com.github.heikyudev.maestrocervecero.service.implementation.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILupuloRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.insumo.LupuloFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.LupuloResponseDTO;
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
class LupuloServicioImplTest {

    @Mock
    private ILupuloRepository lupuloRepository;

    @InjectMocks
    private LupuloServicioImpl lupuloServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("buscarTodos retorna una página de lúpulos correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LupuloEntity lupuloEntity = crearLupuloEntity(1L, "Cascade", FormatoLupulo.PELLET, 6);
        LupuloEntity otroLupuloEntity = crearLupuloEntity(2L, "Saaz", FormatoLupulo.FLOR, 3);

        // Cuando lupuloRepository.findAll(pageable) sea llamado, retorna una página con los lúpulos activos
        // (el filtrado de soft-deleted es automático por @SoftDelete de Hibernate sobre la entidad)
        when(lupuloRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(lupuloEntity, otroLupuloEntity), pageable, 2));

        // === EJECUCION ===
        Page<LupuloResponseDTO> resultado = lupuloServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertLupuloDTO(lupuloEntity, resultado.getContent().get(0));
        assertLupuloDTO(otroLupuloEntity, resultado.getContent().get(1));
        verify(lupuloRepository).findAll(pageable);
    }

    @Test
    @DisplayName("buscarTodos retorna una página vacía cuando no hay lúpulos registrados")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(lupuloRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<LupuloResponseDTO> resultado = lupuloServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(lupuloRepository).findAll(pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("buscarPorId retorna el DTO del lúpulo cuando el ID existe")
    void buscarPorId_debeRetornarLupuloExistente() {
        // === PREPARACION DE DATOS ===
        LupuloEntity lupuloEntity = crearLupuloEntity(1L, "Cascade", FormatoLupulo.PELLET, 6);
        when(lupuloRepository.findById(1L)).thenReturn(Optional.of(lupuloEntity));

        // === EJECUCION ===
        LupuloResponseDTO resultado = lupuloServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertLupuloDTO(lupuloEntity, resultado);
        verify(lupuloRepository).findById(1L);
    }

    @Test
    @DisplayName("buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está soft-deleted")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // @SoftDelete hace que findById devuelva Optional.empty() también para registros eliminados lógicamente
        when(lupuloRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lupuloServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El lúpulo no existe");
        verify(lupuloRepository).findById(99L);
    }

    // ==================== altaLupulo ====================

    @Test
    @DisplayName("altaLupulo lanza ReglaNegocioException y no consulta el repositorio cuando el porcentaje de alfa ácidos es nulo")
    void altaLupulo_debeRechazarAlfaAcidosNulo() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, null);

        assertThatThrownBy(() -> lupuloServicio.altaLupulo(lupuloFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El porcentaje de alfa ácidos debe ser mayor a 0");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(lupuloRepository);
    }

    @Test
    @DisplayName("altaLupulo lanza ReglaNegocioException cuando el porcentaje de alfa ácidos es igual a cero (valor límite)")
    void altaLupulo_debeRechazarAlfaAcidosCero() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, 0);

        assertThatThrownBy(() -> lupuloServicio.altaLupulo(lupuloFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El porcentaje de alfa ácidos debe ser mayor a 0");

        verifyNoInteractions(lupuloRepository);
    }

    @Test
    @DisplayName("altaLupulo lanza ReglaNegocioException cuando el porcentaje de alfa ácidos es negativo")
    void altaLupulo_debeRechazarAlfaAcidosNegativo() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, -1);

        assertThatThrownBy(() -> lupuloServicio.altaLupulo(lupuloFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El porcentaje de alfa ácidos debe ser mayor a 0");

        verifyNoInteractions(lupuloRepository);
    }

    @Test
    @DisplayName("altaLupulo lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe")
    void altaLupulo_debeRechazarNombreDuplicado() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Citra", FormatoLupulo.PELLET, 6);
        when(lupuloRepository.existsByNombreIgnoreCase("Citra")).thenReturn(true);

        assertThatThrownBy(() -> lupuloServicio.altaLupulo(lupuloFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un lúpulo con el nombre 'Citra'");

        verify(lupuloRepository).existsByNombreIgnoreCase("Citra");
        verify(lupuloRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaLupulo lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case (case-insensitive)")
    void altaLupulo_debeRechazarNombreDuplicadoCaseInsensitive() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("citra", FormatoLupulo.PELLET, 6);
        when(lupuloRepository.existsByNombreIgnoreCase("citra")).thenReturn(true);

        assertThatThrownBy(() -> lupuloServicio.altaLupulo(lupuloFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un lúpulo con el nombre 'citra'");

        verify(lupuloRepository).existsByNombreIgnoreCase("citra");
        verify(lupuloRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaLupulo persiste y retorna el DTO asignando fijamente GRAMO como unidad de medida (camino feliz)")
    void altaLupulo_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Mosaic", FormatoLupulo.PELLET, 12);
        when(lupuloRepository.existsByNombreIgnoreCase("Mosaic")).thenReturn(false);
        when(lupuloRepository.save(any(LupuloEntity.class))).thenAnswer(invocation -> {
            LupuloEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        LupuloResponseDTO resultado = lupuloServicio.altaLupulo(lupuloFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<LupuloEntity> captor = ArgumentCaptor.forClass(LupuloEntity.class);
        verify(lupuloRepository).save(captor.capture());
        LupuloEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Mosaic");
        assertThat(entidadCapturada.getFormato()).isEqualTo(FormatoLupulo.PELLET);
        assertThat(entidadCapturada.getAa()).isEqualTo(12);
        // La unidad de medida es fija por regla de negocio y la asigna el service, no el FormDTO
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Mosaic");
        assertThat(resultado.getFormato()).isEqualTo(FormatoLupulo.PELLET);
        assertThat(resultado.getAa()).isEqualTo(12);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);
        verify(lupuloRepository).existsByNombreIgnoreCase("Mosaic");
    }

    @Test
    @DisplayName("altaLupulo persiste con éxito cuando el porcentaje de alfa ácidos está en el límite inferior entero válido")
    void altaLupulo_debePersistirConAlfaAcidosEnLimiteInferiorValido() {
        // === PREPARACION DE DATOS ===
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Saaz", FormatoLupulo.FLOR, 1);
        when(lupuloRepository.existsByNombreIgnoreCase("Saaz")).thenReturn(false);
        when(lupuloRepository.save(any(LupuloEntity.class))).thenAnswer(invocation -> {
            LupuloEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(3L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        LupuloResponseDTO resultado = lupuloServicio.altaLupulo(lupuloFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<LupuloEntity> captor = ArgumentCaptor.forClass(LupuloEntity.class);
        verify(lupuloRepository).save(captor.capture());
        LupuloEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getAa()).isEqualTo(1);
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);

        assertThat(resultado.getAa()).isEqualTo(1);
        assertThat(resultado.getNombre()).isEqualTo("Saaz");
    }

    // ==================== modificarLupulo ====================

    @Test
    @DisplayName("modificarLupulo lanza ReglaNegocioException y no consulta el repositorio cuando el porcentaje de alfa ácidos es inválido")
    void modificarLupulo_debeRechazarAlfaAcidosInvalido() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, 0);

        assertThatThrownBy(() -> lupuloServicio.modificarLupulo(1L, lupuloFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El porcentaje de alfa ácidos debe ser mayor a 0");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(lupuloRepository);
    }

    @Test
    @DisplayName("modificarLupulo lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarLupulo_debeLanzarExcepcionSiNoExiste() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, 6);
        when(lupuloRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lupuloServicio.modificarLupulo(99L, lupuloFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El lúpulo no existe");

        verify(lupuloRepository).findById(99L);
        // Al no existir la entidad, no debe llegarse a validar la duplicidad de nombre
        verify(lupuloRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(lupuloRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarLupulo lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otro lúpulo")
    void modificarLupulo_debeRechazarNombreEnUsoPorOtroLupulo() {
        LupuloEntity lupuloEntity = crearLupuloEntity(1L, "Cascade", FormatoLupulo.PELLET, 6);
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Citra", FormatoLupulo.PELLET, 6);
        when(lupuloRepository.findById(1L)).thenReturn(Optional.of(lupuloEntity));
        when(lupuloRepository.existsByNombreIgnoreCaseAndIdNot("Citra", 1L)).thenReturn(true);

        assertThatThrownBy(() -> lupuloServicio.modificarLupulo(1L, lupuloFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Citra' ya está en uso por otro lúpulo");

        verify(lupuloRepository).findById(1L);
        verify(lupuloRepository).existsByNombreIgnoreCaseAndIdNot("Citra", 1L);
        verify(lupuloRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarLupulo actualiza los datos, conserva GRAMO y persiste cuando el ID existe y el nombre está libre (camino feliz)")
    void modificarLupulo_debeActualizarLupuloExistente() {
        // === PREPARACION DE DATOS ===
        LupuloEntity lupuloEntity = crearLupuloEntity(1L, "Cascade", FormatoLupulo.PELLET, 6);
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade Modificado", FormatoLupulo.FLOR, 8);
        when(lupuloRepository.findById(1L)).thenReturn(Optional.of(lupuloEntity));
        when(lupuloRepository.existsByNombreIgnoreCaseAndIdNot("Cascade Modificado", 1L)).thenReturn(false);
        when(lupuloRepository.save(lupuloEntity)).thenReturn(lupuloEntity);

        // === EJECUCION ===
        LupuloResponseDTO resultado = lupuloServicio.modificarLupulo(1L, lupuloFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<LupuloEntity> captor = ArgumentCaptor.forClass(LupuloEntity.class);
        verify(lupuloRepository).save(captor.capture());
        LupuloEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Cascade Modificado");
        assertThat(entidadCapturada.getFormato()).isEqualTo(FormatoLupulo.FLOR);
        assertThat(entidadCapturada.getAa()).isEqualTo(8);
        // La unidad de medida es fija por regla de negocio: no se toca durante la modificación
        assertThat(entidadCapturada.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);

        assertThat(resultado.getNombre()).isEqualTo("Cascade Modificado");
        assertThat(resultado.getFormato()).isEqualTo(FormatoLupulo.FLOR);
        assertThat(resultado.getAa()).isEqualTo(8);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);
        verify(lupuloRepository).findById(1L);
        verify(lupuloRepository).existsByNombreIgnoreCaseAndIdNot("Cascade Modificado", 1L);
    }

    @Test
    @DisplayName("modificarLupulo permite conservar el propio nombre actual al actualizar otros campos")
    void modificarLupulo_debePermitirConservarNombrePropio() {
        LupuloEntity lupuloEntity = crearLupuloEntity(1L, "Cascade", FormatoLupulo.PELLET, 6);
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("cascade", FormatoLupulo.PELLET, 9);
        when(lupuloRepository.findById(1L)).thenReturn(Optional.of(lupuloEntity));
        when(lupuloRepository.existsByNombreIgnoreCaseAndIdNot("cascade", 1L)).thenReturn(false);
        when(lupuloRepository.save(lupuloEntity)).thenReturn(lupuloEntity);

        LupuloResponseDTO resultado = lupuloServicio.modificarLupulo(1L, lupuloFormDTO);

        assertThat(resultado.getNombre()).isEqualTo("cascade");
        assertThat(resultado.getAa()).isEqualTo(9);
        verify(lupuloRepository).save(lupuloEntity);
    }

    // ==================== bajaLupulo ====================

    @Test
    @DisplayName("bajaLupulo lanza RecursoNoEncontradoException y no elimina cuando el ID no existe")
    void bajaLupulo_debeLanzarExcepcionYNoEliminarSiNoExiste() {
        when(lupuloRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lupuloServicio.bajaLupulo(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lúpulo con ID: 99");

        verify(lupuloRepository).findById(99L);
        verify(lupuloRepository, never()).delete(any());
    }

    @Test
    @DisplayName("bajaLupulo elimina lógicamente (soft-delete) y retorna el DTO cuando el ID existe")
    void bajaLupulo_debeEliminarYRetornarLupuloExistente() {
        LupuloEntity lupuloEntity = crearLupuloEntity(1L, "Cascade", FormatoLupulo.PELLET, 6);
        when(lupuloRepository.findById(1L)).thenReturn(Optional.of(lupuloEntity));

        LupuloResponseDTO resultado = lupuloServicio.bajaLupulo(1L);

        // El borrado físico a nivel repositorio es convertido a UPDATE por el @SoftDelete de Hibernate
        assertLupuloDTO(lupuloEntity, resultado);
        verify(lupuloRepository).findById(1L);
        verify(lupuloRepository).delete(lupuloEntity);
    }

    // ==================== helpers ====================

    private static LupuloEntity crearLupuloEntity(Long id, String nombre, FormatoLupulo formato, Integer aa) {
        return LupuloEntity.builder()
                .id(id)
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.GRAMO)
                .formato(formato)
                .aa(aa)
                .build();
    }

    private static LupuloFormDTO lupuloFormDTO(String nombre, FormatoLupulo formato, Integer aa) {
        return LupuloFormDTO.builder()
                .nombre(nombre)
                .formato(formato)
                .aa(aa)
                .build();
    }

    private static void assertLupuloDTO(LupuloEntity entidad, LupuloResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getUnidadDeMedida()).isEqualTo(entidad.getUnidadDeMedida());
        assertThat(dto.getFormato()).isEqualTo(entidad.getFormato());
        assertThat(dto.getAa()).isEqualTo(entidad.getAa());
    }
}
