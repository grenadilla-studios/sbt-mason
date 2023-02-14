package mason

import sbt._

trait MasonKeys {
  lazy val masonPublishLibrary =
    taskKey[Boolean]("Publish the project JAR to configured Databricks cluster.")

  lazy val masonRemoveLibrary = taskKey[Boolean](
    "Remove existing versions of the project from the configured Databricks cluster."
  )

  lazy val masonUploadFile = taskKey[Boolean](
    "Upload a file from a source location into Databricks."
  )

  lazy val masonOverwriteFileUploads = settingKey[Boolean](
    """|Whether to overwrite uploaded files when uploading to Databricks.
       |If set to false and the file already exists in Databricks, the upload task will fail.
       |""".stripMargin
  )

  lazy val masonArtifactDestinationDir = settingKey[String](
    "The location in Databricks FS to store uploaded artifacts - default is `/FileStore/jars`."
  )

  lazy val masonSourceFileLoc = settingKey[String](
    "The default location to look for files when uploading to Databricks."
  )

  lazy val masonDestinationFileLoc = settingKey[String](
    "The default location in Databricks to upload files."
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
}
