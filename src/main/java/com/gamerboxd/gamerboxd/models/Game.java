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
    @Column(columnDefinition = "text")
    private String summary;
    private String releaseDate;
    private Double ratingIGDB;
    private Double ratingGamerboxd;
    private int numRatingsGamerboxd; // the number of ratings done with the gamerboxd review system.
    private List<String> genres;        // api returns strings so will have to call a different api to translate them
    private List<String> artworkLinks;
    private List<String> platforms;
    private String coverLink;

    public Game() {}

    public Game(int id, String name, String summary, String releaseDate, Double ratingIGDB, Double ratingGamerboxd,
            List<String> genres, List<String> artworkLinks, List<String> platforms, String coverLink) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.releaseDate = releaseDate;
        this.ratingIGDB = ratingIGDB;
        this.ratingGamerboxd = ratingGamerboxd;
        this.genres = genres;
        this.artworkLinks = artworkLinks;
        this.platforms = platforms;
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

    public String getReleaseDate() {
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

    public int getNumRatingsGamerboxd() {
        return numRatingsGamerboxd;
    }

    public List<String> getPlatforms() {
        return platforms;
    }

    public String getCoverLink() {
        return coverLink;
    }

    /** Dont use this, use the one in GameService */
    public void updateRatingGamerboxd(int x) {
        ++this.numRatingsGamerboxd;
        this.ratingGamerboxd += (x - this.ratingGamerboxd) / (this.numRatingsGamerboxd);
    }
}