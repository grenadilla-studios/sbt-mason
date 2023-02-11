package mason

import sbt.Keys._
import sbt._

import java.nio.file.Files

import scala.jdk.CollectionConverters._

object MasonPlugin extends AutoPlugin {
  override val trigger: PluginTrigger = allRequirements
  override val requires: Plugins      = plugins.JvmPlugin
  object autoImport extends MasonKeys
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    masonClusterId       := "",
    masonConfigFile      := None,
    masonEnvironmentName := "DEFAULT",
    masonPublishLibrary  := publishTask.value,
    masonRemoveLibrary   := removeLib.value,
    masonLibraryName     := name.value
  )

  private def removeLib = Def.task {
    implicit val log = sLog.value
    val configParser = new DatabricksConfigParser(masonConfigFile.value)
    val api          = new DatabricksAPI(masonEnvironmentName.value, configParser)

    val libsToRemove = api.librariesToRemove(
      masonLibraryName.value,
      masonClusterId.value
    )
    libsToRemove match {
      case None => {
        log.warn(
          s"No libraries named `${masonLibraryName.value}` found on cluster ${masonClusterId.value}."
        )
        true
      }
      case _ => {
        log.info(
          s"Removing the following libraries from cluster ${masonClusterId.value}:"
        )
        log.info(libsToRemove.getOrElse(Seq.empty).mkString("\n"))
        val uninstallJson =
          api.uninstallJson(libsToRemove, masonClusterId.value)
        val removeResult = api.removeOldVersions(uninstallJson)
        removeResult match {
          case Right(message) => {
            log.success(message)
            true
          }
          case Left(message) => {
            log.error(message)
            false
          }
        }

      }
    }
  }
  private def publishTask = Def.task {
    implicit val log = sLog.value
    val configParser = new DatabricksConfigParser(masonConfigFile.value)
    val api          = new DatabricksAPI(masonEnvironmentName.value, configParser)
    log.info("running publishLocal")
    val _ = publishLocal.value
    log.info(
      s"searching for published jar with name `${masonLibraryName.value}` and version `${version.value}`"
    )
    val scalaVersionNoPatch =
      scalaVersion.value.split("\\.").take(2).mkString(".")
    val startPath      = target.value / s"scala-$scalaVersionNoPatch"
    val projectVersion = version.value
    val matchedJars = Files
      .walk(startPath.toPath)
      .iterator
      .asScala
      .filter(f => {
        val fname = f.getFileName.toString;
        fname.contains(masonLibraryName.value) && fname.contains(
          s"$projectVersion.jar"
        )
      })
      .toSeq
    matchedJars.length match {
      case 0 =>
        log.error(
          s"No published JARs matched the configured library name `${masonLibraryName.value}` - perhaps the name is too specific?"
        )
        false
      case 1 => {
        log.info(
          s"Uploading ${matchedJars.head.toString} to cluster ${masonClusterId.value}."
        )
        val uploaded = api.uploadJar(matchedJars.head)
        uploaded match {
          case Some(uploadedPath) => {
            val installed = api.installJar(masonClusterId.value, uploadedPath)
            installed match {
              case true => {
                val _ = api.restartCluster(masonClusterId.value)
                true
              }
              case false => false
            }

          }
          case None => false
        }
      }
      case _ =>
        log.error(
          s"More than one published JAR matched the configured library name `${masonLibraryName.value}`, please set a more specific name.\n${matchedJars
              .mkString("\n")}"
        )
        false
    }
  }
}
