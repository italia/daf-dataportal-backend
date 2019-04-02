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
import ftd_api.yaml.{Catalog, Dashboard, DashboardIframes, DataApp, Filters, SearchResult, Success, UserStory, Error, Organization, SupersetTable}
import play.api.libs.json._
//import play.api.libs.ws.ahc.AhcWSClient
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
import play.api.libs.ws.WSClient

/**
  * Created by ale on 14/04/17.
  */

class DashboardRepositoryProd extends DashboardRepository {

  import ftd_api.yaml.BodyReads._
  import scala.concurrent.ExecutionContext.Implicits._

//  implicit val system = ActorSystem()
//  implicit val materializer = ActorMaterializer()

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
  private val elasticsearchMaxResult = ConfigReader.getElastcsearchMaxResult

  private val KAFKAPROXY = ConfigReader.getKafkaProxy

  val conf = Configuration.load(Environment.simple())
//  val URLMETABASE = conf.getString("metabase.url").get
//  val metauser = conf.getString("metabase.user").get
//  val metapass = conf.getString("metabase.pass").get

  private val OPEN_DATA_GROUP = "open_data_group"



//  private def  metabaseTableInfo(iframes :Seq[DashboardIframes], wsClient: WSClient): Future[Seq[DashboardIframes]] = {
//
////    val wsClient = AhcWSClient()
//    val futureFrames: Seq[Future[Try[DashboardIframes]]] = iframes.map { iframe =>
//      val tableId = iframe.table.getOrElse("0")
//      val internalUrl = localUrl + s"/metabase/table/$tableId"
//      // Missing metabase session not working
//      val futureTry : Future[Try[DashboardIframes]]= wsClient.url(internalUrl).get()
//       .map { resp =>
//         val tableName = (resp.json \ "name").as[String]
//         iframe.copy(table = Some(tableName))
//       }.map { x:DashboardIframes => scala.util.Success(x)}
//        .recover { case t: Throwable => Failure(t) }
//
//      futureTry
//    }
//
//    val seq: Future[Seq[Try[DashboardIframes]]] = Future.sequence(futureFrames)
//    val dashboardIframes: Future[Seq[DashboardIframes]] = seq.map(
//             _.filter(_.isSuccess)
//              .map(_.get))
//    dashboardIframes
//    //val d: Future[Any] = seq.collect{ case scala.util.Success(x) => x}
//
//  }

  private def extractToInt(indentifier :String): Int = {
    try {
      indentifier.split("_")(1).toInt
    } catch {
      case e: Exception => 0
    }
  }

  def getSupersetTableByTableNameIdAndOrgs(user: String, tableName: String, orgs: Seq[Organization], ws: WSClient): Future[Either[Error, Seq[SupersetTable]]] = {
    def callSupersetDatabaseId(user: String, org: String): Future[Option[Int]] = {
      ws.url(localUrl + s"/superset/database/$user/${org}-db").get()
        .map{ res =>
          val id = res.json \\ "id"
          if(id.isEmpty) None
          else Logger.logger.debug(s"$user: dabaseId ${id.head} for $org"); Some(id.head.as[Int])
        }
    }

    def callSupersetTableName(user: String, org: String, tableName: String, databaseId: Int): Future[Option[Int]] = {
      ws.url(localUrl + s"/superset/table_by_id_name/$user/$tableName/$databaseId").get()
        .map { res =>
          val tableId = res.json \\ "id"
          if(tableId.isEmpty) None
          else Logger.logger.debug(s"$user: dabaseId ${tableId.head} for $org"); Some(tableId.head.as[Int])
        }
    }

    val res = orgs.map{ org =>
      callSupersetDatabaseId(user, org.name)
      .flatMap{ case Some(x) => callSupersetTableName(user, org.name, tableName, x) }
        .map{ case Some(x) => SupersetTable(x, org.name) }
    }

    val seqSupersetTable = Future.traverse(res)(s => s)
    seqSupersetTable.map(seq =>
      if(seq.isEmpty) Left(Error(Some(404), Some("not found"), None))
      else {Logger.logger.debug(s"$user found ${seq.size}"); Right(seq)}
    )
  }

