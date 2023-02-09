package mason

import sbt._

import java.io.File

trait MasonKeys {
  lazy val masonPublishLibrary =
    taskKey[Unit]("Publish the project JAR to configured Databricks cluster.")

  lazy val masonRemoveLibrary = taskKey[Boolean](
    "Remove existing versions of the project from the configured Databricks cluster."
  )
  lazy val masonClusterId =
    settingKey[String]("Cluster ID to use for publishing.")

  lazy val masonConfigFile = settingKey[Option[String]](
    "Use this to override the default of '/USER_HOME/.databrickscfg'"
  )

  lazy val masonEnvironmentName = settingKey[String](
    "The databricks environment from the config file that you would like to interact with. 'DEFAULT' is the default."
  )

  lazy val masonLibraryName = settingKey[String](
    "The name of the library being managed by this plugin; will be used for matching against already installed versions of libraries on clusters."
  )

  // values from zip sample plugin
  lazy val sourceZipDir =
    settingKey[File]("source directory to generate zip from")
  lazy val targetZipDir =
    settingKey[File]("target directory to store generated zip")
  lazy val zip = taskKey[Unit](
    "generates zip file which includes all files from sourceZipDir"
  )
}
