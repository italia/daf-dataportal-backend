package repositories.dashboard

import javax.inject.Singleton

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.{Configuration, Environment}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future

/**
  * Created by ale on 19/04/17.
  */
@Singleton
class MetabaseWs {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  import scala.concurrent.ExecutionContext.Implicits.global

  val conf = Configuration.load(Environment.simple())
  val URL : String = conf.getString("metabase.url").get

  def call(wsClient: WSClient, data :JsObject): Future[Seq[WSResponse]] = {
    def sessionIds: Future[String] = wsClient.url(URL + "/session").post(data).map { response =>
      val resp = (response.json \ "id").as[String]
      println("Ale " + resp)
      resp
    }

    def databases(sessionId :String): Future[Seq[Int]] = {
      wsClient.url(URL + "/database").withHeaders(("X-Metabase-Session", sessionId))
        .get().map{  response =>
        println(response.json)
        val test = (response.json \\ "id")
        println("Test : " + test )
        val test2 = test.map(_.as[Int])
        test2
      }
    }

    def syncAll(databases : Seq[Int], sessionId :String) :Future[Seq[WSResponse]]  = {
      val results = databases.map(x => {
        println("DatabaseId " + x)
        wsClient.url(URL + "/database/" + x + "/sync")
          .withHeaders(("X-Metabase-Session", sessionId)).post("write")
      })
      Future.sequence(results)
    }

    for {
      sessionId <- sessionIds
      database <- databases(sessionId)
      sync   <- syncAll(database, sessionId)
    } yield sync

  }


  def syncMetabase(email: String, pass: String): Future[Unit] = {
    val wsClient = AhcWSClient()
    println("Start")
    val data = Json.obj(
      "email" -> email,
      "password" -> pass
    )
    val syncAll: Future[Seq[WSResponse]] = call(wsClient,data)
      .andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

    Future { println("hello, from elsewhere.") }
  }
}
