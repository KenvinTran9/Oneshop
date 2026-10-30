package com.oneshop.repository;

import com.oneshop.entity.PasswordResetAccount;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PasswordResetAccountRepository extends JpaRepository<PasswordResetAccount, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PasswordResetAccount a where a.userId = :id")
    Optional<PasswordResetAccount> findForUpdate(@Param("id") Long id);

    @Query("select a.user.email from PasswordResetAccount a where a.tokenHash = :hash")
    Optional<String> findEmailByTokenHash(@Param("hash") String hash);

    @Query("select count(a) > 0 from PasswordResetAccount a where a.tokenHash = :hash and a.expiresAt > :now and a.user.active = true")
    boolean isUsable(@Param("hash") String hash, @Param("now") Instant now);

    @Query("select a.credentialVersion from PasswordResetAccount a where a.user.email = :email")
    Optional<Long> findCredentialVersion(@Param("email") String email);
}
