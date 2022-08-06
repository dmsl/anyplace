import com.typesafe.config.ConfigFactory

val conf = ConfigFactory.parseFile(new File("conf/app.base.conf")).resolve()

val _scalaVersion = conf.getString("scala.version")
val _appVersion = conf.getString("application.version")
val _appName= conf.getString("application.name")
val _maintainer= conf.getString("application.maintainer")

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SwaggerPlugin)
  .settings(
    name := _appName,
    version := _appVersion,
    maintainer := _maintainer,
    scalaVersion := _scalaVersion,
    libraryDependencies ++= Seq(
      guice,
      "com.typesafe.play" %% "play-json" % "2.10.0-RC5",
      "com.typesafe.play" %% "play" % "2.8.13",
      "org.mongodb.scala" %% "mongo-scala-driver" % "4.4.0",
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