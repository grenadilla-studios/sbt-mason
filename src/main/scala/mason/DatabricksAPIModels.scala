package mason.api.models.libraries

import cats.syntax.functor._
import io.circe._
import io.circe.generic.auto._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveEnumerationCodec
import io.circe.syntax._
import sttp.client3._
import sttp.client3.circe._
import sttp.model.Method

import scala.util.Either

object ClusterStatus {

  sealed trait LibraryType
  object LibraryType {
    implicit val encodeLibraryType: Encoder[LibraryType] = Encoder.instance {
      case jar @ JarLibrary(_)     => jar.asJson
      case pypi @ PyPiLibrary(_)   => pypi.asJson
      case cran @ CranLibrary(_)   => cran.asJson
      case maven @ MavenLibrary(_) => maven.asJson
    }
    implicit val decodeLibraryType: Decoder[LibraryType] =
      List[Decoder[LibraryType]](
        Decoder[JarLibrary].widen,
        Decoder[PyPiLibrary].widen,
        Decoder[CranLibrary].widen,
        Decoder[MavenLibrary].widen
      ).reduceLeft(_ or _)
  }
  case class JarLibrary(jar: String) extends LibraryType
  case class PyPiPackage(`package`: String)
  case class PyPiLibrary(pypi: PyPiPackage) extends LibraryType
  case class CranPackage(`package`: String, repo: Option[String])
  case class CranLibrary(cran: CranPackage) extends LibraryType
  case class MavenCoordinates(coordinates: String)
  case class MavenLibrary(maven: MavenCoordinates) extends LibraryType
  case class LibraryStatus(
      library: LibraryType,
      status: String,
      messages: Option[Seq[String]],
      is_library_for_all_clusters: Boolean
  )
  case class ClusterLibrariesStatus(
      cluster_id: String,
      library_statuses: Option[Seq[LibraryStatus]]
  )
}
