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

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de localidades correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        LocalidadEntity otraLocalidadEntity = crearLocalidadEntity(2L, "Belgrano", "1428", provinciaEntity(1L));
        LocalidadEntity terceraLocalidadEntity = crearLocalidadEntity(3L, "Recoleta", "1425", provinciaEntity(1L));

        // Cuando localidadRepository.findAll(pageable) sea llamado, retorna una página con las localidades activas
        // (el filtrado por estado = ACTIVO ya está resuelto dentro de la consulta del repositorio)
        when(localidadRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(localidadEntity, otraLocalidadEntity, terceraLocalidadEntity), pageable, 3));

        // === EJECUCION ===
        Page<LocalidadResponseDTO> resultado = localidadServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertLocalidadDTO(localidadEntity, resultado.getContent().get(0));
        assertLocalidadDTO(otraLocalidadEntity, resultado.getContent().get(1));
        assertLocalidadDTO(terceraLocalidadEntity, resultado.getContent().get(2));
        verify(localidadRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay localidades registradas")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(localidadRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<LocalidadResponseDTO> resultado = localidadServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(localidadRepository).findAll(pageable);
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
                .hasMessage("La localidad no existe");
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
                .hasMessage("La provincia no existe");

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
        verify(localidadRepository, never()).existsByCodigoPostal(any());
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
    @DisplayName("CP-AL-04: altaLocalidad lanza RecursoDuplicadoException y no persiste cuando el código postal ya existe")
    void altaLocalidad_debeRechazarCodigoPostalDuplicado() {
        LocalidadFormDTO formDTO = localidadFormDTO("Belgrano", "1414", 1L);
        ProvinciaEntity provinciaEntity = provinciaEntity(1L);
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaId("Belgrano", 1L)).thenReturn(false);
        when(localidadRepository.existsByCodigoPostal("1414")).thenReturn(true);

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe una localidad con el código postal '1414'");

        verify(localidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AL-05: altaLocalidad valida la provincia antes que el nombre duplicado")
    void altaLocalidad_debeValidarProvinciaAntesQueNombre() {
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "1414", 99L);
        when(provinciaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localidadServicio.altaLocalidad(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La provincia no existe");

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
        when(localidadRepository.existsByCodigoPostal("1428")).thenReturn(false);
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

    // ==================== modificarLocalidad ====================

    @Test
    @DisplayName("CP-ML-01: modificarLocalidad lanza RecursoNoEncontradoException y no valida nada más cuando el ID no existe")
    void modificarLocalidad_debeLanzarExcepcionSiNoExiste() {
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "1414", 1L);
        when(localidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> localidadServicio.modificarLocalidad(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La localidad no existe");

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
                .hasMessage("La provincia no existe");

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
    @DisplayName("CP-ML-04: modificarLocalidad lanza RecursoDuplicadoException y no persiste cuando el código postal está en uso por otra localidad")
    void modificarLocalidad_debeRechazarCodigoPostalEnUsoPorOtraLocalidad() {
        LocalidadEntity localidadEntity = crearLocalidadEntity(1L, "Palermo", "1414", provinciaEntity(1L));
        LocalidadFormDTO formDTO = localidadFormDTO("Palermo", "1428", 1L);
        ProvinciaEntity provinciaEntity = provinciaEntity(1L);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(provinciaRepository.findById(1L)).thenReturn(Optional.of(provinciaEntity));
        when(localidadRepository.existsByNombreIgnoreCaseAndProvinciaIdAndIdNot("Palermo", 1L, 1L)).thenReturn(false);
        when(localidadRepository.existsByCodigoPostalAndIdNot("1428", 1L)).thenReturn(true);

        assertThatThrownBy(() -> localidadServicio.modificarLocalidad(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El código postal '1428' ya está en uso por otra localidad");

        verify(localidadRepository, never()).save(any());
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
        when(localidadRepository.existsByCodigoPostalAndIdNot("1414", 1L)).thenReturn(false);
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
        when(localidadRepository.existsByCodigoPostalAndIdNot("1425", 1L)).thenReturn(false);
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
}
