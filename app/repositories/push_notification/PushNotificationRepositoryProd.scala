package repositories.push_notification

import com.mongodb
import com.mongodb.BasicDBObject
import com.mongodb.casbah.Imports.{MongoCredential, MongoDBObject, ServerAddress}
import com.mongodb.casbah.{MongoClient, MongoCollection, MongoDB}
import com.mongodb.casbah.query.Imports.DBObject
import ftd_api.yaml.{DeleteTTLNotificationInfo, Error, InfoNotification, InsertTTLInfo, KafkaOffsett, KeysIntValue, LastOffset, Notification, Subscription, Success, SysNotificationInfo}
import org.joda.time.DateTime
import play.api.Logger
import utils.ConfigReader
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.Future
import scala.util
import scala.util.{Failure, Try}
import scala.concurrent.ExecutionContext.Implicits._

class PushNotificationRepositoryProd extends PushNotificationRepository {


  private val mongoHost: String = ConfigReader.getDbHost
  private val mongoPort = ConfigReader.getDbPort
  private val userName = ConfigReader.userName
  private val userRoot = ConfigReader.userRoot
  private val dbName = ConfigReader.database
  private val dbRootName = ConfigReader.databaseRoot
  private val password = ConfigReader.password
  private val rootPassword = ConfigReader.getRootPasswprd

  private val collNotificationName = ConfigReader.getCollNotificationName
  private val collSubscriptionName = ConfigReader.getCollSubscriptionName
  private val collKafkaOffset = ConfigReader.getCollKafkaOffsets

  val server = new ServerAddress(mongoHost, mongoPort)
  val credentials = MongoCredential.createCredential(userName, dbName, password.toCharArray)
  val logger = Logger.logger

  val catalogManagerHost = ConfigReader.getCatalogManagerHost
  val catalogManagerNotificationPath = ConfigReader.getCatalogManagerNotificationPath
  val openDataUser = ConfigReader.getOpenDataUser
  val openDataGroup = ConfigReader.getOpenDataGroup
  val sysAdminName = ConfigReader.getSysAdminName
  val mapIndexinfo = ConfigReader.getNotificationInfo
  val sysNotificationTypeName = ConfigReader.getSysNotificationTypeName

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

  private def notificationToJson(notification: Notification) = {
    val infoDBObject = notification.info match {
      case Some(info) =>
        MongoDBObject(
          List(
            "name" -> info.name,
            "description" -> info.description,
            "errors" -> info.errors,
            "link" -> info.link,
            "title" -> info.title
          )
        )
      case None    => MongoDBObject()
    }

    val creationDate = notification.createDate.replace("_", "T").concat("Z")
    val endDate = Try{new DateTime(notification.endDate.get.replace("_", "T").concat("Z")).toDate}

    MongoDBObject(
      List(
        "createDate" -> new DateTime(creationDate).toDate,
        "offset" -> notification.offset,
        "status" -> notification.status,
        "user" -> notification.user,
        "notificationtype" -> notification.notificationtype,
        "info" -> infoDBObject,
        "endDate" -> endDate.getOrElse(null)
      )
    )
  }

