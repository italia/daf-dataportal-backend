package controllers.monitor

/**
  * Created by ale on 30/03/17.
  */
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import io.swagger.annotations._
import services.ComponentRegistry

@Singleton
@Api("Monitor")
class MonitorQueryController @Inject() extends Controller {

  @ApiOperation(value = "List of original catalogs imported",
    notes = "List of original catalogs imported",
    responseContainer = "List")
  def catalogs() = Action {
    val test = ComponentRegistry.monitorService.allCatalogs()
    println(test)
    Ok(Json.parse("""[{"ale" : "test"},{ "ale" : "prova"}]"""))
  }

}
