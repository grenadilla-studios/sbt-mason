import org.apache.commons.io.FileUtils
import sbt._
import sbt.internal.util.ManagedLogger

import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.nio.file.StandardCopyOption

import scala.collection.JavaConverters._
import scala.util.Properties

// taken from https://github.com/stryker-mutator/stryker4s/blob/e63e186b3c4c606d88a768fbefa431cd7269b45d/project/GitHooks.scala
object GitHooks {
  def apply(hooksSourceDir: File, hooksTargetDir: File, log: ManagedLogger): Unit =
    if (hooksSourceDir.isDirectory && hooksTargetDir.exists()) {
      IO.listFiles(hooksSourceDir)
        .map(hook => (hook, hooksTargetDir / hook.name))
        // Don't write if hook already exists and file content is the same
        .filterNot { case (originalHook, targetHook) =>
          FileUtils.contentEquals(originalHook, targetHook)
        }
        .foreach { case (originalHook, targetHook) =>
          log.info(s"Copying ${originalHook.name} hook to $targetHook")
          Files.copy(originalHook.asPath, targetHook.asPath, StandardCopyOption.REPLACE_EXISTING)
          if (!Properties.isWin)
            targetHook.setPermissions(PosixFilePermissions.fromString("rwxr-xr-x").asScala.toSet)
        }
    }
}
