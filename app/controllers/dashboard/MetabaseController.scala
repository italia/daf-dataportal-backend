package controllers.dashboard


import javax.inject._

import it.gov.daf.common.sso.common.{LoginInfo, SecuredInvocationManager}
import play.api.cache.CacheApi
import play.api.{Configuration, Environment}
import play.api.mvc._
import play.api.libs.ws._
import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import play.api.inject.ConfigurationProvider

import scala.concurrent.duration._


@Singleton
class MetabaseController @Inject() (ws: WSClient,
                                    cache: CacheApi ,
                                    config: ConfigurationProvider,
                                    sim: SecuredInvocationManager
                                    )(implicit context: ExecutionContext) extends Controller {

  val conf = Configuration.load(Environment.simple())
  val URL = conf.getString("metabase.url").get
  val metauser = conf.getString("metabase.user").get
  val metapass = conf.getString("metabase.pass").get

  val local = conf.getString("app.local.url").get

  def session() = Action.async { implicit request =>
    val data = Json.obj(
      "username" -> metauser,
      "password" -> metapass
    )
    val responseWs: Future[WSResponse] = ws.url(URL + "/api/session").post(data)
      responseWs.map { response =>
        val resp = (response.json \ "id").as[String]
        Logger.info(resp)
        cache.set("metabase." + metauser, resp, 14 days)
        Ok(resp)
      }
  }


  def publicCard(metauser :String) =  Action.async { implicit request =>


    println("wee-->"+URL + "/api/card/public")
    def callPublicSlice(cookie:String, wsClient:WSClient)=
      wsClient.url(URL + "/api/card/public").withHeaders(("X-Metabase-Session", cookie),("Cookie",cookie)).get()

    sim.manageServiceCall( new LoginInfo(metauser,null,"metabase"),callPublicSlice ).map{Ok(_)}

  }
}
