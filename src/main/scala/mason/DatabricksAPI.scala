package mason

import mason.api.models.libraries.ClusterStatus._

import sbt.Logger

import scala.util.{Either, Left, Right}
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.JsonObject
import sttp.model.Method
import scala.util.{Left, Right, Either}
import sttp.client3._
import sttp.client3.circe._
import sttp.model.Method

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

  def buildRequest[T: Decoder](
      method: Method,
      path: String,
      query: Map[String, String]
  ) = {
    logger.info("Building Databricks API request")
    for {
      token <- apiToken
      url <- apiEndpoint(path)
    } yield basicRequest
      .headers(Map("Authentication" -> s"Bearer $token"))
      .method(method, uri"$url?$query")
      .response(asJson[T])
  }

  def libraryStatusRequest(clusterId: String) =
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
        clusterLibStatus.library_statuses.map(
          _.map(_.library)
            .collect { case jar: JarLibrary => jar }
            .map(_.jar)
            .filter(_.contains(libraryName))
        ).filter(_.nonEmpty)
    }
  }
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
              case Left(err) => Left(s"The request to uninstall libraries failed: ${err.toString}")
            }
        }
    }

}
