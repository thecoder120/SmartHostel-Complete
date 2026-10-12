package com.smarthostel.backend;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.time.LocalDate;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
@RestController @RequestMapping("/api/students")
@org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class StudentController {
  private final StudentRepository repo;
  private final RoomRepository rooms;
  private final AuditService audit;
  private final StaffInviteRepository invites;
  private final SecureRandom random=new SecureRandom();
  @org.springframework.beans.factory.annotation.Autowired
  public StudentController(StudentRepository repo, RoomRepository rooms,AuditService audit,StaffInviteRepository invites){this.repo=repo;this.rooms=rooms;this.audit=audit;this.invites=invites;}
  public StudentController(StudentRepository repo, RoomRepository rooms,AuditService audit){this(repo,rooms,audit,null);}
  @GetMapping public List<Student> all(){return repo.findAll();}
  @GetMapping("/{id}") public Student one(@PathVariable Long id){return repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student not found"));}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional public Student create(@Valid @RequestBody Student s){
    s.setId(null);
    if(repo.existsByEmailIgnoreCase(s.getEmail())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already exists");
    requireRoom(s.getRoomNumber(), null);
    s.setActive(true);s.setCheckedOutAt(null);
    s.setAdmittedAt(LocalDate.now());
    if(s.getRoomNumber()!=null) s.setCheckedInAt(LocalDate.now());
    Student saved=repo.save(s); audit.record("STUDENT_CREATED","STUDENT",saved.getId()); return saved;
  }
  @Transactional
  @PutMapping("/{id}") public Student update(@PathVariable Long id,@Valid @RequestBody Student in){
    Student s=one(id);
    if(repo.existsByEmailIgnoreCaseAndIdNot(in.getEmail(),id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already exists");
    if(!s.isActive()&&in.getRoomNumber()!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Check in the student before assigning a room");
    requireRoom(in.getRoomNumber(), id);
    s.setName(in.getName()); s.setEmail(in.getEmail()); s.setPhone(in.getPhone()); s.setCollege(in.getCollege()); s.setRoomNumber(in.getRoomNumber()); Student saved=repo.save(s); audit.record("STUDENT_UPDATED","STUDENT",id); return saved;
  }
  @PostMapping("/{id}/check-in") @Transactional public Student checkIn(@PathVariable Long id,@RequestBody RoomAssignment assignment){
    Student student=one(id);
    if(assignment==null||assignment.roomNumber()==null||assignment.roomNumber().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a room before checking in");
    requireRoom(assignment.roomNumber(), id);
    student.setRoomNumber(assignment.roomNumber()); student.setActive(true); student.setCheckedInAt(LocalDate.now()); student.setCheckedOutAt(null);
    Student saved=repo.save(student);audit.record("STUDENT_CHECKED_IN","STUDENT",id);return saved;
  }
  @PostMapping("/{id}/check-out") @Transactional public Student checkOut(@PathVariable Long id){
    Student student=one(id);
    if(!student.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Student is already checked out");
    student.setActive(false); student.setRoomNumber(null); student.setCheckedOutAt(LocalDate.now());
    Student saved=repo.save(student);audit.record("STUDENT_CHECKED_OUT","STUDENT",id);return saved;
  }
  @PostMapping("/{id}/invite") @Transactional public Map<String,String> createStudentInvite(@PathVariable Long id){
    Student student=one(id);
    byte[] secret=new byte[24];random.nextBytes(secret);String code=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    Instant expiresAt=Instant.now().plus(7,ChronoUnit.DAYS);
    if(invites==null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Student invitations are unavailable");
    invites.save(new StaffInvite(hash(code),currentUser(),expiresAt,id));
    audit.record("STUDENT_INVITE_CREATED","STUDENT",id);
    return Map.of("code",code,"expiresAt",expiresAt.toString());
  }
  private void requireRoom(String roomNumber, Long excludeStudentId){
    if(roomNumber==null)return;
    Room room=rooms.lockByRoomNumberForUpdate(roomNumber).orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selected room does not exist"));
    long assigned=repo.findAllByRoomNumberIgnoreCase(roomNumber).stream()
        .filter(Student::isActive).filter(s->excludeStudentId==null||!s.getId().equals(excludeStudentId)).count();
    if(assigned>=room.getCapacity()) throw new ResponseStatusException(HttpStatus.CONFLICT,"That room is at capacity");
  }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void delete(@PathVariable Long id){
    if(!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Student not found"); repo.deleteById(id);audit.record("STUDENT_DELETED","STUDENT",id);
  }
  public record RoomAssignment(String roomNumber){}
  private static String hash(String value){try{return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
  private static String currentUser(){var auth=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();return auth==null?"system":auth.getName();}
}
