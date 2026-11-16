package com.gamerboxd.gamerboxd.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.gamerboxd.gamerboxd.models.ReviewComment;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Integer> {
    List<ReviewComment> findByReviewIdOrderByCreatedAtAsc(int reviewId);

    long countByReviewId(int reviewId);

    // One row per commented review: [reviewId, commentCount]
    @Query("select c.review.id, count(c) from ReviewComment c where c.review.id in :reviewIds group by c.review.id")
    List<Object[]> countByReviewIds(@Param("reviewIds") Collection<Integer> reviewIds);

    @Transactional
    void deleteByReviewId(int reviewId);

    void deleteByUserId(int userId);

    // Comments left by anyone on this user's reviews
    void deleteByReviewUserId(int userId);
}
