package com.gamerboxd.gamerboxd.services;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gamerboxd.gamerboxd.models.Notification;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.repositories.NotificationRepository;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findAllByUserOrderByTimestamp(user);
    }

    public void pushNotification(String title, String description, User user) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTimestamp(Instant.now());
        notification.setTitle(title);
        notification.setDescription(description);

        this.notificationRepository.save(notification);
    }

    public void clearNotification(Notification notification) {
        this.notificationRepository.delete(notification);
    }

    public void clearNotificationsForUser(User user) {
        this.notificationRepository.deleteAllByUser(user);
    }
}
