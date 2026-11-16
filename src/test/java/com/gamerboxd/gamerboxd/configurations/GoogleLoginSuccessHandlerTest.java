package com.gamerboxd.gamerboxd.configurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.GoogleAuthService;
import com.gamerboxd.gamerboxd.services.GoogleAuthService.PendingGoogleAccount;
import com.gamerboxd.gamerboxd.services.UserService;

class GoogleLoginSuccessHandlerTest {
    private static final String GOOGLE_ID = "108000000000000000000";
    private static final String EMAIL = "ada@gmail.com";

    private UserService userService;
    private GoogleAuthService googleAuthService;
    private GoogleLoginSuccessHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);
    private final User olivia = new User(2, "olivia", "olivia@gmail.com", "pw", UserRole.USER);

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        googleAuthService = new GoogleAuthService();
        handler = new GoogleLoginSuccessHandler(userService, googleAuthService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        when(userService.getUserByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Authentication googleLogin(boolean emailVerified) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", GOOGLE_ID);
        claims.put("email", EMAIL);
        claims.put("email_verified", emailVerified);
        claims.put("name", "Ada Lovelace");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), claims);
        OidcUser google = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken);
        return new OAuth2AuthenticationToken(google, google.getAuthorities(), "google");
    }

    /** Who the session says is signed in after the handler ran, or null. */
    private Object sessionPrincipal() {
        SecurityContext context = (SecurityContext) request.getSession()
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        return context == null || context.getAuthentication() == null
                ? null
                : context.getAuthentication().getPrincipal();
    }

    @Test
    void linkedAccount_signsInAsThatUser() throws Exception {
        when(userService.getUserByGoogleId(GOOGLE_ID)).thenReturn(Optional.of(john));

        handler.onAuthenticationSuccess(request, response, googleLogin(true));

        assertEquals("/", response.getRedirectedUrl());
        AuthenticatedUser principal = assertInstanceOf(AuthenticatedUser.class, sessionPrincipal());
        assertEquals(1, principal.getId());
    }

    @Test
    void newVisitor_isSentToPickUsernameWithoutBeingSignedIn() throws Exception {
        handler.onAuthenticationSuccess(request, response, googleLogin(true));

        assertEquals("/auth/google/finish", response.getRedirectedUrl());
        assertNull(sessionPrincipal());
        assertEquals(new PendingGoogleAccount(GOOGLE_ID, EMAIL, "Ada Lovelace"),
                googleAuthService.getPendingAccount(request));
        verify(userService, never()).registerWithGoogle(any(), any(), any());
    }

    @Test
    void emailAlreadyRegistered_isNotLinkedAutomatically() throws Exception {
        when(userService.isEmailRegistered(EMAIL)).thenReturn(true);

        handler.onAuthenticationSuccess(request, response, googleLogin(true));

        assertEquals("/login?googleEmailTaken", response.getRedirectedUrl());
        assertNull(sessionPrincipal());
        assertNull(googleAuthService.getPendingAccount(request));
        verify(userService, never()).linkGoogleAccount(any(), anyInt());
    }

    @Test
    void unverifiedEmail_isRejected() throws Exception {
        handler.onAuthenticationSuccess(request, response, googleLogin(false));

        assertEquals("/login?googleUnverified", response.getRedirectedUrl());
        assertNull(sessionPrincipal());
        assertNull(googleAuthService.getPendingAccount(request));
    }

    @Test
    void linking_attachesGoogleToTheSignedInUser() throws Exception {
        googleAuthService.startLinking(request, 1);
        when(userService.getUserById(1)).thenReturn(Optional.of(john));

        handler.onAuthenticationSuccess(request, response, googleLogin(true));

        verify(userService).linkGoogleAccount(GOOGLE_ID, 1);
        assertEquals("/users/1/settings", response.getRedirectedUrl());
        assertEquals(1, assertInstanceOf(AuthenticatedUser.class, sessionPrincipal()).getId());
    }

    @Test
    void linking_googleAccountOwnedByAnotherUser_keepsCurrentUserSignedIn() throws Exception {
        googleAuthService.startLinking(request, 1);
        when(userService.getUserById(1)).thenReturn(Optional.of(john));
        when(userService.getUserByGoogleId(GOOGLE_ID)).thenReturn(Optional.of(olivia));
        doThrow(new IllegalArgumentException("taken")).when(userService).linkGoogleAccount(GOOGLE_ID, 1);

        handler.onAuthenticationSuccess(request, response, googleLogin(true));

        assertEquals("/users/1/settings?googleTaken", response.getRedirectedUrl());
        assertEquals(1, assertInstanceOf(AuthenticatedUser.class, sessionPrincipal()).getId());
    }

    @Test
    void linking_appliesToOneSignInOnly() throws Exception {
        googleAuthService.startLinking(request, 1);
        when(userService.getUserById(1)).thenReturn(Optional.of(john));
        handler.onAuthenticationSuccess(request, response, googleLogin(true));

        assertNull(googleAuthService.consumeLinkingUserId(request));
    }
}
