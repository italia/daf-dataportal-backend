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
import ftd_api.yaml.{Catalog, Dashboard, DashboardIframes, Filters, SearchResult, Success, UserStory}
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
import com.sksamuel.elastic4s.searches.queries.matches.MatchQueryDefinition
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
         println("MERDA")
         println(internalUrl)
         println(resp.body)
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
            case e: Exception => e.printStackTrace(); println(x);println("ERROR"); DashboardIframes(None, None, None, None, None, None)
          }
        }
      })

      iframes.filter {
        case DashboardIframes(Some(_), Some(_), Some(_), Some(_), Some(_), Some(_)) => true
        case _ => false
      }
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



     val tdMetabase  :Future[Seq[DashboardIframes]] = requestTdMetabase.map { response =>
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


    val services  = List(test, superset, grafana, tdMetabase)

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

  def searchText(filters: Filters, username: String, groups: List[String]): Seq[SearchResult] = {
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
      fieldDatasetDataFieldName, fieldDatasetDcatTheme, "dcatapit.privatex", "dcatapit.modified", "dcatapit.owner_org", "operational.ext_opendata.name")
    val fieldsOpenData = List("name", "title", "notes", "organization.name", "theme", "modified", "name")
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

    def textStringQuery = {
      filters.text match {
        case Some("") => List()
        case Some(searchString) => {
          searchString.split(" ").flatMap(s => listFieldSearch.filterNot(f => f.equals(fieldDatasetDcatTheme) || f.equals("theme"))
            .map(field => matchQuery(field, s))).toList ::: themeQueryString(searchString)
        }
        case None => List()
      }
    }

    def queryElasticsearch = {
      search(index).types(searchType).query(
        boolQuery()
          .must(
            should(
              textStringQuery
            ),
            must(
                createFilterOrg(filters.org) ::: createFilterStatus(filters.status) :::
              creteThemeFilter(filters.theme, "theme") ::: creteThemeFilter(filters.theme, fieldDatasetDcatTheme)
            ),
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
      ).limit(5000)
    }

    val query = queryElasticsearch

    val res = client.execute{
      query
        .sourceInclude(fieldToReturn)
        .highlighting(listFieldSearch
          .filterNot(s => s.equals("org") || s.equals("dcatapit.owner_org"))
          .map(x => highlight(x).preTag("<span style='background-color:#0BD9D3'>").postTag("</span>")
            .fragmentSize(70))
        )
    }.await

    client.close()

    wrapResponse(res, order, searchText, filters.date)
  }

  private def themeQueryString(search: String): List[MatchQueryDefinition] = {
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
      if(mapTheme.contains(s.toLowerCase)) mapTheme(s.toLowerCase)
      else ""
    ).toList.filterNot(s => s.equals(""))

    if(searchTheme.nonEmpty)
      searchTheme.flatMap(s => List(matchQuery("dcatapit.theme", s), matchQuery("theme", s)))
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
    val opendataOrg = "organization.name"

    inputOrgFilter match {
      case Some(Seq()) => List()
      case Some(org) => List(should(matchQuery(datasetOrg, org.mkString(" ")),
        matchQuery(dashNstorOrg, org.mkString(" "))),
        matchQuery(opendataOrg, org.mkString(" ")))
      case _ => List()
    }
  }

  private def creteThemeFilter(theme: Option[Seq[String]], fieldDatasetDcatTheme: String): List[BoolQueryDefinition] = {
    theme match {
      case Some(Seq()) => List()
      case Some(t) => List(should(matchQuery(fieldDatasetDcatTheme, t.mkString(" "))))
      case _ => List()
    }
  }

  private def filterDate(tupleDateSearchResult: List[(String, SearchResult)], timestamp: String): List[(String, SearchResult)] = {
    val start = timestamp.split(" ")(0)
    val end = timestamp.split(" ")(1)
    tupleDateSearchResult.filter(result => (result._1 >= start) && (result._1 <= end))
  }

  private def themeFormatter(json: JsValue): String = {
    val themeJson = (json \ "theme").getOrElse(Json.parse("y")).toString()
    val themeString = if(themeJson.contains("theme")){
      (Json.parse(themeJson) \\ "theme").mkString(",")
    }
    else themeJson
    if(themeString.equals("null") || themeString.equals("\"[]\"")) ""
    else themeString
  }

  private def createSearchResult(source: SearchHit, search: Boolean): SearchResult = {
    val sourceResponse: String = source.`type` match {
      case "ext_opendata" =>  {
        val theme = themeFormatter(Json.parse(source.sourceAsString))
        val k = source.sourceAsMap.updated("theme", theme)
        val res = k.map(elem =>
          if(elem._2.isInstanceOf[Map[String, AnyRef]]) {
            val value = (elem._2.asInstanceOf[Map[String, AnyRef]])
              .map(child => s""""${child._1}":"${Try(child._2.toString.replaceAll("\"", "")).getOrElse("")}"""").mkString(",")
            s""""${elem._1}":{$value}"""
          }else s""""${elem._1}":"${Try(elem._2.toString.replaceAll("\"", "")).getOrElse("")}""""
        ).mkString(",").replace("\n", "").replaceAll("\r", "").replaceAll("\t", "")
        "{" + res + "}"
      }
      case _ => source.sourceAsString
    }
    val highlight = if(search) {
      source.highlight match {
        case mapHighlight: Map[String, Seq[String]] => {
          Some(
            "{" +
              mapHighlight.map(x =>
                x._1 match {
                  case "widgets" => s""""${x._1}": "${(Json.parse(source.sourceAsString) \ "widgets").get.toString()}""""
                  case _ => s""""${x._1}": "${x._2.mkString("...")}"""
                }
              ).mkString(",")
              + "}"
          )
        }
        case _ => Some("{}")
      }
    } else Some("{}")
    SearchResult(Some(source.`type`), Some(sourceResponse), highlight)
  }

  private def removeDuplicate(seqSearchResult: Seq[SearchResult]): Seq[SearchResult] = {
    val listNameExtOpendata: List[String] = seqSearchResult
      .filter(elem => elem.`type`.get.equals("catalog_test"))
      .map(elem => {
        Try((Json.parse(elem.source.get) \ "operational" \ "ext_opendata" \ "name").get.toString()).getOrElse("")
      }).toList.filterNot(name => name.equals(""))

    val result = seqSearchResult.filterNot(
      elem =>
        elem.`type`.get.equals("ext_opendata") && listNameExtOpendata.contains((Json.parse(elem.source.get) \ "name").toString)
    )
    result
  }

  private def wrapResponse(query: SearchResponse, order: String, search: Boolean, timestamp: Option[String]): Seq[SearchResult] = {
    val seqSearchResult: Seq[SearchResult] = query.hits.hits.map(source =>
      createSearchResult(source, search)
    ).toSeq

    val seqResp = removeDuplicate(seqSearchResult)

    val tupleDateSearchResult: List[(String, SearchResult)] = seqResp.map(x =>
      (extractDate(x.`type`.get, x.source.get), x)
    ).toList

    val res = timestamp match {
      case Some("") => tupleDateSearchResult
      case Some(x) => filterDate(tupleDateSearchResult, x)
      case None => tupleDateSearchResult
    }

    val result = order match {
      case "score" => res
      case "asc" => res.sortWith(_._1 < _._1)
      case _ => res.sortWith(_._1 > _._1)
    }

    result.map(elem => elem._2) ++ aggregationSearch(res.map(x => x._2))
  }

  private def extractDate(typeDate: String, source: String): String = {
    val json = Json.parse(source)
    val result: JsLookupResult = typeDate match {
      case "catalog_test" => json \ "dcatapit" \ "modified"
      case "ext_opendata" => json \ "modified"
      case _ => json \ "timestamp"
    }
    parseDate(result.getOrElse(Json.parse("2017-07-23")).toString().replaceAll("\"", ""))
  }

  private def aggregationSearch(searchResult: List[SearchResult]): Seq[SearchResult] = {
    val searchResultType = createTypeAggr(searchResult.map(x => x.`type`.get))
    val searchResultStatus = createStatusAggr(searchResult)
    val searchResultOrg = createOrgAggr(searchResult)
    val searchResultTheme = createThemeAggr(searchResult)

    Seq(searchResultType, searchResultStatus, searchResultOrg, searchResultTheme)
  }

  private def createThemeAggr(searchResult: List[SearchResult]): SearchResult = {
    val aggrDataset = themeDataset(searchResult.filter(x => x.`type`.get.equals("catalog_test")), false)
    val aggrOpenData = themeDataset(searchResult.filter(x => x.`type`.get.equals("ext_opendata")), true)

    val listTheme = aggrDataset.toList ++ aggrOpenData.toList
    val merged = listTheme.groupBy(_._1).map{case (k, v) => k -> v.map(_._2).sum}
    val aggrString = merged.toList.sortWith(_._1 < _._1).toMap.map(x => s"""${x._1}:"${x._2}"""").mkString(",")
    SearchResult(Some("category"), Some("{" + aggrString + "}"), None)
  }

  private def themeDataset(searchResult: List[SearchResult], isOpen: Boolean): Map[String, Int] = {
    val themeValue = searchResult.map(x =>
      if(isOpen)(Json.parse(x.source.get) \ "theme").get.toString()
      else (Json.parse(x.source.get) \ "dcatapit" \ "theme").get.toString()
    )
    themeValue.map(theme => theme -> themeValue.count(v => v.equals(theme))).sortWith(_._1 < _._1).toMap
  }

  private def createOrgAggr(searchResult: List[SearchResult]): SearchResult = {
    val aggrStories = orgStories(searchResult.filter(x => x.`type`.get.equals("stories")))
    val aggrDataset = orgDataset(searchResult.filter(x => x.`type`.get.equals("catalog_test")), false)
    val aggrDatasetOpen = orgDataset(searchResult.filter(x => x.`type`.get.equals("ext_opendata")), true)
    val aggreDashboards = orgDashboards(searchResult.filter(x => x.`type`.get.equals("dashboards")))

    val listOrg = aggrStories.toList ++ aggrDataset.toList ++ aggreDashboards.toList ++ aggrDatasetOpen.toList
    val merged = listOrg.groupBy(_._1).map{case (k, v) => k -> v.map(_._2).sum}
    val sourceString = merged.toList.sortWith(_._1 < _._1).toMap.map(x => s"""${x._1}:"${x._2}"""").mkString(",")
    SearchResult(Some("organization"), Some("{" + sourceString + "}"), None)
  }

  private def orgStories(searchResult: List[SearchResult]): Map[String, Int] = {
    val orgValue = searchResult.map(x =>
      (Json.parse(x.source.get) \ "org").get.toString()
    )
    orgValue.map(org => org -> orgValue.count(s => s.equals(org))).toMap
  }

  private def orgDashboards(searchResult: List[SearchResult]): Map[String, Int] = {
    val orgValue = searchResult.map(x =>
      (Json.parse(x.source.get) \ "org").get.toString()
    )
    orgValue.map(org => org -> orgValue.count(s => s.equals(org))).toMap
  }

  private def orgDataset(searchResult: List[SearchResult], isOpen: Boolean): Map[String, Int] = {
    val orgValue = searchResult.map(x =>
      if(isOpen) (Json.parse(x.source.get) \ "organization" \ "name").get.toString()
      else (Json.parse(x.source.get) \ "dcatapit" \ "owner_org").get.toString()
    )
    orgValue.map(org => org -> orgValue.count(s => s.equals(org))).toMap
  }

  private def createTypeAggr(typeString: List[String]): SearchResult = {
    val countStories = typeString.count(t => t.equals("stories"))
    val countDash = typeString.count(t => t.equals("dashboards"))
    val countCatalogTest = typeString.count(t => t.equals("catalog_test"))
    val countOpenData = typeString.count(t => t.equals("ext_opendata"))

    val sourceString = s"""{"catalog_test":"$countCatalogTest","dashboards":"$countDash","stories":"$countStories","ext_opendata":"$countOpenData"}"""
    SearchResult(Some("type"), Some(sourceString), None)
  }

  private def createStatusAggr(searchResult: List[SearchResult]): SearchResult = {
    val aggrStories = statusStories(searchResult.filter(s => s.`type`.get.equals("stories")))
    val aggrDataset = statusDataset(searchResult.filter(s => s.`type`.get.equals("catalog_test")))
    val aggreDashboards = statusDashboards(searchResult.filter(s => s.`type`.get.equals("dashboards")))
    val aggreOpenData = searchResult.count(s => s.`type`.get.equals("ext_opendata"))

    val coutn0 = aggrStories("0") +  aggrDataset("0") + aggreDashboards("0")
    val coutn1 = aggrStories("1") +  aggrDataset("1") + aggreDashboards("1")
    val coutn2 = aggrStories("2") +  aggrDataset("2") + aggreDashboards("2") + aggreOpenData

    SearchResult(Some("status"), Some(s"""{"0":"$coutn0","1":"$coutn1", "2":"$coutn2"}"""), None)
  }

  private def statusStories(searchResult: List[SearchResult]): Map[String, Int] = {
    val statusValue = searchResult.map(x =>
      (Json.parse(x.source.get) \ "published").get.toString()
    )
    Map("0" -> statusValue.count(x => x.equals("0")), "1" -> statusValue.count(x => x.equals("1")), "2" -> statusValue.count(x => x.equals("2")))
  }

  private def statusDashboards(searchResult: List[SearchResult]): Map[String, Int] = {
    val statusValue = searchResult.map(x =>
      (Json.parse(x.source.get) \ "status").get.toString()
    )
    Map("0" -> statusValue.count(x => x.equals("0")), "1" -> statusValue.count(x => x.equals("1")), "2" -> statusValue.count(x => x.equals("2")))
  }

  private def statusDataset(searchResult: List[SearchResult]): Map[String, Int] = {
    val statusValue = searchResult.map(x =>
      (Json.parse(x.source.get) \ "dcatapit" \ "privatex").get.toString()
    )
    val countTrue = statusValue.count(x => x.equals("true"))
    val countFalse = statusValue.count(x => x.equals("false"))
    Map("0" -> countTrue, "1" -> countTrue, "2" -> countFalse)
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

  def searchLast(username: String, groups: List[String]): Seq[SearchResult] = {
    val client = HttpClient(ElasticsearchClientUri(elasticsearchUrl, elasticsearchPort))
    val fieldDataset = List("dcatapit.name", "dcatapit.title", "dcatapit.modified", "dcatapit.privatex",
      "dcatapit.theme", "dcatapit.owner_org", "dcatapit.author")
    val fieldStories = listFields("User-Story")
    val fieldDash = listFields("Dashboard")

    val queryDataset = queryHome("catalog_test", username, groups)
    val queryStories = queryHome("stories", username, groups)
      .sortByFieldDesc("timestamp.keyword")
      .size(3)
    val queryDash = queryHome("dashboards", username, groups)
      .sortByFieldDesc("timestamp.keyword")
      .size(3)
    val queryAggr = queryHome("", username, groups)

    val resultDataset = executeQueryHome(client, queryDataset, fieldDataset)
    val resultDash = executeQueryHome(client, queryDash, fieldDash)
    val resultStories = executeQueryHome(client, queryStories, fieldStories)
    val resAggr = executeAggrQueryHome(client, queryAggr)

    client.close()

    val responseDataset = extractLastDataset(wrapResponseHome(resultDataset))
    responseDataset ++ wrapResponseHome(resultDash) ++ wrapResponseHome(resultStories) ++ wrapAggrResponseHome(resAggr)
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
            termQuery("published", "2")
          )
        )
    )
  }

  private def extractLastDataset(seqDataset: Seq[SearchResult]) = {
    val listDateDataset: List[(String, SearchResult)] = seqDataset.map(d => (extractDate(d.`type`.get, d.source.get), d)).toList
    listDateDataset.sortWith(_._1 > _._1).take(3).map(x => x._2)
  }

  private def executeAggrQueryHome(client: HttpClient, query: SearchDefinition): SearchResponse = {
    client.execute(query.aggregations(termsAgg("type", "_type"))).await
  }

  private def executeQueryHome(client: HttpClient, query: SearchDefinition, field: List[String]) = {
    client.execute(query.sourceInclude(field)).await
  }

  private def wrapAggrResponseHome(query: SearchResponse) = {
    val mapAggregation: Map[String, Map[String, Int]] = query.aggregations.map{ elem =>
      val name = elem._1
      val valueMap = elem._2.asInstanceOf[Map[String, Any]]("buckets").asInstanceOf[List[Map[String, AnyVal]]]
        .map(elem =>
          wrapAggrResp(elem.values.toList)
        )
        .map(v => v._1 -> v._2).toMap
      name -> valueMap
    }
    mapAggregation.map(elem => SearchResult(Some(elem._1), Some("{" + elem._2.map(v => s""""${v._1}":"${v._2}"""").mkString(",") + "}"), None))
  }

  private def wrapResponseHome(query: SearchResponse): Seq[SearchResult] = {
    query.hits.hits.map(source =>
      SearchResult(Some(source.`type`), Some(source.sourceAsString), None)
    ).toSeq
  }

}
