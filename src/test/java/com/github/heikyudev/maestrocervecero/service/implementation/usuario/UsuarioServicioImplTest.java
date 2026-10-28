package com.github.heikyudev.maestrocervecero.service.implementation.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario.UsuarioFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.exception.ReglaNegocioException;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
import com.github.heikyudev.maestrocervecero.util.method.MetodosPassword;
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
import org.springframework.security.crypto.password.PasswordEncoder;

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
class UsuarioServicioImplTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private static final String APELLIDO = "Apellido Prueba";

    @InjectMocks
    private UsuarioServicioImpl usuarioServicio;

    // ==================== filtrarUsuarios ====================

    @Test
    @DisplayName("CP-FU-01: filtrarUsuarios retorna una página de usuarios correctamente mapeada a DTO cuando se filtra por nombre, apellido, correo, username y rol")
    void filtrarUsuarios_debeRetornarPaginaMapeadaFiltrandoPorLos4Criterios() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "jperez", "hash1", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioEntity otroUsuarioEntity = crearUsuarioEntity(2L, "jperez2", "hash2", "Juan Perez Hijo", "juan2@mail.com", "2222", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.filtrarUsuarios("Juan", APELLIDO, "juan", "jperez", Rol.OPERARIO_DE_PRODUCCION, pageable))
                .thenReturn(new PageImpl<>(List.of(usuarioEntity, otroUsuarioEntity), pageable, 2));

        // === EJECUCION ===
        Page<UsuarioResponseDTO> resultado = usuarioServicio.filtrarUsuarios("Juan", APELLIDO, "juan", "jperez", Rol.OPERARIO_DE_PRODUCCION, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertUsuarioDTO(usuarioEntity, resultado.getContent().get(0));
        assertUsuarioDTO(otroUsuarioEntity, resultado.getContent().get(1));
        verify(usuarioRepository).filtrarUsuarios("Juan", APELLIDO, "juan", "jperez", Rol.OPERARIO_DE_PRODUCCION, pageable);
    }

    @Test
    @DisplayName("CP-FU-02: filtrarUsuarios propaga nombre, apellido, correo, username y rol nulos sin restringir esos criterios")
    void filtrarUsuarios_debePropagarCriteriosNulos() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash1", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioEntity otroUsuarioEntity = crearUsuarioEntity(2L, "maria", "hash2", "Maria Lopez", "maria@mail.com", "2222", Rol.ADMINISTRADOR);
        when(usuarioRepository.filtrarUsuarios(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(usuarioEntity, otroUsuarioEntity), pageable, 2));

        // === EJECUCION ===
        Page<UsuarioResponseDTO> resultado = usuarioServicio.filtrarUsuarios(null, null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        verify(usuarioRepository).filtrarUsuarios(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-FU-03: filtrarUsuarios retorna una página vacía cuando ningún registro cumple los criterios")
    void filtrarUsuarios_debeRetornarPaginaVaciaSinCoincidencias() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.filtrarUsuarios("Inexistente", null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<UsuarioResponseDTO> resultado = usuarioServicio.filtrarUsuarios("Inexistente", null, null, null, null, pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(usuarioRepository).filtrarUsuarios("Inexistente", null, null, null, null, pageable);
    }

    // ==================== buscarPorId ====================

    @Test
    @DisplayName("CP-BI-01: buscarPorId retorna el DTO del usuario cuando el ID existe")
    void buscarPorId_debeRetornarUsuarioExistente() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash1", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));

        // === EJECUCION ===
        UsuarioResponseDTO resultado = usuarioServicio.buscarPorId(1L);

        // === VERIFICACION ===
        assertUsuarioDTO(usuarioEntity, resultado);
        verify(usuarioRepository).findById(1L);
    }

    @Test
    @DisplayName("CP-BI-02: buscarPorId lanza RecursoNoEncontradoException cuando el ID no existe o está dado de baja")
    void buscarPorId_debeLanzarExcepcionSiNoExiste() {
        // findById filtra por estado = ACTIVO, por lo que devuelve Optional.empty() también para usuarios dados de baja
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioServicio.buscarPorId(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el usuario con ID: 99");
        verify(usuarioRepository).findById(99L);
    }

    // ==================== altaUsuario ====================

    @Test
    @DisplayName("CP-AU-01: altaUsuario lanza RecursoDuplicadoException y no encripta ni persiste cuando el username ya está registrado")
    void altaUsuario_debeRechazarUsernameDuplicado() {
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("juanperez", "123456", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.existsByUsername("juanperez")).thenReturn(true);

        assertThatThrownBy(() -> usuarioServicio.altaUsuario(usuarioFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un usuario con el nombre de usuario 'juanperez'");

        verify(usuarioRepository).existsByUsername("juanperez");
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-02: altaUsuario lanza ReglaNegocioException y no encripta ni persiste cuando la contraseña es nula")
    void altaUsuario_debeRechazarPasswordNula() {
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("carlos_cervecero", null, "Carlos Gomez", "carlos@mail.com", "3333", Rol.GERENTE_DE_PRODUCCION);
        when(usuarioRepository.existsByUsername("carlos_cervecero")).thenReturn(false);

        assertThatThrownBy(() -> usuarioServicio.altaUsuario(usuarioFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La contraseña es obligatoria");

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-03: altaUsuario lanza ReglaNegocioException (contraseña inválida, no 'obligatoria') y no encripta ni persiste cuando la contraseña son solo espacios")
    void altaUsuario_debeRechazarPasswordSoloConEspacios() {
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("carlos_cervecero", "                ", "Carlos Gomez", "carlos@mail.com", "3333", Rol.GERENTE_DE_PRODUCCION);
        when(usuarioRepository.existsByUsername("carlos_cervecero")).thenReturn(false);

        assertThatThrownBy(() -> usuarioServicio.altaUsuario(usuarioFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MetodosPassword.MENSAJE_INVALIDA);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-05: altaUsuario lanza ReglaNegocioException 'obligatoria' cuando la contraseña está vacía")
    void altaUsuario_debeRechazarPasswordVacia() {
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("carlos_cervecero", "", "Carlos Gomez", "carlos@mail.com", "3333", Rol.GERENTE_DE_PRODUCCION);
        when(usuarioRepository.existsByUsername("carlos_cervecero")).thenReturn(false);

        assertThatThrownBy(() -> usuarioServicio.altaUsuario(usuarioFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("La contraseña es obligatoria");

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-06: altaUsuario rechaza una contraseña de menos de 8 caracteres (7 es inválida)")
    void altaUsuario_debeRechazarPasswordCorta() {
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("carlos_cervecero", "abcdefg", "Carlos Gomez", "carlos@mail.com", "3333", Rol.GERENTE_DE_PRODUCCION);
        when(usuarioRepository.existsByUsername("carlos_cervecero")).thenReturn(false);

        assertThatThrownBy(() -> usuarioServicio.altaUsuario(usuarioFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MetodosPassword.MENSAJE_INVALIDA);

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-07: altaUsuario rechaza una contraseña con un espacio en el medio, al inicio o al final")
    void altaUsuario_debeRechazarPasswordConEspacios() {
        when(usuarioRepository.existsByUsername("carlos_cervecero")).thenReturn(false);

        for (String password : new String[]{"clave segura1", " ClaveSegura1", "ClaveSegura1 ", "Clave Segura1"}) {
            UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("carlos_cervecero", password, "Carlos Gomez", "carlos@mail.com", "3333", Rol.GERENTE_DE_PRODUCCION);

            assertThatThrownBy(() -> usuarioServicio.altaUsuario(usuarioFormDTO))
                    .as("password: '%s'", password)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage(MetodosPassword.MENSAJE_INVALIDA);
        }

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-08: altaUsuario acepta contraseñas en los límites válidos (8 y 72 caracteres)")
    void altaUsuario_debeAceptarPasswordEnLosLimites() {
        when(usuarioRepository.existsByUsername("carlos_cervecero")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (String password : new String[]{"a".repeat(8), "a".repeat(72)}) {
            usuarioServicio.altaUsuario(usuarioFormDTO("carlos_cervecero", password, "Carlos Gomez", "carlos@mail.com", "3333", Rol.GERENTE_DE_PRODUCCION));
            verify(passwordEncoder).encode(password);
        }
    }

    @Test
    @DisplayName("CP-AU-09: altaUsuario rechaza una contraseña de más de 72 caracteres, o de más de 72 bytes (límite de BCrypt)")
    void altaUsuario_debeRechazarPasswordDemasiadoLarga() {
        when(usuarioRepository.existsByUsername("carlos_cervecero")).thenReturn(false);

        UsuarioFormDTO masDe72Caracteres = usuarioFormDTO("carlos_cervecero", "a".repeat(73), "Carlos Gomez", "carlos@mail.com", "3333", Rol.GERENTE_DE_PRODUCCION);
        assertThatThrownBy(() -> usuarioServicio.altaUsuario(masDe72Caracteres))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MetodosPassword.MENSAJE_INVALIDA);

        // 40 caracteres válidos, pero 'ñ' ocupa 2 bytes en UTF-8: 80 bytes > 72
        UsuarioFormDTO masDe72Bytes = usuarioFormDTO("carlos_cervecero", "ñ".repeat(40), "Carlos Gomez", "carlos@mail.com", "3333", Rol.GERENTE_DE_PRODUCCION);
        assertThatThrownBy(() -> usuarioServicio.altaUsuario(masDe72Bytes))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("demasiado larga");

        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-04: altaUsuario encripta la contraseña, persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
    void altaUsuario_debePersistirYEncriptarPasswordCuandoDatosSonValidos() {
        // === PREPARACION DE DATOS ===
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("carlos_cervecero", "ClaveSegura123", "Carlos Gomez", "carlos@mail.com", "3333", Rol.GERENTE_DE_PRODUCCION);
        when(usuarioRepository.existsByUsername("carlos_cervecero")).thenReturn(false);
        when(passwordEncoder.encode("ClaveSegura123")).thenReturn("hash-ClaveSegura123");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> {
            UsuarioEntity entidadGuardada = invocation.getArgument(0);
            entidadGuardada.setId(1L);
            return entidadGuardada;
        });

        // === EJECUCION ===
        UsuarioResponseDTO resultado = usuarioServicio.altaUsuario(usuarioFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        UsuarioEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getUsername()).isEqualTo("carlos_cervecero");
        // La contraseña persistida debe ser el hash devuelto por el encoder, nunca el texto plano
        assertThat(entidadCapturada.getPassword()).isEqualTo("hash-ClaveSegura123");
        assertThat(entidadCapturada.getNombre()).isEqualTo("Carlos Gomez");
        assertThat(entidadCapturada.getApellido()).isEqualTo(APELLIDO);
        assertThat(entidadCapturada.getCorreo()).isEqualTo("carlos@mail.com");
        assertThat(entidadCapturada.getTelefono()).isEqualTo("3333");
        assertThat(entidadCapturada.getRol()).isEqualTo(Rol.GERENTE_DE_PRODUCCION);
        // El alta siempre debe registrar al usuario como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);
        // La cuenta debe nacer habilitada, vigente y sin bloqueos: si no, Spring Security rechaza su login
        assertThat(entidadCapturada.isEnabled()).isTrue();
        assertThat(entidadCapturada.isAccountNonExpired()).isTrue();
        assertThat(entidadCapturada.isAccountNonLocked()).isTrue();
        assertThat(entidadCapturada.isCredentialsNonExpired()).isTrue();

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getUsername()).isEqualTo("carlos_cervecero");
        assertThat(resultado.getNombre()).isEqualTo("Carlos Gomez");
        assertThat(resultado.getCorreo()).isEqualTo("carlos@mail.com");
        assertThat(resultado.getTelefono()).isEqualTo("3333");
        assertThat(resultado.getRol()).isEqualTo(Rol.GERENTE_DE_PRODUCCION);
        verify(usuarioRepository).existsByUsername("carlos_cervecero");
        verify(passwordEncoder).encode("ClaveSegura123");
    }

    @Test
    @DisplayName("CP-AU-10: altaUsuario lanza ReglaNegocioException y no consulta la BD cuando algún dato de texto supera su longitud máxima")
    void altaUsuario_debeRechazarTextosDemasiadoLargos() {
        UsuarioFormDTO usernameLargo = usuarioFormDTO("u".repeat(51), "123456", "Juan", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO nombreLargo = usuarioFormDTO("juan", "123456", "n".repeat(101), "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO apellidoLargo = UsuarioFormDTO.builder().username("juan").password("123456").nombre("Juan")
                .apellido("a".repeat(101)).correo("juan@mail.com").telefono("1111").rol(Rol.OPERARIO_DE_PRODUCCION).build();
        UsuarioFormDTO correoLargo = usuarioFormDTO("juan", "123456", "Juan", "c".repeat(256), "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO telefonoLargo = usuarioFormDTO("juan", "123456", "Juan", "juan@mail.com", "1".repeat(16), Rol.OPERARIO_DE_PRODUCCION);

        assertThatThrownBy(() -> usuarioServicio.altaUsuario(usernameLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El nombre de usuario no puede superar 50 caracteres");
        assertThatThrownBy(() -> usuarioServicio.altaUsuario(nombreLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El nombre no puede superar 100 caracteres");
        assertThatThrownBy(() -> usuarioServicio.altaUsuario(apellidoLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El apellido no puede superar 100 caracteres");
        assertThatThrownBy(() -> usuarioServicio.altaUsuario(correoLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El correo electrónico no puede superar 255 caracteres");
        assertThatThrownBy(() -> usuarioServicio.altaUsuario(telefonoLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El teléfono no puede superar 15 dígitos");

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    // ==================== modificarUsuario ====================

    @Test
    @DisplayName("CP-MU-01: modificarUsuario lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void modificarUsuario_debeLanzarExcepcionSiNoExiste() {
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("juan", "123456", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(99L, usuarioFormDTO))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el usuario con ID: 99");

        verify(usuarioRepository).findById(99L);
        // Al no existir la entidad, no debe llegarse a validar la duplicidad de username ni a persistir
        verify(usuarioRepository, never()).existsByUsername(any());
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-MU-02: modificarUsuario lanza RecursoDuplicadoException y no persiste cuando el nuevo username está en uso por otra cuenta")
    void modificarUsuario_debeRechazarUsernameEnUsoPorOtraCuenta() {
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash-actual", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("pedro", "123456", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.existsByUsername("pedro")).thenReturn(true);

        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, usuarioFormDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un usuario con el nombre de usuario 'pedro'");

        verify(usuarioRepository).findById(1L);
        verify(usuarioRepository).existsByUsername("pedro");
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-MU-03: modificarUsuario actualiza username y encripta la nueva contraseña cuando ambos cambian (camino feliz)")
    void modificarUsuario_debeActualizarUsernameYPasswordCuandoAmbosCambian() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash-viejo", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("juan_nuevo", "NuevaClave123", "Juan Perez Nuevo", "juannuevo@mail.com", "2222", Rol.GERENTE_DE_PRODUCCION);
        usuarioFormDTO.setApellido("Apellido Nuevo");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.existsByUsername("juan_nuevo")).thenReturn(false);
        when(passwordEncoder.encode("NuevaClave123")).thenReturn("hash-NuevaClave123");
        when(usuarioRepository.save(usuarioEntity)).thenReturn(usuarioEntity);

        // === EJECUCION ===
        UsuarioResponseDTO resultado = usuarioServicio.modificarUsuario(1L, usuarioFormDTO);

        // === ASSERTS ===
        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        UsuarioEntity entidadCapturada = captor.getValue();
        assertThat(entidadCapturada.getUsername()).isEqualTo("juan_nuevo");
        assertThat(entidadCapturada.getPassword()).isEqualTo("hash-NuevaClave123");
        assertThat(entidadCapturada.getNombre()).isEqualTo("Juan Perez Nuevo");
        assertThat(entidadCapturada.getApellido()).isEqualTo("Apellido Nuevo");
        assertThat(entidadCapturada.getCorreo()).isEqualTo("juannuevo@mail.com");
        assertThat(entidadCapturada.getTelefono()).isEqualTo("2222");
        assertThat(entidadCapturada.getRol()).isEqualTo(Rol.GERENTE_DE_PRODUCCION);

        assertThat(resultado.getUsername()).isEqualTo("juan_nuevo");
        assertThat(resultado.getNombre()).isEqualTo("Juan Perez Nuevo");
        assertThat(resultado.getRol()).isEqualTo(Rol.GERENTE_DE_PRODUCCION);
        verify(usuarioRepository).existsByUsername("juan_nuevo");
        verify(passwordEncoder).encode("NuevaClave123");
    }

    @Test
    @DisplayName("CP-MU-04: modificarUsuario permite conservar el propio username actual y actualiza el resto de los datos")
    void modificarUsuario_debePermitirConservarUsernamePropio() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash-viejo", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("juan", "NuevaClave123", "Juan Perez Actualizado", "juanactualizado@mail.com", "9999", Rol.GERENTE_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(passwordEncoder.encode("NuevaClave123")).thenReturn("hash-NuevaClave123");
        when(usuarioRepository.save(usuarioEntity)).thenReturn(usuarioEntity);

        // === EJECUCION ===
        UsuarioResponseDTO resultado = usuarioServicio.modificarUsuario(1L, usuarioFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getUsername()).isEqualTo("juan");
        assertThat(resultado.getNombre()).isEqualTo("Juan Perez Actualizado");
        assertThat(resultado.getCorreo()).isEqualTo("juanactualizado@mail.com");
        assertThat(resultado.getTelefono()).isEqualTo("9999");
        assertThat(resultado.getRol()).isEqualTo(Rol.GERENTE_DE_PRODUCCION);
        // Al no cambiar el username, no debe consultarse la duplicidad en el repositorio
        verify(usuarioRepository, never()).existsByUsername(any());
        verify(usuarioRepository).save(usuarioEntity);
    }

    @Test
    @DisplayName("CP-MU-05: modificarUsuario permite conservar el propio username actual con distinta capitalización y lo guarda en minúsculas")
    void modificarUsuario_debePermitirConservarUsernamePropioCaseInsensitive() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash-viejo", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        // Mismo username (distinto case): una vez normalizado a minúsculas coincide con el actual y se evita la validación de duplicidad
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("JUAN", "NuevaClave123", "Juan Perez Actualizado", "juanactualizado@mail.com", "9999", Rol.GERENTE_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(passwordEncoder.encode("NuevaClave123")).thenReturn("hash-NuevaClave123");
        when(usuarioRepository.save(usuarioEntity)).thenReturn(usuarioEntity);

        // === EJECUCION ===
        UsuarioResponseDTO resultado = usuarioServicio.modificarUsuario(1L, usuarioFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getUsername()).isEqualTo("juan");
        assertThat(resultado.getNombre()).isEqualTo("Juan Perez Actualizado");
        verify(usuarioRepository, never()).existsByUsername(any());
        verify(usuarioRepository).save(usuarioEntity);
    }

    @Test
    @DisplayName("CP-MU-06: modificarUsuario conserva la contraseña existente y no invoca al encoder cuando la contraseña enviada es nula")
    void modificarUsuario_debeConservarPasswordCuandoEsNula() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash-existente", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("juan", null, "Juan Perez Actualizado", "juanactualizado@mail.com", "9999", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.save(usuarioEntity)).thenReturn(usuarioEntity);

        // === EJECUCION ===
        UsuarioResponseDTO resultado = usuarioServicio.modificarUsuario(1L, usuarioFormDTO);

        // === ASSERTS ===
        assertThat(usuarioEntity.getPassword()).isEqualTo("hash-existente");
        assertThat(resultado.getNombre()).isEqualTo("Juan Perez Actualizado");
        verify(usuarioRepository).save(usuarioEntity);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-MU-07: modificarUsuario conserva la contraseña existente y no invoca al encoder cuando la contraseña enviada está vacía")
    void modificarUsuario_debeConservarPasswordCuandoEstaVacia() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash-existente", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("juan", "", "Juan Perez Actualizado", "juanactualizado@mail.com", "9999", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.save(usuarioEntity)).thenReturn(usuarioEntity);

        // === EJECUCION ===
        UsuarioResponseDTO resultado = usuarioServicio.modificarUsuario(1L, usuarioFormDTO);

        // === ASSERTS ===
        assertThat(usuarioEntity.getPassword()).isEqualTo("hash-existente");
        assertThat(resultado.getNombre()).isEqualTo("Juan Perez Actualizado");
        verify(usuarioRepository).save(usuarioEntity);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-MU-08: modificarUsuario rechaza una contraseña de solo espacios y no modifica nada")
    void modificarUsuario_debeRechazarPasswordSoloConEspacios() {
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash-existente", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("juan", "                ", "Nombre Nuevo", "juanactualizado@mail.com", "9999", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));

        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, usuarioFormDTO))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage(MetodosPassword.MENSAJE_INVALIDA);

        // La validación ocurre antes de tocar la entidad: nada quedó modificado
        assertThat(usuarioEntity.getNombre()).isEqualTo("Juan Perez");
        assertThat(usuarioEntity.getPassword()).isEqualTo("hash-existente");
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-MU-09: modificarUsuario rechaza una contraseña de menos de 8 caracteres o con espacios")
    void modificarUsuario_debeRechazarPasswordInvalida() {
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash-existente", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));

        for (String password : new String[]{"abcdefg", "clave con espacios"}) {
            UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("juan", password, "Nombre Nuevo", "juanactualizado@mail.com", "9999", Rol.OPERARIO_DE_PRODUCCION);

            assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, usuarioFormDTO))
                    .as("password: '%s'", password)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage(MetodosPassword.MENSAJE_INVALIDA);
        }

        assertThat(usuarioEntity.getNombre()).isEqualTo("Juan Perez");
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-MU-10: modificarUsuario lanza ReglaNegocioException y no consulta la BD cuando algún dato de texto supera su longitud máxima")
    void modificarUsuario_debeRechazarTextosDemasiadoLargos() {
        UsuarioFormDTO usernameLargo = usuarioFormDTO("u".repeat(51), "123456", "Juan", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO nombreLargo = usuarioFormDTO("juan", "123456", "n".repeat(101), "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO apellidoLargo = UsuarioFormDTO.builder().username("juan").password("123456").nombre("Juan")
                .apellido("a".repeat(101)).correo("juan@mail.com").telefono("1111").rol(Rol.OPERARIO_DE_PRODUCCION).build();
        UsuarioFormDTO correoLargo = usuarioFormDTO("juan", "123456", "Juan", "c".repeat(256), "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO telefonoLargo = usuarioFormDTO("juan", "123456", "Juan", "juan@mail.com", "1".repeat(16), Rol.OPERARIO_DE_PRODUCCION);

        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, usernameLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El nombre de usuario no puede superar 50 caracteres");
        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, nombreLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El nombre no puede superar 100 caracteres");
        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, apellidoLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El apellido no puede superar 100 caracteres");
        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, correoLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El correo electrónico no puede superar 255 caracteres");
        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, telefonoLargo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El teléfono no puede superar 15 dígitos");

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    // ==================== bajaUsuario ====================

    @Test
    @DisplayName("CP-BU-01: bajaUsuario lanza RecursoNoEncontradoException y no persiste cuando el ID no existe")
    void bajaUsuario_debeLanzarExcepcionYNoPersistirSiNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioServicio.bajaUsuario(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No se encontró el usuario con ID: 99");

        verify(usuarioRepository).findById(99L);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BU-02: bajaUsuario marca el estado como BAJA, persiste y retorna el DTO cuando el ID existe")
    void bajaUsuario_debeMarcarBajaYRetornarUsuarioExistente() {
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash1", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.save(usuarioEntity)).thenReturn(usuarioEntity);

        UsuarioResponseDTO resultado = usuarioServicio.bajaUsuario(1L);

        // La baja es lógica: el estado pasa a BAJA y se persiste con save(), nunca con delete()
        assertThat(usuarioEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertUsuarioDTO(usuarioEntity, resultado);
        verify(usuarioRepository).findById(1L);
        verify(usuarioRepository).save(usuarioEntity);
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    @DisplayName("CP-BU-03: bajaUsuario lanza ReglaNegocioException y no persiste cuando el usuario es el único administrador activo")
    void bajaUsuario_debeRechazarBajaDelUnicoAdministradorActivo() {
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "admin", "hash1", "Admin Root", "admin@mail.com", "1111", Rol.ADMINISTRADOR);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.existsAdministradorActivoDistintoDe(1L)).thenReturn(false);

        assertThatThrownBy(() -> usuarioServicio.bajaUsuario(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("No se puede dar de baja el usuario porque es el único administrador activo");

        assertThat(usuarioEntity.getEstado()).isEqualTo(Estado.ACTIVO);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-BU-04: bajaUsuario permite la baja de un administrador cuando existe otro administrador activo")
    void bajaUsuario_debePermitirBajaDeAdministradorConOtroAdministradorActivo() {
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "admin", "hash1", "Admin Root", "admin@mail.com", "1111", Rol.ADMINISTRADOR);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.existsAdministradorActivoDistintoDe(1L)).thenReturn(true);
        when(usuarioRepository.save(usuarioEntity)).thenReturn(usuarioEntity);

        UsuarioResponseDTO resultado = usuarioServicio.bajaUsuario(1L);

        assertThat(usuarioEntity.getEstado()).isEqualTo(Estado.BAJA);
        assertUsuarioDTO(usuarioEntity, resultado);
        verify(usuarioRepository).existsAdministradorActivoDistintoDe(1L);
        verify(usuarioRepository).save(usuarioEntity);
    }

    @Test
    @DisplayName("CP-BU-05: bajaUsuario no consulta si existen otros administradores cuando el usuario no es administrador")
    void bajaUsuario_noDebeConsultarAdministradoresSiElUsuarioNoEsAdministrador() {
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash1", "Juan Perez", "juan@mail.com", "1111", Rol.GERENTE_DE_COMPRAS);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.save(usuarioEntity)).thenReturn(usuarioEntity);

        usuarioServicio.bajaUsuario(1L);

        assertThat(usuarioEntity.getEstado()).isEqualTo(Estado.BAJA);
        verify(usuarioRepository, never()).existsAdministradorActivoDistintoDe(any());
    }

    // ==================== normalización, unicidad y formato ====================

    @Test
    @DisplayName("CP-FU-04: filtrarUsuarios normaliza los textos buscados (espacios, y nombre de usuario y correo en minúsculas) antes de consultar")
    void filtrarUsuarios_debeNormalizarLosTextosBuscados() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.filtrarUsuarios("Juan Carlos", "Pérez", "juan@mail.com", "jperez", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        usuarioServicio.filtrarUsuarios("   Juan   Carlos ", "  Pérez  ", "  JUAN@Mail.COM ", "  JPerez ", null, pageable);

        // === ASSERTS ===
        verify(usuarioRepository).filtrarUsuarios("Juan Carlos", "Pérez", "juan@mail.com", "jperez", null, pageable);
    }

    @Test
    @DisplayName("CP-FU-05: filtrarUsuarios trata un texto formado solo por espacios como ausencia de filtro")
    void filtrarUsuarios_debeTratarTextosEnBlancoComoSinFiltro() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.filtrarUsuarios(null, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        usuarioServicio.filtrarUsuarios("   ", "  ", " ", "    ", null, pageable);

        // === ASSERTS ===
        verify(usuarioRepository).filtrarUsuarios(null, null, null, null, null, pageable);
    }

    @Test
    @DisplayName("CP-AU-11: altaUsuario guarda los datos normalizados, consulta la unicidad con esos valores y no normaliza la contraseña")
    void altaUsuario_debeGuardarLosValoresNormalizados() {
        // === PREPARACION DE DATOS ===
        UsuarioFormDTO formDTO = UsuarioFormDTO.builder().username("  JPerez ").password("ClaveSegura123")
                .nombre("  Juan   Carlos ").apellido("  Pérez   Gómez ").correo("  JUAN@Mail.COM ").telefono(" 3511234567 ")
                .rol(Rol.GERENTE_DE_COMPRAS).build();
        when(usuarioRepository.existsByUsername("jperez")).thenReturn(false);
        when(passwordEncoder.encode("ClaveSegura123")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        usuarioServicio.altaUsuario(formDTO);

        // === ASSERTS ===
        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        UsuarioEntity guardada = captor.getValue();
        assertThat(guardada.getUsername()).isEqualTo("jperez");
        assertThat(guardada.getNombre()).isEqualTo("Juan Carlos");
        assertThat(guardada.getApellido()).isEqualTo("Pérez Gómez");
        assertThat(guardada.getCorreo()).isEqualTo("juan@mail.com");
        assertThat(guardada.getTelefono()).isEqualTo("3511234567");
        assertThat(formDTO.getUsername()).isEqualTo("  JPerez ");
        verify(usuarioRepository).existsByUsername("jperez");
        verify(passwordEncoder).encode("ClaveSegura123");
        verify(usuarioRepository).flush();
    }

    @Test
    @DisplayName("CP-AU-12: altaUsuario lanza ReglaNegocioException y no consulta la BD cuando el nombre de usuario, el nombre, el apellido o el correo están vacíos")
    void altaUsuario_debeRechazarDatosObligatoriosVacios() {
        UsuarioFormDTO sinUsername = usuarioFormDTO("   ", "ClaveSegura123", "Juan", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO sinNombre = usuarioFormDTO("juan", "ClaveSegura123", "   ", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO sinApellido = UsuarioFormDTO.builder().username("juan").password("ClaveSegura123").nombre("Juan")
                .apellido("  ").correo("juan@mail.com").telefono("1111").rol(Rol.OPERARIO_DE_PRODUCCION).build();
        UsuarioFormDTO sinCorreo = usuarioFormDTO("juan", "ClaveSegura123", "Juan", "   ", "1111", Rol.OPERARIO_DE_PRODUCCION);

        assertThatThrownBy(() -> usuarioServicio.altaUsuario(sinUsername))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El nombre de usuario es obligatorio");
        assertThatThrownBy(() -> usuarioServicio.altaUsuario(sinNombre))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El nombre es obligatorio");
        assertThatThrownBy(() -> usuarioServicio.altaUsuario(sinApellido))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El apellido es obligatorio");
        assertThatThrownBy(() -> usuarioServicio.altaUsuario(sinCorreo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El correo electrónico es obligatorio");

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-13: altaUsuario rechaza un nombre de usuario con espacios internos, tildes u otros caracteres no permitidos")
    void altaUsuario_debeRechazarUsernameConCaracteresNoPermitidos() {
        for (String username : new String[]{"juan perez", "juán", "peña", "juan@perez", "juan/perez"}) {
            UsuarioFormDTO formDTO = usuarioFormDTO(username, "ClaveSegura123", "Juan", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);

            assertThatThrownBy(() -> usuarioServicio.altaUsuario(formDTO))
                    .as("username: '%s'", username)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos, sin espacios");
        }

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-14: altaUsuario acepta un nombre de usuario numérico (DNI) y con punto, guion o guion bajo")
    void altaUsuario_debeAceptarUsernameNumericoYConSeparadores() {
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (String username : new String[]{"12345678", "juan.perez", "juan-perez", "juan_perez"}) {
            usuarioServicio.altaUsuario(usuarioFormDTO(username, "ClaveSegura123", "Juan", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION));
            verify(usuarioRepository).existsByUsername(username);
        }
    }

    @Test
    @DisplayName("CP-AU-15: altaUsuario lanza RecursoDuplicadoException cuando el nombre de usuario normalizado ya está registrado")
    void altaUsuario_debeDetectarUsernameDuplicadoConElValorNormalizado() {
        UsuarioFormDTO formDTO = usuarioFormDTO("  JUANPEREZ ", "ClaveSegura123", "Juan", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.existsByUsername("juanperez")).thenReturn(true);

        assertThatThrownBy(() -> usuarioServicio.altaUsuario(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un usuario con el nombre de usuario 'juanperez'");

        verify(usuarioRepository).existsByUsername("juanperez");
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-16: altaUsuario permite un correo electrónico que ya usa otro usuario activo")
    void altaUsuario_debePermitirUnCorreoCompartidoConOtroUsuario() {
        // === PREPARACION DE DATOS ===
        UsuarioFormDTO formDTO = usuarioFormDTO("juan", "ClaveSegura123", "Juan", "  JUAN@Mail.com ", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.existsByUsername("juan")).thenReturn(false);
        when(passwordEncoder.encode("ClaveSegura123")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // === EJECUCION ===
        UsuarioResponseDTO resultado = usuarioServicio.altaUsuario(formDTO);

        // === ASSERTS ===
        assertThat(resultado.getCorreo()).isEqualTo("juan@mail.com");
        verify(usuarioRepository).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("CP-AU-17: altaUsuario rechaza un teléfono con caracteres que no son dígitos")
    void altaUsuario_debeRechazarTelefonoConCaracteresNoNumericos() {
        for (String telefono : new String[]{"351-123456", "+54351123456", "351 123456", "abc123", "(351)123456"}) {
            UsuarioFormDTO formDTO = usuarioFormDTO("juan", "ClaveSegura123", "Juan", "juan@mail.com", telefono, Rol.OPERARIO_DE_PRODUCCION);

            assertThatThrownBy(() -> usuarioServicio.altaUsuario(formDTO))
                    .as("telefono: '%s'", telefono)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El teléfono solo puede contener dígitos");
        }

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-18: altaUsuario acepta un teléfono nulo o vacío y lo guarda como nulo")
    void altaUsuario_debePermitirTelefonoVacio() {
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        for (String telefono : new String[]{null, "", "   "}) {
            UsuarioResponseDTO resultado = usuarioServicio.altaUsuario(
                    usuarioFormDTO("juan", "ClaveSegura123", "Juan", "juan@mail.com", telefono, Rol.OPERARIO_DE_PRODUCCION));

            assertThat(resultado.getTelefono()).as("telefono: '%s'", telefono).isNull();
        }
    }

    @Test
    @DisplayName("CP-AU-19: altaUsuario no aplica unicidad al teléfono: otro usuario con el mismo teléfono no impide el alta")
    void altaUsuario_noDebeAplicarUnicidadAlTelefono() {
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioResponseDTO primero = usuarioServicio.altaUsuario(usuarioFormDTO("juan", "ClaveSegura123", "Juan", "juan@mail.com", "3511234567", Rol.OPERARIO_DE_PRODUCCION));
        UsuarioResponseDTO segundo = usuarioServicio.altaUsuario(usuarioFormDTO("maria", "ClaveSegura123", "María", "maria@mail.com", "3511234567", Rol.OPERARIO_DE_PRODUCCION));

        assertThat(primero.getTelefono()).isEqualTo("3511234567");
        assertThat(segundo.getTelefono()).isEqualTo("3511234567");
    }

    @Test
    @DisplayName("CP-AU-20: altaUsuario traduce la violación del índice único udx_usuarios_username al mensaje de nombre de usuario duplicado")
    void altaUsuario_debeTraducirLaViolacionDelIndiceDelUsername() {
        UsuarioFormDTO formDTO = usuarioFormDTO("juan", "ClaveSegura123", "Juan", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(passwordEncoder.encode("ClaveSegura123")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_usuarios_username"));

        assertThatThrownBy(() -> usuarioServicio.altaUsuario(formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un usuario con el nombre de usuario 'juan'");
    }

    @Test
    @DisplayName("CP-AU-22: altaUsuario permite reutilizar el nombre de usuario de un usuario dado de baja")
    void altaUsuario_debePermitirReutilizarLosValoresDeUnUsuarioDadoDeBaja() {
        UsuarioFormDTO formDTO = usuarioFormDTO("juan", "ClaveSegura123", "Juan", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        // El único usuario con ese nombre de usuario está dado de baja: la consulta solo considera usuarios activos
        when(usuarioRepository.existsByUsername("juan")).thenReturn(false);
        when(passwordEncoder.encode("ClaveSegura123")).thenReturn("hash");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> usuarioServicio.altaUsuario(formDTO)).doesNotThrowAnyException();

        verify(usuarioRepository).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("CP-MU-11: modificarUsuario guarda los datos normalizados y consulta la unicidad con esos valores")
    void modificarUsuario_debeGuardarLosValoresNormalizados() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity existente = crearUsuarioEntity(1L, "juan", "hash", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO formDTO = UsuarioFormDTO.builder().username("  NuevoUser ").nombre("  Juan   Carlos ").apellido("  Pérez   Gómez ")
                .correo("  NUEVO@Mail.COM ").telefono(" 3511234567 ").rol(Rol.GERENTE_DE_COMPRAS).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByUsername("nuevouser")).thenReturn(false);
        when(usuarioRepository.save(existente)).thenReturn(existente);

        // === EJECUCION ===
        usuarioServicio.modificarUsuario(1L, formDTO);

        // === ASSERTS ===
        assertThat(existente.getUsername()).isEqualTo("nuevouser");
        assertThat(existente.getNombre()).isEqualTo("Juan Carlos");
        assertThat(existente.getApellido()).isEqualTo("Pérez Gómez");
        assertThat(existente.getCorreo()).isEqualTo("nuevo@mail.com");
        assertThat(existente.getTelefono()).isEqualTo("3511234567");
        assertThat(formDTO.getUsername()).isEqualTo("  NuevoUser ");
        verify(usuarioRepository).existsByUsername("nuevouser");
        verify(usuarioRepository).flush();
    }

    @Test
    @DisplayName("CP-MU-12: modificarUsuario lanza ReglaNegocioException y no consulta la BD cuando los datos obligatorios están vacíos o el nombre de usuario tiene caracteres no permitidos")
    void modificarUsuario_debeRechazarDatosObligatoriosVaciosOUsernameInvalido() {
        UsuarioFormDTO sinUsername = usuarioFormDTO("   ", null, "Juan", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO usernameInvalido = usuarioFormDTO("juan perez", null, "Juan", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO sinNombre = usuarioFormDTO("juan", null, "   ", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO sinCorreo = usuarioFormDTO("juan", null, "Juan", "   ", "1111", Rol.OPERARIO_DE_PRODUCCION);

        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, sinUsername))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El nombre de usuario es obligatorio");
        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, usernameInvalido))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("El nombre de usuario solo puede contener letras, números, puntos, guiones y guiones bajos, sin espacios");
        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, sinNombre))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El nombre es obligatorio");
        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, sinCorreo))
                .isInstanceOf(ReglaNegocioException.class).hasMessage("El correo electrónico es obligatorio");

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("CP-MU-13: modificarUsuario lanza RecursoDuplicadoException cuando el nombre de usuario normalizado está en uso por otra cuenta")
    void modificarUsuario_debeDetectarUsernameDuplicadoConElValorNormalizado() {
        UsuarioEntity existente = crearUsuarioEntity(1L, "juan", "hash", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO formDTO = usuarioFormDTO("  PEDRO ", null, "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByUsername("pedro")).thenReturn(true);

        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un usuario con el nombre de usuario 'pedro'");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-MU-14: modificarUsuario permite un correo electrónico que ya usa otro usuario activo")
    void modificarUsuario_debePermitirUnCorreoCompartidoConOtroUsuario() {
        UsuarioEntity existente = crearUsuarioEntity(1L, "juan", "hash", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO formDTO = usuarioFormDTO("juan", null, "Juan Perez", "  OTRO@Mail.com ", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);

        usuarioServicio.modificarUsuario(1L, formDTO);

        assertThat(existente.getCorreo()).isEqualTo("otro@mail.com");
        verify(usuarioRepository).save(existente);
    }

    @Test
    @DisplayName("CP-MU-16: modificarUsuario rechaza un teléfono con caracteres que no son dígitos y no consulta la BD")
    void modificarUsuario_debeRechazarTelefonoConCaracteresNoNumericos() {
        for (String telefono : new String[]{"351-123456", "+54351123456", "abc123"}) {
            UsuarioFormDTO formDTO = usuarioFormDTO("juan", null, "Juan", "juan@mail.com", telefono, Rol.OPERARIO_DE_PRODUCCION);

            assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, formDTO))
                    .as("telefono: '%s'", telefono)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El teléfono solo puede contener dígitos");
        }

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("CP-MU-17: modificarUsuario acepta un teléfono vacío y lo guarda como nulo, y no aplica unicidad al teléfono")
    void modificarUsuario_debePermitirTelefonoVacio() {
        UsuarioEntity existente = crearUsuarioEntity(1L, "juan", "hash", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO formDTO = usuarioFormDTO("juan", null, "Juan Perez", "juan@mail.com", "   ", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);

        usuarioServicio.modificarUsuario(1L, formDTO);

        assertThat(existente.getTelefono()).isNull();
        verify(usuarioRepository).save(existente);
    }

    @Test
    @DisplayName("CP-MU-18: modificarUsuario traduce la violación del índice único udx_usuarios_username al mensaje de nombre de usuario en uso")
    void modificarUsuario_debeTraducirLaViolacionDelIndiceDelUsername() {
        UsuarioEntity existente = crearUsuarioEntity(1L, "juan", "hash", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO formDTO = usuarioFormDTO("pedro", null, "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenThrow(GuardadoUnicoTest.violacionPostgres("23505", "udx_usuarios_username"));

        assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, formDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessage("Ya existe un usuario con el nombre de usuario 'pedro'");
    }

    @Test
    @DisplayName("CP-MU-20: modificarUsuario permite reutilizar el nombre de usuario de un usuario dado de baja")
    void modificarUsuario_debePermitirReutilizarLosValoresDeUnUsuarioDadoDeBaja() {
        UsuarioEntity existente = crearUsuarioEntity(1L, "juan", "hash", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO formDTO = usuarioFormDTO("pedro", null, "Juan Perez", "pedro@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        // El único otro usuario con ese nombre de usuario está dado de baja: la consulta solo considera usuarios activos
        when(usuarioRepository.existsByUsername("pedro")).thenReturn(false);
        when(usuarioRepository.save(existente)).thenReturn(existente);

        assertThatCode(() -> usuarioServicio.modificarUsuario(1L, formDTO)).doesNotThrowAnyException();

        assertThat(existente.getUsername()).isEqualTo("pedro");
        assertThat(existente.getCorreo()).isEqualTo("pedro@mail.com");
    }

    @Test
    @DisplayName("CP-AU-23: altaUsuario lanza ReglaNegocioException y no consulta la BD cuando el correo electrónico no tiene un formato válido")
    void altaUsuario_debeRechazarCorreoConFormatoInvalido() {
        for (String correo : CORREOS_CON_FORMATO_INVALIDO) {
            // Arrange
            UsuarioFormDTO formDTO = usuarioFormDTO("juan", "ClaveSegura123", "Juan", correo, "1111", Rol.OPERARIO_DE_PRODUCCION);

            // Act + Assert
            assertThatThrownBy(() -> usuarioServicio.altaUsuario(formDTO))
                    .as("correo: '%s'", correo)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El correo electrónico no tiene un formato válido");
        }

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    @DisplayName("CP-MU-21: modificarUsuario lanza ReglaNegocioException y no consulta la BD cuando el correo electrónico no tiene un formato válido")
    void modificarUsuario_debeRechazarCorreoConFormatoInvalido() {
        for (String correo : CORREOS_CON_FORMATO_INVALIDO) {
            // Arrange
            UsuarioFormDTO formDTO = usuarioFormDTO("juan", null, "Juan", correo, "1111", Rol.OPERARIO_DE_PRODUCCION);

            // Act + Assert
            assertThatThrownBy(() -> usuarioServicio.modificarUsuario(1L, formDTO))
                    .as("correo: '%s'", correo)
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El correo electrónico no tiene un formato válido");
        }

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    // ==================== helpers ====================

    /**
     * Correos sin el formato de un correo electrónico: sin @, dominio sin punto, dos @, espacio interno, sin parte
     * local, y dominio que empieza o termina en punto.
     */
    private static final String[] CORREOS_CON_FORMATO_INVALIDO =
            {"no-es-correo", "a@b", "a@@b.com", "a b@c.com", "@x.com", "x@.com", "x@y."};

    private static UsuarioEntity crearUsuarioEntity(Long id, String username, String password, String nombre, String correo, String telefono, Rol rol) {
        return UsuarioEntity.builder()
                .id(id)
                .username(username)
                .password(password)
                .nombre(nombre)
                .apellido(APELLIDO)
                .correo(correo)
                .telefono(telefono)
                .rol(rol)
                .estado(Estado.ACTIVO)
                .build();
    }

    private static UsuarioFormDTO usuarioFormDTO(String username, String password, String nombre, String correo, String telefono, Rol rol) {
        return UsuarioFormDTO.builder()
                .username(username)
                .password(password)
                .nombre(nombre)
                .apellido(APELLIDO)
                .correo(correo)
                .telefono(telefono)
                .rol(rol)
                .build();
    }

    private static void assertUsuarioDTO(UsuarioEntity entidad, UsuarioResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getUsername()).isEqualTo(entidad.getUsername());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getApellido()).isEqualTo(entidad.getApellido());
        assertThat(dto.getCorreo()).isEqualTo(entidad.getCorreo());
        assertThat(dto.getTelefono()).isEqualTo(entidad.getTelefono());
        assertThat(dto.getRol()).isEqualTo(entidad.getRol());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
    }
}
