package com.github.heikyudev.maestrocervecero.service.implementation;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.IUsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Puente entre nuestro modelo de usuarios ({@link UsuarioEntity}) y Spring Security.
 * <p>
 * Es el único punto del sistema donde se traduce un {@code UsuarioEntity} de la base de
 * datos a un {@link UserDetails} que Spring Security entiende. {@code DaoAuthenticationProvider}
 * (configurado en {@code SecurityConfig}) invoca a {@link #loadUserByUsername(String)} en
 * cada intento de login para comparar la contraseña ingresada contra el hash guardado.
 */
@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService{

    // Spring inyecta automaticamente el usuario Repository, gracias al
    // RequiredArgsConstructor, el cual crea un constructor con los campos finales
    private final IUsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Si el usuario no existe, esta excepción NUNCA llega tal cual a la vista de login:
        // DaoAuthenticationProvider tiene hideUserNotFoundExceptions=true por defecto, así que
        // la reemplaza por un BadCredentialsException genérico. Esto evita que alguien pueda
        // usar el formulario de login para "adivinar" qué usernames existen en el sistema.
        UsuarioEntity unUsuario = usuarioRepository.findUserEntityByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // El Rol se agrega con el prefijo "ROLE_": es la convención que Spring Security espera
        // para que expresiones como hasRole("ADMINISTRADOR") (sin el prefijo) funcionen -
        // internamente esas expresiones buscan la authority "ROLE_ADMINISTRADOR".
        authorities.add(new SimpleGrantedAuthority("ROLE_".concat(unUsuario.getRol().name())));

        // Los permisos granulares del Rol se agregan SIN prefijo, porque se validan con
        // hasAuthority("USUARIO_CREAR") (comparación exacta, sin el agregado automático "ROLE_"
        // que sí aplica hasRole()). Rol y Permiso son conceptualmente distintos: uno identifica
        // "qué es" el usuario, el otro "qué puede hacer".
        unUsuario.getRol().getPermisos().forEach(permiso -> authorities.add(new SimpleGrantedAuthority(permiso.name())));

        // Retorno un usuario el cual Spring Security Entiende
        return new User(unUsuario.getUsername(),
                unUsuario.getPassword(),
                unUsuario.isEnabled(),
                unUsuario.isAccountNonExpired(),
                unUsuario.isAccountNonLocked(),
                unUsuario.isCredentialsNonExpired(),
                authorities);
    }
}
