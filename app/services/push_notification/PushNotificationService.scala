package services.push_notification

import play.api.{Configuration, Environment}
import ftd_api.yaml.{Error, Notification, Subscription, Success}
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

  }
}

object PushNotificationRegistry extends PushNotificationRepositoryComponent with PushNotificationServiceComponent{

  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("prod")
  val pushNotificationRepository: PushNotificationRepository =  PushNotificationRepository(app)
  val pushNotificationService = new PushNotificationService
}