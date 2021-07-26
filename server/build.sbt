import com.typesafe.config.ConfigFactory

val conf = ConfigFactory.parseFile(new File("conf/anyplace.conf")).resolve()
val appVersion = conf.getString("application.version")
val appName= conf.getString("application.name")

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SwaggerPlugin)
  .settings(
    name := appName,
    version := appVersion,
    scalaVersion := "2.13.6",
    libraryDependencies ++= Seq(
      guice,
      "com.typesafe.play" %% "play-json" % "2.10.0-RC2",
      "com.typesafe.play" %% "play" % "2.8.7",
      "org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0",
      "io.razem" %% "scala-influxdb-client" % "0.6.2",  // TODO:NN REMOVE
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings")
  )

resolvers ++= Seq(
  Resolver.jcenterRepo,
)

// auto generate swagger definitions for domain classes
swaggerRoutesFile := "api.routes"
swaggerDomainNameSpaces := Seq("models")
swaggerPrettyJson := true