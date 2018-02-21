// The simplest possible sbt build file is just one line:

scalaVersion := "2.11.8"

name := "movies-spark"
organization := "com.nahuelvr"
version := "1.0"

fork := true

val sparkVersion = "2.2.1"

resolvers ++= Seq(
  "apache-snapshots" at "http://repository.apache.org/snapshots/"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion,
  "net.liftweb" %% "lift-json" % "3.2.0"
  
)
