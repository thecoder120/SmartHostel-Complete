package com.smarthostel.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class HostelWorkflowTests {
  @Test void checkInRejectsRoomAtCapacity(){
    StudentRepository students=mock(StudentRepository.class);RoomRepository rooms=mock(RoomRepository.class);AuditService audit=mock(AuditService.class);
    StudentController controller=new StudentController(students,rooms,audit);
    Room room=room("A-1",1);Student assigned=student(1L,"Current resident");assigned.setRoomNumber("A-1");
    Student candidate=student(2L,"New resident");
    when(students.findById(2L)).thenReturn(Optional.of(candidate));when(rooms.lockByRoomNumberForUpdate("A-1")).thenReturn(Optional.of(room));
    when(students.findAllByRoomNumberIgnoreCase("A-1")).thenReturn(List.of(assigned));
    ResponseStatusException error=assertThrows(ResponseStatusException.class,()->controller.checkIn(2L,new StudentController.RoomAssignment("A-1")));
    assertEquals(HttpStatus.CONFLICT,error.getStatusCode());verify(students,never()).save(any());
  }

  @Test void checkOutReleasesRoomAndKeepsStudentRecord(){
    StudentRepository students=mock(StudentRepository.class);RoomRepository rooms=mock(RoomRepository.class);AuditService audit=mock(AuditService.class);
    StudentController controller=new StudentController(students,rooms,audit);
    Student resident=student(8L,"Resident");resident.setRoomNumber("B-2");
    when(students.findById(8L)).thenReturn(Optional.of(resident));when(students.save(any())).thenAnswer(call->call.getArgument(0));
    Student checkedOut=controller.checkOut(8L);
    assertFalse(checkedOut.isActive());assertNull(checkedOut.getRoomNumber());assertNotNull(checkedOut.getCheckedOutAt());
    verify(students).save(resident);verify(audit).record("STUDENT_CHECKED_OUT","STUDENT",8L);
  }

  @Test void roomResponseCalculatesBedsFromActiveAssignments(){
    RoomRepository rooms=mock(RoomRepository.class);StudentRepository students=mock(StudentRepository.class);AuditService audit=mock(AuditService.class);
    RoomController controller=new RoomController(rooms,students,audit);Room room=room("C-3",3);
    Student one=student(1L,"One");one.setRoomNumber("C-3");Student two=student(2L,"Two");two.setRoomNumber("C-3");
    when(rooms.findAll()).thenReturn(List.of(room));when(students.findAllByRoomNumberIgnoreCase("C-3")).thenReturn(List.of(one,two));
    Room result=controller.all().getFirst();
    assertEquals(2,result.getOccupiedBeds());assertEquals(1,result.getAvailableBeds());assertTrue(result.isAvailable());
  }

  private Room room(String number,int capacity){Room value=new Room();value.setRoomNumber(number);value.setRoomType("Shared");value.setCapacity(capacity);value.setPrice(java.math.BigDecimal.ZERO);return value;}
  private Student student(Long id,String name){Student value=new Student();value.setId(id);value.setName(name);value.setEmail(name.toLowerCase().replace(' ','_')+"@example.invalid");return value;}
}
