package controllers.dashboard

import it.gov.daf.common.utils.RequestContext
import javax.inject._
import play.api.cache.CacheApi
import play.api.{Configuration, Environment, Logger}
import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.{Await, ExecutionContext, Future}
import play.api.libs.json._
import play.api.inject.ConfigurationProvider

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


@Singleton
class TeamDashboardController @Inject() (ws: WSClient,
                                    cache: CacheApi ,
                                    config: ConfigurationProvider
                                   )(implicit context: ExecutionContext) extends Controller {

  val conf = Configuration.load(Environment.simple())
  val URL = conf.getString("tdmetabase.url").getOrElse("https://dashboard.teamdigitale.governo.it")
  val metauser = conf.getString("tdmetabase.user").getOrElse("xxxxx")
  val metapass = conf.getString("tdmetabase.pass").getOrElse("xxxxxxxxxx")

  val local = conf.getString("app.local.url").getOrElse("xxxxxxxxxx")

  private val logger = Logger(this.getClass.getName)


  def tdMetaSession() = Action.async { implicit request =>
    RequestContext.execInContext[Future[Result]] ("tdMetaSession") { () =>

      val data = Json.obj(
        "username" -> metauser,
        "password" -> metapass
      )
      val responseWs: Future[WSResponse] = ws.url(URL + "/api/session").post(data)
      responseWs.map { response =>
        val resp = (response.json \ "id").as[String]
        println("Ale " + resp)
        cache.set("tdmetabase." + metauser, resp, 14 days)
        Ok(resp)
      }
    }
  }

  def tdMetaPublicCard() =  Action.async { implicit request =>
    RequestContext.execInContext[Future[Result]] ("tdMetaPublicCard") { () =>
      val sessionId = cache.get[String]("tdmetabase." + metauser).getOrElse("test")
      val url = URL + "/api/card/public"
      println(url)
      val responseWs: Future[WSResponse] = ws.url(url).withHeaders(("X-Metabase-Session", sessionId)).get
      responseWs.map { response =>
        if ((response.status == 401)) {
          //val sessionFuture = ws.url(local + "/metabase/session")

          val result: Future[WSResponse] = for {
            session <- ws.url(local + "/tdmetabase/session").get()
            cards <- ws.url(URL + "/api/card/public").withHeaders(("X-Metabase-Session", session.body)).get
          } yield cards

          val responseTry: Try[WSResponse] = Await.ready(result, 3 seconds).value.get
          val jsonResponse: JsValue = responseTry match {
            case Success(v) => v.json
            case Failure(_) => throw new RuntimeException("error")
          }
          Ok(jsonResponse)
        } else {
          Ok(response.json)
        }
      }
    }
  }

}
