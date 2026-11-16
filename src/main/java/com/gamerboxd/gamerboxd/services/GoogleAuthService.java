package com.gamerboxd.gamerboxd.services;

import java.io.Serializable;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Session bookkeeping for Google sign-in.
 *
 * Everything Google told us stays in the server-side session, never in a URL
 * or form field, so a visitor can't claim a Google account they didn't sign
 * in to.
 */
@Service
public class GoogleAuthService {
    private static final String PENDING_ACCOUNT = "google.pendingAccount";
    private static final String LINK_USER_ID = "google.linkUserId";

    /** A verified Google account that has no Gamerboxd account yet. */
    public record PendingGoogleAccount(String googleId, String email, String name) implements Serializable {
    }

    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    /** Signs the user in as the rest of the app expects, with an AuthenticatedUser principal. */
    public void signIn(User user, HttpServletRequest request, HttpServletResponse response) {
        AuthenticatedUser principal = new AuthenticatedUser(user);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    /** Drops the temporary Google authentication without signing anyone in. */
    public void signOut(HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
    }

    public void setPendingAccount(HttpServletRequest request, PendingGoogleAccount account) {
        request.getSession().setAttribute(PENDING_ACCOUNT, account);
    }

    /** The Google account waiting for a username, or null. */
    public PendingGoogleAccount getPendingAccount(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (PendingGoogleAccount) session.getAttribute(PENDING_ACCOUNT);
    }

    public void clearPendingAccount(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(PENDING_ACCOUNT);
        }
    }

    /** Remembers that the next Google sign-in should be linked to this signed-in user. */
    public void startLinking(HttpServletRequest request, int userId) {
        request.getSession().setAttribute(LINK_USER_ID, userId);
    }

    /** Returns the user waiting to link a Google account (or null) and forgets it. */
    public Integer consumeLinkingUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Integer userId = (Integer) session.getAttribute(LINK_USER_ID);
        session.removeAttribute(LINK_USER_ID);
        return userId;
    }

    /** Turns a Google display name (or email) into a starting point that fits our username rules. */
    public String suggestUsername(PendingGoogleAccount account) {
        String fromName = clean(account.name());
        if (fromName.length() >= 3) {
            return fromName;
        }
        String email = account.email() == null ? "" : account.email();
        String fromEmail = clean(email.contains("@") ? email.substring(0, email.indexOf('@')) : email);
        return fromEmail.length() >= 3 ? fromEmail : "";
    }

    private String clean(String value) {
        String cleaned = value == null ? "" : value.trim().replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_-]", "");
        return cleaned.length() > 30 ? cleaned.substring(0, 30) : cleaned;
    }
}
