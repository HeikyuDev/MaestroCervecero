package com.github.heikyudev.maestrocervecero.persistence.repository;

import com.github.heikyudev.maestrocervecero.persistence.entity.equipamiento.EquipamientoEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Repositorio de las secuencias de la base de datos que numeran los identificadores internos generados por el
 * sistema (uno por prefijo: MOL, MAC, OLL, FER y BRL).
 * <p>
 * Las secuencias no son transaccionales: si el alta que pidió un número se revierte, ese número no se reutiliza y
 * queda un hueco en la numeración. Por eso el Service lo pide como último paso, después de todas las validaciones.
 * </p>
 */
@org.springframework.stereotype.Repository
public interface ISecuenciaIdentificadorRepository extends Repository<EquipamientoEntity, Long> {

    @Query(value = "SELECT nextval('seq_identificador_mol')", nativeQuery = true)
    Long siguienteNumeroMolino();

    @Query(value = "SELECT nextval('seq_identificador_mac')", nativeQuery = true)
    Long siguienteNumeroMacerador();

    @Query(value = "SELECT nextval('seq_identificador_oll')", nativeQuery = true)
    Long siguienteNumeroOllaHervor();

    @Query(value = "SELECT nextval('seq_identificador_fer')", nativeQuery = true)
    Long siguienteNumeroFermentador();

    @Query(value = "SELECT nextval('seq_identificador_brl')", nativeQuery = true)
    Long siguienteNumeroBarril();
}
