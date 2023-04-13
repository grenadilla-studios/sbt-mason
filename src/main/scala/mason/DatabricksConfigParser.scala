package mason

import sbt.Logger

import scala.io.Source

class DatabricksConfigParser(fileLocation: Option[String])(implicit logger: Logger) {
  // use standard dotfile if no specific config given
  private lazy val homePath              = System.getProperty("user.home")
  private lazy val defaultConfigFilePath = s"$homePath/.databrickscfg"

  // read the config file
  logger.debug(s"parsing config file located at ${fileLocation.getOrElse(defaultConfigFilePath)}")
  private lazy val configFileContent =
    Source.fromFile(fileLocation.getOrElse(defaultConfigFilePath)).getLines.toSeq

  // parse each bracketed header into a key for a k->v map of each of the settings under it;
  // the .databrickscfg format looks to adhere to TOML but there don't seem to be any regularly
  // maintained TOML parsers for Scala or Java so I'm implementing this very simple parser instead
  private def parseConfigFile(content: Seq[String]): Map[String, Map[String, String]] = {
    def parseRecurse(
        result: Map[String, Map[String, String]],
        content: Seq[String]
    ): Map[String, Map[String, String]] = {
      content match {
        case head +: tail => {
          head.startsWith("[") match {
            case true => {
              val key      = head.replaceAll("\\[|\\]", "").toLowerCase.trim()
              val settings = tail.takeWhile(!_.startsWith("["))
              val mapSettings: Map[String, String] =
                settings.map(_.split("=")).map(a => (a(0).trim(), a(1).trim())).toMap
              parseRecurse(result ++ Map(key -> mapSettings), tail.drop(settings.length))
            }
            case false => parseRecurse(result, tail)
          }
        }
        case Nil => result
      }
    }
    parseRecurse(Map.empty[String, Map[String, String]], content)
  }

  // the result of the parsing
  lazy val parsedConfig = parseConfigFile(configFileContent)
}
