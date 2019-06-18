package repositories.kylo
import java.io.{File, PrintWriter}
import java.nio.charset.CodingErrorAction

import akka.stream.scaladsl.FileIO
import ftd_api.yaml.Error
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import play.api.libs.ws.{WSAuthScheme, WSClient, WSResponse}
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import utils.{ConfigReader, InferSchema}

import scala.concurrent.Future
import scala.io.{Codec, Source}
import scala.concurrent.ExecutionContext.Implicits._

class KyloRepositoryProd extends KyloRepository {

  val logger = Logger.logger

  val INFER_URL = ConfigReader.kyloInferUrl
  val SERDE_CSV = ConfigReader.kyloCsvSerde
  val SERDE_JSON = ConfigReader.kyloJsonSerde


  //true  -> valid
  //false -> non valid
  private def validationColumnName(colName: String): Boolean = {
    //[!$%^&*()+~=`{}[\]<>?\"\'./\\\]
    val regex = """[!$%^&*()+~=`{}[\\]<>?\"'/àèéìòù -]""".r
    regex.findAllIn(colName).isEmpty
  }

  private def validateJsonKeys(jsValue: JsValue): List[(String, Boolean)] = {
    jsValue match {
      case JsArray(jsArray) =>
        jsArray.map{ jsArrayElem => validateJsonKeys(jsArrayElem) }.toList.flatten
      case JsObject(obj) =>
        obj.map{ objElem =>
          val head: (String, Boolean) = (objElem._1.trim, validationColumnName(objElem._1.trim))
          val tail: List[(String, Boolean)] = validateJsonKeys(objElem._2)
          if(tail.nonEmpty) head :: tail
          else List(head)
        }.flatten.toList
      case _ => List()
    }
  }

  override def inferSchema(fileType: String, sampleFile: File, ws: WSClient): Future[Either[Error, String]] = {
    // TODO refactor and parametrize when dealing with other format
    val serde: String = fileType match {
      case "csv"  => SERDE_CSV
      case "json" => SERDE_JSON
    }

    fileType.toLowerCase match {
      case "csv" =>
        logger.debug("csv")
        implicit val codec: Codec = Codec("UTF-8")
        codec.onMalformedInput(CodingErrorAction.REPLACE)
        codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

        val lines: Iterator[String] = Source.fromFile(sampleFile).getLines()
        val header: String = lines.next()
        val separator: String = InferSchema.inferSeparator(header, InferSchema.SEPARATORS)
        val columnsName: List[String] = header.split(separator).toList
        val validationResults: List[(String, Boolean)] = columnsName.map{ col =>
          (col.trim, validationColumnName(col.trim))
        }
        if(validationResults.exists(!_._2)) {
          logger.debug(s"fields not valid: ${validationResults.filter(!_._2).map(_._1).mkString(",")}")
          Future.successful(Left(Error(Some(400), None, Some(validationResults.filter(!_._2).map(_._1).mkString(",")))))
        }
        else {
          val tempFile: File = TemporaryFile(prefix = sampleFile.getName).file
          val writer = new PrintWriter(tempFile)
          writer.println(validationResults.map(_._1).mkString(separator))
          lines.foreach(line => writer.println(line))
          writer.close()

          val response: Future[WSResponse] = ws.url(INFER_URL)
            .withAuth(ConfigReader.kyloUser, ConfigReader.kyloPwd, WSAuthScheme.BASIC)
            .post(akka.stream.scaladsl.Source(FilePart("file", sampleFile.getName, Option("text/csv"),
              FileIO.fromPath(sampleFile.toPath)) :: DataPart("parser", serde) :: List()))

          response.flatMap{ r =>
            logger.debug(s"response from kylo infer schema: ${r.json.toString}")
            Future.successful(Right(Json.stringify(r.json)))
          }
        }
      case "json" =>
        logger.debug("json")

        val lines: String = scala.io.Source.fromFile(sampleFile).mkString
        val json: JsValue = Json.parse(lines)

        val validationResults: List[(String, Boolean)] = validateJsonKeys(json)

        if(validationResults.exists(!_._2)) {
          logger.debug(s"fields not valid: ${validationResults.filter(!_._2).map(_._1).mkString(",")}")
          Future.successful(Left(Error(Some(400), None, Some(validationResults.filter(!_._2).map(_._1).mkString(",")))))
        }
        else {
          val response: Future[WSResponse] = ws.url(INFER_URL)
            .withAuth(ConfigReader.kyloUser, ConfigReader.kyloPwd, WSAuthScheme.BASIC)
            .post(akka.stream.scaladsl.Source(FilePart("file", sampleFile.getName,
              Option("text/csv"), FileIO.fromFile(sampleFile)) :: DataPart("parser",
              serde) :: List()))

          response.flatMap(r => {
            logger.debug(s"response from kylo infer schema: ${r.json.toString}")
            Future.successful(Right(Json.stringify(r.json)))
          })
        }
    }
  }
}
