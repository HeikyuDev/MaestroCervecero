package com.github.heikyudev.maestrocervecero.service.implementation.cliente;

import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cliente.IClienteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cliente.ClienteFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
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
class ClienteServicioImplTest {

    @Mock
    private IClienteRepository clienteRepository;

    @Mock
    private ILocalidadRepository localidadRepository;

    @Mock
    private IDespachoBarrilRepository despachoBarrilRepository;

    @InjectMocks
    private ClienteServicioImpl clienteServicio;

    // ==================== filtrarClientes ====================

    @Test
    @DisplayName("CP-FC-01: filtrarClientes retorna una página de clientes correctamente mapeada a DTO cuando se filtra por nombre, dirección e idLocalidad")
    void filtrarClientes_debeRetornarPaginaMapeadaFiltrandoPorLos3Criterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        when(clienteRepository.filtrarClientes("Juan", "Falsa", 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(clienteEntity), pageable, 1));

        // === EJECUCION ===
        Page<ClienteResponseDTO> resultado = clienteServicio.filtrarClientes("Juan", "Falsa", 1L, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertClienteDTO(clienteEntity, resultado.getContent().get(0));
        verify(clienteRepository).filtrarClientes("Juan", "Falsa", 1L, pageable);
    }

    @Test
    @DisplayName("CP-FC-02: filtrarClientes propaga nombre, dirección e idLocalidad nulos sin restringir esos criterios")
    void filtrarClientes_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteEntity otroClienteEntity = crearClienteEntity(2L, "María López", "1166778899", "maria@mail.com", "Av. Siempre Viva 742", localidadEntity(2L));
        ClienteEntity tercerClienteEntity = crearClienteEntity(3L, "Carlos Gómez", "1155667788", "carlos@mail.com", "Belgrano 456", localidadEntity(1L));
        when(clienteRepository.filtrarClientes(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(clienteEntity, otroClienteEntity, tercerClienteEntity), pageable, 3));

