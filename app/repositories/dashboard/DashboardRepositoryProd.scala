package repositories.dashboard

import java.io.File
import java.net.URL
import java.nio.file.{Files, StandardCopyOption}
import java.util.{Date, UUID}
import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.mongodb
import com.mongodb.DBObject
import com.mongodb.casbah.Imports.{MongoCredential, MongoDBObject, ServerAddress}
import com.mongodb.casbah.{MongoClient, MongoCollection}
import ftd_api.yaml.{Catalog, Dashboard, DashboardIframes, DataApp, Filters, SearchResult, Success, UserStory}
import play.api.libs.json._
import play.api.libs.ws.ahc.AhcWSClient
import utils.ConfigReader

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Try}
import com.sksamuel.elastic4s.http.search.{SearchHit, SearchResponse}
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.HttpClient
import com.sksamuel.elastic4s.searches.SearchDefinition
import com.sksamuel.elastic4s.searches.queries._
import org.elasticsearch.index.query.Operator
import play.api.{Configuration, Environment, Logger}

/**
  * Created by ale on 14/04/17.
  */

class DashboardRepositoryProd extends DashboardRepository {

  import ftd_api.yaml.BodyReads._
  import scala.concurrent.ExecutionContext.Implicits._

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort

  private val localUrl = ConfigReader.getLocalUrl

  private val securityManagerUrl = ConfigReader.securityManHost

  private val supersetUser = ConfigReader.getSupersetUser

  private val userName = ConfigReader.userName
  private val source = ConfigReader.database
  private val password = ConfigReader.password

  val server = new ServerAddress(mongoHost, 27017)
  val credentials = MongoCredential.createCredential(userName, source, password.toCharArray)

  val serverMongoMeta = new ServerAddress("metabase.default.svc.cluster.local", 27017)
  val credentialsMongoMeta = MongoCredential.createCredential("metabase", "metabase", "metabase".toCharArray)

  private val elasticsearchUrl = ConfigReader.getElasticsearchUrl
  private val elasticsearchPort = ConfigReader.getElasticsearchPort

  val conf = Configuration.load(Environment.simple())
  val URLMETABASE = conf.getString("metabase.url").get
  val metauser = conf.getString("metabase.user").get
  val metapass = conf.getString("metabase.pass").get



  private def  metabaseTableInfo(iframes :Seq[DashboardIframes]): Future[Seq[DashboardIframes]] = {

    val wsClient = AhcWSClient()
    val futureFrames: Seq[Future[Try[DashboardIframes]]] = iframes.map { iframe =>
      val tableId = iframe.table.getOrElse("0")
      val internalUrl = localUrl + s"/metabase/table/$tableId"
      // Missing metabase session not working
      val futureTry : Future[Try[DashboardIframes]]= wsClient.url(internalUrl).get()
       .map { resp =>
         val tableName = (resp.json \ "name").as[String]
         iframe.copy(table = Some(tableName))
       }.map { x:DashboardIframes => scala.util.Success(x)}
        .recover { case t: Throwable => Failure(t) }

      futureTry
    }

    val seq: Future[Seq[Try[DashboardIframes]]] = Future.sequence(futureFrames)
    val dashboardIframes: Future[Seq[DashboardIframes]] = seq.map(
             _.filter(_.isSuccess)
              .map(_.get))
    dashboardIframes
    //val d: Future[Any] = seq.collect{ case scala.util.Success(x) => x}

  }

  def save(upFile: File, tableName: String, fileType: String): Success = {
    val message = s"Table created  $tableName"
    val fileName = new Date().getTime() + ".txt"
    val copyFile = new File(System.getProperty("user.home") + "/metabasefile/" + fileName + "_" + tableName)
    copyFile.mkdirs()
    val copyFilePath = copyFile.toPath
    Files.copy(upFile.toPath, copyFilePath, StandardCopyOption.REPLACE_EXISTING)
    //    val mongoClient = MongoClient(mongoHost, mongoPort)
    //    val db = mongoClient("monitor_mdb")
    //val mongoClient = MongoClient(server, List(credentials))
    //val db = mongoClient(source)
    //val mongoClient = MongoClient(serverMongoMeta, List(credentialsMongoMeta))
    //val db = mongoClient("metabase")
    val mongoClient = MongoClient(serverMongoMeta, List(credentialsMongoMeta))
    val db = mongoClient("metabase")
    val coll = db(tableName)
    if (fileType.toLowerCase.equals("json")) {
      val fileString = Source.fromFile(upFile).getLines().mkString
      val jsonArray: Option[JsArray] = DashboardUtil.toJson(fileString)
      val readyToBeSaved = DashboardUtil.convertToJsonString(jsonArray)
      readyToBeSaved.foreach(x => {
        val jsonStr = x.toString()
        val obj = com.mongodb.util.JSON.parse(jsonStr).asInstanceOf[DBObject]
        coll.insert(obj)
      })
    } else if (fileType.toLowerCase.equals("csv")) {
      val csvs = DashboardUtil.trasformMap(upFile)
      val jsons: Seq[JsObject] = csvs.map(x => Json.toJson(x).as[JsObject])
      jsons.foreach(x => {
        val jsonStr = x.toString()
        val obj = com.mongodb.util.JSON.parse(jsonStr).asInstanceOf[DBObject]
        coll.insert(obj)
      })
    }
    mongoClient.close()
    val meta = new MetabaseWs
    //meta.syncMetabase()
    Success(Some(message), Some("Good!!"))
  }

  def update(upFile: File, tableName: String, fileType: String): Success = {
    val message = s"Table updated  $tableName"
    val fileName = new Date().getTime() + ".txt"
    val copyFile = new File(System.getProperty("user.home") + "/metabasefile/" + fileName + "_" + tableName)
    copyFile.mkdirs()
    val copyFilePath = copyFile.toPath
    Files.copy(upFile.toPath, copyFilePath, StandardCopyOption.REPLACE_EXISTING)
    val fileString = Source.fromFile(upFile).getLines().mkString
    val jsonArray: Option[JsArray] = DashboardUtil.toJson(fileString)
    val readyToBeSaved = DashboardUtil.convertToJsonString(jsonArray)
    //val mongoClient = MongoClient(server, List(credentials))
    //val db = mongoClient(source)
    val mongoClient = MongoClient(serverMongoMeta, List(credentialsMongoMeta))
    val db = mongoClient("metabase")
    val coll = db(tableName)
    coll.drop()
    if (fileType.toLowerCase.equals("json")) {
      readyToBeSaved.foreach(x => {
        val jsonStr = x.toString()
        val obj = com.mongodb.util.JSON.parse(jsonStr).asInstanceOf[DBObject]
        coll.insert(obj)
      })
    } else if (fileType.toLowerCase.equals("csv")) {
      val csvs = DashboardUtil.trasformMap(upFile)
      val jsons: Seq[JsObject] = csvs.map(x => Json.toJson(x).as[JsObject])
      jsons.foreach(x => {
        val jsonStr = x.toString()
        val obj = com.mongodb.util.JSON.parse(jsonStr).asInstanceOf[DBObject]
        coll.insert(obj)
      })
    }
    mongoClient.close()
    Success(Some(message), Some("Good!!"))
  }