  def save(upFile: File, tableName: String, fileType: String, wsClient: WSClient): Success = {
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

  def iframesByOrg(user: String,org: String, wsClient: WSClient): Future[Seq[DashboardIframes]] = {

    val result = for {
      tables <- getSupersetTablesByOrg(org, wsClient)
      a <- iFramesByTables(user,tables, wsClient)
    } yield a

    result

  }

  private def getSupersetTablesByOrg(org: String, wsClient: WSClient): Future[Seq[String]] = {

//    val wsClient = AhcWSClient()
    //security-manager/v1/internal/superset/find_orgtables/default_org

    val internalUrl = securityManagerUrl + "/security-manager/v1/internal/superset/find_orgtables/"+org
    println("internalUrl: "+internalUrl)

    wsClient.url(internalUrl).get().map{ resp =>
      println("getSupersetTablesByOrg response: "+resp)
      (resp.json \ "tables").as[Seq[String]]
    }

  }

  private def iFramesByTables(user: String, tables:Seq[String], wsClient: WSClient): Future[Seq[DashboardIframes]]= {
    iframes(user, wsClient).map{ _.filter(dash => dash.table.nonEmpty && tables.contains(dash.table.get) ) }
  }

  def iframes(user: String, wsClient: WSClient): Future[Seq[DashboardIframes]] = {
    val openDataUser = ConfigReader.getSupersetOpenDataUser

    val supersetPublic = localUrl + "/superset/public_slice/" + user
//    val grafanaPublic = localUrl + "/grafana/snapshots/" + user
//    val tdmetabasePublic = localUrl + "/tdmetabase/public_card"


//    val request = wsClient.url(metabasePublic).get()
    // .andThen { case _ => wsClient.close() }
    // .andThen { case _ => system.terminate() }

    val requestIframes = wsClient.url(supersetPublic).get()
    //  .andThen { case _ => wsClient.close() }
    //  .andThen { case _ => system.terminate() }

//    val requestSnapshots = wsClient.url(grafanaPublic).get()
//
//    val requestTdMetabase = wsClient.url(tdmetabasePublic).get

    requestIframes.onComplete{ r => Logger.logger.debug(s"$user: response status superset ${r.get.status}, text ${r.get.statusText}")}


    val superset: Future[Seq[DashboardIframes]] = requestIframes.map { response =>
      Logger.logger.debug(s"iframes for $user response status from superset ${response.status}")
      val json = response.json.as[Seq[JsValue]]
      val iframes = json.map(x => {
        val slice_link = (x \ "slice_link").get.as[String]
        val vizType = (x \ "viz_type").get.as[String]
        val title = slice_link.slice(slice_link.indexOf(">") + 1, slice_link.lastIndexOf("</a>")).trim
        val src = slice_link.slice(slice_link.indexOf("\"") + 1, slice_link.lastIndexOf("\"")) + "&standalone=true"
        val url = if(user == ConfigReader.getSupersetOpenDataUser) ConfigReader.getSupersetOpenDataUrl + src else ConfigReader.getSupersetUrl + src
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
            case e: Exception => /*e.printStackTrace(); println(x);println("ERROR"); */ DashboardIframes(None, None, None, None, None, None)
          }
        }
      })

      val filtered = iframes.filter {
        case DashboardIframes(Some(_), Some(_), Some(_), Some(_), Some(_), Some(_)) => true
        case _ => false
      }

      val res = filtered.sortWith((a,b) => (extractToInt(a.identifier.get) > extractToInt(b.identifier.get)))

      res
    }

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

//    val services = if(user == openDataUser) List(superset, getMetabaseIframes(user, wsClient)) else List(superset)
//
//    def futureToFutureTry[T](f: Future[T]): Future[Try[T]] =
//      f.map(scala.util.Success(_)).recover { case t: Throwable => Failure(t) }
//
//    val withFailed: Seq[Future[Try[Seq[DashboardIframes]]]] = services.map(futureToFutureTry(_))

