package com.github.heikyudev.maestrocervecero.service.implementation.usuario;

import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesNumericas;
import com.github.heikyudev.maestrocervecero.util.validacion.LimitesTecnicos;
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
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.interfaces.usuario.IUsuarioServicio;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
import com.github.heikyudev.maestrocervecero.util.mapper.usuario.MapperUsuario;
import com.github.heikyudev.maestrocervecero.util.method.MetodosPassword;
import com.github.heikyudev.maestrocervecero.util.persistencia.GuardadoUnico;
import com.github.heikyudev.maestrocervecero.util.validacion.NormalizacionTexto;
import com.github.heikyudev.maestrocervecero.util.validacion.ValidacionesTexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UsuarioServicioImpl implements IUsuarioServicio {

    /**
     * Índice único de la base de datos que garantiza que no haya dos usuarios activos con el mismo nombre de usuario.
     */
    private static final String INDICE_USERNAME = "udx_usuarios_username";

    /**
     * Índice único de la base de datos que garantiza que no haya dos usuarios activos con el mismo correo electrónico.
     */
    private static final String INDICE_CORREO = "udx_usuarios_correo";

    // Inyecto el repositorio gracias a LOMBOK
    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Filtra los usuarios activos, opcionalmente por nombre, apellido, correo electrónico, username y/o rol.
     *
     * @param nombre Texto a buscar dentro del nombre, o {@code null} para no filtrar por él.
     * @param apellido Texto a buscar dentro del apellido, o {@code null} para no filtrar por él.
     * @param correo Texto a buscar dentro del correo electrónico, o {@code null} para no filtrar por él.
     * @param username Texto a buscar dentro del nombre de usuario, o {@code null} para no filtrar por él.
     * @param rol El rol exacto a filtrar, o {@code null} para no filtrar por él.
     * @param pageable Configuración de paginación y ordenamiento.
     * @return {@link Page} que contiene los objetos {@link UsuarioResponseDTO} correspondientes.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponseDTO> filtrarUsuarios(String nombre, String apellido, String correo, String username, Rol rol, Pageable pageable) {
        // Los textos buscados se normalizan igual que los guardados: sin espacios de más, y el nombre de usuario y el correo en minúsculas
        return usuarioRepository.filtrarUsuarios(
                        vacioANulo(NormalizacionTexto.nombrePropio(nombre)),
                        vacioANulo(NormalizacionTexto.nombrePropio(apellido)),
                        vacioANulo(NormalizacionTexto.correo(correo)),
                        vacioANulo(NormalizacionTexto.username(username)),
                        rol, pageable)
                .map(MapperUsuario::toDTO);
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
     * Normaliza los datos de texto (el nombre de usuario y el correo electrónico se guardan en minúsculas),
     * valida que sean correctos y que el nombre de usuario y el correo electrónico no estén registrados en otro
     * usuario activo (sin distinguir mayúsculas). La contraseña nunca se normaliza: se valida tal como se
     * recibe y se guarda encriptada. Registra el evento en la auditoría.
     * </p>
     *
     * @param usuarioFormDTO Objeto DTO que contiene los datos de creación del usuario.
     * @return {@link UsuarioResponseDTO} representativo del usuario guardado en la base de datos.
     * @throws RecursoDuplicadoException Si el username o el correo electrónico provistos ya pertenecen a un usuario activo.
     * @throws ReglaNegocioException Si el nombre de usuario, el nombre, el apellido o el correo electrónico faltan o superan su longitud máxima, si el nombre de usuario tiene caracteres no permitidos, si el correo electrónico no tiene un formato válido, si el teléfono informado no tiene solo dígitos o supera los 15, si la contraseña no fue informada, o si no cumple las reglas de {@link MetodosPassword} (entre 8 y 72 caracteres, sin espacios en blanco).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.CREAR,conceptoAuditoria = ConceptoAuditoria.USUARIO)
    public UsuarioResponseDTO altaUsuario(UsuarioFormDTO usuarioFormDTO) {
        // 1. Normalizar los datos de texto sin modificar el DTO (la contraseña no se normaliza)
        String username = NormalizacionTexto.username(usuarioFormDTO.getUsername());
        String nombre = NormalizacionTexto.nombrePropio(usuarioFormDTO.getNombre());
        String apellido = NormalizacionTexto.nombrePropio(usuarioFormDTO.getApellido());
        String correo = NormalizacionTexto.correo(usuarioFormDTO.getCorreo());
        String telefono = NormalizacionTexto.telefono(usuarioFormDTO.getTelefono());

        // 2. Validar que los datos de texto sean obligatorios, correctos y no superen la longitud máxima
        validarDatosDeTexto(username, nombre, apellido, correo, telefono);

        // 3. Validar si el username ya esta registrado
        if (usuarioRepository.existsByUsername(username)) {
            throw usernameDuplicado(username);
        }

        // 4. Validar si el correo electrónico ya esta registrado
        if (usuarioRepository.existsByCorreo(correo)) {
            throw correoDuplicado(correo);
        }

        // 5. Validar que se haya informado una contraseña y que cumpla las reglas (el encoder no
        //    acepta null ni más de 72 bytes: lanzaría una excepción sin controlar en lugar de una
        //    regla de negocio clara)
        if (usuarioFormDTO.getPassword() == null || usuarioFormDTO.getPassword().isEmpty()) {
            throw new ReglaNegocioException("La contraseña es obligatoria");
        }
        MetodosPassword.validarPassword(usuarioFormDTO.getPassword());

        // 6. Creo la entidad que se va a almacenar en la base de datos
        UsuarioEntity usuarioEntity = UsuarioEntity.builder()
                .username(username)
                .password(passwordEncoder.encode(usuarioFormDTO.getPassword()))
                .nombre(nombre)
                .apellido(apellido)
                .correo(correo)
                .telefono(telefono)
                .rol(usuarioFormDTO.getRol())
                .estado(Estado.ACTIVO)
                .build();

        // 7. Guardo la entidad en la base de datos (un pedido simultáneo que viole un índice único se traduce al mensaje del campo)
        //    y devuelvo el DTO correspondiente
        return MapperUsuario.toDTO(GuardadoUnico.guardar(usuarioRepository, usuarioEntity, Map.of(
                INDICE_USERNAME, () -> usernameDuplicado(username),
                INDICE_CORREO, () -> correoDuplicado(correo))));
    }

    /**
     * Actualiza la información de un usuario existente en la base de datos.
     * <p>
     * Normaliza y valida los datos de texto antes de consultar la base de datos. Si el nombre de usuario
     * fue modificado respecto a su estado actual, valida la no colisión con otros usuarios activos; el correo
     * electrónico se valida excluyendo al propio usuario. Actualiza la contraseña únicamente si se envía un valor
     * no nulo ni vacío, y en ese caso debe cumplir las reglas de {@link MetodosPassword}; la contraseña nunca se
     * normaliza.
     * </p>
     *
     * @param id Identificador clave primaria del usuario a modificar.
     * @param usuarioFormDTO DTO con la información actualizada.
     * @return {@link UsuarioResponseDTO} representativo del usuario con los cambios aplicados.
     * @throws RecursoNoEncontradoException Si no se localiza el usuario por el ID proporcionado.
     * @throws RecursoDuplicadoException Si el nuevo username o el nuevo correo electrónico ya se encuentran asignados a otra cuenta activa.
     * @throws ReglaNegocioException Si el nombre de usuario, el nombre, el apellido o el correo electrónico faltan o superan su longitud máxima, si el nombre de usuario tiene caracteres no permitidos, si el correo electrónico no tiene un formato válido, si el teléfono informado no tiene solo dígitos o supera los 15, o si se envía una contraseña que no cumple las reglas (entre 8 y 72 caracteres, sin espacios en blanco).
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.MODIFICAR,conceptoAuditoria = ConceptoAuditoria.USUARIO)
    public UsuarioResponseDTO modificarUsuario(Long id, UsuarioFormDTO usuarioFormDTO) {
        // 1. Normalizar los datos de texto sin modificar el DTO (la contraseña no se normaliza)
        String username = NormalizacionTexto.username(usuarioFormDTO.getUsername());
        String nombre = NormalizacionTexto.nombrePropio(usuarioFormDTO.getNombre());
        String apellido = NormalizacionTexto.nombrePropio(usuarioFormDTO.getApellido());
        String correo = NormalizacionTexto.correo(usuarioFormDTO.getCorreo());
        String telefono = NormalizacionTexto.telefono(usuarioFormDTO.getTelefono());

        // 2. Validar que los datos de texto sean obligatorios, correctos y no superen la longitud máxima
        validarDatosDeTexto(username, nombre, apellido, correo, telefono);

        UsuarioEntity usuarioEntity = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario con ID: " + id));

        // 3. Se valida duplicación ÚNICAMENTE si el username cambió respecto al actual
        if (!username.equals(NormalizacionTexto.username(usuarioEntity.getUsername()))) {
            if (usuarioRepository.existsByUsername(username)) {
                throw usernameDuplicado(username);
            }
        }

        // 4. Se valida duplicación del correo excluyendo el propio ID, de modo que conservar
        //    el correo actual (incluso con distinto case) no falla contra el mismo registro
        if (usuarioRepository.existsByCorreoAndIdNot(correo, id)) {
            throw correoDuplicado(correo);
        }

        // 5. La contraseña es opcional: vacía (o no enviada) significa "conservar la actual". Si se
        //    envía algo, tiene que ser válida: un texto en blanco NO cuenta como "no enviada", se
        //    rechaza. Se valida antes de tocar la entidad.
        String password = usuarioFormDTO.getPassword();
        boolean cambiaPassword = password != null && !password.isEmpty();
        if (cambiaPassword) {
            MetodosPassword.validarPassword(password);
        }

        usuarioEntity.setUsername(username);
        usuarioEntity.setNombre(nombre);
        usuarioEntity.setApellido(apellido);
        usuarioEntity.setCorreo(correo);
        usuarioEntity.setTelefono(telefono);
        usuarioEntity.setRol(usuarioFormDTO.getRol());

        // 6. La contraseña solo se actualiza si fue enviada, y se encripta
        if (cambiaPassword) {
            usuarioEntity.setPassword(passwordEncoder.encode(password));
        }

        // 7. Persisto la entidad actualizada (un pedido simultáneo que viole un índice único se traduce al mensaje del campo)
        //    y devuelvo el DTO correspondiente
        return MapperUsuario.toDTO(GuardadoUnico.guardar(usuarioRepository, usuarioEntity, Map.of(
                INDICE_USERNAME, () -> usernameDuplicado(username),
                INDICE_CORREO, () -> correoDuplicado(correo))));
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
     * @throws ReglaNegocioException Si el usuario es el único administrador activo.
     */
    @Override
    @Transactional
    @AuditableAction(accion = AccionAuditoria.ELIMINAR,conceptoAuditoria = ConceptoAuditoria.USUARIO)
    public UsuarioResponseDTO bajaUsuario(Long id) {
        // 1. Buscamos el usuario. Si no existe, se dispara RecursoNoEncontradoException
        UsuarioEntity usuarioEntity = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario con ID: " + id));

        // 2. Validar que no sea el único administrador activo: sin él, nadie podría gestionar usuarios
        if (usuarioEntity.getRol() == Rol.ADMINISTRADOR && !usuarioRepository.existsAdministradorActivoDistintoDe(id)) {
            throw new ReglaNegocioException("No se puede dar de baja el usuario porque es el único administrador activo");
        }

        // 3. Ejecutamos la baja lógica: cambiamos el estado y persistimos el cambio
        usuarioEntity.setEstado(Estado.BAJA);
        usuarioRepository.save(usuarioEntity);

        // 4. Retornamos el DTO del usuario dado de baja
        return MapperUsuario.toDTO(usuarioEntity);
    }

    /**
     * Valida los datos de texto ya normalizados: el nombre de usuario (obligatorio, con caracteres permitidos y de
     * longitud máxima), el nombre, el apellido y el correo electrónico (obligatorios y de longitud máxima; el correo
     * electrónico, además, con formato válido) y el teléfono (opcional, solo dígitos y de longitud máxima).
     *
     * @throws ReglaNegocioException Si algún dato no cumple su regla.
     */
    private static void validarDatosDeTexto(String username, String nombre, String apellido, String correo, String telefono) {
        ValidacionesTexto.validarUsername(username);
        ValidacionesNumericas.validarLongitudMaxima(username, LimitesTecnicos.LONGITUD_MAXIMA_USERNAME, "El nombre de usuario");
        validarObligatorioYLongitud(nombre, LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El nombre");
        validarObligatorioYLongitud(apellido, LimitesTecnicos.LONGITUD_MAXIMA_NOMBRE, "El apellido");
        validarObligatorioYLongitud(correo, LimitesTecnicos.LONGITUD_MAXIMA_CORREO, "El correo electrónico");
        ValidacionesTexto.validarCorreo(correo);
        ValidacionesTexto.validarTelefono(telefono);
    }

    private static void validarObligatorioYLongitud(String valor, int maximo, String etiqueta) {
        if (valor == null || valor.isEmpty()) {
            throw new ReglaNegocioException(etiqueta + " es obligatorio");
        }
        ValidacionesNumericas.validarLongitudMaxima(valor, maximo, etiqueta);
    }

    /**
     * Un texto de búsqueda formado solo por espacios equivale a no filtrar por ese criterio.
     */
    private static String vacioANulo(String valor) {
        return (valor == null || valor.isEmpty()) ? null : valor;
    }

    private static RecursoDuplicadoException usernameDuplicado(String username) {
        return new RecursoDuplicadoException("Ya existe un usuario con el nombre de usuario '" + username + "'");
    }

    private static RecursoDuplicadoException correoDuplicado(String correo) {
        return new RecursoDuplicadoException("Ya existe un usuario con el correo electrónico '" + correo + "'");
    }
}
