package com.gamerboxd.gamerboxd.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Notification;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.services.NotificationService;
import com.gamerboxd.gamerboxd.services.UserService;

@Controller
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String notificationsView(@AuthenticationPrincipal AuthenticatedUser currentUser, Model model) {
        if (currentUser == null)
            return "redirect:/login";

        List<Notification> notifications = this.notificationService.getUserNotifications(currentUser.getId());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("notificationCount",
                this.notificationService.getUserNotificationUnreadCount(currentUser.getId()));
        model.addAttribute("notifications", notifications);
        return "notifications/list";
    }

    @PostMapping("/notifications/{notificationId}/read")
    public String readNotification(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long notificationId) {
        if (currentUser == null)
            return "redirect:/login";

        Optional<Notification> notification = this.notificationService.getNotificationById(notificationId);

        if (notification.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "notification with given ID not found");

        if (notification.get().getUser().getId() != currentUser.getId())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");

        this.notificationService.setNotificationRead(notificationId);
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String readAllUserNotifications(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (currentUser == null)
            return "redirect:/login";

        this.notificationService.setAllUserNotificationsRead(currentUser.getId());

        return "redirect:/notifications";
    }

    @PostMapping("/notifications/{notificationId}/delete")
    public String deleteNotification(@AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long notificationId) {
        if (currentUser == null)
            return "redirect:/login";

        Optional<Notification> notification = this.notificationService.getNotificationById(notificationId);

        if (notification.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "notification with given ID not found");

        if (notification.get().getUser().getId() != currentUser.getId())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");

        this.notificationService.clearNotification(notificationId);
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/clear")
    public String deleteAllUserNotifications(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        if (currentUser == null)
            return "redirect:/login";

        this.notificationService.clearNotificationsForUser(currentUser.getId());
        return "redirect:/notifications";
    }
}
