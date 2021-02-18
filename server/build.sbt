import com.typesafe.sbt.packager.MappingsHelper._

mappings in Universal ++= directory(baseDirectory.value / "floor_plans")

name := "anyplace"

version := "4.0"

scalaVersion := "2.11.7"

val playVersion = "2.4.2" // see also plugins.sbt

libraryDependencies ++= Seq( jdbc, cache , ws, specs2 % Test )
//scalaVersion := "2.12.11"
//scalaVersion := "2.13.2" // Cant be updated due to ACCES
//libraryDependencies ++= Seq(jdbc, cacheApi, ws, specs2 % Test )
// Needed by StartModule to @Inject EagerSingletons for mounting
// callbacks on startup and termination of the app
// libraryDependencies += "com.google.inject" % "guice" % "3.0"

// TODO ACCES: all these should be updated to 1.0 version, once bayes-scala-gp is replaced.
libraryDependencies ++= Seq(
  // Add here the specific dependencies for this module:
  filters,
    // TODO ACCESS: with % (single percentage) and by appending _VERSION to lib-name,
    //  we force it to use an earlier version
    // The should instead become:
    // "package" %% "lib-name" % "1.0", e.g.:
    // (adding another % between package and lib-name concatenation, and removing _VERSOIN suffix from libname)
    // "org.scalanlp" %% "breeze" % "1.0",
  "org.scalanlp" % "breeze_2.11" % "0.12",
  "org.scalanlp" % "breeze-natives_2.11" % "0.12",
  "org.scalanlp" % "breeze-viz_2.11" % "0.12",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.9.0"
)

// Play json
libraryDependencies += "com.typesafe.play" %% "play-json" % playVersion

// TODO ACCES: DEPRECATE this library
libraryDependencies += "com.github.danielkorzekwa" % "bayes-scala-gp_2.11" % "0.1-SNAPSHOT"

// INFO: updated version to work with couchbase 6.5 version
// Due to wiping/reinstalling Couchbase to v6.5
libraryDependencies += "com.couchbase.client" % "java-client" % "2.7.18"
// This version works with couchbase 6.0
//libraryDependencies += "com.couchbase.client" % "java-client" % "2.4.5"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

resolvers += Resolver.sonatypeRepo("snapshots")

// unmanagedResourceDirectories in Test +=  baseDirectory ( _ /"target/web/public/test" )

javaOptions += "-Dfile.encoding=UTF-8"

//Required for InfluxDB
libraryDependencies += "io.razem" %% "scala-influxdb-client" % "0.6.2"

// libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

resolvers ++= Seq(
  // other resolvers here
  // if you want to use snapshot builds (currently 0.12-SNAPSHOT), use this.
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

lazy val `anyplace` = (project in file(".")).enablePlugins(PlayScala)
