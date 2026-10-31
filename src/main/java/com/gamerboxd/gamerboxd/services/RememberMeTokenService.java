package com.gamerboxd.gamerboxd.services;

import java.util.Date;

import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gamerboxd.gamerboxd.models.RememberMeToken;
import com.gamerboxd.gamerboxd.repositories.RememberMeTokenRepository;

@Service
public class RememberMeTokenService implements PersistentTokenRepository {
    private final RememberMeTokenRepository tokenRepository;

    public RememberMeTokenService(RememberMeTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        tokenRepository.save(new RememberMeToken(token.getSeries(), token.getUsername(), token.getTokenValue(),
                token.getDate().toInstant()));
    }

    @Override
    @Transactional
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        tokenRepository.findById(series).ifPresent(token -> {
            token.setToken(tokenValue);
            token.setLastUsed(lastUsed.toInstant());
        });
    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        return tokenRepository.findById(seriesId)
                .map(token -> new PersistentRememberMeToken(token.getEmail(), token.getSeries(), token.getToken(),
                        Date.from(token.getLastUsed())))
                .orElse(null);
    }

    @Override
    @Transactional
    public void removeUserTokens(String email) {
        tokenRepository.deleteByEmail(email);
    }
}
