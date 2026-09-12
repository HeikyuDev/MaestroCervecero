package com.github.heikyudev.maestrocervecero.service.implementation.parametro_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.parametro_control.ParametroControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.parametro_control.IParametroControlRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.parametro_control.ParametroControlFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.parametro_control.ParametroControlResponseDTO;
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
class ParametroControlServicioImplTest {

    @Mock
    private IParametroControlRepository parametroControlRepository;

    @InjectMocks
    private ParametroControlServicioImpl parametroControlServicio;

    // ==================== filtrarParametrosControl ====================

    @Test
    @DisplayName("CP-FPC-01: filtrarParametrosControl retorna una página de parámetros de control correctamente mapeada a DTO cuando se filtra por nombre")
    void filtrarParametrosControl_debeRetornarPaginaMapeadaFiltrandoPorNombre() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        ParametroControlEntity parametroControlEntity = crearParametroControlEntity(1L, "Temperatura Maceración", "Control de temperatura", 65.0, 68.0);
        ParametroControlEntity otroParametroControlEntity = crearParametroControlEntity(2L, "Temperatura Fermentación", "Control de temperatura", 18.0, 22.0);
        when(parametroControlRepository.filtrarParametrosControl("Temperatura", pageable))
                .thenReturn(new PageImpl<>(List.of(parametroControlEntity, otroParametroControlEntity), pageable, 2));

        // === EJECUCION ===
        Page<ParametroControlResponseDTO> resultado = parametroControlServicio.filtrarParametrosControl("Temperatura", pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertParametroControlDTO(parametroControlEntity, resultado.getContent().get(0));
        assertParametroControlDTO(otroParametroControlEntity, resultado.getContent().get(1));
        verify(parametroControlRepository).filtrarParametrosControl("Temperatura", pageable);
    }

    @Test
    @DisplayName("CP-FPC-02: filtrarParametrosControl propaga nombre nulo sin restringir ese criterio")
    void filtrarParametrosControl_debePropagarNombreNulo() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        ParametroControlEntity parametroControlEntity = crearParametroControlEntity(1L, "Temperatura", "Control de temperatura", 65.0, 68.0);
        ParametroControlEntity otroParametroControlEntity = crearParametroControlEntity(2L, "Densidad", "Control de densidad", 1.010, 1.060);
        when(parametroControlRepository.filtrarParametrosControl(null, pageable))
                .thenReturn(new PageImpl<>(List.of(parametroControlEntity, otroParametroControlEntity), pageable, 2));

        // === EJECUCION ===
        Page<ParametroControlResponseDTO> resultado = parametroControlServicio.filtrarParametrosControl(null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(parametroControlRepository).filtrarParametrosControl(null, pageable);
    }

