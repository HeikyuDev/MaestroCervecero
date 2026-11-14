package com.github.heikyudev.maestrocervecero.persistence.entity.cliente;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditableEntity;
import com.github.heikyudev.maestrocervecero.persistence.entity.audit.ExcluidoDeAuditoria;
import com.github.heikyudev.maestrocervecero.persistence.entity.ubicacion.LocalidadEntity;
import com.github.heikyudev.maestrocervecero.persistence.enums.Estado;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ClienteEntity extends AuditableEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String nombre;

    // Los datos de contacto son personales: nunca se copian al detalle de auditoría
    // Opcional: solo dígitos, sin unicidad
    @ExcluidoDeAuditoria
    private String telefono;

    @ExcluidoDeAuditoria
    @Column(nullable = false)
    private String email;

    @ExcluidoDeAuditoria
    @Column(nullable = false)
    private String direccion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "localidad_id", nullable = false)
    private LocalidadEntity localidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado;
}
