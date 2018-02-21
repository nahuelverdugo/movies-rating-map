package com.nahuelvr.filmAffinity;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import com.nahuelvr.movies.scraper.Movie;
import com.nahuelvr.movies.scraper.MovieRating;
import com.nahuelvr.movies.scraper.MoviesScraper;

public class FilmAffinityScraperTest {

  private MoviesScraper filmAffinityScraper;

  @Before
  public void beforeMethod() {
    filmAffinityScraper = new MoviesScraper();
  }

  @Test
  public void testReviews() throws IllegalAccessException, IllegalArgumentException,
      InvocationTargetException, NoSuchMethodException, SecurityException {
    Method method = MoviesScraper.class.getDeclaredMethod("getMovieReviews", String.class);
    method.setAccessible(true);
    List<MovieRating> reviews = (List<MovieRating>) method.invoke(filmAffinityScraper, "252584");
    assertTrue(!reviews.isEmpty());
    assertTrue(reviews.size() >= 1);
    assertTrue(!MoviesScraper.getCookies().isEmpty());
    assertTrue(MoviesScraper.getCookies().size() > 0);
  }

  @Test
  public void testMovies() throws IllegalAccessException, IllegalArgumentException,
      InvocationTargetException, NoSuchMethodException, SecurityException {
    Method method = MoviesScraper.class.getDeclaredMethod("retrieveMoviesIdsFrom", List.class,
        String.class);
    method.setAccessible(true);
    List<Movie> moviesIds = new ArrayList<>();
    method.invoke(filmAffinityScraper, moviesIds,
        "https://www.filmaffinity.com/es/allfilms_X_Y.html");
    assertTrue(!moviesIds.isEmpty());
    assertTrue(moviesIds.size() > 57);
    assertTrue(!MoviesScraper.getCookies().isEmpty());
    assertTrue(MoviesScraper.getCookies().size() > 0);
  }
}
