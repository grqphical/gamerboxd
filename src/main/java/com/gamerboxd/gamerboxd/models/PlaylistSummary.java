package com.gamerboxd.gamerboxd.models;

/** A playlist as shown in lists, with its counts loaded in the same query. */
public record PlaylistSummary(int id, String title, String description, int userId, String username,
        long gameCount, long likeCount) {
}
