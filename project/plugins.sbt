libraryDependencies ++= Seq(
  "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
  "commons-io"     % "commons-io"      % "2.11.0"
)
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.3") // https://scalacenter.github.io/scalafix
// https://github.com/scalameta/sbt-scalafmt
addSbtPlugin(
  "org.scalameta" % "sbt-scalafmt" % "2.4.6"
)
// https://github.com/sbt/sbt-ci-release
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.11")
