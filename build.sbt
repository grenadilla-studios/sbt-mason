val circeVersion = "0.14.3"
val sttpVersion  = "3.8.8"

lazy val writeHooks = taskKey[Unit]("Write git hooks")
Global / writeHooks := GitHooks(file("git-hooks"), file(".git/hooks"), streams.value.log)

lazy val formatAll      = taskKey[Unit]("Run all code formatting.")
lazy val formatCheckAll = taskKey[Unit]("Check all code formatting.")

lazy val root = (project withId "sbt-mason" in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name         := "sbt-mason",
    organization := "com.grenadillastudios",
    sbtPlugin    := true,
    scalacOptions += "-Ywarn-unused-import",
    semanticdbEnabled := true,
    Global / onLoad ~= (_ andThen ("writeHooks" :: _)),
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
    versionScheme := Some("semver-spec")
  )

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository     := "https://s01.oss.sonatype.org/service/local"
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

formatAll := {
  val sbt = (Compile / scalafmtSbt).value
  val fmt = scalafmtAll.value
  scalafixAll.toTask("").value
}

formatCheckAll := {
  val sbt = (Compile / scalafmtSbtCheck).value
  val fmt = scalafmtCheckAll.value
  scalafixAll.toTask(" --check").value
}
