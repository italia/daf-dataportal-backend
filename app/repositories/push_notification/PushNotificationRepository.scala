package repositories.push_notification

import ftd_api.yaml.{Error, LastOffset, Notification, Subscription, Success}

import scala.concurrent.Future

trait PushNotificationRepository {
  def saveSubscription(user: String, subscription: Subscription): Future[Either[Error, Success]]
  def getSubscriptions(user: String): Future[Seq[Subscription]]
  def saveNotifications(notification: Notification): Future[Either[Error, Success]]
  def updateNotifications(notifications: Seq[Notification]): Future[Either[Error, Success]]
  def getAllNotifications(user: String, limit: Option[Int]): Future[Seq[Notification]]
  def checkNewNotifications(user: String): Future[Seq[Notification]]
  def getLastOffset: Future[LastOffset]

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