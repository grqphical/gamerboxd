package com.gamerboxd.gamerboxd.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.gamerboxd.gamerboxd.models.Notification;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.repositories.NotificationRepository;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationService(NotificationRepository notificationRepository,
            UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    public Optional<Notification> getNotificationById(Long notificationId) {
        return this.notificationRepository.findById(notificationId);
    }

    public List<Notification> getUserNotifications(int userId) {
        return notificationRepository.findAllByUser_IdOrderByTimestamp(userId);
    }

    public void pushNotification(String title, String description, User recipient) {
        if (userService.getUserById(recipient.getId()).isEmpty())
            throw new IllegalArgumentException("User not found with ID: " + recipient.getId());

        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setTimestamp(Instant.now());
        notification.setTitle(title);
        notification.setDescription(description);

        this.notificationRepository.save(notification);
    }

    public void clearNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        this.notificationRepository.delete(notification);
    }

    public void clearNotificationsForUser(int userId) {
        this.notificationRepository.deleteAllByUser_Id(userId);
    }

    public int getUserNotificationUnreadCount(int userId) {
        return this.notificationRepository.findAllByUser_IdAndReadFalse(userId).size();
    }

    public void setNotificationRead(Long notificationId) {
        Notification notification = this.notificationRepository.findById(notificationId).orElseThrow();
        notification.setRead(true);
        this.notificationRepository.save(notification);
    }
}
