package repositories.push_notification

import ftd_api.yaml.{Error, Subscription, Success}

import scala.concurrent.Future

trait PushNotificationRepository {
  def save(user: String, subscription: Subscription): Future[Either[Error, Success]]
  def getSubscriptions(user: String): Future[Seq[Subscription]]

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