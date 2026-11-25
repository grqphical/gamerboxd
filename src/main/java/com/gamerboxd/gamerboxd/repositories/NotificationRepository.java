package com.gamerboxd.gamerboxd.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gamerboxd.gamerboxd.models.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findAllByUser_IdOrderByTimestamp(int userId);

    void deleteAllByUser_Id(int userId);
}
