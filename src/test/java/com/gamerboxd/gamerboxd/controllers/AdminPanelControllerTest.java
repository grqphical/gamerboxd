package com.gamerboxd.gamerboxd.controllers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.services.UserService;

class AdminPanelControllerTest {
    private UserService userService;
    private MockMvc mockMvc;

    private final User admin = new User(1, "admin", "admin@gmail.com", "pw", UserRole.ADMIN);
    private final User john = new User(2, "john", "john@gmail.com", "pw", UserRole.USER);

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminPanelController(userService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                // the "admin" view shares its name with the /admin URL, which the default resolver rejects
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void signInAs(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private MockHttpServletRequestBuilder createUserPost() {
        return post("/admin/createUser")
                .param("username", "olivia")
                .param("email", "olivia@gmail.com")
                .param("password", "s3cret-passphrase")
                .param("role", "USER")
                .param("privacy", "public")
                .param("description", "Hi")
                .param("pronouns", "she/her");
    }

    @Test
    void panel_admin_listsUsers() throws Exception {
        signInAs(admin);
        when(userService.getAllUsers()).thenReturn(List.of(admin, john));

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attribute("users", List.of(admin, john)));
    }

    @Test
    void panel_regularUser_redirectsHome() throws Exception {
        signInAs(john);

        mockMvc.perform(get("/admin"))
                .andExpect(redirectedUrl("/"));

        verifyNoInteractions(userService);
    }

    @Test
    void panel_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/admin/1/delete", "/admin/1/toggleRole", "/admin/1/editDescription" })
    void userActions_regularUser_changeNothing(String url) throws Exception {
        signInAs(john);

        mockMvc.perform(post(url).param("description", "Hacked"))
                .andExpect(redirectedUrl("/"));

        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/admin/1/delete", "/admin/1/toggleRole", "/admin/1/editDescription" })
    void userActions_anonymous_changeNothing(String url) throws Exception {
        mockMvc.perform(post(url))
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(userService);
    }

    @Test
    void createUser_regularUser_changesNothing() throws Exception {
        signInAs(john);

        mockMvc.perform(createUserPost())
                .andExpect(redirectedUrl("/"));

        verifyNoInteractions(userService);
    }

    @Test
    void delete_admin_deletesUser() throws Exception {
        signInAs(admin);

        mockMvc.perform(post("/admin/2/delete"))
                .andExpect(redirectedUrl("/admin"));

        verify(userService).deleteUser(2);
    }

    @Test
    void toggleRole_admin_togglesRole() throws Exception {
        signInAs(admin);

        mockMvc.perform(post("/admin/2/toggleRole"))
                .andExpect(redirectedUrl("/admin"));

        verify(userService).toggleRole(2);
    }

    @Test
    void editDescription_admin_savesDescription() throws Exception {
        signInAs(admin);

        mockMvc.perform(post("/admin/2/editDescription").param("description", "New bio"))
                .andExpect(redirectedUrl("/admin"));

        verify(userService).editDescription(2, "New bio");
    }

    @Test
    void editDescription_admin_missingDescriptionClearsIt() throws Exception {
        signInAs(admin);

        mockMvc.perform(post("/admin/2/editDescription"))
                .andExpect(redirectedUrl("/admin"));

        verify(userService).editDescription(2, "");
    }

    @Test
    void createUser_admin_registersThenAppliesProfileFields() throws Exception {
        signInAs(admin);
        User olivia = new User(3, "olivia", "olivia@gmail.com", "hash", UserRole.USER);
        when(userService.register("olivia", "olivia@gmail.com", "s3cret-passphrase")).thenReturn(olivia);

        mockMvc.perform(createUserPost())
                .andExpect(redirectedUrl("/admin"));

        verify(userService).updateSettings(olivia, "olivia", "olivia@gmail.com", "s3cret-passphrase",
                "public", "Hi", "she/her");
    }
}
