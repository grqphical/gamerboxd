package com.gamerboxd.gamerboxd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamerboxd.gamerboxd.models.Notification;
import com.gamerboxd.gamerboxd.models.User;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findAllByUserOrderByTimestamp(User user);

    void deleteAllByUser(User user);
}
