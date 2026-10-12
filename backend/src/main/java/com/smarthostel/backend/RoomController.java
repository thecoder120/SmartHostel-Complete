package com.smarthostel.backend;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
@RestController @RequestMapping("/api/rooms")
public class RoomController {
  private final RoomRepository repo;
  private final StudentRepository students;
  private final AuditService audit;
  public RoomController(RoomRepository repo, StudentRepository students,AuditService audit){this.repo=repo;this.students=students;this.audit=audit;}
  @GetMapping public List<Room> all(){return repo.findAll().stream().map(this::withOccupancy).toList();}
  @GetMapping("/{id}") public Room one(@PathVariable Long id){return withOccupancy(repo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found")));}
  @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional public Room create(@Valid @RequestBody Room r){
    r.setId(null);
    if(repo.existsByRoomNumberIgnoreCase(r.getRoomNumber())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Room number already exists");
    if(r.getCapacity()<1||r.getCapacity()>1000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Room capacity must be between 1 and 1000");
    Room saved=repo.save(r);audit.record("ROOM_CREATED","ROOM",saved.getId());return saved;
  }
  @Transactional
  @PutMapping("/{id}") public Room update(@PathVariable Long id,@Valid @RequestBody Room in){
    Room r=repo.lockByIdForUpdate(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Room not found"));
    if(repo.existsByRoomNumberIgnoreCaseAndIdNot(in.getRoomNumber(),id)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Room number already exists");
    String oldNumber=r.getRoomNumber();
    if(in.getCapacity()<1||in.getCapacity()>1000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Room capacity must be between 1 and 1000");
    long occupied=students.findAllByRoomNumberIgnoreCase(oldNumber).stream().filter(Student::isActive).count();
    if(in.getCapacity()<occupied) throw new ResponseStatusException(HttpStatus.CONFLICT,"Capacity cannot be lower than current occupancy");
    r.setRoomNumber(in.getRoomNumber()); r.setRoomType(in.getRoomType()); r.setPrice(in.getPrice()); r.setCapacity(in.getCapacity());
    Room saved=repo.save(r);
    if(!oldNumber.equalsIgnoreCase(saved.getRoomNumber())){
      List<Student> assigned=students.findAllByRoomNumberIgnoreCase(oldNumber);
      assigned.forEach(student->student.setRoomNumber(saved.getRoomNumber()));
      students.saveAll(assigned);
    }
    audit.record("ROOM_UPDATED","ROOM",id);return saved;
  }
  private Room withOccupancy(Room room){
    int occupied=(int)students.findAllByRoomNumberIgnoreCase(room.getRoomNumber()).stream().filter(Student::isActive).count();
    room.setOccupiedBeds(occupied); room.setAvailableBeds(Math.max(0,room.getCapacity()-occupied)); room.setAvailable(occupied<room.getCapacity());
    return room;
  }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void delete(@PathVariable Long id){
    Room room=one(id);
    if(students.existsByRoomNumberIgnoreCase(room.getRoomNumber())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Reassign students before deleting this room");
    repo.delete(room);audit.record("ROOM_DELETED","ROOM",id);
  }
}
