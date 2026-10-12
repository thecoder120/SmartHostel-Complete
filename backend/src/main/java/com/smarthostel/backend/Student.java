package com.smarthostel.backend;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.time.LocalDate;
@Entity @Table(name="students")
public class Student {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @NotBlank @Size(max=100) @Column(nullable=false, length=100) private String name;
  @Email @NotBlank @Size(max=254) @Column(nullable=false, unique=true, length=254) private String email;
  @Size(max=32) @Column(length=32) private String phone;
  @Size(max=120) @Column(length=120) private String college;
  @Size(max=32) @Column(length=32) private String roomNumber;
  @Column private Boolean active = true;
  @Column private LocalDate admittedAt = LocalDate.now();
  @Column private LocalDate checkedInAt;
  @Column private LocalDate checkedOutAt;
  public Student() {}
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getName(){return name;} public void setName(String v){name=trim(v);}
  public String getEmail(){return email;} public void setEmail(String v){email=v==null?null:v.trim().toLowerCase(Locale.ROOT);}
  public String getPhone(){return phone;} public void setPhone(String v){phone=trim(v);}
  public String getCollege(){return college;} public void setCollege(String v){college=trim(v);}
  public String getRoomNumber(){return roomNumber;} public void setRoomNumber(String v){roomNumber=v==null||v.isBlank()?null:v.trim().toUpperCase(Locale.ROOT);}
  public boolean isActive(){return active == null || active;}
  public void setActive(boolean value){active=value;}
  public LocalDate getAdmittedAt(){return admittedAt;}
  public void setAdmittedAt(LocalDate value){admittedAt=value;}
  public LocalDate getCheckedInAt(){return checkedInAt;}
  public void setCheckedInAt(LocalDate value){checkedInAt=value;}
  public LocalDate getCheckedOutAt(){return checkedOutAt;}
  public void setCheckedOutAt(LocalDate value){checkedOutAt=value;}
  private static String trim(String value){return value==null?null:value.trim();}
}
