package repositories.widgets

import ftd_api.yaml.{Error, Widget}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

class WidgetsRepositoryDev extends WidgetsRepository  {

  override def getAllWidgets(user: String, groups: List[String], org: Option[String], wsClient: WSClient): Future[Either[Error, Seq[Widget]]] = {
    Future.successful(Left(Error(Some(404), Some("widgets not found"), None)))
  }
}
