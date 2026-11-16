package com.gamerboxd.gamerboxd.configurations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.gamerboxd.gamerboxd.models.User;
import com.gamerboxd.gamerboxd.models.UserRole;
import com.gamerboxd.gamerboxd.repositories.RememberMeTokenRepository;
import com.gamerboxd.gamerboxd.repositories.ReviewRepository;
import com.gamerboxd.gamerboxd.repositories.UserRepository;
import com.gamerboxd.gamerboxd.services.GoogleAuthService;
import com.gamerboxd.gamerboxd.services.GoogleAuthService.PendingGoogleAccount;

@SpringBootTest(properties = { "GOOGLE_CLIENT_ID=test-client", "GOOGLE_CLIENT_SECRET=test-secret" })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleSignInFlowTest {
    private static final String GOOGLE_ID = "108000000000000000000";
    private static final String EMAIL = "ada@gmail.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RememberMeTokenRepository tokenRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    /** A session as the success handler leaves it for a first-time Google visitor. */
    private MockHttpSession googleVerifiedSession() {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        googleAuthService.setPendingAccount(request, new PendingGoogleAccount(GOOGLE_ID, EMAIL, "Ada Lovelace"));
        return session;
    }

    @Test
    void startingGoogleSignIn_redirectsToGoogleWithOurClient() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .startsWith("https://accounts.google.com/o/oauth2/v2/auth?")
                        .contains("client_id=test-client")
                        .contains("scope=openid%20email%20profile")
                        .contains("/login/oauth2/code/google"));
    }

    @Test
    void loginAndSignupPages_offerGoogle() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(content().string(containsString("/oauth2/authorization/google")));
        mockMvc.perform(get("/signup"))
                .andExpect(content().string(containsString("/oauth2/authorization/google")));
    }

    @Test
    void loginPage_explainsWhyAnExistingEmailWasNotLinked() throws Exception {
        mockMvc.perform(get("/login").param("googleEmailTaken", ""))
                .andExpect(content().string(containsString("link Google from your settings")));
    }

    @Test
    void finishPage_isPublicAndPrefillsUsername() throws Exception {
        mockMvc.perform(get("/auth/google/finish").session(googleVerifiedSession()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(EMAIL)))
                .andExpect(content().string(containsString("value=\"Ada_Lovelace\"")));
    }

    @Test
    void finishPage_withoutGoogleSignIn_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/auth/google/finish"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void finish_createsLinkedAccountAndSignsIn() throws Exception {
        MockHttpSession session = googleVerifiedSession();

        mockMvc.perform(post("/auth/google/finish").session(session).with(csrf()).param("username", "ada"))
                .andExpect(redirectedUrl("/"));

        User saved = userRepository.findByGoogleID(GOOGLE_ID).orElseThrow();
        assertThat(saved.getUsername()).isEqualTo("ada");
        assertThat(saved.getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);
        mockMvc.perform(get("/").session(session))
                .andExpect(authenticated().withUsername(EMAIL));
    }

    @Test
    void finish_withoutGoogleSignIn_createsNothing() throws Exception {
        mockMvc.perform(post("/auth/google/finish").with(csrf())
                .param("username", "ada")
                .param("googleId", GOOGLE_ID)
                .param("email", EMAIL))
                .andExpect(redirectedUrl("/login"))
                .andExpect(unauthenticated());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void finish_withoutCsrf_isRejected() throws Exception {
        mockMvc.perform(post("/auth/google/finish").session(googleVerifiedSession()).param("username", "ada"))
                .andExpect(status().isForbidden());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void finish_takenUsername_showsErrorAndAllowsRetry() throws Exception {
        userRepository.save(new User(0, "ada", "other@example.com", "pw", UserRole.USER));
        MockHttpSession session = googleVerifiedSession();

        mockMvc.perform(post("/auth/google/finish").session(session).with(csrf()).param("username", "ada"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("That username is already taken.")));
        mockMvc.perform(post("/auth/google/finish").session(session).with(csrf()).param("username", "ada2"))
                .andExpect(redirectedUrl("/"));

        assertThat(userRepository.findByGoogleID(GOOGLE_ID).orElseThrow().getUsername()).isEqualTo("ada2");
    }

    @Test
    void settings_offersLinkThenUnlink() throws Exception {
        MockHttpSession session = googleVerifiedSession();
        mockMvc.perform(post("/auth/google/finish").session(session).with(csrf()).param("username", "ada"));
        User ada = userRepository.findByGoogleID(GOOGLE_ID).orElseThrow();
        String settings = "/users/" + ada.getId() + "/settings";

        mockMvc.perform(get(settings).session(session))
                .andExpect(content().string(containsString("/auth/google/unlink")))
                .andExpect(content().string(not(containsString("/auth/google/link"))));

        mockMvc.perform(post("/auth/google/unlink").session(session).with(csrf()))
                .andExpect(redirectedUrl(settings));

        assertThat(userRepository.findById(ada.getId()).orElseThrow().getGoogleID()).isNull();
        mockMvc.perform(get(settings).session(session))
                .andExpect(content().string(containsString("/auth/google/link")));
    }

    @Test
    void link_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/auth/google/link"))
                .andExpect(redirectedUrl("/login"));
    }
}
