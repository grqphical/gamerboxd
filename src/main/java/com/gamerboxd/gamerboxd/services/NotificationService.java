package com.gamerboxd.gamerboxd.services;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.gamerboxd.gamerboxd.models.Notification;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.repositories.NotificationRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public List<Notification> getUserNotifications(int userId) {
        return notificationRepository.findAllByUser_IdOrderByTimestamp(userId);
    }

    public void pushNotification(String title, String description, int userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTimestamp(Instant.now());
        notification.setTitle(title);
        notification.setDescription(description);

        this.notificationRepository.save(notification);
    }

    public void clearNotification(int notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        this.notificationRepository.delete(notification);
    }

    public void clearNotificationsForUser(int userId) {
        this.notificationRepository.deleteAllByUser_Id(userId);
    }
}
