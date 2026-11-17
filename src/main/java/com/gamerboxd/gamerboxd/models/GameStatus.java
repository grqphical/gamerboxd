package com.gamerboxd.gamerboxd.models;

public enum GameStatus {
    PLAYED("Played"),
    WANT_TO_PLAY("Want to play"),
    DROPPED("Dropped");

    private final String label;

    GameStatus(String label) {
        this.label = label;
    }

    /** The name shown to users. */
    public String getLabel() {
        return label;
    }
}
