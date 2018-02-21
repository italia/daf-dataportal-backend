package controllers.monitor

/**
  * Created by ale on 30/03/17.
  */
import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import services.ComponentRegistry

@Singleton
class MonitorQueryController @Inject() extends Controller {

  def catalogs() = Action {
    val test = ComponentRegistry.monitorService.allCatalogs()
    println(test)
    Ok(Json.parse("""[{"ale" : "test"},{ "ale" : "dsadasda"}]"""))
  }

}
