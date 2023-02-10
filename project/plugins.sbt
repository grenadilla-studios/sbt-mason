libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.3") // https://scalacenter.github.io/scalafix
 // https://github.com/scalameta/sbt-scalafmt
 addSbtPlugin(
  "org.scalameta" % "sbt-scalafmt" % "2.4.6"
)
// https://github.com/xerial/sbt-sonatype
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.17")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")
// https://github.com/sbt/sbt-release
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