        // === EJECUCION ===
        Page<ClienteResponseDTO> resultado = clienteServicio.filtrarClientes(null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(3);
        verify(clienteRepository).filtrarClientes(null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FC-03: filtrarClientes retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarClientes_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(clienteRepository.filtrarClientes("Inexistente", null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<ClienteResponseDTO> resultado = clienteServicio.filtrarClientes("Inexistente", null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(clienteRepository).filtrarClientes("Inexistente", null, null, pageable);
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
        when(clienteRepository.existsByEmailIgnoreCase("juan@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.altaCliente(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un cliente con el correo electrónico 'juan@mail.com'");

        verifyNoInteractions(localidadRepository);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-02: altaCliente no aplica unicidad al teléfono: otro cliente con el mismo teléfono no impide el alta")
    void altaCliente_noDebeAplicarUnicidadAlTelefono() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "nuevo@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.existsByEmailIgnoreCase("nuevo@mail.com")).thenReturn(false);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(any(ClienteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteResponseDTO resultado = clienteServicio.altaCliente(formDTO);

        assertThat(resultado.getTelefono()).isEqualTo("1122334455");
        verify(clienteRepository).save(any(ClienteEntity.class));
    }

    @Test
    @DisplayName("CP-AC-03: altaCliente lanza RecursoDuplicadoException cuando el correo electrónico normalizado ya existe (distinto case y espacios en los extremos)")
    void altaCliente_debeRechazarEmailDuplicadoConElValorNormalizado() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "  JUAN@mail.com ", "Calle Falsa 123", 1L);
        when(clienteRepository.existsByEmailIgnoreCase("juan@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.altaCliente(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un cliente con el correo electrónico 'juan@mail.com'");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-04: altaCliente lanza RecursoNoEncontradoException y no persiste cuando la localidad no existe")
    void altaCliente_debeRechazarLocalidadInexistente() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "nuevo@mail.com", "Calle Falsa 123", 99L);
        when(clienteRepository.existsByEmailIgnoreCase("nuevo@mail.com")).thenReturn(false);
        when(localidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteServicio.altaCliente(formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la localidad con ID: 99");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-05: altaCliente valida el duplicado de correo electrónico antes que la localidad")
    void altaCliente_debeValidarDuplicadoAntesQueLocalidad() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", 99L);
        when(clienteRepository.existsByEmailIgnoreCase("juan@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.altaCliente(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un cliente con el correo electrónico 'juan@mail.com'");

        verifyNoInteractions(localidadRepository);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AC-06: altaCliente persiste y retorna el DTO cuando los datos son válidos (camino feliz)")
    void altaCliente_debePersistirYRetornarDTOCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", 1L);
        LocalidadEntity localidadEntity = localidadEntity(1L);
        when(clienteRepository.existsByEmailIgnoreCase("juan@mail.com")).thenReturn(false);
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
        verify(clienteRepository).existsByEmailIgnoreCase("juan@mail.com");
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
        verify(clienteRepository, never()).existsByEmailIgnoreCaseAndIdNot(any(), any());
        verifyNoInteractions(localidadRepository);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MC-02: modificarCliente lanza RecursoDuplicadoException y no persiste cuando el correo electrónico está en uso por otro cliente")
    void modificarCliente_debeRechazarEmailEnUsoPorOtroCliente() {
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "otro@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(clienteRepository.existsByEmailIgnoreCaseAndIdNot("otro@mail.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.modificarCliente(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El correo electrónico 'otro@mail.com' ya está en uso por otro cliente");

        verifyNoInteractions(localidadRepository);
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MC-03: modificarCliente no aplica unicidad al teléfono: el teléfono de otro cliente no impide la modificación")
    void modificarCliente_noDebeAplicarUnicidadAlTelefono() {
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1199998888", "juan@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(clienteRepository.existsByEmailIgnoreCaseAndIdNot("juan@mail.com", 1L)).thenReturn(false);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(clienteEntity)).thenReturn(clienteEntity);

        ClienteResponseDTO resultado = clienteServicio.modificarCliente(1L, formDTO);

        assertThat(resultado.getTelefono()).isEqualTo("1199998888");
        verify(clienteRepository).save(clienteEntity);
    }

    @Test
    @DisplayName("CP-MC-04: modificarCliente permite conservar el propio correo electrónico actual con distinta capitalización")
    void modificarCliente_debePermitirConservarEmailPropio() {
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        // Mismo correo (distinto case): el AndIdNot excluye el propio ID y no debe fallar
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "JUAN@mail.com", "Calle Falsa 123", 1L);
        LocalidadEntity localidadEntity = localidadEntity(1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(clienteRepository.existsByEmailIgnoreCaseAndIdNot("juan@mail.com", 1L)).thenReturn(false);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity));
        when(clienteRepository.save(clienteEntity)).thenReturn(clienteEntity);

        ClienteResponseDTO resultado = clienteServicio.modificarCliente(1L, formDTO);

        assertThat(resultado.getEmail()).isEqualTo("juan@mail.com");
        verify(clienteRepository).save(clienteEntity);
    }

    @Test
    @DisplayName("CP-MC-05: modificarCliente lanza RecursoNoEncontradoException y no persiste cuando la localidad no existe")
    void modificarCliente_debeRechazarLocalidadInexistente() {
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", 99L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(clienteRepository.existsByEmailIgnoreCaseAndIdNot("juan@mail.com", 1L)).thenReturn(false);
        when(localidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteServicio.modificarCliente(1L, formDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró la localidad con ID: 99");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MC-06: modificarCliente actualiza los datos y persiste cuando el ID existe y no hay conflictos (camino feliz)")
    void modificarCliente_debeActualizarClienteExistente() {
        // === PREPARACION DE DATOS ===
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez Gómez", "1133445566", "juan.perez@mail.com", "Av. Siempre Viva 742", 2L);
        LocalidadEntity nuevaLocalidadEntity = localidadEntity(2L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(clienteRepository.existsByEmailIgnoreCaseAndIdNot("juan.perez@mail.com", 1L)).thenReturn(false);
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
        verify(clienteRepository).existsByEmailIgnoreCaseAndIdNot("juan.perez@mail.com", 1L);
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
    @DisplayName("CP-BC-02: bajaCliente lanza ReglaNegocioException y no persiste cuando el cliente tiene un barril en su poder")
    void bajaCliente_debeRechazarSiTieneBarrilEnSuPoder() {
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(despachoBarrilRepository.clienteTieneBarrilEnSuPoder(1L)).thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.bajaCliente(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el cliente porque tiene un despacho de barril registrado a su nombre");

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BC-03: bajaCliente marca el estado como BAJA, persiste y retorna el DTO")
    void bajaCliente_debeMarcarBajaYRetornarClienteExistente() {
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        when(despachoBarrilRepository.clienteTieneBarrilEnSuPoder(1L)).thenReturn(false);
        when(clienteRepository.save(clienteEntity)).thenReturn(clienteEntity);

        ClienteResponseDTO resultado = clienteServicio.bajaCliente(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(clienteEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertClienteDTO(clienteEntity, resultado);
        verify(clienteRepository).findById(1L);
        verify(clienteRepository).save(clienteEntity);
        verify(clienteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("CP-BC-04: bajaCliente permite la baja de un cliente con despachos históricos cuyos barriles ya no están en su poder")
    void bajaCliente_debePermitirBajaConDespachoHistoricoSinBarrilEnPoder() {
        ClienteEntity clienteEntity = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteEntity));
        // El cliente tuvo despachos, pero el barril fue devuelto (o despachado luego a otro cliente): la consulta no los cuenta
        when(despachoBarrilRepository.clienteTieneBarrilEnSuPoder(1L)).thenReturn(false);
        when(clienteRepository.save(clienteEntity)).thenReturn(clienteEntity);

        ClienteResponseDTO resultado = clienteServicio.bajaCliente(1L);

        assertThat(clienteEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertClienteDTO(clienteEntity, resultado);
        verify(despachoBarrilRepository).clienteTieneBarrilEnSuPoder(1L);
        verify(clienteRepository).save(clienteEntity);
    }

    @Test
    @DisplayName("CP-BC-05: bajaCliente no consulta los barriles en poder del cliente cuando el cliente no existe")
    void bajaCliente_noDebeConsultarBarrilesSiElClienteNoExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteServicio.bajaCliente(99L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(despachoBarrilRepository);
        verify(clienteRepository, never()).save(any());
    }

    // ==================== normalización, unicidad y formato ====================

    @Test
    @DisplayName("CP-FC-04: filtrarClientes normaliza el nombre y la dirección buscados (espacios extremos e internos) antes de consultar")
    void filtrarClientes_debeNormalizarLosTextosBuscados() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(clienteRepository.filtrarClientes("Juan Pérez", "Calle Falsa", 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        clienteServicio.filtrarClientes("   Juan   Pérez  ", "  Calle   Falsa ", 1L, pageable);

        // === ASSERTS ===
        verify(clienteRepository).filtrarClientes("Juan Pérez", "Calle Falsa", 1L, pageable);
    }

    @Test
    @DisplayName("CP-FC-05: filtrarClientes trata un nombre o una dirección formados solo por espacios como ausencia de filtro")
    void filtrarClientes_debeTratarTextosEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(clienteRepository.filtrarClientes(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        clienteServicio.filtrarClientes("    ", "  ", null, pageable);

        // === ASSERTS ===
        verify(clienteRepository).filtrarClientes(null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-AC-07: altaCliente guarda los datos normalizados y consulta la unicidad del correo con ese valor")
    void altaCliente_debeGuardarLosValoresNormalizados() {
        // === PREPARACION DE DATOS ===
        ClienteFormDTO formDTO = clienteFormDTO("  Juan   Pérez ", " 1122334455 ", "  JUAN@Mail.COM ", "  Calle   Falsa   123 ", 1L);
        when(clienteRepository.existsByEmailIgnoreCase("juan@mail.com")).thenReturn(false);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(any(ClienteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        clienteServicio.altaCliente(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<ClienteEntity> captor = ArgumentCaptor.forClass(ClienteEntity.class);
        verify(clienteRepository).save(captor.capture());
        ClienteEntity guardado = captor.getValue();
        assertThat(guardado.getNombre()).isEqualTo("Juan Pérez");
        assertThat(guardado.getTelefono()).isEqualTo("1122334455");
        assertThat(guardado.getEmail()).isEqualTo("juan@mail.com");
        assertThat(guardado.getDireccion()).isEqualTo("Calle Falsa 123");
        assertThat(formDTO.getEmail()).isEqualTo("  JUAN@Mail.COM ");
        verify(clienteRepository).existsByEmailIgnoreCase("juan@mail.com");
        verify(clienteRepository).flush();
    }

    @Test
    @DisplayName("CP-AC-08: altaCliente lanza ReglaNegocioException y no consulta la BD cuando el nombre, el correo electrónico o la dirección están vacíos")
    void altaCliente_debeRechazarDatosObligatoriosVacios() {
        ClienteFormDTO sinNombre = clienteFormDTO("   ", "1122334455", "juan@mail.com", "Calle Falsa 123", 1L);
        ClienteFormDTO sinEmail = clienteFormDTO("Juan Pérez", "1122334455", "   ", "Calle Falsa 123", 1L);
        ClienteFormDTO sinDireccion = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "   ", 1L);

        assertThatThrownBy(() -> clienteServicio.altaCliente(sinNombre))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El nombre es obligatorio");
        assertThatThrownBy(() -> clienteServicio.altaCliente(sinEmail))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El correo electrónico es obligatorio");
        assertThatThrownBy(() -> clienteServicio.altaCliente(sinDireccion))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("La dirección es obligatoria");

        verifyNoInteractions(clienteRepository, localidadRepository);
    }

    @Test
    @DisplayName("CP-AC-09: altaCliente lanza ReglaNegocioException y no consulta la BD cuando algún dato supera su longitud máxima")
    void altaCliente_debeRechazarTextosDemasiadoLargos() {
        ClienteFormDTO nombreLargo = clienteFormDTO("n".repeat(101), "1122334455", "juan@mail.com", "Calle Falsa 123", 1L);
        ClienteFormDTO emailLargo = clienteFormDTO("Juan Pérez", "1122334455", "e".repeat(256), "Calle Falsa 123", 1L);
        ClienteFormDTO direccionLarga = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "d".repeat(256), 1L);
        ClienteFormDTO telefonoLargo = clienteFormDTO("Juan Pérez", "1".repeat(16), "juan@mail.com", "Calle Falsa 123", 1L);

        assertThatThrownBy(() -> clienteServicio.altaCliente(nombreLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El nombre no puede superar 100 caracteres");
        assertThatThrownBy(() -> clienteServicio.altaCliente(emailLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El correo electrónico no puede superar 255 caracteres");
        assertThatThrownBy(() -> clienteServicio.altaCliente(direccionLarga))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("La dirección no puede superar 255 caracteres");
        assertThatThrownBy(() -> clienteServicio.altaCliente(telefonoLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El teléfono no puede superar 15 dígitos");

        verifyNoInteractions(clienteRepository, localidadRepository);
    }

    @Test
    @DisplayName("CP-AC-10: altaCliente rechaza un teléfono con caracteres que no son dígitos")
    void altaCliente_debeRechazarTelefonoConCaracteresNoNumericos() {
        for (String telefono : new String[]{"11-2233-4455", "+541122334455", "11 2233 4455", "abc123"}) {
            ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", telefono, "juan@mail.com", "Calle Falsa 123", 1L);

            assertThatThrownBy(() -> clienteServicio.altaCliente(formDTO))
                    .as("telefono: '%s'", telefono)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El teléfono solo puede contener dígitos");
        }

        verifyNoInteractions(clienteRepository, localidadRepository);
    }

    @Test
    @DisplayName("CP-AC-11: altaCliente acepta un teléfono nulo o vacío y lo guarda como nulo")
    void altaCliente_debePermitirTelefonoVacio() {
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(any(ClienteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (String telefono : new String[]{null, "", "   "}) {
            ClienteResponseDTO resultado = clienteServicio.altaCliente(clienteFormDTO("Juan Pérez", telefono, "juan@mail.com", "Calle Falsa 123", 1L));

            assertThat(resultado.getTelefono()).as("telefono: '%s'", telefono).isNull();
        }
    }

    @Test
    @DisplayName("CP-AC-12: altaCliente traduce la violación del índice único udx_cliente_email al mensaje de correo electrónico duplicado")
    void altaCliente_debeTraducirLaViolacionDelIndiceUnico() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.existsByEmailIgnoreCase("juan@mail.com")).thenReturn(false);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(any(ClienteEntity.class))).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_cliente_email"));

        assertThatThrownBy(() -> clienteServicio.altaCliente(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un cliente con el correo electrónico 'juan@mail.com'");
    }

    @Test
    @DisplayName("CP-AC-13: altaCliente permite reutilizar el correo electrónico de un cliente dado de baja")
    void altaCliente_debePermitirReutilizarElCorreoDeUnClienteDadoDeBaja() {
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", 1L);
        // El único cliente con ese correo está dado de baja: la consulta solo considera clientes activos
        when(clienteRepository.existsByEmailIgnoreCase("juan@mail.com")).thenReturn(false);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(any(ClienteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> clienteServicio.altaCliente(formDTO)).doesNotThrowAnyException();

        verify(clienteRepository).save(any(ClienteEntity.class));
    }

    @Test
    @DisplayName("CP-AC-14: altaCliente no aplica unicidad al nombre: otro cliente con el mismo nombre no impide el alta")
    void altaCliente_noDebeAplicarUnicidadAlNombre() {
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(any(ClienteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClienteResponseDTO primero = clienteServicio.altaCliente(clienteFormDTO("Juan Pérez", "1122334455", "juan1@mail.com", "Calle Falsa 123", 1L));
        ClienteResponseDTO segundo = clienteServicio.altaCliente(clienteFormDTO("juan pérez", "1122334455", "juan2@mail.com", "Belgrano 456", 1L));

        assertThat(primero.getNombre()).isEqualTo("Juan Pérez");
        assertThat(segundo.getNombre()).isEqualTo("juan pérez");
    }

    @Test
    @DisplayName("CP-MC-07: modificarCliente guarda los datos normalizados y consulta la unicidad del correo con ese valor")
    void modificarCliente_debeGuardarLosValoresNormalizados() {
        // === PREPARACION DE DATOS ===
        ClienteEntity existente = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("  Juan   Pérez   Gómez ", " 1133445566 ", "  NUEVO@Mail.COM ", "  Av.   Siempre   Viva 742 ", 1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByEmailIgnoreCaseAndIdNot("nuevo@mail.com", 1L)).thenReturn(false);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        clienteServicio.modificarCliente(1L, formDTO);

        // === ASSERTS ===
        assertThat(existente.getNombre()).isEqualTo("Juan Pérez Gómez");
        assertThat(existente.getTelefono()).isEqualTo("1133445566");
        assertThat(existente.getEmail()).isEqualTo("nuevo@mail.com");
        assertThat(existente.getDireccion()).isEqualTo("Av. Siempre Viva 742");
        assertThat(formDTO.getEmail()).isEqualTo("  NUEVO@Mail.COM ");
        verify(clienteRepository).existsByEmailIgnoreCaseAndIdNot("nuevo@mail.com", 1L);
        verify(clienteRepository).flush();
    }

    @Test
    @DisplayName("CP-MC-08: modificarCliente lanza ReglaNegocioException y no consulta la BD cuando los datos son vacíos, superan su longitud máxima o el teléfono no tiene solo dígitos")
    void modificarCliente_debeRechazarDatosInvalidos() {
        ClienteFormDTO sinNombre = clienteFormDTO("   ", "1122334455", "juan@mail.com", "Calle Falsa 123", 1L);
        ClienteFormDTO sinEmail = clienteFormDTO("Juan Pérez", "1122334455", "   ", "Calle Falsa 123", 1L);
        ClienteFormDTO sinDireccion = clienteFormDTO("Juan Pérez", "1122334455", "juan@mail.com", "   ", 1L);
        ClienteFormDTO emailLargo = clienteFormDTO("Juan Pérez", "1122334455", "e".repeat(256), "Calle Falsa 123", 1L);
        ClienteFormDTO telefonoInvalido = clienteFormDTO("Juan Pérez", "11-2233-4455", "juan@mail.com", "Calle Falsa 123", 1L);

        assertThatThrownBy(() -> clienteServicio.modificarCliente(1L, sinNombre))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El nombre es obligatorio");
        assertThatThrownBy(() -> clienteServicio.modificarCliente(1L, sinEmail))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El correo electrónico es obligatorio");
        assertThatThrownBy(() -> clienteServicio.modificarCliente(1L, sinDireccion))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("La dirección es obligatoria");
        assertThatThrownBy(() -> clienteServicio.modificarCliente(1L, emailLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El correo electrónico no puede superar 255 caracteres");
        assertThatThrownBy(() -> clienteServicio.modificarCliente(1L, telefonoInvalido))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El teléfono solo puede contener dígitos");

        verifyNoInteractions(clienteRepository, localidadRepository);
    }

    @Test
    @DisplayName("CP-MC-09: modificarCliente lanza RecursoDuplicadoException cuando el correo electrónico normalizado está en uso por otro cliente")
    void modificarCliente_debeDetectarEmailDuplicadoConElValorNormalizado() {
        ClienteEntity existente = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "  OTRO@Mail.com ", "Calle Falsa 123", 1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByEmailIgnoreCaseAndIdNot("otro@mail.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> clienteServicio.modificarCliente(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El correo electrónico 'otro@mail.com' ya está en uso por otro cliente");

        verify(clienteRepository, never()).save(any());
        assertThat(existente.getEmail()).isEqualTo("juan@mail.com");
    }

    @Test
    @DisplayName("CP-MC-10: modificarCliente traduce la violación del índice único udx_cliente_email al mensaje de correo electrónico en uso")
    void modificarCliente_debeTraducirLaViolacionDelIndiceUnico() {
        ClienteEntity existente = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "otro@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByEmailIgnoreCaseAndIdNot("otro@mail.com", 1L)).thenReturn(false);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(existente)).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_cliente_email"));

        assertThatThrownBy(() -> clienteServicio.modificarCliente(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El correo electrónico 'otro@mail.com' ya está en uso por otro cliente");
    }

    @Test
    @DisplayName("CP-MC-11: modificarCliente permite reutilizar el correo electrónico de un cliente dado de baja")
    void modificarCliente_debePermitirReutilizarElCorreoDeUnClienteDadoDeBaja() {
        ClienteEntity existente = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "1122334455", "baja@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        // El único otro cliente con ese correo está dado de baja: la consulta solo considera clientes activos
        when(clienteRepository.existsByEmailIgnoreCaseAndIdNot("baja@mail.com", 1L)).thenReturn(false);
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(existente)).thenReturn(existente);

        assertThatCode(() -> clienteServicio.modificarCliente(1L, formDTO)).doesNotThrowAnyException();

        assertThat(existente.getEmail()).isEqualTo("baja@mail.com");
    }

    @Test
    @DisplayName("CP-MC-12: modificarCliente acepta un teléfono vacío y lo guarda como nulo")
    void modificarCliente_debePermitirTelefonoVacio() {
        ClienteEntity existente = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("Juan Pérez", "   ", "juan@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(existente)).thenReturn(existente);

        clienteServicio.modificarCliente(1L, formDTO);

        assertThat(existente.getTelefono()).isNull();
        verify(clienteRepository).save(existente);
    }

    @Test
    @DisplayName("CP-MC-13: modificarCliente no aplica unicidad al nombre: conservar o repetir el nombre de otro cliente no impide la modificación")
    void modificarCliente_noDebeAplicarUnicidadAlNombre() {
        ClienteEntity existente = crearClienteEntity(1L, "Juan Pérez", "1122334455", "juan@mail.com", "Calle Falsa 123", localidadEntity(1L));
        ClienteFormDTO formDTO = clienteFormDTO("María López", "1122334455", "juan@mail.com", "Calle Falsa 123", 1L);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(localidadRepository.findById(1L)).thenReturn(Optional.of(localidadEntity(1L)));
        when(clienteRepository.save(existente)).thenReturn(existente);

        clienteServicio.modificarCliente(1L, formDTO);

        assertThat(existente.getNombre()).isEqualTo("María López");
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
