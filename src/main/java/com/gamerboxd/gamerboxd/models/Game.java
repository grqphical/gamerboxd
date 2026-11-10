package com.gamerboxd.gamerboxd.models;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A cached storage of certain fields of one game from `IGBDService`.
 * 
 * All members are volatile since they are subject to change by IGDB.
 * Setters are not provided except for ratingGamerboxd as that member 
 * variable is independant from IGDB. 
 * 
 * Seperate issue to re-cache the remaining fields according to changes
 * from IGDB.
 */
@Entity
@Table(name = "Games")
public class Game {
    @Id
    private int id; // should match the id from IGDB
    private String name;
    private String summary;
    private String releaseDate;
    private Double ratingIGDB;
    private Double ratingGamerboxd; // New Average = Old Average + (x - Old Average)/(n + 1)
    private List<String> genres;    // api returns strings so will have to call a different api to translate them
    private List<String> artworkLinks;

    public Game() {}

    public Game(int id, String name, String summary, String releaseDate, Double ratingIGDB, Double ratingGamerboxd,
            List<String> genres, List<String> artworkLinks) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.releaseDate = releaseDate;
        this.ratingIGDB = ratingIGDB;
        this.ratingGamerboxd = ratingGamerboxd;
        this.genres = genres;
        this.artworkLinks = artworkLinks;
    }
 
    public int getId() {
        return id;
    }

    public String geName() {
        return name;
    }

    public String getSummary() {
        return summary;
    }

    public String getReleaseData() {
        return releaseDate;
    }

    public double getRatingIGDB() {
        return ratingIGDB;
    }

    public double getRatingGamerboxd() {
        return ratingGamerboxd;
    }

    public List<String> getGenres() {
        return genres;
    }
    
    public List<String> getArtworkLinks() {
        return artworkLinks;
    }
}