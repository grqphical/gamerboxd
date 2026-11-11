package com.gamerboxd.gamerboxd.models;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A cached storage of certain fields of one game from IGBDService.java.
 * 
 * <p>All members are volatile since they are subject to change by IGDB.
 * Setters are not provided except for ratingGamerboxd as that member 
 * variable is independant from IGDB. 
 * 
 * <p> We care about these fields:
 * <pre> .json
 * [
 *   {
 *      "id": ...,
 *      "name": ...,
 *      "summary": ...,
 *      "first_release_date": ...,
 *      "rating": ...,
 *      "genres": [...],
 *      "artworks": [...],
 *      "cover": ...
 *   }
 * ]
 * </pre>
 * 
 * <p> Seperate issue to re-cache the IGDB fields according to changes.
 * from IGDB.
 */
@Entity
@Table(name = "Games")
public class Game {
    @Id
    private int id;                     // should match the id from IGDB
    private String name;
    private String summary;
    private String releaseDate;
    private Double ratingIGDB;
    private Double ratingGamerboxd;
    private Double numRatingsGamerboxd; // the number of ratings done with the gamerboxd review system.
    private List<String> genres;        // api returns strings so will have to call a different api to translate them
    private List<String> artworkLinks;
    private String coverLink;

    public Game() {}

    public Game(int id, String name, String summary, String releaseDate, Double ratingIGDB, Double ratingGamerboxd,
            List<String> genres, List<String> artworkLinks, String coverLink) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.releaseDate = releaseDate;
        this.ratingIGDB = ratingIGDB;
        this.ratingGamerboxd = ratingGamerboxd;
        this.genres = genres;
        this.artworkLinks = artworkLinks;
        this.coverLink = coverLink;
    }
 
    public int getId() {
        return id;
    }

    public String getName() {
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

    public String getCoverLink() {
        return coverLink;
    }

    /** 
     * Call this to iteratively update this `Game`'s ratingGamerboxd using:
     * <pre> New Average = Old Average + (x - Old Average)/(n + 1) </pre>
     * @param x A newly added rating between [0, 5]
     */
    public void updateRatingGamerboxd(int x) {
        ++this.numRatingsGamerboxd;
        this.ratingGamerboxd += (x - this.ratingGamerboxd) / (this.numRatingsGamerboxd + 1.0);
    }
}