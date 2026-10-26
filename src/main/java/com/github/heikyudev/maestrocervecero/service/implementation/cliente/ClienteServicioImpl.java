package com.github.heikyudev.maestrocervecero.service.implementation.cliente;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.cliente.ClienteEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.barril.IDespachoBarrilRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.cliente.IClienteRepository;
import com.github.heikyudev.maestrocervecero.persistence.repository.ubicacion.ILocalidadRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.cliente.ClienteFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.cliente.IClienteServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.cliente.ClienteResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.cliente.MapperCliente;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClienteServicioImpl implements IClienteServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos clientes activos con el mismo correo electrónico.
     */
    private static final String INDICE_EMAIL = "udx_cliente_email";

    private final IClienteRepository clienteRepository;
    private final ILocalidadRepository localidadRepository;
    private final IDespachoBarrilRepository despachoBarrilRepository;

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
        // Los textos buscados se normalizan igual que los guardados: sin espacios de más
        return clienteRepository.filtrarClientes(
                        vacioANulo(NormalizacionTexto.nombrePropio(nombre)),
                        vacioANulo(NormalizacionTexto.nombrePropio(direccion)),
                        idLocalidad, pageable)
                .map(MapperCliente::toDTO);
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
     * <p>
     * Normaliza los datos de texto (el correo electrónico se guarda en minúsculas), valida que sean correctos y
     * que el correo electrónico no esté registrado en otro cliente activo (sin distinguir mayúsculas). El
     * teléfono es opcional y no tiene unicidad.
     * </p>
     *
     * @param clienteFormDTO Los datos del cliente a registrar.
     * @return El cliente registrado.
     * @throws ReglaNegocioException Si el nombre, la dirección o el correo electrónico faltan o superan su longitud máxima, si el correo electrónico no tiene un formato válido, o si el teléfono informado no tiene solo dígitos o supera los 15.
     * @throws RecursoDuplicadoException Si ya existe un cliente activo con el mismo correo electrónico.
     * @throws RecursoNoEncontradoException Si la localidad referenciada no existe.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR, conceptoAuditoria = ConceptoAuditoria.CLIENTE)
    public ClienteResponseDTO altaCliente(ClienteFormDTO clienteFormDTO) {
        // 1. Normalizar los datos de texto sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(clienteFormDTO.getNombre());
        String telefono = NormalizacionTexto.telefono(clienteFormDTO.getTelefono());
        String email = NormalizacionTexto.correo(clienteFormDTO.getEmail());
        String direccion = NormalizacionTexto.nombrePropio(clienteFormDTO.getDireccion());

        // 2. Validar que los datos de texto sean obligatorios, correctos y no superen la longitud máxima
        validarDatosDeTexto(nombre, telefono, email, direccion);

        // 3. Validar que no exista otro cliente con el mismo correo electrónico
        if (clienteRepository.existsByEmailIgnoreCase(email)) {
            throw emailDuplicado(email);
        }

        // 4. Validar que la localidad esté registrada en el sistema
        LocalidadEntity localidadEntity = localidadRepository.findById(clienteFormDTO.getIdLocalidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la localidad con ID: " + clienteFormDTO.getIdLocalidad()));

        // 5. Construir la entidad que se va a almacenar en la base de datos
        ClienteEntity clienteEntity = ClienteEntity.builder()
                .nombre(nombre)
                .telefono(telefono)
                .email(email)
                .direccion(direccion)
                .localidad(localidadEntity)
                .estado(Estado.ACTIVO)
                .build();

        // 6. Persistir la entidad (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y retornar el DTO de respuesta correspondiente
        return MapperCliente.toDTO(GuardadoUnico.guardar(clienteRepository, clienteEntity,
                Map.of(INDICE_EMAIL, () -> emailDuplicado(email))));
    }

    /**
     * Modifica un cliente existente en el sistema.
     * <p>
     * Normaliza y valida los datos de texto antes de consultar la base de datos, y verifica que el correo
     * electrónico no pertenezca a otro cliente activo, permitiendo conservar el propio.
     * </p>
     *
     * @param id El ID del cliente a modificar.
     * @param clienteFormDTO Los nuevos datos del cliente.
     * @return El cliente modificado.
     * @throws ReglaNegocioException Si el nombre, la dirección o el correo electrónico faltan o superan su longitud máxima, si el correo electrónico no tiene un formato válido, o si el teléfono informado no tiene solo dígitos o supera los 15.
     * @throws RecursoNoEncontradoException Si no existe un cliente activo con el ID especificado, o si la localidad referenciada no existe.
     * @throws RecursoDuplicadoException Si el nuevo correo electrónico ya pertenece a otro cliente activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR, conceptoAuditoria = ConceptoAuditoria.CLIENTE)
    public ClienteResponseDTO modificarCliente(Long id, ClienteFormDTO clienteFormDTO) {
        // 1. Normalizar los datos de texto sin modificar el DTO
        String nombre = NormalizacionTexto.nombrePropio(clienteFormDTO.getNombre());
        String telefono = NormalizacionTexto.telefono(clienteFormDTO.getTelefono());
        String email = NormalizacionTexto.correo(clienteFormDTO.getEmail());
        String direccion = NormalizacionTexto.nombrePropio(clienteFormDTO.getDireccion());

        // 2. Validar que los datos de texto sean obligatorios, correctos y no superen la longitud máxima
        validarDatosDeTexto(nombre, telefono, email, direccion);

        // 3. Localizar el cliente existente. Si no existe, se dispara RecursoNoEncontradoException
        ClienteEntity clienteEntity = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el cliente con ID: " + id));

        // 4. Validar que no exista otro cliente con el mismo correo electrónico, excluyendo el propio ID
        if (clienteRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw emailDuplicado(email);
        }

        // 5. Validar que la localidad esté registrada en el sistema
        LocalidadEntity localidadEntity = localidadRepository.findById(clienteFormDTO.getIdLocalidad())
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la localidad con ID: " + clienteFormDTO.getIdLocalidad()));

        // 6. Recién si todas las validaciones pasaron, aplicar los cambios sobre la entidad
        clienteEntity.setNombre(nombre);
        clienteEntity.setTelefono(telefono);
        clienteEntity.setEmail(email);
        clienteEntity.setDireccion(direccion);
        clienteEntity.setLocalidad(localidadEntity);

        // 7. Persistir la entidad actualizada (un pedido simultáneo que viole el índice único se traduce al mismo mensaje)
        //    y retornar el DTO de respuesta correspondiente
        return MapperCliente.toDTO(GuardadoUnico.guardar(clienteRepository, clienteEntity,
                Map.of(INDICE_EMAIL, () -> emailDuplicado(email))));
    }

    /**
     * Procesa la baja lógica de un cliente existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al cliente con {@link Estado#BAJA} y persiste el
     * cambio. A partir de ese momento, todas las consultas del repositorio dejan de encontrarlo.
     * No tiene sentido dar de baja un cliente que todavía tiene un barril despachado a su nombre,
     * por eso se valida esa condición antes de aplicar la baja.
     * </p>
     *
     * @param id El ID del cliente a dar de baja.
     * @return El cliente dado de baja.
     * @throws RecursoNoEncontradoException Si no existe un cliente activo con el ID especificado.
     * @throws ReglaNegocioException Si el cliente tiene al menos un despacho de barril en estado REGISTRADO (un barril actualmente en su poder).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR, conceptoAuditoria = ConceptoAuditoria.CLIENTE)
    public ClienteResponseDTO bajaCliente(Long id) {
        // 1. Localizar el cliente. Si no existe, se dispara RecursoNoEncontradoException
        ClienteEntity clienteEntity = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el cliente con ID: " + id));

        // 2. Validar que el cliente no tenga un barril actualmente en su poder (despacho REGISTRADO
        //    más reciente del barril, que sigue en estado operativo DESPACHADO)
        if (despachoBarrilRepository.clienteTieneBarrilEnSuPoder(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el cliente porque tiene un barril en su poder");
        }

        // 3. Ejecutar la baja lógica y persistir el cambio
        clienteEntity.setEstado(Estado.BAJA);
        clienteRepository.save(clienteEntity);

        // 4. Retornar el DTO del cliente dado de baja
        return MapperCliente.toDTO(clienteEntity);
    }

    /**
     * Valida los datos de texto ya normalizados: el nombre, el correo electrónico y la dirección (obligatorios y
     * de longitud máxima; el correo electrónico, además, con formato válido) y el teléfono (opcional, solo dígitos y
     * de longitud máxima).
     *
     * @throws ReglaNegocioException Si algún dato no cumple su regla.
     */
    private static void validarDatosDeTexto(String nombre, String telefono, String email, String direccion) {
        validarObligatorioYLongitud(nombre, LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El nombre", "obligatorio");
        validarObligatorioYLongitud(email, LimitesTecnicos.LONGITUD_MAXIMA_CORREO, "El correo electrónico", "obligatorio");
        ValidacionesTexto.validarCorreo(email);
        ValidacionesTexto.validarTelefono(telefono);
        validarObligatorioYLongitud(direccion, LimitesTecnicos.LONGITUD_MAXIMA_DIRECCION, "La dirección", "obligatoria");
    }

    private static void validarObligatorioYLongitud(String valor, int maximo, String etiqueta, String adjetivo) {
        if (valor == null || valor.isEmpty()) {
            throw new ReglaNegocioException(etiqueta + " es " + adjetivo);
        }
        ValidacionesNumericas.validarLongitudMaxima(valor, maximo, etiqueta);
    }

    /**
     * Un texto de búsqueda formado solo por espacios equivale a no filtrar por ese criterio.
     */
    private static String vacioANulo(String valor) {
        return (valor == null || valor.isEmpty()) ? null : valor;
    }

    private static RecursoDuplicadoException emailDuplicado(String email) {
        return new RecursoDuplicadoException("Ya existe un cliente con el correo electrónico '" + email + "'");
    }
}
