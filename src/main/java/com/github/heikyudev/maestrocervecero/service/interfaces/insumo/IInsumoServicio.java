package com.github.heikyudev.maestrocervecero.service.interfaces.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.TipoInsumo;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interfaz que define los servicios de consulta genéricos sobre insumos, sin importar su tipo
 * concreto.
 */
public interface IInsumoServicio {

    /**
     * Obtiene una página de insumos activos de cualquier tipo, filtrados opcionalmente por
     * nombre (coincidencia parcial) y/o tipo concreto (coincidencia exacta). Un parámetro nulo
     * no restringe por ese criterio.
     *
     * @param nombre Texto a buscar dentro del nombre del insumo, o {@code null} para no filtrar por él.
     * @param tipo Tipo concreto de insumo a filtrar, o {@code null} para no filtrar por tipo.
     * @param pageable La configuración de paginación.
     * @return Una página de insumos activos que cumplen los criterios indicados, en su DTO concreto correspondiente.
     */
    Page<InsumoResponseDTO> filtrarInsumos(String nombre, TipoInsumo tipo, Pageable pageable);

    /**
     * Busca un insumo activo por su ID, cualquiera sea su tipo concreto (Malta, Lúpulo o Levadura).
     *
     * @param id El ID del insumo buscado.
     * @return El insumo encontrado, en su DTO concreto correspondiente.
     * @throws com.github.heikyudev.maestrocervecero.service.exception.RecursoNoEncontradoException Si no existe un insumo activo con ese ID.
     */
    InsumoResponseDTO buscarPorId(Long id);
}
