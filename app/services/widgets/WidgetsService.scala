package services.widgets

import ftd_api.yaml.{Error, Widget}
import play.api.libs.ws.WSClient
import play.api.{Configuration, Environment}
import repositories.widgets.{WidgetsRepository, WidgetsRepositoryComponent}

import scala.concurrent.Future

trait WidgetsServiceComponent {
  this: WidgetsRepositoryComponent =>
  val widgetsService: WidgetsService

  class WidgetsService {
    def getAllWidgets(user: String, groups: List[String], org: Option[String], wsClient: WSClient): Future[Either[Error, Seq[Widget]]] = {
      widgetsRepository.getAllWidgets(user, groups, org, wsClient)
    }
  }

}

object WidgetsRegistry extends WidgetsServiceComponent with WidgetsRepositoryComponent {
  val conf = Configuration.load(Environment.simple())
  val app: String = conf.getString("app.type").getOrElse("dev")
  val widgetsRepository =  WidgetsRepository(app)
  val widgetsService = new WidgetsService
}