package com.gamerboxd.gamerboxd.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.GoogleAuthService;
import com.gamerboxd.gamerboxd.services.GoogleAuthService.PendingGoogleAccount;
import com.gamerboxd.gamerboxd.services.UserService;

class GoogleLoginControllerTest {
    private static final String GOOGLE_ID = "108000000000000000000";
    private static final String EMAIL = "ada@gmail.com";

    private UserService userService;
    private GoogleAuthService googleAuthService;
    private MockMvc mockMvc;
    private MockHttpSession session;

    private final User john = new User(1, "john", "john@gmail.com", "pw", UserRole.USER);

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        googleAuthService = new GoogleAuthService();
        mockMvc = MockMvcBuilders.standaloneSetup(new GoogleLoginController(userService, googleAuthService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        session = new MockHttpSession();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void signIn() {
        AuthenticatedUser principal = new AuthenticatedUser(john);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private void googleVerified(String name) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        googleAuthService.setPendingAccount(request, new PendingGoogleAccount(GOOGLE_ID, EMAIL, name));
    }

    private PendingGoogleAccount pendingAccount() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return googleAuthService.getPendingAccount(request);
    }

    @Test
    void finishForm_suggestsUsernameFromGoogleName() throws Exception {
        googleVerified("Ada Lovelace!");

        mockMvc.perform(get("/auth/google/finish").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("google/accountSetup"))
                .andExpect(model().attribute("email", EMAIL))
                .andExpect(model().attribute("username", "Ada_Lovelace"));
    }

    @Test
    void finishForm_unusableName_fallsBackToEmail() throws Exception {
        googleVerified("阿达");

        mockMvc.perform(get("/auth/google/finish").session(session))
                .andExpect(model().attribute("username", "ada"));
    }

    @Test
    void finishForm_withoutGoogleSignIn_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/auth/google/finish").session(session))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void finish_createsAccountFromSessionAndSignsIn() throws Exception {
        googleVerified("Ada Lovelace");
        User ada = new User(7, "ada", EMAIL, "hash", UserRole.USER);
        when(userService.registerWithGoogle("ada", EMAIL, GOOGLE_ID)).thenReturn(ada);

        mockMvc.perform(post("/auth/google/finish").session(session).param("username", "ada"))
                .andExpect(redirectedUrl("/"));

        SecurityContext context = (SecurityContext) session
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(context);
        assertEquals(7, ((AuthenticatedUser) context.getAuthentication().getPrincipal()).getId());
        assertNull(pendingAccount());
    }

    @Test
    void finish_ignoresGoogleIdentitySentByTheBrowser() throws Exception {
        googleVerified("Ada Lovelace");
        when(userService.registerWithGoogle(any(), any(), any()))
                .thenReturn(new User(7, "ada", EMAIL, "hash", UserRole.USER));

        mockMvc.perform(post("/auth/google/finish").session(session)
                .param("username", "ada")
                .param("googleId", "someone-else")
                .param("email", "victim@gmail.com"));

        verify(userService).registerWithGoogle("ada", EMAIL, GOOGLE_ID);
    }

    @Test
    void finish_withoutGoogleSignIn_createsNothing() throws Exception {
        mockMvc.perform(post("/auth/google/finish").session(session).param("username", "ada"))
                .andExpect(redirectedUrl("/login"));

        verify(userService, never()).registerWithGoogle(any(), any(), any());
    }

    @Test
    void finish_invalidUsername_showsErrorAndKeepsPendingAccount() throws Exception {
        googleVerified("Ada Lovelace");
        when(userService.registerWithGoogle("a", EMAIL, GOOGLE_ID))
                .thenThrow(new IllegalArgumentException("Username must be 3-30 characters."));

        mockMvc.perform(post("/auth/google/finish").session(session).param("username", "a"))
                .andExpect(status().isOk())
                .andExpect(view().name("google/accountSetup"))
                .andExpect(model().attribute("error", "Username must be 3-30 characters."))
                .andExpect(model().attribute("username", "a"));

        assertNotNull(pendingAccount());
        assertNull(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
    }

    @Test
    void link_signedIn_remembersUserAndGoesToGoogle() throws Exception {
        signIn();

        mockMvc.perform(get("/auth/google/link").session(session))
                .andExpect(redirectedUrl("/oauth2/authorization/google"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        assertEquals(1, googleAuthService.consumeLinkingUserId(request));
    }

    @Test
    void link_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/auth/google/link").session(session))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void unlink_signedIn_unlinksAndReturnsToSettings() throws Exception {
        signIn();

        mockMvc.perform(post("/auth/google/unlink"))
                .andExpect(redirectedUrl("/users/1/settings"));

        verify(userService).unlinkGoogleAccount(1);
    }

    @Test
    void unlink_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/auth/google/unlink"))
                .andExpect(redirectedUrl("/login"));

        verify(userService, never()).unlinkGoogleAccount(anyInt());
    }
}
