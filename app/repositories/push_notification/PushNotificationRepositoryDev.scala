package repositories.push_notification

import ftd_api.yaml.{Error, Notification, Subscription, Success}

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

  override def updateNotifications(notification: Seq[Notification]): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }

  override def getAllNotifications(user: String): Future[Seq[Notification]] = {
    Future.successful(Seq[Notification]())
  }
}
