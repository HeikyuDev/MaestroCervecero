package com.github.heikyudev.maestrocervecero.persistence.repository.receta;

import com.github.heikyudev.maestrocervecero.persistence.entity.receta.RecetaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de las recetas ({@link RecetaEntity}).
 * <p>
 * La entidad ya no utiliza {@code @SoftDelete}: el filtrado de recetas dadas de baja se realiza
 * explícitamente en cada consulta mediante la condición {@code estado = 'ACTIVO'}. Los métodos
 * heredados de {@link JpaRepository} ({@code findById}, {@code findAll}) se sobrescriben con
 * esa misma condición para que todo el código existente que ya los invoca (sin cambios) siga
 * viendo únicamente recetas activas.
 * </p>
 */
@Repository
public interface IRecetaRepository extends JpaRepository<RecetaEntity, Long> {

    /**
     * Busca una receta activa por su ID.
     *
     * @param id El ID de la receta a buscar.
     * @return Un Optional que contiene la receta si está activa, o vacío en caso contrario.
     */
    @Override
    @Query("SELECT r FROM RecetaEntity r WHERE r.id = :id AND r.estado = 'ACTIVO'")
    Optional<RecetaEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de recetas activas.
     * @param pageable La configuración de paginación.
     * @return Una página de recetas activas.
     */
    @Override
    @Query(value = "SELECT r FROM RecetaEntity r WHERE r.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(r) FROM RecetaEntity r WHERE r.estado = 'ACTIVO'")
    Page<RecetaEntity> findAll(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RecetaEntity r WHERE r.id = :id AND r.estado = 'ACTIVO'")
    Optional<RecetaEntity> findByIdParaActualizarContador(@Param("id") Long id);
}