  def tables(): Seq[Catalog] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val collections = db.collectionNames()
    val catalogs = collections.map(x => Catalog(Some(x))).toSeq
    mongoClient.close()
    catalogs
  }

  def iframesByOrg(user: String,org: String): Future[Seq[DashboardIframes]] = {

    val result = for {
      tables <- getSupersetTablesByOrg(org)
      a <- iFramesByTables(user,tables)
    } yield a

    result

  }

  private def getSupersetTablesByOrg(org: String): Future[Seq[String]] = {

    val wsClient = AhcWSClient()
    //security-manager/v1/internal/superset/find_orgtables/default_org

    val internalUrl = securityManagerUrl + "/security-manager/v1/internal/superset/find_orgtables/"+org
    println("internalUrl: "+internalUrl)

    wsClient.url(internalUrl).get().map{ resp =>
      println("getSupersetTablesByOrg response: "+resp)
      (resp.json \ "tables").as[Seq[String]]
    }

  }

  private def iFramesByTables(user: String, tables:Seq[String]): Future[Seq[DashboardIframes]]= {
    iframes(user).map{ _.filter(dash => dash.table.nonEmpty && tables.contains(dash.table.get) ) }
  }

  def iframes(user: String): Future[Seq[DashboardIframes]] = {
    println("-iframes-")
    val wsClient = AhcWSClient()

    val metabasePublic = localUrl + "/metabase/public_card/" + user
    val supersetPublic = localUrl + "/superset/public_slice/" + user
    val grafanaPublic = localUrl + "/grafana/snapshots/" + user
    val tdmetabasePublic = localUrl + "/tdmetabase/public_card"


    val request = wsClient.url(metabasePublic).get()
    // .andThen { case _ => wsClient.close() }
    // .andThen { case _ => system.terminate() }

    val requestIframes = wsClient.url(supersetPublic).get()
    //  .andThen { case _ => wsClient.close() }
    //  .andThen { case _ => system.terminate() }

    val requestSnapshots = wsClient.url(grafanaPublic).get()

    val requestTdMetabase = wsClient.url(tdmetabasePublic).get


    val superset: Future[Seq[DashboardIframes]] = requestIframes.map { response =>
      val json = response.json.as[Seq[JsValue]]
      val iframes = json.map(x => {
        val slice_link = (x \ "slice_link").get.as[String]
        val vizType = (x \ "viz_type").get.as[String]
        val title = slice_link.slice(slice_link.indexOf(">") + 1, slice_link.lastIndexOf("</a>")).trim
        val src = slice_link.slice(slice_link.indexOf("\"") + 1, slice_link.lastIndexOf("\"")) + "&standalone=true"
        val url = ConfigReader.getSupersetUrl + src
        val decodeSuperst = java.net.URLDecoder.decode(url, "UTF-8");
        val uri = new URL(decodeSuperst)
        val queryString = uri.getQuery.split("&", 2)(0)
        val valore = queryString.split("=", 2)(1)
        if (valore.contains("{\"code") || vizType.equals("separtor") || vizType.equals("filter")) {
          DashboardIframes(None, None, None, None, None, None)
        } else {
          try {
            val identifierJson = Json.parse(s"""$valore""")
            val slice_id = (identifierJson \ "slice_id").asOpt[Int].getOrElse(0)
            val table = (x \ "datasource_link").get.asOpt[String].getOrElse("").split(">").last.split("<").head
            DashboardIframes( Some("superset_" + slice_id.toString), Some(url),Some(vizType), Some("superset"), Some(title), Some(table) )
          } catch {
            case e: Exception => e.printStackTrace(); /*println(x);println("ERROR"); */ DashboardIframes(None, None, None, None, None, None)
          }
        }
      })

      val filtered = iframes.filter {
        case DashboardIframes(Some(_), Some(_), Some(_), Some(_), Some(_), Some(_)) => true
        case _ => false
      }
      filtered.sortWith((a,b) => (a.identifier.get > b.identifier.get))

    }

    val metabase: Future[Seq[DashboardIframes]] = request.map { response =>
      val json = response.json.as[Seq[JsValue]]

      Logger.debug(s"Metabase iframe response: $json")
      json.filter( x => !(x \ "public_uuid").asOpt[String].isEmpty)
        .map(x => {
         val uuid = (x \ "public_uuid").get.as[String]
         val title = (x \ "name").get.as[String]
      //  val id = (x \ "id").get.as[String]
         val tableId =   (x \ "table_id").get.as[Int]
         val url = ConfigReader.getMetabaseUrl + "/public/question/" + uuid
         DashboardIframes( Some("metabase_" + uuid), Some(url), None, Some("metabase"), Some(title), Some(tableId.toString))
      })
    }



    val test: Future[Seq[DashboardIframes]] = for {
        iframes <- metabase
        iframesWithTable <- metabaseTableInfo(iframes)
    } yield iframesWithTable



  /*   val tdMetabase  :Future[Seq[DashboardIframes]] = requestTdMetabase.map { response =>
       val json = response.json.as[Seq[JsValue]]

       Logger.debug(s"tMetabase iframe response: $json")
       json.map( x => {
         val uuid = (x \ "public_uuid").get.as[String]
         val title = (x \ "name").get.as[String]
         val url = ConfigReader.getTdMetabaseURL + "/public/question/" + uuid
         DashboardIframes( Some("metabase_" + uuid), Some(url), None, Some("metabase"), Some(title), None)
         //DashboardIframes(Some(url), Some("tdmetabase"), Some(title), Some("tdmetabase_" + uuid))
       })
     }


    val grafana: Future[Seq[DashboardIframes]] = requestSnapshots.map { response =>
      val json = response.json.as[Seq[JsValue]]
      json.map(x => {
        println("QUI VEDIAMO")
        println(x)
        val uuid = (x \ "key").get.as[String]
        val id = (x \ "id").get.as[Int]
        val title = (x \ "name").get.as[String]
        val url = ConfigReader.getGrafanaUrl + "/dashboard/snapshot/" + uuid
        DashboardIframes( Some("grafana_" + id.toString), Some(url),None,Some("grafana"), Some(title), None)
      })
    }
*/

    val services  = List(test, superset) //, grafana, tdMetabase)

    def futureToFutureTry[T](f: Future[T]): Future[Try[T]] =
      f.map(scala.util.Success(_)).recover { case t: Throwable => Failure(t) }

    val withFailed: Seq[Future[Try[Seq[DashboardIframes]]]] = services.map(futureToFutureTry(_))

    // Can also be done more concisely (but less efficiently) as:
    // f.map(Success(_)).recover{ case t: Throwable => Failure( t ) }
    // NOTE: you might also want to move this into an enrichment class
    //def mapValue[T]( f: Future[Seq[T]] ): Future[Try[Seq[T]]] = {
    //  val prom = Promise[Try[Seq[T]]]()
    //  f onComplete prom.success
    //  prom.future
    //}

    val servicesWithFailed = Future.sequence(withFailed)

    val servicesSuccesses: Future[Seq[Try[Seq[DashboardIframes]]]] = servicesWithFailed.map(_.filter(_.isSuccess))

    val results: Future[Seq[DashboardIframes]] = servicesSuccesses.map(_.flatMap(_.toOption).flatten)

    results
  }

  def dashboards(username: String, groups: List[String], status: Option[Int]): Seq[Dashboard] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    val query = status match {
      case Some(x) => MongoDBObject("published" -> x)
      case None => MongoDBObject()
    }
    val results = coll.find(query).sort(MongoDBObject("timestamp" -> -1)).toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
