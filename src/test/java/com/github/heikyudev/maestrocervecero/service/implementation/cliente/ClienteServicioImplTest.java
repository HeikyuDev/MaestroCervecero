package com.github.heikyudev.maestrocervecero.service.implementation.cliente;

import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.cliente.IClienteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cliente.ClienteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
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
class ClienteServicioImplTest {

    @Mock
    private IClienteRepository clienteRepository;

    @Mock
    private ILocalidadRepository localidadRepository;

    @InjectMocks
    private ClienteServicioImpl clienteServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de clientes correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteEntity otroClienteEntity = crearClienteEntity(2L, "María López", "1166778899", "maria@mail.com", "Av. Siempre Viva 742", localidadEntity(2L));
        ClienteEntity tercerClienteEntity = crearClienteEntity(3L, "Carlos Gómez", "1155667788", "carlos@mail.com", "Belgrano 456", localidadEntity(1L));

        // Cuando clienteRepository.findAll(pageable) sea llamado, retorna una página con los clientes activos
        // (el filtrado por estado = ACTIVO ya está resuelto dentro de la consulta del repositorio)
        when(clienteRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(clienteEntity, otroClienteEntity, tercerClienteEntity), pageable, 3));

        // === EJECUCION ===
        Page<ClienteResponseDTO> resultado = clienteServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        assertClienteDTO(clienteEntity, resultado.getContent().get(0));
        assertClienteDTO(otroClienteEntity, resultado.getContent().get(1));
        assertClienteDTO(tercerClienteEntity, resultado.getContent().get(2));
        verify(clienteRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay clientes registrados")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(clienteRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<ClienteResponseDTO> resultado = clienteServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(clienteRepository).findAll(pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del cliente cuando el ID existe")
    void buscarPorId_debeRetornarClienteExistente() {
        // === PREPARACION DE DATOS ===
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));

        // === EJECUCION ===
        ClienteResponseDTO resultado = clienteServicio.buscarPorId(1L);

        // === ASSERTS ===
        assertClienteDTO(clienteEntity, resultado);
        verify(clienteRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para clientes dados de baja
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el cliente con ID: 99");
        verify(clienteRepository).findById(99L);
    }

    // ==================== altaCliente ====================

    @Test
    @DisplayName("CP-AC-01: altaCliente lanza RecursoDuplicadoException y no persiste cuando el correo electrónico ya existe")
    void altaCliente_debeRechazarEmailDuplicado() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.existsByEmailIgnoreCaseOrTelefono("juan@mail.com", "1122334455")).thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.altaCliente(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un cliente registrado con el mismo correo electrónico o teléfono");

        verifyNoInteractions(localidadRepository);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-02: altaCliente lanza RecursoDuplicadoException y no persiste cuando el teléfono ya existe")
    void altaCliente_debeRechazarTelefonoDuplicado() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "nuevo@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.existsByEmailIgnoreCaseOrTelefono("nuevo@mail.com", "1122334455")).thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.altaCliente(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un cliente registrado con el mismo correo electrónico o teléfono");

        verifyNoInteractions(localidadRepository);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-03: altaCliente lanza RecursoDuplicadoException cuando el correo electrónico ya existe con distinto case (case-insensitive)")
    void altaCliente_debeRechazarEmailDuplicadoCaseInsensitive() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "JUAN@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.existsByEmailIgnoreCaseOrTelefono("JUAN@mail.com", "1122334455")).thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.altaCliente(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un cliente registrado con el mismo correo electrónico o teléfono");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-04: altaCliente lanza RecursoNoEncontradoException y no persiste cuando la localidad no existe")
    void altaCliente_debeRechazarLocalidadInexistente() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "nuevo@mail.com", "Calle Falsa 123", 99L);
        when(clienteRepository.existsByEmailIgnoreCaseOrTelefono("nuevo@mail.com", "1122334455")).thenReturn(false);
        when(localidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteServicio.altaCliente(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la localidad con ID: 99");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-05: altaCliente valida el duplicado de correo/teléfono antes que la localidad")
    void altaCliente_debeValidarDuplicadoAntesQueLocalidad() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", 99L);
        when(clienteRepository.existsByEmailIgnoreCaseOrTelefono("juan@mail.com", "1122334455")).thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.altaCliente(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un cliente registrado con el mismo correo electrónico o teléfono");

        verifyNoInteractions(localidadRepository);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-06: altaCliente persiste y retorna el DTO cuando los datos son válidos (camino feliz)")
    void altaCliente_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", 1L);
        LocalidadEntity localidadEntity = localidadEntity(1L);
        when(clienteRepository.existsByEmailIgnoreCaseOrTelefono("juan@mail.com", "1122334455")).thenReturn(false);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(clienteRepository.save(any(ClienteEntity.class))).thenAnswer(invocation -> {
            ClienteEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        ClienteResponseDTO resultado = clienteServicio.altaCliente(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<ClienteEntity> captor = ArgumentCaptor.forClass(ClienteEntity.class);
        verify(clienteRepository).save(captor.capture());
        ClienteEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Juan Pérez");
        assertThat(entidadCapturada.getTelefono()).isEqualTo("1122334455");
        assertThat(entidadCapturada.getEmail()).isEqualTo("juan@mail.com");
        assertThat(entidadCapturada.getDireccion()).isEqualTo("Calle Falsa 123");
        assertThat(entidadCapturada.getLocalidad()).isEqualTo(localidadEntity);
        // El alta siempre debe registrar al cliente como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Juan Pérez");
        verify(clienteRepository).existsByEmailIgnoreCaseOrTelefono("juan@mail.com", "1122334455");
    }

    // ==================== modificarCliente ====================

    @Test
    @DisplayName("CP-MC-01: modificarCliente lanza RecursoNoEncontradoException y no valida duplicación ni localidad cuando el ID no existe")
    void modificarCliente_debeLanzarExcepcionSiNoExiste() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteServicio.modificarCliente(99L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el cliente con ID: 99");

        verify(clienteRepository).findById(99L);
        verify(clienteRepository, never()).existsByEmailIgnoreCaseOrTelefonoAndIdNot(any(), any(), any());
        verifyNoInteractions(localidadRepository);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MC-02: modificarCliente lanza RecursoDuplicadoException y no persiste cuando el correo o teléfono está en uso por otro cliente")
    void modificarCliente_debeRechazarEmailOTelefonoEnUsoPorOtroCliente() {
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "otro@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(clienteRepository.existsByEmailIgnoreCaseOrTelefonoAndIdNot("otro@mail.com", "1122334455", 1L)).thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.modificarCliente(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe otro cliente registrado con el mismo correo electrónico o teléfono");

        verifyNoInteractions(localidadRepository);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MC-03: modificarCliente permite conservar el propio correo electrónico y teléfono actual")
    void modificarCliente_debePermitirConservarEmailYTelefonoPropio() {
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        // Mismo correo (distinto case) y mismo teléfono: el AndIdNot excluye el propio ID y no debe fallar
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "JUAN@mail.com", "Calle Falsa 123", 1L);
        LocalidadEntity localidadEntity = localidadEntity(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(clienteRepository.existsByEmailIgnoreCaseOrTelefonoAndIdNot("JUAN@mail.com", "1122334455", 1L)).thenReturn(false);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(clienteRepository.save(clienteEntity)).thenReturn(clienteEntity);

        ClienteResponseDTO resultado = clienteServicio.modificarCliente(1L, formDTO);

        assertThat(resultado.getEmail()).isEqualTo("JUAN@mail.com");
        verify(clienteRepository).save(clienteEntity);
    }

    @Test
    @DisplayName("CP-MC-04: modificarCliente lanza RecursoNoEncontradoException y no persiste cuando la localidad no existe")
    void modificarCliente_debeRechazarLocalidadInexistente() {
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", 99L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(clienteRepository.existsByEmailIgnoreCaseOrTelefonoAndIdNot("juan@mail.com", "1122334455", 1L)).thenReturn(false);
        when(localidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteServicio.modificarCliente(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la localidad con ID: 99");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MC-05: modificarCliente actualiza los datos y persiste cuando el ID existe y no hay conflictos (camino feliz)")
    void modificarCliente_debeActualizarClienteExistente() {
        // === PREPARACION DE DATOS ===
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez Gómez", "1133445566", "juan.perez@mail.com", "Av. Siempre Viva 742", 2L);
        LocalidadEntity nuevaLocalidadEntity = localidadEntity(2L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(clienteRepository.existsByEmailIgnoreCaseOrTelefonoAndIdNot("juan.perez@mail.com", "1133445566", 1L)).thenReturn(false);
        when(localidadRepository.findById(2L)).thenReturn(Optional.of(nuevaLocalidadEntity));
        when(clienteRepository.save(clienteEntity)).thenReturn(clienteEntity);

        // === EJECUCION ===
        ClienteResponseDTO resultado = clienteServicio.modificarCliente(1L, formDTO);

        // === ASSERTS ===
        ArgumentCaptor<ClienteEntity> captor = ArgumentCaptor.forClass(ClienteEntity.class);
        verify(clienteRepository).save(captor.capture());
        ClienteEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getNombre()).isEqualTo("Juan Pérez Gómez");
        assertThat(entidadCapturada.getTelefono()).isEqualTo("1133445566");
        assertThat(entidadCapturada.getEmail()).isEqualTo("juan.perez@mail.com");
        assertThat(entidadCapturada.getDireccion()).isEqualTo("Av. Siempre Viva 742");
        assertThat(entidadCapturada.getLocalidad()).isEqualTo(nuevaLocalidadEntity);

        assertThat(resultado.getNombre()).isEqualTo("Juan Pérez Gómez");
        verify(clienteRepository).findById(1L);
        verify(clienteRepository).existsByEmailIgnoreCaseOrTelefonoAndIdNot("juan.perez@mail.com", "1133445566", 1L);
    }

    // ==================== bajaCliente ====================

    @Test
    @DisplayName("CP-BC-01: bajaCliente lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaCliente_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteServicio.bajaCliente(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el cliente con ID: 99");

        verify(clienteRepository).findById(99L);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BC-02: bajaCliente marca el estado como BAJA, persiste y retorna el DTO")
    void bajaCliente_debeMarcarBajaYRetornarClienteExistente() {
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(clienteRepository.save(clienteEntity)).thenReturn(clienteEntity);

        ClienteResponseDTO resultado = clienteServicio.bajaCliente(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(clienteEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertClienteDTO(clienteEntity, resultado);
        verify(clienteRepository).findById(1L);
        verify(clienteRepository).save(clienteEntity);
        verify(clienteRepository, never()).delete(any());
    }

    // ==================== helpers ====================

    private static ClienteEntity crearClienteEntity(Long id, String nombre, String telefono, String email, String direccion, LocalidadEntity localidad) {
        return ClienteEntity.builder()
                .id(id)
                .nombre(nombre)
                .telefono(telefono)
                .email(email)
                .direccion(direccion)
                .localidad(localidad)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static LocalidadEntity localidadEntity(Long id) {
        return LocalidadEntity.builder()
                .id(id)
                .nombre("Localidad " + id)
                .codigoPostal("1000")
                .estado(Estado.ACTIVO)
                .build();
    }

    private static ClienteFormDTO clienteFormDTO(String nombre, String telefono, String email, String direccion, Long idLocalidad) {
        return ClienteFormDTO.builder()
                .nombre(nombre)
                .telefono(telefono)
                .email(email)
                .direccion(direccion)
                .idLocalidad(idLocalidad)
                .build();
    }

    private static void assertClienteDTO(ClienteEntity entidad, ClienteResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getTelefono()).isEqualTo(entidad.getTelefono());
        assertThat(dto.getEmail()).isEqualTo(entidad.getEmail());
        assertThat(dto.getDireccion()).isEqualTo(entidad.getDireccion());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
        if (entidad.getLocalidad() != null) {
            assertThat(dto.getLocalidad().getId()).isEqualTo(entidad.getLocalidad().getId());
        }
    }
}
