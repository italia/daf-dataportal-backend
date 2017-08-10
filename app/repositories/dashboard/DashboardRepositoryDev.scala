package repositories.dashboard

import java.io.File
import java.util.Date

import ftd_api.yaml.{Catalog, DashboardIframes, Success}
import java.nio.file.Files
import java.nio.file.StandardCopyOption

import com.mongodb.DBObject
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.concurrent.Future
import scala.io.Source


/**
  * Created by ale on 14/04/17.
  */
class DashboardRepositoryDev extends DashboardRepository{

  import scala.concurrent.ExecutionContext.Implicits._

  def save(upFile :File,tableName :String, fileType :String) :Success = {
    val message = s"Table created  $tableName"
    val fileName = new Date().getTime() + ".txt"
    val currentPath =  new java.io.File(".").getCanonicalPath
    val copyFile = new File(System.getProperty("user.home") + "/metabasefile/" + tableName)
    copyFile.mkdirs()
    val copyFilePath = copyFile.toPath
    Files.copy(upFile.toPath, copyFilePath, StandardCopyOption.REPLACE_EXISTING)
    if(fileType.toLowerCase.equals("json")){
      val fileString = Source.fromFile(upFile).getLines().mkString
      val jsonArray: Option[JsArray] = DashboardUtil.toJson(fileString)
      val readyToBeSaved = DashboardUtil.convertToJsonString(jsonArray)
      readyToBeSaved.foreach(x => {
        val jsonStr = x.toString()
        println(jsonStr)
      })
    } else if (fileType.toLowerCase.equals("csv")) {
      val csvs = DashboardUtil.trasformMap(upFile)
      val jsons: Seq[JsObject] = csvs.map(x => Json.toJson(x).as[JsObject])
      jsons.foreach(x => {
        val jsonStr = x.toString()
        println(jsonStr)
      })
    }
    Success(Some(message), Some("Good!!"))
  }

  def update(upFile :File,tableName :String, fileType :String) :Success = {
    val message = s"Table created  $tableName"
    val fileName = new Date().getTime() + ".txt"
    val copyFile = new File(System.getProperty("user.home") + "/metabasefile/" + tableName)
    copyFile.mkdirs()
    val copyFilePath = copyFile.toPath
    Files.copy(upFile.toPath, copyFilePath, StandardCopyOption.REPLACE_EXISTING)
    Success(Some(message), Some("Good!!"))
  }

  def tables() :Seq[Catalog] = {
     Seq(Catalog(None), Catalog(None))
  }

  def iframes() :Future[Seq[DashboardIframes]] = {
    Future(Seq(DashboardIframes(None,None,None)))
  }

}
