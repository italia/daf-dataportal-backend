package repositories.dashboard

import java.io.File
import java.nio.file.{Files, StandardCopyOption}
import java.util.Date

import com.mongodb.DBObject
import com.mongodb.casbah.MongoClient
import ftd_api.yaml.{Catalog, Success}
import play.api.libs.json.JsArray
import utils.ConfigReader
import ftd_api.yaml.Catalog

import scala.io.Source

/**
  * Created by ale on 14/04/17.
  */

class DashboardRepositoryProd extends DashboardRepository{

  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort

  def save(upFile :File,tableName :String) :Success = {
    val message = s"Table created  $tableName"
    val fileName = new Date().getTime() + ".txt"
    val copyFile = new File(System.getProperty("user.home") + "/metabasefile/" + fileName + "_" + tableName)
    copyFile.mkdirs()
    val copyFilePath = copyFile.toPath
    Files.copy(upFile.toPath, copyFilePath, StandardCopyOption.REPLACE_EXISTING)
    val fileString = Source.fromFile(upFile).getLines().mkString
    val jsonArray: Option[JsArray] = DashboardUtil.toJson(fileString)
    val readyToBeSaved = DashboardUtil.convertToJsonString(jsonArray)
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db(tableName)
    readyToBeSaved.foreach(x => {
      val jsonStr = x.toString()
      val obj  = com.mongodb.util.JSON.parse(jsonStr).asInstanceOf[DBObject]
      coll.insert(obj)
    })
    mongoClient.close()
    val meta =  new MetabaseWs
    meta.syncMetabase(
      metaemail,
      metapass)
    Success(Some(message), Some("Good!!"))
  }

  def update(upFile :File,tableName :String) :Success = {
    val message = s"Table updated  $tableName"
    val fileName = new Date().getTime() + ".txt"
    val copyFile = new File(System.getProperty("user.home") + "/metabasefile/" + fileName + "_" + tableName)
    copyFile.mkdirs()
    val copyFilePath = copyFile.toPath
    Files.copy(upFile.toPath, copyFilePath, StandardCopyOption.REPLACE_EXISTING)
    val fileString = Source.fromFile(upFile).getLines().mkString
    val jsonArray: Option[JsArray] = DashboardUtil.toJson(fileString)
    val readyToBeSaved = DashboardUtil.convertToJsonString(jsonArray)
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db(tableName)
    coll.drop()
    readyToBeSaved.foreach(x => {
      val jsonStr = x.toString()
      val obj  = com.mongodb.util.JSON.parse(jsonStr).asInstanceOf[DBObject]
      coll.insert(obj)
    })
    mongoClient.close()
    Success(Some(message), Some("Good!!"))
  }

    def tables() :Seq[Catalog] = {
      val mongoClient = MongoClient(mongoHost, mongoPort)
      val db = mongoClient("monitor_mdb")
      val collections = db.collectionNames()
      val catalogs = collections.map(x => Catalog(Some(x))).toSeq
      mongoClient.close()
      catalogs
    }

}
