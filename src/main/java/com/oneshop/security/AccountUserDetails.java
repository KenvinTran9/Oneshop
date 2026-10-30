package com.oneshop.security;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Captures the credential generation alongside the password, including during concurrent resets. */
public class AccountUserDetails extends User {
    private static final long serialVersionUID = 1L;
    private final long credentialVersion;
    public AccountUserDetails(String email, String password, boolean active,
            Collection<? extends GrantedAuthority> authorities, long credentialVersion) {
        super(email, password, active, true, true, true, authorities);
        this.credentialVersion = credentialVersion;
    }
    public long getCredentialVersion() { return credentialVersion; }
    public static long versionOf(Object principal) {
        return principal instanceof AccountUserDetails details ? details.getCredentialVersion() : 0;
    }
}
