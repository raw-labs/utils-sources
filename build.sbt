import sbt.Keys._
import sbt._

import java.nio.file.Paths

ThisBuild / credentials += Credentials(
  "GitHub Package Registry",
  "maven.pkg.github.com",
  "raw-labs",
  sys.env.getOrElse("GITHUB_TOKEN", "")
)

val isRelease = sys.props.getOrElse("release", "false").toBoolean

lazy val commonSettings = Seq(
  homepage := Some(url("https://www.raw-labs.com/")),
  organization := "com.raw-labs",
  organizationName := "RAW Labs SA",
  startYear := Some(2023),
  organizationHomepage := Some(url("https://www.raw-labs.com/")),
  developers := List(Developer("raw-labs", "RAW Labs", "engineering@raw-labs.com", url("https://github.com/raw-labs"))),
  licenses := List(
    "Business Source License 1.1" -> new URI(
      "https://raw.githubusercontent.com/raw-labs/snapi/main/licenses/BSL.txt"
    ).toURL
  ),
  headerSources / excludeFilter := HiddenFileFilter,
  // Use cached resolution of dependencies
  // http://www.scala-sbt.org/0.13/docs/Cached-Resolution.html
  updateOptions := updateOptions.in(Global).value.withCachedResolution(true),
  resolvers += "RAW Labs GitHub Packages" at "https://maven.pkg.github.com/raw-labs/_",
  resolvers ++= Seq(Resolver.mavenLocal),
  resolvers ++= Resolver.sonatypeOssRepos("snapshots"),
  resolvers ++= Resolver.sonatypeOssRepos("releases")
)

lazy val buildSettings = Seq(
  scalaVersion := "2.12.18",
  isSnapshot := !isRelease,
  javacOptions ++= Seq(
    "-source",
    "21",
    "-target",
    "21"
  ),
  scalacOptions ++= Seq(
    "-feature",
    "-unchecked",
    // When compiling in encrypted drives in Linux, the max size of a name is reduced to around 140
    // https://unix.stackexchange.com/a/32834
    "-Xmax-classfile-name",
    "140",
    "-deprecation",
    "-Xlint:-stars-align,_",
    "-Ywarn-dead-code",
    "-Ywarn-macros:after", // Fix for false warning of unused implicit arguments in traits/interfaces.
    "-Ypatmat-exhaust-depth",
    "160"
  )
)

lazy val compileSettings = Seq(
  Compile / doc / sources := Seq.empty,
  Compile / packageDoc / mappings := Seq(),
  Compile / packageSrc / publishArtifact := true,
  Compile / packageDoc / publishArtifact := false,
  Compile / packageBin / packageOptions += Package.ManifestAttributes(
    "Automatic-Module-Name" -> name.value.replace('-', '.')
  ),
  // Add all the classpath to the module path.
  Compile / javacOptions ++= Seq(
    "--module-path",
    (Compile / dependencyClasspath).value.files.absString
  ),
  // The module-info.java requires the Scala classes to be compiled.
  compileOrder := CompileOrder.ScalaThenJava
)

lazy val testSettings = Seq(
  // Exclude module-info.java, otherwise it will fail the compilation.
  Test / doc / sources := {
    (Compile / doc / sources).value.filterNot(_.getName.endsWith("module-info.java"))
  },
  // Ensuring tests are run in a forked JVM for isolation.
  Test / fork := true,
  // Pass system properties starting with "raw." to the forked JVMs.
  Test / javaOptions ++= {
    import scala.collection.JavaConverters._
    val props = System.getProperties
    props
      .stringPropertyNames()
      .asScala
      .filter(_.startsWith("raw."))
      .map(key => s"-D$key=${props.getProperty(key)}")
      .toSeq
  },
  // Set up heap dump options for out-of-memory errors.
  Test / javaOptions ++= Seq(
    "-XX:+HeapDumpOnOutOfMemoryError",
    s"-XX:HeapDumpPath=${Paths.get(sys.env.getOrElse("SBT_FORK_OUTPUT_DIR", "target/test-results")).resolve("heap-dumps")}"
  ),
  Test / publishArtifact := true,
  Test / packageSrc / publishArtifact := true
)

val isCI = sys.env.getOrElse("CI", "false").toBoolean

lazy val publishSettings = Seq(
  versionScheme := Some("early-semver"),
  publish / skip := false,
  publishMavenStyle := true,
  // Temporarily publishing to the Snapi repo until the migration is finished...
  publishTo := Some("GitHub raw-labs Apache Maven Packages" at "https://maven.pkg.github.com/raw-labs/snapi"),
  publishConfiguration := publishConfiguration.value.withOverwrite(isCI)
)

lazy val nonStrictBuildSettings = commonSettings ++ compileSettings ++ buildSettings ++ testSettings

lazy val root = (project in file("."))
  .doPatchDependencies() // Patch Scala dependencies to ensure their names are JPMS-friendly.
  .settings(
    name := "utils-sources",
    nonStrictBuildSettings,
    publishSettings,
    libraryDependencies ++= Seq(
      "com.raw-labs" %% "utils-core" % "0.50.0" % "compile->compile;test->test",
      "org.apache.httpcomponents.client5" % "httpclient5" % "5.2.1",
      "io.jsonwebtoken" % "jjwt-api" % "0.11.5",
      "io.jsonwebtoken" % "jjwt-impl" % "0.11.5",
      "com.github.jwt-scala" %% "jwt-core" % "9.4.4",
      "org.springframework" % "spring-core" % "5.3.13" exclude ("org.springframework", "spring-jcl"), // We use jcl-over-slf4j
      "com.dropbox.core" % "dropbox-core-sdk" % "5.4.5",
      "software.amazon.awssdk" % "s3" % "2.20.69" exclude ("commons-logging", "commons-logging"), // We use slf4j
      "org.postgresql" % "postgresql" % "42.5.4",
      "com.mysql" % "mysql-connector-j" % "8.1.0" exclude ("com.google.protobuf", "protobuf-java"),
      "com.microsoft.sqlserver" % "mssql-jdbc" % "7.0.0.jre10",
      "net.snowflake" % "snowflake-jdbc" % "3.13.33",
      "com.oracle.database.jdbc" % "ojdbc10" % "19.24.0.0",
      "com.teradata.jdbc" % "terajdbc" % "20.00.00.24"
    )
  )
