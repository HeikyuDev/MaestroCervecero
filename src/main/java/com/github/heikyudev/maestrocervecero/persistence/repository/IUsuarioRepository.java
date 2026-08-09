package com.github.heikyudev.maestrocervecero.persistence.repository;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    // Buscar un usuario por su nombre de usuario
    Optional<UsuarioEntity> findUserEntityByUsername(String username);

    boolean existsByUsername(String username);
}
