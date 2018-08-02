package repositories.push_notification

import com.mongodb
import com.mongodb.casbah
import com.mongodb.casbah.Imports.{MongoCredential, ServerAddress}
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


  private def composeQuery(query: Query) =  {
    def simpleQuery(queryComponent: QueryComponent) = {
      mongodb.casbah.Imports.MongoDBObject(queryComponent.nameField -> queryComponent.valueField)
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

  private def validateSeqNotifications(seqMongoDBObj: Seq[casbah.Imports.DBObject]): JsResult[Seq[Notification]] = {
    import ftd_api.yaml.BodyReads.NotificationReads

    val jsonString = com.mongodb.util.JSON.serialize(seqMongoDBObj)
    val json = Json.parse(jsonString)
    json.validate[Seq[Notification]]
  }

  private def notificationToJson(notification: Notification) = {
    import ftd_api.yaml.ResponseWrites.NotificationWrites.writes

    writes(notification)
  }

  override def saveSubscription(user: String, subscription: Subscription): Future[Either[Error, Success]] = {
    import ftd_api.yaml.ResponseWrites.SubscriptionWrites.writes

    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val coll = mongoDB("subscriptions")
    val json = writes(subscription)
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    obj.put("user", user)
    val resultInsert = coll.insert(obj)
    mongoClient.close()

    if(resultInsert.wasAcknowledged()){
      logger.debug(s"subscription saved for user $user")
      Future.successful(Right(Success(Some(s"subscription saved for user $user"), None)))
    }
    else{
      logger.debug(s"error in save subscription for user $user")
      Future.successful(Left(Error(Some(500), Some(s"error in save subsciption for user $user"), None)))
    }

  }

  override def getSubscriptions(user: String): Future[Seq[Subscription]] = {
    import ftd_api.yaml.BodyReads.SubscriptionReads

    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val coll = mongoDB("subscriptions")
    val results = coll.find(composeQuery(SimpleQuery(QueryComponent("user", user)))).toList
    mongoClient.close()
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
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val coll = mongoDB("notifications")
    val obj = com.mongodb.util.JSON.parse(notificationToJson(notification).toString()).asInstanceOf[DBObject]
    val result = coll.insert(obj)
    val response = if(result.wasAcknowledged()){
      logger.debug(s"notification saved in mongo")
      Right(Success(Some(s"notification saved in mongo"), None))
    }
    else{
      logger.debug(s"error in save notification")
      Left(Error(None, Some(s"error in save notification"), None))
    }
    Future.successful(response)
  }

  override def updateNotifications(notifications: Seq[Notification]): Future[Either[Error, Success]] = {
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val coll = mongoDB("notifications")
    val seqJson = notifications.map(n => (n.offset.get, n.user.get, notificationToJson(n)))

    val seqMongoObj = seqJson.map{ case (offset, user, json) =>
      (offset, user, com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject])
    }

    val mongoResults = seqMongoObj.map{
      case (offset, user, mongoObj) =>
        (offset, user,
          coll.update(
            composeQuery(MultiQuery(Seq(QueryComponent("user", user), QueryComponent("offset", offset)))),
            mongoObj)
        )
    }

    mongoClient.close()

    mongoResults.foreach { case(offset, user, writeResultMongo) =>
      if (writeResultMongo.getN == 1) logger.debug(s"offset $offset updated for user $user")
      else logger.debug(s"$offset for user $user not found")
    }

    val result = if(mongoResults.exists{ case (_, _, writeResultMongo) => writeResultMongo.getN == 0})
      Left(Error(Some(500), Some("error in update notifications"), None))
    else
      Right(Success(Some("all notifications are updated"), None))

    Future.successful(result)
  }

  override def getAllNotifications(user: String, limit: Option[Int]): Future[Seq[Notification]] = {
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val results = mongoDB("notifications")
      .find(composeQuery(SimpleQuery(QueryComponent("user", user))))
      .limit(limit.getOrElse(0))
      .sort(composeQuery(SimpleQuery(QueryComponent("timestamp",-1))))
      .toList
    mongoClient.close()
    val notifications = validateSeqNotifications(results) match {
      case s: JsSuccess[Seq[Notification]] => s.get
      case _: JsError => Seq()
    }
    logger.debug(s"getAllNotificaitons: finds ${notifications.size} notifications for user $user")
    Future.successful(notifications)
  }

  override def checkNewNotifications(user: String): Future[Seq[Notification]] = {
    val seqQueryConditions = Seq(QueryComponent("user", user), QueryComponent("status", 0))
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val result = mongoDB("notifications")
      .find(composeQuery(MultiQuery(seqQueryConditions)))
      .sort(composeQuery(SimpleQuery(QueryComponent("timestamp",-1))))
      .toList
    mongoClient.close()
    val notifications = validateSeqNotifications(result) match {
      case s: JsSuccess[Seq[Notification]] => s.get
      case _: JsError => Seq()
    }

    if(notifications.nonEmpty) {
      Logger.logger.debug(s"checkNewNotifications: find ${notifications.size} notifications")
      Logger.logger.debug(s"calling updateNotifications for ${notifications.map(n => s"{offset: ${n.offset.get}, user: ${n.user.get}}").mkString(", ")}")
      updateNotifications(notifications.map(n => n.copy(status = Option(1))))
    }
    else Logger.logger.debug(s"checkNewNotifications: no new notifications")

    Future.successful(notifications)
  }
}



