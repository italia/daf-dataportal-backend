package repositories.widgets

import ftd_api.yaml.{Error, Widget}
import play.api.libs.ws.WSClient

import scala.concurrent.Future

trait WidgetsRepository {

  def getAllWidgets(user: String, groups: List[String], org: Option[String], wsClient: WSClient): Future[Either[Error, Seq[Widget]]]

}

object WidgetsRepository {
  def apply(config: String): WidgetsRepository = config match {
    case "dev" => new WidgetsRepositoryDev
    case "prod" => new WidgetsRepositoryProd
  }
}

trait WidgetsRepositoryComponent {
  val widgetsRepository: WidgetsRepository
}