package controllers.dashboard

import javax.inject._

import play.api.cache.CacheApi
import play.api.{Configuration, Environment}
import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.{Await, ExecutionContext, Future}
import play.api.libs.json._
import play.api.inject.ConfigurationProvider
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.duration._


@Singleton
class GrafanaController @Inject() (ws: WSClient,
                                    cache: CacheApi ,
                                    config: ConfigurationProvider
                                   )(implicit context: ExecutionContext) extends Controller {

  val conf = Configuration.load(Environment.simple())
  val URL = conf.getString("grafana.url").get
  // TMP for test purpose
  val apiKey = conf.getString("grafana.apiKey").get


  def snapshots = Action.async { implicit request =>
    println("ALEOOOOOOOOO")
    println(URL)
    println(apiKey)
    val responseWs: Future[WSResponse] = ws.url(URL + "/api/dashboard/snapshots")
          .withHeaders(("Authorization", "Bearer " + apiKey)).get
    responseWs.map { response =>
      println(response.json)
      Ok(response.json)
    }
  }

}
