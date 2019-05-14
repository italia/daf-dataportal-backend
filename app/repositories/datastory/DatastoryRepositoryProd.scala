package repositories.datastory

import java.time.ZonedDateTime
import java.util.UUID

import com.mongodb
import com.mongodb.{DBObject, casbah}
import com.mongodb.casbah.Imports.{MongoCredential, MongoDBObject, ServerAddress}
import com.mongodb.casbah.query.Imports
import com.mongodb.casbah.{MongoClient, TypeImports, commons}
import play.api.Logger
import ftd_api.yaml.{Datastory, Error, Success}
import play.api.libs.json._
import play.api.libs.ws.WSClient
import utils.ConfigReader

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class DatastoryRepositoryProd extends DatastoryRepository {

  import ftd_api.yaml.BodyReads._
  import ftd_api.yaml.ResponseWrites._

  private val KAFKAPROXY = ConfigReader.getKafkaProxy
  private val catalogHost = ConfigReader.getCatalogManagerHost
  private val catalogNotificationPath = ConfigReader.getCatalogManagerNotificationPath

  private val CATALOG_URL = catalogHost + catalogNotificationPath

  private val OPEN_DATA_GROUP = ConfigReader.getOpenDataGroup

  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort
  private val userName = ConfigReader.userName
  private val source = ConfigReader.database
  private val password = ConfigReader.password

  private val server = new ServerAddress(mongoHost, mongoPort)
  private val credentials = MongoCredential.createCredential(userName, source, password.toCharArray)

  private val collectionName = "datastory"

  private val collection = MongoClient(server, List(credentials))(source)(collectionName)

  private val maxWidgetsNumber = 50
  private val maxTextSize = 10500

  private def validateWidgetsNumeber(datastory: Datastory): Boolean = datastory.widgets.size < maxWidgetsNumber
  private def validateTextSize(datastory: Datastory): Boolean = !datastory.widgets.exists(w => w.text.isDefined && w.text.get.length > maxTextSize)

  override def saveDatastory(user: String, datastory: Datastory, token: String, ws: WSClient): Future[Either[Error, Success]] = {
    if(validateTextSize(datastory) || validateWidgetsNumeber(datastory) )
      datastory.id match {
        case Some(id) =>
          updateDatastory(user, id, datastory, token, ws)
        case None =>
          val uid: String = UUID.randomUUID().toString
          val timestamp = ZonedDateTime.now().toString
          val newDatastory: Datastory = datastory.copy(id = Some(uid), timestamp = Some(timestamp.substring(0, timestamp.indexOf("+")).concat("Z")))
          insertDatastory(user, uid, newDatastory)
      }
    else
      Future.successful(Left(Error(Some(403), Some("datastory too mutch size."), Some(s"[text size] ${validateTextSize(datastory)}, [widgets number] ${validateWidgetsNumeber(datastory)}"))))
  }

  private def insertDatastory(user: String, id: String, datastory: Datastory) = {
    if (checkDatastory(datastory)) {
      val json: JsValue = Json.toJson(datastory)
      val obj: DBObject = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
      val resultInsert: casbah.TypeImports.WriteResult = collection.insert(obj)
      if (resultInsert.wasAcknowledged()) {
        Logger.logger.debug(s"datastory ${datastory.title} saved for user $user"); Future.successful(Right(Success(Some(s"$id"), None)))
      }
      else {
        Logger.logger.debug(s"error in save datastory ${datastory.title}"); Future.successful(Left(Error(Some(500), Some(s"error in save datastory ${datastory.title}"), None)))
      }
    }
    else {
      Logger.logger.debug(s"it is not allowed to save a public datastory with private widget")
      Future.successful(Left(Error(Some(401), Some(s"it is not allowed to save a public datastory with private widget"), None)))
    }
  }

  private def buildMessage(status: Int, title: String, org: String) = {
    status match {
      case 1 => s"La datastory $title è stata pubblicata per l'organizzazione $org"
      case 2 => s"E' stata publicata la datastory $title"
    }
  }

  private def chooseGroup(status: Int, org: String) = {
    status match {
      case 1 => org
      case 2 => OPEN_DATA_GROUP
    }
  }

  private def sendMessageToKafka(datastory: Datastory, token: String, ws: WSClient) = {
    Logger.logger.debug(s"kafka proxy $KAFKAPROXY")

    val message = s"""{
                     |"records":[{"value":{"group":"${chooseGroup(datastory.status, datastory.org)}","token":"$token","notificationtype": "info",
                     |"info":{"title":"Pubblicazione Datastory",
                     |"description":"${buildMessage(datastory.status, datastory.title, datastory.org)}",
                     |"link":"private/datastory/list/${datastory.id.get}"}}}]
                     |}""".stripMargin

    val jsonBody = Json.parse(message)

    Logger.debug(s"body to kafka: $jsonBody")

    val responseWs = ws.url(KAFKAPROXY + "/topics/notification")
      .withHeaders(("Content-Type", "application/vnd.kafka.v2+json"))
      .post(jsonBody)

    responseWs onComplete { res =>
      if( res.get.status == 200 ) {
        Logger.logger.debug(s"message sent to kakfa proxy for user ${datastory.user}")
      }
      else {
        Logger.logger.debug(s"error in sending message to kafka proxy for user ${datastory.user}: ${res.get.statusText}")
      }
    }
  }

  private def updateDatastory(user: String, id: String, datastory: Datastory, token: String, ws: WSClient) = {
    if (checkDatastory(datastory)) {
      getInternalDatastory(id).map(old => if(old.status != datastory.status) sendMessageToKafka(datastory, token, ws))
      val json: JsValue = Json.toJson(datastory)
      val obj: DBObject = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
      val query = MongoDBObject("id" -> id)
      val responseUpdate: TypeImports.WriteResult = collection.update(query, obj)
      if (responseUpdate.isUpdateOfExisting) {
        Logger.logger.debug(s"datastory ${datastory.title} updatate by $user"); Future.successful(Right(Success(Some(s"$id"), None)))
      }
      else {
        Logger.logger.debug(s"error in update datastory ${datastory.title}"); Future.successful(Left(Error(Some(500), Some(s"error in update datastory ${datastory.title}"), None)))
      }
    }
    else {
      Logger.logger.debug(s"it is not allowed to save a public datastory with private widget")
      Future.successful(Left(Error(Some(401), Some(s"it is not allowed to save a public datastory with private widget"), None)))
    }
  }

  private def checkDatastory(datastory: Datastory) = {
    datastory.status match {
      case 2 =>
        if (datastory.widgets.exists(widgets => widgets.pvt)) { Logger.logger.debug("error: found private widget"); false }
        else { Logger.logger.debug("only public widgets"); true }
      case _ => true
    }
  }

  private def getInternalDatastory(id: String): Option[Datastory] = {
    val result: Option[commons.TypeImports.DBObject] = collection.findOne(MongoDBObject("id" -> id))
    val jsonString: String = com.mongodb.util.JSON.serialize(result)
    val json: JsValue = Json.parse(jsonString)
    val datastoryResult: JsResult[Datastory] = json.validate[Datastory]
    datastoryResult match {
      case s: JsSuccess[Datastory] => Some(s.get)
      case e: JsError => Logger.logger.debug(s"[getInternalDatastory] error in parsing datastory: $e"); None
    }
  }

  private def delete(id: String, user: String): Future[Either[Error, Success]] = {
    import mongodb.casbah.query.Imports._
    val query = $and(mongodb.casbah.Imports.MongoDBObject("id" -> id), mongodb.casbah.Imports.MongoDBObject("user" -> user))

    val resultDelete: TypeImports.WriteResult = collection.remove(query)
    if (resultDelete.getN > 0) {
      Logger.logger.debug(s"$user deleted datastory $id")
      Future.successful(Right(Success(Some(s"$user deleted datastory $id"), None)))
    } else {
      Logger.logger.debug(s"$user not delet datastory $id")
      Future.successful(Left(Error(Some(500), Some(s"$user not delet datastory $id"), None)))
    }
  }

  override def deleteDatastory(id: String, user: String): Future[Either[Error, Success]] = {

    getInternalDatastory(id) match {
      case None => Future.successful(Left(Error(Some(404), Some("datastory not found"), None)))
      case Some(datastory) =>
        if (datastory.user.equals(user)) delete(id, user)
        else Future.successful(Left(Error(Some(401), Some(s"$user unauthorized to delete ${datastory.title}"), None)))
    }
  }

  private def createQuery(status: Option[Int], user: String, groups: List[String]): Imports.DBObject = {
    import mongodb.casbah.query.Imports._
    status match {
      case Some(0) =>
        $and(mongodb.casbah.Imports.MongoDBObject("status" -> 0), mongodb.casbah.Imports.MongoDBObject("user" -> user))
      case Some(1) =>
        $and(mongodb.casbah.Imports.MongoDBObject("status" -> 1), "org" $in groups)
      case Some(2) =>
        mongodb.casbah.Imports.MongoDBObject("status" -> 2)
      case None =>
        $or(
          $and(mongodb.casbah.Imports.MongoDBObject("status" -> 0), mongodb.casbah.Imports.MongoDBObject("user" -> user)),
          $and(mongodb.casbah.Imports.MongoDBObject("status" -> 1), "org" $in groups),
          mongodb.casbah.Imports.MongoDBObject("status" -> 2)
        )
    }
  }

  override def getAllDatastory(user: String, groups: List[String], limit: Option[Int], status: Option[Int]): Future[Either[Error, Seq[Datastory]]] = {
    val result: List[Imports.DBObject] = collection.find(createQuery(status, user, groups))
      .sort(mongodb.casbah.Imports.MongoDBObject("timestamp" -> -1))
      .limit(limit.getOrElse(1000)).toList
    if (result.isEmpty) {
      Logger.logger.debug("Datastories not found"); Future.successful(Left(Error(Some(404), Some("Datastories not found"), None)))
    }
    else {
      val jsonStrirng: String = com.mongodb.util.JSON.serialize(result)
      val json: JsValue = Json.parse(jsonStrirng)
      val seqDatastory: JsResult[Seq[Datastory]] = json.validate[Seq[Datastory]]
      seqDatastory match {
        case success: JsSuccess[Seq[Datastory]] => Logger.logger.debug(s"$user found ${success.get.size}"); Future.successful(Right(success.get))
        case error: JsError => Logger.logger.debug(s"[$user] error in get all datastories: $error"); Future.successful(Left(Error(Some(500), Some("Internal Server Error"), None)))
      }
    }
  }

  override def getAllPublicDatastory(limit: Option[Int]): Future[Either[Error, Seq[Datastory]]] = {
    val query = MongoDBObject("status" -> 2)
    val result: List[Imports.DBObject] = collection.find(query)
      .sort(MongoDBObject("timestamp" -> -1))
      .limit(limit.getOrElse(1000)).toList
    if (result.isEmpty) {
      Logger.logger.debug("Datastories not found"); Future.successful(Left(Error(Some(404), Some("Datastories not found"), None)))
    }
    else {
      val jsonStrirng: String = com.mongodb.util.JSON.serialize(result)
      val json: JsValue = Json.parse(jsonStrirng)
      val seqDatastory: JsResult[Seq[Datastory]] = json.validate[Seq[Datastory]]
      seqDatastory match {
        case success: JsSuccess[Seq[Datastory]] => Logger.logger.debug(s"found ${success.get.size}"); Future.successful(Right(success.get))
        case error: JsError => Logger.logger.debug(s"error in get all datastories: $error"); Future.successful(Left(Error(Some(500), Some("Internal Server Error"), None)))
      }
    }
  }

  override def getDatastoryById(id: String, user: String, group: List[String]): Future[Either[Error, Datastory]] = {
    import mongodb.casbah.query.Imports._
    val query = $and(
      mongodb.casbah.Imports.MongoDBObject("id" -> id),
      createQuery(None, user, group)
    )
    val result: Option[TypeImports.DBObject] = collection.findOne(query)
    result match {
      case Some(findResult) =>
        val jsonString: String = com.mongodb.util.JSON.serialize(findResult)
        val json: JsValue = Json.parse(jsonString)
        val datastoryResult: JsResult[Datastory] = json.validate[Datastory]
        datastoryResult match {
          case s: JsSuccess[Datastory] => Future.successful(Right(s.get))
          case e: JsError => Logger.logger.debug(s"[getDatastoryById] error in parsing datastory: $e"); Future.successful(Left(Error(Some(500), Some("Internal Server Error"), None)))
        }
      case None => Future.successful(Left(Error(Some(404), Some(s"Datastory $id not found"), None)))
    }

  }

  override def getPublicDatastoryById(id: String): Future[Either[Error, Datastory]] = {
    import mongodb.casbah.query.Imports._
    val query = $and(
      mongodb.casbah.Imports.MongoDBObject("id" -> id),
      mongodb.casbah.Imports.MongoDBObject("status" -> 2)
    )
    val result: Option[TypeImports.DBObject] = collection.findOne(query)
    result match {
      case Some(findResult) =>
        val jsonString: String = com.mongodb.util.JSON.serialize(findResult)
        val json: JsValue = Json.parse(jsonString)
        val datastoryResult: JsResult[Datastory] = json.validate[Datastory]
        datastoryResult match {
          case s: JsSuccess[Datastory] => Future.successful(Right(s.get))
          case e: JsError => Logger.logger.debug(s"[getDatastoryById] error in parsing datastory: $e"); Future.successful(Left(Error(Some(500), Some("Internal Server Error"), None)))
        }
      case None => Future.successful(Left(Error(Some(404), Some(s"Datastory $id not found"), None)))
    }
  }
}
