package com.smarthostel.backend;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  private final DataSource dataSource;
  public HealthController(DataSource dataSource){this.dataSource=dataSource;}
  @GetMapping("/healthz") public ResponseEntity<Map<String,String>> readiness(){
    try(Connection connection=dataSource.getConnection();Statement statement=connection.createStatement()){
      statement.execute("SELECT 1");return ResponseEntity.ok(Map.of("status","UP"));
    }catch(Exception ex){return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("status","DOWN"));}
  }
}
