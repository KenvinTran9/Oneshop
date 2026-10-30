package com.oneshop.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** One reset capability per account; only a SHA-256 digest is persisted. */
@Entity
@Table(name = "password_reset_accounts")
public class PasswordResetAccount {
    @Id @Column(name = "user_id") private Long userId;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    @Version @Column(nullable = false) private Long version;
    @Column(length = 64, unique = true) private String tokenHash;
    private Instant expiresAt;
    private Instant requestedAt;
    @Column(nullable = false) private long credentialVersion;

    public PasswordResetAccount() { }
    public PasswordResetAccount(User user) { this.userId = user.getId(); this.user = user; }
    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String value) { tokenHash = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { expiresAt = value; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant value) { requestedAt = value; }
    public long getCredentialVersion() { return credentialVersion; }
    public void setCredentialVersion(long value) { credentialVersion = value; }
}
