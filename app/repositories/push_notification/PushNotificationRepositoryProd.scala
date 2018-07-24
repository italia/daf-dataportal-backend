package repositories.push_notification

import com.mongodb
import com.mongodb.casbah
import com.mongodb.casbah.Imports.{MongoCredential, MongoDBObject, ServerAddress}
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.query.Imports
import com.mongodb.casbah.query.Imports.DBObject
import ftd_api.yaml.{Error, Notification, Subscription, Success}
import play.api.Logger
import utils.ConfigReader
import play.api.libs.json._

import scala.concurrent.Future
import scala.util.Try

class PushNotificationRepositoryProd extends PushNotificationRepository {


  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort
  private val userName = ConfigReader.userName
  private val source = ConfigReader.database
  private val password = ConfigReader.password

  val server = new ServerAddress(mongoHost, mongoPort)
  val credentials = MongoCredential.createCredential(userName, source, password.toCharArray)
  val logger = Logger.logger


  override def saveSubscription(user: String, subscription: Subscription): Future[Either[Error, Success]] = {
    import ftd_api.yaml.ResponseWrites.SubscriptionWrites.writes

    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("subscriptions")
    val json = writes(subscription)
    val obj: DBObject = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    obj.put("user", user)
    val resultInsert = Try{coll.save(obj)}

    if(resultInsert.isSuccess){
      Logger.logger.debug(s"subscription saved for user $user")
      Future.successful(Right(Success(Some(s"subscription saved for user $user"), None)))
    }
    else{
      Logger.logger.debug(s"error in save subscription for user $user")
      Future.successful(Left(Error(Some(500), Some(s"error in save subsciption for user $user"), None)))
    }

  }

  override def getSubscriptions(user: String): Future[Seq[Subscription]] = {
    import ftd_api.yaml.BodyReads.SubscriptionReads
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("subscriptions")
    val query = MongoDBObject("user" -> user)
    val results = coll.find(query).toList
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
    val subscriptionsJsResult = json.validate[Seq[Subscription]]
    val subscriptions = subscriptionsJsResult match {
      case s: JsSuccess[Seq[Subscription]] => s.get
      case _: JsError => Seq()
    }

    Future.successful(subscriptions)
  }

  override def saveNotifications(notification: Notification): Future[Either[Error, Success]] = {
    import ftd_api.yaml.ResponseWrites.NotificationWrites.writes
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("notifications")
    val json = writes(notification)
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    val result = Try{coll.insert(obj)}
    val response = if(result.isSuccess) {
      Logger.logger.debug(s"notification saved in mongo")
      Right(Success(Some(s"notification saved in mongo"), None))
    }
      else{
      Logger.logger.debug(s"error in save notification")
      Left(Error(None, Some(s"error in save notification"), None))
    }
    Future.successful(response)
  }

  override def updateNotifications(notifications: Seq[Notification]): Future[Either[Error, Success]] = {
    import ftd_api.yaml.ResponseWrites.NotificationWrites.writes

    def createQuery(user: String, offset: Int): Imports.DBObject = {
      import mongodb.casbah.query.Imports._

      val queryOffset = mongodb.casbah.Imports.MongoDBObject("offset" -> offset)
      val queryUser = mongodb.casbah.Imports.MongoDBObject("user" -> user)
      $and(queryOffset, queryUser)
    }

    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("notifications")
    val seqJson = notifications.map(n => (n.offset.get, n.user.get, writes(n)))
    val seqMongoObj = seqJson.map{ case (offset, user, json) =>
      (offset, user, com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject])
    }
    val mongoResults: Seq[(Int, casbah.TypeImports.WriteResult)] = seqMongoObj.map{
      case (offset, user, mongoObj) => (offset, coll.update(createQuery(user, offset), mongoObj))
    }
    mongoResults.foreach(elem =>
      if(elem._2.getN == 1) Logger.logger.debug(s"${elem._1} updated")
      else Logger.logger.debug(s"${elem._1} not found")
    )

    val result = if(mongoResults.exists(x => x._2.getN() == 0))
      Left(Error(Some(500), Some("error in update notifications"), None))
    else
      Right(Success(Some("all notifications are updated"), None))
    Future.successful(result)
  }

  override def getAllNotifications(user: String): Future[Seq[Notification]] = {
    import ftd_api.yaml.BodyReads.NotificationReads
    val mongoClient = MongoClient(server, List(credentials))
    val db = mongoClient(source)
    val coll = db("notifications")
    val query = MongoDBObject("user" -> user)
    val results = coll.find(query).sort(MongoDBObject("timestamp" -> -1)).toList
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
    val notificationsJsResult = json.validate[Seq[Notification]]
    val notifications = notificationsJsResult match {
      case s: JsSuccess[Seq[Notification]] => s.get
      case _: JsError => Seq()
    }
    Future.successful(notifications)
  }
}



