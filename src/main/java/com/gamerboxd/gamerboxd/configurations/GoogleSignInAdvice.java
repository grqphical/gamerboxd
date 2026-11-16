package com.gamerboxd.gamerboxd.configurations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Lets templates hide the Google buttons when no Google credentials are configured. */
@ControllerAdvice
public class GoogleSignInAdvice {
    private final boolean enabled;

    public GoogleSignInAdvice(@Value("${GOOGLE_CLIENT_ID:}") String clientId) {
        this.enabled = !clientId.isBlank();
    }

    @ModelAttribute("googleSignInEnabled")
    public boolean googleSignInEnabled() {
        return enabled;
    }
}
