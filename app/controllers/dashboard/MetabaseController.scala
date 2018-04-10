package controllers.dashboard


import javax.inject._

import it.gov.daf.common.sso.common.{LoginInfo, SecuredInvocationManager}
import play.api.cache.CacheApi
import play.api.{Configuration, Environment}
import play.api.mvc._
import play.api.libs.ws._
import play.api.Logger

import scala.concurrent.{Await, ExecutionContext, Future}
import play.api.libs.json._
import play.api.inject.ConfigurationProvider

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


@Singleton
class MetabaseController @Inject() (ws: WSClient,
                                    cache: CacheApi ,
                                    config: ConfigurationProvider,
                                    sim: SecuredInvocationManager
                                    )(implicit context: ExecutionContext) extends Controller {

  val conf = Configuration.load(Environment.simple())
  val URL = conf.getString("metabase.url").get
  val metausername = conf.getString("metabase.user").get
  val metapass = conf.getString("metabase.pass").get

  val local = conf.getString("app.local.url").get

  private def cards(tableId :String) = {
    val sessionId  = cache.get[String]("metabase." + metausername).getOrElse("test")
    val url = URL + "/api/card?f=table&model_id=" + tableId
    ws.url(url)
      .withHeaders(("X-Metabase-Session", sessionId))
      .get
      .map{ resp =>
        resp.json
      }
  }

  def session() = Action.async { implicit request =>
    val data = Json.obj(
      "username" -> metausername,
      "password" -> metapass
    )
    val responseWs: Future[WSResponse] = ws.url(URL + "/api/session").post(data)
      responseWs.map { response =>
        val resp = (response.json \ "id").as[String]
        Logger.info(resp)
        cache.set("metabase." + metausername, resp, 14 days)
        Ok(resp)
      }
  }

/*
  def publicCard(metauser :String) =  Action.async { implicit request =>
    // NB metausername is GLOBAL it is temporary must be used the commented method below
    val sessionId  = cache.get[String]("metabase." + metausername).getOrElse("test")
    val url = URL + "/api/card"
    println(url)
    val responseWs: Future[WSResponse] = ws.url(url).withHeaders(("X-Metabase-Session", sessionId)).get
    responseWs.map { response =>
      if((response.status == 401)) {
        //val sessionFuture = ws.url(local + "/metabase/session")

        val result: Future[WSResponse] = for {
          session  <- ws.url(local + "/metabase/session").get()
          cards <- ws.url(URL + "/api/card").withHeaders(("X-Metabase-Session", session.body)).get
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

  def tableInfo(tableId :String) = Action.async { implicit request =>
    val sessionId  = cache.get[String]("metabase." + metausername).getOrElse("test")
    val url = URL + "/api/table/" + tableId
    println(url)
    val responseWs: Future[WSResponse] = ws.url(url).withHeaders(("X-Metabase-Session", sessionId)).get
    responseWs.map { response =>
      Ok(response.json)
    }
  }

 /* def isDatasetOnMetabase(tableName :String) = Action.async { implicit request =>
    val sessionId = cache.get[String]("metabase." + metausername).getOrElse("test")
    val url = URL + "/api/table"
    println(url)
    val responseWs: Future[WSResponse] = ws.url(url).withHeaders(("X-Metabase-Session", sessionId)).get
    responseWs.map { response =>
      val tables = response.json.as[Seq[JsValue]]
      val res: Seq[JsValue] = tables.filter(x => (x \ "name").as[String].equals(tableName))
      val result = if (res.isEmpty){
        JsBoolean(false)
      } else {
        JsBoolean(true)
      }
      Ok(Json.toJson(result))
    } */

    def isDatasetOnMetabase(tableName :String) = Action.async { implicit request =>

      val url = URL + "/api/table"
      Logger.debug("publicCard request:"+ url)

      def callPublicSlice(cookie:String, wsClient:WSClient) = {
        wsClient.url(url)
          .withHeaders(("X-Metabase-Session", cookie),
            ("Cookie", cookie))
          .get()
      }

      sim.manageServiceCall( new LoginInfo(null,null,"metabase"),callPublicSlice ).map { response =>
        val tables = response.json.as[Seq[JsValue]]
        val res: Seq[JsValue] = tables.filter(x => (x \ "name").as[String].equals(tableName))
        val result = if (res.isEmpty) {
          JsBoolean(false)
        } else {
          JsBoolean(true)
        }
        Ok(Json.toJson(result))
      }

  }


  def cardsFromTable(tableName :String) = Action.async { implicit request =>
    val sessionId  = cache.get[String]("metabase." + metausername).getOrElse("test")
    val url = URL + "/api/table"
    println(url)
    val responseWs: Future[WSResponse] = ws.url(url).withHeaders(("X-Metabase-Session", sessionId)).get
    val tablesId: Future[Option[Int]] = responseWs.map { response =>
      val tables = response.json.as[Seq[JsValue]]
      val res: Seq[JsValue] = tables.filter(x => (x \ "name").as[String].equals(tableName))
      //Ok(Json.toJson(res))
      val single = res.map(x => {
        (x \ "id").as[Int]
      })
      if (!single.isEmpty) {
        Some(single.head)
      } else {
        None
      }
    }

    val result: Future[JsValue] = for {
      optId <- tablesId
      card <- cards(optId.get.toString())
    } yield card

    result.map {Ok(_)}
  }



  def publicCard(metauser :String) =  Action.async { implicit request =>

    Logger.debug("publicCard request:"+URL + "/api/card")
    def callPublicSlice(cookie:String, wsClient:WSClient)=
      wsClient.url(URL + "/api/card").withHeaders(("X-Metabase-Session", cookie),("Cookie",cookie)).get()

    sim.manageServiceCall( new LoginInfo(null,null,"metabase"),callPublicSlice ).map{resp => Ok(resp.json)}

  }
}
