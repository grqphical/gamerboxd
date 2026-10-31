package com.gamerboxd.gamerboxd.configurations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.repositories.RememberMeTokenRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SignUpFlowTest {
    private static final String PASSWORD = "s3cret-passphrase";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RememberMeTokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private ResultActions signUp(String username, String email, String password, String confirmPassword)
            throws Exception {
        return mockMvc.perform(post("/signup").with(csrf())
                .param("username", username)
                .param("email", email)
                .param("password", password)
                .param("confirmPassword", confirmPassword));
    }

    @Test
    void form_isPublic() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

    @Test
    void signUp_storesHashAndAllowsSignIn() throws Exception {
        signUp("ada", "Ada@Example.com", PASSWORD, PASSWORD)
                .andExpect(redirectedUrl("/login?registered"));

        User saved = userRepository.findByEmail("ada@example.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        assertThat(saved.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPassword())).isTrue();

        // Case differs from the stored email on purpose
        mockMvc.perform(post("/login").with(csrf()).param("email", "ADA@example.com").param("password", PASSWORD))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("ada@example.com"));
    }

    @Test
    void loginPage_confirmsRegistration() throws Exception {
        mockMvc.perform(get("/login").queryParam("registered", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Account created.")));
    }

    @Test
    void signUp_mismatchedPasswords_showsErrorAndKeepsInput() throws Exception {
        signUp("ada", "ada@example.com", PASSWORD, "something-else")
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(content().string(containsString("Passwords do not match.")))
                .andExpect(content().string(containsString("value=\"ada@example.com\"")))
                .andExpect(content().string(not(containsString(PASSWORD))));

        assertThat(userRepository.findAll()).isEmpty();
    }

    @Test
    void signUp_duplicateEmail_showsError() throws Exception {
        signUp("ada", "ada@example.com", PASSWORD, PASSWORD);

        signUp("grace", "ADA@example.com", PASSWORD, PASSWORD)
                .andExpect(view().name("signup"))
                .andExpect(content().string(containsString("That email is already registered.")));

        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    void signUp_cannotChooseRole() throws Exception {
        mockMvc.perform(post("/signup").with(csrf())
                .param("username", "ada")
                .param("email", "ada@example.com")
                .param("password", PASSWORD)
                .param("confirmPassword", PASSWORD)
                .param("role", "ADMIN"));

        assertThat(userRepository.findByEmail("ada@example.com").orElseThrow().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void signUp_withoutCsrf_isRejected() throws Exception {
        mockMvc.perform(post("/signup")
                .param("username", "ada")
                .param("email", "ada@example.com")
                .param("password", PASSWORD)
                .param("confirmPassword", PASSWORD))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findAll()).isEmpty();
    }
}
