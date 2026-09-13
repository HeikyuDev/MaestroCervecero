package com.github.heikyudev.maestrocervecero.service.implementation.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.presentation.form_dto.usuario.UsuarioFormDTO;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoDuplicadoException;
import com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException;
import com.github.heikyudev.maestrocervecero.service.response_dto.usuario.UsuarioResponseDTO;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServicioImplTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServicioImpl usuarioServicio;

    // ==================== buscarTodos ====================

    @Test
    @DisplayName("CP-BT-01: buscarTodos retorna una página de usuarios correctamente mapeada a DTO")
    void buscarTodos_debeRetornarPaginaMapeada() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash1", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioEntity otroUsuarioEntity = crearUsuarioEntity(2L, "maria", "hash2", "Maria Lopez", "maria@mail.com", "2222", Rol.ADMINISTRADOR);

        // Cuando usuarioRepository.findAll(pageable) sea llamado, retorna una página con los usuarios activos
        // (el filtrado por estado = ACTIVO ya está resuelto dentro de la consulta del repositorio)
        when(usuarioRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(usuarioEntity, otroUsuarioEntity), pageable, 2));

        // === EJECUCION ===
        Page<UsuarioResponseDTO> resultado = usuarioServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertUsuarioDTO(usuarioEntity, resultado.getContent().get(0));
        assertUsuarioDTO(otroUsuarioEntity, resultado.getContent().get(1));
        verify(usuarioRepository).findAll(pageable);
    }

    @Test
    @DisplayName("CP-BT-02: buscarTodos retorna una página vacía cuando no hay usuarios registrados")
    void buscarTodos_debeRetornarPaginaVaciaSinRegistros() {
        // === PREPARACION DE DATOS ===
        Pageable pageable = PageRequest.of(0, 10);
        when(usuarioRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // === EJECUCION ===
        Page<UsuarioResponseDTO> resultado = usuarioServicio.buscarTodos(pageable);

        // === ASSERTS ===
        assertThat(resultado.getContent()).isEmpty();
        verify(usuarioRepository).findAll(pageable);
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
                .hasMessage("El Username ya esta registrado");

        verify(usuarioRepository).existsByUsername("juanperez");
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-AU-02: altaUsuario encripta la contraseña, persiste y retorna el DTO correspondiente cuando los datos son válidos (camino feliz)")
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
        assertThat(entidadCapturada.getCorreo()).isEqualTo("carlos@mail.com");
        assertThat(entidadCapturada.getTelefono()).isEqualTo("3333");
        assertThat(entidadCapturada.getRol()).isEqualTo(Rol.GERENTE_DE_PRODUCCION);
        // El alta siempre debe registrar al usuario como ACTIVO, sin importar lo que traiga el FormDTO
        assertThat(entidadCapturada.getEstado()).isEqualTo(Estado.ACTIVO);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getUsername()).isEqualTo("carlos_cervecero");
        assertThat(resultado.getNombre()).isEqualTo("Carlos Gomez");
        assertThat(resultado.getCorreo()).isEqualTo("carlos@mail.com");
        assertThat(resultado.getTelefono()).isEqualTo("3333");
        assertThat(resultado.getRol()).isEqualTo(Rol.GERENTE_DE_PRODUCCION);
        verify(usuarioRepository).existsByUsername("carlos_cervecero");
        verify(passwordEncoder).encode("ClaveSegura123");
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
                .hasMessage("El nombre de usuario 'pedro' ya está en uso.");

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
    @DisplayName("CP-MU-05: modificarUsuario permite conservar el propio username actual con distinta capitalización (case-insensitive)")
    void modificarUsuario_debePermitirConservarUsernamePropioCaseInsensitive() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash-viejo", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        // Mismo username (distinto case): equalsIgnoreCase evita la validación de duplicidad
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("JUAN", "NuevaClave123", "Juan Perez Actualizado", "juanactualizado@mail.com", "9999", Rol.GERENTE_DE_PRODUCCION);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(passwordEncoder.encode("NuevaClave123")).thenReturn("hash-NuevaClave123");
        when(usuarioRepository.save(usuarioEntity)).thenReturn(usuarioEntity);

        // === EJECUCION ===
        UsuarioResponseDTO resultado = usuarioServicio.modificarUsuario(1L, usuarioFormDTO);

        // === ASSERTS ===
        assertThat(resultado.getUsername()).isEqualTo("JUAN");
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
    @DisplayName("CP-MU-07: modificarUsuario conserva la contraseña existente y no invoca al encoder cuando la contraseña enviada está en blanco")
    void modificarUsuario_debeConservarPasswordCuandoEstaEnBlanco() {
        // === PREPARACION DE DATOS ===
        UsuarioEntity usuarioEntity = crearUsuarioEntity(1L, "juan", "hash-existente", "Juan Perez", "juan@mail.com", "1111", Rol.OPERARIO_DE_PRODUCCION);
        UsuarioFormDTO usuarioFormDTO = usuarioFormDTO("juan", "   ", "Juan Perez Actualizado", "juanactualizado@mail.com", "9999", Rol.OPERARIO_DE_PRODUCCION);
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

    // ==================== helpers ====================

    private static UsuarioEntity crearUsuarioEntity(Long id, String username, String password, String nombre, String correo, String telefono, Rol rol) {
        return UsuarioEntity.builder()
                .id(id)
                .username(username)
                .password(password)
                .nombre(nombre)
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
                .correo(correo)
                .telefono(telefono)
                .rol(rol)
                .build();
    }

    private static void assertUsuarioDTO(UsuarioEntity entidad, UsuarioResponseDTO dto) {
        assertThat(dto.getId()).isEqualTo(entidad.getId());
        assertThat(dto.getUsername()).isEqualTo(entidad.getUsername());
        assertThat(dto.getNombre()).isEqualTo(entidad.getNombre());
        assertThat(dto.getCorreo()).isEqualTo(entidad.getCorreo());
        assertThat(dto.getTelefono()).isEqualTo(entidad.getTelefono());
        assertThat(dto.getRol()).isEqualTo(entidad.getRol());
        assertThat(dto.getEstado()).isEqualTo(entidad.getEstado());
        assertThat(dto.getCreatedBy()).isEqualTo(entidad.getCreatedBy());
        assertThat(dto.getCreatedDate()).isEqualTo(entidad.getCreatedDate());
        assertThat(dto.getLastModifiedBy()).isEqualTo(entidad.getLastModifiedBy());
        assertThat(dto.getLastModifiedDate()).isEqualTo(entidad.getLastModifiedDate());
    }
}
