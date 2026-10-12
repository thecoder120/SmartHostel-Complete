package com.smarthostel.backend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Locale;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthenticationManager authenticationManager;
  private final UserAccountRepository accounts;
  private final PasswordEncoder passwordEncoder;
  private final String staffInviteCode;
  private final StaffInviteRepository invites;
  private final AuditService audit;
  private final SecurityContextRepository contextRepository;
  public AuthController(AuthenticationManager authenticationManager, UserAccountRepository accounts, PasswordEncoder passwordEncoder,
      StaffInviteRepository invites, AuditService audit, SecurityContextRepository contextRepository,
      @Value("${STAFF_INVITE_CODE:}") String staffInviteCode) {
    this.authenticationManager = authenticationManager;
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.invites=invites; this.audit=audit; this.contextRepository=contextRepository;
    this.staffInviteCode = staffInviteCode;
  }

  @GetMapping("/csrf") public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }

  @GetMapping("/config") public Map<String, Boolean> config() {
    return Map.of("inviteRequired", false, "registrationOpen", true);
  }

  @PostMapping("/login")
  public Map<String, String> login(@RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
    if (body.username() == null || body.password() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
    try {
      promoteBootstrapAdministrator();
      Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(body.username().trim(), body.password()));
      HttpSession session=prepareSession(request);
      SecurityContext context = SecurityContextHolder.createEmptyContext();
      context.setAuthentication(authentication);
      SecurityContextHolder.setContext(context);
      session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
      contextRepository.saveContext(context, request, response);
      audit.record("SIGN_IN","STAFF",authentication.getName());
      return accountSummary(authentication.getName());
    } catch (org.springframework.security.core.AuthenticationException exception) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }
  }

  @PostMapping("/register") @Transactional
  public Map<String, String> register(@RequestBody RegisterRequest body, HttpServletRequest request, HttpServletResponse response) {
    StaffInvite matchedInvite=validateInvite(body.inviteCode());
    if (body.username() == null || !body.username().trim().matches("[A-Za-z0-9._-]{3,40}")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must be 3–40 characters and use letters, numbers, dot, underscore, or hyphen");
    }
    if (body.email() == null || !body.email().trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+") || body.email().length() > 254) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address");
    }
    if (body.password() == null || body.password().length() < 8 || body.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
    }
    String username = body.username().trim();
    String email = body.email().trim().toLowerCase(Locale.ROOT);
    if (accounts.existsByUsernameIgnoreCase(username) || accounts.existsByEmailIgnoreCase(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "That username or email is already registered");
    }
    try {
      String role=accounts.count()==0?"ADMIN":"STAFF";
      accounts.saveAndFlush(new UserAccount(username, email, passwordEncoder.encode(body.password()),role));
      if(matchedInvite!=null) matchedInvite.markUsed();
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "That username or email is already registered");
    }
    audit.record("ACCOUNT_CREATED","STAFF",username);
    return establishSession(username, body.password(), request, response);
  }

  private Map<String, String> establishSession(String username, String password, HttpServletRequest request, HttpServletResponse response) {
    Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    HttpSession session=prepareSession(request);
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    contextRepository.saveContext(context, request, response);
    return accountSummary(authentication.getName());
  }

  private HttpSession prepareSession(HttpServletRequest request) {
    HttpSession previous=request.getSession(false);
    if(previous!=null) previous.invalidate();
    return request.getSession(true);
  }

  @GetMapping("/me") public Map<String, String> me(Authentication authentication) {
    return accountSummary(authentication.getName());
  }

  @PostMapping("/password") public Map<String,String> changePassword(@RequestBody PasswordChange body,Authentication authentication,HttpServletRequest request,HttpServletResponse response){
    if(body.currentPassword()==null||body.newPassword()==null||body.newPassword().length()<8||body.newPassword().getBytes(StandardCharsets.UTF_8).length>72)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"New password must be at least 8 characters");
    UserAccount account=accounts.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
    if(!passwordEncoder.matches(body.currentPassword(),account.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Current password is incorrect");
    account.setPasswordHash(passwordEncoder.encode(body.newPassword())); account.setMustChangePassword(false); accounts.save(account);
    Authentication updated=authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(account.getUsername(),body.newPassword()));
    HttpSession session=prepareSession(request);SecurityContext context=SecurityContextHolder.createEmptyContext();context.setAuthentication(updated);SecurityContextHolder.setContext(context);session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context);contextRepository.saveContext(context,request,response);
    audit.record("PASSWORD_CHANGED","STAFF",account.getUsername()); return Map.of("message","Password updated");
  }

  private StaffInvite validateInvite(String submitted){
    if(submitted==null||submitted.isBlank()) return null;
    if(staffInviteCode.isBlank()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"That invite code is not valid");
    if(staffInviteCode.length()<16) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Staff registration is not configured securely");
    if(accounts.count()==0&&MessageDigest.isEqual(staffInviteCode.getBytes(StandardCharsets.UTF_8),submitted.getBytes(StandardCharsets.UTF_8))) return null;
    if(accounts.count()>0){
      StaffInvite invite=invites.lockByCodeHash(hash(submitted)).orElse(null);
      if(invite!=null&&invite.isUsable()) return invite;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN,"That invite code is not valid or has already been used");
  }
  private static String hash(String value){
    try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
    catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
  }
  private void promoteBootstrapAdministrator(){
    List<UserAccount> all=accounts.findAll();
    if(!all.isEmpty()&&all.stream().noneMatch(account->"ADMIN".equals(account.getRole()))) {
      UserAccount first=all.stream().min(java.util.Comparator.comparing(UserAccount::getCreatedAt)).orElseThrow();
      first.setRole("ADMIN");accounts.save(first);
    }
  }
  private Map<String,String> accountSummary(String username){
    UserAccount account=accounts.findByUsernameIgnoreCase(username).orElseThrow();
    return Map.of("username",account.getUsername(),"role",account.getRole(),"email",account.getEmail(),"mustChangePassword",Boolean.toString(account.isMustChangePassword()));
  }

  public record LoginRequest(String username, String password) {}
  public record RegisterRequest(String username, String email, String password, String inviteCode) {}
  public record PasswordChange(String currentPassword,String newPassword) {}
}
