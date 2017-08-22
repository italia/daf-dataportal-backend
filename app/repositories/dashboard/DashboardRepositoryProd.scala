package repositories.dashboard

import java.io.File
import java.nio.file.{Files, StandardCopyOption}
import java.util.Date

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb.DBObject
import com.mongodb.casbah.MongoClient
import ftd_api.yaml.{Catalog, DashboardIframes, Success}
import play.api.libs.json._
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ahc.AhcWSClient
import utils.ConfigReader

import scala.concurrent.{Future, Promise}
import scala.io.Source
import scala.util
import scala.util.{Failure, Try}

/**
  * Created by ale on 14/04/17.
  */

class DashboardRepositoryProd extends DashboardRepository{

  import ftd_api.yaml.BodyReads._
  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort

  private val localUrl = ConfigReader.getLocalUrl

  private val supersetUser = ConfigReader.getSupersetUser

  def save(upFile :File,tableName :String, fileType :String) :Success = {
    val message = s"Table created  $tableName"
    val fileName = new Date().getTime() + ".txt"
    val copyFile = new File(System.getProperty("user.home") + "/metabasefile/" + fileName + "_" + tableName)
    copyFile.mkdirs()
    val copyFilePath = copyFile.toPath
    Files.copy(upFile.toPath, copyFilePath, StandardCopyOption.REPLACE_EXISTING)
    val mongoClient = MongoClient(mongoHost, mongoPort)
    val db = mongoClient("monitor_mdb")
    val coll = db(tableName)
    if(fileType.toLowerCase.equals("json")){
      val fileString = Source.fromFile(upFile).getLines().mkString
      val jsonArray: Option[JsArray] = DashboardUtil.toJson(fileString)
      val readyToBeSaved = DashboardUtil.convertToJsonString(jsonArray)
      readyToBeSaved.foreach(x => {
        val jsonStr = x.toString()
        val obj  = com.mongodb.util.JSON.parse(jsonStr).asInstanceOf[DBObject]
        coll.insert(obj)
      })
    } else if (fileType.toLowerCase.equals("csv")) {
       val csvs = DashboardUtil.trasformMap(upFile)
       val jsons: Seq[JsObject] = csvs.map(x => Json.toJson(x).as[JsObject])
       jsons.foreach(x => {
        val jsonStr = x.toString()
        val obj  = com.mongodb.util.JSON.parse(jsonStr).asInstanceOf[DBObject]
        coll.insert(obj)
      })
    }
    mongoClient.close()
    val meta =  new MetabaseWs
    meta.syncMetabase()
    Success(Some(message), Some("Good!!"))
  }

  def update(upFile :File,tableName :String, fileType :String) :Success = {
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
    if(fileType.toLowerCase.equals("json")){
      readyToBeSaved.foreach(x => {
        val jsonStr = x.toString()
        val obj  = com.mongodb.util.JSON.parse(jsonStr).asInstanceOf[DBObject]
        coll.insert(obj)
      })
    } else if (fileType.toLowerCase.equals("csv")) {
      val csvs = DashboardUtil.trasformMap(upFile)
      val jsons: Seq[JsObject] = csvs.map(x => Json.toJson(x).as[JsObject])
      jsons.foreach(x => {
        val jsonStr = x.toString()
        val obj  = com.mongodb.util.JSON.parse(jsonStr).asInstanceOf[DBObject]
        coll.insert(obj)
      })
    }
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


  def iframes(metaUser :String) :Future[Seq[DashboardIframes]] = {
    val wsClient = AhcWSClient()
    val metabasePublic = localUrl + "/metabase/public_card/" + metaUser
    val supersetPublic = localUrl + "/superset/public_slice/" + supersetUser

    val request = wsClient.url(metabasePublic).get()
    val requestIframes = wsClient.url(supersetPublic).get()

    val metabase: Future[Seq[DashboardIframes]] = request.map { response =>
      val json = response.json.as[Seq[JsValue]]
      json.map(x => {
        val uuid = (x \ "public_uuid").get.as[String]
        val title = (x \ "name").get.as[String]
        val url = ConfigReader.getMetabaseUrl + "/public/question/" + uuid
        DashboardIframes(Some(url), Some("metabase"), Some(title))
      })
    }

    val superset: Future[Seq[DashboardIframes]] = requestIframes.map { response =>
      val json = response.json.as[Seq[JsValue]]
      json.map(x => {
        val slice_link = (x \ "slice_link").get.as[String]
        val title = (x \ "viz_type").get.as[String]
        val src = slice_link.slice(slice_link.indexOf("\"") + 1,slice_link.lastIndexOf("\"")) + "&standalone=true"
        val url = ConfigReader.getSupersetUrl + src
        DashboardIframes(Some(url), Some("superset"), Some(title))
      })
    }

    val services: Seq[Future[Seq[DashboardIframes]]] = List(metabase,superset)

    def futureToFutureTry[T](f: Future[T]): Future[Try[T]] =
        f.map(scala.util.Success(_)).recover{ case t: Throwable => Failure( t ) }

    val withFailed: Seq[Future[Try[Seq[DashboardIframes]]]] = services.map(futureToFutureTry(_))

    // Can also be done more concisely (but less efficiently) as:
    // f.map(Success(_)).recover{ case t: Throwable => Failure( t ) }
    // NOTE: you might also want to move this into an enrichment class
    //def mapValue[T]( f: Future[Seq[T]] ): Future[Try[Seq[T]]] = {
    //  val prom = Promise[Try[Seq[T]]]()
    //  f onComplete prom.success
    //  prom.future
    //}

    val servicesWithFailed  = Future.sequence(withFailed)

    val servicesSuccesses: Future[Seq[Try[Seq[DashboardIframes]]]] = servicesWithFailed.map(_.filter(_.isSuccess))

    val results: Future[Seq[DashboardIframes]] = servicesSuccesses.map(_.flatMap(_.toOption).flatten)

    results
  }

}
