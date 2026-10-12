package com.smarthostel.backend;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Existing student rows predate the active/check-out fields and are treated as active residents. */
@Component
public class LegacyDataInitializer implements CommandLineRunner {
  private final StudentRepository students;
  public LegacyDataInitializer(StudentRepository students){this.students=students;}
  @Override @Transactional public void run(String... args){
    List<Student> legacy=students.findAll().stream().filter(student->!student.isActive()&&student.getCheckedOutAt()==null).toList();
    legacy.forEach(student->student.setActive(true));if(!legacy.isEmpty())students.saveAll(legacy);
  }
}
