name := "mapreduce"

version := "1.0"

scalaVersion := "2.11.7"

exportJars := true

resolvers ++= Seq(
  "typesafe repo" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.11.7"
)