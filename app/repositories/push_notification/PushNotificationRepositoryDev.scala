package repositories.push_notification

import ftd_api.yaml.{Error, Subscription, Success}
import scala.concurrent.Future

class PushNotificationRepositoryDev extends PushNotificationRepository {

  override def save(user: String, subscription: Subscription): Future[Either[Error, Success]] = {
    Future.successful(Right(Success(None, None)))
  }

  override def getSubscriptions(user: String): Future[Seq[Subscription]] = {
    Future.successful(Seq[Subscription]())
  }


}
