import com.typesafe.config.ConfigFactory

val conf = ConfigFactory.parseFile(new File("conf/app.base.conf")).resolve()
val appVersion = conf.getString("application.version")
val appName= conf.getString("application.name")

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SwaggerPlugin)
  .settings(
    name := appName,
    version := appVersion,
    maintainer := "anyplace@cs.ucy.ac.cy",
    scalaVersion := "2.13.6",
    libraryDependencies ++= Seq(
      guice,
      "com.typesafe.play" %% "play-json" % "2.10.0-RC5",
      "com.typesafe.play" %% "play" % "2.8.8",
      "org.mongodb.scala" %% "mongo-scala-driver" % "4.3.0",
    ),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-Xfatal-warnings")
  )

resolvers ++= Seq(
  Resolver.jcenterRepo,
)

swaggerRoutesFile := "api.routes"
// auto generate swagger definitions for domain classes
swaggerDomainNameSpaces := Seq("models")
swaggerPrettyJson := true