package com.gamerboxd.gamerboxd.controllers;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.Notification;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.services.NotificationService;
import com.gamerboxd.gamerboxd.services.UserService;

@Controller
public class NotificationController {
    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
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

    // FOR TESTING ONLY
    @GetMapping("/notifications/push")
    public String pushNotification(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        User user = this.userService.getUserById(currentUser.getId()).orElseThrow();
        this.notificationService.pushNotification("Test", "testing!", user);
        return "OK";
    }
}
