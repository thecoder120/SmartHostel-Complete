package com.smarthostel.backend;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Creates Spring Session's MySQL tables idempotently on startup. */
@Component
public class SessionSchemaInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbc;

  public SessionSchemaInitializer(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void run(ApplicationArguments args) {
    jdbc.execute("""
        CREATE TABLE IF NOT EXISTS SPRING_SESSION (
          PRIMARY_ID CHAR(36) NOT NULL,
          SESSION_ID CHAR(36) NOT NULL,
          CREATION_TIME BIGINT NOT NULL,
          LAST_ACCESS_TIME BIGINT NOT NULL,
          MAX_INACTIVE_INTERVAL INT NOT NULL,
          EXPIRY_TIME BIGINT NOT NULL,
          PRINCIPAL_NAME VARCHAR(100),
          CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID),
          CONSTRAINT SPRING_SESSION_IX1 UNIQUE (SESSION_ID),
          INDEX SPRING_SESSION_IX2 (EXPIRY_TIME),
          INDEX SPRING_SESSION_IX3 (PRINCIPAL_NAME)
        ) ENGINE=InnoDB ROW_FORMAT=DYNAMIC
        """);
    jdbc.execute("""
        CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
          SESSION_PRIMARY_ID CHAR(36) NOT NULL,
          ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
          ATTRIBUTE_BYTES BLOB NOT NULL,
          CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
          CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
            REFERENCES SPRING_SESSION(PRIMARY_ID) ON DELETE CASCADE
        ) ENGINE=InnoDB ROW_FORMAT=DYNAMIC
        """);
  }
}
