package com.github.heikyudev.maestrocervecero.persistence.entity.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Superclase mapeada (no genera tabla propia) que le agrega auditoría a nivel de fila
 * a cualquier entidad que la extienda: quién la creó, cuándo, quién la modificó por última
 * vez y cuándo. Spring Data JPA completa estos campos automáticamente gracias a
 * {@link AuditingEntityListener}, sin que el desarrollador tenga que setearlos a mano.
 * <p>
 * El genérico {@code U} representa el tipo del "auditor" (típicamente el username como
 * String), para no atar esta clase a una implementación concreta de {@code AuditorAware}.
 *
 * @param <U> tipo de dato usado para identificar al usuario auditor (ej: String, Long)
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
// Este es el Supervisor el cual basandose en las etiquetas de Spring Data JPA completa automáticamente estos campos.
// si es @createdBy sabe que tiene que llamar al bean auditorAware para obtener el username del usuario actual.
// si es un @LastModifiedBy sabe que tiene que llamar al bean auditorAware para obtener el username del usuario actual.
// su es un @createdDate tiene que utilizar la fecha del servidor.
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity<U> {

    // Usuario que creó el registro. Spring lo completa una sola vez, al persistir.
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private U createdBy;

    // Fecha y hora de creación del registro. Tampoco se actualiza luego del insert.
    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    // Usuario que hizo la última modificación. Se actualiza en cada update.
    @LastModifiedBy
    @Column(name = "last_modified_by")
    private U lastModifiedBy;

    // Fecha y hora de la última modificación.
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
}
