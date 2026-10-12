package com.smarthostel.backend;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffInviteRepository extends JpaRepository<StaffInvite,Long> {
  Optional<StaffInvite> findByCodeHash(String codeHash);
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select i from StaffInvite i where i.codeHash=:hash")
  Optional<StaffInvite> lockByCodeHash(@org.springframework.data.repository.query.Param("hash") String hash);
}
