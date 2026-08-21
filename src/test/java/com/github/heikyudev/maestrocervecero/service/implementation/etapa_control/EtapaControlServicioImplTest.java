package com.github.heikyudev.maestrocervecero.service.implementation.etapa_control;

import com.github.heikyudev.maestrocervecero.persistence.entity.etapa_control.EtapaControlEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.enums.TipoEtapa;
import com.github.heikyudev.maestrocervecero.persistence.repository.etapa_control.IEtapaControlRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.etapa_control.EtapaControlFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.etapa_control.EtapaControlResponseDTO;
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
class EtapaControlServicioImplTest {

    @Mock
    private IEtapaControlRepository etapaControlRepository;

    @InjectMocks
    private EtapaControlServicioImpl etapaControlServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de etapas de control correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        EtapaControlEntity etapaControlEntity = crearEtapaControlEntity(1L, "Control Densidad", "Medición de densidad", TipoEtapa.MACERACION);
        EtapaControlEntity otraEtapaControlEntity = crearEtapaControlEntity(2L, "Control Temperatura", "Medición de temperatura", TipoEtapa.FERMENTACION);

        // Cuando etapaControlRepository.findAll(pageable) sea llamado, retorna una página con las etapas activas
        // (el filtrado por estado = ACTIVO ya está resuelto dentro de la consulta del repositorio)
        when(etapaControlRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(etapaControlEntity, otraEtapaControlEntity), pageable, 2));

        // === EJECUCION ===
        Page<EtapaControlResponseDTO> resultado = etapaControlServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertEtapaControlDTO(etapaControlEntity, resultado.getContent().get(0));
        assertEtapaControlDTO(otraEtapaControlEntity, resultado.getContent().get(1));
        verify(etapaControlRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay etapas de control registradas")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(etapaControlRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<EtapaControlResponseDTO> resultado = etapaControlServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(etapaControlRepository).findAll(pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la etapa de control cuando el ID existe")
    void buscarPorId_debeRetornarEtapaControlExistente() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControlEntity = crearEtapaControlEntity(1L, "Control Densidad", "Medición de densidad", TipoEtapa.MACERACION);
        when(etapaControlRepository.findById(1L)).thenReturn(Optional.of(etapaControlEntity));

        // === EJECUCION ===
        EtapaControlResponseDTO resultado = etapaControlServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertEtapaControlDTO(etapaControlEntity, resultado);
        verify(etapaControlRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dada de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para etapas de control dadas de baja
        when(etapaControlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etapaControlServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de control con ID: 99");
        verify(etapaControlRepository).findById(99L);
    }

    // ==================== altaEtapaControl ====================

    @Test
    @DisplayName("CP-AC-01: altaEtapaControl lanza ReglaNegocioException y no consulta el repositorio cuando la etapa a controlar es nula")
    void altaEtapaControl_debeRechazarEtapaAControlarNula() {
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("Control pH", null);

        assertThatThrownBy(() -> etapaControlServicio.altaEtapaControl(etapaControlFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa a controlar debe ser MACERACION, FERMENTACION, HERVIDO o MADURACION");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(etapaControlRepository);
    }

    @Test
    @DisplayName("CP-AC-02: altaEtapaControl lanza ReglaNegocioException y no consulta el repositorio cuando la etapa a controlar es MOLIENDA")
    void altaEtapaControl_debeRechazarEtapaMolienda() {
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("Control Molienda", TipoEtapa.MOLIENDA);

        assertThatThrownBy(() -> etapaControlServicio.altaEtapaControl(etapaControlFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa a controlar debe ser MACERACION, FERMENTACION, HERVIDO o MADURACION");

        verifyNoInteractions(etapaControlRepository);
    }

    @Test
    @DisplayName("CP-AC-03: altaEtapaControl lanza ReglaNegocioException y no consulta el repositorio cuando la etapa a controlar es ENVASADO")
    void altaEtapaControl_debeRechazarEtapaEnvasado() {
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("Control Oxígeno", TipoEtapa.ENVASADO);

        assertThatThrownBy(() -> etapaControlServicio.altaEtapaControl(etapaControlFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa a controlar debe ser MACERACION, FERMENTACION, HERVIDO o MADURACION");

        verifyNoInteractions(etapaControlRepository);
    }

    @Test
    @DisplayName("CP-AC-04: altaEtapaControl lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe para la misma etapa")
    void altaEtapaControl_debeRechazarNombreDuplicadoParaMismaEtapa() {
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("Control Densidad", TipoEtapa.MACERACION);
        when(etapaControlRepository.existsByNombreIgnoreCaseAndEtapaAControlar("Control Densidad", TipoEtapa.MACERACION)).thenReturn(true);

        assertThatThrownBy(() -> etapaControlServicio.altaEtapaControl(etapaControlFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una etapa de control con el nombre 'Control Densidad' para la etapa MACERACION");

        verify(etapaControlRepository).existsByNombreIgnoreCaseAndEtapaAControlar("Control Densidad", TipoEtapa.MACERACION);
        verify(etapaControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-05: altaEtapaControl lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case para la misma etapa (case-insensitive)")
    void altaEtapaControl_debeRechazarNombreDuplicadoCaseInsensitive() {
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("control densidad", TipoEtapa.MACERACION);
        when(etapaControlRepository.existsByNombreIgnoreCaseAndEtapaAControlar("control densidad", TipoEtapa.MACERACION)).thenReturn(true);

        assertThatThrownBy(() -> etapaControlServicio.altaEtapaControl(etapaControlFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una etapa de control con el nombre 'control densidad' para la etapa MACERACION");

        verify(etapaControlRepository).existsByNombreIgnoreCaseAndEtapaAControlar("control densidad", TipoEtapa.MACERACION);
        verify(etapaControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-06: altaEtapaControl permite el mismo nombre cuando la etapa a controlar es distinta")
    void altaEtapaControl_debePermitirMismoNombreParaEtapaDistinta() {
        // === PREPARACION DE DATOS ===
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("Control Densidad", TipoEtapa.FERMENTACION);
        // "Control Densidad" ya existe pero para MACERACION, no para FERMENTACION
        when(etapaControlRepository.existsByNombreIgnoreCaseAndEtapaAControlar("Control Densidad", TipoEtapa.FERMENTACION)).thenReturn(false);
        when(etapaControlRepository.save(any(EtapaControlEntity.class))).thenAnswer(invocation -> {
            EtapaControlEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(2L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        EtapaControlResponseDTO resultado = etapaControlServicio.altaEtapaControl(etapaControlFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<EtapaControlEntity> captor = ArgumentCaptor.forClass(EtapaControlEntity.class);
        verify(etapaControlRepository).save(captor.capture());
        EtapaControlEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Control Densidad");
        assertThat(entidadCapturada.getEtapaAControlar()).isEqualTo(TipoEtapa.FERMENTACION);

        assertThat(resultado.getId()).isEqualTo(2L);
        assertThat(resultado.getNombre()).isEqualTo("Control Densidad");
        assertThat(resultado.getEtapaAControlar()).isEqualTo(TipoEtapa.FERMENTACION);
    }

    @Test
    @DisplayName("CP-AC-07: altaEtapaControl persiste y retorna el DTO cuando los datos son válidos y únicos (camino feliz)")
    void altaEtapaControl_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("Control Temperatura Fermentación", TipoEtapa.FERMENTACION);
        when(etapaControlRepository.existsByNombreIgnoreCaseAndEtapaAControlar("Control Temperatura Fermentación", TipoEtapa.FERMENTACION)).thenReturn(false);
        when(etapaControlRepository.save(any(EtapaControlEntity.class))).thenAnswer(invocation -> {
            EtapaControlEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        EtapaControlResponseDTO resultado = etapaControlServicio.altaEtapaControl(etapaControlFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<EtapaControlEntity> captor = ArgumentCaptor.forClass(EtapaControlEntity.class);
        verify(etapaControlRepository).save(captor.capture());
        EtapaControlEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Control Temperatura Fermentación");
        assertThat(entidadCapturada.getDescripcion()).isEqualTo("Descripción de prueba");
        assertThat(entidadCapturada.getEtapaAControlar()).isEqualTo(TipoEtapa.FERMENTACION);
        // El alta siempre debe registrar a la etapa de control como ACTIVA, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Control Temperatura Fermentación");
        assertThat(resultado.getEtapaAControlar()).isEqualTo(TipoEtapa.FERMENTACION);
        verify(etapaControlRepository).existsByNombreIgnoreCaseAndEtapaAControlar("Control Temperatura Fermentación", TipoEtapa.FERMENTACION);
    }

    // ==================== modificarEtapaControl ====================

    @Test
    @DisplayName("CP-MC-01: modificarEtapaControl lanza ReglaNegocioException y no consulta el repositorio cuando la etapa a controlar no es admitida")
    void modificarEtapaControl_debeRechazarEtapaAControlarNoAdmitida() {
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("Control pH", TipoEtapa.MOLIENDA);

        assertThatThrownBy(() -> etapaControlServicio.modificarEtapaControl(1L, etapaControlFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La etapa a controlar debe ser MACERACION, FERMENTACION, HERVIDO o MADURACION");

        // La validación de negocio se ejecuta antes de cualquier acceso a la base de datos
        verifyNoInteractions(etapaControlRepository);
    }

    @Test
    @DisplayName("CP-MC-02: modificarEtapaControl lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otra etapa de control para la misma etapa")
    void modificarEtapaControl_debeRechazarNombreEnUsoPorOtraEtapaControl() {
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("Control pH", TipoEtapa.HERVIDO);
        when(etapaControlRepository.existsByNombreIgnoreCaseAndEtapaAControlarAndIdNot("Control pH", TipoEtapa.HERVIDO, 1L)).thenReturn(true);

        assertThatThrownBy(() -> etapaControlServicio.modificarEtapaControl(1L, etapaControlFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una etapa de control con el nombre 'Control pH' para la etapa HERVIDO");

        verify(etapaControlRepository).existsByNombreIgnoreCaseAndEtapaAControlarAndIdNot("Control pH", TipoEtapa.HERVIDO, 1L);
        // Al detectarse la duplicación, no debe llegarse a buscar por ID ni persistir
        verify(etapaControlRepository, never()).findById(any());
        verify(etapaControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MC-03: modificarEtapaControl lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarEtapaControl_debeLanzarExcepcionSiNoExiste() {
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("Control pH", TipoEtapa.HERVIDO);
        when(etapaControlRepository.existsByNombreIgnoreCaseAndEtapaAControlarAndIdNot("Control pH", TipoEtapa.HERVIDO, 99L)).thenReturn(false);
        when(etapaControlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etapaControlServicio.modificarEtapaControl(99L, etapaControlFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de control con ID: 99");

        verify(etapaControlRepository).findById(99L);
        verify(etapaControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MC-04: modificarEtapaControl actualiza los datos y persiste cuando el ID existe y el nombre está libre (camino feliz)")
    void modificarEtapaControl_debeActualizarEtapaControlExistente() {
        // === PREPARACION DE DATOS ===
        EtapaControlEntity etapaControlEntity = crearEtapaControlEntity(1L, "Control pH", "Descripción original", TipoEtapa.MACERACION);
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("Control pH Final", TipoEtapa.HERVIDO);
        when(etapaControlRepository.existsByNombreIgnoreCaseAndEtapaAControlarAndIdNot("Control pH Final", TipoEtapa.HERVIDO, 1L)).thenReturn(false);
        when(etapaControlRepository.findById(1L)).thenReturn(Optional.of(etapaControlEntity));
        when(etapaControlRepository.save(etapaControlEntity)).thenReturn(etapaControlEntity);

        // === EJECUCION ===
        EtapaControlResponseDTO resultado = etapaControlServicio.modificarEtapaControl(1L, etapaControlFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<EtapaControlEntity> captor = ArgumentCaptor.forClass(EtapaControlEntity.class);
        verify(etapaControlRepository).save(captor.capture());
        EtapaControlEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Control pH Final");
        assertThat(entidadCapturada.getDescripcion()).isEqualTo("Descripción de prueba");
        assertThat(entidadCapturada.getEtapaAControlar()).isEqualTo(TipoEtapa.HERVIDO);

        assertThat(resultado.getNombre()).isEqualTo("Control pH Final");
        assertThat(resultado.getEtapaAControlar()).isEqualTo(TipoEtapa.HERVIDO);
        verify(etapaControlRepository).existsByNombreIgnoreCaseAndEtapaAControlarAndIdNot("Control pH Final", TipoEtapa.HERVIDO, 1L);
        verify(etapaControlRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-MC-05: modificarEtapaControl permite conservar el propio nombre y etapa actuales al actualizar otros campos")
    void modificarEtapaControl_debePermitirConservarNombreYEtapaPropios() {
        EtapaControlEntity etapaControlEntity = crearEtapaControlEntity(1L, "Control pH", "Descripción original", TipoEtapa.HERVIDO);
        // Mismo nombre (distinto case) y misma etapa: el AndIdNot excluye el propio ID y no debe fallar
        EtapaControlFormDTO etapaControlFormDTO = etapaControlFormDTO("control ph", TipoEtapa.HERVIDO);
        when(etapaControlRepository.existsByNombreIgnoreCaseAndEtapaAControlarAndIdNot("control ph", TipoEtapa.HERVIDO, 1L)).thenReturn(false);
        when(etapaControlRepository.findById(1L)).thenReturn(Optional.of(etapaControlEntity));
        when(etapaControlRepository.save(etapaControlEntity)).thenReturn(etapaControlEntity);

        EtapaControlResponseDTO resultado = etapaControlServicio.modificarEtapaControl(1L, etapaControlFormDTO);

        assertThat(resultado.getNombre()).isEqualTo("control ph");
        assertThat(resultado.getEtapaAControlar()).isEqualTo(TipoEtapa.HERVIDO);
        verify(etapaControlRepository).save(etapaControlEntity);
    }

    // ==================== bajaEtapaControl ====================

    @Test
    @DisplayName("CP-BC-01: bajaEtapaControl lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaEtapaControl_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(etapaControlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etapaControlServicio.bajaEtapaControl(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la etapa de control con ID: 99");

        verify(etapaControlRepository).findById(99L);
        verify(etapaControlRepository, never()).existsPlanMonitoreoActivoAsociado(any());
        verify(etapaControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BC-02: bajaEtapaControl lanza ReglaNegocioException y no persiste cuando está asociada a un plan de monitoreo de una receta activa")
    void bajaEtapaControl_debeRechazarConPlanMonitoreoActivoAsociado() {
        EtapaControlEntity etapaControlEntity = crearEtapaControlEntity(1L, "Control Densidad", "Medición de densidad", TipoEtapa.MACERACION);
        when(etapaControlRepository.findById(1L)).thenReturn(Optional.of(etapaControlEntity));
        when(etapaControlRepository.existsPlanMonitoreoActivoAsociado(1L)).thenReturn(true);

        assertThatThrownBy(() -> etapaControlServicio.bajaEtapaControl(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la etapa de control porque está asociada al plan de monitoreo de una receta activa");

        verify(etapaControlRepository).existsPlanMonitoreoActivoAsociado(1L);
        verify(etapaControlRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BC-03: bajaEtapaControl marca el estado como BAJA, persiste y retorna el DTO cuando no tiene planes de monitoreo activos asociados")
    void bajaEtapaControl_debeMarcarBajaYRetornarEtapaControlExistente() {
        EtapaControlEntity etapaControlEntity = crearEtapaControlEntity(1L, "Control Densidad", "Medición de densidad", TipoEtapa.MACERACION);
        when(etapaControlRepository.findById(1L)).thenReturn(Optional.of(etapaControlEntity));
        when(etapaControlRepository.existsPlanMonitoreoActivoAsociado(1L)).thenReturn(false);
        when(etapaControlRepository.save(etapaControlEntity)).thenReturn(etapaControlEntity);

        EtapaControlResponseDTO resultado = etapaControlServicio.bajaEtapaControl(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(etapaControlEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertEtapaControlDTO(etapaControlEntity, resultado);
        verify(etapaControlRepository).findById(1L);
        verify(etapaControlRepository).existsPlanMonitoreoActivoAsociado(1L);
        verify(etapaControlRepository).save(etapaControlEntity);
        verify(etapaControlRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static EtapaControlEntity crearEtapaControlEntity(Long id, String nombre, String descripcion, TipoEtapa etapaAControlar) {
        return EtapaControlEntity.builder()
                .id(id)
                .nombre(nombre)
                .descripcion(descripcion)
                .etapaAControlar(etapaAControlar)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static EtapaControlFormDTO etapaControlFormDTO(String nombre, TipoEtapa etapaAControlar) {
        return EtapaControlFormDTO.builder()
                .nombre(nombre)
                .descripcion("Descripción de prueba")
                .etapaAControlar(etapaAControlar)
                .build();
    }

    private static void assertEtapaControlDTO(EtapaControlEntity entidad, EtapaControlResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getDescripcion()).isEqualTo(entidad.getDescripcion());
        assertThat(dto.getEtapaAControlar()).isEqualTo(entidad.getEtapaAControlar());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
