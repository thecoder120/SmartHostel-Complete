package com.smarthostel.backend;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Locale;
@Entity @Table(name="rooms")
public class Room {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
  @NotBlank @Column(nullable=false, unique=true, length=32) private String roomNumber;
  @NotBlank @Size(max=32) @Column(nullable=false, length=32) private String roomType;
  @NotNull @DecimalMin("0.00") @DecimalMax("99999999.99") @Column(nullable=false, precision=10, scale=2) private BigDecimal price=BigDecimal.ZERO;
  @Column private Integer capacity=1;
  private boolean available=true;
  @Transient private int occupiedBeds;
  @Transient private int availableBeds=1;
  public Room() {}
  public Long getId(){return id;} public void setId(Long id){this.id=id;}
  public String getRoomNumber(){return roomNumber;} public void setRoomNumber(String v){roomNumber=v==null?null:v.trim().toUpperCase(Locale.ROOT);}
  public String getRoomType(){return roomType;} public void setRoomType(String v){roomType=v==null?null:v.trim();}
  public BigDecimal getPrice(){return price;} public void setPrice(BigDecimal v){price=v;}
  public int getCapacity(){return capacity==null||capacity<1?1:capacity;} public void setCapacity(Integer v){capacity=v;}
  public boolean isAvailable(){return available;} public void setAvailable(boolean v){available=v;}
  public int getOccupiedBeds(){return occupiedBeds;} public void setOccupiedBeds(int v){occupiedBeds=v;}
  public int getAvailableBeds(){return availableBeds;} public void setAvailableBeds(int v){availableBeds=v;}
}
