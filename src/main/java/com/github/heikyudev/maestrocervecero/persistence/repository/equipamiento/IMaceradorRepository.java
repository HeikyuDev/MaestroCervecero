package com.github.heikyudev.maestrocervecero.persistence.repository.equipamiento;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.MaceradorEntity;
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
public interface IMaceradorRepository extends JpaRepository<MaceradorEntity, Long> {

    /**
     * Busca un macerador activo por su ID.
     */
    @Override
    @Query("SELECT m FROM MaceradorEntity m WHERE m.id = :id AND m.estado = 'ACTIVO'")
    Optional<MaceradorEntity> findById(@Param("id") Long id);

    /**
     * Obtiene una página de maceradores activos.
     */
    @Override
    @Query(value = "SELECT m FROM MaceradorEntity m WHERE m.estado = 'ACTIVO'",
            countQuery = "SELECT COUNT(m) FROM MaceradorEntity m WHERE m.estado = 'ACTIVO'")
    Page<MaceradorEntity> findAll(Pageable pageable);

    // Creo que voy a tener Bloqueos compartidos
    // Por ejemplo si dos lotes quieren utiliza el mimso macerador
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EquipamientoEntity e WHERE e.id = :id AND e.estado = 'ACTIVO'")
    Optional<MaceradorEntity> buscarPorIdParaIniciarLote(@Param("id") Long id);
}
