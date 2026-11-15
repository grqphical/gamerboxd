package com.gamerboxd.gamerboxd.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamerboxd.gamerboxd.models.ReviewLike;

import jakarta.transaction.Transactional;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Integer> {
    long countByReviewId(int reivewId);
    boolean existsByReviewIdAndUserId(int reviewId, int userId);
    Optional<ReviewLike> findByReviewIdAndUserId(int reviewId, int userId);
    @Transactional 
    void deletebyReviewId(int reviewId);
}
