package com.github.heikyudev.maestrocervecero.service.implementation;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.FormatoLupulo;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILupuloRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.LupuloFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.LupuloResponseDTO;
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

    @Test
    @DisplayName("findAll retorna una página de lúpulos correctamente mapeada a DTO")
    void findAll_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LupuloEntity lupuloEntity = crearLupuloEntity(1L, "Cascade", FormatoLupulo.PELLET, 6);

        // Cuando lupuloRepository.findAll(pageable) sea llamado, retorna una página con el lúpulo activo
        // (el filtrado de soft-deleted es automático por @SoftDelete de Hibernate sobre la entidad)
        when(lupuloRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(lupuloEntity), pageable, 1));

        // === EJECUCION ===
        Page<LupuloResponseDTO> resultado = lupuloServicio.findAll(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertLupuloDTO(lupuloEntity, resultado.getContent().get(0));
        verify(lupuloRepository).findAll(pageable);
    }

    @Test
    @DisplayName("obtenerPorId retorna el DTO del lúpulo cuando el ID existe")
    void obtenerPorId_debeRetornarLupuloExistente() {
        // === PREPARACION DE DATOS ===
        LupuloEntity lupuloEntity = crearLupuloEntity(1L, "Cascade", FormatoLupulo.FLOR, 6);
        when(lupuloRepository.findById(1L)).thenReturn(Optional.of(lupuloEntity));

        // === EJECUCION ===
        LupuloResponseDTO resultado = lupuloServicio.obtenerPorId(1L);

        // === VERIFICACION ===
        assertLupuloDTO(lupuloEntity, resultado);
        verify(lupuloRepository).findById(1L);
    }

    @Test
    @DisplayName("obtenerPorId lanza RecursoNoEncontradoException cuando el ID no existe o está soft-deleted")
    void obtenerPorId_debeLanzarExcepcionSiNoExiste() {
        // @SoftDelete hace que findById devuelva Optional.empty() también para registros eliminados lógicamente
        when(lupuloRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lupuloServicio.obtenerPorId(3982L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El lúpulo no existe");
        verify(lupuloRepository).findById(3982L);
    }

    @Test
    @DisplayName("altaLupulo asigna GRAMO como unidad de medida y persiste cuando los datos son válidos")
    void altaLupulo_debePersistirConUnidadGramo() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, 6);
        when(lupuloRepository.existsByNombreIgnoreCase("Cascade")).thenReturn(false);
        when(lupuloRepository.save(any(LupuloEntity.class))).thenAnswer(invocation -> {
            LupuloEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        LupuloResponseDTO resultado = lupuloServicio.altaLupulo(lupuloFormDTO);

        ArgumentCaptor<LupuloEntity> captor = ArgumentCaptor.forClass(LupuloEntity.class);
        verify(lupuloRepository).save(captor.capture());
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Cascade");
        assertThat(resultado.getFormato()).isEqualTo(FormatoLupulo.PELLET);
        assertThat(resultado.getAa()).isEqualTo(6);
        // La unidad de medida es fija por negocio y la asigna el service, nunca viene del formulario
        assertThat(captor.getValue().getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);
        verify(lupuloRepository).existsByNombreIgnoreCase("Cascade");
    }

    @Test
    @DisplayName("altaLupulo acepta aa 1 como borde válido de la regla positivo (> 0)")
    void altaLupulo_debeAceptarAaPositivoMinimo() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Saaz", FormatoLupulo.FLOR, 1);
        when(lupuloRepository.existsByNombreIgnoreCase("Saaz")).thenReturn(false);
        when(lupuloRepository.save(any(LupuloEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LupuloResponseDTO resultado = lupuloServicio.altaLupulo(lupuloFormDTO);

        assertThat(resultado.getAa()).isEqualTo(1);
        verify(lupuloRepository).save(any(LupuloEntity.class));
    }

    @Test
    @DisplayName("altaLupulo lanza ReglaNegocioException y no persiste ni consulta unicidad cuando el aa es nulo")
    void altaLupulo_debeRechazarAaNulo() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, null);

        assertThatThrownBy(() -> lupuloServicio.altaLupulo(lupuloFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // La validación de negocio se ejecuta antes que la de unicidad y antes de persistir
        verify(lupuloRepository, never()).existsByNombreIgnoreCase(any());
        verify(lupuloRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaLupulo lanza ReglaNegocioException y no persiste cuando el aa es cero")
    void altaLupulo_debeRechazarAaCero() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, 0);

        assertThatThrownBy(() -> lupuloServicio.altaLupulo(lupuloFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(lupuloRepository, never()).existsByNombreIgnoreCase(any());
        verify(lupuloRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaLupulo lanza ReglaNegocioException y no persiste cuando el aa es negativo")
    void altaLupulo_debeRechazarAaNegativo() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, -1);

        assertThatThrownBy(() -> lupuloServicio.altaLupulo(lupuloFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(lupuloRepository, never()).existsByNombreIgnoreCase(any());
        verify(lupuloRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaLupulo lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe (case-insensitive)")
    void altaLupulo_debeRechazarNombreDuplicado() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("cascade", FormatoLupulo.PELLET, 6);
        when(lupuloRepository.existsByNombreIgnoreCase("cascade")).thenReturn(true);

        assertThatThrownBy(() -> lupuloServicio.altaLupulo(lupuloFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(lupuloRepository).existsByNombreIgnoreCase("cascade");
        verify(lupuloRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarLupulo actualiza los datos cuando el ID existe y el nombre está libre")
    void modificarLupulo_debeActualizarLupuloExistente() {
        LupuloEntity lupuloEntity = crearLupuloEntity(1L, "Viejo", FormatoLupulo.FLOR, 5);
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, 6);
        when(lupuloRepository.findById(1L)).thenReturn(Optional.of(lupuloEntity));
        when(lupuloRepository.existsByNombreIgnoreCaseAndIdNot("Cascade", 1L)).thenReturn(false);
        when(lupuloRepository.save(lupuloEntity)).thenReturn(lupuloEntity);

        LupuloResponseDTO resultado = lupuloServicio.modificarLupulo(1L, lupuloFormDTO);

        assertThat(resultado.getNombre()).isEqualTo("Cascade");
        assertThat(resultado.getFormato()).isEqualTo(FormatoLupulo.PELLET);
        assertThat(resultado.getAa()).isEqualTo(6);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);
        verify(lupuloRepository).findById(1L);
        verify(lupuloRepository).existsByNombreIgnoreCaseAndIdNot("Cascade", 1L);
        verify(lupuloRepository).save(lupuloEntity);
    }

    @Test
    @DisplayName("modificarLupulo lanza ReglaNegocioException sin buscar ni persistir cuando el aa es inválido")
    void modificarLupulo_debeRechazarAaInvalido() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, 0);

        assertThatThrownBy(() -> lupuloServicio.modificarLupulo(1L, lupuloFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // La regla de negocio se valida antes de consultar la base de datos
        verifyNoInteractions(lupuloRepository);
    }

    @Test
    @DisplayName("modificarLupulo lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarLupulo_debeLanzarExcepcionSiNoExiste() {
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("Cascade", FormatoLupulo.PELLET, 6);
        when(lupuloRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lupuloServicio.modificarLupulo(3982L, lupuloFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El lúpulo no existe");

        verify(lupuloRepository).findById(3982L);
        verify(lupuloRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(lupuloRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarLupulo lanza RecursoDuplicadoException y no persiste cuando el nombre pertenece a otro lúpulo")
    void modificarLupulo_debeRechazarNombreDuplicadoEnOtroLupulo() {
        LupuloEntity lupuloEntity = crearLupuloEntity(1L, "Viejo", FormatoLupulo.FLOR, 5);
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("cascade", FormatoLupulo.PELLET, 6);
        when(lupuloRepository.findById(1L)).thenReturn(Optional.of(lupuloEntity));
        when(lupuloRepository.existsByNombreIgnoreCaseAndIdNot("cascade", 1L)).thenReturn(true);

        assertThatThrownBy(() -> lupuloServicio.modificarLupulo(1L, lupuloFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(lupuloRepository).existsByNombreIgnoreCaseAndIdNot("cascade", 1L);
        verify(lupuloRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarLupulo permite conservar el propio nombre actualizando solo otros campos")
    void modificarLupulo_debePermitirConservarNombrePropio() {
        LupuloEntity lupuloEntity = crearLupuloEntity(1L, "Cascade", FormatoLupulo.FLOR, 5);
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        LupuloFormDTO lupuloFormDTO = lupuloFormDTO("CASCADE", FormatoLupulo.PELLET, 7);
        when(lupuloRepository.findById(1L)).thenReturn(Optional.of(lupuloEntity));
        when(lupuloRepository.existsByNombreIgnoreCaseAndIdNot("CASCADE", 1L)).thenReturn(false);
        when(lupuloRepository.save(lupuloEntity)).thenReturn(lupuloEntity);

        LupuloResponseDTO resultado = lupuloServicio.modificarLupulo(1L, lupuloFormDTO);

        assertThat(resultado.getFormato()).isEqualTo(FormatoLupulo.PELLET);
        assertThat(resultado.getAa()).isEqualTo(7);
        verify(lupuloRepository).save(lupuloEntity);
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

    @Test
    @DisplayName("bajaLupulo lanza RecursoNoEncontradoException y no elimina cuando el ID no existe")
    void bajaLupulo_debeLanzarExcepcionYNoEliminarSiNoExiste() {
        when(lupuloRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lupuloServicio.bajaLupulo(3982L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el lúpulo con ID: 3982");

        verify(lupuloRepository).findById(3982L);
        verify(lupuloRepository, never()).delete(any());
    }

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
