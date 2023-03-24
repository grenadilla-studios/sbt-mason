package mason

import io.circe.generic.auto._
import io.circe.{Decoder, JsonObject}
import mason.api.models.libraries.ClusterStatus._
import sbt.Logger
import sttp.client3._
import sttp.client3.circe._
import sttp.model.Method

import java.nio.file.Path

import scala.util.{Either, Left, Right}

class DatabricksAPI(
    databricksEnvName: String,
    configParser: DatabricksConfigParser
)(implicit logger: Logger) {
  // fail fast if the provided config file doesn't have the required information
  private val apiToken =
    configParser.parsedConfig
      .get(databricksEnvName.toLowerCase())
      .flatMap(_.get("token"))
  private val databricksHost =
    configParser.parsedConfig
      .get(databricksEnvName.toLowerCase())
      .flatMap(_.get("host"))
  require(
    apiToken.isDefined,
    s"No token defined in config file for environment '$databricksEnvName'"
  )
  require(
    databricksHost.isDefined,
    s"No host defined in config file for environment '$databricksEnvName'"
  )
  private val backend = HttpClientSyncBackend()

  private val apiEndpoint = (path: String) =>
    databricksHost.map(hostname => s"$hostname/api/2.0/$path")

  /**
   * Method for building HTTP requests for the Databricks API.
   *
   * @tparam T
   *   typed representation of what JSON the endpoint will return
   * @param method
   *   http method for the request
   * @param path
   *   api path to build the request for
   * @param query
   *   query string segments to add to the request if needed
   */
  def buildRequest[T: Decoder](
      method: Method,
      path: String,
      query: Map[String, String]
  ): Option[
    RequestT[Identity, Either[ResponseException[String, io.circe.Error], T], Any with Any]
  ] = {
    logger.info("Building Databricks API request")
    for {
      token <- apiToken
      url   <- apiEndpoint(path)
    } yield basicRequest
      .headers(Map("Authentication" -> s"Bearer $token"))
      .method(method, uri"$url?$query")
      .response(asJson[T])
  }

  /**
   * Build an HTTP request for the status of a specific cluster.
   *
   * @param clusterId
   *   id of cluster for which information is being requested
   */
  def libraryStatusRequest(clusterId: String): RequestT[Identity, Either[
    ResponseException[String, io.circe.Error],
    ClusterLibrariesStatus
  ], Any with Any] =
    buildRequest[ClusterLibrariesStatus](
      Method.GET,
      "libraries/cluster-status",
      Map("cluster_id" -> clusterId)
    ) match {
      case Some(req) => req
      case None =>
        throw new Exception(
          "Failed to construct request for libraries/cluster-status endpoint."
        )
    }

  /**
   * Use the provided library name and cluster ID to search the names of the libraries installed on
   * the cluster for matches names and return a list of matches.
   *
   * @param libraryName
   *   name of library to perform matching against already installed libraries
   * @param clusterId
   *   id of cluster to search for matching libraries on
   */
  def librariesToRemove(
      libraryName: String,
      clusterId: String
  ): Option[Seq[String]] = {
    libraryStatusRequest(clusterId).send(backend).body match {
      case Left(err) => {
        logger.warn(err.toString())
        throw new Exception(
          "Failed to fetch data from libraries/cluster-status endpoint."
        )
      }
      case Right(clusterLibStatus) =>
        clusterLibStatus.library_statuses
          .map(
            _.map(_.library)
              .collect { case jar: JarLibrary => jar }
              .map(_.jar)
              .filter(_.contains(libraryName))
          )
          .filter(_.nonEmpty)
    }
  }

  /**
   * Generates a JSON string which defines a library uninstall operation for the provided cluster.
   *
   * @param librariesToRemove
   *   names of the libraries on the cluster which need to be removed
   * @param clusterId
   *   id of cluster to uninstall libraries from
   */
  def uninstallJson(
      librariesToRemove: Option[Seq[String]],
      clusterId: String
  ): Option[String] =
    for {
      removals <- librariesToRemove
    } yield s"""|{
        |  "cluster_id": "${clusterId}",
        |  "libraries": [
        |    ${removals.map(path => s"""{"jar": "$path"}""").mkString(",")}
        |  ]
        |}""".stripMargin

  /**
   * Uninstall libraries from a cluster based on the output of uninstallJson() call
   *
   * @param uninstallJson
   *   json blob which defines the libraries to uninstall and the cluster to affect
   */
  def removeOldVersions(uninstallJson: Option[String]): Either[String, String] =
    uninstallJson match {
      case None =>
        Left(
          "The JSON blob for libraries to remove returned by the uninstallJson method was None"
        )
      case Some(json) =>
        buildRequest[JsonObject](
          Method.POST,
          "libraries/uninstall",
          Map.empty[String, String]
        ) match {
          case None => Left("Unable to build request to uninstall libraries.")
          case Some(request) =>
            request.body(json).send(backend).body match {
              case Right(_) =>
                Right("The request to uninstall libraries suceeded.")
              case Left(err) =>
                Left(
                  s"The request to uninstall libraries failed: ${err.toString}"
                )
            }
        }
    }

  /**
   * Upload an artifact from local storage to Databricks FileStore
   *
   * @param localPath
   *   path to artifact on local filesystem
   * @param targetDir
   *   path in Databricks FileStore to upload the artifact to
   */
  def uploadJar(localPath: Path, targetDir: String): Option[String] = {
    val jarName = localPath.getFileName.toString
    // val targetPath = s"/FileStore/jars/${jarName}"
    val targetPath = Path.of(targetDir).resolve(jarName).toString
    buildRequest[JsonObject](Method.POST, "dbfs/put", Map.empty) match {
      case None =>
        logger.error("Unable to build request to upload new JAR")
        None
      case Some(req) =>
        req
          .multipartBody(
            multipartFile("contents", localPath),
            multipart("path", targetPath),
            multipart("overwrite", "true")
          )
          .send(backend)
          .is200 match {
          case true =>
            logger.success("Uploaded new JAR to Databricks filestore.")
            Some(targetPath)
          case false =>
            logger.error("Failed to upload new JAR to databricks filestore.")
            None
        }
    }
  }

  /**
   * Install a library from Databricks FileStore onto a cluster.
   *
   * @param clusterId
   *   id of cluster on which to install library
   * @param libraryPath
   *   path in Databricks FS where the library to install is located
   */
  def installJar(clusterId: String, libraryPath: String): Boolean = {
    val installJson = s"""|{
      |  "cluster_id": "$clusterId",
      |  "libraries": [
      |    {
      |      "jar": "dbfs:$libraryPath"
      |    }
      |  ]
      |}""".stripMargin
    buildRequest[JsonObject](
      Method.POST,
      "libraries/install",
      Map.empty
    ) match {
      case None => {
        logger.error("Unable to build request to install library on cluster.")
        false
      }
      case Some(req) =>
        req.body(installJson).send(backend).is200 match {
          case true => {
            logger.success(s"Library $libraryPath installed on cluster $clusterId")
            true
          }
          case false => {
            logger.warn(s"Library failed to install on cluster")
            false
          }
        }
    }
  }

  /**
   * Send the restart command to a cluster.
   *
   * @param clusterId
   *   identifier of the cluster to be restarted
   */
  def restartCluster(clusterId: String): Boolean = {
    buildRequest[JsonObject](
      Method.POST,
      "clusters/restart",
      // Map("cluster_id" -> clusterId)
      Map.empty
    ) match {
      case None => {
        logger.error(
          "Unable to build request to restart cluster after install."
        )
        false
      }
      case Some(req) => {
        case class ClusterID(cluster_id: String)
        req.body(ClusterID(clusterId)).send(backend).is200 match {
          case true => {
            logger.success(s"Cluster $clusterId restarted successfully")
            true
          }
          case false => {
            logger.warn(
              s"Cluster $clusterId was not restarted - perhaps it is not running?"
            )
            false
          }
        }
      }
    }
  }

  /**
   * Ensure the given directory exists in the databricks workspace. This request tries to create the
   * directory every time, and Databricks does nothing if the directory already exists.
   *
   * @param directoryPath
   *   the full path of the directory to be created
   */
  def ensureDirectory(directoryPath: String): Boolean = {
    buildRequest[JsonObject](Method.POST, "workspace/mkdirs", Map.empty) match {
      case None =>
        logger.error("Unable to build request to make directories.")
        false
      case Some(req) => {
        case class DirPath(path: String)
        req.body(DirPath(directoryPath)).send(backend).is200 match {
          case true => {
            logger.success(s"Directory $directoryPath exists.")
            true
          }
          case false => {
            logger.warn(s"Failed to create directory $directoryPath")
            false
          }
        }
      }
    }
  }

  /**
   * Upload an arbitrary scala/python/sql/r notebook file to Databricks workspace.
   *
   * @param sourceFilePath
   *   the path to the notebook file on the local filesystem
   * @param destinationFilePath
   *   path in Databricks workspace to which the notebook should be uploaded
   * @param overwite
   *   whether or not to overwrite the notebook content in Databricks if the file already exists
   */
  def uploadFile(sourceFilePath: Path, destinationFilePath: String, overwrite: Boolean): Boolean = {
    def detectLang(filename: String): String = {
      val extension = filename.split("\\.").last
      extension.toLowerCase match {
        case "sc" | "scala" => "SCALA"
        case "py"           => "PYTHON"
        case "sql"          => "SQL"
        case "r"            => "R"
      }
    }
    buildRequest[JsonObject](Method.POST, "workspace/import", Map.empty) match {
      case None =>
        logger.error("Unable to build request to upload file artifact.")
        false
      case Some(req) => {
        val dirExists = ensureDirectory(
          destinationFilePath.split("/").reverse.tail.reverse.mkString("/")
        )
        if (dirExists) {
          val resp = req
            .multipartBody(
              multipartFile("content", sourceFilePath),
              multipart("path", destinationFilePath),
              multipart("overwrite", if (overwrite) "true" else "false"),
              multipart("format", "SOURCE"),
              multipart("language", detectLang(sourceFilePath.getFileName.toString))
            )
            .send(backend)
          resp.is200 match {
            case true =>
              logger.success(
                s"Uploaded file artifact ${sourceFilePath.toString} to ${destinationFilePath}"
              )
              true
            case false =>
              logger.error(s"Failed to upload file artifact ${sourceFilePath.toString}")
              logger.error(resp.body.toString)
              false
          }
        } else {
          false
        }
      }
    }
  }
}
