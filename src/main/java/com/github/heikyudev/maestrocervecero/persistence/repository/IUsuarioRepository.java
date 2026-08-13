package com.github.heikyudev.maestrocervecero.persistence.repository;

import com.github.heikyudev.maestrocervecero.persistence.entity.usuario.UsuarioEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    /**
     * Busca un usuario por su nombre de usuario.
     * @param username El nombre de usuario a buscar.
     * @return Un Optional que contiene el usuario si se encuentra, o vacío si no se encuentra.
     */
    Optional<UsuarioEntity> findUserEntityByUsername(String username);

    /**
     * Verifica si existe un usuario con el nombre de usuario dado.
     * @param username El nombre de usuario a buscar.
     * @return true si existe un usuario con el nombre de usuario dado, false en caso contrario.
     */
    boolean existsByUsername(String username);

    /**
     * Verifica si existe un usuario con el id dado.
     * @param id El id del usuario a buscar.
     * @return true si existe un usuario con el id dado, false en caso contrario.
     */
    boolean existsById(Long id);

    /**
     * Obtiene una página de usuarios.
     * @param pageable La configuración de paginación.
     * @return Una página de usuarios.
     */
    Page<UsuarioEntity> findAll(Pageable pageable);

}
