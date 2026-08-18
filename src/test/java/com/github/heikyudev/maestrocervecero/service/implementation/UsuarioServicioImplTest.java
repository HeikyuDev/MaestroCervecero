package com.github.heikyudev.maestrocervecero.service.implementation;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.UsuarioFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.UsuarioResponseDTO;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServicioImplTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServicioImpl usuarioServicio;

    @Test
    @DisplayName("findAll retorna una página de usuarios correctamente mapeada a DTO")
    void findAll_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS===

        // Objeto de tipo Pageable el cual tiene la peticion que se le envia a la base de datos
        Pageable pageable = PageRequest.of(0, 10);

        // Objeto de tipo UsuarioEntity el cual se va a retornar en la consulta a la base de datos
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "admin", "hash");

        // Cuando el usuarioRepository.findAll(pageable) sea llamado, se retornara una pagina con el usuarioEntity
        when(usuarioRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(usuarioEntity), pageable, 1));

        // Ejecutamos el metodo que queremos Testear (El mismo retorna un Page<UsuarioResponseDTO>)
        Page<UsuarioResponseDTO> resultado = usuarioServicio.findAll(pageable);

        // === ASSERTS ===

        // 1. Confirmamos que el total de elementos que me devolvio el repositorio es de 1
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        // 2. Confirmamos que el contenido de la pagina es igual al usuarioEntity
        assertUsuarioDTO(usuarioEntity, resultado.getContent().get(0));
        verify(usuarioRepository).findAll(pageable);
    }

    @Test
    @DisplayName("obtenerPorId retorna el DTO del usuario cuando el ID existe")
    void obtenerPorId_debeRetornarUsuarioExistente() {
        // === PREPARACION DE DATOS ===

        // Creo el usuarioEntity que me va a retornar el repository
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "admin", "hash");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));

        // === EJECUCION ===
        UsuarioResponseDTO resultado = usuarioServicio.obtenerPorId(1L);

        // === VERIFICACION ===
        assertUsuarioDTO(usuarioEntity, resultado);

        // Verificamos que se haya llamado al método findById del repository
        verify(usuarioRepository).findById(1L);
    }

    @Test
    @DisplayName("obtenerPorId lanza RecursoNoEncontradoException cuando el ID no existe")
    void obtenerPorId_debeLanzarExcepcionSiNoExiste() {
        // === PREPARACION DE DATOS ===
        when(usuarioRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioServicio.obtenerPorId(3982L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("El usuario no existe");
        verify(usuarioRepository).findById(3982L);
    }

    @Test
    @DisplayName("altaUsuario codifica la contraseña y guarda el usuario cuando el username está disponible")
    void altaUsuario_debeCodificarPasswordYGuardarUsuarioNuevo() {
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("nuevo", "plain");
        when(usuarioRepository.existsByUsername("nuevo")).thenReturn(false);
        when(passwordEncoder.encode("plain")).thenReturn("hashed");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> {
            UsuarioEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        UsuarioResponseDTO resultado = usuarioServicio.altaUsuario(usuarioFormDTO);

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getUsername()).isEqualTo("nuevo");
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        verify(usuarioRepository).existsByUsername("nuevo");
        verify(passwordEncoder).encode("plain");
    }

    @Test
    @DisplayName("altaUsuario lanza RecursoDuplicadoException sin codificar ni guardar cuando el username ya existe")
    void altaUsuario_debeRechazarUsernameDuplicado() {
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("existente", "plain");
        when(usuarioRepository.existsByUsername("existente")).thenReturn(true);

        assertThatThrownBy(() -> usuarioServicio.altaUsuario(usuarioFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class);

        verify(usuarioRepository).existsByUsername("existente");
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarUsuario lanza RecursoNoEncontradoException cuando el ID no existe")
    void modificarUsuario_debeLanzarExcepcionSiNoExiste() {
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("nuevo", "plain");
        when(usuarioRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(3982L, usuarioFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(usuarioRepository).findById(3982L);
        verify(usuarioRepository, never()).existsByUsername(any());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarUsuario lanza RecursoDuplicadoException cuando el nuevo username ya está en uso")
    void modificarUsuario_debeRechazarNuevoUsernameDuplicado() {
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "actual", "old-hash");
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("duplicado", "plain");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.existsByUsername("duplicado")).thenReturn(true);

        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, usuarioFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("El nombre de usuario 'duplicado' ya está en uso.");

        verify(usuarioRepository).existsByUsername("duplicado");
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("modificarUsuario actualiza los datos y re-encripta la contraseña cuando se envía una nueva")
    void modificarUsuario_debeActualizarYReencriptarPasswordCuandoSeEnvia() {
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "actual", "old-hash");
        // El username enviado difiere solo en mayúsculas: no dispara la validación de duplicado
        // (equalsIgnoreCase) pero igual se persiste con el casing nuevo.
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("ACTUAL", "plain");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(passwordEncoder.encode("plain")).thenReturn("new-hash");
        when(usuarioRepository.save(usuarioEntity)).thenReturn(usuarioEntity);

        UsuarioResponseDTO resultado = usuarioServicio.modificarUsuario(1L, usuarioFormDTO);

        assertThat(resultado.getUsername()).isEqualTo("ACTUAL");
        assertThat(usuarioEntity.getPassword()).isEqualTo("new-hash");
        verify(usuarioRepository, never()).existsByUsername(any());
        verify(passwordEncoder).encode("plain");
        verify(usuarioRepository).save(usuarioEntity);
    }

    @Test
    @DisplayName("modificarUsuario no re-encripta la contraseña cuando se envía en blanco")
    void modificarUsuario_noDebeReencriptarPasswordCuandoEsBlanco() {
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "actual", "old-hash");
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("libre", "   ");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.existsByUsername("libre")).thenReturn(false);
        when(usuarioRepository.save(usuarioEntity)).thenReturn(usuarioEntity);

        UsuarioResponseDTO resultado = usuarioServicio.modificarUsuario(1L, usuarioFormDTO);

        assertThat(resultado.getUsername()).isEqualTo("libre");
        assertThat(usuarioEntity.getPassword()).isEqualTo("old-hash");
        verify(usuarioRepository).existsByUsername("libre");
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository).save(usuarioEntity);
    }

    @Test
    @DisplayName("bajaUsuario elimina el usuario y retorna su DTO cuando el ID existe")
    void bajaUsuario_debeEliminarYRetornarUsuarioExistente() {
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "admin", "hash");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));

        UsuarioResponseDTO resultado = usuarioServicio.bajaUsuario(1L);

        assertUsuarioDTO(usuarioEntity, resultado);
        verify(usuarioRepository).findById(1L);
        verify(usuarioRepository).delete(usuarioEntity);
    }

    @Test
    @DisplayName("bajaUsuario lanza RecursoNoEncontradoException y no elimina cuando el ID no existe")
    void bajaUsuario_debeLanzarExcepcionYNoEliminarSiNoExiste() {
        when(usuarioRepository.findById(3982L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioServicio.bajaUsuario(3982L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el usuario con ID: 3982");

        verify(usuarioRepository).findById(3982L);
        verify(usuarioRepository, never()).delete(any());
    }

    private static UsuarioEntity crearUsuarioEntity(Long id, String username, String password) {
        return UsuarioEntity.builder()
                .id(id)
                .username(username)
                .password(password)
                .nombre("Nombre")
                .correo("user@example.com")
                .telefono("123456789")
                .rol(Rol.ADMINISTRADOR)
                .build();
    }

    private static UsuarioFormDTO usuarioFormDTO(String username, String password) {
        return UsuarioFormDTO.builder()
                .username(username)
                .password(password)
                .nombre("Nombre")
                .correo("user@example.com")
                .telefono("123456789")
                .rol(Rol.ADMINISTRADOR)
                .build();
    }

    private static void assertUsuarioDTO(UsuarioEntity entidad, UsuarioResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getUsername()).isEqualTo(entidad.getUsername());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getCorreo()).isEqualTo(entidad.getCorreo());
        assertThat(dto.getTelefono()).isEqualTo(entidad.getTelefono());
        assertThat(dto.getRol()).isEqualTo(entidad.getRol());
    }
}
