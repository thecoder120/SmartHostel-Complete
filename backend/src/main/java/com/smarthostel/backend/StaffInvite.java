package com.smarthostel.backend;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="staff_invites")
public class StaffInvite {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @Column(nullable=false,unique=true,length=64) private String codeHash;
  @Column(nullable=false,length=40) private String createdBy;
  @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
  @Column(nullable=false) private Instant expiresAt;
  private Instant usedAt;
  protected StaffInvite(){}
  public StaffInvite(String codeHash,String createdBy,Instant expiresAt){this.codeHash=codeHash;this.createdBy=createdBy;this.expiresAt=expiresAt;}
  public Long getId(){return id;} public String getCodeHash(){return codeHash;} public String getCreatedBy(){return createdBy;}
  public Instant getCreatedAt(){return createdAt;} public Instant getExpiresAt(){return expiresAt;} public Instant getUsedAt(){return usedAt;}
  public boolean isUsable(){return usedAt==null&&expiresAt.isAfter(Instant.now());} public void markUsed(){usedAt=Instant.now();}
}
