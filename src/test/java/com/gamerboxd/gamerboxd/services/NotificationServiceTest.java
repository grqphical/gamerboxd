package com.gamerboxd.gamerboxd.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.gamerboxd.gamerboxd.models.Notification;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.repositories.NotificationRepository;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void pushNotification_savesNotificationForExistingUser() {
        User user = new User(1, "johndoe_", "johndoe@gmail.com", "hashed", UserRole.USER);

        when(userService.getUserById(1)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notificationService.pushNotification("Very Important", "Very Important message", user);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals("Very Important", saved.getTitle());
        assertEquals("Very Important message", saved.getDescription());
        assertEquals(user, saved.getUser());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void pushNotification_throwsWhenUserDoesNotExist() {
        User user = new User(99, "ghost", "ghost@gmail.com", "hashed", UserRole.USER);

        when(userService.getUserById(99)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> notificationService.pushNotification("Title", "Desc", user));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getUserNotifications_returnsWhatRepositoryReturns() {
        Notification n1 = new Notification();
        n1.setTitle("Very Important");
        Notification n2 = new Notification();
        n2.setTitle("Very Important 2");

        when(notificationRepository.findAllByUser_IdOrderByTimestamp(1)).thenReturn(List.of(n1, n2));

        List<Notification> result = notificationService.getUserNotifications(1);

        assertEquals(2, result.size());
        assertEquals("Very Important", result.get(0).getTitle());
        assertEquals("Very Important 2", result.get(1).getTitle());
    }

    @Test
    void getNotificationById_returnsNotificationWhenFound() {
        Long id = 1L;
        Notification notification = new Notification();
        notification.setTitle("Test");

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        Optional<Notification> result = notificationService.getNotificationById(id);

        assertEquals("Test", result.get().getTitle());
        verify(notificationRepository).findById(id);
    }

    @Test
    void getNotificationById_returnsEmptyWhenNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Notification> result = notificationService.getNotificationById(99L);

        assertEquals(false, result.isPresent());
    }

    @Test
    void clearNotification_deletesWhenFound() {
        Long id = 1L;
        Notification notification = new Notification();
        notification.setTitle("Test");

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        notificationService.clearNotification(id);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void clearNotification_throwsWhenNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> notificationService.clearNotification(99L));
    }

    @Test
    void clearNotificationsForUser_deletesAllForUser() {
        notificationService.clearNotificationsForUser(1);

        verify(notificationRepository).deleteAllByUser_Id(1);
    }

    @Test
    void getUserNotificationUnreadCount_returnsCount() {
        when(notificationRepository.findAllByUser_IdAndReadFalseOrderByTimestamp(1))
                .thenReturn(List.of(new Notification(), new Notification()));

        int count = notificationService.getUserNotificationUnreadCount(1);

        assertEquals(2, count);
    }

    @Test
    void setNotificationRead_marksAsRead() {
        Long id = 1L;
        Notification notification = new Notification();

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        notificationService.setNotificationRead(id);

        assertEquals(true, notification.getRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void setAllUserNotificationsRead_marksAllAsRead() {
        Notification n1 = new Notification();
        n1.setRead(false);
        Notification n2 = new Notification();
        n2.setRead(false);

        when(notificationRepository.findAllByUser_IdOrderByTimestamp(1))
                .thenReturn(new java.util.ArrayList<>(List.of(n1, n2)));

        notificationService.setAllUserNotificationsRead(1);

        assertEquals(true, n1.getRead());
        assertEquals(true, n2.getRead());
        verify(notificationRepository).saveAll(any(List.class));
    }
}
