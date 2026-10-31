package com.gamerboxd.gamerboxd.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;                 // not modifiable in settings
    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    private String password;        // modifiable in settings with an "are you sure?"

    // get assigned default values on initialization

    private UserRole role;          // user by default or admin, not modifiable in settings
    private UserPrivacy privacy;    // public by default, private
    private String description;     // 250 char limit "" by default
    private String pronouns;        // "x / y" format or ""

    public User() {
    }

    public User(
        int id, String username, String email, 
        String password, UserRole role) 
    {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;

        this.privacy = UserPrivacy.public;
        this.description = "";
        this.pronouns = "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserPrivacy getPrivacy() {
        return privacy;
    }

    public void setPrivacy() {
        this.privacy = privacy;
    }

    public UserDescription getDescription() {
        return description;
    }

    public void setDescription() {
        this.description = description;
    }

    public String getPronouns() {
        return pronouns;
    }

    public void setPronouns() {
        this.pronouns = pronouns;
    }
}
