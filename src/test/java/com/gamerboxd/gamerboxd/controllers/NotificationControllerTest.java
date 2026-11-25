package com.gamerboxd.gamerboxd.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Notification;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.NotificationService;
import com.gamerboxd.gamerboxd.services.UserService;

class NotificationControllerTest {
    private NotificationService notificationService;
    private MockMvc mockMvc;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);
    private final User olivia = new User(2, "olivia", "olivia@gmail.com", "pw", UserRole.USER);
    private final AuthenticatedUser johnPrincipal = new AuthenticatedUser(john);

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(johnPrincipal, null, johnPrincipal.getAuthorities()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void notificationsView_notAuthenticated_redirectsToLogin() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/notifications"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void notificationsView_authenticated_showsList() throws Exception {
        Notification notification = new Notification(1L, Instant.now(), "Test", "testing!", john);
        List<Notification> notifications = List.of(notification);

        when(notificationService.getUserNotifications(1)).thenReturn(notifications);
        when(notificationService.getUserNotificationUnreadCount(1)).thenReturn(2);

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications/list"))
                .andExpect(model().attribute("currentUser", johnPrincipal))
                .andExpect(model().attribute("notificationCount", 2))
                .andExpect(model().attribute("notifications", notifications));
    }

    @Test
    void readNotification_notAuthenticated_redirectsToLogin() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/notifications/5/read"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void readNotification_notFound_returnsNotFound() throws Exception {
        when(notificationService.getNotificationById(5L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/notifications/5/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    void readNotification_notOwner_returnsUnauthorized() throws Exception {
        Notification oliviasNotification = new Notification(5L, Instant.now(), "Test", "testing!", olivia);
        when(notificationService.getNotificationById(5L)).thenReturn(Optional.of(oliviasNotification));

        mockMvc.perform(post("/notifications/5/read"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readNotification_owner_redirectsToNotifications() throws Exception {
        Notification johnsNotification = new Notification(5L, Instant.now(), "Test", "testing!", john);
        when(notificationService.getNotificationById(5L)).thenReturn(Optional.of(johnsNotification));

        mockMvc.perform(post("/notifications/5/read"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        verify(notificationService).setNotificationRead(5L);
    }

    @Test
    void readAll_notAuthenticated_redirectsToLogin() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/notifications/read-all"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void readAll_authenticated_redirectsToNotifications() throws Exception {
        mockMvc.perform(post("/notifications/read-all"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        verify(notificationService).setAllUserNotificationsRead(1);
    }

    @Test
    void deleteNotification_notAuthenticated_redirectsToLogin() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/notifications/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void deleteNotification_notFound_returnsNotFound() throws Exception {
        when(notificationService.getNotificationById(5L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/notifications/5/delete"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNotification_notOwner_returnsUnauthorized() throws Exception {
        Notification oliviasNotification = new Notification(5L, Instant.now(), "Test", "testing!", olivia);
        when(notificationService.getNotificationById(5L)).thenReturn(Optional.of(oliviasNotification));

        mockMvc.perform(post("/notifications/5/delete"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteNotification_owner_redirectsToNotifications() throws Exception {
        Notification johnsNotification = new Notification(5L, Instant.now(), "Test", "testing!", john);
        when(notificationService.getNotificationById(5L)).thenReturn(Optional.of(johnsNotification));

        mockMvc.perform(post("/notifications/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        verify(notificationService).clearNotification(5L);
    }

    @Test
    void clear_notAuthenticated_redirectsToLogin() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/notifications/clear"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void clear_authenticated_redirectsToNotifications() throws Exception {
        mockMvc.perform(post("/notifications/clear"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/notifications"));

        verify(notificationService).clearNotificationsForUser(1);
    }
}
