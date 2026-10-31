package com.gamerboxd.gamerboxd.models;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "RememberMeTokens")
public class RememberMeToken {
    @Id
    private String series;
    private String email;
    private String token;
    private Instant lastUsed;

    public RememberMeToken() {
    }

    public RememberMeToken(String series, String email, String token, Instant lastUsed) {
        this.series = series;
        this.email = email;
        this.token = token;
        this.lastUsed = lastUsed;
    }

    public String getSeries() {
        return series;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(Instant lastUsed) {
        this.lastUsed = lastUsed;
    }
}
