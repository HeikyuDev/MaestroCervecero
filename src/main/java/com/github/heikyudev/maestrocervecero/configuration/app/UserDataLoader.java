package com.github.heikyudev.maestrocervecero.configuration.app;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AccionAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ConceptoAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.Rol;
import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import com.github.heikyudev.maestrocervecero.persistence.repository.IUsuarioRepository;
import com.github.heikyudev.maestrocervecero.service.aspect.AuditableAction;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDataLoader implements CommandLineRunner {

    private final IUsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // Verificar si el usuario 'admin' ya existe
        if (!usuarioRepository.existsByUsername("admin")) {
            // Crear el usuario por defecto
            UsuarioEntity adminUser = UsuarioEntity.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin"))
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .isEnabled(true)
                    .rol(Rol.ADMINISTRADOR)
                    .build();
            usuarioRepository.save(adminUser);
            System.out.println(">>> Usuario 'admin' creado exitosamente por defecto.");
        }

    }
}
