package com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.FermentadorEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IFermentadorRepository extends JpaRepository<FermentadorEntity, Long> {

    /**
     * Busca un fermentador activo por su ID.
     */
    @Override
    @Query("SELECT f FROM FermentadorEntity f WHERE f.id = :id AND f.estado = 'ACTIVO'")
    Optional<FermentadorEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de fermentadores activos.
     */
    @Override
    @Query(value = "SELECT f FROM FermentadorEntity f WHERE f.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(f) FROM FermentadorEntity f WHERE f.estado = 'ACTIVO'")
    Page<FermentadorEntity> findAll(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EquipamientoEntity e WHERE e.id = :id AND e.estado = 'ACTIVO'")
    Optional<FermentadorEntity> buscarPorIdParaIniciarLote(@Param("id") Long id);
}
