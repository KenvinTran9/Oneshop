package com.oneshop.security;

import com.oneshop.repository.PasswordResetAccountRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;

/** Bind persistent login cookies to the same revocation generation used by sessions and JWTs. */
public class VersionedRememberMeServices extends TokenBasedRememberMeServices {
    private final PasswordResetAccountRepository accounts;
    public VersionedRememberMeServices(String key,UserDetailsService users,PasswordResetAccountRepository accounts){
        super(key,users,RememberMeTokenAlgorithm.SHA256);this.accounts=accounts;
    }
    @Override protected String makeTokenSignature(long expires,String username,String password,RememberMeTokenAlgorithm algorithm){
        long version=accounts.findCredentialVersion(username).orElse(0L);
        return super.makeTokenSignature(expires,username,password+":"+version,algorithm);
    }
}
