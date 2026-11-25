package com.gamerboxd.gamerboxd.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

        // make the "does the user exist" check pass; match this to what your service
        // actually calls
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
}
