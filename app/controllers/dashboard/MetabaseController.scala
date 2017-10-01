package controllers.dashboard


import javax.inject._

import it.gov.daf.sso.client.LoginClientRemote
import it.gov.daf.sso.common.{LoginInfo, SecuredInvocationManager}
import play.api.cache.CacheApi
import play.api.{Configuration, Environment}
import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.{Await, ExecutionContext, Future}
import play.api.libs.json._
import play.api.inject.ConfigurationProvider
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}



@Singleton
class MetabaseController @Inject() (ws: WSClient,
                                    cache: CacheApi ,
                                    config: ConfigurationProvider
                                    )(implicit context: ExecutionContext) extends Controller {

 // import play.api.libs.concurrent.Execution.Implicits.defaultContext

  val conf = Configuration.load(Environment.simple())
  val URL : String = conf.getString("metabase.url").get
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
        println("Ale " + resp)
        cache.set("metabase." + metauser, resp, 14 days)
        Ok(resp)
      }
  }


  def publicCard(metauser :String) =  Action.async { implicit request =>

    val sim = SecuredInvocationManager.instance(LoginClientRemote.instance())

    println("wee-->"+URL + "/api/card/public")
    def callPublicSlice(cookie:String, wsClient:AhcWSClient)=
      ws.url(URL + "/api/card/public").withHeaders(("X-Metabase-Session", cookie),("Cookie","metabase.SESSION_ID="+cookie)).get()

    sim.manageServiceCall( new LoginInfo(metauser,null,"metabase"),callPublicSlice ).map{Ok(_)}

  }

  /*
  def publicCard(metauser :String) =  Action.async { implicit request =>
    val sessionId  = cache.get[String]("metabase." + metauser).getOrElse("test")
    val url = URL + "/api/card/public"
    println(url)
    val responseWs = ws.url(url)
      .withHeaders(("X-Metabase-Session", sessionId)).get()
    responseWs.map { response =>
      if(response.status == 401) {
        val sessionFuture: Future[String] = ws.url(local + "/metabase/session").get().map(_.body)

        val result: Future[WSResponse] = for {
          session <- sessionFuture
          cards <- ws.url(URL + "/api/card/public")
            .withHeaders(("X-Metabase-Session", session)).get()
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
  }*/

}
