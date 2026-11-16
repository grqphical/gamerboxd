package com.gamerboxd.gamerboxd.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ReviewComments", indexes = {
        @Index(name = "idx_review_comments_review", columnList = "review_id"),
        @Index(name = "idx_review_comments_user", columnList = "user_id")
})
public class ReviewComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(optional = false)
    private Review review;

    @ManyToOne(optional = false)
    private User user;

    @Column(length = 1000)
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ReviewComment() {
    }

    public ReviewComment(Review review, User user, String content) {
        this.review = review;
        this.user = user;
        this.content = content;
    }

    public int getId() { return id; }
    public Review getReview() { return review; }
    public User getUser() { return user; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
