package com.smarthostel.backend;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController @RequestMapping("/api/admin") @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  private final UserAccountRepository accounts; private final StaffInviteRepository invites;
  private final AuditEventRepository auditEvents; private final PasswordEncoder passwords; private final AuditService audit;
  private final SecureRandom random=new SecureRandom();
  public AdminController(UserAccountRepository accounts,StaffInviteRepository invites,AuditEventRepository auditEvents,PasswordEncoder passwords,AuditService audit){
    this.accounts=accounts;this.invites=invites;this.auditEvents=auditEvents;this.passwords=passwords;this.audit=audit;
  }
  @GetMapping("/staff") public List<Map<String,Object>> staff(){return accounts.findAll().stream().map(a->Map.<String,Object>of(
      "id",a.getId(),"username",a.getUsername(),"email",a.getEmail(),"role",a.getRole(),"createdAt",a.getCreatedAt(),"mustChangePassword",a.isMustChangePassword())).toList();}
  @PostMapping("/invites") @Transactional public Map<String,String> createInvite(){
    byte[] secret=new byte[24];random.nextBytes(secret);String code=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    invites.save(new StaffInvite(sha256(code),currentUser(),Instant.now().plus(7,ChronoUnit.DAYS)));
    audit.record("INVITE_CREATED","STAFF_INVITE",null);return Map.of("code",code,"expiresAt",Instant.now().plus(7,ChronoUnit.DAYS).toString());
  }
  @GetMapping("/audit") public List<AuditEvent> events(){return auditEvents.findTop200ByOrderByCreatedAtDesc();}
  @PutMapping("/staff/{id}/role") @Transactional public Map<String,String> changeRole(@PathVariable Long id,@RequestBody RoleChange change){
    if(!List.of("ADMIN","STAFF").contains(change.role())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Role must be ADMIN or STAFF");
    UserAccount target=accounts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Staff account not found"));
    if("ADMIN".equals(target.getRole())&&"STAFF".equals(change.role())&&accounts.findAll().stream().filter(a->"ADMIN".equals(a.getRole())).count()<2)
      throw new ResponseStatusException(HttpStatus.CONFLICT,"At least one administrator must remain");
    target.setRole(change.role());accounts.save(target);audit.record("ROLE_CHANGED","STAFF",target.getUsername());return Map.of("role",target.getRole());
  }
  @PostMapping("/staff/{id}/password-reset") @Transactional public Map<String,String> resetPassword(@PathVariable Long id){
    UserAccount target=accounts.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Staff account not found"));
    byte[] secret=new byte[18];random.nextBytes(secret);String temporary=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    target.setPasswordHash(passwords.encode(temporary));target.setMustChangePassword(true);accounts.save(target);
    audit.record("ADMIN_PASSWORD_RESET","STAFF",target.getUsername());return Map.of("temporaryPassword",temporary,"username",target.getUsername());
  }
  private static String sha256(String value){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
  private static String currentUser(){var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();return auth==null?"system":auth.getName();}
  public record RoleChange(String role){}
}
