package com.github.heikyudev.maestrocervecero.persistence.repository.ingreso_insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.ingreso_insumo.LoteInsumoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA de los lotes de insumo ({@link LoteInsumoEntity}).
 */
@Repository
public interface ILoteInsumoRepository extends JpaRepository<LoteInsumoEntity, Long> {

    /**
     * Busca el lote de insumo de un insumo con una identificación de lote de proveedor
     * determinada.
     * <p>
     * Se usa para decidir, al registrar un ingreso, si corresponde unificar la cantidad recibida
     * en un lote ya existente o crear uno nuevo.
     * </p>
     *
     * @param idInsumo El ID del insumo.
     * @param identificacionLoteProveedor Identificación del lote asignada por el proveedor.
     * @return Un Optional que contiene el lote si existe, o vacío en caso contrario.
     */
    Optional<LoteInsumoEntity> findByInsumoIdAndIdentificacionLoteProveedor(Long idInsumo, String identificacionLoteProveedor);
}
