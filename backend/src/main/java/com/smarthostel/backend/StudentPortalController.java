package com.smarthostel.backend;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Read-only portal scoped to the single student linked to the signed-in account. */
@RestController
@RequestMapping("/api/student-portal")
@PreAuthorize("hasRole('STUDENT')")
public class StudentPortalController {
  private final UserAccountRepository accounts;
  private final StudentRepository students;
  private final RoomRepository rooms;

  public StudentPortalController(UserAccountRepository accounts, StudentRepository students, RoomRepository rooms) {
    this.accounts = accounts;
    this.students = students;
    this.rooms = rooms;
  }

  @GetMapping("/me")
  public Map<String, Object> me(Authentication authentication) {
    UserAccount account = accounts.findByUsernameIgnoreCase(authentication.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    if (account.getStudentId() == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This student account is not linked to a resident record");
    }
    Student student = students.findById(account.getStudentId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Your resident record is unavailable; contact hostel staff"));

    Map<String, Object> profile = new LinkedHashMap<>();
    profile.put("name", student.getName());
    profile.put("email", student.getEmail());
    profile.put("phone", student.getPhone());
    profile.put("college", student.getCollege());
    profile.put("active", student.isActive());
    profile.put("admittedAt", student.getAdmittedAt());
    profile.put("checkedInAt", student.getCheckedInAt());
    profile.put("checkedOutAt", student.getCheckedOutAt());
    profile.put("roomNumber", student.getRoomNumber());
    if (student.getRoomNumber() != null) {
      rooms.findByRoomNumberIgnoreCase(student.getRoomNumber()).ifPresent(room -> {
        profile.put("roomType", room.getRoomType());
        profile.put("monthlyPrice", room.getPrice());
      });
    }
    return profile;
  }
}
