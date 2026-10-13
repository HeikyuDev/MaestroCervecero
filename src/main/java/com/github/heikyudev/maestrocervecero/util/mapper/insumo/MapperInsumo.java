package com.github.heikyudev.maestrocervecero.util.mapper.insumo;

import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.InsumoEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LevaduraEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.LupuloEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.insumo.MaltaEntity;
import com.github.heikyudev.maestrocervecero.service.response_dto.insumo.InsumoResponseDTO;
import org.hibernate.Hibernate;

/**
 * MapperInsumo tiene la responsabilidad de mapear de forma polimórfica una entidad
 * {@link InsumoEntity} (Malta, Lúpulo o Levadura) a su {@link InsumoResponseDTO} concreto
 * correspondiente, delegando en el mapper específico de cada tipo según el tipo real de la
 * entidad recibida en tiempo de ejecución.
 * <p>
 * Una relación perezosa hacia {@link InsumoEntity} (por ejemplo, el insumo de un lote de insumo) llega como un
 * proxy de Hibernate de la clase abstracta, que no es instancia de ninguna subclase concreta: por eso el insumo
 * se desenvuelve ({@link Hibernate#unproxy(Object)}) antes de elegir el mapper.
 * </p>
 */
public class MapperInsumo {

    /**
     * Mapea una instancia de {@link InsumoEntity} a su {@link InsumoResponseDTO} concreto.
     *
     * @param insumoEntity Entidad de insumo a convertir (Malta, Lúpulo o Levadura), o un proxy de Hibernate de ella.
     * @return Objeto DTO correspondiente al tipo real de la entidad, o {@code null} si la entidad de entrada es nula.
     * @throws IllegalStateException Si el tipo concreto del insumo no es ninguno de los soportados.
     */
    public static InsumoResponseDTO toDTO(InsumoEntity insumoEntity) {
        if (insumoEntity == null) {
            return null;
        }

        Object insumoReal = Hibernate.unproxy(insumoEntity);

        if (insumoReal instanceof MaltaEntity maltaEntity) {
            return MapperMalta.toDTO(maltaEntity);
        }
        if (insumoReal instanceof LupuloEntity lupuloEntity) {
            return MapperLupulo.toDTO(lupuloEntity);
        }
        if (insumoReal instanceof LevaduraEntity levaduraEntity) {
            return MapperLevadura.toDTO(levaduraEntity);
        }

        throw new IllegalStateException("Tipo de insumo no soportado: " + insumoReal.getClass().getSimpleName());
    }
}
