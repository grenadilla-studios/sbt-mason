package mason

import java.io.File
import sbt.Keys._
import sbt._
import sbt.io.{IO, Path}

object MasonPlugin extends AutoPlugin {
  override val trigger: PluginTrigger = allRequirements
  override val requires: Plugins = plugins.JvmPlugin
  object autoImport extends MasonKeys
  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    targetZipDir := target.value / "zip",
    // zip := zipTask.value,
    masonClusterId := "",
    masonConfigFile := None,
    masonEnvironmentName := "DEFAULT",
    masonPublishLibrary := publishTask.value,
    masonRemoveLibrary := removeLib.value,
    masonLibraryName := name.value
  )

  private def removeLib = Def.task {
    implicit val log = sLog.value
    log.warn(target.value.toString)
    val configParser = new DatabricksConfigParser(masonConfigFile.value)
    val api = new DatabricksAPI(masonEnvironmentName.value, configParser)

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
        log.info(s"Removing the following libraries from cluster ${masonClusterId.value}:")
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
  }
  // private def zipTask = Def.task {
  //   val log = sLog.value

  //   lazy val zip =
  //     new File(targetZipDir.value, sourceZipDir.value.getName + ".zip")
  //   log.info("Zipping file...")
  //   IO.zip(Path.allSubpaths(sourceZipDir.value), zip)
  //   zip
  // }
}
