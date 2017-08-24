package repositories.dashboard

import java.io.File
import java.nio.file.{Files, StandardCopyOption}
import java.util.Date

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb.{DBObject, casbah}
import com.mongodb.casbah.Imports.{MongoCredential, MongoDBObject, ServerAddress}
import com.mongodb.casbah.{MongoClient, TypeImports}
import ftd_api.yaml.{Catalog, Dashboard, DashboardIframes, Success}
import org.bson.types.ObjectId
import play.api.libs.json._
import play.api.libs.ws.ahc.AhcWSClient
import utils.ConfigReader

import scala.collection.immutable.List
import scala.concurrent.Future
import scala.io.Source
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

  private val userName = ConfigReader.userName
  private val source = ConfigReader.database
  private val password = ConfigReader.password

  val server = new ServerAddress(mongoHost, 27017)
  val credentials = MongoCredential.createCredential(userName, source, password.toCharArray)

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

  def dashboards(user :String): Seq[Dashboard] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    val results = coll.find().toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
    println(json)
    val dashboardsJsResult = json.validate[Seq[Dashboard]]
    val dashboards = dashboardsJsResult match {
      case s: JsSuccess[Seq[Dashboard]] => {
        s.get.foreach(x => {println(x._id.get.$oid.get)})
        s.get
      }
      case e: JsError => Seq()
    }
    dashboards
  }


  def dashboardById(user: String, id: String) :Dashboard = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
   // val objectId = new ObjectId(id)
   // val query = MongoDBObject("_id" -> objectId)
   val query = MongoDBObject("title" -> id)
    val result = coll.findOne(query)
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(result)
    val json = Json.parse(jsonString)
    val dashboardJsResult: JsResult[Dashboard] = json.validate[Dashboard]
    val dashboard: Dashboard = dashboardJsResult match {
      case s: JsSuccess[Dashboard] => s.get
      case e: JsError => Dashboard(None,None,None,None,None,None)
    }
    dashboard
  }

  def saveDashboard(dashboard: Dashboard): Success = {
    import ftd_api.yaml.ResponseWrites.DashboardWrites
    val json: JsValue = Json.toJson(dashboard)
    val id = dashboard.title.get
    val query = MongoDBObject("title" -> id)
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    val inserted: casbah.TypeImports.WriteResult = coll.update(query,obj,true)
    val response = Success(Some("Inserted"),Some("Inserted"))
    response

  }

}
