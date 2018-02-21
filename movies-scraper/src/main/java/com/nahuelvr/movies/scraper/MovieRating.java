package com.nahuelvr.movies.scraper;

public class MovieRating {
  private String id;
  private String movieCountry;
  private double rating;
  private int votes;
  private String user;
  private String userCity;
  private String userCountry;
  private double userRating;
  private double ratingUtility;
  private long timestamp;
  private String review1;
  private String review2;

  public MovieRating() {
    super();
  }

  public MovieRating(String id, String movieCountry, double rating, int votes, String user,
      String userCity, String userCountry, double userRating, double ratingUtility, long timestamp,
      String review1, String review2) {
    super();
    this.id = id;
    this.movieCountry = movieCountry;
    this.rating = rating;
    this.votes = votes;
    this.user = user;
    this.userCity = userCity;
    this.userCountry = userCountry;
    this.userRating = userRating;
    this.ratingUtility = ratingUtility;
    this.timestamp = timestamp;
    this.review1 = review1;
    this.review2 = review2;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMovieCountry() {
    return movieCountry;
  }

  public void setMovieCountry(String movieCountry) {
    this.movieCountry = movieCountry;
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

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getUserCity() {
    return userCity;
  }

  public void setUserCity(String userCity) {
    this.userCity = userCity;
  }

  public String getUserCountry() {
    return userCountry;
  }

  public void setUserCountry(String userCountry) {
    this.userCountry = userCountry;
  }

  public double getUserRating() {
    return userRating;
  }

  public void setUserRating(double userRating) {
    this.userRating = userRating;
  }

  public double getRatingUtility() {
    return ratingUtility;
  }

  public void setRatingUtility(double ratingUtility) {
    this.ratingUtility = ratingUtility;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public String getReview1() {
    return review1;
  }

  public void setReview1(String review1) {
    this.review1 = review1;
  }

  public String getReview2() {
    return review2;
  }

  public void setReview2(String review2) {
    this.review2 = review2;
  }

  @Override
  public String toString() {
    return "MovieRating [id=" + id + ", rating=" + rating + ", user=" + user + "]";
  }

}


