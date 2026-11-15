package com.gamerboxd.gamerboxd.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name="ReviewLikes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "user_id"}) // no duplicate reviewLikes
)
public class ReviewLike {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false)
    private Review review;

    @ManyToOne(optional = false)
    private User user;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ReviewLike() {}

    public ReviewLike(Review review, User user) {
        this.review = review;
        this.user = user;
    }

    public int getId() { return id; }

    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
