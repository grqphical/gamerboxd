package com.gamerboxd.gamerboxd.configurations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.gamerboxd.gamerboxd.models.AuthenticatedUser;
import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.repositories.RememberMeTokenRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {
    private static final String EMAIL = "ada@example.com";
    private static final String PASSWORD = "s3cret-passphrase";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RememberMeTokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(new User(0, "ada", EMAIL, passwordEncoder.encode(PASSWORD), UserRole.USER));
    }

    private ResultActions signIn(String password, boolean rememberMe) throws Exception {
        var request = post("/login").with(csrf()).param("email", EMAIL).param("password", password);
        if (rememberMe) {
            request.param("remember-me", "on");
        }
        return mockMvc.perform(request);
    }

    @Test
    void anonymous_canBrowseButNotWrite() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/reviews")).andExpect(status().isOk());
        mockMvc.perform(get("/reviews/new")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void signIn_wrongPassword_isRejected() throws Exception {
        signIn("wrong", false)
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void signIn_exposesUserId() throws Exception {
        MockHttpSession session = (MockHttpSession) signIn(PASSWORD, false)
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername(EMAIL))
                .andExpect(cookie().doesNotExist("remember-me"))
                .andReturn().getRequest().getSession();

        AuthenticatedUser currentUser = (AuthenticatedUser) mockMvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andReturn().getModelAndView().getModel().get("currentUser");

        assertThat(currentUser.getId()).isEqualTo(user.getId());
    }

    @Test
    void rememberMe_storesTokenAndSignsInWithoutSession() throws Exception {
        Cookie rememberMe = signIn(PASSWORD, true)
                .andExpect(cookie().httpOnly("remember-me", true))
                .andReturn().getResponse().getCookie("remember-me");

        assertThat(tokenRepository.findAll()).singleElement()
                .satisfies(token -> assertThat(token.getEmail()).isEqualTo(EMAIL));

        mockMvc.perform(get("/reviews/new").cookie(rememberMe))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(EMAIL));
    }

    @Test
    void signOut_removesRememberMeToken() throws Exception {
        MvcResult signedIn = signIn(PASSWORD, true).andReturn();
        Cookie rememberMe = signedIn.getResponse().getCookie("remember-me");
        MockHttpSession session = (MockHttpSession) signedIn.getRequest().getSession();

        mockMvc.perform(post("/logout").with(csrf()).session(session).cookie(rememberMe))
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(cookie().maxAge("remember-me", 0));

        assertThat(tokenRepository.findAll()).isEmpty();
        mockMvc.perform(get("/reviews/new").cookie(rememberMe)).andExpect(redirectedUrl("/login"));
    }

    @Test
    void signOut_withoutCsrf_isRejected() throws Exception {
        mockMvc.perform(post("/logout")).andExpect(status().isForbidden());
    }
}
