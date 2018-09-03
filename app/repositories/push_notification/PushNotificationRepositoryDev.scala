package repositories.push_notification

import ftd_api.yaml.{Error, LastOffset, Notification, Subscription, Success}

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

  override def getLastOffset(notificationType: String): Future[LastOffset] = {
    Future.successful(LastOffset(0))
  }
}
