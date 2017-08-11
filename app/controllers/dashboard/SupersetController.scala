package controllers.dashboard

import javax.inject._

import play.api.{Configuration, Environment}
import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.Future
import play.api.libs.json._
import play.api.inject.ConfigurationProvider



@Singleton
class SupersetController @Inject() ( ws: WSClient, config: ConfigurationProvider) extends Controller {

  import play.api.libs.concurrent.Execution.Implicits.defaultContext

  def getIframes() = Action.async { implicit request =>
    val iframeJson = ws.url("http://localhost:8088/slicemodelview/api/read")
      .withHeaders("Cookie" -> """Cookie:csrftoken=QmvKnmdoxhe6mmRdHj9OscISZlWi2QT9; __utma=111872281.1060986550.1484824167.1484918611.1484927618.12; Idea-e243532e=3f306772-24a2-4be2-9425-4d30ba6169c8; _ga=GA1.1.1060986550.1484824167; auth_tkt="f0cfdd1af4b673fe1c91e3a25d2ce994596f3bd0ckanadmin!userid_type=unicode"; session=.eJyFkEFqwzAQRe8ya4NkxZYlQekJClllE4qxpXEtokRGo6SE0LtXTkO7aelqhuG9_2Fu0E8JaQaT0xkr6L0DA4KPunWTlhuNiI6rZp3dMDnh2q5DqMBSmvocD3gqvK7FuLFcjK7BVtpaCm0HVbam7aS0UnDXOCF08UK0Q8DiFLGCZXjDfvaUY7qC2cOc82IYu0NzpGwUV4otmI6eyMcTsVBg9nz3tt_nl-gw7Dy-P9Ul9PeQM2F66H8yP0WXEvYfTcFbPK7VK_2AXytYm77-WMPHJ_Y7dH8.DFuvzw.hj0aw6H_DAh_R9YtVND3K6yhy1o""")
      .get
    iframeJson map { response =>
      val results = (response.json \ "result").get
      Ok(results)
    }
  }

  val conf = Configuration.load(Environment.simple())
  val URL : String = conf.getString("superset.url").get
  val user = conf.getString("superset.user").get
  val pass = conf.getString("superset.pass").get

  def session() = Action.async { implicit request =>
    val data = Json.obj(
      "email" -> user,
      "password" -> pass
    )
    val responseWs: Future[WSResponse] = ws.url(URL + "/login/").post(data)
    responseWs.map { response =>
      println(response.cookie("session"))
      println(response.header("Set-Cookie"))
      val session = response.cookie("session").get.toString
      Ok(session)
    }
  }

  def publicSlice(sessionCookie :String) =  Action.async { implicit request =>
    println("ALE")
    val responseWs = ws.url(URL + "slicemodelview/api/read")
      .withHeaders("Cookie" -> sessionCookie).get()
    responseWs.map { response =>
      println(response.json)
      Ok(response.json)
    }
  }

}