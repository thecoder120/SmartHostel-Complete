package com.smarthostel.backend;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="audit_events")
public class AuditEvent {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false, length=80) private String actor;
  @Column(nullable=false, length=80) private String action;
  @Column(nullable=false, length=80) private String recordType;
  @Column(length=80) private String recordId;
  @Column(nullable=false, updatable=false) private Instant createdAt=Instant.now();
  protected AuditEvent() {}
  public AuditEvent(String actor,String action,String recordType,String recordId){this.actor=actor;this.action=action;this.recordType=recordType;this.recordId=recordId;}
  public Long getId(){return id;} public String getActor(){return actor;} public String getAction(){return action;}
  public String getRecordType(){return recordType;} public String getRecordId(){return recordId;} public Instant getCreatedAt(){return createdAt;}
}
