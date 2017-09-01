package repositories.dashboard

import java.io.{File, FileReader}
import java.util

import au.com.bytecode.opencsv.CSVReader
import play.api.libs.json._

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.io.Source
import scala.util.Try

/**
  * Created by ale on 17/04/17.
  */


object DashboardUtil {

  private val SEPARATORS = Array('\t', ':' , ';' , ',' , '#', '|' , '!')

  def toJson(fileString :String) : Option[JsArray] = {
    val json = Json.parse(fileString)
    val result = json match {
      case x : JsArray => Some(x)
      case _ => None
    }
    println(result)
    result
  }

  def convertToJsonString(json :Option[JsArray]) :Seq[JsObject] = {
    val tmp = json.get
    tmp.as[Seq[JsObject]]
  }

  private def inferSeparator(headers :String) : Char = {
    val maxes = SEPARATORS.map(x => headers.count(_ == x)).zipWithIndex
    val max = (maxes.maxBy(_._1))
    SEPARATORS(max._2)
  }

  def trasformMap(file :File): Seq[Map[String,JsValue]] = {
    val header = Source.fromFile(file).getLines().toList.head
    val separator = inferSeparator(header)
    val reader = new CSVReader(new FileReader(file), separator)
    val csvLines: util.List[Array[String]] = reader.readAll
    val lines :mutable.Seq[Array[String]] = csvLines.asScala
    val headers = lines(0)
    val data: mutable.Seq[Array[String]] = lines.slice(1, (lines.length))
    val result: mutable.Seq[Map[String, JsValue]] = data.map{ xs =>
      val jsons = xs.zipWithIndex.map{case (value , in) =>{
        val toCast: Any = Try(value.toInt).getOrElse(Try(value.toDouble).getOrElse(value))
        val casted: JsValue = toCast match {
          case x: Int => JsNumber(x)
          case y: String => JsString(y)
          case z: Double => JsNumber(z)
        }
        (headers(in) -> casted)
      }

      }
      jsons.toMap
    }
    result.toSeq
  }
}