package com.gamerboxd.gamerboxd.configurations;

import java.io.IOException;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.services.GoogleAuthService;
import com.gamerboxd.gamerboxd.services.GoogleAuthService.PendingGoogleAccount;
import com.gamerboxd.gamerboxd.services.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs once Google has confirmed who the visitor is. Swaps Spring's Google
 * principal for one of our own users, by one of four routes:
 * linking to the signed-in user, signing in a linked user, refusing an email
 * that already has an account, or sending a new visitor to pick a username.
 */
@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final UserService userService;
    private final GoogleAuthService googleAuthService;

    public GoogleLoginSuccessHandler(UserService userService, GoogleAuthService googleAuthService) {
        this.userService = userService;
        this.googleAuthService = googleAuthService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OidcUser google = (OidcUser) authentication.getPrincipal();
        String googleId = google.getSubject();
        Optional<User> linkedUser = userService.getUserByGoogleId(googleId);

        Integer linkingUserId = googleAuthService.consumeLinkingUserId(request);
        Optional<User> linkingUser = linkingUserId == null ? Optional.empty() : userService.getUserById(linkingUserId);
        if (linkingUser.isPresent()) {
            response.sendRedirect(linkToSignedInUser(linkingUser.get(), googleId, request, response));
            return;
        }

        if (linkedUser.isPresent()) {
            googleAuthService.signIn(linkedUser.get(), request, response);
            response.sendRedirect("/");
            return;
        }

        // Nobody is signed in to Gamerboxd yet, so don't leave the Google principal in the session
        googleAuthService.signOut(request, response);

        if (!Boolean.TRUE.equals(google.getEmailVerified()) || google.getEmail() == null) {
            response.sendRedirect("/login?googleUnverified");
        } else if (userService.isEmailRegistered(google.getEmail())) {
            // Not linked automatically: whoever registered that email here never proved they own it
            response.sendRedirect("/login?googleEmailTaken");
        } else {
            googleAuthService.setPendingAccount(request,
                    new PendingGoogleAccount(googleId, google.getEmail(), google.getFullName()));
            response.sendRedirect("/auth/google/finish");
        }
    }

    private String linkToSignedInUser(User user, String googleId, HttpServletRequest request,
            HttpServletResponse response) {
        String settings = "/users/" + user.getId() + "/settings";
        // Either way they stay signed in as themselves, not as the Google principal
        googleAuthService.signIn(user, request, response);
        try {
            userService.linkGoogleAccount(googleId, user.getId());
            return settings;
        } catch (IllegalArgumentException e) {
            return settings + "?googleTaken";
        }
    }
}
