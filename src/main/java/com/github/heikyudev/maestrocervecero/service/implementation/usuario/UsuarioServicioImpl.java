package com.github.heikyudev.maestrocervecero.service.implementation.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario.UsuarioFormDTO;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.usuario.MapperUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioServicioImpl implements IUsuarioServicio {

    // Inyecto el repositorio gracias a LOMBOK
    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Filtra los usuarios activos, opcionalmente por nombre, correo electrónico, username y/o rol.
     *
     * @param nombre Texto a buscar dentro del nombre, o {@code null} para no filtrar por él.
     * @param correo Texto a buscar dentro del correo electrónico, o {@code null} para no filtrar por él.
     * @param username Texto a buscar dentro del nombre de usuario, o {@code null} para no filtrar por él.
     * @param rol El rol exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link UsuarioResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> filtrarUsuarios(String nombre, String correo, String username, Rol rol, Pageable pageable) {
        return usuarioRepository.filtrarUsuarios(nombre, correo, username, rol, pageable).map(MapperUsuario::toDTO);
    }

    /**
     * Busca y retorna un usuario específico mediante su identificador único.
     *
     * @param id Identificador clave primaria del usuario buscado.
     * @return Objeto {@link UsuarioResponseDTO} con la información del usuario encontrado.
     * @throws RecursoNoEncontradoException Si no existe ningún usuario registrado con el ID especificado.
     */
    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        // 1. Obtengo la entidad de la base de datos y devuelvo el DTO correspondiente
        return MapperUsuario.toDTO(usuarioRepository.findById(id).
                orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario con ID: " + id)));
    }


    /**
     * Registra un nuevo usuario en el sistema.
     * <p>
     * Valida previamente que el nombre de usuario no se encuentre registrado en el repositorio.
     * Aplica encriptación a la contraseña antes de la persistencia y registra el evento en la auditoría.
     * </p>
     *
     * @param usuarioFormDTO Objeto DTO que contiene los datos de creación del usuario.
     * @return {@link UsuarioResponseDTO} representativo del usuario guardado en la base de datos.
     * @throws RecursoDuplicadoException Si el username provisto ya pertenece a un usuario existente.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR,conceptoAuditoria = ConceptoAuditoria.USUARIO)
    public UsuarioResponseDTO altaUsuario(UsuarioFormDTO usuarioFormDTO) {
        // 1. Validar si el username ya esta registrado
        if(usuarioRepository.existsByUsername(usuarioFormDTO.getUsername())){
            throw new RecursoDuplicadoException("El Username ya esta registrado");
        }

        // 2. Creo la entidad que se va a almacenar en la base de datos
        UsuarioEntity usuarioEntity = UsuarioEntity.builder()
                .username(usuarioFormDTO.getUsername())
                .password(passwordEncoder.encode(usuarioFormDTO.getPassword()))
                .nombre(usuarioFormDTO.getNombre())
                .correo(usuarioFormDTO.getCorreo())
                .telefono(usuarioFormDTO.getTelefono())
                .rol(usuarioFormDTO.getRol())
                .estado(Estado.ACTIVO)
                .build();

        // 3. Guardo la entidad en la base de datos y devuelvo el DTO correspondiente
        return MapperUsuario.toDTO(usuarioRepository.save(usuarioEntity));
    }

    /**
     * Actualiza la información de un usuario existente en la base de datos.
     * <p>
     * Si el nombre de usuario fue modificado respecto a su estado actual, valida la no colisión con
     * otros registros preexistentes. Actualiza la contraseña únicamente si se envía un valor no nulo ni vacío.
     * </p>
     *
     * @param id Identificador clave primaria del usuario a modificar.
     * @param usuarioFormDTO DTO con la información actualizada.
     * @return {@link UsuarioResponseDTO} representativo del usuario con los cambios aplicados.
     * @throws RecursoNoEncontradoException Si no se localiza el usuario por el ID proporcionado.
     * @throws RecursoDuplicadoException Si el nuevo username ya se encuentra asignado a otra cuenta.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR,conceptoAuditoria = ConceptoAuditoria.USUARIO)
    public UsuarioResponseDTO modificarUsuario(Long id, UsuarioFormDTO usuarioFormDTO) {

        UsuarioEntity usuarioEntity = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario con ID: " + id));

        // 1. Se valida duplicación ÚNICAMENTE si el username cambió respecto al actual
        if (!usuarioEntity.getUsername().equalsIgnoreCase(usuarioFormDTO.getUsername())) {
            if (usuarioRepository.existsByUsername(usuarioFormDTO.getUsername())) {
                throw new RecursoDuplicadoException("El nombre de usuario '" + usuarioFormDTO.getUsername() + "' ya está en uso.");
            }
        }

        usuarioEntity.setUsername(usuarioFormDTO.getUsername());
        usuarioEntity.setNombre(usuarioFormDTO.getNombre());
        usuarioEntity.setCorreo(usuarioFormDTO.getCorreo());
        usuarioEntity.setTelefono(usuarioFormDTO.getTelefono());
        usuarioEntity.setRol(usuarioFormDTO.getRol());

        // 2. La contraseña solo se actualiza si fue enviada y se encripta
        if (usuarioFormDTO.getPassword() != null && !usuarioFormDTO.getPassword().isBlank()) {
            usuarioEntity.setPassword(passwordEncoder.encode(usuarioFormDTO.getPassword()));
        }

        return MapperUsuario.toDTO(usuarioRepository.save(usuarioEntity));
    }

    /**
     * Procesa la baja lógica de un usuario existente en el sistema.
     * <p>
     * En lugar de eliminar el registro, marca al usuario con {@link Estado#BAJA} y persiste el
     * cambio. A partir de ese momento, todas las consultas del repositorio (incluyendo el login,
     * a través de {@code findUserEntityByUsername}) dejan de encontrarlo.
     * </p>
     *
     * @param id Identificador clave primaria del usuario a dar de baja.
     * @return {@link UsuarioResponseDTO} con los datos del usuario ya marcado como dado de baja.
     * @throws RecursoNoEncontradoException Si el usuario con el ID especificado no existe o ya fue dado de baja.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR,conceptoAuditoria = ConceptoAuditoria.USUARIO)
    public UsuarioResponseDTO bajaUsuario(Long id) {
        // 1. Buscamos el usuario. Si no existe, se dispara RecursoNoEncontradoException
        UsuarioEntity usuarioEntity = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario con ID: " + id));

        // 2. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        usuarioEntity.setEstado(Estado.BAJA);
        usuarioRepository.save(usuarioEntity);

        // 3. Retornamos el DTO del usuario dado de baja
        return MapperUsuario.toDTO(usuarioEntity);
    }
}
