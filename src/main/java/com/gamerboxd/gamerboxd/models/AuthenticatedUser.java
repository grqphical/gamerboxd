package com.gamerboxd.gamerboxd.models;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// The signed-in user. Get it in a controller with @AuthenticationPrincipal.
public class AuthenticatedUser implements UserDetails {
    private final int id;
    private final String displayName;
    private final String email;
    private final String password;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUser(User user) {
        this.id = user.getId();
        this.displayName = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Spring Security's login identifier, which is the email here
    @Override
    public String getEmail() {  // was `getUsername()`
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
