package repositories.push_notification

import ftd_api.yaml.{DeleteTTLNotificationInfo, Error, InsertTTLInfo, KafkaOffsett, KeysIntValue, LastOffset, Notification, Subscription, Success, SysNotificationInfo, TTL}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

class PushNotificationRepositoryDev extends PushNotificationRepository {

  override def saveSubscription(user: String, subscription: Subscription): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }

  override def getSubscriptions(user: String): Future[Seq[Subscription]] = {
    Future.successful(Seq[Subscription]())
  }

  override def saveNotifications(notifications: Notification): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }

  override def deleteSubscription(user: String, subscription: Subscription): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }

  override def deleteAllSubscription(user: String): Future[Success] = {
    Future.successful(Success(None, None))
  }

  override def updateNotifications(notification: Seq[Notification]): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }

  override def getAllNotifications(user: String, limit: Option[Int]): Future[Seq[Notification]] = {
    Future.successful(Seq[Notification]())
  }

  override def checkNewNotifications(user: String): Future[Seq[Notification]] = {
    Future.successful(Seq[Notification]())
  }

  override def getLastOffset(topicName: String): Future[Either[Error, LastOffset]] = {
    Future.successful(Right(LastOffset(0)))
  }

  override def updateTtl(ttl: Seq[KeysIntValue]): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(Some("ok"), None)))
  }

  override def getTtl: Future[Either[Error, Seq[KeysIntValue]]] = {
    Future.successful(Right(Seq[KeysIntValue]()))
  }

  override def systemNotificationInsert(sysNotificationInfo: SysNotificationInfo, token: String, ws: WSClient): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }

  override def deleteSystemNotificationByOffset(offset: Int): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }

  override def updateSystemNotification(offset: Int, notificationInfo: SysNotificationInfo): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }

  override def getSystemNotificationByOffset(offset: Int): Future[Either[Error, Notification]] = {
    Future.successful(Left(Error(None, None, None)))
  }

  override def getAllSystemNotification: Future[Either[Error, Seq[Notification]]] = {
    Future.successful(Right(Seq[Notification]()))
  }

  override def insertTtl(insertTTLInfo: InsertTTLInfo): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }

  def deleteTtl(deleteTTLNotificationsInfo: DeleteTTLNotificationInfo): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }

  override def getAllPublicSystemNotifications: Future[Either[Error, Seq[Notification]]] = {
    Future.successful(Right(Seq[Notification]()))
  }

  override def updateKafkaOffset(kafkaOffsett: KafkaOffsett): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }
}
