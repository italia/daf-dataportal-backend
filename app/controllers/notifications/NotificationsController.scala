package controllers.notifications

import ftd_api.yaml.{Error, KafkaOffsett}
import it.gov.daf.common.utils.RequestContext
import javax.inject.Inject
import play.api.Logger
import play.api.cache.CacheApi
import play.api.inject.ConfigurationProvider
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.push_notification.PushNotificationRegistry

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


class NotificationsController @Inject()(ws: WSClient,
                                        cache: CacheApi,
                                        config: ConfigurationProvider
                                       )(implicit context: ExecutionContext) extends Controller {

  private val logger = Logger(this.getClass.getName)

  private def validateKafkaOffset(request: Request[AnyContent]): Either[Error, KafkaOffsett] = {
    request.body.asJson match {
      case Some(json) =>
        Try{
          KafkaOffsett(
            topicName = (json \ "topicName").as[String],
            offset = (json \ "offset").as[Long]
          )
        } match {
          case Failure(error) =>
            logger.debug(s"[validateKafkaOffset] error: $error")
            Left(Error(Some(400), Some("request body invalid"), None))
          case Success(value) =>
            logger.debug(s"[validateKafkaOffset] success: $value")
            Right(value)
        }
      case None       =>
        logger.debug("no json found in request body")
        Left(Error(Some(400), Some("json in body missing"), None))
    }
  }

  def updateKafkaOffset = Action.async { implicit request =>
    RequestContext.execInContext[Future[Result]] ("updateKafkaOffset") { () =>
      validateKafkaOffset(request) match {
        case Left(error)         => Future.successful(BadRequest(error.message.getOrElse("")))
        case Right(kafkaOffsett) =>
          PushNotificationRegistry.pushNotificationService.updateKafkaOffset(kafkaOffsett) flatMap {
            case Right(success) => Future.successful(Ok(success.message.getOrElse("")))
            case Left(error)    => Future.successful(InternalServerError(error.message.getOrElse("")))
          }
      }
    }
  }

}
