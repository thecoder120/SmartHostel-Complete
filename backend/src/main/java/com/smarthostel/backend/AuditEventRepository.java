package com.smarthostel.backend;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent,Long> {
  java.util.List<AuditEvent> findTop200ByOrderByCreatedAtDesc();
}
