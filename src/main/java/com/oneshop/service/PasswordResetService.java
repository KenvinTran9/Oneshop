package com.oneshop.service;

import com.oneshop.entity.PasswordResetAccount;
import com.oneshop.repository.PasswordResetAccountRepository;
import com.oneshop.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
    public static final String INVALID_LINK = "Liên kết không hợp lệ, đã hết hạn hoặc đã được sử dụng. Vui lòng yêu cầu liên kết mới.";
    public record Requested(String email, String fullName, String token, String deliveryId) {
        @Override public String toString() { return "PasswordResetRequested[redacted]"; }
    }
    private final UserRepository users;
    private final PasswordResetAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final ApplicationEventPublisher events;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository users, PasswordResetAccountRepository accounts,
                                PasswordEncoder encoder, ApplicationEventPublisher events) {
        this.users = users; this.accounts = accounts; this.encoder = encoder; this.events = events;
    }

    @Transactional
    public void request(String email) {
        var user = users.findForCartChange(email.trim().toLowerCase(Locale.ROOT)).filter(u -> u.isActive());
        if (user.isEmpty()) return;
        var account = accounts.findForUpdate(user.get().getId()).orElseGet(() -> new PasswordResetAccount(user.get()));
        Instant now = Instant.now();
        if (account.getRequestedAt() != null && account.getRequestedAt().isAfter(now.minusSeconds(60))) return;
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        account.setTokenHash(digest(token)); account.setExpiresAt(now.plusSeconds(900)); account.setRequestedAt(now);
        accounts.saveAndFlush(account);
        events.publishEvent(new Requested(user.get().getEmail(), user.get().getFullName(), token, UUID.randomUUID().toString()));
    }

    @Transactional(readOnly = true)
    public boolean isUsable(String token) {
        return validFormat(token) && accounts.isUsable(digest(token), Instant.now());
    }

    @Transactional
    public void reset(String token, String password) {
        if (!validFormat(token)) throw new IllegalArgumentException(INVALID_LINK);
        String hash = digest(token);
        String email = accounts.findEmailByTokenHash(hash).orElseThrow(() -> new IllegalArgumentException(INVALID_LINK));
        // Request and consume both lock user -> reset state. The second query is a current locking read.
        var user = users.findForCartChange(email).filter(u -> u.isActive()).orElseThrow(() -> new IllegalArgumentException(INVALID_LINK));
        var account = accounts.findForUpdate(user.getId()).orElseThrow(() -> new IllegalArgumentException(INVALID_LINK));
        if (!hash.equals(account.getTokenHash()) || account.getExpiresAt() == null || !account.getExpiresAt().isAfter(Instant.now()))
            throw new IllegalArgumentException(INVALID_LINK);
        if (password == null || password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Mật khẩu phải có từ 8 ký tự và tối đa 72 byte.");
        user.setPassword(encoder.encode(password));
        account.setTokenHash(null); account.setExpiresAt(null);
        account.setCredentialVersion(Math.addExact(account.getCredentialVersion(), 1));
        accounts.saveAndFlush(account);
    }

    private boolean validFormat(String token) { return token != null && token.matches("[A-Za-z0-9_-]{43}"); }
    private String digest(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible); }
    }
}
