val circeVersion = "0.14.3"
val sttpVersion  = "3.8.8"

lazy val root = (project withId "sbt-mason" in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name              := "sbt-mason",
    organization      := "com.grenadillastudios",
    // version           := "0.1.0-SNAPSHOT",
    sbtPlugin         := true,
    // publishMavenStyle := true,
    // publishTo         := sonatypePublishToBundle.value,
    scalacOptions += "-Ywarn-unused-import",
    semanticdbEnabled                                       := true,
    Global / onChangedBuildSource                           := ReloadOnSourceChanges,
    Global / semanticdbVersion                              := scalafixSemanticdb.revision,
    Global / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0",
    Global / scalafixScalaBinaryVersion                     := "2.12",
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    scriptedBufferLog := false,
    libraryDependencies ++= Seq(
      "io.circe"                      %% "circe-core"           % circeVersion,
      "io.circe"                      %% "circe-generic"        % circeVersion,
      "io.circe"                      %% "circe-generic-extras" % circeVersion,
      "io.circe"                      %% "circe-parser"         % circeVersion,
      "com.softwaremill.sttp.client3" %% "core"                 % sttpVersion,
      "com.softwaremill.sttp.client3" %% "circe"                % sttpVersion
    ),
    // credentials += {
    //   Credentials(Path.userHome / ".sbt" / "sonatype_credentials")
    // },
    versionScheme := Some("semver-spec")
  )

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/grenadilla-studios/sbt-mason"),
    "scm:git@github.com:grenadilla-studios/sbt-mason.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "btrachey",
    name = "Brian Tracey",
    email = "brian@grenadilla-studios.com",
    url = url("https://github.com/btrachey")
  )
)

ThisBuild / description := "sbt plugin for managing artifacts in Databricks"
ThisBuild / homepage    := Some(url("https://github.com/grenadilla-studios/sbt-mason"))
ThisBuild / licenses := Seq("MIT" -> url("https://github.com/sbt/sbt-assembly/blob/master/LICENSE"))
