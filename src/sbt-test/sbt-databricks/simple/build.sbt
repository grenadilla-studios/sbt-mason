import sbtdatabricks.ZipPlugin

lazy val root = (project in file("."))
.enablePlugins(ZipPlugin)
.settings(
scalaVersion := "2.12.16",
version := "0.1",
sourceZipDir := crossTarget.value
)