//    println(json)
    val dashboardsJsResult = json.validate[Seq[Dashboard]]
    val dashboards = dashboardsJsResult match {
      case s: JsSuccess[Seq[Dashboard]] => s.get
      case _: JsError => Seq()
    }
    dashboards.filter(dash => checkDashboardResponse(dash, username, groups))
  }

  def dashboardsPublic(org: Option[String]): Seq[Dashboard] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    val results = publicQueryToMongo(coll, org, "published")
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
    val dashboardsJsResult = json.validate[Seq[Dashboard]]
    val dashboards = dashboardsJsResult match {
      case s: JsSuccess[Seq[Dashboard]] => s.get
      case e: JsError => Seq()
    }
    dashboards
  }

  def dashboardById(username: String, groups: List[String], id: String): Dashboard = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    //  val objectId = new org.bson.types.ObjectId(id)
    val query = MongoDBObject("id" -> id)
    // val query = MongoDBObject("title" -> id)
    val result = coll.findOne(query)
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(result)
    val json = Json.parse(jsonString)
    val dashboardJsResult: JsResult[Dashboard] = json.validate[Dashboard]
    val dashboard: Dashboard = dashboardJsResult match {
      case s: JsSuccess[Dashboard] => s.get
      case e: JsError => Dashboard(None, None, None, None, None, None, None, None, None, None)
    }
    if(checkDashboardResponse(dashboard, username, groups)) dashboard
    else Dashboard(None, None, None, None, None, None, None, None, None, None)
  }

  def publicDashboardById(id: String): Dashboard = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    //  val objectId = new org.bson.types.ObjectId(id)
    val query = MongoDBObject("id" -> id)
    // val query = MongoDBObject("title" -> id)
    val result = coll.findOne(query)
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(result)
    val json = Json.parse(jsonString)
    val dashboardJsResult: JsResult[Dashboard] = json.validate[Dashboard]
    val dashboard: Dashboard = dashboardJsResult match {
      case s: JsSuccess[Dashboard] => s.getOrElse(Dashboard(None, None, None, None, None, None, None, None, None, None))
      case e: JsError => Dashboard(None, None, None, None, None, None, None, None, None, None)
    }
    if(checkOpenDashboardResponse(dashboard)) dashboard
    else Dashboard(None, None, None, None, None, None, None, None, None, None)
  }

  def saveDashboard(dashboard: Dashboard, user: String): Success = {
    import ftd_api.yaml.ResponseWrites.DashboardWrites
    val id = dashboard.id
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    var saved = "Not Saved"
    var operation = "Not Saved"
    id match {
      case Some(x) => {
        val json: JsValue = Json.toJson(dashboard)
        val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
        val query = MongoDBObject("id" -> x)
        saved = id.get
        operation = "updated"
        val a: mongodb.casbah.TypeImports.WriteResult = coll.update(query, obj)
      }
      case None => {
        dashboard.title match {
          case Some(x) =>
            val uid = UUID.randomUUID().toString
            val timestamps = ZonedDateTime.now()
            val newDash = dashboard.copy(id = Some(uid), user = Some(user), timestamp = Some(timestamps))
            val json: JsValue = Json.toJson(newDash)
            val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
            saved = uid
            operation = "inserted"
            coll.save(obj)
        }
      }
    }
    mongoClient.close()
    val response = Success(Some(saved), Some(operation))
    response

  }

  def deleteDashboard(dashboardId: String): Success = {
    val query = MongoDBObject("id" -> dashboardId)
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    val removed = coll.remove(query)
    val response = Success(Some("Deleted"), Some("Deleted"))
    response
  }

  def stories(username: String, groups: List[String], status: Option[Int], page: Option[Int], limit: Option[Int]): Seq[UserStory] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val query = status match {
      case Some(x) => MongoDBObject("published" -> x)
      case None => MongoDBObject()
    }
    val coll = db("stories")
    val results = coll.find(query)
      .sort(MongoDBObject("timestamp" -> -1))
      .skip(page.getOrElse(0))
      .limit(limit.getOrElse(100)).toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
