package com.github.heikyudev.maestrocervecero.service.implementation;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoLevadura;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.insumo.ILevaduraRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.LevaduraFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.LevaduraResponseDTO;
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

    @Test
    @DisplayName("findAll retorna una página de levaduras correctamente mapeada a DTO")
    void findAll_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle US-05", TipoLevadura.ALE, 10.0);

        // Cuando levaduraRepository.findAll(pageable) sea llamado, retorna una página con la levadura activa
        // (el filtrado de soft-deleted es automático por @SoftDelete de Hibernate sobre la entidad)
        when(levaduraRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(levaduraEntity), pageable, 1));

        // === EJECUCION ===
        Page<LevaduraResponseDTO> resultado = levaduraServicio.findAll(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertLevaduraDTO(levaduraEntity, resultado.getContent().get(0));
        verify(levaduraRepository).findAll(pageable);
    }

    @Test
    @DisplayName("obtenerPorId retorna el DTO de la levadura cuando el ID existe")
    void obtenerPorId_debeRetornarLevaduraExistente() {
        // === PREPARACION DE DATOS ===
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafLager W-34/70", TipoLevadura.LAGER, 15.0);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));

        // === EJECUCION ===
        LevaduraResponseDTO resultado = levaduraServicio.obtenerPorId(1L);

        // === VERIFICACION ===
        assertLevaduraDTO(levaduraEntity, resultado);
        verify(levaduraRepository).findById(1L);
    }

    @Test
    @DisplayName("obtenerPorId lanza RecursoNoEncontradoException cuando el ID no existe o está soft-deleted")
    void obtenerPorId_debeLanzarExcepcionSiNoExiste() {
        // @SoftDelete hace que findById devuelva Optional.empty() también para registros eliminados lógicamente
        when(levaduraRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> levaduraServicio.obtenerPorId(3982L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La levadura no existe");
        verify(levaduraRepository).findById(3982L);
    }

    @Test
    @DisplayName("altaLevadura asigna GRAMO como unidad de medida y persiste cuando los datos son válidos")
    void altaLevadura_debePersistirConUnidadGramo() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle US-05", TipoLevadura.ALE, 10.0);
        when(levaduraRepository.existsByNombreIgnoreCase("SafAle US-05")).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> {
            LevaduraEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        LevaduraResponseDTO resultado = levaduraServicio.altaLevadura(levaduraFormDTO);

        ArgumentCaptor<LevaduraEntity> captor = ArgumentCaptor.forClass(LevaduraEntity.class);
        verify(levaduraRepository).save(captor.capture());
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("SafAle US-05");
        assertThat(resultado.getTipo()).isEqualTo(TipoLevadura.ALE);
        assertThat(resultado.getCantidadCelulasPorGramo()).isEqualTo(10.0);
        // La unidad de medida es fija por negocio y la asigna el service, nunca viene del formulario
        assertThat(captor.getValue().getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);
        verify(levaduraRepository).existsByNombreIgnoreCase("SafAle US-05");
    }

    @Test
    @DisplayName("altaLevadura acepta una cantidad positiva mínima como borde válido de la regla positivo (> 0)")
    void altaLevadura_debeAceptarCantidadPositivaMinima() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle K-97", TipoLevadura.ALE, 0.1);
        when(levaduraRepository.existsByNombreIgnoreCase("SafAle K-97")).thenReturn(false);
        when(levaduraRepository.save(any(LevaduraEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LevaduraResponseDTO resultado = levaduraServicio.altaLevadura(levaduraFormDTO);

        assertThat(resultado.getCantidadCelulasPorGramo()).isEqualTo(0.1);
        verify(levaduraRepository).save(any(LevaduraEntity.class));
    }

    @Test
    @DisplayName("altaLevadura lanza ReglaNegocioException y no persiste ni consulta unicidad cuando la cantidad es nula")
    void altaLevadura_debeRechazarCantidadNula() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle US-05", TipoLevadura.ALE, null);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // La validación de negocio se ejecuta antes que la de unicidad y antes de persistir
        verify(levaduraRepository, never()).existsByNombreIgnoreCase(any());
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaLevadura lanza ReglaNegocioException y no persiste cuando la cantidad es cero")
    void altaLevadura_debeRechazarCantidadCero() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle US-05", TipoLevadura.ALE, 0.0);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(levaduraRepository, never()).existsByNombreIgnoreCase(any());
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaLevadura lanza ReglaNegocioException y no persiste cuando la cantidad es negativa")
    void altaLevadura_debeRechazarCantidadNegativa() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle US-05", TipoLevadura.ALE, -1.0);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(levaduraRepository, never()).existsByNombreIgnoreCase(any());
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaLevadura lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe (case-insensitive)")
    void altaLevadura_debeRechazarNombreDuplicado() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("safale us-05", TipoLevadura.ALE, 10.0);
        when(levaduraRepository.existsByNombreIgnoreCase("safale us-05")).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.altaLevadura(levaduraFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(levaduraRepository).existsByNombreIgnoreCase("safale us-05");
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarLevadura actualiza los datos cuando el ID existe y el nombre está libre")
    void modificarLevadura_debeActualizarLevaduraExistente() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "Vieja", TipoLevadura.HIBRIDA, 8.0);
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle US-05", TipoLevadura.ALE, 10.0);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("SafAle US-05", 1L)).thenReturn(false);
        when(levaduraRepository.save(levaduraEntity)).thenReturn(levaduraEntity);

        LevaduraResponseDTO resultado = levaduraServicio.modificarLevadura(1L, levaduraFormDTO);

        assertThat(resultado.getNombre()).isEqualTo("SafAle US-05");
        assertThat(resultado.getTipo()).isEqualTo(TipoLevadura.ALE);
        assertThat(resultado.getCantidadCelulasPorGramo()).isEqualTo(10.0);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.GRAMO);
        verify(levaduraRepository).findById(1L);
        verify(levaduraRepository).existsByNombreIgnoreCaseAndIdNot("SafAle US-05", 1L);
        verify(levaduraRepository).save(levaduraEntity);
    }

    @Test
    @DisplayName("modificarLevadura lanza ReglaNegocioException sin buscar ni persistir cuando la cantidad es inválida")
    void modificarLevadura_debeRechazarCantidadInvalida() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle US-05", TipoLevadura.ALE, 0.0);

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(1L, levaduraFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // La regla de negocio se valida antes de consultar la base de datos
        verifyNoInteractions(levaduraRepository);
    }

    @Test
    @DisplayName("modificarLevadura lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarLevadura_debeLanzarExcepcionSiNoExiste() {
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SafAle US-05", TipoLevadura.ALE, 10.0);
        when(levaduraRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(3982L, levaduraFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La levadura no existe");

        verify(levaduraRepository).findById(3982L);
        verify(levaduraRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarLevadura lanza RecursoDuplicadoException y no persiste cuando el nombre pertenece a otra levadura")
    void modificarLevadura_debeRechazarNombreDuplicadoEnOtraLevadura() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "Vieja", TipoLevadura.HIBRIDA, 8.0);
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("safale us-05", TipoLevadura.ALE, 10.0);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("safale us-05", 1L)).thenReturn(true);

        assertThatThrownBy(() -> levaduraServicio.modificarLevadura(1L, levaduraFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(levaduraRepository).existsByNombreIgnoreCaseAndIdNot("safale us-05", 1L);
        verify(levaduraRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarLevadura permite conservar el propio nombre actualizando solo otros campos")
    void modificarLevadura_debePermitirConservarNombrePropio() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle US-05", TipoLevadura.HIBRIDA, 8.0);
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        LevaduraFormDTO levaduraFormDTO = levaduraFormDTO("SAFALE US-05", TipoLevadura.ALE, 12.0);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));
        when(levaduraRepository.existsByNombreIgnoreCaseAndIdNot("SAFALE US-05", 1L)).thenReturn(false);
        when(levaduraRepository.save(levaduraEntity)).thenReturn(levaduraEntity);

        LevaduraResponseDTO resultado = levaduraServicio.modificarLevadura(1L, levaduraFormDTO);

        assertThat(resultado.getTipo()).isEqualTo(TipoLevadura.ALE);
        assertThat(resultado.getCantidadCelulasPorGramo()).isEqualTo(12.0);
        verify(levaduraRepository).save(levaduraEntity);
    }

    @Test
    @DisplayName("bajaLevadura elimina lógicamente (soft-delete) y retorna el DTO cuando el ID existe")
    void bajaLevadura_debeEliminarYRetornarLevaduraExistente() {
        LevaduraEntity levaduraEntity = crearLevaduraEntity(1L, "SafAle US-05", TipoLevadura.ALE, 10.0);
        when(levaduraRepository.findById(1L)).thenReturn(Optional.of(levaduraEntity));

        LevaduraResponseDTO resultado = levaduraServicio.bajaLevadura(1L);

        // El borrado físico a nivel repositorio es convertido a UPDATE por el @SoftDelete de Hibernate
        assertLevaduraDTO(levaduraEntity, resultado);
        verify(levaduraRepository).findById(1L);
        verify(levaduraRepository).delete(levaduraEntity);
    }

    @Test
    @DisplayName("bajaLevadura lanza RecursoNoEncontradoException y no elimina cuando el ID no existe")
    void bajaLevadura_debeLanzarExcepcionYNoEliminarSiNoExiste() {
        when(levaduraRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> levaduraServicio.bajaLevadura(3982L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la levadura con ID: 3982");

        verify(levaduraRepository).findById(3982L);
        verify(levaduraRepository, never()).delete(any());
    }

    private static LevaduraEntity crearLevaduraEntity(Long id, String nombre, TipoLevadura tipo, Double cantidadCelulasPorGramo) {
        return LevaduraEntity.builder()
                .id(id)
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.GRAMO)
                .tipo(tipo)
                .cantidadCelulasPorGramo(cantidadCelulasPorGramo)
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
    }
}
