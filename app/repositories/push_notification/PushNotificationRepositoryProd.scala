package repositories.push_notification

import com.mongodb
import com.mongodb.casbah.Imports.{MongoCredential, MongoDBObject, ServerAddress}
import com.mongodb.casbah.{MongoClient, commons}
import com.mongodb.casbah.query.Imports.DBObject
import ftd_api.yaml.{Error, Notification, Subscription, Success}
import play.api.Logger
import utils.ConfigReader
import play.api.libs.json._

import scala.concurrent.Future

class PushNotificationRepositoryProd extends PushNotificationRepository {


  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort
  private val userName = ConfigReader.userName
  private val dbName = ConfigReader.database
  private val password = ConfigReader.password

  val server = new ServerAddress(mongoHost, mongoPort)
  val credentials = MongoCredential.createCredential(userName, dbName, password.toCharArray)
  val logger = Logger.logger


  private def getMongoCollection(collName: String) = {
    MongoClient(server, List(credentials))(dbName)(collName)
  }

  private def componiQuery(query: Query) =  {
    def simpleQuery(queryComponent: QueryComponent) = {
      MongoDBObject(queryComponent.nameField -> queryComponent.valueField)
    }

    def multiAndQuery(seqQueryComponent: Seq[QueryComponent]) = {
      import mongodb.casbah.query.Imports._

      $and(seqQueryComponent
        .map(queryComponent =>
          mongodb.casbah.Imports.MongoDBObject(queryComponent.nameField -> queryComponent.valueField))
      )
    }

    query match {
      case s: SimpleQuery => simpleQuery(s.queryCondition)
      case m: MultiQuery => multiAndQuery(m.seqQueryConditions)
    }

  }

  private def notificationToJson(notification: Notification) = {
    import ftd_api.yaml.ResponseWrites.NotificationWrites.writes

    writes(notification)
  }

  override def saveSubscription(user: String, subscription: Subscription): Future[Either[Error, Success]] = {
    import ftd_api.yaml.ResponseWrites.SubscriptionWrites.writes

    val coll = getMongoCollection("subscriptions")
    val json = writes(subscription)
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    obj.put("user", user)
    val resultInsert = coll.insert(obj)

    if(resultInsert.wasAcknowledged()){
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

    val coll = getMongoCollection("subscriptions")
    val results = coll.find(componiQuery(SimpleQuery(QueryComponent("user", user)))).toList
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
    val coll = getMongoCollection("notifications")
    val obj = com.mongodb.util.JSON.parse(notificationToJson(notification).toString()).asInstanceOf[DBObject]
    val result = coll.insert(obj)
    val response = if(result.wasAcknowledged()){
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
    val coll = getMongoCollection("notifications")
    val seqJson = notifications.map(n => (n.offset.get, n.user.get, notificationToJson(n)))

    val seqMongoObj = seqJson.map{ case (offset, user, json) =>
      (offset, user, com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject])
    }

    val mongoResults = seqMongoObj.map{
      case (offset, user, mongoObj) =>
        (offset, user,
          coll.update(
            componiQuery(MultiQuery(Seq(QueryComponent("user", user), QueryComponent("offset", offset)))),
        mongoObj)
      )
    }

    mongoResults.foreach { case(offset, user, writeResultMongo) =>
      if (writeResultMongo.getN == 1) Logger.logger.debug(s"offset $offset updated for user $user")
      else Logger.logger.debug(s"$offset for user $user not found")
    }

    val result = if(mongoResults.exists{ case (_, _, writeResultMongo) => writeResultMongo.getN == 0})
      Left(Error(Some(500), Some("error in update notifications"), None))
    else
      Right(Success(Some("all notifications are updated"), None))

    Future.successful(result)
  }

  override def getAllNotifications(user: String, limit: Option[Int]): Future[Seq[Notification]] = {
    import ftd_api.yaml.BodyReads.NotificationReads

    val coll = getMongoCollection("notifications")
    val query: commons.Imports.DBObject = MongoDBObject("user" -> user)
    val results = coll.find(query).limit(limit.getOrElse(0))
      .sort(MongoDBObject("timestamp" -> -1)
      ).toList
    val jsonString = com.mongodb.util.JSON.serialize(results)
    val json = Json.parse(jsonString)
    val notificationsJsResult = json.validate[Seq[Notification]]
    val notifications = notificationsJsResult match {
      case s: JsSuccess[Seq[Notification]] => s.get
      case _: JsError => Seq()
    }
    Logger.logger.debug(s"getAllNotificaitons: finds ${notifications.size} notifications for user $user")
    Future.successful(notifications)
  }

  override def checkNewNotifications(user: String): Future[Seq[Notification]] = {
    import ftd_api.yaml.BodyReads.NotificationReads

    Logger.logger.debug(s"check new notifications for user $user")

    val seqQueryConditions = Seq(QueryComponent("user", user), QueryComponent("status", 0))
    val result = getMongoCollection("notifications").find(componiQuery(MultiQuery(seqQueryConditions)))
      .sort(MongoDBObject("timestamp" -> -1)).toList
    val jsonStrig = com.mongodb.util.JSON.serialize(result)
    val json = Json.parse(jsonStrig)
    val notificationsJsResult = json.validate[Seq[Notification]]
    val notifications = notificationsJsResult match {
      case s: JsSuccess[Seq[Notification]] => s.get
      case _: JsError => Seq()
    }

    Logger.logger.debug(s"checkNewNotifications: find ${notifications.size} notifications")

    if(notifications.nonEmpty) {
      Logger.logger.debug(s"calling updateNotifications for ${notifications.map(n => s"{offset: ${n.offset.get}, user: ${n.user.get}}").mkString(", ")}")
      updateNotifications(notifications.map(n => n.copy(status = Option(1))))
    }

    Future.successful(notifications)
  }
}