//    println(json)
    val storiesJsResult = json.validate[Seq[UserStory]]
    val stories = storiesJsResult match {
      case s: JsSuccess[Seq[UserStory]] => s.get
      case e: JsError => Seq()
    }
    stories.filter(story => checkStoryResponse(story, username, groups))
  }

  def storiesPublic(org: Option[String]): Seq[UserStory] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("stories")
    val results = publicQueryToMongo(coll, org, "published")
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
    val storiesJsResult = json.validate[Seq[UserStory]]
    val stories = storiesJsResult match {
      case s: JsSuccess[Seq[UserStory]] => s.get
      case e: JsError => Seq()
    }
    stories
  }

  private def publicQueryToMongo(coll: MongoCollection, org: Option[String], nameStatus: String): List[DBObject] = {
    import mongodb.casbah.query.Imports._

    val orgQuery = org match {
      case Some(x) => mongodb.casbah.Imports.MongoDBObject("org" -> x)
      case None => mongodb.casbah.Imports.MongoDBObject()
    }
    val statusQuery = mongodb.casbah.Imports.MongoDBObject(nameStatus -> 2)
    val sortQuery = mongodb.casbah.Imports.MongoDBObject("timestamp" -> -1)

    coll.find($and(orgQuery, statusQuery)).sort(sortQuery).toList
  }

  def storyById(username: String, groups: List[String], id: String): UserStory = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("stories")
    //  val objectId = new org.bson.types.ObjectId(id)
    val query = MongoDBObject("id" -> id)
    // val query = MongoDBObject("title" -> id)
    val result = coll.findOne(query)
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(result)
    val json = Json.parse(jsonString)
    val storyJsResult: JsResult[UserStory] = json.validate[UserStory]
    val story: UserStory = storyJsResult match {
      case s: JsSuccess[UserStory] => s.get
      case e: JsError => UserStory(None, None, None, None, None, None, None, None, None, None)
    }
    if(checkStoryResponse(story, username, groups)) story
    else UserStory(None, None, None, None, None, None, None, None, None, None)
  }

  def publicStoryById(id: String): UserStory = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("stories")
    //  val objectId = new org.bson.types.ObjectId(id)
    val query = MongoDBObject("id" -> id)
    // val query = MongoDBObject("title" -> id)
    val result = coll.findOne(query)
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(result)
    val json = Json.parse(jsonString)
    val storyJsResult: JsResult[UserStory] = json.validate[UserStory]
    val story: UserStory = storyJsResult match {
      case s: JsSuccess[UserStory] => s.get
      case _: JsError => UserStory(None, None, None, None, None, None, None, None, None, None)
    }
    if(checkOpenStoryResponse(story)) story
    else UserStory(None, None, None, None, None, None, None, None, None, None)
  }

  def saveStory(story: UserStory, user: String): Success = {
    import ftd_api.yaml.ResponseWrites.UserStoryWrites
    val id = story.id
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("stories")
    var saved = "Not Saved"
    var operation = "Not Saved"
    id match {
      case Some(x) => {
        val json: JsValue = Json.toJson(story)
        val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
        val query = MongoDBObject("id" -> x)
        saved = id.get
        operation = "updated"
        coll.update(query, obj)
      }
      case None => {
        story.title match {
          case Some(x) =>
            val uid = UUID.randomUUID().toString
            val timestamps = ZonedDateTime.now()
            val newStory = story.copy(id = Some(uid), user = Some(user), timestamp = Some(timestamps))
            val json: JsValue = Json.toJson(newStory)
            val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
            saved = uid
            operation = "inserted"
            coll.save(obj)
        }

      }
    }

    mongoClient.close()
    val response = Success(Some(saved), Some(operation))
    response
  }

  def deleteStory(storyId: String): Success = {
    val query = MongoDBObject("id" -> storyId)
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("stories")
    val removed = coll.remove(query)
    val response = Success(Some("Deleted"), Some("Deleted"))
    response
  }

  private def checkStoryResponse(story: UserStory, username: String, groups: List[String]): Boolean = {
    if((story.user.getOrElse("no_user").equals(username) && story.published.getOrElse(0) == 0) ||
      (groups.contains(story.org.getOrElse("no_org")) && story.published.getOrElse(0) == 1) ||
      checkOpenStoryResponse(story)) true
    else false
  }

  private def checkOpenStoryResponse(story: UserStory): Boolean = {
    story.published.getOrElse(-1) == 2
  }

  private def checkDashboardResponse(dash: Dashboard, username: String, groups: List[String]): Boolean = {
    if((dash.user.getOrElse("no_user").equals(username) && dash.status.getOrElse(0) == 0) ||
      (groups.contains(dash.org.getOrElse("no_org")) && dash.status.getOrElse(0) == 1) ||
      checkOpenDashboardResponse(dash)) true
    else false
  }

  private def checkOpenDashboardResponse(dash: Dashboard): Boolean = {
    dash.status.getOrElse(-1) == 2
  }

  def getAllDataApp: Seq[DataApp] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)

    Logger.logger.debug(s"mongodb address: ${mongoClient.address}")

    val coll = db("data_applications")
    val result = coll.find().toList
    mongoClient.close()
    val jsonString = com.mongodb.util.JSON.serialize(result)
    val json = Json.parse(jsonString)
    val dataAppJsResult = json.validate[Seq[DataApp]]
    val dataApp = dataAppJsResult match {
      case s: JsSuccess[Seq[DataApp]] => s.get
      case _: JsError => Seq()
    }

    Logger.logger.debug(s"find ${dataApp.size} results")

    dataApp
  }

  def searchText(filters: Filters, username: String, groups: List[String]): Future[List[SearchResult]] = {
    Logger.logger.debug(s"elasticsearchUrl: $elasticsearchUrl elasticsearchPort: $elasticsearchPort")

    val client = HttpClient(ElasticsearchClientUri(elasticsearchUrl, elasticsearchPort))
    val index = "ckan"
    val fieldDatasetDcatName = "dcatapit.name"
    val fieldDatasetDcatTitle = "dcatapit.title"
    val fieldDatasetDcatNote = "dcatapit.notes"
    val fieldDatasetDcatTheme = "dcatapit.theme"
    val fieldDatasetDataFieldName = "dataschema.avro.fields.name"
    val fieldUsDsTitle = "title"
    val fieldUsDsSub = "subtitle"
    val fieldUsDsWget = "widgets"
    val fieldDataset = List(fieldDatasetDcatName, fieldDatasetDcatTitle, fieldDatasetDcatNote,
      fieldDatasetDataFieldName, fieldDatasetDcatTheme, "dcatapit.privatex", "dcatapit.modified", "dcatapit.owner_org")
    val fieldsOpenData = List("name", "title", "notes", "organization.name", "theme", "modified")
    val fieldDashboard = listFields("Dashboard")
    val fieldStories = listFields("User-Story")
    val fieldToReturn = fieldDataset ++ fieldDashboard ++ fieldStories ++ fieldsOpenData

    val listFieldSearch = List(fieldDatasetDcatName, fieldDatasetDcatTitle, fieldDatasetDcatNote, fieldDatasetDcatTheme,
      fieldDatasetDataFieldName, fieldUsDsTitle, fieldUsDsSub, fieldUsDsWget, "dcatapit.owner_org", "org") ::: fieldsOpenData

    val searchText = filters.text match {
      case Some("") => false
      case Some(_) => true
      case None => false
    }

    val searchType = filters.index match {
      case Some(List()) => ""
      case Some(x) => x.mkString(",")
      case None => ""
    }

    val order = filters.order match {
      case Some("score") => "score"
      case Some("asc") => "asc"
      case _ => "desc"
    }

    def queryElasticsearch(limitResult: Int, searchTypeInQuery: String) = {
      search(index).types(searchTypeInQuery).query(
        boolQuery()
          must(
            should(
              textStringQuery(filters.text, listFieldSearch.filterNot(f => f.equals(fieldDatasetDcatTheme) || f.equals("theme")))
            ),
            must(
                createFilterOrg(filters.org) ::: createFilterStatus(filters.status) :::
              createThemeFilter(filters.theme)
            ),
            should(
              must(termQuery("dcatapit.privatex", true), matchQuery("dcatapit.owner_org", groups.mkString(" ")).operator("OR")),
              termQuery("dcatapit.privatex", false),
              must(termQuery("status", 0), termQuery("user", username)),
              must(termQuery("published", 0), termQuery("user", username)),
              must(termQuery("status", 1), matchQuery("org", groups.mkString(" ")).operator("OR")),
              must(termQuery("published", 1), matchQuery("org", groups.mkString(" ")).operator("OR")),
              termQuery("status", 2),
              termQuery("published", 2),
              termQuery("private", false)
            )
          )
      ).limit(limitResult)
    }

    val query = queryElasticsearch(1000, searchType).sourceInclude(fieldToReturn)
      .aggregations(
        termsAgg("type", "_type"),
        termsAgg("status_dash", "status"), termsAgg("status_st", "published"), termsAgg("status_cat", "dcatapit.privatex"), termsAgg("status_ext", "private"),
        termsAgg("org_stdash", "org.keyword"), termsAgg("org_cat", "dcatapit.owner_org.keyword").size(1000), termsAgg("org_ext", "organization.name.keyword").size(1000),
        termsAgg("cat_cat", "dcatapit.theme.keyword").size(1000), termsAgg("cat_ext", "theme.keyword").size(1000)
      )
      .highlighting(listFieldSearch
        .filterNot(s => s.equals("org") || s.equals("dcatapit.owner_org"))
        .map(x => highlight(x).preTag("<span style='background-color:#0BD9D3'>").postTag("</span>")
          .fragmentSize(70))
      )

    val queryAggregationNoCat = queryElasticsearch(0, "ext_opendata")
      .fetchSource(false)
      .aggregations(
        missingAgg("no_category", "theme.keyword")
      )

    val responseFutureQuery: Future[SearchResponse] = client.execute(query)
    val futureSearchResults = wrapResponse(responseFutureQuery, order, searchText, filters.date)

    val futureMapAggregation: Future[Map[String, AnyRef]] = responseFutureQuery.map(r => r.aggregations)

    val responseFutureQueryNoCat: Option[Future[Map[String, AnyRef]]] = searchType match {
      case "" => Some(client.execute(queryAggregationNoCat).map(q => q.aggregations))
      case "ext_opendata" => Some(client.execute(queryAggregationNoCat).map(q => q.aggregations))
      case _ => None
    }

    val futureAggregationResults = createAggregationResponse(futureMapAggregation, responseFutureQueryNoCat)

    val response = for{
      searchResult <- futureSearchResults
      aggregationResult <- futureAggregationResults
    } yield searchResult ::: aggregationResult

    response
  }

  def searchTextPublic(filters: Filters): Future[List[SearchResult]] = {
    Logger.logger.debug(s"elasticsearchUrl: $elasticsearchUrl elasticsearchPort: $elasticsearchPort")

    val client: HttpClient = HttpClient(ElasticsearchClientUri(elasticsearchUrl, elasticsearchPort))
    val index = "ckan"
    val fieldDatasetDcatName = "dcatapit.name"
    val fieldDatasetDcatTitle = "dcatapit.title"
    val fieldDatasetDcatNote = "dcatapit.notes"
    val fieldDatasetDcatTheme = "dcatapit.theme"
    val fieldDatasetDataFieldName = "dataschema.avro.fields.name"
    val fieldUsDsTitle = "title"
    val fieldUsDsSub = "subtitle"
    val fieldUsDsWget = "widgets"
    val fieldDataset = List(fieldDatasetDcatName, fieldDatasetDcatTitle, fieldDatasetDcatNote,
      fieldDatasetDataFieldName, fieldDatasetDcatTheme, "dcatapit.privatex", "dcatapit.modified", "dcatapit.owner_org")
    val fieldsOpenData = List("name", "title", "notes", "organization.name", "theme", "modified")
    val fieldStories = listFields("User-Story")
    val fieldToReturn = fieldDataset ++ fieldStories ++ fieldsOpenData

    val listFieldSearch = List(fieldDatasetDcatName, fieldDatasetDcatTitle, fieldDatasetDcatNote, fieldDatasetDcatTheme,
      fieldDatasetDataFieldName, fieldUsDsTitle, fieldUsDsSub, fieldUsDsWget, "dcatapit.owner_org", "org") ::: fieldsOpenData

    val searchText = filters.text match {
      case Some("") => false
      case Some(_) => true
      case None => false
    }

    val searchType = filters.index match {
      case Some(List()) => ""
      case Some(x) => x.mkString(",")
      case None => ""
    }

    val order = filters.order match {
      case Some("score") => "score"
      case Some("asc") => "asc"
      case _ => "desc"
    }

    def queryElasticsearch(limitResult: Int, searchTypeInQuery: String) = {
      search(index).types(searchTypeInQuery).query(
        boolQuery()
          .must(
            should(
              textStringQuery(filters.text, listFieldSearch.filterNot(f => f.equals(fieldDatasetDcatTheme) || f.equals("theme")))
            ),
            must(
              createFilterOrg(filters.org) ::: createFilterStatus(filters.status) :::
                createThemeFilter(filters.theme)
            ),
            should(
              termQuery("dcatapit.privatex", false),
              termQuery("published", 2),
              termQuery("private", false)
            )
          )
      ).limit(limitResult)
    }

    val query = queryElasticsearch(1000, searchType).sourceInclude(fieldToReturn)
      .aggregations(
        termsAgg("type", "_type"),
        termsAgg("status_dash", "status"), termsAgg("status_st", "published"), termsAgg("status_cat", "dcatapit.privatex"), termsAgg("status_ext", "private"),
        termsAgg("org_stdash", "org.keyword").size(1000), termsAgg("org_cat", "dcatapit.owner_org.keyword").size(1000), termsAgg("org_ext", "organization.name.keyword").size(1000),
        termsAgg("cat_cat", "dcatapit.theme.keyword").size(1000), termsAgg("cat_ext", "theme.keyword").size(1000)
      )
      .highlighting(listFieldSearch
        .filterNot(s => s.equals("org") || s.equals("dcatapit.owner_org"))
        .map(x => highlight(x).preTag("<span style='background-color:#0BD9D3'>").postTag("</span>")
          .fragmentSize(70))
      )

    val queryAggregationNoCat = queryElasticsearch(0, "ext_opendata")
      .fetchSource(false)
      .aggregations(
        missingAgg("no_category", "theme.keyword")
      )

    val responseFutureQuery: Future[SearchResponse] = client.execute(query)
    val futureSearchResults = wrapResponse(responseFutureQuery, order, searchText, filters.date)

    val futureMapAggregation: Future[Map[String, AnyRef]] = responseFutureQuery.map(r => r.aggregations)

    val responseFutureQueryNoCat: Option[Future[Map[String, AnyRef]]] = searchType match {
      case "" => Some(client.execute(queryAggregationNoCat).map(q => q.aggregations))
      case "ext_opendata" => Some(client.execute(queryAggregationNoCat).map(q => q.aggregations))
      case _ => None
    }
    val futureAggregationResults = createAggregationResponse(futureMapAggregation, responseFutureQueryNoCat)

    val response = for{
      searchResult <- futureSearchResults
      aggregationResult <- futureAggregationResults
    } yield searchResult ::: aggregationResult

    response
  }

  private def textStringQuery(text: Option[String], listFields: List[String]) = {
    def setOperator(str: String) = {
      if(str.charAt(0) == '\"' && str.last == '\"') "AND"
      else "OR"
    }
    text match {
      case Some("") => List()
      case Some(searchString) => {
        listFields
          .map(field =>
            matchQuery(field, searchString)
              .operator(setOperator(searchString))) ::: themeQueryString(searchString)
      }
      case None => List()
    }
  }

  private def createAggregationResponse(resAggr: Future[Map[String, AnyRef]], responseNoCat: Option[Future[Map[String, AnyRef]]]) = {
    val  futureMapAggr: Future[Map[String, Map[String, Int]]] = resAggr
      .map(mapAggr =>
        mapAggr
          .map{ elem =>
            val name = elem._1
            val valueMap = elem._2.asInstanceOf[Map[String, Any]]("buckets").asInstanceOf[List[Map[String, AnyVal]]]
              .map(elemMap =>
                wrapAggrResp(elemMap.values.toList)
              ).map(v => v._1 -> v._2).toMap
            name -> valueMap
          }
    )

    val futureMapNoCat: Future[Map[String, Int]] = responseNoCat match {
      case Some(futureRespNoCat) => futureRespNoCat.map(mapNoCat => mapNoCat.map(elem => (elem._1, elem._2.asInstanceOf[Map[String, Int]]("doc_count"))))
      case None => Future(Map[String, Int]())
    }

    val futureRespAggr = for{
      futureThemeAggr <- parseAggrTheme(futureMapAggr, futureMapNoCat)
      futureStatusAggr <- parseAggrStatus(futureMapAggr)
      futureOrgAggr <- parseAggrOrg(futureMapAggr)
      futureTypeAggr <- parseAggrType(futureMapAggr)
    }yield List(futureTypeAggr, futureStatusAggr, futureOrgAggr, futureThemeAggr)

    futureRespAggr
  }

  private def parseAggrType(futureMapAggr: Future[Map[String, Map[String, Int]]]) = {
    futureMapAggr.map(mapAggr => SearchResult(Some("type"), Some("{" + mergeAggr(mapAggr("type").toList).mkString(",") + "}"), None))
  }

  private def parseAggrOrg(futureMapAggr: Future[Map[String, Map[String, Int]]]) = {
    val futureListAggrOrg = for{
      orgCat <- futureMapAggr.map(mapAggr => mapAggr("org_cat").toList)
      orgExt <- futureMapAggr.map(mapAggr => mapAggr("org_ext").toList)
      orgStDash <- futureMapAggr.map(mapAggr => mapAggr("org_stdash").toList)
    } yield orgCat ::: orgExt ::: orgStDash

    futureListAggrOrg.map(listAggrOrg => SearchResult(Some("organization"), Some("{" + mergeAggr(listAggrOrg).mkString(",") + "}"), None))
  }

  private def parseAggrStatus(futureMapAggr: Future[Map[String, Map[String, Int]]]): Future[SearchResult] = {
    def convertAggrDataset(mapStatus: Map[String, Int]) = {
      val countTrue: Int = mapStatus.getOrElse("true", 0)
      val countFalse: Int = mapStatus.getOrElse("false", 0)
      Map("0" -> countTrue, "1" -> countTrue, "2" -> countFalse)
    }

    val futureListStatus = for {
      aggrCat <- futureMapAggr.map(mapAggr => convertAggrDataset(mapAggr("status_cat")).toList)
      aggrExt <- futureMapAggr.map(mapAggr => convertAggrDataset(mapAggr("status_ext")).toList)
      aggrDash <- futureMapAggr.map(mapAggr => mapAggr("status_dash").toList)
      aggrSt <- futureMapAggr.map(mapAggr => mapAggr("status_st").toList)
    }yield aggrCat ::: aggrExt ::: aggrDash ::: aggrSt

    futureListStatus.map(listStatus => SearchResult(Some("status"), Some("{" + mergeAggr(listStatus).mkString(",") + "}"), None))
  }

  private def parseAggrTheme(futureMapAggr: Future[Map[String, Map[String, Int]]], futureMapNoCat: Future[Map[String, Int]]): Future[SearchResult] = {
    def extractAggregationTheme(name: String, count: Int): List[(String, Int)] = {
      if(name.contains("theme")){
        val themes = (Json.parse(name) \\ "theme").repr.map(theme => theme.toString().replace("\"", "")).mkString(",")
        themes.split(",").map(t => (t, count)).toList
      }
      else List((name, count))
    }

    val futureListThemeOpen: Future[List[(String, Int)]] = futureMapAggr.map(mapAggr => mapAggr("cat_cat").toList.flatMap(elem => extractAggregationTheme(elem._1, elem._2)))
    val futureCountNoThemeOpen: Future[Int] = futureListThemeOpen.map(listThemeOpen => listThemeOpen.filter(elem => elem._1.equals("[]") || elem._1.equals("")).map(elem => elem._2).sum)
    val futureListNoTheme = for{
      mapThemeNoCat <- futureMapNoCat
      countNoThemeOpen <- futureCountNoThemeOpen
    }  yield List(("no_category", mapThemeNoCat.values.sum + countNoThemeOpen))

    val futureListThemes: Future[List[(String, Int)]] = for {
      mapThemeDaf <- futureMapAggr.map(mapAggr => mapAggr("cat_cat").toList)
      listNoTheme <- futureListNoTheme
      listThemeOpen <- futureListThemeOpen.map(listThemeExt => listThemeExt.filterNot(elem => elem._1.equals("[]") || elem._1.equals("")))
    }yield listThemeOpen ::: mapThemeDaf ::: listNoTheme

    futureListThemes.map(listThemes => SearchResult(Some("category"), Some("{" + mergeAggr(listThemes.filterNot(elem => elem._2 == 0)).mkString(",") + "}"), None))
  }

  private def mergeAggr(listAggr: List[(String, Int)]) = {
    listAggr.groupBy(_._1).toList.sortBy(_._1).map{case (k, v) => s""""$k": "${v.map(_._2).sum}""""}
  }

  private def themeQueryString(search: String): List[QueryDefinition] = {
    val mapTheme: Map[String, String] = Map(
      "agricoltura" -> "AGRI",
      "economia" -> "ECON",
      "educazione" -> "EDUC",
      "energia" -> "ENER",
      "ambiente" -> "ENVI",
      "governo" -> "GOVE",
      "sanita" -> "HEAL",
      "internazionale" -> "INTR",
      "giustizia" -> "JUST",
      "regione" -> "REGI",
      "societa" -> "SOCI",
      "tecnologia" -> "TECH",
      "trasporto" -> "TRAN"
    )

    val searchTheme = search.split(" ").map(s =>
      mapTheme.getOrElse(s, "")
    ).toList.filterNot(s => s.equals(""))

    if(searchTheme.nonEmpty)
      searchTheme.flatMap(s => List(termQuery("dcatapit.theme", s), matchQuery("theme", s)))
    else
      List()
  }

  private def createFilterStatus(status: Option[Seq[String]]): List[QueryDefinition] = {
    status match {
      case Some(List()) => List()
      case Some(x) => {
        val statusDataset = x.map {
          case "2" => "0"
          case _ => "1"
        }
        List(should(termsQuery("dcatapit.privatex", statusDataset), termsQuery("status", status.get),
          termsQuery("published", status.get), termQuery("private", statusDataset)))
      }
      case _ => List()
    }
  }

  private def createFilterOrg(inputOrgFilter: Option[Seq[String]]) = {
    val datasetOrg = "dcatapit.owner_org"
    val dashNstorOrg = "org"
    val opendataOrg = "organization.name.keyword"

    inputOrgFilter match {
      case Some(Seq()) => List()
      case Some(org) => {
        List(
          should(
            org.flatMap(nameOrg =>
              List(
                matchQuery(datasetOrg, nameOrg).operator("AND"),
                matchQuery(dashNstorOrg, nameOrg).operator("AND"),
                matchQuery(opendataOrg, nameOrg).operator("AND")
              )
            )
          )
        )
      }
      case _ => List()
    }
  }

  private def createThemeFilter(theme: Option[Seq[String]]): List[BoolQueryDefinition] = {
    val result: List[BoolQueryDefinition] = theme match {
      case Some(Seq()) => List()
      case Some(t) => t.map(s =>
        if(s.equals("no_category")){
          should(
            must(matchQuery("_type", "ext_opendata"), boolQuery().not(existsQuery("theme.keyword"))),
            matchQuery("theme.keyword", ""),
            matchQuery("theme.keyword", "[]")
          )
        }
        else should(matchQuery("theme", s).operator(Operator.AND), matchQuery("dcatapit.theme.keyword", s).operator(Operator.AND))
      ).toList
      case _ => List()
    }
    List(should(result))
  }

  private def filterDate(futureTupleSearchResults: Future[List[(String, SearchResult)]], timestamp: String): Future[List[(String, SearchResult)]] = {
    val start = timestamp.split(" ")(0)
    val end = timestamp.split(" ")(1)
    futureTupleSearchResults.map(tupleSearchResults => tupleSearchResults.filter(result => (result._1 >= start) && (result._1 <= end)))
  }

  private def themeFormatter(json: JsValue): String = {
    val themeJson = (json \ "theme").getOrElse(Json.parse("no_category")).toString()
    val themeString = if(themeJson.contains("theme")){
      val listThemes: Array[String] = themeJson.toString.replace("\"[", "").replace("]\"", "").replace("},", "} # ").split(" # ")
      listThemes.map(elem => (Json.parse(elem.trim.replace("\\", "")) \ "theme").get).toList.map(s => s.toString().replace("\"", "")).mkString(",")
    } else themeJson
    if(themeString.equals("null") || themeString.equals("\"[]\"") || themeString.equals("\"\"")) "no_category"
    else themeString
  }

  private def createSearchResult(source: SearchHit, search: Boolean): SearchResult = {
    val sourceResponse: String = source.`type` match {
      case "ext_opendata" =>  {
        val theme = themeFormatter(Json.parse(source.sourceAsString))
        val k = source.sourceAsMap.updated("theme", theme)
        val res = k.map(elem =>
          elem._2 match {
            case map: Map[String, AnyRef] =>
              val value = map
                .map(child => s""""${child._1}":"${Try(child._2.toString.replaceAll("\"", "")).getOrElse("")}"""").mkString(",")
              s""""${elem._1}":{$value}"""
            case _ => s""""${elem._1}":"${Try(elem._2.toString.replaceAll("\"", "")).getOrElse("")}""""
          }
        ).mkString(",").replaceAll("\n", "").replaceAll("\r", "").replaceAll("\t", "")
        "{" + res + "}"
      }
      case _ => source.sourceAsString
    }
    val highlight = source.highlight match {
      case mapHighlight: Map[String, Seq[String]] => {
        Some(
          "{" +
            mapHighlight.map(x =>
              x._1 match {
                case "widgets" => s""""${x._1}": "${(Json.parse(source.sourceAsString) \ "widgets").get.toString()}""""
                case _ => s""""${x._1}": "${x._2.mkString("...").replace("\"", "\\\"").replace("\n", "")}""""
              }
            ).mkString(",")
            + "}"
        )
      }
      case _ => Some("{}")
    }

    SearchResult(Some(source.`type`), Some(sourceResponse), highlight)
  }

  private def wrapResponse(query: Future[SearchResponse], order: String, search: Boolean, timestamp: Option[String]): Future[List[SearchResult]] = {
    val futureTupleSearchResult: Future[List[(String, SearchResult)]] = query.map(q =>
      q.hits.hits.map(source =>
        createSearchResult(source, search)
      ).toList.map(searchResult => (extractDate(searchResult.`type`.get, searchResult.source.get), searchResult))
    )

    val res = timestamp match {
      case Some("") => futureTupleSearchResult
      case Some(x) => filterDate(futureTupleSearchResult, x)
      case None => futureTupleSearchResult
    }

    val result = order match {
      case "score" => res
      case "asc" => res.map(r => r.sortWith(_._1 < _._1))
      case _ => res.map(r => r.sortWith(_._1 > _._1))
    }

    val toReturn = result.map(r => r.map(elem => elem._2))
    toReturn onComplete (r => Logger.logger.debug(s"find ${r.getOrElse(List()).size}"))
    toReturn
  }

  private def extractDate(typeDate: String, source: String): String = {
    val result: JsLookupResult = typeDate match {
      case "catalog_test" => Json.parse(source) \ "dcatapit" \ "modified"
      case "ext_opendata" => Json.parse(source.replace("\\", "\\\"")) \ "modified"
      case _ => Json.parse(source) \ "timestamp"
    }
    parseDate(result.getOrElse(Json.parse("23/07/2017")).toString().replaceAll("\"", ""))
  }

  private def parseDate(date: String): String = {
    if(date.contains("/")){
      val day = date.split("/")(0)
      val month = date.split("/")(1)
      val year = date.split("/")(2)
      s"""$year-$month-$day"""
    }
    else if(date.contains("T")){
      date.substring(0, date.indexOf("T"))
    }
    else date
  }

  private def wrapAggrResp(listCount: List[AnyVal]): (String, Int) = {
    if(listCount.length == 2) (listCount(0).toString, listCount(1).asInstanceOf[Int])
    else {
      val key = listCount(1).toString
      val count = listCount(2).asInstanceOf[Int]
      (key, count)
    }
  }

  private def listFields(obj: String): List[String] = {
    import scala.reflect.runtime.universe
    import scala.reflect.runtime.universe._

    val objType: universe.Type = obj match {
      case "Dashboard" => typeOf[Dashboard]
      case "User-Story" => typeOf[UserStory]
    }
    val fields = objType.members.collect{
      case m: MethodSymbol if m.isCaseAccessor => m.name.toString
    }.toList
    fields
  }

  def searchLast(username: String, groups: List[String]) = {
    Logger.logger.debug(s"elasticsearchUrl: $elasticsearchUrl elasticsearchPort: $elasticsearchPort")

    val client = HttpClient(ElasticsearchClientUri(elasticsearchUrl, elasticsearchPort))
    val fieldsDataset = List("dcatapit.name", "dcatapit.title", "dcatapit.modified", "dcatapit.privatex",
      "dcatapit.theme", "dcatapit.owner_org", "dcatapit.author")
    val fieldsOpenData = List("name", "title", "notes", "organization.name", "theme", "modified")
    val fieldsStories = listFields("User-Story")
    val fieldsDash = listFields("Dashboard")

    val queryDataset = queryHome("catalog_test", username, groups)
    val queryOpendata = queryHome("ext_opendata", "", Nil)
    val queryStories = queryHome("stories", username, groups)
      .sortByFieldDesc("timestamp.keyword")
      .size(3)
    val queryDash = queryHome("dashboards", username, groups)
      .sortByFieldDesc("timestamp.keyword")
      .size(3)
    val queryAggr = queryHome("", username, groups)

    val resultFutureDataset: Future[SearchResponse] = executeQueryHome(client, queryDataset, fieldsDataset)
    val resultFutureOpendata: Future[SearchResponse] = executeQueryHome(client, queryOpendata, fieldsOpenData)
    val resultFutureDash: Future[SearchResponse] = executeQueryHome(client, queryDash, fieldsDash)
    val resultFutureStories: Future[SearchResponse] = executeQueryHome(client, queryStories, fieldsStories)
    val resultFutureAggr: Future[SearchResponse] = executeAggrQueryHome(client, queryAggr)

    val result = for{
      resFutureDataset <- extractAllLastDataset(wrapResponseHome(resultFutureDataset), wrapResponseHome(resultFutureOpendata))
      resFutureStories <- wrapResponseHome(resultFutureStories)
      resFutureDash <- wrapResponseHome(resultFutureDash)
      resFutureAggr <- wrapAggrResponseHome(resultFutureAggr)
    } yield resFutureDataset ::: resFutureStories ::: resFutureDash ::: resFutureAggr

    result onComplete(r => Logger.logger.debug(s"find ${r.getOrElse(List()).size} results"))

    result
  }

  def searchLastPublic(org: Option[String]) = {
    Logger.logger.debug(s"elasticsearchUrl: $elasticsearchUrl elasticsearchPort: $elasticsearchPort")
    Logger.logger.debug(s"organization: $org")

    val client = HttpClient(ElasticsearchClientUri(elasticsearchUrl, elasticsearchPort))
    val fieldsDataset = List("dcatapit.name", "dcatapit.title", "dcatapit.modified", "dcatapit.privatex",
      "dcatapit.theme", "dcatapit.owner_org", "dcatapit.author")
    val fieldsOpenData = List("name", "title", "notes", "organization.name", "theme", "modified")
    val fieldsStories = listFields("User-Story")
    val fieldsDash = listFields("Dashboard")

    val queryDataset = queryHomePublic("catalog_test", org)
    val queryOpendata = queryHomePublic("ext_opendata", org)
    val queryStories = queryHomePublic("stories", org)
      .sortByFieldDesc("timestamp.keyword")
      .size(3)
    val queryAggr = queryHomePublic("", org)

    val resultFutureDataset: Future[SearchResponse] = executeQueryHome(client, queryDataset, fieldsDataset)
    val resultFutureOpendata: Future[SearchResponse] = executeQueryHome(client, queryOpendata, fieldsOpenData)
    val resultFutureStories: Future[SearchResponse] = executeQueryHome(client, queryStories, fieldsStories)
    val resultFutureAggr: Future[SearchResponse] = executeAggrQueryHome(client, queryAggr)

    val result: Future[List[SearchResult]] = for{
      resFutureDataset <- extractAllLastDataset(wrapResponseHome(resultFutureDataset), wrapResponseHome(resultFutureOpendata))
      resFutureStories <- wrapResponseHome(resultFutureStories)
      resFutureAggr <- wrapAggrResponseHome(resultFutureAggr)
    }yield resFutureDataset ::: resFutureStories ::: resFutureAggr

    result onComplete (r => Logger.logger.debug(s"find ${r.getOrElse(List()).size} results"))
    result
  }

  private def queryHome(typeElastic: String, username: String, groups: List[String]) = {
    search("ckan").types(typeElastic).query(
      boolQuery()
        .must(
          should(
            must(termQuery("dcatapit.privatex", "1"), matchQuery("dcatapit.owner_org", groups.mkString(" "))),
            termQuery("dcatapit.privatex", "0"),
            must(termQuery("status", "0"), termQuery("user", username)),
            must(termQuery("published", "0"), termQuery("user", username)),
            must(termQuery("status", "1"), matchQuery("org", groups.mkString(" "))),
            must(termQuery("published", "1"), matchQuery("org", groups.mkString(" "))),
            termQuery("status", "2"),
            termQuery("published", "2"),
            termQuery("private", "false")
          )
        )
    )
  }

  private def queryHomePublic(typeElastic: String, org: Option[String]) = {
    def createTermQueryPublicHomeOrg(org: String) = {
      should(
        must(termQuery("dcatapit.owner_org", org), termQuery("dcatapit.privatex", false)),
        must(termQuery("org", org), termQuery("published", "2")),
        must(termQuery("organization.name", org), termQuery("private", false))
      )
    }
    def createTermQueryPublicHomeNoOrg = {
      should(
        termQuery("dcatapit.privatex", false),
        termQuery("status", "2"),
        termQuery("published", "2"),
        termQuery("private", false)
      )
    }

    search("ckan").types(typeElastic).query(
      boolQuery()
        .must(
          org match {
            case Some(organization) => createTermQueryPublicHomeOrg(organization)
            case _ => createTermQueryPublicHomeNoOrg
          }
        )
    )
  }

  private def extractLastDataset(seqDatasetDaf: Seq[SearchResult]) = {
    val listDateDatasetDaf: List[(String, SearchResult)] = seqDatasetDaf.map(
      d => (extractDate(d.`type`.get, d.source.get), d)
    ).toList
    listDateDatasetDaf.sortWith(_._1 > _._1).take(3)
  }

  private def extractAllLastDataset(seqDatasetDaf: Future[Seq[SearchResult]], seqDatasetOpen: Future[List[SearchResult]]) = {
    val listDaf: Future[List[(String, SearchResult)]] = seqDatasetDaf map (daf => extractLastDataset(daf))
    val listOpen: Future[List[(String, SearchResult)]] = seqDatasetOpen map (open => extractLastDataset(open))
    val listDataset: Future[List[(String, SearchResult)]] = mergeListFuture(listDaf, listOpen)
    val result: Future[List[SearchResult]] = listDataset map (l => l.sortBy(_._1).take(3).map(elem => elem._2))
    result
  }

  private def mergeListFuture(listDaf: Future[List[(String, SearchResult)]], listOpen: Future[List[(String, SearchResult)]]): Future[List[(String, SearchResult)]] = {
    for{
      daf <- listDaf
      open <- listOpen
    } yield daf ::: open
  }

  private def executeAggrQueryHome(client: HttpClient, query: SearchDefinition): Future[SearchResponse] = {
    client.execute(query.aggregations(termsAgg("type", "_type")))
  }

  private def executeQueryHome(client: HttpClient, query: SearchDefinition, field: List[String]): Future[SearchResponse] = {
    client.execute(query.sourceInclude(field))
  }

  private def wrapAggrResponseHome(query: Future[SearchResponse]) = {
   query map(q => q.aggregations.map{ elem =>
      val name = elem._1
      val valueMap = elem._2.asInstanceOf[Map[String, Any]]("buckets").asInstanceOf[List[Map[String, AnyVal]]]
        .map(bucket => wrapAggrResp(bucket.values.toList)).map(v => v._1 -> v._2).toMap
      name -> valueMap
        }.map(elem =>SearchResult(Some(elem._1),Some("{" + elem._2.map(v => s""""${v._1}":"${v._2}"""").mkString(",") + "}"), None)).toList
      )
  }

  private def wrapResponseHome(query: Future[SearchResponse]): Future[List[SearchResult]] = {
    query map (q => q.hits.hits.map(source =>
      createSearchResult(source, false)
    ).toList)
  }

}
