package com.gamerboxd.gamerboxd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.gamerboxd.gamerboxd.models.ReviewComment;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Integer> {
    List<ReviewComment> findByReviewIdOrderByCreatedAtAsc(int reviewId);

    @Transactional
    void deleteByReviewId(int reviewId);
}