    @Test
    @DisplayName("CP-FPC-03: filtrarParametrosControl retorna una página vacía cuando ningún registro cumple el criterio")
    void filtrarParametrosControl_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(parametroControlRepository.filtrarParametrosControl("Inexistente", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<ParametroControlResponseDTO> resultado = parametroControlServicio.filtrarParametrosControl("Inexistente", pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(parametroControlRepository).filtrarParametrosControl("Inexistente", pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del parámetro de control cuando el ID existe")
    void buscarPorId_debeRetornarParametroControlExistente() {
        // === PREPARACION DE DATOS ===
        ParametroControlEntity parametroControlEntity = crearParametroControlEntity(1L, "Temperatura", "Control de temperatura", 65.0, 68.0);
        when(parametroControlRepository.findById(1L)).thenReturn(Optional.of(parametroControlEntity));

        // === EJECUCION ===
        ParametroControlResponseDTO resultado = parametroControlServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertParametroControlDTO(parametroControlEntity, resultado);
        verify(parametroControlRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para parámetros dados de baja
        when(parametroControlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parametroControlServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el parámetro de control con ID: 99");
        verify(parametroControlRepository).findById(99L);
    }

    // ==================== altaParametroControl ====================

    @Test
    @DisplayName("CP-AP-01: altaParametroControl lanza ReglaNegocioException y no consulta el repositorio cuando el valor mínimo es nulo")
    void altaParametroControl_debeRechazarValorMinimoNulo() {
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Temperatura", null, 10.0);

        assertThatThrownBy(() -> parametroControlServicio.altaParametroControl(parametroControlFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El valor mínimo debe ser mayor a 0");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(parametroControlRepository);
    }

    @Test
    @DisplayName("CP-AP-02: altaParametroControl lanza ReglaNegocioException cuando el valor mínimo es igual a cero (valor límite)")
    void altaParametroControl_debeRechazarValorMinimoCero() {
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Temperatura", 0.0, 10.0);

        assertThatThrownBy(() -> parametroControlServicio.altaParametroControl(parametroControlFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El valor mínimo debe ser mayor a 0");

        verifyNoInteractions(parametroControlRepository);
    }

    @Test
    @DisplayName("CP-AP-03: altaParametroControl lanza ReglaNegocioException cuando el valor mínimo es negativo (límite inferior)")
    void altaParametroControl_debeRechazarValorMinimoNegativo() {
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Temperatura", -0.1, 10.0);

        assertThatThrownBy(() -> parametroControlServicio.altaParametroControl(parametroControlFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El valor mínimo debe ser mayor a 0");

        verifyNoInteractions(parametroControlRepository);
    }

    @Test
    @DisplayName("CP-AP-04: altaParametroControl lanza ReglaNegocioException y no consulta el repositorio cuando el valor máximo es nulo")
    void altaParametroControl_debeRechazarValorMaximoNulo() {
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Temperatura", 5.0, null);

        assertThatThrownBy(() -> parametroControlServicio.altaParametroControl(parametroControlFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El valor mínimo no puede ser mayor al valor máximo");

        verifyNoInteractions(parametroControlRepository);
    }

    @Test
    @DisplayName("CP-AP-05: altaParametroControl lanza ReglaNegocioException y no consulta el repositorio cuando el valor mínimo es mayor al máximo")
    void altaParametroControl_debeRechazarValorMinimoMayorAlMaximo() {
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Temperatura", 15.0, 10.0);

        assertThatThrownBy(() -> parametroControlServicio.altaParametroControl(parametroControlFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El valor mínimo no puede ser mayor al valor máximo");

        verifyNoInteractions(parametroControlRepository);
    }

    @Test
    @DisplayName("CP-AP-06: altaParametroControl lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe")
    void altaParametroControl_debeRechazarNombreDuplicado() {
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Densidad", 5.0, 10.0);
        when(parametroControlRepository.existsByNombreIgnoreCase("Densidad")).thenReturn(true);

        assertThatThrownBy(() -> parametroControlServicio.altaParametroControl(parametroControlFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un parámetro de control con el nombre 'Densidad'");

        verify(parametroControlRepository).existsByNombreIgnoreCase("Densidad");
        verify(parametroControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-07: altaParametroControl lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case (case-insensitive)")
    void altaParametroControl_debeRechazarNombreDuplicadoCaseInsensitive() {
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("densidad inicial", 5.0, 10.0);
        when(parametroControlRepository.existsByNombreIgnoreCase("densidad inicial")).thenReturn(true);

        assertThatThrownBy(() -> parametroControlServicio.altaParametroControl(parametroControlFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un parámetro de control con el nombre 'densidad inicial'");

        verify(parametroControlRepository).existsByNombreIgnoreCase("densidad inicial");
        verify(parametroControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AP-08: altaParametroControl persiste y retorna el DTO cuando los datos son válidos y únicos (camino feliz)")
    void altaParametroControl_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Temperatura", 65.0, 68.0);
        when(parametroControlRepository.existsByNombreIgnoreCase("Temperatura")).thenReturn(false);
        when(parametroControlRepository.save(any(ParametroControlEntity.class))).thenAnswer(invocation -> {
            ParametroControlEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        ParametroControlResponseDTO resultado = parametroControlServicio.altaParametroControl(parametroControlFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<ParametroControlEntity> captor = ArgumentCaptor.forClass(ParametroControlEntity.class);
        verify(parametroControlRepository).save(captor.capture());
        ParametroControlEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Temperatura");
        assertThat(entidadCapturada.getDescripcion()).isEqualTo("Descripción de prueba");
        assertThat(entidadCapturada.getValorMinimo()).isEqualTo(65.0);
        assertThat(entidadCapturada.getValorMaximo()).isEqualTo(68.0);
        // El alta siempre debe registrar al parámetro de control como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Temperatura");
        assertThat(resultado.getValorMinimo()).isEqualTo(65.0);
        assertThat(resultado.getValorMaximo()).isEqualTo(68.0);
        verify(parametroControlRepository).existsByNombreIgnoreCase("Temperatura");
    }

    @Test
    @DisplayName("CP-AP-09: altaParametroControl persiste con éxito cuando el valor mínimo es igual al valor máximo (límite válido)")
    void altaParametroControl_debePersistirConValorMinimoIgualAlMaximo() {
        // === PREPARACION DE DATOS ===
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("pH", 5.2, 5.2);
        when(parametroControlRepository.existsByNombreIgnoreCase("pH")).thenReturn(false);
        when(parametroControlRepository.save(any(ParametroControlEntity.class))).thenAnswer(invocation -> {
            ParametroControlEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(2L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        ParametroControlResponseDTO resultado = parametroControlServicio.altaParametroControl(parametroControlFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getValorMinimo()).isEqualTo(5.2);
        assertThat(resultado.getValorMaximo()).isEqualTo(5.2);
    }

    @Test
    @DisplayName("CP-AP-10: altaParametroControl persiste con éxito cuando el valor mínimo está en el límite positivo inferior válido")
    void altaParametroControl_debePersistirConValorMinimoEnLimiteInferiorValido() {
        // === PREPARACION DE DATOS ===
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Oxígeno Disuelto", 0.1, 1.0);
        when(parametroControlRepository.existsByNombreIgnoreCase("Oxígeno Disuelto")).thenReturn(false);
        when(parametroControlRepository.save(any(ParametroControlEntity.class))).thenAnswer(invocation -> {
            ParametroControlEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(3L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        ParametroControlResponseDTO resultado = parametroControlServicio.altaParametroControl(parametroControlFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getValorMinimo()).isEqualTo(0.1);
        assertThat(resultado.getNombre()).isEqualTo("Oxígeno Disuelto");
    }

    // ==================== modificarParametroControl ====================

    @Test
    @DisplayName("CP-MP-01: modificarParametroControl lanza ReglaNegocioException y no consulta el repositorio cuando el valor mínimo es inválido")
    void modificarParametroControl_debeRechazarValorMinimoInvalido() {
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Temperatura", 0.0, 10.0);

        assertThatThrownBy(() -> parametroControlServicio.modificarParametroControl(1L, parametroControlFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El valor mínimo debe ser mayor a 0");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(parametroControlRepository);
    }

    @Test
    @DisplayName("CP-MP-02: modificarParametroControl lanza ReglaNegocioException y no consulta el repositorio cuando el mínimo es mayor al máximo")
    void modificarParametroControl_debeRechazarMinimoMayorAlMaximo() {
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Temperatura", 20.0, 10.0);

        assertThatThrownBy(() -> parametroControlServicio.modificarParametroControl(1L, parametroControlFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El valor mínimo no puede ser mayor al valor máximo");

        verifyNoInteractions(parametroControlRepository);
    }

    @Test
    @DisplayName("CP-MP-03: modificarParametroControl lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otro parámetro de control")
    void modificarParametroControl_debeRechazarNombreEnUsoPorOtroParametroControl() {
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Densidad", 1.0, 2.0);
        when(parametroControlRepository.existsByNombreIgnoreCaseAndIdNot("Densidad", 1L)).thenReturn(true);

        assertThatThrownBy(() -> parametroControlServicio.modificarParametroControl(1L, parametroControlFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un parámetro de control con el nombre 'Densidad'");

        verify(parametroControlRepository).existsByNombreIgnoreCaseAndIdNot("Densidad", 1L);
        // Al detectarse la duplicación, no debe llegarse a buscar por ID ni persistir
        verify(parametroControlRepository, never()).findById(any());
        verify(parametroControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-04: modificarParametroControl lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarParametroControl_debeLanzarExcepcionSiNoExiste() {
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("Temperatura", 5.0, 10.0);
        when(parametroControlRepository.existsByNombreIgnoreCaseAndIdNot("Temperatura", 99L)).thenReturn(false);
        when(parametroControlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parametroControlServicio.modificarParametroControl(99L, parametroControlFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el parámetro de control con ID: 99");

        verify(parametroControlRepository).findById(99L);
        verify(parametroControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MP-05: modificarParametroControl actualiza los datos y persiste cuando el ID existe y el nombre está libre (camino feliz)")
    void modificarParametroControl_debeActualizarParametroControlExistente() {
        // === PREPARACION DE DATOS ===
        ParametroControlEntity parametroControlEntity = crearParametroControlEntity(1L, "Temperatura", "Descripción original", 60.0, 70.0);
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("pH", 5.0, 5.5);
        when(parametroControlRepository.existsByNombreIgnoreCaseAndIdNot("pH", 1L)).thenReturn(false);
        when(parametroControlRepository.findById(1L)).thenReturn(Optional.of(parametroControlEntity));
        when(parametroControlRepository.save(parametroControlEntity)).thenReturn(parametroControlEntity);

        // === EJECUCION ===
        ParametroControlResponseDTO resultado = parametroControlServicio.modificarParametroControl(1L, parametroControlFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<ParametroControlEntity> captor = ArgumentCaptor.forClass(ParametroControlEntity.class);
        verify(parametroControlRepository).save(captor.capture());
        ParametroControlEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("pH");
        assertThat(entidadCapturada.getDescripcion()).isEqualTo("Descripción de prueba");
        assertThat(entidadCapturada.getValorMinimo()).isEqualTo(5.0);
        assertThat(entidadCapturada.getValorMaximo()).isEqualTo(5.5);

        assertThat(resultado.getNombre()).isEqualTo("pH");
        assertThat(resultado.getValorMinimo()).isEqualTo(5.0);
        assertThat(resultado.getValorMaximo()).isEqualTo(5.5);
        verify(parametroControlRepository).existsByNombreIgnoreCaseAndIdNot("pH", 1L);
        verify(parametroControlRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-MP-06: modificarParametroControl permite conservar el propio nombre actual al actualizar otros campos")
    void modificarParametroControl_debePermitirConservarNombrePropio() {
        ParametroControlEntity parametroControlEntity = crearParametroControlEntity(1L, "pH", "Descripción original", 5.0, 5.5);
        // Mismo nombre (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        ParametroControlFormDTO parametroControlFormDTO = parametroControlFormDTO("ph", 5.0, 5.5);
        when(parametroControlRepository.existsByNombreIgnoreCaseAndIdNot("ph", 1L)).thenReturn(false);
        when(parametroControlRepository.findById(1L)).thenReturn(Optional.of(parametroControlEntity));
        when(parametroControlRepository.save(parametroControlEntity)).thenReturn(parametroControlEntity);

        ParametroControlResponseDTO resultado = parametroControlServicio.modificarParametroControl(1L, parametroControlFormDTO);

        assertThat(resultado.getNombre()).isEqualTo("ph");
        verify(parametroControlRepository).save(parametroControlEntity);
    }

    // ==================== bajaParametroControl ====================

    @Test
    @DisplayName("CP-BP-01: bajaParametroControl lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaParametroControl_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(parametroControlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parametroControlServicio.bajaParametroControl(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el parámetro de control con ID: 99");

        verify(parametroControlRepository).findById(99L);
        verify(parametroControlRepository, never()).existsPlanMonitoreoActivoAsociado(any());
        verify(parametroControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BP-02: bajaParametroControl lanza ReglaNegocioException y no persiste cuando está asociado a un plan de monitoreo de una receta activa")
    void bajaParametroControl_debeRechazarConPlanMonitoreoActivoAsociado() {
        ParametroControlEntity parametroControlEntity = crearParametroControlEntity(1L, "Temperatura", "Control de temperatura", 65.0, 68.0);
        when(parametroControlRepository.findById(1L)).thenReturn(Optional.of(parametroControlEntity));
        when(parametroControlRepository.existsPlanMonitoreoActivoAsociado(1L)).thenReturn(true);

        assertThatThrownBy(() -> parametroControlServicio.bajaParametroControl(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el parámetro de control porque está asociado al plan de monitoreo de una receta activa");

        verify(parametroControlRepository).existsPlanMonitoreoActivoAsociado(1L);
        verify(parametroControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BP-03: bajaParametroControl marca el estado como BAJA, persiste y retorna el DTO cuando no tiene planes de monitoreo activos asociados")
    void bajaParametroControl_debeMarcarBajaYRetornarParametroControlExistente() {
        ParametroControlEntity parametroControlEntity = crearParametroControlEntity(1L, "Temperatura", "Control de temperatura", 65.0, 68.0);
        when(parametroControlRepository.findById(1L)).thenReturn(Optional.of(parametroControlEntity));
        when(parametroControlRepository.existsPlanMonitoreoActivoAsociado(1L)).thenReturn(false);
        when(parametroControlRepository.save(parametroControlEntity)).thenReturn(parametroControlEntity);

        ParametroControlResponseDTO resultado = parametroControlServicio.bajaParametroControl(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(parametroControlEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertParametroControlDTO(parametroControlEntity, resultado);
        verify(parametroControlRepository).findById(1L);
        verify(parametroControlRepository).existsPlanMonitoreoActivoAsociado(1L);
        verify(parametroControlRepository).save(parametroControlEntity);
        verify(parametroControlRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static ParametroControlEntity crearParametroControlEntity(Long id, String nombre, String descripcion, Double valorMinimo, Double valorMaximo) {
        return ParametroControlEntity.builder()
                .id(id)
                .nombre(nombre)
                .descripcion(descripcion)
                .valorMinimo(valorMinimo)
                .valorMaximo(valorMaximo)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static ParametroControlFormDTO parametroControlFormDTO(String nombre, Double valorMinimo, Double valorMaximo) {
        return ParametroControlFormDTO.builder()
                .nombre(nombre)
                .descripcion("Descripción de prueba")
                .valorMinimo(valorMinimo)
                .valorMaximo(valorMaximo)
                .build();
    }

    private static void assertParametroControlDTO(ParametroControlEntity entidad, ParametroControlResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getDescripcion()).isEqualTo(entidad.getDescripcion());
        assertThat(dto.getValorMinimo()).isEqualTo(entidad.getValorMinimo());
        assertThat(dto.getValorMaximo()).isEqualTo(entidad.getValorMaximo());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
