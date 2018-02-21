import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization.write
import java.io._

object Main extends App {

  case class Movie(ratingGiven: Map[String, Double], ratingReceived: Map[String, Double], votesGiven: Map[String, Int], votesReceived: Map[String, Int], avgRatingGiven: Double, avgRatingReceived: Double, films: Int)

  val spark = SparkSession.builder().master("local").appName("movies-spark").getOrCreate()
  spark.sparkContext.setLogLevel("WARN")

  val movies = spark.sqlContext.read.format("com.databricks.spark.csv").option("header", "true").option("inferSchema", "true").option("delimiter", "|").load("../movies-scraper/data.csv")
  val sc = spark.sqlContext.read.format("com.databricks.spark.csv").option("header", "true").option("inferSchema", "true").option("ignoreTrailingWhiteSpace", true).option("ignoreLeadingWhiteSpace", true).load("iso3countries.csv")
  sc.createOrReplaceTempView("sc")
  movies.createOrReplaceTempView("movies")

  val countrySet = (spark.sqlContext.sql("SELECT distinct(movieCountry) FROM movies").collect.map(e => e(0)) ++ spark.sqlContext.sql("SELECT distinct(userCountry) FROM movies").collect.map(_(0))).filter(_ != null).map(_.toString).toSet
  val countryISO3Map = countrySet.map(e => (e, spark.sqlContext.sql("SELECT iso3 FROM sc WHERE nombre LIKE '%" + e + "%'").collect.headOption.getOrElse(Row("")).getString(0))).toMap

  def countryToISO3Udf = udf((country: String) => countryISO3Map.get(country).getOrElse("").toString)

  val moviesWMC = movies.withColumn("movieCountry", countryToISO3Udf(col("movieCountry")))
  val moviesWUC = moviesWMC.withColumn("userCountry", countryToISO3Udf(col("userCountry")))
  moviesWUC.createOrReplaceTempView("movies")
  // TODO: this is not really nice
  val mapGivenRating = spark.sqlContext.sql("SELECT userCountry, movieCountry, avg(userRating) FROM movies GROUP BY userCountry, movieCountry").collect.groupBy(_.get(0).toString).transform((k, v) => v.map(e => (e(1).toString, e(2).toString.toDouble)).toMap)
  val mapReceivedRating = spark.sqlContext.sql("SELECT movieCountry, userCountry, avg(userRating) FROM movies GROUP BY movieCountry, userCountry").collect.groupBy(_.get(0).toString).transform((k, v) => v.map(e => (e(1).toString, e(2).toString.toDouble)).toMap)
  val mapGivenVotes = spark.sqlContext.sql("SELECT userCountry, movieCountry, count(userRating) FROM movies GROUP BY userCountry, movieCountry").collect.groupBy(_.get(0).toString).transform((k, v) => v.map(e => (e(1).toString, e(2).toString.toInt)).toMap)
  val mapReceivedVotes = spark.sqlContext.sql("SELECT movieCountry, userCountry, count(userRating) FROM movies GROUP BY movieCountry, userCountry").collect.groupBy(_.get(0).toString).transform((k, v) => v.map(e => (e(1).toString, e(2).toString.toInt)).toMap)
  val mapAvgGivenRating = spark.sqlContext.sql("SELECT userCountry, avg(userRating) FROM movies GROUP BY userCountry").collect.groupBy(_.get(0).toString).transform((k, v) => v(0)(1).toString.toDouble)
  val mapAvgReceivedRating = spark.sqlContext.sql("SELECT movieCountry, avg(userRating) FROM movies GROUP BY movieCountry").collect.groupBy(_.get(0).toString).transform((k, v) => v(0)(1).toString.toDouble)
  val filmsPerCountry = spark.sqlContext.sql("SELECT movieCountry, count(distinct(id)) FROM movies GROUP BY movieCountry").collect.groupBy(_.get(0).toString).transform((k, v) => v(0)(1).toString.toInt)

  val countries = countryISO3Map.values.toSet

  val result = countries.map(e => (e, Movie(mapGivenRating.get(e).getOrElse(null), mapReceivedRating.get(e).getOrElse(null), mapGivenVotes.get(e).getOrElse(null), mapReceivedVotes.get(e).getOrElse(null), mapAvgGivenRating.get(e).getOrElse(0.0), mapAvgReceivedRating.get(e).getOrElse(0.0), filmsPerCountry.get(e).getOrElse(0)))).toMap

  implicit val formats = DefaultFormats

  val file = new File("../movies-map/src/static/data.json")
  val bw = new BufferedWriter(new FileWriter(file))
  bw.write(write(result))
  bw.close()

}
