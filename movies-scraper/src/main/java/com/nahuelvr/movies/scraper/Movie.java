package com.nahuelvr.movies.scraper;

public class Movie {
  private String movieId;
  private String name;
  private int year;
  private double rating;
  private int votes;

  public Movie() {
    super();
  }

  public Movie(String movieId, String name, int year, double rating, int votes) {
    super();
    this.movieId = movieId;
    this.name = name;
    this.year = year;
    this.rating = rating;
    this.votes = votes;
  }

  public String getMovieId() {
    return movieId;
  }

  public void setMovieId(String movieId) {
    this.movieId = movieId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public double getRating() {
    return rating;
  }

  public void setRating(double rating) {
    this.rating = rating;
  }

  public int getVotes() {
    return votes;
  }

  public void setVotes(int votes) {
    this.votes = votes;
  }

  @Override
  public String toString() {
    return "Movie [movieId=" + movieId + ", name=" + name + ", year=" + year + "]";
  }

}
