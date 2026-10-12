package com.smarthostel.backend;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RoomRepository extends JpaRepository<Room, Long> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select r from Room r where upper(r.roomNumber)=upper(:roomNumber)")
  java.util.Optional<Room> lockByRoomNumberForUpdate(@org.springframework.data.repository.query.Param("roomNumber") String roomNumber);
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select r from Room r where r.id=:id")
  java.util.Optional<Room> lockByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
  boolean existsByRoomNumberIgnoreCase(String roomNumber);
  boolean existsByRoomNumberIgnoreCaseAndIdNot(String roomNumber, Long id);
  java.util.Optional<Room> findByRoomNumberIgnoreCase(String roomNumber);
}
