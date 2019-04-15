package services.push_notification

import play.api.{Configuration, Environment}
import ftd_api.yaml.{DeleteTTLNotificationInfo, Error, InsertTTLInfo, KafkaOffsett, KeysIntValue, LastOffset, Notification, Subscription, Success, SysNotificationInfo, TTL}
import play.api.libs.ws.WSClient
import repositories.push_notification.{PushNotificationRepository, PushNotificationRepositoryComponent}

import scala.concurrent.Future

trait PushNotificationServiceComponent {
  this: PushNotificationRepositoryComponent =>
  val pushNotificationService: PushNotificationService

  class PushNotificationService {
    def saveSubscription(user:String, subscription: Subscription): Future[Either[Error, Success]] = {
      pushNotificationRepository.saveSubscription(user, subscription)
    }

    def getSubscriptions(user: String): Future[Seq[Subscription]] = {
      pushNotificationRepository.getSubscriptions(user)
    }

    def deleteSubscription(user: String, subscription: Subscription) = {
      pushNotificationRepository.deleteSubscription(user, subscription)
    }

    def deleteAllSubscription(user: String) = {
      pushNotificationRepository.deleteAllSubscription(user)
    }

    def saveNotifications(notification: Notification) = {
      pushNotificationRepository.saveNotifications(notification)
    }

    def updateNotifications(notification: Seq[Notification]) = {
      pushNotificationRepository.updateNotifications(notification)
    }
    def getAllNotifications(user: String, limit: Option[Int]) = {
      pushNotificationRepository.getAllNotifications(user, limit)
    }

    def checkNewNotifications(user: String): Future[Seq[Notification]] = {
      pushNotificationRepository.checkNewNotifications(user)
    }

    def getLastOffset(topicName: String): Future[Either[Error, Long]] = {
      pushNotificationRepository.getLastOffset(topicName)
    }

    def updateTtl(ttl: Seq[KeysIntValue]): Future[Either[Error, Success]] = {
      pushNotificationRepository.updateTtl(ttl)
    }

    def getTtl: Future[Either[Error, Seq[KeysIntValue]]] = {
      pushNotificationRepository.getTtl
    }

    def systemNotificationInsert(sysNotificationInfo: SysNotificationInfo, token: String, ws: WSClient): Future[Either[Error, Success]] = {
      pushNotificationRepository.systemNotificationInsert(sysNotificationInfo, token, ws)
    }

    def deleteSystemNotificationByOffset(offset: Int): Future[Either[Error, Success]] = {
      pushNotificationRepository.deleteSystemNotificationByOffset(offset)
    }

    def updateSystemNotification(offset: Int, notificationInfo: SysNotificationInfo): Future[Either[Error, Success]] = {
      pushNotificationRepository.updateSystemNotification(offset, notificationInfo)
    }

    def getSystemNotificationByOffset(offset: Int): Future[Either[Error, Notification]] = {
      pushNotificationRepository.getSystemNotificationByOffset(offset)
    }

    def getAllSystemNotification: Future[Either[Error, Seq[Notification]]] = {
      pushNotificationRepository.getAllSystemNotification
    }

    def getAllPublicSystemNotifications: Future[Either[Error, Seq[Notification]]] = {
      pushNotificationRepository.getAllPublicSystemNotifications
    }

    def insertTtl(insertTTLInfo: InsertTTLInfo): Future[Either[Error, Success]] = {
      pushNotificationRepository.insertTtl(insertTTLInfo)
    }

    def deleteTtl(deleteTTLNotificationsInfo: DeleteTTLNotificationInfo): Future[Either[Error, Success]] = {
      pushNotificationRepository.deleteTtl(deleteTTLNotificationsInfo)
    }

    def updateKafkaOffset(kafkaOffsett: KafkaOffsett): Future[Either[Error, Success]] = {
      pushNotificationRepository.updateKafkaOffset(kafkaOffsett)
    }

  }
}

object PushNotificationRegistry extends PushNotificationRepositoryComponent with PushNotificationServiceComponent{

  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("prod")
  val pushNotificationRepository: PushNotificationRepository =  PushNotificationRepository(app)
  val pushNotificationService = new PushNotificationService
}