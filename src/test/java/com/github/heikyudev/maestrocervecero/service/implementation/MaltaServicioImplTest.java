package com.github.heikyudev.maestrocervecero.service.implementation;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoMalta;
import com.github.heikyudev.maestrocervecero.persistence.enums.UnidadDeMedida;
import com.github.heikyudev.maestrocervecero.persistence.repository.IMaltaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.MaltaFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.MaltaResponseDTO;
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

    @Test
    @DisplayName("findAll retorna una página de maltas correctamente mapeada a DTO")
    void findAll_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pale Ale", TipoMalta.BASE, 80);

        // Cuando maltaRepository.findAll(pageable) sea llamado, retorna una página con la malta activa
        // (el filtrado de soft-deleted es automático por @SoftDelete de Hibernate sobre la entidad)
        when(maltaRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(maltaEntity), pageable, 1));

        // === EJECUCION ===
        Page<MaltaResponseDTO> resultado = maltaServicio.findAll(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertMaltaDTO(maltaEntity, resultado.getContent().get(0));
        verify(maltaRepository).findAll(pageable);
    }

    @Test
    @DisplayName("obtenerPorId retorna el DTO de la malta cuando el ID existe")
    void obtenerPorId_debeRetornarMaltaExistente() {
        // === PREPARACION DE DATOS ===
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Munich", TipoMalta.CARAMELO, 75);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));

        // === EJECUCION ===
        MaltaResponseDTO resultado = maltaServicio.obtenerPorId(1L);

        // === VERIFICACION ===
        assertMaltaDTO(maltaEntity, resultado);
        verify(maltaRepository).findById(1L);
    }

    @Test
    @DisplayName("obtenerPorId lanza RecursoNoEncontradoException cuando el ID no existe o está soft-deleted")
    void obtenerPorId_debeLanzarExcepcionSiNoExiste() {
        // @SoftDelete hace que findById devuelva Optional.empty() también para registros eliminados lógicamente
        when(maltaRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maltaServicio.obtenerPorId(3982L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La malta no existe");
        verify(maltaRepository).findById(3982L);
    }

    @Test
    @DisplayName("altaMalta asigna KILOGRAMO como unidad de medida y persiste cuando los datos son válidos")
    void altaMalta_debePersistirConUnidadKilogramo() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pale Ale", TipoMalta.BASE, 80);
        when(maltaRepository.existsByNombreIgnoreCase("Pale Ale")).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenAnswer(invocation -> {
            MaltaEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        MaltaResponseDTO resultado = maltaServicio.altaMalta(maltaFormDTO);

        ArgumentCaptor<MaltaEntity> captor = ArgumentCaptor.forClass(MaltaEntity.class);
        verify(maltaRepository).save(captor.capture());
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Pale Ale");
        assertThat(resultado.getTipo()).isEqualTo(TipoMalta.BASE);
        assertThat(resultado.getRendimiento()).isEqualTo(80);
        // La unidad de medida es fija por negocio y la asigna el service, nunca viene del formulario
        assertThat(captor.getValue().getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        verify(maltaRepository).existsByNombreIgnoreCase("Pale Ale");
    }

    @Test
    @DisplayName("altaMalta acepta rendimiento 0 como borde válido de la regla 0-100 inclusive")
    void altaMalta_debeAceptarRendimientoCero() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Negra", TipoMalta.TOSTADA, 0);
        when(maltaRepository.existsByNombreIgnoreCase("Negra")).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaltaResponseDTO resultado = maltaServicio.altaMalta(maltaFormDTO);

        assertThat(resultado.getRendimiento()).isEqualTo(0);
        verify(maltaRepository).save(any(MaltaEntity.class));
    }

    @Test
    @DisplayName("altaMalta acepta rendimiento 100 como borde válido de la regla 0-100 inclusive")
    void altaMalta_debeAceptarRendimientoCien() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Caramelo 120", TipoMalta.CARAMELO, 100);
        when(maltaRepository.existsByNombreIgnoreCase("Caramelo 120")).thenReturn(false);
        when(maltaRepository.save(any(MaltaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaltaResponseDTO resultado = maltaServicio.altaMalta(maltaFormDTO);

        assertThat(resultado.getRendimiento()).isEqualTo(100);
        verify(maltaRepository).save(any(MaltaEntity.class));
    }

    @Test
    @DisplayName("altaMalta lanza ReglaNegocioException y no persiste ni consulta unicidad cuando el rendimiento es negativo")
    void altaMalta_debeRechazarRendimientoNegativo() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pale Ale", TipoMalta.BASE, -1);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // La validación de negocio se ejecuta antes que la de unicidad y antes de persistir
        verify(maltaRepository, never()).existsByNombreIgnoreCase(any());
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaMalta lanza ReglaNegocioException y no persiste cuando el rendimiento supera 100")
    void altaMalta_debeRechazarRendimientoSuperiorACien() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Pale Ale", TipoMalta.BASE, 101);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        verify(maltaRepository, never()).existsByNombreIgnoreCase(any());
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("altaMalta lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe (case-insensitive)")
    void altaMalta_debeRechazarNombreDuplicado() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("pale ale", TipoMalta.BASE, 80);
        when(maltaRepository.existsByNombreIgnoreCase("pale ale")).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.altaMalta(maltaFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(maltaRepository).existsByNombreIgnoreCase("pale ale");
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMalta actualiza los datos cuando el ID existe y el nombre está libre")
    void modificarMalta_debeActualizarMaltaExistente() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Vieja", TipoMalta.BASE, 70);
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Munich", TipoMalta.CARAMELO, 75);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("Munich", 1L)).thenReturn(false);
        when(maltaRepository.save(maltaEntity)).thenReturn(maltaEntity);

        MaltaResponseDTO resultado = maltaServicio.modificarMalta(1L, maltaFormDTO);

        assertThat(resultado.getNombre()).isEqualTo("Munich");
        assertThat(resultado.getTipo()).isEqualTo(TipoMalta.CARAMELO);
        assertThat(resultado.getRendimiento()).isEqualTo(75);
        assertThat(resultado.getUnidadDeMedida()).isEqualTo(UnidadDeMedida.KILOGRAMO);
        verify(maltaRepository).findById(1L);
        verify(maltaRepository).existsByNombreIgnoreCaseAndIdNot("Munich", 1L);
        verify(maltaRepository).save(maltaEntity);
    }

    @Test
    @DisplayName("modificarMalta lanza ReglaNegocioException sin buscar ni persistir cuando el rendimiento es inválido")
    void modificarMalta_debeRechazarRendimientoInvalido() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Munich", TipoMalta.CARAMELO, 150);

        assertThatThrownBy(() -> maltaServicio.modificarMalta(1L, maltaFormDTO))
                .isInstanceOf(ReglaNegocioException.class);

        // La regla de negocio se valida antes de consultar la base de datos
        verifyNoInteractions(maltaRepository);
    }

    @Test
    @DisplayName("modificarMalta lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarMalta_debeLanzarExcepcionSiNoExiste() {
        MaltaFormDTO maltaFormDTO = maltaFormDTO("Munich", TipoMalta.CARAMELO, 75);
        when(maltaRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maltaServicio.modificarMalta(3982L, maltaFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La malta no existe");

        verify(maltaRepository).findById(3982L);
        verify(maltaRepository, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMalta lanza RecursoDuplicadoException y no persiste cuando el nombre pertenece a otra malta")
    void modificarMalta_debeRechazarNombreDuplicadoEnOtraMalta() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Vieja", TipoMalta.BASE, 70);
        MaltaFormDTO maltaFormDTO = maltaFormDTO("pale ale", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("pale ale", 1L)).thenReturn(true);

        assertThatThrownBy(() -> maltaServicio.modificarMalta(1L, maltaFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(maltaRepository).existsByNombreIgnoreCaseAndIdNot("pale ale", 1L);
        verify(maltaRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarMalta permite conservar el propio nombre actualizando solo otros campos")
    void modificarMalta_debePermitirConservarNombrePropio() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pale Ale", TipoMalta.BASE, 70);
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        MaltaFormDTO maltaFormDTO = maltaFormDTO("PALE ALE", TipoMalta.ESPECIAL, 85);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));
        when(maltaRepository.existsByNombreIgnoreCaseAndIdNot("PALE ALE", 1L)).thenReturn(false);
        when(maltaRepository.save(maltaEntity)).thenReturn(maltaEntity);

        MaltaResponseDTO resultado = maltaServicio.modificarMalta(1L, maltaFormDTO);

        assertThat(resultado.getTipo()).isEqualTo(TipoMalta.ESPECIAL);
        assertThat(resultado.getRendimiento()).isEqualTo(85);
        verify(maltaRepository).save(maltaEntity);
    }

    @Test
    @DisplayName("bajaMalta elimina lógicamente (soft-delete) y retorna el DTO cuando el ID existe")
    void bajaMalta_debeEliminarYRetornarMaltaExistente() {
        MaltaEntity maltaEntity = crearMaltaEntity(1L, "Pale Ale", TipoMalta.BASE, 80);
        when(maltaRepository.findById(1L)).thenReturn(Optional.of(maltaEntity));

        MaltaResponseDTO resultado = maltaServicio.bajaMalta(1L);

        // El borrado físico a nivel repositorio es convertido a UPDATE por el @SoftDelete de Hibernate
        assertMaltaDTO(maltaEntity, resultado);
        verify(maltaRepository).findById(1L);
        verify(maltaRepository).delete(maltaEntity);
    }

    @Test
    @DisplayName("bajaMalta lanza RecursoNoEncontradoException y no elimina cuando el ID no existe")
    void bajaMalta_debeLanzarExcepcionYNoEliminarSiNoExiste() {
        when(maltaRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maltaServicio.bajaMalta(3982L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la malta con ID: 3982");

        verify(maltaRepository).findById(3982L);
        verify(maltaRepository, never()).delete(any());
    }

    private static MaltaEntity crearMaltaEntity(Long id, String nombre, TipoMalta tipo, Integer rendimiento) {
        return MaltaEntity.builder()
                .id(id)
                .nombre(nombre)
                .unidadDeMedida(UnidadDeMedida.KILOGRAMO)
                .tipo(tipo)
                .rendimiento(rendimiento)
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
    }
}
