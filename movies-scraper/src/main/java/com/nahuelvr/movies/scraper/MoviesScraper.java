package com.nahuelvr.movies.scraper;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvException;

public class MoviesScraper {

  private static final Logger LOG = Logger.getLogger(MoviesScraper.class.getName());
  private static final List<String> agents = new ArrayList<>();
  static {
    agents.add(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.38 Safari/537.36");
    agents.add(
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Oupeng/10.2.1.86910 Safari/534.30");
    agents.add("Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");
    agents.add(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13) AppleWebKit/603.1.13 (KHTML, like Gecko) Version/10.1 Safari/603.1.13");
  }
  private static CookieManager cm;
  static {
    cm = new CookieManager();
    cm.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    CookieHandler.setDefault(new CookieManager(cm.getCookieStore(), CookiePolicy.ACCEPT_ALL));
  }
  private static String agent = null;
  private static LocalDateTime lastCookie;
  private static Random r = new Random();
  private static int requests = 0;
  private static final Pattern PATTERN_COUNTRY = Pattern.compile(".*\\((.*)\\)$");
  private static final DateTimeFormatter formatter =
      new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d 'de' MMMM 'de' yyyy")
          .toFormatter(new Locale("en", "EN"));
  private static final String PATTERN_CITY = "[ ]?(\\(.*\\))?\\(.*\\)$";
  // SCRAPER PARAMETERS
  private static final String MOVIES_CSV = "movies.csv";
  private static final String DATA_CSV = "data.csv";
  private static final int SLEEP_ERROR_OR_COOKIE = 25000;
  private static final int SLEEP_RANDOM = 2000;
  private static final int SLEEP_REQUEST = 4000;

  public MoviesScraper() {

  }

  public static void main(String[] args) throws IOException, InterruptedException, CsvException {

    LOG.log(Level.INFO, "Starting...");

    List<Movie> movies = new ArrayList<>();

    File fMovieIds = new File(MOVIES_CSV);
    if (!fMovieIds.exists()) {
      retrieveMovieIds(movies);
      Writer writer = Files.newBufferedWriter(Paths.get(MOVIES_CSV));
      StatefulBeanToCsv<Movie> beanToCsv = new StatefulBeanToCsvBuilder<Movie>(writer).build();
      beanToCsv.write(movies);
      writer.close();
    } else {
      Reader reader = Files.newBufferedReader(Paths.get(MOVIES_CSV));
      ColumnPositionMappingStrategy<Movie> strategy = new ColumnPositionMappingStrategy<>();
      strategy.setType(Movie.class);
      strategy.setColumnMapping("movieId", "name", "rating", "votes", "year");
      CsvToBean<Movie> csvToBean = new CsvToBeanBuilder<Movie>(reader).withMappingStrategy(strategy)
          .withSkipLines(1).build();
      movies = csvToBean.parse();
    }

    Writer writer = Files.newBufferedWriter(Paths.get(DATA_CSV));
    // separator '|' to avoid problems with spark sql
    StatefulBeanToCsv<MovieRating> data = new StatefulBeanToCsvBuilder<MovieRating>(writer).withSeparator('|').build();

    String agent = agents.get(r.nextInt(4));
    getCookies(agent);

    List<MovieRating> list;
    int count = 0;
    for (Movie movie : movies) {
      list = new ArrayList<>(getMovieReviews(movie.getMovieId()));
      data.write(list);
      LOG.log(Level.INFO, "Percentage of reviews extracted: " + ((double) ++count / movies.size()));
    }
    
    writer.close();
  }

  // MOVIES
  public static void retrieveMovieIds(List<Movie> moviesIds)
      throws IOException, InterruptedException {
    String url = "https://www.filmaffinity.com/es/allfilms_X_Y.html";
    char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toUpperCase().toCharArray();
    String filmsUrl;
    for (int i = 0; i < alphabet.length; i++) {
      LOG.log(Level.INFO, "Retrieving ids in: " + Character.toString(alphabet[i]));
      filmsUrl = url.replaceAll("X", Character.toString(alphabet[i]));
      retrieveMoviesIdsFrom(moviesIds, filmsUrl);
    }
    LOG.log(Level.INFO, "Retrieving ids in: 0-9");
    filmsUrl = url.replaceAll("X", "0-9");
    retrieveMoviesIdsFrom(moviesIds, filmsUrl);
  }

  public static void retrieveMoviesIdsFrom(List<Movie> moviesIds, String filmsUrl)
      throws IOException, InterruptedException {
    boolean ok = true;
    int start = 1;
    do {
      try {
        checkCookies();
        Connection con = getConnection(filmsUrl.replaceAll("Y", Integer.toString(start)));
        LOG.log(Level.INFO,
            "Retrieving ids in: " + filmsUrl.replaceAll("Y", Integer.toString(start)));
        Document doc = con.ignoreHttpErrors(true).execute().parse();
        requests++;
        ok = con.response().statusCode() == 200;
        if (ok) {
          extractMoviesFromDocument(moviesIds, doc);
          Thread.sleep(SLEEP_REQUEST + (long) r.nextInt(SLEEP_RANDOM));
          start++;
        } else {
          ok = false;
        }
      } catch (IOException e) {
        LOG.severe(
            "Something wrong happens in: " + filmsUrl.replaceAll("Y", Integer.toString(start)));
        Thread.sleep(SLEEP_ERROR_OR_COOKIE + (long) r.nextInt(SLEEP_RANDOM));
      }
    } while (ok);
  }

  public static void extractMoviesFromDocument(List<Movie> moviesIds, Document doc) {
    for (Element e : doc.select(".movie-card-1")) {
      String movieId = e.attr("data-movie-id");
      Elements eRating = e.select(".avgrat-box");
      String rating = eRating.isEmpty() || "--".equals(eRating.text()) ? null
          : e.select(".avgrat-box").text().replaceAll(",", ".");
      String votes = e.select(".ratcount-box").isEmpty() ? "0.0"
          : e.select(".ratcount-box").text().replaceAll(",", ".");
      String name = e.select(".mc-title>a").isEmpty() ? null
          : e.select(".mc-title>a").text().replaceAll(",", ".");
      String year = e.select(".mc-title").isEmpty() ? null : getMovieYear(e.select(".mc-title"));
      if (rating != null && name != null && year != null) {
        moviesIds.add(new Movie(movieId, name, Integer.parseInt(year), Double.parseDouble(rating),
            Integer.parseInt(votes.replaceAll("\\.", ""))));
      }
    }
  }

  private static String getMovieYear(Elements title) {
    title.select("a").remove();
    String year = title.text().replaceAll("[\\(\\) ]", "");
    return year.matches("[0-9]{4}") ? year : null;
  }

  // REVIEWS
  private static List<MovieRating> getMovieReviews(String movieId)
      throws IOException, InterruptedException {
    List<MovieRating> result = new ArrayList<>();
    String domain = "https://www.filmaffinity.com/es/reviews/Y/X.html";
    String filmUrl = domain.replaceAll("X", movieId);
    int start = 1;
    boolean ok = true;
    do {
      try {
        checkCookies();
        Connection con = getConnection(filmUrl.replaceAll("Y", Integer.toString(start)));
        Document doc = con.ignoreHttpErrors(true).execute().parse();
        requests++;
        ok = con.response().statusCode() == 200;
        if (ok) {
          result.addAll(getReviews(doc, movieId));
          Thread.sleep(SLEEP_REQUEST + (long) r.nextInt(SLEEP_RANDOM));
          start++;
        }
      } catch (IOException e) {
        LOG.severe(
            "Something wrong happens in: " + filmUrl.replaceAll("Y", Integer.toString(start)));
        Thread.sleep(SLEEP_ERROR_OR_COOKIE + (long) r.nextInt(SLEEP_RANDOM));
      }

    } while (ok);
    return result;
  }

  private static List<MovieRating> getReviews(Document doc, String id) {
    List<MovieRating> reviews = new ArrayList<>();
    String movieCountry = getCountry(doc.select(".movie-info"));
    String movieRating = doc.select("#movie-rat-avg").text().replaceAll(",", ".");
    String votes = doc.select("#movie-count-rat").text().replace(".", "");
    for (Element review : doc.select(".rw-item")) {
      String user = review.select(".mr-user-nick").text();
      String country =
          !review.select(".mr-user-country").isEmpty() ? review.select(".mr-user-country").text()
              : "-";
      double utlity = getUtility(review);
      String date =
          !review.select(".review-date").isEmpty() ? review.select(".review-date").text() : null;
      String rating = !review.select(".user-reviews-movie-rating").isEmpty()
          ? review.select(".user-reviews-movie-rating").text()
          : null;
      Elements reviewText = review.select(".review-text1");
      reviewText.select("br").remove();
      String reviewText1 = !reviewText.isEmpty() ? reviewText.text() : "";
      reviewText = review.select(".review-text2");
      reviewText.select("br").remove();
      String reviewText2 = !reviewText.isEmpty() ? reviewText.text() : "";
      try {
        if (rating != null && date != null) {
          reviews.add(new MovieRating(id, movieCountry, Double.parseDouble(movieRating),
              Integer.parseInt(votes), user, getUserCity(country), getUserCountry(country),
              Double.parseDouble(rating), utlity, getTimestamp(date),
              reviewText1.replaceAll("[\\u2028|\\u2029]", ""),
              reviewText2.replaceAll("[\\u2028|\\u2029]", "")));
        }
      } catch (NumberFormatException e) {
        LOG.severe("Error parsing reviews numbers in: " + doc.location());
      }
    }
    return reviews;
  }

  private static long getTimestamp(String date) {
    LocalDateTime dateTime = LocalDateTime.from(LocalDate.parse(date, formatter).atStartOfDay());
    return Timestamp.from(dateTime.toInstant(ZoneOffset.ofHours(0))).getTime();
  }

  private static String getUserCountry(String country) {
    if (country == null) {
      return "";
    }
    Matcher m = PATTERN_COUNTRY.matcher(country.trim());
    return (m.matches()) ? m.group(1) : "";
  }

  private static String getUserCity(String city) {
    return city != null ? city.trim().replaceAll(PATTERN_CITY, "").replaceAll("|", "") : "";
  }

  private static double getUtility(Element review) {
    Elements eUseful = review.select(".reviews-useful-bar > div");
    if (!eUseful.isEmpty()) {
      return Double.parseDouble(eUseful.attr("data-percent").replaceAll("%", ""));
    }
    Elements useful = review.select(".review-useful");
    if (!useful.isEmpty() && !useful.select("b").isEmpty() && useful.select("b").size() == 2) {
      Elements e = useful.select("b");
      if (!"0".equals(e.get(0).text()) && !"0".equals(e.get(1).text())) {
        return Double.parseDouble(e.get(0).text()) / Double.parseDouble(e.get(1).text()) * 100;
      }
    }
    return 0.0;
  }

  private static String getCountry(Elements select) {
    for (Element e : select.select("dt")) {
      if ("País".equals(e.text())) {
        return e.nextElementSibling().text();
      }
    }
    return "-";
  }

  // COOKIES AND CONNECTIONS
  public static void checkCookies() throws InterruptedException, IOException {
    if (agent == null || getCookies().isEmpty() || checkCookiesAge() || requests > 500) {
      agent = agents.get(r.nextInt(agents.size()));
      getCookies(agent);
      requests = 0;
      Thread.sleep(SLEEP_ERROR_OR_COOKIE);
    }
  }

  private static boolean checkCookiesAge() {
    return Math.abs(Duration.between(lastCookie, LocalDateTime.now()).toMinutes()) > 30;
  }

  private static Connection getConnection(String url) {
    return Jsoup.connect(url).header("User-Agent", agent).header("Upgrade-Insecure-Requests", "1")
        .header("Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        .header("Accept-Encoding", "gzip, deflate, sdch, br")
        .header("Accept-Language", "es-ES,es;q=0.8,de;q=0.6,en;q=0.4,it;q=0.2,fr;q=0.2")
        .header("Cookie", StringUtils.join(getCookies(), "; ")).method(Method.GET).timeout(120000);
  }

  private static void getCookies(String agent) throws IOException {
    cm.getCookieStore().removeAll();
    lastCookie = LocalDateTime.now();
    URLConnection conn = new URL("https://www.filmaffinity.com/es").openConnection();
    conn.setRequestProperty("User-Agent", agent);
    conn.setRequestProperty("Referer", "https://www.google.es");
    conn.setRequestProperty("Accept-Language",
        "es-ES,es;q=0.8,de;q=0.6,en;q=0.4,it;q=0.2,fr;q=0.2");
    conn.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch, br");
    conn.setRequestProperty("Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    conn.getContentType();
  }

  public static List<HttpCookie> getCookies() {
    long totalSize = cm.getCookieStore().getCookies().size();
    long noExpiredSize =
        cm.getCookieStore().getCookies().stream().filter(c -> !c.hasExpired()).count();
    if (totalSize > noExpiredSize) {
      cm.getCookieStore().removeAll();
      return new ArrayList<>();
    }
    return cm.getCookieStore().getCookies();
  }

}
