package com.github.heikyudev.maestrocervecero.presentation.form_dto;

import jakarta.validation.groups.Default;

/**
 * Grupo de validación de las restricciones que aplican solo al alta de un registro. Extiende {@link Default}, de modo
 * que validar con este grupo ({@code @Validated(GrupoAlta.class)}) incluye también todas las restricciones comunes.
 * <p>
 * La modificación valida solo el grupo por defecto ({@code @Valid}): los datos que se fijan en el alta y no se pueden
 * cambiar después (por ejemplo, la capacidad total de un equipo) no se envían en el formulario de edición.
 * </p>
 */
public interface GrupoAlta extends Default {
}
