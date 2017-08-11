package controllers.dashboard


import javax.inject._

import play.api.cache.CacheApi
import play.api.{Configuration, Environment}
import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.{Await, Future}
import play.api.libs.json._
import play.api.inject.ConfigurationProvider

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}



@Singleton
class MetabaseController @Inject() (ws: WSClient, cache: CacheApi ,config: ConfigurationProvider) extends Controller {

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
        cache.set(metauser, resp, 14 days)
        Ok(resp)
      }
  }

  def publicCard(metauser :String) =  Action.async { implicit request =>
    val sessionId  = cache.get[String](metauser).getOrElse("test")
    val responseWs = ws.url(URL + "/api/card/public")
      .withHeaders(("X-Metabase-Session", sessionId)).get()
    responseWs.map { response =>
      if(response.status == 401) {
        val sessionFuture = ws.url("http://localhost:9000/metabase/session").get()
        val session: Try[WSResponse] = Await.ready(sessionFuture, 3 seconds).value.get
        val ex: String = session match {
          case Success(v) => v.body
          case Failure(_) => throw new RuntimeException("error")
        }
      }
      Ok(response.json)

    }
  }

}
