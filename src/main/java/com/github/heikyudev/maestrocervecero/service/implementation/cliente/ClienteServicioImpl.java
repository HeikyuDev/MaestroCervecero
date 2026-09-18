package com.github.heikyudev.maestrocervecero.service.implementation.cliente;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.cliente.IClienteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cliente.ClienteFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.cliente.IClienteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.cliente.MapperCliente;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClienteServicioImpl implements IClienteServicio {

    private final IClienteRepository clienteRepository;
    private final ILocalidadRepository localidadRepository;

    /**
     * Filtra los clientes activos, opcionalmente por nombre, dirección y/o localidad.
     * <p>
     * Los clientes dados de baja son excluidos por la condición {@code estado = 'ACTIVO'}
     * aplicada en el repositorio.
     * </p>
     *
     * @param nombre Texto a buscar dentro del nombre, o {@code null} para no filtrar por él.
     * @param direccion Texto a buscar dentro de la dirección, o {@code null} para no filtrar por ella.
     * @param idLocalidad El ID de la localidad a filtrar, o {@code null} para no filtrar por ella.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link ClienteResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> filtrarClientes(String nombre, String direccion, Long idLocalidad, Pageable pageable) {
        return clienteRepository.filtrarClientes(nombre, direccion, idLocalidad, pageable).map(MapperCliente::toDTO);
    }

    /**
     * Busca y retorna un cliente activo mediante su identificador único.
     *
     * @param id El ID del cliente.
     * @return El cliente correspondiente al ID.
     * @throws RecursoNoEncontradoException Si no existe ningún cliente activo con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        return MapperCliente.toDTO(clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el cliente con ID: " + id)));
    }

    /**
     * Registra un nuevo cliente en el sistema.
     *
     * @param clienteFormDTO Los datos del cliente a registrar.
     * @return El cliente registrado.
     * @throws RecursoDuplicadoException Si ya existe un cliente activo con el mismo correo electrónico o teléfono.
     * @throws RecursoNoEncontradoException Si la localidad referenciada no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.CLIENTE)
    public ClienteResponseDTO altaCliente(ClienteFormDTO clienteFormDTO) {
        // 1. Validar que no exista otro cliente con el mismo correo electrónico o teléfono
        if (clienteRepository.existsByEmailIgnoreCaseOrTelefono(clienteFormDTO.getEmail(), clienteFormDTO.getTelefono())) {
            throw new RecursoDuplicadoException("Ya existe un cliente registrado con el mismo correo electrónico o teléfono");
        }

        // 2. Validar que la localidad esté registrada en el sistema
        LocalidadEntity localidadEntity = localidadRepository.findById(clienteFormDTO.getIdLocalidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la localidad con ID: " + clienteFormDTO.getIdLocalidad()));

        // 3. Construir y persistir la entidad, y retornar el DTO de respuesta correspondiente
        ClienteEntity clienteEntity = ClienteEntity.builder()
                .nombre(clienteFormDTO.getNombre())
                .telefono(clienteFormDTO.getTelefono())
                .email(clienteFormDTO.getEmail())
                .direccion(clienteFormDTO.getDireccion())
                .localidad(localidadEntity)
                .estado(Estado.ACTIVO)
                .build();

        return MapperCliente.toDTO(clienteRepository.save(clienteEntity));
    }

    /**
     * Modifica un cliente existente en el sistema.
     *
     * @param id El ID del cliente a modificar.
     * @param clienteFormDTO Los nuevos datos del cliente.
     * @return El cliente modificado.
     * @throws RecursoNoEncontradoException Si no existe un cliente activo con el ID especificado, o si la localidad referenciada no existe.
     * @throws RecursoDuplicadoException Si el nuevo correo electrónico o teléfono ya pertenece a otro cliente activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.CLIENTE)
    public ClienteResponseDTO modificarCliente(Long id, ClienteFormDTO clienteFormDTO) {
        // 1. Localizar el cliente existente. Si no existe, se dispara RecursoNoEncontradoException
        ClienteEntity clienteEntity = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el cliente con ID: " + id));

        // 2. Validar que no exista otro cliente con el mismo correo electrónico o teléfono, excluyendo el propio ID
        if (clienteRepository.existsByEmailIgnoreCaseOrTelefonoAndIdNot(clienteFormDTO.getEmail(), clienteFormDTO.getTelefono(), id)) {
            throw new RecursoDuplicadoException("Ya existe otro cliente registrado con el mismo correo electrónico o teléfono");
        }

        // 3. Validar que la localidad esté registrada en el sistema
        LocalidadEntity localidadEntity = localidadRepository.findById(clienteFormDTO.getIdLocalidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la localidad con ID: " + clienteFormDTO.getIdLocalidad()));

        // 4. Recién si todas las validaciones pasaron, aplicar los cambios sobre la entidad
        clienteEntity.setNombre(clienteFormDTO.getNombre());
        clienteEntity.setTelefono(clienteFormDTO.getTelefono());
        clienteEntity.setEmail(clienteFormDTO.getEmail());
        clienteEntity.setDireccion(clienteFormDTO.getDireccion());
        clienteEntity.setLocalidad(localidadEntity);

        // 5. Persistir la entidad actualizada y retornar el DTO de respuesta correspondiente
        return MapperCliente.toDTO(clienteRepository.save(clienteEntity));
    }

    /**
     * Procesa la baja lógica de un cliente existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al cliente con {@link Estado#BAJA} y persiste el
     * cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarlo.
     * </p>
     *
     * @param id El ID del cliente a dar de baja.
     * @return El cliente dado de baja.
     * @throws RecursoNoEncontradoException Si no existe un cliente activo con el ID especificado.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.CLIENTE)
    public ClienteResponseDTO bajaCliente(Long id) {
        // 1. Localizar el cliente. Si no existe, se dispara RecursoNoEncontradoException
        ClienteEntity clienteEntity = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el cliente con ID: " + id));

        // TODO: validar que el cliente no posea barriles en estado "Despachado" a su nombre
        //  una vez que se implemente el módulo de Barriles / Despacho.

        // 2. Ejecutar la baja lógica y persistir el cambio
        clienteEntity.setEstado(Estado.BAJA);
        clienteRepository.save(clienteEntity);

        // 3. Retornar el DTO del cliente dado de baja
        return MapperCliente.toDTO(clienteEntity);
    }
}
