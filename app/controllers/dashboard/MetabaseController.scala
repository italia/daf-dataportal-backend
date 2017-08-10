package controllers.dashboard


import javax.inject._

import play.api.{Configuration, Environment}
import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.Future
import play.api.libs.json._
import play.api.inject.ConfigurationProvider


@Singleton
class MetabaseController @Inject() (ws: WSClient, config: ConfigurationProvider) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  val conf = Configuration.load(Environment.simple())
  val URL : String = conf.getString("metabase.url").get
  val metauser = conf.getString("metabase.user").get
  val metapass = conf.getString("metabase.pass").get

  def session() = Action.async { implicit request =>
    val data = Json.obj(
      "email" -> metauser,
      "password" -> metapass
    )
    val responseWs: Future[WSResponse] = ws.url(URL + "/api/session").post(data)
      responseWs.map { response =>
        val resp = (response.json \ "id").as[String]
        println("Ale " + resp)
        Ok(resp)
      }
  }

  def publicCard(sessionId :String) =  Action.async { implicit request =>
    println("ALE")
    val responseWs = ws.url(URL + "/api/card/public")
      .withHeaders(("X-Metabase-Session", sessionId)).get()
    responseWs.map { response =>
      println(response.json)
      Ok(response.json)
    }
  }

}
