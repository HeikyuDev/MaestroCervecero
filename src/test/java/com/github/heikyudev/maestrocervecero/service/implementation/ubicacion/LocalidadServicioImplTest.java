package com.github.heikyudev.maestrocervecero.service.implementation.ubicacion;

import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.ProvinciaEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.cliente.IClienteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.proveedor.IVersionProveedorRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.IProvinciaRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.ubicacion.LocalidadFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.ubicacion.LocalidadResponseDTO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalidadServicioImplTest {

    @Mock
    private ILocalidadRepository localidadRepository;

    @Mock
    private IProvinciaRepository provinciaRepository;

    @Mock
    private IVersionProveedorRepository versionProveedorRepository;

    @Mock
    private IClienteRepository clienteRepository;

    @InjectMocks
    private LocalidadServicioImpl localidadServicio;

    // ==================== filtrarLocalidades ====================

    @Test
    @DisplayName("CP-FL-01: filtrarLocalidades retorna una página de localidades correctamente mapeada a DTO cuando se filtra por los 4 criterios")
    void filtrarLocalidades_debeRetornarPaginaMapeadaFiltrandoPorLos4Criterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        when(localidadRepository.filtrarLocalidades("Palermo", "1414", 1L, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(localidadEntity), pageable, 1));

        // === EJECUCION ===
        Page<LocalidadResponseDTO> resultado = localidadServicio.filtrarLocalidades("Palermo", "1414", 1L, 1L, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertLocalidadDTO(localidadEntity, resultado.getContent().get(0));
        verify(localidadRepository).filtrarLocalidades("Palermo", "1414", 1L, 1L, pageable);
    }

    @Test
    @DisplayName("CP-FL-02: filtrarLocalidades propaga los 4 parámetros nulos sin restringir esos criterios")
    void filtrarLocalidades_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        LocalidadEntity otraLocalidadEntity = crearLocalidadEntity(2L, "Belgrano", "1428", provinciaEntity(1L));
        LocalidadEntity terceraLocalidadEntity = crearLocalidadEntity(3L, "Recoleta", "1425", provinciaEntity(1L));
        when(localidadRepository.filtrarLocalidades(null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(localidadEntity, otraLocalidadEntity, terceraLocalidadEntity), pageable, 3));

        // === EJECUCION ===
        Page<LocalidadResponseDTO> resultado = localidadServicio.filtrarLocalidades(null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        verify(localidadRepository).filtrarLocalidades(null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FL-03: filtrarLocalidades retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarLocalidades_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(localidadRepository.filtrarLocalidades("Inexistente", null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<LocalidadResponseDTO> resultado = localidadServicio.filtrarLocalidades("Inexistente", null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(localidadRepository).filtrarLocalidades("Inexistente", null, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO de la localidad cuando el ID existe")
    void buscarPorId_debeRetornarLocalidadExistente() {
        // === PREPARACION DE DATOS ===
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));

        // === EJECUCION ===
        LocalidadResponseDTO resultado = localidadServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertLocalidadDTO(localidadEntity, resultado);
        verify(localidadRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dada de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para localidades dadas de baja
        when(localidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localidadServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la localidad con ID: 99");
        verify(localidadRepository).findById(99L);
    }

    // ==================== altaLocalidad ====================

    @Test
    @DisplayName("CP-AL-01: altaLocalidad lanza RecursoNoEncontradoException y no persiste cuando la provincia no existe")
    void altaLocalidad_debeRechazarProvinciaInexistente() {
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "1414", 99L);
        when(provinciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la provincia con ID: 99");

        verifyNoInteractions(localidadRepository);
    }

    @Test
    @DisplayName("CP-AL-02: altaLocalidad lanza RecursoDuplicadoException y no persiste cuando el nombre ya existe en la misma provincia")
    void altaLocalidad_debeRechazarNombreDuplicadoEnLaMismaProvincia() {
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "1414", 1L);
        ProvinciaEntity provinciaEntity = provinciaEntity(1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaId("Palermo", 1L)).thenReturn(true);

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una localidad con el nombre 'Palermo' para la provincia seleccionada");

        verify(localidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AL-03: altaLocalidad lanza RecursoDuplicadoException cuando el nombre ya existe con distinto case (case-insensitive)")
    void altaLocalidad_debeRechazarNombreDuplicadoCaseInsensitive() {
        LocalidadFormDTO formDTO = localidadFormDTO("palermo", "1414", 1L);
        ProvinciaEntity provinciaEntity = provinciaEntity(1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaId("palermo", 1L)).thenReturn(true);

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una localidad con el nombre 'palermo' para la provincia seleccionada");

        verify(localidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AL-04: altaLocalidad permite un código postal que ya usa otra localidad (el código postal no tiene unicidad)")
    void altaLocalidad_debePermitirCodigoPostalRepetido() {
        LocalidadFormDTO formDTO = localidadFormDTO("Belgrano", "1414", 1L);
        ProvinciaEntity provinciaEntity = provinciaEntity(1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaId("Belgrano", 1L)).thenReturn(false);
        when(localidadRepository.save(any(LocalidadEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalidadResponseDTO resultado = localidadServicio.altaLocalidad(formDTO);

        assertThat(resultado.getCodigoPostal()).isEqualTo("1414");
        verify(localidadRepository).save(any(LocalidadEntity.class));
    }

    @Test
    @DisplayName("CP-AL-05: altaLocalidad valida la provincia antes que el nombre duplicado")
    void altaLocalidad_debeValidarProvinciaAntesQueNombre() {
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "1414", 99L);
        when(provinciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la provincia con ID: 99");

        verifyNoInteractions(localidadRepository);
    }

    @Test
    @DisplayName("CP-AL-06: altaLocalidad persiste y retorna el DTO cuando los datos son válidos (camino feliz)")
    void altaLocalidad_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        LocalidadFormDTO formDTO = localidadFormDTO("Belgrano", "1428", 1L);
        ProvinciaEntity provinciaEntity = provinciaEntity(1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaId("Belgrano", 1L)).thenReturn(false);
        when(localidadRepository.save(any(LocalidadEntity.class))).thenAnswer(invocation -> {
            LocalidadEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        LocalidadResponseDTO resultado = localidadServicio.altaLocalidad(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<LocalidadEntity> captor = ArgumentCaptor.forClass(LocalidadEntity.class);
        verify(localidadRepository).save(captor.capture());
        LocalidadEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Belgrano");
        assertThat(entidadCapturada.getCodigoPostal()).isEqualTo("1428");
        assertThat(entidadCapturada.getProvincia()).isEqualTo(provinciaEntity);
        // El alta siempre debe registrar a la localidad como ACTIVA, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Belgrano");
    }

    @Test
    @DisplayName("CP-AL-07: altaLocalidad lanza ReglaNegocioException y no consulta la BD cuando el nombre supera los 100 caracteres")
    void altaLocalidad_debeRechazarNombreDemasiadoLargo() {
        LocalidadFormDTO formDTO = localidadFormDTO("a".repeat(101), "1414", 1L);

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre no puede superar 100 caracteres");

        verifyNoInteractions(localidadRepository, provinciaRepository);
    }

    @Test
    @DisplayName("CP-AL-08: altaLocalidad lanza ReglaNegocioException y no consulta la BD cuando el código postal supera los 10 caracteres")
    void altaLocalidad_debeRechazarCodigoPostalDemasiadoLargo() {
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "1".repeat(11), 1L);

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El código postal no puede superar 10 caracteres");

        verifyNoInteractions(localidadRepository, provinciaRepository);
    }

    // ==================== modificarLocalidad ====================

    @Test
    @DisplayName("CP-ML-01: modificarLocalidad lanza RecursoNoEncontradoException y no valida nada más cuando el ID no existe")
    void modificarLocalidad_debeLanzarExcepcionSiNoExiste() {
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "1414", 1L);
        when(localidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localidadServicio.modificarLocalidad(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la localidad con ID: 99");

        verify(localidadRepository).findById(99L);
        verifyNoInteractions(provinciaRepository);
        verify(localidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ML-02: modificarLocalidad lanza RecursoNoEncontradoException y no persiste cuando la provincia no existe")
    void modificarLocalidad_debeRechazarProvinciaInexistente() {
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "1414", 99L);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(provinciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localidadServicio.modificarLocalidad(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la provincia con ID: 99");

        verify(localidadRepository, never()).existsByNombreIgnoreCaseAndProvinciaIdAndIdNot(any(), any(), any());
        verify(localidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ML-03: modificarLocalidad lanza RecursoDuplicadoException y no persiste cuando el nombre está en uso por otra localidad de la misma provincia")
    void modificarLocalidad_debeRechazarNombreEnUsoPorOtraLocalidad() {
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        LocalidadFormDTO formDTO = localidadFormDTO("Belgrano", "1414", 1L);
        ProvinciaEntity provinciaEntity = provinciaEntity(1L);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("Belgrano", 1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> localidadServicio.modificarLocalidad(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Belgrano' ya está en uso por otra localidad de la provincia seleccionada");

        verify(localidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ML-04: modificarLocalidad permite un código postal que ya usa otra localidad (el código postal no tiene unicidad)")
    void modificarLocalidad_debePermitirCodigoPostalRepetido() {
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "1428", 1L);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("Palermo", 1L, 1L)).thenReturn(false);
        when(localidadRepository.save(localidadEntity)).thenReturn(localidadEntity);

        LocalidadResponseDTO resultado = localidadServicio.modificarLocalidad(1L, formDTO);

        assertThat(resultado.getCodigoPostal()).isEqualTo("1428");
        verify(localidadRepository).save(localidadEntity);
    }

    @Test
    @DisplayName("CP-ML-05: modificarLocalidad permite conservar el propio nombre y código postal actuales")
    void modificarLocalidad_debePermitirConservarNombreYCodigoPostalPropio() {
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        // Mismo nombre (distinto case) y mismo código postal: el AndIdNot excluye el propio ID y no debe fallar
        LocalidadFormDTO formDTO = localidadFormDTO("palermo", "1414", 1L);
        ProvinciaEntity provinciaEntity = provinciaEntity(1L);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("palermo", 1L, 1L)).thenReturn(false);
        when(localidadRepository.save(localidadEntity)).thenReturn(localidadEntity);

        LocalidadResponseDTO resultado = localidadServicio.modificarLocalidad(1L, formDTO);

        assertThat(resultado.getNombre()).isEqualTo("palermo");
        verify(localidadRepository).save(localidadEntity);
    }

    @Test
    @DisplayName("CP-ML-06: modificarLocalidad actualiza los datos y persiste cuando el ID existe y no hay conflictos (camino feliz)")
    void modificarLocalidad_debeActualizarLocalidadExistente() {
        // === PREPARACION DE DATOS ===
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo Chico", "1425", 2L);
        ProvinciaEntity nuevaProvinciaEntity = provinciaEntity(2L);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(provinciaRepository.findById(2L)).thenReturn(Optional.of(nuevaProvinciaEntity));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("Palermo Chico", 2L, 1L)).thenReturn(false);
        when(localidadRepository.save(localidadEntity)).thenReturn(localidadEntity);

        // === EJECUCION ===
        LocalidadResponseDTO resultado = localidadServicio.modificarLocalidad(1L, formDTO);

        // === ASSERTS ===
        ArgumentCaptor<LocalidadEntity> captor = ArgumentCaptor.forClass(LocalidadEntity.class);
        verify(localidadRepository).save(captor.capture());
        LocalidadEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Palermo Chico");
        assertThat(entidadCapturada.getCodigoPostal()).isEqualTo("1425");
        assertThat(entidadCapturada.getProvincia()).isEqualTo(nuevaProvinciaEntity);

        assertThat(resultado.getNombre()).isEqualTo("Palermo Chico");
        verify(localidadRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-ML-07: modificarLocalidad lanza ReglaNegocioException y no consulta la BD cuando el nombre supera los 100 caracteres")
    void modificarLocalidad_debeRechazarNombreDemasiadoLargo() {
        LocalidadFormDTO formDTO = localidadFormDTO("a".repeat(101), "1414", 1L);

        assertThatThrownBy(() -> localidadServicio.modificarLocalidad(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre no puede superar 100 caracteres");

        verifyNoInteractions(localidadRepository, provinciaRepository);
    }

    @Test
    @DisplayName("CP-ML-08: modificarLocalidad lanza ReglaNegocioException y no consulta la BD cuando el código postal supera los 10 caracteres")
    void modificarLocalidad_debeRechazarCodigoPostalDemasiadoLargo() {
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "1".repeat(11), 1L);

        assertThatThrownBy(() -> localidadServicio.modificarLocalidad(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El código postal no puede superar 10 caracteres");

        verifyNoInteractions(localidadRepository, provinciaRepository);
    }

    // ==================== bajaLocalidad ====================

    @Test
    @DisplayName("CP-BL-01: bajaLocalidad lanza RecursoNoEncontradoException y no consulta dependencias ni persiste cuando el ID no existe")
    void bajaLocalidad_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(localidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localidadServicio.bajaLocalidad(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la localidad con ID: 99");

        verify(localidadRepository).findById(99L);
        verifyNoInteractions(versionProveedorRepository);
        verifyNoInteractions(clienteRepository);
        verify(localidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BL-02: bajaLocalidad lanza ReglaNegocioException y no persiste cuando está asociada a un proveedor activo")
    void bajaLocalidad_debeRechazarConProveedorActivoAsociado() {
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(versionProveedorRepository.existsByLocalidadIdAndEsUltimaVersionTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> localidadServicio.bajaLocalidad(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la localidad porque se encuentra asociada a al menos un proveedor activo");

        verify(versionProveedorRepository).existsByLocalidadIdAndEsUltimaVersionTrue(1L);
        verifyNoInteractions(clienteRepository);
        verify(localidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BL-03: bajaLocalidad lanza ReglaNegocioException y no persiste cuando está asociada a un cliente activo")
    void bajaLocalidad_debeRechazarConClienteActivoAsociado() {
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(versionProveedorRepository.existsByLocalidadIdAndEsUltimaVersionTrue(1L)).thenReturn(false);
        when(clienteRepository.existsByLocalidadId(1L)).thenReturn(true);

        assertThatThrownBy(() -> localidadServicio.bajaLocalidad(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja la localidad porque se encuentra asociada a al menos un cliente activo");

        verify(clienteRepository).existsByLocalidadId(1L);
        verify(localidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BL-04: bajaLocalidad marca el estado como BAJA, persiste y retorna el DTO cuando no tiene proveedores ni clientes asociados")
    void bajaLocalidad_debeMarcarBajaYRetornarLocalidadExistente() {
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(versionProveedorRepository.existsByLocalidadIdAndEsUltimaVersionTrue(1L)).thenReturn(false);
        when(clienteRepository.existsByLocalidadId(1L)).thenReturn(false);
        when(localidadRepository.save(localidadEntity)).thenReturn(localidadEntity);

        LocalidadResponseDTO resultado = localidadServicio.bajaLocalidad(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(localidadEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertLocalidadDTO(localidadEntity, resultado);
        verify(localidadRepository).findById(1L);
        verify(versionProveedorRepository).existsByLocalidadIdAndEsUltimaVersionTrue(1L);
        verify(clienteRepository).existsByLocalidadId(1L);
        verify(localidadRepository).save(localidadEntity);
        verify(localidadRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static LocalidadEntity crearLocalidadEntity(Long id, String nombre, String codigoPostal, ProvinciaEntity provincia) {
        return LocalidadEntity.builder()
                .id(id)
                .nombre(nombre)
                .codigoPostal(codigoPostal)
                .provincia(provincia)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static ProvinciaEntity provinciaEntity(Long id) {
        return ProvinciaEntity.builder()
                .id(id)
                .nombre("Provincia " + id)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static LocalidadFormDTO localidadFormDTO(String nombre, String codigoPostal, Long idProvincia) {
        return LocalidadFormDTO.builder()
                .nombre(nombre)
                .codigoPostal(codigoPostal)
                .idProvincia(idProvincia)
                .build();
    }

    private static void assertLocalidadDTO(LocalidadEntity entidad, LocalidadResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getCodigoPostal()).isEqualTo(entidad.getCodigoPostal());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }

    // ==================== normalización y unicidad ====================

    @Test
    @DisplayName("CP-AL-09: altaLocalidad guarda el nombre normalizado y consulta la unicidad con ese valor")
    void altaLocalidad_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        LocalidadFormDTO formDTO = localidadFormDTO("  Pilsen   Nacional  ", "1414", 1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaId("Pilsen Nacional", 1L)).thenReturn(false);
        when(localidadRepository.save(any(LocalidadEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        localidadServicio.altaLocalidad(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<LocalidadEntity> captor = ArgumentCaptor.forClass(LocalidadEntity.class);
        verify(localidadRepository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(formDTO.getNombre()).isEqualTo("  Pilsen   Nacional  ");
        verify(localidadRepository).existsByNombreIgnoreCaseAndProvinciaId("Pilsen Nacional", 1L);
        verify(localidadRepository).flush();
    }

    @Test
    @DisplayName("CP-AL-10: altaLocalidad lanza ReglaNegocioException y no consulta el repositorio cuando el nombre está vacío")
    void altaLocalidad_debeRechazarValorVacio() {
        LocalidadFormDTO formDTO = localidadFormDTO("   ", "1414", 1L);

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(localidadRepository);
    }

    @Test
    @DisplayName("CP-AL-11: altaLocalidad lanza RecursoDuplicadoException cuando el nombre normalizado ya existe")
    void altaLocalidad_debeDetectarDuplicadoConElValorNormalizado() {
        LocalidadFormDTO formDTO = localidadFormDTO("  pilsen ", "1414", 1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaId("pilsen", 1L)).thenReturn(true);

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una localidad con el nombre 'pilsen' para la provincia seleccionada");

        verify(localidadRepository).existsByNombreIgnoreCaseAndProvinciaId("pilsen", 1L);
        verify(localidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AL-12: altaLocalidad traduce la violación del índice único udx_localidad_nombre al mensaje de duplicado")
    void altaLocalidad_debeTraducirLaViolacionDelIndiceUnico() {
        LocalidadFormDTO formDTO = localidadFormDTO("Pilsen Nacional", "1414", 1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaId("Pilsen Nacional", 1L)).thenReturn(false);
        when(localidadRepository.save(any(LocalidadEntity.class))).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_localidad_nombre"));

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una localidad con el nombre 'Pilsen Nacional' para la provincia seleccionada");
    }

    @Test
    @DisplayName("CP-AL-13: altaLocalidad permite reutilizar el nombre de un registro dado de baja")
    void altaLocalidad_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        LocalidadFormDTO formDTO = localidadFormDTO("Pilsen Nacional", "1414", 1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        // El único registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaId("Pilsen Nacional", 1L)).thenReturn(false);
        when(localidadRepository.save(any(LocalidadEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> localidadServicio.altaLocalidad(formDTO)).doesNotThrowAnyException();

        verify(localidadRepository).save(any(LocalidadEntity.class));
    }

    @Test
    @DisplayName("CP-AL-14: altaLocalidad permite un nombre que difiere de otro solo en una tilde")
    void altaLocalidad_debeTratarComoDistintosLosNombresConDistintaTilde() {
        LocalidadFormDTO formDTO = localidadFormDTO("Pílsen", "1414", 1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaId("Pílsen", 1L)).thenReturn(false);
        when(localidadRepository.save(any(LocalidadEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> localidadServicio.altaLocalidad(formDTO)).doesNotThrowAnyException();

        verify(localidadRepository).existsByNombreIgnoreCaseAndProvinciaId("Pílsen", 1L);
        verify(localidadRepository).save(any(LocalidadEntity.class));
    }

    @Test
    @DisplayName("CP-ML-09: modificarLocalidad guarda el nombre normalizado y consulta la unicidad con ese valor")
    void modificarLocalidad_debeGuardarElValorNormalizado() {
        // === PREPARACION DE DATOS ===
        LocalidadFormDTO formDTO = localidadFormDTO("  Pilsen   Nacional  ", "1414", 1L);
        LocalidadEntity existente = crearLocalidadEntity(1L, "Original", "1414", provinciaEntity(1L));
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("Pilsen Nacional", 1L, 1L)).thenReturn(false);
        when(localidadRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        localidadServicio.modificarLocalidad(1L, formDTO);

        // === ASSERTS ===
        assertThat(existente.getNombre()).isEqualTo("Pilsen Nacional");
        assertThat(formDTO.getNombre()).isEqualTo("  Pilsen   Nacional  ");
        verify(localidadRepository).existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("Pilsen Nacional", 1L, 1L);
        verify(localidadRepository).save(existente);
        verify(localidadRepository).flush();
    }

    @Test
    @DisplayName("CP-ML-10: modificarLocalidad lanza ReglaNegocioException cuando el nombre está vacío")
    void modificarLocalidad_debeRechazarValorVacio() {
        LocalidadFormDTO formDTO = localidadFormDTO("   ", "1414", 1L);

        assertThatThrownBy(() -> localidadServicio.modificarLocalidad(1L, formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre es obligatorio");

        verifyNoInteractions(localidadRepository);
    }

    @Test
    @DisplayName("CP-ML-11: modificarLocalidad lanza RecursoDuplicadoException cuando el nombre normalizado está en uso por otro registro")
    void modificarLocalidad_debeDetectarDuplicadoConElValorNormalizado() {
        LocalidadFormDTO formDTO = localidadFormDTO("  pilsen ", "1414", 1L);
        LocalidadEntity existente = crearLocalidadEntity(1L, "Original", "1414", provinciaEntity(1L));
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("pilsen", 1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> localidadServicio.modificarLocalidad(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'pilsen' ya está en uso por otra localidad de la provincia seleccionada");

        verify(localidadRepository).existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("pilsen", 1L, 1L);
        verify(localidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-ML-12: modificarLocalidad traduce la violación del índice único udx_localidad_nombre al mensaje de duplicado")
    void modificarLocalidad_debeTraducirLaViolacionDelIndiceUnico() {
        LocalidadFormDTO formDTO = localidadFormDTO("Pilsen Nacional", "1414", 1L);
        LocalidadEntity existente = crearLocalidadEntity(1L, "Original", "1414", provinciaEntity(1L));
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("Pilsen Nacional", 1L, 1L)).thenReturn(false);
        when(localidadRepository.save(existente)).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_localidad_nombre"));

        assertThatThrownBy(() -> localidadServicio.modificarLocalidad(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre 'Pilsen Nacional' ya está en uso por otra localidad de la provincia seleccionada");
    }

    @Test
    @DisplayName("CP-ML-13: modificarLocalidad permite reutilizar el nombre de un registro dado de baja")
    void modificarLocalidad_debePermitirReutilizarElValorDeUnRegistroDadoDeBaja() {
        LocalidadFormDTO formDTO = localidadFormDTO("Pilsen Nacional", "1414", 1L);
        LocalidadEntity existente = crearLocalidadEntity(1L, "Original", "1414", provinciaEntity(1L));
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        // El único otro registro con ese valor está dado de baja: la consulta solo considera registros activos
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("Pilsen Nacional", 1L, 1L)).thenReturn(false);
        when(localidadRepository.save(existente)).thenReturn(existente);

        assertThatCode(() -> localidadServicio.modificarLocalidad(1L, formDTO)).doesNotThrowAnyException();

        assertThat(existente.getNombre()).isEqualTo("Pilsen Nacional");
        verify(localidadRepository).save(existente);
    }

    // ==================== normalización y unicidad ====================

    @Test
    @DisplayName("CP-AL-15: altaLocalidad guarda el código postal en mayúsculas y sin espacios en los extremos")
    void altaLocalidad_debeGuardarElCodigoPostalEnMayusculas() {
        LocalidadFormDTO formDTO = localidadFormDTO("Belgrano", "  c1428  ", 1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaId("Belgrano", 1L)).thenReturn(false);
        when(localidadRepository.save(any(LocalidadEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        localidadServicio.altaLocalidad(formDTO);

        ArgumentCaptor<LocalidadEntity> captor = ArgumentCaptor.forClass(LocalidadEntity.class);
        verify(localidadRepository).save(captor.capture());
        assertThat(captor.getValue().getCodigoPostal()).isEqualTo("C1428");
        assertThat(formDTO.getCodigoPostal()).isEqualTo("  c1428  ");
    }

    @Test
    @DisplayName("CP-AL-16: altaLocalidad lanza ReglaNegocioException y no consulta el repositorio cuando el código postal está vacío")
    void altaLocalidad_debeRechazarCodigoPostalVacio() {
        LocalidadFormDTO formDTO = localidadFormDTO("Belgrano", "   ", 1L);

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El código postal es obligatorio");

        verifyNoInteractions(localidadRepository);
    }

    @Test
    @DisplayName("CP-ML-14: modificarLocalidad guarda el código postal en mayúsculas y sin espacios en los extremos")
    void modificarLocalidad_debeGuardarElCodigoPostalEnMayusculas() {
        LocalidadEntity existente = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "  c1428  ", 1L);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity(1L)));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("Palermo", 1L, 1L)).thenReturn(false);
        when(localidadRepository.save(existente)).thenReturn(existente);

        localidadServicio.modificarLocalidad(1L, formDTO);

        assertThat(existente.getCodigoPostal()).isEqualTo("C1428");
        assertThat(formDTO.getCodigoPostal()).isEqualTo("  c1428  ");
    }
}
