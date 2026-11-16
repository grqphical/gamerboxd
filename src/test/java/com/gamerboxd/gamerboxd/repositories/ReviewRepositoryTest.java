package com.gamerboxd.gamerboxd.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.Supplier;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.gamerboxd.gamerboxd.models.Review;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;

import jakarta.persistence.EntityManagerFactory;

@SpringBootTest
@ActiveProfiles("test")
class ReviewRepositoryTest {
    private static final int GAME_ID = 113112;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private User john;

    @BeforeEach
    void setUp() {
        clear();
        john = userRepository.save(new User(0, "john", "john@example.com", "pw", UserRole.USER));
        User olivia = userRepository.save(new User(0, "olivia", "olivia@example.com", "pw", UserRole.USER));
        User sam = userRepository.save(new User(0, "sam", "sam@example.com", "pw", UserRole.USER));
        reviewRepository.save(new Review(john, GAME_ID, 5, "Great"));
        reviewRepository.save(new Review(olivia, GAME_ID, 4, "Good"));
        reviewRepository.save(new Review(sam, GAME_ID, 3, "Fine"));
    }

    @AfterEach
    void clear() {
        reviewRepository.deleteAll();
        userRepository.deleteAll();
    }

    private long statementsFor(Supplier<List<Review>> query) {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        List<Review> reviews = query.get();
        reviews.forEach(r -> r.getUser().getUsername());
        long statements = stats.getPrepareStatementCount();
        stats.setStatisticsEnabled(false);
        return statements;
    }

    @Test
    void findAll_loadsAuthorsInOneQuery() {
        assertEquals(1, statementsFor(reviewRepository::findAllByOrderByCreatedAtDesc));
    }

    @Test
    void findByGame_loadsAuthorsInOneQuery() {
        assertEquals(1, statementsFor(() -> reviewRepository.findAllByGameIdOrderByCreatedAtDesc(GAME_ID)));
    }

    @Test
    void findByUser_loadsAuthorInOneQuery() {
        assertEquals(1, statementsFor(() -> reviewRepository.findAllByUserIdOrderByCreatedAtDesc(john.getId())));
    }

    @Test
    void findAll_returnsNewestFirst() {
        List<Review> reviews = reviewRepository.findAllByOrderByCreatedAtDesc();

        assertEquals(List.of("Fine", "Good", "Great"), reviews.stream().map(Review::getDescription).toList());
    }
}
