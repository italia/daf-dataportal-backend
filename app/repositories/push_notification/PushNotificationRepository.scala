package repositories.push_notification

import ftd_api.yaml.{DeleteTTLNotificationInfo, Error, InsertTTLInfo, KeysIntValue, LastOffset, Notification, Subscription, Success, SysNotificationInfo, TTL}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

trait PushNotificationRepository {
  def saveSubscription(user: String, subscription: Subscription): Future[Either[Error, Success]]
  def deleteSubscription(user: String, subscription: Subscription): Future[Either[Error, Success]]
  def deleteAllSubscription(user: String): Future[Success]
  def getSubscriptions(user: String): Future[Seq[Subscription]]
  def saveNotifications(notification: Notification): Future[Either[Error, Success]]
  def updateNotifications(notifications: Seq[Notification]): Future[Either[Error, Success]]
  def getAllNotifications(user: String, limit: Option[Int]): Future[Seq[Notification]]
  def checkNewNotifications(user: String): Future[Seq[Notification]]
  def getLastOffset(notificationType: String): Future[LastOffset]
  def updateTtl(ttl: Seq[KeysIntValue]): Future[Either[Error, Success]]
  def getTtl: Future[Either[Error, Seq[KeysIntValue]]]
  def systemNotificationInsert(sysNotificationInfo: SysNotificationInfo, token: String, ws: WSClient): Future[Either[Error, Success]]
  def deleteSystemNotificationByOffset(offset: Int): Future[Either[Error, Success]]
  def updateSystemNotification(offset: Int, notificationInfo: SysNotificationInfo): Future[Either[Error, Success]]
  def getSystemNotificationByOffset(offset: Int): Future[Either[Error, Notification]]
  def getAllSystemNotification: Future[Either[Error, Seq[Notification]]]
  def insertTtl(insertTTLInfo: InsertTTLInfo): Future[Either[Error, Success]]
  def deleteTtl(deleteTTLNotificationsInfo: DeleteTTLNotificationInfo): Future[Either[Error, Success]]
}


object PushNotificationRepository {
  def apply(config: String): PushNotificationRepository = config match {
    case "dev" => new PushNotificationRepositoryDev
    case "prod" => new PushNotificationRepositoryProd
  }
}

trait PushNotificationRepositoryComponent {
  val pushNotificationRepository: PushNotificationRepository
}