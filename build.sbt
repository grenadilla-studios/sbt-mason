val circeVersion = "0.14.3"
val sttpVersion = "3.8.8"
lazy val root = (project withId "sbt-mason" in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "sbt-mason",
    organization := "com.grenadillastudios",
    version := "0.1.0-SNAPSHOT",
    sbtPlugin := true,
    scriptedLaunchOpts += ("-Dplugin.version=" + version.value),
    /* scriptedLaunchOpts ++= sys.process.javaVmArguments.filter(a => */
    /*   Seq("-Xmx", "-Xms", "-XX", "-Dsbt.log.noformat").exists(a.startsWith) */
    /* ), */
    scriptedBufferLog := false,
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-generic-extras" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.softwaremill.sttp.client3" %% "core" % sttpVersion,
      "com.softwaremill.sttp.client3" %% "circe" % sttpVersion
    )
  )
