package com.smarthostel.backend;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentRepository extends JpaRepository<Student, Long> {
  boolean existsByEmailIgnoreCase(String email);
  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
  boolean existsByRoomNumberIgnoreCase(String roomNumber);
  java.util.List<Student> findAllByRoomNumberIgnoreCase(String roomNumber);
}
