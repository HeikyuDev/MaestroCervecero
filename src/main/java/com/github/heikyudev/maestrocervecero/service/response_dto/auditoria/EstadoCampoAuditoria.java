package com.github.heikyudev.maestrocervecero.service.response_dto.auditoria;

/**
 * Resultado de comparar un campo entre el estado anterior y el posterior de un registro auditado.
 */
public enum EstadoCampoAuditoria {
    /** El campo existe en los dos estados con el mismo valor. */
    SIN_CAMBIOS,
    /** El campo existe en los dos estados con valores distintos. */
    MODIFICADO,
    /** El campo solo existe en el estado posterior (por ejemplo, en un alta). */
    AGREGADO,
    /** El campo solo existe en el estado anterior. */
    ELIMINADO
}