  override def saveSubscription(user: String, subscription: Subscription): Future[Either[Error, Success]] = {
    import ftd_api.yaml.ResponseWrites.SubscriptionWrites.writes

    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val coll = mongoDB(collSubscriptionName)
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

  override def deleteSubscription(user: String, subscription: Subscription): Future[Either[Error, Success]] = {
    import ftd_api.yaml.ResponseWrites.SubscriptionWrites.writes

    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val coll = mongoDB(collSubscriptionName)
    val json = writes(subscription)
    val obj = com.mongodb.util.JSON.parse(json.toString()).asInstanceOf[DBObject]
    obj.put("user", user)
    val resultRemove = coll.remove(obj)
    if(resultRemove.getN > 0) {
      logger.debug(s"$user deleted $subscription")
      Future.successful(Right(Success(Some(s"$user deleted $subscription"), None)))
    } else {
      logger.debug(s"delete subscription $user: $subscription")
      Future.successful(Left(Error(Some(404), Some("subscription not found"), None)))
    }
  }

  override def getTtl: Future[Either[Error, Seq[KeysIntValue]]] = {
    val mongoClient = com.mongodb.casbah.MongoClient(server, List(credentials))
    val mongoDB: MongoDB = mongoClient(dbName)
    val coll = mongoDB(collNotificationName)

    val indexesInfo = coll.indexInfo.toList.filterNot( x => x.toMap.get("name").equals("_id_"))
    val seqKeyIntValue = indexesInfo.map{ index =>
      Try{
        val json = Json.parse(index.toString)
        KeysIntValue((json \ "partialFilterExpression" \ "notificationtype").get.toString().replace("\"", ""), (json \ "expireAfterSeconds").get.as[Int])
      }
    }

    if(seqKeyIntValue.exists(x => x.isFailure)){ Logger.debug("error in get ttl"); Future.successful(Left(Error(Some(500), Some("Error in get ttl"), None))) }
    else if(seqKeyIntValue.isEmpty) { Logger.debug("ttl not found"); Future.successful(Left(Error(Some(404), Some("TTL not found"), None))) }
    else { Logger.debug("successful get ttl"); Future.successful(Right(seqKeyIntValue.map{v => v.get}.filterNot(v => v.name.equals(sysNotificationTypeName)))) }
  }

  override def updateTtl(ttl: Seq[KeysIntValue]): Future[Either[Error, Success]] = {

    def keyValidation = { ttl.map{ elem => (elem.name, mapIndexinfo.contains(elem.name))} }

    def update(db: MongoDB, keyValue: Int, expirationAfterSeconds: Int) = {
        db.underlying.command(
          new BasicDBObject("collMod", collNotificationName)
            .append("index", new BasicDBObject("keyPattern", new BasicDBObject("createDate", keyValue)).append("expireAfterSeconds", expirationAfterSeconds))
        )
    }

    val rootCredentials = MongoCredential.createCredential(userRoot, dbRootName, rootPassword.toCharArray)

    val mongoClient = com.mongodb.casbah.MongoClient(server, List(rootCredentials))
    val mongoDB: MongoDB = mongoClient(dbName)

    if(keyValidation.forall(x => x._2)){
      val result = ttl.map{ keysIntValue =>
        logger.debug(s"update index ${keysIntValue.name} to value ${keysIntValue.value}")
        (keysIntValue.name, update(mongoDB, mapIndexinfo(keysIntValue.name), keysIntValue.value))
      }

      if(result.exists(v => !v._2.ok())) {
        Logger.debug("error in update ttl for index: " + result.filter(x => !x._2.ok()).map(x => x._1 + ": " + x._2.getErrorMessage).mkString(", "))
        Future.successful(Left(Error(Some(500), Some("error in update ttl for index: " + result.filter(x => !x._2.ok()).map(x => x._1).mkString(", ")), None)))
      } else {
        Logger.debug(result.map{ x => s"${x._1} update"}.mkString(", "))
        Future.successful(Right(Success(Some("ttl updatate fot index: " + result.map(x => x._1).mkString(", ")), None)))
      }
    } else {
      logger.debug("[error in update indexes] " + keyValidation.filter(x => !x._2).map(_._1).mkString(", ") + " not found")
      Future.successful(Left(Error(Some(500), Some("[error in update indexes] " + keyValidation.filter(x => !x._2).map(_._1).mkString(", ") + " not found"), None)))
    }


  }

  override def deleteAllSubscription(user: String): Future[Success] = {
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val coll = mongoDB(collSubscriptionName)
    val obj = DBObject("user" -> user)
    val response = coll.remove(obj)
    logger.debug(s"delete ${response.getN} subscriptions for user $user")
    Future.successful(Success(Some(s"delete all subscriptions for user $user"), None))
  }

  override def getSubscriptions(user: String): Future[Seq[Subscription]] = {
    import ftd_api.yaml.BodyReads.SubscriptionReads

    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val coll = mongoDB(collSubscriptionName)
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
    val mongoClient = com.mongodb.casbah.MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val coll: MongoCollection = mongoDB(collNotificationName)
    val obj = notificationToJson(notification)

    val result = coll.insert(obj)
    val response = if(result.wasAcknowledged()){
      logger.debug(s"notification saved in mongo: ${notification.offset}, ${notification.user}")
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
    val coll = mongoDB(collNotificationName)

    val seqJson = notifications.map(n => (n.offset, n.user, n.notificationtype, notificationToJson(n)))

    val seqMongoObj = seqJson.map{ case (offset, user, notificationType, mongoObj) =>
      (offset, user, notificationType, mongoObj)
    }

    val mongoResults = seqMongoObj.map{
      case (offset, user, notificationType, mongoObj) =>
        (offset, user,
          coll.update(
            composeQuery(MultiQuery(Seq(QueryComponent("user", user), QueryComponent("notificationtype", notificationType), QueryComponent("offset", offset)))),
            mongoObj
          )
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
    val results = mongoDB(collNotificationName)
      .find(composeQuery(SimpleQuery(QueryComponent("user", user))))
      .limit(limit.getOrElse(0))
      .sort(composeQuery(SimpleQuery(QueryComponent("createDate",-1))))
      .toList
    mongoClient.close()
    val notifications = validateSeqNotification(Json.parse(com.mongodb.util.JSON.serialize(results)).as[JsArray])
    logger.debug(s"getAllNotificaitons: finds ${notifications.size} notifications for user $user")
    Future.successful(notifications)
  }

  private def validateSeqNotification(seqNotification: JsArray) = {
    seqNotification.value.map{ jsValue =>
      validateNotification(jsValue)
    }.collect { case Right(r) => r}
  }

  private def validateNotification(json: JsValue) = {
    import ftd_api.yaml.BodyReads.InfoNotificationReads

    def parseDate(dateJsLookup: JsLookupResult) = {
      dateJsLookup match {
        case JsDefined(value)       =>
          val str: String = value.toString().replace("\"", "")
          Some(str.replace("T", "_").substring(0, str.lastIndexOf(".")))
        case _: JsUndefined => None
      }
    }

    val notification = Try{
      Notification(
        offset = (json \ "offset").as[Int],
        status = (json \ "status").as[Int],
        user = (json \ "user").as[String],
        notificationtype = (json \ "notificationtype").as[String],
        createDate = parseDate(json \ "createDate" \ "$date").get,
        info = (json \ "info").asOpt[InfoNotification],
        endDate = parseDate(json \ "endDate" \ "$date")
      )
    }
    notification match {
      case Failure(exception)  => Logger.debug(s"validation error: $exception"); Left(exception)
      case util.Success(value) => Logger.debug(s"success in validation: {${value.user} : ${value.offset}}"); Right(value)
    }
  }

  override def checkNewNotifications(user: String): Future[Seq[Notification]] = {
    val seqQueryConditions = Seq(QueryComponent("user", user), QueryComponent("status", 0))
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val results = mongoDB(collNotificationName)
      .find(composeQuery(MultiQuery(seqQueryConditions)))
      .sort(composeQuery(SimpleQuery(QueryComponent("createDate",-1))))
      .toList
    mongoClient.close()

    val notifications = validateSeqNotification(Json.parse(com.mongodb.util.JSON.serialize(results)).as[JsArray])

    if(notifications.nonEmpty) {
      Logger.logger.debug(s"checkNewNotifications: find ${notifications.size} notifications")
      Logger.logger.debug(s"calling updateNotifications for ${notifications.map(n => s"{offset: ${n.offset}, user: ${n.user}}").mkString(", ")}")
      updateNotifications(notifications.map(n => n.copy(status = 1)))
    }
    Future.successful(notifications)
  }

  override def getLastOffset(topicName: String): Future[Either[Error, Long]] = {

    def validateLastOffset(json: JsValue) = {
      Try{
        KafkaOffsett(
          topicName = (json \ "topicName").as[String],
          offset = (json \ "offset").as[Long]
        )
      } match {
        case Failure(error)             =>
          logger.debug(s"[validate lastOffset] error: $error")
          Left(Error(Some(500), Some(error.getMessage), None))
        case util.Success(kafkaOffsett) =>
          logger.debug(s"[validate lastOffset] success: ${kafkaOffsett.topicName} ${kafkaOffsett.offset}")
          Right(kafkaOffsett.offset)
      }
    }

    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val query = composeQuery(SimpleQuery(QueryComponent("topicName", topicName)))

    logger.debug(s"[validate lastOffset]query -> $query")

    val results = mongoDB(collKafkaOffset)
      .find(query)
      .limit(1)
    mongoClient.close()
    val jsonString: String = com.mongodb.util.JSON.serialize(results)
    val json: JsValue = Json.parse(jsonString)
    Future.successful(validateLastOffset(json))
  }

  private def sendToKafka(sysNotificationInfo: SysNotificationInfo, token: String, ws: WSClient): Future[Either[Error, Success]] = {
    val jsonString =
      s"""
         |{
         |"topicName":"notification",
         |"description":"${sysNotificationInfo.description}",
         |"notificationType":"system",
         |"title":"${sysNotificationInfo.title}",
         |"expirationDate":"${sysNotificationInfo.endDate}",
         |"group":"$openDataGroup"
         |}
      """.stripMargin

    val url = s"$catalogManagerHost/$catalogManagerNotificationPath"

    Logger.debug(s"call to catalog at url $url with body $jsonString")

    val response = ws.url(url)
      .withHeaders("Authorization" -> s"Bearer $token", "Content-Type" -> "application/json")
      .post(Json.parse(jsonString))

    response.map{ res =>
      res.status match {
        case 200       => Logger.debug(s"200: ${res.body}"); Right(Success(Some((res.json \ "message").get.as[String]), None))
        case 401 | 500 => Logger.debug(s"${res.status}: ${res.body}"); Left(Error(Some(res.status), Some(res.body), None))
        case _         => Logger.debug(s"500 --> ${res.status}: ${res.body}"); Left(Error(Some(500), Some(res.body), None))
      }
    }
  }

  override def systemNotificationInsert(sysNotificationInfo: SysNotificationInfo, token: String, ws: WSClient): Future[Either[Error, Success]] = {
    if(sysNotificationInfo.description.equals("") || sysNotificationInfo.title.equals(""))
      { logger.debug(s"[notification ${sysNotificationInfo.title}] title or description missing"); Future.successful(Left(Error(Some(403), Some("title or description missing"), None))) }
    else
      sendToKafka(sysNotificationInfo, token, ws)
  }

  def deleteSystemNotificationByOffset(offset: Int): Future[Either[Error, Success]] = {
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val collection = mongoDB(collNotificationName)
    collection.findOne(new BasicDBObject("offset", offset).append("user", sysAdminName)) match {
      case Some(_) =>
        val responseDelete = collection.remove(new BasicDBObject("offset", offset))
        if(responseDelete.getN > 0) { logger.debug(s"$sysAdminName deleted ${responseDelete.getN} notifications"); Future.successful(Right(Success(Some(s"$sysAdminName deleted ${responseDelete.getN} notifications"), None))) }
        else { logger.debug("error in delete notification"); Future.successful(Left(Error(Some(500), Some(s"error in delete notification"), None)))}
      case None    => Future.successful(Left(Error(Some(404), Some(s"No found sys notification with offset $offset"), None)))
    }
  }

  override def updateSystemNotification(offset: Int, notificationInfo: SysNotificationInfo): Future[Either[Error, Success]] = {

    def parseDate(endDate: String) = Try{new DateTime(endDate.replace("_", "T").concat("Z")).toDate}

    def createQueryUpdateFields = {
      new BasicDBObject("$set", new BasicDBObject("status", 0)
        .append("info.title", notificationInfo.title)
        .append("endDate", parseDate(notificationInfo.endDate).getOrElse(null))
        .append("info.description", notificationInfo.description)
      )
    }

    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val collection = mongoDB(collNotificationName)
    collection.findOne(new BasicDBObject("offset", offset).append("user", sysAdminName)) match {
      case Some(_) =>
        logger.debug(s"Update collection $collNotificationName, offset $offset")
        val query = MongoDBObject("offset" -> offset)
        val responseUpdates = collection.update(query, createQueryUpdateFields, multi = true)
        if(responseUpdates.isUpdateOfExisting) { logger.debug("Mongo update: success"); Future.successful(Right(Success(Some("Mongo update: success"), None)))}
        else { logger.debug("Mongo update: error"); Future.successful(Left(Error(Some(500), Some("Mongo update: success"), None)))}
      case None    => { logger.debug("notifications not found"); Future.successful(Left(Error(Some(404), Some(s"No found sys notification with offset $offset"), None))) }
    }
  }

  override def getSystemNotificationByOffset(offset: Int): Future[Either[Error, Notification]] = {
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val collection = mongoDB(collNotificationName)
    val query = new BasicDBObject("offset", offset).append("user", sysAdminName)
    collection.findOne(query) match {
      case Some(resutlFind) =>
        val json = Json.parse(com.mongodb.util.JSON.serialize(resutlFind))
        validateNotification(json) match {
          case Right(notification) => logger.debug(s"found: $notification"); Future.successful(Right(notification))
          case Left(error)         => logger.debug(error.getMessage); Future.successful(Left(Error(Some(500), Some(error.getMessage), None)))
        }
      case None             => logger.debug(s"notification with offset $offset not found"); Future.successful(Left(Error(Some(404), Some(s"notification with offset $offset not found"), None)))
    }
  }

  override def getAllSystemNotification: Future[Either[Error, Seq[Notification]]] = {
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val collection = mongoDB(collNotificationName)
    val query = new BasicDBObject("user", sysAdminName).append("notificationtype", sysNotificationTypeName)
    val results = collection.find(query).sort(MongoDBObject("createDate" -> 1)).toList
    if(results.isEmpty){ logger.debug("notification not found"); Future.successful(Right(Seq[Notification]()))}
    else{
      val notificationsSeq = validateSeqNotification(Json.parse(com.mongodb.util.JSON.serialize(results)).as[JsArray])
      logger.debug(s"found ${notificationsSeq.size} notifications")
      Future.successful(Right(notificationsSeq))
    }
  }

  override def getAllPublicSystemNotifications: Future[Either[Error, Seq[Notification]]] = {
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val collection = mongoDB(collNotificationName)
    val query = new BasicDBObject("user", openDataUser)
    val results = collection.find(query).sort(MongoDBObject("createDate" -> 1)).toList
    if(results.isEmpty){ logger.debug("notification not found"); Future.successful(Right(Seq[Notification]()))}
    else{
      val notificationsSeq = validateSeqNotification(Json.parse(com.mongodb.util.JSON.serialize(results)).as[JsArray])
      logger.debug(s"found ${notificationsSeq.size} notifications opendata")
      Future.successful(Right(notificationsSeq))
    }
  }

  override def insertTtl(insertTTLInfo: InsertTTLInfo): Future[Either[Error, Success]] = {
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val collection = mongoDB(collNotificationName)

    var partialFilterObject = new BasicDBObject(insertTTLInfo.partialFilter.head.name, insertTTLInfo.partialFilter.head.value)
    insertTTLInfo.partialFilter.foreach{ m => partialFilterObject = partialFilterObject.append(m.name, if(m.isInt.getOrElse(false)) m.value.toInt else m.value)}

    val resultCreateIndex = Try{collection.underlying.createIndex(
      new BasicDBObject(insertTTLInfo.keyName, insertTTLInfo.keyValue),
      new BasicDBObject("expireAfterSeconds", insertTTLInfo.expireAfterSeconds)
        .append("partialFilterExpression", partialFilterObject)
    )}

    if(resultCreateIndex.isSuccess) { logger.debug(s"index ${insertTTLInfo.keyName} created"); Future.successful(Right(Success(Some(s"index ${insertTTLInfo.keyName} created"), None))) }
    else { logger.debug("error in create index"); Future.successful(Left(Error(Some(500), Some("Error in create index"), None))) }
  }

  override def deleteTtl(deleteTTLNotificationsInfo: DeleteTTLNotificationInfo): Future[Either[Error, Success]] = {
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val collection = mongoDB(collNotificationName)
    if(mapIndexinfo.contains(deleteTTLNotificationsInfo.notificationType)){
      logger.debug(s"try to delete index ${deleteTTLNotificationsInfo.keyName}")
      Try{
        collection.dropIndex(s"${deleteTTLNotificationsInfo.keyName}_${mapIndexinfo(deleteTTLNotificationsInfo.notificationType)}")
      } match {
        case Failure(exception) =>
          logger.debug(s"error in delete index ${deleteTTLNotificationsInfo.keyName}: $exception")
          Future.successful(Left(Error(Some(500), Some(s"error in delete index ${deleteTTLNotificationsInfo.keyName}"), None)))
        case util.Success(_) =>
          logger.debug(s"index ${deleteTTLNotificationsInfo.keyName} deleted")
          Future.successful(Right(Success(Some(s"index ${deleteTTLNotificationsInfo.keyName} deleted"), None)))
      }
    } else {
      logger.debug(s"${deleteTTLNotificationsInfo.keyName} not found in configuration file")
      Future.successful(Left(Error(Some(500), Some(s"${deleteTTLNotificationsInfo.keyName} not found in configuration file"), None)))
    }
  }

  override def updateKafkaOffset(kafkaOffsett: KafkaOffsett): Future[Either[Error, Success]] = {
    val mongoClient = MongoClient(server, List(credentials))
    val mongoDB = mongoClient(dbName)
    val collection = mongoDB(collKafkaOffset)
    val query = MongoDBObject("topicName" -> kafkaOffsett.topicName)
    val response = collection.update(query, new BasicDBObject("$set", new BasicDBObject("offset", kafkaOffsett.offset)))
    if(response.isUpdateOfExisting){
      logger.debug(s"topic ${kafkaOffsett.topicName} update to ${kafkaOffsett.offset}")
      Future.successful(Right(Success(Some(s"topic ${kafkaOffsett.topicName} update to ${kafkaOffsett.offset}"), None)))
    } else {
      logger.debug(s"error in update topic ${kafkaOffsett.topicName} update to ${kafkaOffsett.offset}")
      Future.successful(Left(Error(Some(500), Some(s"error in update topic ${kafkaOffsett.topicName} update to ${kafkaOffsett.offset}"), None)))
    }


  }
}



