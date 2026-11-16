package com.gamerboxd.gamerboxd.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gamerboxd.gamerboxd.models.ReviewLike;

import jakarta.transaction.Transactional;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Integer> {
    long countByReviewId(int reivewId);

    boolean existsByReviewIdAndUserId(int reviewId, int userId);

    // One row per liked review: [reviewId, likeCount]
    @Query("select l.review.id, count(l) from ReviewLike l where l.review.id in :reviewIds group by l.review.id")
    List<Object[]> countByReviewIds(@Param("reviewIds") Collection<Integer> reviewIds);

    @Query("select l.review.id from ReviewLike l where l.user.id = :userId and l.review.id in :reviewIds")
    Set<Integer> findLikedReviewIds(@Param("reviewIds") Collection<Integer> reviewIds, @Param("userId") int userId);

    Optional<ReviewLike> findByReviewIdAndUserId(int reviewId, int userId);

    @Transactional
    void deleteByReviewId(int reviewId);

    void deleteByUserId(int userId);
}
