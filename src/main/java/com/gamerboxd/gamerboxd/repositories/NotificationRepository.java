package com.gamerboxd.gamerboxd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamerboxd.gamerboxd.models.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUser_IdOrderByTimestamp(int userId);

    List<Notification> findAllByUser_IdAndReadFalseOrderByTimestamp(int userId);

    void deleteAllByUser_Id(int userId);
}
