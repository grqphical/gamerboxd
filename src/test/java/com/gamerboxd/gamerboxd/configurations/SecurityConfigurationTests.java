package com.gamerboxd.gamerboxd.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;

public class SecurityConfigurationTests {
    private final PasswordEncoder encoder = new SecurityConfig().passwordEncoder();

    @Test
    void encodesAndMatches() {
        String hash = encoder.encode("s3cret-passphrase");

        assertThat(hash).isNotEqualTo("s3cret-passphrase");
        assertThat(encoder.matches("s3cret-passphrase", hash)).isTrue();
        assertThat(encoder.matches("wrong", hash)).isFalse();
    }

    @Test
    void sameInputProducesDifferentHashes() {
        assertThat(encoder.encode("pw")).isNotEqualTo(encoder.encode("pw"));
    }

}
