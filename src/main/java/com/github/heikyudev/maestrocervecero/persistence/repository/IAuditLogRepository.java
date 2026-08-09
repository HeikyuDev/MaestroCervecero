package com.github.heikyudev.maestrocervecero.persistence.repository;

import com.github.heikyudev.maestrocervecero.persistence.entity.audit.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IAuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
}
