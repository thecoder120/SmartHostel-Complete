package com.smarthostel.backend;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_accounts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_accounts_username", columnNames = "username"),
    @UniqueConstraint(name = "uk_user_accounts_email", columnNames = "email")
})
public class UserAccount {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false, length = 40) private String username;
  @Column(nullable = false, length = 254) private String email;
  @Column(nullable = false, length = 100) private String passwordHash;
  @Column(nullable = false, length = 20) private String role = "STAFF";
  @Column(name = "student_id", unique = true) private Long studentId;
  @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
  @Column(nullable = false) private boolean mustChangePassword;

  protected UserAccount() {}
  public UserAccount(String username, String email, String passwordHash, String role) {
    this.username = username;
    this.email = email;
    this.passwordHash = passwordHash;
    this.role = role;
  }
  public Long getId() { return id; }
  public String getUsername() { return username; }
  public String getEmail() { return email; }
  public String getPasswordHash() { return passwordHash; }
  public String getRole() { return role; }
  public Long getStudentId() { return studentId; }
  public Instant getCreatedAt() { return createdAt; }
  public void setRole(String role) { this.role=role; }
  public void setStudentId(Long studentId) { this.studentId=studentId; }
  public boolean isMustChangePassword() { return mustChangePassword; }
  public void setMustChangePassword(boolean value) { mustChangePassword=value; }
  public void setPasswordHash(String value) { passwordHash=value; }
}
