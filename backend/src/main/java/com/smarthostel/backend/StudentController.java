package com.smarthostel.backend;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.time.LocalDate;
@RestController @RequestMapping("/api/students")
public class StudentController {
  private final StudentRepository repo;
  private final RoomRepository rooms;
  private final AuditService audit;
  public StudentController(StudentRepository repo, RoomRepository rooms,AuditService audit){this.repo=repo;this.rooms=rooms;this.audit=audit;}
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
}