    // Can also be done more concisely (but less efficiently) as:
    // f.map(Success(_)).recover{ case t: Throwable => Failure( t ) }
    // NOTE: you might also want to move this into an enrichment class
    //def mapValue[T]( f: Future[Seq[T]] ): Future[Try[Seq[T]]] = {
    //  val prom = Promise[Try[Seq[T]]]()
    //  f onComplete prom.success
    //  prom.future
    //}

//    val servicesWithFailed = Future.sequence(withFailed)
//
//    val servicesSuccesses: Future[Seq[Try[Seq[DashboardIframes]]]] = servicesWithFailed.map(_.filter(_.isSuccess))
//
//    val results: Future[Seq[DashboardIframes]] = servicesSuccesses.map(_.flatMap(_.toOption).flatten)

//    results
    superset
  }

//  private def getMetabaseIframes(user: String, wsClient: WSClient): Future[Seq[DashboardIframes]] = {
//    val metabasePublic = localUrl + "/metabase/public_card/" + user
//
//    val request = wsClient.url(metabasePublic).get()
//
//    request.onComplete{ r => Logger.logger.debug(s"$user: response status metabase ${r.get.status}")}
//
//    val metabase: Future[Seq[DashboardIframes]] = request.map { response =>
//      Logger.logger.debug(s"iframes for $user response status from metabase ${response.status}")
//      val json = response.json.as[Seq[JsValue]]
//
//      Logger.debug(s"Metabase iframe response: $json")
//      json.filter( x => !(x \ "public_uuid").asOpt[String].isEmpty)
//        .map(x => {
//          val uuid = (x \ "public_uuid").get.as[String]
//          val title = (x \ "name").get.as[String]
//          //  val id = (x \ "id").get.as[String]
//          val tableId =   (x \ "table_id").get.as[Int]
//          val url = ConfigReader.getMetabaseUrl + "/public/question/" + uuid
//          DashboardIframes( Some("metabase_" + uuid), Some(url), None, Some("metabase"), Some(title), Some(tableId.toString))
//        })
//    }
//
//    val metabaseWithTables: Future[Seq[DashboardIframes]] = for {
//      iframes <- metabase
//      iframesWithTable <- metabaseTableInfo(iframes, wsClient)
//    } yield iframesWithTable
//
//    metabaseWithTables
//  }

  def dashboards(username: String, groups: List[String], status: Option[Int]): Seq[Dashboard] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    val results = coll.find(privateQueryToMongo("status", username, groups, status))
      .sort(MongoDBObject("timestamp" -> -1))
      .toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
    val dashboardsJsResult = json.validate[Seq[Dashboard]]
    dashboardsJsResult match {
      case s: JsSuccess[Seq[Dashboard]] => s.get
      case _: JsError => Seq()
    }
  }

  def dashboardsPublic(org: Option[String]): Seq[Dashboard] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    val results = coll.find(publicQueryToMongo("status", org))
      .sort(MongoDBObject("timestamp" -> -1))
      .toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
    val dashboardsJsResult = json.validate[Seq[Dashboard]]
    dashboardsJsResult match {
      case s: JsSuccess[Seq[Dashboard]] => s.get
      case e: JsError => Seq()
    }
  }

  def dashboardById(username: String, groups: List[String], id: String): Option[Dashboard] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    val result = coll.findOne(privateQueryById(id, "status", username, groups))
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(result)
    val json = Json.parse(jsonString)
    val dashboardJsResult: JsResult[Dashboard] = json.validate[Dashboard]
    dashboardJsResult match {
      case s: JsSuccess[Dashboard] => Some(s.get)
      case e: JsError => None
    }
  }

  def publicDashboardById(id: String): Option[Dashboard] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    val result = coll.findOne(publicQueryById(id, "status"))
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(result)
    val json = Json.parse(jsonString)
    val dashboardJsResult: JsResult[Dashboard] = json.validate[Dashboard]
    dashboardJsResult match {
      case s: JsSuccess[Dashboard] => Some(s.get)
      case e: JsError => None
    }
  }

  def saveDashboard(dashboard: Dashboard, user: String, shared: Option[Boolean], token: String, wsClient: WSClient): Success = {
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
        if(shared.getOrElse(false) && dashboard.status.get != 0)
          sendMessageToKafka(
            user,
            if(dashboard.status.get == 2) OPEN_DATA_GROUP else dashboard.org.get,
            token,
            s"Pubblicazione Dashboard",
            buildMessaggeToKafka(dashboard.status.get, "Dashboard", dashboard.title.getOrElse(""), dashboard.org.get),
            s"/private/dashboard/list/$x",
            wsClient,
            "generic")
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

  /**
    *
    * @param status new status
    * @param objString the name of the object to update {Storia || Dashboard}
    * @param title the title of the object
    * @param org the organization of the object
    * @return the message to send to kafka
    */
  private def buildMessaggeToKafka(status: Int, objString: String, title: String, org: String): String = {
    status match {
      case 1 => s"La $objString $title è stata pubblicata per l'organizzazione $org"
      case 2 => s"E' stata publicata la $objString $title"
    }
  }

  def deleteDashboard(dashboardId: String): Success = {
    val query = MongoDBObject("id" -> dashboardId)
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("dashboards")
    val removed = coll.remove(query)
    mongoClient.close()
    val response = Success(Some("Deleted"), Some("Deleted"))
    response
  }

  def stories(username: String, groups: List[String], status: Option[Int], page: Option[Int], limit: Option[Int]): Seq[UserStory] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("stories")
    val results = coll.find(privateQueryToMongo("published", username, groups, status))
      .sort(MongoDBObject("timestamp" -> -1))
      .skip(page.getOrElse(0))
      .limit(limit.getOrElse(100)).toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
    val storiesJsResult = json.validate[Seq[UserStory]]
    storiesJsResult match {
      case s: JsSuccess[Seq[UserStory]] => s.get
      case e: JsError => Seq()
    }
  }

  def storiesPublic(org: Option[String]): Seq[UserStory] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("stories")
    val results = coll.find(publicQueryToMongo("published", org))
      .sort(MongoDBObject("timestamp" -> -1))
      .toList
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
    val storiesJsResult = json.validate[Seq[UserStory]]
    storiesJsResult match {
      case s: JsSuccess[Seq[UserStory]] => s.get
      case e: JsError => Seq()
    }
  }

  private def publicQueryToMongo(nameFieldStatus: String, org: Option[String] = None) = {
    import mongodb.casbah.query.Imports._

    val orgQuery = org match {
      case Some(x) => mongodb.casbah.Imports.MongoDBObject("org" -> x)
      case None => mongodb.casbah.Imports.MongoDBObject()
    }
    val statusQuery = mongodb.casbah.Imports.MongoDBObject(nameFieldStatus -> 2)

    $and(orgQuery, statusQuery)
  }

  private def publicQueryById(id: String, nameFieldStatus: String) = {
    import mongodb.casbah.query.Imports._

    $and(mongodb.casbah.Imports.MongoDBObject("id" -> id), publicQueryToMongo(nameFieldStatus))
  }

  private def privateQueryById(id: String, nameFieldStatus: String, user: String, groups: List[String]) = {
    import mongodb.casbah.query.Imports._

    $and(mongodb.casbah.Imports.MongoDBObject("id" -> id), privateQueryToMongo(nameFieldStatus, user, groups))
  }

  private def privateQueryToMongo(nameFieldStatus: String, user: String, groups: List[String], status: Option[Int] = None) = {
    import mongodb.casbah.query.Imports._

    status match {
      case Some(0) => queryToMongoStatus0(user, nameFieldStatus)
      case Some(1) => queryToMongoStatus1(groups, nameFieldStatus)
      case Some(2) => queryToMongoStatus2(nameFieldStatus)
      case None =>  $or(queryToMongoStatus0(user, nameFieldStatus), queryToMongoStatus1(groups, nameFieldStatus), queryToMongoStatus2(nameFieldStatus))
    }
  }

  private def queryToMongoStatus0(user: String, nameFieldStatus: String) = {
    import mongodb.casbah.query.Imports._

    $and(mongodb.casbah.Imports.MongoDBObject("user" -> user), mongodb.casbah.Imports.MongoDBObject(nameFieldStatus -> 0))
  }

  private def queryToMongoStatus1(groups: List[String], nameFieldStatus: String) = {
    import mongodb.casbah.query.Imports._

    $and("org" $in groups, mongodb.casbah.Imports.MongoDBObject(nameFieldStatus -> 1))
  }

  private def queryToMongoStatus2(nameFieldStatus: String) = {
    mongodb.casbah.Imports.MongoDBObject(nameFieldStatus -> 2)
  }

  def storyById(username: String, groups: List[String], id: String): Option[UserStory] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("stories")
    val result = coll.findOne(privateQueryById(id, "published", username, groups))
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(result)
    val json = Json.parse(jsonString)
    val storyJsResult: JsResult[UserStory] = json.validate[UserStory]
    storyJsResult match {
      case s: JsSuccess[UserStory] => Some(s.get)
      case e: JsError => None
    }
  }

  def publicStoryById(id: String): Option[UserStory] = {
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("stories")
    val result = coll.findOne(publicQueryById(id, "published"))
    mongoClient.close
    val jsonString = com.mongodb.util.JSON.serialize(result)
    val json = Json.parse(jsonString)
    val storyJsResult: JsResult[UserStory] = json.validate[UserStory]
    storyJsResult match {
      case s: JsSuccess[UserStory] => Some(s.get)
      case _: JsError => None
    }
  }

  private def sendMessageToKafka(user: String, group: String, token: String, title: String, description: String, link: String, ws: WSClient, notificationtype: String) = {
    Logger.logger.debug(s"kafka proxy $KAFKAPROXY")
    val message = s"""{
                     |"records":[{"value":{"group":"$group","token":"$token","notificationtype": "$notificationtype", "info":{
                     |"title":"$title","description":"$description","link":"$link"}}}]}""".stripMargin

    val jsonBody = Json.parse(message)

    val responseWs = ws.url(KAFKAPROXY + "/topics/notification")
      .withHeaders(("Content-Type", "application/vnd.kafka.v2+json"))
      .post(jsonBody)

    responseWs.map{ res =>
      if( res.status == 200 ) {
        Logger.logger.debug(s"message sent to kakfa proxy for user $user")
      }
      else {
        Logger.logger.debug(s"error in sending message to kafka proxy for user $user: ${res.statusText}")
      }
    }
  }

  def saveStory(story: UserStory, user: String, shared: Option[Boolean], token: String, wsClient: WSClient): Success = {
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
        if(story.user.isEmpty) obj.put("user",  user)
        val query = MongoDBObject("id" -> x)
        saved = id.get
        operation = "updated"
        if(shared.getOrElse(false) && story.published.get != 0)
          sendMessageToKafka(
            user,
            if(story.published.get == 2) OPEN_DATA_GROUP else story.org.get,
            token,
            s"Pubblicazione Storia",
            buildMessaggeToKafka(story.published.get, "Storia", story.title.getOrElse(""), story.org.get),
            s"/private/userstory/list/$x",
            wsClient,
            "generic"
          )
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
    mongoClient.close()
    val response = Success(Some("Deleted"), Some("Deleted"))
    response
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

    Logger.logger.debug(s"found ${dataApp.size} results")

    dataApp
  }

}
