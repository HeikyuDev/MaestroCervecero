package com.github.heikyudev.maestrocervecero.service.implementation.usuario;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import com.github.heikyudev.maestrocervecero.persistence.repository.usuario.IUsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailServiceImplTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @InjectMocks
    private UserDetailServiceImpl userDetailService;

    // ==================== loadUserByUsername ====================

    @Test
    @DisplayName("CP-UD-01: loadUserByUsername normaliza el nombre de usuario ingresado (mayúsculas y espacios) antes de buscar la cuenta")
    void loadUserByUsername_debeNormalizarElUsernameIngresado() {
        // === PREPARACION DE DATOS ===
        when(usuarioRepository.findUserEntityByUsername("admin")).thenReturn(Optional.of(usuario("admin", Rol.ADMINISTRADOR)));

        // === EJECUCION ===
        UserDetails resultado = userDetailService.loadUserByUsername("  ADMIN ");

        // === ASSERTS ===
        assertThat(resultado.getUsername()).isEqualTo("admin");
        assertThat(resultado.getPassword()).isEqualTo("hash");
        verify(usuarioRepository).findUserEntityByUsername("admin");
    }

    @Test
    @DisplayName("CP-UD-02: loadUserByUsername lanza UsernameNotFoundException cuando no hay un usuario activo con ese nombre de usuario")
    void loadUserByUsername_debeLanzarExcepcionSiNoExiste() {
        when(usuarioRepository.findUserEntityByUsername("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailService.loadUserByUsername("Inexistente"))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(usuarioRepository).findUserEntityByUsername("inexistente");
    }

    @Test
    @DisplayName("CP-UD-03: loadUserByUsername arma las authorities con el rol (prefijo ROLE_) y los permisos del rol")
    void loadUserByUsername_debeArmarLasAuthoritiesDelRol() {
        when(usuarioRepository.findUserEntityByUsername("admin")).thenReturn(Optional.of(usuario("admin", Rol.ADMINISTRADOR)));

        UserDetails resultado = userDetailService.loadUserByUsername("Admin");

        assertThat(resultado.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMINISTRADOR")
                .containsAll(Rol.ADMINISTRADOR.getPermisos().stream().map(Enum::name).toList());
        assertThat(resultado.isEnabled()).isTrue();
    }

    // ==================== helpers ====================

    private static UsuarioEntity usuario(String username, Rol rol) {
        return UsuarioEntity.builder()
                .id(1L)
                .username(username)
                .password("hash")
                .nombre("Administrador")
                .apellido("Sistema")
                .correo("admin@maestrocervecero.com")
                .rol(rol)
                .estado(Estado.ACTIVO)
                .build();
    }
}
