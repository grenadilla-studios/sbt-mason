libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.3") // https://scalacenter.github.io/scalafix
addSbtPlugin(
  "org.scalameta" % "sbt-scalafmt" % "2.4.6"
) // https://github.com/scalameta/sbt-scalafmt
