name := "perfberryapi"

version := "1.0.0"

lazy val `perfberryapi` = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.13.10"

libraryDependencies ++= Seq(
  guice,
  filters,
  specs2                   % Test,
  "com.typesafe.play"      %% "play-slick" % "5.0.0",
  "com.typesafe.play"      %% "play-slick-evolutions" % "5.0.0",
  "com.github.tminglei"    %% "slick-pg" % "0.19.3",
  "com.github.tminglei"    %% "slick-pg_play-json" % "0.19.3",
  "net.logstash.logback"   % "logstash-logback-encoder" % "6.3",
  "org.webjars"            % "swagger-ui" % "3.36.2",
  "com.tdunning"           % "t-digest" % "3.1",
  "com.beachape"           %% "enumeratum-play" % "1.6.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % Test
)

routesImport += "modules.LogType"
routesImport += "models.SearchQuery"
